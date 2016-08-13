/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.net.Uri
 *  android.net.http.HttpResponseCache
 *  android.os.Build
 *  android.os.Build$VERSION
 */
package com.squareup.picasso;

import android.content.Context;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Build;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Utils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class UrlConnectionDownloader
implements Downloader {
    static final String RESPONSE_SOURCE = "X-Android-Response-Source";
    static volatile Object cache;
    private static final Object lock;
    private static final String FORCE_CACHE = "only-if-cached,max-age=2147483647";
    private static final ThreadLocal<StringBuilder> CACHE_HEADER_BUILDER;
    private final Context context;

    public UrlConnectionDownloader(Context context) {
        this.context = context.getApplicationContext();
    }

    protected HttpURLConnection openConnection(Uri path) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)new URL(path.toString()).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        return connection;
    }

    @Override
    public Downloader.Response load(Uri uri, int networkPolicy) throws IOException {
        int responseCode;
        if (Build.VERSION.SDK_INT >= 14) {
            UrlConnectionDownloader.installCacheIfNeeded(this.context);
        }
        HttpURLConnection connection = this.openConnection(uri);
        connection.setUseCaches(true);
        if (networkPolicy != 0) {
            String headerValue;
            if (NetworkPolicy.isOfflineOnly(networkPolicy)) {
                headerValue = "only-if-cached,max-age=2147483647";
            } else {
                StringBuilder builder = CACHE_HEADER_BUILDER.get();
                builder.setLength(0);
                if (!NetworkPolicy.shouldReadFromDiskCache(networkPolicy)) {
                    builder.append("no-cache");
                }
                if (!NetworkPolicy.shouldWriteToDiskCache(networkPolicy)) {
                    if (builder.length() > 0) {
                        builder.append(',');
                    }
                    builder.append("no-store");
                }
                headerValue = builder.toString();
            }
            connection.setRequestProperty("Cache-Control", headerValue);
        }
        if ((responseCode = connection.getResponseCode()) >= 300) {
            connection.disconnect();
            throw new Downloader.ResponseException("" + responseCode + " " + connection.getResponseMessage(), networkPolicy, responseCode);
        }
        long contentLength = connection.getHeaderFieldInt("Content-Length", -1);
        boolean fromCache = Utils.parseResponseSourceHeader(connection.getHeaderField("X-Android-Response-Source"));
        return new Downloader.Response(connection.getInputStream(), fromCache, contentLength);
    }

    @Override
    public void shutdown() {
        if (Build.VERSION.SDK_INT >= 14 && cache != null) {
            ResponseCacheIcs.close(cache);
        }
    }

    private static void installCacheIfNeeded(Context context) {
        if (cache == null) {
            try {
                Object object = lock;
                synchronized (object) {
                    if (cache == null) {
                        cache = ResponseCacheIcs.install(context);
                    }
                }
            }
            catch (IOException ignored) {
                // empty catch block
            }
        }
    }

    static {
        lock = new Object();
        CACHE_HEADER_BUILDER = new ThreadLocal<StringBuilder>(){

            @Override
            protected StringBuilder initialValue() {
                return new StringBuilder();
            }
        };
    }

    private static class ResponseCacheIcs {
        private ResponseCacheIcs() {
        }

        static Object install(Context context) throws IOException {
            File cacheDir = Utils.createDefaultCacheDir(context);
            HttpResponseCache cache = HttpResponseCache.getInstalled();
            if (cache == null) {
                long maxSize = Utils.calculateDiskCacheSize(cacheDir);
                cache = HttpResponseCache.install((File)cacheDir, (long)maxSize);
            }
            return cache;
        }

        static void close(Object cache) {
            try {
                ((HttpResponseCache)cache).close();
            }
            catch (IOException ignored) {
                // empty catch block
            }
        }
    }

}


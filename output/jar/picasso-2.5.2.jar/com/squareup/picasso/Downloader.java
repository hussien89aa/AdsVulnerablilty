/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.graphics.Bitmap
 *  android.net.Uri
 */
package com.squareup.picasso;

import android.graphics.Bitmap;
import android.net.Uri;
import com.squareup.picasso.NetworkPolicy;
import java.io.IOException;
import java.io.InputStream;

public interface Downloader {
    public Response load(Uri var1, int var2) throws IOException;

    public void shutdown();

    public static class Response {
        final InputStream stream;
        final Bitmap bitmap;
        final boolean cached;
        final long contentLength;

        @Deprecated
        public Response(Bitmap bitmap, boolean loadedFromCache) {
            if (bitmap == null) {
                throw new IllegalArgumentException("Bitmap may not be null.");
            }
            this.stream = null;
            this.bitmap = bitmap;
            this.cached = loadedFromCache;
            this.contentLength = -1;
        }

        @Deprecated
        public Response(InputStream stream, boolean loadedFromCache) {
            this(stream, loadedFromCache, -1);
        }

        @Deprecated
        public Response(Bitmap bitmap, boolean loadedFromCache, long contentLength) {
            this(bitmap, loadedFromCache);
        }

        public Response(InputStream stream, boolean loadedFromCache, long contentLength) {
            if (stream == null) {
                throw new IllegalArgumentException("Stream may not be null.");
            }
            this.stream = stream;
            this.bitmap = null;
            this.cached = loadedFromCache;
            this.contentLength = contentLength;
        }

        public InputStream getInputStream() {
            return this.stream;
        }

        @Deprecated
        public Bitmap getBitmap() {
            return this.bitmap;
        }

        public long getContentLength() {
            return this.contentLength;
        }
    }

    public static class ResponseException
    extends IOException {
        final boolean localCacheOnly;
        final int responseCode;

        public ResponseException(String message, int networkPolicy, int responseCode) {
            super(message);
            this.localCacheOnly = NetworkPolicy.isOfflineOnly(networkPolicy);
            this.responseCode = responseCode;
        }
    }

}


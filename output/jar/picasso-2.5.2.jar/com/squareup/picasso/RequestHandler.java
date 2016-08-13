/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.graphics.Bitmap
 *  android.graphics.Bitmap$Config
 *  android.graphics.BitmapFactory
 *  android.graphics.BitmapFactory$Options
 *  android.net.NetworkInfo
 */
package com.squareup.picasso;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.NetworkInfo;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.Utils;
import java.io.IOException;
import java.io.InputStream;

public abstract class RequestHandler {
    public abstract boolean canHandleRequest(Request var1);

    public abstract Result load(Request var1, int var2) throws IOException;

    int getRetryCount() {
        return 0;
    }

    boolean shouldRetry(boolean airplaneMode, NetworkInfo info) {
        return false;
    }

    boolean supportsReplay() {
        return false;
    }

    static BitmapFactory.Options createBitmapOptions(Request data) {
        boolean justBounds = data.hasSize();
        boolean hasConfig = data.config != null;
        BitmapFactory.Options options = null;
        if (justBounds || hasConfig) {
            options = new BitmapFactory.Options();
            options.inJustDecodeBounds = justBounds;
            if (hasConfig) {
                options.inPreferredConfig = data.config;
            }
        }
        return options;
    }

    static boolean requiresInSampleSize(BitmapFactory.Options options) {
        return options != null && options.inJustDecodeBounds;
    }

    static void calculateInSampleSize(int reqWidth, int reqHeight, BitmapFactory.Options options, Request request) {
        RequestHandler.calculateInSampleSize(reqWidth, reqHeight, options.outWidth, options.outHeight, options, request);
    }

    static void calculateInSampleSize(int reqWidth, int reqHeight, int width, int height, BitmapFactory.Options options, Request request) {
        int sampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            if (reqHeight == 0) {
                sampleSize = (int)Math.floor((float)width / (float)reqWidth);
            } else if (reqWidth == 0) {
                sampleSize = (int)Math.floor((float)height / (float)reqHeight);
            } else {
                int heightRatio = (int)Math.floor((float)height / (float)reqHeight);
                int widthRatio = (int)Math.floor((float)width / (float)reqWidth);
                sampleSize = request.centerInside ? Math.max(heightRatio, widthRatio) : Math.min(heightRatio, widthRatio);
            }
        }
        options.inSampleSize = sampleSize;
        options.inJustDecodeBounds = false;
    }

    public static final class Result {
        private final Picasso.LoadedFrom loadedFrom;
        private final Bitmap bitmap;
        private final InputStream stream;
        private final int exifOrientation;

        public Result(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            this(Utils.checkNotNull(bitmap, "bitmap == null"), null, loadedFrom, 0);
        }

        public Result(InputStream stream, Picasso.LoadedFrom loadedFrom) {
            this(null, Utils.checkNotNull(stream, "stream == null"), loadedFrom, 0);
        }

        Result(Bitmap bitmap, InputStream stream, Picasso.LoadedFrom loadedFrom, int exifOrientation) {
            if (!(bitmap != null ^ stream != null)) {
                throw new AssertionError();
            }
            this.bitmap = bitmap;
            this.stream = stream;
            this.loadedFrom = Utils.checkNotNull(loadedFrom, "loadedFrom == null");
            this.exifOrientation = exifOrientation;
        }

        public Bitmap getBitmap() {
            return this.bitmap;
        }

        public InputStream getStream() {
            return this.stream;
        }

        public Picasso.LoadedFrom getLoadedFrom() {
            return this.loadedFrom;
        }

        int getExifOrientation() {
            return this.exifOrientation;
        }
    }

}


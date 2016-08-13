/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.content.ContentResolver
 *  android.content.ContentUris
 *  android.content.Context
 *  android.database.Cursor
 *  android.graphics.Bitmap
 *  android.graphics.BitmapFactory
 *  android.graphics.BitmapFactory$Options
 *  android.net.Uri
 *  android.provider.MediaStore
 *  android.provider.MediaStore$Images
 *  android.provider.MediaStore$Images$Thumbnails
 *  android.provider.MediaStore$Video
 *  android.provider.MediaStore$Video$Thumbnails
 */
package com.squareup.picasso;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import com.squareup.picasso.ContentStreamRequestHandler;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import java.io.IOException;
import java.io.InputStream;

class MediaStoreRequestHandler
extends ContentStreamRequestHandler {
    private static final String[] CONTENT_ORIENTATION = new String[]{"orientation"};

    MediaStoreRequestHandler(Context context) {
        super(context);
    }

    @Override
    public boolean canHandleRequest(Request data) {
        Uri uri = data.uri;
        return "content".equals(uri.getScheme()) && "media".equals(uri.getAuthority());
    }

    @Override
    public RequestHandler.Result load(Request request, int networkPolicy) throws IOException {
        boolean isVideo;
        ContentResolver contentResolver = this.context.getContentResolver();
        int exifOrientation = MediaStoreRequestHandler.getExifOrientation(contentResolver, request.uri);
        String mimeType = contentResolver.getType(request.uri);
        boolean bl = isVideo = mimeType != null && mimeType.startsWith("video/");
        if (request.hasSize()) {
            Bitmap bitmap;
            PicassoKind picassoKind = MediaStoreRequestHandler.getPicassoKind(request.targetWidth, request.targetHeight);
            if (!isVideo && picassoKind == PicassoKind.FULL) {
                return new RequestHandler.Result(null, this.getInputStream(request), Picasso.LoadedFrom.DISK, exifOrientation);
            }
            long id = ContentUris.parseId((Uri)request.uri);
            BitmapFactory.Options options = MediaStoreRequestHandler.createBitmapOptions(request);
            options.inJustDecodeBounds = true;
            MediaStoreRequestHandler.calculateInSampleSize(request.targetWidth, request.targetHeight, picassoKind.width, picassoKind.height, options, request);
            if (isVideo) {
                int kind = picassoKind == PicassoKind.FULL ? 1 : picassoKind.androidKind;
                bitmap = MediaStore.Video.Thumbnails.getThumbnail((ContentResolver)contentResolver, (long)id, (int)kind, (BitmapFactory.Options)options);
            } else {
                bitmap = MediaStore.Images.Thumbnails.getThumbnail((ContentResolver)contentResolver, (long)id, (int)picassoKind.androidKind, (BitmapFactory.Options)options);
            }
            if (bitmap != null) {
                return new RequestHandler.Result(bitmap, null, Picasso.LoadedFrom.DISK, exifOrientation);
            }
        }
        return new RequestHandler.Result(null, this.getInputStream(request), Picasso.LoadedFrom.DISK, exifOrientation);
    }

    static PicassoKind getPicassoKind(int targetWidth, int targetHeight) {
        if (targetWidth <= PicassoKind.MICRO.width && targetHeight <= PicassoKind.MICRO.height) {
            return PicassoKind.MICRO;
        }
        if (targetWidth <= PicassoKind.MINI.width && targetHeight <= PicassoKind.MINI.height) {
            return PicassoKind.MINI;
        }
        return PicassoKind.FULL;
    }

    static int getExifOrientation(ContentResolver contentResolver, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, CONTENT_ORIENTATION, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                int n = 0;
                return n;
            }
            int n = cursor.getInt(0);
            return n;
        }
        catch (RuntimeException ignored) {
            int n = 0;
            return n;
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    static enum PicassoKind {
        MICRO(3, 96, 96),
        MINI(1, 512, 384),
        FULL(2, -1, -1);
        
        final int androidKind;
        final int width;
        final int height;

        private PicassoKind(int androidKind, int width, int height) {
            this.androidKind = androidKind;
            this.width = width;
            this.height = height;
        }
    }

}


/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.graphics.Bitmap
 *  android.media.ExifInterface
 *  android.net.Uri
 */
package com.squareup.picasso;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import com.squareup.picasso.ContentStreamRequestHandler;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import java.io.IOException;
import java.io.InputStream;

class FileRequestHandler
extends ContentStreamRequestHandler {
    FileRequestHandler(Context context) {
        super(context);
    }

    @Override
    public boolean canHandleRequest(Request data) {
        return "file".equals(data.uri.getScheme());
    }

    @Override
    public RequestHandler.Result load(Request request, int networkPolicy) throws IOException {
        return new RequestHandler.Result(null, this.getInputStream(request), Picasso.LoadedFrom.DISK, FileRequestHandler.getFileExifRotation(request.uri));
    }

    static int getFileExifRotation(Uri uri) throws IOException {
        ExifInterface exifInterface = new ExifInterface(uri.getPath());
        int orientation = exifInterface.getAttributeInt("Orientation", 1);
        switch (orientation) {
            case 6: {
                return 90;
            }
            case 3: {
                return 180;
            }
            case 8: {
                return 270;
            }
        }
        return 0;
    }
}


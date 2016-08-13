/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.content.ContentResolver
 *  android.content.Context
 *  android.net.Uri
 */
package com.squareup.picasso;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

class ContentStreamRequestHandler
extends RequestHandler {
    final Context context;

    ContentStreamRequestHandler(Context context) {
        this.context = context;
    }

    @Override
    public boolean canHandleRequest(Request data) {
        return "content".equals(data.uri.getScheme());
    }

    @Override
    public RequestHandler.Result load(Request request, int networkPolicy) throws IOException {
        return new RequestHandler.Result(this.getInputStream(request), Picasso.LoadedFrom.DISK);
    }

    InputStream getInputStream(Request request) throws FileNotFoundException {
        ContentResolver contentResolver = this.context.getContentResolver();
        return contentResolver.openInputStream(request.uri);
    }
}


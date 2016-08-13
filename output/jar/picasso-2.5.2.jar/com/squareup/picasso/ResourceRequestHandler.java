/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.content.res.Resources
 *  android.graphics.Bitmap
 *  android.graphics.BitmapFactory
 *  android.graphics.BitmapFactory$Options
 *  android.net.Uri
 */
package com.squareup.picasso;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import com.squareup.picasso.Utils;
import java.io.IOException;

class ResourceRequestHandler
extends RequestHandler {
    private final Context context;

    ResourceRequestHandler(Context context) {
        this.context = context;
    }

    @Override
    public boolean canHandleRequest(Request data) {
        if (data.resourceId != 0) {
            return true;
        }
        return "android.resource".equals(data.uri.getScheme());
    }

    @Override
    public RequestHandler.Result load(Request request, int networkPolicy) throws IOException {
        Resources res = Utils.getResources(this.context, request);
        int id = Utils.getResourceId(res, request);
        return new RequestHandler.Result(ResourceRequestHandler.decodeResource(res, id, request), Picasso.LoadedFrom.DISK);
    }

    private static Bitmap decodeResource(Resources resources, int id, Request data) {
        BitmapFactory.Options options = ResourceRequestHandler.createBitmapOptions(data);
        if (ResourceRequestHandler.requiresInSampleSize(options)) {
            BitmapFactory.decodeResource((Resources)resources, (int)id, (BitmapFactory.Options)options);
            ResourceRequestHandler.calculateInSampleSize(data.targetWidth, data.targetHeight, options, data);
        }
        return BitmapFactory.decodeResource((Resources)resources, (int)id, (BitmapFactory.Options)options);
    }
}


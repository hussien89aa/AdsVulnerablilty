/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.content.res.AssetManager
 *  android.net.Uri
 */
package com.squareup.picasso;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

class AssetRequestHandler
extends RequestHandler {
    protected static final String ANDROID_ASSET = "android_asset";
    private static final int ASSET_PREFIX_LENGTH = "file:///android_asset/".length();
    private final AssetManager assetManager;

    public AssetRequestHandler(Context context) {
        this.assetManager = context.getAssets();
    }

    @Override
    public boolean canHandleRequest(Request data) {
        Uri uri = data.uri;
        return "file".equals(uri.getScheme()) && !uri.getPathSegments().isEmpty() && "android_asset".equals(uri.getPathSegments().get(0));
    }

    @Override
    public RequestHandler.Result load(Request request, int networkPolicy) throws IOException {
        InputStream is = this.assetManager.open(AssetRequestHandler.getFilePath(request));
        return new RequestHandler.Result(is, Picasso.LoadedFrom.DISK);
    }

    static String getFilePath(Request request) {
        return request.uri.toString().substring(ASSET_PREFIX_LENGTH);
    }
}


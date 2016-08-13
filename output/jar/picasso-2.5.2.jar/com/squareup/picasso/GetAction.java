/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.graphics.Bitmap
 *  android.graphics.drawable.Drawable
 */
package com.squareup.picasso;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import com.squareup.picasso.Action;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;

class GetAction
extends Action<Void> {
    GetAction(Picasso picasso, Request data, int memoryPolicy, int networkPolicy, Object tag, String key) {
        super(picasso, null, data, memoryPolicy, networkPolicy, 0, null, key, tag, false);
    }

    @Override
    void complete(Bitmap result, Picasso.LoadedFrom from) {
    }

    @Override
    public void error() {
    }
}


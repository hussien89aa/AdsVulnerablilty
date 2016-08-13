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
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;

class FetchAction
extends Action<Object> {
    private final Object target = new Object();
    private Callback callback;

    FetchAction(Picasso picasso, Request data, int memoryPolicy, int networkPolicy, Object tag, String key, Callback callback) {
        super(picasso, null, data, memoryPolicy, networkPolicy, 0, null, key, tag, false);
        this.callback = callback;
    }

    @Override
    void complete(Bitmap result, Picasso.LoadedFrom from) {
        if (this.callback != null) {
            this.callback.onSuccess();
        }
    }

    @Override
    void error() {
        if (this.callback != null) {
            this.callback.onError();
        }
    }

    @Override
    void cancel() {
        super.cancel();
        this.callback = null;
    }

    @Override
    Object getTarget() {
        return this.target;
    }
}


/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.graphics.Bitmap
 *  android.graphics.drawable.Drawable
 *  android.widget.ImageView
 */
package com.squareup.picasso;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import com.squareup.picasso.Action;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.PicassoDrawable;
import com.squareup.picasso.Request;
import java.lang.ref.WeakReference;

class ImageViewAction
extends Action<ImageView> {
    Callback callback;

    ImageViewAction(Picasso picasso, ImageView imageView, Request data, int memoryPolicy, int networkPolicy, int errorResId, Drawable errorDrawable, String key, Object tag, Callback callback, boolean noFade) {
        super(picasso, imageView, data, memoryPolicy, networkPolicy, errorResId, errorDrawable, key, tag, noFade);
        this.callback = callback;
    }

    @Override
    public void complete(Bitmap result, Picasso.LoadedFrom from) {
        if (result == null) {
            throw new AssertionError((Object)String.format("Attempted to complete action with no result!\n%s", this));
        }
        ImageView target = (ImageView)this.target.get();
        if (target == null) {
            return;
        }
        Context context = this.picasso.context;
        boolean indicatorsEnabled = this.picasso.indicatorsEnabled;
        PicassoDrawable.setBitmap(target, context, result, from, this.noFade, indicatorsEnabled);
        if (this.callback != null) {
            this.callback.onSuccess();
        }
    }

    @Override
    public void error() {
        ImageView target = (ImageView)this.target.get();
        if (target == null) {
            return;
        }
        if (this.errorResId != 0) {
            target.setImageResource(this.errorResId);
        } else if (this.errorDrawable != null) {
            target.setImageDrawable(this.errorDrawable);
        }
        if (this.callback != null) {
            this.callback.onError();
        }
    }

    @Override
    void cancel() {
        super.cancel();
        if (this.callback != null) {
            this.callback = null;
        }
    }
}


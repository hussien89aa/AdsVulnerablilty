/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.content.res.Resources
 *  android.graphics.Bitmap
 *  android.graphics.drawable.Drawable
 */
package com.squareup.picasso;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import com.squareup.picasso.Action;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.Target;

final class TargetAction
extends Action<Target> {
    TargetAction(Picasso picasso, Target target, Request data, int memoryPolicy, int networkPolicy, Drawable errorDrawable, String key, Object tag, int errorResId) {
        super(picasso, target, data, memoryPolicy, networkPolicy, errorResId, errorDrawable, key, tag, false);
    }

    @Override
    void complete(Bitmap result, Picasso.LoadedFrom from) {
        if (result == null) {
            throw new AssertionError((Object)String.format("Attempted to complete action with no result!\n%s", this));
        }
        Target target = (Target)this.getTarget();
        if (target != null) {
            target.onBitmapLoaded(result, from);
            if (result.isRecycled()) {
                throw new IllegalStateException("Target callback must not recycle bitmap!");
            }
        }
    }

    @Override
    void error() {
        Target target = (Target)this.getTarget();
        if (target != null) {
            if (this.errorResId != 0) {
                target.onBitmapFailed(this.picasso.context.getResources().getDrawable(this.errorResId));
            } else {
                target.onBitmapFailed(this.errorDrawable);
            }
        }
    }
}


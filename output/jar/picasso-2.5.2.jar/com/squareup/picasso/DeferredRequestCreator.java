/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.view.ViewTreeObserver
 *  android.view.ViewTreeObserver$OnPreDrawListener
 *  android.widget.ImageView
 */
package com.squareup.picasso;

import android.view.ViewTreeObserver;
import android.widget.ImageView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.RequestCreator;
import java.lang.ref.WeakReference;

class DeferredRequestCreator
implements ViewTreeObserver.OnPreDrawListener {
    final RequestCreator creator;
    final WeakReference<ImageView> target;
    Callback callback;

    DeferredRequestCreator(RequestCreator creator, ImageView target) {
        this(creator, target, null);
    }

    DeferredRequestCreator(RequestCreator creator, ImageView target, Callback callback) {
        this.creator = creator;
        this.target = new WeakReference<ImageView>(target);
        this.callback = callback;
        target.getViewTreeObserver().addOnPreDrawListener((ViewTreeObserver.OnPreDrawListener)this);
    }

    public boolean onPreDraw() {
        ImageView target = this.target.get();
        if (target == null) {
            return true;
        }
        ViewTreeObserver vto = target.getViewTreeObserver();
        if (!vto.isAlive()) {
            return true;
        }
        int width = target.getWidth();
        int height = target.getHeight();
        if (width <= 0 || height <= 0) {
            return true;
        }
        vto.removeOnPreDrawListener((ViewTreeObserver.OnPreDrawListener)this);
        this.creator.unfit().resize(width, height).into(target, this.callback);
        return true;
    }

    void cancel() {
        this.callback = null;
        ImageView target = this.target.get();
        if (target == null) {
            return;
        }
        ViewTreeObserver vto = target.getViewTreeObserver();
        if (!vto.isAlive()) {
            return;
        }
        vto.removeOnPreDrawListener((ViewTreeObserver.OnPreDrawListener)this);
    }
}


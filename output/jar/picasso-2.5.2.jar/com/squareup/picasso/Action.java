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
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

abstract class Action<T> {
    final Picasso picasso;
    final Request request;
    final WeakReference<T> target;
    final boolean noFade;
    final int memoryPolicy;
    final int networkPolicy;
    final int errorResId;
    final Drawable errorDrawable;
    final String key;
    final Object tag;
    boolean willReplay;
    boolean cancelled;

    Action(Picasso picasso, T target, Request request, int memoryPolicy, int networkPolicy, int errorResId, Drawable errorDrawable, String key, Object tag, boolean noFade) {
        this.picasso = picasso;
        this.request = request;
        this.target = target == null ? null : new RequestWeakReference<Object>((Action)this, target, picasso.referenceQueue);
        this.memoryPolicy = memoryPolicy;
        this.networkPolicy = networkPolicy;
        this.noFade = noFade;
        this.errorResId = errorResId;
        this.errorDrawable = errorDrawable;
        this.key = key;
        this.tag = tag != null ? tag : this;
    }

    abstract void complete(Bitmap var1, Picasso.LoadedFrom var2);

    abstract void error();

    void cancel() {
        this.cancelled = true;
    }

    Request getRequest() {
        return this.request;
    }

    T getTarget() {
        return this.target == null ? null : (T)this.target.get();
    }

    String getKey() {
        return this.key;
    }

    boolean isCancelled() {
        return this.cancelled;
    }

    boolean willReplay() {
        return this.willReplay;
    }

    int getMemoryPolicy() {
        return this.memoryPolicy;
    }

    int getNetworkPolicy() {
        return this.networkPolicy;
    }

    Picasso getPicasso() {
        return this.picasso;
    }

    Picasso.Priority getPriority() {
        return this.request.priority;
    }

    Object getTag() {
        return this.tag;
    }

    static class RequestWeakReference<M>
    extends WeakReference<M> {
        final Action action;

        public RequestWeakReference(Action action, M referent, ReferenceQueue<? super M> q) {
            super(referent, q);
            this.action = action;
        }
    }

}


/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.app.Notification
 *  android.content.Context
 *  android.content.res.Resources
 *  android.graphics.Bitmap
 *  android.graphics.Bitmap$Config
 *  android.graphics.drawable.Drawable
 *  android.net.Uri
 *  android.widget.ImageView
 *  android.widget.RemoteViews
 */
package com.squareup.picasso;

import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.RemoteViews;
import com.squareup.picasso.Action;
import com.squareup.picasso.BitmapHunter;
import com.squareup.picasso.Cache;
import com.squareup.picasso.Callback;
import com.squareup.picasso.DeferredRequestCreator;
import com.squareup.picasso.Dispatcher;
import com.squareup.picasso.FetchAction;
import com.squareup.picasso.GetAction;
import com.squareup.picasso.ImageViewAction;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.PicassoDrawable;
import com.squareup.picasso.RemoteViewsAction;
import com.squareup.picasso.Request;
import com.squareup.picasso.Stats;
import com.squareup.picasso.Target;
import com.squareup.picasso.TargetAction;
import com.squareup.picasso.Transformation;
import com.squareup.picasso.Utils;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RequestCreator {
    private static final AtomicInteger nextId = new AtomicInteger();
    private final Picasso picasso;
    private final Request.Builder data;
    private boolean noFade;
    private boolean deferred;
    private boolean setPlaceholder = true;
    private int placeholderResId;
    private int errorResId;
    private int memoryPolicy;
    private int networkPolicy;
    private Drawable placeholderDrawable;
    private Drawable errorDrawable;
    private Object tag;

    RequestCreator(Picasso picasso, Uri uri, int resourceId) {
        if (picasso.shutdown) {
            throw new IllegalStateException("Picasso instance already shut down. Cannot submit new requests.");
        }
        this.picasso = picasso;
        this.data = new Request.Builder(uri, resourceId, picasso.defaultBitmapConfig);
    }

    RequestCreator() {
        this.picasso = null;
        this.data = new Request.Builder(null, 0, null);
    }

    public RequestCreator noPlaceholder() {
        if (this.placeholderResId != 0) {
            throw new IllegalStateException("Placeholder resource already set.");
        }
        if (this.placeholderDrawable != null) {
            throw new IllegalStateException("Placeholder image already set.");
        }
        this.setPlaceholder = false;
        return this;
    }

    public RequestCreator placeholder(int placeholderResId) {
        if (!this.setPlaceholder) {
            throw new IllegalStateException("Already explicitly declared as no placeholder.");
        }
        if (placeholderResId == 0) {
            throw new IllegalArgumentException("Placeholder image resource invalid.");
        }
        if (this.placeholderDrawable != null) {
            throw new IllegalStateException("Placeholder image already set.");
        }
        this.placeholderResId = placeholderResId;
        return this;
    }

    public RequestCreator placeholder(Drawable placeholderDrawable) {
        if (!this.setPlaceholder) {
            throw new IllegalStateException("Already explicitly declared as no placeholder.");
        }
        if (this.placeholderResId != 0) {
            throw new IllegalStateException("Placeholder image already set.");
        }
        this.placeholderDrawable = placeholderDrawable;
        return this;
    }

    public RequestCreator error(int errorResId) {
        if (errorResId == 0) {
            throw new IllegalArgumentException("Error image resource invalid.");
        }
        if (this.errorDrawable != null) {
            throw new IllegalStateException("Error image already set.");
        }
        this.errorResId = errorResId;
        return this;
    }

    public RequestCreator error(Drawable errorDrawable) {
        if (errorDrawable == null) {
            throw new IllegalArgumentException("Error image may not be null.");
        }
        if (this.errorResId != 0) {
            throw new IllegalStateException("Error image already set.");
        }
        this.errorDrawable = errorDrawable;
        return this;
    }

    public RequestCreator tag(Object tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag invalid.");
        }
        if (this.tag != null) {
            throw new IllegalStateException("Tag already set.");
        }
        this.tag = tag;
        return this;
    }

    public RequestCreator fit() {
        this.deferred = true;
        return this;
    }

    RequestCreator unfit() {
        this.deferred = false;
        return this;
    }

    public RequestCreator resizeDimen(int targetWidthResId, int targetHeightResId) {
        Resources resources = this.picasso.context.getResources();
        int targetWidth = resources.getDimensionPixelSize(targetWidthResId);
        int targetHeight = resources.getDimensionPixelSize(targetHeightResId);
        return this.resize(targetWidth, targetHeight);
    }

    public RequestCreator resize(int targetWidth, int targetHeight) {
        this.data.resize(targetWidth, targetHeight);
        return this;
    }

    public RequestCreator centerCrop() {
        this.data.centerCrop();
        return this;
    }

    public RequestCreator centerInside() {
        this.data.centerInside();
        return this;
    }

    public RequestCreator onlyScaleDown() {
        this.data.onlyScaleDown();
        return this;
    }

    public RequestCreator rotate(float degrees) {
        this.data.rotate(degrees);
        return this;
    }

    public RequestCreator rotate(float degrees, float pivotX, float pivotY) {
        this.data.rotate(degrees, pivotX, pivotY);
        return this;
    }

    public RequestCreator config(Bitmap.Config config) {
        this.data.config(config);
        return this;
    }

    public RequestCreator stableKey(String stableKey) {
        this.data.stableKey(stableKey);
        return this;
    }

    public RequestCreator priority(Picasso.Priority priority) {
        this.data.priority(priority);
        return this;
    }

    public RequestCreator transform(Transformation transformation) {
        this.data.transform(transformation);
        return this;
    }

    public RequestCreator transform(List<? extends Transformation> transformations) {
        this.data.transform(transformations);
        return this;
    }

    @Deprecated
    public RequestCreator skipMemoryCache() {
        return this.memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE);
    }

    public /* varargs */ RequestCreator memoryPolicy(MemoryPolicy policy, MemoryPolicy ... additional) {
        if (policy == null) {
            throw new IllegalArgumentException("Memory policy cannot be null.");
        }
        this.memoryPolicy |= policy.index;
        if (additional == null) {
            throw new IllegalArgumentException("Memory policy cannot be null.");
        }
        if (additional.length > 0) {
            for (MemoryPolicy memoryPolicy : additional) {
                if (memoryPolicy == null) {
                    throw new IllegalArgumentException("Memory policy cannot be null.");
                }
                this.memoryPolicy |= memoryPolicy.index;
            }
        }
        return this;
    }

    public /* varargs */ RequestCreator networkPolicy(NetworkPolicy policy, NetworkPolicy ... additional) {
        if (policy == null) {
            throw new IllegalArgumentException("Network policy cannot be null.");
        }
        this.networkPolicy |= policy.index;
        if (additional == null) {
            throw new IllegalArgumentException("Network policy cannot be null.");
        }
        if (additional.length > 0) {
            for (NetworkPolicy networkPolicy : additional) {
                if (networkPolicy == null) {
                    throw new IllegalArgumentException("Network policy cannot be null.");
                }
                this.networkPolicy |= networkPolicy.index;
            }
        }
        return this;
    }

    public RequestCreator noFade() {
        this.noFade = true;
        return this;
    }

    public Bitmap get() throws IOException {
        long started = System.nanoTime();
        Utils.checkNotMain();
        if (this.deferred) {
            throw new IllegalStateException("Fit cannot be used with get.");
        }
        if (!this.data.hasImage()) {
            return null;
        }
        Request finalData = this.createRequest(started);
        String key = Utils.createKey(finalData, new StringBuilder());
        GetAction action = new GetAction(this.picasso, finalData, this.memoryPolicy, this.networkPolicy, this.tag, key);
        return BitmapHunter.forRequest(this.picasso, this.picasso.dispatcher, this.picasso.cache, this.picasso.stats, action).hunt();
    }

    public void fetch() {
        this.fetch(null);
    }

    public void fetch(Callback callback) {
        long started = System.nanoTime();
        if (this.deferred) {
            throw new IllegalStateException("Fit cannot be used with fetch.");
        }
        if (this.data.hasImage()) {
            Request request;
            Bitmap bitmap;
            String key;
            if (!this.data.hasPriority()) {
                this.data.priority(Picasso.Priority.LOW);
            }
            if ((bitmap = this.picasso.quickMemoryCacheCheck(key = Utils.createKey(request = this.createRequest(started), new StringBuilder()))) != null) {
                if (this.picasso.loggingEnabled) {
                    Utils.log("Main", "completed", request.plainId(), "from " + (Object)((Object)Picasso.LoadedFrom.MEMORY));
                }
                if (callback != null) {
                    callback.onSuccess();
                }
            } else {
                FetchAction action = new FetchAction(this.picasso, request, this.memoryPolicy, this.networkPolicy, this.tag, key, callback);
                this.picasso.submit(action);
            }
        }
    }

    public void into(Target target) {
        Bitmap bitmap;
        long started = System.nanoTime();
        Utils.checkMain();
        if (target == null) {
            throw new IllegalArgumentException("Target must not be null.");
        }
        if (this.deferred) {
            throw new IllegalStateException("Fit cannot be used with a Target.");
        }
        if (!this.data.hasImage()) {
            this.picasso.cancelRequest(target);
            target.onPrepareLoad(this.setPlaceholder ? this.getPlaceholderDrawable() : null);
            return;
        }
        Request request = this.createRequest(started);
        String requestKey = Utils.createKey(request);
        if (MemoryPolicy.shouldReadFromMemoryCache(this.memoryPolicy) && (bitmap = this.picasso.quickMemoryCacheCheck(requestKey)) != null) {
            this.picasso.cancelRequest(target);
            target.onBitmapLoaded(bitmap, Picasso.LoadedFrom.MEMORY);
            return;
        }
        target.onPrepareLoad(this.setPlaceholder ? this.getPlaceholderDrawable() : null);
        TargetAction action = new TargetAction(this.picasso, target, request, this.memoryPolicy, this.networkPolicy, this.errorDrawable, requestKey, this.tag, this.errorResId);
        this.picasso.enqueueAndSubmit(action);
    }

    public void into(RemoteViews remoteViews, int viewId, int notificationId, Notification notification) {
        long started = System.nanoTime();
        if (remoteViews == null) {
            throw new IllegalArgumentException("RemoteViews must not be null.");
        }
        if (notification == null) {
            throw new IllegalArgumentException("Notification must not be null.");
        }
        if (this.deferred) {
            throw new IllegalStateException("Fit cannot be used with RemoteViews.");
        }
        if (this.placeholderDrawable != null || this.placeholderResId != 0 || this.errorDrawable != null) {
            throw new IllegalArgumentException("Cannot use placeholder or error drawables with remote views.");
        }
        Request request = this.createRequest(started);
        String key = Utils.createKey(request, new StringBuilder());
        RemoteViewsAction.NotificationAction action = new RemoteViewsAction.NotificationAction(this.picasso, request, remoteViews, viewId, notificationId, notification, this.memoryPolicy, this.networkPolicy, key, this.tag, this.errorResId);
        this.performRemoteViewInto(action);
    }

    public void into(RemoteViews remoteViews, int viewId, int[] appWidgetIds) {
        long started = System.nanoTime();
        if (remoteViews == null) {
            throw new IllegalArgumentException("remoteViews must not be null.");
        }
        if (appWidgetIds == null) {
            throw new IllegalArgumentException("appWidgetIds must not be null.");
        }
        if (this.deferred) {
            throw new IllegalStateException("Fit cannot be used with remote views.");
        }
        if (this.placeholderDrawable != null || this.placeholderResId != 0 || this.errorDrawable != null) {
            throw new IllegalArgumentException("Cannot use placeholder or error drawables with remote views.");
        }
        Request request = this.createRequest(started);
        String key = Utils.createKey(request, new StringBuilder());
        RemoteViewsAction.AppWidgetAction action = new RemoteViewsAction.AppWidgetAction(this.picasso, request, remoteViews, viewId, appWidgetIds, this.memoryPolicy, this.networkPolicy, key, this.tag, this.errorResId);
        this.performRemoteViewInto(action);
    }

    public void into(ImageView target) {
        this.into(target, null);
    }

    public void into(ImageView target, Callback callback) {
        Bitmap bitmap;
        long started = System.nanoTime();
        Utils.checkMain();
        if (target == null) {
            throw new IllegalArgumentException("Target must not be null.");
        }
        if (!this.data.hasImage()) {
            this.picasso.cancelRequest(target);
            if (this.setPlaceholder) {
                PicassoDrawable.setPlaceholder(target, this.getPlaceholderDrawable());
            }
            return;
        }
        if (this.deferred) {
            if (this.data.hasSize()) {
                throw new IllegalStateException("Fit cannot be used with resize.");
            }
            int width = target.getWidth();
            int height = target.getHeight();
            if (width == 0 || height == 0) {
                if (this.setPlaceholder) {
                    PicassoDrawable.setPlaceholder(target, this.getPlaceholderDrawable());
                }
                this.picasso.defer(target, new DeferredRequestCreator(this, target, callback));
                return;
            }
            this.data.resize(width, height);
        }
        Request request = this.createRequest(started);
        String requestKey = Utils.createKey(request);
        if (MemoryPolicy.shouldReadFromMemoryCache(this.memoryPolicy) && (bitmap = this.picasso.quickMemoryCacheCheck(requestKey)) != null) {
            this.picasso.cancelRequest(target);
            PicassoDrawable.setBitmap(target, this.picasso.context, bitmap, Picasso.LoadedFrom.MEMORY, this.noFade, this.picasso.indicatorsEnabled);
            if (this.picasso.loggingEnabled) {
                Utils.log("Main", "completed", request.plainId(), "from " + (Object)((Object)Picasso.LoadedFrom.MEMORY));
            }
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }
        if (this.setPlaceholder) {
            PicassoDrawable.setPlaceholder(target, this.getPlaceholderDrawable());
        }
        ImageViewAction action = new ImageViewAction(this.picasso, target, request, this.memoryPolicy, this.networkPolicy, this.errorResId, this.errorDrawable, requestKey, this.tag, callback, this.noFade);
        this.picasso.enqueueAndSubmit(action);
    }

    private Drawable getPlaceholderDrawable() {
        if (this.placeholderResId != 0) {
            return this.picasso.context.getResources().getDrawable(this.placeholderResId);
        }
        return this.placeholderDrawable;
    }

    private Request createRequest(long started) {
        Request transformed;
        int id = nextId.getAndIncrement();
        Request request = this.data.build();
        request.id = id;
        request.started = started;
        boolean loggingEnabled = this.picasso.loggingEnabled;
        if (loggingEnabled) {
            Utils.log("Main", "created", request.plainId(), request.toString());
        }
        if ((transformed = this.picasso.transformRequest(request)) != request) {
            transformed.id = id;
            transformed.started = started;
            if (loggingEnabled) {
                Utils.log("Main", "changed", transformed.logId(), "into " + transformed);
            }
        }
        return transformed;
    }

    private void performRemoteViewInto(RemoteViewsAction action) {
        Bitmap bitmap;
        if (MemoryPolicy.shouldReadFromMemoryCache(this.memoryPolicy) && (bitmap = this.picasso.quickMemoryCacheCheck(action.getKey())) != null) {
            action.complete(bitmap, Picasso.LoadedFrom.MEMORY);
            return;
        }
        if (this.placeholderResId != 0) {
            action.setImageResource(this.placeholderResId);
        }
        this.picasso.enqueueAndSubmit(action);
    }
}


/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.graphics.Bitmap
 *  android.graphics.Bitmap$Config
 *  android.net.Uri
 *  android.os.Handler
 *  android.os.Looper
 *  android.os.Message
 *  android.os.Process
 *  android.widget.ImageView
 *  android.widget.RemoteViews
 */
package com.squareup.picasso;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.widget.ImageView;
import android.widget.RemoteViews;
import com.squareup.picasso.Action;
import com.squareup.picasso.AssetRequestHandler;
import com.squareup.picasso.BitmapHunter;
import com.squareup.picasso.Cache;
import com.squareup.picasso.ContactsPhotoRequestHandler;
import com.squareup.picasso.ContentStreamRequestHandler;
import com.squareup.picasso.DeferredRequestCreator;
import com.squareup.picasso.Dispatcher;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.FileRequestHandler;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.MediaStoreRequestHandler;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkRequestHandler;
import com.squareup.picasso.PicassoExecutorService;
import com.squareup.picasso.RemoteViewsAction;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.RequestHandler;
import com.squareup.picasso.ResourceRequestHandler;
import com.squareup.picasso.Stats;
import com.squareup.picasso.StatsSnapshot;
import com.squareup.picasso.Target;
import com.squareup.picasso.Utils;
import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;

public class Picasso {
    static final String TAG = "Picasso";
    static final Handler HANDLER = new Handler(Looper.getMainLooper()){

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 8: {
                    List batch = (List)msg.obj;
                    int n = batch.size();
                    for (int i = 0; i < n; ++i) {
                        BitmapHunter hunter = (BitmapHunter)batch.get(i);
                        hunter.picasso.complete(hunter);
                    }
                    break;
                }
                case 3: {
                    Action action = (Action)msg.obj;
                    if (action.getPicasso().loggingEnabled) {
                        Utils.log("Main", "canceled", action.request.logId(), "target got garbage collected");
                    }
                    action.picasso.cancelExistingRequest(action.getTarget());
                    break;
                }
                case 13: {
                    List batch = (List)msg.obj;
                    int n = batch.size();
                    for (int i = 0; i < n; ++i) {
                        Action action = (Action)batch.get(i);
                        action.picasso.resumeAction(action);
                    }
                    break;
                }
                default: {
                    throw new AssertionError((Object)("Unknown handler message received: " + msg.what));
                }
            }
        }
    };
    static volatile Picasso singleton = null;
    private final Listener listener;
    private final RequestTransformer requestTransformer;
    private final CleanupThread cleanupThread;
    private final List<RequestHandler> requestHandlers;
    final Context context;
    final Dispatcher dispatcher;
    final Cache cache;
    final Stats stats;
    final Map<Object, Action> targetToAction;
    final Map<ImageView, DeferredRequestCreator> targetToDeferredRequestCreator;
    final ReferenceQueue<Object> referenceQueue;
    final Bitmap.Config defaultBitmapConfig;
    boolean indicatorsEnabled;
    volatile boolean loggingEnabled;
    boolean shutdown;

    Picasso(Context context, Dispatcher dispatcher, Cache cache, Listener listener, RequestTransformer requestTransformer, List<RequestHandler> extraRequestHandlers, Stats stats, Bitmap.Config defaultBitmapConfig, boolean indicatorsEnabled, boolean loggingEnabled) {
        this.context = context;
        this.dispatcher = dispatcher;
        this.cache = cache;
        this.listener = listener;
        this.requestTransformer = requestTransformer;
        this.defaultBitmapConfig = defaultBitmapConfig;
        int builtInHandlers = 7;
        int extraCount = extraRequestHandlers != null ? extraRequestHandlers.size() : 0;
        ArrayList<RequestHandler> allRequestHandlers = new ArrayList<RequestHandler>(builtInHandlers + extraCount);
        allRequestHandlers.add(new ResourceRequestHandler(context));
        if (extraRequestHandlers != null) {
            allRequestHandlers.addAll(extraRequestHandlers);
        }
        allRequestHandlers.add(new ContactsPhotoRequestHandler(context));
        allRequestHandlers.add(new MediaStoreRequestHandler(context));
        allRequestHandlers.add(new ContentStreamRequestHandler(context));
        allRequestHandlers.add(new AssetRequestHandler(context));
        allRequestHandlers.add(new FileRequestHandler(context));
        allRequestHandlers.add(new NetworkRequestHandler(dispatcher.downloader, stats));
        this.requestHandlers = Collections.unmodifiableList(allRequestHandlers);
        this.stats = stats;
        this.targetToAction = new WeakHashMap<Object, Action>();
        this.targetToDeferredRequestCreator = new WeakHashMap<ImageView, DeferredRequestCreator>();
        this.indicatorsEnabled = indicatorsEnabled;
        this.loggingEnabled = loggingEnabled;
        this.referenceQueue = new ReferenceQueue();
        this.cleanupThread = new CleanupThread(this.referenceQueue, HANDLER);
        this.cleanupThread.start();
    }

    public void cancelRequest(ImageView view) {
        this.cancelExistingRequest((Object)view);
    }

    public void cancelRequest(Target target) {
        this.cancelExistingRequest(target);
    }

    public void cancelRequest(RemoteViews remoteViews, int viewId) {
        this.cancelExistingRequest(new RemoteViewsAction.RemoteViewsTarget(remoteViews, viewId));
    }

    public void cancelTag(Object tag) {
        Utils.checkMain();
        ArrayList<Action> actions = new ArrayList<Action>(this.targetToAction.values());
        int n = actions.size();
        for (int i = 0; i < n; ++i) {
            Action action = actions.get(i);
            if (!action.getTag().equals(tag)) continue;
            this.cancelExistingRequest(action.getTarget());
        }
    }

    public void pauseTag(Object tag) {
        this.dispatcher.dispatchPauseTag(tag);
    }

    public void resumeTag(Object tag) {
        this.dispatcher.dispatchResumeTag(tag);
    }

    public RequestCreator load(Uri uri) {
        return new RequestCreator(this, uri, 0);
    }

    public RequestCreator load(String path) {
        if (path == null) {
            return new RequestCreator(this, null, 0);
        }
        if (path.trim().length() == 0) {
            throw new IllegalArgumentException("Path must not be empty.");
        }
        return this.load(Uri.parse((String)path));
    }

    public RequestCreator load(File file) {
        if (file == null) {
            return new RequestCreator(this, null, 0);
        }
        return this.load(Uri.fromFile((File)file));
    }

    public RequestCreator load(int resourceId) {
        if (resourceId == 0) {
            throw new IllegalArgumentException("Resource ID must not be zero.");
        }
        return new RequestCreator(this, null, resourceId);
    }

    public void invalidate(Uri uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri == null");
        }
        this.cache.clearKeyUri(uri.toString());
    }

    public void invalidate(String path) {
        if (path == null) {
            throw new IllegalArgumentException("path == null");
        }
        this.invalidate(Uri.parse((String)path));
    }

    public void invalidate(File file) {
        if (file == null) {
            throw new IllegalArgumentException("file == null");
        }
        this.invalidate(Uri.fromFile((File)file));
    }

    @Deprecated
    public boolean isDebugging() {
        return this.areIndicatorsEnabled() && this.isLoggingEnabled();
    }

    @Deprecated
    public void setDebugging(boolean debugging) {
        this.setIndicatorsEnabled(debugging);
    }

    public void setIndicatorsEnabled(boolean enabled) {
        this.indicatorsEnabled = enabled;
    }

    public boolean areIndicatorsEnabled() {
        return this.indicatorsEnabled;
    }

    public void setLoggingEnabled(boolean enabled) {
        this.loggingEnabled = enabled;
    }

    public boolean isLoggingEnabled() {
        return this.loggingEnabled;
    }

    public StatsSnapshot getSnapshot() {
        return this.stats.createSnapshot();
    }

    public void shutdown() {
        if (this == singleton) {
            throw new UnsupportedOperationException("Default singleton instance cannot be shutdown.");
        }
        if (this.shutdown) {
            return;
        }
        this.cache.clear();
        this.cleanupThread.shutdown();
        this.stats.shutdown();
        this.dispatcher.shutdown();
        for (DeferredRequestCreator deferredRequestCreator : this.targetToDeferredRequestCreator.values()) {
            deferredRequestCreator.cancel();
        }
        this.targetToDeferredRequestCreator.clear();
        this.shutdown = true;
    }

    List<RequestHandler> getRequestHandlers() {
        return this.requestHandlers;
    }

    Request transformRequest(Request request) {
        Request transformed = this.requestTransformer.transformRequest(request);
        if (transformed == null) {
            throw new IllegalStateException("Request transformer " + this.requestTransformer.getClass().getCanonicalName() + " returned null for " + request);
        }
        return transformed;
    }

    void defer(ImageView view, DeferredRequestCreator request) {
        this.targetToDeferredRequestCreator.put(view, request);
    }

    void enqueueAndSubmit(Action action) {
        Object target = action.getTarget();
        if (target != null && this.targetToAction.get(target) != action) {
            this.cancelExistingRequest(target);
            this.targetToAction.put(target, action);
        }
        this.submit(action);
    }

    void submit(Action action) {
        this.dispatcher.dispatchSubmit(action);
    }

    Bitmap quickMemoryCacheCheck(String key) {
        Bitmap cached = this.cache.get(key);
        if (cached != null) {
            this.stats.dispatchCacheHit();
        } else {
            this.stats.dispatchCacheMiss();
        }
        return cached;
    }

    void complete(BitmapHunter hunter) {
        boolean shouldDeliver;
        Action single = hunter.getAction();
        List<Action> joined = hunter.getActions();
        boolean hasMultiple = joined != null && !joined.isEmpty();
        boolean bl = shouldDeliver = single != null || hasMultiple;
        if (!shouldDeliver) {
            return;
        }
        Uri uri = hunter.getData().uri;
        Exception exception = hunter.getException();
        Bitmap result = hunter.getResult();
        LoadedFrom from = hunter.getLoadedFrom();
        if (single != null) {
            this.deliverAction(result, from, single);
        }
        if (hasMultiple) {
            int n = joined.size();
            for (int i = 0; i < n; ++i) {
                Action join = joined.get(i);
                this.deliverAction(result, from, join);
            }
        }
        if (this.listener != null && exception != null) {
            this.listener.onImageLoadFailed(this, uri, exception);
        }
    }

    void resumeAction(Action action) {
        Bitmap bitmap = null;
        if (MemoryPolicy.shouldReadFromMemoryCache(action.memoryPolicy)) {
            bitmap = this.quickMemoryCacheCheck(action.getKey());
        }
        if (bitmap != null) {
            this.deliverAction(bitmap, LoadedFrom.MEMORY, action);
            if (this.loggingEnabled) {
                Utils.log("Main", "completed", action.request.logId(), "from " + (Object)((Object)LoadedFrom.MEMORY));
            }
        } else {
            this.enqueueAndSubmit(action);
            if (this.loggingEnabled) {
                Utils.log("Main", "resumed", action.request.logId());
            }
        }
    }

    private void deliverAction(Bitmap result, LoadedFrom from, Action action) {
        if (action.isCancelled()) {
            return;
        }
        if (!action.willReplay()) {
            this.targetToAction.remove(action.getTarget());
        }
        if (result != null) {
            if (from == null) {
                throw new AssertionError((Object)"LoadedFrom cannot be null.");
            }
            action.complete(result, from);
            if (this.loggingEnabled) {
                Utils.log("Main", "completed", action.request.logId(), "from " + (Object)((Object)from));
            }
        } else {
            action.error();
            if (this.loggingEnabled) {
                Utils.log("Main", "errored", action.request.logId());
            }
        }
    }

    private void cancelExistingRequest(Object target) {
        ImageView targetImageView;
        DeferredRequestCreator deferredRequestCreator;
        Utils.checkMain();
        Action action = this.targetToAction.remove(target);
        if (action != null) {
            action.cancel();
            this.dispatcher.dispatchCancel(action);
        }
        if (target instanceof ImageView && (deferredRequestCreator = this.targetToDeferredRequestCreator.remove((Object)(targetImageView = (ImageView)target))) != null) {
            deferredRequestCreator.cancel();
        }
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static Picasso with(Context context) {
        if (singleton != null) return singleton;
        reference var1_1 = Picasso.class;
        synchronized (Picasso.class) {
            if (singleton != null) return singleton;
            {
                singleton = new Builder(context).build();
            }
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return singleton;
        }
    }

    public static void setSingletonInstance(Picasso picasso) {
        reference var1_1 = Picasso.class;
        synchronized (Picasso.class) {
            if (singleton != null) {
                throw new IllegalStateException("Singleton instance already exists.");
            }
            singleton = picasso;
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return;
        }
    }

    public static enum LoadedFrom {
        MEMORY(-16711936),
        DISK(-16776961),
        NETWORK(-65536);
        
        final int debugColor;

        private LoadedFrom(int debugColor) {
            this.debugColor = debugColor;
        }
    }

    public static class Builder {
        private final Context context;
        private Downloader downloader;
        private ExecutorService service;
        private Cache cache;
        private Listener listener;
        private RequestTransformer transformer;
        private List<RequestHandler> requestHandlers;
        private Bitmap.Config defaultBitmapConfig;
        private boolean indicatorsEnabled;
        private boolean loggingEnabled;

        public Builder(Context context) {
            if (context == null) {
                throw new IllegalArgumentException("Context must not be null.");
            }
            this.context = context.getApplicationContext();
        }

        public Builder defaultBitmapConfig(Bitmap.Config bitmapConfig) {
            if (bitmapConfig == null) {
                throw new IllegalArgumentException("Bitmap config must not be null.");
            }
            this.defaultBitmapConfig = bitmapConfig;
            return this;
        }

        public Builder downloader(Downloader downloader) {
            if (downloader == null) {
                throw new IllegalArgumentException("Downloader must not be null.");
            }
            if (this.downloader != null) {
                throw new IllegalStateException("Downloader already set.");
            }
            this.downloader = downloader;
            return this;
        }

        public Builder executor(ExecutorService executorService) {
            if (executorService == null) {
                throw new IllegalArgumentException("Executor service must not be null.");
            }
            if (this.service != null) {
                throw new IllegalStateException("Executor service already set.");
            }
            this.service = executorService;
            return this;
        }

        public Builder memoryCache(Cache memoryCache) {
            if (memoryCache == null) {
                throw new IllegalArgumentException("Memory cache must not be null.");
            }
            if (this.cache != null) {
                throw new IllegalStateException("Memory cache already set.");
            }
            this.cache = memoryCache;
            return this;
        }

        public Builder listener(Listener listener) {
            if (listener == null) {
                throw new IllegalArgumentException("Listener must not be null.");
            }
            if (this.listener != null) {
                throw new IllegalStateException("Listener already set.");
            }
            this.listener = listener;
            return this;
        }

        public Builder requestTransformer(RequestTransformer transformer) {
            if (transformer == null) {
                throw new IllegalArgumentException("Transformer must not be null.");
            }
            if (this.transformer != null) {
                throw new IllegalStateException("Transformer already set.");
            }
            this.transformer = transformer;
            return this;
        }

        public Builder addRequestHandler(RequestHandler requestHandler) {
            if (requestHandler == null) {
                throw new IllegalArgumentException("RequestHandler must not be null.");
            }
            if (this.requestHandlers == null) {
                this.requestHandlers = new ArrayList<RequestHandler>();
            }
            if (this.requestHandlers.contains(requestHandler)) {
                throw new IllegalStateException("RequestHandler already registered.");
            }
            this.requestHandlers.add(requestHandler);
            return this;
        }

        @Deprecated
        public Builder debugging(boolean debugging) {
            return this.indicatorsEnabled(debugging);
        }

        public Builder indicatorsEnabled(boolean enabled) {
            this.indicatorsEnabled = enabled;
            return this;
        }

        public Builder loggingEnabled(boolean enabled) {
            this.loggingEnabled = enabled;
            return this;
        }

        public Picasso build() {
            Context context = this.context;
            if (this.downloader == null) {
                this.downloader = Utils.createDefaultDownloader(context);
            }
            if (this.cache == null) {
                this.cache = new LruCache(context);
            }
            if (this.service == null) {
                this.service = new PicassoExecutorService();
            }
            if (this.transformer == null) {
                this.transformer = RequestTransformer.IDENTITY;
            }
            Stats stats = new Stats(this.cache);
            Dispatcher dispatcher = new Dispatcher(context, this.service, Picasso.HANDLER, this.downloader, this.cache, stats);
            return new Picasso(context, dispatcher, this.cache, this.listener, this.transformer, this.requestHandlers, stats, this.defaultBitmapConfig, this.indicatorsEnabled, this.loggingEnabled);
        }
    }

    private static class CleanupThread
    extends Thread {
        private final ReferenceQueue<Object> referenceQueue;
        private final Handler handler;

        CleanupThread(ReferenceQueue<Object> referenceQueue, Handler handler) {
            this.referenceQueue = referenceQueue;
            this.handler = handler;
            this.setDaemon(true);
            this.setName("Picasso-refQueue");
        }

        @Override
        public void run() {
            Process.setThreadPriority((int)10);
            try {
                do {
                    Action.RequestWeakReference remove = (Action.RequestWeakReference)this.referenceQueue.remove(1000);
                    Message message = this.handler.obtainMessage();
                    if (remove != null) {
                        message.what = 3;
                        message.obj = remove.action;
                        this.handler.sendMessage(message);
                        continue;
                    }
                    message.recycle();
                } while (true);
            }
            catch (InterruptedException e) {
            }
            catch (Exception e) {
                this.handler.post(new Runnable(){

                    @Override
                    public void run() {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        void shutdown() {
            this.interrupt();
        }

    }

    public static enum Priority {
        LOW,
        NORMAL,
        HIGH;
        

        private Priority() {
        }
    }

    public static interface RequestTransformer {
        public static final RequestTransformer IDENTITY = new RequestTransformer(){

            @Override
            public Request transformRequest(Request request) {
                return request;
            }
        };

        public Request transformRequest(Request var1);

    }

    public static interface Listener {
        public void onImageLoadFailed(Picasso var1, Uri var2, Exception var3);
    }

}


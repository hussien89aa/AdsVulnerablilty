/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.content.BroadcastReceiver
 *  android.content.Context
 *  android.content.Intent
 *  android.content.IntentFilter
 *  android.graphics.Bitmap
 *  android.net.ConnectivityManager
 *  android.net.NetworkInfo
 *  android.os.Handler
 *  android.os.HandlerThread
 *  android.os.Looper
 *  android.os.Message
 */
package com.squareup.picasso;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.squareup.picasso.Action;
import com.squareup.picasso.BitmapHunter;
import com.squareup.picasso.Cache;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.NetworkRequestHandler;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.PicassoExecutorService;
import com.squareup.picasso.Request;
import com.squareup.picasso.Stats;
import com.squareup.picasso.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

class Dispatcher {
    private static final int RETRY_DELAY = 500;
    private static final int AIRPLANE_MODE_ON = 1;
    private static final int AIRPLANE_MODE_OFF = 0;
    static final int REQUEST_SUBMIT = 1;
    static final int REQUEST_CANCEL = 2;
    static final int REQUEST_GCED = 3;
    static final int HUNTER_COMPLETE = 4;
    static final int HUNTER_RETRY = 5;
    static final int HUNTER_DECODE_FAILED = 6;
    static final int HUNTER_DELAY_NEXT_BATCH = 7;
    static final int HUNTER_BATCH_COMPLETE = 8;
    static final int NETWORK_STATE_CHANGE = 9;
    static final int AIRPLANE_MODE_CHANGE = 10;
    static final int TAG_PAUSE = 11;
    static final int TAG_RESUME = 12;
    static final int REQUEST_BATCH_RESUME = 13;
    private static final String DISPATCHER_THREAD_NAME = "Dispatcher";
    private static final int BATCH_DELAY = 200;
    final DispatcherThread dispatcherThread = new DispatcherThread();
    final Context context;
    final ExecutorService service;
    final Downloader downloader;
    final Map<String, BitmapHunter> hunterMap;
    final Map<Object, Action> failedActions;
    final Map<Object, Action> pausedActions;
    final Set<Object> pausedTags;
    final Handler handler;
    final Handler mainThreadHandler;
    final Cache cache;
    final Stats stats;
    final List<BitmapHunter> batch;
    final NetworkBroadcastReceiver receiver;
    final boolean scansNetworkChanges;
    boolean airplaneMode;

    Dispatcher(Context context, ExecutorService service, Handler mainThreadHandler, Downloader downloader, Cache cache, Stats stats) {
        this.dispatcherThread.start();
        Utils.flushStackLocalLeaks(this.dispatcherThread.getLooper());
        this.context = context;
        this.service = service;
        this.hunterMap = new LinkedHashMap<String, BitmapHunter>();
        this.failedActions = new WeakHashMap<Object, Action>();
        this.pausedActions = new WeakHashMap<Object, Action>();
        this.pausedTags = new HashSet<Object>();
        this.handler = new DispatcherHandler(this.dispatcherThread.getLooper(), this);
        this.downloader = downloader;
        this.mainThreadHandler = mainThreadHandler;
        this.cache = cache;
        this.stats = stats;
        this.batch = new ArrayList<BitmapHunter>(4);
        this.airplaneMode = Utils.isAirplaneModeOn(this.context);
        this.scansNetworkChanges = Utils.hasPermission(context, "android.permission.ACCESS_NETWORK_STATE");
        this.receiver = new NetworkBroadcastReceiver(this);
        this.receiver.register();
    }

    void shutdown() {
        if (this.service instanceof PicassoExecutorService) {
            this.service.shutdown();
        }
        this.downloader.shutdown();
        this.dispatcherThread.quit();
        Picasso.HANDLER.post(new Runnable(){

            @Override
            public void run() {
                Dispatcher.this.receiver.unregister();
            }
        });
    }

    void dispatchSubmit(Action action) {
        this.handler.sendMessage(this.handler.obtainMessage(1, (Object)action));
    }

    void dispatchCancel(Action action) {
        this.handler.sendMessage(this.handler.obtainMessage(2, (Object)action));
    }

    void dispatchPauseTag(Object tag) {
        this.handler.sendMessage(this.handler.obtainMessage(11, tag));
    }

    void dispatchResumeTag(Object tag) {
        this.handler.sendMessage(this.handler.obtainMessage(12, tag));
    }

    void dispatchComplete(BitmapHunter hunter) {
        this.handler.sendMessage(this.handler.obtainMessage(4, (Object)hunter));
    }

    void dispatchRetry(BitmapHunter hunter) {
        this.handler.sendMessageDelayed(this.handler.obtainMessage(5, (Object)hunter), 500);
    }

    void dispatchFailed(BitmapHunter hunter) {
        this.handler.sendMessage(this.handler.obtainMessage(6, (Object)hunter));
    }

    void dispatchNetworkStateChange(NetworkInfo info) {
        this.handler.sendMessage(this.handler.obtainMessage(9, (Object)info));
    }

    void dispatchAirplaneModeChange(boolean airplaneMode) {
        this.handler.sendMessage(this.handler.obtainMessage(10, airplaneMode ? 1 : 0, 0));
    }

    void performSubmit(Action action) {
        this.performSubmit(action, true);
    }

    void performSubmit(Action action, boolean dismissFailed) {
        if (this.pausedTags.contains(action.getTag())) {
            this.pausedActions.put(action.getTarget(), action);
            if (action.getPicasso().loggingEnabled) {
                Utils.log("Dispatcher", "paused", action.request.logId(), "because tag '" + action.getTag() + "' is paused");
            }
            return;
        }
        BitmapHunter hunter = this.hunterMap.get(action.getKey());
        if (hunter != null) {
            hunter.attach(action);
            return;
        }
        if (this.service.isShutdown()) {
            if (action.getPicasso().loggingEnabled) {
                Utils.log("Dispatcher", "ignored", action.request.logId(), "because shut down");
            }
            return;
        }
        hunter = BitmapHunter.forRequest(action.getPicasso(), this, this.cache, this.stats, action);
        hunter.future = this.service.submit(hunter);
        this.hunterMap.put(action.getKey(), hunter);
        if (dismissFailed) {
            this.failedActions.remove(action.getTarget());
        }
        if (action.getPicasso().loggingEnabled) {
            Utils.log("Dispatcher", "enqueued", action.request.logId());
        }
    }

    void performCancel(Action action) {
        Action remove;
        String key = action.getKey();
        BitmapHunter hunter = this.hunterMap.get(key);
        if (hunter != null) {
            hunter.detach(action);
            if (hunter.cancel()) {
                this.hunterMap.remove(key);
                if (action.getPicasso().loggingEnabled) {
                    Utils.log("Dispatcher", "canceled", action.getRequest().logId());
                }
            }
        }
        if (this.pausedTags.contains(action.getTag())) {
            this.pausedActions.remove(action.getTarget());
            if (action.getPicasso().loggingEnabled) {
                Utils.log("Dispatcher", "canceled", action.getRequest().logId(), "because paused request got canceled");
            }
        }
        if ((remove = this.failedActions.remove(action.getTarget())) != null && remove.getPicasso().loggingEnabled) {
            Utils.log("Dispatcher", "canceled", remove.getRequest().logId(), "from replaying");
        }
    }

    void performPauseTag(Object tag) {
        if (!this.pausedTags.add(tag)) {
            return;
        }
        Iterator<BitmapHunter> it = this.hunterMap.values().iterator();
        while (it.hasNext()) {
            boolean hasMultiple;
            BitmapHunter hunter = it.next();
            boolean loggingEnabled = hunter.getPicasso().loggingEnabled;
            Action single = hunter.getAction();
            List<Action> joined = hunter.getActions();
            boolean bl = hasMultiple = joined != null && !joined.isEmpty();
            if (single == null && !hasMultiple) continue;
            if (single != null && single.getTag().equals(tag)) {
                hunter.detach(single);
                this.pausedActions.put(single.getTarget(), single);
                if (loggingEnabled) {
                    Utils.log("Dispatcher", "paused", single.request.logId(), "because tag '" + tag + "' was paused");
                }
            }
            if (hasMultiple) {
                for (int i = joined.size() - 1; i >= 0; --i) {
                    Action action = joined.get(i);
                    if (!action.getTag().equals(tag)) continue;
                    hunter.detach(action);
                    this.pausedActions.put(action.getTarget(), action);
                    if (!loggingEnabled) continue;
                    Utils.log("Dispatcher", "paused", action.request.logId(), "because tag '" + tag + "' was paused");
                }
            }
            if (!hunter.cancel()) continue;
            it.remove();
            if (!loggingEnabled) continue;
            Utils.log("Dispatcher", "canceled", Utils.getLogIdsForHunter(hunter), "all actions paused");
        }
    }

    void performResumeTag(Object tag) {
        if (!this.pausedTags.remove(tag)) {
            return;
        }
        ArrayList<Action> batch = null;
        Iterator<Action> i = this.pausedActions.values().iterator();
        while (i.hasNext()) {
            Action action = i.next();
            if (!action.getTag().equals(tag)) continue;
            if (batch == null) {
                batch = new ArrayList<Action>();
            }
            batch.add(action);
            i.remove();
        }
        if (batch != null) {
            this.mainThreadHandler.sendMessage(this.mainThreadHandler.obtainMessage(13, (Object)batch));
        }
    }

    void performRetry(BitmapHunter hunter) {
        if (hunter.isCancelled()) {
            return;
        }
        if (this.service.isShutdown()) {
            this.performError(hunter, false);
            return;
        }
        NetworkInfo networkInfo = null;
        if (this.scansNetworkChanges) {
            ConnectivityManager connectivityManager = (ConnectivityManager)Utils.getService(this.context, "connectivity");
            networkInfo = connectivityManager.getActiveNetworkInfo();
        }
        boolean hasConnectivity = networkInfo != null && networkInfo.isConnected();
        boolean shouldRetryHunter = hunter.shouldRetry(this.airplaneMode, networkInfo);
        boolean supportsReplay = hunter.supportsReplay();
        if (!shouldRetryHunter) {
            boolean willReplay = this.scansNetworkChanges && supportsReplay;
            this.performError(hunter, willReplay);
            if (willReplay) {
                this.markForReplay(hunter);
            }
            return;
        }
        if (!this.scansNetworkChanges || hasConnectivity) {
            if (hunter.getPicasso().loggingEnabled) {
                Utils.log("Dispatcher", "retrying", Utils.getLogIdsForHunter(hunter));
            }
            if (hunter.getException() instanceof NetworkRequestHandler.ContentLengthException) {
                hunter.networkPolicy |= NetworkPolicy.NO_CACHE.index;
            }
            hunter.future = this.service.submit(hunter);
            return;
        }
        this.performError(hunter, supportsReplay);
        if (supportsReplay) {
            this.markForReplay(hunter);
        }
    }

    void performComplete(BitmapHunter hunter) {
        if (MemoryPolicy.shouldWriteToMemoryCache(hunter.getMemoryPolicy())) {
            this.cache.set(hunter.getKey(), hunter.getResult());
        }
        this.hunterMap.remove(hunter.getKey());
        this.batch(hunter);
        if (hunter.getPicasso().loggingEnabled) {
            Utils.log("Dispatcher", "batched", Utils.getLogIdsForHunter(hunter), "for completion");
        }
    }

    void performBatchComplete() {
        ArrayList<BitmapHunter> copy = new ArrayList<BitmapHunter>(this.batch);
        this.batch.clear();
        this.mainThreadHandler.sendMessage(this.mainThreadHandler.obtainMessage(8, copy));
        this.logBatch(copy);
    }

    void performError(BitmapHunter hunter, boolean willReplay) {
        if (hunter.getPicasso().loggingEnabled) {
            Utils.log("Dispatcher", "batched", Utils.getLogIdsForHunter(hunter), "for error" + (willReplay ? " (will replay)" : ""));
        }
        this.hunterMap.remove(hunter.getKey());
        this.batch(hunter);
    }

    void performAirplaneModeChange(boolean airplaneMode) {
        this.airplaneMode = airplaneMode;
    }

    void performNetworkStateChange(NetworkInfo info) {
        if (this.service instanceof PicassoExecutorService) {
            ((PicassoExecutorService)this.service).adjustThreadCount(info);
        }
        if (info != null && info.isConnected()) {
            this.flushFailedActions();
        }
    }

    private void flushFailedActions() {
        if (!this.failedActions.isEmpty()) {
            Iterator<Action> iterator = this.failedActions.values().iterator();
            while (iterator.hasNext()) {
                Action action = iterator.next();
                iterator.remove();
                if (action.getPicasso().loggingEnabled) {
                    Utils.log("Dispatcher", "replaying", action.getRequest().logId());
                }
                this.performSubmit(action, false);
            }
        }
    }

    private void markForReplay(BitmapHunter hunter) {
        List<Action> joined;
        Action action = hunter.getAction();
        if (action != null) {
            this.markForReplay(action);
        }
        if ((joined = hunter.getActions()) != null) {
            int n = joined.size();
            for (int i = 0; i < n; ++i) {
                Action join = joined.get(i);
                this.markForReplay(join);
            }
        }
    }

    private void markForReplay(Action action) {
        Object target = action.getTarget();
        if (target != null) {
            action.willReplay = true;
            this.failedActions.put(target, action);
        }
    }

    private void batch(BitmapHunter hunter) {
        if (hunter.isCancelled()) {
            return;
        }
        this.batch.add(hunter);
        if (!this.handler.hasMessages(7)) {
            this.handler.sendEmptyMessageDelayed(7, 200);
        }
    }

    private void logBatch(List<BitmapHunter> copy) {
        if (copy == null || copy.isEmpty()) {
            return;
        }
        BitmapHunter hunter = copy.get(0);
        Picasso picasso = hunter.getPicasso();
        if (picasso.loggingEnabled) {
            StringBuilder builder = new StringBuilder();
            for (BitmapHunter bitmapHunter : copy) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(Utils.getLogIdsForHunter(bitmapHunter));
            }
            Utils.log("Dispatcher", "delivered", builder.toString());
        }
    }

    static class NetworkBroadcastReceiver
    extends BroadcastReceiver {
        static final String EXTRA_AIRPLANE_STATE = "state";
        private final Dispatcher dispatcher;

        NetworkBroadcastReceiver(Dispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        void register() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.AIRPLANE_MODE");
            if (this.dispatcher.scansNetworkChanges) {
                filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            }
            this.dispatcher.context.registerReceiver((BroadcastReceiver)this, filter);
        }

        void unregister() {
            this.dispatcher.context.unregisterReceiver((BroadcastReceiver)this);
        }

        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                if (!intent.hasExtra("state")) {
                    return;
                }
                this.dispatcher.dispatchAirplaneModeChange(intent.getBooleanExtra("state", false));
            } else if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
                ConnectivityManager connectivityManager = (ConnectivityManager)Utils.getService(context, "connectivity");
                this.dispatcher.dispatchNetworkStateChange(connectivityManager.getActiveNetworkInfo());
            }
        }
    }

    static class DispatcherThread
    extends HandlerThread {
        DispatcherThread() {
            super("Picasso-Dispatcher", 10);
        }
    }

    private static class DispatcherHandler
    extends Handler {
        private final Dispatcher dispatcher;

        public DispatcherHandler(Looper looper, Dispatcher dispatcher) {
            super(looper);
            this.dispatcher = dispatcher;
        }

        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case 1: {
                    Action action = (Action)msg.obj;
                    this.dispatcher.performSubmit(action);
                    break;
                }
                case 2: {
                    Action action = (Action)msg.obj;
                    this.dispatcher.performCancel(action);
                    break;
                }
                case 11: {
                    Object tag = msg.obj;
                    this.dispatcher.performPauseTag(tag);
                    break;
                }
                case 12: {
                    Object tag = msg.obj;
                    this.dispatcher.performResumeTag(tag);
                    break;
                }
                case 4: {
                    BitmapHunter hunter = (BitmapHunter)msg.obj;
                    this.dispatcher.performComplete(hunter);
                    break;
                }
                case 5: {
                    BitmapHunter hunter = (BitmapHunter)msg.obj;
                    this.dispatcher.performRetry(hunter);
                    break;
                }
                case 6: {
                    BitmapHunter hunter = (BitmapHunter)msg.obj;
                    this.dispatcher.performError(hunter, false);
                    break;
                }
                case 7: {
                    this.dispatcher.performBatchComplete();
                    break;
                }
                case 9: {
                    NetworkInfo info = (NetworkInfo)msg.obj;
                    this.dispatcher.performNetworkStateChange(info);
                    break;
                }
                case 10: {
                    this.dispatcher.performAirplaneModeChange(msg.arg1 == 1);
                    break;
                }
                default: {
                    Picasso.HANDLER.post(new Runnable(){

                        @Override
                        public void run() {
                            throw new AssertionError((Object)("Unknown handler message received: " + msg.what));
                        }
                    });
                }
            }
        }

    }

}


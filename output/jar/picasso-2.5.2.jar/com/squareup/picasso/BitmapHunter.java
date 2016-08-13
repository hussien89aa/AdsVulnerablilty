/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.graphics.Bitmap
 *  android.graphics.BitmapFactory
 *  android.graphics.BitmapFactory$Options
 *  android.graphics.Matrix
 *  android.graphics.Rect
 *  android.net.NetworkInfo
 *  android.os.Handler
 */
package com.squareup.picasso;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.NetworkInfo;
import android.os.Handler;
import com.squareup.picasso.Action;
import com.squareup.picasso.Cache;
import com.squareup.picasso.Dispatcher;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.MarkableInputStream;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.NetworkRequestHandler;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import com.squareup.picasso.Stats;
import com.squareup.picasso.StatsSnapshot;
import com.squareup.picasso.Transformation;
import com.squareup.picasso.Utils;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

class BitmapHunter
implements Runnable {
    private static final Object DECODE_LOCK = new Object();
    private static final ThreadLocal<StringBuilder> NAME_BUILDER = new ThreadLocal<StringBuilder>(){

        @Override
        protected StringBuilder initialValue() {
            return new StringBuilder("Picasso-");
        }
    };
    private static final AtomicInteger SEQUENCE_GENERATOR = new AtomicInteger();
    private static final RequestHandler ERRORING_HANDLER = new RequestHandler(){

        @Override
        public boolean canHandleRequest(Request data) {
            return true;
        }

        @Override
        public RequestHandler.Result load(Request request, int networkPolicy) throws IOException {
            throw new IllegalStateException("Unrecognized type of request: " + request);
        }
    };
    final int sequence = SEQUENCE_GENERATOR.incrementAndGet();
    final Picasso picasso;
    final Dispatcher dispatcher;
    final Cache cache;
    final Stats stats;
    final String key;
    final Request data;
    final int memoryPolicy;
    int networkPolicy;
    final RequestHandler requestHandler;
    Action action;
    List<Action> actions;
    Bitmap result;
    Future<?> future;
    Picasso.LoadedFrom loadedFrom;
    Exception exception;
    int exifRotation;
    int retryCount;
    Picasso.Priority priority;

    BitmapHunter(Picasso picasso, Dispatcher dispatcher, Cache cache, Stats stats, Action action, RequestHandler requestHandler) {
        this.picasso = picasso;
        this.dispatcher = dispatcher;
        this.cache = cache;
        this.stats = stats;
        this.action = action;
        this.key = action.getKey();
        this.data = action.getRequest();
        this.priority = action.getPriority();
        this.memoryPolicy = action.getMemoryPolicy();
        this.networkPolicy = action.getNetworkPolicy();
        this.requestHandler = requestHandler;
        this.retryCount = requestHandler.getRetryCount();
    }

    static Bitmap decodeStream(InputStream stream, Request request) throws IOException {
        MarkableInputStream markStream;
        Bitmap bitmap;
        stream = markStream = new MarkableInputStream(stream);
        long mark = markStream.savePosition(65536);
        BitmapFactory.Options options = RequestHandler.createBitmapOptions(request);
        boolean calculateSize = RequestHandler.requiresInSampleSize(options);
        boolean isWebPFile = Utils.isWebPFile(stream);
        markStream.reset(mark);
        if (isWebPFile) {
            byte[] bytes = Utils.toByteArray(stream);
            if (calculateSize) {
                BitmapFactory.decodeByteArray((byte[])bytes, (int)0, (int)bytes.length, (BitmapFactory.Options)options);
                RequestHandler.calculateInSampleSize(request.targetWidth, request.targetHeight, options, request);
            }
            return BitmapFactory.decodeByteArray((byte[])bytes, (int)0, (int)bytes.length, (BitmapFactory.Options)options);
        }
        if (calculateSize) {
            BitmapFactory.decodeStream((InputStream)stream, (Rect)null, (BitmapFactory.Options)options);
            RequestHandler.calculateInSampleSize(request.targetWidth, request.targetHeight, options, request);
            markStream.reset(mark);
        }
        if ((bitmap = BitmapFactory.decodeStream((InputStream)stream, (Rect)null, (BitmapFactory.Options)options)) == null) {
            throw new IOException("Failed to decode stream.");
        }
        return bitmap;
    }

    @Override
    public void run() {
        block16 : {
            try {
                BitmapHunter.updateThreadName(this.data);
                if (this.picasso.loggingEnabled) {
                    Utils.log("Hunter", "executing", Utils.getLogIdsForHunter(this));
                }
                this.result = this.hunt();
                if (this.result == null) {
                    this.dispatcher.dispatchFailed(this);
                    break block16;
                }
                this.dispatcher.dispatchComplete(this);
            }
            catch (Downloader.ResponseException e) {
                if (!e.localCacheOnly || e.responseCode != 504) {
                    this.exception = e;
                }
                this.dispatcher.dispatchFailed(this);
            }
            catch (NetworkRequestHandler.ContentLengthException e) {
                this.exception = e;
                this.dispatcher.dispatchRetry(this);
            }
            catch (IOException e) {
                this.exception = e;
                this.dispatcher.dispatchRetry(this);
            }
            catch (OutOfMemoryError e) {
                StringWriter writer = new StringWriter();
                this.stats.createSnapshot().dump(new PrintWriter(writer));
                this.exception = new RuntimeException(writer.toString(), e);
                this.dispatcher.dispatchFailed(this);
            }
            catch (Exception e) {
                this.exception = e;
                this.dispatcher.dispatchFailed(this);
            }
            finally {
                Thread.currentThread().setName("Picasso-Idle");
            }
        }
    }

    Bitmap hunt() throws IOException {
        Object is;
        Bitmap bitmap = null;
        if (MemoryPolicy.shouldReadFromMemoryCache(this.memoryPolicy) && (bitmap = this.cache.get(this.key)) != null) {
            this.stats.dispatchCacheHit();
            this.loadedFrom = Picasso.LoadedFrom.MEMORY;
            if (this.picasso.loggingEnabled) {
                Utils.log("Hunter", "decoded", this.data.logId(), "from cache");
            }
            return bitmap;
        }
        this.data.networkPolicy = this.retryCount == 0 ? NetworkPolicy.OFFLINE.index : this.networkPolicy;
        RequestHandler.Result result = this.requestHandler.load(this.data, this.networkPolicy);
        if (result != null) {
            this.loadedFrom = result.getLoadedFrom();
            this.exifRotation = result.getExifOrientation();
            bitmap = result.getBitmap();
            if (bitmap == null) {
                is = result.getStream();
                try {
                    bitmap = BitmapHunter.decodeStream((InputStream)is, this.data);
                }
                finally {
                    Utils.closeQuietly((InputStream)is);
                }
            }
        }
        if (bitmap != null) {
            if (this.picasso.loggingEnabled) {
                Utils.log("Hunter", "decoded", this.data.logId());
            }
            this.stats.dispatchBitmapDecoded(bitmap);
            if (this.data.needsTransformation() || this.exifRotation != 0) {
                is = DECODE_LOCK;
                synchronized (is) {
                    if (this.data.needsMatrixTransform() || this.exifRotation != 0) {
                        bitmap = BitmapHunter.transformResult(this.data, bitmap, this.exifRotation);
                        if (this.picasso.loggingEnabled) {
                            Utils.log("Hunter", "transformed", this.data.logId());
                        }
                    }
                    if (this.data.hasCustomTransformations()) {
                        bitmap = BitmapHunter.applyCustomTransformations(this.data.transformations, bitmap);
                        if (this.picasso.loggingEnabled) {
                            Utils.log("Hunter", "transformed", this.data.logId(), "from custom transformations");
                        }
                    }
                }
                if (bitmap != null) {
                    this.stats.dispatchBitmapTransformed(bitmap);
                }
            }
        }
        return bitmap;
    }

    void attach(Action action) {
        Picasso.Priority actionPriority;
        boolean loggingEnabled = this.picasso.loggingEnabled;
        Request request = action.request;
        if (this.action == null) {
            this.action = action;
            if (loggingEnabled) {
                if (this.actions == null || this.actions.isEmpty()) {
                    Utils.log("Hunter", "joined", request.logId(), "to empty hunter");
                } else {
                    Utils.log("Hunter", "joined", request.logId(), Utils.getLogIdsForHunter(this, "to "));
                }
            }
            return;
        }
        if (this.actions == null) {
            this.actions = new ArrayList<Action>(3);
        }
        this.actions.add(action);
        if (loggingEnabled) {
            Utils.log("Hunter", "joined", request.logId(), Utils.getLogIdsForHunter(this, "to "));
        }
        if ((actionPriority = action.getPriority()).ordinal() > this.priority.ordinal()) {
            this.priority = actionPriority;
        }
    }

    void detach(Action action) {
        boolean detached = false;
        if (this.action == action) {
            this.action = null;
            detached = true;
        } else if (this.actions != null) {
            detached = this.actions.remove(action);
        }
        if (detached && action.getPriority() == this.priority) {
            this.priority = this.computeNewPriority();
        }
        if (this.picasso.loggingEnabled) {
            Utils.log("Hunter", "removed", action.request.logId(), Utils.getLogIdsForHunter(this, "from "));
        }
    }

    private Picasso.Priority computeNewPriority() {
        boolean hasAny;
        Picasso.Priority newPriority = Picasso.Priority.LOW;
        boolean hasMultiple = this.actions != null && !this.actions.isEmpty();
        boolean bl = hasAny = this.action != null || hasMultiple;
        if (!hasAny) {
            return newPriority;
        }
        if (this.action != null) {
            newPriority = this.action.getPriority();
        }
        if (hasMultiple) {
            int n = this.actions.size();
            for (int i = 0; i < n; ++i) {
                Picasso.Priority actionPriority = this.actions.get(i).getPriority();
                if (actionPriority.ordinal() <= newPriority.ordinal()) continue;
                newPriority = actionPriority;
            }
        }
        return newPriority;
    }

    boolean cancel() {
        return this.action == null && (this.actions == null || this.actions.isEmpty()) && this.future != null && this.future.cancel(false);
    }

    boolean isCancelled() {
        return this.future != null && this.future.isCancelled();
    }

    boolean shouldRetry(boolean airplaneMode, NetworkInfo info) {
        boolean hasRetries;
        boolean bl = hasRetries = this.retryCount > 0;
        if (!hasRetries) {
            return false;
        }
        --this.retryCount;
        return this.requestHandler.shouldRetry(airplaneMode, info);
    }

    boolean supportsReplay() {
        return this.requestHandler.supportsReplay();
    }

    Bitmap getResult() {
        return this.result;
    }

    String getKey() {
        return this.key;
    }

    int getMemoryPolicy() {
        return this.memoryPolicy;
    }

    Request getData() {
        return this.data;
    }

    Action getAction() {
        return this.action;
    }

    Picasso getPicasso() {
        return this.picasso;
    }

    List<Action> getActions() {
        return this.actions;
    }

    Exception getException() {
        return this.exception;
    }

    Picasso.LoadedFrom getLoadedFrom() {
        return this.loadedFrom;
    }

    Picasso.Priority getPriority() {
        return this.priority;
    }

    static void updateThreadName(Request data) {
        String name = data.getName();
        StringBuilder builder = NAME_BUILDER.get();
        builder.ensureCapacity("Picasso-".length() + name.length());
        builder.replace("Picasso-".length(), builder.length(), name);
        Thread.currentThread().setName(builder.toString());
    }

    static BitmapHunter forRequest(Picasso picasso, Dispatcher dispatcher, Cache cache, Stats stats, Action action) {
        Request request = action.getRequest();
        List<RequestHandler> requestHandlers = picasso.getRequestHandlers();
        int count = requestHandlers.size();
        for (int i = 0; i < count; ++i) {
            RequestHandler requestHandler = requestHandlers.get(i);
            if (!requestHandler.canHandleRequest(request)) continue;
            return new BitmapHunter(picasso, dispatcher, cache, stats, action, requestHandler);
        }
        return new BitmapHunter(picasso, dispatcher, cache, stats, action, ERRORING_HANDLER);
    }

    static Bitmap applyCustomTransformations(List<Transformation> transformations, Bitmap result) {
        int count = transformations.size();
        for (int i = 0; i < count; ++i) {
            Bitmap newResult;
            final Transformation transformation = transformations.get(i);
            try {
                newResult = transformation.transform(result);
            }
            catch (RuntimeException e) {
                Picasso.HANDLER.post(new Runnable(){

                    @Override
                    public void run() {
                        throw new RuntimeException("Transformation " + transformation.key() + " crashed with exception.", e);
                    }
                });
                return null;
            }
            if (newResult == null) {
                final StringBuilder builder = new StringBuilder().append("Transformation ").append(transformation.key()).append(" returned null after ").append(i).append(" previous transformation(s).\n\nTransformation list:\n");
                for (Transformation t : transformations) {
                    builder.append(t.key()).append('\n');
                }
                Picasso.HANDLER.post(new Runnable(){

                    @Override
                    public void run() {
                        throw new NullPointerException(builder.toString());
                    }
                });
                return null;
            }
            if (newResult == result && result.isRecycled()) {
                Picasso.HANDLER.post(new Runnable(){

                    @Override
                    public void run() {
                        throw new IllegalStateException("Transformation " + transformation.key() + " returned input Bitmap but recycled it.");
                    }
                });
                return null;
            }
            if (newResult != result && !result.isRecycled()) {
                Picasso.HANDLER.post(new Runnable(){

                    @Override
                    public void run() {
                        throw new IllegalStateException("Transformation " + transformation.key() + " mutated input Bitmap but failed to recycle the original.");
                    }
                });
                return null;
            }
            result = newResult;
        }
        return result;
    }

    static Bitmap transformResult(Request data, Bitmap result, int exifRotation) {
        Bitmap newResult;
        int inWidth = result.getWidth();
        int inHeight = result.getHeight();
        boolean onlyScaleDown = data.onlyScaleDown;
        int drawX = 0;
        int drawY = 0;
        int drawWidth = inWidth;
        int drawHeight = inHeight;
        Matrix matrix = new Matrix();
        if (data.needsMatrixTransform()) {
            int targetWidth = data.targetWidth;
            int targetHeight = data.targetHeight;
            float targetRotation = data.rotationDegrees;
            if (targetRotation != 0.0f) {
                if (data.hasRotationPivot) {
                    matrix.setRotate(targetRotation, data.rotationPivotX, data.rotationPivotY);
                } else {
                    matrix.setRotate(targetRotation);
                }
            }
            if (data.centerCrop) {
                float scaleY;
                float scaleX;
                float widthRatio = (float)targetWidth / (float)inWidth;
                float heightRatio = (float)targetHeight / (float)inHeight;
                if (widthRatio > heightRatio) {
                    int newSize = (int)Math.ceil((float)inHeight * (heightRatio / widthRatio));
                    drawY = (inHeight - newSize) / 2;
                    drawHeight = newSize;
                    scaleX = widthRatio;
                    scaleY = (float)targetHeight / (float)drawHeight;
                } else {
                    int newSize = (int)Math.ceil((float)inWidth * (widthRatio / heightRatio));
                    drawX = (inWidth - newSize) / 2;
                    drawWidth = newSize;
                    scaleX = (float)targetWidth / (float)drawWidth;
                    scaleY = heightRatio;
                }
                if (BitmapHunter.shouldResize(onlyScaleDown, inWidth, inHeight, targetWidth, targetHeight)) {
                    matrix.preScale(scaleX, scaleY);
                }
            } else if (data.centerInside) {
                float scale;
                float widthRatio = (float)targetWidth / (float)inWidth;
                float heightRatio = (float)targetHeight / (float)inHeight;
                float f = scale = widthRatio < heightRatio ? widthRatio : heightRatio;
                if (BitmapHunter.shouldResize(onlyScaleDown, inWidth, inHeight, targetWidth, targetHeight)) {
                    matrix.preScale(scale, scale);
                }
            } else if (!(targetWidth == 0 && targetHeight == 0 || targetWidth == inWidth && targetHeight == inHeight)) {
                float sy;
                float sx = targetWidth != 0 ? (float)targetWidth / (float)inWidth : (float)targetHeight / (float)inHeight;
                float f = sy = targetHeight != 0 ? (float)targetHeight / (float)inHeight : (float)targetWidth / (float)inWidth;
                if (BitmapHunter.shouldResize(onlyScaleDown, inWidth, inHeight, targetWidth, targetHeight)) {
                    matrix.preScale(sx, sy);
                }
            }
        }
        if (exifRotation != 0) {
            matrix.preRotate((float)exifRotation);
        }
        if ((newResult = Bitmap.createBitmap((Bitmap)result, (int)drawX, (int)drawY, (int)drawWidth, (int)drawHeight, (Matrix)matrix, (boolean)true)) != result) {
            result.recycle();
            result = newResult;
        }
        return result;
    }

    private static boolean shouldResize(boolean onlyScaleDown, int inWidth, int inHeight, int targetWidth, int targetHeight) {
        return !onlyScaleDown || inWidth > targetWidth || inHeight > targetHeight;
    }

}


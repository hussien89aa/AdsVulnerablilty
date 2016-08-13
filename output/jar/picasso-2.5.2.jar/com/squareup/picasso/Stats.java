/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.graphics.Bitmap
 *  android.os.Handler
 *  android.os.HandlerThread
 *  android.os.Looper
 *  android.os.Message
 */
package com.squareup.picasso;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.squareup.picasso.Cache;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.StatsSnapshot;
import com.squareup.picasso.Utils;

class Stats {
    private static final int CACHE_HIT = 0;
    private static final int CACHE_MISS = 1;
    private static final int BITMAP_DECODE_FINISHED = 2;
    private static final int BITMAP_TRANSFORMED_FINISHED = 3;
    private static final int DOWNLOAD_FINISHED = 4;
    private static final String STATS_THREAD_NAME = "Picasso-Stats";
    final HandlerThread statsThread;
    final Cache cache;
    final Handler handler;
    long cacheHits;
    long cacheMisses;
    long totalDownloadSize;
    long totalOriginalBitmapSize;
    long totalTransformedBitmapSize;
    long averageDownloadSize;
    long averageOriginalBitmapSize;
    long averageTransformedBitmapSize;
    int downloadCount;
    int originalBitmapCount;
    int transformedBitmapCount;

    Stats(Cache cache) {
        this.cache = cache;
        this.statsThread = new HandlerThread("Picasso-Stats", 10);
        this.statsThread.start();
        Utils.flushStackLocalLeaks(this.statsThread.getLooper());
        this.handler = new StatsHandler(this.statsThread.getLooper(), this);
    }

    void dispatchBitmapDecoded(Bitmap bitmap) {
        this.processBitmap(bitmap, 2);
    }

    void dispatchBitmapTransformed(Bitmap bitmap) {
        this.processBitmap(bitmap, 3);
    }

    void dispatchDownloadFinished(long size) {
        this.handler.sendMessage(this.handler.obtainMessage(4, (Object)size));
    }

    void dispatchCacheHit() {
        this.handler.sendEmptyMessage(0);
    }

    void dispatchCacheMiss() {
        this.handler.sendEmptyMessage(1);
    }

    void shutdown() {
        this.statsThread.quit();
    }

    void performCacheHit() {
        ++this.cacheHits;
    }

    void performCacheMiss() {
        ++this.cacheMisses;
    }

    void performDownloadFinished(Long size) {
        ++this.downloadCount;
        this.totalDownloadSize += size.longValue();
        this.averageDownloadSize = Stats.getAverage(this.downloadCount, this.totalDownloadSize);
    }

    void performBitmapDecoded(long size) {
        ++this.originalBitmapCount;
        this.totalOriginalBitmapSize += size;
        this.averageOriginalBitmapSize = Stats.getAverage(this.originalBitmapCount, this.totalOriginalBitmapSize);
    }

    void performBitmapTransformed(long size) {
        ++this.transformedBitmapCount;
        this.totalTransformedBitmapSize += size;
        this.averageTransformedBitmapSize = Stats.getAverage(this.originalBitmapCount, this.totalTransformedBitmapSize);
    }

    StatsSnapshot createSnapshot() {
        return new StatsSnapshot(this.cache.maxSize(), this.cache.size(), this.cacheHits, this.cacheMisses, this.totalDownloadSize, this.totalOriginalBitmapSize, this.totalTransformedBitmapSize, this.averageDownloadSize, this.averageOriginalBitmapSize, this.averageTransformedBitmapSize, this.downloadCount, this.originalBitmapCount, this.transformedBitmapCount, System.currentTimeMillis());
    }

    private void processBitmap(Bitmap bitmap, int what) {
        int bitmapSize = Utils.getBitmapBytes(bitmap);
        this.handler.sendMessage(this.handler.obtainMessage(what, bitmapSize, 0));
    }

    private static long getAverage(int count, long totalSize) {
        return totalSize / (long)count;
    }

    private static class StatsHandler
    extends Handler {
        private final Stats stats;

        public StatsHandler(Looper looper, Stats stats) {
            super(looper);
            this.stats = stats;
        }

        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case 0: {
                    this.stats.performCacheHit();
                    break;
                }
                case 1: {
                    this.stats.performCacheMiss();
                    break;
                }
                case 2: {
                    this.stats.performBitmapDecoded(msg.arg1);
                    break;
                }
                case 3: {
                    this.stats.performBitmapTransformed(msg.arg1);
                    break;
                }
                case 4: {
                    this.stats.performDownloadFinished((Long)msg.obj);
                    break;
                }
                default: {
                    Picasso.HANDLER.post(new Runnable(){

                        @Override
                        public void run() {
                            throw new AssertionError((Object)("Unhandled stats message." + msg.what));
                        }
                    });
                }
            }
        }

    }

}


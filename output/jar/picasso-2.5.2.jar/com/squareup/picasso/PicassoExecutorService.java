/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.net.NetworkInfo
 */
package com.squareup.picasso;

import android.net.NetworkInfo;
import com.squareup.picasso.BitmapHunter;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Utils;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class PicassoExecutorService
extends ThreadPoolExecutor {
    private static final int DEFAULT_THREAD_COUNT = 3;

    PicassoExecutorService() {
        super(3, 3, 0, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<Runnable>(), new Utils.PicassoThreadFactory());
    }

    void adjustThreadCount(NetworkInfo info) {
        if (info == null || !info.isConnectedOrConnecting()) {
            this.setThreadCount(3);
            return;
        }
        block0 : switch (info.getType()) {
            case 1: 
            case 6: 
            case 9: {
                this.setThreadCount(4);
                break;
            }
            case 0: {
                switch (info.getSubtype()) {
                    case 13: 
                    case 14: 
                    case 15: {
                        this.setThreadCount(3);
                        break block0;
                    }
                    case 3: 
                    case 4: 
                    case 5: 
                    case 6: 
                    case 12: {
                        this.setThreadCount(2);
                        break block0;
                    }
                    case 1: 
                    case 2: {
                        this.setThreadCount(1);
                        break block0;
                    }
                }
                this.setThreadCount(3);
                break;
            }
            default: {
                this.setThreadCount(3);
            }
        }
    }

    private void setThreadCount(int threadCount) {
        this.setCorePoolSize(threadCount);
        this.setMaximumPoolSize(threadCount);
    }

    @Override
    public Future<?> submit(Runnable task) {
        PicassoFutureTask ftask = new PicassoFutureTask((BitmapHunter)task);
        this.execute(ftask);
        return ftask;
    }

    private static final class PicassoFutureTask
    extends FutureTask<BitmapHunter>
    implements Comparable<PicassoFutureTask> {
        private final BitmapHunter hunter;

        public PicassoFutureTask(BitmapHunter hunter) {
            super(hunter, null);
            this.hunter = hunter;
        }

        @Override
        public int compareTo(PicassoFutureTask other) {
            Picasso.Priority p2;
            Picasso.Priority p1 = this.hunter.getPriority();
            return p1 == (p2 = other.hunter.getPriority()) ? this.hunter.sequence - other.hunter.sequence : p2.ordinal() - p1.ordinal();
        }
    }

}


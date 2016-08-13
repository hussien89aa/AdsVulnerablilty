/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.graphics.Bitmap
 */
package com.squareup.picasso;

import android.content.Context;
import android.graphics.Bitmap;
import com.squareup.picasso.Cache;
import com.squareup.picasso.Utils;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class LruCache
implements Cache {
    final LinkedHashMap<String, Bitmap> map;
    private final int maxSize;
    private int size;
    private int putCount;
    private int evictionCount;
    private int hitCount;
    private int missCount;

    public LruCache(Context context) {
        this(Utils.calculateMemoryCacheSize(context));
    }

    public LruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Max size must be positive.");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap(0, 0.75f, true);
    }

    @Override
    public Bitmap get(String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        LruCache lruCache = this;
        synchronized (lruCache) {
            Bitmap mapValue = this.map.get(key);
            if (mapValue != null) {
                ++this.hitCount;
                return mapValue;
            }
            ++this.missCount;
        }
        return null;
    }

    @Override
    public void set(String key, Bitmap bitmap) {
        if (key == null || bitmap == null) {
            throw new NullPointerException("key == null || bitmap == null");
        }
        LruCache lruCache = this;
        synchronized (lruCache) {
            ++this.putCount;
            this.size += Utils.getBitmapBytes(bitmap);
            Bitmap previous = this.map.put(key, bitmap);
            if (previous != null) {
                this.size -= Utils.getBitmapBytes(previous);
            }
        }
        this.trimToSize(this.maxSize);
    }

    private void trimToSize(int maxSize) {
        do {
            LruCache lruCache = this;
            synchronized (lruCache) {
                if (this.size < 0 || this.map.isEmpty() && this.size != 0) {
                    throw new IllegalStateException(this.getClass().getName() + ".sizeOf() is reporting inconsistent results!");
                }
                if (this.size <= maxSize || this.map.isEmpty()) {
                    break;
                }
                Map.Entry<String, Bitmap> toEvict = this.map.entrySet().iterator().next();
                String key = toEvict.getKey();
                Bitmap value = toEvict.getValue();
                this.map.remove(key);
                this.size -= Utils.getBitmapBytes(value);
                ++this.evictionCount;
                continue;
            }
        } while (true);
    }

    public final void evictAll() {
        this.trimToSize(-1);
    }

    @Override
    public final synchronized int size() {
        return this.size;
    }

    @Override
    public final synchronized int maxSize() {
        return this.maxSize;
    }

    @Override
    public final synchronized void clear() {
        this.evictAll();
    }

    @Override
    public final synchronized void clearKeyUri(String uri) {
        boolean sizeChanged = false;
        int uriLength = uri.length();
        Iterator<Map.Entry<String, Bitmap>> i = this.map.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, Bitmap> entry = i.next();
            String key = entry.getKey();
            Bitmap value = entry.getValue();
            int newlineIndex = key.indexOf(10);
            if (newlineIndex != uriLength || !key.substring(0, newlineIndex).equals(uri)) continue;
            i.remove();
            this.size -= Utils.getBitmapBytes(value);
            sizeChanged = true;
        }
        if (sizeChanged) {
            this.trimToSize(this.maxSize);
        }
    }

    public final synchronized int hitCount() {
        return this.hitCount;
    }

    public final synchronized int missCount() {
        return this.missCount;
    }

    public final synchronized int putCount() {
        return this.putCount;
    }

    public final synchronized int evictionCount() {
        return this.evictionCount;
    }
}


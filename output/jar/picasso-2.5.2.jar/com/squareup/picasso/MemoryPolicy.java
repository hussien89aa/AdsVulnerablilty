/*
 * Decompiled with CFR 0_114.
 */
package com.squareup.picasso;

public enum MemoryPolicy {
    NO_CACHE(1),
    NO_STORE(2);
    
    final int index;

    static boolean shouldReadFromMemoryCache(int memoryPolicy) {
        return (memoryPolicy & MemoryPolicy.NO_CACHE.index) == 0;
    }

    static boolean shouldWriteToMemoryCache(int memoryPolicy) {
        return (memoryPolicy & MemoryPolicy.NO_STORE.index) == 0;
    }

    private MemoryPolicy(int index) {
        this.index = index;
    }
}


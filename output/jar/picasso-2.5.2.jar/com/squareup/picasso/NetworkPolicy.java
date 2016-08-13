/*
 * Decompiled with CFR 0_114.
 */
package com.squareup.picasso;

public enum NetworkPolicy {
    NO_CACHE(1),
    NO_STORE(2),
    OFFLINE(4);
    
    final int index;

    public static boolean shouldReadFromDiskCache(int networkPolicy) {
        return (networkPolicy & NetworkPolicy.NO_CACHE.index) == 0;
    }

    public static boolean shouldWriteToDiskCache(int networkPolicy) {
        return (networkPolicy & NetworkPolicy.NO_STORE.index) == 0;
    }

    public static boolean isOfflineOnly(int networkPolicy) {
        return (networkPolicy & NetworkPolicy.OFFLINE.index) != 0;
    }

    private NetworkPolicy(int index) {
        this.index = index;
    }
}


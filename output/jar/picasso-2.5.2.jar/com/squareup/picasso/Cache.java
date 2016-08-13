/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.graphics.Bitmap
 */
package com.squareup.picasso;

import android.graphics.Bitmap;

public interface Cache {
    public static final Cache NONE = new Cache(){

        @Override
        public Bitmap get(String key) {
            return null;
        }

        @Override
        public void set(String key, Bitmap bitmap) {
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public int maxSize() {
            return 0;
        }

        @Override
        public void clear() {
        }

        @Override
        public void clearKeyUri(String keyPrefix) {
        }
    };

    public Bitmap get(String var1);

    public void set(String var1, Bitmap var2);

    public int size();

    public int maxSize();

    public void clear();

    public void clearKeyUri(String var1);

}


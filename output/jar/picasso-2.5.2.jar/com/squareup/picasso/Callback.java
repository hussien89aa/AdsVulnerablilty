/*
 * Decompiled with CFR 0_114.
 */
package com.squareup.picasso;

public interface Callback {
    public void onSuccess();

    public void onError();

    public static class EmptyCallback
    implements Callback {
        @Override
        public void onSuccess() {
        }

        @Override
        public void onError() {
        }
    }

}


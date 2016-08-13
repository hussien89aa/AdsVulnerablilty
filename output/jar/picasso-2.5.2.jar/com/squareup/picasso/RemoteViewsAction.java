/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.app.Notification
 *  android.app.NotificationManager
 *  android.appwidget.AppWidgetManager
 *  android.content.Context
 *  android.graphics.Bitmap
 *  android.graphics.drawable.Drawable
 *  android.widget.RemoteViews
 */
package com.squareup.picasso;

import android.app.Notification;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.RemoteViews;
import com.squareup.picasso.Action;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.Utils;

abstract class RemoteViewsAction
extends Action<RemoteViewsTarget> {
    final RemoteViews remoteViews;
    final int viewId;
    private RemoteViewsTarget target;

    RemoteViewsAction(Picasso picasso, Request data, RemoteViews remoteViews, int viewId, int errorResId, int memoryPolicy, int networkPolicy, Object tag, String key) {
        super(picasso, null, data, memoryPolicy, networkPolicy, errorResId, null, key, tag, false);
        this.remoteViews = remoteViews;
        this.viewId = viewId;
    }

    @Override
    void complete(Bitmap result, Picasso.LoadedFrom from) {
        this.remoteViews.setImageViewBitmap(this.viewId, result);
        this.update();
    }

    @Override
    public void error() {
        if (this.errorResId != 0) {
            this.setImageResource(this.errorResId);
        }
    }

    @Override
    RemoteViewsTarget getTarget() {
        if (this.target == null) {
            this.target = new RemoteViewsTarget(this.remoteViews, this.viewId);
        }
        return this.target;
    }

    void setImageResource(int resId) {
        this.remoteViews.setImageViewResource(this.viewId, resId);
        this.update();
    }

    abstract void update();

    static class NotificationAction
    extends RemoteViewsAction {
        private final int notificationId;
        private final Notification notification;

        NotificationAction(Picasso picasso, Request data, RemoteViews remoteViews, int viewId, int notificationId, Notification notification, int memoryPolicy, int networkPolicy, String key, Object tag, int errorResId) {
            super(picasso, data, remoteViews, viewId, errorResId, memoryPolicy, networkPolicy, tag, key);
            this.notificationId = notificationId;
            this.notification = notification;
        }

        @Override
        void update() {
            NotificationManager manager = (NotificationManager)Utils.getService(this.picasso.context, "notification");
            manager.notify(this.notificationId, this.notification);
        }
    }

    static class AppWidgetAction
    extends RemoteViewsAction {
        private final int[] appWidgetIds;

        AppWidgetAction(Picasso picasso, Request data, RemoteViews remoteViews, int viewId, int[] appWidgetIds, int memoryPolicy, int networkPolicy, String key, Object tag, int errorResId) {
            super(picasso, data, remoteViews, viewId, errorResId, memoryPolicy, networkPolicy, tag, key);
            this.appWidgetIds = appWidgetIds;
        }

        @Override
        void update() {
            AppWidgetManager manager = AppWidgetManager.getInstance((Context)this.picasso.context);
            manager.updateAppWidget(this.appWidgetIds, this.remoteViews);
        }
    }

    static class RemoteViewsTarget {
        final RemoteViews remoteViews;
        final int viewId;

        RemoteViewsTarget(RemoteViews remoteViews, int viewId) {
            this.remoteViews = remoteViews;
            this.viewId = viewId;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            RemoteViewsTarget remoteViewsTarget = (RemoteViewsTarget)o;
            return this.viewId == remoteViewsTarget.viewId && this.remoteViews.equals((Object)remoteViewsTarget.remoteViews);
        }

        public int hashCode() {
            return 31 * this.remoteViews.hashCode() + this.viewId;
        }
    }

}


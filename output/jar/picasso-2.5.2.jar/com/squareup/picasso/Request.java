/*
 * Decompiled with CFR 0_114.
 * 
 * Could not load the following classes:
 *  android.graphics.Bitmap
 *  android.graphics.Bitmap$Config
 *  android.net.Uri
 */
package com.squareup.picasso;

import android.graphics.Bitmap;
import android.net.Uri;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class Request {
    private static final long TOO_LONG_LOG = TimeUnit.SECONDS.toNanos(5);
    int id;
    long started;
    int networkPolicy;
    public final Uri uri;
    public final int resourceId;
    public final String stableKey;
    public final List<Transformation> transformations;
    public final int targetWidth;
    public final int targetHeight;
    public final boolean centerCrop;
    public final boolean centerInside;
    public final boolean onlyScaleDown;
    public final float rotationDegrees;
    public final float rotationPivotX;
    public final float rotationPivotY;
    public final boolean hasRotationPivot;
    public final Bitmap.Config config;
    public final Picasso.Priority priority;

    private Request(Uri uri, int resourceId, String stableKey, List<Transformation> transformations, int targetWidth, int targetHeight, boolean centerCrop, boolean centerInside, boolean onlyScaleDown, float rotationDegrees, float rotationPivotX, float rotationPivotY, boolean hasRotationPivot, Bitmap.Config config, Picasso.Priority priority) {
        this.uri = uri;
        this.resourceId = resourceId;
        this.stableKey = stableKey;
        this.transformations = transformations == null ? null : Collections.unmodifiableList(transformations);
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.centerCrop = centerCrop;
        this.centerInside = centerInside;
        this.onlyScaleDown = onlyScaleDown;
        this.rotationDegrees = rotationDegrees;
        this.rotationPivotX = rotationPivotX;
        this.rotationPivotY = rotationPivotY;
        this.hasRotationPivot = hasRotationPivot;
        this.config = config;
        this.priority = priority;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Request{");
        if (this.resourceId > 0) {
            sb.append(this.resourceId);
        } else {
            sb.append((Object)this.uri);
        }
        if (this.transformations != null && !this.transformations.isEmpty()) {
            for (Transformation transformation : this.transformations) {
                sb.append(' ').append(transformation.key());
            }
        }
        if (this.stableKey != null) {
            sb.append(" stableKey(").append(this.stableKey).append(')');
        }
        if (this.targetWidth > 0) {
            sb.append(" resize(").append(this.targetWidth).append(',').append(this.targetHeight).append(')');
        }
        if (this.centerCrop) {
            sb.append(" centerCrop");
        }
        if (this.centerInside) {
            sb.append(" centerInside");
        }
        if (this.rotationDegrees != 0.0f) {
            sb.append(" rotation(").append(this.rotationDegrees);
            if (this.hasRotationPivot) {
                sb.append(" @ ").append(this.rotationPivotX).append(',').append(this.rotationPivotY);
            }
            sb.append(')');
        }
        if (this.config != null) {
            sb.append(' ').append((Object)this.config);
        }
        sb.append('}');
        return sb.toString();
    }

    String logId() {
        long delta = System.nanoTime() - this.started;
        if (delta > TOO_LONG_LOG) {
            return this.plainId() + '+' + TimeUnit.NANOSECONDS.toSeconds(delta) + 's';
        }
        return this.plainId() + '+' + TimeUnit.NANOSECONDS.toMillis(delta) + "ms";
    }

    String plainId() {
        return "[R" + this.id + ']';
    }

    String getName() {
        if (this.uri != null) {
            return String.valueOf(this.uri.getPath());
        }
        return Integer.toHexString(this.resourceId);
    }

    public boolean hasSize() {
        return this.targetWidth != 0 || this.targetHeight != 0;
    }

    boolean needsTransformation() {
        return this.needsMatrixTransform() || this.hasCustomTransformations();
    }

    boolean needsMatrixTransform() {
        return this.hasSize() || this.rotationDegrees != 0.0f;
    }

    boolean hasCustomTransformations() {
        return this.transformations != null;
    }

    public Builder buildUpon() {
        return new Builder(this);
    }

    public static final class Builder {
        private Uri uri;
        private int resourceId;
        private String stableKey;
        private int targetWidth;
        private int targetHeight;
        private boolean centerCrop;
        private boolean centerInside;
        private boolean onlyScaleDown;
        private float rotationDegrees;
        private float rotationPivotX;
        private float rotationPivotY;
        private boolean hasRotationPivot;
        private List<Transformation> transformations;
        private Bitmap.Config config;
        private Picasso.Priority priority;

        public Builder(Uri uri) {
            this.setUri(uri);
        }

        public Builder(int resourceId) {
            this.setResourceId(resourceId);
        }

        Builder(Uri uri, int resourceId, Bitmap.Config bitmapConfig) {
            this.uri = uri;
            this.resourceId = resourceId;
            this.config = bitmapConfig;
        }

        private Builder(Request request) {
            this.uri = request.uri;
            this.resourceId = request.resourceId;
            this.stableKey = request.stableKey;
            this.targetWidth = request.targetWidth;
            this.targetHeight = request.targetHeight;
            this.centerCrop = request.centerCrop;
            this.centerInside = request.centerInside;
            this.rotationDegrees = request.rotationDegrees;
            this.rotationPivotX = request.rotationPivotX;
            this.rotationPivotY = request.rotationPivotY;
            this.hasRotationPivot = request.hasRotationPivot;
            this.onlyScaleDown = request.onlyScaleDown;
            if (request.transformations != null) {
                this.transformations = new ArrayList<Transformation>(request.transformations);
            }
            this.config = request.config;
            this.priority = request.priority;
        }

        boolean hasImage() {
            return this.uri != null || this.resourceId != 0;
        }

        boolean hasSize() {
            return this.targetWidth != 0 || this.targetHeight != 0;
        }

        boolean hasPriority() {
            return this.priority != null;
        }

        public Builder setUri(Uri uri) {
            if (uri == null) {
                throw new IllegalArgumentException("Image URI may not be null.");
            }
            this.uri = uri;
            this.resourceId = 0;
            return this;
        }

        public Builder setResourceId(int resourceId) {
            if (resourceId == 0) {
                throw new IllegalArgumentException("Image resource ID may not be 0.");
            }
            this.resourceId = resourceId;
            this.uri = null;
            return this;
        }

        public Builder stableKey(String stableKey) {
            this.stableKey = stableKey;
            return this;
        }

        public Builder resize(int targetWidth, int targetHeight) {
            if (targetWidth < 0) {
                throw new IllegalArgumentException("Width must be positive number or 0.");
            }
            if (targetHeight < 0) {
                throw new IllegalArgumentException("Height must be positive number or 0.");
            }
            if (targetHeight == 0 && targetWidth == 0) {
                throw new IllegalArgumentException("At least one dimension has to be positive number.");
            }
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
            return this;
        }

        public Builder clearResize() {
            this.targetWidth = 0;
            this.targetHeight = 0;
            this.centerCrop = false;
            this.centerInside = false;
            return this;
        }

        public Builder centerCrop() {
            if (this.centerInside) {
                throw new IllegalStateException("Center crop can not be used after calling centerInside");
            }
            this.centerCrop = true;
            return this;
        }

        public Builder clearCenterCrop() {
            this.centerCrop = false;
            return this;
        }

        public Builder centerInside() {
            if (this.centerCrop) {
                throw new IllegalStateException("Center inside can not be used after calling centerCrop");
            }
            this.centerInside = true;
            return this;
        }

        public Builder clearCenterInside() {
            this.centerInside = false;
            return this;
        }

        public Builder onlyScaleDown() {
            if (this.targetHeight == 0 && this.targetWidth == 0) {
                throw new IllegalStateException("onlyScaleDown can not be applied without resize");
            }
            this.onlyScaleDown = true;
            return this;
        }

        public Builder clearOnlyScaleDown() {
            this.onlyScaleDown = false;
            return this;
        }

        public Builder rotate(float degrees) {
            this.rotationDegrees = degrees;
            return this;
        }

        public Builder rotate(float degrees, float pivotX, float pivotY) {
            this.rotationDegrees = degrees;
            this.rotationPivotX = pivotX;
            this.rotationPivotY = pivotY;
            this.hasRotationPivot = true;
            return this;
        }

        public Builder clearRotation() {
            this.rotationDegrees = 0.0f;
            this.rotationPivotX = 0.0f;
            this.rotationPivotY = 0.0f;
            this.hasRotationPivot = false;
            return this;
        }

        public Builder config(Bitmap.Config config) {
            this.config = config;
            return this;
        }

        public Builder priority(Picasso.Priority priority) {
            if (priority == null) {
                throw new IllegalArgumentException("Priority invalid.");
            }
            if (this.priority != null) {
                throw new IllegalStateException("Priority already set.");
            }
            this.priority = priority;
            return this;
        }

        public Builder transform(Transformation transformation) {
            if (transformation == null) {
                throw new IllegalArgumentException("Transformation must not be null.");
            }
            if (transformation.key() == null) {
                throw new IllegalArgumentException("Transformation key must not be null.");
            }
            if (this.transformations == null) {
                this.transformations = new ArrayList<Transformation>(2);
            }
            this.transformations.add(transformation);
            return this;
        }

        public Builder transform(List<? extends Transformation> transformations) {
            if (transformations == null) {
                throw new IllegalArgumentException("Transformation list must not be null.");
            }
            int size = transformations.size();
            for (int i = 0; i < size; ++i) {
                this.transform(transformations.get(i));
            }
            return this;
        }

        public Request build() {
            if (this.centerInside && this.centerCrop) {
                throw new IllegalStateException("Center crop and center inside can not be used together.");
            }
            if (this.centerCrop && this.targetWidth == 0 && this.targetHeight == 0) {
                throw new IllegalStateException("Center crop requires calling resize with positive width and height.");
            }
            if (this.centerInside && this.targetWidth == 0 && this.targetHeight == 0) {
                throw new IllegalStateException("Center inside requires calling resize with positive width and height.");
            }
            if (this.priority == null) {
                this.priority = Picasso.Priority.NORMAL;
            }
            return new Request(this.uri, this.resourceId, this.stableKey, this.transformations, this.targetWidth, this.targetHeight, this.centerCrop, this.centerInside, this.onlyScaleDown, this.rotationDegrees, this.rotationPivotX, this.rotationPivotY, this.hasRotationPivot, this.config, this.priority);
        }
    }

}


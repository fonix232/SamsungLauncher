package com.android.launcher3.common.drawable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;

public class FastBitmapDrawable extends Drawable {
    private static final int GHOST_MODE_MIN_COLOR_RANGE = 130;
    private static final int MODE_FIT = 0;
    private static final int MODE_MATRIX = 1;
    private static final SparseArray<ColorFilter> sCachedBrightnessFilter = new SparseArray();
    private static ColorMatrix sGhostModeMatrix;
    private static final ColorMatrix sTempMatrix = new ColorMatrix();
    private int mAlpha;
    private final Bitmap mBitmap;
    private int mBrightness;
    private boolean mGhostModeEnabled;
    private int mHeight;
    private int mMode;
    private final Paint mPaint;
    private int mWidth;

    public FastBitmapDrawable(Bitmap b) {
        this.mPaint = new Paint(2);
        this.mBrightness = 0;
        this.mGhostModeEnabled = false;
        this.mAlpha = 255;
        this.mBitmap = b;
        this.mMode = 1;
        if (b != null) {
            this.mWidth = this.mBitmap.getWidth();
            this.mHeight = this.mBitmap.getHeight();
            return;
        }
        this.mHeight = 0;
        this.mWidth = 0;
    }

    public FastBitmapDrawable(Bitmap b, int width, int height) {
        this(b);
        if (b != null && b.getWidth() != width) {
            this.mMode = 0;
            this.mWidth = width;
            this.mHeight = height;
        }
    }

    public void draw(Canvas canvas) {
        if (this.mBitmap != null && !this.mBitmap.isRecycled()) {
            Rect r = getBounds();
            if (this.mMode == 1 && r.width() == this.mWidth && r.height() == this.mHeight) {
                canvas.drawBitmap(this.mBitmap, (float) r.left, (float) r.top, this.mPaint);
            } else {
                canvas.drawBitmap(this.mBitmap, null, r, this.mPaint);
            }
        }
    }

    public void setColorFilter(ColorFilter cf) {
    }

    public int getOpacity() {
        return -3;
    }

    public void setAlpha(int alpha) {
        this.mAlpha = alpha;
        this.mPaint.setAlpha(alpha);
    }

    public void setFilterBitmap(boolean filterBitmap) {
        this.mPaint.setFilterBitmap(filterBitmap);
        this.mPaint.setAntiAlias(filterBitmap);
    }

    public int getAlpha() {
        return this.mAlpha;
    }

    public int getIntrinsicWidth() {
        return this.mWidth;
    }

    public int getIntrinsicHeight() {
        return this.mHeight;
    }

    public int getMinimumWidth() {
        return this.mWidth;
    }

    public int getMinimumHeight() {
        return this.mHeight;
    }

    public Bitmap getBitmap() {
        return this.mBitmap;
    }

    public void setGhostModeEnabled(boolean enabled) {
        if (this.mGhostModeEnabled != enabled) {
            this.mGhostModeEnabled = enabled;
            updateFilter();
        }
    }

    public int getBrightness() {
        return this.mBrightness;
    }

    public void setBrightness(int brightness) {
        if (this.mBrightness != brightness) {
            this.mBrightness = brightness;
            updateFilter();
            invalidateSelf();
        }
    }

    private void updateFilter() {
        if (this.mGhostModeEnabled) {
            if (sGhostModeMatrix == null) {
                sGhostModeMatrix = new ColorMatrix();
                sGhostModeMatrix.setSaturation(0.0f);
                sTempMatrix.set(new float[]{0.49019608f, 0.0f, 0.0f, 0.0f, 130.0f, 0.0f, 0.49019608f, 0.0f, 0.0f, 130.0f, 0.0f, 0.0f, 0.49019608f, 0.0f, 130.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f});
                sGhostModeMatrix.preConcat(sTempMatrix);
            }
            if (this.mBrightness == 0) {
                this.mPaint.setColorFilter(new ColorMatrixColorFilter(sGhostModeMatrix));
                return;
            }
            setBrightnessMatrix(sTempMatrix, this.mBrightness);
            sTempMatrix.postConcat(sGhostModeMatrix);
            this.mPaint.setColorFilter(new ColorMatrixColorFilter(sTempMatrix));
        } else if (this.mBrightness != 0) {
            ColorFilter filter = (ColorFilter) sCachedBrightnessFilter.get(this.mBrightness);
            if (filter == null) {
                filter = new PorterDuffColorFilter(Color.argb(this.mBrightness, 255, 255, 255), Mode.SRC_ATOP);
                sCachedBrightnessFilter.put(this.mBrightness, filter);
            }
            this.mPaint.setColorFilter(filter);
        } else {
            this.mPaint.setColorFilter(null);
        }
    }

    private static void setBrightnessMatrix(ColorMatrix matrix, int brightness) {
        float scale = 1.0f - (((float) brightness) / 255.0f);
        matrix.setScale(scale, scale, scale, 1.0f);
        float[] array = matrix.getArray();
        array[4] = (float) brightness;
        array[9] = (float) brightness;
        array[14] = (float) brightness;
    }
}

package com.android.launcher3.common.drag;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;

public final class DragOutlineHelper {
    private static final int BLUR_MASK_RADIUS = 2;
    private static final int BRIGHT_OUTLINE_DRAW_COUNT_APPICON = 7;
    private static final int BRIGHT_OUTLINE_DRAW_COUNT_WIDGET = 3;
    private static DragOutlineHelper sInstance;
    private final Paint mBlurPaint = new Paint();
    private final BlurMaskFilter mBrightOuterBlurMaskFilter;
    private final Paint mDrawPaint = new Paint();
    private final Paint mErasePaint = new Paint();
    private final BlurMaskFilter mInnerBlurMaskFilter;
    private final BlurMaskFilter mOuterBlurMaskFilter;

    private DragOutlineHelper(Context context) {
        float scale = context.getResources().getDisplayMetrics().density;
        this.mOuterBlurMaskFilter = new BlurMaskFilter(2.0f, Blur.OUTER);
        this.mInnerBlurMaskFilter = new BlurMaskFilter(2.0f, Blur.SOLID);
        this.mBrightOuterBlurMaskFilter = new BlurMaskFilter(scale, Blur.OUTER);
        this.mDrawPaint.setFilterBitmap(true);
        this.mDrawPaint.setAntiAlias(true);
        this.mBlurPaint.setFilterBitmap(true);
        this.mBlurPaint.setAntiAlias(true);
        this.mErasePaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
        this.mErasePaint.setFilterBitmap(true);
        this.mErasePaint.setAntiAlias(true);
    }

    public static DragOutlineHelper obtain(Context context) {
        if (sInstance == null) {
            sInstance = new DragOutlineHelper(context);
        }
        return sInstance;
    }

    void createWidgetDragOutline(Bitmap srcDst, Canvas srcDstCanvas, int outlineColor) {
        Bitmap glowShape = srcDst.extractAlpha();
        this.mBlurPaint.setMaskFilter(this.mOuterBlurMaskFilter);
        int[] outerBlurOffset = new int[2];
        Bitmap outerBlur = glowShape.extractAlpha(this.mBlurPaint, outerBlurOffset);
        this.mBlurPaint.setMaskFilter(this.mBrightOuterBlurMaskFilter);
        int[] brightOutlineOffset = new int[2];
        Bitmap brightOutline = glowShape.extractAlpha(this.mBlurPaint, brightOutlineOffset);
        srcDstCanvas.setBitmap(srcDst);
        srcDstCanvas.drawColor(0, Mode.CLEAR);
        this.mDrawPaint.setColor(outlineColor);
        srcDstCanvas.drawBitmap(outerBlur, (float) outerBlurOffset[0], (float) outerBlurOffset[1], this.mDrawPaint);
        this.mDrawPaint.setColor(outlineColor);
        for (int i = 0; i < 3; i++) {
            srcDstCanvas.drawBitmap(brightOutline, (float) brightOutlineOffset[0], (float) brightOutlineOffset[1], this.mDrawPaint);
        }
        srcDstCanvas.setBitmap(null);
        brightOutline.recycle();
        outerBlur.recycle();
        glowShape.recycle();
    }

    void createIconDragOutline(Bitmap srcDst, Canvas srcDstCanvas, int outlineColor) {
        this.mBlurPaint.setMaskFilter(this.mBrightOuterBlurMaskFilter);
        int[] outerBlurOffset = new int[2];
        Bitmap outerBlur = srcDst.extractAlpha(this.mBlurPaint, outerBlurOffset);
        this.mBlurPaint.setMaskFilter(this.mInnerBlurMaskFilter);
        int[] innerBlurOffset = new int[2];
        Bitmap innerBlur = srcDst.extractAlpha(this.mBlurPaint, innerBlurOffset);
        srcDstCanvas.setBitmap(srcDst);
        srcDstCanvas.drawColor(0, Mode.CLEAR);
        this.mDrawPaint.setColor(outlineColor);
        this.mDrawPaint.setAntiAlias(true);
        for (int i = 0; i < 7; i++) {
            srcDstCanvas.drawBitmap(outerBlur, (float) outerBlurOffset[0], (float) outerBlurOffset[1], this.mDrawPaint);
        }
        srcDstCanvas.drawBitmap(innerBlur, (float) innerBlurOffset[0], (float) innerBlurOffset[1], this.mErasePaint);
        srcDstCanvas.setBitmap(null);
        outerBlur.recycle();
        innerBlur.recycle();
    }
}

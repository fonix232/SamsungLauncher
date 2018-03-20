package com.android.launcher3.common.drag;

import android.animation.FloatArrayEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.animation.DecelerateInterpolator;
import com.android.launcher3.Launcher;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.common.view.DragLayer.LayoutParams;
import com.android.launcher3.util.animation.AppIconBounceAnimation;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.sec.android.app.launcher.R;
import java.util.Arrays;

public class DragView extends View {
    public static int COLOR_CHANGE_DURATION = 120;
    public static final float EXTRA_VIEW_SCALE_FACTOR = 1.0f;
    public static final int EXTRA_VIEW_SHOW_DURATION = 200;
    public static final int VIEW_SHOW_DURATION = 150;
    public static final int VIEW_ZOOM_DURATION = 150;
    private static float sDragAlpha = 1.0f;
    ValueAnimator mAnim;
    private Bitmap mBitmap;
    private AppIconBounceAnimation mBounceAnim;
    private Bitmap mCrossFadeBitmap;
    private float mCrossFadeProgress;
    private float[] mCurrentFilter;
    private DragLayer mDragLayer;
    private Drawable mDragOutline;
    private Rect mDragRegion;
    private Point mDragVisualizeOffset;
    private ValueAnimator mFilterAnimator;
    private boolean mHasDrawn;
    private int mIntrinsicIconSize;
    private boolean mIsExtraDragView;
    private float mOffsetX;
    private float mOffsetY;
    private Paint mPaint;
    private int mRegistrationX;
    private int mRegistrationY;
    private View mSourceView;
    private int mTargetOffsetX;
    private int mTargetOffsetY;
    private int mTopDelta;
    private int mTouchX;
    private int mTouchY;

    public DragView(Launcher launcher, Bitmap bitmap, int registrationX, int registrationY, int left, int top, int width, int height, float initialScale) {
        this(launcher, bitmap, registrationX, registrationY, left, top, width, height, initialScale, initialScale, false);
    }

    public DragView(Launcher launcher, Bitmap bitmap, int registrationX, int registrationY, int left, int top, int width, int height, final float initialScale, final float scaleFactor, boolean extraDragView) {
        super(launcher);
        this.mDragVisualizeOffset = null;
        this.mDragRegion = null;
        this.mDragLayer = null;
        this.mHasDrawn = false;
        this.mCrossFadeProgress = 0.0f;
        this.mOffsetX = 0.0f;
        this.mOffsetY = 0.0f;
        this.mIntrinsicIconSize = 0;
        this.mDragLayer = launcher.getDragLayer();
        this.mIsExtraDragView = extraDragView;
        setScaleX(initialScale);
        setScaleY(initialScale);
        this.mAnim = LauncherAnimUtils.ofFloat(this, 0.0f, 1.0f);
        this.mAnim.setDuration(this.mIsExtraDragView ? 200 : 150);
        this.mAnim.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = ((Float) animation.getAnimatedValue()).floatValue();
                int deltaX = (int) (((float) DragView.this.mTargetOffsetX) * value);
                int deltaY = (int) (((float) DragView.this.mTargetOffsetY) * value);
                DragView.this.mOffsetX = (float) deltaX;
                DragView.this.mOffsetY = (float) deltaY;
                DragView.this.setScaleX(initialScale + ((scaleFactor - initialScale) * value));
                DragView.this.setScaleY(initialScale + ((scaleFactor - initialScale) * value));
                if (DragView.sDragAlpha != 1.0f) {
                    DragView.this.setAlpha((DragView.sDragAlpha * value) + (1.0f - value));
                }
                if (DragView.this.getParent() == null) {
                    animation.cancel();
                    return;
                }
                DragView.this.setTranslationX((float) ((DragView.this.mTouchX - DragView.this.mRegistrationX) + deltaX));
                DragView.this.setTranslationY((float) ((DragView.this.mTouchY - DragView.this.mRegistrationY) + deltaY));
            }
        });
        this.mBitmap = Bitmap.createBitmap(bitmap, left, top, width, height);
        setDragRegion(new Rect(0, 0, width, height));
        this.mRegistrationX = registrationX;
        this.mRegistrationY = registrationY;
        int ms = MeasureSpec.makeMeasureSpec(0, 0);
        measure(ms, ms);
        this.mPaint = new Paint(2);
        setElevation(getResources().getDimension(R.dimen.drag_elevation));
    }

    public void setIntrinsicIconSize(int iconSize) {
        this.mIntrinsicIconSize = iconSize;
    }

    public int getIntrinsicIconSize() {
        return this.mIntrinsicIconSize;
    }

    public float getOffsetY() {
        return this.mOffsetY;
    }

    public float getOffsetX() {
        return this.mOffsetX;
    }

    public int getDragRegionLeft() {
        return this.mDragRegion.left;
    }

    public int getDragRegionTop() {
        return this.mDragRegion.top;
    }

    public int getDragRegionWidth() {
        return this.mDragRegion.width();
    }

    public int getDragRegionHeight() {
        return this.mDragRegion.height();
    }

    public void setDragVisualizeOffset(Point p) {
        this.mDragVisualizeOffset = p;
    }

    public Point getDragVisualizeOffset() {
        return this.mDragVisualizeOffset;
    }

    public void setDragRegion(Rect r) {
        this.mDragRegion = r;
    }

    public Rect getDragRegion() {
        return this.mDragRegion;
    }

    public int getRegistrationX() {
        return this.mRegistrationX;
    }

    public int getRegistrationY() {
        return this.mRegistrationY;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(this.mBitmap.getWidth(), this.mBitmap.getHeight());
    }

    protected void onDraw(Canvas canvas) {
        boolean crossFade = true;
        this.mHasDrawn = true;
        if (this.mCrossFadeProgress <= 0.0f || this.mCrossFadeBitmap == null) {
            crossFade = false;
        }
        if (crossFade) {
            this.mPaint.setAlpha((int) ((1.0f - this.mCrossFadeProgress) * 255.0f));
        }
        canvas.drawBitmap(this.mBitmap, 0.0f, 0.0f, this.mPaint);
        if (crossFade) {
            this.mPaint.setAlpha((int) (this.mCrossFadeProgress * 255.0f));
            canvas.save();
            canvas.scale((((float) this.mBitmap.getWidth()) * 1.0f) / ((float) this.mCrossFadeBitmap.getWidth()), (((float) this.mBitmap.getHeight()) * 1.0f) / ((float) this.mCrossFadeBitmap.getHeight()));
            canvas.drawBitmap(this.mCrossFadeBitmap, 0.0f, 0.0f, this.mPaint);
            canvas.restore();
        }
    }

    public void setCrossFadeBitmap(Bitmap crossFadeBitmap) {
        this.mCrossFadeBitmap = crossFadeBitmap;
    }

    public void crossFade(int duration) {
        ValueAnimator va = LauncherAnimUtils.ofFloat(this, 0.0f, 1.0f);
        va.setDuration((long) duration);
        va.setInterpolator(new DecelerateInterpolator(1.5f));
        va.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                DragView.this.mCrossFadeProgress = animation.getAnimatedFraction();
            }
        });
        va.start();
    }

    public void setColor(int color) {
        if (this.mPaint == null) {
            this.mPaint = new Paint(2);
        }
        if (color != 0) {
            ColorMatrix m1 = new ColorMatrix();
            m1.setSaturation(0.0f);
            ColorMatrix m2 = new ColorMatrix();
            setColorScale(color, m2);
            m1.postConcat(m2);
            animateFilterTo(m1.getArray());
        } else if (this.mCurrentFilter == null) {
            this.mPaint.setColorFilter(null);
            invalidate();
        } else {
            animateFilterTo(new ColorMatrix().getArray());
        }
    }

    private void animateFilterTo(float[] targetFilter) {
        float[] oldFilter = this.mCurrentFilter == null ? new ColorMatrix().getArray() : this.mCurrentFilter;
        this.mCurrentFilter = Arrays.copyOf(oldFilter, oldFilter.length);
        if (this.mFilterAnimator != null) {
            this.mFilterAnimator.cancel();
        }
        this.mFilterAnimator = ValueAnimator.ofObject(new FloatArrayEvaluator(this.mCurrentFilter), new Object[]{oldFilter, targetFilter});
        this.mFilterAnimator.setDuration((long) COLOR_CHANGE_DURATION);
        this.mFilterAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                DragView.this.mPaint.setColorFilter(new ColorMatrixColorFilter(DragView.this.mCurrentFilter));
                DragView.this.invalidate();
            }
        });
        this.mFilterAnimator.start();
    }

    public boolean hasDrawn() {
        return this.mHasDrawn;
    }

    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        this.mPaint.setAlpha((int) (255.0f * alpha));
        invalidate();
    }

    public void show(int touchX, int touchY) {
        this.mDragLayer.addView(this);
        LayoutParams lp = new LayoutParams(0, 0);
        lp.width = this.mBitmap.getWidth();
        lp.height = this.mBitmap.getHeight();
        lp.customPosition = true;
        setLayoutParams(lp);
        this.mTouchX = touchX;
        this.mTouchY = touchY;
        setTranslationX((float) (touchX - this.mRegistrationX));
        setTranslationY((float) (touchY - this.mRegistrationY));
        post(new Runnable() {
            public void run() {
                DragView.this.mAnim.start();
            }
        });
    }

    public void cancelAnimation() {
        if (this.mAnim != null && this.mAnim.isRunning()) {
            this.mAnim.cancel();
        }
        if (this.mBounceAnim != null) {
            this.mBounceAnim.cancel();
        }
    }

    public void resetLayoutParams() {
        this.mOffsetY = 0.0f;
        this.mOffsetX = 0.0f;
        requestLayout();
    }

    void move(int touchX, int touchY) {
        this.mTouchX = touchX;
        this.mTouchY = touchY;
        setTranslationX((float) ((touchX - this.mRegistrationX) + ((int) this.mOffsetX)));
        setTranslationY((float) ((touchY - this.mRegistrationY) + ((int) this.mOffsetY)));
    }

    void remove() {
        if (getParent() != null) {
            this.mDragLayer.removeView(this);
        }
        this.mBitmap = null;
        this.mDragOutline = null;
    }

    public static void setColorScale(int color, ColorMatrix target) {
        target.setScale(((float) Color.red(color)) / 255.0f, ((float) Color.green(color)) / 255.0f, ((float) Color.blue(color)) / 255.0f, ((float) Color.alpha(color)) / 255.0f);
    }

    public void setDragOutline(Drawable dragOutline) {
        this.mDragOutline = dragOutline;
    }

    public Drawable getDragOutline() {
        return this.mDragOutline;
    }

    public void setTargetOffset(int offsetX, int offsetY) {
        this.mTargetOffsetX = offsetX;
        this.mTargetOffsetY = offsetY;
    }

    public void setSourceView(View source) {
        this.mSourceView = source;
    }

    public View getSourceView() {
        return this.mSourceView;
    }

    public boolean isExtraDragView() {
        return this.mIsExtraDragView;
    }

    public void setTopDelta(int topDelta) {
        this.mTopDelta = topDelta;
    }

    public int getTopDelta() {
        return this.mTopDelta;
    }

    public void animateUp() {
        if (this.mBounceAnim == null) {
            this.mBounceAnim = new AppIconBounceAnimation((View) this, 0.9f);
            this.mBounceAnim.animateUp();
        }
    }
}

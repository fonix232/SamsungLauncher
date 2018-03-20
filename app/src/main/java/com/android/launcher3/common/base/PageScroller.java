package com.android.launcher3.common.base;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

public class PageScroller {
    private static final int FLING_MODE = 1;
    private static final int SCROLL_MODE = 0;
    private int mCurrX;
    private int mCurrY;
    private float mDeltaX;
    private float mDeltaY;
    private int mDuration;
    private float mDurationReciprocal;
    private int mFinalX;
    private int mFinalY;
    private boolean mFinished;
    private TimeInterpolator mInterpolator;
    private int mLeftScreen;
    private int mMode;
    private int mRightScreen;
    private long mStartTime;
    private int mStartX;
    private int mStartY;

    public void setInterpolator(TimeInterpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    public PageScroller(Context context) {
        this(context, null);
    }

    private PageScroller(Context context, Interpolator interpolator) {
        this.mFinished = true;
        this.mInterpolator = interpolator;
    }

    public final boolean isFinished() {
        return this.mFinished;
    }

    public final void forceFinished(boolean finished) {
        this.mFinished = finished;
    }

    public final int getDuration() {
        return this.mDuration;
    }

    public final int getCurrX() {
        return this.mCurrX;
    }

    public final int getCurrY() {
        return this.mCurrY;
    }

    public final int getFinalX() {
        return this.mFinalX;
    }

    public boolean computeScrollOffset() {
        if (this.mFinished) {
            return false;
        }
        int timePassed = (int) (AnimationUtils.currentAnimationTimeMillis() - this.mStartTime);
        if (timePassed < this.mDuration) {
            switch (this.mMode) {
                case 0:
                    float x = ((float) timePassed) * this.mDurationReciprocal;
                    if (this.mInterpolator == null) {
                        x = viscousFluid(x);
                    } else {
                        x = this.mInterpolator.getInterpolation(x);
                    }
                    this.mCurrX = this.mStartX + Math.round(this.mDeltaX * x);
                    this.mCurrY = this.mStartY + Math.round(this.mDeltaY * x);
                    return true;
                case 1:
                    this.mCurrX = this.mStartX + Math.round(((float) (this.mFinalX - this.mStartX)) * 1.0f);
                    this.mCurrY = this.mStartY + Math.round(((float) (this.mFinalY - this.mStartY)) * 1.0f);
                    if (this.mCurrX != this.mFinalX || this.mCurrY != this.mFinalY) {
                        return true;
                    }
                    this.mFinished = true;
                    return true;
                default:
                    return true;
            }
        }
        this.mCurrX = this.mFinalX;
        this.mCurrY = this.mFinalY;
        this.mFinished = true;
        return true;
    }

    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        this.mMode = 0;
        this.mFinished = false;
        this.mDuration = duration;
        this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
        this.mStartX = startX;
        this.mStartY = startY;
        this.mFinalX = startX + dx;
        this.mFinalY = startY + dy;
        this.mDeltaX = (float) dx;
        this.mDeltaY = (float) dy;
        this.mDurationReciprocal = 1.0f / ((float) this.mDuration);
    }

    private static float viscousFluid(float x) {
        if (x < 1.0f) {
            return x - (1.0f - ((float) Math.exp((double) (-x))));
        }
        return 0.36787945f + ((1.0f - 0.36787945f) * (1.0f - ((float) Math.exp((double) (1.0f - x)))));
    }

    public void abortAnimation() {
        this.mCurrX = this.mFinalX;
        this.mCurrY = this.mFinalY;
        this.mFinished = true;
    }

    public int timePassed() {
        return (int) (AnimationUtils.currentAnimationTimeMillis() - this.mStartTime);
    }

    public void setFinalX(int newX) {
        this.mFinalX = newX;
        this.mDeltaX = (float) (this.mFinalX - this.mStartX);
        this.mFinished = false;
    }

    public boolean isUpdatedScreenIndex(int leftScreen, int rightScreen) {
        if (this.mLeftScreen == leftScreen && this.mRightScreen == rightScreen) {
            return false;
        }
        this.mLeftScreen = leftScreen;
        this.mRightScreen = rightScreen;
        return true;
    }
}

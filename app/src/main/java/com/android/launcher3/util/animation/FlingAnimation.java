package com.android.launcher3.util.animation;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.PointF;
import android.graphics.Rect;
import android.view.animation.DecelerateInterpolator;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DragView;
import com.android.launcher3.common.view.DragLayer;

public class FlingAnimation implements AnimatorUpdateListener {
    private static final int DRAG_END_DELAY = 300;
    private static final float MAX_ACCELERATION = 0.5f;
    protected float mAX;
    protected float mAY;
    protected final TimeInterpolator mAlphaInterpolator = new DecelerateInterpolator(0.75f);
    protected final float mAnimationTimeFraction;
    protected final DragLayer mDragLayer;
    protected final DragObject mDragObject;
    protected final int mDuration;
    protected final Rect mFrom;
    protected final Rect mIconRect;
    protected final float mUX;
    protected final float mUY;

    public FlingAnimation(DragObject d, PointF vel, Rect iconRect, DragLayer dragLayer) {
        this.mDragObject = d;
        this.mUX = vel.x / 1000.0f;
        this.mUY = vel.y / 1000.0f;
        this.mIconRect = iconRect;
        this.mDragLayer = dragLayer;
        this.mFrom = new Rect();
        dragLayer.getViewRectRelativeToSelf(d.dragView, this.mFrom);
        float scale = d.dragView.getScaleX();
        float xOffset = ((scale - 1.0f) * ((float) d.dragView.getMeasuredWidth())) / 2.0f;
        float yOffset = ((scale - 1.0f) * ((float) d.dragView.getMeasuredHeight())) / 2.0f;
        Rect rect = this.mFrom;
        rect.left = (int) (((float) rect.left) + xOffset);
        rect = this.mFrom;
        rect.right = (int) (((float) rect.right) - xOffset);
        rect = this.mFrom;
        rect.top = (int) (((float) rect.top) + yOffset);
        rect = this.mFrom;
        rect.bottom = (int) (((float) rect.bottom) - yOffset);
        this.mDuration = initDuration();
        this.mAnimationTimeFraction = ((float) this.mDuration) / ((float) (this.mDuration + 300));
    }

    protected int initDuration() {
        float sY = (float) (-this.mFrom.bottom);
        float d = (this.mUY * this.mUY) + ((2.0f * sY) * MAX_ACCELERATION);
        if (d >= 0.0f) {
            this.mAY = MAX_ACCELERATION;
        } else {
            d = 0.0f;
            this.mAY = (this.mUY * this.mUY) / ((-sY) * 2.0f);
        }
        double t = (((double) (-this.mUY)) - Math.sqrt((double) d)) / ((double) this.mAY);
        this.mAX = (float) (((((double) ((-this.mFrom.exactCenterX()) + this.mIconRect.exactCenterX())) - (((double) this.mUX) * t)) * 2.0d) / (t * t));
        return (int) Math.round(t);
    }

    public final int getDuration() {
        return this.mDuration + 300;
    }

    public void onAnimationUpdate(ValueAnimator animation) {
        float t = animation.getAnimatedFraction();
        if (t > this.mAnimationTimeFraction) {
            t = 1.0f;
        } else {
            t /= this.mAnimationTimeFraction;
        }
        DragView dragView = (DragView) this.mDragLayer.getAnimatedView();
        float time = t * ((float) this.mDuration);
        dragView.setTranslationX(((this.mUX * time) + ((float) this.mFrom.left)) + (((this.mAX * time) * time) / 2.0f));
        dragView.setTranslationY(((this.mUY * time) + ((float) this.mFrom.top)) + (((this.mAY * time) * time) / 2.0f));
        dragView.setAlpha(1.0f - this.mAlphaInterpolator.getInterpolation(t));
    }
}

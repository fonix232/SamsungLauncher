package com.android.launcher3.common.base;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.android.launcher3.common.base.view.BaseRecyclerView;
import com.sec.android.app.launcher.R;

public class BaseRecyclerViewFastScrollBar {
    private static final int MAX_TRACK_ALPHA = 30;
    private static final int SCROLL_BAR_VIS_DURATION = 150;
    private boolean mCanThumbDetach;
    private boolean mIgnoreDragGesture;
    private Rect mInvalidateRect = new Rect();
    private boolean mIsDragging;
    private boolean mIsThumbDetached;
    private float mLastTouchY;
    private BaseRecyclerViewFastScrollPopup mPopup;
    private BaseRecyclerView mRv;
    private AnimatorSet mScrollbarAnimator;
    private int mThumbActiveColor;
    private int mThumbCurvature;
    private int mThumbHeight;
    private int mThumbInactiveColor;
    private int mThumbMaxWidth;
    private int mThumbMinWidth;
    private Point mThumbOffset = new Point(-1, -1);
    private Paint mThumbPaint;
    private Path mThumbPath = new Path();
    private int mThumbWidth;
    private Rect mTmpRect = new Rect();
    private int mTouchInset;
    private int mTouchOffset;
    private Paint mTrackPaint;
    private int mTrackWidth;

    public interface FastScrollFocusableView {
        void setFastScrollFocused(boolean z, boolean z2);
    }

    public BaseRecyclerViewFastScrollBar(BaseRecyclerView rv, Resources res) {
        this.mRv = rv;
        this.mPopup = new BaseRecyclerViewFastScrollPopup(rv, res);
        this.mTrackPaint = new Paint();
        this.mTrackPaint.setColor(rv.getFastScrollerTrackColor(-16777216));
        this.mTrackPaint.setAlpha(30);
        this.mThumbInactiveColor = rv.getFastScrollerThumbInactiveColor(res.getColor(R.color.container_fastscroll_thumb_inactive_color));
        this.mThumbActiveColor = res.getColor(R.color.container_fastscroll_thumb_active_color);
        this.mThumbPaint = new Paint();
        this.mThumbPaint.setAntiAlias(true);
        this.mThumbPaint.setColor(this.mThumbInactiveColor);
        this.mThumbPaint.setStyle(Style.FILL);
        int dimensionPixelSize = res.getDimensionPixelSize(R.dimen.container_fastscroll_thumb_min_width);
        this.mThumbMinWidth = dimensionPixelSize;
        this.mThumbWidth = dimensionPixelSize;
        this.mThumbMaxWidth = res.getDimensionPixelSize(R.dimen.container_fastscroll_thumb_max_width);
        this.mThumbHeight = res.getDimensionPixelSize(R.dimen.container_fastscroll_thumb_height);
        this.mThumbCurvature = this.mThumbMaxWidth - this.mThumbMinWidth;
        this.mTouchInset = res.getDimensionPixelSize(R.dimen.container_fastscroll_thumb_touch_inset);
    }

    public void setDetachThumbOnFastScroll() {
        this.mCanThumbDetach = true;
    }

    public void reattachThumbToScroll() {
        this.mIsThumbDetached = false;
    }

    public void setThumbOffset(int x, int y) {
        if (this.mThumbOffset.x != x || this.mThumbOffset.y != y) {
            this.mInvalidateRect.set(this.mThumbOffset.x - this.mThumbCurvature, this.mThumbOffset.y, this.mThumbOffset.x + this.mThumbWidth, this.mThumbOffset.y + this.mThumbHeight);
            this.mThumbOffset.set(x, y);
            updateThumbPath();
            this.mInvalidateRect.union(this.mThumbOffset.x - this.mThumbCurvature, this.mThumbOffset.y, this.mThumbOffset.x + this.mThumbWidth, this.mThumbOffset.y + this.mThumbHeight);
            this.mRv.invalidate(this.mInvalidateRect);
        }
    }

    public Point getThumbOffset() {
        return this.mThumbOffset;
    }

    public void setThumbWidth(int width) {
        this.mInvalidateRect.set(this.mThumbOffset.x - this.mThumbCurvature, this.mThumbOffset.y, this.mThumbOffset.x + this.mThumbWidth, this.mThumbOffset.y + this.mThumbHeight);
        this.mThumbWidth = width;
        updateThumbPath();
        this.mInvalidateRect.union(this.mThumbOffset.x - this.mThumbCurvature, this.mThumbOffset.y, this.mThumbOffset.x + this.mThumbWidth, this.mThumbOffset.y + this.mThumbHeight);
        this.mRv.invalidate(this.mInvalidateRect);
    }

    public int getThumbWidth() {
        return this.mThumbWidth;
    }

    public void setTrackWidth(int width) {
        this.mInvalidateRect.set(this.mThumbOffset.x - this.mThumbCurvature, 0, this.mThumbOffset.x + this.mThumbWidth, this.mRv.getHeight());
        this.mTrackWidth = width;
        updateThumbPath();
        this.mInvalidateRect.union(this.mThumbOffset.x - this.mThumbCurvature, 0, this.mThumbOffset.x + this.mThumbWidth, this.mRv.getHeight());
        this.mRv.invalidate(this.mInvalidateRect);
    }

    public int getTrackWidth() {
        return this.mTrackWidth;
    }

    public int getThumbHeight() {
        return this.mThumbHeight;
    }

    public int getThumbMaxWidth() {
        return this.mThumbMaxWidth;
    }

    public float getLastTouchY() {
        return this.mLastTouchY;
    }

    public boolean isDraggingThumb() {
        return this.mIsDragging;
    }

    public boolean isThumbDetached() {
        return this.mIsThumbDetached;
    }

    public void handleTouchEvent(MotionEvent ev, int downX, int downY, int lastY) {
        ViewConfiguration config = ViewConfiguration.get(this.mRv.getContext());
        int y = (int) ev.getY();
        switch (ev.getAction()) {
            case 0:
                if (isNearThumb(downX, downY)) {
                    this.mTouchOffset = downY - this.mThumbOffset.y;
                    return;
                }
                return;
            case 1:
            case 3:
                this.mTouchOffset = 0;
                this.mLastTouchY = 0.0f;
                this.mIgnoreDragGesture = false;
                if (this.mIsDragging) {
                    this.mIsDragging = false;
                    this.mPopup.animateVisibility(false);
                    animateScrollbar(false);
                    return;
                }
                return;
            case 2:
                this.mIgnoreDragGesture = (Math.abs(y - downY) > config.getScaledPagingTouchSlop() ? 1 : 0) | this.mIgnoreDragGesture;
                if (!this.mIsDragging && !this.mIgnoreDragGesture && isNearThumb(downX, lastY) && Math.abs(y - downY) > config.getScaledTouchSlop()) {
                    this.mRv.getParent().requestDisallowInterceptTouchEvent(true);
                    this.mIsDragging = true;
                    if (this.mCanThumbDetach) {
                        this.mIsThumbDetached = true;
                    }
                    this.mTouchOffset += lastY - downY;
                    this.mPopup.animateVisibility(true);
                    animateScrollbar(true);
                }
                if (this.mIsDragging) {
                    int top = this.mRv.getBackgroundPadding().top;
                    int bottom = (this.mRv.getHeight() - this.mRv.getBackgroundPadding().bottom) - this.mThumbHeight;
                    float boundedY = (float) Math.max(top, Math.min(bottom, y - this.mTouchOffset));
                    String sectionName = this.mRv.scrollToPositionAtProgress((boundedY - ((float) top)) / ((float) (bottom - top)));
                    this.mPopup.setSectionName(sectionName);
                    this.mPopup.animateVisibility(!sectionName.isEmpty());
                    this.mRv.invalidate(this.mPopup.updateFastScrollerBounds(this.mRv, lastY));
                    this.mLastTouchY = boundedY;
                    return;
                }
                return;
            default:
                return;
        }
    }

    public void draw(Canvas canvas) {
        if (this.mThumbOffset.x >= 0 && this.mThumbOffset.y >= 0) {
            if (this.mTrackPaint.getAlpha() > 0) {
                canvas.drawRect((float) this.mThumbOffset.x, 0.0f, (float) (this.mThumbOffset.x + this.mThumbWidth), (float) this.mRv.getHeight(), this.mTrackPaint);
            }
            canvas.drawPath(this.mThumbPath, this.mThumbPaint);
            this.mPopup.draw(canvas);
        }
    }

    private void animateScrollbar(boolean isScrolling) {
        if (this.mScrollbarAnimator != null) {
            this.mScrollbarAnimator.cancel();
        }
        this.mScrollbarAnimator = new AnimatorSet();
        String str = "trackWidth";
        int[] iArr = new int[1];
        iArr[0] = isScrolling ? this.mThumbMaxWidth : this.mThumbMinWidth;
        ObjectAnimator trackWidthAnim = ObjectAnimator.ofInt(this, str, iArr);
        str = "thumbWidth";
        iArr = new int[1];
        iArr[0] = isScrolling ? this.mThumbMaxWidth : this.mThumbMinWidth;
        ObjectAnimator thumbWidthAnim = ObjectAnimator.ofInt(this, str, iArr);
        this.mScrollbarAnimator.playTogether(new Animator[]{trackWidthAnim, thumbWidthAnim});
        if (this.mThumbActiveColor != this.mThumbInactiveColor) {
            TypeEvaluator argbEvaluator = new ArgbEvaluator();
            Object[] objArr = new Object[2];
            objArr[0] = Integer.valueOf(this.mThumbPaint.getColor());
            objArr[1] = Integer.valueOf(isScrolling ? this.mThumbActiveColor : this.mThumbInactiveColor);
            ValueAnimator colorAnimation = ValueAnimator.ofObject(argbEvaluator, objArr);
            colorAnimation.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animator) {
                    BaseRecyclerViewFastScrollBar.this.mThumbPaint.setColor(((Integer) animator.getAnimatedValue()).intValue());
                    BaseRecyclerViewFastScrollBar.this.mRv.invalidate(BaseRecyclerViewFastScrollBar.this.mThumbOffset.x, BaseRecyclerViewFastScrollBar.this.mThumbOffset.y, BaseRecyclerViewFastScrollBar.this.mThumbOffset.x + BaseRecyclerViewFastScrollBar.this.mThumbWidth, BaseRecyclerViewFastScrollBar.this.mThumbOffset.y + BaseRecyclerViewFastScrollBar.this.mThumbHeight);
                }
            });
            this.mScrollbarAnimator.play(colorAnimation);
        }
        this.mScrollbarAnimator.setDuration(150);
        this.mScrollbarAnimator.start();
    }

    private void updateThumbPath() {
        this.mThumbCurvature = this.mThumbMaxWidth - this.mThumbWidth;
        this.mThumbPath.reset();
        this.mThumbPath.moveTo((float) (this.mThumbOffset.x + this.mThumbWidth), (float) this.mThumbOffset.y);
        this.mThumbPath.lineTo((float) (this.mThumbOffset.x + this.mThumbWidth), (float) (this.mThumbOffset.y + this.mThumbHeight));
        this.mThumbPath.lineTo((float) this.mThumbOffset.x, (float) (this.mThumbOffset.y + this.mThumbHeight));
        this.mThumbPath.cubicTo((float) this.mThumbOffset.x, (float) (this.mThumbOffset.y + this.mThumbHeight), (float) (this.mThumbOffset.x - this.mThumbCurvature), (float) (this.mThumbOffset.y + (this.mThumbHeight / 2)), (float) this.mThumbOffset.x, (float) this.mThumbOffset.y);
        this.mThumbPath.close();
    }

    private boolean isNearThumb(int x, int y) {
        this.mTmpRect.set(this.mThumbOffset.x, this.mThumbOffset.y, this.mThumbOffset.x + this.mThumbWidth, this.mThumbOffset.y + this.mThumbHeight);
        this.mTmpRect.inset(this.mTouchInset, this.mTouchInset);
        return this.mTmpRect.contains(x, y);
    }
}

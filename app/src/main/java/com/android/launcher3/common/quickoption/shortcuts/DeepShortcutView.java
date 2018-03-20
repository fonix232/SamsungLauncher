package com.android.launcher3.common.quickoption.shortcuts;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.animation.LogAccelerateInterpolator;
import com.android.launcher3.util.animation.PillRevealOutlineProvider;
import com.android.launcher3.util.animation.PillWidthRevealOutlineProvider;
import com.sec.android.app.launcher.R;

public class DeepShortcutView extends FrameLayout implements AnimatorUpdateListener {
    private static final Point sTempPoint = new Point();
    private DeepShortcutTextView mBubbleText;
    private View mIconView;
    private DeepShortcutsContainer.UnbadgedShortcutInfo mInfo;
    private float mOpenAnimationProgress;
    private final Rect mPillRect;

    private static class CloseInterpolator extends LogAccelerateInterpolator {
        private final float mRemainingProgress;
        private final float mStartProgress;

        CloseInterpolator(float openAnimationProgress) {
            super(100, 0);
            this.mStartProgress = 1.0f - openAnimationProgress;
            this.mRemainingProgress = openAnimationProgress;
        }

        public float getInterpolation(float v) {
            return this.mStartProgress + (super.getInterpolation(v) * this.mRemainingProgress);
        }
    }

    private static class ZoomRevealOutlineProvider extends PillRevealOutlineProvider {
        private final float mFullHeight;
        private final boolean mPivotLeft;
        private final View mTranslateView;
        private final float mTranslateX;
        private final float mTranslateYMultiplier;
        private final View mZoomView;

        ZoomRevealOutlineProvider(int x, int y, Rect pillRect, View translateView, View zoomView, boolean isContainerAboveIcon, boolean pivotLeft) {
            super(x, y, pillRect);
            this.mTranslateView = translateView;
            this.mZoomView = zoomView;
            this.mFullHeight = (float) pillRect.height();
            this.mTranslateYMultiplier = isContainerAboveIcon ? 0.5f : -0.5f;
            this.mPivotLeft = pivotLeft;
            this.mTranslateX = pivotLeft ? ((float) pillRect.height()) / 2.0f : ((float) pillRect.right) - (((float) pillRect.height()) / 2.0f);
        }

        public void setProgress(float progress) {
            super.setProgress(progress);
            this.mZoomView.setScaleX(progress);
            this.mZoomView.setScaleY(progress);
            float height = (float) this.mOutline.height();
            this.mTranslateView.setTranslationY(this.mTranslateYMultiplier * (this.mFullHeight - height));
            this.mTranslateView.setTranslationX(this.mTranslateX - (this.mPivotLeft ? ((float) this.mOutline.left) + (height / 2.0f) : ((float) this.mOutline.right) - (height / 2.0f)));
        }
    }

    public DeepShortcutView(Context context) {
        this(context, null, 0);
    }

    public DeepShortcutView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeepShortcutView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mPillRect = new Rect();
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mIconView = findViewById(R.id.deep_shortcut_icon);
        this.mBubbleText = (DeepShortcutTextView) findViewById(R.id.deep_shortcut);
    }

    public DeepShortcutTextView getBubbleText() {
        return this.mBubbleText;
    }

    public void setWillDrawIcon(boolean willDraw) {
        this.mIconView.setVisibility(willDraw ? View.VISIBLE : View.INVISIBLE);
    }

    public boolean willDrawIcon() {
        return this.mIconView.getVisibility() == View.VISIBLE;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.mPillRect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
    }

    void applyShortcutInfo(DeepShortcutsContainer.UnbadgedShortcutInfo info, DeepShortcutsContainer container) {
        this.mInfo = info;
        this.mBubbleText.applyFromShortcutInfo(info, LauncherAppState.getInstance().getIconCache());
        this.mIconView.setBackground(this.mBubbleText.getIcon());
        this.mBubbleText.setText(info.mDetail.getShortLabel());
        this.mBubbleText.setOnClickListener((Launcher) getContext());
        this.mBubbleText.setOnLongClickListener(container);
        this.mBubbleText.setOnTouchListener(container);
    }

    public View getIconView() {
        return this.mIconView;
    }

    public Animator createOpenAnimation(boolean isContainerAboveIcon, boolean pivotLeft) {
        Point center = getIconCenter();
        ValueAnimator openAnimator = new ZoomRevealOutlineProvider(center.x, center.y, this.mPillRect, this, this.mIconView, isContainerAboveIcon, pivotLeft).createRevealAnimator(this, false);
        this.mOpenAnimationProgress = 0.0f;
        openAnimator.addUpdateListener(this);
        return openAnimator;
    }

    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        this.mOpenAnimationProgress = valueAnimator.getAnimatedFraction();
    }

    public boolean isOpenOrOpening() {
        return this.mOpenAnimationProgress > 0.0f;
    }

    public Animator createCloseAnimation(boolean isContainerAboveIcon, boolean pivotLeft, long duration) {
        Point center = getIconCenter();
        ValueAnimator closeAnimator = new ZoomRevealOutlineProvider(center.x, center.y, this.mPillRect, this, this.mIconView, isContainerAboveIcon, pivotLeft).createRevealAnimator(this, true);
        closeAnimator.setDuration((long) (((float) duration) * this.mOpenAnimationProgress));
        closeAnimator.setInterpolator(new CloseInterpolator(this.mOpenAnimationProgress));
        return closeAnimator;
    }

    public Animator collapseToIcon() {
        int halfHeight = getMeasuredHeight() / 2;
        int iconCenterX = getIconCenter().x;
        return new PillWidthRevealOutlineProvider(this.mPillRect, iconCenterX - halfHeight, iconCenterX + halfHeight).createRevealAnimator(this, true);
    }

    public Point getIconCenter() {
        Point point = sTempPoint;
        int measuredHeight = getMeasuredHeight() / 2;
        sTempPoint.x = measuredHeight;
        point.y = measuredHeight;
        if (Utilities.sIsRtl) {
            sTempPoint.x = getMeasuredWidth() - sTempPoint.x;
        }
        return sTempPoint;
    }
}

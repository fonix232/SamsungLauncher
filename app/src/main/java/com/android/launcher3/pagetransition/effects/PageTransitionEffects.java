package com.android.launcher3.pagetransition.effects;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.LauncherAppState;
import com.sec.android.app.launcher.R;

public abstract class PageTransitionEffects {
    private static final float OVERSCROLL_DAMP_FACTOR = 0.14f;
    protected static final float PAGE_ROTATION = 15.0f;
    protected static final boolean PERFORM_OVERSCROLL_ROTATION = true;
    private static final String TAG = "PageTransitionEffects";
    protected static float TRANSITION_PIVOT = 0.5f;
    static final float sPageZoomScaleLimit = 0.7f;
    public static float transition_rotation_max = 0.0f;
    private int mDragBarSize;
    protected float mDragScrollDrawInward = sPageZoomScaleLimit;
    private int mEditModePanel_left_Adjust = 0;
    private int mEditModePanel_top_Adjust = 0;
    protected float mEditModeShrinkFactor;
    protected boolean mEndPage = false;
    protected float mFastScrollDrawInward = 0.45f;
    protected float mNormalScrollDrawInward = 0.2f;
    protected float mShrinkTranslateX;
    protected float mShrinkTranslateY;

    public abstract void applyTransform(View view, float f, int i);

    public PageTransitionEffects() {
        Resources res = LauncherAppState.getInstance().getContext().getResources();
        this.mEditModeShrinkFactor = ((float) res.getInteger(R.integer.config_workspaceOverviewShrinkPercentage)) / 100.0f;
        this.mNormalScrollDrawInward = res.getFraction(R.fraction.config_workspaceNormalScrollInwardFactor, 1, 1);
        this.mDragScrollDrawInward = res.getFraction(R.fraction.config_workspaceDragScrollInwardFactor, 1, 1);
        transition_rotation_max = (float) res.getInteger(R.integer.transition_rotation_max);
        this.mDragBarSize = res.getDimensionPixelSize(R.dimen.home_editTitleBar);
        this.mEditModePanel_left_Adjust = res.getDimensionPixelSize(R.dimen.workspaceEditModeShrink_left_adjust);
        this.mEditModePanel_top_Adjust = res.getDimensionPixelSize(R.dimen.workspaceEditModeShrink_top_adjust);
    }

    public PageTransitionEffects(Context context) {
    }

    static float mix(float x, float y, float mix) {
        return ((1.0f - mix) * x) + (y * mix);
    }

    protected float backgroundAlphaThreshold(float r) {
        if (r < 0.0f) {
            return 0.0f;
        }
        if (r > 0.3f) {
            return 1.0f;
        }
        return (r - 0.0f) / (0.3f - 0.0f);
    }

    protected boolean isLoopingEnabled() {
        return false;
    }

    private float overScrollInfluenceCurve(float f) {
        f -= 1.0f;
        return ((f * f) * f) + 1.0f;
    }

    protected float maxOverScroll() {
        return OVERSCROLL_DAMP_FACTOR * ((1.0f / Math.abs(1.0f)) * overScrollInfluenceCurve(Math.abs(1.0f)));
    }

    public void reset(View page) {
        page.setPivotX(((float) page.getWidth()) / 2.0f);
        page.setPivotY(((float) page.getHeight()) / 2.0f);
        page.setTranslationX(0.0f);
        page.setTranslationY(0.0f);
        page.setScaleX(1.0f);
        page.setScaleY(1.0f);
        page.setRotationX(0.0f);
        page.setRotationY(0.0f);
        page.setAlpha(1.0f);
        page.setRotation(0.0f);
    }

    public void onLayout(ViewGroup workspace, boolean changed, int left, int top, int right, int bottom) {
        this.mShrinkTranslateX = 0.0f;
        this.mShrinkTranslateY = 0.0f;
        View child = workspace.getChildAt(0);
        if (child != null) {
            float maxShrinkAmount = ((float) child.getHeight()) * (1.0f - this.mEditModeShrinkFactor);
            float maxXlate = maxShrinkAmount * 0.5f;
            if (maxShrinkAmount > ((float) this.mDragBarSize)) {
                maxXlate -= (maxShrinkAmount - ((float) this.mDragBarSize)) * 0.5f;
            }
            this.mShrinkTranslateY = maxXlate / (1.0f - this.mEditModeShrinkFactor);
        }
        this.mShrinkTranslateX += (float) this.mEditModePanel_left_Adjust;
        this.mShrinkTranslateY += (float) this.mEditModePanel_top_Adjust;
    }
}

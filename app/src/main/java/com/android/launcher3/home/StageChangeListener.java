package com.android.launcher3.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

/* compiled from: HomeTransitionAnimation */
class StageChangeListener extends AnimatorListenerAdapter {
    private boolean mAccessibilityEnabled;
    private boolean mShow;
    private View mView;

    StageChangeListener(View v, boolean accessibilityEnabled, boolean show) {
        this.mView = v;
        this.mAccessibilityEnabled = accessibilityEnabled;
        this.mShow = show;
    }

    public void onAnimationEnd(Animator arg0) {
        int alpha = this.mShow ? 1 : 0;
        this.mView.setScaleX(1.0f);
        this.mView.setScaleY(1.0f);
        this.mView.setAlpha((float) alpha);
        AlphaUpdateListener.updateVisibility(this.mView, this.mAccessibilityEnabled);
    }

    public void onAnimationStart(Animator arg0) {
        this.mView.setVisibility(View.VISIBLE);
    }
}

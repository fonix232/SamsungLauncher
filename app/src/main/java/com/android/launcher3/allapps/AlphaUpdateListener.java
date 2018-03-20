package com.android.launcher3.allapps;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

/* compiled from: AppsTransitionAnimation */
class AlphaUpdateListener extends AnimatorListenerAdapter {
    private static final float ALPHA_CUTOFF_THRESHOLD = 0.01f;
    private final boolean mAccessibilityEnabled;
    private final View mView;

    public AlphaUpdateListener(View v, boolean accessibilityEnabled) {
        this.mView = v;
        this.mAccessibilityEnabled = accessibilityEnabled;
    }

    public static void updateVisibility(View view, boolean accessibilityEnabled) {
        int invisibleState = accessibilityEnabled ? 8 : 4;
        if (view.getAlpha() < ALPHA_CUTOFF_THRESHOLD && view.getVisibility() != invisibleState) {
            view.setVisibility(invisibleState);
        } else if (view.getAlpha() > ALPHA_CUTOFF_THRESHOLD && view.getVisibility() != 0) {
            view.setVisibility(View.VISIBLE);
        }
    }

    public void onAnimationEnd(Animator arg0) {
        updateVisibility(this.mView, this.mAccessibilityEnabled);
    }

    public void onAnimationStart(Animator arg0) {
        this.mView.setVisibility(View.VISIBLE);
    }
}

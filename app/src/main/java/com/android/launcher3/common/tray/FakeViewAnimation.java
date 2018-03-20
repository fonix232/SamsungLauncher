package com.android.launcher3.common.tray;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import java.util.HashMap;

class FakeViewAnimation {
    private HashMap<View, Animator> mFakeViewAnimators = new HashMap();

    FakeViewAnimation() {
    }

    public void animate(View targetView, int toTranslationY, boolean toBeShown, int duration) {
        long j = 0;
        if (targetView != null) {
            Animator oldAnimation = (Animator) this.mFakeViewAnimators.get(targetView);
            if (oldAnimation != null) {
                if ((oldAnimation.getStartDelay() > 0) == toBeShown) {
                    return;
                }
            }
            cancel(targetView);
            Animator animation = getAlphaAnimation(targetView, toTranslationY, toBeShown);
            animation.setInterpolator(ViInterpolator.getInterploator(30));
            animation.setDuration((long) duration);
            if (toBeShown) {
                j = 1;
            }
            animation.setStartDelay(j);
            animation.start();
            this.mFakeViewAnimators.put(targetView, animation);
        }
    }

    public void cancel(View targetView) {
        Animator animation = (Animator) this.mFakeViewAnimators.get(targetView);
        if (animation != null) {
            this.mFakeViewAnimators.remove(targetView);
            animation.cancel();
        }
    }

    private Animator getAlphaAnimation(View targetView, int toTranslationY, boolean toBeShown) {
        final float alpha = toBeShown ? 1.0f : 0.0f;
        Animator animation = LauncherAnimUtils.ofFloat(targetView, View.ALPHA.getName(), alpha);
        final View view = targetView;
        final boolean z = toBeShown;
        final int i = toTranslationY;
        animation.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                if (view != null && z) {
                    view.setTranslationY((float) i);
                    view.setVisibility(View.VISIBLE);
                }
            }

            public void onAnimationEnd(Animator animation) {
                FakeViewAnimation.this.mFakeViewAnimators.remove(view);
                if (view != null && !z) {
                    view.setVisibility(View.GONE);
                    view.setTranslationY((float) i);
                }
            }

            public void onAnimationCancel(Animator animation) {
                if (view != null && !z) {
                    view.setAlpha(alpha);
                }
            }
        });
        return animation;
    }
}

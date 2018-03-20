package com.android.launcher3.common.quickoption;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import com.android.launcher3.home.LauncherAppWidgetHostView;

final class ItemRemoveAnimation {
    private AnimatorSet mAnimatorSet;
    private boolean mHasCanceled = false;
    private View mView;

    ItemRemoveAnimation(View v) {
        this.mView = v;
        this.mAnimatorSet = new AnimatorSet();
    }

    void animate() {
        this.mView.clearAnimation();
        this.mView.setPivotX(((float) this.mView.getWidth()) / 2.0f);
        this.mView.setPivotY(((float) this.mView.getHeight()) / 2.0f);
        AnimatorSet animatorSet;
        Animator[] animatorArr;
        if (this.mView instanceof LauncherAppWidgetHostView) {
            LauncherAppWidgetHostView widgetHostView = this.mView;
            widgetHostView.mHasSetPivot = true;
            widgetHostView.setTranslationX(((float) (widgetHostView.getResizeResult().visibleWidth - this.mView.getWidth())) / 2.0f);
            widgetHostView.setTranslationY(((float) (widgetHostView.getResizeResult().visibleHeight - this.mView.getHeight())) / 2.0f);
            float scale = widgetHostView.getResizeResult().scaleToResize;
            animatorSet = this.mAnimatorSet;
            animatorArr = new Animator[3];
            animatorArr[0] = ObjectAnimator.ofFloat(this.mView, View.SCALE_X.getName(), new float[]{scale, scale / 2.0f});
            animatorArr[1] = ObjectAnimator.ofFloat(this.mView, View.SCALE_Y.getName(), new float[]{scale, scale / 2.0f});
            animatorArr[2] = ObjectAnimator.ofFloat(this.mView, View.ALPHA.getName(), new float[]{0.0f});
            animatorSet.playTogether(animatorArr);
        } else {
            animatorSet = this.mAnimatorSet;
            animatorArr = new Animator[5];
            animatorArr[0] = ObjectAnimator.ofFloat(this.mView, View.TRANSLATION_X.getName(), new float[]{1.0f});
            animatorArr[1] = ObjectAnimator.ofFloat(this.mView, View.TRANSLATION_Y.getName(), new float[]{1.0f});
            animatorArr[2] = ObjectAnimator.ofFloat(this.mView, View.SCALE_X.getName(), new float[]{0.5f});
            animatorArr[3] = ObjectAnimator.ofFloat(this.mView, View.SCALE_Y.getName(), new float[]{0.5f});
            animatorArr[4] = ObjectAnimator.ofFloat(this.mView, View.ALPHA.getName(), new float[]{0.0f});
            animatorSet.playTogether(animatorArr);
        }
        this.mAnimatorSet.setDuration(200);
        this.mAnimatorSet.start();
    }

    AnimatorSet getAnimatorSet() {
        return this.mAnimatorSet;
    }

    public void cancel() {
        if (this.mAnimatorSet != null) {
            this.mAnimatorSet.cancel();
            this.mHasCanceled = true;
        }
    }

    boolean hasCanceled() {
        return this.mHasCanceled;
    }
}

package com.android.launcher3.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.util.Property;

public class PropertyResetListener<T, V> extends AnimatorListenerAdapter {
    private Property<T, V> mPropertyToReset;
    private V mResetToValue;

    public PropertyResetListener(Property<T, V> propertyToReset, V resetToValue) {
        this.mPropertyToReset = propertyToReset;
        this.mResetToValue = resetToValue;
    }

    public void onAnimationEnd(Animator animation) {
        this.mPropertyToReset.set(((ObjectAnimator) animation).getTarget(), this.mResetToValue);
    }
}

package com.android.launcher3.util.animation;

import android.animation.TimeInterpolator;

public class LogDecelerateInterpolator implements TimeInterpolator {
    int mBase;
    int mDrift;
    final float mLogScale = (1.0f / computeLog(1.0f, this.mBase, this.mDrift));

    public LogDecelerateInterpolator(int base, int drift) {
        this.mBase = base;
        this.mDrift = drift;
    }

    static float computeLog(float t, int base, int drift) {
        return (((float) (-Math.pow((double) base, (double) (-t)))) + 1.0f) + (((float) drift) * t);
    }

    public float getInterpolation(float t) {
        return computeLog(t, this.mBase, this.mDrift) * this.mLogScale;
    }
}

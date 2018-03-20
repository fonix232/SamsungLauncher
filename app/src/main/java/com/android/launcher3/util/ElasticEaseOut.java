package com.android.launcher3.util;

import android.view.animation.Interpolator;

public final class ElasticEaseOut implements Interpolator {
    private float mAmplitude;
    private float mPeriod;

    public ElasticEaseOut(float amplitude, float period) {
        this.mAmplitude = amplitude;
        this.mPeriod = period;
    }

    public float getInterpolation(float t) {
        return out(t, this.mAmplitude, this.mPeriod);
    }

    private float out(float t, float a, float p) {
        if (t == 0.0f) {
            return 0.0f;
        }
        if (t >= 1.0f) {
            return 1.0f;
        }
        float s;
        if (p == 0.0f) {
            p = 0.3f;
        }
        if (a == 0.0f || a < 1.0f) {
            a = 1.0f;
            s = p / 4.0f;
        } else {
            s = (float) (Math.asin((double) (1.0f / a)) * (((double) p) / 6.283185307179586d));
        }
        return (float) (((((double) a) * Math.pow(2.0d, (double) (-10.0f * t))) * Math.sin((((double) (t - s)) * 6.283185307179586d) / ((double) p))) + 1.0d);
    }
}

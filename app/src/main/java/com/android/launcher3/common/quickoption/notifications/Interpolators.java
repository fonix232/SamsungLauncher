package com.android.launcher3.common.quickoption.notifications;

import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

class Interpolators {
    static final Interpolator FAST_OUT_LINEAR_IN = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);
    static final Interpolator FAST_OUT_SLOW_IN = new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f);
    static final Interpolator LINEAR_OUT_SLOW_IN = new PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f);

    Interpolators() {
    }
}

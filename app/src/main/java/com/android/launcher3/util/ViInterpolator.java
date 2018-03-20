package com.android.launcher3.util;

import android.view.animation.PathInterpolator;

public final class ViInterpolator {
    public static final int ACCELERATION = 1;
    public static final int DECELERATION = 2;
    public static final int SHARP = 3;
    public static final int SINE_IN_33 = 10;
    public static final int SINE_IN_50 = 11;
    public static final int SINE_IN_60 = 12;
    public static final int SINE_IN_70 = 13;
    public static final int SINE_IN_80 = 14;
    public static final int SINE_IN_90 = 15;
    public static final int SINE_IN_OUT_33 = 30;
    public static final int SINE_IN_OUT_50 = 31;
    public static final int SINE_IN_OUT_60 = 32;
    public static final int SINE_IN_OUT_70 = 33;
    public static final int SINE_IN_OUT_80 = 34;
    public static final int SINE_IN_OUT_90 = 35;
    public static final int SINE_OUT_33 = 20;
    public static final int SINE_OUT_50 = 21;
    public static final int SINE_OUT_60 = 22;
    public static final int SINE_OUT_70 = 23;
    public static final int SINE_OUT_80 = 24;
    public static final int SINE_OUT_90 = 25;
    public static final int STANDARD = 0;

    public static PathInterpolator getInterploator(int param) {
        float x1;
        float y1;
        float x2;
        float y2;
        switch (param) {
            case 0:
                x1 = 0.4f;
                y1 = 0.0f;
                x2 = 0.2f;
                y2 = 1.0f;
                break;
            case 1:
                x1 = 0.4f;
                y1 = 0.0f;
                x2 = 1.0f;
                y2 = 1.0f;
                break;
            case 2:
                x1 = 0.0f;
                y1 = 0.0f;
                x2 = 0.2f;
                y2 = 1.0f;
                break;
            case 3:
                x1 = 0.4f;
                y1 = 0.0f;
                x2 = 0.6f;
                y2 = 1.0f;
                break;
            case 10:
                x1 = 0.33f;
                y1 = 0.0f;
                x2 = 0.83f;
                y2 = 0.83f;
                break;
            case 11:
                x1 = 0.5f;
                y1 = 0.0f;
                x2 = 0.83f;
                y2 = 0.83f;
                break;
            case 12:
                x1 = 0.6f;
                y1 = 0.0f;
                x2 = 0.83f;
                y2 = 0.83f;
                break;
            case 13:
                x1 = 0.7f;
                y1 = 0.0f;
                x2 = 0.83f;
                y2 = 0.83f;
                break;
            case 14:
                x1 = 0.8f;
                y1 = 0.0f;
                x2 = 0.83f;
                y2 = 0.83f;
                break;
            case 15:
                x1 = 0.9f;
                y1 = 0.0f;
                x2 = 0.83f;
                y2 = 0.83f;
                break;
            case 20:
                x1 = 0.17f;
                y1 = 0.17f;
                x2 = 0.67f;
                y2 = 1.0f;
                break;
            case 21:
                x1 = 0.17f;
                y1 = 0.17f;
                x2 = 0.5f;
                y2 = 1.0f;
                break;
            case 22:
                x1 = 0.17f;
                y1 = 0.17f;
                x2 = 0.4f;
                y2 = 1.0f;
                break;
            case 23:
                x1 = 0.17f;
                y1 = 0.17f;
                x2 = 0.3f;
                y2 = 1.0f;
                break;
            case 24:
                x1 = 0.17f;
                y1 = 0.17f;
                x2 = 0.2f;
                y2 = 1.0f;
                break;
            case 25:
                x1 = 0.17f;
                y1 = 0.17f;
                x2 = 0.1f;
                y2 = 1.0f;
                break;
            case SINE_IN_OUT_33 /*30*/:
                x1 = 0.33f;
                y1 = 0.0f;
                x2 = 0.67f;
                y2 = 1.0f;
                break;
            case SINE_IN_OUT_50 /*31*/:
                x1 = 0.33f;
                y1 = 0.0f;
                x2 = 0.5f;
                y2 = 1.0f;
                break;
            case 32:
                x1 = 0.33f;
                y1 = 0.0f;
                x2 = 0.4f;
                y2 = 1.0f;
                break;
            case 33:
                x1 = 0.33f;
                y1 = 0.0f;
                x2 = 0.3f;
                y2 = 1.0f;
                break;
            case 34:
                x1 = 0.33f;
                y1 = 0.0f;
                x2 = 0.2f;
                y2 = 1.0f;
                break;
            case 35:
                x1 = 0.33f;
                y1 = 0.0f;
                x2 = 0.1f;
                y2 = 1.0f;
                break;
            default:
                y2 = 0.0f;
                x2 = 0.0f;
                y1 = 0.0f;
                x1 = 0.0f;
                break;
        }
        return new PathInterpolator(x1, y1, x2, y2);
    }
}

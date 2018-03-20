package com.android.launcher3.features;

import com.samsung.android.feature.SemFloatingFeature;

public class FloatingFeature {
    public static String getString(String tag, String defaultValue) {
        SemFloatingFeature instance = SemFloatingFeature.getInstance();
        return instance != null ? instance.getString(tag, defaultValue) : defaultValue;
    }

    public static boolean getBoolean(String tag) {
        SemFloatingFeature instance = SemFloatingFeature.getInstance();
        return instance != null && instance.getBoolean(tag);
    }

    public static boolean getBoolean(String tag, boolean defaultValue) {
        SemFloatingFeature instance = SemFloatingFeature.getInstance();
        return instance != null ? instance.getBoolean(tag, defaultValue) : defaultValue;
    }

    public static String getString(String tag) {
        SemFloatingFeature instance = SemFloatingFeature.getInstance();
        return instance != null ? instance.getString(tag) : null;
    }

    public static int getInt(String tag) {
        SemFloatingFeature instace = SemFloatingFeature.getInstance();
        return instace != null ? instace.getInt(tag) : 0;
    }

    public static int getInt(String tag, int defaultValue) {
        SemFloatingFeature instance = SemFloatingFeature.getInstance();
        return instance != null ? instance.getInt(tag, defaultValue) : defaultValue;
    }
}

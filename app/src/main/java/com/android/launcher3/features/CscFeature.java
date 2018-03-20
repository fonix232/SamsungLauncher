package com.android.launcher3.features;

//import com.samsung.android.feature.SemCscFeature;

public class CscFeature {
    public static boolean getBoolean(String tag) {
        SemCscFeature instance = SemCscFeature.getInstance();
        return instance != null && instance.getBoolean(tag);
    }

    public static boolean getBoolean(String tag, boolean defaultValue) {
        SemCscFeature instance = SemCscFeature.getInstance();
        return instance != null ? instance.getBoolean(tag, defaultValue) : defaultValue;
    }

    public static String getString(String tag) {
        SemCscFeature instance = SemCscFeature.getInstance();
        return instance != null ? instance.getString(tag) : null;
    }

    public static String getString(String tag, String defaultValue) {
        SemCscFeature instance = SemCscFeature.getInstance();
        return instance != null ? instance.getString(tag, defaultValue) : defaultValue;
    }

    public static int getInt(String tag) {
        SemCscFeature instance = SemCscFeature.getInstance();
        return instance != null ? instance.getInt(tag) : 0;
    }

    public static int getInt(String tag, int defaultValue) {
        SemCscFeature instance = SemCscFeature.getInstance();
        return instance != null ? instance.getInt(tag, defaultValue) : defaultValue;
    }
}

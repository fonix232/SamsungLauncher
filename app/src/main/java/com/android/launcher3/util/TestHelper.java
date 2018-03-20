package com.android.launcher3.util;

import android.os.Build;
import android.util.Log;

public final class TestHelper {
    private static final String TAG = "TestHelper";

    public static boolean isRoboUnitTest() {
        return "robolectric".equals(Build.FINGERPRINT);
    }

    public static void enableAccessField(String className, String fieldName) {
        try {
            Class.forName(className).getField(fieldName).setAccessible(true);
        } catch (Exception exception) {
            Log.e(TAG, "MalformedURLException : " + exception.toString());
        }
    }
}

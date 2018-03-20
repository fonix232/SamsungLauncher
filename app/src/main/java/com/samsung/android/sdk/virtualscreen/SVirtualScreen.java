package com.samsung.android.sdk.virtualscreen;

import android.app.ActivityThread;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import com.samsung.android.sdk.SsdkInterface;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.SsdkVendorCheck;

public final class SVirtualScreen implements SsdkInterface {
    private static final String TAG = SVirtualScreen.class.getSimpleName();
    public static final int VIRTUALSCREEN_BASIC_FEATURE = 1;
    static final int VIRTUALSCREEN_SDK_VERSION_CODE = 2;
    static final String VIRTUALSCREEN_SDK_VERSION_NAME = "1.7.1";
    private static boolean enableQueried = false;
    private static boolean isVirtualScreenEnabled = false;
    private SVirtualScreenReflector mVirtualScreenReflector = new SVirtualScreenReflector();

    public SVirtualScreen() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            SVirtualScreenReflector sVirtualScreenReflector = this.mVirtualScreenReflector;
            SVirtualScreenReflector.putMethod(activityThreadClass, "getApplication", (Class[]) null);
            sVirtualScreenReflector = this.mVirtualScreenReflector;
            SVirtualScreenReflector.putMethod(activityThreadClass, "getSystemContext", (Class[]) null);
        } catch (Exception e) {
        }
        initVirtualScreenFeature();
    }

    public void initialize(Context arg0) throws SsdkUnsupportedException {
        if (!SsdkVendorCheck.isSamsungDevice()) {
            throw new SsdkUnsupportedException(Build.BRAND + " is not supported.", 0);
        } else if (!isVirtualScreenEnabled) {
            throw new SsdkUnsupportedException("The device is not supported.", 1);
        }
    }

    public boolean isFeatureEnabled(int feature) {
        switch (feature) {
            case 1:
                return isVirtualScreenEnabled;
            default:
                return false;
        }
    }

    public int getVersionCode() {
        return 2;
    }

    public String getVersionName() {
        return VIRTUALSCREEN_SDK_VERSION_NAME;
    }

    private void initVirtualScreenFeature() {
        Context context = null;
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            ActivityThread activityThreadObj = ActivityThread.currentActivityThread();
            SVirtualScreenReflector sVirtualScreenReflector = this.mVirtualScreenReflector;
            if (SVirtualScreenReflector.checkMethod(activityThreadClass, "getApplication()")) {
                sVirtualScreenReflector = this.mVirtualScreenReflector;
                context = (Context) SVirtualScreenReflector.invoke(activityThreadClass, "getApplication()", activityThreadObj, (Object[]) null);
            }
            if (context == null) {
                sVirtualScreenReflector = this.mVirtualScreenReflector;
                if (SVirtualScreenReflector.checkMethod(activityThreadClass, "getSystemContext()")) {
                    sVirtualScreenReflector = this.mVirtualScreenReflector;
                    context = (Context) SVirtualScreenReflector.invoke(activityThreadClass, "getSystemContext()", activityThreadObj, (Object[]) null);
                }
            }
            if (context != null) {
                PackageManager pm = context.getPackageManager();
                if (pm != null) {
                    isVirtualScreenEnabled = pm.hasSystemFeature("com.samsung.feature.virtualscreen");
                }
            }
        } catch (Exception e) {
        }
    }
}

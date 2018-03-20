package com.android.launcher3.util;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DvfsUtil {
    private static final int DEFAULT_ONE_FRAME = 20;
    private static final int DEFAULT_TIME_OUT = 300;
    public static final int DEFINE_MODEL_BASE = 0;
    static final String TAG = DvfsUtil.class.getSimpleName();
    private static DvfsHelper sDvfsHelper = null;

    public DvfsUtil(Context context) {
        if (sDvfsHelper == null || sDvfsHelper.mContext != context || sDvfsHelper.mAppLauncherBooster == null) {
            sDvfsHelper = new DvfsHelper(context);
        }
    }

    public boolean acquireAppLaunch(Intent intent) {
        if (sDvfsHelper.mAppLauncherBooster == null) {
            return false;
        }
        try {
            sDvfsHelper.mAppLauncherBooster.acquire(intent.getComponent().getPackageName());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "appLauncherBooster is failed");
            return false;
        }
    }

    private void boostUp(int timeOut) {
        sDvfsHelper.boostUp(timeOut, true, true);
    }

    public void boostCpuForSupportedModel(int timeOut) {
        sDvfsHelper.boostUpForSupportedModel(timeOut, false, true);
    }

    public void boostUpForSupportedModel() {
        sDvfsHelper.boostUpForSupportedModel(300, true, true);
    }

    public void boostOneFrame() {
        if (System.currentTimeMillis() - sDvfsHelper.mBoostStart.longValue() > 20) {
            boostUp(20);
        }
    }
}

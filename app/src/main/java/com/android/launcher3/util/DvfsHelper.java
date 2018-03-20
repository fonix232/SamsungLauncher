package com.android.launcher3.util;

import android.content.Context;
import android.os.SemSystemProperties;
import android.util.Log;
import com.android.launcher3.LauncherFeature;
import com.samsung.android.os.SemDvfsManager;

/* compiled from: DvfsUtil */
class DvfsHelper {
    private static final boolean FEATURE_USE_LOW_BOOST_CLOCK = LauncherFeature.isStarProject();
    private static final String LAUNCHER_PACKAGE = "com.sec.android.app.launcher";
    SemDvfsManager mAppLauncherBooster;
    Long mBoostStart;
    Context mContext;
    private MinLimit mCpuMin;
    private MinLimit mGpuMin;
    private final String mProdName = SemSystemProperties.get("ro.product.name");

    DvfsHelper(Context context) {
        this.mContext = context;
        this.mAppLauncherBooster = SemDvfsManager.createInstance(context.getApplicationContext(), "APP_LAUNCH", 21);
        createMinLimit();
        this.mBoostStart = Long.valueOf(System.currentTimeMillis());
    }

    private boolean createMinLimit() {
        int i = 4;
        Log.i(DvfsUtil.TAG, "Support : " + this.mProdName);
        this.mCpuMin = new MinLimit(this.mContext, "com.sec.android.app.launcher", 12, FEATURE_USE_LOW_BOOST_CLOCK ? 4 : 0);
        Context context = this.mContext;
        String str = "com.sec.android.app.launcher";
        if (!FEATURE_USE_LOW_BOOST_CLOCK) {
            i = 2;
        }
        this.mGpuMin = new MinLimit(context, str, 16, i);
        return true;
    }

    void boostUpForSupportedModel(int timeOut, boolean boostGpu, boolean boostCpu) {
        if (supportedDevices(this.mProdName)) {
            boostUp(timeOut, boostGpu, boostCpu);
        }
    }

    void boostUp(int timeOut, boolean boostGpu, boolean boostCpu) {
        if (timeOut == 0) {
            timeOut = durationDevices(this.mProdName);
        }
        if (this.mCpuMin != null && boostCpu) {
            this.mCpuMin.boostUp(timeOut);
        }
        if (this.mGpuMin != null && boostGpu) {
            this.mGpuMin.boostUp(timeOut);
        }
    }

    private boolean supportedDevices(String prodName) {
        if (prodName != null && prodName.contains("j7max")) {
            return true;
        }
        return false;
    }

    private int durationDevices(String prodName) {
        if (prodName != null && prodName.contains("j7max")) {
            return 1000;
        }
        return 0;
    }
}

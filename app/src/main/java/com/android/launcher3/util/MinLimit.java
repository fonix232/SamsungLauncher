package com.android.launcher3.util;

import android.content.Context;
import com.samsung.android.os.SemDvfsManager;

/* compiled from: DvfsUtil */
class MinLimit {
    private SemDvfsManager mDvfsMin;

    MinLimit(Context context, String pkgName, int type, int index) {
        this.mDvfsMin = SemDvfsManager.createInstance(context, pkgName, type);
        if (this.mDvfsMin != null) {
            int[] supportedTable = this.mDvfsMin.getSupportedFrequency();
            if (supportedTable != null && supportedTable.length > index) {
                this.mDvfsMin.setDvfsValue(supportedTable[index]);
            }
        }
    }

    boolean boostUp(int timeOut) {
        if (this.mDvfsMin == null) {
            return false;
        }
        this.mDvfsMin.acquire(timeOut);
        return true;
    }
}

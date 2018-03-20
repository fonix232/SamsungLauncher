package com.android.launcher3.util;

import android.content.Context;
import android.view.Window;
import com.android.launcher3.LauncherFeature;

public class BlurRunnable implements Runnable {
    private final float mAmount;
    private final Context mContext;
    private final Window mDest;
    private final long mDuration;
    private final boolean mShow;

    public BlurRunnable(boolean show, Window dest, float amount, long duration, Context context) {
        this.mShow = show;
        this.mDest = dest;
        this.mAmount = amount;
        this.mDuration = duration;
        this.mContext = context;
    }

    public void run() {
        if (LauncherFeature.supportBackgroundBlurByWindow() && this.mContext != null) {
            new DvfsUtil(this.mContext).boostOneFrame();
            BlurUtils.blurByWindowManager(this.mShow, this.mDest, this.mAmount, this.mDuration);
        }
    }
}

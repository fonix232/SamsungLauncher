package com.android.launcher3.home;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.google.android.hotword.client.HotwordServiceClient;

public class HotWord {
    public static String TAG = "Home.HotWord";
    private HotwordServiceClient mHotwordServiceClient = null;
    private Launcher mLauncher;

    public HotWord(Context context) {
        this.mLauncher = (Launcher) context;
        this.mHotwordServiceClient = new HotwordServiceClient(this.mLauncher);
    }

    public void onAttachedToWindow() {
        this.mHotwordServiceClient.onAttachedToWindow();
    }

    public void onDetachedFromWindow() {
        this.mHotwordServiceClient.onDetachedFromWindow();
    }

    public void setEnableHotWord(boolean enabled) {
        if (this.mHotwordServiceClient == null) {
            return;
        }
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            Log.d(TAG, "setEnableHotWord is called by other thread");
        } else if (Utilities.isTalkBackEnabled(this.mLauncher)) {
            this.mHotwordServiceClient.requestHotwordDetection(false);
        } else {
            Log.i(TAG, "setEnableHotWord enabled=" + enabled);
            this.mHotwordServiceClient.requestHotwordDetection(enabled);
        }
    }
}

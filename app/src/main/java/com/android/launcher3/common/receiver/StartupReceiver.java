package com.android.launcher3.common.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {
    public static final String SYSTEM_READY = "com.android.launcher3.SYSTEM_READY";
    private static final String TAG = "StartupReceiver";

    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Log.i(TAG, "onReceive intent=" + intent);
            context.sendStickyBroadcast(new Intent(SYSTEM_READY));
        }
    }
}

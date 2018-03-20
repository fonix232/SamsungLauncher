package com.android.launcher3.common.bnr.scloud;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.launcher3.LauncherAppState;

public class SCloudRestoreCompleteReceiver extends BroadcastReceiver {
    private static final String ACTION_RESTORE_COMPLETE = "com.sec.android.intent.action.HOME_RESTORE_COMPLETE";
    private static final String TAG = "SCRCR";

    public void onReceive(Context context, Intent intent) {
        if (ACTION_RESTORE_COMPLETE.equals(intent.getAction())) {
            LauncherAppState.setApplicationContext(context.getApplicationContext());
            LauncherAppState appState = LauncherAppState.getInstance();
            if (appState == null || appState.getModel() == null) {
                Log.d(TAG, "appState or launcher model is null!!");
            } else {
                appState.getModel().handleSCloudRestoreComplete(context);
            }
        }
    }
}

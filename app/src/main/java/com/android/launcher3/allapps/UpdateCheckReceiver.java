package com.android.launcher3.allapps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

class UpdateCheckReceiver extends BroadcastReceiver {
    UpdateCheckReceiver() {
    }

    public void onReceive(Context context, Intent intent) {
        Log.e("Receiver", "update check receiver start : " + intent.getAction());
        new UpdateCheckThread(context, true, null, null).start();
    }
}

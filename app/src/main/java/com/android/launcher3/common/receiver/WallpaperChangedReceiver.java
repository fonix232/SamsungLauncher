package com.android.launcher3.common.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.launcher3.LauncherAppState;

public class WallpaperChangedReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent data) {
        LauncherAppState.setApplicationContext(context.getApplicationContext());
        LauncherAppState.getInstance().onWallpaperChanged();
    }
}

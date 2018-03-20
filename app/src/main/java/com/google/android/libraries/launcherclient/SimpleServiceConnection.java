package com.google.android.libraries.launcherclient;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

class SimpleServiceConnection implements ServiceConnection {
    private boolean mBoundSuccessfully;
    private final Context mContext;
    private final int mFlags;

    SimpleServiceConnection(Context context, int flags) {
        this.mContext = context;
        this.mFlags = flags;
    }

    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
    }

    public void onServiceDisconnected(ComponentName componentName) {
    }

    public void unbindSelf() {
        if (this.mBoundSuccessfully) {
            this.mContext.unbindService(this);
            this.mBoundSuccessfully = false;
        }
    }

    public final boolean connectSafely() {
        if (!this.mBoundSuccessfully) {
            try {
                this.mBoundSuccessfully = this.mContext.bindService(LauncherClient.getServiceIntent(this.mContext), this, this.mFlags);
            } catch (SecurityException e) {
                Log.e("LauncherClient", "Unable to connect to overlay service", e);
            }
        }
        return this.mBoundSuccessfully;
    }
}

package com.google.android.libraries.launcherclient;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import com.google.android.libraries.launcherclient.ILauncherOverlay.Stub;
import java.lang.ref.WeakReference;

class AppServiceConnection extends SimpleServiceConnection {
    private static AppServiceConnection sInstance;
    private WeakReference<LauncherClient> mActiveClient;
    private boolean mAutoUnbind;
    private ILauncherOverlay mOverlay;

    static AppServiceConnection get(Context context) {
        if (sInstance == null) {
            sInstance = new AppServiceConnection(context.getApplicationContext());
        }
        return sInstance;
    }

    private AppServiceConnection(Context context) {
        super(context, 33);
    }

    public ILauncherOverlay setClient(LauncherClient client) {
        this.mActiveClient = new WeakReference(client);
        return this.mOverlay;
    }

    public void setAutoUnbind(boolean canAutoUnbind) {
        this.mAutoUnbind = canAutoUnbind;
    }

    public void clearClientIfSame(LauncherClient client, boolean unbind) {
        LauncherClient boundClient = getBoundClient();
        if (boundClient != null && boundClient.equals(client)) {
            this.mActiveClient = null;
            if (unbind) {
                unbindSelf();
                if (sInstance == this) {
                    sInstance = null;
                }
            }
        }
    }

    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        setOverlay(Stub.asInterface(iBinder));
    }

    public void onServiceDisconnected(ComponentName componentName) {
        if (this.mAutoUnbind) {
            unbindSelf();
        }
        setOverlay(null);
    }

    private void setOverlay(ILauncherOverlay overlay) {
        this.mOverlay = overlay;
        LauncherClient client = getBoundClient();
        if (client != null) {
            client.overlayAvailabilityChanged(this.mOverlay);
        }
    }

    private LauncherClient getBoundClient() {
        return this.mActiveClient != null ? (LauncherClient) this.mActiveClient.get() : null;
    }
}

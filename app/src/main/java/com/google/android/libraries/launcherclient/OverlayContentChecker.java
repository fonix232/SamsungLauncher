package com.google.android.libraries.launcherclient;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import com.google.android.libraries.launcherclient.AbsServiceStatusChecker.StatusCallback;
import com.google.android.libraries.launcherclient.ILauncherOverlay.Stub;

public class OverlayContentChecker extends AbsServiceStatusChecker {
    public OverlayContentChecker(Context context) {
        super(context);
    }

    public void checkOverlayContent(StatusCallback statusCallback) {
        checkServiceStatus(statusCallback, LauncherClient.getServiceIntent(this.mContext));
    }

    protected boolean getStatus(IBinder service) throws RemoteException {
        return Stub.asInterface(service).hasOverlayContent();
    }
}

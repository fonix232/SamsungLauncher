package com.google.android.libraries.launcherclient;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import com.google.android.libraries.launcherclient.AbsServiceStatusChecker.StatusCallback;
import com.google.android.libraries.launcherclient.ILauncherOverlay.Stub;

public class HotwordServiceChecker extends AbsServiceStatusChecker {
    public HotwordServiceChecker(Context context) {
        super(context);
    }

    public void checkHotwordService(StatusCallback statusCallback) {
        checkServiceStatus(statusCallback, LauncherClient.getServiceIntent(this.mContext));
    }

    protected boolean getStatus(IBinder service) throws RemoteException {
        return Stub.asInterface(service).isVoiceDetectionRunning();
    }
}

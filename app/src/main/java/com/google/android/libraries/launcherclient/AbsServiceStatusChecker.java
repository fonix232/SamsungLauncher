package com.google.android.libraries.launcherclient;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import com.android.launcher3.home.LauncherAppWidgetHostView;

@TargetApi(19)
public abstract class AbsServiceStatusChecker {
    private static final boolean DBG = false;
    private static final String TAG = "AbsServiceStatusChecker";
    final Context mContext;

    private class ServiceStatusConnection implements ServiceConnection {
        private StatusCallback mStatusCallback;

        public ServiceStatusConnection(StatusCallback statusCallback) {
            this.mStatusCallback = statusCallback;
        }

        public void onServiceDisconnected(ComponentName cn) {
        }

        public void onServiceConnected(ComponentName cn, IBinder service) {
            try {
                this.mStatusCallback.isRunning(AbsServiceStatusChecker.this.getStatus(service));
            } catch (RemoteException e) {
                Log.w(AbsServiceStatusChecker.TAG, "isServiceRunning - remote call failed", e);
                this.mStatusCallback.isRunning(false);
            } finally {
                AbsServiceStatusChecker.this.mContext.unbindService(this);
            }
        }
    }

    public interface StatusCallback {
        void isRunning(boolean z);
    }

    protected abstract boolean getStatus(IBinder iBinder) throws RemoteException;

    protected AbsServiceStatusChecker(Context context) {
        this.mContext = context;
    }

    protected void checkServiceStatus(final StatusCallback statusCallback, Intent intent) {
        intent.setPackage(LauncherAppWidgetHostView.GOOGLE_SEARCH_APP_PACKAGE_NAME);
        if (!this.mContext.bindService(intent, new ServiceStatusConnection(statusCallback), 1)) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                public void run() {
                    AbsServiceStatusChecker.this.assertMainThread();
                    statusCallback.isRunning(false);
                }
            });
        }
    }

    private void assertMainThread() {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Must be called on the main thread.");
        }
    }
}

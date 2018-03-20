package com.google.android.libraries.launcherclient;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import com.android.launcher3.home.LauncherAppWidgetHostView;
import com.google.android.libraries.launcherclient.ILauncherOverlayCallback.Stub;

public class LauncherClient {
    private static final boolean DEBUG = false;
    private static final boolean HIDE_WINDOW_WHEN_OVERLAY_OPEN = false;
    private static final int OPTIONS_FLAG_HOTWORD = 2;
    private static final int OPTIONS_FLAG_OVERLAY = 1;
    private static final int OPTIONS_FLAG_PREWARMING = 4;
    private static final int OVERLAY_OPTION_FLAG_ANIMATE = 1;
    private static final int OVERLAY_OPTION_FLAG_IMMEDIATE = 0;
    private static final int SERVICE_STATUS_ALL_FEATURES_OFF = 0;
    private static final int SERVICE_STATUS_HOTWORD_ACTIVE = 2;
    private static final int SERVICE_STATUS_OVERLAY_ATTACHED = 1;
    private static final String TAG = "DrawerOverlayClient";
    private final Activity mActivity;
    private final SimpleServiceConnection mActivityConnection;
    private final AppServiceConnection mAppConnection;
    private int mCurrentServiceConnectionOptions;
    private boolean mDestroyed;
    private boolean mIsResumed;
    private final LauncherClientCallbacks mLauncherClientCallbacks;
    protected ILauncherOverlay mOverlay;
    private OverlayCallbacks mOverlayCallbacks;
    private int mServiceStatus;
    private final BroadcastReceiver mUpdateReceiver;
    private LayoutParams mWindowAttrs;

    public static class ClientOptions {
        private boolean mEnableHotword;
        private boolean mEnableOverlay;
        private boolean mEnablePrewarming;

        public ClientOptions(boolean enableOverlay, boolean enableHotword, boolean enablePrewarming) {
            this.mEnableOverlay = enableOverlay;
            this.mEnableHotword = enableHotword;
            this.mEnablePrewarming = enablePrewarming;
        }
    }

    private static class OverlayCallbacks extends Stub implements Callback {
        private static final int MSG_UPDATE_SCROLL = 2;
        private static final int MSG_UPDATE_SHIFT = 3;
        private static final int MSG_UPDATE_STATUS = 4;
        private LauncherClient mClient;
        private final Handler mUIHandler = new Handler(Looper.getMainLooper(), this);
        private Window mWindow;
        private boolean mWindowHidden = false;
        private WindowManager mWindowManager;
        private int mWindowShift;

        OverlayCallbacks() {
        }

        @TargetApi(17)
        public void setClient(LauncherClient client) {
            this.mClient = client;
            this.mWindowManager = client.mActivity.getWindowManager();
            Point p = new Point();
            this.mWindowManager.getDefaultDisplay().getRealSize(p);
            this.mWindowShift = -Math.max(p.x, p.y);
            this.mWindow = client.mActivity.getWindow();
        }

        public void clear() {
            this.mClient = null;
            this.mWindowManager = null;
            this.mWindow = null;
        }

        public void overlayScrollChanged(float progress) throws RemoteException {
            this.mUIHandler.removeMessages(2);
            Message.obtain(this.mUIHandler, 2, Float.valueOf(progress)).sendToTarget();
            if (progress > 0.0f) {
                hideActivityNonUI(false);
            }
        }

        public void overlayStatusChanged(int status) {
            Message.obtain(this.mUIHandler, 4, status, 0).sendToTarget();
        }

        public boolean handleMessage(Message msg) {
            if (this.mClient == null) {
                return true;
            }
            switch (msg.what) {
                case 2:
                    if ((this.mClient.mServiceStatus & 1) != 0) {
                        this.mClient.mLauncherClientCallbacks.onOverlayScrollChanged(((Float) msg.obj).floatValue());
                    }
                    return true;
                case 3:
                    LayoutParams attrs = this.mWindow.getAttributes();
                    if (((Boolean) msg.obj).booleanValue()) {
                        attrs.x = this.mWindowShift;
                        attrs.flags |= 512;
                    } else {
                        attrs.x = 0;
                        attrs.flags &= -513;
                    }
                    this.mWindowManager.updateViewLayout(this.mWindow.getDecorView(), attrs);
                    return true;
                case 4:
                    this.mClient.notifyStatusChanged(msg.arg1);
                    return true;
                default:
                    return false;
            }
        }

        private void hideActivityNonUI(boolean isHidden) {
            if (this.mWindowHidden != isHidden) {
                this.mWindowHidden = isHidden;
            }
        }
    }

    public LauncherClient(Activity activity) {
        this(activity, new LauncherClientCallbacksAdapter());
    }

    public LauncherClient(Activity activity, LauncherClientCallbacks callbacks) {
        this(activity, callbacks, new ClientOptions(true, true, true));
    }

    public LauncherClient(Activity activity, LauncherClientCallbacks callbacks, ClientOptions clientOptions) {
        int i;
        this.mUpdateReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Uri data = intent.getData();
                if (VERSION.SDK_INT >= 19 || (data != null && LauncherAppWidgetHostView.GOOGLE_SEARCH_APP_PACKAGE_NAME.equals(data.getSchemeSpecificPart()))) {
                    LauncherClient.this.mActivityConnection.unbindSelf();
                    LauncherClient.this.mAppConnection.unbindSelf();
                    if (LauncherClient.this.mIsResumed) {
                        LauncherClient.this.reconnect();
                    }
                }
            }
        };
        this.mIsResumed = false;
        this.mDestroyed = false;
        this.mServiceStatus = -1;
        this.mActivity = activity;
        this.mLauncherClientCallbacks = callbacks;
        this.mActivityConnection = new SimpleServiceConnection(activity, 65);
        this.mCurrentServiceConnectionOptions = 0;
        int i2 = this.mCurrentServiceConnectionOptions;
        if (clientOptions.mEnableOverlay) {
            i = 1;
        } else {
            i = 0;
        }
        this.mCurrentServiceConnectionOptions = i | i2;
        i2 = this.mCurrentServiceConnectionOptions;
        if (clientOptions.mEnableHotword) {
            i = 2;
        } else {
            i = 0;
        }
        this.mCurrentServiceConnectionOptions = i | i2;
        i2 = this.mCurrentServiceConnectionOptions;
        if (clientOptions.mEnablePrewarming) {
            i = 4;
        } else {
            i = 0;
        }
        this.mCurrentServiceConnectionOptions = i | i2;
        this.mAppConnection = AppServiceConnection.get(activity);
        this.mOverlay = this.mAppConnection.setClient(this);
        IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        filter.addDataScheme("package");
        if (VERSION.SDK_INT >= 19) {
            filter.addDataSchemeSpecificPart(LauncherAppWidgetHostView.GOOGLE_SEARCH_APP_PACKAGE_NAME, 0);
        }
        this.mActivity.registerReceiver(this.mUpdateReceiver, filter);
        reconnect();
    }

    public final void onAttachedToWindow() {
        if (!this.mDestroyed) {
            setWindowAttrs(this.mActivity.getWindow().getAttributes());
        }
    }

    public final void onDetachedFromWindow() {
        if (!this.mDestroyed) {
            setWindowAttrs(null);
        }
    }

    public void onResume() {
        if (!this.mDestroyed) {
            this.mAppConnection.setAutoUnbind(false);
            reconnect();
            this.mIsResumed = true;
            if (this.mOverlay != null && this.mWindowAttrs != null) {
                try {
                    this.mOverlay.onResume();
                } catch (RemoteException e) {
                }
            }
        }
    }

    public void onPause() {
        if (!this.mDestroyed) {
            this.mAppConnection.setAutoUnbind(true);
            this.mActivityConnection.unbindSelf();
            this.mIsResumed = false;
            if (this.mOverlay != null && this.mWindowAttrs != null) {
                try {
                    this.mOverlay.onPause();
                } catch (RemoteException e) {
                }
            }
        }
    }

    public void onDestroy() {
        removeClient(!this.mActivity.isChangingConfigurations());
    }

    public void disconnect() {
        removeClient(true);
    }

    public void setOverlayEnabled(boolean isEnabled) {
        int newOptions;
        if (isEnabled) {
            newOptions = this.mCurrentServiceConnectionOptions | 1;
        } else {
            newOptions = this.mCurrentServiceConnectionOptions & -2;
        }
        if (newOptions != this.mCurrentServiceConnectionOptions) {
            this.mCurrentServiceConnectionOptions = newOptions;
            if (this.mWindowAttrs != null) {
                applyWindowToken();
            }
        }
    }

    private void removeClient(boolean unbindApp) {
        if (!this.mDestroyed) {
            this.mActivity.unregisterReceiver(this.mUpdateReceiver);
        }
        this.mDestroyed = true;
        this.mActivityConnection.unbindSelf();
        if (this.mOverlayCallbacks != null) {
            this.mOverlayCallbacks.clear();
            this.mOverlayCallbacks = null;
        }
        this.mAppConnection.clearClientIfSame(this, unbindApp);
    }

    public void reconnect() {
        if (!this.mDestroyed) {
            if (!this.mAppConnection.connectSafely() || !this.mActivityConnection.connectSafely()) {
                this.mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        LauncherClient.this.notifyStatusChanged(0);
                    }
                });
            }
        }
    }

    private void setWindowAttrs(LayoutParams windowAttrs) {
        this.mWindowAttrs = windowAttrs;
        if (this.mWindowAttrs != null) {
            applyWindowToken();
        } else if (this.mOverlay != null) {
            try {
                this.mOverlay.windowDetached(this.mActivity.isChangingConfigurations());
            } catch (RemoteException e) {
            }
            this.mOverlay = null;
        }
    }

    private void applyWindowToken() {
        if (this.mOverlay != null) {
            try {
                if (this.mOverlayCallbacks == null) {
                    this.mOverlayCallbacks = new OverlayCallbacks();
                }
                this.mOverlayCallbacks.setClient(this);
                this.mOverlay.windowAttached(this.mWindowAttrs, this.mOverlayCallbacks, this.mCurrentServiceConnectionOptions);
                if (this.mIsResumed) {
                    this.mOverlay.onResume();
                } else {
                    this.mOverlay.onPause();
                }
            } catch (RemoteException e) {
            }
        }
    }

    private boolean isConnected() {
        return this.mOverlay != null;
    }

    public void startMove() {
        if (isConnected()) {
            try {
                this.mOverlay.startScroll();
            } catch (RemoteException e) {
            }
        }
    }

    public void endMove() {
        if (isConnected()) {
            try {
                this.mOverlay.endScroll();
            } catch (RemoteException e) {
            }
        }
    }

    public void updateMove(float progressX) {
        if (isConnected()) {
            try {
                this.mOverlay.onScroll(progressX);
            } catch (RemoteException e) {
            }
        }
    }

    public void hideOverlay(boolean animate) {
        if (this.mOverlay != null) {
            try {
                this.mOverlay.closeOverlay(animate ? 1 : 0);
            } catch (RemoteException e) {
            }
        }
    }

    public void showOverlay(boolean animate) {
        if (this.mOverlay != null) {
            try {
                this.mOverlay.openOverlay(animate ? 1 : 0);
            } catch (RemoteException e) {
            }
        }
    }

    public void requestHotwordDetection(boolean start) {
        if (this.mOverlay != null) {
            try {
                this.mOverlay.requestVoiceDetection(start);
            } catch (RemoteException e) {
            }
        }
    }

    void overlayAvailabilityChanged(ILauncherOverlay overlay) {
        this.mOverlay = overlay;
        if (this.mOverlay == null) {
            notifyStatusChanged(0);
        } else if (this.mWindowAttrs != null) {
            applyWindowToken();
        }
    }

    private void notifyStatusChanged(int status) {
        boolean z = true;
        if (this.mServiceStatus != status) {
            boolean z2;
            this.mServiceStatus = status;
            LauncherClientCallbacks launcherClientCallbacks = this.mLauncherClientCallbacks;
            if ((status & 1) != 0) {
                z2 = true;
            } else {
                z2 = false;
            }
            if ((status & 2) == 0) {
                z = false;
            }
            launcherClientCallbacks.onServiceStateChanged(z2, z);
        }
    }

    static Intent getServiceIntent(Context context) {
        String valueOf = String.valueOf(context.getPackageName());
        return new Intent("com.android.launcher3.WINDOW_OVERLAY").setPackage(LauncherAppWidgetHostView.GOOGLE_SEARCH_APP_PACKAGE_NAME).setData(Uri.parse(new StringBuilder(String.valueOf(valueOf).length() + 18).append("app://").append(valueOf).append(":").append(Process.myUid()).toString()).buildUpon().appendQueryParameter("v", Integer.toString(0)).build());
    }
}

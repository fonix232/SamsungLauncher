package com.google.android.hotword.client;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build.VERSION;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.WindowId;
import android.view.WindowId.FocusObserver;
import com.google.android.hotword.service.IHotwordService;
import com.google.android.hotword.service.IHotwordService.Stub;

@TargetApi(19)
public class HotwordServiceClient {
    private static final boolean DBG = false;
    private static final String HOTWORD_SERVICE = "com.google.android.googlequicksearchbox.HOTWORD_SERVICE";
    private static final String TAG = "HotwordServiceClient";
    private static final String VEL_PACKAGE = "com.google.android.googlequicksearchbox";
    private final Activity mActivity;
    private final ServiceConnection mConnection;
    private final FocusObserver mFocusObserver;
    private IHotwordService mHotwordService;
    private boolean mHotwordStart;
    private boolean mIsAvailable = true;
    private boolean mIsBound;
    private boolean mIsFocused = false;
    private boolean mIsRequested = true;

    private class HotwordServiceConnection implements ServiceConnection {
        private HotwordServiceConnection() {
        }

        public void onServiceDisconnected(ComponentName cn) {
            HotwordServiceClient.this.mIsBound = false;
            HotwordServiceClient.this.mHotwordService = null;
        }

        public void onServiceConnected(ComponentName cn, IBinder service) {
            HotwordServiceClient.this.mHotwordService = Stub.asInterface(service);
            HotwordServiceClient.this.internalRequestHotword();
        }
    }

    private class WindowFocusObserver extends FocusObserver {
        private WindowFocusObserver() {
        }

        public void onFocusGained(WindowId windowId) {
            HotwordServiceClient.this.mIsFocused = true;
            HotwordServiceClient.this.internalRequestHotword();
        }

        public void onFocusLost(WindowId windowId) {
            HotwordServiceClient.this.mIsFocused = false;
            HotwordServiceClient.this.internalRequestHotword();
        }
    }

    public HotwordServiceClient(Activity activity) {
        this.mActivity = activity;
        this.mConnection = new HotwordServiceConnection();
        this.mFocusObserver = new WindowFocusObserver();
    }

    public final void onAttachedToWindow() {
        if (!isPreKitKatDevice()) {
            assertMainThread();
            this.mActivity.getWindow().getDecorView().getWindowId().registerFocusObserver(this.mFocusObserver);
            internalBind();
        }
    }

    public final void onDetachedFromWindow() {
        if (!isPreKitKatDevice()) {
            assertMainThread();
            this.mActivity.getWindow().getDecorView().getWindowId().unregisterFocusObserver(this.mFocusObserver);
            if (this.mIsBound) {
                this.mActivity.unbindService(this.mConnection);
                this.mIsBound = false;
            }
        }
    }

    public final void requestHotwordDetection(boolean start) {
        if (!isPreKitKatDevice()) {
            assertMainThread();
            this.mIsRequested = start;
            internalRequestHotword();
        }
    }

    private void assertMainThread() {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Must be called on the main thread.");
        }
    }

    private boolean isPreKitKatDevice() {
        if (VERSION.SDK_INT >= 19) {
            return false;
        }
        Log.w(TAG, "Hotword service isn't usable on pre-Kitkat devices");
        return true;
    }

    private void internalBind() {
        if (this.mIsAvailable && !this.mIsBound) {
            this.mIsAvailable = this.mActivity.bindService(new Intent(HOTWORD_SERVICE).setPackage("com.google.android.googlequicksearchbox"), this.mConnection, 1);
            this.mIsBound = this.mIsAvailable;
            if (!this.mIsAvailable) {
                Log.w(TAG, "Hotword service is not available.");
            }
        }
    }

    private void internalRequestHotword() {
        boolean start = this.mIsFocused && this.mIsRequested;
        if (this.mHotwordStart != start) {
            try {
                this.mHotwordStart = start;
                if (!this.mIsBound) {
                    internalBind();
                } else if (this.mHotwordService != null) {
                    this.mHotwordService.requestHotwordDetection(this.mActivity.getPackageName(), start);
                }
            } catch (RemoteException e) {
                Log.w(TAG, "requestHotwordDetection - remote call failed", e);
            }
        }
    }
}

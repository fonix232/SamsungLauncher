package com.android.launcher3.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings.Secure;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherModel.PackageUpdatedTask;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.view.LiveIconManager;
import com.android.launcher3.folder.folderlock.FolderLock;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class SSecureUpdater {
    private static final String ACTION_SSECURE_UPDATE = "com.samsung.applock.intent.action.SSECURE_UPDATE";
    private static final String PERMISSION_SSECURE_UPDATE = "com.samsung.applock.permission.SSECURE_UPDATE";
    private static final String TAG = "SSecureUpdater";
    private boolean isAppLockEnable;
    private Context mAppContext;
    private SSecureAppStateChangeReceiver mAppStateReceiver;
    private ApplockStateChangedRecevier mStateReceiver;

    private class ApplockStateChangedRecevier extends BroadcastReceiver {
        private ApplockStateChangedRecevier() {
        }

        public void onReceive(Context context, Intent intent) {
            if (FolderLock.APPLOCK_ENABLE_CHANGED.equals(intent.getAction())) {
                boolean value = intent.getBooleanExtra("android.intent.extra.SUBJECT", false);
                Log.d(SSecureUpdater.TAG, "applock enabled value = " + value);
                if (value != SSecureUpdater.this.isAppLockEnable) {
                    StringTokenizer tokenizer;
                    SSecureUpdater.this.isAppLockEnable = value;
                    Log.d(SSecureUpdater.TAG, "isAppLockEnabled " + SSecureUpdater.this.isAppLockEnable);
                    ArrayList<String> packages = new ArrayList();
                    String lockedPackages = Secure.getString(SSecureUpdater.this.mAppContext.getContentResolver(), "applock_locked_apps_packages");
                    String hiddenPackages = Secure.getString(SSecureUpdater.this.mAppContext.getContentResolver(), "ssecure_hidden_apps_packages");
                    if (lockedPackages != null) {
                        tokenizer = new StringTokenizer(lockedPackages, ",");
                        while (tokenizer.hasMoreElements()) {
                            packages.add(tokenizer.nextToken());
                        }
                    }
                    if (hiddenPackages != null) {
                        tokenizer = new StringTokenizer(hiddenPackages, ",");
                        while (tokenizer.hasMoreElements()) {
                            packages.add(tokenizer.nextToken());
                        }
                    }
                    if (!packages.isEmpty()) {
                        LauncherModel launcherModel = LauncherAppState.getInstance().getModel();
                        launcherModel.getClass();
                        launcherModel.enqueueItemUpdatedTask(new PackageUpdatedTask(2, (String[]) packages.toArray(new String[packages.size()]), UserHandleCompat.myUserHandle()));
                    }
                }
            }
        }
    }

    private static class SSecureAppStateChangeReceiver extends BroadcastReceiver {
        private SSecureAppStateChangeReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (SSecureUpdater.ACTION_SSECURE_UPDATE.equals(intent.getAction())) {
                ArrayList<String> packages = intent.getStringArrayListExtra("package_list");
                Log.d(SSecureUpdater.TAG, "SSecure: refreshing packages : " + packages);
                if (packages != null && !packages.isEmpty()) {
                    for (String pkgName : LiveIconManager.getCalendarPackages()) {
                        if (packages.contains(pkgName)) {
                            LiveIconManager.clearLiveIconCache(pkgName);
                        }
                    }
                    LauncherModel launcherModel = LauncherAppState.getInstance().getModel();
                    launcherModel.getClass();
                    launcherModel.enqueueItemUpdatedTask(new PackageUpdatedTask(2, (String[]) packages.toArray(new String[packages.size()]), UserHandleCompat.myUserHandle()));
                }
            }
        }
    }

    private static class SingletonHolder {
        private static final SSecureUpdater sSSecureUpdaterInstance = new SSecureUpdater();

        private SingletonHolder() {
        }
    }

    public static SSecureUpdater getInstance() {
        return SingletonHolder.sSSecureUpdaterInstance;
    }

    private SSecureUpdater() {
        boolean z = false;
        this.isAppLockEnable = false;
        this.mAppContext = LauncherAppState.getInstance().getContext();
        if (isAppLockEnable() && LauncherFeature.isSSecureSupported()) {
            z = true;
        }
        this.isAppLockEnable = z;
        this.mStateReceiver = new ApplockStateChangedRecevier();
        this.mAppStateReceiver = new SSecureAppStateChangeReceiver();
    }

    public void setup() {
        IntentFilter stateFilter = new IntentFilter();
        stateFilter.addAction(FolderLock.APPLOCK_ENABLE_CHANGED);
        this.mAppContext.registerReceiver(this.mStateReceiver, stateFilter, FolderLock.PERMISSION_APPLOCK_STATE_CHANGED, null);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SSECURE_UPDATE);
        this.mAppContext.registerReceiver(this.mAppStateReceiver, filter, PERMISSION_SSECURE_UPDATE, null);
    }

    private boolean isAppLockEnable() {
        if (Secure.getInt(this.mAppContext.getContentResolver(), "app_lock_enabled", 0) != 0) {
            return true;
        }
        return false;
    }

    public void onDestroy() {
        try {
            if (this.mAppStateReceiver != null) {
                this.mAppContext.unregisterReceiver(this.mAppStateReceiver);
            }
            if (this.mStateReceiver != null) {
                this.mAppContext.unregisterReceiver(this.mStateReceiver);
            }
        } catch (Exception e) {
            Log.e(TAG, "Can not unregister a non registered receiver :" + e);
        }
    }
}

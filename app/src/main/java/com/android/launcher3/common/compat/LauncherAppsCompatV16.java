package com.android.launcher3.common.compat;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.compat.LauncherAppsCompat.OnAppsChangedCallbackCompat;
import com.android.vcard.VCardConfig;
import java.util.ArrayList;
import java.util.List;

public class LauncherAppsCompatV16 extends LauncherAppsCompat {
    private List<OnAppsChangedCallbackCompat> mCallbacks = new ArrayList();
    private Context mContext;
    private PackageMonitor mPackageMonitor;
    private PackageManager mPm;

    class PackageMonitor extends BroadcastReceiver {
        PackageMonitor() {
        }

        public void onReceive(Context context, Intent intent) {
            boolean z = false;
            String action = intent.getAction();
            UserHandleCompat user = UserHandleCompat.myUserHandle();
            boolean replacing;
            if ("android.intent.action.PACKAGE_CHANGED".equals(action) || "android.intent.action.PACKAGE_REMOVED".equals(action) || "android.intent.action.PACKAGE_ADDED".equals(action)) {
                String packageName = intent.getData().getSchemeSpecificPart();
                replacing = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
                if (packageName != null && packageName.length() != 0) {
                    if ("android.intent.action.PACKAGE_CHANGED".equals(action)) {
                        for (OnAppsChangedCallbackCompat callback : LauncherAppsCompatV16.this.getCallbacks()) {
                            callback.onPackageChanged(packageName, user);
                        }
                    } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                        if (!replacing) {
                            for (OnAppsChangedCallbackCompat callback2 : LauncherAppsCompatV16.this.getCallbacks()) {
                                callback2.onPackageRemoved(packageName, user);
                            }
                        }
                    } else if (!"android.intent.action.PACKAGE_ADDED".equals(action)) {
                    } else {
                        if (replacing) {
                            for (OnAppsChangedCallbackCompat callback22 : LauncherAppsCompatV16.this.getCallbacks()) {
                                callback22.onPackageChanged(packageName, user);
                            }
                            return;
                        }
                        for (OnAppsChangedCallbackCompat callback222 : LauncherAppsCompatV16.this.getCallbacks()) {
                            callback222.onPackageAdded(packageName, user);
                        }
                    }
                }
            } else if ("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(action)) {
                String str = "android.intent.extra.REPLACING";
                if (!Utilities.ATLEAST_KITKAT) {
                    z = true;
                }
                replacing = intent.getBooleanExtra(str, z);
                packages = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                for (OnAppsChangedCallbackCompat callback2222 : LauncherAppsCompatV16.this.getCallbacks()) {
                    callback2222.onPackagesAvailable(packages, user, replacing);
                }
            } else if ("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
                replacing = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
                packages = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                for (OnAppsChangedCallbackCompat callback22222 : LauncherAppsCompatV16.this.getCallbacks()) {
                    callback22222.onPackagesUnavailable(packages, user, replacing);
                }
            }
        }
    }

    LauncherAppsCompatV16(Context context) {
        this.mPm = context.getPackageManager();
        this.mContext = context;
        this.mPackageMonitor = new PackageMonitor();
    }

    public List<LauncherActivityInfoCompat> getActivityList(String packageName, UserHandleCompat user) {
        Intent mainIntent = new Intent("android.intent.action.MAIN", null);
        mainIntent.addCategory("android.intent.category.LAUNCHER");
        mainIntent.setPackage(packageName);
        List<ResolveInfo> infos = this.mPm.queryIntentActivities(mainIntent, 0);
        List<LauncherActivityInfoCompat> list = new ArrayList(infos.size());
        for (ResolveInfo info : infos) {
            list.add(new LauncherActivityInfoCompatV16(this.mContext, info));
        }
        return list;
    }

    public LauncherActivityInfoCompat resolveActivity(Intent intent, UserHandleCompat user) {
        ResolveInfo info = this.mPm.resolveActivity(intent, 0);
        if (info != null) {
            return new LauncherActivityInfoCompatV16(this.mContext, info);
        }
        return null;
    }

    public void startActivityForProfile(ComponentName component, UserHandleCompat user, Rect sourceBounds, Bundle opts) {
        Intent launchIntent = new Intent("android.intent.action.MAIN");
        launchIntent.addCategory("android.intent.category.LAUNCHER");
        launchIntent.setComponent(component);
        launchIntent.setSourceBounds(sourceBounds);
        launchIntent.addFlags(VCardConfig.FLAG_REFRAIN_QP_TO_NAME_PROPERTIES);
        this.mContext.startActivity(launchIntent, opts);
    }

    public void showAppDetailsForProfile(ComponentName component, UserHandleCompat user) {
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts("package", component.getPackageName(), null));
        intent.setFlags(276856832);
        this.mContext.startActivity(intent, null);
    }

    public synchronized void addOnAppsChangedCallback(OnAppsChangedCallbackCompat callback) {
        if (callback != null) {
            if (!this.mCallbacks.contains(callback)) {
                this.mCallbacks.add(callback);
                if (this.mCallbacks.size() == 1) {
                    registerForPackageIntents();
                }
            }
        }
    }

    public synchronized void removeOnAppsChangedCallback(OnAppsChangedCallbackCompat callback) {
        this.mCallbacks.remove(callback);
        if (this.mCallbacks.size() == 0) {
            unregisterForPackageIntents();
        }
    }

    public boolean isPackageEnabledForProfile(String packageName, UserHandleCompat user) {
        return isAppEnabled(this.mPm, packageName, 0);
    }

    public boolean isActivityEnabledForProfile(ComponentName component, UserHandleCompat user) {
        try {
            ActivityInfo info = this.mPm.getActivityInfo(component, 0);
            if (info == null || !info.isEnabled()) {
                return false;
            }
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    private void unregisterForPackageIntents() {
        this.mContext.unregisterReceiver(this.mPackageMonitor);
    }

    private void registerForPackageIntents() {
        IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addDataScheme("package");
        this.mContext.registerReceiver(this.mPackageMonitor, filter);
        filter = new IntentFilter();
        filter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        filter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        this.mContext.registerReceiver(this.mPackageMonitor, filter);
    }

    synchronized List<OnAppsChangedCallbackCompat> getCallbacks() {
        return new ArrayList(this.mCallbacks);
    }
}

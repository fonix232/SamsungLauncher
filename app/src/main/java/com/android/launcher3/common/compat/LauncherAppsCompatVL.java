package com.android.launcher3.common.compat;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.Callback;
import android.content.pm.ShortcutInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.common.compat.LauncherAppsCompat.OnAppsChangedCallbackCompat;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutInfoCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TargetApi(21)
public class LauncherAppsCompatVL extends LauncherAppsCompat {
    private Map<OnAppsChangedCallbackCompat, WrappedCallback> mCallbacks = new HashMap();
    private LauncherApps mLauncherApps;
    private UserManager mUm;

    private static class WrappedCallback extends Callback {
        private OnAppsChangedCallbackCompat mCallback;

        public WrappedCallback(OnAppsChangedCallbackCompat callback) {
            this.mCallback = callback;
        }

        public void onPackageRemoved(String packageName, UserHandle user) {
            this.mCallback.onPackageRemoved(packageName, UserHandleCompat.fromUser(user));
        }

        public void onPackageAdded(String packageName, UserHandle user) {
            this.mCallback.onPackageAdded(packageName, UserHandleCompat.fromUser(user));
        }

        public void onPackageChanged(String packageName, UserHandle user) {
            this.mCallback.onPackageChanged(packageName, UserHandleCompat.fromUser(user));
        }

        public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
            this.mCallback.onPackagesAvailable(packageNames, UserHandleCompat.fromUser(user), replacing);
        }

        public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
            this.mCallback.onPackagesUnavailable(packageNames, UserHandleCompat.fromUser(user), replacing);
        }

        public void onPackagesSuspended(String[] packageNames, UserHandle user) {
            this.mCallback.onPackagesSuspended(packageNames, UserHandleCompat.fromUser(user));
        }

        public void onPackagesUnsuspended(String[] packageNames, UserHandle user) {
            this.mCallback.onPackagesUnsuspended(packageNames, UserHandleCompat.fromUser(user));
        }

        @TargetApi(25)
        public void onShortcutsChanged(String packageName, List<ShortcutInfo> shortcuts, UserHandle user) {
            if (LauncherFeature.supportDeepShortcut()) {
                List<ShortcutInfoCompat> shortcutInfoCompats = new ArrayList(shortcuts.size());
                for (ShortcutInfo shortcutInfo : shortcuts) {
                    shortcutInfoCompats.add(new ShortcutInfoCompat(shortcutInfo));
                }
                this.mCallback.onShortcutsChanged(packageName, shortcutInfoCompats, UserHandleCompat.fromUser(user));
            }
        }
    }

    LauncherAppsCompatVL(Context context) {
        this.mLauncherApps = (LauncherApps) context.getSystemService("launcherapps");
        this.mUm = (UserManager) context.getSystemService("user");
    }

    public List<LauncherActivityInfoCompat> getActivityList(String packageName, UserHandleCompat user) {
        List<LauncherActivityInfo> list = this.mLauncherApps.getActivityList(packageName, user.getUser());
        if (list.size() == 0) {
            return Collections.emptyList();
        }
        List<LauncherActivityInfoCompat> compatList = new ArrayList(list.size());
        for (LauncherActivityInfo info : list) {
            compatList.add(new LauncherActivityInfoCompatVL(info));
        }
        return compatList;
    }

    public LauncherActivityInfoCompat resolveActivity(Intent intent, UserHandleCompat user) {
        if (!this.mUm.getUserProfiles().contains(user.getUser())) {
            return null;
        }
        LauncherActivityInfo activity = this.mLauncherApps.resolveActivity(intent, user.getUser());
        if (activity != null) {
            return new LauncherActivityInfoCompatVL(activity);
        }
        return null;
    }

    public void startActivityForProfile(ComponentName component, UserHandleCompat user, Rect sourceBounds, Bundle opts) {
        this.mLauncherApps.startMainActivity(component, user.getUser(), sourceBounds, opts);
    }

    public void showAppDetailsForProfile(ComponentName component, UserHandleCompat user) {
        this.mLauncherApps.startAppDetailsActivity(component, user.getUser(), null, null);
    }

    public void addOnAppsChangedCallback(OnAppsChangedCallbackCompat callback) {
        WrappedCallback wrappedCallback = new WrappedCallback(callback);
        synchronized (this.mCallbacks) {
            this.mCallbacks.put(callback, wrappedCallback);
        }
        this.mLauncherApps.registerCallback(wrappedCallback);
    }

    public void removeOnAppsChangedCallback(OnAppsChangedCallbackCompat callback) {
        synchronized (this.mCallbacks) {
            WrappedCallback wrappedCallback = (WrappedCallback) this.mCallbacks.remove(callback);
        }
        if (wrappedCallback != null) {
            this.mLauncherApps.unregisterCallback(wrappedCallback);
        }
    }

    public boolean isPackageEnabledForProfile(String packageName, UserHandleCompat user) {
        return this.mLauncherApps.isPackageEnabled(packageName, user.getUser());
    }

    public boolean isActivityEnabledForProfile(ComponentName component, UserHandleCompat user) {
        return this.mLauncherApps.isActivityEnabled(component, user.getUser());
    }
}

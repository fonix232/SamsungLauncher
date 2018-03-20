package com.android.launcher3.util;

import android.service.notification.StatusBarNotification;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.quickoption.shortcuts.DeepShortcutManager;
import java.util.Arrays;

public class PackageUserKey {
    private int mHashCode;
    public String mPackageName;
    private String mTargetActivity;
    public UserHandleCompat mUser;

    public static PackageUserKey fromItemInfo(IconInfo info) {
        return new PackageUserKey(info.getTargetComponent().getPackageName(), info.user);
    }

    public static PackageUserKey fromNotification(StatusBarNotification notification) {
        return new PackageUserKey(notification.getPackageName(), UserHandleCompat.fromUser(notification.getUser()));
    }

    public PackageUserKey(String packageName, UserHandleCompat user) {
        update(packageName, user);
    }

    private void update(String packageName, UserHandleCompat user) {
        this.mPackageName = packageName;
        this.mUser = user;
        this.mHashCode = Arrays.hashCode(new Object[]{packageName, user});
    }

    public boolean updateFromItemInfo(IconInfo info) {
        if (!DeepShortcutManager.supportsBadgeType(info)) {
            return false;
        }
        update(info.getTargetComponent().getPackageName(), info.user);
        return true;
    }

    public int hashCode() {
        return this.mHashCode;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof PackageUserKey)) {
            return false;
        }
        PackageUserKey otherKey = (PackageUserKey) obj;
        if (this.mPackageName.equals(otherKey.mPackageName) && this.mUser.equals(otherKey.mUser)) {
            return true;
        }
        return false;
    }

    public void setTargetActivity(String activity) {
        this.mTargetActivity = activity;
    }

    public String getTargetActivity() {
        return this.mTargetActivity;
    }
}

package com.android.launcher3;

import android.support.annotation.Nullable;
import android.util.Log;
import com.android.launcher3.notification.NotificationInfo;
import com.android.launcher3.notification.NotificationKeyData;
import com.android.launcher3.util.PackageUserKey;
import java.util.ArrayList;
import java.util.List;

public class BadgeInfo {
    public static final int MAX_COUNT = 999;
    static final String TAG = "Notification.Badge";
    private NotificationInfo mNotificationInfo;
    private List<NotificationKeyData> mNotificationKeys = new ArrayList();
    private PackageUserKey mPackageUserKey;
    private int mTotalCount;

    public BadgeInfo(PackageUserKey packageUserKey) {
        this.mPackageUserKey = packageUserKey;
    }

    public boolean addOrUpdateNotificationKey(NotificationKeyData notificationKey) {
        NotificationKeyData prevKey;
        int indexOfPrevKey = this.mNotificationKeys.indexOf(notificationKey);
        if (indexOfPrevKey == -1) {
            prevKey = null;
        } else {
            prevKey = (NotificationKeyData) this.mNotificationKeys.get(indexOfPrevKey);
        }
        if (prevKey == null) {
            boolean added = this.mNotificationKeys.add(notificationKey);
            if (added) {
                this.mTotalCount += notificationKey.count;
            }
            Log.d(TAG, "notificationKey : " + notificationKey.notificationKey + ", count : " + notificationKey.count + ", mTotalCount : " + this.mTotalCount);
            return added;
        } else if (prevKey.count == notificationKey.count) {
            return false;
        } else {
            this.mTotalCount -= prevKey.count;
            this.mTotalCount += notificationKey.count;
            prevKey.count = notificationKey.count;
            return true;
        }
    }

    public boolean removeNotificationKey(NotificationKeyData notificationKey) {
        boolean removed = this.mNotificationKeys.remove(notificationKey);
        if (removed) {
            this.mTotalCount -= notificationKey.count;
        }
        return removed;
    }

    public List<NotificationKeyData> getNotificationKeys() {
        return this.mNotificationKeys;
    }

    public PackageUserKey getPackageUserKey() {
        return this.mPackageUserKey;
    }

    public int getNotificationCount() {
        return Math.min(this.mTotalCount, MAX_COUNT);
    }

    public void setNotificationToShow(@Nullable NotificationInfo notificationInfo) {
        this.mNotificationInfo = notificationInfo;
    }

    public boolean hasNotificationToShow() {
        return this.mNotificationInfo != null;
    }

    public boolean shouldBeInvalidated(BadgeInfo newBadge) {
        return this.mPackageUserKey.equals(newBadge.mPackageUserKey) && (getNotificationCount() != newBadge.getNotificationCount() || hasNotificationToShow());
    }

    public void setPackageUserKey(PackageUserKey mPackageUserKey) {
        this.mPackageUserKey = mPackageUserKey;
    }
}

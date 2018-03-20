package com.android.launcher3.notification;

import android.app.Notification;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class NotificationKeyData {
    private static final int SHOW_NO_BADGE = -100;
    static final String TAG = "Notification.Badge";
    public int count;
    public final String notificationKey;
    public final String shortcutId;

    private NotificationKeyData(String notificationKey, String shortcutId, int count) {
        this.notificationKey = notificationKey;
        this.shortcutId = shortcutId;
        if (count == -100) {
            this.count = 0;
        } else {
            this.count = Math.max(1, count);
        }
        Log.d(TAG, "notificationKey : " + notificationKey + ", count : " + count + ", this.count : " + this.count);
    }

    public static NotificationKeyData fromNotification(StatusBarNotification sbn) {
        Notification notif = sbn.getNotification();
        return new NotificationKeyData(sbn.getKey(), notif.getShortcutId(), notif.number);
    }

    public static List<String> extractKeysOnly(@NonNull List<NotificationKeyData> notificationKeys) {
        List<String> keysOnly = new ArrayList(notificationKeys.size());
        for (NotificationKeyData notificationKeyData : notificationKeys) {
            keysOnly.add(notificationKeyData.notificationKey);
        }
        return keysOnly;
    }

    public boolean equals(Object obj) {
        if (obj instanceof NotificationKeyData) {
            return ((NotificationKeyData) obj).notificationKey.equals(this.notificationKey);
        }
        return false;
    }
}

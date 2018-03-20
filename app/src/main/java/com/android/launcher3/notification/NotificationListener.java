package com.android.launcher3.notification;

import android.app.Notification;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.provider.Settings.Secure;
import android.service.notification.NotificationListenerService;
import android.service.notification.NotificationListenerService.Ranking;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.PackageUserKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class NotificationListener extends NotificationListenerService {
    static final String NOTI_TAG = "Notification.Badge";
    public static final String PREFERENCES_FIRST_LAUNCHER_TIME = "first_launcher_time";
    private static final String PREFERENCES_GRANT_NOTIFICATION_LISTENER = "grant_notification_listener";
    private static Context mContext;
    private static boolean sIsConnected;
    private static NotificationListener sNotificationListenerInstance = null;
    private static NotificationsChangedListener sNotificationsChangedListener;
    private Ranking mTempRanking = new Ranking();

    public interface NotificationsChangedListener {
        void onNotificationFullRefresh(List<StatusBarNotification> list);

        void onNotificationPosted(PackageUserKey packageUserKey, NotificationKeyData notificationKeyData, boolean z, Notification notification);

        void onNotificationRemoved(PackageUserKey packageUserKey, NotificationKeyData notificationKeyData, Notification notification);
    }

    public NotificationListener() {
        Log.d(NOTI_TAG, "NotificationListener()");
        sNotificationListenerInstance = this;
    }

    public void onListenerConnected() {
        Log.d(NOTI_TAG, "onListenerConnected()");
        super.onListenerConnected();
        sIsConnected = true;
        onNotificationFullRefresh();
    }

    public void onListenerDisconnected() {
        Log.d(NOTI_TAG, "onListenerDisconnected()");
        super.onListenerDisconnected();
        sIsConnected = false;
    }

    @Nullable
    public static NotificationListener getInstanceIfConnected() {
        return sIsConnected ? sNotificationListenerInstance : null;
    }

    public static void setNotificationsChangedListener(NotificationsChangedListener listener, Context context) {
        sNotificationsChangedListener = listener;
        mContext = context;
        checkNotificationListenerAccessPref();
        if (sNotificationListenerInstance != null) {
            sNotificationListenerInstance.onNotificationFullRefresh();
        } else {
            Log.d(NOTI_TAG, "setNotificationsChangedListener() sNotificationListenerInstance is null");
        }
    }

    private static void checkNotificationListenerAccessPref() {
        if (getNotificationListenerAccessPref()) {
            setNotificationListenerAccessPref();
            grantNotificationListenerAccess(mContext.getPackageName());
            setFirstLauncherTimeToPref();
        }
    }

    private static void setFirstLauncherTimeToPref() {
        long firstLauncherTime = System.currentTimeMillis() / 1000;
        Editor prefsEdit = mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
        prefsEdit.putLong(PREFERENCES_FIRST_LAUNCHER_TIME, firstLauncherTime);
        prefsEdit.apply();
    }

    private static void setNotificationListenerAccessPref() {
        Editor prefsEdit = mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
        prefsEdit.putBoolean(PREFERENCES_GRANT_NOTIFICATION_LISTENER, false);
        prefsEdit.apply();
    }

    private static boolean getNotificationListenerAccessPref() {
        return mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).getBoolean(PREFERENCES_GRANT_NOTIFICATION_LISTENER, true);
    }

    private static void grantNotificationListenerAccess(String pkg) {
        ArraySet<ComponentName> possibleServices = loadComponentNames(mContext.getPackageManager(), "android.service.notification.NotificationListenerService", "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE");
        ContentResolver resolver = mContext.getContentResolver();
        ArraySet<String> current = getNotificationListeners(resolver);
        Iterator it = possibleServices.iterator();
        while (it.hasNext()) {
            ComponentName c = (ComponentName) it.next();
            String flatName = c.flattenToString();
            if (Objects.equals(c.getPackageName(), pkg) && !current.contains(flatName)) {
                current.add(flatName);
                Secure.putString(resolver, "enabled_notification_listeners", formatSettings(current));
                return;
            }
        }
    }

    private static String formatSettings(Collection<String> c) {
        if (c == null || c.isEmpty()) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        boolean start = true;
        for (String s : c) {
            if (!"".equals(s)) {
                if (!start) {
                    b.append(':');
                }
                b.append(s);
                start = false;
            }
        }
        return b.toString();
    }

    private static ArraySet<ComponentName> loadComponentNames(PackageManager pm, String serviceName, String permissionName) {
        ArraySet<ComponentName> installed = new ArraySet();
        List<ResolveInfo> installedServices = pm.queryIntentServices(new Intent(serviceName), 786564);
        if (installedServices != null) {
            int count = installedServices.size();
            for (int i = 0; i < count; i++) {
                ServiceInfo info = ((ResolveInfo) installedServices.get(i)).serviceInfo;
                ComponentName component = new ComponentName(info.packageName, info.name);
                if (permissionName.equals(info.permission)) {
                    installed.add(component);
                }
            }
        }
        return installed;
    }

    private static ArraySet<String> getNotificationListeners(ContentResolver resolver) {
        String flat = Secure.getString(resolver, "enabled_notification_listeners");
        ArraySet<String> current = new ArraySet();
        if (flat != null) {
            for (String s : flat.split(":")) {
                if (!TextUtils.isEmpty(s)) {
                    current.add(s);
                }
            }
        }
        return current;
    }

    public static void removeNotificationsChangedListener() {
        sNotificationsChangedListener = null;
    }

    private void onNotificationFullRefresh() {
        RuntimeException e;
        List<StatusBarNotification> activeNotifications = null;
        try {
            if (sIsConnected) {
                activeNotifications = filterNotifications(getActiveNotifications());
            } else {
                Object activeNotifications2 = new ArrayList();
            }
        } catch (SecurityException e2) {
            e = e2;
            Log.e(NOTI_TAG, "can't getActiveNotifications " + e.toString());
            if (sNotificationsChangedListener != null) {
                return;
            }
        } catch (NullPointerException e3) {
            e = e3;
            Log.e(NOTI_TAG, "can't getActiveNotifications " + e.toString());
            if (sNotificationsChangedListener != null) {
            }
            return;
        }
        if (sNotificationsChangedListener != null && activeNotifications != null) {
            sNotificationsChangedListener.onNotificationFullRefresh(activeNotifications);
        }
    }

    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        if (sbn == null) {
            Log.d(NOTI_TAG, "onNotificationPosted().sbn is null");
        } else if (sNotificationsChangedListener != null) {
            Log.d(NOTI_TAG, "[valid] onNotificationPosted : " + sbn);
            sNotificationsChangedListener.onNotificationPosted(PackageUserKey.fromNotification(sbn), NotificationKeyData.fromNotification(sbn), shouldBeFilteredOut(sbn), sbn.getNotification());
        }
    }

    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        if (sbn == null) {
            Log.d(NOTI_TAG, "onNotificationRemoved().sbn is null");
        } else if (sNotificationsChangedListener != null) {
            Log.d(NOTI_TAG, "[valid] onNotificationRemoved : " + sbn);
            sNotificationsChangedListener.onNotificationRemoved(PackageUserKey.fromNotification(sbn), NotificationKeyData.fromNotification(sbn), sbn.getNotification());
        }
    }

    public List<StatusBarNotification> getNotificationsForKeys(List<NotificationKeyData> keys) {
        StatusBarNotification[] notifications = getActiveNotifications((String[]) NotificationKeyData.extractKeysOnly(keys).toArray(new String[keys.size()]));
        return notifications == null ? Collections.EMPTY_LIST : Arrays.asList(notifications);
    }

    private List<StatusBarNotification> filterNotifications(StatusBarNotification[] notifications) {
        if (notifications == null) {
            return null;
        }
        int i;
        Set<Integer> removedNotifications = new HashSet();
        for (i = 0; i < notifications.length; i++) {
            if (shouldBeFilteredOut(notifications[i])) {
                removedNotifications.add(Integer.valueOf(i));
            }
        }
        List<StatusBarNotification> filteredNotifications = new ArrayList(notifications.length - removedNotifications.size());
        for (i = 0; i < notifications.length; i++) {
            if (!removedNotifications.contains(Integer.valueOf(i))) {
                filteredNotifications.add(notifications[i]);
            }
        }
        return filteredNotifications;
    }

    public boolean shouldBeFilteredOut(StatusBarNotification sbn) {
        boolean z = false;
        getCurrentRanking().getRanking(sbn.getKey(), this.mTempRanking);
        if (mContext == null || Utilities.getBadgeSettingValue(mContext) == -1) {
            Log.d(NOTI_TAG, "[filtered] shouldBeFilteredOut().mContext[" + mContext + "], or Utilities.NO_BADGE, [sbn : " + sbn + "]");
            return true;
        } else if (this.mTempRanking.canShowBadge()) {
            Notification notification = sbn.getNotification();
            if (this.mTempRanking.getChannel() == null || this.mTempRanking.getChannel().getId() == null || !this.mTempRanking.getChannel().getId().equals("miscellaneous") || (notification.flags & 2) == 0) {
                boolean isGroupHeader;
                boolean missingTitleAndText;
                if ((notification.flags & 512) != 0) {
                    isGroupHeader = true;
                } else {
                    isGroupHeader = false;
                }
                CharSequence title = notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE);
                CharSequence text = notification.extras.getCharSequence(NotificationCompat.EXTRA_TEXT);
                if (TextUtils.isEmpty(title) && TextUtils.isEmpty(text)) {
                    missingTitleAndText = true;
                } else {
                    missingTitleAndText = false;
                }
                if (isGroupHeader || missingTitleAndText) {
                    Log.d(NOTI_TAG, "[filtered] shouldBeFilteredOut().isGroupHeader[" + isGroupHeader + "], missingTitleAndText[" + missingTitleAndText + "], [sbn : " + sbn + "]");
                }
                if (isGroupHeader || missingTitleAndText) {
                    z = true;
                }
                return z;
            }
            Log.d(NOTI_TAG, "[filtered] shouldBeFilteredOut().Notification.FLAG_ONGOING_EVENT [sbn : " + sbn + "]");
            return true;
        } else {
            Log.d(NOTI_TAG, "[filtered] shouldBeFilteredOut().mTempRanking.canShowBadge[" + this.mTempRanking.canShowBadge() + "], [sbn : " + sbn + "]");
            return true;
        }
    }

    public static void setApplicationContext(Context context) {
        mContext = context;
    }
}

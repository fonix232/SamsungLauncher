package com.android.launcher3.common.view;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.util.BitmapUtils;
import com.android.vcard.VCardConfig;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class LiveIconManager {
    public static final String CALENDAR_ALARM_INTENT_NAME = "com.samsung.action.MIDNIGHT_LIVEICONUPDATE";
    public static final String CLOCK_ALARM_INTENT_NAME = "com.samsung.action.EVERY_MINUTE_CLOCK_UPDATE";
    private static final String DEFAULT_PACKAGE_NAME_ANDROID_CALENDAR = "com.android.calendar";
    public static final String DEFAULT_PACKAGE_NAME_CLOCK = "com.sec.android.app.clockpackage";
    private static final String DEFAULT_PACKAGE_NAME_SAMSUNG_CALENDAR = "com.samsung.android.calendar";
    private static final String TAG = LiveIconManager.class.getSimpleName();
    private static final BitmapCacheContainer sCache = new BitmapCacheContainer();
    private static PendingIntent sCalendarPendingIntent;
    private static PendingIntent sClockPendingIntent;

    @NonNull
    private static List<String> getLiveIconPackages() {
        return Arrays.asList(new String[]{DEFAULT_PACKAGE_NAME_SAMSUNG_CALENDAR, DEFAULT_PACKAGE_NAME_ANDROID_CALENDAR, LauncherFeature.getFloatingClockPackageName(), LauncherFeature.getCscClockPackageName()});
    }

    @NonNull
    public static List<String> getCalendarPackages() {
        return Arrays.asList(new String[]{DEFAULT_PACKAGE_NAME_SAMSUNG_CALENDAR, DEFAULT_PACKAGE_NAME_ANDROID_CALENDAR});
    }

    @NonNull
    public static Bitmap getLiveIcon(Context context, String packageName, UserHandleCompat user) {
        Bitmap liveIconBitmap = sCache.getBitmapCache(packageName, user);
        if (liveIconBitmap != null) {
            return liveIconBitmap;
        }
        liveIconBitmap = createLiveIconBitmap(context, packageName, user);
        sCache.putBitmapCache(packageName, user, liveIconBitmap);
        Log.i(TAG, "getLiveIcon: complete(sync_created)");
        return liveIconBitmap;
    }

    @NonNull
    private static Bitmap createLiveIconBitmap(Context context, String packageName, UserHandleCompat user) {
        Drawable liveIcon;
        try {
            PackageManager pm = context.getPackageManager();
            liveIcon = pm.getApplicationIcon(packageName);
            if (!(user == null || user.getUser() == null)) {
                liveIcon = pm.getUserBadgedIcon(liveIcon, user.getUser());
            }
        } catch (NameNotFoundException e) {
            Log.i("TAG", "not found set default app icon");
            Log.e("TAG", e.toString());
            liveIcon = context.getResources().getDrawable(17629184, null);
        }
        return BitmapUtils.createIconBitmap(liveIcon, context);
    }

    public static boolean isCalendarPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        return getCalendarPackages().contains(packageName);
    }

    static boolean isLiveIconPackage(IconInfo iconInfo) {
        return (iconInfo == null || iconInfo.intent == null || iconInfo.intent.getComponent() == null || !isLiveIconPackage(iconInfo.intent.getComponent().getPackageName())) ? false : true;
    }

    public static boolean isLiveIconPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        if (packageName.contains("/")) {
            packageName = packageName.split("/")[0];
        }
        if (getLiveIconPackages().contains(packageName)) {
            return true;
        }
        return false;
    }

    public static boolean isKnoxLiveIcon(Intent launchIntent) {
        if (!IconView.isKnoxShortcut(launchIntent)) {
            return false;
        }
        long userId = (long) launchIntent.getIntExtra(IconView.EXTRA_SHORTCUT_USER_ID, -1);
        String pkgName = launchIntent.getStringExtra(IconView.EXTRA_SHORTCUT_LIVE_ICON_COMPONENT);
        if (pkgName == null || !pkgName.contains("/")) {
            return false;
        }
        pkgName = pkgName.split("/")[0];
        if (userId < 100 || !isLiveIconPackage(pkgName)) {
            return false;
        }
        return true;
    }

    static boolean applyKnoxLiveIcon(Launcher launcher, IconInfo info) {
        if (!isKnoxLiveIcon(info.intent)) {
            return false;
        }
        info.mIcon = getLiveIcon(launcher, info.intent.getStringExtra(IconView.EXTRA_SHORTCUT_LIVE_ICON_COMPONENT).split("/")[0], info.user);
        return true;
    }

    public static void clearLiveIconCache(String packageName) {
        sCache.removeBitmapCache(packageName);
    }

    public static void setCalendarAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM);
        if (sCalendarPendingIntent == null) {
            sCalendarPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(CALENDAR_ALARM_INTENT_NAME), VCardConfig.FLAG_CONVERT_PHONETIC_NAME_STRINGS);
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(5, 1);
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        alarmManager.set(0, calendar.getTimeInMillis(), sCalendarPendingIntent);
    }

    public static void cancelCalendarAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM);
        if (sCalendarPendingIntent != null) {
            alarmManager.cancel(sCalendarPendingIntent);
        }
    }

    public static void setClockAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM);
        if (sClockPendingIntent == null) {
            sClockPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(CLOCK_ALARM_INTENT_NAME), VCardConfig.FLAG_CONVERT_PHONETIC_NAME_STRINGS);
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(12, 1);
        calendar.set(13, 0);
        calendar.set(14, 0);
        alarmManager.set(1, calendar.getTimeInMillis(), sClockPendingIntent);
    }

    public static void cancelClockAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM);
        if (sClockPendingIntent != null) {
            alarmManager.cancel(sClockPendingIntent);
        }
    }
}

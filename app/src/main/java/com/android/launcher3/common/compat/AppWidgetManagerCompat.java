package com.android.launcher3.common.compat;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.util.ComponentKey;
import java.util.HashMap;
import java.util.List;

public abstract class AppWidgetManagerCompat {
    public static int SEM_WIDGET_CATEGORY_SAMSUNG_EASYHOME_SCREEN = 256;
    public static int SEM_WIDGET_CATEGORY_SAMSUNG_HOME_SCREEN = 512;
    private static AppWidgetManagerCompat sInstance;
    private static final Object sInstanceLock = new Object();
    final AppWidgetManager mAppWidgetManager;
    final Context mContext;

    public abstract boolean bindAppWidgetIdIfAllowed(int i, AppWidgetProviderInfo appWidgetProviderInfo, Bundle bundle);

    public abstract LauncherAppWidgetProviderInfo findProvider(ComponentName componentName, UserHandleCompat userHandleCompat);

    public abstract List<AppWidgetProviderInfo> getAllProviders();

    public abstract HashMap<ComponentKey, AppWidgetProviderInfo> getAllProvidersMap();

    public abstract Bitmap getBadgeBitmap(LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfo, Bitmap bitmap, int i);

    public abstract UserHandleCompat getUser(LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfo);

    public abstract Drawable loadIcon(LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfo, IconCache iconCache);

    public abstract String loadLabel(LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfo);

    public abstract Drawable loadPreview(AppWidgetProviderInfo appWidgetProviderInfo);

    public abstract void startConfigActivity(AppWidgetProviderInfo appWidgetProviderInfo, int i, Activity activity, AppWidgetHost appWidgetHost, int i2);

    public static AppWidgetManagerCompat getInstance(Context context) {
        AppWidgetManagerCompat appWidgetManagerCompat;
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                if (Utilities.ATLEAST_LOLLIPOP) {
                    sInstance = new AppWidgetManagerCompatVL(context.getApplicationContext());
                } else {
                    sInstance = new AppWidgetManagerCompatV16(context.getApplicationContext());
                }
            }
            appWidgetManagerCompat = sInstance;
        }
        return appWidgetManagerCompat;
    }

    AppWidgetManagerCompat(Context context) {
        this.mContext = context;
        this.mAppWidgetManager = AppWidgetManager.getInstance(context);
    }

    public AppWidgetProviderInfo getAppWidgetInfo(int appWidgetId) {
        return this.mAppWidgetManager.getAppWidgetInfo(appWidgetId);
    }

    public LauncherAppWidgetProviderInfo getLauncherAppWidgetInfo(int appWidgetId) {
        AppWidgetProviderInfo info = getAppWidgetInfo(appWidgetId);
        return info == null ? null : LauncherAppWidgetProviderInfo.fromProviderInfo(this.mContext, info);
    }
}

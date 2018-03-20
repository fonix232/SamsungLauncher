package com.android.launcher3.common.compat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.util.ComponentKey;
import java.util.HashMap;
import java.util.List;

class AppWidgetManagerCompatV16 extends AppWidgetManagerCompat {
    AppWidgetManagerCompatV16(Context context) {
        super(context);
    }

    public List<AppWidgetProviderInfo> getAllProviders() {
        return this.mAppWidgetManager.getInstalledProviders();
    }

    public String loadLabel(LauncherAppWidgetProviderInfo info) {
        return Utilities.trim(info.label);
    }

    @TargetApi(17)
    public boolean bindAppWidgetIdIfAllowed(int appWidgetId, AppWidgetProviderInfo info, Bundle options) {
        if (Utilities.ATLEAST_JB_MR1) {
            return this.mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider, options);
        }
        return this.mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider);
    }

    public UserHandleCompat getUser(LauncherAppWidgetProviderInfo info) {
        return UserHandleCompat.myUserHandle();
    }

    public void startConfigActivity(AppWidgetProviderInfo info, int widgetId, Activity activity, AppWidgetHost host, int requestCode) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_CONFIGURE");
        intent.setComponent(info.configure);
        intent.putExtra(Favorites.APPWIDGET_ID, widgetId);
        Utilities.startActivityForResultSafely(activity, intent, requestCode);
    }

    public Drawable loadPreview(AppWidgetProviderInfo info) {
        return this.mContext.getPackageManager().getDrawable(info.provider.getPackageName(), info.previewImage, null);
    }

    public Drawable loadIcon(LauncherAppWidgetProviderInfo info, IconCache cache) {
        return cache.getFullResIcon(info.provider.getPackageName(), info.icon);
    }

    public Bitmap getBadgeBitmap(LauncherAppWidgetProviderInfo info, Bitmap bitmap, int imageHeight) {
        return bitmap;
    }

    public LauncherAppWidgetProviderInfo findProvider(ComponentName provider, UserHandleCompat user) {
        for (AppWidgetProviderInfo info : this.mAppWidgetManager.getInstalledProviders()) {
            if (info.provider.equals(provider)) {
                return LauncherAppWidgetProviderInfo.fromProviderInfo(this.mContext, info);
            }
        }
        return null;
    }

    public HashMap<ComponentKey, AppWidgetProviderInfo> getAllProvidersMap() {
        HashMap<ComponentKey, AppWidgetProviderInfo> result = new HashMap();
        UserHandleCompat user = UserHandleCompat.myUserHandle();
        for (AppWidgetProviderInfo info : this.mAppWidgetManager.getInstalledProviders()) {
            result.put(new ComponentKey(info.provider, user), info);
        }
        return result;
    }
}

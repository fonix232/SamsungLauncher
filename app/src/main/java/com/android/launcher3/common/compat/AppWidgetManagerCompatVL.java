package com.android.launcher3.common.compat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.widget.Toast;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.Reflection;
import com.sec.android.app.launcher.R;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@TargetApi(21)
class AppWidgetManagerCompatVL extends AppWidgetManagerCompat {
    private static final String TAG = "AppWidgetManagerCompat";
    private static Method sGetInstalledProvidersForProfile = null;
    private final PackageManager mPm;
    private final UserManager mUserManager;

    AppWidgetManagerCompatVL(Context context) {
        super(context);
        this.mPm = context.getPackageManager();
        this.mUserManager = (UserManager) context.getSystemService("user");
    }

    public List<AppWidgetProviderInfo> getAllProviders() {
        ArrayList<AppWidgetProviderInfo> providers = new ArrayList();
        for (UserHandle user : this.mUserManager.getUserProfiles()) {
            try {
                providers.addAll(this.mAppWidgetManager.getInstalledProvidersForProfile(user));
                providers.addAll(getWidgetProvider(false, user));
                if (LauncherAppState.getInstance().isEasyModeEnabled()) {
                    providers.addAll(getWidgetProvider(true, user));
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, e.toString());
            }
        }
        return providers;
    }

    public String loadLabel(LauncherAppWidgetProviderInfo info) {
        return info.getLabel(this.mPm);
    }

    public boolean bindAppWidgetIdIfAllowed(int appWidgetId, AppWidgetProviderInfo info, Bundle options) {
        return this.mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.getProfile(), info.provider, options);
    }

    public UserHandleCompat getUser(LauncherAppWidgetProviderInfo info) {
        if (info.isCustomWidget) {
            return UserHandleCompat.myUserHandle();
        }
        return UserHandleCompat.fromUser(info.getProfile());
    }

    public void startConfigActivity(AppWidgetProviderInfo info, int widgetId, Activity activity, AppWidgetHost host, int requestCode) {
        try {
            host.startAppWidgetConfigureActivityForResult(activity, widgetId, 0, requestCode, null);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.activity_not_found, 0).show();
        } catch (SecurityException e2) {
            Toast.makeText(activity, R.string.activity_not_found, 0).show();
        }
    }

    public Drawable loadPreview(AppWidgetProviderInfo info) {
        return info.loadPreviewImage(this.mContext, 0);
    }

    public Drawable loadIcon(LauncherAppWidgetProviderInfo info, IconCache cache) {
        return info.getIcon(this.mContext, cache);
    }

    public Bitmap getBadgeBitmap(LauncherAppWidgetProviderInfo info, Bitmap bitmap, int imageHeight) {
        if (info.isCustomWidget || info.getProfile().equals(Process.myUserHandle())) {
            return bitmap;
        }
        Resources res = this.mContext.getResources();
        int badgeMinTop = res.getDimensionPixelSize(R.dimen.profile_badge_minimum_top);
        int badgeEndPadding = badgeMinTop;
        int badgeSize = Math.min((imageHeight - badgeMinTop) - badgeEndPadding, res.getDimensionPixelSize(R.dimen.profile_badge_size));
        Rect badgeLocation = new Rect(0, 0, badgeSize, badgeSize);
        int top = Math.max((imageHeight - badgeSize) - badgeEndPadding, badgeMinTop);
        if (res.getConfiguration().getLayoutDirection() == 1) {
            badgeLocation.offset(badgeEndPadding, top);
        } else {
            badgeLocation.offset((bitmap.getWidth() - badgeSize) - badgeEndPadding, top);
        }
        Drawable drawable = this.mPm.getUserBadgedDrawableForDensity(new BitmapDrawable(res, bitmap), info.getProfile(), badgeLocation, 0);
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        bitmap.eraseColor(0);
        Canvas c = new Canvas(bitmap);
        drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        drawable.draw(c);
        c.setBitmap(null);
        return bitmap;
    }

    public LauncherAppWidgetProviderInfo findProvider(ComponentName provider, UserHandleCompat user) {
        ArrayList<AppWidgetProviderInfo> providers = new ArrayList();
        UserHandle userHandle = user.getUser();
        providers.addAll(this.mAppWidgetManager.getInstalledProvidersForProfile(userHandle));
        providers.addAll(getWidgetProvider(false, userHandle));
        if (LauncherAppState.getInstance().isEasyModeEnabled()) {
            providers.addAll(getWidgetProvider(true, userHandle));
        }
        Iterator it = providers.iterator();
        while (it.hasNext()) {
            AppWidgetProviderInfo info = (AppWidgetProviderInfo) it.next();
            if (info.provider.equals(provider)) {
                return LauncherAppWidgetProviderInfo.fromProviderInfo(this.mContext, info);
            }
        }
        return null;
    }

    public HashMap<ComponentKey, AppWidgetProviderInfo> getAllProvidersMap() {
        HashMap<ComponentKey, AppWidgetProviderInfo> result = new HashMap();
        for (UserHandle user : this.mUserManager.getUserProfiles()) {
            UserHandleCompat userHandle = UserHandleCompat.fromUser(user);
            for (AppWidgetProviderInfo info : this.mAppWidgetManager.getInstalledProvidersForProfile(user)) {
                result.put(new ComponentKey(info.provider, userHandle), info);
            }
        }
        return result;
    }

    private List<AppWidgetProviderInfo> getWidgetProvider(boolean isEasyMode, UserHandle user) {
        ArrayList<AppWidgetProviderInfo> providers = new ArrayList();
        Class[] categoryType = new Class[]{Integer.TYPE, UserHandle.class, String.class};
        if (sGetInstalledProvidersForProfile == null) {
            sGetInstalledProvidersForProfile = Reflection.getMethod("android.appwidget.AppWidgetManager", "getInstalledProvidersForProfile", categoryType, true);
        }
        if (sGetInstalledProvidersForProfile != null) {
            AppWidgetManager instance = AppWidgetManager.getInstance(this.mContext);
            Method method = sGetInstalledProvidersForProfile;
            Object[] objArr = new Object[3];
            objArr[0] = Integer.valueOf(isEasyMode ? AppWidgetManagerCompat.SEM_WIDGET_CATEGORY_SAMSUNG_EASYHOME_SCREEN : AppWidgetManagerCompat.SEM_WIDGET_CATEGORY_SAMSUNG_HOME_SCREEN);
            objArr[1] = user;
            objArr[2] = null;
            List<AppWidgetProviderInfo> semWidgets = (List) Reflection.invoke(instance, method, objArr);
            if (!(semWidgets == null || semWidgets.isEmpty())) {
                providers.addAll(semWidgets);
            }
        }
        return providers;
    }
}

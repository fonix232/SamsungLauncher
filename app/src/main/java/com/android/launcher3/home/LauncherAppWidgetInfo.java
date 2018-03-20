package com.android.launcher3.home;

import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.util.Log;
import com.android.launcher3.Launcher;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.model.LauncherSettings.Favorites;

public class LauncherAppWidgetInfo extends ItemInfo {
    public static final int CUSTOM_WIDGET_ID = -100;
    public static final int FLAG_ID_NOT_VALID = 1;
    public static final int FLAG_PROVIDER_NOT_READY = 2;
    static final int FLAG_RESTORE_STARTED = 8;
    public static final int FLAG_UI_NOT_READY = 4;
    private static final int NO_ID = -1;
    static final int RESTORE_COMPLETED = 0;
    private static final String TAG = LauncherAppWidgetInfo.class.getSimpleName();
    public int appWidgetId = -1;
    public AppWidgetHostView hostView = null;
    public int installProgress = -1;
    boolean isRotating = false;
    boolean isWeatherCityOneMore = false;
    private boolean mHasNotifiedInitialWidgetSizeChanged;
    public ComponentName providerName;
    int restoreStatus;

    public LauncherAppWidgetInfo(int appWidgetId, ComponentName providerName) {
        if (appWidgetId == -100) {
            this.itemType = 5;
        } else {
            this.itemType = 4;
        }
        this.appWidgetId = appWidgetId;
        this.providerName = providerName;
        this.container = -100;
        this.spanX = -1;
        this.spanY = -1;
        this.user = UserHandleCompat.myUserHandle();
        this.restoreStatus = 0;
    }

    boolean isCustomWidget() {
        return this.appWidgetId == -100;
    }

    public void onAddToDatabase(Context context, ContentValues values) {
        super.onAddToDatabase(context, values);
        values.put(Favorites.APPWIDGET_ID, Integer.valueOf(this.appWidgetId));
        values.put(Favorites.APPWIDGET_PROVIDER, this.providerName.flattenToString());
        values.put("restored", Integer.valueOf(this.restoreStatus));
    }

    void onBindAppWidget(Launcher launcher) {
        onBindAppWidget(launcher, false);
    }

    void onBindAppWidget(Launcher launcher, boolean force) {
        if (!this.mHasNotifiedInitialWidgetSizeChanged) {
            if (this.hostView != null) {
                this.hostView.updateAppWidgetOptions(AppWidgetResizeFrame.makeAppWidgetOptions(this.hostView));
                Log.i(TAG, "updateAppWidgetOptions, WidgetInfo: " + this);
            }
            notifyWidgetSizeChanged(launcher);
        } else if (force) {
            notifyWidgetSizeChanged(launcher);
        }
    }

    void notifyWidgetSizeChanged(Launcher launcher) {
        AppWidgetResizeFrame.updateWidgetSizeRanges(launcher, this.hostView, this.spanX, this.spanY);
        this.mHasNotifiedInitialWidgetSizeChanged = true;
    }

    public void reinflateWidgetsIfNecessary() {
        if (this.hostView instanceof LauncherAppWidgetHostView) {
            LauncherAppWidgetHostView lahv = this.hostView;
            if (lahv == null) {
                Log.i(TAG, "reinflateWidgetsIfNecessary, HostView is null: " + this);
            } else if (lahv.isReinflateRequired()) {
                lahv.updateAppWidgetOptions(AppWidgetResizeFrame.makeAppWidgetOptions(lahv));
                Log.i(TAG, "reinflateWidgetsIfNecessary, WidgetInfo: " + this);
            } else {
                Log.i(TAG, "reinflateWidgetsIfNecessary, Not required: " + this);
            }
        }
    }

    public String toString() {
        return "AppWidget(id=" + Integer.toString(this.appWidgetId) + ")";
    }

    public void unbind() {
        super.unbind();
        this.hostView = null;
    }

    final boolean isWidgetIdValid() {
        return (this.restoreStatus & 1) == 0;
    }

    final boolean hasRestoreFlag(int flag) {
        return (this.restoreStatus & flag) == flag;
    }
}

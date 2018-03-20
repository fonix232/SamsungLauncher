package com.android.launcher3.common.receiver;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.bnr.LauncherBnrHelper;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import java.util.ArrayList;
import java.util.List;

@SuppressLint({"LongLogTag"})
public class AppWidgetsRestoredReceiver extends BroadcastReceiver {
    private static final String TAG = "AppWidgetsRestoredReceiver";

    static void restoreAppWidgetIds(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        ContentResolver cr = context.getContentResolver();
        final List<Integer> idsToRemove = new ArrayList();
        AppWidgetManager widgets = AppWidgetManager.getInstance(context);
        for (int i = 0; i < oldWidgetIds.length; i++) {
            int state;
            Log.i(TAG, "Widget state restore id " + oldWidgetIds[i] + " => " + newWidgetIds[i]);
            if (LauncherModel.isValidProvider(widgets.getAppWidgetInfo(newWidgetIds[i]))) {
                state = 4;
            } else {
                state = 2;
            }
            ContentValues values = new ContentValues();
            values.put(Favorites.APPWIDGET_ID, Integer.valueOf(newWidgetIds[i]));
            values.put("restored", Integer.valueOf(state));
            String[] widgetIdParams = new String[]{Integer.toString(oldWidgetIds[i])};
            if (cr.update(Favorites.CONTENT_URI, values, "appWidgetId=? and (restored & 1) = 1", widgetIdParams) == 0) {
                Cursor cursor = cr.query(Favorites.CONTENT_URI, new String[]{Favorites.APPWIDGET_ID}, "appWidgetId=?", widgetIdParams, null);
                try {
                    if (!cursor.moveToFirst()) {
                        idsToRemove.add(Integer.valueOf(newWidgetIds[i]));
                    }
                    cursor.close();
                } catch (Throwable th) {
                    cursor.close();
                }
            }
        }
        if (!idsToRemove.isEmpty()) {
            final AppWidgetHost appWidgetHost = new AppWidgetHost(context, 1024);
            new AsyncTask<Void, Void, Void>() {
                public Void doInBackground(Void... args) {
                    for (Integer id : idsToRemove) {
                        appWidgetHost.deleteAppWidgetId(id.intValue());
                        Log.e(AppWidgetsRestoredReceiver.TAG, "Widget no longer present, appWidgetId=" + id);
                    }
                    return null;
                }
            }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR, new Void[]{(Void) null});
        }
        LauncherBnrHelper.getInstance().recreateLauncher(context);
    }

    public void onReceive(Context context, Intent intent) {
        if ("android.appwidget.action.APPWIDGET_HOST_RESTORED".equals(intent.getAction())) {
            int[] oldIds = intent.getIntArrayExtra("appWidgetOldIds");
            int[] newIds = intent.getIntArrayExtra("appWidgetIds");
            if (oldIds == null || newIds == null || oldIds.length != newIds.length) {
                Log.e(TAG, "Invalid host restored received");
            } else {
                restoreAppWidgetIds(context, oldIds, newIds);
            }
        }
    }
}

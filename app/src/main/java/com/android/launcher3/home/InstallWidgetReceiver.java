package com.android.launcher3.home;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.AppWidgetManagerCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

public class InstallWidgetReceiver extends BroadcastReceiver {
    private static final String ACTION_BIND_WIDGET = "com.sec.android.launcher.action.BIND_WIDGET";
    private static final String APP_WIDGET_TYPE_KEY = "isAppWidget";
    private static final String EXTRA_APPWIDGET_ID = "appWidgetId";
    private static final String EXTRA_COMPONENT = "componentName";
    private static final String EXTRA_SPAN_X = "spanX";
    private static final String EXTRA_SPAN_Y = "spanY";
    private static final String TAG = "InstallWidgetReceiver";

    private static class PendingInstallWidgetInfo extends ExternalRequestInfo {
        int appWidgetId;
        final ComponentName componentName;
        final Intent data;
        final Context mContext;
        final AppWidgetProviderInfo providerInfo;
        final int spanX;
        final int spanY;

        public PendingInstallWidgetInfo(Intent data, Context context, int widgetId, long time) {
            super(3, UserHandleCompat.myUserHandle(), time);
            this.data = data;
            this.mContext = context;
            this.componentName = ComponentName.unflattenFromString(data.getStringExtra("componentName"));
            this.spanX = data.getIntExtra("spanX", -1);
            this.spanY = data.getIntExtra("spanY", -1);
            this.appWidgetId = widgetId;
            this.providerInfo = null;
        }

        public PendingInstallWidgetInfo(AppWidgetProviderInfo info, int widgetId, Context context, long time) {
            int i = 0;
            super(3, UserHandleCompat.myUserHandle(), time);
            this.data = null;
            this.mContext = context;
            this.componentName = info.provider;
            LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfo = LauncherAppWidgetProviderInfo.fromProviderInfo(this.mContext, info);
            this.spanX = launcherAppWidgetProviderInfo == null ? 0 : launcherAppWidgetProviderInfo.getSpanX();
            if (launcherAppWidgetProviderInfo != null) {
                i = launcherAppWidgetProviderInfo.getSpanY();
            }
            this.spanY = i;
            this.appWidgetId = widgetId;
            this.providerInfo = info;
        }

        public String encodeToString() {
            try {
                if (this.providerInfo != null) {
                    return new JSONStringer().object().key("type").value(3).key("time").value(this.requestTime).key("componentName").value(this.componentName.flattenToString()).key("appWidgetId").value((long) this.appWidgetId).key(InstallWidgetReceiver.APP_WIDGET_TYPE_KEY).value(true).endObject().toString();
                }
                return new JSONStringer().object().key("type").value(3).key("time").value(this.requestTime).key("componentName").value(this.componentName.flattenToString()).key("spanX").value((long) this.spanX).key("spanY").value((long) this.spanY).key("appWidgetId").value((long) this.appWidgetId).endObject().toString();
            } catch (JSONException e) {
                Log.d(InstallWidgetReceiver.TAG, "Exception when adding widget: " + e);
                return null;
            }
        }

        public void runRequestInfo(Context context) {
            LauncherAppState app = LauncherAppState.getInstance();
            ArrayList<ItemInfo> addWidgets = new ArrayList();
            LauncherAppWidgetInfo appWidgetInfo = new LauncherAppWidgetInfo(this.appWidgetId, this.componentName);
            appWidgetInfo.spanX = this.spanX;
            appWidgetInfo.spanY = this.spanY;
            DeviceProfile profile = LauncherAppState.getInstance().getDeviceProfile();
            if (appWidgetInfo.spanX > profile.homeGrid.getCellCountX()) {
                appWidgetInfo.spanX = profile.homeGrid.getCellCountX();
            }
            if (appWidgetInfo.spanY > profile.homeGrid.getCellCountY()) {
                appWidgetInfo.spanY = profile.homeGrid.getCellCountY();
            }
            addWidgets.add(appWidgetInfo);
            app.getModel().getHomeLoader().addAndBindAddedWorkspaceItems(context, addWidgets, false);
        }

        boolean getContainPackage(ArrayList<String> packageNames) {
            return packageNames.contains(getTargetPackage());
        }

        public String getTargetPackage() {
            return this.componentName.getPackageName();
        }
    }

    public void onReceive(final Context context, final Intent data) {
        if (data != null && ACTION_BIND_WIDGET.equals(data.getAction())) {
            final LauncherAppState app = LauncherAppState.getInstance();
            app.getModel();
            LauncherModel.runOnWorkerThread(new Runnable() {
                public void run() {
                    PendingInstallWidgetInfo info = new PendingInstallWidgetInfo(data, context, -1, -1);
                    if (info.componentName == null) {
                        Log.d(InstallWidgetReceiver.TAG, "ComponentName is null or empty");
                    } else if (info.spanX <= 0 || info.spanY <= 0) {
                        Log.d(InstallWidgetReceiver.TAG, "Span [" + info.spanX + "," + info.spanY + "]");
                    } else if (InstallWidgetReceiver.bindWidget(context, info)) {
                        ExternalRequestQueue.queueExternalRequestInfo(info, context, app);
                    } else {
                        Log.d(InstallWidgetReceiver.TAG, "Unable to bind app widget id " + info.appWidgetId + " component " + info.componentName);
                    }
                }
            });
        }
    }

    public static void queuePendingWidgetInfo(final AppWidgetProviderInfo info, final int widgetId, final Context context) {
        final LauncherAppState app = LauncherAppState.getInstance();
        app.getModel();
        LauncherModel.runOnWorkerThread(new Runnable() {
            public void run() {
                PendingInstallWidgetInfo widgetInfo = new PendingInstallWidgetInfo(info, widgetId, context, -1);
                if (widgetInfo.componentName == null) {
                    Log.d(InstallWidgetReceiver.TAG, "ComponentName is null or empty");
                } else if (widgetInfo.spanX <= 0 || widgetInfo.spanY <= 0) {
                    Log.d(InstallWidgetReceiver.TAG, "Span [" + widgetInfo.spanX + "," + widgetInfo.spanY + "]");
                } else if (InstallWidgetReceiver.bindWidget(context, widgetInfo)) {
                    ExternalRequestQueue.queueExternalRequestInfo(widgetInfo, context, app);
                } else {
                    Log.d(InstallWidgetReceiver.TAG, "Unable to bind app widget id " + widgetInfo.appWidgetId + " component " + widgetInfo.componentName);
                }
            }
        });
    }

    private static boolean bindWidget(Context context, PendingInstallWidgetInfo info) {
        boolean success;
        LauncherAppWidgetProviderInfo appWidgetInfo = HomeLoader.getProviderInfo(context, info.componentName, info.user);
        AppWidgetHost appWidgetHost = new AppWidgetHost(context, 1024);
        info.appWidgetId = appWidgetHost.allocateAppWidgetId();
        if (appWidgetInfo == null && HomeLoader.checkHiddenWidget(context, info.componentName)) {
            success = AppWidgetManager.getInstance(context).bindAppWidgetIdIfAllowed(info.appWidgetId, UserHandleCompat.myUserHandle().getUser(), info.componentName, null);
        } else {
            success = AppWidgetManagerCompat.getInstance(context).bindAppWidgetIdIfAllowed(info.appWidgetId, appWidgetInfo, null);
        }
        if (!success) {
            appWidgetHost.deleteAppWidgetId(info.appWidgetId);
        }
        return success;
    }

    static PendingInstallWidgetInfo decode(String encoded, Context context) {
        try {
            JSONObject object = (JSONObject) new JSONTokener(encoded).nextValue();
            int appWidgetId = object.getInt("appWidgetId");
            long requestTime = object.getLong("time");
            String componentName = object.getString("componentName");
            if (object.optBoolean(APP_WIDGET_TYPE_KEY)) {
                AppWidgetProviderInfo info = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId);
                if (info == null || !info.provider.equals(ComponentName.unflattenFromString(componentName))) {
                    return null;
                }
                return new PendingInstallWidgetInfo(info, appWidgetId, context, requestTime);
            }
            Intent data = new Intent();
            data.putExtra("componentName", componentName);
            data.putExtra("spanX", object.getInt("spanX"));
            data.putExtra("spanY", object.getInt("spanY"));
            return new PendingInstallWidgetInfo(data, context, appWidgetId, requestTime);
        } catch (JSONException e) {
            Log.d(TAG, "Exception reading widget to add: " + e);
            return null;
        }
    }
}

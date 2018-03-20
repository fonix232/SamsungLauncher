package com.android.launcher3.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

public class UninstallWidgetReceiver extends BroadcastReceiver {
    private static final String ACTION_UNBIND_WIDGET = "com.sec.android.launcher.action.UNBIND_WIDGET";
    static final String APPWIDGET_ID_KEY = "appwidgetid";
    private static final String TAG = "UninstallWidget";

    private static class PendingUninstallWidgetInfo extends ExternalRequestInfo {
        final int appWidgetId;
        final Intent data;
        final Context mContext;

        public PendingUninstallWidgetInfo(Intent data, Context context, long time) {
            super(4, UserHandleCompat.myUserHandle(), time);
            this.data = data;
            this.mContext = context;
            this.appWidgetId = data.getIntExtra(Favorites.APPWIDGET_ID, 0);
        }

        public String encodeToString() {
            try {
                return new JSONStringer().object().key("type").value(4).key("time").value(this.requestTime).key(UninstallWidgetReceiver.APPWIDGET_ID_KEY).value((long) this.appWidgetId).endObject().toString();
            } catch (JSONException e) {
                Log.d(UninstallWidgetReceiver.TAG, "Exception when adding uninstall widget: " + e);
                return null;
            }
        }

        public void runRequestInfo(Context context) {
            LauncherAppState.getInstance().getModel().getHomeLoader().removeWorkspaceItem(true, this.appWidgetId, null, null, false);
        }

        public String getTargetPackage() {
            return null;
        }

        boolean getContainPackage(ArrayList<String> packageNames) {
            return packageNames.contains(getTargetPackage());
        }
    }

    public void onReceive(final Context context, final Intent data) {
        if (data != null && ACTION_UNBIND_WIDGET.equals(data.getAction())) {
            final LauncherAppState app = LauncherAppState.getInstance();
            app.getModel();
            LauncherModel.runOnWorkerThread(new Runnable() {
                public void run() {
                    PendingUninstallWidgetInfo info = new PendingUninstallWidgetInfo(data, context, -1);
                    if (info.appWidgetId == 0) {
                        Log.d(UninstallWidgetReceiver.TAG, "appWidgetId is invalid");
                    } else {
                        ExternalRequestQueue.queueExternalRequestInfo(info, context, app);
                    }
                }
            });
        }
    }

    static PendingUninstallWidgetInfo decode(String encoded, Context context) {
        try {
            JSONObject object = (JSONObject) new JSONTokener(encoded).nextValue();
            Intent data = new Intent();
            data.putExtra(Favorites.APPWIDGET_ID, object.getInt(APPWIDGET_ID_KEY));
            return new PendingUninstallWidgetInfo(data, context, object.getLong("time"));
        } catch (JSONException e) {
            Log.d(TAG, "Exception reading widget to remove: " + e);
            return null;
        }
    }
}

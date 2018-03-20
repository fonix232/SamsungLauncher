package com.android.launcher3.common.bnr.smartswitch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.launcher3.util.PermissionUtils;
import java.util.ArrayList;

public class SmartSwitchBnr extends BroadcastReceiver {
    public static final String REQUEST_BACKUP_HOMESCREEN = "com.sec.android.intent.action.REQUEST_BACKUP_HOMELAYOUT";
    public static final String REQUEST_RESTORE_HOMESCREEN = "com.sec.android.intent.action.REQUEST_RESTORE_HOMELAYOUT";
    public static final String RESPONSE_BACKUP_HOMESCREEN = "com.sec.android.intent.action.RESPONSE_BACKUP_HOMELAYOUT";
    public static final String RESPONSE_RESTORE_HOMESCREEN = "com.sec.android.intent.action.RESPONSE_RESTORE_HOMELAYOUT";
    private static final String TAG = "Launcher.SmartSwitchBnr";

    public void onReceive(Context context, Intent data) {
        if (context == null || data == null) {
            Log.e(TAG, "onReceive context or intent is null");
            return;
        }
        String action = data.getAction();
        if (action != null) {
            int requestCode;
            ArrayList<String> needPermissionsList = new ArrayList();
            if (data.getIntExtra("ACTION", 0) != 0) {
                Log.d(TAG, "onReceive reqAction is not 0");
            }
            if (REQUEST_BACKUP_HOMESCREEN.equals(action)) {
                requestCode = 0;
            } else if (REQUEST_RESTORE_HOMESCREEN.equals(action)) {
                requestCode = 1;
            } else {
                return;
            }
            if (PermissionUtils.hasSelfPermission(context, PermissionUtils.getPermissions(requestCode), needPermissionsList) == 0) {
                Intent serviceIntent = new Intent(context, SmartSwitchBnrService.class);
                serviceIntent.setAction(data.getAction());
                serviceIntent.putExtras(data);
                context.startService(serviceIntent);
                return;
            }
            String source = data.getStringExtra("SOURCE");
            String sessionTime = data.getStringExtra("EXPORT_SESSION_TIME");
            Log.d(TAG, "onReceive there is no permission");
            if (REQUEST_BACKUP_HOMESCREEN.equals(action)) {
                Intent backupIntent = new Intent(RESPONSE_BACKUP_HOMESCREEN);
                backupIntent.putExtra("RESULT", 1);
                backupIntent.putExtra("ERR_CODE", 4);
                backupIntent.putExtra("REQ_SIZE", 0);
                backupIntent.putExtra("SOURCE", source);
                backupIntent.putExtra("EXPORT_SESSION_TIME", sessionTime);
                context.sendBroadcast(backupIntent);
                return;
            } else if (REQUEST_RESTORE_HOMESCREEN.equals(action)) {
                Intent restoreIntent = new Intent(RESPONSE_RESTORE_HOMESCREEN);
                restoreIntent.putExtra("RESULT", 1);
                restoreIntent.putExtra("ERR_CODE", 4);
                restoreIntent.putExtra("REQ_SIZE", 0);
                restoreIntent.putExtra("SOURCE", source);
                context.sendBroadcast(restoreIntent);
                return;
            } else {
                return;
            }
        }
        Log.e(TAG, "onReceive action value is null");
    }
}

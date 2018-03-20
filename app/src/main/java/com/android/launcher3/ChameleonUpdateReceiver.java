package com.android.launcher3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Process;
import android.util.Log;
import com.android.launcher3.common.customer.PostPositionSharedPref;

public class ChameleonUpdateReceiver extends BroadcastReceiver {
    private static final String ACTION_CHAMELEON_UPDATE_LAUNCHER = "com.samsung.intent.action.CSC_CHAMELEON_UPDATE_LAUNCHER";
    private static final String TAG = "ChameleonUpdate";

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (LauncherFeature.supportSprintExtension() && ACTION_CHAMELEON_UPDATE_LAUNCHER.equals(action)) {
            boolean deleteDB = intent.getBooleanExtra("delete_db", false);
            Log.d(TAG, "Received intent :: " + deleteDB);
            if (deleteDB) {
                resetDBForSprint(context);
            }
        }
    }

    private void resetDBForSprint(Context context) {
        if (context.deleteDatabase(LauncherFiles.LAUNCHER_DB)) {
            Log.d(TAG, "launcher db deleted successfully");
            if (context.deleteDatabase("postposition.db")) {
                Editor editor = context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, 0).edit();
                editor.clear();
                editor.commit();
                Editor editor2 = context.getSharedPreferences(PostPositionSharedPref.PREFERENCES, 0).edit();
                editor2.clear();
                editor2.commit();
                Log.d(TAG, "launcher/postposition db deleted successfully");
                Process.killProcess(Process.myPid());
                Intent home = new Intent(context, Launcher.class);
                home.setFlags(335544320);
                home.setAction("android.intent.action.MAIN");
                home.addFlags(2097152);
                home.addCategory("android.intent.category.HOME");
                context.startActivity(home);
                return;
            }
            Log.e(TAG, "fail to delete postposition.db");
            return;
        }
        Log.d(TAG, "Unable to delete launcher db");
    }
}

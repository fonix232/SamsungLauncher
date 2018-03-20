package com.android.launcher3.home;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.bnr.smartswitch.SmartSwitchBnr;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.model.LauncherSettings.Favorites_Easy;
import com.android.launcher3.common.model.LauncherSettings.Favorites_HomeApps;
import com.android.launcher3.common.model.LauncherSettings.Favorites_HomeOnly;
import com.android.launcher3.common.model.LauncherSettings.Favorites_Standard;
import java.util.Set;

public class RestoreContactShortcutReceiver extends BroadcastReceiver {
    private static final String ACTION_RESTORE_CONTACT_SHORTCUT = "com.samsung.android.launcher.action.RESTORE_CONTACT_SHORTCUT";
    private static final String TAG = "ContactShortcutRestore";

    public void onReceive(final Context context, final Intent data) {
        if (ACTION_RESTORE_CONTACT_SHORTCUT.equals(data.getAction())) {
            LauncherAppState.getInstance().getModel();
            LauncherModel.runOnWorkerThread(new Runnable() {
                public void run() {
                    if (RestoreContactShortcutReceiver.this.updateContactShortcut(context, data)) {
                        Log.e(RestoreContactShortcutReceiver.TAG, "updateContactShortcut true");
                    }
                }
            });
        }
    }

    private boolean updateContactShortcut(Context context, Intent data) {
        String restored = data.getStringExtra("SEC_CONTACT_SHORTCUT_RESTORED");
        if (restored == null) {
            return false;
        }
        Intent intent = (Intent) data.getParcelableExtra("android.intent.extra.shortcut.INTENT");
        String label = data.getStringExtra("android.intent.extra.shortcut.NAME");
        String[] splits = restored.split(",", 2);
        Log.d(TAG, "updateContactShortcut, restored : " + restored);
        if (!"RESTORED".equals(splits[0])) {
            return false;
        }
        try {
            int restoreId = Integer.parseInt(splits[1]);
            if ("com.android.contacts".equals(intent.getData().getAuthority())) {
                boolean result = false;
                ContentValues values = new ContentValues();
                values.put("intent", intent.toUri(0));
                Uri uri = Favorites.CONTENT_URI;
                String selection = "_id=? AND intent like ?";
                String[] selectionArgs = new String[]{Integer.toString(restoreId), "%com.android.contacts%"};
                Log.d(TAG, "id : " + restoreId + " label : " + label);
                if (context.getContentResolver().update(uri, values, selection, selectionArgs) > 0) {
                    Log.e(TAG, "updateContactShortcut restoreId : " + restoreId);
                    LauncherAppState app = LauncherAppState.getInstance();
                    long j = (long) restoreId;
                    app.getModel().getHomeLoader().updateContactShortcutInfo(j, new Intent(intent));
                    result = true;
                }
                if (LauncherFeature.supportEasyModeChange() && !result) {
                    Log.d(TAG, "updateContactShortcut check easy mode");
                    if (LauncherAppState.getInstance().isEasyModeEnabled()) {
                        uri = Favorites_Standard.CONTENT_URI;
                    } else {
                        uri = Favorites_Easy.CONTENT_URI;
                    }
                    if (context.getContentResolver().update(uri, values, selection, selectionArgs) > 0) {
                        Log.e(TAG, "updateContactShortcut restoreId : " + restoreId);
                        result = true;
                    }
                }
                if (LauncherFeature.supportHomeModeChange() && !result) {
                    Log.d(TAG, "updateContactShortcut check home only");
                    if (context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).getBoolean(LauncherAppState.HOME_ONLY_MODE, false)) {
                        uri = Favorites_HomeApps.CONTENT_URI;
                    } else {
                        uri = Favorites_HomeOnly.CONTENT_URI;
                    }
                    if (context.getContentResolver().update(uri, values, selection, selectionArgs) > 0) {
                        Log.e(TAG, "updateContactShortcut restoreId : " + restoreId);
                        result = true;
                    }
                }
                SharedPreferences prefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
                Set<String> contactShortcuts = prefs.getStringSet(Utilities.CONTACT_SHORTCUT_IDS, null);
                if (contactShortcuts != null && contactShortcuts.size() > 0) {
                    contactShortcuts.remove(Integer.toString(restoreId));
                    if (contactShortcuts.isEmpty()) {
                        Editor editor = prefs.edit();
                        int restore_result = prefs.getInt(Utilities.SMARTSWITCH_RESTORE_RESULT, 0);
                        int restore_errCode = prefs.getInt(Utilities.SMARTSWITCH_RESTORE_ERROR_CODE, 0);
                        int restore_file_length = prefs.getInt(Utilities.SMARTSWITCH_SAVE_FILE_LENGTH, 0);
                        String restore_source = prefs.getString(Utilities.SMARTSWITCH_RESTORE_SOURCE, "");
                        Log.d(TAG, "All contact shortcuts have been restored. send restore complete broadcast " + restore_result);
                        editor.remove(Utilities.CONTACT_SHORTCUT_IDS);
                        editor.remove(Utilities.SMARTSWITCH_RESTORE_RESULT);
                        editor.remove(Utilities.SMARTSWITCH_RESTORE_ERROR_CODE);
                        editor.remove(Utilities.SMARTSWITCH_SAVE_FILE_LENGTH);
                        editor.remove(Utilities.SMARTSWITCH_RESTORE_SOURCE);
                        editor.apply();
                        Intent intent2 = new Intent(SmartSwitchBnr.RESPONSE_RESTORE_HOMESCREEN);
                        intent2.putExtra("RESULT", restore_result);
                        intent2.putExtra("ERR_CODE", restore_errCode);
                        intent2.putExtra("REQ_SIZE", restore_file_length);
                        intent2.putExtra("SOURCE", restore_source);
                        context.sendBroadcast(intent2);
                    } else {
                        Log.d(TAG, "remain contact shortcut that will restored : " + contactShortcuts.size());
                        prefs.edit().putStringSet(Utilities.CONTACT_SHORTCUT_IDS, contactShortcuts).apply();
                    }
                }
                if (!result) {
                    Log.d(TAG, "This contacts shortcut info is not match with DB");
                }
            } else {
                Log.e(TAG, "updateContactShortcut failed, not have AUTHORITY");
            }
            return true;
        } catch (NumberFormatException e) {
            return true;
        }
    }
}

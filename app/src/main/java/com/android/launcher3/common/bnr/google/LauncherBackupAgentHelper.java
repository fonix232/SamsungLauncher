package com.android.launcher3.common.bnr.google;

import android.annotation.SuppressLint;
import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupManager;
import android.content.Context;
import android.database.Cursor;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import java.io.IOException;

@SuppressLint({"LongLogTag"})
public class LauncherBackupAgentHelper extends BackupAgentHelper {
    static final boolean DEBUG = false;
    private static final String LAUNCHER_DATA_PREFIX = "L";
    private static final String TAG = "LauncherBackupAgentHelper";
    static final boolean VERBOSE = false;
    private static BackupManager sBackupManager;
    private LauncherBackupHelper mHelper;

    public static void dataChanged(Context context) {
        if (sBackupManager == null) {
            sBackupManager = new BackupManager(context);
        }
        sBackupManager.dataChanged();
    }

    public void onCreate() {
        super.onCreate();
        this.mHelper = new LauncherBackupHelper(this);
        addHelper(LAUNCHER_DATA_PREFIX, this.mHelper);
    }

    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
        LauncherAppState.getLauncherProvider().createEmptyDB();
        if (FavoritesProvider.getInstance() != null) {
            FavoritesProvider.getInstance().removeChangedComponentPref();
        }
        boolean hasData = false;
        try {
            super.onRestore(data, appVersionCode, newState);
            Cursor cursor = getContentResolver().query(Favorites.CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                hasData = cursor.moveToNext();
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Restore failed", e);
            hasData = false;
        }
        if (hasData && this.mHelper.restoreSuccessful) {
            LauncherAppState.getLauncherProvider().clearFlagEmptyDbCreated();
            FavoritesProvider.getInstance().convertShortcutsToLauncherActivities();
            if (this.mHelper.switchDb) {
                Log.i(TAG, "Switch DB after restore");
                FavoritesProvider.getInstance().switchTable(1, true);
                return;
            }
            return;
        }
        LauncherAppState.getInstance().writeHomeOnlyModeEnabled(false);
        LauncherAppState.getLauncherProvider().createEmptyDB();
    }
}

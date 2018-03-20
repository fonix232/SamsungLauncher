package com.samsung.android.scloud.oem.lib.bnr;

import android.content.Context;
import android.content.SharedPreferences;
import com.samsung.android.scloud.oem.lib.LOG;

public class BackupMetaManager {
    private static final String TAG = "BackupMetaManager_";
    private static BackupMetaManager mMetaManager = null;
    private SharedPreferences mBackupMeta = null;

    private static final class META {
        private static final String NAME = "BackupMeta";

        private static final class KEY {
            private static final String FIRST_BACKUP = "FIRST_BACKUP";
            private static final String LAST_BACKUP_TIME = "LAST_BACKUP_TIME";

            private KEY() {
            }
        }

        private META() {
        }
    }

    public static synchronized BackupMetaManager getInstance(Context context) {
        BackupMetaManager backupMetaManager;
        synchronized (BackupMetaManager.class) {
            if (mMetaManager == null) {
                mMetaManager = new BackupMetaManager(context);
            }
            backupMetaManager = mMetaManager;
        }
        return backupMetaManager;
    }

    private BackupMetaManager(Context context) {
        this.mBackupMeta = context.getSharedPreferences("BackupMeta", 0);
    }

    public void setFirstBackup(String sourceKey, boolean isFirstBackup) {
        LOG.f(TAG + sourceKey, "setFirstBackup(): " + isFirstBackup);
        this.mBackupMeta.edit().putBoolean(sourceKey + "_" + "FIRST_BACKUP", isFirstBackup).commit();
    }

    public boolean isFirstBackup(String sourceKey) {
        boolean result = this.mBackupMeta.getBoolean(sourceKey + "_" + "FIRST_BACKUP", true);
        LOG.i(TAG + sourceKey, "setFirstBackup(): " + result);
        return result;
    }

    public void setLastBackupTime(String sourceKey, long time) {
        LOG.f(TAG + sourceKey, "setLastBackupTime(): " + time);
        this.mBackupMeta.edit().putLong(sourceKey + "_" + "LAST_BACKUP_TIME", time).commit();
    }

    public boolean clear(String sourceKey) {
        LOG.f(TAG + sourceKey, "BackupMetaManager cleared!!!");
        return this.mBackupMeta.edit().clear().commit();
    }
}

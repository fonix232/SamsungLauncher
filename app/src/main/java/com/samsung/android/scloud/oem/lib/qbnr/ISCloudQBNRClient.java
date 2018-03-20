package com.samsung.android.scloud.oem.lib.qbnr;

import android.content.Context;
import android.os.ParcelFileDescriptor;

public interface ISCloudQBNRClient {

    public interface QuickBackupListener {
        void complete(boolean z);

        void onProgress(long j, long j2);
    }

    void backup(Context context, ParcelFileDescriptor parcelFileDescriptor, QuickBackupListener quickBackupListener);

    String getDescription(Context context);

    String getLabel(Context context);

    boolean isEnableBackup(Context context);

    boolean isSupportBackup(Context context);

    void restore(Context context, ParcelFileDescriptor parcelFileDescriptor, QuickBackupListener quickBackupListener);
}

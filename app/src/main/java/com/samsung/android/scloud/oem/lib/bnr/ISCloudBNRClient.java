package com.samsung.android.scloud.oem.lib.bnr;

import android.content.Context;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.HashMap;

public interface ISCloudBNRClient {
    boolean backupComplete(Context context, boolean z);

    ArrayList<BNRItem> backupItem(Context context, ArrayList<String> arrayList);

    boolean backupPrepare(Context context);

    boolean clearRestoreData(Context context, String[] strArr);

    String getDescription(Context context);

    ArrayList<BNRFile> getFileMeta(Context context, int i, int i2);

    String getFilePath(Context context, String str, boolean z, String str2);

    HashMap<String, Long> getItemKey(Context context, int i, int i2);

    String getLabel(Context context);

    boolean isEnableBackup(Context context);

    boolean isSupportBackup(Context context);

    boolean restoreComplete(Context context, String[] strArr);

    boolean restoreItem(Context context, ArrayList<BNRItem> arrayList, ArrayList<String> arrayList2);

    boolean restorePrepare(Context context, Bundle bundle);
}

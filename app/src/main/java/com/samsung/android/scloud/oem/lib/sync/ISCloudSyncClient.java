package com.samsung.android.scloud.oem.lib.sync;

import android.accounts.Account;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ISCloudSyncClient {
    public static final String FAIL_CORRUPTED_FILE = "FAIL_CORRUPTED_FILE";

    void accountSignedIn(Context context, Account account);

    void accountSignedOut(Context context, String str);

    boolean complete(Context context, SyncItem syncItem, int i);

    boolean deleteLocal(Context context, String str);

    Map<String, Long> getAttachmentFileInfo(Context context, int i, String str);

    String getLocalChange(Context context, int i, String str, String[] strArr, HashMap<String, ParcelFileDescriptor> hashMap);

    boolean isSyncable(Context context);

    List<SyncItem> prepareToSync(Context context, String[] strArr, long[] jArr, String[] strArr2, String str, String str2);

    String updateLocal(Context context, int i, SyncItem syncItem, String str, HashMap<String, ParcelFileDescriptor> hashMap, String[] strArr);
}

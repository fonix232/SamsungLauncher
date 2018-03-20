package com.android.launcher3.common.customer;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import java.util.ArrayList;
import java.util.Map;

public abstract class PostPositionerBase {
    private static final String TAG = "PostPositionerBase";
    protected LauncherAppState mAppState = null;
    protected Context mContext;
    protected Handler mModelWorker = null;
    protected PostPositionSharedPref mPrefInfo;
    protected PostPositionProvider mProvider;

    public abstract boolean addItem(PostPositionItemRecord postPositionItemRecord, LauncherActivityInfoCompat launcherActivityInfoCompat, UserHandleCompat userHandleCompat);

    protected abstract boolean hasItem(long j, boolean z);

    protected abstract void init();

    protected abstract void setup();

    public PostPositionerBase(Context context, PostPositionProvider provider) {
        this.mContext = context;
        this.mProvider = provider;
        if (this.mPrefInfo == null) {
            this.mPrefInfo = new PostPositionSharedPref(this.mContext);
        }
        if (this.mModelWorker == null) {
            this.mModelWorker = new Handler(LauncherModel.getWorkerLooper());
        }
        setup();
    }

    public void checkFolderValidation() {
        Map<Long, String> idList = this.mPrefInfo.getFolderIdList();
        for (Long id : idList.keySet()) {
            String key = (String) idList.get(id);
            if ((key.endsWith(this.mPrefInfo.getContainerKey(null, PostPositionSharedPref.PREFS_FOLDER_ID)) || key.endsWith(this.mPrefInfo.getContainerKey(null, PostPositionSharedPref.PREFS_PRELOADED_FOLDER_ID))) && !hasItem(id.longValue(), true)) {
                Log.e(TAG, id + " folder is not exist. so remove this from shared pref.");
                this.mPrefInfo.removeKey(key);
            } else if (key.endsWith(this.mPrefInfo.getContainerKey(null, PostPositionSharedPref.PREFS_FOLDER_READY_ID)) && !hasItem(id.longValue(), false)) {
                Log.e(TAG, id + " folder ready item is not exist. so remove this from shared pref.");
                this.mPrefInfo.removeKey(key);
            }
        }
    }

    public void deleteFolder(long folderId, boolean isReload) {
        if (isReload) {
            ArrayList<Long> ids = new ArrayList();
            ids.add(Long.valueOf(folderId));
            this.mPrefInfo.removeItemsInfo(ids);
            return;
        }
        String foldername = this.mPrefInfo.getPreloadedFolderName(folderId);
        if (foldername == null) {
            foldername = this.mPrefInfo.getFolderNameById(folderId);
        }
        if (foldername == null || foldername.isEmpty()) {
            foldername = this.mPrefInfo.getFolderName(folderId);
            if (foldername == null) {
                foldername = this.mPrefInfo.getFolderNameById(folderId);
            }
            if (foldername != null && !foldername.isEmpty()) {
                if (isReload) {
                    this.mPrefInfo.removeFolderId(foldername, false);
                } else {
                    this.mPrefInfo.writeFolderId(foldername, PostPositionSharedPref.REMOVED, false);
                }
            }
        } else if (isReload) {
            this.mPrefInfo.removePreloadedFolderId(foldername);
        } else {
            this.mPrefInfo.writePreloadedFolderId(foldername, PostPositionSharedPref.REMOVED);
        }
    }

    public void resetItem(String cmpName) {
        this.mProvider.resetItem(cmpName);
    }

    public PostPositionSharedPref getSharedPref() {
        return this.mPrefInfo;
    }
}

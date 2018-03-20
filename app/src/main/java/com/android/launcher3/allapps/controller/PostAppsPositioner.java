package com.android.launcher3.allapps.controller;

import android.content.Context;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.allapps.model.AppsModel;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.customer.PostPositionItemRecord;
import com.android.launcher3.common.customer.PostPositionProvider;
import com.android.launcher3.common.customer.PostPositionSharedPref;
import com.android.launcher3.common.customer.PostPositionerBase;
import com.android.launcher3.common.model.LauncherSettings.Favorites;

public class PostAppsPositioner extends PostPositionerBase {
    private static final String TAG = "PostAppsPositioner";
    private AppsModel mAppsModel;

    public PostAppsPositioner(Context context, PostPositionProvider provider) {
        super(context, provider);
    }

    protected void init() {
        if (this.mAppState == null) {
            this.mAppState = LauncherAppState.getInstance();
        }
        if (this.mAppsModel == null) {
            this.mAppsModel = this.mAppState.getModel().getAppsModel();
        }
    }

    protected void setup() {
        this.mPrefInfo.setContainer(Favorites.CONTAINER_APPS);
        init();
    }

    public boolean addItem(PostPositionItemRecord itemRecord, LauncherActivityInfoCompat info, UserHandleCompat user) {
        if (!itemRecord.isAppsAdd()) {
            return false;
        }
        Log.i(TAG, "addAppsItem() : " + itemRecord.getComponentName());
        if (!addItem(info, user, itemRecord)) {
            this.mProvider.updateItemRecordResult(false, itemRecord.getComponentName());
            Log.e(TAG, "addItem() result failed.");
        }
        return true;
    }

    private synchronized boolean addItem(LauncherActivityInfoCompat info, UserHandleCompat user, PostPositionItemRecord itemRecord) {
        boolean z;
        if (itemRecord.getAppsFolderName() == null || itemRecord.getAppsFolderName().equals("")) {
            Log.d(TAG, "folder is not created and folderName from PrefInfo is null");
            z = true;
        } else {
            ItemInfo addedItem = this.mAppsModel.getItemInfoInAppsForComponentName(info.getComponentName(), user, true);
            if (addedItem == null || addedItem.container == -102) {
                long folderId;
                if (itemRecord.isAppsPreloadFolder()) {
                    folderId = this.mPrefInfo.getPreloadedFolderId(itemRecord.getAppsFolderName());
                    Log.d(TAG, "folder is preloaded folder. folderId is " + folderId);
                    if (folderId < 0) {
                        folderId = this.mPrefInfo.getFolderId(itemRecord.getAppsFolderName(), false);
                        Log.d(TAG, "request preloaded folder. but not created by xml so find other folder type : " + folderId);
                    }
                } else {
                    folderId = this.mPrefInfo.getFolderId(itemRecord.getAppsFolderName(), false);
                    Log.d(TAG, "folder is not preloaded folder. folderId is " + folderId);
                }
                if (folderId == PostPositionSharedPref.REMOVED) {
                    Log.d(TAG, itemRecord.getAppsFolderName() + "folder already removed by user.");
                    z = false;
                } else if (folderId > 0) {
                    Log.d(TAG, "folder exist. folderId is " + folderId + " add to folder");
                    if (!this.mAppsModel.addItemToFolder(info, user, folderId)) {
                        Log.e(TAG, "fail to add item to folder  : " + info.getComponentName().flattenToShortString());
                        z = false;
                    }
                    z = true;
                } else {
                    if (itemRecord.getAppsFolderName() != null) {
                        folderId = this.mPrefInfo.getFolderId(itemRecord.getAppsFolderName(), true);
                        ItemInfo appItem = this.mAppsModel.getItemById(folderId);
                        Log.d(TAG, "folder is not created so need to make a folder by folderId : " + folderId + " , " + appItem);
                        if (folderId > 0 && appItem != null && appItem.container == -102) {
                            Log.d(TAG, "create folder from ready id");
                            if (itemRecord.getComponentName().equals(appItem.componentName.flattenToShortString())) {
                                Log.d(TAG, "already write as folder ready id");
                                z = true;
                            } else {
                                long resultFolderId = this.mAppsModel.createFolderAndAddItem(appItem, itemRecord.getAppsFolderName(), info);
                                if (resultFolderId != -1) {
                                    this.mPrefInfo.removeFolderId(itemRecord.getAppsFolderName(), true);
                                    this.mPrefInfo.writeFolderId(itemRecord.getAppsFolderName(), resultFolderId, false);
                                    z = true;
                                } else {
                                    Log.e(TAG, "createFolder Child item isn't exist : " + info.getComponentName().flattenToShortString());
                                    z = false;
                                }
                            }
                        } else if (addedItem != null) {
                            Log.d(TAG, "add item and save ready id: " + addedItem.id);
                            this.mPrefInfo.writeFolderId(itemRecord.getAppsFolderName(), addedItem.id, true);
                        } else {
                            z = false;
                        }
                    }
                    z = true;
                }
            } else {
                Log.d(TAG, "Aleady exist in other folder : " + info.getComponentName().flattenToShortString());
                z = true;
            }
        }
        return z;
    }

    protected boolean hasItem(long id, boolean isFolder) {
        if (isFolder) {
            if (this.mAppsModel.findFolderById(Long.valueOf(id)) != null) {
                return true;
            }
            return false;
        } else if (this.mAppsModel.getItemById(id) == null) {
            return false;
        } else {
            return true;
        }
    }
}

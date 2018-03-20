package com.android.launcher3.common.customer;

import android.content.ComponentName;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.customer.PostPositionProvider.ITEM_TYPE;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PostPositionController {
    public static final boolean SUPPORT_AUTO_FOLDERING = false;
    private static final String TAG = "PostPositionController";
    private static PostPositionController sInstance;
    private String mAppsAutoFolderName;
    private String mAppsInstaller;
    private Context mContext;
    private String mHomeAutoFolderName;
    private String mHomeInstaller;
    private boolean mIsEnabled = false;
    private boolean mIsNoFDRState = false;
    private ArrayList<PostPositionerBase> mPositionerList = new ArrayList(2);
    private PostPositionProvider mProvider;

    public PostPositionController(Context context) {
        this.mContext = context;
    }

    public static PostPositionController getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PostPositionController(context);
        }
        return sInstance;
    }

    public void checkAndEnablePositioner() {
        PostPositionSharedPref ppPref = getSharedPref(-100);
        if (ppPref != null) {
            this.mIsEnabled = ppPref.isEnabled() > 0;
            boolean autoFolderEnable = getAutoFolderingInfo(ppPref);
            if (!this.mIsEnabled && (autoFolderEnable || this.mProvider.hasItemRecord())) {
                ppPref.setEnabled(true);
                this.mIsEnabled = true;
            }
            Log.d(TAG, "checkAndEnableProvider() - " + this.mIsEnabled);
            return;
        }
        Log.d(TAG, "checkAndEnableProvider() - ppPref is null");
    }

    private boolean getAutoFolderingInfo(PostPositionSharedPref ppPref) {
        return false;
    }

    public PostPositionSharedPref getSharedPref(long container) {
        Iterator it = this.mPositionerList.iterator();
        while (it.hasNext()) {
            PostPositionerBase pp = (PostPositionerBase) it.next();
            if (pp != null && ((long) pp.getSharedPref().getContainer()) == container) {
                return pp.getSharedPref();
            }
        }
        return null;
    }

    public boolean isEnabled() {
        return this.mIsEnabled && this.mProvider != null;
    }

    public void setProvider(PostPositionProvider provider) {
        this.mProvider = provider;
    }

    public PostPositionProvider getProvider() {
        return this.mProvider;
    }

    public void registerPositioner(PostPositionerBase pp) {
        this.mPositionerList.add(pp);
    }

    private boolean isAvaliableState() {
        if (!this.mIsEnabled || LauncherAppState.getInstance().isEasyModeEnabled() || LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            return false;
        }
        return true;
    }

    public void addItem(PostPositionItemRecord itemRecord) {
        if (isAvaliableState()) {
            UserHandleCompat user = UserHandleCompat.myUserHandle();
            LauncherActivityInfoCompat info = null;
            ComponentName cn = ComponentName.unflattenFromString(itemRecord.getComponentName());
            if (cn != null) {
                if (itemRecord.getItemType() == ITEM_TYPE.APP) {
                    info = getActivityInfo(cn, user);
                    if (info == null) {
                        Log.i(TAG, "addHomeItem(): Can't find ActivityInfo. - " + itemRecord.getComponentName());
                        return;
                    }
                } else if (itemRecord.getItemType() == ITEM_TYPE.WIDGET) {
                    try {
                        if (Utilities.isComponentActive(this.mContext, cn, this.mContext.getPackageManager().getPackageInfo(cn.getPackageName(), 2).receivers)) {
                            Log.d(TAG, "Widget exist and will be added soon");
                        } else {
                            Log.i(TAG, "Can't find widget component info : " + cn.flattenToShortString());
                            return;
                        }
                    } catch (Exception e) {
                        Log.i(TAG, "Can't find widget component info : " + cn.flattenToShortString());
                        return;
                    }
                }
                this.mProvider.updateItemRecordResult(true, itemRecord.getComponentName());
                Iterator it = this.mPositionerList.iterator();
                while (it.hasNext()) {
                    PostPositionerBase pp = (PostPositionerBase) it.next();
                    if (pp != null) {
                        pp.addItem(itemRecord, info, user);
                    }
                }
                return;
            }
            Log.e(TAG, "addHomeItem(): Wrong Component expression. - " + itemRecord.getComponentName());
            return;
        }
        Log.i(TAG, "Not isAvaliableState!");
    }

    public void addItem(String packageName) {
        addItem(packageName, false);
    }

    public void addItem(String packageName, boolean isWidgetOnly) {
        if (isAvaliableState()) {
            Log.i(TAG, "addItem() - " + packageName + ", isWidgetOnly : " + isWidgetOnly);
            boolean autoFoldering = false;
            if (!(isWidgetOnly || TextUtils.isEmpty(packageName) || (this.mHomeInstaller == null && this.mAppsInstaller == null))) {
                for (LauncherActivityInfoCompat info : LauncherAppsCompat.getInstance(this.mContext).getActivityList(packageName, UserHandleCompat.myUserHandle())) {
                    PostPositionItemRecord record = createItemRecordForAutoFoldering(info.getComponentName());
                    if (record != null) {
                        addItem(record);
                        autoFoldering = true;
                    }
                }
            }
            if (!autoFoldering) {
                PostPositionItemRecord[] items = this.mProvider.getItemRecordsNeedToPosition(packageName);
                if (items != null && items.length > 0) {
                    for (PostPositionItemRecord i : items) {
                        if (!isWidgetOnly || i.getItemType() == ITEM_TYPE.WIDGET) {
                            addItem(i);
                        }
                    }
                    return;
                }
                return;
            }
            return;
        }
        Log.i(TAG, "Not isAvaliableState!");
    }

    public void addAllItems() {
        if (isAvaliableState()) {
            Log.i(TAG, "addAllItems() - All");
            Iterator it = this.mPositionerList.iterator();
            while (it.hasNext()) {
                ((PostPositionerBase) it.next()).checkFolderValidation();
            }
            addItem("");
            return;
        }
        Log.i(TAG, "Not isAvaliableState!");
    }

    private LauncherActivityInfoCompat getActivityInfo(ComponentName cmpName, UserHandleCompat user) {
        List<LauncherActivityInfoCompat> apps = LauncherAppsCompat.getInstance(this.mContext).getActivityList(cmpName.getPackageName(), user);
        if (apps != null) {
            for (LauncherActivityInfoCompat i : apps) {
                if (cmpName.equals(i.getComponentName())) {
                    return i;
                }
            }
        }
        return null;
    }

    private PostPositionItemRecord createItemRecordForAutoFoldering(ComponentName componentName) {
        PostPositionItemRecord postPositionItemRecord = null;
        String installer = this.mContext.getPackageManager().getInstallerPackageName(componentName.getPackageName());
        if (this.mHomeInstaller != null && this.mHomeInstaller.equals(installer)) {
            postPositionItemRecord = new PostPositionItemRecord(componentName.getPackageName() + "/" + componentName.getClassName(), ITEM_TYPE.APP.ordinal());
            postPositionItemRecord.setHomeAdded(true);
            postPositionItemRecord.setHomePreloadFolder(true);
            postPositionItemRecord.setHomeFolderName(this.mHomeAutoFolderName);
        }
        if (this.mAppsInstaller != null && this.mAppsInstaller.equals(installer)) {
            if (postPositionItemRecord == null) {
                postPositionItemRecord = new PostPositionItemRecord(componentName.getPackageName() + "/" + componentName.getClassName(), ITEM_TYPE.APP.ordinal());
            }
            postPositionItemRecord.setAppsAdded(true);
            postPositionItemRecord.setAppsPreloadFolder(true);
            postPositionItemRecord.setAppsFolderName(this.mAppsAutoFolderName);
        }
        return postPositionItemRecord;
    }

    public long getAppsAutoFolderId(String pkgName) {
        return -1;
    }

    public void removeAutoFolderInfo(long folderId) {
        PostPositionSharedPref ppPref = getSharedPref(-102);
        if (ppPref != null && folderId == ppPref.getPreloadedFolderId(this.mAppsAutoFolderName)) {
            ppPref.writeAutoFolderingInfo("", "");
            this.mAppsInstaller = null;
        }
    }

    public void deleteFolder(long item) {
        Log.d(TAG, "deleteFolder() - " + item);
        Iterator it = this.mPositionerList.iterator();
        while (it.hasNext()) {
            PostPositionerBase pp = (PostPositionerBase) it.next();
            if (pp != null) {
                pp.deleteFolder(item, this.mIsNoFDRState);
            }
        }
    }

    public void deleteItems(ArrayList<Long> ids) {
        PostPositionSharedPref ppPref = getSharedPref(-102);
        if (ppPref != null) {
            ppPref.removeItemsInfo(ids);
        }
    }

    public void writeFolderReadyIdForNoFDR(long container, String folderName, long item_id) {
        if (this.mIsNoFDRState && folderName != null && !folderName.isEmpty()) {
            PostPositionSharedPref ppPref = getSharedPref(container);
            if (ppPref != null) {
                ppPref.removePreloadedFolderId(folderName);
                ppPref.removeFolderId(folderName, false);
                ppPref.writeFolderId(folderName, item_id, true);
            }
        }
    }

    public boolean isReloadNeeded() {
        PostPositionSharedPref ppPref = getSharedPref(-100);
        if (ppPref == null || ppPref.getOMCPath().equals(LauncherFeature.getOmcPath())) {
            return false;
        }
        Log.d(TAG, "isReloadNeeded() - true");
        this.mIsNoFDRState = true;
        this.mProvider.delete(PostPositionProvider.CONTENT_URI, null, null);
        ppPref.writeOMCPath();
        ppPref.clearRemovedFolderInfo();
        return true;
    }

    public void onFinishLoaderTask() {
        this.mIsNoFDRState = false;
    }
}

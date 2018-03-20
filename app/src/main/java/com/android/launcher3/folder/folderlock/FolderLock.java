package com.android.launcher3.folder.folderlock;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build.VERSION;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherModel.OnLauncherBindingItemsCompletedListener;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.controller.ControllerBase;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.drag.DragView;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.folder.view.FolderView;
import com.android.launcher3.util.logging.GSIMLogging;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class FolderLock implements OnLauncherBindingItemsCompletedListener {
    public static final String APPLOCK_ENABLE_CHANGED = "com.samsung.applock.intent.action.APPLOCK_ENABLE_CHANGED";
    private static final String APPLOCK_PACKAGENAME = "com.samsung.android.applock";
    private static final String EMPTY_STRING = "";
    private static final String KEY_LASTEST_LOCKED_PACKAGES = "last_locked_package";
    private static final String KNOX_SHORTCUT_PACKAGE = "com.samsung.knox.rcp.components";
    public static final String LAUNCHER_REQUEST = "LAUNCHER_REQUEST";
    private static final String LOCKED_APP_FOLDERS = "smartmanager_locked_apps_folders";
    private static final String LOCKED_PACKAGE = "smartmanager_locked_apps_packages";
    public static final String LOCKED_PACKAGE_ICON = "LOCKED_PACKAGE_ICON";
    public static final String LOCKED_PACKAGE_LABEL = "LOCKED_PACKAGE_LABEL";
    private static final String LOCKED_PACKAGE_N = "applock_locked_apps_packages";
    public static final String LOCKED_PACKAGE_NAME = "LOCKED_PACKAGE_NAME";
    public static final String LOCK_CONTAINER_HOME = "home_locked_items";
    public static final String LOCK_CONTAINER_HOME_ONLY = "home_only_locked_items";
    public static final String LOCK_CONTAINER_MENU = "menu_locked_items";
    private static final String NOTIFY_APPLOCK_UPDATE_ACTION = "com.samsung.applock.intent.action.NOTIFYUPDATE";
    public static final String PERMISSION_APPLOCK_STATE_CHANGED = "com.samsung.applock.permission.STATUSCHANGED";
    public static final int REQUEST_CODE_FOLDER_LOCK = 122;
    public static final int REQUEST_CODE_FOLDER_UNLOCK = 123;
    public static final int REQUEST_CODE_OPEN_LOCKEDFOLDER = 124;
    public static final String REQUEST_LOCK = "request_lock";
    private static final String REQUEST_LOCK_OR_UNLOCK = "lock_or_unlock";
    public static final String REQUEST_UNLOCK = "request_unlock";
    public static final String REQUEST_VERIFY_FROM = "REQUEST_VERIFY_FROM";
    private static final String SEPARATOR_CLS_FOLDERID = "-";
    private static final String SEPARATOR_FOLDERID = ";";
    private static final String SEPARATOR_PKG_CLS = ",";
    private static final String SEPARATOR_STRING = ",";
    public static final String TAG = "Launcher.FolderLock";
    private static final String UNLOCK_APP_ACTION = "com.sec.android.launcher.intent.action.FOLDERLOCK_CHANGED";
    public static final String UNLOCK_CONTAINER_ADDITION = "drag";
    public static final String UNLOCK_CONTAINER_HOME = "home_tempunlocked_items";
    public static final String UNLOCK_CONTAINER_HOME_ONLY = "homeonly_tempunlocked_items";
    public static final String UNLOCK_CONTAINER_MENU = "menu_tempunlocked_items";
    private boolean isFolderLockEnable;
    private Context mAppContext;
    private BackupDragDropInfo mBackupInfo;
    private List<FolderLockActionCallback> mFolderLockActionCallbacks;
    private boolean mIsRecoverMode;
    private Launcher mLauncher;
    private List<FolderInfo> mLockedFolders;
    private LockedItemRecords mLockedItemRecords;
    private FolderLockReceiver mReceiver;
    private CellLayout mReorderLayout;
    private ApplockStateChangedRecevier mStateReceiver;
    private List<String> mWhiteList;

    private class ApplockStateChangedRecevier extends BroadcastReceiver {
        private ApplockStateChangedRecevier() {
        }

        public void onReceive(Context context, Intent intent) {
            if (FolderLock.APPLOCK_ENABLE_CHANGED.equals(intent.getAction())) {
                boolean value = intent.getBooleanExtra("android.intent.extra.SUBJECT", false);
                Log.d(FolderLock.TAG, "applock enabled value = " + value);
                if (value != FolderLock.this.isFolderLockEnable) {
                    FolderLock.this.isFolderLockEnable = value;
                    Log.d(FolderLock.TAG, "isFolderLockEnable " + FolderLock.this.isFolderLockEnable);
                    for (FolderInfo folder : FolderLock.this.mLockedFolders) {
                        folder.setLockedFolderOpenedOnce(false);
                    }
                }
                FolderLock.this.applyFolderNameChanged();
            }
        }
    }

    private static class BackupDragDropInfo {
        private FolderIconView mClickedFolder = null;
        private IconInfo mCopyInfo = null;
        private ItemInfo mDragInfo = null;
        private DragObject mDragObject = null;
        private DragSource mDragSource = null;
        private DragView mDragView = null;
        private boolean mIsDragInFolder = false;
        private ItemInfo mTargetInfo = null;
        private View mTargetView = null;

        BackupDragDropInfo(DragObject object, View target) {
            this.mDragObject = object;
            this.mDragInfo = (ItemInfo) object.dragInfo;
            this.mDragView = object.dragView;
            this.mDragSource = object.dragSource;
            if (this.mDragSource instanceof FolderView) {
                this.mIsDragInFolder = true;
            }
            if (target instanceof FolderIconView) {
                IconInfo icon = (IconInfo) object.dragInfo;
                this.mCopyInfo = icon.makeCloneInfo();
                this.mCopyInfo.container = icon.container;
                this.mCopyInfo.id = icon.id;
            }
            this.mTargetView = target;
            if (target != null && (target.getTag() instanceof ItemInfo)) {
                this.mTargetInfo = (ItemInfo) target.getTag();
            }
        }

        BackupDragDropInfo(ItemInfo info) {
            this.mTargetInfo = info;
        }

        BackupDragDropInfo(FolderIconView view) {
            this.mClickedFolder = view;
        }

        public ItemInfo getDragInfo() {
            return this.mDragInfo;
        }

        public DragObject getDragObject() {
            return this.mDragObject;
        }

        public IconInfo getCopyInfo() {
            return this.mCopyInfo;
        }

        public DragSource getDragparent() {
            return this.mDragSource;
        }

        public FolderView getDragParetFolder() {
            return (FolderView) this.mDragSource;
        }

        public boolean isDragInFolder() {
            return this.mIsDragInFolder;
        }

        public View getTargetView() {
            return this.mTargetView;
        }

        public View getDragView() {
            return this.mDragView;
        }

        public ItemInfo getTargetInfo() {
            return this.mTargetInfo;
        }

        public FolderIconView getClickedFolder() {
            return this.mClickedFolder;
        }
    }

    public interface FolderLockActionCallback {
        View getFolderIconView(FolderInfo folderInfo);

        void moveOutItemsFromLockedFolder(FolderInfo folderInfo, ArrayList<IconInfo> arrayList, ArrayList<IconInfo> arrayList2);
    }

    private class FolderLockReceiver extends BroadcastReceiver {
        private String action;

        private FolderLockReceiver() {
            this.action = null;
        }

        public void onReceive(Context context, Intent intent) {
            this.action = intent.getAction();
            Log.i(FolderLock.TAG, this.action + " size " + FolderLock.this.mLockedFolders.size());
            String str = this.action;
            boolean z = true;
            switch (str.hashCode()) {
                case -2128145023:
                    if (str.equals("android.intent.action.SCREEN_OFF")) {
                        z = false;
                        break;
                    }
                    break;
                case 1907631001:
                    if (str.equals(FolderLock.UNLOCK_APP_ACTION)) {
                        z = true;
                        break;
                    }
                    break;
            }
            switch (z) {
                case false:
                    if (FolderLock.this.isFolderLockEnabled()) {
                        FolderLock.this.clearTempUnlockedFolder();
                        for (FolderInfo folder : FolderLock.this.mLockedFolders) {
                            folder.setLockedFolderOpenedOnce(false);
                        }
                        if ((FolderLock.this.mLauncher.getStageManager().getTopStage().getContainerView() instanceof FolderView) && ((FolderView) FolderLock.this.mLauncher.getStageManager().getTopStage().getContainerView()).getInfo().isLocked()) {
                            FolderLock.this.mLauncher.closeFolderStage();
                            return;
                        }
                        return;
                    }
                    return;
                case true:
                    FolderLock.this.putOutUnlockedItemFromLockedFolder(intent.getStringExtra("android.intent.extra.SUBJECT"));
                    return;
                default:
                    return;
            }
        }
    }

    private static class SingletonHolder {
        private static final FolderLock sFolderLockInstance = new FolderLock();

        private SingletonHolder() {
        }
    }

    public static FolderLock getInstance() {
        return SingletonHolder.sFolderLockInstance;
    }

    private FolderLock() {
        boolean z = false;
        this.mLauncher = null;
        this.isFolderLockEnable = false;
        this.mLockedItemRecords = new LockedItemRecords();
        this.mBackupInfo = null;
        this.mIsRecoverMode = false;
        this.mAppContext = LauncherAppState.getInstance().getContext();
        if (isAppLockEnable() && LauncherFeature.supportFolderLock()) {
            z = true;
        }
        this.isFolderLockEnable = z;
        this.mWhiteList = new ArrayList();
        this.mFolderLockActionCallbacks = new ArrayList();
        this.mReceiver = new FolderLockReceiver();
        this.mStateReceiver = new ApplockStateChangedRecevier();
    }

    public void setup(Launcher launcher) {
        this.mLauncher = launcher;
        this.mLockedFolders = new ArrayList();
        registerListener();
        initWhiteList();
    }

    private void registerListener() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction(UNLOCK_APP_ACTION);
        this.mAppContext.registerReceiver(this.mReceiver, filter);
        IntentFilter stateFilter = new IntentFilter();
        stateFilter.addAction(APPLOCK_ENABLE_CHANGED);
        this.mAppContext.registerReceiver(this.mStateReceiver, stateFilter, PERMISSION_APPLOCK_STATE_CHANGED, null);
        LauncherAppState.getInstance().getModel().registerOnLauncherBindingItemsCompletedListener(this);
    }

    private ActivityManager getActivityManager(Context context) {
        return (ActivityManager) context.getSystemService("activity");
    }

    public boolean isFolderLockEnabled() {
        return this.isFolderLockEnable;
    }

    public void startLockVerifyActivity(ItemInfo info) {
        if (!needPopupConfirm(info)) {
            return;
        }
        if (info instanceof IconInfo) {
            startVerifyActivity((int) REQUEST_CODE_FOLDER_LOCK, (IconInfo) info, REQUEST_LOCK);
        } else if (info instanceof FolderInfo) {
            startVerifyActivity((int) REQUEST_CODE_FOLDER_LOCK, (FolderInfo) info, REQUEST_LOCK);
        }
    }

    public void startUnlockVerifyActivity(ItemInfo info) {
        if (!needPopupConfirm(info)) {
            return;
        }
        if (info instanceof IconInfo) {
            startVerifyActivity((int) REQUEST_CODE_FOLDER_UNLOCK, (IconInfo) info, REQUEST_UNLOCK);
        } else if (info instanceof FolderInfo) {
            startVerifyActivity((int) REQUEST_CODE_FOLDER_UNLOCK, (FolderInfo) info, REQUEST_UNLOCK);
        }
    }

    public void openLockedFolder(FolderInfo folder) {
        startVerifyActivity((int) REQUEST_CODE_OPEN_LOCKEDFOLDER, folder, REQUEST_UNLOCK);
    }

    public void addFolderLockActionCallback(FolderLockActionCallback callback) {
        if (this.mFolderLockActionCallbacks != null) {
            this.mFolderLockActionCallbacks.add(callback);
        }
    }

    public void removeFolderLockActionCallback(FolderLockActionCallback callback) {
        if (this.mFolderLockActionCallbacks != null) {
            this.mFolderLockActionCallbacks.remove(callback);
        }
    }

    public void setRecoverMode(boolean isOn) {
        this.mIsRecoverMode = isOn;
    }

    public boolean getRecoverMode() {
        return this.mIsRecoverMode;
    }

    public void setReorderLayout(CellLayout cellLayout) {
        this.mReorderLayout = cellLayout;
    }

    public CellLayout getReroderLayout() {
        return this.mReorderLayout;
    }

    public void setBackupInfo(DragObject object, View view) {
        this.mBackupInfo = null;
        if (object != null && view != null) {
            this.mBackupInfo = new BackupDragDropInfo(object, view);
        }
    }

    public void setBackupInfo(ItemInfo info) {
        this.mBackupInfo = null;
        if (info != null) {
            this.mBackupInfo = new BackupDragDropInfo(info);
        }
    }

    public void setBackupInfo(FolderIconView folderIconView) {
        this.mBackupInfo = null;
        if (folderIconView != null) {
            this.mBackupInfo = new BackupDragDropInfo(folderIconView);
        }
    }

    public BackupDragDropInfo getBackupInfo() {
        return this.mBackupInfo;
    }

    public void markAsLockedFolderWhenBind(FolderInfo folderInfo) {
        if (this.mLockedFolders == null) {
            this.mLockedFolders = new ArrayList();
        }
        folderInfo.setLocked(true);
        folderInfo.setLockedFolderOpenedOnce(false);
        this.mLockedFolders.add(folderInfo);
    }

    public void lockItem(ItemInfo dragInfo) {
        if (dragInfo instanceof FolderInfo) {
            FolderInfo info = (FolderInfo) dragInfo;
            lockFolder(info);
            info.setLocked(true);
            info.setLockedFolderOpenedOnce(false);
        } else if (dragInfo instanceof IconInfo) {
            IconInfo info2 = (IconInfo) dragInfo;
            if (!isShouldHideQuickOptions(info2)) {
                lockApp(info2);
            }
        }
    }

    public void unlockItem(ItemInfo dragInfo) {
        if (dragInfo instanceof FolderInfo) {
            FolderInfo info = (FolderInfo) dragInfo;
            unlockFolder(info);
            info.setLocked(false);
            info.setLockedFolderOpenedOnce(false);
        } else if (dragInfo instanceof IconInfo) {
            IconInfo info2 = (IconInfo) dragInfo;
            if (!isShouldHideQuickOptions(info2)) {
                unlockApp(info2);
            }
        }
    }

    private void unregisterListener() {
        try {
            LauncherAppState.getInstance().getModel().unregisterOnLauncherBindingItemsCompletedListener(this);
            if (this.mReceiver != null) {
                this.mAppContext.unregisterReceiver(this.mReceiver);
            }
            if (this.mStateReceiver != null) {
                this.mAppContext.unregisterReceiver(this.mStateReceiver);
            }
        } catch (Exception e) {
            Log.d(TAG, "can not unregister a not registed receiver");
        }
    }

    public void onDestroy() {
        unregisterListener();
        this.mLockedFolders = null;
    }

    public void onLauncherBindingItemsCompleted() {
        if (this.isFolderLockEnable) {
            this.mLauncher.runOnUiThread(new Runnable() {
                public void run() {
                    FolderLock.this.checkselfToMoveUnlockItemFromLockedFolder();
                }
            });
        }
    }

    public void lockFolderAfterAdd(FolderInfo folder) {
        lockAppsInFolder(folder);
    }

    private void startVerifyActivity(int requestCode, IconInfo item, String lockOrUnlock) {
        Intent intent = new Intent();
        intent.setAction(getAppLockedCheckAction());
        intent.putExtra(REQUEST_VERIFY_FROM, LAUNCHER_REQUEST);
        intent.putExtra(REQUEST_LOCK_OR_UNLOCK, lockOrUnlock);
        intent.putExtra(LOCKED_PACKAGE_ICON, item.mIcon);
        intent.putExtra(LOCKED_PACKAGE_LABEL, item.title.toString());
        intent.putExtra(LOCKED_PACKAGE_NAME, item.getTargetComponent().getPackageName());
        Utilities.startActivityForResultSafely(this.mLauncher, intent, requestCode);
    }

    private void startVerifyActivity(int requestCode, FolderInfo folderItem, String lockOrUnlock) {
        Intent intent = new Intent();
        intent.setAction(getAppLockedCheckAction());
        intent.putExtra(REQUEST_LOCK_OR_UNLOCK, lockOrUnlock);
        intent.putExtra(REQUEST_VERIFY_FROM, LAUNCHER_REQUEST);
        intent.putExtra(LOCKED_PACKAGE_ICON, getFolderIconBitmap(folderItem));
        intent.putExtra(LOCKED_PACKAGE_LABEL, folderItem.title.toString());
        int itemCounts = folderItem.contents.size();
        String itemPkgNames = "";
        for (int i = 0; i < itemCounts; i++) {
            IconInfo item = (IconInfo) folderItem.contents.get(i);
            if (i != 0) {
                itemPkgNames = itemPkgNames + ",";
            }
            ComponentName cn = item.getTargetComponent();
            if (cn != null) {
                itemPkgNames = itemPkgNames + cn.getPackageName();
            }
        }
        intent.putExtra(LOCKED_PACKAGE_NAME, itemPkgNames);
        Utilities.startActivityForResultSafely(this.mLauncher, intent, requestCode);
    }

    private Bitmap getFolderIconBitmap(FolderInfo info) {
        View v = null;
        for (int i = 0; i < this.mFolderLockActionCallbacks.size(); i++) {
            v = ((FolderLockActionCallback) this.mFolderLockActionCallbacks.get(i)).getFolderIconView(info);
            if (v != null) {
                break;
            }
        }
        if (v == null || !(v instanceof FolderIconView)) {
            return null;
        }
        return ((FolderIconView) v).getFolderIconBitmapWithPlate();
    }

    public boolean needPopupConfirm(ItemInfo info) {
        if (!this.isFolderLockEnable) {
            return false;
        }
        if ((info instanceof FolderInfo) && isTempUnlockedFolder((FolderInfo) info, true)) {
            return false;
        }
        if (this.mBackupInfo == null || this.mBackupInfo.getTargetInfo() == null || !(this.mBackupInfo.getTargetInfo() instanceof FolderInfo) || !isTempUnlockedFolder((FolderInfo) this.mBackupInfo.getTargetInfo(), false)) {
            return true;
        }
        return false;
    }

    public void showItemDropedConfirmDialog(ItemInfo info) {
        LockedItemDropConfirmDialog.createAndShow(this.mLauncher, info);
    }

    private String getAppLockedCheckAction() {
        Class clazz = null;
        String action = null;
        try {
            clazz = Class.forName("android.app.ActivityManager");
        } catch (Exception e) {
            Log.d(TAG, "Can not get Applock Action 1");
        }
        if (clazz == null) {
            return action;
        }
        try {
            return (String) clazz.getMethod("getAppLockedCheckAction", new Class[0]).invoke(getActivityManager(this.mAppContext), new Object[0]);
        } catch (Exception e2) {
            Log.d(TAG, "Can not get Applock Action 2");
            return action;
        }
    }

    private String makeIconInfoComponentName(IconInfo info) {
        if (info == null || info.getIntent() == null || info.getIntent().getComponent() == null) {
            Log.d(TAG, "can not make the componentName of the special info");
            return "";
        }
        ComponentName componentName = info.getIntent().getComponent();
        return componentName.getPackageName() + "," + componentName.getClassName();
    }

    private String makeFolderInfoComponentNames(FolderInfo folderInfo) {
        if (folderInfo == null || folderInfo.contents.size() < 2) {
            Log.d(TAG, "can not make the componentnames of the special folderInfo");
            return "";
        }
        String componentNames = "";
        Iterator it = folderInfo.contents.iterator();
        while (it.hasNext()) {
            IconInfo iconInfo = (IconInfo) it.next();
            if (!componentNames.isEmpty()) {
                componentNames = componentNames + ",";
            }
            componentNames = componentNames + makeIconInfoComponentName(iconInfo);
        }
        return componentNames;
    }

    public boolean canShowLockOptions(Object info) {
        if (!this.isFolderLockEnable) {
            return false;
        }
        if ((info instanceof ItemInfo) && isShouldHideQuickOptions((ItemInfo) info)) {
            return false;
        }
        if ((info instanceof IconInfo) && !isLockedApp((IconInfo) info)) {
            return true;
        }
        if (info instanceof FolderInfo) {
            if (LauncherFeature.isSSecureSupported()) {
                return false;
            }
            FolderInfo folderInfo = (FolderInfo) info;
            if (!isLockedFolderId(getInfoContainer(folderInfo), folderInfo.id)) {
                return true;
            }
        }
        return false;
    }

    public boolean canShowUnlockOptions(Object info) {
        if (!this.isFolderLockEnable) {
            return false;
        }
        if ((info instanceof ItemInfo) && isShouldHideQuickOptions((ItemInfo) info)) {
            return false;
        }
        if ((info instanceof IconInfo) && isLockedApp((IconInfo) info)) {
            return true;
        }
        if (info instanceof FolderInfo) {
            if (LauncherFeature.isSSecureSupported()) {
                return false;
            }
            FolderInfo folderInfo = (FolderInfo) info;
            if (isLockedFolderId(getInfoContainer(folderInfo), folderInfo.id)) {
                return true;
            }
        }
        return false;
    }

    public boolean canShowAddAppsOptions(Object info) {
        FolderInfo folderInfo = (FolderInfo) info;
        return (this.isFolderLockEnable && folderInfo.isLocked() && !folderInfo.isLockedFolderOpenedOnce()) ? false : true;
    }

    public boolean canShowRemoveOptions(Object info) {
        return (this.isFolderLockEnable && ((FolderInfo) info).isLocked()) ? false : true;
    }

    private static String getInfoContainer(ItemInfo info) {
        if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            return LOCK_CONTAINER_HOME_ONLY;
        }
        if (info.container == -102) {
            return LOCK_CONTAINER_MENU;
        }
        if (info.container == -100 || info.container == -101) {
            return LOCK_CONTAINER_HOME;
        }
        return null;
    }

    private boolean isAppLockEnable() {
        if (Secure.getInt(this.mAppContext.getContentResolver(), "app_lock_enabled", 0) != 0) {
            return true;
        }
        return false;
    }

    public boolean isLockedApp(IconInfo item) {
        try {
            String pkgName = item.getIntent().getComponent().getPackageName();
            String lockPackages = getLockedPackagesFromDB();
            ArrayList<String> lockedApps = new ArrayList();
            lockedApps.addAll(Arrays.asList(lockPackages.split(",")));
            if (lockedApps.contains(pkgName)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void lockApp(IconInfo item) {
        String pkgName = item.getIntent().getComponent().getPackageName();
        String lockPackages = getLockedPackagesFromDB();
        if (pkgName != null && !lockPackages.contains(pkgName)) {
            if (lockPackages.isEmpty()) {
                lockPackages = pkgName;
            } else {
                lockPackages = lockPackages + "," + pkgName;
            }
            if (LauncherFeature.isSSecureSupported()) {
                sendAppLockChangedBroadcast(lockPackages, item, true);
            } else {
                updateLockedPackagesToDB(lockPackages);
            }
        }
    }

    private void unlockApp(IconInfo item) {
        ComponentName componentName = item.getIntent().getComponent();
        String pkgName = "";
        String clsName = "";
        if (componentName != null) {
            clsName = componentName.getClassName();
            pkgName = componentName.getPackageName();
        }
        String[] items = getLockedPackagesFromDB().split(",");
        String result = "";
        int i = 0;
        while (i < items.length) {
            if (pkgName == null || !pkgName.equals(items[i])) {
                if (result.isEmpty()) {
                    result = result + items[i];
                } else {
                    result = result + "," + items[i];
                }
            }
            i++;
        }
        putOutUnlockedItemFromLockedFolder(pkgName + "," + clsName);
        if (LauncherFeature.isSSecureSupported()) {
            sendAppLockChangedBroadcast(result, item, false);
        } else {
            updateLockedPackagesToDB(result);
        }
    }

    private void unlockAppsInFolder(FolderInfo folderItem) {
        int i;
        String pkgName = "";
        String clsName = "";
        String lockPackages = getLockedPackagesFromDB();
        String componentsNeedUnlock = "";
        ArrayList<String> lockedPackageList = new ArrayList();
        lockedPackageList.addAll(Arrays.asList(lockPackages.split(",")));
        int itemCounts = folderItem.contents.size();
        for (i = 0; i < itemCounts; i++) {
            IconInfo item = (IconInfo) folderItem.contents.get(i);
            ComponentName componentName = item.getIntent().getComponent();
            if (!(componentName == null || isShouldHideQuickOptions(item))) {
                pkgName = componentName.getPackageName();
                clsName = componentName.getClassName();
                componentName.getClassName();
                if (i != 0) {
                    componentsNeedUnlock = componentsNeedUnlock + SEPARATOR_FOLDERID;
                }
                componentsNeedUnlock = componentsNeedUnlock + pkgName + "," + clsName;
                try {
                    lockedPackageList.remove(pkgName);
                } catch (Exception e) {
                    Log.d(TAG, "the pkg not in the licked list");
                }
            }
        }
        lockPackages = "";
        for (i = 0; i < lockedPackageList.size(); i++) {
            if (i == 0) {
                lockPackages = (String) lockedPackageList.get(i);
            } else {
                lockPackages = lockPackages + "," + ((String) lockedPackageList.get(i));
            }
        }
        putOutUnlockedItemFromLockedFolder(componentsNeedUnlock);
        updateLockedPackagesToDB(lockPackages);
    }

    private boolean isShouldHideQuickOptions(ItemInfo item) {
        if (item != null) {
            try {
                if (item.getIntent() != null) {
                    if (item.getIntent().getComponent() == null || item.getIntent().getComponent().getPackageName() == null) {
                        Log.d(TAG, "An item without componentName or packageName we should hide the quick option");
                        return true;
                    } else if (item.getIntent().getComponent().getPackageName().equals("com.samsung.knox.rcp.components")) {
                        Log.d(TAG, "Knox shortcut we should hide the quick option");
                        return true;
                    } else if (isInWhiteList(item)) {
                        Log.d(TAG, "An item in whitelist we should hide the quick option;");
                        return true;
                    } else {
                        if (item != null && LauncherFeature.isSSecureSupported()) {
                            UserHandleCompat user = item.getUserHandle();
                            if (!(user == null || user.equals(UserHandleCompat.myUserHandle()))) {
                                Log.d(TAG, "AFW user app. we should hide the quick option");
                                return true;
                            }
                        }
                        return false;
                    }
                }
            } catch (Exception e) {
                return false;
            }
        }
        Log.d(TAG, "A null item or An item without intent we should hide the quick option");
        return true;
    }

    private boolean isInWhiteList(ItemInfo item) {
        return this.mWhiteList.contains(item.getIntent().getComponent().getPackageName());
    }

    private void lockAppsInFolder(FolderInfo folderItem) {
        String lockPackages = getLockedPackagesFromDB();
        List<String> packageList = Arrays.asList(lockPackages.split(","));
        int itemCounts = folderItem.contents.size();
        for (int i = 0; i < itemCounts; i++) {
            IconInfo item = (IconInfo) folderItem.contents.get(i);
            ComponentName cn = item.getTargetComponent();
            if (!(isShouldHideQuickOptions(item) || cn == null)) {
                String pkgName = cn.getPackageName();
                if (!packageList.contains(pkgName)) {
                    if (lockPackages.isEmpty()) {
                        lockPackages = pkgName;
                    } else {
                        lockPackages = lockPackages + "," + pkgName;
                    }
                }
            }
        }
        updateLockedPackagesToDB(lockPackages);
        applyFolderNameChanged();
    }

    public void openFolder(FolderInfo item) {
        item.setLockedFolderOpenedOnce(true);
    }

    public void addLockedRecords(FolderInfo item) {
        if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            addLockedRecords(LOCK_CONTAINER_HOME_ONLY, item.id);
        } else if (item.container == -102) {
            addLockedRecords(LOCK_CONTAINER_MENU, item.id);
        } else if (item.container == -100 || item.container == -101 || item.container == -1) {
            addLockedRecords(LOCK_CONTAINER_HOME, item.id);
        }
    }

    private void lockFolder(FolderInfo item) {
        addLockedRecords(item);
        if (!this.mLockedFolders.contains(item)) {
            FolderInfo same = null;
            for (FolderInfo lockeditem : this.mLockedFolders) {
                if (lockeditem.container == item.container && lockeditem.itemType == item.itemType && lockeditem.id == item.id) {
                    same = lockeditem;
                }
            }
            this.mLockedFolders.remove(same);
            this.mLockedFolders.add(item);
        }
        lockAppsInFolder(item);
    }

    private void unlockFolder(FolderInfo item) {
        if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            removeLockedRecords(LOCK_CONTAINER_HOME_ONLY, item.id);
        } else if (item.itemType == 2) {
            removeLockedRecords(getInfoContainer(item), item.id);
        }
        if (this.mLockedFolders.contains(item)) {
            this.mLockedFolders.remove(item);
        }
        unlockAppsInFolder(item);
    }

    private void addLockedRecords(String container, long itemId) {
        this.mLockedItemRecords.add(this.mAppContext, container, itemId);
    }

    private void removeLockedRecords(String container, long itemId) {
        this.mLockedItemRecords.remove(this.mAppContext, container, itemId);
    }

    private boolean isLockedFolderId(String container, long itemId) {
        return this.mLockedItemRecords.contains(this.mAppContext, container, String.valueOf(itemId));
    }

    public boolean isLockedFolder(FolderInfo info) {
        return this.mLockedItemRecords.contains(this.mAppContext, getInfoContainer(info), String.valueOf(info.id));
    }

    private boolean isTempUnlockedFolder(FolderInfo item, boolean isFromDrag) {
        return this.mLockedItemRecords.contains(this.mAppContext, getUnlockContainer(item, isFromDrag), String.valueOf(item.id));
    }

    private void addTempUnlockedFolder(FolderInfo item, boolean isFromDrag) {
        this.mLockedItemRecords.add(this.mAppContext, getUnlockContainer(item, isFromDrag), item.id);
    }

    private static String getUnlockContainer(FolderInfo item, boolean isFromDrag) {
        String container = "";
        String containerAddition = "";
        if (isFromDrag) {
            containerAddition = UNLOCK_CONTAINER_ADDITION;
        }
        if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            return UNLOCK_CONTAINER_HOME_ONLY + containerAddition;
        }
        if (item.container != -102) {
            return UNLOCK_CONTAINER_HOME + containerAddition;
        }
        return UNLOCK_CONTAINER_MENU + containerAddition;
    }

    private void updateLockedPackagesToDB(String pkgNames) {
        if (VERSION.SDK_INT >= 24) {
            Intent intent = new Intent();
            intent.setAction(NOTIFY_APPLOCK_UPDATE_ACTION);
            intent.setPackage(APPLOCK_PACKAGENAME);
            intent.putExtra(KEY_LASTEST_LOCKED_PACKAGES, pkgNames);
            this.mLauncher.sendBroadcast(intent);
            return;
        }
        try {
            Log.e(TAG, "updateLockedPackagesToDB  result =" + System.putString(getContentResolver(), LOCKED_PACKAGE, pkgNames));
        } catch (Exception e) {
            Log.e(TAG, "updateLockedPackagesToDB Settings.System.putString error e=" + e.toString());
        }
    }

    private void updateLockedAppFolderMapping(String names) {
        try {
            boolean result = System.putString(getContentResolver(), LOCKED_APP_FOLDERS, names);
            if (Utilities.DEBUGGABLE()) {
                Log.d(TAG, "updateLockedAppFolderMapping is  : " + result + " / " + names);
            }
        } catch (Exception e) {
            Log.d(TAG, "update the mapping failed");
        }
    }

    private String getLockedPackagesFromDB() {
        String lockedPackages;
        if (VERSION.SDK_INT >= 24) {
            lockedPackages = Secure.getString(getContentResolver(), LOCKED_PACKAGE_N);
        } else {
            lockedPackages = System.getString(getContentResolver(), LOCKED_PACKAGE);
        }
        if (lockedPackages == null) {
            lockedPackages = "";
        }
        if (Utilities.DEBUGGABLE()) {
            Log.d(TAG, "getLockedPackagesFromDB result is  " + lockedPackages);
        }
        return lockedPackages;
    }

    private String getLockedAppFolderMapping() {
        String mappings = System.getString(getContentResolver(), LOCKED_APP_FOLDERS);
        if (mappings == null) {
            return "";
        }
        return mappings;
    }

    public void applyFolderNameChanged() {
        if (this.mLockedFolders != null) {
            String mapping = "";
            String pkgName = "";
            String clsName = "";
            String folderName = "";
            for (FolderInfo folderItem : this.mLockedFolders) {
                if (!LauncherAppState.getInstance().isHomeOnlyModeEnabled() || folderItem.container != -102) {
                    int itemCount = folderItem.contents.size();
                    folderName = String.valueOf(folderItem.title);
                    for (int i = 0; i < itemCount; i++) {
                        IconInfo item = (IconInfo) folderItem.contents.get(i);
                        if (!(item.getIntent() == null || item.getIntent().getComponent() == null)) {
                            pkgName = item.getIntent().getComponent().getPackageName();
                            clsName = item.getIntent().getComponent().getClassName();
                            if (!mapping.isEmpty()) {
                                mapping = mapping + SEPARATOR_FOLDERID;
                            }
                            mapping = ((((mapping + pkgName) + ",") + clsName) + SEPARATOR_CLS_FOLDERID) + folderName;
                        }
                    }
                }
            }
            updateLockedAppFolderMapping(mapping);
        }
    }

    private ContentResolver getContentResolver() {
        return this.mAppContext.getContentResolver();
    }

    private synchronized void putOutUnlockedItemFromLockedFolder(String unlockComponentNames) {
        ArrayList<String> unlockComponentList = new ArrayList();
        unlockComponentList.addAll(Arrays.asList(unlockComponentNames.split(SEPARATOR_FOLDERID)));
        List<FolderInfo> foldersNeedUnLock = new ArrayList();
        if (Utilities.DEBUGGABLE()) {
            Log.d(TAG, "the unlock componentNames of putOutUnlockedItemFromLockedFolder is :  " + unlockComponentNames);
        }
        Iterator it = unlockComponentList.iterator();
        while (it.hasNext()) {
            String unlockComponent = (String) it.next();
            for (FolderInfo folder : this.mLockedFolders) {
                ArrayList<IconInfo> homeNeedUpdateInfos = new ArrayList();
                ArrayList<IconInfo> appsNeedUpdateInfos = new ArrayList();
                if (folder != null) {
                    List<IconInfo> iconInfosInFolder = folder.contents;
                    int remainedCount = iconInfosInFolder.size();
                    for (IconInfo info : iconInfosInFolder) {
                        if (!(info == null || info.getIntent() == null || info.getIntent().getComponent() == null)) {
                            ComponentName componentName = info.getIntent().getComponent();
                            if ((componentName.getPackageName() + "," + componentName.getClassName()).equals(unlockComponent)) {
                                remainedCount--;
                                if (remainedCount < 2 && !foldersNeedUnLock.contains(folder)) {
                                    foldersNeedUnLock.add(folder);
                                }
                                if (folder.container == -102) {
                                    appsNeedUpdateInfos.add(info);
                                } else if (folder.container == -101 || folder.container == -100) {
                                    homeNeedUpdateInfos.add(info);
                                }
                            } else {
                                continue;
                            }
                        }
                    }
                    moveoutHomeOrAppsItemsFromLockedFolder(folder, homeNeedUpdateInfos, appsNeedUpdateInfos);
                }
            }
        }
        removeLockedRecordsSinceOnlyOneItemLeft(foldersNeedUnLock);
        applyFolderNameChanged();
    }

    private synchronized void removeLockedRecordsSinceOnlyOneItemLeft(List<FolderInfo> needRemovefolderInfos) {
        for (FolderInfo needRemovefolderInfo : needRemovefolderInfos) {
            this.mLockedFolders.remove(needRemovefolderInfo);
            removeLockedRecords(getInfoContainer(needRemovefolderInfo), needRemovefolderInfo.id);
        }
    }

    private synchronized void checkselfToMoveUnlockItemFromLockedFolder() {
        if (this.mLockedFolders != null) {
            List<FolderInfo> foldersNeedUnLock = new ArrayList();
            for (FolderInfo folder : this.mLockedFolders) {
                if (Utilities.DEBUGGABLE()) {
                    Log.d(TAG, "checkselfToMoveUnlockItemFromLockedFolder and the mLockedFolders is   :  " + folder.toString());
                }
                ArrayList<IconInfo> homeNeedUpdateInfos = new ArrayList();
                ArrayList<IconInfo> appsNeedUpdateInfos = new ArrayList();
                List<IconInfo> infosInFolder = folder.contents;
                int remainedCount = infosInFolder.size();
                for (int index = 0; index < infosInFolder.size(); index++) {
                    IconInfo iconInfo = (IconInfo) infosInFolder.get(index);
                    if (!isLockedApp(iconInfo)) {
                        remainedCount--;
                        if (remainedCount < 2 && !foldersNeedUnLock.contains(folder)) {
                            foldersNeedUnLock.add(folder);
                        }
                        if (folder.container == -102) {
                            appsNeedUpdateInfos.add(iconInfo);
                        } else if (folder.container == -100 || folder.container == -101) {
                            homeNeedUpdateInfos.add(iconInfo);
                        }
                    }
                }
                moveoutHomeOrAppsItemsFromLockedFolder(folder, homeNeedUpdateInfos, appsNeedUpdateInfos);
            }
            removeLockedRecordsSinceOnlyOneItemLeft(foldersNeedUnLock);
            applyFolderNameChanged();
        }
    }

    private void moveoutHomeOrAppsItemsFromLockedFolder(FolderInfo folder, ArrayList<IconInfo> homeNeedUpdateInfos, ArrayList<IconInfo> appsNeedUpdateInfos) {
        for (int i = 0; i < this.mFolderLockActionCallbacks.size(); i++) {
            ((FolderLockActionCallback) this.mFolderLockActionCallbacks.get(i)).moveOutItemsFromLockedFolder(folder, homeNeedUpdateInfos, appsNeedUpdateInfos);
        }
    }

    private void clearTempUnlockedFolder() {
        this.mLockedItemRecords.removeAll(this.mAppContext, UNLOCK_CONTAINER_HOME_ONLY);
        this.mLockedItemRecords.removeAll(this.mAppContext, UNLOCK_CONTAINER_MENU);
        this.mLockedItemRecords.removeAll(this.mAppContext, UNLOCK_CONTAINER_HOME);
    }

    private void initWhiteList() {
        String[] whiteListString = getWhiteListFromApplock();
        if (whiteListString != null) {
            this.mWhiteList.addAll(Arrays.asList(whiteListString));
            return;
        }
        this.mWhiteList.addAll(Arrays.asList(this.mAppContext.getResources().getStringArray(R.array.applock_white_list_pkg)));
    }

    private String[] getWhiteListFromApplock() {
        try {
            Resources r = this.mLauncher.getPackageManager().getResourcesForApplication(APPLOCK_PACKAGENAME);
            int whiteListId = r.getIdentifier("applock_white_list_pkg", "array", APPLOCK_PACKAGENAME);
            if (whiteListId > 0) {
                String[] whiteListArr = r.getStringArray(whiteListId);
                if (whiteListArr.length > 0) {
                    this.mWhiteList.addAll(Arrays.asList(whiteListArr));
                    Log.d(TAG, "whiteListArr from Applock length is :" + whiteListArr.length);
                }
            }
        } catch (Exception e) {
            Log.i(TAG, "Can not get whitelist from Applock");
        }
        if (this.mWhiteList.size() <= 0) {
            this.mWhiteList.addAll(Arrays.asList(this.mAppContext.getResources().getStringArray(R.array.applock_white_list_pkg)));
        }
        return null;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        BackupDragDropInfo backupInfo = getBackupInfo();
        if (backupInfo != null) {
            ItemInfo dragInfo = backupInfo.getDragInfo();
            IconInfo copyInfo = backupInfo.getCopyInfo();
            FolderView dragFolder = null;
            View targetView = backupInfo.getTargetView();
            switch (requestCode) {
                case REQUEST_CODE_FOLDER_LOCK /*122*/:
                    if (resultCode != -1) {
                        if (resultCode == 0 && dragInfo != null && copyInfo != null && (targetView instanceof FolderIconView)) {
                            FolderInfo fInfo = (FolderInfo) targetView.getTag();
                            if (backupInfo.isDragInFolder()) {
                                dragFolder = backupInfo.getDragParetFolder();
                            }
                            backItemByFolderLock(this, dragInfo, copyInfo, fInfo, dragFolder);
                            break;
                        }
                    } else if (dragInfo == null || copyInfo == null) {
                        if (backupInfo.getTargetInfo() != null) {
                            lockItem(backupInfo.getTargetInfo());
                            break;
                        }
                    } else {
                        ItemInfo targetInfo = backupInfo.getTargetInfo();
                        if (targetInfo instanceof FolderInfo) {
                            addTempUnlockedFolder((FolderInfo) targetInfo, true);
                        }
                        lockItem(dragInfo);
                        applyFolderNameChanged();
                        break;
                    }
                    break;
                case REQUEST_CODE_FOLDER_UNLOCK /*123*/:
                    if (resultCode == -1) {
                        if (dragInfo == null || copyInfo == null) {
                            if (backupInfo.getTargetInfo() != null) {
                                unlockItem(backupInfo.getTargetInfo());
                                break;
                            }
                        }
                        unlockItem(dragInfo);
                        applyFolderNameChanged();
                        break;
                    }
                    break;
                case REQUEST_CODE_OPEN_LOCKEDFOLDER /*124*/:
                    if (resultCode == -1) {
                        FolderIconView clickedFolder = backupInfo.getClickedFolder();
                        if (clickedFolder != null) {
                            openFolder(clickedFolder.getFolderInfo());
                            this.mLauncher.openFolder(clickedFolder);
                            break;
                        }
                    }
                    break;
            }
            setBackupInfo(null, null);
        }
    }

    private void backItemByFolderLock(FolderLock fl, ItemInfo dragInfo, IconInfo copyInfo, FolderInfo targetInfo, FolderView dragFolder) {
        long container;
        long screen;
        int cellx;
        int celly;
        int rank;
        fl.setRecoverMode(true);
        targetInfo.remove((IconInfo) dragInfo);
        long formerID = dragInfo.id;
        dragInfo.copyFrom(copyInfo);
        dragInfo.id = formerID;
        if (dragFolder == null) {
            container = copyInfo.container;
            screen = copyInfo.screenId;
            cellx = copyInfo.cellX;
            celly = copyInfo.cellY;
            rank = copyInfo.rank;
        } else {
            FolderInfo dragOriginal = dragFolder.getInfo();
            container = dragOriginal.container;
            screen = dragOriginal.screenId;
            cellx = dragOriginal.cellX;
            celly = dragOriginal.cellY;
            if (dragOriginal.contents.size() < 2) {
                rank = dragOriginal.rank;
            } else {
                rank = dragInfo.rank;
            }
        }
        if (this.mLauncher.getStageManager().getTopStage() instanceof ControllerBase) {
            ControllerBase cb = (ControllerBase) this.mLauncher.getStageManager().getTopStage();
            cb.recoverCancelItemForFolderLock((IconInfo) dragInfo, container, screen, cellx, celly, rank);
            if (dragFolder != null) {
                container = dragInfo.container;
                screen = dragInfo.screenId;
            }
            if (dragInfo.container == -102) {
                dragInfo.rank = rank;
            }
            cb.addOrMoveItemInDb(dragInfo, container, (long) ((int) screen), cellx, celly, rank);
        }
        fl.setRecoverMode(false);
    }

    private void sendAppLockChangedBroadcast(String pkgNames, IconInfo item, boolean isLocked) {
        String toastMsg;
        String packageName = item.getIntent().getComponent().getPackageName();
        Intent intent = new Intent();
        intent.setAction(NOTIFY_APPLOCK_UPDATE_ACTION);
        intent.setPackage(APPLOCK_PACKAGENAME);
        intent.putExtra(KEY_LASTEST_LOCKED_PACKAGES, pkgNames);
        intent.putExtra("package_name", packageName);
        intent.putExtra("is_locked", isLocked);
        this.mLauncher.sendBroadcast(intent);
        if (isLocked) {
            toastMsg = this.mAppContext.getResources().getString(R.string.app_locked_toast, new Object[]{item.title.toString()});
            GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_APP_LOCK, packageName, -1, false);
        } else {
            toastMsg = this.mAppContext.getResources().getString(R.string.app_unlocked_toast, new Object[]{item.title.toString()});
        }
        Toast.makeText(this.mAppContext, toastMsg, 0).show();
    }
}

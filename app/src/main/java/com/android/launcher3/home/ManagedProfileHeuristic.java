package com.android.launcher3.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.model.FavoritesUpdater;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.util.DualAppUtils;
import com.android.launcher3.util.MainThreadExecutor;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ManagedProfileHeuristic {
    private static final String TAG = "ManagedProfileHeuristic";
    private final Context mContext;
    private final String mFolderNameKey;
    private ArrayList<IconInfo> mHomescreenApps;
    private final LauncherModel mModel = LauncherAppState.getInstance().getModel();
    private final String mPackageSetKey;
    private final SharedPreferences mPrefs;
    private final UserHandleCompat mUser;
    private final long mUserSerial;
    private ArrayList<IconInfo> mWorkFolderApps;

    public static ManagedProfileHeuristic get(Context context, UserHandleCompat user) {
        if (isAvailableCreateManagedProfileHeuristic(context, user)) {
            return new ManagedProfileHeuristic(context, user);
        }
        return null;
    }

    private ManagedProfileHeuristic(Context context, UserHandleCompat user) {
        this.mContext = context;
        this.mUser = user;
        this.mUserSerial = UserManagerCompat.getInstance(context).getSerialNumberForUser(user);
        this.mPackageSetKey = getInstalledPackagesPrefix() + this.mUserSerial;
        this.mFolderNameKey = Utilities.USER_FOLDER_NAME_PREFIX + this.mUserSerial;
        this.mPrefs = this.mContext.getSharedPreferences(LauncherFiles.MANAGED_USER_PREFERENCES_KEY, 0);
    }

    public void processUserApps(List<LauncherActivityInfoCompat> apps) {
        this.mHomescreenApps = new ArrayList();
        this.mWorkFolderApps = new ArrayList();
        HashSet<String> packageSet = new HashSet();
        boolean userAppsExisted = getUserApps(packageSet);
        boolean newPackageAdded = false;
        for (LauncherActivityInfoCompat info : apps) {
            String packageName = info.getComponentName().getPackageName();
            if (!packageSet.contains(packageName)) {
                Cursor cursor = this.mContext.getContentResolver().query(Favorites.CONTENT_URI, new String[]{"_id"}, "intent LIKE ? AND itemType=0 AND hidden=" + String.valueOf(2) + " AND " + BaseLauncherColumns.PROFILE_ID + '=' + String.valueOf(this.mUserSerial), new String[]{'%' + info.getComponentName().flattenToShortString() + '%'}, null);
                if (cursor != null) {
                    try {
                        if (cursor.getCount() > 0) {
                            Log.d(TAG, "work app was hided - " + info.getLabel());
                        } else {
                            cursor.close();
                        }
                    } finally {
                        cursor.close();
                    }
                }
                markForAddition(info);
                packageSet.add(packageName);
                newPackageAdded = true;
            }
        }
        if (newPackageAdded) {
            this.mPrefs.edit().putStringSet(this.mPackageSetKey, packageSet).apply();
            finalizeAdditions(userAppsExisted);
        }
    }

    private void checkAllAppsForWorkFolder(List<LauncherActivityInfoCompat> apps) {
        HashSet<String> packageSet = new HashSet();
        getUserApps(packageSet);
        boolean newPackageAdded = false;
        Log.d(TAG, "checkAllAppsForWorkFolder : " + apps.size());
        for (LauncherActivityInfoCompat info : apps) {
            String packageName = info.getComponentName().getPackageName();
            if (!packageSet.contains(packageName)) {
                Cursor cursor = this.mContext.getContentResolver().query(Favorites.CONTENT_URI, new String[]{"_id"}, "intent LIKE ? AND itemType=0 AND hidden=" + String.valueOf(2) + " AND " + BaseLauncherColumns.PROFILE_ID + '=' + String.valueOf(this.mUserSerial), new String[]{'%' + info.getComponentName().flattenToShortString() + '%'}, null);
                if (cursor != null) {
                    try {
                        if (cursor.getCount() > 0) {
                            Log.d(TAG, "work app was hided - " + info.getLabel());
                        } else {
                            cursor.close();
                        }
                    } finally {
                        cursor.close();
                    }
                }
                Log.d(TAG, "add work folder app : " + info.getLabel());
                this.mWorkFolderApps.add(IconInfo.fromActivityInfo(info, this.mContext));
                packageSet.add(packageName);
                newPackageAdded = true;
            }
        }
        if (newPackageAdded) {
            this.mPrefs.edit().putStringSet(this.mPackageSetKey, packageSet).apply();
        }
    }

    private void markForAddition(LauncherActivityInfoCompat info) {
        this.mWorkFolderApps.add(IconInfo.fromActivityInfo(info, this.mContext));
    }

    private void finalizeWorkFolder() {
        if (!this.mWorkFolderApps.isEmpty()) {
            Collections.sort(this.mWorkFolderApps, new Comparator<IconInfo>() {
                public int compare(IconInfo lhs, IconInfo rhs) {
                    return Long.compare(lhs.firstInstallTime, rhs.firstInstallTime);
                }
            });
            String folderIdKey = getUserFolderIdPrefix() + this.mUserSerial;
            final FolderInfo workFolder;
            if (this.mPrefs.contains(folderIdKey)) {
                long folderId = this.mPrefs.getLong(folderIdKey, 0);
                workFolder = this.mModel.getHomeLoader().findFolderById(Long.valueOf(folderId));
                if (workFolder == null || !workFolder.hasOption(2)) {
                    Log.d(TAG, "work folder does not exist : " + folderId);
                    this.mHomescreenApps.addAll(this.mWorkFolderApps);
                    return;
                }
                saveWorkFolderShortcuts(folderId, workFolder.contents.size());
                final ArrayList<IconInfo> shortcuts = this.mWorkFolderApps;
                new MainThreadExecutor().execute(new Runnable() {
                    public void run() {
                        Iterator it = shortcuts.iterator();
                        while (it.hasNext()) {
                            workFolder.add((IconInfo) it.next());
                        }
                    }
                });
                return;
            }
            Log.d(TAG, "create work folder");
            workFolder = new FolderInfo();
            workFolder.title = this.mPrefs.getString(this.mFolderNameKey, this.mContext.getText(R.string.work_folder_name).toString());
            workFolder.setOption(2, true, null);
            if (this.mWorkFolderApps.size() == 1) {
                Log.d(TAG, "only 1 work folder app. need check other app");
                LauncherAppsCompat launcherapps = LauncherAppsCompat.getInstance(this.mContext);
                if (launcherapps != null) {
                    checkAllAppsForWorkFolder(launcherapps.getActivityList(null, this.mUser));
                }
            }
            Iterator it = this.mWorkFolderApps.iterator();
            while (it.hasNext()) {
                workFolder.add((IconInfo) it.next());
            }
            ArrayList<ItemInfo> itemList = new ArrayList(1);
            itemList.add(workFolder);
            workFolder.id = FavoritesProvider.getInstance().generateNewItemId();
            this.mModel.getHomeLoader().addAndBindAddedWorkspaceItems(this.mContext, itemList, false);
            this.mPrefs.edit().putLong(getUserFolderIdPrefix() + this.mUserSerial, workFolder.id).apply();
            saveWorkFolderShortcuts(workFolder.id, 0);
        }
    }

    private void saveWorkFolderShortcuts(long workFolderId, int startingRank) {
        Iterator it = this.mWorkFolderApps.iterator();
        while (it.hasNext()) {
            ItemInfo info = (ItemInfo) it.next();
            int startingRank2 = startingRank + 1;
            info.rank = startingRank;
            info.container = workFolderId;
            info.screenId = 0;
            info.cellX = 0;
            info.cellY = 0;
            ((FavoritesUpdater) this.mModel.getHomeLoader().getUpdater()).addItem(info);
            startingRank = startingRank2;
        }
    }

    private void finalizeAdditions(boolean addHomeScreenShortcuts) {
        finalizeWorkFolder();
        if (addHomeScreenShortcuts && !this.mHomescreenApps.isEmpty()) {
            this.mModel.getHomeLoader().addAndBindAddedWorkspaceItems(this.mContext, this.mHomescreenApps, false);
        }
    }

    public void processPackageAdd(String[] packages) {
        this.mHomescreenApps = new ArrayList();
        this.mWorkFolderApps = new ArrayList();
        HashSet<String> packageSet = new HashSet();
        boolean userAppsExisted = getUserApps(packageSet);
        boolean newPackageAdded = false;
        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(this.mContext);
        for (String packageName : packages) {
            if (!packageSet.contains(packageName)) {
                List<LauncherActivityInfoCompat> activities = launcherApps.getActivityList(packageName, this.mUser);
                if (activities.isEmpty()) {
                    Log.d(TAG, "activity list is empty : " + packageName);
                } else {
                    markForAddition((LauncherActivityInfoCompat) activities.get(0));
                    packageSet.add(packageName);
                    newPackageAdded = true;
                }
            }
        }
        if (newPackageAdded) {
            this.mPrefs.edit().putStringSet(this.mPackageSetKey, packageSet).apply();
            finalizeAdditions(userAppsExisted);
        }
    }

    public void processPackageRemoved(String[] packages) {
        HashSet<String> packageSet = new HashSet();
        getUserApps(packageSet);
        boolean packageRemoved = false;
        for (String packageName : packages) {
            if (packageSet.remove(packageName)) {
                packageRemoved = true;
            }
        }
        if (packageRemoved) {
            this.mPrefs.edit().putStringSet(this.mPackageSetKey, packageSet).apply();
        }
    }

    private boolean getUserApps(HashSet<String> outExistingApps) {
        Set<String> userApps = this.mPrefs.getStringSet(this.mPackageSetKey, null);
        if (userApps == null) {
            return false;
        }
        outExistingApps.addAll(userApps);
        return true;
    }

    public static void processAllUsers(List<UserHandleCompat> users, Context context) {
        UserManagerCompat userManager = UserManagerCompat.getInstance(context);
        HashSet<String> validKeys = new HashSet();
        for (UserHandleCompat user : users) {
            addAllUserKeys(userManager.getSerialNumberForUser(user), validKeys);
        }
        SharedPreferences prefs = context.getSharedPreferences(LauncherFiles.MANAGED_USER_PREFERENCES_KEY, 0);
        Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet()) {
            if (!validKeys.contains(key)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    private static void addAllUserKeys(long userSerial, HashSet<String> keysOut) {
        keysOut.add(Utilities.INSTALLED_PACKAGES_PREFIX_HOME_ONLY + userSerial);
        keysOut.add(Utilities.INSTALLED_PACKAGES_PREFIX + userSerial);
        keysOut.add(Utilities.USER_FOLDER_ID_PREFIX_HOME_ONLY + userSerial);
        keysOut.add(Utilities.USER_FOLDER_ID_PREFIX + userSerial);
        keysOut.add(Utilities.BLOCK_CREATE_SHORTCUT_PREFIX + userSerial);
        keysOut.add(Utilities.USER_FOLDER_NAME_PREFIX + userSerial);
    }

    static void markExistingUsersForNoFolderCreation(Context context) {
        UserManagerCompat userManager = UserManagerCompat.getInstance(context);
        UserHandleCompat myUser = UserHandleCompat.myUserHandle();
        SharedPreferences prefs = null;
        for (UserHandleCompat user : userManager.getUserProfiles()) {
            if (!myUser.equals(user)) {
                if (prefs == null) {
                    prefs = context.getSharedPreferences(LauncherFiles.MANAGED_USER_PREFERENCES_KEY, 0);
                }
                String folderIdKey = getUserFolderIdPrefix() + userManager.getSerialNumberForUser(user);
                if (!prefs.contains(folderIdKey)) {
                    prefs.edit().putLong(folderIdKey, -1).apply();
                }
            }
        }
    }

    private static String getUserFolderIdPrefix() {
        return LauncherAppState.getInstance().isHomeOnlyModeEnabled() ? Utilities.USER_FOLDER_ID_PREFIX_HOME_ONLY : Utilities.USER_FOLDER_ID_PREFIX;
    }

    private static String getInstalledPackagesPrefix() {
        return LauncherAppState.getInstance().isHomeOnlyModeEnabled() ? Utilities.INSTALLED_PACKAGES_PREFIX_HOME_ONLY : Utilities.INSTALLED_PACKAGES_PREFIX;
    }

    private static boolean isAvailableCreateManagedProfileHeuristic(Context context, UserHandleCompat user) {
        long userSerial = UserManagerCompat.getInstance(context).getSerialNumberForUser(user);
        SharedPreferences prefs = context.getSharedPreferences(LauncherFiles.MANAGED_USER_PREFERENCES_KEY, 0);
        String blockCreateShortcutKey = Utilities.BLOCK_CREATE_SHORTCUT_PREFIX + userSerial;
        boolean blockCreateShortcut = prefs.getBoolean(blockCreateShortcutKey, false);
        Log.d(TAG, "check blockCreateShortcutKey : " + blockCreateShortcutKey + " blockCreate : " + blockCreateShortcut);
        if (UserHandleCompat.myUserHandle().equals(user)) {
            return false;
        }
        if ((DualAppUtils.supportDualApp(context) && DualAppUtils.isDualAppId(user)) || blockCreateShortcut) {
            return false;
        }
        return true;
    }
}

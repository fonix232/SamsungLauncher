package com.android.launcher3.common.customer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.LauncherFeature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PostPositionSharedPref {
    private static final int DISABLED = -1;
    public static final String PREFERENCES = "com.sec.android.app.launcher.prefs.PostPosition";
    private static final String PREFS_APPS = "Apps_";
    private static final String PREFS_AUTO_FOLDER_NAME = "AutoFolderingFolderName";
    public static final String PREFS_FOLDER_ID = "FolderId";
    private static final String PREFS_FOLDER_NAME = "FolderName";
    public static final String PREFS_FOLDER_READY_ID = "FolderReadyId";
    private static final String PREFS_HOME = "Home_";
    private static final String PREFS_INSTALLER_PACKAGE = "Installer_Package";
    private static final String PREFS_OMC_PATH = "OMC_PATH";
    public static final String PREFS_POST_POSITION_ENABLED = "PostPostionEnabled";
    public static final String PREFS_PRELOADED_FOLDER_ID = "PreloadedFolderId";
    private static final String PREFS_PRELOADED_FOLDER_NAME = "PreladedFolderName";
    public static final long REMOVED = 99999;
    private static final String TAG = "PostPositionSharedPref";
    private int mContainer;
    private final Context mContext;
    private SharedPreferences mPrefs = this.mContext.getSharedPreferences(PREFERENCES, 0);

    public PostPositionSharedPref(Context context) {
        this.mContext = context;
    }

    public void setContainer(int container) {
        this.mContainer = container;
    }

    public int getContainer() {
        return this.mContainer;
    }

    public void setEnabled(boolean value) {
        Editor editor = this.mPrefs.edit();
        editor.putLong(PREFS_POST_POSITION_ENABLED, (long) (value ? 1 : 0));
        editor.apply();
    }

    public long isEnabled() {
        return this.mPrefs.getLong(PREFS_POST_POSITION_ENABLED, -1);
    }

    public String getContainerKey(String prefix, String suffix) {
        if (prefix == null) {
            return (this.mContainer == -100 ? PREFS_HOME : PREFS_APPS) + suffix;
        }
        return prefix + (prefix.isEmpty() ? "" : "_") + (this.mContainer == -100 ? PREFS_HOME : PREFS_APPS) + suffix;
    }

    public long getPreloadedFolderId(String foldername) {
        return this.mPrefs.getLong(getContainerKey(foldername, PREFS_PRELOADED_FOLDER_ID), -1);
    }

    public String getPreloadedFolderName(long id) {
        return this.mPrefs.getString(getContainerKey("" + id, PREFS_PRELOADED_FOLDER_NAME), null);
    }

    public void writePreloadedFolderId(String foldername, long id) {
        Editor editor = this.mPrefs.edit();
        editor.putLong(getContainerKey(foldername, PREFS_PRELOADED_FOLDER_ID), id);
        if (id != REMOVED) {
            editor.putString(getContainerKey("" + id, PREFS_PRELOADED_FOLDER_NAME), foldername);
        }
        editor.apply();
        Log.i(TAG, "writePreloadedFolderId : " + this.mContainer + " " + foldername + ", " + id);
    }

    public void writeAutoFolderingInfo(String foldername, String pkgName) {
        Editor editor = this.mPrefs.edit();
        editor.putString(getContainerKey("", PREFS_AUTO_FOLDER_NAME), foldername);
        editor.putString(getContainerKey("", PREFS_INSTALLER_PACKAGE), pkgName);
        editor.apply();
        Log.i(TAG, "writeAutoFolderingInfo : " + this.mContainer + ", " + foldername + ", " + pkgName);
    }

    String getPrefsHomeInstallerPackage() {
        return this.mPrefs.getString("Home_Installer_Package", "");
    }

    String getPrefsAppsInstallerPackage() {
        return this.mPrefs.getString("Apps_Installer_Package", "");
    }

    String getPrefsHomeAutoFolderName() {
        return this.mPrefs.getString("Home_AutoFolderingFolderName", "");
    }

    String getPrefsAppsAutoFolderName() {
        return this.mPrefs.getString("Apps_AutoFolderingFolderName", "");
    }

    public void removePreloadedFolderId(String foldername) {
        Editor editor = this.mPrefs.edit();
        String pref_key = getContainerKey(foldername, PREFS_PRELOADED_FOLDER_ID);
        editor.remove(pref_key);
        editor.apply();
        Log.i(TAG, "removePreloadedFolderId : " + pref_key);
    }

    private String getFolderKey(String foldername, boolean isReadyId) {
        return getContainerKey(foldername, isReadyId ? PREFS_FOLDER_READY_ID : PREFS_FOLDER_ID);
    }

    public long getFolderId(String foldername, boolean isReadyId) {
        return this.mPrefs.getLong(getFolderKey(foldername, isReadyId), -1);
    }

    public String getFolderName(long id) {
        return this.mPrefs.getString(getContainerKey("" + id, PREFS_FOLDER_NAME), null);
    }

    public void writeFolderId(String foldername, long id, boolean isReadyId) {
        String key = getFolderKey(foldername, isReadyId);
        Editor editor = this.mPrefs.edit();
        editor.putLong(key, id);
        editor.apply();
        if (!(isReadyId || id == REMOVED)) {
            editor.putString(getContainerKey("" + id, PREFS_FOLDER_NAME), foldername);
            editor.apply();
        }
        Log.i(TAG, "writeFolderId : " + foldername + ", " + id);
    }

    public void removeFolderId(String foldername, boolean isReadyId) {
        String key = getFolderKey(foldername, isReadyId);
        Editor editor = this.mPrefs.edit();
        editor.remove(key);
        editor.apply();
        Log.i(TAG, "removeFolderId : " + key);
    }

    public void removeKey(String key) {
        Editor editor = this.mPrefs.edit();
        editor.remove(key);
        editor.apply();
        Log.i(TAG, "removeKey : " + key);
    }

    String getOMCPath() {
        String omc_path = this.mPrefs.getString(PREFS_OMC_PATH, "");
        if (!omc_path.isEmpty()) {
            return omc_path;
        }
        omc_path = LauncherFeature.getOmcPath();
        writeOMCPath();
        return omc_path;
    }

    void writeOMCPath() {
        Editor editor = this.mPrefs.edit();
        editor.putString(PREFS_OMC_PATH, LauncherFeature.getOmcPath());
        editor.apply();
        Log.i(TAG, "writeOMCPath : " + LauncherFeature.getOmcPath());
    }

    void clearRemovedFolderInfo() {
        Editor editor = this.mPrefs.edit();
        Map<String, ?> keyMap = this.mPrefs.getAll();
        for (String key : keyMap.keySet()) {
            if (key.endsWith(PREFS_FOLDER_ID) && keyMap.get(key).equals(Long.valueOf(REMOVED))) {
                editor.remove(key);
                editor.apply();
            }
        }
    }

    void removeItemsInfo(ArrayList<Long> ids) {
        Editor editor = this.mPrefs.edit();
        Map<String, ?> keyMap = this.mPrefs.getAll();
        for (String key : keyMap.keySet()) {
            if ((key.endsWith(PREFS_FOLDER_READY_ID) || key.endsWith(PREFS_PRELOADED_FOLDER_ID) || key.endsWith(PREFS_FOLDER_ID)) && ids.contains(keyMap.get(key))) {
                editor.remove(key);
                editor.apply();
            }
            if (key.endsWith(PREFS_FOLDER_NAME) && TextUtils.isDigitsOnly(key.split("_")[0])) {
                editor.remove(key);
                editor.apply();
            }
        }
    }

    public String getFolderNameById(long id) {
        Map<String, ?> keyMap = this.mPrefs.getAll();
        for (String key : keyMap.keySet()) {
            if (key.endsWith(getContainerKey(null, PREFS_FOLDER_ID)) || (key.endsWith(getContainerKey(null, PREFS_PRELOADED_FOLDER_ID)) && keyMap.get(key).equals(Long.valueOf(id)))) {
                return key.split("_")[0];
            }
        }
        return null;
    }

    public Map<Long, String> getFolderIdList() {
        Map<Long, String> idList = new HashMap();
        Map<String, ?> keyMap = this.mPrefs.getAll();
        for (String key : keyMap.keySet()) {
            if (key.endsWith(getContainerKey(null, PREFS_FOLDER_ID)) || key.endsWith(getContainerKey(null, PREFS_PRELOADED_FOLDER_ID)) || key.endsWith(getContainerKey(null, PREFS_FOLDER_READY_ID))) {
                idList.put((Long) keyMap.get(key), key);
            }
        }
        return idList;
    }
}

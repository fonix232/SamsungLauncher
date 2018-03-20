package com.android.launcher3;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.model.LauncherSettings.Favorites_HomeApps;
import com.android.launcher3.common.model.LauncherSettings.Favorites_Standard;
import com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens;
import com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens_HomeApps;
import com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens_Standard;

public class LauncherProviderID extends ContentProvider {
    private static final String AUTHORITY = "com.sec.android.app.launcher.settings.id";
    private static final String CALL_GRID_SIZE = "gridSize";
    private static final String CALL_PREF_CREATE = "createPref";
    private static final String CALL_PREF_EXISTS = "checkPrefExists";
    private static final String CALL_PREF_INIT = "initPref";
    private static final Uri CONTENT_APPWIDGET_UNBIND_URI = Uri.parse("content://com.sec.android.app.launcher.settings.id/appWidgetUnbind");
    private static final String PARAMETER_INIT_ID = "initId";
    private static final String TABLE_PUBLIC_SCREEN_PREFERENCES = "prefs";
    private static final String TAG = "LauncherFacade::ID";
    private static final String WIDGET_CLS_NAME = "widgetClsName";
    private static final String WIDGET_PKG_NAME = "widgetPkgName";
    private static SQLiteDatabase sDb;
    private LauncherProvider mLauncherProvider;

    private static class SingletonHolder {
        private static final LauncherProviderID sLauncherProviderID = new LauncherProviderID();

        private SingletonHolder() {
        }
    }

    public static LauncherProviderID getInstance() {
        return SingletonHolder.sLauncherProviderID;
    }

    private LauncherProvider getLauncherProvider() {
        if (this.mLauncherProvider == null) {
            this.mLauncherProvider = LauncherAppState.getLauncherProvider();
        }
        return this.mLauncherProvider;
    }

    public static void setDatabase(SQLiteDatabase db) {
        if (sDb == null) {
            sDb = db;
        }
    }

    public boolean onCreate() {
        if (LauncherFeature.supportSprintExtension()) {
            LauncherAppState.setLauncherProviderID(this);
        }
        return true;
    }

    public void createPrefsTable() {
        Log.d(TAG, "[SPRINT] creating pref table");
        sDb.execSQL("CREATE TABLE IF NOT EXISTS prefs(key TEXT PRIMARY KEY, value INTEGER, modified INTEGER );");
    }

    public Cursor query(Uri uri, String[] projection, String where, String[] whereArgs, String sortBy) {
        LauncherProvider provider = getLauncherProvider();
        if (provider != null) {
            return provider.query(uri, projection, where, whereArgs, sortBy);
        }
        return null;
    }

    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        LauncherProvider provider = getLauncherProvider();
        if (provider != null) {
            return provider.update(uri, values, where, whereArgs);
        }
        return 0;
    }

    public String getType(Uri uri) {
        LauncherProvider provider = getLauncherProvider();
        if (provider != null) {
            return provider.getType(uri);
        }
        return null;
    }

    public Uri insert(Uri uri, ContentValues initialValues) {
        LauncherProvider provider = getLauncherProvider();
        if (provider == null) {
            return null;
        }
        initValues(uri, initialValues);
        Uri wUri = handleWidget(sDb, uri, initialValues);
        if (wUri != null) {
            return wUri;
        }
        if (initialValues.containsKey(WIDGET_PKG_NAME)) {
            initialValues.remove(WIDGET_PKG_NAME);
        }
        if (initialValues.containsKey(WIDGET_CLS_NAME)) {
            initialValues.remove(WIDGET_CLS_NAME);
        }
        Integer id = initialValues.getAsInteger("_id");
        if (id == null || id.intValue() >= 0) {
            Log.d(TAG, "About to insert");
            return provider.insert(uri, initialValues);
        }
        Log.e(TAG, "[SPRINT] Error: Unable to get row ID");
        return null;
    }

    private Uri handleWidget(SQLiteDatabase db, Uri uri, ContentValues initialValues) {
        if (initialValues == null || initialValues.getAsInteger("itemType") == null) {
            return null;
        }
        if (initialValues.getAsInteger("itemType").intValue() == 4 && initialValues.containsKey(Favorites.APPWIDGET_ID) && initialValues.getAsInteger(Favorites.APPWIDGET_ID) != null && initialValues.getAsInteger(Favorites.APPWIDGET_ID).intValue() == -1 && initialValues.containsKey(WIDGET_PKG_NAME)) {
            String pkg = initialValues.getAsString(WIDGET_PKG_NAME);
            String cls = initialValues.getAsString(WIDGET_CLS_NAME);
            initialValues.remove(WIDGET_PKG_NAME);
            initialValues.remove(WIDGET_CLS_NAME);
            ComponentName cn = new ComponentName(pkg, cls);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getContext());
            boolean insert = false;
            for (AppWidgetProviderInfo w : appWidgetManager.getInstalledProviders()) {
                if (w.provider.equals(cn)) {
                    insert = true;
                    break;
                }
            }
            if (insert) {
                int appWidgetId = new AppWidgetHost(getContext(), 1024).allocateAppWidgetId();
                initialValues.put(Favorites.APPWIDGET_ID, Integer.valueOf(appWidgetId));
                if (getLauncherProvider() == null) {
                    return null;
                }
                String tableName = getFavoriteTable();
                if (TextUtils.isEmpty(tableName)) {
                    Log.d(TAG, "[SPRINT]Error in getting favorite table Name,widget insertion failed");
                    return null;
                }
                long rowId = LauncherProvider.dbInsertAndCheck(db, tableName, null, initialValues);
                if (rowId <= 0) {
                    return null;
                }
                if (appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn)) {
                    return ContentUris.withAppendedId(uri, rowId);
                }
                Log.e(TAG, "Problem allocating appWidgetId");
            } else {
                Log.d(TAG, "No insertion for widget");
            }
        }
        return null;
    }

    private void initValues(Uri uri, ContentValues initialValues) {
        String initId = uri.getQueryParameter(PARAMETER_INIT_ID);
        if (initId != null && !"true".equals(initId)) {
            Log.d(TAG, "Using old ID on insert");
        } else if (getLauncherProvider() != null && initialValues != null) {
            Integer id = initialValues.getAsInteger("_id");
            if (id != null && id.intValue() == 0) {
                Log.d(TAG, "Generating a new ID");
                long maxId = -1;
                if (!LauncherAppState.getInstance().isHomeOnlyModeEnabled() && !LauncherAppState.getInstance().isEasyModeEnabled()) {
                    maxId = FavoritesProvider.getInstance().generateNewItemId();
                } else if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                    maxId = FavoritesProvider.getInstance().getMaxId(Favorites_HomeApps.TABLE_NAME) + 1;
                } else if (LauncherAppState.getInstance().isEasyModeEnabled()) {
                    if (FavoritesProvider.getInstance().tableExists(Favorites_HomeApps.TABLE_NAME)) {
                        maxId = FavoritesProvider.getInstance().getMaxId(Favorites_HomeApps.TABLE_NAME) + 1;
                    } else {
                        maxId = FavoritesProvider.getInstance().getMaxId(Favorites_Standard.TABLE_NAME) + 1;
                    }
                }
                initialValues.put("_id", Long.valueOf(maxId));
            }
        }
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        LauncherProvider provider = getLauncherProvider();
        if (CONTENT_APPWIDGET_UNBIND_URI.equals(uri)) {
            Log.d(TAG, "Unbinding widget");
            new AppWidgetHost(getContext(), 1024).deleteAppWidgetId(Integer.valueOf(selectionArgs[0]).intValue());
            return 1;
        } else if (provider != null) {
            return provider.delete(uri, selection, selectionArgs);
        } else {
            return 0;
        }
    }

    public Bundle call(String method, String arg, Bundle extras) {
        Bundle res = new Bundle(1);
        boolean z = true;
        switch (method.hashCode()) {
            case -1166462073:
                if (method.equals(CALL_PREF_EXISTS)) {
                    z = true;
                    break;
                }
                break;
            case 268333907:
                if (method.equals(CALL_PREF_INIT)) {
                    z = true;
                    break;
                }
                break;
            case 318032999:
                if (method.equals(CALL_GRID_SIZE)) {
                    z = false;
                    break;
                }
                break;
            case 1369102655:
                if (method.equals(CALL_PREF_CREATE)) {
                    z = true;
                    break;
                }
                break;
        }
        switch (z) {
            case false:
                int[] xy = new int[2];
                Utilities.loadCurrentGridSize(getContext(), xy);
                res.putIntArray(CALL_GRID_SIZE, xy);
                break;
            case true:
                boolean isExist = FavoritesProvider.getInstance().tableExists(TABLE_PUBLIC_SCREEN_PREFERENCES);
                Log.d(TAG, "checkPrefExists: " + isExist);
                res.putBoolean(CALL_PREF_EXISTS, isExist);
                break;
            case true:
                if (sDb != null) {
                    createPrefsTable();
                    initPreferences(getContext());
                    res.putBoolean(CALL_PREF_CREATE, true);
                    break;
                }
                Log.e(TAG, "createPref: Unable to create table");
                res.putBoolean(CALL_PREF_CREATE, false);
                break;
            case true:
                String tableName = getWorkSpaceScreensTable();
                if (TextUtils.isEmpty(tableName)) {
                    Log.e(TAG, "[SPRINT]InitPref: Unable to initialize table");
                    res.putBoolean(CALL_PREF_INIT, false);
                }
                int numScreen_HomeApps = FavoritesProvider.getInstance().getItemCount(tableName);
                if (sDb != null && numScreen_HomeApps != 0) {
                    Log.d(TAG, "[SPRINT] Init pref table ");
                    initPreferences(getContext());
                    res.putBoolean(CALL_PREF_INIT, true);
                    break;
                }
                Log.e(TAG, "[SPRINT]InitPref: Unable to initialize table");
                res.putBoolean(CALL_PREF_INIT, false);
                break;
                break;
        }
        return res;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateScreenCount() {
        /*
        r8 = this;
        r5 = com.android.launcher3.common.model.FavoritesProvider.getInstance();
        r6 = "prefs";
        r5 = r5.tableExists(r6);
        if (r5 != 0) goto L_0x000d;
    L_0x000c:
        return;
    L_0x000d:
        r5 = sDb;
        r5.beginTransaction();
        r5 = sDb;	 Catch:{ Exception -> 0x0070 }
        r6 = "UPDATE prefs SET value=?, modified=? WHERE key=?";
        r4 = r5.compileStatement(r6);	 Catch:{ Exception -> 0x0070 }
        r3 = r8.getWorkSpaceScreensTable();	 Catch:{ Exception -> 0x0070 }
        r5 = android.text.TextUtils.isEmpty(r3);	 Catch:{ Exception -> 0x0070 }
        if (r5 != 0) goto L_0x0068;
    L_0x0024:
        r5 = com.android.launcher3.common.model.FavoritesProvider.getInstance();	 Catch:{ Exception -> 0x0070 }
        r5 = r5.getItemCount(r3);	 Catch:{ Exception -> 0x0070 }
        r0 = (long) r5;	 Catch:{ Exception -> 0x0070 }
        r5 = "LauncherFacade::ID";
        r6 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0070 }
        r6.<init>();	 Catch:{ Exception -> 0x0070 }
        r7 = "[SPRINT] updating count to ";
        r6 = r6.append(r7);	 Catch:{ Exception -> 0x0070 }
        r6 = r6.append(r0);	 Catch:{ Exception -> 0x0070 }
        r6 = r6.toString();	 Catch:{ Exception -> 0x0070 }
        android.util.Log.d(r5, r6);	 Catch:{ Exception -> 0x0070 }
        r5 = 1;
        r4.bindLong(r5, r0);	 Catch:{ Exception -> 0x0070 }
        r5 = 2;
        r6 = java.lang.System.currentTimeMillis();	 Catch:{ Exception -> 0x0070 }
        r4.bindLong(r5, r6);	 Catch:{ Exception -> 0x0070 }
        r5 = 3;
        r6 = "numScreens_HomeApps";
        r4.bindString(r5, r6);	 Catch:{ Exception -> 0x0070 }
        r4.execute();	 Catch:{ Exception -> 0x0070 }
        r4.close();	 Catch:{ Exception -> 0x0070 }
        r5 = sDb;	 Catch:{ Exception -> 0x0070 }
        r5.setTransactionSuccessful();	 Catch:{ Exception -> 0x0070 }
    L_0x0062:
        r5 = sDb;
        r5.endTransaction();
        goto L_0x000c;
    L_0x0068:
        r5 = "LauncherFacade::ID";
        r6 = "[SPRINT]unable to update screen count";
        android.util.Log.d(r5, r6);	 Catch:{ Exception -> 0x0070 }
        goto L_0x0062;
    L_0x0070:
        r2 = move-exception;
        r5 = "LauncherFacade::ID";
        r6 = "[SPRINT] ERROR while updating screen count";
        android.util.Log.e(r5, r6);	 Catch:{ all -> 0x007e }
        r5 = sDb;
        r5.endTransaction();
        goto L_0x000c;
    L_0x007e:
        r5 = move-exception;
        r6 = sDb;
        r6.endTransaction();
        throw r5;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.LauncherProviderID.updateScreenCount():void");
    }

    public void updateScreenIndex() {
        if (FavoritesProvider.getInstance().tableExists(TABLE_PUBLIC_SCREEN_PREFERENCES)) {
            sDb.beginTransaction();
            try {
                SQLiteStatement update = sDb.compileStatement("UPDATE prefs SET value=?, modified=? WHERE key=?");
                long index = (long) Utilities.getHomeDefaultPageKey(getContext(), LauncherFiles.HOME_DEFAULT_PAGE_KEY);
                Log.d(TAG, "[SPRINT] updating index to " + index);
                update.bindLong(1, index);
                update.bindLong(2, System.currentTimeMillis());
                update.bindString(3, "defaultScreen_HomeApps");
                update.execute();
                update.close();
                sDb.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "[SPRINT] ERROR while updating screen index");
            } finally {
                sDb.endTransaction();
            }
        }
    }

    public int getScreenIndex() {
        if (!FavoritesProvider.getInstance().tableExists(TABLE_PUBLIC_SCREEN_PREFERENCES)) {
            Log.d(TAG, "[SPRINT] getScreenIndex() Pref does not exist. Creating one");
            createPrefsTable();
            initPreferences(getContext());
        }
        Cursor c = null;
        try {
            c = sDb.rawQuery("SELECT value FROM prefs WHERE key='defaultScreen_HomeApps'", null);
            if (c == null || !c.moveToNext()) {
                if (c != null) {
                    c.close();
                }
                Log.e(TAG, "[SPRINT] Unable to get screen index. Getting from shared preferences, instead");
                return Utilities.getHomeDefaultPageKey(getContext());
            }
            Log.d(TAG, "[SPRINT] index: " + c.getLong(0));
            int i = (int) c.getLong(0);
            if (c == null) {
                return i;
            }
            c.close();
            return i;
        } catch (Exception e) {
            Log.e(TAG, "[SPRINT] Could not get screen index from Prefs: " + e.getMessage());
            if (c != null) {
                c.close();
            }
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
        }
    }

    public void initPreferences(Context context) {
        int homeDefaultPageKey_HomeApps = Utilities.getHomeDefaultPageKey(context, LauncherFiles.HOME_DEFAULT_PAGE_KEY);
        Log.d(TAG, "[SPRINT] Current Default Page index = " + homeDefaultPageKey_HomeApps);
        String tableName = getWorkSpaceScreensTable();
        if (TextUtils.isEmpty(tableName)) {
            Log.e(TAG, "[SPRINT]Unable to get screen tableName , init failed");
            return;
        }
        int numScreen_HomeApps = FavoritesProvider.getInstance().getItemCount(tableName);
        Log.d(TAG, "[SPRINT] Current Page count = " + numScreen_HomeApps);
        if (FavoritesProvider.getInstance().tableExists(TABLE_PUBLIC_SCREEN_PREFERENCES)) {
            sDb.beginTransaction();
            try {
                SQLiteStatement statement = sDb.compileStatement("insert into prefs values(?, ?, ?)");
                statement.bindString(1, "defaultScreen_HomeApps");
                statement.bindLong(2, (long) homeDefaultPageKey_HomeApps);
                statement.bindLong(3, System.currentTimeMillis());
                statement.execute();
                statement.bindString(1, "numScreens_HomeApps");
                statement.bindLong(2, (long) numScreen_HomeApps);
                statement.bindLong(3, System.currentTimeMillis());
                statement.execute();
                statement.close();
                sDb.setTransactionSuccessful();
            } catch (Exception ex) {
                Log.e(TAG, "[SPRINT] error while init pref table: " + ex.getMessage());
            } finally {
                sDb.endTransaction();
            }
            Log.d(TAG, "[SPRINT] init pref table DONE");
            return;
        }
        Log.d(TAG, "[SPRINT] Pref does not exist. Unable to init");
    }

    public void deleteTable() {
        Log.d(TAG, "[SPRINT] dropping pref table");
        sDb.execSQL("DROP TABLE IF EXISTS prefs");
    }

    private String getFavoriteTable() {
        boolean isHomeOnlyMode = LauncherAppState.getInstance().isHomeOnlyModeEnabled();
        boolean isEasyMode = LauncherAppState.getInstance().isEasyModeEnabled();
        if (!isHomeOnlyMode && !isEasyMode) {
            return "favorites";
        }
        if (isHomeOnlyMode) {
            return Favorites_HomeApps.TABLE_NAME;
        }
        if (FavoritesProvider.getInstance().tableExists(Favorites_HomeApps.TABLE_NAME)) {
            return Favorites_HomeApps.TABLE_NAME;
        }
        return Favorites_Standard.TABLE_NAME;
    }

    private String getWorkSpaceScreensTable() {
        boolean isHomeOnlyMode = LauncherAppState.getInstance().isHomeOnlyModeEnabled();
        boolean isEasyMode = LauncherAppState.getInstance().isEasyModeEnabled();
        if (!isHomeOnlyMode && !isEasyMode) {
            return WorkspaceScreens.TABLE_NAME;
        }
        if (isHomeOnlyMode) {
            return WorkspaceScreens_HomeApps.TABLE_NAME;
        }
        if (FavoritesProvider.getInstance().tableExists(WorkspaceScreens_HomeApps.TABLE_NAME)) {
            return WorkspaceScreens_HomeApps.TABLE_NAME;
        }
        return WorkspaceScreens_Standard.TABLE_NAME;
    }
}

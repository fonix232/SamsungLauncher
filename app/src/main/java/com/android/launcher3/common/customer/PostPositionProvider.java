package com.android.launcher3.common.customer;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherProvider;
import com.android.launcher3.allapps.controller.PostAppsPositioner;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.model.DataProvider;
import com.android.launcher3.home.PostHomePositioner;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;

public class PostPositionProvider extends ContentProvider {
    private static final String ALLOWED_CALLING_APP = "allowedCallingApp";
    private static final String CALL_GRID_SIZE = "gridSize";
    public static final String COL_APPS_ADD = "isAppsAdd";
    private static final String COL_APPS_CELL_ORDER = "appsCellOrder";
    public static final String COL_APPS_FOLDER_NAME = "appsFolderName";
    private static final String COL_APPS_FORCE_ATOZ = "appsForceAtoZ";
    private static final String COL_APPS_INDEX = "appsIndex";
    public static final String COL_APPS_PRELOADED_FOLDER = "isAppsPreloadedFolder";
    public static final String COL_COMPONENT_NAME = "componentName";
    public static final String COL_HOME_ADD = "isHomeAdd";
    public static final String COL_HOME_CELL_X = "homeCellX";
    public static final String COL_HOME_CELL_Y = "homeCellY";
    public static final String COL_HOME_FOLDER_NAME = "homeFolderName";
    public static final String COL_HOME_INDEX = "homeIndex";
    private static final String COL_HOME_NEW_PAGE = "isHomeNewPage";
    public static final String COL_HOME_PRELOADED_FOLDER = "isHomePreloadedFolder";
    private static final String COL_HOME_REPLACE = "isHomeReplace";
    private static final String COL_HOME_SHORTCUT_ICON = "homeShortcutIcon";
    private static final String COL_HOME_SHORTCUT_TITLE = "homeShortcutTitle";
    public static final String COL_HOME_WIDGET_SPAN_X = "homeWidgetSpanX";
    public static final String COL_HOME_WIDGET_SPAN_Y = "homeWidgetSpanY";
    public static final String COL_ITEM_TYPE = "itemType";
    private static final String COL_REMOVE_AFTER_POSITION = "removeAfterPosition";
    private static final String COL_RESULT = "result";
    static final Uri CONTENT_URI = Uri.parse(URL);
    static final String DATABASE_NAME = "postposition.db";
    static final int DATABASE_VERSION = 1;
    private static final boolean DEBUG = true;
    private static final byte IDX_APPS_ADD = (byte) 14;
    private static final byte IDX_APPS_CELL_ORDER = (byte) 18;
    private static final byte IDX_APPS_FOLDER_NAME = (byte) 16;
    private static final byte IDX_APPS_FORCE_ATOZ = (byte) 19;
    private static final byte IDX_APPS_INDEX = (byte) 17;
    private static final byte IDX_APPS_PRELOADED_FOLDER = (byte) 15;
    private static final byte IDX_COMPONENT_NAME = (byte) 0;
    private static final byte IDX_HOME_ADD = (byte) 2;
    private static final byte IDX_HOME_CELL_X = (byte) 8;
    private static final byte IDX_HOME_CELL_Y = (byte) 9;
    private static final byte IDX_HOME_FOLDER_NAME = (byte) 4;
    private static final byte IDX_HOME_INDEX = (byte) 7;
    private static final byte IDX_HOME_NEW_PAGE = (byte) 5;
    private static final byte IDX_HOME_PRELOADED_FOLDER = (byte) 3;
    private static final byte IDX_HOME_REPLACE = (byte) 6;
    private static final byte IDX_ITEM_TYPE = (byte) 1;
    private static final byte IDX_REMOVE_AFTER_POSITION = (byte) 20;
    private static final byte IDX_SHORTCUT_ICON = (byte) 13;
    private static final byte IDX_SHORTCUT_TITLE = (byte) 12;
    private static final byte IDX_WIDGET_SPAN_X = (byte) 10;
    private static final byte IDX_WIDGET_SPAN_Y = (byte) 11;
    static final String PROVIDER_NAME = "com.sec.android.launcher.provider.PostPosition";
    static final String TABLE_NAME = "PositionInfo";
    private static final String TAG = "PostPositionProvider";
    static final String URL = "content://com.sec.android.launcher.provider.PostPosition/PositionInfo";
    private SQLiteDatabase mDB;
    protected DatabaseHelper mDBHelper;
    private PostPositionItemRecord mItemRecord;
    private String[] mWhiteListApps;
    private ArrayList<String> mWidgetList = null;

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, PostPositionProvider.DATABASE_NAME, null, 1);
            DataProvider.setContext(context);
        }

        public void onCreate(SQLiteDatabase db) {
            String CREATE_DB_TABLE = " CREATE TABLE PositionInfo (componentName TEXT PRIMARY KEY NOT NULL, itemType INTEGER DEFAULT 0, isHomeAdd BOOLEAN NOT NULL DEFAULT 0, isHomePreloadedFolder BOOLEAN DEFAULT 0, homeFolderName TEXT, isHomeNewPage BOOLEAN DEFAULT 0, isHomeReplace BOOLEAN DEFAULT 0, homeIndex INTEGER DEFAULT -1, homeCellX INTEGER DEFAULT -1, homeCellY INTEGER DEFAULT -1, homeWidgetSpanX INTEGER DEFAULT -1, homeWidgetSpanY INTEGER DEFAULT -1, homeShortcutTitle TEXT, homeShortcutIcon BLOB, isAppsAdd BOOLEAN NOT NULL DEFAULT 0, isAppsPreloadedFolder BOOLEAN DEFAULT 0, appsFolderName TEXT, appsIndex INTEGER DEFAULT -1, appsCellOrder INTEGER DEFAULT -1, appsForceAtoZ BOOLEAN DEFAULT 0, removeAfterPosition BOOLEAN DEFAULT 0, result BOOLEAN DEFAULT 0)";
            db.execSQL(" CREATE TABLE PositionInfo (componentName TEXT PRIMARY KEY NOT NULL, itemType INTEGER DEFAULT 0, isHomeAdd BOOLEAN NOT NULL DEFAULT 0, isHomePreloadedFolder BOOLEAN DEFAULT 0, homeFolderName TEXT, isHomeNewPage BOOLEAN DEFAULT 0, isHomeReplace BOOLEAN DEFAULT 0, homeIndex INTEGER DEFAULT -1, homeCellX INTEGER DEFAULT -1, homeCellY INTEGER DEFAULT -1, homeWidgetSpanX INTEGER DEFAULT -1, homeWidgetSpanY INTEGER DEFAULT -1, homeShortcutTitle TEXT, homeShortcutIcon BLOB, isAppsAdd BOOLEAN NOT NULL DEFAULT 0, isAppsPreloadedFolder BOOLEAN DEFAULT 0, appsFolderName TEXT, appsIndex INTEGER DEFAULT -1, appsCellOrder INTEGER DEFAULT -1, appsForceAtoZ BOOLEAN DEFAULT 0, removeAfterPosition BOOLEAN DEFAULT 0, result BOOLEAN DEFAULT 0)");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS PositionInfo");
            onCreate(db);
        }
    }

    public enum ITEM_TYPE {
        APP,
        WIDGET,
        SHORTCUT
    }

    void resetItem(String cmpName) {
        this.mDB.execSQL("UPDATE PositionInfo SET result=0 WHERE componentName='" + cmpName + "'");
    }

    PostPositionItemRecord getItemRecord(String cmpName, ITEM_TYPE itemType) {
        if (this.mDB == null) {
            Log.i(TAG, "getItemRecord(), database isn't ready state.");
            return null;
        }
        Cursor cursor = null;
        if (this.mWidgetList == null) {
            this.mWidgetList = new ArrayList();
            try {
                String selection = "itemType=" + ITEM_TYPE.WIDGET.ordinal() + " AND " + COL_RESULT + "=0";
                cursor = this.mDB.query(TABLE_NAME, new String[]{COL_COMPONENT_NAME}, selection, null, null, null, null);
                if (cursor != null && cursor.moveToNext()) {
                    this.mWidgetList.add(cursor.getString(0));
                }
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "hasItem widget : " + e.getMessage());
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        try {
            if (itemType == ITEM_TYPE.WIDGET) {
                selection = "componentName LIKE '" + cmpName + "%' AND " + "itemType" + "=" + itemType.ordinal();
            } else {
                selection = "componentName='" + cmpName + "' AND " + "itemType" + "=" + itemType.ordinal();
            }
            cursor = this.mDB.query(TABLE_NAME, null, selection + " AND result=0", null, null, null, null);
            this.mItemRecord = null;
            if (cursor != null && cursor.moveToNext()) {
                if (cursor.getInt(2) > 0 || cursor.getInt(14) > 0) {
                    this.mItemRecord = getItemRecordFromCusor(cursor);
                    if (!this.mItemRecord.isValid()) {
                        this.mItemRecord = null;
                        cursor.close();
                        if (cursor != null) {
                            cursor.close();
                        }
                        return null;
                    }
                }
                this.mItemRecord = null;
                cursor.close();
                if (cursor != null) {
                    cursor.close();
                }
                return null;
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e2) {
            Log.e(TAG, "hasItem : " + e2.getMessage());
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable th2) {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (this.mItemRecord != null) {
            Log.i(TAG, "getItemRecord() : " + this.mItemRecord.getComponentName());
        }
        return this.mItemRecord;
    }

    public void updateItemRecordResult(boolean result, String componentName) {
        try {
            this.mDB.execSQL("UPDATE PositionInfo SET result=" + (result ? 1 : 0) + " WHERE " + COL_COMPONENT_NAME + "='" + componentName + "'");
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Disk is full, updateItemRecordResult failed : " + e);
        }
    }

    boolean hasItemRecord() {
        Cursor cursor = null;
        boolean ret = false;
        try {
            cursor = this.mDB.query(TABLE_NAME, null, "result=0", null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                ret = true;
            }
            if (!(cursor == null || cursor.isClosed())) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "hasItemRecord : " + e.getMessage());
            if (!(cursor == null || cursor.isClosed())) {
                cursor.close();
            }
        } catch (Throwable th) {
            if (!(cursor == null || cursor.isClosed())) {
                cursor.close();
            }
        }
        return ret;
    }

    PostPositionItemRecord[] getItemRecordsNeedToPosition(String packageName) {
        PostPositionItemRecord[] items = null;
        Cursor cursor = null;
        try {
            String selection = "result=0";
            if (!TextUtils.isEmpty(packageName)) {
                selection = selection + " AND componentName LIKE '" + packageName + "/%'";
            }
            cursor = this.mDB.query(TABLE_NAME, null, selection, null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                items = new PostPositionItemRecord[cursor.getCount()];
                int cnt = 0;
                while (cursor.moveToNext()) {
                    items[cnt] = getItemRecordFromCusor(cursor);
                    cnt++;
                }
            }
            if (!(cursor == null || cursor.isClosed())) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "getItemRecordsNeedToPosition : " + e.getMessage());
            if (!(cursor == null || cursor.isClosed())) {
                cursor.close();
            }
        } catch (Throwable th) {
            if (!(cursor == null || cursor.isClosed())) {
                cursor.close();
            }
        }
        return items;
    }

    private PostPositionItemRecord getItemRecordFromCusor(Cursor c) {
        boolean z;
        boolean z2 = true;
        PostPositionItemRecord itemRecord = new PostPositionItemRecord(c.getString(0), c.getInt(1));
        itemRecord.setHomeAdded(c.getInt(2) > 0);
        if (c.getInt(3) > 0) {
            z = true;
        } else {
            z = false;
        }
        itemRecord.setHomePreloadFolder(z);
        itemRecord.setHomeFolderName(c.getString(4));
        if (c.getInt(5) > 0) {
            z = true;
        } else {
            z = false;
        }
        itemRecord.setHomePosition(z, c.getInt(6) > 0, c.getInt(7), c.getInt(8), c.getInt(9));
        itemRecord.setWidgetSpanXY(c.getInt(10), c.getInt(11));
        if (itemRecord.getItemType() == ITEM_TYPE.SHORTCUT) {
            itemRecord.setShortcutInfo(c.getString(12), c.getBlob(13), getContext());
        }
        if (c.getInt(14) > 0) {
            z = true;
        } else {
            z = false;
        }
        itemRecord.setAppsAdded(z);
        if (c.getInt(15) > 0) {
            z = true;
        } else {
            z = false;
        }
        itemRecord.setAppsPreloadFolder(z);
        itemRecord.setAppsFolderName(c.getString(16));
        itemRecord.setAppsPosition(c.getInt(17), c.getInt(18));
        if (c.getInt(19) > 0) {
            z = true;
        } else {
            z = false;
        }
        itemRecord.setAppsForceAtoZ(z);
        if (c.getInt(20) <= 0) {
            z2 = false;
        }
        itemRecord.setRemoveAfterPosition(z2);
        return itemRecord;
    }

    public void disableHomeNewPage(int page) {
        try {
            this.mDB.execSQL("UPDATE PositionInfo SET isHomeNewPage=0  WHERE homeIndex=" + page + " AND " + COL_HOME_NEW_PAGE + "=1");
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Disk is full, disableHomeNewPage failed : " + e);
        }
    }

    boolean isAllowedCallingApp() {
        if (Binder.getCallingUid() != Process.myUid()) {
            if (this.mWhiteListApps == null || this.mWhiteListApps.length == 0) {
                this.mWhiteListApps = getContext().getResources().getStringArray(R.array.sapp_white_list_pkg);
            }
            String callingPackageName = getContext().getPackageManager().getNameForUid(Binder.getCallingUid());
            if (callingPackageName == null) {
                return false;
            }
            boolean isAllowedApp = false;
            for (String equalsIgnoreCase : this.mWhiteListApps) {
                if (equalsIgnoreCase.equalsIgnoreCase(callingPackageName)) {
                    isAllowedApp = true;
                    break;
                }
            }
            if (!isAllowedApp) {
                if (LauncherFeature.isDreamProject() && "com.verizon.mips.services".equalsIgnoreCase(callingPackageName)) {
                    return true;
                }
                Log.d(TAG, "SAPP doesn't alllow to insert item for your app : " + callingPackageName);
                return false;
            }
        }
        return true;
    }

    public boolean onCreate() {
        Log.i(TAG, "Provider onCreated.");
        Context context = getContext();
        LauncherAppState.setApplicationContext(context.getApplicationContext());
        this.mDB = new DatabaseHelper(context).getWritableDatabase();
        PostPositionController ppController = PostPositionController.getInstance(context);
        ppController.setProvider(this);
        ppController.registerPositioner(new PostHomePositioner(context.getApplicationContext(), this));
        ppController.registerPositioner(new PostAppsPositioner(context.getApplicationContext(), this));
        ppController.checkAndEnablePositioner();
        return this.mDB != null;
    }

    public String getType(@NonNull Uri uri) {
        return null;
    }

    public int delete(@NonNull Uri arg0, String arg1, String[] arg2) {
        if (!isAllowedCallingApp()) {
            return 0;
        }
        int count = 0;
        try {
            count = this.mDB.delete(TABLE_NAME, arg1, arg2);
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Disk is full, delete failed : " + e);
        }
        Log.i(TAG, "deleted : " + count);
        return count;
    }

    public Uri insert(@NonNull Uri arg0, ContentValues arg1) {
        if (!isAllowedCallingApp()) {
            return null;
        }
        if (arg1.containsKey(COL_COMPONENT_NAME)) {
            String componentName = arg1.getAsString(COL_COMPONENT_NAME);
            if (ComponentName.unflattenFromString(componentName) == null) {
                Log.e(TAG, "Uncorrect component name is requested." + componentName);
                return null;
            }
            long rowID = 0;
            try {
                rowID = this.mDB.insert(TABLE_NAME, null, arg1);
            } catch (SQLiteFullException e) {
                Log.e(TAG, "Disk is full, insert failed : " + e);
            }
            Log.i(TAG, rowID + ", insert : " + arg1.getAsString(COL_COMPONENT_NAME));
            if (rowID <= 0) {
                return null;
            }
            PostPositionController pp = PostPositionController.getInstance(getContext());
            if (!pp.isEnabled()) {
                pp.checkAndEnablePositioner();
            }
            long typeCode = 0;
            if (arg1.containsKey("itemType")) {
                typeCode = arg1.getAsLong("itemType").longValue();
            }
            if (typeCode == ((long) ITEM_TYPE.WIDGET.ordinal())) {
                if (this.mWidgetList == null) {
                    this.mWidgetList = new ArrayList();
                }
                this.mWidgetList.add(arg1.getAsString(COL_COMPONENT_NAME));
            }
            if (getContext().getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).getBoolean(LauncherProvider.EMPTY_DATABASE_CREATED, false)) {
                return CONTENT_URI;
            }
            PostPositionItemRecord itemRecord = getItemRecord(arg1.getAsString(COL_COMPONENT_NAME), ITEM_TYPE.values()[(int) typeCode]);
            if (itemRecord != null && itemRecord.isHomeAdd()) {
                pp.addItem(itemRecord);
            }
            return CONTENT_URI;
        }
        Log.e(TAG, "componentName key isn't exist!");
        return null;
    }

    public Cursor query(@NonNull Uri arg0, String[] arg1, String arg2, String[] arg3, String arg4) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TABLE_NAME);
        Cursor c = qb.query(this.mDB, arg1, arg2, arg3, null, null, null);
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), arg0);
        }
        return c;
    }

    public int update(@NonNull Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        if (!isAllowedCallingApp()) {
            return 0;
        }
        int count = 0;
        try {
            count = this.mDB.update(TABLE_NAME, arg1, arg2, arg3);
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Disk is full, update failed : " + e);
        }
        Log.i(TAG, "updated : " + count);
        return count;
    }

    public Bundle call(@NonNull String method, String arg, Bundle extras) {
        int i = -1;
        switch (method.hashCode()) {
            case -1649971579:
                if (method.equals(ALLOWED_CALLING_APP)) {
                    i = 0;
                    break;
                }
                break;
            case 318032999:
                if (method.equals(CALL_GRID_SIZE)) {
                    i = 1;
                    break;
                }
                break;
        }
        Bundle res;
        switch (i) {
            case 0:
                res = new Bundle(1);
                res.putBoolean(ALLOWED_CALLING_APP, isAllowedCallingApp());
                return res;
            case 1:
                xy = new int[2];
                DeviceProfile dp = LauncherAppState.getInstance().getDeviceProfile();
                xy[0] = dp.homeGrid.getCellCountX();
                xy[1] = dp.homeGrid.getCellCountY();
                res = new Bundle(1);
                res.putIntArray(CALL_GRID_SIZE, xy);
                return res;
            default:
                return null;
        }
    }

    public long dbInsertOrUpdate(ContentValues values) {
        if (values != null) {
            if (values.getAsString(COL_COMPONENT_NAME) != null) {
                boolean update = false;
                String cn = values.getAsString(COL_COMPONENT_NAME);
                Cursor cursor = this.mDB.query(TABLE_NAME, new String[]{COL_COMPONENT_NAME}, "componentName='" + cn + "'", null, null, null, null);
                if (cursor != null) {
                    update = cursor.getCount() > 0;
                    if (!cursor.isClosed()) {
                        cursor.close();
                    }
                }
                long rtn = 0;
                if (update) {
                    Log.d(TAG, "dbInsertAndCheck : updated - " + cn);
                    try {
                        rtn = (long) this.mDB.update(TABLE_NAME, values, "componentName=?", new String[]{cn});
                    } catch (SQLiteFullException e) {
                        Log.e(TAG, "Disk is full, dbInsertOrUpdate, update failed : " + e);
                    }
                    return rtn;
                }
                Log.d(TAG, "dbInsertAndCheck : inserted - " + cn);
                try {
                    rtn = this.mDB.insert(TABLE_NAME, null, values);
                } catch (SQLiteFullException e2) {
                    Log.e(TAG, "Disk is full, insert failed : " + e2);
                }
                return rtn;
            }
        }
        throw new RuntimeException("Error : attempting to insert null values");
    }
}

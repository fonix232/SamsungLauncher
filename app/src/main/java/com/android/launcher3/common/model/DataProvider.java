package com.android.launcher3.common.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.android.launcher3.common.model.DefaultLayoutParser.LayoutParserCallback;
import com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens;
import java.util.ArrayList;
import java.util.List;

public abstract class DataProvider implements LayoutParserCallback {
    static final int BASE_MIGRATION_VERSION = 30;
    private static final String TAG = "DataProvider";
    protected static Context sContext;
    static SQLiteDatabase sDb;
    long mMaxItemId = -1;

    interface DataInterface {
        void checkId(String str, ContentValues contentValues);

        boolean checkTable();

        void createTable(long j);

        List<Long> deleteEmptyFolders();

        void deleteTable();

        ArrayList<Long> loadScreensFromDb();

        boolean migrateTable(long j, int i, int i2);
    }

    public static void setContext(Context context) {
        if (sContext != null) {
            Log.w(TAG, "DataProvider called twice! old context =" + sContext + " new context =" + context);
        }
        sContext = context;
    }

    public static void setDatabase(SQLiteDatabase db) {
        if (sDb == null) {
            sDb = db;
        }
    }

    public long initializeMaxItemId(String table) {
        return getMaxId(table);
    }

    public long getMaxItemId() {
        return this.mMaxItemId;
    }

    public void setMaxItemId(long id) {
        this.mMaxItemId = id;
    }

    public synchronized long generateNewItemId() {
        if (this.mMaxItemId < 0) {
            throw new RuntimeException("Error: max item id was not initialized");
        }
        this.mMaxItemId++;
        return this.mMaxItemId;
    }

    public long getMaxId(String table) {
        return getMaxId(table, true);
    }

    public long getMaxId(String table, boolean throwException) {
        Cursor c = sDb.query(table, new String[]{"MAX(_id)"}, null, null, null, null, null);
        long id = -1;
        if (c != null && c.moveToNext()) {
            id = c.getLong(0);
        }
        if (c != null) {
            c.close();
        }
        if (id == -1) {
            printDBLog();
            if (throwException) {
                throw new RuntimeException("Error: could not query max id in " + table);
            }
        }
        return id;
    }

    private void printDBLog() {
        String[] selectionArgs = new String[]{String.valueOf(-100)};
        String[] columns = new String[]{"_id", "title", "screen", "intent"};
        Cursor c = sDb.query("favorites", columns, "container = ?", selectionArgs, null, null, null);
        if (c == null) {
            Log.e(TAG, "getMaxId : cursor is null");
        } else {
            while (c.moveToNext()) {
                try {
                    long id = c.getLong(0);
                    String title = c.getString(1);
                    long screenId = c.getLong(2);
                    Log.e(TAG, "id = " + id + " screenId = " + screenId + " title = " + title + " intent = " + c.getString(3) + "\n");
                } catch (Exception e) {
                    Log.e(TAG, "Exception e = " + e);
                } finally {
                    c.close();
                }
            }
        }
        c = sDb.query(WorkspaceScreens.TABLE_NAME, new String[]{"_id", WorkspaceScreens.SCREEN_RANK}, null, null, null, null, null);
        if (c == null) {
            Log.e(TAG, "getMaxId : cursor is null");
            return;
        }
        while (c.moveToNext()) {
            try {
                long id = c.getLong(0);
                Log.e(TAG, "id = " + id + " screenRank = " + c.getLong(1) + "\n");
            } catch (Exception e2) {
                Log.e(TAG, "Exception e = " + e2);
                return;
            } finally {
                c.close();
            }
        }
    }
}

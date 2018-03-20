package com.android.launcher3;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.allapps.AppsBnrHelper;
import com.android.launcher3.common.bnr.LauncherBnrCallBack;
import com.android.launcher3.common.bnr.LauncherBnrHelper;
import com.android.launcher3.common.bnr.google.LauncherBackupAgentHelper;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.model.DataProvider;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.model.LauncherSettings.ChangeLogColumns;
import com.android.launcher3.common.model.LauncherSettings.Settings;
import com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens;
import com.android.launcher3.home.HomeBnrHelper;
import com.android.launcher3.home.ManagedProfileHeuristic;
import com.android.launcher3.home.SessionCommitReceiver;
import com.android.launcher3.remoteconfiguration.RemoteConfigurationManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

public class LauncherProvider extends ContentProvider {
    public static final String AUTHORITY = "com.sec.android.app.launcher.settings";
    private static final int DATABASE_VERSION = 30;
    public static final String EMPTY_DATABASE_CREATED = "EMPTY_DATABASE_CREATED";
    public static final String TABLE_FAVORITES = "favorites";
    private static final String TABLE_WORKSPACE_SCREENS = "workspaceScreens";
    private static final String TAG = "LauncherProvider";
    private LauncherProviderChangeListener mListener;
    private DatabaseHelper mOpenHelper;
    private RemoteConfigurationManager mRemoteConfigurationManager;

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private final Context mContext;
        private final FavoritesProvider mFavoritesProvider;
        private boolean mIsDownGrade = false;
        private LauncherProviderID mLauncherProviderID;
        private LauncherProviderChangeListener mListener;

        DatabaseHelper(Context context) {
            super(context, LauncherFiles.LAUNCHER_DB, null, 30);
            this.mContext = context;
            DataProvider.setContext(context);
            this.mFavoritesProvider = FavoritesProvider.getInstance();
            if (LauncherFeature.supportSprintExtension()) {
                this.mLauncherProviderID = LauncherProviderID.getInstance();
            }
            SQLiteDatabase db = getWritableDatabase();
            DataProvider.setDatabase(db);
            if (this.mLauncherProviderID != null) {
                LauncherProviderID launcherProviderID = this.mLauncherProviderID;
                LauncherProviderID.setDatabase(db);
            }
            if (this.mFavoritesProvider.checkTable()) {
                if (this.mFavoritesProvider.getMaxItemId() == -1) {
                    this.mFavoritesProvider.setMaxItemId(this.mFavoritesProvider.initializeMaxItemId("favorites"));
                }
                if (this.mFavoritesProvider.getMaxScreenId() == -1) {
                    long maxScreenId = this.mFavoritesProvider.getMaxId("workspaceScreens", false);
                    if (maxScreenId == -1) {
                        migrationScreenTableByFavoriteTable(db);
                        throw new RuntimeException("Error: could not query max id in workspaceScreens");
                    }
                    this.mFavoritesProvider.setMaxScreenId(maxScreenId);
                }
                if (this.mIsDownGrade) {
                    throw new RuntimeException("DB version DownGrade error!");
                }
                return;
            }
            createEmptyDB(db);
            throw new RuntimeException("DB table checking error!");
        }

        public void onCreate(SQLiteDatabase db) {
            Log.d(LauncherProvider.TAG, "creating new launcher database");
            DataProvider.setDatabase(db);
            if (this.mLauncherProviderID != null) {
                LauncherProviderID launcherProviderID = this.mLauncherProviderID;
                LauncherProviderID.setDatabase(db);
            }
            this.mFavoritesProvider.setMaxItemId(1);
            this.mFavoritesProvider.setMaxScreenId(0);
            this.mFavoritesProvider.createTable(UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(UserHandleCompat.myUserHandle()));
            if (this.mLauncherProviderID != null) {
                this.mLauncherProviderID.createPrefsTable();
            }
            this.mFavoritesProvider.deleteWidgetHost(this.mListener);
            this.mFavoritesProvider.setMaxItemId(this.mFavoritesProvider.initializeMaxItemId("favorites"));
            setFlagEmptyDbCreated();
            ManagedProfileHeuristic.processAllUsers(Collections.emptyList(), this.mContext);
        }

        private void setFlagEmptyDbCreated() {
            this.mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit().putBoolean(LauncherProvider.EMPTY_DATABASE_CREATED, true).apply();
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(LauncherProvider.TAG, "onUpgrade triggered: " + oldVersion + " to " + newVersion);
            DataProvider.setDatabase(db);
            if (!this.mFavoritesProvider.migrateTable(UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(UserHandleCompat.myUserHandle()), oldVersion, newVersion)) {
                resetSharedPref();
                createEmptyDB(db);
            }
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(LauncherProvider.TAG, "Database version downgrade from: " + oldVersion + " to " + newVersion + ". Wiping database.");
            resetSharedPref();
            DataProvider.setDatabase(db);
            createEmptyDB(db);
            this.mIsDownGrade = true;
        }

        void createEmptyDB(SQLiteDatabase db) {
            this.mFavoritesProvider.deleteTable();
            if (this.mLauncherProviderID != null) {
                this.mLauncherProviderID.deleteTable();
            }
            onCreate(db);
        }

        private void resetSharedPref() {
            Editor editor = this.mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
            editor.clear();
            editor.apply();
        }

        private void migrationScreenTableByFavoriteTable(SQLiteDatabase db) {
            int screenCount;
            int i;
            String[] columns = new String[]{"screen"};
            String str = "favorites";
            SQLiteDatabase sQLiteDatabase = db;
            Cursor c = sQLiteDatabase.query(str, columns, "container = ?", new String[]{String.valueOf(-100)}, null, null, "screen");
            ArrayList<Long> screenOrder = new ArrayList();
            while (c != null) {
                try {
                    if (!c.moveToNext()) {
                        break;
                    }
                    long screenId = c.getLong(0);
                    if (screenId >= 0 && !screenOrder.contains(Long.valueOf(screenId))) {
                        screenOrder.add(Long.valueOf(screenId));
                    }
                } catch (Exception e) {
                    Log.e(LauncherProvider.TAG, "Exception e = " + e);
                    db.delete("workspaceScreens", null, null);
                    if (screenOrder.isEmpty()) {
                        screenOrder.add(Long.valueOf(0));
                    }
                    screenCount = screenOrder.size();
                    for (i = 0; i < screenCount; i++) {
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("_id", (Long) screenOrder.get(i));
                        contentValues.put(WorkspaceScreens.SCREEN_RANK, Integer.valueOf(i));
                        db.insert("workspaceScreens", null, contentValues);
                    }
                    if (this.mLauncherProviderID != null) {
                        this.mLauncherProviderID.updateScreenCount();
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            }
            if (c != null) {
                c.close();
            }
            db.delete("workspaceScreens", null, null);
            if (screenOrder.isEmpty()) {
                screenOrder.add(Long.valueOf(0));
            }
            screenCount = screenOrder.size();
            for (i = 0; i < screenCount; i++) {
                ContentValues contentValues2 = new ContentValues();
                contentValues2.put("_id", (Long) screenOrder.get(i));
                contentValues2.put(WorkspaceScreens.SCREEN_RANK, Integer.valueOf(i));
                db.insert("workspaceScreens", null, contentValues2);
            }
            if (this.mLauncherProviderID != null) {
                this.mLauncherProviderID.updateScreenCount();
            }
        }
    }

    public static class SqlArguments {
        public final String[] args;
        public final String table;
        public final String where;

        public SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = (String) url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (TextUtils.isEmpty(where)) {
                this.table = (String) url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            } else {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                this.table = (String) url.getPathSegments().get(0);
                this.where = null;
                this.args = null;
                return;
            }
            throw new IllegalArgumentException("Invalid URI: " + url);
        }
    }

    public boolean onCreate() {
        Log.i(TAG, "LauncherAccessTestStart: " + System.currentTimeMillis());
        Context context = getContext();
        ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
        LauncherFeature.init(context);
        LauncherAppState.setApplicationContext(context.getApplicationContext());
        LauncherAppState.setLauncherProvider(this);
        this.mOpenHelper = new DatabaseHelper(context);
        StrictMode.setThreadPolicy(oldPolicy);
        Log.i(TAG, "mDesktopModeManager.isDesktopMode() = " + Utilities.isDeskTopMode(context));
        this.mRemoteConfigurationManager = new RemoteConfigurationManager(getContext().getApplicationContext());
        registerBnrCallBack();
        SessionCommitReceiver.applyDefaultUserPrefs(getContext());
        return true;
    }

    public void setLauncherProviderChangeListener(LauncherProviderChangeListener listener) {
        this.mListener = listener;
        this.mOpenHelper.mListener = this.mListener;
        this.mRemoteConfigurationManager.setLauncherProviderChangeListener(listener);
    }

    public String getType(Uri uri) {
        SqlArguments args = new SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(args.where)) {
            return "vnd.android.cursor.dir/" + args.table;
        }
        return "vnd.android.cursor.item/" + args.table;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);
        Cursor result = qb.query(this.mOpenHelper.getWritableDatabase(), projection, args.where, args.args, null, null, sortOrder);
        if (result != null) {
            result.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return result;
    }

    public static long dbInsertAndCheck(SQLiteDatabase db, String table, String nullColumnHack, ContentValues values) {
        if (values == null) {
            throw new RuntimeException("Error: attempting to insert null values");
        } else if (values.containsKey("_id")) {
            FavoritesProvider.getInstance().checkId(table, values);
            long ret = 0;
            try {
                ret = db.insert(table, nullColumnHack, values);
            } catch (SQLiteFullException e) {
                Log.e(TAG, "Disk is full, insert failed : " + e);
            }
            return ret;
        } else {
            throw new RuntimeException("Error: attempting to add item without specifying an id");
        }
    }

    private void reloadLauncherIfExternal() {
        if (Binder.getCallingPid() != Process.myPid()) {
            LauncherAppState app = LauncherAppState.getInstanceNoCreate();
            if (app != null) {
                app.reloadWorkspace();
            }
        }
    }

    private void registerBnrCallBack() {
        ArrayList<LauncherBnrCallBack> callback = new ArrayList(2);
        callback.add(new HomeBnrHelper());
        callback.add(new AppsBnrHelper());
        LauncherBnrHelper.registerBnrCallBack(callback);
    }

    public Uri insert(Uri uri, ContentValues initialValues) {
        SqlArguments args = new SqlArguments(uri);
        if (Binder.getCallingPid() != Process.myPid() && !FavoritesProvider.getInstance().initializeExternalAdd(initialValues)) {
            return null;
        }
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        addModifiedTime(initialValues);
        long rowId = dbInsertAndCheck(db, args.table, null, initialValues);
        if (rowId < 0) {
            return null;
        }
        uri = ContentUris.withAppendedId(uri, rowId);
        notifyListeners();
        sendNotify(uri);
        return uri;
    }

    public int bulkInsert(Uri uri, ContentValues[] values) {
        SqlArguments args = new SqlArguments(uri);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ContentValues value : values) {
                addModifiedTime(value);
                if (dbInsertAndCheck(db, args.table, null, value) < 0) {
                    return 0;
                }
            }
            db.setTransactionSuccessful();
            db.endTransaction();
            notifyListeners();
            sendNotify(uri);
            return values.length;
        } finally {
            db.endTransaction();
        }
    }

    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        ContentProviderResult[] applyBatch;
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            applyBatch = super.applyBatch(operations);
            db.setTransactionSuccessful();
            reloadLauncherIfExternal();
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Disk is full, applyBatch failed : " + e);
            applyBatch = null;
        } finally {
            db.endTransaction();
        }
        return applyBatch;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        int count = 0;
        try {
            count = this.mOpenHelper.getWritableDatabase().delete(args.table, args.where, args.args);
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Disk is full, delete failed : " + e);
        }
        if (count > 0) {
            notifyListeners();
        }
        sendNotify(uri);
        return count;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        addModifiedTime(values);
        int count = 0;
        try {
            count = this.mOpenHelper.getWritableDatabase().update(args.table, values, args.where, args.args);
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Disk is full, update failed : " + e);
        }
        if (count > 0) {
            notifyListeners();
        }
        sendNotify(uri);
        return count;
    }

    public Bundle call(String method, String arg, Bundle extras) {
        int i = -1;
        if (Binder.getCallingUid() != Process.myUid()) {
            switch (method.hashCode()) {
                case 2008800394:
                    if (method.equals("appWidgetReset")) {
                        i = 0;
                        break;
                    }
                    break;
            }
            switch (i) {
                case 0:
                    if (LauncherFeature.supportSprintExtension()) {
                        if (this.mListener != null) {
                            Log.d(TAG, "[SPRINT] Resetting App Widget Listener");
                            this.mListener.onAppWidgetHostReset();
                        } else {
                            Log.d(TAG, "[SPRINT] App Widget Listener is null");
                        }
                        Bundle result = new Bundle();
                        result.putString(arg, "SUCCESS");
                        return result;
                    }
                    break;
            }
            return this.mRemoteConfigurationManager.handleRemoteConfigurationCall(method, arg, extras);
        }
        result = new Bundle();
        switch (method.hashCode()) {
            case -1839494009:
                if (method.equals("getDataBaseVersion")) {
                    i = 2;
                    break;
                }
                break;
            case -1803226544:
                if (method.equals(Settings.METHOD_GET_BOOLEAN)) {
                    i = 0;
                    break;
                }
                break;
            case 948012892:
                if (method.equals(Settings.METHOD_SET_BOOLEAN)) {
                    i = 1;
                    break;
                }
                break;
        }
        switch (i) {
            case 0:
                result.putBoolean("value", getContext().getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).getBoolean(arg, extras.getBoolean(Settings.EXTRA_DEFAULT_VALUE)));
                return result;
            case 1:
                boolean value = extras.getBoolean("value");
                getContext().getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit().putBoolean(arg, value).apply();
                if (this.mListener != null) {
                    if (Utilities.HOMESCREEN_MODE_PREFERENCE_KEY.equals(arg)) {
                        value = false;
                    }
                    this.mListener.onSettingsChanged(arg, value);
                }
                result.putBoolean("value", value);
                return result;
            case 2:
                if (!"getDataBaseVersion".equals(method)) {
                    return result;
                }
                result.putInt("DBVersion", 30);
                return result;
            default:
                return null;
        }
    }

    private void notifyListeners() {
        LauncherBackupAgentHelper.dataChanged(getContext());
    }

    private static void addModifiedTime(ContentValues values) {
        values.put(ChangeLogColumns.MODIFIED, Long.valueOf(System.currentTimeMillis()));
    }

    public synchronized void createEmptyDB() {
        this.mOpenHelper.createEmptyDB(this.mOpenHelper.getWritableDatabase());
    }

    public void clearFlagEmptyDbCreated() {
        getContext().getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit().remove(EMPTY_DATABASE_CREATED).apply();
    }

    private void sendNotify(Uri uri) {
        if (LauncherFeature.supportSprintExtension()) {
            String notify = uri.getQueryParameter("notify");
            if (notify == null || "true".equals(notify)) {
                reloadLauncherIfExternal();
                return;
            }
            return;
        }
        reloadLauncherIfExternal();
    }

    public RemoteConfigurationManager getRemoteConfigurationManager() {
        return this.mRemoteConfigurationManager;
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        LauncherAppState appState = LauncherAppState.getInstanceNoCreate();
        if (appState != null && appState.getModel().isModelIdle()) {
            appState.getModel().dumpState("", fd, writer, args);
        }
    }
}

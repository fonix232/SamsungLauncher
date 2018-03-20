package com.android.launcher3.common.model;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.customer.PostPositionProvider;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.util.BitmapUtils;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.MainThreadExecutor;
import com.android.launcher3.util.TestHelper;
import com.sec.android.app.launcher.R;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;

public class IconCache {
    private static final boolean DEBUG = false;
    private static final String EMPTY_CLASS_NAME = ".";
    private static final float ICON_SIZE_DEFINED_IN_APP_DP = 48.0f;
    private static final Object ICON_UPDATE_TOKEN = new Object();
    private static final int INITIAL_ICON_CACHE_CAPACITY = 50;
    private static final int LOW_RES_SCALE_FACTOR = 5;
    private static final String TAG = "Launcher.IconCache";
    private boolean isIconDpiChanged = false;
    private final int mActivityBgColor;
    private final HashMap<ComponentKey, CacheEntry> mCache = new HashMap(INITIAL_ICON_CACHE_CAPACITY);
    private final Context mContext;
    private final HashMap<UserHandleCompat, Bitmap> mDefaultIcons = new HashMap();
    private final IconDB mIconDb;
    private int mIconDpi;
    private final LauncherAppsCompat mLauncherApps;
    private Bitmap mLowResBitmap;
    private Canvas mLowResCanvas;
    private final Options mLowResOptions;
    private Paint mLowResPaint;
    private final MainThreadExecutor mMainThreadExecutor = new MainThreadExecutor();
    private final int mPackageBgColor;
    private final PackageManager mPackageManager;
    private Bitmap mSDCardBitmap;
    private String mSystemState;
    private final UserManagerCompat mUserManager;
    private final Handler mWorkerHandler;

    static class CacheEntry {
        public CharSequence contentDescription = "";
        public Bitmap icon;
        public boolean isLowResIcon;
        public CharSequence title = "";

        CacheEntry() {
        }
    }

    static final class IconDB extends SQLiteOpenHelper {
        static final String COLUMN_COMPONENT = "componentName";
        static final String COLUMN_ICON = "icon";
        static final String COLUMN_ICON_LOW_RES = "icon_low_res";
        static final String COLUMN_LABEL = "label";
        static final String COLUMN_LAST_UPDATED = "lastUpdated";
        static final String COLUMN_ROWID = "rowid";
        static final String COLUMN_SYSTEM_STATE = "system_state";
        static final String COLUMN_USER = "profileId";
        static final String COLUMN_VERSION = "version";
        private static final int DB_VERSION = 7;
        static final String TABLE_NAME = "icons";

        public IconDB(Context context) {
            super(context, LauncherFiles.APP_ICONS_DB, null, 7);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS icons (componentName TEXT NOT NULL, profileId INTEGER NOT NULL, lastUpdated INTEGER NOT NULL DEFAULT 0, version INTEGER NOT NULL DEFAULT 0, icon BLOB, icon_low_res BLOB, label TEXT, system_state TEXT, PRIMARY KEY (componentName, profileId) );");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion != newVersion) {
                clearDB(db);
            }
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion != newVersion) {
                clearDB(db);
            }
        }

        private void clearDB(SQLiteDatabase db) {
            try {
                db.beginTransaction();
                db.execSQL("DROP TABLE IF EXISTS icons");
                onCreate(db);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(IconCache.TAG, "ClearDB Failed : " + e.getMessage());
            } finally {
                db.endTransaction();
            }
        }
    }

    public static class IconLoadRequest {
        private final Handler mHandler;
        private final Runnable mRunnable;

        IconLoadRequest(Runnable runnable, Handler handler) {
            this.mRunnable = runnable;
            this.mHandler = handler;
        }

        public void cancel() {
            this.mHandler.removeCallbacks(this.mRunnable);
        }
    }

    class SerializedIconUpdateTask implements Runnable {
        private final Stack<LauncherActivityInfoCompat> mAppsToAdd;
        private final Stack<LauncherActivityInfoCompat> mAppsToUpdate;
        private final HashMap<String, PackageInfo> mPkgInfoMap;
        private final HashSet<String> mUpdatedPackages = new HashSet();
        private final long mUserSerial;

        SerializedIconUpdateTask(long userSerial, HashMap<String, PackageInfo> pkgInfoMap, Stack<LauncherActivityInfoCompat> appsToAdd, Stack<LauncherActivityInfoCompat> appsToUpdate) {
            this.mUserSerial = userSerial;
            this.mPkgInfoMap = pkgInfoMap;
            this.mAppsToAdd = appsToAdd;
            this.mAppsToUpdate = appsToUpdate;
        }

        public void run() {
            PackageInfo info;
            LauncherActivityInfoCompat app;
            if (!this.mAppsToUpdate.isEmpty()) {
                app = (LauncherActivityInfoCompat) this.mAppsToUpdate.pop();
                String cn = app.getComponentName().flattenToString();
                ContentValues values = IconCache.this.updateCacheAndGetContentValues(app, true);
                info = (PackageInfo) this.mPkgInfoMap.get(app.getComponentName().getPackageName());
                if (info != null) {
                    values.put("lastUpdated", Long.valueOf(info.lastUpdateTime));
                    values.put("version", Integer.valueOf(info.versionCode));
                    values.put("system_state", IconCache.this.mSystemState);
                }
                try {
                    IconCache.this.mIconDb.getWritableDatabase().update("icons", values, "componentName = ? AND profileId = ?", new String[]{cn, Long.toString(this.mUserSerial)});
                } catch (SQLiteFullException e) {
                    Log.e(IconCache.TAG, "Disk is full, SerializedIconUpdateTask, update failed : " + e);
                }
                this.mUpdatedPackages.add(app.getComponentName().getPackageName());
                if (this.mAppsToUpdate.isEmpty() && !this.mUpdatedPackages.isEmpty()) {
                    LauncherAppState.getInstance().getModel().onPackageIconsUpdated(this.mUpdatedPackages, IconCache.this.mUserManager.getUserForSerialNumber(this.mUserSerial));
                    IconCache.this.isIconDpiChanged = false;
                }
                scheduleNext();
            } else if (!this.mAppsToAdd.isEmpty()) {
                app = (LauncherActivityInfoCompat) this.mAppsToAdd.pop();
                info = (PackageInfo) this.mPkgInfoMap.get(app.getComponentName().getPackageName());
                if (info != null) {
                    synchronized (IconCache.this) {
                        if (!IconCache.this.isExistIconInDB(new ComponentKey(app.getComponentName(), app.getUser()))) {
                            try {
                                IconCache.this.mPackageManager.getPackageInfo(app.getComponentName().getPackageName(), 8192);
                                IconCache.this.addIconToDBAndMemCache(app, info, this.mUserSerial);
                                this.mUpdatedPackages.add(app.getComponentName().getPackageName());
                            } catch (NameNotFoundException e2) {
                                Log.d(IconCache.TAG, "NameNotFoundException : " + info.packageName);
                                scheduleNext();
                                return;
                            }
                        }
                    }
                    if (this.mAppsToAdd.isEmpty() && !this.mUpdatedPackages.isEmpty()) {
                        LauncherAppState.getInstance().getModel().onPackageIconsUpdated(this.mUpdatedPackages, IconCache.this.mUserManager.getUserForSerialNumber(this.mUserSerial));
                        IconCache.this.isIconDpiChanged = false;
                    }
                }
                if (!this.mAppsToAdd.isEmpty()) {
                    scheduleNext();
                }
            }
            if (this.mAppsToAdd.isEmpty()) {
                Log.d(IconCache.TAG, "IconCache Update finished");
                if (LauncherFeature.supportDeepShortcut()) {
                    IconCache.this.mWorkerHandler.post(new Runnable() {
                        public void run() {
                            LauncherAppState.getInstance().getModel().updateDeepShortcutIcons();
                        }
                    });
                }
            }
        }

        public void scheduleNext() {
            IconCache.this.mWorkerHandler.postAtTime(this, IconCache.ICON_UPDATE_TOKEN, SystemClock.uptimeMillis() + 1);
        }
    }

    public IconCache(Context context, int iconSize) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mUserManager = UserManagerCompat.getInstance(this.mContext);
        this.mLauncherApps = LauncherAppsCompat.getInstance(this.mContext);
        this.mIconDpi = getLauncherIconDensity(iconSize);
        this.mIconDb = new IconDB(context);
        this.mWorkerHandler = new Handler(LauncherModel.getWorkerLooper());
        this.mActivityBgColor = context.getResources().getColor(R.color.quantum_panel_bg_color);
        this.mPackageBgColor = context.getResources().getColor(R.color.quantum_panel_bg_color_dark);
        this.mLowResOptions = new Options();
        this.mLowResOptions.inPreferredConfig = Config.RGB_565;
        updateSystemStateString();
        Log.i(TAG, "IconCache : " + this.mIconDpi);
    }

    private Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(), 17629184);
    }

    private Drawable getFullResIcon(Resources resources, int iconId) {
        Drawable d;
        try {
            d = resources.getDrawableForDensity(iconId, this.mIconDpi);
        } catch (NotFoundException e) {
            d = null;
        }
        return d != null ? d : getFullResDefaultActivityIcon();
    }

    public Bitmap getSDCardBitmap() {
        if (this.mSDCardBitmap == null) {
            this.mSDCardBitmap = Bitmap.createScaledBitmap(BitmapUtils.getBitmap(this.mContext.getResources().getDrawableForDensity(R.drawable.sym_app_on_sd_unavailable_icon, this.mIconDpi)), BitmapUtils.getIconBitmapSize(), BitmapUtils.getIconBitmapSize(), true);
        }
        return this.mSDCardBitmap;
    }

    public Drawable getIconFromCache(String packageName) {
        Intent intent = this.mContext.getPackageManager().getLaunchIntentForPackage(packageName);
        CacheEntry cacheEntry = null;
        if (intent != null) {
            cacheEntry = (CacheEntry) this.mCache.get(new ComponentKey(intent.getComponent(), UserHandleCompat.myUserHandle()));
        }
        if (cacheEntry == null) {
            cacheEntry = (CacheEntry) this.mCache.get(getPackageKey(packageName, UserHandleCompat.myUserHandle()));
        }
        if (cacheEntry != null) {
            return new BitmapDrawable(this.mContext.getResources(), cacheEntry.icon);
        }
        return null;
    }

    public Drawable getFullResIcon(String packageName, int iconId) {
        Drawable icon = getIconFromCache(packageName);
        if (icon != null) {
            return icon;
        }
        Resources resources;
        try {
            resources = this.mPackageManager.getResourcesForApplication(packageName);
        } catch (NameNotFoundException e) {
            resources = null;
        }
        if (resources == null || iconId == 0) {
            return getFullResDefaultActivityIcon();
        }
        return getFullResIcon(resources, iconId);
    }

    public Drawable getFullResIcon(ActivityInfo info) {
        Resources resources;
        try {
            resources = this.mPackageManager.getResourcesForApplication(info.applicationInfo);
        } catch (NameNotFoundException e) {
            resources = null;
        }
        int iconId = info.getIconResource();
        if (resources == null || iconId == 0) {
            return makeDefaultbadgeIcon(getFullResDefaultActivityIcon(), info);
        }
        return makeDefaultbadgeIcon(getFullResIcon(resources, iconId), info);
    }

    private Drawable makeDefaultbadgeIcon(Drawable drawable, ActivityInfo info) {
        if (!LauncherFeature.isSSecureSupported() || Utilities.isKnoxMode()) {
            return drawable;
        }
        Drawable d = info.loadIcon(this.mPackageManager);
        Bitmap b = Bitmap.createBitmap(Math.max(d.getIntrinsicWidth(), 1), Math.max(d.getIntrinsicHeight(), 1), Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, b.getWidth(), b.getHeight());
        d.draw(c);
        c.setBitmap(null);
        return d;
    }

    private Bitmap makeDefaultIcon(UserHandleCompat user) {
        return BitmapUtils.createIconBitmap(this.mUserManager.getBadgedDrawableForUser(getFullResDefaultActivityIcon(), user), this.mContext);
    }

    public synchronized void remove(ComponentName componentName, UserHandleCompat user) {
        this.mCache.remove(new ComponentKey(componentName, user));
    }

    private void removeFromMemCacheLocked(String packageName, UserHandleCompat user) {
        HashSet<ComponentKey> forDeletion = new HashSet();
        synchronized (this.mCache) {
            for (ComponentKey key : this.mCache.keySet()) {
                if (key.componentName != null && key.componentName.getPackageName().equals(packageName) && key.user.equals(user)) {
                    forDeletion.add(key);
                }
            }
            Iterator it = forDeletion.iterator();
            while (it.hasNext()) {
                this.mCache.remove((ComponentKey) it.next());
            }
        }
    }

    public synchronized void updateIconsForPkg(String packageName, UserHandleCompat user) {
        removeIconsForPkg(packageName, user);
        try {
            PackageInfo info = this.mPackageManager.getPackageInfo(packageName, 8192);
            long userSerial = this.mUserManager.getSerialNumberForUser(user);
            for (LauncherActivityInfoCompat app : this.mLauncherApps.getActivityList(packageName, user)) {
                addIconToDBAndMemCache(app, info, userSerial);
            }
        } catch (NameNotFoundException e) {
            Log.d(TAG, "Package not found", e);
        }
        return;
    }

    public synchronized void removeIconsForPkg(String packageName, UserHandleCompat user) {
        removeFromMemCacheLocked(packageName, user);
        long userSerial = this.mUserManager.getSerialNumberForUser(user);
        try {
            this.mIconDb.getWritableDatabase().delete("icons", "componentName LIKE ? AND profileId = ?", new String[]{packageName + "/%", Long.toString(userSerial)});
        } catch (SQLiteFullException e) {
            Log.e(TAG, "Disk is full, removeIconsForPkg failed : " + e);
        }
    }

    public void updateDbIcons(Set<String> ignorePackagesForMainUser) {
        this.mWorkerHandler.removeCallbacksAndMessages(ICON_UPDATE_TOKEN);
        updateSystemStateString();
        for (UserHandleCompat user : this.mUserManager.getUserProfiles()) {
            List<LauncherActivityInfoCompat> apps = this.mLauncherApps.getActivityList(null, user);
            if (apps == null || apps.isEmpty()) {
                Log.d(TAG, "There is no getActivityList apps for user " + user);
            } else {
                Set set;
                if (UserHandleCompat.myUserHandle().equals(user)) {
                    set = ignorePackagesForMainUser;
                } else {
                    set = Collections.emptySet();
                }
                updateDBIcons(user, apps, set);
            }
        }
    }

    private void updateDBIcons(UserHandleCompat user, List<LauncherActivityInfoCompat> apps, Set<String> ignorePackages) {
        LauncherActivityInfoCompat app;
        long userSerial = this.mUserManager.getSerialNumberForUser(user);
        PackageManager pm = this.mContext.getPackageManager();
        HashMap<String, PackageInfo> pkgInfoMap = new HashMap();
        for (PackageInfo info : pm.getInstalledPackages(8192)) {
            PackageInfo info2;
            pkgInfoMap.put(info2.packageName, info2);
        }
        HashMap<ComponentName, LauncherActivityInfoCompat> componentMap = new HashMap();
        for (LauncherActivityInfoCompat app2 : apps) {
            componentMap.put(app2.getComponentName(), app2);
        }
        Cursor c = this.mIconDb.getReadableDatabase().query("icons", new String[]{"rowid", PostPositionProvider.COL_COMPONENT_NAME, "lastUpdated", "version", "system_state"}, "profileId = ? ", new String[]{Long.toString(userSerial)}, null, null, null);
        if (c != null) {
            int indexComponent = c.getColumnIndex(PostPositionProvider.COL_COMPONENT_NAME);
            int indexLastUpdate = c.getColumnIndex("lastUpdated");
            int indexVersion = c.getColumnIndex("version");
            int rowIndex = c.getColumnIndex("rowid");
            int systemStateIndex = c.getColumnIndex("system_state");
            HashSet<Integer> itemsToRemove = new HashSet();
            Stack<LauncherActivityInfoCompat> appsToUpdate = new Stack();
            while (c.moveToNext()) {
                ComponentName component = ComponentName.unflattenFromString(c.getString(indexComponent));
                info2 = (PackageInfo) pkgInfoMap.get(component.getPackageName());
                if (info2 == null) {
                    if (!ignorePackages.contains(component.getPackageName())) {
                        remove(component, user);
                        itemsToRemove.add(Integer.valueOf(c.getInt(rowIndex)));
                    }
                } else if ((info2.applicationInfo.flags & 16777216) == 0) {
                    long updateTime = c.getLong(indexLastUpdate);
                    int version = c.getInt(indexVersion);
                    app2 = (LauncherActivityInfoCompat) componentMap.remove(component);
                    if (this.isIconDpiChanged || version != info2.versionCode || updateTime != info2.lastUpdateTime || !TextUtils.equals(this.mSystemState, c.getString(systemStateIndex))) {
                        if (app2 == null) {
                            remove(component, user);
                            itemsToRemove.add(Integer.valueOf(c.getInt(rowIndex)));
                        } else {
                            appsToUpdate.add(app2);
                        }
                    }
                }
            }
            c.close();
            if (!itemsToRemove.isEmpty()) {
                try {
                    this.mIconDb.getWritableDatabase().delete("icons", Utilities.createDbSelectionQuery("rowid", itemsToRemove), null);
                } catch (SQLiteFullException e) {
                    Log.e(TAG, "Disk is full, updateDBIcons, delete failed : " + e);
                }
            }
            if (!componentMap.isEmpty() || !appsToUpdate.isEmpty()) {
                Stack<LauncherActivityInfoCompat> appsToAdd = new Stack();
                appsToAdd.addAll(componentMap.values());
                new SerializedIconUpdateTask(userSerial, pkgInfoMap, appsToAdd, appsToUpdate).scheduleNext();
            }
        }
    }

    private void addIconToDBAndMemCache(LauncherActivityInfoCompat app, PackageInfo info, long userSerial) {
        addIconToDB(updateCacheAndGetContentValues(app, false), app.getComponentName(), info, userSerial);
    }

    private void addIconToDB(ContentValues values, ComponentName key, PackageInfo info, long userSerial) {
        values.put(PostPositionProvider.COL_COMPONENT_NAME, key.flattenToString());
        values.put(BaseLauncherColumns.PROFILE_ID, Long.valueOf(userSerial));
        values.put("lastUpdated", Long.valueOf(info.lastUpdateTime));
        values.put("version", Integer.valueOf(info.versionCode));
        try {
            this.mIconDb.getWritableDatabase().insertWithOnConflict("icons", null, values, 5);
        } catch (SQLException e) {
            Log.e(TAG, "Unable to write icon to DB", e);
        }
    }

    private ContentValues updateCacheAndGetContentValues(LauncherActivityInfoCompat app, boolean replaceExisting) {
        ComponentKey key = new ComponentKey(app.getComponentName(), app.getUser());
        CacheEntry cacheEntry = null;
        if (!replaceExisting) {
            cacheEntry = (CacheEntry) this.mCache.get(key);
            if (cacheEntry == null || cacheEntry.isLowResIcon || cacheEntry.icon == null) {
                cacheEntry = null;
            }
        }
        if (cacheEntry == null) {
            cacheEntry = new CacheEntry();
        }
        Drawable drawable = getCSCPackageItemIcon(app.getComponentName());
        if (drawable == null) {
            drawable = app.getBadgedIconForIconTray(this.mIconDpi);
        }
        cacheEntry.icon = BitmapUtils.createIconBitmap(drawable, this.mContext);
        cacheEntry.title = getPackageItemTitle(app);
        cacheEntry.contentDescription = this.mUserManager.getBadgedLabelForUser(cacheEntry.title, app.getUser());
        this.mCache.put(new ComponentKey(app.getComponentName(), app.getUser()), cacheEntry);
        return newContentValues(cacheEntry.icon, cacheEntry.title.toString(), this.mActivityBgColor);
    }

    public IconLoadRequest updateIconInBackground(final IconView caller, final ItemInfo info) {
        Runnable request = new Runnable() {
            public void run() {
                if (info instanceof IconInfo) {
                    IconInfo iconInfo = info;
                    IconCache.this.getTitleAndIcon(iconInfo, iconInfo.promisedIntent != null ? iconInfo.promisedIntent : iconInfo.intent, iconInfo.user, false);
                }
                IconCache.this.mMainThreadExecutor.execute(new Runnable() {
                    public void run() {
                        caller.reapplyItemInfoFromIconCache(info);
                    }
                });
            }
        };
        this.mWorkerHandler.post(request);
        return new IconLoadRequest(request, this.mWorkerHandler);
    }

    private Bitmap getNonNullIcon(CacheEntry entry, UserHandleCompat user) {
        return entry.icon == null ? getDefaultIcon(user) : entry.icon;
    }

    public void getTitleAndIcon(IconInfo application, LauncherActivityInfoCompat info, boolean useLowResIcon) {
        UserHandleCompat user = info == null ? application.user : info.getUser();
        CacheEntry entry = cacheLocked(application.componentName, info, user, false, useLowResIcon, application.isDisabled);
        synchronized (entry) {
            application.title = Utilities.trim(entry.title);
            application.mIcon = getNonNullIcon(entry, user);
            application.contentDescription = entry.contentDescription;
            application.usingLowResIcon = entry.isLowResIcon;
        }
    }

    public void updateTitleAndIcon(IconInfo application) {
        CacheEntry entry = cacheLocked(application.componentName, null, application.user, false, application.usingLowResIcon, application.isDisabled);
        synchronized (entry) {
            if (!(entry.icon == null || isDefaultIcon(entry.icon, application.user))) {
                application.title = Utilities.trim(entry.title);
                application.mIcon = entry.icon;
                application.contentDescription = entry.contentDescription;
                application.usingLowResIcon = entry.isLowResIcon;
            }
        }
    }

    public Bitmap getIcon(Intent intent, UserHandleCompat user) {
        ComponentName component = intent.getComponent();
        if (component == null) {
            return getDefaultIcon(user);
        }
        return cacheLocked(component, this.mLauncherApps.resolveActivity(intent, user), user, true, false, 0).icon;
    }

    public void getTitleAndIcon(IconInfo iconInfo, Intent intent, UserHandleCompat user, boolean useLowResIcon) {
        ComponentName component = intent.getComponent();
        if (component == null) {
            iconInfo.setIcon(getDefaultIcon(user));
            iconInfo.title = "";
            iconInfo.usingFallbackIcon = true;
            iconInfo.usingLowResIcon = false;
            return;
        }
        getTitleAndIcon(iconInfo, component, this.mLauncherApps.resolveActivity(intent, user), user, true, useLowResIcon);
    }

    public void getTitleAndIcon(IconInfo iconInfo, ComponentName component, LauncherActivityInfoCompat info, UserHandleCompat user, boolean usePkgIcon, boolean useLowResIcon) {
        CacheEntry entry = cacheLocked(component, info, user, usePkgIcon, useLowResIcon, iconInfo.isDisabled);
        synchronized (entry) {
            iconInfo.setIcon(getNonNullIcon(entry, user));
            iconInfo.title = Utilities.trim(info == null ? entry.title : getPackageItemTitle(info));
            iconInfo.contentDescription = entry.contentDescription;
            iconInfo.usingFallbackIcon = isDefaultIcon(entry.icon, user);
            iconInfo.usingLowResIcon = entry.isLowResIcon;
        }
    }

    public synchronized void getTitleAndIconForApp(String packageName, UserHandleCompat user, boolean useLowResIcon, PackageItemInfo infoOut) {
        CacheEntry entry = getWidgetEntryForPackage(packageName, user, useLowResIcon, 0);
        infoOut.iconBitmap = getNonNullIcon(entry, user);
        infoOut.title = Utilities.trim(entry.title);
        infoOut.usingLowResIcon = entry.isLowResIcon;
        infoOut.contentDescription = entry.contentDescription;
    }

    public synchronized Bitmap getDefaultIcon(UserHandleCompat user) {
        if (!this.mDefaultIcons.containsKey(user)) {
            this.mDefaultIcons.put(user, makeDefaultIcon(user));
        }
        return (Bitmap) this.mDefaultIcons.get(user);
    }

    public boolean isDefaultIcon(Bitmap icon, UserHandleCompat user) {
        return this.mDefaultIcons.get(user) == icon;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private com.android.launcher3.common.model.IconCache.CacheEntry cacheLocked(android.content.ComponentName r15, com.android.launcher3.common.compat.LauncherActivityInfoCompat r16, com.android.launcher3.common.compat.UserHandleCompat r17, boolean r18, boolean r19, int r20) {
        /*
        r14 = this;
        r8 = new com.android.launcher3.util.ComponentKey;
        r0 = r17;
        r8.<init>(r15, r0);
        r10 = 0;
        r2 = r14.mCache;
        monitor-enter(r2);
        r1 = r14.mCache;	 Catch:{ all -> 0x008a }
        r1 = r1.get(r8);	 Catch:{ all -> 0x008a }
        r0 = r1;
        r0 = (com.android.launcher3.common.model.IconCache.CacheEntry) r0;	 Catch:{ all -> 0x008a }
        r10 = r0;
        monitor-exit(r2);	 Catch:{ all -> 0x008a }
        if (r10 == 0) goto L_0x001e;
    L_0x0018:
        r1 = r10.isLowResIcon;
        if (r1 == 0) goto L_0x0071;
    L_0x001c:
        if (r19 != 0) goto L_0x0071;
    L_0x001e:
        r2 = r14.mCache;
        monitor-enter(r2);
        r11 = new com.android.launcher3.common.model.IconCache$CacheEntry;	 Catch:{ all -> 0x008d }
        r11.<init>();	 Catch:{ all -> 0x008d }
        r1 = r14.mCache;	 Catch:{ all -> 0x011e }
        r1.put(r8, r11);	 Catch:{ all -> 0x011e }
        monitor-exit(r2);	 Catch:{ all -> 0x011e }
        monitor-enter(r11);
        r0 = r19;
        r1 = r14.getEntryFromDB(r8, r11, r0);	 Catch:{ all -> 0x011b }
        if (r1 != 0) goto L_0x0051;
    L_0x0035:
        if (r16 == 0) goto L_0x0090;
    L_0x0037:
        r1 = r16.getComponentName();	 Catch:{ all -> 0x011b }
        r9 = r14.getCSCPackageItemIcon(r1);	 Catch:{ all -> 0x011b }
        if (r9 != 0) goto L_0x0049;
    L_0x0041:
        r1 = r14.mIconDpi;	 Catch:{ all -> 0x011b }
        r0 = r16;
        r9 = r0.getBadgedIconForIconTray(r1);	 Catch:{ all -> 0x011b }
    L_0x0049:
        r1 = r14.mContext;	 Catch:{ all -> 0x011b }
        r1 = com.android.launcher3.util.BitmapUtils.createIconBitmap(r9, r1);	 Catch:{ all -> 0x011b }
        r11.icon = r1;	 Catch:{ all -> 0x011b }
    L_0x0051:
        r1 = r11.title;	 Catch:{ all -> 0x011b }
        r1 = android.text.TextUtils.isEmpty(r1);	 Catch:{ all -> 0x011b }
        if (r1 == 0) goto L_0x006f;
    L_0x0059:
        if (r16 == 0) goto L_0x006f;
    L_0x005b:
        r0 = r16;
        r13 = r14.getPackageItemTitle(r0);	 Catch:{ all -> 0x011b }
        r11.title = r13;	 Catch:{ all -> 0x011b }
        r1 = r14.mUserManager;	 Catch:{ all -> 0x011b }
        r2 = r11.title;	 Catch:{ all -> 0x011b }
        r0 = r17;
        r1 = r1.getBadgedLabelForUser(r2, r0);	 Catch:{ all -> 0x011b }
        r11.contentDescription = r1;	 Catch:{ all -> 0x011b }
    L_0x006f:
        monitor-exit(r11);	 Catch:{ all -> 0x011b }
        r10 = r11;
    L_0x0071:
        r1 = r15.getPackageName();
        r1 = com.android.launcher3.common.view.LiveIconManager.isLiveIconPackage(r1);
        if (r1 == 0) goto L_0x0089;
    L_0x007b:
        r1 = r14.mContext;
        r2 = r15.getPackageName();
        r0 = r17;
        r1 = com.android.launcher3.common.view.LiveIconManager.getLiveIcon(r1, r2, r0);
        r10.icon = r1;
    L_0x0089:
        return r10;
    L_0x008a:
        r1 = move-exception;
        monitor-exit(r2);	 Catch:{ all -> 0x008a }
        throw r1;
    L_0x008d:
        r1 = move-exception;
    L_0x008e:
        monitor-exit(r2);	 Catch:{ all -> 0x008d }
        throw r1;
    L_0x0090:
        if (r18 == 0) goto L_0x00f5;
    L_0x0092:
        r2 = r15.getPackageName();	 Catch:{ all -> 0x011b }
        r4 = 0;
        r6 = 1;
        r1 = r14;
        r3 = r17;
        r5 = r20;
        r12 = r1.getEntryForPackageLocked(r2, r3, r4, r5, r6);	 Catch:{ all -> 0x011b }
        if (r12 == 0) goto L_0x00ab;
    L_0x00a3:
        r1 = r12.title;	 Catch:{ all -> 0x011b }
        r1 = android.text.TextUtils.isEmpty(r1);	 Catch:{ all -> 0x011b }
        if (r1 == 0) goto L_0x00cf;
    L_0x00ab:
        r1 = r14.mContext;	 Catch:{ all -> 0x011b }
        r7 = com.android.launcher3.common.compat.PackageInstallerCompat.getInstance(r1);	 Catch:{ all -> 0x011b }
        if (r7 == 0) goto L_0x00cf;
    L_0x00b3:
        r1 = r15.getPackageName();	 Catch:{ all -> 0x011b }
        r1 = r7.isSessionInfoItem(r1);	 Catch:{ all -> 0x011b }
        if (r1 == 0) goto L_0x00cf;
    L_0x00bd:
        r7.addAllSessionInfoToCache();	 Catch:{ all -> 0x011b }
        r2 = r15.getPackageName();	 Catch:{ all -> 0x011b }
        r4 = 0;
        r6 = 1;
        r1 = r14;
        r3 = r17;
        r5 = r20;
        r12 = r1.getEntryForPackageLocked(r2, r3, r4, r5, r6);	 Catch:{ all -> 0x011b }
    L_0x00cf:
        if (r12 == 0) goto L_0x00f5;
    L_0x00d1:
        r1 = "Launcher.IconCache";
        r2 = new java.lang.StringBuilder;	 Catch:{ all -> 0x011b }
        r2.<init>();	 Catch:{ all -> 0x011b }
        r3 = "using package default icon for ";
        r2 = r2.append(r3);	 Catch:{ all -> 0x011b }
        r2 = r2.append(r15);	 Catch:{ all -> 0x011b }
        r2 = r2.toString();	 Catch:{ all -> 0x011b }
        android.util.Log.d(r1, r2);	 Catch:{ all -> 0x011b }
        r1 = r12.icon;	 Catch:{ all -> 0x011b }
        r11.icon = r1;	 Catch:{ all -> 0x011b }
        r1 = r12.title;	 Catch:{ all -> 0x011b }
        r11.title = r1;	 Catch:{ all -> 0x011b }
        r1 = r12.contentDescription;	 Catch:{ all -> 0x011b }
        r11.contentDescription = r1;	 Catch:{ all -> 0x011b }
    L_0x00f5:
        r1 = r11.icon;	 Catch:{ all -> 0x011b }
        if (r1 != 0) goto L_0x0051;
    L_0x00f9:
        r1 = "Launcher.IconCache";
        r2 = new java.lang.StringBuilder;	 Catch:{ all -> 0x011b }
        r2.<init>();	 Catch:{ all -> 0x011b }
        r3 = "using default icon for ";
        r2 = r2.append(r3);	 Catch:{ all -> 0x011b }
        r2 = r2.append(r15);	 Catch:{ all -> 0x011b }
        r2 = r2.toString();	 Catch:{ all -> 0x011b }
        android.util.Log.d(r1, r2);	 Catch:{ all -> 0x011b }
        r0 = r17;
        r1 = r14.getDefaultIcon(r0);	 Catch:{ all -> 0x011b }
        r11.icon = r1;	 Catch:{ all -> 0x011b }
        goto L_0x0051;
    L_0x011b:
        r1 = move-exception;
        monitor-exit(r11);	 Catch:{ all -> 0x011b }
        throw r1;
    L_0x011e:
        r1 = move-exception;
        r10 = r11;
        goto L_0x008e;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.model.IconCache.cacheLocked(android.content.ComponentName, com.android.launcher3.common.compat.LauncherActivityInfoCompat, com.android.launcher3.common.compat.UserHandleCompat, boolean, boolean, int):com.android.launcher3.common.model.IconCache$CacheEntry");
    }

    public synchronized void cachePackageInstallInfo(String packageName, UserHandleCompat user, Bitmap icon, CharSequence title) {
        removeFromMemCacheLocked(packageName, user);
        ComponentKey cacheKey = getPackageKey(packageName, user);
        CacheEntry entry = (CacheEntry) this.mCache.get(cacheKey);
        if (entry == null) {
            entry = new CacheEntry();
            this.mCache.put(cacheKey, entry);
        }
        if (!TextUtils.isEmpty(title)) {
            entry.title = title;
        }
        if (icon != null) {
            entry.icon = BitmapUtils.createIconBitmap(icon, this.mContext);
        }
        Log.i(TAG, "cachePackageInstallInfo packageName = " + packageName + ", title = " + title + ", icon = " + (icon != null));
    }

    private static ComponentKey getPackageKey(String packageName, UserHandleCompat user) {
        return new ComponentKey(new ComponentName(packageName, packageName + EMPTY_CLASS_NAME), user);
    }

    private CacheEntry getWidgetEntryForPackage(String packageName, UserHandleCompat user, boolean useLowResIcon, int isDisabled) {
        Intent intent = this.mContext.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            ComponentKey key = new ComponentKey(intent.getComponent(), user);
            CacheEntry entry = (CacheEntry) this.mCache.get(key);
            if (entry != null && (!entry.isLowResIcon || useLowResIcon)) {
                CacheEntry newEntry = new CacheEntry();
                try {
                    int flags = (UserHandleCompat.myUserHandle().equals(user) && isDisabled == 0) ? 0 : 8192;
                    ApplicationInfo appInfo = this.mPackageManager.getPackageInfo(packageName, flags).applicationInfo;
                    if (appInfo == null) {
                        throw new NameNotFoundException("ApplicationInfo is null");
                    }
                    newEntry.title = getPackageItemTitle(appInfo, key.componentName);
                    newEntry.icon = entry.icon;
                    newEntry.isLowResIcon = entry.isLowResIcon;
                    newEntry.contentDescription = entry.contentDescription;
                    return newEntry;
                } catch (NameNotFoundException e) {
                }
            }
        }
        return getEntryForPackageLocked(packageName, user, useLowResIcon, isDisabled, false);
    }

    private CacheEntry getEntryForPackageLocked(String packageName, UserHandleCompat user, boolean useLowResIcon, int isDisabled, boolean useDBUpdate) {
        ComponentKey cacheKey = getPackageKey(packageName, user);
        CacheEntry entry = (CacheEntry) this.mCache.get(cacheKey);
        if (entry == null || (entry.isLowResIcon && !useLowResIcon)) {
            entry = new CacheEntry();
            boolean entryUpdated = true;
            if (!getEntryFromDB(cacheKey, entry, useLowResIcon)) {
                try {
                    int flags = (UserHandleCompat.myUserHandle().equals(user) && isDisabled == 0) ? 0 : 8192;
                    PackageInfo info = this.mPackageManager.getPackageInfo(packageName, flags);
                    ApplicationInfo appInfo = info.applicationInfo;
                    if (appInfo == null) {
                        throw new NameNotFoundException("ApplicationInfo is null");
                    }
                    Drawable drawable = getCSCPackageItemIcon(cacheKey.componentName);
                    if (drawable == null) {
                        drawable = this.mUserManager.getBadgedDrawableForUser(appInfo.loadIcon(this.mPackageManager), user);
                    }
                    entry.icon = BitmapUtils.createIconBitmap(drawable, this.mContext);
                    entry.title = getPackageItemTitle(appInfo, cacheKey.componentName);
                    entry.contentDescription = this.mUserManager.getBadgedLabelForUser(entry.title, user);
                    entry.isLowResIcon = false;
                    if (useDBUpdate) {
                        addIconToDB(newContentValues(entry.icon, entry.title.toString(), this.mPackageBgColor), cacheKey.componentName, info, this.mUserManager.getSerialNumberForUser(user));
                    }
                } catch (NameNotFoundException e) {
                    entryUpdated = false;
                }
            }
            if (entryUpdated) {
                synchronized (this.mCache) {
                    this.mCache.put(cacheKey, entry);
                }
            }
        }
        return entry;
    }

    public void preloadIcon(ComponentName componentName, Bitmap icon, int dpi, String label, long userSerial) {
        try {
            this.mContext.getPackageManager().getActivityIcon(componentName);
        } catch (NameNotFoundException e) {
            int iconSize = BitmapUtils.getIconBitmapSize();
            ContentValues values = newContentValues(Bitmap.createScaledBitmap(icon, iconSize, iconSize, true), label, 0);
            values.put(PostPositionProvider.COL_COMPONENT_NAME, componentName.flattenToString());
            values.put(BaseLauncherColumns.PROFILE_ID, Long.valueOf(userSerial));
            try {
                this.mIconDb.getWritableDatabase().insertWithOnConflict("icons", null, values, 5);
            } catch (SQLiteFullException e2) {
                Log.e(TAG, "Disk is full, preloadIcon, insertWithOnConflict failed : " + e2);
            }
        }
    }

    private boolean getEntryFromDB(ComponentKey cacheKey, CacheEntry entry, boolean lowRes) {
        Cursor c = null;
        try {
            SQLiteDatabase readableDatabase = this.mIconDb.getReadableDatabase();
            String str = "icons";
            String[] strArr = new String[2];
            strArr[0] = lowRes ? "icon_low_res" : "icon";
            strArr[1] = "label";
            c = readableDatabase.query(str, strArr, "componentName = ? AND profileId = ?", new String[]{cacheKey.componentName.flattenToString(), Long.toString(this.mUserManager.getSerialNumberForUser(cacheKey.user))}, null, null, null);
            if (c.moveToNext()) {
                entry.icon = loadIconNoResize(c, 0, lowRes ? this.mLowResOptions : null);
                entry.isLowResIcon = lowRes;
                entry.title = c.getString(1);
                if (entry.title == null) {
                    entry.title = "";
                    entry.contentDescription = "";
                } else {
                    entry.contentDescription = this.mUserManager.getBadgedLabelForUser(entry.title, cacheKey.user);
                }
                if (c == null) {
                    return true;
                }
                c.close();
                return true;
            }
            if (c != null) {
                c.close();
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "getEntryFromDB exception : " + e.getMessage());
            if (c != null) {
                c.close();
            }
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    private boolean isExistIconInDB(ComponentKey cacheKey) {
        Cursor c = null;
        try {
            c = this.mIconDb.getReadableDatabase().query("icons", null, "componentName = ? AND profileId = ?", new String[]{cacheKey.componentName.flattenToString(), Long.toString(this.mUserManager.getSerialNumberForUser(cacheKey.user))}, null, null, null);
            if (c.moveToNext()) {
                Log.e(TAG, "isExistIconInDB exist icon : " + cacheKey.componentName);
                if (c != null) {
                    c.close();
                }
                return true;
            }
            if (c != null) {
                c.close();
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "isExistIconInDB exception : " + e.getMessage());
            if (c != null) {
                c.close();
            }
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
        }
    }

    private void updateSystemStateString() {
        this.mSystemState = Locale.getDefault().toString();
    }

    private ContentValues newContentValues(Bitmap icon, String label, int lowResBackgroundColor) {
        ContentValues values = new ContentValues();
        values.put("icon", Utilities.flattenBitmap(icon));
        values.put("label", label);
        values.put("system_state", this.mSystemState);
        if (lowResBackgroundColor == 0) {
            values.put("icon_low_res", Utilities.flattenBitmap(Bitmap.createScaledBitmap(icon, icon.getWidth() / 5, icon.getHeight() / 5, true)));
        } else {
            synchronized (this) {
                if (this.mLowResBitmap == null) {
                    this.mLowResBitmap = Bitmap.createBitmap(icon.getWidth() / 5, icon.getHeight() / 5, Config.RGB_565);
                    this.mLowResCanvas = new Canvas(this.mLowResBitmap);
                    this.mLowResPaint = new Paint(3);
                }
                this.mLowResCanvas.drawColor(lowResBackgroundColor);
                this.mLowResCanvas.drawBitmap(icon, new Rect(0, 0, icon.getWidth(), icon.getHeight()), new Rect(0, 0, this.mLowResBitmap.getWidth(), this.mLowResBitmap.getHeight()), this.mLowResPaint);
                values.put("icon_low_res", Utilities.flattenBitmap(this.mLowResBitmap));
            }
        }
        return values;
    }

    private static Bitmap loadIconNoResize(Cursor c, int iconIndex, Options options) {
        byte[] data = c.getBlob(iconIndex);
        try {
            return BitmapFactory.decodeByteArray(data, 0, data.length, options);
        } catch (Exception e) {
            return null;
        }
    }

    private int getLauncherIconDensity(int requiredSize) {
        int[] densityBuckets = new int[]{120, 160, 213, 240, 320, DragLayer.ICON_FLICKING_DURATION, 640};
        int density = 640;
        for (int i = densityBuckets.length - 1; i >= 0; i--) {
            if ((ICON_SIZE_DEFINED_IN_APP_DP * ((float) densityBuckets[i])) / 160.0f >= ((float) requiredSize)) {
                density = densityBuckets[i];
            }
        }
        return density;
    }

    public int getIconDpi() {
        return this.mIconDpi;
    }

    private Drawable getCSCPackageItemIcon(ComponentName componentName) {
        Drawable drawable = this.mPackageManager.semGetCscPackageItemIcon(componentName.getClassName());
        if (drawable == null) {
            return this.mPackageManager.semGetCscPackageItemIcon(componentName.getPackageName());
        }
        return drawable;
    }

    private CharSequence getCSCPackageItemText(ComponentName componentName) {
        CharSequence text = this.mPackageManager.semGetCscPackageItemText(componentName.getClassName());
        if (TextUtils.isEmpty(text)) {
            return this.mPackageManager.semGetCscPackageItemText(componentName.getPackageName());
        }
        return text;
    }

    public CharSequence getPackageItemTitle(LauncherActivityInfoCompat info) {
        CharSequence title = getCSCPackageItemText(info.getComponentName());
        if (title == null) {
            return info.getLabel();
        }
        return title;
    }

    public CharSequence getPackageItemTitle(ApplicationInfo info, ComponentName componentName) {
        CharSequence title = getCSCPackageItemText(componentName);
        if (title == null) {
            return info.loadLabel(this.mPackageManager);
        }
        return title;
    }

    public void clearDB() {
        Log.d(TAG, "mIconDb is cleared");
        if (!TestHelper.isRoboUnitTest()) {
            this.mWorkerHandler.post(new Runnable() {
                public void run() {
                    Log.d(IconCache.TAG, "mIconDb is cleared running");
                    IconCache.this.mWorkerHandler.removeCallbacksAndMessages(IconCache.ICON_UPDATE_TOKEN);
                    IconCache.this.mIconDb.clearDB(IconCache.this.mIconDb.getWritableDatabase());
                }
            });
        }
    }

    public void clearCache(int iconSize) {
        this.mWorkerHandler.post(new Runnable() {
            public void run() {
                synchronized (IconCache.this.mCache) {
                    Log.d(IconCache.TAG, "mCache is cleared running");
                    IconCache.this.mCache.clear();
                }
            }
        });
        int currentDensity = getLauncherIconDensity(iconSize);
        if (this.mIconDpi != currentDensity) {
            this.mIconDpi = currentDensity;
            this.isIconDpiChanged = true;
        }
        Log.i(TAG, "clearCache : " + this.mIconDpi);
    }
}

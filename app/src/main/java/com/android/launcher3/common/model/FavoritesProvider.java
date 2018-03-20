package com.android.launcher3.common.model;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteReadOnlyDatabaseException;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Pair;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherProvider;
import com.android.launcher3.LauncherProviderChangeListener;
import com.android.launcher3.LauncherProviderID;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.bnr.LauncherBnrHelper;
import com.android.launcher3.common.bnr.LauncherBnrTag;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.customer.PostPositionController;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.common.model.LauncherSettings.ChangeLogColumns;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.model.LauncherSettings.Favorites_HomeOnly;
import com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.home.LauncherAppWidgetHostView;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.MainThreadExecutor;
import com.android.launcher3.util.StringJoiner;
import com.android.launcher3.util.TestHelper;
import com.android.launcher3.util.WhiteBgManager;
import com.samsung.android.feature.SemCscFeature;
import com.sec.android.app.launcher.R;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class FavoritesProvider extends DataProvider implements DataInterface {
    private static final String CHECK_CHANGED_COMPONENT_EXITST = "checkChangedComponentVersion";
    private static final String CLOCK_WIDGET_EASY_PACKAGE = "com.sec.android.daemonapp";
    private static final String CONTACT_WIDGET_EASY_CLASS = "com.sec.android.widgetapp.easymodecontactswidget.SeniorFavoriteWidgetProviderLarge";
    private static final String CONTACT_WIDGET_EASY_PACKAGE = "com.sec.android.widgetapp.easymodecontactswidget";
    private static final String DALI_PAGE_SETTING_DB = "pagesettings.db";
    private static final boolean DEBUGGABLE = true;
    private static final String EASY_LAUNCHER_DB = "easylauncher.db";
    private static final int EASY_MODE_GRID = 3;
    private static final String EASY_MODE_PREFERENCE_FILE = "com.sec.android.app.easylauncher.prefs.xml";
    private static final String EASY_MODE_PREFERENCE_KEY = "com.sec.android.app.easylauncher.prefs";
    public static final String EMPTY_DATABASE_SWITCHED = "EMPTY_DATABASE_SWITCHED";
    private static final String GRID_INFO_SPLIT = "\\|";
    private static final int HOME_INDEX_EASY_MODE = 2;
    public static final int SWITCH_EASY_MODE = 2;
    public static final int SWITCH_HOME_MODE = 1;
    public static final int SWITCH_HOME_MODE_ON_EASY = 3;
    private static final String TABLE_APPORDER = "appOrder";
    private static final String TABLE_FAVORITES = "favorites";
    private static final String TABLE_FAVORITES_EASY = "favorites_easy";
    private static final String TABLE_FAVORITES_HOME_APPS = "favorites_homeApps";
    private static final String TABLE_FAVORITES_HOME_ONLY = "favorites_homeOnly";
    private static final String TABLE_FAVORITES_STANDARD = "favorites_standard";
    private static final String TABLE_WORKSPACE_SCREENS = "workspaceScreens";
    private static final String TABLE_WORKSPACE_SCREENS_EASY = "workspaceScreens_easy";
    private static final String TABLE_WORKSPACE_SCREENS_HOME_APPS = "workspaceScreens_homeApps";
    private static final String TABLE_WORKSPACE_SCREENS_HOME_ONLY = "workspaceScreens_homeOnly";
    private static final String TABLE_WORKSPACE_SCREENS_STANDARD = "workspaceScreens_standard";
    private static final String TAG = "FavoritesProvider";
    private static final String WEATHER_WIDGET_EASY_CLASS = "com.sec.android.daemonapp.appwidget.WeatherAppWidget";
    private static final String WEATHER_WIDGET_EASY_PACKAGE = "com.sec.android.daemonapp";
    private static final FavoritesProvider sInstance = new FavoritesProvider();
    private final AppWidgetHost mAppWidgetHost = new AppWidgetHost(sContext, 1024);
    private int mCountY = -1;
    private int mDaliPageCnt = 0;
    private long mMaxScreenId = -1;

    public static class AppOrderModify {
        public static final int CREATE_APP = 3;
        public static final int CREATE_FOLDER = 0;
        public static final int DELETE_ITEM = 5;
        public static final int RESET_RESTORED = 7;
        public static final int UPDATE_APP = 4;
        public static final int UPDATE_COLOR = 6;
        public static final int UPDATE_FOLDER = 1;
        public static final int UPDATE_TITLE = 2;
        public int action;
        public final int color = -1;
        public ComponentName component;
        public long container;
        public final boolean hidden = false;
        public long id;
        public int itemtype;
        public long modified;
        public int rank;
        public long screen;
        public int status;
        public CharSequence title;
        public UserHandleCompat user;
    }

    static class FolderDbInfo {
        int cellX;
        int cellY;
        Long id;
        int rank;
        int screen;
        String title;

        FolderDbInfo() {
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void convertAppsTable(java.lang.String r46, int r47) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x0033 in list [B:7:0x002a]
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
        /*
        r45 = this;
        r33 = 0;
        r34 = r45.getMaxId(r46);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = sDb;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2.beginTransaction();	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = sDb;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = "appOrder";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r4 = 0;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r5 = 0;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r6 = 0;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r7 = 0;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r8 = 0;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r9 = "folderId, screen, cell";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r33 = r2.query(r3, r4, r5, r6, r7, r8, r9);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        if (r33 != 0) goto L_0x0034;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
    L_0x001c:
        r2 = "FavoritesProvider";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = "AppsTable converting error : Getting app order list item";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        android.util.Log.e(r2, r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = sDb;
        r2.endTransaction();
        if (r33 == 0) goto L_0x0033;
    L_0x002a:
        r2 = r33.isClosed();
        if (r2 != 0) goto L_0x0033;
    L_0x0030:
        r33.close();
    L_0x0033:
        return;
    L_0x0034:
        r2 = r33.moveToFirst();	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        if (r2 == 0) goto L_0x01a9;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
    L_0x003a:
        r2 = "_id";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r33;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r25 = r0.getColumnIndexOrThrow(r2);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = "folderId";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r33;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r28 = r0.getColumnIndexOrThrow(r2);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = "screen";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r33;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r31 = r0.getColumnIndexOrThrow(r2);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = "title";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r33;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r32 = r0.getColumnIndexOrThrow(r2);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = "componentName";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r33;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r26 = r0.getColumnIndexOrThrow(r2);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = "color";	 Catch:{ IllegalArgumentException -> 0x01c3 }
        r0 = r33;	 Catch:{ IllegalArgumentException -> 0x01c3 }
        r27 = r0.getColumnIndexOrThrow(r2);	 Catch:{ IllegalArgumentException -> 0x01c3 }
    L_0x006a:
        r2 = "profileId";	 Catch:{ IllegalArgumentException -> 0x01c8 }
        r0 = r33;	 Catch:{ IllegalArgumentException -> 0x01c8 }
        r30 = r0.getColumnIndexOrThrow(r2);	 Catch:{ IllegalArgumentException -> 0x01c8 }
    L_0x0072:
        r2 = "hidden";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r33;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r29 = r0.getColumnIndexOrThrow(r2);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r21 = new android.util.LongSparseArray;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r21.<init>();	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r18 = -102; // 0xffffffffffffff9a float:NaN double:NaN;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r38 = 0;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r36 = -1;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r42 = 0;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r12 = -1;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r14 = 0;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
    L_0x008b:
        r44 = new android.content.ContentValues;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r44.<init>();	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = "_id";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r4 = 1;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r34 = r34 + r4;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = java.lang.Long.valueOf(r34);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r44;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0.put(r2, r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = "title";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r33;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r1 = r32;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = r0.getString(r1);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r44;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0.put(r2, r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r33;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r1 = r25;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r10 = r0.getLong(r1);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r33;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r1 = r26;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r16 = r0.getString(r1);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r33;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r1 = r28;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r22 = r0.getLong(r1);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r33;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r1 = r29;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r24 = r0.getInt(r1);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        if (r30 <= 0) goto L_0x01cd;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
    L_0x00d0:
        r0 = r33;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r1 = r30;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = r0.getInt(r1);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = (long) r2;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r40 = r0;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
    L_0x00db:
        if (r27 <= 0) goto L_0x01d1;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
    L_0x00dd:
        r0 = r33;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r1 = r27;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r17 = r0.getInt(r1);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
    L_0x00e5:
        r2 = android.text.TextUtils.isEmpty(r16);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        if (r2 == 0) goto L_0x01d5;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
    L_0x00eb:
        r2 = "itemType";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = 2;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = java.lang.Integer.valueOf(r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r44;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0.put(r2, r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = java.lang.Long.valueOf(r34);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r21;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0.put(r10, r2);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
    L_0x0100:
        r2 = 0;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = (r22 > r2 ? 1 : (r22 == r2 ? 0 : -1));	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        if (r2 <= 0) goto L_0x0110;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
    L_0x0106:
        r2 = r21.get(r22);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = (java.lang.Long) r2;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r18 = r2.longValue();	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
    L_0x0110:
        r2 = "container";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = java.lang.Long.valueOf(r18);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r44;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0.put(r2, r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r33;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r1 = r31;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = r0.getLong(r1);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = (r2 > r38 ? 1 : (r2 == r38 ? 0 : -1));	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        if (r2 != 0) goto L_0x012d;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
    L_0x0127:
        r2 = -1;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = (r22 > r2 ? 1 : (r22 == r2 ? 0 : -1));	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        if (r2 == 0) goto L_0x0131;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
    L_0x012d:
        r2 = (r22 > r36 ? 1 : (r22 == r36 ? 0 : -1));	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        if (r2 != 0) goto L_0x0230;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
    L_0x0131:
        r2 = 1;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r42 = r42 + r2;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = 1;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r12 = r12 + r2;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r47;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = (long) r0;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = (r12 > r2 ? 1 : (r12 == r2 ? 0 : -1));	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        if (r2 < 0) goto L_0x0144;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
    L_0x013f:
        r2 = 1;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r14 = r14 + r2;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r12 = 0;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
    L_0x0144:
        r2 = "screen";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r33;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r1 = r31;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r4 = r0.getLong(r1);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = java.lang.Long.valueOf(r4);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r44;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0.put(r2, r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = "cellX";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = java.lang.Long.valueOf(r12);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r44;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0.put(r2, r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = "cellY";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = java.lang.Long.valueOf(r14);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r44;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0.put(r2, r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = "rank";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = java.lang.Long.valueOf(r42);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r44;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0.put(r2, r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = "hidden";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = java.lang.Integer.valueOf(r24);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r44;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0.put(r2, r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = "profileId";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = java.lang.Long.valueOf(r40);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r44;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0.put(r2, r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = "color";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = java.lang.Integer.valueOf(r17);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r44;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0.put(r2, r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = sDb;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = 0;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r46;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r1 = r44;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2.insert(r0, r3, r1);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = r33.moveToNext();	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        if (r2 != 0) goto L_0x008b;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
    L_0x01a9:
        r33.close();	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = sDb;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2.setTransactionSuccessful();	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = sDb;
        r2.endTransaction();
        if (r33 == 0) goto L_0x0033;
    L_0x01b8:
        r2 = r33.isClosed();
        if (r2 != 0) goto L_0x0033;
    L_0x01be:
        r33.close();
        goto L_0x0033;
    L_0x01c3:
        r20 = move-exception;
        r27 = -1;
        goto L_0x006a;
    L_0x01c8:
        r20 = move-exception;
        r30 = -1;
        goto L_0x0072;
    L_0x01cd:
        r40 = 0;
        goto L_0x00db;
    L_0x01d1:
        r17 = 0;
        goto L_0x00e5;
    L_0x01d5:
        r2 = "itemType";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = 0;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = java.lang.Integer.valueOf(r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r44;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0.put(r2, r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = "intent";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r33;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r1 = r26;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = r0.getString(r1);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = android.content.ComponentName.unflattenFromString(r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r40;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = com.android.launcher3.common.base.item.IconInfo.makeLaunchIntent(r3, r0);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r4 = 0;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = r3.toUri(r4);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0 = r44;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r0.put(r2, r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        goto L_0x0100;
    L_0x0201:
        r20 = move-exception;
        r2 = "FavoritesProvider";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3.<init>();	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r4 = "AppsTable converting error : ";	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = r3.append(r4);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r4 = r20.getMessage();	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = r3.append(r4);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r3 = r3.toString();	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        android.util.Log.e(r2, r3);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r2 = sDb;
        r2.endTransaction();
        if (r33 == 0) goto L_0x0033;
    L_0x0225:
        r2 = r33.isClosed();
        if (r2 != 0) goto L_0x0033;
    L_0x022b:
        r33.close();
        goto L_0x0033;
    L_0x0230:
        r0 = r33;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r1 = r31;	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r38 = r0.getLong(r1);	 Catch:{ Exception -> 0x0201, all -> 0x0242 }
        r36 = r22;
        r42 = 0;
        r12 = 0;
        r14 = 0;
        goto L_0x0144;
    L_0x0242:
        r2 = move-exception;
        r3 = sDb;
        r3.endTransaction();
        if (r33 == 0) goto L_0x0253;
    L_0x024a:
        r3 = r33.isClosed();
        if (r3 != 0) goto L_0x0253;
    L_0x0250:
        r33.close();
    L_0x0253:
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.model.FavoritesProvider.convertAppsTable(java.lang.String, int):void");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void migrationAppsButton(java.lang.String r13) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x003c in list [B:9:0x0033]
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
        /*
        r12 = this;
        r3 = "container=-101";
        r10 = 0;
        r0 = sDb;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r1 = 1;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r2 = new java.lang.String[r1];	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r1 = 0;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r4 = "MAX(screen)";	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r2[r1] = r4;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r4 = 0;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r5 = 0;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r6 = 0;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r7 = 0;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r1 = r13;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r10 = r0.query(r1, r2, r3, r4, r5, r6, r7);	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        if (r10 == 0) goto L_0x003d;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
    L_0x0018:
        r8 = -1;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r0 = r10.moveToFirst();	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        if (r0 == 0) goto L_0x0025;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
    L_0x0020:
        r0 = 0;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r8 = r10.getLong(r0);	 Catch:{ Exception -> 0x0045, all -> 0x006a }
    L_0x0025:
        r6 = r12.getMaxId(r13);	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r0 = 1;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r8 = r8 + r0;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r4 = r12;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r5 = r13;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r4.addAppsButton(r5, r6, r8);	 Catch:{ Exception -> 0x0045, all -> 0x006a }
    L_0x0031:
        if (r10 == 0) goto L_0x003c;
    L_0x0033:
        r0 = r10.isClosed();
        if (r0 != 0) goto L_0x003c;
    L_0x0039:
        r10.close();
    L_0x003c:
        return;
    L_0x003d:
        r0 = "FavoritesProvider";	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r1 = "migrationAppsButton cursor is null";	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        android.util.Log.d(r0, r1);	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        goto L_0x0031;
    L_0x0045:
        r11 = move-exception;
        r0 = "FavoritesProvider";	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r1 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r1.<init>();	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r2 = "migrationAppsButton error : ";	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r1 = r1.append(r2);	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r1 = r1.append(r11);	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        r1 = r1.toString();	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        android.util.Log.e(r0, r1);	 Catch:{ Exception -> 0x0045, all -> 0x006a }
        if (r10 == 0) goto L_0x003c;
    L_0x0060:
        r0 = r10.isClosed();
        if (r0 != 0) goto L_0x003c;
    L_0x0066:
        r10.close();
        goto L_0x003c;
    L_0x006a:
        r0 = move-exception;
        if (r10 == 0) goto L_0x0076;
    L_0x006d:
        r1 = r10.isClosed();
        if (r1 != 0) goto L_0x0076;
    L_0x0073:
        r10.close();
    L_0x0076:
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.model.FavoritesProvider.migrationAppsButton(java.lang.String):void");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void migrationTableForEasyMode(long r28) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x0108 in list [B:33:0x0101]
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
        /*
        r27 = this;
        r13 = 1;
        r5 = "favorites_easy";
        r0 = r27;
        r1 = r28;
        r0.createFavoritesTable(r1, r5);
        r5 = "workspaceScreens_easy";
        r0 = r27;
        r0.createScreensTable(r5);
        r16 = new java.io.File;
        r5 = sContext;
        r6 = "easylauncher.db";
        r5 = r5.getDatabasePath(r6);
        r5 = r5.getParent();
        r6 = "easylauncher.db";
        r0 = r16;
        r0.<init>(r5, r6);
        r5 = r16.exists();
        if (r5 == 0) goto L_0x0109;
    L_0x002c:
        r5 = sContext;
        r6 = "easylauncher.db";
        r5 = r5.getDatabasePath(r6);
        r5 = r5.toString();
        r6 = 0;
        r7 = 0;
        r4 = android.database.sqlite.SQLiteDatabase.openDatabase(r5, r6, r7);
        r5 = r27.movePrefFileForEasy();
        if (r5 == 0) goto L_0x0098;
    L_0x0044:
        r5 = sContext;
        r6 = "com.sec.android.app.easylauncher.prefs";
        r7 = 0;
        r24 = r5.getSharedPreferences(r6, r7);
        r5 = "contact1_onoff";
        r0 = r24;
        r5 = r0.contains(r5);
        if (r5 == 0) goto L_0x0063;
    L_0x0057:
        r5 = "contact1_onoff";
        r6 = 0;
        r0 = r24;
        r5 = r0.getInt(r5, r6);
        if (r5 != 0) goto L_0x0111;
    L_0x0062:
        r13 = 1;
    L_0x0063:
        r19 = new java.io.File;
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r6 = sContext;
        r6 = r6.getApplicationInfo();
        r6 = r6.dataDir;
        r5 = r5.append(r6);
        r6 = "/shared_prefs/";
        r5 = r5.append(r6);
        r6 = "com.sec.android.app.easylauncher.prefs.xml";
        r5 = r5.append(r6);
        r5 = r5.toString();
        r0 = r19;
        r0.<init>(r5);
        r18 = r19.delete();
        if (r18 != 0) goto L_0x0098;
    L_0x0091:
        r5 = "FavoritesProvider";
        r6 = "EASY_MODE_PREFERENCE_FILE was not deleted";
        android.util.Log.e(r5, r6);
    L_0x0098:
        r4.beginTransaction();
        r25 = 0;
        r0 = r27;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r5 = r0.tableExistsForEasy(r4);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        if (r5 == 0) goto L_0x01c9;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
    L_0x00a5:
        r22 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r20 = -1;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r5 = "favorites";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r6 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r7 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r8 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r9 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r10 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r11 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r25 = r4.query(r5, r6, r7, r8, r9, r10, r11);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        if (r25 == 0) goto L_0x00cf;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
    L_0x00b7:
        r5 = r25.moveToFirst();	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        if (r5 == 0) goto L_0x00cc;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
    L_0x00bd:
        r0 = r27;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r1 = r25;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r2 = r28;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r0.addDataToEasyTable(r1, r13, r2);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r5 = r25.moveToNext();	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        if (r5 != 0) goto L_0x00bd;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
    L_0x00cc:
        r25.close();	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
    L_0x00cf:
        r5 = sDb;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r6 = "favorites_easy";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r7 = 1;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r7 = new java.lang.String[r7];	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r10 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r11 = "MAX(screen)";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r7[r10] = r11;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r8 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r9 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r10 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r11 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r12 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r25 = r5.query(r6, r7, r8, r9, r10, r11, r12);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        if (r25 != 0) goto L_0x0114;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
    L_0x00e6:
        r5 = "FavoritesProvider";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r6 = "easylauncher converting error : NPE when getting pagecount";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        android.util.Log.e(r5, r6);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r4.endTransaction();
        if (r25 == 0) goto L_0x00fb;
    L_0x00f2:
        r5 = r25.isClosed();
        if (r5 != 0) goto L_0x00fb;
    L_0x00f8:
        r25.close();
    L_0x00fb:
        r18 = r16.delete();
        if (r18 != 0) goto L_0x0108;
    L_0x0101:
        r5 = "FavoritesProvider";
        r6 = "EASY_LAUNCHER_DB was not deleted";
        android.util.Log.e(r5, r6);
    L_0x0108:
        return;
    L_0x0109:
        r5 = "FavoritesProvider";
        r6 = "EASY_LAUNCHER_DB is not exist";
        android.util.Log.e(r5, r6);
        goto L_0x0108;
    L_0x0111:
        r13 = 0;
        goto L_0x0063;
    L_0x0114:
        r5 = r25.moveToFirst();	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        if (r5 == 0) goto L_0x0121;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
    L_0x011a:
        r5 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r0 = r25;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r22 = r0.getLong(r5);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
    L_0x0121:
        r25.close();	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r17 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
    L_0x0126:
        r0 = r17;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r6 = (long) r0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r5 = (r6 > r22 ? 1 : (r6 == r22 ? 0 : -1));	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        if (r5 > 0) goto L_0x014f;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
    L_0x012d:
        r14 = new android.content.ContentValues;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r14.<init>();	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r5 = "_id";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r6 = java.lang.Integer.valueOf(r17);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r14.put(r5, r6);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r5 = "screenRank";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r6 = java.lang.Integer.valueOf(r17);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r14.put(r5, r6);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r5 = sDb;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r6 = "workspaceScreens_easy";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r7 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r5.insert(r6, r7, r14);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r17 = r17 + 1;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        goto L_0x0126;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
    L_0x014f:
        r5 = sDb;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r6 = "favorites_easy";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r7 = 1;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r7 = new java.lang.String[r7];	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r10 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r11 = "MAX(_id)";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r7[r10] = r11;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r8 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r9 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r10 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r11 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r12 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r25 = r5.query(r6, r7, r8, r9, r10, r11, r12);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        if (r25 != 0) goto L_0x0189;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
    L_0x0166:
        r5 = "FavoritesProvider";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r6 = "easylauncher converting error : NPE when getting max id";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        android.util.Log.e(r5, r6);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r4.endTransaction();
        if (r25 == 0) goto L_0x017b;
    L_0x0172:
        r5 = r25.isClosed();
        if (r5 != 0) goto L_0x017b;
    L_0x0178:
        r25.close();
    L_0x017b:
        r18 = r16.delete();
        if (r18 != 0) goto L_0x0108;
    L_0x0181:
        r5 = "FavoritesProvider";
        r6 = "EASY_LAUNCHER_DB was not deleted";
        android.util.Log.e(r5, r6);
        goto L_0x0108;
    L_0x0189:
        r5 = r25.moveToFirst();	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        if (r5 == 0) goto L_0x022d;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
    L_0x018f:
        r5 = 0;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r0 = r25;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r8 = r0.getLong(r5);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
    L_0x0196:
        r25.close();	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r7 = "favorites_easy";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r10 = 2;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r6 = r27;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r6.addAppsButton(r7, r8, r10);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r5 = 2 - r13;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r0 = r27;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r0.setDefaultHomeForEasy(r5);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
    L_0x01a9:
        r4.setTransactionSuccessful();	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r4.endTransaction();
        if (r25 == 0) goto L_0x01ba;
    L_0x01b1:
        r5 = r25.isClosed();
        if (r5 != 0) goto L_0x01ba;
    L_0x01b7:
        r25.close();
    L_0x01ba:
        r18 = r16.delete();
        if (r18 != 0) goto L_0x0108;
    L_0x01c0:
        r5 = "FavoritesProvider";
        r6 = "EASY_LAUNCHER_DB was not deleted";
        android.util.Log.e(r5, r6);
        goto L_0x0108;
    L_0x01c9:
        r5 = "FavoritesProvider";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r6 = "no table exist : favorites_easy";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        android.util.Log.i(r5, r6);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r5 = "favorites_easy";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r0 = r27;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r1 = r28;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r0.createFavoritesTable(r1, r5);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        goto L_0x01a9;
    L_0x01da:
        r15 = move-exception;
        r5 = "FavoritesProvider";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r6 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r6.<init>();	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r7 = "EasyTable migration error : ";	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r6 = r6.append(r7);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r6 = r6.append(r15);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r6 = r6.toString();	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        android.util.Log.e(r5, r6);	 Catch:{ Exception -> 0x01da, all -> 0x0210 }
        r4.endTransaction();
        if (r25 == 0) goto L_0x0201;
    L_0x01f8:
        r5 = r25.isClosed();
        if (r5 != 0) goto L_0x0201;
    L_0x01fe:
        r25.close();
    L_0x0201:
        r18 = r16.delete();
        if (r18 != 0) goto L_0x0108;
    L_0x0207:
        r5 = "FavoritesProvider";
        r6 = "EASY_LAUNCHER_DB was not deleted";
        android.util.Log.e(r5, r6);
        goto L_0x0108;
    L_0x0210:
        r5 = move-exception;
        r4.endTransaction();
        if (r25 == 0) goto L_0x021f;
    L_0x0216:
        r6 = r25.isClosed();
        if (r6 != 0) goto L_0x021f;
    L_0x021c:
        r25.close();
    L_0x021f:
        r18 = r16.delete();
        if (r18 != 0) goto L_0x022c;
    L_0x0225:
        r6 = "FavoritesProvider";
        r7 = "EASY_LAUNCHER_DB was not deleted";
        android.util.Log.e(r6, r7);
    L_0x022c:
        throw r5;
    L_0x022d:
        r8 = r20;
        goto L_0x0196;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.model.FavoritesProvider.migrationTableForEasyMode(long):void");
    }

    private void prevMigrationForDali() {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x007a in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
        /*
        r10 = this;
        r0 = 0;
        r3 = sDb;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = "ALTER TABLE favorites ADD COLUMN screen INTEGER NOT NULL DEFAULT 0";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3.execSQL(r4);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = sContext;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = "pagesettings.db";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = r3.getDatabasePath(r4);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = r3.getPath();	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = 0;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r5 = 1;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r2 = android.database.sqlite.SQLiteDatabase.openDatabase(r3, r4, r5);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = "SELECT _id, pageOrder FROM page_settings ORDER BY pageOrder";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = 0;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r0 = r2.rawQuery(r3, r4);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        if (r0 == 0) goto L_0x007e;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
    L_0x0023:
        r3 = r0.getCount();	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r10.mDaliPageCnt = r3;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
    L_0x0029:
        r3 = r0.moveToNext();	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        if (r3 == 0) goto L_0x007b;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
    L_0x002f:
        r3 = sDb;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = "UPDATE favorites SET screen=? WHERE containerId=?";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r5 = 2;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r5 = new java.lang.Object[r5];	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r6 = 0;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r7 = 1;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r8 = r0.getLong(r7);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r7 = java.lang.Long.valueOf(r8);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r5[r6] = r7;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r6 = 1;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r7 = 0;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r8 = r0.getLong(r7);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r7 = java.lang.Long.valueOf(r8);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r5[r6] = r7;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3.execSQL(r4, r5);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        goto L_0x0029;
    L_0x0052:
        r1 = move-exception;
        r3 = "FavoritesProvider";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = new java.lang.StringBuilder;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4.<init>();	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r5 = "prevMigrationForDali() : ";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = r4.append(r5);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r5 = r1.getMessage();	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = r4.append(r5);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = r4.toString();	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        android.util.Log.e(r3, r4);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        if (r0 == 0) goto L_0x007a;
    L_0x0071:
        r3 = r0.isClosed();
        if (r3 != 0) goto L_0x007a;
    L_0x0077:
        r0.close();
    L_0x007a:
        return;
    L_0x007b:
        r0.close();	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
    L_0x007e:
        r2.close();	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = sDb;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = "UPDATE favorites SET screen=cellY*3+cellX WHERE container=3";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3.execSQL(r4);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = sDb;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = "UPDATE favorites SET screen=cellX WHERE container=1";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3.execSQL(r4);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = sDb;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = "UPDATE favorites SET container=-100 WHERE container=0";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3.execSQL(r4);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = sDb;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = "UPDATE favorites SET container=-101 WHERE container=1";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3.execSQL(r4);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = sDb;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = "UPDATE favorites SET container=containerId WHERE container=3";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3.execSQL(r4);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = sDb;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = "UPDATE favorites SET itemType=itemType-1";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3.execSQL(r4);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = "appOrder";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = r10.tableExists(r3);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        if (r3 == 0) goto L_0x00e4;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
    L_0x00b3:
        r3 = sDb;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = "ALTER TABLE appOrder ADD COLUMN folderId INTEGER NOT NULL DEFAULT -1";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3.execSQL(r4);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = sDb;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = "ALTER TABLE appOrder ADD COLUMN screen INTEGER NOT NULL DEFAULT -1";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3.execSQL(r4);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = sDb;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = "ALTER TABLE appOrder ADD COLUMN cell INTEGER NOT NULL DEFAULT -1";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3.execSQL(r4);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = sDb;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = "UPDATE appOrder SET folderId=containerId WHERE container=3";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3.execSQL(r4);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = sDb;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = "UPDATE appOrder SET screen=pos WHERE container=3";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3.execSQL(r4);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = sDb;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = "UPDATE appOrder SET screen=containerId WHERE container=2";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3.execSQL(r4);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3 = sDb;	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r4 = "UPDATE appOrder SET cell=pos WHERE container=2";	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
        r3.execSQL(r4);	 Catch:{ SQLException -> 0x0052, all -> 0x00f0 }
    L_0x00e4:
        if (r0 == 0) goto L_0x007a;
    L_0x00e6:
        r3 = r0.isClosed();
        if (r3 != 0) goto L_0x007a;
    L_0x00ec:
        r0.close();
        goto L_0x007a;
    L_0x00f0:
        r3 = move-exception;
        if (r0 == 0) goto L_0x00fc;
    L_0x00f3:
        r4 = r0.isClosed();
        if (r4 != 0) goto L_0x00fc;
    L_0x00f9:
        r0.close();
    L_0x00fc:
        throw r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.model.FavoritesProvider.prevMigrationForDali():void");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void convertShortcutsToLauncherActivities() {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x0091 in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
        /*
        r22 = this;
        r2 = sDb;
        r2.beginTransaction();
        r10 = 0;
        r19 = 0;
        r2 = sContext;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r2 = com.android.launcher3.common.compat.UserManagerCompat.getInstance(r2);	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r3 = com.android.launcher3.common.compat.UserHandleCompat.myUserHandle();	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r20 = r2.getSerialNumberForUser(r3);	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r2 = sDb;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r3 = "favorites";	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r4 = 2;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r4 = new java.lang.String[r4];	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r5 = 0;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r6 = "_id";	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r4[r5] = r6;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r5 = 1;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r6 = "intent";	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r4[r5] = r6;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r5 = new java.lang.StringBuilder;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r5.<init>();	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r6 = "itemType=1 AND profileId=";	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r5 = r5.append(r6);	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r0 = r20;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r5 = r5.append(r0);	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r5 = r5.toString();	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r6 = 0;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r7 = 0;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r8 = 0;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r9 = 0;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r10 = r2.query(r3, r4, r5, r6, r7, r8, r9);	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r2 = sDb;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r3 = "UPDATE favorites SET itemType=0 WHERE _id=?";	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r19 = r2.compileStatement(r3);	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r2 = "_id";	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r13 = r10.getColumnIndexOrThrow(r2);	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r2 = "intent";	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r18 = r10.getColumnIndexOrThrow(r2);	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
    L_0x0058:
        r2 = r10.moveToNext();	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        if (r2 == 0) goto L_0x00b1;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
    L_0x005e:
        r0 = r18;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r17 = r10.getString(r0);	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r2 = 0;
        r0 = r17;	 Catch:{ URISyntaxException -> 0x0097 }
        r16 = android.content.Intent.parseUri(r0, r2);	 Catch:{ URISyntaxException -> 0x0097 }
        r2 = com.android.launcher3.Utilities.isLauncherAppTarget(r16);	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        if (r2 == 0) goto L_0x0058;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
    L_0x0071:
        r14 = r10.getLong(r13);	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r2 = 1;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r0 = r19;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r0.bindLong(r2, r14);	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r19.executeUpdateDelete();	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        goto L_0x0058;
    L_0x007f:
        r12 = move-exception;
        r2 = "FavoritesProvider";	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r3 = "Error deduping shortcuts";	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        android.util.Log.w(r2, r3, r12);	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r2 = sDb;
        r2.endTransaction();
        if (r10 == 0) goto L_0x0091;
    L_0x008e:
        r10.close();
    L_0x0091:
        if (r19 == 0) goto L_0x0096;
    L_0x0093:
        r19.close();
    L_0x0096:
        return;
    L_0x0097:
        r11 = move-exception;
        r2 = "FavoritesProvider";	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r3 = "Unable to parse intent";	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        android.util.Log.e(r2, r3, r11);	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        goto L_0x0058;
    L_0x00a0:
        r2 = move-exception;
        r3 = sDb;
        r3.endTransaction();
        if (r10 == 0) goto L_0x00ab;
    L_0x00a8:
        r10.close();
    L_0x00ab:
        if (r19 == 0) goto L_0x00b0;
    L_0x00ad:
        r19.close();
    L_0x00b0:
        throw r2;
    L_0x00b1:
        r2 = sDb;	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r2.setTransactionSuccessful();	 Catch:{ SQLException -> 0x007f, all -> 0x00a0 }
        r2 = sDb;
        r2.endTransaction();
        if (r10 == 0) goto L_0x00c0;
    L_0x00bd:
        r10.close();
    L_0x00c0:
        if (r19 == 0) goto L_0x0096;
    L_0x00c2:
        r19.close();
        goto L_0x0096;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.model.FavoritesProvider.convertShortcutsToLauncherActivities():void");
    }

    public void deleteWidgetIds(java.lang.String r14) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x006d in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
        /*
        r13 = this;
        r0 = 1;
        r1 = 0;
        r11 = "itemType=?";
        r4 = new java.lang.String[r0];
        r0 = 4;
        r0 = java.lang.String.valueOf(r0);
        r4[r1] = r0;
        r8 = 0;
        r0 = sDb;	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r1 = 1;	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r2 = new java.lang.String[r1];	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r1 = 0;	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r3 = "appWidgetId";	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r2[r1] = r3;	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r3 = "itemType=?";	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r5 = 0;	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r6 = 0;	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r7 = 0;	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r1 = r14;	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r8 = r0.query(r1, r2, r3, r4, r5, r6, r7);	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        if (r8 == 0) goto L_0x006e;	 Catch:{ Exception -> 0x0058, all -> 0x007a }
    L_0x0024:
        r0 = r8.getCount();	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        if (r0 <= 0) goto L_0x006e;	 Catch:{ Exception -> 0x0058, all -> 0x007a }
    L_0x002a:
        r0 = "appWidgetId";	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r10 = r8.getColumnIndexOrThrow(r0);	 Catch:{ Exception -> 0x0058, all -> 0x007a }
    L_0x0030:
        r0 = r8.moveToNext();	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        if (r0 == 0) goto L_0x006e;	 Catch:{ Exception -> 0x0058, all -> 0x007a }
    L_0x0036:
        r12 = r8.getInt(r10);	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r0 = "FavoritesProvider";	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r1 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r1.<init>();	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r2 = "delete WidgetId : ";	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r1 = r1.append(r2);	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r1 = r1.append(r12);	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r1 = r1.toString();	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        android.util.Log.d(r0, r1);	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r0 = r13.mAppWidgetHost;	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r0.deleteAppWidgetId(r12);	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        goto L_0x0030;
    L_0x0058:
        r9 = move-exception;
        r0 = "FavoritesProvider";	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        r1 = r9.toString();	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        android.util.Log.e(r0, r1);	 Catch:{ Exception -> 0x0058, all -> 0x007a }
        if (r8 == 0) goto L_0x006d;
    L_0x0064:
        r0 = r8.isClosed();
        if (r0 != 0) goto L_0x006d;
    L_0x006a:
        r8.close();
    L_0x006d:
        return;
    L_0x006e:
        if (r8 == 0) goto L_0x006d;
    L_0x0070:
        r0 = r8.isClosed();
        if (r0 != 0) goto L_0x006d;
    L_0x0076:
        r8.close();
        goto L_0x006d;
    L_0x007a:
        r0 = move-exception;
        if (r8 == 0) goto L_0x0086;
    L_0x007d:
        r1 = r8.isClosed();
        if (r1 != 0) goto L_0x0086;
    L_0x0083:
        r8.close();
    L_0x0086:
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.model.FavoritesProvider.deleteWidgetIds(java.lang.String):void");
    }

    private FavoritesProvider() {
    }

    public static FavoritesProvider getInstance() {
        if (sContext != null) {
            return sInstance;
        }
        throw new IllegalStateException("FavoritesProvider inited before app context set");
    }

    private void migrationTable(boolean isDaliMigration, String tableName, String screenTableName, long userSerial, boolean convertAppOrder) {
        SharedPreferences prefs = sContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        String tempTableName = tableName + "_tmp";
        String escapedTableName = DatabaseUtils.sqlEscapeString(tableName);
        String escapedTempTableName = DatabaseUtils.sqlEscapeString(tempTableName);
        String escapedScreenTableName = DatabaseUtils.sqlEscapeString(screenTableName);
        Editor prefsEditor = prefs.edit();
        Cursor ti = null;
        try {
            int pageCount;
            int defaultPageIndex;
            if (tableExists(tableName)) {
                createFavoritesTable(userSerial, tempTableName);
                ArrayList<String> colList = new ArrayList();
                ti = sDb.rawQuery("PRAGMA table_info(" + escapedTableName + ")", null);
                if (ti != null) {
                    if (ti.moveToFirst()) {
                        while (true) {
                            colList.add(ti.getString(1));
                            if (!ti.moveToNext()) {
                                break;
                            }
                        }
                    }
                    ti.close();
                }
                if (isDaliMigration) {
                    colList.remove("containerId");
                    colList.remove("preview");
                    colList.remove("show");
                    colList.remove("iconMode");
                    colList.remove("isSystemApp");
                } else {
                    colList.remove("iconMovieUri");
                    colList.remove("secret");
                    colList.remove("timeStamp");
                }
                String sql = "INSERT INTO " + escapedTempTableName + " (";
                StringBuilder cols = new StringBuilder();
                cols.append("");
                int colCnt = 0;
                Iterator it = colList.iterator();
                while (it.hasNext()) {
                    cols.append((String) it.next());
                    colCnt++;
                    if (colCnt < colList.size()) {
                        cols.append(", ");
                    }
                }
                sDb.execSQL((sql + cols.toString() + ") ") + "SELECT " + cols.toString() + " FROM " + escapedTableName);
                sDb.execSQL("DROP TABLE " + escapedTableName);
                sDb.execSQL("ALTER TABLE " + escapedTempTableName + " RENAME TO " + escapedTableName);
            } else {
                Log.i(TAG, "no table exist : " + tableName);
                createFavoritesTable(userSerial, tableName);
            }
            if (convertAppOrder) {
                convertAppsTable(tableName, prefs.getInt("Workspace.CellX", 5));
                migrationAppsButton(tableName);
            }
            AppWidgetManager widgets = AppWidgetManager.getInstance(sContext);
            ArrayList<Pair<ComponentName, Long>> widgetList = new ArrayList();
            ti = sDb.query(tableName, new String[]{Favorites.APPWIDGET_ID}, "appWidgetId > 0", null, null, null, null);
            if (ti != null) {
                if (ti.moveToFirst()) {
                    while (true) {
                        AppWidgetProviderInfo provider = widgets.getAppWidgetInfo((int) ti.getLong(0));
                        if (provider != null) {
                            widgetList.add(Pair.create(provider.provider, Long.valueOf(ti.getLong(0))));
                        }
                        if (!ti.moveToNext()) {
                            break;
                        }
                    }
                }
                ti.close();
            }
            Iterator it2 = widgetList.iterator();
            while (it2.hasNext()) {
                Pair<ComponentName, Long> p = (Pair) it2.next();
                Log.i(TAG, "(" + p.second + ")" + ((ComponentName) p.first).flattenToShortString() + " widget added.");
                SQLiteStatement update = sDb.compileStatement("UPDATE " + tableName + " SET appWidgetProvider=? WHERE appWidgetId=?");
                update.bindString(1, ((ComponentName) p.first).flattenToString());
                update.bindLong(2, ((Long) p.second).longValue());
                update.execute();
                update.close();
            }
            if (isDaliMigration) {
                sContext.deleteDatabase(DALI_PAGE_SETTING_DB);
                sContext.deleteDatabase("widgets.db");
                pageCount = this.mDaliPageCnt + 1;
                defaultPageIndex = prefs.getInt("homescreenindex", 0);
            } else {
                pageCount = prefs.getInt(convertAppOrder ? "screencount" : "screencount.homeonly", 0);
                defaultPageIndex = prefs.getInt(convertAppOrder ? "homescreenindex" : "homescreenindex.homeonly", 0);
                if (prefs.getInt("screencount.briefing", 0) > 0 && pageCount > 0) {
                    pageCount--;
                    defaultPageIndex--;
                    sDb.execSQL("UPDATE " + escapedTableName + " SET " + "screen" + '=' + "screen" + "-1 WHERE " + "container" + '=' + -100);
                }
            }
            String str = convertAppOrder ? LauncherFiles.HOME_DEFAULT_PAGE_KEY : LauncherFiles.HOMEONLY_DEFAULT_PAGE_KEY;
            if (defaultPageIndex < 0) {
                defaultPageIndex = 0;
            }
            prefsEditor.putInt(str, defaultPageIndex).apply();
            if (!tableExists(screenTableName)) {
                createScreensTable(screenTableName);
            }
            for (int i = 0; i < pageCount; i++) {
                sDb.execSQL("INSERT INTO " + escapedScreenTableName + " (" + "_id" + ',' + WorkspaceScreens.SCREEN_RANK + ") VALUES (" + i + ',' + i + ");");
            }
            sDb.execSQL("UPDATE " + escapedTableName + " SET RESTORED=0");
        } finally {
            if (!(ti == null || ti.isClosed())) {
                ti.close();
            }
        }
    }

    public boolean migrateTable(long userSerial, int oldVersion, int newVersion) {
        if (sDb == null) {
            throw new RuntimeException("DB file is null state for migration.");
        } else if (oldVersion >= 30) {
            return false;
        } else {
            boolean ret = false;
            try {
                sDb.beginTransaction();
                SharedPreferences prefs = sContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
                Editor prefsEditor = prefs.edit();
                boolean isDaliMigration = new File(sContext.getDatabasePath(DALI_PAGE_SETTING_DB).getParent(), DALI_PAGE_SETTING_DB).exists();
                String oldZeroPagePrefKey = "home_briefing_enable";
                if (prefs.contains("home_briefing_enable")) {
                    migrateZeroPagePrefs(prefs, prefsEditor, "home_briefing_enable");
                }
                if (isDaliMigration) {
                    prevMigrationForDali();
                    migrationTable(true, "favorites", "workspaceScreens", userSerial, true);
                    createFavoritesTable(userSerial, "favorites_homeOnly");
                    createScreensTable("workspaceScreens_homeOnly");
                } else {
                    if (prefs.contains(LauncherAppState.HOME_ONLY_MODE)) {
                        if (prefs.getBoolean(LauncherAppState.HOME_ONLY_MODE, false)) {
                            migrationTable(false, "favorites", "workspaceScreens", userSerial, false);
                            migrationTable(false, "favorites_homeApps", "workspaceScreens_homeApps", userSerial, true);
                        }
                    }
                    migrationTable(false, "favorites", "workspaceScreens", userSerial, true);
                    migrationTable(false, "favorites_homeOnly", "workspaceScreens_homeOnly", userSerial, false);
                }
                migrationTableForEasyMode(userSerial);
                migrateSharedPrefsForApps(prefs, prefsEditor);
                sDb.execSQL("DROP TABLE IF EXISTS appOrder");
                prefsEditor.remove("MoveApps_Help_Shown");
                prefsEditor.remove("screencount");
                prefsEditor.remove("homescreenindex");
                prefsEditor.remove(WhiteBgManager.PREFERENCES_NEED_DARK_FONT);
                prefsEditor.remove("emptypages");
                prefsEditor.apply();
                sDb.setTransactionSuccessful();
                ret = true;
                return ret;
            } catch (Exception e) {
                Log.e(TAG, "migrateTable converting error : " + e.getMessage());
                return ret;
            } finally {
                sDb.endTransaction();
            }
        }
    }

    public long getMaxScreenId() {
        return this.mMaxScreenId;
    }

    public void setMaxScreenId(long id) {
        this.mMaxScreenId = id;
    }

    public void createTable(long userSerial) {
        createFavoritesTable(userSerial, "favorites");
        createScreensTable("workspaceScreens");
        if (LauncherFeature.supportHomeModeChange()) {
            createFavoritesTable(userSerial, "favorites_homeOnly");
            createScreensTable("workspaceScreens_homeOnly");
        }
        if (LauncherFeature.supportEasyModeChange()) {
            createFavoritesTable(userSerial, "favorites_easy");
            createScreensTable("workspaceScreens_easy");
        }
    }

    private void createFavoritesTable(long userSerial, String tableName) {
        sDb.execSQL("CREATE TABLE " + tableName + " (" + "_id" + " INTEGER PRIMARY KEY," + "title" + " TEXT," + "container" + " INTEGER," + "screen" + " INTEGER," + "cellX" + " INTEGER," + "cellY" + " INTEGER," + "spanX" + " INTEGER," + "spanY" + " INTEGER," + "itemType" + " INTEGER," + BaseLauncherColumns.RANK + " INTEGER NOT NULL DEFAULT 0," + "intent" + " TEXT," + Favorites.APPWIDGET_ID + " INTEGER NOT NULL DEFAULT -1," + Favorites.APPWIDGET_PROVIDER + " TEXT," + BaseLauncherColumns.ICON_TYPE + " INTEGER," + "iconPackage" + " TEXT," + "iconResource" + " TEXT," + "icon" + " BLOB," + ChangeLogColumns.MODIFIED + " INTEGER NOT NULL DEFAULT 0," + "restored" + " INTEGER NOT NULL DEFAULT 0," + BaseLauncherColumns.PROFILE_ID + " INTEGER DEFAULT " + userSerial + "," + BaseLauncherColumns.OPTIONS + " INTEGER NOT NULL DEFAULT 0," + BaseLauncherColumns.COLOR + " INTEGER NOT NULL DEFAULT -1," + "hidden" + " INTEGER NOT NULL DEFAULT 0," + BaseLauncherColumns.NEWCUE + " INTEGER NOT NULL DEFAULT 0," + Favorites.FESTIVAL + " INTEGER NOT NULL DEFAULT 0," + ChangeLogColumns.LOCK + " INTEGER NOT NULL DEFAULT 0" + ");");
    }

    private void createScreensTable(String tableName) {
        sDb.execSQL("CREATE TABLE " + tableName + " (" + "_id" + " INTEGER PRIMARY KEY," + WorkspaceScreens.SCREEN_RANK + " INTEGER," + ChangeLogColumns.MODIFIED + " INTEGER NOT NULL DEFAULT 0," + ChangeLogColumns.LOCK + " INTEGER NOT NULL DEFAULT 0" + ");");
    }

    public void deleteTable() {
        try {
            sDb.beginTransaction();
            sDb.execSQL("DROP TABLE IF EXISTS favorites");
            sDb.execSQL("DROP TABLE IF EXISTS favorites_homeOnly");
            sDb.execSQL("DROP TABLE IF EXISTS favorites_homeApps");
            sDb.execSQL("DROP TABLE IF EXISTS favorites_easy");
            sDb.execSQL("DROP TABLE IF EXISTS favorites_standard");
            sDb.execSQL("DROP TABLE IF EXISTS workspaceScreens");
            sDb.execSQL("DROP TABLE IF EXISTS workspaceScreens_homeOnly");
            sDb.execSQL("DROP TABLE IF EXISTS workspaceScreens_homeApps");
            sDb.execSQL("DROP TABLE IF EXISTS workspaceScreens_easy");
            sDb.execSQL("DROP TABLE IF EXISTS workspaceScreens_standard");
            sDb.execSQL("DROP TABLE IF EXISTS appOrder");
            sDb.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "deleteTable converting error : " + e.getMessage());
        } finally {
            sDb.endTransaction();
        }
    }

    public void deleteTable(String tableName) {
        Log.i(TAG, "deleteTable : " + tableName);
        if (tableExists(tableName)) {
            sDb.delete(tableName, null, null);
        }
    }

    private ArrayList<String> getColumnList() {
        ArrayList<String> colList = new ArrayList();
        colList.add("_id");
        colList.add("title");
        colList.add("container");
        colList.add("screen");
        colList.add("cellX");
        colList.add("cellY");
        colList.add("spanX");
        colList.add("spanY");
        colList.add("itemType");
        colList.add(BaseLauncherColumns.RANK);
        colList.add("intent");
        colList.add(Favorites.APPWIDGET_ID);
        colList.add(Favorites.APPWIDGET_PROVIDER);
        colList.add(BaseLauncherColumns.ICON_TYPE);
        colList.add("iconPackage");
        colList.add("iconResource");
        colList.add("icon");
        colList.add(ChangeLogColumns.MODIFIED);
        colList.add("restored");
        colList.add(BaseLauncherColumns.PROFILE_ID);
        colList.add(BaseLauncherColumns.OPTIONS);
        colList.add(BaseLauncherColumns.COLOR);
        colList.add("hidden");
        colList.add(BaseLauncherColumns.NEWCUE);
        colList.add(Favorites.FESTIVAL);
        colList.add(ChangeLogColumns.LOCK);
        return colList;
    }

    public boolean checkTable() {
        ArrayList<String> colList = getColumnList();
        boolean ret = true;
        Cursor ti = null;
        try {
            ti = sDb.rawQuery("PRAGMA table_info(favorites)", null);
            if (ti == null) {
                ret = false;
                Log.e(TAG, "checkTable : query cursor is null.");
            } else if (colList.size() != ti.getCount()) {
                int expectSize = colList.size();
                int realSize = ti.getCount();
                Log.e(TAG, "checkTable : Column list size isn't matching with reference.");
                Log.e(TAG, "checkTable : expect: " + expectSize + ", real: " + realSize);
                ret = false;
            } else {
                do {
                    if (!ti.moveToNext()) {
                        break;
                    }
                } while (colList.contains(ti.getString(1)));
                Log.e(TAG, "checkTable : The field name [" + ti.getString(1) + "] does not exist.");
                ret = false;
            }
            if (!(ti == null || ti.isClosed())) {
                ti.close();
            }
        } catch (Exception e) {
            ret = false;
            Log.e(TAG, "checkTable : " + e.getMessage());
            if (!(ti == null || ti.isClosed())) {
                ti.close();
            }
        } catch (Throwable th) {
            if (!(ti == null || ti.isClosed())) {
                ti.close();
            }
        }
        return ret;
    }

    public void removeChangedComponentPref() {
        Editor editor = sContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
        editor.remove(CHECK_CHANGED_COMPONENT_EXITST);
        editor.remove("checkChangedComponentVersion_HomeOnly");
        editor.apply();
    }

    private HashMap<String, String> loadChangedComponent() {
        HashMap<String, String> changedList = new HashMap();
        String[] cmpList = sContext.getResources().getStringArray(R.array.changed_component_list);
        if (cmpList.length > 0) {
            for (String cmp : cmpList) {
                String[] key = cmp.split(GRID_INFO_SPLIT);
                if (key.length == 2) {
                    key[0] = key[0].trim();
                    ComponentName before = ComponentName.unflattenFromString(key[0]);
                    key[1] = key[1].trim();
                    ComponentName after = ComponentName.unflattenFromString(key[1]);
                    if (!(before == null || after == null)) {
                        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(sContext);
                        UserHandleCompat user = UserHandleCompat.myUserHandle();
                        List<LauncherActivityInfoCompat> beforeMatches = launcherApps.getActivityList(before.getPackageName(), user);
                        List<LauncherActivityInfoCompat> afterMatches = launcherApps.getActivityList(after.getPackageName(), user);
                        if (!afterMatches.isEmpty() && beforeMatches.isEmpty()) {
                            changedList.put(key[0], key[1]);
                        }
                        if (!beforeMatches.isEmpty() && afterMatches.isEmpty()) {
                            changedList.put(key[1], key[0]);
                        }
                    }
                }
            }
        }
        return changedList;
    }

    void checkChangedComponentExist(boolean isEmptyDb) {
        String versionName = null;
        try {
            PackageInfo info = sContext.getPackageManager().getPackageInfo(sContext.getPackageName(), 0);
            if (info != null) {
                versionName = info.versionName;
            }
        } catch (NameNotFoundException e) {
            Log.i(TAG, "checkChangedComponentExist NameNotFoundException");
        }
        String prefsKey = CHECK_CHANGED_COMPONENT_EXITST;
        if (LauncherAppState.getInstance().isEasyModeEnabled()) {
            prefsKey = prefsKey + "_Easy";
        } else if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
            prefsKey = prefsKey + "_HomeOnly";
        }
        SharedPreferences prefs = sContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        String prevVersionName = prefs.getString(prefsKey, null);
        Log.d(TAG, "checkChangedComponentExist PREF_KEY : " + prefsKey + " prevVersionName : " + prevVersionName);
        if (versionName != null && !versionName.equals(prevVersionName)) {
            HashMap<String, String> changedList = loadChangedComponent();
            String[] projection = new String[]{"_id", "intent"};
            if (!changedList.isEmpty()) {
                for (String before : changedList.keySet()) {
                    String after = (String) changedList.get(before);
                    LongSparseArray<String> changedItems = new LongSparseArray();
                    ContentResolver cr = sContext.getContentResolver();
                    Cursor c = cr.query(Favorites.CONTENT_URI, projection, "intent like '%" + before + "%'", null, null);
                    if (c == null) {
                        Log.d(TAG, "checkChangedComponentExist cursor is null");
                        return;
                    }
                    long id;
                    String intent;
                    while (c.moveToNext()) {
                        try {
                            id = c.getLong(0);
                            intent = c.getString(1).replace(before, after);
                            if (id > 0) {
                                changedItems.put(id, intent);
                            }
                        } finally {
                            if (!c.isClosed()) {
                                c.close();
                            }
                        }
                    }
                    ContentValues values = new ContentValues();
                    for (int i = 0; i < changedItems.size(); i++) {
                        values.clear();
                        id = changedItems.keyAt(i);
                        intent = (String) changedItems.get(id);
                        Uri uri = Favorites.getContentUri(id);
                        values.put("intent", intent);
                        if (isEmptyDb) {
                            values.put("itemType", Integer.valueOf(0));
                        }
                        cr.update(uri, values, null, null);
                        Log.d(TAG, "Changed component updated : " + before + " to " + after);
                    }
                }
                changePackageForManagedProfile(changedList);
            }
            Editor editor = prefs.edit();
            editor.putString(prefsKey, versionName);
            editor.apply();
        }
    }

    private void changePackageForManagedProfile(HashMap<String, String> componentNameMap) {
        UserManagerCompat userManager = UserManagerCompat.getInstance(sContext);
        UserHandleCompat myUser = UserHandleCompat.myUserHandle();
        SharedPreferences prefs = sContext.getSharedPreferences(LauncherFiles.MANAGED_USER_PREFERENCES_KEY, 0);
        boolean isChangeList = false;
        for (UserHandleCompat user : userManager.getUserProfiles()) {
            if (!myUser.equals(user)) {
                String packageSetKey = (LauncherAppState.getInstance().isHomeOnlyModeEnabled() ? Utilities.INSTALLED_PACKAGES_PREFIX_HOME_ONLY : Utilities.INSTALLED_PACKAGES_PREFIX) + userManager.getSerialNumberForUser(user);
                Set<String> userApps = prefs.getStringSet(packageSetKey, null);
                Log.d(TAG, "changePackageForManagedProfile user package key : " + packageSetKey);
                if (userApps != null) {
                    for (String componentName : componentNameMap.keySet()) {
                        ComponentName before = ComponentName.unflattenFromString(componentName);
                        if (userApps.remove(before.getPackageName())) {
                            ComponentName after = ComponentName.unflattenFromString((String) componentNameMap.get(componentName));
                            userApps.add(after.getPackageName());
                            Log.d(TAG, "changePackageForManagedProfile remove : " + before.getPackageName() + " add : " + after.getPackageName());
                            isChangeList = true;
                        }
                    }
                    if (isChangeList) {
                        prefs.edit().putStringSet(packageSetKey, userApps).apply();
                    }
                }
            }
        }
    }

    public boolean tableExists(String tableName) {
        if (sDb == null) {
            return false;
        }
        boolean exist = false;
        Cursor c = sDb.query(true, "sqlite_master", new String[]{"tbl_name"}, "tbl_name = ?", new String[]{tableName}, null, null, null, null, null);
        if (c != null) {
            try {
                exist = c.getCount() > 0;
                c.close();
            } catch (Throwable th) {
                c.close();
            }
        }
        Log.i(TAG, "tableExists tableName : " + tableName + " exist : " + exist);
        return exist;
    }

    private boolean tableExistsForEasy(SQLiteDatabase db) {
        if (db == null) {
            return false;
        }
        boolean exist = false;
        Cursor c = db.query(true, "sqlite_master", new String[]{"tbl_name"}, "tbl_name = ?", new String[]{"favorites"}, null, null, null, null, null);
        if (c != null) {
            try {
                exist = c.getCount() > 0;
                c.close();
            } catch (Throwable th) {
                c.close();
            }
        }
        Log.i(TAG, "tableExists tableName : favorites exist : " + exist);
        return exist;
    }

    public void deleteWidgetHost(final LauncherProviderChangeListener listener) {
        try {
            this.mAppWidgetHost.deleteHost();
        } catch (Exception e) {
            if (!TestHelper.isRoboUnitTest()) {
                e.printStackTrace();
                throw e;
            }
        }
        new MainThreadExecutor().execute(new Runnable() {
            public void run() {
                if (listener != null) {
                    listener.onAppWidgetHostReset();
                }
            }
        });
    }

    public long generateNewScreenId() {
        if (this.mMaxScreenId < 0) {
            throw new RuntimeException("Error: max screen id was not initialized");
        }
        this.mMaxScreenId++;
        return this.mMaxScreenId;
    }

    public boolean initializeExternalAdd(ContentValues values) {
        if (LauncherFeature.supportSprintExtension()) {
            Log.d(TAG, " [SPRINT] - skip generating new ID for new rows as it's already created");
        } else {
            values.put("_id", Long.valueOf(generateNewItemId()));
        }
        Integer itemType = values.getAsInteger("itemType");
        if (!(itemType == null || itemType.intValue() != 4 || values.containsKey(Favorites.APPWIDGET_ID))) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(sContext);
            ComponentName cn = ComponentName.unflattenFromString(values.getAsString(Favorites.APPWIDGET_PROVIDER));
            if (cn == null) {
                return false;
            }
            try {
                int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
                values.put(Favorites.APPWIDGET_ID, Integer.valueOf(appWidgetId));
                if (!appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn)) {
                    return false;
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to initialize external widget", e);
                return false;
            }
        }
        if (values.getAsInteger("container") != null && values.getAsInteger("container").intValue() == -100) {
            Long screenId = values.getAsLong("screen");
            if (!(screenId == null || addScreenIdIfNecessary(screenId.longValue()))) {
                return false;
            }
        }
        return true;
    }

    private boolean addScreenIdIfNecessary(long screenId) {
        if (!hasScreenId(screenId)) {
            int rank = getMaxScreenRank() + 1;
            ContentValues v = new ContentValues();
            v.put("_id", Long.valueOf(screenId));
            v.put(WorkspaceScreens.SCREEN_RANK, Integer.valueOf(rank));
            if (LauncherProvider.dbInsertAndCheck(sDb, "workspaceScreens", null, v) < 0) {
                return false;
            }
        }
        LauncherProviderID providerID = LauncherAppState.getLauncherProviderID();
        if (providerID != null) {
            Log.d(TAG, "[SPRINT] Adding screen is necessary. Updating screen count");
            providerID.updateScreenCount();
        }
        return true;
    }

    private boolean hasScreenId(long screenId) {
        String[] selectionArgs = new String[]{String.valueOf(screenId)};
        Cursor c = sDb.query("workspaceScreens", null, "_id = ?", selectionArgs, null, null, null);
        if (c == null) {
            return false;
        }
        int count = c.getCount();
        c.close();
        if (count > 0) {
            return true;
        }
        return false;
    }

    public int getItemCount(String tableName) {
        Cursor c = sDb.query(tableName, new String[]{"COUNT(*)"}, null, null, null, null, null);
        int count = 0;
        if (c != null && c.moveToNext()) {
            count = c.getInt(0);
        }
        if (c != null) {
            c.close();
        }
        return count;
    }

    private int getMaxScreenRank() {
        Cursor c = sDb.query("workspaceScreens", new String[]{"MAX(screenRank)"}, null, null, null, null, null);
        int rank = -1;
        if (c != null && c.moveToNext()) {
            rank = c.getInt(0);
        }
        if (c != null) {
            c.close();
        }
        return rank;
    }

    private void getHideItems(HashSet<String> hideItems) {
        String[] columns = new String[]{"intent", BaseLauncherColumns.PROFILE_ID, "hidden"};
        String[] selectionArg = new String[]{String.valueOf(0)};
        Cursor c = sDb.query("favorites", columns, "hidden != ?", selectionArg, null, null, null);
        if (c != null) {
            int intentIndex = c.getColumnIndexOrThrow("intent");
            int profileIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.PROFILE_ID);
            int hiddenIndex = c.getColumnIndexOrThrow("hidden");
            while (c.moveToNext()) {
                if ((2 & ((long) c.getInt(hiddenIndex))) != 0) {
                    String intentDescription = c.getString(intentIndex);
                    if (intentDescription != null) {
                        long profile = c.getLong(profileIndex);
                        try {
                            hideItems.add(Intent.parseUri(intentDescription, 0).getComponent().flattenToShortString() + "," + String.valueOf(profile));
                        } catch (URISyntaxException e) {
                            Log.e(TAG, "Unable to parse intent", e);
                        } catch (Throwable th) {
                            c.close();
                        }
                    } else {
                        continue;
                    }
                }
            }
            c.close();
        }
    }

    public void applyHideItem(Set<String> hideItems) {
        int idIndex;
        String[] strArr;
        HashMap<String, Boolean> hideItemsMap = new HashMap();
        for (String item : hideItems) {
            hideItemsMap.put(item, Boolean.valueOf(false));
        }
        sDb.beginTransaction();
        Cursor c = sDb.query("favorites", new String[]{"_id", "container", "intent", BaseLauncherColumns.PROFILE_ID, "hidden"}, "itemType=?", new String[]{String.valueOf(0)}, null, null, null);
        ArrayList<Long> deleteItemList = new ArrayList();
        ArrayList<Long> hideDesktopItemList = new ArrayList();
        ArrayList<Long> hideAppsItemList = new ArrayList();
        LongSparseArray<Long> folderItemList = new LongSparseArray();
        boolean isHomeOnly = LauncherFeature.supportHomeModeChange() && LauncherAppState.getInstance().isHomeOnlyModeEnabled();
        if (c != null) {
            idIndex = c.getColumnIndexOrThrow("_id");
            int containerIndex = c.getColumnIndexOrThrow("container");
            int intentIndex = c.getColumnIndexOrThrow("intent");
            int profileIndex = c.getColumnIndexOrThrow(BaseLauncherColumns.PROFILE_ID);
            int hiddenIndex = c.getColumnIndexOrThrow("hidden");
            while (c.moveToNext()) {
                String intentDescription = c.getString(intentIndex);
                if (intentDescription != null) {
                    Intent intent;
                    try {
                        intent = Intent.parseUri(intentDescription, 0);
                    } catch (Throwable e) {
                        Log.e(TAG, "Unable to parse intent", e);
                    }
                    try {
                        long id = c.getLong(idIndex);
                        long container = c.getLong(containerIndex);
                        long profile = c.getLong(profileIndex);
                        int hidden = c.getInt(hiddenIndex);
                        String componentAndProfile = intent.getComponent().flattenToShortString() + "," + String.valueOf(profile);
                        if (hideItemsMap.containsKey(componentAndProfile)) {
                            hideItemsMap.put(componentAndProfile, Boolean.valueOf(true));
                            if (hidden == 0) {
                                if (isHomeOnly) {
                                    hideDesktopItemList.add(Long.valueOf(id));
                                } else if (container == -100 || container == -101) {
                                    deleteItemList.add(Long.valueOf(id));
                                } else if (container == -102) {
                                    hideAppsItemList.add(Long.valueOf(id));
                                } else {
                                    folderItemList.put(id, Long.valueOf(container));
                                }
                            }
                        } else if (hidden == 2) {
                            deleteItemList.add(Long.valueOf(id));
                        }
                    } catch (Throwable th) {
                        Throwable th2 = th;
                        sDb.endTransaction();
                        throw th2;
                    }
                }
            }
            c.close();
        }
        ArrayList<Long> foldersInApps = new ArrayList();
        ArrayList<Long> foldersInHome = new ArrayList();
        String[] columns = new String[]{"_id"};
        try {
            String[] selectionArg = new String[]{String.valueOf(2), String.valueOf(Favorites.CONTAINER_APPS)};
        } catch (Throwable th3) {
            th2 = th3;
            strArr = columns;
            sDb.endTransaction();
            throw th2;
        }
        try {
            c = sDb.query("favorites", columns, "itemType=? AND container=?", selectionArg, null, null, null);
            if (c != null) {
                idIndex = c.getColumnIndexOrThrow("_id");
                while (c.moveToNext()) {
                    foldersInApps.add(Long.valueOf(c.getLong(idIndex)));
                }
                c.close();
            }
            c = sDb.query("favorites", columns, "itemType=? AND (container=? OR container=?)", new String[]{String.valueOf(2), String.valueOf(-100), String.valueOf(Favorites.CONTAINER_HOTSEAT)}, null, null, null);
            if (c != null) {
                idIndex = c.getColumnIndexOrThrow("_id");
                while (c.moveToNext()) {
                    foldersInHome.add(Long.valueOf(c.getLong(idIndex)));
                }
                c.close();
            }
            int folderListSize = folderItemList.size();
            for (int i = 0; i < folderListSize; i++) {
                Long key = Long.valueOf(folderItemList.keyAt(i));
                Long container2 = (Long) folderItemList.get(key.longValue());
                if (foldersInApps.contains(container2)) {
                    hideAppsItemList.add(key);
                } else if (foldersInHome.contains(container2)) {
                    deleteItemList.add(key);
                }
            }
            updateHideItems(deleteItemList, hideDesktopItemList, hideAppsItemList);
            insertRemainHideItems(hideItemsMap, isHomeOnly);
            sDb.setTransactionSuccessful();
            sDb.endTransaction();
        } catch (Throwable th4) {
            th2 = th4;
            String[] strArr2 = selectionArg;
            strArr = columns;
            sDb.endTransaction();
            throw th2;
        }
    }

    private void updateHideItems(ArrayList<Long> deleteItemList, ArrayList<Long> hideDesktopItemList, ArrayList<Long> hideAppsItemList) {
        if (!deleteItemList.isEmpty()) {
            Log.d(TAG, "remove hideItmes " + TextUtils.join(", ", deleteItemList));
            sDb.delete("favorites", Utilities.createDbSelectionQuery("_id", deleteItemList), null);
        }
        ContentValues values = new ContentValues();
        values.put("hidden", Integer.valueOf(2));
        if (!hideAppsItemList.isEmpty()) {
            Log.d(TAG, "update hideItmes for Apps " + TextUtils.join(", ", hideAppsItemList));
            values.put("container", Integer.valueOf(Favorites.CONTAINER_APPS));
            values.put("screen", Integer.valueOf(0));
            sDb.update("favorites", values, Utilities.createDbSelectionQuery("_id", hideAppsItemList), null);
        }
        if (!hideDesktopItemList.isEmpty()) {
            Log.d(TAG, "update hideItmes for Home" + TextUtils.join(", ", hideDesktopItemList));
            values.put("container", Integer.valueOf(-100));
            values.put("screen", Integer.valueOf(-1));
            sDb.update("favorites", values, Utilities.createDbSelectionQuery("_id", hideDesktopItemList), null);
        }
    }

    private void insertRemainHideItems(HashMap<String, Boolean> homeItemsMap, boolean isHomeOnly) {
        for (String key : homeItemsMap.keySet()) {
            if (!((Boolean) homeItemsMap.get(key)).booleanValue()) {
                long container;
                long screen;
                long profile = 0;
                String component = "";
                String[] arr = key.split(",");
                if (arr.length == 2) {
                    component = arr[0];
                    profile = Long.valueOf(arr[1]).longValue();
                }
                if (isHomeOnly) {
                    container = -100;
                    screen = getMaxScreenId();
                } else {
                    container = -102;
                    screen = 0;
                }
                insertHideItem(component, container, screen, profile);
            }
        }
    }

    private String makeFoldersIdToString(ArrayList<Long> folders) {
        StringJoiner joiner = new StringJoiner(",");
        Iterator it = folders.iterator();
        while (it.hasNext()) {
            joiner.append(((Long) it.next()).longValue());
        }
        return joiner.toString();
    }

    public void copyFavoritesForHomeOnly() {
        Log.d(TAG, "copyFavorites : homeApps layout -> homeOnly layout");
        sDb.beginTransaction();
        ArrayList<Long> folderIds = new ArrayList();
        Cursor cursor;
        try {
            String[] selectionArg = new String[]{String.valueOf(2), String.valueOf(-100), String.valueOf(Favorites.CONTAINER_HOTSEAT)};
            cursor = sDb.query("favorites", new String[]{"_id"}, "itemType=? AND (container=? OR container=?)", selectionArg, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    folderIds.add(Long.valueOf(cursor.getLong(0)));
                }
                cursor.close();
            }
            sDb.delete("workspaceScreens_homeOnly", null, null);
            sDb.execSQL("INSERT INTO workspaceScreens_homeOnly SELECT * FROM workspaceScreens");
            String copySQL = "INSERT INTO favorites_homeOnly SELECT * FROM favorites WHERE container=-100 OR container=-101";
            if (folderIds.size() > 0) {
                copySQL = copySQL + " OR container in (" + makeFoldersIdToString(folderIds) + ')';
            }
            sDb.execSQL(copySQL);
            sDb.delete("favorites_homeOnly", "intent like '%" + Utilities.ACTION_SHOW_APPS_VIEW + "%'", null);
            bindAppWidgetForHomeOnly();
            removeAppShortcutForHomeOnly();
            copyAppsItemsToHome(removeDuplicateItemForHomeOnly());
            sDb.setTransactionSuccessful();
            copyPreferenceForHomeOnly();
            sDb.endTransaction();
        } catch (SQLException ex) {
            try {
                Log.d(TAG, "Error in copy favorites for homeonly : " + ex.getMessage());
            } finally {
                sDb.endTransaction();
            }
        } catch (Throwable th) {
            cursor.close();
        }
    }

    private void bindAppWidgetForHomeOnly() {
        ArrayList<Long> removeWidgets = new ArrayList();
        LongSparseArray<Integer> updateWidgets = new LongSparseArray();
        String[] columns = new String[]{"_id", Favorites.APPWIDGET_PROVIDER, BaseLauncherColumns.PROFILE_ID};
        String[] selectionArg = new String[]{String.valueOf(4)};
        Cursor cursor = sDb.query("favorites_homeOnly", columns, "itemType=?", selectionArg, null, null, null);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(sContext);
        UserManagerCompat userManager = UserManagerCompat.getInstance(sContext);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
                    long id = cursor.getLong(0);
                    String appWidgetProvider = cursor.getString(1);
                    long profileId = cursor.getLong(2);
                    if (appWidgetProvider != null) {
                        ComponentName cn = ComponentName.unflattenFromString(appWidgetProvider);
                        if (appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, userManager.getUserForSerialNumber(profileId).getUser(), cn, null)) {
                            updateWidgets.put(id, Integer.valueOf(appWidgetId));
                        } else {
                            Log.e(TAG, "Unable to bind app widget during copy for homeonly " + cn);
                            removeWidgets.add(Long.valueOf(id));
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        ContentValues values = new ContentValues();
        int count = updateWidgets.size();
        for (int i = 0; i < count; i++) {
            long dbId = updateWidgets.keyAt(i);
            String selection = "_id=" + dbId;
            values.clear();
            values.put(Favorites.APPWIDGET_ID, (Integer) updateWidgets.get(dbId));
            sDb.update("favorites_homeOnly", values, selection, null);
        }
        Log.d(TAG, "bindAppWidgetForHomeOnly removeAppwidget size " + removeWidgets.size());
        if (!removeWidgets.isEmpty()) {
            Log.d(TAG, "remove AppWidget " + TextUtils.join(", ", removeWidgets));
            sDb.delete("favorites_homeOnly", Utilities.createDbSelectionQuery("_id", removeWidgets), null);
        }
    }

    private void removeAppShortcutForHomeOnly() {
        ArrayList<Long> appShortcutIds = new ArrayList();
        String[] selectionArg = new String[]{String.valueOf(1)};
        Cursor cursor = sDb.query("favorites_homeOnly", new String[]{"_id", "intent", BaseLauncherColumns.PROFILE_ID}, "itemType=?", selectionArg, null, null, null);
        if (cursor != null) {
            try {
                UserManagerCompat userManager = UserManagerCompat.getInstance(sContext);
                LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(sContext);
                while (cursor.moveToNext()) {
                    Intent intent = Intent.parseUri(cursor.getString(1), 0);
                    if (Utilities.isLauncherAppTarget(intent)) {
                        ComponentName cn = intent.getComponent();
                        UserHandleCompat user = userManager.getUserForSerialNumber(cursor.getLong(2));
                        for (LauncherActivityInfoCompat app : launcherApps.getActivityList(cn.getPackageName(), user)) {
                            if (cn.equals(app.getComponentName())) {
                                appShortcutIds.add(Long.valueOf(cursor.getLong(0)));
                                break;
                            }
                        }
                    }
                }
                cursor.close();
            } catch (URISyntaxException e) {
                Log.e(TAG, "Unable to parse intent during removeAppShortcutForHomeOnly", e);
            } catch (Throwable th) {
                cursor.close();
            }
        }
        Log.d(TAG, "removeAppShortcutForHomeOnly size " + appShortcutIds.size());
        if (!appShortcutIds.isEmpty()) {
            Log.d(TAG, "remove AppShortcut " + TextUtils.join(", ", appShortcutIds));
            sDb.delete("favorites_homeOnly", Utilities.createDbSelectionQuery("_id", appShortcutIds), null);
        }
    }

    private ArrayList<ComponentKey> removeDuplicateItemForHomeOnly() {
        ArrayList<Long> duplicateItemIds = new ArrayList();
        ArrayList<ComponentKey> cmpList = new ArrayList();
        UserManagerCompat userManager = UserManagerCompat.getInstance(sContext);
        String[] selectionArg = new String[]{String.valueOf(0)};
        String[] columns = new String[]{"_id", "intent", BaseLauncherColumns.PROFILE_ID};
        Cursor cursor = sDb.query("favorites_homeOnly", columns, "itemType=?", selectionArg, null, null, "_id");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    Intent intent = Intent.parseUri(cursor.getString(1), 0);
                    if (intent.getComponent() != null) {
                        ComponentName cmp = intent.getComponent();
                        long id = cursor.getLong(0);
                        ComponentKey cmpKey = new ComponentKey(cmp, userManager.getUserForSerialNumber(cursor.getLong(2)));
                        if (cmpList.contains(cmpKey)) {
                            duplicateItemIds.add(Long.valueOf(id));
                        } else {
                            cmpList.add(cmpKey);
                        }
                    }
                } catch (URISyntaxException e) {
                    Log.e(TAG, "Unable to parse intent during removeDuplicateItemForHomeOnly", e);
                } catch (Throwable th) {
                    cursor.close();
                }
            }
            cursor.close();
        }
        Log.d(TAG, "removeDuplicateItemForHomeOnly size " + duplicateItemIds.size());
        if (!duplicateItemIds.isEmpty()) {
            Log.d(TAG, "remove duplicate item " + TextUtils.join(", ", duplicateItemIds));
            sDb.delete("favorites_homeOnly", Utilities.createDbSelectionQuery("_id", duplicateItemIds), null);
        }
        return cmpList;
    }

    private void copyAppsItemsToHome(ArrayList<ComponentKey> homeItems) {
        String APPS_VIEW_TYPE = "AppsController.ViewType";
        String CUSTOM_GRID = "CUSTOM_GRID";
        String ALPHABETIC_GRID = "ALPHABETIC_GRID";
        LongSparseArray<ArrayList<ItemInfo>> folderChildList = new LongSparseArray();
        LongSparseArray<Long> folderIdMap = new LongSparseArray();
        ArrayList<ItemInfo> addItems = new ArrayList();
        ArrayList<ItemInfo> hiddenItems = new ArrayList();
        String viewType = sContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).getString("AppsController.ViewType", "CUSTOM_GRID");
        makeCopyItemList(homeItems, hiddenItems, addItems, folderChildList);
        if ("ALPHABETIC_GRID".equals(viewType)) {
            sortAlphabeticalOrder(addItems, folderChildList);
        }
        long itemId = initializeMaxItemId("favorites_homeOnly");
        long maxScreenId = getMaxScreenId();
        int maxScreenRank = getMaxScreenRank();
        int[] cellXY = new int[2];
        Utilities.loadCurrentGridSize(sContext, cellXY);
        boolean needScreenAdd = true;
        int cellX = 0;
        int cellY = 0;
        ArrayList<ContentProviderOperation> ops = new ArrayList();
        Iterator it = hiddenItems.iterator();
        while (it.hasNext()) {
            ItemInfo info = (ItemInfo) it.next();
            itemId++;
            info.id = itemId;
            info.screenId = -1;
            ContentValues values = new ContentValues();
            info.onAddToDatabase(sContext, values);
            ops.add(ContentProviderOperation.newInsert(Favorites_HomeOnly.CONTENT_URI).withValues(values).build());
        }
        it = addItems.iterator();
        while (it.hasNext()) {
            info = (ItemInfo) it.next();
            if (needScreenAdd) {
                maxScreenId++;
                maxScreenRank++;
                values = new ContentValues();
                values.put("_id", Long.valueOf(maxScreenId));
                values.put(WorkspaceScreens.SCREEN_RANK, Integer.valueOf(maxScreenRank));
                sDb.insert("workspaceScreens_homeOnly", null, values);
                needScreenAdd = false;
            }
            itemId++;
            if (info instanceof FolderInfo) {
                folderIdMap.put(info.id, Long.valueOf(itemId));
            }
            info.id = itemId;
            info.screenId = maxScreenId;
            int cellX2 = cellX + 1;
            info.cellX = cellX;
            info.cellY = cellY;
            info.spanY = 1;
            info.spanX = 1;
            if (cellX2 == cellXY[0]) {
                cellX = 0;
                cellY++;
            } else {
                cellX = cellX2;
            }
            if (cellY == cellXY[1]) {
                cellY = 0;
                needScreenAdd = true;
            }
            values = new ContentValues();
            info.onAddToDatabase(sContext, values);
            ops.add(ContentProviderOperation.newInsert(Favorites_HomeOnly.CONTENT_URI).withValues(values).build());
        }
        for (int i = 0; i < folderChildList.size(); i++) {
            long folderId = folderChildList.keyAt(i);
            long newFolderId = ((Long) folderIdMap.get(folderId)).longValue();
            ArrayList<ItemInfo> infoList = (ArrayList) folderChildList.get(folderId);
            if (infoList != null) {
                it = infoList.iterator();
                while (it.hasNext()) {
                    info = (ItemInfo) it.next();
                    itemId++;
                    info.id = itemId;
                    info.container = newFolderId;
                    values = new ContentValues();
                    info.onAddToDatabase(sContext, values);
                    ops.add(ContentProviderOperation.newInsert(Favorites_HomeOnly.CONTENT_URI).withValues(values).build());
                }
            }
        }
        try {
            sContext.getContentResolver().applyBatch("com.sec.android.app.launcher.settings", ops);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void makeCopyItemList(ArrayList<ComponentKey> homeItems, ArrayList<ItemInfo> hiddenItems, ArrayList<ItemInfo> addItems, LongSparseArray<ArrayList<ItemInfo>> folderChildList) {
        Intent intent;
        UserManagerCompat userManager = UserManagerCompat.getInstance(sContext);
        LongSparseArray<ItemInfo> folders = new LongSparseArray();
        String[] selectionArg = new String[]{String.valueOf(Favorites.CONTAINER_APPS)};
        int maxScreen = 0;
        Cursor cursor = sDb.query("favorites", new String[]{"MAX(screen)"}, "container=?", selectionArg, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    maxScreen = cursor.getInt(0);
                }
                cursor.close();
            } catch (Throwable th) {
                cursor.close();
            }
        }
        Log.d(TAG, "makeCopyItemList max screen " + maxScreen);
        String[] columns = new String[]{"_id", "title", "intent", BaseLauncherColumns.PROFILE_ID, BaseLauncherColumns.COLOR, BaseLauncherColumns.OPTIONS};
        for (int i = 0; i <= maxScreen; i++) {
            String intentString;
            ComponentName cmp;
            long id;
            UserHandleCompat user;
            selectionArg = new String[]{String.valueOf(Favorites.CONTAINER_APPS), String.valueOf(i), String.valueOf(0)};
            cursor = sDb.query("favorites", columns, "container=? AND screen=? AND hidden=?", selectionArg, null, null, BaseLauncherColumns.RANK);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    intentString = cursor.getString(2);
                    if (intentString != null) {
                        try {
                            intent = Intent.parseUri(intentString, 0);
                            if (intent.getComponent() != null) {
                                cmp = intent.getComponent();
                                id = cursor.getLong(0);
                                user = userManager.getUserForSerialNumber(cursor.getLong(3));
                                if (homeItems.contains(new ComponentKey(cmp, user))) {
                                    Log.d(TAG, "This item is already exist in home : " + cmp);
                                } else {
                                    addItems.add(new IconInfo(id, cmp, -100, -1, user));
                                }
                            }
                        } catch (Throwable e) {
                            Log.e(TAG, "Unable to parse intent during makeCopyItemList", e);
                        } catch (Throwable th2) {
                            cursor.close();
                        }
                    } else {
                        id = cursor.getLong(0);
                        FolderInfo info = new FolderInfo();
                        info.id = id;
                        info.container = -100;
                        info.title = cursor.getString(1);
                        info.color = cursor.getInt(4);
                        info.options = cursor.getInt(5);
                        folders.put(id, info);
                        addItems.add(info);
                    }
                }
                cursor.close();
            }
        }
        int folderCount = folders.size();
        for (int j = 0; j < folderCount; j++) {
            int childCount = 0;
            String[] folderChildColumns = new String[]{"_id", "intent", "screen", BaseLauncherColumns.PROFILE_ID};
            Long folderId = Long.valueOf(folders.keyAt(j));
            cursor = sDb.query("favorites", folderChildColumns, "container=?", new String[]{String.valueOf(folderId)}, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    try {
                        intentString = cursor.getString(1);
                        if (intentString != null) {
                            intent = Intent.parseUri(intentString, 0);
                            if (intent.getComponent() != null) {
                                cmp = intent.getComponent();
                                id = cursor.getLong(0);
                                user = userManager.getUserForSerialNumber(cursor.getLong(3));
                                if (homeItems.contains(new ComponentKey(cmp, user))) {
                                    Log.d(TAG, "This item is already exist in home : " + cmp);
                                } else {
                                    IconInfo iconInfo = new IconInfo(id, cmp, folderId.longValue(), cursor.getLong(2), user);
                                    ArrayList<ItemInfo> childList = (ArrayList) folderChildList.get(folderId.longValue());
                                    if (childList == null) {
                                        childList = new ArrayList();
                                        folderChildList.put(folderId.longValue(), childList);
                                    }
                                    childList.add(iconInfo);
                                    childCount++;
                                }
                            }
                        }
                    } catch (Throwable e2) {
                        Log.e(TAG, "Unable to parse intent during makeCopyItemList(folder child)", e2);
                    } catch (Throwable th3) {
                        cursor.close();
                    }
                }
                cursor.close();
            }
            if (childCount == 0) {
                ItemInfo folderInfo = (ItemInfo) folders.get(folders.keyAt(j));
                if (folderInfo != null) {
                    Log.d(TAG, "makeCopyItemList - remove empty folder : " + folderInfo.title);
                    addItems.remove(folderInfo);
                    folderChildList.remove(folderId.longValue());
                }
            }
        }
        String[] hiddenColumns = new String[]{"_id", "intent", BaseLauncherColumns.PROFILE_ID, "hidden"};
        cursor = sDb.query("favorites", hiddenColumns, "container=? AND hidden!=?", new String[]{String.valueOf(Favorites.CONTAINER_APPS), String.valueOf(0)}, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    intentString = cursor.getString(1);
                    if (intentString != null) {
                        intent = Intent.parseUri(intentString, 0);
                        if (intent.getComponent() != null) {
                            cmp = intent.getComponent();
                            IconInfo info2 = new IconInfo(cursor.getLong(0), cmp, -100, -1, userManager.getUserForSerialNumber(cursor.getLong(2)));
                            info2.hidden = cursor.getInt(3);
                            hiddenItems.add(info2);
                        }
                    }
                } catch (Throwable e22) {
                    Log.e(TAG, "Unable to parse intent during makeCopyItemList", e22);
                } catch (Throwable th4) {
                    cursor.close();
                }
            }
            cursor.close();
        }
    }

    private void sortAlphabeticalOrder(ArrayList<ItemInfo> addItems, LongSparseArray<ArrayList<ItemInfo>> folderChildList) {
        IconCache iconCache = LauncherAppState.getInstance().getIconCache();
        Iterator it = addItems.iterator();
        while (it.hasNext()) {
            ItemInfo info = (ItemInfo) it.next();
            if (info instanceof IconInfo) {
                iconCache.updateTitleAndIcon((IconInfo) info);
            }
        }
        Comparator<ItemInfo> comparator = new Comparator<ItemInfo>() {
            public int compare(ItemInfo info1, ItemInfo info2) {
                if (info1.itemType != info2.itemType) {
                    if (info1.itemType == 2) {
                        return -1;
                    }
                    return 1;
                } else if (info1.title == null) {
                    Log.d(FavoritesProvider.TAG, "info1 title is null " + info1.toString());
                    return -1;
                } else if (info2.title != null) {
                    return info1.title.toString().compareToIgnoreCase(info2.title.toString());
                } else {
                    Log.d(FavoritesProvider.TAG, "info2 title is null " + info2.toString());
                    return 1;
                }
            }
        };
        Collections.sort(addItems, comparator);
        int folderChildCount = folderChildList.size();
        for (int i = 0; i < folderChildCount; i++) {
            ArrayList<ItemInfo> infoList = (ArrayList) folderChildList.get(folderChildList.keyAt(i));
            if (infoList != null) {
                it = infoList.iterator();
                while (it.hasNext()) {
                    info = (ItemInfo) it.next();
                    if (info instanceof IconInfo) {
                        iconCache.updateTitleAndIcon((IconInfo) info);
                    }
                }
                Collections.sort(infoList, comparator);
            }
        }
    }

    private void copyPreferenceForHomeOnly() {
        Editor prefsEditor;
        SharedPreferences prefs = sContext.getSharedPreferences(LauncherFiles.MANAGED_USER_PREFERENCES_KEY, 0);
        UserManagerCompat userManager = UserManagerCompat.getInstance(sContext);
        for (UserHandleCompat profileUser : userManager.getUserProfiles()) {
            if (!UserHandleCompat.myUserHandle().equals(profileUser)) {
                long userSerialNumber = userManager.getSerialNumberForUser(profileUser);
                String folderKey = Utilities.USER_FOLDER_ID_PREFIX + userSerialNumber;
                if (prefs.contains(folderKey)) {
                    Log.d(TAG, "copy user folder id & package list : " + userSerialNumber);
                    prefsEditor = prefs.edit();
                    prefsEditor.putLong(Utilities.USER_FOLDER_ID_PREFIX_HOME_ONLY + userSerialNumber, prefs.getLong(folderKey, 0));
                    prefsEditor.putStringSet(Utilities.INSTALLED_PACKAGES_PREFIX_HOME_ONLY + userSerialNumber, prefs.getStringSet(Utilities.INSTALLED_PACKAGES_PREFIX + userSerialNumber, null));
                    prefsEditor.apply();
                }
            }
        }
        prefs = sContext.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, 0);
        prefsEditor = prefs.edit();
        prefsEditor.putInt("Workspace.HomeOnly.CellX", prefs.getInt("Workspace.CellX", -1));
        prefsEditor.putInt("Workspace.HomeOnly.CellY", prefs.getInt("Workspace.CellY", -1));
        prefsEditor.apply();
    }

    public int loadFavorites(DefaultLayoutParser parser) {
        ArrayList<Long> screenIds = new ArrayList();
        int count = parser.loadLayout(sDb, screenIds);
        if (!parser.isReloadPostPosition()) {
            int cscScreenCount;
            Collections.sort(screenIds);
            int defaultScreenCount = screenIds.size();
            if (Utilities.isKnoxMode()) {
                cscScreenCount = 0;
            } else {
                cscScreenCount = SemCscFeature.getInstance().getInt("CscFeature_Launcher_TotalPageCount");
            }
            Log.i(TAG, "defaultScreenCount : " + defaultScreenCount + " cscScreenCount : " + cscScreenCount);
            if (defaultScreenCount < cscScreenCount) {
                for (int i = 0; i < cscScreenCount; i++) {
                    if (!screenIds.contains(Long.valueOf((long) i))) {
                        screenIds.add(i, Long.valueOf((long) i));
                    }
                }
            }
            int rank = 0;
            ContentValues values = new ContentValues();
            Iterator it = screenIds.iterator();
            while (it.hasNext()) {
                Long id = (Long) it.next();
                values.clear();
                values.put("_id", id);
                values.put(WorkspaceScreens.SCREEN_RANK, Integer.valueOf(rank));
                if (LauncherProvider.dbInsertAndCheck(sDb, "workspaceScreens", null, values) < 0) {
                    throw new RuntimeException("Failed initialize screen tablefrom default layout");
                }
                rank++;
            }
            LauncherProviderID providerID = LauncherAppState.getLauncherProviderID();
            if (providerID != null) {
                Log.d(TAG, "[SPRINT] Will init pref table based on default values");
                providerID.initPreferences(sContext);
            }
            this.mMaxItemId = initializeMaxItemId("favorites");
            this.mMaxScreenId = initializeMaxItemId("workspaceScreens");
        }
        return count;
    }

    public int loadAppsFavorites(DefaultLayoutParser parser) {
        ArrayList<Long> screenIds = new ArrayList();
        int count = parser.loadLayout(sDb, screenIds);
        Collections.sort(screenIds);
        this.mMaxItemId = initializeMaxItemId("favorites");
        return count;
    }

    public int restoreFavorites(DefaultLayoutParser loader) {
        int count = loader.loadLayout(sDb, null);
        this.mMaxItemId = initializeMaxItemId("favorites");
        this.mMaxScreenId = initializeMaxItemId("workspaceScreens");
        return count;
    }

    public void restoreScreens(int pageCount, String tableName) {
        ContentValues values = new ContentValues();
        for (int rank = 0; rank < pageCount; rank++) {
            values.clear();
            values.put("_id", Integer.valueOf(rank));
            values.put(WorkspaceScreens.SCREEN_RANK, Integer.valueOf(rank));
            if (LauncherProvider.dbInsertAndCheck(sDb, tableName, null, values) < 0) {
                throw new RuntimeException("Failed restore screens");
            }
        }
    }

    public int restoreAppsFavorites(DefaultLayoutParser loader) {
        int count = loader.loadLayout(sDb, null);
        this.mMaxItemId = initializeMaxItemId("favorites");
        return count;
    }

    public void removeAndAddHiddenApp(DefaultLayoutParser loader, String restoredCategory, ArrayList<String> tables) {
        ArrayList<ComponentName> hiddenApps = loader.getHiddenApps();
        Log.d(TAG, "removeAndAddHiddenApp size " + hiddenApps.size());
        if (hiddenApps.size() >= 1) {
            ArrayList<String> restoredTables = new ArrayList();
            if (tables != null) {
                restoredTables.addAll(tables);
            } else {
                String[] categories = new String[]{"home", LauncherBnrTag.TAG_HOMEONLY, LauncherBnrTag.TAG_EASY};
                ArrayList<String> restoredCategories = new ArrayList();
                if (!(restoredCategory == null || restoredCategory.isEmpty())) {
                    Collections.addAll(restoredCategories, restoredCategory.split(","));
                }
                for (String categoryName : categories) {
                    if (restoredCategories.contains(categoryName)) {
                        restoredTables.add(LauncherBnrHelper.getFavoritesTable(categoryName));
                    }
                }
            }
            sDb.beginTransaction();
            try {
                Iterator it = hiddenApps.iterator();
                while (it.hasNext()) {
                    ComponentName cn = (ComponentName) it.next();
                    Intent intentWithProfile = IconInfo.makeLaunchIntent(cn, 0);
                    intentWithProfile.toUri(0);
                    new Intent(intentWithProfile).removeExtra(ItemInfo.EXTRA_PROFILE);
                    String selection = "itemType=? AND (intent=? OR intent=?)";
                    String[] selectionArg = new String[]{String.valueOf(0), intentWithProfile.toUri(0), intentWithoutProfile.toUri(0)};
                    ContentValues values = new ContentValues();
                    values.put("intent", intentWithProfile.toUri(0));
                    values.put("title", "");
                    values.put("screen", Integer.valueOf(0));
                    values.put("itemType", Integer.valueOf(0));
                    values.put(BaseLauncherColumns.PROFILE_ID, Integer.valueOf(0));
                    values.put("hidden", Integer.valueOf(1));
                    Iterator it2 = restoredTables.iterator();
                    while (it2.hasNext()) {
                        long id;
                        String tableName = (String) it2.next();
                        Log.d(TAG, "remove hidden app(" + cn + ") in " + tableName + ". delete count : " + sDb.delete(tableName, selection, selectionArg));
                        if (tableName.equals("favorites")) {
                            id = generateNewItemId();
                        } else {
                            id = getMaxId(tableName) + 1;
                        }
                        values.put("_id", Long.valueOf(id));
                        values.put("container", Integer.valueOf(Favorites.CONTAINER_APPS));
                        if (tableName.equals("favorites_homeOnly") || ((tableName.equals("favorites_standard") && LauncherAppState.getInstance().isHomeOnlyModeEnabled(false)) || (tableName.equals("favorites") && LauncherAppState.getInstance().isHomeOnlyModeEnabled()))) {
                            values.put("container", Integer.valueOf(-100));
                        }
                        sDb.insert(tableName, null, values);
                        Log.d(TAG, "insert hidden app(" + cn + ") to " + tableName);
                    }
                }
                sDb.setTransactionSuccessful();
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
            } finally {
                sDb.endTransaction();
            }
        }
    }

    public List<Long> deleteEmptyFolders() {
        ArrayList<Long> folderIds = new ArrayList();
        sDb.beginTransaction();
        try {
            Cursor c = sDb.query("favorites", new String[]{"_id"}, "itemType = 2 AND _id NOT IN (SELECT container FROM favorites)", null, null, null, null);
            PostPositionController postPosition = PostPositionController.getInstance(sContext);
            while (c.moveToNext()) {
                folderIds.add(Long.valueOf(c.getLong(0)));
                postPosition.deleteFolder(c.getLong(0));
            }
            c.close();
            if (folderIds.size() > 0) {
                sDb.delete("favorites", Utilities.createDbSelectionQuery("_id", folderIds), null);
            }
            sDb.setTransactionSuccessful();
        } catch (SQLException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            folderIds.clear();
        } finally {
            sDb.endTransaction();
        }
        return folderIds;
    }

    public void deleteInvalidFolders(ArrayList<IconInfo> items) {
        ArrayList<FolderDbInfo> folders = new ArrayList();
        String[] column = new String[]{"_id", BaseLauncherColumns.RANK, "screen", "cellX", "cellY", "title"};
        Cursor cursor = sDb.query("favorites", column, "itemType = 2 AND container = -102", null, null, null, null);
        if (cursor != null) {
            try {
                int idIndex = cursor.getColumnIndexOrThrow("_id");
                int rankIndex = cursor.getColumnIndexOrThrow(BaseLauncherColumns.RANK);
                int screenIndex = cursor.getColumnIndexOrThrow("screen");
                int cellXIndex = cursor.getColumnIndexOrThrow("cellX");
                int cellYIndex = cursor.getColumnIndexOrThrow("cellY");
                int TitleIndex = cursor.getColumnIndexOrThrow("title");
                while (cursor.moveToNext()) {
                    FolderDbInfo info = new FolderDbInfo();
                    info.id = Long.valueOf(cursor.getLong(idIndex));
                    info.rank = cursor.getInt(rankIndex);
                    info.screen = cursor.getInt(screenIndex);
                    info.cellX = cursor.getInt(cellXIndex);
                    info.cellY = cursor.getInt(cellYIndex);
                    info.title = cursor.getString(TitleIndex);
                    folders.add(info);
                }
            } catch (SQLException e) {
                Log.e(TAG, "error in deleteInvalidFolders e=" + e.toString());
                folders.clear();
            } finally {
                cursor.close();
            }
        }
        int folderCount = folders.size();
        if (folderCount > 0) {
            PostPositionController postPosition = PostPositionController.getInstance(sContext);
            for (int i = 0; i < folderCount; i++) {
                if (folders.get(i) != null) {
                    Long folderDbId = ((FolderDbInfo) folders.get(i)).id;
                    String[] strArr = new String[]{"_id"};
                    cursor = sDb.query("favorites", strArr, "itemType = 0 AND container = " + folderDbId, null, null, null, null);
                    if (cursor != null) {
                        try {
                            if (cursor.getCount() == 1) {
                                cursor.moveToNext();
                                Log.i(TAG, "deleteInvalidFolders folderId=" + folderDbId + " has only 1 item, so we delete this folder");
                                ContentValues values = new ContentValues();
                                Long childDbId = Long.valueOf(cursor.getLong(0));
                                int rank = ((FolderDbInfo) folders.get(i)).rank;
                                int screen = ((FolderDbInfo) folders.get(i)).screen;
                                int cellX = ((FolderDbInfo) folders.get(i)).cellX;
                                int cellY = ((FolderDbInfo) folders.get(i)).cellY;
                                values.put("container", Integer.valueOf(Favorites.CONTAINER_APPS));
                                values.put("screen", Integer.valueOf(screen));
                                values.put("cellX", Integer.valueOf(cellX));
                                values.put("cellY", Integer.valueOf(cellY));
                                values.put("spanX", Integer.valueOf(1));
                                values.put("spanY", Integer.valueOf(1));
                                values.put(BaseLauncherColumns.RANK, Integer.valueOf(rank));
                                sDb.update("favorites", values, "_id=" + childDbId, null);
                                sDb.delete("favorites", "_id=" + folderDbId, null);
                                postPosition.writeFolderReadyIdForNoFDR(-102, ((FolderDbInfo) folders.get(i)).title, childDbId.longValue());
                                synchronized (DataLoader.sBgLock) {
                                    Log.i(TAG, "deleteInvalidFolders folderId=" + folderDbId + " childDbId=" + childDbId + " changed rank=" + rank);
                                    DataLoader.sBgFolders.remove(folderDbId.longValue());
                                    DataLoader.sBgItemsIdMap.remove(folderDbId.longValue());
                                    IconInfo child = null;
                                    Iterator it = items.iterator();
                                    while (it.hasNext()) {
                                        IconInfo info2 = (IconInfo) it.next();
                                        if (info2.id == childDbId.longValue()) {
                                            child = info2;
                                            break;
                                        }
                                    }
                                    if (child != null) {
                                        Log.i(TAG, "This item is not folder's child anymore, so we change app info : " + child);
                                        child.container = -102;
                                        child.rank = rank;
                                        child.screenId = (long) screen;
                                    }
                                }
                            }
                        } catch (SQLException e2) {
                            try {
                                Log.e(TAG, "child error in deleteInvalidFolders e=" + e2.toString());
                                if (cursor != null) {
                                    cursor.close();
                                }
                            } catch (Throwable th) {
                                if (cursor != null) {
                                    cursor.close();
                                }
                            }
                        }
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
    }

    public void checkId(String table, ContentValues values) {
        Long id = values.getAsLong("_id");
        if (id != null) {
            if ("workspaceScreens".equals(table)) {
                if (id.longValue() > this.mMaxScreenId) {
                    this.mMaxScreenId = id.longValue();
                }
            } else if (id.longValue() > this.mMaxItemId) {
                this.mMaxItemId = id.longValue();
            }
        }
    }

    public long insertAndCheck(SQLiteDatabase db, String tableName, ContentValues values) {
        if (tableName != null) {
            return LauncherProvider.dbInsertAndCheck(db, tableName, null, values);
        }
        return LauncherProvider.dbInsertAndCheck(db, "favorites", null, values);
    }

    public ArrayList<Long> loadScreensFromDb() {
        Throwable th;
        ArrayList<Long> screenIds = new ArrayList();
        try {
            Cursor sc = sDb.query("workspaceScreens", null, null, null, null, null, WorkspaceScreens.SCREEN_RANK);
            Throwable th2 = null;
            try {
                int idIndex = sc.getColumnIndexOrThrow("_id");
                while (sc.moveToNext()) {
                    screenIds.add(Long.valueOf(sc.getLong(idIndex)));
                }
                if (sc != null) {
                    if (null != null) {
                        try {
                            sc.close();
                        } catch (Throwable th3) {
                            th2.addSuppressed(th3);
                        }
                    } else {
                        sc.close();
                    }
                }
                return screenIds;
            } catch (Throwable th32) {
                Throwable th4 = th32;
                th32 = th2;
                th2 = th4;
            }
            if (sc != null) {
                if (th32 != null) {
                    try {
                        sc.close();
                    } catch (Throwable th5) {
                        th32.addSuppressed(th5);
                    }
                } else {
                    sc.close();
                }
            }
            throw th2;
            throw th2;
        } catch (Exception e) {
            Launcher.addDumpLog(TAG, "Desktop items loading interrupted - invalid screens: " + e, true);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean switchTable(int r27, boolean r28) {
        /*
        r26 = this;
        monitor-enter(r26);
        switch(r27) {
            case 1: goto L_0x0028;
            case 2: goto L_0x008b;
            case 3: goto L_0x00ac;
            default: goto L_0x0004;
        };
    L_0x0004:
        r19 = "FavoritesProvider";
        r22 = new java.lang.StringBuilder;	 Catch:{ all -> 0x031f }
        r22.<init>();	 Catch:{ all -> 0x031f }
        r23 = "switchTable mode error : ";
        r22 = r22.append(r23);	 Catch:{ all -> 0x031f }
        r0 = r22;
        r1 = r27;
        r22 = r0.append(r1);	 Catch:{ all -> 0x031f }
        r22 = r22.toString();	 Catch:{ all -> 0x031f }
        r0 = r19;
        r1 = r22;
        android.util.Log.d(r0, r1);	 Catch:{ all -> 0x031f }
        r19 = 0;
    L_0x0026:
        monitor-exit(r26);
        return r19;
    L_0x0028:
        r9 = "favorites";
        r10 = "workspaceScreens";
        if (r28 == 0) goto L_0x007f;
    L_0x002e:
        r7 = "favorites_homeOnly";
    L_0x0030:
        if (r28 == 0) goto L_0x0082;
    L_0x0032:
        r5 = "favorites_homeApps";
    L_0x0034:
        if (r28 == 0) goto L_0x0085;
    L_0x0036:
        r8 = "workspaceScreens_homeOnly";
    L_0x0038:
        if (r28 == 0) goto L_0x0088;
    L_0x003a:
        r6 = "workspaceScreens_homeApps";
    L_0x003c:
        r0 = r26;
        r19 = r0.tableExists(r5);	 Catch:{ all -> 0x031f }
        if (r19 == 0) goto L_0x00ce;
    L_0x0044:
        r0 = r26;
        r19 = r0.tableExists(r6);	 Catch:{ all -> 0x031f }
        if (r19 == 0) goto L_0x00ce;
    L_0x004c:
        r19 = "FavoritesProvider";
        r22 = new java.lang.StringBuilder;	 Catch:{ all -> 0x031f }
        r22.<init>();	 Catch:{ all -> 0x031f }
        r23 = "switchTable : ";
        r22 = r22.append(r23);	 Catch:{ all -> 0x031f }
        r0 = r22;
        r22 = r0.append(r5);	 Catch:{ all -> 0x031f }
        r23 = " and ";
        r22 = r22.append(r23);	 Catch:{ all -> 0x031f }
        r0 = r22;
        r22 = r0.append(r6);	 Catch:{ all -> 0x031f }
        r23 = " is already existed";
        r22 = r22.append(r23);	 Catch:{ all -> 0x031f }
        r22 = r22.toString();	 Catch:{ all -> 0x031f }
        r0 = r19;
        r1 = r22;
        android.util.Log.d(r0, r1);	 Catch:{ all -> 0x031f }
        r19 = 1;
        goto L_0x0026;
    L_0x007f:
        r7 = "favorites_homeApps";
        goto L_0x0030;
    L_0x0082:
        r5 = "favorites_homeOnly";
        goto L_0x0034;
    L_0x0085:
        r8 = "workspaceScreens_homeApps";
        goto L_0x0038;
    L_0x0088:
        r6 = "workspaceScreens_homeOnly";
        goto L_0x003c;
    L_0x008b:
        r9 = "favorites";
        r10 = "workspaceScreens";
        if (r28 == 0) goto L_0x00a0;
    L_0x0091:
        r7 = "favorites_easy";
    L_0x0093:
        if (r28 == 0) goto L_0x00a3;
    L_0x0095:
        r5 = "favorites_standard";
    L_0x0097:
        if (r28 == 0) goto L_0x00a6;
    L_0x0099:
        r8 = "workspaceScreens_easy";
    L_0x009b:
        if (r28 == 0) goto L_0x00a9;
    L_0x009d:
        r6 = "workspaceScreens_standard";
    L_0x009f:
        goto L_0x003c;
    L_0x00a0:
        r7 = "favorites_standard";
        goto L_0x0093;
    L_0x00a3:
        r5 = "favorites_easy";
        goto L_0x0097;
    L_0x00a6:
        r8 = "workspaceScreens_standard";
        goto L_0x009b;
    L_0x00a9:
        r6 = "workspaceScreens_easy";
        goto L_0x009f;
    L_0x00ac:
        r9 = "favorites_standard";
        r10 = "workspaceScreens_standard";
        if (r28 == 0) goto L_0x00c2;
    L_0x00b2:
        r7 = "favorites_homeOnly";
    L_0x00b4:
        if (r28 == 0) goto L_0x00c5;
    L_0x00b6:
        r5 = "favorites_homeApps";
    L_0x00b8:
        if (r28 == 0) goto L_0x00c8;
    L_0x00ba:
        r8 = "workspaceScreens_homeOnly";
    L_0x00bc:
        if (r28 == 0) goto L_0x00cb;
    L_0x00be:
        r6 = "workspaceScreens_homeApps";
    L_0x00c0:
        goto L_0x003c;
    L_0x00c2:
        r7 = "favorites_homeApps";
        goto L_0x00b4;
    L_0x00c5:
        r5 = "favorites_homeOnly";
        goto L_0x00b8;
    L_0x00c8:
        r8 = "workspaceScreens_homeApps";
        goto L_0x00bc;
    L_0x00cb:
        r6 = "workspaceScreens_homeOnly";
        goto L_0x00c0;
    L_0x00ce:
        r19 = sDb;	 Catch:{ all -> 0x031f }
        r19.beginTransaction();	 Catch:{ all -> 0x031f }
        r19 = "FavoritesProvider";
        r22 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0318 }
        r22.<init>();	 Catch:{ all -> 0x0318 }
        r23 = "switchTable mode : ";
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r0 = r22;
        r1 = r27;
        r22 = r0.append(r1);	 Catch:{ all -> 0x0318 }
        r23 = " value : ";
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r0 = r22;
        r1 = r28;
        r22 = r0.append(r1);	 Catch:{ all -> 0x0318 }
        r22 = r22.toString();	 Catch:{ all -> 0x0318 }
        r0 = r19;
        r1 = r22;
        android.util.Log.d(r0, r1);	 Catch:{ all -> 0x0318 }
        r4 = new java.util.HashSet;	 Catch:{ all -> 0x0318 }
        r4.<init>();	 Catch:{ all -> 0x0318 }
        r0 = r26;
        r0.getHideItems(r4);	 Catch:{ all -> 0x0318 }
        r19 = sContext;	 Catch:{ all -> 0x0318 }
        r22 = com.android.launcher3.LauncherAppState.getSharedPreferencesKey();	 Catch:{ all -> 0x0318 }
        r23 = 0;
        r0 = r19;
        r1 = r22;
        r2 = r23;
        r11 = r0.getSharedPreferences(r1, r2);	 Catch:{ all -> 0x0318 }
        r19 = r11.edit();	 Catch:{ all -> 0x0318 }
        r22 = "com.sec.android.app.launcher.hideapps.prefs";
        r0 = r19;
        r1 = r22;
        r19 = r0.putStringSet(r1, r4);	 Catch:{ all -> 0x0318 }
        r19.apply();	 Catch:{ all -> 0x0318 }
        r0 = r26;
        r19 = r0.tableExists(r7);	 Catch:{ all -> 0x0318 }
        if (r19 != 0) goto L_0x014b;
    L_0x0136:
        r19 = sContext;	 Catch:{ all -> 0x0318 }
        r18 = com.android.launcher3.common.compat.UserManagerCompat.getInstance(r19);	 Catch:{ all -> 0x0318 }
        r19 = com.android.launcher3.common.compat.UserHandleCompat.myUserHandle();	 Catch:{ all -> 0x0318 }
        r20 = r18.getSerialNumberForUser(r19);	 Catch:{ all -> 0x0318 }
        r0 = r26;
        r1 = r20;
        r0.createFavoritesTable(r1, r7);	 Catch:{ all -> 0x0318 }
    L_0x014b:
        r0 = r26;
        r19 = r0.tableExists(r8);	 Catch:{ all -> 0x0318 }
        if (r19 != 0) goto L_0x0158;
    L_0x0153:
        r0 = r26;
        r0.createScreensTable(r8);	 Catch:{ all -> 0x0318 }
    L_0x0158:
        r19 = sDb;	 Catch:{ all -> 0x0318 }
        r22 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0318 }
        r22.<init>();	 Catch:{ all -> 0x0318 }
        r23 = "ALTER table ";
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r23 = android.database.DatabaseUtils.sqlEscapeString(r9);	 Catch:{ all -> 0x0318 }
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r23 = " rename to ";
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r23 = android.database.DatabaseUtils.sqlEscapeString(r5);	 Catch:{ all -> 0x0318 }
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r22 = r22.toString();	 Catch:{ all -> 0x0318 }
        r0 = r19;
        r1 = r22;
        r0.execSQL(r1);	 Catch:{ all -> 0x0318 }
        r19 = sDb;	 Catch:{ all -> 0x0318 }
        r22 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0318 }
        r22.<init>();	 Catch:{ all -> 0x0318 }
        r23 = "ALTER table ";
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r23 = android.database.DatabaseUtils.sqlEscapeString(r7);	 Catch:{ all -> 0x0318 }
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r23 = " rename to ";
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r23 = android.database.DatabaseUtils.sqlEscapeString(r9);	 Catch:{ all -> 0x0318 }
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r22 = r22.toString();	 Catch:{ all -> 0x0318 }
        r0 = r19;
        r1 = r22;
        r0.execSQL(r1);	 Catch:{ all -> 0x0318 }
        r19 = sDb;	 Catch:{ all -> 0x0318 }
        r22 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0318 }
        r22.<init>();	 Catch:{ all -> 0x0318 }
        r23 = "ALTER table ";
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r23 = android.database.DatabaseUtils.sqlEscapeString(r10);	 Catch:{ all -> 0x0318 }
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r23 = " rename to ";
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r23 = android.database.DatabaseUtils.sqlEscapeString(r6);	 Catch:{ all -> 0x0318 }
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r22 = r22.toString();	 Catch:{ all -> 0x0318 }
        r0 = r19;
        r1 = r22;
        r0.execSQL(r1);	 Catch:{ all -> 0x0318 }
        r19 = sDb;	 Catch:{ all -> 0x0318 }
        r22 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0318 }
        r22.<init>();	 Catch:{ all -> 0x0318 }
        r23 = "ALTER table ";
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r23 = android.database.DatabaseUtils.sqlEscapeString(r8);	 Catch:{ all -> 0x0318 }
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r23 = " rename to ";
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r23 = android.database.DatabaseUtils.sqlEscapeString(r10);	 Catch:{ all -> 0x0318 }
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r22 = r22.toString();	 Catch:{ all -> 0x0318 }
        r0 = r19;
        r1 = r22;
        r0.execSQL(r1);	 Catch:{ all -> 0x0318 }
        r0 = r26;
        r12 = r0.mMaxItemId;	 Catch:{ all -> 0x0318 }
        r0 = r26;
        r14 = r0.mMaxScreenId;	 Catch:{ all -> 0x0318 }
        r19 = "favorites";
        r0 = r26;
        r1 = r19;
        r22 = r0.initializeMaxItemId(r1);	 Catch:{ all -> 0x0318 }
        r0 = r26;
        r1 = r22;
        r0.setMaxItemId(r1);	 Catch:{ all -> 0x0318 }
        r19 = "workspaceScreens";
        r0 = r26;
        r1 = r19;
        r22 = r0.initializeMaxItemId(r1);	 Catch:{ all -> 0x0318 }
        r0 = r26;
        r1 = r22;
        r0.setMaxScreenId(r1);	 Catch:{ all -> 0x0318 }
        r19 = sDb;	 Catch:{ all -> 0x0318 }
        r19.setTransactionSuccessful();	 Catch:{ all -> 0x0318 }
        r19 = "FavoritesProvider";
        r22 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0318 }
        r22.<init>();	 Catch:{ all -> 0x0318 }
        r23 = "switchTable. old/new Max Item Id: ";
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r0 = r22;
        r22 = r0.append(r12);	 Catch:{ all -> 0x0318 }
        r23 = " / ";
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r0 = r26;
        r0 = r0.mMaxItemId;	 Catch:{ all -> 0x0318 }
        r24 = r0;
        r0 = r22;
        r1 = r24;
        r22 = r0.append(r1);	 Catch:{ all -> 0x0318 }
        r22 = r22.toString();	 Catch:{ all -> 0x0318 }
        r0 = r19;
        r1 = r22;
        android.util.Log.d(r0, r1);	 Catch:{ all -> 0x0318 }
        r19 = "FavoritesProvider";
        r22 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0318 }
        r22.<init>();	 Catch:{ all -> 0x0318 }
        r23 = "switchTable. old/new Max Screen Id: ";
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r0 = r22;
        r22 = r0.append(r14);	 Catch:{ all -> 0x0318 }
        r23 = " / ";
        r22 = r22.append(r23);	 Catch:{ all -> 0x0318 }
        r0 = r26;
        r0 = r0.mMaxScreenId;	 Catch:{ all -> 0x0318 }
        r24 = r0;
        r0 = r22;
        r1 = r24;
        r22 = r0.append(r1);	 Catch:{ all -> 0x0318 }
        r22 = r22.toString();	 Catch:{ all -> 0x0318 }
        r0 = r19;
        r1 = r22;
        android.util.Log.d(r0, r1);	 Catch:{ all -> 0x0318 }
        r19 = sDb;	 Catch:{ all -> 0x031f }
        r19.endTransaction();	 Catch:{ all -> 0x031f }
        r0 = r26;
        r0 = r0.mMaxItemId;	 Catch:{ all -> 0x031f }
        r22 = r0;
        r24 = 0;
        r19 = (r22 > r24 ? 1 : (r22 == r24 ? 0 : -1));
        if (r19 != 0) goto L_0x0304;
    L_0x02b4:
        r0 = r26;
        r0 = r0.mMaxScreenId;	 Catch:{ all -> 0x031f }
        r22 = r0;
        r24 = 0;
        r19 = (r22 > r24 ? 1 : (r22 == r24 ? 0 : -1));
        if (r19 != 0) goto L_0x0304;
    L_0x02c0:
        r22 = 0;
        r19 = (r12 > r22 ? 1 : (r12 == r22 ? 0 : -1));
        if (r19 == 0) goto L_0x0304;
    L_0x02c6:
        r19 = "FavoritesProvider";
        r22 = "switchTable. Current page is required load default layout";
        r0 = r19;
        r1 = r22;
        android.util.Log.d(r0, r1);	 Catch:{ all -> 0x031f }
        r17 = com.android.launcher3.LauncherAppState.getSharedPreferencesKey();	 Catch:{ all -> 0x031f }
        r19 = sContext;	 Catch:{ all -> 0x031f }
        r22 = 0;
        r0 = r19;
        r1 = r17;
        r2 = r22;
        r16 = r0.getSharedPreferences(r1, r2);	 Catch:{ all -> 0x031f }
        r19 = "EMPTY_DATABASE_SWITCHED";
        r0 = r16;
        r1 = r19;
        r19 = r0.contains(r1);	 Catch:{ all -> 0x031f }
        if (r19 != 0) goto L_0x0304;
    L_0x02ef:
        r19 = r16.edit();	 Catch:{ all -> 0x031f }
        r22 = "EMPTY_DATABASE_SWITCHED";
        r23 = 1;
        r0 = r19;
        r1 = r22;
        r2 = r23;
        r19 = r0.putBoolean(r1, r2);	 Catch:{ all -> 0x031f }
        r19.apply();	 Catch:{ all -> 0x031f }
    L_0x0304:
        r0 = r26;
        r19 = r0.tableExists(r5);	 Catch:{ all -> 0x031f }
        if (r19 == 0) goto L_0x0322;
    L_0x030c:
        r0 = r26;
        r19 = r0.tableExists(r6);	 Catch:{ all -> 0x031f }
        if (r19 == 0) goto L_0x0322;
    L_0x0314:
        r19 = 1;
        goto L_0x0026;
    L_0x0318:
        r19 = move-exception;
        r22 = sDb;	 Catch:{ all -> 0x031f }
        r22.endTransaction();	 Catch:{ all -> 0x031f }
        throw r19;	 Catch:{ all -> 0x031f }
    L_0x031f:
        r19 = move-exception;
        monitor-exit(r26);
        throw r19;
    L_0x0322:
        r19 = "FavoritesProvider";
        r22 = "switchTable mode error";
        r0 = r19;
        r1 = r22;
        android.util.Log.d(r0, r1);	 Catch:{ all -> 0x031f }
        r19 = 0;
        goto L_0x0026;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.model.FavoritesProvider.switchTable(int, boolean):boolean");
    }

    public int loadHotseatCount() {
        String[] selectionArgs = new String[]{String.valueOf(Favorites.CONTAINER_HOTSEAT)};
        Cursor c = sDb.query("favorites", null, "container=?", selectionArgs, null, null, null);
        if (c == null) {
            return -1;
        }
        int count = c.getCount();
        c.close();
        return count;
    }

    public Cursor loadWorkspaceWithScreenRank(String favoriteTable, String screenTable) {
        String escapedFavoriteTable = DatabaseUtils.sqlEscapeString(favoriteTable);
        return sDb.rawQuery("SELECT f.*, screenRank from " + escapedFavoriteTable + " f join " + DatabaseUtils.sqlEscapeString(screenTable) + " w on f.screen = w._id where container = " + -100 + " and " + LauncherBnrHelper.getUserSelectionArg(sContext) + " order by " + WorkspaceScreens.SCREEN_RANK + ", " + "cellY" + ", " + "cellX", null);
    }

    public void clearFlagEmptyDbSwitched() {
        sContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit().remove(EMPTY_DATABASE_SWITCHED).apply();
    }

    public void updateAppItems(List<AppOrderModify> updates) {
        SQLiteStatement insertFolder = null;
        SQLiteStatement updateFolder = null;
        SQLiteStatement updateTitle = null;
        SQLiteStatement insertApp = null;
        SQLiteStatement updateApp = null;
        SQLiteStatement deleteItem = null;
        SQLiteStatement updateColor = null;
        SQLiteStatement updateRestore = null;
        UserManagerCompat userManager = UserManagerCompat.getInstance(sContext);
        sDb.beginTransaction();
        for (AppOrderModify update : updates) {
            String str;
            StringBuilder append;
            switch (update.action) {
                case 0:
                    Log.d(TAG, "apps favorites insert folder: " + update.id + ", " + update.screen + ", " + update.rank + ", " + update.title);
                    if (insertFolder == null) {
                        insertFolder = sDb.compileStatement("INSERT into favorites (_id,screen,rank,title,color) values(?,?,?,?,?)");
                    }
                    insertFolder.bindLong(1, update.id);
                    insertFolder.bindLong(2, update.screen);
                    insertFolder.bindLong(3, (long) update.rank);
                    if (update.title == null) {
                        insertFolder.bindNull(4);
                    } else {
                        insertFolder.bindString(4, update.title.toString());
                    }
                    update.getClass();
                    insertFolder.bindLong(5, -1);
                    Log.d(TAG, "apps favorites insert folder: " + update.id + ", " + update.screen + ", " + update.rank + ", " + update.title);
                    insertFolder.execute();
                    break;
                case 1:
                    try {
                        Log.d(TAG, "apps favorites update folder: " + update.id + ", " + update.screen + ", " + update.rank + ", " + update.title);
                        if (updateFolder == null) {
                            updateFolder = sDb.compileStatement("UPDATE favorites set screen=?,rank=?,title=? where _id=?");
                        }
                        updateFolder.bindLong(1, update.screen);
                        updateFolder.bindLong(2, (long) update.rank);
                        if (update.title == null) {
                            updateFolder.bindNull(3);
                        } else {
                            updateFolder.bindString(3, update.title.toString());
                        }
                        updateFolder.bindLong(4, update.id);
                        Log.d(TAG, "apps favorites update folder: " + update.id + ", " + update.screen + ", " + update.rank + ", " + update.title);
                        updateFolder.execute();
                        break;
                    } catch (SQLiteReadOnlyDatabaseException e) {
                        Log.d(TAG, "SQLiteReadOnlyDatabaseException:" + e);
                        break;
                    } finally {
                        if (insertFolder != null) {
                            insertFolder.close();
                        }
                        if (updateFolder != null) {
                            updateFolder.close();
                        }
                        if (updateTitle != null) {
                            updateTitle.close();
                        }
                        if (insertApp != null) {
                            insertApp.close();
                        }
                        if (updateApp != null) {
                            updateApp.close();
                        }
                        if (deleteItem != null) {
                            deleteItem.close();
                        }
                        sDb.endTransaction();
                    }
                case 2:
                    Log.d(TAG, "apps favorites update title: " + update.id + ", " + update.title);
                    if (updateTitle == null) {
                        updateTitle = sDb.compileStatement("UPDATE favorites set title=? where _id=?");
                    }
                    if (update.title == null) {
                        updateTitle.bindNull(1);
                    } else {
                        updateTitle.bindString(1, update.title.toString());
                    }
                    updateTitle.bindLong(2, update.id);
                    updateTitle.execute();
                    break;
                case 3:
                    String flattenComponent;
                    if (update.component != null) {
                        flattenComponent = update.component.flattenToShortString();
                    } else {
                        flattenComponent = "component is null";
                    }
                    str = TAG;
                    append = new StringBuilder().append("apps favorites create app: ").append(update.id).append(", ").append(update.container).append(", ").append(update.screen).append(", ").append(update.rank).append(", ");
                    update.getClass();
                    Log.d(str, append.append(false).append(", ").append(update.title).append(", ").append(flattenComponent).append(", ").append(update.itemtype).append(", ").append(update.modified).append(", ").append(update.status).toString());
                    if (insertApp == null) {
                        insertApp = sDb.compileStatement("INSERT into favorites (_id,container,screen,rank,hidden,title,intent,profileId,itemtype,modified,restored) values(?,?,?,?,?,?,?,?,?,?,?)");
                    }
                    insertApp.bindLong(1, update.id);
                    insertApp.bindLong(2, update.container);
                    insertApp.bindLong(3, update.screen);
                    insertApp.bindLong(4, (long) update.rank);
                    update.getClass();
                    insertApp.bindLong(5, 0);
                    if (update.title == null) {
                        insertApp.bindNull(6);
                    } else {
                        insertApp.bindString(6, update.title.toString());
                    }
                    insertApp.bindString(7, new Intent("android.intent.action.MAIN", null).addCategory("android.intent.category.LAUNCHER").setComponent(update.component).setFlags(270532608).toUri(0));
                    insertApp.bindLong(8, userManager.getSerialNumberForUser(update.user));
                    insertApp.bindLong(9, (long) update.itemtype);
                    insertApp.bindLong(10, update.modified);
                    insertApp.bindLong(11, (long) update.status);
                    if (update.component != null) {
                        flattenComponent = update.component.flattenToShortString();
                    } else {
                        flattenComponent = "component is null";
                    }
                    str = TAG;
                    append = new StringBuilder().append("apps favorites create app: ").append(update.id).append(", ").append(update.container).append(", ").append(update.screen).append(", ").append(update.rank).append(", ");
                    update.getClass();
                    Log.d(str, append.append(false).append(", ").append(update.title).append(", ").append(flattenComponent).append(", ").append(update.itemtype).append(", ").append(update.modified).toString());
                    insertApp.execute();
                    break;
                case 4:
                    str = TAG;
                    append = new StringBuilder().append("apps favorites update app: ").append(update.id).append(", ").append(update.container).append(", ").append(update.screen).append(", ").append(update.rank).append(", ");
                    update.getClass();
                    Log.d(str, append.append(false).append(", ").append(update.title).toString());
                    if (updateApp == null) {
                        updateApp = sDb.compileStatement("UPDATE favorites set container=?,screen=?,rank=?,hidden=?,title=?,profileId=? where _id=?");
                    }
                    updateApp.bindLong(1, update.container);
                    updateApp.bindLong(2, update.screen);
                    updateApp.bindLong(3, (long) update.rank);
                    update.getClass();
                    updateApp.bindLong(4, 0);
                    if (update.title == null) {
                        updateApp.bindNull(5);
                    } else {
                        updateApp.bindString(5, update.title.toString());
                    }
                    updateApp.bindLong(6, userManager.getSerialNumberForUser(update.user));
                    updateApp.bindLong(7, update.id);
                    str = TAG;
                    append = new StringBuilder().append("apps favorites update app: ").append(update.id).append(", ").append(update.container).append(", ").append(update.screen).append(", ").append(update.rank).append(", ");
                    update.getClass();
                    Log.d(str, append.append(false).append(", ").append(update.title).toString());
                    updateApp.execute();
                    break;
                case 5:
                    Log.d(TAG, "apps favorites delete item: " + update.id);
                    if (deleteItem == null) {
                        deleteItem = sDb.compileStatement("DELETE from favorites where _id=?");
                    }
                    deleteItem.bindLong(1, update.id);
                    deleteItem.execute();
                    break;
                case 6:
                    str = TAG;
                    append = new StringBuilder().append("apps favorites update color: ").append(update.id).append(", ");
                    update.getClass();
                    Log.d(str, append.append(-1).toString());
                    if (updateColor == null) {
                        updateColor = sDb.compileStatement("UPDATE favorites set color=? where _id=?");
                    }
                    update.getClass();
                    updateColor.bindLong(1, -1);
                    updateColor.bindLong(2, update.id);
                    updateColor.execute();
                    break;
                case 7:
                    Log.d(TAG, "apps favorites update restored ID: " + update.id);
                    if (updateRestore == null) {
                        updateRestore = sDb.compileStatement("UPDATE favorites set restored=? where _id=?");
                    }
                    updateRestore.bindLong(1, 0);
                    updateRestore.bindLong(2, update.id);
                    updateRestore.execute();
                    break;
                default:
                    break;
            }
        }
        sDb.setTransactionSuccessful();
    }

    public AppWidgetHost getAppWidgetHost() {
        return this.mAppWidgetHost;
    }

    private void addDataToEasyTable(Cursor cr, int contactCount, long userSerial) {
        if (cr == null) {
            Log.d(TAG, "Cursor is null!!");
            return;
        }
        int idx_Id = cr.getColumnIndexOrThrow("rowID");
        int idx_screen = cr.getColumnIndexOrThrow("screen");
        int idx_position = cr.getColumnIndexOrThrow("position");
        int idx_appWidgetID = cr.getColumnIndexOrThrow("appWidgetID");
        int idx_packageName = cr.getColumnIndexOrThrow(DefaultLayoutParser.ATTR_PACKAGE_NAME);
        int idx_className = cr.getColumnIndexOrThrow(DefaultLayoutParser.ATTR_CLASS_NAME);
        int idx_appIcon = cr.getColumnIndexOrThrow("appIcon");
        if (!cr.getString(idx_packageName).isEmpty() && !cr.getString(idx_className).isEmpty()) {
            ContentValues values = new ContentValues();
            values.put("_id", Long.valueOf(cr.getLong(idx_Id)));
            values.put("title", (Byte) null);
            if (cr.getInt(idx_screen) != 2 || cr.getInt(idx_position) > 1) {
                values.put("container", Integer.valueOf(-100));
                values.put("cellX", Integer.valueOf(cr.getInt(idx_position) % 3));
                String str = "cellY";
                int i = cr.getInt(idx_position) / 3;
                int i2 = cr.getInt(idx_screen) == 2 ? getSoftKeyForEasy() ? 2 : 1 : 0;
                values.put(str, Integer.valueOf(i2 + i));
                values.put("screen", Integer.valueOf(cr.getInt(idx_screen) - contactCount));
            } else {
                values.put("container", Integer.valueOf(Favorites.CONTAINER_HOTSEAT));
                values.put("cellX", (Byte) null);
                values.put("cellY", (Byte) null);
                values.put("screen", Integer.valueOf(cr.getInt(idx_position)));
            }
            values.put("spanX", Integer.valueOf(1));
            values.put("spanY", Integer.valueOf(1));
            if (cr.getInt(idx_appWidgetID) > 0) {
                values.put("itemType", Integer.valueOf(4));
                values.put("intent", (Byte) null);
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(sContext);
                ComponentName cn;
                int appWidgetId;
                if (LauncherAppWidgetHostView.WEATHER_APPWIDGET_PACKAGE_NAME.equals(cr.getString(idx_packageName))) {
                    cn = ComponentName.createRelative(LauncherAppWidgetHostView.WEATHER_APPWIDGET_PACKAGE_NAME, WEATHER_WIDGET_EASY_CLASS);
                    try {
                        appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
                        if (appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn)) {
                            values.put(Favorites.APPWIDGET_PROVIDER, cn.flattenToString());
                            values.put("spanX", Integer.valueOf(3));
                            values.put("spanY", Integer.valueOf(1));
                            values.put("cellX", Integer.valueOf(0));
                            values.put("cellY", Integer.valueOf(0));
                            values.put(Favorites.APPWIDGET_ID, Integer.valueOf(appWidgetId));
                        } else {
                            Log.e(TAG, "Failed to initialize external widget");
                            return;
                        }
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Failed to initialize external widget", e);
                        return;
                    }
                } else if (CONTACT_WIDGET_EASY_PACKAGE.equals(cr.getString(idx_packageName))) {
                    int preAppWidgetId = cr.getInt(idx_appWidgetID);
                    cn = ComponentName.createRelative(CONTACT_WIDGET_EASY_PACKAGE, CONTACT_WIDGET_EASY_CLASS);
                    try {
                        appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
                        Bundle options = new Bundle();
                        options.putInt("Old_WidgetId", preAppWidgetId);
                        options.putInt("New_WidgetId", appWidgetId);
                        if (appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn, options)) {
                            values.put(Favorites.APPWIDGET_PROVIDER, cn.flattenToString());
                            values.put("spanX", Integer.valueOf(3));
                            values.put("spanY", Integer.valueOf(4));
                            values.put("cellX", Integer.valueOf(0));
                            values.put("cellY", Integer.valueOf(0));
                            values.put(Favorites.APPWIDGET_ID, Integer.valueOf(appWidgetId));
                        } else {
                            Log.e(TAG, "Failed to initialize external widget");
                            return;
                        }
                    } catch (RuntimeException e2) {
                        Log.e(TAG, "Failed to initialize external widget", e2);
                        return;
                    }
                }
            }
            values.put("itemType", Integer.valueOf(0));
            values.put("intent", IconInfo.makeLaunchIntent(ComponentName.createRelative(cr.getString(idx_packageName), cr.getString(idx_className)), userSerial).toUri(0));
            values.put(Favorites.APPWIDGET_PROVIDER, (Byte) null);
            values.put(BaseLauncherColumns.ICON_TYPE, (Byte) null);
            values.put("iconPackage", (Byte) null);
            values.put("iconResource", (Byte) null);
            values.put("icon", cr.getBlob(idx_appIcon));
            sDb.insert("favorites_easy", null, values);
        }
    }

    private void insertHideItem(String component, long container, long screen, long profile) {
        ContentValues values = new ContentValues();
        long id = generateNewItemId();
        Intent intent = IconInfo.makeLaunchIntent(ComponentName.unflattenFromString(component), profile);
        values.put("_id", Long.valueOf(id));
        values.put("intent", intent.toUri(0));
        values.put("container", Long.valueOf(container));
        values.put("title", "");
        values.put("screen", Long.valueOf(screen));
        values.put("itemType", Integer.valueOf(0));
        values.put(BaseLauncherColumns.PROFILE_ID, Long.valueOf(profile));
        values.put("hidden", Integer.valueOf(2));
        insertAndCheck(sDb, "favorites", values);
    }

    private void addAppsButton(String tableName, long maxID, long screen) {
        Log.d(TAG, "addAppsButton tableName : " + tableName + " maxID : " + maxID + " screen : " + screen);
        ContentValues values = new ContentValues();
        values.put("_id", Long.valueOf(maxID + 1));
        values.put("title", "Apps");
        values.put("container", Integer.valueOf(Favorites.CONTAINER_HOTSEAT));
        values.put("cellX", (Byte) null);
        values.put("cellY", (Byte) null);
        values.put("screen", Long.valueOf(screen));
        values.put("spanX", Integer.valueOf(1));
        values.put("spanY", Integer.valueOf(1));
        values.put("itemType", Integer.valueOf(1));
        values.put("intent", new Intent(Utilities.ACTION_SHOW_APPS_VIEW).toUri(0));
        values.put(Favorites.APPWIDGET_PROVIDER, (Byte) null);
        sDb.insert(tableName, null, values);
    }

    private void setDefaultHomeForEasy(int defaultPage) {
        Utilities.setHomeDefaultPageKey(sContext, defaultPage, LauncherFiles.HOMEEASY_DEFAULT_PAGE_KEY);
    }

    private boolean movePrefFileForEasy() {
        return new File(sContext.getApplicationInfo().dataDir + "/cache/" + EASY_MODE_PREFERENCE_FILE).renameTo(new File(sContext.getApplicationInfo().dataDir + "/shared_prefs/" + EASY_MODE_PREFERENCE_FILE));
    }

    private void migrateSharedPrefsForApps(SharedPreferences prefs, Editor prefsEditor) {
        migrateSharedPrefViewTypeForApps(prefs, prefsEditor);
    }

    private void migrateSharedPrefViewTypeForApps(SharedPreferences prefs, Editor prefsEditor) {
        if (prefs != null && prefsEditor != null) {
            String menuViewType = "MenuView.ViewType";
            String appsViewType = "AppsController.ViewType";
            String alphabeticalGrid = "ALPHABETIC_GRID";
            String customeGrid = "CUSTOM_GRID";
            String viewType = prefs.getString("MenuView.ViewType", null);
            if (viewType == null) {
                return;
            }
            if (viewType.equals("ALPHABETIC_GRID") || viewType.equals("CUSTOM_GRID")) {
                prefsEditor.putString("AppsController.ViewType", viewType);
                prefsEditor.remove("MenuView.ViewType");
                prefsEditor.apply();
            }
        }
    }

    private void migrateZeroPagePrefs(SharedPreferences prefs, Editor prefsEditor, String oldPrefKey) {
        if (prefs != null && prefsEditor != null) {
            String PREFERECES_HOMEZEROPAGE_PACKAGE_NAME = "home_zeropage_package_name";
            String PREFERECES_HOMEZEROPAGE_CLASS_NAME = "home_zeropage_class_name";
            prefsEditor.putBoolean(LauncherFiles.ZEROPAGE_ACTIVE_STATE_KEY, prefs.getBoolean(oldPrefKey, false));
            prefsEditor.remove(oldPrefKey);
            String pkgName = prefs.getString("home_zeropage_package_name", null);
            if (!(pkgName == null || pkgName.isEmpty())) {
                prefsEditor.putString(LauncherFiles.ZEROPAGE_PACKAGE_NAME_KEY, pkgName);
                prefsEditor.remove("home_zeropage_package_name");
            }
            String className = prefs.getString("home_zeropage_class_name", null);
            if (!(className == null || className.isEmpty())) {
                prefsEditor.putString(LauncherFiles.ZEROPAGE_CLASS_NAME_KEY, className);
                prefsEditor.remove("home_zeropage_class_name");
            }
            prefsEditor.apply();
        }
    }

    private boolean getSoftKeyForEasy() {
        if (this.mCountY == -1) {
            this.mCountY = Integer.parseInt(sContext.getResources().getStringArray(R.array.home_grid_info)[0].split(GRID_INFO_SPLIT)[1]);
        }
        if (this.mCountY > 3) {
            return true;
        }
        return false;
    }
}

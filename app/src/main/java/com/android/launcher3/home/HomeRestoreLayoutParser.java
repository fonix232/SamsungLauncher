package com.android.launcher3.home;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.bnr.LauncherBnrHelper;
import com.android.launcher3.common.bnr.LauncherBnrTag;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.model.DefaultLayoutParser;
import com.android.launcher3.common.model.DefaultLayoutParser.ShortcutParser;
import com.android.launcher3.common.model.DefaultLayoutParser.TagParser;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.model.LauncherSettings.ChangeLogColumns;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.model.LauncherSettings.Favorites_Easy;
import com.android.launcher3.common.model.LauncherSettings.Favorites_HomeApps;
import com.android.launcher3.common.model.LauncherSettings.Favorites_HomeOnly;
import com.android.launcher3.common.model.LauncherSettings.Favorites_Standard;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutKey;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.util.ScreenGridUtilities;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class HomeRestoreLayoutParser extends HomeDefaultLayoutParser {
    private static final String TAG = "Launcher.HomeRestore";
    private static final String VCF_RESTORE_PATH = (Environment.getExternalStorageDirectory().getAbsolutePath() + "/TempVcfForContact");
    private int mColumns;
    private FavoritesProvider mFavoritesProvider;
    private HashMap<String, Integer> mPageCountMap = new HashMap();
    private XmlPullParser mParser;
    private HashMap<Long, Pair<Pair<Integer, ComponentName>, String>> mRestoreAppWidgetId = new HashMap();
    private ArrayList<String> mRestoredTable;
    private int mRows;
    private HashMap<String, TagParser> mTagParserMap = new HashMap();
    private ComponentName mZeroPageContents = null;

    private class AddIconToHomeSettingsParser implements TagParser {
        private AddIconToHomeSettingsParser() {
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            if (parser.next() == 4) {
                boolean settingValue = Boolean.parseBoolean(parser.getText());
                Log.d(HomeRestoreLayoutParser.TAG, "restore AddIconToHomeEnabled : " + settingValue);
                HomeRestoreLayoutParser.this.mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit().putBoolean(Utilities.ADD_ICON_PREFERENCE_KEY, settingValue).apply();
            }
            return 0;
        }
    }

    private class AppsButtonParser implements TagParser {
        private AppsButtonParser() {
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            Log.i(HomeRestoreLayoutParser.TAG, "restore apps button");
            boolean isEasyModeValue = false;
            boolean isEasyModeEnable = LauncherAppState.getInstance().isEasyModeEnabled();
            if ((isEasyModeEnable && "favorites".equals(tableName)) || (!isEasyModeEnable && Favorites_Easy.TABLE_NAME.equals(tableName))) {
                isEasyModeValue = true;
            }
            LauncherAppState.getInstance().setAppsButtonEnabled(true, isEasyModeValue);
            HomeRestoreLayoutParser.this.mValues.put("container", Integer.valueOf(Favorites.CONTAINER_HOTSEAT));
            HomeRestoreLayoutParser.this.mValues.put("screen", DefaultLayoutParser.getAttributeValue(parser, "screen"));
            HomeRestoreLayoutParser.this.mValues.put(ChangeLogColumns.MODIFIED, Long.valueOf(System.currentTimeMillis()));
            HomeRestoreLayoutParser.this.addShortcut(tableName, "", new Intent(Utilities.ACTION_SHOW_APPS_VIEW), 1);
            return 0;
        }
    }

    private class CategoryParser implements TagParser {
        private CategoryParser() {
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            if (parser.next() == 4) {
                String categoryText = parser.getText();
                Log.d(HomeRestoreLayoutParser.TAG, "restore category : " + categoryText);
                HashMap<String, String> category = new HashMap();
                if (!(categoryText == null || categoryText.isEmpty())) {
                    String[] items = categoryText.split(",");
                    for (Object put : items) {
                        category.put(put, null);
                    }
                }
                if (category.containsKey("home")) {
                    addTag("home");
                }
                if (category.containsKey(LauncherBnrTag.TAG_ZEROPAGE)) {
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_ZEROPAGE, new ZeroPageParser());
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_ZEROPAGE_CONTENTS, new ZeroPageContentsParser());
                }
                if (LauncherFeature.supportHomeModeChange() && category.containsKey(LauncherBnrTag.TAG_HOMEONLY)) {
                    addTag(LauncherBnrTag.TAG_HOMEONLY);
                }
                if (LauncherFeature.supportEasyModeChange()) {
                    addTag(LauncherBnrTag.TAG_HOME_EASY);
                }
                if (LauncherFeature.supportNotificationPanelExpansion()) {
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_NOTIFICATION_PANEL_SETTING, new NotificationPanelSettingsParser());
                }
                if (LauncherFeature.supportRotationSetting()) {
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_ONLY_PORTRAIT_MODE_SETTING, new OnlyPortraitModeSettingsParser());
                }
                if (Utilities.ATLEAST_O) {
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_ADD_ICON_TO_HOME_SETTING, new AddIconToHomeSettingsParser());
                }
            }
            return 0;
        }

        private void addTag(String container) {
            boolean z = true;
            switch (container.hashCode()) {
                case -486342357:
                    if (container.equals(LauncherBnrTag.TAG_HOMEONLY)) {
                        z = true;
                        break;
                    }
                    break;
                case 2117753698:
                    if (container.equals(LauncherBnrTag.TAG_HOME_EASY)) {
                        z = false;
                        break;
                    }
                    break;
            }
            switch (z) {
                case false:
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_PAGECOUNT_EASY, new PageCountParser(LauncherBnrTag.TAG_HOME_EASY));
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_SCREEN_INDEX_EASY, new ScreenIndexParser(LauncherBnrTag.TAG_HOME_EASY));
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_HOME_EASY, null);
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_HOTSEAT_EASY, null);
                    return;
                case true:
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_ROWS_HOMEONLY, new RowsParser());
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_COLUMNS_HOMEONLY, new ColumnsParser(true));
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_PAGECOUNT_HOMEONLY, new PageCountParser(LauncherBnrTag.TAG_HOMEONLY));
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_SCREEN_INDEX_HOMEONLY, new ScreenIndexParser(LauncherBnrTag.TAG_HOMEONLY));
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_HOMEONLY, null);
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_HOTSEAT_HOMEONLY, null);
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_SCREEN_CONTENT, new HomeScreenContentParser());
                    return;
                default:
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_ROWS, new RowsParser());
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_COLUMNS, new ColumnsParser(false));
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_PAGECOUNT, new PageCountParser("home"));
                    HomeRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_SCREEN_INDEX, new ScreenIndexParser("home"));
                    HomeRestoreLayoutParser.this.mTagParserMap.put("home", null);
                    HomeRestoreLayoutParser.this.mTagParserMap.put("hotseat", null);
                    return;
            }
        }
    }

    private class ColumnsParser implements TagParser {
        private boolean mIsHomeOnlyData;

        ColumnsParser(boolean isHomeOnlyData) {
            this.mIsHomeOnlyData = isHomeOnlyData;
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            if (parser.next() == 4) {
                HomeRestoreLayoutParser.this.mColumns = Integer.parseInt(parser.getText());
                Log.i(HomeRestoreLayoutParser.TAG, "restore columns : " + HomeRestoreLayoutParser.this.mColumns);
                if (LauncherFeature.supportFlexibleGrid()) {
                    int[] restoreGridSize = new int[2];
                    if (ScreenGridUtilities.findNearestGridSize(HomeRestoreLayoutParser.this.mContext, restoreGridSize, HomeRestoreLayoutParser.this.mColumns, HomeRestoreLayoutParser.this.mRows)) {
                        Log.d(HomeRestoreLayoutParser.TAG, "restore home grid " + restoreGridSize[0] + DefaultLayoutParser.ATTR_X + restoreGridSize[1]);
                        ScreenGridUtilities.storeGridLayoutPreference(HomeRestoreLayoutParser.this.mContext, restoreGridSize[0], restoreGridSize[1], this.mIsHomeOnlyData);
                    }
                } else if (Utilities.isDeskTopMode(HomeRestoreLayoutParser.this.mContext)) {
                    Log.d(HomeRestoreLayoutParser.TAG, "restore home grid in desktop mode");
                    ScreenGridUtilities.storeGridLayoutPreference(HomeRestoreLayoutParser.this.mContext, HomeRestoreLayoutParser.this.mColumns, HomeRestoreLayoutParser.this.mRows, this.mIsHomeOnlyData);
                }
            }
            return 0;
        }
    }

    private static class HomeScreenContentParser implements TagParser {
        private HomeScreenContentParser() {
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            if (parser.next() == 4) {
                boolean homeScreenContent = Boolean.parseBoolean(parser.getText());
                LauncherAppState.getInstance().writeHomeOnlyModeEnabled(homeScreenContent);
                Log.i(HomeRestoreLayoutParser.TAG, "restore homeScreenContent : " + homeScreenContent);
            }
            return 0;
        }
    }

    private static class NotificationPanelSettingsParser implements TagParser {
        private NotificationPanelSettingsParser() {
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            if (parser.next() == 4) {
                boolean settingValue = Boolean.parseBoolean(parser.getText());
                Log.d(HomeRestoreLayoutParser.TAG, "restore NotificationPanelExpansionEnabled : " + settingValue);
                LauncherAppState.getInstance().setNotificationPanelExpansionEnabled(settingValue, true);
            }
            return 0;
        }
    }

    private class OnlyPortraitModeSettingsParser implements TagParser {
        private OnlyPortraitModeSettingsParser() {
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            if (parser.next() == 4) {
                boolean settingValue = !LauncherFeature.isTablet() && Boolean.parseBoolean(parser.getText());
                Log.d(HomeRestoreLayoutParser.TAG, "restore RotationEnabled : " + settingValue);
                Utilities.setOnlyPortraitMode(HomeRestoreLayoutParser.this.mContext, settingValue);
            }
            return 0;
        }
    }

    private class PageCountParser implements TagParser {
        private String mContainer;

        PageCountParser(String container) {
            this.mContainer = container;
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            if (parser.next() == 4) {
                int pageCount = Integer.parseInt(parser.getText());
                String screenTableName = LauncherBnrHelper.getWorkspaceScreenTable(this.mContainer);
                clearTable(tableName, screenTableName);
                HomeRestoreLayoutParser.this.mFavoritesProvider.restoreScreens(pageCount, screenTableName);
                HomeRestoreLayoutParser.this.mPageCountMap.put(screenTableName, Integer.valueOf(pageCount));
                Log.i(HomeRestoreLayoutParser.TAG, "restore pageCount : " + pageCount);
            }
            return 0;
        }

        private void clearTable(String tableName, String screenTableName) {
            HomeRestoreLayoutParser.this.mFavoritesProvider.deleteWidgetIds(tableName);
            HomeRestoreLayoutParser.this.mFavoritesProvider.deleteTable(tableName);
            HomeRestoreLayoutParser.this.mFavoritesProvider.deleteTable(screenTableName);
            if (HomeRestoreLayoutParser.this.mRestoredTable.size() == 0) {
                HomeRestoreLayoutParser.this.mFavoritesProvider.setMaxItemId(HomeRestoreLayoutParser.this.mFavoritesProvider.initializeMaxItemId(tableName));
            }
            HomeRestoreLayoutParser.this.mFavoritesProvider.setMaxScreenId(HomeRestoreLayoutParser.this.mFavoritesProvider.initializeMaxItemId(screenTableName));
            HomeRestoreLayoutParser.this.mRestoredTable.add(tableName);
        }
    }

    private class RowsParser implements TagParser {
        private RowsParser() {
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            if (parser.next() == 4) {
                HomeRestoreLayoutParser.this.mRows = Integer.parseInt(parser.getText());
                Log.i(HomeRestoreLayoutParser.TAG, "restore rows : " + HomeRestoreLayoutParser.this.mRows);
            }
            return 0;
        }
    }

    private class ScreenIndexParser implements TagParser {
        private String mContainer;

        ScreenIndexParser(String container) {
            this.mContainer = container;
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public long parseAndAdd(org.xmlpull.v1.XmlPullParser r9, java.lang.String r10) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
            /*
            r8 = this;
            r5 = 1;
            r4 = 0;
            r3 = r9.next();
            r6 = 4;
            if (r3 != r6) goto L_0x005d;
        L_0x0009:
            r3 = r9.getText();
            r1 = java.lang.Integer.parseInt(r3);
            if (r1 >= 0) goto L_0x0060;
        L_0x0013:
            r3 = "Launcher.HomeRestore";
            r6 = new java.lang.StringBuilder;
            r6.<init>();
            r7 = "restore screenIndex error : ";
            r6 = r6.append(r7);
            r6 = r6.append(r1);
            r6 = r6.toString();
            android.util.Log.i(r3, r6);
            r1 = 0;
        L_0x002c:
            r6 = r8.mContainer;
            r3 = -1;
            r7 = r6.hashCode();
            switch(r7) {
                case -486342357: goto L_0x00b8;
                case 2117753698: goto L_0x00af;
                default: goto L_0x0036;
            };
        L_0x0036:
            r4 = r3;
        L_0x0037:
            switch(r4) {
                case 0: goto L_0x00c3;
                case 1: goto L_0x00d0;
                default: goto L_0x003a;
            };
        L_0x003a:
            r3 = com.android.launcher3.home.HomeRestoreLayoutParser.this;
            r3 = r3.mContext;
            r4 = "com.sec.android.app.launcher.home.defaultpage.prefs";
            com.android.launcher3.Utilities.setHomeDefaultPageKey(r3, r1, r4);
        L_0x0045:
            r3 = "Launcher.HomeRestore";
            r4 = new java.lang.StringBuilder;
            r4.<init>();
            r5 = "restore screenIndex : ";
            r4 = r4.append(r5);
            r4 = r4.append(r1);
            r4 = r4.toString();
            android.util.Log.i(r3, r4);
        L_0x005d:
            r4 = 0;
            return r4;
        L_0x0060:
            if (r1 < r5) goto L_0x002c;
        L_0x0062:
            r3 = r8.mContainer;
            r2 = com.android.launcher3.common.bnr.LauncherBnrHelper.getWorkspaceScreenTable(r3);
            r3 = com.android.launcher3.home.HomeRestoreLayoutParser.this;
            r3 = r3.mPageCountMap;
            r3 = r3.containsKey(r2);
            if (r3 == 0) goto L_0x002c;
        L_0x0074:
            r3 = com.android.launcher3.home.HomeRestoreLayoutParser.this;
            r3 = r3.mPageCountMap;
            r3 = r3.get(r2);
            r3 = (java.lang.Integer) r3;
            r0 = r3.intValue();
            if (r0 > r1) goto L_0x002c;
        L_0x0086:
            r3 = "Launcher.HomeRestore";
            r6 = new java.lang.StringBuilder;
            r6.<init>();
            r7 = "restore screenIndex error : ";
            r6 = r6.append(r7);
            r6 = r6.append(r1);
            r7 = " pageCount : ";
            r6 = r6.append(r7);
            r6 = r6.append(r0);
            r6 = r6.toString();
            android.util.Log.d(r3, r6);
            if (r0 > 0) goto L_0x00ac;
        L_0x00aa:
            r1 = r4;
        L_0x00ab:
            goto L_0x002c;
        L_0x00ac:
            r1 = r0 + -1;
            goto L_0x00ab;
        L_0x00af:
            r5 = "home_easy";
            r5 = r6.equals(r5);
            if (r5 == 0) goto L_0x0036;
        L_0x00b7:
            goto L_0x0037;
        L_0x00b8:
            r4 = "homeOnly";
            r4 = r6.equals(r4);
            if (r4 == 0) goto L_0x0036;
        L_0x00c0:
            r4 = r5;
            goto L_0x0037;
        L_0x00c3:
            r3 = com.android.launcher3.home.HomeRestoreLayoutParser.this;
            r3 = r3.mContext;
            r4 = "com.sec.android.app.launcher.homeeasy.defaultpage.prefs";
            com.android.launcher3.Utilities.setHomeDefaultPageKey(r3, r1, r4);
            goto L_0x0045;
        L_0x00d0:
            r3 = com.android.launcher3.home.HomeRestoreLayoutParser.this;
            r3 = r3.mContext;
            r4 = "com.sec.android.app.launcher.homeonly.defaultpage.prefs";
            com.android.launcher3.Utilities.setHomeDefaultPageKey(r3, r1, r4);
            goto L_0x0045;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.HomeRestoreLayoutParser.ScreenIndexParser.parseAndAdd(org.xmlpull.v1.XmlPullParser, java.lang.String):long");
        }
    }

    private class ZeroPageContentsParser implements TagParser {
        private ZeroPageContentsParser() {
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            if (parser.next() == 4) {
                String zeroPageContents = parser.getText();
                HomeRestoreLayoutParser.this.mZeroPageContents = ComponentName.unflattenFromString(zeroPageContents);
                Log.i(HomeRestoreLayoutParser.TAG, "restore zeroPageContents : " + zeroPageContents);
            }
            return 0;
        }
    }

    private class ZeroPageParser implements TagParser {
        private ZeroPageParser() {
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            if (parser.next() == 4) {
                boolean zeroPageEnable;
                if (HomeRestoreLayoutParser.this.mZeroPageContents != null) {
                    Log.d(HomeRestoreLayoutParser.TAG, "Backuped zero page contents : " + HomeRestoreLayoutParser.this.mZeroPageContents);
                    if (HomeRestoreLayoutParser.this.mZeroPageContents.equals(ZeroPageController.getZeroPageContents(HomeRestoreLayoutParser.this.mContext))) {
                        zeroPageEnable = Boolean.parseBoolean(parser.getText());
                        ZeroPageController.setZeroPageActiveState(HomeRestoreLayoutParser.this.mContext, zeroPageEnable);
                        Log.i(HomeRestoreLayoutParser.TAG, "restore zeroPageEnable : " + zeroPageEnable);
                    } else {
                        Log.i(HomeRestoreLayoutParser.TAG, "zero page contents mismatch");
                    }
                } else if (new ComponentName(Utilities.DAYLITE_PACKAGE_NAME, Utilities.DAYLITE_CLASS_NAME_MAIN).equals(ZeroPageController.getZeroPageContents(HomeRestoreLayoutParser.this.mContext))) {
                    Log.i(HomeRestoreLayoutParser.TAG, "There is not exist zero page contents in backup data.But exist BixbyHome");
                    ZeroPageController.setZeroPageActiveState(HomeRestoreLayoutParser.this.mContext, true);
                } else {
                    zeroPageEnable = Boolean.parseBoolean(parser.getText());
                    ZeroPageController.setZeroPageActiveState(HomeRestoreLayoutParser.this.mContext, zeroPageEnable);
                    Log.i(HomeRestoreLayoutParser.TAG, "restore zeroPageEnable : " + zeroPageEnable);
                }
            }
            return 0;
        }
    }

    private class RestoreShortcutParser extends ShortcutParser {
        private RestoreShortcutParser() {
            super();
            this.mIsRestore = true;
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) {
            String restored = DefaultLayoutParser.getAttributeValue(parser, "restored");
            if (!TextUtils.isEmpty(restored)) {
                HomeRestoreLayoutParser.this.mValues.put("restored", Integer.valueOf(Integer.parseInt(restored)));
            }
            HomeRestoreLayoutParser.this.mValues.put(ChangeLogColumns.MODIFIED, Long.valueOf(System.currentTimeMillis()));
            long id = super.parseAndAdd(parser, tableName);
            HomeRestoreLayoutParser.this.restoreContactShortcut(DefaultLayoutParser.getAttributeValue(parser, "vcf"), id);
            return id;
        }

        protected Intent parseIntent(XmlPullParser parser) {
            String uri = null;
            try {
                uri = DefaultLayoutParser.getAttributeValue(parser, "uri");
                Intent intent = Intent.parseUri(uri, 0);
                if (intent == null || intent.getComponent() == null || !IconView.KNOX_SHORTCUT_PACKAGE.equals(intent.getComponent().getPackageName())) {
                    return intent;
                }
                return null;
            } catch (URISyntaxException e) {
                Log.w(HomeRestoreLayoutParser.TAG, "Shortcut has malformed uri: " + uri);
                return null;
            }
        }
    }

    private class RestoreAppShortcutParser extends DefaultAppShortcutParser {
        private RestoreAppShortcutParser() {
            super();
            this.mIsRestore = true;
        }

        protected ComponentName getComponent(XmlPullParser parser, String packageName, String className) {
            String restoredStr = DefaultLayoutParser.getAttributeValue(parser, "restored");
            int restored = 0;
            if (!TextUtils.isEmpty(restoredStr)) {
                restored = Integer.parseInt(restoredStr);
                HomeRestoreLayoutParser.this.mValues.put("restored", Integer.valueOf(restored));
            } else if (LauncherBnrHelper.getUsePlayStore()) {
                HomeRestoreLayoutParser.this.mValues.put("restored", Integer.valueOf(64));
            }
            HomeRestoreLayoutParser.this.mValues.put(ChangeLogColumns.MODIFIED, Long.valueOf(System.currentTimeMillis()));
            return LauncherBnrHelper.getComponent(HomeRestoreLayoutParser.this.mContext, restored, packageName, className);
        }
    }

    private class RestoreAppShortcutWithUriParser extends AppShortcutWithUriParser {
        private RestoreAppShortcutWithUriParser() {
            super();
            this.mIsRestore = true;
        }

        protected ComponentName getComponent(XmlPullParser parser, String packageName, String className) {
            String restoredStr = DefaultLayoutParser.getAttributeValue(parser, "restored");
            int restored = 0;
            if (!TextUtils.isEmpty(restoredStr)) {
                restored = Integer.parseInt(restoredStr);
                HomeRestoreLayoutParser.this.mValues.put("restored", Integer.valueOf(restored));
            } else if (LauncherBnrHelper.getUsePlayStore()) {
                HomeRestoreLayoutParser.this.mValues.put("restored", Integer.valueOf(64));
            }
            HomeRestoreLayoutParser.this.mValues.put(ChangeLogColumns.MODIFIED, Long.valueOf(System.currentTimeMillis()));
            return LauncherBnrHelper.getComponent(HomeRestoreLayoutParser.this.mContext, restored, packageName, className);
        }
    }

    private class RestoreAppWidgetParser extends DefaultAppWidgetParser {
        private int mRestoreWidgetId;

        private RestoreAppWidgetParser() {
            super();
            this.mRestoreWidgetId = -1;
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            HomeRestoreLayoutParser.this.mValues.put(ChangeLogColumns.MODIFIED, Long.valueOf(System.currentTimeMillis()));
            this.mRestoreWidgetId = Integer.parseInt(DefaultLayoutParser.getAttributeValue(parser, "appWidgetID"));
            return super.parseAndAdd(parser, tableName);
        }

        protected ComponentName getWidgetComponent(String packageName, String className) {
            ComponentName cn = super.getWidgetComponent(packageName, className);
            if (cn == null) {
                cn = new ComponentName(packageName, className);
                ComponentName changedComponent = LauncherBnrHelper.getChangedWidgetComponent(cn);
                if (changedComponent != null) {
                    return changedComponent;
                }
            }
            return cn;
        }

        protected int getAppWidgetId() {
            return this.mRestoreWidgetId;
        }

        protected boolean bindAppWidget(int appWidgetId, ComponentName cn, long dbId, String tableName) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(HomeRestoreLayoutParser.this.mContext);
            Bundle options = new Bundle();
            options.putString("appWidgetIdForceAllocMode", "ifEmpty");
            options.putInt("appWidgetIdForceAllocHostId", 1024);
            options.putInt("Old_WidgetId", appWidgetId);
            options.putInt("New_WidgetId", appWidgetId);
            if (!appWidgetManager.bindAppWidgetIdIfAllowed(this.mRestoreWidgetId, cn, options)) {
                Log.e(HomeRestoreLayoutParser.TAG, "bindAppWidgetIdIfAllowed() return false");
                HomeRestoreLayoutParser.this.mRestoreAppWidgetId.put(Long.valueOf(dbId), Pair.create(Pair.create(Integer.valueOf(appWidgetId), cn), tableName));
            }
            return true;
        }
    }

    private class RestoreHomeFolderParser extends DefaultHomeFolderParser {
        private RestoreHomeFolderParser() {
            super();
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            HomeRestoreLayoutParser.this.mValues.put(ChangeLogColumns.MODIFIED, Long.valueOf(System.currentTimeMillis()));
            return super.parseAndAdd(parser, tableName);
        }
    }

    private class RestoreUriShortcutParser extends UriShortcutParser {
        private RestoreUriShortcutParser() {
            super();
            this.mIsRestore = true;
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) {
            String restored = DefaultLayoutParser.getAttributeValue(parser, "restored");
            if (!TextUtils.isEmpty(restored)) {
                HomeRestoreLayoutParser.this.mValues.put("restored", Integer.valueOf(Integer.parseInt(restored)));
            }
            HomeRestoreLayoutParser.this.mValues.put(ChangeLogColumns.MODIFIED, Long.valueOf(System.currentTimeMillis()));
            long id = super.parseAndAdd(parser, tableName);
            HomeRestoreLayoutParser.this.restoreContactShortcut(DefaultLayoutParser.getAttributeValue(parser, "vcf"), id);
            if (LauncherFeature.supportDeepShortcut()) {
                HomeRestoreLayoutParser.this.restoreDeepShortcut(parseIntent(parser));
            }
            return id;
        }

        protected Intent parseIntent(XmlPullParser parser) {
            String uri = null;
            try {
                uri = DefaultLayoutParser.getAttributeValue(parser, "uri");
                Intent intent = Intent.parseUri(uri, 0);
                if (intent == null || intent.getComponent() == null || !IconView.KNOX_SHORTCUT_PACKAGE.equals(intent.getComponent().getPackageName())) {
                    return intent;
                }
                return null;
            } catch (URISyntaxException e) {
                Log.w(HomeRestoreLayoutParser.TAG, "Shortcut has malformed uri: " + uri);
                return null;
            }
        }
    }

    HomeRestoreLayoutParser(Context context, FavoritesProvider favoritesProvider, XmlPullParser parser, ArrayList<String> restoredTable) {
        super(context, favoritesProvider.getAppWidgetHost(), favoritesProvider, context.getResources(), 0, null);
        this.mFavoritesProvider = favoritesProvider;
        this.mRestoredTable = restoredTable;
        this.mParser = parser;
    }

    protected HashMap<String, TagParser> getFolderElementsMap() {
        HashMap<String, TagParser> parsers = new HashMap();
        parsers.put(DefaultLayoutParser.TAG_FAVORITE, new RestoreAppShortcutWithUriParser());
        parsers.put(DefaultLayoutParser.TAG_SHORTCUT, new RestoreShortcutParser());
        return parsers;
    }

    protected HashMap<String, TagParser> getLayoutElementsMap() {
        HashMap<String, TagParser> parsers = new HashMap();
        parsers.put(DefaultLayoutParser.TAG_FAVORITE, new RestoreAppShortcutParser());
        parsers.put(DefaultLayoutParser.TAG_APPWIDGET, new RestoreAppWidgetParser());
        parsers.put(DefaultLayoutParser.TAG_SHORTCUT, new RestoreUriShortcutParser());
        if (LauncherFeature.supportDeepShortcut()) {
            parsers.put(DefaultLayoutParser.TAG_DEEP_SHORTCUT, new RestoreUriShortcutParser());
        }
        if (LauncherFeature.supportPairApps()) {
            parsers.put(DefaultLayoutParser.TAG_PAIRAPPS_SHORTCUT, new RestoreUriShortcutParser());
        }
        parsers.put("folder", new RestoreHomeFolderParser());
        parsers.put(LauncherBnrTag.TAG_APPS_BUTTON, new AppsButtonParser());
        return parsers;
    }

    protected int parseLayout(ArrayList<Long> screenIds) {
        this.mTagParserMap.clear();
        this.mTagParserMap.put("category", new CategoryParser());
        this.mRestoreAppWidgetId.clear();
        int count = 0;
        try {
            int depth = this.mParser.getDepth();
            HashMap<String, TagParser> tagParserMap = getLayoutElementsMap();
            while (true) {
                int type = this.mParser.next();
                if ((type != 3 || this.mParser.getDepth() > depth) && type != 1) {
                    if (type == 2) {
                        String name = this.mParser.getName();
                        if (this.mTagParserMap.containsKey(name)) {
                            Log.i(TAG, "restore tag : " + name);
                            String tableName = LauncherBnrHelper.getFavoritesTable(name);
                            if ("hotseat".equals(name)) {
                                LauncherAppState.getInstance().removeAppsButtonPref(false);
                            } else if (LauncherBnrTag.TAG_HOTSEAT_EASY.equals(name)) {
                                LauncherAppState.getInstance().removeAppsButtonPref(true);
                            }
                            Object obj = -1;
                            switch (name.hashCode()) {
                                case -842449841:
                                    if (name.equals(LauncherBnrTag.TAG_HOTSEAT_EASY)) {
                                        obj = 5;
                                        break;
                                    }
                                    break;
                                case -486342357:
                                    if (name.equals(LauncherBnrTag.TAG_HOMEONLY)) {
                                        obj = 1;
                                        break;
                                    }
                                    break;
                                case 3208415:
                                    if (name.equals("home")) {
                                        obj = null;
                                        break;
                                    }
                                    break;
                                case 606989048:
                                    if (name.equals(LauncherBnrTag.TAG_HOTSEAT_HOMEONLY)) {
                                        obj = 4;
                                        break;
                                    }
                                    break;
                                case 1099592658:
                                    if (name.equals("hotseat")) {
                                        obj = 3;
                                        break;
                                    }
                                    break;
                                case 2117753698:
                                    if (name.equals(LauncherBnrTag.TAG_HOME_EASY)) {
                                        obj = 2;
                                        break;
                                    }
                                    break;
                            }
                            switch (obj) {
                                case null:
                                case 1:
                                case 2:
                                    count += defaultHomeParseAndAddNode(this.mParser, tableName, tagParserMap, screenIds, -100);
                                    break;
                                case 3:
                                case 4:
                                case 5:
                                    count += defaultHomeParseAndAddNode(this.mParser, tableName, tagParserMap, null, Favorites.CONTAINER_HOTSEAT);
                                    break;
                                default:
                                    TagParser tagParser = (TagParser) this.mTagParserMap.get(name);
                                    if (tagParser != null) {
                                        tagParser.parseAndAdd(this.mParser, tableName);
                                        break;
                                    }
                                    Log.d(TAG, "Ignoring unknown element tag : " + name);
                                    return -1;
                            }
                        }
                        continue;
                    }
                }
                if (LauncherFeature.supportFlexibleGrid() && !LauncherBnrHelper.sIsEasyMode) {
                    int[] cellXY = new int[2];
                    Utilities.loadCurrentGridSize(this.mContext, cellXY);
                    LauncherAppState.getInstance().getDeviceProfile().setCurrentGrid(cellXY[0], cellXY[1]);
                }
                restoreAppWidgetIds();
                return count;
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Got exception parsing restore favorites.", e);
        } catch (IOException e2) {
            Log.e(TAG, "Got exception parsing restore favorites.", e2);
        }
    }

    private void restoreContactShortcut(String vcf, long id) {
        FileNotFoundException e;
        IOException e2;
        Throwable th;
        if (vcf == null || id < 0) {
            Log.i(TAG, "vcf is null or id < 0");
            return;
        }
        Writer writer = null;
        try {
            File dir = new File(VCF_RESTORE_PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            StringBuffer filePath = new StringBuffer();
            filePath.append("file://").append(VCF_RESTORE_PATH).append('/').append(id).append(".vcf");
            OutputStream outputStream = this.mContext.getContentResolver().openOutputStream(Uri.parse(filePath.toString()));
            if (outputStream != null) {
                Writer writer2 = new BufferedWriter(new OutputStreamWriter(outputStream));
                try {
                    writer2.write(vcf);
                    Log.i(TAG, "restoreContactShortcut vcf file : " + filePath.toString());
                    SharedPreferences prefs = this.mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
                    Set<String> contactShortcuts = prefs.getStringSet(Utilities.CONTACT_SHORTCUT_IDS, new HashSet());
                    contactShortcuts.add(String.valueOf(id));
                    prefs.edit().putStringSet(Utilities.CONTACT_SHORTCUT_IDS, contactShortcuts).apply();
                    Log.i(TAG, "restoreContactShortcut id add to prefs " + id);
                    writer = writer2;
                } catch (FileNotFoundException e3) {
                    e = e3;
                    writer = writer2;
                    try {
                        Log.e(TAG, "restoreContactShortcut, FileNotFoundException : ", e);
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (IOException e22) {
                                Log.e(TAG, "restoreContactShortcut, IOException : " + e22);
                                return;
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (IOException e222) {
                                Log.e(TAG, "restoreContactShortcut, IOException : " + e222);
                            }
                        }
                        throw th;
                    }
                } catch (IOException e4) {
                    e222 = e4;
                    writer = writer2;
                    Log.e(TAG, "restoreContactShortcut, IOException : " + e222);
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e2222) {
                            Log.e(TAG, "restoreContactShortcut, IOException : " + e2222);
                            return;
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    writer = writer2;
                    if (writer != null) {
                        writer.close();
                    }
                    throw th;
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e22222) {
                    Log.e(TAG, "restoreContactShortcut, IOException : " + e22222);
                }
            }
        } catch (FileNotFoundException e5) {
            e = e5;
            Log.e(TAG, "restoreContactShortcut, FileNotFoundException : ", e);
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e6) {
            e22222 = e6;
            Log.e(TAG, "restoreContactShortcut, IOException : " + e22222);
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void restoreAppWidgetIds() {
        if (this.mRestoreAppWidgetId.size() <= 0) {
            Log.d(TAG, "mRestoreAppWidgetId is empty");
            return;
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.mContext);
        AppWidgetHost appWidgetHost = new AppWidgetHost(this.mContext, 1024);
        ContentValues values = new ContentValues();
        for (Long longValue : this.mRestoreAppWidgetId.keySet()) {
            long dbId = longValue.longValue();
            Pair<Pair<Integer, ComponentName>, String> pairInfo = (Pair) this.mRestoreAppWidgetId.get(Long.valueOf(dbId));
            Pair<Integer, ComponentName> info = pairInfo.first;
            int oldWidgetId = ((Integer) info.first).intValue();
            ComponentName componentName = info.second;
            String tableName = pairInfo.second;
            if (!(componentName == null || tableName == null)) {
                Uri uri;
                int appWidgetId = appWidgetHost.allocateAppWidgetId();
                Bundle options = new Bundle();
                options.putInt("Old_WidgetId", oldWidgetId);
                options.putInt("New_WidgetId", appWidgetId);
                Log.d(TAG, "restoreAppWidgetIds, Old_WidgetId : " + oldWidgetId + ", New_WidgetId : " + appWidgetId);
                Object obj = -1;
                switch (tableName.hashCode()) {
                    case -2051565659:
                        if (tableName.equals(Favorites_Standard.TABLE_NAME)) {
                            obj = null;
                            break;
                        }
                        break;
                    case -866479894:
                        if (tableName.equals(Favorites_Easy.TABLE_NAME)) {
                            obj = 1;
                            break;
                        }
                        break;
                    case 444015833:
                        if (tableName.equals(Favorites_HomeApps.TABLE_NAME)) {
                            obj = 3;
                            break;
                        }
                        break;
                    case 444430867:
                        if (tableName.equals(Favorites_HomeOnly.TABLE_NAME)) {
                            obj = 2;
                            break;
                        }
                        break;
                }
                switch (obj) {
                    case null:
                        uri = Favorites_Standard.getContentUri(dbId);
                        break;
                    case 1:
                        uri = Favorites_Easy.getContentUri(dbId);
                        break;
                    case 2:
                        uri = Favorites_HomeOnly.getContentUri(dbId);
                        break;
                    case 3:
                        uri = Favorites_HomeApps.getContentUri(dbId);
                        break;
                    default:
                        uri = Favorites.getContentUri(dbId);
                        break;
                }
                int result;
                if (appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, componentName, options)) {
                    values.clear();
                    values.put(Favorites.APPWIDGET_ID, Integer.valueOf(appWidgetId));
                    result = LauncherAppState.getLauncherProvider().update(uri, values, null, null);
                    Log.d(TAG, "bind widget id result : " + result);
                } else {
                    appWidgetHost.deleteAppWidgetId(appWidgetId);
                    result = LauncherAppState.getLauncherProvider().delete(uri, null, null);
                    Log.e(TAG, "bind widget id fail : " + componentName + " result : " + result);
                }
            }
        }
        this.mRestoreAppWidgetId.clear();
    }

    private void restoreDeepShortcut(Intent intent) {
        if (intent == null || !Utilities.isDeepShortcut(intent)) {
            Log.d(TAG, "intent is null or not deep shortcut");
            return;
        }
        ShortcutKey key = ShortcutKey.fromIntent(intent, UserHandleCompat.myUserHandle());
        Log.d(TAG, "restoreDeepShortcut key : " + key);
        LauncherAppState.getInstance().getShortcutManager().pinShortcut(key);
    }
}

package com.android.launcher3.util.logging;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.model.DefaultLayoutParser;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.home.ZeroPageController;
import com.android.launcher3.util.event.CheckLongPressHelper;
import com.samsung.context.sdk.samsunganalytics.a.g.c.a.c;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

public final class GSIMLogging extends Logging {
    private static final String APP_ID = "com.sec.android.app.launcher";
    public static final String FEATURE_NAME_APPS_FOLDER_COUNT = "APFO";
    public static final String FEATURE_NAME_APPS_FOLDER_NAME = "APFN";
    public static final String FEATURE_NAME_APPS_ICON_STARTED = "APIS";
    public static final String FEATURE_NAME_APPS_PAGE_COUNT = "APPS";
    public static final String FEATURE_NAME_APPS_QUICK_OPTION = "APQO";
    public static final String FEATURE_NAME_APP_LOCK = "APLK";
    public static final String FEATURE_NAME_APP_SEARCH = "APSC";
    public static final String FEATURE_NAME_ATOZ_APPS_REORDER = "AZBT";
    public static final String FEATURE_NAME_AUTO_ALIGN = "ATAN";
    public static final String FEATURE_NAME_CANCEL_DROP_ITEM = "HCMS";
    public static final String FEATURE_NAME_DELETE_APPS_FOLDER = "DTAF";
    public static final String FEATURE_NAME_DELETE_HOME_FOLDER = "DTHF";
    public static final String FEATURE_NAME_DISABLE_APP = "HSDS";
    public static final String FEATURE_NAME_ENTER_ZEROPAGE = "ZPEN";
    public static final String FEATURE_NAME_FOLDER_ADD_APPS_IN_APPS = "FAAA";
    public static final String FEATURE_NAME_FOLDER_ADD_APPS_IN_HOME = "FAAH";
    public static final String FEATURE_NAME_FOLDER_ADD_MULTIPLE_APPS = "FAMA";
    public static final String FEATURE_NAME_FOLDER_N_SEC_OPEN = "FNSO";
    public static final String FEATURE_NAME_GRID_STATUS = "HSGR";
    public static final String FEATURE_NAME_HOME_DEFAULT_ICON_CLICK = "HDIC";
    public static final String FEATURE_NAME_HOME_DEFAULT_PAGE_INDEX = "HDPI";
    public static final String FEATURE_NAME_HOME_EDIT_ENTER = "HOEE";
    public static final String FEATURE_NAME_HOME_EDIT_OPTION = "HOEO";
    public static final String FEATURE_NAME_HOME_EMPTY_PAGE_COUNT = "HEPC";
    public static final String FEATURE_NAME_HOME_FOLDER_COUNT = "HSFO";
    public static final String FEATURE_NAME_HOME_FOLDER_NAME = "HOFN";
    public static final String FEATURE_NAME_HOME_ICON_STARTED = "HOIS";
    public static final String FEATURE_NAME_HOME_ITEM_COUNT = "HOIC";
    public static final String FEATURE_NAME_HOME_ONLY_MODE_ENABLED = "HOMD";
    public static final String FEATURE_NAME_HOME_PAGE_COUNT = "HOME";
    public static final String FEATURE_NAME_HOME_PAGE_REORDER = "HPRO";
    public static final String FEATURE_NAME_HOME_QUICK_OPTION = "HSQO";
    public static final String FEATURE_NAME_HOTSEAT_ADD = "HSAD";
    public static final String FEATURE_NAME_HOTSEAT_DELETE = "HSDT";
    public static final String FEATURE_NAME_HOTSEAT_LIST = "HST";
    public static final String FEATURE_NAME_ITEM_ARRANGMENT = "IWAR";
    public static final String FEATURE_NAME_SEARCH_WIDGET_STARTED = "GSWS";
    public static final String FEATURE_NAME_WIDGET_ADD = "WGAD";
    public static final String FEATURE_NAME_WIDGET_COUNT = "WGCT";
    public static final String FEATURE_NAME_WIDGET_DELETE = "WGDT";
    public static final String FEATURE_NAME_WIDGET_LIST = "LIST";
    public static final String FEATURE_NAME_WIDGET_SEARCH = "WGSC";
    public static final String FEATURE_NAME_ZERO_PAGE_ENABLED = "ZPON";
    public static final String FEATURE_NAME_ZERO_PAGE_STAY_TIME = "ZPST";
    public static final String HOME_EDIT_OPTION_SETTINGS = "Settings";
    public static final String HOME_EDIT_OPTION_WALLPAPER_AND_THEME = "Wallpaper and theme";
    public static final String HOME_EDIT_OPTION_WIDGET = "Widget";
    public static final String HOME_EDIT_OPTION_ZEROPAGE = "Zero page";
    private static final String PREFERECES_WEEKLOGGING_WEEK_NUMBER = "week_of_year_number";
    private static final String TAG = "Launcher.GSIMLogging";

    private static class PreferencesLogging {
        static final String prefName = LauncherAppState.getSharedPreferencesKey();

        private PreferencesLogging() {
        }

        public static void setWeekOfYearNumber(int weekofyear) {
            Editor editor = Logging.sContext.getSharedPreferences(prefName, 0).edit();
            editor.putInt(GSIMLogging.PREFERECES_WEEKLOGGING_WEEK_NUMBER, weekofyear);
            editor.apply();
        }

        public static int getWeekOfYearNumber() {
            return Logging.sContext.getSharedPreferences(prefName, 0).getInt(GSIMLogging.PREFERECES_WEEKLOGGING_WEEK_NUMBER, -1);
        }

        public static void setZeroPageStartTime() {
            long startTime = System.currentTimeMillis();
            Editor editor = Logging.sContext.getSharedPreferences(prefName, 0).edit();
            editor.putLong(GSIMLogging.FEATURE_NAME_ZERO_PAGE_STAY_TIME, startTime);
            editor.apply();
        }

        public static long getZeroPageStayTime() {
            if (Logging.sContext == null) {
                return -1;
            }
            long endTime = System.currentTimeMillis();
            SharedPreferences pref = Logging.sContext.getSharedPreferences(prefName, 0);
            long startTime = pref.getLong(GSIMLogging.FEATURE_NAME_ZERO_PAGE_STAY_TIME, -1);
            pref.edit().remove(GSIMLogging.FEATURE_NAME_ZERO_PAGE_STAY_TIME).apply();
            if (startTime == -1 || startTime > endTime) {
                return -1;
            }
            return (endTime - startTime) / 1000;
        }
    }

    private static class SingletonHolder {
        private static final GSIMLogging sGSIMLoggingInstance = new GSIMLogging();

        private SingletonHolder() {
        }
    }

    public static GSIMLogging getInstance() {
        return SingletonHolder.sGSIMLoggingInstance;
    }

    public void insertLogging(String feature, String extra, long value, boolean status) {
        if (LauncherFeature.supportContextServiceSurveyMode()) {
            final String str = feature;
            final String str2 = extra;
            final long j = value;
            final boolean z = status;
            runOnLoggingThread(new Runnable() {
                public void run() {
                    ContentValues cv = new ContentValues();
                    cv.put("app_id", "com.sec.android.app.launcher");
                    cv.put("feature", str);
                    if (str2 != null) {
                        cv.put("extra", str2);
                    }
                    if (j != -1) {
                        cv.put("value", Long.valueOf(j * 1000));
                    }
                    Intent broadcastIntent = new Intent();
                    if (z) {
                        broadcastIntent.setAction("com.samsung.android.providers.context.log.action.REPORT_APP_STATUS_SURVEY");
                    } else {
                        broadcastIntent.setAction("com.samsung.android.providers.context.log.action.USE_APP_FEATURE_SURVEY");
                    }
                    broadcastIntent.putExtra(c.c, cv);
                    broadcastIntent.setPackage("com.samsung.android.providers.context");
                    Logging.sContext.sendBroadcast(broadcastIntent);
                }
            });
        }
    }

    public void insertMultiLogging(final ArrayList<GSIMLoggingInfo> loggingInfo) {
        if (LauncherFeature.supportContextServiceSurveyMode()) {
            runOnLoggingThread(new Runnable() {
                public void run() {
                    ContentValues[] cvs = new ContentValues[loggingInfo.size()];
                    int i = 0;
                    Iterator it = loggingInfo.iterator();
                    while (it.hasNext()) {
                        GSIMLoggingInfo log = (GSIMLoggingInfo) it.next();
                        cvs[i] = new ContentValues();
                        cvs[i].put("app_id", "com.sec.android.app.launcher");
                        cvs[i].put("feature", log.getFeatures());
                        if (log.getExtras() != null) {
                            cvs[i].put("extra", log.getExtras());
                        }
                        if (log.getValues() != -1) {
                            cvs[i].put("value", Long.valueOf(log.getValues() * 1000));
                        }
                        i++;
                    }
                    Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction("com.samsung.android.providers.context.log.action.REPORT_MULTI_APP_STATUS_SURVEY");
                    broadcastIntent.putExtra(c.c, cvs);
                    broadcastIntent.setPackage("com.samsung.android.providers.context");
                    Logging.sContext.sendBroadcast(broadcastIntent);
                }
            });
        }
    }

    public void runAllStatusLogging() {
        if (LauncherFeature.supportContextServiceSurveyMode()) {
            runOnLoggingThread(new Runnable() {
                public void run() {
                    int weekofyear = Calendar.getInstance().get(3);
                    if (PreferencesLogging.getWeekOfYearNumber() != weekofyear) {
                        GSIMLogging.this.homeWidgetListLogging();
                        GSIMLogging.this.homeDefaultPageIndexLogging();
                        GSIMLogging.this.homeItemCountLogging();
                        GSIMLogging.this.zeroPageStatusLogging();
                        GSIMLogging.this.homeScreenModeLogging();
                        PreferencesLogging.setWeekOfYearNumber(weekofyear);
                        GSIMLogging.this.insertMultiLogging(GSIMLogging.this.hotseatListLogging());
                        GSIMLogging.this.insertLogging("HOME", null, (long) GSIMLogging.this.getHomePageCount(), true);
                        GSIMLogging.this.insertLogging(GSIMLogging.FEATURE_NAME_HOME_EMPTY_PAGE_COUNT, null, (long) GSIMLogging.this.getHomeEmptyPageCount(), true);
                    }
                }
            });
        }
    }

    public void runFirstAppStatusLogging() {
        if (LauncherFeature.supportContextServiceSurveyMode()) {
            runOnLoggingThread(new Runnable() {
                public void run() {
                    ArrayList<GSIMLoggingInfo> loggingInfo = new ArrayList();
                    int numOfHomeFolders = GSIMLogging.this.getItemCountByContainer(-100, true);
                    int numOfAppsFolders = GSIMLogging.this.getItemCountByContainer(Favorites.CONTAINER_APPS, true);
                    int homeFolderNameValue = GSIMLogging.this.getNamedFolderCount(numOfHomeFolders, -100);
                    int appFolderNameValue = GSIMLogging.this.getNamedFolderCount(numOfAppsFolders, Favorites.CONTAINER_APPS);
                    loggingInfo.add(new GSIMLoggingInfo("HOME", null, (long) GSIMLogging.this.getHomePageCount()));
                    loggingInfo.add(new GSIMLoggingInfo(GSIMLogging.FEATURE_NAME_APPS_PAGE_COUNT, null, (long) GSIMLogging.this.getAppsPageCount()));
                    loggingInfo.add(new GSIMLoggingInfo(GSIMLogging.FEATURE_NAME_HOME_FOLDER_COUNT, null, (long) numOfHomeFolders));
                    loggingInfo.add(new GSIMLoggingInfo(GSIMLogging.FEATURE_NAME_APPS_FOLDER_COUNT, null, (long) numOfAppsFolders));
                    loggingInfo.add(new GSIMLoggingInfo(GSIMLogging.FEATURE_NAME_HOME_FOLDER_NAME, String.valueOf(homeFolderNameValue), -1));
                    loggingInfo.add(new GSIMLoggingInfo(GSIMLogging.FEATURE_NAME_APPS_FOLDER_NAME, String.valueOf(appFolderNameValue), -1));
                    loggingInfo.add(new GSIMLoggingInfo(GSIMLogging.FEATURE_NAME_GRID_STATUS, GSIMLogging.this.getGridInfo(), -1));
                    loggingInfo.addAll(GSIMLogging.this.hotseatListLogging());
                    GSIMLogging.this.insertMultiLogging(loggingInfo);
                }
            });
        }
    }

    public void setZeroPageStartTime() {
        PreferencesLogging.setZeroPageStartTime();
    }

    public long getZeroPageStayTime() {
        return PreferencesLogging.getZeroPageStayTime();
    }

    public int classifyZeroPageStayTime(long time) {
        if (time >= 300) {
            return CheckLongPressHelper.LONG_PRESS_TIME_OUT_DEFAULT;
        }
        if (time >= 60) {
            return 60;
        }
        if (time >= 30) {
            return 30;
        }
        if (time >= 5) {
            return 5;
        }
        return 0;
    }

    private String getGridInfo() {
        int[] cellXY = new int[2];
        Utilities.loadCurrentGridSize(sContext, cellXY);
        String column = String.valueOf(cellXY[0]);
        return column + DefaultLayoutParser.ATTR_X + String.valueOf(cellXY[1]);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private java.util.ArrayList<com.android.launcher3.util.logging.GSIMLoggingInfo> hotseatListLogging() {
        /*
        r22 = this;
        r15 = new java.util.ArrayList;
        r15.<init>();
        r4 = 1;
        r6 = new java.lang.String[r4];
        r4 = 0;
        r5 = "intent";
        r6[r4] = r5;
        r7 = "container=-101";
        r9 = "screen";
        r4 = sContext;
        r4 = r4.getContentResolver();
        r5 = com.android.launcher3.common.model.LauncherSettings.Favorites.CONTENT_URI;
        r8 = 0;
        r10 = r4.query(r5, r6, r7, r8, r9);
        r17 = 0;
        if (r10 == 0) goto L_0x008b;
    L_0x0022:
        r4 = "intent";
        r13 = r10.getColumnIndexOrThrow(r4);	 Catch:{ Exception -> 0x006b }
    L_0x0028:
        r4 = r10.moveToNext();	 Catch:{ Exception -> 0x006b }
        if (r4 == 0) goto L_0x0090;
    L_0x002e:
        r17 = r17 + 1;
        r16 = r10.getString(r13);	 Catch:{ URISyntaxException -> 0x008f, Exception -> 0x0094 }
        if (r16 == 0) goto L_0x008c;
    L_0x0036:
        r4 = 0;
        r0 = r16;
        r14 = android.content.Intent.parseUri(r0, r4);	 Catch:{ URISyntaxException -> 0x008f, Exception -> 0x0094 }
        r4 = r14.getComponent();	 Catch:{ URISyntaxException -> 0x008f, Exception -> 0x0094 }
        r12 = r4.getPackageName();	 Catch:{ URISyntaxException -> 0x008f, Exception -> 0x0094 }
    L_0x0045:
        r18 = new com.android.launcher3.util.logging.GSIMLoggingInfo;	 Catch:{ Exception -> 0x006b }
        r4 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x006b }
        r4.<init>();	 Catch:{ Exception -> 0x006b }
        r5 = "HST";
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x006b }
        r0 = r17;
        r4 = r4.append(r0);	 Catch:{ Exception -> 0x006b }
        r4 = r4.toString();	 Catch:{ Exception -> 0x006b }
        r20 = -1;
        r0 = r18;
        r1 = r20;
        r0.<init>(r4, r12, r1);	 Catch:{ Exception -> 0x006b }
        r0 = r18;
        r15.add(r0);	 Catch:{ Exception -> 0x006b }
        goto L_0x0028;
    L_0x006b:
        r11 = move-exception;
        r4 = "Launcher.GSIMLogging";
        r5 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0096 }
        r5.<init>();	 Catch:{ all -> 0x0096 }
        r8 = "hotseatListLogging Exception : ";
        r5 = r5.append(r8);	 Catch:{ all -> 0x0096 }
        r8 = r11.toString();	 Catch:{ all -> 0x0096 }
        r5 = r5.append(r8);	 Catch:{ all -> 0x0096 }
        r5 = r5.toString();	 Catch:{ all -> 0x0096 }
        android.util.Log.e(r4, r5);	 Catch:{ all -> 0x0096 }
        r10.close();
    L_0x008b:
        return r15;
    L_0x008c:
        r12 = "Folder";
        goto L_0x0045;
    L_0x008f:
        r11 = move-exception;
    L_0x0090:
        r10.close();
        goto L_0x008b;
    L_0x0094:
        r11 = move-exception;
        goto L_0x0090;
    L_0x0096:
        r4 = move-exception;
        r10.close();
        throw r4;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.util.logging.GSIMLogging.hotseatListLogging():java.util.ArrayList<com.android.launcher3.util.logging.GSIMLoggingInfo>");
    }

    private void homeWidgetListLogging() {
        Cursor cursor = sContext.getContentResolver().query(Favorites.CONTENT_URI, null, "itemType=4", null, "screen");
        if (cursor != null) {
            try {
                ArrayList<GSIMLoggingInfo> infos = new ArrayList();
                while (cursor.moveToNext()) {
                    try {
                        String provider = cursor.getString(cursor.getColumnIndexOrThrow(Favorites.APPWIDGET_PROVIDER));
                        if (provider != null) {
                            infos.add(new GSIMLoggingInfo(FEATURE_NAME_WIDGET_LIST, provider.split("/")[0], -1));
                        }
                    } catch (Exception e) {
                    }
                }
                insertLogging(FEATURE_NAME_WIDGET_COUNT, null, (long) cursor.getCount(), true);
                insertMultiLogging(infos);
            } catch (Exception e2) {
                Log.e(TAG, "homeWidgetListLogging Exception : " + e2.toString());
            } finally {
                cursor.close();
            }
        }
    }

    private void homeDefaultPageIndexLogging() {
        insertLogging(FEATURE_NAME_HOME_DEFAULT_PAGE_INDEX, Integer.toString(Utilities.getHomeDefaultPageKey(sContext)), -1, true);
    }

    private void homeItemCountLogging() {
        insertLogging(FEATURE_NAME_HOME_ITEM_COUNT, null, (long) getItemCountByContainer(-100, false), true);
    }

    private void homeScreenModeLogging() {
        if (LauncherFeature.supportHomeModeChange()) {
            insertLogging(FEATURE_NAME_HOME_ONLY_MODE_ENABLED, null, (long) (LauncherAppState.getInstance().isHomeOnlyModeEnabled() ? 1 : 0), true);
        }
    }

    private void zeroPageStatusLogging() {
        insertLogging(FEATURE_NAME_ZERO_PAGE_ENABLED, null, (long) (ZeroPageController.isEnableZeroPage() ? 1 : 0), true);
    }

    public int getFolderNameValue(long container) {
        if (LauncherFeature.supportContextServiceSurveyMode()) {
            if (container == -100) {
                return getNamedFolderCount(getItemCountByContainer(-100, true), -100);
            }
            if (container == -102) {
                return getNamedFolderCount(getItemCountByContainer(Favorites.CONTAINER_APPS, true), Favorites.CONTAINER_APPS);
            }
        }
        return -1;
    }
}

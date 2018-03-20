package com.android.launcher3.util.logging;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.util.Log;
import android.util.Pair;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens;
import com.android.launcher3.util.PairAppsUtilities;
import com.sec.android.app.launcher.R;
import java.net.URISyntaxException;
import java.util.ArrayList;

class SALogUtils {
    private static final String TAG = "Launcher.SALogUtils";

    static class GSW {
        private static final String PREFERECES_ENTER_GSW_COUNT = "enter_gsw_count";
        static final String prefName = LauncherAppState.getSharedPreferencesKey();
        String location;
        int page;
        String size;

        GSW(String s, int p, String l) {
            this.size = s;
            this.page = p;
            this.location = l;
        }

        static void insertEnterSearchCount(Context context, boolean update) {
            if (update) {
                setEnterCountPref(context);
            }
            SALogging.getInstance().insertStatusLog(context.getResources().getString(R.string.status_CountOfEnterGSW), getEnterCountPref(context));
        }

        static void setEnterCountPref(Context context) {
            Editor editor = context.getSharedPreferences(prefName, 0).edit();
            editor.putInt(PREFERECES_ENTER_GSW_COUNT, getEnterCountPref(context) + 1);
            editor.apply();
        }

        static int getEnterCountPref(Context context) {
            return context.getSharedPreferences(prefName, 0).getInt(PREFERECES_ENTER_GSW_COUNT, 0);
        }
    }

    static class Items {
        String itemNames;
        int itemcount;

        Items(int count, String shortcuts) {
            this.itemcount = count;
            this.itemNames = shortcuts;
        }
    }

    SALogUtils() {
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static int getFolderCountInHome(android.content.Context r10) {
        /*
        r4 = 61;
        r2 = 0;
        r8 = 0;
        r9 = "itemType is 2";
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r0 = r0.append(r9);
        r1 = " AND (";
        r0 = r0.append(r1);
        r1 = "container";
        r0 = r0.append(r1);
        r0 = r0.append(r4);
        r1 = -100;
        r0 = r0.append(r1);
        r1 = " OR ";
        r0 = r0.append(r1);
        r1 = "container";
        r0 = r0.append(r1);
        r0 = r0.append(r4);
        r1 = -101; // 0xffffffffffffff9b float:NaN double:NaN;
        r0 = r0.append(r1);
        r1 = ")";
        r0 = r0.append(r1);
        r3 = r0.toString();
        r0 = r10.getContentResolver();
        r1 = com.android.launcher3.common.model.LauncherSettings.Favorites.CONTENT_URI;
        r4 = r2;
        r5 = r2;
        r6 = r0.query(r1, r2, r3, r4, r5);
        if (r6 == 0) goto L_0x0060;
    L_0x0053:
        r0 = r6.getCount();	 Catch:{ Exception -> 0x0061 }
        if (r0 <= 0) goto L_0x005d;
    L_0x0059:
        r8 = r6.getCount();	 Catch:{ Exception -> 0x0061 }
    L_0x005d:
        r6.close();
    L_0x0060:
        return r8;
    L_0x0061:
        r7 = move-exception;
        r0 = "Launcher.SALogUtils";
        r1 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0082 }
        r1.<init>();	 Catch:{ all -> 0x0082 }
        r2 = "getHomeFolderCount Exception : ";
        r1 = r1.append(r2);	 Catch:{ all -> 0x0082 }
        r2 = r7.toString();	 Catch:{ all -> 0x0082 }
        r1 = r1.append(r2);	 Catch:{ all -> 0x0082 }
        r1 = r1.toString();	 Catch:{ all -> 0x0082 }
        android.util.Log.e(r0, r1);	 Catch:{ all -> 0x0082 }
        r6.close();
        goto L_0x0060;
    L_0x0082:
        r0 = move-exception;
        r6.close();
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.util.logging.SALogUtils.getFolderCountInHome(android.content.Context):int");
    }

    static ArrayList<String> getHotseatAppItems(Context context) {
        int maxCount = LauncherAppState.getInstance().getDeviceProfile().getMaxHotseatCount();
        ArrayList<String> items = new ArrayList();
        String[] projection = new String[]{"intent", "itemType"};
        Cursor cursor = context.getContentResolver().query(Favorites.CONTENT_URI, projection, "container=-101", null, "screen");
        if (cursor != null) {
            try {
                int indexIntent = cursor.getColumnIndexOrThrow("intent");
                int indexType = cursor.getColumnIndexOrThrow("itemType");
                while (cursor.moveToNext()) {
                    String packageName = cursor.getString(indexIntent);
                    String detail = "Empty";
                    if (packageName != null) {
                        Intent intent = Intent.parseUri(packageName, 0);
                        if (intent != null) {
                            if (intent.getComponent() != null) {
                                detail = intent.getComponent().getPackageName();
                            } else if (intent.getAction() != null && intent.getAction().equals(Utilities.ACTION_SHOW_APPS_VIEW)) {
                                detail = "AppsButton";
                            }
                        }
                    } else if (cursor.getInt(indexType) == 2) {
                        detail = "Folder";
                    }
                    items.add(detail);
                }
                int rem = maxCount - items.size();
                for (int j = 0; j < rem; j++) {
                    items.add(" ");
                }
            } catch (URISyntaxException e) {
                Log.e(TAG, "HotseatAppItemsLogging Exception : " + e.toString());
            } catch (Exception e2) {
                Log.e(TAG, "HotseatAppItemsLogging Exception : " + e2.toString());
            } finally {
                cursor.close();
            }
        }
        return items;
    }

    static Items getHomeApps(Context context, boolean isDefault) {
        String[] projection = new String[]{"intent"};
        String where = "(itemType is 0 or itemType is 1)" + " AND (" + "container" + '=' + -100 + ")";
        if (isDefault) {
            where = where + " AND " + "screen" + '=' + getDefaultScreenId(context);
        }
        String appItem = "";
        int count = 0;
        Cursor cursor = context.getContentResolver().query(Favorites.CONTENT_URI, projection, where, null, null);
        if (cursor != null) {
            try {
                int indexIntent = cursor.getColumnIndexOrThrow("intent");
                while (cursor.moveToNext()) {
                    String intentString = cursor.getString(indexIntent);
                    if (intentString != null) {
                        Intent intent = Intent.parseUri(intentString, 0);
                        if (!(intent == null || intent.getComponent() == null)) {
                            appItem = appItem + intent.getComponent().getPackageName() + ", ";
                            count++;
                        }
                    }
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (Exception e2) {
                Log.e(TAG, "HomeappListLogging Exception : " + e2.toString());
            } finally {
                cursor.close();
            }
        }
        return new Items(count, appItem);
    }

    static Items getHomePairApps(Context context) {
        String appItem = "";
        int count = 0;
        Cursor cursor = context.getContentResolver().query(Favorites.CONTENT_URI, new String[]{"intent"}, "(itemType is 7)" + " AND (" + "container" + '=' + -100 + ")", null, null);
        if (cursor != null) {
            try {
                int indexIntent = cursor.getColumnIndexOrThrow("intent");
                while (cursor.moveToNext()) {
                    String intentString = cursor.getString(indexIntent);
                    if (intentString != null && PairAppsUtilities.isValidComponents(context, intentString)) {
                        count++;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "PairAppsListLogging Exception : " + e.toString());
            } finally {
                cursor.close();
            }
        }
        return new Items(count, appItem);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static com.android.launcher3.util.logging.SALogUtils.GSW getGSWData(android.content.Context r15) {
        /*
        r4 = 0;
        r6 = "com.google.android.googlequicksearchbox/com.google.android.googlequicksearchbox.SearchWidgetProvider";
        r0 = 5;
        r2 = new java.lang.String[r0];
        r0 = 0;
        r1 = "spanX";
        r2[r0] = r1;
        r0 = 1;
        r1 = "spanY";
        r2[r0] = r1;
        r0 = 2;
        r1 = "screen";
        r2[r0] = r1;
        r0 = 3;
        r1 = "cellX";
        r2[r0] = r1;
        r0 = 4;
        r1 = "cellY";
        r2[r0] = r1;
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r1 = "itemType=4 AND appWidgetProvider='";
        r0 = r0.append(r1);
        r0 = r0.append(r6);
        r1 = "'";
        r0 = r0.append(r1);
        r3 = r0.toString();
        r0 = r15.getContentResolver();
        r1 = com.android.launcher3.common.model.LauncherSettings.Favorites.CONTENT_URI;
        r5 = r4;
        r9 = r0.query(r1, r2, r3, r4, r5);
        r10 = 0;
        if (r9 == 0) goto L_0x00a4;
    L_0x0046:
        r0 = r9.moveToNext();	 Catch:{ Exception -> 0x00a5 }
        if (r0 == 0) goto L_0x00a1;
    L_0x004c:
        r0 = "spanX";
        r0 = r9.getColumnIndexOrThrow(r0);	 Catch:{ Exception -> 0x00a5 }
        r14 = r9.getInt(r0);	 Catch:{ Exception -> 0x00a5 }
        r0 = "screen";
        r0 = r9.getColumnIndexOrThrow(r0);	 Catch:{ Exception -> 0x00a5 }
        r13 = r9.getInt(r0);	 Catch:{ Exception -> 0x00a5 }
        r0 = "cellX";
        r0 = r9.getColumnIndexOrThrow(r0);	 Catch:{ Exception -> 0x00a5 }
        r7 = r9.getInt(r0);	 Catch:{ Exception -> 0x00a5 }
        r0 = "cellY";
        r0 = r9.getColumnIndexOrThrow(r0);	 Catch:{ Exception -> 0x00a5 }
        r8 = r9.getInt(r0);	 Catch:{ Exception -> 0x00a5 }
        r11 = new com.android.launcher3.util.logging.SALogUtils$GSW;	 Catch:{ Exception -> 0x00a5 }
        r0 = java.lang.Integer.toString(r14);	 Catch:{ Exception -> 0x00a5 }
        r1 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x00a5 }
        r1.<init>();	 Catch:{ Exception -> 0x00a5 }
        r4 = "[";
        r1 = r1.append(r4);	 Catch:{ Exception -> 0x00a5 }
        r1 = r1.append(r7);	 Catch:{ Exception -> 0x00a5 }
        r4 = ", ";
        r1 = r1.append(r4);	 Catch:{ Exception -> 0x00a5 }
        r1 = r1.append(r8);	 Catch:{ Exception -> 0x00a5 }
        r4 = "]";
        r1 = r1.append(r4);	 Catch:{ Exception -> 0x00a5 }
        r1 = r1.toString();	 Catch:{ Exception -> 0x00a5 }
        r11.<init>(r0, r13, r1);	 Catch:{ Exception -> 0x00a5 }
        r10 = r11;
    L_0x00a1:
        r9.close();
    L_0x00a4:
        return r10;
    L_0x00a5:
        r12 = move-exception;
        r0 = "Launcher.SALogUtils";
        r1 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00c6 }
        r1.<init>();	 Catch:{ all -> 0x00c6 }
        r4 = "GoogleSearchWidgetLogging Exception : ";
        r1 = r1.append(r4);	 Catch:{ all -> 0x00c6 }
        r4 = r12.toString();	 Catch:{ all -> 0x00c6 }
        r1 = r1.append(r4);	 Catch:{ all -> 0x00c6 }
        r1 = r1.toString();	 Catch:{ all -> 0x00c6 }
        android.util.Log.e(r0, r1);	 Catch:{ all -> 0x00c6 }
        r9.close();
        goto L_0x00a4;
    L_0x00c6:
        r0 = move-exception;
        r9.close();
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.util.logging.SALogUtils.getGSWData(android.content.Context):com.android.launcher3.util.logging.SALogUtils$GSW");
    }

    static Items getHomeWidgetList(Context context, boolean isDefault) {
        String widgetList = "";
        String[] projection = new String[]{Favorites.APPWIDGET_PROVIDER};
        String where = "itemType=4";
        String sortOrder = "screen";
        if (isDefault) {
            where = where + " AND " + "screen" + '=' + getDefaultScreenId(context);
        }
        Cursor cursor = context.getContentResolver().query(Favorites.CONTENT_URI, projection, where, null, sortOrder);
        int count = 0;
        if (cursor != null) {
            try {
                int appWidgetProvider = cursor.getColumnIndexOrThrow(Favorites.APPWIDGET_PROVIDER);
                while (cursor.moveToNext()) {
                    String widgetName = cursor.getString(appWidgetProvider);
                    if (widgetName != null) {
                        widgetList = widgetList + ComponentName.unflattenFromString(widgetName).getPackageName() + ", ";
                        count++;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "homeWidgetListLogging Exception : " + e.toString());
            } finally {
                cursor.close();
            }
        }
        return new Items(count, widgetList);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static int countFolderColorNotDefault(android.content.Context r12, boolean r13) {
        /*
        r2 = 0;
        r10 = 0;
        r11 = "itemType is 2";
        r7 = "container=-100 OR container=-101";
        r6 = "color <> -1 AND color <> 0";
        if (r13 != 0) goto L_0x000c;
    L_0x000a:
        r7 = "container=-102";
    L_0x000c:
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r0 = r0.append(r11);
        r1 = " AND (";
        r0 = r0.append(r1);
        r0 = r0.append(r7);
        r1 = ") AND ";
        r0 = r0.append(r1);
        r0 = r0.append(r6);
        r3 = r0.toString();
        r0 = r12.getContentResolver();
        r1 = com.android.launcher3.common.model.LauncherSettings.Favorites.CONTENT_URI;
        r4 = r2;
        r5 = r2;
        r8 = r0.query(r1, r2, r3, r4, r5);
        if (r8 == 0) goto L_0x0048;
    L_0x003b:
        r0 = r8.getCount();	 Catch:{ Exception -> 0x0049 }
        if (r0 <= 0) goto L_0x0045;
    L_0x0041:
        r10 = r8.getCount();	 Catch:{ Exception -> 0x0049 }
    L_0x0045:
        r8.close();
    L_0x0048:
        return r10;
    L_0x0049:
        r9 = move-exception;
        r0 = "Launcher.SALogUtils";
        r1 = new java.lang.StringBuilder;	 Catch:{ all -> 0x006a }
        r1.<init>();	 Catch:{ all -> 0x006a }
        r2 = "getHomeFolderCount Exception : ";
        r1 = r1.append(r2);	 Catch:{ all -> 0x006a }
        r2 = r9.toString();	 Catch:{ all -> 0x006a }
        r1 = r1.append(r2);	 Catch:{ all -> 0x006a }
        r1 = r1.toString();	 Catch:{ all -> 0x006a }
        android.util.Log.e(r0, r1);	 Catch:{ all -> 0x006a }
        r8.close();
        goto L_0x0048;
    L_0x006a:
        r0 = move-exception;
        r8.close();
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.util.logging.SALogUtils.countFolderColorNotDefault(android.content.Context, boolean):int");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static int getShortcutOnHomeCount(android.content.Context r10) {
        /*
        r2 = 0;
        r9 = "itemType is 1";
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r1 = "(container=-100 OR container=-101) AND ";
        r0 = r0.append(r1);
        r0 = r0.append(r9);
        r3 = r0.toString();
        r8 = 0;
        r0 = r10.getContentResolver();
        r1 = com.android.launcher3.common.model.LauncherSettings.Favorites.CONTENT_URI;
        r4 = r2;
        r5 = r2;
        r6 = r0.query(r1, r2, r3, r4, r5);
        if (r6 == 0) goto L_0x0032;
    L_0x0025:
        r0 = r6.getCount();	 Catch:{ Exception -> 0x0033 }
        if (r0 <= 0) goto L_0x002f;
    L_0x002b:
        r8 = r6.getCount();	 Catch:{ Exception -> 0x0033 }
    L_0x002f:
        r6.close();
    L_0x0032:
        return r8;
    L_0x0033:
        r7 = move-exception;
        r0 = "Launcher.SALogUtils";
        r1 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0054 }
        r1.<init>();	 Catch:{ all -> 0x0054 }
        r2 = "getHome1X1ShortcutsCount Exception : ";
        r1 = r1.append(r2);	 Catch:{ all -> 0x0054 }
        r2 = r7.toString();	 Catch:{ all -> 0x0054 }
        r1 = r1.append(r2);	 Catch:{ all -> 0x0054 }
        r1 = r1.toString();	 Catch:{ all -> 0x0054 }
        android.util.Log.e(r0, r1);	 Catch:{ all -> 0x0054 }
        r6.close();
        goto L_0x0032;
    L_0x0054:
        r0 = move-exception;
        r6.close();
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.util.logging.SALogUtils.getShortcutOnHomeCount(android.content.Context):int");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static int getPinnedShortcutsCount(android.content.Context r9) {
        /*
        r2 = 0;
        r3 = "itemType is 6";
        r8 = 0;
        r0 = r9.getContentResolver();
        r1 = com.android.launcher3.common.model.LauncherSettings.Favorites.CONTENT_URI;
        r4 = r2;
        r5 = r2;
        r6 = r0.query(r1, r2, r3, r4, r5);
        if (r6 == 0) goto L_0x001f;
    L_0x0012:
        r0 = r6.getCount();	 Catch:{ Exception -> 0x0020 }
        if (r0 <= 0) goto L_0x001c;
    L_0x0018:
        r8 = r6.getCount();	 Catch:{ Exception -> 0x0020 }
    L_0x001c:
        r6.close();
    L_0x001f:
        return r8;
    L_0x0020:
        r7 = move-exception;
        r0 = "Launcher.SALogUtils";
        r1 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0041 }
        r1.<init>();	 Catch:{ all -> 0x0041 }
        r2 = "getHome1X1ShortcutsCount Exception : ";
        r1 = r1.append(r2);	 Catch:{ all -> 0x0041 }
        r2 = r7.toString();	 Catch:{ all -> 0x0041 }
        r1 = r1.append(r2);	 Catch:{ all -> 0x0041 }
        r1 = r1.toString();	 Catch:{ all -> 0x0041 }
        android.util.Log.e(r0, r1);	 Catch:{ all -> 0x0041 }
        r6.close();
        goto L_0x001f;
    L_0x0041:
        r0 = move-exception;
        r6.close();
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.util.logging.SALogUtils.getPinnedShortcutsCount(android.content.Context):int");
    }

    static Cursor getFolderItems(Context context, int mode) {
        String where = "itemType is 2" + " AND (";
        if (mode == 1) {
            where = where + "container" + '=' + -100 + " OR " + "container" + '=' + Favorites.CONTAINER_HOTSEAT + ")";
        } else if (mode == 2) {
            where = where + "container" + '=' + Favorites.CONTAINER_APPS + ")";
        }
        Cursor cursor = context.getContentResolver().query(Favorites.CONTENT_URI, null, where, null, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    return cursor;
                }
            } catch (Exception e) {
                Log.e(TAG, "getFolderItems Exception : " + e.toString());
            }
        }
        return cursor;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static int getAppsCountInFolder(android.content.Context r9, int r10) {
        /*
        r2 = 0;
        r8 = 0;
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r1 = "container=";
        r0 = r0.append(r1);
        r0 = r0.append(r10);
        r3 = r0.toString();
        r0 = r9.getContentResolver();
        r1 = com.android.launcher3.common.model.LauncherSettings.Favorites.CONTENT_URI;
        r4 = r2;
        r5 = r2;
        r6 = r0.query(r1, r2, r3, r4, r5);
        if (r6 == 0) goto L_0x0030;
    L_0x0023:
        r0 = r6.getCount();	 Catch:{ Exception -> 0x0031 }
        if (r0 <= 0) goto L_0x002d;
    L_0x0029:
        r8 = r6.getCount();	 Catch:{ Exception -> 0x0031 }
    L_0x002d:
        r6.close();
    L_0x0030:
        return r8;
    L_0x0031:
        r7 = move-exception;
        r0 = "Launcher.SALogUtils";
        r1 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0052 }
        r1.<init>();	 Catch:{ all -> 0x0052 }
        r2 = "getAppsCountInFolder Exception : ";
        r1 = r1.append(r2);	 Catch:{ all -> 0x0052 }
        r2 = r7.toString();	 Catch:{ all -> 0x0052 }
        r1 = r1.append(r2);	 Catch:{ all -> 0x0052 }
        r1 = r1.toString();	 Catch:{ all -> 0x0052 }
        android.util.Log.e(r0, r1);	 Catch:{ all -> 0x0052 }
        r6.close();
        goto L_0x0030;
    L_0x0052:
        r0 = move-exception;
        r6.close();
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.util.logging.SALogUtils.getAppsCountInFolder(android.content.Context, int):int");
    }

    static Pair<Integer, String> getHideApps(Context context) {
        int count = 0;
        String appName = "";
        try {
            Throwable th;
            Cursor cursor = context.getContentResolver().query(Favorites.CONTENT_URI, new String[]{"intent"}, "hidden=?", new String[]{String.valueOf(2)}, null);
            Throwable th2 = null;
            try {
                count = cursor.getCount();
                while (cursor.moveToNext()) {
                    String packageName = cursor.getString(0);
                    if (packageName != null) {
                        Intent intent = Intent.parseUri(packageName, 0);
                        if (!(intent == null || intent.getComponent() == null)) {
                            appName = appName + intent.getComponent().getPackageName() + ", ";
                        }
                    }
                }
                if (cursor != null) {
                    if (null != null) {
                        try {
                            cursor.close();
                        } catch (Throwable th3) {
                            th2.addSuppressed(th3);
                        }
                    } else {
                        cursor.close();
                    }
                }
                return new Pair(Integer.valueOf(count), appName);
            } catch (Throwable th32) {
                Throwable th4 = th32;
                th32 = th2;
                th2 = th4;
            }
            throw th2;
            if (cursor != null) {
                if (th32 != null) {
                    try {
                        cursor.close();
                    } catch (Throwable th5) {
                        th32.addSuppressed(th5);
                    }
                } else {
                    cursor.close();
                }
            }
            throw th2;
        } catch (Exception e) {
            Log.e(TAG, "getHideApps Exception : " + e.toString());
        }
    }

    private static long getDefaultScreenId(Context context) {
        long defaultPageId = -1;
        int defaultPageIndex = Utilities.getHomeDefaultPageKey(context);
        String[] projection = new String[]{"_id"};
        String[] selectionArg = new String[]{String.valueOf(defaultPageIndex)};
        Cursor cursor = context.getContentResolver().query(WorkspaceScreens.CONTENT_URI, projection, "screenRank=?", selectionArg, null);
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    defaultPageId = cursor.getLong(0);
                }
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception : " + e.toString());
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            } catch (Throwable th) {
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            }
        }
        if (defaultPageId == -1) {
            Log.w(TAG, "getDefaultScreenId : defaultPageId = " + defaultPageId);
        }
        return defaultPageId;
    }

    static String getPackageListInFolder(Context context, int container) {
        String findInContainer;
        String ret = "{\"folderList\":[";
        String[] projection = new String[]{"container", "title", "intent", "itemType"};
        if (container == -100) {
            findInContainer = "-100,-101";
        } else {
            findInContainer = String.valueOf(container);
        }
        Cursor cursor = context.getContentResolver().query(Favorites.CONTENT_URI, projection, "container IN (SELECT _id FROM favorites WHERE container IN(" + findInContainer + ") AND " + "itemType" + "=" + 2 + ")", null, "container,rank");
        long folderId = -1;
        while (cursor.moveToNext()) {
            try {
                long cur_folderId = cursor.getLong(0);
                Intent intent = Intent.parseUri(cursor.getString(2), 0);
                if (!(intent == null || intent.getComponent() == null)) {
                    String packageName = intent.getComponent().getPackageName();
                    if (folderId != cur_folderId) {
                        if (folderId == -1) {
                            ret = ret + "{";
                        } else {
                            ret = ret + "]},{";
                        }
                        folderId = cur_folderId;
                        ret = (ret + "\"folderId\":\"" + cursor.getLong(0) + "\",\"packageList\":[") + "\"" + packageName + "\"";
                    } else {
                        ret = ret + ",\"" + packageName + "\"";
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "getPackageListInFolder Exception : " + e.toString());
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            } catch (Throwable th) {
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            }
        }
        if (ret.length() > 0) {
            ret = ret + "]} ]}";
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        return ret;
    }
}

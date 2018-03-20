package com.android.launcher3.util.logging;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.util.TestHelper;
import java.util.ArrayList;

abstract class Logging {
    private static final String TAG = "Launcher.Logging";
    protected static Context sContext;
    private static final Handler sLoggingHandler = new Handler(sLoggingThread.getLooper());
    private static final HandlerThread sLoggingThread = new HandlerThread("loggingThread", 19);

    Logging() {
    }

    static {
        sLoggingThread.start();
    }

    protected void runOnLoggingThread(Runnable r) {
        if (!TestHelper.isRoboUnitTest() && sContext != null) {
            if (sLoggingThread.getThreadId() == Process.myTid()) {
                r.run();
            } else {
                sLoggingHandler.post(r);
            }
        }
    }

    protected void removeCallBacks(Runnable r) {
        sLoggingHandler.removeCallbacks(r);
    }

    protected void runOnLoggingThreadDelayed(Runnable r, int duration) {
        if (!TestHelper.isRoboUnitTest()) {
            if (sLoggingThread.getThreadId() == Process.myTid()) {
                r.run();
            } else {
                sLoggingHandler.postDelayed(r, (long) duration);
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected int getHomePageCount() {
        /*
        r9 = this;
        r2 = 0;
        r8 = -1;
        r0 = sContext;
        r0 = r0.getContentResolver();
        r1 = com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens.CONTENT_URI;
        r3 = r2;
        r4 = r2;
        r5 = r2;
        r6 = r0.query(r1, r2, r3, r4, r5);
        if (r6 == 0) goto L_0x0020;
    L_0x0013:
        r0 = r6.getCount();	 Catch:{ Exception -> 0x0021 }
        if (r0 <= 0) goto L_0x001d;
    L_0x0019:
        r8 = r6.getCount();	 Catch:{ Exception -> 0x0021 }
    L_0x001d:
        r6.close();
    L_0x0020:
        return r8;
    L_0x0021:
        r7 = move-exception;
        r0 = "Launcher.Logging";
        r1 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0042 }
        r1.<init>();	 Catch:{ all -> 0x0042 }
        r2 = "gethomePageCount Exception : ";
        r1 = r1.append(r2);	 Catch:{ all -> 0x0042 }
        r2 = r7.toString();	 Catch:{ all -> 0x0042 }
        r1 = r1.append(r2);	 Catch:{ all -> 0x0042 }
        r1 = r1.toString();	 Catch:{ all -> 0x0042 }
        android.util.Log.e(r0, r1);	 Catch:{ all -> 0x0042 }
        r6.close();
        goto L_0x0020;
    L_0x0042:
        r0 = move-exception;
        r6.close();
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.util.logging.Logging.getHomePageCount():int");
    }

    int getHomeEmptyPageCount() {
        ArrayList<Integer> notEmptyPages = new ArrayList();
        String[] projection = new String[]{"screen"};
        Cursor cursor = sContext.getContentResolver().query(Favorites.CONTENT_URI, projection, "container=-100 AND hidden = 0", null, "screen");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    int newPageIndex = cursor.getInt(cursor.getColumnIndexOrThrow("screen"));
                    if (!notEmptyPages.contains(Integer.valueOf(newPageIndex))) {
                        notEmptyPages.add(Integer.valueOf(newPageIndex));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "getPageCount Exception : " + e.toString());
                } finally {
                    cursor.close();
                }
            }
        }
        return getHomePageCount() - notEmptyPages.size();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected int getAppsPageCount() {
        /*
        r9 = this;
        r2 = 0;
        r8 = -1;
        r5 = "screen desc";
        r3 = "container=-102";
        r0 = sContext;
        r0 = r0.getContentResolver();
        r1 = com.android.launcher3.common.model.LauncherSettings.Favorites.CONTENT_URI;
        r4 = r2;
        r6 = r0.query(r1, r2, r3, r4, r5);
        if (r6 == 0) goto L_0x002d;
    L_0x0015:
        r0 = r6.getCount();	 Catch:{ Exception -> 0x002e }
        if (r0 <= 0) goto L_0x002a;
    L_0x001b:
        r6.moveToFirst();	 Catch:{ Exception -> 0x002e }
        r0 = "screen";
        r0 = r6.getColumnIndex(r0);	 Catch:{ Exception -> 0x002e }
        r0 = r6.getInt(r0);	 Catch:{ Exception -> 0x002e }
        r8 = r0 + 1;
    L_0x002a:
        r6.close();
    L_0x002d:
        return r8;
    L_0x002e:
        r7 = move-exception;
        r0 = "Launcher.Logging";
        r1 = new java.lang.StringBuilder;	 Catch:{ all -> 0x004f }
        r1.<init>();	 Catch:{ all -> 0x004f }
        r2 = "getPageCount Exception : ";
        r1 = r1.append(r2);	 Catch:{ all -> 0x004f }
        r2 = r7.toString();	 Catch:{ all -> 0x004f }
        r1 = r1.append(r2);	 Catch:{ all -> 0x004f }
        r1 = r1.toString();	 Catch:{ all -> 0x004f }
        android.util.Log.e(r0, r1);	 Catch:{ all -> 0x004f }
        r6.close();
        goto L_0x002d;
    L_0x004f:
        r0 = move-exception;
        r6.close();
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.util.logging.Logging.getAppsPageCount():int");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected int getItemCountByContainer(int r11, boolean r12) {
        /*
        r10 = this;
        r2 = 0;
        r8 = 0;
        if (r12 == 0) goto L_0x005e;
    L_0x0004:
        r9 = "itemType is 2";
    L_0x0006:
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r0 = r0.append(r9);
        r1 = " AND ";
        r0 = r0.append(r1);
        r1 = "container";
        r0 = r0.append(r1);
        r1 = 61;
        r0 = r0.append(r1);
        r0 = r0.append(r11);
        r1 = " AND ";
        r0 = r0.append(r1);
        r1 = "hidden";
        r0 = r0.append(r1);
        r1 = " = ";
        r0 = r0.append(r1);
        r1 = 0;
        r0 = r0.append(r1);
        r3 = r0.toString();
        r0 = sContext;
        r0 = r0.getContentResolver();
        r1 = com.android.launcher3.common.model.LauncherSettings.Favorites.CONTENT_URI;
        r4 = r2;
        r5 = r2;
        r6 = r0.query(r1, r2, r3, r4, r5);
        if (r6 == 0) goto L_0x005d;
    L_0x0050:
        r0 = r6.getCount();	 Catch:{ Exception -> 0x0061 }
        if (r0 <= 0) goto L_0x005a;
    L_0x0056:
        r8 = r6.getCount();	 Catch:{ Exception -> 0x0061 }
    L_0x005a:
        r6.close();
    L_0x005d:
        return r8;
    L_0x005e:
        r9 = "(itemType is 0 or itemType is 1)";
        goto L_0x0006;
    L_0x0061:
        r7 = move-exception;
        r0 = "Launcher.Logging";
        r1 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0082 }
        r1.<init>();	 Catch:{ all -> 0x0082 }
        r2 = "getHomeFolderCount Exception : ";
        r1 = r1.append(r2);	 Catch:{ all -> 0x0082 }
        r2 = r7.toString();	 Catch:{ all -> 0x0082 }
        r1 = r1.append(r2);	 Catch:{ all -> 0x0082 }
        r1 = r1.toString();	 Catch:{ all -> 0x0082 }
        android.util.Log.e(r0, r1);	 Catch:{ all -> 0x0082 }
        r6.close();
        goto L_0x005d;
    L_0x0082:
        r0 = move-exception;
        r6.close();
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.util.logging.Logging.getItemCountByContainer(int, boolean):int");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected int getNamedFolderCount(int r10, int r11) {
        /*
        r9 = this;
        r2 = 0;
        if (r10 != 0) goto L_0x0005;
    L_0x0003:
        r0 = 0;
    L_0x0004:
        return r0;
    L_0x0005:
        r8 = 0;
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r1 = "itemType=2 AND container=";
        r0 = r0.append(r1);
        r0 = r0.append(r11);
        r1 = " AND ";
        r0 = r0.append(r1);
        r1 = "title";
        r0 = r0.append(r1);
        r1 = " is not null AND ";
        r0 = r0.append(r1);
        r1 = "title";
        r0 = r0.append(r1);
        r1 = " != ''";
        r0 = r0.append(r1);
        r3 = r0.toString();
        r0 = sContext;
        r0 = r0.getContentResolver();
        r1 = com.android.launcher3.common.model.LauncherSettings.Favorites.CONTENT_URI;
        r4 = r2;
        r5 = r2;
        r6 = r0.query(r1, r2, r3, r4, r5);
        if (r6 == 0) goto L_0x0054;
    L_0x0047:
        r0 = r6.getCount();	 Catch:{ Exception -> 0x0058 }
        if (r0 <= 0) goto L_0x0051;
    L_0x004d:
        r8 = r6.getCount();	 Catch:{ Exception -> 0x0058 }
    L_0x0051:
        r6.close();
    L_0x0054:
        if (r8 != 0) goto L_0x007e;
    L_0x0056:
        r0 = 1;
        goto L_0x0004;
    L_0x0058:
        r7 = move-exception;
        r0 = "Launcher.Logging";
        r1 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0079 }
        r1.<init>();	 Catch:{ all -> 0x0079 }
        r2 = "getNamedFolderCount Exception : ";
        r1 = r1.append(r2);	 Catch:{ all -> 0x0079 }
        r2 = r7.toString();	 Catch:{ all -> 0x0079 }
        r1 = r1.append(r2);	 Catch:{ all -> 0x0079 }
        r1 = r1.toString();	 Catch:{ all -> 0x0079 }
        android.util.Log.e(r0, r1);	 Catch:{ all -> 0x0079 }
        r6.close();
        goto L_0x0054;
    L_0x0079:
        r0 = move-exception;
        r6.close();
        throw r0;
    L_0x007e:
        if (r8 == r10) goto L_0x0082;
    L_0x0080:
        r0 = 2;
        goto L_0x0004;
    L_0x0082:
        r0 = 3;
        goto L_0x0004;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.util.logging.Logging.getNamedFolderCount(int, int):int");
    }
}

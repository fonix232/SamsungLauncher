package com.android.launcher3.util;

import android.content.Context;
import android.net.Uri;
import com.android.launcher3.Utilities;

public final class AppFreezerUtils {
    private static final String APP_FREEZER_PACKAGE = "com.samsung.android.lool";
    private static final int APP_FREEZER_UID_VERSION = 17432585;
    private static final Uri APP_FREEZER_URI = Uri.parse("content://com.samsung.android.sm/AppFreezer");
    private static final String TAG = "AppFreezerUtils";

    public static boolean canPutIntoSleepMode(android.content.Context r11, com.android.launcher3.common.compat.UserHandleCompat r12, java.lang.String r13) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x0053 in list [B:14:0x0050]
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
        r10 = 4;
        r5 = 3;
        r1 = 2;
        r8 = 1;
        r9 = 0;
        r2 = new java.lang.String[r8];
        r0 = "package_name";
        r2[r9] = r0;
        r0 = isSupportUID(r11);
        if (r0 == 0) goto L_0x0072;
    L_0x0011:
        r0 = com.android.launcher3.LauncherFeature.isChinaModel();
        if (r0 == 0) goto L_0x0054;
    L_0x0017:
        r3 = "uid = ? AND package_name = ? AND isAppOptTarget = ? AND (extras = ? OR extras = ?) AND autorun = ?";
        r0 = 6;
        r4 = new java.lang.String[r0];
        r0 = r12.hashCode();
        r0 = java.lang.Integer.toString(r0);
        r4[r9] = r0;
        r4[r8] = r13;
        r0 = "1";
        r4[r1] = r0;
        r0 = "0";
        r4[r5] = r0;
        r0 = "2";
        r4[r10] = r0;
        r0 = 5;
        r1 = "1";
        r4[r0] = r1;
    L_0x0039:
        r6 = 0;
        r0 = r11.getContentResolver();	 Catch:{ Exception -> 0x00a5, all -> 0x00c9 }
        r1 = APP_FREEZER_URI;	 Catch:{ Exception -> 0x00a5, all -> 0x00c9 }
        r5 = 0;	 Catch:{ Exception -> 0x00a5, all -> 0x00c9 }
        r6 = r0.query(r1, r2, r3, r4, r5);	 Catch:{ Exception -> 0x00a5, all -> 0x00c9 }
        if (r6 == 0) goto L_0x00a3;	 Catch:{ Exception -> 0x00a5, all -> 0x00c9 }
    L_0x0047:
        r0 = r6.getCount();	 Catch:{ Exception -> 0x00a5, all -> 0x00c9 }
        if (r0 != r8) goto L_0x00a3;
    L_0x004d:
        r0 = r8;
    L_0x004e:
        if (r6 == 0) goto L_0x0053;
    L_0x0050:
        r6.close();
    L_0x0053:
        return r0;
    L_0x0054:
        r3 = "uid = ? AND package_name = ? AND isAppOptTarget = ? AND (extras = ? OR extras = ?)";
        r0 = 5;
        r4 = new java.lang.String[r0];
        r0 = r12.hashCode();
        r0 = java.lang.Integer.toString(r0);
        r4[r9] = r0;
        r4[r8] = r13;
        r0 = "1";
        r4[r1] = r0;
        r0 = "0";
        r4[r5] = r0;
        r0 = "2";
        r4[r10] = r0;
        goto L_0x0039;
    L_0x0072:
        r0 = com.android.launcher3.LauncherFeature.isChinaModel();
        if (r0 == 0) goto L_0x0090;
    L_0x0078:
        r3 = "package_name = ? AND isAppOptTarget = ? AND (extras = ? OR extras = ?) AND autorun = ?";
        r0 = 5;
        r4 = new java.lang.String[r0];
        r4[r9] = r13;
        r0 = "1";
        r4[r8] = r0;
        r0 = "0";
        r4[r1] = r0;
        r0 = "2";
        r4[r5] = r0;
        r0 = "1";
        r4[r10] = r0;
        goto L_0x0039;
    L_0x0090:
        r3 = "package_name = ? AND isAppOptTarget = ? AND (extras = ? OR extras = ?)";
        r4 = new java.lang.String[r10];
        r4[r9] = r13;
        r0 = "1";
        r4[r8] = r0;
        r0 = "0";
        r4[r1] = r0;
        r0 = "2";
        r4[r5] = r0;
        goto L_0x0039;
    L_0x00a3:
        r0 = r9;
        goto L_0x004e;
    L_0x00a5:
        r7 = move-exception;
        r0 = "AppFreezerUtils";	 Catch:{ Exception -> 0x00a5, all -> 0x00c9 }
        r1 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x00a5, all -> 0x00c9 }
        r1.<init>();	 Catch:{ Exception -> 0x00a5, all -> 0x00c9 }
        r5 = "APP_FREEZER :";	 Catch:{ Exception -> 0x00a5, all -> 0x00c9 }
        r1 = r1.append(r5);	 Catch:{ Exception -> 0x00a5, all -> 0x00c9 }
        r5 = r7.toString();	 Catch:{ Exception -> 0x00a5, all -> 0x00c9 }
        r1 = r1.append(r5);	 Catch:{ Exception -> 0x00a5, all -> 0x00c9 }
        r1 = r1.toString();	 Catch:{ Exception -> 0x00a5, all -> 0x00c9 }
        android.util.Log.e(r0, r1);	 Catch:{ Exception -> 0x00a5, all -> 0x00c9 }
        if (r6 == 0) goto L_0x00c7;
    L_0x00c4:
        r6.close();
    L_0x00c7:
        r0 = r9;
        goto L_0x0053;
    L_0x00c9:
        r0 = move-exception;
        if (r6 == 0) goto L_0x00cf;
    L_0x00cc:
        r6.close();
    L_0x00cf:
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.util.AppFreezerUtils.canPutIntoSleepMode(android.content.Context, com.android.launcher3.common.compat.UserHandleCompat, java.lang.String):boolean");
    }

    public static boolean isInSleepMode(android.content.Context r10, com.android.launcher3.common.compat.UserHandleCompat r11, java.lang.String r12) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x0043 in list [B:12:0x0040]
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
        r5 = 3;
        r1 = 2;
        r8 = 1;
        r9 = 0;
        r2 = new java.lang.String[r8];
        r0 = "package_name";
        r2[r9] = r0;
        r0 = isSupportUID(r10);
        if (r0 == 0) goto L_0x0044;
    L_0x0010:
        r3 = "uid = ? AND package_name = ? AND isAppOptTarget = ? AND extras = ?";
        r0 = 4;
        r4 = new java.lang.String[r0];
        r0 = r11.hashCode();
        r0 = java.lang.Integer.toString(r0);
        r4[r9] = r0;
        r4[r8] = r12;
        r0 = "1";
        r4[r1] = r0;
        r0 = "1";
        r4[r5] = r0;
    L_0x0029:
        r6 = 0;
        r0 = r10.getContentResolver();	 Catch:{ Exception -> 0x0055, all -> 0x0079 }
        r1 = APP_FREEZER_URI;	 Catch:{ Exception -> 0x0055, all -> 0x0079 }
        r5 = 0;	 Catch:{ Exception -> 0x0055, all -> 0x0079 }
        r6 = r0.query(r1, r2, r3, r4, r5);	 Catch:{ Exception -> 0x0055, all -> 0x0079 }
        if (r6 == 0) goto L_0x0053;	 Catch:{ Exception -> 0x0055, all -> 0x0079 }
    L_0x0037:
        r0 = r6.getCount();	 Catch:{ Exception -> 0x0055, all -> 0x0079 }
        if (r0 != r8) goto L_0x0053;
    L_0x003d:
        r0 = r8;
    L_0x003e:
        if (r6 == 0) goto L_0x0043;
    L_0x0040:
        r6.close();
    L_0x0043:
        return r0;
    L_0x0044:
        r3 = "package_name = ? AND isAppOptTarget = ? AND extras = ?";
        r4 = new java.lang.String[r5];
        r4[r9] = r12;
        r0 = "1";
        r4[r8] = r0;
        r0 = "1";
        r4[r1] = r0;
        goto L_0x0029;
    L_0x0053:
        r0 = r9;
        goto L_0x003e;
    L_0x0055:
        r7 = move-exception;
        r0 = "AppFreezerUtils";	 Catch:{ Exception -> 0x0055, all -> 0x0079 }
        r1 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0055, all -> 0x0079 }
        r1.<init>();	 Catch:{ Exception -> 0x0055, all -> 0x0079 }
        r5 = "APP_FREEZER :";	 Catch:{ Exception -> 0x0055, all -> 0x0079 }
        r1 = r1.append(r5);	 Catch:{ Exception -> 0x0055, all -> 0x0079 }
        r5 = r7.toString();	 Catch:{ Exception -> 0x0055, all -> 0x0079 }
        r1 = r1.append(r5);	 Catch:{ Exception -> 0x0055, all -> 0x0079 }
        r1 = r1.toString();	 Catch:{ Exception -> 0x0055, all -> 0x0079 }
        android.util.Log.e(r0, r1);	 Catch:{ Exception -> 0x0055, all -> 0x0079 }
        if (r6 == 0) goto L_0x0077;
    L_0x0074:
        r6.close();
    L_0x0077:
        r0 = r9;
        goto L_0x0043;
    L_0x0079:
        r0 = move-exception;
        if (r6 == 0) goto L_0x007f;
    L_0x007c:
        r6.close();
    L_0x007f:
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.util.AppFreezerUtils.isInSleepMode(android.content.Context, com.android.launcher3.common.compat.UserHandleCompat, java.lang.String):boolean");
    }

    public static boolean isSupportUID(Context context) {
        return Utilities.getVersionCode(context, APP_FREEZER_PACKAGE) >= APP_FREEZER_UID_VERSION;
    }
}

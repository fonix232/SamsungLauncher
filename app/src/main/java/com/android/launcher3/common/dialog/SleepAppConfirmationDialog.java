package com.android.launcher3.common.dialog;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import com.android.launcher3.Launcher;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.sec.android.app.launcher.R;

public class SleepAppConfirmationDialog extends DialogFragment implements OnClickListener {
    private static final Uri SLEEP_APP_URI = Uri.parse("content://com.samsung.android.sm/AppFreezer");
    private static final String TAG = "SleepAppConfirmDialog";
    private static final String sFragmentTag = "SleepAppConfirm";
    private static String sPackageName;
    private static String sUserId;

    public void onClick(android.content.DialogInterface r14, int r15) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x00a3 in list []
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
        r12 = 1;
        r11 = 0;
        r0 = -1;
        if (r15 != r0) goto L_0x00a3;
    L_0x0005:
        r0 = sUserId;
        if (r0 == 0) goto L_0x00a3;
    L_0x0009:
        r0 = sPackageName;
        if (r0 == 0) goto L_0x00a3;
    L_0x000d:
        r0 = "SleepAppConfirmDialog";
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r5 = "Requesting sleep app... : ";
        r1 = r1.append(r5);
        r5 = sPackageName;
        r1 = r1.append(r5);
        r1 = r1.toString();
        android.util.Log.d(r0, r1);
        r2 = new java.lang.String[r12];
        r0 = "extras";
        r2[r11] = r0;
        r0 = r13.getActivity();
        r0 = com.android.launcher3.util.AppFreezerUtils.isSupportUID(r0);
        if (r0 == 0) goto L_0x00a4;
    L_0x0037:
        r3 = "uid = ? AND package_name = ?";
        r0 = 2;
        r4 = new java.lang.String[r0];
        r0 = sUserId;
        r4[r11] = r0;
        r0 = sPackageName;
        r4[r12] = r0;
    L_0x0044:
        r6 = 0;
        r0 = r13.getActivity();	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r0 = r0.getContentResolver();	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r1 = SLEEP_APP_URI;	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r5 = 0;	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r6 = r0.query(r1, r2, r3, r4, r5);	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        if (r6 == 0) goto L_0x00ad;	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
    L_0x0056:
        r0 = r6.moveToNext();	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        if (r0 == 0) goto L_0x00ad;	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
    L_0x005c:
        r10 = new android.content.ContentValues;	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r10.<init>();	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r0 = "extras";	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r1 = "1";	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r10.put(r0, r1);	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r0 = r13.getActivity();	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r0 = r0.getContentResolver();	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r1 = SLEEP_APP_URI;	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r0.update(r1, r10, r3, r4);	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r9 = new java.util.ArrayList;	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r9.<init>();	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r0 = sPackageName;	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r9.add(r0);	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r8 = new android.content.Intent;	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r0 = "com.samsung.android.server.am.ACTION_UI_TRIGGER_POLICY";	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r8.<init>(r0);	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r0 = "POLICY_NAME";	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r1 = "applocker";	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r8.putExtra(r0, r1);	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r0 = "PACKAGE_NAME";	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r8.putExtra(r0, r9);	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r0 = r13.getActivity();	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        r0.sendBroadcast(r8);	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        goto L_0x0056;
    L_0x009a:
        r7 = move-exception;
        r7.printStackTrace();	 Catch:{ Exception -> 0x009a, all -> 0x00b3 }
        if (r6 == 0) goto L_0x00a3;
    L_0x00a0:
        r6.close();
    L_0x00a3:
        return;
    L_0x00a4:
        r3 = "package_name = ?";
        r4 = new java.lang.String[r12];
        r0 = sPackageName;
        r4[r11] = r0;
        goto L_0x0044;
    L_0x00ad:
        if (r6 == 0) goto L_0x00a3;
    L_0x00af:
        r6.close();
        goto L_0x00a3;
    L_0x00b3:
        r0 = move-exception;
        if (r6 == 0) goto L_0x00b9;
    L_0x00b6:
        r6.close();
    L_0x00b9:
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.dialog.SleepAppConfirmationDialog.onClick(android.content.DialogInterface, int):void");
    }

    public static void createAndShow(Launcher launcher, UserHandleCompat user, String packageName) {
        sUserId = Integer.toString(user.hashCode());
        sPackageName = packageName;
        new SleepAppConfirmationDialog().show(launcher.getFragmentManager(), sFragmentTag);
    }

    public static boolean isActive(FragmentManager manager) {
        return manager.findFragmentByTag(sFragmentTag) != null;
    }

    public static void dismiss(FragmentTransaction ft, FragmentManager manager) {
        DialogFragment dialog = (DialogFragment) manager.findFragmentByTag(sFragmentTag);
        if (dialog != null) {
            dialog.dismiss();
            ft.remove(dialog);
        }
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Builder(getActivity()).setMessage(R.string.quick_option_sleep_alert).setPositiveButton(R.string.ok, this).setNegativeButton(R.string.cancel, this).create();
    }
}

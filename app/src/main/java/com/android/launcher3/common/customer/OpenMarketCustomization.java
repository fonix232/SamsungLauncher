package com.android.launcher3.common.customer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.model.DataLoader;
import com.android.launcher3.folder.FolderInfo;
import java.util.HashMap;

public class OpenMarketCustomization {
    private static final String AUTHORITY = "com.samsung.android.omcprovider/available_title_icon";
    private static final String OMC_AGENT_ACTIVITY_NAME = "com.samsung.android.app.omcagent.ui.application.AppInstallerActivity";
    private static final String OMC_AGENT_PACKAGE_NAME = "com.samsung.android.app.omcagent";
    private static final String OMC_COLS_ICON = "icon_drawable";
    private static final String OMC_COLS_PACKAGE = "package";
    private static final String OMC_COLS_STATE = "state";
    private static final String OMC_COLS_TITLE = "title";
    private static final String OMC_INTENT_ACTION = "com.samsung.intent.action.OMC_APP_DB_CHANGED";
    private static final String OMC_TARGET_ACTION = "com.samsung.omcagent.intent.action.OMC_APP_MANAGER";
    private static final Uri OMC_URI = Uri.parse("content://com.samsung.android.omcprovider/available_title_icon");
    private static final int STATE_REMOVED = 404;
    private static final String TAG = OpenMarketCustomization.class.getSimpleName();
    private Context mAppContext;
    private ItemChangedListener mAppsListener;
    private ItemChangedListener mHomeListener;
    private HashMap<String, IconTitleValue> mOmcAutoInstallApp;

    public static class IconTitleValue {
        public byte[] icon;
        public String iconPackage;
        public String title;

        public boolean isValid() {
            return (TextUtils.isEmpty(this.iconPackage) || TextUtils.isEmpty(this.title)) ? false : true;
        }
    }

    public interface ItemChangedListener {
        void onItemChanged(IconInfo iconInfo, ContentValues contentValues, boolean z);
    }

    class OMCUpdateReceiver extends BroadcastReceiver {
        OMCUpdateReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Log.i(OpenMarketCustomization.TAG, "OMCUpdateReceiver - onReceive");
            OpenMarketCustomization.this.refresh(context);
        }
    }

    private static class SingletonHolder {
        private static final OpenMarketCustomization sOMCInstance = new OpenMarketCustomization();

        private SingletonHolder() {
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void refresh(android.content.Context r23) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x000c in list [B:46:0x0135]
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
        r8 = com.android.launcher3.common.model.DataLoader.getItemList();
        if (r8 == 0) goto L_0x000c;
    L_0x0006:
        r2 = r8.size();
        if (r2 != 0) goto L_0x000d;
    L_0x000c:
        return;
    L_0x000d:
        r18 = new java.util.HashMap;
        r18.<init>();
        r17 = new java.util.ArrayList;
        r17.<init>();
        r2 = r8.iterator();
    L_0x001b:
        r3 = r2.hasNext();
        if (r3 == 0) goto L_0x0059;
    L_0x0021:
        r15 = r2.next();
        r15 = (com.android.launcher3.common.base.item.ItemInfo) r15;
        r3 = r15 instanceof com.android.launcher3.common.base.item.IconInfo;
        if (r3 == 0) goto L_0x001b;
    L_0x002b:
        r13 = r15;
        r13 = (com.android.launcher3.common.base.item.IconInfo) r13;
        r3 = r13.status;
        r3 = r3 & 32;
        if (r3 == 0) goto L_0x001b;
    L_0x0034:
        r3 = r13.componentName;
        if (r3 == 0) goto L_0x0050;
    L_0x0038:
        r9 = r13.componentName;
    L_0x003a:
        if (r9 == 0) goto L_0x001b;
    L_0x003c:
        r0 = r17;
        r0.add(r13);
        r3 = r9.getPackageName();
        r4 = 0;
        r4 = java.lang.Boolean.valueOf(r4);
        r0 = r18;
        r0.put(r3, r4);
        goto L_0x001b;
    L_0x0050:
        r3 = r13.getPromisedIntent();
        r9 = r3.getComponent();
        goto L_0x003a;
    L_0x0059:
        r2 = TAG;
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = "refresh - found omc items : ";
        r3 = r3.append(r4);
        r4 = r17.size();
        r3 = r3.append(r4);
        r3 = r3.toString();
        android.util.Log.d(r2, r3);
        r2 = r17.size();
        if (r2 == 0) goto L_0x000c;
    L_0x007b:
        r11 = 0;
        r2 = r23.getContentResolver();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r3 = OMC_URI;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = 0;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r5 = 0;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r6 = 0;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r7 = 0;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r11 = r2.query(r3, r4, r5, r6, r7);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        if (r11 == 0) goto L_0x022f;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x008c:
        r2 = r11.moveToNext();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        if (r2 == 0) goto L_0x022f;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x0092:
        r2 = "package";	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = r11.getColumnIndex(r2);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r19 = r11.getString(r2);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = android.text.TextUtils.isEmpty(r19);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        if (r2 != 0) goto L_0x008c;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x00a2:
        r2 = 1;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = java.lang.Boolean.valueOf(r2);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r0 = r18;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r1 = r19;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r0.put(r1, r2);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r3 = r17.iterator();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x00b2:
        r2 = r3.hasNext();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        if (r2 == 0) goto L_0x008c;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x00b8:
        r15 = r3.next();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r15 = (com.android.launcher3.common.base.item.IconInfo) r15;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = r15.componentName;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        if (r2 == 0) goto L_0x0140;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x00c2:
        r9 = r15.componentName;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x00c4:
        if (r9 == 0) goto L_0x00b2;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x00c6:
        r2 = r9.getPackageName();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r0 = r19;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = r0.endsWith(r2);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        if (r2 == 0) goto L_0x00b2;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x00d2:
        r2 = "state";	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = r11.getColumnIndex(r2);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r20 = r11.getInt(r2);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = 404; // 0x194 float:5.66E-43 double:1.996E-321;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r0 = r20;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        if (r0 != r2) goto L_0x014a;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x00e2:
        r4 = r15.container;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r0 = r22;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r16 = r0.getListenerByContainer(r4);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        if (r16 == 0) goto L_0x00b2;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x00ec:
        r2 = TAG;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4.<init>();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r5 = "omc item : ";	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r5 = r15.title;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r5 = " will be removed. - ";	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r6 = r15.container;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = r4.append(r6);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = r4.toString();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        android.util.Log.i(r2, r4);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = 0;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = 1;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r0 = r16;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r0.onItemChanged(r15, r2, r4);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        goto L_0x00b2;
    L_0x011a:
        r12 = move-exception;
        r2 = TAG;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r3 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r3.<init>();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = "Exception refresh omc title and icon : ";	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r3 = r3.append(r4);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r3 = r3.append(r12);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r3 = r3.toString();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        android.util.Log.d(r2, r3);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        if (r11 == 0) goto L_0x000c;
    L_0x0135:
        r2 = r11.isClosed();
        if (r2 != 0) goto L_0x000c;
    L_0x013b:
        r11.close();
        goto L_0x000c;
    L_0x0140:
        r2 = r15.getPromisedIntent();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r9 = r2.getComponent();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        goto L_0x00c4;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x014a:
        r2 = "title";	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = r11.getColumnIndex(r2);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r21 = r11.getString(r2);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = "icon_drawable";	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = r11.getColumnIndex(r2);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r13 = r11.getBlob(r2);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = TAG;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2.<init>();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r5 = "omc title : ";	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = r2.append(r5);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r0 = r21;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = r2.append(r0);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r5 = ", icon : ";	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r5 = r2.append(r5);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        if (r13 != 0) goto L_0x0210;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x0179:
        r2 = "null";	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x017b:
        r2 = r5.append(r2);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = r2.toString();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        android.util.Log.i(r4, r2);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        if (r15 == 0) goto L_0x00b2;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x0188:
        if (r21 == 0) goto L_0x0194;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x018a:
        r2 = r15.title;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r0 = r21;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = r0.equals(r2);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        if (r2 == 0) goto L_0x019d;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x0194:
        r2 = r15.usingFallbackIcon;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        if (r2 == 0) goto L_0x00b2;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x0198:
        if (r13 == 0) goto L_0x00b2;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x019a:
        r2 = r13.length;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        if (r2 <= 0) goto L_0x00b2;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x019d:
        r10 = new android.content.ContentValues;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r10.<init>();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = android.text.TextUtils.isEmpty(r21);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        if (r2 != 0) goto L_0x01b3;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x01a8:
        r2 = "title";	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r0 = r21;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r10.put(r2, r0);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r0 = r21;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r15.title = r0;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x01b3:
        if (r13 == 0) goto L_0x01cb;
    L_0x01b5:
        r2 = 0;
        r4 = r13.length;	 Catch:{ Exception -> 0x0214 }
        r2 = android.graphics.BitmapFactory.decodeByteArray(r13, r2, r4);	 Catch:{ Exception -> 0x0214 }
        r0 = r22;	 Catch:{ Exception -> 0x0214 }
        r4 = r0.mAppContext;	 Catch:{ Exception -> 0x0214 }
        r14 = com.android.launcher3.util.BitmapUtils.createIconBitmap(r2, r4);	 Catch:{ Exception -> 0x0214 }
        r15.setIcon(r14);	 Catch:{ Exception -> 0x0214 }
        r2 = "icon";	 Catch:{ Exception -> 0x0214 }
        r10.put(r2, r13);	 Catch:{ Exception -> 0x0214 }
    L_0x01cb:
        r4 = r15.container;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r0 = r22;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r16 = r0.getListenerByContainer(r4);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        if (r16 == 0) goto L_0x00b2;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x01d5:
        r2 = TAG;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4.<init>();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r5 = "omc item : ";	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r5 = r15.title;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r5 = " updated. - ";	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r6 = r15.container;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = r4.append(r6);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = r4.toString();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        android.util.Log.i(r2, r4);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = 0;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r0 = r16;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r0.onItemChanged(r15, r10, r2);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        goto L_0x00b2;
    L_0x0203:
        r2 = move-exception;
        if (r11 == 0) goto L_0x020f;
    L_0x0206:
        r3 = r11.isClosed();
        if (r3 != 0) goto L_0x020f;
    L_0x020c:
        r11.close();
    L_0x020f:
        throw r2;
    L_0x0210:
        r2 = "blob";	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        goto L_0x017b;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
    L_0x0214:
        r12 = move-exception;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r2 = TAG;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4.<init>();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r5 = "omc icon data error : ";	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r5 = r13.length;	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = r4.append(r5);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        r4 = r4.toString();	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        android.util.Log.e(r2, r4);	 Catch:{ Exception -> 0x011a, all -> 0x0203 }
        goto L_0x01cb;
    L_0x022f:
        if (r11 == 0) goto L_0x000c;
    L_0x0231:
        r2 = r11.isClosed();
        if (r2 != 0) goto L_0x000c;
    L_0x0237:
        r11.close();
        goto L_0x000c;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.customer.OpenMarketCustomization.refresh(android.content.Context):void");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void loadOmcIfNecessary(android.content.Context r12) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x0016 in list [B:34:0x0102]
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
        r11 = this;
        r0 = TAG;
        r1 = "loadOmcIfNecessary";
        android.util.Log.i(r0, r1);
        if (r12 == 0) goto L_0x000f;
    L_0x0009:
        r0 = r12.getContentResolver();
        if (r0 != 0) goto L_0x0017;
    L_0x000f:
        r0 = TAG;
        r1 = "Context or ContentResolver is null";
        android.util.Log.i(r0, r1);
    L_0x0016:
        return;
    L_0x0017:
        r0 = "device_policy";
        r7 = r12.getSystemService(r0);
        r7 = (android.app.admin.DevicePolicyManager) r7;
        if (r7 == 0) goto L_0x002f;
    L_0x0021:
        r0 = r7.semGetDeviceOwner();
        if (r0 == 0) goto L_0x002f;
    L_0x0027:
        r0 = TAG;
        r1 = "DeviceOnwerMode now.";
        android.util.Log.i(r0, r1);
        goto L_0x0016;
    L_0x002f:
        r0 = OMC_URI;
        if (r0 != 0) goto L_0x003b;
    L_0x0033:
        r0 = TAG;
        r1 = "loadOmcIfNecessary uri is null";
        android.util.Log.i(r0, r1);
        goto L_0x0016;
    L_0x003b:
        r0 = new java.util.HashMap;
        r0.<init>();
        r11.mOmcAutoInstallApp = r0;
        r6 = 0;
        r0 = r12.getContentResolver();	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1 = OMC_URI;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r2 = 0;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r3 = 0;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r4 = 0;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r5 = 0;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r6 = r0.query(r1, r2, r3, r4, r5);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        if (r6 == 0) goto L_0x0123;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
    L_0x0053:
        r0 = r6.moveToNext();	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        if (r0 == 0) goto L_0x0123;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
    L_0x0059:
        r9 = 0;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r0 = "state";	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r0 = r6.getColumnIndex(r0);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        if (r0 <= 0) goto L_0x006c;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
    L_0x0062:
        r0 = "state";	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r0 = r6.getColumnIndex(r0);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r9 = r6.getInt(r0);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
    L_0x006c:
        r0 = 404; // 0x194 float:5.66E-43 double:1.996E-321;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        if (r9 == r0) goto L_0x0053;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
    L_0x0070:
        r10 = new com.android.launcher3.common.customer.OpenMarketCustomization$IconTitleValue;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r10.<init>();	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r0 = "package";	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r0 = r6.getColumnIndex(r0);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r0 = r6.getString(r0);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r10.iconPackage = r0;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r0 = "title";	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r0 = r6.getColumnIndex(r0);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r0 = r6.getString(r0);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r10.title = r0;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r0 = "icon_drawable";	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r0 = r6.getColumnIndex(r0);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r0 = r6.getBlob(r0);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r10.icon = r0;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r0 = r10.isValid();	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        if (r0 == 0) goto L_0x010d;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
    L_0x009f:
        r0 = r11.mOmcAutoInstallApp;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1 = r10.iconPackage;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r0.put(r1, r10);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r0 = TAG;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1.<init>();	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r2 = "loadOmcIfNecessary insert package = ";	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1 = r1.append(r2);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r2 = r10.iconPackage;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1 = r1.append(r2);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1 = r1.toString();	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        android.util.Log.i(r0, r1);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r0 = TAG;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1.<init>();	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r2 = "loadOmcIfNecessary insert title = ";	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1 = r1.append(r2);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r2 = r10.title;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1 = r1.append(r2);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1 = r1.toString();	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        android.util.Log.i(r0, r1);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r0 = r10.icon;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        if (r0 != 0) goto L_0x0053;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
    L_0x00de:
        r0 = TAG;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1 = "loadOmcIfNecessary insert icon = null";	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        android.util.Log.i(r0, r1);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        goto L_0x0053;
    L_0x00e7:
        r8 = move-exception;
        r0 = TAG;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1.<init>();	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r2 = "Exception loading omc title and icon : ";	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1 = r1.append(r2);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1 = r1.append(r8);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1 = r1.toString();	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        android.util.Log.d(r0, r1);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        if (r6 == 0) goto L_0x0016;
    L_0x0102:
        r0 = r6.isClosed();
        if (r0 != 0) goto L_0x0016;
    L_0x0108:
        r6.close();
        goto L_0x0016;
    L_0x010d:
        r0 = TAG;	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        r1 = "loadOmcIfNecessary insert item fail.";	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        android.util.Log.i(r0, r1);	 Catch:{ Exception -> 0x00e7, all -> 0x0116 }
        goto L_0x0053;
    L_0x0116:
        r0 = move-exception;
        if (r6 == 0) goto L_0x0122;
    L_0x0119:
        r1 = r6.isClosed();
        if (r1 != 0) goto L_0x0122;
    L_0x011f:
        r6.close();
    L_0x0122:
        throw r0;
    L_0x0123:
        if (r6 == 0) goto L_0x0016;
    L_0x0125:
        r0 = r6.isClosed();
        if (r0 != 0) goto L_0x0016;
    L_0x012b:
        r6.close();
        goto L_0x0016;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.customer.OpenMarketCustomization.loadOmcIfNecessary(android.content.Context):void");
    }

    public static OpenMarketCustomization getInstance() {
        return SingletonHolder.sOMCInstance;
    }

    public static Intent getOmcIntent() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(OMC_AGENT_PACKAGE_NAME, OMC_AGENT_ACTIVITY_NAME));
        return intent;
    }

    public static Intent getOmcIntent(String packageName) {
        Intent intent = new Intent();
        intent.setAction(OMC_TARGET_ACTION);
        if (!(packageName == null || packageName.isEmpty())) {
            intent.putExtra(OMC_COLS_PACKAGE, packageName);
            Log.d(TAG, "Omc Intent created. : " + packageName);
        }
        return intent;
    }

    public void setup(Context context) {
        this.mAppContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction(OMC_INTENT_ACTION);
        this.mAppContext.registerReceiver(new OMCUpdateReceiver(), filter);
    }

    public void setListener(ItemChangedListener listener, boolean isApps) {
        if (isApps) {
            this.mAppsListener = listener;
        } else {
            this.mHomeListener = listener;
        }
    }

    public boolean hasPackage(String packageName) {
        return this.mOmcAutoInstallApp != null && this.mOmcAutoInstallApp.containsKey(packageName);
    }

    public IconTitleValue getIconInfo(String packageName) {
        if (this.mOmcAutoInstallApp == null) {
            return null;
        }
        return (IconTitleValue) this.mOmcAutoInstallApp.get(packageName);
    }

    private ItemChangedListener getListenerByContainer(long container) {
        if (container > 0) {
            FolderInfo fItem = DataLoader.getFolderInfo((int) container);
            if (fItem == null) {
                return null;
            }
            container = fItem.container;
        }
        if (container == -100) {
            return this.mHomeListener;
        }
        if (container == -102) {
            return this.mAppsListener;
        }
        return null;
    }
}

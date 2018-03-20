package com.android.launcher3.common.bnr;

import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherProvider;
import com.android.launcher3.common.bnr.LauncherBnrListener.Result;
import com.android.launcher3.common.bnr.extractor.LCExtractor;
import com.android.launcher3.common.bnr.scloud.SCloudBnr;
import com.android.launcher3.common.bnr.smartswitch.SmartSwitchBnrService;
import com.android.launcher3.common.compat.AppWidgetManagerCompat;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.customer.PostPositionSharedPref;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.model.LauncherSettings.Favorites_Easy;
import com.android.launcher3.common.model.LauncherSettings.Favorites_HomeApps;
import com.android.launcher3.common.model.LauncherSettings.Favorites_HomeOnly;
import com.android.launcher3.common.model.LauncherSettings.Favorites_Standard;
import com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens;
import com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens_Easy;
import com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens_HomeApps;
import com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens_HomeOnly;
import com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens_Standard;
import com.sec.android.app.launcher.R;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class LauncherBnrHelper {
    public static final int BNR_ERROR_CODE_INVALID_DATA = 3;
    public static final int BNR_ERROR_CODE_STORAGE_FULL = 2;
    public static final int BNR_ERROR_CODE_SUCCESS = 0;
    public static final int BNR_ERROR_CODE_UNKNOWN = 1;
    public static final int BNR_RESULT_FAIL = 1;
    public static final int BNR_RESULT_OK = 0;
    private static final String CHANGED_COMPONENT_LIST_XML = "/change_native_packages.xml";
    public static final String HOMESCREEN_BACKUP_EXML = "/homescreen.exml";
    private static final String TAG = "LauncherBnrHelper";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_WIDGET = "widget";
    private static ArrayList<LauncherBnrCallBack> sCallBack;
    private static HashMap<ComponentName, ComponentName> sChangedComponent = new HashMap();
    private static HashMap<ComponentName, ComponentName> sChangedWidgetComponent = new HashMap();
    private static LauncherBnrHelper sInstance;
    public static boolean sIsEasyMode = false;
    public static boolean sIsHomeOnly = false;
    private static boolean sUsePlayStore = false;
    private Result mBnrResult = new Result();
    private ArrayList<String> mRestoredTable = new ArrayList();

    public static synchronized LauncherBnrHelper getInstance() {
        LauncherBnrHelper launcherBnrHelper;
        synchronized (LauncherBnrHelper.class) {
            if (sInstance == null) {
                sInstance = new LauncherBnrHelper();
            }
            launcherBnrHelper = sInstance;
        }
        return launcherBnrHelper;
    }

    public static void registerBnrCallBack(ArrayList<LauncherBnrCallBack> callback) {
        sCallBack = callback;
    }

    public synchronized void backup(Context context, String path, String source, LauncherBnrListener listener) {
        Log.d(TAG, "backup source : " + source);
        Log.d(TAG, "backup path : " + path);
        this.mBnrResult.result = 0;
        this.mBnrResult.errorCode = 0;
        sIsHomeOnly = false;
        SharedPreferences prefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        if (LauncherFeature.supportHomeModeChange()) {
            if (!prefs.getBoolean(LauncherProvider.EMPTY_DATABASE_CREATED, false)) {
                sIsHomeOnly = prefs.getBoolean(LauncherAppState.HOME_ONLY_MODE, false);
            }
            Log.i(TAG, "backup sIsHomeOnly = " + sIsHomeOnly);
        }
        sIsEasyMode = false;
        if (LauncherFeature.supportEasyModeChange()) {
            sIsEasyMode = LauncherAppState.getInstance().isEasyModeEnabled();
        }
        File dir = new File(path);
        if (dir.exists()) {
            String[] fileList = dir.list();
            if (fileList != null) {
                Log.d(TAG, "dir fileList.length : " + fileList.length);
                for (String filename : fileList) {
                    if (!new File(path + '/' + filename).delete()) {
                        Log.e(TAG, "file : " + filename + ", delete failed");
                    }
                }
            }
        } else {
            dir.mkdirs();
        }
        if (sCallBack == null || sCallBack.isEmpty()) {
            Log.e(TAG, "sBackupCallBack is null or empty");
            this.mBnrResult.result = 1;
            this.mBnrResult.errorCode = 1;
            listener.backupComplete(this.mBnrResult, null);
        } else {
            File saveFile = new File(path + HOMESCREEN_BACKUP_EXML);
            if (!saveFile.exists()) {
                try {
                    saveFile.createNewFile();
                } catch (IOException e) {
                    Log.e(TAG, "backup IOException : " + e.toString());
                    this.mBnrResult.result = 1;
                    this.mBnrResult.errorCode = 1;
                    listener.backupComplete(this.mBnrResult, null);
                }
            }
            backupLayout(context, saveFile, source, listener);
            listener.backupComplete(this.mBnrResult, saveFile);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void backupLayout(android.content.Context r17, java.io.File r18, java.lang.String r19, com.android.launcher3.common.bnr.LauncherBnrListener r20) {
        /*
        r16 = this;
        r6 = 0;
        r9 = 0;
        r12 = new java.io.StringWriter;	 Catch:{ RuntimeException -> 0x018d, GeneralSecurityException -> 0x0192, IOException -> 0x0190, Exception -> 0x0144 }
        r12.<init>();	 Catch:{ RuntimeException -> 0x018d, GeneralSecurityException -> 0x0192, IOException -> 0x0190, Exception -> 0x0144 }
        r7 = new java.io.FileOutputStream;	 Catch:{ RuntimeException -> 0x018d, GeneralSecurityException -> 0x0192, IOException -> 0x0190, Exception -> 0x0144 }
        r0 = r18;
        r7.<init>(r0);	 Catch:{ RuntimeException -> 0x018d, GeneralSecurityException -> 0x0192, IOException -> 0x0190, Exception -> 0x0144 }
        r0 = r20;
        r9 = r0.getEncryptStream(r7);	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r11 = android.util.Xml.newSerializer();	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r11.setOutput(r12);	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        r13 = "UTF-8";
        r14 = 1;
        r14 = java.lang.Boolean.valueOf(r14);	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        r11.startDocument(r13, r14);	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        r0 = r16;
        r0.backupCategory(r11);	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        r0 = r16;
        r13 = r0.mBnrResult;	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        r13 = r13.result;	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        if (r13 != 0) goto L_0x0068;
    L_0x0032:
        r13 = sCallBack;	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        r13 = r13.iterator();	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
    L_0x0038:
        r14 = r13.hasNext();	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        if (r14 == 0) goto L_0x0058;
    L_0x003e:
        r3 = r13.next();	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        r3 = (com.android.launcher3.common.bnr.LauncherBnrCallBack) r3;	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        r0 = r16;
        r14 = r0.mBnrResult;	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        r0 = r17;
        r1 = r19;
        r3.backupLayout(r0, r11, r1, r14);	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        r0 = r16;
        r14 = r0.mBnrResult;	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        r14 = r14.result;	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        r15 = 1;
        if (r14 != r15) goto L_0x0038;
    L_0x0058:
        r0 = r16;
        r13 = r0.mBnrResult;	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        r13 = r13.result;	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        r14 = 1;
        if (r13 != r14) goto L_0x0068;
    L_0x0061:
        r0 = r16;
        r13 = r0.mBnrResult;	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        r14 = 3;
        r13.errorCode = r14;	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
    L_0x0068:
        r11.endDocument();	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
        r11.flush();	 Catch:{ RuntimeException -> 0x00d5, Exception -> 0x0106, GeneralSecurityException -> 0x0102, IOException -> 0x0133, all -> 0x0187 }
    L_0x006e:
        r0 = r16;
        r13 = r0.mBnrResult;	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r13 = r13.result;	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        if (r13 != 0) goto L_0x0138;
    L_0x0076:
        r8 = new java.io.ByteArrayInputStream;	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r13 = r12.toString();	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r14 = "UTF-8";
        r13 = r13.getBytes(r14);	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r8.<init>(r13);	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r2 = new java.io.BufferedInputStream;	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r2.<init>(r8);	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r13 = 1024; // 0x400 float:1.435E-42 double:5.06E-321;
        r4 = new byte[r13];	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
    L_0x008e:
        r13 = 0;
        r14 = 1024; // 0x400 float:1.435E-42 double:5.06E-321;
        r10 = r2.read(r4, r13, r14);	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r13 = -1;
        if (r10 == r13) goto L_0x0138;
    L_0x0098:
        r13 = 0;
        r9.write(r4, r13, r10);	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        goto L_0x008e;
    L_0x009d:
        r13 = move-exception;
        r6 = r7;
    L_0x009f:
        r5 = r13;
    L_0x00a0:
        r0 = r16;
        r13 = r0.mBnrResult;	 Catch:{ all -> 0x017b }
        r14 = 1;
        r13.result = r14;	 Catch:{ all -> 0x017b }
        r0 = r16;
        r13 = r0.mBnrResult;	 Catch:{ all -> 0x017b }
        r14 = 1;
        r13.errorCode = r14;	 Catch:{ all -> 0x017b }
        r13 = "LauncherBnrHelper";
        r14 = new java.lang.StringBuilder;	 Catch:{ all -> 0x017b }
        r14.<init>();	 Catch:{ all -> 0x017b }
        r15 = "bnr fail, occur exception : ";
        r14 = r14.append(r15);	 Catch:{ all -> 0x017b }
        r15 = r5.toString();	 Catch:{ all -> 0x017b }
        r14 = r14.append(r15);	 Catch:{ all -> 0x017b }
        r14 = r14.toString();	 Catch:{ all -> 0x017b }
        android.util.Log.e(r13, r14);	 Catch:{ all -> 0x017b }
        if (r9 == 0) goto L_0x00cf;
    L_0x00cc:
        close(r9);
    L_0x00cf:
        if (r6 == 0) goto L_0x00d4;
    L_0x00d1:
        close(r6);
    L_0x00d4:
        return;
    L_0x00d5:
        r5 = move-exception;
        r0 = r16;
        r13 = r0.mBnrResult;	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r14 = 1;
        r13.result = r14;	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r0 = r16;
        r13 = r0.mBnrResult;	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r14 = 1;
        r13.errorCode = r14;	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r13 = "LauncherBnrHelper";
        r14 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r14.<init>();	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r15 = "RuntimeException while generate XML : ";
        r14 = r14.append(r15);	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r15 = r5.toString();	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r14 = r14.append(r15);	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r14 = r14.toString();	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        android.util.Log.e(r13, r14);	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        goto L_0x006e;
    L_0x0102:
        r13 = move-exception;
        r6 = r7;
    L_0x0104:
        r5 = r13;
        goto L_0x00a0;
    L_0x0106:
        r5 = move-exception;
        r0 = r16;
        r13 = r0.mBnrResult;	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r14 = 1;
        r13.result = r14;	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r0 = r16;
        r13 = r0.mBnrResult;	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r14 = 1;
        r13.errorCode = r14;	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r13 = "LauncherBnrHelper";
        r14 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r14.<init>();	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r15 = "Error occurred while generate XML : ";
        r14 = r14.append(r15);	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r15 = r5.toString();	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r14 = r14.append(r15);	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        r14 = r14.toString();	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        android.util.Log.e(r13, r14);	 Catch:{ RuntimeException -> 0x009d, GeneralSecurityException -> 0x0102, IOException -> 0x0133, Exception -> 0x018a, all -> 0x0187 }
        goto L_0x006e;
    L_0x0133:
        r13 = move-exception;
        r6 = r7;
    L_0x0135:
        r5 = r13;
        goto L_0x00a0;
    L_0x0138:
        if (r9 == 0) goto L_0x013d;
    L_0x013a:
        close(r9);
    L_0x013d:
        if (r7 == 0) goto L_0x0195;
    L_0x013f:
        close(r7);
        r6 = r7;
        goto L_0x00d4;
    L_0x0144:
        r5 = move-exception;
    L_0x0145:
        r0 = r16;
        r13 = r0.mBnrResult;	 Catch:{ all -> 0x017b }
        r14 = 1;
        r13.result = r14;	 Catch:{ all -> 0x017b }
        r0 = r16;
        r13 = r0.mBnrResult;	 Catch:{ all -> 0x017b }
        r14 = 2;
        r13.errorCode = r14;	 Catch:{ all -> 0x017b }
        r13 = "LauncherBnrHelper";
        r14 = new java.lang.StringBuilder;	 Catch:{ all -> 0x017b }
        r14.<init>();	 Catch:{ all -> 0x017b }
        r15 = "bnr fail, occur exception : ";
        r14 = r14.append(r15);	 Catch:{ all -> 0x017b }
        r15 = r5.toString();	 Catch:{ all -> 0x017b }
        r14 = r14.append(r15);	 Catch:{ all -> 0x017b }
        r14 = r14.toString();	 Catch:{ all -> 0x017b }
        android.util.Log.e(r13, r14);	 Catch:{ all -> 0x017b }
        if (r9 == 0) goto L_0x0174;
    L_0x0171:
        close(r9);
    L_0x0174:
        if (r6 == 0) goto L_0x00d4;
    L_0x0176:
        close(r6);
        goto L_0x00d4;
    L_0x017b:
        r13 = move-exception;
    L_0x017c:
        if (r9 == 0) goto L_0x0181;
    L_0x017e:
        close(r9);
    L_0x0181:
        if (r6 == 0) goto L_0x0186;
    L_0x0183:
        close(r6);
    L_0x0186:
        throw r13;
    L_0x0187:
        r13 = move-exception;
        r6 = r7;
        goto L_0x017c;
    L_0x018a:
        r5 = move-exception;
        r6 = r7;
        goto L_0x0145;
    L_0x018d:
        r13 = move-exception;
        goto L_0x009f;
    L_0x0190:
        r13 = move-exception;
        goto L_0x0135;
    L_0x0192:
        r13 = move-exception;
        goto L_0x0104;
    L_0x0195:
        r6 = r7;
        goto L_0x00d4;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.bnr.LauncherBnrHelper.backupLayout(android.content.Context, java.io.File, java.lang.String, com.android.launcher3.common.bnr.LauncherBnrListener):void");
    }

    public synchronized void restore(Context context, String path, String source, int debugLevel, LauncherBnrListener listener, Bundle data) {
        Log.d(TAG, "restore source : " + source);
        Log.d(TAG, "restore path : " + path);
        this.mBnrResult.result = 0;
        this.mBnrResult.errorCode = 0;
        sUsePlayStore = false;
        if (data != null) {
            sUsePlayStore = data.getBoolean(SmartSwitchBnrService.RESTORE_USE_PLAYSTORE, false);
        }
        sIsHomeOnly = false;
        SharedPreferences prefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        if (LauncherFeature.supportHomeModeChange()) {
            if (!prefs.getBoolean(LauncherProvider.EMPTY_DATABASE_CREATED, false)) {
                sIsHomeOnly = prefs.getBoolean(LauncherAppState.HOME_ONLY_MODE, false);
            }
            Log.i(TAG, "restore sIsHomeOnly = " + sIsHomeOnly);
        }
        sIsEasyMode = false;
        if (LauncherFeature.supportEasyModeChange()) {
            sIsEasyMode = LauncherAppState.getInstance().isEasyModeEnabled();
        }
        File saveFile = new File(path + HOMESCREEN_BACKUP_EXML);
        if (!saveFile.exists() || sCallBack == null || sCallBack.isEmpty()) {
            this.mBnrResult.result = 1;
            this.mBnrResult.errorCode = 1;
            Log.e(TAG, "restore file not exist or sRestoreCallBack is null");
            listener.restoreComplete(this.mBnrResult, null);
        } else {
            restoreLayout(context, path, saveFile, debugLevel, listener);
            boolean needChangeMode = false;
            boolean toHomeOnly = false;
            if (this.mRestoredTable.size() == 0) {
                Log.d(TAG, "mRestoredTable size is 0");
                this.mBnrResult.result = 1;
                this.mBnrResult.errorCode = 3;
            } else if (LauncherFeature.supportHomeModeChange()) {
                if (sIsEasyMode) {
                    this.mRestoredTable.remove("favorites");
                } else {
                    this.mRestoredTable.remove(Favorites_Easy.TABLE_NAME);
                }
                if (!sIsEasyMode && this.mRestoredTable.size() == 1) {
                    String tableName = (String) this.mRestoredTable.get(0);
                    if (!"favorites".equals(tableName)) {
                        Log.d(TAG, "change mode (restored table count is 1)");
                        needChangeMode = true;
                        if (Favorites_HomeOnly.TABLE_NAME.equals(tableName)) {
                            toHomeOnly = true;
                        }
                    }
                } else if (this.mRestoredTable.size() == 2) {
                    String homeOnlyTableName = (String) this.mRestoredTable.get(1);
                    if (Favorites_HomeApps.TABLE_NAME.equals((String) this.mRestoredTable.get(0)) && !LauncherAppState.getInstance().isHomeOnlyModeEnabled(false)) {
                        Log.d(TAG, "change mode (restored table count is 2)");
                        needChangeMode = true;
                    } else if (Favorites_HomeOnly.TABLE_NAME.equals(homeOnlyTableName) && LauncherAppState.getInstance().isHomeOnlyModeEnabled(false)) {
                        Log.d(TAG, "change mode (restored table count is 2)");
                        toHomeOnly = true;
                        needChangeMode = true;
                    }
                }
            }
            Editor editor = context.getSharedPreferences(PostPositionSharedPref.PREFERENCES, 0).edit();
            if (editor != null) {
                editor.clear();
                editor.apply();
                Log.d(TAG, "post position shared pf deleted successfully");
            }
            listener.restoreComplete(this.mBnrResult, saveFile);
            if (this.mBnrResult.result == 0 && LauncherAppState.getLauncherProvider() != null && prefs.getBoolean(LauncherProvider.EMPTY_DATABASE_CREATED, false)) {
                LauncherAppState.getLauncherProvider().clearFlagEmptyDbCreated();
            }
            if (needChangeMode) {
                changeMode(context, toHomeOnly);
            } else {
                recreateLauncher(context);
            }
        }
    }

    private void restoreLayout(Context context, String path, File saveFile, int debugLevel, LauncherBnrListener listener) {
        InputStream fis;
        Exception e;
        Exception e2;
        InputStream inputStream = null;
        InputStream newIs = null;
        this.mRestoredTable.clear();
        loadChangedComponentFromRes(context);
        loadChangedComponentFromPath(context, path);
        if (debugLevel == 1004) {
            try {
                fis = new FileInputStream(saveFile);
            } catch (GeneralSecurityException e3) {
                e = e3;
            } catch (IOException e4) {
                e = e4;
            } catch (Exception e5) {
                e2 = e5;
            }
            try {
                newIs = listener.getDecryptStream(fis);
                if (newIs != null) {
                    makeDebugLayoutFile(path, fis, newIs);
                    if (newIs != null) {
                        close(newIs);
                    }
                    if (fis != null) {
                        close(fis);
                    }
                    inputStream = fis;
                    return;
                }
            } catch (GeneralSecurityException e6) {
                e = e6;
                inputStream = fis;
            } catch (IOException e7) {
                e = e7;
                inputStream = fis;
            } catch (Exception e8) {
                e2 = e8;
                inputStream = fis;
            } catch (Throwable th) {
                Throwable th2 = th;
                inputStream = fis;
            }
        } else {
            LauncherModel launcherModel = LauncherAppState.getInstance().getModel();
            if (launcherModel != null) {
                Log.d(TAG, "Stop loader before restore layout");
                launcherModel.stopLoader();
                launcherModel.setHasLoaderCompletedOnce(false);
            }
            Iterator it = sCallBack.iterator();
            fis = null;
            while (it.hasNext()) {
                LauncherBnrCallBack bnrCallBack = (LauncherBnrCallBack) it.next();
                inputStream = new FileInputStream(saveFile);
                newIs = listener.getDecryptStream(inputStream);
                if (newIs != null) {
                    XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                    parser.setInput(newIs, "utf-8");
                    bnrCallBack.restoreLayout(context, parser, this.mRestoredTable, this.mBnrResult);
                    close(newIs);
                }
                if (this.mBnrResult.result == 1) {
                    this.mBnrResult.errorCode = 3;
                }
                close(inputStream);
                fis = inputStream;
            }
        }
        inputStream = fis;
        if (newIs != null) {
            close(newIs);
        }
        if (inputStream != null) {
            close(inputStream);
            return;
        }
        return;
        e2 = e;
        try {
            this.mBnrResult.result = 1;
            this.mBnrResult.errorCode = 1;
            Log.e(TAG, "bnr fail, occur exception : " + e2);
            if (newIs != null) {
                close(newIs);
            }
            if (inputStream != null) {
                close(inputStream);
                return;
            }
            return;
        } catch (Throwable th3) {
            th2 = th3;
            if (newIs != null) {
                close(newIs);
            }
            if (inputStream != null) {
                close(inputStream);
            }
            throw th2;
        }
        e2 = e;
        this.mBnrResult.result = 1;
        this.mBnrResult.errorCode = 1;
        Log.e(TAG, "bnr fail, occur exception : " + e2);
        if (newIs != null) {
            close(newIs);
        }
        if (inputStream != null) {
            close(inputStream);
            return;
        }
        return;
        this.mBnrResult.result = 1;
        this.mBnrResult.errorCode = 2;
        Log.e(TAG, "bnr fail, occur exception : " + e2);
        if (newIs != null) {
            close(newIs);
        }
        if (inputStream != null) {
            close(inputStream);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean extractXML(android.content.Context r18, java.lang.String r19, java.lang.String r20) {
        /*
        r17 = this;
        r14 = 0;
        sIsHomeOnly = r14;
        sIsEasyMode = r14;
        r11 = new java.io.File;
        r14 = new java.lang.StringBuilder;
        r14.<init>();
        r15 = android.os.Environment.getExternalStorageDirectory();
        r14 = r14.append(r15);
        r15 = "/LCExtractor";
        r14 = r14.append(r15);
        r15 = 47;
        r14 = r14.append(r15);
        r0 = r19;
        r14 = r14.append(r0);
        r14 = r14.toString();
        r11.<init>(r14);
        r14 = r11.exists();
        if (r14 != 0) goto L_0x0036;
    L_0x0033:
        r11.createNewFile();	 Catch:{ IOException -> 0x00a7 }
    L_0x0036:
        r6 = 0;
        r9 = 0;
        r13 = new java.io.StringWriter;	 Catch:{ RuntimeException -> 0x016d, IOException -> 0x0131 }
        r13.<init>();	 Catch:{ RuntimeException -> 0x016d, IOException -> 0x0131 }
        r7 = new java.io.FileOutputStream;	 Catch:{ RuntimeException -> 0x016d, IOException -> 0x0131 }
        r7.<init>(r11);	 Catch:{ RuntimeException -> 0x016d, IOException -> 0x0131 }
        r9 = r7;
        r12 = android.util.Xml.newSerializer();	 Catch:{ RuntimeException -> 0x00fa, IOException -> 0x016a, all -> 0x0167 }
        r12.setOutput(r13);	 Catch:{ RuntimeException -> 0x007e, IOException -> 0x016a, all -> 0x0167 }
        r14 = "UTF-8";
        r15 = 1;
        r15 = java.lang.Boolean.valueOf(r15);	 Catch:{ RuntimeException -> 0x007e, IOException -> 0x016a, all -> 0x0167 }
        r12.startDocument(r14, r15);	 Catch:{ RuntimeException -> 0x007e, IOException -> 0x016a, all -> 0x0167 }
        r0 = r17;
        r0.addApacheLicense(r12);	 Catch:{ RuntimeException -> 0x007e, IOException -> 0x016a, all -> 0x0167 }
        r0 = r17;
        r1 = r20;
        r0.startLCExtractorTag(r12, r1);	 Catch:{ RuntimeException -> 0x007e, IOException -> 0x016a, all -> 0x0167 }
        r14 = sCallBack;	 Catch:{ RuntimeException -> 0x007e, IOException -> 0x016a, all -> 0x0167 }
        r14 = r14.iterator();	 Catch:{ RuntimeException -> 0x007e, IOException -> 0x016a, all -> 0x0167 }
    L_0x0066:
        r15 = r14.hasNext();	 Catch:{ RuntimeException -> 0x007e, IOException -> 0x016a, all -> 0x0167 }
        if (r15 == 0) goto L_0x00c6;
    L_0x006c:
        r3 = r14.next();	 Catch:{ RuntimeException -> 0x007e, IOException -> 0x016a, all -> 0x0167 }
        r3 = (com.android.launcher3.common.bnr.LauncherBnrCallBack) r3;	 Catch:{ RuntimeException -> 0x007e, IOException -> 0x016a, all -> 0x0167 }
        r0 = r17;
        r15 = r0.mBnrResult;	 Catch:{ RuntimeException -> 0x007e, IOException -> 0x016a, all -> 0x0167 }
        r0 = r18;
        r1 = r20;
        r3.backupLayout(r0, r12, r1, r15);	 Catch:{ RuntimeException -> 0x007e, IOException -> 0x016a, all -> 0x0167 }
        goto L_0x0066;
    L_0x007e:
        r5 = move-exception;
        r14 = "LauncherBnrHelper";
        r15 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x00fa, IOException -> 0x016a, all -> 0x0167 }
        r15.<init>();	 Catch:{ RuntimeException -> 0x00fa, IOException -> 0x016a, all -> 0x0167 }
        r16 = "RuntimeException while generate XML : ";
        r15 = r15.append(r16);	 Catch:{ RuntimeException -> 0x00fa, IOException -> 0x016a, all -> 0x0167 }
        r16 = r5.toString();	 Catch:{ RuntimeException -> 0x00fa, IOException -> 0x016a, all -> 0x0167 }
        r15 = r15.append(r16);	 Catch:{ RuntimeException -> 0x00fa, IOException -> 0x016a, all -> 0x0167 }
        r15 = r15.toString();	 Catch:{ RuntimeException -> 0x00fa, IOException -> 0x016a, all -> 0x0167 }
        android.util.Log.e(r14, r15);	 Catch:{ RuntimeException -> 0x00fa, IOException -> 0x016a, all -> 0x0167 }
        r14 = 0;
        if (r9 == 0) goto L_0x00a1;
    L_0x009e:
        close(r9);
    L_0x00a1:
        if (r7 == 0) goto L_0x00a6;
    L_0x00a3:
        close(r7);
    L_0x00a6:
        return r14;
    L_0x00a7:
        r5 = move-exception;
        r14 = "LauncherBnrHelper";
        r15 = new java.lang.StringBuilder;
        r15.<init>();
        r16 = "extractXML createNewFile IOException : ";
        r15 = r15.append(r16);
        r16 = r5.toString();
        r15 = r15.append(r16);
        r15 = r15.toString();
        android.util.Log.e(r14, r15);
        r14 = 0;
        goto L_0x00a6;
    L_0x00c6:
        r0 = r17;
        r1 = r20;
        r0.endLCExtractorTag(r12, r1);	 Catch:{ RuntimeException -> 0x007e, IOException -> 0x016a, all -> 0x0167 }
        r12.endDocument();	 Catch:{ RuntimeException -> 0x007e, IOException -> 0x016a, all -> 0x0167 }
        r12.flush();	 Catch:{ RuntimeException -> 0x007e, IOException -> 0x016a, all -> 0x0167 }
        r8 = new java.io.ByteArrayInputStream;	 Catch:{ RuntimeException -> 0x00fa, IOException -> 0x016a, all -> 0x0167 }
        r14 = r13.toString();	 Catch:{ RuntimeException -> 0x00fa, IOException -> 0x016a, all -> 0x0167 }
        r15 = "UTF-8";
        r14 = r14.getBytes(r15);	 Catch:{ RuntimeException -> 0x00fa, IOException -> 0x016a, all -> 0x0167 }
        r8.<init>(r14);	 Catch:{ RuntimeException -> 0x00fa, IOException -> 0x016a, all -> 0x0167 }
        r2 = new java.io.BufferedInputStream;	 Catch:{ RuntimeException -> 0x00fa, IOException -> 0x016a, all -> 0x0167 }
        r2.<init>(r8);	 Catch:{ RuntimeException -> 0x00fa, IOException -> 0x016a, all -> 0x0167 }
        r14 = 1024; // 0x400 float:1.435E-42 double:5.06E-321;
        r4 = new byte[r14];	 Catch:{ RuntimeException -> 0x00fa, IOException -> 0x016a, all -> 0x0167 }
    L_0x00eb:
        r14 = 0;
        r15 = 1024; // 0x400 float:1.435E-42 double:5.06E-321;
        r10 = r2.read(r4, r14, r15);	 Catch:{ RuntimeException -> 0x00fa, IOException -> 0x016a, all -> 0x0167 }
        r14 = -1;
        if (r10 == r14) goto L_0x0124;
    L_0x00f5:
        r14 = 0;
        r9.write(r4, r14, r10);	 Catch:{ RuntimeException -> 0x00fa, IOException -> 0x016a, all -> 0x0167 }
        goto L_0x00eb;
    L_0x00fa:
        r5 = move-exception;
        r6 = r7;
    L_0x00fc:
        r14 = "LauncherBnrHelper";
        r15 = new java.lang.StringBuilder;	 Catch:{ all -> 0x015b }
        r15.<init>();	 Catch:{ all -> 0x015b }
        r16 = "RuntimeException : ";
        r15 = r15.append(r16);	 Catch:{ all -> 0x015b }
        r16 = r5.toString();	 Catch:{ all -> 0x015b }
        r15 = r15.append(r16);	 Catch:{ all -> 0x015b }
        r15 = r15.toString();	 Catch:{ all -> 0x015b }
        android.util.Log.e(r14, r15);	 Catch:{ all -> 0x015b }
        r14 = 0;
        if (r9 == 0) goto L_0x011e;
    L_0x011b:
        close(r9);
    L_0x011e:
        if (r6 == 0) goto L_0x00a6;
    L_0x0120:
        close(r6);
        goto L_0x00a6;
    L_0x0124:
        if (r9 == 0) goto L_0x0129;
    L_0x0126:
        close(r9);
    L_0x0129:
        if (r7 == 0) goto L_0x012e;
    L_0x012b:
        close(r7);
    L_0x012e:
        r14 = 1;
        goto L_0x00a6;
    L_0x0131:
        r5 = move-exception;
    L_0x0132:
        r14 = "LauncherBnrHelper";
        r15 = new java.lang.StringBuilder;	 Catch:{ all -> 0x015b }
        r15.<init>();	 Catch:{ all -> 0x015b }
        r16 = "IOException : ";
        r15 = r15.append(r16);	 Catch:{ all -> 0x015b }
        r16 = r5.toString();	 Catch:{ all -> 0x015b }
        r15 = r15.append(r16);	 Catch:{ all -> 0x015b }
        r15 = r15.toString();	 Catch:{ all -> 0x015b }
        android.util.Log.e(r14, r15);	 Catch:{ all -> 0x015b }
        r14 = 0;
        if (r9 == 0) goto L_0x0154;
    L_0x0151:
        close(r9);
    L_0x0154:
        if (r6 == 0) goto L_0x00a6;
    L_0x0156:
        close(r6);
        goto L_0x00a6;
    L_0x015b:
        r14 = move-exception;
    L_0x015c:
        if (r9 == 0) goto L_0x0161;
    L_0x015e:
        close(r9);
    L_0x0161:
        if (r6 == 0) goto L_0x0166;
    L_0x0163:
        close(r6);
    L_0x0166:
        throw r14;
    L_0x0167:
        r14 = move-exception;
        r6 = r7;
        goto L_0x015c;
    L_0x016a:
        r5 = move-exception;
        r6 = r7;
        goto L_0x0132;
    L_0x016d:
        r5 = move-exception;
        goto L_0x00fc;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.bnr.LauncherBnrHelper.extractXML(android.content.Context, java.lang.String, java.lang.String):boolean");
    }

    private void backupCategory(XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.text("\n");
        serializer.startTag(null, "category");
        StringBuffer category = new StringBuffer();
        int count = sCallBack.size();
        for (int i = 0; i < count; i++) {
            String subCategory = ((LauncherBnrCallBack) sCallBack.get(i)).backupCategory();
            if (!(subCategory == null || subCategory.isEmpty())) {
                if (i > 0) {
                    category.append(',');
                }
                category.append(subCategory);
            }
        }
        String c = category.toString();
        serializer.text(c);
        serializer.endTag(null, "category");
        Log.i(TAG, "backupCategory category : " + c);
        if (c.isEmpty()) {
            this.mBnrResult.result = 1;
            this.mBnrResult.errorCode = 3;
            Log.i(TAG, "backupCategory category is empty");
        }
    }

    public static Uri getFavoritesUri(String container) {
        return getFavoritesUri(container, sIsHomeOnly);
    }

    public static Uri getFavoritesUri(String container, boolean isHomeOnly) {
        Uri favoritesUri = Favorites.CONTENT_URI;
        if (!container.contains(LauncherBnrTag.TAG_EASY)) {
            if (sIsEasyMode) {
                favoritesUri = Favorites_Standard.CONTENT_URI;
            }
            if (!container.contains(LauncherBnrTag.TAG_HOMEONLY) && isHomeOnly) {
                favoritesUri = Favorites_HomeApps.CONTENT_URI;
            } else if (container.contains(LauncherBnrTag.TAG_HOMEONLY) && !isHomeOnly) {
                favoritesUri = Favorites_HomeOnly.CONTENT_URI;
            }
            return favoritesUri;
        } else if (sIsEasyMode) {
            return favoritesUri;
        } else {
            return Favorites_Easy.CONTENT_URI;
        }
    }

    public static String getFavoritesTable(String container) {
        String tableName = "favorites";
        if (!container.contains(LauncherBnrTag.TAG_EASY)) {
            if (sIsEasyMode) {
                tableName = Favorites_Standard.TABLE_NAME;
            }
            if (!container.contains(LauncherBnrTag.TAG_HOMEONLY) && sIsHomeOnly) {
                tableName = Favorites_HomeApps.TABLE_NAME;
            } else if (container.contains(LauncherBnrTag.TAG_HOMEONLY) && !sIsHomeOnly) {
                tableName = Favorites_HomeOnly.TABLE_NAME;
            }
            return tableName;
        } else if (sIsEasyMode) {
            return tableName;
        } else {
            return Favorites_Easy.TABLE_NAME;
        }
    }

    public static Uri getWorkspaceScreenUri(String container, boolean isHomeOnly) {
        Uri workspaceScreenUri = WorkspaceScreens.CONTENT_URI;
        if (!container.contains(LauncherBnrTag.TAG_EASY)) {
            if (sIsEasyMode) {
                workspaceScreenUri = WorkspaceScreens_Standard.CONTENT_URI;
            }
            if (!container.contains(LauncherBnrTag.TAG_HOMEONLY) && isHomeOnly) {
                workspaceScreenUri = WorkspaceScreens_HomeApps.CONTENT_URI;
            } else if (container.contains(LauncherBnrTag.TAG_HOMEONLY) && !isHomeOnly) {
                workspaceScreenUri = WorkspaceScreens_HomeOnly.CONTENT_URI;
            }
            return workspaceScreenUri;
        } else if (sIsEasyMode) {
            return workspaceScreenUri;
        } else {
            return WorkspaceScreens_Easy.CONTENT_URI;
        }
    }

    public static String getWorkspaceScreenTable(String container) {
        String tableName = WorkspaceScreens.TABLE_NAME;
        if (!container.contains(LauncherBnrTag.TAG_EASY)) {
            if (sIsEasyMode) {
                tableName = WorkspaceScreens_Standard.TABLE_NAME;
            }
            if (!container.contains(LauncherBnrTag.TAG_HOMEONLY) && sIsHomeOnly) {
                tableName = WorkspaceScreens_HomeApps.TABLE_NAME;
            } else if (container.contains(LauncherBnrTag.TAG_HOMEONLY) && !sIsHomeOnly) {
                tableName = WorkspaceScreens_HomeOnly.TABLE_NAME;
            }
            return tableName;
        } else if (sIsEasyMode) {
            return tableName;
        } else {
            return WorkspaceScreens_Easy.TABLE_NAME;
        }
    }

    public static void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                Log.e(TAG, "close inputStream IOException : " + e.toString());
            }
        }
    }

    public static void close(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "close outputStream IOException : " + e.toString());
            }
        }
    }

    public static void deleteDir(String dirPath) {
        File dir = new File(dirPath);
        if (dir.exists()) {
            String[] fileList = dir.list();
            if (fileList != null) {
                Log.d(TAG, "deleteDir, fileList.length: " + fileList.length);
                for (String filename : fileList) {
                    if (!new File(dirPath + '/' + filename).delete()) {
                        Log.e(TAG, "file: " + filename + ", delete failed");
                    }
                }
            }
        }
        dir.delete();
    }

    public static ComponentName getChangedComponent(ComponentName cn) {
        return (ComponentName) sChangedComponent.get(cn);
    }

    public static ComponentName getChangedWidgetComponent(ComponentName cn) {
        return (ComponentName) sChangedWidgetComponent.get(cn);
    }

    public static ComponentName getComponent(Context context, int restored, String packageName, String className) {
        ComponentName cn = new ComponentName(packageName, className);
        try {
            PackageManager pkgMgr = context.getPackageManager();
            try {
                pkgMgr.getActivityInfo(cn, 0);
            } catch (NameNotFoundException e) {
                ComponentName cn2 = new ComponentName(pkgMgr.currentToCanonicalPackageNames(new String[]{packageName})[0], className);
                try {
                    pkgMgr.getActivityInfo(cn2, 0);
                    cn = cn2;
                } catch (NameNotFoundException e2) {
                    cn = cn2;
                    Log.i(TAG, "invalid componentName : " + cn);
                    if ((restored & 4) != 0) {
                    }
                    if (sChangedComponent.containsKey(cn)) {
                        return (ComponentName) sChangedComponent.get(cn);
                    }
                    return cn;
                }
            }
        } catch (NameNotFoundException e3) {
            Log.i(TAG, "invalid componentName : " + cn);
            if ((restored & 4) != 0 || SCloudBnr.isWillRestored(context, cn)) {
                if (sChangedComponent.containsKey(cn)) {
                    return (ComponentName) sChangedComponent.get(cn);
                }
                return cn;
            } else if (sChangedComponent.containsKey(cn)) {
                return (ComponentName) sChangedComponent.get(cn);
            } else {
                return null;
            }
        }
        return cn;
    }

    private void makeDebugLayoutFile(String path, FileInputStream fis, InputStream newFis) {
        Exception e;
        Throwable th;
        File saveFile = new File(path + "/homescreen_original.xml");
        OutputStream outputStream = null;
        try {
            if (!saveFile.exists()) {
                saveFile.createNewFile();
            }
            OutputStream newFos = new FileOutputStream(saveFile);
            while (true) {
                try {
                    int data = newFis.read();
                    if (data == -1) {
                        break;
                    }
                    newFos.write(data);
                } catch (Exception e2) {
                    e = e2;
                    outputStream = newFos;
                } catch (Throwable th2) {
                    th = th2;
                    outputStream = newFos;
                }
            }
            if (newFis != null) {
                close(newFis);
            }
            if (fis != null) {
                close((InputStream) fis);
            }
            if (newFos != null) {
                close(newFos);
                outputStream = newFos;
                return;
            }
        } catch (Exception e3) {
            e = e3;
            try {
                Log.e(TAG, "debug mode error: " + e);
                if (newFis != null) {
                    close(newFis);
                }
                if (fis != null) {
                    close((InputStream) fis);
                }
                if (outputStream != null) {
                    close(outputStream);
                }
            } catch (Throwable th3) {
                th = th3;
                if (newFis != null) {
                    close(newFis);
                }
                if (fis != null) {
                    close((InputStream) fis);
                }
                if (outputStream != null) {
                    close(outputStream);
                }
                throw th;
            }
        }
    }

    private void changeMode(Context context, boolean toHomeOnly) {
        Log.i(TAG, "changeMode toHomeOnly : " + toHomeOnly);
        FavoritesProvider favoritesProvider = FavoritesProvider.getInstance();
        if (favoritesProvider != null) {
            if (favoritesProvider.switchTable(sIsEasyMode ? 3 : 1, toHomeOnly)) {
                LauncherAppState.getInstance().writeHomeOnlyModeEnabled(toHomeOnly);
                recreateLauncher(context);
                return;
            }
            return;
        }
        Log.i(TAG, "FavoritesProvider instance is null");
    }

    public void recreateLauncher(Context context) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                LauncherAppState app = LauncherAppState.getInstanceNoCreate();
                if (app != null) {
                    LauncherModel model = app.getModel();
                    if (model != null) {
                        model.resetLoadedState(true, true);
                        if (model.getCallback() != null) {
                            if (app.getSettingsActivity() != null) {
                                app.getSettingsActivity().finish();
                            }
                            model.getCallback().recreateLauncher();
                            Log.i(LauncherBnrHelper.TAG, "Launcher recreate");
                            return;
                        }
                        Log.i(LauncherBnrHelper.TAG, "Launcher instance is null");
                        return;
                    }
                    return;
                }
                Log.i(LauncherBnrHelper.TAG, "LauncherAppState instance is null");
            }
        }, 100);
    }

    private void loadChangedComponentFromRes(Context context) {
        String[] cmpList;
        String[] key;
        if (sChangedComponent.isEmpty()) {
            cmpList = context.getResources().getStringArray(R.array.changed_component_list);
            if (cmpList != null && cmpList.length > 0) {
                for (String cmp : cmpList) {
                    key = cmp.split("\\|");
                    if (key.length == 2) {
                        key[0] = key[0].trim();
                        ComponentName before = ComponentName.unflattenFromString(key[0]);
                        key[1] = key[1].trim();
                        addChangedComponent(context, before, ComponentName.unflattenFromString(key[1]));
                    }
                }
            }
        }
        if (sChangedWidgetComponent.isEmpty()) {
            cmpList = context.getResources().getStringArray(R.array.changed_component_widget_list);
            if (cmpList != null && cmpList.length > 0) {
                List<AppWidgetProviderInfo> widgetsAll = AppWidgetManagerCompat.getInstance(context).getAllProviders();
                for (String cmp2 : cmpList) {
                    key = cmp2.split("\\|");
                    if (key.length == 2) {
                        key[0] = key[0].trim();
                        before = ComponentName.unflattenFromString(key[0]);
                        key[1] = key[1].trim();
                        addChangedWidgetComponent(widgetsAll, before, ComponentName.unflattenFromString(key[1]));
                    }
                }
            }
        }
    }

    private void addChangedComponent(Context context, ComponentName before, ComponentName after) {
        if (before != null && after != null) {
            LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
            List<LauncherActivityInfoCompat> beforeMatches = launcherApps.getActivityList(before.getPackageName(), UserHandleCompat.myUserHandle());
            List<LauncherActivityInfoCompat> afterMatches = launcherApps.getActivityList(after.getPackageName(), UserHandleCompat.myUserHandle());
            if (!afterMatches.isEmpty() && beforeMatches.isEmpty()) {
                sChangedComponent.put(before, after);
                Log.i(TAG, "addChangedComponent before = " + before + " after = " + after);
            }
            if (!beforeMatches.isEmpty() && afterMatches.isEmpty()) {
                sChangedComponent.put(after, before);
                Log.i(TAG, "addChangedComponent before = " + after + " after = " + before);
            }
        }
    }

    private void addChangedWidgetComponent(List<AppWidgetProviderInfo> widgetsAll, ComponentName before, ComponentName after) {
        if (before != null && after != null && widgetsAll != null) {
            for (AppWidgetProviderInfo info : widgetsAll) {
                if (info.provider.equals(after)) {
                    sChangedWidgetComponent.put(before, after);
                    Log.i(TAG, "addChangedWidgetComponent before = " + before + " after = " + after);
                    return;
                } else if (info.provider.equals(before)) {
                    sChangedWidgetComponent.put(after, before);
                    Log.i(TAG, "addChangedWidgetComponent before = " + after + " after = " + before);
                    return;
                }
            }
        }
    }

    private void loadChangedComponentFromPath(Context context, String path) {
        Exception e;
        Throwable th;
        Log.i(TAG, "loadChangedComponentFromPath path = " + path);
        File file = new File(path + CHANGED_COMPONENT_LIST_XML);
        if (file.exists()) {
            FileInputStream fileInputStream = null;
            try {
                FileInputStream fis = new FileInputStream(file);
                try {
                    XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                    parser.setInput(fis, "utf-8");
                    while (true) {
                        int type = parser.next();
                        if (type == 1) {
                            break;
                        } else if (type == 2) {
                            String name = parser.getName();
                            if (TAG_PACKAGE.equals(name)) {
                                loadChangedComponentForPackage(context, parser);
                            } else if (TAG_WIDGET.equals(name)) {
                                loadChangedComponentForWidget(context, parser);
                            }
                        }
                    }
                    if (fis != null) {
                        try {
                            fis.close();
                            fileInputStream = fis;
                            return;
                        } catch (Exception e2) {
                            Log.e(TAG, "loadChangedComponentFromPath exception = " + e2);
                            fileInputStream = fis;
                            return;
                        }
                    }
                    return;
                } catch (Exception e3) {
                    e2 = e3;
                    fileInputStream = fis;
                } catch (Throwable th2) {
                    th = th2;
                    fileInputStream = fis;
                }
            } catch (Exception e4) {
                e2 = e4;
                try {
                    Log.e(TAG, "loadChangedComponentFromPath exception = " + e2);
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                            return;
                        } catch (Exception e22) {
                            Log.e(TAG, "loadChangedComponentFromPath exception = " + e22);
                            return;
                        }
                    }
                    return;
                } catch (Throwable th3) {
                    th = th3;
                    if (fileInputStream != null) {
                        try {
                            fileInputStream.close();
                        } catch (Exception e222) {
                            Log.e(TAG, "loadChangedComponentFromPath exception = " + e222);
                        }
                    }
                    throw th;
                }
            }
        }
        Log.e(TAG, "loadChangedComponentFromPath there is no file");
    }

    private void loadChangedComponentForPackage(Context context, XmlPullParser parser) throws Exception {
        int depth = parser.getDepth();
        ArrayList<String> itemList = new ArrayList();
        while (true) {
            int type = parser.next();
            if ((type != 3 || parser.getDepth() > depth) && type != 1) {
                if (type == 2 && parser.next() == 4) {
                    itemList.add(parser.getText());
                }
            }
        }
        int size = itemList.size();
        Log.i(TAG, "loadChangedComponentForPackage item list size = " + size);
        for (int i = 0; i < size - 1; i++) {
            for (int j = i + 1; j < size; j++) {
                addChangedComponent(context, ComponentName.unflattenFromString((String) itemList.get(i)), ComponentName.unflattenFromString((String) itemList.get(j)));
            }
        }
    }

    private void loadChangedComponentForWidget(Context context, XmlPullParser parser) throws Exception {
        int depth = parser.getDepth();
        ArrayList<String> itemList = new ArrayList();
        while (true) {
            int type = parser.next();
            if ((type != 3 || parser.getDepth() > depth) && type != 1) {
                if (type == 2 && parser.next() == 4) {
                    itemList.add(parser.getText());
                }
            }
        }
        int size = itemList.size();
        Log.i(TAG, "loadChangedComponentForWidget item list size = " + size);
        List<AppWidgetProviderInfo> widgetsAll = AppWidgetManagerCompat.getInstance(context).getAllProviders();
        for (int i = 0; i < size - 1; i++) {
            for (int j = i + 1; j < size; j++) {
                addChangedWidgetComponent(widgetsAll, ComponentName.unflattenFromString((String) itemList.get(i)), ComponentName.unflattenFromString((String) itemList.get(j)));
            }
        }
    }

    public static String getUserSelectionArg(Context context) {
        return "profileId=" + UserManagerCompat.getInstance(context).getSerialNumberForUser(UserHandleCompat.myUserHandle());
    }

    private void addApacheLicense(XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.text("\n\n");
        serializer.comment("\nCopyright (C) 2016 The Android Open Source Project\nLicensed under the Apache License, Version 2.0 (the \"License\");\nyou may not use this file except in compliance with the License.\nYou may obtain a copy of the License at\n\n  http://www.apache.org/licenses/LICENSE-2.0\n\nUnless required by applicable law or agreed to in writing, software\ndistributed under the License is distributed on an \"AS IS\" BASIS,\nWITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\nSee the License for the specific language governing permissions and\nlimitations under the License.\n");
    }

    private void startLCExtractorTag(XmlSerializer serializer, String source) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.text("\n\n");
        if (LCExtractor.LCEXTRACTOR_HOME_SOURCE.equals(source)) {
            serializer.startTag(null, "favorites");
            serializer.attribute(null, "xmlns:launcher", "http://schemas.android.com/apk/res/com.sec.android.app.launcher");
            return;
        }
        serializer.startTag(null, "appOrder");
        serializer.attribute(null, "xmlns:launcher", "http://schemas.android.com/apk/res/com.sec.android.app.launcher");
    }

    private void endLCExtractorTag(XmlSerializer serializer, String source) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.text("\n\n");
        if (LCExtractor.LCEXTRACTOR_HOME_SOURCE.equals(source)) {
            serializer.endTag(null, "favorites");
        } else {
            serializer.endTag(null, "appOrder");
        }
    }

    public static boolean getUsePlayStore() {
        return sUsePlayStore;
    }
}

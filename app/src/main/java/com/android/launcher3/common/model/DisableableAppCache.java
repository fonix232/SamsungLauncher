package com.android.launcher3.common.model;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.SecureFolderHelper;
import com.samsung.android.feature.SemCscFeature;
import com.sec.android.app.launcher.R;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class DisableableAppCache {
    private static final Uri APP_POLICY_URI = Uri.parse("content://com.sec.knox.provider2/ApplicationPolicy");
    private static final String BLOCK_DISABLE_METADATA_NAME = "com.sec.android.app.blockdisabling";
    private static final String COLUMN_INSTALL_UNINSTALL_LIST = "getApplicationInstallUninstallList";
    private static final boolean DEBUG = false;
    private static final Object DISABLEABLE_APP_UPDATE_TOKEN = new Object();
    private static final String RIGHT_BRAIN = "Kr.co.rightbrain.RetailMode";
    private static final String[] SELECTION_ARGS = new String[]{UNINSTALL_BLACKLIST};
    private static final String[] SELECTION_ARGS_WHITELIST = new String[]{UNINSTALL_WHITELIST};
    private static final String TAG = "DisableableAppCache";
    private static final String UNBLOCK_DISABLE_METADATA_NAME = "com.sec.android.app.unblockdisabling";
    private static final String UNINSTALL_BLACKLIST = "UninstallationBlacklist";
    private static final String UNINSTALL_WHITELIST = "UninstallationWhitelist";
    public static ArrayList<String> mDisableBlockedItems = new ArrayList();
    public static ArrayList<String> mDisableableItems = new ArrayList();
    public static ArrayList<String> mUninstallBlockedItems = new ArrayList();
    private final Context mContext;
    private boolean mEDMBlockDisableAllList = false;
    private List<String> mEDMBlockDisableContainList = new ArrayList();
    private List<String> mEDMBlockDisablePackageList = new ArrayList();
    private List<String> mEDMBlockUninstallWhitelist = new ArrayList();
    private final PackageManager mPackageManager;
    private final Handler mWorkerHandler;

    class SerializedMakeListTask implements Runnable {
        private final Stack<ResolveInfo> mAppsToMakeList;

        SerializedMakeListTask(Stack<ResolveInfo> appsToMakeList) {
            this.mAppsToMakeList = appsToMakeList;
        }

        public void run() {
            if (!this.mAppsToMakeList.isEmpty()) {
                try {
                    ApplicationInfo info = DisableableAppCache.this.mPackageManager.getApplicationInfo(((ResolveInfo) this.mAppsToMakeList.pop()).activityInfo.packageName, 128);
                    Bundle md = info.metaData;
                    boolean unblock = false;
                    boolean block = false;
                    if (!(md == null || info.packageName == null)) {
                        unblock = md.getBoolean(DisableableAppCache.UNBLOCK_DISABLE_METADATA_NAME);
                        block = md.getBoolean(DisableableAppCache.BLOCK_DISABLE_METADATA_NAME);
                    }
                    if (!DisableableAppCache.mDisableBlockedItems.contains(info.packageName)) {
                        if (block) {
                            DisableableAppCache.mDisableBlockedItems.add(info.packageName);
                        } else if (unblock) {
                            if (!DisableableAppCache.mDisableableItems.contains(info.packageName)) {
                                DisableableAppCache.mDisableableItems.add(info.packageName);
                            }
                        } else if (Utilities.ATLEAST_N_MR1 && SecureFolderHelper.SECURE_FOLDER_PACKAGE_NAME.equals(info.packageName) && !SecureFolderHelper.isSecureFolderExist(DisableableAppCache.this.mContext)) {
                            if (!DisableableAppCache.mDisableableItems.contains(info.packageName)) {
                                DisableableAppCache.mDisableableItems.add(info.packageName);
                            }
                        } else if (!(DisableableAppCache.this.isSignedBySystemSignature(info.packageName) || DisableableAppCache.mDisableableItems.contains(info.packageName))) {
                            DisableableAppCache.mDisableableItems.add(info.packageName);
                        }
                    }
                } catch (Exception e) {
                    Log.e(DisableableAppCache.TAG, "Exception while making list from Meta : " + e.toString());
                }
                scheduleNext();
            }
        }

        public void scheduleNext() {
            DisableableAppCache.this.mWorkerHandler.postAtTime(this, DisableableAppCache.DISABLEABLE_APP_UPDATE_TOKEN, SystemClock.uptimeMillis() + 1);
        }
    }

    public DisableableAppCache(Context context) {
        this.mContext = context;
        this.mPackageManager = context.getPackageManager();
        this.mWorkerHandler = new Handler(LauncherModel.getWorkerLooper());
    }

    public void makeDisableableAppList() {
        getEnterprisePolicyBlockUninstallList();
        getEnterprisePolicyBlockUninstallWhitelist();
        makeDisableableAppListFromMeta();
        addNonDisableAppToListFromXML();
        addNonDisableAppToListFromCSC();
        addDisableAndUninstallBlockedAppToListFromSettingsCSC();
        getHomePackageList();
    }

    public boolean isBlockUninstall(String packageName) {
        return isEnterprisePolicyBlockUninstall(packageName);
    }

    private synchronized boolean isEnterprisePolicyBlockUninstall(String pkgName) {
        boolean z = true;
        synchronized (this) {
            if (pkgName != null) {
                if (this.mEDMBlockDisableAllList) {
                    for (String list : new ArrayList(this.mEDMBlockUninstallWhitelist)) {
                        if (pkgName.startsWith(list)) {
                            z = false;
                            break;
                        }
                    }
                }
                List<String> edmBlockDisableContainList = new ArrayList(this.mEDMBlockDisableContainList);
                List<String> edmBlockDisablePackageList = new ArrayList(this.mEDMBlockDisablePackageList);
                for (String list2 : edmBlockDisableContainList) {
                    if (pkgName.startsWith(list2)) {
                        break;
                    }
                }
                for (String list22 : edmBlockDisablePackageList) {
                    if (pkgName.equals(list22)) {
                        break;
                    }
                }
                z = false;
            }
        }
        return z;
    }

    public synchronized void getEnterprisePolicyBlockUninstallList() {
        this.mEDMBlockDisableAllList = false;
        this.mEDMBlockDisableContainList.clear();
        this.mEDMBlockDisablePackageList.clear();
        Cursor c = null;
        c = this.mContext.getContentResolver().query(APP_POLICY_URI, null, COLUMN_INSTALL_UNINSTALL_LIST, SELECTION_ARGS, null);
        if (c != null) {
            c.moveToFirst();
            do {
                String pkgName = c.getString(c.getColumnIndex(COLUMN_INSTALL_UNINSTALL_LIST));
                int index = pkgName.indexOf(".*");
                if (index > -1) {
                    while (index > 0) {
                        pkgName = pkgName.substring(0, index);
                        index = pkgName.indexOf(".*");
                    }
                    if (index == 0) {
                        this.mEDMBlockDisableAllList = true;
                    } else {
                        try {
                            this.mEDMBlockDisableContainList.add(pkgName);
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (c != null) {
                                c.close();
                            }
                        } catch (Throwable th) {
                            if (c != null) {
                                c.close();
                            }
                        }
                    }
                } else {
                    this.mEDMBlockDisablePackageList.add(pkgName);
                }
            } while (c.moveToNext());
        } else {
            Log.w(TAG, "getEnterprisePolicyBlockUninstallList() : Cursor is null!");
        }
        if (c != null) {
            c.close();
        }
        this.mEDMBlockDisableContainList.add(RIGHT_BRAIN);
    }

    public synchronized void getEnterprisePolicyBlockUninstallWhitelist() {
        this.mEDMBlockUninstallWhitelist.clear();
        Cursor c = null;
        try {
            c = this.mContext.getContentResolver().query(APP_POLICY_URI, null, COLUMN_INSTALL_UNINSTALL_LIST, SELECTION_ARGS_WHITELIST, null);
            if (c != null) {
                c.moveToFirst();
                do {
                    this.mEDMBlockUninstallWhitelist.add(c.getString(c.getColumnIndex(COLUMN_INSTALL_UNINSTALL_LIST)));
                } while (c.moveToNext());
            } else {
                Log.w(TAG, "getEnterprisePolicyBlockUninstallWhitelist() : Cursor is null!");
            }
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            if (c != null) {
                c.close();
            }
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
        }
    }

    private void addNonDisableAppToListFromXML() {
        Exception e;
        String pkgName;
        XmlResourceParser parser = this.mContext.getResources().getXml(R.xml.default_disableapp_skiplist);
        if (parser != null) {
            int eventType;
            try {
                DefaultLayoutParser.beginDocument(parser, "nondisableapps");
            } catch (IOException e2) {
                e = e2;
                e.printStackTrace();
                for (eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                    if (eventType == 4) {
                        pkgName = parser.getText();
                        if (!mDisableBlockedItems.contains(pkgName)) {
                            mDisableBlockedItems.add(pkgName);
                        }
                    }
                }
                return;
            } catch (XmlPullParserException e3) {
                e = e3;
                e.printStackTrace();
                for (eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                    if (eventType == 4) {
                        pkgName = parser.getText();
                        if (mDisableBlockedItems.contains(pkgName)) {
                            mDisableBlockedItems.add(pkgName);
                        }
                    }
                }
                return;
            }
            try {
                for (eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                    if (eventType == 4) {
                        pkgName = parser.getText();
                        if (mDisableBlockedItems.contains(pkgName)) {
                            mDisableBlockedItems.add(pkgName);
                        }
                    }
                }
                return;
            } catch (IOException e4) {
                e = e4;
                e.printStackTrace();
                return;
            } catch (XmlPullParserException e5) {
                e = e5;
                e.printStackTrace();
                return;
            }
        }
        Log.e(TAG, "addNonDisableAppToListFromXML() : Parser is null!");
    }

    private void addNonDisableAppToListFromCSC() {
        Exception e;
        Exception e2;
        Throwable th;
        FileReader cscFile = null;
        try {
            File cscFileChk = new File("/system/csc/default_disableapp_skiplist.xml");
            if (cscFileChk.isFile() && cscFileChk.length() > 0) {
                FileReader cscFile2 = new FileReader("/system/csc/default_disableapp_skiplist.xml");
                try {
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    factory.setNamespaceAware(true);
                    XmlPullParser parser = factory.newPullParser();
                    parser.setInput(cscFile2);
                    for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                        if (eventType == 4) {
                            String pkgName = parser.getText();
                            if (!mDisableBlockedItems.contains(pkgName)) {
                                mDisableBlockedItems.add(pkgName);
                            }
                        }
                    }
                    cscFile = cscFile2;
                } catch (IOException e3) {
                    e = e3;
                    cscFile = cscFile2;
                    e2 = e;
                    try {
                        e2.printStackTrace();
                        if (cscFile == null) {
                            try {
                                cscFile.close();
                            } catch (Exception e22) {
                                e22.printStackTrace();
                                return;
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        if (cscFile != null) {
                            try {
                                cscFile.close();
                            } catch (Exception e222) {
                                e222.printStackTrace();
                            }
                        }
                        throw th;
                    }
                } catch (XmlPullParserException e4) {
                    e = e4;
                    cscFile = cscFile2;
                    e222 = e;
                    e222.printStackTrace();
                    if (cscFile == null) {
                        cscFile.close();
                    }
                } catch (Throwable th3) {
                    th = th3;
                    cscFile = cscFile2;
                    if (cscFile != null) {
                        cscFile.close();
                    }
                    throw th;
                }
            }
            if (cscFile != null) {
                try {
                    cscFile.close();
                } catch (Exception e2222) {
                    e2222.printStackTrace();
                }
            }
        } catch (IOException e5) {
            e = e5;
            e2222 = e;
            e2222.printStackTrace();
            if (cscFile == null) {
                cscFile.close();
            }
        } catch (XmlPullParserException e6) {
            e = e6;
            e2222 = e;
            e2222.printStackTrace();
            if (cscFile == null) {
                cscFile.close();
            }
        }
    }

    private void addDisableAndUninstallBlockedAppToListFromSettingsCSC() {
        if (!SemCscFeature.getInstance().getString("CscFeature_Setting_ConfigForbidAppDisableButton").isEmpty()) {
            for (String pkgName : SemCscFeature.getInstance().getString("CscFeature_Setting_ConfigForbidAppDisableButton").split(",")) {
                if (!mDisableBlockedItems.contains(pkgName)) {
                    mDisableBlockedItems.add(pkgName);
                }
                if (!mUninstallBlockedItems.contains(pkgName)) {
                    mUninstallBlockedItems.add(pkgName);
                }
            }
        }
    }

    private void getHomePackageList() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        for (ResolveInfo resolveInfo : this.mPackageManager.queryIntentActivities(intent, 65600)) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo.applicationInfo.enabled && (activityInfo.applicationInfo.flags & 1) != 0) {
                String activityPkg = activityInfo.packageName;
                mDisableBlockedItems.add(activityPkg);
                Bundle metadata = activityInfo.metaData;
                if (metadata != null) {
                    String metaPkg = metadata.getString("android.app.home.alternate");
                    if (signaturesMatch(metaPkg, activityPkg)) {
                        mDisableBlockedItems.add(metaPkg);
                    }
                }
            }
        }
    }

    private boolean signaturesMatch(String pkg1, String pkg2) {
        if (pkg1 == null || pkg2 == null) {
            return false;
        }
        try {
            if (this.mPackageManager.checkSignatures(pkg1, pkg2) >= 0) {
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void makeDisableableAppListFromMeta() {
        Intent mainIntent = new Intent("android.intent.action.MAIN", null);
        mainIntent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> appList = this.mPackageManager.queryIntentActivities(mainIntent, 131584);
        if (!appList.isEmpty()) {
            Stack<ResolveInfo> appsToMakeList = new Stack();
            appsToMakeList.addAll(appList);
            new SerializedMakeListTask(appsToMakeList).scheduleNext();
        }
    }

    public void updateForPkg(String packageName) {
        if (!mDisableableItems.contains(packageName) && !mDisableBlockedItems.contains(packageName) && !mUninstallBlockedItems.contains(packageName) && !isSignedBySystemSignature(packageName)) {
            mDisableableItems.add(packageName);
        }
    }

    private boolean isSignedBySystemSignature(String packageName) {
        boolean z = false;
        try {
            z = this.mPackageManager.getPackageInfo("android", 64).signatures[0].equals(this.mPackageManager.getPackageInfo(packageName, 64).signatures[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return z;
    }
}

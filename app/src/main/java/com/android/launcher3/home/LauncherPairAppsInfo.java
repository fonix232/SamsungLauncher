package com.android.launcher3.home;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.util.DualAppUtils;
import com.android.launcher3.util.PairAppsUtilities;
import com.samsung.android.app.SemDualAppManager;

public class LauncherPairAppsInfo extends IconInfo {
    private static final String TAG = "LauncherPairAppsInfo";
    public PairAppInfo mFirstApp;
    public PairAppInfo mSecondApp;

    public static class PairAppInfo {
        private ComponentName mCN;
        private Intent mIntent;
        private UserHandleCompat mUser;

        PairAppInfo(Intent intent, ComponentName CN, UserHandleCompat user) {
            this.mIntent = intent;
            this.mCN = CN;
            this.mUser = user;
        }

        public Intent getIntent() {
            return this.mIntent;
        }

        public ComponentName getCN() {
            return this.mCN;
        }

        public UserHandleCompat getUserCompat() {
            return this.mUser;
        }
    }

    LauncherPairAppsInfo(Intent firstIntent, Intent secondIntent, ComponentName firstCN, ComponentName secondCN, UserHandleCompat firstUser, UserHandleCompat secondUser) {
        this.itemType = 7;
        this.container = -100;
        this.mFirstApp = new PairAppInfo(firstIntent, firstCN, firstUser);
        this.mSecondApp = new PairAppInfo(secondIntent, secondCN, secondUser);
        this.user = UserHandleCompat.myUserHandle();
    }

    LauncherPairAppsInfo(Context context, String info) {
        this.itemType = 7;
        this.container = -100;
        setPairAppInfo(context, info);
        setIcon(PairAppsUtilities.buildIcon(context, this.mFirstApp, this.mSecondApp));
        this.title = PairAppsUtilities.buildLabel(context, this.mFirstApp.getCN(), this.mSecondApp.getCN());
    }

    private void setPairAppInfo(Context context, String info) {
        UserManagerCompat userManager = UserManagerCompat.getInstance(context);
        Intent intent = null;
        ComponentName CN = null;
        UserHandleCompat user = null;
        if (info != null) {
            String[] appInfo = info.split(";");
            if (appInfo.length == 2) {
                for (int index = 0; index < 2; index++) {
                    String[] items = appInfo[index].split(":");
                    if (items.length == 2) {
                        CN = ComponentName.unflattenFromString(items[0]);
                        if (CN == null) {
                            Log.d(TAG, "Pair apps error!!");
                            return;
                        } else if (Utilities.isValidComponent(context, CN)) {
                            String pkgName = CN.getPackageName();
                            int userId = Integer.parseInt(items[1]);
                            if (SemDualAppManager.isInstalledWhitelistedPackage(pkgName) && DualAppUtils.isDualApp(userManager.getUserForSerialNumber((long) userId), pkgName)) {
                                int profileId = SemDualAppManager.getDualAppProfileId();
                                if (profileId != -10000) {
                                    intent = new Intent("android.intent.action.MAIN");
                                    intent.addCategory("android.intent.category.LAUNCHER");
                                    intent.setComponent(CN);
                                    user = userManager.getUserForSerialNumber((long) profileId);
                                }
                            } else {
                                intent = new Intent("android.intent.action.MAIN");
                                intent.addCategory("android.intent.category.LAUNCHER");
                                intent.setComponent(CN);
                                user = userManager.getUserForSerialNumber((long) userId);
                            }
                        }
                    }
                    if (index == 0) {
                        this.mFirstApp = new PairAppInfo(intent, CN, user);
                    } else {
                        this.mSecondApp = new PairAppInfo(intent, CN, user);
                    }
                }
            }
        }
    }

    public void onAddToDatabase(Context context, ContentValues values) {
        super.onAddToDatabase(context, values);
        if (this.mFirstApp.getCN() == null || this.mSecondApp.getCN() == null) {
            Log.d(TAG, "PairApps not add to Database!!");
            return;
        }
        String firstCN = this.mFirstApp.getCN().flattenToShortString() + ':' + this.mFirstApp.getUserCompat().getUser().semGetIdentifier();
        values.put("intent", firstCN + ';' + (this.mSecondApp.getCN().flattenToShortString() + ':' + this.mSecondApp.getUserCompat().getUser().semGetIdentifier()));
    }
}

package com.android.launcher3.home;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.Toast;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.util.DualAppUtils;
import com.android.launcher3.util.PairAppsUtilities;
import com.android.launcher3.util.logging.SALogging;
//import com.samsung.android.app.SemDualAppManager;
import com.sec.android.app.launcher.R;
import java.net.URISyntaxException;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

public class InstallPairAppsReceiver extends BroadcastReceiver {
    private static final String ACTION_ADD_PAIR_APP_SHORTCUT = "com.samsung.android.multiwindow.ADD_PAIR_APP_SHORTCUT";
    private static final String EXTRA_FIRST_COMPONENT_KEY = "component.first";
    private static final String EXTRA_FIRST_USER_HANDLE_KEY = "user.first.handle";
    private static final String EXTRA_PAIR_APP_COMPONENT_NAME_FIRST = "component_first";
    private static final String EXTRA_PAIR_APP_COMPONENT_NAME_SECOND = "component_second";
    private static final String EXTRA_PAIR_APP_USERID_FIRST = "userId_first";
    private static final String EXTRA_PAIR_APP_USERID_SECOND = "userId_second";
    private static final String EXTRA_SECOND_COMPONENT_KEY = "component.second";
    private static final String EXTRA_SECOND_USER_HANDLE_KEY = "user.second.handle";
    private static final String ICON_KEY = "icon";
    private static final String LAUNCH_FIRST_INTENT_KEY = "intent.first.launch";
    private static final String LAUNCH_SECOND_INTENT_KEY = "intent.second.launch";
    private static final String PAIR_APPS_SHORTCUT_TYPE_KEY = "isPairAppsShortcut";
    private static final String TAG = "InstallPairAppsReceiver";
    ComponentName mFirstCN;
    Intent mFirstIntent = null;
    UserHandleCompat mFirstUser = null;
    ComponentName mSecondCN;
    Intent mSecondIntent = null;
    UserHandleCompat mSecondUser = null;

    private static class PendingInstallPairAppsInfo extends ExternalRequestInfo {
        private static final int PAIR_APPS_FIRST_ITEM_POS = 0;
        private static final int PAIR_APPS_SECOND_ITEM_POS = 1;
        final LauncherActivityInfoCompat activityInfo = null;
        final Context mContext;
        final Intent mData;
        final Intent mFirst;
        final ComponentName mFirstCN;
        final UserHandleCompat mFirstUser;
        Bitmap mIcon = null;
        String mLabel = "";
        final Intent mSecond;
        final ComponentName mSecondCN;
        final UserHandleCompat mSecondUser;

        PendingInstallPairAppsInfo(Intent data, Context context, long time, Intent first, Intent second, ComponentName firstCN, ComponentName secondCN, UserHandleCompat firstUser, UserHandleCompat secondUser) {
            super(5, UserHandleCompat.myUserHandle(), time);
            this.mContext = context;
            this.mData = data;
            this.mFirst = first;
            this.mSecond = second;
            this.mFirstCN = firstCN;
            this.mSecondCN = secondCN;
            this.mFirstUser = firstUser;
            this.mSecondUser = secondUser;
            this.mLabel = PairAppsUtilities.buildLabel(this.mContext, this.mFirstCN, this.mSecondCN);
            this.mIcon = PairAppsUtilities.buildIcon(this.mContext, this.mFirstCN, this.mSecondCN, this.mFirstUser, this.mSecondUser);
        }

        String encodeToString() {
            try {
                JSONStringer json = new JSONStringer().object().key("type").value(5).key("time").value(this.requestTime).key(InstallPairAppsReceiver.LAUNCH_FIRST_INTENT_KEY).value(this.mFirst.toUri(0)).key(InstallPairAppsReceiver.LAUNCH_SECOND_INTENT_KEY).value(this.mSecond.toUri(0)).key(InstallPairAppsReceiver.EXTRA_FIRST_USER_HANDLE_KEY).value(UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(this.mFirstUser)).key(InstallPairAppsReceiver.EXTRA_SECOND_USER_HANDLE_KEY).value(UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(this.mSecondUser)).key(InstallPairAppsReceiver.EXTRA_FIRST_COMPONENT_KEY).value(this.mFirstCN.flattenToString()).key(InstallPairAppsReceiver.EXTRA_SECOND_COMPONENT_KEY).value(this.mSecondCN.flattenToString()).key(InstallPairAppsReceiver.PAIR_APPS_SHORTCUT_TYPE_KEY).value(true);
                if (this.mIcon != null) {
                    byte[] iconByteArray = Utilities.flattenBitmap(this.mIcon);
                    if (iconByteArray != null) {
                        json = json.key("icon").value(Base64.encodeToString(iconByteArray, 0, iconByteArray.length, 0));
                    }
                }
                return json.endObject().toString();
            } catch (JSONException e) {
                Log.d(InstallPairAppsReceiver.TAG, "Exception when adding shortcut: " + e);
                return null;
            }
        }

        void runRequestInfo(Context context) {
            LauncherAppState app = LauncherAppState.getInstance();
            ArrayList<ItemInfo> addShortcuts = new ArrayList();
            IconInfo iconInfo = getShortcutInfo();
            if (iconInfo != null) {
                addShortcuts.add(iconInfo);
            }
            app.getModel().getHomeLoader().addAndBindAddedWorkspaceItems(context, addShortcuts, false);
            SALogging.getInstance().insertAddPairAppsEventLog(iconInfo);
        }

        IconInfo getShortcutInfo() {
            LauncherPairAppsInfo iconInfo = new LauncherPairAppsInfo(this.mFirst, this.mSecond, this.mFirstCN, this.mSecondCN, this.mFirstUser, this.mSecondUser);
            iconInfo.setIcon(this.mIcon);
            iconInfo.title = this.mLabel;
            iconInfo.intent = this.mData;
            return iconInfo;
        }

        String getTargetPackage() {
            return null;
        }

        boolean getContainPackage(ArrayList<String> packageNames) {
            if (packageNames.contains(getInterPackage(0)) || packageNames.contains(getInterPackage(1))) {
                return true;
            }
            return false;
        }

        String getInterPackage(int pos) {
            String packageName = "";
            if (pos != 0 && pos != 1) {
                return "";
            }
            if (pos == 0) {
                packageName = this.mFirst.getPackage();
                if (packageName == null) {
                    if (this.mFirst.getComponent() == null) {
                        packageName = null;
                    } else {
                        packageName = this.mFirst.getComponent().getPackageName();
                    }
                }
            }
            if (pos == 1) {
                packageName = this.mSecond.getPackage();
                if (packageName == null) {
                    if (this.mSecond.getComponent() == null) {
                        packageName = null;
                    } else {
                        packageName = this.mSecond.getComponent().getPackageName();
                    }
                }
            }
            return packageName;
        }
    }

    public void onReceive(final Context context, Intent data) {
        if (!ACTION_ADD_PAIR_APP_SHORTCUT.equals(data.getAction())) {
            return;
        }
        if (Utilities.isDeskTopMode(context)) {
            Log.i(TAG, "Not support install pair apps on DeX mode");
        } else if (getComponentValue(context, data)) {
            final LauncherAppState app = LauncherAppState.getInstance();
            final PendingInstallPairAppsInfo info = new PendingInstallPairAppsInfo(data, context, -1, this.mFirstIntent, this.mSecondIntent, this.mFirstCN, this.mSecondCN, this.mFirstUser, this.mSecondUser);
            app.getModel();
            LauncherModel.runOnWorkerThread(new Runnable() {
                public void run() {
                    ExternalRequestQueue.queueExternalRequestInfo(info, context, app);
                }
            });
            showInstallToast(context);
        } else {
            Log.i(TAG, "Not install pair apps : extra failed");
        }
    }

    private static void showInstallToast(Context context) {
        String msg = context.getString(R.string.apppair_installed);
        if (Utilities.sIsRtl) {
            msg = '‏' + msg;
        }
        // TODO: Get resource
        //Toast.makeText(new ContextThemeWrapper(context, 16974123), msg, Toast.LENGTH_SHORT).show();
        Log.d(TAG, msg);
    }

    private boolean getComponentValue(Context context, Intent data) {
        String firstCN = data.getStringExtra(EXTRA_PAIR_APP_COMPONENT_NAME_FIRST);
        String secondCN = data.getStringExtra(EXTRA_PAIR_APP_COMPONENT_NAME_SECOND);
        int firstId = data.getIntExtra(EXTRA_PAIR_APP_USERID_FIRST, -1);
        int secondId = data.getIntExtra(EXTRA_PAIR_APP_USERID_SECOND, -1);
        if (firstCN == null || secondCN == null || firstId == -1 || secondId == -1) {
            Log.d(TAG, "getComponentValue : " + firstCN + " " + secondCN + " " + firstId + " " + secondId);
            return false;
        }
        String pkgName;
        int profileId;
        this.mFirstCN = ComponentName.unflattenFromString(firstCN);
        this.mSecondCN = ComponentName.unflattenFromString(secondCN);
        UserManagerCompat userManager = UserManagerCompat.getInstance(context);
        if (this.mFirstCN != null && Utilities.isValidComponent(context, this.mFirstCN)) {
            pkgName = this.mFirstCN.getPackageName();
            // TODO: Samsung specific code
//            if (SemDualAppManager.isInstalledWhitelistedPackage(pkgName) && DualAppUtils.isDualApp(userManager.getUserForSerialNumber((long) firstId), pkgName)) {
//                profileId = SemDualAppManager.getDualAppProfileId();
//                if (profileId != -10000) {
//                    this.mFirstIntent = new Intent("android.intent.action.MAIN");
//                    this.mFirstIntent.addCategory("android.intent.category.LAUNCHER");
//                    this.mFirstIntent.setComponent(this.mFirstCN);
//                    this.mFirstUser = userManager.getUserForSerialNumber((long) profileId);
//                }
//            } else {
                this.mFirstIntent = new Intent("android.intent.action.MAIN");
                this.mFirstIntent.addCategory("android.intent.category.LAUNCHER");
                this.mFirstIntent.setComponent(this.mFirstCN);
                this.mFirstUser = userManager.getUserForSerialNumber((long) firstId);
            //}
        }
        if (this.mSecondCN != null && Utilities.isValidComponent(context, this.mSecondCN)) {
            pkgName = this.mSecondCN.getPackageName();
            // TODO: Samsung specific code
//            if (SemDualAppManager.isInstalledWhitelistedPackage(pkgName) && DualAppUtils.isDualApp(userManager.getUserForSerialNumber((long) secondId), pkgName)) {
//                profileId = SemDualAppManager.getDualAppProfileId();
//                if (profileId != -10000) {
//                    this.mSecondIntent = new Intent("android.intent.action.MAIN");
//                    this.mSecondIntent.addCategory("android.intent.category.LAUNCHER");
//                    this.mSecondIntent.setComponent(this.mSecondCN);
//                    this.mSecondUser = userManager.getUserForSerialNumber((long) profileId);
//                }
//            } else {
                this.mSecondIntent = new Intent("android.intent.action.MAIN");
                this.mSecondIntent.addCategory("android.intent.category.LAUNCHER");
                this.mSecondIntent.setComponent(this.mSecondCN);
                this.mSecondUser = userManager.getUserForSerialNumber((long) secondId);
           // }
        }
        if (this.mFirstIntent == null || this.mSecondIntent == null) {
            return false;
        }
        return true;
    }

    static PendingInstallPairAppsInfo decode(String encoded, Context context) {
        try {
            JSONObject object = (JSONObject) new JSONTokener(encoded).nextValue();
            Intent firstIntent = Intent.parseUri(object.getString(LAUNCH_FIRST_INTENT_KEY), Intent.URI_ALLOW_UNSAFE);
            Intent secondIntent = Intent.parseUri(object.getString(LAUNCH_SECOND_INTENT_KEY), Intent.URI_ALLOW_UNSAFE);
            long requestTime = object.getLong("time");
            UserHandleCompat firstUser = UserManagerCompat.getInstance(context).getUserForSerialNumber(object.getLong(EXTRA_FIRST_USER_HANDLE_KEY));
            UserHandleCompat secondUser = UserManagerCompat.getInstance(context).getUserForSerialNumber(object.getLong(EXTRA_SECOND_USER_HANDLE_KEY));
            ComponentName firstCn = ComponentName.unflattenFromString(object.getString(EXTRA_FIRST_COMPONENT_KEY));
            ComponentName secondCn = ComponentName.unflattenFromString(object.getString(EXTRA_SECOND_COMPONENT_KEY));
            Intent data = new Intent();
            String iconBase64 = object.optString("icon");
            if (!(iconBase64 == null || iconBase64.isEmpty())) {
                byte[] iconArray = Base64.decode(iconBase64, 0);
                data.putExtra("android.intent.extra.shortcut.ICON", BitmapFactory.decodeByteArray(iconArray, 0, iconArray.length));
            }
            return new PendingInstallPairAppsInfo(data, context, requestTime, firstIntent, secondIntent, firstCn, secondCn, firstUser, secondUser);
        } catch (JSONException e) {
            Log.d(TAG, "Exception reading shortcut to add: " + e);
            return null;
        } catch (URISyntaxException e2) {
            Log.d(TAG, "Exception reading shortcut to add: " + e2);
            return null;
        }
    }
}

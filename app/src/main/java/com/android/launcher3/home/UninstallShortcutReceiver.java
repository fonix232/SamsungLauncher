package com.android.launcher3.home;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo.Builder;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.Toast;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.util.DualAppUtils;
import com.sec.android.app.launcher.R;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

public class UninstallShortcutReceiver extends BroadcastReceiver {
    private static final String ACTION_UNINSTALL_SHORTCUT = "com.android.launcher.action.UNINSTALL_SHORTCUT";
    private static final String DUPLICATE_KEY = "duplicate";
    private static final String LAUNCH_INTENT_KEY = "intent.launch";
    private static final String NAME_KEY = "name";
    private static final String TAG = "UninstallShortcut";
    private static final String USER_HANDLE_KEY = "userHandle";

    static class PendingUninstallShortcutInfo extends ExternalRequestInfo {
        final Context mContext;
        final Intent mData;
        final boolean mDuplicate;
        final String mShortcutId;

        PendingUninstallShortcutInfo(Intent data, Context context, long time) {
            super(2, UserHandleCompat.myUserHandle(), time);
            this.mData = data;
            this.mContext = context;
            this.mLaunchIntent = (Intent) data.getParcelableExtra("android.intent.extra.shortcut.INTENT");
            this.mLabel = data.getStringExtra("android.intent.extra.shortcut.NAME");
            this.mDuplicate = data.getBooleanExtra(UninstallShortcutReceiver.DUPLICATE_KEY, true);
            this.mShortcutId = data.getStringExtra(ShortcutInfoCompat.EXTRA_SHORTCUT_ID);
            if (this.mShortcutId != null) {
                Log.d(UninstallShortcutReceiver.TAG, "isPinShortcut :" + this.mLaunchIntent);
                makeShortcutInfo();
            } else if (InstallShortcutReceiver.convertKnoxLiveIconIntent(this.mLaunchIntent, data)) {
                this.user = UserManagerCompat.getInstance(context).getUserForSerialNumber((long) this.mLaunchIntent.getIntExtra(IconView.EXTRA_SHORTCUT_USER_ID, -1));
            } else if (Utilities.isLauncherAppTarget(this.mLaunchIntent)) {
                long serialNumber = UserManagerCompat.getInstance(context).getSerialNumberForUser(this.user);
                if (this.mLaunchIntent.getExtras() != null) {
                    long profile = this.mLaunchIntent.getExtras().getLong(ItemInfo.EXTRA_PROFILE, -1);
                    if (!(profile == -1 || profile == serialNumber)) {
                        serialNumber = profile;
                    }
                }
                this.mLaunchIntent = IconInfo.makeLaunchIntent(this.mLaunchIntent.getComponent(), serialNumber);
            }
            if (DualAppUtils.supportDualApp(context)) {
                UserHandleCompat intentUser = UserHandleCompat.fromIntent(data);
                if (intentUser != null) {
                    this.user = intentUser;
                    Log.d(UninstallShortcutReceiver.TAG, "EXTRA_USER " + this.user.toString());
                }
            }
        }

        @TargetApi(25)
        private void makeShortcutInfo() {
            if (this.mLaunchIntent == null || this.mLaunchIntent.getComponent() == null) {
                this.mLaunchIntent = null;
            } else {
                this.mLaunchIntent = new ShortcutInfoCompat(new Builder(this.mContext, this.mShortcutId).setActivity(this.mLaunchIntent.getComponent()).setIntent(this.mLaunchIntent).build()).makeIntent(this.mContext, this.mLaunchIntent.getComponent().getPackageName());
            }
        }

        public String encodeToString() {
            if (this.mLaunchIntent.getAction() == null) {
                this.mLaunchIntent.setAction("android.intent.action.VIEW");
            }
            try {
                return new JSONStringer().object().key("type").value(2).key("time").value(this.requestTime).key(UninstallShortcutReceiver.LAUNCH_INTENT_KEY).value(this.mLaunchIntent.toUri(0)).key("name").value(this.mLabel).key(UninstallShortcutReceiver.DUPLICATE_KEY).value(this.mDuplicate).key(UninstallShortcutReceiver.USER_HANDLE_KEY).value(UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(this.user)).endObject().toString();
            } catch (JSONException e) {
                Log.d(UninstallShortcutReceiver.TAG, "Exception when adding uninstall shortcut: " + e);
                return null;
            }
        }

        public void runRequestInfo(Context context) {
            LauncherAppState app = LauncherAppState.getInstance();
            String packageName = getTargetPackage();
            if (!TextUtils.isEmpty(packageName)) {
                UserHandleCompat myUserHandle = this.user != null ? this.user : UserHandleCompat.myUserHandle();
                if (DualAppUtils.supportDualApp(context) && DualAppUtils.DUAL_APP_DAAGENT_PACKAGE_NAME.equals(packageName)) {
                    myUserHandle = UserHandleCompat.myUserHandle();
                }
                if (!LauncherModel.isValidPackage(context, packageName, myUserHandle)) {
                    Log.d(UninstallShortcutReceiver.TAG, "Ignoring shortcut for absent package:" + this.mLaunchIntent);
                    return;
                }
            }
            app.getModel().getHomeLoader().removeWorkspaceItem(false, -1, this.mLabel, this.mLaunchIntent, this.mDuplicate);
        }

        public String getTargetPackage() {
            String packageName = this.mLaunchIntent.getPackage();
            if (packageName != null) {
                return packageName;
            }
            if (this.mLaunchIntent.getComponent() == null) {
                return null;
            }
            return this.mLaunchIntent.getComponent().getPackageName();
        }

        boolean getContainPackage(ArrayList<String> packageNames) {
            return packageNames.contains(getTargetPackage());
        }
    }

    public void onReceive(final Context context, final Intent data) {
        if (!ACTION_UNINSTALL_SHORTCUT.equals(data.getAction())) {
            return;
        }
        if (IconView.isKnoxShortcut((Intent) data.getParcelableExtra("android.intent.extra.shortcut.INTENT")) || !Utilities.isDeskTopMode(context)) {
            final LauncherAppState app = LauncherAppState.getInstance();
            app.getModel();
            LauncherModel.runOnWorkerThread(new Runnable() {
                public void run() {
                    Log.d(UninstallShortcutReceiver.TAG, "UNINSTALL_SHORTCUT - onReceive");
                    PendingUninstallShortcutInfo info = new PendingUninstallShortcutInfo(data, context, -1);
                    if (info.mLaunchIntent == null || info.mLabel == null) {
                        Log.e(UninstallShortcutReceiver.TAG, "Invalid uninstall shortcut intent");
                        return;
                    }
                    boolean showToast = false;
                    if (InstallShortcutReceiver.shortcutExistsInDb(context, info, info.getUser(), LauncherAppState.getInstance().isHomeOnlyModeEnabled())) {
                        Log.d(UninstallShortcutReceiver.TAG, "shortcut is exist in DB.");
                        ExternalRequestQueue.queueExternalRequestInfo(info, context, app);
                        showToast = true;
                    } else if (UninstallShortcutReceiver.this.shortcutExistInQueue(context, info.mLaunchIntent)) {
                        Log.d(UninstallShortcutReceiver.TAG, "shortcut is exist in queue.");
                        showToast = true;
                    } else if (InstallShortcutReceiver.convertKnoxLiveIconIntent(info.mLaunchIntent, data)) {
                        long userId = (long) info.mLaunchIntent.getIntExtra(IconView.EXTRA_SHORTCUT_USER_ID, -1);
                        String cmpName = info.mLaunchIntent.getStringExtra(IconView.EXTRA_SHORTCUT_LIVE_ICON_COMPONENT);
                        try {
                            info.mLaunchIntent = Intent.parseUri(UninstallShortcutReceiver.this.makeShortcutIntent(cmpName, userId), 0);
                            UserHandleCompat user = UserManagerCompat.getInstance(context).getUserForSerialNumber(userId);
                            Log.d(UninstallShortcutReceiver.TAG, "convert KnoxLiveIconIntent into LaunchIntent: " + cmpName + ", userId:" + userId);
                            if (InstallShortcutReceiver.shortcutExistsInDb(context, info, user, LauncherAppState.getInstance().isHomeOnlyModeEnabled())) {
                                Log.d(UninstallShortcutReceiver.TAG, "shortcut is exist in DB for Live icon.");
                                info.mLaunchIntent.putExtra(ItemInfo.EXTRA_PROFILE, userId);
                                info.user = UserManagerCompat.getInstance(context).getUserForSerialNumber(userId);
                                ExternalRequestQueue.queueExternalRequestInfo(info, context, app);
                                showToast = true;
                            }
                        } catch (URISyntaxException e) {
                            Log.e(UninstallShortcutReceiver.TAG, "URISyntaxException: " + e);
                        }
                    }
                    if (showToast && !Utilities.isDeskTopMode(context)) {
                        String msg = context.getString(R.string.shortcut_uninstalled, new Object[]{info.mLabel});
                        Toast.makeText(new ContextThemeWrapper(context, 16974123), msg, 0).show();
                        Log.d(UninstallShortcutReceiver.TAG, msg);
                    }
                }
            });
            return;
        }
        Log.i(TAG, "Not support uninstall shortcut on DeX mode");
    }

    private String makeShortcutIntent(String cmpName, long userId) {
        return "#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;launchFlags=0x10200000;component=" + cmpName + ";l.profile=" + userId + ";end";
    }

    private boolean shortcutExistInQueue(Context context, Intent intent) {
        ArrayList<ExternalRequestInfo> externalRequestInfos = ExternalRequestQueue.getExternalRequestListByType(context, 1);
        if (externalRequestInfos.isEmpty()) {
            return ExternalRequestQueue.removeFromExternalRequestQueue(context, 1, intent);
        }
        Iterator it = externalRequestInfos.iterator();
        while (it.hasNext()) {
            ExternalRequestInfo info = (ExternalRequestInfo) it.next();
            if (intent.toUri(0).equals(((PendingInstallShortcutInfo) info).mLaunchIntent.toUri(0))) {
                ExternalRequestQueue.removeFromExternalRequestQueue(context, info);
                return true;
            }
        }
        return false;
    }

    static PendingUninstallShortcutInfo decode(String encoded, Context context) {
        try {
            JSONObject object = (JSONObject) new JSONTokener(encoded).nextValue();
            Intent data = new Intent();
            data.putExtra("android.intent.extra.shortcut.INTENT", Intent.parseUri(object.getString(LAUNCH_INTENT_KEY), 4));
            data.putExtra("android.intent.extra.shortcut.NAME", object.getString("name"));
            data.putExtra(DUPLICATE_KEY, object.getBoolean(DUPLICATE_KEY));
            UserHandleCompat user = UserManagerCompat.getInstance(context).getUserForSerialNumber(object.getLong(USER_HANDLE_KEY));
            if (DualAppUtils.supportDualApp(context) && user != null) {
                data.putExtra("android.intent.extra.USER", user.getUser());
            }
            return new PendingUninstallShortcutInfo(data, context, object.getLong("time"));
        } catch (JSONException e) {
            Log.d(TAG, "Exception reading shortcut to remove: " + e);
            return null;
        } catch (URISyntaxException e2) {
            e2.printStackTrace();
            return null;
        }
    }
}

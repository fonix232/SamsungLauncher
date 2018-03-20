package com.android.launcher3.home;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.Toast;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.bnr.smartswitch.SmartSwitchBnr;
import com.android.launcher3.common.compat.DeferredLauncherActivityInfo;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.model.LauncherSettings.Favorites_Easy;
import com.android.launcher3.common.model.LauncherSettings.Favorites_HomeApps;
import com.android.launcher3.common.model.LauncherSettings.Favorites_HomeOnly;
import com.android.launcher3.common.model.LauncherSettings.Favorites_Standard;
import com.android.launcher3.common.quickoption.shortcuts.DeepShortcutManager;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutCache;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutKey;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.common.view.LiveIconManager;
import com.android.launcher3.gamehome.GameHomeManager;
import com.android.launcher3.util.DualAppUtils;
import com.android.launcher3.util.PinnedShortcutUtils;
import com.android.launcher3.util.StringJoiner;
import com.samsung.android.app.SemDualAppManager;
import com.sec.android.app.launcher.R;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

public class InstallShortcutReceiver extends BroadcastReceiver {
    private static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
    private static final String APP_SHORTCUT_TYPE_KEY = "isAppShortcut";
    private static final boolean DBG = false;
    private static final String DEEPSHORTCUT_TYPE_KEY = "isDeepShortcut";
    private static final String EDM_PACKAGE_NAME = "com.android.server.enterprise.application.ApplicationPolicy";
    private static final String EMD_PACKAGE_KEY = "do_not_show_popup";
    static final String EXTRA_SHORTCUT_DUPLICATE = "duplicate";
    private static final String ICON_KEY = "icon";
    private static final String ICON_RESOURCE_NAME_KEY = "iconResource";
    private static final String ICON_RESOURCE_PACKAGE_NAME_KEY = "iconResourcePackage";
    private static final String LAUNCH_INTENT_KEY = "intent.launch";
    private static final String NAME_KEY = "name";
    static final int NEW_SHORTCUT_BOUNCE_DURATION = 450;
    static final int NEW_SHORTCUT_STAGGER_DELAY = 85;
    private static final String TAG = "InstallShortcutReceiver";
    private static final String THEME_STORE_PACKAGE = "com.samsung.android.themestore";
    private static final String USER_HANDLE_KEY = "userHandle";

    static class PendingInstallShortcutInfo extends ExternalRequestInfo {
        final LauncherActivityInfoCompat mActivityInfo;
        final Context mContext;
        final Intent mData;
        final boolean mNeedToMovePage;
        final ShortcutInfoCompat mShortcutInfo;

        public PendingInstallShortcutInfo(Intent data, Context context, long time) {
            super(1, UserHandleCompat.myUserHandle(), time);
            this.mData = data;
            this.mContext = context;
            this.mLaunchIntent = (Intent) data.getParcelableExtra("android.intent.extra.shortcut.INTENT");
            this.mLabel = data.getStringExtra("android.intent.extra.shortcut.NAME");
            this.mActivityInfo = null;
            this.mShortcutInfo = null;
            this.mNeedToMovePage = false;
            if (InstallShortcutReceiver.convertKnoxLiveIconIntent(this.mLaunchIntent, data)) {
                this.user = UserManagerCompat.getInstance(context).getUserForSerialNumber((long) this.mLaunchIntent.getIntExtra(IconView.EXTRA_SHORTCUT_USER_ID, -1));
            }
            UserHandleCompat intentUser = UserHandleCompat.fromIntent(data);
            if (intentUser != null) {
                this.user = intentUser;
                Log.d(InstallShortcutReceiver.TAG, "EXTRA_USER " + this.user.toString());
            }
        }

        public PendingInstallShortcutInfo(LauncherActivityInfoCompat info, Context context, long time) {
            super(1, info.getUser(), time);
            this.mData = null;
            this.mContext = context;
            this.mActivityInfo = info;
            this.mShortcutInfo = null;
            this.mNeedToMovePage = false;
            this.mLaunchIntent = IconInfo.makeLaunchIntent(context, info, this.user);
            this.mLabel = info.getLabel().toString();
        }

        public PendingInstallShortcutInfo(LauncherActivityInfoCompat info, Context context, long time, Intent intent) {
            super(1, info.getUser(), time);
            this.mData = null;
            this.mContext = context;
            this.mActivityInfo = info;
            this.mShortcutInfo = null;
            this.mNeedToMovePage = false;
            this.mLaunchIntent = IconInfo.makeLaunchIntent(context, intent, this.user);
            this.mLabel = info.getLabel().toString();
        }

        public PendingInstallShortcutInfo(ShortcutInfoCompat info, Context context, long time, boolean needMovePage) {
            super(1, info.getUserHandle(), time);
            this.mData = null;
            this.mContext = context;
            this.mActivityInfo = null;
            this.mShortcutInfo = info;
            this.mNeedToMovePage = needMovePage;
            this.mLaunchIntent = info.makeIntent(context);
            this.mLabel = info.getShortLabel().toString();
        }

        public String encodeToString() {
            try {
                if (this.mActivityInfo != null) {
                    return new JSONStringer().object().key("type").value(1).key("time").value(this.requestTime).key(InstallShortcutReceiver.LAUNCH_INTENT_KEY).value(this.mLaunchIntent.toUri(0)).key(InstallShortcutReceiver.APP_SHORTCUT_TYPE_KEY).value(true).key(InstallShortcutReceiver.USER_HANDLE_KEY).value(UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(this.user)).endObject().toString();
                }
                if (this.mShortcutInfo != null) {
                    return new JSONStringer().object().key("type").value(1).key("time").value(this.requestTime).key(InstallShortcutReceiver.LAUNCH_INTENT_KEY).value(this.mLaunchIntent.toUri(0)).key(InstallShortcutReceiver.DEEPSHORTCUT_TYPE_KEY).value(true).key(InstallShortcutReceiver.USER_HANDLE_KEY).value(UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(this.user)).endObject().toString();
                }
                Bitmap icon = (Bitmap) this.mData.getParcelableExtra("android.intent.extra.shortcut.ICON");
                ShortcutIconResource iconResource = (ShortcutIconResource) this.mData.getParcelableExtra("android.intent.extra.shortcut.ICON_RESOURCE");
                JSONStringer json = new JSONStringer().object().key("type").value(1).key("time").value(this.requestTime).key(InstallShortcutReceiver.LAUNCH_INTENT_KEY).value(this.mLaunchIntent.toUri(0)).key("name").value(InstallShortcutReceiver.ensureValidName(this.mContext, this.mLaunchIntent, this.mLabel).toString()).key(InstallShortcutReceiver.USER_HANDLE_KEY).value(UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(this.user));
                if (icon != null) {
                    byte[] iconByteArray = Utilities.flattenBitmap(icon);
                    json = json.key("icon").value(Base64.encodeToString(iconByteArray, 0, iconByteArray.length, 0));
                }
                if (iconResource != null) {
                    json = json.key("iconResource").value(iconResource.resourceName).key(InstallShortcutReceiver.ICON_RESOURCE_PACKAGE_NAME_KEY).value(iconResource.packageName);
                }
                return json.endObject().toString();
            } catch (JSONException e) {
                Log.d(InstallShortcutReceiver.TAG, "Exception when adding shortcut: " + e);
                return null;
            }
        }

        public void runRequestInfo(Context context) {
            String packageName = getTargetPackage();
            if (!TextUtils.isEmpty(packageName)) {
                UserHandleCompat myUserHandle = this.user != null ? this.user : UserHandleCompat.myUserHandle();
                if (DualAppUtils.supportDualApp(context) && DualAppUtils.DUAL_APP_DAAGENT_PACKAGE_NAME.equals(packageName)) {
                    myUserHandle = UserHandleCompat.myUserHandle();
                }
                if (!LauncherModel.isValidPackage(context, packageName, myUserHandle)) {
                    Log.d(InstallShortcutReceiver.TAG, "Ignoring shortcut for absent package:" + this.mLaunchIntent);
                    return;
                }
            }
            GameHomeManager gameHomeManager = GameHomeManager.getInstance();
            if (gameHomeManager.isGameHomeHidden() && this.mActivityInfo != null && gameHomeManager.hasGameHomeThisPackage(this.mActivityInfo)) {
                Log.e(InstallShortcutReceiver.TAG, "Ignoring shortcut for game packcage & hidden setting");
                return;
            }
            LauncherAppState app = LauncherAppState.getInstance();
            ArrayList<ItemInfo> addShortcuts = new ArrayList();
            IconInfo iconInfo = getShortcutInfo();
            if (iconInfo != null) {
                addShortcuts.add(iconInfo);
                if (iconInfo.getTargetComponent() != null) {
                    iconInfo.mBadgeCount = app.getModel().getHomeLoader().getBadgeCount(iconInfo.getTargetComponent(), iconInfo.user);
                }
            }
            app.getModel().getHomeLoader().addAndBindAddedWorkspaceItems(context, addShortcuts, this.mNeedToMovePage);
        }

        IconInfo getShortcutInfo() {
            if (this.mActivityInfo != null) {
                IconInfo info = IconInfo.fromActivityInfo(this.mActivityInfo, this.mContext);
                List<LauncherActivityInfoCompat> activities = LauncherAppsCompat.getInstance(this.mContext).getActivityList(info.componentName.getPackageName(), info.user);
                boolean isActivityInApps = false;
                if (!activities.isEmpty()) {
                    for (LauncherActivityInfoCompat lai : activities) {
                        if (lai.getComponentName().equals(info.componentName)) {
                            isActivityInApps = true;
                            break;
                        }
                    }
                }
                if (isActivityInApps) {
                    return info;
                }
                info.isAppShortcut = true;
                info.itemType = 1;
                return info;
            } else if (this.mShortcutInfo != null) {
                return new IconInfo(this.mShortcutInfo, this.mContext);
            } else {
                return LauncherAppState.getInstance().getModel().getHomeLoader().infoFromShortcutIntent(this.mContext, this.mData);
            }
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

        boolean isLuncherActivity() {
            return this.mActivityInfo != null;
        }
    }

    private boolean hasActivityForComponent(Context context, ComponentName cn) {
        List<LauncherActivityInfoCompat> apps = LauncherAppsCompat.getInstance(context).getActivityList(cn.getPackageName(), UserHandleCompat.myUserHandle());
        if (apps != null) {
            for (LauncherActivityInfoCompat info : apps) {
                if (cn.equals(info.getComponentName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void onReceive(final Context context, final Intent data) {
        if (!ACTION_INSTALL_SHORTCUT.equals(data.getAction())) {
            return;
        }
        if (Utilities.isDeskTopMode(context)) {
            Log.i(TAG, "Not support install shortcut on DeX mode");
            return;
        }
        final LauncherAppState app = LauncherAppState.getInstance();
        LauncherModel.runOnWorkerThread(new Runnable() {
            public void run() {
                if (InstallShortcutReceiver.isValidExtraType(data, "android.intent.extra.shortcut.INTENT", Intent.class) && InstallShortcutReceiver.isValidExtraType(data, "android.intent.extra.shortcut.ICON_RESOURCE", ShortcutIconResource.class) && InstallShortcutReceiver.isValidExtraType(data, "android.intent.extra.shortcut.ICON", Bitmap.class)) {
                    PendingInstallShortcutInfo info = new PendingInstallShortcutInfo(data, context, -1);
                    if (info.mLaunchIntent == null || info.mLabel == null) {
                        Log.d(InstallShortcutReceiver.TAG, "Invalid install shortcut intent");
                        return;
                    }
                    if (InstallShortcutReceiver.this.updateContactShortcut(context, data, info)) {
                        Log.e(InstallShortcutReceiver.TAG, "updateContactShortcut true");
                        return;
                    } else if (info.isLuncherActivity() || Utilities.hasPermissionForActivity(context, info.mLaunchIntent, null)) {
                        if (LauncherFeature.supportHomeModeChange() && app.isHomeOnlyModeEnabled()) {
                            if ("android.intent.action.MAIN".equals(info.mLaunchIntent.getAction())) {
                                boolean isKnoxShortcut = false;
                                Intent intent = info.mLaunchIntent;
                                if (intent.getComponent() != null && IconView.KNOX_SHORTCUT_PACKAGE.equals(intent.getComponent().getPackageName())) {
                                    data.putExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_DUPLICATE, false);
                                    isKnoxShortcut = true;
                                }
                                if (!(isKnoxShortcut || intent.getComponent() == null)) {
                                    if (InstallShortcutReceiver.this.hasActivityForComponent(context, intent.getComponent())) {
                                        if (Utilities.isLauncherAppTarget(intent)) {
                                            Log.d(InstallShortcutReceiver.TAG, "This shortcut is same with application!");
                                            return;
                                        }
                                        if (InstallShortcutReceiver.this.shortcutExistsOnHomeOnlyMode(context, intent)) {
                                            Log.d(InstallShortcutReceiver.TAG, "This component(" + info.mLabel + ") exist in DB.");
                                            return;
                                        }
                                    } else if (!InstallShortcutReceiver.THEME_STORE_PACKAGE.equals(intent.getComponent().getPackageName())) {
                                        Log.d(InstallShortcutReceiver.TAG, "This component(" + info.mLabel + ") have no ACTION_MAIN");
                                        intent.setAction("android.intent.action.VIEW");
                                    }
                                }
                            } else if (InstallShortcutReceiver.this.shortcutExistsOnHomeOnlyMode(context, info.mLabel, info.mLaunchIntent)) {
                                Log.d(InstallShortcutReceiver.TAG, "This component(" + info.mLabel + ") exist in DB.(check title & intent)");
                                return;
                            }
                        }
                        info = InstallShortcutReceiver.convertToLauncherActivityIfPossible(info);
                        if (info.mActivityInfo == null && info.mLaunchIntent.getAction() == null) {
                            info.mLaunchIntent.setAction("android.intent.action.VIEW");
                        }
                        boolean showDuplicateToast = false;
                        boolean addQueue = true;
                        boolean skipToastPopup = false;
                        if (InstallShortcutReceiver.shortcutExistsInDb(context, info, info.getUser(), false)) {
                            if (!InstallShortcutReceiver.this.uninstallShortcutExistInQueue(context, info.mLaunchIntent)) {
                                Log.d(InstallShortcutReceiver.TAG, "This shortcut (" + info.mLaunchIntent + ") is exist in DB.");
                                showDuplicateToast = true;
                            } else if (IconView.isKnoxShortcut(info.mLaunchIntent)) {
                                Log.d(InstallShortcutReceiver.TAG, "This shortcut (" + info.mLaunchIntent + ") is exist in DB & Uninstallshort queue & knox.");
                            } else {
                                Log.d(InstallShortcutReceiver.TAG, "This shortcut (" + info.mLaunchIntent + ") is exist in DB & Uninstallshort queue.");
                                addQueue = false;
                            }
                        } else if (InstallShortcutReceiver.shortcutExistInQueue(context, info)) {
                            Log.d(InstallShortcutReceiver.TAG, "This shortcut (" + info.mLaunchIntent + ") is exist in queue.");
                            showDuplicateToast = true;
                        }
                        if (!Utilities.isKnoxMode()) {
                            PackageManager pm = context.getPackageManager();
                            List<IntentFilter> intentList = new ArrayList();
                            List<ComponentName> componentList = new ArrayList();
                            if (pm != null) {
                                pm.getPreferredActivities(intentList, componentList, context.getPackageName());
                            }
                            if (componentList.size() <= 0) {
                                Intent intentHome = new Intent("android.intent.action.MAIN");
                                intentHome.addCategory("android.intent.category.HOME");
                                List<ResolveInfo> homeAppList = pm.queryIntentActivities(intentHome, 0);
                                int homeAppCount = 0;
                                if (homeAppList != null) {
                                    homeAppCount = homeAppList.size();
                                    for (ResolveInfo ri : homeAppList) {
                                        if (ri.activityInfo != null && ("com.sec.android.app.easylauncher".equals(ri.activityInfo.packageName) || "com.android.settings".equals(ri.activityInfo.packageName))) {
                                            homeAppCount--;
                                        }
                                    }
                                }
                                Log.d(InstallShortcutReceiver.TAG, "Silent install shortcut due to none PreferredActivities count : " + homeAppCount);
                                if (homeAppCount > 1) {
                                    skipToastPopup = true;
                                }
                            }
                        }
                        if (!showDuplicateToast) {
                            if (addQueue) {
                                ExternalRequestQueue.queueExternalRequestInfo(info, context, app);
                            }
                            if (!Utilities.isDeskTopMode(context) && !skipToastPopup) {
                                InstallShortcutReceiver.showInstallToast(context, info.mLabel);
                                return;
                            }
                            return;
                        } else if (!app.isHomeOnlyModeEnabled() && !Utilities.isDeskTopMode(context) && !skipToastPopup) {
                            Toast.makeText(new ContextThemeWrapper(context, 16974123), context.getString(R.string.shortcut_duplicate, new Object[]{info.mLabel}), 0).show();
                            return;
                        } else {
                            return;
                        }
                    } else {
                        Log.e(InstallShortcutReceiver.TAG, "Ignoring malicious intent " + info.mLaunchIntent.toUri(0));
                        return;
                    }
                }
                Log.e(InstallShortcutReceiver.TAG, "Invalid install shortcut intent case 1");
            }
        });
    }

    public static void queuePendingShortcutInfo(final ShortcutInfoCompat info, final Context context, final boolean needToMovePage) {
        final LauncherAppState app = LauncherAppState.getInstance();
        LauncherModel.runOnWorkerThread(new Runnable() {
            public void run() {
                PendingInstallShortcutInfo shortcutInfo;
                Intent shortcutIntent = info.makeIntent(context);
                DeepShortcutManager deepShortcutManager = new DeepShortcutManager(context, new ShortcutCache());
                String str = shortcutIntent.getPackage();
                String[] strArr = new String[1];
                strArr[0] = shortcutIntent.getStringExtra(ShortcutInfoCompat.EXTRA_SHORTCUT_ID);
                List<ShortcutInfoCompat> shortcutInfoCompatList = deepShortcutManager.queryForFullDetails(str, Arrays.asList(strArr), info.getUserHandle());
                ShortcutInfoCompat queryShortcutInfo = null;
                if (!shortcutInfoCompatList.isEmpty()) {
                    queryShortcutInfo = (ShortcutInfoCompat) shortcutInfoCompatList.get(0);
                }
                Intent intent = queryShortcutInfo != null ? queryShortcutInfo.getShortcutInfo().getIntent() : null;
                if (intent == null || !Utilities.isLauncherAppTarget(intent)) {
                    PendingInstallShortcutInfo pendingInstallShortcutInfo = new PendingInstallShortcutInfo(info, context, -1, needToMovePage);
                } else {
                    Log.d(InstallShortcutReceiver.TAG, "This shortcut does not use app badge");
                    deepShortcutManager.unpinShortcut(ShortcutKey.fromInfo(queryShortcutInfo));
                    if (PinnedShortcutUtils.isRequestFromEDM(queryShortcutInfo.getShortcutInfo(), intent)) {
                        shortcutInfo = new PendingInstallShortcutInfo(new DeferredLauncherActivityInfo(intent.getComponent(), queryShortcutInfo.getUserHandle(), context), context, -1, intent);
                    } else {
                        shortcutInfo = new PendingInstallShortcutInfo(queryShortcutInfo.getActivityInfo(context), context, -1);
                    }
                    if (LauncherAppState.getInstance().isHomeOnlyModeEnabled() && (InstallShortcutReceiver.shortcutExistsInDb(context, shortcutInfo, shortcutInfo.getUser(), false) || InstallShortcutReceiver.shortcutExistInQueue(context, shortcutInfo))) {
                        Toast.makeText(new ContextThemeWrapper(context, 16974123), context.getString(R.string.shortcut_duplicate, new Object[]{shortcutInfo.mLabel}), 0).show();
                        return;
                    }
                }
                ExternalRequestQueue.queueExternalRequestInfo(shortcutInfo, context, app);
                InstallShortcutReceiver.showInstallToast(context, info.getShortLabel());
            }
        });
    }

    public static void queueActivityInfo(final LauncherActivityInfoCompat activity, final Context context) {
        final LauncherAppState app = LauncherAppState.getInstance();
        LauncherModel.runOnWorkerThread(new Runnable() {
            public void run() {
                ExternalRequestQueue.queueExternalRequestInfo(new PendingInstallShortcutInfo(activity, context, -1), context, app);
                InstallShortcutReceiver.showInstallToast(context, activity.getLabel());
            }
        });
    }

    private static void showInstallToast(Context context, CharSequence label) {
        String msg = context.getString(R.string.shortcut_installed, new Object[]{label});
        if (Utilities.sIsRtl) {
            msg = '‏' + msg;
        }
        Toast.makeText(new ContextThemeWrapper(context, 16974123), msg, 0).show();
        Log.d(TAG, msg);
    }

    static boolean shortcutExistsInDb(Context context, ExternalRequestInfo info, UserHandleCompat user, boolean checkShortcutOnly) {
        String intentWithPkg;
        Intent originalIntent = info.mLaunchIntent;
        boolean isDeepShortcut = Utilities.isDeepShortcut(originalIntent);
        Intent intent = new Intent(originalIntent);
        long serialNumber = UserManagerCompat.getInstance(context).getSerialNumberForUser(user);
        if (intent.getLongExtra(ItemInfo.EXTRA_PROFILE, -1) == -1) {
            intent.putExtra(ItemInfo.EXTRA_PROFILE, serialNumber);
        }
        String intentWithoutPkg;
        if (intent.getComponent() != null) {
            String packageName = intent.getComponent().getPackageName();
            if (intent.getPackage() != null) {
                intentWithPkg = intent.toUri(0);
                intentWithoutPkg = new Intent(intent).setPackage(null).toUri(0);
            } else {
                intentWithPkg = new Intent(intent).setPackage(packageName).toUri(0);
                intentWithoutPkg = intent.toUri(0);
            }
        } else {
            intentWithPkg = intent.toUri(0);
            intentWithoutPkg = intent.toUri(0);
        }
        boolean result = false;
        ContentResolver cr = context.getContentResolver();
        String folderIds = null;
        Cursor folderIdCursor = cr.query(Favorites.CONTENT_URI, new String[]{"_id"}, "itemType=2 AND (container=-100 OR container=-101)", null, null);
        if (folderIdCursor != null) {
            StringJoiner stringJoiner = new StringJoiner(",");
            while (folderIdCursor.moveToNext()) {
                stringJoiner.append(folderIdCursor.getLong(0));
            }
            folderIdCursor.close();
            folderIds = stringJoiner.toString();
        }
        String selection = "profileId=? AND (itemType=1";
        if (!checkShortcutOnly) {
            selection = selection + " OR itemType=0";
        }
        selection = (selection + " OR itemType=6") + ") AND (container=-100 OR container=-101";
        if (folderIds != null) {
            selection = selection + " OR container in (" + folderIds + "))";
        } else {
            selection = selection + ')';
        }
        String[] selectionArg = new String[]{Long.toString(serialNumber)};
        Cursor cursor = null;
        cursor = cr.query(Favorites.CONTENT_URI, new String[]{"title", "intent"}, selection, selectionArg, null);
        if (cursor != null) {
            int idxIntent = cursor.getColumnIndexOrThrow("intent");
            int idxTitle = cursor.getColumnIndexOrThrow("title");
            while (cursor.moveToNext()) {
                String string = cursor.getString(idxIntent);
                Intent dbIntent = Intent.parseUri(string, 4);
                if (!isDeepShortcut) {
                    try {
                        if (dbIntent.getLongExtra(ItemInfo.EXTRA_PROFILE, -1) == -1) {
                            dbIntent.putExtra(ItemInfo.EXTRA_PROFILE, serialNumber);
                            string = dbIntent.toUri(0);
                        }
                        if (Utilities.isLauncherAppTarget(dbIntent) && dbIntent.getFlags() == 0) {
                            dbIntent.setFlags(270532608);
                            string = dbIntent.toUri(0);
                        }
                        if (intentWithPkg.equals(string) || intentWithoutPkg.equals(string)) {
                            result = true;
                            break;
                        }
                    } catch (URISyntaxException e) {
                        Log.d(TAG, "shortcutExistsInDb : " + e);
                        if (cursor != null) {
                            cursor.close();
                        }
                    } catch (Throwable th) {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                } else if (Utilities.isDeepShortcut(dbIntent) && originalIntent.getComponent().equals(dbIntent.getComponent())) {
                    if (originalIntent.getStringExtra(ShortcutInfoCompat.EXTRA_SHORTCUT_ID).equals(dbIntent.getStringExtra(ShortcutInfoCompat.EXTRA_SHORTCUT_ID))) {
                        result = true;
                        Log.d(TAG, "shortcut id is same");
                        break;
                    }
                    if (info.mLabel.equals(cursor.getString(idxTitle))) {
                        result = true;
                        Log.d(TAG, "shortcut id is not same but title is same");
                        break;
                    }
                }
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return result;
    }

    private static boolean shortcutExistInQueue(Context context, ExternalRequestInfo info) {
        Intent originalIntent = info.mLaunchIntent;
        boolean isDeepShortcut = Utilities.isDeepShortcut(originalIntent);
        Iterator it = ExternalRequestQueue.getExternalRequestListByType(context, 1).iterator();
        while (it.hasNext()) {
            PendingInstallShortcutInfo pendingInfoInQueue = (PendingInstallShortcutInfo) ((ExternalRequestInfo) it.next());
            if (isDeepShortcut) {
                Intent queueIntent = pendingInfoInQueue.mLaunchIntent;
                if (Utilities.isDeepShortcut(queueIntent) && originalIntent.getComponent().equals(queueIntent.getComponent())) {
                    if (originalIntent.getStringExtra(ShortcutInfoCompat.EXTRA_SHORTCUT_ID).equals(queueIntent.getStringExtra(ShortcutInfoCompat.EXTRA_SHORTCUT_ID))) {
                        Log.d(TAG, "same shortcut id is exist in queue");
                        return true;
                    } else if (info.mLabel.equals(pendingInfoInQueue.mLabel)) {
                        Log.d(TAG, "shortcut id is not same but same title is exist in queue");
                        return true;
                    }
                }
            } else if (originalIntent.toUri(0).equals(pendingInfoInQueue.mLaunchIntent.toUri(0))) {
                Log.d(TAG, "shortcut is exist in queue");
                return true;
            }
        }
        return false;
    }

    private boolean uninstallShortcutExistInQueue(Context context, Intent intent) {
        ArrayList<ExternalRequestInfo> externalRequestInfos = ExternalRequestQueue.getExternalRequestListByType(context, 2);
        if (externalRequestInfos.isEmpty() && !IconView.isKnoxShortcut(intent)) {
            return ExternalRequestQueue.removeFromExternalRequestQueue(context, 2, intent);
        }
        Iterator it = externalRequestInfos.iterator();
        while (it.hasNext()) {
            PendingUninstallShortcutInfo pendingInfo = (PendingUninstallShortcutInfo) ((ExternalRequestInfo) it.next());
            if (intent.toUri(0).equals(pendingInfo.mLaunchIntent.toUri(0))) {
                if (!IconView.isKnoxShortcut(intent)) {
                    ExternalRequestQueue.removeFromExternalRequestQueue(context, pendingInfo);
                }
                return true;
            }
        }
        return false;
    }

    private boolean shortcutExistsOnHomeOnlyMode(Context context, Intent intent) {
        boolean result = false;
        ContentResolver cr = context.getContentResolver();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Set<String> keys = extras.keySet();
            String[] extraKeyBlackList = Utilities.EXTRA_KEY_BLACK_LIST;
            if (extraKeyBlackList != null && extraKeyBlackList.length > 0) {
                for (String key : extraKeyBlackList) {
                    if (keys.contains(key)) {
                        intent.removeExtra(key);
                    }
                }
            }
        }
        String[] str = intent.toUri(0).split("component=");
        if (str == null || str.length <= 1) {
            return false;
        }
        Intent profileIntent = new Intent(intent);
        profileIntent.putExtra(ItemInfo.EXTRA_PROFILE, UserManagerCompat.getInstance(context).getSerialNumberForUser(UserHandleCompat.myUserHandle()));
        String[] extraStr = profileIntent.toUri(0).split("component=");
        Cursor c = cr.query(Favorites.CONTENT_URI, new String[]{"intent"}, "intent like ? or intent like ?", new String[]{"%component=" + str[1], "%component=" + extraStr[1]}, null);
        if (c != null) {
            result = c.moveToFirst();
            c.close();
        }
        return result;
    }

    private boolean shortcutExistsOnHomeOnlyMode(Context context, String title, Intent intent) {
        boolean result = false;
        ContentResolver cr = context.getContentResolver();
        String[] detectStr = intent.toUri(0).split("component=");
        if (detectStr == null || detectStr.length <= 1) {
            return false;
        }
        String[] componentName = detectStr[1].split("\\;");
        if (title.startsWith(" ")) {
            title = title.substring(1);
        }
        if (title.endsWith(" ")) {
            title = title.substring(0, title.length() - 2);
        }
        Log.i(TAG, "shortcutExistsOnHomeOnlyMode:" + title.trim() + " title:" + title + ", " + componentName[0]);
        Cursor c = cr.query(Favorites.CONTENT_URI, new String[]{"intent"}, "(trim(title,' ')=? or trim(title)=?) and intent like ?", new String[]{trimTitle, trimTitle, "%component=" + componentName[0] + "%"}, null);
        if (c != null) {
            result = c.moveToFirst();
            c.close();
        }
        return result;
    }

    static IconInfo fromShortcutIntent(Context context, Intent data) {
        PendingInstallShortcutInfo info = new PendingInstallShortcutInfo(data, context, -1);
        if (info.mLaunchIntent == null || info.mLabel == null) {
            return null;
        }
        return convertToLauncherActivityIfPossible(info).getShortcutInfo();
    }

    static CharSequence ensureValidName(Context context, Intent intent, CharSequence name) {
        if (name == null) {
            try {
                PackageManager pm = context.getPackageManager();
                name = pm.getActivityInfo(intent.getComponent(), 0).loadLabel(pm);
            } catch (NameNotFoundException e) {
                return "";
            }
        }
        return name;
    }

    static boolean convertKnoxLiveIconIntent(Intent launchIntent, Intent data) {
        if (IconView.isKnoxShortcut(launchIntent)) {
            int userId = data.getIntExtra(IconView.EXTRA_SHORTCUT_USER_ID, -1);
            String pkgName = data.getStringExtra(IconView.EXTRA_SHORTCUT_LIVE_ICON_COMPONENT);
            if (userId >= 100 && LiveIconManager.isLiveIconPackage(pkgName)) {
                launchIntent.putExtra(IconView.EXTRA_SHORTCUT_USER_ID, userId);
                launchIntent.putExtra(IconView.EXTRA_SHORTCUT_LIVE_ICON_COMPONENT, pkgName);
                Log.d(TAG, "convertKnoxLiveIconIntent : " + pkgName);
                return true;
            }
        }
        return false;
    }

    static PendingInstallShortcutInfo decode(String encoded, Context context) {
        try {
            JSONObject object = (JSONObject) new JSONTokener(encoded).nextValue();
            Intent launcherIntent = Intent.parseUri(object.getString(LAUNCH_INTENT_KEY), 4);
            UserHandleCompat user = UserManagerCompat.getInstance(context).getUserForSerialNumber(object.getLong(USER_HANDLE_KEY));
            long requestTime = object.getLong("time");
            if (!object.optBoolean(APP_SHORTCUT_TYPE_KEY)) {
                if (!object.optBoolean(DEEPSHORTCUT_TYPE_KEY)) {
                    Intent data = new Intent();
                    data.putExtra("android.intent.extra.shortcut.INTENT", launcherIntent);
                    data.putExtra("android.intent.extra.shortcut.NAME", object.getString("name"));
                    String iconBase64 = object.optString("icon");
                    String iconResourceName = object.optString("iconResource");
                    String iconResourcePackageName = object.optString(ICON_RESOURCE_PACKAGE_NAME_KEY);
                    if (DualAppUtils.supportDualApp(context) && user != null) {
                        data.putExtra("android.intent.extra.USER", user.getUser());
                    }
                    if (iconBase64 != null && !iconBase64.isEmpty()) {
                        byte[] iconArray = Base64.decode(iconBase64, 0);
                        data.putExtra("android.intent.extra.shortcut.ICON", BitmapFactory.decodeByteArray(iconArray, 0, iconArray.length));
                    } else if (!(iconResourceName == null || iconResourceName.isEmpty())) {
                        ShortcutIconResource iconResource = new ShortcutIconResource();
                        iconResource.resourceName = iconResourceName;
                        iconResource.packageName = iconResourcePackageName;
                        data.putExtra("android.intent.extra.shortcut.ICON_RESOURCE", iconResource);
                    }
                    return new PendingInstallShortcutInfo(data, context, requestTime);
                } else if (user == null) {
                    Log.e(TAG, "decode DEEPSHORTCUT_TYPE_KEY, user object is null.");
                    return null;
                } else {
                    List<ShortcutInfoCompat> shortcutInfoCompatList = new DeepShortcutManager(context, new ShortcutCache()).queryForFullDetails(launcherIntent.getPackage(), Arrays.asList(new String[]{launcherIntent.getStringExtra(ShortcutInfoCompat.EXTRA_SHORTCUT_ID)}), user);
                    if (shortcutInfoCompatList.isEmpty()) {
                        Log.d(TAG, "This deep shortcut's query result is empty");
                        return null;
                    }
                    ShortcutInfoCompat shortcutInfo = (ShortcutInfoCompat) shortcutInfoCompatList.get(0);
                    if (shortcutInfo != null) {
                        return new PendingInstallShortcutInfo(shortcutInfo, context, requestTime, false);
                    }
                    Log.d(TAG, "This deep shortcut does not exist in shortcut manager");
                    return null;
                }
            } else if (user == null) {
                return null;
            } else {
                LauncherActivityInfoCompat info = LauncherAppsCompat.getInstance(context).resolveActivity(launcherIntent, user);
                return info == null ? null : new PendingInstallShortcutInfo(info, context, requestTime);
            }
        } catch (JSONException e) {
            Log.d(TAG, "Exception reading shortcut to add: " + e);
            return null;
        } catch (URISyntaxException e2) {
            Log.d(TAG, "Exception reading shortcut to add: " + e2);
            return null;
        }
    }

    private static PendingInstallShortcutInfo convertToLauncherActivityIfPossible(PendingInstallShortcutInfo original) {
        if (original.isLuncherActivity()) {
            return original;
        }
        boolean isDualImApp;
        if (DualAppUtils.supportDualApp(original.mContext) && SemDualAppManager.isDualAppId(original.user.getUser().semGetIdentifier())) {
            isDualImApp = true;
        } else {
            isDualImApp = false;
        }
        long userSerialNumber = UserManagerCompat.getInstance(original.mContext).getSerialNumberForUser(original.user);
        if (!DualAppUtils.DUAL_APP_DAAGENT_PACKAGE_NAME.equals(original.getTargetPackage()) && (isDualImApp || (userSerialNumber < 100 && !original.user.equals(UserHandleCompat.myUserHandle())))) {
            Log.d(TAG, "This is other user's app shortcut " + original.mLaunchIntent + " isDualImApp : " + isDualImApp);
            LauncherActivityInfoCompat launcherInfo = LauncherAppsCompat.getInstance(original.mContext).resolveActivity(original.mLaunchIntent, original.user);
            if (launcherInfo != null) {
                return new PendingInstallShortcutInfo(launcherInfo, original.mContext, original.requestTime);
            }
            Log.d(TAG, "This is other user's app shortcut. But launcherInfo is null!! isDualImApp : " + isDualImApp);
            return original;
        } else if (!Utilities.isLauncherAppTarget(original.mLaunchIntent) || !original.getUser().equals(UserHandleCompat.myUserHandle())) {
            return original;
        } else {
            ResolveInfo info = original.mContext.getPackageManager().resolveActivity(original.mLaunchIntent, 0);
            return info != null ? new PendingInstallShortcutInfo(LauncherActivityInfoCompat.fromResolveInfo(info, original.mContext), original.mContext, original.requestTime) : original;
        }
    }

    private static boolean isValidExtraType(Intent intent, String key, Class type) {
        Parcelable extra = intent.getParcelableExtra(key);
        return extra == null || type.isInstance(extra);
    }

    private boolean updateContactShortcut(Context context, Intent data, PendingInstallShortcutInfo info) {
        String restored = data.getStringExtra("SEC_CONTACT_SHORTCUT_RESTORED");
        if (restored == null) {
            return false;
        }
        String[] splits = restored.split(",", 2);
        Log.d(TAG, "updateContactShortcut, restored : " + restored);
        if (!"RESTORED".equals(splits[0])) {
            return false;
        }
        try {
            int restoreId = Integer.parseInt(splits[1]);
            if ("com.android.contacts".equals(info.mLaunchIntent.getData().getAuthority())) {
                boolean result = false;
                ContentValues values = new ContentValues();
                values.put("intent", info.mLaunchIntent.toUri(0));
                Uri uri = Favorites.CONTENT_URI;
                String selection = "_id=? AND title=? AND intent like ?";
                String[] selectionArgs = new String[]{Integer.toString(restoreId), info.mLabel, "%com.android.contacts%"};
                Log.d(TAG, "id : " + restoreId + " label : " + info.mLabel);
                if (context.getContentResolver().update(uri, values, selection, selectionArgs) > 0) {
                    Log.e(TAG, "updateContactShortcut restoreId : " + restoreId);
                    LauncherAppState app = LauncherAppState.getInstance();
                    long j = (long) restoreId;
                    app.getModel().getHomeLoader().updateContactShortcutInfo(j, new Intent(info.mLaunchIntent));
                    result = true;
                }
                if (LauncherFeature.supportEasyModeChange() && !result) {
                    Log.d(TAG, "updateContactShortcut check easy mode");
                    if (LauncherAppState.getInstance().isEasyModeEnabled()) {
                        uri = Favorites_Standard.CONTENT_URI;
                    } else {
                        uri = Favorites_Easy.CONTENT_URI;
                    }
                    if (context.getContentResolver().update(uri, values, selection, selectionArgs) > 0) {
                        Log.e(TAG, "updateContactShortcut restoreId : " + restoreId);
                        result = true;
                    }
                }
                if (LauncherFeature.supportHomeModeChange() && !result) {
                    Log.d(TAG, "updateContactShortcut check home only");
                    if (context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).getBoolean(LauncherAppState.HOME_ONLY_MODE, false)) {
                        uri = Favorites_HomeApps.CONTENT_URI;
                    } else {
                        uri = Favorites_HomeOnly.CONTENT_URI;
                    }
                    if (context.getContentResolver().update(uri, values, selection, selectionArgs) > 0) {
                        Log.e(TAG, "updateContactShortcut restoreId : " + restoreId);
                        result = true;
                    }
                }
                SharedPreferences prefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
                Set<String> contactShortcuts = prefs.getStringSet(Utilities.CONTACT_SHORTCUT_IDS, null);
                if (contactShortcuts != null && contactShortcuts.size() > 0) {
                    contactShortcuts.remove(Integer.toString(restoreId));
                    if (contactShortcuts.isEmpty()) {
                        Editor editor = prefs.edit();
                        int restore_result = prefs.getInt(Utilities.SMARTSWITCH_RESTORE_RESULT, 0);
                        int restore_errCode = prefs.getInt(Utilities.SMARTSWITCH_RESTORE_ERROR_CODE, 0);
                        int restore_file_length = prefs.getInt(Utilities.SMARTSWITCH_SAVE_FILE_LENGTH, 0);
                        String restore_source = prefs.getString(Utilities.SMARTSWITCH_RESTORE_SOURCE, "");
                        Log.d(TAG, "All contact shortcuts have been restored. send restore complete broadcast " + restore_result);
                        editor.remove(Utilities.CONTACT_SHORTCUT_IDS);
                        editor.remove(Utilities.SMARTSWITCH_RESTORE_RESULT);
                        editor.remove(Utilities.SMARTSWITCH_RESTORE_ERROR_CODE);
                        editor.remove(Utilities.SMARTSWITCH_SAVE_FILE_LENGTH);
                        editor.remove(Utilities.SMARTSWITCH_RESTORE_SOURCE);
                        editor.apply();
                        Intent restoreResult = new Intent(SmartSwitchBnr.RESPONSE_RESTORE_HOMESCREEN);
                        restoreResult.putExtra("RESULT", restore_result);
                        restoreResult.putExtra("ERR_CODE", restore_errCode);
                        restoreResult.putExtra("REQ_SIZE", restore_file_length);
                        restoreResult.putExtra("SOURCE", restore_source);
                        context.sendBroadcast(restoreResult);
                    } else {
                        Log.d(TAG, "remain contact shortcut that will restored : " + contactShortcuts.size());
                        prefs.edit().putStringSet(Utilities.CONTACT_SHORTCUT_IDS, contactShortcuts).apply();
                    }
                }
                if (!result) {
                    Log.d(TAG, "This contacts shortcut info is not match with DB");
                }
            } else {
                Log.e(TAG, "updateContactShortcut failed, not have AUTHORITY");
            }
            return true;
        } catch (NumberFormatException e) {
            return true;
        }
    }
}

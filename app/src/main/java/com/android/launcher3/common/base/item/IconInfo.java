package com.android.launcher3.common.base.item;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutKey;
import com.android.launcher3.util.BitmapUtils;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.ShortcutTray;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class IconInfo extends ItemInfo {
    public static final int DEFAULT = 0;
    public static final int DOWNLOADED_FLAG = 1;
    public static final int FLAG_AUTOINTALL_ICON = 2;
    private static final int FLAG_DISABLED_BY_PUBLISHER = 64;
    public static final int FLAG_DISABLED_EXTERNAL_STORAGE = 32;
    public static final int FLAG_DISABLED_NOT_AVAILABLE = 2;
    public static final int FLAG_DISABLED_QUIET_USER = 8;
    public static final int FLAG_DISABLED_SAFEMODE = 1;
    public static final int FLAG_DISABLED_SUSPENDED = 4;
    public static final int FLAG_INSTALL_SESSION_ACTIVE = 8;
    public static final int FLAG_OMC_RESTORED_ICON = 32;
    public static final int FLAG_RESTORED_ICON = 1;
    public static final int FLAG_RESTORE_STARTED = 16;
    public static final int FLAG_SCLOUD_RESTORED_ICON = 4;
    public static final int FLAG_SMARTSWITCH_RESTORED_ICON = 64;
    private static final int UPDATED_SYSTEM_APP_FLAG = 2;
    public boolean customIcon;
    public CharSequence disabledMessage;
    public long firstInstallTime;
    public int flags;
    public ShortcutIconResource iconResource;
    public Intent intent;
    public boolean isAppShortcut;
    public boolean isAppsButton;
    public int isDisabled;
    public Bitmap mIcon;
    private int mInstallProgress;
    private Bitmap mOriginalIcon;
    public Intent promisedIntent;
    public int status;
    public boolean usingFallbackIcon;
    public boolean usingLowResIcon;

    public IconInfo() {
        this.isDisabled = 0;
        this.flags = 0;
        this.isAppsButton = false;
        this.isAppShortcut = false;
    }

    public IconInfo(Intent intent, CharSequence title, CharSequence contentDescription, Bitmap icon, UserHandleCompat user) {
        this();
        this.intent = intent;
        this.title = Utilities.trim(title);
        this.contentDescription = contentDescription;
        this.mIcon = icon;
        this.user = user;
    }

    public IconInfo(Context context, IconInfo info) {
        super(info);
        this.isDisabled = 0;
        this.flags = 0;
        this.isAppsButton = false;
        this.isAppShortcut = false;
        this.title = Utilities.trim(info.title);
        this.intent = new Intent(info.intent);
        if (info.iconResource != null) {
            this.iconResource = new ShortcutIconResource();
            this.iconResource.packageName = info.iconResource.packageName;
            this.iconResource.resourceName = info.iconResource.resourceName;
        }
        this.customIcon = info.customIcon;
        this.flags = info.flags;
        this.firstInstallTime = info.firstInstallTime;
        this.user = info.user;
        this.status = info.status;
        this.isDisabled = info.isDisabled;
    }

    public IconInfo(Context context, LauncherActivityInfoCompat info, UserHandleCompat user, IconCache iconCache, boolean quietModeEnabled) {
        this.isDisabled = 0;
        this.flags = 0;
        this.isAppsButton = false;
        this.isAppShortcut = false;
        this.componentName = info.getComponentName();
        this.container = -1;
        this.flags = initFlags(info);
        this.firstInstallTime = info.getFirstInstallTime();
        if (Utilities.isAppSuspended(info.getApplicationInfo())) {
            this.isDisabled |= 4;
        }
        if (quietModeEnabled) {
            this.isDisabled |= 8;
        }
        iconCache.getTitleAndIcon(this, info, false);
        this.intent = makeLaunchIntent(context, info, user);
        this.user = user;
    }

    public IconInfo(Context context, LauncherActivityInfoCompat info, UserHandleCompat user, IconCache iconCache) {
        this(context, info, user, iconCache, null);
    }

    public IconInfo(Context context, LauncherActivityInfoCompat info, UserHandleCompat user, IconCache iconCache, Intent shortcutIntent) {
        this.isDisabled = 0;
        this.flags = 0;
        this.isAppsButton = false;
        this.isAppShortcut = false;
        this.componentName = info.getComponentName();
        this.container = -1;
        this.flags = initFlags(info);
        this.firstInstallTime = info.getFirstInstallTime();
        this.title = iconCache != null ? iconCache.getPackageItemTitle(info) : info.getLabel();
        if (Utilities.isAppSuspended(info.getApplicationInfo())) {
            this.isDisabled |= 4;
        }
        if (shortcutIntent != null) {
            this.intent = makeLaunchIntent(context, shortcutIntent, user);
        } else {
            this.intent = makeLaunchIntent(context, info, user);
        }
        this.user = user;
    }

    public IconInfo(long id, ComponentName cmp, long container, long screenRank, UserHandleCompat user) {
        this.isDisabled = 0;
        this.flags = 0;
        this.isAppsButton = false;
        this.isAppShortcut = false;
        this.id = id;
        this.componentName = cmp;
        this.container = container;
        this.screenId = screenRank;
        this.user = user;
        // TODO: Fix intent flags
        // this.intent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER").setComponent(this.componentName).setFlags(270532608);
    }

    @TargetApi(25)
    public IconInfo(ShortcutInfoCompat shortcutInfo, Context context) {
        this.isDisabled = 0;
        this.flags = 0;
        this.isAppsButton = false;
        this.isAppShortcut = false;
        this.user = shortcutInfo.getUserHandle();
        this.itemType = 6;
        this.flags = 0;
        updateFromDeepShortcutInfo(shortcutInfo, context);
    }

    public void updateFromDeepShortcutInfo(ShortcutInfoCompat shortcutInfo, Context context) {
        this.intent = shortcutInfo.makeIntent(context);
        this.title = shortcutInfo.getShortLabel();
        CharSequence label = shortcutInfo.getLongLabel();
        if (TextUtils.isEmpty(label)) {
            label = shortcutInfo.getShortLabel();
        }
        LauncherActivityInfoCompat activityInfo = getActivityInfo(shortcutInfo, context);
        if (activityInfo != null && Utilities.isAppSuspended(activityInfo.getApplicationInfo())) {
            this.isDisabled |= 4;
        }
        this.contentDescription = UserManagerCompat.getInstance(context).getBadgedLabelForUser(label, this.user);
        if (shortcutInfo.isEnabled()) {
            this.isDisabled &= -65;
        } else {
            this.isDisabled |= 64;
        }
        this.disabledMessage = shortcutInfo.getDisabledMessage();
        setBadgedIcon(context, shortcutInfo);
    }

    public void updateDeepShortcutIcon(Context context) {
        setBadgedIcon(context, LauncherAppState.getInstance().getShortcutManager().queryForShortcutKey(ShortcutKey.fromIntent(this.intent, this.user)));
    }

    @TargetApi(25)
    private LauncherActivityInfoCompat getActivityInfo(ShortcutInfoCompat shortcutInfo, Context context) {
        return LauncherAppsCompat.getInstance(context).resolveActivity(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER").setComponent(shortcutInfo.getShortcutInfo().getActivity()), shortcutInfo.getUserHandle());
    }

    public void setIcon(Bitmap b) {
        this.mIcon = b;
    }

    private void setBadgedIcon(Context context, ShortcutInfoCompat shortcutInfo) {
        if (shortcutInfo != null) {
            Bitmap unbadgedBitmap;
            LauncherAppState launcherAppState = LauncherAppState.getInstance();
            Drawable unbadgedDrawable = launcherAppState.getShortcutManager().getShortcutIconDrawable(shortcutInfo);
            IconInfo appInfo = new IconInfo();
            appInfo.user = this.user;
            appInfo.componentName = shortcutInfo.getActivity();
            IconCache cache = launcherAppState.getIconCache();
            if (unbadgedDrawable == null) {
                unbadgedBitmap = cache.getDefaultIcon(UserHandleCompat.myUserHandle());
            } else {
                unbadgedBitmap = ShortcutTray.getIcon(context, BitmapUtils.createIconBitmap(unbadgedDrawable, context), appInfo.componentName, true);
            }
            try {
                cache.getTitleAndIcon(appInfo, this.intent, this.user, false);
                setIcon(BitmapUtils.badgeWithBitmap(unbadgedBitmap, appInfo.mIcon, context));
            } catch (NullPointerException e) {
                setIcon(BitmapUtils.badgeIconForUser(unbadgedBitmap, this.user, context));
            }
        }
    }

    public Bitmap getIcon(IconCache iconCache) {
        if (this.mIcon == null && iconCache != null) {
            this.mIcon = iconCache.getIcon(this.intent, this.user);
        }
        return this.mIcon;
    }

    public void setOriginalIcon(Bitmap b) {
        this.mOriginalIcon = b;
    }

    public Bitmap getOriginalIcon() {
        return this.mOriginalIcon;
    }

    public Intent getIntent() {
        return this.intent;
    }

    public void updateIcon(IconCache iconCache) {
        updateIcon(iconCache, shouldUseLowResIcon());
    }

    public void updateIcon(IconCache iconCache, boolean useLowRes) {
        if (this.itemType == 0) {
            iconCache.getTitleAndIcon(this, this.promisedIntent != null ? this.promisedIntent : this.intent, this.user, useLowRes);
        }
    }

    public void updateTitleAndIcon(IconCache iconCache) {
        if (this.itemType == 0) {
            iconCache.getTitleAndIcon(this, this.promisedIntent != null ? this.promisedIntent : this.intent, this.user, shouldUseLowResIcon());
        }
    }

    public void onAddToDatabase(Context context, ContentValues values) {
        String titleStr;
        super.onAddToDatabase(context, values);
        if (this.title != null) {
            titleStr = this.title.toString();
        } else {
            titleStr = null;
        }
        values.put("title", titleStr);
        String uri = this.promisedIntent != null ? this.promisedIntent.toUri(0) : this.intent != null ? this.intent.toUri(0) : null;
        values.put("intent", uri);
        values.put("restored", Integer.valueOf(this.status));
        if (this.iconResource == null) {
            values.put(BaseLauncherColumns.ICON_TYPE, Integer.valueOf(1));
        } else {
            values.put(BaseLauncherColumns.ICON_TYPE, Integer.valueOf(0));
            values.put("iconPackage", this.iconResource.packageName);
            values.put("iconResource", this.iconResource.resourceName);
        }
        if (this.customIcon || this.isAppShortcut || this.itemType == 1) {
            if (this.mOriginalIcon != null) {
                ItemInfo.writeBitmap(values, this.mOriginalIcon);
            } else {
                ItemInfo.writeBitmap(values, this.mIcon);
            }
        } else if (this.itemType == 0) {
            values.put("icon", (byte[]) null);
        }
    }

    public boolean shouldUseLowResIcon() {
        return this.usingLowResIcon && this.container >= 0 && this.rank >= 9;
    }

    public ComponentName getTargetComponent() {
        return this.promisedIntent != null ? this.promisedIntent.getComponent() : this.intent.getComponent();
    }

    public boolean hasStatusFlag(int flag) {
        return (this.status & flag) != 0;
    }

    public final boolean isPromise() {
        return hasStatusFlag(103);
    }

    public int getInstallProgress() {
        return this.mInstallProgress;
    }

    public void setInstallProgress(int progress) {
        this.mInstallProgress = progress;
        this.status |= 8;
    }

    public static IconInfo fromActivityInfo(LauncherActivityInfoCompat info, Context context) {
        IconInfo iconInfo = new IconInfo();
        iconInfo.user = info.getUser();
        iconInfo.componentName = info.getComponentName();
        CharSequence title = getCSCPackageItemText(context, iconInfo.componentName);
        if (title == null) {
            title = info.getLabel();
        }
        iconInfo.title = Utilities.trim(title);
        iconInfo.contentDescription = UserManagerCompat.getInstance(context).getBadgedLabelForUser(iconInfo.title, info.getUser());
        iconInfo.customIcon = false;
        iconInfo.intent = makeLaunchIntent(context, info, info.getUser());
        iconInfo.itemType = 0;
        iconInfo.flags = initFlags(info);
        iconInfo.firstInstallTime = info.getFirstInstallTime();
        if (Utilities.isAppSuspended(info.getApplicationInfo())) {
            iconInfo.isDisabled |= 4;
        }
        if (UserManagerCompat.getInstance(context).isQuietModeEnabled(iconInfo.user)) {
            iconInfo.isDisabled |= 8;
        }
        return iconInfo;
    }

    public IconInfo makeCloneInfo() {
        Intent intent = null;
        IconInfo info = new IconInfo();
        info.copyFrom(this);
        info.container = -1;
        info.id = -1;
        info.mIcon = this.mIcon;
        info.intent = this.intent != null ? new Intent(this.intent) : null;
        info.title = this.title;
        info.status = this.status;
        info.flags = this.flags;
        if (this.promisedIntent != null) {
            intent = new Intent(this.promisedIntent);
        }
        info.promisedIntent = intent;
        info.mBadgeCount = this.mBadgeCount;
        info.isDisabled = this.isDisabled;
        info.mShowBadge = this.mShowBadge;
        return info;
    }

    public ComponentKey toComponentKey() {
        return new ComponentKey(this.componentName, this.user);
    }

    public static Intent makeLaunchIntent(Context context, LauncherActivityInfoCompat info, UserHandleCompat user) {
        return makeLaunchIntent(info.getComponentName(), UserManagerCompat.getInstance(context).getSerialNumberForUser(user));
    }

    public static Intent makeLaunchIntent(Context context, Intent intent, UserHandleCompat user) {
        return makeLaunchIntent(intent.getComponent(), UserManagerCompat.getInstance(context).getSerialNumberForUser(user));
    }

    public static Intent makeLaunchIntent(ComponentName cn, long serialNumber) {
        return new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER").setComponent(cn).setFlags(270532608).putExtra(ItemInfo.EXTRA_PROFILE, serialNumber);
    }

    public static int initFlags(LauncherActivityInfoCompat info) {
        int appFlags = info.getApplicationInfo().flags;
        if ((appFlags & 1) != 0) {
            return 0;
        }
        int flags = 0 | 1;
        if ((appFlags & 128) != 0) {
            return flags | 2;
        }
        return flags;
    }

    private static CharSequence getCSCPackageItemText(Context context, ComponentName componentName) {
        PackageManager packageManager = context.getPackageManager();
        CharSequence text = null;
        // TODO: Samsung specific code
//        text = packageManager.semGetCscPackageItemText(componentName.getClassName());
//        if (TextUtils.isEmpty(text)) {
//            return packageManager.semGetCscPackageItemText(componentName.getPackageName());
//        }
        return text;
    }

    public Intent getPromisedIntent() {
        return this.promisedIntent != null ? this.promisedIntent : this.intent;
    }

    public String getDeepShortcutId() {
        return this.itemType == 6 ? getPromisedIntent().getStringExtra(ShortcutInfoCompat.EXTRA_SHORTCUT_ID) : null;
    }

    public String toString() {
        return "IconInfo(title=" + this.title + " intent=" + this.intent + " id=" + this.id + " type=" + this.itemType + " container=" + this.container + " screen=" + this.screenId + " cellX=" + this.cellX + " cellY=" + this.cellY + " spanX=" + this.spanX + " spanY=" + this.spanY + " rank=" + this.rank + " hidden=" + this.hidden + " dropPos=" + Arrays.toString(this.dropPos) + " user=" + this.user + ")";
    }

    public static void dumpIconInfoList(String tag, String label, ArrayList<IconInfo> list) {
        Log.d(tag, label + " size=" + list.size());
        Iterator it = list.iterator();
        while (it.hasNext()) {
            IconInfo info = (IconInfo) it.next();
            Log.d(tag, "   title=\"" + info.title + "\" iconBitmap=" + info.mIcon + " customIcon=" + info.customIcon + " firstInstallTime=" + info.firstInstallTime + " componentName=" + info.componentName.getPackageName());
        }
    }
}

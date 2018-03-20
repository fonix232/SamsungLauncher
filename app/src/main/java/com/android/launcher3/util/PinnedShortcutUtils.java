package com.android.launcher3.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.os.PersistableBundle;
import android.util.Log;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.quickoption.shortcuts.DeepShortcutManager;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutCache;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutKey;
import com.android.launcher3.widget.PendingAddPinShortcutInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public final class PinnedShortcutUtils {
    private static final String EDM_PACKAGE_NAME = "com.android.server.enterprise.application.ApplicationPolicy";
    private static final String EMD_PACKAGE_KEY = "do_not_show_popup";
    private static final String TAG = "PinnedShortcutUtils";

    public static void acceptPinItemInfo(DragObject d, ItemInfo info, boolean isAcceptItem) {
        if (info.itemType == 6 && !isAcceptItem && (d.dragInfo instanceof PendingAddPinShortcutInfo)) {
            ((PendingAddPinShortcutInfo) d.dragInfo).getShortcutInfo().accept(false);
        }
    }

    public static void unpinShortcutIfAppTarget(ShortcutInfoCompat compat, Context context) {
        DeepShortcutManager shortcutManager = new DeepShortcutManager(context, new ShortcutCache());
        List<ShortcutInfoCompat> shortcutInfoCompatList = shortcutManager.queryForFullDetails(compat.getPackage(), Arrays.asList(new String[]{compat.getId()}), compat.getUserHandle());
        if (!shortcutInfoCompatList.isEmpty() && Utilities.isLauncherAppTarget(((ShortcutInfoCompat) shortcutInfoCompatList.get(0)).getShortcutInfo().getIntent())) {
            shortcutManager.unpinShortcut(ShortcutKey.fromInfo((ShortcutInfoCompat) shortcutInfoCompatList.get(0)));
        }
    }

    public static boolean isRequestFromEDM(ShortcutInfo info, Intent intent) {
        PersistableBundle bundle = info.getExtras();
        if (bundle != null && bundle.get(EMD_PACKAGE_KEY) != null) {
            return EDM_PACKAGE_NAME.contains((String) bundle.get(EMD_PACKAGE_KEY));
        }
        if (intent.getPackage() == null || intent.getPackage().equals(info.getPackage())) {
            return false;
        }
        return true;
    }

    public static boolean shortcutExists(Context context, Intent intent, UserHandleCompat user) {
        Launcher launcher = (Launcher) context;
        if (!Utilities.isLauncherAppTarget(intent)) {
            return false;
        }
        String intentWithPkg;
        String intentWithoutPkg;
        if (intent.getComponent() == null) {
            intentWithPkg = intent.toUri(0);
            intentWithoutPkg = intent.toUri(0);
        } else if (intent.getPackage() != null) {
            intentWithPkg = intent.toUri(0);
            intentWithoutPkg = new Intent(intent).setPackage(null).toUri(0);
        } else {
            intentWithPkg = new Intent(intent).setPackage(intent.getComponent().getPackageName()).toUri(0);
            intentWithoutPkg = intent.toUri(0);
        }
        if (intentWithPkg == null || intentWithoutPkg == null) {
            return false;
        }
        ArrayList<ItemInfo> itemList = new ArrayList();
        if (!(launcher == null || launcher.getLauncherModel() == null || launcher.getLauncherModel().getHomeLoader() == null)) {
            itemList = launcher.getLauncherModel().getHomeLoader().getAllItemInHome();
        }
        Iterator it = itemList.iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            if (item instanceof IconInfo) {
                IconInfo info = (IconInfo) item;
                Intent targetIntent = info.promisedIntent == null ? info.intent : info.promisedIntent;
                if (!(targetIntent == null || user == null || !user.equals(info.user))) {
                    String strIntent = targetIntent.toUri(0);
                    if (intentWithPkg.equals(strIntent) || intentWithoutPkg.equals(strIntent)) {
                        Log.d(TAG, "shortcutExists : " + info.toString());
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

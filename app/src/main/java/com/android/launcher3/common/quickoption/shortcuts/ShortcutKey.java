package com.android.launcher3.common.quickoption.shortcuts;

import android.content.ComponentName;
import android.content.Intent;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.util.ComponentKey;

public class ShortcutKey extends ComponentKey {
    private ShortcutKey(String packageName, UserHandleCompat user, String id) {
        super(new ComponentName(packageName, id), user);
    }

    public String getId() {
        return this.componentName.getClassName();
    }

    public static ShortcutKey fromInfo(ShortcutInfoCompat shortcutInfo) {
        return new ShortcutKey(shortcutInfo.getPackage(), shortcutInfo.getUserHandle(), shortcutInfo.getId());
    }

    public static ShortcutKey fromIntent(Intent intent, UserHandleCompat user) {
        return new ShortcutKey(intent.getPackage(), user, intent.getStringExtra(ShortcutInfoCompat.EXTRA_SHORTCUT_ID));
    }

    public static ShortcutKey fromShortcutInfo(IconInfo info) {
        return fromIntent(info.getPromisedIntent(), info.user);
    }
}

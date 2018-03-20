package com.android.launcher3.util;

import android.content.ComponentName;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutKey;
import java.util.HashSet;

public abstract class ItemInfoMatcher {
    public abstract boolean matches(ItemInfo itemInfo, ComponentName componentName);

    public static ItemInfoMatcher ofComponents(final HashSet<ComponentName> components, final UserHandleCompat user) {
        return new ItemInfoMatcher() {
            public boolean matches(ItemInfo info, ComponentName cn) {
                return components.contains(cn) && info.user.equals(user);
            }
        };
    }

    public static ItemInfoMatcher ofPackages(final HashSet<String> packageNames, final UserHandleCompat user) {
        return new ItemInfoMatcher() {
            public boolean matches(ItemInfo info, ComponentName cn) {
                return packageNames.contains(cn.getPackageName()) && info.user.equals(user);
            }
        };
    }

    public static ItemInfoMatcher ofShortcutKeys(final HashSet<ShortcutKey> keys) {
        return new ItemInfoMatcher() {
            public boolean matches(ItemInfo info, ComponentName cn) {
                return info.itemType == 6 && keys.contains(ShortcutKey.fromShortcutInfo((IconInfo) info));
            }
        };
    }
}

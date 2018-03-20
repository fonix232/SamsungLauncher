package com.android.launcher3.common.quickoption.shortcuts;

import android.annotation.TargetApi;
import android.util.LruCache;
import java.util.HashMap;
import java.util.List;

@TargetApi(24)
public class ShortcutCache {
    private static final int CACHE_SIZE = 30;
    private final LruCache<ShortcutKey, ShortcutInfoCompat> mCachedShortcuts = new LruCache(30);
    private final HashMap<ShortcutKey, ShortcutInfoCompat> mPinnedShortcuts = new HashMap();

    public void removeShortcuts(List<ShortcutInfoCompat> shortcuts) {
        for (ShortcutInfoCompat shortcut : shortcuts) {
            ShortcutKey key = ShortcutKey.fromInfo(shortcut);
            this.mCachedShortcuts.remove(key);
            this.mPinnedShortcuts.remove(key);
        }
    }

    public ShortcutInfoCompat get(ShortcutKey key) {
        if (this.mPinnedShortcuts.containsKey(key)) {
            return (ShortcutInfoCompat) this.mPinnedShortcuts.get(key);
        }
        return (ShortcutInfoCompat) this.mCachedShortcuts.get(key);
    }

    public void put(ShortcutKey key, ShortcutInfoCompat shortcut) {
        if (shortcut.isPinned()) {
            this.mPinnedShortcuts.put(key, shortcut);
        } else {
            this.mCachedShortcuts.put(key, shortcut);
        }
    }
}

package com.android.launcher3.common.quickoption.shortcuts;

import android.support.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ShortcutFilter {
    static final int MAX_SHORTCUTS = 5;
    @VisibleForTesting
    private static final int NUM_DYNAMIC = 2;
    private static final Comparator<ShortcutInfoCompat> RANK_COMPARATOR = new Comparator<ShortcutInfoCompat>() {
        public int compare(ShortcutInfoCompat a, ShortcutInfoCompat b) {
            if (a.isDeclaredInManifest() && !b.isDeclaredInManifest()) {
                return -1;
            }
            if (a.isDeclaredInManifest() || !b.isDeclaredInManifest()) {
                return Integer.compare(a.getRank(), b.getRank());
            }
            return 1;
        }
    };

    public static List<ShortcutInfoCompat> sortAndFilterShortcuts(List<ShortcutInfoCompat> shortcuts) {
        Collections.sort(shortcuts, RANK_COMPARATOR);
        if (shortcuts.size() <= 5) {
            return shortcuts;
        }
        List<ShortcutInfoCompat> filteredShortcuts = new ArrayList(5);
        int numDynamic = 0;
        int size = shortcuts.size();
        for (int i = 0; i < size; i++) {
            ShortcutInfoCompat shortcut = (ShortcutInfoCompat) shortcuts.get(i);
            int filteredSize = filteredShortcuts.size();
            if (filteredSize < 5) {
                filteredShortcuts.add(shortcut);
                if (shortcut.isDynamic()) {
                    numDynamic++;
                }
            } else if (shortcut.isDynamic() && numDynamic < 2) {
                numDynamic++;
                filteredShortcuts.remove(filteredSize - numDynamic);
                filteredShortcuts.add(shortcut);
            }
        }
        return filteredShortcuts;
    }
}

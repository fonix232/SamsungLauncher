package com.android.launcher3.util;

import java.util.Set;

public abstract class StringFilter {
    public abstract boolean matches(String str);

    private StringFilter() {
    }

    public static StringFilter matchesAll() {
        return new StringFilter() {
            public boolean matches(String str) {
                return true;
            }
        };
    }

    public static StringFilter of(final Set<String> validEntries) {
        return new StringFilter() {
            public boolean matches(String str) {
                return validEntries.contains(str);
            }
        };
    }
}

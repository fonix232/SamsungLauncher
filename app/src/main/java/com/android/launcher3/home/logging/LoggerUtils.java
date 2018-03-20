package com.android.launcher3.home.logging;

import android.util.ArrayMap;
import android.util.SparseArray;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

class LoggerUtils {
    private static final String UNKNOWN = "UNKNOWN";
    private static final ArrayMap<Class, SparseArray<String>> sNameCache = new ArrayMap();

    LoggerUtils() {
    }

    static String getFieldName(int value, Class c) {
        SparseArray<String> cache;
        synchronized (sNameCache) {
            cache = (SparseArray) sNameCache.get(c);
            if (cache == null) {
                cache = new SparseArray();
                for (Field f : c.getDeclaredFields()) {
                    if (f.getType() == Integer.TYPE && Modifier.isStatic(f.getModifiers())) {
                        try {
                            f.setAccessible(true);
                            cache.put(f.getInt(null), f.getName());
                        } catch (IllegalAccessException e) {
                        }
                    }
                }
                sNameCache.put(c, cache);
            }
        }
        String result = (String) cache.get(value);
        return result != null ? result : UNKNOWN;
    }
}

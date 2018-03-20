package com.android.launcher3.common.model;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.samsung.android.knox.SemPersonaManager;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class BadgeCache {
    private static final String[] BADGE_COLUMNS = new String[]{"package", "class", "badgecount"};
    private static final String[] BADGE_MANAGE_COLUMNS = new String[]{"package", "class", "badgecount", "hidden"};
    public static final Uri BADGE_URI = Uri.parse("content://com.sec.badge/apps");
    private static final boolean DEBUGGABLE = true;
    private static final int INITIAL_BADGE_CAPACITY = 20;
    private static final String TAG = "BadgeCache";
    private static final Integer ZERO = Integer.valueOf(0);
    private final Map<CacheKey, Integer> mBadges = new HashMap(20);
    private final Context mContext;

    public static class CacheKey {
        public ComponentName componentName;
        public UserHandleCompat user;

        public CacheKey(ComponentName componentName, UserHandleCompat user) {
            this.componentName = componentName;
            this.user = user;
        }

        public int hashCode() {
            return (this.user == null ? 0 : this.user.hashCode()) + this.componentName.hashCode();
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof CacheKey)) {
                return false;
            }
            CacheKey other = (CacheKey) o;
            if (other.componentName.equals(this.componentName) && other.user.equals(this.user)) {
                return true;
            }
            return false;
        }
    }

    public BadgeCache(Context context) {
        this.mContext = context;
    }

    public int getBadgeCount(CacheKey cacheKey) {
        Integer value = (Integer) this.mBadges.get(cacheKey);
        if (value != null) {
            return value.intValue();
        }
        return 0;
    }

    private static boolean isKnoxIdExceptSecurFolder(int userId) {
        if (SemPersonaManager.isSecureFolderId(userId)) {
            return false;
        }
        return SemPersonaManager.isKnoxId(userId);
    }

    public static Uri maybeAddUserId(Uri uri, int userId) {
        ReflectiveOperationException e;
        try {
            return (Uri) ContentProvider.class.getMethod("maybeAddUserId", new Class[]{Uri.class, Integer.TYPE}).invoke(null, new Object[]{uri, Integer.valueOf(userId)});
        } catch (InvocationTargetException e2) {
            e = e2;
        } catch (IllegalAccessException e3) {
            e = e3;
        } catch (NoSuchMethodException e4) {
            e = e4;
        }
        Log.e(TAG, "Failed to invoke : " + e.getMessage());
        return null;
    }

    public Map<CacheKey, Integer> updateBadgeCounts() {
        for (Entry<CacheKey, Integer> entry : this.mBadges.entrySet()) {
            entry.setValue(ZERO);
        }
        Map<CacheKey, Integer> allBadges = Collections.emptyMap();
        for (UserHandleCompat user : UserManagerCompat.getInstance(this.mContext).getUserProfiles()) {
            Map<CacheKey, Integer> tempBadges = Collections.unmodifiableMap(updateBadgeCounts(user));
            if (tempBadges.size() > 0) {
                allBadges = tempBadges;
            }
            Log.d(TAG, "updateBadgeCounts(), tempBadges.size() : [" + tempBadges.size() + "], user : [" + user + "]");
        }
        Log.d(TAG, "updateBadgeCounts(), final size : " + allBadges.size());
        return allBadges;
    }

    Map<CacheKey, Integer> updateBadgeCounts(UserHandleCompat user) {
        Uri badgeUri = BADGE_URI;
        Map<CacheKey, Integer> badges = Collections.emptyMap();
        if (!user.equals(UserHandleCompat.myUserHandle())) {
            badgeUri = maybeAddUserId(BADGE_URI, user.hashCode());
            if (badgeUri == null) {
                return badges;
            }
            badgeUri = badgeUri.buildUpon().appendQueryParameter("noMultiUser", String.valueOf(true)).build();
        }
        boolean supportBadgeManager = LauncherFeature.isSupportBadgeManage();
        int badgeSettings = LauncherAppState.getInstance().getBadgeSetings();
        Cursor c = null;
        try {
            c = this.mContext.getContentResolver().query(badgeUri, supportBadgeManager ? BADGE_MANAGE_COLUMNS : BADGE_COLUMNS, null, null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String pkgName = c.getString(0);
                    String className = c.getString(1);
                    int badgeCount = c.getInt(2);
                    if (supportBadgeManager) {
                        int hidden = c.getInt(3);
                        if (badgeSettings == 0 || hidden == 1) {
                            badgeCount = 0;
                        }
                    }
                    if (pkgName != null) {
                        Log.d(TAG, "1. updateBadgeCounts: " + pkgName + " = " + badgeCount);
                        if (className != null && (badgeCount > 0 || badgeCount == -1)) {
                            ComponentName cn = new ComponentName(pkgName, className);
                            this.mBadges.put(new CacheKey(cn, user), Integer.valueOf(badgeCount));
                            Log.d(TAG, "2. updateBadgeCounts: " + cn.flattenToShortString() + " = " + badgeCount);
                        }
                    }
                }
                badges = Collections.unmodifiableMap(this.mBadges);
            } else {
                Log.e(TAG, "updateBadgeCounts() failed to query");
            }
            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException e = " + e);
            return badges;
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return badges;
    }

    public int getBadgeCountFromBadgeProvider(String name, UserHandleCompat user) {
        if (!this.mBadges.isEmpty()) {
            for (Entry<CacheKey, Integer> entry : this.mBadges.entrySet()) {
                CacheKey cacheKey = (CacheKey) entry.getKey();
                if (cacheKey.componentName.getPackageName().equals(name) && cacheKey.user.equals(user)) {
                    Log.d(TAG, "getBadgeCountFromBadgeProvider(), cacheKey.componentName.getPackageName() : [" + cacheKey.componentName.getPackageName() + "], entry.getValue().intValue() :[" + ((Integer) entry.getValue()).intValue() + "]");
                    return ((Integer) entry.getValue()).intValue();
                }
            }
        }
        return -1;
    }
}

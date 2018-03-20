package com.android.launcher3.common.compat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.LongArrayMap;
import java.util.HashMap;
import java.util.List;

public class UserManagerCompatVL extends UserManagerCompatV17 {
    private static final String USER_CREATION_TIME_KEY = "user_creation_time_";
    private final Context mContext;
    private final PackageManager mPm;

    UserManagerCompatVL(Context context) {
        super(context);
        this.mPm = context.getPackageManager();
        this.mContext = context;
    }

    public void enableAndResetCache() {
        synchronized (this) {
            boolean isKnox = Utilities.isKnoxMode();
            this.mUsers = new LongArrayMap();
            this.mUserToSerialMap = new HashMap();
            List<UserHandle> users = this.mUserManager.getUserProfiles();
            if (users != null) {
                for (UserHandle user : users) {
                    if (!isKnox || UserHandleCompat.myUserHandle().getUser().equals(user)) {
                        long serial = this.mUserManager.getSerialNumberForUser(user);
                        UserHandleCompat userCompat = UserHandleCompat.fromUser(user);
                        this.mUsers.put(serial, userCompat);
                        this.mUserToSerialMap.put(userCompat, Long.valueOf(serial));
                    }
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.util.List<com.android.launcher3.common.compat.UserHandleCompat> getUserProfiles() {
        /*
        r7 = this;
        monitor-enter(r7);
        r5 = r7.mUsers;	 Catch:{ all -> 0x0023 }
        if (r5 == 0) goto L_0x0015;
    L_0x0005:
        r4 = new java.util.ArrayList;	 Catch:{ all -> 0x0023 }
        r4.<init>();	 Catch:{ all -> 0x0023 }
        r5 = r7.mUserToSerialMap;	 Catch:{ all -> 0x0023 }
        r5 = r5.keySet();	 Catch:{ all -> 0x0023 }
        r4.addAll(r5);	 Catch:{ all -> 0x0023 }
        monitor-exit(r7);	 Catch:{ all -> 0x0023 }
    L_0x0014:
        return r4;
    L_0x0015:
        monitor-exit(r7);	 Catch:{ all -> 0x0023 }
        r5 = r7.mUserManager;
        r3 = r5.getUserProfiles();
        if (r3 != 0) goto L_0x0026;
    L_0x001e:
        r4 = java.util.Collections.emptyList();
        goto L_0x0014;
    L_0x0023:
        r5 = move-exception;
        monitor-exit(r7);	 Catch:{ all -> 0x0023 }
        throw r5;
    L_0x0026:
        r1 = com.android.launcher3.Utilities.isKnoxMode();
        r0 = new java.util.ArrayList;
        r5 = r3.size();
        r0.<init>(r5);
        r5 = r3.iterator();
    L_0x0037:
        r6 = r5.hasNext();
        if (r6 == 0) goto L_0x005b;
    L_0x003d:
        r2 = r5.next();
        r2 = (android.os.UserHandle) r2;
        if (r1 == 0) goto L_0x0053;
    L_0x0045:
        r6 = com.android.launcher3.common.compat.UserHandleCompat.myUserHandle();
        r6 = r6.getUser();
        r6 = r6.equals(r2);
        if (r6 == 0) goto L_0x0037;
    L_0x0053:
        r6 = com.android.launcher3.common.compat.UserHandleCompat.fromUser(r2);
        r0.add(r6);
        goto L_0x0037;
    L_0x005b:
        r4 = r0;
        goto L_0x0014;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.compat.UserManagerCompatVL.getUserProfiles():java.util.List<com.android.launcher3.common.compat.UserHandleCompat>");
    }

    public Drawable getBadgedDrawableForUser(Drawable unbadged, UserHandleCompat user) {
        return this.mPm.getUserBadgedIcon(unbadged, user.getUser());
    }

    public CharSequence getBadgedLabelForUser(CharSequence label, UserHandleCompat user) {
        return user == null ? label : this.mPm.getUserBadgedLabel(label, user.getUser());
    }

    public long getUserCreationTime(UserHandleCompat user) {
        if (Utilities.ATLEAST_MARSHMALLOW) {
            return this.mUserManager.getUserCreationTime(user.getUser());
        }
        SharedPreferences prefs = this.mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        String key = USER_CREATION_TIME_KEY + getSerialNumberForUser(user);
        if (!prefs.contains(key)) {
            prefs.edit().putLong(key, System.currentTimeMillis()).apply();
        }
        return prefs.getLong(key, 0);
    }
}

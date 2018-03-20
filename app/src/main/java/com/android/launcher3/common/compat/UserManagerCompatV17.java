package com.android.launcher3.common.compat;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.UserManager;
import com.android.launcher3.util.LongArrayMap;
import java.util.HashMap;

@TargetApi(17)
public class UserManagerCompatV17 extends UserManagerCompatV16 {
    protected UserManager mUserManager;
    protected HashMap<UserHandleCompat, Long> mUserToSerialMap;
    protected LongArrayMap<UserHandleCompat> mUsers;

    UserManagerCompatV17(Context context) {
        this.mUserManager = (UserManager) context.getSystemService("user");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long getSerialNumberForUser(com.android.launcher3.common.compat.UserHandleCompat r5) {
        /*
        r4 = this;
        r2 = 0;
        if (r5 != 0) goto L_0x0005;
    L_0x0004:
        return r2;
    L_0x0005:
        monitor-enter(r4);
        r1 = r4.mUserToSerialMap;	 Catch:{ all -> 0x0016 }
        if (r1 == 0) goto L_0x001e;
    L_0x000a:
        r1 = r4.mUserToSerialMap;	 Catch:{ all -> 0x0016 }
        r0 = r1.get(r5);	 Catch:{ all -> 0x0016 }
        r0 = (java.lang.Long) r0;	 Catch:{ all -> 0x0016 }
        if (r0 != 0) goto L_0x0019;
    L_0x0014:
        monitor-exit(r4);	 Catch:{ all -> 0x0016 }
        goto L_0x0004;
    L_0x0016:
        r1 = move-exception;
        monitor-exit(r4);	 Catch:{ all -> 0x0016 }
        throw r1;
    L_0x0019:
        r2 = r0.longValue();	 Catch:{ all -> 0x0016 }
        goto L_0x0014;
    L_0x001e:
        monitor-exit(r4);	 Catch:{ all -> 0x0016 }
        r1 = r4.mUserManager;
        r2 = r5.getUser();
        r2 = r1.getSerialNumberForUser(r2);
        goto L_0x0004;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.compat.UserManagerCompatV17.getSerialNumberForUser(com.android.launcher3.common.compat.UserHandleCompat):long");
    }

    public UserHandleCompat getUserForSerialNumber(long serialNumber) {
        synchronized (this) {
            if (this.mUsers != null) {
                UserHandleCompat userHandleCompat = (UserHandleCompat) this.mUsers.get(serialNumber);
                return userHandleCompat;
            }
            return UserHandleCompat.fromUser(this.mUserManager.getUserForSerialNumber(serialNumber));
        }
    }

    public void enableAndResetCache() {
        synchronized (this) {
            this.mUsers = new LongArrayMap();
            this.mUserToSerialMap = new HashMap();
            UserHandleCompat myUser = UserHandleCompat.myUserHandle();
            long serial = this.mUserManager.getSerialNumberForUser(myUser.getUser());
            this.mUsers.put(serial, myUser);
            this.mUserToSerialMap.put(myUser, Long.valueOf(serial));
        }
    }
}

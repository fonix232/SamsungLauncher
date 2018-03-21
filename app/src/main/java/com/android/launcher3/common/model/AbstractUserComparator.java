package com.android.launcher3.common.model;

import android.content.Context;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import java.util.Comparator;

public abstract class AbstractUserComparator<T extends ItemInfo> implements Comparator<T> {
    private final UserHandleCompat mMyUser = UserHandleCompat.myUserHandle();
    private final UserManagerCompat mUserManager;

    public AbstractUserComparator(Context context) {
        this.mUserManager = UserManagerCompat.getInstance(context);
    }

    public int compare(T lhs, T rhs) {
        if (this.mMyUser.equals(lhs.user)) {
            return -1;
        }
        return Long.valueOf(this.mUserManager.getSerialNumberForUser(lhs.user)).compareTo(this.mUserManager.getSerialNumberForUser(rhs.user));
    }
}

package com.android.launcher3.common.compat;

import android.annotation.TargetApi;
import android.content.Context;

@TargetApi(24)
public class UserManagerCompatVN extends UserManagerCompatVL {
    UserManagerCompatVN(Context context) {
        super(context);
    }

    public boolean isQuietModeEnabled(UserHandleCompat user) {
        if (user != null) {
            try {
                return this.mUserManager.isQuietModeEnabled(user.getUser());
            } catch (IllegalArgumentException e) {
            }
        }
        return false;
    }
}

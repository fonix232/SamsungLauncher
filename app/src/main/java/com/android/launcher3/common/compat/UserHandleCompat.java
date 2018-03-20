package com.android.launcher3.common.compat;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Process;
import android.os.UserHandle;
import com.android.launcher3.Utilities;

public class UserHandleCompat {
    private UserHandle mUser;

    private UserHandleCompat(UserHandle user) {
        this.mUser = user;
    }

    private UserHandleCompat() {
    }

    @TargetApi(17)
    public static UserHandleCompat myUserHandle() {
        if (Utilities.ATLEAST_JB_MR1) {
            return new UserHandleCompat(Process.myUserHandle());
        }
        return new UserHandleCompat();
    }

    public static UserHandleCompat fromUser(UserHandle user) {
        if (user == null) {
            return null;
        }
        return new UserHandleCompat(user);
    }

    public UserHandle getUser() {
        return this.mUser;
    }

    public String toString() {
        if (Utilities.ATLEAST_JB_MR1) {
            return this.mUser.toString();
        }
        return "";
    }

    public boolean equals(Object other) {
        if (!(other instanceof UserHandleCompat)) {
            return false;
        }
        if (Utilities.ATLEAST_JB_MR1) {
            return this.mUser.equals(((UserHandleCompat) other).mUser);
        }
        return true;
    }

    public int hashCode() {
        if (Utilities.ATLEAST_JB_MR1) {
            return this.mUser.hashCode();
        }
        return 0;
    }

    public void addToIntent(Intent intent, String name) {
        if (Utilities.ATLEAST_LOLLIPOP && this.mUser != null) {
            intent.putExtra(name, this.mUser);
        }
    }

    public static UserHandleCompat fromIntent(Intent intent) {
        if (Utilities.ATLEAST_LOLLIPOP) {
            UserHandle user = (UserHandle) intent.getParcelableExtra("android.intent.extra.USER");
            if (user != null) {
                return fromUser(user);
            }
        }
        return null;
    }
}

package com.android.launcher3.util;

import android.content.ComponentName;
import android.content.Context;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import java.util.Arrays;

public class ComponentKey {
    static final /* synthetic */ boolean $assertionsDisabled = (!ComponentKey.class.desiredAssertionStatus());
    public final ComponentName componentName;
    private final int mHashCode;
    public final UserHandleCompat user;

    public ComponentKey(ComponentName componentName, UserHandleCompat user) {
        if (!$assertionsDisabled && componentName == null) {
            throw new AssertionError();
        } else if ($assertionsDisabled || user != null) {
            this.componentName = componentName;
            this.user = user;
            this.mHashCode = Arrays.hashCode(new Object[]{componentName, user});
        } else {
            throw new AssertionError();
        }
    }

    public ComponentKey(Context context, String componentKeyStr) {
        int userDelimiterIndex = componentKeyStr.indexOf("#");
        if (userDelimiterIndex != -1) {
            String componentStr = componentKeyStr.substring(0, userDelimiterIndex);
            long componentUser = Long.parseLong(componentKeyStr.substring(userDelimiterIndex + 1));
            this.componentName = ComponentName.unflattenFromString(componentStr);
            this.user = UserManagerCompat.getInstance(context).getUserForSerialNumber(componentUser);
        } else {
            this.componentName = ComponentName.unflattenFromString(componentKeyStr);
            this.user = UserHandleCompat.myUserHandle();
        }
        this.mHashCode = Arrays.hashCode(new Object[]{this.componentName, this.user});
    }

    public String flattenToString(Context context) {
        String flattened = this.componentName.flattenToString();
        if (this.user != null) {
            return flattened + "#" + UserManagerCompat.getInstance(context).getSerialNumberForUser(this.user);
        }
        return flattened;
    }

    public int hashCode() {
        return this.mHashCode;
    }

    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ComponentKey other = (ComponentKey) o;
        if (other.componentName == null || other.user == null || !other.componentName.equals(this.componentName) || !other.user.equals(this.user)) {
            return false;
        }
        return true;
    }
}

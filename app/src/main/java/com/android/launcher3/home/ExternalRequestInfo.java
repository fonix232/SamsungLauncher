package com.android.launcher3.home;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import com.android.launcher3.common.compat.UserHandleCompat;
import java.util.ArrayList;

abstract class ExternalRequestInfo {
    static final int INSTALL_PAIRAPPS = 5;
    static final int INSTALL_SHORTCUT = 1;
    static final int INSTALL_WIDGET = 3;
    static final String TYPE_KEY = "type";
    static final String TYPE_TIME = "time";
    static final int UNINSTALL_SHORTCUT = 2;
    static final int UNINSTALL_WIDGET = 4;
    String mLabel;
    Intent mLaunchIntent;
    long requestTime;
    final int requestType;
    protected UserHandleCompat user;

    abstract String encodeToString();

    abstract boolean getContainPackage(ArrayList<String> arrayList);

    abstract String getTargetPackage();

    abstract void runRequestInfo(Context context);

    ExternalRequestInfo(int type, UserHandleCompat userHandleCompat, long time) {
        this.requestType = type;
        if (time < 0) {
            time = SystemClock.uptimeMillis();
        }
        this.requestTime = time;
        if (userHandleCompat == null) {
            userHandleCompat = UserHandleCompat.myUserHandle();
        }
        this.user = userHandleCompat;
    }

    int getRequestType() {
        return this.requestType;
    }

    UserHandleCompat getUser() {
        return this.user;
    }

    boolean equals(ExternalRequestInfo info) {
        return this.requestTime == info.requestTime && this.requestType == info.requestType && this.user.equals(info.user);
    }
}

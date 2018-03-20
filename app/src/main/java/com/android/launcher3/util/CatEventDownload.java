package com.android.launcher3.util;

import android.content.Intent;
import android.os.Parcelable;

public class CatEventDownload {
    public static final int EVENT_IDLE_SCREEN_AVAILABLE = 5;
    public static final String STK_EVENT_ACTION = "com.samsung.intent.internal.stk.event";
    private static Class sClass;
    private Object instance = null;

    static {
        sClass = null;
        try {
            sClass = Class.forName("com.android.internal.telephony.cat.CatEventDownload");
        } catch (Exception e) {
            sClass = null;
        }
    }

    public CatEventDownload() {
        if (sClass != null) {
            try {
                this.instance = sClass.getConstructor(new Class[]{Integer.TYPE}).newInstance(new Object[]{Integer.valueOf(5)});
            } catch (Exception e) {
            }
        }
    }

    public void putExtra(Intent intent, String string) {
        if (this.instance != null) {
            intent.putExtra(string, (Parcelable) this.instance);
        }
    }
}

package com.samsung.context.sdk.samsunganalytics.p000a.p013i;

import android.content.Context;
import android.content.SharedPreferences;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.i.c */
public class C0801c {
    /* renamed from: a */
    public static final String f174a = "SamsungAnalyticsPrefs";
    /* renamed from: b */
    public static final String f175b = "SASettingPref";
    /* renamed from: c */
    public static final String f176c = "deviceId";
    /* renamed from: d */
    public static final String f177d = "auidType";
    /* renamed from: e */
    public static final String f178e = "AppPrefs";
    /* renamed from: f */
    public static final String f179f = "status_sent_date";

    private C0801c() {
    }

    /* renamed from: a */
    public static SharedPreferences m159a(Context context) {
        return context.getSharedPreferences(f174a, 0);
    }
}

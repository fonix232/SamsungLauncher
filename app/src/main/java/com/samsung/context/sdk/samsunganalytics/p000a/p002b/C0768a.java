package com.samsung.context.sdk.samsunganalytics.p000a.p002b;

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;
import android.telephony.TelephonyManager;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.b.a */
public class C0768a {
    /* renamed from: a */
    private String f49a = "";
    /* renamed from: b */
    private String f50b = "";
    /* renamed from: c */
    private String f51c = "";
    /* renamed from: d */
    private String f52d = "";
    /* renamed from: e */
    private String f53e = "";
    /* renamed from: f */
    private String f54f = "";
    /* renamed from: g */
    private String f55g = "";
    /* renamed from: h */
    private String f56h = "";
    /* renamed from: i */
    private String f57i = "";
    /* renamed from: j */
    private String f58j = "";

    public C0768a(Context context) {
        Locale locale = context.getResources().getConfiguration().locale;
        this.f49a = locale.getDisplayCountry();
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        if (telephonyManager != null) {
            String simOperator = telephonyManager.getSimOperator();
            if (simOperator != null && simOperator.length() >= 3) {
                this.f55g = simOperator.substring(0, 3);
                this.f56h = simOperator.substring(3);
            }
        }
        this.f50b = locale.getLanguage();
        this.f51c = VERSION.RELEASE;
        this.f52d = Build.BRAND;
        this.f53e = Build.MODEL;
        this.f58j = VERSION.INCREMENTAL;
        this.f57i = String.valueOf(TimeUnit.MILLISECONDS.toMinutes((long) TimeZone.getDefault().getRawOffset()));
        try {
            this.f54f = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            C0798a.m148a(getClass(), e);
        }
    }

    /* renamed from: a */
    public String m18a() {
        return this.f55g;
    }

    /* renamed from: b */
    public String m19b() {
        return this.f56h;
    }

    /* renamed from: c */
    public String m20c() {
        return this.f49a;
    }

    /* renamed from: d */
    public String m21d() {
        return this.f50b;
    }

    /* renamed from: e */
    public String m22e() {
        return this.f51c;
    }

    /* renamed from: f */
    public String m23f() {
        return this.f52d;
    }

    /* renamed from: g */
    public String m24g() {
        return this.f53e;
    }

    /* renamed from: h */
    public String m25h() {
        return this.f54f;
    }

    /* renamed from: i */
    public String m26i() {
        return this.f57i;
    }

    /* renamed from: j */
    public String m27j() {
        return this.f58j;
    }
}

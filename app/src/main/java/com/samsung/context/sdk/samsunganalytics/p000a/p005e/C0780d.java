package com.samsung.context.sdk.samsunganalytics.p000a.p005e;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.samsung.context.sdk.samsunganalytics.Configuration;
import com.samsung.context.sdk.samsunganalytics.p000a.C0763a;
import com.samsung.context.sdk.samsunganalytics.p000a.p001a.C0759a;
import com.samsung.context.sdk.samsunganalytics.p000a.p002b.C0768a;
import com.samsung.context.sdk.samsunganalytics.p000a.p004d.C0775c;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0801c;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0802d;
import java.util.HashMap;
import java.util.Map;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.e.d */
public class C0780d {
    private C0780d() {
    }

    /* renamed from: a */
    public static int m72a(Context context, int i) {
        int i2;
        int i3 = 0;
        SharedPreferences a = C0801c.m159a(context);
        if (i == 1) {
            i2 = a.getInt(C0779b.f82b, 0);
            i3 = a.getInt(C0779b.f84d, 0);
        } else if (i == 0) {
            i2 = a.getInt(C0779b.f83c, 0);
            i3 = a.getInt(C0779b.f85e, 0);
        } else {
            i2 = 0;
        }
        return i2 - i3;
    }

    /* renamed from: a */
    public static C0984c m73a(Context context, Configuration configuration, C0768a c0768a, C0763a c0763a) {
        C0984c c0984c = new C0984c(C0759a.GET_POLICY, configuration.getTrackingId(), C0780d.m74a(context, c0768a, configuration), C0801c.m159a(context), c0763a);
        C0798a.m149a("trid: " + configuration.getTrackingId().substring(0, 7) + ", uv: " + configuration.getVersion());
        return c0984c;
    }

    /* renamed from: a */
    public static Map<String, String> m74a(Context context, C0768a c0768a, Configuration configuration) {
        Map<String, String> hashMap = new HashMap();
        hashMap.put("pkn", context.getPackageName());
        hashMap.put("dm", c0768a.m24g());
        if (!TextUtils.isEmpty(c0768a.m18a())) {
            hashMap.put("mcc", c0768a.m18a());
        }
        if (!TextUtils.isEmpty(c0768a.m19b())) {
            hashMap.put("mnc", c0768a.m19b());
        }
        hashMap.put("uv", configuration.getVersion());
        return hashMap;
    }

    /* renamed from: a */
    public static void m75a(Context context, Configuration configuration, C0775c c0775c, C0768a c0768a) {
        c0775c.mo1454a(C0780d.m73a(context, configuration, c0768a, null));
    }

    /* renamed from: a */
    public static void m76a(Context context, Configuration configuration, C0775c c0775c, C0768a c0768a, C0763a c0763a) {
        c0775c.mo1454a(C0780d.m73a(context, configuration, c0768a, c0763a));
    }

    /* renamed from: a */
    public static void m77a(SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putLong(C0779b.f95o, System.currentTimeMillis()).putInt(C0779b.f85e, 0).putInt(C0779b.f84d, 0).apply();
    }

    /* renamed from: a */
    public static boolean m78a(Context context) {
        SharedPreferences a = C0801c.m159a(context);
        if (C0802d.m164a(1, Long.valueOf(a.getLong(C0779b.f95o, 0)))) {
            C0780d.m77a(a);
        }
        return C0802d.m164a(a.getInt(C0779b.f81a, 1), Long.valueOf(a.getLong(C0779b.f94n, 0)));
    }

    /* renamed from: a */
    public static boolean m79a(Context context, int i, int i2) {
        int i3;
        int i4;
        int i5;
        SharedPreferences a = C0801c.m159a(context);
        if (i == 1) {
            i3 = a.getInt(C0779b.f82b, 0);
            i4 = a.getInt(C0779b.f84d, 0);
            i5 = a.getInt(C0779b.f86f, 0);
        } else if (i == 0) {
            i3 = a.getInt(C0779b.f83c, 0);
            i4 = a.getInt(C0779b.f85e, 0);
            i5 = a.getInt(C0779b.f87g, 0);
        } else {
            i5 = 0;
            i4 = 0;
            i3 = 0;
        }
        C0798a.m149a("Quota : " + i3 + "/ Uploaded : " + i4 + "/ limit : " + i5 + "/ size : " + i2);
        if (i3 < i4 + i2) {
            C0798a.m150a("DLS Sender", "send result fail : Over daily quota");
            return false;
        } else if (i5 >= i2) {
            return true;
        } else {
            C0798a.m150a("DLS Sender", "send result fail : Over once quota");
            return false;
        }
    }

    /* renamed from: b */
    public static void m80b(Context context, int i, int i2) {
        SharedPreferences a = C0801c.m159a(context);
        if (i == 1) {
            a.edit().putInt(C0779b.f84d, a.getInt(C0779b.f84d, 0) + i2).apply();
        } else if (i == 0) {
            a.edit().putInt(C0779b.f85e, C0801c.m159a(context).getInt(C0779b.f85e, 0) + i2).apply();
        }
    }
}

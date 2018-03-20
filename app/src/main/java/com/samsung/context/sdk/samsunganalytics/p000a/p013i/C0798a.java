package com.samsung.context.sdk.samsunganalytics.p000a.p013i;

import android.util.Log;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.i.a */
public class C0798a {
    /* renamed from: a */
    private static final String f166a = "SamsungAnalytics:110038";

    private C0798a() {
    }

    /* renamed from: a */
    public static void m148a(Class cls, Exception exception) {
        if (exception != null) {
            Log.w(f166a, "[" + cls.getSimpleName() + "] " + exception.getClass().getSimpleName() + " " + exception.getMessage());
        }
    }

    /* renamed from: a */
    public static void m149a(String str) {
        if (C0802d.m163a()) {
            Log.d(f166a, "[ENG ONLY] " + str);
        }
    }

    /* renamed from: a */
    public static void m150a(String str, String str2) {
        C0798a.m154d("[" + str + "] " + str2);
    }

    /* renamed from: b */
    public static void m151b(String str) {
        Log.v(f166a, str);
    }

    /* renamed from: b */
    public static void m152b(String str, String str2) {
        C0798a.m155e("[" + str + "] " + str2);
    }

    /* renamed from: c */
    public static void m153c(String str) {
        Log.i(f166a, str);
    }

    /* renamed from: d */
    public static void m154d(String str) {
        Log.d(f166a, str);
    }

    /* renamed from: e */
    public static void m155e(String str) {
        Log.e(f166a, str);
    }
}

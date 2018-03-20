package com.samsung.context.sdk.samsunganalytics.p000a.p013i;

import android.os.Build;
import android.support.v4.app.NotificationCompat;
import com.samsung.context.sdk.samsunganalytics.AnalyticsException;
import java.util.Map;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.i.d */
public class C0802d {
    private C0802d() {
    }

    /* renamed from: a */
    public static long m160a(int i) {
        return Long.valueOf(System.currentTimeMillis()).longValue() - (((long) i) * 86400000);
    }

    /* renamed from: a */
    public static String m161a(Map<String, String> map) {
        String str;
        String str2;
        if (((String) map.get("t")).equals("pv")) {
            str = "page: " + ((String) map.get("pn"));
            str2 = "detail: " + ((String) map.get("pd")) + "  value: " + ((String) map.get("pv"));
        } else if (((String) map.get("t")).equals("ev")) {
            str = "event: " + ((String) map.get("en"));
            str2 = "detail: " + ((String) map.get("ed")) + "  value: " + ((String) map.get("ev"));
        } else if (((String) map.get("t")).equals("st")) {
            str = NotificationCompat.CATEGORY_STATUS;
            str2 = (String) map.get("sti");
        } else {
            str = "";
            str2 = "";
        }
        return str + "\n" + str2;
    }

    /* renamed from: a */
    public static void m162a(String str) {
        if (C0802d.m163a()) {
            throw new AnalyticsException(str);
        }
        C0798a.m155e(str);
    }

    /* renamed from: a */
    public static boolean m163a() {
        return Build.TYPE.equals("eng");
    }

    /* renamed from: a */
    public static boolean m164a(int i, Long l) {
        return Long.valueOf(System.currentTimeMillis()).longValue() > l.longValue() + (((long) i) * 86400000);
    }
}

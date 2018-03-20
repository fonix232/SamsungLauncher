package com.samsung.context.sdk.samsunganalytics.p000a.p013i;

import java.util.Map;
import java.util.Map.Entry;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.i.b */
public class C0800b<K, V> {
    /* renamed from: a */
    public static final String f173a = "\u000e";

    /* renamed from: com.samsung.context.sdk.samsunganalytics.a.i.b$a */
    public enum C0799a {
        ONE_DEPTH("\u0002", "\u0003"),
        TWO_DEPTH("\u0004", "\u0005"),
        THREE_DEPTH("\u0006", "\u0007");
        
        /* renamed from: d */
        private String f171d;
        /* renamed from: e */
        private String f172e;

        private C0799a(String str, String str2) {
            this.f171d = str;
            this.f172e = str2;
        }

        /* renamed from: a */
        public String m156a() {
            return this.f171d;
        }

        /* renamed from: b */
        public String m157b() {
            return this.f172e;
        }
    }

    /* renamed from: a */
    public String m158a(Map<K, V> map, C0799a c0799a) {
        String str = null;
        for (Entry entry : map.entrySet()) {
            if (str == null) {
                str = entry.getKey().toString();
            } else {
                str = (str + c0799a.m156a()) + entry.getKey();
            }
            str = (str + c0799a.m157b()) + entry.getValue();
        }
        return str;
    }
}

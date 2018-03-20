package com.samsung.context.sdk.samsunganalytics.p000a.p012h;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0800b;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0800b.C0799a;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0801c;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.h.c */
public class C0797c {
    /* renamed from: f */
    private static final int f160f = 512;
    /* renamed from: a */
    private Set<String> f161a;
    /* renamed from: b */
    private Context f162b;
    /* renamed from: c */
    private final String f163c = C0799a.TWO_DEPTH.m157b();
    /* renamed from: d */
    private final String f164d = C0799a.TWO_DEPTH.m156a();
    /* renamed from: e */
    private final String f165e = C0799a.THREE_DEPTH.m156a();

    public C0797c(Context context) {
        this.f162b = context;
        this.f161a = C0801c.m159a(context).getStringSet(C0801c.f178e, new HashSet());
    }

    /* renamed from: a */
    private SharedPreferences m144a(String str) {
        return this.f162b.getSharedPreferences(str, 0);
    }

    /* renamed from: b */
    private List<String> m145b() {
        if (this.f161a.isEmpty()) {
            return null;
        }
        List<String> arrayList = new ArrayList();
        String str = "";
        for (String str2 : this.f161a) {
            String str22;
            SharedPreferences a = m144a(str22);
            Set b = m146b(str22);
            for (Entry entry : a.getAll().entrySet()) {
                if (b.contains(entry.getKey())) {
                    String str3;
                    String str4 = "";
                    Class cls = entry.getValue().getClass();
                    if (cls.equals(Integer.class) || cls.equals(Float.class) || cls.equals(Long.class) || cls.equals(String.class) || cls.equals(Boolean.class)) {
                        str3 = str4 + ((String) entry.getKey()) + this.f163c + entry.getValue();
                    } else {
                        Set<String> set = (Set) entry.getValue();
                        str4 = str4 + ((String) entry.getKey()) + this.f163c;
                        str3 = null;
                        for (String str222 : set) {
                            if (!TextUtils.isEmpty(str3)) {
                                str3 = str3 + this.f165e;
                            }
                            str3 = str3 + str222;
                        }
                        str3 = str4 + str3;
                    }
                    if (str.length() + str3.length() > 512) {
                        arrayList.add(str);
                        str222 = "";
                    } else {
                        str222 = !TextUtils.isEmpty(str) ? str + this.f164d : str;
                    }
                    str = str222 + str3;
                }
            }
        }
        if (str.length() != 0) {
            arrayList.add(str);
        }
        return arrayList;
    }

    /* renamed from: b */
    private Set<String> m146b(String str) {
        return C0801c.m159a(this.f162b).getStringSet(str, new HashSet());
    }

    /* renamed from: a */
    public List<String> m147a() {
        List<String> b = m145b();
        Map all = m144a("SASettingPref").getAll();
        if (!(all == null || all.isEmpty())) {
            b.add(new C0800b().m158a(all, C0799a.TWO_DEPTH));
        }
        return b;
    }
}

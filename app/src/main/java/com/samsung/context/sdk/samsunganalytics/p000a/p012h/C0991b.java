package com.samsung.context.sdk.samsunganalytics.p000a.p012h;

import android.content.SharedPreferences;
import com.samsung.context.sdk.samsunganalytics.p000a.p004d.C0774b;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0801c;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.h.b */
public class C0991b implements C0774b {
    /* renamed from: a */
    private SharedPreferences f236a;
    /* renamed from: b */
    private Map<String, Set<String>> f237b;

    public C0991b(SharedPreferences sharedPreferences, Map<String, Set<String>> map) {
        this.f236a = sharedPreferences;
        this.f237b = map;
    }

    /* renamed from: a */
    public void mo1451a() {
        for (String remove : this.f236a.getStringSet(C0801c.f178e, new HashSet())) {
            this.f236a.edit().remove(remove).apply();
        }
        Set hashSet = new HashSet();
        this.f236a.edit().remove(C0801c.f178e).apply();
        for (Entry entry : this.f237b.entrySet()) {
            String str = (String) entry.getKey();
            hashSet.add(str);
            this.f236a.edit().putStringSet(str, (Set) entry.getValue()).apply();
        }
        this.f236a.edit().putStringSet(C0801c.f178e, hashSet).apply();
    }

    /* renamed from: b */
    public int mo1452b() {
        C0798a.m149a("RegisterClient:" + this.f236a.getAll().toString());
        return 0;
    }
}

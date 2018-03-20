package com.samsung.context.sdk.samsunganalytics.p000a.p012h;

import android.content.Context;
import com.samsung.context.sdk.samsunganalytics.Configuration;
import com.samsung.context.sdk.samsunganalytics.p000a.p004d.C0774b;
import com.samsung.context.sdk.samsunganalytics.p000a.p007g.C0795c;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0801c;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.h.a */
public class C0990a implements C0774b {
    /* renamed from: a */
    private static boolean f232a = true;
    /* renamed from: b */
    private Context f233b;
    /* renamed from: c */
    private Configuration f234c;
    /* renamed from: d */
    private List<String> f235d;

    public C0990a(Context context, Configuration configuration) {
        this.f233b = context;
        this.f234c = configuration;
    }

    /* renamed from: a */
    public static void m193a(boolean z) {
        f232a = z;
    }

    /* renamed from: c */
    public static boolean m194c() {
        return f232a;
    }

    /* renamed from: a */
    public void mo1451a() {
        this.f235d = new C0797c(this.f233b).m147a();
    }

    /* renamed from: b */
    public int mo1452b() {
        if (this.f235d.isEmpty()) {
            C0798a.m150a("Setting Sender", "No status log");
        } else {
            Map hashMap = new HashMap();
            hashMap.put("ts", String.valueOf(System.currentTimeMillis()));
            hashMap.put("t", "st");
            long j = 0;
            for (String put : this.f235d) {
                long currentTimeMillis;
                hashMap.put("sti", put);
                if (C0795c.m137a(this.f233b, null, this.f234c).mo1690d(hashMap) == 0) {
                    C0798a.m150a("Setting Sender", "Send success");
                    currentTimeMillis = System.currentTimeMillis();
                } else {
                    C0798a.m150a("Setting Sender", "Send fail");
                    currentTimeMillis = j;
                }
                j = currentTimeMillis;
            }
            if (j != 0) {
                C0801c.m159a(this.f233b).edit().putLong(C0801c.f179f, j).apply();
            }
        }
        return 0;
    }
}

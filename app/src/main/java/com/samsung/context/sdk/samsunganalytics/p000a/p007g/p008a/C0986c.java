package com.samsung.context.sdk.samsunganalytics.p000a.p007g.p008a;

import com.samsung.context.sdk.samsunganalytics.Configuration;
import com.samsung.context.sdk.samsunganalytics.p000a.p004d.C0773a;
import com.samsung.context.sdk.samsunganalytics.p000a.p004d.C0774b;
import com.samsung.context.sdk.samsunganalytics.p000a.p007g.C0796d;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.g.a.c */
public class C0986c implements C0774b {
    /* renamed from: a */
    private static final String f207a = "SAM";
    /* renamed from: b */
    private C0787a f208b;
    /* renamed from: c */
    private Configuration f209c;
    /* renamed from: d */
    private C0796d f210d;
    /* renamed from: e */
    private C0773a f211e;
    /* renamed from: f */
    private int f212f = -1;

    public C0986c(C0787a c0787a, Configuration configuration, C0796d c0796d, C0773a c0773a) {
        this.f208b = c0787a;
        this.f209c = configuration;
        this.f210d = c0796d;
        this.f211e = c0773a;
    }

    /* renamed from: a */
    public void mo1451a() {
        try {
            this.f212f = this.f208b.m105d().requestSend(f207a, this.f209c.getTrackingId().substring(0, 3), this.f210d.m141b(), this.f210d.m138a(), "0", "", "1.10.038", this.f210d.m143c());
            C0798a.m149a("send to DLC : " + this.f210d.m143c());
        } catch (Exception e) {
            C0798a.m148a(getClass(), e);
        }
    }

    /* renamed from: b */
    public int mo1452b() {
        if (this.f212f == 0) {
            C0798a.m150a("DLC Sender", "send result success : " + this.f212f);
            return 1;
        }
        C0798a.m150a("DLC Sender", "send result fail : " + this.f212f);
        return -7;
    }
}

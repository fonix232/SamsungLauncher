package com.samsung.context.sdk.samsunganalytics.p000a.p001a;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.a.a */
public enum C0759a {
    GET_POLICY(C0761c.POLICY, C0760b.DEVICE_CONTROLLER_DIR, C0762d.GET),
    SEND_LOG(C0761c.DLS, C0760b.DLS_DIR, C0762d.POST),
    SEND_BUFFERED_LOG(C0761c.DLS, C0760b.DLS_DIR_BAT, C0762d.POST);
    
    /* renamed from: d */
    C0761c f29d;
    /* renamed from: e */
    C0760b f30e;
    /* renamed from: f */
    C0762d f31f;

    private C0759a(C0761c c0761c, C0760b c0760b, C0762d c0762d) {
        this.f29d = c0761c;
        this.f30e = c0760b;
        this.f31f = c0762d;
    }

    /* renamed from: a */
    public String m10a() {
        return this.f29d.m14a() + this.f30e.m12a();
    }

    /* renamed from: b */
    public String m11b() {
        return this.f31f.m16a();
    }
}

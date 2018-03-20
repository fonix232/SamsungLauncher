package com.samsung.context.sdk.samsunganalytics.p000a.p003c;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.c.c */
public class C0772c {

    /* renamed from: com.samsung.context.sdk.samsunganalytics.a.c.c$a */
    public enum C0771a {
        FULL,
        SIMPLE
    }

    private C0772c() {
    }

    /* renamed from: a */
    public static C0770a m56a(C0771a c0771a) {
        return c0771a == C0771a.FULL ? new C0981b() : c0771a == C0771a.SIMPLE ? new C0982d() : new C0982d();
    }
}

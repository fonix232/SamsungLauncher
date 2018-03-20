package com.samsung.context.sdk.samsunganalytics.p000a.p007g.p009c.p011b;

import com.samsung.context.sdk.samsunganalytics.p000a.p007g.C0796d;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.g.c.b.a */
public class C0794a {
    /* renamed from: b */
    private static final int f152b = 25;
    /* renamed from: c */
    private static final int f153c = 100;
    /* renamed from: a */
    protected LinkedBlockingQueue<C0796d> f154a;

    public C0794a() {
        this.f154a = new LinkedBlockingQueue(25);
    }

    public C0794a(int i) {
        if (i < 25) {
            this.f154a = new LinkedBlockingQueue(25);
        } else if (i > 100) {
            this.f154a = new LinkedBlockingQueue(100);
        } else {
            this.f154a = new LinkedBlockingQueue(i);
        }
    }

    /* renamed from: a */
    public Queue<C0796d> m133a() {
        return this.f154a;
    }

    /* renamed from: a */
    public void m134a(C0796d c0796d) {
        if (!this.f154a.offer(c0796d)) {
            C0798a.m150a("QueueManager", "queue size over. remove oldest log");
            this.f154a.poll();
            this.f154a.offer(c0796d);
        }
    }

    /* renamed from: b */
    public long m135b() {
        return (long) this.f154a.size();
    }

    /* renamed from: c */
    public boolean m136c() {
        return this.f154a.isEmpty();
    }
}

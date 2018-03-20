package com.samsung.context.sdk.samsunganalytics.p000a.p007g.p009c;

import android.content.Context;
import com.samsung.context.sdk.samsunganalytics.Configuration;
import com.samsung.context.sdk.samsunganalytics.p000a.p005e.C0779b;
import com.samsung.context.sdk.samsunganalytics.p000a.p007g.C0796d;
import com.samsung.context.sdk.samsunganalytics.p000a.p007g.p009c.p010a.C0790a;
import com.samsung.context.sdk.samsunganalytics.p000a.p007g.p009c.p011b.C0794a;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0801c;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0802d;
import java.util.List;
import java.util.Queue;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.g.c.a */
public class C0793a {
    /* renamed from: d */
    private static C0793a f148d;
    /* renamed from: a */
    private C0790a f149a;
    /* renamed from: b */
    private C0794a f150b;
    /* renamed from: c */
    private boolean f151c;

    private C0793a(Context context, boolean z) {
        this(context, z, 0);
    }

    private C0793a(Context context, boolean z, int i) {
        if (z) {
            this.f149a = new C0790a(context);
        }
        this.f150b = new C0794a(i);
        this.f151c = z;
    }

    /* renamed from: a */
    public static C0793a m118a(Context context, Configuration configuration) {
        if (f148d == null) {
            synchronized (C0793a.class) {
                int queueSize = configuration.getQueueSize();
                if (!configuration.isEnableUseInAppLogging()) {
                    f148d = new C0793a(context, false, queueSize);
                } else if (C0801c.m159a(context).getString(C0779b.f91k, "").equals(C0779b.f93m)) {
                    f148d = new C0793a(context, true, queueSize);
                } else {
                    f148d = new C0793a(context, false, queueSize);
                }
            }
        }
        return f148d;
    }

    /* renamed from: a */
    public static C0793a m119a(Context context, Boolean bool, int i) {
        if (f148d == null) {
            synchronized (C0793a.class) {
                if (bool.booleanValue()) {
                    f148d = new C0793a(context, true, i);
                } else {
                    f148d = new C0793a(context, false, i);
                }
            }
        }
        return f148d;
    }

    /* renamed from: g */
    private void m120g() {
        if (!this.f150b.m133a().isEmpty()) {
            for (C0796d a : this.f150b.m133a()) {
                this.f149a.m112a(a);
            }
            this.f150b.m133a().clear();
        }
    }

    /* renamed from: a */
    public Queue<C0796d> m121a(int i) {
        Queue<C0796d> a;
        if (this.f151c) {
            m129c();
            a = i <= 0 ? this.f149a.m109a() : this.f149a.m110a(i);
        } else {
            a = this.f150b.m133a();
        }
        if (!a.isEmpty()) {
            C0798a.m149a("get log from " + (this.f151c ? "Database " : "Queue ") + "(" + a.size() + ")");
        }
        return a;
    }

    /* renamed from: a */
    public void m122a() {
        this.f151c = false;
    }

    /* renamed from: a */
    public void m123a(long j, String str, String str2) {
        m125a(new C0796d(str, j, str2));
    }

    /* renamed from: a */
    public void m124a(Context context) {
        this.f151c = true;
        if (this.f149a == null) {
            this.f149a = new C0790a(context);
        }
        m120g();
    }

    /* renamed from: a */
    public void m125a(C0796d c0796d) {
        if (this.f151c) {
            this.f149a.m112a(c0796d);
        } else {
            this.f150b.m134a(c0796d);
        }
    }

    /* renamed from: a */
    public void m126a(String str) {
        if (this.f151c) {
            this.f149a.m113a(str);
        }
    }

    /* renamed from: a */
    public void m127a(List<String> list) {
        if (!list.isEmpty() && this.f151c) {
            this.f149a.m114a((List) list);
        }
    }

    /* renamed from: b */
    public boolean m128b() {
        return this.f151c;
    }

    /* renamed from: c */
    public void m129c() {
        if (this.f151c) {
            this.f149a.m111a(C0802d.m160a(5));
        }
    }

    /* renamed from: d */
    public Queue<C0796d> m130d() {
        return m121a(0);
    }

    /* renamed from: e */
    public long m131e() {
        return this.f151c ? this.f149a.m117d() : this.f150b.m135b();
    }

    /* renamed from: f */
    public boolean m132f() {
        return this.f151c ? this.f149a.m116c() : this.f150b.m136c();
    }
}

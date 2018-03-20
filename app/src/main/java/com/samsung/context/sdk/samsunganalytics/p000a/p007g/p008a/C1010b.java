package com.samsung.context.sdk.samsunganalytics.p000a.p007g.p008a;

import android.content.Context;
import com.samsung.context.sdk.samsunganalytics.Configuration;
import com.samsung.context.sdk.samsunganalytics.p000a.C0763a;
import com.samsung.context.sdk.samsunganalytics.p000a.p007g.C0796d;
import com.samsung.context.sdk.samsunganalytics.p000a.p007g.C0987a;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;
import java.util.Map;
import java.util.Queue;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.g.a.b */
public class C1010b extends C0987a {
    /* renamed from: g */
    private C0787a f244g;

    /* renamed from: com.samsung.context.sdk.samsunganalytics.a.g.a.b$1 */
    class C09851 implements C0763a<Void, Void> {
        /* renamed from: a */
        final /* synthetic */ C1010b f206a;

        C09851(C1010b c1010b) {
            this.f206a = c1010b;
        }

        /* renamed from: a */
        public Void m178a(Void voidR) {
            this.f206a.m202b();
            return null;
        }
    }

    public C1010b(Context context, Configuration configuration) {
        super(context, configuration);
        this.f244g = new C0787a(context, new C09851(this));
        this.f244g.m103b();
    }

    /* renamed from: b */
    private void m202b() {
        Queue d = this.e.m130d();
        while (!d.isEmpty()) {
            this.f.mo1454a(new C0986c(this.f244g, this.b, (C0796d) d.poll(), null));
        }
    }

    /* renamed from: a */
    protected Map<String, String> mo1689a(Map<String, String> map) {
        Map<String, String> a = super.mo1689a(map);
        a.remove("do");
        a.remove("dm");
        a.remove("v");
        return a;
    }

    /* renamed from: d */
    public int mo1690d(Map<String, String> map) {
        if (!m182a()) {
            return -5;
        }
        m184c(map);
        if (this.f244g.m104c()) {
            m202b();
        } else {
            this.f244g.m103b();
        }
        return 0;
    }

    /* renamed from: e */
    public int mo1691e(Map<String, String> map) {
        C0798a.m150a("DLCLogSender", "not support sync api");
        mo1690d(map);
        return -100;
    }
}

package com.samsung.context.sdk.samsunganalytics.p000a.p007g.p014b;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.samsung.context.sdk.samsunganalytics.Configuration;
import com.samsung.context.sdk.samsunganalytics.p000a.p004d.C0773a;
import com.samsung.context.sdk.samsunganalytics.p000a.p004d.C0774b;
import com.samsung.context.sdk.samsunganalytics.p000a.p005e.C0779b;
import com.samsung.context.sdk.samsunganalytics.p000a.p005e.C0780d;
import com.samsung.context.sdk.samsunganalytics.p000a.p005e.C0984c;
import com.samsung.context.sdk.samsunganalytics.p000a.p007g.C0796d;
import com.samsung.context.sdk.samsunganalytics.p000a.p007g.C0987a;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.g.b.b */
public class C1011b extends C0987a {
    /* renamed from: g */
    public static final int f245g = 200;

    public C1011b(Context context, Configuration configuration) {
        super(context, configuration);
    }

    /* renamed from: a */
    private int m206a(int i) {
        if (i == -4) {
            C0798a.m150a("DLS Sender", "Network unavailable.");
            return -4;
        } else if (C0780d.m78a(this.a)) {
            C0798a.m150a("DLS Sender", "policy expired. request policy");
            return -6;
        } else if (this.b.getRestrictedNetworkType() != i) {
            return 0;
        } else {
            C0798a.m150a("DLS Sender", "Network unavailable by restrict option:" + i);
            return -4;
        }
    }

    /* renamed from: a */
    private int m207a(int i, C0796d c0796d, C0773a c0773a, boolean z) {
        if (c0796d == null) {
            return -100;
        }
        int length = c0796d.m143c().getBytes().length;
        if (!C0780d.m79a(this.a, i, length)) {
            return -1;
        }
        C0780d.m80b(this.a, i, length);
        C0774b c0988a = new C0988a(c0796d, this.b.getTrackingId(), this.b.getNetworkTimeoutInMilliSeconds(), c0773a);
        if (z) {
            C0798a.m149a("sync send");
            c0988a.mo1451a();
            return c0988a.mo1452b();
        }
        this.f.mo1454a(c0988a);
        return 0;
    }

    /* renamed from: a */
    private int m208a(int i, Queue<C0796d> queue, C0773a c0773a) {
        List arrayList = new ArrayList();
        Queue a;
        while (!a.isEmpty()) {
            Queue linkedBlockingQueue = new LinkedBlockingQueue();
            int a2 = C0780d.m72a(this.a, i);
            int i2 = C0779b.f97q > a2 ? a2 : 51200;
            int i3 = 0;
            while (!a.isEmpty()) {
                C0796d c0796d = (C0796d) a.element();
                if (c0796d.m143c().getBytes().length + i3 > i2) {
                    break;
                }
                i3 += c0796d.m143c().getBytes().length;
                linkedBlockingQueue.add(c0796d);
                a.poll();
                arrayList.add(c0796d.m138a());
                if (a.isEmpty()) {
                    this.e.m127a(arrayList);
                    a = this.e.m121a(200);
                }
            }
            if (linkedBlockingQueue.isEmpty()) {
                return -1;
            }
            this.e.m127a(arrayList);
            m210a(i, linkedBlockingQueue, i3, c0773a);
            C0798a.m150a("DLSLogSender", "send packet : num(" + linkedBlockingQueue.size() + ") size(" + i3 + ")");
        }
        return 0;
    }

    /* renamed from: a */
    private void m210a(int i, Queue<C0796d> queue, int i2, C0773a c0773a) {
        C0780d.m80b(this.a, i, i2);
        this.f.mo1454a(new C0988a((Queue) queue, this.b.getTrackingId(), this.b.getNetworkTimeoutInMilliSeconds(), c0773a));
    }

    /* renamed from: b */
    private int m211b() {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) this.a.getSystemService("connectivity")).getActiveNetworkInfo();
        return (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) ? -4 : activeNetworkInfo.getType();
    }

    /* renamed from: d */
    public int mo1690d(Map<String, String> map) {
        if (!m182a()) {
            return -5;
        }
        final int b = m211b();
        int a = m206a(b);
        if (a != 0) {
            m184c(map);
            if (a != -6) {
                return a;
            }
            C0780d.m75a(this.a, this.b, this.f, this.c);
            this.e.m129c();
            return a;
        }
        C0773a c09891 = new C0773a(this) {
            /* renamed from: b */
            final /* synthetic */ C1011b f231b;

            /* renamed from: a */
            public void mo1455a(int i, String str, String str2) {
            }

            /* renamed from: b */
            public void mo1456b(int i, String str, String str2) {
                this.f231b.e.m123a(Long.valueOf(str).longValue(), "", str2);
                C0780d.m80b(this.f231b.a, b, str2.getBytes().length * -1);
            }
        };
        m207a(b, new C0796d(Long.valueOf((String) map.get("ts")).longValue(), m183b(mo1689a(map))), c09891, false);
        Queue a2 = this.e.m121a(200);
        if (this.e.m128b()) {
            m208a(b, a2, c09891);
        } else {
            while (!a2.isEmpty()) {
                a = m207a(b, (C0796d) a2.poll(), c09891, false);
                if (a != 0) {
                    return a;
                }
            }
        }
        return 0;
    }

    /* renamed from: e */
    public int mo1691e(Map<String, String> map) {
        if (!m182a()) {
            return -5;
        }
        int b = m211b();
        int a = m206a(b);
        if (a != 0) {
            if (a != -6) {
                return a;
            }
            C0984c a2 = C0780d.m73a(this.a, this.b, this.c, null);
            a2.mo1451a();
            a = a2.mo1452b();
            C0798a.m149a("get policy sync " + a);
            if (a != 0) {
                return a;
            }
        }
        return m207a(b, new C0796d(Long.valueOf((String) map.get("ts")).longValue(), m183b(mo1689a(map))), null, true);
    }
}

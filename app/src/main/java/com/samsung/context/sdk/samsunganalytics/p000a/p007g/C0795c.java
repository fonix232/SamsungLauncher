package com.samsung.context.sdk.samsunganalytics.p000a.p007g;

import android.content.Context;
import com.samsung.context.sdk.samsunganalytics.Configuration;
import com.samsung.context.sdk.samsunganalytics.p000a.p007g.p008a.C1010b;
import com.samsung.context.sdk.samsunganalytics.p000a.p007g.p014b.C1011b;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.g.c */
public class C0795c {
    /* renamed from: a */
    private static C1011b f155a;
    /* renamed from: b */
    private static C1010b f156b;

    /* renamed from: com.samsung.context.sdk.samsunganalytics.a.g.c$a */
    public enum C0789a {
        DLC,
        DLS,
        INTENT
    }

    private C0795c() {
    }

    /* renamed from: a */
    public static C0788b m137a(Context context, C0789a c0789a, Configuration configuration) {
        if (c0789a == null) {
            c0789a = configuration.isEnableUseInAppLogging() ? C0789a.DLS : C0789a.DLC;
        }
        if (c0789a == C0789a.DLS) {
            if (f155a == null) {
                synchronized (C0795c.class) {
                    f155a = new C1011b(context, configuration);
                }
            }
            return f155a;
        } else if (c0789a != C0789a.DLC) {
            return null;
        } else {
            if (f156b == null) {
                synchronized (C0795c.class) {
                    f156b = new C1010b(context, configuration);
                }
            }
            return f156b;
        }
    }
}

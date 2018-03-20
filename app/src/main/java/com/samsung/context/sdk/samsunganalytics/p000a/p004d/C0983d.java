package com.samsung.context.sdk.samsunganalytics.p000a.p004d;

import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.d.d */
public class C0983d implements C0775c {
    /* renamed from: a */
    private static ExecutorService f198a;
    /* renamed from: b */
    private static C0983d f199b;

    /* renamed from: com.samsung.context.sdk.samsunganalytics.a.d.d$1 */
    class C07761 implements ThreadFactory {
        /* renamed from: a */
        final /* synthetic */ C0983d f73a;

        C07761(C0983d c0983d) {
            this.f73a = c0983d;
        }

        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setPriority(1);
            thread.setDaemon(true);
            C0798a.m154d("newThread on Executor");
            return thread;
        }
    }

    public C0983d() {
        f198a = Executors.newSingleThreadExecutor(new C07761(this));
    }

    /* renamed from: a */
    public static C0775c m171a() {
        if (f199b == null) {
            f199b = new C0983d();
        }
        return f199b;
    }

    /* renamed from: a */
    public void mo1454a(final C0774b c0774b) {
        f198a.submit(new Runnable(this) {
            /* renamed from: b */
            final /* synthetic */ C0983d f75b;

            public void run() {
                c0774b.mo1451a();
                c0774b.mo1452b();
            }
        });
    }
}

package com.samsung.context.sdk.samsunganalytics.p000a.p007g.p008a;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import com.samsung.context.sdk.samsunganalytics.p000a.C0763a;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;
import com.sec.spp.push.dlc.api.IDlcService;
import com.sec.spp.push.dlc.api.IDlcService.AbstractDlcService;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.g.a.a */
public class C0787a {
    /* renamed from: a */
    private static final String f109a = "com.sec.spp.push.REQUEST_REGISTER";
    /* renamed from: b */
    private static final String f110b = "com.sec.spp.push.REQUEST_DEREGISTER";
    /* renamed from: c */
    private static String f111c = "com.sec.spp.push";
    /* renamed from: d */
    private static String f112d = "com.sec.spp.push.dlc.writer.WriterService";
    /* renamed from: e */
    private static final String f113e = "EXTRA_PACKAGENAME";
    /* renamed from: f */
    private static final String f114f = "EXTRA_INTENTFILTER";
    /* renamed from: g */
    private static final String f115g = "EXTRA_STR";
    /* renamed from: h */
    private static final String f116h = "EXTRA_RESULT_CODE";
    /* renamed from: i */
    private static final String f117i = "EXTRA_STR_ACTION";
    /* renamed from: j */
    private static final int f118j = 100;
    /* renamed from: k */
    private static final int f119k = 200;
    /* renamed from: l */
    private static final int f120l = -2;
    /* renamed from: m */
    private static final int f121m = -3;
    /* renamed from: n */
    private static final int f122n = -4;
    /* renamed from: o */
    private static final int f123o = -5;
    /* renamed from: p */
    private static final int f124p = -6;
    /* renamed from: q */
    private static final int f125q = -7;
    /* renamed from: r */
    private static final int f126r = -8;
    /* renamed from: s */
    private Context f127s;
    /* renamed from: t */
    private BroadcastReceiver f128t;
    /* renamed from: u */
    private String f129u;
    /* renamed from: v */
    private C0763a f130v;
    /* renamed from: w */
    private boolean f131w;
    /* renamed from: x */
    private boolean f132x;
    /* renamed from: y */
    private IDlcService f133y;
    /* renamed from: z */
    private ServiceConnection f134z;

    /* renamed from: com.samsung.context.sdk.samsunganalytics.a.g.a.a$1 */
    class C07851 implements ServiceConnection {
        /* renamed from: a */
        final /* synthetic */ C0787a f107a;

        C07851(C0787a c0787a) {
            this.f107a = c0787a;
        }

        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            C0798a.m150a("DLC Sender", "DLC Client ServiceConnected");
            this.f107a.f133y = AbstractDlcService.m200a(iBinder);
            if (this.f107a.f128t != null) {
                this.f107a.f127s.unregisterReceiver(this.f107a.f128t);
                this.f107a.f128t = null;
            }
            if (this.f107a.f130v != null) {
                this.f107a.f130v.mo1450a(null);
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            C0798a.m150a("DLC Sender", "Client ServiceDisconnected");
            this.f107a.f133y = null;
            this.f107a.f131w = false;
        }
    }

    /* renamed from: com.samsung.context.sdk.samsunganalytics.a.g.a.a$2 */
    class C07862 extends BroadcastReceiver {
        /* renamed from: a */
        final /* synthetic */ C0787a f108a;

        C07862(C0787a c0787a) {
            this.f108a = c0787a;
        }

        public void onReceive(Context context, Intent intent) {
            this.f108a.f132x = false;
            if (intent == null) {
                C0798a.m150a("DLC Sender", "dlc register reply fail");
                return;
            }
            String action = intent.getAction();
            Bundle extras = intent.getExtras();
            if (action == null || extras == null) {
                C0798a.m150a("DLC Sender", "dlc register reply fail");
            } else if (action.equals(this.f108a.f129u)) {
                action = extras.getString(C0787a.f115g);
                int i = extras.getInt(C0787a.f116h);
                C0798a.m150a("DLC Sender", "register DLC result:" + action);
                if (i < 0) {
                    C0798a.m150a("DLC Sender", "register DLC result fail:" + action);
                    return;
                }
                this.f108a.m95a(extras.getString(C0787a.f117i));
            }
        }
    }

    public C0787a(Context context) {
        this.f131w = false;
        this.f132x = false;
        this.f134z = new C07851(this);
        this.f127s = context;
        this.f129u = context.getPackageName();
        this.f129u += ".REGISTER_FILTER";
    }

    public C0787a(Context context, C0763a c0763a) {
        this(context);
        this.f130v = c0763a;
    }

    /* renamed from: a */
    private void m95a(String str) {
        if (this.f131w) {
            m101e();
        }
        try {
            Intent intent = new Intent(str);
            intent.setClassName(f111c, f112d);
            this.f131w = this.f127s.bindService(intent, this.f134z, 1);
            C0798a.m150a("DLCBinder", "bind");
        } catch (Exception e) {
            C0798a.m148a(getClass(), e);
        }
    }

    /* renamed from: e */
    private void m101e() {
        if (this.f131w) {
            try {
                C0798a.m150a("DLCBinder", "unbind");
                this.f127s.unbindService(this.f134z);
                this.f131w = false;
            } catch (Exception e) {
                C0798a.m148a(getClass(), e);
            }
        }
    }

    /* renamed from: a */
    public void m102a() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(this.f129u);
        if (this.f128t == null) {
            this.f128t = new C07862(this);
        }
        this.f127s.registerReceiver(this.f128t, intentFilter);
    }

    /* renamed from: b */
    public void m103b() {
        if (this.f128t == null) {
            m102a();
        }
        if (this.f132x) {
            C0798a.m150a("DLCBinder", "already send register request");
            return;
        }
        Intent intent = new Intent(f109a);
        intent.putExtra(f113e, this.f127s.getPackageName());
        intent.putExtra(f114f, this.f129u);
        intent.setPackage("com.sec.spp.push");
        this.f127s.sendBroadcast(intent);
        this.f132x = true;
        C0798a.m150a("DLCBinder", "send register Request");
        C0798a.m149a("send register Request:" + this.f127s.getPackageName());
    }

    /* renamed from: c */
    public boolean m104c() {
        return this.f131w;
    }

    /* renamed from: d */
    public IDlcService m105d() {
        return this.f133y;
    }
}

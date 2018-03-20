package com.samsung.context.sdk.samsunganalytics.p000a;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings.System;
import android.text.TextUtils;
import com.samsung.context.sdk.samsunganalytics.Configuration;
import com.samsung.context.sdk.samsunganalytics.LogBuilders.CustomBuilder;
import com.samsung.context.sdk.samsunganalytics.LogBuilders.ScreenViewBuilder;
import com.samsung.context.sdk.samsunganalytics.UserAgreement;
import com.samsung.context.sdk.samsunganalytics.p000a.p001a.C0760b;
import com.samsung.context.sdk.samsunganalytics.p000a.p001a.C0761c;
import com.samsung.context.sdk.samsunganalytics.p000a.p002b.C0768a;
import com.samsung.context.sdk.samsunganalytics.p000a.p003c.C0772c;
import com.samsung.context.sdk.samsunganalytics.p000a.p003c.C0772c.C0771a;
import com.samsung.context.sdk.samsunganalytics.p000a.p004d.C0774b;
import com.samsung.context.sdk.samsunganalytics.p000a.p004d.C0983d;
import com.samsung.context.sdk.samsunganalytics.p000a.p005e.C0779b;
import com.samsung.context.sdk.samsunganalytics.p000a.p005e.C0780d;
import com.samsung.context.sdk.samsunganalytics.p000a.p005e.C0781e;
import com.samsung.context.sdk.samsunganalytics.p000a.p007g.C0795c;
import com.samsung.context.sdk.samsunganalytics.p000a.p007g.p009c.C0793a;
import com.samsung.context.sdk.samsunganalytics.p000a.p012h.C0990a;
import com.samsung.context.sdk.samsunganalytics.p000a.p012h.C0991b;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0801c;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0802d;
import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.b */
public class C0769b {
    /* renamed from: a */
    public static final int f59a = 128;
    /* renamed from: b */
    private static final int f60b = 0;
    /* renamed from: c */
    private static final int f61c = 1;
    /* renamed from: d */
    private static final int f62d = 2;
    /* renamed from: e */
    private Application f63e;
    /* renamed from: f */
    private UncaughtExceptionHandler f64f;
    /* renamed from: g */
    private UncaughtExceptionHandler f65g;
    /* renamed from: h */
    private boolean f66h = false;
    /* renamed from: i */
    private boolean f67i = false;
    /* renamed from: j */
    private ActivityLifecycleCallbacks f68j;
    /* renamed from: k */
    private Configuration f69k;

    /* renamed from: com.samsung.context.sdk.samsunganalytics.a.b$2 */
    class C07642 extends BroadcastReceiver {
        /* renamed from: a */
        final /* synthetic */ C0769b f45a;

        C07642(C0769b c0769b) {
            this.f45a = c0769b;
        }

        public void onReceive(Context context, Intent intent) {
            C0798a.m149a("receive BR");
            this.f45a.m44n();
        }
    }

    /* renamed from: com.samsung.context.sdk.samsunganalytics.a.b$4 */
    class C07654 implements UncaughtExceptionHandler {
        /* renamed from: a */
        final /* synthetic */ C0769b f46a;

        C07654(C0769b c0769b) {
            this.f46a = c0769b;
        }

        public void uncaughtException(Thread thread, Throwable th) {
            if (this.f46a.f66h) {
                C0798a.m154d("get un exc");
                this.f46a.m46a(((CustomBuilder) ((CustomBuilder) ((CustomBuilder) ((CustomBuilder) ((CustomBuilder) new CustomBuilder().set("pn", thread.getName())).set("ecn", th.getClass().getSimpleName())).set("exd", C0772c.m56a(C0771a.SIMPLE).mo1453a(thread.getName(), th))).set("t", "ex")).set("ext", "cr")).build(), false);
                this.f46a.f65g.uncaughtException(thread, th);
                return;
            }
            this.f46a.f65g.uncaughtException(thread, th);
        }
    }

    /* renamed from: com.samsung.context.sdk.samsunganalytics.a.b$5 */
    class C07665 implements ActivityLifecycleCallbacks {
        /* renamed from: a */
        final /* synthetic */ C0769b f47a;

        C07665(C0769b c0769b) {
            this.f47a = c0769b;
        }

        public void onActivityCreated(Activity activity, Bundle bundle) {
        }

        public void onActivityDestroyed(Activity activity) {
        }

        public void onActivityPaused(Activity activity) {
        }

        public void onActivityResumed(Activity activity) {
        }

        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }

        public void onActivityStarted(Activity activity) {
            this.f47a.m46a(((ScreenViewBuilder) new ScreenViewBuilder().setScreenView(activity.getComponentName().getShortClassName())).build(), false);
        }

        public void onActivityStopped(Activity activity) {
        }
    }

    /* renamed from: com.samsung.context.sdk.samsunganalytics.a.b$7 */
    class C07677 extends BroadcastReceiver {
        /* renamed from: a */
        final /* synthetic */ C0769b f48a;

        C07677(C0769b c0769b) {
            this.f48a = c0769b;
        }

        public void onReceive(Context context, Intent intent) {
            int i;
            String stringExtra = intent.getStringExtra("DID");
            if (TextUtils.isEmpty(stringExtra)) {
                stringExtra = this.f48a.m43m();
                i = 1;
                C0798a.m154d("Get CF id empty");
            } else {
                i = 0;
                C0798a.m154d("Get CF id");
            }
            this.f48a.m30a(stringExtra, i);
            this.f48a.f63e.getApplicationContext().unregisterReceiver(this);
        }
    }

    /* renamed from: com.samsung.context.sdk.samsunganalytics.a.b$3 */
    class C09793 implements C0763a<Void, Boolean> {
        /* renamed from: a */
        final /* synthetic */ C0769b f194a;

        C09793(C0769b c0769b) {
            this.f194a = c0769b;
        }

        /* renamed from: a */
        public Void m166a(Boolean bool) {
            if (bool.booleanValue()) {
                C0793a.m119a(this.f194a.f63e.getApplicationContext(), Boolean.valueOf(true), this.f194a.f69k.getQueueSize()).m124a(this.f194a.f63e.getApplicationContext());
            }
            return null;
        }
    }

    public C0769b(final Application application, Configuration configuration) {
        this.f63e = application;
        this.f69k = configuration;
        if (!TextUtils.isEmpty(configuration.getDeviceId())) {
            this.f69k.setAuidType(2);
        }
        if (configuration.isEnableAutoDeviceId()) {
            m42l();
        }
        if (configuration.isEnableUseInAppLogging()) {
            m39i();
        } else {
            this.f69k.setUserAgreement(new UserAgreement(this) {
                /* renamed from: b */
                final /* synthetic */ C0769b f193b;

                public boolean isAgreement() {
                    return System.getInt(application.getContentResolver(), "samsung_errorlog_agree", 0) == 1;
                }
            });
        }
        if (m45o()) {
            if (configuration.isEnableFastReady()) {
                C0795c.m137a(application, null, configuration);
            }
            m44n();
        }
        C0798a.m150a("Tracker", "Tracker start:1.10.038");
    }

    /* renamed from: a */
    private void m30a(String str, int i) {
        C0801c.m159a(this.f63e.getApplicationContext()).edit().putString(C0801c.f176c, str).putInt(C0801c.f177d, i).apply();
        this.f69k.setAuidType(i);
        this.f69k.setDeviceId(str);
    }

    /* renamed from: a */
    private boolean m31a(String str) {
        try {
            StringTokenizer stringTokenizer = new StringTokenizer(this.f63e.getApplicationContext().getPackageManager().getPackageInfo(str, 0).versionName, ".");
            int parseInt = Integer.parseInt(stringTokenizer.nextToken());
            int parseInt2 = Integer.parseInt(stringTokenizer.nextToken());
            int parseInt3 = Integer.parseInt(stringTokenizer.nextToken());
            if (parseInt < 2) {
                C0798a.m149a("CF version < 2.0.9");
                return false;
            } else if (parseInt != 2 || parseInt2 != 0 || parseInt3 >= 9) {
                return true;
            } else {
                C0798a.m149a("CF version < 2.0.9");
                return false;
            }
        } catch (Exception e) {
            C0798a.m148a(getClass(), e);
            return false;
        }
    }

    /* renamed from: b */
    private void m33b(final Map<String, String> map) {
        map.remove("t");
        C0983d.m171a().mo1454a(new C0774b(this) {
            /* renamed from: b */
            final /* synthetic */ C0769b f196b;

            /* renamed from: a */
            public void mo1451a() {
                SharedPreferences sharedPreferences = this.f196b.f63e.getSharedPreferences("SASettingPref", 0);
                for (String str : map.keySet()) {
                    sharedPreferences.edit().putString(str, (String) map.get(str)).apply();
                }
            }

            /* renamed from: b */
            public int mo1452b() {
                return 0;
            }
        });
        if (!C0990a.m194c() || !this.f69k.isAlwaysRunningApp()) {
            return;
        }
        if (this.f69k.isEnableUseInAppLogging() || C0781e.m83a()) {
            m38h();
        }
    }

    /* renamed from: h */
    private void m38h() {
        if (C0990a.m194c()) {
            C0990a.m193a(false);
        }
        C0798a.m149a("register BR");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        this.f63e.getApplicationContext().registerReceiver(new C07642(this), intentFilter);
    }

    /* renamed from: i */
    private void m39i() {
        SharedPreferences a = C0801c.m159a(this.f63e);
        C0761c.DLS.m15a(a.getString(C0779b.f88h, ""));
        C0760b.DLS_DIR.m13a(a.getString("uri", ""));
        C0760b.DLS_DIR_BAT.m13a(a.getString(C0779b.f90j, ""));
        if (C0780d.m78a(this.f63e.getApplicationContext())) {
            C0780d.m76a(this.f63e, this.f69k, C0983d.m171a(), new C0768a(this.f63e), new C09793(this));
        }
    }

    /* renamed from: j */
    private ActivityLifecycleCallbacks m40j() {
        if (this.f68j != null) {
            return this.f68j;
        }
        this.f68j = new C07665(this);
        return this.f68j;
    }

    /* renamed from: k */
    private boolean m41k() {
        String str = "com.samsung.android.providers.context";
        str = ".log.action.REQUEST_DID";
        str = ".log.action.GET_DID";
        str = "PKGNAME";
        if (!C0781e.m83a() || this.f69k.isEnableUseInAppLogging() || !TextUtils.isEmpty(this.f69k.getUserId()) || !m31a("com.samsung.android.providers.context")) {
            return false;
        }
        Intent intent = new Intent("com.samsung.android.providers.context.log.action.REQUEST_DID");
        intent.putExtra("PKGNAME", this.f63e.getPackageName());
        intent.setPackage("com.samsung.android.providers.context");
        this.f63e.getApplicationContext().sendBroadcast(intent);
        IntentFilter intentFilter = new IntentFilter("com.samsung.android.providers.context.log.action.GET_DID");
        this.f63e.getApplicationContext().registerReceiver(new C07677(this), intentFilter);
        return true;
    }

    /* renamed from: l */
    private void m42l() {
        SharedPreferences a = C0801c.m159a(this.f63e);
        String string = a.getString(C0801c.f176c, "");
        int i = a.getInt(C0801c.f177d, -1);
        if ((TextUtils.isEmpty(string) || string.length() != 32) && !m41k()) {
            string = m43m();
            i = 1;
        }
        m30a(string, i);
    }

    /* renamed from: m */
    private String m43m() {
        String str = "0123456789abcdefghijklmjopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        SecureRandom secureRandom = new SecureRandom();
        byte[] bArr = new byte[16];
        StringBuilder stringBuilder = new StringBuilder(32);
        int i = 0;
        while (i < 32) {
            secureRandom.nextBytes(bArr);
            try {
                stringBuilder.append("0123456789abcdefghijklmjopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt((int) (Math.abs(new BigInteger(bArr).longValue()) % ((long) "0123456789abcdefghijklmjopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".length()))));
                i++;
            } catch (Exception e) {
                C0798a.m148a(getClass(), e);
                return null;
            }
        }
        return stringBuilder.toString();
    }

    /* renamed from: n */
    private void m44n() {
        if (!C0802d.m164a(7, Long.valueOf(C0801c.m159a(this.f63e).getLong(C0801c.f179f, 0)))) {
            return;
        }
        if (m45o()) {
            C0983d.m171a().mo1454a(new C0990a(this.f63e, this.f69k));
        } else {
            C0798a.m154d("user do not agree");
        }
    }

    /* renamed from: o */
    private boolean m45o() {
        return this.f69k.getUserAgreement().isAgreement();
    }

    /* renamed from: a */
    public int m46a(Map<String, String> map, boolean z) {
        if (!m45o()) {
            C0798a.m154d("user do not agree");
            return -2;
        } else if (map == null || map.isEmpty()) {
            C0798a.m154d("Failure to send Logs : No data");
            return -3;
        } else if (!((String) map.get("t")).equalsIgnoreCase("st")) {
            return z ? C0795c.m137a(this.f63e, null, this.f69k).mo1691e(map) : C0795c.m137a(this.f63e, null, this.f69k).mo1690d(map);
        } else {
            m33b((Map) map);
            return 0;
        }
    }

    /* renamed from: a */
    public void m47a() {
        this.f66h = true;
        if (this.f64f == null) {
            this.f65g = Thread.getDefaultUncaughtExceptionHandler();
            this.f64f = new C07654(this);
            Thread.setDefaultUncaughtExceptionHandler(this.f64f);
        }
    }

    /* renamed from: a */
    public void m48a(Map<String, Set<String>> map) {
        C0983d.m171a().mo1454a(new C0991b(C0801c.m159a(this.f63e), map));
        if (!C0990a.m194c() || !this.f69k.isAlwaysRunningApp()) {
            return;
        }
        if (this.f69k.isEnableUseInAppLogging() || C0781e.m83a()) {
            m38h();
        }
    }

    /* renamed from: b */
    public void m49b() {
        this.f66h = false;
    }

    /* renamed from: c */
    public void m50c() {
        this.f63e.registerActivityLifecycleCallbacks(m40j());
    }

    /* renamed from: d */
    public void m51d() {
        if (this.f68j != null) {
            this.f63e.unregisterActivityLifecycleCallbacks(this.f68j);
        }
    }

    /* renamed from: e */
    public boolean m52e() {
        return this.f66h;
    }

    /* renamed from: f */
    public boolean m53f() {
        return this.f67i;
    }

    /* renamed from: g */
    public Configuration m54g() {
        return this.f69k;
    }
}

package com.samsung.context.sdk.samsunganalytics.p000a.p007g;

import android.content.Context;
import android.text.TextUtils;
import com.samsung.context.sdk.samsunganalytics.Configuration;
import com.samsung.context.sdk.samsunganalytics.p000a.p002b.C0768a;
import com.samsung.context.sdk.samsunganalytics.p000a.p004d.C0775c;
import com.samsung.context.sdk.samsunganalytics.p000a.p004d.C0983d;
import com.samsung.context.sdk.samsunganalytics.p000a.p007g.p009c.C0793a;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0800b;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0800b.C0799a;
import java.util.Map;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.g.a */
public abstract class C0987a implements C0788b {
    /* renamed from: a */
    protected Context f213a;
    /* renamed from: b */
    protected Configuration f214b;
    /* renamed from: c */
    protected C0768a f215c;
    /* renamed from: d */
    protected C0800b<String, String> f216d;
    /* renamed from: e */
    protected C0793a f217e;
    /* renamed from: f */
    protected C0775c f218f = C0983d.m171a();

    public C0987a(Context context, Configuration configuration) {
        this.f213a = context.getApplicationContext();
        this.f214b = configuration;
        this.f215c = new C0768a(context);
        this.f216d = new C0800b();
        this.f217e = C0793a.m118a(context, configuration);
    }

    /* renamed from: a */
    protected Map<String, String> mo1689a(Map<String, String> map) {
        map.put("v", "1.10.038");
        map.put("tid", this.f214b.getTrackingId());
        map.put("la", this.f215c.m21d());
        if (!TextUtils.isEmpty(this.f215c.m18a())) {
            map.put("mcc", this.f215c.m18a());
        }
        if (!TextUtils.isEmpty(this.f215c.m19b())) {
            map.put("mnc", this.f215c.m19b());
        }
        map.put("dm", this.f215c.m24g());
        map.put("auid", this.f214b.getDeviceId());
        if (this.f214b.isUseAnonymizeIp()) {
            map.put("aip", "1");
            String overrideIp = this.f214b.getOverrideIp();
            if (overrideIp != null) {
                map.put("oip", overrideIp);
            }
        }
        if (!TextUtils.isEmpty(this.f214b.getUserId())) {
            map.put("uid", this.f214b.getUserId());
        }
        map.put("do", this.f215c.m22e());
        map.put("av", this.f215c.m25h());
        map.put("uv", this.f214b.getVersion());
        map.put("tz", this.f215c.m26i());
        map.put("at", String.valueOf(this.f214b.getAuidType()));
        map.put("fv", this.f215c.m27j());
        return map;
    }

    /* renamed from: a */
    protected boolean m182a() {
        if (!TextUtils.isEmpty(this.f214b.getDeviceId())) {
            return true;
        }
        C0798a.m150a("Log Sender", "Device id is empty");
        return false;
    }

    /* renamed from: b */
    protected String m183b(Map<String, String> map) {
        return this.f216d.m158a(map, C0799a.ONE_DEPTH);
    }

    /* renamed from: c */
    protected void m184c(Map<String, String> map) {
        this.f217e.m123a(Long.valueOf((String) map.get("ts")).longValue(), (String) map.get("t"), m183b(mo1689a(map)));
    }
}

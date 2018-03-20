package com.samsung.context.sdk.samsunganalytics.p000a.p006f;

import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.f.a */
public class C0784a {
    /* renamed from: b */
    private static final String f104b = "AndroidCAStore";
    /* renamed from: c */
    private static final String f105c = "TLS";
    /* renamed from: a */
    private SSLContext f106a;

    /* renamed from: com.samsung.context.sdk.samsunganalytics.a.f.a$a */
    private static class C0783a {
        /* renamed from: a */
        private static final C0784a f103a = new C0784a();

        private C0783a() {
        }
    }

    private C0784a() {
        m89c();
    }

    /* renamed from: a */
    public static C0784a m88a() {
        return C0783a.f103a;
    }

    /* renamed from: c */
    private void m89c() {
        try {
            KeyStore instance = KeyStore.getInstance(KeyStore.getDefaultType());
            instance.load(null, null);
            KeyStore instance2 = KeyStore.getInstance(f104b);
            instance2.load(null, null);
            Enumeration aliases = instance2.aliases();
            while (aliases.hasMoreElements()) {
                String str = (String) aliases.nextElement();
                X509Certificate x509Certificate = (X509Certificate) instance2.getCertificate(str);
                if (str.startsWith("system:")) {
                    instance.setCertificateEntry(str, x509Certificate);
                }
            }
            TrustManagerFactory instance3 = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            instance3.init(instance);
            this.f106a = SSLContext.getInstance(f105c);
            this.f106a.init(null, instance3.getTrustManagers(), null);
            C0798a.m149a("pinning success");
        } catch (Exception e) {
            C0798a.m149a("pinning fail : " + e.getMessage());
        }
    }

    /* renamed from: b */
    public SSLContext m90b() {
        return this.f106a;
    }
}

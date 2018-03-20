package com.samsung.context.sdk.samsunganalytics.p000a.p005e;

import android.content.SharedPreferences;
import android.net.Uri;
import android.net.Uri.Builder;
import android.text.TextUtils;
import com.samsung.context.sdk.samsunganalytics.p000a.C0763a;
import com.samsung.context.sdk.samsunganalytics.p000a.p001a.C0759a;
import com.samsung.context.sdk.samsunganalytics.p000a.p001a.C0760b;
import com.samsung.context.sdk.samsunganalytics.p000a.p001a.C0761c;
import com.samsung.context.sdk.samsunganalytics.p000a.p004d.C0774b;
import com.samsung.context.sdk.samsunganalytics.p000a.p006f.C0784a;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONException;
import org.json.JSONObject;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.e.c */
public class C0984c implements C0774b {
    /* renamed from: a */
    private String f200a;
    /* renamed from: b */
    private Map<String, String> f201b;
    /* renamed from: c */
    private C0759a f202c;
    /* renamed from: d */
    private HttpsURLConnection f203d = null;
    /* renamed from: e */
    private SharedPreferences f204e;
    /* renamed from: f */
    private C0763a<Void, Boolean> f205f;

    public C0984c(C0759a c0759a, String str, Map<String, String> map, SharedPreferences sharedPreferences, C0763a<Void, Boolean> c0763a) {
        this.f200a = str;
        this.f202c = c0759a;
        this.f201b = map;
        this.f204e = sharedPreferences;
        this.f205f = c0763a;
    }

    /* renamed from: a */
    private void m173a(BufferedReader bufferedReader) {
        if (bufferedReader != null) {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                return;
            }
        }
        if (this.f203d != null) {
            this.f203d.disconnect();
        }
    }

    /* renamed from: a */
    public void mo1451a() {
        try {
            Builder buildUpon = Uri.parse(this.f202c.m10a()).buildUpon();
            for (String str : this.f201b.keySet()) {
                buildUpon.appendQueryParameter(str, (String) this.f201b.get(str));
            }
            String str2 = SimpleDateFormat.getTimeInstance(2, Locale.US).format(new Date());
            buildUpon.appendQueryParameter("ts", str2).appendQueryParameter("tid", this.f200a).appendQueryParameter("hc", C0781e.m82a(this.f200a + str2 + C0781e.f101c));
            this.f203d = (HttpsURLConnection) new URL(buildUpon.build().toString()).openConnection();
            this.f203d.setSSLSocketFactory(C0784a.m88a().m90b().getSocketFactory());
            this.f203d.setRequestMethod(this.f202c.m11b());
            this.f203d.setConnectTimeout(C0779b.f96p);
        } catch (Exception e) {
            C0798a.m155e("Fail to get Policy");
        }
    }

    /* renamed from: a */
    public void m175a(JSONObject jSONObject) {
        try {
            this.f204e.edit().putInt(C0779b.f87g, jSONObject.getInt(C0779b.f87g) * 1024).putInt(C0779b.f83c, jSONObject.getInt(C0779b.f83c) * 1024).putInt(C0779b.f86f, jSONObject.getInt(C0779b.f86f) * 1024).putInt(C0779b.f82b, jSONObject.getInt(C0779b.f82b) * 1024).putString(C0779b.f88h, "https://" + jSONObject.getString(C0779b.f88h)).putString("uri", jSONObject.getString("uri")).putString(C0779b.f90j, jSONObject.getString(C0779b.f90j)).putString(C0779b.f91k, jSONObject.getString(C0779b.f91k)).putInt(C0779b.f81a, jSONObject.getInt(C0779b.f81a)).putLong(C0779b.f94n, System.currentTimeMillis()).apply();
            C0761c.DLS.m15a("https://" + jSONObject.getString(C0779b.f88h));
            C0760b.DLS_DIR.m13a(jSONObject.getString("uri"));
            C0760b.DLS_DIR_BAT.m13a(jSONObject.getString(C0779b.f90j));
            C0798a.m149a("dq-3g: " + (jSONObject.getInt(C0779b.f83c) * 1024) + ", dq-w: " + (jSONObject.getInt(C0779b.f82b) * 1024) + ", oq-3g: " + (jSONObject.getInt(C0779b.f87g) * 1024) + ", oq-w: " + (jSONObject.getInt(C0779b.f86f) * 1024));
        } catch (JSONException e) {
            C0798a.m155e("Fail to get Policy");
            C0798a.m149a("[GetPolicyClient] " + e.getMessage());
        }
    }

    /* renamed from: b */
    public int mo1452b() {
        BufferedReader bufferedReader;
        Throwable th;
        int i = 0;
        BufferedReader bufferedReader2 = null;
        try {
            if (this.f203d.getResponseCode() != 200) {
                C0798a.m155e("Fail to get Policy. Response code : " + this.f203d.getResponseCode());
                i = -61;
            }
            bufferedReader = new BufferedReader(new InputStreamReader(this.f203d.getInputStream()));
            try {
                JSONObject jSONObject = new JSONObject(bufferedReader.readLine());
                int i2 = jSONObject.getInt("rc");
                if (i2 != 1000) {
                    C0798a.m155e("Fail to get Policy; Invalid Message. Result code : " + i2);
                    i2 = -61;
                } else {
                    C0798a.m150a("GetPolicyClient", "Get Policy Success");
                    if (TextUtils.isEmpty(this.f204e.getString(C0779b.f91k, "")) && this.f205f != null) {
                        String string = jSONObject.getString(C0779b.f91k);
                        if (string != null && string.equals(C0779b.f93m)) {
                            this.f205f.mo1450a(Boolean.valueOf(true));
                        }
                    }
                    m175a(jSONObject);
                    i2 = i;
                }
                if (this.f203d != null) {
                    this.f203d.disconnect();
                }
                m173a(bufferedReader);
                return i2;
            } catch (Exception e) {
                bufferedReader2 = bufferedReader;
                try {
                    C0798a.m155e("Fail to get Policy");
                    m173a(bufferedReader2);
                    return -61;
                } catch (Throwable th2) {
                    bufferedReader = bufferedReader2;
                    th = th2;
                    m173a(bufferedReader);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                m173a(bufferedReader);
                throw th;
            }
        } catch (Exception e2) {
            C0798a.m155e("Fail to get Policy");
            m173a(bufferedReader2);
            return -61;
        } catch (Throwable th22) {
            bufferedReader = null;
            th = th22;
            m173a(bufferedReader);
            throw th;
        }
    }
}

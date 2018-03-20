package com.samsung.context.sdk.samsunganalytics.p000a.p007g.p014b;

import android.net.Uri;
import android.net.Uri.Builder;
import android.text.TextUtils;
import com.samsung.context.sdk.samsunganalytics.p000a.p001a.C0759a;
import com.samsung.context.sdk.samsunganalytics.p000a.p004d.C0773a;
import com.samsung.context.sdk.samsunganalytics.p000a.p004d.C0774b;
import com.samsung.context.sdk.samsunganalytics.p000a.p005e.C0781e;
import com.samsung.context.sdk.samsunganalytics.p000a.p006f.C0784a;
import com.samsung.context.sdk.samsunganalytics.p000a.p007g.C0796d;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0800b;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import java.util.zip.GZIPOutputStream;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.g.b.a */
public class C0988a implements C0774b {
    /* renamed from: a */
    private static final C0759a f219a = C0759a.SEND_LOG;
    /* renamed from: b */
    private static final C0759a f220b = C0759a.SEND_BUFFERED_LOG;
    /* renamed from: c */
    private static final int f221c = 3000;
    /* renamed from: d */
    private static final int f222d = 15000;
    /* renamed from: e */
    private Queue<C0796d> f223e;
    /* renamed from: f */
    private C0796d f224f;
    /* renamed from: g */
    private String f225g;
    /* renamed from: h */
    private HttpsURLConnection f226h = null;
    /* renamed from: i */
    private C0773a f227i;
    /* renamed from: j */
    private Boolean f228j = Boolean.valueOf(false);
    /* renamed from: k */
    private int f229k;

    public C0988a(C0796d c0796d, String str, int i, C0773a c0773a) {
        this.f224f = c0796d;
        this.f225g = str;
        this.f227i = c0773a;
        this.f229k = m185a(i);
    }

    public C0988a(Queue<C0796d> queue, String str, int i, C0773a c0773a) {
        this.f223e = queue;
        this.f225g = str;
        this.f227i = c0773a;
        this.f228j = Boolean.valueOf(true);
        this.f229k = m185a(i);
    }

    /* renamed from: a */
    private int m185a(int i) {
        return i == 0 ? 3000 : i > f222d ? f222d : i;
    }

    /* renamed from: a */
    private void m186a(int i, String str) {
        if (this.f227i != null) {
            if (i != 200 || !str.equalsIgnoreCase("1000")) {
                if (this.f228j.booleanValue()) {
                    while (!this.f223e.isEmpty()) {
                        C0796d c0796d = (C0796d) this.f223e.poll();
                        this.f227i.mo1456b(i, c0796d.m141b() + "", c0796d.m143c());
                    }
                    return;
                }
                this.f227i.mo1456b(i, this.f224f.m141b() + "", this.f224f.m143c());
            }
        }
    }

    /* renamed from: a */
    private void m187a(BufferedReader bufferedReader) {
        if (bufferedReader != null) {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                return;
            }
        }
        if (this.f226h != null) {
            this.f226h.disconnect();
        }
    }

    /* renamed from: c */
    private String m188c() {
        if (!this.f228j.booleanValue()) {
            return this.f224f.m143c();
        }
        Iterator it = this.f223e.iterator();
        String c = ((C0796d) it.next()).m143c();
        while (it.hasNext()) {
            c = c + C0800b.f173a + ((C0796d) it.next()).m143c();
        }
        return c;
    }

    /* renamed from: a */
    public void mo1451a() {
        try {
            C0759a c0759a = this.f228j.booleanValue() ? f220b : f219a;
            Builder buildUpon = Uri.parse(c0759a.m10a()).buildUpon();
            String format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm").format(new Date());
            buildUpon.appendQueryParameter("ts", format).appendQueryParameter("tid", this.f225g).appendQueryParameter("hc", C0781e.m82a(this.f225g + format + C0781e.f101c));
            this.f226h = (HttpsURLConnection) new URL(buildUpon.build().toString()).openConnection();
            this.f226h.setSSLSocketFactory(C0784a.m88a().m90b().getSocketFactory());
            this.f226h.setRequestMethod(c0759a.m11b());
            this.f226h.addRequestProperty("Content-Encoding", this.f228j.booleanValue() ? "gzip" : "text");
            this.f226h.setConnectTimeout(this.f229k);
            String c = m188c();
            if (!TextUtils.isEmpty(c)) {
                this.f226h.setDoOutput(true);
                OutputStream bufferedOutputStream = this.f228j.booleanValue() ? new BufferedOutputStream(new GZIPOutputStream(this.f226h.getOutputStream())) : new BufferedOutputStream(this.f226h.getOutputStream());
                bufferedOutputStream.write(c.getBytes());
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
            }
            C0798a.m149a("[DLS Client] Send to DLS : " + c);
        } catch (Exception e) {
            C0798a.m155e("[DLS Client] Send fail.");
            C0798a.m149a("[DLS Client] " + e.getMessage());
        }
    }

    /* renamed from: b */
    public int mo1452b() {
        BufferedReader bufferedReader;
        int i;
        Exception e;
        Throwable th;
        try {
            int responseCode = this.f226h.getResponseCode();
            bufferedReader = new BufferedReader(new InputStreamReader(this.f226h.getInputStream()));
            try {
                String string = new JSONObject(bufferedReader.readLine()).getString("rc");
                if (responseCode == 200 && string.equalsIgnoreCase("1000")) {
                    i = 1;
                    C0798a.m154d("[DLS Sender] send result success : " + responseCode + " " + string);
                } else {
                    i = -7;
                    C0798a.m154d("[DLS Sender] send result fail : " + responseCode + " " + string);
                }
                m186a(responseCode, string);
                m187a(bufferedReader);
            } catch (Exception e2) {
                e = e2;
                try {
                    C0798a.m155e("[DLS Client] Send fail.");
                    C0798a.m149a("[DLS Client] " + e.getMessage());
                    i = -41;
                    m186a(0, "");
                    m187a(bufferedReader);
                    return i;
                } catch (Throwable th2) {
                    th = th2;
                    m187a(bufferedReader);
                    throw th;
                }
            }
        } catch (Exception e3) {
            e = e3;
            bufferedReader = null;
            C0798a.m155e("[DLS Client] Send fail.");
            C0798a.m149a("[DLS Client] " + e.getMessage());
            i = -41;
            m186a(0, "");
            m187a(bufferedReader);
            return i;
        } catch (Throwable th3) {
            th = th3;
            bufferedReader = null;
            m187a(bufferedReader);
            throw th;
        }
        return i;
    }
}

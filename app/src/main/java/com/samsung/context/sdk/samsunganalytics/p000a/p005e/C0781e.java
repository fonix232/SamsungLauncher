package com.samsung.context.sdk.samsunganalytics.p000a.p005e;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build.VERSION;
import android.os.UserManager;
import android.text.TextUtils;
import com.samsung.context.sdk.samsunganalytics.Configuration;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0802d;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;

/* renamed from: com.samsung.context.sdk.samsunganalytics.a.e.e */
public class C0781e {
    /* renamed from: a */
    public static final int f99a = 0;
    /* renamed from: b */
    public static final int f100b = -1001;
    /* renamed from: c */
    public static String f101c = "RSSAV1wsc2s314SAamk";
    /* renamed from: d */
    private static HashMap<String, Integer> f102d;

    private C0781e() {
    }

    /* renamed from: a */
    public static int m81a(String str, String str2) {
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            return -1001;
        }
        if (f102d == null) {
            C0781e.m86b();
        }
        Integer num = (Integer) f102d.get(str);
        if (num == null || num.intValue() >= str2.length()) {
            return 0;
        }
        C0798a.m155e("Invalid length : " + str2);
        C0798a.m155e("MAX length : " + num);
        return -1001;
    }

    /* renamed from: a */
    public static String m82a(String str) {
        String format;
        Exception e;
        if (str == null) {
            return null;
        }
        try {
            MessageDigest.getInstance("SHA-256").update(str.getBytes("UTF-8"));
            format = String.format(Locale.US, "%064x", new Object[]{new BigInteger(1, r0.digest())});
        } catch (NoSuchAlgorithmException e2) {
            e = e2;
            C0798a.m148a(C0781e.class, e);
            format = null;
            return format;
        } catch (UnsupportedEncodingException e3) {
            e = e3;
            C0798a.m148a(C0781e.class, e);
            format = null;
            return format;
        }
        return format;
    }

    /* renamed from: a */
    public static boolean m83a() {
        String str;
        String str2;
        if (VERSION.SDK_INT > 23) {
            str = "com.samsung.android.feature.SemFloatingFeature";
            str2 = "getBoolean";
        } else {
            str = "com.samsung.android.feature.FloatingFeature";
            str2 = "getEnableStatus";
        }
        try {
            Class cls = Class.forName(str);
            Object invoke = cls.getMethod("getInstance", null).invoke(null, new Object[0]);
            boolean booleanValue = ((Boolean) cls.getMethod(str2, new Class[]{String.class}).invoke(invoke, new Object[]{"SEC_FLOATING_FEATURE_CONTEXTSERVICE_ENABLE_SURVEY_MODE"})).booleanValue();
            if (booleanValue) {
                C0798a.m154d("cf feature is supported");
                return booleanValue;
            }
            C0798a.m154d("feature is not supported");
            return booleanValue;
        } catch (Exception e) {
            C0798a.m154d("Floating feature is not supported (non-samsung device)");
            C0798a.m148a(C0781e.class, e);
            return false;
        }
    }

    /* renamed from: a */
    public static boolean m84a(Context context, Configuration configuration) {
        if (context == null) {
            C0802d.m162a("context cannot be null");
            return false;
        } else if (configuration == null) {
            C0802d.m162a("Configuration cannot be null");
            return false;
        } else if (!TextUtils.isEmpty(configuration.getDeviceId()) || configuration.isEnableAutoDeviceId()) {
            if (configuration.isEnableUseInAppLogging()) {
                if (configuration.getUserAgreement() == null) {
                    C0802d.m162a("If you want to use In App Logging, you should implement UserAgreement interface");
                    return false;
                }
            } else if (!C0781e.m85a(context, "com.sec.spp.permission.TOKEN", false)) {
                C0802d.m162a("If you want to use DLC Logger, define 'com.sec.spp.permission.TOKEN_XXXX' permission in AndroidManifest");
                return false;
            } else if (!TextUtils.isEmpty(configuration.getDeviceId())) {
                C0802d.m162a("This mode is not allowed to set device Id");
                return false;
            } else if (!TextUtils.isEmpty(configuration.getUserId())) {
                C0802d.m162a("This mode is not allowed to set user Id");
                return false;
            }
            if (configuration.getVersion() == null) {
                C0802d.m162a("you should set the version");
                return false;
            }
            if (VERSION.SDK_INT >= 24) {
                UserManager userManager = (UserManager) context.getSystemService("user");
                if (!(userManager == null || userManager.isUserUnlocked())) {
                    C0798a.m155e("user did not unlock");
                    return false;
                }
            }
            return true;
        } else {
            C0802d.m162a("Device Id is empty, set Device Id or enable auto device id");
            return false;
        }
    }

    /* renamed from: a */
    public static boolean m85a(Context context, String str, boolean z) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 4096);
            if (packageInfo.requestedPermissions != null) {
                for (String str2 : packageInfo.requestedPermissions) {
                    if (z) {
                        if (str2.equalsIgnoreCase(str)) {
                            return true;
                        }
                    } else if (str2.startsWith(str)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            C0798a.m148a(C0781e.class, e);
        }
        return false;
    }

    /* renamed from: b */
    private static void m86b() {
        f102d = new HashMap();
        f102d.put("pn", Integer.valueOf(100));
        f102d.put("pnd", Integer.valueOf(400));
        f102d.put("en", Integer.valueOf(100));
        f102d.put("ed", Integer.valueOf(400));
        f102d.put("exm", Integer.valueOf(400));
        f102d.put("exd", Integer.valueOf(1000));
        f102d.put("sti", Integer.valueOf(1000));
        f102d.put("cd", Integer.valueOf(1000));
        f102d.put("cm", Integer.valueOf(1000));
    }
}

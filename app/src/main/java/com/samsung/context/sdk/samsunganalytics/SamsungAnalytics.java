package com.samsung.context.sdk.samsunganalytics;

import android.app.Application;
import android.content.Context;
import com.samsung.context.sdk.samsunganalytics.p000a.C0769b;
import com.samsung.context.sdk.samsunganalytics.p000a.p005e.C0781e;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0798a;
import com.samsung.context.sdk.samsunganalytics.p000a.p013i.C0802d;
import java.util.Map;
import java.util.Set;

public class SamsungAnalytics {
    public static final String SDK_VERSION = "1.10.038";
    private static SamsungAnalytics instance;
    private C0769b tracker = null;

    private SamsungAnalytics(Application application, Configuration configuration) {
        if (!C0781e.m84a((Context) application, configuration)) {
            return;
        }
        if (configuration.isEnableUseInAppLogging()) {
            this.tracker = new C0769b(application, configuration);
        } else if (C0781e.m83a()) {
            this.tracker = new C0769b(application, configuration);
        }
    }

    public static Configuration getConfiguration() {
        return (instance == null || instance.tracker == null) ? null : instance.tracker.m54g();
    }

    public static SamsungAnalytics getInstance() {
        if (instance == null) {
            C0802d.m162a("call after setConfiguration() method");
            if (!C0802d.m163a()) {
                return getInstanceAndConfig(null, null);
            }
        }
        return instance;
    }

    private static SamsungAnalytics getInstanceAndConfig(Application application, Configuration configuration) {
        if (instance == null) {
            synchronized (SamsungAnalytics.class) {
                instance = new SamsungAnalytics(application, configuration);
            }
        }
        return instance;
    }

    public static void setConfiguration(Application application, Configuration configuration) {
        getInstanceAndConfig(application, configuration);
    }

    public void disableAutoActivityTracking() {
        try {
            this.tracker.m51d();
        } catch (Exception e) {
            C0798a.m148a(getClass(), e);
        }
    }

    public void disableUncaughtExceptionLogging() {
        try {
            this.tracker.m49b();
        } catch (Exception e) {
            C0798a.m148a(getClass(), e);
        }
    }

    public SamsungAnalytics enableAutoActivityTracking() {
        try {
            this.tracker.m50c();
        } catch (Exception e) {
            C0798a.m148a(getClass(), e);
        }
        return this;
    }

    public SamsungAnalytics enableUncaughtExceptionLogging() {
        try {
            this.tracker.m47a();
        } catch (Exception e) {
            C0798a.m148a(getClass(), e);
        }
        return this;
    }

    public boolean isEnableAutoActivityTracking() {
        try {
            return this.tracker.m53f();
        } catch (Exception e) {
            C0798a.m148a(getClass(), e);
            return false;
        }
    }

    public boolean isEnableUncaughtExceptionLogging() {
        try {
            return this.tracker.m52e();
        } catch (Exception e) {
            C0798a.m148a(getClass(), e);
            return false;
        }
    }

    public void registerSettingPref(Map<String, Set<String>> map) {
        try {
            this.tracker.m48a((Map) map);
        } catch (Exception e) {
            C0798a.m148a(getClass(), e);
        }
    }

    public void restrictNetworkType(int i) {
        try {
            this.tracker.m54g().setRestrictedNetworkType(i);
        } catch (Exception e) {
            C0798a.m148a(getClass(), e);
        }
    }

    public int sendLog(Map<String, String> map) {
        try {
            return this.tracker.m46a((Map) map, false);
        } catch (Exception e) {
            C0798a.m148a(getClass(), e);
            return -100;
        }
    }

    public int sendLogSync(Map<String, String> map) {
        try {
            return this.tracker.m46a((Map) map, true);
        } catch (Exception e) {
            C0798a.m148a(getClass(), e);
            return -100;
        }
    }
}

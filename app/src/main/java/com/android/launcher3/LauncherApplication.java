package com.android.launcher3;

import android.app.Application;
import android.content.Context;
import com.android.launcher3.util.logging.SALogging;
import com.samsung.android.sdk.bixby.BixbyApi;
import com.samsung.android.sdk.bixby.BixbyApi.InterimStateListener;
import com.samsung.android.sdk.bixby.BixbyApi.ResponseResults;
import com.samsung.android.sdk.bixby.BixbyApi.StartStateListener;
import com.samsung.android.sdk.bixby.data.ParamFilling;
import com.samsung.android.sdk.bixby.data.ScreenStateInfo;
import com.samsung.android.sdk.bixby.data.State;
import com.squareup.leakcanary.LeakCanary;

public class LauncherApplication extends Application {
    private final String mAppName = "Home";

    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
        SALogging.getInstance().init(this);
    }

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        final BixbyApi executorMediator = BixbyApi.createInstance(base, "Home");
        executorMediator.setStartStateListener(new StartStateListener() {
            public void onRuleCanceled(String s) {
            }

            public void onStateReceived(State state) {
                executorMediator.sendResponse(ResponseResults.FAILURE);
            }
        });
        executorMediator.setInterimStateListener(new InterimStateListener() {
            public ScreenStateInfo onScreenStatesRequested() {
                return null;
            }

            public boolean onParamFillingReceived(ParamFilling paramFilling) {
                return false;
            }

            public void onRuleCanceled(String s) {
            }

            public void onStateReceived(State state) {
                executorMediator.sendResponse(ResponseResults.FAILURE);
            }
        });
    }
}

package com.android.launcher3.util;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings.Global;
import android.util.Log;

public class NavigationBarPolicy {
    private static final String IMMERSIVE_POSTFIX = ",-com.sec.android.app.launcher";
    private static final String IMMERSIVE_PRFIX_FULL = "immersive.full=";
    private static final String IMMERSIVE_PRFIX_NAVI = "immersive.navigation=";
    private static final String POLICY_URI = "policy_control";
    private static final String TAG = "NavigationBarPolicy";
    private final Context mContext;
    private String mCurrentPolicy;
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (NavigationBarPolicy.this.isDetected()) {
                NavigationBarPolicy.this.setOn();
            }
        }
    };

    public NavigationBarPolicy(Context context) {
        this.mContext = context;
    }

    public boolean isDetected() {
        this.mCurrentPolicy = Global.getString(this.mContext.getContentResolver(), POLICY_URI);
        Log.d(TAG, "isDetected, current policy : " + this.mCurrentPolicy);
        return this.mCurrentPolicy != null && ((this.mCurrentPolicy.startsWith(IMMERSIVE_PRFIX_FULL) || this.mCurrentPolicy.startsWith(IMMERSIVE_PRFIX_NAVI)) && !this.mCurrentPolicy.contains(IMMERSIVE_POSTFIX));
    }

    public void setOn() {
        Global.putString(this.mContext.getContentResolver(), POLICY_URI, this.mCurrentPolicy + IMMERSIVE_POSTFIX);
    }

    public void registerObserver() {
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor(POLICY_URI), true, this.mObserver);
    }

    public void unRegisterObserver() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mObserver);
    }
}

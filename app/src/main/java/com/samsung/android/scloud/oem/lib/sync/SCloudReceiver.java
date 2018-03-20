package com.samsung.android.scloud.oem.lib.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.samsung.android.scloud.oem.lib.LOG;
import java.util.Map.Entry;

public class SCloudReceiver extends BroadcastReceiver {
    public static final String ACCOUNT_SIGNED_IN = "android.intent.action.SAMSUNGACCOUNT_SIGNIN_COMPLETED";
    public static final String ACCOUNT_SIGNED_IN_FIXED = "com.samsung.account.SAMSUNGACCOUNT_SIGNIN_COMPLETED";
    public static final String ACCOUNT_SIGNED_OUT = "android.intent.action.SAMSUNGACCOUNT_SIGNOUT_COMPLETED";
    public static final String ACCOUNT_SIGNED_OUT_FIXED = "com.samsung.account.SAMSUNGACCOUNT_SIGNOUT_COMPLETED";
    public static final String ACCOUNT_TYPE = "com.osp.app.signin";
    public static final String SETUPWIZARD_COMPLETE = "com.sec.android.app.secsetupwizard.SETUPWIZARD_COMPLETE";
    private static final String TAG = "SCloudReceiver";

    public void onReceive(final Context ctx, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            LOG.f(TAG, "onReceive : " + action);
            AccountManager am;
            if (ACCOUNT_SIGNED_IN.equals(action) || ACCOUNT_SIGNED_IN_FIXED.equals(action)) {
                am = AccountManager.get(ctx);
                if (am != null) {
                    final Account[] accounts = am.getAccountsByType(ACCOUNT_TYPE);
                    if (accounts != null && accounts.length >= 1) {
                        new Thread(new Runnable() {
                            public void run() {
                                LOG.f(SCloudReceiver.TAG, "account signed in - clear scloud meta : start -" + ctx.getPackageName());
                                ctx.getSharedPreferences("sync_meta", 0).edit().clear().commit();
                                for (Entry<String, ISCloudSyncClient> item : SyncClientHelper.getInstance(ctx).getClientMap().entrySet()) {
                                    ((ISCloudSyncClient) item.getValue()).accountSignedIn(ctx, accounts[0]);
                                }
                                LOG.f(SCloudReceiver.TAG, "account signed in - clear scloud meta : finished - " + ctx.getPackageName());
                            }
                        }).start();
                    }
                }
            } else if (ACCOUNT_SIGNED_OUT.equals(action) || ACCOUNT_SIGNED_OUT_FIXED.equals(action)) {
                am = AccountManager.get(ctx);
                if (am == null) {
                    return;
                }
                if (am.getAccountsByType(ACCOUNT_TYPE).length == 0) {
                    new Thread(new Runnable() {
                        public void run() {
                            LOG.f(SCloudReceiver.TAG, "account signed out - clear scloud meta : start -" + ctx.getPackageName());
                            ctx.getSharedPreferences("sync_meta", 0).edit().clear().commit();
                            for (Entry<String, ISCloudSyncClient> item : SyncClientHelper.getInstance(ctx).getClientMap().entrySet()) {
                                ((ISCloudSyncClient) item.getValue()).accountSignedOut(ctx, SCloudReceiver.ACCOUNT_TYPE);
                            }
                            LOG.f(SCloudReceiver.TAG, "account signed out - clear scloud meta : finished - " + ctx.getPackageName());
                        }
                    }).start();
                } else {
                    LOG.f(TAG, "account signed out - there is an account.. ignore signed out intent - " + ctx.getPackageName());
                }
            } else {
                LOG.i(TAG, "Unknown intent received: " + action);
            }
        }
    }
}

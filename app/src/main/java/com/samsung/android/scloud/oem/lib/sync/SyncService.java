package com.samsung.android.scloud.oem.lib.sync;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.IBinder;
import com.samsung.android.scloud.oem.lib.LOG;
import com.samsung.android.scloud.oem.lib.SCloudUtil;

public class SyncService extends Service {
    private static final String SCLOUD_INTERFACE_PROVIDER = "com.samsung.android.scloud.oem.lib.sync.SyncClientProivder";
    private static final String SCLOUD_PACKAGE = "com.samsung.android.scloud";
    private static final String SCLOUD_SYNC_PACKAGE = "com.samsung.android.scloud.sync";
    private static final String TAG = "SCloud-SyncService";
    private static SyncAdapter sSyncAdapter = null;
    private static final Object sSyncAdapterLock = new Object();
    private boolean mSupportSCloud = true;

    public void onCreate() {
        synchronized (sSyncAdapterLock) {
            LOG.f(TAG, "onCreate");
            if (sSyncAdapter == null) {
                sSyncAdapter = new SyncAdapter(getApplicationContext(), true);
                try {
                    getPackageManager().getPackageInfo(SCLOUD_PACKAGE, 128);
                } catch (NameNotFoundException e) {
                    try {
                        getPackageManager().getPackageInfo(SCLOUD_SYNC_PACKAGE, 128);
                    } catch (NameNotFoundException e2) {
                        this.mSupportSCloud = false;
                        LOG.f(TAG, "SCloud package is not found !!..");
                    }
                }
                if (this.mSupportSCloud) {
                    try {
                        getPackageManager().setComponentEnabledSetting(new ComponentName(getPackageName(), SCLOUD_INTERFACE_PROVIDER), 1, 1);
                        LOG.f(TAG, "SCloud package is found... enable sync provider");
                    } catch (Exception e3) {
                        LOG.e(TAG, "enable err", e3);
                        this.mSupportSCloud = false;
                    }
                }
            }
        }
    }

    public IBinder onBind(Intent intent) {
        LOG.f(TAG, "onBind");
        if (intent != null) {
            LOG.i(TAG, "action : " + intent.getAction());
            if (intent.getExtras() != null) {
                for (String key : intent.getExtras().keySet()) {
                    LOG.i(TAG, "intent bundle - " + key + ":" + intent.getExtras().get(key));
                }
            }
        }
        if (!this.mSupportSCloud) {
            LOG.f(TAG, "set sync invisible.. SCloud package is not found !!..");
            SCloudUtil.visibleSync(SCloudUtil.getSamsungAccount(this), SyncClientHelper.getInstance(this).getContentAuthority(), false);
        } else if (SyncClientHelper.getInstance(this).isSyncable()) {
            boolean isSyncable = true;
            for (ISCloudSyncClient client : SyncClientHelper.getInstance(this).getClientMap().values()) {
                if (!client.isSyncable(this)) {
                    isSyncable = false;
                    break;
                }
            }
            if (!isSyncable) {
                LOG.f(TAG, "set sync invisible. from ISCloudSyncClient config");
                SCloudUtil.visibleSync(SCloudUtil.getSamsungAccount(this), SyncClientHelper.getInstance(this).getContentAuthority(), false);
            }
        } else {
            LOG.f(TAG, "set sync invisible. from xml config");
            SCloudUtil.visibleSync(SCloudUtil.getSamsungAccount(this), SyncClientHelper.getInstance(this).getContentAuthority(), false);
        }
        return sSyncAdapter.getSyncAdapterBinder();
    }
}

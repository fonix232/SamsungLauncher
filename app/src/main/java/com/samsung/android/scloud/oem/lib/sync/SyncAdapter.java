package com.samsung.android.scloud.oem.lib.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SyncResult;
import android.content.SyncStats;
import android.net.Uri;
import android.os.Bundle;
import com.samsung.android.scloud.oem.lib.LOG;
import java.util.Map.Entry;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    static final String REQUEST_CANCEL = "request_cancel";
    static final String REQUEST_SYNC = "request_sync";
    static final Uri SCLOUD_SYNC_URI = Uri.parse("content://com.samsung.android.scloud.sync.vendor");
    private static final String TAG = "SCloud-SyncAdapter";
    private boolean isCanceled = false;
    private boolean isInProgress = false;
    private SharedPreferences mSyncMeta;

    interface Key {
        public static final String ACCOUNT_NAME = "account_name";
        public static final String AUTHORITY = "authority";
        public static final String CONTENTS_ID = "contents_id";
        public static final String CONTENT_URI = "content_uri";
        public static final String DATA_VERSION = "data_version";
        public static final String LAST_SYNC_TIME = "last_sync_time";
        public static final String META_FILE = "sync_meta";
        public static final String NAME = "name";
        public static final String SYNC_RESULT = "sync_result";
    }

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        LOG.f(TAG, "SyncAdapter initialized : " + autoInitialize);
        this.mSyncMeta = context.getSharedPreferences("sync_meta", 0);
    }

    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        LOG.f(TAG, "onPerformSync - started, S-Cloud Sync Library ver : 3.0, pkg : " + getContext().getPackageName());
        this.isInProgress = true;
        if (extras != null) {
            for (String key : extras.keySet()) {
                LOG.i(TAG, "onPerformSync bundle - " + key + " : " + extras.get(key));
            }
        }
        SyncStats syncStats;
        try {
            for (Entry<String, ISCloudSyncClient> item : SyncClientHelper.getInstance(getContext()).getClientMap().entrySet()) {
                SharedPreferences sharedPreferences = this.mSyncMeta;
                SharedPreferences sharedPreferences2 = sharedPreferences;
                long lastSyncTime = sharedPreferences2.getLong("last_sync_time_" + ((String) item.getKey()), 0);
                String str = TAG;
                String str2 = str;
                LOG.f(str2, "onPerformSync - started. item : " + ((String) item.getKey()) + ", lastSyncTime : " + lastSyncTime);
                Bundle input = new Bundle();
                input.putString("name", (String) item.getKey());
                input.putString("account_name", account.name);
                input.putParcelable(Key.SYNC_RESULT, syncResult);
                input.putLong("last_sync_time", lastSyncTime);
                input.putInt("data_version", SyncClientHelper.getInstance(getContext()).getDataVersion((String) item.getKey()));
                input.putString(Key.CONTENT_URI, SyncClientHelper.getInstance(getContext()).getSupportSyncUri());
                input.putString("contents_id", SyncClientHelper.getInstance(getContext()).getContentsId());
                Bundle result = getContext().getContentResolver().call(SCLOUD_SYNC_URI, REQUEST_SYNC, null, input);
                long nextLastSyncTime = result.getLong("last_sync_time");
                SyncResult syncResultfromDataSync = (SyncResult) result.getParcelable(Key.SYNC_RESULT);
                if (nextLastSyncTime > 0) {
                    Editor edit = this.mSyncMeta.edit();
                    Editor editor = edit;
                    editor.putLong("last_sync_time_" + ((String) item.getKey()), nextLastSyncTime).commit();
                }
                syncResult.databaseError &= syncResultfromDataSync.databaseError;
                syncResult.delayUntil += syncResultfromDataSync.delayUntil;
                syncResult.fullSyncRequested &= syncResultfromDataSync.fullSyncRequested;
                syncResult.moreRecordsToGet &= syncResultfromDataSync.moreRecordsToGet;
                syncResult.partialSyncUnavailable &= syncResultfromDataSync.partialSyncUnavailable;
                syncStats = syncResult.stats;
                syncStats.numAuthExceptions += syncResultfromDataSync.stats.numAuthExceptions;
                syncStats = syncResult.stats;
                syncStats.numConflictDetectedExceptions += syncResultfromDataSync.stats.numConflictDetectedExceptions;
                syncStats = syncResult.stats;
                syncStats.numDeletes += syncResultfromDataSync.stats.numDeletes;
                syncStats = syncResult.stats;
                syncStats.numEntries += syncResultfromDataSync.stats.numEntries;
                syncStats = syncResult.stats;
                syncStats.numInserts += syncResultfromDataSync.stats.numInserts;
                syncStats = syncResult.stats;
                syncStats.numIoExceptions += syncResultfromDataSync.stats.numIoExceptions;
                syncStats = syncResult.stats;
                syncStats.numParseExceptions += syncResultfromDataSync.stats.numParseExceptions;
                syncStats = syncResult.stats;
                syncStats.numSkippedEntries += syncResultfromDataSync.stats.numSkippedEntries;
                syncStats = syncResult.stats;
                syncStats.numUpdates += syncResultfromDataSync.stats.numUpdates;
                syncResult.tooManyDeletions &= syncResultfromDataSync.tooManyDeletions;
                syncResult.tooManyRetries &= syncResultfromDataSync.tooManyRetries;
                str = TAG;
                str2 = str;
                LOG.f(str2, "onPerformSync - finished. item : " + ((String) item.getKey()) + ", nextLastSyncTime : " + nextLastSyncTime);
                if (!syncResult.hasError()) {
                    if (this.isCanceled) {
                        LOG.f(TAG, "sync canceled. skip other sync item.");
                        this.isCanceled = false;
                        break;
                    }
                }
                LOG.f(TAG, "sync result has error. skip other sync item.");
                break;
            }
            LOG.f(TAG, "onPerformSync - finished. proc : " + this.isInProgress + ", cancel : " + this.isCanceled + ", hasError : " + syncResult.hasError());
            this.isInProgress = false;
            if (this.isCanceled) {
                this.isCanceled = false;
            }
        } catch (Exception e) {
            LOG.e(TAG, "error on sync.. ", e);
            syncStats = syncResult.stats;
            syncStats.numAuthExceptions++;
            LOG.f(TAG, "onPerformSync - finished. proc : " + this.isInProgress + ", cancel : " + this.isCanceled + ", hasError : " + syncResult.hasError());
            this.isInProgress = false;
            if (this.isCanceled) {
                this.isCanceled = false;
            }
        } catch (Throwable th) {
            LOG.f(TAG, "onPerformSync - finished. proc : " + this.isInProgress + ", cancel : " + this.isCanceled + ", hasError : " + syncResult.hasError());
            this.isInProgress = false;
            if (this.isCanceled) {
                this.isCanceled = false;
            }
        }
    }

    public void onSyncCanceled() {
        super.onSyncCanceled();
        LOG.f(TAG, "onSyncCanceled - started. proc : " + this.isInProgress + ", cancel : " + this.isCanceled);
        if (this.isInProgress) {
            this.isCanceled = true;
            for (Entry<String, ISCloudSyncClient> item : SyncClientHelper.getInstance(getContext()).getClientMap().entrySet()) {
                LOG.f(TAG, "onSyncCanceled - started. item : " + ((String) item.getKey()));
                Bundle input = new Bundle();
                input.putString("name", (String) item.getKey());
                input.putString(Key.CONTENT_URI, SyncClientHelper.getInstance(getContext()).getSupportSyncUri());
                getContext().getContentResolver().call(SCLOUD_SYNC_URI, REQUEST_CANCEL, null, input);
                LOG.f(TAG, "onSyncCanceled - finished. item : " + ((String) item.getKey()));
            }
        }
        LOG.f(TAG, "onSyncCanceled - finished. proc : " + this.isInProgress + ", cancel : " + this.isCanceled);
    }
}

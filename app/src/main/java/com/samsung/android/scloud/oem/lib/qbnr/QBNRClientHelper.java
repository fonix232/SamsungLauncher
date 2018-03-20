package com.samsung.android.scloud.oem.lib.qbnr;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import com.samsung.android.scloud.oem.lib.LOG;
import com.samsung.android.scloud.oem.lib.bnr.BackupMetaManager;
import com.samsung.android.scloud.oem.lib.bnr.IBNRClientHelper;
import com.samsung.android.scloud.oem.lib.qbnr.ISCloudQBNRClient.QuickBackupListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QBNRClientHelper implements IBNRClientHelper {
    private static final String TAG = "QBNRClientHelper";
    private String category;
    private String contentsId;
    private ISCloudQBNRClient mClient;
    private boolean mIsFinished;
    private boolean mIsSuccess;
    private String mName;
    private long mProcNow;
    private long mProcTotal;
    private final Map<String, SyncServiceHandler> syncServiceHandlerMap = new HashMap();

    private interface Key {
        public static final String CATEGORY = "category";
        public static final String CONTENTS_ID = "contents_id";
        public static final String DESCRIPTION = "description";
        public static final String FILE = "file";
        public static final String IS_ENABLE_BACKUP = "is_enable_backup";
        public static final String IS_FINISHED = "is_finished";
        public static final String IS_FIRST_BACKUP = "is_first_backup";
        public static final String IS_SUCCESS = "is_success";
        public static final String LABEL = "label";
        public static final String NAME = "name";
        public static final String OBSERVING_URI = "observing_uri";
        public static final String PROGRESS = "progress";
        public static final String SUPPORT_BACKUP = "support_backup";
    }

    private interface METHOD {
        public static final String BACKUP = "backup";
        public static final String GET_CLIENT_INFO = "getClientInfo";
        public static final String GET_STATUS = "get_status";
        public static final String RESTORE = "restore";
    }

    private interface SyncServiceHandler {
        Bundle handleServiceAction(Context context, String str, Bundle bundle);
    }

    public QBNRClientHelper(Context context, String name, ISCloudQBNRClient client, String cid, String category) {
        LOG.f(TAG, "init QBNRClientHelper : " + name);
        this.mName = name;
        this.mClient = client;
        this.category = category;
        this.contentsId = cid;
        setHandlers();
    }

    private void init() {
        this.mProcNow = 0;
        this.mProcTotal = 0;
        this.mIsFinished = false;
        this.mIsSuccess = false;
    }

    public Bundle handleRequest(Context context, String method, String name, Bundle param) {
        if (this.syncServiceHandlerMap.containsKey(method)) {
            return ((SyncServiceHandler) this.syncServiceHandlerMap.get(method)).handleServiceAction(context, name, param);
        }
        return null;
    }

    void setHandlers() {
        this.syncServiceHandlerMap.put("getClientInfo", new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                LOG.f(QBNRClientHelper.TAG, "GET_CLIENT_INFO, " + name);
                boolean isFirstBackup = BackupMetaManager.getInstance(context).isFirstBackup(name);
                boolean isSupportBackup = QBNRClientHelper.this.mClient.isSupportBackup(context);
                boolean isEnableBackup = QBNRClientHelper.this.mClient.isEnableBackup(context);
                String label = QBNRClientHelper.this.mClient.getLabel(context);
                String description = QBNRClientHelper.this.mClient.getDescription(context);
                Bundle result = new Bundle();
                result.putBoolean("support_backup", isSupportBackup);
                result.putString("name", name);
                result.putBoolean("is_enable_backup", isEnableBackup);
                result.putBoolean("is_first_backup", isFirstBackup);
                result.putString("label", label);
                result.putString("description", description);
                result.putString("category", QBNRClientHelper.this.category);
                result.putString("contents_id", QBNRClientHelper.this.contentsId);
                LOG.d(QBNRClientHelper.TAG, "GET_CLIENT_INFO, " + name + ", " + QBNRClientHelper.this.contentsId + ", " + label + ", " + description + ", " + QBNRClientHelper.this.category);
                return result;
            }
        });
        this.syncServiceHandlerMap.put(METHOD.BACKUP, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                final Uri observingUri = Uri.parse(extras.getString(Key.OBSERVING_URI));
                final ParcelFileDescriptor file = (ParcelFileDescriptor) extras.getParcelable(Key.FILE);
                QBNRClientHelper.this.init();
                final Context context2 = context;
                final String str = name;
                new Thread(new Runnable() {
                    public void run() {
                        QBNRClientHelper.this.mClient.backup(context2, file, new QuickBackupListener() {
                            public void onProgress(long proc, long total) {
                                LOG.d(QBNRClientHelper.TAG, "onProgress -  proc : " + proc + " / " + total);
                                QBNRClientHelper.this.mProcNow = proc;
                                QBNRClientHelper.this.mProcTotal = total;
                                context2.getContentResolver().notifyChange(observingUri, null);
                            }

                            public void complete(boolean isSuccess) {
                                LOG.f(QBNRClientHelper.TAG, "BACKUP, " + str + ", complete - isSuccess : " + isSuccess);
                                QBNRClientHelper.this.mIsFinished = true;
                                QBNRClientHelper.this.mIsSuccess = isSuccess;
                                context2.getContentResolver().notifyChange(observingUri, null);
                                if (file != null) {
                                    try {
                                        file.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    }
                }, "BACKUP_" + name).start();
                return null;
            }
        });
        this.syncServiceHandlerMap.put(METHOD.RESTORE, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                final Uri observingUri = Uri.parse(extras.getString(Key.OBSERVING_URI));
                final ParcelFileDescriptor file = (ParcelFileDescriptor) extras.getParcelable(Key.FILE);
                QBNRClientHelper.this.init();
                final Context context2 = context;
                final String str = name;
                new Thread(new Runnable() {
                    public void run() {
                        QBNRClientHelper.this.mClient.restore(context2, file, new QuickBackupListener() {
                            public void onProgress(long proc, long total) {
                                LOG.d(QBNRClientHelper.TAG, "onProgress -  proc : " + proc + " / " + total);
                                QBNRClientHelper.this.mProcNow = proc;
                                QBNRClientHelper.this.mProcTotal = total;
                            }

                            public void complete(boolean isSuccess) {
                                LOG.f(QBNRClientHelper.TAG, "RESTORE, " + str + ", complete - isSuccess : " + isSuccess);
                                QBNRClientHelper.this.mIsFinished = true;
                                QBNRClientHelper.this.mIsSuccess = isSuccess;
                                Builder builder = observingUri.buildUpon();
                                builder.appendQueryParameter("is_success", QBNRClientHelper.this.mIsSuccess ? "1" : "0");
                                context2.getContentResolver().notifyChange(builder.build(), null);
                                if (file != null) {
                                    try {
                                        file.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    }
                }, "RESTORE_" + name).start();
                return null;
            }
        });
        this.syncServiceHandlerMap.put(METHOD.GET_STATUS, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                long j = 0;
                LOG.f(QBNRClientHelper.TAG, "GET_STATUS, " + name + ", is_finished : " + QBNRClientHelper.this.mIsFinished + ", is_success : " + QBNRClientHelper.this.mIsSuccess + ", proc : " + QBNRClientHelper.this.mProcNow + ", total : " + QBNRClientHelper.this.mProcTotal);
                Bundle result = new Bundle();
                result.putBoolean(Key.IS_FINISHED, QBNRClientHelper.this.mIsFinished);
                result.putBoolean("is_success", QBNRClientHelper.this.mIsSuccess);
                if (!QBNRClientHelper.this.mIsFinished) {
                    String str = "progress";
                    if (QBNRClientHelper.this.mProcTotal != 0) {
                        j = (QBNRClientHelper.this.mProcNow * 100) / QBNRClientHelper.this.mProcTotal;
                    }
                    result.putInt(str, (int) j);
                }
                return result;
            }
        });
    }
}

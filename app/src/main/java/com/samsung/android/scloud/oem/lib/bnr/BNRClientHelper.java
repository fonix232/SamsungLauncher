package com.samsung.android.scloud.oem.lib.bnr;

import android.content.Context;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import com.samsung.android.scloud.oem.lib.DownloadFileList;
import com.samsung.android.scloud.oem.lib.ItemSavedList;
import com.samsung.android.scloud.oem.lib.LOG;
import com.samsung.android.scloud.oem.lib.RestoreFileList;
import com.samsung.android.scloud.oem.lib.bnr.BNRConstants.RCODE;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class BNRClientHelper implements IBNRClientHelper {
    private static final String BACKUP = "backup";
    private static final int DOWNLOADFILELIST = 1;
    private static final int ITEMLIST = 0;
    private static String OPERATION = "";
    private static final String RESTORE = "restore";
    private static final int RESTOREFILELIST = 2;
    private static final String TAG = "BNRClientHelper_";
    private final Map<String, SyncServiceHandler> SyncServiceHandler_Map = new HashMap();
    private String appname;
    private String category;
    private String contentsId;
    private ISCloudBNRClient mClient;
    private List<String> mDownloadFileList;
    private List<String> mProcessedKeyList;
    private List<String> mRestoreFileList;
    private Bundle mResult = new Bundle();
    private String mTAG = "";

    private interface APPEND {
        public static final String DOWNLOAD = "_scloud_dwnload";
        public static final String ORIGIN = "_scloud_origin";
    }

    private interface Key {
        public static final String CATEGORY = "category";
        public static final String CONTENTS_ID = "contents_id";
        public static final String DESCRIPTION = "description";
        public static final String EXTERNAL = "external";
        public static final String FILE_DESCRIPTOR = "file_descriptor";
        public static final String INSERTED_ID_LIST = "inserted_id_list";
        public static final String IS_CONTINUE = "is_continue";
        public static final String IS_ENABLE_BACKUP = "is_enable_backup";
        public static final String IS_EXTERNAL = "is_external";
        public static final String IS_FAILED = "is_failed";
        public static final String IS_FIRST_BACKUP = "is_first_backup";
        public static final String IS_SUCCESS = "is_success";
        public static final String KEY = "key";
        public static final String LABEL = "label";
        public static final String LOCAL_ID = "local_id";
        public static final String MAX_COUNT = "max_count";
        public static final String MAX_SIZE = "max_size";
        public static final String NAME = "name";
        public static final String PATH = "path";
        public static final String REAL_PATH = "real_path";
        public static final String SIZE = "size";
        public static final String START = "start";
        public static final String SUPPORT_BACKUP = "support_backup";
        public static final String TIMESTAMP = "timestamp";
        public static final String TO_UPLOAD_LIST = "to_upload_list";
        public static final String VALUE = "value";
    }

    private interface METHOD {
        public static final String ACCOUNT_SIGN_IN = "accountSignIn";
        public static final String ACCOUNT_SIGN_OUT = "accountSignOut";
        public static final String BACKUP_COMPLETE = "backupComplete";
        public static final String BACKUP_ITEM = "backupItem";
        public static final String BACKUP_PREPARE = "backupPrepare";
        public static final String GET_CLIENT_INFO = "getClientInfo";
        public static final String GET_FILE_META = "getFileMeta";
        public static final String GET_FILE_PATH = "getFilePath";
        public static final String GET_ITEM_KEY = "getItemKey";
        public static final String RESTORE_COMPLETE = "restoreComplete";
        public static final String RESTORE_FILE = "restoreFile";
        public static final String RESTORE_ITEM = "restoreItem";
        public static final String RESTORE_PREPARE = "restorePrepare";
    }

    private interface SyncServiceHandler {
        Bundle handleServiceAction(Context context, String str, Bundle bundle);
    }

    public BNRClientHelper(Context context, String name, ISCloudBNRClient client, String cid, String category) {
        this.mTAG = TAG + name;
        LOG.f(this.mTAG, "BNRCLIENTHELPER, v: 1.8.0");
        this.appname = name;
        this.mClient = client;
        this.contentsId = cid;
        this.category = category;
        LOG.d(this.mTAG, "BNRCLIENTHELPER, " + cid + ", " + category);
        setServiceHandler();
    }

    public Bundle handleRequest(Context context, String method, String name, Bundle param) {
        if (this.SyncServiceHandler_Map.containsKey(method)) {
            return ((SyncServiceHandler) this.SyncServiceHandler_Map.get(method)).handleServiceAction(context, name, param);
        }
        return null;
    }

    private void setServiceHandler() {
        this.SyncServiceHandler_Map.put("getClientInfo", new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                Bundle result = new Bundle();
                LOG.f(BNRClientHelper.this.mTAG, "GET_CLIENT_INFO, c: " + BNRClientHelper.this.category);
                boolean isSupportBackup = BNRClientHelper.this.mClient.isSupportBackup(context);
                boolean isEnableBackup = BNRClientHelper.this.mClient.isEnableBackup(context);
                LOG.f(BNRClientHelper.this.mTAG, "GET_CLIENT_INFO, s: " + isSupportBackup + ", e: " + isEnableBackup);
                boolean isFirstBackup = BackupMetaManager.getInstance(context).isFirstBackup(name);
                String label = BNRClientHelper.this.mClient.getLabel(context);
                String description = BNRClientHelper.this.mClient.getDescription(context);
                result.putBoolean("support_backup", isSupportBackup);
                result.putString("name", name);
                result.putString("contents_id", BNRClientHelper.this.contentsId);
                result.putBoolean("is_enable_backup", isEnableBackup);
                result.putBoolean("is_first_backup", isFirstBackup);
                result.putString("label", label);
                result.putString("description", description);
                result.putString("category", BNRClientHelper.this.category);
                LOG.d(BNRClientHelper.this.mTAG, "GET_CLIENT_INFO, " + BNRClientHelper.this.contentsId + ", " + label + ", " + description + ", " + BNRClientHelper.this.category);
                return result;
            }
        });
        this.SyncServiceHandler_Map.put(METHOD.BACKUP_PREPARE, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                LOG.f(BNRClientHelper.this.mTAG, "BACKUP_PREPARE, v: 1.8.0");
                BNRClientHelper.OPERATION = "backup";
                BNRClientHelper.this.clearData(context, name);
                boolean isSuccess = BNRClientHelper.this.mClient.backupPrepare(context);
                LOG.f(BNRClientHelper.this.mTAG, "BACKUP_PREPARE, r: " + isSuccess);
                BNRClientHelper.this.mResult.putBoolean("is_success", isSuccess);
                return BNRClientHelper.this.mResult;
            }
        });
        this.SyncServiceHandler_Map.put(METHOD.GET_ITEM_KEY, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                Bundle result = new Bundle();
                int start = extras.getInt(Key.START);
                int max_count = extras.getInt(Key.MAX_COUNT);
                int index = 0;
                LOG.f(BNRClientHelper.this.mTAG, "GET_ITEM_KEY, s: " + start + ", m: " + max_count);
                HashMap<String, Long> items = BNRClientHelper.this.mClient.getItemKey(context, start, max_count);
                if (items == null) {
                    LOG.f(BNRClientHelper.this.mTAG, "GET_ITEM_KEY, nothing to backup");
                    result.putBoolean(Key.IS_CONTINUE, false);
                    result.putBoolean("is_success", true);
                } else if (items.size() == 0) {
                    LOG.f(BNRClientHelper.this.mTAG, "GET_ITEM_KEY, value is incorrect, return err");
                    result.putBoolean("is_success", false);
                } else {
                    LOG.f(BNRClientHelper.this.mTAG, "GET_ITEM_KEY, c: " + items.size());
                    String[] localIdList = new String[items.size()];
                    long[] timestampList = new long[items.size()];
                    for (Entry<String, Long> item : items.entrySet()) {
                        LOG.d(BNRClientHelper.this.mTAG, "GET_ITEM_KEY, item: " + ((String) item.getKey()) + ", " + item.getValue());
                        localIdList[index] = (String) item.getKey();
                        int index2 = index + 1;
                        timestampList[index] = ((Long) item.getValue()).longValue();
                        index = index2;
                    }
                    result.putBoolean(Key.IS_CONTINUE, items.size() >= max_count);
                    result.putStringArray("local_id", localIdList);
                    result.putLongArray("timestamp", timestampList);
                    result.putBoolean("is_success", true);
                }
                return result;
            }
        });
        this.SyncServiceHandler_Map.put(METHOD.GET_FILE_META, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                Bundle result = new Bundle();
                int start = extras.getInt(Key.START);
                int max_count = extras.getInt(Key.MAX_COUNT);
                int index = 0;
                LOG.f(BNRClientHelper.this.mTAG, "GET_FILE_META, s: " + start + ", m: " + max_count);
                ArrayList<BNRFile> files = BNRClientHelper.this.mClient.getFileMeta(context, start, max_count);
                if (files == null) {
                    LOG.f(BNRClientHelper.this.mTAG, "GET_FILE_META, nothing to backup");
                    result.putBoolean(Key.IS_CONTINUE, false);
                    result.putBoolean("is_success", true);
                } else if (files.size() == 0) {
                    LOG.f(BNRClientHelper.this.mTAG, "GET_FILE_META, value is incorrect, return err");
                    result.putBoolean("is_success", false);
                } else {
                    LOG.f(BNRClientHelper.this.mTAG, "GET_FILE_META, c: " + files.size());
                    String[] path = new String[files.size()];
                    long[] size = new long[files.size()];
                    boolean[] isExternal = new boolean[files.size()];
                    long[] timeStamp = new long[files.size()];
                    Iterator it = files.iterator();
                    while (it.hasNext()) {
                        BNRFile file = (BNRFile) it.next();
                        LOG.d(BNRClientHelper.this.mTAG, "GET_FILE_META, " + file.getPath() + ", " + file.getSize() + ", " + file.getisExternal() + ", " + file.getTimeStamp());
                        path[index] = file.getPath();
                        size[index] = file.getSize();
                        isExternal[index] = file.getisExternal();
                        int index2 = index + 1;
                        timeStamp[index] = file.getTimeStamp();
                        index = index2;
                    }
                    result.putBoolean(Key.IS_CONTINUE, files.size() >= max_count);
                    result.putStringArray(Key.PATH, path);
                    result.putLongArray(Key.SIZE, size);
                    result.putBooleanArray(Key.EXTERNAL, isExternal);
                    result.putLongArray("timestamp", timeStamp);
                    result.putBoolean("is_success", true);
                }
                return result;
            }
        });
        this.SyncServiceHandler_Map.put(METHOD.BACKUP_ITEM, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                IOException e;
                FileNotFoundException e2;
                JSONException e3;
                Throwable th;
                Bundle result = new Bundle();
                FileWriter fw = null;
                ArrayList<String> toUploadList = extras.getStringArrayList(Key.TO_UPLOAD_LIST);
                ParcelFileDescriptor fd = (ParcelFileDescriptor) extras.getParcelable(Key.FILE_DESCRIPTOR);
                long maxSize = extras.getLong(Key.MAX_SIZE);
                if (toUploadList != null) {
                    LOG.f(BNRClientHelper.this.mTAG, "BACKUP_ITEM, i: " + toUploadList.size());
                }
                ArrayList<BNRItem> items = BNRClientHelper.this.mClient.backupItem(context, toUploadList);
                if (items == null || items.size() == 0) {
                    LOG.f(BNRClientHelper.this.mTAG, "BACKUP_ITEM, value is incorrect, return err");
                    result.putBoolean("is_success", false);
                } else {
                    LOG.f(BNRClientHelper.this.mTAG, "BACKUP_ITEM, c: " + items.size());
                    try {
                        FileWriter fw2 = new FileWriter(fd.getFileDescriptor());
                        try {
                            String[] nowKey = new String[items.size()];
                            fw2.write("[");
                            BNRItem firstItem = (BNRItem) items.get(0);
                            nowKey[0] = firstItem.getLocalId();
                            long size = firstItem.getSize();
                            JSONObject json = new JSONObject();
                            LOG.d(BNRClientHelper.this.mTAG, "BACKUP_ITEM, item: " + firstItem.getLocalId() + ", " + firstItem.getTimeStamp());
                            json.put(Key.KEY, firstItem.getLocalId());
                            json.put("value", firstItem.getData());
                            json.put("timestamp", firstItem.getTimeStamp());
                            fw2.write(json.toString());
                            if (items.size() > 1) {
                                int index = 1;
                                while (index < items.size()) {
                                    BNRItem item = (BNRItem) items.get(index);
                                    if (item == null) {
                                        LOG.f(BNRClientHelper.this.mTAG, "BACKUP_ITEM, item is incorrect: " + index + ", return err");
                                        fw2.close();
                                        result.putBoolean("is_success", false);
                                        if (fw2 != null) {
                                            try {
                                                fw2.close();
                                            } catch (IOException e4) {
                                                e4.printStackTrace();
                                            }
                                        }
                                        fw = fw2;
                                    } else if (item.getSize() + size >= maxSize) {
                                        break;
                                    } else {
                                        nowKey[index] = item.getLocalId();
                                        size += item.getSize();
                                        fw2.write(",");
                                        LOG.d(BNRClientHelper.this.mTAG, "BACKUP_ITEM, item: " + item.getLocalId() + ", " + item.getTimeStamp());
                                        json.put(Key.KEY, item.getLocalId());
                                        json.put("value", item.getData());
                                        json.put("timestamp", item.getTimeStamp());
                                        fw2.write(json.toString());
                                        index++;
                                    }
                                }
                            }
                            fw2.write("]");
                            fw2.flush();
                            result.putBoolean("is_success", true);
                            result.putStringArray("local_id", nowKey);
                            if (fw2 != null) {
                                try {
                                    fw2.close();
                                    fw = fw2;
                                } catch (IOException e42) {
                                    e42.printStackTrace();
                                    fw = fw2;
                                }
                            }
                        } catch (FileNotFoundException e5) {
                            e2 = e5;
                            fw = fw2;
                        } catch (IOException e6) {
                            e42 = e6;
                            fw = fw2;
                        } catch (JSONException e7) {
                            e3 = e7;
                            fw = fw2;
                        } catch (Throwable th2) {
                            th = th2;
                            fw = fw2;
                        }
                    } catch (FileNotFoundException e8) {
                        e2 = e8;
                        try {
                            LOG.e(BNRClientHelper.this.mTAG, "FileNotFoundException~!!, " + name, e2);
                            result.putBoolean("is_success", false);
                            if (fw != null) {
                                try {
                                    fw.close();
                                } catch (IOException e422) {
                                    e422.printStackTrace();
                                }
                            }
                            return result;
                        } catch (Throwable th3) {
                            th = th3;
                            if (fw != null) {
                                try {
                                    fw.close();
                                } catch (IOException e4222) {
                                    e4222.printStackTrace();
                                }
                            }
                            throw th;
                        }
                    } catch (IOException e9) {
                        e4222 = e9;
                        LOG.e(BNRClientHelper.this.mTAG, "IOException~!!, " + name, e4222);
                        result.putBoolean("is_success", false);
                        if (fw != null) {
                            try {
                                fw.close();
                            } catch (IOException e42222) {
                                e42222.printStackTrace();
                            }
                        }
                        return result;
                    } catch (JSONException e10) {
                        e3 = e10;
                        LOG.e(BNRClientHelper.this.mTAG, "JSONException~!!, " + name, e3);
                        result.putBoolean("is_success", false);
                        if (fw != null) {
                            try {
                                fw.close();
                            } catch (IOException e422222) {
                                e422222.printStackTrace();
                            }
                        }
                        return result;
                    }
                }
                return result;
            }
        });
        this.SyncServiceHandler_Map.put(METHOD.GET_FILE_PATH, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                LOG.f(BNRClientHelper.this.mTAG, "GET_FILE_PATH, " + BNRClientHelper.OPERATION);
                String path = extras.getString(Key.PATH);
                boolean external = extras.getBoolean(Key.EXTERNAL);
                Bundle result = new Bundle();
                LOG.d(BNRClientHelper.this.mTAG, "GET_FILE_PATH, " + path + ", " + external);
                String localPath = BNRClientHelper.this.mClient.getFilePath(context, path, external, BNRClientHelper.OPERATION);
                if (localPath != null) {
                    LOG.d(BNRClientHelper.this.mTAG, "GET_FILE_PATH, r: " + localPath);
                    result.putBoolean("is_success", true);
                    result.putString(Key.REAL_PATH, localPath);
                } else {
                    LOG.f(BNRClientHelper.this.mTAG, "GET_FILE_PATH, value is incorrect, return err");
                    result.putBoolean("is_success", false);
                }
                return result;
            }
        });
        this.SyncServiceHandler_Map.put(METHOD.BACKUP_COMPLETE, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                boolean backupSuccess = extras.getBoolean("is_success");
                LOG.f(BNRClientHelper.this.mTAG, "BACKUP_COMPLETE, " + backupSuccess);
                boolean isSuccess = BNRClientHelper.this.mClient.backupComplete(context, backupSuccess);
                if (isSuccess && backupSuccess) {
                    BackupMetaManager.getInstance(context).setFirstBackup(name, false);
                    BackupMetaManager.getInstance(context).setLastBackupTime(name, System.currentTimeMillis());
                }
                LOG.f(BNRClientHelper.this.mTAG, "BACKUP_COMPLETE, return: " + isSuccess);
                BNRClientHelper.this.mResult.putBoolean("is_success", isSuccess);
                return BNRClientHelper.this.mResult;
            }
        });
        this.SyncServiceHandler_Map.put(METHOD.RESTORE_PREPARE, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                LOG.f(BNRClientHelper.this.mTAG, "RESTORE_PREPARE, v: 1.8.0");
                BNRClientHelper.OPERATION = "restore";
                BNRClientHelper.this.clearRestoredData(context, name);
                Bundle result = new Bundle();
                boolean isSuccess = BNRClientHelper.this.mClient.restorePrepare(context, extras);
                LOG.f(BNRClientHelper.this.mTAG, "RESTORE_PREPARE, return: " + isSuccess);
                result.putBoolean("is_success", isSuccess);
                return result;
            }
        });
        this.SyncServiceHandler_Map.put(METHOD.RESTORE_ITEM, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                ParcelFileDescriptor fd = (ParcelFileDescriptor) extras.getParcelable(Key.FILE_DESCRIPTOR);
                Bundle result = new Bundle();
                ArrayList<BNRItem> items = new ArrayList();
                ArrayList<String> insertedId = new ArrayList();
                LOG.f(BNRClientHelper.this.mTAG, "RESTORE_ITEM, c: " + items.size());
                BNRClientHelper.this.convertToBNRItems(fd, items);
                boolean is_success = BNRClientHelper.this.mClient.restoreItem(context, items, insertedId);
                LOG.f(BNRClientHelper.this.mTAG, "RESTORE_ITEM, return: " + insertedId.size() + ", " + is_success);
                if (insertedId.size() > 0) {
                    Iterator it = insertedId.iterator();
                    while (it.hasNext()) {
                        BNRClientHelper.this.addToList(context, name, 0, (String) it.next());
                    }
                }
                result.putBoolean("is_success", is_success);
                result.putStringArray(Key.INSERTED_ID_LIST, (String[]) insertedId.toArray(new String[insertedId.size()]));
                return result;
            }
        });
        this.SyncServiceHandler_Map.put(METHOD.RESTORE_FILE, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                Bundle result = new Bundle();
                result.putBoolean(Key.IS_FAILED, false);
                BNRClientHelper.this.addToList(context, name, 1, extras.getString(Key.PATH) + APPEND.DOWNLOAD);
                LOG.d(BNRClientHelper.this.mTAG, "RESTORE_FILE, " + extras.getString(Key.PATH));
                if (BNRClientHelper.this.fileCopy(extras.getString(Key.PATH), extras.getString(Key.PATH) + APPEND.ORIGIN)) {
                    BNRClientHelper.this.addToList(context, name, 2, extras.getString(Key.PATH) + APPEND.ORIGIN);
                    if (!BNRClientHelper.this.fileCopy(extras.getString(Key.PATH) + APPEND.DOWNLOAD, extras.getString(Key.PATH))) {
                        result.putBoolean(Key.IS_FAILED, true);
                    }
                } else {
                    result.putBoolean(Key.IS_FAILED, true);
                }
                return result;
            }
        });
        this.SyncServiceHandler_Map.put(METHOD.RESTORE_COMPLETE, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                boolean isSuccess = extras.getBoolean("is_success");
                LOG.f(BNRClientHelper.this.mTAG, "RESTORE_COMPLETE, " + isSuccess);
                Bundle result = new Bundle();
                result.putBoolean("is_success", true);
                if (isSuccess) {
                    File file;
                    if (BNRClientHelper.this.mProcessedKeyList == null) {
                        BNRClientHelper.this.mProcessedKeyList = ItemSavedList.load(context, name);
                    }
                    LOG.f(BNRClientHelper.this.mTAG, "RESTORE_COMPLETE, restoredKeyList size : " + BNRClientHelper.this.mProcessedKeyList.size());
                    if (BNRClientHelper.this.mProcessedKeyList.size() >= 0) {
                        if (BNRClientHelper.this.mClient.restoreComplete(context, (String[]) BNRClientHelper.this.mProcessedKeyList.toArray(new String[BNRClientHelper.this.mProcessedKeyList.size()]))) {
                            BNRClientHelper.this.mProcessedKeyList.clear();
                        } else {
                            LOG.f(BNRClientHelper.this.mTAG, "RESTORE_COMPLETE, restoreComplete() return false ");
                            BNRClientHelper.this.clearRestoredData(context, name);
                            result.putBoolean("is_success", false);
                        }
                    }
                    if (BNRClientHelper.this.mRestoreFileList == null) {
                        BNRClientHelper.this.mRestoreFileList = RestoreFileList.load(context, name);
                    }
                    if (BNRClientHelper.this.mRestoreFileList.size() > 0) {
                        for (String downloadFile : BNRClientHelper.this.mRestoreFileList) {
                            file = new File(downloadFile);
                            if (file != null && file.exists()) {
                                LOG.i(BNRClientHelper.this.mTAG, "clearPreRestoredData() delete, name : " + downloadFile + ", deleted : " + file.delete());
                            }
                        }
                    }
                    if (BNRClientHelper.this.mDownloadFileList == null) {
                        BNRClientHelper.this.mDownloadFileList = DownloadFileList.load(context, name);
                    }
                    if (BNRClientHelper.this.mDownloadFileList.size() > 0) {
                        for (String downloadFile2 : BNRClientHelper.this.mDownloadFileList) {
                            file = new File(downloadFile2);
                            if (file != null && file.exists()) {
                                LOG.i(BNRClientHelper.this.mTAG, "clearPreRestoredData() delete, name : " + downloadFile2 + ", deleted : " + file.delete());
                            }
                        }
                        BNRClientHelper.this.mDownloadFileList.clear();
                    }
                } else {
                    BNRClientHelper.this.clearRestoredData(context, name);
                    result.putBoolean("is_success", true);
                }
                return result;
            }
        });
        this.SyncServiceHandler_Map.put(METHOD.ACCOUNT_SIGN_IN, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                LOG.f(BNRClientHelper.this.mTAG, RCODE.ACCOUNT_SIGN_IN);
                Bundle result = new Bundle();
                BNRClientHelper.this.clearData(context, name);
                if (BackupMetaManager.getInstance(context).clear(name)) {
                    result.putBoolean("is_success", true);
                } else {
                    result.putBoolean("is_success", false);
                }
                return result;
            }
        });
        this.SyncServiceHandler_Map.put(METHOD.ACCOUNT_SIGN_OUT, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                LOG.f(BNRClientHelper.this.mTAG, RCODE.ACCOUNT_SIGN_OUT);
                Bundle result = new Bundle();
                BNRClientHelper.this.clearData(context, name);
                if (BackupMetaManager.getInstance(context).clear(name)) {
                    result.putBoolean("is_success", true);
                } else {
                    result.putBoolean("is_success", false);
                }
                return result;
            }
        });
    }

    public String getName() {
        return this.appname;
    }

    private void convertToBNRItems(ParcelFileDescriptor fd, List<BNRItem> toUploadItems) {
        IOException e;
        JSONArray jSONArray;
        FileInputStream fileInputStream;
        JSONException e2;
        try {
            FileInputStream fileIpStream = new FileInputStream(fd.getFileDescriptor());
            try {
                byte[] buffer = new byte[((int) fd.getStatSize())];
                fileIpStream.read(buffer);
                JSONArray jsonArray = new JSONArray(new String(buffer));
                int i = 0;
                while (i < jsonArray.length()) {
                    try {
                        JSONObject json = jsonArray.optJSONObject(i);
                        BNRItem item = new BNRItem(json.optString(Key.KEY), json.optString("value"), json.optLong("timestamp"));
                        LOG.d(this.mTAG, "converToBNRItems : " + item.getLocalId() + ", " + item.getTimeStamp());
                        toUploadItems.add(item);
                        i++;
                    } catch (IOException e3) {
                        e = e3;
                        jSONArray = jsonArray;
                        fileInputStream = fileIpStream;
                    } catch (JSONException e4) {
                        e2 = e4;
                        jSONArray = jsonArray;
                        fileInputStream = fileIpStream;
                    }
                }
                fileIpStream.close();
                jSONArray = jsonArray;
                fileInputStream = fileIpStream;
            } catch (IOException e5) {
                e = e5;
                fileInputStream = fileIpStream;
                e.printStackTrace();
            } catch (JSONException e6) {
                e2 = e6;
                fileInputStream = fileIpStream;
                e2.printStackTrace();
            }
        } catch (IOException e7) {
            e = e7;
            e.printStackTrace();
        } catch (JSONException e8) {
            e2 = e8;
            e2.printStackTrace();
        }
    }

    private boolean fileCopy(String fromPath, String toPath) {
        FileNotFoundException e;
        IOException e2;
        Throwable th;
        LOG.i(this.mTAG, "fileCopy(), from : " + fromPath + " , to : " + toPath);
        File oldFile = new File(fromPath);
        if (oldFile == null || !oldFile.isFile()) {
            LOG.i(this.mTAG, "oldFile is null or not file~!");
            return true;
        }
        File newFile = new File(toPath);
        if (newFile != null && newFile.exists()) {
            newFile.delete();
        }
        if (oldFile.renameTo(newFile)) {
            if (oldFile.exists()) {
                oldFile.delete();
            }
            return true;
        }
        FileInputStream fis = null;
        FileOutputStream fos = null;
        byte[] buf = new byte[1024];
        try {
            FileInputStream fis2 = new FileInputStream(oldFile);
            try {
                FileOutputStream fos2 = new FileOutputStream(newFile);
                while (true) {
                    try {
                        int read = fis2.read(buf, 0, buf.length);
                        if (read == -1) {
                            break;
                        }
                        fos2.write(buf, 0, read);
                    } catch (FileNotFoundException e3) {
                        e = e3;
                        fos = fos2;
                        fis = fis2;
                    } catch (IOException e4) {
                        e2 = e4;
                        fos = fos2;
                        fis = fis2;
                    } catch (Throwable th2) {
                        th = th2;
                        fos = fos2;
                        fis = fis2;
                    }
                }
                try {
                    if (oldFile.exists()) {
                        oldFile.delete();
                    }
                    if (fis2 != null) {
                        fis2.close();
                    }
                    if (fos2 == null) {
                        return true;
                    }
                    fos2.close();
                    return true;
                } catch (IOException e22) {
                    e22.printStackTrace();
                    return true;
                }
            } catch (FileNotFoundException e5) {
                e = e5;
                fis = fis2;
                try {
                    LOG.e(this.mTAG, "fileCopy() failed", e);
                    try {
                        if (oldFile.exists()) {
                            oldFile.delete();
                        }
                        if (fis != null) {
                            fis.close();
                        }
                        if (fos != null) {
                            return false;
                        }
                        fos.close();
                        return false;
                    } catch (IOException e222) {
                        e222.printStackTrace();
                        return false;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    try {
                        if (oldFile.exists()) {
                            oldFile.delete();
                        }
                        if (fis != null) {
                            fis.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e2222) {
                        e2222.printStackTrace();
                    }
                    throw th;
                }
            } catch (IOException e6) {
                e2222 = e6;
                fis = fis2;
                LOG.e(this.mTAG, "fileCopy() failed", e2222);
                try {
                    if (oldFile.exists()) {
                        oldFile.delete();
                    }
                    if (fis != null) {
                        fis.close();
                    }
                    if (fos != null) {
                        return false;
                    }
                    fos.close();
                    return false;
                } catch (IOException e22222) {
                    e22222.printStackTrace();
                    return false;
                }
            } catch (Throwable th4) {
                th = th4;
                fis = fis2;
                if (oldFile.exists()) {
                    oldFile.delete();
                }
                if (fis != null) {
                    fis.close();
                }
                if (fos != null) {
                    fos.close();
                }
                throw th;
            }
        } catch (FileNotFoundException e7) {
            e = e7;
            LOG.e(this.mTAG, "fileCopy() failed", e);
            if (oldFile.exists()) {
                oldFile.delete();
            }
            if (fis != null) {
                fis.close();
            }
            if (fos != null) {
                return false;
            }
            fos.close();
            return false;
        } catch (IOException e8) {
            e22222 = e8;
            LOG.e(this.mTAG, "fileCopy() failed", e22222);
            if (oldFile.exists()) {
                oldFile.delete();
            }
            if (fis != null) {
                fis.close();
            }
            if (fos != null) {
                return false;
            }
            fos.close();
            return false;
        }
    }

    private void clearData(Context context, String name) {
        this.mProcessedKeyList = ItemSavedList.load(context, name);
        this.mRestoreFileList = RestoreFileList.load(context, name);
        this.mDownloadFileList = DownloadFileList.load(context, name);
        this.mProcessedKeyList.clear();
        this.mRestoreFileList.clear();
        this.mDownloadFileList.clear();
    }

    private void clearRestoredData(Context context, String name) {
        this.mProcessedKeyList = ItemSavedList.load(context, name);
        this.mRestoreFileList = RestoreFileList.load(context, name);
        this.mDownloadFileList = DownloadFileList.load(context, name);
        if (this.mProcessedKeyList.size() > 0) {
            LOG.f(this.mTAG, "remove restored data in previous failed restoring.. - " + this.mProcessedKeyList.size());
            this.mClient.clearRestoreData(context, (String[]) this.mProcessedKeyList.toArray(new String[this.mProcessedKeyList.size()]));
            this.mProcessedKeyList.clear();
        }
        if (this.mRestoreFileList.size() > 0) {
            LOG.f(this.mTAG, "remove restored files in previous failed restoring.. - " + this.mRestoreFileList.size());
            for (String processedFile : this.mRestoreFileList) {
                fileCopy(processedFile + APPEND.ORIGIN, processedFile);
            }
            this.mRestoreFileList.clear();
        }
        if (this.mDownloadFileList.size() > 0) {
            for (String downloadFile : this.mDownloadFileList) {
                File file = new File(downloadFile);
                if (file != null && file.exists()) {
                    LOG.i(this.mTAG, "clearPreRestoredData() delete, name : " + downloadFile + ", deleted : " + file.delete());
                }
            }
            this.mDownloadFileList.clear();
        }
    }

    private void addToList(Context context, String name, int type, String key) {
        switch (type) {
            case 0:
                if (this.mProcessedKeyList == null) {
                    this.mProcessedKeyList = ItemSavedList.load(context, name);
                }
                this.mProcessedKeyList.add(key);
                return;
            case 1:
                if (this.mDownloadFileList == null) {
                    this.mDownloadFileList = DownloadFileList.load(context, name);
                }
                this.mDownloadFileList.add(key);
                return;
            case 2:
                if (this.mRestoreFileList == null) {
                    this.mRestoreFileList = RestoreFileList.load(context, name);
                }
                this.mRestoreFileList.add(key);
                return;
            default:
                return;
        }
    }
}

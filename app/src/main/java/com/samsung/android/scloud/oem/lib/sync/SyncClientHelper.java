package com.samsung.android.scloud.oem.lib.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import com.samsung.android.scloud.oem.lib.LOG;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class SyncClientHelper {
    public static final String CONTENT_SYNC_FILE = "content.sync";
    private static SyncClientHelper INSTANCE = null;
    private static final Map<String, SyncServiceHandler> SyncServiceHandler_Map = new HashMap();
    private static final String TAG = "SCloudClientHelper";
    private Map<String, Integer> mClientDataVersionMap;
    private Map<String, ISCloudSyncClient> mClientMap;
    private String mContentAuthority;
    private String mContentsId;
    private boolean mIsSyncable = false;
    private String mSupprtSyncUri;
    private SharedPreferences mSyncMeta;

    private interface Key {
        public static final String ACCOUNT_NAME = "account_name";
        public static final String ACCOUNT_TYPE = "account_type";
        public static final String CONTENT_SYNC_FILE = "content_sync_file";
        public static final String DATA_VERSION = "data_version";
        public static final String DELETED = "deleted";
        public static final String DELETED_FILE_LIST = "deleted_file_list";
        public static final String DOWNLOAD_FILE_LIST = "download_file_list";
        public static final String FILE_LIST = "file_list";
        public static final String IS_SUCCESS = "is_success";
        public static final String IS_SYNCABLE = "is_syncable";
        public static final String LAST_SYNC_TIME = "last_sync_time";
        public static final String LOCAL_ID = "local_id";
        public static final String META_FILE = "sync_meta";
        public static final String NEED_RECOVER = "need_recover";
        public static final String RCODE = "rcode";
        public static final String SYNC_KEY = "sync_key";
        public static final String TAG = "tag";
        public static final String TIMESTAMP = "timestamp";
        public static final String TIMESTAMP_LIST = "timestamp_list";
        public static final String UPLOAD_FILE_LIST = "upload_file_list";
    }

    private interface METHOD {
        public static final String COMPLETE = "complete";
        public static final String DELETE = "deleteItem";
        public static final String DOWNLOAD = "download";
        public static final String GET_ATTACHMENT_INFO = "getAttachmentInfo";
        public static final String IS_SYNCABLE = "isSyncable";
        public static final String LAST_SYNC_TIME = "lastSyncTime";
        public static final String PREPARE = "prepare";
        public static final String UPLOAD = "upload";
    }

    private interface SyncServiceHandler {
        Bundle handleServiceAction(Context context, String str, Bundle bundle);
    }

    public static synchronized SyncClientHelper getInstance(Context context) {
        SyncClientHelper syncClientHelper;
        synchronized (SyncClientHelper.class) {
            if (INSTANCE == null) {
                INSTANCE = new SyncClientHelper(context);
            }
            syncClientHelper = INSTANCE;
        }
        return syncClientHelper;
    }

    private SyncClientHelper(Context context) {
        LOG.i(TAG, "init SyncClientHelper");
        this.mClientMap = new LinkedHashMap();
        this.mClientDataVersionMap = new HashMap();
        this.mSyncMeta = context.getSharedPreferences("sync_meta", 0);
        register(context);
        LOG.i(TAG, "init SyncClientHelper finished");
    }

    void setClientImple(String name, int dataVersion, ISCloudSyncClient client) {
        LOG.f(TAG, "setClientImple name : " + name + ", version : " + dataVersion);
        this.mClientMap.put(name, client);
        this.mClientDataVersionMap.put(name, Integer.valueOf(dataVersion));
    }

    private void register(Context context) {
        try {
            LOG.d(TAG, "register - started.");
            ApplicationInfo app = context.getPackageManager().getApplicationInfo(context.getPackageName(), 128);
            Bundle meta = app.metaData;
            LOG.f(TAG, "register - meta read : app info = " + app.className + ", " + app.processName + ", " + app.packageName + ", " + meta);
            if (meta == null) {
                LOG.e(TAG, "register - meta read : metadata is null !!");
                return;
            }
            this.mContentsId = meta.getString("scloud_contents_id");
            this.mSupprtSyncUri = meta.getString("scloud_support_authority");
            this.mContentAuthority = meta.getString("scloud_data_authority");
            LOG.d(TAG, "register - meta read : " + this.mContentsId + ", " + this.mSupprtSyncUri + ", " + this.mContentAuthority);
            if (this.mContentsId == null || this.mSupprtSyncUri == null) {
                LOG.e(TAG, "register - scloud_contents_id and scloud_support_authority should be define in meta-data of application");
                return;
            }
            this.mSupprtSyncUri = "content://" + this.mSupprtSyncUri;
            this.mIsSyncable = meta.getBoolean("scloud_support_sync");
            if (this.mIsSyncable) {
                XmlResourceParser xml = context.getResources().getAssets().openXmlResourceParser("res/xml/sync_item.xml");
                LOG.d(TAG, "register - xml1 : " + xml.getName());
                xml.next();
                LOG.d(TAG, "register - xml2 : " + xml.getName());
                xml.next();
                LOG.d(TAG, "register - xml3 : " + xml.getName());
                if (xml.getName().equals("sync_items")) {
                    while (true) {
                        if (xml.next() != 3 || !xml.getName().equals("sync_items")) {
                            LOG.d(TAG, "register - xml4 : " + xml.getName());
                            if (xml.getName().equals("sync_item") && xml.getEventType() == 2) {
                                String name = xml.getAttributeValue(null, "name");
                                String version = xml.getAttributeValue(null, "data_version");
                                int dataVersion = 0;
                                if (version != null) {
                                    try {
                                        dataVersion = Integer.parseInt(version);
                                    } catch (NumberFormatException e) {
                                        LOG.e(TAG, "invalid data_version value : ", e);
                                    }
                                }
                                String clientImplClass = xml.getAttributeValue(null, "client_impl_class");
                                LOG.d(TAG, "register - xml5 : " + name + ", v :" + version + ", " + this.mSupprtSyncUri + ", " + clientImplClass);
                                setClientImple(name, dataVersion, (ISCloudSyncClient) Class.forName(clientImplClass).newInstance());
                            }
                        } else {
                            return;
                        }
                    }
                }
                return;
            }
            LOG.f(TAG, "register - meta read : not support!!");
        } catch (Exception e2) {
            LOG.e(TAG, "parsing error : ", e2);
        }
    }

    public String getContentsId() {
        return this.mContentsId;
    }

    public String getContentAuthority() {
        return this.mContentAuthority;
    }

    public String getSupportSyncUri() {
        return this.mSupprtSyncUri;
    }

    public boolean isSyncable() {
        return this.mIsSyncable;
    }

    public Map<String, ISCloudSyncClient> getClientMap() {
        return this.mClientMap;
    }

    public int getDataVersion(String name) {
        return ((Integer) this.mClientDataVersionMap.get(name)).intValue();
    }

    public Bundle handleRequest(Context context, String method, String name, Bundle param) {
        if (SyncServiceHandler_Map.containsKey(method)) {
            return ((SyncServiceHandler) SyncServiceHandler_Map.get(method)).handleServiceAction(context, name, param);
        }
        return null;
    }

    static {
        SyncServiceHandler_Map.put(METHOD.IS_SYNCABLE, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                LOG.f(SyncClientHelper.TAG, "IsSyncable : " + name);
                boolean isSyncable = ((ISCloudSyncClient) SyncClientHelper.getInstance(context).mClientMap.get(name)).isSyncable(context);
                Bundle result = new Bundle();
                result.putBoolean(Key.IS_SYNCABLE, isSyncable);
                return result;
            }
        });
        SyncServiceHandler_Map.put(METHOD.LAST_SYNC_TIME, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                LOG.f(SyncClientHelper.TAG, "LAST_SYNC_TIME : " + name + ", extras : " + extras);
                if (extras == null || !extras.containsKey("last_sync_time")) {
                    long lastSyncTime = SyncClientHelper.getInstance(context).mSyncMeta.getLong("last_sync_time_" + name, 0);
                    Bundle result = new Bundle();
                    result.putLong("last_sync_time", lastSyncTime);
                    LOG.i(SyncClientHelper.TAG, "getLastSyncTime - name : " + name + ", val : " + lastSyncTime);
                    return result;
                }
                lastSyncTime = extras.getLong("last_sync_time");
                SyncClientHelper.getInstance(context).mSyncMeta.edit().putLong("last_sync_time_" + name, lastSyncTime).commit();
                LOG.i(SyncClientHelper.TAG, "setLastSyncTime - name : " + name + ", val : " + lastSyncTime);
                return null;
            }
        });
        SyncServiceHandler_Map.put(METHOD.PREPARE, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                LOG.f(SyncClientHelper.TAG, "PREPARE To Sync : " + name);
                String[] syncKey = extras.getStringArray(Key.SYNC_KEY);
                long[] timestampList = extras.getLongArray("timestamp");
                String[] tagList = extras.getStringArray(Key.TAG);
                String accountName = extras.getString("account_name");
                List<SyncItem> items = ((ISCloudSyncClient) SyncClientHelper.getInstance(context).mClientMap.get(name)).prepareToSync(context, syncKey, timestampList, tagList, extras.getString(Key.ACCOUNT_TYPE), accountName);
                Bundle result = new Bundle();
                if (items != null) {
                    int size = items.size();
                    String[] localIdArr = new String[size];
                    String[] syncKeyArr = new String[size];
                    String[] tagArr = new String[size];
                    long[] timestampArr = new long[size];
                    boolean[] deletedArr = new boolean[size];
                    for (int i = 0; i < size; i++) {
                        SyncItem item = (SyncItem) items.get(i);
                        localIdArr[i] = item.getLocalId();
                        syncKeyArr[i] = item.getSyncKey();
                        timestampArr[i] = item.getTimestamp();
                        deletedArr[i] = item.isDeleted();
                        tagArr[i] = item.getTag();
                    }
                    result.putBoolean("is_success", true);
                    result.putStringArray("local_id", localIdArr);
                    result.putStringArray(Key.SYNC_KEY, syncKeyArr);
                    result.putLongArray("timestamp", timestampArr);
                    result.putBooleanArray(Key.DELETED, deletedArr);
                    result.putStringArray(Key.TAG, tagArr);
                }
                return result;
            }
        });
        SyncServiceHandler_Map.put(METHOD.GET_ATTACHMENT_INFO, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                int dataVersion = extras.getInt("data_version");
                LOG.f(SyncClientHelper.TAG, "GET_ATTACHMENT_INFO : " + name + ", v : " + dataVersion);
                Map<String, Long> items = ((ISCloudSyncClient) SyncClientHelper.getInstance(context).mClientMap.get(name)).getAttachmentFileInfo(context, dataVersion, extras.getString("local_id"));
                Bundle result = new Bundle();
                if (items != null) {
                    int size = items.size();
                    String[] fileList = null;
                    long[] timestampList = null;
                    if (size != 0) {
                        fileList = new String[size];
                        timestampList = new long[size];
                        int i = 0;
                        for (String key : items.keySet()) {
                            fileList[i] = key;
                            timestampList[i] = ((Long) items.get(key)).longValue();
                            i++;
                        }
                    }
                    result.putStringArray(Key.FILE_LIST, fileList);
                    result.putLongArray(Key.TIMESTAMP_LIST, timestampList);
                }
                return result;
            }
        });
        SyncServiceHandler_Map.put(METHOD.UPLOAD, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                FileWriter fw;
                Exception e;
                Throwable th;
                int dataVersion = extras.getInt("data_version");
                LOG.f(SyncClientHelper.TAG, "UPLOAD : " + name + ", v : " + dataVersion);
                String localId = extras.getString("local_id");
                String[] toUploadAttFile = extras.getStringArray(Key.UPLOAD_FILE_LIST);
                HashMap<String, ParcelFileDescriptor> fdList = new HashMap();
                String content = ((ISCloudSyncClient) SyncClientHelper.getInstance(context).mClientMap.get(name)).getLocalChange(context, dataVersion, localId, toUploadAttFile, fdList);
                Bundle result = new Bundle();
                FileWriter fw2 = null;
                if (content != null) {
                    try {
                        fw = new FileWriter(((ParcelFileDescriptor) extras.getParcelable(Key.CONTENT_SYNC_FILE)).getFileDescriptor());
                    } catch (Exception e2) {
                        e = e2;
                        try {
                            LOG.e(SyncClientHelper.TAG, "getLocalChange err ", e);
                            result.putBoolean("is_success", false);
                            if (fw2 != null) {
                                try {
                                    fw2.close();
                                } catch (Exception e3) {
                                    e3.printStackTrace();
                                }
                            }
                            return result;
                        } catch (Throwable th2) {
                            th = th2;
                            if (fw2 != null) {
                                try {
                                    fw2.close();
                                } catch (Exception e32) {
                                    e32.printStackTrace();
                                }
                            }
                            throw th;
                        }
                    }
                    try {
                        fw.write(content);
                        LOG.i(SyncClientHelper.TAG, "write content Str : content.sync");
                        fw2 = fw;
                    } catch (Exception e4) {
                        e32 = e4;
                        fw2 = fw;
                        LOG.e(SyncClientHelper.TAG, "getLocalChange err ", e32);
                        result.putBoolean("is_success", false);
                        if (fw2 != null) {
                            fw2.close();
                        }
                        return result;
                    } catch (Throwable th3) {
                        th = th3;
                        fw2 = fw;
                        if (fw2 != null) {
                            fw2.close();
                        }
                        throw th;
                    }
                }
                LOG.i(SyncClientHelper.TAG, "content is null : content.sync");
                result.putBoolean("is_success", true);
                result.putSerializable(Key.UPLOAD_FILE_LIST, fdList);
                if (fw2 != null) {
                    try {
                        fw2.close();
                    } catch (Exception e322) {
                        e322.printStackTrace();
                    }
                }
                return result;
            }
        });
        SyncServiceHandler_Map.put(METHOD.DOWNLOAD, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                HashMap<String, ParcelFileDescriptor> arrayDownloadAttFileMap;
                Throwable e;
                Throwable th;
                Bundle result;
                Bundle bundle;
                int dataVersion = extras.getInt("data_version");
                LOG.f(SyncClientHelper.TAG, "DOWNLOAD : " + name + ", v : " + dataVersion);
                String localId = extras.getString("local_id");
                String syncKey = extras.getString(Key.SYNC_KEY);
                if (extras.containsKey(Key.DOWNLOAD_FILE_LIST)) {
                    arrayDownloadAttFileMap = (HashMap) extras.getSerializable(Key.DOWNLOAD_FILE_LIST);
                } else {
                    arrayDownloadAttFileMap = null;
                }
                String[] toDeleteAttFileList = extras.getStringArray(Key.DELETED_FILE_LIST);
                ParcelFileDescriptor contentDesc = (ParcelFileDescriptor) extras.getParcelable(Key.CONTENT_SYNC_FILE);
                SyncItem item = new SyncItem(localId, syncKey, Long.valueOf(extras.getLong("timestamp", 0)).longValue(), false, null);
                StringBuilder sb = new StringBuilder();
                BufferedReader br = null;
                try {
                    BufferedReader br2 = new BufferedReader(new FileReader(contentDesc.getFileDescriptor()));
                    while (true) {
                        try {
                            String str = br2.readLine();
                            if (str == null) {
                                break;
                            }
                            sb.append(str);
                        } catch (FileNotFoundException e2) {
                            br = br2;
                        } catch (Exception e3) {
                            e = e3;
                            br = br2;
                        } catch (Throwable th2) {
                            th = th2;
                            br = br2;
                        }
                    }
                    LOG.i(SyncClientHelper.TAG, "read content file complete : content.sync");
                    if (br2 != null) {
                        try {
                            br2.close();
                            br = br2;
                        } catch (IOException e4) {
                            e4.printStackTrace();
                            br = br2;
                        }
                    }
                } catch (FileNotFoundException e5) {
                    try {
                        LOG.f(SyncClientHelper.TAG, "no content file for content.sync");
                        if (br != null) {
                            try {
                                br.close();
                            } catch (IOException e42) {
                                e42.printStackTrace();
                            }
                        }
                        result = new Bundle();
                        bundle = result;
                        bundle.putString("local_id", ((ISCloudSyncClient) SyncClientHelper.getInstance(context).mClientMap.get(name)).updateLocal(context, dataVersion, item, sb.toString(), arrayDownloadAttFileMap, toDeleteAttFileList));
                        return result;
                    } catch (Throwable th3) {
                        th = th3;
                        if (br != null) {
                            try {
                                br.close();
                            } catch (IOException e422) {
                                e422.printStackTrace();
                            }
                        }
                        throw th;
                    }
                } catch (Exception e6) {
                    e = e6;
                    LOG.e(SyncClientHelper.TAG, "read content file err. FILE : content.sync", e);
                    if (br != null) {
                        try {
                            br.close();
                        } catch (IOException e4222) {
                            e4222.printStackTrace();
                        }
                    }
                    result = new Bundle();
                    bundle = result;
                    bundle.putString("local_id", ((ISCloudSyncClient) SyncClientHelper.getInstance(context).mClientMap.get(name)).updateLocal(context, dataVersion, item, sb.toString(), arrayDownloadAttFileMap, toDeleteAttFileList));
                    return result;
                }
                result = new Bundle();
                try {
                    bundle = result;
                    bundle.putString("local_id", ((ISCloudSyncClient) SyncClientHelper.getInstance(context).mClientMap.get(name)).updateLocal(context, dataVersion, item, sb.toString(), arrayDownloadAttFileMap, toDeleteAttFileList));
                } catch (UnsupportedOperationException e7) {
                    if (ISCloudSyncClient.FAIL_CORRUPTED_FILE.equals(e7.getMessage())) {
                        result.putBoolean(Key.NEED_RECOVER, true);
                    } else {
                        throw e7;
                    }
                }
                return result;
            }
        });
        SyncServiceHandler_Map.put(METHOD.DELETE, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                LOG.f(SyncClientHelper.TAG, "DELETE : " + name);
                boolean isSuc = ((ISCloudSyncClient) SyncClientHelper.getInstance(context).mClientMap.get(name)).deleteLocal(context, extras.getString("local_id"));
                Bundle result = new Bundle();
                result.putBoolean("is_success", isSuc);
                return result;
            }
        });
        SyncServiceHandler_Map.put(METHOD.COMPLETE, new SyncServiceHandler() {
            public Bundle handleServiceAction(Context context, String name, Bundle extras) {
                LOG.f(SyncClientHelper.TAG, "COMPLETE : " + name);
                String localId = extras.getString("local_id");
                String syncKey = extras.getString(Key.SYNC_KEY);
                long timestamp = extras.getLong("timestamp");
                int rCode = extras.getInt(Key.RCODE);
                boolean isSuc = ((ISCloudSyncClient) SyncClientHelper.getInstance(context).mClientMap.get(name)).complete(context, new SyncItem(localId, syncKey, timestamp, false, null), rCode);
                Bundle result = new Bundle();
                result.putBoolean("is_success", isSuc);
                return result;
            }
        });
    }
}

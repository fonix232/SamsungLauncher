package com.samsung.android.scloud.oem.lib.bnr;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import com.samsung.android.scloud.oem.lib.LOG;
import com.samsung.android.scloud.oem.lib.qbnr.ISCloudQBNRClient;
import com.samsung.android.scloud.oem.lib.qbnr.QBNRClientHelper;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class BNRClientProivder extends ContentProvider {
    private static final String CONTENT = "content://";
    private static final String RESTORE = "restore";
    private static final String TAG = "BNRClientProivder, VERSION : 1.8.0";
    private Map<String, IBNRClientHelper> helperMap;

    public int delete(Uri arg0, String arg1, String[] arg2) {
        return 0;
    }

    public String getType(Uri arg0) {
        return null;
    }

    public Uri insert(Uri arg0, ContentValues arg1) {
        return null;
    }

    public boolean onCreate() {
        LOG.i(TAG, "onCreate() ~!!");
        return true;
    }

    public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3, String arg4) {
        return null;
    }

    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        return 0;
    }

    public Bundle call(String method, String arg, Bundle extras) {
        LOG.i(TAG, "call !!  method : " + method + ", arg : " + arg);
        if (this.helperMap == null) {
            register(getContext().getApplicationContext());
        }
        if (this.helperMap.get(arg) != null) {
            return ((IBNRClientHelper) this.helperMap.get(arg)).handleRequest(getContext().getApplicationContext(), method, arg, extras);
        }
        LOG.i(TAG, "call !!  helperMap has no value : " + arg);
        return null;
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        LOG.i(TAG, "openFile !!  uri : " + uri + ", mode : " + mode);
        String str = uri.toString();
        String[] sptr = str.split("/");
        String filename = sptr[sptr.length - 1];
        ParcelFileDescriptor fd = null;
        LOG.i(TAG, "filename !!  uri : " + filename);
        str = str.replace(CONTENT, "");
        str = str.substring(str.indexOf("/"));
        File file = new File(str);
        if (mode.equals("restore")) {
            if (!(file == null || file.exists())) {
                String subfolder = "";
                LOG.i(TAG, "sub folder : " + str.replace("/" + filename, ""));
                File folder = new File(str.replace("/" + filename, ""));
                if (!(folder == null || folder.exists())) {
                    LOG.i(TAG, "make folders : " + subfolder);
                    folder.mkdirs();
                }
            }
        } else if (filename == null || filename.isEmpty()) {
            throw new UnsupportedOperationException();
        }
        try {
            fd = ParcelFileDescriptor.open(new File(str), 939524096);
        } catch (FileNotFoundException e) {
            LOG.e(TAG, "Unable to open file " + str, e);
        }
        return fd;
    }

    private void register(Context context) {
        try {
            LOG.f(TAG, "register - started.");
            this.helperMap = new HashMap();
            for (String file : context.getAssets().list("res/xml")) {
                LOG.d(TAG, "register - list : " + file);
            }
            XmlResourceParser xml = context.getResources().getAssets().openXmlResourceParser("res/xml/backup_item.xml");
            LOG.d(TAG, "register - xml1 : " + xml.getName());
            xml.next();
            LOG.d(TAG, "register - xml2 : " + xml.getName());
            xml.next();
            LOG.d(TAG, "register - xml3 : " + xml.getName());
            if (xml.getName().equals("backup_items")) {
                while (true) {
                    if (xml.next() != 3 || !xml.getName().equals("backup_items")) {
                        LOG.d(TAG, "register - xml4 : " + xml.getName());
                        if (xml.getName().equals("backup_item") && xml.getEventType() == 2) {
                            String name = xml.getAttributeValue(null, "name");
                            String contentsId = xml.getAttributeValue(null, "contents_id");
                            String clientImplClass = xml.getAttributeValue(null, "client_impl_class");
                            String category = xml.getAttributeValue(null, "category");
                            LOG.d(TAG, "register - xml5 : " + name + ", " + contentsId + ", " + clientImplClass + ", " + category);
                            try {
                                if ("true".equals(xml.getAttributeValue(null, "quick_backup"))) {
                                    LOG.f(TAG, "register - xml6 quick_backup : " + name + ", " + contentsId + ", " + clientImplClass);
                                    this.helperMap.put(name, new QBNRClientHelper(context, name, (ISCloudQBNRClient) Class.forName(clientImplClass).newInstance(), contentsId, category));
                                } else {
                                    LOG.f(TAG, "register - xml6 : " + name + ", " + contentsId + ", " + clientImplClass);
                                    this.helperMap.put(name, new BNRClientHelper(context, name, (ISCloudBNRClient) Class.forName(clientImplClass).newInstance(), contentsId, category));
                                }
                            } catch (ClassCastException e) {
                                LOG.e(TAG, "failed cast to BNRClient~!! ", e);
                            }
                        }
                    } else {
                        return;
                    }
                }
            }
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }
}

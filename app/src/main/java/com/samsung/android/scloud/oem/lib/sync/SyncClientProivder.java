package com.samsung.android.scloud.oem.lib.sync;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import com.samsung.android.scloud.oem.lib.LOG;
import com.samsung.android.scloud.oem.lib.SCloudUtil;
import java.io.File;
import java.io.FileNotFoundException;

public class SyncClientProivder extends ContentProvider {
    private static final String TAG = "SyncClientProivder";

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
        return false;
    }

    public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3, String arg4) {
        return null;
    }

    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        return 0;
    }

    public Bundle call(String method, String arg, Bundle extras) {
        LOG.i(TAG, "call !!  method : " + method + ", arg : " + arg);
        try {
            return SyncClientHelper.getInstance(getContext()).handleRequest(getContext(), method, arg, extras);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        LOG.i(TAG, "openFile !!  uri : " + uri + ", mode : " + mode);
        ParcelFileDescriptor fd = null;
        String filename = uri.toString().substring(uri.toString().lastIndexOf("/") + 1);
        LOG.i(TAG, "filename !!  name : " + filename);
        if (filename == null || filename.isEmpty()) {
            throw new UnsupportedOperationException();
        }
        SCloudUtil.ensureValidFileName(filename);
        File dir = getContext().getFilesDir();
        if (!(dir.exists() || dir.mkdir())) {
            LOG.e(TAG, "mkdir() failed ", null);
        }
        String filepath = dir.getAbsolutePath() + "/" + filename;
        File file = new File(filepath);
        LOG.i(TAG, "openFile result local file : " + filepath);
        try {
            fd = ParcelFileDescriptor.open(file, 939524096);
        } catch (FileNotFoundException e) {
            LOG.e(TAG, "Unable to open file " + filepath, e);
        }
        return fd;
    }
}

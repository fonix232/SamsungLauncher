package com.android.launcher3.common.bnr.scloud;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.bnr.LauncherBnrHelper;
import com.android.launcher3.common.bnr.LauncherBnrListener;
import com.android.launcher3.common.bnr.LauncherBnrListener.Result;
import com.samsung.android.scloud.oem.lib.FileTool;
import com.samsung.android.scloud.oem.lib.FileTool.PDMProgressListener;
import com.samsung.android.scloud.oem.lib.qbnr.ISCloudQBNRClient;
import com.samsung.android.scloud.oem.lib.qbnr.ISCloudQBNRClient.QuickBackupListener;
import com.sec.android.app.launcher.R;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SCloudBnr implements ISCloudQBNRClient, LauncherBnrListener {
    public static final String SCLOUD_DIR_PATH = "/BackupRestore";
    public static final String SCLOUD_RESTORE_PATH = "/restore";
    public static final String SCLOUD_SOURCE = "SCLOUD";
    private static final String TAG = "Launcher.SCloudBnr";
    private ParcelFileDescriptor mFile = null;
    private QuickBackupListener mListener = null;

    public boolean isSupportBackup(Context context) {
        return true;
    }

    public boolean isEnableBackup(Context context) {
        return true;
    }

    public String getLabel(Context context) {
        if (LauncherFeature.isJapanModel()) {
            return context.getResources().getString(R.string.application_name_galaxy);
        }
        return context.getResources().getString(R.string.application_name);
    }

    public String getDescription(Context context) {
        if (LauncherFeature.isJapanModel()) {
            return context.getResources().getString(R.string.application_name_galaxy);
        }
        return context.getResources().getString(R.string.application_name);
    }

    public void backup(Context context, ParcelFileDescriptor file, QuickBackupListener listener) {
        Log.d(TAG, "SCloud backup");
        this.mFile = file;
        this.mListener = listener;
        String dirPath = context.getFilesDir() + SCLOUD_DIR_PATH;
        LauncherBnrHelper.deleteDir(dirPath + SCLOUD_RESTORE_PATH);
        Log.d(TAG, "backup file creating start");
        LauncherBnrHelper.getInstance().backup(context, dirPath, SCLOUD_SOURCE, this);
        LauncherBnrHelper.deleteDir(dirPath);
    }

    public void restore(Context context, ParcelFileDescriptor file, final QuickBackupListener listener) {
        Log.d(TAG, "SCloud restore");
        String dirPath = context.getFilesDir() + SCLOUD_DIR_PATH;
        String zipFile = dirPath + LauncherBnrHelper.HOMESCREEN_BACKUP_EXML;
        String restorePath = dirPath + SCLOUD_RESTORE_PATH;
        File target = new File(zipFile);
        LauncherBnrHelper.deleteDir(restorePath);
        if (file != null) {
            InputStream fin = new FileInputStream(file.getFileDescriptor());
            Log.e(TAG, "file.getStatSize(): " + file.getStatSize());
            Log.e(TAG, "target.getAbsolutePath(): " + target.getAbsolutePath());
            try {
                Log.d(TAG, "file write");
                FileTool.writeToFile(fin, file.getStatSize(), target.getAbsolutePath(), new PDMProgressListener() {
                    public void transferred(long now, long total) {
                        listener.onProgress(now, total);
                    }
                });
                Log.d(TAG, "restoring");
                unzip(zipFile, restorePath);
                LauncherBnrHelper.getInstance().restore(context, restorePath, SCLOUD_SOURCE, 0, this, null);
                listener.complete(true);
            } catch (IOException e) {
                listener.complete(false);
            } finally {
                LauncherBnrHelper.close(fin);
            }
        } else {
            Log.e(TAG, "failed, file is null...");
            listener.complete(false);
        }
    }

    public void backupComplete(Result result, File saveFile) {
        IOException e;
        Throwable th;
        Log.i(TAG, "backupComplete result : " + result.result);
        if (result.result == 0) {
            OutputStream out = null;
            try {
                Log.d(TAG, "backup file write");
                OutputStream out2 = new FileOutputStream(this.mFile.getFileDescriptor());
                try {
                    FileTool.writeToFile(saveFile.getPath(), saveFile.length(), (FileOutputStream) out2, new PDMProgressListener() {
                        public void transferred(long now, long total) {
                            SCloudBnr.this.mListener.onProgress(now, total);
                        }
                    });
                    LauncherBnrHelper.close(out2);
                    this.mListener.complete(true);
                    return;
                } catch (IOException e2) {
                    e = e2;
                    out = out2;
                    try {
                        Log.e(TAG, NotificationCompat.CATEGORY_ERROR, e);
                        this.mListener.complete(false);
                        LauncherBnrHelper.close(out);
                        return;
                    } catch (Throwable th2) {
                        th = th2;
                        LauncherBnrHelper.close(out);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    out = out2;
                    LauncherBnrHelper.close(out);
                    throw th;
                }
            } catch (IOException e3) {
                e = e3;
                Log.e(TAG, NotificationCompat.CATEGORY_ERROR, e);
                this.mListener.complete(false);
                LauncherBnrHelper.close(out);
                return;
            }
        }
        this.mListener.complete(false);
    }

    public void restoreComplete(Result result, File saveFile) {
        Log.i(TAG, "restoreComplete result : " + result.result);
    }

    public OutputStream getEncryptStream(FileOutputStream fos) throws GeneralSecurityException, IOException {
        return fos;
    }

    public InputStream getDecryptStream(FileInputStream fis) throws GeneralSecurityException, IOException {
        return fis;
    }

    private void unzip(String zipFile, String unzipLocation) {
        Exception e;
        InputStream zin = null;
        OutputStream fOut = null;
        try {
            Log.e(TAG, "unzip start zipFile = " + zipFile);
            InputStream zin2 = new ZipInputStream(new FileInputStream(zipFile));
            try {
                File unzipFolder = new File(unzipLocation);
                if (!unzipFolder.isDirectory()) {
                    unzipFolder.mkdirs();
                }
                FileOutputStream fOut2 = null;
                while (true) {
                    try {
                        ZipEntry ze = zin2.getNextEntry();
                        if (ze != null) {
                            Log.d(TAG, "unzipping " + ze.getName());
                            if (ze.isDirectory()) {
                                File f = new File(unzipLocation + '/' + ze.getName());
                                if (!f.isDirectory()) {
                                    f.mkdirs();
                                }
                            } else {
                                fOut = new FileOutputStream(unzipLocation + '/' + ze.getName());
                                byte[] b = new byte[1024];
                                while (true) {
                                    int n = zin2.read(b);
                                    if (n == -1) {
                                        break;
                                    }
                                    fOut.write(b, 0, n);
                                }
                                zin2.closeEntry();
                                fOut.close();
                                fOut2 = fOut;
                            }
                        } else {
                            Log.d(TAG, "unzip end");
                            zin2.close();
                            fOut = fOut2;
                            zin = zin2;
                            return;
                        }
                    } catch (Exception e2) {
                        e = e2;
                        fOut = fOut2;
                        zin = zin2;
                    }
                }
            } catch (Exception e3) {
                e = e3;
                zin = zin2;
            }
        } catch (Exception e4) {
            e = e4;
            Log.e(TAG, "unzip", e);
            LauncherBnrHelper.close(fOut);
            LauncherBnrHelper.close(zin);
        }
    }

    public static boolean isWillRestored(Context context, ComponentName cn) {
        if (cn == null) {
            return false;
        }
        File dir = new File(context.getFilesDir() + SCLOUD_DIR_PATH + SCLOUD_RESTORE_PATH);
        if (!dir.exists()) {
            return false;
        }
        String[] fileList = dir.list();
        if (fileList == null) {
            return false;
        }
        Log.d(TAG, "dir fileList.length: " + fileList.length);
        for (String filename : fileList) {
            String[] splits = filename.substring(0, filename.length() - 5).split("@");
            if (splits.length == 3 && splits[1].equals(cn.getPackageName()) && splits[2].equals(cn.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void getRestoreDummyInfo(Context context, ComponentName cn, IconInfo item) {
        if (cn != null) {
            String packageName = cn.getPackageName();
            String className = cn.getClassName();
            String dirPath = context.getFilesDir() + SCLOUD_DIR_PATH + SCLOUD_RESTORE_PATH;
            File dir = new File(dirPath);
            if (dir.exists()) {
                String[] fileList = dir.list();
                if (fileList != null) {
                    Log.d(TAG, "dir fileList.length: " + fileList.length);
                    for (String filename : fileList) {
                        String[] splits = filename.substring(0, filename.length() - 5).split("@");
                        if (splits.length == 3 && splits[1].equals(packageName) && splits[2].equals(className)) {
                            Log.d(TAG, "find title and icon packageName = " + packageName);
                            if (TextUtils.isEmpty(splits[0])) {
                                item.title = "";
                            } else {
                                item.title = Utilities.trim(splits[0]);
                            }
                            Bitmap bitmap = BitmapFactory.decodeFile(dirPath + '/' + filename);
                            if (bitmap != null) {
                                item.setIcon(bitmap);
                            }
                        }
                    }
                }
            }
        }
    }
}

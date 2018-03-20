package com.android.launcher3.common.bnr.extractor;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.bnr.LauncherBnrHelper;
import com.android.launcher3.util.PermissionUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class LCExtractor {
    public static final String ACTION_INTENT_LCEXTRACTOR = "com.sec.android.intent.action.LCEXTRACTOR";
    public static final int EXTRACT_TYPE_HOMEDATA = 1;
    public static final int EXTRACT_TYPE_LAYOUT = 0;
    public static final int EXTRACT_TYPE_NO = -1;
    private static final String FILE_NAME_APPS = "default_application_order";
    private static final String FILE_NAME_WORKSPACE = "default_workspace";
    public static final String HOMEDATA_DIR = ".homedata";
    public static final String HOMESCREEN_DIR = ".homescreen";
    public static final String LCEXTRACTOR_APPS_SOURCE = "LCExtractorApps";
    public static final String LCEXTRACTOR_HOME_SOURCE = "LCExtractorHome";
    public static final String SD_DIRECTORY = "/LCExtractor";
    private static final String TAG = "Launcher.Extractor";
    private static final String[] strTab = new String[]{"", "    ", "        ", "            ", "                ", "                    ", "                        "};
    private Context mContext;
    private int mExtractType;
    private boolean mIsEasyMode;
    private boolean mIsHomeOnly;

    public LCExtractor(Context context, int extractType) {
        this.mContext = context;
        this.mExtractType = extractType;
        if (LauncherFeature.supportHomeModeChange()) {
            this.mIsHomeOnly = LauncherAppState.getInstance().isHomeOnlyModeEnabled();
        } else if (LauncherFeature.supportEasyModeChange()) {
            this.mIsEasyMode = LauncherAppState.getInstance().isEasyModeEnabled();
        }
    }

    public void checkCondition() {
        ArrayList<String> needPermissionsList = new ArrayList();
        int value = PermissionUtils.hasSelfPermission(this.mContext, PermissionUtils.PERMISSIONS_STORAGE, needPermissionsList);
        Log.d(TAG, "hasSelfPermission : " + value);
        if (value != 0) {
            Log.d(TAG, "No storage permission in TouchWizHome");
            Launcher launcher = this.mContext;
            if (PermissionUtils.shouldShowRequestPermissionRationale(launcher, needPermissionsList)) {
                Log.d(TAG, "Permission denied.");
                return;
            } else {
                PermissionUtils.requestPermissions(launcher, needPermissionsList, 2);
                return;
            }
        }
        startExtractLayout();
    }

    public void startExtractLayout() {
        if (this.mExtractType == 1) {
            new AsyncTask<Void, Void, Void>() {
                public Void doInBackground(Void... args) {
                    try {
                        LCExtractor.this.extractData();
                    } catch (IOException e) {
                    }
                    return null;
                }

                protected void onPostExecute(Void aVoid) {
                    Toast.makeText(LCExtractor.this.mContext, "copy complete !", 1).show();
                }
            }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR, new Void[0]);
        } else if (this.mExtractType == 0) {
            Toast.makeText(this.mContext, "Extracting the home screen layout.", 1).show();
            new Thread(new Runnable() {
                public void run() {
                    LauncherBnrHelper bnrHelper = LauncherBnrHelper.getInstance();
                    StringBuffer fileNameWorkspace = new StringBuffer(LCExtractor.FILE_NAME_WORKSPACE);
                    if (LCExtractor.this.mIsEasyMode) {
                        fileNameWorkspace.append("_easy");
                    } else if (LCExtractor.this.mIsHomeOnly) {
                        fileNameWorkspace.append("_homeonly");
                    }
                    fileNameWorkspace.append(".xml");
                    StringBuffer fileNameApps = new StringBuffer(LCExtractor.FILE_NAME_APPS);
                    if (LCExtractor.this.mIsEasyMode) {
                        fileNameApps.append("_easy");
                    }
                    fileNameApps.append(".xml");
                    StringBuffer createMsg = new StringBuffer();
                    createMsg.append(fileNameWorkspace);
                    if (LCExtractor.this.mIsEasyMode || !LCExtractor.this.mIsHomeOnly) {
                        createMsg.append(", ").append(fileNameApps);
                    }
                    createMsg.append(" is creating...");
                    Log.d(LCExtractor.TAG, createMsg.toString());
                    File dir = new File(Environment.getExternalStorageDirectory(), LCExtractor.SD_DIRECTORY);
                    if (dir.exists()) {
                        String[] fileList = dir.list();
                        if (fileList != null) {
                            Log.d(LCExtractor.TAG, "dir fileList.length : " + fileList.length);
                            for (String filename : fileList) {
                                if (!new File(Environment.getExternalStorageDirectory() + LCExtractor.SD_DIRECTORY + '/' + filename).delete()) {
                                    Log.e(LCExtractor.TAG, "file : " + filename + ", delete failed");
                                }
                            }
                        }
                    } else {
                        dir.mkdirs();
                    }
                    if (!bnrHelper.extractXML(LCExtractor.this.mContext, fileNameWorkspace.toString(), LCExtractor.LCEXTRACTOR_HOME_SOURCE)) {
                        Log.e(LCExtractor.TAG, "makeDefaultWorkspace() is failed");
                    }
                    if ((LCExtractor.this.mIsEasyMode || !LCExtractor.this.mIsHomeOnly) && !bnrHelper.extractXML(LCExtractor.this.mContext, fileNameApps.toString(), LCExtractor.LCEXTRACTOR_APPS_SOURCE)) {
                        Log.e(LCExtractor.TAG, "makeDefaultAppOrder() is failed.");
                    }
                }
            }).start();
        }
    }

    public static String getStrTab(int depth, boolean launcherPrefix) {
        if (launcherPrefix) {
            return strTab[depth] + "launcher:";
        }
        return strTab[depth];
    }

    private void extractData() throws IOException {
        File sdDir = Environment.getExternalStorageDirectory();
        File dataDir = Environment.getDataDirectory();
        if (sdDir.canWrite()) {
            File curData = new File(dataDir, "//data//com.sec.android.app.launcher");
            File extData = new File(sdDir, "//.homedata//");
            recusiveDeleteData(extData);
            recusiveCopyData(curData, extData);
            return;
        }
        Toast.makeText(this.mContext, "sd card can't write!", 1).show();
    }

    private void recusiveDeleteData(File target) {
        if (target.isDirectory()) {
            File[] files = target.listFiles();
            if (files != null) {
                for (File aChildren : files) {
                    recusiveDeleteData(aChildren);
                }
            }
        }
        target.delete();
    }

    private void recusiveCopyData(File source, File target) throws IOException {
        int i = 0;
        if (!source.isDirectory()) {
            File directory = target.getParentFile();
            if (directory == null || directory.exists() || directory.mkdirs()) {
                InputStream in = new FileInputStream(source);
                OutputStream out = new FileOutputStream(target);
                byte[] buf = new byte[2048];
                while (true) {
                    int len = in.read(buf);
                    if (len > 0) {
                        out.write(buf, 0, len);
                    } else {
                        in.close();
                        out.close();
                        return;
                    }
                }
            }
            throw new IOException("Cannot create dir " + directory.getAbsolutePath());
        } else if (target.exists() || target.mkdir()) {
            String[] children = source.list();
            if (children != null) {
                int length = children.length;
                while (i < length) {
                    String aChildren = children[i];
                    recusiveCopyData(new File(source, aChildren), new File(target, aChildren));
                    i++;
                }
            }
        } else {
            throw new IOException("Cannot create dir " + target.getAbsolutePath());
        }
    }
}

package com.samsung.android.scloud.oem.lib;

import android.os.Build;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class LOG {
    public static final int A = 7;
    public static final int D = 3;
    public static final int E = 6;
    public static final int F = 0;
    public static final int I = 4;
    private static final String LOG_EXT = ".log";
    static final int MAX_FILE_SIZE = 5242880;
    private static final String MODULE = "sCloudLib";
    private static final String TAG = "PDMLogs";
    public static final int V = 2;
    public static final int W = 5;
    private static boolean bFileLogEnabled = false;
    private static boolean bLogEnabled;
    static final SimpleDateFormat formatter = new SimpleDateFormat("MM.dd_HH-mm-ss", Locale.KOREA);
    static final String logPath = (Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + TAG + "/" + MODULE + "/");
    static File mFile;
    static PrintWriter mWriter;

    static {
        bLogEnabled = false;
        if ("eng".equals(Build.TYPE)) {
            bLogEnabled = true;
        }
    }

    private static synchronized PrintWriter getLogWriter() {
        PrintWriter printWriter;
        synchronized (LOG.class) {
            if (mWriter == null) {
                if (mWriter == null) {
                    try {
                        File folderPath = new File(logPath);
                        if (!folderPath.exists()) {
                            folderPath.mkdirs();
                            Log.i(TAG, "create dir : " + logPath);
                        }
                        mFile = new File(logPath + formatter.format(Long.valueOf(System.currentTimeMillis())) + LOG_EXT);
                        mWriter = new PrintWriter(new FileWriter(mFile));
                        Log.i(TAG, "create writer : " + logPath);
                    } catch (Exception e) {
                        Log.i(TAG, "create error : " + e.getMessage());
                        e.printStackTrace();
                        if (mWriter != null) {
                            mWriter.close();
                        }
                        mWriter = null;
                    }
                }
            } else if (mFile.length() > 5242880 && mFile.length() > 5242880) {
                try {
                    mWriter.close();
                    mFile = new File(logPath + formatter.format(Long.valueOf(System.currentTimeMillis())) + LOG_EXT);
                    mWriter = new PrintWriter(mFile);
                    Log.i(TAG, "create writer : " + logPath);
                } catch (Exception e2) {
                    Log.i(TAG, "create error : " + e2.getMessage());
                    e2.printStackTrace();
                    if (mWriter != null) {
                        mWriter.close();
                    }
                    mWriter = null;
                }
            }
            printWriter = mWriter;
        }
        return printWriter;
    }

    private static void writeLog(String tag, String msg) {
        writeLog(tag, msg, null);
    }

    private static void writeLog(String tag, String msg, Throwable tr) {
        if (bFileLogEnabled) {
            synchronized (formatter) {
                try {
                    PrintWriter writer = getLogWriter();
                    if (writer != null) {
                        writer.write("[" + formatter.format(Long.valueOf(System.currentTimeMillis())) + "][" + tag + "]" + msg + "\n");
                        if (tr != null) {
                            tr.printStackTrace(writer);
                        }
                        writer.flush();
                    }
                } catch (Exception e) {
                    Log.i(TAG, "write error : " + e.getMessage());
                    e.printStackTrace();
                    if (mWriter != null) {
                        mWriter.close();
                    }
                    mWriter = null;
                }
            }
        }
    }

    public static void log(int level, String tag, String msg) {
        switch (level) {
            case 0:
                f(tag, msg);
                return;
            case 2:
                v(tag, msg);
                return;
            case 3:
                d(tag, msg);
                return;
            case 4:
                i(tag, msg);
                return;
            case 5:
                w(tag, msg);
                return;
            case 6:
                f(tag, msg);
                return;
            case 7:
                d(tag, msg);
                return;
            default:
                return;
        }
    }

    public static void i(String tag, String msg) {
        if (bLogEnabled && msg != null) {
            Log.i(tag, msg);
            writeLog(tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (bLogEnabled && msg != null) {
            Log.d(tag, msg);
            writeLog(tag, msg);
        }
    }

    public static void v(String Tag, String msg) {
        if (bLogEnabled && msg != null) {
            Log.v(Tag, msg);
            writeLog(Tag, msg);
        }
    }

    public static void w(String Tag, String msg) {
        if (bLogEnabled && msg != null) {
            Log.w(Tag, msg);
            writeLog(Tag, msg);
        }
    }

    public static void e(String Tag, String msg, Throwable e) {
        Log.e("SCLOUD_ERR-" + Tag, msg, e);
        writeLog("SCLOUD_ERR-" + Tag, msg);
    }

    public static void e(String Tag, String msg) {
        e(Tag, msg, null);
    }

    public static void f(String Tag, String msg) {
        if (msg != null) {
            Log.i(Tag, msg);
            writeLog(Tag, msg);
        }
    }
}

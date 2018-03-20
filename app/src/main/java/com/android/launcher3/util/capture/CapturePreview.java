package com.android.launcher3.util.capture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings.System;
import android.util.Log;
import android.view.ViewGroup;
import com.android.launcher3.Utilities;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CapturePreview {
    private static final int CAPTURE_MIN_SIZE = 10000;
    private static final int DEFAULT_CAPTURE_DELAY = 3000;
    private static final int DIRTY_LAND = 2;
    private static final int DIRTY_PORT = 1;
    private static final String FILE_NAME_LAND = "/homescreenPreviewLand.png";
    private static final String FILE_NAME_PORT = "/homescreenPreview.png";
    private static final String KEY_FILE_TIME_LAND = "homescreenPreview_capturetime_land";
    private static final String KEY_FILE_TIME_PORT = "homescreenPreview_capturetime";
    private static final int MSG_REQUEST_CAPTURE = 0;
    private static final String TAG = "CapturePreview";
    private final Context mContext;
    private int mDirtyFlags = 0;
    private String mExternalCacheDirPath;
    private final Handler mHandler;
    private CaptureListener mListener;
    private SaveTask mSaveTask;

    public interface CaptureListener {
        boolean canCapture();

        ViewGroup getTargetView();
    }

    private class SaveTask extends AsyncTask<Void, Void, Boolean> {
        Bitmap bitmap;
        long captureTime;

        public SaveTask(Bitmap bitmap, long captureTime) {
            this.bitmap = bitmap;
            this.captureTime = captureTime;
        }

        protected Boolean doInBackground(Void... voids) {
            return Boolean.valueOf(saveCapturedBitmap(this.bitmap, this.captureTime));
        }

        public void onCancelled() {
            Log.d(CapturePreview.TAG, "cancelled capturetask");
            if (this.bitmap != null) {
                this.bitmap.recycle();
                this.bitmap = null;
            }
        }

        protected void onPostExecute(Boolean result) {
            if (!result.booleanValue()) {
                Log.d(CapturePreview.TAG, "save fail");
            }
            if (this.bitmap != null) {
                this.bitmap.recycle();
                this.bitmap = null;
            }
        }

        private boolean saveCapturedBitmap(Bitmap bitmap, long captureTime) {
            if (bitmap == null) {
                Log.d(CapturePreview.TAG, "fail to save because bitmap was wrong : " + bitmap);
                return false;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(CompressFormat.PNG, 100, out);
            Log.d(CapturePreview.TAG, "start save : compress capture bitmap : " + out.size());
            if (out.size() < CapturePreview.CAPTURE_MIN_SIZE) {
                Log.d(CapturePreview.TAG, "fail to save because size was too small : " + out.size());
                return false;
            } else if (isCancelled()) {
                return false;
            } else {
                boolean isLand;
                if ((((float) bitmap.getWidth()) * 1.0f) / ((float) bitmap.getHeight()) > 1.0f) {
                    isLand = true;
                } else {
                    isLand = false;
                }
                if (!saveToFileCaptureScreen(out, isLand)) {
                    return false;
                }
                String captureTimeStr = isLand ? CapturePreview.KEY_FILE_TIME_PORT : CapturePreview.KEY_FILE_TIME_LAND;
                try {
                    if (CapturePreview.this.mContext.getContentResolver() != null) {
                        System.putLong(CapturePreview.this.mContext.getContentResolver(), captureTimeStr, captureTime);
                    }
                } catch (Exception e) {
                    Log.w(CapturePreview.TAG, "fail to write the capture time to setting db");
                }
                if (!isCancelled()) {
                    CapturePreview.this.mDirtyFlags = (isLand ? -3 : -2) & CapturePreview.this.mDirtyFlags;
                }
                return true;
            }
        }

        private File getTargetFile(boolean land) {
            String cacheDir = CapturePreview.this.getExternalCacheDirPath(CapturePreview.this.mContext);
            if (cacheDir == null) {
                return null;
            }
            return new File(cacheDir + (land ? CapturePreview.FILE_NAME_LAND : CapturePreview.FILE_NAME_PORT));
        }

        private boolean saveToFileCaptureScreen(ByteArrayOutputStream stream, boolean land) {
            String filePath;
            Throwable th;
            boolean z = false;
            File file = getTargetFile(land);
            if (!(file == null || file.exists())) {
                CapturePreview.this.mExternalCacheDirPath = null;
                file = getTargetFile(land);
            }
            if (file != null) {
                filePath = file.getAbsolutePath();
            } else {
                filePath = null;
            }
            if (file == null || stream == null) {
                Log.w(CapturePreview.TAG, "Null: File or OutputStream");
            } else {
                OutputStream out = null;
                try {
                    OutputStream out2 = new FileOutputStream(file);
                    try {
                        stream.writeTo(out2);
                        Log.d(CapturePreview.TAG, "save result :  path : " + filePath);
                        z = true;
                        if (out2 != null) {
                            try {
                                out2.flush();
                                out2.close();
                            } catch (IOException e) {
                                Log.w(CapturePreview.TAG, "IOException OutputStream.flush : ");
                            }
                        }
                    } catch (FileNotFoundException e2) {
                        out = out2;
                        try {
                            Log.w(CapturePreview.TAG, "FileNotFoundException : " + filePath);
                            if (out != null) {
                                try {
                                    out.flush();
                                    out.close();
                                } catch (IOException e3) {
                                    Log.w(CapturePreview.TAG, "IOException OutputStream.flush : ");
                                }
                            }
                            return z;
                        } catch (Throwable th2) {
                            th = th2;
                            if (out != null) {
                                try {
                                    out.flush();
                                    out.close();
                                } catch (IOException e4) {
                                    Log.w(CapturePreview.TAG, "IOException OutputStream.flush : ");
                                }
                            }
                            throw th;
                        }
                    } catch (IOException e5) {
                        out = out2;
                        Log.w(CapturePreview.TAG, "IOException ByteArrayOutputStream.writeTo : " + filePath);
                        if (out != null) {
                            try {
                                out.flush();
                                out.close();
                            } catch (IOException e6) {
                                Log.w(CapturePreview.TAG, "IOException OutputStream.flush : ");
                            }
                        }
                        return z;
                    } catch (Throwable th3) {
                        th = th3;
                        out = out2;
                        if (out != null) {
                            out.flush();
                            out.close();
                        }
                        throw th;
                    }
                } catch (FileNotFoundException e7) {
                    Log.w(CapturePreview.TAG, "FileNotFoundException : " + filePath);
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                    return z;
                } catch (IOException e8) {
                    Log.w(CapturePreview.TAG, "IOException ByteArrayOutputStream.writeTo : " + filePath);
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                    return z;
                }
            }
            return z;
        }
    }

    public CapturePreview(Context context) {
        this.mContext = context;
        this.mHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 0:
                        CapturePreview.this.startCaptureScreen();
                        return;
                    default:
                        return;
                }
            }
        };
    }

    public void notifyCapture(boolean immediate) {
        notifyCapture(immediate, 3000);
    }

    private void notifyCapture(boolean immediate, int delay) {
        this.mDirtyFlags = 3;
        if (immediate) {
            if (delay < 0) {
                delay = 3000;
            }
            sendMessageForCapture(delay, Utilities.getOrientation());
        }
    }

    public void notifyCaptureIfNecessary() {
        notifyCaptureIfNecessary(3000);
    }

    private void notifyCaptureIfNecessary(int delay) {
        int orientation = Utilities.getOrientation();
        if (((this.mDirtyFlags & 1) != 0 && orientation == 1) || ((this.mDirtyFlags & 2) != 0 && orientation == 2)) {
            notifyCapture(true, delay);
        }
    }

    public void setListener(CaptureListener listener) {
        this.mListener = listener;
    }

    public void stopCapture() {
        this.mHandler.removeCallbacksAndMessages(null);
    }

    private void sendMessageForCapture(int delay, int orientation) {
        Log.d(TAG, "sendMessageForCapture");
        cancelCompressTask();
        this.mHandler.removeMessages(0);
        Message msg = new Message();
        msg.what = 0;
        msg.arg1 = orientation;
        this.mHandler.sendMessageDelayed(msg, (long) delay);
    }

    private void cancelCompressTask() {
        if (this.mSaveTask != null) {
            this.mSaveTask.cancel(true);
            this.mSaveTask = null;
        }
    }

    private void startCaptureScreen() {
        boolean valid = true;
        if (this.mListener == null || !this.mListener.canCapture()) {
            Log.d(TAG, "startCaptureScreen fail : " + this.mListener);
            return;
        }
        if (Utilities.getOrientation() == 1) {
            if ((this.mDirtyFlags & 1) == 0) {
                valid = false;
            }
        } else if ((this.mDirtyFlags & 2) == 0) {
            valid = false;
        }
        if (valid) {
            Bitmap bitmap = getCaptureBitmap(this.mListener.getTargetView());
            if (bitmap == null || bitmap.getWidth() <= 0 || bitmap.getWidth() <= 0) {
                Log.e(TAG, "capture fail : " + bitmap);
                return;
            }
            cancelCompressTask();
            this.mSaveTask = new SaveTask(bitmap, System.currentTimeMillis());
            this.mSaveTask.execute(new Void[0]);
            return;
        }
        Log.d(TAG, "startCaptureScreen fail becuasue current orientation was not requested");
    }

    private Bitmap getCaptureBitmap(ViewGroup targetView) {
        if (targetView == null) {
            return null;
        }
        if (targetView.getChildAt(0) == null) {
            Log.d(TAG, "child views were not ready");
            return null;
        }
        int viewWidth = targetView.getWidth();
        int viewHeight = targetView.getHeight();
        if (viewWidth <= 0 || viewHeight <= 0) {
            Log.d(TAG, "targetView might not have been measured yet" + viewWidth + " , " + viewHeight);
            return null;
        }
        long cur = System.currentTimeMillis();
        Log.d(TAG, "start capture - drawaingEnabled : " + targetView.isDrawingCacheEnabled());
        targetView.setDrawingCacheEnabled(false);
        Log.d(TAG, "capture tagetview width : " + viewWidth + " height : " + viewHeight);
        Bitmap bitmap = Bitmap.createBitmap(viewWidth, viewHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        targetView.draw(canvas);
        canvas.setBitmap(null);
        Log.d(TAG, "capture took  " + (System.currentTimeMillis() - cur));
        return bitmap;
    }

    private String getExternalCacheDirPath(Context context) {
        if (this.mExternalCacheDirPath == null) {
            File dir = context.getExternalCacheDir();
            if (dir == null || !dir.exists()) {
                Log.e(TAG, "Fail to getExternalCacheDirPath");
            } else {
                this.mExternalCacheDirPath = dir.getAbsolutePath();
            }
        }
        return this.mExternalCacheDirPath;
    }
}

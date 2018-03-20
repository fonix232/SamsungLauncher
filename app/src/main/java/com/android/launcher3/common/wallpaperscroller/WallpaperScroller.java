package com.android.launcher3.common.wallpaperscroller;

import android.app.WallpaperManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;

public class WallpaperScroller {
    private static final float EASING_VALUE = 0.333f;
    private static final boolean FEATURE_IS_WALLPAPER_USE_FIXED_ORIENTATION = LauncherFeature.isWallpaperUseFixedOrientation();
    private static final String TAG = "WallpaperScroller";
    private Context mContext;
    private VectorListener mGyroListener = new VectorListener() {
        public void onVectorChanged(float x, float y, float tiltRangeX, float tiltRangeY, float rangeX, float rangeY) {
            if (WallpaperScroller.this.mTiltUpdateHandler != null) {
                WallpaperScroller.this.mTiltUpdateHandler.update(x, y, tiltRangeX, tiltRangeY, rangeX, rangeY);
            }
        }
    };
    private HandlerThread mHandlerThread = null;
    private boolean mIsRunning = false;
    private WallpaperThread mThread;
    private TiltUpdateHandler mTiltUpdateHandler = null;

    private class TiltUpdateHandler extends Handler {
        private static final int MSG_TILT_UPDATE = 1;
        private float mTiltRangeX;
        private float mTiltRangeY;
        private float mWallpaperRangeX;
        private float mWallpaperRangeY;

        private TiltUpdateHandler(Looper looper) {
            super(looper);
            this.mWallpaperRangeX = 0.5f;
            this.mWallpaperRangeY = 0.5f;
            this.mTiltRangeX = 0.0f;
            this.mTiltRangeY = 0.0f;
        }

        private synchronized void shutdown() {
            removeCallbacksAndMessages(null);
        }

        private synchronized void update(float x, float y, float tiltRangeX, float tiltRangeY, float rangeX, float rangeY) {
            this.mTiltRangeX = tiltRangeX;
            this.mTiltRangeY = tiltRangeY;
            this.mWallpaperRangeX = rangeX;
            this.mWallpaperRangeY = rangeY;
            sendMessage(obtainMessage(1, new TiltValue(x, y, tiltRangeX, tiltRangeY, rangeX, rangeY)));
        }

        private synchronized void update(float x, float y) {
            sendMessage(obtainMessage(1, new TiltValue(x, y, this.mTiltRangeX, this.mTiltRangeY, this.mWallpaperRangeX, this.mWallpaperRangeY)));
        }

        private synchronized void setWindowToken(IBinder token) {
            WallpaperScroller.this.mThread.setWindowToken(token);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (msg.obj instanceof TiltValue) {
                        TiltValue info = msg.obj;
                        if (WallpaperScroller.this.mThread != null) {
                            WallpaperScroller.this.mThread.update(info);
                            return;
                        }
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    private static class TiltValue {
        float mTiltRangeX;
        float mTiltRangeY;
        float mWallpaperRangeX;
        float mWallpaperRangeY;
        float mX;
        float mY;

        TiltValue(float x, float y, float t_x_range, float t_y_range, float x_range, float y_range) {
            this.mX = x;
            this.mY = y;
            this.mTiltRangeX = t_x_range;
            this.mTiltRangeY = t_y_range;
            this.mWallpaperRangeX = x_range;
            this.mWallpaperRangeY = y_range;
        }
    }

    private static class WallpaperThread extends Thread {
        private Context mContext;
        boolean mNeedUpdate = false;
        float mOffsetX = 0.0f;
        float mOffsetY = 0.0f;
        boolean mStopped = false;
        float mTiltRangeX = 0.0f;
        float mTiltRangeY = 0.0f;
        float mTiltX = 0.0f;
        float mTiltY = 0.0f;
        IBinder mToken = null;
        private WallpaperManager mWallpaperManager;
        float mWallpaperRangeX = 0.5f;
        float mWallpaperRangeY = 0.5f;
        float mX = 0.0f;
        float mY = 0.0f;

        WallpaperThread(Context ct, WallpaperManager wm) {
            super("updateWallpaper");
            this.mContext = ct;
            this.mWallpaperManager = wm;
        }

        private synchronized void shutdown() {
            this.mStopped = true;
            notifyAll();
        }

        private synchronized void update(TiltValue info) {
            this.mX = info.mX;
            this.mY = info.mY;
            this.mWallpaperRangeX = info.mWallpaperRangeX;
            this.mWallpaperRangeY = info.mWallpaperRangeY;
            this.mTiltRangeX = info.mTiltRangeX;
            this.mTiltRangeY = info.mTiltRangeY;
            notifyAll();
        }

        private synchronized void setWindowToken(IBinder token) {
            this.mToken = token;
        }

        private float computeOffset(float value, float start1, float stop1, float start2, float stop2) {
            return ((stop2 - start2) * ((value - start1) / (stop1 - start1))) + start2;
        }

        private void updateOffset() {
            float diffX = this.mX - this.mTiltX;
            float diffY = this.mY - this.mTiltY;
            if (Math.abs(diffX) > 1.0f) {
                this.mTiltX += diffX * WallpaperScroller.EASING_VALUE;
                this.mOffsetX = computeOffset(this.mTiltX, -this.mTiltRangeX, this.mTiltRangeX, -this.mWallpaperRangeX, this.mWallpaperRangeX);
                this.mNeedUpdate = true;
            }
            if (Math.abs(diffY) > 1.0f) {
                this.mTiltY += diffY * WallpaperScroller.EASING_VALUE;
                this.mOffsetY = computeOffset(this.mTiltY, -this.mTiltRangeY, this.mTiltRangeY, -this.mWallpaperRangeY, this.mWallpaperRangeY);
                this.mNeedUpdate = true;
            }
        }

        private void setWallpaperOffset() {
            try {
                if (WallpaperScroller.FEATURE_IS_WALLPAPER_USE_FIXED_ORIENTATION) {
                    this.mWallpaperManager.setWallpaperOffsets(this.mToken, 0.5f - this.mOffsetX, 0.5f - this.mOffsetY);
                } else if (1 == Utilities.getOrientation()) {
                    this.mWallpaperManager.setWallpaperOffsets(this.mToken, 0.5f - this.mOffsetX, 0.5f - this.mOffsetY);
                } else {
                    this.mWallpaperManager.setWallpaperOffsets(this.mToken, 0.5f - this.mOffsetY, 0.5f - this.mOffsetX);
                }
            } catch (IllegalArgumentException e) {
                Log.d(WallpaperScroller.TAG, "setWallpaperOffsets:IllegalArgumentException ");
            }
        }

        public void run() {
            while (true) {
                synchronized (this) {
                    try {
                        if (this.mToken != null) {
                            updateOffset();
                            if (this.mNeedUpdate) {
                                this.mNeedUpdate = false;
                                setWallpaperOffset();
                                wait(20);
                            } else {
                                wait();
                            }
                        } else {
                            wait();
                        }
                        if (this.mStopped) {
                            return;
                        }
                    } catch (InterruptedException e) {
                        Log.d(WallpaperScroller.TAG, "setWallpaperOffsets:InterruptedException ");
                    }
                }
            }
        }
    }

    public WallpaperScroller(Context context) {
        this.mContext = context;
    }

    public boolean isRunning() {
        return this.mIsRunning;
    }

    public void start(boolean init) {
        Log.i(TAG, "WallpaperScroller start");
        if (this.mThread == null) {
            Log.i(TAG, "WallpaperThread start");
            this.mThread = new WallpaperThread(this.mContext, WallpaperManager.getInstance(this.mContext));
            this.mThread.start();
        }
        if (this.mHandlerThread == null) {
            Log.i(TAG, "HandlerThread start");
            this.mHandlerThread = new HandlerThread("WallpaperScrollerHandlerThread");
            this.mHandlerThread.start();
            this.mTiltUpdateHandler = new TiltUpdateHandler(this.mHandlerThread.getLooper());
            this.mTiltUpdateHandler.update(0.0f, 0.0f, 0.0f, 0.0f, 0.5f, 0.5f);
        }
        GyroForShadow.registerListener(this.mGyroListener, init);
    }

    public void pause() {
        Log.i(TAG, "WallpaperScroller pause");
        if (GyroForShadow.supportScontext(this.mContext)) {
            GyroForShadow.pauseSContext();
        } else {
            GyroForShadow.pauseSensor();
        }
        if (this.mTiltUpdateHandler != null) {
            this.mTiltUpdateHandler.update(0.0f, 0.0f);
        }
        this.mIsRunning = false;
    }

    public void resume(boolean init) {
        Log.i(TAG, "WallpaperScroller resume : init " + init);
        if (this.mTiltUpdateHandler != null) {
            this.mTiltUpdateHandler.update(0.0f, 0.0f);
        }
        if (GyroForShadow.supportScontext(this.mContext)) {
            GyroForShadow.resumeSContext();
        } else {
            GyroForShadow.resumeSensor();
        }
        this.mIsRunning = true;
    }

    public void setWindowToken(IBinder token) {
        Log.i(TAG, "WallpaperScroller set window token : " + token);
        if (this.mTiltUpdateHandler != null) {
            this.mTiltUpdateHandler.setWindowToken(token);
        }
    }

    public void shutdown() {
        Log.i(TAG, "WallpaperScroller shutdown");
        GyroForShadow.unregisterListener(this.mGyroListener);
        if (this.mTiltUpdateHandler != null) {
            this.mTiltUpdateHandler.shutdown();
            this.mTiltUpdateHandler = null;
        }
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quitSafely();
            this.mHandlerThread = null;
        }
        if (this.mThread != null) {
            this.mThread.shutdown();
            this.mThread = null;
        }
    }
}

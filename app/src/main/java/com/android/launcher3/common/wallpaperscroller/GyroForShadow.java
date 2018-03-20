package com.android.launcher3.common.wallpaperscroller;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.util.WallpaperUtils;
import com.samsung.android.hardware.context.SemContextEvent;
import com.samsung.android.hardware.context.SemContextInterruptedGyroAttribute;
import com.samsung.android.hardware.context.SemContextListener;
import com.samsung.android.hardware.context.SemContextManager;
import java.util.Iterator;
import java.util.Vector;

public final class GyroForShadow {
    private static final long DEFAULT_TIME_VALUE = 300000000;
    private static final int DISABLE_INTERRUPT_GYRO = 0;
    private static final int ENABLE_INTERRUPT_GYRO = 1;
    private static final boolean FEATURE_IS_WALLPAPER_USE_FIXED_ORIENTATION = LauncherFeature.isWallpaperUseFixedOrientation();
    private static final int INTERRUPT_GYRO_SERVICE = 48;
    private static String SCONTEXT_SERVICE = "scontext";
    public static final int SENSOR_TYPE_INTERRUPT_GYROSCOPE = 65579;
    private static final int SOURCE_TYPE_SCONTEXT = 2;
    private static final int SOURCE_TYPE_SENSOR = 1;
    private static final int SOURCE_TYPE_UNKNOWN = 0;
    private static String TAG = GyroForShadow.class.getSimpleName();
    private static final float TILT_RANGE_HALF = 5.0f;
    private static final float TILT_RANGE_MAX = 10.0f;
    private static Vector<VectorListener> mListeners = new Vector();
    private static float mMapX = 0.0f;
    private static float mMapY = 0.0f;
    private static float mMobileKeyboardScreenHeight = 0.0f;
    private static float mPrevX = 0.0f;
    private static float mPrevY = 0.0f;
    private static SemContextListener mSContextListener = new SemContextListener() {
        public void onSemContextChanged(SemContextEvent arg0) {
        }
    };
    private static float mScreenHeight = 0.0f;
    private static float mScreenWidth = 0.0f;
    private static int mSourceType = 0;
    private static long mStartTimestamp = 0;
    private static float mTiltRangeX = 0.0f;
    private static float mTiltRangeY = 0.0f;
    private static float mWallpaperRangeX = 0.5f;
    private static float mWallpaperRangeY = 0.5f;
    private static SemContextManager sSContextManager;
    private static SensorManager sSensorManager;
    private static final SensorEventListener sel = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case GyroForShadow.SENSOR_TYPE_INTERRUPT_GYROSCOPE /*65579*/:
                    if (event.timestamp >= GyroForShadow.mStartTimestamp) {
                        GyroForShadow.updateMap(event);
                        Iterator it = GyroForShadow.mListeners.iterator();
                        while (it.hasNext()) {
                            ((VectorListener) it.next()).onVectorChanged(GyroForShadow.mMapX, GyroForShadow.mMapY, GyroForShadow.mTiltRangeX, GyroForShadow.mTiltRangeY, GyroForShadow.mWallpaperRangeX, GyroForShadow.mWallpaperRangeY);
                        }
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    static abstract class VectorListener {
        public abstract void onVectorChanged(float f, float f2, float f3, float f4, float f5, float f6);

        VectorListener() {
        }
    }

    private GyroForShadow() {
    }

    public static void initialize(Context context, final Activity activity) {
        sSensorManager = (SensorManager) context.getSystemService("sensor");
        sSContextManager = (SemContextManager) context.getSystemService(SCONTEXT_SERVICE);
        checkScontext(context);
        new Thread("changeTiltXYRange") {
            public void run() {
                GyroForShadow.changeTiltXYRange(activity);
            }
        }.start();
    }

    private static void stopSensor() {
        if (sSensorManager != null) {
            sSensorManager.unregisterListener(sel);
        }
    }

    private static void startSensor() {
        new Thread() {
            public void run() {
                if (GyroForShadow.sSensorManager != null) {
                    GyroForShadow.sSensorManager.registerListener(GyroForShadow.sel, GyroForShadow.sSensorManager.getDefaultSensor(GyroForShadow.SENSOR_TYPE_INTERRUPT_GYROSCOPE), 1);
                }
            }
        }.start();
    }

    static void resumeSensor() {
        if (mListeners.size() > 0) {
            startSensor();
            mStartTimestamp = SystemClock.elapsedRealtimeNanos() + DEFAULT_TIME_VALUE;
            mPrevX = 0.0f;
            mPrevY = 0.0f;
            mMapY = 0.0f;
            mMapX = 0.0f;
        }
    }

    static void pauseSensor() {
        if (mListeners.size() > 0) {
            stopSensor();
        }
    }

    static void registerListener(VectorListener aListener, boolean init) {
        if (init) {
            mPrevY = 0.0f;
            mPrevX = 0.0f;
        }
        if (!mListeners.contains(aListener)) {
            mListeners.add(aListener);
        }
        startSensor();
    }

    static void unregisterListener(VectorListener aListener) {
        mListeners.remove(aListener);
        if (mListeners.size() == 0) {
            stopSensor();
        }
    }

    static void resumeSContext() {
        if (sSContextManager != null) {
            sSContextManager.registerListener(mSContextListener, INTERRUPT_GYRO_SERVICE, new SemContextInterruptedGyroAttribute(1));
            mStartTimestamp = SystemClock.elapsedRealtimeNanos() + DEFAULT_TIME_VALUE;
            mPrevX = 0.0f;
            mPrevY = 0.0f;
            mMapY = 0.0f;
            mMapX = 0.0f;
        }
    }

    static void pauseSContext() {
        if (sSContextManager != null) {
            sSContextManager.registerListener(mSContextListener, INTERRUPT_GYRO_SERVICE, new SemContextInterruptedGyroAttribute(0));
        }
    }

    static boolean supportScontext(Context context) {
        if (mSourceType == 0) {
            checkScontext(context);
        }
        return mSourceType == 2;
    }

    private static void checkScontext(Context context) {
        PackageManager pm = context.getPackageManager();
        if (pm != null && (pm.hasSystemFeature("com.sec.feature.sensorhub") || pm.hasSystemFeature("com.sec.feature.scontext_lite"))) {
            if (sSContextManager == null) {
                sSContextManager = (SemContextManager) context.getSystemService(SCONTEXT_SERVICE);
            }
            if (sSContextManager.isAvailableService(INTERRUPT_GYRO_SERVICE)) {
                mSourceType = 2;
                return;
            }
        }
        mSourceType = 1;
    }

    private static void updateMap(SensorEvent event) {
        float gyroX = event.values[1];
        float gyroY = event.values[0];
        mPrevX = Math.max(-10.0f, Math.min(TILT_RANGE_MAX, mPrevX + (0.07f * gyroX)));
        mPrevY = Math.max(-10.0f, Math.min(TILT_RANGE_MAX, mPrevY + (0.07f * gyroY)));
        if (Math.abs(mPrevX) > TILT_RANGE_HALF) {
            float tempX;
            if (mPrevX > 0.0f) {
                tempX = TILT_RANGE_MAX - mPrevX;
            } else {
                tempX = -10.0f - mPrevX;
            }
            mMapX = computeOffset(tempX, TILT_RANGE_HALF, -5.0f, -mTiltRangeX, mTiltRangeX);
        } else {
            mMapX = computeOffset(mPrevX, TILT_RANGE_HALF, -5.0f, -mTiltRangeX, mTiltRangeX);
        }
        if (Math.abs(mPrevY) > TILT_RANGE_HALF) {
            float tempY;
            if (mPrevY > 0.0f) {
                tempY = TILT_RANGE_MAX - mPrevY;
            } else {
                tempY = -10.0f - mPrevY;
            }
            mMapY = computeOffset(tempY, TILT_RANGE_HALF, -5.0f, -mTiltRangeY, mTiltRangeY);
            return;
        }
        mMapY = computeOffset(mPrevY, TILT_RANGE_HALF, -5.0f, -mTiltRangeY, mTiltRangeY);
    }

    private static float computeOffset(float value, float start1, float stop1, float start2, float stop2) {
        return ((stop2 - start2) * ((value - start1) / (stop1 - start1))) + start2;
    }

    private static void changeTiltXYRange(Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        if (FEATURE_IS_WALLPAPER_USE_FIXED_ORIENTATION) {
            DeviceProfile dp = ((Launcher) activity).getDeviceProfile();
            mScreenWidth = dp.isLandscape ? (float) metrics.heightPixels : (float) metrics.widthPixels;
            mScreenHeight = dp.isLandscape ? (float) metrics.widthPixels : (float) metrics.heightPixels;
        } else {
            mScreenWidth = (float) metrics.widthPixels;
            mScreenHeight = (float) metrics.heightPixels;
        }
        if (Utilities.isMobileKeyboardMode()) {
            mMobileKeyboardScreenHeight = mScreenHeight;
        }
        mTiltRangeY = (float) (((((double) mScreenHeight) * 1.15d) - ((double) mScreenHeight)) / 3.0d);
        mTiltRangeX = (float) (((((double) mScreenWidth) * 1.15d) - ((double) mScreenWidth)) / 3.0d);
        if (mScreenWidth == 0.0f || mScreenHeight == 0.0f || mTiltRangeX == 0.0f || mTiltRangeY == 0.0f) {
            Log.e(TAG, "GyroForShadow is not initialized");
            return;
        }
        Drawable wallpaperDrawable = WallpaperUtils.getWallpaperDrawable(activity, TAG);
        if (wallpaperDrawable == null) {
            Log.e(TAG, "changeTiltXYRange(), WallpaperManager getDrawable() returned null");
            return;
        }
        int srcImageWidth = wallpaperDrawable.getIntrinsicWidth();
        int srcImageHeight = wallpaperDrawable.getIntrinsicHeight();
        mWallpaperRangeX = 0.5f;
        mWallpaperRangeY = 0.5f;
        if (Utilities.isMobileKeyboardMode()) {
            mScreenHeight = 2560.0f;
        }
        if (srcImageWidth > ((int) (mScreenWidth + (mTiltRangeX * 2.0f)))) {
            mWallpaperRangeX = (0.5f * mTiltRangeX) / ((((float) srcImageWidth) - mScreenWidth) / 2.0f);
        }
        if (srcImageHeight > ((int) (mScreenHeight + (mTiltRangeY * 2.0f)))) {
            mWallpaperRangeY = (0.5f * mTiltRangeY) / ((((float) srcImageHeight) - mScreenHeight) / 2.0f);
        }
        if (Utilities.isMobileKeyboardMode()) {
            mWallpaperRangeY *= mMobileKeyboardScreenHeight / mScreenHeight;
        }
        WallpaperUtils.releaseWallpaperDrawable(TAG);
    }
}

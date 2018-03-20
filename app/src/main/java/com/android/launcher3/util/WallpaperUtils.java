package com.android.launcher3.util;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.WindowManager;
import java.util.HashSet;

public final class WallpaperUtils {
    private static final String TAG = "Launcher.WallpaperUtils";
    public static final String WALLPAPER_HEIGHT_KEY = "wallpaper.height";
    public static final float WALLPAPER_SCREENS_SPAN = 2.0f;
    public static final String WALLPAPER_WIDTH_KEY = "wallpaper.width";
    private static WallpaperManager mWallpaperManager;
    private static HashSet<String> refClassName = new HashSet();
    private static Point sDefaultWallpaperSize = null;
    private static Drawable wpDrawable;

    public static void suggestWallpaperDimension(Resources res, SharedPreferences sharedPrefs, WindowManager windowManager, WallpaperManager wallpaperManager, boolean fallBackToDefaults) {
        Point defaultWallpaperSize = getDefaultWallpaperSize(res, windowManager);
        int savedWidth = sharedPrefs.getInt(WALLPAPER_WIDTH_KEY, -1);
        int savedHeight = sharedPrefs.getInt(WALLPAPER_HEIGHT_KEY, -1);
        if (savedWidth == -1 || savedHeight == -1) {
            if (fallBackToDefaults) {
                savedWidth = defaultWallpaperSize.x;
                savedHeight = defaultWallpaperSize.y;
            } else {
                return;
            }
        }
        if (savedWidth != wallpaperManager.getDesiredMinimumWidth() || savedHeight != wallpaperManager.getDesiredMinimumHeight()) {
            wallpaperManager.suggestDesiredDimensions(savedWidth, savedHeight);
        }
    }

    public static float wallpaperTravelToScreenWidthRatio(int width, int height) {
        return (0.30769226f * (((float) width) / ((float) height))) + 1.0076923f;
    }

    private static synchronized Point getDefaultWallpaperSize(Resources res, WindowManager windowManager) {
        Point point;
        synchronized (WallpaperUtils.class) {
            if (sDefaultWallpaperSize == null) {
                int defaultWidth;
                int defaultHeight;
                Point minDims = new Point();
                Point maxDims = new Point();
                windowManager.getDefaultDisplay().getCurrentSizeRange(minDims, maxDims);
                int maxDim = Math.max(maxDims.x, maxDims.y);
                int minDim = Math.max(minDims.x, minDims.y);
                Point realSize = new Point();
                windowManager.getDefaultDisplay().getRealSize(realSize);
                maxDim = Math.max(realSize.x, realSize.y);
                minDim = Math.min(realSize.x, realSize.y);
                if (res.getConfiguration().smallestScreenWidthDp >= 720) {
                    defaultWidth = (int) (((float) maxDim) * wallpaperTravelToScreenWidthRatio(maxDim, minDim));
                    defaultHeight = maxDim;
                } else {
                    defaultWidth = Math.max((int) (((float) minDim) * 2.0f), maxDim);
                    defaultHeight = maxDim;
                }
                sDefaultWallpaperSize = new Point(defaultWidth, defaultHeight);
            }
            point = sDefaultWallpaperSize;
        }
        return point;
    }

    public static synchronized Drawable getWallpaperDrawable(Context context, String className) {
        Drawable drawable;
        synchronized (WallpaperUtils.class) {
            Log.d(TAG, "getWallpaperDrawable is called by " + className);
            if (mWallpaperManager == null) {
                mWallpaperManager = WallpaperManager.getInstance(context);
            }
            wpDrawable = mWallpaperManager.getDrawable();
            refClassName.add(className);
            drawable = wpDrawable;
        }
        return drawable;
    }

    public static synchronized boolean releaseWallpaperDrawable(String className) {
        boolean z = true;
        synchronized (WallpaperUtils.class) {
            Log.d(TAG, "releaseWallpaperDrawable is called by " + className + ", refLength=" + refClassName.size());
            if (refClassName.contains(className)) {
                refClassName.remove(className);
            }
            if (refClassName.size() < 1) {
                refClassName.clear();
                Bitmap bitmap = wpDrawable.getBitmap();
                if (bitmap != null) {
                    Log.e(TAG, "check bitmap.recycle() on releaseWallpaperDrawable");
                    bitmap.recycle();
                    mWallpaperManager.forgetLoadedWallpaper();
                }
            } else {
                z = false;
            }
        }
        return z;
    }
}

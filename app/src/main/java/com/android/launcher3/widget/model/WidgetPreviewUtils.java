package com.android.launcher3.widget.model;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.common.compat.AppWidgetManagerCompat;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.theme.OpenThemeManager;
import com.android.launcher3.util.BitmapUtils;
import com.sec.android.app.launcher.R;

public class WidgetPreviewUtils {
    private static final String TAG = "WidgetPreviewUtils";
    private static final float WIDGET_PREVIEW_ICON_PADDING_PERCENTAGE = 0.25f;
    private final Context mContext;
    private final IconCache mIconCache;
    private final AppWidgetManagerCompat mManager;

    private static class SingletonHolder {
        private static final WidgetPreviewUtils sWidgetPreviewUtils = new WidgetPreviewUtils();

        private SingletonHolder() {
        }
    }

    public static WidgetPreviewUtils getInstance() {
        return SingletonHolder.sWidgetPreviewUtils;
    }

    private WidgetPreviewUtils() {
        LauncherAppState apps = LauncherAppState.getInstance();
        this.mContext = apps.getContext();
        this.mIconCache = apps.getIconCache();
        this.mManager = AppWidgetManagerCompat.getInstance(this.mContext);
    }

    public Bitmap generatePreview(Launcher launcher, Object info, int previewWidth, int previewHeight) {
        if (info instanceof LauncherAppWidgetProviderInfo) {
            return generateWidgetPreview(launcher, (LauncherAppWidgetProviderInfo) info, previewWidth, null);
        }
        return generateShortcutPreview(launcher, (ResolveInfo) info);
    }

    public Bitmap generateWidgetPreview(Launcher launcher, LauncherAppWidgetProviderInfo info, int maxPreviewWidth, int[] preScaledWidthOut) {
        int previewWidth;
        int previewHeight;
        if (maxPreviewWidth < 0) {
            maxPreviewWidth = Integer.MAX_VALUE;
        }
        Drawable drawable = null;
        if (info.previewImage != 0) {
            drawable = this.mManager.loadPreview(info);
            if (drawable != null) {
                drawable = getMutateDrawable(drawable);
            } else {
                Log.w(TAG, "Can't load widget preview drawable 0x" + Integer.toHexString(info.previewImage) + " for provider: " + info.provider);
            }
        }
        boolean widgetPreviewExists = drawable != null;
        int spanX = info.getSpanX();
        int spanY = info.getSpanY();
        if (widgetPreviewExists) {
            previewWidth = drawable.getIntrinsicWidth();
            previewHeight = drawable.getIntrinsicHeight();
        } else {
            DeviceProfile deviceProfile = launcher.getDeviceProfile();
            previewWidth = deviceProfile.homeGrid.getCellWidth() * spanX;
            previewHeight = deviceProfile.homeGrid.getCellWidth() * spanY;
        }
        float scale = 1.0f;
        if (preScaledWidthOut != null) {
            preScaledWidthOut[0] = previewWidth;
        }
        if (previewWidth > maxPreviewWidth) {
            scale = ((float) maxPreviewWidth) / ((float) previewWidth);
        }
        if (scale != 1.0f) {
            previewWidth = (int) (((float) previewWidth) * scale);
            previewHeight = (int) (((float) previewHeight) * scale);
        }
        Canvas c = new Canvas();
        Bitmap preview = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        c.setBitmap(preview);
        int x = (preview.getWidth() - previewWidth) / 2;
        if (widgetPreviewExists) {
            drawable.setBounds(x, 0, x + previewWidth, previewHeight);
            drawable.draw(c);
        } else {
            new Paint().setFilterBitmap(true);
            int appIconSize = launcher.getDeviceProfile().defaultIconSize;
            BitmapUtils.renderDrawableToBitmap(launcher.getResources().getDrawable(R.drawable.default_widget_preview_holo, null), preview, 0, 0, previewWidth, previewHeight, 1.0f);
            float iconScale = Math.min(((float) Math.min(previewWidth, previewHeight)) / ((float) ((((int) (((float) appIconSize) * WIDGET_PREVIEW_ICON_PADDING_PERCENTAGE)) * 2) + appIconSize)), scale);
            try {
                Drawable icon = getMutateDrawable(this.mManager.loadIcon(info, this.mIconCache));
                if (icon != null) {
                    int hoffset = ((int) ((((float) previewWidth) - (((float) appIconSize) * iconScale)) / 2.0f)) + x;
                    int yoffset = (int) ((((float) previewHeight) - (((float) appIconSize) * iconScale)) / 2.0f);
                    icon.setBounds(hoffset, yoffset, ((int) (((float) appIconSize) * iconScale)) + hoffset, ((int) (((float) appIconSize) * iconScale)) + yoffset);
                    icon.draw(c);
                }
            } catch (NotFoundException e) {
                Log.d(TAG, "Resources.NotFoundExceptio:" + e.toString());
            }
            c.setBitmap(null);
        }
        return this.mManager.getBadgeBitmap(info, preview, Math.min(preview.getHeight(), previewHeight));
    }

    private Bitmap generateShortcutPreview(Launcher launcher, ResolveInfo info) {
        Drawable icon = getMutateDrawable(this.mIconCache.getFullResIcon(info.activityInfo));
        icon.setFilterBitmap(true);
        int iconSize = launcher.getDeviceProfile().defaultIconSize;
        return OpenThemeManager.getInstance().getIconWithTrayIfNeeded(BitmapUtils.createIconBitmap(icon, this.mContext, iconSize, iconSize), iconSize, false);
    }

    private Drawable getMutateDrawable(Drawable drawable) {
        return drawable.mutate();
    }
}

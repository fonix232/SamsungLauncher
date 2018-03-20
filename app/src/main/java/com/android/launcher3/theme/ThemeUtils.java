package com.android.launcher3.theme;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import com.android.launcher3.theme.OpenThemeManager.FolderStyle;
import com.android.launcher3.theme.OpenThemeManager.ThemeItems;
import com.android.launcher3.util.BitmapUtils;

public class ThemeUtils {
    private static final int DEFAULT_SCALE = 70;
    public static final boolean FOLLOWING_THEME = true;
    public static final boolean NOT_FOLLOWING_THEME = false;
    private static Paint mPaint = new Paint();

    static {
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        mPaint.setDither(false);
    }

    public static Bitmap resizeBitmap(Bitmap original, int resultSize) {
        return Bitmap.createScaledBitmap(original, resultSize, resultSize, true);
    }

    public static Drawable getNinepatchWithColor(Bitmap bitmap, int color, Rect padding) {
        byte[] chunk = bitmap.getNinePatchChunk();
        if (chunk == null) {
            return null;
        }
        Drawable ninePatchDrawable = new NinePatchDrawable(bitmap, chunk, padding, null);
        ninePatchDrawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        ninePatchDrawable.setColorFilter(color, Mode.SRC_IN);
        return ninePatchDrawable;
    }

    public static Bitmap integrateIconAndTray(Bitmap bitmap, Bitmap IconTray, int iconWidth, int iconHeight) {
        if (bitmap == null) {
            return null;
        }
        int int_scale = OpenThemeManager.getInstance().getInteger(ThemeItems.ICON_SCALE.value());
        if (int_scale == -1) {
            int_scale = DEFAULT_SCALE;
        }
        float iconScale = ((float) int_scale) * 0.01f;
        Paint mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        mPaint.setDither(false);
        int defaultIconSize = BitmapUtils.getIconBitmapSize();
        if (IconTray == null) {
            return bitmap;
        }
        float scale;
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        if (bitmapWidth != iconWidth) {
            scale = (((float) iconWidth) * iconScale) / ((float) defaultIconSize);
            bitmapWidth = defaultIconSize;
            bitmapHeight = defaultIconSize;
        } else {
            scale = iconScale;
        }
        Bitmap b = Bitmap.createBitmap(iconWidth, iconHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        canvas.drawBitmap(IconTray, 0.0f, 0.0f, mPaint);
        canvas.save();
        canvas.translate(((float) iconWidth) / 2.0f, ((float) iconHeight) / 2.0f);
        canvas.scale(scale, scale);
        canvas.drawBitmap(bitmap, ((float) (-bitmapWidth)) / 2.0f, ((float) (-bitmapHeight)) / 2.0f, mPaint);
        canvas.restore();
        return b;
    }

    @SuppressLint({"ResourceAsColor"})
    public static Bitmap roundBitmap(int color, int iconWidth, int iconHeight, int radius, Drawable strokeDrawable) {
        Bitmap rounder = Bitmap.createBitmap(iconWidth, iconHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(rounder);
        switch (color) {
            case 0:
                color = -16777216;
                break;
            case 1:
                color = FolderStyle.FOLDER_COLOR2;
                break;
            case 2:
                color = FolderStyle.FOLDER_COLOR3;
                break;
            case 3:
                color = FolderStyle.FOLDER_COLOR4;
                break;
            case 4:
                color = FolderStyle.FOLDER_COLOR5;
                break;
        }
        mPaint.setColor(color);
        canvas.drawCircle(((float) iconWidth) / 2.0f, ((float) iconHeight) / 2.0f, (float) radius, mPaint);
        if (strokeDrawable != null) {
            canvas.drawBitmap(BitmapUtils.getBitmap(strokeDrawable, iconWidth, iconHeight), 0.0f, 0.0f, null);
        }
        canvas.setBitmap(null);
        return rounder;
    }

    public static Bitmap roundBitmap(Drawable drawable, int iconWidth, int iconHeight, int radius, Drawable strokeDrawable) {
        Bitmap rounder = Bitmap.createBitmap(iconWidth, iconHeight, Config.ARGB_8888);
        Bitmap source = BitmapUtils.getBitmap(drawable, iconWidth, iconHeight);
        Canvas canvas = new Canvas(rounder);
        canvas.drawBitmap(source, 0.0f, 0.0f, null);
        if (strokeDrawable != null) {
            canvas.drawBitmap(BitmapUtils.getBitmap(strokeDrawable, iconWidth, iconHeight), 0.0f, 0.0f, null);
        }
        canvas.setBitmap(null);
        return rounder;
    }
}

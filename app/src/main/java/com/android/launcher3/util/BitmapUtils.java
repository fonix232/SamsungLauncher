package com.android.launcher3.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.support.v4.internal.view.SupportMenu;
import android.util.Log;
import android.util.SparseArray;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.drawable.FastBitmapDrawable;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;

public class BitmapUtils {
    public static final String TAG = "Launcher.BitmapUtils";
    private static CanvasPool mCanvasPool = new CanvasPool();
    private static final Canvas sCanvas = new Canvas();
    private static int sColorIndex = 0;
    private static int[] sColors = new int[]{SupportMenu.CATEGORY_MASK, -16711936, -16776961};
    private static final Rect sOldBounds = new Rect();

    private static class CanvasPool {
        private final ArrayList<Canvas> mCanvasPool;

        private CanvasPool() {
            this.mCanvasPool = new ArrayList();
        }

        public synchronized Canvas get() {
            Canvas canvas;
            if (this.mCanvasPool.size() > 0) {
                canvas = (Canvas) this.mCanvasPool.remove(0);
            } else {
                Canvas canvas2 = new Canvas();
                canvas2.setDrawFilter(new PaintFlagsDrawFilter(4, 3));
                canvas = canvas2;
            }
            return canvas;
        }

        public synchronized void recycle(Canvas canvas) {
            this.mCanvasPool.add(canvas);
        }
    }

    private static class FixedSizeBitmapDrawable extends BitmapDrawable {
        public FixedSizeBitmapDrawable(Bitmap bitmap) {
            super(null, bitmap);
        }

        public int getIntrinsicHeight() {
            return getBitmap().getWidth();
        }

        public int getIntrinsicWidth() {
            return getBitmap().getWidth();
        }
    }

    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(4, 2));
    }

    public static Bitmap createIconBitmap(Cursor c, int iconIndex, Context context) {
        byte[] data = c.getBlob(iconIndex);
        try {
            return createIconBitmap(BitmapFactory.decodeByteArray(data, 0, data.length), context);
        } catch (Exception e) {
            return null;
        }
    }

    public static FastBitmapDrawable createIconDrawable(Bitmap icon, int iconSize) {
        FastBitmapDrawable d = new FastBitmapDrawable(icon, iconSize, iconSize);
        d.setFilterBitmap(true);
        return d;
    }

    public static Bitmap createIconBitmap(String packageName, String resourceName, Context context) {
        Bitmap bitmap = null;
        try {
            Resources resources = context.getPackageManager().getResourcesForApplication(packageName);
            if (resources != null) {
                bitmap = createIconBitmap(resources.getDrawableForDensity(resources.getIdentifier(resourceName, null, null), LauncherAppState.getInstance().getIconCache().getIconDpi()), context);
            }
        } catch (Exception e) {
        }
        return bitmap;
    }

    public static int getIconBitmapSize() {
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile profile = app.getDeviceProfile();
        if (profile == null) {
            return app.getContext().getResources().getDimensionPixelSize(R.dimen.app_icon_size);
        }
        return profile.defaultIconSize;
    }

    public static Bitmap createIconBitmap(Bitmap icon, Context context) {
        int iconBitmapSize = getIconBitmapSize();
        return (iconBitmapSize == icon.getWidth() && iconBitmapSize == icon.getHeight()) ? icon : createIconBitmap(new BitmapDrawable(context.getResources(), icon), context);
    }

    public static Bitmap createIconBitmap(Drawable icon, Context context) {
        Bitmap bitmap;
        synchronized (sCanvas) {
            int iconBitmapSize = getIconBitmapSize();
            int width = iconBitmapSize;
            int height = iconBitmapSize;
            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            } else if (icon instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                if (bitmapDrawable.getBitmap().getDensity() == 0) {
                    bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
                }
            }
            int sourceWidth = icon.getIntrinsicWidth();
            int sourceHeight = icon.getIntrinsicHeight();
            if (sourceWidth > 0 && sourceHeight > 0) {
                float ratio = ((float) sourceWidth) / ((float) sourceHeight);
                if (sourceWidth > sourceHeight) {
                    height = (int) (((float) width) / ratio);
                } else if (sourceHeight > sourceWidth) {
                    width = (int) (((float) height) * ratio);
                }
            }
            int textureWidth = iconBitmapSize;
            int textureHeight = iconBitmapSize;
            bitmap = Bitmap.createBitmap(textureWidth, textureHeight, Config.ARGB_8888);
            Canvas canvas = sCanvas;
            canvas.setBitmap(bitmap);
            int left = (textureWidth - width) / 2;
            int top = (textureHeight - height) / 2;
            sOldBounds.set(icon.getBounds());
            icon.setBounds(left, top, left + width, top + height);
            icon.draw(canvas);
            icon.setBounds(sOldBounds);
            canvas.setBitmap(null);
        }
        return bitmap;
    }

    public static Bitmap createIconBitmap(Drawable icon, Context context, int width, int height) {
        Rect oldBounds = new Rect();
        int shortcutBounds = context.getResources().getDimensionPixelSize(R.dimen.widget_drag_shortcut_bound);
        Drawable copyIcon = null;
        if (icon.getConstantState() != null) {
            copyIcon = icon.getConstantState().newDrawable();
        }
        if (copyIcon == null) {
            copyIcon = icon;
        }
        if (copyIcon instanceof PaintDrawable) {
            PaintDrawable painter = (PaintDrawable) copyIcon;
            painter.setIntrinsicWidth(width);
            painter.setIntrinsicHeight(height);
        } else if (copyIcon instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) copyIcon;
            if (bitmapDrawable.getBitmap().getDensity() == 0) {
                bitmapDrawable.setTargetDensity(context.getResources().getDisplayMetrics());
            }
        }
        copyIcon.copyBounds(oldBounds);
        float scale = 1.0f;
        int sourceWidth = copyIcon.getIntrinsicWidth();
        int sourceHeight = copyIcon.getIntrinsicHeight();
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            sourceWidth = width;
            sourceHeight = height;
            copyIcon.setBounds(0, 0, width, height);
        } else {
            copyIcon.setBounds(0, 0, sourceWidth, sourceHeight);
            scale = Math.min(((float) width) / ((float) sourceWidth), ((float) height) / ((float) sourceHeight));
            sourceWidth = (int) (((float) sourceWidth) * scale);
            sourceHeight = (int) (((float) sourceHeight) * scale);
        }
        int dx = (width - sourceWidth) + shortcutBounds;
        int dy = (height - sourceHeight) + shortcutBounds;
        Bitmap bitmap = Bitmap.createBitmap(width + shortcutBounds, height + shortcutBounds, Config.ARGB_8888);
        Canvas canvas = mCanvasPool.get();
        canvas.setMatrix(null);
        canvas.setBitmap(bitmap);
        canvas.translate(((float) dx) * 0.5f, ((float) dy) * 0.5f);
        canvas.scale(scale, scale);
        copyIcon.draw(canvas);
        copyIcon.setBounds(oldBounds);
        canvas.setBitmap(null);
        mCanvasPool.recycle(canvas);
        return bitmap;
    }

    public static Bitmap getBitmapWithColor(Context context, int resId, int color) {
        Bitmap bitmap = null;
        if (!(context == null || resId == 0)) {
            Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), resId);
            if (bmp == null) {
                Log.e(TAG, "can't decode resource : " + resId);
            } else {
                Paint paint = new Paint();
                paint.setColor(color);
                bitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                if (!TestHelper.isRoboUnitTest()) {
                    canvas.drawBitmap(bmp.extractAlpha(), 0.0f, 0.0f, paint);
                }
                bmp.recycle();
            }
        }
        return bitmap;
    }

    public static Bitmap getBitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        int srcWidth = drawable.getIntrinsicWidth();
        int srcHeight = drawable.getIntrinsicHeight();
        Bitmap bmp = Bitmap.createBitmap(srcWidth, srcHeight, Config.ARGB_8888);
        renderDrawableToBitmap(drawable, bmp, 0, 0, srcWidth, srcHeight, 1.0f);
        return bmp;
    }

    public static Bitmap getBitmap(Drawable drawable, int dstWidth, int dstHeight) {
        if (drawable == null) {
            return null;
        }
        int srcWidth = drawable.getIntrinsicWidth();
        int srcHeight = drawable.getIntrinsicHeight();
        float ratio = Math.min(((float) dstWidth) / ((float) srcWidth), ((float) dstHeight) / ((float) srcHeight));
        Bitmap bmp = Bitmap.createBitmap(dstWidth, dstHeight, Config.ARGB_8888);
        renderDrawableToBitmap(drawable, bmp, 0, 0, srcWidth, srcHeight, ratio);
        return bmp;
    }

    public static Bitmap getOverlaidIcon(Bitmap source, Bitmap overlay) {
        Bitmap result = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig());
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        canvas.save();
        canvas.drawBitmap(source, 0.0f, 0.0f, paint);
        canvas.restore();
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_OVER));
        canvas.drawBitmap(overlay, 0.0f, 0.0f, paint);
        return result;
    }

    public static void renderDrawableToBitmap(Drawable d, Bitmap bitmap, int x, int y, int w, int h, float scale) {
        renderDrawableToBitmap(d, bitmap, x, y, w, h, scale, -1);
    }

    public static void renderDrawableToBitmap(Drawable d, Bitmap bitmap, int x, int y, int w, int h, float scale, int multiplyColor) {
        if (bitmap != null) {
            Canvas c = new Canvas(bitmap);
            c.scale(scale, scale);
            Rect oldBounds = d.copyBounds();
            d.setBounds(x, y, x + w, y + h);
            d.draw(c);
            d.setBounds(oldBounds);
            if (multiplyColor != -1) {
                c.drawColor(multiplyColor, Mode.MULTIPLY);
            }
            c.setBitmap(null);
        }
    }

    public static int findDominantColorByHue(Bitmap bitmap, int samples) {
        int y;
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        int sampleStride = (int) Math.sqrt((((double) height) * ((double) width)) / ((double) samples));
        if (sampleStride < 1) {
            sampleStride = 1;
        }
        float[] hsv = new float[3];
        float[] hueScoreHistogram = new float[360];
        float highScore = -1.0f;
        int bestHue = -1;
        for (y = 0; y < height; y += sampleStride) {
            int x;
            for (x = 0; x < width; x += sampleStride) {
                int argb = bitmap.getPixel(x, y);
                if (((argb >> 24) & 255) >= 128) {
                    Color.colorToHSV(argb | -16777216, hsv);
                    int hue = (int) hsv[0];
                    if (hue >= 0 && hue < hueScoreHistogram.length) {
                        hueScoreHistogram[hue] = hueScoreHistogram[hue] + (hsv[1] * hsv[2]);
                        if (hueScoreHistogram[hue] > highScore) {
                            highScore = hueScoreHistogram[hue];
                            bestHue = hue;
                        }
                    }
                }
            }
        }
        SparseArray<Float> rgbScores = new SparseArray();
        int bestColor = -16777216;
        highScore = -1.0f;
        for (y = 0; y < height; y += sampleStride) {
            for (x = 0; x < width; x += sampleStride) {
                int rgb = bitmap.getPixel(x, y) | -16777216;
                Color.colorToHSV(rgb, hsv);
                if (((int) hsv[0]) == bestHue) {
                    float newTotal;
                    float s = hsv[1];
                    float v = hsv[2];
                    int bucket = ((int) (100.0f * s)) + ((int) (10000.0f * v));
                    float score = s * v;
                    Float oldTotal = (Float) rgbScores.get(bucket);
                    if (oldTotal == null) {
                        newTotal = score;
                    } else {
                        newTotal = oldTotal.floatValue() + score;
                    }
                    rgbScores.put(bucket, Float.valueOf(newTotal));
                    if (newTotal > highScore) {
                        highScore = newTotal;
                        bestColor = rgb;
                    }
                }
            }
        }
        return bestColor;
    }

    public static Drawable getResizedDrawable(Context context, Drawable d, int width, int height) {
        Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
        bitmap.setDensity(context.getResources().getDisplayMetrics().densityDpi);
        return new BitmapDrawable(context.getResources(), Bitmap.createScaledBitmap(bitmap, width, height, true));
    }

    @TargetApi(21)
    public static Bitmap badgeWithBitmap(Bitmap srcTgt, Bitmap badge, Context context) {
        int badgeSize = context.getResources().getDimensionPixelSize(R.dimen.profile_badge_size);
        synchronized (sCanvas) {
            sCanvas.setBitmap(srcTgt);
            sCanvas.drawBitmap(badge, new Rect(0, 0, badge.getWidth(), badge.getHeight()), new Rect(srcTgt.getWidth() - badgeSize, srcTgt.getHeight() - badgeSize, srcTgt.getWidth(), srcTgt.getHeight()), new Paint(2));
            sCanvas.setBitmap(null);
        }
        return srcTgt;
    }

    public static Bitmap badgeIconForUser(Bitmap icon, UserHandleCompat user, Context context) {
        if (!Utilities.ATLEAST_LOLLIPOP || user == null || UserHandleCompat.myUserHandle().equals(user)) {
            return icon;
        }
        Drawable badged = context.getPackageManager().getUserBadgedIcon(new FixedSizeBitmapDrawable(icon), user.getUser());
        if (badged instanceof BitmapDrawable) {
            return ((BitmapDrawable) badged).getBitmap();
        }
        return createIconBitmap(badged, context);
    }
}

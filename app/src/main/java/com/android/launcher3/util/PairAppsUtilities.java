package com.android.launcher3.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.home.LauncherPairAppsInfo.PairAppInfo;
import com.sec.android.app.launcher.R;

public final class PairAppsUtilities {
    private static final float ICON_REDUCTION_RATIO_PERCENT_66 = 0.34f;
    private static final String TAG = "PairAppsUtilities";

    public static Bitmap buildIcon(Context context, PairAppInfo firstApp, PairAppInfo secondApp) {
        return buildIcon(context, firstApp.getCN(), secondApp.getCN(), firstApp.getUserCompat(), secondApp.getUserCompat());
    }

    public static Bitmap buildIcon(Context context, ComponentName firstCN, ComponentName secondCN, UserHandleCompat firstHandle, UserHandleCompat secondHandle) {
        PackageManager packageManager = context.getPackageManager();
        Drawable firstIcon = null;
        Drawable secondIcon = null;
        try {
            firstIcon = packageManager.getActivityIcon(firstCN);
            secondIcon = packageManager.getActivityIcon(secondCN);
            if (ShortcutTray.isIconTrayEnabled()) {
                firstIcon = getIcon(context, firstIcon, firstCN);
                secondIcon = getIcon(context, secondIcon, secondCN);
            }
            if (firstHandle != null) {
                firstIcon = packageManager.getUserBadgedIcon(firstIcon, firstHandle.getUser());
            }
            if (secondHandle != null) {
                secondIcon = packageManager.getUserBadgedIcon(secondIcon, secondHandle.getUser());
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
        if (firstIcon == null || secondIcon == null) {
            return null;
        }
        return makeIcon(context, firstIcon, secondIcon, ICON_REDUCTION_RATIO_PERCENT_66, ICON_REDUCTION_RATIO_PERCENT_66);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static android.graphics.drawable.Drawable getIcon(android.content.Context r6, android.graphics.drawable.Drawable r7, android.content.ComponentName r8) {
        /*
        r3 = com.android.launcher3.common.view.IconView.isKnoxShortcut(r8);
        if (r3 != 0) goto L_0x0010;
    L_0x0006:
        r3 = com.android.launcher3.theme.OpenThemeManager.getInstance();
        r3 = r3.isDefaultTheme();
        if (r3 != 0) goto L_0x0012;
    L_0x0010:
        r2 = r7;
    L_0x0011:
        return r2;
    L_0x0012:
        r1 = r6.getPackageManager();
        if (r1 != 0) goto L_0x0021;
    L_0x0018:
        r3 = "PairAppsUtilities";
        r4 = "unable to retrieve PackageManager";
        android.util.Log.e(r3, r4);
        r2 = r7;
        goto L_0x0011;
    L_0x0021:
        if (r8 == 0) goto L_0x003b;
    L_0x0023:
        r3 = r8.getPackageName();	 Catch:{ NoSuchMethodError -> 0x0042 }
        r4 = r8.getClassName();	 Catch:{ NoSuchMethodError -> 0x0042 }
        r3 = r1.semCheckComponentMetadataForIconTray(r3, r4);	 Catch:{ NoSuchMethodError -> 0x0042 }
        if (r3 != 0) goto L_0x003b;
    L_0x0031:
        r3 = r8.getPackageName();	 Catch:{ NoSuchMethodError -> 0x0042 }
        r3 = r1.semShouldPackIntoIconTray(r3);	 Catch:{ NoSuchMethodError -> 0x0042 }
        if (r3 == 0) goto L_0x0040;
    L_0x003b:
        r3 = 1;
        r7 = r1.semGetDrawableForIconTray(r7, r3);	 Catch:{ NoSuchMethodError -> 0x0042 }
    L_0x0040:
        r2 = r7;
        goto L_0x0011;
    L_0x0042:
        r0 = move-exception;
        r3 = "PairAppsUtilities";
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "Method not found : ";
        r4 = r4.append(r5);
        r5 = r0.toString();
        r4 = r4.append(r5);
        r4 = r4.toString();
        android.util.Log.e(r3, r4);
        goto L_0x0040;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.util.PairAppsUtilities.getIcon(android.content.Context, android.graphics.drawable.Drawable, android.content.ComponentName):android.graphics.drawable.Drawable");
    }

    private static Bitmap makeIcon(Context context, Drawable firstIcon, Drawable secondIcon, float firstIconRatio, float secondIconRatio) {
        Log.d(TAG, "Make Pair Apps icon");
        int width = context.getResources().getDimensionPixelOffset(R.dimen.appsPicker_icon_size);
        int height = context.getResources().getDimensionPixelOffset(R.dimen.appsPicker_icon_size);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable bg = context.getResources().getDrawable(R.drawable.apps_pair_app_bg, null);
        bg.setBounds(new Rect(0, 0, width, height));
        bg.draw(canvas);
        int right = (int) (((double) width) * 0.6d);
        int bottom = (int) (((double) height) * 0.6d);
        int left = (int) (((double) width) * 0.4d);
        int top = (int) (((double) height) * 0.4d);
        int width_reduction = (int) ((((float) right) * firstIconRatio) / 2.0f);
        int height_reduction = (int) ((((float) bottom) * firstIconRatio) / 2.0f);
        firstIcon.setBounds(width_reduction, height_reduction, right - width_reduction, bottom - height_reduction);
        width_reduction = (int) ((((float) right) * secondIconRatio) / 2.0f);
        height_reduction = (int) ((((float) bottom) * secondIconRatio) / 2.0f);
        secondIcon.setBounds(left + width_reduction, top + height_reduction, width - width_reduction, height - height_reduction);
        secondIcon.draw(canvas);
        firstIcon.draw(canvas);
        return createShadowBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), context.getResources().getColor(R.color.pair_app_shadow_color), (int) context.getResources().getDimension(R.dimen.pair_app_icon_shadow_size), 0.0f, (float) ((int) context.getResources().getDimension(R.dimen.pair_app_icon_shadow_dy)));
    }

    private static Bitmap createShadowBitmap(Bitmap bm, int dstHeight, int dstWidth, int color, int size, float dx, float dy) {
        Bitmap mask = Bitmap.createBitmap(dstWidth, dstHeight, Config.ALPHA_8);
        Matrix scaleToFit = new Matrix();
        scaleToFit.setRectToRect(new RectF(0.0f, 0.0f, (float) bm.getWidth(), (float) bm.getHeight()), new RectF(0.0f, 0.0f, ((float) dstWidth) - dx, ((float) dstHeight) - dy), ScaleToFit.CENTER);
        Matrix dropShadow = new Matrix(scaleToFit);
        dropShadow.postTranslate(dx, dy);
        Canvas maskCanvas = new Canvas(mask);
        Paint paint = new Paint(1);
        maskCanvas.drawBitmap(bm, scaleToFit, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_OUT));
        maskCanvas.drawBitmap(bm, dropShadow, paint);
        BlurMaskFilter filter = new BlurMaskFilter((float) size, Blur.NORMAL);
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setMaskFilter(filter);
        paint.setFilterBitmap(true);
        Bitmap ret = Bitmap.createBitmap(dstWidth, dstHeight, Config.ARGB_8888);
        Canvas retCanvas = new Canvas(ret);
        retCanvas.drawBitmap(mask, 0.0f, 0.0f, paint);
        retCanvas.drawBitmap(bm, scaleToFit, null);
        mask.recycle();
        return ret;
    }

    public static String buildLabel(Context context, ComponentName firstCN, ComponentName secondCN) {
        PackageManager packageManager = context.getPackageManager();
        String firstLabel = null;
        String secondLabel = null;
        try {
            ActivityInfo activityInfo = packageManager.getActivityInfo(firstCN, 0);
            if (activityInfo != null) {
                firstLabel = activityInfo.loadLabel(packageManager).toString();
            }
            activityInfo = packageManager.getActivityInfo(secondCN, 0);
            if (activityInfo != null) {
                secondLabel = activityInfo.loadLabel(packageManager).toString();
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
        if (firstLabel == null || secondLabel == null) {
            return "";
        }
        return firstLabel + "\n" + secondLabel;
    }

    public static boolean isValidComponents(Context context, String info) {
        if (info != null) {
            String[] items = info.split(";");
            if (items.length == 2) {
                String[] firstItem = items[0].split(":");
                String[] secondItem = items[1].split(":");
                if (isValidComponent(context, firstItem) && isValidComponent(context, secondItem)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isValidComponent(Context context, String[] item) {
        if (item.length != 2) {
            return false;
        }
        ComponentName cn = ComponentName.unflattenFromString(item[0]);
        if (cn != null) {
            return Utilities.isValidComponent(context, cn);
        }
        return false;
    }
}

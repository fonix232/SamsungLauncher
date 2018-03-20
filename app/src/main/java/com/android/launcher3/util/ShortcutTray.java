package com.android.launcher3.util;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.Settings.Global;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.theme.OpenThemeManager;

public class ShortcutTray {
    private static final String TAG = "ShortcutTray";
    private static boolean sIconTrayEnabled;

    private ShortcutTray() {
    }

    public static boolean isIconTrayEnabled() {
        return sIconTrayEnabled;
    }

    public static void checkIconTrayEnabled(Context context) {
        boolean z = false;
        if (Global.getInt(context.getContentResolver(), "tap_to_icon", 0) != 0) {
            z = true;
        }
        sIconTrayEnabled = z;
    }

    public static Bitmap getIcon(Context context, Bitmap sourceIcon, ComponentName cn) {
        return getIcon(context, sourceIcon, cn, false);
    }

    public static Bitmap getIcon(Context context, Bitmap sourceIcon, ComponentName cn, boolean isAppShortcut) {
        if (!sIconTrayEnabled || IconView.isKnoxShortcut(cn) || !OpenThemeManager.getInstance().isDefaultTheme()) {
            return sourceIcon;
        }
        Bitmap returnBitmap = sourceIcon;
        Drawable d = getDrawableForIconTray(context, new BitmapDrawable(context.getResources(), sourceIcon), cn, isAppShortcut);
        if (d != null) {
            return BitmapUtils.createIconBitmap(d, context);
        }
        return returnBitmap;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static android.graphics.drawable.Drawable getDrawableForIconTray(android.content.Context r6, android.graphics.drawable.Drawable r7, android.content.ComponentName r8, boolean r9) {
        /*
        r2 = r6.getPackageManager();
        if (r2 != 0) goto L_0x000e;
    L_0x0006:
        r3 = new java.lang.IllegalStateException;
        r4 = "unable to retrieve PackageManager";
        r3.<init>(r4);
        throw r3;
    L_0x000e:
        r1 = 0;
        if (r8 == 0) goto L_0x002b;
    L_0x0011:
        if (r9 != 0) goto L_0x002b;
    L_0x0013:
        r3 = r8.getPackageName();	 Catch:{ NoSuchMethodError -> 0x0031 }
        r4 = r8.getClassName();	 Catch:{ NoSuchMethodError -> 0x0031 }
        r3 = r2.semCheckComponentMetadataForIconTray(r3, r4);	 Catch:{ NoSuchMethodError -> 0x0031 }
        if (r3 != 0) goto L_0x002b;
    L_0x0021:
        r3 = r8.getPackageName();	 Catch:{ NoSuchMethodError -> 0x0031 }
        r3 = r2.semShouldPackIntoIconTray(r3);	 Catch:{ NoSuchMethodError -> 0x0031 }
        if (r3 == 0) goto L_0x0030;
    L_0x002b:
        r3 = 1;
        r1 = r2.semGetDrawableForIconTray(r7, r3);	 Catch:{ NoSuchMethodError -> 0x0031 }
    L_0x0030:
        return r1;
    L_0x0031:
        r0 = move-exception;
        r3 = "ShortcutTray";
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "Method not found : ";
        r4 = r4.append(r5);
        r5 = r0.toString();
        r4 = r4.append(r5);
        r4 = r4.toString();
        android.util.Log.e(r3, r4);
        goto L_0x0030;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.util.ShortcutTray.getDrawableForIconTray(android.content.Context, android.graphics.drawable.Drawable, android.content.ComponentName, boolean):android.graphics.drawable.Drawable");
    }
}

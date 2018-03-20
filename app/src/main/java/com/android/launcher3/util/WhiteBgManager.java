package com.android.launcher3.util;

import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.provider.Settings.System;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.theme.OpenThemeManager;
import com.android.launcher3.theme.OpenThemeManager.ThemeItems;
import com.sec.android.app.launcher.R;

public class WhiteBgManager {
    public static final int LIGHT_TEXT_COLOR = 0;
    public static final String PREFERENCES_NEED_DARK_FONT = "need_dark_font";
    public static final String PREFERENCES_NEED_DARK_NAVIGATIONBAR = "need_dark_navigationbar";
    public static final String PREFERENCES_NEED_DARK_STATUSBAR = "need_dark_statusbar";
    static final String TAG = "WhiteBgManager";
    private static boolean sChangeForWhiteBg = false;
    private static boolean sChangeNavigationBarForWhiteBg = false;
    private static boolean sChangeStatusBarForWhiteBg = false;

    static class AttribImage {
        static int sColorFilter = 0;

        AttribImage() {
        }

        static void setup(Context context, boolean whiteBg) {
            if (whiteBg) {
                sColorFilter = ContextCompat.getColor(context, R.color.filter_color_dark);
            } else {
                sColorFilter = 0;
            }
        }
    }

    static class AttribText {
        static int sColor = 0;
        static float sDy = 0.0f;
        static float sRadius = 0.0f;
        static int sShadowColor = 0;

        AttribText() {
        }

        static void setup(Context context, boolean whiteBg, boolean isFollowingTheme) {
            if (whiteBg) {
                sRadius = (float) context.getResources().getInteger(R.integer.text_shadow_radius_dark);
                sDy = (float) context.getResources().getInteger(R.integer.text_shadow_dy_dark);
                sShadowColor = ContextCompat.getColor(context, R.color.text_shadow_color_dark);
                sColor = ContextCompat.getColor(context, R.color.text_color_dark);
                return;
            }
            sRadius = (float) context.getResources().getInteger(R.integer.text_shadow_radius);
            sDy = (float) context.getResources().getInteger(R.integer.text_shadow_dy);
            if (isFollowingTheme) {
                sShadowColor = OpenThemeManager.getInstance().getPreloadColor(ThemeItems.TEXT_SHADOW_COLOR.value());
                sColor = OpenThemeManager.getInstance().getPreloadColor(ThemeItems.HOME_TEXT_COLOR.value());
                return;
            }
            sShadowColor = ContextCompat.getColor(context, R.color.text_shadow_color);
            sColor = ContextCompat.getColor(context, R.color.text_color);
        }
    }

    public static boolean isWhiteBg() {
        return sChangeForWhiteBg;
    }

    public static boolean isWhiteStatusBar() {
        return sChangeStatusBarForWhiteBg;
    }

    public static boolean isWhiteNavigationBar() {
        return sChangeNavigationBarForWhiteBg;
    }

    public static void setup(Context context) {
        boolean z = false;
        if (System.getInt(context.getContentResolver(), PREFERENCES_NEED_DARK_FONT, 0) != 0) {
            z = true;
        }
        sChangeForWhiteBg = z;
        Log.d(TAG, "sChangeForWhiteBg = " + sChangeForWhiteBg);
        AttribText.setup(context, sChangeForWhiteBg, true);
        AttribImage.setup(context, sChangeForWhiteBg);
    }

    public static void setupForStatusBar(Context context) {
        boolean z = false;
        if (System.getInt(context.getContentResolver(), PREFERENCES_NEED_DARK_STATUSBAR, 0) != 0) {
            z = true;
        }
        sChangeStatusBarForWhiteBg = z;
    }

    public static void setupForNavigationBar(Context context) {
        boolean z = false;
        if (System.getInt(context.getContentResolver(), PREFERENCES_NEED_DARK_NAVIGATIONBAR, 0) != 0) {
            z = true;
        }
        sChangeNavigationBarForWhiteBg = z;
    }

    public static void changeTextColorForBg(Context context, IconView iv, boolean whiteBg, boolean followThemeColor) {
        if (iv != null && context != null) {
            AttribText.setup(context, whiteBg, followThemeColor);
            iv.setTextColor(AttribText.sColor);
            if (!whiteBg && followThemeColor && AttribText.sShadowColor == 33554431) {
                iv.setShadowLayer(0.0f, 0.0f, 0.0f, 0);
            } else {
                iv.setShadowLayer(AttribText.sRadius, 0.0f, AttribText.sDy, AttribText.sShadowColor);
            }
        }
    }

    public static void changeTextColorForBg(Context context, TextView v, boolean whiteBg) {
        changeTextViewColorForBg(context, v, whiteBg, false);
    }

    public static void changeTextViewColorForBg(Context context, TextView v, boolean whiteBg, boolean followThemeColor) {
        if (v != null && context != null) {
            AttribText.setup(context, whiteBg, followThemeColor);
            v.setTextColor(AttribText.sColor);
            if (whiteBg || AttribText.sShadowColor != 33554431) {
                v.setShadowLayer(AttribText.sRadius, 0.0f, AttribText.sDy, AttribText.sShadowColor);
            } else {
                v.setShadowLayer(0.0f, 0.0f, 0.0f, 0);
            }
        }
    }

    public static void changeColorFilterForBg(Context context, ImageView v, boolean whiteBg) {
        if (v != null && context != null) {
            if (whiteBg) {
                v.setColorFilter(AttribImage.sColorFilter);
            } else {
                v.clearColorFilter();
            }
        }
    }

    public static void changeColorFilterForBg(Context context, Drawable drawable, boolean whiteBg) {
        if (drawable != null && context != null) {
            if (whiteBg) {
                drawable.setColorFilter(AttribImage.sColorFilter, Mode.SRC_ATOP);
            } else {
                drawable.clearColorFilter();
            }
        }
    }
}

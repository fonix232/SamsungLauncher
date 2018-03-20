package com.android.launcher3.util;

import android.util.Log;
import android.util.Range;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import com.android.launcher3.LauncherFeature;
import java.util.ArrayList;
import java.util.Iterator;

public final class BlurUtils {
    private static final float BLUR_UPDATE_LEVEL = 12.0f;
    private static final boolean DEBUG = false;
    private static final float DEFAULT_BLUR_AMOUNT = 0.15f;
    private static final long DEFAULT_BLUR_DURATION = -1;
    private static final float MAX_BLUR_AMOUNT = 0.15f;
    private static final String TAG = "BlurUtils";
    private static boolean sBlur = false;
    private static float sBlurAmount = 0.0f;
    private static ArrayList<Range> sRangeList = new ArrayList();

    static {
        sRangeList.add(Range.create(Integer.valueOf(5), Integer.valueOf(7)));
        sRangeList.add(Range.create(Integer.valueOf(7), Integer.valueOf(9)));
        sRangeList.add(Range.create(Integer.valueOf(9), Integer.valueOf(11)));
    }

    private BlurUtils() {
    }

    public static float getBlurAmount() {
        return sBlur ? sBlurAmount : 0.0f;
    }

    public static float getMaxBlurAmount() {
        return 0.15f;
    }

    public static void blurByWindowManager(boolean show, Window dest) {
        blurByWindowManager(show, dest, 0.15f, -1);
    }

    public static void blurByWindowManager(boolean show, Window dest, float amount, long duration) {
        if (sBlur != show || !compareByStage(sRangeList, sBlurAmount, amount, BLUR_UPDATE_LEVEL, 0.15f)) {
            if (sBlur || !show || amount != 0.0f) {
                Log.i(TAG, "blurByWindowManager with show = " + show + ", dest = " + dest + ", amount = " + amount + ", duration = " + duration + ", sBlur=" + sBlur);
                if (LauncherFeature.supportBackgroundBlurByWindow()) {
                    LayoutParams wlp = dest.getAttributes();
                    if (show) {
                        wlp.semAddExtensionFlags(64);
                        wlp.semSetEnterDimDuration(duration);
                        wlp.dimAmount = amount;
                        dest.addFlags(2);
                    } else {
                        wlp.semClearExtensionFlags(64);
                        wlp.semSetEnterDimDuration(duration);
                        dest.clearFlags(2);
                    }
                    sBlur = show;
                    if (!show) {
                        amount = 0.0f;
                    }
                    sBlurAmount = amount;
                }
            }
        }
    }

    public static void resetBlur() {
        sBlur = false;
        sBlurAmount = 0.0f;
    }

    private static boolean compareByStage(ArrayList<Range> rangeList, float value1, float value2, float level, float max) {
        if (level == 0.0f) {
            return false;
        }
        double unitValue = (double) (max / level);
        int left = (int) (((double) value1) / unitValue);
        int right = (int) (((double) value2) / unitValue);
        Iterator it = rangeList.iterator();
        while (it.hasNext()) {
            Range range = (Range) it.next();
            if (range.contains(Integer.valueOf(left)) && range.contains(Integer.valueOf(right))) {
                return true;
            }
        }
        if (left != right) {
            return false;
        }
        return true;
    }
}

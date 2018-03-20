package com.android.launcher3.util;

import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import com.android.launcher3.LauncherFeature;

public class SIPHelper {
    public static final int KEYPAD_MINIMIZE_HEIGHT = 22;
    private static final String TAG = "SIPHelper";

    public static void hideInputMethod(View view, boolean shouldMinimize) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService("input_method");
            if (imm == null) {
                return;
            }
            if (LauncherFeature.disableFullyHideKeypad() && shouldMinimize) {
                imm.semMinimizeSoftInput(view.getWindowToken(), 22);
            } else if (imm.semIsInputMethodShown()) {
                Log.d(TAG, "hideInputMethod view : " + view + "imm.isActive() : " + imm.isActive());
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}

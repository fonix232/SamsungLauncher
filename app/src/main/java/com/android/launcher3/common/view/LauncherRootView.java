package com.android.launcher3.common.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.android.launcher3.common.base.view.InsettableFrameLayout;

public class LauncherRootView extends InsettableFrameLayout {
    public static ScrollHelperByRootView sScrollHelperByRootView = new ScrollHelperByRootView();

    public LauncherRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        sScrollHelperByRootView.setTouchEvent(event);
        return super.onInterceptTouchEvent(event);
    }
}

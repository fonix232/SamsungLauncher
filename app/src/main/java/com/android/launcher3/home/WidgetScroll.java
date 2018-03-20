package com.android.launcher3.home;

import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.StackView;

class WidgetScroll {
    private final int TYPE_GRID = 3;
    private final int TYPE_LIST = 1;
    private final int TYPE_NONE = 0;
    private final int TYPE_STACK = 2;
    private final int TYPE_WEAHTER = 4;
    private int mScrollType = 0;
    private View mScrollView = null;

    WidgetScroll() {
    }

    int getScrollType(View view, boolean isAllowSwipe) {
        if (isAllowSwipe) {
            return this.mScrollType;
        }
        if (view == null || !(view instanceof ViewGroup)) {
            return 0;
        }
        if (view instanceof StackView) {
            this.mScrollView = view;
            return 2;
        } else if (view instanceof ListView) {
            this.mScrollView = view;
            return 1;
        } else if (view instanceof GridView) {
            this.mScrollView = view;
            return 3;
        } else {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                int type = getScrollType(vg.getChildAt(i), false);
                if (type > 0) {
                    this.mScrollType = type;
                    return type;
                }
            }
            this.mScrollType = 0;
            return 0;
        }
    }

    boolean isScrollable() {
        if (this.mScrollType <= 0 || this.mScrollView == null) {
            if (this.mScrollType == 4) {
                return true;
            }
            return false;
        } else if (this.mScrollView.canScrollVertically(1) || this.mScrollView.canScrollVertically(-1)) {
            return true;
        } else {
            return false;
        }
    }

    void setWeatherScrollablility(boolean scrollablility) {
        this.mScrollType = scrollablility ? 4 : 0;
    }

    boolean isWeather() {
        return this.mScrollType == 4;
    }
}

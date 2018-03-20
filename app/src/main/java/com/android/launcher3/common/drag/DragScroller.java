package com.android.launcher3.common.drag;

public interface DragScroller {
    int getScrollZone();

    boolean onEnterScrollArea(int i, int i2, int i3);

    boolean onExitScrollArea();

    void scrollLeft();

    void scrollRight();
}

package com.android.launcher3.common.base.view;

public interface Page {
    void enableHardwareLayers(boolean z);

    int getPageItemCount();

    void removeAllViewsOnPage();

    void removeViewOnPageAt(int i);
}

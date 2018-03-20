package com.android.launcher3.common.deviceprofile;

import android.content.Context;
import android.util.DisplayMetrics;
import com.android.launcher3.Utilities;

public class GridIconInfo {
    private final float START_PADDING_FOR_LANDSCAPE = 2.0f;
    private int mContentTop;
    private final int mDrawablePadding;
    private final int mIconSize;
    private int mIconStartPadding;
    private final int mLineCount;
    private final int mMaxCount;
    private int mTextSize;

    public GridIconInfo(Context context, String[] info) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        this.mMaxCount = Integer.parseInt(info[0]);
        this.mIconSize = Utilities.pxFromDp(Float.parseFloat(info[1]), metrics);
        this.mTextSize = Utilities.pxFromDp(Float.parseFloat(info[2]), metrics);
        this.mDrawablePadding = Utilities.pxFromDp(Float.parseFloat(info[3]), metrics);
        this.mLineCount = Integer.parseInt(info[4]);
        this.mContentTop = -1;
        this.mIconStartPadding = Utilities.pxFromDp(2.0f, metrics);
    }

    public GridIconInfo(int maxCount, int iconSize, int textSize, int drawablePadding, int lineCount, DisplayMetrics metrics) {
        this.mMaxCount = maxCount;
        this.mIconSize = iconSize;
        this.mTextSize = textSize;
        this.mDrawablePadding = drawablePadding;
        this.mLineCount = lineCount;
        this.mContentTop = -1;
        this.mIconStartPadding = Utilities.pxFromDp(2.0f, metrics);
    }

    public int getMaxCount() {
        return this.mMaxCount;
    }

    public int getIconSize() {
        return this.mIconSize;
    }

    public void setTextSize(int size) {
        this.mTextSize = size;
    }

    public int getTextSize() {
        return this.mTextSize;
    }

    public int getDrawablePadding() {
        return this.mDrawablePadding;
    }

    public int getLineCount() {
        return this.mLineCount;
    }

    public void setContentTop(int contentTop) {
        this.mContentTop = contentTop;
    }

    public int getContentTop() {
        return this.mContentTop;
    }

    public int getIconStartPadding() {
        return this.mIconStartPadding;
    }
}

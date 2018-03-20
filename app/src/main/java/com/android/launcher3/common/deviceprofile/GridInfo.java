package com.android.launcher3.common.deviceprofile;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import com.android.launcher3.Utilities;

public class GridInfo {
    private static final String TAG = "Launcher.GridInfo";
    private final int mCellGapX;
    private final int mCellGapY;
    private int mCellHeight;
    private int mCellWidth;
    private final int mCountX;
    private final int mCountY;
    private int mHotseatBarSize;
    private int mHotseatBottom = 0;
    private int mHotseatContentTop;
    private final GridIconInfo mIconInfo;
    private int mIndicatorBottom;
    private final int mPageBottom;
    private final int mPagePadding;
    private final int mPageTop;

    public GridInfo(Context context, String[] info) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int infoSize = info.length;
        this.mCountX = Integer.parseInt(info[0]);
        this.mCountY = Integer.parseInt(info[1]);
        this.mCellGapX = Utilities.pxFromDp(Float.parseFloat(info[2]), metrics);
        this.mCellGapY = Utilities.pxFromDp(Float.parseFloat(info[3]), metrics);
        int iconSize = Utilities.pxFromDp(Float.parseFloat(info[4]), metrics);
        int textSize = Utilities.pxFromDp(Float.parseFloat(info[5]), metrics);
        int drawablePadding = Utilities.pxFromDp(Float.parseFloat(info[6]), metrics);
        int lineCount = Integer.parseInt(info[7]);
        this.mPagePadding = Utilities.pxFromDp(Float.parseFloat(info[8]), metrics);
        this.mPageTop = Utilities.pxFromDp(Float.parseFloat(info[9]), metrics);
        this.mPageBottom = Utilities.pxFromDp(Float.parseFloat(info[10]), metrics);
        if (infoSize > 11) {
            this.mIndicatorBottom = Utilities.pxFromDp(Float.parseFloat(info[11]), metrics);
        }
        if (infoSize > 12) {
            this.mHotseatBarSize = Utilities.pxFromDp(Float.parseFloat(info[12]), metrics);
        }
        if (infoSize > 13) {
            this.mHotseatContentTop = Utilities.pxFromDp(Float.parseFloat(info[13]), metrics);
        } else {
            this.mHotseatContentTop = -1;
        }
        if (infoSize > 14) {
            this.mHotseatBottom = Utilities.pxFromDp(Float.parseFloat(info[14]), metrics);
        }
        this.mIconInfo = new GridIconInfo(this.mCountX, iconSize, textSize, drawablePadding, lineCount, metrics);
        this.mCellHeight = 0;
        this.mCellWidth = 0;
        Log.i(TAG, "countX : " + this.mCountX + ", countY : " + this.mCountY);
    }

    public int getCellCountX() {
        return this.mCountX;
    }

    public int getCellCountY() {
        return this.mCountY;
    }

    public int getCellGapX() {
        return this.mCellGapX;
    }

    public int getCellGapY() {
        return this.mCellGapY;
    }

    public int getIconSize() {
        return this.mIconInfo.getIconSize();
    }

    public int getTextSize() {
        return this.mIconInfo.getTextSize();
    }

    public int getDrawablePadding() {
        return this.mIconInfo.getDrawablePadding();
    }

    public int getPagePadding() {
        return this.mPagePadding;
    }

    public int getPageTop() {
        return this.mPageTop;
    }

    public int getPageBottom() {
        return this.mPageBottom;
    }

    public int getIndicatorBottom() {
        return this.mIndicatorBottom;
    }

    public int getHotseatBarSize() {
        return this.mHotseatBarSize;
    }

    public int getHotseatBottom() {
        return this.mHotseatBottom;
    }

    public int getHotseatContentTop() {
        return this.mHotseatContentTop;
    }

    public void setCellWidth(int cellWidth) {
        this.mCellWidth = cellWidth;
    }

    public int getCellWidth() {
        return this.mCellWidth;
    }

    public void setCellHeight(int cellHeight) {
        this.mCellHeight = cellHeight;
    }

    public int getCellHeight() {
        return this.mCellHeight;
    }

    public int getContentTop() {
        return this.mIconInfo.getContentTop();
    }

    public GridIconInfo getIconInfo() {
        return this.mIconInfo;
    }
}

package com.android.launcher3.util.event;

import android.graphics.Rect;

/* compiled from: ScreenDivision */
class Partition {
    int mEndIndex;
    Rect mRect;
    int mStartIndex;

    Partition(int startIndex, int endIndex, Rect rect) {
        this.mStartIndex = startIndex;
        this.mEndIndex = endIndex;
        this.mRect = new Rect(rect);
    }
}

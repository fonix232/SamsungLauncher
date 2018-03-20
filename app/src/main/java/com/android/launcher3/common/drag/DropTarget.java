package com.android.launcher3.common.drag;

import android.graphics.Rect;
import android.view.View;

public interface DropTarget {
    public static final String TAG = "DropTarget";

    boolean acceptDrop(DragObject dragObject);

    void getHitRectRelativeToDragLayer(Rect rect);

    int getLeft();

    int getOutlineColor();

    View getTargetView();

    int getTop();

    boolean isDropEnabled(boolean z);

    void onDragEnter(DragObject dragObject, boolean z);

    void onDragExit(DragObject dragObject, boolean z);

    void onDragOver(DragObject dragObject);

    void onDrop(DragObject dragObject);
}

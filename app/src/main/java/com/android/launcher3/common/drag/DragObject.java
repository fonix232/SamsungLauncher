package com.android.launcher3.common.drag;

import java.util.ArrayList;

public class DragObject {
    public boolean cancelDropFolder = false;
    public boolean cancelled = false;
    public boolean deferDragViewCleanupPostAnimation = true;
    public boolean dragComplete = false;
    public Object dragInfo = null;
    public DragSource dragSource = null;
    public DragView dragView = null;
    public ArrayList<DragObject> extraDragInfoList = null;
    public ArrayList<DragSource> extraDragSourceList = null;
    public Runnable postAnimationRunnable = null;
    public boolean restored = false;
    public int x = -1;
    public int xOffset = -1;
    public int y = -1;
    public int yOffset = -1;

    public DragObject(DragObject info) {
        copyFrom(info);
    }

    public void copyFrom(DragObject info) {
        this.x = info.x;
        this.y = info.y;
        this.xOffset = info.xOffset;
        this.yOffset = info.yOffset;
        this.dragComplete = info.dragComplete;
        this.dragView = info.dragView;
        this.dragInfo = info.dragInfo;
        this.dragSource = info.dragSource;
        this.postAnimationRunnable = info.postAnimationRunnable;
        this.cancelled = info.cancelled;
        this.cancelDropFolder = info.cancelDropFolder;
        this.restored = info.restored;
        this.deferDragViewCleanupPostAnimation = info.deferDragViewCleanupPostAnimation;
        this.extraDragInfoList = info.extraDragInfoList;
        this.extraDragSourceList = info.extraDragSourceList;
    }

    public final float[] getVisualCenter(float[] recycle) {
        float[] res;
        if (recycle == null) {
            res = new float[2];
        } else {
            res = recycle;
        }
        int top = this.y - this.yOffset;
        res[0] = (float) ((this.dragView.getDragRegion().width() / 2) + (this.x - this.xOffset));
        res[1] = (float) ((this.dragView.getDragRegion().height() / 2) + top);
        return res;
    }
}

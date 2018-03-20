package com.android.launcher3.allapps;

public class DragAppIcon implements Cloneable {
    public int cellX;
    public int cellY;
    public int rank;
    public long screenId;

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public int getScreenId() {
        return Long.valueOf(this.screenId).intValue();
    }
}

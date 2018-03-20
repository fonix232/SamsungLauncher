package com.android.launcher3.widget;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.ViewGroup;

public class WidgetsRowViewHolder extends ViewHolder {
    ViewGroup mContent;

    public WidgetsRowViewHolder(ViewGroup v) {
        super(v);
        this.mContent = v;
    }

    ViewGroup getContent() {
        return this.mContent;
    }
}

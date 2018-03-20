package com.android.launcher3.widget.view;

import android.view.ViewGroup;

public interface ViewRecycler {
    ViewGroup get(boolean z, ViewGroup viewGroup);

    void recycle(ViewGroup viewGroup);
}

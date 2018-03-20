package com.android.launcher3.allapps.view;

import android.view.View;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.view.OnInflateListener;

public interface Stub {
    void cancelTask();

    void inflateInBackground(ItemInfo itemInfo);

    void prefetchIconResInBackground(ItemInfo itemInfo, Inflater inflater);

    void replaceView(View view, boolean z);

    void replaceView(ItemInfo itemInfo, Inflater inflater, boolean z);

    void setInflateListener(OnInflateListener onInflateListener);
}

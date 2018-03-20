package com.android.launcher3.common.base.item;

import android.view.View;

public interface ItemOperator {
    boolean evaluate(ItemInfo itemInfo, View view, View view2);
}

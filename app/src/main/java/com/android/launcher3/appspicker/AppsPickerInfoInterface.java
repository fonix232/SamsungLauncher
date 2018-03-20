package com.android.launcher3.appspicker;

import com.android.launcher3.common.base.item.IconInfo;

public interface AppsPickerInfoInterface {
    boolean isCheckedItem(IconInfo iconInfo);

    void onToggleItem(IconInfo iconInfo);
}

package com.android.launcher3.appspicker;

import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.launcher3.common.base.item.IconInfo;

class AppIconViewHolder {
    CheckBox checkBox;
    int colIndex;
    ViewGroup container;
    ImageView icon;
    IconInfo iconInfo;
    View leftGap;
    int rowIndex;
    int screenMode;
    TextView title;

    AppIconViewHolder() {
    }
}

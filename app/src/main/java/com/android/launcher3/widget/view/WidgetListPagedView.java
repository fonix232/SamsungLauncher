package com.android.launcher3.widget.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout.LayoutParams;
import com.sec.android.app.launcher.R;

public class WidgetListPagedView extends WidgetPagedView {
    public WidgetListPagedView(Context context) {
        this(context, null);
    }

    public WidgetListPagedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetListPagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public int getWidgetItemSingleViewId() {
        return R.layout.widget_item_single_view;
    }

    public int getWidgetItemFolderViewId() {
        return R.layout.widget_item_folder_view;
    }

    public int getWidgetPageLayoutId() {
        return R.layout.widget_page_layout;
    }

    public int getRowCount() {
        return getResources().getInteger(R.integer.widget_page_row);
    }

    public int getColumnCount() {
        return getResources().getInteger(R.integer.widget_page_column);
    }

    public void updateWidgetPagedView() {
        ((LayoutParams) getLayoutParams()).topMargin = getResources().getDimensionPixelSize(R.dimen.widget_page_top_margin);
    }

    public void onConfigurationChangedIfNeeded() {
        updateWidgetPagedView();
        super.onConfigurationChangedIfNeeded();
    }
}

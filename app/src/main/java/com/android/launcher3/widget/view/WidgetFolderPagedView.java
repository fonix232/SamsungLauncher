package com.android.launcher3.widget.view;

import android.content.Context;
import android.util.AttributeSet;
import com.sec.android.app.launcher.R;

public class WidgetFolderPagedView extends WidgetPagedView {
    public WidgetFolderPagedView(Context context) {
        this(context, null);
    }

    public WidgetFolderPagedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetFolderPagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public int getWidgetItemSingleViewId() {
        return R.layout.widget_item_single_view;
    }

    public int getWidgetItemFolderViewId() {
        return R.layout.widget_item_folder_view;
    }

    public int getWidgetPageLayoutId() {
        return R.layout.widget_folder_page_layout;
    }

    public int getRowCount() {
        return getResources().getInteger(R.integer.widget_folder_page_row);
    }

    public int getColumnCount() {
        return getResources().getInteger(R.integer.widget_folder_page_column);
    }

    public int getDesiredWidth() {
        if (getPageCount() > 0) {
            return (getPageAt(0).getDesiredWidth() + getPaddingLeft()) + getPaddingRight();
        }
        return 0;
    }

    public int getDesiredHeight() {
        if (getPageCount() > 0) {
            return (getPageAt(0).getDesiredHeight() + getPaddingTop()) + getPaddingBottom();
        }
        return 0;
    }

    public WidgetPageLayout getPageAt(int index) {
        return (WidgetPageLayout) getChildAt(index);
    }

    public void onConfigurationChangedIfNeeded() {
        super.onConfigurationChangedIfNeeded();
    }
}

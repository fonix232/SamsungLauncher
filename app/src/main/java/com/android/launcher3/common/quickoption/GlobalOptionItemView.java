package com.android.launcher3.common.quickoption;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;
import com.android.launcher3.Launcher;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.List;

public class GlobalOptionItemView extends PopupItemView {
    private Launcher mLauncher;
    private int mOptionSize;

    public GlobalOptionItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GlobalOptionItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mOptionSize = 0;
    }

    public GlobalOptionItemView(Context context) {
        this(context, null, 0);
    }

    void createContainerItems(Launcher launcher, LayoutInflater inflater, List<QuickOptionListItem> globalOptions, boolean hasDeepShortcut, boolean isLandscape, boolean isWidget) {
        this.mLauncher = launcher;
        if (isWidget && isLandscape) {
            this.mPopupHeight = getResources().getDimensionPixelSize(R.dimen.quick_option_global_option_widget_height);
        } else if (hasDeepShortcut) {
            this.mPopupHeight = getResources().getDimensionPixelSize(R.dimen.quick_option_global_option_height);
        } else {
            this.mPopupHeight = getResources().getDimensionPixelSize(R.dimen.quick_option_global_option_single_height);
        }
        int mItemWidth = getPopupWidth() / globalOptions.size();
        setOptionSize(globalOptions.size());
        for (int i = 0; i < getOptionSize(); i++) {
            GlobalOption itemView = (GlobalOption) inflater.inflate(R.layout.global_option_item, this, false);
            itemView.setItem(this.mLauncher, (QuickOptionListItem) globalOptions.get(i), isWidget);
            LayoutParams lp = new LayoutParams(mItemWidth, this.mPopupHeight);
            itemView.setOnClickListener(itemView);
            itemView.setOnKeyListener(this.mKeyListener);
            addView(itemView, i, lp);
        }
        setLayoutParams(new LayoutParams(getPopupWidth(), this.mPopupHeight));
    }

    public int getOptionSize() {
        return this.mOptionSize;
    }

    private void setOptionSize(int optionSize) {
        this.mOptionSize = optionSize;
    }

    ArrayList<View> getAccessibilityFocusChildViewList() {
        ArrayList<View> childList = new ArrayList();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof GlobalOption) {
                childList.add(child);
            }
        }
        return childList;
    }
}

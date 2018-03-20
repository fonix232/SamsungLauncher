package com.android.launcher3.common.base.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import com.sec.android.app.launcher.R;

public abstract class BaseContainerView extends LinearLayout implements Insettable {
    private static final String TAG = "BaseContainerView";
    private int mContainerBoundsInset;
    protected Rect mContentBounds;
    private Rect mContentPadding;
    private Rect mFixedSearchBarBounds;
    private boolean mHasSearchBar;
    private Rect mInsets;
    private Rect mSearchBarBounds;

    protected abstract void onUpdateBackgroundAndPaddings(Rect rect, Rect rect2);

    public BaseContainerView(Context context) {
        this(context, null);
    }

    public BaseContainerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mInsets = new Rect();
        this.mFixedSearchBarBounds = new Rect();
        this.mSearchBarBounds = new Rect();
        this.mContentBounds = new Rect();
        this.mContentPadding = new Rect();
        this.mContainerBoundsInset = getResources().getDimensionPixelSize(R.dimen.container_bounds_inset);
    }

    public final void setInsets(Rect insets) {
        this.mInsets.set(insets);
        updateBackgroundAndPaddings();
    }

    protected void setHasSearchBar() {
        this.mHasSearchBar = true;
    }

    public final void setSearchBarBounds(Rect bounds) {
        this.mFixedSearchBarBounds.set(bounds);
        post(new Runnable() {
            public void run() {
                BaseContainerView.this.updateBackgroundAndPaddings();
            }
        });
    }

    protected void updateBackgroundAndPaddings() {
        Rect padding;
        int i = 0;
        Rect searchBarBounds = new Rect();
        int i2;
        if (isValidSearchBarBounds(this.mFixedSearchBarBounds)) {
            i2 = this.mFixedSearchBarBounds.left;
            if (!this.mHasSearchBar) {
                i = this.mInsets.top + this.mContainerBoundsInset;
            }
            padding = new Rect(i2, i, getMeasuredWidth() - this.mFixedSearchBarBounds.right, this.mInsets.bottom + this.mContainerBoundsInset);
            searchBarBounds.set(this.mFixedSearchBarBounds);
        } else {
            int i3 = this.mContainerBoundsInset + this.mInsets.left;
            if (this.mHasSearchBar) {
                i2 = 0;
            } else {
                i2 = this.mInsets.top + this.mContainerBoundsInset;
            }
            padding = new Rect(i3, i2, this.mInsets.right + this.mContainerBoundsInset, this.mInsets.bottom + this.mContainerBoundsInset);
            searchBarBounds.set(this.mInsets.left + this.mContainerBoundsInset, this.mInsets.top + this.mContainerBoundsInset, getMeasuredWidth() - (this.mInsets.right + this.mContainerBoundsInset), 0);
        }
        if (!padding.equals(this.mContentPadding) || !searchBarBounds.equals(this.mSearchBarBounds)) {
            this.mContentPadding.set(padding);
            this.mContentBounds.set(padding.left, padding.top, getMeasuredWidth() - padding.right, getMeasuredHeight() - padding.bottom);
            this.mSearchBarBounds.set(searchBarBounds);
            onUpdateBackgroundAndPaddings(this.mSearchBarBounds, padding);
        }
    }

    private boolean isValidSearchBarBounds(Rect searchBarBounds) {
        return !searchBarBounds.isEmpty() && searchBarBounds.right <= getMeasuredWidth() && searchBarBounds.bottom <= getMeasuredHeight();
    }
}

package com.android.launcher3.widget.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.GridLayout.LayoutParams;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.common.base.view.Page;
import com.android.launcher3.widget.controller.WidgetState.State;
import com.sec.android.app.launcher.R;
import java.util.List;

public class WidgetPageLayout extends GridLayout implements Page {
    private static final String TAG = "WidgetPageLayout";
    private static final Paint sPaint = new Paint();
    private int mDesiredHeight;
    private int mDesiredWidth;
    private int mHorizontalGap;
    private OnKeyListener mItemKeyListener;
    private int mVerticalGap;
    private ViewRecycler mViewRecycler;
    private boolean mWhiteWallpaper;

    public WidgetPageLayout(Context context) {
        this(context, null);
    }

    public WidgetPageLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetPageLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WidgetPageLayout, defStyle, 0);
        this.mHorizontalGap = a.getDimensionPixelSize(0, 0);
        this.mVerticalGap = a.getDimensionPixelSize(1, 0);
        this.mDesiredWidth = a.getDimensionPixelSize(2, 0);
        this.mDesiredHeight = a.getDimensionPixelSize(3, 0);
        a.recycle();
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        int count = getPageItemCount();
        if (count <= 0) {
            return result;
        }
        return result || event.getY() < ((float) getChildAt(count - 1).getBottom());
    }

    public void enableHardwareLayers(boolean hasLayer) {
        setLayerType(hasLayer ? 2 : 0, sPaint);
    }

    public void removeAllViewsOnPage() {
        removeAllViews();
        enableHardwareLayers(false);
    }

    public void removeViewOnPageAt(int index) {
        if (this.mViewRecycler != null) {
            ViewGroup view = (ViewGroup) getChildAt(index);
            removeViewAt(index);
            this.mViewRecycler.recycle(view);
            return;
        }
        removeViewAt(index);
    }

    public void removeAllViews() {
        if (this.mViewRecycler != null) {
            int childCount = getChildCount();
            ViewGroup[] childViews = new ViewGroup[childCount];
            for (int i = 0; i < childCount; i++) {
                childViews[i] = (ViewGroup) getChildAt(i);
            }
            super.removeAllViews();
            for (ViewGroup view : childViews) {
                this.mViewRecycler.recycle(view);
            }
            return;
        }
        super.removeAllViews();
    }

    public int getPageItemCount() {
        return getChildCount();
    }

    public void setAccessibilityEnabled(boolean enabled) {
        int accessibility = enabled ? 1 : 2;
        setImportantForAccessibility(accessibility);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View v = getChildAt(i);
            if (v instanceof WidgetItemView) {
                v.setImportantForAccessibility(accessibility);
            }
        }
    }

    public void setViewRecycler(ViewRecycler viewRecycler) {
        this.mViewRecycler = viewRecycler;
    }

    public void setItemOnKeyListener(OnKeyListener l) {
        this.mItemKeyListener = l;
    }

    public void changeState(State state, boolean anim) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if (view instanceof WidgetItemView) {
                ((WidgetItemView) view).changeState(state, anim);
            }
        }
    }

    public void changeColorForBg(boolean whiteBg) {
        this.mWhiteWallpaper = whiteBg;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if (view instanceof WidgetItemView) {
                ((WidgetItemView) view).changeColorForBg(whiteBg);
            }
        }
    }

    public void updateCellSpan() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if (view instanceof WidgetItemView) {
                ((WidgetItemView) view).applyCellSpan();
            }
        }
    }

    public void bindItems(List<Object> items, String search, State state) {
        int size = items.size();
        int nextFocusId = -1;
        for (int i = 0; i < size; i++) {
            List<PendingAddItemInfo> infos = (List) items.get(i);
            if (!(infos == null || infos.isEmpty())) {
                boolean z;
                ViewRecycler viewRecycler = this.mViewRecycler;
                if (infos.size() > 1) {
                    z = true;
                } else {
                    z = false;
                }
                WidgetItemView itemView = (WidgetItemView) viewRecycler.get(z, this);
                itemView.setTag(infos.get(0));
                itemView.setWidgets(infos);
                int ix = i % getColumnCount();
                int iy = i / getColumnCount();
                LayoutParams lp = new LayoutParams(GridLayout.spec(iy, GridLayout.START), GridLayout.spec(ix, GridLayout.TOP));
                lp.setGravity(8388659);
                if (iy > 0) {
                    lp.topMargin = this.mVerticalGap;
                }
                if (ix > 0) {
                    lp.setMarginStart(this.mHorizontalGap);
                }
                itemView.applyTileAndSpan(search);
                itemView.changeColorForBg(this.mWhiteWallpaper);
                itemView.changeState(state, false);
                itemView.setOnKeyListener(this.mItemKeyListener);
                addView(itemView, lp);
                itemView.requestPreview(this);
                nextFocusId = configureNextFocus(nextFocusId, itemView, i);
            }
        }
    }

    private int configureNextFocus(int nextFocusId, View itemView, int pos) {
        if (nextFocusId != -1) {
            if (Utilities.sIsRtl) {
                itemView.setNextFocusRightId(nextFocusId);
            } else {
                itemView.setNextFocusLeftId(nextFocusId);
            }
        }
        if (itemView.getId() == -1) {
            itemView.setId(View.generateViewId());
        }
        View privChild = getChildAt(pos - 1);
        if (privChild != null) {
            if (Utilities.sIsRtl) {
                privChild.setNextFocusLeftId(itemView.getId());
            } else {
                privChild.setNextFocusRightId(itemView.getId());
            }
        }
        return itemView.getId();
    }

    public int getDesiredWidth() {
        return ((getPaddingLeft() + getPaddingRight()) + (getColumnCount() * this.mDesiredWidth)) + (Math.max(getRowCount() - 1, 0) * this.mHorizontalGap);
    }

    public int getDesiredHeight() {
        return ((getPaddingTop() + getPaddingBottom()) + (getContentRowCount() * this.mDesiredHeight)) + (Math.max(getContentRowCount() - 1, 0) * this.mVerticalGap);
    }

    private int getContentRowCount() {
        return getRowCount();
    }
}

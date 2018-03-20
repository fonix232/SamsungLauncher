package com.android.launcher3.common.base.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.widget.FrameLayout;
import com.sec.android.app.launcher.R;

public class InsettableFrameLayout extends FrameLayout implements OnHierarchyChangeListener, Insettable {
    protected Rect mInsets = new Rect();

    public static class LayoutParams extends android.widget.FrameLayout.LayoutParams {
        boolean ignoreInsets = false;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.InsettableFrameLayout_Layout);
            this.ignoreInsets = a.getBoolean(0, false);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams lp) {
            super(lp);
        }
    }

    public InsettableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnHierarchyChangeListener(this);
    }

    public void setFrameLayoutChildInsets(View child, Rect newInsets, Rect oldInsets) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (child instanceof Insettable) {
            ((Insettable) child).setInsets(newInsets);
        } else if (!lp.ignoreInsets) {
            lp.topMargin += newInsets.top - oldInsets.top;
            lp.leftMargin += newInsets.left - oldInsets.left;
            lp.rightMargin += newInsets.right - oldInsets.right;
            lp.bottomMargin += newInsets.bottom - oldInsets.bottom;
        }
        child.setLayoutParams(lp);
    }

    public Rect getInsets() {
        return this.mInsets;
    }

    public void setInsets(Rect insets) {
        int n = getChildCount();
        for (int i = 0; i < n; i++) {
            setFrameLayoutChildInsets(getChildAt(i), insets, this.mInsets);
        }
        this.mInsets.set(insets);
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    protected boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    protected LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public void onChildViewAdded(View parent, View child) {
        setFrameLayoutChildInsets(child, this.mInsets, new Rect());
    }

    public void onChildViewRemoved(View parent, View child) {
    }
}

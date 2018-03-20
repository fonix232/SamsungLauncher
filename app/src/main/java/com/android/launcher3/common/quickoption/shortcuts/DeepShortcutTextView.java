package com.android.launcher3.common.quickoption.shortcuts;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.view.IconView;
import com.sec.android.app.launcher.R;

public class DeepShortcutTextView extends IconView {
    private final Rect mDragHandleBounds;
    private final int mDragHandleWidth;
    private boolean mShouldPerformClick;

    public DeepShortcutTextView(Context context) {
        this(context, null, 0);
    }

    public DeepShortcutTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeepShortcutTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mDragHandleBounds = new Rect();
        this.mShouldPerformClick = true;
        Resources resources = getResources();
        this.mDragHandleWidth = (resources.getDimensionPixelSize(R.dimen.deep_shortcut_padding_end) + resources.getDimensionPixelSize(R.dimen.deep_shortcut_drag_handle_size)) + (resources.getDimensionPixelSize(R.dimen.deep_shortcut_drawable_padding) / 2);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.mDragHandleBounds.set(0, 0, this.mDragHandleWidth, getMeasuredHeight());
        if (!Utilities.sIsRtl) {
            this.mDragHandleBounds.offset(getMeasuredWidth() - this.mDragHandleBounds.width(), 0);
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0) {
            this.mShouldPerformClick = !this.mDragHandleBounds.contains((int) ev.getX(), (int) ev.getY());
        }
        return super.onTouchEvent(ev);
    }

    public boolean performClick() {
        return this.mShouldPerformClick && super.performClick();
    }
}

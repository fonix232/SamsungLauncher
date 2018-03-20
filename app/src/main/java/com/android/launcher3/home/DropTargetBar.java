package com.android.launcher3.home;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.common.drag.DragManager;
import com.sec.android.app.launcher.R;

public class DropTargetBar extends FrameLayout {
    private CancelDropTarget mCancelDropTarget;
    private DragManager mDragManager;

    public DropTargetBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DropTargetBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    void setup(DragManager dragMgr) {
        this.mDragManager = dragMgr;
        this.mDragManager.addDragListener(this.mCancelDropTarget);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        setVisibility(4);
        this.mCancelDropTarget = (CancelDropTarget) findViewById(R.id.cancel_target_layout);
    }

    void setDropTargetBarVisible(boolean isVisible) {
        if (isVisible) {
            this.mDragManager.addDropTarget(this.mCancelDropTarget);
        } else {
            this.mDragManager.removeDropTarget(this.mCancelDropTarget);
        }
        setVisibility(isVisible ? 0 : 8);
        this.mCancelDropTarget.setEnable(isVisible);
    }

    void showCancelDropTarget() {
        this.mCancelDropTarget.animateCancelDropTarget();
    }

    void changeColorForBg(boolean whiteBg) {
        this.mCancelDropTarget.changeColorForBg(whiteBg);
    }

    void onConfigurationChangedIfNeeded() {
        int cancelDropTargetHeight;
        LayoutParams lp = (LayoutParams) getLayoutParams();
        lp.height = getResources().getDimensionPixelOffset(R.dimen.drop_target_bar_height);
        lp.topMargin = getResources().getDimensionPixelOffset(R.dimen.drop_target_margin_top);
        setLayoutParams(lp);
        if (LauncherFeature.isTablet()) {
            cancelDropTargetHeight = getResources().getDimensionPixelOffset(R.dimen.drop_target_cancel_animation_circleview_width);
        } else {
            cancelDropTargetHeight = getResources().getDimensionPixelOffset(R.dimen.drop_target_bar_height);
        }
        this.mCancelDropTarget.onConfigurationChangedIfNeeded(cancelDropTargetHeight);
    }
}

package com.android.launcher3.widget.controller;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout.LayoutParams;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.util.UninstallAppUtils;
//import com.android.launcher3.widget.controller.WidgetState.State;
//import com.samsung.android.widget.SemHoverPopupWindow;
import com.sec.android.app.launcher.R;

public class WidgetStateUninstall extends WidgetState implements OnClickListener {
    private View mTitleBar;

    public WidgetStateUninstall(Context context, View titleBar) {
        super(context, titleBar);
    }

    public void onWidgetItemClick(View view) {
        uninstallWidget(view);
    }

    public boolean onWidgetItemLongClick(View view) {
        return false;
    }

    public void enter(WidgetState fromState, AnimatorSet animSet) {
        if (animSet != null) {
            Animator animator = AnimatorInflater.loadAnimator(this.mContext, R.animator.enter_widget_titlebar);
            animator.setTarget(this.mTitleBar);
            animSet.play(animator);
        } else {
            this.mTitleBar.setAlpha(1.0f);
        }
        this.mTitleBar.setVisibility(View.VISIBLE);
        LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(22);
    }

    public void exit(WidgetState toState, AnimatorSet animSet) {
        if (animSet != null) {
            Animator animator = AnimatorInflater.loadAnimator(this.mContext, R.animator.exit_widget_titlebar);
            animator.setTarget(this.mTitleBar);
            animSet.play(animator);
        }
        this.mTitleBar.setVisibility(View.GONE);
    }

    public State getState() {
        return State.UNINSTALL;
    }

    public void initTitleBar(View titleBar) {
        super.initTitleBar(titleBar);
        if (titleBar == null) {
            throw new IllegalArgumentException(getState() + " titlebar is null");
        }
        this.mTitleBar = titleBar;
        View backButton = titleBar.findViewById(R.id.menu_selected_back_down);
        if (backButton != null) {
            backButton.setOnClickListener(this);
            // TODO: Samsung specific code
//            backButton.semSetHoverPopupType(1);
//            SemHoverPopupWindow hover = backButton.semGetHoverPopup(true);
//            if (hover != null) {
//                hover.setContent(this.mContext.getResources().getString(R.string.menu_navigate_up));
//            }
        }
    }

    public boolean onBackPressed() {
        if (this.mActionListener == null) {
            return false;
        }
        this.mActionListener.notifyChangeState(State.NORMAL);
        return true;
    }

    private void uninstallWidget(View view) {
        Object tag = view.getTag();
        if (tag instanceof PendingAddItemInfo) {
            PendingAddItemInfo info = (PendingAddItemInfo)tag;
            if (info.uninstallable(this.mContext)) {
                UninstallAppUtils.startUninstallActivity(this.mContext, info.user, info.componentName);
            }
        }
    }

    public void onClick(View v) {
        if (v.getId() == R.id.menu_selected_back_down && this.mActionListener != null) {
            this.mActionListener.notifyChangeState(State.NORMAL);
        }
    }

    public void setFocus() {
        this.mTitleBar.findViewById(R.id.menu_selected_back_down).requestFocus();
    }

    public void onConfigurationChangedIfNeeded() {
        ((LayoutParams) this.mTitleBar.getLayoutParams()).height = this.mContext.getResources().getDimensionPixelSize(R.dimen.widget_title_bar_height);
    }
}

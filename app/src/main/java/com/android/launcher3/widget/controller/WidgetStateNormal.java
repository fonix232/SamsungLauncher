package com.android.launcher3.widget.controller;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.view.View;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.logging.SALogging;
import com.android.launcher3.widget.controller.WidgetState.State;
import com.android.launcher3.widget.view.WidgetItemFolderView;
import com.android.launcher3.widget.view.WidgetItemSingleView;
import com.android.launcher3.widget.view.WidgetSearchbar;
import com.android.launcher3.widget.view.WidgetSearchbar.MenuActionListener;
import com.sec.android.app.launcher.R;

public class WidgetStateNormal extends WidgetState implements MenuActionListener {
    private WidgetSearchbar mWidgetSearchbar;

    public WidgetStateNormal(Context context, View titleBar) {
        super(context, titleBar);
    }

    public void onWidgetItemClick(View view) {
        if (view instanceof WidgetItemFolderView) {
            if (this.mActionListener != null) {
                this.mActionListener.openFolder(view, true);
                SALogging.getInstance().insertEventLog(this.mContext.getResources().getString(R.string.screen_Widgets), this.mContext.getResources().getString(R.string.event_OpenWidgetFolder));
            }
        } else if (view instanceof WidgetItemSingleView) {
            clickNotAllowed(view);
            SALogging.getInstance().insertEventLog(this.mContext.getResources().getString(R.string.screen_Widgets), this.mContext.getResources().getString(R.string.event_AddWidgetToHomeScreen), "b");
        }
    }

    public boolean onWidgetItemLongClick(View view) {
        if (!(view instanceof WidgetItemSingleView) || this.mActionListener == null) {
            return false;
        }
        this.mActionListener.startDrag(view);
        return true;
    }

    public void enter(WidgetState fromState, AnimatorSet animSet) {
        if (fromState == null || fromState.getState() != State.UNINSTALL) {
            this.mWidgetSearchbar.setAlpha(1.0f);
        } else if (animSet != null) {
            Animator animator = AnimatorInflater.loadAnimator(this.mContext, R.animator.enter_widget_titlebar);
            animator.setTarget(this.mWidgetSearchbar);
            animSet.play(animator);
        }
        this.mWidgetSearchbar.setVisibility(View.VISIBLE);
        this.mWidgetSearchbar.setMenuActionListener(this);
        this.mWidgetSearchbar.enter(getState(), animSet);
    }

    public void exit(WidgetState toState, AnimatorSet animSet) {
        if (!(toState.getState() == State.NORMAL || toState.getState() == State.SEARCH)) {
            onStageExit();
        }
        this.mWidgetSearchbar.exit(getState(), animSet);
        this.mWidgetSearchbar.setMenuActionListener(null);
        if (toState.getState() == State.UNINSTALL) {
            if (animSet != null) {
                Animator animator = AnimatorInflater.loadAnimator(this.mContext, R.animator.exit_widget_titlebar);
                animator.setTarget(this.mWidgetSearchbar);
                animSet.play(animator);
            }
            this.mWidgetSearchbar.setVisibility(View.GONE);
        }
    }

    public State getState() {
        return State.NORMAL;
    }

    public boolean onBackPressed() {
        return false;
    }

    public void initTitleBar(View titleBar) {
        super.initTitleBar(titleBar);
        if (titleBar == null) {
            throw new IllegalArgumentException(getState() + " titlebar is null");
        }
        this.mWidgetSearchbar = (WidgetSearchbar) titleBar;
    }

    protected void setHasInstallableApp(boolean has) {
        this.mWidgetSearchbar.setHasInstallableApp(has);
    }

    protected void changeColorForBg(boolean whitBg) {
        this.mWidgetSearchbar.changeColorAndBackground(whitBg);
    }

    protected void onStageEnter() {
        this.mWidgetSearchbar.onStageEnter();
        this.mWidgetSearchbar.setAlpha(1.0f);
    }

    protected void onStageExit() {
        this.mWidgetSearchbar.onStageExit();
    }

    public void startContactUs() {
        Utilities.startContactUsActivity(this.mContext);
    }

    public void changeStateToUninstall() {
        if (this.mActionListener != null) {
            this.mActionListener.notifyChangeState(State.UNINSTALL);
        }
    }

    public void onVoiceSearch(String query) {
        if (this.mActionListener != null) {
            this.mActionListener.notifyChangeState(State.SEARCH);
            this.mWidgetSearchbar.onVoiceSearch(query);
        }
    }

    public void setFocus() {
        this.mWidgetSearchbar.setFocus();
    }

    public void setFocusToSearchEditText() {
        this.mWidgetSearchbar.setFocusToSearchEditText();
    }

    protected boolean showPopupMenu() {
        return this.mWidgetSearchbar.showPopupMenu();
    }

    public void onConfigurationChangedIfNeeded() {
        this.mWidgetSearchbar.onConfigurationChangedIfNeeded();
    }
}

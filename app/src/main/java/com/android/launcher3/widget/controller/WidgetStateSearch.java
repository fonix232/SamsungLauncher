package com.android.launcher3.widget.controller;

import android.animation.AnimatorSet;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import com.android.launcher3.Utilities;
import com.android.launcher3.widget.controller.WidgetState.State;
import com.android.launcher3.widget.controller.WidgetState.StateActionListener;
import com.android.launcher3.widget.view.WidgetItemFolderView;
import com.android.launcher3.widget.view.WidgetItemSingleView;
import com.android.launcher3.widget.view.WidgetPagedView.Filter;
import com.android.launcher3.widget.view.WidgetSearchbar;
import com.android.launcher3.widget.view.WidgetSearchbar.MenuActionListener;
import com.android.launcher3.widget.view.WidgetSearchbar.SearchListener;

public class WidgetStateSearch extends WidgetState implements SearchListener, MenuActionListener {
    private WidgetSearchbar mWidgetSearchbar;

    public WidgetStateSearch(Context context, View titleBar) {
        super(context, titleBar);
    }

    public void onWidgetItemClick(View view) {
        if (view instanceof WidgetItemFolderView) {
            if (this.mActionListener != null) {
                this.mActionListener.openFolder(view, true);
            }
        } else if (view instanceof WidgetItemSingleView) {
            clickNotAllowed(view);
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
        if (fromState != null && fromState.getState() == State.UNINSTALL) {
            this.mWidgetSearchbar.setVisibility(View.VISIBLE);
        }
        this.mWidgetSearchbar.setSearchListener(this);
        this.mWidgetSearchbar.setMenuActionListener(this);
        this.mWidgetSearchbar.enter(getState(), animSet);
    }

    public void exit(WidgetState toState, AnimatorSet animSet) {
        if (toState.getState() == State.UNINSTALL) {
            this.mWidgetSearchbar.setVisibility(View.GONE);
        }
        if (!(toState.getState() == State.NORMAL || toState.getState() == State.SEARCH)) {
            onStageExit();
        }
        this.mWidgetSearchbar.exit(getState(), animSet);
        this.mWidgetSearchbar.setSearchListener(null);
        this.mWidgetSearchbar.setMenuActionListener(null);
    }

    protected void onStageEnter() {
        this.mWidgetSearchbar.onStageEnter();
    }

    protected void onStageExit() {
        this.mWidgetSearchbar.onStageExit();
    }

    public State getState() {
        return State.SEARCH;
    }

    public void initTitleBar(View titleBar) {
        super.initTitleBar(titleBar);
        if (titleBar == null) {
            throw new IllegalArgumentException(getState() + " titlebar is null");
        }
        this.mWidgetSearchbar = (WidgetSearchbar) titleBar;
    }

    public boolean onBackPressed() {
        boolean handled = this.mWidgetSearchbar.onBackPressed();
        if (handled && this.mActionListener != null) {
            this.mActionListener.notifyChangeState(State.NORMAL);
        }
        return handled;
    }

    public void openKeyBoard() {
        this.mWidgetSearchbar.openKeyboard();
    }

    public void setSearchFilter(Filter filter) {
        if (this.mActionListener instanceof StateActionListener) {
            ((StateActionListener) this.mActionListener).setSearchFilter(filter);
        }
    }

    public void applySearchResult(String searchString) {
        if (this.mActionListener instanceof StateActionListener) {
            ((StateActionListener) this.mActionListener).applySearchResult(searchString);
        }
    }

    public void startContactUs() {
        Utilities.startContactUsActivity(this.mContext);
    }

    public void changeStateToUninstall() {
        if (this.mActionListener != null) {
            this.mActionListener.notifyChangeState(State.UNINSTALL);
        }
    }

    public void setSearchString(String searchString) {
        if (this.mActionListener instanceof StateActionListener) {
            ((StateActionListener) this.mActionListener).setSearchString(searchString);
        }
    }

    public void onUpkeyPressed(View v) {
        onBackPressed();
    }

    public void onPagedViewTouchIntercepted() {
        this.mWidgetSearchbar.onPagedViewTouchIntercepted();
    }

    public void onVoiceSearch(String query) {
        this.mWidgetSearchbar.onVoiceSearch(query);
    }

    public void setFocus() {
        this.mWidgetSearchbar.setFocus();
    }

    public void setFocusToSearchEditText() {
        this.mWidgetSearchbar.setFocusToSearchEditText();
    }

    public void save(Bundle outState) {
        this.mWidgetSearchbar.save(outState);
    }

    protected void changeColorForBg(boolean whitBg) {
        this.mWidgetSearchbar.changeColorAndBackground(whitBg);
    }

    protected boolean showPopupMenu() {
        return this.mWidgetSearchbar.showPopupMenu();
    }
}

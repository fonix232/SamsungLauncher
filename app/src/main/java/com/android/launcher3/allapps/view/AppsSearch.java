package com.android.launcher3.allapps.view;

import android.animation.Animator;
import android.view.View;
import com.android.launcher3.allapps.AppsTransitionAnimation;
import com.android.launcher3.allapps.controller.AppsController;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.stage.StageEntry;
import com.sec.android.app.launcher.R;

public class AppsSearch {
    private final AppsSearchWrapper mSearchView;

    public AppsSearch(View parent, AppsController controller) {
        this.mSearchView = (AppsSearchWrapper) parent.findViewById(R.id.apps_searchbar_container);
        this.mSearchView.setController(controller);
        this.mSearchView.getContainerView().setVisibility(View.VISIBLE);
    }

    public void resume() {
        this.mSearchView.resume();
    }

    public View getContainerView() {
        return this.mSearchView.getContainerView();
    }

    public void setSearchBarVisibility(int visibility) {
        getAppsSearchBarView().setVisibility(visibility);
    }

    public void stageExit(StageEntry data) {
        this.mSearchView.stageExit(data);
    }

    public Animator switchInternalState(AppsTransitionAnimation appsAnim, StageEntry data) {
        return this.mSearchView.switchInternalState(appsAnim, data);
    }

    public void updateRecentApp(IconInfo item) {
        this.mSearchView.updateRecentApp(item);
    }

    public void onConfigurationChangedIfNeeded() {
        this.mSearchView.onConfigurationChangedIfNeeded();
    }

    public boolean showPopupMenu() {
        if (getContainerView().getVisibility() != 0) {
            return false;
        }
        this.mSearchView.showPopupMenu();
        return true;
    }

    public View getAppsSearchBarView() {
        return this.mSearchView.getAppsSearchBarView();
    }

    public boolean launchSfinder() {
        return this.mSearchView.launchSfinder("text_input");
    }
}

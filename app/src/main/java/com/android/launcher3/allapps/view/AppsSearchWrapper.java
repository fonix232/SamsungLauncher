package com.android.launcher3.allapps.view;

import android.animation.Animator;
import android.view.View;
import com.android.launcher3.allapps.AppsTransitionAnimation;
import com.android.launcher3.allapps.controller.AppsController;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.stage.StageEntry;

public interface AppsSearchWrapper {
    View getAppsSearchBarView();

    View getContainerView();

    void hidePopupMenu();

    boolean launchSfinder(String str);

    void onConfigurationChangedIfNeeded();

    void resume();

    void setController(AppsController appsController);

    void showPopupMenu();

    void stageExit(StageEntry stageEntry);

    Animator switchInternalState(AppsTransitionAnimation appsTransitionAnimation, StageEntry stageEntry);

    void updateRecentApp(IconInfo iconInfo);
}

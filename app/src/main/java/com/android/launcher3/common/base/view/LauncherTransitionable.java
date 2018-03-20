package com.android.launcher3.common.base.view;

import com.android.launcher3.Launcher;
import com.android.launcher3.common.stage.StageEntry;

public interface LauncherTransitionable {
    void onLauncherTransitionEnd(Launcher launcher, boolean z, boolean z2, StageEntry stageEntry);

    void onLauncherTransitionPrepare(Launcher launcher, boolean z, boolean z2, StageEntry stageEntry);

    void onLauncherTransitionStart(Launcher launcher, boolean z, boolean z2, StageEntry stageEntry);
}

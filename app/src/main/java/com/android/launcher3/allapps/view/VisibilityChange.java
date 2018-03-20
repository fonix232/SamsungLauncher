package com.android.launcher3.allapps.view;

/* compiled from: AppsContainer */
class VisibilityChange implements Runnable {
    private final AppsContainer mAppsContainer;
    private final int mVisible;

    public VisibilityChange(int mVisible, AppsContainer appsContainer) {
        this.mVisible = mVisible;
        this.mAppsContainer = appsContainer;
    }

    public void run() {
        this.mAppsContainer.setVisibility(this.mVisible);
    }
}

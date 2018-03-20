package com.android.launcher3.executor;

abstract class AbstractAppsStateHandler extends AbstractStateHandler {
    AbstractAppsStateHandler(ExecutorState stateId) {
        super(stateId);
    }

    public boolean isAllowedInHomeOnlyMode() {
        return false;
    }
}

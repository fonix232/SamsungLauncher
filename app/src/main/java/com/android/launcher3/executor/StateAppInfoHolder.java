package com.android.launcher3.executor;

enum StateAppInfoHolder {
    INSTANCE;
    
    private StateAppInfo mAppInfo;

    void setStateAppInfo(StateAppInfo appInfo) {
        this.mAppInfo = appInfo;
    }

    StateAppInfo getStateAppInfo() {
        return this.mAppInfo;
    }
}

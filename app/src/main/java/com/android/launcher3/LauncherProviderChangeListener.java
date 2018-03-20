package com.android.launcher3;

public interface LauncherProviderChangeListener {
    void onAppWidgetHostReset();

    void onSettingsChanged(String str, boolean z);

    void onZeroPageActiveChanged(boolean z);
}

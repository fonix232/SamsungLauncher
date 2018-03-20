package com.android.launcher3.common.compat;

import java.util.HashMap;

public class PackageInstallerCompatV16 extends PackageInstallerCompat {
    PackageInstallerCompatV16() {
    }

    public void onStop() {
    }

    public HashMap<String, Integer> updateAndGetActiveSessionCache() {
        return new HashMap();
    }

    public void addAllSessionInfoToCache() {
    }

    public boolean isSessionInfoItem(String packageName) {
        return false;
    }
}

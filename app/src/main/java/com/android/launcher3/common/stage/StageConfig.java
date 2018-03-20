package com.android.launcher3.common.stage;

import android.content.res.Configuration;
import com.android.launcher3.Launcher;
import com.android.launcher3.util.TestHelper;

class StageConfig {
    int mHeightDp;
    int mMobileKeyboard;
    int mOrientation;
    int mWidthDp;

    StageConfig(Launcher launcher) {
        this(launcher.getResources().getConfiguration());
    }

    StageConfig(Configuration config) {
        this.mOrientation = config.orientation;
        this.mMobileKeyboard = TestHelper.isRoboUnitTest() ? -1 : config.semMobileKeyboardCovered;
        this.mWidthDp = config.screenWidthDp;
        this.mHeightDp = config.screenHeightDp;
    }

    StageConfig(int orientation, int mobileKeyboard, int widthDp, int heightDp) {
        this.mOrientation = orientation;
        this.mMobileKeyboard = mobileKeyboard;
        this.mWidthDp = widthDp;
        this.mHeightDp = heightDp;
    }
}

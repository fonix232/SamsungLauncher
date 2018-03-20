package com.android.launcher3.common.stage;

import android.content.res.Configuration;
import com.android.launcher3.Launcher;
import com.android.launcher3.util.TestHelper;

class StageManagerConfigHelper {
    private int mActivityHeightDp;
    private int mActivityMobileKeyboard;
    private int mActivityOrientation;
    private int mActivityWidthDp;
    boolean mNeedToCallConfigurationChanged = false;

    StageManagerConfigHelper(Launcher activity) {
        setConfig(activity.getResources().getConfiguration());
    }

    void setConfig(Configuration config) {
        this.mActivityOrientation = config.orientation;
        this.mActivityMobileKeyboard = TestHelper.isRoboUnitTest() ? -1 : config.semMobileKeyboardCovered;
        this.mActivityWidthDp = config.screenWidthDp;
        this.mActivityHeightDp = config.screenHeightDp;
    }

    StageConfig makeStageConfigByHelper() {
        return new StageConfig(this.mActivityOrientation, this.mActivityMobileKeyboard, this.mActivityWidthDp, this.mActivityHeightDp);
    }

    boolean isConfigDifferentFromActivity(StageConfig stageConfig) {
        return (this.mActivityOrientation == stageConfig.mOrientation && this.mActivityMobileKeyboard == stageConfig.mMobileKeyboard && this.mActivityWidthDp == stageConfig.mWidthDp && this.mActivityHeightDp == stageConfig.mHeightDp) ? false : true;
    }

    boolean isOrientationChanged(Configuration config) {
        return this.mActivityOrientation != config.orientation;
    }
}

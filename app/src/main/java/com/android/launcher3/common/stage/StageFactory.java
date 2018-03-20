package com.android.launcher3.common.stage;

import com.android.launcher3.allapps.controller.AppsController;
import com.android.launcher3.appspicker.controller.AppsPickerController;
import com.android.launcher3.folder.controller.FolderController;
import com.android.launcher3.home.HomeController;
import com.android.launcher3.widget.controller.WidgetController;
import com.android.launcher3.widget.folder.WidgetFolderController;

public final class StageFactory {
    public static Stage buildStage(int mode) throws IllegalArgumentException {
        Stage s;
        switch (mode) {
            case 1:
                s = new HomeController();
                break;
            case 2:
                s = new AppsController();
                break;
            case 3:
                s = new WidgetController();
                break;
            case 4:
                s = new WidgetFolderController();
                break;
            case 5:
                s = new FolderController();
                break;
            case 6:
                s = new AppsPickerController();
                break;
            default:
                throw new IllegalArgumentException();
        }
        s.setMode(mode);
        return s;
    }
}

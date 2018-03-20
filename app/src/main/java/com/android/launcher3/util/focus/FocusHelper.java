package com.android.launcher3.util.focus;

import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.folder.folderlock.FolderLock;

public final class FocusHelper {
    public static CellLayoutChildren getCellLayoutChildrenForIndex(ViewGroup container, int i) {
        return ((CellLayout) container.getChildAt(i)).getCellLayoutChildren();
    }

    public static void playSoundEffect(int keyCode, View v) {
        switch (keyCode) {
            case 19:
            case 92:
            case FolderLock.REQUEST_CODE_FOLDER_LOCK /*122*/:
                v.playSoundEffect(2);
                return;
            case 20:
            case 93:
            case FolderLock.REQUEST_CODE_FOLDER_UNLOCK /*123*/:
                v.playSoundEffect(4);
                return;
            case 21:
                v.playSoundEffect(1);
                return;
            case 22:
                v.playSoundEffect(3);
                return;
            case 66:
                v.playSoundEffect(0);
                return;
            default:
                return;
        }
    }
}

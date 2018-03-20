package com.android.launcher3.common.drag;

import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.folder.view.FolderIconView;

public interface DragState {
    public static final int DRAG_MODE_ADD_TO_FOLDER = 2;
    public static final int DRAG_MODE_CREATE_FOLDER = 1;
    public static final int DRAG_MODE_FOLDER_OPENED = 4;
    public static final int DRAG_MODE_NONE = 0;
    public static final int DRAG_MODE_REORDER = 3;

    FolderIconView addFolder(CellLayout cellLayout, IconInfo iconInfo);

    boolean canOpenFolder();

    void commit(CellLayout cellLayout);

    void setDragMode(int i);
}

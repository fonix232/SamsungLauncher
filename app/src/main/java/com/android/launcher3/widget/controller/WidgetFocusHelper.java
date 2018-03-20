package com.android.launcher3.widget.controller;

import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import com.android.launcher3.Utilities;
import com.android.launcher3.folder.folderlock.FolderLock;

public class WidgetFocusHelper {
    public static final int CURRENT_PAGE = 2;
    public static final int NEXT_PAGE = 0;
    public static final int PREV_PAGE = 1;

    public interface ItemKeyEventListener {
        void focusToPage(int i, int i2);

        void focusToUp();

        int getColumnCount();

        int getRowCount();
    }

    public static class WidgetItemKeyListener implements OnKeyListener {
        private final ItemKeyEventListener mListener;

        public WidgetItemKeyListener(ItemKeyEventListener l) {
            this.mListener = l;
        }

        public boolean onKey(View v, int keyCode, KeyEvent event) {
            boolean handleKeyEvent;
            if (event.getAction() != 1) {
                handleKeyEvent = true;
            } else {
                handleKeyEvent = false;
            }
            int itemPos = ((ViewGroup) v.getParent()).indexOfChild(v);
            int childCount = ((ViewGroup) v.getParent()).getChildCount();
            int rowIndex = itemPos / this.mListener.getColumnCount();
            if (Utilities.sIsRtl) {
                if (keyCode == 21) {
                    keyCode = 22;
                } else if (keyCode == 22) {
                    keyCode = 21;
                }
            }
            switch (keyCode) {
                case 19:
                    if (handleKeyEvent && rowIndex == 0) {
                        this.mListener.focusToUp();
                        return true;
                    }
                case 21:
                    if (handleKeyEvent && itemPos == 0) {
                        this.mListener.focusToPage(1, keyCode);
                        return true;
                    }
                case 22:
                case 61:
                    if (handleKeyEvent && itemPos == childCount - 1) {
                        this.mListener.focusToPage(0, keyCode);
                        return true;
                    }
                case FolderLock.REQUEST_CODE_FOLDER_LOCK /*122*/:
                case FolderLock.REQUEST_CODE_FOLDER_UNLOCK /*123*/:
                    if (handleKeyEvent && itemPos != 0) {
                        this.mListener.focusToPage(2, keyCode);
                        return true;
                    }
            }
            return false;
        }
    }
}

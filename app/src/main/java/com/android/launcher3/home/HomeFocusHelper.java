package com.android.launcher3.home;

import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.LayoutParams;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.util.focus.FocusHelper;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;

class HomeFocusHelper {
    static final OnKeyListener ALIGN_BUTTON_KEY_LISTENER = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return HomeFocusHelper.handleAlignButtonKeyEvent(v, keyCode, event);
        }
    };
    private static final boolean DEBUG = false;
    static final OnKeyListener HOME_BUTTON_KEY_LISTENER = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return HomeFocusHelper.handleHomeButtonKeyEvent(v, keyCode, event);
        }
    };
    static final OnKeyListener HOTSEAT_ICON_KEY_LISTENER = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return HomeFocusHelper.handleHotseatButtonKeyEvent(v, keyCode, event);
        }
    };
    static final OnKeyListener OVERVIEW_PANEL_OPTION_BUTTON_KEY_LISTENER = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return HomeFocusHelper.handleOverviewPanelOptionButtonKeyEvent(v, keyCode, event);
        }
    };
    static final OnKeyListener PAGE_DELETE_BUTTON_KEY_LISTENER = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return HomeFocusHelper.handlePageDeleteButtonKeyEvent(v, keyCode, event);
        }
    };
    static final OnKeyListener SCREENGRID_PANEL_OPTION_BUTTON_KEY_LISTENER = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return HomeFocusHelper.handleScreenGridPanelOptionButtonKeyEvent(v, keyCode, event);
        }
    };
    static final OnKeyListener SCREENGRID_PANEL_TOP_BUTTON_KEY_LISTENER = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return HomeFocusHelper.handleScreenGridPanelTopButtonKeyEvent(v, keyCode, event);
        }
    };
    private static final String TAG = "HomeFocusHelper";
    static final OnKeyListener WORKSPACE_ICON_KEY_LISTENER = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return HomeFocusHelper.handleIconKeyEvent(v, keyCode, event);
        }
    };
    static final OnKeyListener ZERO_PAGE_SWITCH_KEY_LISTENER = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return HomeFocusHelper.handleZeroPageSwitchKeyEvent(v, keyCode, event);
        }
    };

    HomeFocusHelper() {
    }

    private static boolean handleHotseatButtonKeyEvent(View v, int keyCode, KeyEvent e) {
        boolean consume = HomeFocusLogic.shouldConsume(keyCode);
        if (e.getAction() != 1 && consume) {
            Launcher launcher = (Launcher) v.getContext();
            DeviceProfile profile = launcher.getDeviceProfile();
            CellLayoutChildren hotseatParent = (CellLayoutChildren) v.getParent();
            if (hotseatParent != null) {
                CellLayout hotseatLayout = (CellLayout) hotseatParent.getParent();
                ViewGroup workspace = (Workspace) v.getRootView().findViewById(R.id.workspace);
                int pageIndex = workspace.getNextPage();
                int pageCount = workspace.getChildCount();
                int countX = -1;
                int countY = -1;
                int iconIndex = hotseatParent.indexOfChild(v);
                CellLayout iconLayout = (CellLayout) workspace.getChildAt(pageIndex);
                if (iconLayout != null) {
                    CellLayoutChildren iconParent = iconLayout.getCellLayoutChildren();
                    if (iconParent != null) {
                        CellLayoutChildren parent = null;
                        int[][] matrix = null;
                        if (keyCode == 19 && !profile.isVerticalBarLayout()) {
                            matrix = HomeFocusLogic.createSparseMatrix(iconLayout, hotseatLayout, true);
                            iconIndex += iconParent.getChildCount();
                            countX = matrix.length;
                            countY = iconLayout.getCountY() + hotseatLayout.getCountY();
                            parent = iconParent;
                        } else if (keyCode == 21 && profile.isVerticalBarLayout()) {
                            matrix = HomeFocusLogic.createSparseMatrix(iconLayout, hotseatLayout, false);
                            iconIndex += iconParent.getChildCount();
                            countX = iconLayout.getCountX() + hotseatLayout.getCountX();
                            countY = iconLayout.getCountY();
                            parent = iconParent;
                        } else if (keyCode == 22 && profile.isVerticalBarLayout()) {
                            keyCode = 93;
                        } else if (keyCode == 112) {
                            launcher.getHomeController().removeHomeOrFolderItem((ItemInfo) v.getTag(), v);
                        } else {
                            matrix = HomeFocusLogic.createSparseMatrix(hotseatLayout);
                            countX = hotseatLayout.getCountX();
                            countY = hotseatLayout.getCountY();
                            parent = hotseatParent;
                        }
                        int newIconIndex = HomeFocusLogic.handleKeyEvent(keyCode, countX, countY, matrix, iconIndex, pageIndex, pageCount, parent);
                        View newIcon = null;
                        if (newIconIndex == -8) {
                            parent = FocusHelper.getCellLayoutChildrenForIndex(workspace, pageIndex + 1);
                            newIcon = parent.getChildAt(0);
                            workspace.snapToPage(pageIndex + 1);
                        } else if (newIconIndex == -4) {
                            parent = FocusHelper.getCellLayoutChildrenForIndex(workspace, pageIndex - 1);
                            newIcon = parent.getChildAt(parent.getChildCount() - 1);
                            workspace.snapToPage(pageIndex - 1);
                        } else if (newIconIndex == -11) {
                            workspace.snapToPage(-1);
                        } else if (newIconIndex == -3) {
                            parent = FocusHelper.getCellLayoutChildrenForIndex(workspace, pageIndex - 1);
                            newIcon = parent.getChildAt(0);
                            workspace.snapToPage(pageIndex - 1);
                        }
                        if (parent == iconParent && newIconIndex >= iconParent.getChildCount()) {
                            newIconIndex -= iconParent.getChildCount();
                        }
                        if (newIcon == null && newIconIndex >= 0) {
                            newIcon = parent.getChildAt(newIconIndex);
                        }
                        if (newIcon != null) {
                            newIcon.requestFocus();
                            FocusHelper.playSoundEffect(keyCode, v);
                        }
                    }
                }
            }
        }
        return consume;
    }

    private static boolean handleAlignButtonKeyEvent(View v, int keyCode, KeyEvent e) {
        boolean consume = HomeFocusLogic.shouldConsume(keyCode);
        if (e.getAction() != 1 && consume) {
            View alignButtonTop = (ImageView) getCustomButton(v, true, false, false);
            View alignButtonBottom = (ImageView) getCustomButton(v, false, false, false);
            View pageDeleteButton = getCustomButton(v, false, true, false);
            View homeButton = v.getRootView().findViewById(R.id.default_home_button);
            if (!(alignButtonTop == null || alignButtonBottom == null)) {
                View newIcon = null;
                switch (keyCode) {
                    case 19:
                        if (!v.equals(alignButtonBottom)) {
                            if (v.equals(alignButtonTop) && homeButton != null) {
                                newIcon = homeButton;
                                break;
                            }
                        } else if (!alignButtonTop.isEnabled()) {
                            if (pageDeleteButton == null || pageDeleteButton.getVisibility() != 0) {
                                if (homeButton != null) {
                                    newIcon = homeButton;
                                    break;
                                }
                            }
                            newIcon = pageDeleteButton;
                            break;
                        } else {
                            newIcon = alignButtonTop;
                            break;
                        }
                        break;
                    case 20:
                    case 22:
                    case 61:
                        LinearLayout overviewPanel;
                        if (!v.equals(alignButtonTop)) {
                            if (v.equals(alignButtonBottom)) {
                                overviewPanel = getOverviewPanel(v);
                                if (overviewPanel != null) {
                                    newIcon = overviewPanel.getChildAt(0);
                                    break;
                                }
                            }
                        } else if (pageDeleteButton == null || pageDeleteButton.getVisibility() != 0) {
                            if (!alignButtonBottom.isEnabled()) {
                                overviewPanel = getOverviewPanel(v);
                                if (overviewPanel != null) {
                                    newIcon = overviewPanel.getChildAt(0);
                                    break;
                                }
                            }
                            newIcon = alignButtonBottom;
                            break;
                        } else {
                            newIcon = pageDeleteButton;
                            break;
                        }
                        break;
                    case 66:
                        v.callOnClick();
                        FocusHelper.playSoundEffect(keyCode, v);
                        break;
                }
                if (newIcon != null) {
                    newIcon.requestFocus();
                    FocusHelper.playSoundEffect(keyCode, v);
                }
            }
        }
        return consume;
    }

    private static boolean handleZeroPageSwitchKeyEvent(View v, int keyCode, KeyEvent e) {
        boolean consume = HomeFocusLogic.shouldConsume(keyCode);
        if (e.getAction() != 1 && consume) {
            View newIcon = null;
            switch (keyCode) {
                case 19:
                case 21:
                    View homeButton = v.getRootView().findViewById(R.id.default_home_button);
                    if (homeButton != null) {
                        newIcon = homeButton;
                        break;
                    }
                    break;
                case 20:
                case 22:
                case 61:
                    LinearLayout overviewPanel = getOverviewPanel(v);
                    if (overviewPanel != null) {
                        newIcon = overviewPanel.getChildAt(0);
                        break;
                    }
                    break;
                case 66:
                    v.callOnClick();
                    FocusHelper.playSoundEffect(keyCode, v);
                    break;
            }
            if (newIcon != null) {
                newIcon.requestFocus();
                FocusHelper.playSoundEffect(keyCode, v);
            }
        }
        return consume;
    }

    private static boolean handleHomeButtonKeyEvent(View v, int keyCode, KeyEvent e) {
        boolean consume = HomeFocusLogic.shouldConsume(keyCode);
        if (e.getAction() != 1 && consume) {
            if (Utilities.sIsRtl) {
                if (keyCode == 21) {
                    keyCode = 22;
                } else if (keyCode == 22) {
                    keyCode = 21;
                }
            }
            View newIcon = null;
            switch (keyCode) {
                case 20:
                case 22:
                case 61:
                    View alignButtonTop = (ImageView) getCustomButton(v, true, false, false);
                    View alignButtonBottom = (ImageView) getCustomButton(v, false, false, false);
                    LinearLayout zeroPageSwitch = (LinearLayout) getCustomButton(v, false, false, true);
                    View pageDeleteButton = getCustomButton(v, false, true, false);
                    if (zeroPageSwitch == null) {
                        if (alignButtonTop == null || !alignButtonTop.isEnabled()) {
                            if (pageDeleteButton == null || pageDeleteButton.getVisibility() != 0) {
                                if (alignButtonBottom != null && alignButtonBottom.isEnabled()) {
                                    newIcon = alignButtonBottom;
                                    break;
                                }
                                LinearLayout overviewPanel = getOverviewPanel(v);
                                if (overviewPanel != null) {
                                    newIcon = overviewPanel.getChildAt(0);
                                    break;
                                }
                            }
                            newIcon = pageDeleteButton;
                            break;
                        }
                        newIcon = alignButtonTop;
                        break;
                    }
                    newIcon = zeroPageSwitch.getChildAt(0);
                    break;
                    break;
                case 66:
                    v.callOnClick();
                    FocusHelper.playSoundEffect(keyCode, v);
                    break;
            }
            if (newIcon != null) {
                newIcon.requestFocus();
                FocusHelper.playSoundEffect(keyCode, v);
            }
        }
        return consume;
    }

    private static boolean handleOverviewPanelOptionButtonKeyEvent(View v, int keyCode, KeyEvent e) {
        boolean consume = HomeFocusLogic.shouldConsume(keyCode);
        if (e.getAction() != 1 && consume) {
            LinearLayout overviewPanel = getOverviewPanel(v);
            if (overviewPanel != null) {
                if (Utilities.sIsRtl) {
                    if (keyCode == 21) {
                        keyCode = 22;
                    } else if (keyCode == 22) {
                        keyCode = 21;
                    }
                }
                int index = overviewPanel.indexOfChild(v);
                View newIcon = null;
                View childView;
                switch (keyCode) {
                    case 19:
                        View alignButtonTop = (ImageView) getCustomButton(v, true, false, false);
                        View alignButtonBottom = (ImageView) getCustomButton(v, false, false, false);
                        View zeroPageSwitch = (LinearLayout) getCustomButton(v, false, false, true);
                        View pageDeleteButton = getCustomButton(v, false, true, false);
                        if (zeroPageSwitch == null) {
                            if (alignButtonBottom == null || !alignButtonBottom.isEnabled()) {
                                if (pageDeleteButton == null || pageDeleteButton.getVisibility() != 0) {
                                    if (alignButtonTop != null && alignButtonTop.isEnabled()) {
                                        newIcon = alignButtonTop;
                                        break;
                                    }
                                    View homeButton = v.getRootView().findViewById(R.id.default_home_button);
                                    if (homeButton != null) {
                                        newIcon = homeButton;
                                        break;
                                    }
                                }
                                newIcon = pageDeleteButton;
                                break;
                            }
                            newIcon = alignButtonBottom;
                            break;
                        }
                        newIcon = zeroPageSwitch;
                        break;
                        break;
                    case 21:
                        if (index != 0) {
                            childView = overviewPanel.getChildAt(index - 1);
                            if (childView != null) {
                                childView.requestFocus();
                                childView.playSoundEffect(0);
                                break;
                            }
                        }
                        break;
                    case 22:
                        childView = overviewPanel.getChildAt(index + 1);
                        if (childView != null) {
                            childView.requestFocus();
                            childView.playSoundEffect(0);
                            break;
                        }
                        break;
                }
                if (newIcon != null) {
                    newIcon.requestFocus();
                    FocusHelper.playSoundEffect(keyCode, v);
                }
            }
        }
        return consume;
    }

    private static boolean handlePageDeleteButtonKeyEvent(View v, int keyCode, KeyEvent e) {
        boolean consume = HomeFocusLogic.shouldConsume(keyCode);
        if (e.getAction() != 1 && (consume || keyCode == 66)) {
            View homeButton = v.getRootView().findViewById(R.id.default_home_button);
            if (Utilities.sIsRtl) {
                if (keyCode == 21) {
                    keyCode = 22;
                } else if (keyCode == 22) {
                    keyCode = 21;
                }
            }
            View newIcon = null;
            switch (keyCode) {
                case 19:
                    if (homeButton != null) {
                        newIcon = homeButton;
                        break;
                    }
                    break;
                case 20:
                case 22:
                case 61:
                    View alingButtonBottom = (ImageView) getCustomButton(v, false, false, false);
                    LinearLayout overviewPanel = getOverviewPanel(v);
                    if (alingButtonBottom == null || !alingButtonBottom.isEnabled()) {
                        if (overviewPanel != null) {
                            newIcon = overviewPanel.getChildAt(0);
                            break;
                        }
                    }
                    newIcon = alingButtonBottom;
                    break;
                    break;
                case 21:
                    View alignButtonTop = (ImageView) getCustomButton(v, true, false, false);
                    if (alignButtonTop == null || !alignButtonTop.isEnabled()) {
                        if (homeButton != null) {
                            newIcon = homeButton;
                            break;
                        }
                    }
                    newIcon = alignButtonTop;
                    break;
                    break;
                case 66:
                    v.callOnClick();
                    FocusHelper.playSoundEffect(keyCode, v);
                    break;
            }
            if (newIcon != null) {
                newIcon.requestFocus();
                FocusHelper.playSoundEffect(keyCode, v);
            }
        }
        return consume;
    }

    private static boolean handleIconKeyEvent(View v, int keyCode, KeyEvent e) {
        boolean consume = HomeFocusLogic.shouldConsume(keyCode);
        if (e.getAction() != 1 && consume) {
            int[][] matrix;
            Launcher launcher = (Launcher) v.getContext();
            DeviceProfile profile = launcher.getDeviceProfile();
            CellLayoutChildren parent = (CellLayoutChildren) v.getParent();
            View iconLayout = (CellLayout) parent.getParent();
            ViewGroup workspace = (Workspace) iconLayout.getParent();
            Hotseat hotseat = (Hotseat) ((ViewGroup) workspace.getParent()).findViewById(R.id.hotseat);
            int iconIndex = parent.indexOfChild(v);
            int pageIndex = workspace.indexOfChild(iconLayout);
            int pageCount = workspace.getChildCount();
            int countX = iconLayout.getCountX();
            int countY = iconLayout.getCountY();
            CellLayout hotseatLayout = (CellLayout) hotseat.getChildAt(0);
            CellLayoutChildren hotseatParent = hotseatLayout.getCellLayoutChildren();
            if (keyCode == 20 && !profile.isVerticalBarLayout()) {
                matrix = HomeFocusLogic.createSparseMatrix((CellLayout) iconLayout, hotseatLayout, true);
                countY += hotseatLayout.getCountY();
            } else if (keyCode == 22 && profile.isVerticalBarLayout()) {
                matrix = HomeFocusLogic.createSparseMatrix((CellLayout) iconLayout, hotseatLayout, false);
                countX++;
            } else if (keyCode == 112) {
                launcher.getHomeController().removeHomeOrFolderItem((ItemInfo) v.getTag(), v);
            } else {
                matrix = HomeFocusLogic.createSparseMatrix(iconLayout);
            }
            int newIconIndex = HomeFocusLogic.handleKeyEvent(keyCode, countX, countY, matrix, iconIndex, pageIndex, pageCount, parent);
            View newIcon = null;
            int newPageIndex;
            int row;
            CellLayout iconLayout2;
            int i;
            switch (newIconIndex) {
                case HomeFocusLogic.PREVIOUS_ZERO_PAGE /*-11*/:
                    workspace.snapToPage(-1);
                    break;
                case HomeFocusLogic.NEXT_PAGE_RIGHT_COLUMN /*-10*/:
                case -2:
                    newPageIndex = pageIndex - 1;
                    if (newIconIndex == -10) {
                        newPageIndex = pageIndex + 1;
                    }
                    row = ((LayoutParams) v.getLayoutParams()).cellY;
                    parent = FocusHelper.getCellLayoutChildrenForIndex(workspace, newPageIndex);
                    if (parent != null) {
                        workspace.snapToPage(newPageIndex);
                        iconLayout2 = (CellLayout) parent.getParent();
                        i = keyCode;
                        newIcon = parent.getChildAt(HomeFocusLogic.handleKeyEvent(i, iconLayout2.getCountX() + 1, iconLayout2.getCountY(), HomeFocusLogic.createSparseMatrix(iconLayout2, iconLayout2.getCountX(), row), 100, newPageIndex, pageCount, parent));
                        if (newIcon == null) {
                            workspace.clearFocus();
                            break;
                        }
                    }
                    break;
                case HomeFocusLogic.NEXT_PAGE_LEFT_COLUMN /*-9*/:
                case -5:
                    newPageIndex = pageIndex + 1;
                    if (newIconIndex == -5) {
                        newPageIndex = pageIndex - 1;
                    }
                    row = ((LayoutParams) v.getLayoutParams()).cellY;
                    parent = FocusHelper.getCellLayoutChildrenForIndex(workspace, newPageIndex);
                    if (parent != null) {
                        workspace.snapToPage(newPageIndex);
                        iconLayout2 = (CellLayout) parent.getParent();
                        i = keyCode;
                        newIcon = parent.getChildAt(HomeFocusLogic.handleKeyEvent(i, iconLayout2.getCountX() + 1, iconLayout2.getCountY(), HomeFocusLogic.createSparseMatrix(iconLayout2, -1, row), 100, newPageIndex, pageCount, parent));
                        if (newIcon == null) {
                            workspace.clearFocus();
                            break;
                        }
                    }
                    break;
                case HomeFocusLogic.NEXT_PAGE_FIRST_ITEM /*-8*/:
                    newIcon = FocusHelper.getCellLayoutChildrenForIndex(workspace, pageIndex + 1).getChildAt(0);
                    workspace.snapToPage(pageIndex + 1);
                    break;
                case -7:
                    newIcon = parent.getChildAt(parent.getChildCount() - 1);
                    break;
                case -6:
                    newIcon = parent.getChildAt(0);
                    break;
                case -4:
                    parent = FocusHelper.getCellLayoutChildrenForIndex(workspace, pageIndex - 1);
                    newIcon = parent.getChildAt(parent.getChildCount() - 1);
                    workspace.snapToPage(pageIndex - 1);
                    break;
                case -3:
                    newIcon = FocusHelper.getCellLayoutChildrenForIndex(workspace, pageIndex - 1).getChildAt(0);
                    workspace.snapToPage(pageIndex - 1);
                    break;
                case -1:
                    break;
                default:
                    if (newIconIndex < 0 || newIconIndex >= parent.getChildCount()) {
                        if (parent.getChildCount() <= newIconIndex && newIconIndex < parent.getChildCount() + hotseatParent.getChildCount()) {
                            newIcon = hotseatParent.getChildAt(newIconIndex - parent.getChildCount());
                            break;
                        }
                    }
                    newIcon = parent.getChildAt(newIconIndex);
                    break;
                    break;
            }
            if (newIcon != null) {
                newIcon.requestFocus();
                FocusHelper.playSoundEffect(keyCode, v);
            }
        }
        return consume;
    }

    private static LinearLayout getOverviewPanel(View v) {
        HomeController homeController = ((Launcher) v.getContext()).getHomeController();
        if (homeController != null) {
            OverviewPanel overviewPanel = homeController.getOverviewPanel();
            if (overviewPanel != null) {
                return overviewPanel.getOverviewPanelLayout();
            }
        }
        return null;
    }

    private static View getCustomButton(View v, boolean alignTop, boolean deleteButton, boolean zeroPageSwitch) {
        HomeController homeController = ((Launcher) v.getContext()).getHomeController();
        if (!(homeController == null || homeController.getWorkspace() == null)) {
            if (deleteButton) {
                return homeController.getWorkspace().getPageDeleteBtn();
            }
            if (zeroPageSwitch) {
                return homeController.getWorkspace().getZeroPageSwitchLayout();
            }
            ArrayList<LinearLayout> alignLayoutList = homeController.getWorkspace().getAlignLayoutList();
            if (!alignLayoutList.isEmpty()) {
                LinearLayout alignLayoutTop = (LinearLayout) alignLayoutList.get(0);
                LinearLayout alignLayoutBottom = (LinearLayout) alignLayoutList.get(1);
                if (!(alignLayoutTop == null || alignLayoutBottom == null)) {
                    return alignTop ? alignLayoutTop.getChildAt(0) : alignLayoutBottom.getChildAt(0);
                }
            }
        }
        return null;
    }

    private static boolean handleScreenGridPanelTopButtonKeyEvent(View v, int keyCode, KeyEvent e) {
        boolean consume;
        if (keyCode == 20 || keyCode == 61 || keyCode == 21 || keyCode == 22) {
            consume = true;
        } else {
            consume = false;
        }
        if (e.getAction() != 1 && consume) {
            HomeController homeController = ((Launcher) v.getContext()).getHomeController();
            LinearLayout gridBtnLayout = null;
            LinearLayout screenGridTopContainer = null;
            if (!(homeController == null || homeController.getScreenGridPanel() == null)) {
                gridBtnLayout = homeController.getScreenGridPanel().getGriBtnLayout();
                screenGridTopContainer = (LinearLayout) homeController.getScreenGridPanel().getScreenGridTopConatiner();
            }
            LinearLayout screenGridTopLayout = (LinearLayout) screenGridTopContainer.findViewById(R.id.screen_grid_button_layout);
            if (!(gridBtnLayout == null || screenGridTopLayout == null)) {
                int index = screenGridTopLayout.indexOfChild(v);
                int childCount = screenGridTopLayout.getChildCount();
                View childView;
                switch (keyCode) {
                    case 20:
                    case 61:
                        if (gridBtnLayout != null) {
                            gridBtnLayout.getChildAt(0).requestFocus();
                            FocusHelper.playSoundEffect(keyCode, v);
                            break;
                        }
                        break;
                    case 21:
                        childView = screenGridTopLayout.getChildAt(Math.max(index - 1, 0));
                        if (childView != null && childView.isEnabled()) {
                            childView.requestFocus();
                            FocusHelper.playSoundEffect(keyCode, v);
                            break;
                        }
                    case 22:
                        childView = screenGridTopLayout.getChildAt(Math.min(index + 1, childCount - 1));
                        if (childView != null && childView.isEnabled()) {
                            childView.requestFocus();
                            FocusHelper.playSoundEffect(keyCode, v);
                            break;
                        }
                    default:
                        break;
                }
            }
        }
        return consume;
    }

    private static boolean handleScreenGridPanelOptionButtonKeyEvent(View v, int keyCode, KeyEvent e) {
        boolean consume;
        if (keyCode == 21 || keyCode == 22 || keyCode == 19) {
            consume = true;
        } else {
            consume = false;
        }
        if (e.getAction() != 1 && consume) {
            HomeController homeController = ((Launcher) v.getContext()).getHomeController();
            LinearLayout gridBtnLayout = null;
            LinearLayout screenGridTopContainer = null;
            if (!(homeController == null || homeController.getScreenGridPanel() == null)) {
                gridBtnLayout = homeController.getScreenGridPanel().getGriBtnLayout();
                screenGridTopContainer = (LinearLayout) homeController.getScreenGridPanel().getScreenGridTopConatiner();
            }
            if (!(gridBtnLayout == null || screenGridTopContainer == null)) {
                if (Utilities.sIsRtl) {
                    if (keyCode == 21) {
                        keyCode = 22;
                    } else if (keyCode == 22) {
                        keyCode = 21;
                    }
                }
                int index = gridBtnLayout.indexOfChild(v);
                View childView;
                switch (keyCode) {
                    case 19:
                        if (screenGridTopContainer != null) {
                            screenGridTopContainer.getChildAt(0).requestFocus();
                            FocusHelper.playSoundEffect(keyCode, v);
                            break;
                        }
                        break;
                    case 21:
                        if (index != 0) {
                            childView = gridBtnLayout.getChildAt(index - 1);
                            if (childView != null) {
                                childView.requestFocus();
                                childView.playSoundEffect(0);
                                break;
                            }
                        }
                        break;
                    case 22:
                        childView = gridBtnLayout.getChildAt(index + 1);
                        if (childView != null) {
                            childView.requestFocus();
                            childView.playSoundEffect(0);
                            break;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return consume;
    }
}

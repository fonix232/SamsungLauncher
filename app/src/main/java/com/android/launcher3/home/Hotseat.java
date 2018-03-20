package com.android.launcher3.home;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.FrameLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Stats.LaunchSourceProvider;
import com.android.launcher3.common.base.controller.ControllerBase;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.LayoutParams;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.deviceprofile.GridIconInfo;
import com.android.launcher3.common.drag.DragManager;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.view.FolderIconView;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Iterator;

public class Hotseat extends FrameLayout implements LaunchSourceProvider {
    private static final String TAG = "Launcher.Hotseat";
    private HotseatCellLayout mContent;
    private final boolean mHasVerticalHotseat;
    private HomeController mHomeController;
    private HotseatDragController mHsDragController;
    private Launcher mLauncher;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mLauncher = (Launcher) context;
        this.mHasVerticalHotseat = this.mLauncher.getDeviceProfile().isVerticalBarLayout();
        this.mHsDragController = new HotseatDragController(this.mLauncher, this);
    }

    void bindController(ControllerBase controller) {
        this.mHomeController = (HomeController) controller;
    }

    HotseatDragController getDragController() {
        return this.mHsDragController;
    }

    void setup(DragManager dragMgr) {
        this.mHsDragController.setup(this.mHomeController);
        dragMgr.addDragListener(this.mHsDragController);
        dragMgr.addDropTarget(this.mHsDragController);
    }

    public CellLayout getLayout() {
        return this.mContent;
    }

    boolean hasIcons() {
        return this.mContent.getCellLayoutChildren().getChildCount() > 1;
    }

    public void setOnLongClickListener(OnLongClickListener l) {
        this.mContent.setOnLongClickListener(l);
    }

    int getOrderInHotseat(int x, int y) {
        return this.mHasVerticalHotseat ? (this.mContent.getCountY() - y) - 1 : x;
    }

    int getCellXFromOrder(int rank) {
        return this.mHasVerticalHotseat ? 0 : rank;
    }

    int getCellYFromOrder(int rank) {
        return this.mHasVerticalHotseat ? this.mContent.getCountY() - (rank + 1) : 0;
    }

    boolean isVerticalHotseat() {
        return this.mHasVerticalHotseat;
    }

    protected void onConfigurationChangedIfNeeded() {
        this.mContent.setCellDimensions();
        this.mContent.updateIconViews();
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mContent = (HotseatCellLayout) findViewById(R.id.layout);
        this.mContent.setHotseat(this);
        this.mContent.setMaxCellCount(this.mLauncher.getDeviceProfile().getMaxHotseatCount());
    }

    void beginBind(int size) {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        if (dp.isVerticalBarLayout()) {
            this.mContent.setGridSize(1, size);
        } else {
            this.mContent.setGridSize(size, 1);
        }
        dp.setCurrentHotseatGridIcon(size);
    }

    void resetLayout() {
        this.mContent.removeAllViewsInLayout();
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mHomeController.isModalState() && !this.mHomeController.isSelectState()) {
            return true;
        }
        if (ev.getAction() == 0 || ev.getAction() == 2) {
            this.mHomeController.initBounceAnimation();
        }
        return false;
    }

    public void fillInLaunchSourceData(Bundle sourceData) {
        sourceData.putString("container", "hotseat");
    }

    void changeColorForBg(boolean whiteBg) {
        CellLayoutChildren children = (CellLayoutChildren) this.mContent.getChildAt(0);
        int childCount = children.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View v = children.getChildAt(i);
            if (v instanceof IconView) {
                ((IconView) v).changeTextColorForBg(whiteBg);
            }
        }
    }

    void updateCheckBox(boolean visible) {
        CellLayoutChildren children = (CellLayoutChildren) this.mContent.getChildAt(0);
        int childCount = children.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View v = children.getChildAt(i);
            if (v instanceof FolderIconView) {
                if (LauncherFeature.supportFolderSelect() || !visible) {
                    ((FolderIconView) v).updateCheckBox(visible);
                    ((FolderIconView) v).refreshCountBadge(0);
                }
                ((FolderIconView) v).refreshBadge();
            } else if (v instanceof IconView) {
                ((IconView) v).updateCheckBox(visible);
            }
        }
    }

    void changeGrid(boolean animated) {
        changeGrid(animated, false);
    }

    void changeGrid(boolean animated, boolean changeScreenGrid) {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        this.mContent.setCellDimensions();
        if (!animated) {
            if (dp.isVerticalBarLayout()) {
                this.mContent.setGridSize(1, this.mContent.getCountY());
            } else {
                this.mContent.setGridSize(this.mContent.getCountX(), 1);
            }
            this.mContent.markCellsAsOccupiedForAllChild();
            this.mContent.getCellLayoutChildren().requestLayout();
        }
        GridIconInfo prevGridIcon = dp.hotseatGridIcon;
        boolean result;
        if (dp.isVerticalBarLayout()) {
            result = dp.setCurrentHotseatGridIcon(this.mContent.getCountY());
        } else {
            result = dp.setCurrentHotseatGridIcon(this.mContent.getCountX());
        }
        if ((this.mContent.getPrevCountX() != this.mContent.getCountX() && result) || changeScreenGrid) {
            this.mContent.gridSizeChanged(prevGridIcon, animated);
        }
        if (changeScreenGrid) {
            this.mContent.updateFolderGrid();
        }
    }

    void dumpHotseatItem() {
        CellLayoutChildren clc = this.mContent.getCellLayoutChildren();
        int count = clc.getChildCount();
        for (int i = 0; i < count; i++) {
            IconView child = (IconView) clc.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            Log.d(TAG, "dumpHotseatItem itemInfo : " + ((ItemInfo) child.getTag()));
            Log.d(TAG, "dumpHotseatItem lp.x : " + lp.cellX + " lp.y : " + lp.cellY);
        }
    }

    public void setTargetView(View targetView) {
        this.mContent.setTargetView(targetView);
    }

    View getIconView(ComponentName cn, UserHandle user) {
        if (cn == null) {
            return null;
        }
        CellLayoutChildren clc = this.mContent.getCellLayoutChildren();
        for (int j = 0; j < clc.getChildCount(); j++) {
            View v = clc.getChildAt(j);
            IconInfo info;
            if (v.getTag() instanceof IconInfo) {
                info = (IconInfo) v.getTag();
                if (cn.equals(info.getTargetComponent()) && user.equals(info.getUserHandle().getUser())) {
                    return v;
                }
            } else if (v.getTag() instanceof FolderInfo) {
                Iterator it = ((FolderInfo) v.getTag()).contents.iterator();
                while (it.hasNext()) {
                    info = (IconInfo) it.next();
                    if (cn.equals(info.getTargetComponent()) && user.equals(info.getUserHandle().getUser())) {
                        return v;
                    }
                }
                continue;
            } else {
                continue;
            }
        }
        return null;
    }

    ArrayList<IconView> getIconList() {
        CellLayoutChildren clc = this.mContent.getCellLayoutChildren();
        int count = clc.getChildCount();
        ArrayList<IconView> allItems = new ArrayList();
        for (int i = 0; i < count; i++) {
            View child = clc.getChildAt(i);
            if (child != null && (child instanceof IconView)) {
                allItems.add((IconView) child);
            }
        }
        return allItems;
    }
}

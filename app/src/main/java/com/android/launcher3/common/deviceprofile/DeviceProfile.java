package com.android.launcher3.common.deviceprofile;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Rect;
import android.support.v4.view.GravityCompat;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout.LayoutParams;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.util.ScreenGridUtilities;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Iterator;

public class DeviceProfile {
    private static final String GRID_INFO_SPLIT = "\\|";
    private static final String TAG = "Launcher.DeviceProfile";
    private static final int TEXT_SIZE_DELTA = -2;
    public int appsExtraPaddingTop;
    public GridInfo appsGrid;
    private ArrayList<GridInfo> appsGridInfo = new ArrayList();
    public int availableHeightPx;
    public int availableWidthPx;
    public final int defaultCellHeight;
    public final int defaultCellWidth;
    private GridIconInfo defaultHotseatGridIcon;
    public final int defaultIconSize;
    public GridInfo folderGrid;
    private ArrayList<GridInfo> folderGridInfo = new ArrayList();
    public final int gedHomeCellCountX;
    public final int gedHomeCellCountY;
    public final int gedHomeCellHeight;
    public final int gedHomeCellWidth;
    public final int heightPx;
    public GridInfo homeGrid;
    private ArrayList<GridInfo> homeGridInfo = new ArrayList();
    public GridInfo homeMaxGrid;
    public GridIconInfo hotseatGridIcon;
    private ArrayList<GridIconInfo> hotseatGridIconInfo = new ArrayList();
    public final boolean hotseatRightLayoutWithOrientation;
    public final boolean isLandscape;
    public final boolean isMultiwindowMode;
    private Context mContext;
    private int mHotseatCellWidthSize;
    private int mMultiWindowPanelSize;
    private final int maxHotseatCount;
    public int navigationBarHeight;
    public final int widthPx;
    private final int workspacePagePadding;

    public DeviceProfile(Context context, int availableWidth, int availableHeight, int width, int height, boolean isLandscape, boolean isMultiwindowMode) {
        int cellX;
        int cellY;
        this.isLandscape = isLandscape;
        this.isMultiwindowMode = isMultiwindowMode;
        Resources res = context.getResources();
        this.mContext = context;
        this.widthPx = width;
        this.heightPx = height;
        this.availableWidthPx = availableWidth;
        this.availableHeightPx = availableHeight;
        this.defaultIconSize = res.getDimensionPixelSize(R.dimen.app_icon_size);
        int i = (!isLandscape || LauncherFeature.isTablet()) ? this.heightPx - availableHeight : this.widthPx - availableWidth;
        this.navigationBarHeight = i;
        if (Utilities.isMobileKeyboardMode()) {
            this.navigationBarHeight = 0;
        } else if (LauncherFeature.supportNavigationBar()) {
            int resourceId;
            boolean isLandNavigation = isLandscape && !LauncherFeature.isTablet();
            if (isLandNavigation) {
                resourceId = res.getIdentifier("navigation_bar_width", "dimen", "android");
            } else {
                resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android");
            }
            if (resourceId > 0) {
                this.navigationBarHeight = res.getDimensionPixelSize(resourceId);
                if (isLandNavigation) {
                    this.availableWidthPx = this.widthPx - this.navigationBarHeight;
                } else {
                    this.availableHeightPx = this.heightPx - this.navigationBarHeight;
                }
                Log.i(TAG, "successed to get navigation_bar_height: " + this.navigationBarHeight);
            } else {
                Log.i(TAG, "failed to get navigation_bar_height: " + this.navigationBarHeight);
            }
        }
        this.gedHomeCellWidth = res.getDimensionPixelSize(R.dimen.ged_home_cellwidth);
        this.gedHomeCellHeight = res.getDimensionPixelSize(R.dimen.ged_home_cellheight);
        this.gedHomeCellCountX = res.getInteger(R.integer.ged_home_cellcount_x);
        this.gedHomeCellCountY = res.getInteger(R.integer.ged_home_cellcount_y);
        if (LauncherFeature.isTabAOSupProject()) {
            this.defaultCellWidth = res.getDimensionPixelSize(R.dimen.home_6x6_cellWidth_port);
            this.defaultCellHeight = res.getDimensionPixelSize(R.dimen.home_6x6_cellHeight_port);
            cellX = 6;
            cellY = 6;
        } else {
            this.defaultCellWidth = res.getDimensionPixelSize(R.dimen.home_default_cellWidth_port);
            this.defaultCellHeight = res.getDimensionPixelSize(R.dimen.home_default_cellHeight_port);
            cellX = res.getInteger(R.integer.home_cellCountX);
            cellY = res.getInteger(R.integer.home_cellCountY);
        }
        this.workspacePagePadding = res.getDimensionPixelSize(R.dimen.home_workspace_page_padding);
        this.hotseatRightLayoutWithOrientation = res.getBoolean(R.bool.hotseat_transpose_layout_with_orientation);
        this.appsExtraPaddingTop = res.getDimensionPixelOffset(R.dimen.apps_celllayout_extra_padding_top);
        loadGridInfo(context, this.appsGridInfo, R.array.apps_grid_info);
        setAppsGridInfoTextSizeDelta();
        loadGridInfo(context, this.folderGridInfo, R.array.folder_grid_info);
        loadGridInfo(context, this.homeGridInfo, R.array.home_grid_info);
        loadHotseatGridInfo(context, this.hotseatGridIconInfo, R.array.hotseat_grid_icon_info);
        this.maxHotseatCount = Math.max(res.getInteger(R.integer.hotseat_cellCount), getMaxCellCountX());
        setCurrentGrid(cellX, cellY);
        loadHomeMaxGridInfo();
        int[] appsGridXY = new int[2];
        ScreenGridUtilities.loadCurrentAppsGridSize(context, appsGridXY);
        if (appsGridXY[0] <= 0 || appsGridXY[1] <= 0) {
            appsGridXY[0] = context.getResources().getInteger(R.integer.apps_default_cellCountX);
            appsGridXY[1] = context.getResources().getInteger(R.integer.apps_default_cellCountY);
        }
        setAppsCurrentGrid(appsGridXY[0], appsGridXY[1]);
        this.mMultiWindowPanelSize = res.getDimensionPixelOffset(R.dimen.resizable_launcher_padding_top);
    }

    private void loadGridInfo(Context context, ArrayList<GridInfo> gridArray, int resId) {
        gridArray.clear();
        String[] gridInfo = context.getResources().getStringArray(resId);
        if (gridInfo != null) {
            for (String info : gridInfo) {
                String[] infoSplit = info.split(GRID_INFO_SPLIT);
                if (infoSplit != null) {
                    gridArray.add(new GridInfo(context, infoSplit));
                }
            }
        }
    }

    private void loadHomeMaxGridInfo() {
        int size = this.homeGridInfo.size();
        this.homeMaxGrid = this.homeGrid;
        for (int index = 0; index < size; index++) {
            GridInfo gridInfo = (GridInfo) this.homeGridInfo.get(index);
            if (gridInfo != null && gridInfo.getCellCountY() >= this.homeMaxGrid.getCellCountY()) {
                this.homeMaxGrid = gridInfo;
            }
        }
    }

    private void loadHotseatGridInfo(Context context, ArrayList<GridIconInfo> gridArray, int resId) {
        gridArray.clear();
        String[] gridInfo = context.getResources().getStringArray(resId);
        if (gridInfo != null) {
            for (String info : gridInfo) {
                String[] infoSplit = info.split(GRID_INFO_SPLIT);
                if (infoSplit != null) {
                    gridArray.add(new GridIconInfo(context, infoSplit));
                }
            }
        }
    }

    public ArrayList<GridInfo> getAppsGridInfo() {
        return this.appsGridInfo;
    }

    public void setCurrentGrid(int cellX, int cellY) {
        Log.i(TAG, "setCurrentGrid cellX : " + cellX + ", cellY : " + cellY);
        int size = this.homeGridInfo.size();
        int index = 0;
        while (index < size) {
            GridInfo grid = (GridInfo) this.homeGridInfo.get(index);
            if (grid != null && grid.getCellCountX() == cellX && grid.getCellCountY() == cellY) {
                this.homeGrid = grid;
                break;
            }
            index++;
        }
        if (index >= size) {
            this.homeGrid = (GridInfo) this.homeGridInfo.get(0);
            this.folderGrid = (GridInfo) this.folderGridInfo.get(0);
            this.defaultHotseatGridIcon = (GridIconInfo) this.hotseatGridIconInfo.get(0);
            Log.i(TAG, "There is no grid info to match");
        } else {
            GridInfo gridInfo;
            GridIconInfo gridIconInfo;
            if (index < this.folderGridInfo.size()) {
                gridInfo = (GridInfo) this.folderGridInfo.get(index);
            } else {
                gridInfo = (GridInfo) this.folderGridInfo.get(this.folderGridInfo.size() - 1);
            }
            this.folderGrid = gridInfo;
            if (index < this.hotseatGridIconInfo.size()) {
                gridIconInfo = (GridIconInfo) this.hotseatGridIconInfo.get(index);
            } else {
                gridIconInfo = (GridIconInfo) this.hotseatGridIconInfo.get(0);
            }
            this.defaultHotseatGridIcon = gridIconInfo;
        }
        if (this.hotseatGridIcon == null) {
            this.hotseatGridIcon = this.defaultHotseatGridIcon;
        }
        if (this.homeGrid.getCellWidth() == 0 || this.homeGrid.getCellHeight() == 0) {
            calculateCellSize(this.availableWidthPx, this.availableHeightPx, getWorkspacePageTotalPadding(), this.homeGrid);
            calculateContentTop(this.homeGrid.getIconInfo(), this.homeGrid.getCellHeight());
        }
        if (this.folderGrid.getCellWidth() == 0 || this.folderGrid.getCellHeight() == 0) {
            calculateCellSize(this.availableWidthPx, this.availableHeightPx, getFolderPadding(), this.folderGrid);
            if (calculateIconContentHeight(this.folderGrid.getIconInfo()) > this.folderGrid.getCellHeight()) {
                Log.w(TAG, String.format("folderCellHeight(%d) is less than contentHeight(%d)", new Object[]{Integer.valueOf(this.folderGrid.getCellHeight()), Integer.valueOf(contentHeight)}));
            }
            calculateContentTop(this.folderGrid.getIconInfo(), this.folderGrid.getCellHeight());
        }
        this.defaultHotseatGridIcon.setContentTop(this.homeGrid.getHotseatContentTop());
        if (this.defaultHotseatGridIcon.getContentTop() < 0) {
            calculateContentTop(this.defaultHotseatGridIcon, this.homeGrid.getHotseatBarSize());
        }
    }

    public void setAppsCurrentGrid(int cellX, int cellY) {
        Log.i(TAG, "setAppsCurrentGrid cellX : " + cellX + ", cellY : " + cellY);
        int size = this.appsGridInfo.size();
        int index = 0;
        while (index < size) {
            GridInfo grid = (GridInfo) this.appsGridInfo.get(index);
            if (grid != null && grid.getCellCountX() == cellX && grid.getCellCountY() == cellY) {
                this.appsGrid = grid;
                break;
            }
            index++;
        }
        if (index >= size) {
            this.appsGrid = (GridInfo) this.appsGridInfo.get(0);
        } else {
            this.appsGrid = index < this.appsGridInfo.size() ? (GridInfo) this.appsGridInfo.get(index) : (GridInfo) this.appsGridInfo.get(0);
        }
        Log.d(TAG, "availableWidthPx : " + this.availableWidthPx + " , " + this.availableHeightPx);
        if (this.appsGrid.getCellWidth() == 0 || this.appsGrid.getCellHeight() == 0) {
            calculateCellSize(this.availableWidthPx, this.availableHeightPx, getAppsPadding(), this.appsGrid);
            calculateContentTop(this.appsGrid.getIconInfo(), this.appsGrid.getCellHeight());
        }
    }

    private void setAppsGridInfoTextSizeDelta() {
        Iterator it = this.appsGridInfo.iterator();
        while (it.hasNext()) {
            GridInfo gridInfo = (GridInfo) it.next();
            if (!(gridInfo == null || gridInfo.getIconInfo() == null || !checkTextSizeDelta(this.mContext, gridInfo.getCellCountX(), gridInfo.getCellCountY()))) {
                gridInfo.getIconInfo().setTextSize(gridInfo.getTextSize() - 2);
            }
        }
    }

    private boolean checkTextSizeDelta(Context context, int countX, int countY) {
        return countX == 5 && countY == 6 && "it".equals(Utilities.getLocale(context).getLanguage());
    }

    public boolean setCurrentHotseatGridIcon(int count) {
        boolean result = false;
        if (this.defaultHotseatGridIcon.getMaxCount() < count) {
            int size = this.hotseatGridIconInfo.size();
            int index = 0;
            while (index < size) {
                GridIconInfo iconInfo = (GridIconInfo) this.hotseatGridIconInfo.get(index);
                if (iconInfo.getMaxCount() >= count) {
                    if (this.hotseatGridIcon == null || this.hotseatGridIcon.getIconSize() != iconInfo.getIconSize()) {
                        result = true;
                    }
                    this.hotseatGridIcon = iconInfo;
                    this.hotseatGridIcon.setContentTop(this.homeGrid.getHotseatContentTop());
                    if (this.hotseatGridIcon.getContentTop() < 0) {
                        calculateContentTop(this.hotseatGridIcon, this.homeGrid.getHotseatBarSize());
                    }
                } else {
                    index++;
                }
            }
            this.hotseatGridIcon.setContentTop(this.homeGrid.getHotseatContentTop());
            if (this.hotseatGridIcon.getContentTop() < 0) {
                calculateContentTop(this.hotseatGridIcon, this.homeGrid.getHotseatBarSize());
            }
        } else if (this.hotseatGridIcon != this.defaultHotseatGridIcon) {
            if (this.hotseatGridIcon == null || this.hotseatGridIcon.getIconSize() != this.defaultHotseatGridIcon.getIconSize()) {
                result = true;
            }
            this.hotseatGridIcon = this.defaultHotseatGridIcon;
        }
        return result;
    }

    private void calculateCellSize(int widthPx, int heightPx, Rect pagePadding, GridInfo grid) {
        int width = (widthPx - pagePadding.left) - pagePadding.right;
        int height = (heightPx - pagePadding.top) - pagePadding.bottom;
        if (this.isMultiwindowMode && LauncherFeature.supportNavigationBar()) {
            if (this.isLandscape) {
                width -= this.mContext.getResources().getDimensionPixelOffset(R.dimen.resizable_launcher_padding_top);
            } else {
                height -= this.mContext.getResources().getDimensionPixelOffset(R.dimen.resizable_launcher_padding_top);
            }
        }
        int cellWidth = calculateCellWidthOrHeight(width, grid.getCellGapX(), grid.getCellCountX());
        int cellHeight = calculateCellWidthOrHeight(height, grid.getCellGapY(), grid.getCellCountY());
        grid.setCellWidth(cellWidth);
        grid.setCellHeight(cellHeight);
    }

    private void calculateContentTop(GridIconInfo iconInfo, int height) {
        int contentTop = (height - calculateIconContentHeight(iconInfo)) / 2;
        if (contentTop < 0) {
            contentTop = 0;
        }
        iconInfo.setContentTop(contentTop);
    }

    private int calculateIconContentHeight(GridIconInfo iconInfo) {
        if (this.isLandscape && !LauncherFeature.isTablet()) {
            return iconInfo.getIconSize();
        }
        Paint textPaint = new Paint();
        textPaint.setTextSize((float) iconInfo.getTextSize());
        FontMetrics fm = textPaint.getFontMetrics();
        return (int) (((float) (iconInfo.getIconSize() + iconInfo.getDrawablePadding())) + (((float) iconInfo.getLineCount()) * ((fm.bottom - fm.top) + fm.leading)));
    }

    public void layoutGrid(Launcher launcher) {
        layoutHomeGrid(launcher);
        if (!LauncherFeature.supportHomeModeChange() || !LauncherAppState.getInstanceNoCreate().isHomeOnlyModeEnabled()) {
            layoutAppsGrid(launcher);
        }
    }

    private void layoutHomeGrid(Launcher launcher) {
        PagedView workspace = (PagedView) launcher.findViewById(R.id.workspace);
        Rect padding = getWorkspacePadding();
        if (workspace != null) {
            LayoutParams lp = (LayoutParams) workspace.getLayoutParams();
            lp.gravity = 17;
            workspace.setLayoutParams(lp);
            workspace.setPadding(padding.left, padding.top, padding.right, padding.bottom);
        }
        View hotseat = launcher.findViewById(R.id.hotseat);
        if (hotseat != null) {
            padding = getWorkspacePageTotalPadding();
            lp = (LayoutParams) hotseat.getLayoutParams();
            if (isVerticalBarLayout()) {
                lp.gravity = GravityCompat.END;
                lp.height = -1;
                lp.width = this.homeGrid.getHotseatBarSize();
                hotseat.setPadding(0, padding.top, 0, padding.bottom);
            } else {
                lp.gravity = 80;
                lp.width = -1;
                lp.height = this.homeGrid.getHotseatBarSize();
                lp.bottomMargin = this.homeGrid.getHotseatBottom();
                hotseat.setPadding(padding.left, 0, padding.right, 0);
            }
            hotseat.setLayoutParams(lp);
        }
        View pageIndicator = launcher.findViewById(R.id.home_page_indicator);
        if (pageIndicator != null) {
            if (isVerticalBarLayout()) {
                pageIndicator.setVisibility(View.GONE);
            } else {
                if (launcher.isHomeStage()) {
                    pageIndicator.setVisibility(View.VISIBLE);
                }
                lp = (LayoutParams) pageIndicator.getLayoutParams();
                lp.bottomMargin = this.homeGrid.getIndicatorBottom();
                if (LauncherFeature.supportNavigationBar() && !this.isLandscape) {
                    lp.bottomMargin += this.navigationBarHeight;
                }
            }
        }
        View swipeAffordance = launcher.findViewById(R.id.swipe_affordance);
        if (swipeAffordance == null) {
            return;
        }
        if (isVerticalBarLayout()) {
            swipeAffordance.setVisibility(View.GONE);
            return;
        }
        swipeAffordance.setVisibility(View.VISIBLE);
        lp = (LayoutParams) swipeAffordance.getLayoutParams();
        int bottomMargin = this.homeGrid.getHotseatBarSize() + this.homeGrid.getHotseatBottom();
        lp.bottomMargin = bottomMargin;
        lp.height = getWorkspacePageTotalPadding().bottom - bottomMargin;
    }

    private void layoutAppsGrid(Launcher launcher) {
        PagedView appsPageView = (PagedView) launcher.findViewById(R.id.apps_content);
        Rect padding = getAppsPadding();
        if (appsPageView != null) {
            LayoutParams lp = (LayoutParams) appsPageView.getLayoutParams();
            lp.gravity = 17;
            appsPageView.setLayoutParams(lp);
            appsPageView.setPadding(padding.left, padding.top - this.appsExtraPaddingTop, padding.right, padding.bottom);
        }
        View pageIndicator = launcher.findViewById(R.id.apps_page_indicator);
        if (pageIndicator != null) {
            lp = (LayoutParams) pageIndicator.getLayoutParams();
            lp.bottomMargin = this.appsGrid.getIndicatorBottom();
            if (LauncherFeature.supportNavigationBar() && !this.isLandscape) {
                lp.bottomMargin += this.navigationBarHeight;
            }
        }
    }

    private Rect getWorkspacePadding() {
        return getWorkspacePadding(this.homeGrid);
    }

    public Rect getWorkspacePadding(GridInfo homeGrid) {
        Rect workspacePadding = new Rect();
        Rect padding = new Rect();
        workspacePadding.top = homeGrid.getPageTop() - this.workspacePagePadding;
        workspacePadding.bottom = homeGrid.getPageBottom() - this.workspacePagePadding;
        int pagePadding = homeGrid.getPagePadding() - this.workspacePagePadding;
        workspacePadding.right = pagePadding;
        workspacePadding.left = pagePadding;
        if (!isVerticalBarLayout()) {
            padding.set(workspacePadding.left, workspacePadding.top, workspacePadding.right, workspacePadding.bottom);
        } else if (Utilities.sIsRtl) {
            padding.set(homeGrid.getHotseatBarSize(), workspacePadding.top, workspacePadding.right, 0);
        } else {
            padding.set(workspacePadding.left, workspacePadding.top, homeGrid.getHotseatBarSize(), 0);
        }
        return padding;
    }

    public Rect getWorkspacePageTotalPadding() {
        Rect padding = getWorkspacePadding();
        padding.set(padding.left + this.workspacePagePadding, padding.top + this.workspacePagePadding, padding.right + this.workspacePagePadding, padding.bottom + this.workspacePagePadding);
        return padding;
    }

    private Rect getAppsPadding() {
        Rect padding = new Rect();
        padding.top = this.appsGrid.getPageTop();
        padding.bottom = this.appsGrid.getPageBottom();
        int pagePadding = this.appsGrid.getPagePadding();
        padding.right = pagePadding;
        padding.left = pagePadding;
        return padding;
    }

    private Rect getFolderPadding() {
        Rect padding = new Rect();
        padding.top = this.folderGrid.getPageTop();
        padding.bottom = this.folderGrid.getPageBottom();
        int pagePadding = this.folderGrid.getPagePadding();
        padding.right = pagePadding;
        padding.left = pagePadding;
        return padding;
    }

    public static int calculateCellWidthOrHeight(int length, int gap, int countCell) {
        return countCell <= 0 ? length : ((length + gap) / countCell) - gap;
    }

    public boolean isVerticalBarLayout() {
        return this.isLandscape && this.hotseatRightLayoutWithOrientation;
    }

    public static Rect getPaddingForWidget() {
        return new Rect(0, 0, 0, 0);
    }

    public int getAvailableWidthPx() {
        return this.availableWidthPx;
    }

    public int getOffsetIndicator() {
        return (((GridInfo) this.homeGridInfo.get(0)).getPageBottom() - this.homeGrid.getPageBottom()) / 2;
    }

    public int getOffsetIndicatorForScreenGrid() {
        return (((GridInfo) this.homeGridInfo.get(0)).getPageBottom() - this.homeMaxGrid.getPageBottom()) / 2;
    }

    private int getMaxCellCountX() {
        int maxCount = 0;
        if (this.homeGridInfo != null) {
            Iterator it = this.homeGridInfo.iterator();
            while (it.hasNext()) {
                maxCount = Math.max(maxCount, ((GridInfo) it.next()).getCellCountX());
            }
        }
        return maxCount;
    }

    public int getMaxHotseatCount() {
        return this.maxHotseatCount;
    }

    public int getMultiWindowPanelSize() {
        if (this.isMultiwindowMode) {
            return this.mMultiWindowPanelSize;
        }
        return 0;
    }

    public void setHotseatCellWidthSize(int hotseatCellWidthSize) {
        this.mHotseatCellWidthSize = hotseatCellWidthSize;
    }

    public int getHotseatCellWidthSize() {
        return this.mHotseatCellWidthSize;
    }
}

package com.android.launcher3.allapps.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.AppsBaseListener;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.item.ItemOperator;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.LayoutParams;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.common.base.view.Insettable;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.deviceprofile.GridInfo;
import com.android.launcher3.common.drag.DragManager;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.common.view.LiveIconManager;
import com.android.launcher3.common.view.PageIndicator.PageMarkerResources;
import com.android.launcher3.common.view.PageIndicator.PageMarkerResources.IndicatorType;
import com.android.launcher3.common.view.Removable;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.util.DvfsUtil;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import com.samsung.android.feature.SemGateConfig;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Iterator;

public class AppsPagedView extends PagedView implements Insettable {
    private static final String EXTRA_EMPTY_SCREEN = "extra_empty_screen";
    private static final int REORDER_ANIMATION_DURATION = 150;
    private static final int SNAP_OFF_EMPTY_SCREEN_DURATION = 400;
    private static final String TAG = "Launcher.AppsPagedView";
    private final ArrayList<CellLayout> mCellLayouts = new ArrayList();
    private DragManager mDragMgr;
    private final Launcher mLauncher;
    private Listener mListener;
    private int mOldPageCount = 0;
    private Runnable mRemoveEmptyScreenRunnable;

    public interface Listener extends AppsBaseListener {
        boolean deferToBind();

        void exitDragStateDelayed();

        int getState();

        void initBounceAnimation();

        boolean isAlphabeticalMode();

        boolean isSelectState();

        boolean isSwitchingGridToNormal();

        boolean isSwitchingInternalState();

        boolean isSwitchingState();

        void requestRunDeferredRunnable();
    }

    public AppsPagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mLauncher = (Launcher) context;
        if (getImportantForAccessibility() == 0) {
            setImportantForAccessibility(1);
        }
        setMinScale(1.0f);
        Resources res = getResources();
        int appsPagePadding = this.mLauncher.getDeviceProfile().appsGrid.getPagePadding();
        this.mPageSpacing = appsPagePadding * 2;
        setHintPageZone(res, appsPagePadding);
    }

    protected void init() {
        super.init();
        this.mCenterPagesVertically = false;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
        if (this.mListener == null) {
            throw new IllegalStateException("listener is null");
        }
    }

    public void setup(DragManager dragMgr) {
        this.mDragMgr = dragMgr;
    }

    private void updatePageAlphaValues(int screenCenter, int leftScreen, int rightScreen) {
        if (this.mListener.getState() != 0) {
            updateDragPageAlphaValues(screenCenter, leftScreen, rightScreen);
        }
    }

    private void updateDragPageAlphaValues(int screenCenter, int leftScreen, int rightScreen) {
        if (!this.mListener.isSwitchingState() && this.mListener.getState() != 5) {
            for (int i = leftScreen; i <= rightScreen; i++) {
                CellLayout child = (CellLayout) getChildAt(i);
                if (child != null) {
                    child.setBackgroundAlpha(Math.min(1.0f, Math.abs(getScrollProgress(screenCenter, child, i))));
                }
            }
        }
    }

    public boolean hasExtraEmptyScreen() {
        if (this.mCellLayouts.size() < 2) {
            return false;
        }
        Object tag = ((CellLayout) this.mCellLayouts.get(this.mCellLayouts.size() - 1)).getTag();
        boolean z = tag != null && tag.equals(EXTRA_EMPTY_SCREEN);
        return z;
    }

    private CellLayout getExtraEmptyScreen() {
        if (hasExtraEmptyScreen()) {
            return (CellLayout) this.mCellLayouts.get(this.mCellLayouts.size() - 1);
        }
        return null;
    }

    public int getExtraEmptyScreenIndex() {
        if (hasExtraEmptyScreen()) {
            return this.mCellLayouts.size() - 1;
        }
        return -1;
    }

    public void removeExtraEmptyScreen() {
        int i = 0;
        if (hasExtraEmptyScreen()) {
            int snapDuration;
            int emptyIndex = getExtraEmptyScreenIndex();
            if (getNextPage() == emptyIndex) {
                snapDuration = SNAP_OFF_EMPTY_SCREEN_DURATION;
            } else {
                snapDuration = 0;
            }
            int nextPage = getNextPage();
            if (getNextPage() == emptyIndex) {
                i = 1;
            }
            snapToPage(nextPage - i, snapDuration);
            fadeAndRemoveEmptyScreen(snapDuration);
            if (Utilities.sIsRtl) {
                updateCurrentPageScroll();
            }
        }
    }

    private void fadeAndRemoveEmptyScreen(int delay) {
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", new float[]{0.0f});
        PropertyValuesHolder bgAlpha = PropertyValuesHolder.ofFloat("backgroundAlpha", new float[]{0.0f});
        final CellLayout cl = getExtraEmptyScreen();
        this.mRemoveEmptyScreenRunnable = new Runnable() {
            public void run() {
                if (!AppsPagedView.this.hasExtraEmptyScreen() || cl == null || cl.getPageChildCount() > 0) {
                    AppsPagedView.this.commitExtraEmptyScreen();
                    return;
                }
                AppsPagedView.this.mCellLayouts.remove(AppsPagedView.this.getExtraEmptyScreenIndex());
                AppsPagedView.this.removeView(cl);
                AppsPagedView.this.removeEmptyScreen();
            }
        };
        ObjectAnimator oa = ObjectAnimator.ofPropertyValuesHolder(cl, new PropertyValuesHolder[]{alpha, bgAlpha});
        oa.setDuration((long) 150);
        oa.setStartDelay((long) delay);
        oa.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (AppsPagedView.this.mRemoveEmptyScreenRunnable != null) {
                    AppsPagedView.this.mRemoveEmptyScreenRunnable.run();
                }
            }
        });
        oa.start();
    }

    public void addExtraEmptyScreenOnDrag() {
        if (!this.mListener.isAlphabeticalMode()) {
            this.mRemoveEmptyScreenRunnable = null;
            if (!hasExtraEmptyScreen()) {
                createAppsPage(EXTRA_EMPTY_SCREEN);
            }
        }
    }

    public long getIdForScreen(CellLayout layout) {
        return (long) this.mCellLayouts.indexOf(layout);
    }

    public int commitExtraEmptyScreen() {
        int screenId = getExtraEmptyScreenIndex();
        if (hasExtraEmptyScreen()) {
            if (getExtraEmptyScreen() != null) {
                getExtraEmptyScreen().setTag(null);
            }
            if (getPageIndicator().isGrouping()) {
                removeMarkerForView(screenId);
            } else {
                updateMarker(screenId, getPageIndicatorMarker(screenId));
            }
        }
        return screenId;
    }

    private void removePageAt(int screenId) {
        removeView((View) this.mCellLayouts.get(screenId));
        this.mCellLayouts.remove(screenId);
    }

    public boolean removeEmptyScreen() {
        boolean hasRemovedPage = false;
        int i = this.mCellLayouts.size() - 1;
        while (i >= 0) {
            if (deletablePage(i) && i != getExtraEmptyScreenIndex()) {
                removePageAt(i);
                hasRemovedPage = true;
            }
            i--;
        }
        if (hasRemovedPage) {
            snapToPage(getCurrentPage());
        }
        if (Utilities.sIsRtl) {
            updateCurrentPageScroll();
        }
        return hasRemovedPage;
    }

    private boolean deletablePage(int pageIndex) {
        CellLayout layout = getCellLayout(pageIndex);
        int itemCount = getItemCountPageAt(pageIndex);
        int validItemCount = itemCount;
        for (int i = 0; i < itemCount; i++) {
            View view = layout.getChildOnPageAt(i);
            if ((view instanceof Removable) && ((Removable) view).isMarkToRemove()) {
                validItemCount--;
            }
        }
        return validItemCount <= 0;
    }

    protected PageMarkerResources getPageIndicatorMarker(int pageIndex) {
        if (pageIndex != getExtraEmptyScreenIndex() || this.mCellLayouts.size() <= 1) {
            return super.getPageIndicatorMarker(pageIndex);
        }
        return new PageMarkerResources(IndicatorType.PLUS);
    }

    protected void screenScrolled(int screenCenter, int leftScreen, int rightScreen) {
        updatePageAlphaValues(screenCenter, leftScreen, rightScreen);
    }

    private void postBindPages() {
        boolean needDefferToBind = false;
        try {
            needDefferToBind = this.mListener.deferToBind();
        } catch (NullPointerException e) {
            Log.e(TAG, "postBindPages : " + e.toString());
        }
        if (!needDefferToBind) {
            this.mListener.requestRunDeferredRunnable();
        }
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        postBindPages();
    }

    private AppsViewCellLayout createAppsPage(String tag) {
        AppsViewCellLayout cell = (AppsViewCellLayout) this.mLauncher.getInflater().inflate(R.layout.apps_view_screen, this, false);
        if (tag != null) {
            cell.setTag(tag);
        }
        cell.setBgImage(this.mListener.getState());
        if (this.mListener.getState() == 5) {
            cell.setBackgroundAlpha(1.0f);
            cell.setCrossHairAnimatedVisibility(0, false);
        }
        this.mCellLayouts.add(cell);
        addView(cell);
        return cell;
    }

    public AppsViewCellLayout createAppsPage() {
        return createAppsPage(null);
    }

    public void removeAllPages() {
        this.mCellLayouts.clear();
        removeAllViews();
    }

    protected boolean canOverScroll() {
        return this.mListener.getState() == 0;
    }

    public int getCellCountX() {
        return this.mLauncher.getDeviceProfile().appsGrid.getCellCountX();
    }

    public int getCellCountY() {
        return this.mLauncher.getDeviceProfile().appsGrid.getCellCountY();
    }

    public int getMaxItemsPerScreen() {
        GridInfo appsGrid = this.mLauncher.getDeviceProfile().appsGrid;
        return appsGrid.getCellCountX() * appsGrid.getCellCountY();
    }

    public CellLayout getCellLayout(int index) {
        if (index < 0 || index >= this.mCellLayouts.size()) {
            return null;
        }
        return (CellLayout) this.mCellLayouts.get(index);
    }

    public final void setInsets(Rect insets) {
        this.mInsets.set(insets);
    }

    public void updateAccessibilityFlags(boolean show) {
        int total = getPageCount();
        for (int i = 0; i < total; i++) {
            updateAccessibilityFlags(getCellLayout(i), show);
        }
        int i2 = (this.mListener.getState() == 0 || this.mListener.getState() == 2) ? 0 : 4;
        setImportantForAccessibility(i2);
    }

    private void updateAccessibilityFlags(CellLayout page, boolean show) {
        int accessible = ((this.mListener.getState() == 0 || this.mListener.getState() == 2) && show) ? 0 : 4;
        page.setImportantForAccessibility(2);
        page.getCellLayoutChildren().setImportantForAccessibility(accessible);
        page.setContentDescription(null);
        page.setAccessibilityDelegate(null);
    }

    public void mapPointFromSelfToChild(View v, float[] xy) {
        xy[0] = xy[0] - ((float) v.getLeft());
        xy[1] = xy[1] - ((float) v.getTop());
    }

    public int getItemCountPageAt(int pageNum) {
        ViewGroup page = getCellLayout(pageNum);
        if (page == null || !(page.getChildAt(0) instanceof CellLayoutChildren)) {
            return 0;
        }
        return ((CellLayoutChildren) page.getChildAt(0)).getChildCount();
    }

    public int getRankForNewItem(int pageNum) {
        int nextRank = getItemCountPageAt(pageNum);
        if (nextRank >= getMaxItemsPerScreen()) {
            return getMaxItemsPerScreen() - 1;
        }
        return nextRank;
    }

    protected void pageBeginMoving() {
        super.pageBeginMoving();
        updateClockLiveIcon();
    }

    protected void resetTransitionEffect(View page) {
        if ((this.mListener.getState() == 0 || this.mListener.getState() == 4) && !this.mListener.isSwitchingInternalState()) {
            super.resetTransitionEffect(page);
        }
    }

    private void updateClockLiveIcon() {
        int page = getComingPageForLiveIcon();
        if (page != -1) {
            CellLayout cellLayout = (CellLayout) getChildAt(page);
            if (cellLayout != null) {
                CellLayoutChildren cl = cellLayout.getCellLayoutChildren();
                int count = cl.getChildCount();
                for (int i = 0; i < count; i++) {
                    View view = cl.getChildAt(i);
                    ItemInfo item = (ItemInfo) view.getTag();
                    if ((item instanceof IconInfo) && (view instanceof IconView)) {
                        if (item.componentName != null && Utilities.checkClockPackageName(item.componentName.getPackageName()).booleanValue()) {
                            ((IconView) view).applyFromApplicationInfo((IconInfo) item);
                        }
                    } else if ((item instanceof FolderInfo) && (cl.getChildAt(item) instanceof FolderIconView)) {
                        boolean needToRefreshFolderIcon = false;
                        FolderIconView folderIconView = (FolderIconView) cl.getChildAt(item);
                        int j = 0;
                        while (j < folderIconView.getFolderInfo().contents.size()) {
                            IconInfo insideItem = (IconInfo) folderIconView.getFolderInfo().contents.get(j);
                            if (!(insideItem == null || ((IconInfo) folderIconView.getFolderInfo().contents.get(j)).getIntent().getComponent() == null)) {
                                String packageName = ((IconInfo) folderIconView.getFolderInfo().contents.get(j)).getIntent().getComponent().getPackageName();
                                if (packageName != null && insideItem.rank < 9 && Utilities.checkClockPackageName(packageName).booleanValue()) {
                                    needToRefreshFolderIcon = true;
                                    break;
                                }
                            }
                            j++;
                        }
                        if (needToRefreshFolderIcon) {
                            folderIconView.refreshFolderIcon();
                        }
                    }
                }
            }
        }
    }

    public void updateLiveIcon() {
        int page = getCurrentPage();
        if (page != -1) {
            CellLayout cellLayout = (CellLayout) getChildAt(page);
            if (cellLayout != null) {
                CellLayoutChildren cl = cellLayout.getCellLayoutChildren();
                int count = cl.getChildCount();
                for (int i = 0; i < count; i++) {
                    View view = cl.getChildAt(i);
                    ItemInfo item = (ItemInfo) view.getTag();
                    String packageName;
                    if (item instanceof IconInfo) {
                        packageName = item.componentName.getPackageName();
                        if (packageName != null && LiveIconManager.isLiveIconPackage(packageName) && (view instanceof IconView)) {
                            ((IconView) view).applyFromApplicationInfo((IconInfo) item);
                            view.invalidate();
                        }
                    } else if (item instanceof FolderInfo) {
                        boolean needToRefreshFolderIcon = false;
                        if (cl.getChildAt(item) instanceof FolderIconView) {
                            FolderIconView folderIconView = (FolderIconView) cl.getChildAt(item);
                            int j = 0;
                            while (j < folderIconView.getFolderInfo().contents.size()) {
                                IconInfo insideItem = (IconInfo) folderIconView.getFolderInfo().contents.get(j);
                                if (!(insideItem == null || ((IconInfo) folderIconView.getFolderInfo().contents.get(j)).getIntent().getComponent() == null)) {
                                    packageName = ((IconInfo) folderIconView.getFolderInfo().contents.get(j)).getIntent().getComponent().getPackageName();
                                    if (packageName != null && insideItem.rank < 9 && LiveIconManager.isLiveIconPackage(packageName)) {
                                        needToRefreshFolderIcon = true;
                                        break;
                                    }
                                }
                                j++;
                            }
                            if (needToRefreshFolderIcon) {
                                folderIconView.refreshFolderIcon();
                            }
                        }
                    }
                }
            }
        }
    }

    private int getComingPageForLiveIcon() {
        return this.mNextPage;
    }

    public void updateCheckBox(boolean visible) {
        int pageCount = getPageCount();
        for (int i = 0; i < pageCount; i++) {
            CellLayoutChildren clc = ((CellLayout) getChildAt(i)).getCellLayoutChildren();
            int childCount = clc.getChildCount();
            for (int j = 0; j < childCount; j++) {
                View v = clc.getChildAt(j);
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
    }

    private void updateLayoutByConfigurationChanged(int spacing) {
        updateChildLayout();
        this.mPageSpacing = spacing;
    }

    public void onChangeScreenGrid(int x, int y) {
        int screenCount = getChildCount();
        for (int i = 0; i < screenCount; i++) {
            ((AppsViewCellLayout) getChildAt(i)).setGridSize(x, y);
        }
        updateChildLayout();
    }

    private void updateChildLayout() {
        int screenCount = getChildCount();
        for (int i = 0; i < screenCount; i++) {
            AppsViewCellLayout appsCellLayout = (AppsViewCellLayout) getChildAt(i);
            appsCellLayout.setCellDimensions();
            appsCellLayout.updateIconViews();
        }
    }

    public boolean isTouchActive() {
        return this.mTouchState != 0;
    }

    protected void onPageEndMoving() {
        super.onPageEndMoving();
        if (this.mDragMgr.isDragging() && this.mListener.getState() == 0) {
            this.mDragMgr.forceTouchMove();
        }
        if (SemGateConfig.isGateEnabled()) {
            Log.i("GATE", "<GATE-M>SCREEN_LOADED_APP_MENU_" + getCurrentPage() + "</GATE-M>");
        }
    }

    protected void onPageBeginMoving() {
        super.onPageBeginMoving();
        if (LauncherFeature.supportQuickOption() && this.mLauncher.getDragMgr().isQuickOptionShowing() && this.mCurrentPage != this.mNextPage) {
            this.mDragMgr.removeQuickOptionView();
        }
    }

    protected boolean determineScrollingStart(MotionEvent ev) {
        boolean scrollable = super.determineScrollingStart(ev);
        if (scrollable) {
            new DvfsUtil(getContext()).boostCpuForSupportedModel(0);
        }
        return scrollable;
    }

    protected boolean determineScrollingStart(MotionEvent ev, float touchSlopScale) {
        if (this.mListener.isSelectState()) {
            touchSlopScale *= 2.5f;
        }
        return super.determineScrollingStart(ev, touchSlopScale);
    }

    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        return false;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0 || ev.getAction() == 2) {
            this.mListener.initBounceAnimation();
        }
        return getChildCount() <= 0 || super.onTouchEvent(ev);
    }

    public boolean supportWhiteBg() {
        return false;
    }

    protected void notifyPageScroll(int page, int x, int y, int scrollX, int pageCount) {
        if (this.mListener.isSelectState()) {
            super.notifyPageScroll(page, x, y, scrollX, pageCount);
        }
    }

    protected void notifyPageChange(int page, int scrollX, int pageCount) {
        if (this.mListener.isSelectState()) {
            super.notifyPageChange(page, scrollX, pageCount);
        }
    }

    protected void updatePageTransform(View page, int index, int screenCenter) {
        if (this.mListener.getState() == 0) {
            super.updatePageTransform(page, index, screenCenter);
        }
    }

    public void scrollTo(int x, int y) {
        if (this.mListener.getState() != 2 || (x >= 0 && x <= getMaxScrollX())) {
            super.scrollTo(x, y);
        }
    }

    public int getCustomPageCount() {
        return hasExtraEmptyScreen() ? 1 : 0;
    }

    public int getSupportCustomPageCount() {
        return 1;
    }

    public void snapToPageSALogging(boolean isPageIndicator) {
        int method = isPageIndicator ? 1 : 0;
        Resources res = getResources();
        if (this.mLauncher.getMultiSelectManager().isMultiSelectMode()) {
            SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Apps_SelectMode), res.getString(R.string.event_Home_ChangePage), (long) method);
        } else if (this.mListener.getState() == 4) {
            SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Apps_CleanUpPages), res.getString(R.string.event_Apps_CleanUp_ChangePage), (long) method);
        } else {
            SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Apps_2xxx), res.getString(R.string.event_Home_ChangePage), (long) method);
        }
    }

    boolean isSwitchingGridToNormal() {
        return this.mListener.isSwitchingGridToNormal();
    }

    boolean isGridState() {
        return this.mListener.getState() == 5;
    }

    boolean isTidyState() {
        return this.mListener.getState() == 4;
    }

    public void loggingPageCount() {
        int pageCount = getPageCount();
        if (this.mOldPageCount != pageCount && pageCount > 0) {
            this.mOldPageCount = pageCount;
            GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_APPS_PAGE_COUNT, null, (long) pageCount, true);
        }
    }

    public void onConfigurationChangedIfNeeded() {
        super.onConfigurationChangedIfNeeded();
        Resources res = getResources();
        int appsPagePadding = this.mLauncher.getDeviceProfile().appsGrid.getPagePadding();
        setHintPageZone(res, appsPagePadding);
        if (isShowingHintPages()) {
            showHintPages();
        }
        if (isGridState()) {
            float screenGridShrinkFactor = ((float) res.getInteger(R.integer.config_apps_gridShrinkPercentage)) / 100.0f;
            setScaleX(screenGridShrinkFactor);
            setScaleY(screenGridShrinkFactor);
            setTranslationY(-((float) (res.getDimensionPixelSize(R.dimen.apps_paged_view_offsetY_screengrid) + res.getDimensionPixelSize(R.dimen.all_apps_grid_top_bottom_padding))));
            if (getPageIndicator() != null) {
                getPageIndicator().setTranslationY((float) (-res.getDimensionPixelSize(R.dimen.apps_indicator_margin_bottom_screengrid)));
            }
        }
        updateLayoutByConfigurationChanged(appsPagePadding * 2);
    }

    private void setHintPageZone(Resources res, int appsPagePadding) {
        this.mHintPageWidth = res.getDimensionPixelSize(R.dimen.apps_hint_page_width) + appsPagePadding;
        this.mHintPageLeftZone = res.getDimensionPixelSize(R.dimen.hint_page_scroll_zone);
        this.mHintPageRightZone = res.getDisplayMetrics().widthPixels - this.mHintPageLeftZone;
        this.mTranslatePagesOffset = (float) res.getDimensionPixelSize(R.dimen.apps_pulling_pages_offset);
        if (LauncherFeature.supportNavigationBar() && !LauncherFeature.isTablet()) {
            DeviceProfile dp = this.mLauncher.getDeviceProfile();
            if (Utilities.getNavigationBarPositon() == 1) {
                this.mHintPageLeftZone += dp.navigationBarHeight;
            } else if (Utilities.getNavigationBarPositon() == 2) {
                this.mHintPageRightZone -= dp.navigationBarHeight;
            }
        }
    }

    public void rearrangeAllViews(boolean animate) {
        long cur = System.currentTimeMillis();
        Iterator it = new ArrayList(this.mCellLayouts).iterator();
        while (it.hasNext()) {
            CellLayout page = (CellLayout) it.next();
            ArrayList<View> iconViews = page.getCellLayoutChildren().getChildren();
            page.clearOccupiedCells();
            Iterator it2 = iconViews.iterator();
            while (it2.hasNext()) {
                View view = (View) it2.next();
                ItemInfo info = (ItemInfo) view.getTag();
                CellLayout to = getCellLayout((int) info.screenId);
                CellLayout from = (CellLayout) view.getParent().getParent();
                if (to == null || !from.equals(to)) {
                    from.removeView(view);
                    addItem(view, info);
                } else if (animate) {
                    updateItemToNewPosition(info, info.rank, info.screenId, (long) 0, 150, (boolean[][]) null);
                } else {
                    updateItemToNewPosition(info, info.rank, info.screenId, 0, 0, (boolean[][]) null);
                }
            }
            page.markCellsAsOccupiedForAllChild();
        }
        Log.d(TAG, "rearrangeChildren took : " + (System.currentTimeMillis() - cur));
    }

    public void addItem(View view, ItemInfo item) {
        LayoutParams lp;
        Log.d(TAG, "addItem = title : " + item.title + " , rank : " + item.rank + " , screen : " + item.screenId);
        int pagePos = item.rank % getMaxItemsPerScreen();
        item.cellX = pagePos % getCellCountX();
        item.cellY = pagePos / getCellCountX();
        ViewGroup.LayoutParams genericLp = view.getLayoutParams();
        if (genericLp == null || !(genericLp instanceof LayoutParams)) {
            lp = new LayoutParams(item.cellX, item.cellY, 1, 1);
        } else {
            lp = (LayoutParams) genericLp;
            lp.cellX = item.cellX;
            lp.cellY = item.cellY;
        }
        if (!(view instanceof FolderIconView) && (view instanceof IconView) && this.mListener.getState() == 2) {
            ((IconView) view).updateCheckBox(true);
        }
        for (int i = getChildCount(); i <= ((int) item.screenId); i++) {
            createAppsPage();
        }
        try {
            getCellLayout((int) item.screenId).addViewToCellLayout(view, -1, this.mLauncher.getViewIdForItem(item), lp, true);
        } catch (Exception e) {
            Log.e(TAG, "Exception in Adding item : mAppsPagedView = " + this + ", item.screenId = " + item.screenId);
            e.printStackTrace();
        }
    }

    public void addItemToLastPosition(IconInfo iconInfo) {
        if (iconInfo != null) {
            int lastPage = getPageCount() - 1;
            int rank = findFirstEmptyCell(lastPage);
            if (rank == -1) {
                createAppsPage();
                rank = 0;
                lastPage++;
            }
            iconInfo.container = -102;
            addViewForRankScreen(this.mListener.createItemView(iconInfo, getCellLayout(lastPage), null), iconInfo, rank, lastPage);
            this.mListener.updateItemInDb(iconInfo);
        }
    }

    public void addViewForRankScreen(View view, ItemInfo item, int rank, int screen) {
        int countX = getCellCountX();
        item.rank = rank;
        item.screenId = (long) screen;
        item.cellX = rank % countX;
        item.cellY = rank / countX;
        item.mDirty = true;
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        lp.cellX = rank % countX;
        lp.cellY = rank / countX;
        getCellLayout(screen).addViewToCellLayout(view, -1, this.mLauncher.getViewIdForItem(item), lp, true);
    }

    public void updateItemToNewPosition(ItemInfo item, int pos, long screen, long startDelay, int duration, boolean[][] occupied) {
        CellLayout page = getCellLayout((int) screen);
        View v = page.getCellLayoutChildren().getChildAt(item);
        setItemLocation(item, pos, screen);
        if (duration < 0) {
            duration = 150;
        }
        if (duration == 0 && (page instanceof AppsViewCellLayout)) {
            ((AppsViewCellLayout) page).childToPosition(v, item.rank % getCellCountX(), item.rank / getCellCountX());
            return;
        }
        page.animateChildToPosition(v, item.rank % getCellCountX(), item.rank / getCellCountX(), duration, (int) startDelay, true, false, occupied);
    }

    private void setItemLocation(ItemInfo item, int position, long screen) {
        if (position == -1) {
            throw new IllegalArgumentException("Invalid position");
        }
        item.screenId = screen;
        item.rank = position;
    }

    public int findFirstEmptyCell(int screenIndex) {
        boolean[] ops = findAllOccupiedCells(screenIndex);
        for (int i = 0; i < ops.length; i++) {
            if (!ops[i]) {
                return i;
            }
        }
        return -1;
    }

    private boolean[] findAllOccupiedCells(int screenIndex) {
        boolean[] ops = new boolean[getMaxItemsPerScreen()];
        CellLayout layout = getCellLayout(screenIndex);
        if (layout != null) {
            boolean[][] occupied = layout.getOccupied();
            int cellCountX = getCellCountX();
            int cellCountY = getCellCountY();
            for (int x = 0; x < cellCountX; x++) {
                for (int y = 0; y < cellCountY; y++) {
                    ops[(y * cellCountX) + x] = occupied[x][y];
                }
            }
        }
        return ops;
    }

    private ArrayList<CellLayoutChildren> getAllCellLayoutChildren() {
        ArrayList<CellLayoutChildren> childrenArrayList = new ArrayList();
        Iterator it = this.mCellLayouts.iterator();
        while (it.hasNext()) {
            childrenArrayList.add(((CellLayout) it.next()).getCellLayoutChildren());
        }
        return childrenArrayList;
    }

    public void mapOverItems(boolean recurse, ItemOperator op) {
        Iterator it = getAllCellLayoutChildren().iterator();
        while (it.hasNext()) {
            CellLayoutChildren clc = (CellLayoutChildren) it.next();
            int itemCount = clc.getChildCount();
            int itemIdx = 0;
            while (itemIdx < itemCount) {
                View item = clc.getChildAt(itemIdx);
                ItemInfo info = (ItemInfo) item.getTag();
                if (!op.evaluate(info, item, null)) {
                    if (recurse && (info instanceof FolderInfo) && (item instanceof FolderIconView)) {
                        FolderIconView folder = (FolderIconView) item;
                        Iterator it2 = folder.getFolderView().getItemsInReadingOrder().iterator();
                        while (it2.hasNext()) {
                            View child = (View) it2.next();
                            if (op.evaluate((ItemInfo) child.getTag(), child, folder)) {
                                return;
                            }
                        }
                    }
                    itemIdx++;
                } else {
                    return;
                }
            }
        }
    }

    public boolean hasEmptyCellAtPages() {
        int total_page = getChildCount();
        int i = 0;
        while (i < total_page - 1 && ((AppsViewCellLayout) getCellLayout(i)).isFullyOccupied()) {
            i++;
        }
        if (i < total_page - 1) {
            return true;
        }
        return false;
    }

    public String getPageDescription() {
        int lastPage = getChildCount();
        int currentPage = getCurrentPage() + 1;
        return getResources().getString(R.string.default_scroll_format, new Object[]{Integer.valueOf(currentPage), Integer.valueOf(lastPage)});
    }

    public int getDesiredWidth() {
        if (getPageCount() > 0) {
            return (((CellLayout) getPageAt(0)).getDesiredWidth() + getPaddingLeft()) + getPaddingRight();
        }
        return 0;
    }

    public int updateChildrenLayersEnabled(int pageIndex, boolean enable) {
        int currentPage = pageIndex >= 0 ? pageIndex : getNextPage();
        CellLayout layout = (CellLayout) getPageAt(currentPage);
        if (layout != null) {
            layout.enableHardwareLayer(enable);
        }
        return currentPage;
    }

    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (!this.mUpdateOnlyCurrentPage || this.mIsPageMoving || child == null || child.equals(getChildAt(getCurrentPage()))) {
            return super.drawChild(canvas, child, drawingTime);
        }
        Log.d(TAG, "drawChild, mUpdateOnlyCurrentPage && !mIsPageMoving && !currentPage => draw skip!");
        return false;
    }
}

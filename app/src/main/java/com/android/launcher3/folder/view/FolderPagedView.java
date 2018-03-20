package com.android.launcher3.folder.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel.OnLauncherBindingItemsCompletedListener;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.controller.AppsController;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.item.ItemOperator;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.LayoutParams;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.deviceprofile.GridInfo;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.common.view.IconViewStub;
import com.android.launcher3.common.view.OnInflateListener;
import com.android.launcher3.common.view.PageIndicator;
import com.android.launcher3.common.view.PageIndicator.PageMarkerResources;
import com.android.launcher3.common.view.PageIndicator.PageMarkerResources.IndicatorType;
import com.android.launcher3.folder.controller.FolderFocusListener;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class FolderPagedView extends PagedView {
    private static final int PAGE_ACTIVE_RANGE = 3;
    private static final long PAGE_INDICATOR_ANIMATION_DURATION = 400;
    private static final long PAGE_INDICATOR_ANIMATION_STAGGERED_DELAY = 150;
    private static final long PAGE_INDICATOR_ANIMATION_START_DELAY = 300;
    private static final float PAGE_INDICATOR_OVERSHOOT_TENSION = 4.9f;
    private static final int REORDER_ANIMATION_DURATION = 230;
    private static final float SCROLL_HINT_FRACTION = 0.07f;
    private static final int START_VIEW_REORDER_DELAY = 30;
    private static final String TAG = "FolderPagedView";
    private static final float VIEW_REORDER_DELAY_FACTOR = 0.9f;
    private static final int[] sTempPosArray = new int[2];
    private View mAddButton;
    private int mAllocatedContentSize;
    private boolean mBorderHidden;
    private FolderView mFolder;
    private int mGridCountX;
    private int mGridCountY;
    private final IconCache mIconCache;
    private final HashMap<IconInfo, IconViewStub> mIconViewStubMaps = new HashMap();
    private final LayoutInflater mInflater;
    private FolderFocusListener mKeyListener;
    private int mMaxCountX;
    private int mMaxCountY;
    private int mMaxItemsPerPage;
    private PageIndicator mPageIndicator;
    private final HashMap<View, Runnable> mPendingAnimations = new HashMap();

    public FolderPagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile profile = app.getDeviceProfile();
        this.mMaxCountX = profile.folderGrid.getCellCountX();
        this.mMaxCountY = profile.folderGrid.getCellCountY();
        this.mMaxItemsPerPage = this.mMaxCountX * this.mMaxCountY;
        this.mInflater = LayoutInflater.from(context);
        this.mIconCache = app.getIconCache();
    }

    public void setFolder(FolderView folder) {
        this.mFolder = folder;
        this.mKeyListener = new FolderFocusListener();
        this.mPageIndicator = (PageIndicator) folder.findViewById(R.id.folder_page_indicator);
        post(new Runnable() {
            public void run() {
                FolderPagedView.this.mPageIndicator.offsetWindowCenterTo(false);
            }
        });
    }

    private void setupContentDimensions(int count) {
        this.mAllocatedContentSize = count;
        this.mGridCountX = this.mMaxCountX;
        this.mGridCountY = this.mMaxCountY;
        for (int i = getPageCount() - 1; i >= 0; i--) {
            getPageAt(i).setGridSize(this.mGridCountX, this.mGridCountY);
        }
    }

    public ArrayList<IconInfo> bindItems(ArrayList<IconInfo> items) {
        ArrayList<View> icons = new ArrayList();
        ArrayList<IconInfo> extra = new ArrayList();
        Iterator it = items.iterator();
        while (it.hasNext()) {
            IconInfo item = (IconInfo) it.next();
            if (this.mFolder.getInfo().container == -102) {
                item.ignoreCheckItemInfo = true;
            }
            if (icons.size() < 9 || item.itemType != 0) {
                icons.add(createNewView(item, true));
            } else {
                icons.add((IconViewStub) createNewView(item, false));
            }
        }
        arrangeChildren(icons, icons.size(), false);
        handleIconViewStubs();
        return extra;
    }

    public void rebindItems(ArrayList<IconInfo> items) {
        ArrayList<View> oldViews = this.mFolder.getItemsInReadingOrder();
        LongSparseArray<View> viewArray = new LongSparseArray();
        Iterator it = oldViews.iterator();
        while (it.hasNext()) {
            View view = (View) it.next();
            IconInfo info = (IconInfo) view.getTag();
            if (info != null) {
                viewArray.put(info.id, view);
            }
        }
        ArrayList<View> newViews = new ArrayList();
        for (int i = 0; i < items.size(); i++) {
            IconInfo item = (IconInfo) items.get(i);
            view = (View) viewArray.get(item.id);
            if (view != null) {
                newViews.add(view);
            } else {
                newViews.add(createNewView(item));
            }
        }
        arrangeChildren(newViews, newViews.size(), false);
    }

    private void handleIconViewStubs() {
        if (!this.mIconViewStubMaps.isEmpty()) {
            if (this.mFolder.getInfo().isContainApps()) {
                LauncherAppState.getInstance().getModel().registerOnLauncherBindingItemsCompletedListener(new OnLauncherBindingItemsCompletedListener() {
                    public void onLauncherBindingItemsCompleted() {
                        Log.i(FolderPagedView.TAG, "onLauncherBindingItemsCompleted");
                        FolderPagedView.this.inflateAllIconViewStubsInBackground();
                        final AnonymousClass2 listener = this;
                        new Handler().post(new Runnable() {
                            public void run() {
                                LauncherAppState.getInstance().getModel().unregisterOnLauncherBindingItemsCompletedListener(listener);
                            }
                        });
                    }
                });
                return;
            }
            inflateAllIconViewStubsInBackground();
        }
    }

    public void inflateAllIconViewStubsInBackground() {
        synchronized (this.mIconViewStubMaps) {
            Log.d(TAG, "inflateAllIconViewStubsInBackground : stubs=" + this.mIconViewStubMaps.size() + ", info=" + this.mFolder.getInfo());
            for (IconViewStub stub : this.mIconViewStubMaps.values()) {
                stub.inflateInBackground((IconInfo) stub.getTag());
            }
        }
    }

    public void inflateIconViewStubPerPage(int pageIndex) {
        if (pageIndex >= 0 && pageIndex < getChildCount()) {
            CellLayout page = getPageAt(pageIndex);
            int itemCountX = page.getCountX();
            int itemCountY = page.getCountY();
            for (int j = 0; j < itemCountY; j++) {
                for (int i = 0; i < itemCountX; i++) {
                    View v = page.getChildAt(i, j);
                    if (v instanceof IconViewStub) {
                        IconViewStub stub = (IconViewStub) v;
                        if (pageIndex == 0) {
                            stub.inflateImmediately();
                        } else {
                            stub.inflateInBackgroundUrgent((IconInfo) getTag());
                        }
                    }
                }
            }
        }
    }

    public int allocateRankForNewItem(boolean isNeedToMove) {
        int rank = getItemCount();
        ArrayList<View> views = new ArrayList(this.mFolder.getItemsInReadingOrder());
        if (rank > views.size()) {
            Log.w(TAG, "allocateRankForNewItem : number of items is not matched. " + rank + ":" + views.size());
            rank = views.size();
        }
        views.add(rank, null);
        arrangeChildren(views, views.size(), false);
        if (isNeedToMove) {
            setCurrentPage(rank / this.mMaxItemsPerPage);
        }
        return rank;
    }

    public View createAndAddViewForRank(IconInfo item, int rank) {
        View icon = createNewView(item);
        addViewForRank(icon, item, rank);
        return icon;
    }

    public void addViewForRank(View view, IconInfo item, int rank) {
        int pagePos = rank % this.mMaxItemsPerPage;
        int pageNo = rank / this.mMaxItemsPerPage;
        item.rank = rank;
        item.cellX = pagePos % this.mGridCountX;
        item.cellY = pagePos / this.mGridCountX;
        CellLayout page = getPageAt(pageNo);
        if (page != null) {
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            lp.cellX = item.cellX;
            lp.cellY = item.cellY;
            page.addViewToCellLayout(view, -1, ((Launcher) getContext()).getViewIdForItem(item), lp, true);
            return;
        }
        Log.w(TAG, "addViewForRank : can't get " + pageNo + " page");
    }

    private void addViewForRank(View view, int rank) {
        int pagePos = rank % this.mMaxItemsPerPage;
        int pageNo = rank / this.mMaxItemsPerPage;
        int cellX = pagePos % this.mGridCountX;
        int cellY = pagePos / this.mGridCountX;
        int viewId = view.getId() > 0 ? view.getId() : View.generateViewId();
        CellLayout page = getPageAt(pageNo);
        if (page != null) {
            page.addViewToCellLayout(view, -1, viewId, new LayoutParams(cellX, cellY, 1, 1), false);
        } else {
            Log.w(TAG, "addViewForRank : can't get " + pageNo + " page");
        }
    }

    public void insertViewBeforeArrangeChildren(View view, IconInfo item, int rank) {
        insertViewBeforeArrangeChildren(view, item, rank, getItemCount() + 1);
    }

    public void insertViewBeforeArrangeChildren(View view, IconInfo item, int rankToInsert, int rankToKeep) {
        ArrayList<View> views = this.mFolder.getItemsInReadingOrder();
        if (rankToInsert >= views.size()) {
            rankToInsert = views.size();
            item.rank = views.size();
        }
        if (rankToKeep <= item.rank || rankToKeep >= views.size()) {
            views.add(rankToInsert, view);
            return;
        }
        View dropTargetView = (View) views.remove(rankToKeep);
        views.add(rankToInsert, view);
        views.add(rankToKeep, dropTargetView);
    }

    public boolean isInActiveRange(int rank) {
        int pageIndex = rank / this.mMaxItemsPerPage;
        int maxBound = Math.min(getCurrentPage() + 1, getPageCount() - 1);
        if (pageIndex < Math.max(getCurrentPage() - 1, 0) || pageIndex > maxBound) {
            return false;
        }
        return true;
    }

    public boolean isAllIconViewInflated() {
        return this.mIconViewStubMaps.isEmpty();
    }

    private void applyIconViewInfo(IconView iconView, IconInfo iconInfo) {
        if (iconView != null && iconInfo != null) {
            if (this.mFolder.getInfo().isContainApps()) {
                iconView.setIconDisplay(3);
                iconView.applyFromApplicationInfo(iconInfo, iconInfo.isPromise());
            } else {
                iconView.setIconDisplay(4);
                iconView.applyFromShortcutInfo(iconInfo, this.mIconCache);
            }
            iconView.setOnClickListener(this.mFolder);
            iconView.setOnLongClickListener(this.mFolder);
            iconView.setOnKeyListener(this.mKeyListener);
            iconView.setLayoutParams(new LayoutParams(iconInfo.cellX, iconInfo.cellY, iconInfo.spanX, iconInfo.spanY));
        }
    }

    @SuppressLint({"InflateParams"})
    public View createNewView(final IconInfo iconInfo, boolean realView) {
        if (realView) {
            View iconView = (IconView) this.mInflater.inflate(R.layout.icon, null, false);
            applyIconViewInfo(iconView, iconInfo);
            return iconView;
        }
        View viewStub = new IconViewStub(getContext(), (int) R.layout.icon);
        viewStub.addOnInflateListener(new OnInflateListener() {
            public void onInflate(View stub, View inflated, boolean cancelled) {
                if (inflated instanceof IconView) {
                    IconView iconView = (IconView) inflated;
                    FolderPagedView.this.applyIconViewInfo(iconView, iconInfo);
                    IconInfo info = (IconInfo) iconView.getTag();
                    FolderPagedView.this.mFolder.notifyIconViewInflated(info != null ? info.rank : 0);
                    stub.setTag(null);
                }
                synchronized (FolderPagedView.this.mIconViewStubMaps) {
                    if (FolderPagedView.this.mIconViewStubMaps.containsKey(iconInfo)) {
                        FolderPagedView.this.mIconViewStubMaps.remove(iconInfo);
                        if (FolderPagedView.this.mIconViewStubMaps.isEmpty()) {
                            Log.d(FolderPagedView.TAG, "All icon views are updated. info=" + FolderPagedView.this.mFolder.getInfo());
                        }
                    }
                }
            }
        });
        viewStub.setLayoutParams(new LayoutParams(iconInfo.cellX, iconInfo.cellY, iconInfo.spanX, iconInfo.spanY));
        viewStub.setTag(iconInfo);
        View view = viewStub;
        synchronized (this.mIconViewStubMaps) {
            this.mIconViewStubMaps.put(iconInfo, viewStub);
        }
        return view;
    }

    public View createNewView(IconInfo iconInfo) {
        return createNewView(iconInfo, true);
    }

    public CellLayout getPageAt(int index) {
        return (CellLayout) getChildAt(index);
    }

    public void removeCellLayoutView(View view) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            getPageAt(i).removeView(view);
        }
    }

    public CellLayout getCurrentCellLayout() {
        return getPageAt(getNextPage());
    }

    public String getPageDescription() {
        return getCurrentPageDescription();
    }

    private CellLayout createAndAddNewPage() {
        FolderCellLayout page = new FolderCellLayout(getContext());
        page.getCellLayoutChildren().setMotionEventSplittingEnabled(false);
        page.setImportantForAccessibility(2);
        page.setGridSize(this.mGridCountX, this.mGridCountY);
        addView(page, -1, generateDefaultLayoutParams());
        return page;
    }

    protected int getChildGap() {
        return getPaddingLeft() + getPaddingRight();
    }

    public void setFixedSize(int width, int height) {
        width -= getPaddingLeft() + getPaddingRight();
        height -= getPaddingTop() + getPaddingBottom();
        for (int i = getChildCount() - 1; i >= 0; i--) {
            ((CellLayout) getChildAt(i)).setFixedSize(width, height);
        }
    }

    public void removeItem(View v) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            getPageAt(i).removeView(v);
        }
    }

    public void arrangeChildren(ArrayList<View> list, int itemCount) {
        arrangeChildren(list, itemCount, true);
    }

    @SuppressLint({"RtlHardcoded"})
    private void arrangeChildren(ArrayList<View> list, int itemCount, boolean saveChanges) {
        int i;
        setPageIndicatorAnimation(false);
        if (!(this.mAddButton == null || this.mAddButton.getParent() == null)) {
            list.add(this.mAddButton);
            itemCount++;
        }
        ArrayList<CellLayout> pages = new ArrayList();
        for (i = 0; i < getChildCount(); i++) {
            CellLayout page = (CellLayout) getChildAt(i);
            page.removeAllViews();
            pages.add(page);
        }
        setupContentDimensions(itemCount);
        Iterator<CellLayout> pageItr = pages.iterator();
        CellLayout currentPage = null;
        int position = 0;
        int rank = 0;
        i = 0;
        while (i < itemCount) {
            View v = list.size() > i ? (View) list.get(i) : null;
            if (currentPage == null || position >= this.mMaxItemsPerPage) {
                if (pageItr.hasNext()) {
                    currentPage = (CellLayout) pageItr.next();
                } else {
                    currentPage = createAndAddNewPage();
                }
                position = 0;
            }
            if (v != null) {
                if (v.getParent() != null) {
                    Log.w(TAG, "arrangeChildren : child view has wrong parent view. (child=" + v + ", parent=" + v.getParent() + ")");
                    ((ViewGroup) v.getParent()).removeView(v);
                }
                LayoutParams lp = (LayoutParams) v.getLayoutParams();
                int newX = position % this.mGridCountX;
                int newY = position / this.mGridCountX;
                LayoutParams lp2;
                if (v.getTag() instanceof ItemInfo) {
                    ItemInfo info = (ItemInfo) v.getTag();
                    if (!(info.cellX == newX && info.cellY == newY && info.rank == rank)) {
                        info.cellX = newX;
                        info.cellY = newY;
                        info.rank = rank;
                        if (saveChanges && info.hidden == 0) {
                            if ((this.mFolder.getBaseController() instanceof AppsController) && this.mFolder.isAppsAlphabeticViewType()) {
                                this.mFolder.getBaseController().addOrMoveItemInDb(info, this.mFolder.getInfo().id, -1, -1, -1, -1);
                                info.cellX = newX;
                                info.cellY = newY;
                                info.rank = rank;
                                info.mDirty = false;
                            } else {
                                this.mFolder.getBaseController().addOrMoveItemInDb(info, this.mFolder.getInfo().id, 0, info.cellX, info.cellY, info.rank);
                            }
                        }
                    }
                    if (lp == null) {
                        lp2 = new LayoutParams(info.cellX, info.cellY, 1, 1);
                    } else {
                        lp.cellX = info.cellX;
                        lp.cellY = info.cellY;
                        lp2 = lp;
                    }
                    currentPage.addViewToCellLayout(v, -1, ((Launcher) getContext()).getViewIdForItem(info), lp2, true);
                } else {
                    if (lp == null) {
                        lp2 = new LayoutParams(newX, newY, 1, 1);
                    } else {
                        lp.cellX = newX;
                        lp.cellY = newY;
                        lp2 = lp;
                    }
                    currentPage.addViewToCellLayout(v, -1, v.getId() > 0 ? v.getId() : View.generateViewId(), lp2, false);
                }
                if (rank < 9 && (v instanceof IconView)) {
                    ((IconView) v).verifyHighRes();
                }
            }
            rank++;
            position++;
            i++;
        }
        int currentPageIndex = getNextPage();
        boolean removed = false;
        while (pageItr.hasNext() && getPageCount() > 1) {
            removeView((View) pageItr.next());
            removed = true;
        }
        if (removed && currentPageIndex > getPageCount() - 1) {
            setCurrentPage(0);
        }
        setPageIndicatorAnimation(true);
    }

    public int getDesiredWidth() {
        if (getPageCount() > 0) {
            return (getPageAt(0).getDesiredWidth() + getPaddingLeft()) + getPaddingRight();
        }
        return 0;
    }

    public int getDesiredHeight() {
        if (getPageCount() > 0) {
            return (getPageAt(0).getDesiredHeight() + getPaddingTop()) + getPaddingBottom();
        }
        return 0;
    }

    public int getCellLayoutChildrenWidth() {
        if (getPageCount() <= 0) {
            return 0;
        }
        return getPageAt(0).getDesiredWidth() - (getPageAt(0).getPaddingRight() + getPageAt(0).getPaddingLeft());
    }

    public int getCellLayoutChildrenHeight() {
        if (getPageCount() <= 0) {
            return 0;
        }
        return getPageAt(0).getDesiredHeight() - (getPageAt(0).getPaddingBottom() + getPageAt(0).getPaddingTop());
    }

    public int getItemCount() {
        int lastPageIndex = getChildCount() - 1;
        if (lastPageIndex < 0) {
            return 0;
        }
        int count = getPageAt(lastPageIndex).getCellLayoutChildren().getChildCount() + (this.mMaxItemsPerPage * lastPageIndex);
        if (this.mAddButton == null || this.mAddButton.getParent() == null) {
            return count;
        }
        return count - 1;
    }

    public int findNearestArea(int pixelX, int pixelY) {
        int pageIndex = getNextPage();
        getPageAt(pageIndex).findNearestArea(pixelX, pixelY, 1, 1, sTempPosArray);
        return Math.min(getAllocatedContentSize() - 1, ((this.mMaxItemsPerPage * pageIndex) + (sTempPosArray[1] * this.mGridCountX)) + sTempPosArray[0]);
    }

    protected PageMarkerResources getPageIndicatorMarker(int pageIndex) {
        return new PageMarkerResources(IndicatorType.DEFAULT);
    }

    public boolean isFull() {
        return false;
    }

    public boolean isAppsFolder() {
        return this.mFolder != null && (this.mFolder.getBaseController() instanceof AppsController);
    }

    public View getFirstItem() {
        if (getChildCount() < 1) {
            return null;
        }
        CellLayout currentPage = getCurrentCellLayout();
        if (currentPage != null) {
            return currentPage.getCellLayoutChildren().getChildAt(0, 0);
        }
        Log.w(TAG, "getFirstItem : can't get current page");
        return null;
    }

    public View getLastItem() {
        if (getChildCount() < 1) {
            return null;
        }
        CellLayout currentPage = getCurrentCellLayout();
        if (currentPage != null) {
            CellLayoutChildren lastContainer = currentPage.getCellLayoutChildren();
            int lastRank = lastContainer.getChildCount() - 1;
            if (!(this.mAddButton == null || this.mAddButton.getParent() == null)) {
                lastRank--;
            }
            if (this.mGridCountX > 0) {
                return lastContainer.getChildAt(lastRank % this.mGridCountX, lastRank / this.mGridCountX);
            }
            return lastContainer.getChildAt(lastRank);
        }
        Log.w(TAG, "getLastItem : can't get current page");
        return null;
    }

    public View iterateOverItems(ItemOperator op) {
        int pageCount = getChildCount();
        for (int k = 0; k < pageCount; k++) {
            CellLayout page = getPageAt(k);
            int itemCount = 0;
            int childCount = page.getCellLayoutChildren().getChildCount();
            int itemCountX = page.getCountX();
            int itemCountY = page.getCountY();
            for (int j = 0; j < itemCountY; j++) {
                for (int i = 0; i < itemCountX; i++) {
                    View v = page.getChildAt(i, j);
                    if (v != null) {
                        itemCount++;
                        if (v.getTag() instanceof ItemInfo) {
                            if (op.evaluate((ItemInfo) v.getTag(), v, this)) {
                                return v;
                            }
                        } else {
                            continue;
                        }
                    }
                }
            }
            if (itemCount != childCount) {
                Log.w(TAG, String.format("iterateOverItems : items are not matched in %d page (itemCount=%d, childCount=%d)", new Object[]{Integer.valueOf(k), Integer.valueOf(itemCount), Integer.valueOf(childCount)}));
            }
        }
        return null;
    }

    public String getAccessibilityDescription() {
        return getContext().getString(R.string.folder_opened);
    }

    public void setFocusOnFirstChild() {
        CellLayout currentPage = getCurrentCellLayout();
        if (currentPage != null) {
            View firstChild = currentPage.getChildAt(0, 0);
            if (firstChild != null) {
                firstChild.requestFocus();
            }
        }
    }

    protected void notifyPageSwitchListener() {
        super.notifyPageSwitchListener();
        if (this.mFolder != null) {
            this.mFolder.updateContentFocus();
        }
    }

    public void showScrollHint(int direction) {
        int i;
        if (direction == 0) {
            i = 1;
        } else {
            i = 0;
        }
        int delta = (getScrollForPage(getNextPage()) + ((int) (((float) getWidth()) * ((i ^ Utilities.sIsRtl) != 0 ? -0.07f : SCROLL_HINT_FRACTION)))) - getScrollX();
        if (delta != 0) {
            this.mScroller.setInterpolator(new DecelerateInterpolator());
            this.mScroller.startScroll(getScrollX(), 0, delta, 0, 500);
            invalidate();
        }
    }

    public void clearScrollHint() {
        if (getScrollX() != getScrollForPage(getNextPage())) {
            snapToPage(getNextPage());
        }
    }

    public void completePendingPageChanges() {
        if (!this.mPendingAnimations.isEmpty()) {
            for (Entry<View, Runnable> e : new HashMap(this.mPendingAnimations).entrySet()) {
                ((View) e.getKey()).animate().cancel();
                ((Runnable) e.getValue()).run();
            }
        }
    }

    public boolean rankOnCurrentPage(int rank) {
        return rank / this.mMaxItemsPerPage == getNextPage();
    }

    protected void onPageBeginMoving() {
        super.onPageBeginMoving();
        this.mBorderHidden = false;
        setAccessibilityFocusChange(false);
        getVisiblePages(sTempPosArray);
        for (int i = sTempPosArray[0]; i <= sTempPosArray[1]; i++) {
            verifyVisibleHighResIcons(i);
        }
    }

    protected void onPageEndMoving() {
        super.onPageEndMoving();
        setAccessibilityFocusChange(true);
        if (!isAllIconViewInflated()) {
            inflateIconViewStubPerPage(getCurrentPage());
        }
    }

    public void verifyVisibleHighResIcons(int pageNo) {
        CellLayout page = getPageAt(pageNo);
        if (page != null) {
            CellLayoutChildren parent = page.getCellLayoutChildren();
            for (int i = parent.getChildCount() - 1; i >= 0; i--) {
                if (parent.getChildAt(i) instanceof IconView) {
                    ((IconView) parent.getChildAt(i)).verifyHighRes();
                }
            }
        }
    }

    protected void setAccessibilityFocusChange(boolean enable) {
        FolderCellLayout layout = (FolderCellLayout) getChildAt(getNextPage());
        if (layout != null) {
            layout.setAccessibilityEnabled(enable);
        }
    }

    protected void resetTransitionEffect(View page) {
        if (this.mFolder.getFolderState() == 1) {
            super.resetTransitionEffect(page);
        }
    }

    protected void screenScrolled(int screenCenter, int leftScreen, int rightScreen) {
        updateBorderAlphaValues(screenCenter);
        updatePageAlphaValues(screenCenter, leftScreen, rightScreen);
    }

    private void updateBorderAlphaValues(int screenCenter) {
        if (this.mFolder.getFolderState() != 2) {
            int currentPage = getNextPage();
            CellLayout child = getPageAt(currentPage);
            if (child != null) {
                float scrollProgress = getScrollProgress(screenCenter, child, currentPage);
                float reverseAlpha = 1.0f - backgroundAlphaThreshold(Math.abs(scrollProgress));
                if ((currentPage == 0 && scrollProgress <= 0.0f) || (currentPage == getPageCount() - 1 && scrollProgress >= 0.0f)) {
                    reverseAlpha = 1.0f;
                    this.mBorderHidden = false;
                } else if (this.mBorderHidden) {
                    if (this.mTouchState != 0) {
                        reverseAlpha = 0.0f;
                    }
                } else if (reverseAlpha == 0.0f) {
                    this.mBorderHidden = true;
                }
                this.mFolder.setBorderAlpha(reverseAlpha);
            }
        }
    }

    private void updatePageAlphaValues(int screenCenter, int leftScreen, int rightScreen) {
        int folderState = this.mFolder.getFolderState();
        if (folderState == 2 || folderState == 3) {
            updateDragPageAlphaValues(screenCenter, leftScreen, rightScreen);
        }
    }

    private void updateDragPageAlphaValues(int screenCenter, int leftScreen, int rightScreen) {
        for (int i = leftScreen; i <= rightScreen; i++) {
            FolderCellLayout child = (FolderCellLayout) getChildAt(i);
            if (child != null) {
                float alpha = Math.min(1.0f, Math.abs(getScrollProgress(screenCenter, child, i)));
                child.setBackgroundAlpha(alpha);
                child.setPartialBackgroundAlpha(1.0f - alpha);
            }
        }
    }

    private float backgroundAlphaThreshold(float r) {
        if (r < 0.0f) {
            return 0.0f;
        }
        if (r > 0.08f) {
            return 1.0f;
        }
        return (r - 0.0f) / (0.08f - 0.0f);
    }

    public int getAllocatedContentSize() {
        int availableContentSize = this.mAllocatedContentSize;
        if (this.mAddButton == null || this.mAddButton.getParent() == null) {
            return availableContentSize;
        }
        return availableContentSize - 1;
    }

    public void realTimeReorder(int empty, int target, boolean immediately) {
        completePendingPageChanges();
        int delay = 0;
        float delayAmount = 30.0f;
        int pageToAnimate = getNextPage();
        int pageT = target / this.mMaxItemsPerPage;
        int pagePosT = target % this.mMaxItemsPerPage;
        int pagePosE = empty % this.mMaxItemsPerPage;
        int pageE = empty / this.mMaxItemsPerPage;
        if (target != empty) {
            int direction;
            int moveStart;
            int moveEnd;
            int endPos;
            int startPos;
            CellLayout page;
            final View v;
            CharSequence title;
            if (target > empty) {
                direction = 1;
                if (immediately) {
                    moveStart = empty;
                    moveEnd = target;
                    endPos = pagePosE;
                    startPos = pagePosE;
                } else if (pageE < pageToAnimate) {
                    if (pageT < pageToAnimate) {
                        moveStart = empty;
                        moveEnd = target;
                        endPos = pagePosT;
                        startPos = pagePosT;
                    } else {
                        moveStart = empty;
                        moveEnd = pageToAnimate * this.mMaxItemsPerPage;
                        startPos = 0;
                        endPos = pagePosT;
                    }
                } else if (pageE != pageToAnimate) {
                    moveStart = empty;
                    moveEnd = target;
                    endPos = pagePosE;
                    startPos = pagePosE;
                } else if (pageT > pageToAnimate) {
                    moveStart = ((pageToAnimate + 1) * this.mMaxItemsPerPage) - 1;
                    moveEnd = target;
                    startPos = pagePosE;
                    endPos = this.mMaxItemsPerPage - 1;
                } else {
                    moveEnd = -1;
                    moveStart = -1;
                    startPos = pagePosE;
                    endPos = pagePosT;
                }
            } else {
                direction = -1;
                if (immediately) {
                    moveStart = empty;
                    moveEnd = target;
                    endPos = pagePosE;
                    startPos = pagePosE;
                } else if (pageE > pageToAnimate) {
                    if (pageT > pageToAnimate) {
                        moveStart = empty;
                        moveEnd = target;
                        endPos = pagePosT;
                        startPos = pagePosT;
                    } else {
                        moveStart = empty;
                        moveEnd = ((pageToAnimate + 1) * this.mMaxItemsPerPage) - 1;
                        startPos = this.mMaxItemsPerPage - 1;
                        endPos = pagePosT;
                    }
                } else if (pageE != pageToAnimate) {
                    moveStart = empty;
                    moveEnd = target;
                    endPos = pagePosE;
                    startPos = pagePosE;
                } else if (pageT < pageToAnimate) {
                    moveStart = pageToAnimate * this.mMaxItemsPerPage;
                    moveEnd = target;
                    startPos = pagePosE;
                    endPos = 0;
                } else {
                    moveEnd = -1;
                    moveStart = -1;
                    startPos = pagePosE;
                    endPos = pagePosT;
                }
            }
            while (moveStart != moveEnd) {
                int rankToMove = moveStart + direction;
                int p = rankToMove / this.mMaxItemsPerPage;
                int pagePos = rankToMove % this.mMaxItemsPerPage;
                int x = pagePos % this.mGridCountX;
                int y = pagePos / this.mGridCountX;
                page = getPageAt(p);
                if (page != null) {
                    v = page.getChildAt(x, y);
                    if (v != null) {
                        if (v.getTag() instanceof IconInfo) {
                            title = ((IconInfo) v.getTag()).title;
                        }
                        if (pageToAnimate != p || immediately) {
                            page.removeView(v);
                            if (v.getTag() instanceof IconInfo) {
                                addViewForRank(v, (IconInfo) v.getTag(), moveStart);
                            } else {
                                addViewForRank(v, moveStart);
                            }
                        } else {
                            float f;
                            int newRank = moveStart;
                            final float translationX = v.getTranslationX();
                            final int i = newRank;
                            Runnable endAction = new Runnable() {
                                public void run() {
                                    FolderPagedView.this.mPendingAnimations.remove(v);
                                    v.setTranslationX(translationX);
                                    if (v.getParent() != null) {
                                        ((CellLayout) v.getParent().getParent()).removeView(v);
                                    } else {
                                        Log.w(FolderPagedView.TAG, "realTimeReorder : parent already lost - " + v);
                                    }
                                    if (v.getTag() instanceof IconInfo) {
                                        FolderPagedView.this.addViewForRank(v, (IconInfo) v.getTag(), i);
                                    } else {
                                        FolderPagedView.this.addViewForRank(v, i);
                                    }
                                }
                            };
                            ViewPropertyAnimator animate = v.animate();
                            if (((direction > 0 ? 1 : 0) ^ Utilities.sIsRtl) != 0) {
                                f = (float) (-v.getWidth());
                            } else {
                                f = (float) v.getWidth();
                            }
                            animate.translationXBy(f).setDuration(230).setStartDelay(0).withEndAction(endAction);
                            this.mPendingAnimations.put(v, endAction);
                        }
                    } else {
                        Log.w(TAG, String.format("realTimeReorder : can't find x%d,y%d item in %d page (from=%d, to=%d)", new Object[]{Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(p), Integer.valueOf(rankToMove), Integer.valueOf(moveStart)}));
                    }
                } else {
                    Log.e(TAG, String.format("realTimeReorder : can't get %d page (from=%d, to=%d)", new Object[]{Integer.valueOf(p), Integer.valueOf(rankToMove), Integer.valueOf(moveStart)}));
                }
                moveStart = rankToMove;
            }
            if ((endPos - startPos) * direction > 0) {
                page = getPageAt(pageToAnimate);
                for (int i2 = startPos; i2 != endPos; i2 += direction) {
                    int nextPos = i2 + direction;
                    v = page.getChildAt(nextPos % this.mGridCountX, nextPos / this.mGridCountX);
                    if (v != null) {
                        LayoutParams lp = (LayoutParams) v.getLayoutParams();
                        lp.isLockedToGrid = true;
                        page.getCellLayoutChildren().setupLp(lp);
                        lp.isLockedToGrid = false;
                        if (v.getTag() instanceof ItemInfo) {
                            ItemInfo itemInfo = (ItemInfo) v.getTag();
                            itemInfo.rank -= direction;
                            title = ((ItemInfo) v.getTag()).title;
                        }
                        if (page.animateChildToPosition(v, i2 % this.mGridCountX, i2 / this.mGridCountX, REORDER_ANIMATION_DURATION, delay, true, true, (boolean[][]) null)) {
                            delay = (int) (((float) delay) + delayAmount);
                            delayAmount *= 0.9f;
                        }
                    } else {
                        Log.w(TAG, String.format("realTimeReorder : can't find %dth item in %d page (from=%d, to=%d)", new Object[]{Integer.valueOf(nextPos), Integer.valueOf(pageToAnimate), Integer.valueOf(nextPos), Integer.valueOf(i2)}));
                    }
                }
            }
        }
    }

    public void setMarkerScale(float scale) {
        int count = this.mPageIndicator.getChildCount();
        for (int i = 0; i < count; i++) {
            View marker = this.mPageIndicator.getChildAt(i);
            marker.animate().cancel();
            marker.setScaleX(scale);
            marker.setScaleY(scale);
        }
    }

    public void animateMarkers() {
        int count = this.mPageIndicator.getChildCount();
        Interpolator interpolator = new OvershootInterpolator(PAGE_INDICATOR_OVERSHOOT_TENSION);
        for (int i = 0; i < count; i++) {
            this.mPageIndicator.getChildAt(i).animate().scaleX(1.0f).scaleY(1.0f).setInterpolator(interpolator).setDuration(PAGE_INDICATOR_ANIMATION_DURATION).setStartDelay((PAGE_INDICATOR_ANIMATION_STAGGERED_DELAY * ((long) i)) + PAGE_INDICATOR_ANIMATION_START_DELAY);
        }
    }

    public int itemsPerPage() {
        return this.mMaxItemsPerPage;
    }

    private int allocateRankForAddButton() {
        int rank = getItemCount();
        ArrayList<View> views = new ArrayList(this.mFolder.getItemsInReadingOrder());
        views.add(rank, null);
        arrangeChildren(views, views.size(), false);
        return rank;
    }

    public void setAddButton(View addButton) {
        if (this.mAddButton != null) {
            if (this.mAddButton.getParent() != null) {
                ((ViewGroup) this.mAddButton.getParent()).removeView(this.mAddButton);
            }
            this.mAddButton = null;
        }
        this.mAddButton = addButton;
    }

    public void setHintPageWidth(int width) {
        this.mHintPageWidth = width;
    }

    public ArrayList<View> updateCheckBox(boolean visible) {
        ArrayList<View> folderItems = new ArrayList();
        int pageCount = getPageCount();
        for (int i = 0; i < pageCount; i++) {
            CellLayoutChildren clc = ((CellLayout) getChildAt(i)).getCellLayoutChildren();
            int childCount = clc.getChildCount();
            for (int j = 0; j < childCount; j++) {
                View v = clc.getChildAt(j);
                if (v instanceof IconView) {
                    folderItems.add(v);
                    ((IconView) v).updateCheckBox(visible);
                }
            }
        }
        return folderItems;
    }

    public void updateFolderGrid() {
        GridInfo folderGrid = LauncherAppState.getInstance().getDeviceProfile().folderGrid;
        this.mMaxCountX = folderGrid.getCellCountX();
        this.mMaxCountY = folderGrid.getCellCountY();
        this.mMaxItemsPerPage = this.mMaxCountX * this.mMaxCountY;
        int pageCount = getPageCount();
        for (int i = 0; i < pageCount; i++) {
            CellLayoutChildren clc = ((CellLayout) getChildAt(i)).getCellLayoutChildren();
            int childCount = clc.getChildCount();
            for (int j = 0; j < childCount; j++) {
                View v = clc.getChildAt(j);
                if (v instanceof IconView) {
                    ((IconView) v).applyStyle();
                    ((IconView) v).reapplyItemInfo((ItemInfo) v.getTag());
                }
            }
        }
    }

    public void onChangeFolderIconTextColor() {
        boolean isWhiteBg = this.mFolder.isWhiteBg();
        int pageCount = getPageCount();
        for (int i = 0; i < pageCount; i++) {
            CellLayoutChildren clc = ((CellLayout) getChildAt(i)).getCellLayoutChildren();
            int childCount = clc.getChildCount();
            for (int j = 0; j < childCount; j++) {
                View v = clc.getChildAt(j);
                if (v instanceof IconView) {
                    if (this.mFolder.getBaseController() instanceof AppsController) {
                        ((IconView) v).reapplyItemInfo((ItemInfo) v.getTag());
                        ((IconView) v).changeTextColorForBg(false);
                    } else {
                        ((IconView) v).reapplyItemInfo((ItemInfo) v.getTag());
                        ((IconView) v).changeTextColorForBg(isWhiteBg);
                    }
                }
            }
        }
    }

    public void updateCellDimensions() {
        int pageCount = getPageCount();
        for (int i = 0; i < pageCount; i++) {
            ((FolderCellLayout) getChildAt(i)).updateCellDimensionsIfNeeded();
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0 || ev.getAction() == 2) {
            this.mFolder.stopBounceAnimation();
        }
        return super.onTouchEvent(ev);
    }

    public boolean supportWhiteBg() {
        return !isAppsFolder();
    }

    protected boolean determineScrollingStart(MotionEvent ev, float touchSlopScale) {
        if (this.mFolder.getFolderState() == 3) {
            touchSlopScale *= 2.5f;
        }
        return super.determineScrollingStart(ev, touchSlopScale);
    }

    protected void notifyPageScroll(int page, int x, int y, int scrollX, int pageCount) {
        if (this.mFolder.getFolderState() == 3) {
            super.notifyPageScroll(page, x, y, scrollX, pageCount);
        }
    }

    protected void notifyPageChange(int page, int scrollX, int pageCount) {
        if (this.mFolder.getFolderState() == 3) {
            super.notifyPageChange(page, scrollX, pageCount);
        }
    }

    protected void updatePageTransform(View page, int index, int screenCenter) {
        if (this.mFolder.getFolderState() == 1) {
            super.updatePageTransform(page, index, screenCenter);
        }
    }

    public void scrollTo(int x, int y) {
        if (this.mFolder.getFolderState() != 3 || (x >= 0 && x <= getMaxScrollX())) {
            super.scrollTo(x, y);
        }
    }

    public void snapToPageSALogging(boolean isPageIndicator) {
        String screenID;
        String eventID;
        int method = isPageIndicator ? 1 : 0;
        Resources res = getResources();
        if (((Launcher) getContext()).getMultiSelectManager().isMultiSelectMode()) {
            screenID = this.mFolder.getInfo().isContainApps() ? res.getString(R.string.screen_AppsFolder_SelectMode) : res.getString(R.string.screen_HomeFolder_SelectMode);
            eventID = res.getString(R.string.event_Folder_SM_ChangePage);
        } else {
            screenID = this.mFolder.getInfo().isContainApps() ? res.getString(R.string.screen_AppsFolder_Primary) : res.getString(R.string.screen_HomeFolder_Primary);
            eventID = res.getString(R.string.event_FolderChangePage);
        }
        SALogging.getInstance().insertEventLog(screenID, eventID, (long) method);
    }

    public void onConfigurationChangedIfNeeded() {
        updateCellDimensions();
    }

    public void callRefreshLiveIcon() {
        int pageCount = getPageCount();
        for (int i = 0; i < pageCount; i++) {
            CellLayoutChildren clc = ((CellLayout) getChildAt(i)).getCellLayoutChildren();
            int childCount = clc.getChildCount();
            for (int j = 0; j < childCount; j++) {
                View v = clc.getChildAt(j);
                if (v instanceof IconView) {
                    ((IconView) v).onLiveIconRefresh();
                }
            }
        }
    }
}

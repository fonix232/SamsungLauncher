package com.android.launcher3.widget.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.provider.Settings.Secure;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;
import com.android.launcher3.Launcher;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.common.base.view.LauncherTransitionable;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.common.view.PageIndicator;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.android.launcher3.util.focus.FocusHelper;
import com.android.launcher3.util.logging.SALogging;
import com.android.launcher3.widget.controller.WidgetFocusHelper.ItemKeyEventListener;
import com.android.launcher3.widget.controller.WidgetFocusHelper.WidgetItemKeyListener;
import com.android.launcher3.widget.controller.WidgetState.State;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class WidgetPagedView extends PagedView implements LauncherTransitionable, OnClickListener, OnLongClickListener, OnTouchListener {
    private static final int PAGE_CACHE_SIZE = 3;
    public static final int PAGE_MINIMIZE = 0;
    public static final int PAGE_NORMALIZE = 1;
    private static final String TAG = "WidgetPagedView";
    private boolean mChildrenLayersEnabled;
    private List<Object> mDisplayWidgetItems;
    private boolean mDragInProgress;
    public boolean mDragOnSearchState;
    private Filter mFilter;
    private String mFilterString;
    private ItemKeyEventListener mItemKeyEventListener;
    private WidgetItemKeyListener mItemOnKeyListener;
    private final ItemViewPool mItemViewPool;
    private final LayoutInflater mLayoutInflater;
    private Listener mListener;
    private int mNumWidgetPages;
    private int mPageCacheSize;
    private List<Object> mWidgetItems;

    public interface Filter {
        List<Object> filterWidgets(List<Object> list);
    }

    public interface Listener {
        State getState();

        boolean isWhiteWallpaper();

        void onPagedViewFocusUp();

        void onPagedViewTouchIntercepted();

        void onSearchResult(boolean z);

        void onWidgetItemClick(View view);

        boolean onWidgetItemLongClick(View view);
    }

    private class ItemViewPool implements ViewRecycler {
        private static final int TYPE_FOLDER = 1;
        private static final int TYPE_SINGLE = 0;
        private final LayoutInflater mInflater;
        private final HashMap<Integer, List<ViewGroup>> mPool = new HashMap();

        public ItemViewPool(LayoutInflater inflater) {
            this.mInflater = inflater;
            this.mPool.put(Integer.valueOf(0), new ArrayList());
            this.mPool.put(Integer.valueOf(1), new ArrayList());
        }

        public synchronized ViewGroup get(boolean folder, ViewGroup parent) {
            ViewGroup viewGroup;
            int type = 0;
            synchronized (this) {
                if (folder) {
                    type = 1;
                }
                List<ViewGroup> recycleViews = (List) this.mPool.get(Integer.valueOf(type));
                if (recycleViews.size() > 0) {
                    viewGroup = (ViewGroup) recycleViews.remove(0);
                } else {
                    int id;
                    if (folder) {
                        id = WidgetPagedView.this.getWidgetItemFolderViewId();
                    } else {
                        id = WidgetPagedView.this.getWidgetItemSingleViewId();
                    }
                    viewGroup = (ViewGroup) this.mInflater.inflate(id, parent, false);
                }
            }
            return viewGroup;
        }

        public synchronized void recycle(ViewGroup view) {
            int type = view instanceof WidgetItemFolderView ? 1 : 0;
            if (view instanceof WidgetItemView) {
                ((WidgetItemView) view).resetToRecycle();
            }
            ((List) this.mPool.get(Integer.valueOf(type))).add(view);
        }

        public synchronized void clear() {
            for (List<ViewGroup> container : this.mPool.values()) {
                container.clear();
            }
        }
    }

    public abstract int getColumnCount();

    public abstract int getRowCount();

    public abstract int getWidgetItemFolderViewId();

    public abstract int getWidgetItemSingleViewId();

    public abstract int getWidgetPageLayoutId();

    public WidgetPagedView(Context context) {
        this(context, null);
    }

    public WidgetPagedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetPagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mWidgetItems = new ArrayList();
        this.mDisplayWidgetItems = new ArrayList();
        this.mDragOnSearchState = false;
        this.mDragInProgress = false;
        this.mPageCacheSize = 1;
        this.mChildrenLayersEnabled = false;
        this.mItemKeyEventListener = new ItemKeyEventListener() {
            public int getRowCount() {
                return WidgetPagedView.this.getRowCount();
            }

            public int getColumnCount() {
                return WidgetPagedView.this.getColumnCount();
            }

            public void focusToUp() {
                if (WidgetPagedView.this.mListener != null) {
                    WidgetPagedView.this.mListener.onPagedViewFocusUp();
                }
            }

            public void focusToPage(int direction, int keyCode) {
                int i = 1;
                ViewGroup page;
                int itemPos;
                View itemView;
                switch (direction) {
                    case 0:
                    case 1:
                        boolean next;
                        if (direction == 0) {
                            next = true;
                        } else {
                            next = false;
                        }
                        int currentPage = WidgetPagedView.this.getCurrentPage();
                        if (!next) {
                            i = -1;
                        }
                        int toPage = currentPage + i;
                        page = (ViewGroup) WidgetPagedView.this.getPageAt(toPage);
                        if (page != null) {
                            if (next) {
                                itemPos = 0;
                            } else {
                                itemPos = page.getChildCount() - 1;
                            }
                            if (WidgetPagedView.this.isScrolling()) {
                                WidgetPagedView.this.setCurrentPage(toPage);
                            }
                            WidgetPagedView.this.loadAssociatedPages(toPage);
                            itemView = page.getChildAt(itemPos);
                            if (itemView != null) {
                                itemView.requestFocus();
                                FocusHelper.playSoundEffect(keyCode, itemView);
                                return;
                            }
                            return;
                        }
                        return;
                    case 2:
                        page = (ViewGroup) WidgetPagedView.this.getPageAt(WidgetPagedView.this.getCurrentPage());
                        if (page != null) {
                            itemPos = -1;
                            if (keyCode == FolderLock.REQUEST_CODE_FOLDER_LOCK) {
                                itemPos = 0;
                            } else if (keyCode == FolderLock.REQUEST_CODE_FOLDER_UNLOCK) {
                                itemPos = page.getChildCount() - 1;
                            }
                            if (itemPos != -1) {
                                itemView = page.getChildAt(itemPos);
                                if (itemView != null) {
                                    itemView.requestFocus();
                                    FocusHelper.playSoundEffect(keyCode, itemView);
                                    return;
                                }
                                return;
                            }
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mItemViewPool = new ItemViewPool(this.mLayoutInflater);
        this.mItemOnKeyListener = new WidgetItemKeyListener(this.mItemKeyEventListener);
        setSaveEnabled(true);
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public void setWidgetItems(List<Object> widgets) {
        this.mWidgetItems.clear();
        this.mWidgetItems.addAll(widgets);
    }

    protected void onUnhandledTap(MotionEvent ev) {
    }

    private void updatePageCounts() {
        this.mNumWidgetPages = (int) Math.ceil((double) (((float) this.mDisplayWidgetItems.size()) / ((float) (getRowCount() * getColumnCount()))));
    }

    protected void onDataReady(int width, int height) {
        Object obj;
        updatePageCounts();
        String str = TAG;
        StringBuilder append = new StringBuilder().append("onDataReady done, mWidgetItems = ");
        if (this.mWidgetItems == null) {
            obj = "null";
        } else {
            obj = Integer.valueOf(this.mWidgetItems.size());
        }
        Log.d(str, append.append(obj).toString());
        runFilter(Math.max(0, getCurrentPage()), true);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (!(isDataReady() || this.mWidgetItems.isEmpty())) {
            setDataReady();
            setMeasuredDimension(width, height);
            onDataReady(width, height);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void invalidateWigetItems() {
        if (!this.mDragInProgress) {
            PageIndicator pageIndicator = getPageIndicator();
            if (pageIndicator != null) {
                LayoutParams lp2 = (LayoutParams) pageIndicator.getLayoutParams();
                if (this instanceof WidgetFolderPagedView) {
                    lp2.bottomMargin = 0;
                } else {
                    lp2.bottomMargin = getResources().getDimensionPixelSize(R.dimen.widget_page_indicator_bottom_margin);
                }
                pageIndicator.disableLayoutTransitions();
            }
            runFilter();
            if (pageIndicator != null) {
                pageIndicator.enableLayoutTransitions();
            }
            if (!isDataReady()) {
                requestLayout();
            }
        }
    }

    private void runFilter() {
        runFilter(-1, false);
    }

    private void runFilter(int page, boolean force) {
        this.mDisplayWidgetItems.clear();
        if (this.mFilter != null) {
            this.mDisplayWidgetItems.addAll(this.mFilter.filterWidgets(this.mWidgetItems));
        } else {
            this.mDisplayWidgetItems.addAll(this.mWidgetItems);
        }
        if (this.mListener != null) {
            this.mListener.onSearchResult(!this.mDisplayWidgetItems.isEmpty());
        }
        updatePageCounts();
        invalidatePageData(page != -1 ? page : getCurrentPage());
    }

    public void onClick(View v) {
        if ((v instanceof WidgetItemView) && this.mListener != null) {
            this.mListener.onWidgetItemClick(v);
        }
    }

    protected void syncPages() {
        boolean whiteWallpaper;
        removeAllViews();
        this.mItemViewPool.clear();
        if (this.mListener != null) {
            whiteWallpaper = this.mListener.isWhiteWallpaper();
        } else {
            whiteWallpaper = false;
        }
        for (int j = 0; j < this.mNumWidgetPages; j++) {
            WidgetPageLayout layout = (WidgetPageLayout) this.mLayoutInflater.inflate(getWidgetPageLayoutId(), this, false);
            layout.setViewRecycler(this.mItemViewPool);
            layout.setItemOnKeyListener(this.mItemOnKeyListener);
            layout.changeColorForBg(whiteWallpaper);
            addView(layout);
        }
    }

    public void syncPageItems(int page, boolean immediate) {
        syncWidgetPageItems(page, immediate);
    }

    private void syncWidgetPageItems(int pageIndex, boolean immediate) {
        int numItemsPerPage = getRowCount() * getColumnCount();
        int offset = pageIndex * numItemsPerPage;
        int end = Math.min(offset + numItemsPerPage, this.mDisplayWidgetItems.size());
        if (offset <= end) {
            List<Object> items = this.mDisplayWidgetItems.subList(offset, end);
            Log.i(TAG, "syncWidgetPageItems Page: " + pageIndex + " immediate " + immediate + " subListOffsets " + offset + ", " + end);
            WidgetPageLayout page = (WidgetPageLayout) getPageAt(pageIndex);
            page.removeAllViews();
            page.bindItems(items, this.mFilterString, getState());
            int size = page.getChildCount();
            for (int i = 0; i < size; i++) {
                WidgetItemView itemView = (WidgetItemView) page.getChildAt(i);
                itemView.setOnClickListener(this);
                if (itemView.supportLongClick()) {
                    itemView.setOnLongClickListener(this);
                }
            }
        }
    }

    protected void onPageEndMoving() {
        super.onPageEndMoving();
        updateChildrenLayersEnabled(false);
        setAccessibilityFocusChange(true);
    }

    protected String getCurrentPageDescription() {
        return String.format(getContext().getString(R.string.default_scroll_format), new Object[]{Integer.valueOf(getNextPage() + 1), Integer.valueOf(this.mNumWidgetPages)});
    }

    protected void onPageBeginMoving() {
        updateChildrenLayersEnabled(false);
        performAccessibilityAction(128, null);
        setAccessibilityFocusChange(false);
        super.onPageBeginMoving();
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case 0:
                if (this.mListener != null) {
                    this.mListener.onPagedViewTouchIntercepted();
                    break;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    public void prepareInOut(int level, boolean invalidate) {
        int cacheSize = this.mPageCacheSize;
        if (level == 1 && invalidate) {
            invalidateWigetItems();
        }
        this.mPageCacheSize = level == 1 ? 3 : 1;
        if (cacheSize != this.mPageCacheSize) {
            loadAssociatedPages(getCurrentPage());
            if (this.mPageCacheSize == 1) {
                this.mItemViewPool.clear();
            }
        }
        Log.d(TAG, "prepareInOut items : " + this.mWidgetItems.size());
    }

    protected void setAccessibilityFocusChange(boolean enable) {
        WidgetPageLayout layout = (WidgetPageLayout) getChildAt(getNextPage());
        if (layout != null) {
            layout.setAccessibilityEnabled(enable);
        }
    }

    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }

    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        super.addFocusables(views, direction, focusableMode);
    }

    public boolean handleKeyEvent(int keyCode) {
        return false;
    }

    public void clearAccessibilityFocus() {
        if (Secure.getInt(getContext().getContentResolver(), "accessibility_enabled", 0) == 1) {
            Log.i(TAG, "Try to clear accessibility focus in widgets");
            WidgetPageLayout page = (WidgetPageLayout) getChildAt(getCurrentPage());
            if (page != null) {
                for (int i = 0; i < page.getChildCount(); i++) {
                    View v = page.getChildAt(i);
                    if (v != null && v.isAccessibilityFocused()) {
                        v.performAccessibilityAction(128, null);
                    }
                }
            }
        }
    }

    public void notifyChangeState(State toState, State fromState) {
        int index = getCurrentPage();
        int count = getChildCount();
        int i = 0;
        while (i < count) {
            View view = getChildAt(i);
            if (view instanceof WidgetPageLayout) {
                ((WidgetPageLayout) view).changeState(toState, index == i);
            }
            i++;
        }
    }

    public void changeColorForBg(boolean whiteBg) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if (view instanceof WidgetPageLayout) {
                ((WidgetPageLayout) view).changeColorForBg(whiteBg);
            }
        }
        PageIndicator pageIndicator = getPageIndicator();
        if (pageIndicator != null) {
            pageIndicator.changeColorForBg(whiteBg);
        }
    }

    public void updateCellSpan() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if (view instanceof WidgetPageLayout) {
                ((WidgetPageLayout) view).updateCellSpan();
            }
        }
    }

    public State getState() {
        return this.mListener != null ? this.mListener.getState() : State.NORMAL;
    }

    public void setSearchFilter(Filter filter) {
        this.mFilter = filter;
    }

    public void applySearchResult(String searchString) {
        runFilter(0, false);
    }

    public void setSearchString(String searchString) {
        this.mFilterString = searchString;
    }

    public void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace, StageEntry data) {
    }

    public void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace, StageEntry data) {
    }

    public void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace, StageEntry data) {
    }

    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    public boolean onLongClick(View v) {
        if (!(v instanceof WidgetItemView) || this.mListener == null) {
            return false;
        }
        return this.mListener.onWidgetItemLongClick(v);
    }

    public int getPageCacheSize() {
        return this.mPageCacheSize;
    }

    protected boolean isContentsRefreshable() {
        return true;
    }

    public void updateChildrenLayersEnabled(boolean force) {
        boolean enableChildrenLayers;
        if (force || isPageMoving()) {
            enableChildrenLayers = true;
        } else {
            enableChildrenLayers = false;
        }
        if (enableChildrenLayers != this.mChildrenLayersEnabled) {
            this.mChildrenLayersEnabled = enableChildrenLayers;
            if (this.mChildrenLayersEnabled) {
                enableHwLayersOnVisiblePages();
                return;
            }
            for (int i = 0; i < getPageCount(); i++) {
                ((WidgetPageLayout) getChildAt(i)).enableHardwareLayers(false);
            }
        }
    }

    private void enableHwLayersOnVisiblePages() {
        if (this.mChildrenLayersEnabled) {
            int screenCount = getChildCount();
            getVisiblePages(this.mTempVisiblePagesRange);
            int leftScreen = this.mTempVisiblePagesRange[0];
            int rightScreen = this.mTempVisiblePagesRange[1];
            if (leftScreen == rightScreen) {
                if (rightScreen < screenCount - 1) {
                    rightScreen++;
                } else if (leftScreen > 0) {
                    leftScreen--;
                }
            }
            int i = 0;
            while (i < screenCount) {
                boolean enableLayer;
                WidgetPageLayout layout = (WidgetPageLayout) getPageAt(i);
                if (leftScreen > i || i > rightScreen) {
                    enableLayer = false;
                } else {
                    enableLayer = true;
                }
                layout.enableHardwareLayers(enableLayer);
                i++;
            }
        }
    }

    public int getDisplayItemCount() {
        return this.mDisplayWidgetItems.size();
    }

    public Object getDisplayItem(int idx) {
        if (this.mDisplayWidgetItems.size() - 1 >= idx) {
            return this.mDisplayWidgetItems.get(idx);
        }
        return null;
    }

    public void onConfigurationChangedIfNeeded() {
        if (isPageMoving()) {
            pageEndMoving();
        }
        invalidateWigetItems();
    }

    public WidgetItemView findItemView(PendingAddItemInfo findInfo) {
        if (findInfo == null) {
            Log.w(TAG, "findInfo is null");
            return null;
        }
        ComponentName componentName = findInfo.componentName;
        UserHandleCompat userHandle = findInfo.getUserHandle();
        int pageCount = getChildCount();
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            WidgetPageLayout page = (WidgetPageLayout) getChildAt(pageIndex);
            if (page instanceof WidgetPageLayout) {
                int itemCount = page.getChildCount();
                for (int itemIndex = 0; itemIndex < itemCount; itemIndex++) {
                    WidgetItemView child = (WidgetItemView) page.getChildAt(itemIndex);
                    if (child instanceof WidgetItemView) {
                        PendingAddItemInfo info = (PendingAddItemInfo) child.getWidgets().get(0);
                        if (info != null && componentName.equals(info.componentName) && userHandle.equals(info.getUserHandle())) {
                            return child;
                        }
                    }
                }
                continue;
            }
        }
        Log.w(TAG, "can not find the anchorview");
        return null;
    }

    public void snapToPageSALogging(boolean isPageIndicator) {
        int method = isPageIndicator ? 1 : 0;
        Resources res = getResources();
        if (this instanceof WidgetFolderPagedView) {
            SALogging.getInstance().insertEventLog(res.getString(R.string.screen_WidgetTray), res.getString(R.string.event_ChangePageIntray), (long) method);
        } else {
            SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Widgets), res.getString(R.string.event_ChangePage), (long) method);
        }
    }
}

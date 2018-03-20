package com.android.launcher3.common.drag;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.controller.AppsDragController;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.multiselect.MultiSelectManager;
import com.android.launcher3.common.quickoption.QuickOptionView;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.home.CancelDropTarget;
import com.android.launcher3.home.HotseatDragController;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.alarm.Alarm;
import com.android.launcher3.util.alarm.OnAlarmListener;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DragManager {
    private static final Comparator<DragObject> CONTAINER_COMPARATOR = new Comparator<DragObject>() {
        public final int compare(DragObject a, DragObject b) {
            ItemInfo aInfo = a.dragInfo;
            ItemInfo bInfo = b.dragInfo;
            if (aInfo.container == bInfo.container) {
                return 0;
            }
            return aInfo.container < bInfo.container ? -1 : 1;
        }
    };
    private static final long DELAY_TO_MOVE_EXTRA_ITEM = 10;
    public static final int DRAG_ACTION_COPY = 1;
    public static final int DRAG_ACTION_MOVE = 0;
    private static final Comparator<DragSource> DRAG_SOURCE_COMPARATOR = new Comparator<DragSource>() {
        public final int compare(DragSource a, DragSource b) {
            int aInfo = a.getDragSourceType();
            int bInfo = b.getDragSourceType();
            if (aInfo == bInfo) {
                return 0;
            }
            return aInfo < bInfo ? 1 : -1;
        }
    };
    private static final int MOVE_STAGE_TIME = 1000;
    private static final boolean PROFILE_DRAWING_DURING_DRAG = false;
    public static final int RESCROLL_DELAY = (PagedView.PAGE_SNAP_ANIMATION_DURATION + 150);
    public static final long RESET_LAST_TOUCH_UP_TIME = -1;
    public static final int SCROLL_DELAY = 500;
    public static final int SCROLL_LEFT = 0;
    public static final int SCROLL_NONE = -1;
    private static final int SCROLL_OUTSIDE_ZONE = 0;
    public static final int SCROLL_RIGHT = 1;
    private static final int SCROLL_WAITING_IN_ZONE = 1;
    private static final String TAG = "Launcher.DragManager";
    private final int[] mCoordinatesTemp = new int[2];
    private int mDistanceSinceScroll = 0;
    private Rect mDragLayerRect = new Rect();
    private DragObject mDragObject;
    private DragScroller mDragScroller;
    private boolean mDragging;
    private int mDropAnimationMaxDuration;
    private ArrayList<DropTarget> mDropTargets = new ArrayList();
    private Handler mHandler;
    private InputMethodManager mInputMethodManager;
    private boolean mIsInScrollArea;
    private DropTarget mLastDropTarget;
    private int[] mLastTouch = new int[2];
    private long mLastTouchUpTime = -1;
    private Launcher mLauncher;
    private ArrayList<DragListener> mListeners = new ArrayList();
    private int mMotionDownX;
    private int mMotionDownY;
    private Alarm mMoveStageAlarm;
    private View mMoveTarget;
    private MultiSelectManager mMultiSelectManager = null;
    private int mOutlineColor;
    private QuickOptionView mQuickOptionView = null;
    private Rect mRectTemp = new Rect();
    private ScrollRunnable mScrollRunnable = new ScrollRunnable();
    private int mScrollState = 0;
    private View mScrollView;
    private int mScrollZone;
    private int[] mTmpPoint = new int[2];
    private IBinder mWindowToken;

    public interface DragListener {
        boolean onDragEnd();

        boolean onDragStart(DragSource dragSource, Object obj, int i);
    }

    private class ScrollRunnable implements Runnable {
        private int mDirection;

        ScrollRunnable() {
        }

        public void run() {
            if (DragManager.this.mDragScroller != null) {
                if (this.mDirection == 0) {
                    DragManager.this.mDragScroller.scrollLeft();
                } else {
                    DragManager.this.mDragScroller.scrollRight();
                }
                DragManager.this.mScrollState = 0;
                DragManager.this.mDistanceSinceScroll = 0;
                DragManager.this.mDragScroller.onExitScrollArea();
                if (DragManager.this.isDragging()) {
                    DragManager.this.checkScrollState(DragManager.this.mLastTouch[0], DragManager.this.mLastTouch[1]);
                }
            }
        }

        void setDirection(int direction) {
            this.mDirection = direction;
        }
    }

    public DragManager(Launcher launcher) {
        this.mLauncher = launcher;
        this.mHandler = new Handler();
        this.mDropAnimationMaxDuration = this.mLauncher.getResources().getInteger(R.integer.config_dropAnimMaxDuration);
        this.mMoveStageAlarm = new Alarm();
        if (LauncherFeature.supportMultiSelect()) {
            this.mMultiSelectManager = this.mLauncher.getMultiSelectManager();
        }
    }

    public boolean dragging() {
        return this.mDragging;
    }

    public void startDrag(View draggedView, Bitmap draggedBmp, DragSource source, Object dragInfo, Rect viewImageBounds, int dragAction, float initialDragViewScale, Drawable outline, boolean alignCenter) {
        startDrag(draggedView, draggedBmp, source, dragInfo, viewImageBounds, dragAction, initialDragViewScale, outline, alignCenter, null);
    }

    public void startDrag(View draggedView, Bitmap draggedBmp, DragSource source, Object dragInfo, Rect viewImageBounds, int dragAction, float initialDragViewScale, Drawable outline, boolean alignCenter, Point point) {
        int[] loc = this.mCoordinatesTemp;
        this.mLauncher.getDragLayer().getLocationInDragLayer(draggedView, loc);
        int offsetX = alignCenter ? (draggedView.getWidth() - draggedBmp.getWidth()) / 2 : 0;
        int offsetY = alignCenter ? (draggedView.getHeight() - draggedBmp.getHeight()) / 2 : 0;
        if (point != null) {
            this.mMotionDownX = point.x;
            this.mMotionDownY = point.y;
        }
        DragView dragView = startDrag(draggedView, draggedBmp, ((loc[0] + viewImageBounds.left) + offsetX) + ((int) (((((float) draggedBmp.getWidth()) * initialDragViewScale) - ((float) draggedBmp.getWidth())) / 2.0f)), ((loc[1] + viewImageBounds.top) + offsetY) + ((int) (((((float) draggedBmp.getHeight()) * initialDragViewScale) - ((float) draggedBmp.getHeight())) / 2.0f)), source, dragInfo, dragAction, null, null, initialDragViewScale, outline, false, false);
        if ((dragInfo instanceof ItemInfo) && ((ItemInfo) dragInfo).itemType == 6) {
            dragView.setIntrinsicIconSize(draggedBmp.getHeight());
        }
        if (dragAction == 0) {
            draggedView.setVisibility(View.GONE);
        }
    }

    public DragView startDrag(View draggedView, Bitmap draggedBmp, int dragLayerX, int dragLayerY, DragSource source, Object dragInfo, int dragAction, Point dragOffset, Rect dragRegion, float initialDragViewScale, Drawable outline, boolean allowQuickOption, boolean fromEmptyCell) {
        int i;
        if (source != null) {
            this.mOutlineColor = source.getOutlineColor();
        }
        if (this.mInputMethodManager == null) {
            this.mInputMethodManager = (InputMethodManager) this.mLauncher.getSystemService("input_method");
        }
        this.mInputMethodManager.hideSoftInputFromWindow(this.mWindowToken, 0);
        Iterator it = this.mListeners.iterator();
        while (it.hasNext()) {
            ((DragListener) it.next()).onDragStart(source, dragInfo, dragAction);
        }
        int emptyCellOffsetX = draggedBmp.getWidth() / 2;
        int emptyCellOffsetY = draggedBmp.getHeight() / 2;
        int registrationX = fromEmptyCell ? emptyCellOffsetX : this.mMotionDownX - dragLayerX;
        int registrationY = fromEmptyCell ? emptyCellOffsetY : this.mMotionDownY - dragLayerY;
        int dragRegionLeft = dragRegion == null ? 0 : dragRegion.left;
        int dragRegionTop = dragRegion == null ? 0 : dragRegion.top;
        this.mDragging = true;
        this.mDragObject = new DragObject();
        this.mDragObject.dragComplete = false;
        this.mDragObject.xOffset = fromEmptyCell ? emptyCellOffsetX : this.mMotionDownX - (dragLayerX + dragRegionLeft);
        DragObject dragObject = this.mDragObject;
        if (fromEmptyCell) {
            i = emptyCellOffsetY;
        } else {
            i = this.mMotionDownY - (dragLayerY + dragRegionTop);
        }
        dragObject.yOffset = i;
        this.mDragObject.dragSource = source;
        this.mDragObject.dragInfo = dragInfo;
        DragObject dragObject2 = this.mDragObject;
        DragView dragView = new DragView(this.mLauncher, draggedBmp, registrationX, registrationY, 0, 0, draggedBmp.getWidth(), draggedBmp.getHeight(), initialDragViewScale);
        dragObject2.dragView = dragView;
        dragView.setDragOutline(outline);
        this.mDragObject.dragView.setSourceView(draggedView);
        if (dragOffset != null) {
            dragView.setDragVisualizeOffset(new Point(dragOffset));
        }
        if (dragRegion != null) {
            dragView.setDragRegion(new Rect(dragRegion));
        }
        if (fromEmptyCell) {
            dragView.setTargetOffset(registrationX - emptyCellOffsetX, registrationY - emptyCellOffsetY);
        }
        if (this.mMultiSelectManager != null) {
            ArrayList<View> arrayList = new ArrayList(this.mMultiSelectManager.getCheckedAppsViewList());
            if (((dragInfo instanceof FolderInfo) || (dragInfo instanceof IconInfo)) && arrayList.size() > 0) {
                ArrayList<DragObject> extraDragObjects = new ArrayList();
                ArrayList<DragSource> extraDragSources = new ArrayList();
                int extraOrder = 0;
                Iterator it2 = arrayList.iterator();
                while (it2.hasNext()) {
                    int i2;
                    View checkedApp = (View) it2.next();
                    DragObject dragObject3 = new DragObject();
                    dragObject3.dragSource = this.mMultiSelectManager.getCheckedAppDragSource(checkedApp.hashCode());
                    dragObject3.dragInfo = checkedApp.getTag();
                    int[] tempPt = new int[2];
                    AtomicInteger atomicInteger = new AtomicInteger(6);
                    Bitmap viewBitmap = DragViewHelper.createDragBitmap(checkedApp, atomicInteger, false);
                    int bmpWidth = viewBitmap.getWidth();
                    int bmpHeight = viewBitmap.getHeight();
                    float checkedAppScale = this.mLauncher.getDragLayer().getLocationInDragLayer(checkedApp, tempPt);
                    int checkAppWidth = this.mLauncher.getDeviceProfile().isLandscape ? DragViewHelper.getBadgeIconViewWidth((IconView) checkedApp) : checkedApp.getWidth();
                    int checkedAppX = Math.round(((float) tempPt[0]) - ((((float) bmpWidth) - (((float) checkAppWidth) * checkedAppScale)) / 2.0f));
                    int checkedAppY = Math.round((((float) tempPt[1]) - ((((float) bmpHeight) - (((float) bmpHeight) * checkedAppScale)) / 2.0f)) - (((float) atomicInteger.get()) / 2.0f));
                    float scaleFactor = (((float) dragView.getDragRegionWidth()) * 1.0f) / ((float) (bmpWidth - atomicInteger.get()));
                    int scaleValue = (int) (((float) Math.abs(draggedBmp.getWidth() - bmpWidth)) * (scaleFactor - 1.0f));
                    int deltaX = dragView.getDragRegionLeft() - atomicInteger.get();
                    int deltaY = dragView.getDragRegionTop() - atomicInteger.get();
                    int pageDeltaX = 0;
                    ItemInfo itemInfo = (ItemInfo) checkedApp.getTag();
                    if (itemInfo.container > 0 && dragObject3.dragSource != null) {
                        pageDeltaX = (this.mDragObject.dragSource.getPageIndexForDragView(null) - dragObject3.dragSource.getPageIndexForDragView(itemInfo)) * this.mLauncher.getResources().getDisplayMetrics().widthPixels;
                    }
                    int landStartDelta = (!this.mLauncher.getDeviceProfile().isLandscape || LauncherFeature.isTablet()) ? 0 : ((checkAppWidth - bmpWidth) / 2) - ((IconView) checkedApp).getIconInfo().getIconStartPadding();
                    int extraViewOffsetDelta = this.mLauncher.getResources().getDimensionPixelSize(R.dimen.multi_select_extra_view_delta_offset);
                    extraOrder++;
                    DragView extraDragView = new DragView(this.mLauncher, viewBitmap, ((this.mMotionDownX - checkedAppX) - (((extraViewOffsetDelta * extraOrder) + deltaX) + scaleValue)) + pageDeltaX, (this.mMotionDownY - checkedAppY) - (((extraViewOffsetDelta * extraOrder) + deltaY) + scaleValue), 0, 0, bmpWidth, bmpHeight, checkedAppScale, scaleFactor, true);
                    dragObject3.dragView = extraDragView;
                    extraDragView.setSourceView(checkedApp);
                    if (fromEmptyCell) {
                        i2 = (((this.mMotionDownX - checkedAppX) + pageDeltaX) - emptyCellOffsetX) - landStartDelta;
                    } else {
                        i2 = ((dragLayerX - checkedAppX) + pageDeltaX) - landStartDelta;
                    }
                    if (fromEmptyCell) {
                        i = (this.mMotionDownY - checkedAppY) - emptyCellOffsetY;
                    } else {
                        i = dragLayerY - checkedAppY;
                    }
                    extraDragView.setTargetOffset(i2, i);
                    extraDragView.setTopDelta(deltaY);
                    if (dragOffset != null) {
                        extraDragView.setDragVisualizeOffset(new Point(dragOffset));
                    }
                    extraDragObjects.add(dragObject3);
                    if (!extraDragSources.contains(dragObject3.dragSource)) {
                        extraDragSources.add(dragObject3.dragSource);
                    }
                }
                if (extraDragObjects.size() > 0) {
                    this.mDragObject.extraDragInfoList = extraDragObjects;
                    this.mDragObject.extraDragSourceList = extraDragSources;
                    for (int i3 = this.mDragObject.extraDragInfoList.size() - 1; i3 >= 0; i3--) {
                        DragObject extraDragObject = (DragObject) this.mDragObject.extraDragInfoList.get(i3);
                        if (extraDragObject.dragView != null) {
                            extraDragObject.dragView.show(this.mMotionDownX, this.mMotionDownY);
                        }
                    }
                    Iterator it3 = this.mDragObject.extraDragSourceList.iterator();
                    while (it3.hasNext()) {
                        DragSource extraDragSource = (DragSource) it3.next();
                        ArrayList<DragObject> targetExtraDragObjects = new ArrayList();
                        int dragObjectRank = ((ItemInfo) this.mDragObject.dragInfo).rank;
                        int count = 0;
                        it = this.mDragObject.extraDragInfoList.iterator();
                        while (it.hasNext()) {
                            DragObject d = (DragObject) it.next();
                            if (extraDragSource.equals(d.dragSource)) {
                                targetExtraDragObjects.add(d);
                            }
                            if (extraDragSource instanceof AppsDragController) {
                                count = countItemsBelowThanDraggedItemRank(d, count);
                            }
                        }
                        int emptyRank = dragObjectRank - count;
                        int screenId = (int) ((ItemInfo) this.mDragObject.dragInfo).screenId;
                        extraDragSource.onExtraObjectDragged(targetExtraDragObjects);
                        if (extraDragSource instanceof AppsDragController) {
                            ((AppsDragController) extraDragSource).onAdjustDraggedObjectPosition(this.mDragObject, emptyRank, dragObjectRank, screenId);
                        }
                    }
                }
            }
        }
        dragView.show(this.mMotionDownX, this.mMotionDownY);
        if (LauncherFeature.supportQuickOption() && allowQuickOption) {
            createQuickOptionView(this.mDragObject);
        }
        if (!(this.mMoveStageAlarm == null || source == null)) {
            final DragSource dragSource;
            if (source.getDragSourceType() == 1 || source.getDragSourceType() == 4) {
                this.mMoveStageAlarm.cancelAlarm();
                this.mMoveStageAlarm.setAlarm(1000);
                dragSource = source;
                this.mMoveStageAlarm.setOnAlarmListener(new OnAlarmListener() {
                    public void onAlarm(Alarm alarm) {
                        if (LauncherFeature.supportQuickOption()) {
                            DragManager.this.removeQuickOptionView();
                        }
                        if (DragManager.this.mLauncher != null && DragManager.this.mLauncher.getTrayManager() != null && !DragManager.this.mLauncher.isHomeStage()) {
                            DragManager.this.mLauncher.getTrayManager().changeStageWithDrag(dragSource);
                        }
                    }
                });
            } else if (source.getDragSourceType() == 6 || source.getDragSourceType() == 7) {
                this.mMoveStageAlarm.cancelAlarm();
                this.mMoveStageAlarm.setAlarm(100);
                dragSource = source;
                this.mMoveStageAlarm.setOnAlarmListener(new OnAlarmListener() {
                    public void onAlarm(Alarm alarm) {
                        if (DragManager.this.mLauncher != null && !DragManager.this.mLauncher.isHomeStage()) {
                            if (!DragManager.this.mLauncher.isFolderStage() || DragManager.this.mLauncher.getSecondTopStageMode() != 1) {
                                DragManager.this.mLauncher.getTrayManager().changeStageWithDrag(dragSource);
                            }
                        }
                    }
                });
            }
        }
        return dragView;
    }

    public boolean dispatchKeyEvent() {
        return this.mDragging;
    }

    public boolean isDragging() {
        return this.mDragging;
    }

    public void cancelDrag() {
        if (this.mMoveStageAlarm != null) {
            this.mMoveStageAlarm.cancelAlarm();
        }
        if (this.mDragging) {
            this.mDragObject.deferDragViewCleanupPostAnimation = false;
            this.mDragObject.cancelled = true;
            this.mDragObject.dragComplete = true;
            if (this.mLastDropTarget != null) {
                this.mLastDropTarget.onDragExit(this.mDragObject, false);
            }
            this.mDragObject.dragSource.onDropCompleted(null, this.mDragObject, false);
            if (!(this.mDragObject.extraDragInfoList == null || this.mDragObject.extraDragSourceList == null)) {
                Iterator it = this.mDragObject.extraDragSourceList.iterator();
                while (it.hasNext()) {
                    DragSource extraDragSource = (DragSource) it.next();
                    ArrayList<DragObject> targetExtraDragObjects = new ArrayList();
                    Iterator it2 = this.mDragObject.extraDragInfoList.iterator();
                    while (it2.hasNext()) {
                        DragObject d = (DragObject) it2.next();
                        if (extraDragSource.equals(d.dragSource)) {
                            d.cancelled = true;
                            d.dragComplete = true;
                            targetExtraDragObjects.add(d);
                        }
                    }
                    extraDragSource.onExtraObjectDropCompleted(null, null, targetExtraDragObjects, 0);
                }
            }
        }
        endDrag();
    }

    public void onAppsRemoved(ArrayList<String> packageNames, HashSet<ComponentName> cns) {
        removePossibleQuickOptionView(cns);
        if (this.mDragObject != null) {
            IconInfo rawDragInfo = this.mDragObject.dragInfo;
            if (rawDragInfo instanceof IconInfo) {
                IconInfo dragInfo = rawDragInfo;
                Iterator it = cns.iterator();
                while (it.hasNext()) {
                    ComponentName componentName = (ComponentName) it.next();
                    if (dragInfo.intent != null) {
                        ComponentName cn = dragInfo.intent.getComponent();
                        boolean isSameComponent = cn != null && (cn.equals(componentName) || (packageNames != null && packageNames.contains(cn.getPackageName())));
                        if (isSameComponent) {
                            cancelDrag();
                            return;
                        }
                    }
                }
            }
        }
    }

    public void onAppsRemoved(ItemInfoMatcher matcher) {
        if (this.mDragObject != null) {
            IconInfo rawDragInfo = this.mDragObject.dragInfo;
            if (rawDragInfo instanceof IconInfo) {
                IconInfo dragInfo = rawDragInfo;
                ComponentName cn = dragInfo.getTargetComponent();
                if (cn != null && matcher.matches(dragInfo, cn)) {
                    cancelDrag();
                }
            }
        }
    }

    private void removePossibleQuickOptionView(HashSet<ComponentName> cns) {
        if (LauncherFeature.supportQuickOption() && isQuickOptionShowing()) {
            ItemInfo info = this.mQuickOptionView.getItemInfo();
            if (info != null && info.componentName != null && cns != null && cns.contains(info.componentName)) {
                removeQuickOptionView();
            }
        }
    }

    private void endDrag() {
        if (this.mDragging) {
            Iterator it;
            this.mDragging = false;
            clearScrollRunnable();
            boolean isDeferred = false;
            if (this.mDragObject.dragView != null) {
                isDeferred = this.mDragObject.deferDragViewCleanupPostAnimation;
                if (isDeferred) {
                    final DragObject deferredDragObject = this.mDragObject;
                    final DragView deferredDragView = this.mDragObject.dragView;
                    final Object deferredDragInfo = this.mDragObject.dragInfo;
                    deferredDragView.postDelayed(new Runnable() {
                        public void run() {
                            boolean isWidget = (deferredDragInfo instanceof PendingAddItemInfo) && (((PendingAddItemInfo) deferredDragInfo).getProviderInfo() instanceof LauncherAppWidgetProviderInfo);
                            if (deferredDragView.getParent() != null && !isWidget) {
                                deferredDragView.remove();
                                Log.e(DragManager.TAG, "force remove deferredDragView - cancelled=" + deferredDragObject.cancelled + ", cancelDropFolder=" + deferredDragObject.cancelDropFolder + ", restored=" + deferredDragObject.restored + ", source=" + deferredDragObject.dragSource + ", info=" + deferredDragObject.dragInfo);
                            }
                        }
                    }, (long) this.mDropAnimationMaxDuration);
                    if (this.mDragObject.extraDragInfoList != null) {
                        it = this.mDragObject.extraDragInfoList.iterator();
                        while (it.hasNext()) {
                            DragObject extraDrag = (DragObject) it.next();
                            final DragView extraDragView = extraDrag.dragView;
                            if (extraDrag.deferDragViewCleanupPostAnimation) {
                                extraDragView.postDelayed(new Runnable() {
                                    public void run() {
                                        extraDragView.remove();
                                    }
                                }, (long) this.mDropAnimationMaxDuration);
                            } else {
                                extraDragView.remove();
                            }
                        }
                    }
                } else {
                    this.mDragObject.dragView.remove();
                    if (this.mDragObject.extraDragInfoList != null) {
                        it = this.mDragObject.extraDragInfoList.iterator();
                        while (it.hasNext()) {
                            ((DragObject) it.next()).dragView.remove();
                        }
                    }
                }
                this.mDragObject.dragView = null;
                this.mDragObject.extraDragInfoList = null;
                this.mDragObject.extraDragSourceList = null;
            }
            if (!isDeferred) {
                it = new ArrayList(this.mListeners).iterator();
                while (it.hasNext()) {
                    ((DragListener) it.next()).onDragEnd();
                }
            }
        }
    }

    public void onDeferredEndDrag(DragView dragView) {
        dragView.remove();
        if (this.mDragObject.deferDragViewCleanupPostAnimation) {
            Iterator it = new ArrayList(this.mListeners).iterator();
            while (it.hasNext()) {
                ((DragListener) it.next()).onDragEnd();
            }
        }
    }

    private int[] getClampedDragLayerPos(float x, float y) {
        this.mLauncher.getDragLayer().getLocalVisibleRect(this.mDragLayerRect);
        this.mTmpPoint[0] = (int) Math.max((float) this.mDragLayerRect.left, Math.min(x, (float) (this.mDragLayerRect.right - 1)));
        this.mTmpPoint[1] = (int) Math.max((float) this.mDragLayerRect.top, Math.min(y, (float) (this.mDragLayerRect.bottom - 1)));
        return this.mTmpPoint;
    }

    public long getLastGestureUpTime() {
        if (this.mDragging) {
            return System.currentTimeMillis();
        }
        return this.mLastTouchUpTime;
    }

    public void resetLastGestureUpTime() {
        this.mLastTouchUpTime = -1;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        int[] dragLayerPos = getClampedDragLayerPos(ev.getX(), ev.getY());
        int dragLayerX = dragLayerPos[0];
        int dragLayerY = dragLayerPos[1];
        switch (action) {
            case 0:
                this.mMotionDownX = dragLayerX;
                this.mMotionDownY = dragLayerY;
                this.mLastDropTarget = null;
                break;
            case 1:
                this.mLastTouchUpTime = System.currentTimeMillis();
                if (this.mMoveStageAlarm != null) {
                    this.mMoveStageAlarm.cancelAlarm();
                }
                if (this.mDragging) {
                    drop((float) dragLayerX, (float) dragLayerY);
                }
                endDrag();
                break;
            case 3:
                cancelDrag();
                break;
        }
        return this.mDragging;
    }

    public void setMoveTarget(View view) {
        this.mMoveTarget = view;
    }

    public boolean dispatchUnhandledMove(View focused, int direction) {
        return this.mMoveTarget != null && this.mMoveTarget.dispatchUnhandledMove(focused, direction);
    }

    private void clearScrollRunnable() {
        this.mHandler.removeCallbacks(this.mScrollRunnable);
        if (this.mScrollState == 1) {
            this.mScrollState = 0;
            this.mScrollRunnable.setDirection(1);
            this.mDragScroller.onExitScrollArea();
        }
    }

    private void handleMoveEvent(int x, int y) {
        if (LauncherFeature.supportQuickOption() && dragOutOfQuickOptionBoundary(x, y)) {
            if (this.mMoveStageAlarm != null) {
                this.mMoveStageAlarm.cancelAlarm();
            }
            removeQuickOptionViewWithoutSALogging();
            if (((this.mDragObject.dragInfo instanceof IconInfo) || (this.mDragObject.dragInfo instanceof FolderInfo)) && ((this.mMultiSelectManager == null || !this.mMultiSelectManager.isMultiSelectMode()) && ((ItemInfo) this.mDragObject.dragInfo).itemType != 6)) {
                this.mDragObject.dragView.animateUp();
            }
        }
        if (this.mDragObject != null) {
            if (this.mDragObject.dragView != null) {
                this.mDragObject.dragView.move(x, y);
            }
            if (this.mDragObject.extraDragInfoList != null) {
                final int touchX = x;
                final int touchY = y;
                int count = 0;
                Iterator it = this.mDragObject.extraDragInfoList.iterator();
                while (it.hasNext()) {
                    final DragView targetView = ((DragObject) it.next()).dragView;
                    count++;
                    this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (targetView != null) {
                                targetView.move(touchX, touchY);
                            }
                        }
                    }, ((long) count) * DELAY_TO_MOVE_EXTRA_ITEM);
                }
            }
            int[] coordinates = this.mCoordinatesTemp;
            DropTarget dropTarget = findDropTarget(x, y, coordinates, false);
            this.mDragObject.x = coordinates[0];
            this.mDragObject.y = coordinates[1];
            checkTouchMove(dropTarget);
            int outlineColor = dropTarget == null ? this.mOutlineColor : dropTarget.getOutlineColor();
            if (outlineColor != this.mOutlineColor) {
                Drawable outline = this.mDragObject.dragView.getDragOutline();
                if (outline != null) {
                    outline.setColorFilter(outlineColor, Mode.SRC_IN);
                }
                this.mOutlineColor = outlineColor;
            }
        }
        this.mDistanceSinceScroll = (int) (((double) this.mDistanceSinceScroll) + Math.hypot((double) (this.mLastTouch[0] - x), (double) (this.mLastTouch[1] - y)));
        this.mLastTouch[0] = x;
        this.mLastTouch[1] = y;
        checkScrollState(x, y);
    }

    public void forceTouchMove() {
        int[] dummyCoordinates = this.mCoordinatesTemp;
        DropTarget dropTarget = findDropTarget(this.mLastTouch[0], this.mLastTouch[1], dummyCoordinates, false);
        this.mDragObject.x = dummyCoordinates[0];
        this.mDragObject.y = dummyCoordinates[1];
        checkTouchMove(dropTarget);
    }

    private void checkTouchMove(DropTarget dropTarget) {
        if (dropTarget != null) {
            if (this.mLastDropTarget != dropTarget) {
                if (this.mLastDropTarget != null) {
                    this.mLastDropTarget.onDragExit(this.mDragObject, true);
                }
                dropTarget.onDragEnter(this.mDragObject, true);
            }
            dropTarget.onDragOver(this.mDragObject);
        } else if (this.mLastDropTarget != null) {
            this.mLastDropTarget.onDragExit(this.mDragObject, false);
        }
        this.mLastDropTarget = dropTarget;
    }

    private void checkScrollState(int x, int y) {
        if (this.mLastDropTarget instanceof DragScroller) {
            int forwardDirection;
            int backwardsDirection;
            int delay = this.mDistanceSinceScroll < ViewConfiguration.get(this.mLauncher).getScaledWindowTouchSlop() ? RESCROLL_DELAY : 500;
            if (Utilities.sIsRtl) {
                forwardDirection = 1;
            } else {
                forwardDirection = 0;
            }
            if (Utilities.sIsRtl) {
                backwardsDirection = 0;
            } else {
                backwardsDirection = 1;
            }
            if (isLeftScrollZone(x)) {
                this.mIsInScrollArea = true;
                if (this.mScrollState == 0 && this.mDragScroller.onEnterScrollArea(x, y, forwardDirection)) {
                    this.mScrollRunnable.setDirection(forwardDirection);
                    this.mHandler.postDelayed(this.mScrollRunnable, (long) delay);
                    this.mScrollState = 1;
                    return;
                }
                return;
            } else if (isRightScrollZone(x)) {
                this.mIsInScrollArea = true;
                if (this.mScrollState == 0 && this.mDragScroller.onEnterScrollArea(x, y, backwardsDirection)) {
                    this.mScrollRunnable.setDirection(backwardsDirection);
                    this.mHandler.postDelayed(this.mScrollRunnable, (long) delay);
                    this.mScrollState = 1;
                    return;
                }
                return;
            } else {
                this.mIsInScrollArea = false;
                clearScrollRunnable();
                return;
            }
        }
        clearScrollRunnable();
    }

    private boolean isLeftScrollZone(int x) {
        if (LauncherFeature.supportNavigationBar() && !LauncherFeature.isTablet() && Utilities.getNavigationBarPositon() == 1) {
            if (x < this.mScrollZone + this.mLauncher.getDeviceProfile().navigationBarHeight) {
                return true;
            }
            return false;
        } else if (x >= this.mScrollZone) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isRightScrollZone(int x) {
        if (LauncherFeature.supportNavigationBar() && !LauncherFeature.isTablet() && Utilities.getNavigationBarPositon() == 2) {
            if (x > (this.mScrollView.getWidth() - this.mScrollZone) - this.mLauncher.getDeviceProfile().navigationBarHeight) {
                return true;
            }
            return false;
        } else if (x <= this.mScrollView.getWidth() - this.mScrollZone) {
            return false;
        } else {
            return true;
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (!this.mDragging) {
            return false;
        }
        int action = ev.getAction();
        int[] dragLayerPos = getClampedDragLayerPos(ev.getX(), ev.getY());
        int dragLayerX = dragLayerPos[0];
        int dragLayerY = dragLayerPos[1];
        switch (action) {
            case 0:
                this.mMotionDownX = dragLayerX;
                this.mMotionDownY = dragLayerY;
                if (dragLayerX < this.mScrollZone || dragLayerX > this.mScrollView.getWidth() - this.mScrollZone) {
                    this.mScrollState = 1;
                    this.mHandler.postDelayed(this.mScrollRunnable, 500);
                } else {
                    this.mScrollState = 0;
                }
                handleMoveEvent(dragLayerX, dragLayerY);
                break;
            case 1:
                handleMoveEvent(dragLayerX, dragLayerY);
                this.mHandler.removeCallbacks(this.mScrollRunnable);
                if (this.mMoveStageAlarm != null) {
                    this.mMoveStageAlarm.cancelAlarm();
                }
                if (this.mDragging) {
                    drop((float) dragLayerX, (float) dragLayerY);
                }
                endDrag();
                break;
            case 2:
                handleMoveEvent(dragLayerX, dragLayerY);
                break;
            case 3:
                this.mHandler.removeCallbacks(this.mScrollRunnable);
                cancelDrag();
                break;
        }
        return true;
    }

    private void drop(float x, float y) {
        if (LauncherFeature.supportQuickOption() && dragOutOfQuickOptionBoundary((int) x, (int) y)) {
            removeQuickOptionView();
        }
        int[] coordinates = this.mCoordinatesTemp;
        DropTarget dropTarget = findDropTarget((int) x, (int) y, coordinates, true);
        this.mDragObject.x = coordinates[0];
        this.mDragObject.y = coordinates[1];
        boolean accepted = false;
        if (dropTarget != null) {
            Iterator it;
            DragSource extraDragSource;
            boolean isNeedToSort = false;
            if (!(this.mDragObject.extraDragInfoList == null || this.mDragObject.extraDragSourceList == null || this.mDragObject.extraDragSourceList.size() <= 1)) {
                it = this.mDragObject.extraDragSourceList.iterator();
                while (it.hasNext()) {
                    extraDragSource = (DragSource) it.next();
                    if (dropTarget == extraDragSource && (extraDragSource instanceof HotseatDragController) && extraDragSource.getEmptyCount() < this.mDragObject.extraDragInfoList.size()) {
                        isNeedToSort = true;
                        if (this.mDragObject.dragSource != extraDragSource) {
                            this.mDragObject.cancelled = true;
                        }
                    }
                }
            }
            this.mDragObject.dragComplete = true;
            dropTarget.onDragExit(this.mDragObject, false);
            if (isNeedToSort && this.mDragObject.extraDragInfoList != null) {
                Collections.sort(this.mDragObject.extraDragInfoList, CONTAINER_COMPARATOR);
                Collections.sort(this.mDragObject.extraDragSourceList, DRAG_SOURCE_COMPARATOR);
            }
            if (dropTarget.acceptDrop(this.mDragObject)) {
                dropTarget.onDrop(this.mDragObject);
                accepted = true;
            }
            DragSource dragSource = this.mDragObject.dragSource;
            View targetView = dropTarget.getTargetView();
            DragObject dragObject = this.mDragObject;
            boolean z = accepted && (!this.mDragObject.cancelled || this.mDragObject.cancelDropFolder);
            dragSource.onDropCompleted(targetView, dragObject, z);
            if (!(this.mDragObject.extraDragInfoList == null || this.mDragObject.extraDragSourceList == null)) {
                it = this.mDragObject.extraDragSourceList.iterator();
                while (it.hasNext()) {
                    extraDragSource = (DragSource) it.next();
                    ArrayList<DragObject> targetExtraDragObjects = new ArrayList();
                    ArrayList<DragObject> cancelledExtraDragObjects = new ArrayList();
                    Iterator it2 = this.mDragObject.extraDragInfoList.iterator();
                    while (it2.hasNext()) {
                        DragObject d = (DragObject) it2.next();
                        if (extraDragSource.equals(d.dragSource)) {
                            if (!accepted || d.cancelled) {
                                cancelledExtraDragObjects.add(d);
                            } else {
                                targetExtraDragObjects.add(d);
                            }
                        }
                    }
                    if (this.mDragObject.cancelled && isNeedToSort && extraDragSource.equals(this.mDragObject.dragSource)) {
                        DragObject dragObject2 = new DragObject();
                        dragObject2.copyFrom(this.mDragObject);
                        cancelledExtraDragObjects.add(dragObject2);
                        isNeedToSort = false;
                    }
                    extraDragSource.onExtraObjectDropCompleted(dropTarget.getTargetView(), targetExtraDragObjects, cancelledExtraDragObjects, this.mDragObject.extraDragInfoList.size() + 1);
                }
            }
        }
        if (LauncherFeature.supportQuickOption() && isQuickOptionShowing()) {
            this.mQuickOptionView.insertSALoggingEvent();
        } else if (SALogging.getInstance().getAppShortcutPinningInfo() != null && !(dropTarget instanceof CancelDropTarget)) {
            SALogging.getInstance().insertAppShortcutPinnedEventLog(this.mLauncher);
        }
    }

    private DropTarget findDropTarget(int x, int y, int[] dropCoordinates, boolean isDrop) {
        Rect r = this.mRectTemp;
        ArrayList<DropTarget> dropTargets = this.mDropTargets;
        for (int i = dropTargets.size() - 1; i >= 0; i--) {
            DropTarget target = (DropTarget) dropTargets.get(i);
            if (target.isDropEnabled(isDrop)) {
                target.getHitRectRelativeToDragLayer(r);
                this.mDragObject.x = x;
                this.mDragObject.y = y;
                if (r.contains(x, y)) {
                    dropCoordinates[0] = x;
                    dropCoordinates[1] = y;
                    this.mLauncher.getDragLayer().mapCoordInSelfToDescendent(target.getTargetView(), dropCoordinates);
                    return target;
                }
            }
        }
        return null;
    }

    public void setDragScroller(DragScroller scroller) {
        clearScrollRunnable();
        this.mDragScroller = scroller;
        setScrollZone();
    }

    public void setScrollZone() {
        if (this.mDragScroller != null) {
            this.mScrollZone = this.mDragScroller.getScrollZone();
        }
    }

    public void setWindowToken(IBinder token) {
        this.mWindowToken = token;
    }

    public void addDragListener(DragListener l) {
        this.mListeners.add(l);
    }

    public void removeDragListener(DragListener l) {
        this.mListeners.remove(l);
    }

    public void addDropTarget(DropTarget target) {
        if (!this.mDropTargets.contains(target)) {
            this.mDropTargets.add(target);
        }
    }

    public void removeDropTarget(DropTarget target) {
        this.mDropTargets.remove(target);
    }

    public void setScrollView(View v) {
        this.mScrollView = v;
    }

    public void createQuickOptionViewFromCenterKey(View view, DragSource dragSource) {
        DragObject dragObject = new DragObject();
        dragObject.dragInfo = view.getTag();
        dragObject.dragSource = dragSource;
        setAnchor(view);
        createQuickOptionView(dragObject);
        if (isQuickOptionShowing()) {
            if (!this.mQuickOptionView.hasFocus()) {
                this.mQuickOptionView.requestFocus();
            }
            this.mQuickOptionView.insertSALoggingEvent();
        }
    }

    private void setAnchor(View view) {
        Rect r = new Rect();
        view.getGlobalVisibleRect(r);
        this.mLauncher.getQuickOptionManager().setAnchorRect(r);
        this.mLauncher.getQuickOptionManager().setAnchorView(view);
    }

    private void createQuickOptionView(DragObject d) {
        if (this.mQuickOptionView != null) {
            this.mLauncher.getDragLayer().removeViewInLayout(this.mQuickOptionView);
            this.mQuickOptionView = null;
        }
        int optionFlag = d.dragSource.getQuickOptionFlags(d);
        if (optionFlag != 0) {
            List<String> deepShortcutIds = null;
            if (LauncherFeature.supportDeepShortcut() && (d.dragInfo instanceof IconInfo)) {
                deepShortcutIds = LauncherAppState.getInstance().getShortcutManager().getShortcutIdsForItem((IconInfo) d.dragInfo);
            }
            this.mQuickOptionView = (QuickOptionView) View.inflate(this.mLauncher, R.layout.quick_option, null);
            if (this.mQuickOptionView != null) {
                this.mQuickOptionView.show(d, optionFlag, deepShortcutIds);
            }
        }
    }

    private boolean dragOutOfQuickOptionBoundary(int x, int y) {
        return Math.sqrt(Math.pow((double) Math.abs(this.mMotionDownX - x), 2.0d) + Math.pow((double) Math.abs(this.mMotionDownY - y), 2.0d)) > ((double) this.mLauncher.getResources().getInteger(R.integer.quick_options_dismiss_distance));
    }

    public QuickOptionView getQuickOptionView() {
        return this.mQuickOptionView;
    }

    public boolean isQuickOptionShowing() {
        return this.mQuickOptionView != null && this.mQuickOptionView.getState() == 0;
    }

    public void quickOptionNavigationBarPositionChanged() {
        this.mQuickOptionView.navigationBarPositionChanged();
    }

    public void removeQuickOptionView() {
        removeQuickOptionView("4");
    }

    public void removeQuickOptionView(String detailOfRemove) {
        if (this.mQuickOptionView != null) {
            this.mQuickOptionView.remove(this.mLauncher.hasWindowFocus());
            this.mQuickOptionView = null;
            SALogging.getInstance().insertQuickOptionEventLog(15, this.mLauncher, detailOfRemove);
        }
    }

    private void removeQuickOptionViewWithoutSALogging() {
        if (this.mQuickOptionView != null) {
            this.mQuickOptionView.remove(this.mLauncher.hasWindowFocus());
            this.mQuickOptionView = null;
        }
    }

    public DragView createDragView(View appView, int targetLocationX, int targetLocationY) {
        int[] tempPt = new int[2];
        AtomicInteger atomicInteger = new AtomicInteger(6);
        Bitmap viewBitmap = DragViewHelper.createDragBitmap(appView, atomicInteger, false);
        int bmpWidth = viewBitmap.getWidth();
        int bmpHeight = viewBitmap.getHeight();
        float appScale = this.mLauncher.getDragLayer().getLocationInDragLayer(appView, tempPt);
        int appX = Math.round(((float) tempPt[0]) - ((((float) bmpWidth) - (((float) appView.getWidth()) * appScale)) / 2.0f));
        int appY = Math.round((((float) tempPt[1]) - ((((float) bmpHeight) - (((float) bmpHeight) * appScale)) / 2.0f)) - (((float) atomicInteger.get()) / 2.0f));
        DragView dragView = new DragView(this.mLauncher, viewBitmap, -appX, -appY, 0, 0, viewBitmap.getWidth(), viewBitmap.getHeight(), appScale, 1.0f, false);
        dragView.setTargetOffset(targetLocationX - appX, targetLocationY - appY);
        return dragView;
    }

    private int countItemsBelowThanDraggedItemRank(DragObject d, int count) {
        if (((ItemInfo) d.dragInfo).rank < ((ItemInfo) this.mDragObject.dragInfo).rank && ((ItemInfo) this.mDragObject.dragInfo).screenId == ((ItemInfo) d.dragInfo).screenId && ((ItemInfo) d.dragInfo).container == -102) {
            return count + 1;
        }
        return count;
    }

    public DragObject getDragObject() {
        return this.mDragObject;
    }

    public boolean isInScrollArea() {
        return this.mIsInScrollArea;
    }

    public void onDriverDragMove(float x, float y) {
        int[] dragLayerPos = getClampedDragLayerPos(x, y);
        handleMoveEvent(dragLayerPos[0], dragLayerPos[1]);
    }

    public void onDriverDragExitWindow() {
        if (this.mLastDropTarget != null) {
            this.mLastDropTarget.onDragExit(this.mDragObject, false);
            this.mLastDropTarget = null;
        }
    }

    public void onDriverDragEnd(float x, float y) {
        drop(x, y);
        endDrag();
    }

    public void onDriverDragCancel() {
        cancelDrag();
    }

    public void cancelDragIfViewRemoved(View v) {
        if (v != null && this.mDragObject != null && this.mDragObject.dragView != null && v.equals(this.mDragObject.dragView.getSourceView())) {
            if (LauncherFeature.supportQuickOption() && isQuickOptionShowing()) {
                removeQuickOptionView();
            }
            cancelDrag();
        }
    }
}

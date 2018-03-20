package com.android.launcher3.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.appwidget.AppWidgetHostView;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.LayoutParams;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.drag.DragState;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.util.alarm.Alarm;
import com.android.launcher3.util.alarm.OnAlarmListener;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

class WorkspaceReorderController {
    private static final int INVALID_DIRECTION = -100;
    private static final int MODE_ACCEPT_DROP = 4;
    private static final int MODE_DRAG_OVER = 1;
    static final int MODE_ON_DROP = 2;
    static final int MODE_ON_DROP_EXTERNAL = 3;
    private static final int MODE_SHOW_REORDER_HINT = 0;
    private static final int REORDER_ANIMATION_DURATION = 150;
    private static final int REORDER_PREVIEW_MAGNITUDE = 20;
    private static final int REORDER_TIMEOUT = 350;
    private static final String TAG = "WSReorderController";
    private int mCountX;
    private int mCountY;
    private int[] mDirectionVector = new int[2];
    private ArrayList<View> mIntersectingViews = new ArrayList();
    private boolean mItemPlacementDirty = false;
    private int mLastReorderX = -1;
    private int mLastReorderY = -1;
    private CellLayout mLayout;
    private boolean[][] mOccupied;
    private Rect mOccupiedRect = new Rect();
    private int[] mPreviousReorderDirection = new int[2];
    private final Alarm mReorderAlarm = new Alarm();
    private HashMap<View, ReorderPreviewAnimation> mShakeAnimators = new HashMap();
    private DragState mTargetState;
    private final int[] mTempLocation = new int[2];
    private boolean[][] mTmpOccupied;
    private final int[] mTmpPoint = new int[2];

    private static class CellAndSpan {
        int spanX;
        int spanY;
        int x;
        int y;

        CellAndSpan() {
        }

        void copy(CellAndSpan copy) {
            copy.x = this.x;
            copy.y = this.y;
            copy.spanX = this.spanX;
            copy.spanY = this.spanY;
        }

        CellAndSpan(int x, int y, int spanX, int spanY) {
            this.x = x;
            this.y = y;
            this.spanX = spanX;
            this.spanY = spanY;
        }

        public String toString() {
            return "(" + this.x + ", " + this.y + ": " + this.spanX + ", " + this.spanY + ")";
        }
    }

    private static class ItemConfiguration {
        int dragViewSpanX;
        int dragViewSpanY;
        int dragViewX;
        int dragViewY;
        ArrayList<View> intersectingViews;
        boolean isSolution;
        HashMap<View, CellAndSpan> map;
        private HashMap<View, CellAndSpan> savedMap;
        ArrayList<View> sortedViews;

        private ItemConfiguration() {
            this.map = new HashMap();
            this.savedMap = new HashMap();
            this.sortedViews = new ArrayList();
            this.isSolution = false;
        }

        void save() {
            for (View v : this.map.keySet()) {
                ((CellAndSpan) this.map.get(v)).copy((CellAndSpan) this.savedMap.get(v));
            }
        }

        void restore() {
            for (View v : this.savedMap.keySet()) {
                ((CellAndSpan) this.savedMap.get(v)).copy((CellAndSpan) this.map.get(v));
            }
        }

        void add(View v, CellAndSpan cs) {
            this.map.put(v, cs);
            this.savedMap.put(v, new CellAndSpan());
            this.sortedViews.add(v);
        }

        int area() {
            return this.dragViewSpanX * this.dragViewSpanY;
        }
    }

    private class ReorderPreviewAnimation {
        static final int MODE_HINT = 0;
        static final int MODE_PREVIEW = 1;
        private static final int PREVIEW_DURATION = 300;
        Animator animator;
        View child;
        float finalDeltaX;
        float finalDeltaY;
        float initDeltaX;
        float initDeltaY;
        int mode;
        boolean repeating = false;

        public ReorderPreviewAnimation(View child, int mode, int cellX0, int cellY0, int cellX1, int cellY1, int spanX, int spanY) {
            WorkspaceReorderController.this.mLayout.regionToCenterPoint(cellX0, cellY0, spanX, spanY, WorkspaceReorderController.this.mTmpPoint);
            int x0 = WorkspaceReorderController.this.mTmpPoint[0];
            int y0 = WorkspaceReorderController.this.mTmpPoint[1];
            WorkspaceReorderController.this.mLayout.regionToCenterPoint(cellX1, cellY1, spanX, spanY, WorkspaceReorderController.this.mTmpPoint);
            int dX = WorkspaceReorderController.this.mTmpPoint[0] - x0;
            int dY = WorkspaceReorderController.this.mTmpPoint[1] - y0;
            this.finalDeltaX = 0.0f;
            this.finalDeltaY = 0.0f;
            int dir = mode == 0 ? -1 : 1;
            if (!(dX == dY && dX == 0)) {
                if (dY == 0) {
                    this.finalDeltaX = (((float) (-dir)) * Math.signum((float) dX)) * 20.0f;
                } else if (dX == 0) {
                    this.finalDeltaY = (((float) (-dir)) * Math.signum((float) dY)) * 20.0f;
                } else {
                    double angle = Math.atan((double) (((float) dY) / ((float) dX)));
                    this.finalDeltaX = (float) ((int) (((double) (((float) (-dir)) * Math.signum((float) dX))) * Math.abs(Math.cos(angle) * 20.0d)));
                    this.finalDeltaY = (float) ((int) (((double) (((float) (-dir)) * Math.signum((float) dY))) * Math.abs(Math.sin(angle) * 20.0d)));
                }
            }
            this.mode = mode;
            this.initDeltaX = child.getTranslationX();
            this.initDeltaY = child.getTranslationY();
            this.child = child;
        }

        void animate() {
            if (WorkspaceReorderController.this.mShakeAnimators.containsKey(this.child)) {
                ((ReorderPreviewAnimation) WorkspaceReorderController.this.mShakeAnimators.get(this.child)).cancel();
                WorkspaceReorderController.this.mShakeAnimators.remove(this.child);
                if (this.finalDeltaX == 0.0f && this.finalDeltaY == 0.0f) {
                    completeAnimationImmediately();
                    return;
                }
            }
            if (this.finalDeltaX != 0.0f || this.finalDeltaY != 0.0f) {
                ValueAnimator va = LauncherAnimUtils.ofFloat(this.child, 0.0f, 1.0f);
                this.animator = va;
                va.setRepeatMode(2);
                va.setRepeatCount(-1);
                va.setDuration(this.mode == 0 ? 350 : 300);
                va.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float r1;
                        float r = ((Float) animation.getAnimatedValue()).floatValue();
                        if (ReorderPreviewAnimation.this.mode == 0 && ReorderPreviewAnimation.this.repeating) {
                            r1 = 1.0f;
                        } else {
                            r1 = r;
                        }
                        float y = (ReorderPreviewAnimation.this.finalDeltaY * r1) + ((1.0f - r1) * ReorderPreviewAnimation.this.initDeltaY);
                        ReorderPreviewAnimation.this.child.setTranslationX((ReorderPreviewAnimation.this.finalDeltaX * r1) + ((1.0f - r1) * ReorderPreviewAnimation.this.initDeltaX));
                        ReorderPreviewAnimation.this.child.setTranslationY(y);
                    }
                });
                va.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationRepeat(Animator animation) {
                        ReorderPreviewAnimation.this.initDeltaX = 0.0f;
                        ReorderPreviewAnimation.this.initDeltaY = 0.0f;
                        ReorderPreviewAnimation.this.repeating = true;
                    }

                    public void onAnimationEnd(Animator animation) {
                        ReorderPreviewAnimation.this.child.animate().translationX(0.0f);
                        ReorderPreviewAnimation.this.child.animate().translationY(0.0f);
                    }
                });
                WorkspaceReorderController.this.mShakeAnimators.put(this.child, this);
                va.start();
            }
        }

        private void cancel() {
            if (this.animator != null) {
                this.animator.cancel();
            }
        }

        void completeAnimationImmediately() {
            if (this.animator != null) {
                this.animator.cancel();
            }
            AnimatorSet s = LauncherAnimUtils.createAnimatorSet();
            this.animator = s;
            r1 = new Animator[2];
            r1[0] = LauncherAnimUtils.ofFloat(this.child, "translationX", 0.0f);
            r1[1] = LauncherAnimUtils.ofFloat(this.child, "translationY", 0.0f);
            s.playTogether(r1);
            s.setDuration(150);
            s.setInterpolator(new DecelerateInterpolator(1.5f));
            s.start();
        }
    }

    static class SpanInfo {
        int minSpanX;
        int minSpanY;
        int spanX;
        int spanY;
        final ArrayList<int[]> supportSpans;

        SpanInfo(int minSpanX, int minSpanY, int spanX, int spanY, ArrayList<int[]> supportSpans) {
            this.minSpanX = minSpanX;
            this.minSpanY = minSpanY;
            this.spanX = spanX;
            this.spanY = spanY;
            this.supportSpans = supportSpans;
        }

        static SpanInfo getSpanInfo(ItemInfo info) {
            boolean isWidget = (info instanceof LauncherAppWidgetInfo) || (info instanceof PendingAddWidgetInfo);
            ArrayList<int[]> supportSpans = null;
            int minSpanX = info.spanX;
            int minSpanY = info.spanY;
            if (isWidget) {
                LauncherAppWidgetProviderInfo widgetInfo = null;
                if (info instanceof PendingAddWidgetInfo) {
                    widgetInfo = ((PendingAddWidgetInfo) info).info;
                } else {
                    AppWidgetHostView hostView = ((LauncherAppWidgetInfo) info).hostView;
                    if (hostView != null) {
                        widgetInfo = (LauncherAppWidgetProviderInfo) hostView.getAppWidgetInfo();
                    }
                }
                if (widgetInfo != null) {
                    minSpanX = widgetInfo.getMinSpanX();
                    minSpanY = widgetInfo.getMinSpanY();
                    supportSpans = widgetInfo.getSupportedSpans();
                } else {
                    Log.i(WorkspaceReorderController.TAG, "widgetInfo is null " + info);
                }
            }
            return new SpanInfo(minSpanX, minSpanY, info.spanX, info.spanY, supportSpans);
        }
    }

    private class ViewCluster {
        static final int BOTTOM = 3;
        static final int LEFT = 0;
        static final int RIGHT = 2;
        static final int TOP = 1;
        int[] bottomEdge = new int[WorkspaceReorderController.this.mCountX];
        boolean bottomEdgeDirty;
        Rect boundingRect = new Rect();
        boolean boundingRectDirty;
        PositionComparator comparator = new PositionComparator();
        ItemConfiguration config;
        int[] leftEdge = new int[WorkspaceReorderController.this.mCountY];
        boolean leftEdgeDirty;
        int[] rightEdge = new int[WorkspaceReorderController.this.mCountY];
        boolean rightEdgeDirty;
        int[] topEdge = new int[WorkspaceReorderController.this.mCountX];
        boolean topEdgeDirty;
        ArrayList<View> views;

        class PositionComparator implements Comparator<View> {
            int whichEdge = 0;

            PositionComparator() {
            }

            public int compare(View left, View right) {
                CellAndSpan l = (CellAndSpan) ViewCluster.this.config.map.get(left);
                CellAndSpan r = (CellAndSpan) ViewCluster.this.config.map.get(right);
                switch (this.whichEdge) {
                    case 0:
                        return (r.x + r.spanX) - (l.x + l.spanX);
                    case 1:
                        return (r.y + r.spanY) - (l.y + l.spanY);
                    case 2:
                        return l.x - r.x;
                    default:
                        return l.y - r.y;
                }
            }
        }

        ViewCluster(ArrayList<View> views, ItemConfiguration config) {
            this.views = (ArrayList) views.clone();
            this.config = config;
            resetEdges();
        }

        void resetEdges() {
            int i;
            for (i = 0; i < WorkspaceReorderController.this.mCountX; i++) {
                this.topEdge[i] = -1;
                this.bottomEdge[i] = -1;
            }
            for (i = 0; i < WorkspaceReorderController.this.mCountY; i++) {
                this.leftEdge[i] = -1;
                this.rightEdge[i] = -1;
            }
            this.leftEdgeDirty = true;
            this.rightEdgeDirty = true;
            this.bottomEdgeDirty = true;
            this.topEdgeDirty = true;
            this.boundingRectDirty = true;
        }

        void computeEdge(int which, int[] edge) {
            int count = this.views.size();
            for (int i = 0; i < count; i++) {
                CellAndSpan cs = (CellAndSpan) this.config.map.get(this.views.get(i));
                int j;
                switch (which) {
                    case 0:
                        int left = cs.x;
                        j = cs.y;
                        while (j < cs.y + cs.spanY && j < WorkspaceReorderController.this.mCountY) {
                            if (left < edge[j] || edge[j] < 0) {
                                edge[j] = left;
                            }
                            j++;
                        }
                        break;
                    case 1:
                        int top = cs.y;
                        j = cs.x;
                        while (j < cs.x + cs.spanX && j < WorkspaceReorderController.this.mCountX) {
                            if (top < edge[j] || edge[j] < 0) {
                                edge[j] = top;
                            }
                            j++;
                        }
                        break;
                    case 2:
                        int right = cs.x + cs.spanX;
                        j = cs.y;
                        while (j < cs.y + cs.spanY && j < WorkspaceReorderController.this.mCountY) {
                            if (right > edge[j]) {
                                edge[j] = right;
                            }
                            j++;
                        }
                        break;
                    case 3:
                        int bottom = cs.y + cs.spanY;
                        j = cs.x;
                        while (j < cs.x + cs.spanX && j < WorkspaceReorderController.this.mCountX) {
                            if (bottom > edge[j]) {
                                edge[j] = bottom;
                            }
                            j++;
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        boolean isViewTouchingEdge(View v, int whichEdge) {
            CellAndSpan cs = (CellAndSpan) this.config.map.get(v);
            int[] edge = getEdge(whichEdge);
            int i;
            switch (whichEdge) {
                case 0:
                    i = cs.y;
                    while (i < cs.y + cs.spanY && i < WorkspaceReorderController.this.mCountY) {
                        if (edge[i] == cs.x + cs.spanX) {
                            return true;
                        }
                        i++;
                    }
                    break;
                case 1:
                    i = cs.x;
                    while (i < cs.x + cs.spanX && i < WorkspaceReorderController.this.mCountX) {
                        if (edge[i] == cs.y + cs.spanY) {
                            return true;
                        }
                        i++;
                    }
                    break;
                case 2:
                    i = cs.y;
                    while (i < cs.y + cs.spanY && i < WorkspaceReorderController.this.mCountY) {
                        if (edge[i] == cs.x) {
                            return true;
                        }
                        i++;
                    }
                    break;
                case 3:
                    i = cs.x;
                    while (i < cs.x + cs.spanX && i < WorkspaceReorderController.this.mCountX) {
                        if (edge[i] == cs.y) {
                            return true;
                        }
                        i++;
                    }
                    break;
            }
            return false;
        }

        void shift(int whichEdge, int delta) {
            Iterator it = this.views.iterator();
            while (it.hasNext()) {
                CellAndSpan c = (CellAndSpan) this.config.map.get((View) it.next());
                switch (whichEdge) {
                    case 0:
                        c.x -= delta;
                        break;
                    case 1:
                        c.y -= delta;
                        break;
                    case 2:
                        c.x += delta;
                        break;
                    default:
                        c.y += delta;
                        break;
                }
            }
            resetEdges();
        }

        void addView(View v) {
            this.views.add(v);
            resetEdges();
        }

        Rect getBoundingRect() {
            if (this.boundingRectDirty) {
                boolean first = true;
                Iterator it = this.views.iterator();
                while (it.hasNext()) {
                    CellAndSpan c = (CellAndSpan) this.config.map.get((View) it.next());
                    if (first) {
                        this.boundingRect.set(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
                        first = false;
                    } else {
                        this.boundingRect.union(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
                    }
                }
            }
            return this.boundingRect;
        }

        int[] getEdge(int which) {
            switch (which) {
                case 0:
                    return getLeftEdge();
                case 1:
                    return getTopEdge();
                case 2:
                    return getRightEdge();
                default:
                    return getBottomEdge();
            }
        }

        int[] getLeftEdge() {
            if (this.leftEdgeDirty) {
                computeEdge(0, this.leftEdge);
            }
            return this.leftEdge;
        }

        int[] getRightEdge() {
            if (this.rightEdgeDirty) {
                computeEdge(2, this.rightEdge);
            }
            return this.rightEdge;
        }

        int[] getTopEdge() {
            if (this.topEdgeDirty) {
                computeEdge(1, this.topEdge);
            }
            return this.topEdge;
        }

        int[] getBottomEdge() {
            if (this.bottomEdgeDirty) {
                computeEdge(3, this.bottomEdge);
            }
            return this.bottomEdge;
        }

        void sortConfigurationForEdgePush(int edge) {
            this.comparator.whichEdge = edge;
            Collections.sort(this.config.sortedViews, this.comparator);
        }
    }

    private class ReorderAlarmListener implements OnAlarmListener {
        View child;
        DragObject d;
        float[] dragViewCenter;
        SpanInfo spanInfo;
        int[] targetCell;

        ReorderAlarmListener(float[] dragViewCenter, int[] targetCell, SpanInfo spanInfo, DragObject d, View child) {
            this.dragViewCenter = dragViewCenter;
            this.targetCell = targetCell;
            this.spanInfo = spanInfo;
            this.child = child;
            this.d = d;
        }

        public void onAlarm(Alarm alarm) {
            if (WorkspaceReorderController.this.mLayout == null) {
                WorkspaceReorderController.this.revertTempState();
                return;
            }
            int[] resultSpan = new int[2];
            this.targetCell = WorkspaceReorderController.this.mLayout.findNearestArea((int) this.dragViewCenter[0], (int) this.dragViewCenter[1], this.spanInfo.minSpanX, this.spanInfo.minSpanY, this.targetCell);
            WorkspaceReorderController.this.mLastReorderX = this.targetCell[0];
            WorkspaceReorderController.this.mLastReorderY = this.targetCell[1];
            this.targetCell = WorkspaceReorderController.this.performReorder((int) this.dragViewCenter[0], (int) this.dragViewCenter[1], this.spanInfo, this.child, this.targetCell, resultSpan, 1);
            if (this.targetCell[0] < 0 || this.targetCell[1] < 0) {
                WorkspaceReorderController.this.revertTempState();
            } else if (WorkspaceReorderController.this.mTargetState != null) {
                WorkspaceReorderController.this.mTargetState.setDragMode(3);
            }
            boolean resize = (resultSpan[0] == this.spanInfo.spanX && resultSpan[1] == this.spanInfo.spanY) ? false : true;
            WorkspaceReorderController.this.mLayout.visualizeDropLocation((ItemInfo) this.d.dragInfo, this.d.dragView.getDragOutline(), this.targetCell[0], this.targetCell[1], resultSpan[0], resultSpan[1], resize);
        }
    }

    WorkspaceReorderController(DragState targetState) {
        this.mTargetState = targetState;
    }

    void prepareChildForDrag(CellLayout layout, View child) {
        layout.markCellsAsUnoccupiedForView(child);
    }

    void setReorderTarget(CellLayout layout) {
        revertTempState();
        this.mLayout = layout;
        if (this.mLayout != null) {
            this.mCountX = this.mLayout.getCountX();
            this.mCountY = this.mLayout.getCountY();
            this.mOccupied = this.mLayout.getOccupied();
            this.mTmpOccupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{this.mCountX, this.mCountY});
        }
        this.mPreviousReorderDirection[0] = -100;
        this.mPreviousReorderDirection[1] = -100;
    }

    private int[] findNearestArea(int cellX, int cellY, int spanX, int spanY, int[] direction, boolean[][] occupied, boolean[][] blockOccupied, int[] result) {
        int[] bestXY;
        if (result != null) {
            bestXY = result;
        } else {
            bestXY = new int[2];
        }
        float bestDistance = Float.MAX_VALUE;
        int bestDirectionScore = Integer.MIN_VALUE;
        int countX = this.mCountX;
        int countY = this.mCountY;
        for (int y = 0; y < countY - (spanY - 1); y++) {
            for (int x = 0; x < countX - (spanX - 1); x++) {
                int i = 0;
                while (i < spanX) {
                    int j = 0;
                    while (j < spanY) {
                        if (occupied[x + i][y + j] && (blockOccupied == null || blockOccupied[i][j])) {
                            break;
                        }
                        j++;
                    }
                    i++;
                }
                float distanceX = (float) (x - cellX);
                float distanceY = (float) (y - cellY);
                float distance = (float) Math.hypot((double) distanceX, (double) distanceY);
                int[] curDirection = this.mTmpPoint;
                curDirection[0] = (int) Math.signum(distanceX);
                curDirection[1] = (int) Math.signum(distanceY);
                int curDirectionScore = (direction[0] * curDirection[0]) + (direction[1] * curDirection[1]);
                boolean directionMatches = direction[0] == curDirection[0] && direction[1] == curDirection[1];
                if (((directionMatches || !false) && Float.compare(distance, bestDistance) < 0) || (Float.compare(distance, bestDistance) == 0 && curDirectionScore > bestDirectionScore)) {
                    bestDistance = distance;
                    bestDirectionScore = curDirectionScore;
                    bestXY[0] = x;
                    bestXY[1] = y;
                }
            }
        }
        if (bestDistance == Float.MAX_VALUE) {
            bestXY[0] = -1;
            bestXY[1] = -1;
        }
        return bestXY;
    }

    private boolean addViewToTempLocation(View v, Rect rectOccupiedByPotentialDrop, int[] direction, ItemConfiguration currentState) {
        CellAndSpan c = (CellAndSpan) currentState.map.get(v);
        boolean success = false;
        markCellsForView(c.x, c.y, c.spanX, c.spanY, this.mTmpOccupied, false);
        markCellsForRect(rectOccupiedByPotentialDrop, this.mTmpOccupied, true);
        findNearestArea(c.x, c.y, c.spanX, c.spanY, direction, this.mTmpOccupied, (boolean[][]) null, this.mTempLocation);
        if (this.mTempLocation[0] >= 0 && this.mTempLocation[1] >= 0) {
            c.x = this.mTempLocation[0];
            c.y = this.mTempLocation[1];
            success = true;
        }
        markCellsForView(c.x, c.y, c.spanX, c.spanY, this.mTmpOccupied, true);
        return success;
    }

    private boolean pushViewsToTempLocation(ArrayList<View> views, Rect rectOccupiedByPotentialDrop, int[] direction, View dragView, ItemConfiguration currentState) {
        int whichEdge;
        int pushDistance;
        ViewCluster cluster = new ViewCluster(views, currentState);
        Rect clusterRect = cluster.getBoundingRect();
        boolean fail = false;
        if (direction[0] < 0) {
            whichEdge = 0;
            pushDistance = clusterRect.right - rectOccupiedByPotentialDrop.left;
        } else if (direction[0] > 0) {
            whichEdge = 2;
            pushDistance = rectOccupiedByPotentialDrop.right - clusterRect.left;
        } else if (direction[1] < 0) {
            whichEdge = 1;
            pushDistance = clusterRect.bottom - rectOccupiedByPotentialDrop.top;
        } else {
            whichEdge = 3;
            pushDistance = rectOccupiedByPotentialDrop.bottom - clusterRect.top;
        }
        if (pushDistance <= 0) {
            return false;
        }
        Iterator it = views.iterator();
        while (it.hasNext()) {
            CellAndSpan c = (CellAndSpan) currentState.map.get((View) it.next());
            markCellsForView(c.x, c.y, c.spanX, c.spanY, this.mTmpOccupied, false);
        }
        currentState.save();
        cluster.sortConfigurationForEdgePush(whichEdge);
        while (pushDistance > 0 && !fail) {
            it = currentState.sortedViews.iterator();
            while (it.hasNext()) {
                View v = (View) it.next();
                if (!(cluster.views.contains(v) || v == dragView || !cluster.isViewTouchingEdge(v, whichEdge))) {
                    if (!((LayoutParams) v.getLayoutParams()).canReorder) {
                        fail = true;
                        break;
                    }
                    cluster.addView(v);
                    c = (CellAndSpan) currentState.map.get(v);
                    markCellsForView(c.x, c.y, c.spanX, c.spanY, this.mTmpOccupied, false);
                }
            }
            pushDistance--;
            cluster.shift(whichEdge, 1);
        }
        boolean foundSolution = false;
        clusterRect = cluster.getBoundingRect();
        if (fail || clusterRect.left < 0 || clusterRect.right > this.mCountX || clusterRect.top < 0 || clusterRect.bottom > this.mCountY) {
            currentState.restore();
        } else {
            foundSolution = true;
        }
        it = cluster.views.iterator();
        while (it.hasNext()) {
            c = (CellAndSpan) currentState.map.get((View) it.next());
            markCellsForView(c.x, c.y, c.spanX, c.spanY, this.mTmpOccupied, true);
        }
        return foundSolution;
    }

    private boolean addViewsToTempLocation(ArrayList<View> views, Rect rectOccupiedByPotentialDrop, int[] direction, View dragView, ItemConfiguration currentState) {
        if (views.size() == 0) {
            return true;
        }
        boolean success = false;
        Rect boundingRect = null;
        Iterator it = views.iterator();
        while (it.hasNext()) {
            CellAndSpan c = (CellAndSpan) currentState.map.get((View) it.next());
            if (boundingRect == null) {
                Rect rect = new Rect(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
            } else {
                boundingRect.union(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
            }
        }
        Iterator it2 = views.iterator();
        while (it2.hasNext()) {
            c = (CellAndSpan) currentState.map.get((View) it2.next());
            markCellsForView(c.x, c.y, c.spanX, c.spanY, this.mTmpOccupied, false);
        }
        boolean[][] blockOccupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{boundingRect.width(), boundingRect.height()});
        int top = boundingRect.top;
        int left = boundingRect.left;
        it2 = views.iterator();
        while (it2.hasNext()) {
            c = (CellAndSpan) currentState.map.get((View) it2.next());
            markCellsForView(c.x - left, c.y - top, c.spanX, c.spanY, blockOccupied, true);
        }
        markCellsForRect(rectOccupiedByPotentialDrop, this.mTmpOccupied, true);
        findNearestArea(boundingRect.left, boundingRect.top, boundingRect.width(), boundingRect.height(), direction, this.mTmpOccupied, blockOccupied, this.mTempLocation);
        if (this.mTempLocation[0] >= 0 && this.mTempLocation[1] >= 0) {
            int deltaX = this.mTempLocation[0] - boundingRect.left;
            int deltaY = this.mTempLocation[1] - boundingRect.top;
            it = views.iterator();
            while (it.hasNext()) {
                c = (CellAndSpan) currentState.map.get((View) it.next());
                c.x += deltaX;
                c.y += deltaY;
            }
            success = true;
        }
        it = views.iterator();
        while (it.hasNext()) {
            c = (CellAndSpan) currentState.map.get((View) it.next());
            markCellsForView(c.x, c.y, c.spanX, c.spanY, this.mTmpOccupied, true);
        }
        return success;
    }

    private boolean attemptPushInDirection(ArrayList<View> intersectingViews, Rect occupied, int[] direction, View ignoreView, ItemConfiguration solution) {
        int temp;
        if (Math.abs(direction[0]) + Math.abs(direction[1]) > 1) {
            temp = direction[1];
            direction[1] = 0;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction, ignoreView, solution)) {
                return true;
            }
            direction[1] = temp;
            temp = direction[0];
            direction[0] = 0;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction, ignoreView, solution)) {
                return true;
            }
            direction[0] = temp;
            direction[0] = direction[0] * -1;
            direction[1] = direction[1] * -1;
            temp = direction[1];
            direction[1] = 0;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction, ignoreView, solution)) {
                return true;
            }
            direction[1] = temp;
            temp = direction[0];
            direction[0] = 0;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction, ignoreView, solution)) {
                return true;
            }
            direction[0] = temp;
            direction[0] = direction[0] * -1;
            direction[1] = direction[1] * -1;
        } else if (pushViewsToTempLocation(intersectingViews, occupied, direction, ignoreView, solution)) {
            return true;
        } else {
            direction[0] = direction[0] * -1;
            direction[1] = direction[1] * -1;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction, ignoreView, solution)) {
                return true;
            }
            direction[0] = direction[0] * -1;
            direction[1] = direction[1] * -1;
            temp = direction[1];
            direction[1] = direction[0];
            direction[0] = temp;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction, ignoreView, solution)) {
                return true;
            }
            direction[0] = direction[0] * -1;
            direction[1] = direction[1] * -1;
            if (pushViewsToTempLocation(intersectingViews, occupied, direction, ignoreView, solution)) {
                return true;
            }
            direction[0] = direction[0] * -1;
            direction[1] = direction[1] * -1;
            temp = direction[1];
            direction[1] = direction[0];
            direction[0] = temp;
        }
        return false;
    }

    private boolean rearrangementExists(int cellX, int cellY, int spanX, int spanY, int[] direction, View ignoreView, ItemConfiguration solution) {
        if (cellX < 0 || cellY < 0) {
            return false;
        }
        CellAndSpan c;
        Iterator it;
        this.mIntersectingViews.clear();
        this.mOccupiedRect.set(cellX, cellY, cellX + spanX, cellY + spanY);
        if (ignoreView != null) {
            c = (CellAndSpan) solution.map.get(ignoreView);
            if (c != null) {
                c.x = cellX;
                c.y = cellY;
            }
        }
        Rect r0 = new Rect(cellX, cellY, cellX + spanX, cellY + spanY);
        Rect r1 = new Rect();
        for (View child : solution.map.keySet()) {
            if (child != ignoreView) {
                c = (CellAndSpan) solution.map.get(child);
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                r1.set(c.x, c.y, c.x + c.spanX, c.y + c.spanY);
                if (!Rect.intersects(r0, r1)) {
                    continue;
                } else if (!lp.canReorder) {
                    return false;
                } else {
                    this.mIntersectingViews.add(child);
                }
            }
        }
        solution.intersectingViews = new ArrayList(this.mIntersectingViews);
        if (attemptPushInDirection(this.mIntersectingViews, this.mOccupiedRect, direction, ignoreView, solution)) {
            return true;
        }
        if (addViewsToTempLocation(this.mIntersectingViews, this.mOccupiedRect, direction, ignoreView, solution)) {
            return true;
        }
        it = this.mIntersectingViews.iterator();
        while (it.hasNext()) {
            if (!addViewToTempLocation((View) it.next(), this.mOccupiedRect, direction, solution)) {
                return false;
            }
        }
        return true;
    }

    private void computeDirectionVector(float deltaX, float deltaY, int[] result) {
        double angle = Math.atan((double) (deltaY / deltaX));
        result[0] = 0;
        result[1] = 0;
        if (Math.abs(Math.cos(angle)) > 0.5d) {
            result[0] = (int) Math.signum(deltaX);
        }
        if (Math.abs(Math.sin(angle)) > 0.5d) {
            result[1] = (int) Math.signum(deltaY);
        }
    }

    private void copyOccupiedArray(boolean[][] occupied) {
        for (int i = 0; i < this.mCountX; i++) {
            System.arraycopy(this.mOccupied[i], 0, occupied[i], 0, this.mCountY);
        }
    }

    private ItemConfiguration findReorderSolution(int pixelX, int pixelY, SpanInfo spanInfo, int[] direction, View dragView, boolean decX, ItemConfiguration solution) {
        copyCurrentStateToSolution(solution);
        copyOccupiedArray(this.mTmpOccupied);
        int i = pixelX;
        int i2 = pixelY;
        int[] result = this.mLayout.findNearestArea(i, i2, spanInfo.spanX, spanInfo.spanY, new int[2]);
        if (rearrangementExists(result[0], result[1], spanInfo.spanX, spanInfo.spanY, direction, dragView, solution)) {
            solution.isSolution = true;
            solution.dragViewX = result[0];
            solution.dragViewY = result[1];
            solution.dragViewSpanX = spanInfo.spanX;
            solution.dragViewSpanY = spanInfo.spanY;
            return solution;
        }
        if (spanInfo.supportSpans != null && spanInfo.supportSpans.size() > 0) {
            int[] s;
            int size = spanInfo.supportSpans.size();
            int index = 0;
            while (index < size) {
                s = (int[]) spanInfo.supportSpans.get(index);
                if (spanInfo.spanX == s[0] && spanInfo.spanY == s[1]) {
                    break;
                }
                index++;
            }
            if (index > 0 && index < size) {
                s = (int[]) spanInfo.supportSpans.get(index - 1);
                return findReorderSolution(pixelX, pixelY, new SpanInfo(spanInfo.minSpanX, spanInfo.minSpanY, s[0], s[1], spanInfo.supportSpans), direction, dragView, true, solution);
            }
        } else if (spanInfo.spanX > spanInfo.minSpanX && (spanInfo.minSpanY == spanInfo.spanY || decX)) {
            return findReorderSolution(pixelX, pixelY, new SpanInfo(spanInfo.minSpanX, spanInfo.minSpanX, spanInfo.spanX - 1, spanInfo.spanY, spanInfo.supportSpans), direction, dragView, false, solution);
        } else if (spanInfo.spanY > spanInfo.minSpanY) {
            return findReorderSolution(pixelX, pixelY, new SpanInfo(spanInfo.minSpanX, spanInfo.minSpanX, spanInfo.spanX, spanInfo.spanY - 1, spanInfo.supportSpans), direction, dragView, true, solution);
        }
        solution.isSolution = false;
        return solution;
    }

    private void copyCurrentStateToSolution(ItemConfiguration solution) {
        CellLayoutChildren clc = this.mLayout.getCellLayoutChildren();
        int childCount = clc.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = clc.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            solution.add(child, new CellAndSpan(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan));
        }
    }

    private void copySolutionToTempState(ItemConfiguration solution, View dragView) {
        int i;
        CellLayoutChildren clc = this.mLayout.getCellLayoutChildren();
        for (i = 0; i < this.mCountX; i++) {
            for (int j = 0; j < this.mCountY; j++) {
                this.mTmpOccupied[i][j] = false;
            }
        }
        int childCount = clc.getChildCount();
        for (i = 0; i < childCount; i++) {
            View child = clc.getChildAt(i);
            if (child != dragView) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                CellAndSpan c = (CellAndSpan) solution.map.get(child);
                if (c != null) {
                    lp.tmpCellX = c.x;
                    lp.tmpCellY = c.y;
                    lp.cellHSpan = c.spanX;
                    lp.cellVSpan = c.spanY;
                    markCellsForView(c.x, c.y, c.spanX, c.spanY, this.mTmpOccupied, true);
                }
            }
        }
        markCellsForView(solution.dragViewX, solution.dragViewY, solution.dragViewSpanX, solution.dragViewSpanY, this.mTmpOccupied, true);
    }

    private void animateItemsToSolution(ItemConfiguration solution, View dragView, boolean commitDragView) {
        int i;
        boolean[][] occupied = this.mTmpOccupied;
        CellLayoutChildren clc = this.mLayout.getCellLayoutChildren();
        for (i = 0; i < this.mCountX; i++) {
            for (int j = 0; j < this.mCountY; j++) {
                occupied[i][j] = false;
            }
        }
        int childCount = clc.getChildCount();
        for (i = 0; i < childCount; i++) {
            View child = clc.getChildAt(i);
            if (child != dragView) {
                if (child.getTag() instanceof ItemInfo) {
                    CellAndSpan c = (CellAndSpan) solution.map.get(child);
                    if (c != null) {
                        this.mLayout.animateChildToPosition(child, c.x, c.y, 150, 0, false, false, this.mTmpOccupied);
                        markCellsForView(c.x, c.y, c.spanX, c.spanY, occupied, true);
                    }
                } else {
                    Log.i(TAG, "animateItemsToSolution() - child is not ItemInfo type. : " + child);
                }
            }
        }
        if (commitDragView) {
            markCellsForView(solution.dragViewX, solution.dragViewY, solution.dragViewSpanX, solution.dragViewSpanY, occupied, true);
        }
    }

    private void beginOrAdjustReorderPreviewAnimations(ItemConfiguration solution, View dragView, int mode) {
        CellLayoutChildren clc = this.mLayout.getCellLayoutChildren();
        int childCount = clc.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = clc.getChildAt(i);
            if (child != dragView) {
                CellAndSpan c = (CellAndSpan) solution.map.get(child);
                boolean skip = (mode != 0 || solution.intersectingViews == null || solution.intersectingViews.contains(child)) ? false : true;
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (!(c == null || skip)) {
                    new ReorderPreviewAnimation(child, mode, lp.cellX, lp.cellY, c.x, c.y, c.spanX, c.spanY).animate();
                }
            }
        }
    }

    private void completeAndClearReorderPreviewAnimations() {
        for (ReorderPreviewAnimation a : this.mShakeAnimators.values()) {
            a.completeAnimationImmediately();
        }
        this.mShakeAnimators.clear();
    }

    private void commitTempPlacement() {
        int i;
        CellLayoutChildren clc = this.mLayout.getCellLayoutChildren();
        for (i = 0; i < this.mCountX; i++) {
            System.arraycopy(this.mTmpOccupied[i], 0, this.mOccupied[i], 0, this.mCountY);
        }
        int childCount = clc.getChildCount();
        for (i = 0; i < childCount; i++) {
            View child = clc.getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            ItemInfo info = (ItemInfo) child.getTag();
            if (info != null) {
                if (!(info.cellX == lp.tmpCellX && info.cellY == lp.tmpCellY && info.spanX == lp.cellHSpan && info.spanY == lp.cellVSpan)) {
                    info.requiresDbUpdate = true;
                }
                int i2 = lp.tmpCellX;
                lp.cellX = i2;
                info.cellX = i2;
                i2 = lp.tmpCellY;
                lp.cellY = i2;
                info.cellY = i2;
                info.spanX = lp.cellHSpan;
                info.spanY = lp.cellVSpan;
            }
        }
        this.mTargetState.commit(this.mLayout);
    }

    void setUseTempCoords(boolean useTempCoords) {
        CellLayoutChildren clc = this.mLayout.getCellLayoutChildren();
        int childCount = clc.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ((LayoutParams) clc.getChildAt(i).getLayoutParams()).useTmpCoords = useTempCoords;
        }
    }

    private ItemConfiguration findConfigurationNoShuffle(int pixelX, int pixelY, SpanInfo spanInfo, ItemConfiguration solution) {
        int[] result = new int[2];
        int[] resultSpan = new int[2];
        if (spanInfo.supportSpans == null || spanInfo.supportSpans.size() <= 0) {
            this.mLayout.findNearestVacantArea(pixelX, pixelY, spanInfo.minSpanX, spanInfo.minSpanY, spanInfo.spanX, spanInfo.spanY, result, resultSpan);
        } else {
            int[] s;
            int size = spanInfo.supportSpans.size();
            int index = 0;
            while (index < size) {
                s = (int[]) spanInfo.supportSpans.get(index);
                if (spanInfo.spanX == s[0] && spanInfo.spanY == s[1]) {
                    break;
                }
                index++;
            }
            if (index < size) {
                while (index >= 0) {
                    s = (int[]) spanInfo.supportSpans.get(index);
                    this.mLayout.findNearestVacantArea(pixelX, pixelY, s[0], s[1], result);
                    if (result[0] >= 0 && result[1] >= 0) {
                        resultSpan[0] = s[0];
                        resultSpan[1] = s[1];
                        break;
                    }
                    index--;
                }
            } else {
                result[1] = -1;
                result[0] = -1;
            }
        }
        if (result[0] < 0 || result[1] < 0) {
            solution.isSolution = false;
        } else {
            copyCurrentStateToSolution(solution);
            solution.dragViewX = result[0];
            solution.dragViewY = result[1];
            solution.dragViewSpanX = resultSpan[0];
            solution.dragViewSpanY = resultSpan[1];
            solution.isSolution = true;
        }
        return solution;
    }

    private void getDirectionVectorForDrop(int dragViewCenterX, int dragViewCenterY, int spanX, int spanY, View dragView, int[] resultDirection) {
        int[] targetDestination = new int[2];
        this.mLayout.findNearestArea(dragViewCenterX, dragViewCenterY, spanX, spanY, targetDestination);
        Rect dragRect = new Rect();
        this.mLayout.regionToRect(targetDestination[0], targetDestination[1], spanX, spanY, dragRect);
        dragRect.offset(dragViewCenterX - dragRect.centerX(), dragViewCenterY - dragRect.centerY());
        Rect dropRegionRect = new Rect();
        getViewsIntersectingRegion(targetDestination[0], targetDestination[1], spanX, spanY, dragView, dropRegionRect, this.mIntersectingViews);
        int dropRegionSpanX = dropRegionRect.width();
        int dropRegionSpanY = dropRegionRect.height();
        this.mLayout.regionToRect(dropRegionRect.left, dropRegionRect.top, dropRegionRect.width(), dropRegionRect.height(), dropRegionRect);
        int deltaX = (dropRegionRect.centerX() - dragViewCenterX) / spanX;
        int deltaY = (dropRegionRect.centerY() - dragViewCenterY) / spanY;
        if (dropRegionSpanX == this.mCountX || spanX == this.mCountX) {
            deltaX = 0;
        }
        if (dropRegionSpanY == this.mCountY || spanY == this.mCountY) {
            deltaY = 0;
        }
        if (deltaX == 0 && deltaY == 0) {
            resultDirection[0] = 1;
            resultDirection[1] = 0;
        } else {
            computeDirectionVector((float) deltaX, (float) deltaY, resultDirection);
        }
        if (Utilities.sIsRtl) {
            resultDirection[0] = resultDirection[0] * -1;
        }
    }

    private void getViewsIntersectingRegion(int cellX, int cellY, int spanX, int spanY, View dragView, Rect boundingRect, ArrayList<View> intersectingViews) {
        CellLayoutChildren clc = this.mLayout.getCellLayoutChildren();
        if (boundingRect != null) {
            boundingRect.set(cellX, cellY, cellX + spanX, cellY + spanY);
        }
        intersectingViews.clear();
        Rect r0 = new Rect(cellX, cellY, cellX + spanX, cellY + spanY);
        Rect r1 = new Rect();
        int count = clc.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = clc.getChildAt(i);
            if (child != dragView) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                r1.set(lp.cellX, lp.cellY, lp.cellX + lp.cellHSpan, lp.cellY + lp.cellVSpan);
                if (Rect.intersects(r0, r1)) {
                    this.mIntersectingViews.add(child);
                    if (boundingRect != null) {
                        boundingRect.union(r1);
                    }
                }
            }
        }
    }

    private boolean isNearestDropLocationOccupied(int pixelX, int pixelY, int spanX, int spanY, View dragView, int[] result) {
        if (this.mLayout == null) {
            return false;
        }
        result = this.mLayout.findNearestArea(pixelX, pixelY, spanX, spanY, result);
        getViewsIntersectingRegion(result[0], result[1], spanX, spanY, dragView, null, this.mIntersectingViews);
        return !this.mIntersectingViews.isEmpty();
    }

    void cleanupReorder(boolean cancelAlarm) {
        if (cancelAlarm) {
            this.mReorderAlarm.cancelAlarm();
        }
        this.mLastReorderX = -1;
        this.mLastReorderY = -1;
    }

    void revertTempState() {
        if (this.mLayout != null) {
            CellLayoutChildren clc = this.mLayout.getCellLayoutChildren();
            completeAndClearReorderPreviewAnimations();
            if (isItemPlacementDirty()) {
                int count = clc.getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = clc.getChildAt(i);
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    if (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.cellY) {
                        lp.tmpCellX = lp.cellX;
                        lp.tmpCellY = lp.cellY;
                        this.mLayout.animateChildToPosition(child, lp.cellX, lp.cellY, 150, 0, false, false, this.mTmpOccupied);
                    }
                }
                setItemPlacementDirty(false);
            }
        }
    }

    boolean createAreaForResize(int cellX, int cellY, int spanX, int spanY, View dragView, int[] direction, boolean commit) {
        if (this.mLayout == null) {
            return false;
        }
        int[] pixelXY = new int[2];
        this.mLayout.regionToCenterPoint(cellX, cellY, spanX, spanY, pixelXY);
        if (Utilities.sIsRtl) {
            direction[0] = direction[0] * -1;
        }
        ItemConfiguration swapSolution = findReorderSolution(pixelXY[0], pixelXY[1], new SpanInfo(spanX, spanY, spanX, spanY, null), direction, dragView, true, new ItemConfiguration());
        setUseTempCoords(true);
        if (swapSolution.isSolution) {
            copySolutionToTempState(swapSolution, dragView);
            setItemPlacementDirty(true);
            animateItemsToSolution(swapSolution, dragView, commit);
            if (commit) {
                commitTempPlacement();
                completeAndClearReorderPreviewAnimations();
                setItemPlacementDirty(false);
                setUseTempCoords(false);
            } else {
                beginOrAdjustReorderPreviewAnimations(swapSolution, dragView, 1);
            }
            this.mLayout.getCellLayoutChildren().requestLayout();
        }
        return swapSolution.isSolution;
    }

    int[] performReorder(int pixelX, int pixelY, SpanInfo spanInfo, View dragView, int[] result, int[] resultSpan, int mode) {
        if (this.mLayout == null) {
            return result;
        }
        result = this.mLayout.findNearestArea(pixelX, pixelY, spanInfo.spanX, spanInfo.spanY, result);
        if (resultSpan == null) {
            resultSpan = new int[2];
        }
        if ((mode == 2 || mode == 3 || mode == 4) && this.mPreviousReorderDirection[0] != -100) {
            this.mDirectionVector[0] = this.mPreviousReorderDirection[0];
            this.mDirectionVector[1] = this.mPreviousReorderDirection[1];
            if (mode == 2 || mode == 3) {
                this.mPreviousReorderDirection[0] = -100;
                this.mPreviousReorderDirection[1] = -100;
            }
        } else {
            getDirectionVectorForDrop(pixelX, pixelY, spanInfo.spanX, spanInfo.spanY, dragView, this.mDirectionVector);
            this.mPreviousReorderDirection[0] = this.mDirectionVector[0];
            this.mPreviousReorderDirection[1] = this.mDirectionVector[1];
        }
        ItemConfiguration swapSolution = findReorderSolution(pixelX, pixelY, spanInfo, this.mDirectionVector, dragView, true, new ItemConfiguration());
        ItemConfiguration noShuffleSolution = findConfigurationNoShuffle(pixelX, pixelY, spanInfo, new ItemConfiguration());
        ItemConfiguration finalSolution = null;
        if (swapSolution.isSolution && swapSolution.area() >= noShuffleSolution.area()) {
            finalSolution = swapSolution;
        } else if (noShuffleSolution.isSolution) {
            finalSolution = noShuffleSolution;
        }
        if (mode == 0) {
            if (finalSolution != null) {
                beginOrAdjustReorderPreviewAnimations(finalSolution, dragView, 0);
                result[0] = finalSolution.dragViewX;
                result[1] = finalSolution.dragViewY;
                resultSpan[0] = finalSolution.dragViewSpanX;
                resultSpan[1] = finalSolution.dragViewSpanY;
            } else {
                resultSpan[1] = -1;
                resultSpan[0] = -1;
                result[1] = -1;
                result[0] = -1;
            }
            return result;
        }
        boolean foundSolution = true;
        setUseTempCoords(true);
        if (finalSolution != null) {
            result[0] = finalSolution.dragViewX;
            result[1] = finalSolution.dragViewY;
            resultSpan[0] = finalSolution.dragViewSpanX;
            resultSpan[1] = finalSolution.dragViewSpanY;
            if (mode == 1 || mode == 2 || mode == 3) {
                copySolutionToTempState(finalSolution, dragView);
                setItemPlacementDirty(true);
                animateItemsToSolution(finalSolution, dragView, mode == 2);
                if (mode == 2 || mode == 3) {
                    commitTempPlacement();
                    completeAndClearReorderPreviewAnimations();
                    setItemPlacementDirty(false);
                } else {
                    beginOrAdjustReorderPreviewAnimations(finalSolution, dragView, 1);
                }
            }
        } else {
            foundSolution = false;
            resultSpan[1] = -1;
            resultSpan[0] = -1;
            result[1] = -1;
            result[0] = -1;
        }
        if (mode == 2 || mode == 3 || !foundSolution) {
            setUseTempCoords(false);
        }
        this.mLayout.getCellLayoutChildren().requestLayout();
        return result;
    }

    boolean startReorder(float[] dragViewCenter, int[] targetCell, SpanInfo spanInfo, DragObject d, View child, int dragMode) {
        int reorderX = targetCell[0];
        int reorderY = targetCell[1];
        boolean nearestDropOccupied = isNearestDropLocationOccupied((int) dragViewCenter[0], (int) dragViewCenter[1], spanInfo.spanX, spanInfo.spanY, child, targetCell);
        if (!nearestDropOccupied) {
            revertTempState();
            this.mLayout.visualizeDropLocation((ItemInfo) d.dragInfo, d.dragView.getDragOutline(), targetCell[0], targetCell[1], spanInfo.spanX, spanInfo.spanY, false);
        } else if (!((dragMode != 0 && dragMode != 3) || this.mReorderAlarm.alarmPending() || (this.mLastReorderX == reorderX && this.mLastReorderY == reorderY))) {
            int i = (int) dragViewCenter[0];
            int i2 = (int) dragViewCenter[1];
            performReorder(i, i2, spanInfo, child, targetCell, new int[2], 0);
            this.mReorderAlarm.setOnAlarmListener(new ReorderAlarmListener(dragViewCenter, targetCell, spanInfo, d, child));
            this.mReorderAlarm.setAlarm(350);
        }
        return nearestDropOccupied;
    }

    private void setItemPlacementDirty(boolean dirty) {
        this.mItemPlacementDirty = dirty;
    }

    private boolean isItemPlacementDirty() {
        return this.mItemPlacementDirty;
    }

    private void markCellsForRect(Rect r, boolean[][] occupied, boolean value) {
        markCellsForView(r.left, r.top, r.width(), r.height(), occupied, value);
    }

    private void markCellsForView(int cellX, int cellY, int spanX, int spanY, boolean[][] occupied, boolean value) {
        if (cellX >= 0 && cellY >= 0) {
            int x = cellX;
            while (x < cellX + spanX && x < this.mCountX) {
                int y = cellY;
                while (y < cellY + spanY && y < this.mCountY) {
                    occupied[x][y] = value;
                    y++;
                }
                x++;
            }
        }
    }
}

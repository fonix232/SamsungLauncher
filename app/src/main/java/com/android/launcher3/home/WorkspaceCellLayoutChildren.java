package com.android.launcher3.home;

import android.content.Context;
import android.graphics.Rect;
import android.util.LongSparseArray;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.common.view.IconView;
import java.util.Iterator;

public class WorkspaceCellLayoutChildren extends CellLayoutChildren {
    private static final int GRID_CHANGE_ANIMATION_DELAY = 400;
    private static final int GRID_CHANGE_ANIMATION_DURATION = 300;
    private final AnimationSet mGridChangeAnimationSet = new AnimationSet(true);
    private boolean mIsGridChanging = false;
    private LongSparseArray<Rect> mPreviousRectMap = new LongSparseArray();

    public WorkspaceCellLayoutChildren(Context context) {
        super(context);
    }

    public int getDescendantFocusability() {
        Workspace workspace = getParent() == null ? null : (Workspace) getParent().getParent();
        if (workspace == null || (!workspace.isOverviewState() && !workspace.isScreenGridState())) {
            return super.getDescendantFocusability();
        }
        return 393216;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Workspace workspace = (Workspace) getParent().getParent();
        return (workspace == null || workspace.isPlusPage((WorkspaceCellLayout) getParent()) || (!workspace.isOverviewState() && !workspace.isScreenGridState())) ? false : true;
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        this.mGridChangeAnimationSet.getAnimations().clear();
        super.onLayout(changed, l, t, r, b);
        if (this.mIsGridChanging) {
            startGridChangeAnimation();
        }
    }

    private void startGridChangeAnimation() {
        this.mIsGridChanging = false;
        this.mPreviousRectMap.clear();
        final CellLayout cl = (CellLayout) getParent();
        if (cl != null) {
            cl.setCrossHairAnimatedVisibility(8, false);
            cl.invalidate();
        }
        post(new Runnable() {
            public void run() {
                int[] CurrentGridXY = new int[2];
                Utilities.loadCurrentGridSize(WorkspaceCellLayoutChildren.this.getContext(), CurrentGridXY);
                if (cl != null && cl.getCountX() == CurrentGridXY[0] && cl.getCountY() == CurrentGridXY[1]) {
                    WorkspaceCellLayoutChildren.this.postDelayed(new Runnable() {
                        public void run() {
                            if (WorkspaceCellLayoutChildren.this.mGridChangeAnimationSet.hasStarted() && !WorkspaceCellLayoutChildren.this.mGridChangeAnimationSet.hasEnded()) {
                                WorkspaceCellLayoutChildren.this.mGridChangeAnimationSet.start();
                            }
                        }
                    }, 400);
                } else if (WorkspaceCellLayoutChildren.this.mGridChangeAnimationSet.hasStarted() && !WorkspaceCellLayoutChildren.this.mGridChangeAnimationSet.hasEnded()) {
                    WorkspaceCellLayoutChildren.this.mGridChangeAnimationSet.start();
                }
                ((CellLayout) WorkspaceCellLayoutChildren.this.getParent()).setCrossHairAnimatedVisibility(0, true);
            }
        });
    }

    protected void buildCustomAnimationSet(View childView, int left, int top, int right, int bottom) {
        if (this.mIsGridChanging) {
            long dbId;
            ItemInfo item = (ItemInfo) childView.getTag();
            if (item == null) {
                dbId = -100;
            } else {
                dbId = item.id;
            }
            Pair<ItemInfo, View> pairItem = Pair.create(item, childView);
            Rect prevRect = (Rect) this.mPreviousRectMap.get(dbId);
            if (prevRect != null) {
                ScaleAnimation scaleAnim;
                Animation translateAnimation;
                this.mPreviousRectMap.remove(dbId);
                int[] absXY = new int[2];
                childView.getLocationOnScreen(absXY);
                parentOffset = new int[2];
                getLocationOnScreen(parentOffset);
                absXY[0] = absXY[0] - parentOffset[0];
                if (childView instanceof LauncherAppWidgetHostView) {
                    right = (int) (((float) right) * childView.getScaleX());
                    left = (int) (((float) left) * childView.getScaleX());
                    bottom = (int) (((float) bottom) * childView.getScaleY());
                    top = (int) (((float) top) * childView.getScaleY());
                }
                Rect rect = new Rect(absXY[0], absXY[1], absXY[0] + (right - left), absXY[1] + (bottom - top));
                float moveX;
                float moveY;
                if (childView instanceof LauncherAppWidgetHostView) {
                    float scaleX = ((float) prevRect.width()) / ((float) rect.width());
                    float scaleY = ((float) prevRect.height()) / ((float) rect.height());
                    moveX = ((float) (prevRect.left - rect.left)) / scaleX;
                    moveY = ((float) (prevRect.top - rect.top)) / scaleY;
                    scaleAnim = new ScaleAnimation(scaleX, 1.0f, scaleY, 1.0f, 1, 0.0f, 1, 0.0f);
                    translateAnimation = new TranslateAnimation(moveX, 0.0f, moveY, 0.0f);
                } else {
                    float scale = ((float) prevRect.height()) / ((float) rect.height());
                    moveX = ((((float) prevRect.left) + (((float) prevRect.width()) / 2.0f)) - (((float) rect.left) + (((float) rect.width()) / 2.0f))) / scale;
                    moveY = ((((float) prevRect.top) + (((float) prevRect.height()) / 2.0f)) - (((float) rect.top) + (((float) rect.height()) / 2.0f))) / scale;
                    ScaleAnimation scaleAnimation = new ScaleAnimation(scale, 1.0f, scale, 1.0f, 1, 0.5f, 1, 0.5f);
                    translateAnimation = new TranslateAnimation(moveX, 0.0f, moveY, 0.0f);
                }
                translateAnimation = new AnimationSet(true);
                translateAnimation.setDuration(300);
                if (getParent() instanceof CellLayout) {
                    if (((WorkspaceCellLayout) getParent()).mRestoredItems.contains(pairItem)) {
                        translateAnimation.addAnimation(new AlphaAnimation(0.0f, 1.0f));
                    } else {
                        translateAnimation.addAnimation(translate);
                    }
                }
                translateAnimation.addAnimation(scaleAnim);
                childView.setAnimation(translateAnimation);
                this.mGridChangeAnimationSet.addAnimation(translateAnimation);
            }
        }
    }

    public void makePreviousRectMap(View childView) {
        Object tag = childView.getTag();
        int[] absXY = new int[2];
        childView.getLocationOnScreen(absXY);
        parentOffset = new int[2];
        getLocationOnScreen(parentOffset);
        absXY[0] = absXY[0] - parentOffset[0];
        long dbId = tag == null ? -100 : ((ItemInfo) tag).id;
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        if (childView instanceof LauncherAppWidgetHostView) {
            scaleX = childView.getScaleX();
            scaleY = childView.getScaleY();
        }
        this.mPreviousRectMap.put(dbId, new Rect((int) ((float) absXY[0]), (int) ((float) absXY[1]), (int) (((float) absXY[0]) + (((float) childView.getWidth()) * scaleX)), (int) (((float) absXY[1]) + (((float) childView.getHeight()) * scaleY))));
    }

    public void setGridChangeState(boolean isGridChanging) {
        this.mIsGridChanging = isGridChanging;
    }

    public boolean isGridChanging() {
        return this.mIsGridChanging;
    }

    public void callRefreshLiveIcon() {
        Iterator it = getChildrenAllItems().iterator();
        while (it.hasNext()) {
            View childView = getChildAt((ItemInfo) it.next());
            if (childView instanceof IconView) {
                ((IconView) childView).onLiveIconRefresh();
            }
        }
    }
}

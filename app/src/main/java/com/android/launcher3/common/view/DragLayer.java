package com.android.launcher3.common.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.common.base.view.InsettableFrameLayout;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.drag.DragManager;
import com.android.launcher3.common.drag.DragState;
import com.android.launcher3.common.drag.DragView;
import com.android.launcher3.common.multiselect.MultiSelectManager;
import com.android.launcher3.common.quickoption.shortcuts.DeepShortcutManager;
import com.android.launcher3.common.quickoption.shortcuts.DeepShortcutsContainer;
import com.android.launcher3.common.stage.StageManager;
import com.android.launcher3.home.AppWidgetResizeFrame;
import com.android.launcher3.home.LauncherAppWidgetHostView;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Iterator;

public class DragLayer extends InsettableFrameLayout {
    public static final int ANIMATION_END_DISAPPEAR = 0;
    public static final int ANIMATION_END_REMAIN_VISIBLE = 2;
    public static final int ICON_FLICKING_DURATION = 480;
    private static final int SCRIM_COLOR = 0;
    private static final String TAG = "DragLayer";
    private View mAnchorView = null;
    private Rect mAnchorViewInitialRect = null;
    private int mAnchorViewInitialScrollX = 0;
    private float mBackgroundAlpha = 0.0f;
    private ImageView mBackgroundImage;
    private float mBackgroundImageAlpha = -1.0f;
    private int mChildCountOnLastUpdate = -1;
    private final TimeInterpolator mCubicEaseOutInterpolator = new DecelerateInterpolator(1.5f);
    private DragManager mDragMgr;
    private ValueAnimator mDropAnim = null;
    private DragView mDropView = null;
    private final Rect mHitRect = new Rect();
    private boolean mIsNaviBarPositionChanged = false;
    private Launcher mLauncher;
    private AppWidgetResizeFrame mResizeFrame;
    private final int[] mTmpXY = new int[2];
    private int mTopViewIndex;
    private TouchCompleteListener mTouchCompleteListener;
    private boolean mWillOrientationChange = false;
    private Rect mWindowInset = new Rect();
    private int mXDown;
    private int mYDown;

    public interface TouchCompleteListener {
        void onTouchComplete();
    }

    public static class LayoutParams extends com.android.launcher3.common.base.view.InsettableFrameLayout.LayoutParams {
        public boolean customPosition = false;
        public int x;
        public int y;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams lp) {
            super(lp);
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getWidth() {
            return this.width;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getHeight() {
            return this.height;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getX() {
            return this.x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getY() {
            return this.y;
        }
    }

    public DragLayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMotionEventSplittingEnabled(false);
        setChildrenDrawingOrderEnabled(true);
    }

    public void setup(Launcher launcher, DragManager dragMgr) {
        this.mLauncher = launcher;
        this.mDragMgr = dragMgr;
        this.mDragMgr.setScrollView(this);
        this.mBackgroundImage = (ImageView) findViewById(R.id.launcher_bg);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        return this.mDragMgr.dispatchKeyEvent() || super.dispatchKeyEvent(event);
    }

    public boolean isEventOverView(View view, MotionEvent ev) {
        getDescendantRectRelativeToSelf(view, this.mHitRect);
        return this.mHitRect.contains((int) ev.getX(), (int) ev.getY());
    }

    private boolean handleTouchDown(MotionEvent ev, boolean intercept) {
        Rect hitRect = new Rect();
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        if (this.mResizeFrame != null) {
            this.mResizeFrame.getHitRect(hitRect);
            if (hitRect.contains(x, y) && this.mResizeFrame.beginResizeIfPointInRegion(x - this.mResizeFrame.getLeft(), y - this.mResizeFrame.getTop())) {
                this.mResizeFrame.onTouchDown();
                this.mXDown = x;
                this.mYDown = y;
                requestDisallowInterceptTouchEvent(true);
                return true;
            }
        }
        MultiSelectManager multiSelectManager = this.mLauncher.getMultiSelectManager();
        if (multiSelectManager != null && multiSelectManager.isMultiSelectMode() && multiSelectManager.isShowingHelpDialog() && multiSelectManager.handleTouchDown(ev)) {
            return true;
        }
        if (LauncherFeature.supportDeepShortcut()) {
            DeepShortcutManager shortcutManager = LauncherAppState.getInstance().getShortcutManager();
            DeepShortcutsContainer deepShortcutsContainer = shortcutManager.getOpenShortcutsContainer(this.mLauncher);
            if (deepShortcutsContainer != null) {
                if (isEventOverView(deepShortcutsContainer, ev)) {
                    return false;
                }
                shortcutManager.closeShortcutsContainer(this.mLauncher);
                if (isEventOverView(deepShortcutsContainer.getDeferredDragIcon(), ev)) {
                    return false;
                }
                return true;
            }
        }
        if (intercept || !this.mLauncher.finishStageOnTouchOutSide()) {
            return false;
        }
        return true;
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (LauncherFeature.supportQuickOption() && this.mDragMgr.isQuickOptionShowing() && ev.getAction() == 0) {
            Rect hitRect = new Rect();
            int x = (int) ev.getX();
            int y = (int) ev.getY();
            this.mDragMgr.getQuickOptionView().getHitRect(hitRect);
            if (!hitRect.contains(x, y)) {
                this.mDragMgr.removeQuickOptionView("2");
                if (this.mResizeFrame == null) {
                    return true;
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == 0) {
            if (handleTouchDown(ev, true)) {
                return true;
            }
        } else if (action == 1 || action == 3) {
            if (this.mTouchCompleteListener != null) {
                this.mTouchCompleteListener.onTouchComplete();
            }
            this.mTouchCompleteListener = null;
        }
        return this.mDragMgr.onInterceptTouchEvent(ev);
    }

    public boolean onHoverEvent(MotionEvent ev) {
        return false;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled = false;
        int action = ev.getAction();
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        if (action == 0) {
            if (handleTouchDown(ev, false)) {
                return true;
            }
        } else if (action == 1 || action == 3) {
            if (this.mTouchCompleteListener != null) {
                this.mTouchCompleteListener.onTouchComplete();
            }
            this.mTouchCompleteListener = null;
        }
        if (this.mResizeFrame != null) {
            handled = true;
            switch (action) {
                case 1:
                case 3:
                    this.mResizeFrame.visualizeResizeForDelta(x - this.mXDown, y - this.mYDown);
                    this.mResizeFrame.onTouchUp();
                    break;
                case 2:
                    this.mResizeFrame.visualizeResizeForDelta(x - this.mXDown, y - this.mYDown);
                    break;
            }
        }
        if (handled) {
            return true;
        }
        return this.mDragMgr.onTouchEvent(ev);
    }

    public void addChildrenForAccessibility(ArrayList<View> childrenForAccessibility) {
        if (Talk.INSTANCE.isAccessibilityEnabled() && this.mDragMgr != null && this.mDragMgr.isQuickOptionShowing()) {
            Iterator it = this.mDragMgr.getQuickOptionView().getAccessibilityFocusChildViewList().iterator();
            while (it.hasNext()) {
                childrenForAccessibility.add((View) it.next());
            }
            return;
        }
        super.addChildrenForAccessibility(childrenForAccessibility);
    }

    public float getDescendantRectRelativeToSelf(View descendant, Rect r, boolean adjustMultiWindowPanel) {
        this.mTmpXY[0] = 0;
        this.mTmpXY[1] = 0;
        float scale = getDescendantCoordRelativeToSelf(descendant, this.mTmpXY);
        r.set(this.mTmpXY[0], this.mTmpXY[1], (int) (((float) this.mTmpXY[0]) + (((float) descendant.getMeasuredWidth()) * scale)), (int) (((float) this.mTmpXY[1]) + (((float) descendant.getMeasuredHeight()) * scale)));
        if (this.mLauncher != null && adjustMultiWindowPanel) {
            DeviceProfile dp = this.mLauncher.getDeviceProfile();
            if (dp.isMultiwindowMode) {
                if (!dp.isLandscape) {
                    r.top += dp.getMultiWindowPanelSize();
                    r.bottom += dp.getMultiWindowPanelSize();
                } else if (Utilities.getNavigationBarPositon() == 1) {
                    r.left += dp.getMultiWindowPanelSize() + dp.navigationBarHeight;
                    r.right += dp.getMultiWindowPanelSize() + dp.navigationBarHeight;
                } else {
                    r.left += dp.getMultiWindowPanelSize();
                    r.right += dp.getMultiWindowPanelSize();
                }
            }
        }
        return scale;
    }

    public float getDescendantRectRelativeToSelf(View descendant, Rect r) {
        return getDescendantRectRelativeToSelf(descendant, r, true);
    }

    public float getLocationInDragLayer(View child, int[] loc) {
        loc[0] = 0;
        loc[1] = 0;
        return getDescendantCoordRelativeToSelf(child, loc);
    }

    public float getDescendantCoordRelativeToSelf(View descendant, int[] coord) {
        return getDescendantCoordRelativeToSelf(descendant, coord, false);
    }

    public float getDescendantCoordRelativeToSelf(View descendant, int[] coord, boolean includeRootScroll) {
        return Utilities.getDescendantCoordRelativeToParent(descendant, this, coord, includeRootScroll);
    }

    public float mapCoordInSelfToDescendent(View descendant, int[] coord) {
        return Utilities.mapCoordInSelfToDescendent(descendant, this, coord);
    }

    public void getViewRectRelativeToSelf(View v, Rect r) {
        int[] loc = new int[2];
        getLocationInWindow(loc);
        int x = loc[0];
        int y = loc[1];
        v.getLocationInWindow(loc);
        int left = loc[0] - x;
        int top = loc[1] - y;
        r.set(left, top, v.getMeasuredWidth() + left, v.getMeasuredHeight() + top);
    }

    public boolean dispatchUnhandledMove(View focused, int direction) {
        return this.mDragMgr.dispatchUnhandledMove(focused, direction);
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    protected boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    protected LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            android.widget.FrameLayout.LayoutParams flp = (android.widget.FrameLayout.LayoutParams) child.getLayoutParams();
            if (flp instanceof LayoutParams) {
                LayoutParams lp = (LayoutParams) flp;
                if (lp.customPosition) {
                    child.layout(lp.x, lp.y, lp.x + lp.width, lp.y + lp.height);
                }
            }
        }
    }

    public boolean clearAllResizeFrames() {
        if (this.mResizeFrame == null) {
            return false;
        }
        this.mResizeFrame.commitResize();
        removeView(this.mResizeFrame);
        this.mResizeFrame = null;
        return true;
    }

    public void addResizeFrame(DragState dragState, LauncherAppWidgetHostView widget, CellLayout cellLayout) {
        if (this.mResizeFrame != null) {
            removeView(this.mResizeFrame);
            this.mResizeFrame = null;
        }
        if (widget == null) {
            Log.d(TAG, "addResizeFrame() : widget hostview is null");
            return;
        }
        AppWidgetResizeFrame resizeFrame = new AppWidgetResizeFrame(getContext(), dragState, widget, cellLayout, this);
        LayoutParams lp = new LayoutParams(-1, -1);
        lp.customPosition = true;
        addView(resizeFrame, lp);
        this.mResizeFrame = resizeFrame;
        resizeFrame.snapToWidget(false);
        if (widget.getAppWidgetInfo() != null) {
            SALogging.getInstance().insertEnterResizeWidgetLog();
        }
    }

    public boolean isResizeFrameArea(float x, float y) {
        if (this.mResizeFrame != null) {
            Rect hitRect = new Rect();
            this.mResizeFrame.getHitRect(hitRect);
            if (hitRect.contains((int) x, (int) y) && !this.mResizeFrame.beginResizeIfPointInRegion(((int) x) - this.mResizeFrame.getLeft(), ((int) y) - this.mResizeFrame.getTop())) {
                return true;
            }
        }
        return false;
    }

    public void animateViewIntoPosition(DragView dragView, int[] pos, float alpha, float scaleX, float scaleY, int animationEndStyle, Runnable onFinishRunnable, int duration) {
        Rect r = new Rect();
        getViewRectRelativeToSelf(dragView, r);
        animateViewIntoPosition(dragView, r.left, r.top, pos[0], pos[1], alpha, 1.0f, 1.0f, scaleX, scaleY, onFinishRunnable, animationEndStyle, duration, null);
    }

    public void animateViewIntoPosition(DragView dragView, View child, Runnable onFinishAnimationRunnable, View anchorView) {
        animateViewIntoPosition(dragView, child, -1, onFinishAnimationRunnable, anchorView, 0);
    }

    public void animateViewIntoPosition(DragView dragView, View child, int duration, Runnable onFinishAnimationRunnable, View anchorView) {
        animateViewIntoPosition(dragView, child, duration, onFinishAnimationRunnable, anchorView, 0);
    }

    public void animateViewIntoPosition(DragView dragView, View child, int duration, Runnable onFinishAnimationRunnable, View anchorView, int translatedX) {
        animateViewIntoPosition(dragView, child, duration, onFinishAnimationRunnable, anchorView, translatedX, 0);
    }

    public void animateViewIntoPosition(DragView dragView, View child, int duration, Runnable onFinishAnimationRunnable, View anchorView, int translatedX, int translatedY) {
        Rect r = new Rect();
        getViewRectRelativeToSelf(dragView, r);
        int[] coord = new int[2];
        float childScale = 1.0f;
        if (child.getParent() instanceof CellLayoutChildren) {
            com.android.launcher3.common.base.view.CellLayout.LayoutParams lp = (com.android.launcher3.common.base.view.CellLayout.LayoutParams) child.getLayoutParams();
            ((CellLayoutChildren) child.getParent()).measureChild(child);
            childScale = child.getScaleX();
            coord[0] = lp.x + ((int) ((((float) child.getMeasuredWidth()) * (1.0f - childScale)) / 2.0f));
            coord[1] = lp.y + ((int) ((((float) child.getMeasuredHeight()) * (1.0f - childScale)) / 2.0f));
        }
        float scale = getDescendantCoordRelativeToSelf((View) child.getParent(), coord) * childScale;
        int toX = coord[0] + translatedX;
        int toY = coord[1] + translatedY;
        float toScale = scale;
        if (child instanceof IconView) {
            IconView iconView = (IconView) child;
            android.widget.FrameLayout.LayoutParams frameLp = (android.widget.FrameLayout.LayoutParams) iconView.getIconVew().getLayoutParams();
            if (!dragView.isExtraDragView()) {
                toScale = scale * (((float) iconView.getIconSize()) / ((float) dragView.getIntrinsicIconSize()));
            }
            toY = (int) (((float) (toY + Math.round(((float) ((dragView.isExtraDragView() ? dragView.getTopDelta() : 0) + (frameLp.topMargin + iconView.getPaddingTop()))) * toScale))) - ((((float) dragView.getMeasuredHeight()) * (1.0f - toScale)) / 2.0f));
            if (dragView.getDragVisualizeOffset() != null) {
                toY -= Math.round(((float) dragView.getDragVisualizeOffset().y) * toScale);
            }
            toX -= (dragView.getMeasuredWidth() - Math.round(((float) child.getMeasuredWidth()) * scale)) / 2;
            if (iconView.isLandscape()) {
                toX = (coord[0] + translatedX) + Math.round(((float) iconView.getIconInfo().getIconStartPadding()) * scale);
                toY = (coord[1] + translatedY) + (Math.round(((float) (child.getMeasuredHeight() - dragView.getHeight())) * scale) / 2);
            }
        } else {
            toY -= Math.round(((float) (dragView.getHeight() - child.getMeasuredHeight())) * scale) / 2;
            toX -= Math.round(((float) (dragView.getMeasuredWidth() - child.getMeasuredWidth())) * scale) / 2;
        }
        int fromX = r.left;
        int fromY = r.top;
        child.setVisibility(4);
        final View view = child;
        final Runnable runnable = onFinishAnimationRunnable;
        animateViewIntoPosition(dragView, fromX, fromY, toX, toY - dragView.getTopDelta(), 1.0f, 1.0f, 1.0f, toScale, toScale, new Runnable() {
            public void run() {
                view.setVisibility(View.VISIBLE);
                if (runnable != null) {
                    runnable.run();
                }
                if (view instanceof IconView) {
                    TextView countBadge = ((IconView) view).getCountBadgeView();
                    if (countBadge != null && countBadge.getVisibility() == 0) {
                        ((IconView) view).updateCountBadge(false, true);
                    }
                    ((IconView) view).refreshBadge();
                }
            }
        }, 0, duration, anchorView);
    }

    public void animateViewIntoPosition(DragView view, int fromX, int fromY, int toX, int toY, float finalAlpha, float initScaleX, float initScaleY, float finalScaleX, float finalScaleY, Runnable onCompleteRunnable, int animationEndStyle, int duration, View anchorView) {
        animateView(view, new Rect(fromX, fromY, view.getMeasuredWidth() + fromX, view.getMeasuredHeight() + fromY), new Rect(toX, toY, view.getMeasuredWidth() + toX, view.getMeasuredHeight() + toY), finalAlpha, initScaleX, initScaleY, finalScaleX, finalScaleY, duration, null, null, onCompleteRunnable, animationEndStyle, anchorView);
    }

    public void animateView(DragView view, Rect from, Rect to, float finalAlpha, float initScaleX, float initScaleY, float finalScaleX, float finalScaleY, int duration, Interpolator motionInterpolator, Interpolator alphaInterpolator, Runnable onCompleteRunnable, int animationEndStyle, View anchorView) {
        float dist = (float) Math.hypot((double) (to.left - from.left), (double) (to.top - from.top));
        Resources res = getResources();
        float maxDist = (float) res.getInteger(R.integer.config_dropAnimMaxDist);
        if (duration < 0) {
            duration = res.getInteger(R.integer.config_dropAnimMaxDuration);
            if (dist < maxDist) {
                duration = (int) (((float) duration) * this.mCubicEaseOutInterpolator.getInterpolation(dist / maxDist));
            }
            duration = Math.max(duration, res.getInteger(R.integer.config_dropAnimMinDuration));
        }
        TimeInterpolator interpolator = null;
        if (alphaInterpolator == null || motionInterpolator == null) {
            interpolator = this.mCubicEaseOutInterpolator;
        }
        if (view.isExtraDragView()) {
            Rect anchorViewInitialRect;
            int anchorViewInitialScrollX;
            final float initAlpha = view.getAlpha();
            final float dropViewScale = view.getScaleX();
            if (anchorView != null) {
                Rect anchorViewRect = new Rect();
                getDescendantRectRelativeToSelf(anchorView, anchorViewRect);
                anchorViewInitialRect = anchorViewRect;
                anchorViewInitialScrollX = anchorView.getScrollX();
            } else {
                anchorViewInitialRect = null;
                anchorViewInitialScrollX = 0;
            }
            final DragView dragView = view;
            final Interpolator interpolator2 = alphaInterpolator;
            final Interpolator interpolator3 = motionInterpolator;
            final float f = initScaleX;
            final float f2 = initScaleY;
            final float f3 = finalScaleX;
            final float f4 = finalScaleY;
            final float f5 = finalAlpha;
            final Rect rect = from;
            final Rect rect2 = to;
            final View view2 = anchorView;
            animateExtraDragView(view, new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    float alphaPercent;
                    float motionPercent;
                    float percent = ((Float) animation.getAnimatedValue()).floatValue();
                    int width = dragView.getMeasuredWidth();
                    int height = dragView.getMeasuredHeight();
                    if (interpolator2 == null) {
                        alphaPercent = percent;
                    } else {
                        alphaPercent = interpolator2.getInterpolation(percent);
                    }
                    if (interpolator3 == null) {
                        motionPercent = percent;
                    } else {
                        motionPercent = interpolator3.getInterpolation(percent);
                    }
                    float initialScaleX = f * dropViewScale;
                    float initialScaleY = f2 * dropViewScale;
                    float scaleX = (f3 * percent) + ((1.0f - percent) * initialScaleX);
                    float scaleY = (f4 * percent) + ((1.0f - percent) * initialScaleY);
                    float alpha = (f5 * alphaPercent) + (initAlpha * (1.0f - alphaPercent));
                    float fromLeft = ((float) rect.left) + (((initialScaleX - 1.0f) * ((float) width)) / 2.0f);
                    float fromTop = ((float) rect.top) + (((initialScaleY - 1.0f) * ((float) height)) / 2.0f);
                    int x = (int) (((float) Math.round((((float) rect2.left) - fromLeft) * motionPercent)) + fromLeft);
                    int y = (int) (((float) Math.round((((float) rect2.top) - fromTop) * motionPercent)) + fromTop);
                    int anchorAdjustX = 0;
                    int anchorAdjustY = 0;
                    if (view2 != null) {
                        int moveX = 0;
                        int moveY = 0;
                        Rect anchorViewRect = new Rect();
                        float scale = DragLayer.this.getDescendantRectRelativeToSelf(view2, anchorViewRect);
                        if (anchorViewInitialRect != null) {
                            moveX = (int) ((((((float) anchorViewRect.left) / scale) - ((float) anchorViewInitialRect.left)) * scale) + 0.5f);
                            moveY = (int) ((((((float) anchorViewRect.top) / scale) - ((float) anchorViewInitialRect.top)) * scale) + 0.5f);
                        }
                        anchorAdjustX = (int) ((view2.getScaleX() * ((float) (anchorViewInitialScrollX - view2.getScrollX()))) + ((float) moveX));
                        anchorAdjustY = moveY;
                    }
                    int yPos = (y - dragView.getScrollY()) + anchorAdjustY;
                    dragView.setTranslationX((float) ((x - dragView.getScrollX()) + anchorAdjustX));
                    dragView.setTranslationY((float) yPos);
                    dragView.setScaleX(scaleX);
                    dragView.setScaleY(scaleY);
                    dragView.setAlpha(alpha);
                }
            }, duration, interpolator, onCompleteRunnable);
            return;
        }
        initAlpha = view.getAlpha();
        dropViewScale = view.getScaleX();
        dragView = view;
        interpolator2 = alphaInterpolator;
        interpolator3 = motionInterpolator;
        f = initScaleX;
        f2 = initScaleY;
        f3 = finalScaleX;
        f4 = finalScaleY;
        f5 = finalAlpha;
        rect = from;
        rect2 = to;
        animateView(view, new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float alphaPercent;
                float motionPercent;
                float percent = ((Float) animation.getAnimatedValue()).floatValue();
                int width = dragView.getMeasuredWidth();
                int height = dragView.getMeasuredHeight();
                if (interpolator2 == null) {
                    alphaPercent = percent;
                } else {
                    alphaPercent = interpolator2.getInterpolation(percent);
                }
                if (interpolator3 == null) {
                    motionPercent = percent;
                } else {
                    motionPercent = interpolator3.getInterpolation(percent);
                }
                float initialScaleX = f * dropViewScale;
                float initialScaleY = f2 * dropViewScale;
                float scaleX = (f3 * percent) + ((1.0f - percent) * initialScaleX);
                float scaleY = (f4 * percent) + ((1.0f - percent) * initialScaleY);
                float alpha = (f5 * alphaPercent) + (initAlpha * (1.0f - alphaPercent));
                float fromLeft = ((float) rect.left) + (((initialScaleX - 1.0f) * ((float) width)) / 2.0f);
                float fromTop = ((float) rect.top) + (((initialScaleY - 1.0f) * ((float) height)) / 2.0f);
                int x = (int) (((float) Math.round((((float) rect2.left) - fromLeft) * motionPercent)) + fromLeft);
                int y = (int) (((float) Math.round((((float) rect2.top) - fromTop) * motionPercent)) + fromTop);
                int anchorAdjustX = 0;
                int anchorAdjustY = 0;
                if (DragLayer.this.mAnchorView != null) {
                    int moveX = 0;
                    int moveY = 0;
                    Rect anchorViewRect = new Rect();
                    float scale = DragLayer.this.getDescendantRectRelativeToSelf(DragLayer.this.mAnchorView, anchorViewRect);
                    if (DragLayer.this.mAnchorViewInitialRect != null) {
                        moveX = (int) ((((((float) anchorViewRect.left) / scale) - ((float) DragLayer.this.mAnchorViewInitialRect.left)) * scale) + 0.5f);
                        moveY = (int) ((((((float) anchorViewRect.top) / scale) - ((float) DragLayer.this.mAnchorViewInitialRect.top)) * scale) + 0.5f);
                    }
                    anchorAdjustX = (int) ((DragLayer.this.mAnchorView.getScaleX() * ((float) (DragLayer.this.mAnchorViewInitialScrollX - DragLayer.this.mAnchorView.getScrollX()))) + ((float) moveX));
                    anchorAdjustY = moveY;
                }
                int yPos = (y - DragLayer.this.mDropView.getScrollY()) + anchorAdjustY;
                DragLayer.this.mDropView.setTranslationX((float) ((x - DragLayer.this.mDropView.getScrollX()) + anchorAdjustX));
                DragLayer.this.mDropView.setTranslationY((float) yPos);
                DragLayer.this.mDropView.setScaleX(scaleX);
                DragLayer.this.mDropView.setScaleY(scaleY);
                DragLayer.this.mDropView.setAlpha(alpha);
            }
        }, duration, interpolator, onCompleteRunnable, animationEndStyle, anchorView);
    }

    public void animateView(DragView view, AnimatorUpdateListener updateCb, int duration, TimeInterpolator interpolator, final Runnable onCompleteRunnable, final int animationEndStyle, View anchorView) {
        if (this.mDropAnim != null) {
            this.mDropAnim.cancel();
        }
        this.mDropView = view;
        this.mDropView.cancelAnimation();
        this.mDropView.resetLayoutParams();
        this.mAnchorView = anchorView;
        this.mAnchorViewInitialRect = null;
        this.mDropAnim = new ValueAnimator();
        this.mDropAnim.setInterpolator(interpolator);
        this.mDropAnim.setDuration((long) duration);
        this.mDropAnim.setFloatValues(new float[]{0.0f, 1.0f});
        this.mDropAnim.addUpdateListener(updateCb);
        this.mDropAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                if (DragLayer.this.mAnchorView != null) {
                    Rect anchorViewRect = new Rect();
                    float scale = DragLayer.this.getDescendantRectRelativeToSelf(DragLayer.this.mAnchorView, anchorViewRect);
                    if (scale != 1.0f) {
                        anchorViewRect.left = (int) ((((float) anchorViewRect.left) / scale) + 0.5f);
                        anchorViewRect.top = (int) ((((float) anchorViewRect.top) / scale) + 0.5f);
                        anchorViewRect.right = (int) ((((float) anchorViewRect.right) / scale) + 0.5f);
                        anchorViewRect.bottom = (int) ((((float) anchorViewRect.bottom) / scale) + 0.5f);
                    }
                    DragLayer.this.mAnchorViewInitialRect = anchorViewRect;
                    DragLayer.this.mAnchorViewInitialScrollX = DragLayer.this.mAnchorView.getScrollX();
                }
            }

            public void onAnimationEnd(Animator animation) {
                if (onCompleteRunnable != null) {
                    onCompleteRunnable.run();
                }
                switch (animationEndStyle) {
                    case 0:
                        DragLayer.this.clearAnimatedView();
                        break;
                }
                DragLayer.this.mDropAnim.removeAllListeners();
                DragLayer.this.mDropAnim = null;
            }
        });
        this.mDropAnim.start();
    }

    private void animateExtraDragView(final DragView view, AnimatorUpdateListener updateCb, int duration, TimeInterpolator interpolator, final Runnable onCompleteRunnable) {
        view.cancelAnimation();
        view.resetLayoutParams();
        ValueAnimator dropAnim = new ValueAnimator();
        dropAnim.setInterpolator(interpolator);
        dropAnim.setDuration((long) duration);
        dropAnim.setFloatValues(new float[]{0.0f, 1.0f});
        dropAnim.addUpdateListener(updateCb);
        dropAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (onCompleteRunnable != null) {
                    onCompleteRunnable.run();
                }
                if (view != null) {
                    DragLayer.this.mDragMgr.onDeferredEndDrag(view);
                }
                DragLayer.this.invalidate();
            }
        });
        dropAnim.start();
    }

    public void removeAnimation(DragView view, Runnable onCompleteRunnable) {
        this.mDropView = view;
        this.mDropView.cancelAnimation();
        this.mDropView.resetLayoutParams();
        if (onCompleteRunnable != null) {
            onCompleteRunnable.run();
        }
        clearAnimatedView();
    }

    public void clearAnimatedView() {
        if (this.mDropAnim != null) {
            this.mDropAnim.cancel();
        }
        if (this.mDropView != null) {
            this.mDragMgr.onDeferredEndDrag(this.mDropView);
        }
        this.mDropView = null;
        invalidate();
    }

    public View getAnimatedView() {
        return this.mDropView;
    }

    public void onChildViewAdded(View parent, View child) {
        super.onChildViewAdded(parent, child);
        updateChildIndices();
    }

    public void onChildViewRemoved(View parent, View child) {
        updateChildIndices();
    }

    public void bringChildToFront(View child) {
        super.bringChildToFront(child);
        updateChildIndices();
    }

    private void updateChildIndices() {
        this.mTopViewIndex = -1;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (getChildAt(i) instanceof DragView) {
                this.mTopViewIndex = i;
            }
        }
        this.mChildCountOnLastUpdate = childCount;
    }

    protected int getChildDrawingOrder(int childCount, int i) {
        if (this.mChildCountOnLastUpdate != childCount) {
            updateChildIndices();
        }
        if (this.mTopViewIndex == -1) {
            return i;
        }
        if (i == childCount - 1) {
            return this.mTopViewIndex;
        }
        return i >= this.mTopViewIndex ? i + 1 : i;
    }

    protected void dispatchDraw(Canvas canvas) {
        if (this.mBackgroundAlpha > 0.0f) {
            canvas.drawColor((((int) (this.mBackgroundAlpha * 255.0f)) << 24) | 0);
        }
        Log.d(TAG, "DragLayer dispatchDraw, mIsNaviBarPositionChanged = " + this.mIsNaviBarPositionChanged + ", mWillOrientationChange = " + this.mWillOrientationChange);
        doWindowChange();
        super.dispatchDraw(canvas);
    }

    public void dispatchConfigurationChanged(Configuration newConfig) {
    }

    public void setBackgroundAlpha(float alpha) {
        if (alpha != this.mBackgroundAlpha) {
            this.mBackgroundAlpha = alpha;
            invalidate();
        }
    }

    public float getBackgroundAlpha() {
        return this.mBackgroundAlpha;
    }

    public void setBackgroundImage(int resId) {
        if (this.mBackgroundImage != null && resId > 0) {
            this.mBackgroundImage.setImageResource(resId);
            if (this.mBackgroundImage.getVisibility() != 0) {
                this.mBackgroundImage.setVisibility(View.VISIBLE);
            }
        }
    }

    public void setBackgroundImageAlpha(float alpha) {
        if (this.mBackgroundImage != null && alpha != this.mBackgroundImageAlpha) {
            this.mBackgroundImageAlpha = alpha;
            this.mBackgroundImage.setAlpha(alpha);
        }
    }

    public float getBackgroundImageAlpha() {
        return this.mBackgroundImageAlpha;
    }

    public void setTouchCompleteListener(TouchCompleteListener listener) {
        this.mTouchCompleteListener = listener;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d(TAG, "DragLayer onMeasure, mIsNaviBarPositionChanged = " + this.mIsNaviBarPositionChanged + ", mWillOrientationChange = " + this.mWillOrientationChange);
        doWindowChange();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void doWindowChange() {
        StageManager stageManager = this.mLauncher.getStageManager();
        if (this.mIsNaviBarPositionChanged && !this.mWillOrientationChange) {
            doNavibarPositionChange(stageManager);
        }
        if (stageManager.needToCallConfigurationChanged()) {
            Log.d(TAG, "DragLayer onConfigurationChanged");
            stageManager.onConfigurationChanged();
            if (this.mIsNaviBarPositionChanged && this.mWillOrientationChange) {
                this.mWillOrientationChange = false;
                doNavibarPositionChange(stageManager);
            }
        }
    }

    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        int i = 2;
        boolean z = false;
        Log.d(TAG, "DragLayer onApplyWindowInsets");
        if (LauncherFeature.supportNavigationBar() && isChangedWindowInset(insets)) {
            Log.d(TAG, "DragLayer onApplyWindowInsets changed");
            setWindowInset(insets);
            if (this.mWindowInset.bottom != 0 || Utilities.isMobileKeyboardMode()) {
                Utilities.setNavigationBarPosition(0);
                if (getResources().getConfiguration().orientation == 2) {
                    z = true;
                }
                this.mWillOrientationChange = z;
            } else {
                if (this.mWindowInset.right == 0) {
                    i = 1;
                }
                Utilities.setNavigationBarPosition(i);
                this.mWillOrientationChange = getResources().getConfiguration().orientation == 1;
            }
            this.mIsNaviBarPositionChanged = true;
            Log.d(TAG, "DragLayer onApplyWindowInsets mWillOrientationChange : " + this.mWillOrientationChange);
        }
        return super.onApplyWindowInsets(insets);
    }

    private boolean isChangedWindowInset(WindowInsets insets) {
        return (this.mWindowInset.bottom == insets.getSystemWindowInsetBottom() && this.mWindowInset.right == insets.getSystemWindowInsetRight() && this.mWindowInset.left == insets.getSystemWindowInsetLeft()) ? false : true;
    }

    private void setWindowInset(WindowInsets insets) {
        this.mWindowInset.bottom = insets.getSystemWindowInsetBottom();
        this.mWindowInset.right = insets.getSystemWindowInsetRight();
        this.mWindowInset.left = insets.getSystemWindowInsetLeft();
    }

    private void doNavibarPositionChange(StageManager stageManager) {
        Log.d(TAG, "DragLayer doNavibarPositionChange, navigationBarPositionChanged");
        this.mIsNaviBarPositionChanged = false;
        stageManager.navigationBarPositionChanged();
        post(new Runnable() {
            public void run() {
                if (DragLayer.this.mResizeFrame != null) {
                    DragLayer.this.mResizeFrame.snapToWidget(false);
                }
                if (DragLayer.this.mDragMgr != null && DragLayer.this.mDragMgr.isQuickOptionShowing()) {
                    DragLayer.this.mDragMgr.quickOptionNavigationBarPositionChanged();
                }
            }
        });
    }
}

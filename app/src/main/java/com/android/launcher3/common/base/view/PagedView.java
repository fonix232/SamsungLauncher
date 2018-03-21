package com.android.launcher3.common.base.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.LayoutTransition.TransitionListener;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.Trace;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.BaseSavedState;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.PageScroller;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.common.view.LauncherRootView;
import com.android.launcher3.common.view.PageIndicator;
import com.android.launcher3.common.view.PageIndicator.PageMarkerResources;
import com.android.launcher3.common.view.PageIndicatorMarker;
import com.android.launcher3.util.DvfsUtil;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.event.ScrollDeterminator;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class PagedView extends ViewGroup implements OnHierarchyChangeListener {
    private static final int ANIM_TAG_KEY = 100;
    private static final boolean DEBUG = false;
    public static final long EXTRA_EMPTY_SCREEN_ID = -201;
    protected static final float EXTRA_TOUCH_SLOP_SCALE_RATIO = 2.5f;
    private static final int FLING_THRESHOLD_VELOCITY = 500;
    public static final int HINT_PAGE_ANIMATION_DURATION = 200;
    protected static final int INVALID_PAGE = -1;
    protected static final int INVALID_POINTER = -1;
    public static final int INVALID_RESTORE_PAGE = -1001;
    public static final long INVALID_SCREEN_ID = -1;
    private static final float MAX_SCROLL_PROGRESS = 1.0f;
    private static final int MIN_FLING_VELOCITY = 250;
    private static final int MIN_LENGTH_FOR_FLING = 25;
    private static final int MIN_SNAP_VELOCITY = 1500;
    private static final int NUM_ANIMATIONS_RUNNING_BEFORE_ZOOM_OUT = 2;
    private static final float OVERSCROLL_DAMP_FACTOR = 0.32f;
    public static final int PAGE_SNAP_ANIMATION_DURATION;
    private static final int REORDERING_DROP_REPOSITION_DURATION = 200;
    private static final int REORDERING_REORDER_REPOSITION_DURATION = 300;
    private static final int REORDERING_SIDE_PAGE_HOVER_TIMEOUT = 80;
    private static final float RETURN_TO_ORIGINAL_PAGE_THRESHOLD = 0.33f;
    private static final float SIGNIFICANT_MOVE_THRESHOLD = 0.4f;
    public static final int SLOW_PAGE_SNAP_ANIMATION_DURATION = 950;
    private static final String TAG = "PagedView";
    protected static final int TOUCH_STATE_PINCH_ZOOM = 3;
    protected static final int TOUCH_STATE_REORDERING = 2;
    protected static final int TOUCH_STATE_REST = 0;
    protected static final int TOUCH_STATE_SCROLLING = 1;
    private static final float TRANSITION_BOUNCE_MIN_ALPHA = 0.7f;
    private static final float TRANSITION_BOUNCE_MIN_SCALE = 0.96f;
    private static final int[] sTmpIntPoint = new int[2];
    private static final Matrix sTmpInvMatrix = new Matrix();
    private static final float[] sTmpPoint = new float[2];
    private static final Rect sTmpRect = new Rect();
    private final int SCROLL_DONE;
    private final int SCROLL_NONE;
    private final int SCROLL_READY_TO_PULLING;
    private final int SCROLL_READY_TO_RETURN;
    private final int SCROLL_START_PULLING;
    private final int SCROLL_START_RETURN;
    private ArrayList<Integer> mActivePages;
    protected int mActivePointerId;
    private boolean mCancelTap;
    protected boolean mCenterPagesVertically;
    protected int mChildCountOnLastLayout;
    private boolean mContinuallyScroll;
    protected int mCurrentPage;
    private Interpolator mDefaultInterpolator;
    private boolean mDeferLoadAssociatedPagesUntilScrollCompletes;
    private ArrayList<Boolean> mDirtyPageContent;
    private float mDownMotionX;
    private float mDownMotionY;
    private float mDownScrollX;
    protected View mDragView;
    private float mDragViewBaselineLeft;
    private boolean mEnablePageIndicatorAnim;
    protected boolean mFadeInAdjacentScreens;
    protected int mFlingThresholdVelocity;
    protected boolean mForceDrawAllChildrenNextFrame;
    protected boolean mForceScreenScrolled;
    protected int mHintPageLeftZone;
    protected int mHintPageRightZone;
    protected int mHintPageWidth;
    protected final Rect mInsets;
    private boolean mIsDataReady;
    public boolean mIsPageMoving;
    private boolean mIsReordering;
    protected boolean mIsResumed;
    private boolean mIsShowingHintPages;
    protected float mLastMotionX;
    protected float mLastMotionY;
    private int mLastScreenCenter;
    protected OnLongClickListener mLongClickListener;
    protected int mMaxScrollX;
    private int mMaximumVelocity;
    protected int mMinFlingVelocity;
    private float mMinScale;
    protected int mMinSnapVelocity;
    protected int mNextPage;
    private int mNormalChildHeight;
    protected float mPageBackgroundAlpha;
    private PageIndicator mPageIndicator;
    private int mPageIndicatorViewId;
    protected int mPageLayoutHeightGap;
    protected int mPageLayoutWidthGap;
    private PageScrollListener mPageScrollListener;
    private int[] mPageScrolls;
    protected int mPageSpacing;
    private PageSwitchListener mPageSwitchListener;
    private float mParentDownMotionX;
    private float mParentDownMotionY;
    private int mPostReorderingPreZoomInRemainingAnimationCount;
    private Runnable mPostReorderingPreZoomInRunnable;
    private ObjectAnimator mPullingPagesAnim;
    private boolean mReorderingStarted;
    protected int mRestorePage;
    private ScrollDeterminator mScrollDeterminator;
    private int mScrollState;
    protected PageScroller mScroller;
    private int mSidePageHoverIndex;
    private Runnable mSidePageHoverRunnable;
    protected int[] mTempVisiblePagesRange;
    protected float mTotalMotionX;
    protected int mTouchSlop;
    protected int mTouchState;
    private TransitionListener mTransitionListener;
    private float mTranslateAllPages;
    protected float mTranslatePagesOffset;
    protected int mUnboundedScrollX;
    protected boolean mUpdateOnlyCurrentPage;
    private boolean mUseMinScale;
    private VelocityTracker mVelocityTracker;
    private Rect mViewport;

    public static class LayoutParams extends android.view.ViewGroup.LayoutParams {
        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    public interface PageScrollListener {
        void onPageChange(int i, int i2, int i3);

        void onPageScroll(int i, int i2, int i3, int i4, int i5);
    }

    interface PageSwitchListener {
        void onPageSwitch(View view, int i);
    }

    public static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int currentPage;

        SavedState(Parcelable superState) {
            super(superState);
            this.currentPage = -1;
        }

        private SavedState(Parcel in) {
            super(in);
            this.currentPage = -1;
            this.currentPage = in.readInt();
        }

        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.currentPage);
        }
    }

    public static class ScrollInterpolator implements Interpolator {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return ((((t * t) * t) * t) * t) + 1.0f;
        }
    }

    static {
        int i;
        if (Utilities.sIsRtl) {
            i = 400;
        } else {
            i = 750;
        }
        PAGE_SNAP_ANIMATION_DURATION = i;
    }

    public PagedView(Context context) {
        this(context, null);
    }

    public PagedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mRestorePage = -1001;
        this.mNextPage = -1;
        this.mPageSpacing = 0;
        this.mLastScreenCenter = -1;
        this.mTouchState = 0;
        this.mForceScreenScrolled = false;
        this.mTempVisiblePagesRange = new int[2];
        this.mActivePointerId = -1;
        this.mFadeInAdjacentScreens = false;
        this.mIsPageMoving = false;
        this.mViewport = new Rect();
        this.mMinScale = 1.0f;
        this.mUseMinScale = false;
        this.mSidePageHoverIndex = -1;
        this.mReorderingStarted = false;
        this.mInsets = new Rect();
        this.mActivePages = new ArrayList();
        this.mIsDataReady = false;
        this.mTranslateAllPages = 0.0f;
        this.mTranslatePagesOffset = 0.0f;
        this.mHintPageLeftZone = 0;
        this.mHintPageRightZone = 0;
        this.mHintPageWidth = 0;
        this.mScrollDeterminator = null;
        this.mUpdateOnlyCurrentPage = false;
        this.mTransitionListener = new TransitionListener() {
            public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
            }

            public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {
                if (!transition.isRunning()) {
                    transition.removeTransitionListener(this);
                    PagedView.this.updateMaxScrollX();
                }
            }
        };
        this.SCROLL_NONE = -1;
        this.SCROLL_READY_TO_PULLING = 0;
        this.SCROLL_READY_TO_RETURN = 1;
        this.SCROLL_START_PULLING = 2;
        this.SCROLL_START_RETURN = 3;
        this.SCROLL_DONE = 4;
        this.mScrollState = -1;
        this.mIsShowingHintPages = false;
        this.mContinuallyScroll = false;
        // TODO: Samsung specific code
//        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PagedView, defStyle, 0);
//        this.mPageLayoutWidthGap = a.getDimensionPixelSize(0, 0);
//        this.mPageLayoutHeightGap = a.getDimensionPixelSize(1, 0);
//        this.mPageIndicatorViewId = a.getResourceId(2, -1);
//        a.recycle();
        setHapticFeedbackEnabled(false);
        init();
    }

    protected void init() {
        this.mScroller = new PageScroller(getContext());
        setDefaultInterpolator(new ScrollInterpolator());
        this.mCurrentPage = 0;
        this.mCenterPagesVertically = true;
        this.mEnablePageIndicatorAnim = true;
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        this.mTouchSlop = configuration.getScaledTouchSlop();
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        float density = getResources().getDisplayMetrics().density;
        this.mFlingThresholdVelocity = (int) (500.0f * density);
        this.mMinFlingVelocity = (int) (250.0f * density);
        this.mMinSnapVelocity = (int) (1500.0f * density);
        setOnHierarchyChangeListener(this);
        setWillNotDraw(false);
        this.mDirtyPageContent = new ArrayList();
        this.mDirtyPageContent.ensureCapacity(32);
        this.mIsResumed = true;
    }

    protected void setDefaultInterpolator(Interpolator interpolator) {
        this.mDefaultInterpolator = interpolator;
        this.mScroller.setInterpolator(this.mDefaultInterpolator);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mPageIndicator == null && this.mPageIndicatorViewId > -1) {
            this.mPageIndicator = findPageIndicator();
            this.mPageIndicator.removeAllMarkers();
            ArrayList<PageMarkerResources> markers = new ArrayList();
            int indicatorCount = Math.min(getChildCount(), this.mPageIndicator.getMaxVisibleSize() - getSupportCustomPageCount());
            for (int i = 0; i < indicatorCount; i++) {
                markers.add(getPageIndicatorMarker(i));
            }
            this.mPageIndicator.addMarkers(markers, this.mEnablePageIndicatorAnim, this);
            OnClickListener listener = getPageIndicatorClickListener();
            if (listener != null) {
                this.mPageIndicator.setOnClickListener(listener);
            }
            this.mPageIndicator.setContentDescription(getPageIndicatorDescription());
        }
    }

    public PageIndicator findPageIndicator() {
        ViewGroup grandParent = (ViewGroup) ((ViewGroup) getParent()).getParent();
        if (grandParent.getParent() instanceof LauncherRootView) {
            grandParent = (ViewGroup) grandParent.getParent();
        }
        return (PageIndicator) grandParent.findViewById(this.mPageIndicatorViewId);
    }

    protected String getPageIndicatorDescription() {
        return getCurrentPageDescription();
    }

    protected OnClickListener getPageIndicatorClickListener() {
        return null;
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mPageIndicator = null;
    }

    private float[] mapPointFromViewToParent(View v, float x, float y) {
        sTmpPoint[0] = x;
        sTmpPoint[1] = y;
        v.getMatrix().mapPoints(sTmpPoint);
        float[] fArr = sTmpPoint;
        fArr[0] = fArr[0] + ((float) v.getLeft());
        fArr = sTmpPoint;
        fArr[1] = fArr[1] + ((float) v.getTop());
        return sTmpPoint;
    }

    private float[] mapPointFromParentToView(View v, float x, float y) {
        sTmpPoint[0] = x - ((float) v.getLeft());
        sTmpPoint[1] = y - ((float) v.getTop());
        v.getMatrix().invert(sTmpInvMatrix);
        sTmpInvMatrix.mapPoints(sTmpPoint);
        return sTmpPoint;
    }

    private void updateDragViewTranslationDuringDrag() {
        if (this.mDragView != null) {
            float y = this.mLastMotionY - this.mDownMotionY;
            this.mDragView.setTranslationX(((this.mLastMotionX - this.mDownMotionX) + (((float) getScrollX()) - this.mDownScrollX)) + (this.mDragViewBaselineLeft - ((float) this.mDragView.getLeft())));
            this.mDragView.setTranslationY(y);
        }
    }

    public void setMinScale(float f) {
        this.mMinScale = f;
        this.mUseMinScale = true;
        requestLayout();
    }

    public void setScaleX(float scaleX) {
        super.setScaleX(scaleX);
        if (isReordering(true)) {
            float[] p = mapPointFromParentToView(this, this.mParentDownMotionX, this.mParentDownMotionY);
            this.mLastMotionX = p[0];
            this.mLastMotionY = p[1];
            updateDragViewTranslationDuringDrag();
        }
    }

    public int getViewportWidth() {
        return this.mViewport.width();
    }

    public int getViewportHeight() {
        return this.mViewport.height();
    }

    public int getViewportOffsetX() {
        return (getMeasuredWidth() - getViewportWidth()) / 2;
    }

    public int getViewportOffsetY() {
        return (getMeasuredHeight() - getViewportHeight()) / 2;
    }

    public PageIndicator getPageIndicator() {
        return this.mPageIndicator;
    }

    protected PageMarkerResources getPageIndicatorMarker(int pageIndex) {
        return new PageMarkerResources();
    }

    public void setPageSwitchListener(PageSwitchListener pageSwitchListener) {
        this.mPageSwitchListener = pageSwitchListener;
        if (this.mPageSwitchListener != null) {
            this.mPageSwitchListener.onPageSwitch(getPageAt(this.mCurrentPage), this.mCurrentPage);
        }
    }

    public int getCurrentPage() {
        return this.mCurrentPage;
    }

    public int getNextPage() {
        return this.mNextPage != -1 ? this.mNextPage : this.mCurrentPage;
    }

    public int getPageCount() {
        return getChildCount();
    }

    public View getPageAt(int index) {
        return getChildAt(index);
    }

    protected int indexToPage(int index) {
        return index;
    }

    protected void updateCurrentPageScroll() {
        int newX = 0;
        if (this.mCurrentPage >= 0 && this.mCurrentPage < getPageCount()) {
            newX = getScrollForPage(this.mCurrentPage);
        }
        scrollTo(newX, 0);
        this.mScroller.setFinalX(newX);
        forceFinishScroller();
    }

    private void abortScrollerAnimation(boolean resetNextPage) {
        this.mScroller.abortAnimation();
        if (isContentsRefreshable() && this.mDeferLoadAssociatedPagesUntilScrollCompletes) {
            computeScroll();
        }
        if (resetNextPage) {
            this.mNextPage = -1;
        }
    }

    private void forceFinishScroller() {
        this.mScroller.forceFinished(true);
        if (this.mNextPage != -1) {
            this.mCurrentPage = validateNewPage(this.mNextPage);
            pageEndMoving();
            this.mNextPage = -1;
        }
    }

    private int validateNewPage(int newPage) {
        return Math.max(0, Math.min(newPage, getPageCount() - 1));
    }

    public void setCurrentPage(int currentPage) {
        if (!this.mScroller.isFinished()) {
            abortScrollerAnimation(true);
        }
        if (getChildCount() != 0) {
            this.mForceScreenScrolled = true;
            this.mCurrentPage = validateNewPage(currentPage);
            notifyPageChange(this.mCurrentPage, getScrollForPage(this.mCurrentPage), getPageCount());
            updateCurrentPageScroll();
            notifyPageSwitchListener();
            invalidate();
        }
    }

    public void setRestorePage(int restorePage) {
        this.mRestorePage = restorePage;
    }

    public int getRestorePage() {
        return this.mRestorePage;
    }

    protected void notifyPageSwitchListener() {
        if (this.mPageSwitchListener != null) {
            this.mPageSwitchListener.onPageSwitch(getPageAt(getNextPage()), getNextPage());
        }
        updatePageIndicator();
    }

    protected void pageBeginMoving() {
        if (!this.mIsPageMoving) {
            this.mIsPageMoving = true;
            onPageBeginMoving();
        }
    }

    protected void pageEndMoving() {
        if (this.mIsPageMoving) {
            this.mIsPageMoving = false;
            onPageEndMoving();
        }
    }

    public boolean isPageMoving() {
        return this.mIsPageMoving;
    }

    protected void onPageBeginMoving() {
    }

    protected void onPageEndMoving() {
    }

    public void setOnLongClickListener(OnLongClickListener l) {
        this.mLongClickListener = l;
        int count = getPageCount();
        for (int i = 0; i < count; i++) {
            getPageAt(i).setOnLongClickListener(l);
        }
        super.setOnLongClickListener(l);
    }

    public void scrollBy(int x, int y) {
        scrollTo(getScrollX() + x, getScrollY() + y);
    }

    public void scrollTo(int x, int y) {
        boolean isXBeforeFirstPage = Utilities.sIsRtl ? x > this.mMaxScrollX : x < 0;
        boolean isXAfterLastPage = Utilities.sIsRtl ? x < 0 : x > this.mMaxScrollX;
        if (!(isXBeforeFirstPage || isXAfterLastPage) || canOverScroll()) {
            this.mUnboundedScrollX += x - getScrollX();
        }
        if (isXBeforeFirstPage) {
            int i;
            if (Utilities.sIsRtl) {
                i = this.mMaxScrollX;
            } else {
                i = 0;
            }
            super.scrollTo(i, y);
            if (canOverScroll()) {
                overScroll(Utilities.sIsRtl ? this.mUnboundedScrollX - this.mMaxScrollX : this.mUnboundedScrollX);
            }
        } else if (isXAfterLastPage) {
            super.scrollTo(Utilities.sIsRtl ? 0 : this.mMaxScrollX, y);
            if (canOverScroll()) {
                overScroll(Utilities.sIsRtl ? this.mUnboundedScrollX : this.mUnboundedScrollX - this.mMaxScrollX);
            }
        } else {
            super.scrollTo(x, y);
        }
        if (isReordering(true)) {
            float[] p = mapPointFromParentToView(this, this.mParentDownMotionX, this.mParentDownMotionY);
            this.mLastMotionX = p[0];
            this.mLastMotionY = p[1];
            updateDragViewTranslationDuringDrag();
        }
        if (x >= 0 && x <= this.mMaxScrollX) {
            notifyPageScroll(this.mCurrentPage, x, y, getScrollForPage(this.mCurrentPage), getPageCount());
        }
    }

    protected boolean computeScrollHelper() {
        if (this.mScroller.computeScrollOffset()) {
            if (!(getScrollX() == this.mScroller.getCurrX() && getScrollY() == this.mScroller.getCurrY())) {
                scrollTo((int) (((float) this.mScroller.getCurrX()) * (1.0f / 1.0f)), this.mScroller.getCurrY());
            }
            invalidate();
            return true;
        } else if (this.mNextPage == -1) {
            return false;
        } else {
            int oldPage = this.mCurrentPage;
            this.mCurrentPage = validateNewPage(this.mNextPage);
            this.mNextPage = -1;
            notifyPageChange(this.mCurrentPage, getScrollForPage(this.mCurrentPage), getPageCount());
            notifyPageSwitchListener();
            if (this.mDeferLoadAssociatedPagesUntilScrollCompletes && this.mCurrentPage != oldPage) {
                loadAssociatedPages(this.mCurrentPage);
                this.mDeferLoadAssociatedPagesUntilScrollCompletes = false;
            }
            if (this.mTouchState == 0) {
                pageEndMoving();
                if (getScrollX() != getScrollForPage(this.mCurrentPage)) {
                    post(new Runnable() {
                        public void run() {
                            PagedView.this.snapToPage(PagedView.this.mCurrentPage);
                        }
                    });
                }
            }
            onPostReorderingAnimationCompleted(false);
            if (!Talk.INSTANCE.isAccessibilityEnabled() || oldPage == this.mCurrentPage) {
                return true;
            }
            announceForAccessibility(getCurrentPageDescription());
            return true;
        }
    }

    public void computeScroll() {
        computeScrollHelper();
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(-2, -2);
    }

    protected android.view.ViewGroup.LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    protected boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public int getNormalChildHeight() {
        return this.mNormalChildHeight;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getChildCount() == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        int scaledWidthSize;
        int scaledHeightSize;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int maxSize = Math.max((dm.widthPixels + this.mInsets.left) + this.mInsets.right, (dm.heightPixels + this.mInsets.top) + this.mInsets.bottom);
        int parentWidthSize = (int) (2.0f * ((float) maxSize));
        int parentHeightSize = (int) (2.0f * ((float) maxSize));
        if (this.mUseMinScale) {
            scaledWidthSize = (int) (((float) parentWidthSize) / this.mMinScale);
            scaledHeightSize = (int) (((float) parentHeightSize) / this.mMinScale);
        } else {
            scaledWidthSize = widthSize;
            scaledHeightSize = heightSize;
        }
        this.mViewport.set(0, 0, widthSize, heightSize);
        if (widthMode == 0 || heightMode == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else if (widthSize <= 0 || heightSize <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            int verticalPadding = getPaddingTop() + getPaddingBottom();
            int horizontalPadding = getPaddingLeft() + getPaddingRight();
            int referenceChildWidth = 0;
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = getPageAt(i);
                if (child.getVisibility() != View.GONE) {
                    int childWidthMode;
                    int childHeightMode;
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    if (lp.width == -2) {
                        childWidthMode = Integer.MIN_VALUE;
                    } else {
                        childWidthMode = 1073741824;
                    }
                    if (lp.height == -2) {
                        childHeightMode = Integer.MIN_VALUE;
                    } else {
                        childHeightMode = 1073741824;
                    }
                    int childWidth = ((getViewportWidth() - horizontalPadding) - this.mInsets.left) - this.mInsets.right;
                    int childHeight = ((getViewportHeight() - verticalPadding) - this.mInsets.top) - this.mInsets.bottom;
                    this.mNormalChildHeight = childHeight;
                    if (referenceChildWidth == 0) {
                        referenceChildWidth = childWidth;
                    }
                    child.measure(MeasureSpec.makeMeasureSpec(childWidth, childWidthMode), MeasureSpec.makeMeasureSpec(childHeight, childHeightMode));
                }
            }
            setMeasuredDimension(scaledWidthSize, scaledHeightSize);
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (getChildCount() != 0) {
            int endIndex;
            int childCount = getChildCount();
            int offsetX = getViewportOffsetX();
            int offsetY = getViewportOffsetY();
            this.mViewport.offset(offsetX, offsetY);
            int startIndex = Utilities.sIsRtl ? childCount - 1 : 0;
            if (Utilities.sIsRtl) {
                endIndex = -1;
            } else {
                endIndex = childCount;
            }
            int delta = Utilities.sIsRtl ? -1 : 1;
            int verticalPadding = getPaddingTop() + getPaddingBottom();
            int childLeft = offsetX + getPaddingLeft();
            if (this.mPageScrolls == null || childCount != this.mChildCountOnLastLayout) {
                this.mPageScrolls = new int[childCount];
            }
            for (int i = startIndex; i != endIndex; i += delta) {
                View child = getPageAt(i);
                if (child.getVisibility() != View.GONE) {
                    int childTop = (getPaddingTop() + offsetY) + this.mInsets.top;
                    if (this.mCenterPagesVertically) {
                        childTop += ((((getViewportHeight() - this.mInsets.top) - this.mInsets.bottom) - verticalPadding) - child.getMeasuredHeight()) / 2;
                    }
                    int childWidth = child.getMeasuredWidth();
                    child.layout(childLeft, childTop, child.getMeasuredWidth() + childLeft, childTop + child.getMeasuredHeight());
                    this.mPageScrolls[i] = (childLeft - getPaddingLeft()) - offsetX;
                    childLeft += (childWidth + this.mPageSpacing) + getChildGap();
                }
            }
            LayoutTransition transition = getLayoutTransition();
            if (transition == null || !transition.isRunning()) {
                updateMaxScrollX();
            } else {
                transition.addTransitionListener(this.mTransitionListener);
            }
            if (!isPageMoving() && this.mCurrentPage >= 0 && this.mCurrentPage < childCount && getScrollX() != getScrollForPage(this.mCurrentPage)) {
                updateCurrentPageScroll();
            }
            if (this.mScroller.isFinished() && this.mChildCountOnLastLayout != childCount) {
                if (this.mRestorePage != -1001) {
                    setCurrentPage(this.mRestorePage);
                    this.mRestorePage = -1001;
                } else {
                    setCurrentPage(getNextPage());
                }
            }
            this.mChildCountOnLastLayout = childCount;
            if (isReordering(true)) {
                updateDragViewTranslationDuringDrag();
            }
        }
    }

    protected int getChildGap() {
        return 0;
    }

    private void updateMaxScrollX() {
        int index = 0;
        int childCount = getChildCount();
        if (childCount > 0) {
            if (!Utilities.sIsRtl) {
                index = childCount - 1;
            }
            this.mMaxScrollX = getScrollForPage(index);
            return;
        }
        this.mMaxScrollX = 0;
    }

    public int getMaxScrollX() {
        return this.mMaxScrollX;
    }

    public void setPageSpacing(int pageSpacing) {
        this.mPageSpacing = pageSpacing;
        requestLayout();
    }

    protected void screenScrolled(int screenCenter, int leftScreen, int rightScreen) {
    }

    public void onChildViewAdded(View parent, View child) {
        if (!(this.mPageIndicator == null || isReordering(false))) {
            int pageIndex = indexOfChild(child);
            this.mPageIndicator.addMarker(pageIndex, getPageIndicatorMarker(pageIndex), this.mEnablePageIndicatorAnim, this);
        }
        this.mForceScreenScrolled = true;
        invalidate();
    }

    public void onChildViewRemoved(View parent, View child) {
        this.mForceScreenScrolled = true;
        invalidate();
    }

    public void addMarkerForView(int index) {
        if (this.mPageIndicator != null && !isReordering(false)) {
            this.mPageIndicator.addMarker(index, getPageIndicatorMarker(index), this.mEnablePageIndicatorAnim, this);
        }
    }

    public void removeMarkerForView(int index) {
        if (this.mPageIndicator != null && !isReordering(false)) {
            this.mPageIndicator.removeMarker(index, this.mEnablePageIndicatorAnim);
        }
    }

    public void setMarkerStartOffset(int offset) {
        if (this.mPageIndicator != null) {
            this.mPageIndicator.setMarkerStartOffset(offset);
        }
    }

    public void setActiveMarker(int index) {
        if (this.mPageIndicator != null && !isPageMoving()) {
            this.mPageIndicator.setActiveMarker(index);
        }
    }

    public void updateMarker(int index, PageMarkerResources marker) {
        if (this.mPageIndicator != null) {
            this.mPageIndicator.updateMarker(index, marker);
        }
    }

    public void setZeroPageMarker(boolean existZeroPage) {
        if (this.mPageIndicator != null) {
            this.mPageIndicator.setZeroPageMarker(existZeroPage);
        }
    }

    private void updatePageIndicator() {
        if (this.mPageIndicator != null) {
            this.mPageIndicator.setContentDescription(getPageIndicatorDescription());
            if (!isReordering(false)) {
                this.mPageIndicator.setActiveMarker(getNextPage());
            }
        }
    }

    public void removeView(View v) {
        removeMarkerForView(indexOfChild(v));
        super.removeView(v);
    }

    public void removeViewInLayout(View v) {
        removeMarkerForView(indexOfChild(v));
        super.removeViewInLayout(v);
    }

    public void removeViewAt(int index) {
        removeMarkerForView(index);
        super.removeViewAt(index);
    }

    public void removeAllViewsInLayout() {
        if (this.mPageIndicator != null) {
            this.mPageIndicator.removeAllMarkers();
        }
        super.removeAllViewsInLayout();
    }

    protected int getChildOffset(int index) {
        if (index < 0 || index > getChildCount() - 1) {
            return 0;
        }
        return getPageAt(index).getLeft() - getViewportOffsetX();
    }

    protected void getVisiblePages(int[] range) {
        int pageCount = getChildCount();
        int[] iArr = sTmpIntPoint;
        sTmpIntPoint[1] = 0;
        iArr[0] = 0;
        range[0] = -1;
        range[1] = -1;
        if (pageCount > 0) {
            int viewportWidth = getViewportWidth();
            int curScreen = 0;
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                View currPage = getPageAt(i);
                sTmpIntPoint[0] = 0;
                Utilities.getDescendantCoordRelativeToParent(getContext(), currPage, this, sTmpIntPoint, false);
                if (sTmpIntPoint[0] > viewportWidth) {
                    if (range[0] != -1) {
                        break;
                    }
                } else {
                    sTmpIntPoint[0] = currPage.getMeasuredWidth();
                    Utilities.getDescendantCoordRelativeToParent(getContext(), currPage, this, sTmpIntPoint, false);
                    if (sTmpIntPoint[0] >= 0) {
                        curScreen = i;
                        if (range[0] < 0) {
                            range[0] = curScreen;
                        }
                    } else if (range[0] != -1) {
                        break;
                    }
                }
            }
            range[1] = curScreen;
            return;
        }
        range[0] = -1;
        range[1] = -1;
    }

    protected boolean shouldDrawChild(View child) {
        return child.getVisibility() == View.VISIBLE;
    }

    protected void dispatchDraw(Canvas canvas) {
        int pageCount = getChildCount();
        if (pageCount > 0) {
            int screenCenter = getScrollX() + (getViewportWidth() / 2);
            getVisiblePages(this.mTempVisiblePagesRange);
            int leftScreen = this.mTempVisiblePagesRange[0];
            int rightScreen = this.mTempVisiblePagesRange[1];
            if (this.mIsShowingHintPages) {
                int currentPage = getHintCenterPage();
                leftScreen = Math.max(currentPage - 1, 0);
                rightScreen = Math.min(currentPage + 1, getChildCount() - 1);
            } else if (leftScreen == rightScreen) {
                if (rightScreen < pageCount - 1) {
                    rightScreen++;
                }
                if (leftScreen > 0) {
                    leftScreen--;
                }
            }
            if (leftScreen < 0) {
                leftScreen = 0;
            }
            if (screenCenter != this.mLastScreenCenter || this.mForceScreenScrolled) {
                this.mForceScreenScrolled = false;
                screenScrolled(screenCenter, leftScreen, rightScreen);
                this.mLastScreenCenter = screenCenter;
            }
            if (this.mScroller.isUpdatedScreenIndex(leftScreen, rightScreen)) {
                new DvfsUtil(getContext()).boostOneFrame();
            }
            if (leftScreen != -1 && rightScreen != -1) {
                long drawingTime = getDrawingTime();
                canvas.save();
                canvas.clipRect(getScrollX(), getScrollY(), (getScrollX() + getRight()) - getLeft(), (getScrollY() + getBottom()) - getTop());
                int i = pageCount - 1;
                while (i >= 0) {
                    View v = getPageAt(i);
                    if (v != this.mDragView) {
                        if (this.mForceDrawAllChildrenNextFrame || (leftScreen <= i && i <= rightScreen && shouldDrawChild(v))) {
                            if (this.mIsPageMoving) {
                                updatePageTransform(v, i, screenCenter);
                            } else if (!this.mIsShowingHintPages) {
                                resetTransitionEffect(v);
                            }
                            drawChild(canvas, v, drawingTime);
                        } else {
                            resetTransitionEffectForInvisiblePage(v);
                        }
                    }
                    i--;
                }
                if (this.mDragView != null) {
                    drawChild(canvas, this.mDragView, drawingTime);
                }
                this.mForceDrawAllChildrenNextFrame = false;
                canvas.restore();
            }
        }
    }

    private float maxOverScroll() {
        return OVERSCROLL_DAMP_FACTOR * ((1.0f / Math.abs(1.0f)) * overScrollInfluenceCurve(Math.abs(1.0f)));
    }

    private float overScrollInfluenceCurve(float f) {
        f -= 1.0f;
        return ((f * f) * f) + 1.0f;
    }

    private void dampedOverScroll(float amount) {
        int screenSize = getMeasuredWidth();
        float f = amount / ((float) screenSize);
        if (f != 0.0f) {
            f = (f / Math.abs(f)) * overScrollInfluenceCurve(Math.abs(f));
            if (Math.abs(f) >= 1.0f) {
                f /= Math.abs(f);
            }
            int overScrollAmount = Math.round((OVERSCROLL_DAMP_FACTOR * f) * ((float) screenSize));
            if (amount < 0.0f) {
                super.scrollTo(overScrollAmount, getScrollY());
            } else {
                super.scrollTo(this.mMaxScrollX + overScrollAmount, getScrollY());
            }
            invalidate();
        }
    }

    protected boolean canOverScroll() {
        return true;
    }

    protected void overScroll(int amount) {
        dampedOverScroll((float) amount);
    }

    protected void updatePageTransform(View page, int index, int screenCenter) {
        int overScrollLeftIndex;
        resetTransitionEffect(page);
        float scrollProgress = getScrollProgress(screenCenter, page, index);
        int pageCount = getChildCount();
        if (Utilities.sIsRtl) {
            overScrollLeftIndex = pageCount - 1;
        } else {
            overScrollLeftIndex = 0;
        }
        int overScrollRightIndex = Utilities.sIsRtl ? 0 : pageCount - 1;
        if ((index == overScrollLeftIndex && scrollProgress < 0.0f) || (index == overScrollRightIndex && scrollProgress > 0.0f)) {
            new DvfsUtil(getContext()).boostOneFrame();
            overscrollEffect(page, scrollProgress, pageCount);
        }
    }

    protected void resetTransitionEffect(View page) {
        page.setPivotX(((float) page.getWidth()) / 2.0f);
        page.setPivotY(((float) page.getHeight()) / 2.0f);
        page.setTranslationX(0.0f);
        page.setTranslationY(0.0f);
        page.setScaleX(1.0f);
        page.setScaleY(1.0f);
        page.setAlpha(1.0f);
    }

    protected void resetTransitionEffectForInvisiblePage(View page) {
    }

    private void overscrollEffect(View page, float scrollProgress, int pageCount) {
        float translationX;
        float scaleFactor = Math.max(TRANSITION_BOUNCE_MIN_SCALE, 1.0f - ((0.1f * ViInterpolator.getInterploator(33).getInterpolation(Math.abs(scrollProgress / 3.0f))) / maxOverScroll()));
        if (indexToPage(indexOfChild(page)) != (Utilities.sIsRtl ? pageCount - 1 : 0) || scrollProgress >= 0.0f) {
            translationX = (float) (getScrollX() - this.mMaxScrollX);
        } else {
            translationX = (float) getScrollX();
        }
        page.setPivotX(((float) page.getMeasuredWidth()) / 2.0f);
        page.setPivotY(((float) page.getMeasuredHeight()) / 2.0f);
        page.setScaleX(scaleFactor);
        page.setScaleY(scaleFactor);
        page.setTranslationX(translationX);
        page.setAlpha(Math.max(Math.min(1.0f, 1.0f - Math.abs(scrollProgress / 2.0f)), TRANSITION_BOUNCE_MIN_ALPHA));
    }

    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        int page = indexToPage(indexOfChild(child));
        if (page == this.mCurrentPage && this.mScroller.isFinished()) {
            return false;
        }
        snapToPage(page);
        return true;
    }

    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        int focusablePage;
        if (this.mNextPage != -1) {
            focusablePage = this.mNextPage;
        } else {
            focusablePage = this.mCurrentPage;
        }
        View v = getPageAt(focusablePage);
        if (v != null) {
            return v.requestFocus(direction, previouslyFocusedRect);
        }
        return false;
    }

    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (super.dispatchUnhandledMove(focused, direction)) {
            return true;
        }
        if (Utilities.sIsRtl) {
            if (direction == 17) {
                direction = 66;
            } else if (direction == 66) {
                direction = 17;
            }
        }
        if (direction == 17) {
            if (getCurrentPage() >= 0) {
                snapToPage(getCurrentPage() - 1);
                return true;
            }
        } else if (direction == 66 && getCurrentPage() < getPageCount() - 1) {
            snapToPage(getCurrentPage() + 1);
            return true;
        }
        return false;
    }

    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        View focusablePage = null;
        if (this.mCurrentPage >= 0 && this.mCurrentPage < getPageCount()) {
            focusablePage = getPageAt(this.mCurrentPage);
        }
        if (direction == 17) {
            if (this.mCurrentPage > 0) {
                focusablePage = getPageAt(this.mCurrentPage - 1);
            }
        } else if (direction == 66 && this.mCurrentPage < getPageCount() - 1) {
            focusablePage = getPageAt(this.mCurrentPage + 1);
        }
        if (focusablePage != null) {
            focusablePage.addFocusables(views, direction, focusableMode);
        }
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }

    public void focusableViewAvailable(View focused) {
        View current = getPageAt(this.mCurrentPage);
        View v = focused;
        while (v != current) {
            if (v != this && (v.getParent() instanceof View)) {
                v = (View) v.getParent();
            } else {
                return;
            }
        }
        super.focusableViewAvailable(focused);
    }

    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            getPageAt(this.mCurrentPage).cancelLongPress();
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    private boolean isTouchPointInViewportWithBuffer(int x, int y) {
        sTmpRect.set(this.mViewport.left - (this.mViewport.width() / 2), this.mViewport.top, this.mViewport.right + (this.mViewport.width() / 2), this.mViewport.bottom);
        return sTmpRect.contains(x, y);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        acquireVelocityTrackerAndAddMovement(ev);
        if (getChildCount() <= 0) {
            return super.onInterceptTouchEvent(ev);
        }
        int action = ev.getAction();
        if (action == 2 && this.mTouchState == 1) {
            return true;
        }
        switch (action & 255) {
            case 0:
                boolean finishedScrolling;
                float x = ev.getX();
                float y = ev.getY();
                this.mDownMotionX = x;
                this.mDownMotionY = y;
                this.mDownScrollX = (float) getScrollX();
                this.mLastMotionX = x;
                this.mLastMotionY = y;
                float[] p = mapPointFromViewToParent(this, x, y);
                this.mParentDownMotionX = p[0];
                this.mParentDownMotionY = p[1];
                this.mTotalMotionX = 0.0f;
                this.mActivePointerId = ev.getPointerId(0);
                int xDist = Math.abs(this.mScroller.getFinalX() - this.mScroller.getCurrX());
                if (this.mScroller.isFinished() || xDist < this.mTouchSlop / 3) {
                    finishedScrolling = true;
                } else {
                    finishedScrolling = false;
                }
                if (!finishedScrolling) {
                    if (!isTouchPointInViewportWithBuffer((int) this.mDownMotionX, (int) this.mDownMotionY)) {
                        Log.d(TAG, "reset touch state case 2");
                        this.mTouchState = 0;
                        break;
                    }
                    this.mTouchState = 1;
                    break;
                }
                Log.d(TAG, "reset touch state case 1");
                this.mTouchState = 0;
                if (!this.mScroller.isFinished()) {
                    setCurrentPage(getNextPage());
                    pageEndMoving();
                    break;
                }
                break;
            case 1:
            case 3:
                resetTouchState();
                break;
            case 2:
                Trace.beginSection("PagedView.onInterceptTouchEvent : " + this.mActivePointerId);
                Trace.endSection();
                if (this.mActivePointerId != -1) {
                    determineScrollingStart(ev);
                    break;
                }
                break;
            case 6:
                onSecondaryPointerUp(ev);
                releaseVelocityTracker();
                break;
        }
        if (this.mTouchState == 0) {
            return false;
        }
        return true;
    }

    protected boolean determineScrollingStart(MotionEvent ev) {
        return determineScrollingStart(ev, 1.0f);
    }

    protected boolean determineScrollingStart(MotionEvent ev, float touchSlopScale) {
        int pointerIndex = ev.findPointerIndex(this.mActivePointerId);
        if (pointerIndex == -1) {
            return false;
        }
        float x = ev.getX(pointerIndex);
        if (!isTouchPointInViewportWithBuffer((int) x, (int) ev.getY(pointerIndex))) {
            return false;
        }
        boolean xMoved;
        if (((int) Math.abs(x - this.mDownMotionX)) > Math.round(((float) this.mTouchSlop) * touchSlopScale)) {
            xMoved = true;
        } else {
            xMoved = false;
        }
        if (xMoved && isHorizontalScroll()) {
            Log.d(TAG, "Determined start scrolling - TOUCH_STATE_SCROLLING");
            this.mTouchState = 1;
            this.mTotalMotionX += Math.abs(this.mLastMotionX - x);
            onScrollInteractionBegin();
            pageBeginMoving();
            scrollPageOnMoveEvent(ev);
            this.mLastMotionX = x;
            return true;
        } else if (this.mScrollDeterminator == null) {
            return false;
        } else {
            this.mScrollDeterminator.setSlopCompensation();
            return false;
        }
    }

    protected void cancelCurrentPageLongPress() {
        View currentPage = getPageAt(this.mCurrentPage);
        if (currentPage != null) {
            currentPage.cancelLongPress();
        }
    }

    protected float getScrollProgress(int screenCenter, View v, int page) {
        int totalDistance;
        int delta = screenCenter - (getScrollForPage(page) + (getViewportWidth() / 2));
        int count = getChildCount();
        int adjacentPage = page + 1;
        if ((delta < 0 && !Utilities.sIsRtl) || (delta > 0 && Utilities.sIsRtl)) {
            adjacentPage = page - 1;
        }
        if (adjacentPage < 0 || adjacentPage > count - 1) {
            totalDistance = v.getMeasuredWidth() + this.mPageSpacing;
        } else {
            totalDistance = Math.abs(getScrollForPage(adjacentPage) - getScrollForPage(page));
        }
        return Math.max(Math.min(((float) delta) / (((float) totalDistance) * 1.0f), 1.0f), -1.0f);
    }

    public int getScrollForPage(int index) {
        if (this.mPageScrolls == null || index >= this.mPageScrolls.length || index < 0) {
            return 0;
        }
        return this.mPageScrolls[index];
    }

    public int getLayoutTransitionOffsetForPage(int index) {
        if (this.mPageScrolls == null || index >= this.mPageScrolls.length || index < 0) {
            return 0;
        }
        return (int) (getChildAt(index).getX() - ((float) ((this.mPageScrolls[index] + (Utilities.sIsRtl ? getPaddingRight() : getPaddingLeft())) + getViewportOffsetX())));
    }

    protected int getNearestHoverOverPageIndex() {
        int i = 1;
        int i2 = -1;
        if (this.mDragView == null) {
            return -1;
        }
        int dragX = (int) (this.mDragView.getTranslationX() + mapPointFromViewToParent(this, this.mDownMotionX, this.mDownMotionY)[0]);
        int currentIndex = indexOfChild(this.mDragView);
        if (dragX < 0) {
            if (!Utilities.sIsRtl) {
                i = -1;
            }
            return currentIndex + i;
        } else if (dragX <= this.mDragView.getMeasuredWidth()) {
            return currentIndex;
        } else {
            if (!Utilities.sIsRtl) {
                i2 = 1;
            }
            return currentIndex + i2;
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        if (getChildCount() <= 0) {
            return super.onTouchEvent(ev);
        }
        acquireVelocityTrackerAndAddMovement(ev);
        float[] pt;
        switch (ev.getAction() & 255) {
            case 0:
                if (!this.mScroller.isFinished()) {
                    abortScrollerAnimation(false);
                }
                float x = ev.getX();
                this.mLastMotionX = x;
                this.mDownMotionX = x;
                x = ev.getY();
                this.mLastMotionY = x;
                this.mDownMotionY = x;
                this.mDownScrollX = (float) getScrollX();
                float[] p = mapPointFromViewToParent(this, this.mLastMotionX, this.mLastMotionY);
                this.mParentDownMotionX = p[0];
                this.mParentDownMotionY = p[1];
                this.mTotalMotionX = 0.0f;
                this.mActivePointerId = ev.getPointerId(0);
                if (this.mTouchState == 1) {
                    onScrollInteractionBegin();
                    pageBeginMoving();
                    break;
                }
                break;
            case 1:
                if (this.mTouchState == 1) {
                    int activePointerId = this.mActivePointerId;
                    int pointerIndex = ev.findPointerIndex(activePointerId);
                    if (pointerIndex == -1) {
                        return true;
                    }
                    float x2 = ev.getX(pointerIndex);
                    VelocityTracker velocityTracker = this.mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, (float) this.mMaximumVelocity);
                    int velocityX = (int) velocityTracker.getXVelocity(activePointerId);
                    int deltaX = (int) (x2 - this.mDownMotionX);
                    int pageWidth = LauncherAppState.getInstance().getDeviceProfile().availableWidthPx;
                    if (getPageAt(this.mCurrentPage) != null) {
                        pageWidth = getPageAt(this.mCurrentPage).getMeasuredWidth();
                    }
                    boolean isSignificantMove = ((float) Math.abs(deltaX)) > ((float) pageWidth) * SIGNIFICANT_MOVE_THRESHOLD;
                    this.mTotalMotionX += Math.abs(this.mLastMotionX - x2);
                    boolean isFling = this.mTotalMotionX > 25.0f && Math.abs(velocityX) > this.mFlingThresholdVelocity;
                    boolean returnToOriginalPage = false;
                    if (((float) Math.abs(deltaX)) > ((float) pageWidth) * RETURN_TO_ORIGINAL_PAGE_THRESHOLD && Math.signum((float) velocityX) != Math.signum((float) deltaX) && isFling) {
                        returnToOriginalPage = true;
                    }
                    boolean isDeltaXLeft = Utilities.sIsRtl ? deltaX > 0 : deltaX < 0;
                    boolean isVelocityXLeft = Utilities.sIsRtl ? velocityX > 0 : velocityX < 0;
                    if (((isSignificantMove && !isDeltaXLeft && !isFling) || (isFling && !isVelocityXLeft)) && this.mCurrentPage > 0) {
                        snapToPageWithVelocity(returnToOriginalPage ? this.mCurrentPage : this.mCurrentPage - 1, velocityX);
                        snapToPageSALogging(false);
                    } else if (!((isSignificantMove && isDeltaXLeft && !isFling) || (isFling && isVelocityXLeft)) || this.mCurrentPage >= getChildCount() - 1) {
                        snapToDestination();
                    } else {
                        snapToPageWithVelocity(returnToOriginalPage ? this.mCurrentPage : this.mCurrentPage + 1, velocityX);
                        snapToPageSALogging(false);
                    }
                    onScrollInteractionEnd();
                } else if (this.mTouchState == 2) {
                    this.mLastMotionX = ev.getX();
                    this.mLastMotionY = ev.getY();
                    pt = mapPointFromViewToParent(this, this.mLastMotionX, this.mLastMotionY);
                    this.mParentDownMotionX = pt[0];
                    this.mParentDownMotionY = pt[1];
                    updateDragViewTranslationDuringDrag();
                } else if (!this.mCancelTap) {
                    if (checkTouchedPageIndicator(ev.getRawX(), ev.getRawY())) {
                        this.mPageIndicator.clickPageIndicator(ev);
                    } else {
                        onUnhandledTap(ev);
                    }
                }
                removeCallbacks(this.mSidePageHoverRunnable);
                resetTouchState();
                break;
            case 2:
                if (this.mTouchState != 1 || !isHorizontalScroll()) {
                    if (this.mTouchState != 2) {
                        determineScrollingStart(ev);
                        break;
                    }
                    this.mLastMotionX = ev.getX();
                    this.mLastMotionY = ev.getY();
                    pt = mapPointFromViewToParent(this, this.mLastMotionX, this.mLastMotionY);
                    this.mParentDownMotionX = pt[0];
                    this.mParentDownMotionY = pt[1];
                    updateDragViewTranslationDuringDrag();
                    final int dragViewIndex = indexOfChild(this.mDragView);
                    final int pageUnderPointIndex = getNearestHoverOverPageIndex();
                    if (pageUnderPointIndex > -1) {
                        if (pageUnderPointIndex != indexOfChild(this.mDragView)) {
                            this.mTempVisiblePagesRange[0] = 0;
                            this.mTempVisiblePagesRange[1] = getPageCount() - 1;
                            if (this.mTempVisiblePagesRange[0] <= pageUnderPointIndex && pageUnderPointIndex <= this.mTempVisiblePagesRange[1] && pageUnderPointIndex != this.mSidePageHoverIndex && this.mScroller.isFinished()) {
                                this.mSidePageHoverIndex = pageUnderPointIndex;
                                this.mSidePageHoverRunnable = new Runnable() {
                                    public void run() {
                                        if (PagedView.this.mTouchState == 2) {
                                            int shiftDelta;
                                            PagedView.this.snapToPage(pageUnderPointIndex);
                                            if (dragViewIndex < pageUnderPointIndex) {
                                                shiftDelta = -1;
                                            } else {
                                                shiftDelta = 1;
                                            }
                                            int lowerIndex = dragViewIndex < pageUnderPointIndex ? dragViewIndex + 1 : pageUnderPointIndex;
                                            int upperIndex = dragViewIndex > pageUnderPointIndex ? dragViewIndex - 1 : pageUnderPointIndex;
                                            for (int i = lowerIndex; i <= upperIndex; i++) {
                                                View v = PagedView.this.getChildAt(i);
                                                int oldX = PagedView.this.getViewportOffsetX() + PagedView.this.getChildOffset(i);
                                                int newX = PagedView.this.getViewportOffsetX() + PagedView.this.getChildOffset(i + shiftDelta);
                                                AnimatorSet anim = (AnimatorSet) v.getTag(100);
                                                if (anim != null) {
                                                    anim.cancel();
                                                }
                                                v.setTranslationX((float) (oldX - newX));
                                                anim = new AnimatorSet();
                                                anim.setDuration(300);
                                                Animator[] animatorArr = new Animator[1];
                                                animatorArr[0] = ObjectAnimator.ofFloat(v, "translationX", new float[]{0.0f});
                                                anim.playTogether(animatorArr);
                                                anim.start();
                                                v.setTag(anim);
                                            }
                                            PagedView.this.removeView(PagedView.this.mDragView);
                                            PagedView.this.addView(PagedView.this.mDragView, pageUnderPointIndex);
                                            PagedView.this.mSidePageHoverIndex = -1;
                                            if (PagedView.this.mPageIndicator != null) {
                                                PagedView.this.mPageIndicator.setActiveMarker(PagedView.this.getNextPage());
                                            }
                                            SALogging.getInstance().insertEventLog(PagedView.this.getResources().getString(R.string.screen_HomeOption), PagedView.this.getResources().getString(R.string.event_Movetopage));
                                        }
                                    }
                                };
                                postDelayed(this.mSidePageHoverRunnable, 80);
                                break;
                            }
                        }
                    }
                    removeCallbacks(this.mSidePageHoverRunnable);
                    this.mSidePageHoverIndex = -1;
                    break;
                }
                return scrollPageOnMoveEvent(ev);
            case 3:
                if (this.mTouchState == 1) {
                    snapToDestination();
                    onScrollInteractionEnd();
                }
                resetTouchState();
                break;
            case 6:
                onSecondaryPointerUp(ev);
                releaseVelocityTracker();
                break;
        }
        return true;
    }

    private boolean scrollPageOnMoveEvent(MotionEvent ev) {
        int pointerIndex = ev.findPointerIndex(this.mActivePointerId);
        if (pointerIndex != -1) {
            int x = (int) ev.getX(pointerIndex);
            int deltaX = x - ((int) this.mLastMotionX);
            this.mTotalMotionX += (float) Math.abs(deltaX);
            if (((float) Math.abs(deltaX)) >= 1.0f) {
                int deltaXwithCompensation = deltaX;
                if (this.mScrollDeterminator != null) {
                    deltaXwithCompensation = this.mScrollDeterminator.getDeltaXwithCompensation(deltaX);
                }
                scrollBy(-deltaXwithCompensation, 0);
                this.mLastMotionX = (float) x;
            } else {
                awakenScrollBars();
            }
        }
        return true;
    }

    private void resetTouchState() {
        Log.d(TAG, "resetTouchState()");
        releaseVelocityTracker();
        endReordering();
        this.mCancelTap = false;
        this.mTouchState = 0;
        this.mActivePointerId = -1;
    }

    protected void onScrollInteractionBegin() {
    }

    protected void onScrollInteractionEnd() {
    }

    protected void onUnhandledTap(MotionEvent ev) {
        ((Launcher) getContext()).onClick(this);
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        boolean isForwardScroll = false;
        if ((event.getSource() & 2) != 0) {
            switch (event.getAction()) {
                case 8:
                    float hscroll;
                    float vscroll;
                    if ((event.getMetaState() & 1) != 0) {
                        vscroll = 0.0f;
                        hscroll = event.getAxisValue(9);
                    } else {
                        vscroll = -event.getAxisValue(9);
                        hscroll = event.getAxisValue(10);
                    }
                    if (!(hscroll == 0.0f && vscroll == 0.0f)) {
                        if (Utilities.sIsRtl) {
                            if (hscroll < 0.0f || vscroll < 0.0f) {
                                isForwardScroll = true;
                            }
                        } else if (hscroll > 0.0f || vscroll > 0.0f) {
                            isForwardScroll = true;
                        }
                        if (isForwardScroll) {
                            scrollRight();
                            return true;
                        }
                        scrollLeft();
                        return true;
                    }
            }
        }
        return super.onGenericMotionEvent(event);
    }

    private void acquireVelocityTrackerAndAddMovement(MotionEvent ev) {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(ev);
    }

    private void releaseVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.clear();
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        int pointerIndex = (ev.getAction() & MotionEventCompat.ACTION_POINTER_INDEX_MASK) >> 8;
        if (ev.getPointerId(pointerIndex) == this.mActivePointerId) {
            int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            float x = ev.getX(newPointerIndex);
            this.mDownMotionX = x;
            this.mLastMotionX = x;
            this.mLastMotionY = ev.getY(newPointerIndex);
            this.mActivePointerId = ev.getPointerId(newPointerIndex);
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.clear();
            }
        }
    }

    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        int page = indexToPage(indexOfChild(child));
        if (page >= 0 && page != getCurrentPage() && !isInTouchMode()) {
            snapToPage(page);
        }
    }

    public int getPageNearestToCenterOfScreen() {
        int minDistanceFromScreenCenter = Integer.MAX_VALUE;
        int minDistanceFromScreenCenterIndex = -1;
        int screenCenter = (getViewportOffsetX() + getScrollX()) + (getViewportWidth() / 2);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            int distanceFromScreenCenter = Math.abs(((getViewportOffsetX() + getChildOffset(i)) + (getPageAt(i).getMeasuredWidth() / 2)) - screenCenter);
            if (distanceFromScreenCenter < minDistanceFromScreenCenter) {
                minDistanceFromScreenCenter = distanceFromScreenCenter;
                minDistanceFromScreenCenterIndex = i;
            }
        }
        return minDistanceFromScreenCenterIndex;
    }

    protected void snapToDestination() {
        snapToPage(getPageNearestToCenterOfScreen(), PAGE_SNAP_ANIMATION_DURATION);
    }

    private float distanceInfluenceForSnapDuration(float f) {
        return (float) Math.sin((double) ((float) (((double) (f - 0.5f)) * 0.4712389167638204d)));
    }

    protected void snapToPageWithVelocity(int whichPage, int velocity) {
        whichPage = validateNewPage(whichPage);
        int halfScreenSize = getViewportWidth() / 2;
        int delta = getScrollForPage(whichPage) - getScrollX();
        if (Math.abs(velocity) < this.mMinFlingVelocity) {
            snapToPage(whichPage, PAGE_SNAP_ANIMATION_DURATION);
            return;
        }
        snapToPage(whichPage, delta, Math.round(1000.0f * Math.abs((((float) halfScreenSize) + (((float) halfScreenSize) * distanceInfluenceForSnapDuration(Math.min(1.0f, (((float) Math.abs(delta)) * 1.0f) / ((float) (halfScreenSize * 2)))))) / ((float) Math.max(this.mMinSnapVelocity, Math.abs(velocity))))) * 4);
    }

    public void snapToPage(int whichPage) {
        snapToPage(whichPage, PAGE_SNAP_ANIMATION_DURATION);
    }

    public void snapToPageImmediately(int whichPage) {
        snapToPage(whichPage, PAGE_SNAP_ANIMATION_DURATION, true, null);
    }

    public void snapToPage(int whichPage, int duration) {
        snapToPage(whichPage, duration, false, null);
    }

    public void snapToPage(int whichPage, int duration, TimeInterpolator interpolator) {
        snapToPage(whichPage, duration, false, interpolator);
    }

    protected void snapToPage(int whichPage, int duration, boolean immediate, TimeInterpolator interpolator) {
        whichPage = validateNewPage(whichPage);
        snapToPage(whichPage, getScrollForPage(whichPage) - getScrollX(), duration, immediate, interpolator);
    }

    protected void snapToPage(int whichPage, int delta, int duration) {
        snapToPage(whichPage, delta, duration, false, null);
    }

    public void snapToPageSALogging(boolean isPageIndicator) {
    }

    protected void snapToPage(int whichPage, int delta, int duration, boolean immediate, TimeInterpolator interpolator) {
        whichPage = validateNewPage(whichPage);
        this.mNextPage = whichPage;
        Log.d(TAG, "Snap To Next Page [" + this.mNextPage + "]");
        View focusedChild = getFocusedChild();
        if (!(focusedChild == null || whichPage == this.mCurrentPage || focusedChild != getPageAt(this.mCurrentPage))) {
            focusedChild.clearFocus();
        }
        pageBeginMoving();
        awakenScrollBars(duration);
        if (immediate) {
            duration = 0;
        } else if (duration == 0) {
            duration = Math.abs(delta);
        }
        if (!this.mScroller.isFinished()) {
            abortScrollerAnimation(false);
        }
        if (interpolator != null) {
            this.mScroller.setInterpolator(interpolator);
        } else {
            this.mScroller.setInterpolator(this.mDefaultInterpolator);
        }
        if (this.mIsShowingHintPages) {
            if (this.mPullingPagesAnim != null && this.mPullingPagesAnim.isRunning()) {
                this.mPullingPagesAnim.cancel();
            }
            this.mScrollState = -1;
            resetPulledPages();
            setHintPageTranslation();
        }
        this.mScroller.startScroll(getScrollX(), 0, delta, 0, duration);
        updatePageIndicator();
        if (immediate) {
            computeScroll();
            pageEndMoving();
        }
        this.mDeferLoadAssociatedPagesUntilScrollCompletes = true;
        this.mForceScreenScrolled = true;
        invalidate();
    }

    protected boolean isScrollableToZeroPage() {
        return false;
    }

    public void scrollLeft() {
        int page = getNextPage();
        if (page > 0 || (page == 0 && isScrollableToZeroPage())) {
            if (isScrolling()) {
                cancelDeferLoadAssociatedPagesUntilScrollCompletes();
                setCurrentPage(page);
            }
            loadAssociatedPages(page - 1);
            snapToPage(page - 1);
        }
    }

    public void scrollRight() {
        int page = getNextPage();
        if (page < getChildCount() - 1) {
            if (isScrolling()) {
                cancelDeferLoadAssociatedPagesUntilScrollCompletes();
                setCurrentPage(page);
            }
            loadAssociatedPages(page + 1);
            snapToPage(page + 1);
        }
    }

    public int getPageForView(View v) {
        if (v != null) {
            ViewParent vp = v.getParent();
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                if (vp == getPageAt(i)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean performLongClick() {
        this.mCancelTap = true;
        return super.performLongClick();
    }

    private void animateDragViewToOriginalPosition() {
        if (this.mDragView != null) {
            AnimatorSet anim = new AnimatorSet();
            anim.setDuration(200);
            Animator[] animatorArr = new Animator[4];
            animatorArr[0] = ObjectAnimator.ofFloat(this.mDragView, "translationX", new float[]{0.0f});
            animatorArr[1] = ObjectAnimator.ofFloat(this.mDragView, "translationY", new float[]{0.0f});
            animatorArr[2] = ObjectAnimator.ofFloat(this.mDragView, "scaleX", new float[]{1.0f});
            animatorArr[3] = ObjectAnimator.ofFloat(this.mDragView, "scaleY", new float[]{1.0f});
            anim.playTogether(animatorArr);
            anim.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    PagedView.this.onPostReorderingAnimationCompleted(true);
                }
            });
            anim.start();
        }
    }

    public void onStartReordering() {
        if (this.mPostReorderingPreZoomInRemainingAnimationCount > 0) {
            this.mPostReorderingPreZoomInRemainingAnimationCount = 0;
        }
        this.mTouchState = 2;
        this.mIsReordering = true;
        invalidate();
    }

    private void onPostReorderingAnimationCompleted(boolean animationCompleted) {
        this.mPostReorderingPreZoomInRemainingAnimationCount--;
        if (this.mPostReorderingPreZoomInRunnable == null) {
            return;
        }
        if (this.mPostReorderingPreZoomInRemainingAnimationCount == 0 || animationCompleted) {
            this.mPostReorderingPreZoomInRunnable.run();
            this.mPostReorderingPreZoomInRunnable = null;
        }
    }

    public void onEndReordering() {
        this.mIsReordering = false;
        this.mDragView = null;
    }

    public boolean startReordering(View v) {
        int dragViewIndex = indexOfChild(v);
        if (this.mTouchState != 0 || dragViewIndex == -1) {
            return false;
        }
        this.mTempVisiblePagesRange[0] = 0;
        this.mTempVisiblePagesRange[1] = getPageCount() - 1;
        this.mReorderingStarted = true;
        if (this.mTempVisiblePagesRange[0] > dragViewIndex || dragViewIndex > this.mTempVisiblePagesRange[1]) {
            return false;
        }
        this.mDragView = getChildAt(dragViewIndex);
        this.mDragView.animate().scaleX(1.15f).scaleY(1.15f).setDuration(100).start();
        this.mDragViewBaselineLeft = (float) this.mDragView.getLeft();
        snapToPage(getPageNearestToCenterOfScreen());
        onStartReordering();
        return true;
    }

    boolean isReordering(boolean testTouchState) {
        boolean state = this.mIsReordering;
        if (!testTouchState) {
            return state;
        }
        return state & (this.mTouchState == 2);
    }

    void endReordering() {
        if (this.mReorderingStarted) {
            this.mReorderingStarted = false;
            final Runnable onCompleteRunnable = new Runnable() {
                public void run() {
                    PagedView.this.onEndReordering();
                }
            };
            this.mPostReorderingPreZoomInRunnable = new Runnable() {
                public void run() {
                    onCompleteRunnable.run();
                }
            };
            this.mPostReorderingPreZoomInRemainingAnimationCount = 2;
            snapToPage(indexOfChild(this.mDragView), 0);
            animateDragViewToOriginalPosition();
            return;
        }
        this.mIsReordering = false;
        this.mDragView = null;
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        boolean z = true;
        super.onInitializeAccessibilityNodeInfo(info);
        if (getPageCount() <= 1) {
            z = false;
        }
        info.setScrollable(z);
        if (getCurrentPage() < getPageCount() - 1) {
            info.addAction(4096);
        }
        if (getCurrentPage() > 0) {
            info.addAction(8192);
        }
        info.setClassName(getClass().getName());
        info.setLongClickable(false);
        info.removeAction(AccessibilityAction.ACTION_LONG_CLICK);
    }

    public void sendAccessibilityEvent(int eventType) {
        if (eventType != 4096) {
            super.sendAccessibilityEvent(eventType);
        }
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        boolean z = true;
        super.onInitializeAccessibilityEvent(event);
        if (getPageCount() <= 1) {
            z = false;
        }
        event.setScrollable(z);
    }

    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (super.performAccessibilityAction(action, arguments)) {
            return true;
        }
        switch (action) {
            case 4096:
                if (getCurrentPage() < getPageCount() - 1) {
                    scrollRight();
                    return true;
                }
                break;
            case 8192:
                if (getCurrentPage() > 0) {
                    scrollLeft();
                    return true;
                }
                break;
        }
        return false;
    }

    protected String getCurrentPageDescription() {
        return String.format(getContext().getString(R.string.default_scroll_format), new Object[]{getNextPage() + 1, getChildCount()});
    }

    public boolean onHoverEvent(MotionEvent event) {
        return true;
    }

    protected void invalidatePageData() {
        invalidatePageData(-1, false);
    }

    protected void invalidatePageData(int page) {
        invalidatePageData(page, false);
    }

    private void invalidatePageData(int page, boolean immediateAndOnly) {
        if (isContentsRefreshable() && isDataReady()) {
            this.mDirtyPageContent.clear();
            syncPages();
            measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
            if (page > -1) {
                setCurrentPage(Math.min(getPageCount() - 1, page));
            }
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                this.mDirtyPageContent.add(Boolean.TRUE);
            }
            loadAssociatedPages(this.mCurrentPage, immediateAndOnly);
            requestLayout();
        }
    }

    protected boolean isContentsRefreshable() {
        return false;
    }

    protected boolean isDataReady() {
        return this.mIsDataReady;
    }

    public void setDataReady() {
        this.mIsDataReady = true;
    }

    protected void syncPages() {
    }

    protected void syncPageItems(int page, boolean immediate) {
    }

    protected int getPageCacheSize() {
        return getChildCount();
    }

    private boolean isActivePage(int page) {
        return this.mActivePages.contains(page);
    }

    public void loadAssociatedPages(int page) {
        loadAssociatedPages(page, false);
    }

    private void loadAssociatedPages(int page, boolean immediateAndOnly) {
        if (isContentsRefreshable()) {
            int i;
            int count = getChildCount();
            for (i = this.mDirtyPageContent.size(); i < count; i++) {
                this.mDirtyPageContent.add(Boolean.TRUE);
            }
            if (page >= 0 && page < count) {
                setActivePages(page);
                syncPageItemsInternal(page, immediateAndOnly);
                if (!immediateAndOnly) {
                    for (i = 0; i < count; i++) {
                        if (page != i) {
                            if (isActivePage(i)) {
                                syncPageItemsInternal(i, false);
                            } else {
                                Page pageLayout = (Page) getPageAt(i);
                                if (pageLayout.getPageItemCount() > 0) {
                                    pageLayout.removeAllViewsOnPage();
                                }
                                this.mDirtyPageContent.set(i, Boolean.TRUE);
                                getPageAt(i).setVisibility(View.INVISIBLE);
                            }
                        }
                    }
                }
            }
        }
    }

    protected void setActivePages(int currentPageIndex) {
        this.mActivePages.clear();
        ArrayList<Integer> activePages = this.mActivePages;
        activePages.add(currentPageIndex);
        int cacheSize = getPageCacheSize();
        int pageCount = getPageCount();
        int i;
        if (cacheSize >= pageCount) {
            for (i = 0; i < pageCount; i++) {
                activePages.add(i);
            }
            return;
        }
        int size = cacheSize - 1;
        int pagesToLeft = Math.round(((float) size) / 2.0f);
        int pagesToRight = size / 2;
        for (i = 1; i <= pagesToLeft; i++) {
            int leftIndex = currentPageIndex - i;
            if (leftIndex >= 0) {
                activePages.add(leftIndex);
            }
        }
        for (i = 1; i <= pagesToRight; i++) {
            int rightIndex = currentPageIndex + i;
            if (rightIndex < pageCount) {
                activePages.add(rightIndex);
            }
        }
    }

    private void syncPageItemsInternal(int page, boolean immediateAndOnly) {
        if (((Boolean) this.mDirtyPageContent.get(page)).booleanValue()) {
            syncPageItems(page, immediateAndOnly);
            getPageAt(page).setVisibility(View.VISIBLE);
            this.mDirtyPageContent.set(page, Boolean.FALSE);
        }
    }

    public void setCrosshairsVisibilityChilds(int visibilityChilds, boolean animate) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            CellLayout cellLayout = (CellLayout) getPageAt(i);
            if (cellLayout != null) {
                boolean animatePage = animate ? getCurrentPage() == indexOfChild(cellLayout) : false;
                cellLayout.setCrossHairAnimatedVisibility(visibilityChilds, animatePage);
            }
        }
    }

    public void setCrosshairsVisibilityChilds(int visibilityChilds) {
        setCrosshairsVisibilityChilds(visibilityChilds, true);
    }

    public void setPageIndicatorAnimation(boolean enable) {
        this.mEnablePageIndicatorAnim = enable;
    }

    public ArrayList<IconView> findIconViews(String keyword) {
        if (keyword == null) {
            return null;
        }
        ArrayList<IconView> result = new ArrayList();
        for (int i = 0; i < getPageCount(); i++) {
            CellLayoutChildren clc = ((CellLayout) getChildAt(i)).getCellLayoutChildren();
            for (int j = 0; j < clc.getChildCount(); j++) {
                View v = clc.getChildAt(j);
                if ((v instanceof IconView) && keyword.replaceAll("\\s", "").compareToIgnoreCase(((IconView) v).getTitle().replaceAll("\\s", "")) == 0) {
                    result.add((IconView) v);
                }
            }
        }
        return result;
    }

    public IconView findIconView(ComponentName cn) {
        if (cn == null) {
            return null;
        }
        for (int i = 0; i < getPageCount(); i++) {
            CellLayoutChildren clc = ((CellLayout) getChildAt(i)).getCellLayoutChildren();
            for (int j = 0; j < clc.getChildCount(); j++) {
                View v = clc.getChildAt(j);
                if (v.getTag() instanceof IconInfo) {
                    IconInfo info = (IconInfo) v.getTag();
                    if (info == null) {
                        continue;
                    } else {
                        ComponentName compareCn = info.componentName;
                        if (compareCn == null && info.getIntent() != null) {
                            compareCn = info.getIntent().getComponent();
                        }
                        if (compareCn != null && compareCn.getClassName().equals(cn.getClassName())) {
                            return (IconView) v;
                        }
                    }
                }
            }
        }
        return null;
    }

    public void showHintPages() {
        this.mScrollState = -1;
        postDelayed(new Runnable() {
            public void run() {
                PagedView.this.mIsShowingHintPages = true;
            }
        }, 200);
        float to = Utilities.sIsRtl ? (float) (-this.mHintPageWidth) : (float) this.mHintPageWidth;
        if (this.mCurrentPage != 0) {
            animateTranslationPage(getPageAt(this.mCurrentPage - 1), 0.0f, to);
        }
        if (this.mCurrentPage < getChildCount()) {
            animateTranslationPage(getPageAt(this.mCurrentPage + 1), 0.0f, -to);
        }
    }

    public void hideHintPages() {
        if (this.mIsShowingHintPages) {
            hideHintPages(getHintCenterPage());
        }
    }

    private void hideHintPages(int dropScreen) {
        this.mScrollState = -1;
        postDelayed(new Runnable() {
            public void run() {
                PagedView.this.mIsShowingHintPages = false;
            }
        }, 200);
        float from = Utilities.sIsRtl ? (float) (-this.mHintPageWidth) : (float) this.mHintPageWidth;
        if (dropScreen > 0) {
            animateTranslationPage(getPageAt(dropScreen - 1), from, 0.0f);
        }
        if (dropScreen < getChildCount()) {
            animateTranslationPage(getPageAt(dropScreen + 1), -from, 0.0f);
        }
        View dropScreenView = getPageAt(dropScreen);
        if (!(dropScreenView == null || dropScreenView.getTranslationX() == 0.0f)) {
            dropScreenView.setTranslationX(0.0f);
        }
        if (isTranslatedPages()) {
            Log.d(TAG, "resetPulledPages called from hideHintPage " + this.mTranslateAllPages + ", " + getTranslationX());
            resetPulledPages();
        }
    }

    public boolean isShowingHintPages() {
        return this.mIsShowingHintPages;
    }

    private void animateTranslationPage(View page, float from, float to) {
        ObjectAnimator pageViewAnim = ObjectAnimator.ofFloat(page, "translationX", new float[]{from, to});
        pageViewAnim.setDuration(200);
        pageViewAnim.start();
    }

    private void setHintPageTranslation() {
        int leftPage = Math.max(Math.min(this.mNextPage - 1, this.mCurrentPage - 1), 0);
        int rightPage = Math.min(Math.max(this.mNextPage + 1, this.mCurrentPage + 1), getChildCount() - 1);
        float translationX = Utilities.sIsRtl ? (float) (-this.mHintPageWidth) : (float) this.mHintPageWidth;
        for (int i = leftPage; i <= rightPage; i++) {
            View page = getChildAt(i);
            if (page != null) {
                if (i == this.mNextPage - 1) {
                    page.setTranslationX(1.0f * translationX);
                } else if (i == this.mNextPage + 1) {
                    page.setTranslationX(-translationX);
                } else {
                    page.setTranslationX(0.0f);
                }
            }
        }
    }

    public void dragPullingPages(float touchX) {
        if (this.mIsShowingHintPages) {
            if (this.mScrollState == -1) {
                if (this.mTranslateAllPages == 0.0f && !this.mContinuallyScroll && isPullingPageTouchArea(touchX)) {
                    this.mScrollState = 0;
                } else if (!isPullingPageTouchArea(touchX)) {
                    this.mContinuallyScroll = false;
                }
            } else if (this.mScrollState == 4 && !isPullingPageTouchArea(touchX)) {
                this.mScrollState = 1;
            }
            if (this.mScrollState == 0) {
                this.mScrollState = 2;
                int currentPage = getHintCenterPage();
                if (!Utilities.sIsRtl && touchX > ((float) this.mHintPageRightZone) && currentPage < getPageCount() - 1) {
                    this.mTranslateAllPages = -this.mTranslatePagesOffset;
                } else if (!Utilities.sIsRtl && touchX < ((float) this.mHintPageLeftZone) && currentPage > 0) {
                    this.mTranslateAllPages = this.mTranslatePagesOffset;
                } else if (Utilities.sIsRtl && touchX > ((float) this.mHintPageRightZone) && currentPage > 0) {
                    this.mTranslateAllPages = -this.mTranslatePagesOffset;
                } else if (Utilities.sIsRtl && touchX < ((float) this.mHintPageLeftZone) && currentPage < getPageCount() - 1) {
                    this.mTranslateAllPages = this.mTranslatePagesOffset;
                }
                if (this.mPullingPagesAnim != null && this.mPullingPagesAnim.isRunning()) {
                    this.mPullingPagesAnim.cancel();
                }
                animatePullingPages();
                return;
            } else if (this.mScrollState == 1) {
                this.mScrollState = 3;
                animatePullingPages();
                return;
            } else {
                return;
            }
        }
        Log.e(TAG, "Cannot drag to next page. It's abnormal state during dragging item.");
    }

    protected void resetPulledPages() {
        this.mTranslateAllPages = 0.0f;
        setTranslationX(this.mTranslateAllPages);
    }

    private int getHintCenterPage() {
        return (!isPageMoving() || this.mNextPage == -1) ? this.mCurrentPage : this.mNextPage;
    }

    private boolean isPullingPageTouchArea(float touchX) {
        return touchX > ((float) this.mHintPageRightZone) || touchX < ((float) this.mHintPageLeftZone);
    }

    public boolean isScrolling() {
        return !this.mScroller.isFinished();
    }

    public float getPageBackgroundAlpha() {
        return this.mPageBackgroundAlpha;
    }

    public void forcelyAnimateReturnPages() {
        if (this.mScrollState != -1 && this.mTranslateAllPages != 0.0f) {
            if (this.mPullingPagesAnim != null && this.mPullingPagesAnim.isRunning()) {
                this.mPullingPagesAnim.cancel();
            }
            this.mScrollState = 3;
            animatePullingPages();
        }
    }

    private void animatePullingPages() {
        Log.d(TAG, "animatePullingPages() : " + getTranslationX());
        if (this.mScrollState == 2) {
            this.mPullingPagesAnim = ObjectAnimator.ofFloat(this, "translationX", new float[]{0.0f, this.mTranslateAllPages});
        } else if (this.mScrollState == 3) {
            this.mPullingPagesAnim = ObjectAnimator.ofFloat(this, "translationX", new float[]{this.mTranslateAllPages, 0.0f});
        }
        this.mPullingPagesAnim.setDuration(200);
        this.mPullingPagesAnim.setInterpolator(new DecelerateInterpolator());
        this.mPullingPagesAnim.start();
        this.mPullingPagesAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                Log.d(PagedView.TAG, "End of page pulling anim. State is " + PagedView.this.mScrollState);
                if (PagedView.this.mScrollState == 3) {
                    PagedView.this.mTranslateAllPages = 0.0f;
                    PagedView.this.mScrollState = -1;
                } else if (PagedView.this.mScrollState == 2) {
                    PagedView.this.mScrollState = 4;
                    PagedView.this.mContinuallyScroll = true;
                } else {
                    PagedView.this.resetPulledPages();
                    PagedView.this.mScrollState = -1;
                }
            }
        });
    }

    private boolean isTranslatedPages() {
        return (this.mTranslateAllPages == 0.0f && getTranslationX() == 0.0f) ? false : true;
    }

    public boolean canDragScroll() {
        return this.mScrollState == 4 || this.mContinuallyScroll;
    }

    public boolean supportWhiteBg() {
        return true;
    }

    protected void cancelScroll() {
        if (this.mTouchState == 1 || this.mTouchState == 2) {
            if (this.mTouchState == 2) {
                endReordering();
            }
            snapToDestination();
        }
        this.mTouchState = 0;
        releaseVelocityTracker();
    }

    public void setPageScrollListener(PageScrollListener pageScrollListener) {
        this.mPageScrollListener = pageScrollListener;
    }

    protected void notifyPageScroll(int page, int x, int y, int scrollX, int pageCount) {
        if (this.mPageScrollListener != null) {
            this.mPageScrollListener.onPageScroll(page, x, y, scrollX, pageCount);
        }
    }

    protected void notifyPageChange(int page, int scrollX, int pageCount) {
        if (this.mPageScrollListener != null) {
            this.mPageScrollListener.onPageChange(page, scrollX, pageCount);
        }
    }

    public int getCustomPageCount() {
        return 0;
    }

    public int getSupportCustomPageCount() {
        return 0;
    }

    public void setScrollDeterminator(ScrollDeterminator scrollDeterminator) {
        this.mScrollDeterminator = scrollDeterminator;
    }

    private boolean isHorizontalScroll() {
        return this.mScrollDeterminator == null ? true : this.mScrollDeterminator.isHorizontalScroll();
    }

    protected void onConfigurationChangedIfNeeded() {
        if (this.mPageIndicator != null) {
            this.mPageIndicator.offsetWindowCenterTo(false);
            updatePageIndicator();
        }
    }

    public void cancelDeferLoadAssociatedPagesUntilScrollCompletes() {
        this.mDeferLoadAssociatedPagesUntilScrollCompletes = false;
    }

    private boolean checkTouchedPageIndicator(float x, float y) {
        int[] cordinate = new int[2];
        this.mPageIndicator.getLocationOnScreen(cordinate);
        float minWidth = (float) cordinate[0];
        float minHeight = (float) cordinate[1];
        Rect pageIndicatorArea = new Rect((int) minWidth, (int) minHeight, (int) (minWidth + ((float) this.mPageIndicator.getWidth())), (int) (minHeight + ((float) this.mPageIndicator.getHeight())));
        if (this.mPageIndicator == null || !pageIndicatorArea.contains((int) x, (int) y)) {
            return false;
        }
        return true;
    }

    public void updateOnlyCurrentPage(boolean updateOnlyCurrentPage) {
        this.mUpdateOnlyCurrentPage = updateOnlyCurrentPage;
    }

    private void disablePageIndicatorAnimation() {
        this.mEnablePageIndicatorAnim = false;
        if (this.mPageIndicator != null) {
            Iterator it = this.mPageIndicator.getMarkers().iterator();
            while (it.hasNext()) {
                ((PageIndicatorMarker) it.next()).disableMarkerAnimation();
            }
            this.mPageIndicator.disableLayoutTransitions();
        }
    }

    private void enablePageIndicatorAnimation() {
        this.mEnablePageIndicatorAnim = true;
        if (this.mPageIndicator != null) {
            Iterator it = this.mPageIndicator.getMarkers().iterator();
            while (it.hasNext()) {
                ((PageIndicatorMarker) it.next()).enableMarkerAnimation();
            }
            this.mPageIndicator.enableLayoutTransitions();
        }
    }

    public boolean isResumed() {
        return this.mIsResumed;
    }

    public void onResume() {
        this.mIsResumed = true;
        enablePageIndicatorAnimation();
    }

    public void onPause() {
        this.mIsResumed = false;
        disablePageIndicatorAnimation();
    }

    public void updateMarginForPageIndicator() {
        if (this.mPageIndicator != null) {
            this.mPageIndicator.updateMarginForPageIndicator();
        }
    }
}

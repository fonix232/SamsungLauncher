package com.android.launcher3.common.base.view;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewDebug.ExportedProperty;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.view.FolderIconView.FolderRingAnimator;
import com.android.launcher3.home.LauncherAppWidgetHostView;
import com.android.launcher3.home.LauncherAppWidgetHostView.ResizeResult;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.animation.InterruptibleInOutAnimator;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.sec.android.app.launcher.R;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

public abstract class CellLayout extends ViewGroup {
    private static final int BADGE_SCALE_ANIM_DURATION = 200;
    private static final float CREATE_FOLDER_SCALE = 0.3f;
    private static final int CREATE_FOLDER_SCALE_DURATION = 333;
    static final String TAG = "CellLayout";
    private static Drawable sCrosshairsDrawable;
    private static final Paint sPaint = new Paint();
    private float mBackgroundAlpha;
    protected int mCellHeight;
    protected int mCellWidth;
    protected CellLayoutChildren mChildren;
    protected int mCountX;
    protected int mCountY;
    protected CrossHairView mCrossHairView;
    private final int[] mDragCell;
    private float[] mDragOutlineAlphas;
    private InterruptibleInOutAnimator[] mDragOutlineAnims;
    private int mDragOutlineCurrent;
    private Rect[] mDragOutlines;
    private final int[] mDragSpan;
    private boolean mDragging;
    private TimeInterpolator mEaseOutInterpolator;
    protected int mFixedCellHeight;
    protected int mFixedCellWidth;
    private int mFixedHeight;
    private int mFixedWidth;
    private ArrayList<FolderRingAnimator> mFolderOuterRings;
    protected int mHeightGap;
    protected int mIconStartPadding;
    protected boolean mLandscape;
    protected Launcher mLauncher;
    protected boolean[][] mOccupied;
    private HashMap<LayoutParams, Animator> mReorderAnimators;
    private final int[] mTempLocation;
    private final Rect mTempRect;
    protected final Stack<Rect> mTempRectStack;
    private final int[] mTmpPoint;
    protected int mWidthGap;
    private View targetView;

    public static final class CellInfo {
        public View cell;
        public int cellX = -1;
        public int cellY = -1;
        public long container;
        public CellLayout layout;
        public long screenId;
        public int spanX;
        public int spanY;

        public CellInfo(View v, ItemInfo info) {
            this.cell = v;
            this.cellX = info.cellX;
            this.cellY = info.cellY;
            this.spanX = info.spanX;
            this.spanY = info.spanY;
            this.screenId = info.screenId;
            this.container = info.container;
            this.layout = (CellLayout) v.getParent().getParent();
        }

        public String toString() {
            return "Cell[view=" + (this.cell == null ? "null" : this.cell.getClass()) + ", x=" + this.cellX + ", y=" + this.cellY + "]";
        }
    }

    protected class CrossHairView extends View {
        static final int CROSS_HAIR_VI_DURATION = 300;
        private boolean mAnimate = false;
        private boolean mIsAnimaing = false;
        CellLayout mParent;
        private Runnable mRunnable;

        public CrossHairView(Context context, CellLayout cl) {
            super(context);
            this.mParent = cl;
            setVisibility(View.GONE);
        }

        protected void onVisibilityChanged(final View changedView, final int visibility) {
            float endAlpha = 1.0f;
            if (this.mAnimate) {
                this.mAnimate = false;
                if (this.mParent == null || !this.mParent.isShown()) {
                    this.mIsAnimaing = false;
                    if (visibility == 8 && this.mParent != null) {
                        this.mParent.removeView(changedView);
                    }
                    if (this.mRunnable != null) {
                        changedView.post(this.mRunnable);
                        return;
                    }
                    return;
                }
                float startAlpha = visibility == 0 ? 0.0f : 1.0f;
                if (visibility != 0) {
                    endAlpha = 0.0f;
                }
                AlphaAnimation alpha = new AlphaAnimation(startAlpha, endAlpha);
                alpha.setDuration(300);
                alpha.setAnimationListener(new AnimationListener() {
                    public void onAnimationStart(Animation animation) {
                        CrossHairView.this.mIsAnimaing = true;
                    }

                    public void onAnimationEnd(Animation animation) {
                        CrossHairView.this.mIsAnimaing = false;
                        if (visibility == 8 && CrossHairView.this.mParent != null) {
                            CrossHairView.this.mParent.removeView(changedView);
                        }
                        if (CrossHairView.this.mRunnable != null) {
                            changedView.post(CrossHairView.this.mRunnable);
                        }
                    }

                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                changedView.startAnimation(alpha);
            } else if (visibility == 8 && this.mParent != null) {
                this.mParent.removeView(changedView);
            }
        }

        protected void onDraw(Canvas canvas) {
            int width = CellLayout.sCrosshairsDrawable.getIntrinsicWidth();
            int height = CellLayout.sCrosshairsDrawable.getIntrinsicHeight();
            int x = ((getPaddingLeft() - (CellLayout.this.mWidthGap / 2)) - (width / 2)) + (CellLayout.this.mCellWidth + CellLayout.this.mWidthGap);
            for (int col = 1; col <= CellLayout.this.mCountX - 1; col++) {
                int y = (((getPaddingTop() + CellLayout.this.getTopPaddingCustomPage()) - (CellLayout.this.mHeightGap / 2)) - (height / 2)) + (CellLayout.this.mCellHeight + CellLayout.this.mHeightGap);
                for (int row = 1; row <= CellLayout.this.mCountY - 1; row++) {
                    CellLayout.sCrosshairsDrawable.setBounds(x, y, x + width, y + height);
                    CellLayout.sCrosshairsDrawable.draw(canvas);
                    y += CellLayout.this.mCellHeight + CellLayout.this.mHeightGap;
                }
                x += CellLayout.this.mCellWidth + CellLayout.this.mWidthGap;
            }
        }

        public void setAnimate(boolean animate) {
            this.mAnimate = animate;
        }

        public boolean isAnimating() {
            return this.mIsAnimaing;
        }

        public void setRunnable(Runnable runnable) {
            this.mRunnable = runnable;
        }
    }

    public static class LayoutParams extends MarginLayoutParams {
        public boolean canReorder;
        @ExportedProperty
        public int cellHSpan;
        @ExportedProperty
        public int cellVSpan;
        @ExportedProperty
        public int cellX;
        @ExportedProperty
        public int cellY;
        boolean dropped;
        public boolean isLockedToGrid;
        public int tmpCellX;
        public int tmpCellY;
        public boolean useTmpCoords;
        @ExportedProperty
        public int x;
        @ExportedProperty
        public int y;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.isLockedToGrid = true;
            this.canReorder = true;
            this.cellHSpan = 1;
            this.cellVSpan = 1;
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams source) {
            super(source);
            this.isLockedToGrid = true;
            this.canReorder = true;
            this.cellHSpan = 1;
            this.cellVSpan = 1;
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            this.isLockedToGrid = true;
            this.canReorder = true;
            int i = source.cellX;
            this.tmpCellX = i;
            this.cellX = i;
            i = source.cellY;
            this.tmpCellY = i;
            this.cellY = i;
            this.cellHSpan = source.cellHSpan;
            this.cellVSpan = source.cellVSpan;
        }

        public LayoutParams(int cellX, int cellY, int cellHSpan, int cellVSpan) {
            super(-1, -1);
            this.isLockedToGrid = true;
            this.canReorder = true;
            this.tmpCellX = cellX;
            this.cellX = cellX;
            this.tmpCellY = cellY;
            this.cellY = cellY;
            this.cellHSpan = cellHSpan;
            this.cellVSpan = cellVSpan;
        }

        public void setup(int cellWidth, int cellHeight, int widthGap, int heightGap, int colCount) {
            if (this.isLockedToGrid) {
                int myCellHSpan = this.cellHSpan;
                int myCellVSpan = this.cellVSpan;
                int myCellX = this.useTmpCoords ? this.tmpCellX : this.cellX;
                int myCellY = this.useTmpCoords ? this.tmpCellY : this.cellY;
                if (Utilities.sIsRtl) {
                    myCellX = (colCount - myCellX) - this.cellHSpan;
                    if (myCellX < 0) {
                        myCellX = 0;
                    }
                }
                this.width = (((myCellHSpan * cellWidth) + ((myCellHSpan - 1) * widthGap)) - this.leftMargin) - this.rightMargin;
                this.height = (((myCellVSpan * cellHeight) + ((myCellVSpan - 1) * heightGap)) - this.topMargin) - this.bottomMargin;
                this.x = ((cellWidth + widthGap) * myCellX) + this.leftMargin;
                this.y = ((cellHeight + heightGap) * myCellY) + this.topMargin;
            }
        }

        public String toString() {
            return "(" + this.cellX + ", " + this.cellY + ")";
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

    protected abstract void setCellDimensions();

    public CellLayout(Context context) {
        this(context, null);
    }

    public CellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyle) {
        int i;
        super(context, attrs, defStyle);
        this.mLandscape = false;
        this.mTmpPoint = new int[2];
        this.mTempLocation = new int[2];
        this.mFolderOuterRings = new ArrayList();
        this.mFixedWidth = -1;
        this.mFixedHeight = -1;
        this.mDragOutlines = new Rect[4];
        this.mDragOutlineAlphas = new float[this.mDragOutlines.length];
        this.mDragOutlineAnims = new InterruptibleInOutAnimator[this.mDragOutlines.length];
        this.mDragOutlineCurrent = 0;
        this.mReorderAnimators = new HashMap();
        this.mDragCell = new int[2];
        this.mDragSpan = new int[2];
        this.mDragging = false;
        this.mTempRect = new Rect();
        this.targetView = null;
        this.mTempRectStack = new Stack();
        setWillNotDraw(false);
        setClipToPadding(false);
        this.mLauncher = (Launcher) context;
        initChildren(context);
        setCellDimensions();
        this.mOccupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{this.mCountX, this.mCountY});
        setAlwaysDrawnWithCacheEnabled(false);
        this.mEaseOutInterpolator = new DecelerateInterpolator(2.5f);
        int[] iArr = this.mDragCell;
        this.mDragCell[1] = -1;
        iArr[0] = -1;
        iArr = this.mDragSpan;
        this.mDragSpan[1] = -1;
        iArr[0] = -1;
        for (i = 0; i < this.mDragOutlines.length; i++) {
            this.mDragOutlines[i] = new Rect(-1, -1, -1, -1);
        }
        int duration = getResources().getInteger(R.integer.config_dragOutlineFadeTime);
        Arrays.fill(this.mDragOutlineAlphas, 0.0f);
        for (i = 0; i < this.mDragOutlineAnims.length; i++) {
            final InterruptibleInOutAnimator anim = new InterruptibleInOutAnimator(this, (long) duration, 0.0f, 255.0f);
            anim.getAnimator().setInterpolator(this.mEaseOutInterpolator);
            final int thisIndex = i;
            anim.getAnimator().addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (((Drawable) anim.getTag()) == null) {
                        animation.cancel();
                        return;
                    }
                    CellLayout.this.mDragOutlineAlphas[thisIndex] = ((Float) animation.getAnimatedValue()).floatValue();
                    CellLayout.this.invalidate(CellLayout.this.mDragOutlines[thisIndex]);
                }
            });
            anim.getAnimator().addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    if (((Float) ((ValueAnimator) animation).getAnimatedValue()).floatValue() == 0.0f) {
                        anim.setTag(null);
                    }
                }
            });
            this.mDragOutlineAnims[i] = anim;
        }
        addView(this.mChildren);
        this.mCrossHairView = new CrossHairView(context, this);
        if (sCrosshairsDrawable == null) {
            sCrosshairsDrawable = context.getDrawable(R.drawable.edit_guide);
        }
    }

    protected void initChildren(Context context) {
        this.mChildren = new CellLayoutChildren(context);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return super.onTouchEvent(ev);
    }

    public void enableHardwareLayer(boolean hasLayer) {
        CellLayoutChildren cellLayoutChildren = this.mChildren;
        int i = (!hasLayer || this.mLauncher.isPaused()) ? 0 : 2;
        cellLayoutChildren.setLayerType(i, sPaint);
    }

    public void buildHardwareLayer() {
        this.mChildren.buildLayer();
    }

    public void setCellDimensions(int width, int height, int widthGap, int heightGap) {
        this.mCellWidth = width;
        this.mFixedCellWidth = width;
        this.mCellHeight = height;
        this.mFixedCellHeight = height;
        this.mWidthGap = widthGap;
        this.mHeightGap = heightGap;
        this.mChildren.setCellDimensions(this.mCellWidth, this.mCellHeight, this.mWidthGap, this.mHeightGap, this.mCountX);
    }

    public void setGridSize(int x, int y) {
        this.mCountX = x;
        this.mCountY = y;
        this.mOccupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{this.mCountX, this.mCountY});
        this.mTempRectStack.clear();
        this.mChildren.setCellDimensions(this.mCellWidth, this.mCellHeight, this.mWidthGap, this.mHeightGap, this.mCountX);
        requestLayout();
    }

    protected void onDraw(Canvas canvas) {
        int i;
        for (i = 0; i < this.mDragOutlines.length; i++) {
            float alpha = this.mDragOutlineAlphas[i];
            if (alpha > 0.0f) {
                this.mTempRect.set(this.mDragOutlines[i]);
                Utilities.rectAboutCenter(this.mTempRect);
                Drawable outline = (Drawable) this.mDragOutlineAnims[i].getTag();
                outline.setAlpha((int) alpha);
                outline.setBounds(this.mTempRect);
                outline.draw(canvas);
            }
        }
        for (i = 0; i < this.mFolderOuterRings.size(); i++) {
            FolderRingAnimator fra = (FolderRingAnimator) this.mFolderOuterRings.get(i);
            cellToPoint(fra.mCellX, fra.mCellY, 1, this.mTempLocation);
            View child = getChildAt(fra.mCellX, fra.mCellY);
            if (child != null) {
                int centerX = this.mTempLocation[0] + (this.mCellWidth / 2);
                int centerY = ((this.mTempLocation[1] + (getContentIconSize() / 2)) + getContentTop()) + child.getPaddingTop();
                if (this.mLandscape) {
                    centerX = (this.mTempLocation[0] + (getContentIconSize() / 2)) + ((IconView) child).getIconVew().getLeft();
                    centerY = this.mTempLocation[1] + (this.mCellHeight / 2);
                }
                Drawable d = fra.getInnerRingDrawable();
                if (d != null) {
                    int width = (int) fra.getInnerRingSize();
                    int height = width;
                    canvas.save();
                    canvas.translate((float) (centerX - (width / 2)), (float) (centerY - (width / 2)));
                    d.setBounds(0, 0, width, height);
                    d.draw(canvas);
                    canvas.restore();
                }
            }
        }
    }

    public void showFolderAccept(FolderRingAnimator fra) {
        this.mFolderOuterRings.add(fra);
    }

    public void hideFolderAccept(FolderRingAnimator fra) {
        if (this.mFolderOuterRings.contains(fra)) {
            this.mFolderOuterRings.remove(fra);
            invalidate();
        }
    }

    public void hideFolderAcceptForcedly() {
        this.mFolderOuterRings.clear();
        invalidate();
    }

    public ArrayList<FolderRingAnimator> getFolderRings() {
        return this.mFolderOuterRings;
    }

    public void restoreInstanceState(SparseArray<Parcelable> states) {
        try {
            dispatchRestoreInstanceState(states);
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "Ignoring an error while restoring a view instance state", ex);
        }
    }

    public void cancelLongPress() {
        super.cancelLongPress();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).cancelLongPress();
        }
    }

    public int getCountX() {
        return this.mCountX;
    }

    public int getCountY() {
        return this.mCountY;
    }

    public boolean addViewToCellLayout(View child, int index, int childId, LayoutParams params, boolean markCells) {
        LayoutParams lp = params;
        if (lp.cellX < 0 || lp.cellX > this.mCountX - 1 || lp.cellY < 0 || lp.cellY > this.mCountY - 1) {
            return false;
        }
        if (lp.cellHSpan < 0) {
            lp.cellHSpan = this.mCountX;
        }
        if (lp.cellVSpan < 0) {
            lp.cellVSpan = this.mCountY;
        }
        child.setId(childId);
        this.mChildren.addView(child, index, lp);
        if (markCells) {
            markCellsAsOccupiedForView(child);
        }
        return true;
    }

    public void removeAllViews() {
        clearOccupiedCells();
        this.mChildren.removeAllViews();
    }

    public void removeAllViewsInLayout() {
        if (this.mChildren.getChildCount() > 0) {
            clearOccupiedCells();
            this.mChildren.removeAllViewsInLayout();
        }
    }

    public void removeView(View view) {
        if (view instanceof CrossHairView) {
            super.removeView(view);
            return;
        }
        markCellsAsUnoccupiedForView(view);
        if (this.targetView == null) {
            this.mChildren.removeView(view);
        } else {
            createFolderVI(view);
        }
    }

    public void removeViewAt(int index) {
        markCellsAsUnoccupiedForView(this.mChildren.getChildAt(index));
        this.mChildren.removeViewAt(index);
    }

    public void removeViewInLayout(View view) {
        markCellsAsUnoccupiedForView(view);
        if (this.targetView == null) {
            this.mChildren.removeViewInLayout(view);
        } else {
            createFolderVI(view);
        }
    }

    private void createFolderVI(final View view) {
        if (view instanceof IconView) {
            ((IconView) view).animateBadge(false, 200, 0, true);
            ((IconView) view).animateTitleView(false, 0, 200, true);
        }
        view.animate().setDuration(333).scaleX(CREATE_FOLDER_SCALE).scaleY(CREATE_FOLDER_SCALE).x(this.targetView.getX()).y(this.targetView.getY()).setListener(new AnimatorListener() {
            public void onAnimationStart(Animator animation) {
            }

            public void onAnimationEnd(Animator animation) {
                CellLayout.this.mChildren.removeView(view);
            }

            public void onAnimationCancel(Animator animation) {
                CellLayout.this.mChildren.removeView(view);
            }

            public void onAnimationRepeat(Animator animation) {
            }
        }).start();
    }

    public void removeViews(int start, int count) {
        for (int i = start; i < start + count; i++) {
            markCellsAsUnoccupiedForView(this.mChildren.getChildAt(i));
        }
        this.mChildren.removeViews(start, count);
    }

    public void removeViewsInLayout(int start, int count) {
        for (int i = start; i < start + count; i++) {
            markCellsAsUnoccupiedForView(this.mChildren.getChildAt(i));
        }
        this.mChildren.removeViewsInLayout(start, count);
    }

    public void pointToCellExact(int x, int y, int spanX, int[] result) {
        int hStartPadding = getPaddingLeft();
        int vStartPadding = getPaddingTop();
        result[0] = (x - hStartPadding) / (this.mCellWidth + this.mWidthGap);
        result[1] = (y - vStartPadding) / (this.mCellHeight + this.mHeightGap);
        if (Utilities.sIsRtl) {
            result[0] = (this.mCountX - result[0]) - spanX;
        }
        int xAxis = this.mCountX;
        int yAxis = this.mCountY;
        if (result[0] < 0) {
            result[0] = 0;
        }
        if (result[0] >= xAxis) {
            result[0] = xAxis - 1;
        }
        if (result[1] < 0) {
            result[1] = 0;
        }
        if (result[1] >= yAxis) {
            result[1] = yAxis - 1;
        }
    }

    private void cellToPoint(int cellX, int cellY, int spanX, int[] result) {
        int tempCellX;
        int hStartPadding = getPaddingLeft();
        int vStartPadding = getPaddingTop();
        if (Utilities.sIsRtl) {
            tempCellX = (this.mCountX - cellX) - spanX;
        } else {
            tempCellX = cellX;
        }
        result[0] = ((this.mCellWidth + this.mWidthGap) * tempCellX) + hStartPadding;
        result[1] = ((this.mCellHeight + this.mHeightGap) * cellY) + vStartPadding;
    }

    void cellToCenterPoint(int cellX, int cellY, int[] result) {
        regionToCenterPoint(cellX, cellY, 1, 1, result);
    }

    public void regionToCenterPoint(int cellX, int cellY, int spanX, int spanY, int[] result) {
        int tempCellX;
        int hStartPadding = getPaddingLeft();
        int vStartPadding = getPaddingTop();
        if (Utilities.sIsRtl) {
            tempCellX = (this.mCountX - cellX) - spanX;
        } else {
            tempCellX = cellX;
        }
        result[0] = (((this.mCellWidth + this.mWidthGap) * tempCellX) + hStartPadding) + (((this.mCellWidth * spanX) + ((spanX - 1) * this.mWidthGap)) / 2);
        result[1] = (((this.mCellHeight + this.mHeightGap) * cellY) + vStartPadding) + (((this.mCellHeight * spanY) + ((spanY - 1) * this.mHeightGap)) / 2);
    }

    public void regionToIconCenterPoint(int cellX, int cellY, int spanX, int spanY, int[] result) {
        int tempCellX;
        int hStartPadding = getPaddingLeft();
        int vStartPadding = getPaddingTop();
        if (Utilities.sIsRtl) {
            tempCellX = (this.mCountX - cellX) - spanX;
        } else {
            tempCellX = cellX;
        }
        result[0] = (((this.mCellWidth + this.mWidthGap) * tempCellX) + hStartPadding) + (((spanX * ((this.mIconStartPadding * 2) + getContentIconSize())) + ((spanX - 1) * this.mWidthGap)) / 2);
        result[1] = (((this.mCellHeight + this.mHeightGap) * cellY) + vStartPadding) + (((this.mCellHeight * spanY) + ((spanY - 1) * this.mHeightGap)) / 2);
    }

    public void regionToRect(int cellX, int cellY, int spanX, int spanY, Rect result) {
        int tempCellX;
        int hStartPadding = getPaddingLeft();
        int vStartPadding = getPaddingTop();
        if (Utilities.sIsRtl) {
            tempCellX = (this.mCountX - cellX) - spanX;
        } else {
            tempCellX = cellX;
        }
        int left = hStartPadding + ((this.mCellWidth + this.mWidthGap) * tempCellX);
        int top = vStartPadding + ((this.mCellHeight + this.mHeightGap) * cellY);
        result.set(left, top, ((this.mCellWidth * spanX) + ((spanX - 1) * this.mWidthGap)) + left, ((this.mCellHeight * spanY) + ((spanY - 1) * this.mHeightGap)) + top);
    }

    public float getDistanceFromCell(float x, float y, int[] cell) {
        if (this.mLandscape) {
            regionToIconCenterPoint(cell[0], cell[1], 1, 1, this.mTmpPoint);
        } else {
            cellToCenterPoint(cell[0], cell[1], this.mTmpPoint);
        }
        return (float) Math.hypot((double) (x - ((float) this.mTmpPoint[0])), (double) (y - ((float) this.mTmpPoint[1])));
    }

    public int getCellWidth() {
        return this.mCellWidth;
    }

    public int getCellHeight() {
        return this.mCellHeight;
    }

    public int getWidthGap() {
        return this.mWidthGap;
    }

    public int getHeightGap() {
        return this.mHeightGap;
    }

    public void setFixedSize(int width, int height) {
        this.mFixedWidth = width;
        this.mFixedHeight = height;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int childWidthSize = widthSize - (getPaddingLeft() + getPaddingRight());
        int childHeightSize = heightSize - (getPaddingTop() + getPaddingBottom());
        if (this.mFixedCellWidth < 0 || this.mFixedCellHeight < 0) {
            int cw = DeviceProfile.calculateCellWidthOrHeight(childWidthSize, this.mWidthGap, this.mCountX);
            int ch = DeviceProfile.calculateCellWidthOrHeight(childHeightSize, this.mHeightGap, this.mCountY);
            if (!(cw == this.mCellWidth && ch == this.mCellHeight)) {
                this.mCellWidth = cw;
                this.mCellHeight = ch;
                this.mChildren.setCellDimensions(this.mCellWidth, this.mCellHeight, this.mWidthGap, this.mHeightGap, this.mCountX);
            }
        }
        int newWidth = childWidthSize;
        int newHeight = childHeightSize;
        if (this.mFixedWidth > 0 && this.mFixedHeight > 0) {
            newWidth = this.mFixedWidth;
            newHeight = this.mFixedHeight;
        } else if (widthSpecMode == 0 || heightSpecMode == 0) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }
        this.mChildren.measure(MeasureSpec.makeMeasureSpec(newWidth, 1073741824), MeasureSpec.makeMeasureSpec(newHeight, 1073741824));
        int maxWidth = this.mChildren.getMeasuredWidth();
        int maxHeight = this.mChildren.getMeasuredHeight();
        if (this.mFixedWidth <= 0 || this.mFixedHeight <= 0) {
            setMeasuredDimension(widthSize, heightSize);
        } else {
            setMeasuredDimension(maxWidth, maxHeight);
        }
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = getPaddingLeft();
        int right = getPaddingRight();
        int top = getPaddingTop();
        int bottom = getPaddingBottom();
        setChildrenLayout(left, top, (r - l) - right, (b - t) - bottom);
        if (this.mCrossHairView != null && this.mCrossHairView.getVisibility() != 8) {
            this.mCrossHairView.layout(left, top, (r - l) - right, (b - t) - bottom);
        }
    }

    protected void setChildrenLayout(int l, int t, int r, int b) {
        this.mChildren.layout(l, t, r, b);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public void setChildrenDrawingCacheEnabled(boolean enabled) {
        this.mChildren.setChildrenDrawingCacheEnabled(enabled);
    }

    public void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        this.mChildren.setChildrenDrawnWithCacheEnabled(enabled);
    }

    public void setBackground(Drawable background) {
        super.setBackground(background);
        if (background != null) {
            background.setAlpha((int) (this.mBackgroundAlpha * 255.0f));
        }
    }

    public float getBackgroundAlpha() {
        return this.mBackgroundAlpha;
    }

    public void setBackgroundAlpha(float alpha) {
        if (this.mBackgroundAlpha != alpha) {
            this.mBackgroundAlpha = alpha;
            Drawable background = getBackground();
            if (background != null) {
                background.setAlpha((int) (this.mBackgroundAlpha * 255.0f));
            }
        }
    }

    public CellLayoutChildren getCellLayoutChildren() {
        return this.mChildren;
    }

    public View getChildAt(int x, int y) {
        return this.mChildren.getChildAt(x, y);
    }

    public boolean animateChildToPosition(View child, int cellX, int cellY, int duration, int delay, boolean permanent, boolean adjustOccupied, boolean[][] tempOccupied) {
        return animateChildToPosition(child, cellX, cellY, duration, delay, permanent, adjustOccupied, tempOccupied, 0, null);
    }

    public boolean animateChildToPosition(View child, int cellX, int cellY, int duration, int delay, boolean permanent, boolean adjustOccupied, boolean[][] tempOccupied, int deltaX, Runnable runnable) {
        CellLayoutChildren clc = getCellLayoutChildren();
        boolean[][] occupied = this.mOccupied;
        if (!permanent) {
            occupied = tempOccupied;
        }
        if (occupied == null) {
            return false;
        }
        if (clc.indexOfChild(child) == -1) {
            return false;
        }
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        ItemInfo info = (ItemInfo) child.getTag();
        if (this.mReorderAnimators.containsKey(lp)) {
            ((Animator) this.mReorderAnimators.get(lp)).cancel();
            this.mReorderAnimators.remove(lp);
        }
        final int oldX = lp.x;
        final int oldY = lp.y;
        if (adjustOccupied) {
            markCellsForView(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, occupied, false);
            markCellsForView(cellX, cellY, lp.cellHSpan, lp.cellVSpan, occupied, true);
        }
        lp.isLockedToGrid = true;
        if (permanent) {
            lp.cellX = cellX;
            lp.cellY = cellY;
            if (info != null) {
                info.cellX = cellX;
                info.cellY = cellY;
            }
        } else {
            lp.tmpCellX = cellX;
            lp.tmpCellY = cellY;
        }
        clc.setupLp(lp);
        if (child instanceof LauncherAppWidgetHostView) {
            ResizeResult result = ((LauncherAppWidgetHostView) child).getResizeResult();
            if (result != null) {
                lp.width = result.width;
                lp.height = result.height;
            }
        }
        lp.isLockedToGrid = false;
        final int newX = deltaX + lp.x;
        final int newY = lp.y;
        lp.x = oldX;
        lp.y = oldY;
        if (oldX == newX && oldY == newY && runnable == null) {
            lp.isLockedToGrid = true;
            return true;
        }
        ValueAnimator va = LauncherAnimUtils.ofFloat(child, 0.0f, 1.0f);
        va.setDuration((long) duration);
        this.mReorderAnimators.put(lp, va);
        final View view = child;
        va.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float r = ((Float) animation.getAnimatedValue()).floatValue();
                lp.x = (int) (((1.0f - r) * ((float) oldX)) + (((float) newX) * r));
                lp.y = (int) (((1.0f - r) * ((float) oldY)) + (((float) newY) * r));
                view.layout(lp.x, lp.y, lp.x + lp.width, lp.y + lp.height);
            }
        });
        final View view2 = child;
        final Runnable runnable2 = runnable;
        va.addListener(new AnimatorListenerAdapter() {
            boolean cancelled = false;

            public void onAnimationEnd(Animator animation) {
                if (!this.cancelled || CellLayout.this.updateChildIfReorderAnimationCancel()) {
                    lp.isLockedToGrid = true;
                    view2.requestLayout();
                }
                if (CellLayout.this.mReorderAnimators.containsKey(lp)) {
                    CellLayout.this.mReorderAnimators.remove(lp);
                }
                if (runnable2 != null) {
                    runnable2.run();
                }
            }

            public void onAnimationCancel(Animator animation) {
                this.cancelled = true;
            }
        });
        va.setStartDelay((long) delay);
        va.start();
        return true;
    }

    public boolean isReorderAnimating() {
        return !this.mReorderAnimators.isEmpty();
    }

    public void visualizeDropLocation(ItemInfo info, Drawable dragOutline, int cellX, int cellY, int spanX, int spanY, boolean resize) {
        int oldDragCellX = this.mDragCell[0];
        int oldDragCellY = this.mDragCell[1];
        int oldDragSpanX = this.mDragSpan[0];
        int oldDragSpanY = this.mDragSpan[1];
        if (dragOutline != null) {
            if (cellX != oldDragCellX || cellY != oldDragCellY || spanX != oldDragSpanX || spanY != oldDragSpanY) {
                this.mDragCell[0] = cellX;
                this.mDragCell[1] = cellY;
                this.mDragSpan[0] = spanX;
                this.mDragSpan[1] = spanY;
                int[] topLeft = this.mTmpPoint;
                cellToPoint(cellX, cellY, spanX, topLeft);
                int left = topLeft[0];
                int top = topLeft[1];
                int outlineTop = -1;
                int width = dragOutline.getIntrinsicWidth();
                int height = dragOutline.getIntrinsicHeight();
                boolean isAppOrShortcut = info != null && info.isAppOrShortcutType();
                if (isAppOrShortcut) {
                    outlineTop = getContentTop();
                    width = getContentIconSize();
                    height = width;
                }
                if (this.mLandscape && isAppOrShortcut) {
                    top += (this.mCellHeight - dragOutline.getIntrinsicHeight()) / 2;
                    left += this.mIconStartPadding;
                } else {
                    left += (((this.mCellWidth * spanX) + ((spanX - 1) * this.mWidthGap)) - width) / 2;
                    if (outlineTop != -1) {
                        top += outlineTop;
                    } else {
                        top += (((this.mCellHeight * spanY) + ((spanY - 1) * this.mHeightGap)) - height) / 2;
                    }
                }
                int oldIndex = this.mDragOutlineCurrent;
                this.mDragOutlineAnims[oldIndex].animateOut();
                this.mDragOutlineCurrent = (oldIndex + 1) % this.mDragOutlines.length;
                Rect r = this.mDragOutlines[this.mDragOutlineCurrent];
                r.set(left, top, left + width, top + height);
                if (resize) {
                    cellToRect(cellX, cellY, spanX, spanY, r);
                }
                this.mDragOutlineAnims[this.mDragOutlineCurrent].setTag(dragOutline);
                this.mDragOutlineAnims[this.mDragOutlineCurrent].animateIn();
                if (!this.mLauncher.isQuickOptionShowing()) {
                    Talk.INSTANCE.say(getItemPositionDescription(cellX, cellY));
                }
            }
        }
    }

    protected String getItemPositionDescription(int x, int y) {
        return String.format(getContext().getString(R.string.tts_item_dims_format), new Object[]{Integer.valueOf(y + 1), Integer.valueOf(x + 1)});
    }

    public int getContentIconSize() {
        return -1;
    }

    public int getContentTop() {
        return -1;
    }

    public void clearDragOutlines() {
        this.mDragOutlineAnims[this.mDragOutlineCurrent].animateOut();
        int[] iArr = this.mDragCell;
        this.mDragCell[1] = -1;
        iArr[0] = -1;
        iArr = this.mDragSpan;
        this.mDragSpan[1] = -1;
        iArr[0] = -1;
    }

    public int[] findNearestVacantArea(int pixelX, int pixelY, int spanX, int spanY, int[] result) {
        return findNearestVacantArea(pixelX, pixelY, spanX, spanY, spanX, spanY, result, null);
    }

    public int[] findNearestVacantArea(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX, int spanY, int[] result, int[] resultSpan) {
        return findNearestArea(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY, true, result, resultSpan);
    }

    private void lazyInitTempRectStack() {
        if (this.mTempRectStack.isEmpty()) {
            for (int i = 0; i < this.mCountX * this.mCountY; i++) {
                this.mTempRectStack.push(new Rect());
            }
        }
    }

    private void recycleTempRects(Stack<Rect> used) {
        while (!used.isEmpty()) {
            this.mTempRectStack.push(used.pop());
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int[] findNearestArea(int r31, int r32, int r33, int r34, int r35, int r36, boolean r37, int[] r38, int[] r39) {
        /*
        r30 = this;
        r30.lazyInitTempRectStack();
        r26 = com.android.launcher3.Utilities.sIsRtl;
        if (r26 == 0) goto L_0x0095;
    L_0x0007:
        r0 = r31;
        r0 = (float) r0;
        r26 = r0;
        r0 = r30;
        r0 = r0.mCellWidth;
        r27 = r0;
        r0 = r30;
        r0 = r0.mWidthGap;
        r28 = r0;
        r27 = r27 + r28;
        r28 = r35 + -1;
        r27 = r27 * r28;
        r0 = r27;
        r0 = (float) r0;
        r27 = r0;
        r28 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        r27 = r27 / r28;
        r26 = r26 + r27;
        r0 = r26;
        r0 = (int) r0;
        r31 = r0;
    L_0x002e:
        r0 = r32;
        r0 = (float) r0;
        r26 = r0;
        r0 = r30;
        r0 = r0.mCellHeight;
        r27 = r0;
        r0 = r30;
        r0 = r0.mHeightGap;
        r28 = r0;
        r27 = r27 + r28;
        r28 = r36 + -1;
        r27 = r27 * r28;
        r0 = r27;
        r0 = (float) r0;
        r27 = r0;
        r28 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        r27 = r27 / r28;
        r26 = r26 - r27;
        r0 = r26;
        r0 = (int) r0;
        r32 = r0;
        if (r38 == 0) goto L_0x00be;
    L_0x0057:
        r7 = r38;
    L_0x0059:
        r4 = 9218868437227405311; // 0x7fefffffffffffff float:NaN double:1.7976931348623157E308;
        r6 = new android.graphics.Rect;
        r26 = -1;
        r27 = -1;
        r28 = -1;
        r29 = -1;
        r0 = r26;
        r1 = r27;
        r2 = r28;
        r3 = r29;
        r6.<init>(r0, r1, r2, r3);
        r21 = new java.util.Stack;
        r21.<init>();
        r0 = r30;
        r10 = r0.mCountX;
        r0 = r30;
        r11 = r0.mCountY;
        if (r33 <= 0) goto L_0x0094;
    L_0x0082:
        if (r34 <= 0) goto L_0x0094;
    L_0x0084:
        if (r35 <= 0) goto L_0x0094;
    L_0x0086:
        if (r36 <= 0) goto L_0x0094;
    L_0x0088:
        r0 = r35;
        r1 = r33;
        if (r0 < r1) goto L_0x0094;
    L_0x008e:
        r0 = r36;
        r1 = r34;
        if (r0 >= r1) goto L_0x00c5;
    L_0x0094:
        return r7;
    L_0x0095:
        r0 = r31;
        r0 = (float) r0;
        r26 = r0;
        r0 = r30;
        r0 = r0.mCellWidth;
        r27 = r0;
        r0 = r30;
        r0 = r0.mWidthGap;
        r28 = r0;
        r27 = r27 + r28;
        r28 = r35 + -1;
        r27 = r27 * r28;
        r0 = r27;
        r0 = (float) r0;
        r27 = r0;
        r28 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        r27 = r27 / r28;
        r26 = r26 - r27;
        r0 = r26;
        r0 = (int) r0;
        r31 = r0;
        goto L_0x002e;
    L_0x00be:
        r26 = 2;
        r0 = r26;
        r7 = new int[r0];
        goto L_0x0059;
    L_0x00c5:
        r24 = 0;
    L_0x00c7:
        r26 = r34 + -1;
        r26 = r11 - r26;
        r0 = r24;
        r1 = r26;
        if (r0 >= r1) goto L_0x024f;
    L_0x00d1:
        r22 = 0;
    L_0x00d3:
        r26 = r33 + -1;
        r26 = r10 - r26;
        r0 = r22;
        r1 = r26;
        if (r0 >= r1) goto L_0x024b;
    L_0x00dd:
        r25 = -1;
        r23 = -1;
        if (r37 == 0) goto L_0x01b7;
    L_0x00e3:
        r17 = 0;
    L_0x00e5:
        r0 = r17;
        r1 = r33;
        if (r0 >= r1) goto L_0x010c;
    L_0x00eb:
        r19 = 0;
    L_0x00ed:
        r0 = r19;
        r1 = r34;
        if (r0 >= r1) goto L_0x0109;
    L_0x00f3:
        r0 = r30;
        r0 = r0.mOccupied;
        r26 = r0;
        r27 = r22 + r17;
        r26 = r26[r27];
        r27 = r24 + r19;
        r26 = r26[r27];
        if (r26 == 0) goto L_0x0106;
    L_0x0103:
        r22 = r22 + 1;
        goto L_0x00d3;
    L_0x0106:
        r19 = r19 + 1;
        goto L_0x00ed;
    L_0x0109:
        r17 = r17 + 1;
        goto L_0x00e5;
    L_0x010c:
        r23 = r33;
        r25 = r34;
        r18 = 1;
        r0 = r23;
        r1 = r35;
        if (r0 < r1) goto L_0x014f;
    L_0x0118:
        r13 = 1;
    L_0x0119:
        r0 = r25;
        r1 = r36;
        if (r0 < r1) goto L_0x0151;
    L_0x011f:
        r16 = 1;
    L_0x0121:
        if (r13 == 0) goto L_0x0125;
    L_0x0123:
        if (r16 != 0) goto L_0x01a8;
    L_0x0125:
        if (r18 == 0) goto L_0x0171;
    L_0x0127:
        if (r13 != 0) goto L_0x0171;
    L_0x0129:
        r19 = 0;
    L_0x012b:
        r0 = r19;
        r1 = r25;
        if (r0 >= r1) goto L_0x0154;
    L_0x0131:
        r26 = r22 + r23;
        r27 = r10 + -1;
        r0 = r26;
        r1 = r27;
        if (r0 > r1) goto L_0x014b;
    L_0x013b:
        r0 = r30;
        r0 = r0.mOccupied;
        r26 = r0;
        r27 = r22 + r23;
        r26 = r26[r27];
        r27 = r24 + r19;
        r26 = r26[r27];
        if (r26 == 0) goto L_0x014c;
    L_0x014b:
        r13 = 1;
    L_0x014c:
        r19 = r19 + 1;
        goto L_0x012b;
    L_0x014f:
        r13 = 0;
        goto L_0x0119;
    L_0x0151:
        r16 = 0;
        goto L_0x0121;
    L_0x0154:
        if (r13 != 0) goto L_0x0158;
    L_0x0156:
        r23 = r23 + 1;
    L_0x0158:
        r0 = r23;
        r1 = r35;
        if (r0 < r1) goto L_0x019f;
    L_0x015e:
        r26 = 1;
    L_0x0160:
        r13 = r13 | r26;
        r0 = r25;
        r1 = r36;
        if (r0 < r1) goto L_0x01a2;
    L_0x0168:
        r26 = 1;
    L_0x016a:
        r16 = r16 | r26;
        if (r18 != 0) goto L_0x01a5;
    L_0x016e:
        r18 = 1;
    L_0x0170:
        goto L_0x0121;
    L_0x0171:
        if (r16 != 0) goto L_0x0158;
    L_0x0173:
        r17 = 0;
    L_0x0175:
        r0 = r17;
        r1 = r23;
        if (r0 >= r1) goto L_0x019a;
    L_0x017b:
        r26 = r24 + r25;
        r27 = r11 + -1;
        r0 = r26;
        r1 = r27;
        if (r0 > r1) goto L_0x0195;
    L_0x0185:
        r0 = r30;
        r0 = r0.mOccupied;
        r26 = r0;
        r27 = r22 + r17;
        r26 = r26[r27];
        r27 = r24 + r25;
        r26 = r26[r27];
        if (r26 == 0) goto L_0x0197;
    L_0x0195:
        r16 = 1;
    L_0x0197:
        r17 = r17 + 1;
        goto L_0x0175;
    L_0x019a:
        if (r16 != 0) goto L_0x0158;
    L_0x019c:
        r25 = r25 + 1;
        goto L_0x0158;
    L_0x019f:
        r26 = 0;
        goto L_0x0160;
    L_0x01a2:
        r26 = 0;
        goto L_0x016a;
    L_0x01a5:
        r18 = 0;
        goto L_0x0170;
    L_0x01a8:
        r18 = 1;
        r0 = r23;
        r1 = r35;
        if (r0 < r1) goto L_0x0248;
    L_0x01b0:
        r13 = 1;
    L_0x01b1:
        r0 = r25;
        r1 = r36;
        if (r0 < r1) goto L_0x01b7;
    L_0x01b7:
        r0 = r30;
        r8 = r0.mTmpPoint;
        r0 = r30;
        r1 = r22;
        r2 = r24;
        r0.cellToCenterPoint(r1, r2, r8);
        r0 = r30;
        r0 = r0.mTempRectStack;
        r26 = r0;
        r26 = r26.isEmpty();
        if (r26 != 0) goto L_0x0103;
    L_0x01d0:
        r0 = r30;
        r0 = r0.mTempRectStack;
        r26 = r0;
        r12 = r26.pop();
        r12 = (android.graphics.Rect) r12;
        r26 = r22 + r23;
        r27 = r24 + r25;
        r0 = r22;
        r1 = r24;
        r2 = r26;
        r3 = r27;
        r12.set(r0, r1, r2, r3);
        r9 = 0;
        r26 = r21.iterator();
    L_0x01f0:
        r27 = r26.hasNext();
        if (r27 == 0) goto L_0x0205;
    L_0x01f6:
        r20 = r26.next();
        r20 = (android.graphics.Rect) r20;
        r0 = r20;
        r27 = r0.contains(r12);
        if (r27 == 0) goto L_0x01f0;
    L_0x0204:
        r9 = 1;
    L_0x0205:
        r0 = r21;
        r0.push(r12);
        r26 = 0;
        r26 = r8[r26];
        r26 = r26 - r31;
        r0 = r26;
        r0 = (double) r0;
        r26 = r0;
        r28 = 1;
        r28 = r8[r28];
        r28 = r28 - r32;
        r0 = r28;
        r0 = (double) r0;
        r28 = r0;
        r14 = java.lang.Math.hypot(r26, r28);
        r26 = (r14 > r4 ? 1 : (r14 == r4 ? 0 : -1));
        if (r26 > 0) goto L_0x022a;
    L_0x0228:
        if (r9 == 0) goto L_0x0230;
    L_0x022a:
        r26 = r12.contains(r6);
        if (r26 == 0) goto L_0x0103;
    L_0x0230:
        r4 = r14;
        r26 = 0;
        r7[r26] = r22;
        r26 = 1;
        r7[r26] = r24;
        if (r39 == 0) goto L_0x0243;
    L_0x023b:
        r26 = 0;
        r39[r26] = r23;
        r26 = 1;
        r39[r26] = r25;
    L_0x0243:
        r6.set(r12);
        goto L_0x0103;
    L_0x0248:
        r13 = 0;
        goto L_0x01b1;
    L_0x024b:
        r24 = r24 + 1;
        goto L_0x00c7;
    L_0x024f:
        r26 = 9218868437227405311; // 0x7fefffffffffffff float:NaN double:1.7976931348623157E308;
        r26 = (r4 > r26 ? 1 : (r4 == r26 ? 0 : -1));
        if (r26 != 0) goto L_0x0264;
    L_0x0258:
        r26 = 0;
        r27 = -1;
        r7[r26] = r27;
        r26 = 1;
        r27 = -1;
        r7[r26] = r27;
    L_0x0264:
        r0 = r30;
        r1 = r21;
        r0.recycleTempRects(r1);
        goto L_0x0094;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.base.view.CellLayout.findNearestArea(int, int, int, int, int, int, boolean, int[], int[]):int[]");
    }

    public int[] findNearestArea(int pixelX, int pixelY, int spanX, int spanY, int[] result) {
        return findNearestArea(pixelX, pixelY, spanX, spanY, spanX, spanY, false, result, null);
    }

    public boolean findCellForSpan(int[] cellXY, int spanX, int spanY, boolean lastPosition) {
        boolean foundCell = false;
        boolean foundLastCell = false;
        int endX = this.mCountX - (spanX - 1);
        int endY = this.mCountY - (spanY - 1);
        int startX = 0;
        int startY = 0;
        int y;
        int x;
        int i;
        int j;
        if (lastPosition) {
            for (y = this.mCountY - 1; y >= 0 && !foundLastCell; y--) {
                for (x = this.mCountX - 1; x >= 0; x--) {
                    if (this.mOccupied[x][y]) {
                        startX = x;
                        startY = y;
                        foundLastCell = true;
                        break;
                    }
                }
            }
            for (y = startY; y < endY && !foundCell; y++) {
                x = startX;
                while (x < endX) {
                    for (i = 0; i < spanX; i++) {
                        j = 0;
                        while (j < spanY) {
                            if (this.mOccupied[x + i][y + j]) {
                                startX = 0;
                                x = (x + i) + 1;
                            } else {
                                j++;
                            }
                        }
                    }
                    if (cellXY != null) {
                        cellXY[0] = x;
                        cellXY[1] = y;
                    }
                    foundCell = true;
                }
            }
        } else {
            for (y = 0; y < endY && !foundCell; y++) {
                x = 0;
                while (x < endX) {
                    for (i = 0; i < spanX; i++) {
                        j = 0;
                        while (j < spanY) {
                            if (this.mOccupied[x + i][y + j]) {
                                x = (x + i) + 1;
                            } else {
                                j++;
                            }
                        }
                    }
                    if (cellXY != null) {
                        cellXY[0] = x;
                        cellXY[1] = y;
                    }
                    foundCell = true;
                }
            }
        }
        return foundCell;
    }

    public void onDragEnter() {
        this.mDragging = true;
    }

    public void onDragExit() {
        if (this.mDragging) {
            this.mDragging = false;
        }
        int[] iArr = this.mDragCell;
        this.mDragCell[1] = -1;
        iArr[0] = -1;
        iArr = this.mDragSpan;
        this.mDragSpan[1] = -1;
        iArr[0] = -1;
        this.mDragOutlineAnims[this.mDragOutlineCurrent].animateOut();
        this.mDragOutlineCurrent = (this.mDragOutlineCurrent + 1) % this.mDragOutlineAnims.length;
    }

    public void onDropChild(View child) {
        if (child != null) {
            ((LayoutParams) child.getLayoutParams()).dropped = true;
            child.requestLayout();
        }
    }

    public void cellToRect(int cellX, int cellY, int cellHSpan, int cellVSpan, Rect resultRect) {
        int tempCellX;
        int cellWidth = this.mCellWidth;
        int cellHeight = this.mCellHeight;
        int widthGap = this.mWidthGap;
        int heightGap = this.mHeightGap;
        int hStartPadding = getPaddingLeft();
        int vStartPadding = getPaddingTop();
        if (Utilities.sIsRtl) {
            tempCellX = (this.mCountX - cellX) - cellHSpan;
        } else {
            tempCellX = cellX;
        }
        int x = hStartPadding + ((cellWidth + widthGap) * tempCellX);
        int y = vStartPadding + ((cellHeight + heightGap) * cellY);
        Rect rect = resultRect;
        rect.set(x, y, x + ((cellHSpan * cellWidth) + ((cellHSpan - 1) * widthGap)), y + ((cellVSpan * cellHeight) + ((cellVSpan - 1) * heightGap)));
    }

    public void clearOccupiedCells() {
        for (int x = 0; x < this.mCountX; x++) {
            for (int y = 0; y < this.mCountY; y++) {
                this.mOccupied[x][y] = false;
            }
        }
    }

    public void markCellsAsOccupiedForView(View view) {
        if (view != null && view.getParent() == this.mChildren) {
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            markCellsForView(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, this.mOccupied, true);
        }
    }

    public void markCellsAsUnoccupiedForView(View view) {
        if (view != null && view.getParent() == this.mChildren) {
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            markCellsForView(lp.cellX, lp.cellY, lp.cellHSpan, lp.cellVSpan, this.mOccupied, false);
        }
    }

    protected void markCellsForView(int cellX, int cellY, int spanX, int spanY, boolean[][] occupied, boolean value) {
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

    public int getDesiredWidth() {
        return ((getPaddingLeft() + getPaddingRight()) + (this.mCountX * this.mCellWidth)) + (Math.max(this.mCountX - 1, 0) * this.mWidthGap);
    }

    public int getDesiredHeight() {
        return ((getPaddingTop() + getPaddingBottom()) + (this.mCountY * this.mCellHeight)) + (Math.max(this.mCountY - 1, 0) * this.mHeightGap);
    }

    public boolean isOccupied(int x, int y) {
        if (x < this.mCountX && y < this.mCountY) {
            return this.mOccupied[x][y];
        }
        throw new RuntimeException("Position exceeds the bound of this CellLayout");
    }

    public android.view.ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    protected boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    protected android.view.ViewGroup.LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public boolean hasTargetView() {
        return this.targetView != null;
    }

    public void setTargetView(View targetView) {
        this.targetView = targetView;
    }

    public boolean[][] getOccupied() {
        return this.mOccupied;
    }

    public void setCrossHairAnimatedVisibility(final int visibility, final boolean animate) {
        if (this.mCrossHairView.isAnimating()) {
            this.mCrossHairView.setRunnable(new Runnable() {
                public void run() {
                    CellLayout.this.setCrossHairAnimatedVisibility(visibility, animate);
                    CellLayout.this.mCrossHairView.setRunnable(null);
                }
            });
            return;
        }
        if (visibility == 0 && this.mCrossHairView.getParent() != this) {
            addCrossHairView();
        }
        this.mCrossHairView.setAnimate(animate);
        if (supportWhiteBg() && WhiteBgManager.isWhiteBg()) {
            changeCrossHairFliter(WhiteBgManager.isWhiteBg());
        } else {
            changeCrossHairFliter(false);
        }
        this.mCrossHairView.setVisibility(visibility);
    }

    protected void addCrossHairView() {
        addView(this.mCrossHairView);
    }

    protected void changeCrossHairFliter(boolean whiteBg) {
        WhiteBgManager.changeColorFilterForBg(this.mLauncher, sCrosshairsDrawable, whiteBg);
    }

    public View getChildOnPageAt(int i) {
        return this.mChildren.getChildAt(i);
    }

    public int getPageChildCount() {
        return this.mChildren.getChildCount();
    }

    protected boolean updateChildIfReorderAnimationCancel() {
        return false;
    }

    protected boolean supportWhiteBg() {
        return true;
    }

    protected int getTopPaddingCustomPage() {
        return 0;
    }

    public void callRefreshLiveIcon() {
    }

    public void markCellsAsOccupiedForAllChild() {
        int count = this.mChildren.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = this.mChildren.getChildAt(i);
            if (child != null) {
                markCellsAsOccupiedForView(child);
            }
        }
    }
}

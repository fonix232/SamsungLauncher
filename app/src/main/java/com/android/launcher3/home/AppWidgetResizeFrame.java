package com.android.launcher3.home;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.drag.DragState;
import com.android.launcher3.common.model.DefaultLayoutParser;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.home.LauncherAppWidgetHostView.ResizeResult;
import com.android.launcher3.util.Talk;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;

public class AppWidgetResizeFrame extends FrameLayout {
    static final boolean DEBUG_FRAME = true;
    private static final float DIMMED_HANDLE_ALPHA = 0.0f;
    private static final float RESIZE_THRESHOLD = 0.66f;
    private static final int SNAP_DURATION = 150;
    private static final String TAG = "AppWidgetResizeFrame";
    private Drawable mBackground;
    private Drawable mBackgroundInvalid;
    private final int mBackgroundPadding;
    private int mBaselineHeight;
    private int mBaselineWidth;
    private int mBaselineX;
    private int mBaselineY;
    private final int mBorderOffset;
    private boolean mBottomBorderActive;
    private final ImageView mBottomHandle;
    private int mBottomTouchRegionAdjustment = 0;
    private final WorkspaceCellLayout mCellLayout;
    private int mDeltaX;
    private int mDeltaXAddOn;
    private int mDeltaY;
    private int mDeltaYAddOn;
    private final int[] mDirectionVector = new int[2];
    private final DragLayer mDragLayer;
    private boolean mForceInvalid = false;
    private final int mHandleWidth;
    private boolean mIsInvalidArea = false;
    private final int[] mLastDirectionVector = new int[2];
    private final Launcher mLauncher;
    private boolean mLeftBorderActive;
    private final ImageView mLeftHandle;
    private int mMinHSpan;
    private int mMinVSpan;
    private WorkspaceReorderController mReorderController;
    private int mResizeMode;
    private boolean mRightBorderActive;
    private final ImageView mRightHandle;
    private int mRunningHInc;
    private int mRunningVInc;
    private final int[] mTmpPt = new int[2];
    private boolean mTopBorderActive;
    private final ImageView mTopHandle;
    private int mTopTouchRegionAdjustment = 0;
    private final Rect mWidgetPadding;
    private String mWidgetSpanFormat;
    private final LauncherAppWidgetHostView mWidgetView;

    public AppWidgetResizeFrame(Context context, DragState dragState, LauncherAppWidgetHostView widgetView, CellLayout cellLayout, DragLayer dragLayer) {
        super(context);
        this.mLauncher = (Launcher) context;
        this.mCellLayout = (WorkspaceCellLayout) cellLayout;
        this.mWidgetView = widgetView;
        LauncherAppWidgetProviderInfo info = (LauncherAppWidgetProviderInfo) widgetView.getAppWidgetInfo();
        this.mResizeMode = info.resizeMode();
        this.mDragLayer = dragLayer;
        this.mMinHSpan = info.getMinSpanX();
        this.mMinVSpan = info.getMinSpanY();
        Resources res = getResources();
        this.mBackground = res.getDrawable(R.drawable.widget_resize_frame, null);
        this.mBackgroundInvalid = res.getDrawable(R.drawable.widget_resize_frame_invalid, null);
        this.mBackgroundPadding = res.getDimensionPixelSize(R.dimen.resize_frame_background_padding);
        this.mBorderOffset = res.getDimensionPixelSize(R.dimen.resize_frame_border_offset);
        setPadding(this.mBackgroundPadding, this.mBackgroundPadding, this.mBackgroundPadding, this.mBackgroundPadding);
        float handleSizeFactor = res.getFraction(R.fraction.widget_resize_handle_size_factor, 1, 1);
        if (Talk.INSTANCE.isTouchExplorationEnabled()) {
            this.mHandleWidth = (int) (((float) res.getDrawable(R.drawable.widget_resize_handle, null).getIntrinsicWidth()) * handleSizeFactor);
        } else {
            this.mHandleWidth = res.getDrawable(R.drawable.widget_resize_handle, null).getIntrinsicWidth();
        }
        this.mWidgetSpanFormat = res.getString(R.string.talkback_widget_dims_format);
        String resizeStr = res.getString(R.string.tts_resize);
        String sizeStr = String.format(this.mWidgetSpanFormat, new Object[]{Integer.valueOf(info.getSpanX()), Integer.valueOf(info.getSpanY())});
        this.mLeftHandle = new ImageView(context);
        this.mLeftHandle.setImageResource(R.drawable.widget_resize_handle);
        int handleSize = Talk.INSTANCE.isTouchExplorationEnabled() ? (int) (((float) this.mLeftHandle.getDrawable().getIntrinsicWidth()) * handleSizeFactor) : -2;
        LayoutParams lp = new LayoutParams(handleSize, handleSize, 8388627);
        this.mLeftHandle.setContentDescription(resizeStr + ", " + res.getString(R.string.tts_left_handle) + ", " + sizeStr);
        this.mLeftHandle.setFocusable(true);
        addView(this.mLeftHandle, lp);
        this.mRightHandle = new ImageView(context);
        this.mRightHandle.setImageResource(R.drawable.widget_resize_handle);
        lp = new LayoutParams(handleSize, handleSize, 8388629);
        this.mRightHandle.setContentDescription(resizeStr + ", " + res.getString(R.string.tts_right_handle) + ", " + sizeStr);
        this.mRightHandle.setFocusable(true);
        addView(this.mRightHandle, lp);
        this.mTopHandle = new ImageView(context);
        this.mTopHandle.setImageResource(R.drawable.widget_resize_handle);
        lp = new LayoutParams(handleSize, handleSize, 49);
        this.mTopHandle.setContentDescription(resizeStr + ", " + res.getString(R.string.tts_top_handle) + ", " + sizeStr);
        this.mTopHandle.setFocusable(true);
        addView(this.mTopHandle, lp);
        this.mBottomHandle = new ImageView(context);
        this.mBottomHandle.setImageResource(R.drawable.widget_resize_handle);
        lp = new LayoutParams(handleSize, handleSize, 81);
        this.mBottomHandle.setContentDescription(resizeStr + ", " + res.getString(R.string.tts_bottom_handle) + ", " + sizeStr);
        this.mBottomHandle.setFocusable(true);
        addView(this.mBottomHandle, lp);
        this.mWidgetPadding = DeviceProfile.getPaddingForWidget();
        if (this.mResizeMode == 1) {
            this.mTopHandle.setVisibility(View.GONE);
            this.mBottomHandle.setVisibility(View.GONE);
        } else if (this.mResizeMode == 2) {
            this.mLeftHandle.setVisibility(View.GONE);
            this.mRightHandle.setVisibility(View.GONE);
        }
        if (this.mMinHSpan >= this.mCellLayout.getCountX()) {
            this.mLeftHandle.setVisibility(View.GONE);
            this.mRightHandle.setVisibility(View.GONE);
        }
        if (this.mMinVSpan >= this.mCellLayout.getCountY()) {
            this.mTopHandle.setVisibility(View.GONE);
            this.mBottomHandle.setVisibility(View.GONE);
        }
        this.mReorderController = new WorkspaceReorderController(dragState);
        this.mReorderController.prepareChildForDrag(this.mCellLayout, this.mWidgetView);
        this.mReorderController.setReorderTarget(this.mCellLayout);
        setOnLongClickListener(this.mLauncher.getHomeController());
        setContentDescription(getContext().getString(R.string.tts_resize_widget, new Object[]{info.label}));
    }

    public boolean beginResizeIfPointInRegion(int x, int y) {
        boolean z;
        boolean anyBordersActive;
        float f = 1.0f;
        boolean horizontalActive;
        if ((this.mResizeMode & 1) != 0) {
            horizontalActive = true;
        } else {
            horizontalActive = false;
        }
        boolean verticalActive;
        if ((this.mResizeMode & 2) != 0) {
            verticalActive = true;
        } else {
            verticalActive = false;
        }
        int touchArea = ((int) (((float) this.mHandleWidth) * this.mWidgetView.getResizeResult().scaleToResize)) + (this.mBackgroundPadding * 2);
        if (x >= touchArea || !horizontalActive) {
            z = false;
        } else {
            z = true;
        }
        this.mLeftBorderActive = z;
        if (x <= getWidth() - touchArea || !horizontalActive) {
            z = false;
        } else {
            z = true;
        }
        this.mRightBorderActive = z;
        if (y >= this.mTopTouchRegionAdjustment + touchArea || !verticalActive) {
            z = false;
        } else {
            z = true;
        }
        this.mTopBorderActive = z;
        if (y <= (getHeight() - touchArea) + this.mBottomTouchRegionAdjustment || !verticalActive) {
            z = false;
        } else {
            z = true;
        }
        this.mBottomBorderActive = z;
        if (this.mLeftBorderActive || this.mRightBorderActive || this.mTopBorderActive || this.mBottomBorderActive) {
            anyBordersActive = true;
        } else {
            anyBordersActive = false;
        }
        this.mBaselineWidth = getMeasuredWidth();
        this.mBaselineHeight = getMeasuredHeight();
        this.mBaselineX = getLeft();
        this.mBaselineY = getTop();
        if (anyBordersActive) {
            float f2;
            ImageView imageView = this.mLeftHandle;
            if (this.mLeftBorderActive) {
                f2 = 1.0f;
            } else {
                f2 = 0.0f;
            }
            imageView.setAlpha(f2);
            imageView = this.mRightHandle;
            if (this.mRightBorderActive) {
                f2 = 1.0f;
            } else {
                f2 = 0.0f;
            }
            imageView.setAlpha(f2);
            imageView = this.mTopHandle;
            if (this.mTopBorderActive) {
                f2 = 1.0f;
            } else {
                f2 = 0.0f;
            }
            imageView.setAlpha(f2);
            ImageView imageView2 = this.mBottomHandle;
            if (!this.mBottomBorderActive) {
                f = 0.0f;
            }
            imageView2.setAlpha(f);
        }
        if (LauncherFeature.supportQuickOption() && this.mLauncher.getDragMgr().isQuickOptionShowing()) {
            this.mLauncher.getDragMgr().removeQuickOptionView();
        }
        return anyBordersActive;
    }

    private void updateDeltas(int deltaX, int deltaY) {
        int[] r = this.mCellLayout.spanToPixel(this.mMinHSpan, this.mMinVSpan);
        int minWidth = (int) (((float) r[0]) * this.mWidgetView.getResizeResult().scaleToResize);
        int minHeight = (int) (((float) r[1]) * this.mWidgetView.getResizeResult().scaleToResize);
        boolean left = false;
        boolean top = false;
        if (this.mLeftBorderActive) {
            this.mDeltaX = Math.max(-this.mBaselineX, deltaX);
            this.mDeltaX = Math.min(this.mBaselineWidth - minWidth, this.mDeltaX);
            if (this.mBaselineWidth - deltaX <= minWidth) {
                left = true;
            }
        } else if (this.mRightBorderActive) {
            this.mDeltaX = Math.min(this.mDragLayer.getWidth() - (this.mBaselineX + this.mBaselineWidth), deltaX);
            this.mDeltaX = Math.max((-this.mBaselineWidth) + minWidth, this.mDeltaX);
            if (this.mBaselineWidth + deltaX <= minWidth) {
                left = true;
            }
        }
        if (this.mTopBorderActive) {
            this.mDeltaY = Math.max(-this.mBaselineY, deltaY);
            this.mDeltaY = Math.min(this.mBaselineHeight - minHeight, this.mDeltaY);
            if (this.mBaselineHeight - deltaY <= minHeight) {
                top = true;
            }
        } else if (this.mBottomBorderActive) {
            this.mDeltaY = Math.min(this.mDragLayer.getHeight() - (this.mBaselineY + this.mBaselineHeight), deltaY);
            this.mDeltaY = Math.max((-this.mBaselineHeight) + minHeight, this.mDeltaY);
            if (this.mBaselineHeight + deltaY <= minHeight) {
                top = true;
            }
        }
        if (left || top) {
            this.mForceInvalid = true;
        } else {
            this.mForceInvalid = false;
        }
    }

    public void visualizeResizeForDelta(int deltaX, int deltaY) {
        visualizeResizeForDelta(deltaX, deltaY, false);
    }

    private void visualizeResizeForDelta(int deltaX, int deltaY, boolean onDismiss) {
        updateDeltas(deltaX, deltaY);
        DragLayer.LayoutParams lp = (DragLayer.LayoutParams) getLayoutParams();
        if (this.mLeftBorderActive) {
            lp.x = this.mBaselineX + this.mDeltaX;
            lp.width = this.mBaselineWidth - this.mDeltaX;
        } else if (this.mRightBorderActive) {
            lp.width = this.mBaselineWidth + this.mDeltaX;
        }
        if (this.mTopBorderActive) {
            lp.y = this.mBaselineY + this.mDeltaY;
            lp.height = this.mBaselineHeight - this.mDeltaY;
        } else if (this.mBottomBorderActive) {
            lp.height = this.mBaselineHeight + this.mDeltaY;
        }
        resizeWidgetIfNeeded(onDismiss);
        requestLayout();
    }

    private void resizeWidgetIfNeeded(boolean onDismiss) {
        float hSpanIncF = ((1.0f * ((float) (this.mDeltaX + this.mDeltaXAddOn))) / ((float) (this.mCellLayout.getCellWidth() + this.mCellLayout.getWidthGap()))) - ((float) this.mRunningHInc);
        float vSpanIncF = ((1.0f * ((float) (this.mDeltaY + this.mDeltaYAddOn))) / ((float) (this.mCellLayout.getCellHeight() + this.mCellLayout.getHeightGap()))) - ((float) this.mRunningVInc);
        int hSpanInc = 0;
        int vSpanInc = 0;
        int cellXInc = 0;
        int cellYInc = 0;
        int countX = this.mCellLayout.getCountX();
        int countY = this.mCellLayout.getCountY();
        if (Math.abs(hSpanIncF) > RESIZE_THRESHOLD) {
            hSpanInc = Math.round(hSpanIncF);
        }
        if (Math.abs(vSpanIncF) > RESIZE_THRESHOLD) {
            vSpanInc = Math.round(vSpanIncF);
        }
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) this.mWidgetView.getLayoutParams();
        int spanX = lp.cellHSpan;
        int spanY = lp.cellVSpan;
        int cellX = lp.useTmpCoords ? lp.tmpCellX : lp.cellX;
        int cellY = lp.useTmpCoords ? lp.tmpCellY : lp.cellY;
        int leftCheck;
        if (Utilities.sIsRtl) {
            leftCheck = countX - (cellX + spanX);
        } else {
            leftCheck = cellX;
        }
        int rightCheck = Utilities.sIsRtl ? cellX : countX - (cellX + spanX);
        if (this.mForceInvalid || ((this.mBottomBorderActive && ((float) (countY - (cellY + spanY))) < vSpanIncF) || ((this.mTopBorderActive && ((float) (-cellY)) > vSpanIncF) || ((this.mLeftBorderActive && ((float) (-leftCheck)) > hSpanIncF) || (this.mRightBorderActive && ((float) rightCheck) < hSpanIncF))))) {
            setVisualInvalid();
        } else {
            setVisualOk();
        }
        if (onDismiss || hSpanInc != 0 || vSpanInc != 0) {
            int hSpanDelta = 0;
            int vSpanDelta = 0;
            if (this.mLeftBorderActive) {
                if (Utilities.sIsRtl) {
                    hSpanInc = Math.min(countX - (cellX + spanX), hSpanInc * -1);
                } else {
                    cellXInc = Math.min(lp.cellHSpan - this.mMinHSpan, Math.max(-cellX, hSpanInc));
                    hSpanInc = Math.min(cellX, hSpanInc * -1);
                }
                hSpanInc = Math.max(-(lp.cellHSpan - this.mMinHSpan), hSpanInc);
                hSpanDelta = -hSpanInc;
            } else if (this.mRightBorderActive) {
                if (Utilities.sIsRtl) {
                    cellXInc = Math.min(lp.cellHSpan - this.mMinHSpan, Math.max(-cellX, -hSpanInc));
                    hSpanInc = Math.min(cellX, hSpanInc);
                } else {
                    hSpanInc = Math.min(countX - (cellX + spanX), hSpanInc);
                }
                hSpanInc = Math.max(-(lp.cellHSpan - this.mMinHSpan), hSpanInc);
                hSpanDelta = hSpanInc;
            }
            if (this.mTopBorderActive) {
                cellYInc = Math.min(lp.cellVSpan - this.mMinVSpan, Math.max(-cellY, vSpanInc));
                vSpanInc = Math.max(-(lp.cellVSpan - this.mMinVSpan), Math.min(cellY, vSpanInc * -1));
                vSpanDelta = -vSpanInc;
            } else if (this.mBottomBorderActive) {
                vSpanInc = Math.max(-(lp.cellVSpan - this.mMinVSpan), Math.min(countY - (cellY + spanY), vSpanInc));
                vSpanDelta = vSpanInc;
            }
            this.mDirectionVector[0] = 0;
            this.mDirectionVector[1] = 0;
            if (this.mLeftBorderActive || this.mRightBorderActive) {
                spanX += hSpanInc;
                cellX += cellXInc;
                if (hSpanDelta != 0) {
                    this.mDirectionVector[0] = this.mLeftBorderActive ? -1 : 1;
                }
            }
            if (this.mTopBorderActive || this.mBottomBorderActive) {
                spanY += vSpanInc;
                cellY += cellYInc;
                if (vSpanDelta != 0) {
                    this.mDirectionVector[1] = this.mTopBorderActive ? -1 : 1;
                }
            }
            if (onDismiss || vSpanDelta != 0 || hSpanDelta != 0) {
                if (onDismiss) {
                    this.mDirectionVector[0] = this.mLastDirectionVector[0];
                    this.mDirectionVector[1] = this.mLastDirectionVector[1];
                } else {
                    this.mLastDirectionVector[0] = this.mDirectionVector[0];
                    this.mLastDirectionVector[1] = this.mDirectionVector[1];
                }
                if (((LauncherAppWidgetProviderInfo) this.mWidgetView.getAppWidgetInfo()).isAvailableSize(spanX, spanY) && this.mReorderController.createAreaForResize(cellX, cellY, spanX, spanY, this.mWidgetView, this.mDirectionVector, onDismiss)) {
                    lp.tmpCellX = cellX;
                    lp.tmpCellY = cellY;
                    lp.cellHSpan = spanX;
                    lp.cellVSpan = spanY;
                    this.mRunningVInc += vSpanDelta;
                    this.mRunningHInc += hSpanDelta;
                    if (!onDismiss) {
                        Talk.INSTANCE.say(String.format(this.mWidgetSpanFormat, new Object[]{Integer.valueOf(lp.cellHSpan), Integer.valueOf(lp.cellVSpan)}));
                        updateWidgetSizeRanges(this.mLauncher, this.mWidgetView, spanX, spanY);
                        Resources res = this.mLauncher.getResources();
                        SALogging.getInstance().insertEventLog(res.getString(R.string.screen_Home_Selected), res.getString(R.string.event_ResizeWidget));
                    }
                } else {
                    setVisualInvalid();
                }
                this.mWidgetView.requestLayout();
            }
        }
    }

    static void updateWidgetSizeRanges(Launcher launcher, AppWidgetHostView widgetView, int spanX, int spanY) {
        if (widgetView != null) {
            Rect widgetSize = getWidgetSizeRanges(launcher, widgetView, spanX, spanY);
            widgetView.updateAppWidgetSize(makeAppWidgetOptions(widgetView), widgetSize.left, widgetSize.top, widgetSize.right, widgetSize.bottom);
        }
    }

    static Bundle makeAppWidgetOptions(AppWidgetHostView widgetView) {
        Bundle opts = new Bundle();
        if (((LauncherAppWidgetHostView) widgetView).supportFlingOption()) {
            opts.putInt("fling", -1);
        }
        if (LauncherFeature.supportGSARoundingFeature() && ((LauncherAppWidgetHostView) widgetView).getIsGSB()) {
            Log.d(TAG, "updateWidgetSizeRanges :google widget opts " + widgetView.getLeft() + "/" + widgetView.getRight() + "/" + widgetView.getTop() + "/" + widgetView.getBottom());
            opts.putString("attached-launcher-identifier", "samsung-dream-launcher");
            opts.putString("requested-widget-style", "cqsb");
            opts.putFloat("widget-screen-bounds-left", (float) widgetView.getLeft());
            opts.putFloat("widget-screen-bounds-top", (float) widgetView.getTop());
            opts.putFloat("widget-screen-bounds-right", (float) widgetView.getRight());
            opts.putFloat("widget-screen-bounds-bottom", (float) widgetView.getBottom());
        }
        return opts;
    }

    public static Rect getWidgetSizeRanges(Context launcher, AppWidgetHostView widgetView, int spanX, int spanY) {
        Rect rect = new Rect();
        DeviceProfile dp = LauncherAppState.getInstance().getDeviceProfile();
        float density = launcher.getResources().getDisplayMetrics().density;
        int cellWidth = dp.homeGrid.getCellWidth();
        int cellHeight = dp.homeGrid.getCellHeight();
        int widthGap = dp.homeGrid.getCellGapX();
        int heightGap = dp.homeGrid.getCellGapY();
        int width = (int) (((float) ((spanX * cellWidth) + ((spanX - 1) * widthGap))) / density);
        int height = (int) (((float) ((spanY * cellHeight) + ((spanY - 1) * heightGap))) / density);
        if (LauncherFeature.supportFlexibleGrid() && widgetView != null) {
            ResizeResult resize = ((LauncherAppWidgetHostView) widgetView).getResizeResult();
            if (resize.width == 0 || resize.height == 0) {
                resize = LauncherAppWidgetHostView.calculateWidgetSize(spanX, spanY, (spanX * cellWidth) + (spanX >= 2 ? (spanX - 1) * widthGap : 0), (spanY * cellHeight) + (spanY >= 2 ? (spanY - 1) * heightGap : 0));
            }
            width = (int) (((float) width) / resize.scaleToResize);
            height = (int) (((float) height) / resize.scaleToResize);
        }
        rect.set(width, height, width, height);
        return rect;
    }

    public void commitResize() {
        resizeWidgetIfNeeded(true);
        requestLayout();
    }

    public void onTouchDown() {
        setHandleDescription();
    }

    public void onTouchUp() {
        int yThreshold = this.mCellLayout.getCellHeight() + this.mCellLayout.getHeightGap();
        this.mDeltaXAddOn = this.mRunningHInc * (this.mCellLayout.getCellWidth() + this.mCellLayout.getWidthGap());
        this.mDeltaYAddOn = this.mRunningVInc * yThreshold;
        this.mDeltaX = 0;
        this.mDeltaY = 0;
        setVisualOk();
        post(new Runnable() {
            public void run() {
                AppWidgetResizeFrame.this.snapToWidget(true);
            }
        });
        Talk.INSTANCE.say(getContext().getString(R.string.tts_widget_resized));
    }

    public void snapToWidget(boolean animate) {
        DragLayer.LayoutParams lp = (DragLayer.LayoutParams) getLayoutParams();
        int handleOffset = this.mHandleWidth / 2;
        int scaledWidth = this.mWidgetView.getWidth();
        int newWidth = ((((handleOffset * 2) + ((int) (((float) scaledWidth) * this.mWidgetView.getResizeResult().scaleToResize))) + (this.mBackgroundPadding * 2)) - this.mWidgetPadding.left) - this.mWidgetPadding.right;
        int newHeight = ((((handleOffset * 2) + ((int) (((float) this.mWidgetView.getHeight()) * this.mWidgetView.getResizeResult().scaleToResize))) + (this.mBackgroundPadding * 2)) - this.mWidgetPadding.top) - this.mWidgetPadding.bottom;
        this.mTmpPt[0] = this.mWidgetView.getLeft();
        this.mTmpPt[1] = this.mWidgetView.getTop();
        this.mDragLayer.getDescendantCoordRelativeToSelf(this.mCellLayout.getCellLayoutChildren(), this.mTmpPt);
        int newX = ((this.mTmpPt[0] - handleOffset) - this.mBackgroundPadding) + this.mWidgetPadding.left;
        int newY = ((this.mTmpPt[1] - handleOffset) - this.mBackgroundPadding) + this.mWidgetPadding.top;
        if (newY < 0) {
            this.mTopTouchRegionAdjustment = -newY;
        } else {
            this.mTopTouchRegionAdjustment = 0;
        }
        if (newY + newHeight > this.mDragLayer.getHeight()) {
            this.mBottomTouchRegionAdjustment = -((newY + newHeight) - this.mDragLayer.getHeight());
        } else {
            this.mBottomTouchRegionAdjustment = 0;
        }
        if (animate) {
            PropertyValuesHolder width = PropertyValuesHolder.ofInt("width", new int[]{lp.width, newWidth});
            PropertyValuesHolder height = PropertyValuesHolder.ofInt("height", new int[]{lp.height, newHeight});
            PropertyValuesHolder x = PropertyValuesHolder.ofInt(DefaultLayoutParser.ATTR_X, new int[]{lp.x, newX});
            PropertyValuesHolder y = PropertyValuesHolder.ofInt(DefaultLayoutParser.ATTR_Y, new int[]{lp.y, newY});
            ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(lp, this, width, height, x, y);
            ObjectAnimator leftOa = LauncherAnimUtils.ofFloat(this.mLeftHandle, "alpha", 1.0f);
            ObjectAnimator rightOa = LauncherAnimUtils.ofFloat(this.mRightHandle, "alpha", 1.0f);
            ObjectAnimator topOa = LauncherAnimUtils.ofFloat(this.mTopHandle, "alpha", 1.0f);
            ObjectAnimator bottomOa = LauncherAnimUtils.ofFloat(this.mBottomHandle, "alpha", 1.0f);
            oa.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    AppWidgetResizeFrame.this.requestLayout();
                }
            });
            AnimatorSet set = LauncherAnimUtils.createAnimatorSet();
            if (this.mResizeMode == 2) {
                set.playTogether(new Animator[]{oa, topOa, bottomOa});
            } else if (this.mResizeMode == 1) {
                set.playTogether(new Animator[]{oa, leftOa, rightOa});
            } else {
                set.playTogether(new Animator[]{oa, leftOa, rightOa, topOa, bottomOa});
            }
            set.setDuration(150);
            set.start();
            return;
        }
        lp.width = newWidth;
        lp.height = newHeight;
        lp.x = newX;
        lp.y = newY;
        this.mLeftHandle.setAlpha(1.0f);
        this.mRightHandle.setAlpha(1.0f);
        this.mTopHandle.setAlpha(1.0f);
        this.mBottomHandle.setAlpha(1.0f);
        requestLayout();
    }

    protected void dispatchDraw(Canvas canvas) {
        Drawable d;
        DragLayer.LayoutParams lp = (DragLayer.LayoutParams) getLayoutParams();
        int margin = (this.mBackgroundPadding + (this.mHandleWidth / 2)) - this.mBorderOffset;
        if (this.mIsInvalidArea) {
            d = this.mBackgroundInvalid;
        } else {
            d = this.mBackground;
        }
        d.setBounds(margin, margin, lp.width - margin, lp.height - margin);
        d.draw(canvas);
        super.dispatchDraw(canvas);
    }

    private void setVisualOk() {
        this.mIsInvalidArea = false;
        this.mBottomHandle.setImageResource(R.drawable.widget_resize_handle);
        this.mTopHandle.setImageResource(R.drawable.widget_resize_handle);
        this.mLeftHandle.setImageResource(R.drawable.widget_resize_handle);
        this.mRightHandle.setImageResource(R.drawable.widget_resize_handle);
    }

    private void setVisualInvalid() {
        this.mIsInvalidArea = true;
        this.mBottomHandle.setImageResource(R.drawable.widget_resize_handle_invalid);
        this.mTopHandle.setImageResource(R.drawable.widget_resize_handle_invalid);
        this.mLeftHandle.setImageResource(R.drawable.widget_resize_handle_invalid);
        this.mRightHandle.setImageResource(R.drawable.widget_resize_handle_invalid);
    }

    LauncherAppWidgetHostView getResizeWidgetView() {
        return this.mWidgetView;
    }

    private void setHandleDescription() {
        String handle;
        if (this.mLeftBorderActive) {
            handle = getContext().getString(R.string.tts_left_handle);
        } else if (this.mRightBorderActive) {
            handle = getContext().getString(R.string.tts_right_handle);
        } else if (this.mTopBorderActive) {
            handle = getContext().getString(R.string.tts_top_handle);
        } else {
            handle = getContext().getString(R.string.tts_bottom_handle);
        }
        Talk.INSTANCE.say(getContext().getString(R.string.tts_long_pressd_widget_handle, new Object[]{handle}));
    }
}

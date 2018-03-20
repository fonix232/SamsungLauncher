package com.android.launcher3.util.event;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.view.LauncherRootView;
import com.android.launcher3.common.view.ScrollHelperByRootView;
import com.android.launcher3.common.view.ScrollHelperByRootView.ScrollTouchListener;

public class ScrollDeterminator implements ScrollTouchListener {
    private static final float BOUNDARY = 1.0f;
    private static final int DIRECTION_HORIZONTAL = 1;
    private static final int DIRECTION_MOVE_ON_BLOCK = -2;
    private static final int DIRECTION_UNINSPEDTED = -1;
    private static final int DIRECTION_VERTICAL = 0;
    private static final int LOCKED_COUNT = 10;
    private float mAverageAcceleration;
    private Rect mBlockArea;
    private boolean mEnableHScroll;
    private boolean mIsBlocked;
    private boolean mIsTalkBackEnabled;
    private ScrollHelperByRootView mScrollHelperByRootView;
    private SlopCompensation mSlopCompensation;
    private TouchSlopHelper mTouchSlopHelper;

    private static class SlopCompensation {
        static final float WEIGHT_OFFSET = 0.2f;
        static final float WEIGHT_SLOPE = 0.04f;
        float mSlopCompensationX;

        private SlopCompensation() {
            this.mSlopCompensationX = 0.0f;
        }

        int getDeltaXwithCompensation(int deltaTouch) {
            boolean isSameDirection = ((float) deltaTouch) * this.mSlopCompensationX > 0.0f;
            float weight = Math.min(1.0f, Math.max((WEIGHT_SLOPE * ((float) Math.abs(deltaTouch))) - WEIGHT_OFFSET, 0.0f));
            int delta = deltaTouch;
            if (isSameDirection) {
                float compensation = weight < 1.0f ? weight * this.mSlopCompensationX : this.mSlopCompensationX;
                this.mSlopCompensationX -= compensation;
                return deltaTouch + ((int) compensation);
            }
            this.mSlopCompensationX = 0.0f;
            return delta;
        }
    }

    private static class TouchSlopHelper {
        private int mSystemTouchSlop;

        private TouchSlopHelper() {
            this.mSystemTouchSlop = 0;
        }

        void setSystemTouchSlop(int pagedViewTouchSlop) {
            this.mSystemTouchSlop = pagedViewTouchSlop;
        }

        int getSystemTouchSlop() {
            return this.mSystemTouchSlop;
        }
    }

    public ScrollDeterminator() {
        this.mAverageAcceleration = 0.0f;
        this.mIsBlocked = false;
        this.mEnableHScroll = true;
        this.mIsTalkBackEnabled = true;
        this.mBlockArea = new Rect(0, 0, 0, 0);
        this.mTouchSlopHelper = new TouchSlopHelper();
        this.mScrollHelperByRootView = null;
        this.mSlopCompensation = new SlopCompensation();
        this.mScrollHelperByRootView = LauncherRootView.sScrollHelperByRootView;
    }

    ScrollHelperByRootView getScrollHelper() {
        if (this.mScrollHelperByRootView != null) {
            this.mScrollHelperByRootView = LauncherRootView.sScrollHelperByRootView;
        }
        return this.mScrollHelperByRootView;
    }

    public void registrateController(int controller) {
        this.mScrollHelperByRootView.addListener(controller, this);
    }

    public boolean isLocked() {
        return getScrollHelper() != null && getScrollHelper().getCount() >= 10;
    }

    public int getCountTouchMove() {
        return getScrollHelper() != null ? getScrollHelper().getCount() : 0;
    }

    public void setForceBlock() {
        this.mIsBlocked = true;
    }

    public boolean isHorizontalScroll() {
        return getDirection() == 1;
    }

    public boolean isVerticalScroll() {
        return getDirection() == 0;
    }

    public boolean isVerticalScrollWithSlop() {
        return isVerticalScrollWithThreshold(this.mTouchSlopHelper.getSystemTouchSlop());
    }

    public boolean isVerticalScrollWithThreshold(int threshold) {
        return isVerticalScroll() && Math.abs(this.mScrollHelperByRootView.getYDistanceFromPress()) > ((float) threshold);
    }

    public boolean isMovingOnBlock() {
        return getDirection() == -2;
    }

    private float getAverageAccelaration() {
        if (isLocked() || getScrollHelper() == null) {
            return this.mAverageAcceleration;
        }
        float averageAccelaration = getScrollHelper().getAverageAccelaration();
        this.mAverageAcceleration = averageAccelaration;
        return averageAccelaration;
    }

    public int getDirection() {
        if (getScrollHelper() == null) {
            return -1;
        }
        float absAccelaration = Math.abs(getAverageAccelaration());
        if (absAccelaration == 0.0f) {
            return -1;
        }
        if (this.mIsBlocked) {
            return -2;
        }
        return absAccelaration > 1.0f ? 0 : 1;
    }

    boolean setScrollableView(PagedView pagedView) {
        if (pagedView != null && pagedView.getPageCount() <= 1) {
            this.mEnableHScroll = false;
        }
        return this.mEnableHScroll;
    }

    public boolean setScrollableView(boolean scrollable) {
        return scrollable;
    }

    private void setScrollableId() {
        if (!this.mIsTalkBackEnabled && this.mScrollHelperByRootView.getScrollId() > 0) {
            this.mIsBlocked = true;
        }
    }

    public boolean setBlockArea(PagedView pagedView, float inputX, float inputY) {
        if (pagedView == null || pagedView.getChildCount() <= 0) {
            this.mBlockArea.right = 0;
            this.mBlockArea.bottom = 0;
        } else {
            setBlockArea(pagedView.getChildAt(0));
            System.out.println(this.mBlockArea);
            this.mIsBlocked = isBlockedArea(inputX, inputY);
        }
        return this.mIsBlocked;
    }

    private void setBlockArea(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        this.mBlockArea.left = 0;
        this.mBlockArea.top = 0;
        this.mBlockArea.right = LauncherAppState.getInstance().getDeviceProfile().getAvailableWidthPx();
        DeviceProfile dp = LauncherAppState.getInstance().getDeviceProfile();
        if (!dp.isMultiwindowMode || dp.isLandscape) {
            this.mBlockArea.bottom = location[1];
            return;
        }
        this.mBlockArea.bottom = location[1] - dp.getMultiWindowPanelSize();
    }

    private boolean isBlockedArea(float x, float y) {
        return this.mBlockArea.contains((int) x, (int) y);
    }

    public int setSystemTouchSlop(Context context) {
        if (context == null) {
            return 0;
        }
        ViewConfiguration configuration = ViewConfiguration.get(context);
        if (configuration != null) {
            this.mTouchSlopHelper.setSystemTouchSlop(configuration.getScaledTouchSlop());
        }
        return this.mTouchSlopHelper.getSystemTouchSlop();
    }

    public boolean cancelLongPressOnHScroll() {
        return (getScrollHelper() == null || this.mEnableHScroll || Math.abs(getScrollHelper().getXDistanceFromPress()) <= ((float) this.mTouchSlopHelper.getSystemTouchSlop())) ? false : true;
    }

    public boolean setTalkBackEnabled(Context context) {
        boolean isTalkBackEnabled = Utilities.isTalkBackEnabled(context);
        this.mIsTalkBackEnabled = isTalkBackEnabled;
        return isTalkBackEnabled;
    }

    public void setSlopCompensation() {
        this.mSlopCompensation.mSlopCompensationX = getScrollHelper().getXDistanceFromPress();
    }

    public int getDeltaXwithCompensation(int delta) {
        return this.mSlopCompensation.getDeltaXwithCompensation(delta);
    }

    public int onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction() & 255) {
            case 0:
                this.mEnableHScroll = true;
                this.mIsBlocked = isBlockedArea(x, y);
                return 0;
            case 2:
                setScrollableId();
                return 2;
            default:
                return event.getAction() & 255;
        }
    }
}

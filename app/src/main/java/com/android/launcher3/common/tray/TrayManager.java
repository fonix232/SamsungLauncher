package com.android.launcher3.common.tray;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewStub;
import android.view.Window;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.drag.DragManager;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.drag.DropTarget;
import com.android.launcher3.util.BlurUtils;
import com.android.launcher3.util.DvfsUtil;
import com.android.launcher3.util.LightingEffectManager;
import com.android.launcher3.util.alarm.Alarm;
import com.android.launcher3.util.alarm.OnAlarmListener;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.android.launcher3.util.event.ScreenDivision;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class TrayManager {
    private static final int BLUR_INTERVAL = 2;
    private static final int BLUR_VALID_DECIMAL_PLACE = 2;
    private static final int DIRECTION_BOTTOM = 1;
    private static final int DIRECTION_NONE = -1;
    private static final int DIRECTION_TOP = 0;
    private static final int FLING_THRESHOLD_VELOCITY = 500;
    private static final int INVALID_POINTER = -1;
    public static final String KEY_SUPPRESS_CHANGE_STAGE_ONCE = "KEY_SUPPRESS_CHANGE_STAGE_ONCE";
    private static final String TAG = "TrayManager";
    private static final int TOUCH_IGNORE_TIME_FOR_NOTIFICATION_PANEL = 300;
    private static final int TRANSITION_BLUR_SLOP_COUNT = 6;
    private static final int UNSET_SUPPRESS_CHANGE_STAGE_DELAY = 400;
    private int mActivePointerId = -1;
    private float mCurrentChangedOffsetY = 0.0f;
    private int mDirection = 0;
    private boolean mDisallowCallBacksVisibity;
    private DragManager mDragManager;
    private FakeViewAnimation mFakeViewAnimation = new FakeViewAnimation();
    private int mFakeViewAnimationTime;
    private DragEventCallback mFakeViewDragEventListener = new DragEventCallback() {
        public void onDragEnter(int direction) {
            if (!TrayManager.this.mSuppressChangeStage) {
                int lightDirection;
                if (direction == 0) {
                    lightDirection = 0;
                } else {
                    lightDirection = 1;
                }
                LightingEffectManager.INSTANCE.turnOnEachLight(lightDirection, true);
            }
        }

        public void onDragExit(int direction) {
            TrayManager.this.mSuppressChangeStage = false;
            if (TrayManager.this.mNeedToShowLightingEffectLater) {
                TrayManager.this.mNeedToShowLightingEffectLater = false;
                TrayManager.this.mTransferPanelTopView.setSuppressChangeStage(false);
                TrayManager.this.mTransferPanelBottomView.setSuppressChangeStage(false);
                TrayManager.this.showLightingEffect();
                return;
            }
            LightingEffectManager.INSTANCE.turnOnEachLight(direction == 0 ? 0 : 1, false);
        }

        public void onChangeStage(TrayLevel targetLevel, int direction) {
            TrayManager.this.changeStageWithDrag(targetLevel, direction, -1);
        }

        public DropTarget getDropTarget(TrayLevel targetLevel) {
            return TrayManager.this.findDropTarget(targetLevel);
        }
    };
    private int mFakeViewHeightToShow;
    private float mFirstDownY = 0.0f;
    private int mFlingThresholdVelocity;
    private boolean mIsExpandedNotiPanel = false;
    private boolean mIsRunningSwipeAnimation;
    private float mLastDownX = 0.0f;
    private float mLastDownY = 0.0f;
    private Launcher mLauncher;
    private int mMaximumVelocity;
    private boolean mMoveAndAnimated = false;
    private boolean mMoved = false;
    private boolean mNeedToShowLightingEffectLater;
    private float mOffset = 0.0f;
    private float mStartOffsetY = 0.0f;
    private boolean mSuppressChangeStage;
    private float mSwipeDistanceRatio = 1.0f;
    private int mTouchSlop;
    private int mTouchStartIndex;
    private float mTouchUpVelocity;
    private boolean mTouching = false;
    private FakeView mTransferPanelBottomView;
    private FakeView mTransferPanelTopView;
    private CopyOnWriteArrayList<TrayInteractionListener> mTrayEventCallbacks = new CopyOnWriteArrayList();
    private int mTrayHeight;
    private int mTraySlipY;
    private int mTraySlipYForNotificationPanel;
    private float mUndergroundBgBlur;
    private final Alarm mUnsetSuppressChangeStageAlarm = new Alarm();
    private final OnAlarmListener mUnsetSuppressChangeStageAlarmListener = new OnAlarmListener() {
        public void onAlarm(Alarm alarm) {
            TrayManager.this.mSuppressChangeStage = false;
            if (TrayManager.this.mNeedToShowLightingEffectLater) {
                boolean isDragEntered = TrayManager.this.mTransferPanelBottomView.isDragEntered() || TrayManager.this.mTransferPanelTopView.isDragEntered();
                if (isDragEntered) {
                    TrayManager.this.mNeedToShowLightingEffectLater = true;
                    return;
                }
                TrayManager.this.mNeedToShowLightingEffectLater = false;
                TrayManager.this.mTransferPanelTopView.setSuppressChangeStage(false);
                TrayManager.this.mTransferPanelBottomView.setSuppressChangeStage(false);
                TrayManager.this.showLightingEffect();
                return;
            }
            TrayManager.this.mTransferPanelTopView.setSuppressChangeStage(false);
            TrayManager.this.mTransferPanelBottomView.setSuppressChangeStage(false);
        }
    };
    private VelocityTracker mVelocityTracker = VelocityTracker.obtain();

    public static class TrayEvent {
        public static final int EVENT_CHANGED_OFFSET_Y = 2;
        public static final int EVENT_CHANGE_STAGE = 10;
        public static final int EVENT_MOVE_END = 5;
        public static final int EVENT_MOVE_START = 4;
        public static final int EVENT_SET_Y_OFFSET = 1;
        public static final int EVENT_TOUCH_UP = 3;
        public boolean mDisallowVisible;
        public final int mEventType;
        public final float mValue;

        TrayEvent(int type, float value) {
            this.mEventType = type;
            this.mValue = value;
        }
    }

    public interface TrayInteractionListener {
        boolean determineStageChange(int i, float f, float f2, float f3, int i2);

        DropTarget getDropTarget();

        ScreenDivision getScreenDivision();

        float getTrayBgBlurAmount();

        TrayLevel getTrayLevel();

        float getTrayScale();

        boolean isMovingOnBlock();

        boolean isOverBlurSlop(int i);

        boolean isScrollLocked();

        boolean isVerticalScroll();

        void onReceiveTrayEvent(TrayEvent trayEvent);

        void onSwipeBlockListener(float f, float f2);

        void requestBlurChange(boolean z, Window window, float f, long j);

        void startTrayMove();
    }

    public enum TrayLevel {
        Overground,
        Underground
    }

    public void setup(Activity activity) {
        setup(activity, null);
    }

    public void setup(Activity activity, DragManager dragMgr) {
        ViewConfiguration configuration = ViewConfiguration.get(activity);
        this.mLauncher = (Launcher) activity;
        this.mDragManager = dragMgr;
        Resources res = activity.getResources();
        int screenHeight = Utilities.getFullScreenHeight(this.mLauncher);
        this.mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        this.mTouchSlop = configuration.getScaledPagingTouchSlop();
        this.mFlingThresholdVelocity = (int) (500.0f * res.getDisplayMetrics().density);
        ViewStub topStub = (ViewStub) activity.findViewById(R.id.tray_fake_view_top_stub);
        ViewStub bottomStub = (ViewStub) activity.findViewById(R.id.tray_fake_view_bottom_stub);
        topStub.setLayoutResource(R.layout.tray_transfer_panel_top);
        this.mTransferPanelTopView = (FakeView) topStub.inflate();
        this.mTransferPanelTopView.setDirection(0);
        this.mTransferPanelTopView.setDragEventListener(this.mFakeViewDragEventListener);
        bottomStub.setLayoutResource(R.layout.tray_transfer_panel_bottom);
        this.mTransferPanelBottomView = (FakeView) bottomStub.inflate();
        this.mTransferPanelBottomView.setDirection(1);
        setBottomViewDragEnable();
        this.mFakeViewHeightToShow = res.getDimensionPixelSize(R.dimen.tray_fake_view_height_to_show);
        this.mFakeViewAnimationTime = res.getInteger(R.integer.config_homeDragTransitionDuration);
        this.mTrayHeight = screenHeight;
        this.mTraySlipY = res.getDimensionPixelSize(R.dimen.tray_slip_y_on_transition_type_3);
        this.mSwipeDistanceRatio = res.getFraction(R.fraction.config_swipeDistanceRatio, 1, 1);
        if (LauncherFeature.supportNotificationPanelExpansionWithHomeMoving()) {
            this.mTraySlipYForNotificationPanel = this.mTraySlipY / 3;
        }
    }

    public void onDestroy() {
        this.mTrayEventCallbacks.clear();
        if (this.mTransferPanelTopView != null) {
            this.mTransferPanelTopView.setDragEventListener(null);
        }
        if (this.mTransferPanelBottomView != null) {
            this.mTransferPanelBottomView.setDragEventListener(null);
        }
    }

    public void addTrayEventCallbacks(TrayInteractionListener cb) {
        if (this.mTrayEventCallbacks != null) {
            this.mTrayEventCallbacks.add(cb);
            if (cb.getTrayLevel() == TrayLevel.Underground) {
                this.mUndergroundBgBlur = cb.getTrayBgBlurAmount();
            }
        }
    }

    public void removeTrayEventCallbacks(TrayInteractionListener cb) {
        if (this.mTrayEventCallbacks != null) {
            this.mTrayEventCallbacks.remove(cb);
        }
    }

    public void onChangeTrayTranslationY(TrayInteractionListener sender, float y, int trayHeight) {
        this.mCurrentChangedOffsetY = y;
        this.mTrayHeight = trayHeight;
        if (this.mTrayEventCallbacks != null) {
            TrayEvent trayEvent = new TrayEvent(2, y);
            trayEvent.mDisallowVisible = this.mDisallowCallBacksVisibity;
            Iterator it = this.mTrayEventCallbacks.iterator();
            while (it.hasNext()) {
                ((TrayInteractionListener) it.next()).onReceiveTrayEvent(trayEvent);
            }
        }
        handleWallpaperBlur(y, sender);
    }

    public void setSuppressChangeStageOnce() {
        Log.i(TAG, "setSuppressChangeStageOnce");
        this.mUnsetSuppressChangeStageAlarm.cancelAlarm();
        this.mUnsetSuppressChangeStageAlarm.setOnAlarmListener(this.mUnsetSuppressChangeStageAlarmListener);
        this.mUnsetSuppressChangeStageAlarm.setAlarm(400);
        this.mSuppressChangeStage = true;
        this.mNeedToShowLightingEffectLater = false;
        LightingEffectManager.INSTANCE.showEffect(false, this.mFakeViewAnimationTime, Utilities.isMobileKeyboardMode());
        this.mTransferPanelTopView.setSuppressChangeStage(true);
        this.mTransferPanelBottomView.setSuppressChangeStage(true);
    }

    public void pullTrayForDrag(TrayInteractionListener sender, int trayHeight) {
        TrayLevel toLevel;
        String descTop;
        String descBottom;
        this.mTrayHeight = trayHeight;
        TrayLevel senderLevel = sender.getTrayLevel();
        boolean isLandscape;
        if (Utilities.getOrientation() == 2) {
            isLandscape = true;
        } else {
            isLandscape = false;
        }
        if (TrayLevel.Overground.equals(senderLevel)) {
            toLevel = TrayLevel.Underground;
            descTop = "";
            descBottom = this.mLauncher.getResources().getString(R.string.move_to_apps);
        } else {
            toLevel = TrayLevel.Overground;
            descTop = this.mLauncher.getResources().getString(R.string.move_to_home);
            descBottom = "";
        }
        this.mTransferPanelTopView.setTrayLevel(toLevel);
        this.mTransferPanelTopView.setDescription(descTop);
        animateFakeView(this.mTransferPanelTopView, 0, true);
        if (!(Utilities.isMobileKeyboardMode() || isLandscape)) {
            this.mTransferPanelBottomView.setTrayLevel(toLevel);
            this.mTransferPanelBottomView.setDescription(descBottom);
            int bottomViewHeight = this.mTransferPanelBottomView.getHeight();
            if (bottomViewHeight == 0) {
                this.mTransferPanelBottomView.measure(0, 0);
                bottomViewHeight = this.mTransferPanelBottomView.getMeasuredHeight();
            }
            animateFakeView(this.mTransferPanelBottomView, trayHeight - bottomViewHeight, true);
        }
        if (this.mSuppressChangeStage) {
            this.mNeedToShowLightingEffectLater = true;
        } else {
            showLightingEffect();
        }
        if (this.mDragManager != null) {
            this.mDragManager.removeDropTarget(this.mTransferPanelTopView);
            this.mDragManager.addDropTarget(this.mTransferPanelTopView);
            if (!Utilities.isMobileKeyboardMode() && !isLandscape) {
                this.mDragManager.removeDropTarget(this.mTransferPanelBottomView);
                this.mDragManager.addDropTarget(this.mTransferPanelBottomView);
            }
        }
    }

    public void releaseTrayForDrag(TrayInteractionListener sender, int trayHeight) {
        this.mTrayHeight = trayHeight;
        this.mUnsetSuppressChangeStageAlarm.cancelAlarm();
        this.mSuppressChangeStage = false;
        this.mNeedToShowLightingEffectLater = false;
        LightingEffectManager.INSTANCE.showEffect(false, this.mFakeViewAnimationTime, Utilities.isMobileKeyboardMode());
        animateFakeView(this.mTransferPanelTopView, 0, false);
        animateFakeView(this.mTransferPanelBottomView, trayHeight - this.mTransferPanelBottomView.getHeight(), false);
        if (this.mDragManager != null) {
            this.mDragManager.removeDropTarget(this.mTransferPanelTopView);
            this.mDragManager.removeDropTarget(this.mTransferPanelBottomView);
        }
    }

    public int getTrayMovingRange() {
        return this.mTraySlipY > 0 ? this.mTraySlipY : Utilities.getFullScreenHeight(this.mLauncher);
    }

    public float getTrayMovingDistance() {
        return this.mOffset;
    }

    public int getHeightOfTrayForDrag() {
        return this.mFakeViewHeightToShow;
    }

    private void animateFakeView(View targetView, int toTranslationY, boolean toBeShown) {
        this.mFakeViewAnimation.animate(targetView, toTranslationY, toBeShown, this.mFakeViewAnimationTime);
    }

    private void handleWallpaperBlur(float offsetY, TrayInteractionListener sender) {
        if (!this.mIsRunningSwipeAnimation) {
            float value;
            int movingRange = getTrayMovingRange();
            boolean isOverBlurSlop = sender == null || sender.isOverBlurSlop(6);
            if (offsetY >= 0.0f) {
                value = offsetY / ((float) movingRange);
            } else {
                value = (-offsetY) / ((float) movingRange);
            }
            value = Math.min(value, 1.0f);
            boolean show = value > 0.0f;
            float blurAmount = Utilities.simplifyDecimalFraction(this.mUndergroundBgBlur * value, 2, 2);
            if ((blurAmount == 0.0f || ((double) blurAmount) > ((double) this.mUndergroundBgBlur) * 0.9d || isOverBlurSlop) && sender != null) {
                sender.requestBlurChange(show, this.mLauncher.getWindow(), blurAmount, show ? 0 : 100);
            }
        }
    }

    public void setSwipeAnimationStatus(boolean isStart, float toTranslation, long animDuration) {
        float f = 0.0f;
        if (isStart) {
            boolean blur = toTranslation != 0.0f;
            if (LauncherFeature.supportBackgroundBlurByWindow()) {
                Window window = this.mLauncher.getWindow();
                if (blur) {
                    f = this.mUndergroundBgBlur;
                }
                BlurUtils.blurByWindowManager(blur, window, f, animDuration);
            }
        }
        this.mIsRunningSwipeAnimation = isStart;
    }

    public boolean isMoving() {
        return this.mMoved;
    }

    public void resetMoving() {
        if (this.mMoved) {
            trayTouchUp(false);
            this.mMoved = false;
            this.mActivePointerId = -1;
        } else if (this.mLauncher.isHomeStage()) {
            setTrayTranslationY(0.0f);
        } else if (this.mLauncher.isAppsStage()) {
            setTrayTranslationY((float) getOffsetLimit(TrayLevel.Overground, -1).top);
        }
    }

    public boolean onInterceptTouchEvent(TrayInteractionListener sender, MotionEvent ev) {
        boolean z = false;
        int action = ev.getActionMasked();
        if (action == 2 && this.mMoved) {
            return true;
        }
        boolean interceptMotion = false;
        switch (action) {
            case 0:
                this.mVelocityTracker.clear();
                this.mVelocityTracker.addMovement(ev);
                new DvfsUtil(this.mLauncher).boostUpForSupportedModel();
                this.mLastDownX = ev.getRawX();
                float rawY = ev.getRawY();
                this.mLastDownY = rawY;
                this.mFirstDownY = rawY;
                this.mOffset = 0.0f;
                this.mMoved = false;
                this.mDirection = 0;
                this.mActivePointerId = ev.getPointerId(0);
                if (sender != null) {
                    sender.onSwipeBlockListener(ev.getX(), ev.getY());
                }
                this.mTouchStartIndex = getScreenDivisionSection(sender, ev);
                this.mIsExpandedNotiPanel = false;
                break;
            case 1:
            case 3:
                this.mMoved = false;
                this.mDirection = 0;
                this.mActivePointerId = -1;
                break;
            case 2:
                this.mVelocityTracker.addMovement(ev);
                if (this.mActivePointerId != -1) {
                    float deltaY = Math.abs(ev.getRawY() - this.mLastDownY);
                    if (sender != null && willMove(deltaY, sender)) {
                        sender.startTrayMove();
                        LauncherAnimUtils.onDestroyActivity();
                        interceptMotion = true;
                        prepareTrayMove(sender, ev, ev.getRawY() - (ev.getY() * sender.getTrayScale()), ev.findPointerIndex(this.mActivePointerId));
                        this.mMoved = true;
                        break;
                    }
                }
                break;
            case 6:
                onSecondaryPointerUp(sender, ev);
                break;
        }
        if (interceptMotion || this.mMoved) {
            z = true;
        }
        return z;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean onTouchEvent(com.android.launcher3.common.tray.TrayManager.TrayInteractionListener r33, android.view.MotionEvent r34) {
        /*
        r32 = this;
        r0 = r32;
        r0 = r0.mVelocityTracker;
        r27 = r0;
        r0 = r27;
        r1 = r34;
        r0.addMovement(r1);
        r5 = r34.getActionMasked();
        r26 = r33.getTrayScale();
        r27 = r34.getRawX();
        r28 = r34.getX();
        r28 = r28 * r26;
        r15 = r27 - r28;
        r27 = r34.getRawY();
        r28 = r34.getY();
        r28 = r28 * r26;
        r16 = r27 - r28;
        r0 = r32;
        r0 = r0.mActivePointerId;
        r27 = r0;
        r0 = r34;
        r1 = r27;
        r6 = r0.findPointerIndex(r1);
        switch(r5) {
            case 0: goto L_0x0041;
            case 1: goto L_0x022c;
            case 2: goto L_0x008c;
            case 3: goto L_0x022c;
            case 4: goto L_0x003e;
            case 5: goto L_0x003e;
            case 6: goto L_0x0394;
            default: goto L_0x003e;
        };
    L_0x003e:
        r27 = 1;
    L_0x0040:
        return r27;
    L_0x0041:
        r27 = r34.getRawX();
        r0 = r27;
        r1 = r32;
        r1.mLastDownX = r0;
        r27 = r34.getRawY();
        r0 = r27;
        r1 = r32;
        r1.mLastDownY = r0;
        r0 = r27;
        r1 = r32;
        r1.mFirstDownY = r0;
        r27 = 0;
        r0 = r27;
        r1 = r32;
        r1.mOffset = r0;
        r27 = 0;
        r0 = r27;
        r1 = r32;
        r1.mMoved = r0;
        r27 = 0;
        r0 = r27;
        r1 = r32;
        r1.mDirection = r0;
        r27 = 0;
        r0 = r34;
        r1 = r27;
        r27 = r0.getPointerId(r1);
        r0 = r27;
        r1 = r32;
        r1.mActivePointerId = r0;
        r27 = 0;
        r0 = r27;
        r1 = r32;
        r1.mTouchUpVelocity = r0;
        goto L_0x003e;
    L_0x008c:
        r27 = -1;
        r0 = r27;
        if (r6 == r0) goto L_0x003e;
    L_0x0092:
        r0 = r32;
        r0 = r0.mMoved;
        r27 = r0;
        if (r27 == 0) goto L_0x003e;
    L_0x009a:
        r0 = r34;
        r27 = r0.getX(r6);
        r27 = r27 + r15;
        r0 = r27;
        r1 = r32;
        r1.mLastDownX = r0;
        r0 = r34;
        r27 = r0.getY(r6);
        r27 = r27 + r16;
        r0 = r27;
        r1 = r32;
        r1.mLastDownY = r0;
        r0 = r32;
        r0 = r0.mLastDownY;
        r27 = r0;
        r0 = r32;
        r0 = r0.mFirstDownY;
        r28 = r0;
        r27 = r27 - r28;
        r0 = r32;
        r0 = r0.mSwipeDistanceRatio;
        r28 = r0;
        r21 = r27 * r28;
        r0 = r32;
        r0 = r0.mStartOffsetY;
        r27 = r0;
        r14 = r27 + r21;
        r0 = r32;
        r0 = r0.mTrayHeight;
        r27 = r0;
        if (r27 <= 0) goto L_0x018e;
    L_0x00dc:
        r10 = 0;
        r18 = 0;
        r27 = r33.getTrayLevel();
        r0 = r32;
        r0 = r0.mDirection;
        r28 = r0;
        r0 = r32;
        r1 = r27;
        r2 = r28;
        r11 = r0.getOffsetLimit(r1, r2);
        r27 = com.android.launcher3.LauncherAppState.getInstance();
        r27 = r27.getNotificationPanelExpansionEnabled();
        if (r27 == 0) goto L_0x0137;
    L_0x00fd:
        r27 = com.android.launcher3.common.tray.TrayManager.TrayLevel.Overground;
        r28 = r33.getTrayLevel();
        r27 = r27.equals(r28);
        if (r27 == 0) goto L_0x0137;
    L_0x0109:
        r0 = r32;
        r0 = r0.mDirection;
        r27 = r0;
        if (r27 <= 0) goto L_0x0137;
    L_0x0111:
        r27 = com.android.launcher3.LauncherFeature.supportNotificationPanelExpansionWithHomeMoving();
        if (r27 == 0) goto L_0x01cf;
    L_0x0117:
        r27 = 1;
        r0 = r27;
        r1 = r32;
        r1.mDisallowCallBacksVisibity = r0;
        r28 = r34.getEventTime();
        r30 = r34.getDownTime();
        r22 = r28 - r30;
        r28 = 300; // 0x12c float:4.2E-43 double:1.48E-321;
        r27 = (r22 > r28 ? 1 : (r22 == r28 ? 0 : -1));
        if (r27 >= 0) goto L_0x019f;
    L_0x012f:
        r27 = 0;
        r0 = r27;
        r11.bottom = r0;
        r18 = 1;
    L_0x0137:
        r0 = r11.top;
        r27 = r0;
        r0 = r27;
        r0 = (float) r0;
        r27 = r0;
        r27 = (r14 > r27 ? 1 : (r14 == r27 ? 0 : -1));
        if (r27 >= 0) goto L_0x0159;
    L_0x0144:
        r0 = r11.top;
        r27 = r0;
        r0 = r27;
        r0 = (float) r0;
        r27 = r0;
        r27 = r14 - r27;
        r0 = r27;
        r10 = (int) r0;
        r0 = r11.top;
        r27 = r0;
        r0 = r27;
        r14 = (float) r0;
    L_0x0159:
        r0 = r11.bottom;
        r27 = r0;
        r0 = r27;
        r0 = (float) r0;
        r27 = r0;
        r27 = (r14 > r27 ? 1 : (r14 == r27 ? 0 : -1));
        if (r27 <= 0) goto L_0x017b;
    L_0x0166:
        r0 = r11.bottom;
        r27 = r0;
        r0 = r27;
        r0 = (float) r0;
        r27 = r0;
        r27 = r14 - r27;
        r0 = r27;
        r10 = (int) r0;
        r0 = r11.bottom;
        r27 = r0;
        r0 = r27;
        r14 = (float) r0;
    L_0x017b:
        if (r18 != 0) goto L_0x018e;
    L_0x017d:
        r0 = r32;
        r0 = r0.mFirstDownY;
        r27 = r0;
        r0 = (float) r10;
        r28 = r0;
        r27 = r27 + r28;
        r0 = r27;
        r1 = r32;
        r1.mFirstDownY = r0;
    L_0x018e:
        r27 = r33.isMovingOnBlock();
        if (r27 != 0) goto L_0x0199;
    L_0x0194:
        r0 = r32;
        r0.setTrayTranslationY(r14);
    L_0x0199:
        r0 = r32;
        r0.mOffset = r14;
        goto L_0x003e;
    L_0x019f:
        r0 = r32;
        r0 = r0.mTraySlipYForNotificationPanel;
        r27 = r0;
        r0 = r27;
        r11.bottom = r0;
        r0 = r32;
        r0 = r0.mOffset;
        r27 = r0;
        r27 = r14 - r27;
        r0 = r32;
        r0 = r0.mTraySlipYForNotificationPanel;
        r28 = r0;
        r0 = r28;
        r0 = (float) r0;
        r28 = r0;
        r27 = (r27 > r28 ? 1 : (r27 == r28 ? 0 : -1));
        if (r27 <= 0) goto L_0x0137;
    L_0x01c0:
        r28 = 300; // 0x12c float:4.2E-43 double:1.48E-321;
        r12 = r22 - r28;
        r0 = (float) r12;
        r27 = r0;
        r0 = r27;
        r14 = java.lang.Math.min(r0, r14);
        goto L_0x0137;
    L_0x01cf:
        r0 = r32;
        r0 = r0.mLastDownY;
        r27 = r0;
        r0 = r32;
        r0 = r0.mFirstDownY;
        r28 = r0;
        r8 = r27 - r28;
        r27 = 0;
        r27 = (r8 > r27 ? 1 : (r8 == r27 ? 0 : -1));
        if (r27 <= 0) goto L_0x0222;
    L_0x01e3:
        r0 = r32;
        r0 = r0.mIsExpandedNotiPanel;
        r27 = r0;
        if (r27 != 0) goto L_0x021e;
    L_0x01eb:
        r27 = r33.isMovingOnBlock();
        if (r27 != 0) goto L_0x021e;
    L_0x01f1:
        r27 = 1;
        r0 = r27;
        r1 = r32;
        r1.mIsExpandedNotiPanel = r0;
        r0 = r32;
        r0 = r0.mLauncher;
        r27 = r0;
        com.android.launcher3.Utilities.expandNotificationsPanel(r27);
        r27 = "TrayManager";
        r28 = new java.lang.StringBuilder;
        r28.<init>();
        r29 = android.view.MotionEvent.actionToString(r5);
        r28 = r28.append(r29);
        r29 = " quickOpenNotificationPanel";
        r28 = r28.append(r29);
        r28 = r28.toString();
        android.util.Log.d(r27, r28);
    L_0x021e:
        r27 = 1;
        goto L_0x0040;
    L_0x0222:
        r27 = 0;
        r0 = r27;
        r11.bottom = r0;
        r18 = 1;
        goto L_0x0137;
    L_0x022c:
        r27 = r33.isMovingOnBlock();
        if (r27 != 0) goto L_0x038a;
    L_0x0232:
        r0 = r32;
        r0 = r0.mMoved;
        r27 = r0;
        if (r27 == 0) goto L_0x038a;
    L_0x023a:
        r7 = 0;
        r27 = -1;
        r0 = r27;
        if (r6 == r0) goto L_0x035c;
    L_0x0241:
        r0 = r32;
        r0 = r0.mVelocityTracker;
        r27 = r0;
        r28 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
        r0 = r32;
        r0 = r0.mMaximumVelocity;
        r29 = r0;
        r0 = r29;
        r0 = (float) r0;
        r29 = r0;
        r27.computeCurrentVelocity(r28, r29);
        r0 = r32;
        r0 = r0.mVelocityTracker;
        r27 = r0;
        r0 = r32;
        r0 = r0.mActivePointerId;
        r28 = r0;
        r24 = r27.getXVelocity(r28);
        r0 = r32;
        r0 = r0.mVelocityTracker;
        r27 = r0;
        r0 = r32;
        r0 = r0.mActivePointerId;
        r28 = r0;
        r25 = r27.getYVelocity(r28);
        r0 = r25;
        r1 = r32;
        r1.mTouchUpVelocity = r0;
        r0 = r32;
        r0 = r0.mIsExpandedNotiPanel;
        r27 = r0;
        if (r27 != 0) goto L_0x035c;
    L_0x0285:
        r4 = 1045220557; // 0x3e4ccccd float:0.2 double:5.164075695E-315;
        r27 = 0;
        r27 = (r24 > r27 ? 1 : (r24 == r27 ? 0 : -1));
        if (r27 == 0) goto L_0x037b;
    L_0x028e:
        r20 = r25 / r24;
    L_0x0290:
        r27 = r34.getX();
        r0 = r32;
        r0 = r0.mLastDownX;
        r28 = r0;
        r27 = r27 - r28;
        r27 = java.lang.Math.abs(r27);
        r0 = r32;
        r1 = r27;
        r2 = r33;
        r27 = r0.willMove(r1, r2);
        if (r27 == 0) goto L_0x037f;
    L_0x02ac:
        r27 = r33.isScrollLocked();
        if (r27 != 0) goto L_0x037f;
    L_0x02b2:
        r17 = 1;
    L_0x02b4:
        r27 = 0;
        r27 = (r24 > r27 ? 1 : (r24 == r27 ? 0 : -1));
        if (r27 != 0) goto L_0x02c0;
    L_0x02ba:
        r27 = 0;
        r27 = (r25 > r27 ? 1 : (r25 == r27 ? 0 : -1));
        if (r27 == 0) goto L_0x02cb;
    L_0x02c0:
        r27 = java.lang.Math.abs(r20);
        r28 = 1045220557; // 0x3e4ccccd float:0.2 double:5.164075695E-315;
        r27 = (r27 > r28 ? 1 : (r27 == r28 ? 0 : -1));
        if (r27 <= 0) goto L_0x0383;
    L_0x02cb:
        r19 = 1;
    L_0x02cd:
        if (r19 == 0) goto L_0x02e8;
    L_0x02cf:
        r0 = r25;
        r0 = (int) r0;
        r27 = r0;
        r0 = r32;
        r0 = r0.mOffset;
        r28 = r0;
        r0 = r32;
        r1 = r33;
        r2 = r27;
        r3 = r28;
        r27 = r0.determineStageChange(r1, r2, r3);
        if (r27 != 0) goto L_0x02ea;
    L_0x02e8:
        if (r17 == 0) goto L_0x0387;
    L_0x02ea:
        r7 = 1;
    L_0x02eb:
        if (r7 == 0) goto L_0x030c;
    L_0x02ed:
        r9 = r32.getScreenDivisionSection(r33, r34);
        r27 = com.android.launcher3.util.logging.SALogging.getInstance();
        r28 = r33.getTrayLevel();
        r28 = r28.ordinal();
        r0 = r32;
        r0 = r0.mTouchStartIndex;
        r29 = r0;
        r0 = r27;
        r1 = r28;
        r2 = r29;
        r0.insertGesturePointOnTrayChange(r1, r2, r9);
    L_0x030c:
        r27 = "TrayManager";
        r28 = new java.lang.StringBuilder;
        r28.<init>();
        r29 = android.view.MotionEvent.actionToString(r5);
        r28 = r28.append(r29);
        r29 = " : v = ";
        r28 = r28.append(r29);
        r0 = r28;
        r1 = r24;
        r28 = r0.append(r1);
        r29 = ", ";
        r28 = r28.append(r29);
        r0 = r28;
        r1 = r25;
        r28 = r0.append(r1);
        r29 = ", threshold = ";
        r28 = r28.append(r29);
        r0 = r32;
        r0 = r0.mFlingThresholdVelocity;
        r29 = r0;
        r28 = r28.append(r29);
        r29 = ", scroll = ";
        r28 = r28.append(r29);
        r0 = r28;
        r1 = r17;
        r28 = r0.append(r1);
        r28 = r28.toString();
        android.util.Log.d(r27, r28);
    L_0x035c:
        r0 = r32;
        r0.trayTouchUp(r7);
        r27 = 0;
        r0 = r27;
        r1 = r32;
        r1.mMoved = r0;
    L_0x0369:
        r27 = 0;
        r0 = r27;
        r1 = r32;
        r1.mDirection = r0;
        r27 = -1;
        r0 = r27;
        r1 = r32;
        r1.mActivePointerId = r0;
        goto L_0x003e;
    L_0x037b:
        r20 = r25;
        goto L_0x0290;
    L_0x037f:
        r17 = 0;
        goto L_0x02b4;
    L_0x0383:
        r19 = 0;
        goto L_0x02cd;
    L_0x0387:
        r7 = 0;
        goto L_0x02eb;
    L_0x038a:
        r27 = 0;
        r0 = r32;
        r1 = r27;
        r0.trayTouchUp(r1);
        goto L_0x0369;
    L_0x0394:
        r32.onSecondaryPointerUp(r33, r34);
        goto L_0x003e;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.tray.TrayManager.onTouchEvent(com.android.launcher3.common.tray.TrayManager$TrayInteractionListener, android.view.MotionEvent):boolean");
    }

    private void prepareTrayMove(TrayInteractionListener sender, MotionEvent ev, float rawOffsetY, int activePointerIndex) {
        try {
            float y = ev.getY(activePointerIndex) + rawOffsetY;
            trayMoveStart();
            LauncherAnimUtils.onDestroyActivity();
            this.mDisallowCallBacksVisibity = false;
            this.mMoved = true;
            this.mDirection = (int) (y - this.mLastDownY);
            this.mLastDownY = y;
            if (sender != null) {
                TrayLevel senderLevel = sender.getTrayLevel();
                if (TrayLevel.Underground.equals(senderLevel)) {
                    Rect limit = getOffsetLimit(senderLevel, this.mDirection);
                    if (this.mCurrentChangedOffsetY > ((float) limit.bottom)) {
                        setTrayTranslationY((float) limit.top);
                    } else if (this.mCurrentChangedOffsetY < ((float) limit.top)) {
                        setTrayTranslationY((float) limit.bottom);
                    }
                }
            }
            this.mFirstDownY = y;
            this.mStartOffsetY = this.mCurrentChangedOffsetY;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "activePointerIndex = " + activePointerIndex);
            e.printStackTrace();
        }
    }

    private void onSecondaryPointerUp(TrayInteractionListener sender, MotionEvent ev) {
        int pointerIndex = (ev.getAction() & MotionEventCompat.ACTION_POINTER_INDEX_MASK) >> 8;
        if (ev.getPointerId(pointerIndex) == this.mActivePointerId) {
            int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            float newX = ev.getX(newPointerIndex);
            float newY = ev.getY(newPointerIndex);
            float viewScale = sender != null ? sender.getTrayScale() : 1.0f;
            float rawOffsetY = ev.getRawY() - (ev.getY() * viewScale);
            float gap = this.mLastDownY - this.mFirstDownY;
            this.mLastDownX = newX + (ev.getRawX() - (ev.getX() * viewScale));
            this.mLastDownY = newY + rawOffsetY;
            this.mFirstDownY = this.mLastDownY - gap;
            this.mActivePointerId = ev.getPointerId(newPointerIndex);
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.clear();
            }
        }
    }

    private Rect getOffsetLimit(TrayLevel senderLevel, int direction) {
        Rect limit = new Rect();
        if ((direction <= 0 || !TrayLevel.Overground.equals(senderLevel)) && (direction >= 0 || !TrayLevel.Underground.equals(senderLevel))) {
            limit.top = -this.mTraySlipY;
            limit.bottom = 0;
        } else {
            limit.top = 0;
            limit.bottom = this.mTraySlipY;
        }
        return limit;
    }

    private void setTrayTranslationY(float y) {
        if (this.mTrayEventCallbacks != null) {
            TrayEvent trayEvent = new TrayEvent(1, y);
            Iterator it = this.mTrayEventCallbacks.iterator();
            while (it.hasNext()) {
                ((TrayInteractionListener) it.next()).onReceiveTrayEvent(trayEvent);
            }
        }
    }

    private void trayMoveStart() {
        this.mTouching = true;
        this.mMoveAndAnimated = true;
        if (this.mTrayEventCallbacks != null) {
            TrayEvent trayEvent = new TrayEvent(4, -1.0f);
            Iterator it = this.mTrayEventCallbacks.iterator();
            while (it.hasNext()) {
                ((TrayInteractionListener) it.next()).onReceiveTrayEvent(trayEvent);
            }
        }
    }

    public void trayMoveEnd(int stageMode) {
        this.mMoveAndAnimated = false;
        if (this.mTrayEventCallbacks != null) {
            TrayEvent trayEvent = new TrayEvent(5, (float) stageMode);
            Iterator it = this.mTrayEventCallbacks.iterator();
            while (it.hasNext()) {
                ((TrayInteractionListener) it.next()).onReceiveTrayEvent(trayEvent);
            }
        }
    }

    private void trayTouchUp(boolean changedStage) {
        this.mTouching = false;
        if (this.mTrayEventCallbacks != null) {
            TrayEvent trayEvent = new TrayEvent(3, changedStage ? 1.0f : 0.0f);
            Iterator it = this.mTrayEventCallbacks.iterator();
            while (it.hasNext()) {
                ((TrayInteractionListener) it.next()).onReceiveTrayEvent(trayEvent);
            }
        }
    }

    public boolean isMoveAndAnimated() {
        return this.mMoveAndAnimated;
    }

    public boolean isTouching() {
        return this.mTouching;
    }

    private boolean willMove(float deltaY, TrayInteractionListener sender) {
        return deltaY >= ((float) this.mTouchSlop) && (sender.isVerticalScroll() || sender.isMovingOnBlock());
    }

    private boolean determineStageChange(TrayInteractionListener sender, int velocity, float offset) {
        if (sender != null) {
            if (sender.determineStageChange(velocity, offset, this.mFirstDownY, this.mLastDownY, this.mFlingThresholdVelocity)) {
                return true;
            }
        }
        return false;
    }

    private void changeStageWithDrag(TrayLevel targetLevel, int toDirection, int dragSourceType) {
        if (this.mTrayEventCallbacks != null) {
            if (targetLevel == TrayLevel.Overground) {
                loggingDirection(toDirection, dragSourceType);
            }
            TrayEvent trayEvent = new TrayEvent(10, (float) targetLevel.ordinal());
            Iterator it = this.mTrayEventCallbacks.iterator();
            while (it.hasNext()) {
                ((TrayInteractionListener) it.next()).onReceiveTrayEvent(trayEvent);
            }
        }
    }

    public void changeStageWithDrag(DragSource source) {
        TrayLevel toLevel = TrayLevel.Overground;
        if (!(this.mTrayEventCallbacks == null || source == null)) {
            Iterator it = this.mTrayEventCallbacks.iterator();
            while (it.hasNext()) {
                TrayInteractionListener cb = (TrayInteractionListener) it.next();
                if (source.equals(cb.getDropTarget())) {
                    toLevel = cb.getTrayLevel() == TrayLevel.Underground ? TrayLevel.Overground : TrayLevel.Underground;
                }
            }
        }
        changeStageWithDrag(toLevel, -1, source != null ? source.getDragSourceType() : 0);
    }

    private DropTarget findDropTarget(TrayLevel targetLevel) {
        if (this.mTrayEventCallbacks == null) {
            return null;
        }
        Iterator it = this.mTrayEventCallbacks.iterator();
        while (it.hasNext()) {
            TrayInteractionListener cb = (TrayInteractionListener) it.next();
            if (cb.getTrayLevel().equals(targetLevel)) {
                return cb.getDropTarget();
            }
        }
        return null;
    }

    public void setDisallowCallBacksVisibity(boolean disallow) {
        if (!LauncherFeature.supportNotificationPanelExpansionWithHomeMoving()) {
            this.mDisallowCallBacksVisibity = disallow;
        }
    }

    public void setBottomViewDragEnable() {
        boolean isLandscape = Utilities.getOrientation() == 2;
        if (this.mTransferPanelBottomView == null) {
            return;
        }
        if (Utilities.isMobileKeyboardMode() || isLandscape) {
            this.mTransferPanelBottomView.setDragEventListener(null);
        } else {
            this.mTransferPanelBottomView.setDragEventListener(this.mFakeViewDragEventListener);
        }
    }

    private void loggingDirection(int direction, int dragSourceType) {
        String eventID;
        Resources res = this.mLauncher.getResources();
        boolean isMultiSelectMode = false;
        int value = 1;
        String screenID = null;
        if (!(this.mLauncher.getDragMgr().getDragObject() == null || this.mLauncher.getDragMgr().getDragObject().extraDragInfoList == null)) {
            int extraItemsCount = this.mLauncher.getDragMgr().getDragObject().extraDragInfoList.size();
            isMultiSelectMode = extraItemsCount > 0;
            value = 1 + extraItemsCount;
        }
        if (this.mLauncher.isAppsStage() || dragSourceType == 1) {
            screenID = isMultiSelectMode ? res.getString(R.string.screen_Apps_SelectMode) : res.getString(R.string.screen_Apps_Selected);
        } else if (dragSourceType == 4) {
            screenID = isMultiSelectMode ? res.getString(R.string.screen_AppsFolder_SelectMode) : res.getString(R.string.screen_AppsFolder_Selected);
        }
        switch (direction) {
            case 0:
                eventID = res.getString(R.string.event_Apps_AddToHomeScreenTopTray);
                break;
            case 1:
                eventID = res.getString(R.string.event_Apps_AddToHomeScreenBottomTray);
                break;
            default:
                eventID = res.getString(R.string.event_Apps_AddToHomeScreenLongPress);
                break;
        }
        if (screenID != null) {
            SALogging.getInstance().insertEventLog(screenID, eventID, (long) value);
        }
    }

    public void onConfigurationChanged() {
        this.mTransferPanelTopView.setDescriptionHeight(this.mLauncher.getResources().getDimensionPixelSize(R.dimen.tray_transfer_panel_top_height));
    }

    public int calculateDuration(float movingDistance, int originDuration) {
        if (this.mTouchUpVelocity == 0.0f) {
            return originDuration;
        }
        return Math.min(originDuration, (int) Math.max(200.0f, (1200.0f / Math.max(2.0f, Math.abs(0.5f * (this.mTouchUpVelocity / 1500.0f)))) * Math.max(0.2f, movingDistance)));
    }

    private int getScreenDivisionSection(TrayInteractionListener sender, MotionEvent ev) {
        if (sender == null || sender.getScreenDivision() == null) {
            return 0;
        }
        return sender.getScreenDivision().getNumOfSection(ev.getRawX(), ev.getRawY());
    }

    private void showLightingEffect() {
        boolean exceptBottom = Utilities.getOrientation() == 2 || Utilities.isMobileKeyboardMode();
        LightingEffectManager.INSTANCE.showEffect(true, this.mFakeViewAnimationTime, exceptBottom);
    }
}

package com.android.launcher3.home;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.System;
import android.util.Log;
import android.util.Property;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RemoteViews;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.multiselect.MultiSelectManager;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.common.view.DragLayer.TouchCompleteListener;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.event.CheckLongPressHelper;
import com.android.launcher3.util.event.StylusEventHelper;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;

public class LauncherAppWidgetHostView extends AppWidgetHostView implements TouchCompleteListener {
    private static final Uri ACCU_WEATHERINFO_URI = Uri.parse("content://com.sec.android.daemonapp.ap.accuweather.provider/weatherinfo");
    private static final int ALPHA_DURATION = 250;
    private static final String EASY_CONTACTS_APPWIDGET_PACKAGE_NAME = "com.sec.android.widgetapp.easymodecontactswidget";
    private static final int FLING_INVALID = -1;
    private static final int FLING_NOT_SUPPORT = 0;
    private static final int FLING_SUPPORT = 1;
    public static final String GOOGLE_SEARCH_APP_PACKAGE_NAME = "com.google.android.googlequicksearchbox";
    public static final String PRV_HOSTVIEW = "previous_hostView";
    private static final int SWIPE_MIN_DISTANCE = 60;
    private static final String TAG = LauncherAppWidgetHostView.class.getSimpleName();
    private static final int TRANSLATION_DURATION = 250;
    public static final String WEATHER_APPWIDGET_PACKAGE_NAME = "com.sec.android.daemonapp";
    public static final Uri WETHER_SETTING_INFO_URI = Uri.parse("content://com.sec.android.daemonapp.ap.accuweather.provider/settings");
    private AnimatorSet mAnimationSet = null;
    private Context mContext;
    private DragLayer mDragLayer;
    private int mFlingOption = -1;
    private GestureDetector mGestureDetector = null;
    OnGestureListener mGestureListener = new OnGestureListener() {
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        public boolean onSingleTapUp(MotionEvent arg0) {
            return false;
        }

        public void onShowPress(MotionEvent arg0) {
        }

        public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
            boolean z = true;
            if (arg0.getY() >= ((float) LauncherAppWidgetHostView.this.mIndicatorHeight) / 2.0f && LauncherAppWidgetHostView.this.mIsWeatherFling && Math.abs(arg0.getY() - arg1.getY()) > 60.0f) {
                boolean z2;
                LauncherAppWidgetHostView launcherAppWidgetHostView = LauncherAppWidgetHostView.this;
                if (arg0.getY() - arg1.getY() > 0.0f) {
                    z2 = true;
                } else {
                    z2 = false;
                }
                launcherAppWidgetHostView.mIsScrollingUp = z2;
                LauncherAppWidgetHostView launcherAppWidgetHostView2 = LauncherAppWidgetHostView.this;
                if (LauncherAppWidgetHostView.this.mIsScrollingUp) {
                    z = false;
                }
                launcherAppWidgetHostView2.mIsScrollingDown = z;
            }
            return false;
        }

        public void onLongPress(MotionEvent arg0) {
        }

        public boolean onDown(MotionEvent arg0) {
            return false;
        }
    };
    public boolean mHasSetPivot = false;
    private HierarchyChangeListener mHierarchyChangeListener = null;
    private int mIndicatorHeight = 0;
    LayoutInflater mInflater;
    private boolean mIsAllowSwipe = false;
    public boolean mIsGSB = false;
    private boolean mIsScrollingDown = false;
    private boolean mIsScrollingUp = false;
    private boolean mIsWeatherCityOneMore = false;
    private boolean mIsWeatherFling = false;
    private float mLastDownY = 0.0f;
    private CheckLongPressHelper mLongPressHelper;
    private int mPreviousOrientation;
    FrameLayout mPrvHostView = null;
    private ResizeResult mResizeResult;
    private final Interpolator mSineInOut33 = ViInterpolator.getInterploator(30);
    private final Interpolator mSineInOut80 = ViInterpolator.getInterploator(34);
    private float mSlop;
    private StylusEventHelper mStylusEventHelper;
    private ContentObserver mWeatherCityAddObserver = null;
    private WidgetScroll mWidgetScroll = new WidgetScroll();

    private class HierarchyChangeListener implements OnHierarchyChangeListener {
        LauncherAppWidgetHostView mHostView;

        public HierarchyChangeListener(LauncherAppWidgetHostView hostView) {
            this.mHostView = hostView;
        }

        public void onChildViewAdded(View parent, View child) {
            if (!LauncherAppWidgetHostView.this.mWidgetScroll.isWeather()) {
                LauncherAppWidgetHostView.this.mWidgetScroll.getScrollType(this.mHostView, LauncherAppWidgetHostView.this.mIsAllowSwipe);
            }
        }

        public void onChildViewRemoved(View parent, View child) {
            if (!LauncherAppWidgetHostView.this.mWidgetScroll.isWeather()) {
                LauncherAppWidgetHostView.this.mWidgetScroll.getScrollType(this.mHostView, LauncherAppWidgetHostView.this.mIsAllowSwipe);
            }
        }
    }

    public static class ResizeResult {
        boolean forceToResize;
        public int height;
        public float scaleToResize;
        public int visibleHeight;
        public int visibleWidth;
        public int width;
    }

    private class WeatherCityAddObserver extends ContentObserver {
        public WeatherCityAddObserver() {
            super(new Handler());
        }

        public void onChange(boolean selfChange) {
            LauncherAppWidgetHostView.this.checkWeatherCount();
        }
    }

    public LauncherAppWidgetHostView(Context context) {
        super(context);
        this.mContext = context;
        this.mLongPressHelper = new CheckLongPressHelper(this);
        this.mStylusEventHelper = new StylusEventHelper(this);
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mDragLayer = ((Launcher) context).getDragLayer();
        this.mResizeResult = new ResizeResult();
        this.mResizeResult.forceToResize = false;
        this.mResizeResult.scaleToResize = 1.0f;
        this.mResizeResult.width = getWidth();
        this.mResizeResult.height = getHeight();
        if (LauncherFeature.supportGSAPreWarming() && getAppWidgetInfo() != null && getAppWidgetInfo().provider.getPackageName().equals(GOOGLE_SEARCH_APP_PACKAGE_NAME)) {
            this.mIsGSB = true;
        }
    }

    protected View getErrorView() {
        return this.mInflater.inflate(R.layout.appwidget_error, this, false);
    }

    public void updateLastInflationOrientation() {
        this.mPreviousOrientation = this.mContext.getApplicationContext().getResources().getConfiguration().orientation;
        Log.i(TAG, "updateLastInflationOrientation, orientation: " + this.mPreviousOrientation + ", widget: " + getAppWidgetInfo());
    }

    public void updateAppWidget(RemoteViews remoteViews) {
        updateLastInflationOrientation();
        super.updateAppWidget(remoteViews);
    }

    boolean isReinflateRequired() {
        if (this.mPreviousOrientation != Utilities.getOrientation()) {
            return true;
        }
        return false;
    }

    private void resetTouchStateOfTopContainer() {
        boolean isFind = false;
        ViewParent parent = this;
        while (parent != null) {
            if (parent instanceof HomeContainer) {
                isFind = true;
                break;
            }
            parent = parent.getParent();
        }
        if (isFind) {
            ((HomeContainer) parent).resetTouchState();
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        MultiSelectManager multiSelectMgr = ((Launcher) this.mContext).getMultiSelectManager();
        if (multiSelectMgr != null && multiSelectMgr.isMultiSelectMode()) {
            return true;
        }
        if (ev.getAction() == 0) {
            this.mLongPressHelper.cancelLongPress();
        }
        if (this.mLongPressHelper.hasPerformedLongPress()) {
            this.mLongPressHelper.cancelLongPress();
            return true;
        } else if (this.mStylusEventHelper.checkAndPerformStylusEvent(ev)) {
            this.mLongPressHelper.cancelLongPress();
            return true;
        } else {
            if (this.mGestureDetector != null) {
                this.mGestureDetector.onTouchEvent(ev);
            }
            if (ev.getAction() == 1) {
                SALogging.getInstance().insertItemLaunchLog((ItemInfo) getTag(), (Launcher) this.mContext);
                if (!(getAppWidgetInfo() == null || getAppWidgetInfo().provider == null || !"com.google.android.googlequicksearchbox.SearchWidgetProvider".equals(getAppWidgetInfo().provider.getClassName()))) {
                    GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_SEARCH_WIDGET_STARTED, null, -1, false);
                    SALogging.getInstance().insertGoogleSearchLaunchCount();
                }
            }
            switch (ev.getAction()) {
                case 0:
                    if (!this.mStylusEventHelper.inStylusButtonPressed()) {
                        this.mLongPressHelper.postCheckForLongPress();
                    }
                    this.mDragLayer.setTouchCompleteListener(this);
                    if (this.mWidgetScroll.isScrollable()) {
                        if (this.mWidgetScroll.isWeather() && this.mGestureDetector != null && !isAvailableChangeCity()) {
                            return false;
                        }
                        resetTouchStateOfTopContainer();
                    }
                    this.mLastDownY = ev.getRawY();
                    return false;
                case 1:
                case 3:
                    this.mLongPressHelper.cancelLongPress();
                    if (!this.mIsScrollingUp && !this.mIsScrollingDown) {
                        return false;
                    }
                    AppWidgetHostView origView = ((LauncherAppWidgetInfo) getTag()).hostView;
                    if (this.mAnimationSet != null && this.mAnimationSet.isRunning()) {
                        this.mAnimationSet.cancel();
                    }
                    return doWidgetAnimation(origView, this.mIsScrollingUp);
                case 2:
                    if (!Utilities.pointInView(this, ev.getX(), ev.getY(), this.mSlop)) {
                        this.mLongPressHelper.cancelLongPress();
                    }
                    if (!this.mIsAllowSwipe || Math.abs(ev.getRawY() - this.mLastDownY) < this.mSlop) {
                        return false;
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case 1:
            case 3:
                this.mLongPressHelper.cancelLongPress();
                break;
            case 2:
                if (!Utilities.pointInView(this, ev.getX(), ev.getY(), this.mSlop)) {
                    this.mLongPressHelper.cancelLongPress();
                    break;
                }
                break;
        }
        return false;
    }

    public boolean onInterceptHoverEvent(MotionEvent ev) {
        if (System.getInt(getContext().getContentResolver(), "pen_hovering", 0) != 0 && ev.getToolType(0) == 2) {
            return true;
        }
        return false;
    }

    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (!(this.mAnimationSet == null || this.mAnimationSet.isRunning())) {
            this.mAnimationSet.start();
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mSlop = (float) ViewConfiguration.get(getContext()).getScaledTouchSlop();
        LauncherAppWidgetInfo item = (LauncherAppWidgetInfo) getTag();
        if (item == null || !WEATHER_APPWIDGET_PACKAGE_NAME.equals(item.providerName.getPackageName())) {
            this.mFlingOption = 0;
        } else {
            if (this.mGestureDetector == null) {
                this.mGestureDetector = new GestureDetector(getContext(), this.mGestureListener);
                if (this.mWeatherCityAddObserver == null) {
                    this.mWeatherCityAddObserver = new WeatherCityAddObserver();
                    getContext().getContentResolver().registerContentObserver(ACCU_WEATHERINFO_URI, true, this.mWeatherCityAddObserver);
                }
                if (item.isRotating) {
                    this.mIsWeatherCityOneMore = item.isWeatherCityOneMore;
                    this.mWidgetScroll.setWeatherScrollablility(this.mIsWeatherCityOneMore);
                } else {
                    checkWeatherCount();
                }
            }
            this.mFlingOption = 1;
        }
        if (item != null && EASY_CONTACTS_APPWIDGET_PACKAGE_NAME.equals(item.providerName.getPackageName())) {
            this.mIsAllowSwipe = true;
        }
        if (LauncherFeature.supportGSAPreWarming() && item != null && GOOGLE_SEARCH_APP_PACKAGE_NAME.equals(item.providerName.getPackageName())) {
            this.mIsGSB = true;
        }
        if (item != null) {
            try {
                if (!this.mWidgetScroll.isWeather()) {
                    this.mWidgetScroll.getScrollType(this, this.mIsAllowSwipe);
                }
                if (this.mGestureDetector == null && !this.mIsAllowSwipe && this.mHierarchyChangeListener == null) {
                    this.mHierarchyChangeListener = new HierarchyChangeListener(this);
                    setOnHierarchyChangeListener(this.mHierarchyChangeListener);
                    Log.d(TAG, "setOnHierarchyChangeListener " + item.appWidgetId);
                }
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }
        }
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mGestureDetector != null) {
            this.mGestureDetector = null;
            if (this.mWeatherCityAddObserver != null) {
                getContext().getContentResolver().unregisterContentObserver(this.mWeatherCityAddObserver);
                this.mWeatherCityAddObserver = null;
                LauncherAppWidgetInfo item = (LauncherAppWidgetInfo) getTag();
                if (item.isRotating) {
                    item.isWeatherCityOneMore = this.mIsWeatherCityOneMore;
                }
            }
        }
    }

    private FrameLayout createPrvView(View origView) {
        if (origView == null) {
            return null;
        }
        origView.buildDrawingCache();
        Bitmap cacheBitmap = origView.getDrawingCache();
        if (cacheBitmap == null || cacheBitmap.isRecycled()) {
            Log.e(TAG, "startFlickAnimation getDrawingCache fail or cacheBitmap is isRecycled");
            return null;
        }
        FrameLayout fl = new FrameLayout(getContext());
        ImageView prvHostView = new ImageView(getContext());
        prvHostView.setImageBitmap(cacheBitmap);
        ((CellLayoutChildren) origView.getParent()).addView(fl, origView.getLayoutParams());
        fl.addView(prvHostView);
        fl.setTag(PRV_HOSTVIEW);
        return fl;
    }

    private void startWidgetAnimation(final AppWidgetHostView origView, final FrameLayout prvView, boolean isUp) {
        float f;
        float currentHeight = ((float) origView.getHeight()) * origView.getScaleY();
        prvView.setPivotX(0.0f);
        prvView.setPivotY(0.0f);
        prvView.setScaleX(this.mResizeResult.scaleToResize);
        prvView.setScaleY(this.mResizeResult.scaleToResize);
        View childAt = prvView.getChildAt(0);
        PropertyValuesHolder[] propertyValuesHolderArr = new PropertyValuesHolder[1];
        Property property = View.TRANSLATION_Y;
        float[] fArr = new float[2];
        fArr[0] = 0.0f;
        if (isUp) {
            f = -currentHeight;
        } else {
            f = currentHeight;
        }
        fArr[1] = f;
        propertyValuesHolderArr[0] = PropertyValuesHolder.ofFloat(property, fArr);
        ObjectAnimator.ofPropertyValuesHolder(childAt, propertyValuesHolderArr).setDuration(250).setInterpolator(this.mSineInOut33);
        View childAt2 = origView.getChildAt(0);
        PropertyValuesHolder[] propertyValuesHolderArr2 = new PropertyValuesHolder[1];
        Property property2 = View.TRANSLATION_Y;
        float[] fArr2 = new float[2];
        if (!isUp) {
            currentHeight = -currentHeight;
        }
        fArr2[0] = currentHeight;
        fArr2[1] = 0.0f;
        propertyValuesHolderArr2[0] = PropertyValuesHolder.ofFloat(property2, fArr2);
        Animator origTransAni = ObjectAnimator.ofPropertyValuesHolder(childAt2, propertyValuesHolderArr2).setDuration(250);
        origTransAni.setInterpolator(this.mSineInOut33);
        ObjectAnimator.ofPropertyValuesHolder(prvView.getChildAt(0), new PropertyValuesHolder[]{PropertyValuesHolder.ofFloat(View.ALPHA, new float[]{1.0f, 0.0f})}).setDuration(250).setInterpolator(this.mSineInOut80);
        ObjectAnimator.ofPropertyValuesHolder(origView.getChildAt(0), new PropertyValuesHolder[]{PropertyValuesHolder.ofFloat(View.ALPHA, new float[]{0.0f, 1.0f})}).setDuration(250).setInterpolator(this.mSineInOut80);
        origTransAni.addListener(new AnimatorListener() {
            public void onAnimationEnd(Animator animator) {
                LauncherAppWidgetHostView.this.initWidgetAnimation(origView, prvView);
            }

            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }

            public void onAnimationStart(Animator animator) {
            }
        });
        origView.getChildAt(0).setAlpha(0.0f);
        this.mAnimationSet = new AnimatorSet();
        this.mAnimationSet.playTogether(new Animator[]{origTransAni, prvTransAni, prvAlphaAni, origAlphaAni});
    }

    private void initWidgetAnimation(View origView, View prvView) {
        origView.destroyDrawingCache();
        CellLayoutChildren parent = (CellLayoutChildren) origView.getParent();
        if (parent != null) {
            parent.removeView(prvView);
        }
        this.mPrvHostView = null;
        this.mAnimationSet = null;
        if (((AppWidgetHostView) origView).getChildAt(0) != null) {
            ((AppWidgetHostView) origView).getChildAt(0).setAlpha(1.0f);
        }
    }

    private boolean doWidgetAnimation(AppWidgetHostView origView, boolean isUp) {
        this.mIsScrollingUp = false;
        this.mIsScrollingDown = false;
        if (this.mPrvHostView != null) {
            return false;
        }
        this.mPrvHostView = createPrvView(origView);
        if (this.mPrvHostView == null) {
            return false;
        }
        Bundle opts = new Bundle();
        opts.putInt("fling", isUp ? 0 : 1);
        origView.updateAppWidgetOptions(opts);
        if (this.mIsWeatherCityOneMore) {
            startWidgetAnimation(origView, this.mPrvHostView, isUp);
            return true;
        }
        initWidgetAnimation(origView, this.mPrvHostView);
        return false;
    }

    private void checkWeatherCount() {
        Cursor c = getContext().getContentResolver().query(ACCU_WEATHERINFO_URI, null, null, null, null);
        if (c == null || c.getCount() < 2) {
            this.mIsWeatherCityOneMore = false;
        } else {
            this.mIsWeatherCityOneMore = true;
        }
        this.mWidgetScroll.setWeatherScrollablility(this.mIsWeatherCityOneMore);
        Log.i(TAG, "checkWeatherCount(), mIsWeatherCityOneMore = " + this.mIsWeatherCityOneMore);
        if (c != null && !c.isClosed()) {
            c.close();
        }
    }

    public boolean supportFlingOption() {
        if (this.mFlingOption == -1) {
            LauncherAppWidgetInfo item = (LauncherAppWidgetInfo) getTag();
            if (item == null || !WEATHER_APPWIDGET_PACKAGE_NAME.equals(item.providerName.getPackageName())) {
                this.mFlingOption = 0;
            } else {
                this.mFlingOption = 1;
            }
        }
        if (this.mFlingOption == 1) {
            return true;
        }
        return false;
    }

    public boolean getIsGSB() {
        return this.mIsGSB;
    }

    public void cancelLongPress() {
        super.cancelLongPress();
        this.mLongPressHelper.cancelLongPress();
    }

    public AppWidgetProviderInfo getAppWidgetInfo() {
        AppWidgetProviderInfo info = super.getAppWidgetInfo();
        if (info == null || (info instanceof LauncherAppWidgetProviderInfo)) {
            return info;
        }
        throw new IllegalStateException("Launcher widget must have LauncherAppWidgetProviderInfo");
    }

    public LauncherAppWidgetProviderInfo getLauncherAppWidgetProviderInfo() {
        return (LauncherAppWidgetProviderInfo) getAppWidgetInfo();
    }

    public void onTouchComplete() {
        if (!this.mLongPressHelper.hasPerformedLongPress()) {
            this.mLongPressHelper.cancelLongPress();
        }
    }

    public int getDescendantFocusability() {
        return 393216;
    }

    public void setResizeScaleResult(ResizeResult result) {
        this.mResizeResult = result;
        Log.d(TAG, "setResizeScaleResult() " + this.mResizeResult.width + "/ " + this.mResizeResult.height + " scaleToResize = " + this.mResizeResult.scaleToResize + "(widget id = " + getAppWidgetId() + ")");
        if (!this.mHasSetPivot) {
            setPivotX(0.0f);
            setPivotY(0.0f);
        }
        setScaleX(this.mResizeResult.scaleToResize);
        setScaleY(this.mResizeResult.scaleToResize);
    }

    public static ResizeResult calculateWidgetSize(int spanX, int spanY, int width, int height) {
        ResizeResult result = new ResizeResult();
        result.forceToResize = false;
        result.scaleToResize = 1.0f;
        result.width = width;
        result.height = height;
        result.visibleWidth = width;
        result.visibleHeight = height;
        DeviceProfile profile = LauncherAppState.getInstance().getDeviceProfile();
        int cellSpacingWidth = profile.homeGrid.getCellGapX();
        int cellSpacingHeight = profile.homeGrid.getCellGapY();
        int requiredWidth = (spanX * profile.defaultCellWidth) + (spanX >= 2 ? (spanX - 1) * cellSpacingWidth : 0);
        float scaleForMobileKeyboard = 1.0f;
        if (Utilities.isMobileKeyboardMode()) {
            scaleForMobileKeyboard = 0.7f;
        }
        int requiredHeight = (int) (((float) ((spanY >= 2 ? (spanY - 1) * cellSpacingHeight : 0) + (spanY * profile.defaultCellHeight))) / scaleForMobileKeyboard);
        if (width < requiredWidth) {
            result.forceToResize = true;
            result.width = requiredWidth;
        }
        if (height < requiredHeight) {
            result.forceToResize = true;
            result.height = requiredHeight;
        }
        if (result.forceToResize) {
            float sx = ((float) width) / ((float) result.width);
            float sy = ((float) height) / ((float) result.height);
            if (!(sx == 1.0f && sy == 1.0f)) {
                if (sx < sy) {
                    result.scaleToResize = sx;
                } else {
                    result.scaleToResize = sy;
                }
                result.width = (int) ((((float) width) * 1.0f) / result.scaleToResize);
                result.height = (int) ((((float) height) * 1.0f) / result.scaleToResize);
            }
        }
        return result;
    }

    public ResizeResult getResizeResult() {
        return this.mResizeResult;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isAvailableChangeCity() {
        /*
        r11 = this;
        r3 = 0;
        r9 = 1;
        r10 = 0;
        r1 = r11.getContext();
        r0 = r1.getContentResolver();
        r2 = new java.lang.String[r9];
        r1 = "COL_SETTING_PINNED_LOCATION";
        r2[r10] = r1;
        r1 = WETHER_SETTING_INFO_URI;
        r4 = r3;
        r5 = r3;
        r6 = r0.query(r1, r2, r3, r4, r5);
        r8 = 0;
        if (r6 == 0) goto L_0x002a;
    L_0x001c:
        r1 = r6.moveToFirst();	 Catch:{ Exception -> 0x004c }
        if (r1 == 0) goto L_0x0027;
    L_0x0022:
        r1 = 0;
        r8 = r6.getInt(r1);	 Catch:{ Exception -> 0x004c }
    L_0x0027:
        r6.close();
    L_0x002a:
        if (r8 != 0) goto L_0x0072;
    L_0x002c:
        r1 = r9;
    L_0x002d:
        r11.mIsWeatherFling = r1;
        r1 = TAG;
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = "isAvailableChangeCity(), mIsWeatherFling = ";
        r3 = r3.append(r4);
        r4 = r11.mIsWeatherFling;
        r3 = r3.append(r4);
        r3 = r3.toString();
        android.util.Log.i(r1, r3);
        if (r8 == r9) goto L_0x0074;
    L_0x004b:
        return r9;
    L_0x004c:
        r7 = move-exception;
        r1 = TAG;	 Catch:{ all -> 0x006d }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x006d }
        r3.<init>();	 Catch:{ all -> 0x006d }
        r4 = "isAvailableChangeCity Exception : ";
        r3 = r3.append(r4);	 Catch:{ all -> 0x006d }
        r4 = r7.toString();	 Catch:{ all -> 0x006d }
        r3 = r3.append(r4);	 Catch:{ all -> 0x006d }
        r3 = r3.toString();	 Catch:{ all -> 0x006d }
        android.util.Log.e(r1, r3);	 Catch:{ all -> 0x006d }
        r6.close();
        goto L_0x002a;
    L_0x006d:
        r1 = move-exception;
        r6.close();
        throw r1;
    L_0x0072:
        r1 = r10;
        goto L_0x002d;
    L_0x0074:
        r9 = r10;
        goto L_0x004b;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.LauncherAppWidgetHostView.isAvailableChangeCity():boolean");
    }
}

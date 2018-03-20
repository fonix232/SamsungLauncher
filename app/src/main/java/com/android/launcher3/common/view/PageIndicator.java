package com.android.launcher3.common.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.home.ZeroPageController;
import com.android.launcher3.theme.OpenThemeManager;
import com.android.launcher3.theme.OpenThemeManager.ThemeItems;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.WhiteBgManager;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;

public class PageIndicator extends LinearLayout {
    private static final int ANIM_DURATION = 100;
    private static final float ANIM_SCALE = 0.5f;
    private static final int PANEL_ANIMATION_TIME = 600;
    private static final String TAG = "PageIndicator";
    private int mActiveMarkerIndex;
    private int mEnableGroupingSize;
    private boolean mExistPlusPage;
    private boolean mExistZeroPage;
    private int mIndicatorMargin;
    private LayoutInflater mLayoutInflater;
    private int mMarkerGap;
    private int mMarkerMargin;
    private int mMarkerStartOffset;
    private int mMarkerWidth;
    private ArrayList<PageIndicatorMarker> mMarkers;
    private int mMaxVisibleSize;
    private PagedView mPagedView;
    private int[] mWindowRange;

    public static class PageMarkerResources {
        Drawable active;
        Drawable inactive;
        IndicatorType type;

        public enum IndicatorType {
            DEFAULT,
            HOME,
            PLUS,
            ZEROPAGE,
            FESTIVAL
        }

        public PageMarkerResources() {
            OpenThemeManager themeManager = OpenThemeManager.getInstance();
            Context context = LauncherAppState.getInstance().getContext();
            this.type = IndicatorType.DEFAULT;
            if (themeManager.isDefaultTheme()) {
                this.active = context.getDrawable(R.drawable.homescreen_menu_page_navi_default_f);
                this.inactive = context.getDrawable(R.drawable.homescreen_menu_page_navi_default);
                return;
            }
            this.active = themeManager.getPreloadDrawable(ThemeItems.PAGEINDICATOR_DEFAULT.value());
            this.inactive = themeManager.getPreloadDrawable(ThemeItems.PAGEINDICATOR_DEFAULT.value());
        }

        public PageMarkerResources(IndicatorType indicatorType) {
            OpenThemeManager themeManager = OpenThemeManager.getInstance();
            Context context = LauncherAppState.getInstance().getContext();
            this.type = indicatorType;
            switch (indicatorType) {
                case HOME:
                    if (themeManager.isDefaultTheme()) {
                        this.active = context.getDrawable(R.drawable.homescreen_menu_page_navi_home_f);
                        this.inactive = context.getDrawable(R.drawable.homescreen_menu_page_navi_home);
                        return;
                    }
                    this.active = themeManager.getPreloadDrawable(ThemeItems.PAGEINDICATOR_HOME.value());
                    this.inactive = themeManager.getPreloadDrawable(ThemeItems.PAGEINDICATOR_HOME.value());
                    return;
                case PLUS:
                    this.active = context.getDrawable(R.drawable.homescreen_menu_page_navi_plus_f);
                    this.inactive = context.getDrawable(R.drawable.homescreen_menu_page_navi_plus);
                    return;
                case ZEROPAGE:
                    if (themeManager.isDefaultTheme()) {
                        this.active = context.getDrawable(R.drawable.homescreen_menu_page_navi_headlines_f);
                        this.inactive = context.getDrawable(R.drawable.homescreen_menu_page_navi_headlines);
                        return;
                    }
                    this.active = themeManager.getPreloadDrawable(ThemeItems.PAGEINDICATOR_HEADLINE.value());
                    this.inactive = themeManager.getPreloadDrawable(ThemeItems.PAGEINDICATOR_HEADLINE.value());
                    return;
                case FESTIVAL:
                    this.active = themeManager.getPreloadDrawable(ThemeItems.PAGEINDICATOR_FESTIVAL.value());
                    this.inactive = themeManager.getPreloadDrawable(ThemeItems.PAGEINDICATOR_FESTIVAL.value());
                    return;
                default:
                    if (themeManager.isDefaultTheme()) {
                        this.active = context.getDrawable(R.drawable.homescreen_menu_page_navi_default_f);
                        this.inactive = context.getDrawable(R.drawable.homescreen_menu_page_navi_default);
                        return;
                    }
                    this.active = themeManager.getPreloadDrawable(ThemeItems.PAGEINDICATOR_DEFAULT.value());
                    this.inactive = themeManager.getPreloadDrawable(ThemeItems.PAGEINDICATOR_DEFAULT.value());
                    return;
            }
        }
    }

    public PageIndicator(Context context) {
        this(context, null);
    }

    public PageIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageIndicator(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mMarkers = new ArrayList();
        this.mWindowRange = new int[2];
        this.mMarkerStartOffset = 0;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PageIndicator, defStyle, 0);
        this.mMaxVisibleSize = a.getInteger(0, 15);
        this.mEnableGroupingSize = this.mMaxVisibleSize;
        Resources res = context.getResources();
        this.mMarkerGap = res.getDimensionPixelSize(R.dimen.pageIndicator_dot_gap);
        this.mMarkerMargin = res.getDimensionPixelSize(R.dimen.pageIndicator_dot_margin);
        this.mMarkerWidth = res.getDimensionPixelSize(R.dimen.pageIndicator_dot_size);
        this.mIndicatorMargin = res.getDimensionPixelSize(R.dimen.pageIndicator_margin);
        this.mWindowRange[0] = 0;
        this.mWindowRange[1] = 0;
        this.mLayoutInflater = LayoutInflater.from(context);
        setImportantForAccessibility(4);
        a.recycle();
    }

    public void enableLayoutTransitions() {
        if (this.mPagedView != null && this.mPagedView.isResumed()) {
            LayoutTransition transition = getLayoutTransition();
            transition.enableTransitionType(2);
            transition.enableTransitionType(3);
            transition.enableTransitionType(0);
            transition.enableTransitionType(1);
        }
    }

    public void disableLayoutTransitions() {
        LayoutTransition transition = getLayoutTransition();
        transition.disableTransitionType(2);
        transition.disableTransitionType(3);
        transition.disableTransitionType(0);
        transition.disableTransitionType(1);
    }

    private void updateActiveMarker() {
        disableLayoutTransitions();
        for (int i = 0; i < this.mMarkers.size(); i++) {
            PageIndicatorMarker marker = (PageIndicatorMarker) this.mMarkers.get(i);
            if (i == this.mActiveMarkerIndex) {
                marker.activate(false);
            } else {
                marker.inactivate(false);
            }
        }
        enableLayoutTransitions();
    }

    public void offsetWindowCenterTo(boolean allowAnimations) {
        int i;
        int maxWidth;
        if (this.mActiveMarkerIndex < 0) {
            Log.e(TAG, "ActiveMarkerIndex is invalid");
        }
        int windowEnd = Math.min(this.mMarkers.size(), this.mMaxVisibleSize);
        boolean windowMoved = (this.mWindowRange[0] == 0 && this.mWindowRange[1] == windowEnd) ? false : true;
        if (!allowAnimations) {
            disableLayoutTransitions();
        }
        for (i = getChildCount() - 1; i >= 0; i--) {
            PageIndicatorMarker marker = (PageIndicatorMarker) getChildAt(i);
            if (this.mMarkers.indexOf(marker) < 0) {
                removeView(marker);
            }
        }
        int markerGap = this.mMarkerGap;
        int markerMargin = this.mMarkerMargin;
        int indicatorWidth = ((this.mMarkerWidth + this.mMarkerGap) * this.mMarkers.size()) + (this.mMarkerMargin * (this.mMarkers.size() - 1));
        Point displaySize = new Point();
        Utilities.getScreenSize(getContext(), displaySize);
        int parentWidth = getParent() != null ? ((View) getParent()).getWidth() : -1;
        if (parentWidth <= 0 || parentWidth > displaySize.x) {
            parentWidth = displaySize.x;
        }
        if (parentWidth < displaySize.x) {
            maxWidth = parentWidth;
        } else {
            maxWidth = parentWidth - (this.mIndicatorMargin * 2);
        }
        if (indicatorWidth > maxWidth && this.mMarkers.size() != 0) {
            markerGap = (maxWidth / this.mMarkers.size()) - this.mMarkerWidth;
            markerMargin = 0;
        }
        i = 0;
        while (i < this.mMarkers.size()) {
            marker = (PageIndicatorMarker) this.mMarkers.get(i);
            LayoutParams lp = (LayoutParams) marker.getLayoutParams();
            lp.width = this.mMarkerWidth + markerGap;
            if (i == this.mMarkers.size() - 1) {
                lp.setMarginEnd(0);
            } else {
                lp.setMarginEnd(markerMargin);
            }
            marker.setLayoutParams(lp);
            if (0 <= i && i < windowEnd) {
                if (indexOfChild(marker) < 0) {
                    addView(marker, i);
                }
                if (i != this.mActiveMarkerIndex || marker.getMarkerType() == IndicatorType.PLUS) {
                    marker.inactivate(windowMoved);
                } else {
                    marker.activate(windowMoved);
                }
            }
            i++;
        }
        if (!allowAnimations) {
            enableLayoutTransitions();
        }
        this.mWindowRange[0] = 0;
        this.mWindowRange[1] = windowEnd;
    }

    public ArrayList<PageIndicatorMarker> getMarkers() {
        return this.mMarkers;
    }

    public void setPlusPage(boolean page) {
        this.mExistPlusPage = page;
    }

    private int getCustomPageCount() {
        return this.mPagedView.getCustomPageCount();
    }

    private int getPageIndex(int pageIndicatorIndex) {
        if (this.mPagedView == null) {
            return 0;
        }
        int normalPages = this.mPagedView.getPageCount() - getCustomPageCount();
        int pageIndex = pageIndicatorIndex;
        int defaultGroup = normalPages / this.mEnableGroupingSize;
        if (defaultGroup <= 0) {
            return pageIndex;
        }
        int defaultGroupNum = this.mEnableGroupingSize - (normalPages % this.mEnableGroupingSize);
        if (!this.mExistZeroPage) {
            pageIndex++;
        }
        if (pageIndex <= defaultGroupNum) {
            pageIndex = ((pageIndex - 1) * defaultGroup) + 1;
        } else {
            pageIndex = ((defaultGroup * defaultGroupNum) + ((defaultGroup + 1) * ((pageIndex - defaultGroupNum) - 1))) + 1;
        }
        if (this.mExistZeroPage) {
            return pageIndex;
        }
        return pageIndex - 1;
    }

    private int getAdjustedPageIndex(int pageIndex) {
        if (this.mPagedView == null) {
            return 0;
        }
        int normalPages = this.mPagedView.getPageCount() - getCustomPageCount();
        int adjustedPageIndex = pageIndex;
        int defaultGroup = normalPages / this.mEnableGroupingSize;
        if (adjustedPageIndex == 0 || defaultGroup <= 0) {
            return adjustedPageIndex;
        }
        if (!this.mExistZeroPage) {
            adjustedPageIndex++;
        }
        int defaultGroupNum = this.mEnableGroupingSize - (normalPages % this.mEnableGroupingSize);
        if (adjustedPageIndex <= defaultGroup * defaultGroupNum) {
            adjustedPageIndex = ((adjustedPageIndex - 1) / defaultGroup) + 1;
        } else {
            adjustedPageIndex = ((((adjustedPageIndex - (defaultGroup * defaultGroupNum)) - 1) / (defaultGroup + 1)) + defaultGroupNum) + 1;
        }
        if (this.mExistZeroPage) {
            return adjustedPageIndex;
        }
        return adjustedPageIndex - 1;
    }

    private void setPagedView(PagedView pagedView) {
        this.mPagedView = pagedView;
        this.mEnableGroupingSize = this.mMaxVisibleSize - this.mPagedView.getSupportCustomPageCount();
    }

    public void addMarker(int index, PageMarkerResources marker, boolean allowAnimations, PagedView pagedView) {
        setPagedView(pagedView);
        addMarker(index, marker, allowAnimations, 0);
    }

    private void addMarker(int index, PageMarkerResources marker, boolean allowAnimations, int lastIndex) {
        if (index == -1) {
            index = -1;
        } else {
            index = Math.max(0, Math.min(index, this.mMarkers.size()));
        }
        if (this.mMarkers.size() >= this.mMarkerStartOffset + index) {
            if (!isGrouping() || index == -1 || !canNotEditMarker(index, marker.type)) {
                if (index < this.mActiveMarkerIndex) {
                    this.mActiveMarkerIndex++;
                }
                PageIndicatorMarker m = (PageIndicatorMarker) this.mLayoutInflater.inflate(R.layout.page_indicator_marker, this, false);
                m.setMarkerDrawables(marker.active, marker.inactive, marker.type);
                if (this.mPagedView.supportWhiteBg()) {
                    m.changeColorForBg(WhiteBgManager.isWhiteBg());
                }
                OnClickListener listener = getPageIndicatorMarkerClickListener();
                m.setClickable(true);
                m.setSoundEffectsEnabled(false);
                m.setOnClickListener(listener);
                this.mMarkers.add(Math.max(0, this.mMarkerStartOffset + index), m);
                if (lastIndex == 0 || index == lastIndex) {
                    offsetWindowCenterTo(allowAnimations);
                }
            }
        }
    }

    public void addMarkers(ArrayList<PageMarkerResources> markers, boolean allowAnimations, PagedView pagedView) {
        setPagedView(pagedView);
        for (int i = 0; i < markers.size(); i++) {
            addMarker(Integer.MAX_VALUE, (PageMarkerResources) markers.get(i), allowAnimations, markers.size() - 1);
        }
    }

    public void updateMarker(int index, PageMarkerResources marker) {
        ((PageIndicatorMarker) this.mMarkers.get(Math.max(0, Math.min(this.mMarkers.size() - 1, getAdjustedPageIndex(index))))).setMarkerDrawables(marker.active, marker.inactive, marker.type);
    }

    public void updateHomeMarker(int index, PageMarkerResources marker) {
        index = Math.max(0, Math.min(this.mMarkers.size() - 1, getAdjustedPageIndex(this.mMarkerStartOffset + index)));
        int size = this.mMarkers.size();
        int i = 0;
        while (i < size) {
            PageIndicatorMarker m = (PageIndicatorMarker) this.mMarkers.get(i);
            if (i == index) {
                m.setMarkerDrawables(marker.active, marker.inactive, marker.type);
            } else if (this.mExistZeroPage && i == 0) {
                markerRes = new PageMarkerResources(IndicatorType.ZEROPAGE);
                m.setMarkerDrawables(markerRes.active, markerRes.inactive, markerRes.type);
            } else if (m.getMarkerType() == IndicatorType.HOME) {
                markerRes = new PageMarkerResources(IndicatorType.DEFAULT);
                m.setMarkerDrawables(markerRes.active, markerRes.inactive, markerRes.type);
            }
            i++;
        }
    }

    public void setZeroPageMarker(boolean existZeroPage) {
        this.mExistZeroPage = existZeroPage;
    }

    private boolean canNotEditMarker(int index, IndicatorType type) {
        return (type == IndicatorType.ZEROPAGE || type == IndicatorType.PLUS || (index == 0 && this.mExistZeroPage && type == IndicatorType.HOME)) ? false : true;
    }

    public void removeMarker(int index, boolean allowAnimations) {
        boolean z = true;
        if (this.mMarkers.size() > 0) {
            index = Math.max(0, Math.min(this.mMarkers.size() - (this.mExistPlusPage ? 2 : 1), this.mMarkerStartOffset + index));
            IndicatorType markerType = ((PageIndicatorMarker) this.mMarkers.get(index)).getMarkerType();
            if (!isGrouping() || !canNotEditMarker(index, markerType)) {
                this.mMarkers.remove(index);
                if (!allowAnimations || markerType == IndicatorType.ZEROPAGE) {
                    z = false;
                }
                offsetWindowCenterTo(z);
            }
        }
    }

    public void removeAllMarkers() {
        this.mMarkers.clear();
        removeAllViews();
    }

    public void setActiveMarker(int index) {
        this.mActiveMarkerIndex = getAdjustedPageIndex(this.mMarkerStartOffset + index);
        updateActiveMarker();
    }

    public void setMarkerStartOffset(int offset) {
        this.mMarkerStartOffset = offset;
    }

    public int getMarkerStartOffset() {
        return this.mMarkerStartOffset;
    }

    private OnClickListener getPageIndicatorMarkerClickListener() {
        return new OnClickListener() {
            public void onClick(View v) {
                if (PageIndicator.this.mPagedView != null) {
                    int index = PageIndicator.this.mMarkers.indexOf(v);
                    int page = Math.max(ZeroPageController.isEnableZeroPage() ? -1 : 0, PageIndicator.this.getPageIndex(index) - PageIndicator.this.mMarkerStartOffset);
                    if (!(!PageIndicator.this.isGrouping() || ((PageIndicatorMarker) PageIndicator.this.mMarkers.get(index)).getMarkerType() == IndicatorType.ZEROPAGE || ((PageIndicatorMarker) PageIndicator.this.mMarkers.get(index)).getMarkerType() == IndicatorType.PLUS)) {
                        PageIndicator.this.showPageNumber(PageIndicator.this.mMarkerStartOffset + page);
                    }
                    if (PageIndicator.this.mPagedView.isScrolling()) {
                        PageIndicator.this.mPagedView.cancelDeferLoadAssociatedPagesUntilScrollCompletes();
                        PageIndicator.this.mPagedView.setCurrentPage(PageIndicator.this.mPagedView.getNextPage());
                    }
                    PageIndicator.this.mPagedView.loadAssociatedPages(page);
                    PageIndicator.this.mPagedView.snapToPage(page);
                    if (page != -1) {
                        PageIndicator.this.mPagedView.snapToPageSALogging(true);
                    }
                }
            }
        };
    }

    public void changeColorForBg(boolean whiteBg) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            ((PageIndicatorMarker) getChildAt(i)).changeColorForBg(whiteBg);
        }
    }

    public int getMaxVisibleSize() {
        return this.mMaxVisibleSize;
    }

    public boolean isGrouping() {
        if (this.mPagedView == null) {
            return false;
        }
        int normalPages = this.mPagedView.getPageCount() - getCustomPageCount();
        if (this.mMarkers.size() < this.mEnableGroupingSize || normalPages <= this.mEnableGroupingSize) {
            return false;
        }
        return true;
    }

    private void showPageNumber(int number) {
        if (number < 0) {
            return;
        }
        if (number != 0 || !this.mExistZeroPage) {
            String pageNum;
            if (!this.mExistZeroPage) {
                number++;
            }
            LinearLayout layout = (LinearLayout) this.mLayoutInflater.inflate(R.layout.page_indicator_toast, this, false);
            TextView tx = (TextView) layout.findViewById(R.id.page_indicator_toast_text);
            String currentLanguage = Utilities.getLocale(getContext()).getLanguage();
            if ("ar".equals(currentLanguage) || "fa".equals(currentLanguage)) {
                pageNum = Utilities.toArabicDigits(String.valueOf(number), currentLanguage);
            } else {
                pageNum = String.valueOf(number);
            }
            tx.setText(pageNum);
            DeviceProfile dp = LauncherAppState.getInstance().getDeviceProfile();
            int xOffset = 0;
            if (dp.isMultiwindowMode && dp.isLandscape) {
                xOffset = dp.getMultiWindowPanelSize() / 2;
            }
            final Toast toast = new Toast(getContext());
            toast.setGravity(49, xOffset, getPageNumberTopMargin());
            toast.setView(layout);
            toast.show();
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    toast.cancel();
                }
            }, 600);
        }
    }

    private int getPageNumberTopMargin() {
        Resources res = getContext().getResources();
        int indicatorPopupGap = res.getDimensionPixelSize(R.dimen.pageindicator_toast_gap);
        int indicatorPopupSize = res.getDimensionPixelSize(R.dimen.pageindicator_toast_size);
        int statusbarSize = res.getDimensionPixelSize(R.dimen.status_bar_height);
        int[] location = new int[2];
        getLocationInWindow(location);
        return ((location[1] - indicatorPopupGap) - indicatorPopupSize) - statusbarSize;
    }

    public AnimatorSet getPageIndicatorAnimatorSet(final boolean isShowAnim, final Runnable runnableVisibility) {
        float f;
        float f2 = 1.0f;
        AnimatorSet animatorSet = new AnimatorSet();
        String name = View.ALPHA.getName();
        float[] fArr = new float[2];
        if (isShowAnim) {
            f = 0.0f;
        } else {
            f = 1.0f;
        }
        fArr[0] = f;
        fArr[1] = isShowAnim ? 1.0f : 0.0f;
        PropertyValuesHolder pvhShowAlpha = PropertyValuesHolder.ofFloat(name, fArr);
        name = View.SCALE_X.getName();
        fArr = new float[2];
        if (isShowAnim) {
            f = 0.5f;
        } else {
            f = 1.0f;
        }
        fArr[0] = f;
        fArr[1] = isShowAnim ? 1.0f : 0.5f;
        PropertyValuesHolder pvhShowScaleX = PropertyValuesHolder.ofFloat(name, fArr);
        name = View.SCALE_Y.getName();
        fArr = new float[2];
        if (isShowAnim) {
            f = 0.5f;
        } else {
            f = 1.0f;
        }
        fArr[0] = f;
        if (!isShowAnim) {
            f2 = 0.5f;
        }
        fArr[1] = f2;
        PropertyValuesHolder pvhShowScaleY = PropertyValuesHolder.ofFloat(name, fArr);
        animatorSet.setDuration(100);
        animatorSet.setInterpolator(ViInterpolator.getInterploator(15));
        animatorSet.play(ObjectAnimator.ofPropertyValuesHolder(this, new PropertyValuesHolder[]{pvhShowAlpha, pvhShowScaleX, pvhShowScaleY}));
        animatorSet.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animator) {
                PageIndicator.this.setVisibility(View.VISIBLE);
            }

            public void onAnimationEnd(Animator animation) {
                if (isShowAnim) {
                    runnableVisibility.run();
                } else {
                    PageIndicator.this.setVisibility(4);
                }
                PageIndicator.this.setScaleX(1.0f);
                PageIndicator.this.setScaleY(1.0f);
            }
        });
        return animatorSet;
    }

    public void clickPageIndicator(MotionEvent ev) {
        PageIndicatorMarker childView = (PageIndicatorMarker) getIndicatorChild(ev.getRawX());
        if (childView != null && ev.getAction() == 1) {
            childView.performClick();
        }
    }

    private View getIndicatorChild(float x) {
        int childCount = getChildCount();
        for (int childIndex = 0; childIndex < childCount; childIndex++) {
            if (isTouchedIndicatorChild(getChildAt(childIndex), x)) {
                return getChildAt(childIndex);
            }
        }
        return null;
    }

    private boolean isTouchedIndicatorChild(View childView, float x) {
        int[] coordinate = new int[2];
        childView.getLocationOnScreen(coordinate);
        float left = (float) coordinate[0];
        float right = left + ((float) childView.getWidth());
        if (x < left || x > right) {
            return false;
        }
        return true;
    }

    public void updateMarginForPageIndicator() {
        DeviceProfile dp = LauncherAppState.getInstance().getDeviceProfile();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        lp.rightMargin = 0;
        lp.leftMargin = 0;
        if (!dp.isLandscape) {
            return;
        }
        if (Utilities.getNavigationBarPositon() != 1) {
            lp.rightMargin += dp.navigationBarHeight / 2;
        } else if (!dp.isMultiwindowMode) {
            lp.leftMargin += dp.navigationBarHeight / 2;
        }
    }
}

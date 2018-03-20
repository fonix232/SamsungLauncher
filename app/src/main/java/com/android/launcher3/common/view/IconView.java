package com.android.launcher3.common.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.launcher3.BadgeInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.deviceprofile.GridIconInfo;
import com.android.launcher3.common.drawable.FastBitmapDrawable;
import com.android.launcher3.common.drawable.PreloadIconDrawable;
import com.android.launcher3.common.model.FavoritesUpdater;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.IconCache.IconLoadRequest;
import com.android.launcher3.common.model.PackageItemInfo;
import com.android.launcher3.common.multiselect.MultiSelectManager;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.theme.OpenThemeManager;
import com.android.launcher3.theme.OpenThemeManager.ThemeItems;
import com.android.launcher3.util.BitmapUtils;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.animation.AppIconBounceAnimation;
import com.android.launcher3.util.event.CheckLongKeyHelper;
import com.android.launcher3.util.event.CheckLongPressHelper;
import com.android.launcher3.util.event.StylusEventHelper;
import com.sec.android.app.launcher.R;

public class IconView extends FrameLayout implements Removable {
    private static final int CHECKBOX_ANIM_DURATION = 100;
    public static final int DISPLAY_APPS = 2;
    public static final int DISPLAY_FOLDER_ITEM = 3;
    public static final int DISPLAY_HOME_FOLDER_ITEM = 4;
    public static final int DISPLAY_HOTSEAT = 1;
    public static final int DISPLAY_WORKSPACE = 0;
    public static final String EXTRA_SHORTCUT_LIVE_ICON_COMPONENT = "liveicon_cmpname";
    public static final String EXTRA_SHORTCUT_USER_ID = "userid";
    private static final Boolean FEATURE_IS_TABLET = Boolean.valueOf(LauncherFeature.isTablet());
    private static final int INVALID_DATA = -1;
    public static final String KNOX_SHORTCUT_PACKAGE = "com.samsung.knox.rcp.components";
    private static final float NOTI_BADGE_POSITION_TOP_FACTOR = 0.27f;
    private static final float NOTI_BADGE_POSITION_TOP_FACTOR_FOR_LANDSCAPE = 0.085f;
    private static final float NOTI_DOT_BADGE_POSITION_RIGHT_FACTOR = (FEATURE_IS_TABLET.booleanValue() ? 0.36f : 0.32f);
    private static final float NOTI_DOT_BADGE_POSITION_TOP_FACTOR = 0.5f;
    private static final int SCALE_ANIMATION_DURATION = 300;
    private static final Interpolator SINE_IN_OUT_80 = ViInterpolator.getInterploator(34);
    private static final String TAG = IconView.class.getSimpleName();
    private static float mPositionHorizontalFactor = -1.0f;
    private static float mPositionVerticalFactor = -1.0f;
    private final float ICON_NORMAL_ALPHA;
    protected TextView mBadgeView;
    private AppIconBounceAnimation mBounceAnim;
    private CheckBox mCheckBox;
    protected TextView mCountBadgeView;
    private boolean mDisableRelayout;
    protected int mDrawablePadding;
    private Drawable mIcon;
    private int mIconDisplay;
    private IconLoadRequest mIconLoadRequest;
    private Drawable mIconShadowDrawable;
    protected int mIconSize;
    protected Drawable mIconTextBackground;
    protected ImageView mIconView;
    private boolean mIsBadgeHidden;
    protected boolean mIsPhoneLandscape;
    private boolean mIsPressedEnterKey;
    protected final Launcher mLauncher;
    private final CheckLongKeyHelper mLongKeyHelper;
    private final CheckLongPressHelper mLongPressHelper;
    private boolean mMarkToRemove;
    private ImageView mShadow;
    private float mSlop;
    private final StylusEventHelper mStylusEventHelper;
    protected TextView mTitleView;

    public IconView(Context context) {
        this(context, null, 0);
    }

    public IconView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.ICON_NORMAL_ALPHA = 1.0f;
        this.mDisableRelayout = false;
        this.mIsBadgeHidden = false;
        this.mMarkToRemove = false;
        this.mIsPressedEnterKey = false;
        this.mLauncher = (Launcher) context;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.IconView, defStyle, 0);
        this.mIconDisplay = a.getInteger(1, -1);
        this.mIconSize = a.getDimensionPixelSize(0, 0);
        a.recycle();
        this.mLongPressHelper = new CheckLongPressHelper(this);
        this.mLongKeyHelper = new CheckLongKeyHelper(this, this.mLauncher);
        this.mStylusEventHelper = new StylusEventHelper(this);
        if (OpenThemeManager.getInstance().getBoolean(ThemeItems.SHADOW.value(), false)) {
            this.mIconShadowDrawable = OpenThemeManager.getInstance().getPreloadDrawable(ThemeItems.SHADOW.value());
        }
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mIconView = (ImageView) findViewById(R.id.iconview_imageView);
        this.mTitleView = (TextView) findViewById(R.id.iconview_titleView);
        this.mShadow = (ImageView) findViewById(R.id.iconview_shadow);
        this.mCountBadgeView = (TextView) findViewById(R.id.iconview_count_badge);
        this.mBadgeView = (TextView) findViewById(R.id.iconview_badge);
        if (LauncherAppState.getInstance().isEasyModeEnabled()) {
            this.mBadgeView.setBackground(getResources().getDrawable(R.drawable.homescreen_badge_easymode, null));
            this.mBadgeView.setTextSize(0, getResources().getDimension(R.dimen.badge_text_size_easymode));
        }
        this.mCheckBox = (CheckBox) findViewById(R.id.iconview_checkbox);
        if (this.mCheckBox != null) {
            this.mCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    MultiSelectManager multiSelectManager = IconView.this.mLauncher.getMultiSelectManager();
                    boolean checked = isChecked && multiSelectManager != null && multiSelectManager.canSelectItem();
                    if (isChecked && !checked) {
                        buttonView.setChecked(checked);
                    }
                    ((ItemInfo) IconView.this.getTag()).setChecked(checked);
                    IconView.this.mLauncher.getStageManager().onCheckedChanged(IconView.this, checked);
                    IconView.this.updateContentDescription(true);
                }
            });
        }
        if (mPositionHorizontalFactor == -1.0f) {
            mPositionHorizontalFactor = ((float) getResources().getInteger(R.integer.config_badgePositionHorizontalPermil)) / 1000.0f;
        }
        if (mPositionVerticalFactor == -1.0f) {
            mPositionVerticalFactor = ((float) getResources().getInteger(R.integer.config_badgePositionVerticalPermil)) / 1000.0f;
        }
        applyStyle();
        setClipToPadding(false);
        decorateViewComponent();
    }

    public void applyStyle() {
        GridIconInfo iconInfo = getIconInfo();
        if (iconInfo != null) {
            boolean z;
            if (!getStateOrientation() || FEATURE_IS_TABLET.booleanValue()) {
                z = false;
            } else {
                z = true;
            }
            this.mIsPhoneLandscape = z;
            this.mIconSize = iconInfo.getIconSize();
            this.mDrawablePadding = iconInfo.getDrawablePadding();
            this.mTitleView.setTextSize(0, (float) iconInfo.getTextSize());
            this.mTitleView.setMaxLines(iconInfo.getLineCount());
            this.mTitleView.setGravity(this.mIsPhoneLandscape ? 51 : 49);
            if (iconInfo.getLineCount() == 0) {
                this.mTitleView.setVisibility(View.GONE);
            } else {
                this.mTitleView.setVisibility(View.VISIBLE);
            }
            int contentTop = iconInfo.getContentTop();
            int iconStartPadding = iconInfo.getIconStartPadding();
            LayoutParams lpIconView = (LayoutParams) this.mIconView.getLayoutParams();
            if (lpIconView != null) {
                applyStyleIconView(contentTop, iconStartPadding, lpIconView);
                applyStyleTitleView(contentTop, iconStartPadding, (LayoutParams) this.mTitleView.getLayoutParams());
            }
            if (!(this.mCheckBox == null || this.mCheckBox.getButtonDrawable() == null)) {
                applyStyleCheckBox(contentTop, (LayoutParams) this.mCheckBox.getLayoutParams());
            }
            if (!(this.mCountBadgeView == null || this.mCountBadgeView.getBackground() == null)) {
                applyStyleCountBadgeView(contentTop, (LayoutParams) this.mCountBadgeView.getLayoutParams());
            }
            if (this.mShadow != null && this.mIconShadowDrawable != null) {
                applyStyleShadow(contentTop, (LayoutParams) this.mShadow.getLayoutParams());
            }
        }
    }

    public boolean isLandscape() {
        return this.mIsPhoneLandscape;
    }

    private boolean getStateOrientation() {
        LauncherAppState appState = LauncherAppState.getInstance();
        DeviceProfile deviceProfile = appState != null ? appState.getDeviceProfile() : null;
        return deviceProfile != null && deviceProfile.isLandscape;
    }

    private void applyStyleIconView(int contentTop, int leftMargin, LayoutParams lp) {
        if (lp == null) {
            return;
        }
        if (this.mIsPhoneLandscape) {
            lp.gravity = 19;
            lp.setMargins(leftMargin, 0, 0, 0);
            if (Utilities.sIsRtl) {
                lp.setMarginStart(0);
                lp.setMarginEnd(leftMargin);
                return;
            }
            lp.setMarginStart(leftMargin);
            lp.setMarginEnd(0);
            return;
        }
        lp.gravity = 49;
        lp.setMargins(0, contentTop, 0, 0);
        lp.setMarginStart(0);
        lp.setMarginEnd(0);
    }

    protected void applyStyleTitleView(int contentTop, int leftMargin, LayoutParams lp) {
        if (lp != null) {
            int offset = this.mIconSize + this.mDrawablePadding;
            if (this.mIsPhoneLandscape) {
                lp.gravity = 19;
                lp.setMargins(leftMargin + offset, 0, 0, 0);
                if (Utilities.sIsRtl) {
                    lp.setMarginStart(0);
                    lp.setMarginEnd(leftMargin + offset);
                    return;
                }
                lp.setMarginStart(leftMargin + offset);
                lp.setMarginEnd(0);
                return;
            }
            lp.gravity = 49;
            lp.setMargins(0, contentTop + offset, 0, 0);
            lp.setMarginStart(0);
            lp.setMarginEnd(0);
        }
    }

    private void applyStyleCheckBox(int contentTop, LayoutParams lpCheckBox) {
        if (lpCheckBox != null) {
            int hMargin;
            int vMargin;
            try {
                this.mCheckBox.semSetButtonDrawableSize(getResources().getDimensionPixelSize(R.dimen.multi_select_checkbox_width), getResources().getDimensionPixelSize(R.dimen.multi_select_checkbox_height));
            } catch (NoSuchMethodError e) {
                Log.e(TAG, e.toString());
            }
            if (this.mCheckBox.getButtonDrawable() != null) {
                hMargin = (int) (((float) this.mCheckBox.getButtonDrawable().getIntrinsicWidth()) * mPositionHorizontalFactor);
            } else {
                hMargin = 0;
            }
            if (this.mCheckBox.getButtonDrawable() != null) {
                vMargin = (int) (((float) this.mCheckBox.getButtonDrawable().getIntrinsicWidth()) * mPositionVerticalFactor);
            } else {
                vMargin = 0;
            }
            int topMargin = contentTop - vMargin;
            if (topMargin < 0) {
                topMargin = 0;
            }
            lpCheckBox.topMargin = topMargin;
            if (this.mIsPhoneLandscape) {
                lpCheckBox.gravity = 51;
                if (Utilities.sIsRtl) {
                    lpCheckBox.leftMargin = 0;
                    return;
                } else {
                    lpCheckBox.rightMargin = 0;
                    return;
                }
            }
            lpCheckBox.gravity = 49;
            if (Utilities.sIsRtl) {
                lpCheckBox.leftMargin = (this.mIconSize / 2) - hMargin;
            } else {
                lpCheckBox.rightMargin = (this.mIconSize / 2) - hMargin;
            }
        }
    }

    private void applyStyleCountBadgeView(int contentTop, LayoutParams lpCountBadgeView) {
        if (lpCountBadgeView != null) {
            int margin = this.mCountBadgeView.getBackground().getIntrinsicWidth() / 4;
            int topMargin = contentTop - margin;
            if (topMargin < 0) {
                topMargin = 0;
            }
            lpCountBadgeView.topMargin = topMargin;
            if (this.mIsPhoneLandscape) {
                lpCountBadgeView.gravity = 51;
                if (Utilities.sIsRtl) {
                    lpCountBadgeView.rightMargin = (this.mIconSize / 2) + (margin * 2);
                    return;
                } else {
                    lpCountBadgeView.leftMargin = (this.mIconSize / 2) + (margin * 2);
                    return;
                }
            }
            lpCountBadgeView.gravity = 49;
            if (Utilities.sIsRtl) {
                lpCountBadgeView.rightMargin = (this.mIconSize / 2) - margin;
            } else {
                lpCountBadgeView.leftMargin = (this.mIconSize / 2) - margin;
            }
        }
    }

    private void applyStyleShadow(int contentTop, LayoutParams lpShadow) {
        if (lpShadow != null) {
            lpShadow.topMargin = ((this.mIconSize * 4) / 5) + contentTop;
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            updateBadgeLayout();
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    protected void updateBadgeLayout() {
        boolean isBadgeWithDot = true;
        GridIconInfo iconInfo = getIconInfo();
        if (iconInfo == null) {
            Log.d(TAG, "updateBadgeLayout is not able to continue due to the null pointer");
        } else if (this.mBadgeView != null && this.mBadgeView.getBackground() != null && (getTag() instanceof ItemInfo) && ((ItemInfo) getTag()).mBadgeCount != 0) {
            int contentTop = iconInfo.getContentTop();
            LayoutParams lp = (LayoutParams) this.mBadgeView.getLayoutParams();
            if (lp != null) {
                int topValue;
                int margin = this.mBadgeView.getBackground().getIntrinsicWidth() / 4;
                if (Utilities.getBadgeSettingValue(getContext()) != 1) {
                    isBadgeWithDot = false;
                }
                if (this.mIsPhoneLandscape) {
                    lp.gravity = 51;
                    if (Utilities.sIsRtl) {
                        lp.rightMargin = (this.mIconSize / 2) + (margin * 2);
                    } else if (isBadgeWithDot) {
                        lp.leftMargin = this.mIconSize - ((int) (((float) this.mBadgeView.getBackground().getIntrinsicWidth()) * NOTI_DOT_BADGE_POSITION_RIGHT_FACTOR));
                    } else {
                        lp.leftMargin = (this.mIconSize / 2) + (this.mIconSize / 4);
                    }
                } else {
                    lp.gravity = 49;
                    if (Utilities.sIsRtl) {
                        lp.rightMargin = (this.mIconSize / 2) - margin;
                    } else if (isBadgeWithDot) {
                        lp.leftMargin = (this.mIconSize / 2) - ((int) (((float) this.mBadgeView.getBackground().getIntrinsicWidth()) * NOTI_DOT_BADGE_POSITION_RIGHT_FACTOR));
                    } else {
                        lp.leftMargin = (this.mIconSize / 2) - margin;
                    }
                }
                if (isBadgeWithDot) {
                    topValue = (int) (((float) this.mBadgeView.getBackground().getIntrinsicWidth()) * NOTI_DOT_BADGE_POSITION_TOP_FACTOR);
                } else if (this.mIsPhoneLandscape) {
                    topValue = (int) (((float) this.mBadgeView.getBackground().getIntrinsicWidth()) * NOTI_BADGE_POSITION_TOP_FACTOR_FOR_LANDSCAPE);
                } else {
                    topValue = (int) (((float) this.mBadgeView.getBackground().getIntrinsicWidth()) * NOTI_BADGE_POSITION_TOP_FACTOR);
                }
                int topMargin = contentTop - topValue;
                if (topMargin < 0) {
                    topMargin = 0;
                }
                lp.topMargin = topMargin;
            }
            this.mBadgeView.setLayoutParams(lp);
        }
    }

    public GridIconInfo getIconInfo() {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        switch (this.mIconDisplay) {
            case 0:
                return dp.homeGrid.getIconInfo();
            case 1:
                return dp.hotseatGridIcon;
            case 2:
                return dp.appsGrid.getIconInfo();
            case 3:
            case 4:
                return dp.folderGrid.getIconInfo();
            default:
                return null;
        }
    }

    public void setIconDisplay(int iconDisplay) {
        if (this.mIconDisplay != iconDisplay) {
            this.mIconDisplay = iconDisplay;
            applyStyle();
            if (getTag() instanceof IconInfo) {
                reapplyItemInfo((ItemInfo) getTag());
            }
        }
    }

    protected void decorateViewComponent() {
        OpenThemeManager themeManager = OpenThemeManager.getInstance();
        int titleColor = themeManager.getPreloadColor(ThemeItems.HOME_TEXT_COLOR.value());
        int titleHighlightColor = themeManager.getPreloadColor(ThemeItems.TEXT_HIGHLIGHT.value());
        int titleShadowColor = themeManager.getPreloadColor(ThemeItems.TEXT_SHADOW_COLOR.value());
        if (this.mIconTextBackground == null) {
            this.mIconTextBackground = themeManager.getPreloadDrawable(ThemeItems.TITLE_BACKGROUND.value());
        }
        if (!(!themeManager.isPinkTheme() && WhiteBgManager.isWhiteBg() && this.mIconTextBackground == null)) {
            if (titleColor != 33554431) {
                setTextColor(titleColor);
            }
            if (titleHighlightColor != 33554431) {
                this.mTitleView.setHighlightColor(titleHighlightColor);
            }
            if (titleShadowColor != 33554431) {
                setShadowLayer(this.mTitleView.getShadowRadius(), this.mTitleView.getShadowDx(), this.mTitleView.getShadowDy(), titleShadowColor);
            } else {
                setShadowLayer(0.0f, 0.0f, 0.0f, 0);
            }
        }
        TextView badge = (TextView) findViewById(R.id.iconview_badge);
        if (badge != null) {
            int badgeFontColor = themeManager.getPreloadColor(ThemeItems.BADGE_TEXT_COLOR.value());
            if (badgeFontColor == 33554431) {
                badgeFontColor = getResources().getColor(R.color.badge_text_color, null);
            }
            badge.setTextColor(badgeFontColor);
        }
    }

    protected boolean applyKnoxLiveIcon(IconInfo info) {
        if (!LiveIconManager.isKnoxLiveIcon(info.intent)) {
            return false;
        }
        info.mIcon = LiveIconManager.getLiveIcon(this.mLauncher, info.intent.getStringExtra(EXTRA_SHORTCUT_LIVE_ICON_COMPONENT).split("/")[0], info.user);
        return true;
    }

    public void applyFromShortcutInfo(IconInfo info, IconCache iconCache) {
        applyFromShortcutInfo(info, iconCache, false);
    }

    public void applyFromShortcutInfo(IconInfo info, IconCache iconCache, boolean promiseStateChanged) {
        Bitmap b;
        if (LiveIconManager.applyKnoxLiveIcon(this.mLauncher, info)) {
            b = info.mIcon;
        } else if (LiveIconManager.isLiveIconPackage(info)) {
            String packageName = info.intent.getComponent().getPackageName();
            Log.i("IconView", "applyFromShortcutInfo - start GetLive : " + packageName);
            if (info.itemType == 6) {
                info.updateDeepShortcutIcon(this.mLauncher);
                b = info.getIcon(iconCache);
            } else {
                b = LiveIconManager.getLiveIcon(this.mLauncher, packageName, info.user);
            }
            Log.i("IconView", "applyFromShortcutInfo - end GetLive : " + packageName);
        } else if (info.isAppsButton) {
            b = OpenThemeManager.getInstance().getThemeAppIcon();
        } else {
            b = info.getIcon(iconCache);
        }
        refreshIcon(info, promiseStateChanged, b);
        changeTextColorForBg(WhiteBgManager.isWhiteBg());
        if (Utilities.isNeededToTestLauncherResume()) {
            Utilities.printCallStack("home");
        }
    }

    public void applyFromApplicationInfo(IconInfo info) {
        applyFromApplicationInfo(info, false);
    }

    public void applyFromApplicationInfo(IconInfo info, boolean promiseStateChanged) {
        if (LiveIconManager.isLiveIconPackage(info)) {
            String packageName = info.intent.getComponent().getPackageName();
            Log.i("IconView", "applyFromApplicationInfo - start GetLive : " + packageName);
            info.setIcon(LiveIconManager.getLiveIcon(this.mLauncher, packageName, info.user));
            Log.i("IconView", "applyFromApplicationInfo - end GetLive : " + packageName);
        }
        if (info.mIcon == null) {
            info.setIcon(info.getIcon(LauncherAppState.getInstance().getIconCache()));
        }
        refreshIcon(info, promiseStateChanged, info.mIcon);
        changeTextColorForBg(false);
        if (Utilities.isNeededToTestLauncherResume()) {
            Utilities.printCallStack("apps");
        }
    }

    void refreshIcon(IconInfo info, boolean promiseStateChanged, Bitmap b) {
        FastBitmapDrawable iconDrawable;
        if ((info.isDisabled & 32) != 0) {
            iconDrawable = BitmapUtils.createIconDrawable(LauncherAppState.getInstance().getIconCache().getSDCardBitmap(), this.mIconSize);
        } else {
            iconDrawable = BitmapUtils.createIconDrawable(b, this.mIconSize);
            iconDrawable.setGhostModeEnabled(info.isDisabled != 0);
        }
        setIcon(info, iconDrawable);
        if (info.contentDescription != null) {
            setContentDescription(info.contentDescription);
        }
        setText(info.title);
        setTag(info);
        if (info.isAppsButton) {
            this.mCheckBox = null;
        }
        if (promiseStateChanged || info.isPromise()) {
            applyState(promiseStateChanged);
        }
        refreshBadge();
    }

    public int getIconSize() {
        return this.mIconSize;
    }

    public void setLongPressTimeout(int longPressTimeout) {
        this.mLongPressHelper.setLongPressTimeout(longPressTimeout);
    }

    public void setTag(Object tag) {
        if (tag instanceof ItemInfo) {
            ItemInfo itemInfo = (ItemInfo) tag;
            boolean isAppItem = itemInfo.container == -102 || itemInfo.ignoreCheckItemInfo;
            if (!isAppItem) {
                FavoritesUpdater.checkItemInfo((ItemInfo) tag);
            }
        }
        super.setTag(tag);
    }

    public Drawable getIcon() {
        return this.mIcon;
    }

    protected AppIconBounceAnimation getBounceAnimation() {
        if (this.mIconView != null) {
            return new AppIconBounceAnimation(this.mIconView);
        }
        return null;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean result = super.onInterceptTouchEvent(event);
        this.mIsPressedEnterKey = false;
        switch (event.getAction()) {
            case 0:
                if (Utilities.startActivityTouchDown(this.mLauncher, this)) {
                    return false;
                }
                return result;
            default:
                return result;
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (onTouchOutofIconArea(event)) {
            cancelLongPress();
            return true;
        }
        boolean result = super.onTouchEvent(event);
        if (this.mStylusEventHelper.checkAndPerformStylusEvent(event)) {
            this.mLongPressHelper.cancelLongPress();
            result = true;
        }
        MultiSelectManager multiSelectMgr = this.mLauncher.getMultiSelectManager();
        switch (event.getAction()) {
            case 0:
                if (multiSelectMgr == null || !multiSelectMgr.isMultiSelectMode()) {
                    this.mBounceAnim = getBounceAnimation();
                    if (this.mBounceAnim != null) {
                        this.mBounceAnim.animateDown();
                    }
                }
                if (this.mStylusEventHelper.inStylusButtonPressed()) {
                    return result;
                }
                this.mLongPressHelper.postCheckForLongPress();
                return result;
            case 1:
            case 3:
                if (multiSelectMgr == null || !multiSelectMgr.isMultiSelectMode()) {
                    if (this.mBounceAnim != null) {
                        this.mBounceAnim.animateUp();
                    }
                    setAlpha(1.0f);
                }
                this.mBounceAnim = null;
                this.mLongPressHelper.cancelLongPress();
                return result;
            case 2:
                if (Utilities.pointInView(this, event.getX(), event.getY(), this.mSlop)) {
                    return result;
                }
                this.mLongPressHelper.cancelLongPress();
                return result;
            default:
                return result;
        }
    }

    public void draw(Canvas canvas) {
        drawTextBackground(canvas);
        if (this.mTitleView.getCurrentTextColor() == getResources().getColor(17170445, null)) {
            this.mTitleView.getPaint().clearShadowLayer();
            super.draw(canvas);
            return;
        }
        super.draw(canvas);
    }

    protected void drawTextBackground(Canvas canvas) {
        if (this.mTitleView.getText() != null && this.mIconTextBackground != null && this.mTitleView.getText().length() > 0) {
            int top;
            int bottom;
            int startMargin;
            int extraPadding = OpenThemeManager.getInstance().mTextBackgroundExtraPadding;
            int extraPaddingBottom = OpenThemeManager.getInstance().mTextBackgroundExtraPaddingBottom;
            int width = 0;
            int textLineCount = this.mTitleView.getLineCount();
            int textLineHeight = this.mTitleView.getLineHeight();
            LayoutParams lp = (LayoutParams) this.mIconView.getLayoutParams();
            Layout layout = this.mTitleView.getLayout();
            for (int i = 0; i < textLineCount; i++) {
                int lineWidth = (int) layout.getLineWidth(i);
                if (width < lineWidth) {
                    width = lineWidth;
                }
            }
            width += extraPadding;
            if (this.mIsPhoneLandscape) {
                top = ((getHeight() - (textLineCount * textLineHeight)) - extraPadding) / 2;
                bottom = (getHeight() - top) + (extraPadding / 2);
                startMargin = ((lp.leftMargin + this.mIconSize) + this.mDrawablePadding) - (extraPadding / 2);
                if (getWidth() < startMargin + width) {
                    width = getWidth() - startMargin;
                }
            } else {
                top = ((lp.topMargin + this.mIconSize) + this.mTitleView.getExtendedPaddingTop()) + this.mDrawablePadding;
                bottom = ((textLineCount * textLineHeight) + top) + extraPaddingBottom;
                startMargin = (getWidth() - width) / 2;
                if (getWidth() < width) {
                    width = getWidth();
                    startMargin = 0;
                }
                if (bottom > getHeight()) {
                    bottom = getHeight();
                }
            }
            this.mIconTextBackground.setBounds(startMargin, top, startMargin + width, bottom);
            this.mIconTextBackground.draw(canvas);
        }
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mSlop = (float) ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    public void setTextColor(int color) {
        this.mTitleView.setTextColor(color);
    }

    public void setTextColor(ColorStateList colors) {
        this.mTitleView.setTextColor(colors);
    }

    public void setShadowLayer(float radius, float dx, float dy, int color) {
        this.mTitleView.setShadowLayer(radius, dx, dy, color);
    }

    public void cancelLongPress() {
        super.cancelLongPress();
        this.mLongPressHelper.cancelLongPress();
    }

    public void applyState(boolean promiseStateChanged) {
        if (getTag() instanceof IconInfo) {
            IconInfo info = (IconInfo) getTag();
            int progressLevel = info.isPromise() ? info.hasStatusFlag(8) ? info.getInstallProgress() : 0 : 100;
            if (this.mIcon != null) {
                PreloadIconDrawable preloadDrawable;
                if (this.mIcon instanceof PreloadIconDrawable) {
                    preloadDrawable = (PreloadIconDrawable) this.mIcon;
                } else {
                    preloadDrawable = new PreloadIconDrawable(this.mIcon);
                    setIcon(info, preloadDrawable);
                }
                preloadDrawable.setLevel(progressLevel);
                if (promiseStateChanged) {
                    preloadDrawable.maybePerformFinishedAnimation();
                }
            }
        }
    }

    protected void setShadow() {
        if (this.mIconDisplay == 0 && this.mIconShadowDrawable != null && this.mShadow != null) {
            this.mShadow.setImageBitmap(BitmapUtils.createIconBitmap(this.mIconShadowDrawable, getContext(), this.mIconSize, this.mIconSize / 3));
        }
    }

    private void setIcon(ItemInfo info, Drawable icon) {
        setShadow();
        if (this.mIconView != null) {
            IconInfo iconInfo = null;
            if (info instanceof IconInfo) {
                iconInfo = (IconInfo) info;
            }
            if (!(icon instanceof FastBitmapDrawable) || iconInfo == null || iconInfo.itemType != 1 || iconInfo.isAppsButton || iconInfo.isAppShortcut || LiveIconManager.isKnoxLiveIcon(iconInfo.intent)) {
                this.mIcon = icon;
            } else {
                this.mIcon = BitmapUtils.createIconDrawable(OpenThemeManager.getInstance().getIconWithTrayIfNeeded(((FastBitmapDrawable) icon).getBitmap(), this.mIconSize, false), this.mIconSize);
            }
            this.mIconView.setImageDrawable(this.mIcon);
        }
    }

    public Drawable setIcon(Drawable icon) {
        this.mIcon = icon;
        if (this.mIconView != null) {
            this.mIconView.setImageDrawable(this.mIcon);
        }
        return icon;
    }

    public void onLiveIconRefresh() {
        if (getTag() instanceof IconInfo) {
            IconInfo iconInfo = (IconInfo) getTag();
            if (LiveIconManager.isLiveIconPackage(iconInfo)) {
                FastBitmapDrawable iconDrawable;
                if (iconInfo.itemType == 6) {
                    iconInfo.updateDeepShortcutIcon(this.mLauncher);
                } else {
                    iconInfo.setIcon(LiveIconManager.getLiveIcon(this.mLauncher, iconInfo.intent.getComponent().getPackageName(), iconInfo.user));
                }
                if ((iconInfo.isDisabled & 32) != 0) {
                    iconDrawable = BitmapUtils.createIconDrawable(LauncherAppState.getInstance().getIconCache().getSDCardBitmap(), this.mIconSize);
                } else {
                    iconDrawable = BitmapUtils.createIconDrawable(iconInfo.mIcon, this.mIconSize);
                    iconDrawable.setGhostModeEnabled(iconInfo.isDisabled != 0);
                }
                setIcon(iconInfo, iconDrawable);
            }
        }
    }

    public void requestLayout() {
        if (!this.mDisableRelayout) {
            super.requestLayout();
        }
    }

    public void reapplyItemInfo(ItemInfo info) {
        if (getTag() == info) {
            this.mDisableRelayout = true;
            if (info instanceof IconInfo) {
                if (info.isContainApps() || (info.container >= 0 && this.mLauncher.getHomeController().getHomescreenIconByItemId(info.container) == null)) {
                    applyFromApplicationInfo((IconInfo) info);
                } else {
                    applyFromShortcutInfo((IconInfo) info, LauncherAppState.getInstance().getIconCache());
                }
            }
            this.mDisableRelayout = false;
        }
    }

    public void reapplyItemInfoFromIconCache(ItemInfo info) {
        if (getTag() == info) {
            this.mDisableRelayout = true;
            if (info instanceof IconInfo) {
                if (info.isContainApps()) {
                    applyFromApplicationInfo((IconInfo) info);
                } else {
                    applyFromShortcutInfo((IconInfo) info, LauncherAppState.getInstance().getIconCache());
                }
                if (info.rank < 9 && info.container >= 0) {
                    View folderIcon = this.mLauncher.getHomeController().getHomescreenIconByItemId(info.container);
                    if (folderIcon != null) {
                        folderIcon.invalidate();
                    }
                }
            }
            this.mDisableRelayout = false;
        }
    }

    public void verifyHighRes() {
        if (this.mIconLoadRequest != null) {
            this.mIconLoadRequest.cancel();
            this.mIconLoadRequest = null;
        }
        if (getTag() instanceof IconInfo) {
            IconInfo info = (IconInfo) getTag();
            if (info.usingLowResIcon) {
                this.mIconLoadRequest = LauncherAppState.getInstance().getIconCache().updateIconInBackground(this, info);
            }
        } else if (getTag() instanceof PackageItemInfo) {
            PackageItemInfo info2 = (PackageItemInfo) getTag();
            if (info2.usingLowResIcon) {
                this.mIconLoadRequest = LauncherAppState.getInstance().getIconCache().updateIconInBackground(this, info2);
            }
        }
    }

    public void refreshBadge() {
        if (this.mBadgeView != null) {
            MultiSelectManager multiSelectMgr = this.mLauncher.getMultiSelectManager();
            boolean multiSelectMode = multiSelectMgr != null && multiSelectMgr.isMultiSelectMode();
            if (this.mIsBadgeHidden || !(getTag() instanceof IconInfo) || ((IconInfo) getTag()).mBadgeCount <= 0 || !((IconInfo) getTag()).mShowBadge) {
                setBadgeViewToInvisible(multiSelectMode);
                return;
            }
            IconInfo iconInfo = (IconInfo) getTag();
            int badge = iconInfo.mBadgeCount;
            if (badge >= 1000) {
                badge = BadgeInfo.MAX_COUNT;
            }
            if (badge == 1) {
                setContentDescription(iconInfo.contentDescription + ", " + getResources().getString(R.string.new_notification));
            } else {
                setContentDescription(iconInfo.contentDescription + ", " + String.format(getResources().getString(R.string.new_notifications), new Object[]{Integer.valueOf(badge)}));
            }
            int badgeSettingValue = Utilities.getBadgeSettingValue(getContext());
            Drawable badgeBgDrawable = getBadgeBgDrawable(badgeSettingValue);
            if (badgeBgDrawable != null) {
                this.mBadgeView.setBackground(badgeBgDrawable);
            }
            if (badgeSettingValue == 1) {
                this.mBadgeView.setTextSize(0.0f);
                this.mBadgeView.setPadding(0, 0, 0, 0);
                this.mBadgeView.setWidth(getResources().getDimensionPixelSize(R.dimen.badge_dot_icon_size));
                this.mBadgeView.setHeight(getResources().getDimensionPixelSize(R.dimen.badge_dot_icon_size));
            } else {
                String badgeCount = String.valueOf(badge);
                String currentLanguage = Utilities.getLocale(this.mLauncher).getLanguage();
                if ("ar".equals(currentLanguage) || "fa".equals(currentLanguage)) {
                    badgeCount = Utilities.toArabicDigits(badgeCount, currentLanguage);
                } else {
                    badgeCount = String.valueOf(badgeCount);
                }
                this.mBadgeView.setText(badgeCount);
                int width;
                if (LauncherAppState.getInstance().isEasyModeEnabled()) {
                    this.mBadgeView.setTextSize(0, getResources().getDimension(R.dimen.badge_text_size_easymode));
                    if (badge > 99) {
                        width = getResources().getDimensionPixelSize(R.dimen.badge_icon_width_three_number_easymode);
                    } else if (badge > 9) {
                        width = getResources().getDimensionPixelSize(R.dimen.badge_icon_width_two_number_easymode);
                    } else {
                        width = getResources().getDimensionPixelSize(R.dimen.badge_icon_size_easymode);
                    }
                    this.mBadgeView.setWidth(width);
                    this.mBadgeView.setHeight(getResources().getDimensionPixelSize(R.dimen.badge_icon_size_easymode));
                } else {
                    this.mBadgeView.setTextSize(0, getResources().getDimension(R.dimen.badge_text_size));
                    if (FEATURE_IS_TABLET.booleanValue()) {
                        if (badge > 99) {
                            width = getResources().getDimensionPixelSize(R.dimen.badge_icon_width_three_number);
                        } else {
                            width = getResources().getDimensionPixelSize(R.dimen.badge_icon_size);
                        }
                        this.mBadgeView.setWidth(width);
                    } else {
                        if (badge > 99) {
                            width = getResources().getDimensionPixelSize(R.dimen.badge_icon_width_three_number);
                        } else if (badge > 9) {
                            width = getResources().getDimensionPixelSize(R.dimen.badge_icon_width_two_number);
                        } else {
                            width = getResources().getDimensionPixelSize(R.dimen.badge_icon_size);
                        }
                        this.mBadgeView.setWidth(width);
                    }
                    this.mBadgeView.setHeight(getResources().getDimensionPixelSize(R.dimen.badge_icon_size));
                }
            }
            updateBadgeLayout();
            if (multiSelectMode) {
                setBadgeViewToInvisible(multiSelectMode);
            } else {
                this.mBadgeView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setBadgeViewToInvisible(boolean multiSelectMode) {
        this.mBadgeView.setVisibility(4);
        if (getTag() instanceof IconInfo) {
            IconInfo info = (IconInfo) getTag();
            if (info.contentDescription == null) {
                return;
            }
            if (multiSelectMode) {
                String string;
                StringBuilder append = new StringBuilder().append(info.contentDescription).append(", ");
                if (this.mCheckBox.isChecked()) {
                    string = getResources().getString(R.string.selected);
                } else {
                    string = getResources().getString(R.string.not_selected);
                }
                setContentDescription(append.append(string).toString());
                return;
            }
            setContentDescription(info.contentDescription);
        }
    }

    public void setIsBadgeHidden(boolean hidden) {
        this.mIsBadgeHidden = hidden;
    }

    protected Drawable getBadgeBgDrawable(int badgeSettingValue) {
        boolean isGrey = isGreyIcon();
        boolean isEasyMode = LauncherAppState.getInstance().isEasyModeEnabled();
        Drawable badgeBgDrawable;
        if (badgeSettingValue == 1) {
            if (!OpenThemeManager.getInstance().isDefaultTheme()) {
                badgeBgDrawable = OpenThemeManager.getInstance().preloadBadgeDrawable(badgeSettingValue);
            } else if (isEasyMode) {
                badgeBgDrawable = getResources().getDrawable(R.drawable.tw_noti_badge_mtrl_easymode, null);
            } else if (this.mIsPhoneLandscape) {
                badgeBgDrawable = getResources().getDrawable(R.drawable.tw_noti_badge_mtrl_land, null);
            } else {
                badgeBgDrawable = getResources().getDrawable(R.drawable.tw_noti_badge_mtrl, null);
            }
            if (!isGrey) {
                return badgeBgDrawable;
            }
            if (isEasyMode) {
                return getResources().getDrawable(R.drawable.tw_noti_badge_mtrl_easymode_grey, null);
            }
            if (this.mIsPhoneLandscape) {
                return getResources().getDrawable(R.drawable.tw_noti_badge_mtrl_grey_land, null);
            }
            return getResources().getDrawable(R.drawable.tw_noti_badge_mtrl_grey, null);
        }
        if (!OpenThemeManager.getInstance().isDefaultTheme()) {
            badgeBgDrawable = OpenThemeManager.getInstance().preloadBadgeDrawable(badgeSettingValue);
        } else if (isEasyMode) {
            if (this.mIsPhoneLandscape) {
                badgeBgDrawable = getResources().getDrawable(R.drawable.homescreen_badge_easymode_land, null);
            } else {
                badgeBgDrawable = getResources().getDrawable(R.drawable.homescreen_badge_easymode, null);
            }
        } else if (this.mIsPhoneLandscape) {
            badgeBgDrawable = getResources().getDrawable(R.drawable.homescreen_badge_land, null);
        } else {
            badgeBgDrawable = getResources().getDrawable(R.drawable.homescreen_badge, null);
        }
        if (!isGrey) {
            return badgeBgDrawable;
        }
        if (isEasyMode) {
            if (this.mIsPhoneLandscape) {
                return getResources().getDrawable(R.drawable.homescreen_badge_easymode_grey_land, null);
            }
            return getResources().getDrawable(R.drawable.homescreen_badge_easymode_grey, null);
        } else if (this.mIsPhoneLandscape) {
            return getResources().getDrawable(R.drawable.homescreen_badge_grey_land, null);
        } else {
            return getResources().getDrawable(R.drawable.homescreen_badge_grey, null);
        }
    }

    protected boolean isGreyIcon() {
        return UserManagerCompat.getInstance(getContext()).isQuietModeEnabled(((IconInfo) getTag()).getUserHandle());
    }

    public void animateBadge(boolean visible, int duration, long delay, boolean animated) {
        animateEachScale(this.mBadgeView, visible, duration, delay, animated);
    }

    public void animateTitleView(boolean visible, int duration, long delay, boolean animated) {
        animateEachScale(this.mTitleView, visible, duration, delay, animated);
    }

    public ImageView getIconVew() {
        return this.mIconView;
    }

    public void setText(CharSequence text) {
        if (this.mTitleView != null && !this.mTitleView.getText().equals(text)) {
            this.mTitleView.setText(text);
        }
    }

    public TextPaint getPaint() {
        if (this.mTitleView != null) {
            return this.mTitleView.getPaint();
        }
        return null;
    }

    public void changeTextColorForBg(boolean whiteBg) {
        if (!OpenThemeManager.getInstance().isPinkTheme() && this.mIconTextBackground == null) {
            boolean followThemeColor = true;
            ItemInfo info = (ItemInfo) getTag();
            if (info != null && (info.isContainApps() || info.container >= 0)) {
                followThemeColor = this.mIconDisplay == 4;
            }
            WhiteBgManager.changeTextColorForBg(this.mLauncher, this, whiteBg, followThemeColor);
        }
    }

    public void updateCheckBox(boolean visible) {
        updateCheckBox(visible, true);
    }

    public void updateCheckBox(boolean visible, boolean animated) {
        if (this.mCheckBox != null) {
            Object tag = getTag();
            if (LauncherFeature.supportFolderSelect() || !(tag instanceof FolderInfo)) {
                if (tag != null) {
                    this.mCheckBox.setChecked(((ItemInfo) tag).getChecked());
                } else {
                    this.mCheckBox.setChecked(false);
                }
                animateEachScale(this.mCheckBox, visible, 100, 0, animated);
                setLongPressTimeout(visible ? 200 : 300);
                refreshBadge();
                updateContentDescription(visible);
            }
        }
    }

    private void updateContentDescription(boolean isSelectState) {
        ItemInfo info = (ItemInfo) getTag();
        CharSequence description = info instanceof FolderInfo ? String.format(getResources().getString(R.string.folder_name_format), new Object[]{info.title}) : info.contentDescription;
        if (description == null) {
            return;
        }
        if (isSelectState) {
            setContentDescription(description + ", " + (this.mCheckBox.isChecked() ? getResources().getString(R.string.selected) : getResources().getString(R.string.not_selected)));
        } else {
            setContentDescription(description);
        }
    }

    public CheckBox getCheckBox() {
        return this.mCheckBox;
    }

    public String getTitle() {
        return this.mTitleView.getText().toString();
    }

    public static boolean isKnoxShortcut(Intent launchIntent) {
        return launchIntent != null && isKnoxShortcut(launchIntent.getComponent());
    }

    public static boolean isKnoxShortcut(ComponentName cn) {
        return cn != null && KNOX_SHORTCUT_PACKAGE.equals(cn.getPackageName());
    }

    protected void animateEachScale(final View view, final boolean visible, int duration, long delay, boolean animated) {
        float end = 1.0f;
        if (view != null && indexOfChild(view) != -1) {
            if (animated) {
                float start;
                if (visible) {
                    start = 0.0f;
                } else {
                    start = 1.0f;
                }
                if (!visible) {
                    end = 0.0f;
                }
                PropertyValuesHolder[] propertyValuesHolderArr = new PropertyValuesHolder[2];
                propertyValuesHolderArr[0] = PropertyValuesHolder.ofFloat(View.SCALE_X, new float[]{start, end});
                propertyValuesHolderArr[1] = PropertyValuesHolder.ofFloat(View.SCALE_Y, new float[]{start, end});
                Animator animator = ObjectAnimator.ofPropertyValuesHolder(view, propertyValuesHolderArr);
                animator.setDuration((long) duration);
                animator.setInterpolator(SINE_IN_OUT_80);
                animator.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationStart(Animator animation) {
                        if (visible) {
                            view.setVisibility(View.VISIBLE);
                        }
                    }

                    public void onAnimationEnd(Animator animation) {
                        if (!visible) {
                            view.setVisibility(4);
                        }
                    }
                });
                animator.setStartDelay(delay);
                animator.start();
                return;
            }
            float f;
            if (visible) {
                f = 1.0f;
            } else {
                f = 0.0f;
            }
            view.setScaleX(f);
            if (!visible) {
                end = 0.0f;
            }
            view.setScaleY(end);
            view.setVisibility(visible ? 0 : 4);
        }
    }

    public void animateChildScale(GridIconInfo prevGridIconInfo) {
        if (prevGridIconInfo != null) {
            animateChildScale(prevGridIconInfo, null);
        }
    }

    protected void animateChildScale(GridIconInfo prevGridIconInfo, Animator addedAnimator) {
        int preIconSize = prevGridIconInfo.getIconSize();
        float iconScale = ((float) preIconSize) / ((float) this.mIconSize);
        float titleScale = ((float) prevGridIconInfo.getTextSize()) / this.mTitleView.getTextSize();
        LayoutParams lp = (LayoutParams) this.mIconView.getLayoutParams();
        float iconTransitionY = (float) ((prevGridIconInfo.getContentTop() - lp.topMargin) + ((preIconSize - this.mIconSize) / 2));
        float titleTransitionY = (float) (((((prevGridIconInfo.getContentTop() + preIconSize) + prevGridIconInfo.getDrawablePadding()) - lp.topMargin) - this.mIconSize) - this.mDrawablePadding);
        AnimatorSet animatorSet = new AnimatorSet();
        ImageView imageView = this.mIconView;
        r11 = new PropertyValuesHolder[3];
        r11[0] = PropertyValuesHolder.ofFloat(View.SCALE_X, new float[]{iconScale, 1.0f});
        r11[1] = PropertyValuesHolder.ofFloat(View.SCALE_Y, new float[]{iconScale, 1.0f});
        r11[2] = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, new float[]{iconTransitionY, 0.0f});
        animatorSet.play(ObjectAnimator.ofPropertyValuesHolder(imageView, r11));
        TextView textView = this.mTitleView;
        r11 = new PropertyValuesHolder[3];
        r11[0] = PropertyValuesHolder.ofFloat(View.SCALE_X, new float[]{titleScale, 1.0f});
        r11[1] = PropertyValuesHolder.ofFloat(View.SCALE_Y, new float[]{titleScale, 1.0f});
        r11[2] = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, new float[]{titleTransitionY, 0.0f});
        animatorSet.play(ObjectAnimator.ofPropertyValuesHolder(textView, r11));
        if (addedAnimator != null) {
            animatorSet.play(addedAnimator);
        }
        animatorSet.setDuration(300);
        animatorSet.start();
    }

    public void updateCountBadge(boolean visible, boolean animate) {
        updateCountBadge(visible, 0, animate);
    }

    public void updateCountBadge(boolean visible, int extraCount) {
        updateCountBadge(visible, extraCount, false);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = super.onKeyDown(keyCode, event);
        if (keyCode != 23 && keyCode != 66) {
            return handled;
        }
        this.mIsPressedEnterKey = true;
        event.startTracking();
        return true;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 23 || keyCode == 66) {
            this.mIsPressedEnterKey = false;
        }
        return super.onKeyUp(keyCode, event);
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode != 23 && keyCode != 66) {
            return super.onKeyLongPress(keyCode, event);
        }
        this.mLongKeyHelper.postCheckForLongKey();
        return true;
    }

    public boolean performLongClick() {
        return !this.mIsPressedEnterKey && super.performLongClick();
    }

    private void updateCountBadge(boolean visible, int extraCount, boolean animate) {
        if (this.mCountBadgeView == null) {
            this.mCountBadgeView = (TextView) findViewById(R.id.iconview_count_badge);
            applyStyle();
        }
        if (visible) {
            String badgeCount = String.valueOf(this.mLauncher.getMultiSelectManager().getCheckedAppCount() + extraCount);
            String currentLanguage = Utilities.getLocale(this.mLauncher).getLanguage();
            if ("ar".equals(currentLanguage) || "fa".equals(currentLanguage)) {
                badgeCount = Utilities.toArabicDigits(badgeCount, currentLanguage);
            } else {
                badgeCount = String.valueOf(badgeCount);
            }
            this.mCountBadgeView.setText(badgeCount);
            this.mCountBadgeView.setScaleX(1.0f);
            this.mCountBadgeView.setScaleY(1.0f);
            this.mCountBadgeView.setVisibility(View.VISIBLE);
        } else {
            animateEachScale(this.mCountBadgeView, false, 200, 0, animate);
        }
        refreshBadge();
    }

    public TextView getCountBadgeView() {
        return this.mCountBadgeView;
    }

    public void setTitleViewVisibility(int visibility) {
        this.mTitleView.setVisibility(visibility);
    }

    public boolean onTouchOutofIconArea(MotionEvent event) {
        MultiSelectManager multiSelectMgr = this.mLauncher.getMultiSelectManager();
        if (multiSelectMgr != null && multiSelectMgr.isMultiSelectMode()) {
            return false;
        }
        boolean result;
        float expandTouchArea = ((float) this.mIconSize) * 0.3f;
        float right = this.mIsPhoneLandscape ? (float) this.mTitleView.getRight() : ((float) this.mIconView.getRight()) + expandTouchArea;
        if (event.getX() < ((float) this.mIconView.getLeft()) - expandTouchArea || event.getX() > right || 0.0f > event.getY() || ((float) getHeight()) < event.getY()) {
            result = true;
        } else {
            result = false;
        }
        if (event.getAction() != 3 && event.getAction() != 1) {
            return result;
        }
        if (!result) {
            return false;
        }
        if (this.mBounceAnim != null) {
            this.mBounceAnim.animateUp();
        }
        setAlpha(1.0f);
        this.mBounceAnim = null;
        return true;
    }

    public int getIconDisplay() {
        return this.mIconDisplay;
    }

    public void markToRemove(boolean tobeRemove) {
        this.mMarkToRemove = tobeRemove;
    }

    public boolean isMarkToRemove() {
        return this.mMarkToRemove;
    }
}

package com.android.launcher3.theme;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.util.BitmapUtils;
import com.sec.android.app.launcher.R;

public class OpenThemeManager {
    public static final int INVALID_COLOR = 33554431;
    public static final int INVALID_INT_VALUE = -1;
    private static final String TAG = "OpenThemeManager";
    public static final int THEME_INVALID_VALUE = 0;
    private static Context mContext;
    private static IconCache mIconCache;
    private FolderStyle mFolderStyle;
    private Drawable mPreloadBadgeDrawable;
    private int mPreloadBadgeTextColor;
    private Drawable mPreloadIconShadowDrawable;
    private Drawable mPreloadPageIndicatorDefaultDrawable;
    private Drawable mPreloadPageIndicatorFestivalDrawable;
    private Drawable mPreloadPageIndicatorHeadlineDrawable;
    private Drawable mPreloadPageIndicatorHomeDrawable;
    private int mPreloadTextColor;
    private int mPreloadTextHighlightColor;
    private int mPreloadTextShadowColor;
    private Drawable mPreloadTitleBGDrawable;
    public int mTextBackgroundExtraPadding;
    public int mTextBackgroundExtraPaddingBottom;
    private OpenThemeLoader mThemeLoader;
    private String[] themeKey;

    public static class FolderStyle {
        private static final int CLOSE_FOLDER_COLOR2 = -6630913;
        private static final int CLOSE_FOLDER_COLOR3 = -8985682;
        private static final int CLOSE_FOLDER_COLOR4 = -1131138;
        private static final int CLOSE_FOLDER_COLOR5 = -794227;
        public static final int CLOSE_FOLDER_TYPE_COLOR = 0;
        public static final int CLOSE_FOLDER_TYPE_IMAGE = 1;
        private static final int DEFAULT_CLOSE_FOLDER_COLOR = -460819;
        private static final int DEFAULT_COLOR_INDEX = 0;
        private static final int DEFAULT_FOLDER_COLOR = -4276546;
        public static final int DEFAULT_FOLDER_TEXT_COLOR = -16777216;
        private static final int DEFAULT_OPEN_FOLDER_BG_COLOR = -328966;
        private static final int DEFAULT_SHAPE = 0;
        public static final int FOLDER_COLOR2 = -15353411;
        public static final int FOLDER_COLOR3 = -27099;
        public static final int FOLDER_COLOR4 = -1263094;
        public static final int FOLDER_COLOR5 = -5777865;
        public static final int INVALID_COLOR = 33554431;
        private static final int INVALID_SHAPE = -1;
        private static final int INVALID_TYPE = -1;
        private static final int NUM_FOLDER_COLOR = 5;
        public static final int OPEN_FOLDER_TYPE_DEFAULT = 0;
        public static final int OPEN_FOLDER_TYPE_IMAGE = 2;
        private static final int[] mDefaultCloseFolderColor = new int[]{DEFAULT_CLOSE_FOLDER_COLOR, CLOSE_FOLDER_COLOR2, CLOSE_FOLDER_COLOR3, CLOSE_FOLDER_COLOR4, CLOSE_FOLDER_COLOR5};
        private static final int[] mDefaultFolderTitleColor = new int[]{DEFAULT_FOLDER_COLOR, FOLDER_COLOR2, FOLDER_COLOR3, FOLDER_COLOR4, FOLDER_COLOR5};
        private static final int[] mFolderShapeBorderRes = new int[]{R.mipmap.homescreen_ic_folder_border, R.drawable.home_ic_folder_shape2_line, R.drawable.home_ic_folder_shape3_line, R.drawable.home_ic_folder_shape4_line, R.drawable.home_ic_folder_shape5_line};
        private static final int[] mFolderShapeRes = new int[]{R.mipmap.folder_transparent_shape, R.drawable.home_ic_folder_shape2, R.drawable.home_ic_folder_shape3, R.drawable.home_ic_folder_shape4, R.drawable.home_ic_folder_shape5};
        private int mCloseFolderShape = 0;
        public int mFirstCloseFolderIconColor = 33554431;
        public FolderAttr[] mFolderAttr = new FolderAttr[5];
        private int mFolderType = 0;
        private int mOpenFolderType = 0;
        private OpenThemeManager mThemeManager = null;

        private class FolderAttr {
            private int folderSize;
            private int mCloseFolderColor;
            private Bitmap mCloseFolderImage;
            private int mOpenFolderBgColor;
            private Drawable mOpenFolderImage;
            private int mOpenFolderTextColor;
            private int mOpenFolderTitleColor;

            private FolderAttr() {
                this.folderSize = OpenThemeManager.mContext.getResources().getDimensionPixelSize(R.dimen.app_icon_size);
            }

            private void set(int shape, int closeColor, int bgColor, int titleColor, int textColor) {
                Context context = OpenThemeManager.mContext;
                this.mCloseFolderColor = closeColor;
                this.mOpenFolderTitleColor = titleColor;
                this.mOpenFolderTextColor = textColor;
                this.mOpenFolderBgColor = bgColor;
                this.mOpenFolderImage = FolderStyle.this.mThemeManager.getItemDrawableforDefaultResource("open_folder_background_theme");
                if (this.mOpenFolderImage instanceof NinePatchDrawable) {
                    Bitmap openBg = FolderStyle.this.mThemeManager.getItemBitmap(ThemeItems.OPEN_FOLDER_BG.value());
                    if (openBg != null) {
                        openBg.recycle();
                    }
                }
                if (!(this.mOpenFolderImage == null || this.mOpenFolderBgColor == 33554431)) {
                    this.mOpenFolderImage.setColorFilter(this.mOpenFolderBgColor, Mode.SRC_IN);
                }
                if (FolderStyle.this.mThemeManager.isDefaultTheme()) {
                    int resourceId;
                    switch (closeColor) {
                        case FolderStyle.CLOSE_FOLDER_COLOR3 /*-8985682*/:
                            resourceId = R.mipmap.homescreen_ic_folder_green;
                            break;
                        case FolderStyle.CLOSE_FOLDER_COLOR2 /*-6630913*/:
                            resourceId = R.mipmap.homescreen_ic_folder_blue;
                            break;
                        case FolderStyle.CLOSE_FOLDER_COLOR4 /*-1131138*/:
                            resourceId = R.mipmap.homescreen_ic_folder_orange;
                            break;
                        case FolderStyle.CLOSE_FOLDER_COLOR5 /*-794227*/:
                            resourceId = R.mipmap.homescreen_ic_folder_yellow;
                            break;
                        default:
                            resourceId = R.mipmap.homescreen_ic_folder_default;
                            break;
                    }
                    this.mCloseFolderImage = BitmapUtils.getBitmap(OpenThemeManager.mContext.getResources().getDrawable(resourceId));
                } else {
                    this.mCloseFolderImage = BitmapUtils.getBitmapWithColor(context, FolderStyle.mFolderShapeRes[shape], closeColor);
                }
                this.mCloseFolderImage = ThemeUtils.resizeBitmap(this.mCloseFolderImage, this.folderSize);
            }

            private void set(Drawable close, Drawable open, int titleColor, int textColor) {
                this.mCloseFolderImage = BitmapUtils.getBitmap(close);
                this.mOpenFolderImage = open;
                this.mOpenFolderTitleColor = titleColor;
                this.mOpenFolderTextColor = textColor;
                this.mCloseFolderImage = ThemeUtils.resizeBitmap(this.mCloseFolderImage, this.folderSize);
            }

            private void set(Drawable close, int bgColor, int titleColor, int textColor) {
                this.mCloseFolderImage = BitmapUtils.getBitmap(close);
                this.mOpenFolderBgColor = bgColor;
                this.mOpenFolderTitleColor = titleColor;
                this.mOpenFolderTextColor = textColor;
                this.mOpenFolderImage = FolderStyle.this.mThemeManager.getItemDrawableforDefaultResource("open_folder_background_theme");
                if (this.mOpenFolderImage != null) {
                    this.mOpenFolderImage.setColorFilter(this.mOpenFolderBgColor, Mode.SRC_IN);
                }
                this.mCloseFolderImage = ThemeUtils.resizeBitmap(this.mCloseFolderImage, this.folderSize);
            }

            private void set(int shape, int closeColor, Drawable open, int titleColor, int textColor) {
                Context context = OpenThemeManager.mContext;
                this.mCloseFolderColor = closeColor;
                this.mCloseFolderImage = BitmapUtils.getBitmapWithColor(context, FolderStyle.mFolderShapeRes[shape], closeColor);
                this.mOpenFolderImage = open;
                this.mOpenFolderTitleColor = titleColor;
                this.mOpenFolderTextColor = textColor;
                this.mCloseFolderImage = ThemeUtils.resizeBitmap(this.mCloseFolderImage, this.folderSize);
            }

            private int getCloseFolderColor() {
                return this.mCloseFolderColor;
            }

            private int getOpenFolderTitleColor() {
                return this.mOpenFolderTitleColor;
            }

            private int getOpenFolderTextColor() {
                return this.mOpenFolderTextColor;
            }

            private Bitmap getCloseFolderImage() {
                return this.mCloseFolderImage;
            }

            private Drawable getOpenFolderImage() {
                return this.mOpenFolderImage;
            }
        }

        FolderStyle(OpenThemeManager themeManager) {
            this.mThemeManager = themeManager;
            for (int i = 0; i < 5; i++) {
                this.mFolderAttr[i] = new FolderAttr();
            }
        }

        private void init() {
            for (int i = 0; i < 5; i++) {
                this.mFolderAttr[i].set(0, mDefaultCloseFolderColor[i], 33554431, mDefaultFolderTitleColor[i], -16777216);
            }
            this.mFolderType = 0;
        }

        void setFolderTheme() {
            init();
            int[] folderColor = new int[5];
            this.mFolderType = this.mThemeManager.getInteger(ThemeItems.CLOSE_FOLDER_TYPE.value());
            int openFolderTitleColor = this.mThemeManager.getColor(ThemeItems.OPEN_FOLDER_TITLE_COLOR.value());
            int openFolderTextColor = this.mThemeManager.getColor(ThemeItems.OPEN_FOLDER_TEXT_COLOR.value());
            Drawable openFolderBg = this.mThemeManager.getDrawable(ThemeItems.OPEN_FOLDER_BG.value());
            int openFolderBgColor = this.mThemeManager.getColor(ThemeItems.OPEN_FOLDER_BG_COLOR.value());
            int openFolderType = this.mThemeManager.getInteger(ThemeItems.OPEN_FOLDER_TYPE.value());
            Drawable folderImage = this.mThemeManager.getDrawable(ThemeItems.CLOSE_FOLDER_ICON1.value());
            if (openFolderBg instanceof NinePatchDrawable) {
                Bitmap openBg = this.mThemeManager.getItemBitmap(ThemeItems.OPEN_FOLDER_BG.value());
                if (openBg != null) {
                    openBg.recycle();
                }
            }
            if (openFolderType == -1) {
                openFolderType = 0;
            }
            if (openFolderTitleColor == 33554431) {
                openFolderTitleColor = DEFAULT_FOLDER_COLOR;
            }
            this.mOpenFolderType = openFolderType;
            if (this.mFolderType != 1 || folderImage == null) {
                this.mCloseFolderShape = this.mThemeManager.getInteger(ThemeItems.CLOSE_FOLDER_SHAPE.value());
                if (this.mCloseFolderShape == -1 || (this.mFolderType == 1 && folderImage == null)) {
                    this.mCloseFolderShape = 0;
                }
                for (int i = 0; i < 5; i++) {
                    folderColor[i] = this.mThemeManager.getColor(ThemeItems.CLOSE_FOLDER_COLOR1.value() + i);
                }
                this.mFirstCloseFolderIconColor = this.mThemeManager.getColor(ThemeItems.CLOSE_FOLDER_COLOR1.value());
                if (openFolderType != 2 || openFolderBg == null) {
                    if (openFolderType == 0 || openFolderBgColor == 33554431 || (openFolderType == 2 && openFolderBg == null)) {
                        openFolderBgColor = DEFAULT_OPEN_FOLDER_BG_COLOR;
                        if (openFolderType == 0) {
                            openFolderTitleColor = 33554431;
                            openFolderTextColor = -16777216;
                        }
                    }
                    setFolder(this.mCloseFolderShape, folderColor, openFolderBgColor, openFolderTitleColor, openFolderTextColor);
                    return;
                }
                setFolder(this.mCloseFolderShape, folderColor, openFolderBg, openFolderTitleColor, openFolderTextColor);
            } else if (openFolderType != 2 || openFolderBg == null) {
                if (openFolderType == 0 || openFolderBgColor == 33554431 || (openFolderType == 2 && openFolderBg == null)) {
                    openFolderBgColor = DEFAULT_OPEN_FOLDER_BG_COLOR;
                    if (openFolderType == 0) {
                        openFolderTitleColor = 33554431;
                        openFolderTextColor = -16777216;
                    }
                }
                setFolder(folderImage, openFolderBgColor, openFolderTitleColor, openFolderTextColor);
            } else {
                setFolder(folderImage, openFolderBg, openFolderTitleColor, openFolderTextColor);
            }
        }

        private void setFolder(int shape, int[] closeColor, int bgColor, int titleColor, int textColor) {
            boolean usefolderColorTitle = false;
            if (titleColor == 33554431) {
                usefolderColorTitle = true;
            }
            int color = 33554431;
            int i = 0;
            while (i < 5) {
                if (closeColor[i] == 33554431) {
                    closeColor[i] = mDefaultCloseFolderColor[i];
                }
                if (usefolderColorTitle) {
                    if (i == 0 && this.mFirstCloseFolderIconColor == 33554431) {
                        titleColor = mDefaultFolderTitleColor[i];
                    } else {
                        titleColor = closeColor[i];
                    }
                }
                if (textColor == 33554431) {
                    textColor = -16777216;
                }
                if (bgColor == 33554431) {
                    bgColor = DEFAULT_OPEN_FOLDER_BG_COLOR;
                }
                this.mFolderAttr[i].set(shape, closeColor[i], bgColor, titleColor, textColor);
                if (color == closeColor[i] || i <= 0) {
                    color = closeColor[i];
                } else {
                    color = 33554431;
                }
                i++;
            }
            if (color != 33554431) {
                this.mFolderType = 1;
            }
        }

        private void setFolder(Drawable close, Drawable open, int titleColor, int textColor) {
            if (titleColor == 33554431) {
                titleColor = mDefaultFolderTitleColor[0];
            }
            if (textColor == 33554431) {
                textColor = -16777216;
            }
            this.mFolderAttr[0].set(close, open, titleColor, textColor);
        }

        private void setFolder(Drawable close, int bgColor, int titleColor, int textColor) {
            if (titleColor == 33554431) {
                titleColor = mDefaultFolderTitleColor[0];
            }
            if (textColor == 33554431) {
                textColor = -16777216;
            }
            if (bgColor == 33554431) {
                textColor = DEFAULT_OPEN_FOLDER_BG_COLOR;
            }
            this.mFolderAttr[0].set(close, bgColor, titleColor, textColor);
        }

        private void setFolder(int shape, int[] closeColor, Drawable open, int titleColor, int textColor) {
            int color = 33554431;
            int i = 0;
            while (i < 5) {
                if (closeColor[i] == 33554431) {
                    closeColor[i] = mDefaultCloseFolderColor[i];
                }
                if (titleColor == 33554431) {
                    titleColor = closeColor[0];
                }
                if (textColor == 33554431) {
                    textColor = -16777216;
                }
                this.mFolderAttr[i].set(shape, closeColor[i], open, titleColor, textColor);
                if (color == closeColor[i] || i <= 0) {
                    color = closeColor[i];
                } else {
                    color = 33554431;
                }
                i++;
            }
            if (color != 33554431) {
                this.mFolderType = 1;
            }
        }

        public Bitmap getCloseFolderBackground(int colorIndex, int dstWidth, int dstHeight) {
            if (this.mFolderType == 1) {
                colorIndex = 0;
            } else if (colorIndex < 0 || colorIndex >= 5) {
                colorIndex = 0;
            }
            return Bitmap.createScaledBitmap(this.mFolderAttr[colorIndex].getCloseFolderImage(), dstWidth, dstHeight, true);
        }

        public Bitmap getCloseFolderImage(int colorIndex) {
            if (this.mFolderType == 1) {
                colorIndex = 0;
            } else if (colorIndex < 0 || colorIndex >= 5) {
                colorIndex = 0;
            }
            return this.mFolderAttr[colorIndex].getCloseFolderImage();
        }

        public Drawable getOpenFolderBackground(int colorIndex) {
            return this.mFolderAttr[0].getOpenFolderImage();
        }

        public int getTitleColor(int colorIndex) {
            if (this.mFolderType == 1) {
                colorIndex = 0;
            } else if (colorIndex < 0 || colorIndex >= 5) {
                colorIndex = 0;
            }
            return this.mFolderAttr[colorIndex].getOpenFolderTitleColor();
        }

        public int getTextColor(int colorIndex) {
            if (this.mFolderType == 1) {
                colorIndex = 0;
            } else if (colorIndex < 0 || colorIndex >= 5) {
                colorIndex = 0;
            }
            return this.mFolderAttr[colorIndex].getOpenFolderTextColor();
        }

        public int getCloseFolderColor(int colorIndex) {
            if (this.mFolderType == 1) {
                colorIndex = 0;
            } else if (colorIndex < 0 || colorIndex >= 5) {
                colorIndex = 0;
            }
            return this.mFolderAttr[colorIndex].getCloseFolderColor();
        }

        public int getFolderType() {
            return this.mFolderType;
        }

        public int getOpenFolderType() {
            return this.mOpenFolderType;
        }

        public int getFirstCloseFolderIconColor() {
            return this.mFirstCloseFolderIconColor;
        }

        public int getCloseFolderShape() {
            return this.mCloseFolderShape;
        }

        public int getCloseFolderBorderRes() {
            return mFolderShapeBorderRes[this.mCloseFolderShape];
        }

        public Bitmap getCloseFolderShapedBitmapWithUserColor(Context context, int color) {
            return BitmapUtils.getBitmapWithColor(context, mFolderShapeRes[this.mCloseFolderShape], color);
        }
    }

    public enum IconBgScope {
        ALL(0),
        UNASSIGNED(1),
        NOTHING(2);
        
        private final int value;

        private IconBgScope(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    private static class SingletonHolder {
        private static final OpenThemeManager sOpenThemeMgr = new OpenThemeManager();

        private SingletonHolder() {
        }
    }

    public enum ThemeItems {
        ICON_BG_RANGE(0),
        ICON_SCALE(1),
        GRID_X(2),
        GRID_Y(3),
        PAGEINDICATOR_HOME(4),
        PAGEINDICATOR_DEFAULT(5),
        PAGEINDICATOR_HEADLINE(6),
        SHADOW(7),
        APPTITLE_ONOFF(8),
        FONT(9),
        TITLE_BACKGROUND(10),
        HOME_TEXT_COLOR(11),
        APPS_TEXT_COLOR(12),
        ALL_APPS_ICON(13),
        ICON_TRAY(14),
        APPS_TRAY_BG(15),
        TRANSITON_EFFECT(16),
        TEXT_HIGHLIGHT(17),
        BADGE_BG(18),
        BADGE_BG_COLOR(19),
        BADGE_TEXT_COLOR(20),
        TEXT_SHADOW(21),
        TEXT_SHADOW_COLOR(22),
        CLOSE_FOLDER_ICON1(23),
        CLOSE_FOLDER_ICON2(24),
        CLOSE_FOLDER_ICON3(25),
        CLOSE_FOLDER_ICON4(26),
        CLOSE_FOLDER_ICON5(27),
        CLOSE_FOLDER_TYPE(28),
        CLOSE_FOLDER_SHAPE(29),
        CLOSE_FOLDER_COLOR1(30),
        CLOSE_FOLDER_COLOR2(31),
        CLOSE_FOLDER_COLOR3(32),
        CLOSE_FOLDER_COLOR4(33),
        CLOSE_FOLDER_COLOR5(34),
        OPEN_FOLDER_TYPE(35),
        OPEN_FOLDER_BG(36),
        OPEN_FOLDER_BG_COLOR(37),
        OPEN_FOLDER_TITLE_COLOR(38),
        OPEN_FOLDER_TEXT_COLOR(39),
        PAGEINDICATOR_FESTIVAL(40);
        
        private final int value;

        private ThemeItems(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    public enum ThemeType {
        DEFAULT(0),
        HOME(1),
        APP_ICON(2),
        EVENT(3),
        WINSET(4),
        BADGE(5);
        
        private final int value;

        private ThemeType(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    public static OpenThemeManager getInstance() {
        return SingletonHolder.sOpenThemeMgr;
    }

    private OpenThemeManager() {
        this.mPreloadTextColor = 33554431;
        this.mPreloadTextHighlightColor = 33554431;
        this.mPreloadTextShadowColor = 33554431;
        this.mPreloadBadgeTextColor = 33554431;
        this.mPreloadIconShadowDrawable = null;
        this.mPreloadBadgeDrawable = null;
        this.mPreloadPageIndicatorDefaultDrawable = null;
        this.mPreloadPageIndicatorHomeDrawable = null;
        this.mPreloadPageIndicatorHeadlineDrawable = null;
        this.mPreloadPageIndicatorFestivalDrawable = null;
        this.mPreloadTitleBGDrawable = null;
        this.themeKey = new String[]{"icon_bg_range", "icon_scale_size", "home_cell_count_x", "home_cell_count_y", "homescreen_menu_page_navi_home_f", "homescreen_menu_page_navi_default_f", "homescreen_menu_page_navi_headlines_f", "enable_shadow", "enable_title", "font", "title_background", "home_title_color", "apps_title_color", "ic_allapps", "ic_icon_bg", "homescreen_apps_bg", "page_transition_effect", "material_blue_grey_900", "counter_bubble", "badge_bg_color", "badge_text_color", "enable_text_shadow", "text_shadow_color", "homescreen_ic_folder_default", "home_ic_folder_gray", "home_ic_folder_red", "home_ic_folder_yellow", "home_ic_folder_green", "close_folder_type", "close_folder_shape", "close_folder_color1", "close_folder_color2", "close_folder_color3", "close_folder_color4", "close_folder_color5", "open_folder_type", "home_folder_bg_default", "folder_popup_bg_color", "open_folder_title_color", "open_folder_text_color", "homescreen_menu_page_navi_festival"};
        mContext = LauncherAppState.getInstance().getContext();
        mIconCache = LauncherAppState.getInstance().getIconCache();
        this.mThemeLoader = new OpenThemeLoader(mContext);
        this.mFolderStyle = new FolderStyle(this);
        this.mFolderStyle.setFolderTheme();
    }

    public void preloadResources() {
        preloadColor();
        preloadDrawable();
        preloadOtherResources();
    }

    private void preloadOtherResources() {
        Resources r = mContext.getResources();
        this.mTextBackgroundExtraPadding = r.getDimensionPixelSize(R.dimen.home_icon_text_theme_extra_padding_width);
        this.mTextBackgroundExtraPaddingBottom = r.getDimensionPixelSize(R.dimen.home_icon_text_theme_extra_padding_bottom);
    }

    private void preloadColor() {
        this.mPreloadTextColor = getColor(ThemeItems.HOME_TEXT_COLOR.value());
        this.mPreloadTextHighlightColor = getColor(ThemeItems.TEXT_HIGHLIGHT.value());
        if (getBoolean(ThemeItems.TEXT_SHADOW.value(), true)) {
            this.mPreloadTextShadowColor = getColor(ThemeItems.TEXT_SHADOW_COLOR.value());
        } else {
            this.mPreloadTextShadowColor = 33554431;
        }
        this.mPreloadBadgeTextColor = getColor(ThemeItems.BADGE_TEXT_COLOR.value());
    }

    public Drawable preloadBadgeDrawable(int badgeSettingValue) {
        boolean z = true;
        Resources r = mContext.getResources();
        if (r == null) {
            return null;
        }
        int badgeBgColor = getColor(ThemeItems.BADGE_BG_COLOR.value());
        if (badgeSettingValue != 1) {
            z = false;
        }
        return getDrawable(r, badgeBgColor, z);
    }

    private Drawable getDrawable(Resources r, int badgeBgColor, boolean badgeWithDot) {
        Drawable drawable;
        Bitmap small;
        if (LauncherAppState.getInstance().isEasyModeEnabled()) {
            if (badgeWithDot) {
                drawable = r.getDrawable(R.drawable.tw_noti_badge_mtrl_easymode);
                small = BitmapFactory.decodeResource(r, R.drawable.tw_noti_badge_mtrl_easymode);
            } else {
                drawable = r.getDrawable(R.drawable.homescreen_badge_easymode);
                small = BitmapFactory.decodeResource(r, R.drawable.homescreen_badge_easymode);
            }
        } else if (!badgeWithDot) {
            drawable = r.getDrawable(R.drawable.homescreen_badge);
            small = BitmapFactory.decodeResource(r, R.drawable.homescreen_badge);
        } else if (Utilities.isPortrait()) {
            drawable = r.getDrawable(R.drawable.tw_noti_badge_mtrl);
            small = BitmapFactory.decodeResource(r, R.drawable.tw_noti_badge_mtrl);
        } else {
            drawable = r.getDrawable(R.drawable.tw_noti_badge_mtrl_land);
            small = BitmapFactory.decodeResource(r, R.drawable.tw_noti_badge_mtrl_land);
        }
        if (badgeBgColor != 33554431) {
            Rect padding = new Rect();
            drawable.getPadding(padding);
            Drawable preloadBadgeDrawable = ThemeUtils.getNinepatchWithColor(small, badgeBgColor, padding);
            if (preloadBadgeDrawable != null) {
                return preloadBadgeDrawable;
            }
            drawable.mutate().setColorFilter(badgeBgColor, Mode.SRC_IN);
            return drawable;
        }
        Log.d(TAG, "Can't set badge color to invalid");
        return drawable;
    }

    private void preloadDrawable() {
        Resources r = mContext.getResources();
        if (r != null) {
            this.mPreloadIconShadowDrawable = r.getDrawable(R.drawable.homescreen_shadow);
            this.mPreloadBadgeDrawable = preloadBadgeDrawable(Utilities.getBadgeSettingValue(mContext));
            this.mPreloadPageIndicatorDefaultDrawable = getDrawable(ThemeItems.PAGEINDICATOR_DEFAULT.value());
            this.mPreloadPageIndicatorHomeDrawable = getDrawable(ThemeItems.PAGEINDICATOR_HOME.value());
            this.mPreloadPageIndicatorFestivalDrawable = getDrawable(ThemeItems.PAGEINDICATOR_FESTIVAL.value());
            this.mPreloadPageIndicatorHeadlineDrawable = getDrawable(ThemeItems.PAGEINDICATOR_HEADLINE.value());
        }
        this.mPreloadTitleBGDrawable = getDrawable(ThemeItems.TITLE_BACKGROUND.value());
    }

    public Bitmap getIconWithTrayIfNeeded(Bitmap iconBitmap, int iconSize, boolean isThemeIcon) {
        Bitmap newIcon = null;
        if (iconBitmap == null) {
            return null;
        }
        int icon_bg_range = getInteger(ThemeItems.ICON_BG_RANGE.value());
        if (icon_bg_range == IconBgScope.UNASSIGNED.value()) {
            if (!isThemeIcon) {
                newIcon = ThemeUtils.integrateIconAndTray(iconBitmap, load3rdPartyIconTray(iconSize, iconSize), iconSize, iconSize);
            }
        } else if (icon_bg_range == IconBgScope.ALL.value()) {
            newIcon = ThemeUtils.integrateIconAndTray(iconBitmap, load3rdPartyIconTray(iconSize, iconSize), iconSize, iconSize);
        }
        if (newIcon == null) {
            return iconBitmap;
        }
        return newIcon;
    }

    private Bitmap load3rdPartyIconTray(int width, int height) {
        Drawable icon = getDrawable(ThemeItems.ICON_TRAY.value());
        if (icon != null) {
            return getBitmapWithSizeForTheme(icon, width, height);
        }
        return null;
    }

    public Bitmap getBitmapWithSizeForTheme(Drawable drawable, int dstWidth, int dstHeight) {
        if (drawable == null) {
            return null;
        }
        int srcWidth = drawable.getIntrinsicWidth();
        int srcHeight = drawable.getIntrinsicHeight();
        float ratio = Math.min(((float) dstWidth) / ((float) srcWidth), ((float) dstHeight) / ((float) srcHeight));
        Bitmap bmp = Bitmap.createBitmap(dstWidth, dstHeight, Config.ARGB_8888);
        if (bmp == null) {
            return bmp;
        }
        Canvas c = new Canvas(bmp);
        if (srcWidth > srcHeight) {
            c.translate(0.0f, (((float) dstHeight) - (((float) srcHeight) * ratio)) / 2.0f);
        } else if (srcHeight > srcWidth) {
            c.translate((((float) dstWidth) - (((float) srcWidth) * ratio)) / 2.0f, 0.0f);
        }
        c.scale(ratio, ratio);
        Rect oldBounds = drawable.copyBounds();
        drawable.setBounds(0, 0, srcWidth, srcHeight);
        drawable.draw(c);
        drawable.setBounds(oldBounds);
        c.setBitmap(null);
        return bmp;
    }

    public int getInteger(int intId) {
        String packageName = this.mThemeLoader.getThemePackageName(getThemeType(this.themeKey[intId]));
        Resources r = this.mThemeLoader.getResources(packageName);
        int integer = -1;
        if (r != null) {
            int resId = r.getIdentifier(this.themeKey[intId], "integer", packageName);
            if (resId == 0) {
                r = this.mThemeLoader.getResources(this.mThemeLoader.getDefaultPackageName());
                if (r != null) {
                    resId = r.getIdentifier(this.themeKey[intId], "integer", this.mThemeLoader.getDefaultPackageName());
                    if (resId == 0) {
                        return integer;
                    }
                }
                Log.d(TAG, "Can't find such integer in the default resource : " + intId);
                return -1;
            }
            try {
                integer = r.getInteger(resId);
            } catch (NotFoundException e) {
                Log.d(TAG, "Theme package has existed [" + packageName + "] but no resources.");
            }
        } else {
            Log.d(TAG, "fail to getting resources from " + packageName);
        }
        return integer;
    }

    public int getColor(int colorId) {
        String packageName = this.mThemeLoader.getThemePackageName(getThemeType(this.themeKey[colorId]));
        Resources r = this.mThemeLoader.getResources(packageName);
        int color = 33554431;
        if (r != null) {
            int resId = r.getIdentifier(this.themeKey[colorId], BaseLauncherColumns.COLOR, packageName);
            if (resId == 0) {
                return getItemColorforDefaultResource(this.themeKey[colorId]);
            }
            try {
                return r.getColor(resId);
            } catch (NotFoundException e) {
                Log.d(TAG, "Theme package has existed [" + packageName + "] but no resources.");
                return color;
            }
        }
        Log.d(TAG, "fail to getting resources from " + packageName);
        return color;
    }

    public int getItemColorforDefaultResource(String colorId) {
        Resources r = mContext.getResources();
        if (r == null) {
            return 33554431;
        }
        int resId = r.getIdentifier(colorId, BaseLauncherColumns.COLOR, this.mThemeLoader.getDefaultPackageName());
        if (resId == 0) {
            return 33554431;
        }
        return r.getColor(resId);
    }

    public String getString(int stringId) {
        String packageName = this.mThemeLoader.getThemePackageName(getThemeType(this.themeKey[stringId]));
        String string = null;
        Resources r = this.mThemeLoader.getResources(packageName);
        if (r != null) {
            int resId = r.getIdentifier(this.themeKey[stringId], "string", packageName);
            if (resId == 0) {
                r = this.mThemeLoader.getResources(this.mThemeLoader.getDefaultPackageName());
                if (r != null) {
                    resId = r.getIdentifier(this.themeKey[stringId], "string", this.mThemeLoader.getDefaultPackageName());
                    if (resId == 0) {
                        return string;
                    }
                }
                Log.d(TAG, "Can't find such string in the default resource : " + stringId);
                return null;
            }
            try {
                string = r.getString(resId);
            } catch (NotFoundException e) {
                Log.d(TAG, "Theme package has existed [" + packageName + "] but no resources.");
            }
        } else {
            Log.d(TAG, "fail to getting resources from " + packageName);
        }
        return string;
    }

    public boolean getBoolean(int boolId, boolean defValue) {
        String packageName = this.mThemeLoader.getThemePackageName(getThemeType(this.themeKey[boolId]));
        boolean bool = defValue;
        Resources r = this.mThemeLoader.getResources(packageName);
        if (r != null) {
            int resId = r.getIdentifier(this.themeKey[boolId], "bool", packageName);
            if (resId == 0) {
                r = this.mThemeLoader.getResources(this.mThemeLoader.getDefaultPackageName());
                if (r != null) {
                    resId = r.getIdentifier(this.themeKey[boolId], "bool", this.mThemeLoader.getDefaultPackageName());
                    if (resId == 0) {
                        return bool;
                    }
                }
                Log.d(TAG, "Can't find such bool in the default resource : " + boolId);
                return false;
            }
            try {
                bool = r.getBoolean(resId);
            } catch (NotFoundException e) {
                Log.d(TAG, "Theme package has existed [" + packageName + "] but no resources.");
            }
        } else {
            Log.d(TAG, "fail to getting resources from " + packageName);
        }
        return bool;
    }

    public Drawable getDrawable(int drawableId) {
        String packageName = this.mThemeLoader.getThemePackageName(getThemeType(this.themeKey[drawableId]));
        Drawable drawable = null;
        Resources r = this.mThemeLoader.getResources(packageName);
        if (r != null) {
            int resId = r.getIdentifier(this.themeKey[drawableId], "drawable", packageName);
            if (resId == 0) {
                try {
                    drawable = getItemDrawableforDefaultResource(this.themeKey[drawableId]);
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    drawable = r.getDrawable(resId);
                    if (drawable == null) {
                        drawable = getItemDrawableforDefaultResource(this.themeKey[drawableId]);
                    }
                } catch (NotFoundException e2) {
                    Log.d(TAG, "Theme package has existed [" + packageName + "] but no resources.");
                }
            }
        } else {
            Log.d(TAG, "Can't find such drawable in the resource : " + drawableId);
        }
        return drawable;
    }

    public Drawable getItemDrawableforDefaultResource(String drawableId) {
        Resources r = mContext.getResources();
        if (r == null) {
            return null;
        }
        int resId = r.getIdentifier(drawableId, "drawable", this.mThemeLoader.getDefaultPackageName());
        if (resId == 0) {
            return null;
        }
        return r.getDrawable(resId);
    }

    public Bitmap getItemBitmap(int bitmapId) {
        String packageName = this.mThemeLoader.getThemePackageName(getThemeType(this.themeKey[bitmapId]));
        Resources r = this.mThemeLoader.getResources(packageName);
        if (r == null) {
            return null;
        }
        int resId = r.getIdentifier(this.themeKey[bitmapId], "drawable", packageName);
        if (resId == 0) {
            return getItemBitmapforDefaultResource(this.themeKey[bitmapId]);
        }
        return BitmapFactory.decodeResource(r, resId);
    }

    private Bitmap getItemBitmapforDefaultResource(String bitmapId) {
        Resources r = mContext.getResources();
        if (r == null) {
            return null;
        }
        int resId = r.getIdentifier(bitmapId, "drawable", this.mThemeLoader.getDefaultPackageName());
        if (resId == 0) {
            return null;
        }
        return BitmapFactory.decodeResource(r, resId);
    }

    public boolean isDefaultTheme() {
        Boolean ret = Boolean.valueOf(false);
        if (mContext.getPackageName().equals(this.mThemeLoader.getThemePackageName(ThemeType.HOME.value))) {
            ret = Boolean.valueOf(true);
        }
        return ret.booleanValue();
    }

    public boolean isPinkTheme() {
        Boolean ret = Boolean.valueOf(false);
        if ("com.sec.Pink.common.home".equals(this.mThemeLoader.getThemePackageName(ThemeType.HOME.value))) {
            ret = Boolean.valueOf(true);
        }
        return ret.booleanValue();
    }

    public boolean isThemeChanged() {
        return this.mThemeLoader.isThemeChanged();
    }

    public boolean isFromThemeResources(int id, String type) {
        String packageName = this.mThemeLoader.getThemePackageName(getThemeType(this.themeKey[id]));
        Resources r = this.mThemeLoader.getResources(packageName);
        if (r == null || r.getIdentifier(this.themeKey[id], type, packageName) == 0) {
            return false;
        }
        return true;
    }

    public Bitmap getThemeAppIcon() {
        return getIconWithTrayIfNeeded(BitmapUtils.createIconBitmap(getDrawable(ThemeItems.ALL_APPS_ICON.value()), mContext), BitmapUtils.getIconBitmapSize(), isFromThemeResources(ThemeItems.ALL_APPS_ICON.value(), "drawable"));
    }

    public void initThemeForIconLoading(boolean needReload) {
        if (isThemeChanged()) {
            Log.d(TAG, "Theme changed, clear mIconDB.");
            mIconCache.clearDB();
            this.mThemeLoader.setIsThemeChanged(false);
        }
        if (needReload) {
            Log.d(TAG, "dpi changed, reload ThemeLoader and FolderStyle");
            this.mThemeLoader.reloadThemePackages();
            this.mFolderStyle.setFolderTheme();
        }
    }

    public FolderStyle getFolderStyle() {
        return this.mFolderStyle;
    }

    public int getPreloadColor(int id) {
        String key = this.themeKey[id];
        if (key.equals("home_title_color")) {
            return this.mPreloadTextColor;
        }
        if (key.equals("material_blue_grey_900")) {
            return this.mPreloadTextHighlightColor;
        }
        if (key.equals("text_shadow_color")) {
            return this.mPreloadTextShadowColor;
        }
        if (key.equals("badge_text_color")) {
            return this.mPreloadBadgeTextColor;
        }
        return 33554431;
    }

    public Drawable getPreloadDrawable(int id) {
        String key = this.themeKey[id];
        if (key.equals("enable_shadow")) {
            return this.mPreloadIconShadowDrawable;
        }
        if (key.equals("counter_bubble")) {
            return this.mPreloadBadgeDrawable;
        }
        if (key.equals("homescreen_menu_page_navi_home_f")) {
            return this.mPreloadPageIndicatorHomeDrawable;
        }
        if (key.equals("homescreen_menu_page_navi_default_f")) {
            return this.mPreloadPageIndicatorDefaultDrawable;
        }
        if (key.equals("homescreen_menu_page_navi_festival")) {
            return this.mPreloadPageIndicatorFestivalDrawable;
        }
        if (key.equals("homescreen_menu_page_navi_headlines_f")) {
            return this.mPreloadPageIndicatorHeadlineDrawable;
        }
        if (key.equals("title_background")) {
            return this.mPreloadTitleBGDrawable;
        }
        return null;
    }

    private int getThemeType(String intId) {
        int type = ThemeType.DEFAULT.value();
        Object obj = -1;
        switch (intId.hashCode()) {
            case -1735252490:
                if (intId.equals("open_folder_type")) {
                    obj = 4;
                    break;
                }
                break;
            case -1602711204:
                if (intId.equals("icon_scale_size")) {
                    obj = 1;
                    break;
                }
                break;
            case -1549057161:
                if (intId.equals("close_folder_shape")) {
                    obj = 5;
                    break;
                }
                break;
            case -1227406312:
                if (intId.equals("close_folder_color1")) {
                    obj = 12;
                    break;
                }
                break;
            case -1227406311:
                if (intId.equals("close_folder_color2")) {
                    obj = 13;
                    break;
                }
                break;
            case -1227406310:
                if (intId.equals("close_folder_color3")) {
                    obj = 14;
                    break;
                }
                break;
            case -1227406309:
                if (intId.equals("close_folder_color4")) {
                    obj = 15;
                    break;
                }
                break;
            case -1227406308:
                if (intId.equals("close_folder_color5")) {
                    obj = 16;
                    break;
                }
                break;
            case -1019754332:
                if (intId.equals("close_folder_type")) {
                    obj = 3;
                    break;
                }
                break;
            case -1006616232:
                if (intId.equals("home_folder_bg_default")) {
                    obj = 26;
                    break;
                }
                break;
            case -951635840:
                if (intId.equals("open_folder_title_color")) {
                    obj = 10;
                    break;
                }
                break;
            case -829751021:
                if (intId.equals("homescreen_menu_page_navi_default_f")) {
                    obj = 22;
                    break;
                }
                break;
            case -769629765:
                if (intId.equals("homescreen_menu_page_navi_home_f")) {
                    obj = 21;
                    break;
                }
                break;
            case -622029819:
                if (intId.equals("badge_bg_color")) {
                    obj = 17;
                    break;
                }
                break;
            case -556956306:
                if (intId.equals("ic_allapps")) {
                    obj = 27;
                    break;
                }
                break;
            case -543784691:
                if (intId.equals("folder_popup_bg_color")) {
                    obj = 9;
                    break;
                }
                break;
            case -458259455:
                if (intId.equals("homescreen_ic_folder_default")) {
                    obj = 25;
                    break;
                }
                break;
            case -372171891:
                if (intId.equals("open_folder_text_color")) {
                    obj = 11;
                    break;
                }
                break;
            case -338915492:
                if (intId.equals("home_title_color")) {
                    obj = 6;
                    break;
                }
                break;
            case 57287029:
                if (intId.equals("title_background")) {
                    obj = 24;
                    break;
                }
                break;
            case 65071151:
                if (intId.equals("counter_bubble")) {
                    obj = 29;
                    break;
                }
                break;
            case 75922374:
                if (intId.equals("material_blue_grey_900")) {
                    obj = 7;
                    break;
                }
                break;
            case 250842569:
                if (intId.equals("icon_bg_range")) {
                    obj = null;
                    break;
                }
                break;
            case 472316337:
                if (intId.equals("homescreen_menu_page_navi_headlines_f")) {
                    obj = 23;
                    break;
                }
                break;
            case 692702134:
                if (intId.equals("enable_text_shadow")) {
                    obj = 20;
                    break;
                }
                break;
            case 952759819:
                if (intId.equals("page_transition_effect")) {
                    obj = 2;
                    break;
                }
                break;
            case 992897366:
                if (intId.equals("text_shadow_color")) {
                    obj = 8;
                    break;
                }
                break;
            case 1259710300:
                if (intId.equals("enable_shadow")) {
                    obj = 19;
                    break;
                }
                break;
            case 1664329709:
                if (intId.equals("badge_text_color")) {
                    obj = 18;
                    break;
                }
                break;
            case 1993584550:
                if (intId.equals("ic_icon_bg")) {
                    obj = 28;
                    break;
                }
                break;
        }
        switch (obj) {
            case null:
            case 1:
                return ThemeType.APP_ICON.value();
            case 2:
            case 3:
            case 4:
            case 5:
                return ThemeType.HOME.value();
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
                return ThemeType.HOME.value();
            case 17:
            case 18:
                return ThemeType.BADGE.value();
            case 19:
            case 20:
                return ThemeType.HOME.value();
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case MotionEventCompat.AXIS_SCROLL /*26*/:
                return ThemeType.HOME.value();
            case MotionEventCompat.AXIS_RELATIVE_X /*27*/:
            case MotionEventCompat.AXIS_RELATIVE_Y /*28*/:
                return ThemeType.APP_ICON.value();
            case 29:
                return ThemeType.BADGE.value();
            default:
                return type;
        }
    }
}

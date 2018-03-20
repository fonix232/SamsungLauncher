package com.android.launcher3.home;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.controller.ControllerBase;
import com.android.launcher3.util.BitmapUtils;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;

public class OverviewPanel extends LinearLayout {
    private static final String TAG = "Launcher.OverviewPanel";
    private static int sThemeStoreType = 0;
    private static final boolean sUseThemeBtn = Utilities.ATLEAST_O;
    private final int DRAWABLE_TOP;
    private final float ICON_PRESS_ALPHA_VALUE;
    private int mChildCount;
    private HomeController mHomeController;
    private Launcher mLauncher;
    private OnTouchListener mOnTouchListener;
    LinearLayout mOverviewPanelLayout;
    private AlertDialog mThemeDownloadDialog;
    private TextView mThemesButton;
    private TextView mWallpapersButton;

    public OverviewPanel(Context context) {
        this(context, null);
    }

    public OverviewPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverviewPanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.DRAWABLE_TOP = 1;
        this.ICON_PRESS_ALPHA_VALUE = 0.5f;
        this.mThemeDownloadDialog = null;
        this.mOnTouchListener = new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent ev) {
                int action = ev.getAction();
                if (action == 0) {
                    v.setAlpha(0.5f);
                } else if (action != 2) {
                    v.setAlpha(1.0f);
                }
                return false;
            }
        };
        this.mLauncher = (Launcher) context;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        int drawableSize = getResources().getDimensionPixelSize(R.dimen.overview_panel_options_button_drawable_size);
        this.mWallpapersButton = (TextView) findViewById(R.id.wallpapers_button);
        this.mWallpapersButton.setOnKeyListener(HomeFocusHelper.OVERVIEW_PANEL_OPTION_BUTTON_KEY_LISTENER);
        this.mWallpapersButton.setOnTouchListener(this.mOnTouchListener);
        this.mThemesButton = (TextView) findViewById(R.id.theme_button);
        this.mThemesButton.setCompoundDrawablesWithIntrinsicBounds(null, BitmapUtils.getResizedDrawable(getContext(), getResources().getDrawable(R.drawable.homescreen_ic_reorder_theme, null), drawableSize, drawableSize), null, null);
        this.mThemesButton.setOnKeyListener(HomeFocusHelper.OVERVIEW_PANEL_OPTION_BUTTON_KEY_LISTENER);
        this.mThemesButton.setOnTouchListener(this.mOnTouchListener);
        this.mThemesButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!OverviewPanel.this.mHomeController.isSwitchingState() && !OverviewPanel.this.mHomeController.isReorderAnimating()) {
                    OverviewPanel.this.onClickWallpapersAndThemesButton(1);
                }
            }
        });
        TextView widgetsButton = (TextView) findViewById(R.id.widget_button);
        widgetsButton.setCompoundDrawablesWithIntrinsicBounds(null, BitmapUtils.getResizedDrawable(getContext(), getResources().getDrawable(R.drawable.homescreen_ic_reorder_widgets, null), drawableSize, drawableSize), null, null);
        widgetsButton.setOnKeyListener(HomeFocusHelper.OVERVIEW_PANEL_OPTION_BUTTON_KEY_LISTENER);
        widgetsButton.setOnTouchListener(this.mOnTouchListener);
        widgetsButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (OverviewPanel.this.mHomeController.isSwitchingState()) {
                    Log.e(OverviewPanel.TAG, "can't enter widgetList: isSwitchingState()");
                } else if (OverviewPanel.this.mHomeController.isReorderAnimating()) {
                    Log.e(OverviewPanel.TAG, "can't enter widgetList: isReorderAnimating()");
                } else if (OverviewPanel.this.mHomeController.getWorkspace().isPageMoving()) {
                    Log.e(OverviewPanel.TAG, "can't enter widgetList: isPageMoving()");
                } else {
                    OverviewPanel.this.onClickWidgetsButton();
                }
            }
        });
        widgetsButton.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                int extractType = Utilities.checkHomeHiddenDir();
                if (extractType > -1) {
                    OverviewPanel.this.mLauncher.startLCExtractor(extractType);
                }
                return true;
            }
        });
        TextView settingsButton = (TextView) findViewById(R.id.settings_button);
        settingsButton.setCompoundDrawablesWithIntrinsicBounds(null, BitmapUtils.getResizedDrawable(getContext(), getResources().getDrawable(R.drawable.homescreen_ic_reorder_setting, null), drawableSize, drawableSize), null, null);
        settingsButton.setVisibility(View.VISIBLE);
        settingsButton.setOnKeyListener(HomeFocusHelper.OVERVIEW_PANEL_OPTION_BUTTON_KEY_LISTENER);
        settingsButton.setOnTouchListener(this.mOnTouchListener);
        settingsButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!OverviewPanel.this.mHomeController.isSwitchingState() && !OverviewPanel.this.mHomeController.isReorderAnimating()) {
                    OverviewPanel.this.onClickSettingsButton();
                }
            }
        });
        updateLayoutByThemeStoreType();
        updateOverviewPanelLayout();
        setEditTextBg();
        setContentDescription();
    }

    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        if (visibility == 0) {
            updateLayoutByThemeStoreType();
            if (WhiteBgManager.isWhiteBg()) {
                changeColorForBg(true);
            }
        }
        super.onVisibilityChanged(changedView, visibility);
    }

    private void updateLayoutByThemeStoreType() {
        int drawableSize = getResources().getDimensionPixelSize(R.dimen.overview_panel_options_button_drawable_size);
        sThemeStoreType = Utilities.checkThemeStoreState(this.mLauncher);
        if (sUseThemeBtn || sThemeStoreType == 1 || sThemeStoreType == 2) {
            this.mWallpapersButton.setCompoundDrawablesWithIntrinsicBounds(null, BitmapUtils.getResizedDrawable(getContext(), getResources().getDrawable(R.drawable.homescreen_ic_reorder_wallpaper, null), drawableSize, drawableSize), null, null);
        } else {
            this.mWallpapersButton.setText(getResources().getString(R.string.wallpapers_and_themes_button_text));
            this.mWallpapersButton.setCompoundDrawablesWithIntrinsicBounds(null, BitmapUtils.getResizedDrawable(getContext(), getResources().getDrawable(R.drawable.homescreen_ic_reorder_theme, null), drawableSize, drawableSize), null, null);
        }
        if (sThemeStoreType == 2) {
            this.mWallpapersButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (!OverviewPanel.this.mHomeController.isSwitchingState() && !OverviewPanel.this.mHomeController.isReorderAnimating()) {
                        OverviewPanel.this.onClickWallpapersButton();
                    }
                }
            });
        } else {
            this.mWallpapersButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (!OverviewPanel.this.mHomeController.isSwitchingState() && !OverviewPanel.this.mHomeController.isReorderAnimating()) {
                        OverviewPanel.this.onClickWallpapersAndThemesButton(0);
                    }
                }
            });
        }
        TextView textView = this.mThemesButton;
        int i = (sUseThemeBtn && (sThemeStoreType == 0 || sThemeStoreType == 3)) ? 0 : 8;
        textView.setVisibility(i);
    }

    void bindController(ControllerBase controller) {
        this.mHomeController = (HomeController) controller;
    }

    private void onClickWallpapersButton() {
        Log.d(TAG, "onClickWallpapersButton");
        Intent chooser = new Intent("com.sec.android.app.wallpapers.WallpaperPickerActivity");
        chooser.putExtra("type", 0);
        chooser.putExtra("android.intent.extra.INTENT", new Intent("android.intent.action.SET_WALLPAPER"));
        chooser.putExtra("mode", "null");
        GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOME_EDIT_OPTION, GSIMLogging.HOME_EDIT_OPTION_WALLPAPER_AND_THEME, -1, false);
        SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_HomeOption), getResources().getString(R.string.event_Wallpapers));
        Utilities.startActivityForResultSafely(this.mLauncher, chooser, 10);
    }

    private void onClickWallpapersAndThemesButton(int contentType) {
        Log.d(TAG, "onClickWallpapersAndThemesButton - type = " + contentType + ", sThemeStoreType = " + sThemeStoreType);
        if (sThemeStoreType == 3) {
            showThemeDownloadDialog(contentType);
        } else {
            Utilities.startThemeStore(this.mLauncher, contentType);
        }
        GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOME_EDIT_OPTION, GSIMLogging.HOME_EDIT_OPTION_WALLPAPER_AND_THEME, -1, false);
        if (!sUseThemeBtn) {
            SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_HomeOption), getResources().getString(R.string.event_WallpapersandThemes));
        } else if (contentType == 0) {
            SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_HomeOption), getResources().getString(R.string.event_Wallpapers));
        } else {
            SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_HomeOption), getResources().getString(R.string.event_Themes));
        }
    }

    private void onClickWidgetsButton() {
        Log.d(TAG, "onClickWidgetsButton");
        if (this.mLauncher.isSafeModeEnabled()) {
            Toast.makeText(this.mLauncher, R.string.safemode_widget_error, 0).show();
        } else {
            this.mLauncher.showWidgetsView(true, true);
        }
        GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOME_EDIT_OPTION, GSIMLogging.HOME_EDIT_OPTION_WIDGET, -1, false);
        SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_HomeOption), getResources().getString(R.string.event_Widgets));
    }

    private void onClickSettingsButton() {
        Log.d(TAG, "onClickSettingsButton");
        this.mLauncher.startHomeSettingActivity();
        SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_HomeOption), getResources().getString(R.string.event_Homesettings));
        GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOME_EDIT_OPTION, GSIMLogging.HOME_EDIT_OPTION_SETTINGS, -1, false);
    }

    private void setEditTextBg() {
        if (Utilities.isEnableBtnBg(getContext())) {
            for (int index = 0; index < this.mChildCount; index++) {
                View child = this.mOverviewPanelLayout.getChildAt(index);
                if (child instanceof TextView) {
                    child.setBackgroundResource(R.drawable.panel_btn_bg);
                }
            }
        }
    }

    private void setContentDescription() {
        String buttonString = getResources().getString(R.string.accessibility_button);
        for (int index = 0; index < this.mChildCount; index++) {
            View child = this.mOverviewPanelLayout.getChildAt(index);
            if (child instanceof TextView) {
                child.setContentDescription(((TextView) child).getText() + ", " + buttonString);
            }
        }
    }

    void changeColorForBg(boolean whiteBg) {
        for (int index = 0; index < this.mChildCount; index++) {
            View child = this.mOverviewPanelLayout.getChildAt(index);
            if (child instanceof TextView) {
                changeColorForChildPanel((TextView) child, whiteBg);
            }
        }
    }

    private void changeColorForChildPanel(TextView childPanel, boolean whiteBg) {
        WhiteBgManager.changeTextColorForBg(this.mLauncher, childPanel, whiteBg);
        WhiteBgManager.changeColorFilterForBg(this.mLauncher, childPanel.getCompoundDrawables()[1], whiteBg);
    }

    void onConfigurationChangedIfNeeded() {
        MarginLayoutParams marginLp = (MarginLayoutParams) getLayoutParams();
        marginLp.bottomMargin = getResources().getDimensionPixelOffset(R.dimen.overview_panel_margin_bottom);
        setLayoutParams(marginLp);
        updateOverviewPanelLayout();
        for (int index = 0; index < this.mChildCount; index++) {
            View child = this.mOverviewPanelLayout.getChildAt(index);
            if (child instanceof TextView) {
                updateButtonLayout((TextView) child);
            }
        }
        if (WhiteBgManager.isWhiteBg()) {
            changeColorForBg(true);
        }
    }

    private void updateButtonLayout(TextView optionButton) {
        if (optionButton != null) {
            int optionMenuHeight = getResources().getDimensionPixelOffset(R.dimen.overview_panel_options_button_height);
            int drawableSize = getResources().getDimensionPixelSize(R.dimen.overview_panel_options_button_drawable_size);
            boolean isLandscape = this.mLauncher.getDeviceProfile().isLandscape;
            optionButton.setTextSize(0, getResources().getDimension(R.dimen.overview_panel_text_size));
            optionButton.setCompoundDrawablesWithIntrinsicBounds(null, BitmapUtils.getResizedDrawable(getContext(), isLandscape ? optionButton.getCompoundDrawables()[1] : getBtnDrawable(optionButton.getId()), drawableSize, drawableSize), null, null);
            optionButton.setCompoundDrawablePadding(getResources().getDimensionPixelOffset(R.dimen.overview_panel_drawable_padding));
            LayoutParams lp = optionButton.getLayoutParams();
            lp.height = optionMenuHeight;
            optionButton.setLayoutParams(lp);
        }
    }

    private Drawable getBtnDrawable(int id) {
        Resources res = getResources();
        switch (id) {
            case R.id.wallpapers_button:
                if (sUseThemeBtn || sThemeStoreType == 1 || sThemeStoreType == 2) {
                    return res.getDrawable(R.drawable.homescreen_ic_reorder_wallpaper, null);
                }
                return res.getDrawable(R.drawable.homescreen_ic_reorder_theme, null);
            case R.id.theme_button:
                return res.getDrawable(R.drawable.homescreen_ic_reorder_theme, null);
            case R.id.widget_button:
                return res.getDrawable(R.drawable.homescreen_ic_reorder_widgets, null);
            case R.id.settings_button:
                return res.getDrawable(R.drawable.homescreen_ic_reorder_setting, null);
            default:
                return null;
        }
    }

    private void updateOverviewPanelLayout() {
        if (this.mOverviewPanelLayout == null) {
            this.mOverviewPanelLayout = (LinearLayout) findViewById(R.id.overview_panel_layout);
        }
        this.mChildCount = this.mOverviewPanelLayout.getChildCount();
        if (this.mOverviewPanelLayout != null) {
            int sideMargin = getResources().getDimensionPixelSize(R.dimen.overview_panel_margin_side);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) this.mOverviewPanelLayout.getLayoutParams();
            lp.rightMargin = sideMargin;
            lp.leftMargin = sideMargin;
            lp.weight = (float) this.mChildCount;
        }
    }

    LinearLayout getOverviewPanelLayout() {
        return this.mOverviewPanelLayout;
    }

    private void showThemeDownloadDialog(final int contentType) {
        if (this.mThemeDownloadDialog == null || !this.mThemeDownloadDialog.isShowing()) {
            Resources res = getResources();
            String samsungTheme = res.getString(R.string.samsung_theme_text);
            Builder builder = new Builder(this.mLauncher);
            builder.setTitle(res.getString(R.string.download_text, new Object[]{samsungTheme}));
            builder.setMessage(res.getString(R.string.theme_download_text, new Object[]{samsungTheme, samsungTheme}) + "\n" + res.getString(R.string.wallpaper_using_help_text, new Object[]{samsungTheme}));
            builder.setPositiveButton(res.getString(R.string.zeropage_download), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Utilities.startThemeStore(OverviewPanel.this.mLauncher, contentType);
                }
            });
            builder.setNegativeButton(res.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    dialog.dismiss();
                }
            });
            builder.setOnDismissListener(new OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    OverviewPanel.this.mThemeDownloadDialog = null;
                }
            });
            this.mThemeDownloadDialog = builder.create();
            this.mThemeDownloadDialog.setCanceledOnTouchOutside(false);
            this.mThemeDownloadDialog.show();
        }
    }

    void closeThemeDownloadDialog() {
        if (this.mThemeDownloadDialog != null) {
            this.mThemeDownloadDialog.dismiss();
            this.mThemeDownloadDialog = null;
        }
    }
}

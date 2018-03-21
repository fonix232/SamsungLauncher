package com.android.launcher3.allapps.view;

import android.animation.Animator;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnDismissListener;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.AppsTransitionAnimation;
import com.android.launcher3.allapps.controller.AppsController;
import com.android.launcher3.allapps.controller.AppsController.ViewType;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
//import com.samsung.android.graphics.spr.SemPathRenderingDrawable;
import com.sec.android.app.launcher.R;

public class AppsSearchBar extends LinearLayout implements AppsSearchWrapper {
    private static final String EXTRA_MODE_KEY = "launch_mode";
    static final String EXTRA_MODE_TEXT_INPUT = "text_input";
    private static final String EXTRA_MODE_VOICE_INPUT = "voice_input";
    private static final String SFINDER_CLS_NAME = "com.samsung.android.app.galaxyfinder.GalaxyFinderActivity";
    private static final String SFINDER_PKG_NAME = "com.samsung.android.app.galaxyfinder";
    private static final String TAG = "AppsSearchBar";
    private AppsController mAppsController;
    private OnMenuItemClickListener mMenuItemClickListener;
    private OnClickListener mMoreBtnOnClickListener;
    private ImageButton mMoreButton;
    private View mMoreButtonContainer;
    private ImageButton mMoreButtonLand;
    private PopupMenu mPopupMenu;
    private View mSearchBarContainerView;
    private OnClickListener mSearchBarViewClickListener;
    private ImageButton mSearchButtonLand;
    private EditText mSearchEditText;
    private LinearLayout mSearchEditTextWrapper;
    private OnTouchListener mSearchEditTouchListener;
    private OnKeyListener mSearchViewKeyListener;
    private ImageView mSearchVoiceButton;
    private RelativeLayout mSearchWrapper;
    private LinearLayout mSearchWrapper_land;
    private boolean mSfinderInstalled;
    private View mVoiceButtonContainer;

    public AppsSearchBar(Context context) {
        this(context, null);
    }

    public AppsSearchBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppsSearchBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mSfinderInstalled = true;
        this.mSearchViewKeyListener = new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == 1) {
                    return false;
                }
                switch (keyCode) {
                    case 4:
                    case 24:
                    case 25:
                    case 61:
                    case 111:
                        return false;
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 268:
                    case 269:
                    case 270:
                    case 271:
                        return true;
                    default:
                        AppsSearchBar.this.launchSfinder(AppsSearchBar.EXTRA_MODE_TEXT_INPUT);
                        return true;
                }
            }
        };
        this.mSearchEditTouchListener = new OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() != 1) {
                    return false;
                }
                AppsSearchBar.this.launchSfinder(AppsSearchBar.EXTRA_MODE_TEXT_INPUT);
                return true;
            }
        };
        this.mSearchBarViewClickListener = new OnClickListener() {
            public void onClick(View view) {
                String mode = view.getId() == AppsSearchBar.this.mSearchVoiceButton.getId() ? AppsSearchBar.EXTRA_MODE_VOICE_INPUT : AppsSearchBar.EXTRA_MODE_TEXT_INPUT;
                AppsSearchBar.this.mSearchEditText.clearFocus();
                AppsSearchBar.this.launchSfinder(mode);
            }
        };
        this.mMoreBtnOnClickListener = new OnClickListener() {
            public void onClick(View v) {
                AppsSearchBar.this.mSearchEditText.clearFocus();
                AppsSearchBar.this.showPopupMenu();
            }
        };
        this.mMenuItemClickListener = new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.options_menu_view_type) {
                    SALogging.getInstance().insertEventLog(AppsSearchBar.this.getResources().getString(R.string.screen_Apps_2xxx), AppsSearchBar.this.getResources().getString(R.string.event_Apps_Sort));
                    AppsSearchBar.this.mAppsController.chooseViewType();
                } else if (item.getItemId() == R.id.options_menu_contactus) {
                    SALogging.getInstance().insertEventLog(AppsSearchBar.this.getResources().getString(R.string.screen_Apps_2xxx), AppsSearchBar.this.getResources().getString(R.string.event_Apps_ContactUs));
                    AppsSearchBar.this.mAppsController.startContactUs();
                } else if (item.getItemId() == R.id.options_menu_tide_up_pages) {
                    SALogging.getInstance().insertEventLog(AppsSearchBar.this.getResources().getString(R.string.screen_Apps_2xxx), AppsSearchBar.this.getResources().getString(R.string.event_Apps_CleanUpPages));
                    AppsSearchBar.this.mAppsController.prepareTidedUpPages();
                } else if (item.getItemId() == R.id.options_menu_home_screen_settings) {
                    SALogging.getInstance().insertEventLog(AppsSearchBar.this.getResources().getString(R.string.screen_Apps_2xxx), AppsSearchBar.this.getResources().getString(R.string.event_Apps_Settings));
                    AppsSearchBar.this.mAppsController.startHomeScreenSetting();
                } else if (item.getItemId() == R.id.options_menu_sfinder_settings) {
                    SALogging.getInstance().insertEventLog(AppsSearchBar.this.getResources().getString(R.string.screen_Apps_2xxx), AppsSearchBar.this.getResources().getString(R.string.event_Apps_Finder_Settings));
                    AppsSearchBar.this.mAppsController.startSfinderSettingActivity();
                } else if (item.getItemId() != R.id.options_menu_galaxy_essentials) {
                    return false;
                } else {
                    AppsSearchBar.this.mAppsController.startGalaxyEssentials();
                    SALogging.getInstance().insertEventLog(AppsSearchBar.this.getResources().getString(R.string.screen_Apps_2xxx), AppsSearchBar.this.getResources().getString(R.string.event_GalaxyEssentials));
                }
                return true;
            }
        };
    }

    protected void onFinishInflate() {
        int i;
        super.onFinishInflate();
        this.mSearchWrapper_land = (LinearLayout) findViewById(R.id.app_search_wrapper_land);
        this.mSearchWrapper = (RelativeLayout) findViewById(R.id.app_search_wrapper);
        this.mSearchEditTextWrapper = (LinearLayout) findViewById(R.id.app_search_edit_text_wrapper);
        this.mSearchEditTextWrapper.setOnKeyListener(this.mSearchViewKeyListener);
        this.mSearchEditText = (EditText) findViewById(R.id.app_search_edit_text);
        this.mSearchEditText.setBackgroundColor(0);
        this.mSearchEditText.setPadding(0, 0, 0, 0);
        this.mSearchEditText.setOnClickListener(this.mSearchBarViewClickListener);
        this.mSearchEditText.setOnTouchListener(this.mSearchEditTouchListener);
        if (LauncherFeature.isTablet()) {
            this.mSearchEditText.setHint(getDecoratedHint(getResources().getString(R.string.app_search_tablet)));
        } else {
            this.mSearchEditText.setHint(getDecoratedHint(getResources().getString(R.string.app_search)));
        }
        this.mSearchVoiceButton = (ImageButton) findViewById(R.id.voice_search_icon_imageview);
        this.mVoiceButtonContainer = findViewById(R.id.voice_search_button);
        View view = this.mVoiceButtonContainer;
        if (((Launcher) getContext()).hasVoiceSearch()) {
            i = 0;
        } else {
            i = 8;
        }
        view.setVisibility(i);
        ColorFilter filter = new LightingColorFilter(getResources().getColor(R.color.apps_search_titlebar_color, null), 0);
        this.mSearchVoiceButton.setColorFilter(filter);
        this.mSearchVoiceButton.setImageAlpha(179);
        this.mSearchBarContainerView = findViewById(R.id.search_container);
        this.mMoreButtonLand = (ImageButton) findViewById(R.id.more_icon_imageview_land);
        this.mSearchButtonLand = (ImageButton) findViewById(R.id.search_icon_imageview_land);
        if (Utilities.isPackageExist(getContext(), SFINDER_PKG_NAME)) {
            DeviceProfile dp = ((Launcher) getContext()).getDeviceProfile();
            if ((dp.isVerticalBarLayout() || dp.isLandscape) && !LauncherFeature.isTablet()) {
                resizeButtonAndImageOnLand();
                this.mSearchWrapper.setVisibility(View.GONE);
                this.mSearchWrapper_land.setVisibility(View.VISIBLE);
            } else {
                this.mSearchWrapper.setVisibility(View.VISIBLE);
                this.mSearchWrapper_land.setVisibility(View.GONE);
            }
            this.mMoreButtonContainer = findViewById(R.id.more_button_search);
            this.mMoreButton = (ImageButton) findViewById(R.id.more_icon_imageview);
            findViewById(R.id.more_button_guest_container).setVisibility(View.GONE);
            findViewById(R.id.more_icon_guest).setVisibility(View.GONE);
            this.mMoreButton.setColorFilter(filter);
            this.mMoreButton.setImageAlpha(179);
        } else {
            this.mSfinderInstalled = false;
            this.mSearchBarContainerView.setPadding(0, 0, (int) getResources().getDimension(R.dimen.apps_search_titlebar_guest_margin_right), 0);
            this.mMoreButtonContainer = findViewById(R.id.more_button_guest_container);
            this.mMoreButton = (ImageButton) findViewById(R.id.more_icon_guest);
            this.mSearchWrapper.setVisibility(View.GONE);
            this.mSearchWrapper_land.setVisibility(View.GONE);
            this.mMoreButtonContainer.setVisibility(View.VISIBLE);
            this.mMoreButton.setVisibility(View.VISIBLE);
        }
        this.mSearchVoiceButton.setOnClickListener(this.mSearchBarViewClickListener);
        this.mMoreButton.setOnClickListener(this.mMoreBtnOnClickListener);
        this.mMoreButtonLand.setOnClickListener(this.mMoreBtnOnClickListener);
        this.mSearchButtonLand.setOnClickListener(this.mSearchBarViewClickListener);
    }

    private CharSequence getDecoratedHint(CharSequence hintText) {
        int textSize = (int) (((double) this.mSearchEditText.getTextSize()) * 1.25d);
        Drawable searchIcon = getContext().getDrawable(R.drawable.tw_ic_search_api_mtrl_alpha);
        DrawableCompat.setTint(searchIcon, getContext().getColor(R.color.apps_search_titlebar_color));
        searchIcon.setBounds(1, -15, textSize + 1, textSize - 15);
        SpannableStringBuilder ssb = new SpannableStringBuilder("  ");
        ssb.append(hintText);
        ssb.setSpan(new ImageSpan(searchIcon), 0, 1, 33);
        return ssb;
    }

    private void setupAppsOptionsMenu(Menu menu) {
        boolean z = false;
        MenuItem viewType = menu.findItem(R.id.options_menu_view_type);
        MenuItem contactUs = menu.findItem(R.id.options_menu_contactus);
        MenuItem tideUpPages = menu.findItem(R.id.options_menu_tide_up_pages);
        MenuItem search = menu.findItem(R.id.options_menu_search_recommend);
        MenuItem clearhistory = menu.findItem(R.id.options_menu_clear_search_history);
        MenuItem homeScreenSettings = menu.findItem(R.id.options_menu_home_screen_settings);
        MenuItem sFinderSettings = menu.findItem(R.id.options_menu_sfinder_settings);
        MenuItem galaxyEssentials = menu.findItem(R.id.options_menu_galaxy_essentials);
        search.setVisible(false);
        clearhistory.setVisible(false);
        tideUpPages.setVisible(false);
        viewType.setVisible(true);
        homeScreenSettings.setVisible(true);
        sFinderSettings.setVisible(Utilities.isPackageExist(getContext(), SFINDER_PKG_NAME));
        if (Utilities.isSamsungMembersEnabled(getContext())) {
            contactUs.setVisible(true);
        } else {
            contactUs.setVisible(false);
        }
        if (!Utilities.isKnoxMode() && Utilities.isSamsungAppEnabled(getContext())) {
            z = true;
        }
        galaxyEssentials.setVisible(z);
        if (this.mAppsController.getViewType() == ViewType.CUSTOM_GRID) {
            tideUpPages.setVisible(true);
        }
    }

    public void showPopupMenu() {
        this.mAppsController.initBounceAnimation();
        if (this.mPopupMenu != null) {
            this.mPopupMenu.dismiss();
            this.mPopupMenu = null;
        }
        this.mPopupMenu = new PopupMenu(getContext(), this.mSearchBarContainerView, GravityCompat.END);
        this.mPopupMenu.getMenuInflater().inflate(R.menu.apps_options_menu, this.mPopupMenu.getMenu());
        this.mPopupMenu.setOnMenuItemClickListener(this.mMenuItemClickListener);
        setupAppsOptionsMenu(this.mPopupMenu.getMenu());
        this.mPopupMenu.setOnDismissListener(new OnDismissListener() {
            public void onDismiss(PopupMenu menu) {
                AppsSearchBar.this.mPopupMenu = null;
            }
        });
        this.mPopupMenu.show();
        SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_Apps_2xxx), "0");
    }

    public boolean launchSfinder(String mode) {
        if (!Utilities.isPackageExist(getContext(), SFINDER_PKG_NAME)) {
            return false;
        }
        String eventId;
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(SFINDER_PKG_NAME, SFINDER_CLS_NAME));
        intent.putExtra(EXTRA_MODE_KEY, mode);
        try {
            getContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Unable to start S Finder.  intent=" + intent, e);
        }
        if (EXTRA_MODE_TEXT_INPUT.equals(mode)) {
            eventId = getResources().getString(R.string.event_Apps_Search);
        } else {
            eventId = getResources().getString(R.string.event_Apps_VoiceSearch);
        }
        GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_APP_SEARCH, null, -1, false);
        SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_Apps_2xxx), eventId);
        return true;
    }

    public void resume() {
        this.mSearchEditText.clearFocus();
    }

    public View getContainerView() {
        return this;
    }

    public void setController(AppsController controller) {
        this.mAppsController = controller;
    }

    public void hidePopupMenu() {
        if (this.mPopupMenu != null) {
            this.mPopupMenu.dismiss();
            this.mPopupMenu = null;
        }
    }

    public void stageExit(StageEntry data) {
        hidePopupMenu();
    }

    public Animator switchInternalState(AppsTransitionAnimation appsAnim, StageEntry data) {
        return null;
    }

    public void updateRecentApp(IconInfo item) {
    }

    public void onConfigurationChangedIfNeeded() {
        Resources res = getContext().getResources();
        ((LayoutParams) getLayoutParams()).setMargins(0, res.getDimensionPixelSize(R.dimen.all_apps_search_view_margin_top), 0, 0);
        if (this.mSfinderInstalled) {
            if (!((Launcher) getContext()).getDeviceProfile().isLandscape || LauncherFeature.isTablet()) {
                this.mSearchWrapper.setVisibility(View.VISIBLE);
                this.mSearchWrapper_land.setVisibility(View.GONE);
            } else {
                resizeButtonAndImageOnLand();
                this.mSearchWrapper.setVisibility(View.GONE);
                this.mSearchWrapper_land.setVisibility(View.VISIBLE);
            }
        }
        if (this.mSearchBarContainerView != null) {
            this.mSearchBarContainerView.setPadding(res.getDimensionPixelSize(R.dimen.apps_search_titlebar_margin_left), 0, res.getDimensionPixelSize(R.dimen.apps_search_titlebar_margin_right), 0);
        }
    }

    public View getAppsSearchBarView() {
        return getContainerView();
    }

    private void resizeButtonAndImageOnLand() {
        Bitmap searchBitmap;
        Bitmap moreBitmap;
        Resources res = getContext().getResources();
        int backgroudSize = res.getDimensionPixelSize(R.dimen.more_icon_btn_bg_width_land);
        LayoutParams moreParams = (LayoutParams) this.mMoreButtonLand.getLayoutParams();
        moreParams.height = backgroudSize;
        moreParams.width = backgroudSize;
        LayoutParams searchParams = (LayoutParams) this.mSearchButtonLand.getLayoutParams();
        searchParams.height = backgroudSize;
        searchParams.width = backgroudSize;
        int iconSize = res.getDimensionPixelSize(R.dimen.more_icon_btn_width_land);
        Drawable searchDrawable = this.mSearchButtonLand.getDrawable();
        // TODO: Samsung specific code
//        if (searchDrawable instanceof SemPathRenderingDrawable) {
//            searchBitmap = ((SemPathRenderingDrawable) searchDrawable).getBitmap();
//        } else {
            searchBitmap = ((BitmapDrawable) searchDrawable).getBitmap();
        //}
        this.mSearchButtonLand.setImageDrawable(new BitmapDrawable(res, Bitmap.createScaledBitmap(searchBitmap, iconSize, iconSize, true)));
        Drawable moreDrawable = this.mMoreButtonLand.getDrawable();
        // TODO: Samsung specific code
//        if (moreDrawable instanceof SemPathRenderingDrawable) {
//            moreBitmap = ((SemPathRenderingDrawable) moreDrawable).getBitmap();
//        } else {
            moreBitmap = ((BitmapDrawable) moreDrawable).getBitmap();
        //}
        this.mMoreButtonLand.setImageDrawable(new BitmapDrawable(res, Bitmap.createScaledBitmap(moreBitmap, iconSize, iconSize, true)));
    }
}

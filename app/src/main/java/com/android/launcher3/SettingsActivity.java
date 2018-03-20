package com.android.launcher3;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.launcher3.common.model.BadgeSettingsFragment;
import com.android.launcher3.common.model.LauncherSettings.Settings;
import com.android.launcher3.home.AppsButtonSettingsActivity;
import com.android.launcher3.util.GlobalSettingUtils;
import com.android.launcher3.util.ScreenGridUtilities;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;

public class SettingsActivity extends Activity {
    private static final String APP_ICON_BADGE_INTENT = "com.samsung.settings.APP_ICON_BADGES_SETTINGS";
    private static final int BADGE_MANAGER_FRAGMENT = 1;
    private static final String NOTIFICATION_BADGING = "notification_badging";
    private static final int SETTINGS_FRAGMENT = 0;
    private static final String TAG = "SettingsActivity";
    private static final String VISIBLE_FRAGMENT = "visible_fragment";
    private ActionBar mActionBar = null;
    private BadgeSettingsFragment mBadgeSettingsFragment = null;
    private LauncherSettingsFragment mLauncherSettingsFragment = null;
    private int mVisibleFragment = 0;

    public static class LauncherSettingsFragment extends PreferenceFragment implements OnPreferenceChangeListener {
        private static final int EASY_MODE = 0;
        private static final String HOME_SETTING_SHOW_EASY_MODE_TIPS = "home_setting_show_easy_mode_tips";
        private static final int STANDARD_MODE = 1;
        private ImageView mEasyModeDeleteButton;
        private TextView mEasyModeSettingButton;
        private boolean mIsEasyMode;
        private Preference mPreAboutPage;
        private SwitchPreference mPreAddAppsToHomeScreenSetting;
        private SwitchPreference mPreAppIconBadgeSetting;
        private Preference mPreAppsButtonSetting;
        private Preference mPreAppsScreenGrid;
        private Preference mPreDayLiteSetting;
        private SwitchPreference mPreNotificationPanelSetting;
        private SwitchPreference mPreOnlyPortraitModeSetting;
        private Preference mPreScreenGrid;
        private Preference mPreWidget;
        private Preference mPrefHomeScreenMode;

        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            ViewGroup root = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
            if (root != null && LauncherFeature.supportEasyModeChange() && this.mIsEasyMode && getContext().getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).getBoolean(HOME_SETTING_SHOW_EASY_MODE_TIPS, true)) {
                LinearLayout easyModeSettingLayout = (LinearLayout) inflater.inflate(R.layout.home_setting_easy_mode_layout, container, false);
                root.addView(easyModeSettingLayout, 0);
                this.mEasyModeDeleteButton = (ImageView) root.findViewById(R.id.easy_mode_delete_button);
                this.mEasyModeSettingButton = (TextView) root.findViewById(R.id.easy_mode_setting_button);
                if (Utilities.isEnableBtnBg(getContext())) {
                    this.mEasyModeSettingButton.setBackgroundResource(R.drawable.tw_text_action_btn_material_light);
                }
                setEasyModeSetting(root, easyModeSettingLayout);
            }
            return root;
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.launcher_preferences);
            getPreferenceManager().setSharedPreferencesName(LauncherAppState.getSharedPreferencesKey());
            this.mPreScreenGrid = findPreference(Utilities.GRID_PREFERENCE_KEY);
            this.mPreAppsButtonSetting = findPreference(Utilities.APPS_BUTTON_SETTING_PREFERENCE_KEY);
            this.mPreNotificationPanelSetting = (SwitchPreference) findPreference(Utilities.NOTIFICATION_PANEL_SETTING_PREFERENCE_KEY);
            this.mPreOnlyPortraitModeSetting = (SwitchPreference) findPreference(Utilities.ONLY_PORTRAIT_MODE_SETTING_PREFERENCE_KEY);
            this.mPreAddAppsToHomeScreenSetting = (SwitchPreference) findPreference(Utilities.ADD_APPS_TO_HOME_SCREEN_PREFERENCE_KEY);
            this.mPreAppIconBadgeSetting = (SwitchPreference) findPreference(Utilities.APP_ICON_BADGES_PREFERENCE_KEY);
            this.mPrefHomeScreenMode = findPreference(Utilities.HOMESCREEN_MODE_PREFERENCE_KEY);
            this.mPreWidget = findPreference(Utilities.WIDGETS_PREFERENCE_KEY);
            this.mPreDayLiteSetting = findPreference(Utilities.DAYLITE_SETTING_PREFERENCE_KEY);
            this.mPreAppsScreenGrid = findPreference(Utilities.APPS_GRID_PREFERENCE_KEY);
            this.mPreAboutPage = findPreference(Utilities.ABOUT_PREFERENCE_KEY);
            this.mIsEasyMode = System.getInt(getActivity().getContentResolver(), "easy_mode_switch", 1) == 0;
            if (this.mIsEasyMode != LauncherAppState.getInstance().isEasyModeEnabled()) {
                getActivity().finish();
                return;
            }
            if (LauncherFeature.supportFlexibleGrid()) {
                setHomeScreenGrid(savedInstanceState);
            } else {
                getPreferenceScreen().removePreference(this.mPreScreenGrid);
            }
            setHomeScreenMode();
            setBadgeManager();
            setHideApps();
            if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                getPreferenceScreen().removePreference(this.mPreAppsButtonSetting);
            } else {
                setAppsButtonSetting();
            }
            if (LauncherFeature.supportEasyModeChange() && this.mIsEasyMode) {
                getPreferenceScreen().removePreference(this.mPreScreenGrid);
                getPreferenceScreen().removePreference(this.mPrefHomeScreenMode);
                setAppsButtonSetting();
            }
            if (LauncherFeature.supportNotificationPanelExpansion()) {
                setNotificationPanelSetting();
            } else {
                getPreferenceScreen().removePreference(this.mPreNotificationPanelSetting);
            }
            if (LauncherFeature.supportAboutPage()) {
                this.mPreAboutPage.setTitle(String.format(getString(R.string.about), new Object[]{getString(R.string.about_home_screen)}));
                setAboutPage();
            } else {
                getPreferenceScreen().removePreference(this.mPreAboutPage);
            }
            getPreferenceScreen().removePreference(this.mPreWidget);
            getPreferenceScreen().removePreference(this.mPreDayLiteSetting);
            if (!LauncherFeature.supportFlexibleGrid() || LauncherAppState.getInstance().isHomeOnlyModeEnabled() || !LauncherFeature.supportEasyModeChange() || this.mIsEasyMode) {
                getPreferenceScreen().removePreference(this.mPreAppsScreenGrid);
            } else {
                setAppsScreenMode(savedInstanceState);
            }
            if (LauncherFeature.supportRotationSetting()) {
                setOnlyPortraitModeSetting();
            } else {
                getPreferenceScreen().removePreference(this.mPreOnlyPortraitModeSetting);
            }
            if (Utilities.ATLEAST_O) {
                setAppIconBadgesSetting();
                if (LauncherFeature.isChinaModel() || LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                    getPreferenceScreen().removePreference(this.mPreAddAppsToHomeScreenSetting);
                    return;
                } else {
                    setAddAppsToHomeScreenSetting();
                    return;
                }
            }
            getPreferenceScreen().removePreference(this.mPreAppIconBadgeSetting);
            getPreferenceScreen().removePreference(this.mPreAddAppsToHomeScreenSetting);
        }

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Bundle extras = new Bundle();
            extras.putBoolean("value", ((Boolean) newValue).booleanValue());
            getActivity().getContentResolver().call(Settings.CONTENT_URI, Settings.METHOD_SET_BOOLEAN, preference.getKey(), extras);
            return true;
        }

        private void setBadgeManager() {
            Preference mBadgeManage = findPreference(Utilities.BADGE_MANAGER_PREFERENCE_KEY);
            if (mBadgeManage != null) {
                mBadgeManage.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference arg0) {
                        SALogging.getInstance().insertEventLog(LauncherSettingsFragment.this.getResources().getString(R.string.screen_HomeSettings), LauncherSettingsFragment.this.getResources().getString(R.string.event_ManageAppBadges));
                        ((SettingsActivity) LauncherSettingsFragment.this.getActivity()).showBadgeManagerSettings();
                        return false;
                    }
                });
            }
            if (!LauncherFeature.isSupportBadgeManage()) {
                PreferenceScreen preferenceScreen = getPreferenceScreen();
                if (preferenceScreen != null) {
                    preferenceScreen.removePreference(mBadgeManage);
                }
            }
        }

        private void setHomeScreenMode() {
            if (this.mPrefHomeScreenMode != null) {
                this.mPrefHomeScreenMode.semSetSummaryColorToColorPrimaryDark(true);
                if (LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                    this.mPrefHomeScreenMode.setSummary(R.string.home_screen_mode_only_home);
                } else {
                    this.mPrefHomeScreenMode.setSummary(R.string.home_screen_mode_apps);
                }
                this.mPrefHomeScreenMode.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference arg0) {
                        SALogging.getInstance().insertEventLog(LauncherSettingsFragment.this.getResources().getString(R.string.screen_HomeSettings), LauncherSettingsFragment.this.getResources().getString(R.string.event_HomeScreenLayout));
                        if (LauncherSettingsFragment.this.getActivity() != null) {
                            try {
                                Intent intent = new Intent();
                                intent.setClassName(LauncherSettingsFragment.this.getActivity().getPackageName(), "com.android.launcher3.home.HomeModeChangeActivity");
                                LauncherSettingsFragment.this.getActivity().startActivity(intent);
                            } catch (SecurityException e) {
                                Log.w(SettingsActivity.TAG, "SecurityException e = " + e);
                            }
                        }
                        return false;
                    }
                });
            }
        }

        private void setAppsScreenMode(Bundle savedInstanceState) {
            String summary;
            int[] xy = new int[2];
            ScreenGridUtilities.loadCurrentAppsGridSize(LauncherAppState.getInstance().getContext(), xy);
            int columns = xy[0];
            int rows = xy[1];
            String currentLanguage = Utilities.getLocale(getContext()).getLanguage();
            if (savedInstanceState != null && columns == 0 && rows == 0) {
                summary = savedInstanceState.getString(Utilities.APPS_SCREEN_GRID_SUMMARY);
            } else if ("ar".equals(currentLanguage) || "fa".equals(currentLanguage)) {
                summary = Utilities.toArabicDigits(String.valueOf(rows), currentLanguage) + "X" + Utilities.toArabicDigits(String.valueOf(columns), currentLanguage);
            } else {
                summary = Integer.toString(columns) + "X" + Integer.toString(rows);
            }
            if (this.mPreAppsScreenGrid != null) {
                this.mPreAppsScreenGrid.setSummary(summary);
                this.mPreAppsScreenGrid.semSetSummaryColorToColorPrimaryDark(true);
                this.mPreAppsScreenGrid.setEnabled(true);
                this.mPreAppsScreenGrid.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference arg0) {
                        if (LauncherSettingsFragment.this.getActivity() != null) {
                            SALogging.getInstance().insertEventLog(LauncherSettingsFragment.this.getResources().getString(R.string.screen_HomeSettings), LauncherSettingsFragment.this.getResources().getString(R.string.event_AppsScreenGrid));
                            Intent intent = new Intent();
                            intent.setAction("android.intent.action.MAIN");
                            intent.putExtra(Utilities.EXTRA_ENTER_APPS_SCREEN_GRID, true);
                            intent.addCategory("android.intent.category.HOME");
                            LauncherSettingsFragment.this.getActivity().startActivity(intent);
                        }
                        return false;
                    }
                });
            }
        }

        private void setHideApps() {
            Preference prefHideApps = findPreference(Utilities.HIDE_APPS_PREFERENCE_KEY);
            if (prefHideApps != null) {
                prefHideApps.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference arg0) {
                        Activity settingActivity = LauncherSettingsFragment.this.getActivity();
                        SALogging.getInstance().insertEventLog(LauncherSettingsFragment.this.getResources().getString(R.string.screen_HomeSettings), LauncherSettingsFragment.this.getResources().getString(R.string.event_HideApps));
                        if (settingActivity != null) {
                            Intent intent = new Intent();
                            intent.setAction("android.intent.action.MAIN");
                            intent.putExtra(Utilities.EXTRA_ENTER_HIDE_APPS, true);
                            intent.addCategory("android.intent.category.HOME");
                            settingActivity.startActivity(intent);
                        }
                        return false;
                    }
                });
            }
        }

        private void setAboutPage() {
            Preference prefNotice = findPreference(Utilities.ABOUT_PREFERENCE_KEY);
            if (prefNotice != null) {
                prefNotice.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference arg0) {
                        Activity settingActivity = LauncherSettingsFragment.this.getActivity();
                        SALogging.getInstance().insertEventLog(LauncherSettingsFragment.this.getResources().getString(R.string.screen_HomeSettings), LauncherSettingsFragment.this.getResources().getString(R.string.event_AboutPage));
                        if (settingActivity != null) {
                            try {
                                Intent intent = new Intent();
                                intent.setClassName(LauncherSettingsFragment.this.getActivity().getPackageName(), "com.android.launcher3.AboutPageActivity");
                                LauncherSettingsFragment.this.getActivity().startActivity(intent);
                            } catch (SecurityException e) {
                                Log.w(SettingsActivity.TAG, "SecurityException e = " + e);
                            }
                        }
                        return false;
                    }
                });
            }
        }

        private void setAppsButtonSetting() {
            if (this.mPreAppsButtonSetting != null) {
                this.mPreAppsButtonSetting.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference arg0) {
                        SALogging.getInstance().insertEventLog(LauncherSettingsFragment.this.getResources().getString(R.string.screen_HomeSettings), LauncherSettingsFragment.this.getResources().getString(R.string.event_AppsButton));
                        if (LauncherSettingsFragment.this.getActivity() != null) {
                            try {
                                Intent intent = new Intent();
                                intent.setClassName(LauncherSettingsFragment.this.getActivity().getPackageName(), "com.android.launcher3.home.AppsButtonSettingsActivity");
                                LauncherSettingsFragment.this.getActivity().startActivity(intent);
                            } catch (SecurityException e) {
                                Log.w(SettingsActivity.TAG, "SecurityException e = " + e);
                            }
                        }
                        return false;
                    }
                });
            }
        }

        void updatePreNotificationPanelSetting(boolean value) {
            if (this.mPreNotificationPanelSetting != null) {
                this.mPreNotificationPanelSetting.setChecked(value);
            }
        }

        private void setNotificationPanelSetting() {
            if (this.mPreNotificationPanelSetting != null) {
                this.mPreNotificationPanelSetting.setChecked(LauncherAppState.getInstance().getNotificationPanelExpansionEnabled());
                this.mPreNotificationPanelSetting.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        LauncherAppState.getInstance().setNotificationPanelExpansionEnabled(((Boolean) newValue).booleanValue(), false);
                        return true;
                    }
                });
            }
        }

        private void setWidgetsButtonSetting() {
            if (this.mPreWidget != null) {
                this.mPreWidget.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference arg0) {
                        if (LauncherSettingsFragment.this.getActivity() != null) {
                            Intent intent = new Intent();
                            intent.setAction("android.intent.action.MAIN");
                            intent.putExtra(Utilities.EXTRA_ENTER_WIDGETS, true);
                            intent.addCategory("android.intent.category.HOME");
                            LauncherSettingsFragment.this.getActivity().startActivity(intent);
                        }
                        return false;
                    }
                });
            }
        }

        private void setDayLiteSetting() {
            if (this.mPreDayLiteSetting != null) {
                this.mPreDayLiteSetting.setTitle(getString(R.string.title_settings, new Object[]{Utilities.getAppLabel(getContext(), Utilities.DAYLITE_PACKAGE_NAME)}));
                this.mPreDayLiteSetting.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference arg0) {
                        if (LauncherSettingsFragment.this.getActivity() != null) {
                            try {
                                Intent intent = new Intent();
                                intent.setClassName(Utilities.DAYLITE_PACKAGE_NAME, Utilities.DAYLITE_CLASS_NAME_SETTING);
                                LauncherSettingsFragment.this.getActivity().startActivity(intent);
                            } catch (SecurityException e) {
                                Log.w(SettingsActivity.TAG, "SecurityException e = " + e);
                            }
                        }
                        return false;
                    }
                });
            }
        }

        private void setOnlyPortraitModeSetting() {
            if (this.mPreOnlyPortraitModeSetting != null) {
                this.mPreOnlyPortraitModeSetting.setChecked(Utilities.isOnlyPortraitMode());
                this.mPreOnlyPortraitModeSetting.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object obj) {
                        boolean value = ((Boolean) obj).booleanValue();
                        SALogging.getInstance().insertEventLog(LauncherSettingsFragment.this.getResources().getString(R.string.screen_HomeSettings), LauncherSettingsFragment.this.getResources().getString(R.string.event_PortraitModeOnly), value ? 1 : 0);
                        if (value) {
                            LauncherSettingsFragment.this.getActivity().setRequestedOrientation(5);
                        } else {
                            LauncherSettingsFragment.this.getActivity().setRequestedOrientation(-1);
                        }
                        Utilities.setOnlyPortraitMode(LauncherSettingsFragment.this.getContext(), value);
                        return true;
                    }
                });
            }
        }

        private void setAppIconBadgesSetting() {
            boolean z = true;
            if (this.mPreAppIconBadgeSetting != null) {
                SwitchPreference switchPreference = this.mPreAppIconBadgeSetting;
                if (Secure.getInt(getActivity().getContentResolver(), SettingsActivity.NOTIFICATION_BADGING, 1) != 1) {
                    z = false;
                }
                switchPreference.setChecked(z);
                this.mPreAppIconBadgeSetting.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        Secure.putInt(LauncherSettingsFragment.this.getActivity().getContentResolver(), SettingsActivity.NOTIFICATION_BADGING, ((Boolean) newValue).booleanValue() ? 1 : 0);
                        return true;
                    }
                });
                this.mPreAppIconBadgeSetting.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent();
                        intent.setAction(SettingsActivity.APP_ICON_BADGE_INTENT);
                        try {
                            LauncherSettingsFragment.this.getActivity().startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            Log.e(SettingsActivity.TAG, "Unable to launch. app icon badge intent=" + e);
                        }
                        return true;
                    }
                });
            }
        }

        private void setAddAppsToHomeScreenSetting() {
            if (this.mPreAddAppsToHomeScreenSetting != null) {
                final SharedPreferences prefs = getContext().getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
                this.mPreAddAppsToHomeScreenSetting.setChecked(prefs.getBoolean(Utilities.ADD_ICON_PREFERENCE_KEY, false));
                this.mPreAddAppsToHomeScreenSetting.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        SALogging.getInstance().insertEventLog(LauncherSettingsFragment.this.getResources().getString(R.string.screen_HomeSettings), LauncherSettingsFragment.this.getResources().getString(R.string.event_AddAppsToHome), ((Boolean) newValue).booleanValue() ? 1 : 0);
                        SALogging.getInstance().insertStatusLog(LauncherSettingsFragment.this.getResources().getString(R.string.status_AddAppsToHome), ((Boolean) newValue).booleanValue() ? 1 : 0);
                        prefs.edit().putBoolean(Utilities.ADD_ICON_PREFERENCE_KEY, ((Boolean) newValue).booleanValue()).apply();
                        return true;
                    }
                });
            }
        }

        private void setEasyModeSetting(final ViewGroup root, final View easyModeSettingLayout) {
            if (this.mEasyModeDeleteButton != null) {
                this.mEasyModeDeleteButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        root.removeView(easyModeSettingLayout);
                        Editor editor = LauncherSettingsFragment.this.getContext().getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
                        editor.putBoolean(LauncherSettingsFragment.HOME_SETTING_SHOW_EASY_MODE_TIPS, false);
                        editor.apply();
                    }
                });
            }
            if (this.mEasyModeSettingButton != null) {
                this.mEasyModeSettingButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        if (LauncherSettingsFragment.this.getActivity() != null) {
                            try {
                                Intent intent = new Intent();
                                intent.setClassName("com.android.settings", "com.android.settings.Settings$EasyModeAppActivity");
                                LauncherSettingsFragment.this.getActivity().startActivity(intent);
                                root.removeView(easyModeSettingLayout);
                                Editor editor = LauncherSettingsFragment.this.getContext().getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
                                editor.putBoolean(LauncherSettingsFragment.HOME_SETTING_SHOW_EASY_MODE_TIPS, false);
                                editor.apply();
                            } catch (ActivityNotFoundException e) {
                                Log.w(SettingsActivity.TAG, "ActivityNotFoundException e = " + e);
                            }
                        }
                    }
                });
            }
        }

        public void onSaveInstanceState(Bundle outState) {
            if (this.mPreScreenGrid != null) {
                outState.putString(Utilities.SCREEN_GRID_SUMMARY, (String) this.mPreScreenGrid.getSummary());
            }
            if (this.mPreAppsScreenGrid != null) {
                outState.putString(Utilities.APPS_SCREEN_GRID_SUMMARY, (String) this.mPreAppsScreenGrid.getSummary());
            }
            super.onSaveInstanceState(outState);
        }

        private void setHomeScreenGrid(Bundle savedInstanceState) {
            String summary;
            int[] xy = new int[2];
            Utilities.loadCurrentGridSize(LauncherAppState.getInstance().getContext(), xy);
            int columns = xy[0];
            int rows = xy[1];
            String currentLanguage = Utilities.getLocale(getContext()).getLanguage();
            if (savedInstanceState != null && columns == 0 && rows == 0) {
                summary = savedInstanceState.getString(Utilities.SCREEN_GRID_SUMMARY);
            } else if ("ar".equals(currentLanguage) || "fa".equals(currentLanguage)) {
                summary = Utilities.toArabicDigits(String.valueOf(rows), currentLanguage) + "X" + Utilities.toArabicDigits(String.valueOf(columns), currentLanguage);
            } else {
                summary = Integer.toString(columns) + "X" + Integer.toString(rows);
            }
            if (this.mPreScreenGrid != null) {
                this.mPreScreenGrid.setSummary(summary);
                this.mPreScreenGrid.semSetSummaryColorToColorPrimaryDark(true);
                this.mPreScreenGrid.setEnabled(true);
                this.mPreScreenGrid.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference arg0) {
                        SALogging.getInstance().insertEventLog(LauncherSettingsFragment.this.getResources().getString(R.string.screen_HomeSettings), LauncherSettingsFragment.this.getResources().getString(R.string.event_HomeScreenGrid));
                        if (LauncherSettingsFragment.this.getActivity() != null) {
                            Intent intent = new Intent();
                            intent.setAction("android.intent.action.MAIN");
                            intent.putExtra(Utilities.EXTRA_ENTER_SCREEN_GRID, true);
                            intent.addCategory("android.intent.category.HOME");
                            LauncherSettingsFragment.this.getActivity().startActivity(intent);
                        }
                        return false;
                    }
                });
            }
        }
    }

    protected void onResume() {
        super.onResume();
        AppsButtonSettingsActivity appsButtonActivity = LauncherAppState.getInstance().getAppsButtonSettingsActivity();
        if (appsButtonActivity != null) {
            appsButtonActivity.finish();
            LauncherAppState.getInstance().setAppsButtonSettingsActivity(null);
        }
        if (this.mLauncherSettingsFragment != null && Utilities.ATLEAST_O) {
            this.mLauncherSettingsFragment.setAppIconBadgesSetting();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mActionBar = getActionBar();
        if (this.mActionBar != null) {
            this.mActionBar.setDisplayOptions(8);
            this.mActionBar.setDisplayShowTitleEnabled(true);
            this.mActionBar.setDisplayHomeAsUpEnabled(true);
        }
        if (Utilities.isOnlyPortraitMode()) {
            setRequestedOrientation(5);
        } else {
            setRequestedOrientation(-1);
        }
        setContentView(R.layout.settings_main);
        LauncherFeature.setSupportFlexibleGrid(getResources().getBoolean(R.bool.is_supportFlexibleGrid));
        this.mLauncherSettingsFragment = (LauncherSettingsFragment) getFragmentManager().findFragmentById(R.id.screen_settings);
        this.mBadgeSettingsFragment = (BadgeSettingsFragment) getFragmentManager().findFragmentById(R.id.badge_settings);
        if (savedInstanceState != null) {
            int visibleFragment = savedInstanceState.getInt(VISIBLE_FRAGMENT, 0);
            if (visibleFragment == 0) {
                showSettingsFragment();
            } else if (visibleFragment == 1) {
                showBadgeManagerSettings();
            }
        } else {
            Log.d(TAG, "onCreate: showHomeScreenSettings");
            showSettingsFragment();
        }
        LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(6);
        LauncherAppState.getInstance().setSettingsActivity(this);
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(VISIBLE_FRAGMENT, this.mVisibleFragment);
        super.onSaveInstanceState(outState);
    }

    public void onBackPressed() {
        Log.d(TAG, "onBackPressed()");
        if (this.mLauncherSettingsFragment != null && this.mVisibleFragment == 0) {
            super.onBackPressed();
            if (GlobalSettingUtils.getStartSetting() && !GlobalSettingUtils.getSettingMultiWindow()) {
                startSettingApp();
            }
        } else if (this.mBadgeSettingsFragment != null && this.mVisibleFragment == 1) {
            showSettingsFragment();
            this.mBadgeSettingsFragment.startDatabaseLoader();
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 16908332:
                onBackPressed();
                SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_HomeSettings), getResources().getString(R.string.event_NavigateUp));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showSettingsFragment() {
        this.mVisibleFragment = 0;
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (this.mBadgeSettingsFragment != null) {
            transaction.hide(this.mBadgeSettingsFragment);
        }
        transaction.show(this.mLauncherSettingsFragment);
        transaction.commit();
        if (this.mActionBar != null) {
            this.mActionBar.setDisplayShowCustomEnabled(false);
            this.mActionBar.setDisplayShowTitleEnabled(true);
            this.mActionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle(GlobalSettingUtils.getStartSetting() ? R.string.home_screen_global_setting_title : R.string.homes_screen_settings);
    }

    public void showBadgeManagerSettings() {
        Log.d(TAG, "showBadgeManagerSettings: " + (this.mBadgeSettingsFragment != null));
        this.mVisibleFragment = 1;
        this.mBadgeSettingsFragment.updateList();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.hide(this.mLauncherSettingsFragment);
        transaction.show(this.mBadgeSettingsFragment);
        transaction.commit();
        setTitle(R.string.badge_manage_title);
    }

    public boolean enableAllAppsBadge(boolean enable) {
        if (!isBadgeManagerFragmentShowing()) {
            return false;
        }
        this.mBadgeSettingsFragment.enableAllAppsbadge(enable);
        return true;
    }

    public boolean enableSingleAppBadge(String appName, boolean enable) {
        if (!isBadgeManagerFragmentShowing()) {
            return false;
        }
        this.mBadgeSettingsFragment.enableAppBadge(appName, enable);
        return true;
    }

    public boolean isSingleAppBadgeChecked(String className) {
        return isBadgeManagerFragmentShowing() && this.mBadgeSettingsFragment.isSingleAppBadgeChecked(className);
    }

    public boolean isAllAppsBadgeSwitchChecked() {
        return isBadgeManagerFragmentShowing() && this.mBadgeSettingsFragment.isAllAppsBadgeSwitchChecked();
    }

    public boolean isBadgeManagerFragmentShowing() {
        return this.mVisibleFragment == 1 && this.mBadgeSettingsFragment != null;
    }

    public boolean isSettingFragmentShowing() {
        return this.mLauncherSettingsFragment != null && this.mVisibleFragment == 0;
    }

    public void updatePreNotificationPanelSetting(boolean value) {
        this.mLauncherSettingsFragment.updatePreNotificationPanelSetting(value);
    }

    private void startSettingApp() {
        Log.d(TAG, "launch Setting App");
        Intent settingApp = new Intent("android.intent.action.MAIN");
        settingApp.setComponent(GlobalSettingUtils.getSettingCN());
        settingApp.setFlags(268566528);
        Bundle bundle = new Bundle();
        bundle.putBoolean("needSearchMenuInSub", true);
        settingApp.putExtras(bundle);
        try {
            startActivity(settingApp);
            overridePendingTransition(R.anim.global_settings_in, R.anim.settings_activity_out);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Unable to launch. intent=" + settingApp, e);
        }
        GlobalSettingUtils.resetSettingsValue();
        GlobalSettingUtils.setBackToSetting(true);
    }
}

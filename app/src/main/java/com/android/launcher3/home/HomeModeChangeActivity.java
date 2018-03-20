package com.android.launcher3.home;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.model.LauncherSettings.Favorites_HomeApps;
import com.android.launcher3.common.model.LauncherSettings.Favorites_HomeOnly;
import com.android.launcher3.common.model.LauncherSettings.Settings;
import com.android.launcher3.theme.OpenThemeManager;
import com.android.launcher3.util.GlobalSettingUtils;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;

public class HomeModeChangeActivity extends Activity implements OnClickListener, OnCheckedChangeListener {
    public static final String ACTION_CHANGE_HOMEONLYMODE = "com.android.launcher.action.CHANGE_HOMEONLYMODE";
    public static final String EXTRA_HOMEONLYEMODE = "homeOnlyeMode";
    public static final String PERMISSION_CHANGE_HOMEONLYMODE = "com.samsung.android.launcher.permission.CHANGE_HOMEONLYMODE";
    private static final String TAG = HomeModeChangeActivity.class.getSimpleName();
    private TextView mApplyButton;
    private RadioButton mAppsRadio;
    private boolean mEnabledHomeOnly;
    private TextView mHelpText;
    private RadioButton mHomeOnlyRadio;
    private boolean mNeedInit = false;
    private ImageView mPreview;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null && HomeModeChangeActivity.ACTION_CHANGE_HOMEONLYMODE.equals(intent.getAction())) {
                Log.i(HomeModeChangeActivity.TAG, HomeModeChangeActivity.ACTION_CHANGE_HOMEONLYMODE);
                HomeModeChangeActivity.this.preformOnClick(intent.getBooleanExtra(HomeModeChangeActivity.EXTRA_HOMEONLYEMODE, false));
                HomeModeChangeActivity.this.askConfirmation();
            }
        }
    };
    private Activity mSettingsActivity;

    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        if (Utilities.isOnlyPortraitMode()) {
            setRequestedOrientation(5);
        } else {
            setRequestedOrientation(-1);
        }
        setContentView(R.layout.home_screen_mode_view);
        this.mEnabledHomeOnly = LauncherAppState.getInstance().isHomeOnlyModeEnabled();
        initViews();
        checkNeedInit();
        initActionBar();
        updatePreviewAndHelpText(this.mEnabledHomeOnly);
        setTitle(R.string.home_screen_layout);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CHANGE_HOMEONLYMODE);
        registerReceiver(this.mReceiver, filter, "com.samsung.android.launcher.permission.CHANGE_HOMEONLYMODE", null);
        this.mSettingsActivity = LauncherAppState.getInstance().getSettingsActivity();
    }

    protected void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
        unregisterReceiver(this.mReceiver);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel_button:
                SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_HomeScreenLayout), getResources().getString(R.string.event_CancelHomeScreenLayout));
                finish();
                return;
            case R.id.save_button:
                askConfirmation();
                return;
            case R.id.mode_apps:
                preformOnClick(false);
                return;
            case R.id.mode_home_only:
                preformOnClick(true);
                return;
            default:
                return;
        }
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        this.mAppsRadio.setChecked(!isChecked);
        updatePreviewAndHelpText(isChecked);
    }

    private void initViews() {
        this.mPreview = (ImageView) findViewById(R.id.mode_preview_image);
        this.mAppsRadio = (RadioButton) findViewById(R.id.mode_apps_radio);
        this.mHomeOnlyRadio = (RadioButton) findViewById(R.id.mode_home_only_radio);
        this.mHelpText = (TextView) findViewById(R.id.mode_help_text);
        if (this.mEnabledHomeOnly) {
            this.mHomeOnlyRadio.setChecked(true);
        } else {
            this.mAppsRadio.setChecked(true);
        }
        findViewById(R.id.mode_apps).setOnClickListener(this);
        findViewById(R.id.mode_home_only).setOnClickListener(this);
        this.mHomeOnlyRadio.setOnCheckedChangeListener(this);
    }

    private void checkNeedInit() {
        boolean noSuchTable = false;
        Uri contentUri = LauncherAppState.getInstance().isHomeOnlyModeEnabled(false) ? Favorites_HomeApps.CONTENT_URI : Favorites_HomeOnly.CONTENT_URI;
        if (!FavoritesProvider.getInstance().tableExists(contentUri.getLastPathSegment())) {
            noSuchTable = true;
        }
        if (noSuchTable) {
            this.mNeedInit = true;
            return;
        }
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() <= 0) {
                    this.mNeedInit = true;
                }
                cursor.close();
            } catch (Throwable th) {
                cursor.close();
            }
        }
    }

    private void initActionBar() {
        ActionBar mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayShowCustomEnabled(true);
            mActionBar.setCustomView(R.layout.edit_bar_layout);
            mActionBar.getCustomView().findViewById(R.id.cancel_button).setOnClickListener(this);
            this.mApplyButton = (TextView) mActionBar.getCustomView().findViewById(R.id.save_button);
            TextView cancleButton = (TextView) mActionBar.getCustomView().findViewById(R.id.cancel_button);
            String buttonString = getResources().getString(R.string.accessibility_button);
            this.mApplyButton.setContentDescription(this.mApplyButton.getText() + ", " + buttonString);
            cancleButton.setContentDescription(cancleButton.getText() + ", " + buttonString);
            Utilities.setMaxFontScale(this, this.mApplyButton);
            Utilities.setMaxFontScale(this, cancleButton);
            this.mApplyButton.setOnClickListener(this);
            this.mApplyButton.setEnabled(false);
            if (OpenThemeManager.getInstance().isDefaultTheme()) {
                LinearLayout headerBar = (LinearLayout) findViewById(R.id.action_bar);
                headerBar.setBackground(getResources().getDrawable(R.drawable.edit_app_bar_bg, null));
                if (Utilities.isEnableBtnBg(this)) {
                    for (int index = 0; index < headerBar.getChildCount(); index++) {
                        headerBar.getChildAt(index).setBackgroundResource(R.drawable.tw_text_action_btn_material_light);
                    }
                }
            }
        }
    }

    private void updatePreviewAndHelpText(boolean isHomeOnly) {
        if (isHomeOnly) {
            this.mApplyButton.setEnabled(!this.mEnabledHomeOnly);
            this.mPreview.setImageDrawable(getResources().getDrawable(R.drawable.homesettings_img_preview_home_only, null));
            this.mHelpText.setText(R.string.home_screen_mode_home_only_body_text);
            return;
        }
        this.mApplyButton.setEnabled(this.mEnabledHomeOnly);
        this.mPreview.setImageDrawable(getResources().getDrawable(R.drawable.homesettings_img_preview_home_apps, null));
        this.mHelpText.setText(R.string.home_screen_mode_home_apps_body_text);
    }

    private void askConfirmation() {
        if (this.mNeedInit) {
            if (this.mAppsRadio.isChecked() && LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                showModeChangeDialog(R.string.home_screen_mode_change_alert_text, false);
            } else if (this.mHomeOnlyRadio.isChecked() && !LauncherAppState.getInstance().isHomeOnlyModeEnabled()) {
                showModeChangeDialog(R.string.home_screen_mode_change_with_copy_layout, true);
            }
        } else if ((this.mAppsRadio.isChecked() && LauncherAppState.getInstance().isHomeOnlyModeEnabled()) || (this.mHomeOnlyRadio.isChecked() && !LauncherAppState.getInstance().isHomeOnlyModeEnabled())) {
            showModeChangeDialog(R.string.home_screen_mode_change_alert_text, false);
        }
    }

    private void preformOnClick(boolean homeOnlyMode) {
        String string;
        this.mHomeOnlyRadio.setChecked(homeOnlyMode);
        SALogging instance = SALogging.getInstance();
        String string2 = getResources().getString(R.string.screen_HomeScreenLayout);
        if (homeOnlyMode) {
            string = getResources().getString(R.string.event_HomeScreenLayout_HomeOnly);
        } else {
            string = getResources().getString(R.string.event_HomeScreenLayout_HomeAndApps);
        }
        instance.insertEventLog(string2, string);
    }

    private void showModeChangeDialog(int msgId, final boolean needNeutralButton) {
        Builder alertDialogBuilder = new Builder(this);
        alertDialogBuilder.setTitle(R.string.home_screen_mode_change_dialog_title);
        alertDialogBuilder.setMessage(msgId).setCancelable(true).setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                if (needNeutralButton) {
                    FavoritesProvider.getInstance().copyFavoritesForHomeOnly();
                }
                HomeModeChangeActivity.this.setHomeScreenMode();
            }
        });
        if (needNeutralButton) {
            alertDialogBuilder.setNeutralButton(17039360, null);
            alertDialogBuilder.setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface arg0, int arg1) {
                    HomeModeChangeActivity.this.setHomeScreenMode();
                }
            });
        } else {
            alertDialogBuilder.setNegativeButton(17039360, null);
        }
        AlertDialog alert = alertDialogBuilder.create();
        alert.setOnDismissListener(new OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(20);
            }
        });
        LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(this.mEnabledHomeOnly ? 23 : 15);
        alert.show();
    }

    private void setHomeScreenMode() {
        if (LauncherFeature.supportHomeModeChange()) {
            boolean isHomeOnlyMode = false;
            if (this.mHomeOnlyRadio.isChecked()) {
                isHomeOnlyMode = true;
            }
            Bundle extras = new Bundle();
            extras.putBoolean("value", isHomeOnlyMode);
            getContentResolver().call(Settings.CONTENT_URI, Settings.METHOD_SET_BOOLEAN, Utilities.HOMESCREEN_MODE_PREFERENCE_KEY, extras);
            SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_HomeScreenLayout), getResources().getString(R.string.event_ApplylHomeScreenLayout), isHomeOnlyMode ? "2" : "1");
            SALogging.getInstance().insertStatusLog(getResources().getString(R.string.status_HomeScreenLayout), isHomeOnlyMode ? "2" : "1");
            final Activity settingsActivity = LauncherAppState.getInstance().getSettingsActivity();
            new Handler().post(new Runnable() {
                public void run() {
                    if (settingsActivity != null) {
                        settingsActivity.finish();
                    } else if (HomeModeChangeActivity.this.mSettingsActivity != null) {
                        HomeModeChangeActivity.this.mSettingsActivity.finish();
                    }
                }
            });
            GlobalSettingUtils.resetSettingsValue();
            finish();
        }
    }
}

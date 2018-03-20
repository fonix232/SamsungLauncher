package com.android.launcher3.home;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.TextView;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherSettings.Settings;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.common.view.LiveIconManager;
import com.android.launcher3.theme.OpenThemeManager;
import com.android.launcher3.theme.OpenThemeManager.FolderStyle;
import com.android.launcher3.theme.OpenThemeManager.ThemeItems;
import com.android.launcher3.util.BitmapUtils;
import com.android.launcher3.util.DualAppUtils;
import com.android.launcher3.util.ShortcutTray;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Iterator;

public class AppsButtonSettingsActivity extends Activity implements OnClickListener, OnCheckedChangeListener {
    public static final String ACTION_CHANGE_APPS_BUTTON_BIXBY = "showApps";
    public static final String EXTRA_SHOW_APPS = "showApps";
    private static final String GRID_INFO_SPLIT = "\\|";
    private static final String TAG = AppsButtonSettingsActivity.class.getSimpleName();
    private TextView mApplyButton;
    private ImageView mAppsIcon;
    private boolean mEnabledAppsButton;
    private TextView mHelpText;
    private RadioButton mHideAppsRadio;
    private ImageView mLastIcon;
    private LinearLayout mPreview;
    private RadioButton mShowAppsRadio;

    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        if (Utilities.isOnlyPortraitMode()) {
            setRequestedOrientation(5);
        } else {
            setRequestedOrientation(-1);
        }
        this.mEnabledAppsButton = LauncherAppState.getInstance().getAppsButtonEnabled();
        if (!(getIntent() == null || getIntent().getAction() == null || !getIntent().getAction().equals("showApps"))) {
            changeAppsButtonEnabled(getIntent().getBooleanExtra("showApps", false));
            finish();
        }
        setContentView(R.layout.apps_button_setting_activity_layout);
        initViews();
        initActionBar();
        addIconsToPreview(getHotseatIconFromDb());
        LauncherAppState.getInstance().setAppsButtonSettingsActivity(this);
    }

    public void onClick(View v) {
        boolean z = true;
        switch (v.getId()) {
            case R.id.show_apps:
                preformOnClick(true);
                return;
            case R.id.hide_apps:
                preformOnClick(false);
                return;
            case R.id.cancel_button:
                SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_AppsButton), getResources().getString(R.string.event_CancelAppsbutton));
                finish();
                return;
            case R.id.save_button:
                if (this.mEnabledAppsButton) {
                    z = false;
                }
                changeAppsButtonEnabled(z);
                finish();
                return;
            default:
                return;
        }
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        this.mHideAppsRadio.setChecked(!isChecked);
        updatePreview(isChecked);
    }

    private void preformOnClick(boolean showApps) {
        String string;
        this.mShowAppsRadio.setChecked(showApps);
        SALogging instance = SALogging.getInstance();
        String string2 = getResources().getString(R.string.screen_AppsButton);
        if (showApps) {
            string = getResources().getString(R.string.event_ShowAppsButton);
        } else {
            string = getResources().getString(R.string.event_HideAppsButton);
        }
        instance.insertEventLog(string2, string);
    }

    private void initViews() {
        this.mPreview = (LinearLayout) findViewById(R.id.icons_layout);
        this.mHelpText = (TextView) findViewById(R.id.help_instruction);
        this.mShowAppsRadio = (RadioButton) findViewById(R.id.show_apps_radio);
        this.mHideAppsRadio = (RadioButton) findViewById(R.id.hide_apps_radio);
        if (this.mEnabledAppsButton) {
            this.mShowAppsRadio.setChecked(true);
        } else {
            this.mHideAppsRadio.setChecked(true);
        }
        findViewById(R.id.show_apps).setOnClickListener(this);
        findViewById(R.id.hide_apps).setOnClickListener(this);
        this.mShowAppsRadio.setOnCheckedChangeListener(this);
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
                headerBar.setBackground(getResources().getDrawable(R.drawable.edit_app_bar_bg));
                if (Utilities.isEnableBtnBg(this)) {
                    for (int index = 0; index < headerBar.getChildCount(); index++) {
                        headerBar.getChildAt(index).setBackgroundResource(R.drawable.tw_text_action_btn_material_light);
                    }
                }
            }
        }
    }

    private void updatePreview(boolean isShowAppsChecked) {
        if (isShowAppsChecked) {
            this.mApplyButton.setEnabled(!this.mEnabledAppsButton);
            if (this.mLastIcon != null) {
                this.mPreview.removeView(this.mLastIcon);
            }
            this.mPreview.addView(this.mAppsIcon);
            return;
        }
        this.mApplyButton.setEnabled(this.mEnabledAppsButton);
        this.mPreview.removeView(this.mAppsIcon);
        if (this.mLastIcon != null) {
            this.mPreview.addView(this.mLastIcon);
        }
    }

    private void addIconsToPreview(ArrayList<Drawable> iconDrawables) {
        int previewIconSize = getResources().getDimensionPixelSize(R.dimen.apps_button_setting_iconSize);
        if (iconDrawables != null && this.mPreview != null) {
            Iterator it = iconDrawables.iterator();
            while (it.hasNext()) {
                Drawable d = (Drawable) it.next();
                ImageView icon = new ImageView(this);
                LayoutParams params = new LayoutParams(-2, -2);
                params.weight = 1.0f;
                params.height = previewIconSize;
                params.width = previewIconSize;
                icon.setImageDrawable(d);
                this.mPreview.addView(icon, params);
            }
            getAllAppsIcon();
            if (this.mEnabledAppsButton) {
                this.mPreview.addView(this.mAppsIcon);
                return;
            }
            int maxCount = LauncherAppState.getInstance().getDeviceProfile().getMaxHotseatCount();
            if (iconDrawables.size() == maxCount) {
                this.mLastIcon = (ImageView) this.mPreview.getChildAt(maxCount - 1);
                this.mHelpText.setVisibility(View.VISIBLE);
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private java.util.ArrayList<android.graphics.drawable.Drawable> getHotseatIconFromDb() {
        /*
        r21 = this;
        r14 = new java.util.ArrayList;
        r14.<init>();
        r2 = 5;
        r4 = new java.lang.String[r2];
        r2 = 0;
        r3 = "_id";
        r4[r2] = r3;
        r2 = 1;
        r3 = "intent";
        r4[r2] = r3;
        r2 = 2;
        r3 = "icon";
        r4[r2] = r3;
        r2 = 3;
        r3 = "itemType";
        r4[r2] = r3;
        r2 = 4;
        r3 = "color";
        r4[r2] = r3;
        r5 = "container=-101";
        r7 = "screen";
        r2 = r21.getContentResolver();
        r3 = com.android.launcher3.common.model.LauncherSettings.Favorites.CONTENT_URI;
        r6 = 0;
        r10 = r2.query(r3, r4, r5, r6, r7);
        if (r10 == 0) goto L_0x00c7;
    L_0x0032:
        r2 = com.android.launcher3.LauncherAppState.getInstance();	 Catch:{ Exception -> 0x00a7 }
        r13 = r2.getIconCache();	 Catch:{ Exception -> 0x00a7 }
        r19 = com.android.launcher3.common.compat.UserHandleCompat.myUserHandle();	 Catch:{ Exception -> 0x00a7 }
        r2 = r21.getResources();	 Catch:{ Exception -> 0x00a7 }
        r3 = 2131362090; // 0x7f0a012a float:1.834395E38 double:1.0530327875E-314;
        r20 = r2.getDimensionPixelSize(r3);	 Catch:{ Exception -> 0x00a7 }
        r2 = "intent";
        r17 = r10.getColumnIndexOrThrow(r2);	 Catch:{ Exception -> 0x00a7 }
        r2 = "_id";
        r16 = r10.getColumnIndexOrThrow(r2);	 Catch:{ Exception -> 0x00a7 }
    L_0x0055:
        r2 = r10.moveToNext();	 Catch:{ Exception -> 0x00a7 }
        if (r2 == 0) goto L_0x009a;
    L_0x005b:
        r0 = r17;
        r18 = r10.getString(r0);	 Catch:{ Exception -> 0x0099 }
        r0 = r16;
        r2 = r10.getLong(r0);	 Catch:{ Exception -> 0x0099 }
        r15 = java.lang.Long.valueOf(r2);	 Catch:{ Exception -> 0x0099 }
        if (r18 != 0) goto L_0x009e;
    L_0x006d:
        r2 = "color";
        r9 = r10.getColumnIndexOrThrow(r2);	 Catch:{ Exception -> 0x0099 }
        r2 = r15.longValue();	 Catch:{ Exception -> 0x0099 }
        r6 = r10.getInt(r9);	 Catch:{ Exception -> 0x0099 }
        r0 = r21;
        r8 = r0.getHotseatFolderIconFromDb(r2, r6);	 Catch:{ Exception -> 0x0099 }
    L_0x0081:
        if (r8 == 0) goto L_0x0055;
    L_0x0083:
        r11 = new android.graphics.drawable.BitmapDrawable;	 Catch:{ Exception -> 0x0099 }
        r2 = r21.getResources();	 Catch:{ Exception -> 0x0099 }
        r3 = 1;
        r0 = r20;
        r1 = r20;
        r3 = android.graphics.Bitmap.createScaledBitmap(r8, r0, r1, r3);	 Catch:{ Exception -> 0x0099 }
        r11.<init>(r2, r3);	 Catch:{ Exception -> 0x0099 }
        r14.add(r11);	 Catch:{ Exception -> 0x0099 }
        goto L_0x0055;
    L_0x0099:
        r12 = move-exception;
    L_0x009a:
        r10.close();
    L_0x009d:
        return r14;
    L_0x009e:
        r0 = r21;
        r1 = r19;
        r8 = r0.getIconBitmap(r10, r13, r1);	 Catch:{ Exception -> 0x0099 }
        goto L_0x0081;
    L_0x00a7:
        r12 = move-exception;
        r2 = TAG;	 Catch:{ all -> 0x00c9 }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00c9 }
        r3.<init>();	 Catch:{ all -> 0x00c9 }
        r6 = "Exception : ";
        r3 = r3.append(r6);	 Catch:{ all -> 0x00c9 }
        r6 = r12.toString();	 Catch:{ all -> 0x00c9 }
        r3 = r3.append(r6);	 Catch:{ all -> 0x00c9 }
        r3 = r3.toString();	 Catch:{ all -> 0x00c9 }
        android.util.Log.e(r2, r3);	 Catch:{ all -> 0x00c9 }
        r10.close();
    L_0x00c7:
        r14 = 0;
        goto L_0x009d;
    L_0x00c9:
        r2 = move-exception;
        r10.close();
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.AppsButtonSettingsActivity.getHotseatIconFromDb():java.util.ArrayList<android.graphics.drawable.Drawable>");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private android.graphics.Bitmap getHotseatFolderIconFromDb(long r18, int r20) {
        /*
        r17 = this;
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "container=";
        r2 = r2.append(r3);
        r0 = r18;
        r2 = r2.append(r0);
        r3 = " and ";
        r2 = r2.append(r3);
        r3 = "rank";
        r2 = r2.append(r3);
        r3 = "< 9";
        r2 = r2.append(r3);
        r5 = r2.toString();
        r7 = "rank";
        r2 = 3;
        r4 = new java.lang.String[r2];
        r2 = 0;
        r3 = "intent";
        r4[r2] = r3;
        r2 = 1;
        r3 = "icon";
        r4[r2] = r3;
        r2 = 2;
        r3 = "itemType";
        r4[r2] = r3;
        r2 = r17.getContentResolver();
        r3 = com.android.launcher3.common.model.LauncherSettings.Favorites.CONTENT_URI;
        r6 = 0;
        r9 = r2.query(r3, r4, r5, r6, r7);
        r12 = new java.util.ArrayList;
        r12.<init>();
        r2 = com.android.launcher3.LauncherAppState.getInstance();
        r2 = r2.getDeviceProfile();
        r2 = r2.hotseatGridIcon;
        r14 = r2.getIconSize();
        if (r9 == 0) goto L_0x0089;
    L_0x005b:
        r2 = com.android.launcher3.LauncherAppState.getInstance();
        r13 = r2.getIconCache();
        r15 = com.android.launcher3.common.compat.UserHandleCompat.myUserHandle();
    L_0x0067:
        r2 = r9.moveToNext();	 Catch:{ Exception -> 0x0092 }
        if (r2 == 0) goto L_0x0086;
    L_0x006d:
        r0 = r17;
        r8 = r0.getIconBitmap(r9, r13, r15);	 Catch:{ Exception -> 0x0085 }
        r10 = new android.graphics.drawable.BitmapDrawable;	 Catch:{ Exception -> 0x0085 }
        r2 = r17.getResources();	 Catch:{ Exception -> 0x0085 }
        r3 = 1;
        r3 = android.graphics.Bitmap.createScaledBitmap(r8, r14, r14, r3);	 Catch:{ Exception -> 0x0085 }
        r10.<init>(r2, r3);	 Catch:{ Exception -> 0x0085 }
        r12.add(r10);	 Catch:{ Exception -> 0x0085 }
        goto L_0x0067;
    L_0x0085:
        r11 = move-exception;
    L_0x0086:
        r9.close();
    L_0x0089:
        r0 = r17;
        r1 = r20;
        r2 = r0.drawFolderPreview(r12, r1);
        return r2;
    L_0x0092:
        r11 = move-exception;
        r2 = TAG;	 Catch:{ all -> 0x00b3 }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00b3 }
        r3.<init>();	 Catch:{ all -> 0x00b3 }
        r6 = "Exception : ";
        r3 = r3.append(r6);	 Catch:{ all -> 0x00b3 }
        r6 = r11.toString();	 Catch:{ all -> 0x00b3 }
        r3 = r3.append(r6);	 Catch:{ all -> 0x00b3 }
        r3 = r3.toString();	 Catch:{ all -> 0x00b3 }
        android.util.Log.e(r2, r3);	 Catch:{ all -> 0x00b3 }
        r9.close();
        goto L_0x0089;
    L_0x00b3:
        r2 = move-exception;
        r9.close();
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.AppsButtonSettingsActivity.getHotseatFolderIconFromDb(long, int):android.graphics.Bitmap");
    }

    private Bitmap getIconBitmap(Cursor cursor, IconCache iconCache, UserHandleCompat myUserHandle) {
        Bitmap bitmap = null;
        try {
            int intentIndex = cursor.getColumnIndexOrThrow("intent");
            int iconIndex = cursor.getColumnIndexOrThrow("icon");
            int type = cursor.getInt(cursor.getColumnIndexOrThrow("itemType"));
            Intent intent = Intent.parseUri(cursor.getString(intentIndex), 0);
            if (type != 1) {
                bitmap = iconCache.getIcon(intent, myUserHandle);
            } else if (intent.getAction() != null && intent.getAction().equals(Utilities.ACTION_SHOW_APPS_VIEW)) {
                return null;
            } else {
                bitmap = BitmapUtils.createIconBitmap(cursor, iconIndex, (Context) this);
                if (bitmap == null) {
                    bitmap = iconCache.getDefaultIcon(myUserHandle);
                } else {
                    bitmap = ShortcutTray.getIcon(this, bitmap, intent.getComponent());
                    if (DualAppUtils.supportDualApp(this) && DualAppUtils.isDualAppId(myUserHandle)) {
                        bitmap = DualAppUtils.makeUserBadgedIcon(this, bitmap, bitmap.getWidth(), myUserHandle.getUser());
                    }
                    bitmap = OpenThemeManager.getInstance().getIconWithTrayIfNeeded(bitmap, bitmap.getWidth(), false);
                }
            }
            if (intent.getComponent() != null) {
                String packageName = intent.getComponent().getPackageName() != null ? intent.getComponent().getPackageName() : null;
                if (LiveIconManager.isLiveIconPackage(packageName)) {
                    bitmap = LiveIconManager.getLiveIcon(this, packageName, myUserHandle);
                }
                if (LiveIconManager.isKnoxLiveIcon(intent)) {
                    bitmap = LiveIconManager.getLiveIcon(this, intent.getStringExtra(IconView.EXTRA_SHORTCUT_LIVE_ICON_COMPONENT).split("/")[0], myUserHandle);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception : " + e.toString());
        }
        return bitmap;
    }

    private int getIconSize() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        String[] gridInfo = getResources().getStringArray(R.array.hotseat_grid_icon_info);
        int maxCount = LauncherAppState.getInstance().getDeviceProfile().getMaxHotseatCount();
        if (gridInfo != null) {
            for (String info : gridInfo) {
                String[] infoSplit = info.split(GRID_INFO_SPLIT);
                if (infoSplit != null && Integer.parseInt(infoSplit[0]) == maxCount) {
                    return Utilities.pxFromDp(Float.parseFloat(infoSplit[1]), metrics);
                }
            }
        }
        return LauncherAppState.getInstance().getDeviceProfile().hotseatGridIcon.getIconSize();
    }

    private Bitmap drawFolderPreview(ArrayList drawables, int folderColor) {
        Bitmap folderIcon;
        int iconSize = getIconSize();
        FolderStyle fs = OpenThemeManager.getInstance().getFolderStyle();
        if (fs != null) {
            folderIcon = Bitmap.createScaledBitmap(fs.getCloseFolderImage(folderColor), iconSize, iconSize, false);
        } else {
            folderIcon = BitmapUtils.getBitmap(getDrawable(R.mipmap.homescreen_ic_folder_default), iconSize, iconSize);
        }
        Canvas canvas = new Canvas(folderIcon);
        int miniIconGap = getResources().getDimensionPixelSize(R.dimen.folder_mini_icon_3x3_gap);
        int previewPadding = getResources().getDimensionPixelSize(R.dimen.folder_preview_padding);
        int baselineIconSize = ((iconSize - (previewPadding * 2)) - (miniIconGap * 2)) / 3;
        float baselineIconScale = ((float) baselineIconSize) / ((float) iconSize);
        Rect mOldBounds = new Rect();
        int i = drawables.size() - 1;
        while (i >= 0) {
            Drawable d = (Drawable) drawables.get(i);
            int posX = Utilities.sIsRtl ? 2 - (i % 3) : i % 3;
            int posY = i / 3;
            canvas.save();
            mOldBounds.set(d.getBounds());
            int l = ((baselineIconSize + miniIconGap) * posX) + previewPadding;
            int t = ((baselineIconSize + miniIconGap) * posY) + previewPadding;
            d.setBounds(l, t, ((int) (((float) iconSize) * baselineIconScale)) + l, ((int) (((float) iconSize) * baselineIconScale)) + t);
            d.draw(canvas);
            d.clearColorFilter();
            canvas.restore();
            i--;
        }
        return folderIcon;
    }

    private void getAllAppsIcon() {
        this.mAppsIcon = new ImageView(this);
        int size = getResources().getDimensionPixelSize(R.dimen.apps_button_setting_iconSize);
        LayoutParams params = new LayoutParams(-2, -2);
        params.weight = 1.0f;
        params.height = size;
        params.width = size;
        this.mAppsIcon.setLayoutParams(params);
        OpenThemeManager themeManager = OpenThemeManager.getInstance();
        this.mAppsIcon.setImageBitmap(OpenThemeManager.getInstance().getIconWithTrayIfNeeded(BitmapUtils.getBitmap(themeManager.getDrawable(ThemeItems.ALL_APPS_ICON.value()), size, size), size, themeManager.isFromThemeResources(ThemeItems.ALL_APPS_ICON.value(), "drawable")));
    }

    public void changeAppsButtonEnabled(boolean enable) {
        if (this.mEnabledAppsButton != enable) {
            Log.i(TAG, "setAppsButtonEnabled : " + enable);
            Bundle extras = new Bundle();
            extras.putBoolean("value", enable);
            getContentResolver().call(Settings.CONTENT_URI, Settings.METHOD_SET_BOOLEAN, Utilities.APPS_BUTTON_SETTING_PREFERENCE_KEY, extras);
            SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_AppsButton), getResources().getString(R.string.event_ApplyAppsbutton), enable ? "1" : "2");
            SALogging.getInstance().insertStatusLog(getResources().getString(R.string.status_AppsButton), enable ? 1 : 0);
        }
    }
}

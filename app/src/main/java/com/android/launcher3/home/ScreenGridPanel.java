package com.android.launcher3.home;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.controller.ControllerBase;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.model.DefaultLayoutParser;
import com.android.launcher3.util.GlobalSettingUtils;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;

public class ScreenGridPanel extends LinearLayout {
    private static final String TAG = "ScreenGridPanel";
    private static String mSpanDescriptionFormat;
    private TextView mApplyView;
    private int mAppsCellX;
    private int mAppsCellY;
    private String[] mAppsGridButtonMap;
    private TextView mCancelView;
    private OnClickListener mCancleButtonClickListener;
    private LinearLayout mGridBtnLayout;
    private OnClickListener mGridButtonClickListener;
    private HomeController mHomeController;
    private String[] mHomeGridInfoMap;
    private Launcher mLauncher;
    private OnClickListener mSaveButtonClickListener;
    private String[] mScreenGridButtonMap;
    private TextView mScreenGridExplainView;
    private View mScreenGridTopContainer;
    private int[] mSelectedGrid;

    public ScreenGridPanel(Context context) {
        this(context, null);
    }

    public ScreenGridPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScreenGridPanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mScreenGridButtonMap = null;
        this.mHomeGridInfoMap = null;
        this.mAppsGridButtonMap = null;
        this.mScreenGridExplainView = null;
        this.mScreenGridTopContainer = null;
        this.mSelectedGrid = null;
        this.mGridButtonClickListener = new OnClickListener() {
            public void onClick(View view) {
                String buttonName = (String) view.getTag();
                int[] cellXY = new int[2];
                int gridX = Character.getNumericValue(buttonName.charAt(0));
                int gridY = Character.getNumericValue(buttonName.charAt(2));
                int gridIndex = ScreenGridPanel.this.getGridInfoIndex(gridX, gridY);
                int appsGridX = Character.getNumericValue(ScreenGridPanel.this.mAppsGridButtonMap[gridIndex].charAt(0));
                int appsGridY = Character.getNumericValue(ScreenGridPanel.this.mAppsGridButtonMap[gridIndex].charAt(2));
                String gridExplainString = ScreenGridPanel.this.getResources().getString(R.string.screen_grid_explain_text, new Object[]{Integer.valueOf(appsGridX), Integer.valueOf(appsGridY)});
                Utilities.loadCurrentGridSize(ScreenGridPanel.this.mLauncher, cellXY);
                if (!ScreenGridPanel.this.mHomeController.isChangeGridState()) {
                    if (cellXY[0] == gridX && cellXY[1] == gridY) {
                        ScreenGridPanel.this.mApplyView.setEnabled(false);
                        ScreenGridPanel.this.mApplyView.setAlpha(0.4f);
                        ScreenGridPanel.this.mScreenGridExplainView.setVisibility(View.GONE);
                    } else {
                        ScreenGridPanel.this.mApplyView.setEnabled(true);
                        ScreenGridPanel.this.mApplyView.setAlpha(1.0f);
                        if (appsGridX == ScreenGridPanel.this.mAppsCellX && appsGridY == ScreenGridPanel.this.mAppsCellY) {
                            ScreenGridPanel.this.mScreenGridExplainView.setVisibility(View.GONE);
                        } else {
                            ScreenGridPanel.this.mScreenGridExplainView.setText(gridExplainString);
                        }
                    }
                    ScreenGridPanel.this.updateBtnForScreenGrid(buttonName);
                    view.announceForAccessibility(view.getContentDescription());
                    ScreenGridPanel.this.mHomeController.changeGrid(gridX, gridY, true);
                    SALogging.getInstance().insertClickGridButtonLog(gridX, gridY, true);
                }
            }
        };
        this.mSaveButtonClickListener = new OnClickListener() {
            public void onClick(View view) {
                Log.d(ScreenGridPanel.TAG, "ScreenGrid save button clicked.");
                if (GlobalSettingUtils.getStartSetting()) {
                    GlobalSettingUtils.resetSettingsValue();
                    ScreenGridPanel.this.mLauncher.finishSettingsActivity();
                }
                if (ScreenGridPanel.this.mHomeController != null) {
                    ScreenGridPanel.this.mHomeController.applyGridChangeFinally();
                    ScreenGridPanel.this.mHomeController.finishAllStage();
                    ScreenGridPanel.this.mHomeController.enterNormalState(true);
                }
                DeviceProfile grid = ScreenGridPanel.this.mLauncher.getDeviceProfile();
                SALogging.getInstance().insertChangeGridLog(grid.homeGrid.getCellCountX(), grid.homeGrid.getCellCountY(), false, true);
                ScreenGridPanel.this.mAppsCellX = ScreenGridPanel.this.mLauncher.getDeviceProfile().appsGrid.getCellCountX();
                ScreenGridPanel.this.mAppsCellY = ScreenGridPanel.this.mLauncher.getDeviceProfile().appsGrid.getCellCountY();
                ScreenGridPanel.this.mScreenGridExplainView.setVisibility(View.GONE);
                ScreenGridPanel.this.mApplyView.performAccessibilityAction(128, null);
            }
        };
        this.mCancleButtonClickListener = new OnClickListener() {
            public void onClick(View v) {
                Log.d(ScreenGridPanel.TAG, "ScreenGrid cancel button clicked.");
                ScreenGridPanel.this.mLauncher.startHomeSettingActivity();
                ScreenGridPanel.this.mHomeController.exitScreenGridStateDelayed();
                SALogging.getInstance().insertEventLog(ScreenGridPanel.this.mLauncher.getResources().getString(R.string.screen_HomeScreenGrid), ScreenGridPanel.this.mLauncher.getResources().getString(R.string.event_SG_Cancel));
            }
        };
        this.mLauncher = (Launcher) context;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources res = getResources();
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        String currentGrid = grid.homeGrid.getCellCountX() + DefaultLayoutParser.ATTR_X + grid.homeGrid.getCellCountY();
        mSpanDescriptionFormat = res.getString(R.string.talkback_widget_dims_format);
        boolean isEnableBtnBg = Utilities.isEnableBtnBg(getContext());
        int padding = res.getDimensionPixelSize(R.dimen.overview_panel_bg_padding);
        int margin = res.getDimensionPixelSize(R.dimen.screen_grid_panel_margin);
        if (this.mScreenGridButtonMap == null) {
            this.mScreenGridButtonMap = res.getStringArray(R.array.support_grid_size);
        }
        if (this.mHomeGridInfoMap == null) {
            this.mHomeGridInfoMap = res.getStringArray(R.array.home_grid_info);
        }
        if (this.mAppsGridButtonMap == null) {
            this.mAppsGridButtonMap = res.getStringArray(R.array.apps_grid_info);
        }
        if (this.mSelectedGrid == null) {
            this.mSelectedGrid = new int[2];
        }
        if (this.mScreenGridButtonMap != null && this.mScreenGridButtonMap.length > 0) {
            this.mGridBtnLayout = (LinearLayout) findViewById(R.id.screen_grid_btn_layout);
            int screenGridButtonCount = this.mScreenGridButtonMap.length;
            int screenGridButtonWidth = res.getDimensionPixelSize(R.dimen.screen_grid_panel_width);
            if (this.mScreenGridButtonMap.length == 4) {
                if (VERSION.SEM_INT >= 2501) {
                    screenGridButtonWidth = res.getDimensionPixelSize(R.dimen.screen_grid_panel_width_4);
                } else {
                    screenGridButtonCount--;
                }
            }
            for (int i = 0; i < screenGridButtonCount; i++) {
                String buttonName = this.mScreenGridButtonMap[i];
                TextView screenGridButton = new TextView(this.mLauncher);
                int resId = getButtonResId(buttonName);
                screenGridButton.setTag(buttonName);
                screenGridButton.setCompoundDrawablesWithIntrinsicBounds(0, resId, 0, 0);
                screenGridButton.setTextAppearance(R.style.ScreenGridButton);
                screenGridButton.setOnClickListener(this.mGridButtonClickListener);
                screenGridButton.setOnKeyListener(HomeFocusHelper.SCREENGRID_PANEL_OPTION_BUTTON_KEY_LISTENER);
                screenGridButton.setBackgroundResource(R.drawable.focusable_view_bg);
                if (isEnableBtnBg) {
                    screenGridButton.setBackgroundResource(R.drawable.panel_btn_bg);
                    screenGridButton.setPadding(padding, padding, padding, padding);
                }
                String currentLanguage = Utilities.getLocale(getContext()).getLanguage();
                if ("ar".equals(currentLanguage) || "fa".equals(currentLanguage)) {
                    screenGridButton.setText(Utilities.toArabicDigits(String.valueOf(buttonName.charAt(2)), currentLanguage) + DefaultLayoutParser.ATTR_X + Utilities.toArabicDigits(String.valueOf(buttonName.charAt(0)), currentLanguage));
                } else {
                    screenGridButton.setText(buttonName);
                }
                screenGridButton.setWidth(screenGridButtonWidth);
                screenGridButton.setHeight(res.getDimensionPixelSize(R.dimen.screen_grid_panel_height));
                screenGridButton.setGravity(49);
                screenGridButton.setSelected(currentGrid.equals(buttonName));
                screenGridButton.setFocusable(true);
                LayoutParams llp = new LayoutParams(-2, -2);
                llp.setMargins(margin, 0, margin, 0);
                screenGridButton.setLayoutParams(llp);
                this.mGridBtnLayout.addView(screenGridButton);
            }
        }
    }

    private int getGridInfoIndex(int gridX, int gridY) {
        int index = 0;
        String[] strArr = this.mHomeGridInfoMap;
        int length = strArr.length;
        int i = 0;
        while (i < length) {
            String map = strArr[i];
            int x = Character.getNumericValue(map.charAt(0));
            int y = Character.getNumericValue(map.charAt(2));
            if (x != gridX || y != gridY) {
                index++;
                i++;
            } else if (index >= this.mAppsGridButtonMap.length) {
                return 0;
            } else {
                return index;
            }
        }
        return 0;
    }

    void bindController(ControllerBase controller) {
        this.mHomeController = (HomeController) controller;
    }

    public void setScreenGridProxy(String gridOption) {
        int child = 0;
        int cellX = gridOption.charAt(0);
        int cellY = gridOption.charAt(gridOption.length() - 1);
        String[] strArr = this.mHomeGridInfoMap;
        int length = strArr.length;
        int i = 0;
        while (i < length) {
            String map = strArr[i];
            int homeX = map.charAt(0);
            int homeY = map.charAt(2);
            if (cellX == homeX && cellY == homeY) {
                this.mGridButtonClickListener.onClick(this.mGridBtnLayout.getChildAt(child));
                return;
            } else {
                child++;
                i++;
            }
        }
    }

    public boolean checkValidGridOption(String gridOption) {
        int cellX = gridOption.charAt(0);
        int cellY = gridOption.charAt(gridOption.length() - 1);
        for (String map : this.mHomeGridInfoMap) {
            int homeX = map.charAt(0);
            int homeY = map.charAt(2);
            if (cellX == homeX && cellY == homeY) {
                return true;
            }
        }
        return false;
    }

    public boolean checkMatchGridOption(String gridOption) {
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        String currentGrid = grid.homeGrid.getCellCountX() + DefaultLayoutParser.ATTR_X + grid.homeGrid.getCellCountY();
        if (currentGrid == null || gridOption == null || currentGrid.compareToIgnoreCase(gridOption) != 0) {
            return false;
        }
        return true;
    }

    void initScreenGridTopContainer() {
        if (this.mScreenGridTopContainer == null) {
            this.mScreenGridTopContainer = this.mLauncher.findViewById(R.id.screen_grid_top_container);
            if (this.mScreenGridExplainView == null) {
                this.mAppsCellX = this.mLauncher.getDeviceProfile().appsGrid.getCellCountX();
                this.mAppsCellY = this.mLauncher.getDeviceProfile().appsGrid.getCellCountY();
                this.mScreenGridExplainView = (TextView) this.mLauncher.findViewById(R.id.screen_grid_explain_text);
                this.mScreenGridExplainView.setText(getResources().getString(R.string.screen_grid_explain_text, new Object[]{Integer.valueOf(this.mAppsCellX), Integer.valueOf(this.mAppsCellY)}));
                if (WhiteBgManager.isWhiteBg()) {
                    WhiteBgManager.changeTextColorForBg(this.mLauncher, this.mScreenGridExplainView, true);
                }
            }
            this.mApplyView = (TextView) this.mScreenGridTopContainer.findViewById(R.id.screen_grid_apply_button);
            this.mCancelView = (TextView) this.mScreenGridTopContainer.findViewById(R.id.screen_grid_cancel_button);
            String buttonString = getResources().getString(R.string.accessibility_button);
            this.mApplyView.setContentDescription(this.mApplyView.getText() + ", " + buttonString);
            this.mCancelView.setContentDescription(this.mCancelView.getText() + ", " + buttonString);
            this.mApplyView.setOnClickListener(this.mSaveButtonClickListener);
            this.mCancelView.setOnClickListener(this.mCancleButtonClickListener);
            this.mApplyView.setOnKeyListener(HomeFocusHelper.SCREENGRID_PANEL_TOP_BUTTON_KEY_LISTENER);
            this.mCancelView.setOnKeyListener(HomeFocusHelper.SCREENGRID_PANEL_TOP_BUTTON_KEY_LISTENER);
            this.mApplyView.setEnabled(false);
            this.mApplyView.setAlpha(0.4f);
            if (Utilities.isEnableBtnBg(this.mLauncher)) {
                this.mApplyView.setBackgroundResource(R.drawable.panel_btn_bg);
                this.mCancelView.setBackgroundResource(R.drawable.panel_btn_bg);
            }
            if (WhiteBgManager.isWhiteBg()) {
                changeColorForBg(true);
            }
            this.mScreenGridTopContainer.bringToFront();
        }
    }

    private void updateBtnForScreenGrid(String tag) {
        if (tag != null) {
            int count = this.mGridBtnLayout.getChildCount();
            this.mSelectedGrid[0] = Character.getNumericValue(tag.charAt(0));
            this.mSelectedGrid[1] = Character.getNumericValue(tag.charAt(2));
            for (int i = 0; i < count; i++) {
                View v = this.mGridBtnLayout.getChildAt(i);
                String name = (String) v.getTag();
                if (name != null) {
                    v.setSelected(name.equals(tag));
                    setSpanDescription(name.equals(tag));
                    int gridX = Character.getNumericValue(name.charAt(0));
                    int gridY = Character.getNumericValue(name.charAt(2));
                    v.setContentDescription(String.format(mSpanDescriptionFormat, new Object[]{Integer.valueOf(gridX), Integer.valueOf(gridY)}));
                }
            }
        }
    }

    private int getButtonResId(String tag) {
        String packageName = this.mLauncher.getPackageName();
        return getResources().getIdentifier("drawable/screen_grid_icon_" + tag, "drawable", packageName);
    }

    private void setSpanDescription(boolean setSelected) {
        String selectTTS;
        Resources res = getResources();
        String selected = res.getString(R.string.selected);
        String notSelected = res.getString(R.string.not_selected);
        if (setSelected) {
            selectTTS = selected;
        } else {
            selectTTS = notSelected;
        }
        mSpanDescriptionFormat = res.getString(R.string.talkback_widget_dims_format) + ", " + selectTTS;
    }

    void updateButtonStatus() {
        int[] cellXY = new int[2];
        Utilities.loadCurrentGridSize(this.mLauncher, cellXY);
        updateBtnForScreenGrid(cellXY[0] + DefaultLayoutParser.ATTR_X + cellXY[1]);
        this.mApplyView.setEnabled(false);
        this.mApplyView.setAlpha(0.4f);
    }

    void changeColorForBg(boolean whiteBg) {
        int childCount = this.mGridBtnLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (this.mGridBtnLayout.getChildAt(i) != null) {
                WhiteBgManager.changeColorFilterForBg(this.mLauncher, ((TextView) this.mGridBtnLayout.getChildAt(i)).getCompoundDrawables()[1], whiteBg);
                WhiteBgManager.changeTextColorForBg(this.mLauncher, (TextView) this.mGridBtnLayout.getChildAt(i), whiteBg);
            }
        }
        if (this.mScreenGridExplainView != null) {
            WhiteBgManager.changeTextColorForBg(this.mLauncher, this.mScreenGridExplainView, whiteBg);
        }
        WhiteBgManager.changeTextColorForBg(this.mLauncher, this.mApplyView, whiteBg);
        WhiteBgManager.changeTextColorForBg(this.mLauncher, this.mCancelView, whiteBg);
    }

    public View getScreenGridTopConatiner() {
        return this.mScreenGridTopContainer;
    }

    void onConfigurationChangedIfNeeded() {
        MarginLayoutParams marginLp = (MarginLayoutParams) getLayoutParams();
        marginLp.bottomMargin = getResources().getDimensionPixelOffset(R.dimen.screen_grid_panel_margin_bottom);
        setLayoutParams(marginLp);
        if (this.mScreenGridExplainView != null) {
            ViewGroup.LayoutParams explainViewLp = this.mScreenGridExplainView.getLayoutParams();
            explainViewLp.height = getResources().getDimensionPixelOffset(R.dimen.screen_grid_explain_view_height);
            this.mScreenGridExplainView.setLayoutParams(explainViewLp);
        }
        LinearLayout buttonLayout = (LinearLayout) this.mLauncher.findViewById(R.id.screen_grid_button_layout);
        ViewGroup.LayoutParams buttonLayoutLp = buttonLayout.getLayoutParams();
        buttonLayoutLp.height = getResources().getDimensionPixelOffset(R.dimen.screen_grid_top_button_height);
        buttonLayout.setLayoutParams(buttonLayoutLp);
        updateTextSize(this.mApplyView);
        updateTextSize(this.mCancelView);
    }

    private void updateTextSize(TextView button) {
        if (button != null) {
            button.setTextSize(0, getResources().getDimension(R.dimen.screen_grid_top_button_text_size));
        }
    }

    public LinearLayout getGriBtnLayout() {
        return this.mGridBtnLayout;
    }

    public int[] getSelectedGrid() {
        return this.mSelectedGrid;
    }
}

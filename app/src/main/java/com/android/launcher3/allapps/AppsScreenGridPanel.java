package com.android.launcher3.allapps;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.controller.AppsController;
import com.android.launcher3.common.base.controller.ControllerBase;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.model.DefaultLayoutParser;
import com.android.launcher3.home.HomeFocusLogic;
import com.android.launcher3.util.GlobalSettingUtils;
import com.android.launcher3.util.ScreenGridUtilities;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;

public class AppsScreenGridPanel extends LinearLayout {
    private static final String TAG = "AppsScreenGridPanel";
    private final OnClickListener mApplyButtonClickListener;
    private View mApplyView;
    private AppsController mAppsController;
    private String[] mAppsGridButtonMap;
    private final OnKeyListener mAppsScreenGridPanelKeyListener;
    private final OnClickListener mCancelButtonClickListener;
    private View mCancelView;
    private LinearLayout mGridBtnLayout;
    private ArrayList<TextView> mGridBtnTextViews;
    private final OnClickListener mGridButtonClickListener;
    private final Launcher mLauncher;
    private String[] mScreenGridButtonMap;
    private LinearLayout mScreenGridTopContainer;
    private int[] mSelectedGrid;
    private String mSpanDescriptionFormat;

    public AppsScreenGridPanel(Context context) {
        this(context, null);
    }

    public AppsScreenGridPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppsScreenGridPanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mScreenGridButtonMap = null;
        this.mAppsGridButtonMap = null;
        this.mScreenGridTopContainer = null;
        this.mSelectedGrid = null;
        this.mGridButtonClickListener = new OnClickListener() {
            public void onClick(View view) {
                if (!AppsScreenGridPanel.this.mAppsController.isSwitchingState()) {
                    String buttonName = (String) view.getTag();
                    int gridX = Character.getNumericValue(buttonName.charAt(0));
                    int gridY = Character.getNumericValue(buttonName.charAt(2));
                    AppsScreenGridPanel.this.updateBtnForScreenGrid(buttonName);
                    AppsScreenGridPanel.this.mAppsController.changeScreenGrid(true, gridX, gridY);
                    SALogging.getInstance().insertClickGridButtonLog(gridX, gridY, false);
                }
            }
        };
        this.mApplyButtonClickListener = new OnClickListener() {
            public void onClick(View view) {
                if (!AppsScreenGridPanel.this.mAppsController.isSwitchingState()) {
                    if (GlobalSettingUtils.getStartSetting()) {
                        GlobalSettingUtils.resetSettingsValue();
                        AppsScreenGridPanel.this.mLauncher.finishSettingsActivity();
                    }
                    Log.d(AppsScreenGridPanel.TAG, "ScreenGrid apply button clicked.");
                    AppsScreenGridPanel.this.mAppsController.applyScreenGrid();
                    DeviceProfile grid = AppsScreenGridPanel.this.mLauncher.getDeviceProfile();
                    SALogging.getInstance().insertChangeGridLog(grid.appsGrid.getCellCountX(), grid.appsGrid.getCellCountY(), false, false);
                    AppsScreenGridPanel.this.mApplyView.performAccessibilityAction(128, null);
                }
            }
        };
        this.mCancelButtonClickListener = new OnClickListener() {
            public void onClick(View v) {
                if (!AppsScreenGridPanel.this.mAppsController.isSwitchingState()) {
                    AppsScreenGridPanel.this.mAppsController.cancelChangeScreenGrid();
                    Log.d(AppsScreenGridPanel.TAG, "ScreenGrid cancel button clicked.");
                    SALogging.getInstance().insertEventLog(AppsScreenGridPanel.this.getResources().getString(R.string.screen_AppsScreenGrid), AppsScreenGridPanel.this.getResources().getString(R.string.event_SG_Cancel_Apps));
                }
            }
        };
        this.mAppsScreenGridPanelKeyListener = new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                boolean consume = HomeFocusLogic.shouldConsume(keyCode);
                if (!(event.getAction() == 1 || !consume || AppsScreenGridPanel.this.mGridBtnLayout == null || AppsScreenGridPanel.this.mScreenGridTopContainer == null)) {
                    if (Utilities.sIsRtl) {
                        if (keyCode == 21) {
                            keyCode = 22;
                        } else if (keyCode == 22) {
                            keyCode = 21;
                        }
                    }
                    ViewGroup parent = v.getParent() == AppsScreenGridPanel.this.mGridBtnLayout ? AppsScreenGridPanel.this.mGridBtnLayout : AppsScreenGridPanel.this.mScreenGridTopContainer;
                    int index = parent.indexOfChild(v);
                    int childCount = parent.getChildCount();
                    View childView;
                    switch (keyCode) {
                        case 19:
                            if (AppsScreenGridPanel.this.mGridBtnLayout == parent) {
                                childView = AppsScreenGridPanel.this.mScreenGridTopContainer.getChildAt(0);
                                if (childView != null && childView.isEnabled()) {
                                    childView.requestFocus();
                                    childView.playSoundEffect(0);
                                    break;
                                }
                            }
                            break;
                        case 20:
                            if (AppsScreenGridPanel.this.mScreenGridTopContainer == parent) {
                                childView = AppsScreenGridPanel.this.mGridBtnLayout.getChildAt(0);
                                if (childView != null && childView.isEnabled()) {
                                    childView.requestFocus();
                                    childView.playSoundEffect(0);
                                    break;
                                }
                            }
                            break;
                        case 21:
                            childView = parent.getChildAt(Math.max(index - 1, 0));
                            if (childView != null && childView.isEnabled()) {
                                childView.requestFocus();
                                childView.playSoundEffect(0);
                                break;
                            }
                        case 22:
                            childView = parent.getChildAt(Math.min(index + 1, childCount - 1));
                            if (childView != null && childView.isEnabled()) {
                                childView.requestFocus();
                                childView.playSoundEffect(0);
                                break;
                            }
                        default:
                            break;
                    }
                }
                return consume;
            }
        };
        this.mLauncher = (Launcher) context;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources res = getResources();
        this.mSpanDescriptionFormat = res.getString(R.string.talkback_widget_dims_format);
        if (this.mScreenGridButtonMap == null) {
            String gridSet = ScreenGridUtilities.loadAppsSupportedGridSet(getContext());
            if (gridSet == null || gridSet.isEmpty()) {
                this.mScreenGridButtonMap = res.getStringArray(R.array.support_apps_grid_size);
            } else {
                this.mScreenGridButtonMap = gridSet.split("\\|");
            }
        }
        if (this.mAppsGridButtonMap == null) {
            this.mAppsGridButtonMap = res.getStringArray(R.array.apps_grid_info);
        }
        if (this.mSelectedGrid == null) {
            this.mSelectedGrid = new int[2];
        }
        DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
        String currentGrid = deviceProfile.appsGrid.getCellCountX() + DefaultLayoutParser.ATTR_X + deviceProfile.appsGrid.getCellCountY();
        if (this.mScreenGridButtonMap != null && this.mScreenGridButtonMap.length > 0) {
            this.mGridBtnLayout = (LinearLayout) findViewById(R.id.apps_screen_grid_btn_layout);
            for (String buttonName : this.mScreenGridButtonMap) {
                addGridButton(buttonName, currentGrid);
            }
        }
    }

    public void bindController(ControllerBase controller) {
        this.mAppsController = (AppsController) controller;
    }

    public void initScreenGridTopContainer() {
        if (this.mScreenGridTopContainer == null) {
            this.mScreenGridTopContainer = (LinearLayout) this.mLauncher.findViewById(R.id.apps_screen_grid_top_container);
            this.mApplyView = this.mScreenGridTopContainer.findViewById(R.id.apps_screen_grid_apply_button);
            this.mCancelView = this.mScreenGridTopContainer.findViewById(R.id.apps_screen_grid_cancel_button);
            String buttonString = getResources().getString(R.string.accessibility_button);
            this.mApplyView.setContentDescription(((TextView) this.mApplyView).getText() + ", " + buttonString);
            this.mCancelView.setContentDescription(((TextView) this.mCancelView).getText() + ", " + buttonString);
            this.mApplyView.setOnClickListener(this.mApplyButtonClickListener);
            this.mCancelView.setOnClickListener(this.mCancelButtonClickListener);
            this.mApplyView.setEnabled(false);
            this.mApplyView.setAlpha(0.4f);
            this.mApplyView.setOnKeyListener(this.mAppsScreenGridPanelKeyListener);
            this.mCancelView.setOnKeyListener(this.mAppsScreenGridPanelKeyListener);
            if (Utilities.isEnableBtnBg(this.mLauncher)) {
                this.mApplyView.setBackgroundResource(R.drawable.panel_btn_bg);
                this.mCancelView.setBackgroundResource(R.drawable.panel_btn_bg);
            }
            this.mScreenGridTopContainer.bringToFront();
        }
    }

    public void updateApplyCancelButton() {
        int[] cellXY = new int[2];
        ScreenGridUtilities.loadCurrentAppsGridSize(this.mLauncher, cellXY);
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        int gridX = grid.appsGrid.getCellCountX();
        int gridY = grid.appsGrid.getCellCountY();
        if (cellXY[0] == gridX && cellXY[1] == gridY) {
            this.mApplyView.setEnabled(false);
            this.mApplyView.setAlpha(0.4f);
            return;
        }
        this.mApplyView.setEnabled(true);
        this.mApplyView.setAlpha(1.0f);
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
                    v.setContentDescription(String.format(this.mSpanDescriptionFormat, new Object[]{Integer.valueOf(gridX), Integer.valueOf(gridY)}));
                }
            }
        }
    }

    private int getButtonResId(String tag) {
        String packageName = this.mLauncher.getPackageName();
        return getResources().getIdentifier("drawable/screen_grid_icon_" + tag, "drawable", packageName);
    }

    public void setScreenGridProxy(String gridOption) {
        if (gridOption != null) {
            int child = 0;
            int cellX = gridOption.charAt(0);
            int cellY = gridOption.charAt(gridOption.length() - 1);
            String[] strArr = this.mAppsGridButtonMap;
            int length = strArr.length;
            int i = 0;
            while (i < length) {
                String map = strArr[i];
                int appsX = map.charAt(0);
                int appsY = map.charAt(2);
                if (cellX == appsX && cellY == appsY) {
                    this.mGridButtonClickListener.onClick(this.mGridBtnLayout.getChildAt(child));
                    return;
                } else {
                    child++;
                    i++;
                }
            }
        }
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
        this.mSpanDescriptionFormat = res.getString(R.string.talkback_widget_dims_format) + ", " + selectTTS;
    }

    public void updateButtonStatus() {
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        updateBtnForScreenGrid(grid.appsGrid.getCellCountX() + DefaultLayoutParser.ATTR_X + grid.appsGrid.getCellCountY());
        this.mApplyView.setEnabled(false);
        this.mApplyView.setAlpha(0.4f);
    }

    public View getScreenGridTopContainer() {
        return this.mScreenGridTopContainer;
    }

    public void onConfigurationChangedIfNeeded() {
        MarginLayoutParams marginLp = (MarginLayoutParams) getLayoutParams();
        marginLp.bottomMargin = getResources().getDimensionPixelOffset(R.dimen.screen_grid_panel_margin_bottom);
        setLayoutParams(marginLp);
        LayoutParams topContainerLp = (LayoutParams) this.mScreenGridTopContainer.getLayoutParams();
        topContainerLp.height = getResources().getDimensionPixelOffset(R.dimen.screen_grid_top_button_height);
        this.mScreenGridTopContainer.setLayoutParams(topContainerLp);
        updateTextSize((TextView) this.mApplyView);
        updateTextSize((TextView) this.mCancelView);
    }

    private void updateTextSize(TextView button) {
        if (button != null) {
            button.setTextSize(0, getResources().getDimension(R.dimen.screen_grid_top_button_text_size));
        }
    }

    public void updateGridBtnLayout() {
        int i;
        String gridSet = ScreenGridUtilities.loadAppsSupportedGridSet(getContext());
        Log.d(TAG, "updateGridBtnLayout gridSet: " + gridSet);
        if (gridSet == null || gridSet.isEmpty()) {
            this.mScreenGridButtonMap = getResources().getStringArray(R.array.support_apps_grid_size);
        } else {
            this.mScreenGridButtonMap = gridSet.split("\\|");
        }
        DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
        String currentGrid = deviceProfile.appsGrid.getCellCountX() + DefaultLayoutParser.ATTR_X + deviceProfile.appsGrid.getCellCountY();
        for (i = 0; i < this.mGridBtnTextViews.size(); i++) {
            setGridBtnText((TextView) this.mGridBtnTextViews.get(i), this.mScreenGridButtonMap[i]);
            ((TextView) this.mGridBtnTextViews.get(i)).setTag(this.mScreenGridButtonMap[i]);
        }
        if (this.mScreenGridButtonMap.length < this.mGridBtnTextViews.size()) {
            for (TextView view : this.mGridBtnTextViews.subList(this.mScreenGridButtonMap.length, this.mGridBtnTextViews.size() - this.mScreenGridButtonMap.length)) {
                this.mGridBtnLayout.removeView(view);
                this.mGridBtnTextViews.remove(view);
            }
            this.mGridBtnLayout.invalidate();
        } else if (this.mScreenGridButtonMap.length > this.mGridBtnTextViews.size()) {
            int viewSize = this.mGridBtnTextViews.size();
            int gridSize = this.mScreenGridButtonMap.length;
            for (i = viewSize; i < gridSize; i++) {
                addGridButton(this.mScreenGridButtonMap[i], currentGrid);
            }
            this.mGridBtnLayout.invalidate();
        }
        updateBtnForScreenGrid(currentGrid);
    }

    private void addGridButton(String buttonName, String currentGrid) {
        boolean isEnableBtnBg = Utilities.isEnableBtnBg(getContext());
        int padding = getResources().getDimensionPixelSize(R.dimen.overview_panel_bg_padding);
        int margin = getResources().getDimensionPixelSize(R.dimen.screen_grid_panel_margin);
        TextView screenGridButton = new TextView(this.mLauncher);
        int resId = getButtonResId(buttonName);
        screenGridButton.setTag(buttonName);
        screenGridButton.setCompoundDrawablesWithIntrinsicBounds(0, resId, 0, 0);
        screenGridButton.setTextAppearance(R.style.ScreenGridButton);
        screenGridButton.setOnClickListener(this.mGridButtonClickListener);
        screenGridButton.setOnKeyListener(this.mAppsScreenGridPanelKeyListener);
        screenGridButton.setBackgroundResource(R.drawable.focusable_view_bg);
        if (isEnableBtnBg) {
            screenGridButton.setBackgroundResource(R.drawable.panel_btn_bg);
            screenGridButton.setPadding(padding, padding, padding, padding);
        }
        setGridBtnText(screenGridButton, buttonName);
        screenGridButton.setWidth(getResources().getDimensionPixelSize(R.dimen.apps_screen_grid_panel_width));
        screenGridButton.setHeight(getResources().getDimensionPixelSize(R.dimen.screen_grid_panel_height));
        screenGridButton.setGravity(49);
        screenGridButton.setSelected(currentGrid.equals(buttonName));
        screenGridButton.setFocusable(true);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(-2, -2);
        llp.setMargins(margin, 0, margin, 0);
        screenGridButton.setLayoutParams(llp);
        this.mGridBtnLayout.addView(screenGridButton);
        if (this.mGridBtnTextViews == null) {
            this.mGridBtnTextViews = new ArrayList();
        }
        this.mGridBtnTextViews.add(screenGridButton);
    }

    private void setGridBtnText(TextView screenGridButton, String buttonName) {
        String currentLanguage = Utilities.getLocale(getContext()).getLanguage();
        if ("ar".equals(currentLanguage) || "fa".equals(currentLanguage)) {
            screenGridButton.setText(Utilities.toArabicDigits(String.valueOf(buttonName.charAt(2)), currentLanguage) + DefaultLayoutParser.ATTR_X + Utilities.toArabicDigits(String.valueOf(buttonName.charAt(0)), currentLanguage));
        } else {
            screenGridButton.setText(buttonName);
        }
    }

    public int[] getSelectedGrid() {
        return this.mSelectedGrid;
    }

    public boolean checkValidGridOption(String gridOption) {
        int cellX = gridOption.charAt(0);
        int cellY = gridOption.charAt(gridOption.length() - 1);
        for (String map : this.mAppsGridButtonMap) {
            int homeX = map.charAt(0);
            int homeY = map.charAt(2);
            if (cellX == homeX && cellY == homeY) {
                return true;
            }
        }
        return false;
    }
}

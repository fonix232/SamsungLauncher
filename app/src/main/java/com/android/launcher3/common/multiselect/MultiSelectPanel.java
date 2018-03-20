package com.android.launcher3.common.multiselect;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.model.DataLoader;
import com.android.launcher3.common.quickoption.shortcuts.DeepShortcutManager;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Iterator;

public class MultiSelectPanel extends LinearLayout {
    public static final int BTN_CREATE_FOLDER = 2;
    public static final int BTN_MAX = 3;
    public static final int BTN_REMOVE_SHORTCUT = 1;
    public static final int BTN_UNINSTALL = 0;
    public static final int DIM_TYPE_ALL_FOLDER_ITEMS = 3;
    public static final int DIM_TYPE_ENABLE = 0;
    public static final int DIM_TYPE_ONE_ITEM = 1;
    public static final int DIM_TYPE_SELECT_FOLDER = 2;
    private static final Interpolator SINE_IN_OUT_33 = ViInterpolator.getInterploator(30);
    private static final Interpolator SINE_IN_OUT_80 = ViInterpolator.getInterploator(34);
    private boolean mAcceptDropToFolder;
    private AnimatorSet mAnimator;
    private TextView mCreateFolderButton;
    private int mDimTypeCreateFolder;
    private boolean[] mEnabledBtn;
    private Launcher mLauncher;
    private MultiSelectManager mMultiSelectManager;
    private OnClickListener mOnClickListener;
    private OnTouchListener mOnTouchListener;
    private TextView mRemoveShortcutButton;
    private View mRemoveShortcutButtonLayout;
    private TextView mUninstallButton;

    public MultiSelectPanel(Context context) {
        this(context, null);
    }

    public MultiSelectPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiSelectPanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mAnimator = null;
        this.mAcceptDropToFolder = false;
        this.mDimTypeCreateFolder = 0;
        this.mEnabledBtn = new boolean[3];
        this.mOnClickListener = new OnClickListener() {
            public void onClick(View v) {
                MultiSelectPanel.this.onClickMultiSelectPanel(MultiSelectPanel.this.getIdByView(v));
            }
        };
        this.mOnTouchListener = new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent ev) {
                if (MultiSelectPanel.this.getButtonEnabled(MultiSelectPanel.this.getIdByView(v))) {
                    int action = ev.getAction();
                    if (action == 0) {
                        v.setAlpha(0.5f);
                    } else if (action != 2) {
                        v.setAlpha(1.0f);
                    }
                }
                return false;
            }
        };
        this.mLauncher = (Launcher) context;
        this.mMultiSelectManager = this.mLauncher.getMultiSelectManager();
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mUninstallButton = (TextView) findViewById(R.id.multi_select_uninstall);
        this.mUninstallButton.setOnTouchListener(this.mOnTouchListener);
        this.mUninstallButton.setOnClickListener(this.mOnClickListener);
        this.mRemoveShortcutButtonLayout = findViewById(R.id.multi_select_remove_shortcut_layout);
        this.mRemoveShortcutButton = (TextView) findViewById(R.id.multi_select_remove_shortcut);
        this.mRemoveShortcutButton.setOnTouchListener(this.mOnTouchListener);
        this.mRemoveShortcutButton.setOnClickListener(this.mOnClickListener);
        this.mCreateFolderButton = (TextView) findViewById(R.id.multi_select_create_folder);
        this.mCreateFolderButton.setOnTouchListener(this.mOnTouchListener);
        this.mCreateFolderButton.setOnClickListener(this.mOnClickListener);
        setEditTextBg();
        setEnabledButton(0, false);
        setEnabledButton(1, false);
        setEnabledButton(2, false);
    }

    private void onClickMultiSelectPanel(int id) {
        if (this.mMultiSelectManager != null) {
            this.mMultiSelectManager.onClickMultiSelectPanel(id);
        }
    }

    private void setEditTextBg() {
        boolean isEnableBtnBg = Utilities.isEnableBtnBg(getContext());
        int padding = getResources().getDimensionPixelSize(R.dimen.multi_select_panel_bg_padding);
        if (isEnableBtnBg) {
            this.mUninstallButton.setBackgroundResource(R.drawable.panel_btn_bg);
            this.mUninstallButton.setPadding(padding, 0, padding, 0);
            this.mRemoveShortcutButton.setBackgroundResource(R.drawable.panel_btn_bg);
            this.mRemoveShortcutButton.setPadding(padding, 0, padding, 0);
            this.mCreateFolderButton.setBackgroundResource(R.drawable.panel_btn_bg);
            this.mCreateFolderButton.setPadding(padding, 0, padding, 0);
        }
    }

    public void showMultiSelectPanel(final boolean show, boolean animated) {
        float scale = 1.0f;
        int i = 0;
        cancelAnimation();
        float alpha = show ? 1.0f : 0.0f;
        if (!show) {
            scale = 0.95f;
        }
        if (animated) {
            ObjectAnimator scaleXAnim = LauncherAnimUtils.ofFloat(this, View.SCALE_X.getName(), scale);
            ObjectAnimator scaleYAnim = LauncherAnimUtils.ofFloat(this, View.SCALE_Y.getName(), scale);
            ObjectAnimator alphaAnim = LauncherAnimUtils.ofFloat(this, View.ALPHA.getName(), alpha);
            scaleXAnim.setInterpolator(SINE_IN_OUT_80);
            scaleYAnim.setInterpolator(SINE_IN_OUT_80);
            alphaAnim.setInterpolator(SINE_IN_OUT_33);
            this.mAnimator = new AnimatorSet();
            this.mAnimator.playTogether(new Animator[]{scaleXAnim, scaleYAnim, alphaAnim});
            this.mAnimator.setDuration(200);
            this.mAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    if (show) {
                        MultiSelectPanel.this.setVisibility(View.VISIBLE);
                        MultiSelectPanel.this.bringToFront();
                    }
                }

                public void onAnimationEnd(Animator animation) {
                    if (!show) {
                        MultiSelectPanel.this.setVisibility(View.GONE);
                    }
                }

                public void onAnimationCancel(Animator animation) {
                    if (!show) {
                        MultiSelectPanel.this.setVisibility(View.GONE);
                    }
                }
            });
            this.mAnimator.start();
            return;
        }
        setScaleX(scale);
        setScaleY(scale);
        setAlpha(alpha);
        if (!show) {
            i = 8;
        }
        setVisibility(i);
    }

    private void cancelAnimation() {
        if (this.mAnimator != null) {
            this.mAnimator.cancel();
        }
        this.mAnimator = null;
    }

    private View getViewById(int id) {
        switch (id) {
            case 0:
                return this.mUninstallButton;
            case 1:
                return this.mRemoveShortcutButton;
            case 2:
                return this.mCreateFolderButton;
            default:
                return null;
        }
    }

    private int getIdByView(View view) {
        if (view == this.mUninstallButton) {
            return 0;
        }
        if (view == this.mRemoveShortcutButton) {
            return 1;
        }
        if (view == this.mCreateFolderButton) {
            return 2;
        }
        return 0;
    }

    void updateEnabledButton() {
        SparseBooleanArray btnEnableList = getEnabledButton();
        for (int i = 0; i < btnEnableList.size(); i++) {
            int key = btnEnableList.keyAt(i);
            setEnabledButton(key, btnEnableList.get(key));
        }
        setContentDescription();
    }

    private void setEnabledButton(int id, boolean enabled) {
        TextView button = (TextView) getViewById(id);
        if (button != null) {
            button.setAlpha(enabled ? 1.0f : 0.4f);
            setShadowLayer(button, enabled);
            this.mEnabledBtn[id] = enabled;
        }
    }

    public boolean getButtonEnabled(int id) {
        return this.mEnabledBtn[id];
    }

    private SparseBooleanArray getEnabledButton() {
        ArrayList<View> appsViewList = this.mMultiSelectManager.getCheckedAppsViewList();
        SparseBooleanArray btnEnableList = new SparseBooleanArray();
        btnEnableList.put(0, false);
        btnEnableList.put(1, false);
        btnEnableList.put(2, false);
        this.mAcceptDropToFolder = false;
        this.mMultiSelectManager.clearUninstallPendigList();
        if (appsViewList.size() <= 0) {
            this.mDimTypeCreateFolder = 1;
        } else {
            btnEnableList.put(1, true);
            if (appsViewList.size() > 1) {
                this.mDimTypeCreateFolder = 0;
                btnEnableList.put(2, true);
            } else {
                this.mDimTypeCreateFolder = 1;
            }
            ArrayList<Long> folderContainerList = new ArrayList();
            boolean canUninstall = false;
            Iterator it = appsViewList.iterator();
            while (it.hasNext()) {
                View view = (View) it.next();
                ItemInfo item = (ItemInfo) view.getTag();
                if (LauncherFeature.supportFolderSelect()) {
                    if (view instanceof FolderIconView) {
                        this.mDimTypeCreateFolder = 2;
                        btnEnableList.put(2, false);
                    } else if (view instanceof IconView) {
                        this.mAcceptDropToFolder = true;
                    }
                }
                if (item instanceof IconInfo) {
                    ComponentName compName = item.componentName;
                    String pkgName = null;
                    if (compName == null) {
                        compName = ((IconInfo) item).getTargetComponent();
                    }
                    if (compName != null) {
                        pkgName = compName.getPackageName();
                    }
                    if (DeepShortcutManager.supportsShortcuts(item) && !canUninstall && pkgName != null && Utilities.canUninstall(this.mLauncher, pkgName)) {
                        canUninstall = true;
                    }
                    if (!DeepShortcutManager.supportsShortcuts(item) || (!canUninstall && (pkgName == null || !Utilities.canDisable(this.mLauncher, pkgName)))) {
                        this.mMultiSelectManager.addUninstallPendingList(item.title != null ? item.title.toString() : "");
                    } else {
                        btnEnableList.put(0, true);
                    }
                }
                if (item.container > 0) {
                    folderContainerList.add(Long.valueOf(item.container));
                }
            }
            updateUninstallButtonText(canUninstall);
            if (this.mLauncher.isFolderStage()) {
                int size = folderContainerList.size();
                if (size > 0 && size == appsViewList.size()) {
                    long refContainer = ((Long) folderContainerList.get(size - 1)).longValue();
                    FolderInfo folderInfo = DataLoader.getFolderInfo((int) refContainer);
                    if (folderInfo != null && folderInfo.getItemCount() == size) {
                        boolean enable = false;
                        Iterator it2 = folderContainerList.iterator();
                        while (it2.hasNext()) {
                            if (((Long) it2.next()).longValue() != refContainer) {
                                enable = true;
                                break;
                            }
                        }
                        if (!enable) {
                            this.mDimTypeCreateFolder = 3;
                        }
                        btnEnableList.put(2, enable);
                    }
                }
            }
        }
        return btnEnableList;
    }

    private void setShadowLayer(TextView view, boolean enabled) {
        int i = 1;
        Resources res = this.mLauncher.getResources();
        Stage top = this.mLauncher.getStageManager().getTopStage();
        Stage secondTop = this.mLauncher.getStageManager().getSecondTopStage();
        boolean isHome;
        if (top == null || !(top.getMode() == 1 || (secondTop != null && top.getMode() == 5 && secondTop.getMode() == 1))) {
            isHome = false;
        } else {
            isHome = true;
        }
        if (WhiteBgManager.isWhiteBg() && isHome) {
            i = 0;
        }
        if (enabled & i) {
            view.setShadowLayer((float) res.getInteger(R.integer.text_shadow_radius), 0.0f, (float) res.getInteger(R.integer.text_shadow_dy), res.getColor(R.color.text_shadow_color, null));
        } else {
            view.setShadowLayer(0.0f, 0.0f, 0.0f, 0);
        }
    }

    protected void onVisibilityChanged(View changedView, int visibility) {
        boolean z = false;
        if (visibility == 0) {
            boolean isHome = isHomeStage();
            if (LauncherAppState.getInstance().isHomeOnlyModeEnabled() || !isHome) {
                this.mRemoveShortcutButtonLayout.setVisibility(View.GONE);
            } else {
                this.mRemoveShortcutButtonLayout.setVisibility(View.VISIBLE);
            }
            updateMultiSelectPanelLayout();
            updateTextViewPosition();
            if (WhiteBgManager.isWhiteBg() && isHome) {
                z = true;
            }
            changeColorForBg(z);
        }
        super.onVisibilityChanged(changedView, visibility);
    }

    private boolean isHomeStage() {
        Stage top = this.mLauncher.getStageManager().getTopStage();
        Stage secondTop = this.mLauncher.getStageManager().getSecondTopStage();
        if (top.getMode() == 1) {
            return true;
        }
        if (secondTop != null && top.getMode() == 5 && secondTop.getMode() == 1) {
            return true;
        }
        return false;
    }

    private int getWeightSumPanel() {
        int weight = 0;
        for (int i = 0; i < 3; i++) {
            if (getViewById(i).getVisibility() == 0) {
                weight++;
            }
        }
        return weight;
    }

    public boolean acceptDropToFolder() {
        return this.mAcceptDropToFolder;
    }

    int getDimTypeCreateFolder() {
        return this.mDimTypeCreateFolder;
    }

    private void changeColorForBg(boolean whiteBg) {
        int index;
        WhiteBgManager.changeColorFilterForBg(this.mLauncher, ((LinearLayout) findViewById(R.id.multi_select_panel_layout)).getBackground(), whiteBg);
        if (isLandscapeMode()) {
            index = 0;
        } else {
            index = 1;
        }
        WhiteBgManager.changeTextColorForBg(this.mLauncher, this.mUninstallButton, whiteBg);
        WhiteBgManager.changeColorFilterForBg(this.mLauncher, this.mUninstallButton.getCompoundDrawables()[index], whiteBg);
        setShadowLayer(this.mUninstallButton, this.mEnabledBtn[0]);
        WhiteBgManager.changeTextColorForBg(this.mLauncher, this.mRemoveShortcutButton, whiteBg);
        WhiteBgManager.changeColorFilterForBg(this.mLauncher, this.mRemoveShortcutButton.getCompoundDrawables()[index], whiteBg);
        setShadowLayer(this.mRemoveShortcutButton, this.mEnabledBtn[1]);
        WhiteBgManager.changeTextColorForBg(this.mLauncher, this.mCreateFolderButton, whiteBg);
        WhiteBgManager.changeColorFilterForBg(this.mLauncher, this.mCreateFolderButton.getCompoundDrawables()[index], whiteBg);
        setShadowLayer(this.mCreateFolderButton, this.mEnabledBtn[2]);
    }

    void onConfigurationChangedIfNeeded() {
        Resources res = this.mLauncher.getResources();
        LayoutParams lp = getLayoutParams();
        lp.height = res.getDimensionPixelSize(R.dimen.multi_select_panel_height);
        setLayoutParams(lp);
        updateTextSize(this.mUninstallButton);
        updateTextSize(this.mRemoveShortcutButton);
        updateTextSize(this.mCreateFolderButton);
        updateTextViewOnConfigurationChanged();
    }

    private void updateTextSize(TextView button) {
        if (button != null) {
            button.setTextSize(0, getResources().getDimension(R.dimen.multi_select_panel_text_size));
        }
    }

    private void updateTextViewOnConfigurationChanged() {
        updateTextViewPosition();
        boolean z = WhiteBgManager.isWhiteBg() && isHomeStage();
        changeColorForBg(z);
        int drawablePadding = this.mLauncher.getResources().getDimensionPixelSize(R.dimen.multi_select_panel_drawable_padding);
        for (int i = 0; i < 3; i++) {
            ((TextView) getViewById(i)).setCompoundDrawablePadding(drawablePadding);
        }
    }

    private void updateTextViewPosition() {
        if (isLandscapeMode()) {
            this.mUninstallButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.homescreen_edit_selection_uninstall, 0, 0, 0);
            this.mRemoveShortcutButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.homescreen_edit_selection_remove, 0, 0, 0);
            this.mCreateFolderButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.homescreen_edit_selection_create, 0, 0, 0);
        } else {
            this.mUninstallButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.homescreen_edit_selection_uninstall, 0, 0);
            this.mRemoveShortcutButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.homescreen_edit_selection_remove, 0, 0);
            this.mCreateFolderButton.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.homescreen_edit_selection_create, 0, 0);
        }
        for (int i = 0; i < 3; i++) {
            int i2;
            View viewById = getViewById(i);
            if (isLandscapeMode()) {
                i2 = -2;
            } else {
                i2 = -1;
            }
            viewById.setLayoutParams(new LinearLayout.LayoutParams(i2, -1));
        }
    }

    private void updateUninstallButtonText(boolean canUninstall) {
        if (canUninstall) {
            this.mUninstallButton.setText(this.mLauncher.getString(R.string.multi_select_uninstall));
        } else {
            this.mUninstallButton.setText(this.mLauncher.getString(R.string.multi_select_disable));
        }
    }

    String getTextForUninstallButton() {
        return this.mUninstallButton.getText() != null ? this.mUninstallButton.getText().toString() : null;
    }

    private void setContentDescription() {
        String btnText = getResources().getString(R.string.accessibility_button);
        String dimText = getResources().getString(R.string.accessibility_dimmed);
        for (int i = 0; i < 3; i++) {
            TextView btn = (TextView) getViewById(i);
            if (btn.getVisibility() != 8) {
                if (getButtonEnabled(i)) {
                    btn.setContentDescription(btn.getText() + ", " + btnText);
                } else {
                    btn.setContentDescription(btn.getText() + ", " + btnText + ", " + dimText);
                }
            }
        }
    }

    private boolean isLandscapeMode() {
        return this.mLauncher.getDeviceProfile().isLandscape && !LauncherFeature.isTablet();
    }

    void updateMultiSelectPanelLayout() {
        LinearLayout panel = (LinearLayout) findViewById(R.id.multi_select_panel_layout);
        if (panel != null) {
            Resources res = this.mLauncher.getResources();
            DeviceProfile dp = this.mLauncher.getDeviceProfile();
            int sideMargin = res.getDimensionPixelSize(R.dimen.multi_select_panel_margin_start);
            int topMargin = res.getDimensionPixelSize(R.dimen.multi_select_panel_margin_top);
            int bottomMargin = res.getDimensionPixelSize(R.dimen.multi_select_panel_margin_bottom);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) panel.getLayoutParams();
            if (!dp.isLandscape) {
                lp.leftMargin = sideMargin;
                lp.rightMargin = sideMargin;
            } else if (Utilities.getNavigationBarPositon() == 1) {
                lp.leftMargin = (dp.isMultiwindowMode ? 0 : dp.navigationBarHeight) + sideMargin;
                lp.rightMargin = sideMargin;
            } else {
                lp.leftMargin = sideMargin;
                lp.rightMargin = dp.navigationBarHeight + sideMargin;
            }
            lp.topMargin = topMargin;
            lp.bottomMargin = bottomMargin;
            lp.width = res.getDisplayMetrics().widthPixels - (lp.leftMargin + lp.rightMargin);
            lp.weight = (float) getWeightSumPanel();
        }
    }
}

package com.android.launcher3.common.multiselect;

import android.app.Activity;
import android.content.ComponentName;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.PagedView.PageScrollListener;
import com.android.launcher3.common.dialog.DisableAppConfirmationDialog;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.util.DualAppUtils;
import com.android.launcher3.util.UninstallAppUtils;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Iterator;

public class MultiSelectManager {
    private static final int CONFIGURATION_CHANGE_DELAY = 300;
    private static final int MAX_COUNT = 50;
    private static final int MIN_COUNT_CREATE_FOLDER = 2;
    private static final String TAG = "MultiSelectManager";
    private SparseArray<DragSource> mCheckedAppsSourceMap = new SparseArray();
    private ArrayList<View> mCheckedAppsViewList = new ArrayList();
    private int mCurrentMobileKeyboard;
    private int mCurrentOrientation;
    private boolean mIsFromHomeKey = false;
    private Launcher mLauncher;
    private ArrayList<MultiSelectListener> mMultiSelectCallbacks = new ArrayList();
    private MultiSelectHelpDialog mMultiSelectHelpDialog;
    private boolean mMultiSelectMode = false;
    private MultiSelectPanel mMultiSelectPanel;
    private ArrayList<String> mPostUninstallPendingList = new ArrayList();
    private Toast mToast;
    private ArrayList<Object> mUninstallAppList = new ArrayList();
    private ArrayList<String> mUninstallPendingList = new ArrayList();

    public interface MultiSelectListener {
        void onChangeSelectMode(boolean z, boolean z2);

        void onClickMultiSelectPanel(int i);

        void onSetPageScrollListener(PageScrollListener pageScrollListener);
    }

    public void setup(Activity activity) {
        this.mLauncher = (Launcher) activity;
        this.mMultiSelectPanel = (MultiSelectPanel) this.mLauncher.findViewById(R.id.multi_select_panel);
        this.mMultiSelectHelpDialog = (MultiSelectHelpDialog) this.mLauncher.findViewById(R.id.multi_select_help_bubble);
        this.mCurrentOrientation = Utilities.getOrientation();
        this.mCurrentMobileKeyboard = -1;
    }

    public void onDestroy() {
        this.mMultiSelectCallbacks.clear();
    }

    public void addMultiSelectCallbacks(MultiSelectListener cb) {
        if (this.mMultiSelectCallbacks != null) {
            this.mMultiSelectCallbacks.add(cb);
            cb.onSetPageScrollListener(this.mMultiSelectHelpDialog);
        }
    }

    public void removeMultiSelectCallbacks(MultiSelectListener cb) {
        if (this.mMultiSelectCallbacks != null) {
            this.mMultiSelectCallbacks.remove(cb);
        }
    }

    public void onChangeSelectMode(boolean enter, boolean animated) {
        Log.d(TAG, "onChangeSelectMode - enter = " + enter);
        this.mMultiSelectMode = enter;
        if (this.mMultiSelectCallbacks != null && this.mMultiSelectCallbacks.size() > 0) {
            Iterator it = this.mMultiSelectCallbacks.iterator();
            while (it.hasNext()) {
                ((MultiSelectListener) it.next()).onChangeSelectMode(enter, animated);
            }
            if (enter) {
                if (enableHelpDialog()) {
                    showHelpDialog(true);
                }
            } else if (isShowingHelpDialog()) {
                hideHelpDialog(true);
            }
        }
        if (!enter) {
            if (this.mMultiSelectPanel.getVisibility() == 0) {
                showMultiSelectPanel(false, animated);
            }
            SALogging.getInstance().insertMultiSelectCancelLog(this.mLauncher, false, this.mIsFromHomeKey);
        }
        this.mIsFromHomeKey = false;
    }

    public void homeKeyPressed() {
        this.mIsFromHomeKey = true;
    }

    public boolean isMultiSelectMode() {
        return this.mMultiSelectMode;
    }

    public MultiSelectPanel getMultiSelectPanel() {
        return this.mMultiSelectPanel;
    }

    public void showMultiSelectPanel(boolean show, boolean animated) {
        if (show || this.mMultiSelectPanel.getVisibility() == 0) {
            this.mMultiSelectPanel.showMultiSelectPanel(show, animated);
        }
    }

    public void onClickMultiSelectPanel(int id) {
        SALogging.getInstance().insertMultiSelectLog(id, this.mCheckedAppsViewList, this.mLauncher, this.mMultiSelectPanel.getTextForUninstallButton());
        if (!this.mMultiSelectPanel.getButtonEnabled(id)) {
            showToast(id);
        } else if (this.mMultiSelectCallbacks != null && this.mMultiSelectCallbacks.size() > 0) {
            boolean done = false;
            Iterator it = this.mMultiSelectCallbacks.iterator();
            while (it.hasNext()) {
                MultiSelectListener cb = (MultiSelectListener) it.next();
                if (!done && id == 0) {
                    startUninstallActivity();
                    done = true;
                }
                cb.onClickMultiSelectPanel(id);
            }
            clearCheckedApps();
        }
    }

    public void addCheckedApp(View view, DragSource source) {
        this.mCheckedAppsViewList.add(view);
        this.mCheckedAppsSourceMap.put(view.hashCode(), source);
        updateEnabledButton();
    }

    public void removeCheckedApp(View view) {
        this.mCheckedAppsViewList.remove(view);
        this.mCheckedAppsSourceMap.remove(view.hashCode());
        if (this.mMultiSelectMode) {
            updateEnabledButton();
        }
        if (this.mCheckedAppsViewList.isEmpty() && isShowingHelpDialog()) {
            hideHelpDialog(true);
        }
    }

    public void clearCheckedApps() {
        while (this.mCheckedAppsViewList.size() > 0) {
            View view = (View) this.mCheckedAppsViewList.get(0);
            if (view instanceof IconView) {
                CheckBox checkBox = ((IconView) view).getCheckBox();
                if (checkBox != null) {
                    checkBox.setChecked(false);
                }
            }
        }
        this.mCheckedAppsViewList.clear();
        this.mCheckedAppsSourceMap.clear();
        this.mUninstallPendingList.clear();
    }

    public int getCheckedAppCount() {
        return this.mCheckedAppsViewList.size();
    }

    public ArrayList<View> getCheckedAppsViewList() {
        return this.mCheckedAppsViewList;
    }

    public DragSource getCheckedAppDragSource(int hashCode) {
        if (this.mCheckedAppsSourceMap != null) {
            return (DragSource) this.mCheckedAppsSourceMap.get(hashCode);
        }
        return null;
    }

    private void updateEnabledButton() {
        this.mMultiSelectPanel.updateEnabledButton();
    }

    public int getCheckedItemCountInFolder(long container) {
        int count = 0;
        if (this.mCheckedAppsViewList.size() > 0) {
            Iterator it = this.mCheckedAppsViewList.iterator();
            while (it.hasNext()) {
                if (container == ((ItemInfo) ((View) it.next()).getTag()).container) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean canLongClick(View view) {
        if (this.mMultiSelectMode && (view instanceof FolderIconView)) {
            return LauncherFeature.supportFolderSelect();
        }
        return true;
    }

    public boolean canSelectItem() {
        boolean ret;
        if (this.mCheckedAppsViewList.size() < MAX_COUNT) {
            ret = true;
        } else {
            ret = false;
        }
        if (!ret) {
            String text = String.format(this.mLauncher.getString(R.string.multi_select_max_count_notice), new Object[]{Integer.valueOf(MAX_COUNT)});
            if (this.mToast == null) {
                this.mToast = Toast.makeText(this.mLauncher, text, 0);
            } else {
                this.mToast.setText(text);
            }
            this.mToast.show();
        }
        return ret;
    }

    public void showToast(int id) {
        String text = "";
        switch (id) {
            case 0:
                if (this.mUninstallPendingList.size() >= 1 || this.mPostUninstallPendingList.size() >= 1) {
                    boolean postUninstall;
                    int size;
                    String title;
                    if (this.mPostUninstallPendingList.size() > 0) {
                        postUninstall = true;
                    } else {
                        postUninstall = false;
                    }
                    if (postUninstall) {
                        size = this.mPostUninstallPendingList.size();
                    } else {
                        size = this.mUninstallPendingList.size();
                    }
                    if (postUninstall) {
                        title = (String) this.mPostUninstallPendingList.get(0);
                    } else {
                        title = (String) this.mUninstallPendingList.get(0);
                    }
                    if (size <= 1) {
                        text = String.format(this.mLauncher.getString(R.string.multi_select_disable_app_notice_one), new Object[]{title});
                        break;
                    }
                    text = String.format(this.mLauncher.getString(R.string.multi_select_disable_app_notice_other), new Object[]{title, Integer.valueOf(size - 1)});
                    break;
                }
                return;
                break;
            case 1:
                if (this.mCheckedAppsViewList.size() >= 1) {
                    text = String.format(this.mLauncher.getString(R.string.multi_select_remove_shortcut_notice), new Object[]{((ItemInfo) ((View) this.mCheckedAppsViewList.get(0)).getTag()).title, Integer.valueOf(this.mCheckedAppsViewList.size() - 1)});
                    break;
                }
                return;
            case 2:
                if (this.mMultiSelectPanel.getDimTypeCreateFolder() != 1) {
                    if (this.mMultiSelectPanel.getDimTypeCreateFolder() != 2) {
                        if (this.mMultiSelectPanel.getDimTypeCreateFolder() == 3) {
                            text = this.mLauncher.getString(R.string.multi_select_create_folder_all_folder_items_notice);
                            break;
                        }
                    }
                    text = this.mLauncher.getString(R.string.multi_select_create_folder_select_folder_notice);
                    break;
                }
                text = String.format(this.mLauncher.getString(R.string.multi_select_create_folder_one_item_notice), new Object[]{Integer.valueOf(2)});
                break;
                break;
        }
        if (this.mToast == null) {
            this.mToast = Toast.makeText(this.mLauncher, text, 0);
        } else {
            this.mToast.setText(text);
        }
        this.mToast.show();
    }

    private void startUninstallActivity() {
        if (this.mCheckedAppsViewList.size() > 0) {
            clearUninstallApplist();
            Iterator it = this.mCheckedAppsViewList.iterator();
            while (it.hasNext()) {
                this.mUninstallAppList.add(((View) it.next()).getTag());
            }
            postUninstallActivity();
        }
    }

    public void postUninstallActivity() {
        if (this.mUninstallAppList.size() > 0) {
            if (DisableAppConfirmationDialog.isActive(this.mLauncher.getFragmentManager())) {
                Log.d(TAG, "postUninstallActivity - return by previous disable dialog");
                return;
            }
            Log.d(TAG, "postUninstallActivity - size = " + this.mUninstallAppList.size());
            IconInfo uninstallApp = this.mUninstallAppList.remove(0);
            IconInfo iconInfo = null;
            ComponentName compName = null;
            String pkgName = null;
            if (uninstallApp instanceof IconInfo) {
                iconInfo = uninstallApp;
                compName = iconInfo.componentName;
                pkgName = null;
                if (compName == null) {
                    compName = iconInfo.getTargetComponent();
                }
                if (compName != null) {
                    pkgName = compName.getPackageName();
                }
            }
            if (Utilities.canDisable(this.mLauncher, pkgName)) {
                disableApp(iconInfo, compName);
            } else if (!Utilities.canUninstall(this.mLauncher, pkgName)) {
                r5 = this.mPostUninstallPendingList;
                r4 = (iconInfo == null || iconInfo.title == null) ? ((ItemInfo) uninstallApp).title != null ? uninstallApp.title.toString() : "" : iconInfo.title.toString();
                r5.add(r4);
                postUninstallActivity();
            } else if (DualAppUtils.supportDualApp(this.mLauncher) && (DualAppUtils.isDualApp(iconInfo.user, pkgName) || DualAppUtils.hasDualApp(iconInfo.user, pkgName))) {
                DualAppUtils.uninstallOrDisableDualApp(this.mLauncher, pkgName, iconInfo.user);
            } else if (!UninstallAppUtils.startUninstallActivity(this.mLauncher, uninstallApp)) {
                r5 = this.mPostUninstallPendingList;
                r4 = (iconInfo == null || iconInfo.title == null) ? ((ItemInfo) uninstallApp).title != null ? uninstallApp.title.toString() : "" : iconInfo.title.toString();
                r5.add(r4);
                postUninstallActivity();
            }
        } else if (this.mPostUninstallPendingList.size() > 0) {
            showToast(0);
            this.mPostUninstallPendingList.clear();
        }
    }

    public void clearUninstallApplist() {
        this.mUninstallAppList.clear();
        this.mUninstallPendingList.clear();
        this.mPostUninstallPendingList.clear();
    }

    public void addUninstallPendingList(String title) {
        this.mUninstallPendingList.add(title);
    }

    public void clearUninstallPendigList() {
        this.mUninstallPendingList.clear();
    }

    public boolean acceptDropToFolder() {
        return LauncherFeature.supportFolderSelect() && this.mLauncher.getDragMgr().getDragObject().extraDragInfoList != null && this.mMultiSelectPanel.acceptDropToFolder();
    }

    void showHelpDialog(boolean animate) {
        Log.d(TAG, "showHelpDialog");
        if (this.mMultiSelectHelpDialog != null && this.mCheckedAppsViewList.size() > 0) {
            this.mMultiSelectHelpDialog.show((View) this.mCheckedAppsViewList.get(0), animate);
        }
    }

    public void hideHelpDialog(boolean animate) {
        Log.d(TAG, "hideHelpDialog");
        if (this.mMultiSelectHelpDialog != null) {
            this.mMultiSelectHelpDialog.hide(animate);
        }
    }

    public boolean isShowingHelpDialog() {
        return this.mMultiSelectHelpDialog != null && this.mMultiSelectHelpDialog.isShowingHelpDialog();
    }

    public boolean handleTouchDown(MotionEvent ev) {
        return this.mMultiSelectHelpDialog.handleTouchDown(ev);
    }

    void setEnableHelpDialog(boolean enable) {
        Editor editor = this.mLauncher.getSharedPrefs().edit();
        editor.putBoolean(LauncherFiles.MULTI_SELECT_HELP_KEY, enable);
        editor.apply();
    }

    private boolean enableHelpDialog() {
        return this.mLauncher.getSharedPrefs().getBoolean(LauncherFiles.MULTI_SELECT_HELP_KEY, true);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (this.mCurrentOrientation != newConfig.orientation || this.mCurrentMobileKeyboard != newConfig.semMobileKeyboardCovered) {
            this.mCurrentMobileKeyboard = newConfig.semMobileKeyboardCovered;
            this.mCurrentOrientation = newConfig.orientation;
            this.mMultiSelectPanel.onConfigurationChangedIfNeeded();
            if (this.mMultiSelectHelpDialog != null && this.mMultiSelectHelpDialog.isShowingHelpDialog()) {
                hideHelpDialog(false);
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        MultiSelectManager.this.showHelpDialog(false);
                    }
                }, 300);
            }
        }
    }

    private void disableApp(IconInfo iconInfo, ComponentName compName) {
        String pkgName = compName.getPackageName();
        if (DualAppUtils.supportDualApp(this.mLauncher) && (DualAppUtils.isDualApp(iconInfo.user, pkgName) || DualAppUtils.hasDualApp(iconInfo.user, pkgName))) {
            DualAppUtils.uninstallOrDisableDualApp(this.mLauncher, pkgName, iconInfo.user);
            return;
        }
        ApplicationInfo appInfo = null;
        Drawable icon = null;
        PackageManager pm = this.mLauncher.getPackageManager();
        if (pm != null) {
            try {
                appInfo = pm.getApplicationInfo(pkgName, 0);
                icon = pm.getActivityIcon(compName);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "NameNotFoundException : " + e.toString());
            }
            Runnable r = new Runnable() {
                public void run() {
                    MultiSelectManager.this.postUninstallActivity();
                }
            };
            if (appInfo != null) {
                if (icon == null) {
                    icon = pm.getApplicationIcon(appInfo);
                }
                DisableAppConfirmationDialog.createAndShow(this.mLauncher, iconInfo.user, pkgName, iconInfo.title.toString(), icon, this.mLauncher.getFragmentManager(), r);
                return;
            }
            new Handler().post(r);
        }
    }

    public void updateMultiSelectPanelLayout() {
        if (this.mMultiSelectMode) {
            this.mMultiSelectPanel.updateMultiSelectPanelLayout();
        }
    }
}

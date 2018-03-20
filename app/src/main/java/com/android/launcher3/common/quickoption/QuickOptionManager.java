package com.android.launcher3.common.quickoption;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.controller.ControllerBase;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.dialog.DisableAppConfirmationDialog;
import com.android.launcher3.common.dialog.FolderDeleteDialog;
import com.android.launcher3.common.dialog.SleepAppConfirmationDialog;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.common.stage.StageEntry;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.controller.FolderController;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.home.HomeController;
import com.android.launcher3.home.LauncherAppWidgetInfo;
import com.android.launcher3.home.ZeroPageController;
import com.android.launcher3.util.AppFreezerUtils;
import com.android.launcher3.util.DualAppUtils;
import com.android.launcher3.util.SecureFolderHelper;
import com.android.launcher3.util.logging.GSIMLogging;
import com.android.launcher3.util.logging.SALogging;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import com.samsung.android.knox.SemPersonaManager;
import com.sec.android.app.launcher.BuildConfig;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class QuickOptionManager {
    public static final int OPTION_ADD_APPS = 2048;
    public static final int OPTION_ADD_TO_HOME = 4;
    public static final int OPTION_ADD_TO_PERSONAL = 16384;
    public static final int OPTION_APP_INFO = 1024;
    public static final int OPTION_CLEAR_BADGE = 32;
    public static final int OPTION_DELETE_FOLDER = 8;
    public static final int OPTION_DISABLE = 512;
    public static final int OPTION_INSTALL_DUAL_IM = 32768;
    public static final int OPTION_LOCK = 4096;
    public static final int OPTION_MOVE_FROM_FOLDER = 16;
    public static final int OPTION_PUT_TO_SLEEP = 128;
    public static final int OPTION_REMOVE = 2;
    public static final int OPTION_SECURE_FOLDER = 64;
    public static final int OPTION_SELECT = 1;
    public static final int OPTION_SET_TO_ZEROPAGE = 65536;
    public static final int OPTION_UNINSTALL = 256;
    public static final int OPTION_UNLOCK = 8192;
    private static final String TAG = "QuickOptionManager";
    private Rect mAnchorRect;
    private View mAnchorView;
    private ItemBounceAnimation mBounceAnimation;
    private Stage mController;
    private FolderLock mFolderLock;
    private ItemRemoveAnimation mItemRemoveAnimation;
    private final Launcher mLauncher;
    private boolean mOptionOfHomeItem;

    public QuickOptionManager(Launcher launcher) {
        this.mLauncher = launcher;
        if (LauncherFeature.supportFolderLock()) {
            this.mFolderLock = FolderLock.getInstance();
        }
    }

    List<QuickOptionListItem> getOptions(DragObject dragObject, int flags) {
        boolean isSupportNotiPreview = LauncherFeature.supportNotificationPreview();
        this.mController = dragObject.dragSource.getController();
        ItemInfo itemInfo = dragObject.dragInfo;
        this.mOptionOfHomeItem = this.mController instanceof HomeController;
        ComponentName componentName = null;
        String packageName = null;
        if ((itemInfo instanceof IconInfo) && !((IconInfo) itemInfo).isPromise()) {
            componentName = ((IconInfo) itemInfo).getTargetComponent();
        } else if (itemInfo instanceof LauncherAppWidgetInfo) {
            componentName = ((LauncherAppWidgetInfo) itemInfo).providerName;
        }
        if (componentName != null) {
            packageName = componentName.getPackageName();
        }
        List<QuickOptionListItem> options = new ArrayList();
        if ((flags & 1) != 0) {
            options.add(getOptionSelect(itemInfo));
        }
        if ((flags & 2) != 0) {
            options.add(getOptionRemove(itemInfo));
        }
        if ((flags & 4) != 0) {
            options.add(getOptionAddToHome(itemInfo));
        }
        if ((flags & 8) != 0 && (itemInfo instanceof FolderInfo)) {
            options.add(getOptionDeleteFolder((FolderInfo) itemInfo));
        }
        if (!isSupportNotiPreview) {
            if ((flags & 16) != 0 && (itemInfo instanceof IconInfo)) {
                options.add(getOptionMoveFromFolder((IconInfo) itemInfo));
            }
            if (!(LauncherFeature.supportNotificationBadge() || (flags & 32) == 0)) {
                options.add(getOptionClearBadge(itemInfo, componentName));
            }
            if ((flags & 64) != 0) {
                options.add(getOptionSecureFolder(itemInfo.user, packageName));
            }
            if ((flags & 128) != 0) {
                options.add(getOptionPutToSleep(itemInfo.user, packageName));
            }
        }
        if ((flags & 256) != 0 && (itemInfo instanceof IconInfo)) {
            options.add(getOptionUninstall((IconInfo) itemInfo, componentName));
        }
        if ((flags & 512) != 0 && (itemInfo instanceof IconInfo)) {
            options.add(getOptionDisable((IconInfo) itemInfo, componentName));
        }
        if (!isSupportNotiPreview) {
            if (canShowAppInfo(itemInfo) && (flags & 1024) != 0) {
                options.add(getOptionAppInfo(itemInfo.user, componentName));
            }
            if ((flags & 2048) != 0 && (itemInfo instanceof FolderInfo)) {
                options.add(getOptionAddApps((FolderInfo) itemInfo));
            }
            if ((flags & 4096) != 0) {
                options.add(getOptionLock(itemInfo));
            }
            if ((flags & 8192) != 0) {
                options.add(getOptionUnlock(itemInfo));
            }
            if ((flags & 16384) != 0 && (itemInfo instanceof IconInfo)) {
                options.add(getOptionAddToPersonal((IconInfo) itemInfo, componentName));
            }
            if ((32768 & flags) != 0) {
                options.add(getOptionInstallDualIM(itemInfo.user, packageName));
            }
        }
        if (LauncherFeature.supportSetToZeroPage() && (65536 & flags) != 0) {
            options.add(getOptionSetToZeroPage(itemInfo));
        }
        options.removeAll(Collections.singleton(null));
        return options;
    }

    private QuickOptionListItem getOptionSelect(final ItemInfo itemInfo) {
        if (!(itemInfo instanceof IconInfo) && !(itemInfo instanceof FolderInfo)) {
            return null;
        }
        QuickOptionListItem select = new QuickOptionListItem();
        select.setIconRsrId(R.drawable.quick_ic_select);
        select.setTitleRsrId(R.string.quick_option_select);
        select.setTtsTitleRsrId(R.string.tts_quick_option_select_items);
        select.setCallback(new Runnable() {
            public void run() {
                itemInfo.setChecked(true);
                QuickOptionManager.this.mLauncher.onChangeSelectMode(true, true);
                if (QuickOptionManager.this.mAnchorView != null) {
                    QuickOptionManager.this.mAnchorView.performAccessibilityAction(64, null);
                    GSIMLogging.getInstance().insertLogging(QuickOptionManager.this.mOptionOfHomeItem ? GSIMLogging.FEATURE_NAME_HOME_QUICK_OPTION : GSIMLogging.FEATURE_NAME_APPS_QUICK_OPTION, "Select multiple items", -1, false);
                }
            }
        });
        return select;
    }

    private QuickOptionListItem getOptionRemove(final ItemInfo itemInfo) {
        if (!isRemovable(itemInfo)) {
            return null;
        }
        QuickOptionListItem remove = new QuickOptionListItem();
        remove.setIconRsrId(R.drawable.quick_ic_remove);
        if (itemInfo.itemType == 1 || itemInfo.itemType == 6 || itemInfo.itemType == 0 || itemInfo.itemType == 7) {
            remove.setTitleRsrId(R.string.quick_option_remove_shortcut);
            remove.setTtsTitleRsrId(R.string.tts_quick_option_remove_from_home);
        } else if (itemInfo.itemType == 4 || itemInfo.itemType == 5) {
            remove.setTitleRsrId(R.string.quick_option_remove_from_home_screen);
            remove.setTtsTitleRsrId(R.string.tts_quick_option_remove_from_home_screen);
        } else if (itemInfo.itemType == 2) {
            remove.setTitleRsrId(R.string.quick_option_delete_folder);
            remove.setTtsTitleRsrId(R.string.tts_quick_option_delete_folder);
        } else {
            remove.setTitleRsrId(R.string.quick_option_remove);
        }
        remove.setCallback(new Runnable() {
            final View anchorView = QuickOptionManager.this.mAnchorView;

            public void run() {
                if (QuickOptionManager.this.mController instanceof HomeController) {
                    HomeController homeController = (HomeController) QuickOptionManager.this.mController;
                    if (homeController.isItemInFolder(itemInfo)) {
                        homeController.removeHomeItem(itemInfo);
                        homeController.deleteItemFromDb(itemInfo);
                    } else {
                        if ((itemInfo instanceof LauncherAppWidgetInfo) && homeController.getState() == 3) {
                            homeController.exitResizeState(false);
                        }
                        homeController.removeHomeOrFolderItem(itemInfo, this.anchorView);
                    }
                    GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_HOME_QUICK_OPTION, "Remove", -1, false);
                }
            }
        });
        return remove;
    }

    private QuickOptionListItem getOptionClearBadge(final ItemInfo itemInfo, final ComponentName componentName) {
        if (itemInfo.mBadgeCount == 0) {
            return null;
        }
        QuickOptionListItem clearBadge = new QuickOptionListItem();
        clearBadge.setIconRsrId(R.drawable.quick_ic_clear_badge);
        clearBadge.setTitleRsrId(R.string.quick_option_clear_badge);
        clearBadge.setCallback(new Runnable() {
            public void run() {
                if (itemInfo instanceof IconInfo) {
                    Utilities.clearBadge(QuickOptionManager.this.mLauncher, componentName, itemInfo.user);
                } else if (itemInfo instanceof FolderInfo) {
                    Iterator it = itemInfo.contents.iterator();
                    while (it.hasNext()) {
                        IconInfo iconInfo = (IconInfo) it.next();
                        if (iconInfo.mBadgeCount != 0) {
                            Utilities.clearBadge(QuickOptionManager.this.mLauncher, iconInfo.getTargetComponent(), iconInfo.user);
                        }
                    }
                }
                GSIMLogging.getInstance().insertLogging(QuickOptionManager.this.mOptionOfHomeItem ? GSIMLogging.FEATURE_NAME_HOME_QUICK_OPTION : GSIMLogging.FEATURE_NAME_APPS_QUICK_OPTION, "Clear Badge", -1, false);
            }
        });
        return clearBadge;
    }

    private QuickOptionListItem getOptionSecureFolder(UserHandleCompat user, final String packageName) {
        if (packageName == null || !SecureFolderHelper.canAddAppToSecureFolder(this.mLauncher, user, packageName)) {
            return null;
        }
        QuickOptionListItem addToSecureFolder = new QuickOptionListItem();
        addToSecureFolder.setIconRsrId(R.drawable.quick_ic_add_to_secure_folder);
        addToSecureFolder.setTitleRsrId(R.string.quick_option_secure_folder);
        addToSecureFolder.setCallback(new Runnable() {
            public void run() {
                SecureFolderHelper.addAppToSecureFolder(QuickOptionManager.this.mLauncher, packageName);
                GSIMLogging.getInstance().insertLogging(QuickOptionManager.this.mOptionOfHomeItem ? GSIMLogging.FEATURE_NAME_HOME_QUICK_OPTION : GSIMLogging.FEATURE_NAME_APPS_QUICK_OPTION, "Add to Secure Folder", -1, false);
            }
        });
        return addToSecureFolder;
    }

    private QuickOptionListItem getOptionPutToSleep(final UserHandleCompat user, final String packageName) {
        if (packageName != null) {
            if (AppFreezerUtils.canPutIntoSleepMode(this.mLauncher, user, packageName)) {
                QuickOptionListItem putToSleep = new QuickOptionListItem();
                putToSleep.setIconRsrId(R.drawable.quick_ic_sleep_now);
                putToSleep.setTitleRsrId(R.string.quick_option_sleep);
                putToSleep.setCallback(new Runnable() {
                    public void run() {
                        SleepAppConfirmationDialog.createAndShow(QuickOptionManager.this.mLauncher, user, packageName);
                        GSIMLogging.getInstance().insertLogging(QuickOptionManager.this.mOptionOfHomeItem ? GSIMLogging.FEATURE_NAME_HOME_QUICK_OPTION : GSIMLogging.FEATURE_NAME_APPS_QUICK_OPTION, "Sleep", -1, false);
                    }
                });
                return putToSleep;
            }
            Log.i(TAG, "Unable to put into sleep this app : " + packageName);
        }
        return null;
    }

    private QuickOptionListItem getOptionUninstall(final IconInfo iconInfo, final ComponentName componentName) {
        if (componentName != null) {
            final String packageName = componentName.getPackageName();
            if (Utilities.canUninstall(this.mLauncher, packageName)) {
                QuickOptionListItem uninstall = new QuickOptionListItem();
                uninstall.setIconRsrId(R.drawable.quick_ic_uninstall);
                uninstall.setTitleRsrId(R.string.quick_option_uninstall);
                uninstall.setTtsTitleRsrId(R.string.tts_quick_option_uninstall);
                uninstall.setCallback(new Runnable() {
                    public void run() {
                        if (DualAppUtils.supportDualApp(QuickOptionManager.this.mLauncher) && (DualAppUtils.isDualApp(iconInfo.user, packageName) || DualAppUtils.hasDualApp(iconInfo.user, packageName))) {
                            DualAppUtils.uninstallOrDisableDualApp(QuickOptionManager.this.mLauncher, packageName, iconInfo.user);
                        } else {
                            QuickOptionManager.this.mLauncher.startApplicationUninstallActivity(componentName, iconInfo.flags, iconInfo.user, true);
                        }
                        GSIMLogging.getInstance().insertLogging(QuickOptionManager.this.mOptionOfHomeItem ? GSIMLogging.FEATURE_NAME_HOME_QUICK_OPTION : GSIMLogging.FEATURE_NAME_APPS_QUICK_OPTION, "Uninstall", -1, false);
                    }
                });
                return uninstall;
            }
        }
        return null;
    }

    private QuickOptionListItem getOptionDisable(final IconInfo iconInfo, final ComponentName componentName) {
        if (componentName != null) {
            final String packageName = componentName.getPackageName();
            if (Utilities.canDisable(this.mLauncher, packageName)) {
                QuickOptionListItem disable = new QuickOptionListItem();
                disable.setIconRsrId(R.drawable.quick_ic_disable);
                disable.setTitleRsrId(R.string.quick_option_disable);
                disable.setTtsTitleRsrId(R.string.tts_quick_option_disable);
                disable.setCallback(new Runnable() {
                    public void run() {
                        if (DualAppUtils.supportDualApp(QuickOptionManager.this.mLauncher) && (DualAppUtils.isDualApp(iconInfo.user, packageName) || DualAppUtils.hasDualApp(iconInfo.user, packageName))) {
                            DualAppUtils.uninstallOrDisableDualApp(QuickOptionManager.this.mLauncher, packageName, iconInfo.user);
                            return;
                        }
                        ApplicationInfo appInfo = null;
                        Drawable icon = null;
                        PackageManager pm = QuickOptionManager.this.mLauncher.getPackageManager();
                        if (pm != null) {
                            try {
                                appInfo = pm.getApplicationInfo(packageName, 0);
                                icon = pm.getActivityIcon(componentName);
                            } catch (NameNotFoundException e) {
                                Log.e(QuickOptionManager.TAG, "NameNotFoundException : " + e.toString());
                            }
                            if (appInfo != null) {
                                String str;
                                if (icon == null) {
                                    icon = pm.getApplicationIcon(appInfo);
                                }
                                DisableAppConfirmationDialog.createAndShow(QuickOptionManager.this.mLauncher, iconInfo.user, packageName, iconInfo.title.toString(), icon, QuickOptionManager.this.mLauncher.getFragmentManager(), null);
                                GSIMLogging instance = GSIMLogging.getInstance();
                                if (QuickOptionManager.this.mOptionOfHomeItem) {
                                    str = GSIMLogging.FEATURE_NAME_HOME_QUICK_OPTION;
                                } else {
                                    str = GSIMLogging.FEATURE_NAME_APPS_QUICK_OPTION;
                                }
                                instance.insertLogging(str, "Disable", -1, false);
                            }
                        }
                    }
                });
                return disable;
            } else if (!Utilities.canUninstall(this.mLauncher, packageName)) {
                QuickOptionListItem dimmedDisable = new QuickOptionListItem();
                dimmedDisable.setIconRsrId(R.drawable.quick_ic_disable);
                dimmedDisable.setTitleRsrId(R.string.quick_option_dimmed_disable);
                dimmedDisable.setTtsTitleRsrId(R.string.tts_quick_option_disable);
                dimmedDisable.setCallback(new Runnable() {
                    public void run() {
                        Toast.makeText(QuickOptionManager.this.mLauncher, String.format(QuickOptionManager.this.mLauncher.getString(R.string.quick_option_cant_disable), new Object[]{iconInfo.title.toString()}), 0).show();
                    }
                });
                return dimmedDisable;
            }
        }
        return null;
    }

    private QuickOptionListItem getOptionAppInfo(final UserHandleCompat user, final ComponentName componentName) {
        if (componentName == null) {
            return null;
        }
        QuickOptionListItem appInfo = new QuickOptionListItem();
        appInfo.setIconRsrId(R.drawable.quick_ic_app_info);
        appInfo.setTitleRsrId(R.string.quick_option_app_info);
        appInfo.setCallback(new Runnable() {
            public void run() {
                LauncherAppsCompat.getInstance(QuickOptionManager.this.mLauncher).showAppDetailsForProfile(componentName, user);
                GSIMLogging.getInstance().insertLogging(QuickOptionManager.this.mOptionOfHomeItem ? GSIMLogging.FEATURE_NAME_HOME_QUICK_OPTION : GSIMLogging.FEATURE_NAME_APPS_QUICK_OPTION, "App info", -1, false);
            }
        });
        return appInfo;
    }

    private QuickOptionListItem getOptionDeleteFolder(final FolderInfo folderInfo) {
        QuickOptionListItem removeFolder = new QuickOptionListItem();
        removeFolder.setIconRsrId(R.drawable.quick_ic_remove);
        removeFolder.setTitleRsrId(R.string.quick_option_delete_folder);
        removeFolder.setTtsTitleRsrId(R.string.tts_quick_option_delete_folder);
        removeFolder.setCallback(new Runnable() {
            public void run() {
                new FolderDeleteDialog().show(QuickOptionManager.this.mLauncher.getFragmentManager(), QuickOptionManager.this.mController, folderInfo);
                GSIMLogging.getInstance().insertLogging(QuickOptionManager.this.mOptionOfHomeItem ? GSIMLogging.FEATURE_NAME_HOME_QUICK_OPTION : GSIMLogging.FEATURE_NAME_APPS_QUICK_OPTION, "Delete folder", -1, false);
            }
        });
        return removeFolder;
    }

    private QuickOptionListItem getOptionAddApps(FolderInfo folderInfo) {
        if (this.mFolderLock != null && !this.mFolderLock.canShowAddAppsOptions(folderInfo)) {
            return null;
        }
        QuickOptionListItem addApps = new QuickOptionListItem();
        addApps.setIconRsrId(R.drawable.quick_ic_add_apps);
        addApps.setTitleRsrId(R.string.quick_option_add_apps);
        if (!(this.mAnchorView instanceof FolderIconView)) {
            return null;
        }
        addApps.setCallback(new Runnable() {
            public void run() {
                StageEntry data = new StageEntry();
                data.putExtras(FolderController.KEY_FOLDER_ICON_VIEW, QuickOptionManager.this.mAnchorView);
                QuickOptionManager.this.mLauncher.getStageManager().startStage(6, data);
                GSIMLogging.getInstance().insertLogging(QuickOptionManager.this.mOptionOfHomeItem ? GSIMLogging.FEATURE_NAME_HOME_QUICK_OPTION : GSIMLogging.FEATURE_NAME_APPS_QUICK_OPTION, "Add Apps", -1, false);
            }
        });
        return addApps;
    }

    private QuickOptionListItem getOptionLock(final ItemInfo itemInfo) {
        if (itemInfo == null) {
            return null;
        }
        final FolderLock folderLock = FolderLock.getInstance();
        if (!folderLock.canShowLockOptions(itemInfo)) {
            return null;
        }
        QuickOptionListItem lock = new QuickOptionListItem();
        lock.setIconRsrId(R.drawable.quick_ic_lock);
        lock.setTitleRsrId(R.string.quick_option_lock);
        lock.setCallback(new Runnable() {
            public void run() {
                int i = 1;
                folderLock.setBackupInfo(itemInfo);
                folderLock.startLockVerifyActivity(itemInfo);
                String screenID = "";
                int stageMode = QuickOptionManager.this.mLauncher.getTopStageMode();
                if (stageMode == 1) {
                    screenID = QuickOptionManager.this.mLauncher.getResources().getString(R.string.screen_Home_1xxx);
                } else if (stageMode == 2) {
                    screenID = QuickOptionManager.this.mLauncher.getResources().getString(R.string.screen_Apps_2xxx);
                } else if (stageMode == 5) {
                    int secondTopStage = QuickOptionManager.this.mLauncher.getSecondTopStageMode();
                    if (secondTopStage == 1) {
                        screenID = QuickOptionManager.this.mLauncher.getResources().getString(R.string.screen_HomeFolder_Primary);
                    } else if (secondTopStage == 2) {
                        screenID = QuickOptionManager.this.mLauncher.getResources().getString(R.string.screen_AppsFolder_Primary);
                    }
                }
                String title = "";
                if (itemInfo.title != null) {
                    title = itemInfo.title.toString();
                }
                SALogging instance = SALogging.getInstance();
                String string = QuickOptionManager.this.mLauncher.getResources().getString(R.string.event_AppLockHome_Lock);
                StringBuilder stringBuilder = new StringBuilder();
                if (!itemInfo.isAppOrShortcutType()) {
                    i = 0;
                }
                instance.insertEventLog(screenID, string, stringBuilder.append(i).append(" ").append(title).toString());
            }
        });
        return lock;
    }

    private QuickOptionListItem getOptionUnlock(final ItemInfo itemInfo) {
        if (itemInfo == null) {
            return null;
        }
        final FolderLock folderLock = FolderLock.getInstance();
        if (!folderLock.canShowUnlockOptions(itemInfo)) {
            return null;
        }
        QuickOptionListItem unlock = new QuickOptionListItem();
        unlock.setIconRsrId(R.drawable.quick_ic_unlock);
        unlock.setTitleRsrId(R.string.quick_option_unlock);
        unlock.setCallback(new Runnable() {
            public void run() {
                int i = 1;
                folderLock.setBackupInfo(itemInfo);
                folderLock.startUnlockVerifyActivity(itemInfo);
                String screenID = "";
                int stageMode = QuickOptionManager.this.mLauncher.getTopStageMode();
                if (stageMode == 1) {
                    screenID = QuickOptionManager.this.mLauncher.getResources().getString(R.string.screen_Home_1xxx);
                } else if (stageMode == 2) {
                    screenID = QuickOptionManager.this.mLauncher.getResources().getString(R.string.screen_Apps_2xxx);
                } else if (stageMode == 5) {
                    int secondTopStage = QuickOptionManager.this.mLauncher.getSecondTopStageMode();
                    if (secondTopStage == 1) {
                        screenID = QuickOptionManager.this.mLauncher.getResources().getString(R.string.screen_HomeFolder_Primary);
                    } else if (secondTopStage == 2) {
                        screenID = QuickOptionManager.this.mLauncher.getResources().getString(R.string.screen_AppsFolder_Primary);
                    }
                }
                String title = "";
                if (itemInfo.title != null) {
                    title = itemInfo.title.toString();
                }
                SALogging instance = SALogging.getInstance();
                String string = QuickOptionManager.this.mLauncher.getResources().getString(R.string.event_AppLockHome_UnLock);
                StringBuilder stringBuilder = new StringBuilder();
                if (!itemInfo.isAppOrShortcutType()) {
                    i = 0;
                }
                instance.insertEventLog(screenID, string, stringBuilder.append(i).append(" ").append(title).toString());
            }
        });
        return unlock;
    }

    private QuickOptionListItem getOptionAddToPersonal(final IconInfo iconInfo, final ComponentName componentName) {
        if (!(!Utilities.isKnoxMode() || SemPersonaManager.isKioskModeEnabled(this.mLauncher.getApplicationContext()) || componentName == null)) {
            final String packageName = componentName.getPackageName();
            if (SemPersonaManager.isPossibleAddToPersonal(packageName)) {
                QuickOptionListItem addToPersonal = new QuickOptionListItem();
                addToPersonal.setIconRsrId(R.drawable.quick_ic_add_to_personal);
                addToPersonal.setTitleRsrId(R.string.quick_option_add_to_personal);
                addToPersonal.setCallback(new Runnable() {
                    public void run() {
                        Utilities.addToPersonal(QuickOptionManager.this.mLauncher, packageName, componentName, iconInfo.title.toString(), iconInfo.mIcon);
                        GSIMLogging.getInstance().insertLogging(QuickOptionManager.this.mOptionOfHomeItem ? GSIMLogging.FEATURE_NAME_HOME_QUICK_OPTION : GSIMLogging.FEATURE_NAME_APPS_QUICK_OPTION, "Add to personal", -1, false);
                    }
                });
                return addToPersonal;
            }
        }
        return null;
    }

    private QuickOptionListItem getOptionInstallDualIM(UserHandleCompat user, final String packageName) {
        if (!DualAppUtils.canInstallDualApp(this.mLauncher, user, packageName)) {
            return null;
        }
        QuickOptionListItem installDualIM = new QuickOptionListItem();
        installDualIM.setIconRsrId(R.drawable.quick_ic_install_second_app);
        installDualIM.setTitleRsrId(R.string.quick_option_install_dual_im);
        installDualIM.setCallback(new Runnable() {
            public void run() {
                DualAppUtils.installDualApp(QuickOptionManager.this.mLauncher, packageName);
                GSIMLogging.getInstance().insertLogging(QuickOptionManager.this.mOptionOfHomeItem ? GSIMLogging.FEATURE_NAME_HOME_QUICK_OPTION : GSIMLogging.FEATURE_NAME_APPS_QUICK_OPTION, "Install second app", -1, false);
            }
        });
        return installDualIM;
    }

    private QuickOptionListItem getOptionAddToHome(final ItemInfo itemInfo) {
        QuickOptionListItem addToHome = new QuickOptionListItem();
        addToHome.setIconRsrId(R.drawable.quick_ic_add_shortcut_to_home);
        addToHome.setTitleRsrId(R.string.quick_option_add_shortcut_to_home);
        addToHome.setTtsTitleRsrId(R.string.tts_quick_option_add_to_home);
        addToHome.setCallback(new Runnable() {
            public void run() {
                QuickOptionManager.this.mLauncher.getHomeController().addShortcutToHome(itemInfo);
                QuickOptionManager.this.mLauncher.getStageManager().finishAllStage(null);
                QuickOptionManager.this.mLauncher.getStageManager().startStage(1, null);
                GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_APPS_QUICK_OPTION, "Add shortcut to home", -1, false);
            }
        });
        return addToHome;
    }

    private QuickOptionListItem getOptionMoveFromFolder(final IconInfo iconInfo) {
        QuickOptionListItem takeOutOfFolder = new QuickOptionListItem();
        takeOutOfFolder.setIconRsrId(R.drawable.quick_ic_move_to_apps);
        takeOutOfFolder.setTitleRsrId(R.string.quick_option_move_from_folder);
        takeOutOfFolder.setCallback(new Runnable() {
            public void run() {
                ((ControllerBase) QuickOptionManager.this.mController).moveItemFromFolder(iconInfo);
                GSIMLogging.getInstance().insertLogging(QuickOptionManager.this.mOptionOfHomeItem ? GSIMLogging.FEATURE_NAME_HOME_QUICK_OPTION : GSIMLogging.FEATURE_NAME_APPS_QUICK_OPTION, "Move from folder", -1, false);
            }
        });
        return takeOutOfFolder;
    }

    private QuickOptionListItem getOptionSetToZeroPage(final ItemInfo itemInfo) {
        if (!(itemInfo instanceof IconInfo)) {
            return null;
        }
        QuickOptionListItem setToZeroPage = new QuickOptionListItem();
        setToZeroPage.setIconRsrId(R.drawable.quick_ic_add_apps);
        setToZeroPage.setTitleRsrId(R.string.quick_option_set_to_zeropage);
        setToZeroPage.setCallback(new Runnable() {
            public void run() {
                String packageName = itemInfo.getIntent().getComponent().getPackageName();
                String className = itemInfo.getIntent().getComponent().getClassName();
                Intent intent = new Intent();
                intent.setAction(ZeroPageController.ACTION_INTENT_SET_ZEROPAGE);
                intent.setPackage(BuildConfig.APPLICATION_ID);
                intent.putExtra("zeroapp_package_name", packageName);
                intent.putExtra("zeroapp_class_name", className);
                QuickOptionManager.this.mLauncher.sendBroadcast(intent);
            }
        });
        return setToZeroPage;
    }

    private boolean isRemovable(ItemInfo itemInfo) {
        if ((itemInfo != null && itemInfo.itemType == 1) || itemInfo.itemType == 6 || itemInfo.itemType == 4 || itemInfo.itemType == 5 || itemInfo.itemType == 7) {
            return true;
        }
        if (!LauncherAppState.getInstance().isHomeOnlyModeEnabled() && (itemInfo.itemType == 2 || itemInfo.itemType == 0)) {
            return true;
        }
        if (LauncherAppState.getInstance().isHomeOnlyModeEnabled() && isInWidgetItemsList(itemInfo)) {
            return true;
        }
        return false;
    }

    private boolean isInWidgetItemsList(ItemInfo itemInfo) {
        try {
            if ((itemInfo instanceof IconInfo) && itemInfo.itemType == 1) {
                String className = ((IconInfo) itemInfo).getTargetComponent().getClassName();
                for (Object o : this.mLauncher.getLauncherModel().getWidgetsLoader().getWidgetItems()) {
                    if (o instanceof ArrayList) {
                        Iterator it = ((ArrayList) o).iterator();
                        while (it.hasNext()) {
                            ItemInfo widget = (ItemInfo) it.next();
                            if (widget instanceof PendingAddShortcutInfo) {
                                ActivityInfo activityInfo = ((PendingAddShortcutInfo) widget).getActivityInfo();
                                if (className.equals(activityInfo.targetActivity == null ? activityInfo.name : activityInfo.targetActivity)) {
                                    return true;
                                }
                            }
                        }
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    View getAnchorView() {
        return this.mAnchorView;
    }

    public void setAnchorView(View v) {
        this.mAnchorView = v;
    }

    Rect getAnchorRect() {
        return this.mAnchorRect;
    }

    public void setAnchorRect(Rect r) {
        this.mAnchorRect = r;
    }

    ItemRemoveAnimation createItemRemoveAnimation() {
        if (this.mAnchorView == null) {
            return null;
        }
        this.mItemRemoveAnimation = new ItemRemoveAnimation(this.mAnchorView);
        return this.mItemRemoveAnimation;
    }

    ItemRemoveAnimation getItemRemoveAnimation() {
        return this.mItemRemoveAnimation;
    }

    void clearItemRemoveAnimation() {
        this.mItemRemoveAnimation = null;
    }

    public void startBounceAnimation() {
        if (this.mAnchorView != null) {
            this.mBounceAnimation = new ItemBounceAnimation(this.mAnchorView, needSmallBounceAnimation());
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    if (QuickOptionManager.this.mBounceAnimation != null) {
                        QuickOptionManager.this.mBounceAnimation.animate();
                    }
                }
            }, 350);
        }
    }

    private boolean needSmallBounceAnimation() {
        DeviceProfile dp = this.mLauncher.getDeviceProfile();
        if (dp.isLandscape) {
            return true;
        }
        if (this.mLauncher.getTopStageMode() != 1 || dp.homeGrid.getCellCountY() <= 5) {
            return false;
        }
        return true;
    }

    void clearItemBounceAnimation() {
        if (this.mBounceAnimation != null) {
            this.mBounceAnimation.stop();
            this.mBounceAnimation = null;
        }
    }

    public void onDestroy() {
        if (this.mItemRemoveAnimation != null) {
            this.mItemRemoveAnimation.cancel();
        }
    }

    private boolean canShowAppInfo(ItemInfo itemInfo) {
        return itemInfo.itemType != 7;
    }
}

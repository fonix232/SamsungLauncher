package com.android.launcher3.remoteconfiguration;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Point;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherProviderChangeListener;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.compat.AppWidgetManagerCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens;
import com.android.launcher3.proxy.LauncherProxy;
import com.android.launcher3.util.ScreenGridUtilities;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;

import java.util.ArrayList;

public class RemoteConfigurationManager {
    private static final String GRANT_PERMISSION = "com.samsung.android.launcher.permission.WRITE_SETTINGS";
    private static final long MAX_WAIT_DURATION_FOR_WORK_THREAD = 1000;
    private static final String TAG = RemoteConfigurationManager.class.getSimpleName();
    private final LauncherAppState mAppstate;
    private Runnable mCompleteRunnableOnWorkThread;
    private final Object mConfigurationLock = new Object();
    private final Context mContext;
    private LauncherProviderChangeListener mListener;

    public RemoteConfigurationManager(Context context) {
        this.mContext = context;
        this.mAppstate = LauncherAppState.getInstance();
    }

    public void setLauncherProviderChangeListener(LauncherProviderChangeListener listener) {
        this.mListener = listener;
    }

    private boolean checkPermission() {
        if (Binder.getCallingUid() != Process.myUid()) {
            String callingPackageName = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            if (callingPackageName == null) {
                return false;
            }
            boolean hasGrantPermission;
            if (this.mContext.checkCallingPermission("com.samsung.android.launcher.permission.WRITE_SETTINGS") == PackageManager.PERMISSION_GRANTED) {
                hasGrantPermission = true;
            } else {
                hasGrantPermission = false;
            }
            if (!hasGrantPermission) {
                Log.d(TAG, "Not allowed package name : " + callingPackageName);
                return false;
            }
        }
        return true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public android.os.Bundle handleRemoteConfigurationCall(java.lang.String r7, java.lang.String r8, android.os.Bundle r9) {
        /*
        r6 = this;
        r5 = 1;
        r3 = 0;
        r4 = -1;
        r2 = r7.hashCode();
        switch(r2) {
            case -1662122150: goto L_0x003c;
            case -1220486243: goto L_0x0046;
            default: goto L_0x000a;
        };
    L_0x000a:
        r2 = r4;
    L_0x000b:
        switch(r2) {
            case 0: goto L_0x0050;
            case 1: goto L_0x0055;
            default: goto L_0x000e;
        };
    L_0x000e:
        r2 = 0;
        r6.mCompleteRunnableOnWorkThread = r2;
        r1 = r6.handleGetMethods(r7, r9);
        if (r1 != 0) goto L_0x0029;
    L_0x0017:
        r2 = r6.checkPermission();
        if (r2 != 0) goto L_0x0078;
    L_0x001d:
        r1 = new android.os.Bundle;
        r1.<init>();
        r2 = "invocation_result";
        r4 = -100;
        r1.putInt(r2, r4);
    L_0x0029:
        r2 = r6.waitForWorkThread();
        if (r2 != 0) goto L_0x008d;
    L_0x002f:
        r2 = TAG;
        r3 = "handleRemoteConfigurationCall : workThread is too busy";
        android.util.Log.w(r2, r3);
        r2 = "delay_result";
        r1.putBoolean(r2, r5);
    L_0x003b:
        return r1;
    L_0x003c:
        r2 = "get_home_mode";
        r2 = r7.equals(r2);
        if (r2 == 0) goto L_0x000a;
    L_0x0044:
        r2 = r3;
        goto L_0x000b;
    L_0x0046:
        r2 = "get_support_feature";
        r2 = r7.equals(r2);
        if (r2 == 0) goto L_0x000a;
    L_0x004e:
        r2 = r5;
        goto L_0x000b;
    L_0x0050:
        r1 = r6.getHomeMode(r8);
        goto L_0x003b;
    L_0x0055:
        r0 = "find_app_position";
        r1 = new android.os.Bundle;
        r1.<init>();
        r2 = r8.hashCode();
        switch(r2) {
            case 579038797: goto L_0x006e;
            default: goto L_0x0063;
        };
    L_0x0063:
        r2 = r4;
    L_0x0064:
        switch(r2) {
            case 0: goto L_0x0068;
            default: goto L_0x0067;
        };
    L_0x0067:
        goto L_0x000e;
    L_0x0068:
        r2 = "find_app_position";
        r1.putBoolean(r2, r5);
        goto L_0x003b;
    L_0x006e:
        r2 = "find_app_position";
        r2 = r8.equals(r2);
        if (r2 == 0) goto L_0x0063;
    L_0x0076:
        r2 = r3;
        goto L_0x0064;
    L_0x0078:
        r1 = r6.handleSetMethods(r7, r9);
        if (r1 != 0) goto L_0x0089;
    L_0x007e:
        r1 = new android.os.Bundle;
        r1.<init>();
        r2 = "invocation_result";
        r1.putInt(r2, r4);
        goto L_0x0029;
    L_0x0089:
        r6.setCompleteRunnableForWait();
        goto L_0x0029;
    L_0x008d:
        r2 = "delay_result";
        r1.putBoolean(r2, r3);
        goto L_0x003b;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.remoteconfiguration.RemoteConfigurationManager.handleRemoteConfigurationCall(java.lang.String, java.lang.String, android.os.Bundle):android.os.Bundle");
    }

    private boolean isCompleteOnWorkThread() {
        return this.mCompleteRunnableOnWorkThread == null;
    }

    private void setCompleteRunnableForWait() {
        this.mCompleteRunnableOnWorkThread = new Runnable() {
            public void run() {
                synchronized (RemoteConfigurationManager.this.mConfigurationLock) {
                    RemoteConfigurationManager.this.mCompleteRunnableOnWorkThread = null;
                    RemoteConfigurationManager.this.mConfigurationLock.notify();
                }
            }
        };
        LauncherModel.runOnWorkerThread(this.mCompleteRunnableOnWorkThread);
    }

    private boolean waitForWorkThread() {
        long waitStartTime = SystemClock.uptimeMillis();
        long waitingTime = 0;
        synchronized (this.mConfigurationLock) {
            if (!isCompleteOnWorkThread()) {
                try {
                    this.mConfigurationLock.wait(MAX_WAIT_DURATION_FOR_WORK_THREAD);
                    waitingTime = SystemClock.uptimeMillis() - waitStartTime;
                    if (waitingTime >= MAX_WAIT_DURATION_FOR_WORK_THREAD) {
                        return false;
                    }
                } catch (InterruptedException e) {
                    Log.w(TAG, "waitForWorkThread : e = " + e.toString());
                    return false;
                }
            }
            Log.d(TAG, "waitForWorkThread : waitingTime = " + waitingTime);
            return true;
        }
    }

    private Bundle handleGetMethods(String method, Bundle extras) {
        switch (method) {
            case "get_hotseat_item_count":
                return getHotseatItemCount();
            case "get_supplement_service_page_visibility":
                return getSupplementServicePageVisibility();
            case "get_hotseat_item":
                return getHotseatItem(extras);
            case "get_home_cell_dimension":
                return getHomeCellDimension();
            case "get_hotseat_maxitem_count":
                return getHotseatMaxItemCount();
            case "get_rotation_state":
                return getRotationState();
            case "get_apps_cell_dimension":
                return getAppsCellDimension();
            case "get_apps_button_state":
                return getAppsButtonState();
            default:
                return null;
        }
    }

    private Bundle handleSetMethods(String method, Bundle extras) {
        switch (method) {
            case "add_widget":
                return addWorkspaceItem(extras, true);
            case "remove_shortcut":
                return removeWorkspaceItem(extras, false);
            case "remove_page_from_home":
                return removePageFromHome(extras);
            case "add_hotseat_item":
                return addHotseatItem(extras);
            case "remove_hotseat_item":
                return removeHotseatItem(extras);
            case "set_supplement_service_page_visibility":
                return setSupplementServicePageVisibility(extras);
            case "make_empty_position":
                return makeEmptyPosition(extras);
            case "remove_widget":
                return removeWorkspaceItem(extras, true);
            case "switch_home_mode":
                return switchHomeMode(extras);
            case "add_shortcut":
                return addWorkspaceItem(extras, false);
            case "disable_apps_button":
                return setAppsButton(false, method);
            case "enable_apps_button":
                return setAppsButton(true, method);
            default:
                return null;
        }
    }

    private Bundle getHomeMode(String arg) {
        Log.d(TAG, "get_home_mode Called.");
        Bundle result = new Bundle();
        if (this.mAppstate.isEasyModeEnabled()) {
            result.putString(arg, "easy_mode");
        } else if (this.mAppstate.isHomeOnlyModeEnabled()) {
            result.putString(arg, LauncherAppState.HOME_ONLY_MODE);
        } else {
            result.putString(arg, LauncherAppState.HOME_APPS_MODE);
        }
        return result;
    }

    private Bundle switchHomeMode(Bundle param) {
        Log.v(TAG, "switchHomeMode");
        Bundle result = new Bundle();
        int resultValue = -4;
        if (param != null) {
            switch (param.getString("home_mode")) {
                case LauncherAppState.HOME_ONLY_MODE:
                case LauncherAppState.HOME_APPS_MODE:
                    this.mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit().putBoolean(Utilities.HOMESCREEN_MODE_PREFERENCE_KEY, param.getString("home_mode").equals(LauncherAppState.HOME_ONLY_MODE)).apply();
                    if (this.mListener != null) {
                        this.mListener.onSettingsChanged(Utilities.HOMESCREEN_MODE_PREFERENCE_KEY, true);
                    }
                    resultValue = 0;
                    break;
                default:
                    break;
            }
        }
        Log.w(TAG, "switchHomeMode : param is null");
        result.putInt("invocation_result", resultValue);
        return result;
    }

    private Bundle getHomeCellDimension() {
        Log.v(TAG, "getHomeCellDimension");
        Bundle result = new Bundle();
        int resultValue = 0;
        int[] grid = new int[2];
        ScreenGridUtilities.loadCurrentGridSize(this.mContext, grid, this.mAppstate.isHomeOnlyModeEnabled());
        if (grid[0] == -1 || grid[1] == -1) {
            resultValue = -2;
        } else {
            result.putInt("cols", grid[0]);
            result.putInt("rows", grid[1]);
        }
        result.putInt("invocation_result", resultValue);
        return result;
    }

    private Bundle getAppsCellDimension() {
        Log.v(TAG, "getAppsCellDimension");
        Bundle result = new Bundle();
        int resultValue = 0;
        int[] grid = new int[2];
        ScreenGridUtilities.loadCurrentAppsGridSize(this.mContext, grid);
        if (grid[0] == -1 || grid[1] == -1) {
            resultValue = -2;
        } else {
            result.putInt("cols", grid[0]);
            result.putInt("rows", grid[1]);
        }
        result.putInt("invocation_result", resultValue);
        return result;
    }

    private Bundle getHotseatItemCount() {
        Log.v(TAG, "getHotseatItemCount");
        Bundle result = new Bundle();
        result.putInt("itemcount", this.mAppstate.getModel().getHomeLoader().getHotseatItemCount());
        result.putInt("invocation_result", 0);
        return result;
    }

    private Bundle getHotseatMaxItemCount() {
        Log.v(TAG, "getHotseatMaxItemCount");
        Bundle result = new Bundle();
        result.putInt("itemcount", this.mAppstate.getDeviceProfile().getMaxHotseatCount());
        result.putInt("invocation_result", 0);
        return result;
    }

    private Bundle addHotseatItem(Bundle param) {
        Log.v(TAG, "addHotseatItem");
        Bundle result = new Bundle();
        int resultValue = 0;
        ComponentName cn = null;
        int index = -1;
        if (param == null) {
            Log.d(TAG, "addHotseatItem - param is null");
            resultValue = -4;
        } else {
            cn = (ComponentName) param.getParcelable("component");
            index = param.getInt("index");
            int maxCount = this.mAppstate.getDeviceProfile().getMaxHotseatCount();
            if (cn == null || index < 0 || index >= maxCount) {
                Log.d(TAG, "addHotseatItem - componentName is null, index is " + index);
                resultValue = -4;
            }
        }
        if (resultValue == 0 && !ExternalMethodQueue.queueExternalMethodInfo(new ExternalMethodInfo("add_hotseat_item", param, -1), this.mContext)) {
            this.mAppstate.getModel().getHomeLoader().addHotseatItemByComponentName(cn, index, UserHandleCompat.myUserHandle());
        }
        result.putInt("invocation_result", resultValue);
        return result;
    }

    private Bundle getHotseatItem(Bundle param) {
        Log.v(TAG, "getHotseatItem");
        Bundle result = new Bundle();
        int resultValue = 0;
        int index = -1;
        if (param == null) {
            Log.d(TAG, "getHotseatItem - param is null");
            resultValue = -4;
        } else {
            index = param.getInt("index");
            int maxCount = this.mAppstate.getDeviceProfile().getMaxHotseatCount();
            if (index < 0 || index >= maxCount) {
                Log.d(TAG, "getHotseatItem - index is " + index);
                resultValue = -4;
            }
        }
        if (resultValue == 0) {
            String[] projection = new String[]{"intent", BaseLauncherColumns.PROFILE_ID};
            String[] selectionArg = new String[]{String.valueOf(index), String.valueOf(Favorites.CONTAINER_HOTSEAT)};
            Cursor cursor = this.mContext.getContentResolver().query(Favorites.CONTENT_URI, projection, "screen=? AND container=?", selectionArg, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        String intentString = cursor.getString(0);
                        long profileId = cursor.getLong(1);
                        ComponentName cn = null;
                        if (intentString != null) {
                            cn = Intent.parseUri(intentString, 0).getComponent();
                        }
                        result.putParcelable("component", cn);
                        result.putLong("user_id", profileId);
                    } else {
                        resultValue = -3;
                    }
                    if (!cursor.isClosed()) {
                        cursor.close();
                    }
                } catch (Exception e) {
                    resultValue = -3;
                    Log.e(TAG, "Exception : " + e.toString());
                    if (!cursor.isClosed()) {
                        cursor.close();
                    }
                } catch (Throwable th) {
                    if (!cursor.isClosed()) {
                        cursor.close();
                    }
                }
            } else {
                resultValue = -3;
            }
        }
        result.putInt("invocation_result", resultValue);
        return result;
    }

    private Bundle removeHotseatItem(Bundle param) {
        Log.v(TAG, "removeHotseatItem");
        Bundle result = new Bundle();
        int resultValue = 0;
        int index = LauncherProxy.INVALID_VALUE;
        if (param == null) {
            Log.d(TAG, "removeHotseatItem - param is null");
            resultValue = -4;
        } else {
            index = param.getInt("index");
            int hotseatCount = this.mAppstate.getModel().getHomeLoader().getHotseatItemCount();
            if (index < 0 || index >= hotseatCount) {
                Log.d(TAG, "removeHotseatItem - index is " + index);
                resultValue = -3;
            }
        }
        if (resultValue == 0 && !ExternalMethodQueue.queueExternalMethodInfo(new ExternalMethodInfo("remove_hotseat_item", param, -1), this.mContext)) {
            this.mAppstate.getModel().getHomeLoader().removeHotseatItemByIndex(index);
        }
        result.putInt("invocation_result", resultValue);
        return result;
    }

    private Bundle addWorkspaceItem(Bundle param, boolean isWidget) {
        int resultValue;
        Log.v(TAG, "addWorkspaceItem");
        Bundle result = new Bundle();
        ComponentName cn = null;
        int spanY = LauncherProxy.INVALID_VALUE;
        int spanX = LauncherProxy.INVALID_VALUE;
        int cellY = LauncherProxy.INVALID_VALUE;
        int cellX = LauncherProxy.INVALID_VALUE;
        int page = LauncherProxy.INVALID_VALUE;
        boolean paramError = true;
        DeviceProfile dp = this.mAppstate.getDeviceProfile();
        if (param == null) {
            Log.w(TAG, "addWorkspaceItem : param is null");
        } else {
            cn = (ComponentName) param.getParcelable("component");
            if (cn == null) {
                Log.w(TAG, "addWorkspaceItem : componentName is null");
            } else {
                page = param.getInt("page", LauncherProxy.INVALID_VALUE);
                if (page == LauncherProxy.INVALID_VALUE) {
                    Log.w(TAG, "addWorkspaceItem : page index is INVALID");
                } else {
                    Point pos = (Point) param.getParcelable("coordination_position");
                    if (pos == null) {
                        Log.w(TAG, "addWorkspaceItem : position is null");
                    } else {
                        cellX = pos.x;
                        cellY = pos.y;
                        if (cellX >= dp.homeGrid.getCellCountX() || cellX < 0) {
                            Log.w(TAG, "addWorkspaceItem : cellX is " + cellX);
                        } else if (cellY >= dp.homeGrid.getCellCountY() || cellY < 0) {
                            Log.w(TAG, "addWorkspaceItem : cellY is " + cellY);
                        } else if (isWidget) {
                            Point size = (Point) param.getParcelable("coordination_size");
                            if (size == null) {
                                Log.w(TAG, "addWorkspaceItem : size is null");
                            } else {
                                makeAdjustedWidgetSize(cn, size);
                                spanX = size.x;
                                spanY = size.y;
                                if (cellX + spanX > dp.homeGrid.getCellCountX() || spanX < 1) {
                                    Log.w(TAG, "addWorkspaceItem : cellX is " + cellX + ", spanX is " + spanX + ". and it's out of a cell.");
                                } else if (cellY + spanY > dp.homeGrid.getCellCountY() || spanY < 1) {
                                    Log.w(TAG, "addWorkspaceItem : cellY is " + cellY + ", spanY is " + spanY + ". and it's out of a cell.");
                                } else {
                                    paramError = false;
                                }
                            }
                        } else {
                            paramError = false;
                        }
                    }
                }
            }
        }
        if (paramError) {
            resultValue = -4;
        } else if (isItemExist(cn, false, isWidget)) {
            if (!ExternalMethodQueue.queueExternalMethodInfo(new ExternalMethodInfo(isWidget ? "add_widget" : "add_shortcut", param, -1), this.mContext)) {
                this.mAppstate.getModel().getHomeLoader().addOrMoveItem(page, cellX, cellY, spanX, spanY, cn, isWidget);
            }
            resultValue = 0;
        } else {
            Log.w(TAG, "addWorkspaceItem : " + cn + " is not exist a on Device");
            resultValue = -3;
        }
        result.putInt("invocation_result", resultValue);
        return result;
    }

    private Bundle removeWorkspaceItem(Bundle param, boolean isWidget) {
        int resultValue;
        Log.v(TAG, "removeWorkspaceItem : isWidget ? " + isWidget);
        Bundle result = new Bundle();
        ComponentName cn = null;
        if (param == null) {
            Log.w(TAG, "removeWorkspaceItem : param is null");
        } else {
            cn = (ComponentName) param.getParcelable("component");
            if (cn == null) {
                Log.w(TAG, "removeWorkspaceItem : componentName is null");
            }
        }
        if (cn == null) {
            resultValue = -4;
        } else if (this.mAppstate.isHomeOnlyModeEnabled() && !isWidget) {
            Log.w(TAG, "removeWorkspaceItem : HomeOnlyMode do not support to remove shortcut");
            resultValue = -2;
        } else if (isItemExist(cn, true, isWidget)) {
            if (!ExternalMethodQueue.queueExternalMethodInfo(new ExternalMethodInfo(isWidget ? "remove_widget" : "remove_shortcut", param, -1), this.mContext)) {
                this.mAppstate.getModel().getHomeLoader().removeItem(cn, isWidget, UserHandleCompat.myUserHandle());
            }
            resultValue = 0;
        } else {
            Log.w(TAG, "removeWorkspaceItem : " + cn + " is not exist on Workspace");
            resultValue = -3;
        }
        result.putInt("invocation_result", resultValue);
        return result;
    }

    private Bundle makeEmptyPosition(Bundle param) {
        int resultValue;
        Log.v(TAG, "makeEmptyPosition");
        Bundle result = new Bundle();
        int spanY = LauncherProxy.INVALID_VALUE;
        int spanX = LauncherProxy.INVALID_VALUE;
        int cellY = LauncherProxy.INVALID_VALUE;
        int cellX = LauncherProxy.INVALID_VALUE;
        int page = LauncherProxy.INVALID_VALUE;
        boolean paramError = true;
        DeviceProfile dp = this.mAppstate.getDeviceProfile();
        if (param == null) {
            Log.w(TAG, "makeEmptyPosition : param is null");
        } else {
            page = param.getInt("page", LauncherProxy.INVALID_VALUE);
            if (page == LauncherProxy.INVALID_VALUE) {
                Log.w(TAG, "makeEmptyPosition : page index is INVALID");
            } else {
                Point pos = (Point) param.getParcelable("coordination_position");
                if (pos == null) {
                    Log.w(TAG, "makeEmptyPosition : position is null");
                } else {
                    cellX = pos.x;
                    cellY = pos.y;
                    if (cellX >= dp.homeGrid.getCellCountX() || cellX < 0) {
                        Log.w(TAG, "makeEmptyPosition : cellX is " + cellX);
                    } else if (cellY >= dp.homeGrid.getCellCountY() || cellY < 0) {
                        Log.w(TAG, "makeEmptyPosition : cellY is " + cellY);
                    } else {
                        Point size = (Point) param.getParcelable("coordination_size");
                        if (size == null) {
                            Log.w(TAG, "makeEmptyPosition : size is null");
                        } else {
                            spanX = size.x;
                            spanY = size.y;
                            if (spanX > dp.homeGrid.getCellCountX() || spanX < 1) {
                                Log.w(TAG, "makeEmptyPosition : spanX is " + spanX);
                            } else if (spanY > dp.homeGrid.getCellCountY() || spanY < 1) {
                                Log.w(TAG, "makeEmptyPosition : spanY is " + spanY);
                            } else {
                                paramError = false;
                            }
                        }
                    }
                }
            }
        }
        if (paramError) {
            resultValue = -4;
        } else {
            int pageCount = this.mAppstate.getModel().getHomeLoader().getWorkspaceScreenCount(true);
            if (page >= pageCount) {
                Log.w(TAG, "makeEmptyPosition : param value is more than page count removeIndex = " + page + " pageCount = " + pageCount);
                resultValue = -3;
            } else {
                if (!ExternalMethodQueue.queueExternalMethodInfo(new ExternalMethodInfo("make_empty_position", param, -1), this.mContext)) {
                    this.mAppstate.getModel().getHomeLoader().removeItemsByPosition(page, cellX, cellY, spanX, spanY);
                }
                resultValue = 0;
            }
        }
        result.putInt("invocation_result", resultValue);
        return result;
    }

    private Bundle removePageFromHome(Bundle param) {
        Log.v(TAG, "removePageFromHome");
        Bundle result = new Bundle();
        int resultValue = -4;
        if (param == null) {
            Log.w(TAG, "removePageFromHome : param is null");
        } else {
            int removeIndex = param.getInt("page", LauncherProxy.INVALID_VALUE);
            if (removeIndex == LauncherProxy.INVALID_VALUE) {
                Log.w(TAG, "removePageFromHome : param value is null");
            } else if (removeIndex < 0) {
                Log.w(TAG, "removePageFromHome : param value is a negative num");
            } else {
                int pageCount = this.mAppstate.getModel().getHomeLoader().getWorkspaceScreenCount(true);
                if (removeIndex >= pageCount) {
                    Log.w(TAG, "removePageFromHome : param value is more than page count removeIndex = " + removeIndex + " pageCount = " + pageCount);
                    resultValue = -3;
                } else if (pageCount == 1 && removeIndex == 0) {
                    Log.w(TAG, "removePageFromHome : total page count is 1");
                    resultValue = -2;
                } else if (!this.mAppstate.isHomeOnlyModeEnabled() || isEmptyPage(removeIndex)) {
                    if (!ExternalMethodQueue.queueExternalMethodInfo(new ExternalMethodInfo("remove_page_from_home", param, -1), this.mContext)) {
                        this.mAppstate.getModel().getHomeLoader().removeScreen(removeIndex);
                    }
                    resultValue = 0;
                } else {
                    Log.w(TAG, "removePageFromHome : " + removeIndex + " page is not empty");
                    resultValue = -2;
                }
            }
        }
        result.putInt("invocation_result", resultValue);
        return result;
    }

    private Bundle getSupplementServicePageVisibility() {
        boolean zeroPageEnable;
        boolean active = true;
        Log.v(TAG, "getSupplementServicePageVisibility");
        Bundle result = new Bundle();
        SharedPreferences prefs = this.mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        if (this.mAppstate.getModel().isSupportVirtualScreen() && this.mAppstate.getEnableZeroPage()) {
            zeroPageEnable = true;
        } else {
            zeroPageEnable = false;
        }
        if (!(zeroPageEnable && prefs.getBoolean(LauncherFiles.ZEROPAGE_ACTIVE_STATE_KEY, zeroPageEnable))) {
            active = false;
        }
        result.putBoolean("visibility", active);
        result.putInt("invocation_result", 0);
        return result;
    }

    private Bundle setSupplementServicePageVisibility(Bundle param) {
        Log.v(TAG, "setSupplementServicePageVisibility");
        Bundle result = new Bundle();
        if (param == null) {
            Log.w(TAG, "setSupplementServicePageVisibility : param is null");
            result.putInt("invocation_result", -4);
        } else {
            boolean zeroPageEnable;
            if (this.mAppstate.getModel().isSupportVirtualScreen() && this.mAppstate.getEnableZeroPage()) {
                zeroPageEnable = true;
            } else {
                zeroPageEnable = false;
            }
            if (zeroPageEnable) {
                boolean active = param.getBoolean("visibility");
                if (this.mListener != null) {
                    this.mListener.onZeroPageActiveChanged(active);
                } else {
                    Editor editor = this.mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
                    editor.putBoolean(LauncherFiles.ZEROPAGE_ACTIVE_STATE_KEY, active);
                    editor.apply();
                    SALogging.getInstance().insertStatusLog(this.mContext.getResources().getString(R.string.status_zeropagesetting), active ? "1" : "0");
                }
                result.putInt("invocation_result", 0);
            } else {
                result.putInt("invocation_result", -2);
            }
        }
        return result;
    }

    private Bundle setAppsButton(boolean enable, String method) {
        IconInfo iconInfo = null;
        Log.v(TAG, method);
        Bundle result = new Bundle();
        int resultValue = 0;
        if (this.mAppstate.isHomeOnlyModeEnabled()) {
            resultValue = -2;
        } else if (!ExternalMethodQueue.queueExternalMethodInfo(new ExternalMethodInfo(method, null, -1), this.mContext)) {
            LauncherModel model = this.mAppstate.getModel();
            Context context = this.mContext;
            if (enable) {
                iconInfo = Utilities.createAppsButton(this.mContext);
            }
            model.updateAppsButton(context, enable, iconInfo);
        }
        result.putInt("invocation_result", resultValue);
        return result;
    }

    private Bundle getAppsButtonState() {
        Log.v(TAG, "getAppsButtonState");
        Bundle result = new Bundle();
        int resultValue = 0;
        if (this.mAppstate.isHomeOnlyModeEnabled()) {
            resultValue = -2;
        } else {
            result.putBoolean("state", this.mAppstate.getAppsButtonEnabled());
        }
        result.putInt("invocation_result", resultValue);
        return result;
    }

    private boolean isItemExist(ComponentName cnFromParam, boolean onWorkspace, boolean isWidget) {
        Cursor cursor;
        if (!isWidget) {
            cursor = this.mContext.getContentResolver().query(Favorites.CONTENT_URI, new String[]{"_id", "itemType", "container", "intent"}, null, null, null);
            ArrayList<Long> homeFolderIds = new ArrayList();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    int type = cursor.getInt(1);
                    int container = cursor.getInt(2);
                    if (!onWorkspace) {
                        try {
                            if (findItemByIntent(cnFromParam, cursor.getString(3))) {
                                if (cursor.isClosed()) {
                                    return true;
                                }
                                cursor.close();
                                return true;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Exception : " + e.toString());
                            if (!cursor.isClosed()) {
                                cursor.close();
                            }
                        } catch (Throwable th) {
                            if (!cursor.isClosed()) {
                                cursor.close();
                            }
                        }
                    } else if (container != -100) {
                        continue;
                    } else if (type == 2) {
                        homeFolderIds.add(Long.valueOf(id));
                    } else {
                        if (findItemByIntent(cnFromParam, cursor.getString(3))) {
                            if (cursor.isClosed()) {
                                return true;
                            }
                            cursor.close();
                            return true;
                        }
                    }
                }
                if (!homeFolderIds.isEmpty()) {
                    cursor.moveToFirst();
                    do {
                        if (homeFolderIds.contains(Long.valueOf((long) cursor.getInt(2)))) {
                            if (findItemByIntent(cnFromParam, cursor.getString(3))) {
                                if (cursor.isClosed()) {
                                    return true;
                                }
                                cursor.close();
                                return true;
                            }
                        }
                    } while (cursor.moveToNext());
                }
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            }
        } else if (onWorkspace) {
            String[] projection = new String[]{Favorites.APPWIDGET_PROVIDER};
            String[] selectionArg = new String[]{"null"};
            cursor = this.mContext.getContentResolver().query(Favorites.CONTENT_URI, projection, "appWidgetProvider!=?", selectionArg, null);
            if (cursor != null) {
                do {
                    try {
                        if (cursor.moveToNext()) {
                        } else if (!cursor.isClosed()) {
                            cursor.close();
                        }
                    } catch (Exception e2) {
                        Log.e(TAG, "Exception : " + e2.toString());
                        if (!cursor.isClosed()) {
                            cursor.close();
                        }
                    } catch (Throwable th2) {
                        if (!cursor.isClosed()) {
                            cursor.close();
                        }
                    }
                }
                while (!cnFromParam.equals(ComponentName.unflattenFromString(cursor.getString(0))));
                if (cursor.isClosed()) {
                    return true;
                }
                cursor.close();
                return true;
            }
        } else {
            if (AppWidgetManagerCompat.getInstance(this.mContext).findProvider(cnFromParam, UserHandleCompat.myUserHandle()) != null) {
                return true;
            }
        }
        return false;
    }

    private void makeAdjustedWidgetSize(ComponentName cn, Point size) {
        LauncherAppWidgetProviderInfo providerInfo = AppWidgetManagerCompat.getInstance(this.mContext).findProvider(cn, UserHandleCompat.myUserHandle());
        if (providerInfo != null) {
            size.x = providerInfo.getNearestWidth(size.x);
            size.y = providerInfo.getNearestHeight(size.y);
        }
    }

    private boolean findItemByIntent(ComponentName cnFromParam, String intentString) {
        if (intentString == null) {
            return false;
        }
        try {
            if (cnFromParam.equals(Intent.parseUri(intentString, 0).getComponent())) {
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Exception : " + e.toString());
            return false;
        }
    }

    private boolean isEmptyPage(int pageIndex) {
        long pageId = -1;
        boolean isEmpty = false;
        Cursor cursor = this.mContext.getContentResolver().query(WorkspaceScreens.CONTENT_URI, new String[]{"_id"}, "screenRank=?", new String[]{String.valueOf(pageIndex)}, null);
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    pageId = cursor.getLong(0);
                }
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception : " + e.toString());
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            } catch (Throwable th) {
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            }
        }
        if (pageId == -1) {
            Log.w(TAG, "isEmptyPage : pageId = " + pageId);
        } else {
            cursor = this.mContext.getContentResolver().query(Favorites.CONTENT_URI, null, "container=? AND screen=?", new String[]{String.valueOf(-100), String.valueOf(pageId)}, null);
            if (cursor != null) {
                try {
                    isEmpty = cursor.getCount() == 0;
                    if (!cursor.isClosed()) {
                        cursor.close();
                    }
                } catch (Exception e2) {
                    Log.e(TAG, "Exception : " + e2.toString());
                    if (!cursor.isClosed()) {
                        cursor.close();
                    }
                } catch (Throwable th2) {
                    if (!cursor.isClosed()) {
                        cursor.close();
                    }
                }
            }
        }
        return isEmpty;
    }

    private Bundle getRotationState() {
        Log.d(TAG, "getRotationState");
        Bundle result = new Bundle();
        result.putBoolean("state", Utilities.isOnlyPortraitMode());
        return result;
    }
}

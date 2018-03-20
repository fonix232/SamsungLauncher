package com.android.launcher3.executor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.home.LauncherAppWidgetInfo;
import com.android.launcher3.proxy.LauncherProxy;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.Parameter;
import java.util.ArrayList;
import java.util.Map;

class StateParamHelper {
    private static final String TAG = StateParamHelper.class.getSimpleName();
    private Map<String, Parameter> mParams;
    private boolean mSlotValueVerified = false;

    enum Type {
        INTEGER {
            boolean check(String value) {
                try {
                    Integer.parseInt(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        },
        BOOLEAN {
            boolean check(String value) {
                if (value == null || "".equals(value)) {
                    return false;
                }
                if ("true".compareToIgnoreCase(value) == 0 || "false".compareToIgnoreCase(value) == 0) {
                    return true;
                }
                return false;
            }
        },
        STRING {
            boolean check(String value) {
                if ("".equals(value)) {
                    return false;
                }
                return true;
            }
        };

        abstract boolean check(String str);
    }

    private StateParamHelper() {
    }

    @NonNull
    static StateParamHelper newHelper(@NonNull Map<String, Parameter> params) {
        StateParamHelper instance = new StateParamHelper();
        instance.mParams = params;
        return instance;
    }

    boolean hasSlotValue(@NonNull String key, @NonNull Type type) {
        boolean result = true;
        try {
            if (this.mParams.containsKey(key)) {
                String slotValue = ((Parameter) this.mParams.get(key)).getSlotValue();
                if ("".equals(slotValue) || slotValue == null) {
                    result = false;
                }
                if (!type.check(slotValue)) {
                    result = false;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Wrong param's value " + key);
            result = false;
        }
        this.mSlotValueVerified = true;
        return result;
    }

    @Nullable
    String getString(@NonNull String key) {
        if (this.mSlotValueVerified) {
            return ((Parameter) this.mParams.get(key)).getSlotValue();
        }
        throw new IllegalStateException("hasSlotValue should be invoked first");
    }

    boolean getBoolean(@NonNull String key) {
        if (this.mSlotValueVerified) {
            return Boolean.parseBoolean(((Parameter) this.mParams.get(key)).getSlotValue());
        }
        throw new IllegalStateException("hasSlotValue should be invoked first");
    }

    int getInt(@NonNull String key) {
        if (this.mSlotValueVerified) {
            return Integer.parseInt(((Parameter) this.mParams.get(key)).getSlotValue());
        }
        throw new IllegalStateException("hasSlotValue should be invoked first");
    }

    @Deprecated
    static int getIntParamValue(@NonNull AbstractStateHandler stateHandler, Map<String, Parameter> params, String key, String nlgTartget, String nlgKey) {
        if (params.containsKey(key)) {
            try {
                String value = ((Parameter) params.get(key)).getSlotValue();
                Log.i(TAG, "getIntParamValue : " + value);
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                stateHandler.mNlgRequestInfo = new NlgRequestInfo(nlgTartget).addScreenParam(nlgKey, "Exist", "no");
                Log.d(TAG, "Wrong param's value " + key);
            }
        } else {
            stateHandler.mNlgRequestInfo = new NlgRequestInfo(nlgTartget).addScreenParam(nlgKey, "Exist", "no");
            Log.i(TAG, "No Int Param exist : " + key);
            return LauncherProxy.INVALID_VALUE;
        }
    }

    @Deprecated
    static int getIntParamValue(@NonNull AbstractStateHandler stateHandler, Map<String, Parameter> params, String key, String nlgTartget) {
        return getIntParamValue(stateHandler, params, key, nlgTartget, key);
    }

    @Deprecated
    static String getStringParamValue(@NonNull AbstractStateHandler stateHandler, Map<String, Parameter> params, String key, String nlgTartget, String nlgKey) {
        if (params.containsKey(key)) {
            String value = ((Parameter) params.get(key)).getSlotValue();
            Log.i(TAG, "getStringParamValue : " + value);
            if (value != null && !"".equals(value)) {
                return value;
            }
            stateHandler.mNlgRequestInfo = new NlgRequestInfo(nlgTartget).addScreenParam(nlgKey, "Exist", "no");
            return value;
        }
        stateHandler.mNlgRequestInfo = new NlgRequestInfo(nlgTartget).addScreenParam(nlgKey, "Exist", "no");
        Log.i(TAG, "No String Param exist : " + key);
        return null;
    }

    @Deprecated
    static String getStringParamValue(@NonNull AbstractStateHandler stateHandler, Map<String, Parameter> params, String key, String nlgTartget) {
        return getStringParamValue(stateHandler, params, key, nlgTartget, key);
    }

    @Deprecated
    static boolean getBooleanParamValue(@NonNull AbstractStateHandler stateHandler, Map<String, Parameter> params, String key, String nlgTartget, String nlgKey) {
        if (params.containsKey(key)) {
            try {
                String value = ((Parameter) params.get(key)).getSlotValue();
                Log.i(TAG, "getBooleanParamValue : " + value);
                return Boolean.parseBoolean(value);
            } catch (Exception e) {
                stateHandler.mNlgRequestInfo = new NlgRequestInfo(nlgTartget).addScreenParam(nlgKey, "Exist", "no");
                Log.d(TAG, "Wrong param's value " + key);
            }
        } else {
            stateHandler.mNlgRequestInfo = new NlgRequestInfo(nlgTartget).addScreenParam(nlgKey, "Exist", "no");
            Log.i(TAG, "No Boolean Param exist : " + key);
            return false;
        }
    }

    @Deprecated
    static boolean getBooleanParamValue(@NonNull AbstractStateHandler stateHandler, Map<String, Parameter> params, String key, String nlgTartget) {
        return getBooleanParamValue(stateHandler, params, key, nlgTartget, key);
    }

    @Deprecated
    static void setWidgetNamebyComponentName(@NonNull AbstractStateHandler stateHandler, StateAppInfo appInfo) {
        if (appInfo.getComponentName() != null && appInfo.getName() == " ") {
            ArrayList<ItemInfo> items = stateHandler.getLauncherProxy().getHomeWidgetItemInfo(appInfo);
            if (items != null && items.size() != 0) {
                ItemInfo item = (ItemInfo) items.get(0);
                if (item != null && (item instanceof LauncherAppWidgetInfo)) {
                    appInfo.setName(item.title.toString());
                }
            }
        }
    }
}

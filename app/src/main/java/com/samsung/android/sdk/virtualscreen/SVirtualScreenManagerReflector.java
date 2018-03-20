package com.samsung.android.sdk.virtualscreen;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

class SVirtualScreenManagerReflector extends SVirtualScreenReflector {
    private static final boolean DEBUG = true;
    private static final boolean DEBUG_REFLECTION = false;
    private static final int MULTISCREEN_VER_1 = 1;
    private static final String TAG = SVirtualScreenManagerReflector.class.getSimpleName();
    private static int mVersionCode = 0;
    private static Class<?> sKlassVirtualScreenManager;
    private boolean mInitialized = false;
    private Object mInstanceVirtualScreenManager;

    public static class LaunchParams {
        static String[] FIELD_NAMES = new String[]{"FLAG_BASE_ACTIVITY", "FLAG_CLEAR_TASKS", "FLAG_NO_ANIMATION", "FLAG_RECREATE_VIRTUALSCREEN", "FLAG_LAYOUT_POLICY", "FLAG_FOCUS_POLICY", "FLAG_ZEROPAGE_POLICY", "FLAG_REUSE_TASK_POLICY"};
        public static int FLAG_BASE_ACTIVITY;
        public static int FLAG_CLEAR_TASKS;
        public static int FLAG_FOCUS_POLICY;
        public static int FLAG_LAYOUT_POLICY;
        public static int FLAG_NO_ANIMATION;
        public static int FLAG_RECREATE_VIRTUALSCREEN;
        public static int FLAG_REUSE_TASK_POLICY;
        public static int FLAG_ZEROPAGE_POLICY;

        static {
            int N = FIELD_NAMES.length;
            Class<?> klass = null;
            try {
                if (SVirtualScreenManagerReflector.mVersionCode < 1) {
                    klass = Class.forName("com.samsung.android.multidisplay.virtualscreen.VirtualScreenLaunchParams");
                } else {
                    klass = Class.forName("com.samsung.android.multiscreen.MultiScreenLaunchParams");
                }
            } catch (ClassNotFoundException e) {
            }
            for (int i = 0; i < N; i++) {
                try {
                    Field src = klass.getDeclaredField(FIELD_NAMES[i]);
                    Field dst = LaunchParams.class.getField(FIELD_NAMES[i]);
                    dst.set(dst, src.get(src));
                } catch (NoSuchFieldException e2) {
                    e2.printStackTrace();
                } catch (IllegalArgumentException e3) {
                    e3.printStackTrace();
                } catch (IllegalAccessException e4) {
                    e4.printStackTrace();
                }
            }
        }
    }

    static {
        loadKlass();
    }

    SVirtualScreenManagerReflector(Context context) {
        if (!new SVirtualScreen().isFeatureEnabled(1)) {
            return;
        }
        if (context == null) {
            throw new NullPointerException("context is null");
        }
        try {
            this.mInstanceVirtualScreenManager = sKlassVirtualScreenManager.getConstructor(new Class[]{Context.class}).newInstance(new Object[]{context});
            this.mInitialized = true;
            Log.d(TAG, "completely initialized");
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "NoSuchMethodException !");
            e.printStackTrace();
        } catch (InstantiationException e2) {
            Log.e(TAG, "InstantiationException !");
            e2.printStackTrace();
        } catch (IllegalAccessException e3) {
            Log.e(TAG, "IllegalAccessException !");
            e3.printStackTrace();
        } catch (IllegalArgumentException e4) {
            Log.e(TAG, "IllegalArgumentException !");
            e4.printStackTrace();
        } catch (InvocationTargetException e5) {
            Log.e(TAG, "InvocationTargetException ! cause=" + e5.getCause());
            e5.printStackTrace();
        }
    }

    static void loadKlass() {
        if (sKlassVirtualScreenManager == null) {
            try {
                sKlassVirtualScreenManager = Class.forName("com.samsung.android.multiscreen.virtualscreen.VirtualScreenManager");
                mVersionCode = 1;
                Log.d(TAG, "mVersionCode : " + mVersionCode + " Support from N OS");
            } catch (ClassNotFoundException e) {
                try {
                    sKlassVirtualScreenManager = Class.forName("com.samsung.android.multidisplay.virtualscreen.VirtualScreenManager");
                    Log.d(TAG, "mVersionCode : " + mVersionCode + " Support until M OS");
                } catch (ClassNotFoundException e2) {
                    return;
                }
            }
            SVirtualScreenReflector.putMethod(sKlassVirtualScreenManager, "startActivity", new Class[]{Intent.class, Bundle.class});
            SVirtualScreenReflector.putMethod(sKlassVirtualScreenManager, "bindVirtualScreen", (Class[]) null);
            SVirtualScreenReflector.putMethod(sKlassVirtualScreenManager, "unBindVirtualScreen", (Class[]) null);
            SVirtualScreenReflector.putMethod(sKlassVirtualScreenManager, "setOffset", new Class[]{Integer.TYPE, Integer.TYPE, Boolean.TYPE});
            SVirtualScreenReflector.putMethod(sKlassVirtualScreenManager, "getOffset", (Class[]) null);
            SVirtualScreenReflector.putMethod(sKlassVirtualScreenManager, "isMoving", (Class[]) null);
            SVirtualScreenReflector.putMethod(sKlassVirtualScreenManager, "getDisplayId", new Class[]{Rect.class, Integer.TYPE});
            SVirtualScreenReflector.putMethod(sKlassVirtualScreenManager, "updateVirtualScreen", new Class[]{Rect.class, Integer.TYPE});
            SVirtualScreenReflector.putMethod(Intent.class, "getVirtualScreenParams", (Class[]) null);
            SVirtualScreenReflector.putMethod(Intent.class, "getLaunchParams", (Class[]) null);
            SVirtualScreenReflector.putMethod(sKlassVirtualScreenManager, "getDisplayIdByPackage", new Class[]{String.class});
            try {
                Class<?> klass = Class.forName("com.samsung.android.multiscreen.MultiScreenLaunchParams");
                SVirtualScreenReflector.putMethod(klass, "setDisplayId", new Class[]{Integer.TYPE});
                SVirtualScreenReflector.putMethod(klass, "setFlags", new Class[]{Integer.TYPE});
                SVirtualScreenReflector.putMethod(klass, "setBaseDisplayId", new Class[]{Integer.TYPE});
                SVirtualScreenReflector.putMethod(klass, "setLaunchParams", new Class[]{Object.class});
            } catch (ClassNotFoundException e3) {
            }
        }
        checkVersion();
    }

    boolean initialized() {
        return this.mInitialized;
    }

    boolean setOffset(int offsetX, int offsetY, boolean force) {
        String methodNameWithParam = "setOffset(int,int,boolean)";
        if (!SVirtualScreenReflector.checkMethod(sKlassVirtualScreenManager, methodNameWithParam)) {
            return false;
        }
        return ((Boolean) SVirtualScreenReflector.invoke(sKlassVirtualScreenManager, methodNameWithParam, this.mInstanceVirtualScreenManager, Integer.valueOf(offsetX), Integer.valueOf(offsetY), Boolean.valueOf(force))).booleanValue();
    }

    Point getOffset() {
        String methodNameWithParam = "getOffset()";
        if (SVirtualScreenReflector.checkMethod(sKlassVirtualScreenManager, methodNameWithParam)) {
            return (Point) SVirtualScreenReflector.invoke(sKlassVirtualScreenManager, methodNameWithParam, this.mInstanceVirtualScreenManager, new Object[0]);
        }
        return new Point();
    }

    int startActivity(Intent intent, Bundle options, com.samsung.android.sdk.virtualscreen.SVirtualScreenManager.LaunchParams params) {
        String methodNameWithParam = "startActivity(Intent,Bundle)";
        if (!(intent == null || params == null)) {
            Object ob;
            if (mVersionCode < 1) {
                try {
                    if (SVirtualScreenReflector.checkMethod(Intent.class, "getVirtualScreenParams()")) {
                        ob = SVirtualScreenReflector.invoke(Intent.class, "getVirtualScreenParams()", intent, new Object[0]);
                        if (params.bounds != null) {
                            ob.getClass().getField("mBounds").set(ob, params.bounds);
                        } else if (params.displayId > -1) {
                            ob.getClass().getField("mDisplayId").setInt(ob, params.displayId);
                        }
                        ob.getClass().getField("mFlags").setInt(ob, params.flags);
                    }
                } catch (NoSuchFieldException e) {
                    Log.e(TAG, "startActivity() : NoSuchMethodException ! getVirtualScreenParams");
                } catch (IllegalAccessException e2) {
                    Log.e(TAG, "IllegalAccessException !");
                } catch (Exception e3) {
                }
            } else if (SVirtualScreenReflector.checkMethod(Intent.class, "getLaunchParams()")) {
                ob = SVirtualScreenReflector.invoke(Intent.class, "getLaunchParams()", intent, new Object[0]);
                if (params.bounds != null) {
                    int displayId = getDisplayId(params.bounds, params.flags);
                    if (displayId <= -1) {
                        Log.d(TAG, "startActivity() Can not startActivity in VirtualScreen displayId : " + displayId);
                        return -1;
                    } else if (SVirtualScreenReflector.checkMethod(ob.getClass(), "setDisplayId(int)")) {
                        SVirtualScreenReflector.invoke(ob.getClass(), "setDisplayId(int)", ob, Integer.valueOf(displayId));
                    }
                } else if (params.displayId > -1 && SVirtualScreenReflector.checkMethod(ob.getClass(), "setDisplayId(int)")) {
                    SVirtualScreenReflector.invoke(ob.getClass(), "setDisplayId(int)", ob, Integer.valueOf(params.displayId));
                }
                if (SVirtualScreenReflector.checkMethod(ob.getClass(), "setFlags(int)")) {
                    SVirtualScreenReflector.invoke(ob.getClass(), "setFlags(int)", ob, Integer.valueOf(params.flags));
                }
            }
        }
        if (!SVirtualScreenReflector.checkMethod(sKlassVirtualScreenManager, methodNameWithParam)) {
            return -1;
        }
        return ((Integer) SVirtualScreenReflector.invoke(sKlassVirtualScreenManager, methodNameWithParam, this.mInstanceVirtualScreenManager, intent, options)).intValue();
    }

    int getDisplayId(Rect bound, int flags) {
        String methodNameWithParam = "getDisplayId(Rect,int)";
        if (!SVirtualScreenReflector.checkMethod(sKlassVirtualScreenManager, methodNameWithParam)) {
            return -1;
        }
        return ((Integer) SVirtualScreenReflector.invoke(sKlassVirtualScreenManager, methodNameWithParam, this.mInstanceVirtualScreenManager, bound, Integer.valueOf(flags))).intValue();
    }

    boolean bindVirtualScreen() {
        String methodNameWithParam = "bindVirtualScreen()";
        if (SVirtualScreenReflector.checkMethod(sKlassVirtualScreenManager, methodNameWithParam)) {
            return ((Boolean) SVirtualScreenReflector.invoke(sKlassVirtualScreenManager, methodNameWithParam, this.mInstanceVirtualScreenManager, new Object[0])).booleanValue();
        }
        return false;
    }

    boolean unBindVirtualScreen() {
        String methodNameWithParam = "unBindVirtualScreen()";
        if (SVirtualScreenReflector.checkMethod(sKlassVirtualScreenManager, methodNameWithParam)) {
            return ((Boolean) SVirtualScreenReflector.invoke(sKlassVirtualScreenManager, methodNameWithParam, this.mInstanceVirtualScreenManager, new Object[0])).booleanValue();
        }
        return false;
    }

    boolean isMoving() {
        String methodNameWithParam = "isMoving()";
        if (SVirtualScreenReflector.checkMethod(sKlassVirtualScreenManager, methodNameWithParam)) {
            return ((Boolean) SVirtualScreenReflector.invoke(sKlassVirtualScreenManager, methodNameWithParam, this.mInstanceVirtualScreenManager, new Object[0])).booleanValue();
        }
        return false;
    }

    boolean updateVirtualScreen(Rect bound, int flags) {
        String methodNameWithParam = "updateVirtualScreen(Rect,int)";
        if (!SVirtualScreenReflector.checkMethod(sKlassVirtualScreenManager, methodNameWithParam)) {
            return false;
        }
        return ((Boolean) SVirtualScreenReflector.invoke(sKlassVirtualScreenManager, methodNameWithParam, this.mInstanceVirtualScreenManager, bound, Integer.valueOf(flags))).booleanValue();
    }

    int getDisplayIdByPackage(String packageName) {
        String methodNameWithParam = "getDisplayIdByPackage(String)";
        if (!SVirtualScreenReflector.checkMethod(sKlassVirtualScreenManager, methodNameWithParam)) {
            return -1;
        }
        return ((Integer) SVirtualScreenReflector.invoke(sKlassVirtualScreenManager, methodNameWithParam, this.mInstanceVirtualScreenManager, packageName)).intValue();
    }

    Intent updateMultiScreenLaunchParams(Intent intent, com.samsung.android.sdk.virtualscreen.SVirtualScreenManager.LaunchParams params) {
        if (intent == null || params == null) {
            return intent;
        }
        Object obj = null;
        if (SVirtualScreenReflector.checkMethod(Intent.class, "getLaunchParams()")) {
            obj = SVirtualScreenReflector.invoke(Intent.class, "getLaunchParams()", intent, new Object[0]);
            int baseDisplayId = params.baseDisplayId;
            if (baseDisplayId <= -1) {
                Log.d(TAG, "updateMultiScreenLaunchParams() Can not updateMultiScreenParams baseDisplayId : " + baseDisplayId);
                return null;
            }
            if (SVirtualScreenReflector.checkMethod(obj.getClass(), "setBaseDisplayId(int)")) {
                SVirtualScreenReflector.invoke(obj.getClass(), "setBaseDisplayId(int)", obj, Integer.valueOf(baseDisplayId));
            }
            if (SVirtualScreenReflector.checkMethod(obj.getClass(), "setFlags(int)")) {
                SVirtualScreenReflector.invoke(obj.getClass(), "setFlags(int)", obj, Integer.valueOf(params.flags));
            }
        }
        if (!SVirtualScreenReflector.checkMethod(Intent.class, "setLaunchParams(Object)")) {
            return intent;
        }
        SVirtualScreenReflector.invoke(Intent.class, "setLaunchParams(Object)", intent, obj);
        return intent;
    }

    private static int getFrameworkVersionCode() {
        return -1;
    }

    private static String getFrameworkVersionName() {
        return "UNKNOWN";
    }

    private static int getRequiredMinimumSdkVersionCode() {
        return -1;
    }

    private static String getRequiredMinimumSdkVersionName() {
        return "UNKNOWN";
    }

    private static void checkVersion() {
    }
}

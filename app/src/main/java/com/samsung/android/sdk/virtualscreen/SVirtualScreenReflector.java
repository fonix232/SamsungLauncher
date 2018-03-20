package com.samsung.android.sdk.virtualscreen;

import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

class SVirtualScreenReflector {
    private static final boolean DEBUG_REFLECTION = false;
    private static final String TAG = SVirtualScreenReflector.class.getSimpleName();
    private static HashMap<String, Method> mMethodMap = new HashMap();

    protected SVirtualScreenReflector() {
    }

    public static void putMethod(Class<?> cls, String methodName, Class<?>[] params) {
        try {
            Method m = cls.getMethod(methodName, params);
            StringBuilder b = new StringBuilder(256);
            b.append(methodName);
            b.append("(");
            if (params != null) {
                int paramSize = params.length;
                for (int i = 0; i < paramSize; i++) {
                    b.append(params[i].getSimpleName());
                    if (i < paramSize - 1) {
                        b.append(",");
                    }
                }
            }
            b.append(")");
            mMethodMap.put(cls.getSimpleName() + "." + b.toString(), m);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "putMethod() : NoSuchMethodException ! methodName=" + methodName);
            e.printStackTrace();
        }
    }

    public static boolean checkMethod(Class<?> cls, String methodNameWithParam) {
        if (((Method) mMethodMap.get(cls.getSimpleName() + "." + methodNameWithParam)) != null) {
            return true;
        }
        return false;
    }

    public static Object invoke(Class<?> cls, String methodNameWithParam, Object instance, Object... args) {
        try {
            Method method = (Method) mMethodMap.get(cls.getSimpleName() + "." + methodNameWithParam);
            if (method != null) {
                return method.invoke(instance, args);
            }
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException !");
            e.printStackTrace();
        } catch (IllegalArgumentException e2) {
            Log.e(TAG, "IllegalArgumentException !");
            e2.printStackTrace();
        } catch (InvocationTargetException e3) {
            throw new RuntimeException(e3.getCause());
        } catch (Exception e4) {
            e4.printStackTrace();
        }
        return null;
    }
}

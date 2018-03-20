package com.android.launcher3.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class Reflection {
    private Reflection() {
    }

    public static Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Field getField(String className, String fieldName, boolean accessible) {
        try {
            return getField(Class.forName(className), fieldName, accessible);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Field getField(Class<?> c, String fieldName, boolean accessible) {
        try {
            Field foundField = c.getDeclaredField(fieldName);
            foundField.setAccessible(accessible);
            return foundField;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Method getMethod(String className, String methodName, boolean accessible) {
        try {
            Method foundMethod = Class.forName(className).getDeclaredMethod(methodName, new Class[0]);
            foundMethod.setAccessible(accessible);
            return foundMethod;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e2) {
            e2.printStackTrace();
        }
        return null;
    }

    public static Method getMethod(String className, String methodName, Class<?>[] paramTypes, boolean accessible) {
        try {
            Method foundMethod = Class.forName(className).getDeclaredMethod(methodName, paramTypes);
            foundMethod.setAccessible(accessible);
            return foundMethod;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e2) {
            e2.printStackTrace();
        }
        return null;
    }

    public static Method getMethod(Class<?> c, String methodName, Class<?>[] paramTypes, boolean accessible) {
        try {
            Method foundMethod = c.getDeclaredMethod(methodName, paramTypes);
            foundMethod.setAccessible(accessible);
            return foundMethod;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Constructor<?> getConstructor(String className, Class<?>[] paramTypes, boolean accessible) {
        try {
            Constructor<?> foundConstructor = Class.forName(className).getDeclaredConstructor(paramTypes);
            foundConstructor.setAccessible(accessible);
            return foundConstructor;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e2) {
            e2.printStackTrace();
        }
        return null;
    }

    public static Object invoke(Object instance, Method method, Object... args) {
        try {
            return method.invoke(instance, args);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
            return null;
        } catch (InvocationTargetException e3) {
            e3.printStackTrace();
            return null;
        }
    }

    public static Object getFieldValue(Object object, Field field) {
        try {
            return field.get(object);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
            return null;
        }
    }
}

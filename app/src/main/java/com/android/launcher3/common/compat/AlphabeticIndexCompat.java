package com.android.launcher3.common.compat;

import android.content.Context;
import com.android.launcher3.Utilities;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

public class AlphabeticIndexCompat extends BaseAlphabeticIndex {
    private static final String MID_DOT = "∙";
    private Method mAddLabelsMethod;
    private Object mAlphabeticIndex;
    private String mDefaultMiscLabel;
    private Method mGetBucketIndexMethod;
    private Method mGetBucketLabelMethod;
    private boolean mHasValidAlphabeticIndex;
    private Method mSetMaxLabelCountMethod;

    public AlphabeticIndexCompat(Context context) {
        try {
            Locale curLocale = Utilities.getLocale(context);
            Class clazz = Class.forName("libcore.icu.AlphabeticIndex");
            Constructor ctor = clazz.getConstructor(new Class[]{Locale.class});
            this.mAddLabelsMethod = clazz.getDeclaredMethod("addLabels", new Class[]{Locale.class});
            this.mSetMaxLabelCountMethod = clazz.getDeclaredMethod("setMaxLabelCount", new Class[]{Integer.TYPE});
            this.mGetBucketIndexMethod = clazz.getDeclaredMethod("getBucketIndex", new Class[]{String.class});
            this.mGetBucketLabelMethod = clazz.getDeclaredMethod("getBucketLabel", new Class[]{Integer.TYPE});
            this.mAlphabeticIndex = ctor.newInstance(new Object[]{curLocale});
            try {
                if (!curLocale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
                    this.mAddLabelsMethod.invoke(this.mAlphabeticIndex, new Object[]{Locale.ENGLISH});
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (curLocale.getLanguage().equals(Locale.JAPANESE.getLanguage())) {
                this.mDefaultMiscLabel = "他";
            } else {
                this.mDefaultMiscLabel = MID_DOT;
            }
            this.mHasValidAlphabeticIndex = true;
        } catch (NoSuchMethodException e2) {
            this.mHasValidAlphabeticIndex = false;
        } catch (ClassNotFoundException e3) {
            this.mHasValidAlphabeticIndex = false;
        } catch (InstantiationException e4) {
            this.mHasValidAlphabeticIndex = false;
        } catch (InvocationTargetException e5) {
            this.mHasValidAlphabeticIndex = false;
        } catch (IllegalAccessException e6) {
            this.mHasValidAlphabeticIndex = false;
        }
    }

    public void setMaxLabelCount(int count) {
        if (this.mHasValidAlphabeticIndex) {
            try {
                this.mSetMaxLabelCountMethod.invoke(this.mAlphabeticIndex, new Object[]{Integer.valueOf(count)});
                return;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        super.setMaxLabelCount(count);
    }

    public String computeSectionName(CharSequence cs) {
        String s = Utilities.trim(cs);
        String sectionName = getBucketLabel(getBucketIndex(s));
        String trimmedSectionName = Utilities.trim(sectionName);
        if (trimmedSectionName == null || !trimmedSectionName.isEmpty() || s.length() <= 0) {
            return sectionName;
        }
        int c = s.codePointAt(0);
        if (Character.isDigit(c)) {
            return "#";
        }
        if (Character.isLetter(c)) {
            return this.mDefaultMiscLabel;
        }
        return MID_DOT;
    }

    protected int getBucketIndex(String s) {
        if (this.mHasValidAlphabeticIndex) {
            try {
                return ((Integer) this.mGetBucketIndexMethod.invoke(this.mAlphabeticIndex, new Object[]{s})).intValue();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return super.getBucketIndex(s);
    }

    protected String getBucketLabel(int index) {
        if (this.mHasValidAlphabeticIndex) {
            try {
                return (String) this.mGetBucketLabelMethod.invoke(this.mAlphabeticIndex, new Object[]{Integer.valueOf(index)});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return super.getBucketLabel(index);
    }
}

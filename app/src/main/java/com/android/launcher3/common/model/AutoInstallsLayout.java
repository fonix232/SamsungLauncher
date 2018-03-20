package com.android.launcher3.common.model;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.launcher3.Utilities;
import java.io.IOException;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AutoInstallsLayout {
    private static final String ACTION_LAUNCHER_CUSTOMIZATION = "android.autoinstalls.config.action.PLAY_AUTO_INSTALL";
    private static final String TAG = "AutoInstallsLayout";
    private static final String TAG_WORKSPACE = "workspace";
    private static HashMap<String, String> sAutoInstallApp;
    private final String mPackageName;
    private final Resources mResources;

    public static AutoInstallsLayout get(Context context) {
        Pair<String, Resources> customizationApkInfo = Utilities.findSystemApk(ACTION_LAUNCHER_CUSTOMIZATION, context.getPackageManager());
        if (customizationApkInfo == null) {
            return null;
        }
        if (TextUtils.isEmpty((CharSequence) customizationApkInfo.first) || customizationApkInfo.second == null) {
            return null;
        }
        Log.i(TAG, "there is customizationApkInfo");
        return new AutoInstallsLayout((String) customizationApkInfo.first, (Resources) customizationApkInfo.second);
    }

    private AutoInstallsLayout(String packageName, Resources res) {
        this.mPackageName = packageName;
        this.mResources = res;
        loadAutoInstallApp();
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public Resources getResources() {
        return this.mResources;
    }

    public static boolean isAutoInstallApp(String packageName, String className) {
        if (sAutoInstallApp == null || sAutoInstallApp.isEmpty()) {
            return false;
        }
        String autoInstallClassName = (String) sAutoInstallApp.get(packageName);
        if (autoInstallClassName == null || !autoInstallClassName.equals(className)) {
            return false;
        }
        Log.i(TAG, "isAutoInstallApp, packageName : " + packageName + ", className : " + className);
        return true;
    }

    private void loadAutoInstallApp() {
        Exception e;
        if (this.mPackageName == null || this.mResources == null) {
            Log.i(TAG, "loadAutoInstallApp, mPackageName or mResources is null");
            return;
        }
        int resId = this.mResources.getIdentifier("default_layout", "xml", this.mPackageName);
        if (resId <= 0) {
            Log.i(TAG, "loadAutoInstallApp, there is no default_layout.xml");
            return;
        }
        XmlPullParser parser = this.mResources.getXml(resId);
        if (parser == null) {
            Log.i(TAG, "loadAutoInstallApp, parser is null");
            return;
        }
        sAutoInstallApp = new HashMap();
        try {
            DefaultLayoutParser.beginDocument(parser, TAG_WORKSPACE);
            int depth = parser.getDepth();
            while (true) {
                int type = parser.next();
                if ((type == 3 && parser.getDepth() <= depth) || type == 1) {
                    return;
                }
                if (type == 2) {
                    String tagName = parser.getName();
                    if (DefaultLayoutParser.TAG_AUTO_INSTALL.equals(tagName)) {
                        String packageName = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_PACKAGE_NAME);
                        String className = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_CLASS_NAME);
                        if (!(packageName == null || className == null)) {
                            sAutoInstallApp.put(packageName, className);
                            Log.i(TAG, "loadAutoInstallApp, packageName : " + packageName + ", className : " + className);
                        }
                    } else {
                        Log.e(TAG, "invalid tag : " + tagName);
                    }
                }
            }
        } catch (XmlPullParserException e2) {
            e = e2;
        } catch (IOException e3) {
            e = e3;
        }
        Log.e(TAG, "Got exception parsing autoinstall.", e);
    }
}

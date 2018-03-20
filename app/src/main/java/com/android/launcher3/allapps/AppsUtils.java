package com.android.launcher3.allapps;

import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import com.android.launcher3.allapps.controller.AppsFocusListener;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.view.IconView;
import com.sec.android.app.launcher.R;

public final class AppsUtils {
    public static View createAppIcon(Activity activity, ViewGroup parent, IconInfo info, OnClickListener onClickListener, OnLongClickListener onLongClickListener, AppsFocusListener appsFocusListener) {
        IconView appIcon = (IconView) activity.getWindow().getLayoutInflater().inflate(R.layout.icon, parent, false);
        appIcon.setIconDisplay(2);
        appIcon.applyFromApplicationInfo(info);
        appIcon.setOnClickListener(onClickListener);
        appIcon.setOnLongClickListener(onLongClickListener);
        appIcon.setOnKeyListener(appsFocusListener);
        appIcon.setOnFocusChangeListener(appsFocusListener);
        return appIcon;
    }

    public static View createAppIcon(Activity activity, ViewGroup parent, IconView appIcon, IconInfo info, OnClickListener onClickListener, OnLongClickListener onLongClickListener, AppsFocusListener appsFocusListener) {
        if (appIcon == null) {
            return createAppIcon(activity, parent, info, onClickListener, onLongClickListener, appsFocusListener);
        }
        appIcon.setIconDisplay(2);
        appIcon.applyFromApplicationInfo(info);
        appIcon.setOnClickListener(onClickListener);
        appIcon.setOnLongClickListener(onLongClickListener);
        appIcon.setOnKeyListener(appsFocusListener);
        appIcon.setOnFocusChangeListener(appsFocusListener);
        return appIcon;
    }
}

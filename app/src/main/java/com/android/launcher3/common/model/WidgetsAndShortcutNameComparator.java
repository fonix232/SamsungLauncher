package com.android.launcher3.common.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.compat.AppWidgetManagerCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.util.ComponentKey;
import java.text.Collator;
import java.util.Comparator;
import java.util.HashMap;

public class WidgetsAndShortcutNameComparator implements Comparator<Object> {
    private final Collator mCollator = Collator.getInstance();
    private final HashMap<ComponentKey, String> mLabelCache = new HashMap();
    private final UserHandleCompat mMainHandle = UserHandleCompat.myUserHandle();
    private final AppWidgetManagerCompat mManager;
    private final PackageManager mPackageManager;

    public WidgetsAndShortcutNameComparator(Context context) {
        this.mManager = AppWidgetManagerCompat.getInstance(context);
        this.mPackageManager = context.getPackageManager();
    }

    public void reset() {
        this.mLabelCache.clear();
    }

    public final int compare(Object objA, Object objB) {
        boolean aWorkProfile;
        ComponentKey keyA = getComponentKey(objA);
        ComponentKey keyB = getComponentKey(objB);
        if (this.mMainHandle.equals(keyA.user)) {
            aWorkProfile = false;
        } else {
            aWorkProfile = true;
        }
        boolean bWorkProfile;
        if (this.mMainHandle.equals(keyB.user)) {
            bWorkProfile = false;
        } else {
            bWorkProfile = true;
        }
        if (aWorkProfile && !bWorkProfile) {
            return 1;
        }
        if (!aWorkProfile && bWorkProfile) {
            return -1;
        }
        String labelA = (String) this.mLabelCache.get(keyA);
        String labelB = (String) this.mLabelCache.get(keyB);
        if (labelA == null) {
            labelA = getLabel(objA);
            this.mLabelCache.put(keyA, labelA);
        }
        if (labelB == null) {
            labelB = getLabel(objB);
            this.mLabelCache.put(keyB, labelB);
        }
        return this.mCollator.compare(labelA, labelB);
    }

    private ComponentKey getComponentKey(Object o) {
        if (o instanceof LauncherAppWidgetProviderInfo) {
            LauncherAppWidgetProviderInfo widgetInfo = (LauncherAppWidgetProviderInfo) o;
            return new ComponentKey(widgetInfo.provider, this.mManager.getUser(widgetInfo));
        }
        ResolveInfo info = (ResolveInfo) o;
        return new ComponentKey(new ComponentName(info.activityInfo.packageName, info.activityInfo.name), UserHandleCompat.myUserHandle());
    }

    private String getLabel(Object o) {
        if (!(o instanceof LauncherAppWidgetProviderInfo)) {
            return Utilities.trim(((ResolveInfo) o).loadLabel(this.mPackageManager));
        }
        return Utilities.trim(this.mManager.loadLabel((LauncherAppWidgetProviderInfo) o));
    }
}

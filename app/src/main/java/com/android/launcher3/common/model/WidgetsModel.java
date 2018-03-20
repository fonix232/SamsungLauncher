package com.android.launcher3.common.model;

import android.content.Context;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.AlphabeticIndexCompat;
import com.android.launcher3.common.compat.AppWidgetManagerCompat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class WidgetsModel {
    private static final boolean DEBUG = false;
    private static final String TAG = "WidgetsModel";
    private final Comparator<ItemInfo> mAppNameComparator;
    private final AppWidgetManagerCompat mAppWidgetMgr;
    private final IconCache mIconCache;
    private AlphabeticIndexCompat mIndexer;
    private ArrayList<PackageItemInfo> mPackageItemInfos = new ArrayList();
    private ArrayList<Object> mRawList;
    private final WidgetsAndShortcutNameComparator mWidgetAndShortcutNameComparator;
    private HashMap<PackageItemInfo, ArrayList<Object>> mWidgetsList = new HashMap();

    public WidgetsModel(Context context, IconCache iconCache) {
        this.mAppWidgetMgr = AppWidgetManagerCompat.getInstance(context);
        this.mWidgetAndShortcutNameComparator = new WidgetsAndShortcutNameComparator(context);
        this.mAppNameComparator = new AppNameComparator(context).getAppInfoComparator();
        this.mIconCache = iconCache;
        this.mIndexer = new AlphabeticIndexCompat(context);
    }

    private WidgetsModel(WidgetsModel model) {
        this.mAppWidgetMgr = model.mAppWidgetMgr;
        this.mPackageItemInfos = (ArrayList) model.mPackageItemInfos.clone();
        this.mWidgetsList = (HashMap) model.mWidgetsList.clone();
        this.mRawList = (ArrayList) model.mRawList.clone();
        this.mWidgetAndShortcutNameComparator = model.mWidgetAndShortcutNameComparator;
        this.mAppNameComparator = model.mAppNameComparator;
        this.mIconCache = model.mIconCache;
    }

    public int getPackageSize() {
        if (this.mPackageItemInfos == null) {
            return 0;
        }
        return this.mPackageItemInfos.size();
    }

    public PackageItemInfo getPackageItemInfo(int pos) {
        if (pos >= this.mPackageItemInfos.size() || pos < 0) {
            return null;
        }
        return (PackageItemInfo) this.mPackageItemInfos.get(pos);
    }

    public List<Object> getSortedWidgets(int pos) {
        return (List) this.mWidgetsList.get(this.mPackageItemInfos.get(pos));
    }

    public List<Object> getSortedWidgets() {
        return getSortedWidgets(0, this.mPackageItemInfos.size());
    }

    public List<Object> getSortedWidgets(int start, int count) {
        List<Object> outList = new ArrayList();
        int pos = start;
        while (pos < pos + count && pos < this.mPackageItemInfos.size() && pos >= 0) {
            outList.add(getSortedWidgets(pos));
            pos++;
        }
        return outList;
    }

    public WidgetsModel clone() {
        return new WidgetsModel(this);
    }
}

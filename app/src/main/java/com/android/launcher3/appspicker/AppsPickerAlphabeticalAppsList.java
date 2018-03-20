package com.android.launcher3.appspicker;

import android.content.Context;
import android.util.Log;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import com.android.launcher3.allapps.AlphabeticalAppsList.AdapterItem;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.util.ComponentKey;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class AppsPickerAlphabeticalAppsList extends AlphabeticalAppsList {
    private static final String TAG = AppsPickerAlphabeticalAppsList.class.getSimpleName();
    private HashMap<Integer, PositionInfo> mAppsPositionInfoMap = new HashMap();
    private Context mContext;
    private ArrayList<IconInfo> mHiddenApps = new ArrayList();
    private ArrayList<ComponentKey> mLastOrderedFilter;
    private int mNumAppsPerRow = this.mContext.getResources().getInteger(R.integer.config_appsPicker_NumAppsPerRow);

    private static class PositionInfo {
        public int mEnd;
        public int mStart;

        public PositionInfo(int start, int end) {
            this.mStart = start;
            this.mEnd = end;
        }
    }

    public AppsPickerAlphabeticalAppsList(Context context) {
        super(context);
        this.mContext = context;
    }

    public void setHiddenApps(List<IconInfo> apps) {
        this.mHiddenApps.clear();
        this.mHiddenApps.addAll(apps);
        setOrderedFilter(this.mLastOrderedFilter);
    }

    public boolean hasFilter() {
        return true;
    }

    public boolean hasNoFilteredResults() {
        return false;
    }

    public void setOrderedFilter(ArrayList<ComponentKey> f) {
        ArrayList<ComponentKey> filteredComponents;
        this.mLastOrderedFilter = f;
        if (f == null) {
            filteredComponents = new ArrayList();
            for (IconInfo info : getApps()) {
                filteredComponents.add(info.toComponentKey());
            }
        } else {
            filteredComponents = f;
        }
        filterHiddenApps(filteredComponents);
        super.setOrderedFilter(filteredComponents);
    }

    private void filterHiddenApps(ArrayList<ComponentKey> f) {
        Iterator it = this.mHiddenApps.iterator();
        while (it.hasNext()) {
            IconInfo info = (IconInfo) it.next();
            ComponentKey hiddenAppKey = info.toComponentKey();
            Iterator it2 = f.iterator();
            while (it2.hasNext()) {
                ComponentKey key = (ComponentKey) it2.next();
                if (hiddenAppKey.equals(key)) {
                    Log.v(TAG, "filterHiddenApps : filter " + info.componentName.getPackageName());
                    f.remove(key);
                    break;
                }
            }
        }
    }

    public int getNumAppsToShow() {
        return Math.max(super.size() - this.mHiddenApps.size(), 0);
    }

    public int getAppsMapSize() {
        return this.mAppsPositionInfoMap.size();
    }

    public List<AdapterItem> getSearchedRowItems(int position) {
        int row = position * this.mNumAppsPerRow;
        if (this.mNumAppsPerRow + row < getAdapterItems().size()) {
            return getAdapterItems().subList(row, this.mNumAppsPerRow + row);
        }
        return null;
    }

    public List<AdapterItem> getRowItems(int position) {
        boolean isExistGalaxyAppsButton = false;
        int adapterItemSize = 0;
        int isNotItemTypeCnt = 0;
        if (getAdapterItems() != null) {
            adapterItemSize = getAdapterItems().size();
            for (int i = 0; i < adapterItemSize; i++) {
                if (((AdapterItem) getAdapterItems().get(i)).viewType != 1) {
                    isNotItemTypeCnt++;
                    isExistGalaxyAppsButton = true;
                }
            }
        }
        if (isExistGalaxyAppsButton && adapterItemSize == isNotItemTypeCnt) {
            return null;
        }
        PositionInfo info = (PositionInfo) this.mAppsPositionInfoMap.get(Integer.valueOf(position));
        return (info == null || info.mEnd > adapterItemSize) ? null : getAdapterItems().subList(info.mStart, info.mEnd);
    }

    public int getNumAppsPerRow() {
        return this.mNumAppsPerRow;
    }

    public void setNumAppsPerRow() {
        this.mNumAppsPerRow = this.mContext.getResources().getInteger(R.integer.config_appsPicker_NumAppsPerRow);
    }

    public void resetMap() {
        this.mAppsPositionInfoMap.clear();
    }

    public void initAppPositionInfoMap() {
        Log.d(TAG, "initAppPositionInfoMap : AdapterItems size = " + getAdapterItems().size());
        int appsSize = 0;
        int startIndex = 0;
        int rowIndex = 0;
        for (int i = 0; i < getAdapterItems().size(); i++) {
            if (((AdapterItem) getAdapterItems().get(i)).appIndex >= 0) {
                appsSize++;
            }
        }
        while (appsSize > 0 && ((AdapterItem) getAdapterItems().get(startIndex)).sectionName != null) {
            String compName = ((AdapterItem) getAdapterItems().get(startIndex)).sectionName;
            int endIndex = startIndex;
            while (endIndex < this.mNumAppsPerRow + startIndex && endIndex < appsSize && compName.equals(((AdapterItem) getAdapterItems().get(endIndex)).sectionName)) {
                endIndex++;
            }
            this.mAppsPositionInfoMap.put(Integer.valueOf(rowIndex), new PositionInfo(startIndex, endIndex));
            startIndex = endIndex;
            rowIndex++;
            if (startIndex >= appsSize) {
                return;
            }
        }
    }

    public int getRowFromTitle(String title) {
        Log.i(TAG, "getRowFromTitle title=" + title);
        if (title == null) {
            return 0;
        }
        for (Integer key : this.mAppsPositionInfoMap.keySet()) {
            PositionInfo pos = (PositionInfo) this.mAppsPositionInfoMap.get(key);
            if (pos != null) {
                for (int i = pos.mStart; i <= pos.mEnd; i++) {
                    AdapterItem item = (AdapterItem) getAdapterItems().get(i);
                    if (item != null) {
                        String mapTitle = item.iconInfo.title.toString();
                        Log.i(TAG, "getRowFromTitle mapTitle=" + mapTitle);
                        if (title.equalsIgnoreCase(mapTitle)) {
                            return key.intValue();
                        }
                    }
                }
                continue;
            }
        }
        return 0;
    }
}

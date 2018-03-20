package com.android.launcher3.allapps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.v7.widget.RecyclerView.Adapter;
import android.util.Log;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.compat.AlphabeticIndexCompat;
import com.android.launcher3.common.model.AppNameComparator;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.locale.LocaleUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TreeMap;

public class AlphabeticalAppsList {
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_PREDICTIONS = false;
    private static final int FAST_SCROLL_FRACTION_DISTRIBUTE_BY_NUM_SECTIONS = 1;
    private static final int FAST_SCROLL_FRACTION_DISTRIBUTE_BY_ROWS_FRACTION = 0;
    private static final String GALAXYAPPS = "com.sec.android.app.samsungapps";
    public static final int NETWORK_NOT_CONNECTED = 5;
    private static final String PLAYSTORE = "com.android.vending";
    public static final int SEND_SEARCH_END = 2;
    public static final int SEND_THREAD_INFORMATION = 1;
    public static final String TAG = "AlphabeticalAppsList";
    int called = 0;
    private Adapter mAdapter;
    private List<AdapterItem> mAdapterItems = new ArrayList();
    private AppNameComparator mAppNameComparator;
    private final List<IconInfo> mApps = new ArrayList();
    private HashMap<CharSequence, String> mCachedSectionNames = new HashMap();
    private final HashMap<ComponentKey, IconInfo> mComponentToAppMap = new HashMap();
    private final int mFastScrollDistributionMode = 1;
    private List<FastScrollSectionInfo> mFastScrollerSections = new ArrayList();
    private List<IconInfo> mFilteredApps = new ArrayList();
    private List<ItemDetails> mGalaxyItems = new ArrayList();
    public Handler mIncomingHandler = new Handler(new IncomingHandlerCallback());
    private AlphabeticIndexCompat mIndexer;
    private Launcher mLauncher;
    private MergeAlgorithm mMergeAlgorithm;
    private int mNumAppRowsInAdapter;
    private int mNumAppsPerRow;
    private int mNumPredictedAppsPerRow;
    private List<ComponentKey> mPredictedAppComponents = new ArrayList();
    private List<IconInfo> mPredictedApps = new ArrayList();
    private ArrayList<IconInfo> mRecentAppList = new ArrayList();
    private List<IconInfo> mRecentApps = new ArrayList();
    private ArrayList<ComponentKey> mSearchResults;
    private List<SectionInfo> mSections = new ArrayList();

    public static class AdapterItem {
        public int appIndex = -1;
        public IconInfo iconInfo = null;
        public ItemDetails itemDetails = null;
        public int position;
        public int rowAppIndex;
        public int rowIndex;
        public int sectionAppIndex = -1;
        public SectionInfo sectionInfo;
        public String sectionName = null;
        public int viewType;

        public static AdapterItem asNotiText(int pos) {
            AdapterItem item = new AdapterItem();
            item.viewType = 6;
            item.position = pos;
            return item;
        }

        public static AdapterItem asRecentApp(int pos, SectionInfo section, String sectionName, int sectionAppIndex, IconInfo iconInfo, int appIndex) {
            AdapterItem item = new AdapterItem();
            item.viewType = 7;
            item.position = pos;
            item.sectionInfo = section;
            item.sectionName = sectionName;
            item.sectionAppIndex = sectionAppIndex;
            item.iconInfo = iconInfo;
            item.appIndex = appIndex;
            return item;
        }

        public static AdapterItem asSectionBreak(int pos, SectionInfo section) {
            AdapterItem item = new AdapterItem();
            item.viewType = 0;
            item.position = pos;
            item.sectionInfo = section;
            section.sectionBreakItem = item;
            return item;
        }

        public static AdapterItem asPredictedApp(int pos, SectionInfo section, String sectionName, int sectionAppIndex, IconInfo iconInfo, int appIndex) {
            AdapterItem item = asApp(pos, section, sectionName, sectionAppIndex, iconInfo, appIndex);
            item.viewType = 2;
            return item;
        }

        public static AdapterItem asApp(int pos, SectionInfo section, String sectionName, int sectionAppIndex, IconInfo iconInfo, int appIndex) {
            AdapterItem item = new AdapterItem();
            item.viewType = 1;
            item.position = pos;
            item.sectionInfo = section;
            item.sectionName = sectionName;
            item.sectionAppIndex = sectionAppIndex;
            item.iconInfo = iconInfo;
            item.appIndex = appIndex;
            return item;
        }

        public static AdapterItem asEmptySearch(int pos) {
            AdapterItem item = new AdapterItem();
            item.viewType = 3;
            item.position = pos;
            return item;
        }

        public static AdapterItem asDivider(int pos) {
            AdapterItem item = new AdapterItem();
            item.viewType = 4;
            item.position = pos;
            return item;
        }

        public static AdapterItem asMarketSearch(int pos) {
            AdapterItem item = new AdapterItem();
            item.viewType = 5;
            item.position = pos;
            return item;
        }

        public static AdapterItem asTitle(int pos) {
            AdapterItem item = new AdapterItem();
            item.viewType = 8;
            item.position = pos;
            return item;
        }

        public static AdapterItem asViewGalaxyButton(int pos) {
            AdapterItem item = new AdapterItem();
            item.viewType = 9;
            item.position = pos;
            return item;
        }

        public static AdapterItem asViewMarketButton(int pos) {
            AdapterItem item = new AdapterItem();
            item.viewType = 10;
            item.position = pos;
            return item;
        }

        public static AdapterItem asListHeader(int pos, String sectionName) {
            AdapterItem item = new AdapterItem();
            item.viewType = 11;
            item.position = pos;
            item.sectionName = sectionName;
            return item;
        }

        public static AdapterItem asGalaxyApp(int pos, ItemDetails itemDetail, int appIndex) {
            AdapterItem item = new AdapterItem();
            item.viewType = 12;
            item.position = pos;
            item.itemDetails = itemDetail;
            item.appIndex = appIndex;
            return item;
        }
    }

    public static class FastScrollSectionInfo {
        public AdapterItem fastScrollToItem;
        public String sectionName;
        public float touchFraction;

        public FastScrollSectionInfo(String sectionName) {
            this.sectionName = sectionName;
        }
    }

    class IncomingHandlerCallback implements Callback {
        IncomingHandlerCallback() {
        }

        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    int size = msg.getData().getInt(Key.SIZE);
                    if (size > 3) {
                        size = 3;
                    }
                    if (size == AlphabeticalAppsList.this.called) {
                        return false;
                    }
                    Bitmap bmp = msg.obj;
                    String title = msg.getData().getString("title");
                    String ID = msg.getData().getString("ID");
                    if (!AlphabeticalAppsList.this.isAppInstalled(ID, false)) {
                        String seller = msg.getData().getString("seller");
                        String price = msg.getData().getString("price");
                        String rating = msg.getData().getString("rating");
                        ItemDetails item = new ItemDetails();
                        item.setName(title);
                        item.setPkgName(ID);
                        item.setIconImage(bmp);
                        item.setItemSeller(seller);
                        item.setPrice(price);
                        item.setRating(rating);
                        AlphabeticalAppsList.this.mGalaxyItems.add(item);
                        AlphabeticalAppsList alphabeticalAppsList = AlphabeticalAppsList.this;
                        alphabeticalAppsList.called++;
                        break;
                    }
                    return false;
                case 2:
                    if (AlphabeticalAppsList.this.mGalaxyItems.size() > 0) {
                        AlphabeticalAppsList.this.updateAdapterItems();
                        break;
                    }
                    break;
                case 5:
                    Log.d(AlphabeticalAppsList.TAG, "NETWORK_NOT_CONNECTED");
                    break;
            }
            return true;
        }
    }

    public interface MergeAlgorithm {
        boolean continueMerging(SectionInfo sectionInfo, SectionInfo sectionInfo2, int i, int i2, int i3);
    }

    public static class SectionInfo {
        public AdapterItem firstAppItem;
        public int numApps;
        public AdapterItem sectionBreakItem;
    }

    public AlphabeticalAppsList(Context context) {
        this.mLauncher = (Launcher) context;
        this.mIndexer = new AlphabeticIndexCompat(context);
        this.mAppNameComparator = new AppNameComparator(context);
    }

    public void setNumAppsPerRow(int numAppsPerRow, int numPredictedAppsPerRow, MergeAlgorithm mergeAlgorithm) {
        this.mNumAppsPerRow = numAppsPerRow;
        this.mNumPredictedAppsPerRow = numPredictedAppsPerRow;
        this.mMergeAlgorithm = mergeAlgorithm;
        updateAdapterItems();
    }

    public void setAdapter(Adapter adapter) {
        this.mAdapter = adapter;
    }

    public List<IconInfo> getApps() {
        return this.mApps;
    }

    public List<SectionInfo> getSections() {
        return this.mSections;
    }

    public List<FastScrollSectionInfo> getFastScrollerSections() {
        return this.mFastScrollerSections;
    }

    public List<AdapterItem> getAdapterItems() {
        return this.mAdapterItems;
    }

    public int getNumAppRows() {
        return this.mNumAppRowsInAdapter;
    }

    public int getNumFilteredApps() {
        return this.mFilteredApps.size();
    }

    public boolean hasFilter() {
        return this.mSearchResults != null;
    }

    public boolean hasNoFilteredResults() {
        return this.mSearchResults != null && this.mFilteredApps.isEmpty();
    }

    public void setOrderedFilter(ArrayList<ComponentKey> f) {
        if (this.mSearchResults != f) {
            this.mSearchResults = f;
            updateAdapterItems();
        }
    }

    public void setPredictedApps(List<ComponentKey> apps) {
        this.mPredictedAppComponents.clear();
        this.mPredictedAppComponents.addAll(apps);
        onAppsUpdated();
    }

    public void setApps(List<IconInfo> apps) {
        this.mComponentToAppMap.clear();
        addApps(apps);
    }

    public void addApps(List<IconInfo> apps) {
        updateApps(apps);
    }

    public void updateApps(List<IconInfo> apps) {
        for (IconInfo info : apps) {
            if (info.componentName != null) {
                this.mComponentToAppMap.put(info.toComponentKey(), info);
            }
        }
        onAppsUpdated();
    }

    public void removeApps(List<IconInfo> apps) {
        for (IconInfo info : apps) {
            if (info.componentName != null) {
                this.mComponentToAppMap.remove(info.toComponentKey());
            }
        }
        onAppsUpdated();
    }

    private void onAppsUpdated() {
        this.mApps.clear();
        this.mApps.addAll(this.mComponentToAppMap.values());
        Collections.sort(this.mApps, this.mAppNameComparator.getAppInfoComparator());
        if (Utilities.getLocale(this.mLauncher).equals(Locale.SIMPLIFIED_CHINESE)) {
            TreeMap<String, ArrayList<IconInfo>> sectionMap = new TreeMap(this.mAppNameComparator.getSectionNameComparator());
            for (IconInfo info : this.mApps) {
                String sectionName = getAndUpdateCachedSectionName(info.title);
                ArrayList<IconInfo> sectionApps = (ArrayList) sectionMap.get(sectionName);
                if (sectionApps == null) {
                    sectionApps = new ArrayList();
                    sectionMap.put(sectionName, sectionApps);
                }
                sectionApps.add(info);
            }
            List<IconInfo> allApps = new ArrayList(this.mApps.size());
            for (Entry<String, ArrayList<IconInfo>> entry : sectionMap.entrySet()) {
                allApps.addAll((Collection) entry.getValue());
            }
            this.mApps.clear();
            this.mApps.addAll(allApps);
        } else {
            for (IconInfo info2 : this.mApps) {
                getAndUpdateCachedSectionName(info2.title);
            }
        }
        updateAdapterItems();
    }

    private void updateAdapterItems() {
        IconInfo info;
        FastScrollSectionInfo fastScrollSectionInfo;
        int position;
        AdapterItem appItem;
        SectionInfo sectionInfo = null;
        String lastSectionName = null;
        FastScrollSectionInfo fastScrollSectionInfo2 = null;
        int position2 = 0;
        int i = 0;
        this.mFilteredApps.clear();
        this.mFastScrollerSections.clear();
        this.mAdapterItems.clear();
        this.mSections.clear();
        this.mPredictedApps.clear();
        if (!(this.mPredictedAppComponents == null || this.mPredictedAppComponents.isEmpty() || hasFilter())) {
            for (ComponentKey ck : this.mPredictedAppComponents) {
                info = (IconInfo) this.mComponentToAppMap.get(ck);
                if (info != null) {
                    this.mPredictedApps.add(info);
                } else {
                    Log.e(TAG, "Predicted app not found: " + ck.flattenToString(this.mLauncher));
                }
                if (this.mPredictedApps.size() == this.mNumPredictedAppsPerRow) {
                    break;
                }
            }
            if (!this.mPredictedApps.isEmpty()) {
                sectionInfo = new SectionInfo();
                fastScrollSectionInfo = new FastScrollSectionInfo("");
                position = 0 + 1;
                AdapterItem sectionItem = AdapterItem.asSectionBreak(0, sectionInfo);
                this.mSections.add(sectionInfo);
                this.mFastScrollerSections.add(fastScrollSectionInfo);
                this.mAdapterItems.add(sectionItem);
                position2 = position;
                for (IconInfo info2 : this.mPredictedApps) {
                    position = position2 + 1;
                    int i2 = sectionInfo.numApps;
                    sectionInfo.numApps = i2 + 1;
                    int appIndex = i + 1;
                    appItem = AdapterItem.asPredictedApp(position2, sectionInfo, "", i2, info2, i);
                    if (sectionInfo.firstAppItem == null) {
                        sectionInfo.firstAppItem = appItem;
                        fastScrollSectionInfo.fastScrollToItem = appItem;
                    }
                    this.mAdapterItems.add(appItem);
                    this.mFilteredApps.add(info2);
                    i = appIndex;
                    position2 = position;
                }
            }
        }
        String sectionName;
        if (hasFilter()) {
            for (IconInfo info22 : getFiltersAppInfos()) {
                if (!info22.isHiddenByXML()) {
                    sectionName = getAndUpdateCachedSectionName(info22.title);
                    if (sectionInfo == null || !sectionName.equals(lastSectionName)) {
                        lastSectionName = sectionName;
                        sectionInfo = new SectionInfo();
                        fastScrollSectionInfo = new FastScrollSectionInfo(sectionName);
                        this.mSections.add(sectionInfo);
                        this.mFastScrollerSections.add(fastScrollSectionInfo);
                        if (!hasFilter()) {
                            position = position2 + 1;
                            this.mAdapterItems.add(AdapterItem.asSectionBreak(position2, sectionInfo));
                            position2 = position;
                        }
                    }
                    position = position2 + 1;
                    i2 = sectionInfo.numApps;
                    sectionInfo.numApps = i2 + 1;
                    appIndex = i + 1;
                    appItem = AdapterItem.asApp(position2, sectionInfo, sectionName, i2, info22, i);
                    if (sectionInfo.firstAppItem == null) {
                        sectionInfo.firstAppItem = appItem;
                        fastScrollSectionInfo2.fastScrollToItem = appItem;
                    }
                    this.mAdapterItems.add(appItem);
                    this.mFilteredApps.add(info22);
                    i = appIndex;
                    position2 = position;
                }
            }
            if (hasNoFilteredResults()) {
                position = position2 + 1;
                this.mAdapterItems.add(AdapterItem.asEmptySearch(position2));
                position2 = position;
            }
            if (this.mGalaxyItems.size() > 0) {
                int i3 = 0;
                position = position2 + 1;
                this.mAdapterItems.add(AdapterItem.asTitle(position2));
                position2 = position + 1;
                this.mAdapterItems.add(AdapterItem.asDivider(position));
                for (ItemDetails itemDetails : getGalaxyAppsInfos()) {
                    position = position2 + 1;
                    appIndex = i + 1;
                    this.mAdapterItems.add(AdapterItem.asGalaxyApp(position2, itemDetails, i));
                    if (i3 < this.mGalaxyItems.size() - 1) {
                        position2 = position + 1;
                        this.mAdapterItems.add(AdapterItem.asDivider(position));
                        i3++;
                    } else {
                        position2 = position;
                    }
                    i = appIndex;
                }
            }
            if (LauncherFeature.supportGalaxyAppsSearch()) {
                if (isAppInstalled(GALAXYAPPS, true)) {
                    position = position2 + 1;
                    this.mAdapterItems.add(AdapterItem.asViewGalaxyButton(position2));
                    position2 = position;
                }
                if (isAppInstalled(PLAYSTORE, true)) {
                    position = position2 + 1;
                    this.mAdapterItems.add(AdapterItem.asViewMarketButton(position2));
                    position2 = position;
                }
            }
        } else if (false) {
            for (IconInfo info222 : getApps()) {
                sectionName = getAndUpdateCachedSectionName(info222.title);
                if (sectionInfo == null || !sectionName.equals(lastSectionName)) {
                    lastSectionName = sectionName;
                    sectionInfo = new SectionInfo();
                    fastScrollSectionInfo = new FastScrollSectionInfo(sectionName);
                    this.mSections.add(sectionInfo);
                    this.mFastScrollerSections.add(fastScrollSectionInfo);
                    position = position2 + 1;
                    this.mAdapterItems.add(AdapterItem.asListHeader(position2, sectionName));
                    position2 = position;
                }
                position = position2 + 1;
                i2 = sectionInfo.numApps;
                sectionInfo.numApps = i2 + 1;
                appIndex = i + 1;
                appItem = AdapterItem.asApp(position2, sectionInfo, sectionName, i2, info222, i);
                if (sectionInfo.firstAppItem == null) {
                    sectionInfo.firstAppItem = appItem;
                    fastScrollSectionInfo2.fastScrollToItem = appItem;
                }
                this.mAdapterItems.add(appItem);
                i = appIndex;
                position2 = position;
            }
        } else if (this.mRecentAppList.size() > 0) {
            for (IconInfo info2222 : getRecentAppInfos()) {
                sectionName = getAndUpdateCachedSectionName(info2222.title);
                if (sectionInfo == null || !sectionName.equals(lastSectionName)) {
                    lastSectionName = sectionName;
                    sectionInfo = new SectionInfo();
                    fastScrollSectionInfo = new FastScrollSectionInfo(sectionName);
                    this.mSections.add(sectionInfo);
                    this.mFastScrollerSections.add(fastScrollSectionInfo);
                }
                position = position2 + 1;
                i2 = sectionInfo.numApps;
                sectionInfo.numApps = i2 + 1;
                appIndex = i + 1;
                appItem = AdapterItem.asRecentApp(position2, sectionInfo, sectionName, i2, info2222, i);
                if (sectionInfo.firstAppItem == null) {
                    sectionInfo.firstAppItem = appItem;
                    fastScrollSectionInfo2.fastScrollToItem = appItem;
                }
                this.mAdapterItems.add(appItem);
                i = appIndex;
                position2 = position;
            }
        } else {
            position = position2 + 1;
            this.mAdapterItems.add(AdapterItem.asNotiText(position2));
            position2 = position;
            return;
        }
        mergeSections();
        if (this.mNumAppsPerRow != 0) {
            AdapterItem item;
            int numAppsInSection = 0;
            int numAppsInRow = 0;
            int rowIndex = -1;
            for (AdapterItem item2 : this.mAdapterItems) {
                item2.rowIndex = 0;
                if (item2.viewType == 0) {
                    numAppsInSection = 0;
                } else if (item2.viewType == 1 || item2.viewType == 2 || item2.viewType == 7) {
                    if (numAppsInSection % this.mNumAppsPerRow == 0) {
                        numAppsInRow = 0;
                        rowIndex++;
                    }
                    item2.rowIndex = rowIndex;
                    item2.rowAppIndex = numAppsInRow;
                    numAppsInSection++;
                    numAppsInRow++;
                }
            }
            this.mNumAppRowsInAdapter = rowIndex + 1;
            switch (1) {
                case 0:
                    float rowFraction = 1.0f / ((float) this.mNumAppRowsInAdapter);
                    for (FastScrollSectionInfo info3 : this.mFastScrollerSections) {
                        item2 = info3.fastScrollToItem;
                        if (item2.viewType == 1 || item2.viewType == 2 || item2.viewType == 7) {
                            info3.touchFraction = (((float) item2.rowIndex) * rowFraction) + (((float) item2.rowAppIndex) * (rowFraction / ((float) this.mNumAppsPerRow)));
                        } else {
                            info3.touchFraction = 0.0f;
                        }
                    }
                    break;
                case 1:
                    float perSectionTouchFraction = 1.0f / ((float) this.mFastScrollerSections.size());
                    float cumulativeTouchFraction = 0.0f;
                    for (FastScrollSectionInfo info32 : this.mFastScrollerSections) {
                        item2 = info32.fastScrollToItem;
                        if (item2.viewType == 1 || item2.viewType == 2 || item2.viewType == 7) {
                            info32.touchFraction = cumulativeTouchFraction;
                            cumulativeTouchFraction += perSectionTouchFraction;
                        } else {
                            info32.touchFraction = 0.0f;
                        }
                    }
                    break;
            }
        }
        Log.d(TAG, "updateAdapterItems: mAdapterItems.size()" + this.mAdapterItems.size() + " mApps.size() = " + this.mApps.size());
        if (this.mAdapter != null) {
            this.mAdapter.notifyDataSetChanged();
        }
    }

    private List<IconInfo> getFiltersAppInfos() {
        if (this.mSearchResults == null) {
            return this.mApps;
        }
        List<IconInfo> result = new ArrayList();
        Iterator it = this.mSearchResults.iterator();
        while (it.hasNext()) {
            IconInfo match = (IconInfo) this.mComponentToAppMap.get((ComponentKey) it.next());
            if (match != null) {
                result.add(match);
            }
        }
        return result;
    }

    private void mergeSections() {
        if (this.mMergeAlgorithm != null && this.mNumAppsPerRow != 0 && !hasFilter()) {
            int i = 0;
            while (i < this.mSections.size() - 1) {
                SectionInfo section = (SectionInfo) this.mSections.get(i);
                int sectionAppCount = section.numApps;
                int mergeCount = 1;
                while (i < this.mSections.size() - 1 && this.mMergeAlgorithm.continueMerging(section, (SectionInfo) this.mSections.get(i + 1), sectionAppCount, this.mNumAppsPerRow, mergeCount)) {
                    int j;
                    AdapterItem item;
                    SectionInfo nextSection = (SectionInfo) this.mSections.remove(i + 1);
                    this.mAdapterItems.remove(nextSection.sectionBreakItem);
                    int nextPos = this.mAdapterItems.indexOf(section.firstAppItem) + section.numApps;
                    for (j = nextPos; j < nextSection.numApps + nextPos; j++) {
                        item = (AdapterItem) this.mAdapterItems.get(j);
                        item.sectionInfo = section;
                        item.sectionAppIndex += section.numApps;
                    }
                    for (j = this.mAdapterItems.indexOf(nextSection.firstAppItem); j < this.mAdapterItems.size(); j++) {
                        item = (AdapterItem) this.mAdapterItems.get(j);
                        item.position--;
                    }
                    section.numApps += nextSection.numApps;
                    sectionAppCount += nextSection.numApps;
                    mergeCount++;
                }
                i++;
            }
        }
    }

    private String getAndUpdateCachedSectionName(CharSequence title) {
        String sectionName = (String) this.mCachedSectionNames.get(title);
        if (sectionName != null) {
            return sectionName;
        }
        sectionName = LocaleUtils.getInstance().makeSectionString(title.toString(), true);
        this.mCachedSectionNames.put(title, sectionName);
        return sectionName;
    }

    public int size() {
        return this.mApps.size();
    }

    public HashMap<ComponentKey, IconInfo> getAppInfos() {
        for (IconInfo info : this.mApps) {
            this.mComponentToAppMap.put(info.toComponentKey(), info);
        }
        return this.mComponentToAppMap;
    }

    public void setRecentAppMap(ArrayList<IconInfo> set) {
        this.mRecentAppList = set;
    }

    private List<IconInfo> getRecentAppInfos() {
        ArrayList<IconInfo> recent = new ArrayList();
        for (int i = 0; i < this.mRecentAppList.size(); i++) {
            IconInfo match = (IconInfo) this.mRecentAppList.get(i);
            if (match != null) {
                recent.add(match);
            }
        }
        return recent;
    }

    public void clearGalaxyItems() {
        this.called = 0;
        if (this.mGalaxyItems != null) {
            this.mGalaxyItems.clear();
        }
    }

    private List<ItemDetails> getGalaxyAppsInfos() {
        ArrayList<ItemDetails> apps = new ArrayList();
        for (int i = 0; i < this.mGalaxyItems.size(); i++) {
            ItemDetails match = (ItemDetails) this.mGalaxyItems.get(i);
            if (match != null) {
                apps.add(match);
            }
        }
        return apps;
    }

    public boolean isAppInstalled(String strAppPackage, boolean launcherCategory_check) {
        PackageManager pm = this.mLauncher.getPackageManager();
        if (launcherCategory_check && pm.getLaunchIntentForPackage(strAppPackage) == null) {
            return false;
        }
        try {
            pm.getPackageInfo(strAppPackage, 1);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    public List<IconInfo> getFilteredAppsList() {
        return this.mFilteredApps;
    }
}

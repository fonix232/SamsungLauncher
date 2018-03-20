package com.android.launcher3.allapps.controller;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.allapps.controller.AppsAdapterProvider.DataHolder;
import com.android.launcher3.allapps.controller.AppsController.ViewType;
import com.android.launcher3.allapps.model.AppsModel;
import com.android.launcher3.allapps.model.AppsModel.ModelListener;
import com.android.launcher3.allapps.view.AppsIconViewStub;
import com.android.launcher3.allapps.view.FolderIconViewStub;
import com.android.launcher3.allapps.view.Inflater;
import com.android.launcher3.allapps.view.Stub;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.customer.PostPositionController;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.model.CursorInfo;
import com.android.launcher3.common.model.DefaultLayoutParser;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.view.OnInflateListener;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.util.LongArrayMap;
import com.android.launcher3.util.ScreenGridUtilities;
import com.android.launcher3.util.StringJoiner;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

abstract class AppsAdapter implements ModelListener, Inflater {
    static final Comparator<ItemInfo> ORDER_COMPARATOR = new Comparator<ItemInfo>() {
        public int compare(ItemInfo lhs, ItemInfo rhs) {
            return ((((int) lhs.screenId) * 100) + lhs.rank) - ((((int) rhs.screenId) * 100) + rhs.rank);
        }
    };
    private static final String TAG = "AppsAdapter";
    final AppsModel mAppsModel;
    final DataHolder mDataHolder;
    private final Runnable mDeferUpdate = new Runnable() {
        public void run() {
            Log.d(AppsAdapter.TAG, "update by defer");
            AppsAdapter.this.notifyUpdate(null);
        }
    };
    private final ArrayList<Stub> mFirstLoadStub;
    private FolderLock mFolderLock;
    protected final Handler mHandler = new Handler(Looper.getMainLooper());
    private final OnInflateListener mInflateListener = new OnInflateListener() {
        public void onInflate(View stub, View inflated, boolean cancelled) {
            if (!cancelled && inflated == null) {
                Log.w(AppsAdapter.TAG, "invalid case : try inflate again and remove stub : " + stub.getTag());
                ((Stub) stub).replaceView(AppsAdapter.this.createItemView((ItemInfo) stub.getTag(), null), false);
            } else if (!(cancelled || (stub.getTag() instanceof FolderInfo))) {
                AppsAdapter.this.createItemView((ItemInfo) stub.getTag(), inflated);
            }
            if (AppsAdapter.this.mStub.contains(stub)) {
                AppsAdapter.this.mStub.remove(stub);
            }
            if (AppsAdapter.this.mFirstLoadStub.contains(stub)) {
                AppsAdapter.this.mFirstLoadStub.remove(stub);
                if (AppsAdapter.this.mFirstLoadStub.isEmpty() && !AppsAdapter.this.mDataHolder.mDestroyed) {
                    Log.d(AppsAdapter.TAG, "onLauncherBindingItemsCompleted");
                    AppsAdapter.this.mAppsModel.onLauncherBindingItemsCompleted();
                }
            }
        }
    };
    final ArrayList<ItemInfo> mItems;
    protected final Launcher mLauncher;
    protected final DataListener mListener;
    private final ArrayList<ItemInfo> mPendingUpdateItems;
    private final ArrayList<Runnable> mSavedDeferRunnables;
    private final ArrayList<Stub> mStub;
    boolean mUpdateLocked = false;

    interface DataListener {
        void addItem(View view, ItemInfo itemInfo);

        View createItemView(ItemInfo itemInfo, View view);

        View getAppsIconByItemId(long j);

        int getCellCountX();

        int getMaxItemsPerScreen();

        void makeEmptyCellAndReorder(int i, int i2);

        boolean needDeferredUpdate();

        void rearrangeAllViews(boolean z);

        void removeAllViews();

        void removeApps(ArrayList<ItemInfo> arrayList);

        void removeEmptyCellsAndViews(ArrayList<ItemInfo> arrayList);

        void updateApps(ArrayList<ItemInfo> arrayList);

        void updateGridInfo();

        void updateRestoreItems(HashSet<ItemInfo> hashSet);
    }

    protected abstract Normalizer<Object> getNormalizer();

    protected abstract String getTag();

    protected abstract ViewType getViewType();

    protected abstract void removeViewAndReorder(IconInfo iconInfo);

    protected abstract void updateAppsContents(ArrayList<ItemInfo> arrayList);

    protected abstract void updateFolderContents(LongArrayMap<ArrayList<ItemInfo>> longArrayMap);

    AppsAdapter(Context context, DataListener listener, DataHolder holder) {
        this.mLauncher = (Launcher) context;
        this.mListener = listener;
        this.mAppsModel = LauncherAppState.getInstance().getModel().getAppsModel();
        this.mDataHolder = holder;
        this.mItems = holder.mItems;
        this.mStub = holder.mStubs;
        this.mFirstLoadStub = holder.mFirstLoadStubs;
        this.mPendingUpdateItems = holder.mPendingUdpateItems;
        this.mSavedDeferRunnables = holder.mSavedDeferRunnables;
        if (LauncherFeature.supportFolderLock()) {
            this.mFolderLock = FolderLock.getInstance();
        }
    }

    void destroy() {
        this.mAppsModel.removeModelListener(this);
        this.mDataHolder.mDestroyed = true;
        this.mDataHolder.mFirstLoaded = false;
        this.mDataHolder.mModelPrepared = false;
        cancelStubTask();
        resetData();
    }

    public void resume() {
        notifyUpdate(null);
    }

    void activate() {
        Log.d(TAG, "activate adapter");
        this.mAppsModel.addModelListener(this);
    }

    void deactivate() {
        this.mAppsModel.removeModelListener(this);
    }

    public void onLoadComplete(int taskState) {
        this.mDataHolder.mModelPrepared = true;
        this.mDataHolder.mFirstLoaded = false;
        if (taskState == 1) {
            notifyUpdate(this.mAppsModel.getFolderChildUpdate());
        } else {
            notifyUpdate(null);
        }
    }

    public void notifyUpdate(ArrayList<ItemInfo> updated) {
        Log.d(getTag(), "notifyUpdate : " + this.mLauncher);
        if (waitNextUpdate()) {
            Log.d(getTag(), " waitNextUpdate1 : " + this.mLauncher);
            return;
        }
        if (!(updated == null || updated.isEmpty())) {
            synchronized (this.mPendingUpdateItems) {
                updated.removeAll(this.mPendingUpdateItems);
                this.mPendingUpdateItems.addAll(updated);
            }
        }
        if (this.mListener.needDeferredUpdate()) {
            Log.d(TAG, "addDeferredUpdateRunnable");
            addDeferredUpdateRunnable();
        } else if (!this.mDataHolder.mStopped) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    if (AppsAdapter.this.waitNextUpdate()) {
                        Log.d(AppsAdapter.TAG, "waitNextUpdate2 : " + AppsAdapter.this.mLauncher);
                        return;
                    }
                    ArrayList updateItems;
                    Log.d(AppsAdapter.TAG, "notifyUpdate start : " + AppsAdapter.this.mLauncher);
                    ArrayList<ItemInfo> items = AppsAdapter.this.adjustNewItems(AppsAdapter.this.mAppsModel.getTopLevelItemsInApps());
                    synchronized (AppsAdapter.this.mPendingUpdateItems) {
                        updateItems = new ArrayList(AppsAdapter.this.mPendingUpdateItems);
                        AppsAdapter.this.mPendingUpdateItems.clear();
                    }
                    AppsAdapter.this.updateItemsAndContents(items);
                    if (!updateItems.isEmpty()) {
                        AppsAdapter.debugItemInfo("updateItem : ", updateItems);
                        AppsAdapter.this.mListener.updateApps(updateItems);
                    }
                    AppsAdapter.this.updateFolderContents(AppsAdapter.getFolderItems(updateItems));
                    AppsAdapter.this.mDataHolder.mFirstLoaded = true;
                    Log.d(AppsAdapter.TAG, "notifyUpdate end : " + AppsAdapter.this.mLauncher);
                }
            });
        }
    }

    private boolean waitNextUpdate() {
        boolean wait = !this.mDataHolder.mModelPrepared || this.mAppsModel.isUpdateLocked() || this.mDataHolder.mDestroyed;
        if (wait) {
            Log.d(TAG, "waitNextUpdate : " + this.mDataHolder.mModelPrepared + " , " + this.mAppsModel.isUpdateLocked() + " , " + this.mDataHolder.mDestroyed);
        }
        return wait;
    }

    public void removeAllItems() {
        Log.d(TAG, "removeAllItems");
        this.mDataHolder.mFirstLoaded = false;
        this.mDataHolder.mModelPrepared = false;
        this.mHandler.post(new Runnable() {
            public void run() {
                Log.d(AppsAdapter.TAG, "cancel stub and reset data");
                AppsAdapter.this.cancelStubTask();
                AppsAdapter.this.resetData();
            }
        });
    }

    public void updateIconAndTitle(final ArrayList<ItemInfo> updated) {
        this.mHandler.post(new Runnable() {
            public void run() {
                if (AppsAdapter.this.mDataHolder.mDestroyed) {
                    Log.d(AppsAdapter.TAG, "ignore updateIconAndTitle because of destroyed : " + AppsAdapter.this.mLauncher);
                } else {
                    AppsAdapter.this.mListener.updateApps(updated);
                }
            }
        });
    }

    public void onLoadStart() {
        setupGridInfo();
    }

    public void addItemToFolder(final FolderInfo folder, final ArrayList<IconInfo> items) {
        Log.d(TAG, "post to main addItemToFolder");
        this.mHandler.post(new Runnable() {
            public void run() {
                if (!(AppsAdapter.this.mListener.getAppsIconByItemId(folder.id) instanceof FolderIconView)) {
                    IconInfo item;
                    Iterator it = items.iterator();
                    while (it.hasNext()) {
                        item = (IconInfo) it.next();
                        if (AppsAdapter.this.mItems.contains(item)) {
                            AppsAdapter.this.mItems.remove(item);
                            AppsAdapter.this.removeViewAndReorder(item);
                        }
                    }
                    ArrayList<ContentValues> valueList = new ArrayList();
                    ArrayList<ItemInfo> itemInfos = new ArrayList();
                    Iterator it2 = items.iterator();
                    while (it2.hasNext()) {
                        item = (IconInfo) it2.next();
                        try {
                            long oldParentId = item.container;
                            if (oldParentId != -102) {
                                ItemInfo oldParent = AppsAdapter.this.mAppsModel.getItemById(oldParentId);
                                if (oldParent instanceof FolderInfo) {
                                    ((FolderInfo) oldParent).remove(item);
                                    Log.e(AppsAdapter.TAG, "remove from oldParent : " + oldParent + " , " + item);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(AppsAdapter.TAG, "error while removing item from oldParent : " + e.toString());
                        }
                        item.container = folder.id;
                        item.screenId = -1;
                        item.rank = -1;
                        item.mDirty = true;
                        ContentValues update = new ContentValues();
                        update.put("container", Long.valueOf(folder.id));
                        update.put("screen", Integer.valueOf(-1));
                        update.put(BaseLauncherColumns.RANK, Integer.valueOf(-1));
                        valueList.add(update);
                        itemInfos.add(item);
                    }
                    AppsAdapter.this.updateItemsInDatabaseHelper(AppsAdapter.this.mLauncher, valueList, itemInfos);
                }
                AppsAdapter.debugItemInfo("addItemToFolder item : ", items);
                folder.add(items);
            }
        });
    }

    public void updateRestoreItems(final HashSet<ItemInfo> updates) {
        Log.d(TAG, "post to main updateRestoreItems");
        this.mHandler.post(new Runnable() {
            public void run() {
                AppsAdapter.this.mListener.updateRestoreItems(updates);
            }
        });
    }

    private void updateItemsAndContents(ArrayList<ItemInfo> items) {
        ArrayList<ItemInfo> oldItems = new ArrayList(this.mItems);
        synchronized (this.mItems) {
            this.mItems.clear();
            this.mItems.addAll(items);
        }
        updateAppsContents(oldItems);
    }

    private static LongArrayMap<ArrayList<ItemInfo>> getFolderItems(ArrayList<ItemInfo> items) {
        LongArrayMap<ArrayList<ItemInfo>> folderItems = new LongArrayMap();
        Iterator it = items.iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            if (item.container != -102) {
                ArrayList<ItemInfo> childs = (ArrayList) folderItems.get(item.container);
                if (childs == null) {
                    childs = new ArrayList();
                    folderItems.put(item.container, childs);
                }
                childs.add(item);
            }
        }
        return folderItems;
    }

    void addItemView(ArrayList<ItemInfo> added, boolean useStub) {
        if (!this.mLauncher.getVisible()) {
            Log.w(TAG, "Launcher window is still not attached");
        }
        for (int index = 0; index < added.size() && !this.mDataHolder.mDestroyed; index++) {
            ItemInfo item = (ItemInfo) added.get(index);
            if ((item instanceof IconInfo) || (item instanceof FolderInfo)) {
                View view;
                if (useStub) {
                    view = createItemViewStub(item);
                } else {
                    view = createItemView(item, null);
                }
                this.mListener.addItem(view, item);
            } else {
                Log.e(TAG, "invalid item : " + item);
            }
        }
    }

    boolean addItemView(ItemInfo item) {
        if (this.mDataHolder.mDestroyed) {
            Log.w(TAG, "Launcher destroyed");
            return false;
        }
        if (!this.mLauncher.getVisible()) {
            Log.w(TAG, "Launcher window is still not attached or destroyed");
        }
        this.mListener.addItem(createItemView(item, null), item);
        return true;
    }

    public View inflateView(ItemInfo item) {
        return item != null ? createItemView(item, null) : null;
    }

    private View createItemViewStub(ItemInfo item) {
        View view;
        if (item instanceof FolderInfo) {
            view = new FolderIconViewStub(this.mLauncher);
        } else if (item instanceof IconInfo) {
            view = new AppsIconViewStub(this.mLauncher, (int) R.layout.icon);
        } else {
            throw new IllegalArgumentException();
        }
        view.setTag(item);
        Stub stub = (Stub) view;
        stub.setInflateListener(this.mInflateListener);
        if (this.mDataHolder.mFirstLoaded) {
            this.mStub.add(stub);
        } else {
            this.mFirstLoadStub.add(stub);
        }
        stub.inflateInBackground(item);
        stub.prefetchIconResInBackground(item, this);
        return view;
    }

    private View createItemView(ItemInfo item, View recycleView) {
        if ((item instanceof FolderInfo) && LauncherFeature.supportFolderLock()) {
            FolderInfo folderInfo = (FolderInfo) item;
            if (this.mFolderLock != null && this.mFolderLock.isLockedFolder(folderInfo)) {
                this.mFolderLock.markAsLockedFolderWhenBind(folderInfo);
            }
        }
        return this.mListener.createItemView(item, recycleView);
    }

    void postDeferredRunnables() {
        if (!this.mSavedDeferRunnables.isEmpty()) {
            synchronized (this.mSavedDeferRunnables) {
                Runnable[] savedDeferRunnable = (Runnable[]) this.mSavedDeferRunnables.toArray(new Runnable[this.mSavedDeferRunnables.size()]);
                this.mSavedDeferRunnables.clear();
            }
            for (Runnable r : savedDeferRunnable) {
                this.mHandler.post(r);
            }
        }
    }

    private void addDeferredUpdateRunnable() {
        if (!this.mSavedDeferRunnables.contains(this.mDeferUpdate)) {
            synchronized (this.mSavedDeferRunnables) {
                this.mSavedDeferRunnables.add(this.mDeferUpdate);
            }
        }
    }

    public void setStateAndUpdateLock(int state, boolean updateLock) {
    }

    private ArrayList<ItemInfo> adjustNewItems(ArrayList<ItemInfo> items) {
        this.mDataHolder.mAddItems.removeAll(items);
        items.addAll(this.mDataHolder.mAddItems);
        this.mDataHolder.mRemoveItems.retainAll(items);
        items.removeAll(this.mDataHolder.mRemoveItems);
        if (!this.mDataHolder.mAddItems.isEmpty()) {
            debugItemInfo("model does not have an item which is added : ", this.mDataHolder.mAddItems);
        }
        if (!this.mDataHolder.mRemoveItems.isEmpty()) {
            debugItemInfo("model have an item which is deleted : ", this.mDataHolder.mRemoveItems);
        }
        return items;
    }

    public ItemInfo getItemById(long id) {
        synchronized (this.mItems) {
            Iterator it = this.mItems.iterator();
            while (it.hasNext()) {
                ItemInfo info = (ItemInfo) it.next();
                if (info.id == id) {
                    return info;
                }
            }
            return null;
        }
    }

    private void cancelStubTask() {
        Iterator it = this.mStub.iterator();
        while (it.hasNext()) {
            ((Stub) it.next()).cancelTask();
        }
        this.mStub.clear();
        it = this.mFirstLoadStub.iterator();
        while (it.hasNext()) {
            ((Stub) it.next()).cancelTask();
        }
        this.mStub.clear();
        this.mFirstLoadStub.clear();
    }

    public void terminate() {
        Log.d(TAG, "adapter terminate");
        destroy();
    }

    private void resetData() {
        this.mDataHolder.mItems.clear();
        this.mDataHolder.mPendingUdpateItems.clear();
        this.mDataHolder.mSavedDeferRunnables.clear();
        this.mDataHolder.mAddItems.clear();
        this.mDataHolder.mRemoveItems.clear();
        this.mListener.removeAllViews();
    }

    public long addItem(ItemInfo info) {
        debugItemInfo("addItem to apps by ui", info);
        synchronized (this.mItems) {
            if (info.isContainApps() && !this.mItems.contains(info)) {
                this.mItems.add(info);
                if (info instanceof FolderInfo) {
                    this.mDataHolder.mAddItems.add(info);
                    this.mDataHolder.mRemoveItems.remove(info);
                }
            }
        }
        return this.mAppsModel.addItem(info);
    }

    public void updateItem(ContentValues value, ItemInfo info) {
        if (!this.mUpdateLocked) {
            updateItem(info);
            this.mAppsModel.updateItem(value, info);
        }
    }

    void updateItemsInDatabaseHelper(Context context, ArrayList<ContentValues> valueList, ArrayList<ItemInfo> items) {
        if (!this.mUpdateLocked) {
            Iterator it = items.iterator();
            while (it.hasNext()) {
                updateItem((ItemInfo) it.next());
            }
            this.mAppsModel.updateItemsInDatabaseHelper(context, valueList, items);
        }
    }

    public void deleteItem(ItemInfo info) {
        debugItemInfo("deleteItem from apps by ui", info);
        synchronized (this.mItems) {
            if (this.mItems.contains(info)) {
                this.mItems.remove(info);
                if (info instanceof FolderInfo) {
                    this.mDataHolder.mRemoveItems.add(info);
                    this.mDataHolder.mAddItems.remove(info);
                }
            }
        }
        PostPositionController pp = PostPositionController.getInstance(this.mLauncher);
        if (pp.isEnabled()) {
            if (info instanceof FolderInfo) {
                pp.deleteFolder(info.id);
            } else {
                ArrayList<Long> ids = new ArrayList();
                ids.add(Long.valueOf(info.id));
                pp.deleteItems(ids);
            }
        }
        this.mAppsModel.deleteItem(info);
    }

    void updateDirtyItems() {
        ArrayList<ItemInfo> updateList = getDirtyItems();
        if (updateList == null || updateList.isEmpty()) {
            Log.d(TAG, "update dirty list is empty");
            return;
        }
        ArrayList<ContentValues> contentValues = new ArrayList();
        Iterator it = updateList.iterator();
        while (it.hasNext()) {
            ItemInfo info = (ItemInfo) it.next();
            ContentValues values = new ContentValues();
            info.onAddToDatabase(this.mLauncher, values);
            contentValues.add(values);
        }
        updateItemsInDatabaseHelper(this.mLauncher, contentValues, updateList);
    }

    private void updateItem(ItemInfo info) {
        debugItemInfo("updateItem by ui : ", info);
        synchronized (this.mItems) {
            if (info.isContainApps()) {
                synchronized (this.mItems) {
                    if (!(this.mItems.contains(info) || hasItem(info))) {
                        this.mItems.add(info);
                        if (info instanceof FolderInfo) {
                            debugItemInfo("updateItem add to apps by ui", info);
                            this.mDataHolder.mAddItems.add(info);
                            this.mDataHolder.mRemoveItems.remove(info);
                        }
                    }
                }
            } else {
                synchronized (this.mItems) {
                    if (this.mItems.contains(info)) {
                        this.mItems.remove(info);
                        if (info instanceof FolderInfo) {
                            debugItemInfo("updateItem remove from apps by ui", info);
                            this.mDataHolder.mRemoveItems.add(info);
                            this.mDataHolder.mAddItems.remove(info);
                        }
                    }
                }
            }
        }
    }

    private boolean hasItem(ItemInfo item) {
        synchronized (this.mItems) {
            Iterator it = this.mItems.iterator();
            while (it.hasNext()) {
                if (((ItemInfo) it.next()).id == item.id) {
                    return true;
                }
            }
            return false;
        }
    }

    private ArrayList<ItemInfo> getDirtyItems() {
        ArrayList<ItemInfo> updateList = new ArrayList();
        Iterator it = this.mItems.iterator();
        while (it.hasNext()) {
            ItemInfo info = (ItemInfo) it.next();
            if (info.mDirty) {
                updateList.add(info);
                info.mDirty = false;
            }
            if (info instanceof FolderInfo) {
                Iterator it2 = ((FolderInfo) info).contents.iterator();
                while (it2.hasNext()) {
                    ItemInfo child = (ItemInfo) it2.next();
                    if (child.mDirty) {
                        updateList.add(child);
                        child.mDirty = false;
                    }
                }
            }
        }
        return updateList;
    }

    private String makeFoldersIdToString(LongArrayMap<ItemInfo> folders) {
        StringJoiner joiner = new StringJoiner(",");
        Iterator it = folders.iterator();
        while (it.hasNext()) {
            joiner.append(((ItemInfo) it.next()).id);
        }
        return joiner.toString();
    }

    void reloadAllItemsFromDB(boolean withFolderContents) {
        Uri contentUri = Favorites.CONTENT_URI;
        String[] selectionArg = new String[]{String.valueOf(Favorites.CONTAINER_APPS)};
        ContentResolver contentResolver = this.mLauncher.getContentResolver();
        Cursor c = contentResolver.query(contentUri, null, "(container=?)", selectionArg, "rank ASC");
        if (c != null) {
            if (c.getCount() <= 0) {
                c.close();
                return;
            }
            long id;
            int rank;
            ItemInfo item;
            CursorInfo cursorInfo = new CursorInfo(c);
            LongArrayMap<ItemInfo> folders = new LongArrayMap();
            LongArrayMap<ItemInfo> folderContents = new LongArrayMap();
            LongArrayMap<ItemInfo> itemIdMap = new LongArrayMap();
            Iterator it = this.mItems.iterator();
            while (it.hasNext()) {
                ItemInfo itemInfo = (ItemInfo) it.next();
                itemIdMap.put(itemInfo.id, itemInfo);
            }
            while (c.moveToNext()) {
                id = c.getLong(cursorInfo.idIndex);
                int screenId = c.getInt(cursorInfo.screenIndex);
                rank = c.getInt(cursorInfo.rankIndex);
                item = (ItemInfo) itemIdMap.get(id);
                if (item != null) {
                    item.screenId = (long) screenId;
                    item.rank = rank;
                    if (withFolderContents && (item instanceof FolderInfo)) {
                        folders.put(item.id, item);
                        it = ((FolderInfo) item).contents.iterator();
                        while (it.hasNext()) {
                            ItemInfo folderContentInfo = (ItemInfo) it.next();
                            folderContents.put(folderContentInfo.id, folderContentInfo);
                        }
                    }
                } else {
                    try {
                        String component = c.getString(cursorInfo.intentIndex);
                        Log.d(TAG, "There is no item in mItems but it is exist on database. Intent : " + component + ", hidden : " + c.getInt(cursorInfo.hiddenIndex));
                    } catch (Throwable th) {
                        if (!c.isClosed()) {
                            c.close();
                        }
                    }
                }
            }
            if (!c.isClosed()) {
                c.close();
            }
            if (folders.size() > 0) {
                String selection = "container in (" + makeFoldersIdToString(folders) + ')';
                c = contentResolver.query(contentUri, null, selection, null, "rank ASC");
                if (c == null) {
                    return;
                }
                if (c.getCount() <= 0) {
                    c.close();
                    return;
                }
                while (c.moveToNext()) {
                    id = c.getLong(cursorInfo.idIndex);
                    rank = c.getInt(cursorInfo.rankIndex);
                    item = (ItemInfo) folderContents.get(id);
                    if (item != null) {
                        item.rank = rank;
                    } else {
                        try {
                            Log.d(TAG, "no item in sBgItemsIdMap for folder");
                        } finally {
                            c.close();
                        }
                    }
                }
            }
        }
    }

    private void setupGridInfo() {
        DeviceProfile dp = LauncherAppState.getInstance().getDeviceProfile();
        int[] gridXY = new int[2];
        if (LauncherAppState.getInstance().isEasyModeEnabled()) {
            gridXY[0] = dp.appsGrid.getCellCountX();
            gridXY[1] = dp.appsGrid.getCellCountY();
        } else {
            ScreenGridUtilities.loadCurrentAppsGridSize(this.mLauncher, gridXY);
            String gridSet = ScreenGridUtilities.loadAppsSupportedGridSet(this.mLauncher);
            if (gridXY[0] == -1 || gridXY[1] == -1) {
                setupDefaultGridInfo(dp, gridXY);
                setupGridSet();
            } else if (gridSet == null || gridSet.isEmpty()) {
                setupGridSet();
            }
            int cellCountX = dp.appsGrid.getCellCountX();
            int cellCountY = dp.appsGrid.getCellCountY();
            if (!(cellCountX == gridXY[0] && cellCountY == gridXY[1])) {
                Log.d(TAG, "updateGridInfo : " + gridXY[0] + " , " + gridXY[1]);
                updateAppsGridInfo(gridXY[0], gridXY[1]);
            }
        }
        Log.d(TAG, "setupGridInfo : " + gridXY[0] + " , " + gridXY[1] + " , easyMode " + LauncherAppState.getInstance().isEasyModeEnabled());
    }

    private void updateAppsGridInfo(final int gridX, final int gridY) {
        this.mHandler.post(new Runnable() {
            public void run() {
                LauncherAppState.getInstance().getDeviceProfile().setAppsCurrentGrid(gridX, gridY);
                AppsAdapter.this.mListener.updateGridInfo();
            }
        });
    }

    private void setupGridSet() {
        String[] gridSet = this.mLauncher.getResources().getStringArray(R.array.support_apps_grid_size);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < gridSet.length; index++) {
            String grid = gridSet[index];
            Log.d(TAG, "grid - index(" + index + "), value(" + grid + ")");
            if (grid == null || grid.isEmpty()) {
                throw new IllegalArgumentException("invalid grid");
            }
            builder.append(grid);
            if (index != gridSet.length - 1) {
                builder.append("|");
            }
        }
        ScreenGridUtilities.storeAppsSupportedGridSet(this.mLauncher, builder.toString());
        Log.d(TAG, "setupAppsGridSet : " + builder.toString());
    }

    private void setupDefaultGridInfo(DeviceProfile dp, int[] gridXY) {
        boolean isDreamProject;
        int gridX;
        Resources res = this.mLauncher.getResources();
        if (LauncherFeature.isDreamProject() || LauncherFeature.isCruiserProject()) {
            isDreamProject = true;
        } else {
            isDreamProject = false;
        }
        if (isDreamProject || LauncherFeature.isTablet()) {
            gridX = res.getInteger(R.integer.apps_default_cellCountX);
        } else {
            gridX = dp.homeGrid.getCellCountX();
        }
        String[] gridSet = res.getStringArray(R.array.support_apps_grid_size);
        String grid = "";
        int index = 0;
        while (index < gridSet.length) {
            grid = gridSet[index];
            Log.d(TAG, "grid - index(" + index + "), value(" + grid + ")");
            if (grid == null || grid.isEmpty()) {
                throw new IllegalArgumentException("invalid grid");
            } else if (grid.startsWith(String.valueOf(gridX))) {
                break;
            } else {
                index++;
            }
        }
        if (grid.isEmpty()) {
            gridXY[0] = res.getInteger(R.integer.apps_default_cellCountX);
            gridXY[1] = res.getInteger(R.integer.apps_default_cellCountY);
        } else {
            String[] gridValues = grid.split(DefaultLayoutParser.ATTR_X);
            gridXY[0] = Integer.valueOf(gridValues[0]).intValue();
            gridXY[1] = Integer.valueOf(gridValues[1]).intValue();
        }
        ScreenGridUtilities.storeAppsGridLayoutPreference(this.mLauncher, gridXY[0], gridXY[1]);
        Log.d(TAG, "setupDefaultAppsGridInfo : " + gridXY[0] + " , " + gridXY[1]);
    }

    static void debugItemInfo(String message, ArrayList<?> items) {
        Iterator it = items.iterator();
        while (it.hasNext()) {
            Object item = it.next();
            if (item instanceof ItemInfo) {
                debugItemInfo(message, (ItemInfo) item);
            } else {
                Log.e(TAG, "invalid item : " + message + " , " + item);
            }
        }
    }

    static void debugItemInfo(String message, ItemInfo item) {
        debugItemInfo(message, item, false);
    }

    private static void debugItemInfo(String message, ItemInfo item, boolean printStack) {
        if (item == null) {
            Log.e(TAG, "no itemInfo for debug");
            return;
        }
        StringJoiner joiner = new StringJoiner(", ");
        joiner.append("(id=" + item.id);
        joiner.append("container=" + item.container);
        joiner.append("screen=" + item.screenId);
        joiner.append("rank=" + item.rank);
        joiner.append("" + item.title);
        joiner.append("" + (item instanceof FolderInfo) + ")");
        if (printStack) {
            Log.d(TAG, message + joiner.toString(), new Exception());
        } else {
            Log.d(TAG, message + joiner.toString());
        }
    }
}

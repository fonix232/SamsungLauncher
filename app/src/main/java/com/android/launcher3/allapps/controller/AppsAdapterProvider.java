package com.android.launcher3.allapps.controller;

import android.content.ContentValues;
import android.content.Context;
import com.android.launcher3.Launcher;
import com.android.launcher3.allapps.controller.AppsController.ViewType;
import com.android.launcher3.allapps.view.Stub;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.util.LongArrayMap;
import java.util.ArrayList;

public class AppsAdapterProvider {
    private AppsAdapter mActiveAdapter;
    private LongArrayMap<AppsAdapter> mAdapters = new LongArrayMap();
    private final DataHolder mDataHolder;
    private final Launcher mLauncher;
    private final DataListener mListener;

    public static class DataHolder {
        ArrayList<ItemInfo> mAddItems = new ArrayList();
        boolean mDestroyed = false;
        ArrayList<Stub> mFirstLoadStubs = new ArrayList();
        boolean mFirstLoaded = false;
        ArrayList<ItemInfo> mItems = new ArrayList();
        boolean mModelPrepared = false;
        ArrayList<ItemInfo> mPendingUdpateItems = new ArrayList();
        ArrayList<ItemInfo> mRemoveItems = new ArrayList();
        ArrayList<Runnable> mSavedDeferRunnables = new ArrayList();
        boolean mStopped = true;
        ArrayList<Stub> mStubs = new ArrayList();
    }

    public AppsAdapterProvider(Launcher launcher, DataListener listener, ViewType type) {
        this.mLauncher = launcher;
        this.mListener = listener;
        this.mDataHolder = new DataHolder();
        this.mActiveAdapter = getAdapter(type);
        this.mActiveAdapter.activate();
    }

    public AppsAdapter createAdapter(ViewType type) {
        return type == ViewType.CUSTOM_GRID ? new AppsCustomAdapter(this.mLauncher, this.mListener, this.mDataHolder) : new AppsAlphabetAdapter(this.mLauncher, this.mListener, this.mDataHolder);
    }

    public AppsAdapter getAdapter(ViewType type) {
        AppsAdapter adapter = (AppsAdapter) this.mAdapters.get((long) type.ordinal());
        if (adapter != null) {
            return adapter;
        }
        adapter = createAdapter(type);
        this.mAdapters.put((long) type.ordinal(), adapter);
        return adapter;
    }

    public void setAdapter(ViewType type) {
        if (this.mActiveAdapter.getViewType() != type) {
            this.mActiveAdapter.deactivate();
            this.mActiveAdapter = getAdapter(type);
            this.mActiveAdapter.activate();
        }
    }

    public void setStateAndUpdateLock(int state, boolean updateLock) {
        this.mActiveAdapter.setStateAndUpdateLock(state, updateLock);
    }

    public void start() {
        this.mDataHolder.mStopped = false;
    }

    public void resume() {
        this.mActiveAdapter.resume();
    }

    public void stop() {
        this.mDataHolder.mStopped = true;
    }

    public void destroy() {
        this.mActiveAdapter.destroy();
    }

    public Normalizer<Object> getNormalizer() {
        return this.mActiveAdapter.getNormalizer();
    }

    public long addItem(ItemInfo info) {
        return this.mActiveAdapter.addItem(info);
    }

    public void updateItem(ContentValues value, ItemInfo info) {
        this.mActiveAdapter.updateItem(value, info);
    }

    public void updateItemsInDatabaseHelper(Context context, ArrayList<ContentValues> valueList, ArrayList<ItemInfo> items) {
        this.mActiveAdapter.updateItemsInDatabaseHelper(context, valueList, items);
    }

    public void deleteItem(ItemInfo info) {
        this.mActiveAdapter.deleteItem(info);
    }

    public void updateDirtyItems() {
        this.mActiveAdapter.updateDirtyItems();
    }

    public void requestRunDeferredRunnables() {
        this.mActiveAdapter.postDeferredRunnables();
    }

    public void reloadAllItemsFromDB(boolean withFolderContents) {
        this.mActiveAdapter.reloadAllItemsFromDB(withFolderContents);
    }

    public ItemInfo getItemById(long id) {
        return this.mActiveAdapter.getItemById(id);
    }
}

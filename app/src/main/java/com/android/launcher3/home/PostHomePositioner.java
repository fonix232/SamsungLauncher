package com.android.launcher3.home;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.compat.AppWidgetManagerCompat;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.customer.PostPositionItemRecord;
import com.android.launcher3.common.customer.PostPositionProvider;
import com.android.launcher3.common.customer.PostPositionProvider.ITEM_TYPE;
import com.android.launcher3.common.customer.PostPositionSharedPref;
import com.android.launcher3.common.customer.PostPositionerBase;
import com.android.launcher3.common.model.DataLoader;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.model.FavoritesUpdater;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.folder.FolderInfo;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;

public class PostHomePositioner extends PostPositionerBase {
    private static final boolean DEBUG = true;
    private static final long INVALID_VALUE = -1;
    private static final boolean SUPPORT_EXTRA_POSITION = false;
    private static final boolean SUPPORT_FOLDER_ONLY = false;
    private static final String TAG = "PostHomePositioner";
    private AppWidgetHost mAppWidgetHost;
    private FavoritesUpdater mFavoritesUpdater;
    private HomeLoader mHomeLoader;

    public PostHomePositioner(Context context, PostPositionProvider provider) {
        super(context, provider);
    }

    protected void init() {
        if (this.mAppState == null) {
            this.mAppState = LauncherAppState.getInstance();
        }
        if (this.mHomeLoader == null) {
            this.mHomeLoader = this.mAppState.getModel().getHomeLoader();
        }
        if (this.mFavoritesUpdater == null) {
            this.mFavoritesUpdater = (FavoritesUpdater) this.mHomeLoader.getUpdater();
        }
    }

    protected void setup() {
        this.mAppWidgetHost = new AppWidgetHost(this.mContext, 1024);
        this.mPrefInfo.setContainer(-100);
        init();
    }

    public synchronized boolean addItem(final PostPositionItemRecord itemRecord, final LauncherActivityInfoCompat info, final UserHandleCompat user) {
        boolean z = false;
        synchronized (this) {
            if (itemRecord.isHomeAdd()) {
                if (info == null) {
                    if (itemRecord.getItemType() == ITEM_TYPE.APP) {
                        Log.e(TAG, "addHomeItem() : info is null. - " + itemRecord.getComponentName());
                    }
                }
                Log.i(TAG, "addHomeItem() : " + itemRecord.getComponentName() + ", Type : " + itemRecord.getItemType());
                this.mModelWorker.post(new Runnable() {
                    public void run() {
                        PostHomePositioner.this.checkAndUpdatePositionInfo(itemRecord);
                        if (!PostHomePositioner.this.addItem(info, user, itemRecord)) {
                            PostHomePositioner.this.mProvider.updateItemRecordResult(false, itemRecord.getComponentName());
                            Log.e(PostHomePositioner.TAG, "addItem() result failed.");
                        }
                    }
                });
                z = true;
            }
        }
        return z;
    }

    private boolean isAlreadyExistOnHomescreen(LauncherActivityInfoCompat info, UserHandleCompat user) {
        if (info == null || user == null) {
            return false;
        }
        Iterator it = DataLoader.getItemInfoByComponentName(info.getComponentName(), user, true).iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            if (item != null) {
                long container = item.container;
                if (item.container > 0) {
                    ItemInfo folder = this.mHomeLoader.getItemById(item.container);
                    if (folder != null) {
                        container = folder.container;
                    }
                }
                if (container == -100 || container == -101) {
                    Log.d(TAG, info.getLabel() + " already exist on homescreen");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean addItem(LauncherActivityInfoCompat info, UserHandleCompat user, PostPositionItemRecord itemRecord) {
        if (itemRecord.getItemType() == ITEM_TYPE.WIDGET) {
            final ComponentName cn = ComponentName.unflattenFromString(itemRecord.getComponentName());
            if (cn != null) {
                final UserHandleCompat userHandleCompat = user;
                final PostPositionItemRecord postPositionItemRecord = itemRecord;
                this.mModelWorker.postDelayed(new Runnable() {
                    public void run() {
                        PostHomePositioner.this.addWidget(cn, userHandleCompat, postPositionItemRecord);
                        PostHomePositioner.this.mProvider.updateItemRecordResult(true, postPositionItemRecord.getComponentName());
                    }
                }, 2000);
            }
            return true;
        } else if (isAlreadyExistOnHomescreen(info, user)) {
            return true;
        } else {
            if (itemRecord.getHomeFolderName() != null && !itemRecord.getHomeFolderName().equals("")) {
                long folderId;
                if (itemRecord.isHomePreloadFolder()) {
                    folderId = this.mPrefInfo.getPreloadedFolderId(itemRecord.getHomeFolderName());
                    Log.d(TAG, "folder is preloaded folder. folderId is " + folderId);
                    if (folderId < 0) {
                        folderId = this.mPrefInfo.getFolderId(itemRecord.getHomeFolderName(), false);
                        Log.d(TAG, "request preloaded folder. but not created by xml so find other folder type : " + folderId);
                    }
                } else {
                    folderId = this.mPrefInfo.getFolderId(itemRecord.getHomeFolderName(), false);
                    Log.d(TAG, "folder is not preloaded folder. folderId is " + folderId);
                }
                if (folderId == PostPositionSharedPref.REMOVED) {
                    Log.d(TAG, itemRecord.getHomeFolderName() + " folder already removed by user.");
                    return false;
                } else if (folderId > 0) {
                    Log.d(TAG, "folder exist. folderId is " + folderId + " add to folder");
                    if (!addToFolder(info, user, folderId, itemRecord)) {
                        Log.e(TAG, "Child item isn't exist : " + info.getComponentName().flattenToShortString());
                        return false;
                    }
                } else if (itemRecord.getHomeFolderName() != null) {
                    folderId = this.mPrefInfo.getFolderId(itemRecord.getHomeFolderName(), true);
                    Log.d(TAG, "folder is not created so need to make a folder by folderId : " + folderId);
                    IconInfo hItem = (IconInfo) this.mHomeLoader.getItemById(folderId);
                    if (folderId <= 0 || hItem == null || hItem.container != -100) {
                        ItemInfo shortcutItem = addShortcut(info, user, itemRecord);
                        if (shortcutItem == null) {
                            Log.e(TAG, "addShortcut return item is null : " + info.getComponentName().flattenToShortString());
                            return false;
                        }
                        Log.d(TAG, "add item and save ready id: " + shortcutItem.id);
                        this.mPrefInfo.writeFolderId(itemRecord.getHomeFolderName(), shortcutItem.id, true);
                    } else if (hItem.componentName == null || !itemRecord.getComponentName().equals(hItem.componentName.flattenToShortString())) {
                        Log.d(TAG, "create folder from ready id");
                        ArrayList<ItemInfo> itemList = new ArrayList(2);
                        ArrayList<ItemInfo> arrayList = new ArrayList(1);
                        FolderInfo fItem = new FolderInfo();
                        fItem.id = FavoritesProvider.getInstance().generateNewItemId();
                        fItem.title = itemRecord.getHomeFolderName();
                        fItem.screenId = hItem.screenId;
                        fItem.cellX = hItem.cellX;
                        fItem.cellY = hItem.cellY;
                        IconInfo addItem1 = createIconInfo(info, itemRecord);
                        IconInfo addItem2 = hItem.makeCloneInfo();
                        addItem2.rank = 1;
                        addItem1.rank = 0;
                        addItem1.screenId = 0;
                        addItem2.screenId = 0;
                        addItem1.cellX = 0;
                        addItem2.cellX = 0;
                        addItem1.cellY = 0;
                        addItem2.cellY = 0;
                        fItem.add(addItem1);
                        fItem.add(addItem2);
                        itemList.add(fItem);
                        arrayList.add(hItem);
                        this.mHomeLoader.addAndBindAddedWorkspaceItems(this.mContext, itemList, false);
                        this.mHomeLoader.removeWorkspaceItem(arrayList);
                        long j = fItem.id;
                        addItem1.container = j;
                        addItem2.container = j;
                        this.mFavoritesUpdater.addItem(addItem1);
                        this.mFavoritesUpdater.addItem(addItem2);
                        this.mPrefInfo.removeFolderId(itemRecord.getHomeFolderName(), true);
                        this.mPrefInfo.writeFolderId(itemRecord.getHomeFolderName(), fItem.id, false);
                    } else {
                        Log.d(TAG, "already write as folder ready id");
                        return true;
                    }
                }
            } else if (addShortcut(info, user, itemRecord) == null) {
                Log.e(TAG, "Child item isn't exist : " + info.getComponentName().flattenToShortString());
                return false;
            }
            return true;
        }
    }

    private boolean addToFolder(LauncherActivityInfoCompat info, UserHandleCompat user, long folderId, PostPositionItemRecord itemRecord) {
        FolderInfo fItem = this.mHomeLoader.findFolderById(Long.valueOf(folderId));
        if (fItem == null) {
            return false;
        }
        Log.d(TAG, "addToHomeFolder() - " + fItem.title);
        ArrayList<ItemInfo> addShortcuts = new ArrayList();
        IconInfo hItem = createIconInfo(info, itemRecord);
        hItem.container = folderId;
        hItem.screenId = 0;
        hItem.cellX = 0;
        hItem.cellY = 0;
        hItem.rank = fItem.contents.size();
        addShortcuts.add(hItem);
        this.mHomeLoader.addAndBindAddedWorkspaceItems(this.mContext, addShortcuts, false);
        return true;
    }

    private IconInfo addShortcut(LauncherActivityInfoCompat info, UserHandleCompat user, PostPositionItemRecord itemRecord) {
        Log.d(TAG, "addToHomeShortcut()");
        performHomeNewPage(itemRecord, false);
        if (!itemRecord.isHomeNewPage()) {
            performHomeReplace(itemRecord);
        }
        long screenId = this.mHomeLoader.getWorkspaceScreenId(itemRecord.getHomeIndex());
        if (screenId == -1) {
            Log.d(TAG, "not exist page : " + itemRecord.getHomeIndex());
            return null;
        }
        boolean find;
        int[] emptyCell = new int[]{itemRecord.getHomeCellX(), itemRecord.getHomeCellY()};
        HomeItemPositionHelper itemPositionHelper = this.mHomeLoader.getItemPositionHelper();
        if (itemRecord.isHomeReplace()) {
            find = true;
        } else {
            find = itemPositionHelper.findEmptyCell(emptyCell, screenId, 0, 0, true);
            if (!find) {
                find = itemPositionHelper.findNearEmptyCell(emptyCell, screenId, itemRecord.getHomeCellX(), itemRecord.getHomeCellY());
            }
        }
        if (!find) {
            int screenCount = this.mHomeLoader.getWorkspaceScreenCount();
            for (int i = 0; i < screenCount && !find; i++) {
                screenId = this.mHomeLoader.getWorkspaceScreenId(i);
                if (i != itemRecord.getHomeIndex()) {
                    find = itemPositionHelper.findEmptyCell(emptyCell, screenId, 0, 0, true);
                    if (!find) {
                        find = itemPositionHelper.findNearEmptyCell(emptyCell, screenId, itemRecord.getHomeCellX(), itemRecord.getHomeCellY());
                    }
                }
            }
            if (!find) {
                screenId = this.mHomeLoader.insertWorkspaceScreen(this.mContext, screenCount, -1);
            }
        }
        IconInfo hItem = createIconInfo(info, itemRecord);
        hItem.screenId = screenId;
        hItem.cellX = emptyCell[0];
        hItem.cellY = emptyCell[1];
        hItem.spanY = 1;
        hItem.spanX = 1;
        ArrayList<ItemInfo> addShortcuts = new ArrayList();
        addShortcuts.add(hItem);
        this.mHomeLoader.addAndBindAddedWorkspaceItems(this.mContext, addShortcuts, false);
        return hItem;
    }

    private void addWidget(ComponentName cn, UserHandleCompat user, PostPositionItemRecord itemRecord) {
        ArrayList<ItemInfo> widgets = DataLoader.getItemInfoByComponentName(cn, user, false);
        if (widgets == null || widgets.size() <= 0) {
            Log.d(TAG, "addToHomeWidget()");
            performHomeNewPage(itemRecord, false);
            if (!itemRecord.isHomeNewPage()) {
                performHomeReplace(itemRecord);
            }
            HomeItemPositionHelper itemPositionHelper = this.mHomeLoader.getItemPositionHelper();
            long screenId = this.mHomeLoader.getWorkspaceScreenId(itemRecord.getHomeIndex());
            if (screenId == -1) {
                Log.d(TAG, "not exist page : " + itemRecord.getHomeIndex());
                return;
            }
            boolean find;
            int[] emptyCell = new int[2];
            if (itemRecord.isHomeReplace()) {
                find = true;
            } else {
                emptyCell[0] = itemRecord.getHomeCellX();
                emptyCell[1] = itemRecord.getHomeCellY();
                find = itemPositionHelper.findEmptyCell(emptyCell, screenId, itemRecord.getWidgetSpanX(), itemRecord.getWidgetSpanY(), true);
            }
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.mContext);
            try {
                int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
                if (appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn)) {
                    LauncherAppWidgetProviderInfo info = LauncherAppWidgetProviderInfo.fromProviderInfo(this.mContext, AppWidgetManagerCompat.getInstance(this.mContext).getAppWidgetInfo(appWidgetId));
                    LauncherAppWidgetInfo launcherInfo = new LauncherAppWidgetInfo(appWidgetId, cn);
                    launcherInfo.spanX = itemRecord.getWidgetSpanX();
                    launcherInfo.spanY = itemRecord.getWidgetSpanY();
                    launcherInfo.minSpanX = info.getMinSpanX();
                    launcherInfo.minSpanY = info.getMinSpanY();
                    launcherInfo.user = UserHandleCompat.fromUser(info.getProfile());
                    launcherInfo.screenId = screenId;
                    launcherInfo.cellX = itemRecord.getHomeCellX();
                    launcherInfo.cellY = itemRecord.getHomeCellY();
                    if (!find) {
                        if (itemPositionHelper.findEmptyCell(emptyCell, screenId, itemRecord.getWidgetSpanX(), itemRecord.getWidgetSpanY(), true)) {
                            launcherInfo.cellX = emptyCell[0];
                            launcherInfo.cellY = emptyCell[1];
                        } else {
                            screenId = performHomeNewPage(itemRecord, true);
                            if (screenId != -1) {
                                launcherInfo.screenId = screenId;
                            }
                        }
                    }
                    ArrayList<ItemInfo> addWidgets = new ArrayList();
                    addWidgets.add(launcherInfo);
                    this.mHomeLoader.addAndBindAddedWorkspaceItems(this.mContext, addWidgets, false);
                    return;
                }
                Log.e(TAG, "Failed to initialize external widget");
                return;
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to initialize external widget", e);
                return;
            }
        }
        Log.d(TAG, "already exist widget on workspace.");
    }

    private void checkAndUpdatePositionInfo(PostPositionItemRecord itemRecord) {
        int index = itemRecord.getHomeIndex();
        int x = itemRecord.getHomeCellX();
        int y = itemRecord.getHomeCellY();
        int spanX = itemRecord.getWidgetSpanX();
        int spanY = itemRecord.getWidgetSpanY();
        if (index >= this.mHomeLoader.getWorkspaceScreenCount()) {
            index = this.mHomeLoader.getWorkspaceScreenCount() - 1;
        } else if (index < 0) {
            index = 0;
        }
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        int[] gridSize = new int[]{this.mAppState.getDeviceProfile().homeGrid.getCellCountX(), this.mAppState.getDeviceProfile().homeGrid.getCellCountY()};
        if (x > gridSize[0] - 1) {
            x = gridSize[0] - 1;
        }
        if (y > gridSize[1] - 1) {
            y = gridSize[1] - 1;
        }
        if (itemRecord.getItemType() == ITEM_TYPE.WIDGET) {
            if (spanX < 1) {
                spanX = 1;
            }
            if (spanY < 1) {
                spanY = 1;
            }
            if (spanX > gridSize[0]) {
                spanX = gridSize[0];
            }
            if (spanY > gridSize[1]) {
                spanY = gridSize[1];
            }
            if (x + spanX > gridSize[0]) {
                spanX = gridSize[0] - x;
            }
            if (y + spanY > gridSize[1]) {
                spanY = gridSize[1] - y;
            }
            itemRecord.setWidgetSpanXY(spanX, spanY);
        }
        if (itemRecord.getItemType() == ITEM_TYPE.SHORTCUT) {
            String shortcutTitle = itemRecord.getHomeShortcutTitle();
            Bitmap shortcutIcon = itemRecord.getHomeShortcutIcon();
            if (shortcutTitle == null || shortcutIcon == null) {
                shortcutTitle = "Untitled";
                shortcutIcon = this.mAppState.getIconCache().getDefaultIcon(UserHandleCompat.myUserHandle());
            }
            itemRecord.setShortcutInfo(shortcutTitle, shortcutIcon);
        }
        itemRecord.setHomePosition(itemRecord.isHomeNewPage(), itemRecord.isHomeReplace(), index, x, y);
    }

    private IconInfo createIconInfo(LauncherActivityInfoCompat info, PostPositionItemRecord itemRecord) {
        if (itemRecord.getItemType() == ITEM_TYPE.APP) {
            return IconInfo.fromActivityInfo(info, this.mContext);
        }
        if (itemRecord.getItemType() == ITEM_TYPE.SHORTCUT) {
            return this.mHomeLoader.infoFromShortcutIntent(this.mContext, itemRecord.getShorcutIntent());
        }
        return null;
    }

    private long performHomeNewPage(PostPositionItemRecord itemRecord, boolean force) {
        if (!force) {
            return -1;
        }
        if (!itemRecord.isHomeNewPage() && !force) {
            return -1;
        }
        Log.i(TAG, "performHomeNewPage()");
        this.mProvider.disableHomeNewPage(itemRecord.getHomeIndex());
        return this.mHomeLoader.insertWorkspaceScreen(this.mContext, itemRecord.getHomeIndex() + 1, -1);
    }

    private void performHomeReplace(PostPositionItemRecord itemRecord) {
    }

    private boolean inArea(Rect A, Rect B) {
        int[] gridSize = new int[]{this.mAppState.getDeviceProfile().homeGrid.getCellCountX(), this.mAppState.getDeviceProfile().homeGrid.getCellCountY()};
        boolean[][] occupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{gridSize[0], gridSize[1]});
        int x = A.left;
        while (x <= A.right && x < gridSize[0]) {
            int y = A.top;
            while (y <= A.bottom && y < gridSize[1]) {
                occupied[x][y] = true;
                y++;
            }
            x++;
        }
        x = B.left;
        while (x <= B.right && x < gridSize[0]) {
            y = B.top;
            while (y <= B.bottom && y < gridSize[1]) {
                if (occupied[x][y]) {
                    return true;
                }
                y++;
            }
            x++;
        }
        return false;
    }

    void deleteHomeArea(int homeIndex, Rect rectB) {
        long screenId = this.mHomeLoader.getWorkspaceScreenId(homeIndex);
        ArrayList<ItemInfo> removeList = new ArrayList();
        String whereCondition = "screen=" + screenId + " AND " + "container" + "=" + -100;
        Cursor c = this.mContext.getContentResolver().query(Favorites.CONTENT_URI, new String[]{"_id", "itemType", "cellX", "cellY", "spanX", "spanY"}, whereCondition, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                try {
                    long itemId = c.getLong(0);
                    int cellX = c.getInt(2);
                    int cellY = c.getInt(3);
                    if (inArea(new Rect(cellX, cellY, (cellX + c.getInt(4)) - 1, (cellY + c.getInt(5)) - 1), rectB)) {
                        removeList.add(this.mHomeLoader.getItemById(itemId));
                    }
                } finally {
                    c.close();
                }
            }
        }
        Log.d(TAG, "deleteHomeArea() - " + removeList.size() + " items removed.");
        if (!removeList.isEmpty()) {
            this.mHomeLoader.removeWorkspaceItem(removeList);
        }
    }

    protected boolean hasItem(long id, boolean isFolder) {
        if (isFolder) {
            if (this.mHomeLoader.findFolderById(Long.valueOf(id)) != null) {
                return true;
            }
            return false;
        } else if (this.mHomeLoader.getItemById(id) == null) {
            return false;
        } else {
            return true;
        }
    }
}

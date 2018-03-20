package com.android.launcher3.common.bnr.google;

import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.app.backup.BackupManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherProvider;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.bnr.LauncherBnrHelper;
import com.android.launcher3.common.bnr.LauncherBnrTag;
import com.android.launcher3.common.bnr.google.BackupProtos.CheckedMessage;
import com.android.launcher3.common.bnr.google.BackupProtos.DeviceProfileData;
import com.android.launcher3.common.bnr.google.BackupProtos.Favorite;
import com.android.launcher3.common.bnr.google.BackupProtos.Journal;
import com.android.launcher3.common.bnr.google.BackupProtos.Key;
import com.android.launcher3.common.bnr.google.BackupProtos.Resource;
import com.android.launcher3.common.bnr.google.BackupProtos.Screen;
import com.android.launcher3.common.bnr.google.BackupProtos.Widget;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.common.model.LauncherSettings.ChangeLogColumns;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens;
import com.android.launcher3.home.HomeLoader;
import com.android.launcher3.util.BitmapUtils;
import com.android.launcher3.util.ScreenGridUtilities;
import com.google.protobuf.nano.InvalidProtocolBufferNanoException;
import com.google.protobuf.nano.MessageNano;
import com.sec.android.app.launcher.R;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.zip.CRC32;

class LauncherBackupHelper implements BackupHelper {
    private static final int APPWIDGET_ID_INDEX = 4;
    private static final int APPWIDGET_PROVIDER_INDEX = 3;
    private static final int BACKUP_INVALID_VALUE = -1;
    private static final int BACKUP_VERSION = 4;
    private static final int CELLX_INDEX = 5;
    private static final int CELLY_INDEX = 6;
    private static final int CONTAINER_INDEX = 7;
    private static final boolean DEBUG = false;
    private static final String[] FAVORITE_PROJECTION = new String[]{"_id", ChangeLogColumns.MODIFIED, "intent", Favorites.APPWIDGET_PROVIDER, Favorites.APPWIDGET_ID, "cellX", "cellY", "container", "icon", "iconPackage", "iconResource", BaseLauncherColumns.ICON_TYPE, "itemType", "screen", "spanX", "spanY", "title", BaseLauncherColumns.PROFILE_ID, BaseLauncherColumns.RANK};
    private static final int ICON_INDEX = 8;
    private static final int ICON_PACKAGE_INDEX = 9;
    private static final int ICON_RESOURCE_INDEX = 10;
    private static final int ICON_TYPE_INDEX = 11;
    private static final int ID_INDEX = 0;
    private static final int ID_MODIFIED = 1;
    private static final int INTENT_INDEX = 2;
    private static final int ITEM_TYPE_INDEX = 12;
    private static final String JOURNAL_KEY = "#";
    private static final int MAX_ICONS_PER_PASS = 10;
    private static final int MAX_JOURNAL_SIZE = 1000000;
    private static final int MAX_WIDGETS_PER_PASS = 5;
    private static final int RANK_INDEX = 18;
    private static final int SCREEN_INDEX = 13;
    private static final String[] SCREEN_PROJECTION = new String[]{"_id", ChangeLogColumns.MODIFIED, WorkspaceScreens.SCREEN_RANK};
    private static final int SCREEN_RANK_INDEX = 2;
    private static final int SPANX_INDEX = 14;
    private static final int SPANY_INDEX = 15;
    private static final String TAG = "LauncherBackupHelper";
    private static final int TITLE_INDEX = 16;
    private static final boolean VERBOSE = false;
    private boolean mBackupDataWasUpdated;
    private BackupManager mBackupManager;
    private byte[] mBuffer = new byte[512];
    private final Context mContext;
    private DeviceProfileData mDeviceProfileData;
    private final HashSet<String> mExistingKeys;
    private IconCache mIconCache;
    private boolean mIsHomeOnly = false;
    private final ArrayList<Key> mKeys;
    private long mLastBackupRestoreTime;
    private final long mUserSerial;
    boolean restoreSuccessful;
    private int restoredBackupVersion = 1;
    boolean switchDb;
    private HashSet<String> widgetSizes = new HashSet();

    private static class InvalidBackupException extends IOException {
        private static final long serialVersionUID = 8931456637211665082L;

        private InvalidBackupException(Throwable cause) {
            super(cause);
        }

        private InvalidBackupException(String reason) {
            super(reason);
        }
    }

    public LauncherBackupHelper(Context context) {
        this.mContext = context;
        this.mExistingKeys = new HashSet();
        this.mKeys = new ArrayList();
        this.restoreSuccessful = true;
        this.switchDb = false;
        this.mUserSerial = UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(UserHandleCompat.myUserHandle());
    }

    private void dataChanged() {
        if (this.mBackupManager == null) {
            this.mBackupManager = new BackupManager(this.mContext);
        }
        this.mBackupManager.dataChanged();
    }

    private void applyJournal(Journal journal) {
        this.mLastBackupRestoreTime = journal.t;
        this.mExistingKeys.clear();
        if (journal.key != null) {
            for (Key key : journal.key) {
                this.mExistingKeys.add(keyToBackupKey(key));
            }
        }
        this.restoredBackupVersion = journal.backupVersion;
    }

    public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {
        Journal in = readJournal(oldState);
        if (launcherIsReady()) {
            if (this.mDeviceProfileData == null) {
                this.mDeviceProfileData = new DeviceProfileData();
            }
            LauncherAppState app = LauncherAppState.getInstance();
            initDeviceProfileData();
            this.mIconCache = app.getIconCache();
            Log.v(TAG, "lastBackupTime = " + in.t);
            this.mKeys.clear();
            applyJournal(in);
            long newBackupTime = System.currentTimeMillis();
            this.mBackupDataWasUpdated = false;
            try {
                backupFavorites(data, false);
                backupScreens(data, false);
                backupIcons(data);
                backupWidgets(data, false);
                if (LauncherFeature.supportHomeModeChange()) {
                    backupFavorites(data, true);
                    backupScreens(data, true);
                    backupWidgets(data, true);
                }
                HashSet<String> validKeys = new HashSet();
                Iterator it = this.mKeys.iterator();
                while (it.hasNext()) {
                    validKeys.add(keyToBackupKey((Key) it.next()));
                }
                this.mExistingKeys.removeAll(validKeys);
                it = this.mExistingKeys.iterator();
                while (it.hasNext()) {
                    data.writeEntityHeader((String) it.next(), -1);
                    this.mBackupDataWasUpdated = true;
                }
                this.mExistingKeys.clear();
                if (!this.mBackupDataWasUpdated) {
                    boolean z = (in.profile != null && Arrays.equals(MessageNano.toByteArray(in.profile), MessageNano.toByteArray(this.mDeviceProfileData)) && in.backupVersion == 4 && in.appVersion == getAppVersion()) ? false : true;
                    this.mBackupDataWasUpdated = z;
                }
                if (this.mBackupDataWasUpdated) {
                    this.mLastBackupRestoreTime = newBackupTime;
                    writeRowToBackup(JOURNAL_KEY, getCurrentStateJournal(), data);
                }
            } catch (IOException e) {
                Log.e(TAG, "launcher backup has failed", e);
            }
            writeNewStateDescription(newState);
            return;
        }
        dataChanged();
        writeJournal(newState, in);
    }

    private boolean isBackupCompatible(Journal oldState) {
        DeviceProfileData currentProfile = this.mDeviceProfileData;
        DeviceProfileData oldProfile = oldState.profile;
        if (oldProfile == null || oldProfile.cols <= 0 || oldProfile.rows <= 0) {
            return false;
        }
        if (LauncherFeature.supportFlexibleGrid()) {
            return true;
        }
        if (currentProfile.cols < oldProfile.cols || currentProfile.rows < oldProfile.rows) {
            return false;
        }
        return true;
    }

    public void restoreEntity(BackupDataInputStream data) {
        if (this.restoreSuccessful) {
            if (this.mDeviceProfileData == null) {
                this.mDeviceProfileData = new DeviceProfileData();
                this.mIconCache = new IconCache(this.mContext, this.mContext.getResources().getDimensionPixelSize(R.dimen.app_icon_size));
                initDeviceProfileData();
            }
            int dataSize = data.size();
            if (this.mBuffer.length < dataSize) {
                this.mBuffer = new byte[dataSize];
            }
            try {
                int bytesRead = data.read(this.mBuffer, 0, dataSize);
                String backupKey = data.getKey();
                if (JOURNAL_KEY.equals(backupKey)) {
                    if (this.mKeys.isEmpty()) {
                        Journal journal = new Journal();
                        MessageNano.mergeFrom(journal, readCheckedBytes(this.mBuffer, dataSize));
                        applyJournal(journal);
                        this.restoreSuccessful = isBackupCompatible(journal);
                        restoreProfile(journal);
                        return;
                    }
                    Log.wtf(TAG, keyToBackupKey((Key) this.mKeys.get(0)) + " received after " + JOURNAL_KEY);
                    this.restoreSuccessful = false;
                } else if (this.mExistingKeys.isEmpty() || this.mExistingKeys.contains(backupKey)) {
                    Key key = backupKeyToKey(backupKey);
                    this.mKeys.add(key);
                    switch (key.type) {
                        case 1:
                            restoreFavorite(key, this.mBuffer, dataSize, false);
                            return;
                        case 2:
                            restoreScreen(key, this.mBuffer, dataSize, false);
                            return;
                        case 3:
                            restoreIcon(key, this.mBuffer, dataSize);
                            return;
                        case 4:
                            restoreWidget(key, this.mBuffer, dataSize);
                            return;
                        case 5:
                            if (LauncherFeature.supportHomeModeChange()) {
                                restoreFavorite(key, this.mBuffer, dataSize, true);
                                return;
                            }
                            return;
                        case 6:
                            if (LauncherFeature.supportHomeModeChange()) {
                                restoreScreen(key, this.mBuffer, dataSize, true);
                                return;
                            }
                            return;
                        case 7:
                            if (LauncherFeature.supportHomeModeChange()) {
                                restoreWidget(key, this.mBuffer, dataSize);
                                return;
                            }
                            return;
                        default:
                            Log.w(TAG, "unknown restore entity type: " + key.type);
                            this.mKeys.remove(key);
                            return;
                    }
                }
            } catch (IOException e) {
                Log.w(TAG, "ignoring unparsable backup entry", e);
            }
        }
    }

    public void writeNewStateDescription(ParcelFileDescriptor newState) {
        writeJournal(newState, getCurrentStateJournal());
    }

    private Journal getCurrentStateJournal() {
        Journal journal = new Journal();
        journal.t = this.mLastBackupRestoreTime;
        journal.key = (Key[]) this.mKeys.toArray(new Key[this.mKeys.size()]);
        journal.appVersion = getAppVersion();
        journal.backupVersion = 4;
        journal.profile = this.mDeviceProfileData;
        return journal;
    }

    private int getAppVersion() {
        int i = 0;
        try {
            return this.mContext.getPackageManager().getPackageInfo(this.mContext.getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            return i;
        }
    }

    private void initDeviceProfileData() {
        SharedPreferences prefs = this.mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        this.mIsHomeOnly = false;
        if (LauncherFeature.supportHomeModeChange() && !prefs.getBoolean(LauncherProvider.EMPTY_DATABASE_CREATED, false)) {
            this.mIsHomeOnly = prefs.getBoolean(LauncherAppState.HOME_ONLY_MODE, false);
        }
        int[] cellXY = new int[2];
        getGridSize(cellXY, false);
        this.mDeviceProfileData.cols = cellXY[0];
        this.mDeviceProfileData.rows = cellXY[1];
        getGridSize(cellXY, true);
        this.mDeviceProfileData.colsHomeOnly = cellXY[0];
        this.mDeviceProfileData.rowsHomeOnly = cellXY[1];
        this.mDeviceProfileData.homeIndex = prefs.getInt(LauncherFiles.HOME_DEFAULT_PAGE_KEY, 0);
        if (LauncherFeature.supportHomeModeChange()) {
            this.mDeviceProfileData.homeIndexHomeOnly = prefs.getInt(LauncherFiles.HOMEONLY_DEFAULT_PAGE_KEY, 0);
            return;
        }
        this.mDeviceProfileData.homeIndexHomeOnly = -1;
    }

    private void getGridSize(int[] cellXY, boolean isHomeOnlyData) {
        cellXY[1] = -1;
        cellXY[0] = -1;
        if (!isHomeOnlyData || LauncherFeature.supportHomeModeChange()) {
            if (LauncherFeature.supportFlexibleGrid()) {
                ScreenGridUtilities.loadCurrentGridSize(this.mContext, cellXY, isHomeOnlyData);
            }
            if (cellXY[0] == -1 || cellXY[1] == -1) {
                cellXY[0] = this.mContext.getResources().getInteger(R.integer.home_cellCountX);
                cellXY[1] = this.mContext.getResources().getInteger(R.integer.home_cellCountY);
            }
        }
    }

    private void backupFavorites(BackupDataOutput data, boolean isHomeOnlyData) throws IOException {
        Cursor cursor = this.mContext.getContentResolver().query(LauncherBnrHelper.getFavoritesUri(isHomeOnlyData ? LauncherBnrTag.TAG_HOMEONLY : "home", this.mIsHomeOnly), FAVORITE_PROJECTION, LauncherBnrHelper.getUserSelectionArg(this.mContext), null, null);
        if (cursor != null) {
            if (isHomeOnlyData && cursor.getCount() == 0) {
                this.mDeviceProfileData.colsHomeOnly = -1;
                this.mDeviceProfileData.rowsHomeOnly = -1;
                this.mDeviceProfileData.homeIndexHomeOnly = -1;
                cursor.close();
                return;
            }
            int type = isHomeOnlyData ? 5 : 1;
            try {
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    long updateTime = cursor.getLong(1);
                    Key key = getKey(type, id);
                    this.mKeys.add(key);
                    if (!this.mExistingKeys.contains(keyToBackupKey(key)) || updateTime >= this.mLastBackupRestoreTime || this.restoredBackupVersion < 4) {
                        writeRowToBackup(key, packFavorite(cursor), data);
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

    private void restoreProfile(Journal oldState) {
        if (this.restoreSuccessful) {
            DeviceProfileData profile = oldState.profile;
            if (profile != null) {
                LauncherAppState app = LauncherAppState.getInstance();
                if (!LauncherFeature.supportHomeModeChange() || profile.colsHomeOnly <= -1 || profile.rowsHomeOnly <= -1 || !app.isHomeOnlyModeEnabled()) {
                    app.writeHomeOnlyModeEnabled(false);
                } else {
                    this.switchDb = true;
                }
                if (LauncherFeature.supportFlexibleGrid()) {
                    int[] cellXY = new int[2];
                    if (ScreenGridUtilities.findNearestGridSize(this.mContext, cellXY, profile.cols, profile.rows)) {
                        ScreenGridUtilities.storeGridLayoutPreference(this.mContext, cellXY[0], cellXY[1], false);
                        if (LauncherFeature.supportHomeModeChange() && profile.colsHomeOnly > -1 && profile.rowsHomeOnly > -1) {
                            if (ScreenGridUtilities.findNearestGridSize(this.mContext, cellXY, profile.colsHomeOnly, profile.rowsHomeOnly)) {
                                ScreenGridUtilities.storeGridLayoutPreference(this.mContext, cellXY[0], cellXY[1], true);
                            } else {
                                this.restoreSuccessful = false;
                                return;
                            }
                        }
                        Utilities.loadCurrentGridSize(this.mContext, cellXY);
                        app.getDeviceProfile().setCurrentGrid(cellXY[0], cellXY[1]);
                    } else {
                        this.restoreSuccessful = false;
                        return;
                    }
                }
                Editor editor = this.mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
                editor.putInt(LauncherFiles.HOME_DEFAULT_PAGE_KEY, profile.homeIndex);
                if (LauncherFeature.supportHomeModeChange() && profile.homeIndexHomeOnly != -1) {
                    editor.putInt(LauncherFiles.HOMEONLY_DEFAULT_PAGE_KEY, profile.homeIndexHomeOnly);
                }
                editor.apply();
            }
        }
    }

    private void restoreFavorite(Key key, byte[] buffer, int dataSize, boolean isHomeOnlyData) throws IOException {
        this.mContext.getContentResolver().insert(LauncherBnrHelper.getFavoritesUri(isHomeOnlyData ? LauncherBnrTag.TAG_HOMEONLY : "home", this.mIsHomeOnly), unpackFavorite(buffer, dataSize));
    }

    private void backupScreens(BackupDataOutput data, boolean isHomeOnlyData) throws IOException {
        Cursor cursor = this.mContext.getContentResolver().query(LauncherBnrHelper.getWorkspaceScreenUri(isHomeOnlyData ? LauncherBnrTag.TAG_HOMEONLY : "home", this.mIsHomeOnly), SCREEN_PROJECTION, null, null, null);
        if (cursor != null) {
            int type = isHomeOnlyData ? 6 : 2;
            try {
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    long updateTime = cursor.getLong(1);
                    Key key = getKey(type, id);
                    this.mKeys.add(key);
                    if (!this.mExistingKeys.contains(keyToBackupKey(key)) || updateTime >= this.mLastBackupRestoreTime) {
                        writeRowToBackup(key, packScreen(cursor), data);
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

    private void restoreScreen(Key key, byte[] buffer, int dataSize, boolean isHomeOnlyData) throws IOException {
        this.mContext.getContentResolver().insert(LauncherBnrHelper.getWorkspaceScreenUri(isHomeOnlyData ? LauncherBnrTag.TAG_HOMEONLY : "home", this.mIsHomeOnly), unpackScreen(buffer, dataSize));
    }

    private void backupIcons(BackupDataOutput data) throws IOException {
        ContentResolver cr = this.mContext.getContentResolver();
        int dpi = this.mContext.getResources().getDisplayMetrics().densityDpi;
        UserHandleCompat myUserHandle = UserHandleCompat.myUserHandle();
        int backupUpIconCount = 0;
        Cursor cursor = cr.query(Favorites.CONTENT_URI, FAVORITE_PROJECTION, "(itemType=0 OR itemType=1) AND " + LauncherBnrHelper.getUserSelectionArg(this.mContext), null, null);
        if (cursor != null) {
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                try {
                    Intent intent = Intent.parseUri(cursor.getString(2), 0);
                    ComponentName cn = intent.getComponent();
                    Key key = null;
                    String backupKey = null;
                    if (cn != null) {
                        key = getKey(3, cn.flattenToShortString());
                        backupKey = keyToBackupKey(key);
                    } else {
                        Log.w(TAG, "empty intent on application favorite: " + id);
                    }
                    if (this.mExistingKeys.contains(backupKey)) {
                        this.mKeys.add(key);
                    } else if (backupKey != null) {
                        if (backupUpIconCount < 10) {
                            Bitmap icon = this.mIconCache.getIcon(intent, myUserHandle);
                            if (!(icon == null || this.mIconCache.isDefaultIcon(icon, myUserHandle))) {
                                writeRowToBackup(key, packIcon(dpi, icon), data);
                                this.mKeys.add(key);
                                backupUpIconCount++;
                            }
                        } else {
                            dataChanged();
                        }
                    }
                } catch (URISyntaxException e) {
                    Log.e(TAG, "invalid URI on application favorite: " + id);
                } catch (IOException e2) {
                    Log.e(TAG, "unable to save application icon for favorite: " + id);
                } catch (Throwable th) {
                    cursor.close();
                }
            }
            cursor.close();
        }
    }

    private void restoreIcon(Key key, byte[] buffer, int dataSize) throws IOException {
        Resource res = (Resource) unpackProto(new Resource(), buffer, dataSize);
        Bitmap icon = BitmapFactory.decodeByteArray(res.data, 0, res.data.length);
        if (icon == null) {
            Log.w(TAG, "failed to unpack icon for " + key.name);
        } else {
            this.mIconCache.preloadIcon(ComponentName.unflattenFromString(key.name), icon, res.dpi, "", this.mUserSerial);
        }
    }

    private void backupWidgets(BackupDataOutput data, boolean isHomeOnlyData) throws IOException {
        ContentResolver cr = this.mContext.getContentResolver();
        int dpi = this.mContext.getResources().getDisplayMetrics().densityDpi;
        int backupWidgetCount = 0;
        Cursor cursor = cr.query(LauncherBnrHelper.getFavoritesUri(isHomeOnlyData ? LauncherBnrTag.TAG_HOMEONLY : "home", this.mIsHomeOnly), FAVORITE_PROJECTION, "itemType=4 AND " + LauncherBnrHelper.getUserSelectionArg(this.mContext), null, null);
        if (cursor != null) {
            int type = isHomeOnlyData ? 7 : 4;
            try {
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    String providerName = cursor.getString(3);
                    ComponentName provider = ComponentName.unflattenFromString(providerName);
                    Key key = null;
                    String backupKey = null;
                    if (provider != null) {
                        key = getKey(type, providerName);
                        backupKey = keyToBackupKey(key);
                    } else {
                        Log.w(TAG, "empty intent on appwidget: " + id);
                    }
                    if (this.mExistingKeys.contains(backupKey) && this.restoredBackupVersion >= 3) {
                        this.mKeys.add(key);
                    } else if (backupKey != null) {
                        if (backupWidgetCount < 5) {
                            writeRowToBackup(key, packWidget(dpi, provider, UserHandleCompat.myUserHandle()), data);
                            this.mKeys.add(key);
                            backupWidgetCount++;
                        } else {
                            dataChanged();
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

    private void restoreWidget(Key key, byte[] buffer, int dataSize) throws IOException {
        Widget widget = (Widget) unpackProto(new Widget(), buffer, dataSize);
        if (widget.icon.data != null) {
            Bitmap icon = BitmapFactory.decodeByteArray(widget.icon.data, 0, widget.icon.data.length);
            if (icon == null) {
                Log.w(TAG, "failed to unpack widget icon for " + key.name);
            } else {
                this.mIconCache.preloadIcon(ComponentName.unflattenFromString(widget.provider), icon, widget.icon.dpi, widget.label, this.mUserSerial);
            }
        }
        this.widgetSizes.add(widget.provider + JOURNAL_KEY + widget.minSpanX + "," + widget.minSpanY);
    }

    private Key getKey(int type, long id) {
        Key key = new Key();
        key.type = type;
        key.id = id;
        key.checksum = checkKey(key);
        return key;
    }

    private Key getKey(int type, String name) {
        Key key = new Key();
        key.type = type;
        key.name = name;
        key.checksum = checkKey(key);
        return key;
    }

    private String keyToBackupKey(Key key) {
        return Base64.encodeToString(MessageNano.toByteArray(key), 2);
    }

    private Key backupKeyToKey(String backupKey) throws InvalidBackupException {
        try {
            Key key = Key.parseFrom(Base64.decode(backupKey, 0));
            if (key.checksum == checkKey(key)) {
                return key;
            }
            throw new InvalidBackupException("invalid key read from stream" + backupKey);
        } catch (Throwable e) {
            throw new InvalidBackupException(e);
        } catch (Throwable e2) {
            throw new InvalidBackupException(e2);
        }
    }

    private long checkKey(Key key) {
        CRC32 checksum = new CRC32();
        checksum.update(key.type);
        checksum.update((int) (key.id & 65535));
        checksum.update((int) ((key.id >> 32) & 65535));
        if (!TextUtils.isEmpty(key.name)) {
            checksum.update(key.name.getBytes());
        }
        return checksum.getValue();
    }

    private Favorite packFavorite(Cursor c) {
        Favorite favorite = new Favorite();
        favorite.id = c.getLong(0);
        favorite.screen = c.getInt(13);
        favorite.container = c.getInt(7);
        favorite.cellX = c.getInt(5);
        favorite.cellY = c.getInt(6);
        favorite.spanX = c.getInt(14);
        favorite.spanY = c.getInt(15);
        favorite.iconType = c.getInt(11);
        favorite.rank = c.getInt(18);
        String title = c.getString(16);
        if (!TextUtils.isEmpty(title)) {
            favorite.title = title;
        }
        String intentDescription = c.getString(2);
        Intent intent = null;
        if (!TextUtils.isEmpty(intentDescription)) {
            try {
                intent = Intent.parseUri(intentDescription, 0);
                intent.removeExtra(ItemInfo.EXTRA_PROFILE);
                favorite.intent = intent.toUri(0);
            } catch (URISyntaxException e) {
                Log.e(TAG, "Invalid intent", e);
            }
        }
        favorite.itemType = c.getInt(12);
        if (favorite.itemType == 4) {
            favorite.appWidgetId = c.getInt(4);
            String appWidgetProvider = c.getString(3);
            if (!TextUtils.isEmpty(appWidgetProvider)) {
                favorite.appWidgetProvider = appWidgetProvider;
            }
        } else if (favorite.itemType == 1) {
            boolean isAppsButton = false;
            if (!(intent == null || intent.getAction() == null || !intent.getAction().equals(Utilities.ACTION_SHOW_APPS_VIEW))) {
                isAppsButton = true;
            }
            if (!isAppsButton) {
                if (favorite.iconType == 0) {
                    String iconPackage = c.getString(9);
                    if (!TextUtils.isEmpty(iconPackage)) {
                        favorite.iconPackage = iconPackage;
                    }
                    String iconResource = c.getString(10);
                    if (!TextUtils.isEmpty(iconResource)) {
                        favorite.iconResource = iconResource;
                    }
                }
                byte[] blob = c.getBlob(8);
                if (blob != null && blob.length > 0) {
                    favorite.icon = blob;
                }
            }
        }
        return favorite;
    }

    private ContentValues unpackFavorite(byte[] buffer, int dataSize) throws IOException {
        Favorite favorite = (Favorite) unpackProto(new Favorite(), buffer, dataSize);
        ContentValues values = new ContentValues();
        values.put("_id", Long.valueOf(favorite.id));
        values.put("screen", Integer.valueOf(favorite.screen));
        values.put("container", Integer.valueOf(favorite.container));
        values.put("cellX", Integer.valueOf(favorite.cellX));
        values.put("cellY", Integer.valueOf(favorite.cellY));
        values.put("spanX", Integer.valueOf(favorite.spanX));
        values.put("spanY", Integer.valueOf(favorite.spanY));
        values.put(BaseLauncherColumns.RANK, Integer.valueOf(favorite.rank));
        if (favorite.itemType == 1) {
            boolean isAppsButton = false;
            Intent intent = null;
            if (favorite.intent != null) {
                try {
                    intent = Intent.parseUri(favorite.intent, 0);
                } catch (URISyntaxException e) {
                    Log.e(TAG, "Invalid intent", e);
                }
            }
            if (!(intent == null || intent.getAction() == null || !intent.getAction().equals(Utilities.ACTION_SHOW_APPS_VIEW))) {
                isAppsButton = true;
            }
            values.put(BaseLauncherColumns.ICON_TYPE, Integer.valueOf(favorite.iconType));
            if (isAppsButton) {
                LauncherAppState.getInstance().setAppsButtonEnabled(true);
            } else {
                if (favorite.iconType == 0) {
                    values.put("iconPackage", favorite.iconPackage);
                    values.put("iconResource", favorite.iconResource);
                }
                values.put("icon", favorite.icon);
            }
        }
        if (TextUtils.isEmpty(favorite.title)) {
            values.put("title", "");
        } else {
            values.put("title", favorite.title);
        }
        if (!TextUtils.isEmpty(favorite.intent)) {
            values.put("intent", favorite.intent);
        }
        values.put("itemType", Integer.valueOf(favorite.itemType));
        values.put(BaseLauncherColumns.PROFILE_ID, Long.valueOf(UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(UserHandleCompat.myUserHandle())));
        if (favorite.itemType == 4) {
            if (!TextUtils.isEmpty(favorite.appWidgetProvider)) {
                values.put(Favorites.APPWIDGET_PROVIDER, favorite.appWidgetProvider);
            }
            values.put(Favorites.APPWIDGET_ID, Integer.valueOf(favorite.appWidgetId));
            values.put("restored", Integer.valueOf(7));
            if (!LauncherFeature.supportFlexibleGrid() && (favorite.cellX + favorite.spanX > this.mDeviceProfileData.cols || favorite.cellY + favorite.spanY > this.mDeviceProfileData.rows)) {
                this.restoreSuccessful = false;
                throw new InvalidBackupException("Widget not in screen bounds, aborting restore");
            }
        }
        values.put("restored", Integer.valueOf(1));
        if (!LauncherFeature.supportFlexibleGrid() && favorite.container == -100 && (favorite.cellX >= this.mDeviceProfileData.cols || favorite.cellY >= this.mDeviceProfileData.rows)) {
            this.restoreSuccessful = false;
            throw new InvalidBackupException("Item not in desktop bounds, aborting restore");
        }
        return values;
    }

    private Screen packScreen(Cursor c) {
        Screen screen = new Screen();
        screen.id = c.getLong(0);
        screen.rank = c.getInt(2);
        return screen;
    }

    private ContentValues unpackScreen(byte[] buffer, int dataSize) throws InvalidProtocolBufferNanoException {
        Screen screen = (Screen) unpackProto(new Screen(), buffer, dataSize);
        ContentValues values = new ContentValues();
        values.put("_id", Long.valueOf(screen.id));
        values.put(WorkspaceScreens.SCREEN_RANK, Integer.valueOf(screen.rank));
        return values;
    }

    private Resource packIcon(int dpi, Bitmap icon) {
        Resource res = new Resource();
        res.dpi = dpi;
        res.data = Utilities.flattenBitmap(icon);
        return res;
    }

    private Widget packWidget(int dpi, ComponentName provider, UserHandleCompat user) {
        LauncherAppWidgetProviderInfo info = HomeLoader.getProviderInfo(this.mContext, provider, user);
        Widget widget = new Widget();
        widget.provider = provider.flattenToShortString();
        widget.label = info.label;
        widget.configure = info.configure != null;
        if (info.icon != 0) {
            widget.icon = new Resource();
            widget.icon.data = Utilities.flattenBitmap(BitmapUtils.createIconBitmap(this.mIconCache.getFullResIcon(provider.getPackageName(), info.icon), this.mContext));
            widget.icon.dpi = dpi;
        }
        Point spans = info.getMinSpans();
        widget.minSpanX = spans.x;
        widget.minSpanY = spans.y;
        return widget;
    }

    private <T extends MessageNano> T unpackProto(T proto, byte[] buffer, int dataSize) throws InvalidProtocolBufferNanoException {
        MessageNano.mergeFrom(proto, readCheckedBytes(buffer, dataSize));
        return proto;
    }

    private Journal readJournal(ParcelFileDescriptor oldState) {
        Journal journal = new Journal();
        if (oldState != null) {
            FileInputStream inStream = new FileInputStream(oldState.getFileDescriptor());
            try {
                int availableBytes = inStream.available();
                if (availableBytes < MAX_JOURNAL_SIZE) {
                    byte[] buffer = new byte[availableBytes];
                    int bytesRead = 0;
                    boolean valid = false;
                    InvalidProtocolBufferNanoException lastProtoException = null;
                    while (availableBytes > 0) {
                        try {
                            int result = inStream.read(buffer, bytesRead, 1);
                            if (result > 0) {
                                availableBytes -= result;
                                bytesRead += result;
                            } else {
                                Log.w(TAG, "unexpected end of file while reading journal.");
                                availableBytes = 0;
                            }
                        } catch (IOException e) {
                            buffer = null;
                            availableBytes = 0;
                        }
                        try {
                            MessageNano.mergeFrom(journal, readCheckedBytes(buffer, bytesRead));
                            valid = true;
                            availableBytes = 0;
                        } catch (InvalidProtocolBufferNanoException e2) {
                            lastProtoException = e2;
                            journal.clear();
                        }
                    }
                    if (!valid) {
                        Log.w(TAG, "could not find a valid journal", lastProtoException);
                    }
                }
                try {
                    inStream.close();
                } catch (IOException e3) {
                    Log.w(TAG, "failed to close the journal", e3);
                }
            } catch (IOException e32) {
                Log.w(TAG, "failed to close the journal", e32);
                try {
                    inStream.close();
                } catch (IOException e322) {
                    Log.w(TAG, "failed to close the journal", e322);
                }
            } catch (Throwable th) {
                try {
                    inStream.close();
                } catch (IOException e3222) {
                    Log.w(TAG, "failed to close the journal", e3222);
                }
                throw th;
            }
        }
        return journal;
    }

    private void writeRowToBackup(Key key, MessageNano proto, BackupDataOutput data) throws IOException {
        writeRowToBackup(keyToBackupKey(key), proto, data);
    }

    private void writeRowToBackup(String backupKey, MessageNano proto, BackupDataOutput data) throws IOException {
        byte[] blob = writeCheckedBytes(proto);
        data.writeEntityHeader(backupKey, blob.length);
        data.writeEntityData(blob, blob.length);
        this.mBackupDataWasUpdated = true;
    }

    private void writeJournal(ParcelFileDescriptor newState, Journal journal) {
        try {
            FileOutputStream outStream = new FileOutputStream(newState.getFileDescriptor());
            outStream.write(writeCheckedBytes(journal));
            outStream.close();
        } catch (IOException e) {
            Log.w(TAG, "failed to write backup journal", e);
        }
    }

    private byte[] writeCheckedBytes(MessageNano proto) {
        CheckedMessage wrapper = new CheckedMessage();
        wrapper.payload = MessageNano.toByteArray(proto);
        CRC32 checksum = new CRC32();
        checksum.update(wrapper.payload);
        wrapper.checksum = checksum.getValue();
        return MessageNano.toByteArray(wrapper);
    }

    private static byte[] readCheckedBytes(byte[] buffer, int dataSize) throws InvalidProtocolBufferNanoException {
        CheckedMessage wrapper = new CheckedMessage();
        MessageNano.mergeFrom(wrapper, buffer, 0, dataSize);
        CRC32 checksum = new CRC32();
        checksum.update(wrapper.payload);
        if (wrapper.checksum == checksum.getValue()) {
            return wrapper.payload;
        }
        throw new InvalidProtocolBufferNanoException("checksum does not match");
    }

    private boolean launcherIsReady() {
        Cursor cursor = this.mContext.getContentResolver().query(Favorites.CONTENT_URI, FAVORITE_PROJECTION, null, null, null);
        if (cursor == null) {
            return false;
        }
        cursor.close();
        return LauncherAppState.getInstanceNoCreate() != null;
    }
}

package com.android.launcher3.home;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.RawContactsEntity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.bnr.LauncherBnrCallBack;
import com.android.launcher3.common.bnr.LauncherBnrHelper;
import com.android.launcher3.common.bnr.LauncherBnrListener.Result;
import com.android.launcher3.common.bnr.LauncherBnrTag;
import com.android.launcher3.common.bnr.extractor.LCExtractor;
import com.android.launcher3.common.bnr.scloud.SCloudBnr;
import com.android.launcher3.common.model.CursorInfo;
import com.android.launcher3.common.model.DefaultLayoutParser;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.common.model.LauncherSettings.WorkspaceScreens;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.util.ScreenGridUtilities;
import com.android.vcard.VCardComposer;
import com.android.vcard.VCardConfig;
import com.sec.android.app.launcher.R;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public class HomeBnrHelper implements LauncherBnrCallBack {
    private static final String TAG = "Launcher.HomeBnr";
    private boolean isPossibleHomeBackup;
    private boolean isPossibleHomeOnlyBackup;
    private Context mContext;
    private boolean mLauncherPrefix = false;

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void backupHotseat(java.lang.String r16, java.lang.String r17, org.xmlpull.v1.XmlSerializer r18, com.android.launcher3.common.bnr.LauncherBnrListener.Result r19) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x004e in list [B:9:0x0069]
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
        /*
        r15 = this;
        r3 = r15.mContext;
        r1 = r3.getContentResolver();
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r5 = "container=-101 and ";
        r3 = r3.append(r5);
        r5 = r15.mContext;
        r5 = com.android.launcher3.common.bnr.LauncherBnrHelper.getUserSelectionArg(r5);
        r3 = r3.append(r5);
        r4 = r3.toString();
        r14 = "container desc, screen";
        r2 = com.android.launcher3.common.bnr.LauncherBnrHelper.getFavoritesUri(r16);
        r3 = 0;
        r5 = 0;
        r6 = "container desc, screen";
        r8 = r1.query(r2, r3, r4, r5, r6);
        if (r8 != 0) goto L_0x004f;
    L_0x002f:
        r3 = 1;
        r0 = r19;
        r0.result = r3;
        r3 = "Launcher.HomeBnr";
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r6 = "backupHotseat, fail to open cursor, ";
        r5 = r5.append(r6);
        r0 = r16;
        r5 = r5.append(r0);
        r5 = r5.toString();
        android.util.Log.e(r3, r5);
    L_0x004e:
        return;
    L_0x004f:
        r3 = r8.moveToFirst();	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        if (r3 == 0) goto L_0x0063;	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
    L_0x0055:
        r5 = r15;	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        r6 = r16;	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        r7 = r17;	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        r9 = r1;	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        r10 = r18;	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        r11 = r2;	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        r12 = r19;	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        r5.backupItem(r6, r7, r8, r9, r10, r11, r12);	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
    L_0x0063:
        r3 = r8.isClosed();
        if (r3 != 0) goto L_0x004e;
    L_0x0069:
        r8.close();
        goto L_0x004e;
    L_0x006d:
        r13 = move-exception;
        r3 = 1;
        r0 = r19;	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        r0.result = r3;	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        r3 = "Launcher.HomeBnr";	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        r5 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        r5.<init>();	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        r6 = "backupHotseat Exception : ";	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        r5 = r5.append(r6);	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        r6 = r13.toString();	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        r5 = r5.append(r6);	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        r5 = r5.toString();	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        android.util.Log.e(r3, r5);	 Catch:{ Exception -> 0x006d, all -> 0x0099 }
        r3 = r8.isClosed();
        if (r3 != 0) goto L_0x004e;
    L_0x0095:
        r8.close();
        goto L_0x004e;
    L_0x0099:
        r3 = move-exception;
        r5 = r8.isClosed();
        if (r5 != 0) goto L_0x00a3;
    L_0x00a0:
        r8.close();
    L_0x00a3:
        throw r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.HomeBnrHelper.backupHotseat(java.lang.String, java.lang.String, org.xmlpull.v1.XmlSerializer, com.android.launcher3.common.bnr.LauncherBnrListener$Result):void");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void backupWorkspace(java.lang.String r12, java.lang.String r13, org.xmlpull.v1.XmlSerializer r14, com.android.launcher3.common.bnr.LauncherBnrListener.Result r15) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x0037 in list [B:9:0x0051]
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
        /*
        r11 = this;
        r1 = 1;
        r0 = r11.mContext;
        r4 = r0.getContentResolver();
        r6 = com.android.launcher3.common.bnr.LauncherBnrHelper.getFavoritesUri(r12);
        r9 = com.android.launcher3.common.bnr.LauncherBnrHelper.getFavoritesTable(r12);
        r10 = com.android.launcher3.common.bnr.LauncherBnrHelper.getWorkspaceScreenTable(r12);
        r0 = com.android.launcher3.common.model.FavoritesProvider.getInstance();
        r3 = r0.loadWorkspaceWithScreenRank(r9, r10);
        if (r3 != 0) goto L_0x0038;
    L_0x001d:
        r15.result = r1;
        r0 = "Launcher.HomeBnr";
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "backupWorkspace, fail to open cursor, ";
        r1 = r1.append(r2);
        r1 = r1.append(r12);
        r1 = r1.toString();
        android.util.Log.e(r0, r1);
    L_0x0037:
        return;
    L_0x0038:
        r0 = r3.moveToFirst();	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        if (r0 == 0) goto L_0x004b;	 Catch:{ Exception -> 0x0055, all -> 0x007f }
    L_0x003e:
        r0 = "\n";	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        r14.text(r0);	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        r0 = r11;	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        r1 = r12;	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        r2 = r13;	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        r5 = r14;	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        r7 = r15;	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        r0.backupItem(r1, r2, r3, r4, r5, r6, r7);	 Catch:{ Exception -> 0x0055, all -> 0x007f }
    L_0x004b:
        r0 = r3.isClosed();
        if (r0 != 0) goto L_0x0037;
    L_0x0051:
        r3.close();
        goto L_0x0037;
    L_0x0055:
        r8 = move-exception;
        r0 = 1;
        r15.result = r0;	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        r0 = "Launcher.HomeBnr";	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        r1 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        r1.<init>();	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        r2 = "backupWorkspace Exception : ";	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        r1 = r1.append(r2);	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        r2 = r8.toString();	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        r1 = r1.append(r2);	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        r1 = r1.toString();	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        android.util.Log.e(r0, r1);	 Catch:{ Exception -> 0x0055, all -> 0x007f }
        r0 = r3.isClosed();
        if (r0 != 0) goto L_0x0037;
    L_0x007b:
        r3.close();
        goto L_0x0037;
    L_0x007f:
        r0 = move-exception;
        r1 = r3.isClosed();
        if (r1 != 0) goto L_0x0089;
    L_0x0086:
        r3.close();
    L_0x0089:
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.home.HomeBnrHelper.backupWorkspace(java.lang.String, java.lang.String, org.xmlpull.v1.XmlSerializer, com.android.launcher3.common.bnr.LauncherBnrListener$Result):void");
    }

    public String backupCategory() {
        this.isPossibleHomeBackup = false;
        this.isPossibleHomeOnlyBackup = false;
        StringBuffer category = new StringBuffer();
        if (FavoritesProvider.getInstance().getItemCount(LauncherBnrHelper.getFavoritesTable("home")) > 0) {
            this.isPossibleHomeBackup = true;
            category.append("home");
            category.append(',');
            category.append("hotseat");
        }
        if (LauncherFeature.supportHomeModeChange()) {
            if (FavoritesProvider.getInstance().getItemCount(LauncherBnrHelper.getFavoritesTable(LauncherBnrTag.TAG_HOMEONLY)) > 0) {
                this.isPossibleHomeOnlyBackup = true;
                if (this.isPossibleHomeBackup) {
                    category.append(',');
                }
                category.append(LauncherBnrTag.TAG_HOMEONLY);
                category.append(',');
                category.append(LauncherBnrTag.TAG_HOTSEAT_HOMEONLY);
            }
        }
        if (this.isPossibleHomeBackup || this.isPossibleHomeOnlyBackup) {
            category.append(',');
            category.append(LauncherBnrTag.TAG_ZEROPAGE);
        }
        return category.toString();
    }

    public void backupLayout(Context context, XmlSerializer serializer, String source, Result result) {
        Log.i(TAG, "backupLayout");
        this.mContext = context;
        if (LCExtractor.LCEXTRACTOR_HOME_SOURCE.equals(source)) {
            backupWorkspace("home", source, serializer, result);
            backupHotseat("hotseat", source, serializer, result);
        } else if (!LCExtractor.LCEXTRACTOR_APPS_SOURCE.equals(source)) {
            backupPageSetting("home", serializer, result);
            backupZeroPage(serializer, result);
            backupSettings(serializer, result);
            backupWorkspace("home", source, serializer, result);
            backupHotseat("hotseat", source, serializer, result);
            if (LauncherFeature.supportHomeModeChange() && result.result == 0) {
                backupPageSetting(LauncherBnrTag.TAG_HOMEONLY, serializer, result);
                backupHomeScreenContent(serializer, result);
                backupWorkspace(LauncherBnrTag.TAG_HOMEONLY, source, serializer, result);
                backupHotseat(LauncherBnrTag.TAG_HOTSEAT_HOMEONLY, source, serializer, result);
            }
            if (LauncherFeature.supportEasyModeChange() && result.result == 0) {
                if (FavoritesProvider.getInstance().getItemCount(LauncherBnrHelper.getFavoritesTable(LauncherBnrTag.TAG_EASY)) > 0) {
                    backupPageSetting(LauncherBnrTag.TAG_EASY, serializer, result);
                    backupWorkspace(LauncherBnrTag.TAG_HOME_EASY, source, serializer, result);
                    backupHotseat(LauncherBnrTag.TAG_HOTSEAT_EASY, source, serializer, result);
                }
            }
        }
    }

    public void restoreLayout(Context context, XmlPullParser parser, ArrayList<String> restoredTable, Result result) {
        FavoritesProvider favoritesProvider = FavoritesProvider.getInstance();
        if (favoritesProvider == null) {
            Log.i(TAG, "FavoritesProvider.getInstance() is null");
            result.result = 1;
            return;
        }
        Log.i(TAG, "restoreLayout");
        if (favoritesProvider.restoreFavorites(new HomeRestoreLayoutParser(context, favoritesProvider, parser, restoredTable)) < 0) {
            result.result = 1;
        }
    }

    private void backupValue(XmlSerializer serializer, String tag, String value) throws IOException {
        serializer.text("\n");
        serializer.startTag(null, tag);
        serializer.text(value);
        serializer.endTag(null, tag);
    }

    private void backupPageSetting(String container, XmlSerializer serializer, Result result) {
        if (LauncherBnrTag.TAG_HOMEONLY.equals(container) && !this.isPossibleHomeOnlyBackup) {
            return;
        }
        if (!"home".equals(container) || this.isPossibleHomeBackup) {
            String suffix;
            String tableName;
            int screenIndex;
            int[] cellXY = new int[]{-1, -1};
            if (LauncherBnrTag.TAG_EASY.equals(container)) {
                suffix = '_' + container;
                tableName = LauncherBnrHelper.getWorkspaceScreenTable(LauncherBnrTag.TAG_HOME_EASY);
                screenIndex = Utilities.getHomeDefaultPageKey(this.mContext, LauncherFiles.HOMEEASY_DEFAULT_PAGE_KEY);
            } else if (LauncherBnrTag.TAG_HOMEONLY.equals(container)) {
                suffix = '_' + container;
                tableName = LauncherBnrHelper.getWorkspaceScreenTable(LauncherBnrTag.TAG_HOMEONLY);
                ScreenGridUtilities.loadCurrentGridSize(this.mContext, cellXY, true);
                screenIndex = Utilities.getHomeDefaultPageKey(this.mContext, LauncherFiles.HOMEONLY_DEFAULT_PAGE_KEY);
            } else {
                suffix = "";
                tableName = LauncherBnrHelper.getWorkspaceScreenTable("home");
                ScreenGridUtilities.loadCurrentGridSize(this.mContext, cellXY, false);
                screenIndex = Utilities.getHomeDefaultPageKey(this.mContext, LauncherFiles.HOME_DEFAULT_PAGE_KEY);
            }
            int pageCount = FavoritesProvider.getInstance().getItemCount(tableName);
            Log.d(TAG, "backupPageSetting container : " + container + ", pageCount : " + pageCount);
            try {
                serializer.text("\n");
                if (!LauncherBnrTag.TAG_EASY.equals(container)) {
                    int countX = cellXY[0];
                    int countY = cellXY[1];
                    if (countX == -1 || countY == -1) {
                        countX = this.mContext.getResources().getInteger(R.integer.home_cellCountX);
                        countY = this.mContext.getResources().getInteger(R.integer.home_cellCountY);
                    }
                    Log.d(TAG, "backupPageSetting home grid x = " + countX + ", y = " + countY);
                    backupValue(serializer, LauncherBnrTag.TAG_ROWS + suffix, Integer.toString(countY));
                    backupValue(serializer, LauncherBnrTag.TAG_COLUMNS + suffix, Integer.toString(countX));
                }
                backupValue(serializer, LauncherBnrTag.TAG_PAGECOUNT + suffix, Integer.toString(pageCount));
                backupValue(serializer, LauncherBnrTag.TAG_SCREEN_INDEX + suffix, Integer.toString(screenIndex));
            } catch (Exception e) {
                result.result = 1;
                Log.e(TAG, "backupPageSetting Exception : " + e.toString());
            }
        }
    }

    private void backupSettings(XmlSerializer serializer, Result result) {
        SharedPreferences prefs = this.mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        if (LauncherFeature.supportNotificationPanelExpansion() && prefs.contains(Utilities.NOTIFICATION_PANEL_SETTING_PREFERENCE_KEY)) {
            boolean notificationPanelExpansionEnabled = prefs.getBoolean(Utilities.NOTIFICATION_PANEL_SETTING_PREFERENCE_KEY, false);
            Log.d(TAG, "backupSettings - NotificationPanelExpansionEnabled : " + notificationPanelExpansionEnabled);
            try {
                serializer.text("\n");
                backupValue(serializer, LauncherBnrTag.TAG_NOTIFICATION_PANEL_SETTING, Boolean.toString(notificationPanelExpansionEnabled));
            } catch (Exception e) {
                result.result = 1;
                Log.e(TAG, "backupSettings NotificationPanelExpansion : " + e.toString());
            }
        }
        if (LauncherFeature.supportRotationSetting() && prefs.contains(Utilities.ONLY_PORTRAIT_MODE_SETTING_PREFERENCE_KEY)) {
            boolean rotationEnabled = prefs.getBoolean(Utilities.ONLY_PORTRAIT_MODE_SETTING_PREFERENCE_KEY, true);
            Log.d(TAG, "backupSettings - RotationEnabled : " + rotationEnabled);
            try {
                serializer.text("\n");
                backupValue(serializer, LauncherBnrTag.TAG_ONLY_PORTRAIT_MODE_SETTING, Boolean.toString(rotationEnabled));
            } catch (Exception e2) {
                result.result = 1;
                Log.e(TAG, "backupSettings RotationEnabled : " + e2.toString());
            }
        }
        if (Utilities.ATLEAST_O && prefs.contains(Utilities.ADD_ICON_PREFERENCE_KEY)) {
            boolean addIconToHomeEnabled = prefs.getBoolean(Utilities.ADD_ICON_PREFERENCE_KEY, false);
            Log.d(TAG, "backupSettings - AddIconToHomeEnabled : " + addIconToHomeEnabled);
            try {
                serializer.text("\n");
                backupValue(serializer, LauncherBnrTag.TAG_ADD_ICON_TO_HOME_SETTING, Boolean.toString(addIconToHomeEnabled));
            } catch (Exception e22) {
                result.result = 1;
                Log.e(TAG, "backupSettings AddIconToHomeEnabled : " + e22.toString());
            }
        }
    }

    private void backupZeroPage(XmlSerializer serializer, Result result) {
        boolean zeroPageEnable = ZeroPageController.isActiveZeroPage(this.mContext, true);
        ComponentName zeroPageContents = ZeroPageController.getZeroPageContents(this.mContext);
        Log.d(TAG, "zeroPageEnable : " + zeroPageEnable);
        try {
            serializer.text("\n");
            if (zeroPageContents != null) {
                backupValue(serializer, LauncherBnrTag.TAG_ZEROPAGE_CONTENTS, zeroPageContents.flattenToString());
            }
            backupValue(serializer, LauncherBnrTag.TAG_ZEROPAGE, Boolean.toString(zeroPageEnable));
        } catch (Exception e) {
            result.result = 1;
            Log.e(TAG, "backupZeroPage Exception : " + e.toString());
        }
    }

    private void backupHomeScreenContent(XmlSerializer serializer, Result result) {
        if (this.isPossibleHomeOnlyBackup) {
            Log.d(TAG, "sIsHomeOnly : " + LauncherBnrHelper.sIsHomeOnly);
            try {
                serializer.text("\n");
                backupValue(serializer, LauncherBnrTag.TAG_SCREEN_CONTENT, Boolean.toString(LauncherBnrHelper.sIsHomeOnly));
            } catch (Exception e) {
                result.result = 1;
                Log.e(TAG, "backupHomeScreenContent Exception : " + e.toString());
            }
        }
    }

    private void backupItem(String container, String source, Cursor cursor, ContentResolver cr, XmlSerializer serializer, Uri uri, Result result) throws IllegalArgumentException, IllegalStateException, IOException {
        boolean isLCExtractor = LCExtractor.LCEXTRACTOR_HOME_SOURCE.equals(source);
        serializer.text("\n");
        if (isLCExtractor) {
            serializer.text(LCExtractor.getStrTab(1, false));
        }
        serializer.startTag(null, container);
        CursorInfo cursorInfo = new CursorInfo(cursor);
        do {
            int itemType = cursor.getInt(cursorInfo.itemTypeIndex);
            String intent = cursor.getString(cursorInfo.intentIndex);
            int hidden = cursor.getInt(cursorInfo.hiddenIndex);
            int restore = cursor.getInt(cursorInfo.restoredIndex);
            if (hidden == 0 && restore == 0) {
                ComponentName componentName = null;
                Intent intentInfo = null;
                if (!TextUtils.isEmpty(intent)) {
                    try {
                        intentInfo = Intent.parseUri(intent, 0);
                        componentName = intentInfo.getComponent();
                    } catch (URISyntaxException e) {
                    }
                }
                switch (itemType) {
                    case 0:
                        if (componentName != null) {
                            backupApplicationItem(componentName, cursor, cursorInfo, source, serializer);
                            break;
                        }
                        break;
                    case 1:
                    case 6:
                    case 7:
                        String tag = DefaultLayoutParser.TAG_SHORTCUT;
                        if (itemType == 6) {
                            tag = DefaultLayoutParser.TAG_DEEP_SHORTCUT;
                        } else if (itemType == 7) {
                            tag = DefaultLayoutParser.TAG_PAIRAPPS_SHORTCUT;
                        }
                        if (!IconView.KNOX_SHORTCUT_PACKAGE.equals(componentName != null ? componentName.getPackageName() : null)) {
                            backupShortcutItem(intent, intentInfo, cursor, cursorInfo, tag, source, serializer, result);
                            break;
                        }
                        break;
                    case 2:
                        backupFolderItem(cursor, cursorInfo, cr, source, serializer, uri, result);
                        break;
                    case 4:
                        backupWidgetItem(cursor, cursorInfo, source, serializer);
                        break;
                }
                if (isLCExtractor) {
                    serializer.text("\n");
                }
            }
        } while (cursor.moveToNext());
        serializer.text("\n");
        if (isLCExtractor) {
            serializer.text(LCExtractor.getStrTab(1, false));
        }
        serializer.endTag(null, container);
        if ("home".equals(container) && isLCExtractor) {
            serializer.text("\n\n");
        }
    }

    private void backupApplicationItem(ComponentName componentName, Cursor cursor, CursorInfo cursorInfo, String source, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
        boolean isLCExtractor = LCExtractor.LCEXTRACTOR_HOME_SOURCE.equals(source);
        String attrPrefix = isLCExtractor ? "\n" + LCExtractor.getStrTab(3, this.mLauncherPrefix) : "";
        long containerType = cursor.getLong(cursorInfo.containerIndex);
        int screen = cursor.getInt(cursorInfo.screenIndex);
        if (containerType == -100) {
            screen = cursor.getInt(cursor.getColumnIndexOrThrow(WorkspaceScreens.SCREEN_RANK));
        }
        String cellX = String.valueOf(cursor.getInt(cursorInfo.cellXIndex));
        String cellY = String.valueOf(cursor.getInt(cursorInfo.cellYIndex));
        String packageName = componentName.getPackageName();
        String className = componentName.getClassName();
        serializer.text("\n");
        if (isLCExtractor) {
            serializer.text(LCExtractor.getStrTab(2, false));
            serializer.comment(' ' + Utilities.getAppLabel(this.mContext, packageName) + ' ');
            serializer.text("\n" + LCExtractor.getStrTab(2, false));
        }
        serializer.startTag(null, DefaultLayoutParser.TAG_FAVORITE);
        serializer.attribute(null, attrPrefix + "screen", String.valueOf(screen));
        if (!TextUtils.isEmpty(packageName)) {
            serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_PACKAGE_NAME, packageName);
        }
        if (!TextUtils.isEmpty(className)) {
            serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_CLASS_NAME, className);
        }
        if (containerType == -100) {
            if (!TextUtils.isEmpty(cellX)) {
                serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_X, cellX);
            }
            if (!TextUtils.isEmpty(cellY)) {
                serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_Y, cellY);
            }
        }
        if (SCloudBnr.SCLOUD_SOURCE.equals(source)) {
            serializer.attribute(null, "restored", String.valueOf(4));
        }
        serializer.endTag(null, DefaultLayoutParser.TAG_FAVORITE);
    }

    private void backupShortcutItem(String intent, Intent intentInfo, Cursor cursor, CursorInfo cursorInfo, String tag, String source, XmlSerializer serializer, Result result) throws IllegalArgumentException, IllegalStateException, IOException {
        boolean isLCExtractor = LCExtractor.LCEXTRACTOR_HOME_SOURCE.equals(source);
        String attrPrefix = isLCExtractor ? "\n" + LCExtractor.getStrTab(3, this.mLauncherPrefix) : "";
        long containerType = cursor.getLong(cursorInfo.containerIndex);
        int screen = cursor.getInt(cursorInfo.screenIndex);
        if (containerType == -100) {
            screen = cursor.getInt(cursor.getColumnIndexOrThrow(WorkspaceScreens.SCREEN_RANK));
        }
        String cellX = String.valueOf(cursor.getInt(cursorInfo.cellXIndex));
        String cellY = String.valueOf(cursor.getInt(cursorInfo.cellYIndex));
        String title = cursor.getString(cursorInfo.titleIndex);
        if (intentInfo == null || intentInfo.getAction() == null || !intentInfo.getAction().equals(Utilities.ACTION_SHOW_APPS_VIEW)) {
            serializer.text("\n");
            if (isLCExtractor) {
                serializer.text(LCExtractor.getStrTab(2, false));
                serializer.comment(' ' + tag + " : " + title + ' ');
                serializer.text("\n" + LCExtractor.getStrTab(2, false));
            }
            serializer.startTag(null, tag);
            serializer.attribute(null, attrPrefix + "screen", String.valueOf(screen));
            if (cursor.getInt(cursorInfo.iconTypeIndex) == 0) {
                String iconPackage = cursor.getString(cursorInfo.iconPackageIndex);
                String iconResource = cursor.getString(cursorInfo.iconResourceIndex);
                if (!(TextUtils.isEmpty(iconPackage) || isLCExtractor)) {
                    serializer.attribute(null, "iconPackage", iconPackage);
                }
                if (!(TextUtils.isEmpty(iconResource) || isLCExtractor)) {
                    serializer.attribute(null, "iconResource", iconResource);
                }
            }
            if (!TextUtils.isEmpty(title)) {
                serializer.attribute(null, attrPrefix + "title", title);
            }
            if (containerType == -100) {
                if (!TextUtils.isEmpty(cellX)) {
                    serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_X, cellX);
                }
                if (!TextUtils.isEmpty(cellY)) {
                    serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_Y, cellY);
                }
            }
            if (SCloudBnr.SCLOUD_SOURCE.equals(source)) {
                serializer.attribute(null, "restored", String.valueOf(4));
            }
            if (!TextUtils.isEmpty(intent)) {
                serializer.attribute(null, attrPrefix + "uri", intent);
            }
            if (!isLCExtractor) {
                byte[] data = cursor.getBlob(cursorInfo.iconIndex);
                if (data != null && data.length > 0) {
                    serializer.attribute(null, "icon", Base64.encodeToString(data, 2));
                }
            }
            if (!SCloudBnr.SCLOUD_SOURCE.equals(source)) {
                backupContactShortcut(intent, serializer, result);
            }
            serializer.endTag(null, tag);
        } else if (!isLCExtractor) {
            backupAppsButton(screen, serializer);
        }
    }

    private void backupWidgetItem(Cursor cursor, CursorInfo cursorInfo, String source, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
        boolean isLCExtractor = LCExtractor.LCEXTRACTOR_HOME_SOURCE.equals(source);
        String attrPrefix = isLCExtractor ? "\n" + LCExtractor.getStrTab(3, this.mLauncherPrefix) : "";
        AppWidgetManager widgets = AppWidgetManager.getInstance(this.mContext);
        int appWidgetId = cursor.getInt(cursorInfo.appWidgetIdIndex);
        AppWidgetProviderInfo widgetInfo = widgets.getAppWidgetInfo(appWidgetId);
        if (widgetInfo != null && widgetInfo.provider != null) {
            String packageName = widgetInfo.provider.getPackageName();
            String className = widgetInfo.provider.getClassName();
            long containerType = cursor.getLong(cursorInfo.containerIndex);
            int screen = cursor.getInt(cursorInfo.screenIndex);
            if (containerType == -100) {
                screen = cursor.getInt(cursor.getColumnIndexOrThrow(WorkspaceScreens.SCREEN_RANK));
            }
            String cellX = String.valueOf(cursor.getInt(cursorInfo.cellXIndex));
            String cellY = String.valueOf(cursor.getInt(cursorInfo.cellYIndex));
            String spanX = String.valueOf(cursor.getInt(cursorInfo.spanXIndex));
            String spanY = String.valueOf(cursor.getInt(cursorInfo.spanYIndex));
            serializer.text("\n");
            if (isLCExtractor) {
                serializer.text(LCExtractor.getStrTab(2, false));
                serializer.comment(" appwidget : " + Utilities.getAppLabel(this.mContext, packageName) + ' ');
                serializer.text("\n" + LCExtractor.getStrTab(2, false));
            }
            serializer.startTag(null, DefaultLayoutParser.TAG_APPWIDGET);
            serializer.attribute(null, attrPrefix + "screen", String.valueOf(screen));
            if (!TextUtils.isEmpty(packageName)) {
                serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_PACKAGE_NAME, packageName);
            }
            if (!TextUtils.isEmpty(className)) {
                serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_CLASS_NAME, className);
            }
            if (!TextUtils.isEmpty(cellX)) {
                serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_X, cellX);
            }
            if (!TextUtils.isEmpty(cellY)) {
                serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_Y, cellY);
            }
            if (!TextUtils.isEmpty(spanX)) {
                serializer.attribute(null, attrPrefix + "spanX", spanX);
            }
            if (!TextUtils.isEmpty(spanY)) {
                serializer.attribute(null, attrPrefix + "spanY", spanY);
            }
            if (!isLCExtractor) {
                serializer.attribute(null, "appWidgetID", String.valueOf(appWidgetId));
            }
            if (SCloudBnr.SCLOUD_SOURCE.equals(source)) {
                serializer.attribute(null, "restored", String.valueOf(4));
            }
            serializer.endTag(null, DefaultLayoutParser.TAG_APPWIDGET);
        }
    }

    private void backupFolderItem(Cursor cursor, CursorInfo cursorInfo, ContentResolver cr, String source, XmlSerializer serializer, Uri uri, Result result) throws IllegalArgumentException, IllegalStateException, IOException {
        boolean isLCExtractor = LCExtractor.LCEXTRACTOR_HOME_SOURCE.equals(source);
        String attrPrefix = isLCExtractor ? "\n" + LCExtractor.getStrTab(3, this.mLauncherPrefix) : "";
        long id = cursor.getLong(cursorInfo.idIndex);
        long containerType = cursor.getLong(cursorInfo.containerIndex);
        int screen = cursor.getInt(cursorInfo.screenIndex);
        if (containerType == -100) {
            screen = cursor.getInt(cursor.getColumnIndexOrThrow(WorkspaceScreens.SCREEN_RANK));
        }
        String cellX = String.valueOf(cursor.getInt(cursorInfo.cellXIndex));
        String cellY = String.valueOf(cursor.getInt(cursorInfo.cellYIndex));
        String title = cursor.getString(cursorInfo.titleIndex);
        serializer.text("\n");
        if (isLCExtractor) {
            serializer.text(LCExtractor.getStrTab(2, false));
            serializer.comment(" folder : " + title + ' ');
            serializer.text("\n" + LCExtractor.getStrTab(2, false));
        }
        serializer.startTag(null, "folder");
        serializer.attribute(null, attrPrefix + "screen", String.valueOf(screen));
        if (!TextUtils.isEmpty(title)) {
            serializer.attribute(null, attrPrefix + "title", title);
        }
        if (containerType == -100) {
            if (!TextUtils.isEmpty(cellX)) {
                serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_X, cellX);
            }
            if (!TextUtils.isEmpty(cellY)) {
                serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_Y, cellY);
            }
        }
        backupFolderItemById(id, source, cr, serializer, uri, result, isLCExtractor);
        if (isLCExtractor) {
            serializer.text(LCExtractor.getStrTab(2, false));
        }
        serializer.endTag(null, "folder");
    }

    private void backupFolderItemById(long folderId, String source, ContentResolver cr, XmlSerializer serializer, Uri uri, Result result, boolean isLCExtractor) throws IOException {
        Cursor cursor = cr.query(uri, null, "container=" + folderId + " and " + LauncherBnrHelper.getUserSelectionArg(this.mContext), null, BaseLauncherColumns.RANK);
        if (cursor == null) {
            result.result = 1;
            Log.e(TAG, "backupFolderItemById, fail to open cursor");
            return;
        }
        String attrPrefix = isLCExtractor ? "\n" + LCExtractor.getStrTab(4, this.mLauncherPrefix) : "";
        CursorInfo cursorInfo = new CursorInfo(cursor);
        while (cursor.moveToNext()) {
            int itemType = cursor.getInt(cursorInfo.itemTypeIndex);
            int rank = cursor.getInt(cursorInfo.rankIndex);
            String title = cursor.getString(cursorInfo.titleIndex);
            String intent = cursor.getString(cursorInfo.intentIndex);
            int hidden = cursor.getInt(cursorInfo.hiddenIndex);
            int restore = cursor.getInt(cursorInfo.restoredIndex);
            if (hidden == 0 && restore == 0) {
                ComponentName componentName = null;
                if (!TextUtils.isEmpty(intent)) {
                    try {
                    } catch (URISyntaxException e) {
                    }
                    try {
                        componentName = Intent.parseUri(intent, 0).getComponent();
                    } finally {
                        if (!cursor.isClosed()) {
                            cursor.close();
                        }
                    }
                }
                String packageName = null;
                String className = null;
                if (componentName != null) {
                    packageName = componentName.getPackageName();
                    className = componentName.getClassName();
                }
                switch (itemType) {
                    case 0:
                        if (componentName == null) {
                            break;
                        }
                        serializer.text("\n");
                        if (isLCExtractor) {
                            serializer.text(LCExtractor.getStrTab(3, false));
                            serializer.comment(' ' + Utilities.getAppLabel(this.mContext, packageName) + ' ');
                            serializer.text("\n" + LCExtractor.getStrTab(3, false));
                        }
                        serializer.startTag(null, DefaultLayoutParser.TAG_FAVORITE);
                        serializer.attribute(null, attrPrefix + "screen", String.valueOf(rank));
                        if (!TextUtils.isEmpty(packageName)) {
                            serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_PACKAGE_NAME, packageName);
                        }
                        if (!TextUtils.isEmpty(className)) {
                            serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_CLASS_NAME, className);
                        }
                        if (SCloudBnr.SCLOUD_SOURCE.equals(source)) {
                            serializer.attribute(null, "restored", String.valueOf(4));
                        }
                        serializer.endTag(null, DefaultLayoutParser.TAG_FAVORITE);
                        break;
                    case 1:
                    case 6:
                        if (!IconView.KNOX_SHORTCUT_PACKAGE.equals(packageName)) {
                            String tag = itemType == 1 ? DefaultLayoutParser.TAG_SHORTCUT : DefaultLayoutParser.TAG_DEEP_SHORTCUT;
                            if (isLCExtractor) {
                                serializer.text(LCExtractor.getStrTab(3, false));
                                serializer.comment(' ' + tag + " : " + title + ' ');
                                serializer.text("\n" + LCExtractor.getStrTab(3, false));
                            }
                            serializer.text("\n");
                            serializer.startTag(null, tag);
                            serializer.attribute(null, attrPrefix + "screen", String.valueOf(rank));
                            if (cursor.getInt(cursorInfo.iconTypeIndex) == 0) {
                                String iconPackage = cursor.getString(cursorInfo.iconPackageIndex);
                                String iconResource = cursor.getString(cursorInfo.iconResourceIndex);
                                if (!TextUtils.isEmpty(iconPackage)) {
                                    serializer.attribute(null, "iconPackage", iconPackage);
                                }
                                if (!TextUtils.isEmpty(iconResource)) {
                                    serializer.attribute(null, "iconResource", iconResource);
                                }
                            }
                            if (!TextUtils.isEmpty(title)) {
                                serializer.attribute(null, attrPrefix + "title", title);
                            }
                            if (SCloudBnr.SCLOUD_SOURCE.equals(source)) {
                                serializer.attribute(null, "restored", String.valueOf(4));
                            }
                            if (!TextUtils.isEmpty(intent)) {
                                serializer.attribute(null, attrPrefix + "uri", intent);
                            }
                            byte[] data = cursor.getBlob(cursorInfo.iconIndex);
                            if (!(data == null || data.length <= 0 || isLCExtractor)) {
                                serializer.attribute(null, "icon", Base64.encodeToString(data, 2));
                            }
                            if (!SCloudBnr.SCLOUD_SOURCE.equals(source)) {
                                backupContactShortcut(intent, serializer, result);
                            }
                            serializer.endTag(null, tag);
                            break;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        serializer.text("\n");
    }

    private void backupAppsButton(int screen, XmlSerializer serializer) throws IOException {
        serializer.text("\n");
        serializer.startTag(null, LauncherBnrTag.TAG_APPS_BUTTON);
        serializer.attribute(null, "screen", String.valueOf(screen));
        serializer.endTag(null, LauncherBnrTag.TAG_APPS_BUTTON);
    }

    private void backupContactShortcut(String intentDescription, XmlSerializer serialize, Result result) {
        Exception e;
        Throwable th;
        try {
            Intent intent = Intent.parseUri(intentDescription, 0);
            if (intent != null) {
                Uri lookupUri = intent.getData();
                if (lookupUri != null && "com.android.contacts".equals(lookupUri.getAuthority())) {
                    Log.d(TAG, "vcf file making... lookup Uri : " + lookupUri.toString());
                    VCardComposer composer;
                    try {
                        Uri contentUriForRawContactsEntity = RawContactsEntity.CONTENT_URI.buildUpon().appendQueryParameter("for_export_only", "1").build();
                        composer = new VCardComposer(this.mContext, VCardConfig.getVCardTypeFromString("default"), true);
                        try {
                            if (composer.init(lookupUri, null, null, null, contentUriForRawContactsEntity)) {
                                if (composer.getCount() != 0) {
                                    serialize.attribute(null, "vcf", composer.createOneEntry());
                                    Log.d(TAG, "vcf file make success");
                                } else {
                                    Log.e(TAG, "not have composer");
                                }
                                if (composer != null) {
                                    composer.terminate();
                                    return;
                                }
                                return;
                            }
                            Log.e(TAG, "initialization failed : " + composer.getErrorReason());
                            if (composer != null) {
                                composer.terminate();
                            }
                        } catch (Exception e2) {
                            e = e2;
                            try {
                                result.result = 1;
                                Log.e(TAG, "backupContactShortcut Exception : " + e.toString());
                                if (composer != null) {
                                    composer.terminate();
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                if (composer != null) {
                                    composer.terminate();
                                }
                                throw th;
                            }
                        }
                    } catch (Exception e3) {
                        e = e3;
                        composer = null;
                        result.result = 1;
                        Log.e(TAG, "backupContactShortcut Exception : " + e.toString());
                        if (composer != null) {
                            composer.terminate();
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        composer = null;
                        if (composer != null) {
                            composer.terminate();
                        }
                        throw th;
                    }
                }
            }
        } catch (URISyntaxException e4) {
            Log.e(TAG, "return Intent.parseUri, URISyntaxException");
        }
    }
}

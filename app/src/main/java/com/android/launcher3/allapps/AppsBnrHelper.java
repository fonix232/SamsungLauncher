package com.android.launcher3.allapps;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.controller.AppsController;
import com.android.launcher3.allapps.controller.AppsController.ViewType;
import com.android.launcher3.allapps.model.AppsDefaultLayoutParser;
import com.android.launcher3.common.bnr.LauncherBnrCallBack;
import com.android.launcher3.common.bnr.LauncherBnrHelper;
import com.android.launcher3.common.bnr.LauncherBnrListener.Result;
import com.android.launcher3.common.bnr.LauncherBnrTag;
import com.android.launcher3.common.bnr.extractor.LCExtractor;
//import com.android.launcher3.common.bnr.scloud.SCloudBnr;
import com.android.launcher3.common.model.CursorInfo;
import com.android.launcher3.common.model.DefaultLayoutParser;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.util.ScreenGridUtilities;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public class AppsBnrHelper implements LauncherBnrCallBack {
    static final String APPS_VIEW_TYPE_ALPHABETIC = "ALPHABETIC";
    static final String APPS_VIEW_TYPE_CUSTOM = "CUSTOM";
    private static final String GOOGLE_VOICE_SEARCH_CLASS_NAME = "com.google.android.googlequicksearchbox.VoiceSearchActivity";
    private static final String TAG = "Launcher.AppsBnr";
    private Context mContext;
    private boolean mLauncherPrefix = false;

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void backupApps(android.content.Context r16, java.lang.String r17, org.xmlpull.v1.XmlSerializer r18, java.lang.String r19, com.android.launcher3.common.bnr.LauncherBnrListener.Result r20) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x0039 in list [B:9:0x005b]
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
        r1 = r16.getContentResolver();
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r5 = "container=-102 and ";
        r3 = r3.append(r5);
        r5 = r15.mContext;
        r5 = com.android.launcher3.common.bnr.LauncherBnrHelper.getUserSelectionArg(r5);
        r3 = r3.append(r5);
        r4 = r3.toString();
        r14 = "screen, rank";
        r2 = com.android.launcher3.common.bnr.LauncherBnrHelper.getFavoritesUri(r17);
        r3 = 0;
        r5 = 0;
        r6 = "screen, rank";
        r8 = r1.query(r2, r3, r4, r5, r6);
        if (r8 != 0) goto L_0x003a;
    L_0x002d:
        r3 = "Launcher.AppsBnr";
        r5 = "backupApps, fail to open cursor";
        android.util.Log.e(r3, r5);
        r3 = 1;
        r0 = r20;
        r0.result = r3;
    L_0x0039:
        return;
    L_0x003a:
        r3 = r8.moveToFirst();	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        if (r3 == 0) goto L_0x0055;	 Catch:{ Exception -> 0x005f, all -> 0x008b }
    L_0x0040:
        r3 = "\n";	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r0 = r18;	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r0.text(r3);	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r5 = r15;	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r6 = r17;	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r7 = r19;	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r9 = r1;	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r10 = r18;	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r11 = r2;	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r12 = r20;	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r5.backupItem(r6, r7, r8, r9, r10, r11, r12);	 Catch:{ Exception -> 0x005f, all -> 0x008b }
    L_0x0055:
        r3 = r8.isClosed();
        if (r3 != 0) goto L_0x0039;
    L_0x005b:
        r8.close();
        goto L_0x0039;
    L_0x005f:
        r13 = move-exception;
        r3 = 1;
        r0 = r20;	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r0.result = r3;	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r3 = "Launcher.AppsBnr";	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r5 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r5.<init>();	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r6 = "backupApps Exception : ";	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r5 = r5.append(r6);	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r6 = r13.toString();	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r5 = r5.append(r6);	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r5 = r5.toString();	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        android.util.Log.e(r3, r5);	 Catch:{ Exception -> 0x005f, all -> 0x008b }
        r3 = r8.isClosed();
        if (r3 != 0) goto L_0x0039;
    L_0x0087:
        r8.close();
        goto L_0x0039;
    L_0x008b:
        r3 = move-exception;
        r5 = r8.isClosed();
        if (r5 != 0) goto L_0x0095;
    L_0x0092:
        r8.close();
    L_0x0095:
        throw r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.allapps.AppsBnrHelper.backupApps(android.content.Context, java.lang.String, org.xmlpull.v1.XmlSerializer, java.lang.String, com.android.launcher3.common.bnr.LauncherBnrListener$Result):void");
    }

    public String backupCategory() {
        if (FavoritesProvider.getInstance().getItemCount(LauncherBnrHelper.getFavoritesTable("appOrder")) > 0) {
            return "appOrder";
        }
        return null;
    }

    public void backupLayout(Context context, XmlSerializer serializer, String source, Result result) {
        Log.i(TAG, "backupLayout");
        this.mContext = context;
        if (!LCExtractor.LCEXTRACTOR_HOME_SOURCE.equals(source)) {
            if (LCExtractor.LCEXTRACTOR_APPS_SOURCE.equals(source)) {
                backupApps(context, "appOrder", serializer, source, result);
                return;
            }
            backupAppsViewType(serializer, result);
            backupAppsGrid(serializer, result);
            backupApps(context, "appOrder", serializer, source, result);
            if (LauncherFeature.supportEasyModeChange() && result.result == 0) {
                backupApps(context, LauncherBnrTag.TAG_APPORDER_EASY, serializer, source, result);
            }
        }
    }

    public void restoreLayout(Context context, XmlPullParser parser, ArrayList<String> arrayList, Result result) {
        FavoritesProvider favoritesProvider = FavoritesProvider.getInstance();
        if (favoritesProvider == null) {
            Log.i(TAG, "FavoritesProvider.getInstance() is null");
            result.result = 1;
            return;
        }
        Log.i(TAG, "restoreLayout");
        DefaultLayoutParser layoutParser = new AppsRestoreLayoutParser(context, favoritesProvider, parser);
        if (favoritesProvider.restoreAppsFavorites(layoutParser) < 0) {
            result.result = 1;
            return;
        }
        favoritesProvider.removeAndAddHiddenApp(new AppsDefaultLayoutParser(context, null, null, context.getResources(), 0), ((AppsRestoreLayoutParser) layoutParser).getRestoredCategory(), null);
    }

    private void backupAppsViewType(XmlSerializer serializer, Result result) {
        try {
            serializer.text("\n");
            String viewType = AppsController.getViewTypeFromSharedPreference(this.mContext);
            Log.d(TAG, "backupAppsViewType viewType = " + viewType);
            serializer.text("\n");
            serializer.startTag(null, LauncherBnrTag.TAG_VIEW_TYPE_APPORDER);
            if (ViewType.ALPHABETIC_GRID.name().equals(viewType)) {
                serializer.text(APPS_VIEW_TYPE_ALPHABETIC);
            } else {
                serializer.text(APPS_VIEW_TYPE_CUSTOM);
            }
            serializer.endTag(null, LauncherBnrTag.TAG_VIEW_TYPE_APPORDER);
        } catch (Exception e) {
            result.result = 1;
            Log.e(TAG, "backupAppsViewType Exception : " + e.toString());
        }
    }

    private void backupAppsGrid(XmlSerializer serializer, Result result) {
        try {
            int[] cellXY = new int[2];
            ScreenGridUtilities.loadCurrentAppsGridSize(this.mContext, cellXY);
            Log.d(TAG, "backupAppsGrid x = " + cellXY[0] + ", y = " + cellXY[1]);
            serializer.text("\n");
            serializer.startTag(null, LauncherBnrTag.TAG_ROWS_APPORDER);
            serializer.text(Integer.toString(cellXY[1]));
            serializer.endTag(null, LauncherBnrTag.TAG_ROWS_APPORDER);
            serializer.text("\n");
            serializer.startTag(null, LauncherBnrTag.TAG_COLUMNS_APPORDER);
            serializer.text(Integer.toString(cellXY[0]));
            serializer.endTag(null, LauncherBnrTag.TAG_COLUMNS_APPORDER);
        } catch (Exception e) {
            result.result = 1;
            Log.e(TAG, "backupAppsGrid Exception : " + e.toString());
        }
    }

    private void backupItem(String container, String source, Cursor cursor, ContentResolver cr, XmlSerializer serializer, Uri uri, Result result) throws IllegalArgumentException, IllegalStateException, IOException {
        boolean isLCExtractor = LCExtractor.LCEXTRACTOR_APPS_SOURCE.equals(source);
        String attrPrefix = "";
        if (isLCExtractor) {
            attrPrefix = "\n" + LCExtractor.getStrTab(2, this.mLauncherPrefix);
        } else {
            serializer.text("\n");
            serializer.startTag(null, container);
        }
        CursorInfo cursorInfo = new CursorInfo(cursor);
        do {
            long id = cursor.getLong(cursorInfo.idIndex);
            int itemType = cursor.getInt(cursorInfo.itemTypeIndex);
            int screen = cursor.getInt(cursorInfo.screenIndex);
            String title = cursor.getString(cursorInfo.titleIndex);
            String intent = cursor.getString(cursorInfo.intentIndex);
            int hidden = cursor.getInt(cursorInfo.hiddenIndex);
            int restore = cursor.getInt(cursorInfo.restoredIndex);
            boolean isVoiceSearch = false;
            if ((hidden == 0 && restore == 0) || isLCExtractor) {
                ComponentName componentName = null;
                if (!TextUtils.isEmpty(intent)) {
                    try {
                        componentName = Intent.parseUri(intent, 0).getComponent();
                    } catch (URISyntaxException e) {
                    }
                }
                String packageName = null;
                String className = null;
                if (componentName != null) {
                    packageName = componentName.getPackageName();
                    className = componentName.getClassName();
                }
                if (isLCExtractor) {
                    if (GOOGLE_VOICE_SEARCH_CLASS_NAME.equals(className)) {
                        isVoiceSearch = true;
                    }
                    serializer.text("\n" + LCExtractor.getStrTab(1, false));
                } else {
                    serializer.text("\n");
                }
                switch (itemType) {
                    case 0:
                        if (componentName != null) {
                            if (isLCExtractor) {
                                serializer.comment(' ' + Utilities.getAppLabel(this.mContext, packageName) + ' ');
                                serializer.text("\n" + LCExtractor.getStrTab(1, false));
                            }
                            serializer.startTag(null, DefaultLayoutParser.TAG_FAVORITE);
                            serializer.attribute(null, attrPrefix + "screen", String.valueOf(screen));
                            if (!TextUtils.isEmpty(packageName)) {
                                serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_PACKAGE_NAME, packageName);
                            }
                            if (!TextUtils.isEmpty(className)) {
                                serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_CLASS_NAME, className);
                            }
                            // TODO: Samsung specific code
//                            if (SCloudBnr.SCLOUD_SOURCE.equals(source)) {
//                                serializer.attribute(null, "restored", String.valueOf(4));
//                            }
                            if (isLCExtractor && isVoiceSearch) {
                                serializer.attribute(null, attrPrefix + "hidden", "true");
                            }
                            serializer.endTag(null, DefaultLayoutParser.TAG_FAVORITE);
                            break;
                        }
                        break;
                    case 2:
                        if (isLCExtractor) {
                            serializer.comment(" folder : " + title + ' ');
                            serializer.text("\n" + LCExtractor.getStrTab(1, false));
                        }
                        serializer.startTag(null, "folder");
                        serializer.attribute(null, attrPrefix + "screen", String.valueOf(screen));
                        if (!TextUtils.isEmpty(title)) {
                            serializer.attribute(null, attrPrefix + "title", title);
                        }
                        backupFolderItemById(id, source, cr, serializer, uri, result, isLCExtractor);
                        if (isLCExtractor) {
                            serializer.text(LCExtractor.getStrTab(1, false));
                        }
                        serializer.endTag(null, "folder");
                        break;
                    default:
                        break;
                }
            }
        } while (cursor.moveToNext());
        if (!isLCExtractor) {
            serializer.text("\n");
            serializer.endTag(null, container);
        }
    }

    private void backupFolderItemById(long folderId, String source, ContentResolver cr, XmlSerializer serializer, Uri uri, Result result, boolean isLCExtractor) throws IOException {
        String attrPrefix = "";
        Cursor cursor = cr.query(uri, null, "container=" + folderId + " and " + LauncherBnrHelper.getUserSelectionArg(this.mContext), null, BaseLauncherColumns.RANK);
        if (cursor == null) {
            result.result = 1;
            Log.e(TAG, "backupFolderItemById, fail to open cursor");
            return;
        }
        if (isLCExtractor) {
            serializer.text(LCExtractor.getStrTab(2, false));
            attrPrefix = "\n" + LCExtractor.getStrTab(3, this.mLauncherPrefix);
        }
        CursorInfo cursorInfo = new CursorInfo(cursor);
        while (cursor.moveToNext()) {
            try {
                int itemType = cursor.getInt(cursorInfo.itemTypeIndex);
                int rank = cursor.getInt(cursorInfo.rankIndex);
                String intent = cursor.getString(cursorInfo.intentIndex);
                int hidden = cursor.getInt(cursorInfo.hiddenIndex);
                int restore = cursor.getInt(cursorInfo.restoredIndex);
                if (hidden == 0 && restore == 0) {
                    serializer.text("\n");
                    ComponentName componentName = null;
                    if (!TextUtils.isEmpty(intent)) {
                        try {
                            componentName = Intent.parseUri(intent, 0).getComponent();
                        } catch (URISyntaxException e) {
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
                            if (componentName != null) {
                                if (isLCExtractor) {
                                    serializer.text(LCExtractor.getStrTab(2, false));
                                    serializer.comment(' ' + Utilities.getAppLabel(this.mContext, packageName) + ' ');
                                    serializer.text("\n" + LCExtractor.getStrTab(2, false));
                                }
                                serializer.startTag(null, DefaultLayoutParser.TAG_FAVORITE);
                                serializer.attribute(null, attrPrefix + "screen", String.valueOf(rank));
                                if (!TextUtils.isEmpty(packageName)) {
                                    serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_PACKAGE_NAME, packageName);
                                }
                                if (!TextUtils.isEmpty(className)) {
                                    serializer.attribute(null, attrPrefix + DefaultLayoutParser.ATTR_CLASS_NAME, className);
                                }
                                // TODO: Samsung specific code
//                                if (SCloudBnr.SCLOUD_SOURCE.equals(source)) {
//                                    serializer.attribute(null, "restored", String.valueOf(4));
//                                }
                                serializer.endTag(null, DefaultLayoutParser.TAG_FAVORITE);
                                break;
                            }
                            break;
                        default:
                            break;
                    }
                }
            } finally {
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            }
        }
        serializer.text("\n");
    }
}

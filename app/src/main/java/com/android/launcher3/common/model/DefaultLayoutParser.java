package com.android.launcher3.common.model;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherProvider.SqlArguments;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.bnr.LauncherBnrHelper;
import com.android.launcher3.common.compat.LauncherActivityInfoCompat;
import com.android.launcher3.common.compat.LauncherAppsCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.compat.UserManagerCompat;
import com.android.launcher3.common.customer.OpenMarketCustomization;
import com.android.launcher3.common.customer.OpenMarketCustomization.IconTitleValue;
import com.android.launcher3.common.customer.PostPositionController;
import com.android.launcher3.common.customer.PostPositionProvider;
import com.android.launcher3.common.customer.PostPositionSharedPref;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.util.BitmapUtils;
import com.sec.android.app.launcher.R;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public abstract class DefaultLayoutParser {
    private static final String ACTION_APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE = "com.android.launcher.action.APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE";
    protected static final String ATTR_APPSGRID_SUPPORTSET = "supportSet";
    private static final String ATTR_CARRIER = "carrier";
    public static final String ATTR_CLASS_NAME = "className";
    protected static final String ATTR_GRID_DEFAULT = "default";
    public static final String ATTR_HIDDEN = "hidden";
    public static final String ATTR_ICON = "icon";
    public static final String ATTR_ICON_PACKAGE = "iconPackage";
    public static final String ATTR_ICON_RESOURCE = "iconResource";
    private static final String ATTR_KEY = "key";
    public static final String ATTR_PACKAGE_NAME = "packageName";
    protected static final String ATTR_POST_POSITION = "postPosition";
    public static final String ATTR_RESERVED_FOLDER = "reservedFolder";
    public static final String ATTR_RESTORED = "restored";
    public static final String ATTR_SCREEN = "screen";
    public static final String ATTR_SPAN_X = "spanX";
    public static final String ATTR_SPAN_Y = "spanY";
    public static final String ATTR_TITLE = "title";
    public static final String ATTR_URI = "uri";
    private static final String ATTR_VALUE = "value";
    public static final String ATTR_VCF = "vcf";
    public static final String ATTR_WIDGET_ID = "appWidgetID";
    public static final String ATTR_X = "x";
    public static final String ATTR_Y = "y";
    static final String CSC_PATH = "/system/csc";
    private static final int INVALID_VALUE = -1;
    private static final String TAG = "DefaultLayoutParser";
    public static final String TAG_APPORDER = "appOrder";
    protected static final String TAG_APPSGRIDINFO = "appsGridInfo";
    public static final String TAG_APPWIDGET = "appwidget";
    public static final String TAG_AUTO_INSTALL = "autoinstall";
    public static final String TAG_DEEP_SHORTCUT = "deepshortcut";
    protected static final String TAG_EXTRA = "extra";
    public static final String TAG_FAVORITE = "favorite";
    public static final String TAG_FAVORITES = "favorites";
    public static final String TAG_FOLDER = "folder";
    public static final String TAG_HOME = "home";
    protected static final String TAG_HOMEGRIDINFO = "homeGridInfo";
    public static final String TAG_HOTSEAT = "hotseat";
    protected static final String TAG_NON_DISABLE_APPS = "nondisableapps";
    public static final String TAG_PAIRAPPS_SHORTCUT = "pairApps";
    public static final String TAG_SHORTCUT = "shortcut";
    static final String XML_DISABLE_APP_SKIP_LIST = "/default_disableapp_skiplist.xml";
    protected final AppWidgetHost mAppWidgetHost;
    private String[] mCSCFolderTitleKeyMap;
    protected final LayoutParserCallback mCallback;
    protected final Context mContext;
    protected SQLiteDatabase mDb;
    protected final int mLayoutId;
    private OpenMarketCustomization mOMC;
    protected final PackageManager mPackageManager;
    private boolean mReloadPostPosition = false;
    protected final String mRootTag;
    protected final Resources mSourceRes;
    protected final ContentValues mValues;

    private static class FolderTagInfo {
        protected int mFolderCellX;
        protected int mFolderCellY;
        protected int mFolderScreen;
        protected String mFolderTitle;
        protected boolean mIsPostPosition;

        private FolderTagInfo() {
            this.mFolderScreen = -1;
            this.mFolderCellX = -1;
            this.mFolderCellY = -1;
            this.mIsPostPosition = false;
        }

        void setFolderInfo(ContentValues values) {
            this.mFolderTitle = values.getAsString("title");
            if (values.getAsInteger("screen") != null) {
                this.mFolderScreen = values.getAsInteger("screen").intValue();
            }
            if (values.getAsInteger("cellX") != null) {
                this.mFolderCellX = values.getAsInteger("cellX").intValue();
            }
            if (values.getAsInteger("cellY") != null) {
                this.mFolderCellY = values.getAsInteger("cellY").intValue();
            }
        }

        void setPostPositionTag(boolean value) {
            this.mIsPostPosition = value;
        }
    }

    public interface LayoutParserCallback {
        long generateNewItemId();

        long insertAndCheck(SQLiteDatabase sQLiteDatabase, String str, ContentValues contentValues);
    }

    public interface TagParser {
        long parseAndAdd(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException;
    }

    protected class AppShortcutParser extends FolderTagInfo implements TagParser {
        protected boolean mIsRestore = false;

        protected AppShortcutParser() {
            super();
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) {
            String packageName = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_PACKAGE_NAME);
            String className = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_CLASS_NAME);
            if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(className)) {
                return invalidPackageOrClass(parser, tableName);
            }
            ComponentName cn = getComponent(parser, packageName, className);
            if (cn == null) {
                return -1;
            }
            String title = "";
            boolean isPAIorOMC = false;
            if (!this.mIsRestore && AutoInstallsLayout.isAutoInstallApp(packageName, className)) {
                DefaultLayoutParser.this.mValues.put("restored", Integer.valueOf(2));
                isPAIorOMC = true;
            } else if (this.mIsRestore || DefaultLayoutParser.this.mOMC == null || !DefaultLayoutParser.this.mOMC.hasPackage(packageName)) {
                boolean usePostPosition = this.mIsPostPosition || "true".equals(DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_POST_POSITION));
                if (usePostPosition && DefaultLayoutParser.this.isPostPositionInsertCondition(cn)) {
                    String cellX = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_X);
                    String cellY = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_Y);
                    String screen = DefaultLayoutParser.getAttributeValue(parser, "screen");
                    ContentValues cv = new ContentValues();
                    cv.put(PostPositionProvider.COL_COMPONENT_NAME, cn.flattenToShortString());
                    cv.put("itemType", Integer.valueOf(0));
                    cv.put(PostPositionProvider.COL_HOME_ADD, Boolean.valueOf(true));
                    if (this.mFolderTitle != null && !"".equals(this.mFolderTitle)) {
                        cv.put(PostPositionProvider.COL_HOME_PRELOADED_FOLDER, Boolean.valueOf(true));
                        cv.put(PostPositionProvider.COL_HOME_FOLDER_NAME, this.mFolderTitle);
                        if (this.mFolderScreen > -1 && this.mFolderCellX > -1 && this.mFolderCellY > -1) {
                            cv.put(PostPositionProvider.COL_HOME_INDEX, Integer.valueOf(this.mFolderScreen));
                            cv.put(PostPositionProvider.COL_HOME_CELL_X, Integer.valueOf(this.mFolderCellX));
                            cv.put(PostPositionProvider.COL_HOME_CELL_Y, Integer.valueOf(this.mFolderCellY));
                        }
                    } else if (!(screen == null || cellX == null || cellY == null)) {
                        cv.put(PostPositionProvider.COL_HOME_INDEX, Integer.valueOf(Integer.parseInt(screen)));
                        cv.put(PostPositionProvider.COL_HOME_CELL_X, Integer.valueOf(Integer.parseInt(cellX)));
                        cv.put(PostPositionProvider.COL_HOME_CELL_Y, Integer.valueOf(Integer.parseInt(cellY)));
                    }
                    PostPositionController.getInstance(DefaultLayoutParser.this.mContext).getProvider().dbInsertOrUpdate(cv);
                    return -1;
                }
            } else {
                IconTitleValue update = DefaultLayoutParser.this.mOMC.getIconInfo(packageName);
                DefaultLayoutParser.this.mValues.put("restored", Integer.valueOf(32));
                DefaultLayoutParser.this.mValues.put("icon", update.icon);
                title = update.title;
                isPAIorOMC = true;
                Log.d(DefaultLayoutParser.TAG, "update omc title and icon " + update.iconPackage + " title = " + update.title);
            }
            if (DefaultLayoutParser.this.mReloadPostPosition) {
                return -1;
            }
            int type;
            if (Boolean.parseBoolean(DefaultLayoutParser.getAttributeValue(parser, "hidden"))) {
                DefaultLayoutParser.this.mValues.put("hidden", Integer.valueOf(1));
                DefaultLayoutParser.this.mValues.put("screen", Integer.valueOf(-1));
            }
            if (!this.mIsRestore && !isPAIorOMC) {
                type = 1;
                for (LauncherActivityInfoCompat activity : LauncherAppsCompat.getInstance(DefaultLayoutParser.this.mContext).getActivityList(packageName, UserHandleCompat.myUserHandle())) {
                    if (activity.getComponentName() != null && activity.getComponentName().equals(cn)) {
                        type = 0;
                        break;
                    }
                }
            } else {
                type = 0;
            }
            return DefaultLayoutParser.this.addShortcut(tableName, title, new Intent("android.intent.action.MAIN", null).addCategory("android.intent.category.LAUNCHER").setComponent(cn).setFlags(/* TODO: Fix intent flag 270532608*/ Intent.FLAG_FROM_BACKGROUND), type);
        }

        protected long invalidPackageOrClass(XmlPullParser parser, String tableName) {
            Log.w(DefaultLayoutParser.TAG, "Skipping invalid <favorite> with no component");
            return -1;
        }

        protected ComponentName getComponent(XmlPullParser parser, String packageName, String className) {
            return new ComponentName(packageName, className);
        }
    }

    public class AppWidgetParser implements TagParser {
        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public long parseAndAdd(org.xmlpull.v1.XmlPullParser r23, java.lang.String r24) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
            /*
            r22 = this;
            r0 = r22;
            r3 = com.android.launcher3.common.model.DefaultLayoutParser.this;
            r3 = r3.mReloadPostPosition;
            if (r3 == 0) goto L_0x000d;
        L_0x000a:
            r12 = -1;
        L_0x000c:
            return r12;
        L_0x000d:
            r3 = "packageName";
            r0 = r23;
            r15 = com.android.launcher3.common.model.DefaultLayoutParser.getAttributeValue(r0, r3);
            r3 = "className";
            r0 = r23;
            r2 = com.android.launcher3.common.model.DefaultLayoutParser.getAttributeValue(r0, r3);
            r3 = android.text.TextUtils.isEmpty(r15);
            if (r3 != 0) goto L_0x0029;
        L_0x0023:
            r3 = android.text.TextUtils.isEmpty(r2);
            if (r3 == 0) goto L_0x0033;
        L_0x0029:
            r3 = "DefaultLayoutParser";
            r8 = "Skipping invalid <favorite> with no component";
            android.util.Log.d(r3, r8);
            r12 = -1;
            goto L_0x000c;
        L_0x0033:
            r0 = r22;
            r5 = r0.getWidgetComponent(r15, r2);
            if (r5 != 0) goto L_0x003e;
        L_0x003b:
            r12 = -1;
            goto L_0x000c;
        L_0x003e:
            r10 = new android.os.Bundle;
            r10.<init>();
            r18 = r23.getDepth();
        L_0x0047:
            r16 = r23.next();
            r3 = 3;
            r0 = r16;
            if (r0 != r3) goto L_0x0058;
        L_0x0050:
            r3 = r23.getDepth();
            r0 = r18;
            if (r3 <= r0) goto L_0x0093;
        L_0x0058:
            r3 = 2;
            r0 = r16;
            if (r0 != r3) goto L_0x0047;
        L_0x005d:
            r3 = "extra";
            r8 = r23.getName();
            r3 = r3.equals(r8);
            if (r3 == 0) goto L_0x008b;
        L_0x0069:
            r3 = "key";
            r0 = r23;
            r14 = com.android.launcher3.common.model.DefaultLayoutParser.getAttributeValue(r0, r3);
            r3 = "value";
            r0 = r23;
            r17 = com.android.launcher3.common.model.DefaultLayoutParser.getAttributeValue(r0, r3);
            if (r14 == 0) goto L_0x0083;
        L_0x007b:
            if (r17 == 0) goto L_0x0083;
        L_0x007d:
            r0 = r17;
            r10.putString(r14, r0);
            goto L_0x0047;
        L_0x0083:
            r3 = new java.lang.RuntimeException;
            r8 = "Widget extras must have a key and value";
            r3.<init>(r8);
            throw r3;
        L_0x008b:
            r3 = new java.lang.RuntimeException;
            r8 = "Widgets can contain only extras";
            r3.<init>(r8);
            throw r3;
        L_0x0093:
            r12 = -1;
            r4 = r22.getAppWidgetId();	 Catch:{ RuntimeException -> 0x0128 }
            r0 = r22;
            r3 = com.android.launcher3.common.model.DefaultLayoutParser.this;	 Catch:{ RuntimeException -> 0x0128 }
            r3 = r3.mCallback;	 Catch:{ RuntimeException -> 0x0128 }
            r6 = r3.generateNewItemId();	 Catch:{ RuntimeException -> 0x0128 }
            r3 = r22;
            r8 = r24;
            r3 = r3.bindAppWidget(r4, r5, r6, r8);	 Catch:{ RuntimeException -> 0x0128 }
            if (r3 != 0) goto L_0x00b1;
        L_0x00ad:
            r12 = -1;
            goto L_0x000c;
        L_0x00b1:
            r0 = r22;
            r3 = com.android.launcher3.common.model.DefaultLayoutParser.this;	 Catch:{ RuntimeException -> 0x0128 }
            r3 = r3.mValues;	 Catch:{ RuntimeException -> 0x0128 }
            r8 = "itemType";
            r19 = 4;
            r19 = java.lang.Integer.valueOf(r19);	 Catch:{ RuntimeException -> 0x0128 }
            r0 = r19;
            r3.put(r8, r0);	 Catch:{ RuntimeException -> 0x0128 }
            r0 = r22;
            r3 = com.android.launcher3.common.model.DefaultLayoutParser.this;	 Catch:{ RuntimeException -> 0x0128 }
            r3 = r3.mValues;	 Catch:{ RuntimeException -> 0x0128 }
            r8 = "appWidgetId";
            r19 = java.lang.Integer.valueOf(r4);	 Catch:{ RuntimeException -> 0x0128 }
            r0 = r19;
            r3.put(r8, r0);	 Catch:{ RuntimeException -> 0x0128 }
            r0 = r22;
            r3 = com.android.launcher3.common.model.DefaultLayoutParser.this;	 Catch:{ RuntimeException -> 0x0128 }
            r3 = r3.mValues;	 Catch:{ RuntimeException -> 0x0128 }
            r8 = "appWidgetProvider";
            r19 = r5.flattenToString();	 Catch:{ RuntimeException -> 0x0128 }
            r0 = r19;
            r3.put(r8, r0);	 Catch:{ RuntimeException -> 0x0128 }
            r0 = r22;
            r3 = com.android.launcher3.common.model.DefaultLayoutParser.this;	 Catch:{ RuntimeException -> 0x0128 }
            r3 = r3.mValues;	 Catch:{ RuntimeException -> 0x0128 }
            r8 = "_id";
            r19 = java.lang.Long.valueOf(r6);	 Catch:{ RuntimeException -> 0x0128 }
            r0 = r19;
            r3.put(r8, r0);	 Catch:{ RuntimeException -> 0x0128 }
            r0 = r22;
            r3 = com.android.launcher3.common.model.DefaultLayoutParser.this;	 Catch:{ RuntimeException -> 0x0128 }
            r3 = r3.mCallback;	 Catch:{ RuntimeException -> 0x0128 }
            r0 = r22;
            r8 = com.android.launcher3.common.model.DefaultLayoutParser.this;	 Catch:{ RuntimeException -> 0x0128 }
            r8 = r8.mDb;	 Catch:{ RuntimeException -> 0x0128 }
            r0 = r22;
            r0 = com.android.launcher3.common.model.DefaultLayoutParser.this;	 Catch:{ RuntimeException -> 0x0128 }
            r19 = r0;
            r0 = r19;
            r0 = r0.mValues;	 Catch:{ RuntimeException -> 0x0128 }
            r19 = r0;
            r0 = r24;
            r1 = r19;
            r12 = r3.insertAndCheck(r8, r0, r1);	 Catch:{ RuntimeException -> 0x0128 }
            r20 = 0;
            r3 = (r12 > r20 ? 1 : (r12 == r20 ? 0 : -1));
            if (r3 >= 0) goto L_0x0132;
        L_0x011d:
            r0 = r22;
            r3 = com.android.launcher3.common.model.DefaultLayoutParser.this;	 Catch:{ RuntimeException -> 0x0128 }
            r3 = r3.mAppWidgetHost;	 Catch:{ RuntimeException -> 0x0128 }
            r3.deleteAppWidgetId(r4);	 Catch:{ RuntimeException -> 0x0128 }
            goto L_0x000c;
        L_0x0128:
            r9 = move-exception;
            r3 = "DefaultLayoutParser";
            r8 = "Problem allocating appWidgetId";
            android.util.Log.e(r3, r8, r9);
            goto L_0x000c;
        L_0x0132:
            r3 = r10.isEmpty();	 Catch:{ RuntimeException -> 0x0128 }
            if (r3 != 0) goto L_0x000c;
        L_0x0138:
            r11 = new android.content.Intent;	 Catch:{ RuntimeException -> 0x0128 }
            r3 = "com.android.launcher.action.APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE";
            r11.<init>(r3);	 Catch:{ RuntimeException -> 0x0128 }
            r11.setComponent(r5);	 Catch:{ RuntimeException -> 0x0128 }
            r11.putExtras(r10);	 Catch:{ RuntimeException -> 0x0128 }
            r3 = "appWidgetId";
            r11.putExtra(r3, r4);	 Catch:{ RuntimeException -> 0x0128 }
            r0 = r22;
            r3 = com.android.launcher3.common.model.DefaultLayoutParser.this;	 Catch:{ RuntimeException -> 0x0128 }
            r3 = r3.mContext;	 Catch:{ RuntimeException -> 0x0128 }
            r3.sendBroadcast(r11);	 Catch:{ RuntimeException -> 0x0128 }
            goto L_0x000c;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.common.model.DefaultLayoutParser.AppWidgetParser.parseAndAdd(org.xmlpull.v1.XmlPullParser, java.lang.String):long");
        }

        protected ComponentName getWidgetComponent(String packageName, String className) {
            ComponentName cn = new ComponentName(packageName, className);
            try {
                DefaultLayoutParser.this.mPackageManager.getReceiverInfo(cn, 0);
            } catch (Exception e) {
                cn = new ComponentName(DefaultLayoutParser.this.mPackageManager.currentToCanonicalPackageNames(new String[]{packageName})[0], className);
                try {
                    DefaultLayoutParser.this.mPackageManager.getReceiverInfo(cn, 0);
                } catch (Exception e2) {
                    Log.d(DefaultLayoutParser.TAG, "Can't find widget provider: " + className);
                    return null;
                }
            }
            return cn;
        }

        protected int getAppWidgetId() {
            return DefaultLayoutParser.this.mAppWidgetHost.allocateAppWidgetId();
        }

        protected boolean bindAppWidget(int appWidgetId, ComponentName cn, long dbId, String tableName) {
            if (AppWidgetManager.getInstance(DefaultLayoutParser.this.mContext).bindAppWidgetIdIfAllowed(appWidgetId, cn)) {
                return true;
            }
            Log.e(DefaultLayoutParser.TAG, "Unable to bind app widget id " + cn);
            return false;
        }
    }

    protected class AppsFolderParser implements TagParser {
        private final HashMap<String, TagParser> mAppsFolderElements;
        private boolean mIsCSC;

        public AppsFolderParser(DefaultLayoutParser this$0) {
            this(this$0.getFolderElementsMap());
        }

        public AppsFolderParser(HashMap<String, TagParser> elements) {
            this.mIsCSC = false;
            this.mAppsFolderElements = elements;
        }

        public void setIsCSC(boolean isCSC) {
            this.mIsCSC = isCSC;
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            String title;
            if (this.mIsCSC) {
                title = DefaultLayoutParser.this.getCSCFolderTitleWithTranslation(DefaultLayoutParser.getAttributeValue(parser, "title"));
            } else {
                if (parser instanceof XmlResourceParser) {
                    int titleResId = DefaultLayoutParser.getAttributeResourceValue((XmlResourceParser) parser, "title", 0);
                    if (titleResId != 0) {
                        title = DefaultLayoutParser.this.mSourceRes.getString(titleResId);
                    } else {
                        title = DefaultLayoutParser.this.getCSCFolderTitleWithTranslation(DefaultLayoutParser.getAttributeValue(parser, "title"));
                    }
                } else {
                    title = DefaultLayoutParser.getAttributeValue(parser, "title");
                }
                if (title == null) {
                    title = "";
                }
            }
            String carrier = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_CARRIER);
            boolean postPosition = "true".equals(DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_POST_POSITION));
            long folderId = -1;
            DefaultLayoutParser.this.mValues.put("title", title);
            DefaultLayoutParser.this.mValues.put("itemType", Integer.valueOf(2));
            DefaultLayoutParser.this.mValues.put("_id", Long.valueOf(DefaultLayoutParser.this.mCallback.generateNewItemId()));
            if (!DefaultLayoutParser.this.mReloadPostPosition) {
                folderId = DefaultLayoutParser.this.mCallback.insertAndCheck(DefaultLayoutParser.this.mDb, tableName, DefaultLayoutParser.this.mValues);
                if (folderId < 0) {
                    Log.e(DefaultLayoutParser.TAG, "Unable to add folder");
                    return -1;
                }
            }
            ContentValues contentValues = new ContentValues(DefaultLayoutParser.this.mValues);
            ArrayList<Long> folderItems = new ArrayList();
            int folderDepth = parser.getDepth();
            int rank = 0;
            while (true) {
                int type = parser.next();
                if (type == 3 && parser.getDepth() <= folderDepth) {
                    break;
                } else if (type == 2) {
                    DefaultLayoutParser.this.mValues.clear();
                    DefaultLayoutParser.this.mValues.put("container", Long.valueOf(folderId));
                    DefaultLayoutParser.this.mValues.put(BaseLauncherColumns.RANK, Integer.valueOf(rank));
                    TagParser tagParser = (TagParser) this.mAppsFolderElements.get(parser.getName());
                    if (tagParser != null) {
                        if (tagParser instanceof FolderTagInfo) {
                            FolderTagInfo appsTag = (FolderTagInfo) tagParser;
                            appsTag.setFolderInfo(contentValues);
                            appsTag.setPostPositionTag(postPosition);
                        }
                        long id = tagParser.parseAndAdd(parser, tableName);
                        if (id >= 0) {
                            folderItems.add(Long.valueOf(id));
                            rank++;
                        }
                    } else {
                        throw new RuntimeException("Invalid folder item " + parser.getName());
                    }
                }
            }
            if (DefaultLayoutParser.this.mReloadPostPosition) {
                return -1;
            }
            PostPositionSharedPref ppPref = PostPositionController.getInstance(DefaultLayoutParser.this.mContext).getSharedPref(-102);
            long addedId = folderId;
            if (folderItems.size() < 2) {
                SqlArguments args = new SqlArguments(Favorites.getContentUri(folderId), null, null);
                DefaultLayoutParser.this.mDb.delete(args.table, args.where, args.args);
                if (folderItems.size() != 1) {
                    return -1;
                }
                ContentValues childValues = new ContentValues();
                DefaultLayoutParser.copyInteger(contentValues, childValues, "container");
                DefaultLayoutParser.copyInteger(contentValues, childValues, "screen");
                DefaultLayoutParser.copyInteger(contentValues, childValues, BaseLauncherColumns.RANK);
                addedId = ((Long) folderItems.get(0)).longValue();
                DefaultLayoutParser.this.mDb.update("favorites", childValues, "_id=" + addedId, null);
                if (ppPref == null) {
                    return addedId;
                }
                ppPref.writeFolderId(title, addedId, true);
                return addedId;
            } else if (ppPref == null) {
                return addedId;
            } else {
                ppPref.writePreloadedFolderId(title, folderId);
                return addedId;
            }
        }
    }

    protected class AppsParser extends FolderTagInfo implements TagParser {
        protected boolean mIsRestore = false;

        protected AppsParser() {
            super();
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) {
            String packageName = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_PACKAGE_NAME);
            String className = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_CLASS_NAME);
            if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(className)) {
                return invalidPackageOrClass(parser);
            }
            ComponentName cn = getComponent(parser, packageName, className);
            if (cn == null) {
                return -1;
            }
            String title = "";
            if (!this.mIsRestore && AutoInstallsLayout.isAutoInstallApp(packageName, className)) {
                DefaultLayoutParser.this.mValues.put("restored", Integer.valueOf(2));
            } else if (this.mIsRestore || DefaultLayoutParser.this.mOMC == null || !DefaultLayoutParser.this.mOMC.hasPackage(packageName)) {
                boolean usePostPosition = this.mIsPostPosition || "true".equals(DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_POST_POSITION));
                if (usePostPosition && DefaultLayoutParser.this.isPostPositionInsertCondition(cn) && this.mFolderTitle != null && !"".equals(this.mFolderTitle)) {
                    ContentValues cv = new ContentValues();
                    cv.put(PostPositionProvider.COL_COMPONENT_NAME, cn.flattenToShortString());
                    cv.put("itemType", Integer.valueOf(0));
                    cv.put(PostPositionProvider.COL_APPS_ADD, Boolean.valueOf(true));
                    cv.put(PostPositionProvider.COL_APPS_PRELOADED_FOLDER, Boolean.valueOf(true));
                    cv.put(PostPositionProvider.COL_APPS_FOLDER_NAME, this.mFolderTitle);
                    PostPositionController.getInstance(DefaultLayoutParser.this.mContext).getProvider().dbInsertOrUpdate(cv);
                    return -1;
                }
            } else {
                IconTitleValue update = DefaultLayoutParser.this.mOMC.getIconInfo(packageName);
                DefaultLayoutParser.this.mValues.put("restored", Integer.valueOf(32));
                DefaultLayoutParser.this.mValues.put("icon", update.icon);
                title = update.title;
                Log.d(DefaultLayoutParser.TAG, "update omc title and icon " + update.iconPackage + " title = " + update.title);
            }
            if (Boolean.parseBoolean(DefaultLayoutParser.getAttributeValue(parser, "hidden"))) {
                DefaultLayoutParser.this.mValues.put("hidden", Integer.valueOf(1));
            }
            return DefaultLayoutParser.this.addApps(tableName, title, cn, 0);
        }

        protected long invalidPackageOrClass(XmlPullParser parser) {
            Log.w(DefaultLayoutParser.TAG, "Skipping invalid <favorite> with no component");
            return -1;
        }

        protected ComponentName getComponent(XmlPullParser parser, String packageName, String className) {
            return new ComponentName(packageName, className);
        }
    }

    public class AutoInstallParser implements TagParser {
        public long parseAndAdd(XmlPullParser parser, String tableName) {
            String packageName = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_PACKAGE_NAME);
            String className = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_CLASS_NAME);
            if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(className)) {
                Log.d(DefaultLayoutParser.TAG, "Skipping invalid <favorite> with no component");
                return -1;
            }
            DefaultLayoutParser.this.mValues.put("restored", Integer.valueOf(2));
            return DefaultLayoutParser.this.addShortcut(tableName, "", new Intent("android.intent.action.MAIN", null).addCategory("android.intent.category.LAUNCHER").setComponent(new ComponentName(packageName, className)).setFlags(/* TODO: Fix intent flag 270532608 */ Intent.FLAG_FROM_BACKGROUND), 0);
        }
    }

    protected class FolderParser implements TagParser {
        private final HashMap<String, TagParser> mFolderElements;
        private boolean mIsCSC;
        private boolean mIsInvalidFolder;

        public FolderParser(DefaultLayoutParser this$0) {
            this(this$0.getFolderElementsMap());
        }

        public FolderParser(HashMap<String, TagParser> elements) {
            this.mIsCSC = false;
            this.mIsInvalidFolder = false;
            this.mFolderElements = elements;
        }

        public void setIsCSC(boolean isCSC) {
            this.mIsCSC = isCSC;
        }

        public void setInvalidFolder(boolean isInvalidFolder) {
            this.mIsInvalidFolder = isInvalidFolder;
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            String title;
            if (this.mIsCSC) {
                title = DefaultLayoutParser.this.getCSCFolderTitleWithTranslation(DefaultLayoutParser.getAttributeValue(parser, "title"));
            } else {
                if (parser instanceof XmlResourceParser) {
                    int titleResId = DefaultLayoutParser.getAttributeResourceValue((XmlResourceParser) parser, "title", 0);
                    if (titleResId != 0) {
                        title = DefaultLayoutParser.this.mSourceRes.getString(titleResId);
                    } else {
                        title = DefaultLayoutParser.this.getCSCFolderTitleWithTranslation(DefaultLayoutParser.getAttributeValue(parser, "title"));
                    }
                } else {
                    title = DefaultLayoutParser.getAttributeValue(parser, "title");
                }
                if (title == null) {
                    title = "";
                }
            }
            String carrier = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_CARRIER);
            boolean postPosition = "true".equals(DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_POST_POSITION));
            DefaultLayoutParser.this.mValues.put("title", title);
            DefaultLayoutParser.this.mValues.put("itemType", Integer.valueOf(2));
            DefaultLayoutParser.this.mValues.put("spanX", Integer.valueOf(1));
            DefaultLayoutParser.this.mValues.put("spanY", Integer.valueOf(1));
            DefaultLayoutParser.this.mValues.put("_id", Long.valueOf(DefaultLayoutParser.this.mCallback.generateNewItemId()));
            long folderId = -1;
            if (!(this.mIsInvalidFolder || DefaultLayoutParser.this.mReloadPostPosition)) {
                folderId = DefaultLayoutParser.this.mCallback.insertAndCheck(DefaultLayoutParser.this.mDb, tableName, DefaultLayoutParser.this.mValues);
                if (folderId < 0) {
                    Log.e(DefaultLayoutParser.TAG, "Unable to add folder");
                    return -1;
                }
            }
            ContentValues contentValues = new ContentValues(DefaultLayoutParser.this.mValues);
            ArrayList<Long> folderItems = new ArrayList();
            int folderDepth = parser.getDepth();
            int rank = 0;
            while (true) {
                int type = parser.next();
                if (type == 3 && parser.getDepth() <= folderDepth) {
                    break;
                } else if (type == 2 && !this.mIsInvalidFolder) {
                    DefaultLayoutParser.this.mValues.clear();
                    DefaultLayoutParser.this.mValues.put("container", Long.valueOf(folderId));
                    DefaultLayoutParser.this.mValues.put(BaseLauncherColumns.RANK, Integer.valueOf(rank));
                    TagParser tagParser = (TagParser) this.mFolderElements.get(parser.getName());
                    if (tagParser != null) {
                        if (tagParser instanceof FolderTagInfo) {
                            FolderTagInfo appShortcutTag = (FolderTagInfo) tagParser;
                            appShortcutTag.setFolderInfo(contentValues);
                            appShortcutTag.setPostPositionTag(postPosition);
                        }
                        long id = tagParser.parseAndAdd(parser, tableName);
                        if (id >= 0) {
                            folderItems.add(Long.valueOf(id));
                            rank++;
                        }
                    } else {
                        throw new RuntimeException("Invalid folder item " + parser.getName());
                    }
                }
            }
            if (this.mIsInvalidFolder) {
                this.mIsInvalidFolder = false;
                return -1;
            } else if (DefaultLayoutParser.this.mReloadPostPosition) {
                return -1;
            } else {
                PostPositionSharedPref ppPref = PostPositionController.getInstance(DefaultLayoutParser.this.mContext).getSharedPref(-100);
                long addedId = folderId;
                if (folderItems.size() < 2) {
                    SqlArguments args = new SqlArguments(Favorites.getContentUri(folderId), null, null);
                    DefaultLayoutParser.this.mDb.delete(args.table, args.where, args.args);
                    if (folderItems.size() != 1) {
                        return -1;
                    }
                    ContentValues childValues = new ContentValues();
                    DefaultLayoutParser.copyInteger(contentValues, childValues, "container");
                    DefaultLayoutParser.copyInteger(contentValues, childValues, "screen");
                    DefaultLayoutParser.copyInteger(contentValues, childValues, "cellX");
                    DefaultLayoutParser.copyInteger(contentValues, childValues, "cellY");
                    addedId = ((Long) folderItems.get(0)).longValue();
                    DefaultLayoutParser.this.mDb.update("favorites", childValues, "_id=" + addedId, null);
                    if (ppPref == null) {
                        return addedId;
                    }
                    ppPref.writeFolderId(title, addedId, true);
                    return addedId;
                } else if (ppPref == null) {
                    return addedId;
                } else {
                    ppPref.writePreloadedFolderId(title, folderId);
                    return addedId;
                }
            }
        }
    }

    public class ShortcutParser implements TagParser {
        private boolean mIsCSC = false;
        protected boolean mIsRestore = false;

        public void setIsCSC(boolean isCSC) {
            this.mIsCSC = isCSC;
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) {
            String title = null;
            Drawable icon = null;
            Intent intent = parseIntent(parser);
            if (intent == null) {
                return -1;
            }
            int type = 1;
            PackageManager pm = DefaultLayoutParser.this.mContext.getPackageManager();
            if (this.mIsCSC) {
                String titleId = DefaultLayoutParser.getAttributeValue(parser, "title");
                String imgId = DefaultLayoutParser.getAttributeValue(parser, "icon");
                // TODO: Samsung specific code
//                if (pm != null) {
//                    title = (String) DefaultLayoutParser.this.mContext.getPackageManager().semGetCscPackageItemText(titleId);
//                    icon = DefaultLayoutParser.this.mContext.getPackageManager().semGetCscPackageItemIcon(imgId);
//                }
                if (title == null || title.isEmpty() || icon == null) {
                    Log.w(DefaultLayoutParser.TAG, "Shortcut is missing title or icon resource ID from csc resource");
                    return -1;
                }
                ItemInfo.writeBitmap(DefaultLayoutParser.this.mValues, BitmapUtils.createIconBitmap(icon, DefaultLayoutParser.this.mContext));
                DefaultLayoutParser.this.mValues.put(BaseLauncherColumns.ICON_TYPE, Integer.valueOf(1));
            } else if (parser instanceof XmlResourceParser) {
                int titleResId = DefaultLayoutParser.getAttributeResourceValue((XmlResourceParser) parser, "title", 0);
                int iconId = DefaultLayoutParser.getAttributeResourceValue((XmlResourceParser) parser, "icon", 0);
                if (titleResId == 0 || iconId == 0) {
                    Log.d(DefaultLayoutParser.TAG, "Ignoring shortcut");
                    return -1;
                }
                title = DefaultLayoutParser.this.mSourceRes.getString(titleResId);
                icon = DefaultLayoutParser.this.mSourceRes.getDrawable(iconId);
                if (title == null || title.isEmpty() || icon == null) {
                    Log.d(DefaultLayoutParser.TAG, "Ignoring shortcut, can't load icon or title");
                    return -1;
                }
                ItemInfo.writeBitmap(DefaultLayoutParser.this.mValues, BitmapUtils.createIconBitmap(icon, DefaultLayoutParser.this.mContext));
                DefaultLayoutParser.this.mValues.put(BaseLauncherColumns.ICON_TYPE, Integer.valueOf(0));
                DefaultLayoutParser.this.mValues.put("iconPackage", DefaultLayoutParser.this.mSourceRes.getResourcePackageName(iconId));
                DefaultLayoutParser.this.mValues.put("iconResource", DefaultLayoutParser.this.mSourceRes.getResourceName(iconId));
            } else {
                title = DefaultLayoutParser.getAttributeValue(parser, "title");
                if (title == null) {
                    title = "";
                }
                String iconPackage = DefaultLayoutParser.getAttributeValue(parser, "iconPackage");
                String iconResource = DefaultLayoutParser.getAttributeValue(parser, "iconResource");
                if (iconPackage == null || iconResource == null) {
                    if (this.mIsRestore && Utilities.isLauncherAppTarget(intent) && pm != null && pm.resolveActivity(intent, 0) == null) {
                        Log.d(DefaultLayoutParser.TAG, "App shortcut, but not exist in pm");
                        ComponentName cn = LauncherBnrHelper.getChangedComponent(intent.getComponent());
                        if (cn != null) {
                            Log.d(DefaultLayoutParser.TAG, "App shortcut, changecomponent : " + cn);
                            intent.setComponent(cn);
                        }
                    }
                    if (LauncherFeature.supportDeepShortcut() && Utilities.isDeepShortcut(intent)) {
                        type = 6;
                        DefaultLayoutParser.this.mValues.put(BaseLauncherColumns.ICON_TYPE, Integer.valueOf(1));
                        Log.d(DefaultLayoutParser.TAG, "Deep shortcut, type change to deep shortcut");
                    } else if (LauncherFeature.supportPairApps() && DefaultLayoutParser.TAG_PAIRAPPS_SHORTCUT.equals(parser.getName())) {
                        type = 7;
                        DefaultLayoutParser.this.mValues.put(BaseLauncherColumns.ICON_TYPE, Integer.valueOf(1));
                        Log.d(DefaultLayoutParser.TAG, "Pair Apps shortcut, type change to pair apps shortcut");
                    } else {
                        String image = DefaultLayoutParser.getAttributeValue(parser, "icon");
                        if (image != null) {
                            byte[] iconBytes = Base64.decode(image, 2);
                            DefaultLayoutParser.this.mValues.put(BaseLauncherColumns.ICON_TYPE, Integer.valueOf(1));
                            DefaultLayoutParser.this.mValues.put("icon", iconBytes);
                        }
                    }
                } else {
                    DefaultLayoutParser.this.mValues.put(BaseLauncherColumns.ICON_TYPE, Integer.valueOf(0));
                    DefaultLayoutParser.this.mValues.put("iconPackage", iconPackage);
                    DefaultLayoutParser.this.mValues.put("iconResource", iconResource);
                }
            }
            return DefaultLayoutParser.this.addShortcut(tableName, title, intent, type);
        }

        protected Intent parseIntent(XmlPullParser parser) {
            String uri = null;
            try {
                uri = DefaultLayoutParser.getAttributeValue(parser, "uri");
                Intent intent = Intent.parseUri(uri, 0);
                // TODO: Fix intent flag
                //intent.setFlags(270532608);
                return intent;
            } catch (URISyntaxException e) {
                Log.w(DefaultLayoutParser.TAG, "Shortcut has malformed uri: " + uri);
                return null;
            }
        }
    }

    protected abstract HashMap<String, TagParser> getFolderElementsMap();

    protected abstract ArrayList<ComponentName> getHiddenApps();

    protected abstract HashMap<String, TagParser> getLayoutElementsMap();

    protected abstract int parseLayout(ArrayList<Long> arrayList);

    public DefaultLayoutParser(Context context, AppWidgetHost appWidgetHost, LayoutParserCallback callback, Resources res, int layoutId, String rootTag) {
        this.mContext = context;
        this.mAppWidgetHost = appWidgetHost;
        this.mCallback = callback;
        this.mPackageManager = context.getPackageManager();
        this.mValues = new ContentValues();
        this.mRootTag = rootTag;
        this.mSourceRes = res;
        this.mLayoutId = layoutId;
        this.mOMC = OpenMarketCustomization.getInstance();
    }

    public int loadLayout(SQLiteDatabase db, ArrayList<Long> screenIds) {
        this.mDb = db;
        try {
            return parseLayout(screenIds);
        } catch (Exception e) {
            Log.w(TAG, "Got exception parsing layout.", e);
            return -1;
        }
    }

    protected long addShortcut(String tableName, String title, Intent intent, int type) {
        if (this.mReloadPostPosition) {
            return -1;
        }
        long id = this.mCallback.generateNewItemId();
        this.mValues.put("intent", intent.toUri(0));
        this.mValues.put("title", title);
        this.mValues.put("itemType", Integer.valueOf(type));
        this.mValues.put("spanX", Integer.valueOf(1));
        this.mValues.put("spanY", Integer.valueOf(1));
        this.mValues.put("_id", Long.valueOf(id));
        if (this.mCallback.insertAndCheck(this.mDb, tableName, this.mValues) < 0) {
            return -1;
        }
        return id;
    }

    protected long addApps(String tableName, String title, ComponentName cn, int type) {
        if (this.mReloadPostPosition) {
            return -1;
        }
        long id = this.mCallback.generateNewItemId();
        this.mValues.put("intent", IconInfo.makeLaunchIntent(cn, UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(UserHandleCompat.myUserHandle())).toUri(0));
        this.mValues.put("title", title);
        this.mValues.put("itemType", Integer.valueOf(type));
        this.mValues.put("_id", Long.valueOf(id));
        if (this.mCallback.insertAndCheck(this.mDb, tableName, this.mValues) < 0) {
            return -1;
        }
        return id;
    }

    boolean isPostPositionInsertCondition(ComponentName cn) {
        boolean updateToSAPP = true;
        if (this.mReloadPostPosition) {
            return true;
        }
        if (Utilities.isPackageExist(this.mContext, cn.getPackageName())) {
            updateToSAPP = false;
        }
        if (updateToSAPP || getActivityInfo(cn, UserHandleCompat.myUserHandle()) != null) {
            return updateToSAPP;
        }
        return true;
    }

    protected String getCSCFolderTitleWithTranslation(String title) {
        if (this.mCSCFolderTitleKeyMap == null) {
            this.mCSCFolderTitleKeyMap = this.mContext.getResources().getStringArray(R.array.csc_folder_title_for_translation);
        }
        if (this.mCSCFolderTitleKeyMap != null && this.mCSCFolderTitleKeyMap.length > 0) {
            for (String titleList : this.mCSCFolderTitleKeyMap) {
                if (titleList.startsWith(title + '|')) {
                    try {
                        int resId = this.mContext.getResources().getIdentifier(titleList.replace(title + '|', ""), "string", this.mContext.getPackageName());
                        if (resId > 0) {
                            title = this.mContext.getResources().getString(resId);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Could not catch getIdentifier from resource : " + title);
                    }
                    return title;
                }
            }
        }
        return title;
    }

    public static final void beginDocument(XmlPullParser parser, String firstElementName) throws XmlPullParserException, IOException {
        int type;
        do {
            type = parser.next();
            if (type == 2) {
                break;
            }
        } while (type != 1);
        if (type != 2) {
            throw new XmlPullParserException("No start tag found");
        } else if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() + ", expected " + firstElementName);
        }
    }

    public static String getAttributeValue(XmlPullParser parser, String attribute) {
        String value = parser.getAttributeValue("http://schemas.android.com/apk/res-auto/com.android.launcher3", attribute);
        if (value == null) {
            return parser.getAttributeValue(null, attribute);
        }
        return value;
    }

    protected static int getAttributeResourceValue(XmlResourceParser parser, String attribute, int defaultValue) {
        int value = parser.getAttributeResourceValue("http://schemas.android.com/apk/res-auto/com.android.launcher3", attribute, defaultValue);
        if (value == defaultValue) {
            return parser.getAttributeResourceValue(null, attribute, defaultValue);
        }
        return value;
    }

    static void copyInteger(ContentValues from, ContentValues to, String key) {
        to.put(key, from.getAsInteger(key));
    }

    public String chooseFilePath(String omcPath, String cscPath) {
        if (new File(omcPath).exists()) {
            Log.d(TAG, "checkOMCFilePath => omcFile exists, omc file path : " + omcPath + " will return.");
            return omcPath;
        }
        Log.d(TAG, "checkOMCFilePath => omcFile : " + omcPath + " not exists, csc file path : " + cscPath + " will return.");
        return cscPath;
    }

    private LauncherActivityInfoCompat getActivityInfo(ComponentName cmpName, UserHandleCompat user) {
        List<LauncherActivityInfoCompat> apps = LauncherAppsCompat.getInstance(this.mContext).getActivityList(cmpName.getPackageName(), user);
        if (apps != null) {
            for (LauncherActivityInfoCompat i : apps) {
                if (cmpName.equals(i.getComponentName())) {
                    return i;
                }
            }
        }
        return null;
    }

    public void setReloadPostPosition(boolean value) {
        this.mReloadPostPosition = value;
    }

    boolean isReloadPostPosition() {
        return this.mReloadPostPosition;
    }
}

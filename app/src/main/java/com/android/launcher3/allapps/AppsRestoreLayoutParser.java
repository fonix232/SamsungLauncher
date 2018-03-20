package com.android.launcher3.allapps;

import android.content.ComponentName;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.controller.AppsController;
import com.android.launcher3.allapps.controller.AppsController.ViewType;
import com.android.launcher3.allapps.model.AppsDefaultLayoutParser;
import com.android.launcher3.allapps.model.AppsDefaultLayoutParser.DefaultAppsFolderParser;
import com.android.launcher3.allapps.model.AppsDefaultLayoutParser.DefaultAppsParser;
import com.android.launcher3.common.bnr.LauncherBnrHelper;
import com.android.launcher3.common.bnr.LauncherBnrTag;
import com.android.launcher3.common.deviceprofile.GridInfo;
import com.android.launcher3.common.model.DefaultLayoutParser;
import com.android.launcher3.common.model.DefaultLayoutParser.TagParser;
import com.android.launcher3.common.model.FavoritesProvider;
import com.android.launcher3.common.model.LauncherSettings.ChangeLogColumns;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.util.ScreenGridUtilities;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class AppsRestoreLayoutParser extends AppsDefaultLayoutParser {
    private static final String TAG = "Launcher.AppsRestore";
    private int mColumns;
    private FavoritesProvider mFavoritesProvider;
    private XmlPullParser mParser;
    private int mRows;
    private HashMap<String, TagParser> mTagParserMap = new HashMap();
    private String restoredCategory;

    private class CategoryParser implements TagParser {
        private CategoryParser() {
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            if (parser.next() == 4) {
                String category = parser.getText();
                Log.d(AppsRestoreLayoutParser.TAG, "restore category : " + category);
                AppsRestoreLayoutParser.this.restoredCategory = category;
                if (category == null) {
                    Log.i(AppsRestoreLayoutParser.TAG, "category is null!!");
                    return -1;
                } else if (category.contains("appOrder")) {
                    AppsRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_VIEW_TYPE_APPORDER, new ViewTypeParser());
                    AppsRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_ROWS_APPORDER, new RowsParser());
                    AppsRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_COLUMNS_APPORDER, new ColumnsParser());
                    AppsRestoreLayoutParser.this.mTagParserMap.put("appOrder", null);
                    if (LauncherFeature.supportEasyModeChange()) {
                        AppsRestoreLayoutParser.this.mTagParserMap.put(LauncherBnrTag.TAG_APPORDER_EASY, null);
                    }
                } else {
                    Log.i(AppsRestoreLayoutParser.TAG, "there is no appOrder in category");
                }
            }
            return 0;
        }
    }

    private class ColumnsParser implements TagParser {
        private ColumnsParser() {
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            if (parser.next() == 4) {
                AppsRestoreLayoutParser.this.mColumns = Integer.parseInt(parser.getText());
                Log.i(AppsRestoreLayoutParser.TAG, "restore columns : " + AppsRestoreLayoutParser.this.mColumns);
                if (LauncherFeature.supportFlexibleGrid()) {
                    ArrayList<GridInfo> appsGridInfo = LauncherAppState.getInstance().getDeviceProfile().getAppsGridInfo();
                    boolean found = false;
                    if (appsGridInfo != null) {
                        Iterator it = appsGridInfo.iterator();
                        while (it.hasNext()) {
                            GridInfo info = (GridInfo) it.next();
                            if (info.getCellCountX() == AppsRestoreLayoutParser.this.mColumns && info.getCellCountY() == AppsRestoreLayoutParser.this.mRows) {
                                found = true;
                                Log.i(AppsRestoreLayoutParser.TAG, "restore apps grid x = " + AppsRestoreLayoutParser.this.mColumns + ", y = " + AppsRestoreLayoutParser.this.mRows);
                                break;
                            }
                        }
                    }
                    if (found) {
                        ScreenGridUtilities.storeAppsGridLayoutPreference(AppsRestoreLayoutParser.this.mContext, AppsRestoreLayoutParser.this.mColumns, AppsRestoreLayoutParser.this.mRows);
                    }
                } else if (Utilities.isDeskTopMode(AppsRestoreLayoutParser.this.mContext)) {
                    Log.d(AppsRestoreLayoutParser.TAG, "restore apps grid in desktop mode");
                    ScreenGridUtilities.storeAppsGridLayoutPreference(AppsRestoreLayoutParser.this.mContext, AppsRestoreLayoutParser.this.mColumns, AppsRestoreLayoutParser.this.mRows);
                }
            }
            return 0;
        }
    }

    private class RowsParser implements TagParser {
        private RowsParser() {
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            if (parser.next() == 4) {
                AppsRestoreLayoutParser.this.mRows = Integer.parseInt(parser.getText());
                Log.i(AppsRestoreLayoutParser.TAG, "restore rows : " + AppsRestoreLayoutParser.this.mRows);
            }
            return 0;
        }
    }

    private class ViewTypeParser implements TagParser {
        private ViewTypeParser() {
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            if (parser.next() == 4) {
                String viewType = parser.getText();
                Log.i(AppsRestoreLayoutParser.TAG, "restore view type : " + viewType);
                if ("ALPHABETIC".equals(viewType)) {
                    AppsController.setViewTypeFromSharedPreference(AppsRestoreLayoutParser.this.mContext, ViewType.ALPHABETIC_GRID);
                } else {
                    AppsController.setViewTypeFromSharedPreference(AppsRestoreLayoutParser.this.mContext, ViewType.CUSTOM_GRID);
                }
            }
            return 0;
        }
    }

    private class RestoreAppsFolderParser extends DefaultAppsFolderParser {
        private RestoreAppsFolderParser() {
            super();
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            AppsRestoreLayoutParser.this.mValues.put(ChangeLogColumns.MODIFIED, Long.valueOf(System.currentTimeMillis()));
            return super.parseAndAdd(parser, tableName);
        }
    }

    private class RestoreAppsParser extends DefaultAppsParser {
        private RestoreAppsParser() {
            super();
            this.mIsRestore = true;
        }

        protected ComponentName getComponent(XmlPullParser parser, String packageName, String className) {
            String restoredStr = DefaultLayoutParser.getAttributeValue(parser, "restored");
            int restored = 0;
            if (!TextUtils.isEmpty(restoredStr)) {
                restored = Integer.parseInt(restoredStr);
                AppsRestoreLayoutParser.this.mValues.put("restored", Integer.valueOf(restored));
            } else if (LauncherBnrHelper.getUsePlayStore()) {
                AppsRestoreLayoutParser.this.mValues.put("restored", Integer.valueOf(64));
            }
            AppsRestoreLayoutParser.this.mValues.put(ChangeLogColumns.MODIFIED, Long.valueOf(System.currentTimeMillis()));
            return LauncherBnrHelper.getComponent(AppsRestoreLayoutParser.this.mContext, restored, packageName, className);
        }
    }

    AppsRestoreLayoutParser(Context context, FavoritesProvider favoritesProvider, XmlPullParser parser) {
        super(context, null, favoritesProvider, context.getResources(), 0, null);
        this.mFavoritesProvider = favoritesProvider;
        this.mParser = parser;
    }

    protected HashMap<String, TagParser> getFolderElementsMap() {
        HashMap<String, TagParser> parsers = new HashMap();
        parsers.put(DefaultLayoutParser.TAG_FAVORITE, new RestoreAppsParser());
        return parsers;
    }

    protected HashMap<String, TagParser> getLayoutElementsMap() {
        HashMap<String, TagParser> parsers = new HashMap();
        parsers.put(DefaultLayoutParser.TAG_FAVORITE, new RestoreAppsParser());
        parsers.put("folder", new RestoreAppsFolderParser());
        return parsers;
    }

    protected int parseLayout(ArrayList<Long> screenIds) {
        Exception e;
        this.mRank = 0;
        this.mTagParserMap.clear();
        this.mTagParserMap.put("category", new CategoryParser());
        try {
            HashMap<String, TagParser> tagParserMap = getLayoutElementsMap();
            int depth = this.mParser.getDepth();
            while (true) {
                int type = this.mParser.next();
                if ((type == 3 && this.mParser.getDepth() <= depth) || type == 1) {
                    break;
                } else if (type == 2) {
                    String name = this.mParser.getName();
                    if (this.mTagParserMap.containsKey(name)) {
                        Log.i(TAG, "restore tag : " + name);
                        String tableName = LauncherBnrHelper.getFavoritesTable(name);
                        if ("appOrder".equals(name) || LauncherBnrTag.TAG_APPORDER_EASY.equals(name)) {
                            this.mFavoritesProvider.setMaxItemId(this.mFavoritesProvider.initializeMaxItemId(tableName));
                            defaultAppsParseAndAddNode(this.mParser, tableName, tagParserMap, screenIds, Favorites.CONTAINER_APPS);
                            if (LauncherBnrTag.TAG_APPORDER_EASY.equals(name)) {
                                this.restoredCategory += ",easy";
                            }
                        } else if (((TagParser) this.mTagParserMap.get(name)).parseAndAdd(this.mParser, tableName) < 0) {
                            return -1;
                        }
                    } else {
                        continue;
                    }
                }
            }
        } catch (XmlPullParserException e2) {
            e = e2;
        } catch (IOException e3) {
            e = e3;
        }
        return this.mRank;
        this.mRank = -1;
        Log.e(TAG, "Got exception parsing restore appOrder.", e);
        return this.mRank;
    }

    public String getRestoredCategory() {
        return this.restoredCategory;
    }
}

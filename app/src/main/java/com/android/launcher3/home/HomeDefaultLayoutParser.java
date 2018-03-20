package com.android.launcher3.home;

import android.appwidget.AppWidgetHost;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.customer.PostPositionController;
import com.android.launcher3.common.customer.PostPositionProvider;
import com.android.launcher3.common.model.DefaultLayoutParser;
import com.android.launcher3.common.model.DefaultLayoutParser.AppWidgetParser;
import com.android.launcher3.common.model.DefaultLayoutParser.AutoInstallParser;
import com.android.launcher3.common.model.DefaultLayoutParser.LayoutParserCallback;
import com.android.launcher3.common.model.DefaultLayoutParser.ShortcutParser;
import com.android.launcher3.common.model.DefaultLayoutParser.TagParser;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.util.ScreenGridUtilities;
import com.sec.android.app.launcher.R;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class HomeDefaultLayoutParser extends DefaultLayoutParser {
    private static final String CSC_PATH = "/system/csc";
    private static final String OMC_ETC_PATH = "/etc";
    private static final String TAG = "HomeDefaultLayoutParser";
    private static final String XML_WORKSPACE = "/default_workspace.xml";
    private static final String XML_WORKSPACE_EASY = "/default_workspace_easy.xml";
    private static final String XML_WORKSPACE_GUEST = "/default_workspace_guest.xml";
    private static final String XML_WORKSPACE_HOMEONLY = "/default_workspace_homeonly.xml";
    private static final String XML_WORKSPACE_HOMEONLY_KNOX = "/default_workspace_homeonly_knox.xml";
    private static final String XML_WORKSPACE_HOMEONLY_SECUREFOLDER = "/default_workspace_homeonly_securefolder.xml";
    private static final String XML_WORKSPACE_KNOX = "/default_workspace_knox.xml";
    private static final String XML_WORKSPACE_SECUREFOLDER = "/default_workspace_securefolder.xml";
    private final String mDefaultWorkspacePath;
    private String mDefaultWorkspacePathEasy;
    private String mDefaultWorkspacePathGuest;
    private String mDefaultWorkspacePathHomeOnly;
    private String mDefaultWorkspacePathHomeOnlyKnox;
    private String mDefaultWorkspacePathHomeOnlySecureFolder;
    private String mDefaultWorkspacePathKnox;
    private String mDefaultWorkspacePathSecureFolder;
    private int mHomeParseContainer;
    private boolean mIsCSC;
    private final int[] mUsedGridSize;

    private class HomeGridInfoParser implements TagParser {
        private HomeGridInfoParser() {
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            String defaultGrid = DefaultLayoutParser.getAttributeValue(parser, "default");
            if (!(defaultGrid == null || defaultGrid.isEmpty())) {
                try {
                    String[] gridXY = defaultGrid.split(DefaultLayoutParser.ATTR_X);
                    int[] newGrid = new int[2];
                    if (ScreenGridUtilities.findNearestGridSize(HomeDefaultLayoutParser.this.mContext, newGrid, Integer.valueOf(gridXY[0]).intValue(), Integer.valueOf(gridXY[1]).intValue())) {
                        HomeDefaultLayoutParser.this.changeGridAndLayout(newGrid[0], newGrid[1]);
                        Log.d(HomeDefaultLayoutParser.TAG, "write default homegrid to preference : " + newGrid[0] + DefaultLayoutParser.ATTR_X + newGrid[1]);
                    }
                } catch (Exception e) {
                    Log.d(HomeDefaultLayoutParser.TAG, "write default homegrid exception : " + e);
                }
            }
            return -1;
        }
    }

    class AppShortcutWithUriParser extends AppShortcutParser {
        AppShortcutWithUriParser() {
            super();
        }

        protected long invalidPackageOrClass(XmlPullParser parser, String tableName) {
            String uri = DefaultLayoutParser.getAttributeValue(parser, "uri");
            if (TextUtils.isEmpty(uri)) {
                Log.e(HomeDefaultLayoutParser.TAG, "Skipping invalid <favorite> with no component or uri");
                return -1;
            }
            try {
                Intent metaIntent = Intent.parseUri(uri, 0);
                ResolveInfo resolved = HomeDefaultLayoutParser.this.mPackageManager.resolveActivity(metaIntent, 65536);
                List<ResolveInfo> appList = HomeDefaultLayoutParser.this.mPackageManager.queryIntentActivities(metaIntent, 65536);
                if (wouldLaunchResolverActivity(resolved, appList)) {
                    ResolveInfo systemApp = getSingleSystemActivity(appList);
                    if (systemApp == null) {
                        Log.w(HomeDefaultLayoutParser.TAG, "No preference or single system activity found for " + metaIntent.toString());
                        return -1;
                    }
                    resolved = systemApp;
                }
                ActivityInfo info = resolved.activityInfo;
                Intent intent = HomeDefaultLayoutParser.this.mPackageManager.getLaunchIntentForPackage(info.packageName);
                if (intent == null) {
                    return -1;
                }
                intent.setFlags(270532608);
                return HomeDefaultLayoutParser.this.addShortcut(tableName, info.loadLabel(HomeDefaultLayoutParser.this.mPackageManager).toString(), intent, 0);
            } catch (URISyntaxException e) {
                Log.e(HomeDefaultLayoutParser.TAG, "Unable to add meta-favorite: " + uri, e);
                return -1;
            }
        }

        private ResolveInfo getSingleSystemActivity(List<ResolveInfo> appList) {
            ResolveInfo systemResolve = null;
            int N = appList.size();
            int i = 0;
            while (i < N) {
                try {
                    if ((HomeDefaultLayoutParser.this.mPackageManager.getApplicationInfo(((ResolveInfo) appList.get(i)).activityInfo.packageName, 0).flags & 1) != 0) {
                        if (systemResolve != null) {
                            return null;
                        }
                        systemResolve = (ResolveInfo) appList.get(i);
                    }
                    i++;
                } catch (NameNotFoundException e) {
                    Log.w(HomeDefaultLayoutParser.TAG, "Unable to get info about resolve results", e);
                    return null;
                }
            }
            return systemResolve;
        }

        private boolean wouldLaunchResolverActivity(ResolveInfo resolved, List<ResolveInfo> appList) {
            for (int i = 0; i < appList.size(); i++) {
                ResolveInfo tmp = (ResolveInfo) appList.get(i);
                if (tmp.activityInfo.name.equals(resolved.activityInfo.name) && tmp.activityInfo.packageName.equals(resolved.activityInfo.packageName)) {
                    return false;
                }
            }
            return true;
        }
    }

    class DefaultAppShortcutParser extends AppShortcutParser {
        DefaultAppShortcutParser() {
            super();
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) {
            HomeDefaultLayoutParser.this.mValues.put("container", Integer.valueOf(HomeDefaultLayoutParser.this.mHomeParseContainer));
            HomeDefaultLayoutParser.this.mValues.put("screen", DefaultLayoutParser.getAttributeValue(parser, "screen"));
            String cellX = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_X);
            String cellY = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_Y);
            if (HomeDefaultLayoutParser.this.mHomeParseContainer == -100) {
                HomeDefaultLayoutParser.this.mValues.put("cellX", cellX);
                HomeDefaultLayoutParser.this.mValues.put("cellY", cellY);
            }
            long result = super.parseAndAdd(parser, tableName);
            if (result >= 0 && HomeDefaultLayoutParser.this.mHomeParseContainer == -100) {
                HomeDefaultLayoutParser.this.setUsedGridSize(cellX, cellY, null, null);
            }
            return result;
        }
    }

    class DefaultAppWidgetParser extends AppWidgetParser {
        DefaultAppWidgetParser() {
            super();
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            HomeDefaultLayoutParser.this.mValues.put("container", Integer.valueOf(-100));
            String screen = DefaultLayoutParser.getAttributeValue(parser, "screen");
            HomeDefaultLayoutParser.this.mValues.put("screen", screen);
            String cellX = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_X);
            String cellY = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_Y);
            String spanX = DefaultLayoutParser.getAttributeValue(parser, "spanX");
            String spanY = DefaultLayoutParser.getAttributeValue(parser, "spanY");
            HomeDefaultLayoutParser.this.mValues.put("cellX", cellX);
            HomeDefaultLayoutParser.this.mValues.put("cellY", cellY);
            HomeDefaultLayoutParser.this.mValues.put("spanX", spanX);
            HomeDefaultLayoutParser.this.mValues.put("spanY", spanY);
            long result = super.parseAndAdd(parser, tableName);
            if (result < 0 || HomeDefaultLayoutParser.this.mHomeParseContainer != -100) {
                if ("true".equals(DefaultLayoutParser.getAttributeValue(parser, "postPosition"))) {
                    String packageName = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_PACKAGE_NAME);
                    String className = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_CLASS_NAME);
                    if (!(TextUtils.isEmpty(packageName) || TextUtils.isEmpty(className))) {
                        ComponentName cn = new ComponentName(packageName, className);
                        ContentValues cv = new ContentValues();
                        cv.put(PostPositionProvider.COL_COMPONENT_NAME, cn.flattenToShortString());
                        cv.put("itemType", Integer.valueOf(1));
                        cv.put(PostPositionProvider.COL_HOME_ADD, Boolean.valueOf(true));
                        cv.put(PostPositionProvider.COL_HOME_INDEX, Integer.valueOf(Integer.parseInt(screen)));
                        cv.put(PostPositionProvider.COL_HOME_CELL_X, Integer.valueOf(Integer.parseInt(cellX)));
                        cv.put(PostPositionProvider.COL_HOME_CELL_Y, Integer.valueOf(Integer.parseInt(cellY)));
                        cv.put(PostPositionProvider.COL_HOME_WIDGET_SPAN_X, Integer.valueOf(Integer.parseInt(spanX)));
                        cv.put(PostPositionProvider.COL_HOME_WIDGET_SPAN_Y, Integer.valueOf(Integer.parseInt(spanY)));
                        PostPositionController.getInstance(HomeDefaultLayoutParser.this.mContext).getProvider().dbInsertOrUpdate(cv);
                    }
                }
            } else {
                HomeDefaultLayoutParser.this.setUsedGridSize(cellX, cellY, spanX, spanY);
            }
            return result;
        }
    }

    private class DefaultHomeAutoInstallParser extends AutoInstallParser {
        private DefaultHomeAutoInstallParser() {
            super();
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) {
            HomeDefaultLayoutParser.this.mValues.put("container", Integer.valueOf(HomeDefaultLayoutParser.this.mHomeParseContainer));
            HomeDefaultLayoutParser.this.mValues.put("screen", DefaultLayoutParser.getAttributeValue(parser, "screen"));
            String cellX = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_X);
            String cellY = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_Y);
            if (HomeDefaultLayoutParser.this.mHomeParseContainer == -100) {
                HomeDefaultLayoutParser.this.mValues.put("cellX", cellX);
                HomeDefaultLayoutParser.this.mValues.put("cellY", cellY);
            }
            long result = super.parseAndAdd(parser, tableName);
            if (result >= 0 && HomeDefaultLayoutParser.this.mHomeParseContainer == -100) {
                HomeDefaultLayoutParser.this.setUsedGridSize(cellX, cellY, null, null);
            }
            return result;
        }
    }

    class DefaultHomeFolderParser extends FolderParser {
        DefaultHomeFolderParser() {
            super(HomeDefaultLayoutParser.this);
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            setIsCSC(HomeDefaultLayoutParser.this.mIsCSC);
            HomeDefaultLayoutParser.this.mValues.put("container", Integer.valueOf(HomeDefaultLayoutParser.this.mHomeParseContainer));
            String screen = DefaultLayoutParser.getAttributeValue(parser, "screen");
            String cellX = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_X);
            String cellY = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_Y);
            HomeDefaultLayoutParser.this.mValues.put("screen", screen);
            HomeDefaultLayoutParser.this.mValues.put("cellX", cellX);
            HomeDefaultLayoutParser.this.mValues.put("cellY", cellY);
            if (Integer.valueOf(screen).intValue() < 0) {
                setInvalidFolder(true);
            }
            long result = super.parseAndAdd(parser, tableName);
            if (result >= 0 && HomeDefaultLayoutParser.this.mHomeParseContainer == -100) {
                HomeDefaultLayoutParser.this.setUsedGridSize(cellX, cellY, null, null);
            }
            return result;
        }
    }

    class UriShortcutParser extends ShortcutParser {
        UriShortcutParser() {
            super();
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) {
            setIsCSC(HomeDefaultLayoutParser.this.mIsCSC);
            HomeDefaultLayoutParser.this.mValues.put("container", Integer.valueOf(HomeDefaultLayoutParser.this.mHomeParseContainer));
            HomeDefaultLayoutParser.this.mValues.put("screen", DefaultLayoutParser.getAttributeValue(parser, "screen"));
            String cellX = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_X);
            String cellY = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_Y);
            if (HomeDefaultLayoutParser.this.mHomeParseContainer == -100) {
                HomeDefaultLayoutParser.this.mValues.put("cellX", cellX);
                HomeDefaultLayoutParser.this.mValues.put("cellY", cellY);
            }
            long result = super.parseAndAdd(parser, tableName);
            if (result >= 0 && HomeDefaultLayoutParser.this.mHomeParseContainer == -100) {
                HomeDefaultLayoutParser.this.setUsedGridSize(cellX, cellY, null, null);
            }
            return result;
        }
    }

    HomeDefaultLayoutParser(Context context, AppWidgetHost appWidgetHost, LayoutParserCallback callback, Resources sourceRes, int layoutId) {
        super(context, appWidgetHost, callback, sourceRes, layoutId, "favorites");
        this.mDefaultWorkspacePath = chooseFilePath(LauncherFeature.getOmcPath() + XML_WORKSPACE, "/system/csc/default_workspace.xml");
        this.mDefaultWorkspacePathGuest = null;
        this.mDefaultWorkspacePathKnox = null;
        this.mDefaultWorkspacePathHomeOnly = null;
        this.mDefaultWorkspacePathHomeOnlyKnox = null;
        this.mDefaultWorkspacePathEasy = null;
        this.mDefaultWorkspacePathSecureFolder = null;
        this.mDefaultWorkspacePathHomeOnlySecureFolder = null;
        this.mIsCSC = false;
        this.mUsedGridSize = new int[2];
        this.mHomeParseContainer = -100;
        String omcPath = LauncherFeature.getOmcPath();
        String omc_etcPath = omcPath + OMC_ETC_PATH;
        if (omcPath.startsWith("/odm/")) {
            omc_etcPath = (omcPath + "/").replace("/conf/", OMC_ETC_PATH);
        }
        this.mDefaultWorkspacePathGuest = chooseFilePath(omc_etcPath + XML_WORKSPACE_GUEST, "/system/csc/default_workspace_guest.xml");
        this.mDefaultWorkspacePathKnox = chooseFilePath(omc_etcPath + XML_WORKSPACE_KNOX, "/system/csc/default_workspace_knox.xml");
        this.mDefaultWorkspacePathEasy = chooseFilePath(omc_etcPath + XML_WORKSPACE_EASY, "/system/csc/default_workspace_easy.xml");
        this.mDefaultWorkspacePathHomeOnly = chooseFilePath(omc_etcPath + XML_WORKSPACE_HOMEONLY, "/system/csc/default_workspace_homeonly.xml");
        this.mDefaultWorkspacePathHomeOnlyKnox = chooseFilePath(omc_etcPath + XML_WORKSPACE_HOMEONLY_KNOX, "/system/csc/default_workspace_homeonly_knox.xml");
        this.mDefaultWorkspacePathSecureFolder = chooseFilePath(omc_etcPath + XML_WORKSPACE_SECUREFOLDER, "/system/csc/default_workspace_securefolder.xml");
        this.mDefaultWorkspacePathHomeOnlySecureFolder = chooseFilePath(omc_etcPath + XML_WORKSPACE_HOMEONLY_SECUREFOLDER, "/system/csc/default_workspace_homeonly_securefolder.xml");
    }

    public HomeDefaultLayoutParser(Context context, AppWidgetHost appWidgetHost, LayoutParserCallback callback, Resources sourceRes, int layoutId, String rootTag) {
        super(context, appWidgetHost, callback, sourceRes, layoutId, rootTag);
        this.mDefaultWorkspacePath = chooseFilePath(LauncherFeature.getOmcPath() + XML_WORKSPACE, "/system/csc/default_workspace.xml");
        this.mDefaultWorkspacePathGuest = null;
        this.mDefaultWorkspacePathKnox = null;
        this.mDefaultWorkspacePathHomeOnly = null;
        this.mDefaultWorkspacePathHomeOnlyKnox = null;
        this.mDefaultWorkspacePathEasy = null;
        this.mDefaultWorkspacePathSecureFolder = null;
        this.mDefaultWorkspacePathHomeOnlySecureFolder = null;
        this.mIsCSC = false;
        this.mUsedGridSize = new int[2];
        this.mHomeParseContainer = -100;
    }

    protected HashMap<String, TagParser> getFolderElementsMap() {
        HashMap<String, TagParser> parsers = new HashMap();
        parsers.put(DefaultLayoutParser.TAG_FAVORITE, new AppShortcutWithUriParser());
        parsers.put(DefaultLayoutParser.TAG_SHORTCUT, new ShortcutParser());
        parsers.put(DefaultLayoutParser.TAG_AUTO_INSTALL, new AutoInstallParser());
        return parsers;
    }

    protected HashMap<String, TagParser> getLayoutElementsMap() {
        HashMap<String, TagParser> parsers = new HashMap();
        parsers.put(DefaultLayoutParser.TAG_FAVORITE, new DefaultAppShortcutParser());
        parsers.put(DefaultLayoutParser.TAG_APPWIDGET, new DefaultAppWidgetParser());
        parsers.put(DefaultLayoutParser.TAG_SHORTCUT, new UriShortcutParser());
        parsers.put("folder", new DefaultHomeFolderParser());
        parsers.put(DefaultLayoutParser.TAG_AUTO_INSTALL, new DefaultHomeAutoInstallParser());
        parsers.put("homeGridInfo", new HomeGridInfoParser());
        return parsers;
    }

    protected int parseLayout(ArrayList<Long> screenIds) {
        File fileCheck;
        Exception e;
        Throwable th;
        XmlPullParser parser;
        FileReader fileReader = null;
        int count = 0;
        int[] iArr = this.mUsedGridSize;
        this.mUsedGridSize[1] = 0;
        iArr[0] = 0;
        boolean isGuest = Utilities.isGuest();
        boolean isKnoxMode = Utilities.isKnoxMode();
        boolean isHomeOnly = LauncherAppState.getInstance().isHomeOnlyModeEnabled();
        boolean isEasyMode = LauncherAppState.getInstance().isEasyModeEnabled();
        boolean isSecureFolderMode = Utilities.isSecureFolderMode();
        if (isSecureFolderMode) {
            if (isHomeOnly) {
                try {
                    fileCheck = new File(this.mDefaultWorkspacePathHomeOnlySecureFolder);
                } catch (XmlPullParserException e2) {
                    e = e2;
                    e = e;
                    try {
                        Exception e3;
                        Log.e(TAG, "Got exception parsing favorites.", e3);
                        if (fileReader != null) {
                            try {
                                fileReader.close();
                            } catch (Exception e32) {
                                Log.e(TAG, "Got exception parsing favorites.", e32);
                            }
                        }
                        return count;
                    } catch (Throwable th2) {
                        th = th2;
                        if (fileReader != null) {
                            try {
                                fileReader.close();
                            } catch (Exception e322) {
                                Log.e(TAG, "Got exception parsing favorites.", e322);
                            }
                        }
                        throw th;
                    }
                } catch (IOException e4) {
                    e = e4;
                    e322 = e;
                    Log.e(TAG, "Got exception parsing favorites.", e322);
                    if (fileReader != null) {
                        fileReader.close();
                    }
                    return count;
                } catch (NotFoundException e5) {
                    e = e5;
                    e322 = e;
                    Log.e(TAG, "Got exception parsing favorites.", e322);
                    if (fileReader != null) {
                        fileReader.close();
                    }
                    return count;
                }
            }
            fileCheck = new File(this.mDefaultWorkspacePathSecureFolder);
        } else if (isKnoxMode) {
            if (isHomeOnly) {
                fileCheck = new File(this.mDefaultWorkspacePathHomeOnlyKnox);
            } else {
                fileCheck = new File(this.mDefaultWorkspacePathKnox);
            }
        } else if (isEasyMode) {
            fileCheck = new File(this.mDefaultWorkspacePathEasy);
        } else if (this.mLayoutId != 0) {
            fileCheck = null;
        } else if (isGuest) {
            fileCheck = new File(this.mDefaultWorkspacePathGuest);
            if (!fileCheck.isFile() || fileCheck.length() <= 0) {
                fileCheck = new File(this.mDefaultWorkspacePath);
            }
        } else if (isHomeOnly) {
            fileCheck = new File(this.mDefaultWorkspacePathHomeOnly);
            if (!fileCheck.isFile() || fileCheck.length() <= 0) {
                fileCheck = new File(this.mDefaultWorkspacePath);
            }
        } else {
            fileCheck = new File(this.mDefaultWorkspacePath);
        }
        if (fileCheck == null || !fileCheck.isFile() || fileCheck.length() <= 0) {
            int resId;
            if (isSecureFolderMode) {
                if (isHomeOnly) {
                    resId = R.xml.default_workspace_homeonly_securefolder;
                } else {
                    resId = R.xml.default_workspace_securefolder;
                }
            } else if (isKnoxMode) {
                if (isHomeOnly) {
                    resId = R.xml.default_workspace_homeonly_knox;
                } else {
                    resId = R.xml.default_workspace_knox;
                }
            } else if (isEasyMode) {
                resId = R.xml.default_workspace_easy;
            } else if (this.mLayoutId != 0) {
                resId = this.mLayoutId;
            } else if (isGuest) {
                resId = R.xml.default_workspace_guest;
            } else if (isHomeOnly) {
                resId = R.xml.default_workspace_homeonly;
            } else {
                resId = R.xml.default_workspace;
            }
            parser = this.mSourceRes.getXml(resId);
        } else {
            FileReader fileReader2 = new FileReader(fileCheck);
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                parser = factory.newPullParser();
                parser.setInput(fileReader2);
                this.mIsCSC = true;
                fileReader = fileReader2;
            } catch (XmlPullParserException e6) {
                e = e6;
                fileReader = fileReader2;
                e322 = e;
                Log.e(TAG, "Got exception parsing favorites.", e322);
                if (fileReader != null) {
                    fileReader.close();
                }
                return count;
            } catch (IOException e7) {
                e = e7;
                fileReader = fileReader2;
                e322 = e;
                Log.e(TAG, "Got exception parsing favorites.", e322);
                if (fileReader != null) {
                    fileReader.close();
                }
                return count;
            } catch (NotFoundException e8) {
                e = e8;
                fileReader = fileReader2;
                e322 = e;
                Log.e(TAG, "Got exception parsing favorites.", e322);
                if (fileReader != null) {
                    fileReader.close();
                }
                return count;
            } catch (Throwable th3) {
                th = th3;
                fileReader = fileReader2;
                if (fileReader != null) {
                    fileReader.close();
                }
                throw th;
            }
        }
        if (parser != null) {
            DefaultLayoutParser.beginDocument(parser, this.mRootTag);
            int depth = parser.getDepth();
            HashMap<String, TagParser> tagParserMap = getLayoutElementsMap();
            while (true) {
                int type = parser.next();
                if ((type != 3 || parser.getDepth() > depth) && type != 1) {
                    if (type == 2) {
                        String tagName = parser.getName();
                        if ("home".equals(tagName)) {
                            count += defaultHomeParseAndAddNode(parser, "favorites", tagParserMap, screenIds, -100);
                        } else if ("hotseat".equals(tagName)) {
                            count += defaultHomeParseAndAddNode(parser, "favorites", tagParserMap, screenIds, Favorites.CONTAINER_HOTSEAT);
                        } else {
                            Log.e(TAG, "invalid tag : " + tagName);
                        }
                    }
                }
            }
            checkValidCurrentGrid();
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (Exception e3222) {
                    Log.e(TAG, "Got exception parsing favorites.", e3222);
                }
            }
            return count;
        }
        checkValidCurrentGrid();
        if (fileReader != null) {
            fileReader.close();
        }
        return count;
    }

    int defaultHomeParseAndAddNode(XmlPullParser parser, String tableName, HashMap<String, TagParser> tagParserMap, ArrayList<Long> screenIds, int container) throws XmlPullParserException, IOException {
        this.mHomeParseContainer = container;
        int startDepth = parser.getDepth();
        int homeItemCount = 0;
        while (true) {
            int type = parser.next();
            if ((type != 3 || parser.getDepth() > startDepth) && type != 1) {
                if (type == 2) {
                    this.mValues.clear();
                    TagParser tagParser;
                    if ("homeGridInfo".equals(parser.getName())) {
                        tagParser = (TagParser) tagParserMap.get(parser.getName());
                        if (tagParser != null) {
                            tagParser.parseAndAdd(parser, tableName);
                        }
                    } else {
                        long screenId = Long.parseLong(DefaultLayoutParser.getAttributeValue(parser, "screen"));
                        tagParser = (TagParser) tagParserMap.get(parser.getName());
                        if (tagParser == null) {
                            Log.d(TAG, "Ignoring unknown element tag: " + parser.getName());
                        } else if (tagParser.parseAndAdd(parser, tableName) >= 0) {
                            if (container == -100 && screenId >= 0 && screenIds != null) {
                                if (!screenIds.contains(Long.valueOf(screenId))) {
                                    screenIds.add(Long.valueOf(screenId));
                                }
                            }
                            homeItemCount++;
                        }
                    }
                }
            }
        }
        return homeItemCount;
    }

    private void checkValidCurrentGrid() {
        int[] newGrid = new int[2];
        int[] currentGrid = new int[2];
        ScreenGridUtilities.findNearestGridSize(this.mContext, newGrid, this.mUsedGridSize[0], this.mUsedGridSize[1]);
        Utilities.loadCurrentGridSize(this.mContext, currentGrid);
        if (newGrid[0] > currentGrid[0] || newGrid[1] > currentGrid[1]) {
            changeGridAndLayout(newGrid[0], newGrid[1]);
            Log.i(TAG, "changeScreenGrid currentGrid x : " + currentGrid[0] + ", y : " + currentGrid[1]);
            Log.i(TAG, "changeScreenGrid newGrid x : " + newGrid[0] + ", y : " + newGrid[1] + " isHomeOnly : " + LauncherAppState.getInstance().isHomeOnlyModeEnabled());
        }
    }

    private void setUsedGridSize(String cellX, String cellY, String cellSpanX, String cellSpanY) {
        if (cellX != null && cellY != null) {
            int x = Integer.parseInt(cellX);
            int y = Integer.parseInt(cellY);
            int spanX = 1;
            int spanY = 1;
            if (!(cellSpanX == null || cellSpanY == null)) {
                spanX = Integer.parseInt(cellSpanX);
                spanY = Integer.parseInt(cellSpanY);
            }
            if (x + spanX > this.mUsedGridSize[0]) {
                this.mUsedGridSize[0] = x + spanX;
            }
            if (y + spanY > this.mUsedGridSize[1]) {
                this.mUsedGridSize[1] = y + spanY;
            }
        }
    }

    private void changeGridAndLayout(int cellX, int cellY) {
        ScreenGridUtilities.storeGridLayoutPreference(this.mContext, cellX, cellY, LauncherAppState.getInstance().isHomeOnlyModeEnabled());
        LauncherAppState.getInstance().getDeviceProfile().setCurrentGrid(cellX, cellY);
        ScreenGridUtilities.storeCurrentScreenGridSetting(this.mContext, cellX, cellY);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                LauncherModel model = LauncherAppState.getInstance().getModel();
                if (model != null && model.getCallback() != null) {
                    model.getCallback().relayoutLauncher();
                }
            }
        });
    }

    protected ArrayList<ComponentName> getHiddenApps() {
        return new ArrayList();
    }
}

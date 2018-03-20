package com.android.launcher3.allapps.model;

import android.appwidget.AppWidgetHost;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.util.Log;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.customer.PostPositionController;
import com.android.launcher3.common.customer.PostPositionSharedPref;
import com.android.launcher3.common.model.DefaultLayoutParser;
import com.android.launcher3.common.model.DefaultLayoutParser.AutoInstallParser;
import com.android.launcher3.common.model.DefaultLayoutParser.LayoutParserCallback;
import com.android.launcher3.common.model.DefaultLayoutParser.TagParser;
import com.android.launcher3.common.model.LauncherSettings.BaseLauncherColumns;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import com.android.launcher3.util.ScreenGridUtilities;
import com.sec.android.app.launcher.R;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class AppsDefaultLayoutParser extends DefaultLayoutParser {
    static final String CSC_PATH = "/system/csc";
    private static final String OMC_ETC_PATH = "/etc";
    private static final String TAG = "AppsDefaultLayoutParser";
    static final String XML_APPORDER = "/default_application_order.xml";
    static final String XML_APPORDER_GUEST = "/default_application_order_guest.xml";
    static final String XML_APPORDER_KNOX = "/default_application_order_knox.xml";
    private String OMC_PATH;
    private String mAppOrderPath;
    private String mAppOrderPathGuest;
    private String mAppOrderPathKnox;
    private long mAppsParseContainer;
    private boolean mIsCSC;
    private boolean mIsGuest;
    private boolean mIsKnoxMode;
    protected int mRank;

    private class AppsGridInfoParser implements TagParser {
        private AppsGridInfoParser() {
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            String defaultGrid = DefaultLayoutParser.getAttributeValue(parser, "default");
            if (!(defaultGrid == null || defaultGrid.isEmpty())) {
                String[] gridXY = defaultGrid.split(DefaultLayoutParser.ATTR_X);
                int gridX = Integer.valueOf(gridXY[0]).intValue();
                int gridY = Integer.valueOf(gridXY[1]).intValue();
                ScreenGridUtilities.storeAppsGridLayoutPreference(AppsDefaultLayoutParser.this.mContext, gridX, gridY);
                Log.d(AppsDefaultLayoutParser.TAG, "write default appsgrid to preference from omc : " + gridX + DefaultLayoutParser.ATTR_X + gridY);
            }
            String supportSet = DefaultLayoutParser.getAttributeValue(parser, "supportSet");
            if (!(supportSet == null || supportSet.isEmpty())) {
                ScreenGridUtilities.storeAppsSupportedGridSet(AppsDefaultLayoutParser.this.mContext, supportSet);
                Log.d(AppsDefaultLayoutParser.TAG, "write appsgridset to preference from omc : " + supportSet);
            }
            return -1;
        }
    }

    class DefaultAppAutoInstallParser extends AutoInstallParser {
        DefaultAppAutoInstallParser() {
            super();
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) {
            if (AppsDefaultLayoutParser.this.mAppsParseContainer == -102) {
                AppsDefaultLayoutParser.this.mValues.put(BaseLauncherColumns.RANK, Integer.valueOf(AppsDefaultLayoutParser.this.mRank));
                AppsDefaultLayoutParser.this.mValues.put("screen", DefaultLayoutParser.getAttributeValue(parser, "screen"));
                AppsDefaultLayoutParser.this.mValues.put("container", Integer.valueOf(Favorites.CONTAINER_APPS));
            }
            return super.parseAndAdd(parser, tableName);
        }
    }

    public class DefaultAppsFolderParser extends AppsFolderParser {
        public DefaultAppsFolderParser() {
            super(AppsDefaultLayoutParser.this);
        }

        public /* bridge */ /* synthetic */ void setIsCSC(boolean z) {
            super.setIsCSC(z);
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) throws XmlPullParserException, IOException {
            setIsCSC(AppsDefaultLayoutParser.this.mIsCSC);
            AppsDefaultLayoutParser.this.mValues.put(BaseLauncherColumns.RANK, Integer.valueOf(AppsDefaultLayoutParser.this.mRank));
            AppsDefaultLayoutParser.this.mValues.put("screen", DefaultLayoutParser.getAttributeValue(parser, "screen"));
            AppsDefaultLayoutParser.this.mValues.put("container", Integer.valueOf(Favorites.CONTAINER_APPS));
            AppsDefaultLayoutParser.this.mAppsParseContainer = -1;
            return super.parseAndAdd(parser, tableName);
        }
    }

    public class DefaultAppsParser extends AppsParser {
        public DefaultAppsParser() {
            super();
        }

        public long parseAndAdd(XmlPullParser parser, String tableName) {
            if (AppsDefaultLayoutParser.this.mAppsParseContainer == -102) {
                AppsDefaultLayoutParser.this.mValues.put(BaseLauncherColumns.RANK, Integer.valueOf(AppsDefaultLayoutParser.this.mRank));
                AppsDefaultLayoutParser.this.mValues.put("screen", DefaultLayoutParser.getAttributeValue(parser, "screen"));
                AppsDefaultLayoutParser.this.mValues.put("container", Integer.valueOf(Favorites.CONTAINER_APPS));
            }
            return super.parseAndAdd(parser, tableName);
        }
    }

    public AppsDefaultLayoutParser(Context context, AppWidgetHost appWidgetHost, LayoutParserCallback callback, Resources sourceRes, int layoutId) {
        super(context, appWidgetHost, callback, sourceRes, layoutId, "appOrder");
        this.mAppOrderPath = null;
        this.mAppOrderPathGuest = null;
        this.mAppOrderPathKnox = null;
        this.mIsCSC = false;
        this.mIsKnoxMode = false;
        this.mIsGuest = false;
        this.mAppsParseContainer = -102;
        this.OMC_PATH = LauncherFeature.getOmcPath();
        this.mAppOrderPath = chooseFilePath(this.OMC_PATH + XML_APPORDER, "/system/csc/default_application_order.xml");
        this.mAppOrderPathGuest = chooseFilePath(this.OMC_PATH + OMC_ETC_PATH + XML_APPORDER_GUEST, "/system/csc/default_application_order_guest.xml");
        this.mAppOrderPathKnox = chooseFilePath(this.OMC_PATH + OMC_ETC_PATH + XML_APPORDER_KNOX, "/system/csc/default_application_order_knox.xml");
    }

    public AppsDefaultLayoutParser(Context context, AppWidgetHost appWidgetHost, LayoutParserCallback callback, Resources sourceRes, int layoutId, String rootTag) {
        super(context, appWidgetHost, callback, sourceRes, layoutId, rootTag);
        this.mAppOrderPath = null;
        this.mAppOrderPathGuest = null;
        this.mAppOrderPathKnox = null;
        this.mIsCSC = false;
        this.mIsKnoxMode = false;
        this.mIsGuest = false;
        this.mAppsParseContainer = -102;
    }

    protected HashMap<String, TagParser> getFolderElementsMap() {
        HashMap<String, TagParser> parsers = new HashMap();
        parsers.put(DefaultLayoutParser.TAG_FAVORITE, new DefaultAppsParser());
        parsers.put(DefaultLayoutParser.TAG_AUTO_INSTALL, new AutoInstallParser());
        return parsers;
    }

    protected HashMap<String, TagParser> getLayoutElementsMap() {
        HashMap<String, TagParser> parsers = new HashMap();
        parsers.put(DefaultLayoutParser.TAG_FAVORITE, new DefaultAppsParser());
        parsers.put("folder", new DefaultAppsFolderParser());
        parsers.put(DefaultLayoutParser.TAG_AUTO_INSTALL, new DefaultAppAutoInstallParser());
        parsers.put("appsGridInfo", new AppsGridInfoParser());
        return parsers;
    }

    protected int parseLayout(ArrayList<Long> screenIds) {
        XmlPullParserException e;
        Throwable th;
        IOException e2;
        NotFoundException e3;
        FileReader fileReader = null;
        this.mIsGuest = Utilities.isGuest();
        this.mIsKnoxMode = Utilities.isKnoxMode();
        try {
            File fileCheck;
            XmlPullParser parser;
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            if (this.mIsKnoxMode) {
                fileCheck = new File(this.mAppOrderPathKnox);
            } else if (this.mLayoutId != 0) {
                fileCheck = null;
            } else if (this.mIsGuest) {
                fileCheck = new File(this.mAppOrderPathGuest);
                if (fileCheck == null || !fileCheck.isFile() || fileCheck.length() <= 0) {
                    fileCheck = new File(this.mAppOrderPath);
                }
            } else {
                fileCheck = new File(this.mAppOrderPath);
            }
            if (fileCheck == null || !fileCheck.isFile() || fileCheck.length() <= 0) {
                int resId;
                if (this.mIsKnoxMode) {
                    resId = R.xml.default_application_order_knox;
                } else if (this.mLayoutId != 0) {
                    resId = this.mLayoutId;
                } else if (this.mIsGuest) {
                    resId = R.xml.default_application_order_guest;
                } else {
                    resId = R.xml.default_application_order;
                }
                parser = this.mSourceRes.getXml(resId);
            } else {
                FileReader fileReader2 = new FileReader(fileCheck);
                try {
                    parser = factory.newPullParser();
                    parser.setInput(fileReader2);
                    this.mIsCSC = true;
                    fileReader = fileReader2;
                } catch (XmlPullParserException e4) {
                    e = e4;
                    fileReader = fileReader2;
                    try {
                        Log.e(TAG, "Got exception parsing appOrder.", e);
                        if (fileReader != null) {
                            try {
                                fileReader.close();
                            } catch (Exception e5) {
                                Log.e(TAG, "Got exception parsing appOrder.", e5);
                            }
                        }
                        return this.mRank;
                    } catch (Throwable th2) {
                        th = th2;
                        if (fileReader != null) {
                            try {
                                fileReader.close();
                            } catch (Exception e52) {
                                Log.e(TAG, "Got exception parsing appOrder.", e52);
                            }
                        }
                        throw th;
                    }
                } catch (IOException e6) {
                    e2 = e6;
                    fileReader = fileReader2;
                    Log.e(TAG, "Got exception parsing appOrder.", e2);
                    if (fileReader != null) {
                        try {
                            fileReader.close();
                        } catch (Exception e522) {
                            Log.e(TAG, "Got exception parsing appOrder.", e522);
                        }
                    }
                    return this.mRank;
                } catch (NotFoundException e7) {
                    e3 = e7;
                    fileReader = fileReader2;
                    Log.e(TAG, "Got exception parsing appOrder.", e3);
                    if (fileReader != null) {
                        try {
                            fileReader.close();
                        } catch (Exception e5222) {
                            Log.e(TAG, "Got exception parsing appOrder.", e5222);
                        }
                    }
                    return this.mRank;
                } catch (Throwable th3) {
                    th = th3;
                    fileReader = fileReader2;
                    if (fileReader != null) {
                        fileReader.close();
                    }
                    throw th;
                }
            }
            DefaultLayoutParser.beginDocument(parser, this.mRootTag);
            defaultAppsParseAndAddNode(parser, "favorites", getLayoutElementsMap(), screenIds, Favorites.CONTAINER_APPS);
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (Exception e52222) {
                    Log.e(TAG, "Got exception parsing appOrder.", e52222);
                }
            }
        } catch (XmlPullParserException e8) {
            e = e8;
            Log.e(TAG, "Got exception parsing appOrder.", e);
            if (fileReader != null) {
                fileReader.close();
            }
            return this.mRank;
        } catch (IOException e9) {
            e2 = e9;
            Log.e(TAG, "Got exception parsing appOrder.", e2);
            if (fileReader != null) {
                fileReader.close();
            }
            return this.mRank;
        } catch (NotFoundException e10) {
            e3 = e10;
            Log.e(TAG, "Got exception parsing appOrder.", e3);
            if (fileReader != null) {
                fileReader.close();
            }
            return this.mRank;
        }
        return this.mRank;
    }

    protected void defaultAppsParseAndAddNode(XmlPullParser parser, String tableName, HashMap<String, TagParser> tagParserMap, ArrayList<Long> screenIds, int container) throws XmlPullParserException, IOException {
        int startDepth = parser.getDepth();
        this.mRank = 0;
        while (true) {
            int type = parser.next();
            if ((type == 3 && parser.getDepth() <= startDepth) || type == 1) {
                return;
            }
            if (type == 2) {
                this.mValues.clear();
                TagParser tagParser;
                if ("appsGridInfo".equals(parser.getName())) {
                    tagParser = (TagParser) tagParserMap.get(parser.getName());
                    if (tagParser != null) {
                        tagParser.parseAndAdd(parser, tableName);
                    }
                } else {
                    String screenStr = DefaultLayoutParser.getAttributeValue(parser, "screen");
                    if (screenStr != null) {
                        long screenId = Long.parseLong(screenStr);
                        tagParser = (TagParser) tagParserMap.get(parser.getName());
                        if (tagParser == null) {
                            Log.d(TAG, "Ignoring unknown element tag: " + parser.getName());
                        } else {
                            this.mAppsParseContainer = -102;
                            long newElementId = tagParser.parseAndAdd(parser, tableName);
                            if (newElementId >= 0) {
                                if (container == -102 && screenIds != null) {
                                    if (!screenIds.contains(Long.valueOf(screenId))) {
                                        screenIds.add(Long.valueOf(screenId));
                                    }
                                }
                                this.mRank++;
                                if (newElementId > 0) {
                                    String reservedFolder = DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_RESERVED_FOLDER);
                                    if (reservedFolder != null) {
                                        PostPositionSharedPref ppPref = PostPositionController.getInstance(this.mContext).getSharedPref(-102);
                                        if (ppPref != null) {
                                            ppPref.writeFolderId(reservedFolder, newElementId, true);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected ArrayList<ComponentName> getHiddenApps() {
        XmlPullParserException e;
        Throwable th;
        IOException e2;
        NotFoundException e3;
        ArrayList<ComponentName> hiddenApps = new ArrayList();
        FileReader fileReader = null;
        try {
            XmlPullParser parser;
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            File fileCheck = new File(this.mAppOrderPath);
            if (!fileCheck.isFile() || fileCheck.length() <= 0) {
                parser = this.mSourceRes.getXml(R.xml.default_application_order);
            } else {
                FileReader fileReader2 = new FileReader(fileCheck);
                try {
                    parser = factory.newPullParser();
                    parser.setInput(fileReader2);
                    this.mIsCSC = true;
                    fileReader = fileReader2;
                } catch (XmlPullParserException e4) {
                    e = e4;
                    fileReader = fileReader2;
                    try {
                        Log.e(TAG, "Got exception parsing appOrder.", e);
                        if (fileReader != null) {
                            try {
                                fileReader.close();
                            } catch (Exception e5) {
                                Log.e(TAG, "Got exception parsing appOrder.", e5);
                            }
                        }
                        return hiddenApps;
                    } catch (Throwable th2) {
                        th = th2;
                        if (fileReader != null) {
                            try {
                                fileReader.close();
                            } catch (Exception e52) {
                                Log.e(TAG, "Got exception parsing appOrder.", e52);
                            }
                        }
                        throw th;
                    }
                } catch (IOException e6) {
                    e2 = e6;
                    fileReader = fileReader2;
                    Log.e(TAG, "Got exception parsing appOrder.", e2);
                    if (fileReader != null) {
                        try {
                            fileReader.close();
                        } catch (Exception e522) {
                            Log.e(TAG, "Got exception parsing appOrder.", e522);
                        }
                    }
                    return hiddenApps;
                } catch (NotFoundException e7) {
                    e3 = e7;
                    fileReader = fileReader2;
                    Log.e(TAG, "Got exception parsing appOrder.", e3);
                    if (fileReader != null) {
                        try {
                            fileReader.close();
                        } catch (Exception e5222) {
                            Log.e(TAG, "Got exception parsing appOrder.", e5222);
                        }
                    }
                    return hiddenApps;
                } catch (Throwable th3) {
                    th = th3;
                    fileReader = fileReader2;
                    if (fileReader != null) {
                        fileReader.close();
                    }
                    throw th;
                }
            }
            DefaultLayoutParser.beginDocument(parser, this.mRootTag);
            int startDepth = parser.getDepth();
            while (true) {
                int type = parser.next();
                if ((type != 3 || parser.getDepth() > startDepth) && type != 1) {
                    if (type == 2 && DefaultLayoutParser.TAG_FAVORITE.equals(parser.getName()) && Boolean.parseBoolean(DefaultLayoutParser.getAttributeValue(parser, "hidden"))) {
                        hiddenApps.add(new ComponentName(DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_PACKAGE_NAME), DefaultLayoutParser.getAttributeValue(parser, DefaultLayoutParser.ATTR_CLASS_NAME)));
                    }
                }
            }
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (Exception e52222) {
                    Log.e(TAG, "Got exception parsing appOrder.", e52222);
                }
            }
        } catch (XmlPullParserException e8) {
            e = e8;
            Log.e(TAG, "Got exception parsing appOrder.", e);
            if (fileReader != null) {
                fileReader.close();
            }
            return hiddenApps;
        } catch (IOException e9) {
            e2 = e9;
            Log.e(TAG, "Got exception parsing appOrder.", e2);
            if (fileReader != null) {
                fileReader.close();
            }
            return hiddenApps;
        } catch (NotFoundException e10) {
            e3 = e10;
            Log.e(TAG, "Got exception parsing appOrder.", e3);
            if (fileReader != null) {
                fileReader.close();
            }
            return hiddenApps;
        }
        return hiddenApps;
    }
}

package com.android.launcher3.common.model;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources.NotFoundException;
import android.content.res.XmlResourceParser;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.deviceprofile.GridInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import org.xmlpull.v1.XmlPullParserException;

public class LauncherAppWidgetProviderInfo extends AppWidgetProviderInfo {
    public static final Creator CREATOR = new Creator() {
        public Object createFromParcel(Parcel parcel) {
            return new LauncherAppWidgetProviderInfo(parcel);
        }

        public LauncherAppWidgetProviderInfo[] newArray(int size) {
            return new LauncherAppWidgetProviderInfo[size];
        }
    };
    private static final String TAG = "LauncherAWProviderInfo";
    public boolean isCustomWidget = false;
    private GridInfo mHomeGridInfo;
    private int mMinSpanX;
    private int mMinSpanY;
    private int mSpanX;
    private int mSpanY;
    private SupportCellSpans mSupportCellSpans;
    private int mTempMinResizeHeight = 0;
    private int mTempMinResizeWidth = 0;

    private class SupportCellSpans {
        private static final String WIDGET_RESIZE = "com.sec.android.widgetapp.APPWIDGET_RESIZE";
        private static final String WIDGET_SUPPORT_INFO = "com.sec.android.appwidget.widgetinfo";
        private int mMaxXSpan;
        private int mMaxYSpan;
        private int mMinXSpan;
        private int mMinYSpan;
        private int mResizeMode;
        private ArrayList<int[]> mSupportSpans;

        private SupportCellSpans() {
            this.mSupportSpans = new ArrayList();
            this.mMinXSpan = 1000;
            this.mMinYSpan = 1000;
            this.mMaxXSpan = 1;
            this.mMaxYSpan = 1;
            this.mResizeMode = LauncherAppWidgetProviderInfo.this.resizeMode;
        }

        private void addSupportSpan(int spanX, int spanY) {
            this.mSupportSpans.add(new int[]{spanX, spanY});
            updateSpanAndResizeMode(spanX, spanY, false);
        }

        private void updateSpanAndResizeMode(int spanX, int spanY, boolean refresh) {
            if (refresh) {
                int minXSpan = 1000;
                int minYSpan = 1000;
                int maxXSpan = 1;
                int maxYSpan = 1;
                Iterator it = this.mSupportSpans.iterator();
                while (it.hasNext()) {
                    int[] spanXY = (int[]) it.next();
                    minXSpan = Math.min(minXSpan, spanXY[0]);
                    minYSpan = Math.min(minYSpan, spanXY[1]);
                    maxXSpan = Math.max(maxXSpan, spanXY[0]);
                    maxYSpan = Math.max(maxYSpan, spanXY[1]);
                }
                this.mMinXSpan = minXSpan;
                this.mMinYSpan = minYSpan;
                this.mMaxXSpan = maxXSpan;
                this.mMaxYSpan = maxYSpan;
            }
            this.mMinXSpan = Math.min(this.mMinXSpan, spanX);
            this.mMinYSpan = Math.min(this.mMinYSpan, spanY);
            this.mMaxXSpan = Math.max(this.mMaxXSpan, spanX);
            this.mMaxYSpan = Math.max(this.mMaxYSpan, spanY);
            if (this.mMaxXSpan != this.mMinXSpan) {
                this.mResizeMode |= 1;
            }
            if (this.mMaxYSpan != this.mMinYSpan) {
                this.mResizeMode |= 2;
            }
        }

        private void parseSupportSpans() {
            Context context = LauncherAppState.getInstance().getContext();
            AppWidgetProviderInfo info = LauncherAppWidgetProviderInfo.this;
            if (info.provider != null) {
                try {
                    Context packageContext = context.createPackageContext(info.provider.getPackageName(), 4);
                    PackageManager localPackageManager = packageContext.getPackageManager();
                    for (ResolveInfo resolveInfo : context.getPackageManager().queryBroadcastReceivers(new Intent(WIDGET_RESIZE).setComponent(info.provider), 128)) {
                        if (resolveInfo.activityInfo.name.equals(info.provider.getClassName())) {
                            XmlResourceParser parser = resolveInfo.activityInfo.loadXmlMetaData(localPackageManager, WIDGET_SUPPORT_INFO);
                            if (parser != null) {
                                int type;
                                do {
                                    try {
                                        type = parser.next();
                                        if (type == 2) {
                                            break;
                                        }
                                    } catch (XmlPullParserException e) {
                                        e.printStackTrace();
                                    } catch (IOException e2) {
                                        e2.printStackTrace();
                                    }
                                } while (type != 1);
                                int sizeId = parser.getAttributeResourceValue(null, "supportCellSizes", 0);
                                if (sizeId > 0) {
                                    String[] reult = null;
                                    try {
                                        reult = packageContext.getResources().getStringArray(sizeId);
                                    } catch (NotFoundException e3) {
                                        Log.e(LauncherAppWidgetProviderInfo.TAG, "Not found the array for supportCellSizes : " + info.provider);
                                    }
                                    if (reult != null) {
                                        int defaultSpanX = LauncherAppWidgetProviderInfo.this.getSpanX();
                                        int defaultSpanY = LauncherAppWidgetProviderInfo.this.getSpanY();
                                        addSupportSpan(defaultSpanX, defaultSpanY);
                                        int length = reult.length;
                                        int i = 0;
                                        while (i < length) {
                                            String parsed = reult[i];
                                            int x = parsed.indexOf(120);
                                            try {
                                                int spanX = Integer.parseInt(parsed.substring(0, x));
                                                int spanY = Integer.parseInt(parsed.substring(x + 1));
                                                if (spanX == defaultSpanX && spanY == defaultSpanY) {
                                                    i++;
                                                } else {
                                                    addSupportSpan(spanX, spanY);
                                                    i++;
                                                }
                                            } catch (Exception e4) {
                                                Log.e(LauncherAppWidgetProviderInfo.TAG, "parsed = " + parsed + " , x = " + x);
                                                e4.printStackTrace();
                                            }
                                        }
                                        Collections.sort(this.mSupportSpans, new Comparator<int[]>() {
                                            public int compare(int[] lhs, int[] rhs) {
                                                if (lhs[0] * lhs[1] == rhs[0] * rhs[1]) {
                                                    return lhs[0] - rhs[0];
                                                }
                                                return (lhs[0] * lhs[1]) - (rhs[0] * rhs[1]);
                                            }
                                        });
                                        return;
                                    }
                                    return;
                                }
                                return;
                            }
                            return;
                        }
                    }
                } catch (NameNotFoundException e5) {
                    e5.printStackTrace();
                }
            }
        }

        private int getNearestWidth(int w) {
            return Math.max(Math.min(w, this.mMaxXSpan), this.mMinXSpan);
        }

        private int getNearestHeight(int h) {
            return Math.max(Math.min(h, this.mMaxYSpan), this.mMinYSpan);
        }

        private int getMinXSpan() {
            return this.mMinXSpan;
        }

        private int getMinYSpan() {
            return this.mMinYSpan;
        }

        private int getResizeMode() {
            return this.mResizeMode;
        }

        private ArrayList<int[]> getSupportedSpans() {
            return this.mSupportSpans;
        }

        private int getSupportSpanCount() {
            return this.mSupportSpans.size();
        }

        private boolean isAvailableSize(int w, int h) {
            Iterator it = this.mSupportSpans.iterator();
            while (it.hasNext()) {
                int[] s = (int[]) it.next();
                if (s[0] == w && s[1] == h) {
                    return true;
                }
            }
            return false;
        }
    }

    public static LauncherAppWidgetProviderInfo fromProviderInfo(Context context, AppWidgetProviderInfo info) {
        Parcel p = Parcel.obtain();
        info.writeToParcel(p, 0);
        p.setDataPosition(0);
        LauncherAppWidgetProviderInfo lawpi = new LauncherAppWidgetProviderInfo(p);
        p.recycle();
        return lawpi;
    }

    public LauncherAppWidgetProviderInfo(Parcel in) {
        super(in);
        if (this.provider != null && info.provider.getClassName().equals("com.google.android.googlequicksearchbox.SearchWidgetProvider") && LauncherFeature.supportGSARoundingFeature()) {
            try {
                int resId = LauncherAppState.getInstance().getContext().getPackageManager().getReceiverInfo(this.provider, 128).metaData.getInt("com.google.android.gsa.searchwidget.alt_initial_layout_cqsb", -1);
                if (resId != -1) {
                    Log.d(TAG, "GSA replace initialLayout. ");
                    info.initialLayout = resId;
                }
            } catch (Exception e) {
                Log.d(TAG, "Fail to replace GSA initialLayout.");
            }
        }
        calculateSpans();
    }

    private void calculateSpans() {
        DeviceProfile dp = LauncherAppState.getInstance().getDeviceProfile();
        if ((dp != null && needToUpdateSpans(dp.homeGrid)) || (this.mTempMinResizeWidth != this.minResizeWidth && this.mTempMinResizeHeight != this.minResizeHeight)) {
            this.mHomeGridInfo = dp.homeGrid;
            Rect widgetPadding = DeviceProfile.getPaddingForWidget();
            int[] spanXY = getSpan((this.minWidth + widgetPadding.left) + widgetPadding.right, (this.minHeight + widgetPadding.top) + widgetPadding.bottom, dp, false);
            this.mSpanX = spanXY[0];
            this.mSpanY = spanXY[1];
            int[] minSpanXY = getSpan((this.minResizeWidth + widgetPadding.left) + widgetPadding.right, (this.minResizeHeight + widgetPadding.top) + widgetPadding.bottom, dp, true);
            this.mTempMinResizeWidth = this.minResizeWidth;
            this.mTempMinResizeHeight = this.minResizeHeight;
            this.mMinSpanX = Math.max(1, minSpanXY[0]);
            this.mMinSpanY = Math.max(1, minSpanXY[1]);
            if (this.mSupportCellSpans == null) {
                this.mSupportCellSpans = new SupportCellSpans();
                this.mSupportCellSpans.parseSupportSpans();
            }
            this.mSupportCellSpans.updateSpanAndResizeMode(this.mSpanX, this.mSpanY, true);
        } else if (dp == null) {
            Log.w(TAG, "ignore calculate spans because DeviceProfile is null");
        }
    }

    public String getLabel(PackageManager packageManager) {
        if (this.isCustomWidget) {
            return Utilities.trim(this.label);
        }
        return super.loadLabel(packageManager);
    }

    public Drawable getIcon(Context context, IconCache cache) {
        if (this.isCustomWidget) {
            return cache.getFullResIcon(this.provider.getPackageName(), this.icon);
        }
        return super.loadIcon(context, LauncherAppState.getInstance().getIconCache().getIconDpi());
    }

    public String toString(PackageManager pm) {
        if (this.isCustomWidget) {
            return "WidgetProviderInfo(" + this.provider + ")";
        }
        return String.format("WidgetProviderInfo provider:%s package:%s short:%s label:%s", new Object[]{this.provider.toString(), this.provider.getPackageName(), this.provider.getShortClassName(), getLabel(pm)});
    }

    public Point getMinSpans() {
        int minSpanX;
        int i = -1;
        calculateSpans();
        int mode = resizeMode();
        if ((mode & 1) != 0) {
            minSpanX = getMinSpanX();
        } else {
            minSpanX = -1;
        }
        if ((mode & 2) != 0) {
            i = getMinSpanY();
        }
        return new Point(minSpanX, i);
    }

    public int getSpanX() {
        calculateSpans();
        return this.mSpanX;
    }

    public int getSpanY() {
        calculateSpans();
        return this.mSpanY;
    }

    public int getMinSpanX() {
        calculateSpans();
        if (this.mSupportCellSpans.getSupportSpanCount() > 0) {
            return this.mSupportCellSpans.getMinXSpan();
        }
        return this.mMinSpanX;
    }

    public int getMinSpanY() {
        calculateSpans();
        if (this.mSupportCellSpans.getSupportSpanCount() > 0) {
            return this.mSupportCellSpans.getMinYSpan();
        }
        return this.mMinSpanY;
    }

    public int resizeMode() {
        if (this.mSupportCellSpans.getSupportSpanCount() > 0) {
            return this.mSupportCellSpans.getResizeMode();
        }
        return this.resizeMode;
    }

    public int getNearestWidth(int w) {
        return this.mSupportCellSpans.getNearestWidth(w);
    }

    public int getNearestHeight(int h) {
        return this.mSupportCellSpans.getNearestHeight(h);
    }

    public ArrayList<int[]> getSupportedSpans() {
        return this.mSupportCellSpans.getSupportedSpans();
    }

    public boolean isAvailableSize(int w, int h) {
        return this.mSupportCellSpans.getSupportSpanCount() <= 0 || this.mSupportCellSpans.isAvailableSize(w, h);
    }

    public boolean needToUpdateSpans(GridInfo newGridInfo) {
        if (this.mHomeGridInfo != null && this.mHomeGridInfo.getCellCountX() == newGridInfo.getCellCountX() && this.mHomeGridInfo.getCellCountX() == newGridInfo.getCellCountY() && this.mHomeGridInfo.getCellWidth() == newGridInfo.getCellWidth() && this.mHomeGridInfo.getCellHeight() == newGridInfo.getCellHeight()) {
            return false;
        }
        return true;
    }

    private int[] getSpan(int cellWidth, int cellHeight, DeviceProfile dp, boolean resize) {
        int smallsize = Math.min(dp.gedHomeCellWidth, dp.gedHomeCellHeight);
        int spanX = getSpanCount(cellWidth, smallsize, 0);
        int spanY = getSpanCount(cellHeight, smallsize, 0);
        boolean isEasyMode = LauncherAppState.getInstance().isEasyModeEnabled();
        int baseSpanX = (isEasyMode || !resize) ? dp.homeGrid.getCellCountX() : dp.gedHomeCellCountX;
        int baseSpanY = (isEasyMode || !resize) ? dp.homeGrid.getCellCountY() : dp.gedHomeCellCountY;
        return new int[]{Math.min(baseSpanX, spanX), Math.min(baseSpanY, spanY)};
    }

    private int getSpanCount(int targetSize, int cellSize, int cellGap) {
        int denominator = cellSize + cellGap;
        return Math.max(1, (((targetSize + cellGap) + denominator) - 1) / denominator);
    }
}

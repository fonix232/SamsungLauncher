package com.android.launcher3.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.provider.Settings.System;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.model.DefaultLayoutParser;
import com.android.launcher3.home.LauncherAppWidgetInfo;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ScreenGridUtilities {
    private static final String APPS_GRID_CELLX = "Apps.CellX";
    private static final String APPS_GRID_CELLY = "Apps.CellY";
    private static final String APPS_GRID_SUPPORTED_SET = "Apps.GridSet";
    public static final int BOTTOM_LEFT = 1;
    public static final int BOTTOM_RIGHT = 0;
    public static final int DENSITY_LARGE = 0;
    public static final int DENSITY_NORMAL = 1;
    public static final int DENSITY_SMALL = 2;
    private static final String GRID_CELLX = "Workspace.CellX";
    private static final int GRID_CELLX_DEFAULT = 4;
    private static final String GRID_CELLX_HOMEONLY = "Workspace.HomeOnly.CellX";
    private static final String GRID_CELLY = "Workspace.CellY";
    private static final int GRID_CELLY_DEFAULT = 5;
    private static final String GRID_CELLY_HOMEONLY = "Workspace.HomeOnly.CellY";
    private static final String GRID_CHANGED = "Grid.Changed";
    public static final int INVALID_CELL_INFO = -1;
    private static final int SW_LARGE = 320;
    private static final int SW_NORMAL = 360;
    private static final int SW_SMALL = 411;
    private static final String TAG = "ScreenGridUtilities";
    public static final int TOP_LAND_RIGHT = 4;
    public static final int TOP_LEFT = 3;
    public static final int TOP_RIGHT = 2;
    private static Comparator<ItemInfo> mItemComparator_BL = new Comparator<ItemInfo>() {
        public int compare(ItemInfo item1, ItemInfo item2) {
            if (item1.cellX == item2.cellX) {
                return item2.cellY - item1.cellY;
            }
            return item2.cellX - item1.cellX;
        }
    };
    private static Comparator<ItemInfo> mItemComparator_BR = new Comparator<ItemInfo>() {
        public int compare(ItemInfo item1, ItemInfo item2) {
            if (item1.cellX == item2.cellX) {
                return item2.cellY - item1.cellY;
            }
            return item1.cellX - item2.cellX;
        }
    };
    private static Comparator<ItemInfo> mItemComparator_TL = new Comparator<ItemInfo>() {
        public int compare(ItemInfo item1, ItemInfo item2) {
            if (item1.cellX == item2.cellX) {
                return item1.cellY - item2.cellY;
            }
            return item2.cellX - item1.cellX;
        }
    };
    private static Comparator<ItemInfo> mItemComparator_TLR = new Comparator<ItemInfo>() {
        public int compare(ItemInfo item1, ItemInfo item2) {
            if (item1.cellY == item2.cellY) {
                return item1.cellX - item2.cellX;
            }
            return item1.cellY - item2.cellY;
        }
    };
    private static Comparator<ItemInfo> mItemComparator_TR = new Comparator<ItemInfo>() {
        public int compare(ItemInfo item1, ItemInfo item2) {
            if (item1.cellX == item2.cellX) {
                return item1.cellY - item2.cellY;
            }
            return item1.cellX - item2.cellX;
        }
    };
    private static Comparator<Pair<ItemInfo, View>> mPairItemComparator_BL = new Comparator<Pair<ItemInfo, View>>() {
        public int compare(Pair<ItemInfo, View> item1, Pair<ItemInfo, View> item2) {
            if (((ItemInfo) item1.first).cellX == ((ItemInfo) item2.first).cellX) {
                return ((ItemInfo) item2.first).cellY - ((ItemInfo) item1.first).cellY;
            }
            return ((ItemInfo) item2.first).cellX - ((ItemInfo) item1.first).cellX;
        }
    };
    private static Comparator<Pair<ItemInfo, View>> mPairItemComparator_BR = new Comparator<Pair<ItemInfo, View>>() {
        public int compare(Pair<ItemInfo, View> item1, Pair<ItemInfo, View> item2) {
            if (((ItemInfo) item1.first).cellX == ((ItemInfo) item2.first).cellX) {
                return ((ItemInfo) item2.first).cellY - ((ItemInfo) item1.first).cellY;
            }
            return ((ItemInfo) item1.first).cellX - ((ItemInfo) item2.first).cellX;
        }
    };
    private static Comparator<Pair<ItemInfo, View>> mPairItemComparator_TL = new Comparator<Pair<ItemInfo, View>>() {
        public int compare(Pair<ItemInfo, View> item1, Pair<ItemInfo, View> item2) {
            if (((ItemInfo) item1.first).cellX == ((ItemInfo) item2.first).cellX) {
                return ((ItemInfo) item1.first).cellY - ((ItemInfo) item2.first).cellY;
            }
            return ((ItemInfo) item2.first).cellX - ((ItemInfo) item1.first).cellX;
        }
    };
    private static Comparator<Pair<ItemInfo, View>> mPairItemComparator_TR = new Comparator<Pair<ItemInfo, View>>() {
        public int compare(Pair<ItemInfo, View> item1, Pair<ItemInfo, View> item2) {
            if (((ItemInfo) item1.first).cellX == ((ItemInfo) item2.first).cellX) {
                return ((ItemInfo) item1.first).cellY - ((ItemInfo) item2.first).cellY;
            }
            return ((ItemInfo) item1.first).cellX - ((ItemInfo) item2.first).cellX;
        }
    };

    public static int getOutSidePosition(List<ItemInfo> items, int columnSize, int rowSize, int diffX, int diffY) {
        ArrayList<Integer> outSideList = new ArrayList();
        int bottomRight = 0;
        int bottomLeft = 0;
        int topRight = 0;
        int topLeft = 0;
        for (ItemInfo item : items) {
            int x = item.cellX;
            int y = item.cellY;
            if (item instanceof LauncherAppWidgetInfo) {
                int spanX = item.spanX;
                int spanY = item.spanY;
                if (spanX > columnSize) {
                    spanX = columnSize;
                }
                if (spanY > rowSize) {
                    spanY = rowSize;
                }
                if (x < columnSize && y < rowSize && x + spanX <= columnSize && y + spanY <= rowSize) {
                    bottomRight += spanX * spanY;
                }
                if (x > diffX && y < rowSize && x + spanX > diffX && y + spanY <= rowSize) {
                    bottomLeft += spanX * spanY;
                }
                if (x < columnSize && y > diffY && x + spanX <= columnSize && y + spanY > diffY) {
                    topRight += spanX * spanY;
                }
                if (x > diffX && y > diffY && x + spanX > diffX && y + spanY > diffY) {
                    topLeft += spanX * spanY;
                }
            } else {
                if (x < columnSize && y < rowSize) {
                    bottomRight++;
                }
                if (x > diffX && y < rowSize) {
                    bottomLeft++;
                }
                if (x < columnSize && y > diffY) {
                    topRight++;
                }
                if (x > diffX && y > diffY) {
                    topLeft++;
                }
            }
        }
        outSideList.add(Integer.valueOf(bottomRight));
        outSideList.add(Integer.valueOf(bottomLeft));
        outSideList.add(Integer.valueOf(topRight));
        outSideList.add(Integer.valueOf(topLeft));
        Log.d(TAG, "getOutSidePosition : bottomRight = " + bottomRight + ", bottomLeft = " + bottomLeft + ", topRight = " + topRight + ", topLeft = " + topLeft);
        return outSideList.indexOf(Collections.max(outSideList));
    }

    public static List<ItemInfo> getOutSideItems(List<ItemInfo> items, int outSidePosition) {
        if (items != null && items.size() > 0) {
            switch (outSidePosition) {
                case 0:
                    Collections.sort(items, mItemComparator_BR);
                    break;
                case 1:
                    Collections.sort(items, mItemComparator_BL);
                    break;
                case 2:
                    Collections.sort(items, mItemComparator_TR);
                    break;
                case 3:
                    Collections.sort(items, mItemComparator_TL);
                    break;
                case 4:
                    Collections.sort(items, mItemComparator_TLR);
                    break;
            }
        }
        return items;
    }

    public static List<Pair<ItemInfo, View>> getPairOutSideItems(List<Pair<ItemInfo, View>> items, int outSidePosition) {
        if (items != null && items.size() > 0) {
            if (outSidePosition == 1) {
                Collections.sort(items, mPairItemComparator_BL);
            } else if (outSidePosition == 0) {
                Collections.sort(items, mPairItemComparator_BR);
            } else if (outSidePosition == 3) {
                Collections.sort(items, mPairItemComparator_TL);
            } else if (outSidePosition == 2) {
                Collections.sort(items, mPairItemComparator_TR);
            }
        }
        return items;
    }

    public static void storeGridLayoutPreference(Context context, int cellX, int cellY, boolean isHomeOnly) {
        SharedPreferences prefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        if (prefs != null) {
            Editor editor = prefs.edit();
            if (editor != null) {
                if (isHomeOnly) {
                    editor.putInt(GRID_CELLX_HOMEONLY, cellX);
                    editor.putInt(GRID_CELLY_HOMEONLY, cellY);
                } else {
                    editor.putInt(GRID_CELLX, cellX);
                    editor.putInt(GRID_CELLY, cellY);
                }
                editor.apply();
            }
        }
    }

    public static void storeCurrentScreenGridSetting(Context context, int cellX, int cellY) {
        try {
            System.putString(context.getContentResolver(), "launcher_current_screen_grid", Integer.toString(cellX) + DefaultLayoutParser.ATTR_X + Integer.toString(cellY));
        } catch (Exception e) {
            Log.e(TAG, "storeGridLayoutPreference Settings.System.putString error e=" + e.toString());
        }
    }

    public static void loadCurrentGridSize(Context context, int[] xy, boolean isHomeOnly) {
        SharedPreferences prefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        if (isHomeOnly) {
            xy[0] = prefs.getInt(GRID_CELLX_HOMEONLY, -1);
            xy[1] = prefs.getInt(GRID_CELLY_HOMEONLY, -1);
            return;
        }
        xy[0] = prefs.getInt(GRID_CELLX, -1);
        xy[1] = prefs.getInt(GRID_CELLY, -1);
    }

    public static void storeChangeGridValue(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        if (prefs != null) {
            Editor editor = prefs.edit();
            if (editor != null) {
                editor.putBoolean(GRID_CHANGED, true);
                editor.apply();
            }
        }
    }

    public static boolean loadChageGridValue(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        if (prefs == null || !prefs.getBoolean(GRID_CHANGED, false)) {
            return false;
        }
        return true;
    }

    public static boolean findNearestGridSize(Context context, int[] restoreGridSize, int countX, int countY) {
        String[] support = context.getResources().getStringArray(R.array.support_grid_size);
        if (support == null) {
            return false;
        }
        int newCountX = 0;
        int newCountY = 0;
        for (String grid : support) {
            String[] gridSplit = grid.split(DefaultLayoutParser.ATTR_X);
            if (gridSplit != null && gridSplit.length == 2) {
                newCountX = Integer.parseInt(gridSplit[0]);
                newCountY = Integer.parseInt(gridSplit[1]);
                if (countX <= newCountX && countY <= newCountY) {
                    break;
                }
            }
        }
        if (newCountX <= 0 || newCountY <= 0) {
            return false;
        }
        restoreGridSize[0] = newCountX;
        restoreGridSize[1] = newCountY;
        return true;
    }

    private static int getGridValueForDensity(Context context) {
        if (context != null) {
            switch (context.getResources().getConfiguration().smallestScreenWidthDp) {
                case SW_LARGE /*320*/:
                case SW_NORMAL /*360*/:
                    return 0;
                case SW_SMALL /*411*/:
                    return 2;
            }
        }
        return 1;
    }

    public static void changeGridForDpi(Context context, boolean isHomeOnly) {
        DeviceProfile dp = LauncherAppState.getInstance().getDeviceProfile();
        int gridX = 4;
        int gridY = 5;
        String[] gridSplit = context.getResources().getStringArray(R.array.support_grid_size)[getGridValueForDensity(context)].split(DefaultLayoutParser.ATTR_X);
        if (gridSplit.length == 2) {
            gridX = Integer.parseInt(gridSplit[0]);
            gridY = Integer.parseInt(gridSplit[1]);
        }
        if ((!LauncherFeature.supportEasyModeChange() || !LauncherAppState.getInstance().isEasyModeEnabled()) && !loadChageGridValue(context)) {
            if (dp.homeGrid.getCellCountX() != gridX || dp.homeGrid.getCellCountY() != gridY) {
                dp.setCurrentGrid(gridX, gridY);
                storeGridLayoutPreference(context, gridX, gridY, isHomeOnly);
            }
        }
    }

    public static void storeAppsGridLayoutPreference(Context context, int cellX, int cellY) {
        if (!LauncherFeature.isTablet()) {
            SharedPreferences prefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
            if (prefs != null) {
                Editor editor = prefs.edit();
                if (editor != null) {
                    editor.putInt(APPS_GRID_CELLX, cellX);
                    editor.putInt(APPS_GRID_CELLY, cellY);
                    editor.apply();
                }
            }
        }
    }

    public static void loadCurrentAppsGridSize(Context context, int[] gridXY) {
        if (LauncherFeature.isTablet()) {
            gridXY[0] = context.getResources().getInteger(R.integer.apps_default_cellCountX);
            gridXY[1] = context.getResources().getInteger(R.integer.apps_default_cellCountY);
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        gridXY[0] = prefs.getInt(APPS_GRID_CELLX, -1);
        gridXY[1] = prefs.getInt(APPS_GRID_CELLY, -1);
    }

    public static void storeAppsSupportedGridSet(Context context, String gridSet) {
        if (!LauncherFeature.isTablet()) {
            if (gridSet == null || gridSet.isEmpty()) {
                throw new IllegalArgumentException("invalid gridSet" + gridSet);
            }
            SharedPreferences prefs = context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
            if (prefs != null) {
                Editor editor = prefs.edit();
                if (editor != null) {
                    editor.putString(APPS_GRID_SUPPORTED_SET, gridSet);
                    editor.apply();
                }
            }
        }
    }

    public static String loadAppsSupportedGridSet(Context context) {
        return context.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).getString(APPS_GRID_SUPPORTED_SET, "");
    }
}

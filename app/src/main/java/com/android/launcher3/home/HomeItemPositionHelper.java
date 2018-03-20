package com.android.launcher3.home;

import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.model.LauncherSettings.Favorites;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

class HomeItemPositionHelper {
    private static final String TAG = "HomeItemPositionHelper";
    private ContentResolver mContentResolver;
    private ArrayList<CellPositionInfo> mPreservedPosList = new ArrayList();

    private static class CellPositionInfo {
        long screenId;
        int spanX = 1;
        int spanY = 1;
        int x;
        int y;

        CellPositionInfo(long screenId, int x, int y, int spanX, int spanY) {
            this.screenId = screenId;
            this.x = x;
            this.y = y;
            this.spanX = spanX;
            this.spanY = spanY;
        }
    }

    HomeItemPositionHelper(ContentResolver contentResolver) {
        this.mContentResolver = contentResolver;
    }

    boolean findNextAvailableIconSpaceInScreen(ArrayList<ItemInfo> occupiedPos, long screenId, int[] xy, int spanX, int spanY, boolean lastPosition) {
        Iterator it;
        int x;
        int y;
        DeviceProfile dp = LauncherAppState.getInstance().getDeviceProfile();
        int xCount = dp.homeGrid.getCellCountX();
        int yCount = dp.homeGrid.getCellCountY();
        boolean[][] occupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{xCount, yCount});
        if (occupiedPos != null) {
            it = occupiedPos.iterator();
            while (it.hasNext()) {
                ItemInfo r = (ItemInfo) it.next();
                int right = r.cellX + r.spanX;
                int bottom = r.cellY + r.spanY;
                x = r.cellX;
                while (x >= 0 && x < right && x < xCount) {
                    y = r.cellY;
                    while (y >= 0 && y < bottom && y < yCount) {
                        occupied[x][y] = true;
                        y++;
                    }
                    x++;
                }
            }
        }
        if (this.mPreservedPosList.size() > 0) {
            int[] gridSize = new int[]{dp.homeGrid.getCellCountX(), dp.homeGrid.getCellCountY()};
            it = this.mPreservedPosList.iterator();
            while (it.hasNext()) {
                CellPositionInfo p = (CellPositionInfo) it.next();
                if (screenId == p.screenId) {
                    x = p.x;
                    while (x < p.x + p.spanX && x < gridSize[0]) {
                        y = p.y;
                        while (y < p.y + p.spanY && y < gridSize[1]) {
                            occupied[x][y] = true;
                            y++;
                        }
                        x++;
                    }
                }
            }
        }
        return findVacantCell(xy, spanX, spanY, xCount, yCount, occupied, lastPosition);
    }

    boolean findNearEmptyCell(int[] xy, long screenId, int fromX, int fromY) {
        return findNearEmptyCell(xy, screenId, 1, 1, fromX, fromY);
    }

    boolean findEmptyCell(int[] xy, long screenId, int itemSpanX, int itemSpanY) {
        return findEmptyCell(xy, screenId, itemSpanX, itemSpanY, false);
    }

    boolean findEmptyCell(int[] xy, long screenId, int itemSpanX, int itemSpanY, boolean checkOccupy) {
        DeviceProfile dp = LauncherAppState.getInstance().getDeviceProfile();
        int xCount = dp.homeGrid.getCellCountX();
        int yCount = dp.homeGrid.getCellCountY();
        boolean[][] occupied = getOccupiedTable(screenId, xCount, yCount);
        if (checkOccupy) {
            if (itemSpanX > 1 || itemSpanY > 1) {
                for (int x = xy[0]; x < xy[0] + itemSpanX; x++) {
                    for (int y = xy[1]; y < xy[1] + itemSpanY; y++) {
                        if (occupied[x][y]) {
                            return false;
                        }
                    }
                }
            } else if (occupied[xy[0]][xy[1]]) {
                return false;
            } else {
                return true;
            }
        }
        return findVacantCell(xy, itemSpanX, itemSpanY, xCount, yCount, occupied);
    }

    boolean findEmptyCellWithOccupied(int[] xy, int itemSpanX, int itemSpanY, int xCount, int yCount, boolean[][] occupied) {
        return findVacantCell(xy, itemSpanX, itemSpanY, xCount, yCount, occupied);
    }

    void clearPreservedPosition() {
        this.mPreservedPosList.clear();
    }

    void addToPreservedPosition(long screenId, int x, int y) {
        addToPreservedPosition(screenId, x, y, 1, 1);
    }

    void addToPreservedPosition(long screenId, int x, int y, int spanX, int spanY) {
        this.mPreservedPosList.add(new CellPositionInfo(screenId, x, y, spanX, spanY));
    }

    private boolean[][] getOccupiedTable(long screenId, int xCount, int yCount) {
        int[] gridSize = new int[]{xCount, yCount};
        boolean[][] occupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{gridSize[0], gridSize[1]});
        Iterator it = getItemsInLocalCoordinates(-100, screenId).iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            int cellX = item.cellX;
            int cellY = item.cellY;
            int spanX = item.spanX;
            int spanY = item.spanY;
            int x = cellX;
            while (x < cellX + spanX && x < gridSize[0]) {
                int y = cellY;
                while (y < cellY + spanY && y < gridSize[1]) {
                    occupied[x][y] = true;
                    y++;
                }
                x++;
            }
        }
        if (this.mPreservedPosList.size() > 0) {
            it = this.mPreservedPosList.iterator();
            while (it.hasNext()) {
                CellPositionInfo p = (CellPositionInfo) it.next();
                if (screenId == p.screenId) {
                    x = p.x;
                    while (x < p.x + p.spanX && x < gridSize[0]) {
                        y = p.y;
                        while (y < p.y + p.spanY && y < gridSize[1]) {
                            occupied[x][y] = true;
                            y++;
                        }
                        x++;
                    }
                }
            }
        }
        return occupied;
    }

    private boolean findNearEmptyCell(int[] xy, long screenId, int itemSpanX, int itemSpanY, int fromX, int fromY) {
        DeviceProfile dp = LauncherAppState.getInstance().getDeviceProfile();
        int xCount = dp.homeGrid.getCellCountX();
        int yCount = dp.homeGrid.getCellCountY();
        return findNearVacantCell(xy, fromX, fromY, itemSpanX, itemSpanY, xCount, yCount, getOccupiedTable(screenId, xCount, yCount));
    }

    private ArrayList<ItemInfo> getItemsInLocalCoordinates(long container, long screenId) {
        ArrayList<ItemInfo> items = new ArrayList();
        Cursor c = null;
        try {
            c = this.mContentResolver.query(Favorites.CONTENT_URI, new String[]{"cellX", "cellY", "spanX", "spanY", Favorites.FESTIVAL}, "container=? and screen=? and hidden=0", new String[]{String.valueOf(container), String.valueOf(screenId)}, null);
            if (c != null) {
                int cellXIndex = c.getColumnIndexOrThrow("cellX");
                int cellYIndex = c.getColumnIndexOrThrow("cellY");
                int spanXIndex = c.getColumnIndexOrThrow("spanX");
                int spanYIndex = c.getColumnIndexOrThrow("spanY");
                while (c.moveToNext()) {
                    ItemInfo item = new ItemInfo();
                    item.container = container;
                    item.screenId = screenId;
                    item.cellX = c.getInt(cellXIndex);
                    item.cellY = c.getInt(cellYIndex);
                    item.spanX = c.getInt(spanXIndex);
                    item.spanY = c.getInt(spanYIndex);
                    if (item.cellX == -1 || item.cellY == -1) {
                        Log.d(TAG, "Need handling an occupied item which has wrong coordinates cellX : " + item.cellX + " cellY : " + item.cellY);
                    } else {
                        items.add(item);
                    }
                }
            }
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            items.clear();
            Log.e(TAG, "getItemsInLocalCoordinates : " + e.getMessage());
            return items;
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return items;
    }

    boolean findVacantCell(int[] vacant, int spanX, int spanY, int xCount, int yCount, boolean[][] occupied, boolean lastPosition) {
        vacant[0] = 0;
        vacant[1] = 0;
        int offsetY = 0;
        int y;
        int x;
        if (lastPosition) {
            for (y = yCount - 1; y >= 0; y--) {
                x = xCount - 1;
                while (x >= 0) {
                    if (occupied[x][y]) {
                        int i;
                        if (spanX > 1) {
                            vacant[0] = x + spanX >= xCount ? 0 : x + 1;
                            if (x + spanX >= xCount) {
                                i = y + 1;
                            } else {
                                i = y;
                            }
                            vacant[1] = i;
                        } else {
                            vacant[0] = x + 1 == xCount ? 0 : x + 1;
                            if (x + 1 == xCount) {
                                i = y + 1;
                            } else {
                                i = y;
                            }
                            vacant[1] = i;
                        }
                        if (spanY > 1) {
                            offsetY = spanY - 1;
                        }
                    } else {
                        x--;
                    }
                }
            }
            if (spanY > 1) {
                offsetY = spanY - 1;
            }
        } else {
            for (y = 0; y + spanY <= yCount; y++) {
                for (x = 0; x + spanX <= xCount; x++) {
                    boolean available = !occupied[x][y];
                    for (int i2 = x; i2 < x + spanX; i2++) {
                        for (int j = y; j < y + spanY; j++) {
                            available = available && !occupied[i2][j];
                            if (!available) {
                                break;
                            }
                        }
                    }
                    if (available) {
                        vacant[0] = x;
                        vacant[1] = y;
                        return true;
                    }
                }
            }
        }
        if (!lastPosition || vacant[1] + offsetY >= yCount) {
            return false;
        }
        return true;
    }

    private synchronized boolean findVacantCell(int[] vacant, int spanX, int spanY, int xCount, int yCount, boolean[][] occupied) {
        boolean z;
        if (occupied == null) {
            z = false;
        } else if (occupied.length != xCount || (occupied.length > 0 && occupied[0].length != yCount)) {
            Log.e(TAG, "findVacantCell size isn't matched with array. " + occupied.length + "/" + xCount + ", " + occupied[0].length + "/" + yCount);
            z = false;
        } else {
            loop0:
            for (int y = 0; y < yCount; y++) {
                for (int x = 0; x < xCount; x++) {
                    boolean available = !occupied[x][y];
                    int vacantSize = 0;
                    int i = x;
                    while (i < x + spanX && i < xCount) {
                        int j = y;
                        while (j < y + spanY && j < yCount) {
                            available = available && !occupied[i][j];
                            vacantSize++;
                            if (!available) {
                                vacantSize = 0;
                                break;
                            }
                            j++;
                        }
                        i++;
                    }
                    if (vacantSize == spanX * spanY) {
                        vacant[0] = x;
                        vacant[1] = y;
                        z = true;
                        break loop0;
                    }
                }
            }
            z = false;
        }
        return z;
    }

    synchronized boolean findNearVacantCell(int[] vacant, int fromX, int fromY, int spanX, int spanY, int xCount, int yCount, boolean[][] occupied) {
        boolean z;
        if (fromX < 0 || fromX >= xCount || fromY < 0 || fromY >= yCount) {
            z = false;
        } else {
            if (occupied != null) {
                if (occupied.length == xCount && (occupied.length <= 0 || occupied[0].length == yCount)) {
                    int[][] visit = (int[][]) Array.newInstance(Integer.TYPE, new int[]{xCount, yCount});
                    for (int i = 0; i < xCount; i++) {
                        for (int j = 0; j < yCount; j++) {
                            visit[i][j] = 0;
                        }
                    }
                    visit[fromX][fromY] = 1;
                    Queue<Integer> q = new LinkedList();
                    q.offer(Integer.valueOf(fromX));
                    q.offer(Integer.valueOf(fromY));
                    while (!q.isEmpty()) {
                        int front_x = ((Integer) q.poll()).intValue();
                        int front_y = ((Integer) q.poll()).intValue();
                        if (!checkNearVacantCell(q, visit, vacant, front_x - 1, front_y, spanX, spanY, xCount, yCount, occupied)) {
                            if (!checkNearVacantCell(q, visit, vacant, front_x + 1, front_y, spanX, spanY, xCount, yCount, occupied)) {
                                if (!checkNearVacantCell(q, visit, vacant, front_x, front_y - 1, spanX, spanY, xCount, yCount, occupied)) {
                                    if (checkNearVacantCell(q, visit, vacant, front_x, front_y + 1, spanX, spanY, xCount, yCount, occupied)) {
                                        z = true;
                                        break;
                                    }
                                }
                                z = true;
                                break;
                            }
                            z = true;
                            break;
                        }
                        z = true;
                        break;
                    }
                    z = false;
                }
            }
            z = false;
        }
        return z;
    }

    private boolean checkNearVacantCell(Queue<Integer> q, int[][] visit, int[] vacant, int x, int y, int spanX, int spanY, int xCount, int yCount, boolean[][] occupied) {
        boolean needCheckSpan = spanX > 1 || spanY > 1;
        if (in(x, y, xCount, yCount) && visit[x][y] == 0) {
            boolean available = !occupied[x][y];
            if (available && needCheckSpan) {
                if (x + spanX > xCount || y + spanY > yCount) {
                    available = false;
                } else {
                    int i = x;
                    loop0:
                    while (i < x + spanX && i < xCount) {
                        int j = y;
                        while (j < y + spanY && j < yCount) {
                            available = available && !occupied[i][j];
                            if (!available) {
                                break loop0;
                            }
                            j++;
                        }
                        i++;
                    }
                }
            }
            if (available) {
                vacant[0] = x;
                vacant[1] = y;
                return true;
            }
            q.add(Integer.valueOf(x));
            q.add(Integer.valueOf(y));
            visit[x][y] = 1;
        }
        return false;
    }

    private boolean in(int x, int y, int xCount, int yCount) {
        return x >= 0 && x < xCount && y >= 0 && y < yCount;
    }
}

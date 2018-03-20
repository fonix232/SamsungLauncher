package com.android.launcher3.home;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.base.view.CellLayout.LayoutParams;
import com.android.launcher3.common.base.view.CellLayoutChildren;
import com.android.launcher3.folder.folderlock.FolderLock;
import com.sec.android.app.launcher.R;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Locale;

public class HomeFocusLogic {
    public static final int CURRENT_PAGE_FIRST_ITEM = -6;
    public static final int CURRENT_PAGE_LAST_ITEM = -7;
    private static final boolean DEBUG = false;
    public static final int EMPTY = -1;
    public static final int NEXT_PAGE_FIRST_ITEM = -8;
    public static final int NEXT_PAGE_LEFT_COLUMN = -9;
    public static final int NEXT_PAGE_RIGHT_COLUMN = -10;
    public static final int NOOP = -1;
    public static final int PIVOT = 100;
    public static final int PREVIOUS_PAGE_FIRST_ITEM = -3;
    public static final int PREVIOUS_PAGE_LAST_ITEM = -4;
    public static final int PREVIOUS_PAGE_LEFT_COLUMN = -5;
    public static final int PREVIOUS_PAGE_RIGHT_COLUMN = -2;
    public static final int PREVIOUS_ZERO_PAGE = -11;
    private static final String TAG = "HomeFocusLogic";

    public static boolean shouldConsume(int keyCode) {
        return keyCode == 21 || keyCode == 22 || keyCode == 19 || keyCode == 20 || keyCode == FolderLock.REQUEST_CODE_FOLDER_LOCK || keyCode == FolderLock.REQUEST_CODE_FOLDER_UNLOCK || keyCode == 92 || keyCode == 93 || keyCode == 67 || keyCode == 112;
    }

    public static int handleKeyEvent(int keyCode, int cntX, int cntY, int[][] map, int iconIdx, int pageIndex, int pageCount, CellLayoutChildren parent) {
        boolean isHotseat = parent.getParent() != null && (parent.getParent() instanceof HotseatCellLayout);
        int newIndex;
        switch (keyCode) {
            case 19:
                return handleDpadVertical(parent, iconIdx, cntX, cntY, map, -1);
            case 20:
                return handleDpadVertical(parent, iconIdx, cntX, cntY, map, 1);
            case 21:
                newIndex = handleDpadHorizontal(parent, iconIdx, cntX, cntY, map, -1);
                if (Utilities.sIsRtl || newIndex != -1) {
                    if (!Utilities.sIsRtl || newIndex != -1 || pageIndex >= pageCount - 1) {
                        return newIndex;
                    }
                    if (isHotseat) {
                        return -8;
                    }
                    return -9;
                } else if (pageIndex > 0) {
                    if (isHotseat) {
                        return -4;
                    }
                    return -2;
                } else if (pageIndex == 0) {
                    return -11;
                } else {
                    return newIndex;
                }
            case 22:
                newIndex = handleDpadHorizontal(parent, iconIdx, cntX, cntY, map, 1);
                if (Utilities.sIsRtl || newIndex != -1 || pageIndex >= pageCount - 1) {
                    if (!Utilities.sIsRtl || newIndex != -1) {
                        return newIndex;
                    }
                    if (pageIndex > 0) {
                        if (isHotseat) {
                            return -4;
                        }
                        return -2;
                    } else if (pageIndex == 0) {
                        return -11;
                    } else {
                        return newIndex;
                    }
                } else if (isHotseat) {
                    return -8;
                } else {
                    return -9;
                }
            case 92:
                return handlePageUp(pageIndex);
            case 93:
                return handlePageDown(pageIndex, pageCount);
            case FolderLock.REQUEST_CODE_FOLDER_LOCK /*122*/:
                return handleMoveHome();
            case FolderLock.REQUEST_CODE_FOLDER_UNLOCK /*123*/:
                return handleMoveEnd();
            default:
                return -1;
        }
    }

    private static int[][] createFullMatrix(int m, int n) {
        int[][] matrix = (int[][]) Array.newInstance(Integer.TYPE, new int[]{m, n});
        for (int i = 0; i < m; i++) {
            Arrays.fill(matrix[i], -1);
        }
        return matrix;
    }

    public static int[][] createSparseMatrix(CellLayout layout) {
        CellLayoutChildren parent = layout.getCellLayoutChildren();
        int m = layout.getCountX();
        int[][] matrix = createFullMatrix(m, layout.getCountY());
        for (int i = 0; i < parent.getChildCount(); i++) {
            int cx = ((LayoutParams) parent.getChildAt(i).getLayoutParams()).cellX;
            int cy = ((LayoutParams) parent.getChildAt(i).getLayoutParams()).cellY;
            if (Utilities.sIsRtl) {
                cx = (m - cx) - 1;
            }
            matrix[cx][cy] = i;
        }
        return matrix;
    }

    public static int[][] createSparseMatrix(CellLayout iconLayout, CellLayout hotseatLayout, boolean isHorizontal) {
        int m;
        int n;
        int i;
        ViewGroup iconParent = iconLayout.getCellLayoutChildren();
        ViewGroup hotseatParent = hotseatLayout.getCellLayoutChildren();
        if (isHorizontal) {
            if (hotseatLayout.getCountX() > iconLayout.getCountX()) {
                m = hotseatLayout.getCountX();
            } else {
                m = iconLayout.getCountX();
            }
            n = iconLayout.getCountY() + hotseatLayout.getCountY();
        } else {
            if (hotseatLayout.getCountY() > iconLayout.getCountY()) {
                n = hotseatLayout.getCountY();
            } else {
                n = iconLayout.getCountY();
            }
            m = iconLayout.getCountX() + hotseatLayout.getCountX();
        }
        int[][] matrix = createFullMatrix(m, n);
        for (i = 0; i < iconParent.getChildCount(); i++) {
            int cx = ((LayoutParams) iconParent.getChildAt(i).getLayoutParams()).cellX;
            matrix[cx][((LayoutParams) iconParent.getChildAt(i).getLayoutParams()).cellY] = i;
        }
        for (i = hotseatParent.getChildCount() - 1; i >= 0; i--) {
            if (isHorizontal) {
                matrix[((LayoutParams) hotseatParent.getChildAt(i).getLayoutParams()).cellX + 0][iconLayout.getCountY()] = iconParent.getChildCount() + i;
            } else {
                matrix[iconLayout.getCountX()][((LayoutParams) hotseatParent.getChildAt(i).getLayoutParams()).cellY + 0] = iconParent.getChildCount() + i;
            }
        }
        return matrix;
    }

    public static int[][] createSparseMatrix(CellLayout iconLayout, int pivotX, int pivotY) {
        ViewGroup iconParent = iconLayout.getCellLayoutChildren();
        int count_x = iconLayout.getCountX() + 1;
        int count_y = iconLayout.getCountY();
        int[][] matrix = createFullMatrix(count_x, count_y);
        for (int i = 0; i < iconParent.getChildCount(); i++) {
            int cx = ((LayoutParams) iconParent.getChildAt(i).getLayoutParams()).cellX;
            int cy = ((LayoutParams) iconParent.getChildAt(i).getLayoutParams()).cellY;
            if (pivotX < 0) {
                matrix[cx - pivotX][cy] = i;
            } else {
                matrix[cx][cy] = i;
            }
        }
        int x = Math.max(0, Math.min(pivotX, count_x - 1));
        matrix[x][Math.max(0, Math.min(pivotY, count_y - 1))] = 100;
        if (pivotX >= count_x || pivotY >= count_y) {
            Log.e(TAG, "PIVOT error, pivotX: " + pivotX + ", count_x: " + count_x + ", pivotY: " + pivotY + ", count_y: " + count_y);
        }
        return matrix;
    }

    private static int handleDpadHorizontal(CellLayoutChildren parent, int iconIdx, int cntX, int cntY, int[][] matrix, int increment) {
        if (matrix == null) {
            throw new IllegalStateException("Dpad navigation requires a matrix.");
        }
        int i;
        int newIconIndex = -1;
        int xPos = -1;
        int yPos = -1;
        for (i = 0; i < cntX; i++) {
            for (int j = 0; j < cntY; j++) {
                if (matrix[i][j] == iconIdx) {
                    xPos = i;
                    yPos = j;
                }
            }
        }
        i = xPos + increment;
        while (i >= 0 && i < cntX) {
            newIconIndex = inspectMatrixHorizontal(parent, i, yPos, cntX, cntY, matrix);
            if (newIconIndex != -1) {
                return newIconIndex;
            }
            i += increment;
        }
        for (int coeff = 1; coeff < cntY; coeff++) {
            int nextYPos1 = yPos + (coeff * increment);
            int nextYPos2 = yPos - (coeff * increment);
            i = xPos + (increment * coeff);
            while (i >= 0 && i < cntX) {
                newIconIndex = inspectMatrixHorizontal(parent, i, nextYPos1, cntX, cntY, matrix);
                if (newIconIndex != -1) {
                    return newIconIndex;
                }
                newIconIndex = inspectMatrixHorizontal(parent, i, nextYPos2, cntX, cntY, matrix);
                if (newIconIndex != -1) {
                    return newIconIndex;
                }
                i += increment;
            }
        }
        return newIconIndex;
    }

    private static int handleDpadVertical(CellLayoutChildren parent, int iconIndex, int cntX, int cntY, int[][] matrix, int increment) {
        if (matrix == null) {
            throw new IllegalStateException("Dpad navigation requires a matrix.");
        }
        int newIconIndex = -1;
        if (cntY == 1) {
            return -1;
        }
        int j;
        int xPos = -1;
        int yPos = -1;
        CellLayoutChildren hotseatParent = ((CellLayout) ((Hotseat) ((ViewGroup) ((Workspace) ((CellLayout) parent.getParent()).getParent()).getParent()).findViewById(R.id.hotseat)).getChildAt(0)).getCellLayoutChildren();
        for (int i = 0; i < cntX; i++) {
            for (j = 0; j < cntY; j++) {
                if (matrix[i][j] == iconIndex) {
                    xPos = i;
                    yPos = j;
                }
            }
        }
        if (xPos == -1 || yPos == -1 || iconIndex == -1) {
            Log.d(TAG, "Invalid icon index");
            return -1;
        }
        View v;
        if (yPos == cntY - 1) {
            v = hotseatParent.getChildAt(matrix[xPos][yPos] - parent.getChildCount());
            if (v == null) {
                v = parent.getChildAt(matrix[xPos][yPos]);
            }
        } else {
            v = parent.getChildAt(matrix[xPos][yPos]);
        }
        j = yPos + increment;
        while (j >= 0 && j < cntY && j >= 0) {
            newIconIndex = inspectMatrixVertical(parent, v, j, cntX, cntY, matrix);
            if (newIconIndex != -1) {
                return newIconIndex;
            }
            j += increment;
        }
        if (increment > 0) {
            newIconIndex = inspectMatrixVertical(hotseatParent, v, j - 1, hotseatParent.getChildCount(), cntY, matrix);
            if (newIconIndex != -1) {
                return newIconIndex;
            }
        }
        return newIconIndex;
    }

    private static int handleMoveHome() {
        return -6;
    }

    private static int handleMoveEnd() {
        return -7;
    }

    private static int handlePageDown(int pageIndex, int pageCount) {
        if (pageIndex < pageCount - 1) {
            return -8;
        }
        return -7;
    }

    private static int handlePageUp(int pageIndex) {
        if (pageIndex > 0) {
            return -3;
        }
        if (pageIndex == 0) {
            return -11;
        }
        return -6;
    }

    private static boolean isValid(int xPos, int yPos, int countX, int countY) {
        return xPos >= 0 && xPos < countX && yPos >= 0 && yPos < countY;
    }

    private static int inspectMatrixHorizontal(CellLayoutChildren parent, int x, int y, int cntX, int cntY, int[][] matrix) {
        if (!isValid(x, y, cntX, cntY) || matrix[x][y] == -1) {
            return -1;
        }
        View child = parent.getChildAt(matrix[x][y]);
        if (child == null || (child instanceof LauncherAppWidgetHostView)) {
            return -1;
        }
        return matrix[x][y];
    }

    private static int inspectMatrixVertical(CellLayoutChildren parent, View v, int y, int cntX, int cntY, int[][] matrix) {
        int newIconIndex = -1;
        int tempDistance = -1;
        int referencePosition = (v.getLeft() + v.getRight()) / 2;
        if (isValid(0, y, cntX, cntY)) {
            for (int i = 0; i < cntX; i++) {
                if (matrix[i][y] != -1) {
                    View child;
                    if (parent instanceof WorkspaceCellLayoutChildren) {
                        child = parent.getChildAt(matrix[i][y]);
                    } else {
                        child = parent.getChildAt(i);
                    }
                    if (!(child == null || (child instanceof LauncherAppWidgetHostView))) {
                        int distanceFromReference = Math.abs(referencePosition - ((child.getLeft() + child.getRight()) / 2));
                        if (newIconIndex == -1) {
                            if ((child.getTag() instanceof ItemInfo) && ((ItemInfo) child.getTag()).container == -101) {
                                newIconIndex = matrix[(int) ((ItemInfo) child.getTag()).screenId][y];
                            } else {
                                newIconIndex = matrix[i][y];
                            }
                            tempDistance = distanceFromReference;
                        } else if (distanceFromReference < tempDistance) {
                            if ((child.getTag() instanceof ItemInfo) && ((ItemInfo) child.getTag()).container == -101) {
                                newIconIndex = matrix[(int) ((ItemInfo) child.getTag()).screenId][y];
                            } else {
                                newIconIndex = matrix[i][y];
                            }
                            tempDistance = distanceFromReference;
                        }
                    }
                }
            }
        }
        return newIconIndex;
    }

    private static String getStringIndex(int index) {
        switch (index) {
            case NEXT_PAGE_LEFT_COLUMN /*-9*/:
                return "NEXT_PAGE_LEFT_COLUMN";
            case NEXT_PAGE_FIRST_ITEM /*-8*/:
                return "NEXT_PAGE_FIRST";
            case -7:
                return "CURRENT_PAGE_LAST";
            case -6:
                return "CURRENT_PAGE_FIRST";
            case -4:
                return "PREVIOUS_PAGE_LAST";
            case -3:
                return "PREVIOUS_PAGE_FIRST";
            case -2:
                return "PREVIOUS_PAGE_RIGHT_COLUMN";
            case -1:
                return "NOOP";
            default:
                return Integer.toString(index);
        }
    }

    private static void printMatrix(int[][] matrix) {
        Log.v(TAG, "\tprintMap:");
        int m = matrix.length;
        int n = matrix[0].length;
        for (int j = 0; j < n; j++) {
            StringBuffer colY = new StringBuffer("\t\t");
            for (int i = 0; i < m; i++) {
                colY.append(String.format(Locale.ENGLISH, "%3d", new Object[]{Integer.valueOf(matrix[i][j])}));
            }
            Log.v(TAG, colY.toString());
        }
    }
}

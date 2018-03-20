package com.android.launcher3.allapps.controller;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Context;
import android.util.Log;
import android.view.View;
import com.android.launcher3.allapps.AppsReorderListener;
import com.android.launcher3.allapps.DragAppIcon;
import com.android.launcher3.allapps.view.AppsPagedView;
import com.android.launcher3.allapps.view.AppsPagedView.Listener;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.CellLayout;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.common.view.Removable;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class AppsReorderController implements AppsReorderListener {
    private static final int INVALID_DIRECTION = -100;
    public static final int REMOVE_ANIMATION_DURATION = 150;
    public static final int REORDER_ANIMATION_DURATION = 150;
    private static final int REORDER_DEFAULT_DELAY_AMOUNT = 30;
    public static final int REORDER_LEFT_DIRECTION = 1;
    private static final int REORDER_RIGHT_DIRECTION = -1;
    public static final int REORDER_TIMEOUT = 350;
    private static final String TAG = "AppsReorderController";
    private final AppsPagedView mAppsPagedView;
    private int mCountX;
    private int mCountY;
    private ArrayList<AnimatorSet> mDeleteAnimators = new ArrayList();
    private boolean mIsOverLastItemMoved = false;
    private Listener mListener;
    private int mOverLastItemFirstPage = 0;
    private int mOverLastItemLastPage = 0;
    boolean[][] mTmpOccupied;

    public AppsReorderController(Context context, AppsPagedView pagedView) {
        this.mAppsPagedView = pagedView;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public void setReorderTarget(CellLayout layout) {
        if (layout != null) {
            this.mCountX = layout.getCountX();
            this.mCountY = layout.getCountY();
            this.mTmpOccupied = (boolean[][]) Array.newInstance(Boolean.TYPE, new int[]{this.mCountX, this.mCountY});
        }
    }

    public void realTimeReorder(DragAppIcon empty, DragAppIcon target) {
        int pageT = target.getScreenId();
        int rankT = target.rank;
        int pageE = empty.getScreenId();
        int rankE = empty.rank;
        int movePage = pageE;
        int direction;
        if (pageE == pageT) {
            int startPos = rankE;
            int endPos = rankT;
            if (rankT > rankE) {
                direction = 1;
            } else if (rankT < rankE) {
                direction = -1;
            } else {
                return;
            }
            realTimeReorder(0, 30.0f, startPos, endPos, direction, movePage);
            return;
        }
        realTimeReorder(0, 30.0f, rankE, this.mAppsPagedView.getItemCountPageAt(pageE), 1, pageE);
        int pageNum = getNumScreenNeededChange(pageT);
        this.mOverLastItemLastPage = pageNum;
        this.mOverLastItemFirstPage = pageT;
        for (int i = pageNum; i >= pageT; i--) {
            if (this.mAppsPagedView.getItemCountPageAt(i) >= this.mAppsPagedView.getMaxItemsPerScreen()) {
                overLastItemNextScreen(0, 30.0f, i);
            }
            startPos = this.mAppsPagedView.getItemCountPageAt(i);
            if (pageT == i) {
                endPos = rankT;
            } else {
                endPos = 0;
            }
            movePage = i;
            direction = 1;
            if (startPos > endPos) {
                direction = -1;
            }
            realTimeReorder(0, 30.0f, startPos, endPos, direction, movePage);
        }
    }

    public void makeEmptyCellAndReorder(int screenId, int rank) {
        if (rank < 0 || rank >= this.mAppsPagedView.getMaxItemsPerScreen()) {
            Log.d(TAG, "rank position = " + rank + " is wrong");
            return;
        }
        int startPos = this.mAppsPagedView.getItemCountPageAt(screenId);
        if (startPos <= rank) {
            Log.d(TAG, "startPos = " + startPos + " rank = " + rank + ", startPos <= rank");
            return;
        }
        int endPos = rank;
        int movePage = screenId;
        for (int i = getNumScreenNeededChangeForMakeEmptyCellAndReorder(screenId); i >= screenId; i--) {
            if (this.mAppsPagedView.getItemCountPageAt(i) == this.mAppsPagedView.getMaxItemsPerScreen()) {
                overLastItemNextScreenForMakeEmptyCellAndReorder(0, 30.0f, i);
            }
            startPos = this.mAppsPagedView.getItemCountPageAt(i);
            if (screenId == i) {
                endPos = rank;
            } else {
                endPos = 0;
            }
            realTimeReorder(0, 30.0f, startPos, endPos, -1, i);
        }
    }

    public void undoOverLastItems() {
        int firstPage = getOverLastItemFirstPage();
        int lastPage = getOverLastItemLastPage();
        for (int movePage = firstPage; movePage <= lastPage; movePage++) {
            int endPos = this.mAppsPagedView.getItemCountPageAt(movePage);
            Log.d(TAG, "movePage : " + movePage + "endPos : " + endPos);
            if (movePage != firstPage) {
                realTimeReorder(30, 0.0f, 0, endPos, 1, movePage);
            }
            if (movePage != lastPage) {
                undoOverLastItemNextScreen(30, 0.0f, movePage + 1);
            }
        }
        setExistOverLastItemMoved(false);
    }

    public void realTimeReorder(int delay, float delayAmount, int startPos, int endPos, int direction, int movePage) {
        if (this.mAppsPagedView.getItemCountPageAt(movePage) > 0) {
            cancelDeleteAnimator();
            Log.d(TAG, "startPos : " + startPos + " / endPos : " + endPos + " / direction : " + direction + " / movePage : " + movePage);
            int countX = this.mAppsPagedView.getCellCountX();
            int countY = this.mAppsPagedView.getCellCountY();
            if ((endPos - startPos <= 0 || direction <= 0) && (endPos - startPos >= 0 || direction >= 0)) {
                Log.w(TAG, "direction is not valid");
                return;
            }
            CellLayout page = this.mAppsPagedView.getCellLayout(movePage);
            int i = startPos;
            while (i != endPos) {
                int nextPos = i + direction;
                View v = page.getChildAt(nextPos % countX, nextPos / countX);
                if (v != null) {
                    ((ItemInfo) v.getTag()).rank -= direction;
                    if (!this.mListener.isAlphabeticalMode()) {
                        ((ItemInfo) v.getTag()).mDirty = true;
                    }
                }
                if (i / countX < countY) {
                    if (page.animateChildToPosition(v, i % countX, i / countX, 150, delay, true, true, this.mTmpOccupied)) {
                        delay = (int) (((float) delay) + delayAmount);
                        delayAmount *= 0.9f;
                    }
                }
                i += direction;
            }
        }
    }

    public boolean getExistOverLastItemMoved() {
        return this.mIsOverLastItemMoved;
    }

    public void setExistOverLastItemMoved(boolean moved) {
        this.mIsOverLastItemMoved = moved;
    }

    public void removeEmptyCell(DragAppIcon empty) {
        int rankE = empty.rank;
        int pageE = empty.getScreenId();
        int startPos = rankE;
        int endPos = this.mAppsPagedView.getItemCountPageAt(pageE);
        CellLayout page = this.mAppsPagedView.getCellLayout(pageE);
        if (page == null) {
            Log.d(TAG, "This was removed!!");
        } else if (page.getChildAt(startPos % this.mAppsPagedView.getCellCountX(), startPos / this.mAppsPagedView.getCellCountX()) != null) {
            Log.d(TAG, "This cell is not empty cell!!");
        } else {
            realTimeReorder(0, 30.0f, startPos, endPos, 1, pageE);
        }
    }

    public void removeEmptyCellsAndViews(ArrayList<DragAppIcon> listToRemove, DragAppIcon currentEmpty, boolean animate) {
        Collections.sort(listToRemove, new Comparator<DragAppIcon>() {
            public int compare(DragAppIcon lhs, DragAppIcon rhs) {
                return ((((int) rhs.screenId) * 100) + rhs.rank) - ((((int) lhs.screenId) * 100) + lhs.rank);
            }
        });
        int countX = this.mAppsPagedView.getCellCountX();
        AnimatorSet removeViewAnimSet = LauncherAnimUtils.createAnimatorSet();
        Iterator it = listToRemove.iterator();
        while (it.hasNext()) {
            DragAppIcon empty = (DragAppIcon) it.next();
            final int startPos = empty.rank;
            final int pageE = empty.getScreenId();
            final CellLayout page = this.mAppsPagedView.getCellLayout(pageE);
            if (page == null) {
                Log.d(TAG, "This was removed!!");
                return;
            } else if (startPos < 0) {
                Log.e(TAG, "startPos is invalid!!");
            } else {
                final int endPos = this.mAppsPagedView.getItemCountPageAt(pageE);
                final View v = page.getChildAt(startPos % countX, startPos / countX);
                if (v == null) {
                    Log.d(TAG, "This cell is already removed");
                } else if (animate) {
                    if (!(v instanceof FolderIconView) && (v instanceof IconView)) {
                        ((IconView) v).markToRemove(true);
                    }
                    r2 = new Animator[3];
                    r2[0] = LauncherAnimUtils.ofFloat(v, View.SCALE_X.getName(), 0.0f);
                    r2[1] = LauncherAnimUtils.ofFloat(v, View.SCALE_Y.getName(), 0.0f);
                    r2[2] = LauncherAnimUtils.ofFloat(v, View.ALPHA.getName(), 0.0f);
                    removeViewAnimSet.playTogether(r2);
                    removeViewAnimSet.setDuration(150);
                    final DragAppIcon dragAppIcon = currentEmpty;
                    removeViewAnimSet.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            page.removeView(v);
                            AppsReorderController.this.doRealTimeReorder(dragAppIcon, startPos, endPos, pageE);
                        }
                    });
                } else {
                    page.removeView(v);
                }
                if (!animate) {
                    doRealTimeReorder(currentEmpty, startPos, endPos, pageE);
                }
            }
        }
        if (animate) {
            removeViewAnimSet.start();
        }
    }

    private void doRealTimeReorder(DragAppIcon currentEmpty, int startPos, int endPos, int pageE) {
        if (!this.mListener.isAlphabeticalMode()) {
            realTimeReorder(0, 30.0f, startPos, endPos, 1, pageE);
        }
        if (currentEmpty != null && currentEmpty.screenId == ((long) pageE) && currentEmpty.rank > startPos && currentEmpty.rank < endPos) {
            currentEmpty.rank--;
        }
    }

    public void removeEmptyCellsAndViews(ArrayList<ItemInfo> removeItems) {
        removeEmptyCellsAndViews(removeItems, false);
    }

    public void removeEmptyCellsAndViews(ArrayList<ItemInfo> removeItems, boolean animate) {
        ArrayList<Long> dirtyScreen = new ArrayList();
        ArrayList<AnimatorSet> deleteAnimators = null;
        Iterator it = removeItems.iterator();
        while (it.hasNext()) {
            ItemInfo item = (ItemInfo) it.next();
            View v = this.mListener.getAppsIconByItemId(item.id);
            if (v != null) {
                CellLayout parent = (CellLayout) v.getParent().getParent();
                if (parent == null) {
                    Log.w(TAG, "celllayout is not exist : " + item.screenId);
                } else if (animate) {
                    if (deleteAnimators == null) {
                        deleteAnimators = new ArrayList();
                    }
                    deleteAnimators.add(startDeleteAnimation(parent, v));
                } else {
                    parent.removeView(v);
                }
            }
            if (!dirtyScreen.contains(Long.valueOf(item.screenId))) {
                dirtyScreen.add(Long.valueOf(item.screenId));
            }
        }
        if (!(deleteAnimators == null || deleteAnimators.isEmpty())) {
            Log.d(TAG, "start deleteAnimators");
            it = deleteAnimators.iterator();
            while (it.hasNext()) {
                ((AnimatorSet) it.next()).start();
            }
            this.mDeleteAnimators.addAll(deleteAnimators);
        }
        if (!this.mListener.isAlphabeticalMode()) {
            Iterator it2 = dirtyScreen.iterator();
            while (it2.hasNext()) {
                removeEmptyCellAtPage(0, (this.mAppsPagedView.getCellCountX() * this.mAppsPagedView.getCellCountY()) - 1, (int) ((Long) it2.next()).longValue(), animate);
            }
        }
    }

    public void removeEmptyCellAtPage(int startPos, int endPos, int pageIndex, boolean animate) {
        int countX = this.mAppsPagedView.getCellCountX();
        CellLayout page = this.mAppsPagedView.getCellLayout(pageIndex);
        if (page == null) {
            Log.w(TAG, "page is not exist : " + pageIndex);
            return;
        }
        page.clearOccupiedCells();
        int pageRank = 0;
        int startDelay = 0;
        float delayAmount = 30.0f;
        for (int i = startPos; i <= endPos; i++) {
            View v = page.getChildAt(i % countX, i / countX);
            boolean tobRemove = v instanceof Removable ? ((Removable) v).isMarkToRemove() : false;
            if (!(v == null || tobRemove)) {
                ItemInfo info = (ItemInfo) v.getTag();
                if (i != pageRank) {
                    long j;
                    AppsPagedView appsPagedView = this.mAppsPagedView;
                    long j2 = (long) pageIndex;
                    if (animate) {
                        startDelay = (int) (((float) startDelay) + delayAmount);
                        j = (long) startDelay;
                    } else {
                        j = 0;
                    }
                    appsPagedView.updateItemToNewPosition(info, pageRank, j2, j, animate ? -1 : 0, (boolean[][]) null);
                    delayAmount *= 0.9f;
                    startDelay = (int) (((float) startDelay) + delayAmount);
                    info.mDirty = true;
                }
                pageRank++;
            }
        }
        page.markCellsAsOccupiedForAllChild();
    }

    private AnimatorSet startDeleteAnimation(final CellLayout page, final View v) {
        final AnimatorSet deleteAnimator = LauncherAnimUtils.createAnimatorSet();
        if (!(v instanceof FolderIconView) && (v instanceof Removable)) {
            ((Removable) v).markToRemove(true);
        }
        r1 = new Animator[3];
        r1[0] = LauncherAnimUtils.ofFloat(v, View.SCALE_X.getName(), 0.0f);
        r1[1] = LauncherAnimUtils.ofFloat(v, View.SCALE_Y.getName(), 0.0f);
        r1[2] = LauncherAnimUtils.ofFloat(v, View.ALPHA.getName(), 0.0f);
        deleteAnimator.playTogether(r1);
        deleteAnimator.setDuration(150);
        deleteAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                page.removeView(v);
            }

            public void onAnimationEnd(Animator animation) {
                AppsReorderController.this.mDeleteAnimators.remove(deleteAnimator);
            }
        });
        return deleteAnimator;
    }

    private int getNumScreenNeededChangeForMakeEmptyCellAndReorder(int startScreen) {
        int maxItemCount = this.mAppsPagedView.getMaxItemsPerScreen();
        int pageCount = this.mAppsPagedView.getPageCount();
        for (int i = startScreen; i < pageCount; i++) {
            if (this.mAppsPagedView.getItemCountPageAt(i) < maxItemCount) {
                return i;
            }
        }
        return pageCount - 1;
    }

    private void overLastItemNextScreenForMakeEmptyCellAndReorder(int delay, float delayAmount, int movePage) {
        if (movePage <= this.mAppsPagedView.getPageCount() - 1) {
            CellLayout page = this.mAppsPagedView.getCellLayout(movePage);
            View v = page.getChildAt(this.mAppsPagedView.getCellCountX() - 1, this.mAppsPagedView.getCellCountY() - 1);
            if (v != null) {
                page.removeView(v);
                if (this.mAppsPagedView.getPageAt(movePage + 1) == null) {
                    this.mAppsPagedView.createAppsPage();
                } else if (movePage + 1 == this.mAppsPagedView.getExtraEmptyScreenIndex()) {
                    this.mAppsPagedView.commitExtraEmptyScreen();
                }
                this.mAppsPagedView.addViewForRankScreen(v, (ItemInfo) v.getTag(), 0, movePage + 1);
            }
        }
    }

    public int getNumScreenNeededChange(int startScreen) {
        int maxItemCount = this.mAppsPagedView.getMaxItemsPerScreen();
        int pageCount = this.mAppsPagedView.getPageCount();
        for (int i = startScreen; i < pageCount; i++) {
            if (this.mAppsPagedView.getItemCountPageAt(i) < maxItemCount) {
                return i;
            }
        }
        return 0;
    }

    public void overLastItemNextScreen(int delay, float delayAmount, int movePage) {
        if (movePage < this.mAppsPagedView.getPageCount() - 1) {
            CellLayout page = this.mAppsPagedView.getCellLayout(movePage);
            View v = page.getChildAt(this.mAppsPagedView.getCellCountX() - 1, this.mAppsPagedView.getCellCountY() - 1);
            if (v != null) {
                page.removeView(v);
                if (movePage + 1 == this.mAppsPagedView.getExtraEmptyScreenIndex()) {
                    this.mAppsPagedView.commitExtraEmptyScreen();
                }
                this.mAppsPagedView.addViewForRankScreen(v, (ItemInfo) v.getTag(), 0, movePage + 1);
                setExistOverLastItemMoved(true);
                Log.d(TAG, "overLastItemNextScreen to " + (movePage + 1));
            }
        }
    }

    private void undoOverLastItemNextScreen(int delay, float delayAmount, int movePage) {
        if (movePage < this.mAppsPagedView.getPageCount()) {
            CellLayout page = this.mAppsPagedView.getCellLayout(movePage);
            View v = page.getChildAt(0, 0);
            if (v != null) {
                Log.d(TAG, "undoOverLastItemNextScreen : movePage = " + movePage + " v = " + ((ItemInfo) v.getTag()).title);
                page.removeView(v);
                this.mAppsPagedView.addViewForRankScreen(v, (ItemInfo) v.getTag(), this.mAppsPagedView.getMaxItemsPerScreen() - 1, movePage - 1);
            }
        }
    }

    private int getOverLastItemFirstPage() {
        return this.mOverLastItemFirstPage;
    }

    private int getOverLastItemLastPage() {
        return this.mOverLastItemLastPage;
    }

    private void cancelDeleteAnimator() {
        if (!this.mDeleteAnimators.isEmpty()) {
            ArrayList<AnimatorSet> animators = new ArrayList(this.mDeleteAnimators);
            this.mDeleteAnimators.clear();
            Iterator it = animators.iterator();
            while (it.hasNext()) {
                ((AnimatorSet) it.next()).cancel();
            }
            Log.d(TAG, "cancel deleteAnimation : " + animators.size());
        }
    }
}

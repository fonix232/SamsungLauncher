package com.android.launcher3.common.quickoption;

import android.animation.AnimatorSet;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.quickoption.shortcuts.DeepShortcutManager;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutFilter;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.common.quickoption.shortcuts.ShortcutKey;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.List;

public class AppShortcutItemView extends PopupItemView {
    private static final int MAX_SHORTCUTS_IF_NOTIFICATIONS = 3;
    private final int ITEM_ANIM_START_GAP;
    private int mAppShortcutListSize;
    private Launcher mLauncher;

    public AppShortcutItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppShortcutItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.ITEM_ANIM_START_GAP = 33;
        this.mAppShortcutListSize = 0;
    }

    public AppShortcutItemView(Context context) {
        super(context, null, 0);
        this.ITEM_ANIM_START_GAP = 33;
        this.mAppShortcutListSize = 0;
    }

    void createContainerItems(Launcher launcher, LayoutInflater inflater, ItemInfo itemInfo, List<String> appShortcutIds, boolean hasNotifications) {
        this.mLauncher = launcher;
        List<QuickOptionListItem> appShortcutList = new ArrayList();
        makeAppShortcutList(appShortcutList, appShortcutIds, (IconInfo) itemInfo, hasNotifications);
        setAppShortcutListSize(appShortcutList.size());
        setPopupHeight(addAppShortcutViews(inflater, appShortcutList));
    }

    private void makeAppShortcutList(List<QuickOptionListItem> appShortcutList, List<String> appShortcutIds, IconInfo iconInfo, boolean hasNotifications) {
        DeepShortcutManager manager = LauncherAppState.getInstance().getShortcutManager();
        List<ShortcutInfoCompat> shortcuts = ShortcutFilter.sortAndFilterShortcuts(manager.queryForShortcutsContainer(iconInfo.getTargetComponent(), appShortcutIds, iconInfo.user));
        int numShortcuts = shortcuts.size();
        if (hasNotifications && numShortcuts > 3) {
            numShortcuts = 3;
        }
        for (int i = 0; i < numShortcuts; i++) {
            ShortcutInfoCompat shortcut = (ShortcutInfoCompat) shortcuts.get(i);
            QuickOptionListItem item = new QuickOptionListItem();
            item.setType(1);
            item.setShortcutKey(ShortcutKey.fromInfo(shortcut));
            item.setTitle(shortcut.getShortLabel().toString());
            item.setIcon(manager.getShortcutIconDrawable(shortcut));
            appShortcutList.add(item);
        }
    }

    private int addAppShortcutViews(LayoutInflater inflater, List<QuickOptionListItem> appShortcutList) {
        int appShortcutSize = appShortcutList.size();
        Resources res = getResources();
        int listTopPadding = res.getDimensionPixelSize(R.dimen.quick_option_listview_padding_top);
        int listBottomPadding = res.getDimensionPixelSize(R.dimen.quick_option_listview_padding_top);
        int itemTopBottomPadding = res.getDimensionPixelSize(R.dimen.quick_option_item_gap) / 2;
        int iconSize = res.getDimensionPixelSize(R.dimen.quick_options_icon_size);
        int itemStartEndPadding = res.getDimensionPixelSize(R.dimen.quick_option_listview_padding_start);
        int totalHeight = 0;
        for (int i = 0; i < appShortcutSize; i++) {
            int itemTopPadding;
            int itemBottomPadding;
            if (i == 0) {
                itemTopPadding = listTopPadding;
            } else {
                itemTopPadding = itemTopBottomPadding;
            }
            if (i == appShortcutSize - 1) {
                itemBottomPadding = listBottomPadding;
            } else {
                itemBottomPadding = itemTopBottomPadding;
            }
            int itemHeight = (iconSize + itemTopPadding) + itemBottomPadding;
            totalHeight += itemHeight;
            AppShortcut itemView = (AppShortcut) inflater.inflate(R.layout.app_shortcut_item, this, false);
            itemView.setItem(this.mLauncher, (QuickOptionListItem) appShortcutList.get(i));
            LayoutParams lp = new LayoutParams(getPopupWidth(), itemHeight);
            itemView.setPadding(itemStartEndPadding, itemTopPadding, itemStartEndPadding, itemBottomPadding);
            itemView.setOnClickListener(itemView);
            itemView.setOnLongClickListener(itemView);
            itemView.setOnKeyListener(this.mKeyListener);
            addView(itemView, i, lp);
        }
        return totalHeight;
    }

    public int getAppShortcutListSize() {
        return this.mAppShortcutListSize;
    }

    private void setAppShortcutListSize(int appShortcutListSize) {
        this.mAppShortcutListSize = appShortcutListSize;
    }

    AnimatorSet getItemShowAnim() {
        AnimatorSet allAnimSet = new AnimatorSet();
        for (int i = 0; i < getChildCount(); i++) {
            AnimatorSet itemAnim = ((AppShortcut) getChildAt(i)).getItemAnim();
            itemAnim.setStartDelay((long) (i * 33));
            allAnimSet.play(itemAnim);
        }
        return allAnimSet;
    }

    ArrayList<View> getAccessibilityFocusChildViewList() {
        ArrayList<View> childList = new ArrayList();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof AppShortcut) {
                childList.add(child);
            }
        }
        return childList;
    }
}

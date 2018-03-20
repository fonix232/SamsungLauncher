package com.android.launcher3.notification;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.view.DragLayer;
import com.android.launcher3.common.view.IconView;
//import com.samsung.android.widget.SemTipPopup;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationHelpTipManager {
    private static final String GLOBAL_NOTIFICATION_SETTINGS_INTENT = "com.samsung.settings.APP_ICON_BADGES_SETTINGS";
    private static final long GRANTED_TIME_TO_SHOW = 3600;
    private static final int MAX_COUNT = 1;
    private static final String PREFERENCES_SHOW_NOTIFICATION_HELP_TIP_COUNT = "show_notification_help_tip_count";
    private static final String TAG = "NotificationHelpTip";
    private final Context mContext;
    private int mCount = -1;
    private final DragLayer mDragLayer;
    private boolean mEnableToShow = true;
    private ArrayList<IconView> mHotSeatList;
//    private SemTipPopup mSmartTip;
    private long mTime = -1;
    private ArrayList<IconView> mWorkSpaceList;

    public NotificationHelpTipManager(Context context, DragLayer dragLayer) {
        this.mDragLayer = dragLayer;
        this.mContext = context;
    }

    public boolean isValidToShowHelpTip() {
        return checkGrantedTimeToShow() && checkNotificationHelpTipAccessPref();
    }

    public void setIconViewList(ArrayList<IconView> workSpace, ArrayList<IconView> hotSeat) {
        this.mWorkSpaceList = workSpace;
        this.mHotSeatList = hotSeat;
    }

    public void enableShowHelpTip() {
        this.mEnableToShow = true;
    }

    public void disableShowHelpTip() {
        this.mEnableToShow = false;
//        if (this.mSmartTip != null) {
//            this.mSmartTip.dismiss(false);
//        }
    }

    public boolean updateHelpTip() {
        if (!this.mEnableToShow || !checkNotificationHelpTipAccessPref()) {
            return false;
        }
        IconView targetIconView;
        ArrayList<IconView> workSpaceList = setIconList(this.mWorkSpaceList);
        ArrayList<IconView> hotSeatList = setIconList(this.mHotSeatList);
        setSmartTipAttributes();
        if (hotSeatList.size() > 0) {
            targetIconView = findTargetIcon(hotSeatList);
        } else {
            targetIconView = findTargetIcon(workSpaceList);
        }
        if (targetIconView == null) {
            return false;
        }
        setHelpTipsPosition(targetIconView);
        return true;
    }

    private boolean checkGrantedTimeToShow() {
        if (this.mTime < 0) {
            this.mTime = getFirstLauncherTimeToPref();
        }
        Log.d(TAG, "currentTime : " + (System.currentTimeMillis() / 1000));
        Log.d(TAG, "differentTime" + ((System.currentTimeMillis() / 1000) - this.mTime));
        return (System.currentTimeMillis() / 1000) - this.mTime > GRANTED_TIME_TO_SHOW;
    }

    private ArrayList<IconView> setIconList(List<IconView> list) {
        ArrayList<IconView> iconList = new ArrayList();
        if (list != null) {
            for (IconView iconView : list) {
                if (iconView != null) {
                    ItemInfo itemInfo = (ItemInfo) iconView.getTag();
                    if (itemInfo != null && itemInfo.mBadgeCount > 0) {
                        iconList.add(iconView);
                    }
                }
            }
        }
        return iconList;
    }

    private void setHelpTipsPosition(IconView targetIconView) {
        boolean bottomTop = true;
        int[] pos = new int[]{0, 0};
        Utilities.getDescendantCoordRelativeToParent(targetIconView, this.mDragLayer, pos, false);
        ItemInfo item = (ItemInfo) targetIconView.getTag();
        Log.d(TAG, "CellX = " + item.cellX + ", CellY = " + item.cellY + ", posX = " + pos[0] + ", posY = " + pos[1]);
        int targetX = pos[0] + (targetIconView.getWidth() / 2);
        int targetY = pos[1];
//        if (this.mSmartTip != null) {
//            if (item.container == -101 || ((targetIconView.isLandscape() || item.cellY != 0) && !(targetIconView.isLandscape() && (item.cellY == 0 || item.cellY == 1)))) {
//                bottomTop = false;
//            }
//            show(bottomTop, targetX, targetY, targetIconView);
//            int i = this.mCount + 1;
//            this.mCount = i;
//            setNotificationHelpTipAccessPref(i);
//        }
    }

    private void show(boolean bottomTop, int targetX, int targetY, IconView targetIconView) {
//        if (bottomTop) {
//            this.mSmartTip.setTargetPosition(targetX, targetIconView.getHeight() + targetY);
//            this.mSmartTip.show(2);
//            return;
//        }
//        this.mSmartTip.setTargetPosition(targetX, targetY);
//        this.mSmartTip.show(0);
    }

    private void setSmartTipAttributes() {
//        if (this.mSmartTip != null) {
//            this.mSmartTip.dismiss(false);
//            this.mSmartTip = null;
//        }
//        this.mSmartTip = new SemTipPopup(this.mDragLayer);
//        this.mSmartTip.setMessage(this.mContext.getResources().getString(R.string.badge_help_tip_body));
//        this.mSmartTip.setExpanded(true);
//        this.mSmartTip.setAction(this.mContext.getResources().getString(R.string.badge_help_tip_button), new OnClickListener() {
//            public void onClick(View view) {
//                performDetails();
//                if (NotificationHelpTipManager.this.mSmartTip != null) {
//                    NotificationHelpTipManager.this.mSmartTip.dismiss(false);
//                }
//            }
//
//            private void performDetails() {
//                Intent intent = new Intent();
//                intent.setAction(NotificationHelpTipManager.GLOBAL_NOTIFICATION_SETTINGS_INTENT);
//                try {
//                    if (NotificationHelpTipManager.this.mContext != null) {
//                        NotificationHelpTipManager.this.mContext.startActivity(intent);
//                    }
//                } catch (ActivityNotFoundException e) {
//                    Log.e(NotificationHelpTipManager.TAG, e.toString());
//                }
//            }
//        });
    }

    private IconView findTargetIcon(List<IconView> list) {
        if (list.size() <= 0) {
            return null;
        }
        Collections.sort(list, new Comparator<IconView>() {
            public int compare(IconView lhs, IconView rhs) {
                ItemInfo lh = (ItemInfo) lhs.getTag();
                ItemInfo rh = (ItemInfo) rhs.getTag();
                int ret = lh.cellY - rh.cellY;
                return ret == 0 ? lh.cellX - rh.cellX : ret;
            }
        });
        return (IconView) list.get(0);
    }

    private boolean checkNotificationHelpTipAccessPref() {
        if (this.mCount < 0) {
            this.mCount = getNotificationHelpTipAccessPref();
        }
        if (this.mCount < 1) {
            return true;
        }
        return false;
    }

    private void setNotificationHelpTipAccessPref(int count) {
        if (this.mContext != null) {
            Editor prefsEdit = this.mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
            if (prefsEdit != null) {
                prefsEdit.putInt(PREFERENCES_SHOW_NOTIFICATION_HELP_TIP_COUNT, count);
                prefsEdit.apply();
            }
        }
    }

    private int getNotificationHelpTipAccessPref() {
        int count = this.mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).getInt(PREFERENCES_SHOW_NOTIFICATION_HELP_TIP_COUNT, 0);
        Log.e(TAG, "get count : " + count);
        return count;
    }

    private long getFirstLauncherTimeToPref() {
        long time = this.mContext.getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).getLong(NotificationListener.PREFERENCES_FIRST_LAUNCHER_TIME, System.currentTimeMillis() / 1000);
        Log.e(TAG, "get time : " + time);
        return time;
    }

    public boolean hasIconList() {
        return (this.mWorkSpaceList == null || this.mHotSeatList == null) ? false : true;
    }
}

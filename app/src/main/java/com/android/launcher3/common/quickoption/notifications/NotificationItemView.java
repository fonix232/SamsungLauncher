package com.android.launcher3.common.quickoption.notifications;

import android.animation.Animator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.android.launcher3.BadgeInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.anim.PillHeightRevealOutlineProvider;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.drag.DragObject;
import com.android.launcher3.common.quickoption.PopupItemView;
import com.android.launcher3.common.quickoption.notifications.NotificationFooterLayout.IconAnimationEndListener;
import com.android.launcher3.notification.NotificationInfo;
import com.android.launcher3.notification.NotificationKeyData;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationItemView extends PopupItemView {
    private static final Comparator<StatusBarNotification> NOTIFICATION_COMPARATOR = new Comparator<StatusBarNotification>() {
        public final int compare(StatusBarNotification a, StatusBarNotification b) {
            long aInfo = a.getNotification().when;
            long bInfo = b.getNotification().when;
            if (aInfo == bInfo) {
                return 0;
            }
            return aInfo < bInfo ? 1 : -1;
        }
    };
    private static final String TAG = "NotificationItemView";
    private static final Rect sTempRect = new Rect();
    private boolean mAnimatingNextIcon;
    private NotificationFooterLayout mFooter;
    private View mIconView;
    private ItemInfo mItemInfo;
    private final Launcher mLauncher;
    private NotificationMainView mMainView;
    private final Rect mPillRect;
    private SwipeHelper mSwipeHelper;

    private static class UpdateNotificationChild implements Runnable {
        private final List<NotificationInfo> mNotificationInfos;
        private final NotificationItemView mNotificationView;

        UpdateNotificationChild(NotificationItemView notificationView, List<NotificationInfo> notificationInfos) {
            this.mNotificationView = notificationView;
            this.mNotificationInfos = notificationInfos;
        }

        public void run() {
            this.mNotificationView.applyNotificationInfos(this.mNotificationInfos);
        }
    }

    public NotificationItemView(Context context) {
        this(context, null, 0);
    }

    public NotificationItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mLauncher = (Launcher) context;
        this.mPillRect = new Rect();
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mIconView = findViewById(R.id.popup_item_icon);
        this.mMainView = (NotificationMainView) findViewById(R.id.main_view);
        this.mFooter = (NotificationFooterLayout) findViewById(R.id.footer);
        this.mSwipeHelper = new SwipeHelper(0, this.mMainView, getContext(), this);
    }

    public void show(DragObject dragObject, List<NotificationKeyData> notificationKeys) {
        this.mItemInfo = (ItemInfo) dragObject.dragInfo;
        if (this.mMainView == null) {
            this.mMainView = (NotificationMainView) findViewById(R.id.main_view);
        }
        if (this.mFooter == null) {
            this.mFooter = (NotificationFooterLayout) findViewById(R.id.footer);
        }
        if (this.mSwipeHelper == null) {
            this.mSwipeHelper = new SwipeHelper(0, this.mMainView, getContext(), this);
        }
        this.mMainView.setOnKeyListener(this.mKeyListener);
        if (this.mItemInfo instanceof IconInfo) {
            BadgeInfo badgeInfo = this.mLauncher.getLauncherModel().getBadgeInfoForItem(this.mItemInfo);
            if (badgeInfo != null) {
                String description;
                if (badgeInfo.getNotificationCount() >= 2) {
                    description = getResources().getString(R.string.quick_option_and_more_notification, new Object[]{Integer.valueOf(this.mLauncher.getDragMgr().getQuickOptionView().getQuickOptionSize()), Integer.valueOf(badgeInfo.getNotificationCount()), iconInfo.title});
                } else {
                    description = getResources().getString(R.string.quick_option_and_one_notification, new Object[]{Integer.valueOf(this.mLauncher.getDragMgr().getQuickOptionView().getQuickOptionSize()), iconInfo.title});
                }
                this.mFooter.setContentDescription(description);
            }
        }
        new Handler(LauncherModel.getWorkerLooper()).postAtFrontOfQueue(createUpdateRunnable(this.mLauncher, this.mItemInfo, new Handler(Looper.getMainLooper()), notificationKeys, this));
    }

    public int getPopupHeight(boolean enableFooter) {
        Resources res = getResources();
        return (res.getDimensionPixelSize(R.dimen.notification_main_height) + res.getDimensionPixelSize(R.dimen.notification_header_height)) + (enableFooter ? res.getDimensionPixelSize(R.dimen.notification_footer_height) + res.getDimensionPixelSize(R.dimen.popup_item_divider_height) : 0);
    }

    public Animator animateHeightRemoval() {
        return new PillHeightRevealOutlineProvider(this.mPillRect, (float) getResources().getDimensionPixelSize(R.dimen.quick_option_popup_radius), getPopupHeight(false)).createRevealAnimator(this, true);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.mPillRect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return this.mMainView.getNotificationInfo() != null && (this.mSwipeHelper.onTouchEvent(ev) || super.onTouchEvent(ev));
    }

    private static Runnable createUpdateRunnable(final Launcher launcher, ItemInfo originalInfo, final Handler uiHandler, final List<NotificationKeyData> notificationKeys, final NotificationItemView notificationView) {
        return new Runnable() {
            public void run() {
                if (notificationView != null) {
                    List<StatusBarNotification> notifications = launcher.getLauncherModel().getStatusBarNotificationsForKeys(notificationKeys);
                    if (notifications.size() > 1) {
                        Collections.sort(notifications, NotificationItemView.NOTIFICATION_COMPARATOR);
                    }
                    List<NotificationInfo> infos = new ArrayList(notifications.size());
                    for (int i = 0; i < notifications.size(); i++) {
                        infos.add(new NotificationInfo(launcher, (StatusBarNotification) notifications.get(i)));
                    }
                    uiHandler.post(new UpdateNotificationChild(notificationView, infos));
                }
            }
        };
    }

    private void applyNotificationInfos(List<NotificationInfo> notificationInfos) {
        if (!notificationInfos.isEmpty()) {
            this.mMainView.applyNotificationInfo((NotificationInfo) notificationInfos.get(0), this.mIconView);
            for (int i = 1; i < notificationInfos.size(); i++) {
                this.mFooter.addNotificationInfo((NotificationInfo) notificationInfos.get(i));
            }
            this.mFooter.commitNotificationInfos();
        }
    }

    public void trimNotifications(List<String> notificationKeys) {
        if (this.mMainView.getNotificationInfo() == null) {
            Log.d(TAG, "NotificationInfo is null");
            return;
        }
        if (!(!notificationKeys.contains(this.mMainView.getNotificationInfo().notificationKey)) || this.mAnimatingNextIcon) {
            this.mFooter.trimNotifications(notificationKeys);
            return;
        }
        this.mAnimatingNextIcon = true;
        this.mMainView.setVisibility(4);
        this.mMainView.setTranslationX(0.0f);
        this.mIconView.getGlobalVisibleRect(sTempRect);
        final NotificationItemView itemView = this;
        this.mFooter.animateFirstNotificationTo(sTempRect, new IconAnimationEndListener() {
            public void onIconAnimationEnd(NotificationInfo newMainNotification) {
                if (newMainNotification != null) {
                    NotificationItemView.this.mMainView.applyNotificationInfo(newMainNotification, NotificationItemView.this.mIconView, true, itemView);
                    NotificationItemView.this.mMainView.setVisibility(View.VISIBLE);
                }
                NotificationItemView.this.mAnimatingNextIcon = false;
            }
        });
    }

    public ArrayList<View> getAccessibilityFocusChildViewList(boolean enableFooter) {
        ArrayList<View> childList = new ArrayList();
        childList.add(this.mMainView);
        if (enableFooter) {
            childList.add(this.mFooter);
        }
        return childList;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mMainView.getNotificationInfo() == null) {
            return false;
        }
        getParent().requestDisallowInterceptTouchEvent(true);
        return this.mSwipeHelper.onInterceptTouchEvent(ev);
    }
}

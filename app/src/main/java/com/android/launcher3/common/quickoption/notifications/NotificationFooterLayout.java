package com.android.launcher3.common.quickoption.notifications;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.anim.PropertyListBuilder;
import com.android.launcher3.anim.PropertyResetListener;
import com.android.launcher3.notification.NotificationInfo;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.animation.LauncherAnimUtils;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NotificationFooterLayout extends FrameLayout {
    private static final int MAX_FOOTER_NOTIFICATIONS = 5;
    private static final String TAG = "NotificationFooter";
    private static final Rect sTempRect = new Rect();
    private final int NOTIFICATION_FOOTER_POSITION_DURATION;
    private int mBackgroundColor;
    private final LayoutParams mIconLayoutParams;
    private LinearLayout mIconRow;
    private final Launcher mLauncher;
    private final List<NotificationInfo> mNotifications;
    private View mOverflowEllipsis;
    private final List<NotificationInfo> mOverflowNotifications;
    private final Interpolator mSineInOut80;

    public interface IconAnimationEndListener {
        void onIconAnimationEnd(NotificationInfo notificationInfo);
    }

    public NotificationFooterLayout(Context context) {
        this(context, null, 0);
    }

    public NotificationFooterLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationFooterLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.NOTIFICATION_FOOTER_POSITION_DURATION = 333;
        this.mNotifications = new ArrayList();
        this.mOverflowNotifications = new ArrayList();
        this.mSineInOut80 = ViInterpolator.getInterploator(34);
        Resources res = getResources();
        this.mLauncher = (Launcher) context;
        int iconSize = res.getDimensionPixelSize(R.dimen.notification_footer_icon_size);
        this.mIconLayoutParams = new LayoutParams(iconSize, iconSize);
        this.mIconLayoutParams.gravity = 16;
        this.mIconLayoutParams.setMarginStart(((res.getDimensionPixelSize(R.dimen.quick_option_item_width) - (res.getDimensionPixelSize(R.dimen.notification_footer_margin) * 2)) - (iconSize * 6)) / 5);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mOverflowEllipsis = findViewById(R.id.overflow);
        this.mIconRow = (LinearLayout) findViewById(R.id.icon_row);
        this.mBackgroundColor = getResources().getColor(R.color.quick_options_popup_color, null);
    }

    public void addNotificationInfo(NotificationInfo notificationInfo) {
        if (this.mNotifications.size() <= 5) {
            this.mNotifications.add(notificationInfo);
        } else {
            this.mOverflowNotifications.add(notificationInfo);
        }
    }

    public void commitNotificationInfos() {
        this.mIconRow.removeAllViews();
        int i = 0;
        while (i < this.mNotifications.size() && (this.mOverflowNotifications.isEmpty() || i < 5)) {
            addNotificationIconForInfo((NotificationInfo) this.mNotifications.get(i));
            i++;
        }
        updateOverflowEllipsisVisibility();
    }

    private void updateOverflowEllipsisVisibility() {
        this.mOverflowEllipsis.setVisibility(this.mOverflowNotifications.isEmpty() ? 8 : 0);
    }

    private View addNotificationIconForInfo(NotificationInfo info) {
        View icon = new View(getContext());
        icon.setBackground(info.getIconForBackground(getContext(), this.mBackgroundColor));
        icon.setOnClickListener(info);
        icon.setTag(info);
        icon.setImportantForAccessibility(2);
        this.mIconRow.addView(icon, 0, this.mIconLayoutParams);
        return icon;
    }

    public void animateFirstNotificationTo(Rect toBounds, IconAnimationEndListener callback) {
        final View firstNotification = this.mIconRow.getChildAt(this.mIconRow.getChildCount() - 1);
        if (firstNotification == null) {
            Log.d(TAG, "firstNotification = null ");
            if (this.mLauncher != null && this.mLauncher.getDragMgr().getQuickOptionView() != null) {
                this.mLauncher.getDragMgr().getQuickOptionView().removeNotificationView();
                return;
            }
            return;
        }
        AnimatorSet animation = LauncherAnimUtils.createAnimatorSet();
        Rect fromBounds = sTempRect;
        firstNotification.getGlobalVisibleRect(fromBounds);
        float scale = ((float) toBounds.height()) / ((float) fromBounds.height());
        float shiftX = ((float) (toBounds.width() - fromBounds.width())) / 2.0f;
        PropertyListBuilder translationY = new PropertyListBuilder().scale(scale).translationY(((float) (toBounds.top - fromBounds.top)) + (((((float) fromBounds.height()) * scale) - ((float) fromBounds.height())) / 2.0f));
        if (!Utilities.sIsRtl) {
            shiftX = -shiftX;
        }
        Animator moveAndScaleIcon = LauncherAnimUtils.ofPropertyValuesHolder(firstNotification, translationY.translationX(shiftX).build());
        moveAndScaleIcon.setDuration((long) 333);
        moveAndScaleIcon.setInterpolator(this.mSineInOut80);
        final IconAnimationEndListener iconAnimationEndListener = callback;
        moveAndScaleIcon.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                iconAnimationEndListener.onIconAnimationEnd((NotificationInfo) firstNotification.getTag());
                NotificationFooterLayout.this.removeViewFromIconRow(firstNotification);
            }
        });
        animation.play(moveAndScaleIcon);
        int gapWidth = this.mIconLayoutParams.width + this.mIconLayoutParams.getMarginStart();
        if (Utilities.sIsRtl) {
            gapWidth = -gapWidth;
        }
        if (!this.mOverflowNotifications.isEmpty()) {
            NotificationInfo notification = (NotificationInfo) this.mOverflowNotifications.remove(0);
            this.mNotifications.add(notification);
            float[] fArr = new float[2];
            animation.play(ObjectAnimator.ofFloat(addNotificationIconForInfo(notification), ALPHA, new float[]{0.0f, 1.0f}).setDuration((long) 333));
        }
        int numIcons = this.mIconRow.getChildCount() - 1;
        PropertyResetListener<View, Float> propertyResetListener = new PropertyResetListener(TRANSLATION_X, Float.valueOf(0.0f));
        for (int i = 0; i < numIcons; i++) {
            Animator shiftChild = ObjectAnimator.ofFloat(this.mIconRow.getChildAt(i), TRANSLATION_X, new float[]{(float) gapWidth});
            shiftChild.addListener(propertyResetListener);
            shiftChild.setDuration((long) 333);
            animation.play(shiftChild);
        }
        animation.start();
    }

    private void removeViewFromIconRow(View child) {
        this.mIconRow.removeView(child);
        if (child.getTag() instanceof NotificationInfo) {
            this.mNotifications.remove(child.getTag());
        }
        updateOverflowEllipsisVisibility();
        if (this.mNotifications.size() == 6 && this.mOverflowNotifications.isEmpty()) {
            commitNotificationInfos();
        }
        if (this.mIconRow.getChildCount() == 0 && this.mLauncher != null && this.mLauncher.getDragMgr().getQuickOptionView() != null) {
            Animator collapseFooter = this.mLauncher.getDragMgr().getQuickOptionView().reduceNotificationViewHeight(true);
            collapseFooter.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    ((ViewGroup) NotificationFooterLayout.this.getParent()).removeView(NotificationFooterLayout.this);
                }
            });
            collapseFooter.start();
        }
    }

    public void trimNotifications(List<String> notifications) {
        if (isAttachedToWindow() && this.mIconRow.getChildCount() != 0) {
            Iterator<NotificationInfo> overflowIterator = this.mOverflowNotifications.iterator();
            while (overflowIterator.hasNext()) {
                if (!notifications.contains(((NotificationInfo) overflowIterator.next()).notificationKey)) {
                    overflowIterator.remove();
                }
            }
            for (int i = this.mIconRow.getChildCount() - 1; i >= 0; i--) {
                View child = this.mIconRow.getChildAt(i);
                if (!notifications.contains(((NotificationInfo) child.getTag()).notificationKey)) {
                    removeViewFromIconRow(child);
                }
            }
        }
    }
}

package com.android.launcher3.notification;

import android.app.ActivityOptions;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.util.PackageUserKey;
import com.sec.android.app.launcher.R;

public class NotificationInfo implements OnClickListener {
    private static final String TAG = "NotificationInfo";
    public final boolean autoCancel;
    public final boolean dismissable;
    public final PendingIntent intent;
    private int mBadgeIcon;
    private int mIconColor;
    private Drawable mIconDrawable;
    private boolean mIsIconLarge;
    public final String notificationKey;
    public final PackageUserKey packageUserKey;
    public final CharSequence text;
    public final CharSequence title;

    public NotificationInfo(Context context, StatusBarNotification statusBarNotification) {
        boolean z = true;
        this.packageUserKey = PackageUserKey.fromNotification(statusBarNotification);
        this.notificationKey = statusBarNotification.getKey();
        Notification notification = statusBarNotification.getNotification();
        this.title = notification.extras.getCharSequence(NotificationCompat.EXTRA_TITLE);
        this.text = notification.extras.getCharSequence(NotificationCompat.EXTRA_TEXT);
        this.mBadgeIcon = notification.getBadgeIconType();
        Icon icon = this.mBadgeIcon == 1 ? null : notification.getLargeIcon();
        if (icon == null) {
            icon = notification.getSmallIcon();
            if (icon != null) {
                this.mIconDrawable = icon.loadDrawable(context);
            }
            this.mIconColor = statusBarNotification.getNotification().color;
            this.mIsIconLarge = false;
        } else {
            this.mIconDrawable = icon.loadDrawable(context);
            this.mIsIconLarge = true;
        }
        if (this.mIconDrawable == null) {
            this.mIconDrawable = new BitmapDrawable(context.getResources(), LauncherAppState.getInstance().getIconCache().getDefaultIcon(UserHandleCompat.fromUser(statusBarNotification.getUser())));
            this.mBadgeIcon = 0;
        }
        this.intent = notification.contentIntent;
        if (this.intent == null) {
            Log.d(TAG, " intent is null title = " + this.title);
        }
        this.autoCancel = (notification.flags & 16) != 0;
        if ((notification.flags & 2) != 0) {
            z = false;
        }
        this.dismissable = z;
    }

    public boolean shouldShowIconInBadge() {
        if (this.mIsIconLarge && this.mBadgeIcon == 2) {
            return true;
        }
        return !this.mIsIconLarge && this.mBadgeIcon == 1;
    }

    public Drawable getIconForBackground(Context context, int background) {
        if (this.mIsIconLarge) {
            return this.mIconDrawable;
        }
        this.mIconColor = context.getColor(R.color.notification_icon_default_color);
        Drawable icon = this.mIconDrawable.mutate();
        icon.setTintList(null);
        icon.setTint(this.mIconColor);
        return icon;
    }

    public void onClick(View view) {
        Exception e;
        Launcher launcher = (Launcher) view.getContext();
        try {
            this.intent.send(null, 0, null, null, null, null, ActivityOptions.makeClipRevealAnimation(view, 0, 0, view.getWidth(), view.getHeight()).toBundle());
        } catch (CanceledException e2) {
            e = e2;
            Log.e(TAG, "exception " + e.toString());
            if (this.autoCancel) {
                launcher.getLauncherModel().cancelNotification(this.notificationKey);
            }
            if (launcher.getDragMgr().getQuickOptionView() == null) {
                launcher.getDragMgr().getQuickOptionView().remove(false);
            }
        } catch (NullPointerException e3) {
            e = e3;
            Log.e(TAG, "exception " + e.toString());
            if (this.autoCancel) {
                launcher.getLauncherModel().cancelNotification(this.notificationKey);
            }
            if (launcher.getDragMgr().getQuickOptionView() == null) {
                launcher.getDragMgr().getQuickOptionView().remove(false);
            }
        }
        if (this.autoCancel) {
            launcher.getLauncherModel().cancelNotification(this.notificationKey);
        }
        if (launcher.getDragMgr().getQuickOptionView() == null) {
            launcher.getDragMgr().getQuickOptionView().remove(false);
        }
    }
}

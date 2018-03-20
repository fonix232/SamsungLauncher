package com.android.launcher3.common.quickoption.notifications;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.notification.NotificationInfo;
import com.android.launcher3.util.ViInterpolator;
import com.sec.android.app.launcher.R;

public class NotificationMainView extends FrameLayout implements Callback {
    private final int NOTIFICATION_MAIN_VIEW_ALPHA_DURATION;
    private int mBackgroundColor;
    private NotificationInfo mNotificationInfo;
    private final Interpolator mSineInOut80;
    private ViewGroup mTextAndBackground;
    private TextView mTextView;
    private TextView mTitleView;

    public NotificationMainView(Context context) {
        this(context, null, 0);
    }

    public NotificationMainView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationMainView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.NOTIFICATION_MAIN_VIEW_ALPHA_DURATION = 183;
        this.mSineInOut80 = ViInterpolator.getInterploator(34);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mTextAndBackground = (ViewGroup) findViewById(R.id.text_and_background);
        ColorDrawable colorBackground = (ColorDrawable) this.mTextAndBackground.getBackground();
        this.mBackgroundColor = colorBackground.getColor();
        this.mTextAndBackground.setBackground(new RippleDrawable(ColorStateList.valueOf(getAttrColor(getContext(), 16843820)), colorBackground, null));
        this.mTitleView = (TextView) this.mTextAndBackground.findViewById(R.id.title);
        this.mTextView = (TextView) this.mTextAndBackground.findViewById(R.id.text);
    }

    private int getAttrColor(Context context, int attr) {
        TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
        int colorAccent = ta.getColor(0, 0);
        ta.recycle();
        return colorAccent;
    }

    public void applyNotificationInfo(NotificationInfo mainNotification, View iconView) {
        applyNotificationInfo(mainNotification, iconView, false, null);
    }

    public void applyNotificationInfo(NotificationInfo mainNotification, View iconView, boolean animate, final View itemView) {
        TextView textView;
        int i;
        int i2 = 5;
        this.mNotificationInfo = mainNotification;
        CharSequence title = this.mNotificationInfo.title;
        CharSequence text = this.mNotificationInfo.text;
        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(text)) {
            this.mTitleView.setMaxLines(2);
            textView = this.mTitleView;
            if (!TextUtils.isEmpty(title)) {
                text = title;
            }
            textView.setText(text);
            this.mTextView.setVisibility(View.GONE);
        } else {
            this.mTitleView.setText(title);
            this.mTextView.setText(text);
        }
        TextView textView2 = this.mTitleView;
        if (Utilities.sIsRtl) {
            i = 5;
        } else {
            i = 3;
        }
        textView2.setGravity(i);
        textView = this.mTextView;
        if (!Utilities.sIsRtl) {
            i2 = 3;
        }
        textView.setGravity(i2);
        iconView.setBackground(this.mNotificationInfo.getIconForBackground(getContext(), this.mBackgroundColor));
        if (this.mNotificationInfo.intent != null) {
            setOnClickListener(this.mNotificationInfo);
        }
        setTranslationX(0.0f);
        setTag(new ItemInfo());
        if (animate) {
            ObjectAnimator alpha = ObjectAnimator.ofFloat(this.mTextAndBackground, ALPHA, new float[]{0.0f, 1.0f}).setDuration(183);
            if (itemView != null) {
                alpha.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationEnd(Animator animation) {
                        itemView.getBackground().setTint(ContextCompat.getColor(NotificationMainView.this.getContext(), R.color.notification_color_beneath));
                    }
                });
            }
            alpha.setInterpolator(this.mSineInOut80);
            alpha.start();
        }
    }

    public NotificationInfo getNotificationInfo() {
        return this.mNotificationInfo;
    }

    public View getChildAtPosition(MotionEvent ev) {
        return this;
    }

    public boolean canChildBeDismissed(View v) {
        return this.mNotificationInfo != null && this.mNotificationInfo.dismissable;
    }

    public void onChildDismissed(View v) {
        ((Launcher) v.getContext()).getLauncherModel().cancelNotification(this.mNotificationInfo.notificationKey);
    }
}

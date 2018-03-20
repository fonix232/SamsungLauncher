package com.android.launcher3.common.quickoption;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;

public class AppShortcut extends FrameLayout implements OnClickListener, OnLongClickListener {
    private final int ANIM_DURATION;
    private final float END_ANIM_ALPHA;
    private final float END_ANIM_SCALE;
    private final float START_ANIM_ALPHA;
    private final float START_ANIM_SCALE;
    private final AnimatorListenerAdapter mAnimListenerAdapter;
    private ImageView mImageView;
    private boolean mIsPressedEnterKey;
    private Launcher mLauncher;
    private QuickOptionListItem mOptionItem;
    private TextView mTextView;

    public AppShortcut(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppShortcut(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.START_ANIM_SCALE = 0.3f;
        this.END_ANIM_SCALE = 1.0f;
        this.START_ANIM_ALPHA = 0.0f;
        this.END_ANIM_ALPHA = 1.0f;
        this.ANIM_DURATION = 333;
        this.mIsPressedEnterKey = false;
        this.mAnimListenerAdapter = new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animator) {
                AppShortcut.this.mImageView.setScaleX(0.3f);
                AppShortcut.this.mImageView.setScaleY(0.3f);
                AppShortcut.this.mImageView.setAlpha(0.0f);
                AppShortcut.this.mTextView.setAlpha(0.0f);
                AppShortcut.this.mImageView.setVisibility(View.VISIBLE);
                AppShortcut.this.mTextView.setVisibility(View.VISIBLE);
            }

            public void onAnimationEnd(Animator animator) {
                AppShortcut.this.mImageView.setScaleX(1.0f);
                AppShortcut.this.mImageView.setScaleY(1.0f);
                AppShortcut.this.mImageView.setAlpha(1.0f);
                AppShortcut.this.mTextView.setAlpha(1.0f);
            }
        };
    }

    public AppShortcut(Context context) {
        this(context, null, 0);
    }

    void setItem(Launcher launcher, QuickOptionListItem option) {
        this.mLauncher = launcher;
        this.mOptionItem = option;
        this.mTextView = (TextView) findViewById(R.id.app_shorcut_title);
        this.mImageView = (ImageView) findViewById(R.id.app_shorcut_icon);
        this.mTextView.setText(option.getTitle());
        this.mImageView.setImageDrawable(option.getIcon());
        this.mTextView.setSelected(true);
        this.mTextView.setVisibility(View.GONE);
        this.mImageView.setVisibility(View.GONE);
    }

    AnimatorSet getItemAnim() {
        AnimatorSet itemAnim = new AnimatorSet();
        ObjectAnimator iconScaleXAnim = ObjectAnimator.ofFloat(this.mImageView, View.SCALE_X.getName(), new float[]{0.3f, 1.0f});
        ObjectAnimator iconScaleYAnim = ObjectAnimator.ofFloat(this.mImageView, View.SCALE_Y.getName(), new float[]{0.3f, 1.0f});
        ObjectAnimator iconAlphaAnim = ObjectAnimator.ofFloat(this.mImageView, View.ALPHA.getName(), new float[]{0.0f, 1.0f});
        ObjectAnimator textAlphaAnim = ObjectAnimator.ofFloat(this.mTextView, View.ALPHA.getName(), new float[]{0.0f, 1.0f});
        iconScaleXAnim.setDuration(333);
        iconScaleYAnim.setDuration(333);
        iconAlphaAnim.setDuration(333);
        textAlphaAnim.setDuration(333);
        iconScaleXAnim.setInterpolator(ViInterpolator.getInterploator(34));
        iconScaleYAnim.setInterpolator(ViInterpolator.getInterploator(34));
        iconAlphaAnim.setInterpolator(ViInterpolator.getInterploator(34));
        textAlphaAnim.setInterpolator(ViInterpolator.getInterploator(34));
        itemAnim.addListener(this.mAnimListenerAdapter);
        itemAnim.playTogether(new Animator[]{iconScaleXAnim, iconScaleYAnim, iconAlphaAnim, textAlphaAnim});
        return itemAnim;
    }

    public void onClick(View view) {
        insertSALoggingEvent(false);
        LauncherAppState.getInstance().getShortcutManager().startShortcut(this.mOptionItem.getShortcutKey());
        this.mLauncher.getDragMgr().removeQuickOptionView();
    }

    @TargetApi(25)
    public boolean onLongClick(View v) {
        if (this.mIsPressedEnterKey) {
            return false;
        }
        insertSALoggingEvent(true);
        return ((QuickOptionView) getParent().getParent()).onItemLongClick(LauncherAppState.getInstance().getShortcutManager().queryForShortcutKey(this.mOptionItem.getShortcutKey()));
    }

    private void insertSALoggingEvent(boolean isLongClick) {
        if (this.mOptionItem.getShortcutKey() != null && this.mOptionItem.getShortcutKey().componentName != null) {
            if (isLongClick) {
                SALogging.getInstance().insertAppShortcutPinningStartEventLog(this.mOptionItem.getShortcutKey().getId(), this.mOptionItem.getShortcutKey().componentName.getPackageName());
            } else {
                SALogging.getInstance().insertAppShortcutEventLog(this.mOptionItem.getShortcutKey().getId(), this.mOptionItem.getShortcutKey().componentName.getPackageName(), this.mLauncher);
            }
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isKeyCodeEnter(keyCode)) {
            this.mIsPressedEnterKey = true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isKeyCodeEnter(keyCode)) {
            this.mIsPressedEnterKey = false;
        }
        return super.onKeyUp(keyCode, event);
    }

    private boolean isKeyCodeEnter(int keyCode) {
        return keyCode == 23 || keyCode == 66;
    }
}

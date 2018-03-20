package com.android.launcher3.common.quickoption;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;

public class GlobalOption extends FrameLayout implements OnClickListener {
    private int mIconSize;
    private Launcher mLauncher;
    private QuickOptionListItem mOptionItem;

    public GlobalOption(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GlobalOption(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public GlobalOption(Context context) {
        this(context, null, 0);
    }

    void setItem(Launcher launcher, QuickOptionListItem option, boolean isWidget) {
        this.mLauncher = launcher;
        this.mOptionItem = option;
        this.mIconSize = getResources().getDimensionPixelSize(R.dimen.quick_options_icon_size);
        TextView textView = (TextView) findViewById(R.id.global_icon_text);
        ImageView imageView = (ImageView) findViewById(R.id.global_icon_image);
        textView.setText(option.getTitleRsrId());
        imageView.setImageDrawable(getResources().getDrawable(option.getIconRsrId(), null));
        if (isWidget) {
            textView.setSingleLine();
        }
        if (option.getTitleRsrId() == R.string.quick_option_dimmed_disable) {
            textView.setAlpha(0.4f);
            imageView.setAlpha(0.4f);
        }
        if (option.getTtsTitleRsrId() > 0) {
            setContentDescription(getResources().getString(option.getTtsTitleRsrId()));
        }
    }

    int getIconSize() {
        return this.mIconSize;
    }

    public void onClick(View view) {
        if (this.mOptionItem.getCallback() != null) {
            SALogging.getInstance().insertQOEventLog(this.mOptionItem.getTitleRsrId(), this.mLauncher);
            if (this.mOptionItem.isOptionRemove()) {
                final ItemRemoveAnimation itemRemoveAnimation = this.mLauncher.getQuickOptionManager().createItemRemoveAnimation();
                if (itemRemoveAnimation != null) {
                    itemRemoveAnimation.getAnimatorSet().addListener(new AnimatorListener() {
                        public void onAnimationStart(Animator animator) {
                        }

                        public void onAnimationRepeat(Animator animator) {
                        }

                        public void onAnimationEnd(Animator animator) {
                            if (!itemRemoveAnimation.hasCanceled()) {
                                GlobalOption.this.mOptionItem.getCallback().run();
                            }
                        }

                        public void onAnimationCancel(Animator animator) {
                        }
                    });
                    this.mLauncher.getDragMgr().removeQuickOptionView("2");
                    return;
                }
            }
            this.mOptionItem.getCallback().run();
        }
        this.mLauncher.getDragMgr().removeQuickOptionView();
    }
}

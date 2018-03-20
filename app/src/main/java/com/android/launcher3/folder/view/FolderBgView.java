package com.android.launcher3.folder.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.LightingEffectManager;
import com.sec.android.app.launcher.R;

public class FolderBgView extends FrameLayout {
    private LinearLayout mHelpContainer;
    private TextView mHelpText;

    public FolderBgView(Context context) {
        this(context, null);
    }

    public FolderBgView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FolderBgView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mHelpContainer = (LinearLayout) findViewById(R.id.folder_bg_help_container);
        this.mHelpText = (TextView) findViewById(R.id.folder_bg_help_text);
    }

    public void showHelpForEdit(boolean toBeShown, int animDuration, boolean withLighting) {
        float toAlpha = toBeShown ? 1.0f : 0.0f;
        if (animDuration > 0) {
            this.mHelpContainer.animate().alpha(toAlpha).setDuration((long) animDuration).start();
        } else {
            this.mHelpContainer.setAlpha(toAlpha);
        }
        if (withLighting) {
            LightingEffectManager.INSTANCE.showEffect(toBeShown, animDuration, Utilities.getOrientation() == 2);
        }
    }

    public void setHelpTextContainerHeightAndGravity(int height, int gravity) {
        LayoutParams lp = (LayoutParams) this.mHelpContainer.getLayoutParams();
        if (lp != null && height >= 0) {
            lp.height = height;
        }
        this.mHelpContainer.setGravity(gravity);
    }

    public void setHelpTextColor(boolean isWhiteBg) {
        int bgColor;
        if (isWhiteBg) {
            bgColor = getResources().getColor(R.color.apps_picker_black_color, null);
        } else {
            bgColor = getResources().getColor(R.color.apps_picker_white_color, null);
        }
        this.mHelpText.setTextColor(bgColor);
    }

    public void onMoveInFolder() {
        LightingEffectManager.INSTANCE.turnOffAllLight();
    }

    public void onMoveFromFolderTop() {
        LightingEffectManager.INSTANCE.turnOnEachLight(0, true);
    }

    public void onMoveFromFolderBottom() {
        LightingEffectManager.INSTANCE.turnOnEachLight(1, true);
    }
}

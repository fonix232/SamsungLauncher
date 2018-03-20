package com.android.launcher3.common.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.deviceprofile.GridIconInfo;
import com.android.launcher3.theme.OpenThemeManager;
import com.android.launcher3.theme.OpenThemeManager.ThemeItems;
import com.android.launcher3.util.WhiteBgManager;
import com.sec.android.app.launcher.R;

public class PairAppsIconView extends IconView {
    private boolean mNeedToMultiLine = false;
    protected TextView mTitleViewSecond;

    public PairAppsIconView(Context context) {
        super(context);
    }

    public PairAppsIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PairAppsIconView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void onFinishInflate() {
        this.mTitleViewSecond = (TextView) findViewById(R.id.iconview_titleView_second);
        super.onFinishInflate();
        if (Utilities.canScreenRotate()) {
            this.mTitleViewSecond.setGravity(this.mIsPhoneLandscape ? 51 : 49);
        }
        this.mBadgeView = null;
    }

    public void animateTitleView(boolean visible, int duration, long delay, boolean animated) {
        super.animateTitleView(visible, duration, delay, animated);
        animateEachScale(this.mTitleViewSecond, visible, duration, delay, animated);
    }

    public void applyStyle() {
        super.applyStyle();
        GridIconInfo iconInfo = getIconInfo();
        if (iconInfo != null) {
            this.mNeedToMultiLine = iconInfo.getLineCount() > 1;
            this.mTitleView.setTextSize(0, (float) iconInfo.getTextSize());
            this.mTitleView.setMaxLines(1);
            this.mTitleViewSecond.setTextSize(0, (float) iconInfo.getTextSize());
            this.mTitleViewSecond.setMaxLines(1);
            if (Utilities.canScreenRotate()) {
                int i;
                TextView textView = this.mTitleViewSecond;
                if (this.mIsPhoneLandscape) {
                    i = 51;
                } else {
                    i = 49;
                }
                textView.setGravity(i);
            }
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        GridIconInfo iconInfo = getIconInfo();
        if (iconInfo != null) {
            applyTitleView(iconInfo.getContentTop(), iconInfo.getIconStartPadding(), (LayoutParams) this.mTitleView.getLayoutParams());
            super.onLayout(changed, left, top, right, bottom);
        }
    }

    protected void applyStyleTitleView(int contentTop, int leftMargin, LayoutParams lp) {
        if (lp != null) {
            if (this.mIsPhoneLandscape) {
                lp.gravity = 51;
                lp.leftMargin = (this.mIconSize + leftMargin) + this.mDrawablePadding;
                lp.topMargin = 0;
            } else {
                lp.gravity = 49;
                lp.topMargin = (this.mIconSize + contentTop) + this.mDrawablePadding;
                lp.leftMargin = 0;
            }
            LayoutParams lpTitleView = (LayoutParams) this.mTitleViewSecond.getLayoutParams();
            if (lpTitleView == null) {
                return;
            }
            if (this.mIsPhoneLandscape) {
                lpTitleView.gravity = 51;
                lpTitleView.leftMargin = (this.mIconSize + leftMargin) + this.mDrawablePadding;
                lpTitleView.topMargin = lp.topMargin + this.mTitleView.getLineHeight();
                return;
            }
            lpTitleView.gravity = 49;
            lpTitleView.topMargin = ((this.mIconSize + contentTop) + this.mDrawablePadding) + this.mTitleView.getLineHeight();
            lpTitleView.leftMargin = 0;
        }
    }

    private void applyTitleView(int contentTop, int leftMargin, LayoutParams lp) {
        if (lp != null) {
            if (this.mIsPhoneLandscape) {
                lp.gravity = 51;
                lp.leftMargin = (this.mIconSize + leftMargin) + this.mDrawablePadding;
                lp.topMargin = (getHeight() - (this.mTitleView.getLineHeight() + this.mTitleView.getMeasuredHeight())) / 2;
            } else {
                lp.gravity = 49;
                lp.topMargin = (this.mIconSize + contentTop) + this.mDrawablePadding;
                lp.leftMargin = 0;
            }
            LayoutParams lpTitleView = (LayoutParams) this.mTitleViewSecond.getLayoutParams();
            if (lpTitleView == null) {
                return;
            }
            if (this.mIsPhoneLandscape) {
                lpTitleView.gravity = 51;
                lpTitleView.leftMargin = (this.mIconSize + leftMargin) + this.mDrawablePadding;
                lpTitleView.topMargin = lp.topMargin + this.mTitleView.getLineHeight();
                return;
            }
            lpTitleView.gravity = 49;
            lpTitleView.topMargin = ((this.mIconSize + contentTop) + this.mDrawablePadding) + this.mTitleView.getLineHeight();
            lpTitleView.leftMargin = 0;
        }
    }

    void refreshIcon(IconInfo info, boolean promiseStateChanged, Bitmap b) {
        super.refreshIcon(info, promiseStateChanged, b);
        setText(info.title);
        setTag(info);
    }

    public void setText(CharSequence text) {
        String[] pairAppsLabel = Utilities.trim(text).split("\n");
        if (this.mNeedToMultiLine) {
            if (!(this.mTitleView == null || this.mTitleView.getText().equals(pairAppsLabel[0]))) {
                this.mTitleView.setText(pairAppsLabel[0]);
            }
            if (this.mTitleViewSecond != null && !this.mTitleViewSecond.getText().equals(pairAppsLabel[1])) {
                this.mTitleViewSecond.setText(pairAppsLabel[1]);
                return;
            }
            return;
        }
        this.mTitleView.setText(pairAppsLabel[0] + "/" + pairAppsLabel[1]);
        this.mTitleViewSecond.setText(null);
    }

    protected void decorateViewComponent() {
        super.decorateViewComponent();
        OpenThemeManager themeManager = OpenThemeManager.getInstance();
        int titleHighlightColor = themeManager.getPreloadColor(ThemeItems.TEXT_HIGHLIGHT.value());
        if ((themeManager.isPinkTheme() || !WhiteBgManager.isWhiteBg() || this.mIconTextBackground != null) && titleHighlightColor != 33554431) {
            this.mTitleViewSecond.setHighlightColor(titleHighlightColor);
        }
    }

    public void drawTextBackground(Canvas canvas) {
        if (this.mIconTextBackground != null && this.mTitleView.getText() != null && this.mTitleView.getText().length() > 0 && this.mTitleViewSecond.getText() != null) {
            int width;
            int top;
            int bottom;
            int startMargin;
            int extraPadding = OpenThemeManager.getInstance().mTextBackgroundExtraPadding;
            int extraPaddingBottom = OpenThemeManager.getInstance().mTextBackgroundExtraPaddingBottom;
            int textLineHeight = this.mTitleView.getLineHeight();
            int textLineCount = this.mTitleViewSecond.getText().length() > 0 ? 2 : 1;
            LayoutParams lp = (LayoutParams) this.mIconView.getLayoutParams();
            int titleViewWidth = (int) this.mTitleView.getLayout().getLineWidth(0);
            int titleViewSecondWidth = (int) this.mTitleViewSecond.getLayout().getLineWidth(0);
            if (titleViewWidth >= titleViewSecondWidth) {
                width = titleViewWidth;
            } else {
                width = titleViewSecondWidth;
            }
            width += extraPadding;
            if (this.mIsPhoneLandscape) {
                top = ((getHeight() - (textLineCount * textLineHeight)) - extraPadding) / 2;
                bottom = (getHeight() - top) + (extraPadding / 2);
                startMargin = ((lp.leftMargin + this.mIconSize) + this.mDrawablePadding) - (extraPadding / 2);
                if (getWidth() < startMargin + width) {
                    width = getWidth() - startMargin;
                }
            } else {
                top = ((lp.topMargin + this.mIconSize) + this.mTitleView.getExtendedPaddingTop()) + this.mDrawablePadding;
                bottom = ((textLineCount * textLineHeight) + top) + extraPaddingBottom;
                startMargin = (getWidth() - width) / 2;
                if (getWidth() < width) {
                    width = getWidth();
                    startMargin = 0;
                }
                if (bottom > getHeight()) {
                    bottom = getHeight();
                }
            }
            this.mIconTextBackground.setBounds(startMargin, top, startMargin + width, bottom);
            this.mIconTextBackground.draw(canvas);
        }
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (this.mTitleViewSecond.getCurrentTextColor() == getResources().getColor(17170445, null)) {
            this.mTitleViewSecond.getPaint().clearShadowLayer();
            super.draw(canvas);
        }
    }

    public String getTitle() {
        return this.mTitleView.getText().toString() + "\n" + this.mTitleViewSecond.getText().toString();
    }

    public void setShadowLayer(float radius, float dx, float dy, int color) {
        super.setShadowLayer(radius, dx, dy, color);
        this.mTitleViewSecond.setShadowLayer(radius, dx, dy, color);
    }

    public void setTextColor(ColorStateList colors) {
        this.mTitleViewSecond.setTextColor(colors);
    }

    public void setTextColor(int color) {
        super.setTextColor(color);
        this.mTitleViewSecond.setTextColor(color);
    }

    public void setTitleViewVisibility(int visibility) {
        super.setTitleViewVisibility(visibility);
        this.mTitleViewSecond.setVisibility(visibility);
    }
}

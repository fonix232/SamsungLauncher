package com.android.launcher3.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import com.android.launcher3.Launcher;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.drawable.FastBitmapDrawable;
import com.android.launcher3.common.drawable.PreloadIconDrawable;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.util.BitmapUtils;
import com.sec.android.app.launcher.R;

public class PendingAppWidgetHostView extends LauncherAppWidgetHostView implements OnClickListener {
    private static final float MIN_SATUNATION = 0.7f;
    private static final float SETUP_ICON_SIZE_FACTOR = 0.4f;
    private Drawable mCenterDrawable;
    private OnClickListener mClickListener;
    private View mDefaultView;
    private final boolean mDisabledForSafeMode;
    private boolean mDrawableSizeChanged;
    private Bitmap mIcon;
    private final Intent mIconLookupIntent;
    private final LauncherAppWidgetInfo mInfo;
    private Launcher mLauncher;
    private final TextPaint mPaint;
    private final Rect mRect = new Rect();
    private Drawable mSettingIconDrawable;
    private Layout mSetupTextLayout;
    private final int mStartState;

    PendingAppWidgetHostView(Context context, LauncherAppWidgetInfo info, boolean disabledForSafeMode) {
        super(context);
        this.mLauncher = (Launcher) context;
        this.mInfo = info;
        this.mStartState = info.restoreStatus;
        this.mIconLookupIntent = new Intent().setComponent(info.providerName);
        this.mDisabledForSafeMode = disabledForSafeMode;
        this.mPaint = new TextPaint();
        this.mPaint.setColor(-1);
        this.mPaint.setTextSize(TypedValue.applyDimension(0, (float) this.mLauncher.getDeviceProfile().homeGrid.getTextSize(), getResources().getDisplayMetrics()));
        setBackgroundResource(R.drawable.quantum_panel_dark);
        setWillNotDraw(false);
        setElevation(getResources().getDimension(R.dimen.pending_widget_elevation));
    }

    public void updateAppWidgetSize(Bundle newOptions, int minWidth, int minHeight, int maxWidth, int maxHeight) {
    }

    protected View getDefaultView() {
        if (this.mDefaultView == null) {
            this.mDefaultView = this.mInflater.inflate(R.layout.appwidget_not_ready, this, false);
            this.mDefaultView.setOnClickListener(this);
            applyState();
        }
        return this.mDefaultView;
    }

    public void setOnClickListener(OnClickListener l) {
        this.mClickListener = l;
    }

    boolean isReinflateRequired() {
        return this.mStartState != this.mInfo.restoreStatus;
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mDrawableSizeChanged = true;
    }

    void updateIcon(IconCache cache) {
        Bitmap icon = cache.getIcon(this.mIconLookupIntent, this.mInfo.user);
        if (this.mIcon != icon) {
            this.mIcon = icon;
            if (this.mCenterDrawable != null) {
                this.mCenterDrawable.setCallback(null);
                this.mCenterDrawable = null;
            }
            if (this.mIcon != null) {
                int iconSize = this.mLauncher.getDeviceProfile().homeGrid.getIconSize();
                if (this.mDisabledForSafeMode) {
                    FastBitmapDrawable disabledIcon = BitmapUtils.createIconDrawable(this.mIcon, iconSize);
                    disabledIcon.setGhostModeEnabled(true);
                    this.mCenterDrawable = disabledIcon;
                    this.mSettingIconDrawable = null;
                } else if (isReadyForClickSetup()) {
                    this.mCenterDrawable = new FastBitmapDrawable(this.mIcon);
                    this.mSettingIconDrawable = getResources().getDrawable(R.drawable.homescreen_ic_reorder_setting).mutate();
                    updateSettingColor();
                } else {
                    this.mCenterDrawable = new PreloadIconDrawable(BitmapUtils.createIconDrawable(this.mIcon, iconSize));
                    this.mCenterDrawable.setCallback(this);
                    this.mSettingIconDrawable = null;
                    applyState();
                }
                this.mDrawableSizeChanged = true;
            }
        }
    }

    private void updateSettingColor() {
        hsv = new float[3];
        Color.colorToHSV(BitmapUtils.findDominantColorByHue(this.mIcon, 20), hsv);
        hsv[1] = Math.min(hsv[1], MIN_SATUNATION);
        hsv[2] = 1.0f;
        this.mSettingIconDrawable.setColorFilter(Color.HSVToColor(hsv), Mode.SRC_IN);
    }

    protected boolean verifyDrawable(Drawable who) {
        return who == this.mCenterDrawable || super.verifyDrawable(who);
    }

    void applyState() {
        if (this.mCenterDrawable != null) {
            this.mCenterDrawable.setLevel(Math.max(this.mInfo.installProgress, 0));
        }
    }

    public void onClick(View v) {
        if (this.mClickListener != null) {
            this.mClickListener.onClick(this);
        }
    }

    boolean isReadyForClickSetup() {
        return (this.mInfo.restoreStatus & 2) == 0 && (this.mInfo.restoreStatus & 4) != 0;
    }

    private void updateDrawableBounds() {
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int minPadding = getResources().getDimensionPixelSize(R.dimen.pending_widget_min_padding);
        int availableWidth = ((getWidth() - paddingLeft) - paddingRight) - (minPadding * 2);
        int availableHeight = ((getHeight() - paddingTop) - paddingBottom) - (minPadding * 2);
        if (this.mSettingIconDrawable == null) {
            int size = Math.min(grid.homeGrid.getIconSize(), Math.min(availableWidth, availableHeight));
            this.mRect.set(0, 0, size, size);
            this.mRect.offsetTo((getWidth() - this.mRect.width()) / 2, (getHeight() - this.mRect.height()) / 2);
            this.mCenterDrawable.setBounds(this.mRect);
            return;
        }
        int iconTop;
        float iconSize = (float) Math.min(availableWidth, availableHeight);
        int maxSize = Math.max(availableWidth, availableHeight);
        if (iconSize * 1.8f > ((float) maxSize)) {
            iconSize = ((float) maxSize) / 1.8f;
        }
        int actualIconSize = (int) Math.min(iconSize, (float) grid.homeGrid.getIconSize());
        this.mSetupTextLayout = new StaticLayout(getResources().getText(R.string.gadget_setup_text), this.mPaint, availableWidth, Alignment.ALIGN_CENTER, 1.0f, 0.0f, true);
        int textHeight = this.mSetupTextLayout.getHeight();
        if ((((float) textHeight) + (((float) actualIconSize) * 1.8f)) + ((float) grid.homeGrid.getDrawablePadding()) < ((float) availableHeight)) {
            iconTop = (((getHeight() - textHeight) - grid.homeGrid.getDrawablePadding()) - actualIconSize) / 2;
        } else {
            iconTop = (getHeight() - actualIconSize) / 2;
            this.mSetupTextLayout = null;
        }
        this.mRect.set(0, 0, actualIconSize, actualIconSize);
        this.mRect.offset((getWidth() - actualIconSize) / 2, iconTop);
        this.mCenterDrawable.setBounds(this.mRect);
        this.mRect.left = paddingLeft + minPadding;
        this.mRect.right = this.mRect.left + ((int) (SETUP_ICON_SIZE_FACTOR * ((float) actualIconSize)));
        this.mRect.top = paddingTop + minPadding;
        this.mRect.bottom = this.mRect.top + ((int) (SETUP_ICON_SIZE_FACTOR * ((float) actualIconSize)));
        this.mSettingIconDrawable.setBounds(this.mRect);
        if (this.mSetupTextLayout != null) {
            this.mRect.left = paddingLeft + minPadding;
            this.mRect.top = this.mCenterDrawable.getBounds().bottom + grid.homeGrid.getDrawablePadding();
        }
    }

    protected void onDraw(Canvas canvas) {
        if (this.mCenterDrawable != null) {
            if (this.mDrawableSizeChanged) {
                updateDrawableBounds();
                this.mDrawableSizeChanged = false;
            }
            this.mCenterDrawable.draw(canvas);
            if (this.mSettingIconDrawable != null) {
                this.mSettingIconDrawable.draw(canvas);
            }
            if (this.mSetupTextLayout != null) {
                canvas.save();
                canvas.translate((float) this.mRect.left, (float) this.mRect.top);
                this.mSetupTextLayout.draw(canvas);
                canvas.restore();
            }
        }
    }
}

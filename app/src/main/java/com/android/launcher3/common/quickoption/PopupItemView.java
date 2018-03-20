package com.android.launcher3.common.quickoption;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View.OnKeyListener;
import android.widget.LinearLayout;
import com.sec.android.app.launcher.R;

public class PopupItemView extends LinearLayout {
    private final Paint mBackgroundClipPaint;
    protected OnKeyListener mKeyListener;
    private final Matrix mMatrix;
    protected int mPopupHeight;
    private final int mPopupWidth;
    private final Bitmap mRoundedCornerBitmap;

    public PopupItemView(Context context) {
        this(context, null, 0);
    }

    public PopupItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PopupItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mBackgroundClipPaint = new Paint(5);
        this.mMatrix = new Matrix();
        this.mPopupWidth = getResources().getDimensionPixelSize(R.dimen.quick_option_item_width);
        int radius = getResources().getDimensionPixelSize(R.dimen.quick_option_popup_radius);
        this.mRoundedCornerBitmap = Bitmap.createBitmap(radius, radius, Config.ALPHA_8);
        Canvas canvas = new Canvas();
        canvas.setBitmap(this.mRoundedCornerBitmap);
        canvas.drawArc(0.0f, 0.0f, (float) (radius * 2), (float) (radius * 2), 180.0f, 90.0f, true, this.mBackgroundClipPaint);
        this.mBackgroundClipPaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
    }

    protected void dispatchDraw(Canvas canvas) {
        int saveCount = canvas.saveLayer(0.0f, 0.0f, (float) getWidth(), (float) getHeight(), null);
        super.dispatchDraw(canvas);
        float cornerWidth = (float) this.mRoundedCornerBitmap.getWidth();
        float cornerHeight = (float) this.mRoundedCornerBitmap.getHeight();
        this.mMatrix.reset();
        canvas.drawBitmap(this.mRoundedCornerBitmap, this.mMatrix, this.mBackgroundClipPaint);
        this.mMatrix.setRotate(90.0f, cornerWidth / 2.0f, cornerHeight / 2.0f);
        this.mMatrix.postTranslate(((float) canvas.getWidth()) - cornerWidth, 0.0f);
        canvas.drawBitmap(this.mRoundedCornerBitmap, this.mMatrix, this.mBackgroundClipPaint);
        this.mMatrix.setRotate(180.0f, cornerWidth / 2.0f, cornerHeight / 2.0f);
        this.mMatrix.postTranslate(((float) canvas.getWidth()) - cornerWidth, ((float) canvas.getHeight()) - cornerHeight);
        canvas.drawBitmap(this.mRoundedCornerBitmap, this.mMatrix, this.mBackgroundClipPaint);
        this.mMatrix.setRotate(270.0f, cornerWidth / 2.0f, cornerHeight / 2.0f);
        this.mMatrix.postTranslate(0.0f, ((float) canvas.getHeight()) - cornerHeight);
        canvas.drawBitmap(this.mRoundedCornerBitmap, this.mMatrix, this.mBackgroundClipPaint);
        canvas.restoreToCount(saveCount);
    }

    public int getPopupHeight() {
        return this.mPopupHeight;
    }

    public int getPopupWidth() {
        return this.mPopupWidth;
    }

    protected void setPopupHeight(int height) {
        this.mPopupHeight = height;
    }

    public void setOnQuickOptionKeyListener(OnKeyListener keyListener) {
        this.mKeyListener = keyListener;
    }
}

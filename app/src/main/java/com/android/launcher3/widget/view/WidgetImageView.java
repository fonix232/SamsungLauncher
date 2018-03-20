package com.android.launcher3.widget.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class WidgetImageView extends View {
    private Bitmap mBitmap;
    private final RectF mDstRectF = new RectF();
    private final Paint mPaint = new Paint(3);

    public WidgetImageView(Context context) {
        super(context);
    }

    public WidgetImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WidgetImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
        invalidate();
    }

    public Bitmap getBitmap() {
        return this.mBitmap;
    }

    protected void onDraw(Canvas canvas) {
        if (this.mBitmap != null) {
            updateDstRectF();
            canvas.drawBitmap(this.mBitmap, null, this.mDstRectF, this.mPaint);
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    private void updateDstRectF() {
        if (this.mBitmap.getWidth() > getWidth()) {
            this.mDstRectF.set(0.0f, 0.0f, (float) getWidth(), ((float) this.mBitmap.getHeight()) * (((float) getWidth()) / ((float) this.mBitmap.getWidth())));
            return;
        }
        this.mDstRectF.set(((float) (getWidth() - this.mBitmap.getWidth())) * 0.5f, 0.0f, ((float) (getWidth() + this.mBitmap.getWidth())) * 0.5f, (float) this.mBitmap.getHeight());
    }

    public Rect getBitmapBounds() {
        updateDstRectF();
        Rect rect = new Rect();
        this.mDstRectF.round(rect);
        return rect;
    }
}

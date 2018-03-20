package com.android.launcher3.widget.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;

public class LivePreviewWidgetCell extends WidgetCell {
    private RemoteViews mPreview;

    public LivePreviewWidgetCell(Context context) {
        this(context, null);
    }

    public LivePreviewWidgetCell(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LivePreviewWidgetCell(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setPreview(RemoteViews view) {
        this.mPreview = view;
    }

    public void ensurePreview() {
        if (this.mPreview != null && this.mActiveRequest == null && (this.mInfo instanceof LauncherAppWidgetProviderInfo)) {
            Bitmap preview = generateFromRemoteViews(this.mContext, this.mPreview, (LauncherAppWidgetProviderInfo) this.mInfo, this.mPresetPreviewSize, new int[1]);
            if (preview != null) {
                applyPreview(this.mInfo, preview);
                return;
            }
        }
        super.ensurePreview();
    }

    public static Bitmap generateFromRemoteViews(Context activity, RemoteViews views, LauncherAppWidgetProviderInfo info, int previewSize, int[] preScaledWidthOut) {
        DeviceProfile dp = LauncherAppState.getInstance().getDeviceProfile();
        int viewWidth = dp.defaultCellWidth * info.getSpanX();
        int viewHeight = dp.defaultCellHeight * info.getSpanY();
        try {
            float scale;
            int bitmapWidth;
            int bitmapHeight;
            View v = views.apply(activity, new FrameLayout(activity));
            v.measure(MeasureSpec.makeMeasureSpec(viewWidth, 1073741824), MeasureSpec.makeMeasureSpec(viewHeight, 1073741824));
            viewWidth = v.getMeasuredWidth();
            viewHeight = v.getMeasuredHeight();
            v.layout(0, 0, viewWidth, viewHeight);
            preScaledWidthOut[0] = viewWidth;
            if (viewWidth > previewSize) {
                scale = ((float) previewSize) / ((float) viewWidth);
                bitmapWidth = previewSize;
                bitmapHeight = (int) (((float) viewHeight) * scale);
            } else {
                scale = 1.0f;
                bitmapWidth = viewWidth;
                bitmapHeight = viewHeight;
            }
            Bitmap preview = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Config.ARGB_8888);
            Canvas c = new Canvas(preview);
            c.scale(scale, scale);
            v.draw(c);
            c.setBitmap(null);
            return preview;
        } catch (Exception e) {
            return null;
        }
    }
}

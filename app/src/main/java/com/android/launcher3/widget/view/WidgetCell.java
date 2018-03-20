package com.android.launcher3.widget.view;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.common.compat.AppWidgetManagerCompat;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.common.deviceprofile.DeviceProfile;
import com.android.launcher3.common.model.IconCache;
import com.android.launcher3.common.model.LauncherAppWidgetProviderInfo;
import com.android.launcher3.util.BitmapUtils;
import com.android.launcher3.util.event.StylusEventHelper;
import com.android.launcher3.widget.PendingAddPinShortcutInfo;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.android.launcher3.widget.PinShortcutRequestActivityInfo;
import com.android.launcher3.widget.model.WidgetPreviewLoader;
import com.android.launcher3.widget.model.WidgetPreviewLoader.PreviewLoadRequest;
import com.sec.android.app.launcher.R;

public class WidgetCell extends LinearLayout implements OnLayoutChangeListener, PreviewLoadListener {
    private static final boolean DEBUG = false;
    private static final int FADE_IN_DURATION_MS = 90;
    private static final float PREVIEW_SCALE = 0.8f;
    private static final String TAG = "WidgetCell";
    private static final float WIDTH_SCALE = 2.6f;
    public int cellSize;
    protected PreviewLoadRequest mActiveRequest;
    protected Context mContext;
    private String mDimensionsFormatString;
    protected Object mInfo;
    protected int mPresetPreviewSize;
    private StylusEventHelper mStylusEventHelper;
    private TextView mWidgetDims;
    private WidgetImageView mWidgetImage;
    private TextView mWidgetName;
    private WidgetPreviewLoader mWidgetPreviewLoader;

    public WidgetCell(Context context) {
        this(context, null);
    }

    public WidgetCell(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetCell(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Resources r = context.getResources();
        this.mContext = context;
        this.mStylusEventHelper = new StylusEventHelper(this);
        this.mDimensionsFormatString = r.getString(R.string.widget_dims_format);
        setContainerWidth();
        setWillNotDraw(false);
        setClipToPadding(false);
    }

    private void setContainerWidth() {
        this.cellSize = (int) (((float) LauncherAppState.getInstance().getDeviceProfile().homeGrid.getCellWidth()) * WIDTH_SCALE);
        this.mPresetPreviewSize = (int) (((float) this.cellSize) * PREVIEW_SCALE);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mWidgetImage = (WidgetImageView) findViewById(R.id.widget_preview);
        this.mWidgetName = (TextView) findViewById(R.id.widget_name);
        this.mWidgetDims = (TextView) findViewById(R.id.widget_dims);
    }

    public void clear() {
        this.mWidgetImage.animate().cancel();
        this.mWidgetImage.setBitmap(null);
        this.mWidgetName.setText(null);
        this.mWidgetDims.setText(null);
        if (this.mActiveRequest != null) {
            this.mActiveRequest.cleanup();
            this.mActiveRequest = null;
        }
    }

    public void applyFromAppWidgetProviderInfo(LauncherAppWidgetProviderInfo info, WidgetPreviewLoader loader) {
        DeviceProfile profile = LauncherAppState.getInstance().getDeviceProfile();
        this.mInfo = info;
        this.mWidgetName.setText(AppWidgetManagerCompat.getInstance(getContext()).loadLabel(info));
        int hSpan = Math.min(info.getSpanX(), profile.homeGrid.getCellCountX());
        int vSpan = Math.min(info.getSpanY(), profile.homeGrid.getCellCountY());
        this.mWidgetDims.setText(String.format(this.mDimensionsFormatString, new Object[]{Integer.valueOf(hSpan), Integer.valueOf(vSpan)}));
        this.mWidgetPreviewLoader = loader;
    }

    public void applyFromResolveInfo(PackageManager pm, ResolveInfo info, WidgetPreviewLoader loader) {
        this.mInfo = info;
        this.mWidgetName.setText(info.loadLabel(pm));
        this.mWidgetDims.setText(String.format(this.mDimensionsFormatString, new Object[]{Integer.valueOf(1), Integer.valueOf(1)}));
        this.mWidgetPreviewLoader = loader;
    }

    public int[] getPreviewSize() {
        return new int[]{this.mPresetPreviewSize, this.mPresetPreviewSize};
    }

    public void applyPreview(Object info, Bitmap bitmap) {
        if (bitmap != null) {
            this.mWidgetImage.setBitmap(bitmap);
            this.mWidgetImage.setAlpha(0.0f);
            this.mWidgetImage.animate().alpha(1.0f).setDuration(90);
        }
    }

    public void ensurePreview() {
        if (this.mActiveRequest == null) {
            int[] size = getPreviewSize();
            this.mActiveRequest = this.mWidgetPreviewLoader.getPreview(this.mInfo, size[0], size[1], this);
        }
    }

    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        removeOnLayoutChangeListener(this);
        ensurePreview();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled = super.onTouchEvent(ev);
        if (this.mStylusEventHelper.checkAndPerformStylusEvent(ev)) {
            return true;
        }
        return handled;
    }

    public WidgetImageView getWidgetView() {
        return this.mWidgetImage;
    }

    public void applyFromShortcutInfo(PinShortcutRequestActivityInfo info, IconCache cache) {
        Bitmap preview;
        this.mInfo = info;
        this.mWidgetName.setText(info.getLabel());
        this.mWidgetDims.setText(String.format(this.mDimensionsFormatString, new Object[]{Integer.valueOf(1), Integer.valueOf(1)}));
        Drawable unbadgedDrawable = info.getFullResIcon(cache);
        if (unbadgedDrawable == null) {
            preview = cache.getDefaultIcon(UserHandleCompat.myUserHandle());
        } else {
            preview = BitmapUtils.createIconBitmap(unbadgedDrawable, getContext());
        }
        applyPreview(info, preview);
        setTag(new PendingAddPinShortcutInfo(info));
    }

    private String getTagToString() {
        if ((getTag() instanceof PendingAddWidgetInfo) || (getTag() instanceof PendingAddShortcutInfo)) {
            return getTag().toString();
        }
        return "";
    }
}

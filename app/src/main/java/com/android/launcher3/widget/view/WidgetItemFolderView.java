package com.android.launcher3.widget.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.base.item.PendingAddItemInfo;
import com.android.launcher3.common.drawable.FastBitmapDrawable;
import com.android.launcher3.util.ViInterpolator;
import com.android.launcher3.util.WhiteBgManager;
import com.android.launcher3.util.threadpool.ThreadPool.JobContext;
import com.sec.android.app.launcher.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WidgetItemFolderView extends WidgetItemView {
    private static final String TAG = "WidgetFolderView";
    private ImageView mArrowBtn;
    private TextView mCountView;
    private ImageView mPreview1;
    private ImageView mPreview2;
    private TextView mTitle;

    public WidgetItemFolderView(Context context) {
        super(context);
    }

    public WidgetItemFolderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WidgetItemFolderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        this.mTitle = (TextView) findViewById(R.id.widget_folder_name);
        this.mCountView = (TextView) findViewById(R.id.widget_folder_items_count);
        this.mPreview1 = (ImageView) findViewById(R.id.widget_item_folder_preview_1);
        this.mPreview2 = (ImageView) findViewById(R.id.widget_item_folder_preview_2);
        this.mArrowBtn = (ImageView) findViewById(R.id.widget_folder_view_arrow_btn);
        this.mArrowBtn.getDrawable().setAutoMirrored(true);
        this.mUninstallIcon = (ImageView) findViewById(R.id.widget_folder_uninstall_icon);
    }

    public void setWidgetFolderImage(ArrayList<Bitmap> previews) {
        if (previews != null) {
            Bitmap preview1 = (Bitmap) previews.get(0);
            if (preview1 != null) {
                this.mPreview1.setImageDrawable(new FastBitmapDrawable(preview1));
            }
            if (this.mPreview2 != null && previews.size() > 1) {
                Bitmap preview2 = (Bitmap) previews.get(1);
                if (preview2 != null) {
                    this.mPreview2.setImageDrawable(new FastBitmapDrawable(preview2));
                }
            }
        }
    }

    protected void applyTitle(String title) {
        String number = String.valueOf(this.mWidgets.size());
        String currentLanguage = Utilities.getLocale(getContext()).getLanguage();
        if ("ar".equals(currentLanguage) || "fa".equals(currentLanguage)) {
            number = Utilities.toArabicDigits(String.valueOf(this.mWidgets.size()), currentLanguage);
        } else {
            number = String.valueOf(this.mWidgets.size());
        }
        if (!LauncherFeature.supportNewWidgetList()) {
            number = '(' + number + ')';
        }
        this.mTitle.setText(title);
        this.mTitle.setSingleLine(true);
        this.mTitle.setEllipsize(TruncateAt.END);
        this.mCountView.setText(number);
        setTalkbackDescription(title, this.mWidgets.size());
    }

    public String getWidgetFolderTitle() {
        return this.mTitle.getText().toString();
    }

    private void setTalkbackDescription(String title, int size) {
        String folder = getResources().getString(R.string.talkback_folder) + String.format(getResources().getString(R.string.talkback_n_items), new Object[]{Integer.valueOf(size)});
        if (title == null || title.isEmpty()) {
            title = folder;
        } else {
            String titleEnd = "";
            if (title.length() >= folder.length()) {
                titleEnd = title.substring(title.length() - folder.length());
            }
            if (!titleEnd.equalsIgnoreCase(folder)) {
                if (Locale.getDefault().toString().contains("fr")) {
                    title = folder.concat(" " + title);
                } else {
                    title = title + " " + folder;
                }
            }
        }
        setContentDescription(title);
    }

    public void resetToRecycle() {
        super.resetToRecycle();
        this.mPreview1.setImageDrawable(null);
        if (this.mPreview2 != null) {
            this.mPreview2.setImageDrawable(null);
        }
    }

    protected Object loadPreview(JobContext jc, List<PendingAddItemInfo> items, int width, int height) {
        ArrayList<Bitmap> folderBitmaps = new ArrayList();
        int previewCount = this.mPreview2 == null ? 1 : 2;
        for (int i = 0; i < previewCount; i++) {
            if (jc.isCancelled()) {
                folderBitmaps.clear();
                return null;
            }
            folderBitmaps.add(this.mPreviewUtils.generatePreview(this.mLauncher, ((PendingAddItemInfo) items.get(i)).getProviderInfo(), width, height));
        }
        return folderBitmaps;
    }

    protected void postToSetPreview(Object object, int width, int height) {
        setWidgetFolderImage((ArrayList) object);
    }

    protected TextView getTitleTextView() {
        return this.mTitle;
    }

    protected void prepareUninstallEnter(boolean isSystemApp, boolean animate) {
        if (!isSystemApp) {
            this.mArrowBtn.setVisibility(4);
            if (LauncherFeature.supportNewWidgetList()) {
                this.mCountView.setVisibility(4);
            }
        } else if (LauncherFeature.supportNewWidgetList()) {
            setAlpha(0.46f);
            this.mArrowBtn.setVisibility(4);
            this.mCountView.setVisibility(4);
        } else {
            this.mArrowBtn.setAlpha(0.4f);
        }
        if (LauncherFeature.supportNewWidgetList() && animate) {
            AnimatorSet animatorSet = new AnimatorSet();
            if (isSystemApp) {
                Animator animator = ObjectAnimator.ofPropertyValuesHolder(this, new PropertyValuesHolder[]{mBackgroundAlphaOut});
                animatorSet.playTogether(new Animator[]{animator});
            }
            Animator animatorArrowBtn = ObjectAnimator.ofPropertyValuesHolder(this.mArrowBtn, new PropertyValuesHolder[]{mWidgetInfoAlphaOut});
            Animator animatorCountView = ObjectAnimator.ofPropertyValuesHolder(this.mCountView, new PropertyValuesHolder[]{mWidgetInfoAlphaOut});
            animatorSet.playTogether(new Animator[]{animatorArrowBtn});
            animatorSet.playTogether(new Animator[]{animatorCountView});
            animatorSet.setInterpolator(ViInterpolator.getInterploator(35));
            animatorSet.setDuration((long) getResources().getInteger(R.integer.widget_itemview_icon_anim_duration));
            animatorSet.start();
        }
    }

    protected void prepareUninstallExit(boolean isSystemApp, boolean animate) {
        if (!isSystemApp) {
            this.mArrowBtn.setVisibility(View.VISIBLE);
            if (LauncherFeature.supportNewWidgetList()) {
                this.mCountView.setVisibility(View.VISIBLE);
            }
        } else if (LauncherFeature.supportNewWidgetList()) {
            setAlpha(1.0f);
            this.mArrowBtn.setVisibility(View.VISIBLE);
            this.mCountView.setVisibility(View.VISIBLE);
        } else {
            this.mArrowBtn.setAlpha(1.0f);
        }
        if (!LauncherFeature.supportNewWidgetList()) {
            return;
        }
        if (animate) {
            AnimatorSet animatorSet = new AnimatorSet();
            if (isSystemApp) {
                Animator animator = ObjectAnimator.ofPropertyValuesHolder(this, new PropertyValuesHolder[]{mBackgroundAlphaIn});
                animatorSet.playTogether(new Animator[]{animator});
            }
            Animator animatorArrowBtn = ObjectAnimator.ofPropertyValuesHolder(this.mArrowBtn, new PropertyValuesHolder[]{mWidgetInfoAlphaIn});
            Animator animatorCountView = ObjectAnimator.ofPropertyValuesHolder(this.mCountView, new PropertyValuesHolder[]{mWidgetInfoAlphaIn});
            animatorSet.playTogether(new Animator[]{animatorArrowBtn});
            animatorSet.playTogether(new Animator[]{animatorCountView});
            animatorSet.setInterpolator(ViInterpolator.getInterploator(35));
            animatorSet.setDuration((long) getResources().getInteger(R.integer.widget_itemview_icon_anim_duration));
            animatorSet.start();
            return;
        }
        setAlpha(1.0f);
        this.mArrowBtn.setAlpha(1.0f);
        this.mCountView.setAlpha(1.0f);
    }

    protected int getPreviewImageWidth() {
        return getResources().getDimensionPixelSize(R.dimen.widget_itemview_folder_preview_image_width);
    }

    protected int getPreviewImageHeight() {
        return getResources().getDimensionPixelSize(R.dimen.widget_itemview_folder_preview_image_height);
    }

    protected boolean supportLongClick() {
        return false;
    }

    public void changeColorForBg(boolean whiteBg) {
        WhiteBgManager.changeTextColorForBg(getContext(), this.mTitle, whiteBg);
        WhiteBgManager.changeTextColorForBg(getContext(), this.mCountView, whiteBg);
        WhiteBgManager.changeColorFilterForBg(getContext(), this.mArrowBtn, whiteBg);
        int bgDrawable = getWidgetPreviewBg(whiteBg);
        if (LauncherFeature.supportNewWidgetList()) {
            setBackgroundResource(bgDrawable);
            return;
        }
        this.mPreview1.setBackgroundResource(bgDrawable);
        if (this.mPreview2 != null) {
            this.mPreview2.setBackgroundResource(bgDrawable);
        }
    }
}

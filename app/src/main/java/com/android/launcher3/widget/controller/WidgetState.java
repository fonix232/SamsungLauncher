package com.android.launcher3.widget.controller;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Toast;
import com.android.launcher3.widget.view.WidgetPagedView.Filter;
import com.sec.android.app.launcher.R;

public abstract class WidgetState {
    protected static Paint sTitleLayerPaint = new Paint();
    protected ActionListener mActionListener;
    protected final Context mContext;
    protected int mTitleBarHeight;
    private Toast mToast;

    public interface ActionListener {
        void notifyChangeState(State state);

        void openFolder(View view, boolean z);

        void startDrag(View view);
    }

    public enum State {
        NONE,
        NORMAL,
        UNINSTALL,
        SEARCH,
        FOLDER
    }

    public interface StateActionListener extends ActionListener {
        void applySearchResult(String str);

        void setSearchFilter(Filter filter);

        void setSearchString(String str);
    }

    public abstract void enter(WidgetState widgetState, AnimatorSet animatorSet);

    public abstract void exit(WidgetState widgetState, AnimatorSet animatorSet);

    public abstract State getState();

    public abstract boolean onBackPressed();

    public abstract void setFocus();

    static {
        sTitleLayerPaint.setFilterBitmap(true);
        sTitleLayerPaint.setAntiAlias(true);
    }

    public WidgetState(Context context, View titleBar) {
        this.mContext = context;
        initTitleBar(titleBar);
    }

    public void onWidgetItemClick(View view) {
    }

    public boolean onWidgetItemLongClick(View view) {
        return false;
    }

    public void onPagedViewTouchIntercepted() {
    }

    protected void initTitleBar(View titleBar) {
        this.mTitleBarHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.widget_title_bar_height);
    }

    public void setFocusToSearchEditText() {
    }

    public void refreshView() {
    }

    public void refreshModel() {
    }

    public void save(Bundle bundle) {
    }

    public void restore(Bundle bundle) {
    }

    public void onVoiceSearch(String query) {
    }

    public void openKeyBoard() {
    }

    public void setActionListener(ActionListener listener) {
        this.mActionListener = listener;
    }

    protected void setHasInstallableApp(boolean has) {
    }

    protected void changeColorForBg(boolean whitBg) {
    }

    protected void onStageEnter() {
    }

    protected void onStageExit() {
    }

    protected boolean showPopupMenu() {
        return false;
    }

    public void onConfigurationChangedIfNeeded() {
    }

    protected void clickNotAllowed(View view) {
        if (this.mToast == null) {
            this.mToast = Toast.makeText(this.mContext, R.string.long_press_widget_to_add, 0);
        } else {
            this.mToast.setText(R.string.long_press_widget_to_add);
        }
        this.mToast.show();
        float offsetY = (float) this.mContext.getResources().getDimensionPixelSize(R.dimen.widget_drag_iew_offsetY);
        View p = (View) view.findViewById(R.id.widget_preview).getParent();
        AnimatorSet bounce = new AnimatorSet();
        bounce.play(ObjectAnimator.ofFloat(p, View.TRANSLATION_Y, new float[]{offsetY}).setDuration(125)).before(ObjectAnimator.ofFloat(p, View.TRANSLATION_Y, new float[]{0.0f}).setDuration(100));
        bounce.setInterpolator(new AccelerateInterpolator());
        bounce.start();
    }
}

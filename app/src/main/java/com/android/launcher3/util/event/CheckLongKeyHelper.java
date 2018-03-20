package com.android.launcher3.util.event;

import android.view.View;
import com.android.launcher3.Launcher;
import com.android.launcher3.common.drag.DragSource;
import com.android.launcher3.common.stage.Stage;

public class CheckLongKeyHelper {
    private static final String TAG = "CheckLongKeyHelper";
    private boolean mHasPerformedLongKey;
    private final Launcher mLauncher;
    private DragSource mParent;
    private CheckForLongKey mPendingCheckForLongKey;
    private final View mView;

    class CheckForLongKey implements Runnable {
        CheckForLongKey() {
        }

        public void run() {
            boolean isMultiSelectedMode = true;
            if (!(CheckLongKeyHelper.this.mLauncher == null || CheckLongKeyHelper.this.mLauncher.getMultiSelectManager() == null)) {
                isMultiSelectedMode = CheckLongKeyHelper.this.mLauncher.getMultiSelectManager().isMultiSelectMode();
            }
            if (CheckLongKeyHelper.this.mView.getParent() != null && CheckLongKeyHelper.this.mView.hasWindowFocus() && !CheckLongKeyHelper.this.mHasPerformedLongKey && !isMultiSelectedMode) {
                CheckLongKeyHelper.this.openQuickOptionView();
                CheckLongKeyHelper.this.mHasPerformedLongKey = true;
            }
        }
    }

    public CheckLongKeyHelper(View v, Launcher launcher) {
        this.mView = v;
        this.mLauncher = launcher;
    }

    private void openQuickOptionView() {
        this.mParent = getDragSource();
        if (this.mLauncher != null && this.mLauncher.getDragMgr() != null && this.mParent != null) {
            this.mLauncher.getDragMgr().createQuickOptionViewFromCenterKey(this.mView, this.mParent);
        }
    }

    private DragSource getDragSource() {
        if (this.mView == null || this.mLauncher == null || this.mLauncher.getStageManager() == null) {
            return null;
        }
        Stage stage = this.mLauncher.getStageManager().getTopStage();
        if (stage != null) {
            return stage.getDragSourceForLongKey();
        }
        return null;
    }

    public void postCheckForLongKey() {
        this.mHasPerformedLongKey = false;
        if (this.mPendingCheckForLongKey == null) {
            this.mPendingCheckForLongKey = new CheckForLongKey();
        }
        this.mView.post(this.mPendingCheckForLongKey);
    }

    public void cancelLongKey() {
        this.mHasPerformedLongKey = false;
        if (this.mPendingCheckForLongKey != null) {
            this.mView.removeCallbacks(this.mPendingCheckForLongKey);
            this.mPendingCheckForLongKey = null;
        }
    }

    public boolean hasPerformedLongKey() {
        return this.mHasPerformedLongKey;
    }
}

package com.android.launcher3.home;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import com.sec.android.app.launcher.R;

public class AddItemOnLastPageDialog extends DialogFragment implements OnClickListener {
    private static boolean mIsTargetHotseat = false;
    private static Runnable mNegativeRunnable = null;
    private static Runnable mPositiveRunnable = null;
    private static int mRemainCnt = 0;
    private static int mTotalCnt = 0;
    private static final String sFragmentTag = "AddItemOnLastPageDialog";

    static void createAndShow(FragmentManager manager, Runnable addLastItemRunnable, Runnable restoreRunnable, int remainCnt, int totalCnt, boolean isTargetHotseat) {
        mPositiveRunnable = addLastItemRunnable;
        mNegativeRunnable = restoreRunnable;
        mRemainCnt = remainCnt;
        mTotalCnt = totalCnt;
        mIsTargetHotseat = isTargetHotseat;
        new AddItemOnLastPageDialog().show(manager, sFragmentTag);
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == -1 && mPositiveRunnable != null) {
            mPositiveRunnable.run();
            mPositiveRunnable = null;
        } else if (which == -2 && mNegativeRunnable != null) {
            mNegativeRunnable.run();
            mNegativeRunnable = null;
        }
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Builder(getActivity()).setMessage(String.format(getString(mIsTargetHotseat ? R.string.add_to_last_page_for_hotseat : R.string.add_to_last_page), new Object[]{Integer.valueOf(mTotalCnt), Integer.valueOf(mRemainCnt)})).setPositiveButton(R.string.add, this).setNegativeButton(R.string.cancel, this).create();
    }

    public static void dismiss(FragmentManager manager) {
        DialogFragment dialog = (DialogFragment) manager.findFragmentByTag(sFragmentTag);
        if (dialog != null) {
            dialog.dismissAllowingStateLoss();
        }
    }

    public void onDismiss(DialogInterface dialog) {
        if (!(mPositiveRunnable == null || mNegativeRunnable == null)) {
            mNegativeRunnable.run();
        }
        super.onDismiss(dialog);
    }

    public static boolean isActive(FragmentManager manager) {
        return manager.findFragmentByTag(sFragmentTag) != null;
    }
}

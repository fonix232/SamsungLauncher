package com.android.launcher3.common.dialog;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.common.base.controller.ControllerBase;
import com.android.launcher3.common.stage.Stage;
import com.android.launcher3.folder.FolderInfo;
import com.sec.android.app.launcher.R;

public class FolderDeleteDialog extends DialogFragment implements OnClickListener {
    public static final String FRAGMENT_TAG = "FolderDeleteDialog";
    private Stage mController;
    private FolderInfo mFolderInfo;

    public void show(FragmentManager manager, Stage controller, FolderInfo folderInfo) {
        if (!isActive(manager)) {
            this.mFolderInfo = folderInfo;
            this.mController = controller;
            show(manager, FRAGMENT_TAG);
        }
    }

    public static boolean isActive(FragmentManager manager) {
        return manager.findFragmentByTag(FRAGMENT_TAG) != null;
    }

    public static void dismiss(FragmentTransaction ft, FragmentManager manager) {
        DialogFragment dialog = (DialogFragment) manager.findFragmentByTag(FRAGMENT_TAG);
        if (dialog != null) {
            dialog.dismiss();
            ft.remove(dialog);
        }
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            dismiss(ft, getFragmentManager());
            ft.addToBackStack(null);
            return null;
        } else if (this.mFolderInfo == null) {
            return null;
        } else {
            return new Builder(getActivity()).setMessage(getString(LauncherAppState.getInstance().isHomeOnlyModeEnabled() ? R.string.quick_option_delete_folder_home_only_alert : R.string.quick_option_delete_folder_alert)).setPositiveButton(R.string.remove_popup_positive, this).setNegativeButton(R.string.cancel, this).create();
        }
    }

    public void onClick(DialogInterface dialog, int whichButton) {
        if (whichButton != -1) {
            return;
        }
        if (getActivity() == null) {
            Log.e(FRAGMENT_TAG, "Activity is null!");
        } else {
            ((ControllerBase) this.mController).deleteFolder(this.mFolderInfo);
        }
    }
}

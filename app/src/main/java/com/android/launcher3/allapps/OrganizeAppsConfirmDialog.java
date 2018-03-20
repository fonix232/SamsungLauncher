package com.android.launcher3.allapps;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import com.android.launcher3.allapps.controller.AppsController;
import com.sec.android.app.launcher.R;

public class OrganizeAppsConfirmDialog extends DialogFragment implements OnClickListener {
    private static final String FRAGMENT_TAG = "OrganizeAppsDialog";
    private AppsController mAppsController;

    public void show(FragmentManager manager, AppsController appsController) {
        if (!isActive(manager)) {
            this.mAppsController = appsController;
            show(manager, FRAGMENT_TAG);
        }
    }

    boolean isActive(FragmentManager manager) {
        return manager.findFragmentByTag(FRAGMENT_TAG) != null;
    }

    OrganizeAppsConfirmDialog getCurrentInstance(FragmentManager manager) {
        return (OrganizeAppsConfirmDialog) manager.findFragmentByTag(FRAGMENT_TAG);
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
        }
        Context context = getActivity();
        return new Builder(context).setTitle(R.string.organize_apps_confirm_alert_title).setMessage(getString(R.string.organize_apps_confirm_alert)).setPositiveButton(R.string.ok, this).create();
    }

    public void onClick(DialogInterface dialog, int whichButton) {
        if (whichButton == -1) {
            organizeApps();
        }
    }

    private void organizeApps() {
        if (getActivity() == null) {
            Log.e(FRAGMENT_TAG, "organizeApps() : activity is null");
            return;
        }
        this.mAppsController.setOrganizeAppsAlertEnable(false);
        this.mAppsController.prepareTidedUpPages();
    }

    public void onDismiss(DialogInterface dialog) {
        cancelDelete();
        super.onDismiss(dialog);
    }

    public void cancelDelete() {
    }
}

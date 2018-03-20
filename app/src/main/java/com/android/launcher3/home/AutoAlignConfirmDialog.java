package com.android.launcher3.home;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;

public class AutoAlignConfirmDialog extends DialogFragment implements OnClickListener {
    private static final String sFragmentTag = "AutoAlignConfirm";
    private CheckBox mCheckBox;
    private HomeController mHomeController;
    private boolean mUpward;

    public static void createAndShow(FragmentManager manager, HomeController homeController, boolean upward) {
        if (!isActive(manager)) {
            AutoAlignConfirmDialog dialog = new AutoAlignConfirmDialog();
            dialog.setHomeController(homeController);
            dialog.setAlignDirection(upward);
            dialog.showAllowingStateLoss(manager, sFragmentTag);
        }
    }

    public static boolean isActive(FragmentManager manager) {
        return manager.findFragmentByTag(sFragmentTag) != null;
    }

    private void setHomeController(HomeController homeController) {
        this.mHomeController = homeController;
    }

    private void setAlignDirection(boolean upward) {
        this.mUpward = upward;
    }

    public static void dismiss(FragmentTransaction ft, FragmentManager manager) {
        DialogFragment dialog = (DialogFragment) manager.findFragmentByTag(sFragmentTag);
        if (dialog != null) {
            dialog.dismiss();
            ft.remove(dialog);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        View layout = View.inflate(context, R.layout.autoalign_popup, null);
        this.mCheckBox = (CheckBox) layout.findViewById(R.id.do_not_show_again);
        Builder builder = new Builder(context);
        builder.setTitle(R.string.autoalign_popup_title);
        builder.setPositiveButton(R.string.apply, this);
        builder.setNegativeButton(R.string.cancel, this);
        builder.setView(layout);
        LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(21);
        return builder.create();
    }

    public void onDismiss(DialogInterface dialog) {
        LauncherAppState.getInstance().getTopViewChangedMessageHandler().sendMessage(5);
        super.onDismiss(dialog);
    }

    public void onClick(DialogInterface dialog, int whichButton) {
        if (whichButton == -1) {
            SALogging.getInstance().insertEventLog(getContext().getResources().getString(R.string.screen_HomeOption), getContext().getResources().getString(R.string.event_Align_positive));
            this.mHomeController.autoAlignItems(this.mUpward, false);
        } else if (whichButton == -2) {
            SALogging.getInstance().insertEventLog(getContext().getResources().getString(R.string.screen_HomeOption), getContext().getResources().getString(R.string.event_Align_negative));
        }
        if (this.mCheckBox != null && this.mCheckBox.isChecked()) {
            Editor editor = getContext().getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
            editor.putBoolean(LauncherFiles.AUTOALIGN_SHOW_DIALOG_KEY, false);
            editor.apply();
        }
    }

    public void showAllowingStateLoss(FragmentManager fm, String tag) {
        if (fm != null) {
            FragmentTransaction t = fm.beginTransaction();
            t.add(this, tag);
            t.commitAllowingStateLoss();
        }
    }
}

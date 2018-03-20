package com.android.launcher3.folder.folderlock;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.view.KeyEvent;
import com.android.launcher3.Launcher;
import com.android.launcher3.common.base.item.ItemInfo;
import com.sec.android.app.launcher.R;

public class LockedItemDropConfirmDialog extends DialogFragment implements OnClickListener {
    private static final String TAG = "LockedItemDropConfirmDialog";
    private static ItemInfo sDropedInfo = null;
    private static final String sFragmentTag = "LockedItemDropConfirm";

    static void createAndShow(Launcher launcher, ItemInfo info) {
        sDropedInfo = info;
        new LockedItemDropConfirmDialog().show(launcher.getFragmentManager(), sFragmentTag);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String lock_unlock_dialog_msg = getResources().getString(R.string.lock_or_unlock_dislog_msg);
        String positive_button_string = getResources().getString(R.string.quick_option_lock);
        AlertDialog dialog = new Builder(getActivity()).setTitle("").setMessage(lock_unlock_dialog_msg).setPositiveButton(positive_button_string, this).setNegativeButton(getResources().getString(R.string.quick_option_unlock), this).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                return keyCode == 4 && event.getRepeatCount() == 0;
            }
        });
        return dialog;
    }

    public void onClick(DialogInterface dialog, int which) {
        FolderLock folderLock = FolderLock.getInstance();
        switch (which) {
            case -2:
                folderLock.unlockItem(sDropedInfo);
                return;
            default:
                return;
        }
    }

    public void onDestroy() {
        super.onDestroy();
    }
}

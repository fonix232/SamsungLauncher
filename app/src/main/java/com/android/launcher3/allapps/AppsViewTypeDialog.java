package com.android.launcher3.allapps;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.os.Bundle;
import com.android.launcher3.allapps.controller.AppsController.ViewType;
import com.android.launcher3.util.logging.SALogging;
import com.sec.android.app.launcher.R;

public class AppsViewTypeDialog extends DialogFragment implements OnClickListener {
    private static final String sFragmentTag = "MenuViewTypeDialog";
    private final String[] choices = new String[2];
    private OnViewTypeChagnedListener onViewTypeChagnedListener;

    public interface OnViewTypeChagnedListener {
        void onDismiss();

        void onResult(ViewType viewType);
    }

    public static void createAndShow(ViewType viewType, FragmentManager manager, OnViewTypeChagnedListener listener) {
        if (!isActive(manager)) {
            AppsViewTypeDialog df = new AppsViewTypeDialog();
            int selected = -1;
            switch (viewType) {
                case CUSTOM_GRID:
                    selected = 0;
                    break;
                case ALPHABETIC_GRID:
                    selected = 1;
                    break;
            }
            Bundle args = new Bundle();
            args.putInt("selected", selected);
            df.setArguments(args);
            df.show(manager, sFragmentTag);
            df.setOnAppsViewTypeChagnedListener(listener);
        }
    }

    static boolean isActive(FragmentManager manager) {
        return manager.findFragmentByTag(sFragmentTag) != null;
    }

    public static void dismiss(FragmentTransaction ft, FragmentManager manager) {
        DialogFragment dialog = (DialogFragment) manager.findFragmentByTag(sFragmentTag);
        if (dialog != null) {
            dialog.dismiss();
            ft.remove(dialog);
        }
    }

    public void setOnAppsViewTypeChagnedListener(OnViewTypeChagnedListener listener) {
        this.onViewTypeChagnedListener = listener;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources r = getResources();
        this.choices[0] = r.getString(R.string.viewtype_custom_grid);
        this.choices[1] = r.getString(R.string.viewtype_alphabetic_grid);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            dismiss(ft, getFragmentManager());
            ft.addToBackStack(null);
            return null;
        }
        Context c = getActivity();
        return new Builder(c).setTitle(R.string.options_menu_sort).setSingleChoiceItems(this.choices, getArguments().getInt("selected"), this).setNegativeButton(R.string.menu_edit_cancel, this).create();
    }

    public void onCancel(DialogInterface dialogInterface) {
        if (this.onViewTypeChagnedListener != null) {
            this.onViewTypeChagnedListener.onDismiss();
        }
    }

    public void onClick(DialogInterface dialog, int whichButton) {
        if (whichButton != -2) {
            ViewType viewType;
            switch (whichButton) {
                case 1:
                    viewType = ViewType.ALPHABETIC_GRID;
                    break;
                default:
                    viewType = ViewType.CUSTOM_GRID;
                    break;
            }
            if (this.onViewTypeChagnedListener != null) {
                this.onViewTypeChagnedListener.onResult(viewType);
            }
            dialog.dismiss();
            return;
        }
        if (this.onViewTypeChagnedListener != null) {
            this.onViewTypeChagnedListener.onDismiss();
        }
        try {
            SALogging.getInstance().insertEventLog(getResources().getString(R.string.screen_Apps_SelectMode), getResources().getString(R.string.event_Apps_SortCancel));
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
}

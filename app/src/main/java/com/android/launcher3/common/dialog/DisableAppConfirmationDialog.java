package com.android.launcher3.common.dialog;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.util.Log;
import android.widget.Toast;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.compat.UserHandleCompat;
import com.android.launcher3.util.BitmapUtils;
import com.android.launcher3.util.Reflection;
import com.android.launcher3.util.logging.GSIMLogging;
import com.sec.android.app.launcher.R;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;

public class DisableAppConfirmationDialog extends DialogFragment implements OnClickListener {
    private static final String TAG = "DisableAppConfirmDialog";
    private static final String sFragmentTag = "DisableAppConfirm";
    private static Bitmap sIcon;
    private static String sPackage;
    private static String sPackageLabel;
    private static Runnable sPostRunnable = null;
    private static int sUserID;

    public static void createAndShow(Context context, UserHandleCompat user, String packageName, String label, Drawable icon, FragmentManager manager, Runnable runnable) {
        if (!isActive(manager) && label != null) {
            DisableAppConfirmationDialog dialog = new DisableAppConfirmationDialog();
            Bundle args = new Bundle();
            args.putString("package", packageName);
            args.putString("label", label);
            args.putInt("android.intent.extra.USER", user.hashCode());
            Bitmap bmp = BitmapUtils.createIconBitmap(icon, context, 144, 144);
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            bmp.compress(CompressFormat.PNG, 50, bs);
            args.putByteArray("icon", bs.toByteArray());
            dialog.setArguments(args);
            dialog.show(manager, sFragmentTag);
            sPostRunnable = runnable;
        }
    }

    public static boolean isActive(FragmentManager manager) {
        return (manager == null || manager.findFragmentByTag(sFragmentTag) == null) ? false : true;
    }

    public static void dismissIfNeeded(Context context, FragmentManager manager) {
        if (!LauncherModel.isValidPackage(context, sPackage, UserHandleCompat.fromUser(UserHandle.getUserHandleForUid(sUserID)))) {
            dismiss(manager.beginTransaction(), manager);
        }
    }

    public static void dismiss(FragmentTransaction ft, FragmentManager manager) {
        DialogFragment dialog = (DialogFragment) manager.findFragmentByTag(sFragmentTag);
        if (dialog != null) {
            dialog.dismiss();
            ft.remove(dialog);
        }
    }

    public static void showIfNeeded(FragmentManager manager) {
        DialogFragment dialog = (DialogFragment) manager.findFragmentByTag(sFragmentTag);
        if (dialog != null) {
            dialog.show(manager, sFragmentTag);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sPackage = getArguments().getString("package");
        sPackageLabel = getArguments().getString("label");
        sUserID = getArguments().getInt("android.intent.extra.USER");
        byte[] bytes = getArguments().getByteArray("icon");
        sIcon = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String disable_massage;
        Context context = getActivity();
        Builder builder = new Builder(context);
        builder.setIcon(new BitmapDrawable(context.getResources(), sIcon)).setTitle(sPackageLabel);
        builder.setPositiveButton(R.string.tts_quick_option_disable, this);
        builder.setNegativeButton(R.string.cancel, this);
        if (Utilities.isKnoxMode()) {
            String knoxName = Utilities.getKnoxContainerName(context);
            disable_massage = getString(R.string.disable_message_knox_mode, new Object[]{sPackageLabel, knoxName});
        } else if (LauncherFeature.isVZWModel()) {
            disable_massage = getString(R.string.disable_message_vzw, new Object[]{sPackageLabel});
        } else {
            disable_massage = getString(R.string.disable_message, new Object[]{sPackageLabel, sPackageLabel});
        }
        if (Utilities.sIsRtl) {
            disable_massage = "‏" + disable_massage;
        }
        builder.setMessage(disable_massage);
        return builder.create();
    }

    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (sPostRunnable != null) {
            new Handler().post(sPostRunnable);
            sPostRunnable = null;
        }
    }

    public void onClick(DialogInterface dialog, int whichButton) {
        if (whichButton == -1) {
            try {
                if (Utilities.isKnoxMode() || sUserID == UserHandleCompat.myUserHandle().hashCode()) {
                    PackageManager pkgMgr = getActivity().getPackageManager();
                    if (pkgMgr != null) {
                        pkgMgr.setApplicationEnabledSetting(sPackage, 3, 0);
                        GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_DISABLE_APP, sPackage, -1, false);
                        return;
                    }
                    Log.e(TAG, "Couldn't get package manager.");
                    return;
                }
                Class iPackageManagerStubClass = Reflection.getClass("android.content.pm.IPackageManager$Stub");
                Class ServiceManagerClass = Reflection.getClass("android.os.ServiceManager");
                if (iPackageManagerStubClass != null && ServiceManagerClass != null) {
                    Method asInterfaceMethod = Reflection.getMethod(iPackageManagerStubClass, "asInterface", new Class[]{IBinder.class}, true);
                    Method getServiceMethod = Reflection.getMethod(ServiceManagerClass, "getService", new Class[]{String.class}, true);
                    if (asInterfaceMethod != null && getServiceMethod != null) {
                        Object[] objArr = new Object[1];
                        objArr[0] = Reflection.invoke(null, getServiceMethod, "package");
                        Object iPackageManager = Reflection.invoke(null, asInterfaceMethod, objArr);
                        if (iPackageManager != null) {
                            Method setApplicationEnabledSetting = Reflection.getMethod(iPackageManager.getClass(), "setApplicationEnabledSetting", new Class[]{String.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, String.class}, true);
                            if (setApplicationEnabledSetting != null) {
                                Reflection.invoke(iPackageManager, setApplicationEnabledSetting, sPackage, Integer.valueOf(3), Integer.valueOf(0), Integer.valueOf(sUserID), "");
                                GSIMLogging.getInstance().insertLogging(GSIMLogging.FEATURE_NAME_DISABLE_APP, sPackage, -1, false);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Problem disabling package.", e);
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), "Disabling has been failed", 1).show();
                }
            }
        }
    }
}

package com.android.launcher3.common.bnr.smartswitch;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.Utilities;
import com.android.launcher3.common.bnr.LauncherBnrHelper;
import com.android.launcher3.common.bnr.LauncherBnrListener;
import com.android.launcher3.common.bnr.LauncherBnrListener.Result;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class SmartSwitchBnrService extends IntentService implements LauncherBnrListener {
    private static final String BNR_PERMISSION = "com.sec.permission.BACKUP_RESTORE_HOMESCREEN";
    private static final String REQUEST_RESTORE_CONTACT_SHORTCUT = "com.sec.android.intent.action.REQUEST_RESTORE_CONTACT_SHORTCUT";
    public static final String RESTORE_USE_PLAYSTORE = "USE_PLAYSTORE";
    private static final String TAG = "Launcher.SSBnrService";
    private static final String VCF_RESTORE_PATH = (Environment.getExternalStorageDirectory().getAbsolutePath() + "/TempVcfForContact");
    private int mSecurityLevel = 0;
    private String mSessionKey = null;
    private String mSessionTime = null;
    private String mSource = null;

    public SmartSwitchBnrService() {
        super("SmartSwitchBnrService");
    }

    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            Log.e(TAG, "intent is null");
            return;
        }
        String action = intent.getAction();
        Log.d(TAG, "onHandleIntent action : " + action);
        this.mSessionKey = null;
        this.mSessionTime = null;
        this.mSource = null;
        this.mSecurityLevel = 0;
        String path;
        if (SmartSwitchBnr.REQUEST_BACKUP_HOMESCREEN.equals(action)) {
            path = intent.getStringExtra("SAVE_PATH");
            this.mSessionKey = intent.getStringExtra("SESSION_KEY");
            this.mSessionTime = intent.getStringExtra("EXPORT_SESSION_TIME");
            this.mSource = intent.getStringExtra("SOURCE");
            this.mSecurityLevel = intent.getIntExtra("SECURITY_LEVEL", 0);
            LauncherBnrHelper.getInstance().backup(getApplicationContext(), path, this.mSource, this);
        } else if (SmartSwitchBnr.REQUEST_RESTORE_HOMESCREEN.equals(action)) {
            path = intent.getStringExtra("SAVE_PATH");
            this.mSessionKey = intent.getStringExtra("SESSION_KEY");
            this.mSource = intent.getStringExtra("SOURCE");
            this.mSecurityLevel = intent.getIntExtra("SECURITY_LEVEL", 0);
            int debugLevel = intent.getIntExtra("DEBUG_LEVEL", 0);
            Bundle data = null;
            ArrayList<String> extraBackup = intent.getStringArrayListExtra("EXTRA_BACKUP_ITEM");
            if (extraBackup != null) {
                Iterator it = extraBackup.iterator();
                while (it.hasNext()) {
                    if (RESTORE_USE_PLAYSTORE.equals((String) it.next())) {
                        data = new Bundle();
                        data.putBoolean(RESTORE_USE_PLAYSTORE, true);
                        break;
                    }
                }
            }
            Editor editor = getApplicationContext().getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0).edit();
            editor.remove(Utilities.CONTACT_SHORTCUT_IDS);
            editor.apply();
            LauncherBnrHelper.getInstance().restore(getApplicationContext(), path, this.mSource, debugLevel, this, data);
        }
    }

    public void backupComplete(Result result, File saveFile) {
        Log.i(TAG, "backupComplete result : " + result.result);
        Intent backupResult = new Intent(SmartSwitchBnr.RESPONSE_BACKUP_HOMESCREEN);
        backupResult.putExtra("RESULT", result.result);
        backupResult.putExtra("ERR_CODE", result.errorCode);
        if (saveFile != null) {
            backupResult.putExtra("REQ_SIZE", (int) saveFile.length());
        } else {
            backupResult.putExtra("REQ_SIZE", 0);
        }
        backupResult.putExtra("SOURCE", this.mSource);
        backupResult.putExtra("EXPORT_SESSION_TIME", this.mSessionTime);
        getApplicationContext().sendBroadcast(backupResult);
    }

    public void restoreComplete(Result result, File saveFile) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(LauncherAppState.getSharedPreferencesKey(), 0);
        Set<String> contactShortcuts = prefs.getStringSet(Utilities.CONTACT_SHORTCUT_IDS, null);
        if (contactShortcuts == null || contactShortcuts.size() <= 0) {
            Log.i(TAG, "restoreComplete result : " + result.result);
            Intent restoreResult = new Intent(SmartSwitchBnr.RESPONSE_RESTORE_HOMESCREEN);
            restoreResult.putExtra("RESULT", result.result);
            restoreResult.putExtra("ERR_CODE", result.errorCode);
            if (saveFile != null) {
                restoreResult.putExtra("REQ_SIZE", (int) saveFile.length());
            } else {
                restoreResult.putExtra("REQ_SIZE", 0);
            }
            restoreResult.putExtra("SOURCE", this.mSource);
            getApplicationContext().sendBroadcast(restoreResult);
            return;
        }
        Log.d(TAG, "send restoreComplete after restore contact shortcut " + contactShortcuts.size());
        Editor editor = prefs.edit();
        editor.putInt(Utilities.SMARTSWITCH_RESTORE_RESULT, result.result);
        editor.putInt(Utilities.SMARTSWITCH_RESTORE_ERROR_CODE, result.errorCode);
        if (saveFile != null) {
            editor.putInt(Utilities.SMARTSWITCH_SAVE_FILE_LENGTH, (int) saveFile.length());
        } else {
            editor.putInt(Utilities.SMARTSWITCH_SAVE_FILE_LENGTH, 0);
        }
        editor.putString(Utilities.SMARTSWITCH_RESTORE_SOURCE, this.mSource);
        editor.apply();
        Log.d(TAG, "send broadcast - restore contact shortcut");
        Intent contactIntent = new Intent(REQUEST_RESTORE_CONTACT_SHORTCUT);
        contactIntent.putExtra("FILE_PATH", VCF_RESTORE_PATH);
        contactIntent.setComponent(new ComponentName("com.samsung.android.contacts", "com.samsung.contacts.receiver.ContactsReceiver"));
        getApplicationContext().sendBroadcast(contactIntent, "com.sec.permission.BACKUP_RESTORE_HOMESCREEN");
    }

    public OutputStream getEncryptStream(FileOutputStream fos) throws GeneralSecurityException, IOException {
        return encryptStream(fos, this.mSessionKey, this.mSecurityLevel);
    }

    public InputStream getDecryptStream(FileInputStream fis) throws GeneralSecurityException, IOException {
        return decryptStream(fis, this.mSessionKey, this.mSecurityLevel);
    }

    private OutputStream encryptStream(OutputStream out, String sessionKey, int securityLevel) throws GeneralSecurityException, IOException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[cipher.getBlockSize()];
        new SecureRandom().nextBytes(iv);
        AlgorithmParameterSpec spec = new IvParameterSpec(iv);
        out.write(iv);
        byte[] salt = new byte[16];
        if (securityLevel == 1) {
            new SecureRandom().nextBytes(salt);
            out.write(salt);
        }
        SecretKeySpec secretKey = null;
        try {
            secretKey = generateSecretKey(sessionKey, securityLevel, salt);
        } catch (Exception e) {
            Log.e(TAG, "encryptStream secretKey Exception : " + e.toString());
        }
        cipher.init(1, secretKey, spec);
        return new CipherOutputStream(out, cipher);
    }

    private InputStream decryptStream(InputStream in, String sessionKey, int securityLevel) throws GeneralSecurityException, IOException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[cipher.getBlockSize()];
        if (in.read(iv) < 0) {
            return null;
        }
        AlgorithmParameterSpec spec = new IvParameterSpec(iv);
        byte[] salt = new byte[16];
        if (securityLevel == 1) {
            in.read(salt);
        }
        SecretKeySpec secretKey = null;
        try {
            secretKey = generateSecretKey(sessionKey, securityLevel, salt);
        } catch (Exception e) {
            Log.e(TAG, "decryptStream secretKey Exception : " + e.toString());
        }
        cipher.init(2, secretKey, spec);
        return new CipherInputStream(in, cipher);
    }

    private SecretKeySpec generateSecretKey(String password, int securityLevel, byte[] salt) throws Exception {
        if (securityLevel == 1) {
            return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(new PBEKeySpec(password.toCharArray(), salt, 1000, 256)).getEncoded(), "AES");
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(password.getBytes("UTF-8"));
        byte[] keyBytes = new byte[16];
        System.arraycopy(digest.digest(), 0, keyBytes, 0, keyBytes.length);
        return new SecretKeySpec(keyBytes, "AES");
    }
}

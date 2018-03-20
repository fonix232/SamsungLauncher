package com.android.launcher3.common.bnr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

public interface LauncherBnrListener {

    public static class Result {
        public int errorCode = 0;
        public int result = 0;
    }

    void backupComplete(Result result, File file);

    InputStream getDecryptStream(FileInputStream fileInputStream) throws GeneralSecurityException, IOException;

    OutputStream getEncryptStream(FileOutputStream fileOutputStream) throws GeneralSecurityException, IOException;

    void restoreComplete(Result result, File file);
}

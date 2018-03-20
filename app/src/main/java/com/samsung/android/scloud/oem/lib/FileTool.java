package com.samsung.android.scloud.oem.lib;

import android.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class FileTool {
    private static final String TAG = "FileTool";
    private static MessageDigest mMessageDigest = null;

    public interface PDMProgressListener {
        void transferred(long j, long j2);
    }

    public static synchronized String getMessageDigestFromString(String msg) throws IOException, NoSuchAlgorithmException {
        String messageDigest;
        synchronized (FileTool.class) {
            messageDigest = getMessageDigest(new ByteArrayInputStream(msg.getBytes("UTF-8")));
        }
        return messageDigest;
    }

    public static synchronized String getMessageDigest(String filepath) throws IOException, NoSuchAlgorithmException {
        String messageDigest;
        synchronized (FileTool.class) {
            messageDigest = getMessageDigest(new FileInputStream(filepath));
        }
        return messageDigest;
    }

    public static synchronized String getMessageDigest(InputStream fis) throws IOException, NoSuchAlgorithmException {
        StringBuilder checksum;
        synchronized (FileTool.class) {
            byte[] buff = new byte[8192];
            if (mMessageDigest == null) {
                mMessageDigest = MessageDigest.getInstance("MD5");
            } else {
                mMessageDigest.reset();
            }
            while (true) {
                int len = fis.read(buff);
                if (len <= 0) {
                    break;
                }
                mMessageDigest.update(buff, 0, len);
            }
            fis.close();
            byte[] md5Data = mMessageDigest.digest();
            checksum = new StringBuilder();
            for (byte b : md5Data) {
                int bHex = b & 255;
                if (bHex <= 15) {
                    checksum.append("0");
                }
                checksum.append(Integer.toHexString(bHex));
            }
        }
        return checksum.toString().toUpperCase();
    }

    public static boolean isSameFileInputStream(FileInputStream fis, String checkSum) {
        try {
            if (getMessageDigest((InputStream) fis).equals(checkSum)) {
                return true;
            }
            return false;
        } catch (NoSuchAlgorithmException e) {
            return false;
        } catch (IOException e2) {
            return false;
        }
    }

    public static boolean isSameFile(String filepath, String checkSum) {
        try {
            if (getMessageDigest(filepath).equals(checkSum)) {
                return true;
            }
            return false;
        } catch (NoSuchAlgorithmException e) {
            return false;
        } catch (IOException e2) {
            return false;
        }
    }

    public static void writeToFile(InputStream inputStream, long size, FileOutputStream fileOpStream, PDMProgressListener handler) throws IOException {
        try {
            LOG.d(TAG, "writeToFile - start Write with stream : " + fileOpStream);
            byte[] buffer = new byte[131072];
            long sum = 0;
            while (true) {
                int len = inputStream.read(buffer);
                if (len <= 0) {
                    break;
                }
                sum += (long) len;
                if (handler != null) {
                    handler.transferred(sum, size);
                }
                fileOpStream.write(buffer, 0, len);
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (fileOpStream != null) {
                fileOpStream.close();
            }
        } catch (IOException ioe) {
            throw ioe;
        } catch (Throwable th) {
            if (inputStream != null) {
                inputStream.close();
            }
            if (fileOpStream != null) {
                fileOpStream.close();
            }
        }
    }

    public static void writeToFile(InputStream inputStream, long size, String filepath, PDMProgressListener handler) throws IOException {
        LOG.d(TAG, "writeToFile - start Write with stream : " + filepath);
        String[] split = filepath.split("/");
        String folderPath = filepath.substring(0, filepath.length() - split[split.length - 1].length());
        File file = new File(folderPath);
        if (!file.exists()) {
            LOG.i(TAG, "Creating folder : " + folderPath);
            if (!file.mkdirs()) {
                LOG.f(TAG, "ORSMetaResponse.fromBinaryFile(): Can not create directory. ");
                throw new IOException();
            }
        }
        writeToFile(inputStream, size, new FileOutputStream(filepath, false), handler);
    }

    public static void writeToFile(String inputFile, long size, FileOutputStream outputStream, PDMProgressListener handler) throws IOException {
        File file = new File(inputFile);
        if (file.exists()) {
            writeToFile(new FileInputStream(file), size, outputStream, handler);
            return;
        }
        throw new IOException();
    }

    public static byte[] getByteArr(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteOpStream = new ByteArrayOutputStream();
        byte[] buff = new byte[1024];
        while (true) {
            int len = inputStream.read(buff, 0, 1024);
            if (len == -1) {
                return byteOpStream.toByteArray();
            }
            byteOpStream.write(buff, 0, len);
        }
    }

    public static String encode(String message) {
        String str = null;
        try {
            str = Base64.encodeToString(MessageDigest.getInstance("SHA1").digest(message.getBytes("UTF-8")), 0);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e2) {
            e2.printStackTrace();
        }
        return str;
    }

    public static void makeFolder(String filepath) throws IOException {
        String[] split = filepath.split("/");
        String folderPath = filepath.substring(0, filepath.length() - split[split.length - 1].length());
        File file = new File(folderPath);
        if (!file.exists()) {
            LOG.i(TAG, "Creating folder : " + folderPath);
            if (!file.mkdirs()) {
                LOG.f(TAG, "ORSMetaResponse.fromBinaryFile(): Can not create directory. ");
                throw new IOException();
            }
        }
    }
}

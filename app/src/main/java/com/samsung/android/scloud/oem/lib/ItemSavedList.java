package com.samsung.android.scloud.oem.lib;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ItemSavedList extends ArrayList<String> {
    private static final String ITEM = "_processedItem";
    static final String SCLOUD_LOCAL_URI = "/scloud/";
    private static final long serialVersionUID = 1;
    private FileWriter fw;
    private String mTAG = "ItemSavedList_";
    private File processedKeyFile;

    public static ItemSavedList load(Context context, String name) {
        return new ItemSavedList(context, name);
    }

    private ItemSavedList(Context context, String name) {
        IOException e;
        Throwable th;
        this.mTAG += name;
        File folder = new File(context.getFilesDir() + SCLOUD_LOCAL_URI);
        if (!folder.exists()) {
            LOG.i(this.mTAG, "isDirectoryMade : " + folder.mkdir());
        }
        this.processedKeyFile = new File(context.getFilesDir() + SCLOUD_LOCAL_URI + name + ITEM);
        LOG.i(this.mTAG, "create : " + context.getFilesDir() + SCLOUD_LOCAL_URI + name + ITEM + ", " + this.processedKeyFile.exists() + ", " + this.processedKeyFile.length());
        BufferedReader bufferedReader = null;
        try {
            if (this.processedKeyFile.exists() && this.processedKeyFile.length() > 0) {
                List<String> prev = new ArrayList();
                BufferedReader br = new BufferedReader(new FileReader(this.processedKeyFile));
                while (true) {
                    try {
                        String id = br.readLine();
                        if (id == null) {
                            break;
                        }
                        id = id.trim();
                        if (!id.isEmpty()) {
                            LOG.i(this.mTAG, "read : " + id);
                            prev.add(id);
                        }
                    } catch (IOException e2) {
                        e = e2;
                        bufferedReader = br;
                    } catch (Throwable th2) {
                        th = th2;
                        bufferedReader = br;
                    }
                }
                addAll(prev);
                LOG.i(this.mTAG, "Add prev data : " + prev.size());
                bufferedReader = br;
            }
            this.fw = new FileWriter(this.processedKeyFile, true);
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
        } catch (IOException e4) {
            e3 = e4;
            try {
                e3.printStackTrace();
                LOG.e(this.mTAG, "ItemSavedList err", e3);
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e32) {
                        e32.printStackTrace();
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e322) {
                        e322.printStackTrace();
                    }
                }
                throw th;
            }
        }
    }

    public boolean add(String object) {
        try {
            this.fw.write(object + "\n");
            this.fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
            LOG.e(this.mTAG, "ItemSavedList err", e);
        }
        return super.add(object);
    }

    public void clear() {
        try {
            this.fw.close();
            LOG.i(this.mTAG, "clear this : " + this.processedKeyFile.exists() + ", " + this.processedKeyFile.delete());
            this.fw = new FileWriter(this.processedKeyFile, true);
        } catch (IOException e) {
            e.printStackTrace();
            LOG.e(this.mTAG, "ItemSavedList err", e);
        }
        super.clear();
    }
}

package com.samsung.android.scloud.oem.lib;

import com.android.launcher3.folder.folderlock.FolderLock;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONReader {
    private int buf;
    private Reader inputStreamReader;
    private boolean isJSONArray;
    private int preBuf;

    public JSONReader(Reader inputStreamReader) throws IOException {
        this.inputStreamReader = inputStreamReader;
        while (true) {
            int read = inputStreamReader.read();
            this.buf = read;
            if (read != 32) {
                break;
            }
            this.preBuf = this.buf;
        }
        this.isJSONArray = this.buf == 91;
        if (!this.isJSONArray && this.buf != FolderLock.REQUEST_CODE_FOLDER_UNLOCK) {
            throw new IOException("This is not JSON stream");
        }
    }

    public boolean isJSONArray() throws IOException {
        return this.isJSONArray;
    }

    public JSONObject getJSONObjectInArray() throws IOException, JSONException {
        if (this.isJSONArray) {
            StringBuilder sb = new StringBuilder();
            do {
                int read = this.inputStreamReader.read();
                this.buf = read;
                if (read == FolderLock.REQUEST_CODE_FOLDER_UNLOCK) {
                    break;
                }
                this.preBuf = this.buf;
            } while (this.buf != -1);
            if (this.buf == -1) {
                return null;
            }
            sb.append((char) this.buf);
            int find = 1;
            int pass = 1;
            while (find != 0) {
                this.preBuf = this.buf;
                this.buf = this.inputStreamReader.read();
                if (this.buf == -1) {
                    break;
                }
                sb.append((char) this.buf);
                if (this.buf == 34 && this.preBuf != 92) {
                    pass *= -1;
                }
                if (this.buf == FolderLock.REQUEST_CODE_FOLDER_UNLOCK && pass > 0) {
                    find++;
                }
                if (this.buf == 125 && pass > 0) {
                    find--;
                }
            }
            if (this.buf != -1) {
                return new JSONObject(sb.toString());
            }
            return null;
        }
        throw new IOException("This is not JSON array stream");
    }

    public static void main(String[] args) throws JSONException, IOException {
        JSONObject json = new JSONObject();
        json.put("name", "asdf{}{}{{}");
        json.put("val", "asdf\"{adsf}\"\"\"}\"{");
        json.put("jsonstr", json.toString());
        new JSONReader(new StringReader("[" + json.toString() + "]")).getJSONObjectInArray();
    }
}

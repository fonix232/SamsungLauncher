package com.android.launcher3.common.bnr;

import android.content.Context;
import com.android.launcher3.common.bnr.LauncherBnrListener.Result;
import java.util.ArrayList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public interface LauncherBnrCallBack {
    String backupCategory();

    void backupLayout(Context context, XmlSerializer xmlSerializer, String str, Result result);

    void restoreLayout(Context context, XmlPullParser xmlPullParser, ArrayList<String> arrayList, Result result);
}

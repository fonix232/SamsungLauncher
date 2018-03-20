package com.android.launcher3.allapps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Message;
//import android.os.SemSystemProperties;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class UpdateCheckThread extends Thread {
    private static final int NETWORK_NOT_CONNECTED = 5;
    private static final int RESULT_CONTENTS_SIZE = 20;
    private static final int SEND_SEARCH_END = 2;
    private static final int SEND_THREAD_INFORMATION = 1;
    private static final String SERVER_URL = "http://vas.samsungapps.com/myApps/display/searchAppList.as";
    private static final String TAG = "UpdateCheckThread";
    private final AlphabeticalAppsList mApps;
    private Context mContext;
    private final boolean mFromReceiver;
    private String mQuery;

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void run() {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x013a in list [B:10:0x0129]
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:282)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
        /*
        r11 = this;
        r10 = 2;
        r6 = "UpdateCheckThread";
        r7 = new java.lang.StringBuilder;
        r7.<init>();
        r8 = "start update check (from Receiver : ";
        r7 = r7.append(r8);
        r8 = r11.mFromReceiver;
        r7 = r7.append(r8);
        r8 = ")";
        r7 = r7.append(r8);
        r7 = r7.toString();
        android.util.Log.e(r6, r7);
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r6 = "http://vas.samsungapps.com/myApps/display/searchAppList.as";
        r0.append(r6);
        r6 = "?";
        r0.append(r6);
        r6 = "keyword=";
        r0.append(r6);
        r6 = r11.mQuery;
        r0.append(r6);
        r6 = "&imgWidth=";
        r0.append(r6);
        r6 = 135; // 0x87 float:1.89E-43 double:6.67E-322;
        r0.append(r6);
        r6 = "&alignOrder=";
        r0.append(r6);
        r6 = "bestMatch";
        r0.append(r6);
        r6 = "&deviceId=";
        r0.append(r6);
        r6 = android.os.Build.MODEL;
        r0.append(r6);
        r6 = "&startNum=";
        r0.append(r6);
        r6 = 1;
        r0.append(r6);
        r6 = "&endNum=";
        r0.append(r6);
        r6 = 30;
        r0.append(r6);
        r6 = "&mcc=";
        r0.append(r6);
        r6 = r11.getMCC();
        r0.append(r6);
        r6 = "&mnc=";
        r0.append(r6);
        r6 = r11.getMNC();
        r0.append(r6);
        r6 = "&csc=";
        r0.append(r6);
        r6 = r11.getCSC();
        r0.append(r6);
        r6 = "&osVersion=";
        r0.append(r6);
        r6 = android.os.Build.VERSION.SDK_INT;
        r6 = java.lang.String.valueOf(r6);
        r0.append(r6);
        r6 = "&srcType=";
        r0.append(r6);
        r6 = "HOMESCREEN";
        r0.append(r6);
        r6 = "&clientType=";
        r0.append(r6);
        r6 = "GALAXYAPPS";
        r0.append(r6);
        r6 = "&sdlVersion=";
        r0.append(r6);
        r6 = android.os.Build.VERSION.SEM_INT;
        r6 = java.lang.String.valueOf(r6);
        r0.append(r6);
        r3 = 0;
        r4 = r0.toString();	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r6 = "UpdateCheckThread";	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r7 = new java.lang.StringBuilder;	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r7.<init>();	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r8 = "check url : ";	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r7 = r7.append(r8);	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r7 = r7.append(r4);	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r7 = r7.toString();	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        android.util.Log.i(r6, r7);	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r6 = " ";	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r7 = "%20";	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r4 = r4.replaceAll(r6, r7);	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r5 = new java.net.URL;	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r5.<init>(r4);	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r3 = r11.checkUpdate(r5);	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        if (r3 != 0) goto L_0x010d;
    L_0x00ef:
        r6 = r11.mFromReceiver;
        if (r6 != 0) goto L_0x010d;
    L_0x00f3:
        r6 = "UpdateCheckThread";
        r7 = new java.lang.StringBuilder;
        r7.<init>();
        r8 = "end update check mFromReceiver : ";
        r7 = r7.append(r8);
        r8 = r11.mFromReceiver;
        r7 = r7.append(r8);
        r7 = r7.toString();
        android.util.Log.e(r6, r7);
    L_0x010d:
        r6 = "UpdateCheckThread";
        r7 = new java.lang.StringBuilder;
        r7.<init>();
        r8 = "end update check : ";
        r7 = r7.append(r8);
        r7 = r7.append(r3);
        r7 = r7.toString();
        android.util.Log.e(r6, r7);
        r6 = r11.mApps;
        if (r6 == 0) goto L_0x013a;
    L_0x0129:
        r6 = r11.mApps;
        r6 = r6.mIncomingHandler;
        r2 = r6.obtainMessage();
        r2.what = r10;
        r6 = r11.mApps;
        r6 = r6.mIncomingHandler;
        r6.sendMessage(r2);
    L_0x013a:
        return;
    L_0x013b:
        r1 = move-exception;
        r6 = "UpdateCheckThread";	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r7 = new java.lang.StringBuilder;	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r7.<init>();	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r8 = "MalformedURLException : ";	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r7 = r7.append(r8);	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r8 = r1.toString();	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r7 = r7.append(r8);	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        r7 = r7.toString();	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        android.util.Log.e(r6, r7);	 Catch:{ MalformedURLException -> 0x013b, all -> 0x01a6 }
        if (r3 != 0) goto L_0x0178;
    L_0x015a:
        r6 = r11.mFromReceiver;
        if (r6 != 0) goto L_0x0178;
    L_0x015e:
        r6 = "UpdateCheckThread";
        r7 = new java.lang.StringBuilder;
        r7.<init>();
        r8 = "end update check mFromReceiver : ";
        r7 = r7.append(r8);
        r8 = r11.mFromReceiver;
        r7 = r7.append(r8);
        r7 = r7.toString();
        android.util.Log.e(r6, r7);
    L_0x0178:
        r6 = "UpdateCheckThread";
        r7 = new java.lang.StringBuilder;
        r7.<init>();
        r8 = "end update check : ";
        r7 = r7.append(r8);
        r7 = r7.append(r3);
        r7 = r7.toString();
        android.util.Log.e(r6, r7);
        r6 = r11.mApps;
        if (r6 == 0) goto L_0x013a;
    L_0x0194:
        r6 = r11.mApps;
        r6 = r6.mIncomingHandler;
        r2 = r6.obtainMessage();
        r2.what = r10;
        r6 = r11.mApps;
        r6 = r6.mIncomingHandler;
        r6.sendMessage(r2);
        goto L_0x013a;
    L_0x01a6:
        r6 = move-exception;
        if (r3 != 0) goto L_0x01c7;
    L_0x01a9:
        r7 = r11.mFromReceiver;
        if (r7 != 0) goto L_0x01c7;
    L_0x01ad:
        r7 = "UpdateCheckThread";
        r8 = new java.lang.StringBuilder;
        r8.<init>();
        r9 = "end update check mFromReceiver : ";
        r8 = r8.append(r9);
        r9 = r11.mFromReceiver;
        r8 = r8.append(r9);
        r8 = r8.toString();
        android.util.Log.e(r7, r8);
    L_0x01c7:
        r7 = "UpdateCheckThread";
        r8 = new java.lang.StringBuilder;
        r8.<init>();
        r9 = "end update check : ";
        r8 = r8.append(r9);
        r8 = r8.append(r3);
        r8 = r8.toString();
        android.util.Log.e(r7, r8);
        r7 = r11.mApps;
        if (r7 == 0) goto L_0x01f4;
    L_0x01e3:
        r7 = r11.mApps;
        r7 = r7.mIncomingHandler;
        r2 = r7.obtainMessage();
        r2.what = r10;
        r7 = r11.mApps;
        r7 = r7.mIncomingHandler;
        r7.sendMessage(r2);
    L_0x01f4:
        throw r6;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.launcher3.allapps.UpdateCheckThread.run():void");
    }

    public UpdateCheckThread(Context context, boolean fromReceiver, String query, AlphabeticalAppsList apps) {
        this.mContext = context;
        this.mFromReceiver = fromReceiver;
        this.mQuery = query;
        this.mApps = apps;
    }

    private boolean checkUpdate(URL url) {
        Message msg;
        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(url.openStream(), "UTF-8");
            String start_name = "";
            String end_name = "";
            SearchResultStore result = new SearchResultStore();
            GalaxyAppsContent content = null;
            Bitmap bmp = null;
            int count = 0;
            for (int eventType = parser.getEventType(); eventType != 1; eventType = parser.next()) {
                switch (eventType) {
                    case 2:
                        start_name = parser.getName();
                        if (!"resultCode".equals(start_name)) {
                            if (!"resultMsg".equals(start_name)) {
                                if (!"totalCount".equals(start_name)) {
                                    if (!"currencyUnit".equals(start_name)) {
                                        if (!"currencyUnitPrecedes".equals(start_name)) {
                                            if (!"currencyUnitHasPenny".equals(start_name)) {
                                                if (!"decimalSymbol".equals(start_name)) {
                                                    if (!"currencyUnitDivision".equals(start_name)) {
                                                        if (!"digitGroupingSymbol".equals(start_name)) {
                                                            if (!"keyword".equals(start_name)) {
                                                                if (!"productID".equals(start_name)) {
                                                                    if (!"productName".equals(start_name)) {
                                                                        if (!"productImgUrl".equals(start_name)) {
                                                                            if (!"appID".equals(start_name)) {
                                                                                if (!"version".equals(start_name)) {
                                                                                    if (!"versionCode".equals(start_name)) {
                                                                                        if (!"realContentSize".equals(start_name)) {
                                                                                            if (!"sellerName".equals(start_name)) {
                                                                                                if (!"rating".equals(start_name)) {
                                                                                                    if (!"price".equals(start_name)) {
                                                                                                        if (!"discountFlag".equals(start_name)) {
                                                                                                            if (!"discountPrice".equals(start_name)) {
                                                                                                                break;
                                                                                                            }
                                                                                                            content.discountPrice = parser.nextText();
                                                                                                            break;
                                                                                                        }
                                                                                                        content.discountFlag = parser.nextText();
                                                                                                        break;
                                                                                                    }
                                                                                                    content.price = parser.nextText();
                                                                                                    break;
                                                                                                }
                                                                                                content.rating = parser.nextText();
                                                                                                break;
                                                                                            }
                                                                                            content.sellerName = parser.nextText();
                                                                                            break;
                                                                                        }
                                                                                        content.realContentSize = parser.nextText();
                                                                                        break;
                                                                                    }
                                                                                    content.versionCode = parser.nextText();
                                                                                    break;
                                                                                }
                                                                                content.version = parser.nextText();
                                                                                break;
                                                                            }
                                                                            content.appID = parser.nextText();
                                                                            break;
                                                                        }
                                                                        content.productImgUrl = parser.nextText();
                                                                        break;
                                                                    }
                                                                    content.productName = parser.nextText();
                                                                    break;
                                                                }
                                                                content = new GalaxyAppsContent();
                                                                content.productID = parser.nextText();
                                                                break;
                                                            }
                                                            result.keyword = parser.nextText();
                                                            break;
                                                        }
                                                        result.digitGroupingSymbol = parser.nextText();
                                                        break;
                                                    }
                                                    result.currencyUnitDivision = parser.nextText();
                                                    break;
                                                }
                                                result.decimalSymbol = parser.nextText();
                                                break;
                                            }
                                            result.currencyUnitHasPenny = parser.nextText();
                                            break;
                                        }
                                        result.currencyUnitPrecedes = parser.nextText();
                                        break;
                                    }
                                    result.currencyUnit = parser.nextText();
                                    break;
                                }
                                result.totalCount = parser.nextText();
                                break;
                            }
                            result.resultMsg = parser.nextText();
                            break;
                        }
                        result.resultCode = parser.nextText();
                        break;
                    case 3:
                        end_name = parser.getName();
                        Log.d(TAG, "end_name:" + end_name);
                        if (!"content".equals(end_name)) {
                            break;
                        }
                        result.contents.add(content);
                        break;
                    default:
                        break;
                }
            }
            Log.d(TAG, "resultCode:" + result.resultCode);
            Log.d(TAG, "resultMsg:" + result.resultMsg);
            Log.d(TAG, "totalCount:" + result.totalCount);
            Log.d(TAG, "currencyUnit:" + result.currencyUnit);
            Log.d(TAG, "currencyUnitPrecedes:" + result.currencyUnitPrecedes);
            Log.d(TAG, "currencyUnitHasPenny:" + result.currencyUnitHasPenny);
            Log.d(TAG, "decimalSymbol:" + result.decimalSymbol);
            Log.d(TAG, "currencyUnitDivision:" + result.currencyUnitDivision);
            Log.d(TAG, "digitGroupingSymbol:" + result.digitGroupingSymbol);
            Log.d(TAG, "size:" + result.contents.size());
            for (int i = 0; i < result.contents.size() && count < 20; i++) {
                String str = TAG;
                String str2 = str;
                Log.d(str2, "productID:" + ((GalaxyAppsContent) result.contents.get(i)).productID);
                str = TAG;
                str2 = str;
                Log.d(str2, "productName:" + ((GalaxyAppsContent) result.contents.get(i)).productName);
                str = TAG;
                str2 = str;
                Log.d(str2, "productImgUrl:" + ((GalaxyAppsContent) result.contents.get(i)).productImgUrl);
                str = TAG;
                str2 = str;
                Log.d(str2, "panelImgUrl:" + ((GalaxyAppsContent) result.contents.get(i)).panelImgUrl);
                str = TAG;
                str2 = str;
                Log.d(str2, "edgeAppType:" + ((GalaxyAppsContent) result.contents.get(i)).edgeAppType);
                str = TAG;
                str2 = str;
                Log.d(str2, "appID:" + ((GalaxyAppsContent) result.contents.get(i)).appID);
                str = TAG;
                str2 = str;
                Log.d(str2, "versionCode:" + ((GalaxyAppsContent) result.contents.get(i)).versionCode);
                str = TAG;
                str2 = str;
                Log.d(str2, "realContentSize:" + ((GalaxyAppsContent) result.contents.get(i)).realContentSize);
                str = TAG;
                str2 = str;
                Log.d(str2, "sellerName:" + ((GalaxyAppsContent) result.contents.get(i)).sellerName);
                str = TAG;
                str2 = str;
                Log.d(str2, "rating:" + ((GalaxyAppsContent) result.contents.get(i)).rating);
                str = TAG;
                str2 = str;
                Log.d(str2, "price:" + ((GalaxyAppsContent) result.contents.get(i)).price);
                str = TAG;
                str2 = str;
                Log.d(str2, "discountFlag:" + ((GalaxyAppsContent) result.contents.get(i)).discountFlag);
                str = TAG;
                str2 = str;
                Log.d(str2, "discountPrice:" + ((GalaxyAppsContent) result.contents.get(i)).discountPrice);
                try {
                    bmp = BitmapFactory.decodeStream(new URL(((GalaxyAppsContent) result.contents.get(i)).productImgUrl).openConnection().getInputStream());
                } catch (Exception e) {
                    Log.e("Error", e.getMessage());
                }
                Log.d(TAG, "count : " + count);
                Bundle data = new Bundle();
                data.putString("title", ((GalaxyAppsContent) result.contents.get(i)).productName);
                data.putString("ID", ((GalaxyAppsContent) result.contents.get(i)).appID);
                data.putString("seller", ((GalaxyAppsContent) result.contents.get(i)).sellerName);
                data.putString("price", ((GalaxyAppsContent) result.contents.get(i)).price);
                data.putString("rating", ((GalaxyAppsContent) result.contents.get(i)).rating);
                if (result.contents.size() > 20) {
                    data.putInt(Key.SIZE, 20);
                } else {
                    data.putInt(Key.SIZE, result.contents.size());
                }
                if (this.mApps != null) {
                    msg = this.mApps.mIncomingHandler.obtainMessage();
                    msg.what = 1;
                    msg.setData(data);
                    msg.obj = bmp;
                    this.mApps.mIncomingHandler.sendMessage(msg);
                    count++;
                }
            }
            return true;
        } catch (XmlPullParserException e2) {
            Log.e(TAG, "xml parsing error:" + e2.toString());
            return false;
        } catch (SocketException e3) {
            Log.e(TAG, "network is unavailable:" + e3.toString());
            return false;
        } catch (IOException e4) {
            Log.e(TAG, "network error:" + e4.toString());
            if (this.mApps != null) {
                msg = this.mApps.mIncomingHandler.obtainMessage();
                msg.what = 5;
                this.mApps.mIncomingHandler.sendMessage(msg);
            }
            return false;
        }
    }

    private String getMCC() {
        TelephonyManager telMgr = (TelephonyManager) this.mContext.getSystemService("phone");
        String mcc = "";
        if (telMgr == null) {
            return mcc;
        }
        String networkOperator = telMgr.getSimOperator();
        if (!(networkOperator == null || networkOperator.length() == 0)) {
            mcc = networkOperator.substring(0, 3);
        }
        return mcc;
    }

    private String getMNC() {
        String mnc = "";
        TelephonyManager telMgr = (TelephonyManager) this.mContext.getSystemService("phone");
        if (telMgr == null) {
            return mnc;
        }
        String networkOperator = telMgr.getSimOperator();
        return (networkOperator == null || networkOperator.length() == 0) ? mnc : networkOperator.substring(3);
    }

//    String getCSC() {
//        return SemSystemProperties.get("ro.csc.sales_code");
//    }
}

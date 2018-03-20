package com.samsung.android.sdk.bixby;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;
import com.samsung.android.bixby.agent.IBixbyAgentAppService.Stub;
import com.samsung.android.bixby.agent.IBixbyAgentAppServiceCallback;
import java.lang.reflect.InvocationTargetException;
import org.json.JSONException;
import org.json.JSONObject;

public class BixbyAppService extends Service {
    private static final String BIXBY_AGENT_PACKAGE_NAME = "com.samsung.android.bixby.agent";
    private static final String BIXBY_COMMAND_VERSION = "1.0";
    private static final boolean DEBUG;
    private static final String TAG = (BixbyAppService.class.getSimpleName() + "_0.2.7");
    private static HandlerThread mActorThreadHandler;
    private static Signature mBixbAgentSignature = new Signature(Base64.decode("MIIE1DCCA7ygAwIBAgIJANIJlaecDarWMA0GCSqGSIb3DQEBBQUAMIGiMQswCQYDVQQGEwJLUjEUMBIGA1UECBMLU291dGggS29yZWExEzARBgNVBAcTClN1d29uIENpdHkxHDAaBgNVBAoTE1NhbXN1bmcgQ29ycG9yYXRpb24xDDAKBgNVBAsTA0RNQzEVMBMGA1UEAxMMU2Ftc3VuZyBDZXJ0MSUwIwYJKoZIhvcNAQkBFhZhbmRyb2lkLm9zQHNhbXN1bmcuY29tMB4XDTExMDYyMjEyMjUxMloXDTM4MTEwNzEyMjUxMlowgaIxCzAJBgNVBAYTAktSMRQwEgYDVQQIEwtTb3V0aCBLb3JlYTETMBEGA1UEBxMKU3V3b24gQ2l0eTEcMBoGA1UEChMTU2Ftc3VuZyBDb3Jwb3JhdGlvbjEMMAoGA1UECxMDRE1DMRUwEwYDVQQDEwxTYW1zdW5nIENlcnQxJTAjBgkqhkiG9w0BCQEWFmFuZHJvaWQub3NAc2Ftc3VuZy5jb20wggEgMA0GCSqGSIb3DQEBAQUAA4IBDQAwggEIAoIBAQDJhjhKPh8vsgZnDnjvIyIVwNJvRaInKNuZpE2hHDWsM6cf4HHEotaCWptMiLMz7ZbzxebGZtYPPulMSQiFq8+NxmD3B6q8d+rT4tDYrugQjBXNJg8uhQQsKNLyktqjxtoMe/I5HbeEGq3o/fDJ0N7893Ek5tLeCp4NLadGw2cOT/zchbcBu0dEhhuW/3MR2jYDxaEDNuVf+jS0NT7tyF9RAV4VGMZ+MJ45+HY5/xeBB/EJzRhBGmB38mlktuY/inC5YZ2wQwajI8Gh0jr4Z+GfFPVw/+Vz0OOgwrMGMqrsMXM4CZS+HjQeOpC9LkthVIH0bbOeqDgWRI7DX+sXNcHzAgEDo4IBCzCCAQcwHQYDVR0OBBYEFJMsOvcLYnoMdhC1oOdCfWz66j8eMIHXBgNVHSMEgc8wgcyAFJMsOvcLYnoMdhC1oOdCfWz66j8eoYGopIGlMIGiMQswCQYDVQQGEwJLUjEUMBIGA1UECBMLU291dGggS29yZWExEzARBgNVBAcTClN1d29uIENpdHkxHDAaBgNVBAoTE1NhbXN1bmcgQ29ycG9yYXRpb24xDDAKBgNVBAsTA0RNQzEVMBMGA1UEAxMMU2Ftc3VuZyBDZXJ0MSUwIwYJKoZIhvcNAQkBFhZhbmRyb2lkLm9zQHNhbXN1bmcuY29tggkA0gmVp5wNqtYwDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQUFAAOCAQEAMpYB/kDgNqSobMXUndjBtUFZmOcmN1OLDUMDaaxRUw9jqs6MAZoaZmFqLxuyxfq9bzEyYfOA40cWI/BT2ePFP1/W0ZZdewAOTcJEwbJ+L+mjI/8Hf1LEZ16GJHqoARhxN+MMm78BxWekKZ20vwslt9cQenuB7hAvcv9HlQFk4mdS4RTEL4udKkLnMIiX7GQOoZJO0Tq76dEgkSti9JJkk6htuUwLRvRMYWHVjC9kgWSJDFEt+yjULIVb9HDb7i2raWDK0E6B9xUl3tRs3Q81n5nEYNufAH2WzoO0shisLYLEjxJgjUaXM/BaM3VZRmnMv4pJVUTWxXAek2nAjIEBWA==", 0));
    private static final boolean mIsUserBuild = "user".equals(Build.TYPE);
    Stub mBinder = new Stub() {
        public void sendCommand(String jsonCommandFromBa) throws RemoteException {
            if (BixbyAppService.DEBUG) {
                Log.d(BixbyAppService.TAG, "BixbyAppService Command From EM: " + jsonCommandFromBa);
            } else {
                Log.d(BixbyAppService.TAG, "BixbyAppService Command From EM");
            }
            if (BixbyAppService.this.checkSenderIdentity()) {
                BixbyAppService.this.mHandler.post(new CommandHandlerRunnable(jsonCommandFromBa));
            } else {
                Log.e(BixbyAppService.TAG, "sendCommand: Unauthorized access detected!");
            }
        }

        public void setCallback(IBixbyAgentAppServiceCallback callback) throws RemoteException {
            Log.d(BixbyAppService.TAG, "BixbyAppService setCallback");
            if (BixbyAppService.this.checkSenderIdentity()) {
                BixbyAppService.this.mCallbackToBa = callback;
            } else {
                Log.e(BixbyAppService.TAG, "setCallback: Unauthorized access detected!");
            }
        }
    };
    private BixbyApi mBixbyApi;
    private IBixbyAgentAppServiceCallback mCallbackToBa;
    private Handler mHandler = new Handler();
    private boolean mIsKnoxId = false;
    BixbyApi.OnResponseCallback mResponseFromMediator = new BixbyApi.OnResponseCallback() {
        public void onResponse(String result, String msg) throws IllegalStateException {
            if (BixbyAppService.DEBUG) {
                Log.d(BixbyAppService.TAG, "Send command to EM " + result + " " + msg);
            } else {
                Log.d(BixbyAppService.TAG, "Send command to EM " + result);
            }
            if (BixbyAppService.this.mCallbackToBa == null) {
                Log.e(BixbyAppService.TAG, "No Bixby Agent response callback method registered.");
                return;
            }
            try {
                String jsonResponse = BixbyAppService.this.handleResponseCommand(result, msg);
                if (jsonResponse == null) {
                    Log.e(BixbyAppService.TAG, "Failed to handle response command to Bixby Agent.");
                    return;
                }
                if (BixbyAppService.DEBUG) {
                    Log.d(BixbyAppService.TAG, "jsonResponse: " + jsonResponse);
                }
                BixbyAppService.this.mCallbackToBa.onResponse(jsonResponse);
            } catch (Exception e) {
                Log.e(BixbyAppService.TAG, "Failed to send command to Bixby Agent.");
            }
        }
    };

    private static class CommandHandler extends Handler {
        CommandHandler(Looper looper) {
            super(looper);
        }

        public void dispatchMessage(Message msg) {
            try {
                super.dispatchMessage(msg);
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
    }

    static {
        boolean z;
        if (mIsUserBuild) {
            z = false;
        } else {
            z = true;
        }
        DEBUG = z;
    }

    public IBinder onBind(Intent intent) {
        this.mBixbyApi.onServiceBound(intent);
        return this.mBinder;
    }

    public boolean onUnbind(Intent intent) {
        this.mBixbyApi.onServiceUnbound(intent);
        return super.onUnbind(intent);
    }

    public void onCreate() {
        Log.d(TAG, "BixbyAppService onCreate package:" + getApplication().getPackageName());
        super.onCreate();
        this.mIsKnoxId = isKnoxId();
        this.mBixbyApi = BixbyApi.getInstance();
        this.mBixbyApi.setResponseCallback(this.mResponseFromMediator);
        if (BIXBY_AGENT_PACKAGE_NAME.equals(getApplication().getPackageName())) {
            if (mActorThreadHandler == null) {
                mActorThreadHandler = new HandlerThread("ExtCmdHandler");
                mActorThreadHandler.start();
            }
            this.mHandler = new CommandHandler(mActorThreadHandler.getLooper());
            this.mBixbyApi.mHandler = this.mHandler;
        }
        this.mBixbyApi.onServiceCreated();
    }

    private boolean isKnoxId() {
        Exception e;
        try {
            int userId = ((Integer) Class.forName("android.os.UserHandle").getMethod("semGetMyUserId", new Class[0]).invoke(null, new Object[0])).intValue();
            if (DEBUG) {
                Log.d(TAG, "userId = " + userId);
            }
            boolean bRet = ((Boolean) Class.forName("com.samsung.android.knox.SemPersonaManager").getMethod("isKnoxId", new Class[]{Integer.TYPE}).invoke(null, new Object[]{Integer.valueOf(userId)})).booleanValue();
            if (!DEBUG) {
                return bRet;
            }
            Log.d(TAG, "bRet = " + bRet);
            return bRet;
        } catch (ClassNotFoundException e2) {
            e = e2;
            Log.e(TAG, "isKnoxId: Can't read information on KNOX.");
            Log.d(TAG, e.toString());
            return false;
        } catch (NoSuchMethodException e3) {
            e = e3;
            Log.e(TAG, "isKnoxId: Can't read information on KNOX.");
            Log.d(TAG, e.toString());
            return false;
        } catch (SecurityException e4) {
            e = e4;
            Log.e(TAG, "isKnoxId: Can't read information on KNOX.");
            Log.d(TAG, e.toString());
            return false;
        } catch (IllegalAccessException e5) {
            e = e5;
            Log.e(TAG, "isKnoxId: Can't read information on KNOX.");
            Log.d(TAG, e.toString());
            return false;
        } catch (IllegalArgumentException e6) {
            e = e6;
            Log.e(TAG, "isKnoxId: Can't read information on KNOX.");
            Log.d(TAG, e.toString());
            return false;
        } catch (InvocationTargetException e7) {
            e = e7;
            Log.e(TAG, "isKnoxId: Can't read information on KNOX.");
            Log.d(TAG, e.toString());
            return false;
        } catch (NullPointerException e8) {
            e = e8;
            Log.e(TAG, "isKnoxId: Can't read information on KNOX.");
            Log.d(TAG, e.toString());
            return false;
        }
    }

    private String handleResponseCommand(String resultCode, String msg) {
        if ("esem_request_nlg".equals(resultCode)) {
            return wrapCommand(resultCode, msg);
        }
        if ("esem_request_tts".equals(resultCode)) {
            return wrapCommand(resultCode, msg);
        }
        if ("esem_context_result".equals(resultCode)) {
            return wrapCommand(resultCode, msg);
        }
        if ("esem_param_filling_result".equals(resultCode)) {
            return wrapCommand(resultCode, "\"result\":\"" + msg + "\"");
        }
        if ("esem_state_log".equals(resultCode)) {
            return wrapCommand(resultCode, msg);
        }
        if ("esem_client_control".equals(resultCode)) {
            return wrapCommand(resultCode, msg);
        }
        if ("state_command_result".equals(resultCode)) {
            return makeStateResultCommand(msg);
        }
        if ("esem_chatty_mode_result".equals(resultCode)) {
            return wrapCommand(resultCode, "\"result\":\"" + msg + "\"");
        }
        if ("esem_cancel_chatty_mode".equals(resultCode)) {
            return wrapCommand(resultCode, msg);
        }
        if ("esem_split_state_result".equals(resultCode)) {
            return wrapCommand(resultCode, "\"selectedStateId\":\"" + msg + "\"");
        }
        if ("esem_all_states_result".equals(resultCode)) {
            return wrapCommand(resultCode, "\"result\":\"" + msg + "\"");
        }
        if ("esem_user_confirm_result".equals(resultCode)) {
            return wrapCommand(resultCode, msg);
        }
        Log.e(TAG, "handleResponseCommand: Unsupported Command:" + resultCode);
        return null;
    }

    private String wrapCommand(String command, String body) {
        return "{" + "\"version\":\"" + "1.0" + "\"," + "\"command\":\"" + command + "\"," + "\"content\":{" + body + "}}";
    }

    private String makeStateResultCommand(String result) {
        if (this.mBixbyApi.mStateCommandJsonFromBa == null) {
            Log.e(TAG, "makeStateResultCommand: Can't make a state result command. Ignored.");
            return null;
        }
        try {
            JSONObject jsonObjSrc = new JSONObject(this.mBixbyApi.mStateCommandJsonFromBa);
            JSONObject jsonObjRes = new JSONObject();
            JSONObject jsonObjResContent = new JSONObject();
            jsonObjRes.put("version", "1.0");
            jsonObjRes.put("command", "esem_state_result");
            jsonObjRes.put("requestId", jsonObjSrc.getString("requestId"));
            jsonObjResContent.put("result", result);
            jsonObjResContent.put("state", jsonObjSrc.getJSONObject("content").getJSONObject("state"));
            jsonObjRes.put("content", jsonObjResContent);
            return jsonObjRes.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean checkSenderIdentity() throws RemoteException {
        if (!mIsUserBuild || this.mIsKnoxId) {
            return true;
        }
        int uid = Binder.getCallingUid();
        PackageManager pm = getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);
        if (packages == null) {
            return false;
        }
        for (String pName : packages) {
            if (BIXBY_AGENT_PACKAGE_NAME.equals(pName)) {
                try {
                    Signature[] sigs = pm.getPackageInfo(pName, PackageManager.GET_SIGNATURES).signatures;
                    if (sigs != null && sigs.length > 0 && mBixbAgentSignature.equals(sigs[0])) {
                        return true;
                    }
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public void onDestroy() {
        Log.d(TAG, "BixbyAppService onDestroy package:" + getApplication().getPackageName());
        this.mBixbyApi.clearData();
        this.mBixbyApi.onServiceDestroyed();
        super.onDestroy();
    }
}

package com.samsung.android.sdk.bixby;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.util.event.CheckLongPressHelper;
import com.samsung.android.sdk.bixby.TestInformationReader.TestInformation;
import com.samsung.android.sdk.bixby.data.NlgRequestInfo;
import com.samsung.android.sdk.bixby.data.ParamFilling;
import com.samsung.android.sdk.bixby.data.PathRuleInfo;
import com.samsung.android.sdk.bixby.data.ScreenStateInfo;
import com.samsung.android.sdk.bixby.data.State;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BixbyApi {
    static final String CM_ACTION = "com.samsung.android.rubin.app.intent.action.CM_LOGGING";
    private static final String CM_PACKAGE = "com.samsung.android.rubin.app";
    private static final boolean DEBUG;
    static final String RESULT_CODE_ALL_STATES = "esem_all_states_result";
    static final String RESULT_CODE_APP_CONTEXT = "esem_context_result";
    static final String RESULT_CODE_CHATTY_MODE = "esem_chatty_mode_result";
    static final String RESULT_CODE_CHATTY_MODE_CANCEL = "esem_cancel_chatty_mode";
    static final String RESULT_CODE_CLIENT_CONTROL = "esem_client_control";
    static final String RESULT_CODE_LOG_STATE = "esem_state_log";
    static final String RESULT_CODE_NLG = "esem_request_nlg";
    static final String RESULT_CODE_PARAM_FILLING = "esem_param_filling_result";
    static final String RESULT_CODE_SPLIT_STATE = "esem_split_state_result";
    static final String RESULT_CODE_STATE_COMMAND = "state_command_result";
    static final String RESULT_CODE_TTS = "esem_request_tts";
    static final String RESULT_CODE_USER_CONFIRM = "esem_user_confirm_result";
    private static final int SEQ_NUM_FIRST = 1;
    private static final int SEQ_NUM_RULE_CANCEL = -1;
    private static final int SEQ_NUM_TEST = 0;
    private static final String STR_FAILURE = "failure";
    private static final String STR_RULE_COMPLETE = "rule_complete";
    private static final String STR_SUCCESS = "success";
    private static final String TAG = (BixbyApi.class.getSimpleName() + VER);
    static final String VER = "_0.2.7";
    private static BixbyApi mInstance;
    private static final Object syncObj = new Object();
    final String TEST_INFORMATIONS = "testInformations";
    private AbstractEventMonitor mAbstractEventMonitor;
    private String mActiveAppName;
    private ChattyModeListener mChattyModeListener;
    private ConfirmResultListener mConfirmResultListener;
    private Context mContext;
    Handler mHandler = new Handler();
    private InterimStateListener mInterimListener;
    private boolean mIsPartiallyLanded = false;
    private boolean mIsRuleRunning = false;
    private boolean mIsTestMode = false;
    private boolean mIsTestRunning = false;
    private State mLastReceivedStateCmd = null;
    private ScreenStateInfo mLastScreenStateInfo = ScreenStateInfo.STATE_NOT_APPLICABLE;
    private MultiPathRuleListener mMultiPathRuleListener;
    private OnConfirmResultListener mOnConfirmResultListener;
    private OnNlgEndListener mOnNlgEndListener;
    private OnTtsResultListener mOnTtsResultListener;
    private String mPackageVersionName;
    private PathRuleInfo mPathRuleInfo = null;
    private OnResponseCallback mResponseCallback;
    private int mSendStateRetryCount;
    private Runnable mSendStateRunnable;
    private StartStateListener mStartListener;
    String mStateCommandJsonFromBa;
    private TestListener mTestListener;

    public static abstract class AbstractEventMonitor {
        public void onPathRuleStarted(PathRuleInfo pri) {
            Log.d(BixbyApi.TAG, "AbstractEventMonitor onPathRuleStarted()");
        }

        public void onServiceBound(Intent intent) {
            Log.d(BixbyApi.TAG, "AbstractEventMonitor onServiceBound()");
        }

        public void onServiceUnbound(Intent intent) {
            Log.d(BixbyApi.TAG, "AbstractEventMonitor onServiceUnbound()");
        }

        public void onServiceCreated() {
            Log.d(BixbyApi.TAG, "AbstractEventMonitor onServiceCreated()");
        }

        public void onServiceDestroyed() {
            Log.d(BixbyApi.TAG, "AbstractEventMonitor onServiceDestroyed()");
        }
    }

    public interface ChattyModeListener {
        boolean onChatTextReceived(String str, boolean z);
    }

    public interface CommonStateListener {
        void onRuleCanceled(String str);

        void onStateReceived(State state);
    }

    public enum ConfirmMode {
        SEND,
        DELETE,
        TURN_ON,
        APPLY,
        FORWARD,
        MERGE,
        DISCARD,
        RESET,
        UPDATE,
        EXECUTE,
        INQUIRE,
        SAVE,
        REPLY,
        COMMON;

        public String toString() {
            String prefix = "\"confirmMode\":";
            switch (this) {
                case SEND:
                    return "\"confirmMode\":\"send\"";
                case DELETE:
                    return "\"confirmMode\":\"delete\"";
                case TURN_ON:
                    return "\"confirmMode\":\"turnOn\"";
                case APPLY:
                    return "\"confirmMode\":\"apply\"";
                case FORWARD:
                    return "\"confirmMode\":\"forward\"";
                case MERGE:
                    return "\"confirmMode\":\"merge\"";
                case DISCARD:
                    return "\"confirmMode\":\"discard\"";
                case RESET:
                    return "\"confirmMode\":\"reset\"";
                case UPDATE:
                    return "\"confirmMode\":\"update\"";
                case EXECUTE:
                    return "\"confirmMode\":\"execute\"";
                case INQUIRE:
                    return "\"confirmMode\":\"inquire\"";
                case SAVE:
                    return "\"confirmMode\":\"save\"";
                case REPLY:
                    return "\"confirmMode\":\"reply\"";
                case COMMON:
                    return "\"confirmMode\":\"common\"";
                default:
                    return super.toString();
            }
        }
    }

    public enum ConfirmResult {
        YES,
        NO,
        CANCEL,
        OTHER,
        UNKNOWN;

        public static ConfirmResult toEnum(String result) {
            Object obj = -1;
            switch (result.hashCode()) {
                case -1367724422:
                    if (result.equals("cancel")) {
                        obj = 2;
                        break;
                    }
                    break;
                case 3521:
                    if (result.equals("no")) {
                        obj = 1;
                        break;
                    }
                    break;
                case 119527:
                    if (result.equals("yes")) {
                        obj = null;
                        break;
                    }
                    break;
                case 106069776:
                    if (result.equals("other")) {
                        obj = 3;
                        break;
                    }
                    break;
            }
            switch (obj) {
                case null:
                    return YES;
                case 1:
                    return NO;
                case 2:
                    return CANCEL;
                case 3:
                    return OTHER;
                default:
                    return UNKNOWN;
            }
        }
    }

    public interface ConfirmResultListener {
        void onResult(ConfirmResult confirmResult);
    }

    public interface MultiPathRuleListener {
        String onPathRuleSplit(List<String> list);
    }

    public enum NlgParamMode {
        NONE,
        TARGETED,
        MULTIPLE,
        CONFIRM;

        public String toString() {
            String prefix = "\"nlgParamMode\":";
            switch (this) {
                case NONE:
                    return "\"nlgParamMode\":\"none\"";
                case TARGETED:
                    return "\"nlgParamMode\":\"targeted\"";
                case MULTIPLE:
                    return "\"nlgParamMode\":\"multiple\"";
                case CONFIRM:
                    return "\"nlgParamMode\":\"confirm\"";
                default:
                    return super.toString();
            }
        }
    }

    public interface OnConfirmResultListener {
        void onConfirmResult(ConfirmResult confirmResult);
    }

    public interface OnNlgEndListener {
        void onNlgEnd();
    }

    interface OnResponseCallback {
        void onResponse(String str, String str2);
    }

    public interface OnTtsResultListener {
        void onTtsResult(TtsResult ttsResult);
    }

    public enum ResponseResults {
        SUCCESS(0),
        FAILURE(1),
        STATE_SUCCESS(0),
        STATE_FAILURE(1),
        TEST_SETUP_SUCCESS(2),
        TEST_SETUP_FAILURE(3),
        TEST_TEARDOWN_SUCCESS(4),
        TEST_TEARDOWN_FAILURE(5),
        TEST_ALL_STATES_SUCCESS(6),
        TEST_ALL_STATES_FAILURE(7),
        RULE_COMPLETE(8);
        
        private int value;

        private ResponseResults(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public String toString() {
            switch (this) {
                case SUCCESS:
                case STATE_SUCCESS:
                case TEST_SETUP_SUCCESS:
                case TEST_TEARDOWN_SUCCESS:
                case TEST_ALL_STATES_SUCCESS:
                    return BixbyApi.STR_SUCCESS;
                case FAILURE:
                case STATE_FAILURE:
                case TEST_SETUP_FAILURE:
                case TEST_TEARDOWN_FAILURE:
                case TEST_ALL_STATES_FAILURE:
                    return BixbyApi.STR_FAILURE;
                case RULE_COMPLETE:
                    return BixbyApi.STR_RULE_COMPLETE;
                default:
                    return super.toString();
            }
        }
    }

    public interface TestListener {
        void onAllStates(ArrayList<State> arrayList);

        void onSetup(Map<String, String> map);

        void onTearDown(Map<String, String> map);
    }

    public enum TtsMode {
        CUT,
        WAIT;

        public String toString() {
            String prefix = "\"ttsMode\":";
            switch (this) {
                case CUT:
                    return "\"ttsMode\":\"cut\"";
                case WAIT:
                    return "\"ttsMode\":\"wait\"";
                default:
                    return super.toString();
            }
        }
    }

    public enum TtsResult {
        COMPLETE,
        STOP_ON_ERROR,
        STOP_ON_CANCEL,
        UNKNOWN;

        public static TtsResult toEnum(String result) {
            Object obj = -1;
            switch (result.hashCode()) {
                case -2089014459:
                    if (result.equals("stop_on_error")) {
                        obj = 1;
                        break;
                    }
                    break;
                case -599445191:
                    if (result.equals(METHOD.COMPLETE)) {
                        obj = null;
                        break;
                    }
                    break;
                case -408027939:
                    if (result.equals("stop_on_cancel")) {
                        obj = 2;
                        break;
                    }
                    break;
            }
            switch (obj) {
                case null:
                    return COMPLETE;
                case 1:
                    return STOP_ON_ERROR;
                case 2:
                    return STOP_ON_CANCEL;
                default:
                    return UNKNOWN;
            }
        }
    }

    public interface InterimStateListener extends CommonStateListener {
        boolean onParamFillingReceived(ParamFilling paramFilling);

        ScreenStateInfo onScreenStatesRequested();
    }

    public interface StartStateListener extends CommonStateListener {
    }

    static {
        boolean z;
        if ("user".equals(Build.TYPE)) {
            z = false;
        } else {
            z = true;
        }
        DEBUG = z;
    }

    protected BixbyApi() {
    }

    public static BixbyApi createInstance(Context context, String activeAppName) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null.");
        }
        BixbyApi bixbyApi;
        synchronized (syncObj) {
            if (mInstance == null) {
                mInstance = new BixbyApi();
            }
            mInstance.setContext(context);
            mInstance.setActiveApp(activeAppName);
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                mInstance.setVersionName(packageInfo.versionName);
                if (DEBUG) {
                    Log.d(TAG, "createInstance: Version Name:" + packageInfo.versionName + ", " + activeAppName);
                }
            } catch (NameNotFoundException e) {
                Log.e(TAG, "createInstance: cannot get versionName from package = " + context.getPackageName());
                mInstance.setVersionName("");
            }
            bixbyApi = mInstance;
        }
        return bixbyApi;
    }

    private void setContext(Context context) {
        this.mContext = context;
    }

    private void setVersionName(String versionName) {
        this.mPackageVersionName = versionName;
    }

    public void setActiveApp(String appName) {
        if (appName == null || appName.trim().isEmpty()) {
            throw new IllegalStateException("appName should not be null or empty");
        }
        this.mActiveAppName = appName;
    }

    public String getActiveApp() {
        return this.mActiveAppName;
    }

    public static BixbyApi getInstance() throws IllegalStateException {
        BixbyApi bixbyApi;
        synchronized (syncObj) {
            if (mInstance == null) {
                throw new IllegalStateException("Instance is null. please call createInstance() for the first time.");
            }
            bixbyApi = mInstance;
        }
        return bixbyApi;
    }

    public void setStartStateListener(StartStateListener stateListener) {
        this.mStartListener = stateListener;
    }

    public void setInterimStateListener(InterimStateListener stateListener) {
        this.mInterimListener = stateListener;
    }

    public void clearInterimStateListener() {
        if (this.mInterimListener != null) {
            this.mLastScreenStateInfo = this.mInterimListener.onScreenStatesRequested();
            this.mInterimListener = null;
        }
    }

    public void setChattyModeListener(ChattyModeListener chattyModeListener) {
        if (this.mChattyModeListener != null && chattyModeListener == null) {
            sendCommandToBa(RESULT_CODE_CHATTY_MODE_CANCEL, "");
        }
        this.mChattyModeListener = chattyModeListener;
    }

    public void setMultiPathRuleListener(MultiPathRuleListener multiPathRuleListener) {
        this.mMultiPathRuleListener = multiPathRuleListener;
    }

    public void setTestListener(TestListener testListener) {
        this.mTestListener = testListener;
    }

    public void setAbstractEventMonitor(AbstractEventMonitor abstractEventMonitor) {
        this.mAbstractEventMonitor = abstractEventMonitor;
    }

    public void sendResponse(ResponseResults result) {
        if (result == ResponseResults.TEST_ALL_STATES_FAILURE || result == ResponseResults.TEST_ALL_STATES_SUCCESS) {
            sendCommandToBa(RESULT_CODE_ALL_STATES, result.toString());
        } else if (this.mLastReceivedStateCmd == null) {
            Log.e(TAG, "Invalid sendResponse call.");
        } else {
            if (result == ResponseResults.FAILURE) {
                result = ResponseResults.STATE_FAILURE;
            } else if (result == ResponseResults.SUCCESS) {
                result = ResponseResults.STATE_SUCCESS;
            }
            sendCommandToBa(RESULT_CODE_STATE_COMMAND, result.toString());
            handleTestResponse(result, this.mLastReceivedStateCmd);
            if (this.mLastReceivedStateCmd.isLastState().booleanValue() || result == ResponseResults.STATE_FAILURE || result == ResponseResults.TEST_SETUP_FAILURE) {
                setRuleRunning(false);
                setTestRunning(false);
            }
            setPartiallyLanded(false);
            this.mLastReceivedStateCmd = null;
        }
    }

    private void handleTestResponse(ResponseResults result, State lastReceivedStateCmd) {
        if (lastReceivedStateCmd.isLastState().booleanValue()) {
            setRuleRunning(false);
            if (isTestRunning()) {
                setTestRunning(false);
            }
        } else if (lastReceivedStateCmd.getSeqNum().intValue() == 0 && result == ResponseResults.TEST_SETUP_SUCCESS) {
            setTestRunning(true);
        }
    }

    public void requestNlg(NlgRequestInfo nri, NlgParamMode mode) throws IllegalArgumentException {
        if (nri == null) {
            throw new IllegalArgumentException("NlgRequestInfo cannot be null.");
        }
        setOnNlgEndListener(null);
        setConfirmResultListener(null);
        setOnConfirmResultListener(null);
        sendCommandToBa(RESULT_CODE_NLG, setNlgData(nri, mode, null));
    }

    public void requestNlg(NlgRequestInfo nri, NlgParamMode mode, OnNlgEndListener listener) throws IllegalArgumentException {
        if (nri == null) {
            throw new IllegalArgumentException("NlgRequestInfo cannot be null.");
        } else if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null.");
        } else {
            setOnNlgEndListener(listener);
            setConfirmResultListener(null);
            setOnConfirmResultListener(null);
            sendCommandToBa(RESULT_CODE_NLG, setNlgData(nri, mode, listener));
        }
    }

    private String setNlgData(NlgRequestInfo nri, NlgParamMode mode, OnNlgEndListener listener) {
        String needCallback;
        String currState = "NONE";
        if (mode == NlgParamMode.MULTIPLE) {
            currState = getNlgStateInfo();
        }
        String stateInfo = "\"currentStateIds\":\"" + currState + "\"";
        if (listener != null) {
            needCallback = "\"needResultCallback\":true";
        } else {
            needCallback = "\"needResultCallback\":false";
        }
        return String.format("\"requestedAppName\":\"%s\",%s,%s,%s,%s", new Object[]{this.mActiveAppName, nri.toString(), stateInfo, mode.toString(), needCallback});
    }

    public void requestConfirm(NlgRequestInfo nri, ConfirmMode mode, ConfirmResultListener listener) throws IllegalArgumentException {
        requestConfirm(nri, mode, null, listener);
    }

    public void requestConfirm(NlgRequestInfo nri, ConfirmMode mode, String nextRuleId, ConfirmResultListener listener) throws IllegalArgumentException {
        if (nri == null) {
            throw new IllegalArgumentException("NlgRequestInfo cannot be null.");
        } else if (listener == null) {
            throw new IllegalArgumentException("ConfirmResultListener cannot be null.");
        } else {
            String ruleIdStr;
            if (nextRuleId == null) {
                ruleIdStr = "";
            } else {
                ruleIdStr = ",\"nextRuleId\":\"" + nextRuleId + "\"";
            }
            String stateInfo = "\"currentStateIds\":\"" + getNlgStateInfo() + "\"";
            String ret = String.format("\"requestedAppName\":\"%s\",%s,%s,%s,%s%s", new Object[]{this.mActiveAppName, nri.toString(), stateInfo, NlgParamMode.CONFIRM.toString(), mode.toString(), ruleIdStr});
            setConfirmResultListener(listener);
            sendCommandToBa(RESULT_CODE_NLG, ret);
        }
    }

    public void requestConfirm(NlgRequestInfo nri, ConfirmMode mode, OnConfirmResultListener listener) throws IllegalArgumentException {
        requestConfirm(nri, mode, null, listener);
    }

    public void requestConfirm(NlgRequestInfo nri, ConfirmMode mode, String nextRuleId, OnConfirmResultListener listener) throws IllegalArgumentException {
        if (nri == null) {
            throw new IllegalArgumentException("NlgRequestInfo cannot be null.");
        } else if (listener == null) {
            throw new IllegalArgumentException("ConfirmResultListener cannot be null.");
        } else {
            String ruleIdStr;
            if (nextRuleId == null) {
                ruleIdStr = "";
            } else {
                ruleIdStr = ",\"nextRuleId\":\"" + nextRuleId + "\"";
            }
            String stateInfo = "\"currentStateIds\":\"" + getNlgStateInfo() + "\"";
            String ret = String.format("\"requestedAppName\":\"%s\",%s,%s,%s,%s%s", new Object[]{this.mActiveAppName, nri.toString(), stateInfo, NlgParamMode.CONFIRM.toString(), mode.toString(), ruleIdStr});
            setOnConfirmResultListener(listener);
            sendCommandToBa(RESULT_CODE_NLG, ret);
        }
    }

    public void requestTts(String text, TtsMode mode) throws IllegalArgumentException {
        if (TextUtils.isEmpty(text)) {
            throw new IllegalArgumentException("text cannot be null or empty.");
        }
        setOnTtsResultListener(null);
        requestTtsInternal(text, mode);
    }

    public void requestTts(String text, TtsMode mode, OnTtsResultListener listener) throws IllegalArgumentException {
        if (TextUtils.isEmpty(text)) {
            throw new IllegalArgumentException("text cannot be null or empty.");
        } else if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null.");
        } else {
            setOnTtsResultListener(listener);
            requestTtsInternal(text, mode);
        }
    }

    private void requestTtsInternal(String text, TtsMode mode) {
        String encodedText = JSONObject.quote(text);
        sendCommandToBa(RESULT_CODE_TTS, String.format("\"appName\":\"%s\",\"text\":%s,%s", new Object[]{getActiveApp(), encodedText, mode.toString()}));
    }

    public boolean isTestMode() {
        return this.mIsTestMode;
    }

    public void logEnterState(String stateId) throws IllegalStateException {
        logState("state_enter", stateId);
    }

    public void logEnterStates(Set<String> stateIdSet) throws IllegalStateException {
        StringBuilder statesBuilder = new StringBuilder();
        if (!(stateIdSet == null || stateIdSet.isEmpty())) {
            for (String stateId : stateIdSet) {
                statesBuilder.append(stateId).append(",");
            }
            statesBuilder.deleteCharAt(statesBuilder.length() - 1);
        }
        logState("state_enter", statesBuilder.toString());
    }

    public void logExitState(String stateId) throws IllegalStateException {
        logState("state_exit", stateId);
    }

    public void logExitStates(Set<String> stateIdSet) throws IllegalStateException {
        StringBuilder statesBuilder = new StringBuilder();
        if (!(stateIdSet == null || stateIdSet.isEmpty())) {
            for (String stateId : stateIdSet) {
                statesBuilder.append(stateId).append(",");
            }
            statesBuilder.deleteCharAt(statesBuilder.length() - 1);
        }
        logState("state_exit", statesBuilder.toString());
    }

    public void logOutputParam(String paramName, String value) throws IllegalStateException {
        Intent i = createIntent("output_param");
        i.putExtra("paramName", paramName);
        i.putExtra("paramValue", value);
        this.mContext.sendBroadcast(i);
    }

    private void logState(String command, String stateIds) throws IllegalStateException {
        if (stateIds == null) {
            throw new IllegalArgumentException("Log value cannot be null.");
        }
        try {
            sendCommandToBa(RESULT_CODE_LOG_STATE, createLogStateData(command, stateIds));
        } catch (Exception e) {
            Log.e(TAG, "logState: Can't send log to BixbyAgent.");
        }
        Intent i = createIntent(command);
        i.putExtra("stateIds", stateIds);
        this.mContext.sendBroadcast(i);
    }

    private String createLogStateData(String command, String stateIds) {
        return new StringBuffer().append("\"appName\":\"").append(this.mActiveAppName).append("\",").append("\"logType\":\"").append(command).append("\",").append("\"stateIds\":\"").append(stateIds).append("\"").toString();
    }

    private Intent createIntent(String command) throws IllegalStateException {
        Intent intent = new Intent();
        intent.setAction(CM_ACTION);
        intent.setPackage(CM_PACKAGE);
        intent.putExtra("command", command);
        intent.putExtra("appName", this.mActiveAppName);
        intent.putExtra("appVersion", this.mPackageVersionName);
        intent.putExtra("timestamp", getTimestamp());
        return intent;
    }

    private Long getTimestamp() {
        return Long.valueOf(System.currentTimeMillis());
    }

    private void handleTestState(String jsonState) {
        this.mIsTestMode = true;
        Log.d(TAG, "handleTestState: SeqNo 0 found. isTestMode true");
        try {
            JSONObject jObj = new JSONObject(jsonState);
            if (!jObj.has("testInformations")) {
                sendResponse(ResponseResults.TEST_SETUP_SUCCESS);
            } else if (this.mTestListener == null) {
                sendResponse(ResponseResults.TEST_SETUP_FAILURE);
            } else {
                List<TestInformation> tiList = TestInformationReader.read(jObj.get("testInformations").toString());
                if (tiList == null || tiList.isEmpty()) {
                    sendResponse(ResponseResults.TEST_SETUP_FAILURE);
                    return;
                }
                for (TestInformation ti : tiList) {
                    if (TestInformation.TYPE_SETUP.equals(ti.getType())) {
                        if (ti.getContent() == null) {
                            sendResponse(ResponseResults.TEST_SETUP_FAILURE);
                            return;
                        } else {
                            this.mTestListener.onSetup(ti.getContent());
                            return;
                        }
                    } else if (!TestInformation.TYPE_TEARDOWN.equals(ti.getType())) {
                        Log.d(TAG, "Unsupported Item:" + ti.getType());
                    } else if (ti.getContent() == null) {
                        sendResponse(ResponseResults.TEST_SETUP_FAILURE);
                        return;
                    } else {
                        this.mTestListener.onTearDown(ti.getContent());
                        return;
                    }
                }
                sendResponse(ResponseResults.TEST_SETUP_SUCCESS);
            }
        } catch (JSONException e) {
            Log.e(TAG, "handleTestState: Invalid JSON:" + jsonState);
            sendResponse(ResponseResults.TEST_SETUP_FAILURE);
        }
    }

    void sendState(String jsonState) {
        setRuleRunning(true);
        State state = StateReader.read(jsonState);
        this.mLastReceivedStateCmd = state;
        if (state.getSeqNum().intValue() == -1) {
            handleRuleCancel(state);
            return;
        }
        clearListeners();
        if (state.getSeqNum().intValue() == 0) {
            handleTestState(jsonState);
        } else if (state.getSeqNum().intValue() == 1) {
            handleFirstState(state);
        } else {
            handleStates(state);
        }
    }

    private void handleStates(final State state) {
        if (this.mSendStateRunnable != null) {
            Log.e(TAG, "sendState: Remove pending state.");
            this.mHandler.removeCallbacks(this.mSendStateRunnable);
        }
        this.mSendStateRetryCount = 0;
        this.mSendStateRunnable = new Runnable() {
            public void run() {
                if (BixbyApi.this.mInterimListener != null) {
                    Log.e(BixbyApi.TAG, "sendState: Call onStateReceived() :" + state.getStateId());
                    BixbyApi.this.mInterimListener.onStateReceived(state);
                    BixbyApi.this.mSendStateRunnable = null;
                } else if (BixbyApi.this.mSendStateRetryCount > 33) {
                    Log.e(BixbyApi.TAG, "sendState: Failed to call onStateReceived()");
                    BixbyApi.this.mSendStateRunnable = null;
                } else {
                    BixbyApi.this.mHandler.postDelayed(this, (long) CheckLongPressHelper.LONG_PRESS_TIME_OUT_DEFAULT);
                    BixbyApi.this.mSendStateRetryCount = BixbyApi.this.mSendStateRetryCount + 1;
                    Log.e(BixbyApi.TAG, "sendState: Interim Listener is not set. Waiting for it.");
                }
            }
        };
        this.mHandler.post(this.mSendStateRunnable);
        if (state.isLastState().booleanValue()) {
            this.mIsTestMode = false;
        }
    }

    private void handleFirstState(State state) {
        if (this.mStartListener != null) {
            this.mStartListener.onStateReceived(state);
            return;
        }
        Log.v(TAG, "sendState: The first state arrived but StartListener has not been set.");
        sendCommandToBa(RESULT_CODE_STATE_COMMAND, ResponseResults.STATE_FAILURE.toString());
    }

    private void handleRuleCancel(State state) {
        setRuleRunning(false);
        if (this.mOnConfirmResultListener != null) {
            this.mOnConfirmResultListener.onConfirmResult(ConfirmResult.CANCEL);
        } else if (this.mConfirmResultListener != null) {
            this.mConfirmResultListener.onResult(ConfirmResult.CANCEL);
        }
        if (this.mInterimListener == null && this.mStartListener == null) {
            Log.e(TAG, "sendState: No listener is set.");
            return;
        }
        if (this.mInterimListener != null) {
            this.mInterimListener.onRuleCanceled(state.getRuleId());
        }
        if (this.mStartListener != null) {
            this.mStartListener.onRuleCanceled(state.getRuleId());
        }
        clearListeners();
    }

    void sendChatText(String chatText, boolean directSend) {
        String ret = ResponseResults.FAILURE.toString();
        if (this.mChattyModeListener == null) {
            Log.d(TAG, "sendChatText: ChattyModeListener is null.");
        } else if (this.mChattyModeListener.onChatTextReceived(chatText, directSend)) {
            ret = ResponseResults.SUCCESS.toString();
        }
        sendCommandToBa(RESULT_CODE_CHATTY_MODE, ret);
    }

    void sendMultiStates(JSONArray jsonArray) throws JSONException {
        ArrayList<String> stateIds = new ArrayList();
        if (jsonArray != null) {
            int len = jsonArray.length();
            for (int i = 0; i < len; i++) {
                stateIds.add(jsonArray.get(i).toString());
            }
        }
        String selected = "";
        if (this.mMultiPathRuleListener == null) {
            Log.d(TAG, "sendMultiStates: MultiPathRuleListener is null.");
        } else {
            selected = this.mMultiPathRuleListener.onPathRuleSplit(stateIds);
            if (selected == null) {
                selected = "";
            }
        }
        sendCommandToBa(RESULT_CODE_SPLIT_STATE, selected);
    }

    void sendAllStates(JSONArray jsonArray) throws JSONException {
        ArrayList<State> allStates = new ArrayList();
        if (jsonArray != null) {
            int len = jsonArray.length();
            for (int i = 0; i < len; i++) {
                allStates.add(StateReader.read(jsonArray.get(i).toString()));
            }
        }
        if (this.mTestListener == null || allStates.isEmpty()) {
            Log.d(TAG, "sendAllStates: mTestListener is null.");
            sendCommandToBa(RESULT_CODE_ALL_STATES, ResponseResults.TEST_ALL_STATES_FAILURE.toString());
            return;
        }
        this.mTestListener.onAllStates(allStates);
    }

    void setResponseCallback(OnResponseCallback callback) {
        this.mResponseCallback = callback;
    }

    void requestContext() {
        String msg;
        boolean ret = false;
        StringBuilder sb = new StringBuilder();
        String states;
        if (this.mInterimListener != null) {
            ScreenStateInfo ssi = this.mInterimListener.onScreenStatesRequested();
            sb.append("{").append("\"appName\":\"").append(this.mActiveAppName).append("\"");
            if (ssi != ScreenStateInfo.STATE_NOT_APPLICABLE) {
                states = ssi.toString();
                if (states != null) {
                    sb.append(",").append(states);
                    ret = true;
                } else {
                    Log.e(TAG, "requestContext: No state ids.");
                }
            } else {
                Log.e(TAG, "requestContext: STATE_NOT_APPLICABLE");
            }
        } else {
            sb.append("{").append("\"appName\":\"").append(this.mActiveAppName).append("\"");
            Log.e(TAG, "requestContext: InterimListener is not set. ");
            if (this.mLastScreenStateInfo != ScreenStateInfo.STATE_NOT_APPLICABLE) {
                Log.e(TAG, "requestContext: Lastly backed up Screen State info used.");
                states = this.mLastScreenStateInfo.toString();
                if (states != null) {
                    sb.append(",").append(states);
                    sb.append(",\"isBackedUpState\":true");
                    ret = true;
                } else {
                    Log.e(TAG, "requestContext: No state ids.");
                }
            }
        }
        if (this.mChattyModeListener != null) {
            sb.append(",\"isChattyModeSupported\":true");
        }
        sb.append("}");
        if (ret) {
            msg = "\"result\": \"" + ResponseResults.SUCCESS.toString() + "\"";
        } else {
            msg = "\"result\": \"" + ResponseResults.FAILURE.toString() + "\"";
        }
        sendCommandToBa(RESULT_CODE_APP_CONTEXT, msg + ",\"appContext\":" + sb.toString());
    }

    void sendParamFilling(ParamFilling pf) {
        String ret = ResponseResults.FAILURE.toString();
        if (this.mInterimListener == null) {
            Log.d(TAG, "ParamFilling: InterimListener is null.");
        } else if (this.mInterimListener.onParamFillingReceived(pf)) {
            ret = ResponseResults.SUCCESS.toString();
        }
        sendCommandToBa(RESULT_CODE_PARAM_FILLING, ret);
    }

    void onServiceBound(Intent intent) {
        if (this.mAbstractEventMonitor != null) {
            this.mAbstractEventMonitor.onServiceBound(intent);
        }
    }

    void onServiceUnbound(Intent intent) {
        if (this.mAbstractEventMonitor != null) {
            this.mAbstractEventMonitor.onServiceUnbound(intent);
        }
    }

    void onServiceCreated() {
        if (this.mAbstractEventMonitor != null) {
            this.mAbstractEventMonitor.onServiceCreated();
        }
    }

    void onServiceDestroyed() {
        if (this.mAbstractEventMonitor != null) {
            this.mAbstractEventMonitor.onServiceDestroyed();
        }
    }

    public PathRuleInfo getPathRuleInfo() {
        if (this.mIsRuleRunning) {
            return this.mPathRuleInfo;
        }
        return null;
    }

    void handlePathRuleInfo(PathRuleInfo pri) {
        this.mPathRuleInfo = pri;
        if (this.mAbstractEventMonitor != null) {
            this.mAbstractEventMonitor.onPathRuleStarted(pri);
        }
    }

    private String getNlgStateInfo() {
        StringBuilder statesBuilder = new StringBuilder();
        if (this.mInterimListener != null) {
            ScreenStateInfo ssi = this.mInterimListener.onScreenStatesRequested();
            if (ssi == ScreenStateInfo.STATE_NOT_APPLICABLE) {
                throw new IllegalArgumentException("Partial Landing handler requires the current state ID. onScreenStatesRequested() is not allowed to return null.");
            }
            LinkedHashSet<String> stateIds = ssi.getStates();
            if (!(stateIds == null || stateIds.isEmpty())) {
                Iterator it = stateIds.iterator();
                while (it.hasNext()) {
                    statesBuilder.append((String) it.next()).append(",");
                }
                statesBuilder.deleteCharAt(statesBuilder.length() - 1);
            }
        } else {
            statesBuilder.append("");
        }
        return statesBuilder.toString();
    }

    public boolean isRuleRunning() {
        return this.mIsRuleRunning;
    }

    private void setRuleRunning(boolean isRuleRunning) {
        this.mIsRuleRunning = isRuleRunning;
    }

    public boolean isTestRunning() {
        return this.mIsTestRunning;
    }

    private void setTestRunning(boolean isTestRunning) {
        this.mIsTestRunning = isTestRunning;
    }

    public static boolean isBixbySupported() {
        Exception e;
        try {
            Class<?> semFloatingFeature = Class.forName("com.samsung.android.feature.SemFloatingFeature");
            Object semFloatingFeatureInstance = semFloatingFeature.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
            boolean ret = ((Boolean) semFloatingFeature.getMethod("getBoolean", new Class[]{String.class, Boolean.TYPE}).invoke(semFloatingFeatureInstance, new Object[]{"SEC_FLOATING_FEATURE_COMMON_SUPPORT_BIXBY", Boolean.valueOf(false)})).booleanValue();
            Log.d(TAG, "isBixbySupported:" + ret);
            return ret;
        } catch (ClassNotFoundException e2) {
            e = e2;
        } catch (NoSuchMethodException e3) {
            e = e3;
        } catch (SecurityException e4) {
            e = e4;
        } catch (IllegalAccessException e5) {
            e = e5;
        } catch (IllegalArgumentException e6) {
            e = e6;
        } catch (InvocationTargetException e7) {
            e = e7;
        } catch (NullPointerException e8) {
            e = e8;
        }
        Log.d(TAG, "isBixbySupported: Can't read information on Bixby support.");
        Log.d(TAG, e.toString());
        return false;
    }

    public void setAppVisible(boolean isAppVisible) {
        if (isRuleRunning()) {
            sendCommandToBa(RESULT_CODE_CLIENT_CONTROL, "\"appVisible\":" + isAppVisible);
            return;
        }
        Log.d(TAG, "setAppVisible: Path Rule is not running.");
    }

    public void setAppTouchable(boolean isAppTouchable) {
        if (isRuleRunning()) {
            sendCommandToBa(RESULT_CODE_CLIENT_CONTROL, "\"appTouchable\":" + isAppTouchable);
            return;
        }
        Log.d(TAG, "setAppTouchable: Path Rule is not running.");
    }

    public void extendTimeout(int nSecs) {
        if (!isRuleRunning()) {
            Log.e(TAG, "extendTimeout: Path Rule is not running.");
        } else if (nSecs < 1) {
            Log.e(TAG, "extendTimeout: Timeout value is not in the valid range. ");
        } else {
            sendCommandToBa(RESULT_CODE_CLIENT_CONTROL, "\"pathRuleTimeout\":" + nSecs);
        }
    }

    private void notifyActivityLaunchState(boolean isLaunched) {
        if (isRuleRunning()) {
            sendCommandToBa(RESULT_CODE_CLIENT_CONTROL, "\"activityLaunched\":" + isLaunched);
            return;
        }
        Log.e(TAG, "activityLaunched: Path Rule is not running.");
    }

    public boolean isPartiallyLanded() {
        return this.mIsPartiallyLanded;
    }

    void setPartiallyLanded(boolean bLanded) {
        this.mIsPartiallyLanded = bLanded;
    }

    private void sendCommandToBa(String cmd, String msg) {
        if (this.mResponseCallback != null) {
            this.mResponseCallback.onResponse(cmd, msg);
        } else if (!cmd.equals(RESULT_CODE_LOG_STATE) && !cmd.equals(RESULT_CODE_CHATTY_MODE_CANCEL)) {
            Log.e(TAG, "sendCommandToBa: Bixby Agent is not connected.");
        }
    }

    void sendUserConfirm(String appName, String result) {
        boolean isSuccessful = false;
        ConfirmResult cr = ConfirmResult.toEnum(result);
        Log.e(TAG, "mOnConfirmResultListener:" + this.mOnConfirmResultListener);
        Log.e(TAG, "mConfirmResultListener:" + this.mConfirmResultListener);
        if (this.mOnConfirmResultListener == null && this.mConfirmResultListener == null) {
            Log.e(TAG, "Confirm Result Listener null. Ignored.");
        } else if (cr != ConfirmResult.UNKNOWN) {
            isSuccessful = true;
        } else {
            Log.e(TAG, "Invalid Confirmation Result: " + result + ". Ignored");
        }
        String str = "\"appName\":\"%s\",\"result\":\"%s\"";
        Object[] objArr = new Object[2];
        objArr[0] = appName;
        objArr[1] = isSuccessful ? STR_SUCCESS : STR_FAILURE;
        sendCommandToBa(RESULT_CODE_USER_CONFIRM, String.format(str, objArr));
        if (isSuccessful) {
            if (this.mOnConfirmResultListener != null) {
                this.mOnConfirmResultListener.onConfirmResult(cr);
            } else if (this.mConfirmResultListener != null) {
                this.mConfirmResultListener.onResult(cr);
            }
            Log.d(TAG, "Confirmation Result called: " + result);
            setConfirmResultListener(null);
            setOnConfirmResultListener(null);
        }
    }

    private void setConfirmResultListener(ConfirmResultListener listener) {
        this.mConfirmResultListener = listener;
    }

    private void setOnConfirmResultListener(OnConfirmResultListener listener) {
        Log.e(TAG, "setOnConfirmResultListener:" + listener);
        this.mOnConfirmResultListener = listener;
    }

    private void setOnTtsResultListener(OnTtsResultListener listener) {
        this.mOnTtsResultListener = listener;
    }

    private void setOnNlgEndListener(OnNlgEndListener listener) {
        this.mOnNlgEndListener = listener;
    }

    void sendTtsResult(String result) {
        if (this.mOnTtsResultListener == null) {
            Log.e(TAG, "unexpected TTS result. Ignored.");
            return;
        }
        this.mOnTtsResultListener.onTtsResult(TtsResult.toEnum(result));
        setOnTtsResultListener(null);
    }

    void sendNlgEnd() {
        Log.d(TAG, "sendNlgEnd");
        if (this.mOnNlgEndListener == null) {
            Log.e(TAG, "unexpected NLG End result. Ignored.");
            return;
        }
        this.mOnNlgEndListener.onNlgEnd();
        setOnNlgEndListener(null);
    }

    void clearData() {
        setRuleRunning(false);
        setTestRunning(false);
        setResponseCallback(null);
        setPartiallyLanded(false);
        clearListeners();
    }

    private void clearListeners() {
        setConfirmResultListener(null);
        setOnConfirmResultListener(null);
        setOnTtsResultListener(null);
        setOnNlgEndListener(null);
    }
}

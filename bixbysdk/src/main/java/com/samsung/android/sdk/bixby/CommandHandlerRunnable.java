package com.samsung.android.sdk.bixby;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

class CommandHandlerRunnable implements Runnable {
    static final String CMD_ALL_STATES = "emes_all_states";
    static final String CMD_CHATTY_MODE = "emes_chatty_mode";
    static final String CMD_CONTEXT = "emes_request_context";
    static final String CMD_FILLING = "emes_request_param_filling";
    static final String CMD_NLG_RESULT = "emes_nlg_end_result";
    static final String CMD_PARTIAL_LANDING_STATE = "emes_partial_landing_state";
    static final String CMD_PATH_RULE_INFO = "emes_pathrule_info";
    static final String CMD_SPLIT_STATE = "emes_split_state";
    static final String CMD_STATE = "emes_state";
    static final String CMD_TTS_RESULT = "emes_tts_result";
    static final String CMD_USER_CONFIRM = "emes_user_confirm";
    private static final String TAG = (CommandHandlerRunnable.class.getSimpleName() + "_0.2.7");
    private BixbyApi mBixbyApi = BixbyApi.getInstance();
    private final String mJsonCommand;

    CommandHandlerRunnable(String jsonCommand) {
        this.mJsonCommand = jsonCommand;
    }

    public void run() {
        try {
            JSONObject jsonObj = new JSONObject(this.mJsonCommand);
            String command = jsonObj.getString("command");
            Log.d(TAG, "Command from EM: " + command);
            if (command.equals(CMD_CONTEXT)) {
                this.mBixbyApi.requestContext();
                return;
            }
            JSONObject jsonContent = getContent(jsonObj);
            if (command.equals(CMD_STATE)) {
                this.mBixbyApi.mStateCommandJsonFromBa = this.mJsonCommand;
                this.mBixbyApi.sendState(jsonContent.get("state").toString());
            } else if (command.equals(CMD_FILLING)) {
                this.mBixbyApi.sendParamFilling(ParamFillingReader.read(jsonContent.get("slotFillingResult").toString()));
            } else if (command.equals(CMD_PATH_RULE_INFO)) {
                this.mBixbyApi.handlePathRuleInfo(PathRuleInfoReader.read(jsonContent.get("pathRuleInfo").toString()));
            } else if (command.equals(CMD_CHATTY_MODE)) {
                this.mBixbyApi.sendChatText(jsonContent.get("utterance").toString(), jsonContent.getBoolean("directSend"));
            } else if (command.equals(CMD_SPLIT_STATE)) {
                this.mBixbyApi.sendMultiStates(jsonContent.getJSONArray("stateIds"));
            } else if (command.equals(CMD_ALL_STATES)) {
                this.mBixbyApi.sendAllStates(jsonContent.getJSONArray("states"));
            } else if (command.equals(CMD_PARTIAL_LANDING_STATE)) {
                this.mBixbyApi.setPartiallyLanded(jsonContent.getBoolean("isLanded"));
            } else if (command.equals(CMD_USER_CONFIRM)) {
                this.mBixbyApi.sendUserConfirm(jsonContent.get("appName").toString(), jsonContent.get("result").toString());
            } else if (command.equals(CMD_TTS_RESULT)) {
                this.mBixbyApi.sendTtsResult(jsonContent.get("result").toString());
            } else if (command.equals(CMD_NLG_RESULT)) {
                this.mBixbyApi.sendNlgEnd();
            } else {
                Log.e(TAG, "Unknown command arrived : " + command);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private JSONObject getContent(JSONObject jsonObj) throws JSONException {
        return jsonObj.getJSONObject("content");
    }
}

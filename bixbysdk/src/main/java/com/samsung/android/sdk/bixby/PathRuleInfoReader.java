package com.samsung.android.sdk.bixby;

import com.samsung.android.sdk.bixby.data.PathRuleInfo;
import com.samsung.android.sdk.bixby.data.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class PathRuleInfoReader {
    private static final String APPS = "apps";
    private static final String EXECUTION_TYPE = "executionType";
    private static final String INTENT = "intent";
    private static final String IS_CALLEE_PATH_RULE = "isCalleePathRule";
    private static final String IS_FROM_SIMULATOR = "isFromSimulator";
    private static final String IS_ROOT = "isRoot";
    private static final String PATH_RULE_ID = "pathRuleId";
    private static final String PATH_RULE_NAME = "pathRuleName";
    private static final String PATH_RULE_STATES = "states";
    private static final String SAMPLE_UTTERANCE = "sampleUtterance";
    private static final String UTTERANCE = "utterance";

    PathRuleInfoReader() {
    }

    public static PathRuleInfo read(String json) throws IllegalArgumentException {
        Exception e;
        String executionType = null;
        boolean isCalleePathRule = false;
        boolean isFromSimulator = false;
        List<State> stateList = null;
        try {
            int idx;
            JSONObject jSONObject = new JSONObject(json);
            String pathRuleId = jSONObject.getString(PATH_RULE_ID);
            String pathRuleName = jSONObject.getString(PATH_RULE_NAME);
            String intent = jSONObject.getString("intent");
            String utterance = jSONObject.getString(UTTERANCE);
            String sampleUtterance = jSONObject.getString(SAMPLE_UTTERANCE);
            JSONArray jsonArray = jSONObject.getJSONArray(APPS);
            String[] apps = new String[jsonArray.length()];
            for (idx = 0; idx < jsonArray.length(); idx++) {
                apps[idx] = jsonArray.optString(idx);
            }
            if (jSONObject.has(EXECUTION_TYPE)) {
                executionType = jSONObject.getString(EXECUTION_TYPE);
            }
            boolean isRoot = jSONObject.getBoolean(IS_ROOT);
            if (jSONObject.has(IS_CALLEE_PATH_RULE)) {
                isCalleePathRule = jSONObject.getBoolean(IS_CALLEE_PATH_RULE);
            }
            if (jSONObject.has(IS_FROM_SIMULATOR)) {
                isFromSimulator = jSONObject.getBoolean(IS_FROM_SIMULATOR);
            }
            if (jSONObject.has(PATH_RULE_STATES)) {
                JSONArray stateArr = jSONObject.getJSONArray(PATH_RULE_STATES);
                List<State> stateList2 = new ArrayList();
                idx = 0;
                while (idx < stateArr.length()) {
                    try {
                        stateList2.add(StateReader.read(stateArr.optString(idx)));
                        idx++;
                    } catch (Exception e2) {
                        e = e2;
                        stateList = stateList2;
                    }
                }
                Collections.sort(stateList2, new Comparator<State>() {
                    public int compare(State o1, State o2) {
                        return o1.getSeqNum().compareTo(o2.getSeqNum());
                    }
                });
                stateList = stateList2;
            }
            return new PathRuleInfo(pathRuleId, pathRuleName, intent, utterance, sampleUtterance, apps, executionType, isRoot, isCalleePathRule, isFromSimulator, stateList);
        } catch (JSONException e3) {
            e = e3;
            throw new IllegalArgumentException(e.toString());
        }
    }
}

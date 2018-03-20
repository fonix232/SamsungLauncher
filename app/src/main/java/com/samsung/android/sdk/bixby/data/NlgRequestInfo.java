package com.samsung.android.sdk.bixby.data;

import java.util.HashMap;
import java.util.Map.Entry;

public class NlgRequestInfo {
    private String mRequestStateId;
    private HashMap<String, String> mResultParams = new HashMap();
    private String mRuleId = null;
    private HashMap<String, Attribute> mScreenParams = new HashMap();

    public static class Attribute {
        public String name;
        public String value;

        private Attribute(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    public NlgRequestInfo(String requestedStateId) throws IllegalArgumentException {
        if (requestedStateId == null || requestedStateId.trim().isEmpty()) {
            throw new IllegalArgumentException("The input parameter is null or empty.");
        }
        this.mRequestStateId = requestedStateId;
    }

    public NlgRequestInfo(String requestedStateId, String ruleId) throws IllegalArgumentException {
        if (requestedStateId == null || requestedStateId.trim().isEmpty()) {
            throw new IllegalArgumentException("The input parameter is null or empty.");
        } else if (ruleId == null || ruleId.trim().isEmpty()) {
            throw new IllegalArgumentException("The input parameter is null or empty.");
        } else {
            this.mRequestStateId = requestedStateId;
            this.mRuleId = ruleId;
        }
    }

    public HashMap<String, Attribute> getScreenParams() {
        return this.mScreenParams;
    }

    public HashMap<String, String> getResultParams() {
        return this.mResultParams;
    }

    public String getRequestStateId() {
        return this.mRequestStateId;
    }

    public String getRuleId() {
        return this.mRuleId;
    }

    public NlgRequestInfo addScreenParam(String name, String attrName, String attrValue) throws IllegalArgumentException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("The input parameter is null or empty.");
        } else if (this.mScreenParams.containsKey(name)) {
            throw new IllegalArgumentException("The screen parameter name is duplicated. " + name);
        } else {
            this.mScreenParams.put(name, new Attribute(attrName, attrValue));
            return this;
        }
    }

    public NlgRequestInfo addResultParam(String name, String value) {
        if (name == null || name.trim().isEmpty() || value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("The input parameter is null or empty");
        } else if (this.mResultParams.containsKey(name)) {
            throw new IllegalArgumentException("The result parameter name is duplicated." + name);
        } else {
            this.mResultParams.put(name, value);
            return this;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\"requestedStateId\":\"").append(this.mRequestStateId).append("\"");
        if (this.mRuleId != null) {
            sb.append(",\"ruleId\":\"").append(this.mRuleId).append("\"");
        }
        if (this.mScreenParams.size() > 0) {
            sb.append(",\"screenParameters\":[");
            for (Entry<String, Attribute> elem : this.mScreenParams.entrySet()) {
                sb.append("{\"parameterName\":\"").append((String) elem.getKey()).append("\",").append("\"attributeName\":\"").append(((Attribute) elem.getValue()).name).append("\",\"attributeValue\":\"").append(((Attribute) elem.getValue()).value).append("\"},");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("]");
        }
        if (this.mResultParams.size() > 0) {
            sb.append(",\"resultParameters\":[");
            for (Entry<String, String> elem2 : this.mResultParams.entrySet()) {
                sb.append("{\"name\":\"").append((String) elem2.getKey()).append("\",\"value\":\"").append((String) elem2.getValue()).append("\"},");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("]");
        }
        return sb.toString();
    }
}

package com.samsung.android.sdk.bixby.data;

import java.util.Iterator;
import java.util.LinkedHashSet;

public class ScreenStateInfo {
    public static final ScreenStateInfo STATE_NOT_APPLICABLE = null;
    String mCallerAppName = "";
    LinkedHashSet<String> mStateList;

    public ScreenStateInfo(String currentStateId) throws IllegalArgumentException {
        if (currentStateId == null || currentStateId.trim().isEmpty()) {
            throw new IllegalArgumentException("The input parameter is null or empty.");
        }
        this.mStateList = new LinkedHashSet();
        this.mStateList.add(currentStateId);
    }

    public ScreenStateInfo(LinkedHashSet<String> currentStateIds) throws IllegalArgumentException {
        if (currentStateIds == null || currentStateIds.isEmpty()) {
            throw new IllegalArgumentException("The input parameter is null or empty.");
        }
        this.mStateList = currentStateIds;
    }

    public LinkedHashSet<String> getStates() {
        return this.mStateList;
    }

    public ScreenStateInfo addState(String currentStateId) throws IllegalArgumentException {
        if (currentStateId == null || currentStateId.trim().isEmpty()) {
            throw new IllegalArgumentException("The input parameter is null or empty.");
        } else if (this.mStateList.contains(currentStateId)) {
            throw new IllegalArgumentException("The screen parameter name is duplicated. " + currentStateId);
        } else {
            this.mStateList.add(currentStateId);
            return this;
        }
    }

    public ScreenStateInfo setCallerAppName(String callerAppName) throws IllegalArgumentException {
        if (callerAppName == null || callerAppName.trim().isEmpty()) {
            throw new IllegalArgumentException("The input parameter is null or empty.");
        }
        this.mCallerAppName = callerAppName;
        return this;
    }

    public String getCallerAppName() {
        return this.mCallerAppName;
    }

    public String toString() {
        StringBuilder statesBuilder = new StringBuilder();
        if (!this.mCallerAppName.isEmpty()) {
            statesBuilder.append("\"callerAppName\":");
            statesBuilder.append("\"").append(this.mCallerAppName).append("\",");
        }
        statesBuilder.append("\"stateIds\":[");
        if (this.mStateList == null || this.mStateList.size() <= 0) {
            return null;
        }
        Iterator it = this.mStateList.iterator();
        while (it.hasNext()) {
            statesBuilder.append("\"").append((String) it.next()).append("\",");
        }
        statesBuilder.deleteCharAt(statesBuilder.length() - 1);
        statesBuilder.append("]");
        return statesBuilder.toString();
    }
}

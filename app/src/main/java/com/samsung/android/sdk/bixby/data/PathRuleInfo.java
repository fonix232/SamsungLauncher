package com.samsung.android.sdk.bixby.data;

import java.util.List;

public final class PathRuleInfo {
    private String[] apps = null;
    private String executionType = null;
    private String intent = null;
    private boolean isCalleePathRule;
    private boolean isFromSimulator;
    private boolean isRoot;
    private String pathRuleId = null;
    private String pathRuleName = null;
    private String sampleUtterance = null;
    private List<State> states;
    private String utterance = null;

    public PathRuleInfo(String pathRuleId, String pathRuleName, String intent, String utterance, String sampleUtterance, String[] apps, String executionType, boolean isRoot, boolean isCalleePathRule, boolean isFromSimulator, List<State> states) {
        this.pathRuleId = pathRuleId;
        this.pathRuleName = pathRuleName;
        this.intent = intent;
        this.utterance = utterance;
        this.sampleUtterance = sampleUtterance;
        this.apps = apps;
        this.executionType = executionType;
        this.isRoot = isRoot;
        this.isCalleePathRule = isCalleePathRule;
        this.isFromSimulator = isFromSimulator;
        this.states = states;
    }

    public String getPathRuleId() {
        return this.pathRuleId;
    }

    public String getPathRuleName() {
        return this.pathRuleName;
    }

    public String getIntent() {
        return this.intent;
    }

    public String getUtterance() {
        return this.utterance;
    }

    public String getSampleUtterance() {
        return this.sampleUtterance;
    }

    public String getExecutionType() {
        return this.executionType;
    }

    public String[] getApps() {
        return this.apps;
    }

    public boolean isRoot() {
        return this.isRoot;
    }

    public boolean isCalleePathRule() {
        return this.isCalleePathRule;
    }

    public boolean isFromSimulator() {
        return this.isFromSimulator;
    }

    public List<State> getStates() {
        return this.states;
    }
}

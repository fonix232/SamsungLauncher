package com.samsung.android.sdk.bixby.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import com.samsung.android.sdk.bixby.BuildConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class State implements Parcelable {
    public static final Creator<State> CREATOR = new Creator<State>() {
        public State createFromParcel(Parcel source) {
            return new State(source);
        }

        public State[] newArray(int size) {
            return new State[size];
        }
    };
    private String appName;
    private Boolean isExecuted;
    private Boolean isLandingState;
    private Boolean isLastState;
    private Boolean isResent;
    private List<Parameter> parameters = new ArrayList();
    private String ruleId;
    private Integer seqNum;
    private String specVer = BuildConfig.VERSION_NAME;
    private String stateId;
    private String subIntent;

    public State(String specVer, Integer seqNum, Boolean isExecuted, String appName, String ruleId, String stateId, Boolean isResent, Boolean isLandingState, Boolean isLastState, String subIntent, List<Parameter> parameters) {
        this.specVer = specVer;
        this.seqNum = seqNum;
        this.isExecuted = isExecuted;
        this.appName = appName;
        this.ruleId = ruleId;
        this.stateId = stateId;
        this.isResent = isResent;
        this.isLandingState = isLandingState;
        this.isLastState = isLastState;
        this.subIntent = subIntent;
        this.parameters = parameters;
    }

    public State(Parcel parcel) {
        boolean z;
        boolean z2 = true;
        this.specVer = parcel.readString();
        this.seqNum = Integer.valueOf(parcel.readInt());
        this.isExecuted = Boolean.valueOf(parcel.readByte() != (byte) 0);
        this.appName = parcel.readString();
        this.ruleId = parcel.readString();
        this.stateId = parcel.readString();
        if (parcel.readByte() != (byte) 0) {
            z = true;
        } else {
            z = false;
        }
        this.isResent = Boolean.valueOf(z);
        if (parcel.readByte() != (byte) 0) {
            z = true;
        } else {
            z = false;
        }
        this.isLandingState = Boolean.valueOf(z);
        if (parcel.readByte() == (byte) 0) {
            z2 = false;
        }
        this.isLastState = Boolean.valueOf(z2);
        this.subIntent = parcel.readString();
        this.parameters = parcel.createTypedArrayList(Parameter.CREATOR);
    }

    public Integer getSeqNum() {
        return this.seqNum;
    }

    public void setSeqNum(Integer seqNum) {
        this.seqNum = seqNum;
    }

    public String getAppName() {
        return this.appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getStateId() {
        return this.stateId;
    }

    public void setStateId(String stateId) {
        this.stateId = stateId;
    }

    public List<Parameter> getParameters() {
        return this.parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public Boolean isResent() {
        return this.isResent;
    }

    public void setResent(Boolean isResent) {
        this.isResent = isResent;
    }

    public Boolean isLandingState() {
        return this.isLandingState;
    }

    public void setLandingState(Boolean isLandingState) {
        this.isLandingState = isLandingState;
    }

    public Boolean isExecuted() {
        return this.isExecuted;
    }

    public void setExecuted(Boolean isExecuted) {
        this.isExecuted = isExecuted;
    }

    public void setLastState(Boolean isLastState) {
        this.isLastState = isLastState;
    }

    public Boolean isLastState() {
        return this.isLastState;
    }

    public String getSubIntent() {
        return this.subIntent;
    }

    public void setSubIntent(String subIntent) {
        this.subIntent = subIntent;
    }

    public String getRuleId() {
        return this.ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public Map<String, Parameter> getParamMap() {
        Map<String, Parameter> paramMap = new HashMap();
        for (Parameter p : this.parameters) {
            paramMap.put(p.getParameterName(), p);
        }
        return paramMap;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        int i;
        int i2 = 1;
        dest.writeString(this.specVer);
        dest.writeInt(this.seqNum.intValue());
        dest.writeByte((byte) (this.isExecuted.booleanValue() ? 1 : 0));
        dest.writeString(this.appName);
        dest.writeString(this.ruleId);
        dest.writeString(this.stateId);
        if (this.isResent.booleanValue()) {
            i = 1;
        } else {
            i = 0;
        }
        dest.writeByte((byte) i);
        if (this.isLandingState.booleanValue()) {
            i = 1;
        } else {
            i = 0;
        }
        dest.writeByte((byte) i);
        if (!this.isLastState.booleanValue()) {
            i2 = 0;
        }
        dest.writeByte((byte) i2);
        dest.writeString(this.subIntent);
        dest.writeTypedList(this.parameters);
    }
}

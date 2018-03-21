package com.samsung.android.sdk.bixby.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParamFilling implements Parcelable {
    public static final Creator<ParamFilling> CREATOR = new Creator<ParamFilling>() {
        public ParamFilling createFromParcel(Parcel in) {
            return new ParamFilling(in);
        }

        public ParamFilling[] newArray(int size) {
            return new ParamFilling[size];
        }
    };
    String appName;
    String intent;
    List<ScreenParameter> mScreenParameters = new ArrayList();
    List<String> screenStates = new ArrayList();
    String utterance;

    public ParamFilling(String utterance, String intent, String appName, List<String> screenStates, List<ScreenParameter> screenParameters) {
        this.utterance = utterance;
        this.intent = intent;
        this.appName = appName;
        this.screenStates = screenStates;
        this.mScreenParameters = screenParameters;
    }

    protected ParamFilling(Parcel in) {
        this.utterance = in.readString();
        this.intent = in.readString();
        this.appName = in.readString();
        this.screenStates = in.createStringArrayList();
        this.mScreenParameters = in.createTypedArrayList(ScreenParameter.CREATOR);
    }

    public String getUtterance() {
        return this.utterance;
    }

    public void setUtterance(String utterance) {
        this.utterance = utterance;
    }

    public String getIntent() {
        return this.intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getAppName() {
        return this.appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<String> getScreenStates() {
        return this.screenStates;
    }

    public void setScreenStates(List<String> screenStates) {
        this.screenStates = screenStates;
    }

    public List<ScreenParameter> getScreenParameters() {
        return this.mScreenParameters;
    }

    public void setScreenParameters(List<ScreenParameter> screenParameters) {
        this.mScreenParameters = screenParameters;
    }

    public Map<String, ScreenParameter> getScreenParamMap() {
        Map<String, ScreenParameter> paramMap = new HashMap();
        for (ScreenParameter p : this.mScreenParameters) {
            paramMap.put(p.getParameterName(), p);
        }
        return paramMap;
    }

    public static Creator<ParamFilling> getCREATOR() {
        return CREATOR;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.utterance);
        dest.writeString(this.intent);
        dest.writeString(this.appName);
        dest.writeStringList(this.screenStates);
        dest.writeTypedList(this.mScreenParameters);
    }

    public int describeContents() {
        return 0;
    }
}

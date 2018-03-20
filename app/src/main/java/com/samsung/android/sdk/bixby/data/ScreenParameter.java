package com.samsung.android.sdk.bixby.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.ArrayList;
import java.util.List;

public class ScreenParameter implements Parcelable {
    public static final Creator<ScreenParameter> CREATOR = new Creator<ScreenParameter>() {
        public ScreenParameter createFromParcel(Parcel in) {
            return new ScreenParameter(in);
        }

        public ScreenParameter[] newArray(int size) {
            return new ScreenParameter[size];
        }
    };
    private String CHObjectType;
    private List<CHObject> CHObjects = new ArrayList();
    private String parameterName;
    private String parameterType;
    private String slotName;
    private String slotType;
    private String slotValue;

    protected ScreenParameter(Parcel in) {
        this.slotType = in.readString();
        this.slotName = in.readString();
        this.slotValue = in.readString();
        this.CHObjectType = in.readString();
        this.CHObjects = in.createTypedArrayList(CHObject.CREATOR);
        this.parameterName = in.readString();
        this.parameterType = in.readString();
    }

    public ScreenParameter(String slotType, String slotName, String slotValue, String CHObjectType, List<CHObject> CHObjects, String parameterName, String parameterType) {
        this.slotType = slotType;
        this.slotName = slotName;
        this.slotValue = slotValue;
        this.CHObjectType = CHObjectType;
        this.CHObjects = CHObjects;
        this.parameterName = parameterName;
        this.parameterType = parameterType;
    }

    public String getSlotType() {
        return this.slotType;
    }

    public void setSlotType(String slotType) {
        this.slotType = slotType;
    }

    public String getSlotName() {
        return this.slotName;
    }

    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    public String getSlotValue() {
        return this.slotValue;
    }

    public void setSlotValue(String slotValue) {
        this.slotValue = slotValue;
    }

    public String getCHObjectType() {
        return this.CHObjectType;
    }

    public void setCHObjectType(String CHObjectType) {
        this.CHObjectType = CHObjectType;
    }

    public List<CHObject> getCHObjects() {
        return this.CHObjects;
    }

    public void setCHObjects(List<CHObject> CHObjects) {
        this.CHObjects = CHObjects;
    }

    public String getParameterName() {
        return this.parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getParameterType() {
        return this.parameterType;
    }

    public void setParameterType(String parameterType) {
        this.parameterType = parameterType;
    }

    public static Creator<ScreenParameter> getCREATOR() {
        return CREATOR;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.slotType);
        dest.writeString(this.slotName);
        dest.writeString(this.slotValue);
        dest.writeString(this.CHObjectType);
        dest.writeTypedList(this.CHObjects);
        dest.writeString(this.parameterName);
        dest.writeString(this.parameterType);
    }

    public int describeContents() {
        return 0;
    }
}

package com.samsung.android.sdk.bixby.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.ArrayList;
import java.util.List;

public class Parameter implements Parcelable {
    public static final Creator<Parameter> CREATOR = new Creator<Parameter>() {
        public Parameter createFromParcel(Parcel in) {
            return new Parameter(in);
        }

        public Parameter[] newArray(int size) {
            return new Parameter[size];
        }
    };
    private String CHObjectType;
    private List<CHObject> CHObjects = new ArrayList();
    private Boolean isMandatory;
    private String parameterName;
    private String parameterType;
    private String slotName;
    private String slotType;
    private String slotValue;
    private String slotValueType;

    public Parameter() {
        // TODO: Fill out empty constructor
    }

    public Parameter(String slotType, String slotName, String slotValue, String slotValueType, String CHObjectType, List<CHObject> CHObjects, String parameterName, String parameterType, Boolean isMandatory) {
        this.slotType = slotType;
        this.slotName = slotName;
        this.slotValue = slotValue;
        this.slotValueType = slotValueType;
        this.CHObjectType = CHObjectType;
        this.CHObjects = CHObjects;
        this.parameterName = parameterName;
        this.parameterType = parameterType;
        this.isMandatory = isMandatory;
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

    public String getSlotValueType() {
        return this.slotValueType;
    }

    public void setSlotValueType(String slotValueType) {
        this.slotValueType = slotValueType;
    }

    public void setCHObjects(List<CHObject> CHObjects) {
        this.CHObjects = CHObjects;
    }

    public List<CHObject> getCHObjects() {
        return this.CHObjects;
    }

    public void setCHObjectType(String CHObjectType) {
        this.CHObjectType = CHObjectType;
    }

    public String getCHObjectType() {
        return this.CHObjectType;
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

    public Boolean getIsMandatory() {
        return this.isMandatory;
    }

    public void setIsMandatory(Boolean isMandatory) {
        this.isMandatory = isMandatory;
    }

    public Parameter(Parcel in) {
        Boolean bool;
        boolean z = true;
        this.slotType = in.readString();
        this.slotName = in.readString();
        this.slotValue = in.readString();
        this.slotValueType = in.readString();
        this.CHObjectType = in.readString();
        if (in.readByte() == (byte) 1) {
            this.CHObjects = new ArrayList();
            in.readList(this.CHObjects, CHObject.class.getClassLoader());
        } else {
            this.CHObjects = null;
        }
        this.parameterName = in.readString();
        this.parameterType = in.readString();
        byte isMandatoryVal = in.readByte();
        if (isMandatoryVal == (byte) 2) {
            bool = null;
        } else {
            if (isMandatoryVal == (byte) 0) {
                z = false;
            }
            bool = Boolean.valueOf(z);
        }
        this.isMandatory = bool;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        int i = 1;
        dest.writeString(this.slotType);
        dest.writeString(this.slotName);
        dest.writeString(this.slotValue);
        dest.writeString(this.slotValueType);
        dest.writeString(this.CHObjectType);
        if (this.CHObjects == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeList(this.CHObjects);
        }
        dest.writeString(this.parameterName);
        dest.writeString(this.parameterType);
        if (this.isMandatory == null) {
            dest.writeByte((byte) 2);
            return;
        }
        if (!this.isMandatory.booleanValue()) {
            i = 0;
        }
        dest.writeByte((byte) i);
    }
}

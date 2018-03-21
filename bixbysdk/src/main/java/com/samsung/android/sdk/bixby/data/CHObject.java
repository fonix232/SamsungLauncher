package com.samsung.android.sdk.bixby.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class CHObject implements Parcelable {
    public static final Creator<CHObject> CREATOR = new Creator<CHObject>() {
        public CHObject createFromParcel(Parcel in) {
            return new CHObject(in);
        }

        public CHObject[] newArray(int size) {
            return new CHObject[size];
        }
    };
    private String CH_Type;
    private String CH_Value;
    private String CH_ValueType;

    public CHObject() {
        // TODO: Fill out empty constructor
    }

    public CHObject(String CHType, String CHValue, String CHValueType) {
        this.CH_Type = CHType;
        this.CH_Value = CHValue;
        this.CH_ValueType = CHValueType;
    }

    public String getCHType() {
        return this.CH_Type;
    }

    public void setCHType(String CHType) {
        this.CH_Type = CHType;
    }

    public String getCHValue() {
        return this.CH_Value;
    }

    public void setCHValue(String CHValue) {
        this.CH_Value = CHValue;
    }

    public String getCHValueType() {
        return this.CH_ValueType;
    }

    public void setCHValueType(String CHValueType) {
        this.CH_ValueType = CHValueType;
    }

    public CHObject(Parcel in) {
        this.CH_Type = in.readString();
        this.CH_Value = in.readString();
        this.CH_ValueType = in.readString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.CH_Type);
        dest.writeString(this.CH_Value);
        dest.writeString(this.CH_ValueType);
    }
}

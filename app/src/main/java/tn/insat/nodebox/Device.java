package tn.insat.nodebox;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.firebase.Timestamp;

public class Device  implements Parcelable {
    private String name;
    private String state;
    private String topic;
    private String IP="";
    private Boolean isDevice;
    private Timestamp timestamp;

    public Device() {
    }

    public Device(String name, String state,Boolean isDevice) {
        this.name = name;
        this.state = state;
        this.isDevice = isDevice;
    }

    public Device(Parcel parcel) {

        this.name  = parcel.readString();
        this.isDevice = (parcel.readInt() == 1);
        this.topic = parcel.readString();
        this.state = parcel.readString();
        this.IP = parcel.readString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getIsDevice() {
        return isDevice;
    }

    public void setIsDevice(Boolean isDevice) {
        this.isDevice = isDevice;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String toString() {
        return (this.name);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.name);
        parcel.writeInt(this.isDevice ? 1 : 0);
        parcel.writeString(this.topic);
        parcel.writeString(this.state);
        parcel.writeString(this.IP);
    }

    public static final Creator<Device> CREATOR = new Creator<Device>() {

        @Override
        public Device createFromParcel(Parcel parcel) {
            return new Device(parcel);
        }

        @Override
        public Device[] newArray(int i) {
            return new Device[i];
        }
    };

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }
}
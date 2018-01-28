package com.diamond.SmartVoice.Controllers.Homey.json;

import com.diamond.SmartVoice.Controllers.Capability;
import com.diamond.SmartVoice.Controllers.UDevice;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.List;

/**
 * @author Dmitriy Ponomarev
 */
public class Device extends UDevice {
    private String roomName;

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("driver")
    private Object driver; // TODO

    @SerializedName("zone")
    private Room zone;

    @SerializedName("data")
    private Object data; // TODO

    @SerializedName("icon")
    private String icon;

    @SerializedName("settings")
    private Settings settings;

    @SerializedName("class")
    private String getclass;

    @SerializedName("capabilities")
    private Object capabilities; // TODO

    @SerializedName("capabilitiesArray")
    public List<Capability> capabilitiesArray;

    @SerializedName("capabilitiesOptions")
    private Object capabilitiesOptions; // TODO

    @SerializedName("flags")
    private Object flags; // TODO

    @SerializedName("mobile")
    private Object mobile; // TODO

    @SerializedName("order")
    private int order;

    @SerializedName("online")
    private boolean online;

    @SerializedName("state")
    public HashMap<Capability, String> state;

    @SerializedName("lastUpdated")
    private Object lastUpdated; // TODO

    @SerializedName("available")
    private boolean available;

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoomID() {
        return zone.getId();
    }

    public boolean isVisible() {
        return available && !"266".equals(settings.zw_manufacturer_id) && super.isVisible();
    }

    @Override
    public String toString() {
        return "{" + id + ", " + name + "}";
    }
}
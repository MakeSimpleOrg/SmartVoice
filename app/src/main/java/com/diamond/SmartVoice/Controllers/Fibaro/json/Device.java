package com.diamond.SmartVoice.Controllers.Fibaro.json;

import com.diamond.SmartVoice.Controllers.UDevice;
import com.google.gson.annotations.SerializedName;

/**
 * @author Dmitriy Ponomarev
 */
public class Device extends UDevice {
    private String roomName;

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("roomID")
    private int roomID;

    @SerializedName("type")
    private String type;

    @SerializedName("baseType")
    private String baseType;

    @SerializedName("enabled")
    private boolean enabled;

    @SerializedName("visible")
    private boolean visible;

    @SerializedName("isPlugin")
    private boolean isPlugin;

    @SerializedName("parentId")
    private int parentId;

    @SerializedName("remoteGatewayId")
    private int remoteGatewayId;

    @SerializedName("viewXml")
    private boolean viewXml;

    @SerializedName("configXml")
    private boolean configXml;

    @SerializedName("interfaces")
    private Object interfaces;

    @SerializedName("properties")
    private DeviceProperties properties;

    @SerializedName("actions")
    private Object actions;

    @SerializedName("created")
    private int created;

    @SerializedName("modified")
    private int modified;

    @SerializedName("sortOrder")
    private int sortOrder;

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

    public int getRoomID() {
        return roomID;
    }

    public String getType() {
        return type;
    }

    public DeviceProperties getProperties() {
        return properties;
    }

    public boolean isThermostat() {
        return type.equals("com.fibaro.setPoint") || type.equals("com.fibaro.thermostatDanfoss") || type.equals("com.fibaro.thermostatHorstmann");
    }

    private boolean devicesWithSaveLogsOnly = false;

    public void setDevicesWithSaveLogsOnly(boolean value) {
        devicesWithSaveLogsOnly = value;
    }

    @Override
    public String getValue() {
        if(getProperties() == null || getProperties().getValue() == null)
            return "0";
        return getProperties().getValue();
    }

    @Override
    public String getHumidity() {
        if(getProperties() == null || getProperties().getValue() == null)
            return "";
        return "" + (int) Double.parseDouble(getValue());
    }

    @Override
    public String getLight() {
        if(getProperties() == null || getProperties().getValue() == null)
            return "";
        return "" + (int) Double.parseDouble(getValue());
    }

    @Override
    public String getTemperature() {
        if(getProperties() == null || getProperties().getValue() == null)
            return "";
        return "" + (int) Double.parseDouble(getValue());
    }

    public boolean isVisible() {
        if (devicesWithSaveLogsOnly && getProperties() != null && !"true".equals(getProperties().getSaveLogs()))
            return false;
        return visible && super.isVisible();
    }

    @Override
    public String toString() {
        return "{" + id + ", " + name + "}";
    }
}
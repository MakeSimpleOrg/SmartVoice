package com.diamond.SmartVoice.Controllers.Fibaro.json;

import com.diamond.SmartVoice.Controllers.URoom;
import com.google.gson.annotations.SerializedName;

/**
 * @author Dmitriy Ponomarev
 */
public class Room extends URoom {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("sectionID")
    private int sectionID;

    @SerializedName("icon")
    private String icon;

    @SerializedName("defaultSensors")
    private Sensor defaultSensors;

    @SerializedName("defaultThermostat")
    private int defaultThermostat;

    @SerializedName("sortOrder")
    private int sortOrder;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
package com.diamond.SmartVoice.Controllers.Homey.json;

import com.diamond.SmartVoice.Controllers.Fibaro.json.Sensor;
import com.diamond.SmartVoice.Controllers.URoom;
import com.google.gson.annotations.SerializedName;

/**
 * @author Dmitriy Ponomarev
 */
public class Room extends URoom {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("icon")
    private String icon;

    @SerializedName("index")
    private int index;

    @SerializedName("parent")
    private String parent;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
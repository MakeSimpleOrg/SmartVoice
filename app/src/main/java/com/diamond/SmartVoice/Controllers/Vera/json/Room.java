package com.diamond.SmartVoice.Controllers.Vera.json;

import com.diamond.SmartVoice.Controllers.URoom;
import com.google.gson.annotations.SerializedName;

/**
 * @author Dmitriy Ponomarev
 */
public class Room extends URoom {
    @SerializedName("name")
    private String name;

    @SerializedName("id")
    private String id;

    @SerializedName("section")
    private String section;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return Integer.parseInt(id);
    }

    public void setId(String id) {
        this.id = id;
    }
}

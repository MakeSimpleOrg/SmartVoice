package com.diamond.SmartVoice.Controllers.Zipato.json;

import com.diamond.SmartVoice.Controllers.URoom;
import com.google.gson.annotations.SerializedName;

public class Room extends URoom {
    @SerializedName("link")
    private String link;

    @SerializedName("name")
    private String name;

    @SerializedName("id")
    public int id;

    public String getId() {
        return "" + id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

package com.diamond.SmartVoice.Controllers.Zipato.json;

import com.diamond.SmartVoice.Controllers.UScene;
import com.google.gson.annotations.SerializedName;

/**
 * @author Dmitriy Ponomarev
 */
public class Scene extends UScene {
    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("order")
    private String order;

    private String id;

    public String getRoomName() {
        return "None";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

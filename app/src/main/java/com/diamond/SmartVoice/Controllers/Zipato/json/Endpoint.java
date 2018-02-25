package com.diamond.SmartVoice.Controllers.Zipato.json;

import com.google.gson.annotations.SerializedName;

public class Endpoint {
    @SerializedName("link")
    private String link;

    @SerializedName("name")
    private String name;

    @SerializedName("room")
    private int room;

    @SerializedName("tags")
    private Object tags;

    @SerializedName("uuid")
    private String uuid;
}

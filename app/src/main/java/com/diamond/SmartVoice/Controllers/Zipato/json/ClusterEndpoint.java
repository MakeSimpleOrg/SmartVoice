package com.diamond.SmartVoice.Controllers.Zipato.json;

import com.google.gson.annotations.SerializedName;

public class ClusterEndpoint {
    @SerializedName("link")
    private String link;

    @SerializedName("name")
    public String name;

    @SerializedName("room")
    private int room;

    @SerializedName("uuid")
    private String uuid;
}

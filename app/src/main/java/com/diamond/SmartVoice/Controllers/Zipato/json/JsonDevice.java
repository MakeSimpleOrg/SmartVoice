package com.diamond.SmartVoice.Controllers.Zipato.json;

import com.google.gson.annotations.SerializedName;

public class JsonDevice {
    @SerializedName("link")
    private String link;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("uuid")
    private String uuid;
}

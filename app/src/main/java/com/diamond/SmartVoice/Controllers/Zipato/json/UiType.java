package com.diamond.SmartVoice.Controllers.Zipato.json;

import com.google.gson.annotations.SerializedName;

public class UiType {
    @SerializedName("link")
    private String link;

    @SerializedName("name")
    private String name;

    @SerializedName("endpointType")
    public String endpointType;

    @SerializedName("relativeUrl")
    private String relativeUrl;
}

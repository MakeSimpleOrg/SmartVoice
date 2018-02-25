package com.diamond.SmartVoice.Controllers.Zipato.json;

import com.google.gson.annotations.SerializedName;

public class Definition {
    @SerializedName("id")
    private int id;

    @SerializedName("attribute")
    private String attribute;

    @SerializedName("attributeType")
    private String attributeType;

    @SerializedName("cluster")
    public String cluster;

    @SerializedName("readable")
    private boolean readable;

    @SerializedName("reportable")
    private boolean reportable;

    @SerializedName("writable")
    private boolean writable;
}

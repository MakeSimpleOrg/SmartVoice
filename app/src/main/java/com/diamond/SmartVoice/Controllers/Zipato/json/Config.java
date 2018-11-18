package com.diamond.SmartVoice.Controllers.Zipato.json;

import com.google.gson.annotations.SerializedName;

public class Config {
    @SerializedName("name")
    private String name;

    @SerializedName("master")
    private boolean master;

    @SerializedName("hidden")
    private boolean hidden;

    @SerializedName("reported")
    private boolean reported;

    @SerializedName("expire")
    private String expire;

    @SerializedName("compression")
    private String compression;

    @SerializedName("type")
    private String type;

    @SerializedName("unit")
    public String unit;

    @SerializedName("enumValues")
    private Object enumValues;

    @SerializedName("scale")
    private double scale;

    @SerializedName("precision")
    private int precision;

    @SerializedName("room")
    private int room;
}

package com.diamond.SmartVoice.Controllers.Zipato.json;

import com.google.gson.annotations.SerializedName;

/**
 * @author Dmitriy Ponomarev
 */
public class AttributesFull {
    @SerializedName("uuid")
    public String uuid;

    @SerializedName("name")
    public String name;

    @SerializedName("definition")
    public Definition definition;

    @SerializedName("config")
    public Config config;

    @SerializedName("value")
    public Value value;

    @SerializedName("clusterEndpoint")
    public ClusterEndpoint clusterEndpoint;

    @SerializedName("endpoint")
    public Endpoint endpoint;

    @SerializedName("device")
    public JsonDevice device;

    @SerializedName("network")
    public Network network;

    @SerializedName("showIcon")
    public boolean showIcon;

    @SerializedName("room")
    public Room room;

    @SerializedName("attributeId")
    public int attributeId;

    @SerializedName("uiType")
    public UiType uiType;
}

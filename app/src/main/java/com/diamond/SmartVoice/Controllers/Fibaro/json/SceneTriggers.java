package com.diamond.SmartVoice.Controllers.Fibaro.json;

import com.google.gson.annotations.SerializedName;

/**
 * @author Dmitriy Ponomarev
 */
public class SceneTriggers {
    @SerializedName("properties")
    private SceneProperties[] properties;

    @SerializedName("globals")
    private String[] globals;

    @SerializedName("events")
    private String[] events;
}

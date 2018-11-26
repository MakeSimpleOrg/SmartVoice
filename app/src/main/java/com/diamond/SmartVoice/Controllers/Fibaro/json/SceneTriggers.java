package com.diamond.SmartVoice.Controllers.Fibaro.json;

import com.google.gson.annotations.SerializedName;

/**
 * @author Dmitriy Ponomarev
 */
class SceneTriggers {
    @SerializedName("properties")
    private SceneProperties[] properties;

    @SerializedName("globals")
    private Object globals;

    @SerializedName("events")
    private Object events;
}

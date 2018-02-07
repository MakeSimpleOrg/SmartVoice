package com.diamond.SmartVoice.Controllers.Homey.json;

import com.diamond.SmartVoice.Controllers.Capability;
import com.google.gson.annotations.SerializedName;

/**
 * @author Dmitriy Ponomarev
 */
class State {
    @SerializedName("id")
    private Capability id;

    @SerializedName("name")
    private String value;
}
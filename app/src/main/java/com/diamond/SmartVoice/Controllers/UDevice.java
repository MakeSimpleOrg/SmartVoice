package com.diamond.SmartVoice.Controllers;

import java.util.ArrayList;

import org.json.JSONObject;

/**
 * @author Dmitriy Ponomarev
 */
public abstract class UDevice {
    public String ai_name;

    public UType uType = UType.None;

    public abstract String getId();

    public abstract String getName();

    public abstract String getRoomName();

    public abstract String getValue();

    public abstract String getHumidity();

    public abstract String getLight();

    public abstract String getTemperature();

    public boolean isVisible() {
        return !getName().startsWith("_");
    }
}
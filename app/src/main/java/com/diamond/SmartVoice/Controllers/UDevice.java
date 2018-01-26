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
        return uType != UType.None && !getName().startsWith("_");
    }

    public String getStatus()
    {
        switch (uType) {
            case OnOff:
                return getValue().equals("false") || getValue().equals("0") ? "Выключено" : "Включено";
            case OpenClose:
                return getValue().equals("false") || getValue().equals("0") ? "Открыто" : "Закрыто";
            case Value:
                return getValue();
            case Humidity:
                return getHumidity();
            case Light:
                return getLight();
            case Temperature:
                return getTemperature();
        }
        return "";
    }
}
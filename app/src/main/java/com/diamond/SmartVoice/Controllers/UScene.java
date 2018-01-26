package com.diamond.SmartVoice.Controllers;

/**
 * @author Dmitriy Ponomarev
 */
public abstract class UScene {
    public String ai_name;

    public abstract String getId();

    public abstract String getName();

    public abstract String getRoomName();

    public boolean isVisible() {
        return !getName().startsWith("_");
    }
}
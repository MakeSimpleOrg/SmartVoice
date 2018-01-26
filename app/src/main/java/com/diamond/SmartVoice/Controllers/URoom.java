package com.diamond.SmartVoice.Controllers;

/**
 * @author Dmitriy Ponomarev
 */
public abstract class URoom {
    public abstract int getId();

    public abstract String getName();

    public boolean isVisible() {
        return !getName().startsWith("_");
    }
}
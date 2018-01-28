package com.diamond.SmartVoice.Controllers;

import java.util.HashMap;

/**
 * @author Dmitriy Ponomarev
 */
public abstract class UDevice {
    public String ai_name;
    public int ai_flag = 0;

    public abstract String getId();

    public abstract String getName();

    public abstract String getRoomName();

    public boolean isVisible() {
        return !_capabilities.isEmpty() && !getName().startsWith("_");
    }

    private HashMap<Capability, String> _capabilities = new HashMap<>();

    public void addCapability(Capability cap, String value) {
        _capabilities.put(cap, value);
    }

    public HashMap<Capability, String> getCapabilities() {
        return _capabilities;
    }
}
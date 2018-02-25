package com.diamond.SmartVoice.Controllers.Zipato.json;

import com.diamond.SmartVoice.Controllers.UDevice;

/**
 * @author Dmitriy Ponomarev
 */
public class Device extends UDevice {
    private String roomName;

    private String id;

    private String name;

    private String value;

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "{" + id + ", " + name + "}";
    }
}
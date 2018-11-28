package com.diamond.SmartVoice.Controllers.WirenBoard;

import com.diamond.SmartVoice.Controllers.UDevice;

import java.util.ArrayList;
import java.util.Locale;

/**
 * @author Dmitriy Ponomarev
 */
public class Device extends UDevice {
    private final WirenBoard controller;

    public Device(WirenBoard controller)
    {
        this.controller = controller;
    }

    private String id;

    private String name;

    private String roomId;

    private String template;

    private ArrayList<String> topics = new ArrayList<String>();

    public String getAiName()
    {
        return (getRoomName() + " " + ai_name).toLowerCase(Locale.getDefault());
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

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        Room room = controller.getRoomById(roomId);
        return room == null ? "" : room.getName();
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public ArrayList<String> getTopics() {
        return topics;
    }

    public void addTopic(String topic) {
        this.topics.add(topic);
    }

    public boolean isVisible() {
        //return super.isVisible(); FIXME
        return true;
    }

    @Override
    public String toString() {
        return "{" + id + ", " + name + "}";
    }
}
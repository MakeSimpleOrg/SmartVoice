package com.diamond.SmartVoice.Controllers.Vera.json;

import com.diamond.SmartVoice.Controllers.UScene;
import com.google.gson.annotations.SerializedName;

/**
 * @author Dmitriy Ponomarev
 */
public class Scene extends UScene {
    private String roomName;

    @SerializedName("active")
    private String active;

    @SerializedName("name")
    private String name;

    @SerializedName("id")
    private String id;

    @SerializedName("room")
    private String room;

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoom() {
        return room;
    }

    public String getRoomID() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }
}

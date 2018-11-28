package com.diamond.SmartVoice.Controllers.WirenBoard;

import com.diamond.SmartVoice.Controllers.URoom;

/**
 * @author Dmitriy Ponomarev
 */
public class Room extends URoom {
    private String id;

    private String name;

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
}
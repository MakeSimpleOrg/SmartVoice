package com.diamond.SmartVoice.Fibaro;

import com.diamond.SmartVoice.UScene;

public class Scene extends UScene {
    public int id;
    public String name;
    public String type;
    public String properties;
    public int roomID;
    public int iconID;
    public String runConfig;
    public boolean autostart;
    public boolean protectedByPIN;
    public boolean killable;
    public int maxRunningInstances;
    public int runningInstances;
    public boolean visible;
    public boolean isLua;
    public Triggers triggers;
    public String liliStartCommand;
    public String liliStopCommand;
    public int sortOrder;

    public class Triggers {
        public Properties[] properties;
        public String[] globals;
        public String[] events;
    }

    public class Properties {
        public String id;
        public String name;
    }
}
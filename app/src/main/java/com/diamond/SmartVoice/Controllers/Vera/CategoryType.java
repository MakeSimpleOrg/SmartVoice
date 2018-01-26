package com.diamond.SmartVoice.Controllers.Vera;

import com.diamond.SmartVoice.Controllers.UType;

/**
 * @author Dmitriy Ponomarev
 */
public enum CategoryType {
    Controller(0, "Controller", UType.None),
    Interface(1, "Interface", UType.None),
    DimmableLight(2, "Dimmable Light", UType.OnOff),
    Switch(3, "Switch", UType.OnOff),
    SecuritySensor(4, "Security Sensor", UType.None),
    HVAC(5, "HVAC", UType.None),
    Camera(6, "Camera", UType.None),
    DoorLock(7, "Door Lock", UType.OpenClose),
    WindowCovering(8, "Window Covering", UType.OnOff),
    RemoteControl(9, "Remote Control", UType.None),
    IRTransmitter(10, "IR Transmitter", UType.None),
    GenericIO(11, "Generic I/O", UType.None),
    GenericSensor(12, "Generic Sensor", UType.None),
    SerialPort(13, "Serial Port", UType.None),
    SceneController(14, "Scene Controller", UType.None),
    AV(15, "A/V", UType.None),
    HumiditySensor(16, "Humidity Sensor", UType.Humidity),
    TemperatureSensor(17, "Temperature Sensor", UType.Temperature),
    LightSensor(18, "Light Sensor", UType.Light),
    ZWaveInterface(19, "Z-Wave Interface", UType.None),
    InsteonInterface(20, "Insteon Interface", UType.None),
    PowerMeter(21, "Power Meter", UType.None),
    AlarmPanel(22, "Alarm Panel", UType.None),
    AlarmPartition(23, "Alarm Partition", UType.None),
    Siren(24, "Siren", UType.None),
    Weather(25, "Weather", UType.None),
    PhilipsController(26, "Philips Controller", UType.None),
    Appliance(27, "Appliance", UType.None),
    UVSensor(28, "UV Sensor", UType.Value),
    Unknown(-1, "Unknown", UType.None);

    private int id;
    private String name;
    private UType uType;

    private CategoryType(int id, String name, UType uType) {
        this.id = id;
        this.name = name;
        this.uType = uType;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UType getUType() {
        return uType;
    }

    @Override
    public String toString() {
        return "{" + id + ", " + name + "}";
    }
}
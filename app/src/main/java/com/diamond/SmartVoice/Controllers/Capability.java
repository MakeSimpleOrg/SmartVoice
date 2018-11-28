package com.diamond.SmartVoice.Controllers;

/**
 * @author Dmitriy Ponomarev
 */
public enum Capability {
    onoff,
    openclose,
    windowcoverings_state,
    dim,
    light_hue,
    light_saturation,
    light_temperature,
    light_mode,
    light_rgbw,
    volume_set,
    alarm_contact,
    alarm_battery,
    measure_battery,
    measure_power,
    meter_power,
    measure_temperature,
    measure_co2,
    measure_humidity,
    measure_light,
    measure_noise,
    measure_pressure,
    measure_generic,
    button,
    target_temperature,
    thermostat_mode;

    public static Capability get(String name) {
        for (Capability value : Capability.values())
            if (value.name().equalsIgnoreCase(name))
                return value;
        return null;
    }
}

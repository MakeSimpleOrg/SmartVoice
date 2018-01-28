package com.diamond.SmartVoice.Controllers.Homey.json;

import com.google.gson.annotations.SerializedName;

/**
 * @author Dmitriy Ponomarev
 */
public class Settings {
    @SerializedName("zw_node_id")
    private String zw_node_id;

    @SerializedName("zw_manufacturer_id")
    public String zw_manufacturer_id;

    @SerializedName("zw_product_type_id")
    private String zw_product_type_id;

    @SerializedName("zw_product_id")
    private String zw_product_id;

    @SerializedName("zw_secure")
    private String zw_secure;

    @SerializedName("zw_battery")
    private String zw_battery;

    @SerializedName("zw_device_class_basic")
    private String zw_device_class_basic;

    @SerializedName("zw_device_class_generic")
    private String zw_device_class_generic;

    @SerializedName("zw_device_class_specific")
    private String zw_device_class_specific;

    @SerializedName("zw_wakeup_interval")
    private int zw_wakeup_interval;

    @SerializedName("zw_group_1")
    private String zw_group_1;

    @SerializedName("zw_group_2")
    private String zw_group_2;

    @SerializedName("zw_group_3")
    private String zw_group_3;
}

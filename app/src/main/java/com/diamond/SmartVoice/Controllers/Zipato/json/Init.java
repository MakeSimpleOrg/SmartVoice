package com.diamond.SmartVoice.Controllers.Zipato.json;

import com.google.gson.annotations.SerializedName;

/**
 * @author Dmitriy Ponomarev
 */
public class Init {
    @SerializedName("success")
    public boolean success;

    @SerializedName("jsessionid")
    public String jsessionid;

    @SerializedName("nonce")
    public String nonce;
}
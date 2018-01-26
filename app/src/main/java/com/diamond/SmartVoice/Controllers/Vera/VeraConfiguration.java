package com.diamond.SmartVoice.Controllers.Vera;

/**
 * @author Dmitriy Ponomarev
 */
public class VeraConfiguration {
    private String veraIpAddress;
    private Integer veraPort;
    private Boolean clearNames = true;

    public String getVeraIpAddress() {
        return veraIpAddress;
    }

    public void setVeraIpAddress(String ipAddress) {
        veraIpAddress = ipAddress;
    }

    public Integer getVeraPort() {
        return veraPort;
    }

    public void setVeraPort(Integer port) {
        veraPort = port;
    }

    public Boolean getClearNames() {
        return clearNames;
    }

    public void setClearNames(Boolean clearNames) {
        this.clearNames = clearNames;
    }
}
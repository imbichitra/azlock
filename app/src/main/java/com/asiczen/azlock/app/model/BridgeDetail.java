package com.asiczen.azlock.app.model;

public class BridgeDetail {
    private final String bridgeId;
    private final String password;

    public BridgeDetail(String bridgeId, String password) {
        this.bridgeId = bridgeId;
        this.password = password;
    }

    public String getBridgeId() {
        return bridgeId;
    }

    public String getPassword() {
        return password;
    }
}

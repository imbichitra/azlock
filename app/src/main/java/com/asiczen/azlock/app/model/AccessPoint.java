package com.asiczen.azlock.app.model;


/**
 * Created by user on 1/5/2016.
 */
public class AccessPoint {
    private final String BSSID;
    private String SSID;
    private final String capabilities;
    private int level;
    private int status;

    /*private static final int CONNECTED = 1001;
    private static final int DISCONNECTED = 1002;
    private static final int ASSOCIATED = 1003;
    private static final int ASSOCIATING = 1004;
    private static final int AUTHENTICATING = 1005;
    private static final int COMPLETED = 1006;
    private static final int AUTHENTICATION_FAILED = 1007;*/
    private static final int UNKNOWN = -1;

    /*public static HashMap<Integer, String> connectionStatusMap = new HashMap<Integer, String>(){{
        put(CONNECTED, "Connected");
        put(DISCONNECTED, "Disconnected");
        put(ASSOCIATED, "Associated");
        put(ASSOCIATING, "Associating");
        put(AUTHENTICATING, "Authenticating");
        put(AUTHENTICATION_FAILED, "Authentication Failed");
        put(COMPLETED, "Completed");
        put(UNKNOWN, "Unknown");
    }};*/

    /*public AccessPoint(String BSSID, String SSID, String capabilities, int level)
    {
        this.BSSID = BSSID;
        this.SSID = SSID;
        this.capabilities = capabilities;
        this.level = level;
        this.status = UNKNOWN;
    }*/

    public AccessPoint(String BSSID, String SSID)
    {
        this.BSSID = BSSID;
        this.SSID = SSID;
        this.capabilities = null;
        this.level = 0;
        this.status = UNKNOWN;
    }

    private String getBSSID() {
        return BSSID;
    }

    /*public int getLevel() {
        return level;
    }*/

    /*public int getStatus() {
        return status;
    }*/

    /*public String getSSID() {
        return SSID;
    }*/

    /*public void setLevel(int level) {
        this.level = level;
    }*/

    /*public void setSSID(String SSID) {
        this.SSID = SSID;
    }*/

    /*public void setStatus(int status) {
        this.status = status;
    }*/


    @Override
    public boolean equals(Object o) {
        return this.getBSSID().equals(((AccessPoint) o).getBSSID());
    }

    @Override
    public String toString() {
        return "SSID:"+SSID+"\nBSSID:"+BSSID+"\nStrength:"+level
                +"\nStatus:"+status+"\nCapabilities:"+capabilities;
    }
}

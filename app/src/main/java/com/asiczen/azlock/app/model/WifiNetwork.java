package com.asiczen.azlock.app.model;

import java.io.Serializable;

/**
 * Created by user on 2/10/2016.
 */
public class WifiNetwork implements Serializable{
    private final String SSID;
    private String password;
    private int security, autoConnect;

    private static final int NONE = 0;
    public static final int WPA = 1;

    public static final int UPDATE_SSID = 3;
    public static final int UPDATE_SECURITY = 4;
    public static final int UPDATE_PASSWORD = 5;
    public static final int UPDATE_AUTO_CONNECT = 6;
    public static final int UPDATE_ALL = 7;

    public WifiNetwork(String SSID, int security){
        this.SSID = SSID;
        this.security = security;
        autoConnect = 0;
    }

    public WifiNetwork(String SSID){
        this.SSID = SSID;
        this.security = -1;
        security = NONE;
        autoConnect = 0;
    }

    public String getSSID() {
        return SSID;
    }

    public int getAutoConnect() {
        return autoConnect;
    }

    public String getPassword() {
        return password;
    }

    public int getSecurity() {
        return security;
    }

    public void setAutoConnect(int autoConnect) {
        this.autoConnect = autoConnect;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        return this.getSSID().equals(((WifiNetwork) o).getSSID());
    }

    @Override
    public int hashCode(){
        return  getSSID()!=null ? getSSID().hashCode() : this.hashCode();
    }

    @Override
    public String toString() {
        return getSSID();
    }
}

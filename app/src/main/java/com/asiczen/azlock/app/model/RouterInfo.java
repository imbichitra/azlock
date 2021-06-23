package com.asiczen.azlock.app.model;

/*
 * Created by user on 10/5/2015.
 */
public class RouterInfo {
    private String address;
    private int port;

    public RouterInfo(String address, int port){
        this.address = address;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getAddress() {
        return address;
    }

    /*public void setAddress(String address) {
        this.address = address;
    }*/

    /*public void setPort(int port) {
        this.port = port;
    }*/

    @Override
    public String toString() {
        return "Address:"+getAddress()+"\n"+"Port:"+getPort()+"\n";
    }
}

package com.asiczen.azlock.net;

public interface MqttReceiveListener {
    void onConnect();
    void onDisconnect();
    void onDataAvailable(byte[] data);
    void onDelivery();
    void subscribed();
    void unableToSubscribe();
    void unableToPublish();
    void succOrFailToUnSubscribe();
}

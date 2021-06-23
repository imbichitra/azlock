package com.asiczen.azlock.net;

public interface MqttInterface {
    int UN_SUBSCRIBE = 1;
    int DISCONNECT_TO_MQTT = 2;
    int DEFAULT_WAIT_TIME = 45000;
    int DISCONNECT_TIME = 10000;
    void dataAvailable(byte[] data);
    void timeOutError();
    void unableToSubscribe();
    void succOrFailToUnSubscribe();
}

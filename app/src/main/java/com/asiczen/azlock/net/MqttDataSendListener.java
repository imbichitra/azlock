package com.asiczen.azlock.net;

public interface MqttDataSendListener {
    void sendData(byte[] data, String subscribeTopic, String publishTopic,int WAIT_TIME, MqttInterface myInterface);
    void unSubscribe();
    void disconnectMqtt();
}

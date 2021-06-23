package com.asiczen.azlock;

/**
 * Created by Somnath on 6/22/2016.
 */
public interface OnReceiveListener {
    void onConnect();
    void onDisconnect();
    void onDataAvailable(String data);
    void onDataAvailable(byte[] data);
    void onServicesDiscovered();
    void onError(int errorCode);
}

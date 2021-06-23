package com.asiczen.azlock;

/**
 * Created by Somnath on 7/5/2016.
 */
public interface OnDataSendListener {
    void onSend(byte[] data);
    void onSend(byte[] data, OnDataAvailableListener onDataAvailableListener,
                String progressMessage);
}

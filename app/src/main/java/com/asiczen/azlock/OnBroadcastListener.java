package com.asiczen.azlock;

/**
 * Created by user on 9/24/2015.
 */
public interface OnBroadcastListener {
    int SCAN_RESULTS_UPDATED = 110;
    int NEW_RSSI = 111;
    int CONNECTIVITY_CHANGED = 112;
    int ERROR_AUTHENTICATING = 113;
    int CONNECTED_WIFI_INFO = 114;
    int SUPPLICANT_NEW_STATE = 115;

    void onReceive(int resultCode, Object result);
}

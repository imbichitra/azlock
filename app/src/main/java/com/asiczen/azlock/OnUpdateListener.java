package com.asiczen.azlock;

/**
 * Created by user on 9/24/2015.
 */
public interface OnUpdateListener {

    int IMAGE_UPDATED = 16;
    int GUEST_UPDATED = 15;
    int GUEST_VIEW_UPDATED = 24;
    int LOG_UPDATED = 21;
    int BATTERY_STATUS_UPDATED = 18;
    int DOOR_NAME_UPDATED = 20;
    int LOCK_STATUS_UPDATED = 22;
    int TAMPER_NOTIFICATION_UPDATED = 23;
    int ASK_PIN_UPDATED = 25;
    int PLAY_SOUND_UPDATED = 26;
    int AJAR_UPDATE = 28;

    void onUpdate(int resultCode, Object result);
}

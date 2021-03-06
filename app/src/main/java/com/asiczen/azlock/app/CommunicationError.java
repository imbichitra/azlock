package com.asiczen.azlock.app;


import android.util.Log;

/**
 * Created by user on 1/21/2016.
 */
public class CommunicationError {

    private static final String TAG = CommunicationError.class.getSimpleName();

    protected static final int INVALID_COMMAND = 2;
    protected static final int INVALID_ACCESS_MODE = 3;
    protected static final int INVALID_CHECKSUM = 4;
    protected static final int INVALID_LENGTH = 5;
    protected static final int INVALID_OPTION = 6;
    protected static final int INVALID_PHONE_TIME = 7;
    protected static final int INVALID_ACCESS_TIME = 8;
    protected static final int GUEST_MAX_ATTEMPT = 9;
    protected static final int GUEST_KEY_NOT_FOUND = 10;
    protected static final int GUEST_AUTH_FAILED = 11;
    protected static final int OUT_OF_SPACE = 12;
    protected static final int DEVICE_NOT_CALIBRATED = 13;
    protected static final int NOT_HANDSHAKED = 14;
    private static final int BLE_NOT_FOUND = 15;
    private static final int WRONG_PASSWPRD = 16;
    private static final int AP_NOT_FOUND = 17;
    private static final int CONNECT_FAIL = 18;
    private static final int INVALID_DATA = 19;
    private static final int BLE_ALREADY_DISCONNECTED = 20;
    private static final int BLE_ALREADY_CONNECTED = 21;
    public static final int BLE_NOT_CONNECTED = 22;
    private static final int PREVIOUS_PACKET_NOT_ARRIVED = 23;
    public static final int DOOR_NOT_CLOSED = 24;
    private static final int IMEI_ALREADY_EXISTS = 25;
    private static final int NAME_ALREADY_EXISTS = 26;
    private static final int LOCK_LIST_FULL = 27;
    public static final int INVALID_RESET_CODE = 28;
    private static final int GUEST_NOT_FOUND = 29;
    private static final int DUPLICATE_LOCK = 30;
    private static final int CHANGE_PASSWORD = 31;
    private static final int BRIDGE_ID_EXIST = 32;
    private static final int BRIDGE_BUSY = 33;
    private static final int LOCK_NOT_FOUND = 35;
    private static final int WRONG_BRIDGE_ID_PASSWORD = 34;
    private static final int NO_INTERNET = 36;
    private static final int TIMEOUT = 37;
    private static final int RF_POWER = 38;

    private static String message = null;

    public static String getMessage(int errorCode)
    {
        switch (errorCode){
            case INVALID_COMMAND:
                message = "Invalid Command";
                Log.e(TAG, message);
                break;
            case INVALID_ACCESS_MODE:
                message = "Invalid Access Mode";
                Log.e(TAG, message);
                break;
            case INVALID_CHECKSUM:
                message = "Invalid Checksum";
                Log.e(TAG, message);
                break;
            case INVALID_LENGTH:
                message = "Invalid Length";
                Log.e(TAG, message);
                break;
            case INVALID_OPTION:
                message = "Invalid Option";
                Log.e(TAG, message);
                break;
            case INVALID_ACCESS_TIME:
                message = "Invalid Access Time";
                Log.e(TAG, message);
                break;
            case INVALID_PHONE_TIME:
                message = "Invalid Phone Time";
                Log.e(TAG, message);
                break;
            case GUEST_MAX_ATTEMPT:
                message = "Maximum Attempt";
                Log.e(TAG, message);
                break;
            case GUEST_KEY_NOT_FOUND:
                message = "Key not found";
                Log.e(TAG, message);
                break;
            case GUEST_AUTH_FAILED:
                message = "Authentication Failed";
                Log.e(TAG, message);
                break;
            case OUT_OF_SPACE:
                message = "Out of Space";
                Log.e(TAG, message);
                break;
            case DEVICE_NOT_CALIBRATED:
                message = "Device not Calibrated";
                Log.e(TAG, message);
                break;
            case NOT_HANDSHAKED:
                message = "Packet Length Mismatch";
                Log.e(TAG, message);
                break;
            case BLE_ALREADY_CONNECTED:
                message = "Device already connected";
                Log.e(TAG, message);
                break;
            case BLE_ALREADY_DISCONNECTED:
                message = "Device already disconnected";
                Log.e(TAG, message);
                break;
            case BLE_NOT_CONNECTED:
                message = "Device not connected";
                Log.e(TAG, message);
                break;
            case BLE_NOT_FOUND:
                message = "Device not found";
                Log.e(TAG, message);
                break;
            case CONNECT_FAIL:
                message = "Connection failed";
                Log.e(TAG, message);
                break;
            case INVALID_DATA:
                message = "Invalid data";
                Log.e(TAG, message);
                break;
            case WRONG_PASSWPRD:
                message = "Wrong password";
                Log.e(TAG, message);
                break;
            case PREVIOUS_PACKET_NOT_ARRIVED:
                message = "previous packet not arrived";
                Log.e(TAG, message);
                break;
            case AP_NOT_FOUND:
                message = "AP not found";
                Log.e(TAG, message);
                break;
            case DOOR_NOT_CLOSED:
                message = "Close the door properly";
                Log.e(TAG, message);
                break;
            case IMEI_ALREADY_EXISTS:
                message = "User already registered";
                Log.e(TAG, message);
                break;
            case NAME_ALREADY_EXISTS:
                message = "Name already registered";
                Log.e(TAG, message);
                break;
            case GUEST_NOT_FOUND:
                message = "Guest doesn't exist !";
                Log.e(TAG, message);
                break;
            case LOCK_LIST_FULL:
                message = "Lock list full, you can't add more than 5 lock";
                Log.e(TAG, message);
                break;
            case DUPLICATE_LOCK:
                message = "This Lock is already added";
                Log.e(TAG, message);
                break;
            case CHANGE_PASSWORD:
                message = "Old password is incorrect";
                Log.e(TAG, message);
                break;
            case BRIDGE_ID_EXIST:
                message = "The Bridge id is taken,Please try another one";
                Log.e(TAG, message);
                break;
            case LOCK_NOT_FOUND:
                message = "Lock not found please refresh the list";
                Log.e(TAG, message);
                break;
            case WRONG_BRIDGE_ID_PASSWORD:
                message = "Invalid password";
                Log.e(TAG, message);
                break;
            case BRIDGE_BUSY:
                message = "Bridge is busy try after some time";
                Log.e(TAG, message);
                break;
            case NO_INTERNET:
                message = "There is no internet availability to your bridge";
                Log.e(TAG, message);
                break;
            case TIMEOUT:
                message = "Unable to contact to server,please check your internet";
                Log.e(TAG, message);
                break;
            case RF_POWER:
                message = "RF Configuration failed.";
                Log.e(TAG, message);
                break;
        }
        return message;
    }
}

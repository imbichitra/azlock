package com.asiczen.azlibrary;

class CommunicationError {
    private static final int INVALID_COMMAND = 2;
    private static final int INVALID_ACCESS_MODE = 3;
    private static final int INVALID_CHECKSUM = 4;
    private static final int INVALID_LENGTH = 5;
    private static final int INVALID_OPTION = 6;
    private static final int INVALID_PHONE_TIME = 7;
    private static final int INVALID_ACCESS_TIME = 8;
    private static final int GUEST_MAX_ATTEMPT = 9;
    private static final int GUEST_KEY_NOT_FOUND = 10;
    private static final int GUEST_AUTH_FAILED = 11;
    private static final int OUT_OF_SPACE = 12;
    private static final int DEVICE_NOT_CALIBRATED = 13;
    private static final int NOT_HANDSHAKED = 14;
    private static final int BLE_NOT_FOUND = 15;
    private static final int WRONG_PASSWPRD = 16;
    private static final int AP_NOT_FOUND = 17;
    private static final int CONNECT_FAIL = 18;
    private static final int INVALID_DATA = 19;
    private static final int BLE_ALREADY_DISCONNECTED = 20;
    private static final int BLE_ALREADY_CONNECTED = 21;
    private static final int BLE_NOT_CONNECTED = 22;
    private static final int PREVIOUS_PACKET_NOT_ARRIVED = 23;
    private static final int DOOR_NOT_CLOSED = 24;
    private static final int IMEI_ALREADY_EXISTS = 25;
    private static final int NAME_ALREADY_EXISTS = 26;
    //public static final int INVALID_RESET_CODE = 28;

    private static String message = null;

    public static String getMessage(int errorCode)
    {
        switch (errorCode){
            case INVALID_COMMAND:
                message = "Invalid Command";
                break;
            case INVALID_ACCESS_MODE:
                message = "Invalid Access Mode";
                break;
            case INVALID_CHECKSUM:
                message = "Invalid Checksum";
                break;
            case INVALID_LENGTH:
                message = "Invalid Length";
                break;
            case INVALID_OPTION:
                message = "Invalid Option";
                break;
            case INVALID_ACCESS_TIME:
                message = "Invalid Access Time";
                break;
            case INVALID_PHONE_TIME:
                message = "Invalid Phone Time";
                break;
            case GUEST_MAX_ATTEMPT:
                message = "Maximum Attempt";
                break;
            case GUEST_KEY_NOT_FOUND:
                message = "Key not found";
                break;
            case GUEST_AUTH_FAILED:
                message = "Authentication Failed";
                break;
            case OUT_OF_SPACE:
                message = "Out of Space";
                break;
            case DEVICE_NOT_CALIBRATED:
                message = "Device not Calibrated";
                break;
            case NOT_HANDSHAKED:
                message = "Packet Length Mismatch";
                break;
            case BLE_ALREADY_CONNECTED:
                message = "Device already connected";
                break;
            case BLE_ALREADY_DISCONNECTED:
                message = "Device already disconnected";
                break;
            case BLE_NOT_CONNECTED:
                message = "Device not connected";
                break;
            case BLE_NOT_FOUND:
                message = "Device not found";
                break;
            case CONNECT_FAIL:
                message = "Connection failed";
                break;
            case INVALID_DATA:
                message = "Invalid data";
                break;
            case WRONG_PASSWPRD:
                message = "Wrong password";
                break;
            case PREVIOUS_PACKET_NOT_ARRIVED:
                message = "previous packet not arrived";
                break;
            case AP_NOT_FOUND:
                message = "AP not found";
                break;
            case DOOR_NOT_CLOSED:
                message = "Close the door properly";
                break;
            case IMEI_ALREADY_EXISTS:
                message = "User already registered";
                break;
            case NAME_ALREADY_EXISTS:
                message = "Name already registered";
                break;
        }
        return message;
    }
}

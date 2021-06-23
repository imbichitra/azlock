package com.asiczen.azlibrary;

interface Packet {
    interface BatteryPacket{
        char REQUEST_TYPE = 'K';
        char MODE = 'V';
        int SENT_PACKET_LENGTH = 4;
        char BATTERY = 'B';

        int BATTERY_STATUS_POS = 4;
    }
    interface HandshakePacket
    {
        int SENT_PACKET_LENGTH = 10;
        int REGISTRATION_STATUS_POS = 2;
        int BATTERY_STATUS_POS = 5;
    }
    interface LockPacket
    {
        int RECEIVED_PACKET_LENGTH = 5;
        int SENT_PACKET_LENGTH = 17;
        int DOOR_STATUS_REQUEST_POSITION = 3;
        int CURRENT_DATE_TIME_START = 10;
        int CHECKSUM_SENT = 16;
        int LOCK_STATUS_POS = 2;
    }
    interface RegisterGuestPacket
    {

        int SENT_PACKET_LENGTH_ADD_GUEST_2 = 17;


        int GUEST_NAME_START = 9;
        //int GUEST_PHONE_START = 26;
        int ACCESS_TYPE_POSITION = 31; //18 //3;

        int PHONE_MAC_ID_START = 3;

    }
    interface AjarPacket{
        int SENT_PACKET_LENGTH = 6;
        /* SEND PACKET PARAMETERS */
        int AJAR_STATUS_POSITION = 3;
        int AUTOLOCK_STATUS_POSITION =4 ;
        int AUTOLOCK_TIME_POSITION = 5;

        /* RECEIVED PACKET PARAMETERS */
        int AJAR_STATUS = 17;
        int AUTOLOCK_STATU = 18;
        int AUTOLOCK_TIME = 19;
        int STATUS=2;

        /*success and fail status */
        char SUCCESS = 'S';

    }
}

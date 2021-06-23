package com.asiczen.azlock.util;

/*
 * Created by user on 8/18/2015.
 */
public interface Packet {
    int REQUEST_PACKET_TYPE_POS = 0;
    int REQUEST_ACCESS_MODE_POS = 1;
    int REQUEST_PACKET_LENGTH_POS = 2;
    int REQUEST_PACKET_LENGTH_POS1 = 1;
    int RESPONSE_PACKET_TYPE_POS = 0;
    int RESPONSE_PACKET_LENGTH_POS = 3;
    int RESPONSE_ACTION_STATUS_POS = 2;
    int RESPONSE_COMMAND_STATUS_POS = 1;

    //int RESPONSE_ACCESS_MODE_POS = 2;
    //int RESPONSE_DEVICE_STATUS_POS = 3;
    int MAX_PKT_SIZE =32;
    char SUCCESS = 'S';
    char FAILURE = 'F';

    interface HandshakePacket
    {
        /* Handshake Packet Details */
        int RECEIVED_PACKET_LENGTH = 20;
        int SENT_PACKET_LENGTH = 10;

        /* SEND PACKET PARAMETERS */
        //int PHONE_MAC_START = 3;
        //int TEST_NUM_POS = 3;
        //int PHONE_IP_ADDRESS_START = 9;

        /* RECEIVED PACKET PARAMETERS */
        int REGISTRATION_STATUS_POS = 2;
        int PACKET_LENGTH_POS = 3;
        int DOOR_STATUS_POS = 4;
        int BATTERY_STATUS_POS = 5;
        int OWNER_NAME_START = 6;
        //int OWNER_PHONE_START = 6;
        int TAMPER_STATUS_POS = 16;
        //int DOOR_NAME_START = 68;
        //int OWNER_PIN_START = 84;
        //int DOOR_MAC_ID_START = 84;
        //int CHECKSUM_RECV = 17;

        /* TAMPER STATUS */
        char TAMPERED = '1';
        char NOT_TAMPERED = '0';
    }

    interface OwnerRegistrationPacket
    {
        /* Owner Packet Details */
        int SENT_PACKET_LENGTH = 20;
        int RECEIVED_PACKET_LENGTH = 5;

        //char REGISTER = 'R';
        //char EDIT_OWNER_DETAILS = 'E';
        //char CHANGE_PIN = 'P';
        char DELETE_ALL_GUESTS='2';
        char DELETE_ALL_LOGS='1';
        char DELETE_ALL_LOGS_AND_GUESTS='3';
        char DELETE_NOTHING='0';

        /* Options */
        //char PACKET_NEW_OWNER_ID = '8';
        //char PACKET_ALT_OWNER_ID = '9';

        int OWNER_NAME_START = 9;
        int DELETE_FLAG = 19;
        int RESET_CODE_POS = 20;

        /* RECEIVED PACKET PARAMETERS */
        //int PACKET_ID_POS = 4;
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
    interface TamperPacket
    {
        int SENT_PACKET_LENGTH = 15;

        /* Options */
        char ENABLE = 'E';
        char DISABLE = 'D';

        /* SEND PACKET PARAMETERS */
        int NOTIFICATION_POSITION = 3;
        int OWNER_PHONE_START = 4;

        /* RECEIVED PACKET PARAMETERS */
        //int CHECKSUM_RECV = 4;
    }

    interface RemoteConnectionModePacket
    {
        /* Lock Packet Details */
        int RECEIVED_PACKET_LENGTH = 5;
        int SENT_PACKET_LENGTH = 11;

        /* Options */
        char CONNECT = 'C';
        char DISCONNECT = 'D';

        /* SEND PACKET PARAMETERS */
        int CONNECTION_MODE_POSITION = 3;
        int DOOR_MAC_ID_START = 4;

        /* RECEIVED PACKET PARAMETERS */
        //int CHECKSUM_RECV = 4;
    }

    interface ScanLockPacket{
        /* Lock Packet Details */
        int RECEIVED_PACKET_LENGTH_MIN = 6;
        //int RECEIVED_PACKET_LENGTH_MAX = 116;
        int SENT_PACKET_LENGTH = 49;//36
        int LENGTH = 36;

        /* Options */
        char SCAN = 'S';
        char REFRESH = 'R';
        int ONE_DEVICE_DETAILS_LENGTH=22;
        int DOOR_MAC_LENGTH=6;

        /* SEND PACKET PARAMETERS */
        int SCAN_POSITION1 = 2;

        /* RECEIVED PACKET PARAMETERS */
        int NUMBER_OF_AVAILABLE_DEVICES_POSITION=4;
        int DEVICE_1_MAC_START=5;
        int DOOR_1_NAME_START=11;
        int BRIDGE_ID_START_POSITION = 4;
        int BRIDGE_PASSWORD_START_POSITION = 20;
        int BRIDGE_KEY_START_POSITION = 36;
        //int DEVICE_2_MAC_START=27;
        //int DOOR_2_NAME_START=33;
        //int DEVICE_3_MAC_START=49;
        //int DOOR_3_NAME_START=55;
        //int DEVICE_4_MAC_START=71;
        //int DOOR_4_NAME_START=77;
        //int DEVICE_5_MAC_START=93;
        //int DOOR_5_NAME_START=99;
        //int CHECKSUM_RECV = 115;
    }

    interface LockPacket
    {
        /* Lock Packet Details */
        int RECEIVED_PACKET_LENGTH = 5;
        int SENT_PACKET_LENGTH = 17;

        /* SEND PACKET PARAMETERS */
        int DOOR_STATUS_REQUEST_POSITION = 3;
        int CURRENT_DATE_TIME_START = 10;

        /* RECEIVED PACKET PARAMETERS */
        int LOCK_STATUS_POS = 2;
        /*int BATTERY_STATUS = 4;
        int CHECKSUM_RECV = 4;*/
    }

    interface RegisterGuestPacket
    {
        /* RegsterGuest Packet Details */
        /*int RECEIVED_PACKET_LENGTH_ADD_GUEST = 6;
        int SENT_PACKET_LENGTH_ADD_GUEST_1 = 19;*/
        int SENT_PACKET_LENGTH_ADD_GUEST_2 = 17;

        /* Action Commands */
        //char ADD_GUEST = 'A';

        /* Options */
        /*char PACKET_1_ID = '0';
        char PACKET_2_ID = '1';*/

        /* SEND PACKET PARAMETERS */
        int GUEST_NAME_START = 9;
        //int GUEST_PHONE_START = 26;
        int ACCESS_TYPE_POSITION = 31; //18 //3;
        int START_DATE_OF_ACCESS_START = 19; //4;
        int START_TIME_OF_ACCESS_START = 23; //8;
        int END_DATE_OF_ACCESS_START = 25; //10;
        int END_TIME_OF_ACCESS_START = 29; //14;
        int PHONE_MAC_ID_START = 3;
        //int CHECKSUM_SENT_1 = 18;
        //int CHECKSUM_SENT_2 = 16;

        /* RECEIVED PACKET PARAMETERS */
        /*int PACKET_ID = 4;
        int CHECKSUM_RECV = 5;*/
    }

    interface DeleteGuestPacket
    {
        /* Delete Packet Details */
        int RECEIVED_PACKET_LENGTH_DELETE_GUEST = 5;
        int SENT_PACKET_LENGTH_DELETE_SELECTED_GUEST = 10;
        int SENT_PACKET_LENGTH_DELETE_ALL_GUEST = 4;

        /* Action Commands */
        //char DELETE_GUEST = 'D';
        char DELETE_ALL = '1';
        char DELETE_SELECTED = '0';

        /* SEND PACKET PARAMETERS */
        int GUEST_MAC_START = 3;

        /* RECEIVED PACKET PARAMETERS */
        //int CHECKSUM_RECV = 4;
    }

    interface LogRequestPacket
    {
        /* LogRequest Packet Details */
        int RECEIVED_PACKET_LENGTH = 20;
        int SENT_PACKET_LENGTH = 5;

        /* SEND PACKET PARAMETERS */
        int LOG_READ_FLAG = 3;
        char PACKET_ID = '6';

        /* RECEIVED PACKET PARAMETERS */
        int PACKET_ID_POS = 4;
        int GUEST_MAC_START = 5;
        int ACCESS_DATE_START = 11;
        //int ACCESS_TIME_START = 15;
        int ACCESS_STATUS_POS = 17;
        int ACCESS_FAILURE_REASON_CODE_POS = 18;
        //int CHECKSUM_RECV = 19;
    }

    interface LogDeletePacket
    {
        int SENT_PACKET_LENGTH = 18;

        /* SEND PACKET PARAMETERS */
        int DELETE_ALL_FLAG_POS = 3;
        int GUEST_MAC_START = 4;
        int ACCESS_DATE_START = 10;
        //int ACCESS_TIME_START = 14;
        int ACCESS_STATUS_POS = 16;
        //int ACCESS_FAILURE_REASON_POS = 17;

        char PACKET_ID = '7';

        /* RECEIVED PACKET PARAMETERS */
        int PACKET_ID_POS = 4;
        //int CHECKSUM_RECV = 4;
    }

    interface ConfigPacket {
        int SENT_CONFIG_TIME_PACKET_LENGTH = 10;
        //int SENT_CALIBRATION_PACKET_LENGTH = 5;

        /* SEND PACKET PARAMETERS */
        int CURRENT_DATE_TIME_POSITION = 3;

        /* RECEIVED PACKET PARAMETERS */
        //int CHECKSUM_RECV = 4;
    }

    interface NewOwnerPacket
    {
        int SENT_PACKET_LENGTH = 12;

        /* SEND PACKET PARAMETERS */
        //int DEVICE_MAC_ID_START = 3;
        //int PIN_START = 9;

        /* RECEIVED PACKET PARAMETERS */
        //int CHECKSUM_RECV = 6;
        //int ACTION_STATUS_POS = 4;
        //int PACKET_LENGTH_POS = 5;
    }

    interface UpdateGuestPacket
    {
        /* Packet Details */
        int RECEIVED_PACKET_1_LENGTH = 20;
        int RECEIVED_PACKET_2_LENGTH = 20;
        int SENT_PACKET_LENGTH = 5;

        /* OPTION */
        char REFRESH_GUEST_LIST = 'R';
        //char PACKET_1 = '1';
        //char PACKET_2 = '2';
        //char PACKET_1_ID = '4';
        //char PACKET_2_ID = '5';

        /* SEND PACKET PARAMETERS */
        int REFRESH_FLAG = 3;

        /* RECEIVED PACKET PARAMETERS */
        int SEQUENCE_NUMBER_POS = 5;
        int PACKET_TYPE_POSITION = 4;
        int GUEST_NAME_START = 12;
        int ACCESS_TYPE_POSITION = 8;
        int START_ACCESS_DATE_POS = 9;
        //int START_ACCESS_TIME_POS = 11;
        int END_ACCESS_DATE_POS = 13;
        //int END_ACCESS_TIME_POS = 17;
        int PHONE_MAC_ID_START = 6;
        int CHECKSUM_RECV_1 = 19;
        //int CHECKSUM_RECV_2 = 19;
        //int PACKET_ID = 4;
    }

    interface SetupPacket
    {
        /* Setup Packet Details */
        int SENT_PACKET_LENGTH = 5;

        /* SEND PACKET PARAMETERS */
        //int DISTANCE_POS = 3;

        /* RECEIVED PACKET PARAMETERS */
        //int CHECKSUM_RECV = 4;
    }

    interface SelftestPacket
    {
        /* Setup Packet Details */
        int SENT_PACKET_LENGTH = 11;

        /* SEND PACKET PARAMETERS */
        //int DEVICE_MAC_START = 3;
        //int TEST_NUM_POS = 3;
        //int SELFTEST_CMD_POS = 9;

        /* RECEIVED PACKET PARAMETERS */
        //int OWNER_STATUS_POS = 2;
        //int DEVICE_STATUS_POS = 3;
        //int ACTION_STATUS_POS = 4;
        //int PACKET_LENGTH_POS = 5;
        //int CHECKSUM_RECV = 6;
    }

    interface RouterConfigPacket
    {
        /* Setup Packet Details */
        int RECEIVED_PACKET_LENGTH = 5;
        int SENT_PACKET_LENGTH = 68;

        /* SEND PACKET PARAMETERS */
        int ROUTER_SSID_START = 4;
        //int ROUTER_SEC_TYPE_POS = 35;
        int ROUTER_PASSOWRD_START = 36;
        //int ROUTER_PORT_START = 80;

        /* RECEIVED PACKET PARAMETERS */
        //int CHECKSUM_RECV = 4;
    }

    interface BridgeParam{
        /* Setup Packet Details */

        int SENT_PACKET_LENGTH = 36;

        int BRIDGE_SSID_START = 4;
        int BRIDGE_PASSWORD_START = 20;
    }
    interface BrockerPacket{
        /* Setup Packet Details */

        int SENT_PACKET_LENGTH = 30;

        int BROCKER_IP_START = 4;
        int BROCKER_PORT_START = 8;
        int USER_ID_START = 10;
        int PASSWORD_START = 20;
    }

    interface LockDetail{
        int SENT_PACKET_LENGTH = 5;

        int LOCK_MAC_ID_START = 4;
        int LOCK_MAC_ID_SIZE = 6;
    }

    interface BridgeLockOperationPacket
    {
        /* Type of operation */
        char ADD_LOCK='A';
        char DELETE_LOCK='D';
        char REFRESH_LOCK_LIST='R';

        /* SEND PACKET PARAMETERS */
        int OPERATION_TYPE=3;

        /* RECEIVED PACKET PARAMETERS */
        //int ACTION_STATUS_POS = 4;
        //int RECV_PACKET_LENGTH_POS = 5;
        //int CHECKSUM_RECV = 6;
    }

    interface AddDeleteLockPacket extends BridgeLockOperationPacket{
        /* Setup Packet Details */
        int RECEIVED_PACKET_LENGTH = 5;
        int SENT_PACKET_LENGTH = 11;

        int CHECKSUM_SENT = 10;

        /* RECEIVED PACKET PARAMETERS */
        //int CHECKSUM_RECV = 4;
    }

    interface RefreshLockPacket extends BridgeLockOperationPacket{
        /* Setup Packet Details */
        int RECEIVED_PACKET_LENGTH_MIN = 6;
        //int RECEIVED_PACKET_LENGTH_MAX = 36;
        int SENT_PACKET_LENGTH = 5;

        /* SEND PACKET PARAMETERS */

        /* RECEIVED PACKET PARAMETERS */
        int NUMBER_OF_LOCKS =4;
        int LOCK_MAC_ID_START = 5;
        int LOCK_MAC_ID_SIZE = 6;
    }

    interface DoorSettingsPacket
    {
        int SENT_PACKET_LENGTH = 20;

        /* SETTINGS OPTIONS */
        //char RENAME_DOOR = 'R';
        char CHANGE_PASSWORD = 'P';

        /* SEND PACKET PARAMETERS */
        int SETTINGS_OPTION_POS = 3;
        int DOOR_NAME_START = 3;
        int DOOR_PASSWORD_START = 4;
    }

    interface FactoryResetPacket {
        int SENT_PACKET_LENGTH = 9;
        /* SEND PACKET PARAMETERS */
        int RESET_POS = 3;
        int RESET_CODE_POS0=5;
        int RESET_CODE_POS1=6;
        int RESET_CODE_POS2=7;
        int RESET_CODE_POS3=8;
        int RESET_ERR_CODE_POS = 1;
    }

    interface BatteryCount{
        int PACKET_LENGTH = 4;
        char IDENTIFIER = 'C';
        int BATTERY_COUNT_START = 4;
    }

    interface PublishTopic{
        //String MQTT_BROKER_URL = "tcp://13.127.109.11:1883";
        String MQTT_BROKER_URL = "ssl://iot.asiczen.com:8883";
        ///String MQTT_BROKER_URL = "ssl://13.127.109.11:8883";
        //publish topic
        String PUBLISH_TOPIC = "BridgeId";
        String SUBSCRIBE_TOPIC_YES = "yes";
        String SUBSCRIBE_TOPIC_NO = "no";
        /*String BRIDGE_SCAN = "BridgeId_SCAN";
        String BRIDGE_CONNECT = "BridgeId_CONNECT";
        String BRIDGE_WHOAMI = "BridgeId_WHOAMI";
        String BRIDGE_OWNER_REG = "BridgeId_OWNER_REG";
        String BRIDGE_OWNER_CHANGE = "BridgeId_OWNER_CHANGE";
        String BRIDGE_DOOR_ACCESS = "BridgeId_DOOR_ACCESS";
        String BRIDGE_GUEST_ADD = "BridgeId_GUEST_ADD";
        String BRIDGE_GUEST_ACCESS_DEL = "BridgeId_GUEST_ACCESS_DEL";
        String BRIDGE_GUEST_ACCESS_DEL_ALL = "BridgeId_GUEST_ACCESS_DEL_ALL";
        String BRIDGE_GUEST_ACCESS_REFRESH = "BridgeId_GUEST_ACCESS_REFRESH";
        String BRIDGE_LOG_FETCH = "BridgeId_LOG_FETCH";
        String BRIDGE_LOG_DELETE = "BridgeId_LOG_DELETE";
        String BRIDGE_TAMPER_NOTIFICATION = "BridgeId_TAMPER_NOTIFICATION";
        String BRIDGE_RESET = "BridgeId_RESET";
        String BRIDGE_VERSION = "BridgeId_VERSION";
        String BRIDGE_AJAR_CONFIG = "BridgeId_AJAR_CONFIG";*/
    }

    interface SubscribeTopic{
        //subscribe topic
        int SUCCESS = 1;
        int FAIL = 0;
        String SUBSCRIBE_TOPIC = "BridgeId_RESPONSE";
        /*String BRIDGE_SCAN = "BridgeId_SCAN_RESPONSE";
        String BRIDGE_CONNECT = "BridgeId_CONNECT_RESPONSE";
        String BRIDGE_WHOAMI = "BridgeId_WHOAMI_RESPONSE";
        String BRIDGE_OWNER_REG = "BridgeId_OWNER_REG_RESPONSE";
        String BRIDGE_OWNER_CHANGE = "BridgeId_OWNER_CHANGE_RESPONSE";
        String BRIDGE_DOOR_ACCESS = "BridgeId_DOOR_ACCESS_RESPONSE";
        String BRIDGE_GUEST_ADD = "BridgeId_GUEST_ADD_RESPONSE";
        String BRIDGE_GUEST_ACCESS_DEL = "BridgeId_GUEST_ACCESS_DEL_RESPONSE";
        String BRIDGE_GUEST_ACCESS_DEL_ALL = "BridgeId_GUEST_ACCESS_DEL_ALL";
        String BRIDGE_GUEST_ACCESS_REFRESH = "BridgeId_GUEST_ACCESS_REFRESH_RESPONSE";
        String BRIDGE_LOG_FETCH = "BridgeId_LOG_FETCH_RESPONSE";
        String BRIDGE_LOG_DELETE = "BridgeId_LOG_DELETE_RESPONSE";
        String BRIDGE_TAMPER_NOTIFICATION = "BridgeId_TAMPER_NOTIFICATION_RESPONSE";
        String BRIDGE_RESET = "BridgeId_RESET_RESPONSE";
        String BRIDGE_VERSION = "BridgeId_VERSION_RESPONSE";
        String BRIDGE_AJAR_CONFIG = "BridgeId_AJAR_CONFIG_RESPONSE";*/
    }
}

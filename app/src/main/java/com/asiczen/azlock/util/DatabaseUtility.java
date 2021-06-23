package com.asiczen.azlock.util;

/*
 * Created by user on 8/13/2015.
 */
public interface DatabaseUtility {

    // Database Version
    int DATABASE_VERSION = 5;
    int UTIL_DATABASE_VERSION = 3;
    // Database Name
    String DATABASE_NAME = "lacerta";

    interface UserTable
    {
        String TABLE_NAME = "user";

        // Table Columns names for USER_TABLE
        String ID = "u_id";
        String NAME = "u_name";
        String PHONE = "u_phone";
        String EMAIL = "u_email";
        String IMAGE = "u_image";
        String PIN = "pin";
        String ACCESS_MODE = "access_mode";
        String ACCESS_TYPE = "access_type";
        String START_ACCESS_DATETIME = "start_access";
        String END_ACCESS_DATETIME = "end_access";
    }
    /*
    * many-to-many relationship (one user can register for multiple doors, also one door can register to multiple user)
    */
    interface RegisteredDoorTable
    {
        String TABLE_NAME = "Registered_Door";

        String ID = "reg_door_id";
        String USER_ID = "user_id";
        String DOOR_ID = "door_id";
        String KEY_STATUS = "key_status";
    }

    interface DoorTable
    {
        String TABLE_NAME = "door";

        String ID = "d_id";
        String NAME = "d_name";
        String ROUTER_ADDRESS = "router_addr";
        String ROUTER_PORT = "router_port";
        String DOOR_IP = "door_ip";
        String SUBNET_MASK = "subnet_mask";
        String DEFAULT_GATEWAY = "default_gateway";
    }

    /*
    * one-to-many relationship (one user can have multiple logs)
    * one-to-many relationship (one door can have multiple logs)
    */
    interface LogTable
    {
        String TABLE_NAME = "log";

        String ID = "l_id";
        String ACCESS_DATE_TIME = "l_datetime";
        String ACCESS_STATUS = "l_status";
        String ACCESS_FAILURE_REASON = "f_reason";
        String USER_ID = "user_id";
        String DOOR_ID = "door_id";
    }

    interface WifiConfigTable
    {
        String TABLE_NAME = "wifi_network";
        String ID = "net_id";
        String SSID = "net_ssid";
        String SECURITY = "net_security";
        String PASSWORD = "net_password";
        String AUTO_CONNECT = "net_connect";
    }
    interface DisplayTable{
        String TABLE_NAME="display_table";
        String ID="id";
        String DOOR_NAME="door_name";
        String MAC_ID="mac_id";
        String DATE_TIME = "created_at";
    }

    interface BridgeTable{
        String TABLE_NAME = "bridge_table";
        String BRIDGE_ID = "bridgeid";
        String PASSWORD = "password";
    }
}

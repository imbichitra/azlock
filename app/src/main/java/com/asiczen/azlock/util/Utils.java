package com.asiczen.azlock.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;

import com.asiczen.azlock.app.CommunicationError;
import com.asiczen.azlock.app.model.Guest;
import com.asiczen.azlock.app.model.WifiNetwork;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.security.CryptoUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Utils extends CommunicationError {
    @SuppressLint("StaticFieldLeak")
    public static final ViewGroup nullParent = null;
    private static final String TAG = Utils.class.getSimpleName();
    public static BluetoothAdapter bluetoothAdapter = null;
    public static final char LOCKED = 'L';
    public static final char UNLOCKED = 'U';
    public static final int SUCCESS = 1;
    public static final int FAILURE = -1;
    public static final int CANCELLED = 0;
    public static final int GUEST_EDIT_PROFILE_CODE = 2;
    public static final int GUEST_REGISTRATION_CODE = 3;
    public static final int DATABASE_ERROR_CODE = 101;
    public static final int DEVICE_ERROR_CODE = 102;
    public static final String EXTRA_CALLER_ACTIVITY_NAME = "callerActivity";
    public static final String EXTRA_DOWNLOAD_LOG = "downloadLog";
    /* Group Categories */
    public static final char LIMITED_TIME_ACCESS = 'L';
    public static final char FULL_TIME_ACCESS = 'F';
    // length of guest and owner strings
    private static final int MASTER_NAME_LENGTH = 32;
    private static final int MASTER_PASSKEY_LENGTH = 32;
    private static final int MASTER_PHONE_LENGTH = 13;
    private static final int MASTER_EMAIL_LENGTH = 32;
    private static final int MASTER_ADDRESS_LENGTH = 96;
    //private static final int MASTER_DOOR_LENGTH = 16;
    private static final int MASTER_NAME_START = 0;
    private static final int MASTER_PASSKEY_START = MASTER_NAME_START + MASTER_NAME_LENGTH;
    private static final int MASTER_PHONE_START = MASTER_PASSKEY_START + MASTER_PASSKEY_LENGTH;
    private static final int MASTER_EMAIL_START = MASTER_PHONE_START + MASTER_PHONE_LENGTH;
    private static final int MASTER_ADDRESS_START = MASTER_EMAIL_START + MASTER_EMAIL_LENGTH;
    //private static final int MASTER_DOOR_START = MASTER_ADDRESS_START + MASTER_ADDRESS_LENGTH;
    private static final int GUEST_NAME_LENGTH = 32;
    private static final int GUEST_PHONE_LENGTH = 13;
    private static final int GUEST_PASSKEY_LENGTH = 32;
    private static final int GUEST_EMAIL_LENGTH = 64;
    private static final int GUEST_ADDRESS_LENGTH = 128;
    private static final int GUEST_GROUP_LENGTH = 6;
    private static final int GUEST_DATE_LENGTH = 10;
    private static final int GUEST_TIME_LENGTH = 7;
    private static final int GUEST_DURATION_LENGTH = 3;
    private static final int GUEST_KEY_LENGTH = 32;
    private static final int GUEST_KEY_STS_LENGTH = 16;
    private static final int GUEST_NAME_START = 0;
    private static final int GUEST_PHONE_START = GUEST_NAME_START + GUEST_NAME_LENGTH;
    private static final int GUEST_PASSKEY_START = GUEST_PHONE_START + GUEST_PHONE_LENGTH;
    private static final int GUEST_EMAIL_START = GUEST_PASSKEY_START + GUEST_PASSKEY_LENGTH;
    private static final int GUEST_ADDRESS_START = GUEST_EMAIL_START + GUEST_EMAIL_LENGTH;
    private static final int GUEST_GROUP_START = GUEST_ADDRESS_START + GUEST_ADDRESS_LENGTH;
    private static final int GUEST_DATE_START = GUEST_GROUP_START + GUEST_GROUP_LENGTH;
    private static final int GUEST_TIME_START = GUEST_DATE_START + GUEST_DATE_LENGTH;
    private static final int GUEST_DURATION_START = GUEST_TIME_START + GUEST_TIME_LENGTH;
    private static final int GUEST_KEY_START = GUEST_DURATION_START + GUEST_DURATION_LENGTH;
    private static final int GUEST_KEY_STS_START = GUEST_KEY_START + GUEST_KEY_LENGTH;
    //private static final int GUEST_INFO_LENGTH = GUEST_KEY_STS_START + GUEST_KEY_STS_LENGTH;
    //public static final int GUEST_LOG_NAME_START  = 6;
    private static final int GUEST_LOG_ADDRESS_START  = 6;
    private static final int GUEST_LOG_ADDRESS_LENGTH  = 128;
    private static final int GUEST_LOG_PHONE_START  = GUEST_LOG_ADDRESS_START + GUEST_LOG_ADDRESS_LENGTH;
    private static final int GUEST_LOG_PHONE_LENGTH = 13;
    private static final int GUEST_LOG_GNAME_START = GUEST_LOG_PHONE_START + GUEST_LOG_PHONE_LENGTH;
    private static final int GUEST_LOG_GNAME_LENGTH = 32;
    //private static final int GUEST_LOG_START  = GUEST_LOG_GNAME_START + GUEST_LOG_GNAME_LENGTH;
    private static final int APP_MODE_KEY_START = 6;
    private static final int APP_MODE_KEY_LENGTH = 17;
    private static final int APP_MODE_NAME_LENGTH = 32;
    private static final int APP_MODE_PHONE_LENGTH = 13;
    //private static final int APP_MODE_EMAIL_LENGTH = 32;
    private static final int APP_MODE_NAME_START = APP_MODE_KEY_START + APP_MODE_KEY_LENGTH;
    private static final int APP_MODE_PHONE_START = APP_MODE_NAME_START + APP_MODE_NAME_LENGTH;
    //private static final int APP_MODE_EMAIL_START = APP_MODE_PHONE_START + APP_MODE_PHONE_LENGTH;

    // commands supported by the device
    public static final char HANDSHAKE_REQ = '0'; // automatically send after every connection
    public static final char CONNECTION_MODE_REQ = '9'; // automatically send after every connection
    public static final char CONFIG_TIME_REQ = '6';
    public static final char KEY_REQ = '4';
    public static final char RENAME_DOOR_REQUEST = '2';
    public static final char CHANGE_PASSWORD_REQUEST = '2';
    public static final char LOCK_ACCESS_REQUEST = '3';
    public static final char LOG_REQUEST = '5'; // history file, communicate the guest info
    public static final char SCAN_LOCK_REQUEST = '8'; // scan available devices over internet
    public static final char OWNER_REQUEST = '1'; // time of master config, send master info to device and receive updated info of master
    public static final char TAMPER_REQUEST = '6'; // time of master config, send master info to device and receive updated info of master
    public static final char ROUTER_CONFIG_REQUEST = '7'; // for remote access, store wifi router details
    public static final char ADD_LOCK_REQUEST = 'B'; // add lock to bridge through wifi
    public static final char REFRESH_LOCK_REQUEST = 'D'; // add lock to bridge through wifi
    public static final char DELETE_LOCK_REQUEST = 'C'; // add lock to bridge through wifi
    public static final char BRIDGE_PARAM_REQUEST = 'Y'; //for change the bridge param
    private static final char UNDEFINED_REQ = 'U';
    public static final char FACTORY_RESET_REQ = 'A';
    public static final char AJAR_REQUEST = 'J';
    public static final char BRIDGE_PASSWORD_CHANGE_REQ = 'W';
    public static final char ROUTER_PARAM_REQUEST = 'I';
    public static final char AJAR_STATUS = 1;
    public static final char AJAR_C = 0;
    public static final char AUTOLOCK_C = 1;
    public static final int PACKET_LENGTH_POS = 2;
    public static final char OWNER_NOT_REGISTERED = '0';
    public static final char OWNER_REGISTERED = '1';
    public static final char GUEST_REGISTERED = '3';
    public static final char BATTERY_COUNT_REQUEST = 'F';
    public static final char RF_POWER = 'G';
    public static int TIME=29;
    public static int TIME_STAMP=14;
    public static final String STATUS = "STATUS";

    // Device Info options
    // Received Packet parameters
    public static final int PHONE_MAC_ID_LEN_IN_HEX = 6;

    // command mode
    public static final char APP_MODE_OWNER = 'O';
    public static final char APP_MODE_GUEST = 'G';
    public static final char APP_MODE_VISITOR = 'V';

    // common status for all type of commands
    //private static final char DEV_NOT_RESPONDING = 'D';
    //private static final char DEV_NOT_PRESENT = 'N';
    private static final char CMD_TIMEOUT = 'T';
    private static final char CMD_NOT_SUPPORTED = 'S';
    private static final char CMD_LEN_MISMATCH =	'M';
    private static final char CMD_INVALID_CHKSUM = 'K';
    public static final int CMD_OK = 1;

    // guest registration status
    /*private static final char GUEST_CANT_READ = 'R';
    private static final char GUEST_FIELD_EMPTY = 'E';
    private static final char GUEST_CANT_WRITE =	'W';
    private static final char GUEST_NO_SPACE = 'F';
    private static final char GUEST_KEY_VALID = 'V';
    private static final char GUEST_ALREADY_DELETED = 'K';*/

    // owner status
    /*private static final char MASTER_CANT_READ = 'R';
    private static final char MASTER_FIELD_EMPTY = 'E';
    private static final char MASTER_CANT_WRITE =	'W';*/

    //Device Status Error
    /*public static final char FLASH_CANT_READ = 'F';
    public static final char FLASH_CANT_WRITE = 'W';
    public static final char FLASH_FIELD_EMPTY = 'E';
    public static final char FLASH_NO_SPACE = 'S';
    private static final char TIME_NOT_SET = 'T';
    private static final char ALARM_NOT_SET = 'A';
    private static final char FLASH_READ_ERROR = 'F';
    private static final char FLASH_WRITE_ERROR = 'W';
    private static final char TIME_NOT_READ = 'R';*/

    // device calibration errors

    // device lock/unlock status
    /*private static final char STS_AUTH_FAILED = 'A';
    private static final char STS_AUTH_FAILED_BEFORE_ACCESS_TIME = 'B';
    private static final char STS_KEY_NOT_READ = 'K';

    private static final char STS_AUTH_KEY_MISMATCH = 'M';
    private static final char STS_AUTH_KEY_EXPIRED = 'E';
    private static final char STS_NO_ACCESS = 'X';
    private static final char STS_MAX_ATTEMPT = 'T';
    private static final char STS_TIME_AUTH_FAILED = 'F';
    private static final char INVALID_KEY = 'I';*/

    // diagnosis status
    public static final char STS_FETCH = 'A';
    public static final char STS_END = 'E';

    // diagnosis commands
    public static ArrayList<Guest> selectedGuests;
    public static int selectedGuestsSize;
    public static final String PIN_FILE ="AANCMKFLPENRIJD";
    //public static final String ROUTER_DETAILS_SUGGESTION_FILE ="RAOCMUFTPERIJDSG";
    public static final String LOCK_MAC_LIST_FILE ="LLICOKSHJSSIDFF"; // used to save locks added to bridge
    public static final String SAVE_DATETIME_FILE ="SANCMDDLPEXVIJD";
    public static final String PLAY_SOUND_FILE ="PZLCMAPDFOEPEXVIJD";
    public static final String SHAKE_FILE="SHAKEFILE";
    public static final String BRIDGE_FILE="BRIDGE_FILE";
    public static final String IS_STATIC="IS_STATIC";
    public static final int NEW_PIN_FLAG =101;
    public static final int CHANGE_PIN_FLAG =102;

    //command and response details
    public char requestType = UNDEFINED_REQ;
    public int requestStatus = 0;
    public int requestDirection = 0;
    public String commandDetails = null;
    private String responseDetails = null;

    public static final int TCP_PACKET_UNDEFINED = 10;
    /*private static final int TCP_PACKET_SENT_FAILED = 12;
    private static final int TCP_PACKET_RECEIVED_FAILED = 14;*/

    //public static final int TCP_RECEIVE_PACKET = 17;
    public static final int TCP_SEND_PACKET = 18;
    public static final int TCP_DIRECTION_UNDEFINED = 0;

    //public static Utils LockDemoUtils = new Utils();
    public static final String GUEST_LIST_TYPE_OPTIONS_FILE = "GLTOFABFHDZCSOFH.config";
    public static final String TAMPER_NOTIFICATION_CONFIG_FILE = "SISCFABFH138OFH.config";
    public static final int SHOW_ACTIVE_GUESTS_ONLY = 1;
    public static final int SHOW_EXPIRED_GUESTS_ONLY = 2;
    public static final int SHOW_ALL_GUESTS_EXCEPT_KEY_DELETED = 0;
    public static final int SHOW_ALL_GUESTS = -1;
    public static final int ENABLE_TAMPER_NOTIFICATION = 1;
    public static final int DISABLE_TAMPER_NOTIFICATION = 0;

    //Brocker parameter
    public static final char BROCKER_REQUEST = 'E';
    public static final String SHOW_BRIDGE_DATA = "SHOW_BRIDGE_DATA";
    public static final String ADD_BRIDGE_DATA = "ADD_BRIDGE_DATA";
    public static final String BRIDGE_ID = "BRIDGE_ID";
    public static final String BRIDGE_PASSWORD = "BRIDGE_PASSWORD";
    public static final int BRIDGE_CHECK = 1;

    public static final String BRIDGE_OPERATION = "BRIDGE_OPERATION";

    //private static byte [] utilsTcpPacket;
    private static final Utils LockDemoUtils = new Utils();

    private static String PublishTopic = null;
    private static String SubscribeTopic = null;
    public static String host = "10.10.10.1";
    public static String brokerIp = "13.127.109.11";
    public static String userId = "azBridge";
    public static String password = "AzeN@2018$";

    public static ArrayList<String> accessTypeList = new ArrayList<String>(){
        {
            add("Limited Time");
            add("Full Time");
        }
    };

    /*public static HashMap<Integer, String> errors = new HashMap<Integer, String>(){
        {
            put(DATABASE_ERROR_CODE, "data cannot be inserted due to Database error");
            put(DEVICE_ERROR_CODE, "Operation Failed due to device error");
            put(INVALID_COMMAND, "Invalid Command");
            put(INVALID_ACCESS_MODE, "Invalid access mode");
            put(INVALID_CHECKSUM, "Invalid checksum");
            put(INVALID_LENGTH, "Invalid packet length");
            put(INVALID_OPTION, "Invalid option");
            put(INVALID_PHONE_TIME, "Invalid phone time");
            put(INVALID_ACCESS_TIME, "Invalid access time");
            put(GUEST_MAX_ATTEMPT, "Max Attempt");
            put(GUEST_KEY_NOT_FOUND, "Guest Key not found");
            put(GUEST_AUTH_FAILED, "Guest authentication failed");
            put(OUT_OF_SPACE, "Device is running out of space");
            put(DEVICE_NOT_CALIBRATED, "Device not calibrated");
            put(NOT_HANDSHAKED, "Device not handshaked");
        }
    };*/

    public void setUtilsInfo(Utils u)
    {
        if (u.commandDetails != null)
        {
            LockDemoUtils.commandDetails = u.commandDetails;
        }
        else
        {
            Log.i("Utils", "Lacerta:::Null Command");
        }
        LockDemoUtils.requestType = u.requestType;
        if(u.responseDetails != null)
        {
            LockDemoUtils.responseDetails = u.responseDetails;
        }
        else
        {
            Log.i("Utils", "Lacerta:::Null Response");
        }
        LockDemoUtils.requestStatus = u.requestStatus;
        LockDemoUtils.requestDirection = u.requestDirection;
    }

    /*private Utils getUtilsInfo()
    {
        if (LockDemoUtils.commandDetails == null)
        {
            Log.i("Utils", "Lacerta:::Null Command");
        }
        if(LockDemoUtils.responseDetails == null)
        {
            Log.i("Utils", "Lacerta:::Null Response");
        }
        return LockDemoUtils;
    }*/

    /*private void getChecksum(byte chksum)
    {
        Log.d("Utils", "Checksum "+(byte)pktCheckSum);
    }*/

    /*public byte calculateChecksum(byte [] packet, boolean isCmd)
    {
        int cs = 0;
        int pktLen;
        if(isCmd) {
            pktLen = packet[SEND_PACKET_LENGTH_POS] - 1;
        } else {
            pktLen = packet[RECV_PACKET_LENGTH_POS] - 1;
        }
        //Log.d("Utils", "calculateChecksum Packet length "+pktLen);
        for(int i = 0; i < pktLen; ++ i) {
            cs += (int)packet[i] & 0xFF;
        }
        // if cs > 0xFFFF
        if(cs > 0xFF) {
            int tempcs1 = cs;
            cs = 0;
            for (; tempcs1 != 0; tempcs1 = (tempcs1 >> 8)) {
                cs += (tempcs1 & 0xFF);
            }
        }
        if(cs > 0xFF) {
            int tempcs1 = cs;
            cs = 0;
            for (; tempcs1 != 0; tempcs1 = (tempcs1 >> 8)) {
                cs += (tempcs1 & 0xFF);
            }
        }
        pktCheckSum = cs;
        final int WAIT_TIME = 100;
        new CountDownTimer(WAIT_TIME, 10){
            private int tempcs = pktCheckSum;
            public void onTick(long milliSecond) {
                int tempcs1 = tempcs;
                tempcs = 0;
                for(;tempcs1 != 0; tempcs1 = (tempcs1 >> 8))
                {
                    tempcs += (tempcs1 & 0xFF);
                }
                if(tempcs < 0x100) {
                    Log.d("Utils", ":::processChecksum onTick");
                    getChecksum((byte) tempcs);
                    cancel();
                }
            }

            public void onFinish() {
                Log.d("Utils", ":::processChecksum onFinish");
            }
        }.start();
        return (byte)pktCheckSum;
        Log.d("Utils", "Checksum "+(byte)(~cs));
        return (byte)~cs;
    }

    public boolean isChecksumValid(byte [] packet, boolean isCmd)
    {
        return true;
        int pktLen,cs=0;

        if(isCmd) {
            pktLen = packet[Packet.REQUEST_PACKET_LENGTH_POS] - 1;
        } else {
            pktLen = packet[Packet.RESPONSE_PACKET_LENGTH_POS] - 1;
        }
        if(pktLen <= 0) {
            Log.d("Utils", "isChecksumValid Packet length "+pktLen);
            return false;
        }
        else
        {
            for(int i = 0; i < pktLen; ++ i) {
                cs += (int)packet[i] & 0xFF;
            }
            if(cs > 0xFF) {
                int tempcs1 = cs;
                cs = 0;
                for (; tempcs1 != 0; tempcs1 = (tempcs1 >> 8)) {
                    cs += (tempcs1 & 0xFF);
                }
            }
            if(cs > 0xFF) {
                int tempcs1 = cs;
                cs = 0;
                for (; tempcs1 != 0; tempcs1 = (tempcs1 >> 8)) {
                    cs += (tempcs1 & 0xFF);
                }
            }
            if (packet[pktLen] == (byte) ~ cs)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
    } */

    private byte ch2Byte(char c)
    {
        byte b = 0;
        char[] ref = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        for(int i = 0; i < ref.length; ++ i) {
            if(c == ref[i]) {
                b = (byte)i;
            }
        }
        return b;
    }

    public byte [] getMacIdInHex(String macid)
    {
        byte [] hexBytes = new byte [Utils.PHONE_MAC_ID_LEN_IN_HEX];
        String mac = macid.replaceAll(":", "").toUpperCase();
        for(int i = 0; i < hexBytes.length; ++ i) {
            hexBytes[i] = (byte) (ch2Byte(mac.charAt((i << 1) + 1)) |
                    (ch2Byte(mac.charAt(i << 1)) << 4));
        }
        return hexBytes;
    }

    public static String getStringFromHex(String in)
    {
        String out ;
        char [] ref = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        byte [] strBytes;
        strBytes = in.getBytes(StandardCharsets.ISO_8859_1);
        char [] outCh = new char[strBytes.length << 1];
        for(int i = 0; i < strBytes.length; i ++) {
            outCh[(i << 1)] = ref[(((strBytes[i]&0xff) >> 4) & 0x0F)];
            outCh[(i << 1) + 1] = ref[(strBytes[i] & 0x0F)];
        }
        out = new String(outCh);
        return out;
    }

    /*public String int2Str(int value)
    {
        int temp1 = value;
        int temp3 = 0;
        char[] reference = {'0', '1', '2', '4', '5', '6', '7', '8', '9'};
        char[] output = new char[16];
        char[] output_int = new char[16];

        for (int i = 0; i < 16; ++ i)
        {
            output [i] = '\0';
            output_int [i] = '\0';
        }
        do {
            if (temp1 == 0)
            {
                break;
            }
            output[temp3] = reference[temp1 % 10];
            temp1 /= 10;
            temp3 ++;
        }while(true);

        for (temp3 = 0; (temp3 < 16) && (output [temp3] != '\0'); ++temp3);

        for (int i = 0; i < temp3; ++ i)
        {
            output_int [i] = output [temp3 - i - 1];
        }

        return new String(output_int);
    }*/

   /* public void delay(int ms_count)
    {
        for(int i = 0; i < ms_count; ++ i)
        {
            for (int j = 0; j < 10000; ++ j)
            {
                ;
            }
        }
    }*/


	/*public static String getIPFromMac(String MAC) {
		*//*
		 * method modified from:
		 * 
		 * http://www.flattermann.net/2011/02/android-howto-find-the-hardware-mac-address-of-a-remote-host/
		 * 
		 * *//*
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("/proc/net/arp"));
			String line;
			while ((line = br.readLine()) != null) {

				String[] splitted = line.split(" +");
				if (splitted.length >= 4) {
					// Basic sanity check
					String device = splitted[5];
					if (device.matches(".*" +p2pInt+ ".*")){
						String mac = splitted[3];
						if (mac.matches(MAC)) {
							return splitted[0];
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
                if(br !=  null) {
                    br.close();
                }
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
                e.getCause();
                e.printStackTrace();
            }
		}
		return null;
	}*/


	/*public static String getLocalIPAddress() {
		*//*
		 * modified from:
		 * 
		 * http://thinkandroid.wordpress.com/2010/03/27/incorporating-socket-programming-into-your-applications/
		 * 
		 * *//*
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();

					String iface = intf.getName();
					if(iface.matches(".*" +p2pInt+ ".*")){
						if (inetAddress instanceof Inet4Address) { // fix for Galaxy Nexus. IPv4 is easy to use :-)
							return getDottedDecimalIP(inetAddress.getAddress());
						}
					}
				}
			}
		} catch (SocketException ex) {
			Log.e("NetworkAddressFactory", "SocketException " + getLocalIPAddress(), ex);
		} catch (NullPointerException ex) {
			Log.e("NetworkAddressFactory", "NullPointerException " + getLocalIPAddress(), ex);
		}
		return null;
	}*/

	/*private static String getDottedDecimalIP(byte[] ipAddr) {
		*//*
		 * ripped from:
		 * 
		 * http://stackoverflow.com/questions/10053385/how-to-get-each-devices-ip-address-in-wifi-direct-scenario
		 * 
		 * *//*
		String ipAddrStr = "";
		for (int i=0; i<ipAddr.length; i++) {
			if (i > 0) {
				ipAddrStr += ".";
			}
			ipAddrStr += ipAddr[i]&0xFF;
		}
		return ipAddrStr;
	}*/
    /*
    private class UtilsCommand
    {
        private String commandString;
        public UtilsCommand(byte b1, byte b2, byte [] ba3, String sInfo)
        {
            byte [] ba1 = new byte [1];
            byte [] ba2 = new byte [2];
            ba1 [0] = b1;
            String bs1 = new String(ba1);
            if(bs1.length() < COMMAND_TYPE_LENGTH)
            {
                for (int i = bs1.length(); i < COMMAND_TYPE_LENGTH; ++ i)
                {
                    bs1.concat(" ");
                }
            }
            ba2 [0] = b2;
            String s = new String(ba1) + new String(ba2) + new String(ba3) + sInfo;
            byte [] len = new byte [1];
            len [0] = (byte)s.length();
        }
    }

    private class UtilsResponse
    {
        private byte cmdStatus;
        private byte cmdType;
        private byte devStatus;
        private byte len;
        private byte [] resData;
        private String resInfo;
        public UtilsResponse(String res)
        {

        }
    }
    */


    public static class CommunicationError
    {
        public static String commandStatusError(char errorCode)
        {
            String errorMessage = "";
            switch (errorCode)
            {
                case CMD_NOT_SUPPORTED:
                    errorMessage = "Command not Supported";
                    break;
                case CMD_LEN_MISMATCH:
                    errorMessage = "Command Lenth Mismatch";
                    break;
                case CMD_TIMEOUT:
                    errorMessage = "Communication Time out";
                    break;
                case Utils.CMD_INVALID_CHKSUM:
                    errorMessage = "Invalid Checksum";
                    break;
            }
            return errorMessage;
        }

    }

    public static String getModifiedMac(String mac)
    {
        StringBuilder temp= new StringBuilder();
        String[] s = mac.split(":");
        for(String str : s)
            temp.append(str);
        return temp.toString();
    }
    public static String generateMac(String mac)
    {
        StringBuilder x= new StringBuilder();
        int j=0;

        for(int i=0;i<mac.length();i++)
        {
            x.append(mac.charAt(i));
            j++;
            if(j==2 && i != mac.length()-1)
            {
                x.append(":");
                j=0;
            }
        }
        return x.toString();
    }
    public static int parseInt(String bytes, int index)
    {
        return (bytes.charAt(index) & 0xFF);
    }

    public static byte[] toUnsignedBytes(String bytes)
    {
        byte[] b=new byte[bytes.length()];
        for(int i=0;i<bytes.length();i++)
        {   //Log.e("Utils", "byte to int:"+parseInt(bytes, i));
            b[i]=(byte) parseInt(bytes, i);
        }
        return b;
    }

    /*public static boolean isValidEmailAddress(String email) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "^[_A-Za-z0-9]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }*/

    public static DisplayMetrics getDisplayMetrics(Activity activity){
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm;
    }

    /*public static boolean forget(Activity activity)
    {
        WifiManager wifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        boolean isForget = false;
        boolean isSaved = false;
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                int networkId = wifiInfo.getNetworkId();
                isForget = wifiManager.removeNetwork(networkId);
                isSaved = wifiManager.saveConfiguration();
            }
        }
        return isForget && isSaved;
    }*/


    public static boolean isWifiNetworkConnected(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        NetworkInfo netInfo = null;
        if (connectivity != null) {
            netInfo = connectivity.getActiveNetworkInfo();
        }
        if(netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED){
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            DatabaseHandler databaseHandler = new DatabaseHandler(context);
            ArrayList<WifiNetwork> wifiNetworks = databaseHandler.getNetworks();
            for(WifiNetwork wifiNetwork : wifiNetworks) {
                if(wifiInfo.getSSID().equals("\""+wifiNetwork.getSSID()+"\"")) {
                    return true;
                }
            }
        }
        return false;
    }

    /*public static String getUserId()
    {
        String IMEI = AppContext.getContext().getImei();
        return getUserId(IMEI);
    }*/

    public static String getUserId(String imei)
    {
        if(imei.length()<15){
            imei = imei + "0";
        }
        int[] a = new int[imei.length()];
        int p=0;
        StringBuilder key= new StringBuilder();
        //byte[] k=new byte[6];
        for(int i=0;i<a.length;i++){
            a[i]=Character.getNumericValue(imei.charAt(i));
            p+=a[i];
        }
        for(int i=0;i<a.length;i=i+3)
        {
            int sum=a[i]*a[i+1]+a[i+2];
            key.append(Integer.toHexString(sum + p));
        }
        key.append(Integer.toHexString(p));
        return key.toString().toUpperCase();
    }

    public static byte[] toByteArray(String byteArray)
    {
        return new Utils().getMacIdInHex(byteArray);
    }

    public static void printByteArray(byte[] txValue)
    {
        try {
            int i = 0;
            StringBuilder sb = new StringBuilder();
            while(i != txValue.length)
            {
                sb.append(String.format("%02X ", txValue[i]));
                i++;
            }
            String text=sb.toString();
            System.out.println("Byte Array:"+text);
            //text = new String(txValue, "ISO-8859-1");
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }

    /*public static void forget(Activity activity)
    {
        WifiManager wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                //int networkId = wifiInfo.getNetworkId();
                //isForget = wifiManager.removeNetwork(networkId);
                //isSaved = wifiManager.saveConfiguration();
            }
        }
    }*/

    /*public static byte[] toPrimitive(Byte[] byteObjects)
    {
        byte[] bytes = new byte[byteObjects.length];
        int j=0;
        // Unboxing byte values. (Byte[] to byte[])
        for(Byte b: byteObjects)
            bytes[j++] = b;
        return bytes;
    }*/

    public static byte[] getTime(){
        long millis = System.currentTimeMillis();
        byte[] data = new byte[2];
        data[0]=(byte)(millis/10);
        data[1]=(byte)(millis/2);
        return data;
    }

    public static String getPacketData(byte []data){
        for(byte b :data)
            Log.d(TAG, "getPacketData: "+String.format("%02X",b));
        String packet;
        packet = new String(data, StandardCharsets.ISO_8859_1).replaceAll("^\\x00*", "");
        return packet;
    }

    public static byte[] encriptData(byte []data){
        byte[] packet0 = new byte[32];
        byte[] packet = new byte[16];
        byte[] key=AppContext.getAppKey();
        CryptoUtils encode = new CryptoUtils(key);
        for(int i=0;i<=1; i++)
        {
            System.arraycopy(data, i*16, packet,0,16);
            try {
                packet = encode.AESEncode(packet);
                System.arraycopy(packet, 0, packet0,i*16,16);
            }
            catch (Exception e){e.printStackTrace();}
        }
        return packet0;
    }

    public static String getPublishTopic() {
        Log.d(TAG, "getPublishTopic: "+PublishTopic);
        return PublishTopic;
    }

    public static void setPublishTopic(String publishTopic) {
        PublishTopic = publishTopic;
    }

    public static String getSubscribeTopic() {
        Log.d(TAG, "getSubscribeTopic: "+SubscribeTopic);
        return SubscribeTopic;
    }

    public static void setSubscribeTopic(String subscribeTopic) {
        String topic = "_RESPONSE";
        SubscribeTopic = subscribeTopic+ topic;
    }
}

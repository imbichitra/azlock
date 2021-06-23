package com.asiczen.azlibrary;

import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class Utils implements Packet{
    private static final int REQUEST_PACKET_TYPE_POS = 0;
    private static final int REQUEST_ACCESS_MODE_POS = 1;
    private static final int REQUEST_PACKET_LENGTH_POS = 2;
    static final int RESPONSE_PACKET_TYPE_POS = 0;
    static final int RESPONSE_COMMAND_STATUS_POS = 1;
    private static final int MAX_PKT_SIZE =32;
    private  static final char APP_MODE_VISITOR = 'V';
    private static final char HANDSHAKE_REQ = '0';
    private static final int PHONE_MAC_ID_LEN_IN_HEX = 6;
    public static final char LOCK_ACCESS_REQUEST = '3';
    public static final int CMD_OK = 1;
    public static final char LOCKED = 'L';
    public static final char UNLOCKED = 'U';
    private final static int SEND_PACKET_LENGTH_POS = 2;
    public static final char OWNER_REGISTERED = '1';
    public static final char GUEST_REGISTERED = '3';
    public static final char KEY_REQ = '4';
    private static final char APP_MODE_OWNER = 'O';
    private static final char FULL_TIME_ACCESS = 'F';
    private static final char AJAR_REQUEST = 'J';
    static final int RESPONSE_ACTION_STATUS_POS = 2;
    static final char SUCCESS = 'S';
    private static final char OWNER_NOT_REGISTERED = '0';
    private static final int  ownerNotRegistered=0;
    private static final int youAreNotRegistered=1;
    private static final int unSupported=2;
    private static final int inValidDoor=3;
    private static final int TIME=29;
    private static final int TIME_STAMP=14;

    private static final byte[] regPacket1 = {(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
            (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
            (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF};

    private static final String[] message = {"Owner Not Registered", "You are not registered", "Unsupported String Decoding Exception", "Invalid or Null DoorMode"};
    static {
        System.loadLibrary("ndklink");
    }
    public native String getInfo();
    public static String getKey(String IMEI)
    {
        if(IMEI.length()<15){
            IMEI = IMEI + "0";
        }
        int[] a = new int[IMEI.length()];
        int p=0;
        StringBuilder key= new StringBuilder();
        //byte[] k=new byte[6];
        for(int i=0;i<a.length;i++){
            a[i]=Character.getNumericValue(IMEI.charAt(i));
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

    private String getKey(){
        return getInfo();
    }
    public static byte[] getHandshakePacket(String mac){
        byte[] packet0 = new byte[32];
        byte[] packet = new byte[16];
        byte[] data = new byte[MAX_PKT_SIZE];
        data[REQUEST_PACKET_TYPE_POS] = Utils.HANDSHAKE_REQ;
        data[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_VISITOR;
        data[REQUEST_PACKET_LENGTH_POS] = HandshakePacket.SENT_PACKET_LENGTH;

        if(mac != null) {
            byte[] macHexBytes = Utils.toByteArray(mac);
            System.arraycopy(macHexBytes, 0, data, 3, Utils.PHONE_MAC_ID_LEN_IN_HEX);
        }
        data[Utils.TIME_STAMP]=Utils.getTime()[0];
        data[Utils.TIME_STAMP+1]=Utils.getTime()[1];
        data[Utils.TIME]=Utils.getTime()[0];
        data[Utils.TIME+1]=Utils.getTime()[1];

        Utils u=new Utils();
        String KEY=u.getKey();
        byte[] key=hexStringToByteArray(KEY);
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


    private static byte[] toByteArray(String byteArray)
    {
        return new Utils().getMacIdInHex(byteArray);
    }


    private byte [] getMacIdInHex(String macid)
    {
        byte [] hexBytes = new byte [Utils.PHONE_MAC_ID_LEN_IN_HEX];
        String mac = macid.replaceAll(":", "").toUpperCase();
        for(int i = 0; i < hexBytes.length; ++ i) {
            hexBytes[i] = (byte) (ch2Byte(mac.charAt((i << 1) + 1)) |
                    (ch2Byte(mac.charAt(i << 1)) << 4));
        }
        return hexBytes;
    }
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


    private static byte[] hexStringToByteArray(String s) {

        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }
    public static int parseInt(String bytes, int index)
    {
        return (bytes.charAt(index) & 0xFF);
    }
    public static byte[] lockUnlock(String mac,char appMode,char isLocked){
        byte[] packet = new byte[16];
        byte[] packet0 = new byte[32];
        byte[] data = new byte[MAX_PKT_SIZE];
        data[RESPONSE_PACKET_TYPE_POS] = Utils.LOCK_ACCESS_REQUEST;
        data[REQUEST_ACCESS_MODE_POS] = (byte) (appMode);
        data[REQUEST_PACKET_LENGTH_POS] = LockPacket.SENT_PACKET_LENGTH;
        byte[] macInHex = Utils.toByteArray(mac);
        System.arraycopy(macInHex, 0, data, 4, macInHex.length);
        data[LockPacket.DOOR_STATUS_REQUEST_POSITION] = (byte) (isLocked);
        int[] currentDateTime = splitDateTime();
        for(int i = 0; i < currentDateTime.length; i++)
        {
            data[i + LockPacket.CURRENT_DATE_TIME_START] = (byte) currentDateTime[i];
        }
        data[LockPacket.CHECKSUM_SENT] = calculateChecksum(data);
        Utils u=new Utils();
        String KEY=u.getKey();
        byte[] key=hexStringToByteArray(KEY);
        CryptoUtils encode = new CryptoUtils(key);
        for(int i=0;i<=1; i++)
        {
            System.arraycopy(data, i*16, packet,0,16);
            try {
                packet = encode.AESEncode(packet);
                System.arraycopy(packet, 0, packet0,i*16,16);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return packet0;
    }
    private static int[] splitDateTime()
    {
        int[] x;
        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(new Date());
        String time = new SimpleDateFormat("HH:mm", Locale.ENGLISH).format(new Date());
        String[] sDate = date.split("/");
        String[] sTime = time.split(":");
        x=new int[sDate.length+sTime.length+1];
        int i;
        for(i = 0; i < sDate.length; i++) {
            if(i != sDate.length - 1) {
                x[i] = Integer.parseInt(sDate[i]);
            } else {
                x[i] = Integer.parseInt(sDate[i])/100;
                x[++i] = Integer.parseInt(sDate[i-1])%100;
            }
        }
        for(int j = i, k = 0; k < sTime.length; j++, k++)
            x[j] = Integer.parseInt(sTime[k]);
        return x;
    }
    private static byte calculateChecksum(byte[] packet)
    {
        int cs = 0;
        int pktLen;
        pktLen = packet[SEND_PACKET_LENGTH_POS] - 1;
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
        Log.d("Utils", "Checksum "+(byte)(~cs));
        return (byte)~cs;
    }

    public static byte[] addGuest(String name,String Imei) throws MyException {

        byte[] packet = new byte[16];
        byte[] packet0 = new byte[32];
        regPacket1[REQUEST_PACKET_TYPE_POS] = Utils.KEY_REQ;
        regPacket1[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
        regPacket1[REQUEST_PACKET_LENGTH_POS] = RegisterGuestPacket.SENT_PACKET_LENGTH_ADD_GUEST_2;
        try{
            for (int i = 0; i < name.length(); i++) {
                regPacket1[i + RegisterGuestPacket.GUEST_NAME_START] = (byte) name.charAt(i);
                if(i == name.length() - 1){
                    regPacket1[i+1] += '\0';
                }
            }
        }catch (Exception e){
            throw new MyException(e.toString());
        }

        regPacket1[RegisterGuestPacket.ACCESS_TYPE_POSITION] = (byte) FULL_TIME_ACCESS;
        try {

            if(Imei != null) {
                byte[] idHex = Utils.toByteArray(Imei);
                System.arraycopy(idHex, 0, regPacket1, RegisterGuestPacket.PHONE_MAC_ID_START,PHONE_MAC_ID_LEN_IN_HEX);
            }

        } catch (Exception e) {
            throw new MyException("Enter a valid Imei Number");
        }

        Utils u=new Utils();
        String KEY=u.getKey();
        byte[] key=hexStringToByteArray(KEY);
        CryptoUtils encode = new CryptoUtils(key);
        for(int i=0;i<=1; i++)
        {
            System.arraycopy(regPacket1, i*16, packet,0,16);
            try {
                packet = encode.AESEncode(packet);
                System.arraycopy(packet, 0, packet0,i*16,16);
            } catch (Exception e) {
                throw new MyException("Unsupported String Encoding Exception");
            }
        }
        return packet0;
    }
    static class MyException extends Exception
    {
        MyException(String s)
        {
            // Call constructor of parent Exception
            super(s);
        }
    }

    public static String getErrorCode(byte[] packet){


        String text;
        try {
            text=new String(packet, StandardCharsets.ISO_8859_1);
            char ch=text.charAt(0);
            if(ch=='0') {
                //handshake packet error code
                char registrationStatus = text.charAt(HandshakePacket.REGISTRATION_STATUS_POS);
                AppMode appMode = (registrationStatus == Utils.OWNER_REGISTERED ||
                        registrationStatus == Utils.OWNER_NOT_REGISTERED) ? AppMode.OWNER : AppMode.GUEST;
                boolean isGuestRegistered = (registrationStatus == Utils.GUEST_REGISTERED);
                boolean isOwnerRegistered = (registrationStatus == Utils.OWNER_REGISTERED);

                if (appMode == AppMode.OWNER) {
                    if (!isOwnerRegistered) {
                        return message[ownerNotRegistered];
                    }
                } else {
                    if (!isGuestRegistered) {
                        return message[youAreNotRegistered];
                    }
                }
            }

            //lock or unlock error code
            if(text.length() >= Packet.LockPacket.RECEIVED_PACKET_LENGTH) {
                if(text.charAt(Utils.RESPONSE_PACKET_TYPE_POS) == Utils.LOCK_ACCESS_REQUEST
                        && Utils.parseInt(text,Utils.RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {

                    if(text.charAt(Packet.LockPacket.LOCK_STATUS_POS) == Utils.LOCKED)
                    {
                        //locked
                    }
                    else if(text.charAt(Packet.LockPacket.LOCK_STATUS_POS) == Utils.UNLOCKED)
                    {
                        //unlocked
                    }
                }
                else
                {
                    return CommunicationError.getMessage(Utils.parseInt(text, RESPONSE_COMMAND_STATUS_POS));
                }
            }else{
                return message[inValidDoor];
            }
        }catch (Exception e){
            return message[unSupported];
        }
        return message[unSupported];
    }

    public static int[] getAjarStatus(byte[] packet){
        int[] res = new int[3];
        String text= new String(packet, StandardCharsets.ISO_8859_1);
        int ajarStatue =  Utils.parseInt(text, AjarPacket.AJAR_STATUS);
        int autolockStatus =  Utils.parseInt(text, AjarPacket.AUTOLOCK_STATU);
        int autoLockTime =  Utils.parseInt(text, AjarPacket.AUTOLOCK_TIME);
        res[0]=  ajarStatue;
        res[1]=  autolockStatus;
        res[2]=  autoLockTime;
        return res;
    }

    public static byte[] getAjarPacket(int ajarStatus,int autolocStatus,int time)throws MyException{
        if(time>10 || time < 4)
            throw new MyException("Autolock time should be in between 4 to 10 seconds");
        if(ajarStatus == 0 && autolocStatus == 1){
            throw new MyException("When Ajar is zero Autolock should be zero.Please set autolocStatus to zero");
        }
        byte[] packet0 = new byte[32];
        byte[] data = new byte[MAX_PKT_SIZE];
        byte[] packet = new byte[16];
        data[REQUEST_PACKET_TYPE_POS] = Utils.AJAR_REQUEST;
        data[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_OWNER;
        data[REQUEST_PACKET_LENGTH_POS] = AjarPacket.SENT_PACKET_LENGTH;
        data[AjarPacket.AJAR_STATUS_POSITION] = (byte) ajarStatus;
        data[AjarPacket.AUTOLOCK_STATUS_POSITION] = (byte) autolocStatus;
        data[AjarPacket.AUTOLOCK_TIME_POSITION] = (byte) time;


        Utils u=new Utils();
        String KEY=u.getKey();
        byte[] key=hexStringToByteArray(KEY);
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

    public static String getAjarResponse(byte[] packet){
        String[] msg = {"success", "failure"};
        int i=1;
        String text= new String(packet, StandardCharsets.ISO_8859_1);
        if(text.charAt(AjarPacket.STATUS) == AjarPacket.SUCCESS){
            i=0;
        }
        return msg[i];
    }

    public static int getBatteryStatus(byte[] packet){
        String data;
        data=new String(packet, StandardCharsets.ISO_8859_1);
        return Utils.parseInt(data, HandshakePacket.BATTERY_STATUS_POS);
    }

    private static byte[] getTime(){
        long millis = System.currentTimeMillis();
        byte[] data = new byte[2];
        data[0]=(byte)(millis/10);
        data[1]=(byte)(millis/2);
        return data;
    }

    public static byte[] getBatterryPacket(){
        byte[] packet0 = new byte[32];
        byte[] data = new byte[MAX_PKT_SIZE];
        byte[] packet = new byte[16];

        data[0]=BatteryPacket.REQUEST_TYPE;
        data[1]=BatteryPacket.MODE;
        data[2]=BatteryPacket.SENT_PACKET_LENGTH;
        data[3]=BatteryPacket.BATTERY;

        Utils u=new Utils();
        String KEY=u.getKey();
        byte[] key=hexStringToByteArray(KEY);
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

    public static int getPeriodicBatteryStatus(byte[] packet){
        String data;
        data=new String(packet, StandardCharsets.ISO_8859_1);
        return Utils.parseInt(data, BatteryPacket.BATTERY_STATUS_POS);
    }
}

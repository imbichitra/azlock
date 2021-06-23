package com.asiczen.azlibrary;

import android.annotation.SuppressLint;
import android.os.Build;
import android.telephony.TelephonyManager;

import java.nio.charset.StandardCharsets;

class AzLibrary {
    @SuppressLint({"MissingPermission", "HardwareIds"})
    public static String getImei(TelephonyManager telephonyManager){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Utils.getKey(telephonyManager.getImei());
        }
        else
        {
            return Utils.getKey(telephonyManager.getDeviceId());
        }
    }

    public static byte[] getHandshakePacket(String mac){
        return Utils.getHandshakePacket(mac);
    }

    public static String getHandshakeResponsePacket(byte[] res){
        String text;
        String[] message = {"success", "failure"};
        int i=1;
        char registrationStatus;
        try {
            text= new String(res, StandardCharsets.ISO_8859_1);
            registrationStatus = text.charAt(Packet.HandshakePacket.REGISTRATION_STATUS_POS);
            if (registrationStatus == Utils.OWNER_REGISTERED) {
                i=0 ;
            } else if (registrationStatus == Utils.GUEST_REGISTERED) {
                i=0;
            } else {
                i=1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return message[i];
        }
        return message[i];
    }

    public static byte[] lock(String mac,char appMode) throws Utils.MyException {
        try{
            return Utils.lockUnlock(mac,appMode,'L');
        }
        catch (Exception e){
            throw new Utils.MyException(e.toString());
        }
    }

    public static byte[] unlock(String mac,char appMode) throws Utils.MyException {
        try{
            return Utils.lockUnlock(mac,appMode,'U');
        }catch (Exception e){
            throw new Utils.MyException(e.toString());
        }
    }

    public static String getlockUnlockResponePacket(byte[] res){
        String packet;
        String[] message = {"locked", "unlocked", "failure"};
        int i=2;

        try {
            packet= new String(res, StandardCharsets.ISO_8859_1);
            if(packet.length() >= Packet.LockPacket.RECEIVED_PACKET_LENGTH) {
                if(packet.charAt(Utils.RESPONSE_PACKET_TYPE_POS) == Utils.LOCK_ACCESS_REQUEST
                        && Utils.parseInt(packet,Utils.RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {

                    if(packet.charAt(Packet.LockPacket.LOCK_STATUS_POS) == Utils.LOCKED)
                    {
                        i=0;
                    }
                    else if(packet.charAt(Packet.LockPacket.LOCK_STATUS_POS) == Utils.UNLOCKED)
                    {
                        i=1;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return message[i];
        }
        return message[i];
    }

    public static byte[] addGuest(String name,String Imei) throws Utils.MyException {
        return Utils.addGuest(name,Imei);
    }

    public static String guestResponsePacket(byte[] packet){
        String text;
        String[] message = {"success", "failure"};
        int i=1;
        try {
            text=new String(packet, StandardCharsets.ISO_8859_1);
            if(text.charAt(Utils.RESPONSE_PACKET_TYPE_POS) == Utils.KEY_REQ
                    && Utils.parseInt(text,Utils.RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                if(text.charAt(Utils.RESPONSE_ACTION_STATUS_POS) == Utils.SUCCESS){
                    i=0;
                }
            }

        }catch (Exception e){
            return message[i];
        }
        return message[i];
    }

    public static String getErrorCode(byte[] packet){
        return Utils.getErrorCode(packet);
    }

    public static int[] getAjarStatus(byte[] packet){
        return Utils.getAjarStatus(packet);
    }

    public static byte[] getAjarPacket(int ajarStatus,int autolocStatus,int time)throws Utils.MyException{
        return Utils.getAjarPacket(ajarStatus,autolocStatus,time);
    }

    public static String getAjarResponse(byte[] packet){
        return Utils.getAjarResponse(packet);
    }

    public static int getBatteryStatus(byte[] packet){
        return Utils.getBatteryStatus(packet);
    }

    public static byte[] getBatterryPacket(){
        return Utils.getBatterryPacket();
    }

    public static int getPeriodicBatteryStatus(byte[] packet){
        return Utils.getPeriodicBatteryStatus(packet);
    }
}

package com.asiczen.azlock.content;

import android.content.Context;

import android.util.Log;

import com.asiczen.azlock.app.model.BridgeDetail;
import com.asiczen.azlock.net.MqttDataSendListener;
import com.asiczen.azlock.net.OnDataSendListener;
import com.asiczen.azlock.app.AppMode;
import com.asiczen.azlock.app.ConnectionMode;
import com.asiczen.azlock.app.DeviceStatus;
import com.asiczen.azlock.app.LockStatus;
import com.asiczen.azlock.app.Notification;
import com.asiczen.azlock.app.model.Door;
import com.asiczen.azlock.app.model.RouterInfo;
import com.asiczen.azlock.app.model.User;
import com.asiczen.azlock.util.DateTimeFormat;
import com.asiczen.azlock.util.FileAccess;
import com.asiczen.azlock.util.Utils;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Class to global information about this application environment. It
 * allows access to application-specific resources and classes, as well as
 * up-calls for application-level operations such as connection information,
 * connected door, application mode (owner/guest) and lock status, etc.
 */
public class AppContext {

    private static final AppContext ourInstance = new AppContext();

    private static final String TAG=AppContext.class.getSimpleName();
    private boolean isConnected;
    private ConnectionMode connectionMode;
    private Door door;
    private User user;
    private AppMode appMode;
    private String imei;
    private DeviceStatus deviceStatus;
    private RouterInfo routerInfo;
    private LockStatus lockStatus;
    private OnDataSendListener onDataSendListener;
    private MqttDataSendListener mqttSendListener;
    private boolean isTamperNotificationEnabled;
    private boolean shouldAskPin;
    private boolean shouldConfigPin;
    private String pin;
    private BridgeDetail bridgeDetail;
    //private final int ASK_PIN_DISABLE=0;
    //private Date savedDateTime;
    private boolean playSound;
    private static String key;
    private static String UserId;
    private static String Password;
    private static String ip_address;
    private static String addLock_url;
    private static String change_password_url;
    private static String writepin_url;
    private static String forgot_password_url;
    private static String readpin_url;
    private static String create_url;
    private static String send_mail_url;
    private static String dob_verification_url;
    private static String secret_Q1_verification_url;
    private static String secret_Q2_verification_url;
    private static String secret_ans_url;
    private static String post_to_customer_info_url;
    private static String login_url;
    private static String adminlogin;
    private static String raise_issue_url;
    private String onoff=null;
    private String ajar=null;

    private int ajarStatus;
    private int autolockStatus;
    private int autolockTime;

    public static String getClientId(Context context){
        MySharedPreferences sharedPreferences = new MySharedPreferences(context);
        Date currentTime = Calendar.getInstance().getTime();
        Calendar mcalendar = Calendar.getInstance();
        mcalendar.setTime(currentTime);
        return sharedPreferences.getMac()/*+mcalendar.getTimeInMillis()*/;
    }
    public BridgeDetail getBridgeDetail() {
        return bridgeDetail;
    }

    public void setBridgeDetail(BridgeDetail bridgeDetail) {
        this.bridgeDetail = bridgeDetail;
    }
    public int getAjarStatus() {
        return ajarStatus;
    }

    public void setAjarStatus(int ajarStatus) {
        this.ajarStatus = ajarStatus;
    }

    public int getAutolockStatus() {
        return autolockStatus;
    }

    public void setAutolockStatus(int autolockStatus) {
        this.autolockStatus = autolockStatus;
    }

    public int getAutolockTime() {
        return autolockTime;
    }

    public void setAutolockTime(int autolockTime) {
        this.autolockTime = autolockTime;
    }

    /**
     * Get the application context
     * @return application context
     */
    public static AppContext getContext() {
        return ourInstance;
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
    /**
     * cannot accessible to other classes
     */
    public void setTag(String s){
        onoff=s;
    }
    public String getTag(){
        return onoff;
    }
    public void setAjarTag(String s){
        ajar=s;
    }
    public String getAjarTag(){
        return ajar;
    }
    private AppContext() {
        door=null;
    }

    /**
     * Return the details of connected door.
     *
     * @return The details of connected door.
     *
     * @see #setDoor(Door)
     */
    public Door getDoor() {
        return door;
    }

    /**
     * Modify the details of connected door.
     * The default door is null.
     */
    public void setDoor(Door door) {
        this.door = door;
    }

    /**
     * Return the details of connected user.
     *
     * @return The details of connected user.
     *
     * @see #setUser(User)
     */
    public User getUser() {
        return user;
    }

    /**
     * Modify the details of connected door.
     * The default door is null.
     *
     * param user
     *
     * For more details check {Link User}
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Return current application mode. It defines the
     * connected user is {Link AppMode.GUEST} or {Link AppMode.OWNER}
     * For more details check {Link AppMode}
     *
     * return The application mode.
     *
     * see #setAppMode(AppMode)
     */
    public AppMode getAppMode() {
        return appMode;
    }

    /**
     * Modify current application mode.  The default priority is {Link AppMode.GUEST}.
     * Check {link AppMode} for different values can be set.
     *
     * param appMode current application mode.
     *
     * @see #getAppMode()
     */
    public void setAppMode(AppMode appMode) {
        this.appMode = appMode;
    }

    /**
     * Get connection status.
     *
     * @return True if app is connected with the lock, False otherwise.
     *
     * @see #setConnectionStatus(boolean)
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Modify current application mode.
     *
     * @param connected current connection status.
     *
     * @see #isConnected()
     */
    public void setConnectionStatus(boolean connected) {
        isConnected = connected;
    }

    /**
     * Return current connection mode. It defines the device
     * connected over BLE or Remote.
     * For more details check {Link ConnectionMode}
     *
     * @return The connection mode.
     *
     * @see #setConnectionMode(ConnectionMode)
     */
    public ConnectionMode getConnectionMode() {
        return connectionMode;
    }

    /**
     * Modify connection mode to {Link ConnectionMode.CONNECTION_MODE_REMOTE}
     * or {Link ConnectionMode.CONNECTION_MODE_BLE}
     * Check {link ConnectionMode}.
     *
     * param connectionMode
     *
     * @see #getConnectionMode()
     */
    public void setConnectionMode(ConnectionMode connectionMode) {
        this.connectionMode = connectionMode;
    }

    /**
     * Return IMEI of the phone. IMEI (International Mobile Equipment Identity)
     * is a unique 15-digit serial number given to every mobile phone which can
     * then be used to check information such as the phone's Country of Origin,
     * the Manufacturer and it's Model Number.
     *
     * @return String IMEI number.
     */
    public String getImei() {
        return imei;
    }

    /**
     * Store IMEI number for current device.
     *
     * param imei
     *
     * @see #getImei()
     */
    /*public void setImei(String imei) {
        this.imei = imei;
    }*/

    /**
     * Return device status after connection established. It defines the
     * connected device has successfully authenticated or not.
     * For more details check {Link DeviceStatus}
     *
     * return The device status.
     *
     * see #setDeviceStatus(DeviceStatus)
     */
    public DeviceStatus getDeviceStatus() {
        return deviceStatus;
    }

    /**
     * Modify device status to {Link DeviceStatus.DEVICE_HANDSHAKED} if
     * authentication successful, {Link DeviceStatus.NO_DEVICE}
     * Default value is {Link DeviceStatus.NO_DEVICE}
     * Check {link DeviceStatus} for more details.
     *
     * param deviceStatus
     *
     * see #getDeviceStatus()
     */
    public void setDeviceStatus(DeviceStatus deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public RouterInfo getRouterInfo() {
        return routerInfo;
    }

    /*public void setRouterInfo(RouterInfo routerInfo) {
        this.routerInfo = routerInfo;
    }*/

    public LockStatus getLockStatus() {
        return lockStatus;
    }

    public void setLockStatus(LockStatus lockStatus) {
        this.lockStatus = lockStatus;
    }

    public OnDataSendListener getOnDataSendListener() {
        return onDataSendListener;
    }

    public void setOnDataSendListener(OnDataSendListener onDataSendListener) {
        this.onDataSendListener = onDataSendListener;
    }

    public MqttDataSendListener getMqttSendListener() {
        return mqttSendListener;
    }

    public void setMqttSendListener(MqttDataSendListener mqttSendListener) {
        this.mqttSendListener = mqttSendListener;
    }

    public String getPin() {
        return pin;
    }

    private void setPin(String pin) {
        this.pin = pin;
    }

    public void savePin(Context context, String pin, boolean isChecked){
        FileAccess fileAccess=new FileAccess(context, Utils.PIN_FILE);
        String askFlag=isChecked ? "1:" : "0:";
        fileAccess.write(askFlag+pin);
        checkPinStatus(context);
    }

    public void updateAskPinStatus(Context context, boolean shouldAsk){
        checkPinStatus(context);
        Log.d(TAG, "updateAskPinStatus:"+shouldAsk+":"+getPin());
        savePin(context, getPin(), shouldAsk);
    }

    public void setPlaySoundOnLockUnlock(Context context, boolean playSound){
        FileAccess fileAccess=new FileAccess(context, Utils.PLAY_SOUND_FILE);
        String playFlag=playSound ? "1" : "0";
        fileAccess.write(playFlag);
        checkPlaySoundOnLockUnlock(context);
    }

    public void checkPlaySoundOnLockUnlock(Context context){
        FileAccess fileAccess=new FileAccess(context, Utils.PLAY_SOUND_FILE);
        String play=fileAccess.read();
        if(fileAccess.FILE_NOT_FOUND){
            setPlaySoundOnLockUnlock(context, true);
            playSound=true;
        }
        else {
            playSound = play != null && play.equals("1");
        }
    Log.d(TAG, "checkPlaySoundOnLockUnlock/playSound:"+playSound);    }
    public void setShakeToFile(Context context,String data){
        Log.d(TAG,"on/of shake write data="+data);
        FileAccess fileAccess=new FileAccess(context,Utils.SHAKE_FILE);
        fileAccess.write(data);

    }
    public boolean shouldPlaySound(){
        return playSound;
    }
    public  boolean shakeonOff(Context context){
        FileAccess fileAccess=new FileAccess(context,Utils.SHAKE_FILE);
        String ss=fileAccess.read();
        //Log.d(TAG,"on/of shake read data="+ss);
        Log.d(TAG,"FILE_NOT_FOUND="+fileAccess.FILE_NOT_FOUND);
        if(!fileAccess.FILE_NOT_FOUND){
            return ss.equals("1");
        }else {
            return false;
        }
    }

    public void updateNotificationStatus(Notification notification, Context context){
        if (notification == Notification.TAMPER) {
            FileAccess fileAccess = new FileAccess(context, Utils.TAMPER_NOTIFICATION_CONFIG_FILE);
            String tamperNotificationFlag = fileAccess.read();
            if (tamperNotificationFlag == null || fileAccess.FILE_NOT_FOUND) {
                isTamperNotificationEnabled = false;
            } else if (!tamperNotificationFlag.isEmpty()) {
                isTamperNotificationEnabled = Integer.parseInt(tamperNotificationFlag) == Utils.ENABLE_TAMPER_NOTIFICATION;
            }
        }
    }

    public boolean getNotificationStatus(Notification notification){
        if (notification == Notification.TAMPER) {
            return isTamperNotificationEnabled;
        }
        return false;
    }

    public boolean shouldAskPin(){
        return shouldAskPin;
    }

    public boolean shouldConfigPin(){
        return shouldConfigPin;
    }

    public void checkPinStatus(Context context){
        FileAccess fileAccess=new FileAccess(context, Utils.PIN_FILE);
        String askPinFlag = fileAccess.read();
        if(askPinFlag == null || fileAccess.FILE_NOT_FOUND || askPinFlag.isEmpty())
        {
            shouldConfigPin=true;
        }
        else {
            Log.d(TAG, "checkPinStatus/Pin:"+askPinFlag);
            String[] val = askPinFlag.split(":");
            shouldConfigPin=false;
            int ASK_PIN_ENABLE = 1;
            shouldAskPin = (Integer.parseInt(val[0]) == ASK_PIN_ENABLE);
            setPin(val[1]);
        }
Log.d(TAG, "shouldAskPin:"+shouldAskPin+"\nshouldConfigPin:"+shouldConfigPin);    }

    public Date getSavedDateTime(Context context)
    {
        FileAccess fileAccess=new FileAccess(context, Utils.SAVE_DATETIME_FILE);
        String savedTime=fileAccess.read();
        if(savedTime==null || fileAccess.FILE_NOT_FOUND || savedTime.isEmpty())
        {
            return null;
        }
        return DateTimeFormat.toDate(savedTime, new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH));
    }

    public void saveDateTime(Context context)
    {
        FileAccess fileAccess=new FileAccess(context, Utils.SAVE_DATETIME_FILE);
        fileAccess.write(DateTimeFormat.toString(new Date(), new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH)));
    }

    public void clearSavedDateTime(Context context){
        FileAccess fileAccess=new FileAccess(context, Utils.SAVE_DATETIME_FILE);
        fileAccess.write("");
    }
    /*public void savePinToFile(String pin){
       // String state;
       // state= Environment.getExternalStorageState();

            File Root= Environment.getExternalStorageDirectory();
            Log.d(TAG,"path="+Root);
            File Dir=new File("/data/Azlock");
            if(!Dir.exists()){
                Dir.mkdir();
            }
            File file=new File(Dir,"MyMessage.txt");
            try{
                FileOutputStream fileOutputStream=new FileOutputStream(file,false);
                fileOutputStream.write(pin.getBytes());
                fileOutputStream.close();
            }catch (FileNotFoundException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }


    }*/

    public String readPinFromFile(){
        //File Root= Environment.getExternalStorageDirectory();
        File Dir=new File("/data/Azlock");
        File file=new File(Dir,"MyMessage.txt");
        String pin="";
        // StringBuffer sb=new StringBuffer();

        try {
            FileInputStream fileInputStream=new FileInputStream(file);
            //InputStreamReader inputStreamReader=new InputStreamReader(fileInputStream);
            //BufferedReader bufferedReader=new BufferedReader(inputStreamReader);
            // StringBuffer stringBuffer=new StringBuffer();
            pin= "";
            /*if((pin=bufferedReader.readLine())!=null){

            }*/

        }
        catch (IOException e){
            e.printStackTrace();
        }
        return pin;
    }

    public void setData(String[] data){
        for(int i=0;i<data.length;i++){
            switch (i){
                case 0:
                    //private boolean isShakeOn;
                    //private String filename = "MyMessage.txt";
                    //private String filepath = "com.asiczen.azlock";
                    //static File myExternalFile;
                    //String app_key = data[i];
                    break;
                case 1:
                    key=data[i];
                    break;
                case 2:
                    UserId=data[i];
                    break;
                case 3:
                    Password=data[i];
                    break;
                case 4:
                    ip_address=data[i];
                    break;
                case 5:
                    addLock_url=data[i];
                    break;
                case 6:
                    change_password_url=data[i];
                    break;
                case 7:
                    writepin_url=data[i];
                    break;
                case 8:
                    forgot_password_url=data[i];
                    break;
                case 9:
                    readpin_url=data[i];
                    break;
                case 10:
                    create_url=data[i];
                    break;
                case 11:
                    send_mail_url=data[i];
                    break;
                case 12:
                    dob_verification_url=data[i];
                    break;
                case 13:
                    secret_Q1_verification_url=data[i];
                    break;
                case 14:
                    secret_Q2_verification_url=data[i];
                    break;
                case 15:
                    secret_ans_url=data[i];
                    break;
                case 16:
                    post_to_customer_info_url=data[i];
                    break;
                case 17:
                    login_url=data[i];
                    break;
                case 18:
                    adminlogin=data[i];
                    break;
                case 19:
                    raise_issue_url=data[i];
                    break;
            }
        }
    }
    public static byte[] getAppKey(){
        String s="2B7E151628AED2A6ABF7158809CF4F3C";
       // return hexStringToByteArray(App_key);
        return hexStringToByteArray(s);
    }
    public static byte[] getKey(){
        return hexStringToByteArray(key);
    }
    public static String getUserId(){
      return unHex(UserId);
    }
    public static String getPassword(){
        return unHex(Password);
    }
    public static String getIp_address(){
        return unHex(ip_address);
    }
    public static String getAddLock_url(){
        return unHex(addLock_url);
    }
    public static String getChange_password_url(){
        return unHex(change_password_url);
    }
    public static String getWritepin_url(){
        return unHex(writepin_url);
    }
    public static String getForgot_password_url(){
        return unHex(forgot_password_url);
    }
    public static String getReadpin_url(){
        return unHex(readpin_url);
    }
    public static String getCreate_url(){
        return unHex(create_url);
    }
    public static String getSend_mail_url(){
        return unHex(send_mail_url);
    }
    public static String getDob_verification_url(){
        return unHex(dob_verification_url);
    }
    public static String getSecret_Q1_verification_url(){
        return unHex(secret_Q1_verification_url);
    }
    public static String getSecret_Q2_verification_url(){
        return unHex(secret_Q2_verification_url);
    }
    public static String getSecret_ans_url(){
        return unHex(secret_ans_url);
    }
    public static String getPost_to_customer_info_url(){
        return unHex(post_to_customer_info_url);
    }
    public static String getLogin_url(){
        return unHex(login_url);
    }
    public static String getAdminlogin(){
        return unHex(adminlogin);
    }
    public static String getRaise_issue_url(){
        return unHex(raise_issue_url);
    }

    public void setTemperStatus(boolean isTamperNotificationEnabled) {
        this.isTamperNotificationEnabled = isTamperNotificationEnabled;
    }

    public boolean getTemperStatus(){
        return this.isTamperNotificationEnabled;
    }

    public static class DisplayTableContent implements Serializable {
        int priority;
        String door_name;
        String mac_id;
        DisplayTableContent(int priority,String door_name,String mac_id){
            this.priority=priority;
            this.door_name=door_name;
            this.mac_id=mac_id;
        }

        public String getDootName(){
            return door_name;
        }
        public String getMacId(){
            return mac_id;
        }
    }
    private static String unHex(String arg) {
        StringBuilder str = new StringBuilder();
        for(int i=0;i<arg.length();i+=2)
        {
            String s = arg.substring(i, (i + 2));
            int decimal = Integer.parseInt(s, 16);
            str.append((char) decimal);
        }
        return str.toString();
    }
}

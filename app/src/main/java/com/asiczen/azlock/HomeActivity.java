package com.asiczen.azlock;

import android.app.Activity;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import com.asiczen.azlock.content.MySharedPreferences;
import com.github.clans.fab.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.asiczen.azlock.app.AppMode;
import com.asiczen.azlock.app.CommunicationError;
import com.asiczen.azlock.app.DeviceStatus;
import com.asiczen.azlock.app.LockStatus;
import com.asiczen.azlock.app.model.Guest;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.content.LogoutBroadcastReceiver;
import com.asiczen.azlock.content.SessionManager;
import com.asiczen.azlock.net.MqttDataSendListener;
import com.asiczen.azlock.net.MqttInterface;
import com.asiczen.azlock.net.NetClientAsyncTask;
import com.asiczen.azlock.net.OnDataAvailableListener;
import com.asiczen.azlock.net.OnDataSendListener;
import com.asiczen.azlock.net.OnTaskCompleted;
import com.asiczen.azlock.util.CountDownTimer;
import com.asiczen.azlock.util.DateTimeFormat;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.Utils;
import com.github.clans.fab.FloatingActionMenu;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static com.asiczen.azlock.ConnectActivity.TEMP_FLAG_ENABLE;
import static com.asiczen.azlock.ConnectActivity.UNAUTHORIZED_ACCESS;
import static com.asiczen.azlock.app.ConnectionMode.CONNECTION_MODE_BLE;
import static com.asiczen.azlock.app.ConnectionMode.CONNECTION_MODE_REMOTE;

/**
 * Created by Somnath on 12/15/2016.
 */

public class HomeActivity extends AppCompatActivity implements Packet, OnUpdateListener {
    private Context mContext;
    private AppContext appContext;
    private Vibrator vibrator;
    private TextView doorNameTextView,lockAccessTextView,batteryPercentageTextView;
    private ProgressBar activityProgressBar;
    private ImageView batteryIcon;
    private ImageButton lockImageButton;
    private int batteryStatus;
    private static OnDataSendListener mOnDataSendListener;
    private static final String TAG = HomeActivity.class.getSimpleName();
    private SessionManager sessionManager;
    private IntentFilter intentFilter;
    private LogoutBroadcastReceiver logoutBroadcastReceiver;
    private static final int REQUEST_CODE_GUEST_LIST_ACTIVITY = 77;


    private SensorManager mSensorManager;
    private float aceVal;  //current acceleration value and gravity
    private float aceLast; //last acceleration value and gravity
    private  float shake; //acceleratio value differ from gravity
    private static boolean flag=true;
    private boolean isShakeOn;
    private SharedPreferences sharedpreferences;
    private MqttDataSendListener mqttDataSendListener;
    FloatingActionButton exit_fab,disconnect_fab;
    private MySharedPreferences mySharedPreferences;
	private int isDisconOrUnsub = -1; //it is used for check weather to send disconnect packet to  mqtt or unsubscribe to mqtt.
    public static int actionToPerform = -1;
    private boolean isDoubleBackPressed = false;
    private boolean isMqttDisconnectCall = false;
    private MediaPlayer player;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Activity activity;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);
        mContext = this;
        appContext = AppContext.getContext();
        if(!appContext.isConnected()){
            finish();
        }
        mySharedPreferences = new MySharedPreferences(this);
        if (appContext.getAppMode()== AppMode.GUEST)
        {
            closeContextMenu();
        }

        sharedpreferences = getSharedPreferences(Utils.BRIDGE_FILE, Context.MODE_PRIVATE);

        sessionManager = new SessionManager(this);
        logoutBroadcastReceiver = new LogoutBroadcastReceiver(this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(SessionManager.ACTION_LOGOUT);
        intentFilter.addAction(SessionManager.ACTION_EXIT);
        //activity = getParent();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mOnDataSendListener=appContext.getOnDataSendListener();
        SettingsActivity.setOnUpdateListener(this);
        appContext.checkPlaySoundOnLockUnlock(mContext);

        saveDateTime();

        if(sessionManager.verify()){
            finish();
        }

        batteryStatus = getIntent().getIntExtra(ConnectActivity.BATTERY_STATUS_EXTRA, Integer.MIN_VALUE);
        int tamperStatus = getIntent().getIntExtra(ConnectActivity.TAMPER_STATUS_EXTRA,0);
        doorNameTextView = findViewById(R.id.doorTextView);
        lockAccessTextView = findViewById(R.id.access_status);
        activityProgressBar = findViewById(R.id.progressBar);
        lockImageButton = findViewById(R.id.lockImageButton);
        batteryIcon = findViewById(R.id.battery_imageView);
        batteryPercentageTextView = findViewById(R.id.battery_percent_textView);
        FloatingActionMenu menuFab = findViewById(R.id.menu_fab);
        exit_fab = findViewById(R.id.exit_fab);
        disconnect_fab = findViewById(R.id.disconnect_fab);
        menuFab.setClosedOnTouchOutside(true);
        displayBatteryStatus(batteryStatus);
        Log.d(TAG, "tamperStatus:"+ tamperStatus);
        if(tamperStatus== TEMP_FLAG_ENABLE || tamperStatus == UNAUTHORIZED_ACCESS)
        {
            /*. Please check SMS for the details. is removed because sms service is stoped by google*/
            new AlertDialog.Builder(mContext).setTitle("Unauthorized Access")
                    .setMessage(Html.fromHtml("<font color='#FF0000'>Unregistered user have tried to access the lock</font>"))

                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).create().show();
        }


        isShakeOn=appContext.shakeonOff(this);
        Log.d(TAG,"isShakeOn="+isShakeOn);
        if(isShakeOn) {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (mSensorManager != null) {
                mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
            }
            aceVal = SensorManager.GRAVITY_EARTH;
            aceLast = SensorManager.GRAVITY_EARTH;
            shake = 0.00f;
        }
        mqttDataSendListener = appContext.getMqttSendListener();

        //auto lock on 3d tauch
        LockStatus lockStatus = appContext.getLockStatus();
        Log.d(TAG, "onCreate: "+ConnectActivity.actionToPerform +" "+lockStatus);
        if(actionToPerform!=-1 && lockStatus == LockStatus.LOCKED){
            actionToPerform = -1;
            lockUnlock();
        }
    }
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if(level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE && level <= ComponentCallbacks2.TRIM_MEMORY_COMPLETE){
            Log.d(TAG,"onTrimMemory");
            sessionManager.exit();
        }
    }
    private final SensorEventListener mSensorListener = new SensorEventListener(){

        @Override
        public void onSensorChanged(SensorEvent event) {
            if(isShakeOn) {

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                aceLast = aceVal;
                aceVal = (float) Math.sqrt((double) (x * x + y * y + z * z));
                float delta = aceVal - aceLast;
                shake = shake * 0.9f + delta;
                //Log.d(TAG, "shake value="+shake);
                if (shake > 20 && flag) {
                    //do some thing
                    //handle multiple shake event
                        Log.d(TAG, "shake11");
                        flag = false;
                        lockUnlock();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    public void onClickLockImageButton(View v){
       lockUnlock();
    }
    private void lockUnlock(){
        if(appContext.isConnected()) {
            //new LockAccessAsyncTask(MainActivity.this).execute();

            Date savedTime = appContext.getSavedDateTime(this);
            Date currentTime = new Date();
            if (appContext.getAppMode()==AppMode.GUEST && savedTime != null && currentTime.before(savedTime)) {
                Log.e(TAG, "TimeMismatch: Deleting guest "+appContext.getUser().getId());
                new DeleteGuestsAsyncTask(this, this).execute((Guest) appContext.getUser());
                appContext.clearSavedDateTime(mContext);
                final int WAIT_TIME=10000, INTERVAL=2000;
                final CountDownTimer countDownTimer=new CountDownTimer(WAIT_TIME, INTERVAL) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        sessionManager.exit();
                    }
                };
                countDownTimer.start();
                new AlertDialog.Builder(mContext).setCancelable(false)
                        .setTitle("Time Mismatch")
                        .setMessage("Time mismatch detected which leads to delete your key. Contact owner for more information. azLock will automatically exit after 10 seconds.")
                        .setPositiveButton("EXIT", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                countDownTimer.cancel();
                                sessionManager.exit();
                            }
                        }).create().show();
            }
            else {
                activityProgressBar.setVisibility(View.VISIBLE);
                lockAccessTextView.setVisibility(View.VISIBLE);
                lockAccessTextView.setText(R.string.accessing_lock);
                lockImageButton.setClickable(false);
                doLockAccess();
            }
        }
        else{
            Toast.makeText(mContext, "Device not connected", Toast.LENGTH_LONG).show();
        }
    }
    public void onExitFabClick(View v){
        if(appContext.getConnectionMode()==CONNECTION_MODE_REMOTE) {
            /*new RemoteDisconnectAsyncTask(HomeActivity.this, new OnTaskCompleted<Boolean>() {
                @Override
                public void onTaskCompleted(int resultCode, Boolean data) {
                    saveDateTime();
                    sessionManager.exit();
                }
            }, "Disconnecting...").execute(appContext.getDoor().getId());*/
            disconnectFromMqtt("exit");
        }
        else {
            saveDateTime();
            sessionManager.exit();
        }
    }

    public void onDisconnectFabClick(View v){
        appContext.setConnectionStatus(false);
        if(appContext.getConnectionMode()==CONNECTION_MODE_BLE) {
            sessionManager.logout();
        }
        else if(appContext.getConnectionMode()==CONNECTION_MODE_REMOTE)
        {
            /*new RemoteDisconnectAsyncTask(HomeActivity.this, new OnTaskCompleted<Boolean>() {
                @Override
                public void onTaskCompleted(int resultCode, Boolean data) {
                    sessionManager.logout();
                }
            }, "Disconnecting...").execute(appContext.getDoor().getId());*/
            disconnectFromMqtt("disconnect");
        }
    }

    @Override
    protected void onStart()
    {
        Log.d(TAG, "onStart");
        registerReceiver(logoutBroadcastReceiver, intentFilter);
        super.onStart();
    }

    @Override
    protected void onDestroy()
    {
        if(logoutBroadcastReceiver!=null) {
            unregisterReceiver(logoutBroadcastReceiver);
        }
        super.onDestroy();
    }

    @Override
    protected void onStop()
    {
        if(isShakeOn)
            mSensorManager.unregisterListener(mSensorListener);
        Log.d(TAG, "onStop");
        saveDateTime();
        stopPlayer();
        super.onStop();
    }
   
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if(sessionManager.verify() || appContext.getDeviceStatus() != DeviceStatus.DEVICE_HANDSHAKED){
            finish();
        }
        onUpdate(OnUpdateListener.LOCK_STATUS_UPDATED, null);
    }

    @Override
    public void onBackPressed()
    {
        /*new AlertDialog.Builder(mContext)
                .setTitle(R.string.popup_title)
                .setMessage(R.string.popup_message)
                .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(appContext.getConnectionMode()==CONNECTION_MODE_REMOTE) {
                            //this is required if lock connected to RemoteDisconnectAsyncTask
                           *//* new RemoteDisconnectAsyncTask(HomeActivity.this, new OnTaskCompleted<Boolean>() {
                                @Override
                                public void onTaskCompleted(int resultCode, Boolean data) {
                                    saveDateTime();
                                    sessionManager.logout();

                                }
                            }, "Disconnecting...").execute(appContext.getDoor().getId());*//*

                            disconnectFromMqtt("disconnect");
                        }
                        else{
                            saveDateTime();
                            sessionManager.logout();
                        }
                    }
                })
                .setNegativeButton(R.string.popup_no, null)
                .show();*/

        if(isMqttDisconnectCall){
            return;
        }
        if (isDoubleBackPressed){
            if(appContext.getConnectionMode()==CONNECTION_MODE_REMOTE) {
                //this is required if lock connected to RemoteDisconnectAsyncTask
                           /* new RemoteDisconnectAsyncTask(HomeActivity.this, new OnTaskCompleted<Boolean>() {
                                @Override
                                public void onTaskCompleted(int resultCode, Boolean data) {
                                    saveDateTime();
                                    sessionManager.logout();

                                }
                            }, "Disconnecting...").execute(appContext.getDoor().getId());*/

                disconnectFromMqtt("disconnect");
            }
            else{
                saveDateTime();
                sessionManager.logout();
            }
        }else {
            Toast.makeText(this, "Press again to exit", Toast.LENGTH_LONG).show();
        }
        isDoubleBackPressed = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isDoubleBackPressed = false;
            }
        },2000);
    }

    private void disconnectFromMqtt(final String action){
        isMqttDisconnectCall = true;
        byte[] packet = new byte[RemoteConnectionModePacket.SENT_PACKET_LENGTH];
        packet[REQUEST_PACKET_TYPE_POS] = Utils.CONNECTION_MODE_REQ;
        packet[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_OWNER;
        packet[REQUEST_PACKET_LENGTH_POS] = RemoteConnectionModePacket.SENT_PACKET_LENGTH;
        packet[RemoteConnectionModePacket.CONNECTION_MODE_POSITION] = RemoteConnectionModePacket.DISCONNECT;
        String doorId = appContext.getDoor().getId();
        if (doorId != null) {
            // convert mac in to byte
            byte[] doorMac = Utils.toByteArray(doorId);
            for (byte b:doorMac)
                Log.d(TAG, "sendConnectCommand: "+String.format("%02X",b));
            System.arraycopy(doorMac, 0, packet, RemoteConnectionModePacket.DOOR_MAC_ID_START, Utils.PHONE_MAC_ID_LEN_IN_HEX);
        }

        isDisconOrUnsub = MqttInterface.UN_SUBSCRIBE;
        mqttDataSendListener.sendData(packet, PublishTopic.SUBSCRIBE_TOPIC_NO, "",MqttInterface.DISCONNECT_TIME, new MqttInterface() {
            @Override
            public void dataAvailable(byte[] data) {
                isMqttDisconnectCall = false;
                /*saveDateTime();
                if (action.equals("exit")){
                    mqttDataSendListener.disconnectMqtt();
                    sessionManager.exit();
                }else {
                    mqttDataSendListener.disconnectMqtt();
                    sessionManager.logout();
                }*/
                Log.d(TAG, "dataAvailable: ");
                //isDisconOrUnsub = MqttInterface.DISCONNECT_TO_MQTT;
                //mqttDataSendListener.unSubscribe();
                disConnectToMqtt(action);
            }

            @Override
            public void timeOutError() {
            	isMqttDisconnectCall = false;
                Log.d(TAG, "timeOutError: "+isDisconOrUnsub);
                /*if (isDisconOrUnsub == MqttInterface.UN_SUBSCRIBE) {
                    Log.d(TAG, "timeOutError: UN_SUBSCRIBE");
                    isDisconOrUnsub = MqttInterface.DISCONNECT_TO_MQTT;
                    mqttDataSendListener.unSubscribe();
                }else {
                    Log.d(TAG, "timeOutError: DISCONNECT_TO_MQTT");
                    disConnectToMqtt(action);
                }*/
                disConnectToMqtt(action);
                //Toast.makeText(HomeActivity.this, "Unable to disconnect", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void unableToSubscribe() {

            }

            @Override
            public void succOrFailToUnSubscribe() {
                Log.d(TAG, "succOrFailToUnSubscribe: ");
                //disConnectToMqtt(action);
            }
        });
    }

    private void disConnectToMqtt(String action){
        isDisconOrUnsub = -1;
        saveDateTime();
        if (action.equals("exit")){
            mqttDataSendListener.disconnectMqtt();
            sessionManager.exit();
        }else {
            mqttDataSendListener.disconnectMqtt();
            sessionManager.logout();
        }
    }
    private void saveDateTime(){
        Date savedTime=appContext.getSavedDateTime(mContext);
        if(appContext.getAppMode()== AppMode.GUEST && (savedTime==null || new Date().after(savedTime))) {
            appContext.saveDateTime(mContext);
            Log.w(TAG, "Caching system time..");
        }
        else {
            Log.e(TAG, "Either System time mismatch or App running in owner mode..");
        }
    }

    @Override
    public void onUpdate(int requestCode, Object result) {
        if(requestCode == OnUpdateListener.BATTERY_STATUS_UPDATED){
            Log.d(TAG, "onUpdate/BATTERY_STATUS_UPDATED");
            displayBatteryStatus(batteryStatus);
        }
        else if(requestCode == OnUpdateListener.DOOR_NAME_UPDATED){
            Log.d(TAG, "onUpdate/DOOR_NAME_UPDATED");
            doorNameTextView.setText(appContext.getDoor().getName());
        }
        else if(requestCode == OnUpdateListener.LOCK_STATUS_UPDATED){
            if(appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {
                Log.d("MainActivity", "Starting LockFragment");
                showLockButton();
                doorNameTextView.setText(appContext.getDoor().getName());
                invalidateOptionsMenu();
            }
        }
    }

    private void showLockButton()
    {
        LockStatus lockStatus = appContext.getLockStatus();
        Log.d(TAG, "Showing Lock button [Lock Status:" + lockStatus + "]");
        lockImageButton.setVisibility(View.VISIBLE);
        if(lockStatus == LockStatus.LOCKED)
        {
            Log.d(TAG, "Status Locked");
            lockImageButton.setImageResource(R.drawable.lock_src_selector);
        }
        else if(lockStatus == LockStatus.UNLOCKED)
        {
            Log.d(TAG, "Status Unlocked");
            lockImageButton.setImageResource(R.drawable.unlock_src_selector);
        }
        else
        {
            Toast toast = Toast.makeText(mContext,"No Door Status", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void doLockAccess()
    {
        Utils u = new Utils();
        u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
        u.requestDirection = Utils.TCP_SEND_PACKET;
        u.requestType = Utils.LOCK_ACCESS_REQUEST;
        final byte[] packet = new byte[MAX_PKT_SIZE];
        packet[RESPONSE_PACKET_TYPE_POS] = Utils.LOCK_ACCESS_REQUEST;
        packet[REQUEST_ACCESS_MODE_POS] = (byte) (appContext.getAppMode()== AppMode.OWNER ? Utils.APP_MODE_OWNER : Utils.APP_MODE_GUEST);
        packet[REQUEST_PACKET_LENGTH_POS] = LockPacket.SENT_PACKET_LENGTH;
        byte[] macInHex = Utils.toByteArray(mySharedPreferences.getMac());
        if(macInHex != null) {
            System.arraycopy(macInHex, 0, packet, 4, macInHex.length);
        }
        LockStatus lockStatus = appContext.getLockStatus();
        packet[LockPacket.DOOR_STATUS_REQUEST_POSITION] = (byte) ((lockStatus ==
                LockStatus.LOCKED) ? Utils.UNLOCKED : Utils.LOCKED);
        int[] currentDateTime = DateTimeFormat.splitDateTime();
        for(int i = 0; i < currentDateTime.length; i++)
        {
            packet[i + LockPacket.CURRENT_DATE_TIME_START] = (byte) currentDateTime[i];
        }
        //packet[LockPacket.CHECKSUM_SENT] = u.calculateChecksum(packet, true);

        if(appContext.getConnectionMode()== CONNECTION_MODE_BLE) {
            Log.d(TAG, "doLockAccess: ble connection");
            Utils.printByteArray(packet);
            mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                @Override
                public void onDataAvailable(String data) {
                    Log.d(TAG, "onDataAvailable: ");
                    processLockPacket(data);
                }
            }, null);
        }
        else if(appContext.getConnectionMode()== CONNECTION_MODE_REMOTE) {

            u.commandDetails = new String(packet, StandardCharsets.ISO_8859_1);
            u.setUtilsInfo(u);
            if (sharedpreferences.getBoolean(Utils.IS_STATIC, false)) {
                Log.d(TAG, "doLockAccess: NetClientAsyncTask");
                NetClientAsyncTask clientAsyncTask = new NetClientAsyncTask(true, HomeActivity.this, appContext.getRouterInfo().getAddress(),
                        appContext.getRouterInfo().getPort(),
                        packet, new OnTaskCompleted<String>() {
                    @Override
                    public void onTaskCompleted(int resultCode, String value) {
                        Log.d(TAG, "onTaskCompleted:" + value);
                        if (resultCode == Activity.RESULT_OK) {
                            processLockPacket(value);
                        } else {
                            lockAccessTextView.setVisibility(View.INVISIBLE);
                            activityProgressBar.setVisibility(View.INVISIBLE);
                            lockImageButton.setClickable(true);
                            if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.SOCKET_NOT_CONNECTED) {
                                Snackbar.make(findViewById(android.R.id.content), "Not connected", Snackbar.LENGTH_LONG).show();
                            } else if (NetClientAsyncTask.ERROR_CODE == NetClientAsyncTask.MESSAGE_NOT_RECEIVED) {
                                Snackbar.make(findViewById(android.R.id.content), "Timeout occurred", Snackbar.LENGTH_LONG).show();
                            }
                        }
                    }
                });
                clientAsyncTask.execute();
            }else {
                exit_fab.setEnabled(false);
                disconnect_fab.setEnabled(false);
                Log.d(TAG, "doLockAccess: ");
                byte[] data = Utils.encriptData(packet);
                for (byte b:data)
                    Log.d(TAG, "doLockAccess: "+String.format("%02X",b));
                mqttDataSendListener.sendData(data, PublishTopic.SUBSCRIBE_TOPIC_NO, PublishTopic.PUBLISH_TOPIC,MqttInterface.DEFAULT_WAIT_TIME, new MqttInterface() {
                    @Override
                    public void dataAvailable(byte[] data1) {
                        String value = Utils.getPacketData(data1);
                        //mqttDataSendListener.unSubscribe();
                        enableFabButtons();
                        processLockPacket(value);
                    }

                    @Override
                    public void timeOutError() {
                        Toast.makeText(HomeActivity.this, "Unable to communicate with device.", Toast.LENGTH_SHORT).show();
                        lockAccessTextView.setVisibility(View.INVISIBLE);
                        activityProgressBar.setVisibility(View.INVISIBLE);
                        lockImageButton.setClickable(true);
                        //mqttDataSendListener.unSubscribe();
                        enableFabButtons();
                        //Toast.makeText(HomeActivity.this, "Time out please try again.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void unableToSubscribe() {
                        lockAccessTextView.setVisibility(View.INVISIBLE);
                        activityProgressBar.setVisibility(View.INVISIBLE);
                        lockImageButton.setClickable(true);
                        enableFabButtons();
                    }

                    @Override
                    public void succOrFailToUnSubscribe() {

                    }
                });
            }
        }
    }

    private void enableFabButtons(){
        exit_fab.setEnabled(true);
        disconnect_fab.setEnabled(true);
    }
    private void processLockPacket(String packet)
    {
        //Log.d(TAG,"packet="+packet);

        //Utils u = new Utils();
        Log.d(TAG, "processLockPacket:length "+packet.length());
        if(packet.length() >= LockPacket.RECEIVED_PACKET_LENGTH) {
            //byte [] strBytes;
            try {
                //strBytes = packet.getBytes("ISO-8859-1");
                //Log.d(TAG, "Processing Received Packet:" + strBytes);
                Log.d(TAG, "processLockPacket:inside try "+packet.charAt(RESPONSE_PACKET_TYPE_POS));
                    if(packet.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.LOCK_ACCESS_REQUEST
                            && Utils.parseInt(packet,RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {

                        Log.d(TAG, "processLockPacket:status "+packet.charAt(LockPacket.LOCK_STATUS_POS));
                        if(packet.charAt(LockPacket.LOCK_STATUS_POS) == Utils.LOCKED)
                        {
                            Log.d(TAG, "LOCKED "+appContext.shouldPlaySound());
                            if(appContext.shouldPlaySound()) {
                                playSound(R.raw.d_lock);
                            }
                            lockImageButton.setImageResource(R.drawable.lock_src_selector);
                            appContext.setLockStatus(LockStatus.LOCKED);
                        }
                        else if(packet.charAt(LockPacket.LOCK_STATUS_POS) == Utils.UNLOCKED)
                        {
                            Log.d(TAG, "UNLOCKED "+appContext.shouldPlaySound());
                            if(appContext.shouldPlaySound()) {
                                playSound(R.raw.d_unlock);
                            }
                            lockImageButton.setImageResource(R.drawable.unlock_src_selector);
                            appContext.setLockStatus(LockStatus.UNLOCKED);
                        }
                        vibrator.vibrate(100);
                    }
                    else
                    {
                        if (Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS) == CommunicationError.BLE_NOT_CONNECTED){
                            mqttDataSendListener.disconnectMqtt();
                            sessionManager.logout();
                            Toast.makeText(this, "Connection Lost", Toast.LENGTH_SHORT).show();
                        }else {
                            if(Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS) == CommunicationError.DOOR_NOT_CLOSED){
                                if(appContext.shouldPlaySound()) {
                                    playSound(R.raw.close_door);
                                }
                            }
                            String errorMessage = CommunicationError.getMessage(Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS));
                            Log.e("Error [MainActivity]",errorMessage);
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                lockAccessTextView.setVisibility(View.INVISIBLE);
                activityProgressBar.setVisibility(View.INVISIBLE);
                lockImageButton.setClickable(true);
            }
            catch(Exception e)
            {
                Log.e(TAG, "Unsupported String Decoding Exception");
                lockAccessTextView.setVisibility(View.INVISIBLE);
                activityProgressBar.setVisibility(View.INVISIBLE);
                lockImageButton.setClickable(true);
                e.printStackTrace();
            }
            /*catch (InterruptedException e) {
                e.printStackTrace();
            }*/
        } 
        else
        {
            lockAccessTextView.setVisibility(View.INVISIBLE);
            activityProgressBar.setVisibility(View.INVISIBLE);
            //Toast toast = Toast.makeText(this, "Invalid or Null DoorMode", Toast.LENGTH_LONG);
            //toast.show();
            Log.e(TAG, "Invalid or Null DoorMode" );
        }
        Runtime.getRuntime().gc();
        flag=true;
    }

    private void playSound(int sound) {
        if (player == null){
            player = MediaPlayer.create(mContext, sound);
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlayer();
                }
            });
            player.start();
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void stopPlayer(){
        if (player != null){
            player.reset();
            player.release();
            player = null;
        }
    }


    private void displayBatteryStatus(int percentage)
    {
       Log.d(TAG, "Battery Percentage:"+percentage);
            batteryIcon.setVisibility(View.VISIBLE);
            batteryPercentageTextView.setVisibility(View.VISIBLE);
            String mpercentage = percentage + "%";
            batteryPercentageTextView.setText(mpercentage);
            if (percentage < 10)
            {
                batteryIcon.setImageResource(R.mipmap.ic_battery_alert_black_36dp);
                int color = Color.parseColor("#ffcc0000");
                batteryIcon.setColorFilter(color);
            }
            else if (percentage < 25)
            {
                batteryIcon.setImageResource(R.mipmap.ic_battery_20_black_36dp);
            }
            else if (percentage < 40)
            {
                batteryIcon.setImageResource(R.mipmap.ic_battery_30_black_36dp);
            }
            else if (percentage < 55)
            {
                batteryIcon.setImageResource(R.mipmap.ic_battery_50_black_36dp);
            }
            else if (percentage < 70)
            {
                batteryIcon.setImageResource(R.mipmap.ic_battery_60_black_36dp);
            }
            else if (percentage < 85)
            {
                batteryIcon.setImageResource(R.mipmap.ic_battery_80_black_36dp);
            }
            else if (percentage < 95)
            {
                batteryIcon.setImageResource(R.mipmap.ic_battery_90_black_36dp);
            }
            else if (percentage <= 100)
            {
                batteryIcon.setImageResource(R.mipmap.ic_battery_full_black_36dp);
            }
            else
                {
                batteryIcon.setImageResource(R.mipmap.ic_battery_unknown_black_36dp);
            }

            if (percentage <= 20)
            {
                new AlertDialog.Builder(mContext).setTitle("Warning")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(Html.fromHtml("<font color='#FF0000' line-height='40px'>Battery is critically low. Please replace the battery for further use.</font>"))
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).create().show();
            }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("MainActivity", "onCreateOptionsMenu called");
        if(appContext.getAppMode() == AppMode.OWNER) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
        }
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menu.clear();
        Log.d("MainActivity", "onPrepareOptionsMenu called");
        if(appContext.getAppMode() == AppMode.OWNER) {
            Log.d("MainActivity", "onPrepareOptionsMenu/OwnerRegistered");
            getMenuInflater().inflate(R.menu.menu_main, menu);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle menu item clicks here.
        int id = item.getItemId();

        if(appContext.getAppMode() == AppMode.OWNER){
            switch (id) {
                case R.id.History:
                    //if (appContext.getConnectionMode() == ConnectionMode.CONNECTION_MODE_BLE){
                        Intent guestLogIntent = new Intent(this, GuestLogActivity.class);
                        guestLogIntent.putExtra(Utils.EXTRA_CALLER_ACTIVITY_NAME, "MainActivity");
                        guestLogIntent.putExtra(Utils.EXTRA_DOWNLOAD_LOG, false);
                        Log.d(TAG,"Starting GuestLogActivity");
                        startActivity(guestLogIntent); 
                    /*}else {
                        Toast.makeText(this, "This feature is under implementation", Toast.LENGTH_LONG).show();
                    }*/
                    
                    return true;

                case R.id.viewGuest:
                    //if (appContext.getConnectionMode() == ConnectionMode.CONNECTION_MODE_BLE) {
                        DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
                        databaseHandler.updateKeyStatus(null, null);
                        databaseHandler.close();
                        Intent guestListIntent = new Intent(this, GuestListActivity.class);
                        Log.d(TAG, "Starting GuestListActivity");
                        startActivityForResult(guestListIntent, REQUEST_CODE_GUEST_LIST_ACTIVITY);
                    /*}else {
                        Toast.makeText(this, "This feature is under implementation", Toast.LENGTH_LONG).show();
                    }*/
                    return true;
                case R.id.Version:
                    byte[] packet = new byte[32];

                    packet[0] = 'E';
                    packet[1] = (byte) Utils.APP_MODE_VISITOR;
                    packet[2] = 4;
                    packet[3] = 'V';

                    Log.d(TAG, "factoryReset");
                    mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                        @Override
                        public void onDataAvailable(String data) {
                            Log.d(TAG, "lock version Data:"+data);
                            if(7==(int)data.charAt(3)) {
                                String buffer = data.charAt(4) + "." +
                                        data.charAt(5) + "." +
                                        data.charAt(6);
                                showVersionDialog(buffer);
                            }
                            else{
                                Toast.makeText(mContext, "Try again", Toast.LENGTH_SHORT).show();
                            }
                        }
                    },"Sending Request...");
                    return true;
                /*case R.id.Profile:
                    Intent profileIntent = new Intent(this, OwnerProfileActivity.class);
                    profileIntent.putExtra(ARGUMENT_FROM_MAIN, Utils.MASTER_EDIT_PROFILE_CODE);
                    Log.d("Main", "Starting OwnerProfileActivity");
                    startActivityForResult(profileIntent, Utils.MASTER_EDIT_PROFILE_CODE);
                    return true;*/

                case R.id.Settings:
                    SettingsActivity.isFirstTime=true;
                    Intent deviceOptionsIntent = new Intent(this, SettingsActivity.class);
                    //deviceOptionsIntent.putExtra("BluetoothDevice", mDevice);
                    Log.d(TAG, "Starting SettingsActivity");
                    startActivity(deviceOptionsIntent);
                    return true;

                case R.id.changeDevice:
                    Log.d(TAG, "Changing Device");
                    /*ExitFragment.ExitApp(MainActivity.this);
                    wifiManager.setWifiEnabled(false);
                    Intent i = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);*/
                    new androidx.appcompat.app.AlertDialog.Builder(mContext).setTitle("Warning")
                            .setMessage("Changing Door will disconnect you from current Door. Do you want to continue?")
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    /*ConnectActivity.setOnUpdateListener(MainActivity.this);
                                    DeviceListFragment.isDisconnecting = true;
                                    ConnectActivity.releaseP2pDevice();*/
                                }
                            })
                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create().show();
                    return true;
//AZFACTORYMODE: comment the following in non factory mode
               /*case R.id.reset:
                    factoryReset();
                    return true;*/

                case R.id.About:
                    Intent aboutIntent = new Intent(this, AboutActivity.class);
                    Log.d("Main", "Starting AboutActivity");
                    startActivity(aboutIntent);
                    return true;

                default:
                    return super.onOptionsItemSelected(item);
            }
        }
        return true;
    }/* else {
            switch(id)
            {
//AZFACTORYMODE: comment this in non factory mode
               *//*case R.id.reset:
                    factoryReset();
                    return true;*//*

                default:
                    return super.onOptionsItemSelected(item);
            }
        }*/

    private void showVersionDialog(String version){
        final  AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("FW Version");
        builder.setMessage(version);
        builder.setCancelable(true);
        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface d, int arg1) {
                d.cancel();
            }
        });
        final AlertDialog closedialog= builder.create();

        closedialog.show();
    }
    @Override
    public void onPause() {
        super.onPause();
     }
    /*public void factoryReset(){
        Utils u=new Utils();
        if (appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {
            byte[] packet = new byte[MAX_PKT_SIZE];

            packet[REQUEST_PACKET_TYPE_POS] = Utils.FACTORY_RESET_REQ;
            packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_VISITOR;
            packet[REQUEST_PACKET_LENGTH_POS] = FactoryResetPacket.SENT_PACKET_LENGTH;
            packet[FactoryResetPacket.RESET_POS] = 'R';
            packet[FactoryResetPacket.CHECKSUM_SENT] = u.calculateChecksum(packet, true);
            Log.d(TAG, "factoryReset");
            mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                @Override
                public void onDataAvailable(String data) {
                    Log.d(TAG, "receivedData:"+data);
                    if(data.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.RENAME_DOOR_REQUEST) {
                        processResetResponsePacket(data);
                    }
                }
            },"Sending Request...");
        }
    }*/

    public void processResetResponsePacket(String packet) {
        //Utils u = new Utils();
        if(packet != null) {
            //byte[] strBytes;
            try {
                //strBytes = packet.getBytes("ISO-8859-1");

                    if (packet.charAt(RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                        if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                            Toast.makeText(mContext, "Reset Successful", Toast.LENGTH_LONG).show();
                        }
                        else{
                            Toast.makeText(mContext, "Reset Failed", Toast.LENGTH_LONG).show();
                        }
                    }
            }
            catch(Exception e) {
                Log.d("SettingsActivity", "Unsupported String Decoding Exception");
            }
        }
    }
}

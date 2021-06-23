package com.asiczen.azlock;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.asiczen.azlock.app.AdapterViewCode;
import com.asiczen.azlock.app.AppMode;
import com.asiczen.azlock.app.CommunicationError;
import com.asiczen.azlock.app.DeviceStatus;
import com.asiczen.azlock.app.model.Door;
import com.asiczen.azlock.app.model.WifiNetwork;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.CustomAdapter;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.content.LogoutBroadcastReceiver;
import com.asiczen.azlock.content.SessionManager;
import com.asiczen.azlock.net.OnDataAvailableListener;
import com.asiczen.azlock.net.OnDataSendListener;
import com.asiczen.azlock.util.FileAccess;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.Utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/*
 * Created by user on 8/31/2015.
 */
public class SettingsActivity extends AppCompatActivity implements Packet, RegisterOwnerActivity.OnRegistrationListener,
        OnUpdateListener, OnSearchListener, OnCheckedChangeListener{

    private Context mContext;
    private AppContext appContext;
    private final String TAG = SettingsActivity.class.getSimpleName();
    private Calendar calendar;
    private int day, month, yyyy, hour, min;
    private static OnUpdateListener mOnUpdateListener;

    private final int SET_TIME = 111111;
    private final int RENAME_DOOR = 0;
    private final int CHANGE_PASSWORD = 222222;
    //private final int CALIBRATE_DEVICE = 8;
    private final int TAMPER_NOTIFICATION = 1;
    private final int AJAR = 2;
    private final int AUTO_LOCK = 3;

    private TextView errorTextView;
    private AlertDialog dialog;
    private String doorName, doorPassword, confirmPasswd;

    private EditText ownerContactEditText;
    private String mobile;
    //private final String doorID=null;
    private CustomAdapter<String> settingsCustomAdapter;
    private FileAccess fileAccess;
    private static boolean isTamperNotificationEnabled;
    private OnDataSendListener mOnDataSendListener;
    private SessionManager sessionManager;
    private IntentFilter intentFilter;
    private LogoutBroadcastReceiver logoutBroadcastReceiver;
    public static boolean isFirstTime=false;
    //public static boolean fisttime=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.deviceoptions);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.show();
            actionBar.setTitle(Html.fromHtml("<font color='#FFFFFF'>Settings</font>"));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mContext = this;
        appContext=AppContext.getContext();
        sessionManager = new SessionManager(this);
        logoutBroadcastReceiver = new LogoutBroadcastReceiver(this);
        intentFilter=new IntentFilter();
        intentFilter.addAction(SessionManager.ACTION_LOGOUT);
        intentFilter.addAction(SessionManager.ACTION_EXIT);

        this.fileAccess = new FileAccess(this, Utils.TAMPER_NOTIFICATION_CONFIG_FILE);
        mOnDataSendListener=appContext.getOnDataSendListener();

        //mDevice = getIntent().getParcelableExtra("BluetoothDevice");
        // doorID = appContext.getDoor().getId();
        if(appContext.getAjarStatus() == 1 || appContext.getAutolockStatus() == 1){
            appContext.setAjarTag(null);
        }
        if(appContext.getAjarStatus() == 0 && appContext.getAutolockStatus() == 0){
            appContext.setAjarTag("ajar");
        }
        String[] deviceInfoOptions;
        if(appContext.getAppMode()== AppMode.OWNER) {
            deviceInfoOptions = getResources().getStringArray(R.array.device_info_options);
        } 
		else {
            return;
        }
        ListView deviceOptionsListView =  findViewById(R.id.deviceOptionsList);
        settingsCustomAdapter = new CustomAdapter<>(this, R.layout.list_item_multiple_choice,
                new ArrayList<>(Arrays.asList(deviceInfoOptions)), AdapterViewCode.DEVICE_SETTINGS_VIEW_CODE);
        deviceOptionsListView.setAdapter(settingsCustomAdapter);
        calendar = Calendar.getInstance();
        yyyy = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        min = calendar.get(Calendar.MINUTE);

        deviceOptionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               switch (position) {
                    case SET_TIME:
                        new DatePickerDialog(mContext, new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                if (view.isShown()) {
                                    yyyy = year;
                                    month = monthOfYear;
                                    day = dayOfMonth;
                                    new TimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {
                                        @Override
                                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                            if (view.isShown()) {
                                                hour = hourOfDay;
                                                min = minute;
                                                doExecuteCommand(SET_TIME);
                                            }
                                        }
                                    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
                                }
                            }
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
                        break;
                    case AUTO_LOCK:
                        if(appContext.getAutolockStatus() == Utils.AJAR_STATUS) {
                            View time = getLayoutInflater().inflate(R.layout.change_door_name, Utils.nullParent,false);
                            final EditText editText = time.findViewById(R.id.door_name_editText);
                            editText.setHint("Ex:4 to 10 seconds");
                            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                            new AlertDialog.Builder(mContext)
                                    .setTitle("Configure Auto lock time")
                                    .setView(time)
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            String time = editText.getText().toString();
                                            if (!time.isEmpty()) {
                                                try {
                                                    int second =Integer.parseInt(time);
                                                    if(second>=4 && second<=10){
                                                        appContext.setAutolockTime(second);
                                                        setAjar(false, 10);
                                                    }else{
                                                        Toast.makeText(mContext,"Seconds must be in between 4 to 10", Toast.LENGTH_LONG).show();
                                                    }
                                                }catch (Exception e){
                                                    Toast.makeText(SettingsActivity.this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                                                }

                                            } else {
                                                Toast.makeText(mContext, "Time can not be empty", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    })
                                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    }).create().show();
                        }
                        break;
                    case RENAME_DOOR:
                        View renameView = getLayoutInflater().inflate(R.layout.change_door_name, Utils.nullParent,false);
                        final EditText renameEditText =  renameView.findViewById(R.id.door_name_editText);
                        new AlertDialog.Builder(mContext)
                                .setTitle("Rename Door")
                                .setView(renameView)
                                .setPositiveButton("RENAME", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        doorName = renameEditText.getText().toString();
                                        if(doorName.isEmpty()){
                                            Toast.makeText(mContext, "Door Name cannot be empty", Toast.LENGTH_LONG).show();
                                        }else if(doorName.trim().length()>8){
                                            Toast.makeText(mContext, "Door name must be less then 8", Toast.LENGTH_SHORT).show();
                                        } else if(doorName.trim().toLowerCase().contains("azlock")){
                                            Toast.makeText(mContext, "Any combination of azlock is not allowed as door name", Toast.LENGTH_SHORT).show();
                                        }else {
                                            doExecuteCommand(RENAME_DOOR);
                                        }
                                    }
                                })
                                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                }).create().show();

                        break;
                    case CHANGE_PASSWORD:
                        View passwordView = getLayoutInflater().inflate(R.layout.change_password, Utils.nullParent,false);
                        final EditText passwordEditText =  passwordView.findViewById(R.id.passwd_editText);
                        final EditText confirmPasswordEditText =  passwordView.findViewById(R.id.confirm_passwd_editText);
                        final AlertDialog dialog1 = new AlertDialog.Builder(mContext)
                                .setTitle("Change Password")
                                .setView(passwordView)
                                .setPositiveButton("SET", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).create();
                        dialog1.show();
                        dialog1.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                doorPassword = passwordEditText.getText().toString();
                                confirmPasswd = confirmPasswordEditText.getText().toString();
                                if (doorPassword != null && !doorPassword.isEmpty() && !confirmPasswd.isEmpty() && doorPassword.length() >= 8 && doorPassword.equals(confirmPasswd)) {
                                    doExecuteCommand(CHANGE_PASSWORD);
                                    dialog1.dismiss();
                                } else {
                                    if (doorPassword == null || doorPassword.isEmpty()) {
                                        Toast.makeText(mContext, "Password cannot be empty", Toast.LENGTH_LONG).show();
                                    } else if (confirmPasswd.isEmpty()) {
                                        Toast.makeText(mContext, "Confirm Password cannot be empty", Toast.LENGTH_LONG).show();
                                    } else if (doorPassword.length() < 8) {
                                        Toast.makeText(mContext, "Password must be greater than 8", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(mContext, "Password must be matched", Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        });
                        break;
                    /*
                    case CALIBRATE_DEVICE:
                        DialogFragment calibrationFragment = new CalibrationFragment();
                        //calibration_dialog.setCancelable(false);
                        calibrationFragment.show(getFragmentManager(), "CalibrationFragment");
                        break;*/

                    case TAMPER_NOTIFICATION:
                       // onSelectTamperNotification();
                        break;
                }
            }     }
        );
    }

    private void onSelectTamperNotification()
    {
        /*String tamperNotificationFlag = fileAccess.read();
        if(tamperNotificationFlag == null && fileAccess.FILE_NOT_FOUND){
            isTamperNotificationEnabled = false;
            fileAccess.write(String.valueOf(Utils.DISABLE_TAMPER_NOTIFICATION));
            Log.d(TAG, "DISABLE_TAMPER_NOTIFICATION\t[WRITE]");
        }
        else if(tamperNotificationFlag != null && !tamperNotificationFlag.isEmpty()) {
            isTamperNotificationEnabled = (Integer.parseInt(tamperNotificationFlag) == Utils.ENABLE_TAMPER_NOTIFICATION);
            Log.d(TAG, "TAMPER_NOTIFICATION:"+ tamperNotificationFlag +"\t[READ]");
        }*/
        Log.d(TAG, "onSelectTamperNotification: "+appContext.getTag());
        if (appContext.getTag() != null || isFirstTime) {
            isFirstTime=false;
            appContext.setTag(null);
            return;
        }
        isTamperNotificationEnabled = appContext.getTemperStatus();
        if(!isTamperNotificationEnabled) {
            View tamperNotificationView = getLayoutInflater().inflate(R.layout.tamper_notification, Utils.nullParent,false);
            ownerContactEditText =  tamperNotificationView.findViewById(R.id.owner_contact_editText);
            //enableNotificationCheckbox = (CheckBox) tamperNotificationView.findViewById(R.id.enable_notification_checkBox);
            errorTextView = tamperNotificationView.findViewById(R.id.error_textView);

            dialog = new AlertDialog.Builder(mContext)
                    .setView(tamperNotificationView)
                    .setCancelable(false)
                    .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "onClick: Cancel");
                            settingsCustomAdapter.onUpdate(OnUpdateListener.TAMPER_NOTIFICATION_UPDATED, null);

                            appContext.setTag("TAG");
                            dialog.dismiss();
                        }
                    })
                    .create();

           /* enableNotificationCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    //dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(isChecked);
                }
            });*/
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {

                @Override
                public void onShow(DialogInterface dialog) {
                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            });
            dialog.show();
            //dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mobile = ownerContactEditText.getText().toString();
                    if (mobile.isEmpty() || mobile.length() < 10) {
                        errorTextView.setVisibility(View.VISIBLE);
                    } else {
                        shouldEnable = true;
                        setTamperNotification(mobile);
                        dialog.dismiss();
                    }
                }
            });
        }
        else
        {

            new AlertDialog.Builder(mContext).setTitle("Tamper Notification")
                    .setCancelable(false)
                    .setMessage("By disabling this feature, the App cannot be able to send any notification on unauthorized access. Are you sure you want to disable this feature?")
                    .setPositiveButton("Disable", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            shouldEnable = false;
                            setTamperNotification(null);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            settingsCustomAdapter.onUpdate(OnUpdateListener.TAMPER_NOTIFICATION_UPDATED, null);
                            //if(!tamperNotificationFlag.equals("1"))
                                appContext.setTag("TAG");
                            dialog.dismiss();
                        }
                    }).create().show();
        }
    }


    private boolean shouldAsk=false;
    private void onSelectAskPin(){
        String msg, buttonText;
        if(appContext.shouldAskPin()){
            msg="By disabling this feature, the App will never ask for pin and whoever has your phone can access azLock. Are you sure you want to disable this feature?";
            buttonText="Disable";
            shouldAsk=false;
        }
        else{
            msg="By enabling this feature, the App will ask every time for pin on start. Are you sure you want to enable this feature?";
            buttonText="Enable";
            shouldAsk=true;
        }
        new AlertDialog.Builder(mContext).setTitle("Update Security")
                .setCancelable(false)
                .setMessage(msg)
                .setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            appContext.updateAskPinStatus(mContext, shouldAsk);
            settingsCustomAdapter.onUpdate(OnUpdateListener.ASK_PIN_UPDATED, null);
        }
    })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            settingsCustomAdapter.onUpdate(OnUpdateListener.ASK_PIN_UPDATED, null);
            dialog.dismiss();
        }
    }).create().show();
}
    private void onSelectPlaySound(boolean playSound)
    {
        appContext.setPlaySoundOnLockUnlock(mContext, playSound);
        appContext.checkPlaySoundOnLockUnlock(mContext);
    }

    public static boolean shouldEnable = false;
    private boolean isAjarAnable;
    private void setTamperNotification(String contact)
    {
        Log.d(TAG,"Sending Packet");
        if(appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {
            Utils u = new Utils();
            u.requestType = Utils.TAMPER_REQUEST;
            u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
            u.requestDirection = Utils.TCP_SEND_PACKET;
            byte[] packet = new byte[MAX_PKT_SIZE];
            packet[REQUEST_PACKET_TYPE_POS] = Utils.TAMPER_REQUEST;
            packet[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_OWNER;
            packet[REQUEST_PACKET_LENGTH_POS] = TamperPacket.SENT_PACKET_LENGTH;
            packet[TamperPacket.NOTIFICATION_POSITION] = (byte)(shouldEnable ? TamperPacket.ENABLE : TamperPacket.DISABLE);
            if(shouldEnable) {
                for (int i = 0; i < contact.length(); i++) {
                    packet[i + TamperPacket.OWNER_PHONE_START] = (byte) contact.charAt(i);
                }
            }
            //packet[TamperPacket.CHECKSUM_SENT] = u.calculateChecksum(packet, true);
            mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                @Override
                public void onDataAvailable(String data) {
                    Log.d(TAG, "receivedData:"+data);
                    processPacket(data, TAMPER_NOTIFICATION);
                }
            },(shouldEnable ? "Enabling Notification..." : "Disabling Notification..."));
        }
    }

    /*private void setAlternateOwner(String name, String mac, boolean shouldDeleteGuests, boolean shouldDeleteLogs)
    {
        //String errMsg = "";
        Owner owner = new Owner();
        Door door = new Door();
        door.setId(doorID);
        door.setName(appContext.getDoor().getName());
        owner.setId(mac);
        owner.setName(name);
        owner.setAccessMode("owner");
        if(appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {
            Utils u = new Utils();
            u.requestType = Utils.OWNER_REQUEST;
            u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
            u.requestDirection = Utils.TCP_SEND_PACKET;
            byte[] packet = new byte[MAX_PKT_SIZE];
            packet[REQUEST_PACKET_TYPE_POS] = Utils.OWNER_REQUEST;
            packet[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_OWNER;
            packet[REQUEST_PACKET_LENGTH_POS] = OwnerRegistrationPacket.SENT_PACKET_LENGTH;

            byte[] ownerMacId = Utils.toByteArray(owner.getId());
            System.arraycopy(ownerMacId, 0, packet, 3, ownerMacId.length);
            for (int i = 0; i < name.length() && i < (OwnerRegistrationPacket.DELETE_FLAG - OwnerRegistrationPacket.OWNER_NAME_START); i++) {
                packet[i + OwnerRegistrationPacket.OWNER_NAME_START] = (byte) name.charAt(i);
                if (i == name.length() - 1) {
                    packet[i + 1] += '\0';
                }
            }
            if((shouldDeleteGuests && shouldDeleteLogs)) {
                packet[OwnerRegistrationPacket.DELETE_FLAG] = OwnerRegistrationPacket.DELETE_ALL_LOGS_AND_GUESTS;
            }
            else if((shouldDeleteGuests)) {
                packet[OwnerRegistrationPacket.DELETE_FLAG] = OwnerRegistrationPacket.DELETE_ALL_GUESTS;
            }
            else if((shouldDeleteLogs)) {
                packet[OwnerRegistrationPacket.DELETE_FLAG] = OwnerRegistrationPacket.DELETE_ALL_LOGS;
            }
            else {
                packet[OwnerRegistrationPacket.DELETE_FLAG] = OwnerRegistrationPacket.DELETE_NOTHING;
            }

            if(!name.isEmpty())
            {
                //packet[OwnerRegistrationPacket.CHECKSUM_SENT] = u.calculateChecksum(packet, true);
                Utils.printByteArray(packet);
                try {
                    mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                        @Override
                        public void onDataAvailable(String data) {
                            Log.d(TAG, "receivedData:"+data);
                            processAlternateOwnerPacket(data);
                        }
                    },"Registering...");
                    Log.d(TAG,new String(packet, StandardCharsets.ISO_8859_1));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                u.commandDetails = new String(packet, StandardCharsets.ISO_8859_1);
            }
            else {
                Toast.makeText(mContext, "Enter valid name", Toast.LENGTH_LONG).show();
            }
        }
        else {

            Toast.makeText(mContext, "No connected device found", Toast.LENGTH_LONG).show();
        }
    }*/

   /* private void processAlternateOwnerPacket(String receivedPacket)
    {
        //Utils u = new Utils();
        Log.d(TAG, "Processing received packet:" + receivedPacket);
        if(receivedPacket != null && receivedPacket.length() >= OwnerRegistrationPacket.RECEIVED_PACKET_LENGTH)
        {
            //byte[] strBytes;
            try
            {
                //strBytes = receivedPacket.getBytes("ISO-8859-1");

                    if(receivedPacket.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.OWNER_REQUEST)
                    {
                        if (Utils.parseInt(receivedPacket,RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK)
                        {
                            Log.d(TAG, "RegistrationPacket/COMMAND_STATUS_POS"+
                                    Utils.parseInt(receivedPacket,RESPONSE_COMMAND_STATUS_POS));

                            if (receivedPacket.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS)
                            {

                                onRegistration(Utils.SUCCESS);
                            } else if (receivedPacket.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE)
                            {
                                Log.d(TAG, "[Error]: Owner's doorMode cannot be inserted due to Device error");
                                onRegistration(Utils.DEVICE_ERROR_CODE);
                            }
                        }
                        else
                        {
                            String errorMessage = CommunicationError.getMessage(
                                    receivedPacket.charAt(RESPONSE_COMMAND_STATUS_POS));
                            Log.d(TAG, "CMD_STS_ERR:"+errorMessage);
                            Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                    else
                    {
                        Log.d(TAG, "RESPONSE_PACKET_TYPE_ERROR");
                    }
            } catch (Exception e) {
                Log.d(TAG, "Unsupported String Decoding Exception");
            }
        } else {
            Toast toast = Toast.makeText(mContext, "Invalid or Null DoorMode", Toast.LENGTH_LONG);
            toast.show();

        }
    }*/

    private void doExecuteCommand(int requestCode)
    {
        //int deviceInfoOption = requestCode;
        //boolean isValidMac = true;
        if(appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {
            Utils u = new Utils();
            u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
            u.requestDirection = Utils.TCP_SEND_PACKET;

            byte[] packet = new byte[MAX_PKT_SIZE];



            if(requestCode == SET_TIME)
            {
                Log.d("SettingsActivity", "Setting Time/Alarm :"+requestCode);
                u.requestType = Utils.CONFIG_TIME_REQ;
                packet[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_OWNER;
                packet[REQUEST_PACKET_TYPE_POS] = Utils.CONFIG_TIME_REQ;
                packet[REQUEST_PACKET_LENGTH_POS] = ConfigPacket.SENT_CONFIG_TIME_PACKET_LENGTH;
                int[] dateTime = new int[]{day, month+1, (yyyy/100), (yyyy%100), hour, min};
                for(int x : dateTime)
                {
                    Log.d("SettingsActivity", "Setting Time/"+x);
                }
                for(int i=0;i<dateTime.length;i++)
                    packet[i+ConfigPacket.CURRENT_DATE_TIME_POSITION] = (byte) dateTime[i];
                //packet[ConfigPacket.CHECKSUM_SENT] = u.calculateChecksum(packet, true);
            }
            else if(requestCode == RENAME_DOOR){
                u.requestType = Utils.RENAME_DOOR_REQUEST;
                packet[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_OWNER;
                packet[REQUEST_PACKET_TYPE_POS] = Utils.RENAME_DOOR_REQUEST;
                packet[REQUEST_PACKET_LENGTH_POS] = DoorSettingsPacket.SENT_PACKET_LENGTH;
                //packet[DoorSettingsPacket.SETTINGS_OPTION_POS] = DoorSettingsPacket.RENAME_DOOR;

                for(int i = 0; i < doorName.length(); i++) {
                    packet[DoorSettingsPacket.DOOR_NAME_START + i] = (byte) doorName.charAt(i);
                }
                //packet[DoorSettingsPacket.CHECKSUM_SENT] = u.calculateChecksum(packet, true);
            }
            else if(requestCode == CHANGE_PASSWORD){
                u.requestType = Utils.CHANGE_PASSWORD_REQUEST;
                packet[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_OWNER;
                packet[REQUEST_PACKET_TYPE_POS] = Utils.CHANGE_PASSWORD_REQUEST;
                packet[REQUEST_PACKET_LENGTH_POS] = DoorSettingsPacket.SENT_PACKET_LENGTH;
                packet[DoorSettingsPacket.SETTINGS_OPTION_POS] = DoorSettingsPacket.CHANGE_PASSWORD;

                for(int i = 0; i < doorPassword.length(); i++) {
                    packet[DoorSettingsPacket.DOOR_PASSWORD_START + i] = (byte) doorPassword.charAt(i);
                }
                //packet[DoorSettingsPacket.CHECKSUM_SENT] = u.calculateChecksum(packet, true);
            }
/*else if(requestCode == SET_ALTERNATE_PHONE_AS_OWNER){
                try {
                    byte[] mac = u.getMacIdInHex(phoneMac.getText().toString());
                    for (int i = 0; i < mac.length; i++)
                        packet[i + DeviceInfoPacket.SET_DATE_TIME_POSITION] = mac[i];
                } catch (Exception e){
                    errorTextView.setText("Invalid MAC address.");
                    errorTextView.setVisibility(View.VISIBLE);
                    isValidMac = false;
                }
            }*/


            u.commandDetails = new String(packet, StandardCharsets.ISO_8859_1);
            Log.d("DeviceOptionActivity", "Sent Packet:" + u.commandDetails);
            u.setUtilsInfo(u);

            mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                @Override
                public void onDataAvailable(String data) {
Log.d(TAG, "receivedData:"+data);                            if(data.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.RENAME_DOOR_REQUEST) {
                        processPacket(data, RENAME_DOOR);
                    }
                }
            },"Sending Request...");

        }
        else {
            Log.d("SettingsActivity", "Device not connected");
        }
    }

    private void processPacket(String packet, int requestCode)
    {
        //Utils u = new Utils();
        Log.d("SettingsActivity", "Received Packet:" + packet+" [ "+requestCode+" ]");
        if(packet != null) {
            //strBytes = packet.getBytes(StandardCharsets.ISO_8859_1);

            if (packet.charAt(RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                    String responseMsg = null;

                    switch (requestCode) {
                        case SET_TIME:
                            responseMsg = "Device time has been set successfully";
                            break;
                        case RENAME_DOOR:
                            DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
                            databaseHandler.update(new Door(appContext.getDoor().getId(), doorName));
                            databaseHandler.update(new WifiNetwork(doorName), appContext.getDoor().getName(), WifiNetwork.UPDATE_SSID);
                            responseMsg = "Door name has been changed successfully";
                            appContext.getDoor().setName(doorName);
                            mOnUpdateListener.onUpdate(OnUpdateListener.DOOR_NAME_UPDATED, null);
                            databaseHandler.close();
                            break;
                        case CHANGE_PASSWORD:
                            responseMsg = "Password has been set successfully";
                            break;
                        case TAMPER_NOTIFICATION:
                            responseMsg = shouldEnable ? "Notification Successfully Enabled" : "Notification Successfully Disabled";
                            if(shouldEnable) {
                                appContext.setTemperStatus(true);
                                fileAccess.write(String.valueOf(Utils.ENABLE_TAMPER_NOTIFICATION));
                                Log.d(TAG, "ENABLE_TAMPER_NOTIFICATION\t[writing]");
                            }
                            else
                            {
                                appContext.setTemperStatus(false);
                                fileAccess.write(String.valueOf(Utils.DISABLE_TAMPER_NOTIFICATION));
                                Log.d(TAG, "DISABLE_TAMPER_NOTIFICATION\t[writing]");
                            }
                            settingsCustomAdapter.onUpdate(OnUpdateListener.TAMPER_NOTIFICATION_UPDATED, null);
                            break;

                    }
                    if (responseMsg != null) {
                            /*new AlertDialog.Builder(mContext).setMessage(responseMsg)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    }).create().show();*/
                        Snackbar.make(findViewById(android.R.id.content), responseMsg, Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                } else if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                    new AlertDialog.Builder(mContext).setMessage("Error occurred while fetching details from the device.")
                            .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //doExecuteCommand(deviceInfoOption);
                                    dialog.dismiss();
                                }
                            }).create().show();
                }
            }
            else
            {
                Log.d("SettingsActivity", CommunicationError.getMessage(Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS)));
                Toast.makeText(mContext, CommunicationError.getMessage(Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS)),
                        Toast.LENGTH_LONG).show();
            }
        }
        else
        {
            Log.d("SettingsActivity", "Invalid Packet");
            new AlertDialog.Builder(mContext)
                    .setMessage("Invalid Packet")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(sessionManager.verify()){
            finish();
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        registerReceiver(logoutBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy()
    {
        shouldEnable = false;
        if(logoutBroadcastReceiver!=null) {
            unregisterReceiver(logoutBroadcastReceiver);
        }
        Runtime.getRuntime().gc();
        Log.d("SettingsActivity", "onDestroy() called");
        super.onDestroy();
    }

    public static void setOnUpdateListener(HomeActivity onUpdateListener){
        mOnUpdateListener =  onUpdateListener;
    }

    @Override
    public void onRegistration(int resultCode) {
        if(resultCode == Utils.SUCCESS){
            Snackbar.make(findViewById(android.R.id.content), "Owner Successfully Changed", Snackbar.LENGTH_LONG).show();
        }
        else if(resultCode == Utils.DEVICE_ERROR_CODE) {
            Snackbar.make(findViewById(android.R.id.content), "Device Error", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onUpdate(int resultCode, Object result) {

    }


    @Override
    public void onSearch(List results) {

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked, int requestCode) {
        switch (requestCode){
            case REQUEST_TAMPER_NOTIFICATION:
                onSelectTamperNotification();
                break;
            case REQUEST_ASK_PIN:
                onSelectAskPin();
                break;
            case REQUEST_PLAY_SOUND:
                onSelectPlaySound(isChecked);
                break;
            case REQUEST_AJAR:
                ajarCall(isChecked);
                //Toast.makeText(mContext, "AJAR"+isChecked, Toast.LENGTH_SHORT).show();
                break;
            case REQUEST_AUTO_LOCK:
                autolockCall(isChecked);
                //Toast.makeText(mContext, "AUTO LOCK"+isChecked, Toast.LENGTH_SHORT).show();
                break;

        }
    }
    private void ajarCall(boolean isChecked){
        if (appContext.getAjarTag() == null ) {
            if(appContext.getAutolockStatus() == 1)
                appContext.setAjarTag(null);
            else
                appContext.setAjarTag("ajar");
            return;
        }
        if(!isChecked)
            appContext.setAutolockStatus(0);
        setAjar(isChecked,Utils.AJAR_C);
    }
    private void autolockCall(boolean isChecked){

        if (appContext.getAjarTag() == null ) {
            appContext.setAjarTag("ajar");
            return;//used to handle multiple showing of message showing
        }
        if(appContext.getAjarStatus() ==0) {
            if(isChecked) {//id does not enable autolock if ajar is off
                appContext.setAjarTag(null);
                //Toast.makeText(mContext, "Enable Ajar", Toast.LENGTH_SHORT).show();
                Snackbar.make(findViewById(android.R.id.content), "Please Enable Ajar", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                settingsCustomAdapter.onUpdate(OnUpdateListener.AJAR_UPDATE, null);
            }else{
                setAjar(false,Utils.AUTOLOCK_C);
            }
        }else
            setAjar(isChecked,Utils.AUTOLOCK_C);
    }
    private void setAjar(boolean anable,final int i){

        int ajarStatus;
        int autolocStatus;
        isAjarAnable=anable;
        if(i==Utils.AJAR_C){
            ajarStatus = anable?1:0;
            autolocStatus = appContext.getAutolockStatus();
        }else{
            ajarStatus = appContext.getAjarStatus();
            if(i==10){
                autolocStatus = appContext.getAutolockStatus();
            }
            else{
                autolocStatus = anable?1:0;
            }
        }
        Log.d(TAG, "setAjar: ajarStatus="+ajarStatus+" autolocStatus="+autolocStatus);
        if(appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {

            byte[] packet = new byte[MAX_PKT_SIZE];
            packet[REQUEST_PACKET_TYPE_POS] = Utils.AJAR_REQUEST;
            packet[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_OWNER;
            packet[REQUEST_PACKET_LENGTH_POS] = AjarPacket.SENT_PACKET_LENGTH;
            packet[AjarPacket.AJAR_STATUS_POSITION] = (byte) ajarStatus;
            packet[AjarPacket.AUTOLOCK_STATUS_POSITION] = (byte) autolocStatus;
            packet[AjarPacket.AUTOLOCK_TIME_POSITION] = (byte) appContext.getAutolockTime();


            mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                @Override
                public void onDataAvailable(String data) {
                    Log.d(TAG, "receivedData:"+data);
                    //appContext.setAjarStatus('1');
                    Log.d(TAG, "i= "+i);
                    if(i==Utils.AJAR_C)
                        processAjarPacket(data, AJAR);
                    else if(i==Utils.AUTOLOCK_C)
                        processAjarPacket(data, AUTO_LOCK);
                    else
                        settingsCustomAdapter.onUpdate(OnUpdateListener.AJAR_UPDATE, null);
                }
            },(anable ? "Enabling Ajar..." : "Disabling Ajar..."));
        }
    }
    private void processAjarPacket(String packet,int requestcode){

        String responseMsg="";
        Log.d(TAG, "processAjarPacket: status "+isAjarAnable);
        Log.d(TAG, "processAjarPacket: packet "+packet.charAt(AjarPacket.STATUS));
        switch (requestcode){
            case AJAR:
                responseMsg = isAjarAnable? "AJAR Successfully Enabled" : "AJAR Successfully Disabled";
                if(packet.charAt(AjarPacket.STATUS) == AjarPacket.SUCCESS){
                    if(isAjarAnable)
                        appContext.setAjarStatus(1);
                    else {
                        //deviceOptionsListView.getChildAt(AUTO_LOCK).setEnabled(false);
                        appContext.setAjarStatus(0);

                    }
                }
                settingsCustomAdapter.onUpdate(OnUpdateListener.AJAR_UPDATE, null);
                break;
            case AUTO_LOCK:
                responseMsg = isAjarAnable? "Autolock Successfully Enabled" : "Autolock Successfully Disabled";
                if(packet.charAt(AjarPacket.STATUS) == AjarPacket.SUCCESS){
                    if(isAjarAnable)
                        appContext.setAutolockStatus(1);
                    else
                        appContext.setAutolockStatus(0);
                }
                settingsCustomAdapter.onUpdate(OnUpdateListener.AJAR_UPDATE, null);
                break;
        }
        if(!responseMsg.isEmpty()){
            Snackbar.make(findViewById(android.R.id.content), responseMsg, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }
}

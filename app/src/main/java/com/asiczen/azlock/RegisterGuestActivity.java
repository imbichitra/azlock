package com.asiczen.azlock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.asiczen.azlock.app.CommunicationError;
import com.asiczen.azlock.app.ConnectionMode;
import com.asiczen.azlock.app.DeviceStatus;
import com.asiczen.azlock.app.model.Door;
import com.asiczen.azlock.app.model.Guest;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.LogoutBroadcastReceiver;
import com.asiczen.azlock.content.SessionManager;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.net.MqttDataSendListener;
import com.asiczen.azlock.net.MqttInterface;
import com.asiczen.azlock.net.OnDataAvailableListener;
import com.asiczen.azlock.net.OnDataSendListener;
import com.asiczen.azlock.util.DateTimeFormat;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.RoundedImageView;
import com.asiczen.azlock.util.Utils;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import static android.Manifest.permission_group.CAMERA;
import static com.asiczen.azlock.GuestListActivity.ARGUMENT_FROM_GUEST_LIST_ACTIVITY;

/*
 * Created by user on 9/4/2015.
 */
public class RegisterGuestActivity extends AppCompatActivity implements Packet {

    private Context mContext;
    private AppContext appContext;
    private EditText name;
    @SuppressLint("StaticFieldLeak")
    public static EditText phoneMac;
    private TextView UpdateName;
    private Button startDate;
    private Button endDate;
    private ImageView photoImageView;
    private String startTime24, endTime24;
    private Calendar calendar;
    private int accessTypeId;
    private Guest guest;
    private Door door;
    private int[] startDateToSend, endDateToSend, startTimeToSend, endTimeToSend;
    private final static int RESULT_SELECT_IMAGE_CODE = 100;
    private static int requestCode;
    private static final String TAG = RegisterGuestActivity.class.getSimpleName();
    private OnDataSendListener mOnDataSendListener;
    private String packet2;
    private SessionManager sessionManager;
    private IntentFilter intentFilter;
    private LogoutBroadcastReceiver logoutBroadcastReceiver;
    private static final int REQUEST_CAMERA =1;

    private MqttDataSendListener mqttDataSendListener;
    String mStartDate;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_guest);
        requestCode = Objects.requireNonNull(getIntent().getExtras()).getInt(ARGUMENT_FROM_GUEST_LIST_ACTIVITY);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.show();
            if(requestCode == Utils.GUEST_EDIT_PROFILE_CODE){
                actionBar.setTitle(Html.fromHtml("<font color='#FFFFFF'> Update Guest</font>"));
            }
            if(requestCode == Utils.GUEST_REGISTRATION_CODE ){
                actionBar.setTitle(Html.fromHtml("<font color='#FFFFFF'>Register Guest</font>"));
            }

            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mContext = this;
        accessTypeId=0;
        appContext=AppContext.getContext();
        mOnDataSendListener = appContext.getOnDataSendListener();
       // requestCode = getIntent().getExtras().getInt(ARGUMENT_FOR_GUEST);

        mqttDataSendListener = appContext.getMqttSendListener();

        startDateToSend = new int[4];
        endDateToSend = new int[4];
        startTimeToSend = new int[2];
        endTimeToSend = new int[2];
        calendar = Calendar.getInstance();
        if(!appContext.isConnected()){
            finish();
        }
        sessionManager = new SessionManager(this);
        logoutBroadcastReceiver = new LogoutBroadcastReceiver(this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(SessionManager.ACTION_LOGOUT);
        intentFilter.addAction(SessionManager.ACTION_EXIT);

        ImageView mac_scanner = findViewById(R.id.mac_scanner);
        mac_scanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if(checkPermission()){
                        showScanner();
                        //Toast.makeText(this, "Permission is granted!", Toast.LENGTH_SHORT).show();
                    }else {
                        requestPermission();
                    }
                }else {
                    showScanner();
                }
            }
        });
        name =  findViewById(R.id.NameEditText);

        //phone =  findViewById(R.id.phone_num);
        phoneMac =  findViewById(R.id.PhoneMacIdEditText);
        Spinner accessTypesSpinner =  findViewById(R.id.access_type_spinner);
        startDate =  findViewById(R.id.start_date_button);
        endDate =  findViewById(R.id.end_date_button);
        Button register =  findViewById(R.id.register_button);
        photoImageView =  findViewById(R.id.photo_imageView);

        drawImage(photoImageView, BitmapFactory.decodeResource(MainActivity.mContext.getResources(), R.mipmap.ic_user));
        ArrayAdapter<String> accessTypeListAdapter = new ArrayAdapter<>(mContext, R.layout.spinner_row_item,
                R.id.spinner_row_textView, Utils.accessTypeList);
        accessTypesSpinner.setAdapter(accessTypeListAdapter);

        if(requestCode == Utils.GUEST_EDIT_PROFILE_CODE){
            mac_scanner.setVisibility(View.GONE);
            UpdateName=findViewById(R.id.UpdateName);
            UpdateName.setText(GuestDetailsFragment.guest.getName());
            //name.setText(GuestDetailsFragment.guest.getName());
            name.setVisibility(View.GONE);
            //phone.setText(GuestDetailsFragment.guest.getPhone());
            //email.setText(GuestDetailsFragment.guest.getEmail());
            phoneMac.setText(GuestDetailsFragment.guest.getId());
            name.setEnabled(false);
            phoneMac.setEnabled(false);
            accessTypesSpinner.setSelection(accessTypeListAdapter.getPosition(GuestDetailsFragment.guest.getAccessType()));
            Bitmap image = GuestDetailsFragment.guest.getImage();
            if(image == null) {
                image = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_user);
            }
            register.setText(R.string.update);
            drawImage(photoImageView, image);
        }

        mStartDate = "\t"+ DateTimeFormat.getDate(calendar.get(Calendar.DAY_OF_MONTH),
                (calendar.get(Calendar.MONTH) + 1), calendar.get(Calendar.YEAR), 2)+"\t\t"
                +DateTimeFormat.convertTimeTo12Hours(calendar.get(Calendar.HOUR_OF_DAY) +
                ":" + calendar.get(Calendar.MINUTE));
        startDate.setText(mStartDate);
        String mEndDate = "\t"+DateTimeFormat.getDate(calendar.get(Calendar.DAY_OF_MONTH),
                (calendar.get(Calendar.MONTH) + 1), calendar.get(Calendar.YEAR), 2)+"\t\t"
                +DateTimeFormat.convertTimeTo12Hours(calendar.get(Calendar.HOUR_OF_DAY) +
                ":" + calendar.get(Calendar.MINUTE));
        endDate.setText(mEndDate);

        startDateToSend[0] = endDateToSend[0] = calendar.get(Calendar.DAY_OF_MONTH);
        startDateToSend[1] = endDateToSend[1] = (calendar.get(Calendar.MONTH) + 1);
        startDateToSend[2] = endDateToSend[2] = calendar.get(Calendar.YEAR) / 100;
        startDateToSend[3] = endDateToSend[3] = calendar.get(Calendar.YEAR) % 100;
        startTimeToSend[0] = endTimeToSend[0] = calendar.get(Calendar.HOUR_OF_DAY);
        startTimeToSend[1] = endTimeToSend[1] = calendar.get(Calendar.MINUTE);
        startTime24 = endTime24 = calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE);

        accessTypesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(mContext, parent.getItemAtPosition(position).toString(), Toast.LENGTH_SHORT).show();
                accessTypeId = position;
                if (position == 0) {
                    startDate.setVisibility(View.VISIBLE);
                    findViewById(R.id.start_time_label).setVisibility(View.VISIBLE);
                    findViewById(R.id.end_time_label).setVisibility(View.VISIBLE);
                    endDate.setVisibility(View.VISIBLE);
                } else if (position == 1) {
                    startDate.setVisibility(View.GONE);
                    endDate.setVisibility(View.GONE);
                    findViewById(R.id.start_time_label).setVisibility(View.GONE);
                    findViewById(R.id.end_time_label).setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        photoImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                try {
                    //Pick Image From Gallery
                    /*Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, RESULT_SELECT_IMAGE_CODE);*/
                    Intent intent = new   Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent, RESULT_SELECT_IMAGE_CODE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });
    }
    private void showScanner(){
         Intent scannCode = new Intent(RegisterGuestActivity.this,ScannCodeActivity.class);
         startActivity(scannCode);
    }
    private boolean checkPermission(){
        return (ContextCompat.checkSelfPermission(RegisterGuestActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
    }
    private void requestPermission(){
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},REQUEST_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0) {
                boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (cameraAccepted) {
                    showScanner();
                    //Toast.makeText(this, "permissin granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "permissin denied!", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(CAMERA)) {
                            displayAlertMessage("You need to all access for both permission",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            requestPermission();
                                        }
                                    });
                        }
                    }
                }
            }
        }
        // super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void displayAlertMessage(String message, DialogInterface.OnClickListener listener){
        new AlertDialog.Builder(RegisterGuestActivity.this)
                .setTitle(message)
                .setPositiveButton("OK",listener)
                .setNegativeButton("Cancel",listener)
                .create()
                .show();
    }
    public void onClickStartDateButton(View v){
        new DatePickerDialog(mContext, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                //Toast.makeText(mContext, year + ":" + monthOfYear + ":" + dayOfMonth, Toast.LENGTH_SHORT).show();
                final String currentTime = "\t"+DateTimeFormat.getMonthName(monthOfYear + 1) + " " + dayOfMonth + ", " + year;
                startDate.setText(currentTime);
                startDateToSend[0] = dayOfMonth;
                startDateToSend[1] = monthOfYear + 1;
                startDateToSend[2] = year / 100;
                startDateToSend[3] = year % 100;

                new TimePickerDialog(mContext,new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        startTime24 = hourOfDay+":"+minute;
                        String time = currentTime+"\t\t"+DateTimeFormat.convertTimeTo12Hours(hourOfDay + ":" + minute);
                        startDate.setText(time);
                        startTimeToSend[0] = hourOfDay;
                        startTimeToSend[1] = minute;
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    public void onClickEndDateButton(View v){
        new DatePickerDialog(mContext, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                //Toast.makeText(mContext, year + ":" + monthOfYear + ":" + dayOfMonth, Toast.LENGTH_SHORT).show();
                final String temp = "\t"+DateTimeFormat.getMonthName(monthOfYear + 1) + " " + dayOfMonth + ", " + year;
                endDate.setText(temp);
                endDateToSend[0] = dayOfMonth;
                endDateToSend[1] = monthOfYear + 1;
                endDateToSend[2] = year / 100;
                endDateToSend[3] = year % 100;

                new TimePickerDialog(mContext,new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        //Toast.makeText(mContext, hourOfDay+":"+minute, Toast.LENGTH_SHORT).show();
                        endTime24 = hourOfDay+":"+minute;
                        String time= temp+"\t\t"+DateTimeFormat.convertTimeTo12Hours(hourOfDay + ":" + minute);
                        endDate.setText(time);
                        endTimeToSend[0] = hourOfDay;
                        endTimeToSend[1] = minute;
                    }
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    public void onClickRegisterGuestButton(View v){
        if(requestCode == Utils.GUEST_EDIT_PROFILE_CODE) {
            Guest mguest = new Guest();
            Door mdoor = new Door();
            String guestKey = phoneMac.getText().toString();
            mguest.setId(guestKey);

            String doorID = appContext.getDoor().getId();
            if(doorID != null) {
                mdoor.setId(doorID);
            }
            deleteGuest(mguest,mdoor,accessTypeId);
        }else {
            DatabaseHandler db = new DatabaseHandler(mContext);
            boolean isGusetAvauilable = db.isExist(phoneMac.getText().toString().toUpperCase());
            if (isGusetAvauilable){
                Toast.makeText(mContext, "Guest already registered", Toast.LENGTH_SHORT).show();
            }else {
                doGuestRegistration(accessTypeId);
            }
            //doGuestRegistration(accessTypeId);
        }
    }

    private void deleteGuest(final Guest mguest, final Door mdoor, final int accessTypeId) {
        Utils u = new Utils();
        u.requestType = Utils.KEY_REQ;
        u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
        u.requestDirection = Utils.TCP_SEND_PACKET;

        byte[] packet = new byte[MAX_PKT_SIZE];
        packet[REQUEST_PACKET_TYPE_POS] = Utils.KEY_REQ;
        packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
        packet[REQUEST_PACKET_LENGTH_POS] = DeleteGuestPacket.SENT_PACKET_LENGTH_DELETE_SELECTED_GUEST;

        byte[] guestMac = u.getMacIdInHex(mguest.getId());
        System.arraycopy(guestMac, 0, packet, DeleteGuestPacket.GUEST_MAC_START, guestMac.length);
        if(appContext.getConnectionMode()== ConnectionMode.CONNECTION_MODE_REMOTE){
            byte[] en_packet = Utils.encriptData(packet);
            mqttDataSendListener.sendData(en_packet, "", "",MqttInterface.DEFAULT_WAIT_TIME, new MqttInterface() {
                @Override
                public void dataAvailable(byte[] data) {
                    processDeletePacket(Utils.getPacketData(data), mguest,mdoor,accessTypeId);
                }

                @Override
                public void timeOutError() {
                    Toast.makeText(RegisterGuestActivity.this, "Please try again", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void unableToSubscribe() {

                }

                @Override
                public void succOrFailToUnSubscribe() {

                }
            });
        }else{
            mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                @Override
                public void onDataAvailable(String data) {
                    Log.d(TAG, data);
                    processDeletePacket(data, mguest,mdoor,accessTypeId);
                }
            },null);
        }
    }

    private void processDeletePacket(String packet, Guest mguest, Door mdoor, int accessTypeId){
        DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
        if (packet != null && packet.length() >= Packet.DeleteGuestPacket.RECEIVED_PACKET_LENGTH_DELETE_GUEST){
            if (packet.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.KEY_REQ &&
                    packet.charAt(RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                    Log.d(TAG, "Delete guest" + databaseHandler.delete(mguest, mdoor));
                    doGuestRegistration(accessTypeId);
                }else if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                    Log.e(TAG, "Unable to delete guest");
                }
            }
        }else {
            String message = null;
            if (packet != null) {
                message = CommunicationError.getMessage(packet.charAt(RESPONSE_COMMAND_STATUS_POS));
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
        switch(requestCode){
            case RESULT_SELECT_IMAGE_CODE:
                if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                    try{
                        Uri uri = data.getData();
                        CropImage.activity(uri)
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .setAspectRatio(1,1)
                                .start(this);
                    }catch(Exception e) {
                        e.printStackTrace();
                        Intent returnFromGalleryIntent = new Intent();
                        setResult(RESULT_CANCELED, returnFromGalleryIntent);
                    }
                } else {
                    Log.i("OwnerProfileActivity", "RESULT_CANCELED");
                    Intent returnFromGalleryIntent = new Intent();
                    setResult(RESULT_CANCELED, returnFromGalleryIntent);
                }
                break;

            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK && data != null){
                    try {
                        CropImage.ActivityResult result = CropImage.getActivityResult(data);
                        Uri resultUri = result.getUri();
                        InputStream inputStream = getContentResolver().openInputStream(resultUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        drawImage(photoImageView,bitmap);
                        drawImage(photoImageView, bitmap);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(logoutBroadcastReceiver);
        Runtime.getRuntime().gc();
        super.onDestroy();
    }

    public void onStart()
    {
        super.onStart();
        registerReceiver(logoutBroadcastReceiver, intentFilter);
    }

    public void onResume()
    {
        super.onResume();
        if(sessionManager.verify()){
            finish();
        }
    }

    public static void setOnUpdateListener(OnUpdateListener onUpdateListener){
        //public static final String ARGUMENT_FOR_GUEST = "doorMode";
        Log.d(TAG, "setOnUpdateListener: ");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        /*MenuInflater inflater = getMenuInflater();
          inflater.inflate(R.menu.edit_profile_menu, menu);
          menu.findItem(R.id.changePin).setVisible(false);*/
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
                /*    case R.id.saveProfile:
                doGuestRegistration(accessTypeId);
                break;
            case R.id.developer:
                testGuestsIndex = 0;
                doGuestRegistrationInDeveloperMode(testGuestsIndex);
                break;*/
        }
        return true;
    }

    //byte[] regPacket1 = new byte[MAX_PKT_SIZE];
    private final byte[] regPacket1 = {(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
            (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,
            (byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF};

    private void doGuestRegistration(int accessTypeId)
    {
        Log.d("RegisterGuestActivity","Registering Guest:"+accessTypeId);
        boolean isValidMac = true;
        boolean isValidDate = true;
        boolean isValidStartDate = true;
        DatabaseHandler db = new DatabaseHandler(mContext);
        int j = 1;
        String errMsg;
        guest = new Guest();
        door = new Door();
        guest.setAccessMode("Guest");

        if (appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {
            Utils u = new Utils();
            u.requestType = Utils.KEY_REQ;
            u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
            u.requestDirection = Utils.TCP_SEND_PACKET;

            regPacket1[REQUEST_PACKET_TYPE_POS] = Utils.KEY_REQ;
            regPacket1[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
            regPacket1[REQUEST_PACKET_LENGTH_POS] = RegisterGuestPacket.SENT_PACKET_LENGTH_ADD_GUEST_2;
            String doorID = appContext.getDoor().getId();
            if(doorID != null) {
                door.setId(doorID);
                door.status = Door.KEY_SHARED;
                Door d = db.getDoor(doorID);
                if(d != null) {
                    String s = d.getName();
                    door.setName(s);
                }
            }
            String name;
            if(requestCode == Utils.GUEST_EDIT_PROFILE_CODE){
                name=UpdateName.getText().toString();
            }else {
                name = this.name.getText().toString();
            }

            guest.setName(name);
            for (int i = 0; i < name.length(); i++) {
                regPacket1[i + RegisterGuestPacket.GUEST_NAME_START] = (byte) name.charAt(i);
                if(i == name.length() - 1){
                    regPacket1[i+1] += '\0';
                }
            }

            String guestKey = phoneMac.getText().toString();

            Log.d(TAG,"Guest IMEI:"+guestKey);
            Log.d(TAG,"Request Code:"+requestCode);

            if(requestCode == Utils.GUEST_EDIT_PROFILE_CODE){
                //guest.setImage(GuestDetailsFragment.guest.getImage());
                Bitmap bitmap = ((BitmapDrawable)photoImageView.getDrawable()).getBitmap();
                guest.setImage(bitmap);
                //guest.setId(guestMac);
            }else {
                Bitmap bitmap = ((BitmapDrawable)photoImageView.getDrawable()).getBitmap();
                guest.setImage(bitmap);
            }
            /*else {
                guest.setId(Utils.getUserId(guestMac));
            }*/
            guest.setId(guestKey);

            char access = ((accessTypeId == 0) ? Utils.LIMITED_TIME_ACCESS : Utils.FULL_TIME_ACCESS);
            guest.setAccessType(Utils.accessTypeList.get(accessTypeId));
            regPacket1[RegisterGuestPacket.ACCESS_TYPE_POSITION] = (byte) access;
            if(access == Utils.LIMITED_TIME_ACCESS) {
                String startDateTime, endDateTime;
                Log.d("RegisterGuestActivity", "Guest Access Type:" + guest.getAccessType());
                startDateTime = DateTimeFormat.getDate(startDateToSend[0], startDateToSend[1],
                        (startDateToSend[2] * 100 + startDateToSend[3]), 3) + " " + startTime24;
                endDateTime = DateTimeFormat.getDate(endDateToSend[0], endDateToSend[1],
                        (endDateToSend[2] * 100 + endDateToSend[3]), 3) + " " + endTime24;
                Log.d(TAG, "doGuestRegistration: startDateTime "+startDateTime+" endDateTime "+endDateTime);
                guest.setAccessStartDateTime(startDateTime);
                guest.setAccessEndDateTime(endDateTime);

                for (int i = 0; i < startDateToSend.length; i++) {
                    regPacket1[i + RegisterGuestPacket.START_DATE_OF_ACCESS_START] = (byte) startDateToSend[i];
                }
                for (int i = 0; i < startTimeToSend.length; i++) {
                    regPacket1[i + RegisterGuestPacket.START_TIME_OF_ACCESS_START] = (byte) startTimeToSend[i];
                }
                for (int i = 0; i < endDateToSend.length; i++) {
                    regPacket1[i + RegisterGuestPacket.END_DATE_OF_ACCESS_START] = (byte) endDateToSend[i];
                }
                for (int i = 0; i < endTimeToSend.length; i++) {
                    regPacket1[i + RegisterGuestPacket.END_TIME_OF_ACCESS_START] = (byte) endTimeToSend[i];
                }
            }

             //String contact =this.phone.getText().toString();
             //guest.setPhone(contact);
            boolean isFieldEmpty = name.isEmpty() || guestKey.isEmpty();
            boolean isValidKey = guestKey.length() == 12;
            if(requestCode != Utils.GUEST_EDIT_PROFILE_CODE && (isFieldEmpty || !isValidKey)){
                new AlertDialog.Builder(mContext)
                        .setTitle("Validation Failed")
                        .setMessage(Html.fromHtml("<font color='#FF0000'>"+(j++)+". Fields cannot be empty<br/>"+j+". Invalid Key</font>"))
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
            }
            else {
                errMsg = "";
                // convert mac in to byte
                Log.d("RegisterGuestActivity", "Guest MAC:" + guestKey);
                try {
                    byte[] idHex = Utils.toByteArray(guest.getId());
                    Log.d(TAG,"Guest UserId:");
                    Utils.printByteArray(idHex);
                    System.arraycopy(idHex, 0, regPacket1, RegisterGuestPacket.PHONE_MAC_ID_START, Utils.PHONE_MAC_ID_LEN_IN_HEX);
                } catch (Exception e) {
                    isValidMac = false;
                    //Toast.makeText(mContext, "Enter a valid MAC address", Toast.LENGTH_LONG).show();
                    errMsg += ". Enter a valid MAC address.<br/>";
                }

                /*if (!Utils.isValidEmailAddress(email) || !contact.contains("+")) {
                    isValidated = false;
                    if (!Utils.isValidEmailAddress(email)) {
                        errMsg += (j++) + ". Email address is not valid.<br/>";
                    }
                    if (!contact.contains("+")){
                        errMsg += (j++) + ". Contact number is not valid.<br/>";
                    }
                } else */
                Log.d(TAG, "doGuestRegistration: "+guest.getAccessStartDateTime());
                try {
                    isValidDate = guest.getAccessType().equalsIgnoreCase("Full Time") || (guest.getAccessType().equalsIgnoreCase("Limited Time")
                            && (Objects.requireNonNull(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).parse(guest.getAccessStartDateTime())).compareTo
                            (new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).parse(guest.getAccessEndDateTime())) < 0));
                    Log.d("RegisterGuestActivity", "isValidDate:" + isValidDate);
                    if(!isValidDate){
                        errMsg += ". End date must be greater than start date.<br/>";
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                try {
                    Date date = new Date();
                    SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm") ;
                    String d = dateFormat1.format(date);
                    isValidStartDate = (Objects.requireNonNull(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).parse(guest.getAccessStartDateTime())).compareTo
                            (new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).parse(d)) > 0);

                    if (!isValidStartDate){
                        errMsg += "Start date and time must be greater than current date and time.<br/>";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (isValidStartDate && isValidMac && isValidDate) {
                    u.commandDetails = new String(regPacket1, StandardCharsets.ISO_8859_1);
                    Log.d("RegisterGuestActivity", "Sending Packet:" + u.commandDetails);
                    u.setUtilsInfo(u);

                    if(appContext.getConnectionMode()== ConnectionMode.CONNECTION_MODE_REMOTE){
                        byte[] packet = Utils.encriptData(regPacket1);
                        mqttDataSendListener.sendData(packet, "", "",MqttInterface.DEFAULT_WAIT_TIME, new MqttInterface() {
                            @Override
                            public void dataAvailable(byte[] data) {
                                packet2= Utils.getPacketData(data);
                                Utils.printByteArray(data);
                                Log.d(TAG, "internet guest add: "+Utils.getStringFromHex(packet2));
                                processRegistrationPacket(guest, door);
                            }

                            @Override
                            public void timeOutError() {
                                Toast.makeText(RegisterGuestActivity.this, "Please try again", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void unableToSubscribe() {

                            }

                            @Override
                            public void succOrFailToUnSubscribe() {

                            }
                        });
                    }else{
                        mOnDataSendListener.onSend(regPacket1, new OnDataAvailableListener() {
                            @Override
                            public void onDataAvailable(String data) {
                                packet2=data;
                                Log.d(TAG, "ble guest add: "+Utils.getStringFromHex(packet2));
                                processRegistrationPacket(guest, door);
                            }
                        },"Registering Guest...");
                    }
                } else {
                    new AlertDialog.Builder(mContext).setTitle("Validation Failed")
                            .setMessage(Html.fromHtml("<font color='#FF0000'>"+errMsg+"</font>"))
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    }).create().show();
                }
            }
        }
        db.close();
    }

   /* private void runTcpClientAsService() {
        //Log.d(TAG, "Calling MessagingService:"+MainActivity.routerInfo.getAddress()+" "+MainActivity.routerInfo.getPort());
        *//*Intent lIntent = new Intent(this, MessagingService.class);
        //lIntent.putExtra(MessagingService.EXTRAS_GROUP_OWNER_ADDRESS, MainActivity.routerInfo.getAddress());
        //lIntent.putExtra(MessagingService.EXTRAS_GROUP_OWNER_PORT, MainActivity.routerInfo.getPort());
        lIntent.putExtra(MessagingService.EXTRAS_GROUP_OWNER_ADDRESS, "103.39.241.18");
        lIntent.putExtra(MessagingService.EXTRAS_GROUP_OWNER_PORT, 2014);
        startService(lIntent);
        Log.d(TAG, "Exiting MessagingService: "+lIntent+"Context:"+mContext);*//*
    }
*/
   private void processRegistrationPacket(Guest guest, Door door)
    {
        Intent returnIntent = new Intent();
        //Utils u = new Utils();
        Log.d("RegisterGuestActivity", "Packet :" + packet2);
        if(packet2 != null) {
            //byte [] strBytes2;
            try {
                //strBytes2 = packet2.getBytes("ISO-8859-1");
                   if(packet2.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.KEY_REQ
                            && Utils.parseInt(packet2,RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                            if(packet2.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                                DatabaseHandler db = new DatabaseHandler(mContext);
                                Log.d(TAG, "guestId:"+guest.getId()+" [exist="+db.isExist(guest.getId())+"]");
                                if((!new DatabaseHandler(mContext).isExist(guest.getId()))) {
                                    if(db.insert(guest) && new DatabaseHandler(mContext).registerDoor(guest, door)) {
                                        //new AlertDialog.Builder(mContext).setMessage("Owner is Successfully Registered").create().show();
                                        //Toast.makeText(mContext, "Guest is Successfully Registered", Toast.LENGTH_LONG).show();
                                        new DatabaseHandler(mContext).updateKeyStatus(guest.getId(), door.getId());
                                        returnIntent.putExtra(ARGUMENT_FROM_GUEST_LIST_ACTIVITY, Utils.SUCCESS);
                                        setResult(RESULT_OK, returnIntent);
                                        finish();

                                        /* Send sms is commented from 19-Nov-2018
                                        *  because google stope the service of SMS and Call Log in android
                                        *  Message :- you are registered as guest at (device name)
                                        *  phone no-guest.getPhone(),device name-door.getName()
                                        * */
                                       /* SmsManager smsManager = SmsManager.getDefault();
                                        smsManager.sendTextMessage(guest.getPhone(), "AzLock", "you are registered as guest at "+ door.getName(), null, null);*/


                                       //mRegistrationListener.onRegistrationSuccess(Utils.GUEST_EDIT_PROFILE_CODE);
                                    }
                                    else {
                                        //Log.d("RegisterGuestActivity", "[Error]: Guest's doorMode cannot be inserted due to DB error");
                                        //mRegistrationListener.onRegistrationFailure(Utils.GUEST_EDIT_PROFILE_CODE, Utils.DATABASE_ERROR_CODE);
                                        returnIntent.putExtra(ARGUMENT_FROM_GUEST_LIST_ACTIVITY, Utils.FAILURE);
                                        setResult(RESULT_OK, returnIntent);
                                        finish();
                                    }
                                }
                                else {
                                    if(db.update(guest)) {
                                        if(!new DatabaseHandler(mContext).isRegistered(guest, door)) {
                                            new DatabaseHandler(mContext).registerDoor(guest, door);
                                        }
                                        new DatabaseHandler(mContext).updateKeyStatus(guest.getId(), door.getId());
                                        returnIntent.putExtra(ARGUMENT_FROM_GUEST_LIST_ACTIVITY, Utils.SUCCESS);
                                        setResult(RESULT_OK, returnIntent);
                                        finish();
                                        //mRegistrationListener.onRegistrationSuccess(Utils.GUEST_EDIT_PROFILE_CODE);
                                    }
                                    else {
                                        Log.d("RegisterGuestActivity", "[Error]: Guest's doorMode cannot be inserted due to DB error");
                                        //mRegistrationListener.onRegistrationFailure(Utils.GUEST_EDIT_PROFILE_CODE, Utils.DATABASE_ERROR_CODE);
                                        returnIntent.putExtra(ARGUMENT_FROM_GUEST_LIST_ACTIVITY, Utils.DATABASE_ERROR_CODE);
                                        setResult(RESULT_OK, returnIntent);
                                        finish();
                                    }
                                }
                                db.close();
                            }
                            else if(packet2.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                                returnIntent.putExtra(ARGUMENT_FROM_GUEST_LIST_ACTIVITY, Utils.DEVICE_ERROR_CODE);
                                setResult(RESULT_OK, returnIntent);
                                finish();
                                //mRegistrationListener.onRegistrationFailure(Utils.GUEST_EDIT_PROFILE_CODE, Utils.DEVICE_ERROR_CODE);
                            }

                    } else {
                        Log.e("CMD_ERR", "Error Message will be added");
                        //Toast toast = Toast.makeText(mContext, "CMD_ERR", Toast.LENGTH_LONG);
                        //toast.show();
                        returnIntent.putExtra(ARGUMENT_FROM_GUEST_LIST_ACTIVITY, Utils.FAILURE);
                        setResult(RESULT_OK, returnIntent);
                    }
            } catch(Exception e) {
                Log.e("RegisterGuestActivity", "Unsupported String Decoding Exception");
            }
        } else {
            Toast toast = Toast.makeText(mContext, "Invalid or Null DoorMode", Toast.LENGTH_LONG);
            toast.show();
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        finish();
        super.onBackPressed();
    }

    private void drawImage(ImageView imageView, Bitmap bm)
    {
        RoundedImageView roundedImageView = new RoundedImageView(mContext);
        Bitmap conv_bm = roundedImageView.getCroppedBitmap(bm, 250, 7, Color.WHITE);
        imageView.setImageBitmap(conv_bm);
    }
}

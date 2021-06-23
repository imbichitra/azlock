package com.asiczen.azlock;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.asiczen.azlock.content.MySharedPreferences;
import com.asiczen.azlock.net.VolleyErrors;
import com.asiczen.azlock.net.VolleyRequest;
import com.asiczen.azlock.net.VolleyResponse;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.azlock.app.CommunicationError;
import com.asiczen.azlock.app.DeviceStatus;
import com.asiczen.azlock.app.Validate;
import com.asiczen.azlock.app.model.Door;
import com.asiczen.azlock.app.model.Owner;
import com.asiczen.azlock.app.model.WifiNetwork;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.ConcreteValidator;
import com.asiczen.azlock.content.CustomTextWatcher;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.content.LogoutBroadcastReceiver;
import com.asiczen.azlock.content.SessionManager;
import com.asiczen.azlock.content.Validator;
import com.asiczen.azlock.net.OnDataAvailableListener;
import com.asiczen.azlock.net.OnDataSendListener;
import com.asiczen.azlock.util.HttpAsyncTask;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static com.asiczen.azlock.net.VolleyRequest.STATUS;
import static com.asiczen.azlock.net.VolleyRequest.STATUS_SUCCESS;


/**
 * Created by Somnath on 12/8/2016.
 */

public class RegisterOwnerActivity extends AppCompatActivity implements Packet,HttpAsyncTask.AsyncResponse {

    private final String URL_POST=AppContext.getIp_address()+AppContext.getCreate_url();
    private Context mContext;
    private AppContext appContext;
    private SessionManager sessionManager;
    private Intent returnIntent;
    private IntentFilter intentFilter;
    private LogoutBroadcastReceiver logoutBroadcastReceiver;
    private final String TAG = RegisterOwnerActivity.class.getSimpleName();
    private Owner owner;
    private Door door;
    private String message;
    private static OnDataSendListener mOnDataSendListener;
    public static final String DOOR_ID_EXTRA="com.asiczen.blelock.DOOR_ID_EXTRA";
    public static final String REGISTRATION_SUCCESS_EXTRA="com.asiczen.blelock.registration";
    public static final String RENAME_SUCCESS_EXTRA="com.asiczen.blelock.rename";
    private EditText ownerNameTextView, doorNameTextView;
    private TextInputLayout inputLayoutName, inputLayoutDoorName;
    //private ImageView lockLogoImageView;
    private LinearLayout uiStatusLinearLayout;
    private ProgressBar uiStatusProgressBar;
    private TextView uiStatusTextView;
    private Validator validator;
    private byte[] packet;
    private TextView dialogTextView;
    private AlertDialog dialog;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_owner_2);
        mContext = this;
        appContext = AppContext.getContext();
        if(!appContext.isConnected()){
            finish();
        }
        MySharedPreferences sharedPreferences = new MySharedPreferences(this);
        sessionManager = new SessionManager(this);
        logoutBroadcastReceiver = new LogoutBroadcastReceiver(this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(SessionManager.ACTION_DISCONNECTED);
        intentFilter.addAction(SessionManager.ACTION_EXIT);
        returnIntent = new Intent();
        Intent intent=getIntent();
        message = null;
        ownerNameTextView =  findViewById(R.id.name_editText);
        doorNameTextView =  findViewById(R.id.door_name_editText);
        inputLayoutName =  findViewById(R.id.input_layout_name);
        inputLayoutDoorName =  findViewById(R.id.input_layout_door_name);

        //lockLogoImageView =  findViewById(R.id.lock_logo);

        uiStatusLinearLayout= findViewById(R.id.registering_progress_linearLayout);
        uiStatusProgressBar= findViewById(R.id.progressBar2);
        uiStatusTextView= findViewById(R.id.textView4);
        uiStatusProgressBar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);

        validator = new ConcreteValidator(this);
        ownerNameTextView.addTextChangedListener(new CustomTextWatcher(this, ownerNameTextView, inputLayoutName, validator));
        doorNameTextView.addTextChangedListener(new CustomTextWatcher(this, doorNameTextView, inputLayoutDoorName, validator));

        Activity activity = getParent();
        mOnDataSendListener=appContext.getOnDataSendListener();

        owner=new Owner();
        owner.setId(sharedPreferences.getMac());
        owner.setAccessMode("owner");

        door=new Door();
        door.setId(intent.getStringExtra(DOOR_ID_EXTRA));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View bridgeConnectView= getLayoutInflater().inflate(R.layout.progressbar, Utils.nullParent,false);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setView(bridgeConnectView);
        dialogTextView=bridgeConnectView.findViewById(R.id.progressDialog);
        dialog = builder.create();
    }

    public void onClickRegisterOwnerButton(View v){
        if(!validator.validate(Validate.NAME, inputLayoutName, ownerNameTextView))
        {
            return;
        }
        if(!validator.validate(Validate.DOOR_NAME, inputLayoutDoorName, doorNameTextView))
        {
            return;
        }
        String ownerName=ownerNameTextView.getText().toString().trim();
        String doorName=doorNameTextView.getText().toString().trim();

        if(!ownerName.isEmpty() && !doorName.isEmpty()) {
            if(doorName.trim().toLowerCase().equals("azlock")){
                doorNameTextView.setError("Any combination of azlock is not allowed as door name");
            }else if(doorName.length()>8){
                doorNameTextView.setError("Door name must be less then 8");
            }
            else {
                owner.setName(ownerName);
                door.setName(doorName);
                register();
            }
        }
    }

    private void setUiProgressStatus(int visibility){
        switch (visibility){
            case View.VISIBLE:
                uiStatusLinearLayout.setVisibility(View.VISIBLE);
                uiStatusProgressBar.setVisibility(View.VISIBLE);
                uiStatusTextView.setVisibility(View.VISIBLE);
                break;

            case View.INVISIBLE:
                uiStatusLinearLayout.setVisibility(View.INVISIBLE);
                uiStatusProgressBar.setVisibility(View.INVISIBLE);
                uiStatusTextView.setVisibility(View.INVISIBLE);
                break;
        }
    }

    @Override
    protected void onResume()
    {
        if(!appContext.isConnected()){
            finish();
        }
        super.onResume();
    }

    @Override
    protected void onStart()
    {
        registerReceiver(logoutBroadcastReceiver, intentFilter);
        super.onStart();
    }

    @Override
    protected void onDestroy()
    {
        unregisterReceiver(logoutBroadcastReceiver);
        super.onDestroy();
    }

    private void register()
    {
        Log.d("RegisterFrag", "ConnectActivity.deviceStatus:"+appContext.getDeviceStatus());
        if(appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {
            Utils u = new Utils();
            u.requestType = Utils.OWNER_REQUEST;
            u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
            u.requestDirection = Utils.TCP_SEND_PACKET;
            packet = new byte[MAX_PKT_SIZE];
            packet[REQUEST_PACKET_TYPE_POS] = Utils.OWNER_REQUEST;
            packet[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_VISITOR;
            packet[REQUEST_PACKET_LENGTH_POS] = Packet.OwnerRegistrationPacket.SENT_PACKET_LENGTH;

            byte[] ownerMacId = new Utils().getMacIdInHex(owner.getId());
            System.arraycopy(ownerMacId, 0, packet, 3, ownerMacId.length);
            for (int i = 0; i < owner.getName().length() && i < (OwnerRegistrationPacket.DELETE_FLAG - OwnerRegistrationPacket.OWNER_NAME_START); i++) {
                packet[i + Packet.OwnerRegistrationPacket.OWNER_NAME_START] = (byte) owner.getName().charAt(i);
                if (i == owner.getName().length() - 1) {
                    packet[i + 1] += '\0';
                }
            }

            /* use the current timestamp as reset code */
            int i = (int) (new Date().getTime() / 1000);
            packet[Packet.OwnerRegistrationPacket.RESET_CODE_POS] = (byte) (i >> 24);
            packet[Packet.OwnerRegistrationPacket.RESET_CODE_POS + 1] = (byte) (i >> 16);
            packet[Packet.OwnerRegistrationPacket.RESET_CODE_POS + 2] = (byte) (i >> 8);
            packet[Packet.OwnerRegistrationPacket.RESET_CODE_POS + 3] = (byte) (i);

            /* send data to server and proceed only if got a response */
            Log.d(TAG, "Registration of door ID =" + door.getId());
            HttpAsyncTask.context=this;

            JSONObject obj = new JSONObject();
            try {
                obj.put("mac_id", ""+door.getId());
                obj.put("reset_no", ""+ i);

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (!owner.getName().isEmpty()){
                /*HttpAsyncTask httpTask = new HttpAsyncTask();
                httpTask.delegate = this;
                httpTask.execute(URL_POST,obj.toString());*/
                dialogTextView.setText(R.string.connecting);
                dialog.show();
                VolleyRequest.jsonObjectRequest(this, URL_POST, obj, Request.Method.POST, new VolleyResponse() {
                    @Override
                    public void VolleyError(VolleyError error) {
                        if (dialog!=null && dialog.isShowing())
                            dialog.dismiss();
                        Toast.makeText(RegisterOwnerActivity.this, VolleyErrors.error(error), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void VolleyObjectResponse(JSONObject response) {
                        Log.d(TAG, "VolleyObjectResponse: "+response.toString());
                        if (dialog!=null && dialog.isShowing())
                            dialog.dismiss();
                        try {
                            if (response.getString(STATUS).equals(STATUS_SUCCESS)){
                                setUiProgressStatus(View.VISIBLE);
                                mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                                    @Override
                                    public void onDataAvailable(String data) {
                                        setUiProgressStatus(View.INVISIBLE);
                                        Utils.printByteArray(data.getBytes(StandardCharsets.ISO_8859_1));
                                        processRegisterPacket(data);
                                    }
                                }, "Registering...");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
            else{
                Toast.makeText(mContext, "Enter valid name", Toast.LENGTH_LONG).show();
            }
        }
        else {
            Log.d("OwnerRegisterFrag", "Device Not Connected");
            Toast.makeText(mContext, "No connected device found", Toast.LENGTH_LONG).show();
        }
    }

    private void processRegisterPacket(String receivedPacket)
    {
        //Utils u = new Utils();
        Log.d("OwnerProfileActivity", "Processing received packet:" + receivedPacket);
        if(receivedPacket != null && receivedPacket.length() >= OwnerRegistrationPacket.RECEIVED_PACKET_LENGTH) {
            //byte[] strBytes;

            try {
                //strBytes = receivedPacket.getBytes("ISO-8859-1");

                    if(receivedPacket.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.OWNER_REQUEST) {
                        Log.d(TAG, "RegistrationPacket Type OK");
                        Log.d("OwnerProfileActivity", "RegistrationPacket/COMMAND_STATUS_POS: "+
                                Utils.parseInt(receivedPacket,RESPONSE_COMMAND_STATUS_POS));
                        if (Utils.parseInt(receivedPacket,RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                            Log.d("OwnerProfileActivity", "RegistrationPacket/COMMAND_STATUS_POS"+
                                    Utils.parseInt(receivedPacket,RESPONSE_COMMAND_STATUS_POS));

                            if (receivedPacket.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                                Log.d(TAG, "RegistrationPacket SUCCESS Updating Code:"+owner+"\n"+door);
                                boolean x=new DatabaseHandler(mContext).insert(owner);
                                boolean y=new DatabaseHandler(mContext).insert(door);
                                boolean z=new DatabaseHandler(mContext).registerDoor(owner, door);
                                if (x && y && z) {
                                    appContext.setDoor(door);
                                    appContext.setUser(owner);
                                    returnIntent.putExtra(REGISTRATION_SUCCESS_EXTRA, Utils.SUCCESS);
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            setAjar();
                                            //renameDoor();

                                        }
                                    }, 500);

                                } else {
                                    Log.d("OwnerProfileActivity", "[Error]: Owner's doorMode cannot be inserted due to DB error");
                                    returnIntent.putExtra(REGISTRATION_SUCCESS_EXTRA, Utils.DATABASE_ERROR_CODE);
                                    setResult(RESULT_CANCELED, returnIntent);
                                    finish();
                                }
                            } else if (receivedPacket.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                                Log.d("OwnerProfileActivity", "[Error]: Owner's doorMode cannot be inserted due to Device error");
                                returnIntent.putExtra(REGISTRATION_SUCCESS_EXTRA, Utils.DEVICE_ERROR_CODE);
                                setResult(RESULT_CANCELED, returnIntent);
                                finish();
                            }
                        } else {
                            String errorMessage = CommunicationError.getMessage(receivedPacket.charAt(RESPONSE_COMMAND_STATUS_POS));
                            Log.d("CMD_STS_ERR", errorMessage);
                            Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
                            returnIntent.putExtra(REGISTRATION_SUCCESS_EXTRA, Utils.DEVICE_ERROR_CODE);
                            setResult(RESULT_CANCELED, returnIntent);
                            finish();
                        }
                    } else {
                        Log.d(TAG, "RESPONSE_PACKET_TYPE_ERROR");
                        returnIntent.putExtra(REGISTRATION_SUCCESS_EXTRA, Utils.DEVICE_ERROR_CODE);
                        setResult(RESULT_CANCELED, returnIntent);
                        finish();
                    }
            } catch (Exception e) {
                Log.d("OwnerProfileActivity", "Unsupported String Decoding Exception");
                returnIntent.putExtra(REGISTRATION_SUCCESS_EXTRA, Utils.DEVICE_ERROR_CODE);
                setResult(RESULT_CANCELED, returnIntent);
                finish();
            }
        } else {
            Toast toast = Toast.makeText(mContext, "Invalid or Null DoorMode", Toast.LENGTH_LONG);
            toast.show();
            Log.d("OwnerProfileActivity", "Packet Received"+receivedPacket);
            returnIntent.putExtra(REGISTRATION_SUCCESS_EXTRA, Utils.DEVICE_ERROR_CODE);
            setResult(RESULT_CANCELED, returnIntent);
            finish();
        }
    }
    private void setAjar(){

        if(appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {
            appContext.setAjarStatus(Utils.AJAR_STATUS);
            appContext.setAutolockStatus(Utils.AJAR_C);
            appContext.setAutolockTime(4);
            byte[] packet = new byte[MAX_PKT_SIZE];
            packet[REQUEST_PACKET_TYPE_POS] = Utils.AJAR_REQUEST;
            packet[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_OWNER;
            packet[REQUEST_PACKET_LENGTH_POS] = AjarPacket.SENT_PACKET_LENGTH;
            packet[AjarPacket.AJAR_STATUS_POSITION] = (byte) appContext.getAjarStatus();
            packet[AjarPacket.AUTOLOCK_STATUS_POSITION] = (byte) appContext.getAutolockStatus();
            packet[AjarPacket.AUTOLOCK_TIME_POSITION] = (byte) appContext.getAutolockTime();
            Log.d(TAG, "setAjar: start ========================");
            for (byte b : packet) {
                String st = String.format("%02X", b);
                System.out.print(st);
            }
            Log.d(TAG, "setAjar: end ========================");

            mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                @Override
                public void onDataAvailable(String data) {
                    Log.d(TAG, "receivedData:"+data);
                    //appContext.setAjarStatus('1');
                    if (data.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                        renameDoor();
                    }
                }
            },("Enabling Ajar..."));
        }
    }
    private void renameDoor()
    {
        if(appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {
            Utils u = new Utils();
            u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
            u.requestDirection = Utils.TCP_SEND_PACKET;

            byte[] packet = new byte[MAX_PKT_SIZE];
            Log.d(TAG, "Renaming Door...");

            message = "Renaming Door...";
            u.requestType = Utils.RENAME_DOOR_REQUEST;
            packet[REQUEST_ACCESS_MODE_POS] = Utils.APP_MODE_OWNER;
            packet[REQUEST_PACKET_TYPE_POS] = Utils.RENAME_DOOR_REQUEST;
            packet[REQUEST_PACKET_LENGTH_POS] = DoorSettingsPacket.SENT_PACKET_LENGTH;

            for(int i = 0; i < door.getName().length(); i++) {
                packet[DoorSettingsPacket.DOOR_NAME_START + i] = (byte) door.getName().charAt(i);
            }


            if(!door.getName().isEmpty()) {
                u.commandDetails = new String(packet, StandardCharsets.ISO_8859_1);
                Log.d("OwnerRegisterFrag", "Sent Packet:" + u.commandDetails);
                Log.d(TAG, "renameDoor: start ========================");
                for (byte b : packet) {
                    String st = String.format("%02X", b);
                    System.out.print(st);
                }
                Log.d(TAG, "renameDoor: end ========================");
                setUiProgressStatus(View.VISIBLE);
                mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                    @Override
                    public void onDataAvailable(String data) {
                        setUiProgressStatus(View.INVISIBLE);
                        Utils.printByteArray(data.getBytes(StandardCharsets.ISO_8859_1));
                        processRenameDoorPacket(data);
                    }
                }, message);

            }
            else {
                Toast.makeText(mContext,"Door name is empty", Toast.LENGTH_LONG).show();
            }
        }
        else {
            Log.d("OwnerRegisterFrag", "No device connected");
        }
    }

    private void processRenameDoorPacket(String packet)
    {
        //Utils u = new Utils();
        Log.d("OwnerRegisterFrag", "Received Packet:" + packet);
        if(packet != null) {
            //byte[] strBytes;
            try {
                //strBytes = packet.getBytes("ISO-8859-1");

                    if (Utils.parseInt(packet,RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                        if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                            //String responseMsg = null;
                            DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
                            databaseHandler.update(door);
                            databaseHandler.update(new WifiNetwork(door.getName()), appContext.getDoor().getName().replace("\"", ""), WifiNetwork.UPDATE_SSID);
                            Log.d("OwnerRegFrag", "Saved Networks:\n" + databaseHandler.getNetworks().toString());
                            //responseMsg = "Door name has been changed successfully";
                            appContext.getDoor().setName(door.getName());
                            //mOnUpdateListener.onUpdate(OnUpdateListener.DOOR_NAME_UPDATED);
                            databaseHandler.close();
                            returnIntent.putExtra(RENAME_SUCCESS_EXTRA, Utils.SUCCESS);
                            setResult(RESULT_OK, returnIntent);
                            finish();
                        } else if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                            new AlertDialog.Builder(mContext)
                                    .setMessage("Error occurred while fetching details from the device.")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            //added after raise issue in not going to home page after owner registration
                                            setResult(RESULT_CANCELED, returnIntent);
                                            finish();
                                        }
                                    }).create().show();
                        }
                    }
                    else
                    {
                        Log.e("OwnerRegisterFrag", "Renaming Door/"+Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS)
                                +":"+CommunicationError.getMessage(Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS)));
                        new AlertDialog.Builder(mContext)
                                .setMessage(CommunicationError.getMessage(Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS)))
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        //added after raise issue in not going to home page after owner registration
                                        setResult(RESULT_CANCELED, returnIntent);
                                        finish();
                                    }
                                }).create().show();
                    }
            } catch(Exception e) {
                Log.d("OwnerRegisterFrag", "Unsupported String Decoding Exception");
            }
        }
        else
        {
            Log.d("OwnerRegisterFrag", "Invalid Packet");
            new AlertDialog.Builder(mContext)
                    .setMessage("Invalid Packet").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    }).create().show();
        }
        returnIntent.putExtra(RENAME_SUCCESS_EXTRA, Utils.FAILURE);
        setResult(RESULT_CANCELED, returnIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setTitle(R.string.popup_title).setMessage(R.string.popup_message)
                .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sessionManager.exit();
                    }
                })
                .setNegativeButton(R.string.popup_no, null).show();
    }

    @Override
    public void processFinish(String output,int errorCode) {
        Log.d(TAG,"response from server "+output);
        dialog.dismiss();
        if (output.equals("Y")) {
            setUiProgressStatus(View.VISIBLE);
            mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                @Override
                public void onDataAvailable(String data) {
                    setUiProgressStatus(View.INVISIBLE);
                    Utils.printByteArray(data.getBytes(StandardCharsets.ISO_8859_1));
                    processRegisterPacket(data);
                }
            }, "Registering...");

        }
        else{
            Toast.makeText(mContext, "Unable to contact server", Toast.LENGTH_LONG).show();
        }
    }

    interface OnRegistrationListener
    {
        void onRegistration(int resultCode);
    }

    private static final Handler mHandler = new Handler() {
        @Override

        //Handler events that received from BLE service
        public void handleMessage(Message msg) {
            Log.d("RegisterOwnerActivity", "mHandler/Message:"+msg);
        }
    };

}

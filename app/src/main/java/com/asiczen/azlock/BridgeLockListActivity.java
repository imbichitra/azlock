package com.asiczen.azlock;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Vibrator;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.asiczen.azlock.app.AdapterViewCode;
import com.asiczen.azlock.app.CommunicationError;
import com.asiczen.azlock.content.CustomAdapter;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.net.NetClientAsyncTask;
import com.asiczen.azlock.net.WifiBroadcastReceiver;
import com.asiczen.azlock.net.OnTaskCompleted;
import com.asiczen.azlock.util.BridgeLockUtil;
import com.asiczen.azlock.util.FileAccess;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.Utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by somnath on 28-06-2017.
 */

public class BridgeLockListActivity extends AppCompatActivity implements Packet, OnBroadcastListener, OnSearchListener<String> {

    private Context mContext;
    private WifiBroadcastReceiver wifiBroadcastReceiver;
    private NetClientAsyncTask netClientAsyncTask;
    private IntentFilter mIntentFilter;
    private ListView lockMacListView;
    private final String TAG=BridgeLockListActivity.class.getSimpleName();
    private AlertDialog addLockAlertDialog;
    private FileAccess macFileAccess;
    private String[] addedLockLists;
    private Vibrator vibrator;
    private BridgeLockUtil bridgeLockUtil;
    private ProgressDialog progressDialog;
    private List<String> bridgeLockList;
    private final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private BluetoothAdapter mBtAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WifiManager mWifiManager;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.bridge_lock_list);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
            actionBar.setTitle(Html.fromHtml("<font color='#FFFFFF'>Locks</font>"));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mContext = this;
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        wifiBroadcastReceiver = new WifiBroadcastReceiver(mWifiManager, this, this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        bridgeLockUtil=new BridgeLockUtil(mContext);
        progressDialog=new ProgressDialog(mContext);

        addedLockLists=new String[]{};
        lockMacListView = findViewById(R.id.lock_listView);
        lockMacListView.setEmptyView(findViewById(R.id.empty));
        FloatingActionButton logFilterFab = findViewById(R.id.lock_menu_fab);
        logFilterFab.setImageDrawable(ContextCompat.getDrawable(mContext, R.mipmap.ic_add_white_48dp));
        bridgeLockList=new ArrayList<>();
        updateMacList();
        Utils.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBtAdapter = Utils.bluetoothAdapter;
    }

    private void updateMacList(){
        macFileAccess=new FileAccess(mContext, Utils.LOCK_MAC_LIST_FILE);
        String readMacFile=macFileAccess.read();
        if(macFileAccess.FILE_NOT_FOUND || readMacFile==null){
            //addedLockLists=new String[]{};
            Log.d(TAG, "updateMacList: ");
        }
        else {
            addedLockLists=readMacFile.split(",");
        }

        /* Arrays.asList returns fixed sized list, you cannot change the structure of list
        *  i.e. you can't add to or remove from the list. So we can't pass Arrays.asList
        *  directly to CustomeAdapter.
        * */
        if (bridgeLockList.size()>0)
            bridgeLockList.clear();
        bridgeLockList.addAll(Arrays.asList(bridgeLockUtil.getLocks()));
        Log.d(TAG, "updateMacList: "+bridgeLockList);
        CustomAdapter<String> lockMacCustomAdapter = new CustomAdapter<>(this, android.R.layout.simple_list_item_1,
                bridgeLockList, AdapterViewCode.BRIDGE_LOCK_LIST_VIEW_CODE);
        lockMacListView.setAdapter(lockMacCustomAdapter);
        lockMacCustomAdapter.notifyDataSetChanged();
        Log.d(TAG, "added lock list:"+readMacFile+" Length:"+addedLockLists.length);

        //disable the add button if 5 lock is already added
        /*if (bridgeLockList.size() == 5){
            logFilterFab.setEnabled(false);
        }else {
            logFilterFab.setEnabled(true);
        }*/
        lockMacListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                vibrator.vibrate(20);
                final int pos=position;
                new AlertDialog.Builder(mContext).setTitle("Delete Lock")
                        .setMessage("Do you want to delete selected lock (MAC:"+bridgeLockList.get(position)+")?")
                        .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                doLockDelete(bridgeLockList.get(pos));
                            }
                        }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
                return true;
            }
        });
    }

    public void onClickAddLockFab(View view){
        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }else {
            goToDeviceListActivity();
        }
        /*final ViewGroup nullParent = null;
        View addView=getLayoutInflater().inflate(R.layout.add_lock_to_bridge,nullParent,false);
        final EditText macEditText= addView.findViewById(R.id.mac_editText);
        addLockAlertDialog=new AlertDialog.Builder(mContext)
                .setTitle("Add Lock")
                .setView(addView)
                .setPositiveButton("ADD", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create();
        addLockAlertDialog.show();
        addLockAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mac=macEditText.getText().toString();
                if(!mac.isEmpty() && mac.length()==17) {
                    String verify = mac.replaceAll(":","");
                    int length = mac.length() - verify.length();
                    if(length == 5) {
                        addLockAlertDialog.dismiss();
                        doAddLock(mac);
                    }else {
                        Toast.makeText(mContext,"Please enter colon between two character",Toast.LENGTH_LONG).show();
                    }
                }
                else{
                    Toast.makeText(mContext,"Enter valid mac address",Toast.LENGTH_LONG).show();
                }
            }
        });*/
    }

    private void goToDeviceListActivity() {
        Intent intent = new Intent(this,DeviceListActivity.class);
        startActivityForResult(intent, REQUEST_SELECT_DEVICE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = data.getStringExtra(BluetoothDevice.EXTRA_NAME);
                if (deviceName != null &&  deviceName.equals("azLock")) {
                    Snackbar.make(findViewById(android.R.id.content), "azLock is not allowed to add,Please add another Lock.", Snackbar.LENGTH_LONG).show();
                } else {
                    Log.d(TAG, "onActivityResult: " + deviceAddress);
                    doAddLock(deviceAddress);
                }
            }
        }
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                goToDeviceListActivity();
            } else {
                Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void doAddLock(final String mac){
        Utils u = new Utils();
        u.requestType = Utils.ROUTER_CONFIG_REQUEST;
        u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
        u.requestDirection = Utils.TCP_SEND_PACKET;

        byte[] packet = new byte[AddDeleteLockPacket.SENT_PACKET_LENGTH];
        packet[REQUEST_PACKET_TYPE_POS] = Utils.ADD_LOCK_REQUEST;
        packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
        packet[REQUEST_PACKET_LENGTH_POS] = AddDeleteLockPacket.SENT_PACKET_LENGTH;
        packet[AddDeleteLockPacket.OPERATION_TYPE] = AddDeleteLockPacket.ADD_LOCK;

        byte[] macInHex=u.getMacIdInHex(mac);
        Log.d(TAG, "mac:"+mac+" byte arr length:"+macInHex.length);
        System.arraycopy(macInHex, 0, packet, 4, macInHex.length);
        //packet[AddDeleteLockPacket.CHECKSUM_SENT] = u.calculateChecksum(packet, true);
        u.commandDetails = new String(packet, StandardCharsets.ISO_8859_1);
        Log.d(TAG, "Sent Packet:" + u.commandDetails+" [checksum:"+packet[AddDeleteLockPacket.CHECKSUM_SENT]+"]");
        netClientAsyncTask=new NetClientAsyncTask(false,BridgeLockListActivity.this, Utils.host, 80,
                packet, new OnTaskCompleted<String>() {
            @Override
            public void onTaskCompleted(int resultCode, String value) {
                Log.d(TAG, "onTaskCompleted:"+value);

                if(resultCode== Activity.RESULT_OK) {
                    if(value!=null) {
                        Utils.printByteArray(value.getBytes(StandardCharsets.ISO_8859_1));
                        processAddLockPacket(value,mac);
                    }
                }
                else
                {
                    if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.SOCKET_NOT_CONNECTED)
                    {
                        Snackbar.make(findViewById(android.R.id.content), "Not connected", Snackbar.LENGTH_LONG).show();
                    }
                    else if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.MESSAGE_NOT_RECEIVED)
                    {
                        Snackbar.make(findViewById(android.R.id.content), "Timeout occurred", Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        });
        netClientAsyncTask.showProgressDialog(true,"Adding lock...");
        netClientAsyncTask.execute();
    }

    private void processAddLockPacket(String packet,String mac){
        Log.d(TAG, "Received Packet"+packet);
        if(packet != null && Utils.parseInt(packet, RESPONSE_PACKET_LENGTH_POS) >= AddDeleteLockPacket.RECEIVED_PACKET_LENGTH) {
             try {
                  if ((packet.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.ADD_LOCK_REQUEST)
                            && packet.charAt(RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                        if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                            if(addLockAlertDialog!=null) {
                                addLockAlertDialog.dismiss();
                            }
                            Toast.makeText(mContext,"Lock added",Toast.LENGTH_LONG).show();
                            bridgeLockUtil.add(mac);
                            updateMacList();
                        } else if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                            Toast.makeText(mContext,"Failed to add lock",Toast.LENGTH_LONG).show();
                        }
                    } else {
                      Snackbar.make(findViewById(android.R.id.content),CommunicationError.getMessage(Utils.parseInt(packet,RESPONSE_COMMAND_STATUS_POS )), Snackbar.LENGTH_LONG).show();
                    }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Invalid Packet");
            Snackbar.make(findViewById(android.R.id.content),"Invalid Packet", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onReceive(int resultCode, Object result) {
        boolean isWifiConnected;
        String connectedBSSID;

        switch (resultCode)
        {
            case OnBroadcastListener.CONNECTIVITY_CHANGED:
                isWifiConnected = (Boolean) result;
                Log.d(TAG, "isWifiConnected:"+isWifiConnected);
                if(!isWifiConnected)
                {
                    Toast.makeText(mContext,"Connection lost",Toast.LENGTH_LONG).show();
                    finish();
                }
                break;

            case OnBroadcastListener.CONNECTED_WIFI_INFO:
                connectedBSSID = (String) result;
                Log.d(TAG, "connectedBSSID:"+connectedBSSID);
                break;

            default:
                break;
        }
    }

    @Override
    protected void onPause()
    {
        unregisterReceiver(wifiBroadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver(wifiBroadcastReceiver, mIntentFilter);
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.log_menu, menu);
        MenuItem item = menu.findItem(R.id.download);
        invalidateOptionsMenu();
        item.setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        } else if (item.getItemId() == R.id.Refresh) {
            refreshLockList();
        }
        return true;
    }

    private void refreshLockList(){
        Utils u = new Utils();
        u.requestType = Utils.LOG_REQUEST;
        u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
        u.requestDirection = Utils.TCP_SEND_PACKET;
        byte[] packet = new byte[RefreshLockPacket.SENT_PACKET_LENGTH];
        packet[REQUEST_PACKET_TYPE_POS] = Utils.REFRESH_LOCK_REQUEST;
        packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
        packet[REQUEST_PACKET_LENGTH_POS] = RefreshLockPacket.SENT_PACKET_LENGTH;
        packet[RefreshLockPacket.OPERATION_TYPE] = RefreshLockPacket.REFRESH_LOCK_LIST;
        //packet[LogRequestPacket.CHECKSUM_SENT] = u.calculateChecksum(packet, true);

        NetClientAsyncTask clientAsyncTask=new NetClientAsyncTask(false,this, Utils.host, 80,packet,
                new OnTaskCompleted<String>() {
                    @Override
                    public void onTaskCompleted(int resultCode, String result) {
                        checkConfigPacketStatus(resultCode,result);
                    }
        });
        clientAsyncTask.showProgressDialog(true, "Downloading locks...");
        clientAsyncTask.execute();
       // updateMacList();
    }
    private void checkConfigPacketStatus(int resultCode, String value){
        if(resultCode== Activity.RESULT_OK) {
            if(value!=null) {
               /* try {
                    Utils.printByteArray(value.getBytes("ISO-8859-1"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }*/
                processRefreshPacket(value);
            }
        }
        else
        {
            if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.SOCKET_NOT_CONNECTED)
            {
                Snackbar.make(findViewById(android.R.id.content), "Not connected", Snackbar.LENGTH_LONG).show();
            }
            else if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.MESSAGE_NOT_RECEIVED)
            {
                Snackbar.make(findViewById(android.R.id.content), "Timeout occurred", Snackbar.LENGTH_LONG).show();
            }
        }
    }
    private void processRefreshPacket(String packet){
        Log.d(TAG, "Received Packet" + packet);
        if (packet != null && Utils.parseInt(packet, RESPONSE_PACKET_LENGTH_POS) >= RefreshLockPacket.RECEIVED_PACKET_LENGTH_MIN) {
            int len = Utils.parseInt(packet, RESPONSE_PACKET_LENGTH_POS);
            Log.d(TAG, "Length:" + len);
            try {
                if ((packet.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.REFRESH_LOCK_REQUEST)
                        && packet.charAt(RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                    if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                        //int numberOfLocks = Character.getNumericValue(packet.charAt(RefreshLockPacket.NUMBER_OF_LOCKS));
                        int numberOfLocks = packet.charAt(RefreshLockPacket.NUMBER_OF_LOCKS);
                        Log.d(TAG, "numberOfLocks:" + numberOfLocks);
                        if (numberOfLocks > 0) {
                            int lockMacIdStart = RefreshLockPacket.LOCK_MAC_ID_START;
                            StringBuilder lockList = new StringBuilder();
                            for (int i = 0; i < numberOfLocks; i++, lockMacIdStart += RefreshLockPacket.LOCK_MAC_ID_SIZE) {
                                String lockMacId = Utils.getStringFromHex(packet.substring(lockMacIdStart,
                                        lockMacIdStart + RefreshLockPacket.LOCK_MAC_ID_SIZE));
                                lockMacId = Utils.generateMac(lockMacId); // add ":" in mac id
                                if (i < numberOfLocks - 1) {
                                    lockList.append(lockMacId).append(",");
                                } else {
                                    lockList.append(lockMacId);
                                }
                            }
                            Log.d(TAG, "Lock list after refresh: " + lockList);
                            macFileAccess.write(lockList.toString());
                            updateMacList();
                        } else {
                            macFileAccess.write("");
                            Toast.makeText(mContext, "No locks found", Toast.LENGTH_LONG).show();
                        }
                        updateMacList();

                    } else if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                        Toast.makeText(mContext, "Failed to download locks", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Snackbar.make(findViewById(android.R.id.content),CommunicationError.getMessage(Utils.parseInt(packet,RESPONSE_COMMAND_STATUS_POS )), Snackbar.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Invalid Packet");
                Toast.makeText(mContext, "Unable to Download List", Toast.LENGTH_SHORT).show();
        }
    }

    private void doLockDelete(final String mac)
    {
        progressDialog.setIndeterminate(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        this.progressDialog.setMessage("Deleting Lock...");
        this.progressDialog.show();
        Log.d(TAG, "Preparing Delete Packet:" + mac);
        try {
            Utils u = new Utils();
            u.requestType = Utils.DELETE_LOCK_REQUEST;
            u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
            u.requestDirection = Utils.TCP_SEND_PACKET;

            byte[] packet = new byte[AddDeleteLockPacket.SENT_PACKET_LENGTH];
            packet[REQUEST_PACKET_TYPE_POS] = Utils.DELETE_LOCK_REQUEST;
            packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
            packet[REQUEST_PACKET_LENGTH_POS] = AddDeleteLockPacket.SENT_PACKET_LENGTH;
            packet[AddDeleteLockPacket.OPERATION_TYPE] = AddDeleteLockPacket.DELETE_LOCK;

            byte[] macInHex=u.getMacIdInHex(mac);
            System.arraycopy(macInHex, 0, packet, 4, macInHex.length);
            //packet[AddDeleteLockPacket.CHECKSUM_SENT] = u.calculateChecksum(packet, true);

            u.commandDetails = new String(packet, StandardCharsets.ISO_8859_1);
            Log.d(TAG, "Sent Packet:" + u.commandDetails);
            u.setUtilsInfo(u);

            netClientAsyncTask=new NetClientAsyncTask(false,this, Utils.host, 80,
                    packet, new OnTaskCompleted<String>() {
                @Override
                public void onTaskCompleted(int resultCode, String value) {
                    Log.d(TAG, "onTaskCompleted:"+value);

                    if(resultCode== Activity.RESULT_OK) {
                        if(value!=null) {
                            Utils.printByteArray(value.getBytes(StandardCharsets.ISO_8859_1));
                            processPacket(value, mac);
                        }
                    }
                    else
                    {
                        if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.SOCKET_NOT_CONNECTED)
                        {
                            Toast.makeText(mContext,"Not Connected",Toast.LENGTH_LONG).show();
                        }
                        else if(NetClientAsyncTask.ERROR_CODE==NetClientAsyncTask.MESSAGE_NOT_RECEIVED)
                        {
                            Toast.makeText(mContext,"Connection Timeout",Toast.LENGTH_LONG).show();
                        }
                    }
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                }
            });
            netClientAsyncTask.execute();
        } catch (IndexOutOfBoundsException e) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            e.printStackTrace();
        }
    }

    private void processPacket(String packet, String mac)
    {
        DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
        Log.d(TAG, "processPacket/Received Packet:" + packet);
        if(packet != null && Utils.parseInt(packet, RESPONSE_PACKET_LENGTH_POS) >= AddDeleteLockPacket.RECEIVED_PACKET_LENGTH) {
            try {
                if ((packet.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.DELETE_LOCK_REQUEST) &&
                            packet.charAt(RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                        if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                            Log.d(TAG, mac+" Lock Deleted [SUCCESS]");
                            bridgeLockUtil.delete(mac);
                            updateMacList();
                            Log.d(TAG, "Successfully Deleted");
                        } else if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                            Log.d(TAG, "Deletion Failed");
                            Snackbar.make(findViewById(android.R.id.content),CommunicationError.getMessage(Utils.parseInt(packet,RESPONSE_COMMAND_STATUS_POS )), Snackbar.LENGTH_LONG).show();
                        }
                    }
                    else{
                        Log.d(TAG, "Error:"+ Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS));
                    Snackbar.make(findViewById(android.R.id.content),CommunicationError.getMessage(Utils.parseInt(packet,RESPONSE_COMMAND_STATUS_POS )), Snackbar.LENGTH_LONG).show();
                    }
            } catch(Exception e) {
                Log.d(TAG, "Unsupported String Decoding Exception");
            }
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        databaseHandler.close();
    }

    @Override
    public void onSearch(List<String> results) {
    }
}

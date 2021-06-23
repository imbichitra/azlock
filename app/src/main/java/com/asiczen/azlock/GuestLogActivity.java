package com.asiczen.azlock;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;

import com.asiczen.azlock.content.MySharedPreferences;
import com.asiczen.azlock.util.NotificationHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.azlock.app.AdapterViewCode;
import com.asiczen.azlock.app.model.AccessLog;
import com.asiczen.azlock.app.CommunicationError;
import com.asiczen.azlock.app.ConnectionMode;
import com.asiczen.azlock.app.DeviceStatus;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.CustomAdapter;
import com.asiczen.azlock.content.LogoutBroadcastReceiver;
import com.asiczen.azlock.net.NetClientAsyncTask;
import com.asiczen.azlock.content.NetClientContext;
import com.asiczen.azlock.content.SessionManager;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.net.OnDataAvailableListener;
import com.asiczen.azlock.net.OnDataSendListener;
import com.asiczen.azlock.net.OnTaskCompleted;
import com.asiczen.azlock.util.DateTimeFormat;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/*
 * Created by user on 8/21/2015.
 */
public class GuestLogActivity extends AppCompatActivity implements Packet, OnSearchListener {

    private final String TAG = GuestLogActivity.class.getSimpleName();
    private CustomAdapter<GuestLog> adapter;
    private ArrayList<GuestLog> guestLogs;
    private ListView guestListView;
    private TextView fromDateLogFilter, toDateLogFilter;
    private String[] fromDate, toDate;
    private Calendar calendar;

    private final Activity activity = this;
    private Context mContext;
    private Vibrator vibrator;
    private AppContext appContext;
    private ActionMode mActionMode;
    public static int logCounter = 0;
    public static ArrayList<GuestLog> selectedLogs;
    public static int selectedLogSize;
    private GuestLogActivity.GuestLog selectedItem;
    private boolean downloadLog;
    private OnDataSendListener mOnDataSendListener;
    private SessionManager sessionManager;
    private IntentFilter intentFilter;
    private LogoutBroadcastReceiver logoutBroadcastReceiver;
    private NetClientContext netClientContext;
    private AlertDialog dialog, alertDialog;
    private TextView textView;
    private ProgressBar progressBar;

    //swastik
    // int  c=0;
    private final List<String> hPacket = new ArrayList<>();
    //final ViewGroup nullParent = null;
    private MySharedPreferences sharedPreferences;
    private final int WRITE_EXTERNAL_STORAGE = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guest);
        mContext = this;
        appContext = AppContext.getContext();
        sharedPreferences = new MySharedPreferences(this);
        netClientContext = NetClientContext.getContext();
        calendar = Calendar.getInstance();
        selectedLogs = new ArrayList<>();
        selectedLogSize = 0;
        fromDate = new String[3];
        toDate = new String[3];
        mOnDataSendListener = appContext.getOnDataSendListener();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        downloadLog = getIntent().getBooleanExtra(Utils.EXTRA_DOWNLOAD_LOG, false);
        sessionManager = new SessionManager(this);
        logoutBroadcastReceiver = new LogoutBroadcastReceiver(this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(SessionManager.ACTION_LOGOUT);
        intentFilter.addAction(SessionManager.ACTION_EXIT);

        FloatingActionButton logFilterFab = findViewById(R.id.menu_fab);
        logFilterFab.setImageDrawable(ContextCompat.getDrawable(mContext, R.mipmap.ic_filter_list_white_48dp));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View bridgeConnectView = getLayoutInflater().inflate(R.layout.progressbar, null, false);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setView(bridgeConnectView);
        TextView dialogTextView = bridgeConnectView.findViewById(R.id.progressDialog);
        dialog = builder.create();
        dialogTextView.setText(R.string.loading_history);
        dialog.show();

        AlertDialog.Builder alertbuilder = new AlertDialog.Builder(this);
        View history = getLayoutInflater().inflate(R.layout.horiznoltal_progress, null, false);
        alertbuilder.setCancelable(false); // if you want user to wait for some process to finish,
        alertbuilder.setView(history);
        progressBar = history.findViewById(R.id.progressBar);
        textView = history.findViewById(R.id.textView);
        alertDialog = alertbuilder.create();

        guestListView = findViewById(R.id.GuestListView);
        guestListView.setEmptyView(findViewById(R.id.empty));
        new GuestLogAsyncTask().execute();

        guestListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode != null) {
                    // add or remove selection for current list item
                    vibrator.vibrate(20);
                    onListItemSelect(position);
                } else {
                    Log.d("GuestLogActivity", "Reason(Pos:" + position + "):" + guestLogs.get(position).getFailureReason());
                    if (guestLogs.get(position).getFailureReason() != null && !guestLogs.get(position).getFailureReason().isEmpty()) {
                        new AlertDialog.Builder(mContext).setTitle("Failure Reason")
                                .setMessage(guestLogs.get(position).getFailureReason())
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).create().show();
                    }
                }
            }
        });
    }

    public void onClickFab(View v) {
        final DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
        fromDate[0] = toDate[0] = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        fromDate[1] = toDate[1] = String.valueOf(calendar.get(Calendar.MONTH) + 1);
        fromDate[2] = toDate[2] = String.valueOf(calendar.get(Calendar.YEAR));

        View dateFilter = getLayoutInflater().inflate(R.layout.guestlogfilter, null, false);
        fromDateLogFilter = dateFilter.findViewById(R.id.fromDateTextView);
        toDateLogFilter = dateFilter.findViewById(R.id.toDateTextView);
        fromDateLogFilter.setText(DateTimeFormat.getDate(calendar.get(Calendar.DAY_OF_MONTH),
                (calendar.get(Calendar.MONTH) + 1), calendar.get(Calendar.YEAR), 2));
        toDateLogFilter.setText(DateTimeFormat.getDate(calendar.get(Calendar.DAY_OF_MONTH),
                (calendar.get(Calendar.MONTH) + 1), calendar.get(Calendar.YEAR), 2));

        fromDateLogFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(mContext, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        fromDateLogFilter.setText(DateTimeFormat.getDate(dayOfMonth, (monthOfYear + 1), year, 2));
                        fromDate[0] = String.valueOf(dayOfMonth);
                        fromDate[1] = String.valueOf(monthOfYear + 1);
                        fromDate[2] = String.valueOf(year);
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        toDateLogFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(mContext, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        toDateLogFilter.setText(DateTimeFormat.getDate(dayOfMonth, (monthOfYear + 1), year, 2));
                        toDate[0] = String.valueOf(dayOfMonth);
                        toDate[1] = String.valueOf(monthOfYear + 1);
                        toDate[2] = String.valueOf(year);
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        new AlertDialog.Builder(mContext).setTitle("Filter History")
                .setView(dateFilter).setPositiveButton("SEARCH", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d("GuestLogActivity", DateTimeFormat.getDate(fromDate, 3) + " <-> " + DateTimeFormat.getDate(toDate, 3));
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                try {
                    Date d1 = dateFormat.parse(DateTimeFormat.getDate(fromDate, 3));
                    Date d2 = dateFormat.parse(DateTimeFormat.getDate(toDate, 3));
                    if (d2 != null) {
                        if (d2.compareTo(d1) >= 0) {
                            guestLogs = databaseHandler.getLogByDates(appContext.getDoor().getId(),
                                    DateTimeFormat.getDate(fromDate, 3), DateTimeFormat.getDate(toDate, 3));
                            adapter = new CustomAdapter<>(activity, R.layout.guestlog, guestLogs, AdapterViewCode.GUEST_LOG_VIEW_CODE);
                            guestListView.setAdapter(adapter);
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(mContext, "End date must be greater.", Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }

            }
        }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).create().show();
        databaseHandler.close();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (logoutBroadcastReceiver != null) {
            unregisterReceiver(logoutBroadcastReceiver);
        }
        Runtime.getRuntime().gc();
        Log.d(TAG, "onDestroy called");
    }

    @Override
    public void onPause() {
        super.onPause();
        Runtime.getRuntime().gc();
        Log.d(TAG, "onPause called");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        if (sessionManager.verify()) {
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(logoutBroadcastReceiver, intentFilter);
        Log.d(TAG, "onStart() Called");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
    }

    private void updateLog() {
        Log.d("GuestLogActivity", "Updating Log");
        if (appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {
            Utils u = new Utils();
            u.requestType = Utils.LOG_REQUEST;
            u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
            u.requestDirection = Utils.TCP_SEND_PACKET;
            byte[] packet = new byte[MAX_PKT_SIZE];
            packet[REQUEST_PACKET_TYPE_POS] = Utils.LOG_REQUEST;
            packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
            packet[REQUEST_PACKET_LENGTH_POS] = LogRequestPacket.SENT_PACKET_LENGTH;
            DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
            packet[LogRequestPacket.LOG_READ_FLAG] = (byte) (databaseHandler.isLogExist(appContext.getDoor().getId()) ? 0 : 1);

            if (appContext.getConnectionMode() == ConnectionMode.CONNECTION_MODE_BLE || appContext.getConnectionMode() == ConnectionMode.CONNECTION_MODE_REMOTE) {
                mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                    @Override
                    public void onDataAvailable(String data) {
                        Log.v(TAG, "Log packet received");

                        processPacket(data);
                    }
                }, null);
            } else {
                NetClientAsyncTask clientAsyncTask = new NetClientAsyncTask(true, this, packet, appContext.getRouterInfo().getAddress(),
                        appContext.getRouterInfo().getPort(), new OnTaskCompleted<LinkedList<String>>() {
                    @Override
                    public void onTaskCompleted(int resultCode, LinkedList<String> result) {
                        for (String data : result) {
                            processPacket(data);
                        }
                    }
                });
                clientAsyncTask.showProgressDialog(true, "Downloading logs...");
                clientAsyncTask.setActivityName(GuestLogActivity.class.getSimpleName());
                netClientContext.setNetClient(clientAsyncTask);
                netClientContext.setReceiveMultiple(true, RESPONSE_ACTION_STATUS_POS, Utils.STS_END);
                clientAsyncTask.execute();
            }
        }
    }


    //swastik
    private void handelPacket(List<String> hPacket) {

        DatabaseHandler db = new DatabaseHandler(mContext);
        //db.deleteAllLogs();
        Log.d(TAG, "handelPacket: total length "+hPacket.size());
        for (String packet : hPacket) {


            Log.d(TAG, "History packet:" + packet);

            String guestMac = Utils.getStringFromHex(packet.substring(LogRequestPacket.GUEST_MAC_START,
                    LogRequestPacket.ACCESS_DATE_START));
            String accessDateTime = DateTimeFormat.parseDateFormat(packet,
                    LogRequestPacket.ACCESS_DATE_START);
            Log.d(TAG, "accessDateTime: "+accessDateTime);
            String accessStatus = (packet.charAt(LogRequestPacket.ACCESS_STATUS_POS)
                    == Utils.LOCKED ? "LOCKED" : (packet.charAt(LogRequestPacket.ACCESS_STATUS_POS)
                    == Utils.UNLOCKED ? "UNLOCKED" : "UNKNOWN"));
            String failureReason = ((Utils.parseInt(packet, LogRequestPacket.ACCESS_FAILURE_REASON_CODE_POS)
                    == Utils.CMD_OK) ? null : CommunicationError.getMessage(Utils.parseInt(packet,
                    LogRequestPacket.ACCESS_FAILURE_REASON_CODE_POS)));
            Log.d("GuestLogActivity", "Failure Reason:" + packet.charAt(
                    LogRequestPacket.ACCESS_FAILURE_REASON_CODE_POS) + " >> " + failureReason);
            //GuestLog log = new GuestLog(guestMac, accessDateTime, accessStatus);
            AccessLog accessLog = new AccessLog(guestMac, accessDateTime,
                    accessStatus, failureReason, appContext.getDoor().getId());
            Log.d("GuestLogActivity", "Log:" + accessLog);
            if (!sharedPreferences.getMac().equals(guestMac) && db.insert(accessLog)) {
                Log.d("GuestLogActivity", "Log updated.");
            } else {
                Log.d("GuestLogActivity", "Database can n0ot be updated.");
            }

        }

    }

    private void processPacket(String packet) {
        //Utils u = new Utils();

        DatabaseHandler db = new DatabaseHandler(mContext);
        //Log.d("GuestLogActivity", "processing recv packet/Packet Length(" + packet.length()+"):"+packet);
        //Log.d("GuestLogActivity", "CMD_STS_POS:"+Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS));
        //Log.d("GuestLogActivity", "ACTION_STATUS_POS:"+packet.charAt(RESPONSE_ACTION_STATUS_POS));
        if (packet != null && Utils.parseInt(packet, RESPONSE_PACKET_LENGTH_POS) >= LogRequestPacket.RECEIVED_PACKET_LENGTH
                && packet.charAt(LogRequestPacket.PACKET_ID_POS) == LogRequestPacket.PACKET_ID) {

            try {
                if (packet.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.LOG_REQUEST
                        && Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                    if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == Utils.STS_FETCH) {
                        //swastik

                        hPacket.add(packet);


                        logCounter++;


                        progressBar.setProgress(logCounter);
                        //String msg = logCounter + "/" + progressBar.getMax();
                        textView.setText("Log History downloading...");
                        alertDialog.show();


                        Log.v(TAG, logCounter + " History downloaded");

                    } else if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == Utils.STS_END) {
                        Log.d("GuestLogActivity", "Download Complete");

                                /*if(appContext.getConnectionMode()==ConnectionMode.CONNECTION_MODE_REMOTE && netClientContext!=null) {
                                    netClientContext.disconnectClient();
                                }*/ //bichi
                        //swastik
                        Log.d(TAG, "total length" + hPacket.size());
                        guestLogs.clear();
                        handelPacket(hPacket);

                        //END

                        guestLogs = db.getGuestLog(appContext.getDoor().getId());
                        Log.d("GuestLogActivity", guestLogs.size() + "");
                        adapter = new CustomAdapter<>(this, R.layout.guestlog, guestLogs, AdapterViewCode.GUEST_LOG_VIEW_CODE);
                        guestListView.setAdapter(adapter);
                        if (alertDialog != null && alertDialog.isShowing()) {

                            alertDialog.dismiss();
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                        }

                        adapter.onUpdate(OnUpdateListener.LOG_UPDATED, null);
                        Runtime.getRuntime().gc();
                    }
                } else {
                    if (alertDialog != null && alertDialog.isShowing()) {
                        alertDialog.dismiss();
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }

                    String errorMessage = CommunicationError.getMessage(Utils.parseInt(packet, RESPONSE_COMMAND_STATUS_POS));
                    Log.e("CMD_STS_ERR", errorMessage);

                    Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
                }

            } catch (Exception e) {
                if (alertDialog != null && alertDialog.isShowing()) {
                    alertDialog.dismiss();
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }

                Log.d("GuestLogActivity", "Unsupported String Decoding Exception");
                e.printStackTrace();
            }
        } else {
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.dismiss();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            if (packet != null) {
                Log.e(TAG, "Invalid or Null DoorMode" );
                //Toast.makeText(mContext, "Invalid or Null DoorMode", Toast.LENGTH_LONG).show();
            }
            Log.d("GuestLogActivity", "Packet Received:" + packet);
        }
    }

    private void onListItemSelect(int position) {
        adapter.toggleSelection(position);
        boolean hasCheckedItems = adapter.getSelectedCount() > 0;
        if (hasCheckedItems && mActionMode == null) {
            // there are some selected items, start the actionMode
            mActionMode = startActionMode(new ActionModeCallback());
        } else if (!hasCheckedItems && mActionMode != null) {
            // there no selected items, finish the actionMode
            mActionMode.finish();
        }

        if (mActionMode != null) {
            mActionMode.setTitle(adapter.getSelectedCount() + " selected");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.log_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    boolean isRefreshClicked = false;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        } else if (item.getItemId() == R.id.Refresh) {
            if (!isRefreshClicked) {
                isRefreshClicked = true;
                logCounter = 0;
                if (hPacket.size() > 0)
                    hPacket.clear();
                downloadHistory();
            }
        }else if(item.getItemId() == R.id.download){
            saveLog();
        }
        return true;
    }

    private void downloadHistory() {
        final DatabaseHandler databaseHandler = new DatabaseHandler(mContext);

        if (appContext.getConnectionMode() == ConnectionMode.CONNECTION_MODE_BLE || appContext.getConnectionMode()==ConnectionMode.CONNECTION_MODE_REMOTE) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            alertDialog.show();
            updateLog();
        }
        isRefreshClicked = false;
        databaseHandler.close();
    }

    public static void setOnUpdateListener(OnUpdateListener onUpdateListener) {
        Log.d("GuestLogActivity", "setOnUpdateListener: ");
    }

    @Override
    public void onSearch(List results) {

    }

    private class ActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // inflate contextual menu
            mode.getMenuInflater().inflate(R.menu.contextual_menu, menu);
            menu.getItem(1).setVisible(false);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            switch (item.getItemId()) {
                case R.id.menu_delete:
                    // retrieve selected items and delete them out
                    SparseBooleanArray selected = adapter.getSelectedIds();
                    selectedLogSize = selected.size();
                    selectedLogs.clear();
                    for (int i = (selected.size() - 1); i >= 0; i--) {
                        if (selected.valueAt(i)) {
                            if (CustomAdapter.viewCode == AdapterViewCode.GUEST_LOG_VIEW_CODE) {
                                selectedItem = adapter.getItem(selected.keyAt(i));
                                selectedLogs.add(selectedItem);
                            }
                        }
                    }
                    new AlertDialog.Builder(mContext).setTitle("Delete History")
                            .setMessage("Do you want to delete selected log?")
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    adapter.remove(selectedItem);
                                }
                            })
                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create().show();
                    mode.finish(); // Action picked, so close the CAB
                    return true;

                case R.id.menu_select_all:
                    SparseBooleanArray selectedItemIds = adapter.getSelectedIds();
                    for (int i = 0; i < adapter.getCount(); i++) {
                        if (!selectedItemIds.get(i)) {
                            adapter.toggleSelection(i);
                        }
                    }
                    return true;

                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // remove selection
            adapter.removeSelection();
            mActionMode = null;
            vibrator.vibrate(40);
        }
    }

    public static class GuestLog {
        private String guestName;
        private String guestId;
        private final String accessDateTime;
        private final String accessStatus;
        private final int logId;
        private Bitmap image;
        private String failureReason;

        public GuestLog() {
            guestName = guestId = accessDateTime = accessStatus = null;
            image = null;
            logId = 0;
        }

        public GuestLog(String guestId, String accessDateTime, String accessStatus) {
            this.guestId = guestId;
            this.accessDateTime = accessDateTime;
            this.accessStatus = accessStatus;
            logId = 0;
            image = null;
        }

        public GuestLog(int logId, String guestName, Bitmap image, String accessDateTime, String accessStatus, String failureReason) {
            this.guestName = guestName;
            this.image = image;
            this.accessDateTime = accessDateTime;
            this.accessStatus = accessStatus;
            this.logId = logId;
            this.failureReason = failureReason;
        }

        public int getLogId() {
            return logId;
        }

        public void setGuestId(String guestId) {
            this.guestId = guestId;
        }

        public void setImage(Bitmap image) {
            this.image = image;
        }

        public String getGuestId() {
            return guestId;
        }

        public String getGuestName() {
            return guestName;
        }

        public Bitmap getImage() {
            return image;
        }

        public String getAccessDateTime() {
            return accessDateTime;
        }

        public String getAccessStatus() {
            return accessStatus;
        }

        String getFailureReason() {
            return failureReason;
        }

        @Override
        public String toString() {
            return "Name:" + getGuestName() + "\nTime:" + getAccessDateTime() + "\nStatus:" + getAccessStatus();
        }
    }

    class GuestLogAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
            Log.d(TAG, "Selected Guests:" + Utils.selectedGuests);
            if (Utils.selectedGuests != null && Utils.selectedGuests.size() > 0) {
                guestLogs = databaseHandler.getLogByGuests(Utils.selectedGuests, appContext.getDoor().getId());
                Log.d(TAG, "!selectedGuests/" + guestLogs.size());
            } else {
                //guests = new DatabaseHandler(mContext).getGuestLog(Utils.getModifiedMac(MainActivity.whoLoggedIn.getId()),ConnectActivity.doorID);
                guestLogs = databaseHandler.getGuestLog(appContext.getDoor().getId());
                Log.d(TAG, "!selectedGuests/" + guestLogs.size());
            }
            databaseHandler.close();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter = new CustomAdapter<>(GuestLogActivity.this, R.layout.guestlog, guestLogs, AdapterViewCode.GUEST_LOG_VIEW_CODE);
                    guestListView.setAdapter(adapter);
                    guestListView.setEmptyView(findViewById(R.id.empty));
                    adapter.notifyDataSetChanged();
                   /* if(progressDialog != null && progressDialog.isShowing()){
                        progressDialog.dismiss();
                    }*/
                    dialog.dismiss();
                    if (downloadLog) {
                        logCounter = 0;
                        downloadHistory();
                    }
                }
            });
            return null;
        }
    }

    private void saveLog(){
        if(ActivityCompat.checkSelfPermission(GuestLogActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            saveGuestLogData();
        }else {
            ActivityCompat.requestPermissions(GuestLogActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},WRITE_EXTERNAL_STORAGE);
        }
    }

    private class DownloadWebPageTask extends AsyncTask<Void, Void, String>{

        @Override
        protected void onPostExecute(String rootPath) {
            super.onPostExecute(rootPath);
            NotificationHelper notificationHelper = new NotificationHelper(GuestLogActivity.this);
            NotificationCompat.Builder nb = notificationHelper.getNogetNotification1("Download Completed", "Location:Internal storage/AzLog",rootPath);
            notificationHelper.getManager().notify(1, nb.build());
            Toast.makeText(mContext, "Download Completed", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(Void... voids) {
            String rootPath="",filePath="";
            try {
                rootPath = Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + "/AzLog/";

                Log.d(TAG, "saveGuestLogData: "+rootPath);
                File root = new File(rootPath);
                if (!root.exists()) {
                    root.mkdirs();
                }

                filePath = rootPath + appContext.getDoor().getName()+"_guest_history_log.csv";
                File f = new File(filePath);
                if (f.exists()) {
                    f.delete();
                }
                f.createNewFile();

                FileOutputStream out = new FileOutputStream(f);

                try {
                    String entry1 = "NAME" + "," +
                            "DATE" + "," +
                            "STATUS" + "\n";
                    out.write( entry1.getBytes() );
                    for (int i=0;i<guestLogs.size();i++){
                        String status;
                        if (guestLogs.get(i).getAccessStatus().equalsIgnoreCase("LOCKED")){
                            status = "LOCKED";
                        }else if(guestLogs.get(i).getAccessStatus().equalsIgnoreCase("UNLOCKED")){
                            status = "UNLOCKED";
                        }else {
                            status = "ERROR";
                        }
                        String entry = guestLogs.get(i).getGuestName() + "," +
                                DateTimeFormat.getDate(guestLogs.get(i).getAccessDateTime(), 3).replace(",","") + "," +
                                status + "\n";
                        out.write( entry.getBytes() );
                    }
                    out.close();
                    Log.d(TAG, "saveLog: "+rootPath);
                } catch( Exception e ) {
                    e.printStackTrace();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return filePath;
        }
    }
    private void saveGuestLogData(){
        /*try {
            String rootPath = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/AzLog/";

            Log.d(TAG, "saveGuestLogData: "+rootPath);
            File root = new File(rootPath);
            if (!root.exists()) {
                root.mkdirs();
            }
            File f = new File(rootPath + "log.csv");
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();

            FileOutputStream out = new FileOutputStream(f);

            try {
                String entry1 = "NAME" + "," +
                        "DATE" + "," +
                        "STATUS" + "\n";
                out.write( entry1.getBytes() );
                for (int i=0;i<guestLogs.size();i++){
                    String status;
                    if (guestLogs.get(i).getAccessStatus().equalsIgnoreCase("LOCKED")){
                        status = "LOCKED";
                    }else if(guestLogs.get(i).getAccessStatus().equalsIgnoreCase("UNLOCKED")){
                        status = "UNLOCKED";
                    }else {
                        status = "ERROR";
                    }
                    String entry = guestLogs.get(i).getGuestName() + "," +
                            DateTimeFormat.getDate(guestLogs.get(i).getAccessDateTime(), 3).replace(",","") + "," +
                            status + "\n";
                    out.write( entry.getBytes() );
                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                    mNotificationManager.notify(0, getNogetNotification1("Downloaded","downloading",rootPath + "log.csv").build());

                }
                out.close();
                Log.d(TAG, "saveLog: "+rootPath);
            } catch( Exception e ) {
                e.printStackTrace();
            }
        }catch (Exception e){
            e.printStackTrace();
        }*/
        new DownloadWebPageTask().execute();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == WRITE_EXTERNAL_STORAGE && (grantResults.length > 0 ) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
            saveGuestLogData();
        }
    }

    public void onBackPressed() {
        String callerActivity = getIntent().getStringExtra(Utils.EXTRA_CALLER_ACTIVITY_NAME);
        if (callerActivity != null && callerActivity.equals("GuestListActivity")) {
            CustomAdapter.viewCode = AdapterViewCode.GUEST_LIST_VIEW_CODE;
        }
        super.onBackPressed();
        finish();
    }
}

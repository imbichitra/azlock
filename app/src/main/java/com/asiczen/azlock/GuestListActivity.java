package com.asiczen.azlock;

import android.Manifest;
import android.app.Activity;
//import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.DialogFragment;

import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.azlock.app.AdapterViewCode;
import com.asiczen.azlock.app.CommunicationError;
import com.asiczen.azlock.app.ConnectionMode;
import com.asiczen.azlock.app.DeviceStatus;
import com.asiczen.azlock.app.model.Door;
import com.asiczen.azlock.app.model.Guest;
import com.asiczen.azlock.app.model.Owner;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.CustomAdapter;
import com.asiczen.azlock.content.LogoutBroadcastReceiver;
import com.asiczen.azlock.content.SessionManager;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.net.OnDataAvailableListener;
import com.asiczen.azlock.net.OnDataSendListener;
import com.asiczen.azlock.util.DateTimeFormat;
import com.asiczen.azlock.util.FileAccess;
import com.asiczen.azlock.util.ImageUtility;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.RoundedImageView;
import com.asiczen.azlock.util.Utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*
 * Created by user on 8/10/2015.
 */
public class GuestListActivity extends AppCompatActivity implements OnUpdateListener, GuestDetailsFragment.OnChangeOwnerSelectedListener,
        OnSearchListener, OnDeleteListener, GuestDetailsFragment.OnEditListener, Packet {

    //Bitmap bmImage;
    private ListView listView;
    private AppContext appContext;
    private SessionManager sessionManager;
    private CustomAdapter<Guest> adapter;
    private CustomAdapter<Guest> searchAdapter;
    private ArrayList<Guest> guests = new ArrayList<>();
    private final ArrayList<Guest> refreshedGuests = new ArrayList<>();
    private List<Guest> searchResults = new ArrayList<>();
    private static int selectAdapterCode;
    //private final Guest selectedItem = null;
    public static boolean isSelectAll = false;
    private static final int SEARCH_ADAPTER = 2;
    private static final int BASIC_ADAPTER = 1;
    public static final String ARGUMENT_FROM_GUEST_LIST_ACTIVITY = "data";
    private final String TAG = GuestListActivity.class.getSimpleName();

    private Activity guestListActivity;
    private ActionMode mActionMode;
    public Context mContext;
    private SearchView searchView;
    //private CheckBox confirmDeleteGuest;
    //private View deleteGuestConfirmationView;
    //private ListView deleteGuestListView;
    //private ArrayAdapter<Guest> deleteGuestAdapter;
    //private SparseBooleanArray selected;
    public static int guestCounter = 0;
    private OnDataSendListener mOnDataSendListener;

    private static ProgressDialog progressDialog1, progressDialog2;
    public static int progressBarStatus = 0;
    public static String guestListItemType;
    private static final int REQUEST_DANGEROUS_PERMISSION = 11;
    private Vibrator vibrator;
    private IntentFilter intentFilter;
    private LogoutBroadcastReceiver logoutBroadcastReceiver;

    private String name, mac, pin;
    private boolean shouldDeleteGuests, shouldDeleteLogs;
    private CheckBox deleteLogsCheckbox;
    private CheckBox deleteGuestsCheckbox;
    private AlertDialog dialog;
    private TextView nameTextView, altOwnerMacEditText;
    private EditText pinEditText;
    private String receivedDataPacket1 = null, receivedDataPacket2 = null;
    //private NetClientContext clientContext;
    //final ViewGroup nullParent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guest);
        mContext = this;
        guestListActivity = this;
        appContext = AppContext.getContext();
        sessionManager = new SessionManager(this);
        Utils.selectedGuests = new ArrayList<>();
        logoutBroadcastReceiver = new LogoutBroadcastReceiver(this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(SessionManager.ACTION_LOGOUT);
        intentFilter.addAction(SessionManager.ACTION_EXIT);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        boolean isAboveVersion6 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
        mOnDataSendListener = appContext.getOnDataSendListener();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (sessionManager.verify()) {
            finish();
        }

        if (isAboveVersion6) {
            boolean locationPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
            boolean storagePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            Log.d(TAG, "Permission:" + locationPermission + ", " + storagePermission);
            if (!locationPermission || !storagePermission) {
                // Should we show an explanation?
                boolean shouldShowLocationPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
                boolean shouldShowStoragePermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (shouldShowLocationPermissionRationale || shouldShowStoragePermissionRationale) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    new AlertDialog.Builder(this).setTitle("Permission Denied")
                            .setCancelable(false)
                            .setMessage("Without these permissions the app is unable to save guest details and can not store any data on this device. Are you sure you want to deny these permissions?")
                            .setPositiveButton("I'M SURE", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setNegativeButton("RETRY", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(GuestListActivity.this,
                                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                            REQUEST_DANGEROUS_PERMISSION);
                                }
                            }).create().show();

                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_DANGEROUS_PERMISSION);

                    // REQUEST_DANGEROUS_PERMISSION is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }
        }

        //swipeView = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        /*FileAccess fileAccess = new FileAccess(mContext, Utils.GUEST_LIST_TYPE_OPTIONS_FILE);
        guestListItemType = fileAccess.read();
        DatabaseHandler databaseHandler = new DatabaseHandler(this);
        if(guestListItemType == null && fileAccess.FILE_NOT_FOUND){
            fileAccess.write(String.valueOf(Utils.SHOW_ALL_GUESTS_EXCEPT_KEY_DELETED));
            guests = databaseHandler.getGuestsExceptDeletedKey(MainActivity.doorID);
            Log.d("GuestList", "GuestsExceptDeletedKeyLength:"+guests.size()+"[guestListItemType = NULL]");
        }
        if(!guestListItemType.isEmpty() && Integer.parseInt(guestListItemType) == Utils.SHOW_ACTIVE_GUESTS_ONLY){
            guests = databaseHandler.getActiveGuests(MainActivity.doorID);
        } else {
            guests = databaseHandler.getGuestsExceptDeletedKey(MainActivity.doorID);
        }*/
        CustomAdapter.setOnDeleteListener(this);
        Utils.selectedGuestsSize = 0;
        progressDialog1 = new ProgressDialog(mContext);
        progressDialog1.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog1.setMessage("Loading Guests...");
        progressDialog1.setIndeterminate(true);
        progressDialog1.show();

        Log.d("GuestListActivity", guests.size() + "(size)");
        FloatingActionButton registerGuestFab = findViewById(R.id.menu_fab);
        registerGuestFab.setImageDrawable(ContextCompat.getDrawable(mContext, R.mipmap.ic_person_add_white_48dp));

        listView = findViewById(R.id.GuestListView);
        //adapter = new CustomAdapter<>(this, R.layout.guestlist, guests, CustomAdapter.GUEST_LIST_VIEW_CODE);
        selectAdapterCode = BASIC_ADAPTER;
        //listView.setAdapter(adapter);
        listView.setEmptyView(findViewById(R.id.empty));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode == null) {
                    /* no items selected, so perform item click actions
                     * like moving to next activity */
                    /* close search view if its visible
                    if (searchView != null && searchView.isShown()) {
                        searchMenuItem.collapseActionView();
                        searchView.setQuery("", false);
                    }*/
                    //GuestDetailsFragment.guest = guests.get(position);
                    GuestDetailsFragment.guest = adapter.getItem(position);
                    new GuestDetailsFragment().show(getSupportFragmentManager(), "Guest Details");
                } else {
                    // add or remove selection for current list item
                    vibrator.vibrate(20);
                    onListItemSelect(position);
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                vibrator.vibrate(30);
                onListItemSelect(position);
                return true;
            }
        });
        new GuestListAsyncTask().execute();
        RegisterGuestActivity.setOnUpdateListener(this);

        /*swipeView.setColorSchemeColors(Color.BLUE, Color.RED, Color.rgb(204,102,0),Color.rgb(204,0,102),Color.rgb(0,0,0));
        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeView.setRefreshing(true);
                Log.d("Swipe", "Refreshing");
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeView.setRefreshing(false);
                        Log.d("Swipe", "Refreshed");
                    }
                }, 3000);
            }
        });*/
        //databaseHandler.close();
    }

    public void onClickFab(View v) {
        Intent registerGuestIntent = new Intent(GuestListActivity.this, RegisterGuestActivity.class);
        registerGuestIntent.putExtra(ARGUMENT_FROM_GUEST_LIST_ACTIVITY, Utils.GUEST_REGISTRATION_CODE);
        startActivityForResult(registerGuestIntent, Utils.GUEST_REGISTRATION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == REQUEST_DANGEROUS_PERMISSION) {
            boolean isGranted = true;
            for (int grantResult : grantResults) {
                isGranted = isGranted && grantResult == PackageManager.PERMISSION_GRANTED;
            }
            if (isGranted) {
                // permission was granted, yay! Do the
                // contacts-related task you need to do.
                guestCounter = 0;
                if (appContext.getConnectionMode() == ConnectionMode.CONNECTION_MODE_BLE || appContext.getConnectionMode() == ConnectionMode.CONNECTION_MODE_REMOTE) {
                    progressDialog2 = new ProgressDialog(mContext);
                    progressDialog2.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog2.setMessage("Downloading Guests");
                    progressDialog2.setIndeterminate(true);
                    progressDialog2.setCancelable(false);
                    progressDialog2.setProgressNumberFormat(null);
                    progressDialog2.setProgressPercentFormat(null);
                    progressDialog2.show();
                }
                updateGuests();
            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                finish();
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void onListItemSelect(int position) {
        isSelectAll = false;
        if (selectAdapterCode == BASIC_ADAPTER) {
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
        } else if (selectAdapterCode == SEARCH_ADAPTER) {
            searchAdapter.toggleSelection(position);
            boolean hasCheckedItems = searchAdapter.getSelectedCount() > 0;

            if (hasCheckedItems && mActionMode == null)
                // there are some selected items, start the actionMode
                mActionMode = startActionMode(new ActionModeCallback());
            else if (!hasCheckedItems && mActionMode != null)
                // there no selected items, finish the actionMode
                mActionMode.finish();

            if (mActionMode != null)
                mActionMode.setTitle(searchAdapter.getSelectedCount() + " selected");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.guest_list_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchMenuItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchMenuItem.getActionView();
        if (searchView != null) {

            ((EditText) searchView.findViewById(androidx.appcompat.R.id.search_src_text)).setTextColor(Color.WHITE);
            ((EditText) searchView.findViewById(androidx.appcompat.R.id.search_src_text)).setHintTextColor(Color.WHITE);

            searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    searchView.setQuery("", false);
                    DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
                    ArrayList<Guest> updatedList = databaseHandler.getGuests(appContext.getDoor().getId(), Utils.SHOW_ALL_GUESTS);
                    selectAdapterCode = BASIC_ADAPTER;
                    adapter = new CustomAdapter<>(guestListActivity, R.layout.guestlist, updatedList,
                            AdapterViewCode.GUEST_LIST_VIEW_CODE);
                    listView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    databaseHandler.close();
                    return true;
                }
            });
            searchMenuItem.setActionView(searchView);
            if (searchManager != null) {
                searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            }
            searchView.setSubmitButtonEnabled(true);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    adapter.getFilter().filter(newText);
                    return true;
                }
            });
        }
        return super.onCreateOptionsMenu(menu);
    }

    boolean isRefreshClicked = false;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        final DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.Refresh:
                if (!isRefreshClicked) {
                    isRefreshClicked = true;
                    guestCounter = 0;
                    Log.d(TAG, "onOptionsItemSelected: ");
                    if (appContext.getConnectionMode() == ConnectionMode.CONNECTION_MODE_BLE || appContext.getConnectionMode() == ConnectionMode.CONNECTION_MODE_REMOTE) {
                        progressDialog2 = new ProgressDialog(mContext);
                        progressDialog2.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        progressDialog2.setMessage("Downloading Guests");
                        progressDialog2.setIndeterminate(true);
                        progressDialog2.setCancelable(false);
                        progressDialog2.setProgressNumberFormat(null);
                        progressDialog2.setProgressPercentFormat(null);
                        progressDialog2.show();
                        updateGuests();
                    }
                }
                break;
            case R.id.appearance:
                View guestAppearanceView = getLayoutInflater().inflate(R.layout.guest_appearance, null, false);
                RadioGroup appearanceGroup = guestAppearanceView.findViewById(R.id.appearanceRadioGroup);
                RadioButton allRadioButton = guestAppearanceView.findViewById(R.id.all_guests_radioButton);
                RadioButton activeRadioButton = guestAppearanceView.findViewById(R.id.active_guest_radioButton);
                RadioButton expiredRadioButton = guestAppearanceView.findViewById(R.id.expired_guests_radioButton);
                final FileAccess fileAccess = new FileAccess(mContext, Utils.GUEST_LIST_TYPE_OPTIONS_FILE);
                GuestListActivity.guestListItemType = fileAccess.read();
                if (GuestListActivity.guestListItemType == null && fileAccess.FILE_NOT_FOUND) {
                    fileAccess.write(String.valueOf(Utils.SHOW_ALL_GUESTS_EXCEPT_KEY_DELETED));
                    allRadioButton.setChecked(true);
                } else if (GuestListActivity.guestListItemType != null && !GuestListActivity.guestListItemType.isEmpty()) {
                    int x = Integer.parseInt(GuestListActivity.guestListItemType);
                    if (x == Utils.SHOW_ACTIVE_GUESTS_ONLY)
                        activeRadioButton.setChecked(true);
                    else if (x == Utils.SHOW_EXPIRED_GUESTS_ONLY)
                        expiredRadioButton.setChecked(true);
                    else if (x == Utils.SHOW_ALL_GUESTS_EXCEPT_KEY_DELETED)
                        allRadioButton.setChecked(true);
                }
                appearanceGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {

                        if (checkedId == R.id.all_guests_radioButton) {
                            fileAccess.write(String.valueOf(Utils.SHOW_ALL_GUESTS_EXCEPT_KEY_DELETED));
                        } else if (checkedId == R.id.active_guest_radioButton) {
                            fileAccess.write(String.valueOf(Utils.SHOW_ACTIVE_GUESTS_ONLY));
                        } else if (checkedId == R.id.expired_guests_radioButton) {
                            fileAccess.write(String.valueOf(Utils.SHOW_EXPIRED_GUESTS_ONLY));
                        }
                        adapter.onUpdate(OnUpdateListener.GUEST_UPDATED, null);
                    }
                });
                new AlertDialog.Builder(mContext).setTitle("Guest Appearance")
                        .setView(guestAppearanceView)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create().show();
                break;
        }
        databaseHandler.close();
        return true;
    }

    private int sequenceNumber = Integer.MIN_VALUE;

    private void updateGuests() {
        Log.d("GuestListActivity", "Updating Guest List");
        if (appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {
            Utils u = new Utils();
            u.requestType = Utils.KEY_REQ;
            u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
            u.requestDirection = Utils.TCP_SEND_PACKET;
            byte[] packet = new byte[MAX_PKT_SIZE];
            packet[REQUEST_PACKET_TYPE_POS] = Utils.KEY_REQ;
            packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
            packet[REQUEST_PACKET_LENGTH_POS] = UpdateGuestPacket.SENT_PACKET_LENGTH;

            packet[UpdateGuestPacket.REFRESH_FLAG] = UpdateGuestPacket.REFRESH_GUEST_LIST;
            //packet[UpdateGuestPacket.CHECKSUM_SENT] = u.calculateChecksum(packet, true);

            /*if(appContext.getConnectionMode()==ConnectionMode.CONNECTION_MODE_REMOTE) {
                clientContext = NetClientContext.getContext();
                clientContext.setReceiveMultiple(true, RESPONSE_ACTION_STATUS_POS, Utils.STS_END);

                NetClientAsyncTask clientAsyncTask = new NetClientAsyncTask(true, this, packet, appContext.getRouterInfo().getAddress(),
                        appContext.getRouterInfo().getPort(), new OnTaskCompleted<LinkedList<String>>() {
                    @Override
                    public void onTaskCompleted(int resultCode, LinkedList<String> list) {
                        for (String data : list) {
                            if (data != null && data.charAt(RESPONSE_ACTION_STATUS_POS) == Utils.STS_FETCH) {
                                Log.d(TAG, "STS_FETCH [sequenceNumber=" + sequenceNumber + "]");
                                if (sequenceNumber < 0) {
                                    sequenceNumber = Utils.parseInt(data, UpdateGuestPacket.SEQUENCE_NUMBER_POS);
                                    Log.d(TAG, "sequenceNumber=" + sequenceNumber);
                                    if (data.charAt(UpdateGuestPacket.PACKET_TYPE_POSITION) == '4') {
                                        receivedDataPacket1 = data;
                                        Log.d(TAG, "Data set");
                                    }
                                } else if (sequenceNumber == Utils.parseInt(data, UpdateGuestPacket.SEQUENCE_NUMBER_POS)
                                        && data.charAt(UpdateGuestPacket.PACKET_TYPE_POSITION) == '5') {
                                    receivedDataPacket2 = data;
                                    Log.d(TAG, "Data set 2");
                                    sequenceNumber = Integer.MIN_VALUE;
                                    processPacket(receivedDataPacket1, receivedDataPacket2);
                                }
                            } else {
                                DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
                                if (appContext.getConnectionMode() == ConnectionMode.CONNECTION_MODE_REMOTE && clientContext != null) {
                                    clientContext.disconnectClient();
                                }
                                Log.d(TAG, "STS_END GuestListActivity Download Complete");
                                ArrayList<Guest> registeredGuests = databaseHandler.getGuests(appContext.getDoor().getId(), Utils.SHOW_ALL_GUESTS);
                                Log.d("GuestListActivity", "Registered Guests:" + registeredGuests);
                                Log.d("GuestListActivity", "Refreshed Guests:" + refreshedGuests);
                                registeredGuests.removeAll(refreshedGuests);
                                Log.d("GuestListActivity", "Guests Need to be Deleted:" + registeredGuests);
                                databaseHandler.setKeyShared(refreshedGuests);
                                if (registeredGuests.size() > 0) {
                                    databaseHandler.deleteKeys(registeredGuests);
                                }

                                //progressDialog2.setMessage("Updating Guest list");
                                guests.clear();
                                guests = databaseHandler.getGuests(appContext.getDoor().getId(), Utils.SHOW_ALL_GUESTS);
                                if (registeredGuests.size() > 0) {
                                    guests.removeAll(registeredGuests);
                                }
                                listView = findViewById(R.id.GuestListView);
                                adapter = new CustomAdapter<>(GuestListActivity.this, R.layout.guestlist, guests, AdapterViewCode.GUEST_LIST_VIEW_CODE);
                                selectAdapterCode = BASIC_ADAPTER;
                                listView.setAdapter(adapter);
                                //progressDialog2.dismiss();
                                guestCounter = 0;
                                //mOnUpdateListener.onUpdate(OnUpdateListener.GUEST_UPDATED);
                                databaseHandler.close();
                            }
                        }
                    }
                });
                clientAsyncTask.showProgressDialog(true, "Downloading guest details...");
                clientAsyncTask.setActivityName(GuestListActivity.class.getSimpleName());
                clientContext.setNetClient(clientAsyncTask);
                clientAsyncTask.execute();
            }
            else {*/

            final DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
            databaseHandler.deleteAllGuests();
            mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                @Override
                public void onDataAvailable(String data) {
                    Log.d(TAG, "onDataAvailable:" + data);
                    if (data != null && data.charAt(RESPONSE_ACTION_STATUS_POS) == Utils.STS_FETCH) {
                        Log.d(TAG, "STS_FETCH [sequenceNumber=" + sequenceNumber + "]");
                        if (data.charAt(UpdateGuestPacket.PACKET_TYPE_POSITION) == '4'){
                            Utils.printByteArray(data.getBytes());
                            sequenceNumber = Utils.parseInt(data, UpdateGuestPacket.SEQUENCE_NUMBER_POS);
                            receivedDataPacket1 = data;
                        }else if (sequenceNumber == Utils.parseInt(data, UpdateGuestPacket.SEQUENCE_NUMBER_POS)
                                && data.charAt(UpdateGuestPacket.PACKET_TYPE_POSITION) == '5') {
                            receivedDataPacket2 = data;
                            Log.d(TAG, "Data set 2");
                            isRefreshClicked = false;
                            processPacket(receivedDataPacket1, receivedDataPacket2);
                        }
                    } else {
                        isRefreshClicked = false;
                            /*if (appContext.getConnectionMode() == ConnectionMode.CONNECTION_MODE_REMOTE && clientContext != null) {
                                clientContext.disconnectClient();
                            }*/ //bichi
                        Log.w("GuestListActivity", "Download Complete");
                        ArrayList<Guest> registeredGuests = databaseHandler.getGuests(appContext.getDoor().getId(), Utils.SHOW_ALL_GUESTS);
                        Log.d("GuestListActivity", "Registered Guests:" + registeredGuests);
                        Log.d("GuestListActivity", "Refreshed Guests:" + refreshedGuests);
                        registeredGuests.removeAll(refreshedGuests);
                        Log.d("GuestListActivity", "Guests Need to be Deleted:" + registeredGuests);
                        databaseHandler.setKeyShared(refreshedGuests);
                        if (registeredGuests.size() > 0) {
                            databaseHandler.deleteKeys(registeredGuests);
                        }

                        progressDialog2.setMessage("Updating Guest list");
                        guests.clear();
                        guests = databaseHandler.getGuests(appContext.getDoor().getId(), Utils.SHOW_ALL_GUESTS);
                        if (registeredGuests.size() > 0) {
                            guests.removeAll(registeredGuests);
                        }
                        listView = findViewById(R.id.GuestListView);
                        adapter = new CustomAdapter<>(GuestListActivity.this, R.layout.guestlist, guests, AdapterViewCode.GUEST_LIST_VIEW_CODE);
                        selectAdapterCode = BASIC_ADAPTER;
                        listView.setAdapter(adapter);
                        progressDialog2.dismiss();
                        guestCounter = 0;
                        //mOnUpdateListener.onUpdate(OnUpdateListener.GUEST_UPDATED);
                        databaseHandler.close();
                    }
                }
            }, "Receiving...");
            //}
        }
    }

    private void processPacket(String packet1, String packet2) {
        DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
        Log.d(TAG, "Packet 1:" + packet1 + "\nPacket2:" + packet2);
        if (packet1 != null && Utils.parseInt(packet1, RESPONSE_PACKET_LENGTH_POS) >= UpdateGuestPacket.RECEIVED_PACKET_1_LENGTH
                && packet2 != null && Utils.parseInt(packet2, RESPONSE_PACKET_LENGTH_POS) >= UpdateGuestPacket.RECEIVED_PACKET_2_LENGTH) {
            byte[] strBytes2;
            Log.d(TAG, "OK 2");
            try {
                //strBytes = packet1.getBytes(StandardCharsets.ISO_8859_1);
                strBytes2 = packet2.getBytes(StandardCharsets.ISO_8859_1);

                if (packet1.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.KEY_REQ
                        && packet1.charAt(RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK
                        && packet2.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.KEY_REQ
                        && packet2.charAt(RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                    if (packet1.charAt(RESPONSE_ACTION_STATUS_POS) == Utils.STS_FETCH
                            && packet2.charAt(RESPONSE_ACTION_STATUS_POS) == Utils.STS_FETCH) {
                        Guest guest = new Guest();
                        String name = packet1.substring(UpdateGuestPacket.GUEST_NAME_START, UpdateGuestPacket.CHECKSUM_RECV_1) + packet2.charAt(6);

                        int endIndex = name.indexOf('ÿ');
                        if (endIndex < 0) {
                            guest.setName(name);
                        } else {
                            guest.setName(name.substring(0, endIndex));
                        }
                                /*String phone = packet.substring(UpdateGuestPacket.GUEST_PHONE_START, UpdateGuestPacket.GUEST_EMAIL_START);
                                guest.setPhone(phone);*/
                        Log.d("GuestList", "Access Type:" + packet2.charAt(UpdateGuestPacket.ACCESS_TYPE_POSITION));
                        String accessType = (packet2.charAt(UpdateGuestPacket.ACCESS_TYPE_POSITION) == Utils.LIMITED_TIME_ACCESS ? "Limited Time" : "Full Time");
                        guest.setAccessType(accessType);

                        //String startAccessDateTime = DateTimeFormat.parseDateFormat(packet2, UpdateGuestPacket.START_ACCESS_DATE_POS);
                        String startAccessDateTime = DateTimeFormat.parseTime(strBytes2, UpdateGuestPacket.START_ACCESS_DATE_POS);
                        guest.setAccessStartDateTime(startAccessDateTime);
                        //String endAccessDateTime = DateTimeFormat.parseDateFormat(packet2, UpdateGuestPacket.END_ACCESS_DATE_POS);
                        String endAccessDateTime = DateTimeFormat.parseTime(strBytes2, UpdateGuestPacket.END_ACCESS_DATE_POS);
                        guest.setAccessEndDateTime(endAccessDateTime);

                        Log.d(TAG, startAccessDateTime + " " + endAccessDateTime);

                                /*SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
                                calendar.setTime(dateFormat.parse(startAccessDateTime));
                                if(group.equalsIgnoreCase("Friend")) {
                                    calendar.add(Calendar.HOUR, -1);
                                    calendar.add(Calendar.DATE, durationInDays+1);
                                    String endAccessDateTime = dateFormat.format(calendar.getTime());
                                    guest.setAccessEndDateTime(endAccessDateTime);
                                } else if(guest.getGroup().equalsIgnoreCase("Guest")) {
                                    calendar.add(Calendar.HOUR, durationInHour);
                                    calendar.add(Calendar.DATE, durationInDays);
                                    String endAccessDateTime = dateFormat.format(calendar.getTime());
                                    guest.setAccessEndDateTime(endAccessDateTime);
                                }*/
                        guest.setId(Utils.getStringFromHex(packet1.substring(UpdateGuestPacket.PHONE_MAC_ID_START,
                                UpdateGuestPacket.GUEST_NAME_START)));
                        //guest.setAccessDuration(String.valueOf(durationInHour));
                        Bitmap guestImage = databaseHandler.getImage(guest);
                        if (guestImage != null) {
                            guest.setImage(guestImage);
                        }
                        //Log.d("GuestListActivity", guest.getId() + " > isExist in DB:" + new DatabaseHandler(mContext).isExist(guest.getId()));
                        Log.d("GuestListActivity", "Guest:" + guest);

                        if (databaseHandler.isExist(guest.getId())) {
                            Log.d("GuestListActivity", "Updating Guest:" + databaseHandler.update(guest));
                            Log.d("GuestListActivity", "Registering Guest:" + databaseHandler.registerDoor(guest, appContext.getDoor()));
                        } else {
                            Log.d("GuestListActivity", "Inserting Guest:" + databaseHandler.insert(guest));
                            Log.d("GuestListActivity", "Registering Guest:" + databaseHandler.registerDoor(guest, appContext.getDoor()));
                        }
                        refreshedGuests.add(guest);
                        guestCounter++;
                        if (progressDialog2 != null && progressDialog2.isShowing()) {
                            progressDialog2.setMessage(guestCounter + " Guest downloaded");
                        }
                    } else {
                        if (progressDialog2 != null && progressDialog2.isShowing()) {
                            progressDialog2.dismiss();
                        }
                        Log.d("GuestListActivity", "Error Downloading guests");
                    }
                } else {
                    if (progressDialog2 != null && progressDialog2.isShowing()) {
                        progressDialog2.dismiss();
                    }
                    String errorMessage = Utils.CommunicationError.commandStatusError(packet1.charAt(RESPONSE_PACKET_TYPE_POS));
                    Log.d("CMD_STS_ERR", errorMessage);
                    Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
                }
            } catch (IndexOutOfBoundsException e) {
                if (progressDialog2 != null && progressDialog2.isShowing()) {
                    progressDialog2.dismiss();
                }
                e.printStackTrace();
            }
        }
        databaseHandler.close();
        Runtime.getRuntime().gc();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
        if((isAboveVersion6 && locationPermission) || !isAboveVersion6) {
            registerReceiver(wifiBroadcastReceiver, filter);
        }
        */
        isSelectAll = false;
        if (sessionManager.verify()) {
            finish();
        }
        Log.d("GuestListActivity", "onResume() Called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        /*
        if((isAboveVersion6 && locationPermission) || !isAboveVersion6) {
            unregisterReceiver(wifiBroadcastReceiver);
        }
        */
        Log.d("GuestListActivity", "onPause() Called");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
        if (requestCode == Utils.GUEST_REGISTRATION_CODE || requestCode == Utils.GUEST_EDIT_PROFILE_CODE)
            if (Activity.RESULT_OK == resultCode) {
                int result = Objects.requireNonNull(data.getExtras()).getInt(ARGUMENT_FROM_GUEST_LIST_ACTIVITY);
                if (result == Utils.SUCCESS) {
                    Log.d("GuestListActivity", "Guest Registered");

                    FileAccess fileAccess = new FileAccess(mContext, Utils.GUEST_LIST_TYPE_OPTIONS_FILE);
                    guestListItemType = fileAccess.read();
                    Log.d("GuestListActivity", guestListItemType);
                    if (guestListItemType == null && fileAccess.FILE_NOT_FOUND) {
                        fileAccess.write(String.valueOf(Utils.SHOW_ALL_GUESTS_EXCEPT_KEY_DELETED));
                    } else if (guestListItemType != null && !guestListItemType.isEmpty()) {
                        guests.clear();
                        if (Integer.parseInt(guestListItemType) == Utils.SHOW_ACTIVE_GUESTS_ONLY) {
                            guests = databaseHandler.getGuests(appContext.getDoor().getId(), Utils.SHOW_ACTIVE_GUESTS_ONLY);
                        } else {
                            guests = databaseHandler.getGuests(appContext.getDoor().getId(), Utils.SHOW_ALL_GUESTS_EXCEPT_KEY_DELETED);
                        }
                        Log.d("GuestListActivity", "Guest Size:" + guests.size());
                        adapter.clear();
                        adapter = new CustomAdapter<>(GuestListActivity.this, R.layout.guestlist, guests, AdapterViewCode.GUEST_LIST_VIEW_CODE);
                        selectAdapterCode = BASIC_ADAPTER;
                        listView.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    }

                    if (requestCode == Utils.GUEST_REGISTRATION_CODE) {
                        Snackbar.make(findViewById(R.id.menu_fab), "Guest Successfully Registered", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    } else {
                        Snackbar.make(findViewById(R.id.menu_fab), "Guest Details Updated", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                } else {
                    Log.d("GuestListActivity", "Registration/Edit Failed");
                    Snackbar.make(findViewById(R.id.menu_fab), "Registration Failed", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    //new android.app.AlertDialog.Builder(mContext).setMessage(Utils.errors.get(result)).create().show();
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        databaseHandler.close();
    }

    @Override
    public void onUpdate(int requestCode, Object result) {
        DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
        if (requestCode == IMAGE_UPDATED) {
            ArrayList<Guest> updatedList;
            Log.d("GuestListActivity", "onImageUpdated/Refreshing List");
            if (searchView != null && searchView.isShown()) {
                updatedList = new ArrayList<>();
                for (Guest guest : searchResults) {
                    updatedList.add((Guest) databaseHandler.getUser(guest.getId()));
                }
            } else
                updatedList = databaseHandler.getGuests(appContext.getDoor().getId(), Utils.SHOW_ALL_GUESTS);
            adapter = new CustomAdapter<>(GuestListActivity.this, R.layout.guestlist, updatedList, AdapterViewCode.GUEST_LIST_VIEW_CODE);
            listView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            //mOnUpdateListener.onUpdate(OnUpdateListener.IMAGE_UPDATED);
        } else if (requestCode == GUEST_VIEW_UPDATED) {
            adapter.onUpdate(OnUpdateListener.GUEST_UPDATED, null);
        }
        databaseHandler.close();
    }

    @Override
    public void onSearch(List results) {
        Log.d("GuestListActivity", "onSearch() called");
        if (searchView != null && searchView.isShown()) {
            searchResults = results;
            selectAdapterCode = SEARCH_ADAPTER;
            searchAdapter = new CustomAdapter<>(GuestListActivity.this, R.layout.guestlist, searchResults,
                    AdapterViewCode.GUEST_LIST_VIEW_CODE);
            listView.setAdapter(searchAdapter);
            searchAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDelete() {
        DatabaseHandler databaseHandler = new DatabaseHandler(this);
        //databaseHandler.delete(GuestDetailsFragment.guest, appContext.getDoor());
        ArrayList<Guest> updatedList = databaseHandler.getGuests(appContext.getDoor().getId(), Utils.SHOW_ALL_GUESTS);
        Log.d("GuestListActivity", "AZLOCK onDeleted/Refreshing List");
        adapter = new CustomAdapter<>(this, R.layout.guestlist, updatedList, AdapterViewCode.GUEST_LIST_VIEW_CODE);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        databaseHandler.close();
        Snackbar.make(findViewById(R.id.menu_fab), "Guest Successfully Deleted", Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onEdit(DialogFragment dialogFragment) {
        Intent registerGuestIntent = new Intent(mContext, RegisterGuestActivity.class);
        registerGuestIntent.putExtra(ARGUMENT_FROM_GUEST_LIST_ACTIVITY, Utils.GUEST_EDIT_PROFILE_CODE);
        startActivityForResult(registerGuestIntent, Utils.GUEST_EDIT_PROFILE_CODE);
    }

    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // inflate contextual menu
            mode.getMenuInflater().inflate(R.menu.contextual_menu, menu);
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
                    new DeleteActionMode().execute();
                    //mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.menu_log:
                    // retrieve selected items and delete them out
                    SparseBooleanArray selectedItems = (selectAdapterCode == BASIC_ADAPTER) ? adapter.getSelectedIds()
                            : searchAdapter.getSelectedIds();
                    Utils.selectedGuests.clear();
                    for (int i = (selectedItems.size() - 1); i >= 0; i--) {
                        if (selectedItems.valueAt(i)) {
                            Guest guest = (selectAdapterCode == BASIC_ADAPTER) ? adapter.getItem(selectedItems.keyAt(i))
                                    : searchAdapter.getItem(selectedItems.keyAt(i));
                            Utils.selectedGuests.add(guest);
                        }
                    }
                    Intent guestLogIntent = new Intent(GuestListActivity.this, GuestLogActivity.class);
                    guestLogIntent.putExtra(Utils.EXTRA_CALLER_ACTIVITY_NAME, "GuestListActivity");
                    mode.finish(); // Action picked, so close the CAB
                    startActivity(guestLogIntent);
                    return true;
                case R.id.menu_select_all:
                    isSelectAll = true;
                    if (selectAdapterCode == BASIC_ADAPTER) {
                        SparseBooleanArray selectedItemIds = adapter.getSelectedIds();
                        for (int i = 0; i < adapter.getCount(); i++) {
                            if (!selectedItemIds.get(i)) {
                                adapter.toggleSelection(i);
                            }
                        }
                    } else if (selectAdapterCode == SEARCH_ADAPTER) {
                        SparseBooleanArray selectedItemIds = searchAdapter.getSelectedIds();
                        for (int i = 0; i < searchAdapter.getCount(); i++) {
                            if (!selectedItemIds.get(i)) {
                                searchAdapter.toggleSelection(i);
                            }
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
            if (selectAdapterCode == BASIC_ADAPTER) adapter.removeSelection();
            else if (selectAdapterCode == SEARCH_ADAPTER) searchAdapter.removeSelection();
            mActionMode = null;
            vibrator.vibrate(40);
        }
    }

    static class DeleteActionMode extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Runtime.getRuntime().gc();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        registerReceiver(logoutBroadcastReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");
        unregisterReceiver(logoutBroadcastReceiver);
        Runtime.getRuntime().gc();
        super.onDestroy();
    }

    class GuestListAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Log.d("GuestList", "[Door = " + appContext.getDoor().getId() + "]");
            FileAccess fileAccess = new FileAccess(mContext, Utils.GUEST_LIST_TYPE_OPTIONS_FILE);
            guestListItemType = fileAccess.read();
            DatabaseHandler databaseHandler = new DatabaseHandler(mContext);
            if (guestListItemType == null && fileAccess.FILE_NOT_FOUND) {
                fileAccess.write(String.valueOf(Utils.SHOW_ALL_GUESTS_EXCEPT_KEY_DELETED));
                guests = databaseHandler.getGuests(appContext.getDoor().getId(), Utils.SHOW_ALL_GUESTS_EXCEPT_KEY_DELETED);
                Log.d("GuestList", "GuestsExceptDeletedKeyLength:" + guests.size() + "[guestListItemType = NULL]");
            } else if (guestListItemType != null && !guestListItemType.isEmpty()) {
                guests = databaseHandler.getGuests(appContext.getDoor().getId(), Integer.parseInt(guestListItemType));
                /*if (Integer.parseInt(guestListItemType) == Utils.SHOW_ACTIVE_GUESTS_ONLY) {
                    guests = databaseHandler.getGuests(MainActivity.doorID, Utils.SHOW_ACTIVE_GUESTS_ONLY);
                    Log.d("GuestList", "ActiveGuestsLength:"+guests.size());
                } else {
                    guests = databaseHandler.getGuests(MainActivity.doorID);
                    Log.d("GuestList", "GuestsExceptDeletedKeyLength:"+guests.size());
                }*/
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter = new CustomAdapter<>(GuestListActivity.this, R.layout.guestlist, guests,
                            AdapterViewCode.GUEST_LIST_VIEW_CODE);
                    listView.setAdapter(adapter);
                    listView.setEmptyView(findViewById(R.id.empty));
                    adapter.notifyDataSetChanged();
                    if (progressDialog1 != null && progressDialog1.isShowing()) {
                        progressDialog1.dismiss();
                    }
                }
            });
            databaseHandler.close();
            return null;
        }
    }

    private void setAlternateOwner(String name, String mac, boolean shouldDeleteGuests, boolean shouldDeleteLogs) {
        Owner owner = new Owner();
        Door door = new Door();
        door.setId(appContext.getDoor().getId());
        door.setName(appContext.getDoor().getName());
        owner.setId(mac);
        owner.setName(name);
        owner.setAccessMode("owner");
        if (appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {
            Utils u = new Utils();
            u.requestType = Utils.OWNER_REQUEST;
            u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
            u.requestDirection = Utils.TCP_SEND_PACKET;
            //byte[] packet = new byte[MAX_PKT_SIZE];
            byte[] packet = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
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
            if ((shouldDeleteGuests && shouldDeleteLogs)) {
                packet[OwnerRegistrationPacket.DELETE_FLAG] = OwnerRegistrationPacket.DELETE_ALL_LOGS_AND_GUESTS;
            } else if ((shouldDeleteGuests)) {
                packet[OwnerRegistrationPacket.DELETE_FLAG] = OwnerRegistrationPacket.DELETE_ALL_GUESTS;
            } else if ((shouldDeleteLogs)) {
                packet[OwnerRegistrationPacket.DELETE_FLAG] = OwnerRegistrationPacket.DELETE_ALL_LOGS;
            } else {
                packet[OwnerRegistrationPacket.DELETE_FLAG] = OwnerRegistrationPacket.DELETE_NOTHING;
            }

            if (!name.isEmpty()) {
                //packet[OwnerRegistrationPacket.CHECKSUM_SENT] = u.calculateChecksum(packet, true);
                Utils.printByteArray(packet);
                try {
                    // value = new String(packet).getBytes("UTF-8");
                    // Log.d(TAG,"Handshake Packet Length:"+value.length);
                    mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                        @Override
                        public void onDataAvailable(String data) {
                            Log.d(TAG, "receivedData:" + data);
                            processAlternateOwnerPacket(data);
                        }
                    }, "Registering...");
                    Log.d(TAG, new String(packet, StandardCharsets.ISO_8859_1));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                u.commandDetails = new String(packet, StandardCharsets.ISO_8859_1);
            } else {
                Toast.makeText(mContext, "Name field can't be empty", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d("OwnerRegisterFrag", "Device Not Connected");
            Toast.makeText(mContext, "No connected device found", Toast.LENGTH_LONG).show();
        }
    }

    private void processAlternateOwnerPacket(String receivedPacket) {
        Log.d(TAG, "Processing received packet:" + receivedPacket);
        if (receivedPacket != null && receivedPacket.length() >= OwnerRegistrationPacket.RECEIVED_PACKET_LENGTH) {
            //strBytes = receivedPacket.getBytes(StandardCharsets.ISO_8859_1);
            if (receivedPacket.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.OWNER_REQUEST) {
                if (Utils.parseInt(receivedPacket, RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                    Log.d(TAG, "RegistrationPacket/COMMAND_STATUS_POS" +
                            Utils.parseInt(receivedPacket, RESPONSE_COMMAND_STATUS_POS));

                    if (receivedPacket.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                        Log.d(TAG, "RegistrationPacket SUCCESS");
                        new DatabaseHandler(mContext).delete(GuestDetailsFragment.guest, appContext.getDoor());
                        guests.remove(GuestDetailsFragment.guest);
                        adapter.notifyDataSetChanged();
                        Snackbar.make(findViewById(android.R.id.content), "Owner Successfully Changed", Snackbar.LENGTH_LONG).show();
                    } else if (receivedPacket.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                        Log.d(TAG, "[Error]: Owner's data cannot be inserted due to Device error");
                        Snackbar.make(findViewById(android.R.id.content), "Device Error", Snackbar.LENGTH_LONG).show();
                    }

                } else {
                    String errorMessage = CommunicationError.getMessage(
                            receivedPacket.charAt(RESPONSE_COMMAND_STATUS_POS));
                    Log.d(TAG, "CMD_STS_ERR:" + errorMessage);
                    Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
                }
            } else {
                Log.d(TAG, "RESPONSE_PACKET_TYPE_ERROR");
            }
        } else {
            /*Toast toast = Toast.makeText(mContext, "Invalid or Null Data", Toast.LENGTH_LONG);
            toast.show();*/
            Log.d(TAG, "Packet Received" + receivedPacket);
            Log.e(TAG, "Invalid or Null DoorMode" );
        }
    }

    private void changeOwner(Guest guest) {
        View registrationView = getLayoutInflater().inflate(R.layout.change_owner, null, false);
        nameTextView = registrationView.findViewById(R.id.alternate_owner_name_editText);
        altOwnerMacEditText = registrationView.findViewById(R.id.alternate_owner_mac_editText);
        pinEditText = registrationView.findViewById(R.id.pin_editText);
        CheckBox confirmCheckbox = registrationView.findViewById(R.id.alternate_phone_confirmation_checkBox);
        deleteGuestsCheckbox = registrationView.findViewById(R.id.delete_guests_checkBox);
        deleteLogsCheckbox = registrationView.findViewById(R.id.del_log_checkBox);
        ImageView guestImageView = registrationView.findViewById(R.id.guest_imageView);

        nameTextView.setText(guest.getName());
        altOwnerMacEditText.setText(guest.getId());

        /* set guest image */
        Bitmap image = guest.getImage();
        RoundedImageView roundedImageView = new RoundedImageView(mContext);
        Bitmap conv_bm = null;
        DisplayMetrics displayMetrics = Utils.getDisplayMetrics(this);
        if (displayMetrics.widthPixels == 1080) {
            conv_bm = roundedImageView.getCroppedBitmap(ImageUtility.resize(image), 160, 1, Color.WHITE);
        } else if (displayMetrics.widthPixels == 720) {
            conv_bm = roundedImageView.getCroppedBitmap(ImageUtility.resize(image), 100, 1, Color.WHITE);
        } else if (displayMetrics.widthPixels < 720) {
            conv_bm = roundedImageView.getCroppedBitmap(ImageUtility.resize(image), 70, 1, Color.WHITE);
        }
        guestImageView.setImageBitmap(conv_bm);

        dialog = new AlertDialog.Builder(mContext)
                .setView(registrationView)
                .setCancelable(false)
                .setPositiveButton("MAKE OWNER", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create();

        confirmCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(isChecked);
            }
        });
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {
                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        });
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = nameTextView.getText().toString();
                mac = altOwnerMacEditText.getText().toString();
                pin = pinEditText.getText().toString();
                shouldDeleteGuests = deleteGuestsCheckbox.isChecked();
                shouldDeleteLogs = deleteLogsCheckbox.isChecked();
                Log.d(TAG, "Entered Pin:" + pin);

                if (name.isEmpty() && mac.isEmpty()) {
                    Toast.makeText(mContext, "Enter Name and IMEI", Toast.LENGTH_LONG).show();
                } else if (name.isEmpty()) {
                    Toast.makeText(mContext, "Enter Name", Toast.LENGTH_LONG).show();
                } else if (mac.isEmpty()) {
                    Toast.makeText(mContext, "Enter IMEI", Toast.LENGTH_LONG).show();
                } else if (pin.isEmpty()) {
                    Toast.makeText(mContext, "Enter PIN", Toast.LENGTH_LONG).show();
                } else {
                    String savedPin = appContext.getPin();
                    Log.d(TAG, "Saved Pin:" + savedPin);
                    if (savedPin.equals(pin)) {
                        Log.d(TAG, "PIN OK");
                        dialog.dismiss();
                        setAlternateOwner(name, mac, shouldDeleteGuests, shouldDeleteLogs);
                    } else {
                        Toast.makeText(mContext, "Wrong PIN", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public void onChangeOwnerSelected(Guest guest) {
        changeOwner(guest);
    }
}

package com.asiczen.azlock;

import android.Manifest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import android.os.Build;
import android.os.Bundle;

import android.os.Handler;
import android.provider.Settings;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;


import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.LogoutBroadcastReceiver;
import com.asiczen.azlock.content.MySharedPreferences;
import com.asiczen.azlock.content.SessionManager;
import com.asiczen.azlock.net.VolleyRequest;
import com.asiczen.azlock.net.VolleyResponse;
import com.asiczen.azlock.security.CryptoUtils;
import com.asiczen.azlock.util.HttpAsyncTask;
import com.asiczen.azlock.util.Utils;
import org.json.JSONObject;
import java.util.Timer;
import java.util.TimerTask;

import static com.asiczen.azlock.net.VolleyRequest.STATUS;
import static com.asiczen.azlock.net.VolleyRequest.STATUS_FAIL;


public class MainActivity extends AppCompatActivity implements SessionManager.OnSessionBroadcastListener, HttpAsyncTask.AsyncResponse {

    private final CryptoUtils decode = new CryptoUtils();
    @SuppressLint("StaticFieldLeak")
    public static Context mContext;
    private AppContext appContext;
    private LogoutBroadcastReceiver logoutBroadcastReceiver;
    private IntentFilter intentFilter;
    private final String TAG = MainActivity.class.getSimpleName();
    private boolean isAboveVersion6;
    private static final int REQUEST_PERMISSION = 202;
    private final Handler handler = new Handler();

    private ProgressBar progressBar;
    /*String URL="readpin.php";
    String URL_POST=BuildConfig.Port+URL;*/
    private String URL_POST;
    private SessionManager sessionManager;


    native String[] getInfo();

    static {
        System.loadLibrary("ndklink");
    }


    MySharedPreferences onBoradScreenStatus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        mContext = this;

        // get Application Context
        appContext = AppContext.getContext();
        // create session to detect logout, disconnection or exit
        //sessionManager=new SessionManager(mContext, this);
        // create logout broadcast receiver to receive events
        sessionManager = new SessionManager(this);
        logoutBroadcastReceiver = new LogoutBroadcastReceiver(this);
        // Intent filter for logout receiver
        intentFilter = new IntentFilter();
        intentFilter.addAction(SessionManager.ACTION_LOGOUT);
        intentFilter.addAction(SessionManager.ACTION_EXIT);

        // check the android version
        isAboveVersion6 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
        // update pin status to Application Context
        appContext.checkPinStatus(mContext);

        /*handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                onClickContinueButton();
            }
        }, 2000);*/
        progressBar=findViewById(R.id.progressBar_cyclic);
        progressBar.setVisibility(View.GONE);
      

        //calling of  native c file
        MainActivity m = new MainActivity();
        String[] days = m.getInfo();
       /* for(int i=0;i<days.length;i++){
            Log.d(TAG,"STRING IS "+days[i]);
        }*/
        appContext.setData(days);
        URL_POST = AppContext.getIp_address() + AppContext.getReadpin_url();
        /*String data=BuildConfig.key;
    private byte[] key=AppContext.hexStringToByteArray(data);*/
        byte[] key = AppContext.getKey();
        decode.generateKey(key);

        onBoradScreenStatus = new MySharedPreferences(getApplicationContext());


        if(ConnectActivity.actionToPerform != -1){
            onClickContinueButton();
        }else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onClickContinueButton();
                }
            }, 2000);
        }

    }

    private void onClickContinueButton() {
        if (isAboveVersion6) {
            // if android version is above 6.0 then we need to check dangerous permissions.
            boolean locationPermission = (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
            /*, smsPermission*/
            /*boolean readPhoneStatePermission = (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED);*/
            /* SEND_SMS permission is commented  because google stope the service
             * of SEND_SMS and Call log
             * */
            /*smsPermission = (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.SEND_SMS)== PackageManager.PERMISSION_GRANTED);*/

            // if required permission is not set then request user to set it.
            if (!locationPermission /*|| !readPhoneStatePermission*/ /*|| !smsPermission*/ || !isLocationEnabled(this)) {
                Intent intent = new Intent(MainActivity.this, RequestPermissionActivity.class);
                startActivityForResult(intent, REQUEST_PERMISSION);
            } else {
                // if permission is set, then go forward.
                forward();
            }
        } else {
            // if permission is not required, then go forward.
            forward();
        }
    }

    private static boolean isLocationEnabled(Context context) {
        int locationMode;
        //String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }
        return false;
    }

    @SuppressLint("HardwareIds")
    private void forward() {
        if (appContext.shouldConfigPin()) {
            
            progressBar.setVisibility(View.VISIBLE);

            JSONObject object = new JSONObject();
            //commented for android Q compatibility
            /*TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                appContext.setImei(tm.getImei());
                Log.d(TAG, "IMEI: "+appContext.getImei()+" "+tm.getImei());
            }else{
                appContext.setImei(tm.getDeviceId());
                Log.d(TAG, "IMEI: "+appContext.getImei());
            }*/
            String mac = onBoradScreenStatus.getMac(); //get the mac from SharedPreferences
            Log.d(TAG, "forward: "+mac);
            try {
                object.put("appKey",mac);
            } catch (Exception e) {
                e.printStackTrace();
            }
            /*HttpAsyncTask.context=this;
            HttpAsyncTask httpTask = new HttpAsyncTask();
            httpTask.delegate = this;
            Log.d(TAG,"hello "+object.toString());
            httpTask.execute(URL_POST,object.toString());*/
            VolleyRequest.jsonObjectRequest(this, URL_POST, object, Request.Method.POST, new VolleyResponse() {
                @Override
                public void VolleyError(VolleyError error) {
                    if (progressBar!=null)
                        progressBar.setVisibility(View.GONE);
                    //Toast.makeText(MainActivity.this, VolleyErrors.error(error), Toast.LENGTH_SHORT).show();
                    final  AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Try again");
                    builder.setMessage("Unable to contact server please check your internet connection");
                    builder.setCancelable(true);
                    builder.setNegativeButton("Ok", new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface d, int arg1) {
                            d.cancel();
                            sessionManager.exit();
                        }
                    });
                    final AlertDialog closedialog= builder.create();

                    closedialog.show();

                    final Timer timer2 = new Timer();
                    timer2.schedule(new TimerTask() {
                        public void run() {
                            closedialog.dismiss();
                            timer2.cancel();
                            sessionManager.exit();//this will cancel the timer of the system
                        }
                    }, 5000);
                }

                @Override
                public void VolleyObjectResponse(JSONObject response) {
                    Log.d(TAG, "VolleyObjectResponse: "+response.toString());
                    if (progressBar!=null)
                        progressBar.setVisibility(View.GONE);
                    try {
                        if (response.getString(STATUS).equals(STATUS_FAIL)){
                            Intent intent = new Intent(MainActivity.this, ConfigPinActivity.class);
                            intent.putExtra("pinFlag", Utils.NEW_PIN_FLAG);
                            startActivity(intent);
                        }else {
                            byte[] packet = Base64.decode(response.getString(STATUS), Base64.DEFAULT);
                            String pin = "";
                            try {
                                byte[] val = decode.AESEncode(packet);
                                pin = new String(val);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Log.d(TAG,"pin="+pin);
                            appContext.savePin(MainActivity.this,pin.substring(0,4),true);
                            Intent intent = new Intent(MainActivity.this, AskPinActivity.class);
                            intent.putExtra("flag", "0");
                            startActivity(intent);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        else if(appContext.shouldAskPin())
        {
            // PIN feature enabled
            // PIN is already configured. Authenticate user by asking for PIN
            Intent intent = new Intent(MainActivity.this, AskPinActivity.class);
            intent.putExtra("flag", "0");
            startActivity(intent);
        }
        else
        {
            // user disabled the PIN feature, redirect to connection page
            Intent intent = new Intent(MainActivity.this, ConnectActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, REQUEST_PERMISSION + " " + resultCode);
        if (requestCode == REQUEST_PERMISSION) {
            if (resultCode == RESULT_CANCELED) {
                finish();
            } else {
                forward();
            }
        }
    }

    @Override public void onLogout()
    {
        Log.e(TAG, "Logging out...");
    }

    @Override
    public void onExit()
    {
        Log.d(TAG, "Exiting...");
        /*Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);*/
        finish();
    }

    public void onStart()
    {
        super.onStart();
        // register logout receiver whenever this activity starts execution
        registerReceiver(logoutBroadcastReceiver, intentFilter);
    }

    public void onDestroy()
    {
        // unregister logout receiver
        unregisterReceiver(logoutBroadcastReceiver);
        super.onDestroy();
    }

    public void onBackPressed()
    {
        super.onBackPressed();
        onExit();
    }

    @Override
    public void processFinish(String output,int errorCode) {
       
        progressBar.setVisibility(View.GONE);
        if(output.equals("N")) {
            Intent intent = new Intent(MainActivity.this, ConfigPinActivity.class);
            intent.putExtra("pinFlag", Utils.NEW_PIN_FLAG);
            startActivity(intent);
        }
        else if(output.equals("T")){
            final  AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Try again");
            builder.setMessage("Unable to contact server please check your internet connection");
            builder.setCancelable(true);
            builder.setNegativeButton("Ok", new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface d, int arg1) {
                    d.cancel();
                    sessionManager.exit();
                }
            });
            final AlertDialog closedialog= builder.create();

            closedialog.show();

            final Timer timer2 = new Timer();
            timer2.schedule(new TimerTask() {
                public void run() {
                    closedialog.dismiss();
                    timer2.cancel();
                    sessionManager.exit();//this will cancel the timer of the system
                }
            }, 5000);
            //Toast.makeText(this, "Unable to contact server", Toast.LENGTH_LONG).show();
        }
        else {
            byte[] packet = Base64.decode(output, Base64.DEFAULT);
            String pin = "";
            try {
                byte[] val = decode.AESEncode(packet);
                pin = new String(val);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //Log.d(TAG,"pin="+pin);
            appContext.savePin(this,pin.substring(0,4),true);
            Intent intent = new Intent(MainActivity.this, AskPinActivity.class);
            intent.putExtra("flag", "0");
            startActivity(intent);
        }
    }

}

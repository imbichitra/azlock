package com.asiczen.azlock;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.asiczen.azlock.content.MySharedPreferences;
import com.asiczen.azlock.net.VolleyErrors;
import com.asiczen.azlock.net.VolleyRequest;
import com.asiczen.azlock.net.VolleyResponse;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.azlock.app.Validate;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.ConcreteValidator;
import com.asiczen.azlock.content.CustomTextWatcher;
import com.asiczen.azlock.content.LogoutBroadcastReceiver;
import com.asiczen.azlock.content.SessionManager;
import com.asiczen.azlock.content.Validator;
import com.asiczen.azlock.security.CryptoUtils;
import com.asiczen.azlock.util.HttpAsyncTask;
import com.asiczen.azlock.util.Utils;

import org.json.JSONObject;
import static com.asiczen.azlock.net.VolleyRequest.STATUS;
import static com.asiczen.azlock.net.VolleyRequest.STATUS_SUCCESS;

/**
 * Created by Somnath on 1/9/2017.
 */

public class ConfigPinActivity extends AppCompatActivity implements SessionManager.OnSessionBroadcastListener,HttpAsyncTask.AsyncResponse{
   /* String data=BuildConfig.key;
    private byte[] key=AppContext.hexStringToByteArray(data);*/
   private final byte[] key=AppContext.getKey();
    private final CryptoUtils encode = new CryptoUtils(key);
    private Context mContext;
    private AppContext appContext;
    private SessionManager sessionManager;
    private IntentFilter intentFilter;
    private LogoutBroadcastReceiver logoutBroadcastReceiver;
    private final String TAG = ConfigPinActivity.class.getSimpleName();
    private EditText pinTextView, confirmPinTextView;
    private TextInputLayout inputLayoutPin, inputLayoutconfirmPin;
    private CheckBox askPinCheckbox;
    private Validator validator;
    private int pinFlag;
    // private ProgressDialog pdialog;
    private TextView dialogTextView;
    private AlertDialog dialog;

   /* String URL="writepin.php";
    String URL_POST=BuildConfig.Port+URL;*/
    private final String URL_POST=AppContext.getIp_address()+AppContext.getWritepin_url();
    private boolean isChecked;
    private String pin;
    public static boolean callFrmFH=false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_pin);
        mContext = this;
        appContext = AppContext.getContext();
        pinFlag=getIntent().getIntExtra("pinFlag",-1);
        sessionManager=new SessionManager(this, this);
        logoutBroadcastReceiver = new LogoutBroadcastReceiver(this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(SessionManager.ACTION_EXIT);

        pinTextView =  findViewById(R.id.input_pin);
        confirmPinTextView =  findViewById(R.id.input_confirm_pin);
        inputLayoutPin =  findViewById(R.id.input_layout_pin);
        inputLayoutconfirmPin =  findViewById(R.id.input_layout_confirm_pin);
        askPinCheckbox =  findViewById(R.id.ask_pin_checkBox);

        confirmPinTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    onClickContinueButton(null);
                }
                return true;
            }
        });

        ProgressBar uiStatusProgressBar =  findViewById(R.id.registering_progressBar3);
        uiStatusProgressBar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);

        validator = new ConcreteValidator(this);
        pinTextView.addTextChangedListener(new CustomTextWatcher(this, pinTextView, inputLayoutPin, validator));
        confirmPinTextView.addTextChangedListener(new CustomTextWatcher(this, confirmPinTextView, inputLayoutconfirmPin, validator));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View bridgeConnectView= getLayoutInflater().inflate(R.layout.progressbar, null,false);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setView(bridgeConnectView);
        dialogTextView=bridgeConnectView.findViewById(R.id.progressDialog);
        dialog = builder.create();
        if(callFrmFH)
            inputLayoutPin.setHint("Enter New Pin");

    }
    public void onClickContinueButton(View v){
        if(!validator.validate(Validate.PIN, inputLayoutPin, pinTextView))
        {
            return;
        }
        if(!validator.validate(Validate.PIN, inputLayoutconfirmPin, confirmPinTextView))
        {
            return;
        }
        pin=pinTextView.getText().toString();
        String confirmPin=confirmPinTextView.getText().toString();

        isChecked=askPinCheckbox.isChecked();
        if(pin != null && !pin.isEmpty() && !confirmPin.isEmpty()) {
            if(!pin.equals(confirmPin)){
                inputLayoutconfirmPin.setError("Pin doesn't match");
                //Snackbar.make(findViewById(android.R.id.content),"Pin doesn't match",Snackbar.LENGTH_LONG).show();
                return;
            }
            byte[] packet = new byte[16];
            byte[] password = pin.getBytes();
            System.arraycopy(password, 0, packet, 0, password.length);
            Utils.printByteArray(packet);
            try {
                packet = encode.AESEncode(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String encData= Base64.encodeToString(packet,Base64.DEFAULT);
            savePinToServer(encData);
            /*appContext.savePin(mContext, pin, isChecked);
            //appContext.savePinToFile(encData);
            if(pinFlag==Utils.NEW_PIN_FLAG) {
                Intent intent = new Intent(ConfigPinActivity.this, ConnectActivity.class);
                startActivity(intent);
            }
            else if(pinFlag==Utils.CHANGE_PIN_FLAG){
                Toast.makeText(mContext,"PIN changed successfully",Toast.LENGTH_LONG).show();
                finish();
            }*/
        }
    }
    private void savePinToServer(final String encodePin){
       dialogTextView.setText(R.string.connecting);
        dialog.show();
        JSONObject object= new JSONObject();
        MySharedPreferences sharedPreferences = new MySharedPreferences(this);
        try {
            object.put("appKey",sharedPreferences.getMac());
            object.put("pin",encodePin);

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
                if (dialog!=null && dialog.isShowing())
                    dialog.dismiss();
                Toast.makeText(ConfigPinActivity.this, VolleyErrors.error(error), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void VolleyObjectResponse(JSONObject response) {
                Log.d(TAG, "VolleyObjectResponse: "+response.toString());
                if (dialog!=null && dialog.isShowing())
                    dialog.dismiss();
                try {
                    if (response.getString(STATUS).equals(STATUS_SUCCESS)){
                        appContext.savePin(mContext, pin, isChecked);
                        if(pinFlag==Utils.NEW_PIN_FLAG) {

                            Intent intent = new Intent(ConfigPinActivity.this, ConnectActivity.class);
                            startActivity(intent);
                        }
                        else if(pinFlag==Utils.CHANGE_PIN_FLAG){
                            Intent intent = new Intent(ConfigPinActivity.this, ConnectActivity.class);
                            startActivity(intent);
                            Toast.makeText(mContext,"PIN changed successfully",Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    public void onBackPressed()
    {
        super.onBackPressed();
        if(pinFlag==Utils.NEW_PIN_FLAG) {
            sessionManager.exit();
        }
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
        if(logoutBroadcastReceiver!=null) {
            unregisterReceiver(logoutBroadcastReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onLogout() {
        finish();
    }

    @Override
    public void onExit() {
        finish();
    }


    @Override
    public void processFinish(String output,int errorCode) {
        dialog.dismiss();
        if(output.equals("Y")){
            appContext.savePin(mContext, pin, isChecked);
            if(pinFlag==Utils.NEW_PIN_FLAG) {

                Intent intent = new Intent(ConfigPinActivity.this, ConnectActivity.class);
                startActivity(intent);
            }
            else if(pinFlag==Utils.CHANGE_PIN_FLAG){
                Intent intent = new Intent(ConfigPinActivity.this, ConnectActivity.class);
                startActivity(intent);
                Toast.makeText(mContext,"PIN changed successfully",Toast.LENGTH_LONG).show();
                finish();
            }
        }
        else {
            Toast.makeText(mContext, "Unable to contact server", Toast.LENGTH_SHORT).show();
        }
    }
}

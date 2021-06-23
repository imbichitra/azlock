package com.asiczen.azlock;


import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.content.MySharedPreferences;
import com.asiczen.azlock.net.VolleyErrors;
import com.asiczen.azlock.net.VolleyRequest;
import com.asiczen.azlock.net.VolleyResponse;
import com.asiczen.azlock.security.CryptoUtils;
import com.asiczen.azlock.util.HttpAsyncTask;
import com.asiczen.azlock.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.asiczen.azlock.net.VolleyRequest.STATUS;
import static com.asiczen.azlock.net.VolleyRequest.STATUS_SUCCESS;


public class userlogin extends AppCompatActivity implements HttpAsyncTask.AsyncResponse{
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;

    /*String URL="login.php";
    String URL_POST=BuildConfig.Port+URL;*/
    private final String URL_POST=AppContext.getIp_address()+AppContext.getLogin_url();
    private EditText _passwordText;
    //private ProgressDialog pdialog;

    /*String data=BuildConfig.key;
    private byte[] key= AppContext.hexStringToByteArray(data);*/
    private final byte[] key=AppContext.getKey();
    private final CryptoUtils encode = new CryptoUtils(key);
    private AutoCompleteTextView _emailText;
    private DatabaseHandler databaseHandler ;
    //public static String issueRaise="NO";

    private TextView dialogTextView;
    private AlertDialog dialog;

    /*public final static int RAISE_ISSUE = 0;
    public final static int RESET_DEVICE = 1;*/
    public final static int GET_IMEI = 2;
    public int COMMAND_TO_PROCESS =-1;
    public static final String Command = "process_request";

    TextView errorEmail,errorPassword;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
            actionBar.setTitle(Html.fromHtml("<font color='#FFFFFF'>User Login</font>"));
            //actionBar.setDisplayHomeAsUpEnabled(true);
        }
        errorEmail = findViewById(R.id.errorEmail);
        errorPassword = findViewById(R.id.errorPassword);
        Intent mIntent = getIntent();
        COMMAND_TO_PROCESS = mIntent.getIntExtra(Command, -1);

        _passwordText=findViewById(R.id.input_password);
        Button _loginButton =  findViewById(R.id.btn_login);
        TextView _signupLink =  findViewById(R.id.link_signup);
        TextView forgot_password = findViewById(R.id.passwd_text);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View bridgeConnectView= getLayoutInflater().inflate(R.layout.progressbar, Utils.nullParent,false);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setView(bridgeConnectView);
        dialogTextView=bridgeConnectView.findViewById(R.id.progressDialog);
        dialog = builder.create();

        _emailText=findViewById(R.id.input_email);
        _emailText.setThreshold(1);
        databaseHandler = new DatabaseHandler(this);
        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                boolean b=validate();
                Log.d(TAG,"validate is :"+b);
                if(b){
                    dialogTextView.setText(R.string.connecting);
                    dialog.show();
                    sendPostDataToServer();
                }

            }
        });

        _signupLink.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(getApplicationContext(),SignupActivity.class);
                startActivityForResult(intent, REQUEST_SIGNUP);
                //finish();
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });
        forgot_password.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),ForgotPassword.class);
                startActivity(intent);
                //finish();
            }
        });

        List<String> auto = databaseHandler.getData();
        if(!auto.isEmpty()) {
            String[] ss = new String[auto.size()];
            for(int i = 0; i< auto.size(); i++){
                ss[i]= auto.get(i);
            }
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, ss);
            _emailText.setAdapter(arrayAdapter);
        }
    }



    @Override
    public void onBackPressed() {
        // Disable going back to the MainActivity
        finish();
    }

    private boolean validate() {
        boolean valid = true;
        String email = _emailText.getText().toString();
        databaseHandler.insertData(email);
        String password = _passwordText.getText().toString();
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            //_emailText.setError("Enter a valid email address");
            errorEmail.setVisibility(View.VISIBLE);
            errorEmail.setText("Enter a valid email address.");
            valid = false;
        } else {
            errorEmail.setVisibility(View.GONE);
            _emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            errorPassword.setVisibility(View.VISIBLE);
            if (password.isEmpty()){
                errorPassword.setText("Please Enter password.");
            }else{
                errorPassword.setText("Password must be in between 4 to 10 alphanumeric characters.");
            }
            valid = false;
        } else {
            errorPassword.setVisibility(View.GONE);
        }
        return valid;
    }

    private  void sendPostDataToServer(){
        JSONObject object= new JSONObject();
        String data;
        try {
            String text=_emailText.getText().toString();

                byte[] packet = new byte[16];
                byte[] password = _passwordText.getText().toString().getBytes();
                System.arraycopy(password, 0, packet, 0, password.length);
                Utils.printByteArray(packet);
                packet = encode.AESEncode(packet);
                Utils.printByteArray(packet);
                //data = new String(packet);
                data=bytesToHexString(packet);
                Log.d(TAG, "data :" + data);

                object.put("user_id",text);
                object.put("password",data);
        } catch (Exception e) {
            e.printStackTrace();
        }


        /*HttpAsyncTask.context=this;
        HttpAsyncTask httpTask = new HttpAsyncTask();
        httpTask.delegate = this;
        Log.d(TAG,"hello "+object.toString());
        httpTask.execute(URL_POST,object.toString(),"GET_IMEI");*/
        VolleyRequest.jsonObjectRequest(this, URL_POST, object, Request.Method.POST, new VolleyResponse() {
            @Override
            public void VolleyError(VolleyError error) {
                if (dialog!=null && dialog.isShowing())
                    dialog.dismiss();
                Toast.makeText(userlogin.this, VolleyErrors.error(error), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void VolleyObjectResponse(JSONObject response) {
                Log.d(TAG, "VolleyObjectResponse: "+response.toString());
                if (dialog!=null && dialog.isShowing())
                    dialog.dismiss();
                try {
                    if (response.getString(STATUS).equals(STATUS_SUCCESS)){
                        String email = _emailText.getText().toString();
                        String password = _passwordText.getText().toString();
                        loginData.setUserId(email);
                        loginData.setPassword(password);
                        MySharedPreferences sharedPreferences = new MySharedPreferences(userlogin.this);
                        String mac = Utils.getUserId(response.getString("appkey"));
                        sharedPreferences.setUserDetail(email,password,mac);
                        if(sharedPreferences.getValues(MySharedPreferences.MOB_NO).isEmpty()){
                            gotoSendOtpActivity();
                        }else{
                            goToMainActivity();
                        }
                    }else {
                        Toast.makeText(userlogin.this, "Invalid user id or password", Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }



    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes){
         sb.append(String.format("%02x", b&0xff));
        }
        return sb.toString();
    }


    @Override
    public void processFinish(String output,int errorCode) {
        if (output.contains("Y")){
                Log.d(TAG, "valid user");
                String email = _emailText.getText().toString();
                String password = _passwordText.getText().toString();
                loginData.setUserId(email);
                loginData.setPassword(password);
                dialog.dismiss();

               /*if(issueRaise.equals("NO")) {
                   Intent intent = new Intent(this, Resetactivity.class);
                   startActivity(intent);
               }else{
                   issueRaise="NO";
                   Intent intent = new Intent(this, RaiseIssue.class);
                   startActivity(intent);
               }*/
        }else if(output.equals("N")){
            Log.d(TAG,"Invalid user id or password");
            dialog.dismiss();
            Toast.makeText(this, "Invalid user id or password", Toast.LENGTH_LONG).show();
        }else{
            dialog.dismiss();
            Log.d(TAG,"Time out to connect server");
            Toast.makeText(this, "Unable to contact server", Toast.LENGTH_LONG).show();
        }
    }

    /*private void retriveImei(String output, String email, String password) {
        Log.d(TAG, "retriveImei: "+output);
        String mac = Utils.getUserId(output.substring(2));
        Log.d(TAG, "retriveImei:mac "+mac);
        MySharedPreferences sharedPreferences = new MySharedPreferences(this);
        sharedPreferences.setUserDetail(email,password,mac);
        goToMainActivity();
        finish();
    }*/
    private void goToMainActivity(){
        Intent logIn = new Intent(userlogin.this, MainActivity.class);
        startActivity(logIn);
        finish();
    }

    private void gotoSendOtpActivity() {
        Intent otpSend = new Intent(userlogin.this, SendOtpActivity.class);
        startActivity(otpSend);
        finish();
    }
    /*private void goToRaiseIssue() {
        Intent intent = new Intent(this, RaiseIssue.class);
        startActivity(intent);
    }*/

    /*private void gotToResetactivity() {
        Intent intent = new Intent(this, Resetactivity.class);
        startActivity(intent);
    }*/

    public void goBack(View view) {
        super.onBackPressed();
    }

    ArrayList<Integer> l = new ArrayList<Integer>();
    public void setVisibility(View view) {
        if(l.size()==0) {
            l.add(0);
            _passwordText.setInputType(InputType.TYPE_CLASS_TEXT);
        }else {
            l.clear();
            _passwordText.setInputType(InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }
}

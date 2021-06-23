package com.asiczen.azlock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.net.VolleyErrors;
import com.asiczen.azlock.net.VolleyRequest;
import com.asiczen.azlock.net.VolleyResponse;
import com.asiczen.azlock.util.HttpAsyncTask;

import org.json.JSONObject;

import static com.asiczen.azlock.net.VolleyRequest.STATUS;
import static com.asiczen.azlock.net.VolleyRequest.STATUS_SUCCESS;

public class ForgotPassword extends AppCompatActivity implements HttpAsyncTask.AsyncResponse{
    /*String data=BuildConfig.key;
    private byte[] key= AppContext.hexStringToByteArray(data);*/
    //private final byte[] key=AppContext.getKey();
    private static final String TAG = "ForgotPassword";
    private ProgressDialog pdialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        final EditText id=findViewById(R.id.input_email);
        Button bt=findViewById(R.id.button3);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email=id.getText().toString();
                if(email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){

                    id.setError("Enter a valid email address");
                }
                else{
                    sendData(email);
                }
            }
        });
        pdialog=new ProgressDialog(this);
        pdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pdialog.setCancelable(false);


        TextView login = findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ForgotPassword.this,userlogin.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void sendData(String email){
        pdialog.setMessage("Connecting...");
        pdialog.show();
        JSONObject object= new JSONObject();
        try {
            object.put("user_id",email);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*String URL="forgot_password.php";
        String URL_POST=BuildConfig.Port+URL;*/
        String URL_POST=AppContext.getIp_address()+AppContext.getForgot_password_url();
        /*HttpAsyncTask.context=this;
        HttpAsyncTask httpTask = new HttpAsyncTask();
        httpTask.delegate = this;
        Log.d(TAG,"hello "+object.toString());
        httpTask.execute(URL_POST,object.toString());*/
        VolleyRequest.jsonObjectRequest(this, URL_POST, object, Request.Method.POST, new VolleyResponse() {
            @Override
            public void VolleyError(VolleyError error) {
                if (pdialog!=null && pdialog.isShowing())
                    pdialog.dismiss();
                Toast.makeText(ForgotPassword.this, VolleyErrors.error(error), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void VolleyObjectResponse(JSONObject response) {
                Log.d(TAG, "VolleyObjectResponse: "+response.toString());
                if (pdialog!=null && pdialog.isShowing())
                    pdialog.dismiss();
                try {
                    if (response.getString(STATUS).equals(STATUS_SUCCESS)){
                        Toast.makeText(ForgotPassword.this, "Reset Password is send to your email", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ForgotPassword.this, userlogin.class);
                        startActivity(intent);
                        finish();
                    }else {
                        Toast.makeText(ForgotPassword.this, "email id does not exist", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void processFinish(String output,int errorCode) {
        if (output.equals("Y")){
            Log.d(TAG, "valid user");
            pdialog.dismiss();
            Toast.makeText(this, "Reset Password is send to your email", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, userlogin.class);
            startActivity(intent);
            finish();
        }else if(output.equals("N")){
            Log.d(TAG,"Invalid User Id or password");
            pdialog.dismiss();
            Toast.makeText(this, "email id does not exist", Toast.LENGTH_SHORT).show();
        }else{
            pdialog.dismiss();
            Log.d(TAG,"Time out to connect server");
            Toast.makeText(this, "Unable to contact server", Toast.LENGTH_LONG).show();
        }
    }
}

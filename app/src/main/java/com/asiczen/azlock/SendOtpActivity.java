package com.asiczen.azlock;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.MySharedPreferences;
import com.asiczen.azlock.net.VolleyErrors;
import com.asiczen.azlock.net.VolleyRequest;
import com.asiczen.azlock.net.VolleyResponse;
import com.asiczen.azlock.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import static com.asiczen.azlock.net.VolleyRequest.STATUS;
import static com.asiczen.azlock.net.VolleyRequest.STATUS_SUCCESS;

public class SendOtpActivity extends AppCompatActivity {
    String mobileNo="";
    MySharedPreferences mySharedPreferences;
    ProgressBar progressBar;
    Button buttonGetOtp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_otp);
        progressBar = findViewById(R.id.progress_bar);

        mySharedPreferences = new MySharedPreferences(this);

        final EditText inputMobile = findViewById(R.id.input_number);
        buttonGetOtp = findViewById(R.id.buttonGetOpt);
        final String regex = "(0/91)?[6-9][0-9]{9}";
        buttonGetOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(inputMobile.getText().toString().trim().isEmpty()){
                    Toast.makeText(SendOtpActivity.this, "Enter Mobile", Toast.LENGTH_SHORT).show();
                    return;
                }else {
                    mobileNo = inputMobile.getText().toString().trim();
                    if(mobileNo.matches(regex)){
                        sendOtp(mobileNo);
                    }else {
                        Toast.makeText(SendOtpActivity.this, "Please enter a valid phone number.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    void sendOtp(final String no){
        progressBar.setVisibility(View.VISIBLE);
        buttonGetOtp.setVisibility(View.GONE);
        JSONObject object = new JSONObject();
        try {
            object.put(	"mobile_no",no);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = "https://13.127.109.11/Azlock/verify_no.php";
        VolleyRequest.jsonObjectRequest(this, url,object,Request.Method.POST, new VolleyResponse() {
            @Override
            public void VolleyError(VolleyError error) {
                progressBar.setVisibility(View.GONE);
                buttonGetOtp.setVisibility(View.VISIBLE);
                Toast.makeText(SendOtpActivity.this, VolleyErrors.error(error), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void VolleyObjectResponse(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                buttonGetOtp.setVisibility(View.VISIBLE);
                Log.d("SendOtp", "VolleyObjectResponse: "+response.toString());
                try {
                    String otp = response.getString("otp");
                    Intent intent = new Intent(SendOtpActivity.this,VerifyOtpActivity.class);
                    intent.putExtra("otp",otp);
                    intent.putExtra("number",no);
                    startActivity(intent);
                    //finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
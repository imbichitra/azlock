package com.asiczen.azlock;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.asiczen.azlock.content.MySharedPreferences;
import com.asiczen.azlock.net.VolleyErrors;
import com.asiczen.azlock.net.VolleyRequest;
import com.asiczen.azlock.net.VolleyResponse;
import com.chaos.view.PinView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class VerifyOtpActivity extends AppCompatActivity {

    public static final String TAG = VerifyOtpActivity.class.getSimpleName();
    private TextView timer,textResendOTP;
    String otp;
    MySharedPreferences mySharedPreferences;
    String number;
    PinView pinView;
    private int timeInMilliSeconds = 1000 * 60;
    private CountDownTimer countDownTimer;
    int min =0;
    int max =60;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);
        timer = findViewById(R.id.time);
        mySharedPreferences = new MySharedPreferences(this);

        number = getIntent().getStringExtra("number");
        pinView = findViewById(R.id.pinview);
        textResendOTP = findViewById(R.id.textResendOTP);

        TextView textMobile = findViewById(R.id.textMobile);
        textMobile.setText(String.format(
                "+91-%s",number
        ));

        otp = getIntent().getStringExtra("otp");
        countDown();
    }

    private void countDown(){
        textResendOTP.setVisibility(View.GONE);
        timer.setVisibility(View.VISIBLE);
        countDownTimer = new CountDownTimer(timeInMilliSeconds,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                min++;
                Log.d(TAG, "onTick: "+min);
                updateProgress(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                textResendOTP.setVisibility(View.VISIBLE);
                timer.setVisibility(View.GONE);
            }
        }.start();
    }
    @SuppressLint("DefaultLocale")
    private void updateProgress(long milliSeconds){
        // long minutes = (milliseconds / 1000) / 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliSeconds);

        // long seconds = (milliseconds / 1000)%60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliSeconds)-TimeUnit.MILLISECONDS.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliSeconds));
        timer.setText(String.format("%02d:%02d", minutes,seconds));
    }
    void sendOtp(String no){
        JSONObject object = new JSONObject();
        try {
            object.put(	"mobile_no",no);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String url = "https://13.127.109.11/Azlock/verify_no.php";
        VolleyRequest.jsonObjectRequest(this, url,object, Request.Method.POST, new VolleyResponse() {
            @Override
            public void VolleyError(VolleyError error) {

                Toast.makeText(VerifyOtpActivity.this, VolleyErrors.error(error), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void VolleyObjectResponse(JSONObject response) {
                Log.d("SendOtp", "VolleyObjectResponse: "+response.toString());
                try {
                    otp = response.getString("otp");
                    Toast.makeText(VerifyOtpActivity.this, "Otp sent successfully", Toast.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public void verify(View view) {
        Log.d(TAG, "verify: "+pinView.getText().toString());
        String otp1= pinView.getText().toString();
        Log.d(TAG, "verify: "+otp);
        Log.d(TAG, "verify1: "+otp1);

        if (otp1.equals(otp)){
            mySharedPreferences.setValues(MySharedPreferences.MOB_NO,number);
            goToMainActivity();
            //Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, "Invalid OTP", Toast.LENGTH_LONG).show();
        }
    }

    public void resendOtp(View view) {
        countDown();
        sendOtp(number);
    }

    private void goToMainActivity(){
        if (countDownTimer!=null){
            countDownTimer.cancel();
        }
        Intent logIn = new Intent(this, MainActivity.class);
        logIn.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(logIn);
        finish();
    }

}
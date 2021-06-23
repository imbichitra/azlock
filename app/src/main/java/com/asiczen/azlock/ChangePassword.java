package com.asiczen.azlock;

import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
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
import com.asiczen.azlock.security.CryptoUtils;
import com.asiczen.azlock.util.HttpAsyncTask;
import com.asiczen.azlock.util.Utils;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import static com.asiczen.azlock.net.VolleyRequest.STATUS;
import static com.asiczen.azlock.net.VolleyRequest.STATUS_SUCCESS;

public class ChangePassword extends AppCompatActivity implements HttpAsyncTask.AsyncResponse{
    public static final String TAG = ChangePassword.class.getSimpleName();
    private EditText id, c_pwd,n_pwd;
    /*String data=BuildConfig.key;
    private byte[] key= AppContext.hexStringToByteArray(data);*/
    private final byte[] key=AppContext.getKey();
    private final CryptoUtils encode = new CryptoUtils(key);
    private AlertDialog dialog;
    private TextInputLayout errorEmail,errorCpassowrd,errorNpassowrd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        id=findViewById(R.id.input_email);
        c_pwd=findViewById(R.id.c_password);
        n_pwd=findViewById(R.id.new_password);
        Button btn=findViewById(R.id.btn_change);

        errorEmail = findViewById(R.id.errorEmail);
        errorCpassowrd = findViewById(R.id.errorCpassowrd);
        errorNpassowrd = findViewById(R.id.errorNpassowrd);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email=id.getText().toString();
                String current_pwd=c_pwd.getText().toString();
                String new_pwd=n_pwd.getText().toString();
                if(validateData(email,current_pwd,new_pwd)){
                    sendData(email,current_pwd,new_pwd);
                }

            }
        });
        createDialog();
    }
    private boolean validateData(String email,String current_pwd,String new_pwd){
        if(email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ){
            errorEmail.setError("enter a valid email address");
            return false;
        }else{
            errorEmail.setError(null);
        }
        if(current_pwd.isEmpty()){
            errorCpassowrd.setError("This field should not be empty");
            return false;
        }else{
            errorCpassowrd.setError(null);
        }
        if(new_pwd.isEmpty() || (new_pwd.length() < 4 || new_pwd.length() >10)){
            errorNpassowrd.setError("between 4 and 10 alphanumeric characters");
            return false;
        }else {
            errorNpassowrd.setError(null);
        }
        return true;
    }
    private void sendData(String email,String c_pdw,String n_pwd){
        JSONObject object= new JSONObject();
        String data;
        String data1;
        try {
            byte[] packet1 = new byte[16];
            byte[] password = c_pdw.getBytes();
            System.arraycopy(password, 0, packet1, 0, password.length);
            packet1 = encode.AESEncode(packet1);
            data=bytesToHexString(packet1);

            byte[] packet2 = new byte[16];
            byte[] password2 = n_pwd.getBytes();
            System.arraycopy(password2, 0, packet2, 0, password2.length);
            packet2 = encode.AESEncode(packet2);
            data1=bytesToHexString(packet2);

            object.put("user_id",email);
            object.put("c_pwd",data);
            object.put("n_pwd",data1);
            /*String URL="change_password.php";
            String URL_POST=BuildConfig.Port+URL;*/
            dialog.show();

            String URL_POST=AppContext.getIp_address()+AppContext.getChange_password_url();
            /*HttpAsyncTask.context=this;
            HttpAsyncTask httpTask = new HttpAsyncTask();
            httpTask.delegate = this;
            httpTask.execute(URL_POST,object.toString());*/
            VolleyRequest.jsonObjectRequest(this, URL_POST, object, Request.Method.POST, new VolleyResponse() {
                @Override
                public void VolleyError(VolleyError error) {
                    if (dialog!=null && dialog.isShowing())
                        dialog.dismiss();
                    Toast.makeText(ChangePassword.this, VolleyErrors.error(error), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void VolleyObjectResponse(JSONObject response) {
                    Log.d(TAG, "VolleyObjectResponse: "+response.toString());
                    if (dialog!=null && dialog.isShowing())
                        dialog.dismiss();
                    try {
                        if (response.getString(STATUS).equals(STATUS_SUCCESS)){
                            Toast.makeText(ChangePassword.this, "Password changed successfully", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(ChangePassword.this, userlogin.class);
                            startActivity(intent);
                            finish();
                        }else {
                            Toast.makeText(ChangePassword.this, "Invalid Email or Password", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        if (output.equals("Y")){
            Toast.makeText(this, "Password changed successfully", Toast.LENGTH_LONG).show();
            dialog.dismiss();
            Intent intent = new Intent(this, userlogin.class);
            startActivity(intent);
            finish();
        }else if(output.equals("N")){
            dialog.dismiss();
            Toast.makeText(this, "Invalid Email or Password", Toast.LENGTH_LONG).show();
        }else{
            dialog.dismiss();
            Toast.makeText(this, "Slow internet or check your internet Connectivity", Toast.LENGTH_LONG).show();
        }
    }

    private void createDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View bridgeConnectView= getLayoutInflater().inflate(R.layout.progressbar, Utils.nullParent,false);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setView(bridgeConnectView);
        TextView dialogTextView=bridgeConnectView.findViewById(R.id.progressDialog);
        dialogTextView.setText(R.string.connecting);
        dialog = builder.create();
    }
}

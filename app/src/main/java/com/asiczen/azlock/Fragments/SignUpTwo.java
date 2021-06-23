package com.asiczen.azlock.Fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.asiczen.azlock.R;
import com.asiczen.azlock.app.model.SignUpData;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.net.VolleyErrors;
import com.asiczen.azlock.net.VolleyRequest;
import com.asiczen.azlock.net.VolleyResponse;
import com.asiczen.azlock.security.CryptoUtils;
import com.asiczen.azlock.userlogin;
import com.asiczen.azlock.util.HttpAsyncTask;
import com.asiczen.azlock.util.Utils;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import static com.asiczen.azlock.net.VolleyRequest.STATUS;
import static com.asiczen.azlock.net.VolleyRequest.STATUS_EMAIL_EXIST;
import static com.asiczen.azlock.net.VolleyRequest.STATUS_SUCCESS;

public class SignUpTwo extends Fragment implements HttpAsyncTask.AsyncResponse {

    private EditText question_1,question_2,answer_1,answer_2;
    private Button btn_signup;
    private String question1,question2,answer1,answer2;
    private SignUpData signUpData;
    private final byte[] key= AppContext.getKey();
    private final CryptoUtils encode = new CryptoUtils(key);
    public static final String TAG = SignUpTwo.class.getSimpleName();
    private TextView dialogTextView;
    private AlertDialog dialog;
    private ImageView back;
    private TextInputLayout errrorQus1,errrorQus2,errrorAns1,errrorAns2;

    public SignUpTwo() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_sign_up_two, container, false);
        init(view);
        signUpData = SignUpData.getInstance();
        createDialog();
        btn_signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isValidData()){
                    signUpData.setQuestion1(question1);
                    signUpData.setQuestion2(question2);
                    signUpData.setAnswer1(answer1);
                    signUpData.setAnswer2(answer2);
                    sendDataToServer();
                }
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getFragmentManager() != null) {
                    getFragmentManager().popBackStack();
                }
            }
        });
        return view;
    }

    private void createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getContext()));
        View bridgeConnectView= getLayoutInflater().inflate(R.layout.progressbar, Utils.nullParent,false);
        builder.setCancelable(false); // if you want user to wait for some process to finish,
        builder.setView(bridgeConnectView);
        dialogTextView=bridgeConnectView.findViewById(R.id.progressDialog);
        dialog = builder.create();
    }

    private boolean isValidData() {
        question1 = question_1.getText().toString();
        question2 = question_2.getText().toString();
        answer1 = answer_1.getText().toString();
        answer2 = answer_2.getText().toString();
        //boolean valid = true;
        if(question1.isEmpty()){
            errrorQus1.setError("Please enter question1");
            return false;
        }else
        {
            errrorQus1.setError(null);
            errrorQus1.clearFocus();
        }
        if(answer1.isEmpty()|| answer1.length() < 4 || answer1.length() > 15){
            errrorAns1.setError("Answer length between 4 and 15 ");
            return false;
        }else
        {
            errrorAns1.setError(null);
            errrorAns1.clearFocus();
        }
        if(question2.isEmpty()){
            errrorQus2.setError("please enter question2");
            return false;
        }else
        {
            errrorQus2.setError(null);
            errrorQus2.clearFocus();
        }
        if(answer2.isEmpty()|| answer2.length() < 4 || answer2.length() > 15){
            errrorAns2.setError("answer length between 4 and 15");
            return false;
        }else
        {
            errrorAns2.setError(null);
            errrorAns2.clearFocus();
        }
        return true;
    }

    private void init(View view) {
        question_1 = view.findViewById(R.id.question1);
        question_2 = view.findViewById(R.id.question2);
        answer_1 = view.findViewById(R.id.answer1);
        answer_2 = view.findViewById(R.id.answer2);
        btn_signup = view.findViewById(R.id.btn_signup);
        back = view.findViewById(R.id.back);

        errrorAns1 = view.findViewById(R.id.errorAns1);
        errrorAns2 = view.findViewById(R.id.errorAns2);
        errrorQus1 = view.findViewById(R.id.errorQus1);
        errrorQus2 = view.findViewById(R.id.errorQus2);
    }

    private void  sendDataToServer(){
        String URL_POST=AppContext.getIp_address()+AppContext.getPost_to_customer_info_url();
        JSONObject object= new JSONObject();
        String data="";
        byte[] packet = new byte[16];
        try {

            byte[] password = signUpData.getPassword().getBytes();
            System.arraycopy(password, 0, packet,0, password.length);
            Utils.printByteArray(packet);
            packet = encode.AESEncode(packet);
            Utils.printByteArray(packet);
            data=bytesToHexString(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            object.put("user_id",signUpData.getEmail());
            object.put("name",signUpData.getName());
            object.put("password",data);
            object.put("secret_Q1",signUpData.getQuestion1());
            object.put("secret_Q2",signUpData.getQuestion2());
            object.put("secret_Ans1",signUpData.getAnswer1());
            object.put("secret_Ans2",signUpData.getAnswer2());
            object.put("dob",signUpData.getDob());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        dialogTextView.setText(R.string.connecting);
        dialog.show();
        /*HttpAsyncTask.context=getContext();
        HttpAsyncTask httpTask = new HttpAsyncTask();
        httpTask.delegate = this;
        httpTask.execute(URL_POST,object.toString());*/

        VolleyRequest.jsonObjectRequest(getContext(), URL_POST, object, Request.Method.POST, new VolleyResponse() {
            @Override
            public void VolleyError(VolleyError error) {
                if (dialog!=null && dialog.isShowing())
                    dialog.dismiss();
                Toast.makeText(getContext(), VolleyErrors.error(error), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void VolleyObjectResponse(JSONObject response) {
                Log.d(TAG, "VolleyObjectResponse: "+response.toString());
                if (dialog!=null && dialog.isShowing())
                    dialog.dismiss();
                try {
                    if (response.getString(STATUS).equals(STATUS_SUCCESS)){
                        Toast.makeText(getContext(), "You have registered successfully.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getContext(), userlogin.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                    if (response.getString(STATUS).equals(STATUS_EMAIL_EXIST)){
                        Toast.makeText(getContext(), "Email id is already registered", Toast.LENGTH_LONG).show();
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
    public void processFinish(String output, int errorCode) {
        if (output.equals("Y")){
            Log.d(TAG,"Data inserted successfully");
            dialog.dismiss();
            Toast.makeText(getContext(), "You have registered successfully.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getContext(), userlogin.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }else if(output.equals("E")){
            Log.d(TAG,"Email id is already registered");
            dialog.dismiss();
            Toast.makeText(getContext(), "Email id is already registered", Toast.LENGTH_LONG).show();
        }else{
            dialog.dismiss();
            Log.d(TAG,"Time out to connect server");
            Toast.makeText(getContext(), "Unable to contact server or check your internet", Toast.LENGTH_LONG).show();
        }
    }
}

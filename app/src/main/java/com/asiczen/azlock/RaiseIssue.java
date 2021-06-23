package com.asiczen.azlock;

import android.app.ProgressDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.MySharedPreferences;
import com.asiczen.azlock.net.VolleyErrors;
import com.asiczen.azlock.net.VolleyRequest;
import com.asiczen.azlock.net.VolleyResponse;
import com.asiczen.azlock.util.HttpAsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import static com.asiczen.azlock.net.VolleyRequest.STATUS;
import static com.asiczen.azlock.net.VolleyRequest.STATUS_SUCCESS;

public class RaiseIssue extends AppCompatActivity implements HttpAsyncTask.AsyncResponse{
    public static final String TAG = RaiseIssue.class.getSimpleName();
    private EditText mac,contactNo,issue,description;
    private ProgressDialog pdialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raise_issue);
        mac=findViewById(R.id.mac);
        contactNo=findViewById(R.id.Cno);
        issue=findViewById(R.id.edittext);
        description=findViewById(R.id.description);
        pdialog = new ProgressDialog(this);
        pdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pdialog.setIndeterminate(true);
        pdialog.setCancelable(false);
        MySharedPreferences sharedPreferences = new MySharedPreferences(this);
    }
    public void clicked(View v){
        String macId=mac.getText().toString();
        String cno=contactNo.getText().toString();
        String issueT=issue.getText().toString();


        if(macId.isEmpty()){
            mac.setError("Enter the Mac Id");

        }
        else if(macId.length()!=17){
            mac.setError("Enter valid Mac Id");

        }
        else if(cno.isEmpty()){
            contactNo.setError("Enter Contact No");

        }
        else if(cno.length()<10){
            contactNo.setError("Enter valid Contact No");

        }
        else if(issueT.isEmpty()){
            issue.setError("Enter Issue");
        }
        else {
            description.setError(null);
            pdialog.setMessage("Connecting...");
            pdialog.show();
           sendData();
        }
    }
    private void sendData(){
        JSONObject object= new JSONObject();

        try {
            object.put("user_id",loginData.getUserId());
            object.put("mac",mac.getText().toString().trim());
            object.put("cno",contactNo.getText().toString().trim());
            object.put("issue",issue.getText().toString().trim());
            object.put("description",description.getText().toString().trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
        String URL_POST= AppContext.getIp_address()+AppContext.getRaise_issue_url();
        /*HttpAsyncTask.context=this;
        HttpAsyncTask httpTask = new HttpAsyncTask();
        httpTask.delegate = this;
        httpTask.execute(URL_POST,object.toString());*/
        VolleyRequest.jsonObjectRequest(this, URL_POST, object, Request.Method.POST, new VolleyResponse() {
            @Override
            public void VolleyError(VolleyError error) {
                if (pdialog!=null && pdialog.isShowing())
                    pdialog.dismiss();
                Toast.makeText(RaiseIssue.this, VolleyErrors.error(error), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void VolleyObjectResponse(JSONObject response) {
                Log.d(TAG, "VolleyObjectResponse: "+response.toString());
                if (pdialog!=null && pdialog.isShowing())
                    pdialog.dismiss();
                try {
                    if (response.getString(STATUS).equals(STATUS_SUCCESS)){
                        Toast.makeText(RaiseIssue.this, "Issue raised successfully", Toast.LENGTH_LONG).show();
                        finish();
                    }else {
                        Toast.makeText(RaiseIssue.this, "Either the userId is invalid or unable to contact server", Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }
    @Override
    public void processFinish(String output,int errorCode) {
        pdialog.dismiss();
        if(output.equals("Y")){
            Toast.makeText(this, "Issue raised successfully", Toast.LENGTH_LONG).show();
            finish();
        }
        else {
            Toast.makeText(this, "Either the userId is invalid or unable to contact server", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}

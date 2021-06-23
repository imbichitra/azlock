package com.asiczen.azlock.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.asiczen.azlock.content.AppContext;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * Created by dbiswal on 19-02-2018.
 */

public class HttpAsyncTask extends AsyncTask<String, String, String> {
    @SuppressLint("StaticFieldLeak")
    public static Context context;
    private static final String TAG = HttpAsyncTask.class.getSimpleName();
    public AsyncResponse delegate = null;
    private static int retrieveImei =-1;
    public HttpAsyncTask(){

    }
    /*public HttpAsyncTask(Context context){
        this.context=context;
    }*/
    private static int errorCode=-1;
    @Override
    protected String doInBackground(String... urls) {
        if (urls.length>=3 && urls[2].equals("GET_IMEI"))
            retrieveImei=1;
        return POST(urls[0],urls[1]);
    }
    // onPostExecute displays the results of the AsyncTask.
    @Override
    protected void onPostExecute(String result) {
        Log.d(" HttpAsyncTask","response data : "+result);
        if(result.equals("T") || result.equals("N"))
            delegate.processFinish(result,errorCode);
        else
            delegate.processFinish(getJsonData(result),errorCode);
        //progressBar.setVisibility(View.GONE);
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line;
        StringBuilder result = new StringBuilder();
        while((line = bufferedReader.readLine()) != null)
            result.append(line);

        inputStream.close();
        return result.toString();
    }

    private static String POST(String url,String data){

        InputStream inputStream;
        String result;
        //Log.d("hhh",""+data);
        try {

            /*String id= BuildConfig.UserId;
            String pwd=BuildConfig.Password;*/
            String id= AppContext.getUserId();
            String pwd= AppContext.getPassword();
            int timeout=10;
            DefaultHttpClient client = new MyHttpClient(context);
            
            HttpParams httpParams = client.getParams();
            httpParams.setParameter(
                    CoreConnectionPNames.CONNECTION_TIMEOUT, timeout * 1000);
            httpParams.setParameter(
                    CoreConnectionPNames.SO_TIMEOUT, timeout * 1000);
           // HttpGet get = new HttpGet(url);
            HttpPost httpPost = new HttpPost(url);

            String auth =new String(Base64.encode(( id + ":" + pwd).getBytes(),Base64.URL_SAFE|Base64.NO_WRAP));
            httpPost.addHeader("Authorization", "Basic " + auth);
            StringEntity se = new StringEntity(data);
            httpPost.setEntity(se);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            HttpResponse getResponse = client.execute(httpPost);
            inputStream = getResponse.getEntity().getContent();

            // 10. convert inputstream to string
            if(inputStream != null){
                result = convertInputStreamToString(inputStream);
                Log.d("HttpAsyncTask","response data from server :"+result);
            }
            else
                result = "N";
        }
        catch (Exception e){
            e.printStackTrace();
            result = "T";
        }
        return result;
    }
    private static String getJsonData(String data){
        String result = null;
        String appKey;
        if(data!=null){
            try {
                JSONObject jsonObj = new JSONObject(data);
                Log.d(TAG, "getJsonData: "+jsonObj.toString());
                result=jsonObj.getString("status");

                try {
                    if (retrieveImei == 1) {
                        appKey = jsonObj.getString("appkey");
                        StringBuilder sb = new StringBuilder();
                        if (!appKey.isEmpty()) {
                            sb.append(result).append(":").append(appKey);
                            result = sb.toString();
                        }
                    }
                    errorCode=jsonObj.getInt("errorCode");
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
            catch (final JSONException e){
                    e.printStackTrace();
                result = "MAIL_NOT_SEND";
            }
        }
        return result;
    }
    public interface AsyncResponse {
        void processFinish(String output,int errorCode);
    }
}

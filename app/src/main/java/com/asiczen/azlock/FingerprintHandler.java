package com.asiczen.azlock;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.asiczen.azlock.util.Utils;

@RequiresApi(api = Build.VERSION_CODES.M)
class FingerprintHandler extends FingerprintManager.AuthenticationCallback {
    private final Context context;
    public static boolean flag;
    private CancellationSignal cenCancellationSignal;
    public static String changePin="0";
    EditText editText;

    public FingerprintHandler(Context context) {
        this.context = context;
    }

    public void startAuthentication(FingerprintManager fingerprintManager, FingerprintManager.CryptoObject cryptoObject, EditText editText) {
        cenCancellationSignal = new CancellationSignal();
        this.editText = editText;
        if(ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED)
            return;
        fingerprintManager.authenticate(cryptoObject,cenCancellationSignal,0,this,null);

    }

    @Override
    public void onAuthenticationFailed() {
        super.onAuthenticationFailed();
        Toast.makeText(context, "Fingerprint Authentication failed!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        super.onAuthenticationSucceeded(result);
        Log.d("FingerprinHandler","onAuthentication called");
        if(flag) {
            flag=false;
            Log.d("FingerprinHandler","changepin="+changePin);
            if(changePin.equals("1")) {
                ConfigPinActivity.callFrmFH=true;
                Intent intent = new Intent(context, ConfigPinActivity.class);
                intent.putExtra("pinFlag", Utils.CHANGE_PIN_FLAG);
                context.startActivity(intent);
                changePin="0";
            }
            else {
                context.startActivity(new Intent(context, ConnectActivity.class));
            }
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
            }
            ((Activity) context).finish();
        }
    }
    public void stopListening() {
        if (cenCancellationSignal != null) {
            cenCancellationSignal.cancel();
            cenCancellationSignal = null;
        }
    }
}

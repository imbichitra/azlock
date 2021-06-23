package com.asiczen.azlock;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.LogoutBroadcastReceiver;
import com.asiczen.azlock.content.SessionManager;
import com.asiczen.azlock.util.Utils;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Created by Somnath on 1/10/2017.
 */

public class AskPinActivity extends AppCompatActivity implements SessionManager.OnSessionBroadcastListener {

    private SessionManager sessionManager;
    private IntentFilter intentFilter;
    private LogoutBroadcastReceiver logoutBroadcastReceiver;
    private final String TAG = AskPinActivity.class.getSimpleName();
    private EditText askPinEditText;
    private String pin;
    private int pinFlag;
    private KeyStore keyStore;
    private static final String KEY_NAME = "EDMTDev";
    private Cipher cipher;
    private boolean istrue;
    private boolean isFpAvailable=true;
    private FingerprintHandler helper;
    private FingerprintManager.CryptoObject cryptoObject;
    private FingerprintManager fingerprintManager;
    private TextInputLayout input_layout_pin;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ask_pin);

        AppContext appContext = AppContext.getContext();
        pinFlag=getIntent().getIntExtra("pinFlag",-1);
        input_layout_pin = findViewById(R.id.input_layout_pin);

        String changePinOnOff = getIntent().getStringExtra("flag");
        if(changePinOnOff !=null && changePinOnOff.equals("1")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                FingerprintHandler.changePin = "1";
            }
        }else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                FingerprintHandler.changePin = "0";
            }
        }
        //String s=getIntent().getStringExtra("isTrue");
        //disable the finger print image
        istrue=fingerprintVerify();
        Log.d(TAG,"isTrue="+istrue);
        Log.d(TAG,"isTrue="+isFpAvailable);
        if(!isFpAvailable || !istrue)
            findViewById(R.id.imageFinger).setVisibility(View.INVISIBLE);

        sessionManager=new SessionManager(this, this);
        logoutBroadcastReceiver = new LogoutBroadcastReceiver(this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(SessionManager.ACTION_EXIT);

        askPinEditText= findViewById(R.id.ask_pin_edittext);
        askPinEditText.setOnClickListener(new View.OnClickListener() { //required to stop the verification using fingerprint
            @Override
            public void onClick(View v) {
                stopListeningToFingerPrint();
            }
        });
        askPinEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    Log.i(TAG,"Enter pressed");
                    //onClickContinueButton(null);
                }
                return false;
            }
        });
        askPinEditText.requestFocus();
        if(!isFpAvailable || !istrue) { //if mobile does not have fingerprint then direct open keyboard otherwise use fingerprint to open
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        }
        pin = appContext.getPin();

    }

    public void onClickContinueButton(View v){
        if(pin.equals(askPinEditText.getText().toString())){
            if(pinFlag== Utils.CHANGE_PIN_FLAG){
                ConfigPinActivity.callFrmFH=true;
                Intent intent = new Intent(AskPinActivity.this, ConfigPinActivity.class);
                intent.putExtra("pinFlag", Utils.CHANGE_PIN_FLAG);
                startActivity(intent);
            }
            else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    FingerprintHandler.flag=false;
                }
                Intent intent = new Intent(AskPinActivity.this, ConnectActivity.class);
                startActivity(intent);
            }
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(askPinEditText.getWindowToken(), 0);
            }
            finish();
        }
        else {
            String message = "Invalid PIN";
            if (TextUtils.isEmpty(askPinEditText.getText().toString()))
                message = "Please enter a PIN";
            //Snackbar.make(findViewById(android.R.id.content),message,Snackbar.LENGTH_LONG).show();
            input_layout_pin.setError(message);
            askPinEditText.setText("");
        }
        //InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.hideSoftInputFromWindow(askPinEditText.getWindowToken(), 0);
    }

    public void onBackPressed()
    {
        super.onBackPressed();
        if(pinFlag != Utils.CHANGE_PIN_FLAG) {
            sessionManager.exit();
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"onresume");
        Log.d(TAG,"onresume ISTRUE="+istrue);
        Log.d(TAG,"onresume ISTRUE="+isFpAvailable);
        if(istrue && isFpAvailable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d(TAG, "hello");
                helper.startAuthentication(fingerprintManager, cryptoObject,askPinEditText);
                FingerprintHandler.flag = true;
            }
        }
    }

    private void stopListeningToFingerPrint(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(isFpAvailable)
                helper.stopListening();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause()");
        stopListeningToFingerPrint();
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
    private boolean fingerprintVerify(){
        boolean isTrue=Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            fingerprintManager= (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
            if (fingerprintManager==null){
                isFpAvailable=false;
                return false;
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {

                return false;
            }
            if (fingerprintManager !=null && !fingerprintManager.isHardwareDetected()) {
                isFpAvailable=false;

                //Toast.makeText(this, "Fingerprint authentication permission not enable", Toast.LENGTH_SHORT).show();
            }else {
                if (fingerprintManager !=null && !fingerprintManager.hasEnrolledFingerprints()) {
                    isFpAvailable = false;
                    Toast.makeText(this, "Register at least one fingerprint in Settings", Toast.LENGTH_SHORT).show();
                }else {
                    assert keyguardManager != null;
                    if (!keyguardManager.isKeyguardSecure())
                        Toast.makeText(this, "Lock screen security not enabled in Settings", Toast.LENGTH_SHORT).show();
                    else
                        genKey();

                    if (cipherInit()) {
                        cryptoObject = new FingerprintManager.CryptoObject(cipher);
                        helper= new FingerprintHandler(this);
                        helper.startAuthentication(fingerprintManager, cryptoObject,askPinEditText);
                    }
                }
            }
        }
        return  isTrue;
    }
    private boolean cipherInit() {

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES+"/"+KeyProperties.BLOCK_MODE_CBC+"/"+KeyProperties.ENCRYPTION_PADDING_PKCS7);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

        try {
            keyStore.load(null);
            SecretKey key = (SecretKey)keyStore.getKey(KEY_NAME,null);
            cipher.init(Cipher.ENCRYPT_MODE,key);
            return true;
        } catch (IOException e1) {

            e1.printStackTrace();
            return false;
        } catch (NoSuchAlgorithmException e1) {

            e1.printStackTrace();
            return false;
        } catch (CertificateException e1) {

            e1.printStackTrace();
            return false;
        } catch (UnrecoverableKeyException e1) {

            e1.printStackTrace();
            return false;
        } catch (KeyStoreException e1) {

            e1.printStackTrace();
            return false;
        } catch (InvalidKeyException e1) {

            e1.printStackTrace();
            return false;
        }

    }

    private void genKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        KeyGenerator keyGenerator = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }

        try {
            keyStore.load(null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                assert keyGenerator != null;
                keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setUserAuthenticationRequired(true)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7).build()
                );
            }
            assert keyGenerator != null;
            keyGenerator.generateKey();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        catch (InvalidAlgorithmParameterException e)
        {
            e.printStackTrace();
        }
    }
}

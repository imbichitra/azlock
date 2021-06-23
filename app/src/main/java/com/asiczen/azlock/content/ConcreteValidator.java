package com.asiczen.azlock.content;

import android.app.Activity;
import com.google.android.material.textfield.TextInputLayout;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.asiczen.azlock.R;
import com.asiczen.azlock.app.Validate;

/**
 * Created by Somnath on 12/19/2016.
 */

public class ConcreteValidator implements Validator {

    private final Activity activity;
    private static final String TAG=ConcreteValidator.class.getSimpleName();

    public ConcreteValidator(Activity activity){
        this.activity=activity;
    }
    @Override
    public boolean validate(Validate validate, TextInputLayout textInputLayout, EditText editText) {

        String text = editText.getText().toString().trim();
        String errMessage=null;
        Log.d(TAG, "validate:"+text);
        boolean isValid = false;
        switch (validate)
        {
            case EMAIL:
                isValid = text.isEmpty() || !isValidEmail(text);
                errMessage=activity.getString(R.string.err_msg_email);
                break;

            case PASSWORD:
                isValid = text.isEmpty();
                errMessage=activity.getString(R.string.err_msg_password);
                break;

            case PIN:
                isValid = !isValidPin(text);
                if(TextUtils.isEmpty(text)) {
                    errMessage = activity.getString(R.string.err_msg_pin_empty);
                }
                else if(text.length()!=4) {
                    errMessage = activity.getString(R.string.err_msg_pin_length);
                }
                else if(!text.matches("\\d+(?:\\.\\d+)?")) {
                    errMessage = activity.getString(R.string.err_msg_pin_pattern);
                }
                break;

            case NAME:
                isValid = text.isEmpty();
                errMessage=activity.getString(R.string.err_msg_name);
                break;

            case DOOR_NAME:
                isValid = text.isEmpty();
                errMessage=activity.getString(R.string.err_msg_door_name);
                break;

            case PHONE:
                isValid = text.isEmpty() || !isValidPhone(text);
                errMessage=activity.getString(R.string.err_msg_name);
                break;

            case IP_ADDRESS:
                isValid = text.isEmpty() || !isValidIpAddress(text);
                errMessage=activity.getString(R.string.err_msg_door_name);
                break;
        }
        Log.d(TAG, "validate:"+text+" ["+isValid+"]");
        if(isValid){
            textInputLayout.setErrorEnabled(true);
            textInputLayout.setError(errMessage);
            requestFocus(editText);
            return false;
        }
        else {
            textInputLayout.setError(null);
            textInputLayout.setErrorEnabled(false);
        }

        return true;
    }

    private static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private static boolean isValidIpAddress(String ip) {
        return !TextUtils.isEmpty(ip) && android.util.Patterns.IP_ADDRESS.matcher(ip).matches();
    }

    private static boolean isValidPhone(String phone) {
        return !TextUtils.isEmpty(phone) && Patterns.PHONE.matcher(phone).matches();
    }

    private static boolean isValidPin(String pin) {
        return !TextUtils.isEmpty(pin) && pin.length()==4 && pin.matches("\\d+(?:\\.\\d+)?");
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }
}

package com.asiczen.azlock.content;

import android.app.Activity;
import com.google.android.material.textfield.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.asiczen.azlock.R;
import com.asiczen.azlock.app.Validate;

/**
 * Created by Somnath on 12/19/2016.
 */

public class CustomTextWatcher implements TextWatcher {
    private final Validator validator;
    private TextInputLayout textInputLayout;
    private EditText editText;

    private CustomTextWatcher(Activity activity, View view, Validator validator) {
        /* http://www.androidhive.info/2015/09/android-material-design-floating-labels-for-edittext/ */
        this.validator = validator;
    }

    public CustomTextWatcher(Activity activity, EditText editText, TextInputLayout textInputLayout, Validator validator) {
        this.editText=editText;
        this.textInputLayout=textInputLayout;
        this.validator = validator;
    }

    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    public void afterTextChanged(Editable editable) {
        //Log.d(TAG, "afterTextChanged:"+editable.toString());
        switch (editText.getId()) {
            case R.id.name_editText:
                validator.validate(Validate.NAME, textInputLayout, editText);
                break;

            case R.id.door_name_editText:
                validator.validate(Validate.DOOR_NAME, textInputLayout, editText);
                break;

            case R.id.input_email:
                validator.validate(Validate.EMAIL, textInputLayout, editText);
                break;

            case R.id.input_password:
                validator.validate(Validate.PASSWORD, textInputLayout, editText);
                break;

            case R.id.input_confirm_pin:
            case R.id.input_pin:
                validator.validate(Validate.PIN, textInputLayout, editText);
                break;
        }
    }

}


package com.asiczen.azlock.content;

import com.google.android.material.textfield.TextInputLayout;
import android.widget.EditText;

import com.asiczen.azlock.app.Validate;

/**
 * Created by Somnath on 12/19/2016.
 */

public interface Validator {
    boolean validate(Validate validate, TextInputLayout textInputLayout, EditText editText);
}

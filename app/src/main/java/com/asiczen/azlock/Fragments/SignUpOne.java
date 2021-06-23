package com.asiczen.azlock.Fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.azlock.R;
import com.asiczen.azlock.SignupActivity;
import com.asiczen.azlock.app.model.SignUpData;
import com.asiczen.azlock.userlogin;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Objects;

public class SignUpOne extends Fragment {

    private EditText input_name,input_email,input_password,input_confirm_password;
    private TextView dob,link_login;
    private ImageView btn_forward,btn_date;
    private String name,email,password,Dob;
    private DatePickerDialog.OnDateSetListener listener;
    private ImageView back;
    private final Activity activity;
    TextInputLayout errorPassword,errorCPassword,errorName,errorEmail;

    public SignUpOne(Activity activity) {
        // Required empty public constructor
        this.activity = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_sign_up_one, container, false);
        init(view);
        btn_forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isValidData()){
                    SignUpData signUpData = SignUpData.getInstance();
                    signUpData.setName(name);
                    signUpData.setEmail(email);
                    signUpData.setPassword(password);
                    signUpData.setDob(Dob);
                    gotToSignUpTwo();
                }
            }
        });

        btn_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                final int day = cal.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dialog = new DatePickerDialog(
                        Objects.requireNonNull(getContext()),
                        android.R.style.Theme_DeviceDefault_Dialog,
                        listener,
                        year, month, day
                );
                Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        listener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                month = month + 1;
                String date = year + "-" + month + "-" + dayOfMonth;
                dob.setText(date);
            }
        };

        SpannableString ss = new SpannableString("Already a member? Login");
        ss.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.green)), 17, 23, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        link_login.setText(ss);
        link_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getContext(), userlogin.class);
                startActivity(i);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.onBackPressed();
            }
        });
        return view;
    }

    private boolean isValidData() {
        name = input_name.getText().toString();
        email = input_email.getText().toString();
        password = input_password.getText().toString();
        String confirmPassword = input_confirm_password.getText().toString();
        Dob = dob.getText().toString();
        //boolean valid =true;
        if (name.isEmpty() || name.length() < 3) {
            errorName.setError("Name should be at least 3 characters");
            return false;
        } else {
            errorName.setError(null);
            errorName.clearFocus();
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorEmail.setError("Enter a valid email address");
            //input_email.setError("Enter a valid email address");
            return false;
        } else {
            errorEmail.setError(null);
            errorEmail.clearFocus();
        }
        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            errorPassword.setError("Input Password [ 4 to 10 characters]");
            return false;
        }else {
            errorPassword.setError(null);
            errorPassword.clearFocus();
        }
        if (confirmPassword.isEmpty() || confirmPassword.length() < 4 || confirmPassword.length() > 10) {
            errorCPassword.setError("Input Password [ 4 to 10 characters]");
            return false;
        }else {
            errorCPassword.setError(null);
            errorCPassword.clearFocus();
        }
        if (!password.equals(confirmPassword)){
            Toast.makeText(getContext(), "Password and confirm password not match.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (Dob.isEmpty()){
            Toast.makeText(getContext(), "Enter D.O.B", Toast.LENGTH_SHORT).show();
            return false;
        }else {
            dob.setError(null);
            dob.clearFocus();
        }
        return true;
    }

    private void gotToSignUpTwo() {
        SignupActivity.fragmentManager.beginTransaction().replace(R.id.fragment_container, new SignUpTwo(),null).addToBackStack(null).commit();
    }

    private void init(View view) {
        input_name = view.findViewById(R.id.input_name);
        input_email = view.findViewById(R.id.input_email);
        input_password = view.findViewById(R.id.input_password);
        input_confirm_password = view.findViewById(R.id.input_confirm_password);
        dob = view.findViewById(R.id.dob);
        btn_forward = view.findViewById(R.id.btn_forward);
        btn_date = view.findViewById(R.id.btn_date);
        link_login = view.findViewById(R.id.link_login);
        back = view.findViewById(R.id.back);

        errorPassword = view.findViewById(R.id.password);
        errorCPassword = view.findViewById(R.id.cpassword);
        errorName= view.findViewById(R.id.errorName);
        errorEmail = view.findViewById(R.id.errorEmail);
    }
}

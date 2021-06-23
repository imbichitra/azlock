package com.asiczen.azlock;


import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;

import com.asiczen.azlock.Fragments.SignUpOne;


public class SignupActivity extends AppCompatActivity{
    //private static final String TAG = "SignupActivity";
    public static FragmentManager fragmentManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        fragmentManager = getSupportFragmentManager();
        if(findViewById(R.id.fragment_container)!=null){
            if(savedInstanceState!=null){ //if dont use it it overlap the layout
                return;
            }
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            SignUpOne sign = new SignUpOne(this);
            fragmentTransaction.add(R.id.fragment_container,sign,"Fragment One");
            fragmentTransaction.commit();
        }
    }
}
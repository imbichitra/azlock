package com.asiczen.azlock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.asiczen.azlock.Adapters.SliderAdapter;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.content.MySharedPreferences;

import java.util.ArrayList;

public class SlideViewActivity extends AppCompatActivity {
    public static final String TAG =SlideViewActivity.class.getSimpleName();
    native String[] getUrls();

    static {
        System.loadLibrary("ndklink");
    }
    private ViewPager viewPager;
    private LinearLayout dotLayout;

    private TextView[] mDots;
    private TextView next,skip;
    int pageCount;
    private RelativeLayout relativeLayout;
    String buttonStatus = "Next";
    MySharedPreferences onBoradScreenStatus;

    ArrayList<ShortcutInfo> shortcutInfos = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_view);
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        changeStatusBarColor();
        viewPager = findViewById(R.id.view_pager);
        dotLayout = findViewById(R.id.dots);
        next = findViewById(R.id.next);
        skip = findViewById(R.id.skip);
        relativeLayout = findViewById(R.id.main_layout);
        AppContext appContext = AppContext.getContext();
        String[] urls = getUrls();
        appContext.setData(urls);
        onBoradScreenStatus = new MySharedPreferences(getApplicationContext());
        DatabaseHandler databaseHandler = new DatabaseHandler(this);
        ArrayList<AppContext.DisplayTableContent> list = databaseHandler.getDataFromDisplayTable();

        /*if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
            if (!list.isEmpty())
                switch (list.size()){
                    case 1:
                    case 2:
                    case 3:
                        shortcutManager.removeAllDynamicShortcuts();
                        addShortcutInfo(list.size(),list);
                        break;
                }

            createShortcutInfo("Key",
                    "Key",
                    "id3",
                    R.drawable.ic_key,
                    getIntent(ConnectActivity.OPEN_KEY)
            );

            if (shortcutManager != null) {
                shortcutManager.setDynamicShortcuts(shortcutInfos);
            }
        }*/

        int action = getIntent().getIntExtra("val",-1);
        ConnectActivity.actionToPerform = action; //this for open shutcut action performed by use on long pressing app.
        HomeActivity.actionToPerform = action;

        Log.d(TAG, "onCreate:ok "+action);
        if (onBoradScreenStatus.getOnBoradScreenStatus()
                && onBoradScreenStatus.isUserDataSet()
                && !onBoradScreenStatus.getValues(MySharedPreferences.MOB_NO).isEmpty()){
            goToMainActivity();
        }else if (onBoradScreenStatus.getOnBoradScreenStatus() && !onBoradScreenStatus.isUserDataSet()){
            goToLoginActivity();
        }else if (onBoradScreenStatus.getOnBoradScreenStatus() && onBoradScreenStatus.getValues(MySharedPreferences.MOB_NO).isEmpty()){
            gotoSendOtpActivity();
        }

        SliderAdapter sliderAdapter = new SliderAdapter(this);
        viewPager.setAdapter(sliderAdapter);
        addDotsIndicator(0);
        viewPager.addOnPageChangeListener(viewListener);

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(buttonStatus.equals("Done")){
                    //goToMainActivity();
                    goToLoginActivity();
                }else{
                    viewPager.setCurrentItem(pageCount+1);
                }
            }
        });
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //goToMainActivity();
                goToLoginActivity();
            }
        });
    }

    public void addDotsIndicator(int position){
        mDots = new TextView[4];
        dotLayout.removeAllViews();
        for(int i=0;i<mDots.length;i++){
            mDots[i] = new TextView(this);
            mDots[i].setText(Html.fromHtml("&#8226"));
            mDots[i].setTextSize(35);
            mDots[i].setTextColor(getResources().getColor(R.color.tab_indicator_gray));

            dotLayout.addView(mDots[i]);
        }
        if (mDots.length>0){
            if(position==0)
                relativeLayout.setBackgroundColor(getResources().getColor(R.color.slider2));
            mDots[position].setTextColor(getResources().getColor(R.color.backgroundColor));
        }
    }
    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {

        }

        @Override
        public void onPageSelected(int i) {
            addDotsIndicator(i);
            pageCount = i;
            if (i==0){
                next.setText("Next");
                skip.setText("Skip");
                buttonStatus = "Next";
                relativeLayout.setBackgroundColor(getResources().getColor(R.color.slider2));
            }else if(i == mDots.length-1){
                skip.setVisibility(View.INVISIBLE);
                next.setText("Done");
                buttonStatus = "Done";
                relativeLayout.setBackgroundColor(getResources().getColor(R.color.dimgray));
            }else{
                if (i==1)
                    relativeLayout.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                else
                    relativeLayout.setBackgroundColor(getResources().getColor(R.color.slider));
                skip.setVisibility(View.VISIBLE);
                next.setText("Next");
                skip.setText("Skip");
                buttonStatus = "Next";
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {

        }
    };

    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    private void goToMainActivity(){
        Intent logIn = new Intent(SlideViewActivity.this, MainActivity.class);
        startActivity(logIn);
        finish();
    }

    private void goToLoginActivity(){
        onBoradScreenStatus.setOnBrodScreenStatus();
        Intent logIn = new Intent(SlideViewActivity.this, userlogin.class);
        logIn.putExtra(userlogin.Command,userlogin.GET_IMEI);
        startActivity(logIn);
        finish();
    }

    private void gotoSendOtpActivity() {
        Intent otpSend = new Intent(SlideViewActivity.this, SendOtpActivity.class);
        startActivity(otpSend);
        finish();
    }


    private Intent getIntent(int value){
        Intent intent = new Intent(this, SlideViewActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra("val",value);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addCategory("android.shortcut.conversation");
        return intent;
    }

    private void addShortcutInfo(int length,ArrayList<AppContext.DisplayTableContent> list){
        for (int i=0;i<length;i++){
            Log.d(TAG, "addShortcutInfo: "+list.get(i).getDootName());
            createShortcutInfo(list.get(i).getMacId(),
                    list.get(i).getDootName(),
                    "id"+i,
                    R.drawable.ic_lock,
                    getIntent(i)
            );
        }
    }
    private void createShortcutInfo(String shortLabel,String logLabel,String id,int icon,Intent intent){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, id)
                    .setShortLabel(shortLabel)
                    .setLongLabel(logLabel)
                    .setIcon(Icon.createWithResource(this, icon))
                    .setIntent(intent)
                    .build();
            shortcutInfos.add(shortcut);
        }
    }
}

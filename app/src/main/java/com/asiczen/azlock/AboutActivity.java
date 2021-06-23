package com.asiczen.azlock;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/*
 * Created by  on 09-03-2015.
 */
public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_new);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.show();
            actionBar.setTitle(Html.fromHtml("<font color='#FFFFFF'>About</font>"));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        TextView version=findViewById(R.id.version);
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            String v = pInfo.versionName;  //get the version name from gradle file
            version.setText(v);
            Log.d("Hello","version name ="+version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        findViewById(R.id.facebook_imageButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/azlock.in/"));
                    startActivity(myIntent);
                } catch (ActivityNotFoundException e) {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Invalid link", Toast.LENGTH_LONG);
                    toast.show();
                    e.printStackTrace();
                }
            }
        });

        findViewById(R.id.google_plus_imageButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/AzlockSales"));
                    startActivity(myIntent);
                } catch (ActivityNotFoundException e) {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Invalid link", Toast.LENGTH_LONG);
                    toast.show();
                    e.printStackTrace();
                }
            }
        });

        findViewById(R.id.youtube_imageView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/channel/UCIynVF90fNgjK-tMg3lLKaA"));
                    startActivity(myIntent);
                } catch (ActivityNotFoundException e) {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Invalid link", Toast.LENGTH_LONG);
                    toast.show();
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}

package com.asiczen.azlock;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;


public class GuideActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.introduction);
        String url = "file:///android_asset/textaz.html";
        WebView wv = this.findViewById(R.id.wvAboutUs);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.loadUrl(url);
        wv.setWebViewClient(new MyBrowser());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
           // actionBar.hide();
            actionBar.show();
            actionBar.setTitle(Html.fromHtml("<font color='#FFFFFF'>Guide</font>"));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
        private static class MyBrowser extends WebViewClient{
            @Override
            public boolean shouldOverrideUrlLoading(WebView view,String url) {
                view.loadUrl(url);
                return true;
            }
        }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}

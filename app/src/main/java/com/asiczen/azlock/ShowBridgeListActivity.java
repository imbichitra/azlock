package com.asiczen.azlock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Html;
import android.view.View;

import com.asiczen.azlock.Adapters.RecyclerAdapters;
import com.asiczen.azlock.app.model.BridgeDetail;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class ShowBridgeListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private List<BridgeDetail> bridgeDetails = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_bridge_list);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.show();
            actionBar.setTitle(Html.fromHtml("<font color='#FFFFFF'>Select bridge to connect </font>"));
            //actionBar.setDisplayHomeAsUpEnabled(true);
        }
        DatabaseHandler databaseHandler = new DatabaseHandler(this);
        bridgeDetails = databaseHandler.getBridgeData(0);
        recyclerView = findViewById(R.id.recycler_view);
        setTheAdapter();
    }

    private void setTheAdapter() {
        RecyclerAdapters mAdapter = new RecyclerAdapters(bridgeDetails, Utils.SHOW_BRIDGE_DATA,R.layout.bridge_detail_items);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new RecyclerAdapters.onRecyclerViewItemClickListener() {
            @Override
            public void onItemClickListener(View view, int position) {
                Intent result = new Intent();
                result.putExtra(Utils.BRIDGE_ID, bridgeDetails.get(position).getBridgeId());
                result.putExtra(Utils.BRIDGE_PASSWORD, bridgeDetails.get(position).getPassword());
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });
    }
}

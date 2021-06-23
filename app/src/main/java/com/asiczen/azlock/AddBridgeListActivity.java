package com.asiczen.azlock;

import android.graphics.Color;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.azlock.Adapters.RecyclerAdapters;
import com.asiczen.azlock.app.model.BridgeDetail;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.util.RecyclerItemTouchHelper;
import com.asiczen.azlock.util.RecyclerItemTouchHelperListener;
import com.asiczen.azlock.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class AddBridgeListActivity extends AppCompatActivity implements RecyclerItemTouchHelperListener {

    public static final String TAG = AddBridgeListActivity.class.getSimpleName();
    private Button add_bridge;
    private EditText bridge_id,bridge_password;
    private List<BridgeDetail> bridgeDetails = new ArrayList<>();
    private RecyclerAdapters mAdapter;
    private RecyclerView recyclerView;
    private LinearLayout root_layout;
    private DatabaseHandler databaseHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bridge_list);

        init();
        databaseHandler = new DatabaseHandler(this);
        bridgeDetails = databaseHandler.getBridgeData(0);
        TextView error = findViewById(R.id.error);
        if (bridgeDetails.size()==0)
            error.setVisibility(View.VISIBLE);
        setTheAdapter();
        add_bridge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String bridgeId = bridge_id.getText().toString();
                String bridgePassword = bridge_password.getText().toString();

                boolean isDataContain = !TextUtils.isEmpty(bridgeId) && !TextUtils.isEmpty(bridgePassword);
                if (isDataContain){
                    if (!bridgeId.contains("azBridge")) {
                        addDataToDb(bridgeId, bridgePassword);
                        bridge_id.setText("");
                        bridge_password.setText("");
                    }
                    else
                        Toast.makeText(AddBridgeListActivity.this, "azBridge is not allowed", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(AddBridgeListActivity.this, "Please fill the data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void addDataToDb(String bridgeId, String bridgePassword) {
        BridgeDetail bridgeDetail = new BridgeDetail(bridgeId, bridgePassword);
        long i = databaseHandler.insertBridgeData(bridgeDetail);
        Log.d(TAG, "addDataToDb: "+i);
        if (i>0) {
            if (i == Integer.MAX_VALUE){
                //Toast.makeText(this, "Password of "+bridgeId+" bridge is up", Toast.LENGTH_LONG).show();
                return;
            }
            bridgeDetails.add(bridgeDetail);
            mAdapter.notifyDataSetChanged();
        }else {
            Toast.makeText(this, "Bridge id "+bridgeId+" is already taken", Toast.LENGTH_SHORT).show();
        }
    }
    private void init() {
        add_bridge = findViewById(R.id.add);
        bridge_id = findViewById(R.id.bridge_id);
        bridge_password = findViewById(R.id.bridge_password);
        recyclerView = findViewById(R.id.recycler_view);
        root_layout = findViewById(R.id.root_layout);
    }
    private void setTheAdapter() {
        mAdapter = new RecyclerAdapters(bridgeDetails, Utils.ADD_BRIDGE_DATA,R.layout.bridge_detail_items);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);
        ItemTouchHelper.SimpleCallback simpleCallback = new RecyclerItemTouchHelper(0,ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,this);
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }
    @Override
    public void onSwipe(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if(viewHolder instanceof RecyclerAdapters.ViewHolder){
            final BridgeDetail items = bridgeDetails.get(position);
            final int deletIndex = viewHolder.getAdapterPosition();
            mAdapter.remove(deletIndex);
            databaseHandler.deleteBrideData(items);
            Snackbar snackbar = Snackbar.make(root_layout,"1 archived",Snackbar.LENGTH_SHORT);
            snackbar.setAction("Undo", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    databaseHandler.insertBridgeData(items);
                    // ((SlotItemAdapter.MyViewHolder) viewHolder).view_foreground.setVisibility(View.INVISIBLE);
                    mAdapter.restoreItems(items,deletIndex);
                    //mAdapter.notifyDataSetChanged();
                }
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();

            mAdapter.notifyDataSetChanged();
        }
    }
}

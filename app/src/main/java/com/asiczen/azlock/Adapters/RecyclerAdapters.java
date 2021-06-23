package com.asiczen.azlock.Adapters;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asiczen.azlock.R;
import com.asiczen.azlock.app.model.BridgeDetail;
import com.asiczen.azlock.util.Utils;

import java.util.List;

public class RecyclerAdapters extends RecyclerView.Adapter<RecyclerAdapters.ViewHolder> {

    private List<BridgeDetail> bridgeDetails;
    private final String whichCall;
    private final int layout;

    List<Integer> images;
    List<String> titels;
    private onRecyclerViewItemClickListener mItemClickListener;

    public RecyclerAdapters(List<BridgeDetail> bridgeDetails,String whichCall,int layout){
        this.bridgeDetails = bridgeDetails;
        this.whichCall = whichCall;
        this.layout = layout;
    }

    //for bridge activity
    public RecyclerAdapters(List images,List titels,String whichCall,int layout){
        this.images = images;
        this.titels = titels;
        this.whichCall = whichCall;
        this.layout = layout;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(layout,viewGroup,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder myAdapter, final int i) {
        if (whichCall.equals(Utils.SHOW_BRIDGE_DATA) || whichCall.equals(Utils.ADD_BRIDGE_DATA)){
            BridgeDetail bridgeDetail = bridgeDetails.get(i);
            myAdapter.bridge_id.setText(bridgeDetail.getBridgeId());
            if (whichCall.equals(Utils.SHOW_BRIDGE_DATA)) {
                myAdapter.view_background.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mItemClickListener != null) {
                            mItemClickListener.onItemClickListener(v, i);
                        }
                    }
                });
            }
        }else if (whichCall.equals(Utils.BRIDGE_OPERATION)){
            myAdapter.bridge_image_id.setImageResource(images.get(i));
            myAdapter.bridge_titel_id.setText(titels.get(i));
            myAdapter.bridge_main_layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItemClickListener != null) {
                        mItemClickListener.onItemClickListener(v, i);
                    }
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        switch (whichCall){
            case Utils.SHOW_BRIDGE_DATA :
            case Utils.ADD_BRIDGE_DATA:
                return bridgeDetails.size();
            case Utils.BRIDGE_OPERATION:
                return titels.size();
            default:
                return 0;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView bridge_id;
        public CardView view_background;

        //bridge activity
        LinearLayout bridge_main_layout;
        ImageView bridge_image_id;
        TextView bridge_titel_id;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            if (whichCall.equals(Utils.SHOW_BRIDGE_DATA) || whichCall.equals(Utils.ADD_BRIDGE_DATA)) {
                bridge_id = itemView.findViewById(R.id.bridge_id);
                view_background = itemView.findViewById(R.id.view_background);
            }else if (whichCall.equals(Utils.BRIDGE_OPERATION)){
                bridge_main_layout = itemView.findViewById(R.id.bridge_main_layout);
                bridge_image_id = itemView.findViewById(R.id.bridge_image_id);
                bridge_titel_id = itemView.findViewById(R.id.bridge_titel_id);
            }
        }
    }
    public void remove(int position) {
        bridgeDetails.remove(position);
        notifyItemRemoved(position);
    }

    public void restoreItems(BridgeDetail item,int position){
        bridgeDetails.add(item);
        notifyItemInserted(position);
    }

    public void setOnItemClickListener(onRecyclerViewItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }

    public interface onRecyclerViewItemClickListener {
        void onItemClickListener(View view, int position);
    }
}

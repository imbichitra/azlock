package com.asiczen.azlock;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.asiczen.azlock.app.model.Door;
import com.asiczen.azlock.util.Utils;

import java.util.ArrayList;
import java.util.Objects;


/**
 * Created by somnath on 24-10-2017.
 */

class PopulatelistAdapter extends ArrayAdapter<Door> {

    private final Context mcontext;
    private final int mresource;
    private final UserMode userMode;


    public PopulatelistAdapter(Context context, int resource, ArrayList<Door> doors,UserMode um){
        super(context,resource,doors);
        mcontext=context;
        mresource=resource;
        userMode=um;
    }

    @SuppressLint("ViewHolder")
    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String doorId = Objects.requireNonNull(getItem(position)).getId();
        String doorName = Objects.requireNonNull(getItem(position)).getName();
        LayoutInflater inflater=LayoutInflater.from(mcontext);
        convertView=inflater.inflate(mresource,parent,false);
        TextView tvName=  convertView.findViewById(R.id.name);
        TextView tvAddr=  convertView.findViewById(R.id.address);
        TextView tvUser=  convertView.findViewById(R.id.user);
        doorId = Utils.generateMac(doorId).toUpperCase();
        tvName.setText(doorName);
        tvAddr.setText(doorId);

        Cursor res=userMode.getData(doorId);
        if(res.getCount() > 0){
            res.moveToFirst();
            do {
                if (res.getString(1).equals("guest")) {
                    tvUser.setText(R.string.guest);

                } else if (res.getString(1).equals("owner")) {
                    tvUser.setText(R.string.owner);

                } else {
                    tvUser.setText(" ");
                }
            }while(res.moveToNext());
            res.close();
        }
        return convertView;
    }
}

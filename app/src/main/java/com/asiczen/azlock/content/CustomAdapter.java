package com.asiczen.azlock.content;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.asiczen.azlock.DeleteGuestsAsyncTask;
import com.asiczen.azlock.DeleteLogAsyncTask;
import com.asiczen.azlock.GuestListActivity;
import com.asiczen.azlock.GuestLogActivity;
import com.asiczen.azlock.OnCheckedChangeListener;
import com.asiczen.azlock.OnDeleteListener;
import com.asiczen.azlock.OnSearchListener;
import com.asiczen.azlock.OnUpdateListener;
import com.asiczen.azlock.R;
import com.asiczen.azlock.SettingsActivity;
import com.asiczen.azlock.app.AdapterViewCode;
import com.asiczen.azlock.app.model.Door;
import com.asiczen.azlock.app.model.Guest;
import com.asiczen.azlock.net.OnDataSendListener;
import com.asiczen.azlock.util.DateTimeFormat;
import com.asiczen.azlock.util.FileAccess;
import com.asiczen.azlock.util.ImageUtility;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.RoundedImageView;
import com.asiczen.azlock.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * Created by user on 8/10/2015.
 */
public class CustomAdapter<T> extends ArrayAdapter<T> implements Packet, Filterable, OnUpdateListener {

    private final Context context;
    private final AppContext appContext;
    private final Activity activity;
    private final List<T> objects;
    private SparseBooleanArray mSelectedItemsIds;
    public static AdapterViewCode viewCode;
    private GuestFilter guestFilter;
    private  List<T> filteredGuestList;
    public static Door door;
    //public static boolean isGuestDeleted = false;
    private final OnSearchListener<T> onSearchListener;
    //Guest thisGuest;
    //private OnUpdateListener mOnUpdateListener;
    //String progressbarMsg = null;
    //public static int guestPosition = 0;
    private final int resource;
    private FileAccess fileAccess;
    private String tamperNotificationFlag;
    private final String TAG = CustomAdapter.class.getSimpleName();
    private OnCheckedChangeListener mOnCheckedChangeListener;
    //Bitmap bmImage;


    private final int TAMPER_NOTIFICATION_INDEX = 1;
    private final int AJAR_INDEX = 2;
    private final int AUTO_LOCK_INDEX = 3;
    //private Comparator<Object> mComparator;

    public CustomAdapter(Activity activity, int resourceId, List<T> objects, AdapterViewCode viewCode) {
        super(activity, resourceId, objects);
        mSelectedItemsIds = new SparseBooleanArray();
        this.context = activity;
        this.activity = activity;
        this.objects = objects;
        this.filteredGuestList = objects;
        CustomAdapter.viewCode = viewCode;
        this.resource = resourceId;
        appContext = AppContext.getContext();
        OnDataSendListener mOnDataSendListener = appContext.getOnDataSendListener();
        GuestLogActivity.setOnUpdateListener(this);
        Log.d(TAG, objects.toString());
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            onSearchListener = (OnSearchListener<T>) activity;
            if(viewCode == AdapterViewCode.DEVICE_SETTINGS_VIEW_CODE) {
                this.fileAccess = new FileAccess(context, Utils.TAMPER_NOTIFICATION_CONFIG_FILE);
                tamperNotificationFlag = fileAccess.read();
                mOnCheckedChangeListener = (OnCheckedChangeListener) activity;
            }
            else if(viewCode==AdapterViewCode.GUEST_LIST_VIEW_CODE) {
                DatabaseHandler databaseHandler = new DatabaseHandler(context);
                databaseHandler.updateKeyStatus(null, null);
                notifyDataSetChanged();
                databaseHandler.close();
            }
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.getClass().getSimpleName()
                    + " must implement SearchListener and CheckedChangeListener");
        }

        getFilter();
    }

    @Override
    public void onUpdate(int resultCode, Object result) {
        if(resultCode == OnUpdateListener.GUEST_UPDATED){
            DatabaseHandler databaseHandler = new DatabaseHandler(context);
            ArrayList<Guest> updatedGuests = new ArrayList<>();
            FileAccess fileAccess = new FileAccess(context, Utils.GUEST_LIST_TYPE_OPTIONS_FILE);
            GuestListActivity.guestListItemType = fileAccess.read();
            if(GuestListActivity.guestListItemType == null && fileAccess.FILE_NOT_FOUND){
                fileAccess.write(String.valueOf(Utils.SHOW_ALL_GUESTS_EXCEPT_KEY_DELETED));
                updatedGuests = databaseHandler.getGuests(appContext.getDoor().getId(), Utils.SHOW_ALL_GUESTS_EXCEPT_KEY_DELETED);
            }
            else if(GuestListActivity.guestListItemType != null && !GuestListActivity.guestListItemType.isEmpty()) {
                updatedGuests = databaseHandler.getGuests(appContext.getDoor().getId(), Integer.parseInt(GuestListActivity.guestListItemType));
            }
            objects.clear();
            for(Guest g : updatedGuests){
                objects.add((T) g);
            }
            databaseHandler.updateKeyStatus(null, null);
            notifyDataSetChanged();
            databaseHandler.close();
        }
        else if(resultCode == OnUpdateListener.LOG_UPDATED){
            for(GuestLogActivity.GuestLog guestLog : GuestLogActivity.selectedLogs) {
                objects.remove(guestLog);
            }
            notifyDataSetChanged();
        }
        else if(resultCode == OnUpdateListener.TAMPER_NOTIFICATION_UPDATED){
            tamperNotificationFlag = fileAccess.read();
            Log.d(TAG, "Cancel"+tamperNotificationFlag+"\t[READ]");
            notifyDataSetChanged();
        }
        else if(resultCode == OnUpdateListener.ASK_PIN_UPDATED){
            Log.d(TAG, "onUpdate/ASK_PIN_UPDATED");
            notifyDataSetChanged();
        }
        else if(resultCode == OnUpdateListener.PLAY_SOUND_UPDATED){
            Log.d(TAG, "onUpdate/PLAY_SOUND_UPDATED");
            notifyDataSetChanged();
        }
        else if(resultCode == OnUpdateListener.AJAR_UPDATE){
            notifyDataSetChanged();
        }
    }

    /*public void onRefresh(ArrayList<AccessPoint> accessPoints){
        this.objects.clear();
        for(AccessPoint accessPoint : accessPoints){
            objects.add((T) accessPoint);
        }
        notifyDataSetChanged();
    }*/

    private static class GuestLogViewHolder {
        TextView name, accessDateTime;// accessStatus;
        ImageView image, statusImageView;
    }

    private static class GuestDetailsViewHolder {
        TextView name;
        ImageView image;
        TextView keyStatus;
    }

    private static class SettingsViewHolder {
        RelativeLayout settingsRelativeLayout;
        TextView settingsText,time;
        CheckBox settingsCheckBox;
        Switch settingsSwitch;
    }

    /*private static class AccessPointViewHolder {
        TextView apName;
        ImageView image;
        TextView status;
    }*/

    private static class BridgeLockListViewHolder{
        TextView mac;
    }

    @Override
    public int getCount() {
        return filteredGuestList.size();
    }

    @Nullable
    @Override
    public T getItem(int position) {
        return filteredGuestList.get(position);
    }

    @Override
    public int getPosition(@Nullable T item) {
        return filteredGuestList.indexOf(item);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        DatabaseHandler databaseHandler = new DatabaseHandler(context);
        RoundedImageView roundedImageView;
        if(viewCode == AdapterViewCode.GUEST_LOG_VIEW_CODE) {
            GuestLogViewHolder guestLogViewHolder = new GuestLogViewHolder();
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                if (inflater != null) {
                    convertView = inflater.inflate(R.layout.guestlog, parent, false);
                }
                LinearLayout guestLogLayout = convertView.findViewById(R.id.guest_log_linearLayout);

                guestLogViewHolder.name = convertView.findViewById(R.id.guestName_textView);
                guestLogViewHolder.accessDateTime = convertView.findViewById(R.id.access_datetime_textView);
                //guestLogViewHolder.accessStatus = (TextView) convertView.findViewById(R.id.access_status_textView);
                guestLogViewHolder.image = convertView.findViewById(R.id.guest_imageView);
                guestLogViewHolder.statusImageView = convertView.findViewById(R.id.statusImageView);
                convertView.setTag(guestLogViewHolder);
            } else {
                guestLogViewHolder = (GuestLogViewHolder) convertView.getTag();
            }

            GuestLogActivity.GuestLog guest = (GuestLogActivity.GuestLog) getItem(position);
            if (guest != null) {
                guestLogViewHolder.name.setText(guest.getGuestName());
            }
            //Log.d("CustomAdapter", "AccessDateTime:"+guest.getAccessDateTime());
            if (guest != null) {
                guestLogViewHolder.accessDateTime.setText(DateTimeFormat.getDate(guest.getAccessDateTime(), 3));
            }
            //guestLogViewHolder.accessStatus.setText(guest.getAccessStatus());
            if (guest != null) {
                if (guest.getAccessStatus().equalsIgnoreCase("LOCKED")) {
                    //guestLogViewHolder.accessStatus.setTextColor(Color.rgb(46,139,87));
                    guestLogViewHolder.statusImageView.setImageResource(R.mipmap.ic_lock_action);
                    //int color = Color.parseColor("#3CB371");
                    //guestLogViewHolder.statusImageView.setColorFilter(color);
                }
                else if (guest.getAccessStatus().equalsIgnoreCase("UNLOCKED"))
                {
                    guestLogViewHolder.statusImageView.setImageResource(R.mipmap.ic_unlock_action);
                    //int color = Color.parseColor("#3CB371");
                    //guestLogViewHolder.statusImageView.setColorFilter(color);
                }
                else {
                    guestLogViewHolder.statusImageView.setImageResource(R.mipmap.ic_access_error_3);
                    //int color = Color.parseColor("#ffcc0000");
                    //guestLogViewHolder.statusImageView.setColorFilter(color);
                }
            }
            Bitmap image = null;
            if (guest != null) {
                image = guest.getImage();
            }
            /*if(image == null) {
                bmImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_user);
                //bmImage = BitmapFactory.decodeFile(guests.get(position).getImage());
            }
            else    bmImage = image;
            roundedImageView = new RoundedImageView(context);
            Bitmap conv_bm = roundedImageView.getCroppedBitmap(bmImage, 100, 1, Color.WHITE);*/
            roundedImageView = new RoundedImageView(context);
            Bitmap conv_bm = null;
            DisplayMetrics displayMetrics = Utils.getDisplayMetrics(activity);
            if(displayMetrics.widthPixels == 1080) {
                //Log.d("Adapter", "Resolution: 1080p");
                conv_bm = roundedImageView.getCroppedBitmap(ImageUtility.resize(image), 160, 1, Color.WHITE);
            }
            else if(displayMetrics.widthPixels == 720) {
                //Log.d("Adapter", "Resolution: 720p");
                conv_bm = roundedImageView.getCroppedBitmap(ImageUtility.resize(image), 100, 1, Color.WHITE);
            }
            else if(displayMetrics.widthPixels < 720) {
                //Log.d("Adapter", "Resolution: "+Utils.getDisplayMetrics(activity).widthPixels);
                conv_bm = roundedImageView.getCroppedBitmap(ImageUtility.resize(image), 70, 1, Color.WHITE);
            }
            guestLogViewHolder.image.setImageBitmap(conv_bm);
        }
        else if(viewCode == AdapterViewCode.GUEST_LIST_VIEW_CODE) {
            GuestDetailsViewHolder guestDetailsViewHolder = new GuestDetailsViewHolder();
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                if (inflater != null) {
                    convertView = inflater.inflate(R.layout.guestlist, parent, false);
                }
                guestDetailsViewHolder.name = convertView.findViewById(R.id.guestName_textView);
                guestDetailsViewHolder.keyStatus = convertView.findViewById(R.id.key_status_textView);
                guestDetailsViewHolder.image = convertView.findViewById(R.id.guest_imageView);
                convertView.setTag(guestDetailsViewHolder);
            } else {
                guestDetailsViewHolder = (GuestDetailsViewHolder) convertView.getTag();
            }

            Guest guest = (Guest) getItem(position);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
            //Log.d("CustomAdapter", guest.getId() + " (" + guest.getName() + ") Registered:" +
                    //new DatabaseHandler(context).isRegistered(guest.getId(), MainActivity.doorID));
            int status = 0;
            if (guest != null) {
                status = databaseHandler.getKeyStatus(guest.getId(), appContext.getDoor().getId());
            }
            guestDetailsViewHolder.keyStatus.setText(Door.statusString.get(status));
            if (guest != null) {
                guestDetailsViewHolder.name.setText(guest.getName());
            }
            Bitmap image = null;
            if (guest != null) {
                image = guest.getImage();
            }
            //image = ImageUtility.decodeSampledBitmapFromResource(context.getResources(), R.drawable.ic_user_small, 75, 75);
            //image = BitmapFactory.decodeFile(guests.get(position).getImage());

            roundedImageView = new RoundedImageView(context);
            Bitmap conv_bm = null;
            DisplayMetrics displayMetrics = Utils.getDisplayMetrics(activity);
            if(displayMetrics.widthPixels == 1080) {
                conv_bm = roundedImageView.getCroppedBitmap(image, 160, 1, Color.WHITE);
            }
            else if(displayMetrics.widthPixels == 720) {
                conv_bm = roundedImageView.getCroppedBitmap(image, 100, 1, Color.WHITE);
            }
            else if(displayMetrics.widthPixels < 720) {
                conv_bm = roundedImageView.getCroppedBitmap(image, 70, 1, Color.WHITE);
            }
            guestDetailsViewHolder.image.setImageBitmap(conv_bm);
        }
        else if(viewCode == AdapterViewCode.DEVICE_SETTINGS_VIEW_CODE) {
            SettingsViewHolder settingsViewHolder = new SettingsViewHolder();
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                if (inflater != null) {
                    convertView = inflater.inflate(resource, parent, false);
                }
                settingsViewHolder.settingsRelativeLayout = convertView.findViewById(R.id.list_item_relativeLayout);
                settingsViewHolder.settingsText = convertView.findViewById(R.id.list_item_textView);
                settingsViewHolder.time = convertView.findViewById(R.id.autolock_time);
                //settingsViewHolder.settingsCheckBox = (CheckBox) convertView.findViewById(R.id.list_item_checkBox);
                settingsViewHolder.settingsSwitch =  convertView.findViewById(R.id.list_item_checkBox);
                final int pos = position;

                settingsViewHolder.settingsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        Log.d(TAG, "onCheckedChanged: ");
                        Switch s=(Switch)buttonView;
                        //mOnCheckedChangeListener.onCheckedChanged( buttonView, s.isChecked(), OnCheckedChangeListener.REQUEST_TAMPER_NOTIFICATION);
                        if (pos == TAMPER_NOTIFICATION_INDEX) {
                            Log.d(TAG, "onCheckedChanged: ");
                            mOnCheckedChangeListener.onCheckedChanged( buttonView, s.isChecked(), OnCheckedChangeListener.REQUEST_TAMPER_NOTIFICATION);
                        }
                        else if(pos == AJAR_INDEX){
                            mOnCheckedChangeListener.onCheckedChanged( buttonView, s.isChecked(), OnCheckedChangeListener.REQUEST_AJAR);
                        }
                        else if(pos == AUTO_LOCK_INDEX){
                            mOnCheckedChangeListener.onCheckedChanged( buttonView, s.isChecked(), OnCheckedChangeListener.REQUEST_AUTO_LOCK);
                        }
                    }
                });
                /*settingsViewHolder.settingsSwitch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //CheckBox checkBox = (CheckBox) v;
                        Switch s=(Switch)v;
                        Log.d(TAG, "onClickListener:" + s.isChecked() + " [ " + pos + " ]");
                       if (pos == TAMPER_NOTIFICATION_INDEX) {
                           mOnCheckedChangeListener.onCheckedChanged((CompoundButton) v, s.isChecked(), OnCheckedChangeListener.REQUEST_TAMPER_NOTIFICATION);
                       } } });*/

                convertView.setTag(settingsViewHolder);
            } else {
                settingsViewHolder = (SettingsViewHolder) convertView.getTag();
            }

            settingsViewHolder.settingsText.setText((String) getItem(position));


            //settingsViewHolder.settingsCheckBox.setVisibility((position == TAMPER_NOTIFICATION_INDEX) ? View.VISIBLE : View.INVISIBLE);
            /*settingsViewHolder.settingsSwitch.setVisibility((position == TAMPER_NOTIFICATION_INDEX) ? View.VISIBLE : View.INVISIBLE);
            settingsViewHolder.settingsSwitch.setVisibility((position == AJAR_INDEX) ? View.VISIBLE : View.INVISIBLE);
            settingsViewHolder.settingsSwitch.setVisibility((position == AUTO_LOCK_INDEX) ? View.VISIBLE : View.INVISIBLE);*/
            if(position == 0){
                settingsViewHolder.settingsSwitch.setVisibility(View.INVISIBLE);
                settingsViewHolder.time.setVisibility(View.INVISIBLE);
            }

            if(position == AJAR_INDEX){

                int status=appContext.getAjarStatus();
                settingsViewHolder.settingsSwitch.setChecked(status == Utils.AJAR_STATUS);
            }
            if(position == AUTO_LOCK_INDEX){
                settingsViewHolder.time.setVisibility(View.VISIBLE);
                settingsViewHolder.time.setText(String.valueOf(appContext.getAutolockTime()));
                int status=appContext.getAutolockStatus();
                settingsViewHolder.settingsSwitch.setChecked(status == Utils.AJAR_STATUS);
            }
          if (position == TAMPER_NOTIFICATION_INDEX) {
              boolean status = appContext.getTemperStatus();
              settingsViewHolder.settingsSwitch.setChecked(status);
              //settingsViewHolder.settingsSwitch.setChecked(appContext.getNotificationStatus(Notification.TAMPER));
              Log.d(TAG, "TEMPER FLAG: "+SettingsActivity.shouldEnable);
              try{
                  if(SettingsActivity.shouldEnable || status) {  //SettingsActivity.shouldEnable
                      Log.d(TAG,"TRUE PART "+SettingsActivity.shouldEnable);
                      //settingsViewHolder.settingsCheckBox.setChecked(appContext.getNotificationStatus(Notification.TAMPER));
                      settingsViewHolder.settingsSwitch.setChecked(true);
                  }
                  else {
                      SettingsActivity.isFirstTime=false;
                      Log.d(TAG,"ELSE PART "+SettingsActivity.shouldEnable);
                      settingsViewHolder.settingsSwitch.setChecked(false);
                  }
              }
              catch (Exception e){
                  SettingsActivity.isFirstTime=false;
                  settingsViewHolder.settingsSwitch.setChecked(false);
              }
            }
        }
        else if(viewCode == AdapterViewCode.BRIDGE_LOCK_LIST_VIEW_CODE) {
            BridgeLockListViewHolder lockListViewHolder= new BridgeLockListViewHolder();
            if(convertView==null){
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                if (inflater != null) {
                    convertView = inflater.inflate(resource, parent, false);
                }
                lockListViewHolder.mac = convertView.findViewById(android.R.id.text1);
                convertView.setTag(lockListViewHolder);
            }
            else{
                lockListViewHolder=(BridgeLockListViewHolder) convertView.getTag();
            }
            lockListViewHolder.mac.setText((String) getItem(position));
        }
        /*else if(viewCode == ACCESS_POINT_VIEW_CODE) {
            AccessPointViewHolder scanResultViewHolder = new AccessPointViewHolder();

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.ap_list_row, parent, false);
                scanResultViewHolder.image = (ImageView) convertView.findViewById(R.id.device_imageView);
                scanResultViewHolder.status = (TextView) convertView.findViewById(R.id.deviceStatus_textView);
                scanResultViewHolder.apName = (TextView) convertView.findViewById(R.id.deviceName_textView);
                convertView.setTag(scanResultViewHolder);
            } else {
                scanResultViewHolder = (AccessPointViewHolder) convertView.getTag();
            }
            AccessPoint accessPoint = (AccessPoint) getItem(position);
            scanResultViewHolder.apName.setText(accessPoint.getSSID());
            int status = accessPoint.getStatus();
            if(status != AccessPoint.UNKNOWN){
                scanResultViewHolder.status.setVisibility(View.VISIBLE);
                scanResultViewHolder.status.setText(AccessPoint.connectionStatusMap.get(status));
            }
            else {
                scanResultViewHolder.status.setVisibility(View.GONE);
            }
        }*/

        int backgroundColor = mSelectedItemsIds.get(position) ? ContextCompat.getColor(context, R.color.silver) : Color.TRANSPARENT;
        convertView.setBackgroundColor(backgroundColor);
        databaseHandler.close();
        return convertView;
    }
    /*public boolean areAllItemsEnabled() {
        return false;
    }

    public boolean isEnabled(int position) {
        if(position == 3 && appContext.getAjarStatus()==Utils.AJAR_STATUS)
            return true;
        else {

            return false;
        }
    }*/
    @Override
    public Filter getFilter() {
        //Log.d("CustomAdapter", "getFilter() called");
        if (guestFilter == null) {
            guestFilter = new GuestFilter();
        }
        return guestFilter;
    }

    @Override
    public void add(T object) {
        objects.add(object);
        notifyDataSetChanged();
        //Toast.makeText(context, laptops.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void remove(T object) {
        // super.remove(object);
        DatabaseHandler databaseHandler = new DatabaseHandler(context);
        if(viewCode == AdapterViewCode.GUEST_LOG_VIEW_CODE) {
            new DeleteLogAsyncTask(activity, false, this).execute(GuestLogActivity.selectedLogs
                    .toArray(new GuestLogActivity.GuestLog[GuestLogActivity.selectedLogs.size()]));
        }
        else if(viewCode == AdapterViewCode.GUEST_LIST_VIEW_CODE)
        {
            new DeleteGuestsAsyncTask(activity, this).execute(Utils.selectedGuests
                    .toArray(new Guest[Utils.selectedGuests.size()]));
        }
        notifyDataSetChanged();
        databaseHandler.close();
    }

    public List<T> getGuests() {
        return objects;
    }

    public void toggleSelection(int position) {
        selectView(position, !mSelectedItemsIds.get(position));
    }

    public void removeSelection() {
        mSelectedItemsIds = new SparseBooleanArray();
        notifyDataSetChanged();
    }

    private void selectView(int position, boolean value) {
        if (value) {
            mSelectedItemsIds.put(position, true);
        } else {
            mSelectedItemsIds.delete(position);
        }
        notifyDataSetChanged();
    }

    public int getSelectedCount() {
        return mSelectedItemsIds.size();
    }

    public SparseBooleanArray getSelectedIds() {
        return mSelectedItemsIds;
    }


    public static void setOnDeleteListener(OnDeleteListener onDeleteListener)
    {
        Log.d("CustomAdapter", "setOnDeleteListener: ");
    }

    private class GuestFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            //Log.d("GuestFilter", "performFiltering() called:"+constraint);
            FilterResults filterResults = new FilterResults();
            if (constraint!=null && constraint.length()>0) {
                ArrayList<T> tempList = new ArrayList<>();

                // search content in friend list
                for (int i=0;i<objects.size();i++) {
                    //Log.d("GuestFilter", "performFiltering()/Adding...");
                    if (((Guest) objects.get(i)).getName().toLowerCase().contains(constraint.toString().toLowerCase())) {
                        tempList.add(objects.get(i));
                        //Log.d("GuestFilter", "performFiltering()/Added: " + ((Guest) guests.get(i)).getName());
                    }
                }
                filterResults.count = tempList.size();
                filterResults.values = tempList;
            } else {
                filterResults.count = objects.size();
                filterResults.values = objects;
            }
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredGuestList = (ArrayList<T>) results.values;
            //Log.d("GuestFilter", "publishResults() called:"+filteredGuestList);
            onSearchListener.onSearch(filteredGuestList);
            notifyDataSetChanged();
        }

    }
}

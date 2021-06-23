package com.asiczen.azlock;

import android.app.Activity;
import android.app.Dialog;
//import android.app.DialogFragment;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.asiczen.azlock.app.DeviceStatus;
import com.asiczen.azlock.app.model.Door;
import com.asiczen.azlock.app.model.Guest;
import com.asiczen.azlock.content.AppContext;
import com.asiczen.azlock.content.DatabaseHandler;
import com.asiczen.azlock.net.OnDataAvailableListener;
import com.asiczen.azlock.net.OnDataSendListener;
import com.asiczen.azlock.util.DateTimeFormat;
import com.asiczen.azlock.util.Packet;
import com.asiczen.azlock.util.RoundedImageView;
import com.asiczen.azlock.util.Utils;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import at.markushi.ui.CircleButton;

/*
 * Created by user on 8/6/2015.
 */
public class GuestDetailsFragment extends DialogFragment implements Packet {
    private Context mContext;
    private AppContext appContext;
    public static Guest guest;
    private final String TAG = GuestDetailsFragment.class.getSimpleName();

    //private final int GALLERY_ACTIVITY_CODE=200;
    private final static int RESULT_SELECT_IMAGE_CODE = 100;
    private final int RESULT_CROP = 400;

    //private final int MESSENGER_REQUEST = 1;

    private ImageView guestImage;
    private TextView guestMac;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mContext = getActivity();
        appContext = AppContext.getContext();

        LayoutInflater inflater = Objects.requireNonNull(getActivity()).getLayoutInflater();
        View guestDetailsView = inflater.inflate(R.layout.userdetails, null,false);
        mOnDataSendListener = appContext.getOnDataSendListener();

        guestImage = guestDetailsView.findViewById(R.id.photo_imageView);
        TextView guestName = guestDetailsView.findViewById(R.id.guestName_textView);
        TextView accessType = guestDetailsView.findViewById(R.id.access_type_textView);
        ImageButton copyContent = guestDetailsView.findViewById(R.id.copy_imageButton);
        TextView startAccessTime = guestDetailsView.findViewById(R.id.startAccessTime);
        TextView endAccessTime = guestDetailsView.findViewById(R.id.endAccessTime);
        guestMac = guestDetailsView.findViewById(R.id.guest_mac_textView);

        CircleButton deleteButton = guestDetailsView.findViewById(R.id.delete_imageView);
        CircleButton editButton = guestDetailsView.findViewById(R.id.edit_imageView);
        CircleButton setOwnerButton = guestDetailsView.findViewById(R.id.set_owner_imageView);

        guestName.setText(guest.getName());
        guestMac.setText(guest.getId());
        Bitmap image = guest.getImage();//new DatabaseHandler(mContext).getImage(guest);
        if(image == null) {
            image = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_user);
        }
        guest.setImage(image);
        drawImage(image);
        //guestPhone.setText(guest.getPhone());
        accessType.setText(guest.getAccessType());
        if(guest.getAccessType().equalsIgnoreCase("Limited Time")){
            Log.d("GuestDeatilsFragment", "Guest:" + guest);
            guestDetailsView.findViewById(R.id.accesst_time_border).setVisibility(View.VISIBLE);
            guestDetailsView.findViewById(R.id.accessTime_relativeLayout).setVisibility(View.VISIBLE);
            guestDetailsView.findViewById(R.id.textView22).setVisibility(View.VISIBLE);
            guestDetailsView.findViewById(R.id.fromAccessTime).setVisibility(View.VISIBLE);
            guestDetailsView.findViewById(R.id.toAccessTime).setVisibility(View.VISIBLE);
            startAccessTime.setVisibility(View.VISIBLE);
            endAccessTime.setVisibility(View.VISIBLE);

            startAccessTime.setText(DateTimeFormat.getDate(guest.getAccessStartDateTime(), 1));
            endAccessTime.setText(DateTimeFormat.getDate(guest.getAccessEndDateTime(), 1));
        } else if(guest.getAccessType().equalsIgnoreCase("Full Time")){
            guestDetailsView.findViewById(R.id.accesst_time_border).setVisibility(View.GONE);
            guestDetailsView.findViewById(R.id.accessTime_relativeLayout).setVisibility(View.GONE);
        }

        //if(!(new DatabaseHandler(mContext).isRegistered(guest.getId(), appContext.getDoor().getId())))
            //deleteGuest.setVisibility(View.INVISIBLE);
        /*if(image == null)
            bmImage = BitmapFactory.decodeResource(getResources(), R.drawable.ic_user);
        else {
            bmImage = image;
            guest.setImage(image);
        }
        drawImage(bmImage);*/

        int color = Color.parseColor("#1E90FF");
        //msg.setColorFilter(color);
        //caller.setColorFilter(color);
        copyContent.setColorFilter(color);

        guestImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Create intent to Open Image applications like Gallery, Google Photos
                //Pick Image From Gallery
                //Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                //i.setType("image/*");
                //i.setAction(Intent.ACTION_GET_CONTENT);
                // Start the Intent
                //startActivityForResult(i, RESULT_SELECT_IMAGE);

                /*Intent gallery_Intent = new Intent(mContext, GalleryUtil.class);
                startActivityForResult(gallery_Intent, GALLERY_ACTIVITY_CODE);*/

                try {
                    //Pick Image From Gallery
                    /*Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, RESULT_SELECT_IMAGE_CODE);*/
                    Log.d(TAG, "onTouch: ");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

        /*caller.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + guestPhone.getText()));
                startActivity(callIntent);
            }
        });

        msg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + guestPhone.getText()));
                //sendIntent.setData(Uri.parse("sms:"+guestPhone.getText()));
                sendIntent.putExtra("sms_body", "System generated message");
                startActivityForResult(sendIntent, MESSENGER_REQUEST);
            }
        });*/

        copyContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager)
                        mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("GUEST_MAC", guestMac.getText().toString());
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                }
                Toast.makeText(mContext, "User Id copied", Toast.LENGTH_LONG).show();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "AZLOCK: DELETE BUTTON CLIECKED");
                new AlertDialog.Builder(Objects.requireNonNull(getActivity())).setMessage("Do you want delete this guest")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                doGuestDelete();
                                GuestDetailsFragment.this.dismiss();
                                Runtime.getRuntime().gc();
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEditListener.onEdit(GuestDetailsFragment.this);
                dismiss();
            }
        });

        setOwnerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnChangeOwnerSelectedListener.onChangeOwnerSelected(guest);
                dismiss();
            }
        });

        deleteButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast toast=Toast.makeText(mContext, "Delete Guest", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return true;
            }
        });

        editButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast toast=Toast.makeText(mContext, "Edit Guest", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return true;
            }
        });

        setOwnerButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast toast=Toast.makeText(mContext, "Make Owner", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return true;
            }
        });

        return new AlertDialog.Builder(Objects.requireNonNull(getActivity())).setView(guestDetailsView)
                .create();
    }

    private void doGuestDelete()
    {
        Door door = new Door();
        if (appContext.getDeviceStatus() == DeviceStatus.DEVICE_HANDSHAKED) {
            Utils u = new Utils();
            u.requestType = Utils.KEY_REQ;
            u.requestStatus = Utils.TCP_PACKET_UNDEFINED;
            u.requestDirection = Utils.TCP_SEND_PACKET;

            byte[] packet = new byte[MAX_PKT_SIZE];
            packet[REQUEST_PACKET_TYPE_POS] = Utils.KEY_REQ;
            packet[REQUEST_ACCESS_MODE_POS] = (byte) Utils.APP_MODE_OWNER;
            packet[REQUEST_PACKET_LENGTH_POS] = DeleteGuestPacket.SENT_PACKET_LENGTH_DELETE_SELECTED_GUEST;
            //String doorID = MainActivity.whichDoor.getId();
            door.setId(appContext.getDoor().getId());
            door.setName(appContext.getDoor().getName());//db.getDoor(doorID).getName());

            byte[] guestMac = u.getMacIdInHex(Utils.generateMac(guest.getId()));
            System.arraycopy(guestMac, 0, packet, 3, guestMac.length);
            //packet[DeleteGuestPacket.CHECKSUM_DELETE_SELECTED_SENT] = u.calculateChecksum(packet, true);
            Log.d("CustomAdapter", "AZLOCK Sent Guest:" + guest.getName());
            u.commandDetails = new String(packet, StandardCharsets.ISO_8859_1);
            Log.d("CustomAdapter", "Sent Packet:" + u.commandDetails);
            u.setUtilsInfo(u);

            mOnDataSendListener.onSend(packet, new OnDataAvailableListener() {
                @Override
                public void onDataAvailable(String data) {
                    Log.d(TAG, "onDataAvailable:"+data);
                    processPacket(data);
                }
            },"Deleting Guest");

        }
    }

    private void processPacket(String packet)
    {
        Log.d(TAG, "Received Packet:" + packet);
        //Utils u = new Utils();
        if (packet != null && packet.length() >= DeleteGuestPacket.RECEIVED_PACKET_LENGTH_DELETE_GUEST){
            //byte [] strBytes;
            Log.d(TAG, "Received Packet Length:" + packet.length());
            try {
                //strBytes = packet.getBytes("ISO-8859-1");

                    if (packet.charAt(RESPONSE_PACKET_TYPE_POS) == Utils.KEY_REQ &&
                            packet.charAt(RESPONSE_COMMAND_STATUS_POS) == Utils.CMD_OK) {
                        Log.d(TAG, "COMMAND_STATUS [ OK ]");
                            if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == SUCCESS) {
                                //new DatabaseHandler(context).delete((Guest) guest, door);
                                Log.d(TAG, "AZLOCK Deleted:" + new DatabaseHandler(mContext).delete(guest, appContext.getDoor()));
                                deleteListener.onDelete();
                                this.dismiss();
                                Log.d(TAG, "Successfully Deleted");
                            } else if (packet.charAt(RESPONSE_ACTION_STATUS_POS) == FAILURE) {
                                Log.d(TAG, "Deletion Failed");
                            }
                    }
                    else{
                        new androidx.appcompat.app.AlertDialog.Builder(mContext).setMessage(Utils.CommunicationError.commandStatusError(
                                packet.charAt(RESPONSE_COMMAND_STATUS_POS)))
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).create().show();
                    }

            } catch(Exception e) {
                Log.d(TAG, "Unsupported String Decoding Exception");
            }
        }
    }

    /*@Override
    public void onDismiss(DialogInterface dialog) {
        Log.d("GuestDetailsFragment", "onDismiss called");
        mOnUpdateListener.onImageUpdated(this);
    }*/

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case RESULT_SELECT_IMAGE_CODE:
                if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                    try{
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA };
                        Cursor cursor = Objects.requireNonNull(getActivity()).getContentResolver().query(selectedImage,
                                filePathColumn, null, null, null);
                        assert cursor != null;
                        cursor.moveToFirst();
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String picturePath = cursor.getString(columnIndex);
                        Log.d("Picture Path", picturePath);
                        performCrop(picturePath);
                        cursor.close();
                        /*
                        //return Image Path to the Main Activity
                        Intent returnFromGalleryIntent = new Intent();
                        returnFromGalleryIntent.putExtra("picturePath", picturePath);
                        getActivity().setResult(getActivity().RESULT_OK, returnFromGalleryIntent);*/
                    }catch(Exception e){
                        e.printStackTrace();
                        Intent returnFromGalleryIntent = new Intent();
                        Objects.requireNonNull(getActivity()).setResult(Activity.RESULT_CANCELED, returnFromGalleryIntent);
                    }
                }else{
                    Log.i(getTag(), "RESULT_CANCELED");
                    Intent returnFromGalleryIntent = new Intent();
                    Objects.requireNonNull(getActivity()).setResult(Activity.RESULT_CANCELED, returnFromGalleryIntent);
                }
                break;

            case RESULT_CROP:
                if(resultCode == Activity.RESULT_OK){
                    Bundle extras = data.getExtras();
                    Bitmap selectedBitmap = null;
                    if (extras != null) {
                        selectedBitmap = extras.getParcelable("data");
                    }
                    guest.setImage(selectedBitmap);
                    boolean isUpdated = new DatabaseHandler(mContext).setImage(guest);
                    Log.d("GuestDetailsFragment", "Image Update:" + isUpdated + " " + guest.getName() + ":" + guest.getImage());
                    if(isUpdated) {
                        mOnUpdateListener.onUpdate(OnUpdateListener.IMAGE_UPDATED, null);
                    }
                    // Set The Bitmap Data To ImageView
                    //guestImage.setImageBitmap(selectedBitmap);
                    guestImage.setImageBitmap(new DatabaseHandler(mContext).getImage(guest));
                    guestImage.setScaleType(ImageView.ScaleType.FIT_XY);
                    //drawImage(selectedBitmap);
                    drawImage(new DatabaseHandler(mContext).getImage(guest));
                }
                break;
        }
        /*if (requestCode == GALLERY_ACTIVITY_CODE) {
            if(resultCode == Activity.RESULT_OK){
                picturePath = data.getStringExtra("picturePath");
                //perform Crop on the Image Selected from Gallery
                performCrop(picturePath);
            }
        }*/
    }

    private void performCrop(String picUri) {
        try {
            //Start Crop Activity

            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            // indicate image type and Uri
            File f = new File(picUri);
            Uri contentUri = Uri.fromFile(f);

            cropIntent.setDataAndType(contentUri, "image/*");
            // set crop properties
            cropIntent.putExtra("crop", "true");
            // indicate aspect of desired crop
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            // indicate output X and Y
            cropIntent.putExtra("outputX", 280);
            cropIntent.putExtra("outputY", 280);

            // retrieve data on return
            cropIntent.putExtra("return-data", true);
            // start the activity - we handle returning in onActivityResult
            startActivityForResult(cropIntent, RESULT_CROP);
        }
        // respond to users whose devices do not support the crop action
        catch (ActivityNotFoundException anfe) {
            // display an error message
            String errorMessage = "your device doesn't support the crop action!";
            Toast toast = Toast.makeText(mContext, errorMessage, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void drawImage(Bitmap bm)
    {
        RoundedImageView roundedImageView = new RoundedImageView(mContext);
        Bitmap conv_bm = null;
        Activity activity = getActivity();
        if (activity != null) {
            if(Utils.getDisplayMetrics(activity).widthPixels == 1080) {
                conv_bm = roundedImageView.getCroppedBitmap(bm, 340, 1, Color.WHITE);
            }
            else if(Utils.getDisplayMetrics(activity).widthPixels == 720) {
                conv_bm = roundedImageView.getCroppedBitmap(bm, 220, 1, Color.WHITE);
            }
            else if(Utils.getDisplayMetrics(activity).widthPixels < 720) {
                conv_bm = roundedImageView.getCroppedBitmap(bm, 150, 1, Color.WHITE);
            }
        }
        guestImage.setImageBitmap(conv_bm);
    }

    // Use this instance of the interface to deliver action events
    private OnUpdateListener mOnUpdateListener;
    private OnDeleteListener deleteListener;
    private OnEditListener onEditListener;
    private OnDataSendListener mOnDataSendListener;
    private OnChangeOwnerSelectedListener mOnChangeOwnerSelectedListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mOnUpdateListener = (OnUpdateListener) context;
            deleteListener = (OnDeleteListener) context;
            onEditListener = (OnEditListener) context;
            mOnChangeOwnerSelectedListener = (OnChangeOwnerSelectedListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement appropriate Listeners");
        }
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    /*@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mOnUpdateListener = (OnUpdateListener) activity;
            deleteListener = (OnDeleteListener) activity;
            onEditListener = (OnEditListener) activity;
            mOnChangeOwnerSelectedListener = (OnChangeOwnerSelectedListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement appropriate Listeners");
        }
    }*/

    @Override
    public void onDetach()
    {
        super.onDetach();
    }

   /* interface OnImageUpdateListener
    {
        void onImageUpdated(DialogFragment dialogFragment);
    }*/
    interface OnEditListener
    {
        void onEdit(DialogFragment dialogFragment);
    }

    interface OnChangeOwnerSelectedListener
    {
        void onChangeOwnerSelected(Guest guest);
    }
}

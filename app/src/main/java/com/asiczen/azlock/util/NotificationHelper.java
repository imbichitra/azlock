package com.asiczen.azlock.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.asiczen.azlock.R;

import java.io.File;

public class NotificationHelper extends ContextWrapper {
    private NotificationManager notifManager;
    public static final String CHANNEL_ID="channel1";
    public static final String CHANNEL_NAME = "Channel";
    Context context;

    public NotificationHelper(Context base) {
        super(base);
        context=base;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannels();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createChannels() {

        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,
                CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.setShowBadge(true);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        getManager().createNotificationChannel(notificationChannel);


    }
    public NotificationManager getManager() {
        if (notifManager == null) {
            notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notifManager;
    }

    public NotificationCompat.Builder getNogetNotification1(String title, String body,String file) {
       /* Intent intent=new Intent();
        Uri uri = Uri.parse(Environment.getExternalStorageDirectory() + "/AzLog/");
        intent.setDataAndType(Uri.fromFile(new File(file)), "text/csv");*/

        /*Intent intent=new Intent();
        Uri contentUri = FileProvider.getUriForFile(context,
                context.getApplicationContext().getPackageName(), new File(Environment.getExternalStorageDirectory() + "/AzLog/guest_history_log.csv"));
        context.getApplicationContext().grantUriPermission(context.getApplicationContext().getPackageName(),
                contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        intent.setDataAndType(contentUri, "text/csv");*/
        //String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AzLog/guest_history_log.csv";
        Intent intent=new Intent(Intent.ACTION_VIEW);
        Uri apkURI = FileProvider.getUriForFile(getApplicationContext(),
                getApplicationContext().getPackageName(),new File(file));

        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        String mimeType=myMime.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(apkURI.toString()));//It will return the mimetype

        intent.setDataAndType(apkURI, mimeType);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);


        PendingIntent pendingIntent=PendingIntent.getActivity(this,1,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_cloud_download)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationManager.IMPORTANCE_HIGH);//NotificationManager.IMPORTANCE_HIGH
    }

}

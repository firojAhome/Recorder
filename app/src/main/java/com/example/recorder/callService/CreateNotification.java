package com.example.recorder.callService;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.session.MediaSession;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.recorder.MainActivity;
import com.example.recorder.R;
import com.example.recorder.utils.Constant;

public class CreateNotification {

    public static Notification notification;

    public static void createNotification(Context context, int pauseButton, String contextText){

        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);


        MediaSessionCompat mediaSessionCompat = new MediaSessionCompat(context,"tag");

        Intent pauseIntent = new Intent(context, NotificationActionService.class)
                .setAction(Constant.ACTION_PAUSE);
        PendingIntent pausePendingIntent = PendingIntent.getBroadcast(context,0,
                pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, Constant.CHANNEL_ID);
        notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Recorder is running")
                .setContentText(contextText)
//                    .setLargeIcon(icon)
                .setOnlyAlertOnce(true)//show notification for only first time
                .setShowWhen(false)
                .addAction(pauseButton, "PAUSE", pausePendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0)
                        .setMediaSession(mediaSessionCompat.getSessionToken()))
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        notificationManagerCompat.notify(1, notification);



    }
}

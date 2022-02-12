package com.example.recorder.utils;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.File;
import java.util.Locale;

public class Utils {

    public static boolean isMicrophoneMute(Context context) {
        AudioManager audioManager = (AudioManager)
                context.getSystemService(Context.AUDIO_SERVICE);
        // get original mode
        int originalMode = audioManager.getMode();
        // change mute
        boolean state = audioManager.isMicrophoneMute();
        Log.e("show the ","mic state "+state);
//        audioManager.setMicrophoneMute(state);
        // set mode back
//        audioManager.setMode(originalMode);
        return state;
    }


    public static void setMicrophoneStatus(Context context, boolean state){
        AudioManager audioManager = (AudioManager)
                context.getSystemService(Context.AUDIO_SERVICE);

        audioManager.setMicrophoneMute(state);
    }
    public static boolean checkVibrationIsOn(Context context){
        boolean status = false;
        AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        if(am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE){
            status = true;
        } //vibrate on
        else if (1 == Settings.System.getInt(context.getContentResolver(), "vibrate_when_ringing", 0)) {
            status = true;
        }

        return status;
    }

    private boolean validateMicAvailability(Context context) {
        Boolean available = true;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return false;
        }
        AudioRecord recorder =
                new AudioRecord(MediaRecorder.AudioSource.MIC, 44100,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_DEFAULT, 44100);
        try{
            if(recorder.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED ){
                available = false;

            }

//            recorder.startRecording();
            if(recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING){
//                recorder.stop();
                available = false;
                Log.e("check is ","available false");
            }
//            recorder.stop();
        } finally{
//            recorder.release();
//            recorder = null;
        }

        return available;
    }





    public static boolean getMicrophoneAvailable(Context context) {
        MediaRecorder recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        recorder.setOutputFile(new File(context.getCacheDir(), "MediaUtil#micAvailTestFile").getAbsolutePath());
        boolean available = true;
        try {
            recorder.prepare();
            recorder.start();

        } catch (Exception exception) {
            available = false;
        }
        recorder.release();
        return available;
    }



   /* @RequiresApi(api = Build.VERSION_CODES.O)
    public void startMyOwnForeground() {
        Log.e("show crashes", "android version 8");
        String NOTIFICATION_CHANNEL_ID = "com.ahom.callrecorder";
//        String channelName = "My Background Service";
//        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
//        chan.setLightColor(Color.BLUE);
//        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
//        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        assert manager != null;
//        manager.createNotificationChannel(chan);

        Intent actionActivity = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                actionActivity, 0);

        Intent broadcastIntent = new Intent(this, PhoneCallService.class);
        broadcastIntent.putExtra("toast_message", 0);
        PendingIntent actionIntent = PendingIntent.getBroadcast(this, 0,
                broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CreateNotification.CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setContentText("Recording...")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setColor(Color.BLUE)
                .setContentIntent(contentIntent)
                .build();

        startForeground(1, notification);

    }

    public void showNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this,
                0, intent, 0);
        Intent broadcastPauseIntent = new Intent(this, PhoneCallService.class)
                .setAction(CreateNotification.ACTION_PAUSE);
        PendingIntent actionPauseIntent = PendingIntent.getBroadcast(this, 0,
                broadcastPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        Notification notification = new NotificationCompat.Builder(this, CreateNotification.CHANNEL_ID)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(contentIntent)
                .addAction(R.drawable.pause_button, "pause ", actionPauseIntent)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }*/

/*            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){


        //set background notification layout
//            Bitmap icon = BitmapFactory.decodeResource(context.getResources(), pauseButton);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
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


    } else{
        Notification notification = new NotificationCompat.Builder(context, CreateNotification.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Recorder is running")
                .setContentText(contextText)
                .setOnlyAlertOnce(true)//show notification for only first time
                .setShowWhen(false)
                .addAction(pauseButton, "PAUSE", pausePendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0)
                        .setMediaSession(mediaSessionCompat.getSessionToken()))
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();


        notificationManagerCompat.notify(FOREGROUND_SERVICE_TYPE_MICROPHONE, notification);
    }*/

    public static void cancelNotification(Context ctx, int notifyId) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
        nMgr.cancel(notifyId);
    }


}

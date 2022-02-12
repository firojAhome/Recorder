package com.example.recorder.callService;

import static android.os.Build.VERSION.SDK_INT;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.recorder.MainActivity;
import com.example.recorder.R;
import com.example.recorder.utils.Constant;
import com.example.recorder.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhoneStateReceiver extends Service {

    private static final String ACTION_IN = "android.intent.action.PHONE_STATE";
    private static final String ACTION_OUT = "android.intent.action.NEW_OUTGOING_CALL";

    private final Handler mHandler = new Handler();
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    String callNumber, filePath, audioPath;
    ParcelFileDescriptor parcelFileDescriptor;
    String newDeviceFileName;
    String file_name, time;
    public MediaRecorder recorder;
    public File tempFile;
    public static String Call_Records = "Call Records";
    IntentFilter intentToReceiveFilter = new IntentFilter();
    CallReceiver callReceiver;
    public static String phoneNumber;
    NotificationManager notificationManager;
    boolean isPause = true;
    String recordingPause = "Recording Pause";
    String recordingStart = "Recording Start";
    int seconds = 0;
    Handler handler = new Handler();

    public PhoneStateReceiver() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        callReceiver = new CallReceiver();
        intentToReceiveFilter.addAction(ACTION_IN);
        intentToReceiveFilter.addAction(ACTION_OUT);
        this.registerReceiver(callReceiver, intentToReceiveFilter, null,
                mHandler);


        Thread aThread = new Thread(String.valueOf(this));
        aThread.start();

        serviceLooper = mHandler.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

    }


    private final class ServiceHandler extends Handler {


        public ServiceHandler(@NonNull Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int time = 2;
            for (int i = 0; i < time; i++) {
                Log.v("timer", "i value = " + i);
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Intent actionActivity = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                actionActivity, 0);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? getNotificationChannel(notificationManager) : "";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("App is running in background")
                .setContentIntent(contentIntent)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
        startForeground(110, notification);

        return START_STICKY;
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private String getNotificationChannel(NotificationManager notificationManager) {

        String channelName = getResources().getString(R.string.app_name);
        NotificationChannel channel = new NotificationChannel(Constant.CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return Constant.CHANNEL_ID;
    }



    public void stopForegroundService() {
        stopSelf();
    }

    //old version device
    public void startRecordingOLdDevice(Context context, String number) {

        phoneNumber = number;
        String fileDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        File dateDir = new File(Environment.getExternalStorageDirectory(), "/" + Call_Records + "/" + fileDate);
        File sampleDir = new File(Environment.getExternalStorageDirectory(), "/" + Call_Records + "");
        if (!sampleDir.exists()) {
            sampleDir.mkdir();
        }
        if (!dateDir.exists()) {
            dateDir.mkdir();
        }

        callNumber = number;
        time = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a").format(new Date());

        file_name = number + " " + time;
        try {
            System.out.println("check file name suffix" + file_name);
            tempFile = File.createTempFile(file_name, ".mp3", dateDir);

            filePath = tempFile.getAbsolutePath();
            if (tempFile.exists()) {
                tempFile.delete();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }



        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioEncodingBitRate(96000);
        recorder.setAudioSamplingRate(44100);

        int len = filePath.length() - (44 + file_name.length());
        audioPath = filePath.substring(0, filePath.length() - len).trim() + ".mp3";
        recorder.setOutputFile(audioPath);
        recorder.setAudioChannels(1);

        try {
            recorder.prepare();
            recorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Toast.makeText(context, "Some thing went wrong while capturing the audio", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Some thing went wrong while capturing the audio", Toast.LENGTH_SHORT).show();
        }
    }

    //latest version

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void startCallRecordingNewDevice(Context context, String number) {
        phoneNumber = number;
        try {
            Uri audiouri;
            callNumber = number;
            String fileDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
            time = new SimpleDateFormat("dd-MM-yyyy hh_mm_ssa").format(new Date());
            newDeviceFileName = number + " " + time;
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, newDeviceFileName);
            values.put(MediaStore.Audio.Media.DATE_ADDED, (int) (System.currentTimeMillis() / 1000));
            values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg");
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Call Records/" + fileDate + "/");
            audiouri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
            parcelFileDescriptor = getContentResolver().openFileDescriptor(audiouri, "w");


            if (parcelFileDescriptor != null) {
                recorder = new MediaRecorder();
                Log.e("show recorder ", "start recording id " + recorder);
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setAudioSamplingRate(16000);
                recorder.setOutputFile(parcelFileDescriptor.getFileDescriptor());


                try {
                    recorder.prepare();
                    recorder.start();
//                    recorder.setAudioChannels(1);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }

            }

        } catch (IOException e) {
            Toast.makeText(context, "Some thing went wrong while capturing the audio", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }


    public void stopRecording(Context context) {
        Log.e("shor recorder ", "stop recording id " + recorder);

        if (recorder != null){
            try {
                recorder.stop();
                recorder.reset();
                recorder.release();
                recorder = null;
            } catch (IllegalStateException e) {
                e.printStackTrace();
                Toast.makeText(context, "some thing went wrong while recording", Toast.LENGTH_SHORT).show();
            }
        }

    }


    //method to retrieve contact name
    @SuppressLint("Range")
    private String getContactName(String number, Context context) {
        String contactName = null;

        // define the columns I want the query to return
        String[] projection = new String[]{
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.NUMBER,
                ContactsContract.PhoneLookup.HAS_PHONE_NUMBER};

        // encode the phone number and build the filter URI
        Uri contactUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));

        // query time
        Cursor cursor = context.getContentResolver().query(contactUri,
                projection, null, null, null);

        if (cursor.moveToFirst()) {
            contactName = cursor.getString(cursor
                    .getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            Log.e("show name cursor", "get contact " + contactName);
        }
        cursor.close();

        if (contactName == null) {
            return number;
        }
        return contactName;

//            return contactNumber.equals(null) ? number : contactName;
    }


    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getExtras().getString("actionname");

            Log.e("show the ","broadcast action  "+action);
            if (action.equals("PAUSE")){
                if (isPause){
                    stopRecording(context);
                    CreateNotification.createNotification(context,R.drawable.resume_button,recordingPause);
                    Utils.setMicrophoneStatus(context,false);
                    isPause = false;
                } else {
                    Utils.setMicrophoneStatus(context,true);
                    CreateNotification.createNotification(context,R.drawable.pause_button,recordingStart);
                    if (SDK_INT >= Build.VERSION_CODES.Q) {
                        startCallRecordingNewDevice(context, "pause");
                    } else {
                        startRecordingOLdDevice(context, "pause");
                    }
                    isPause = true;
                }
            }

        }
    };

        public abstract class PhoneCallService extends BroadcastReceiver {
        private int lastState = TelephonyManager.CALL_STATE_IDLE;
        private Date callStartTime;
        private boolean isIncoming;
        public String savedNumber;

        @Override
        public void onReceive(Context context, Intent intent) {

            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            if (Build.VERSION.SDK_INT >= 26 && intent != null && intent.getExtras() != null
                    && TextUtils.isEmpty(intent.getExtras().getString("incoming_number"))) {
                return;
            }


            Bundle bundle = intent.getExtras();
            String phoneNr = bundle.getString("incoming_number");

            savedNumber = intent.getStringExtra("incoming_number");
            String contact = getContactName(savedNumber, context);

            int state = 0;
            if (stateStr != null){
                if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    state = TelephonyManager.CALL_STATE_IDLE;
                    Log.e("looper", "looper event idle" + state);
                    onCallStateChanged(context, state, contact);

                } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    state = TelephonyManager.CALL_STATE_OFFHOOK;
                    Log.e("looper", "looper event hook" + state);
                    onCallStateChanged(context, state, contact);

                } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    state = TelephonyManager.CALL_STATE_RINGING;
                    Log.e("looper", "looper event ringing" + state);
                    onCallStateChanged(context, state, contact);
                }
            }
        }


            private void onCallStateChanged(Context context, int state, String number) {
                if(lastState == state){
                    //No change, debounce extras
                    return;
                }

                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        isIncoming = true;
                        callStartTime = new Date();
                        onIncomingCallReceived(context, number, callStartTime);

                        break;

                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        if(lastState != TelephonyManager.CALL_STATE_RINGING){
                            isIncoming = false;
                            callStartTime = new Date();
                            onOutgoingCallStarted(context, number, callStartTime);
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if(lastState == TelephonyManager.CALL_STATE_RINGING){
                            //Ring but no pickup-  a miss
                            onMissedCall(context, number, callStartTime);
                        }
                        else if(isIncoming){
                            onIncomingCallEnded(context, number, callStartTime, new Date());
                        }
                        else{
                            onOutgoingCallEnded(context, number, callStartTime, new Date());
                        }
                        break;
                }
                lastState = state;

            }

        //Derived classes should override these to respond to specific events of interest
        protected abstract void onIncomingCallReceived(Context ctx, String number, Date start);

        protected abstract void onIncomingCallEnded(Context ctx, String number, Date start, Date end);

        protected abstract void onOutgoingCallStarted(Context ctx, String number, Date start);

        protected abstract void onOutgoingCallEnded(Context ctx, String number, Date start, Date end);

        protected abstract void onMissedCall(Context ctx, String number, Date start);

    }


    public class CallReceiver extends PhoneCallService {

        @Override
        protected void onIncomingCallReceived(Context ctx, String number, Date start) {
            Log.d("onIncomingCallReceived", number + " " + start.toString());

            callStart(ctx,number);
        }


        @Override
        protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
            Log.d("onIncomingCallEnded", number + " " + start.toString() + "\t" + end.toString());
            callEnded(ctx);

        }

        @Override
        protected void onOutgoingCallStarted(Context ctx, String number, Date start) {
            Log.d("onOutgoingCallStarted", number + " " + start.toString());
            callStart(ctx,number);
        }

        @Override
        protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
            Log.d("onOutgoingCallEnded", number + " " + start.toString() + "\t" + end.toString());
            callEnded(ctx);
        }

        @Override
        protected void onMissedCall(Context ctx, String number, Date start) {
            Log.d("onMissedCall", number + " " + start.toString());
        }
    }


    public void callStart(Context ctx,String number){

        CreateNotification.createNotification(ctx,R.drawable.pause_button,recordingStart);
        if (SDK_INT >= Build.VERSION_CODES.Q) {
            startCallRecordingNewDevice(ctx, number);
        } else {
            startRecordingOLdDevice(ctx, number);
        }
        runTimer(ctx);
        registerReceiver(broadcastReceiver,  new IntentFilter("TRACKS_TRACKS"));
    }

    public void callEnded(Context context){
        stopRecording(context);
        CreateNotification.createNotification(context,0,"");
        handler.removeCallbacksAndMessages(null);
        unregisterReceiver(broadcastReceiver);
    }


    private void runTimer(Context context) {

        handler.post(new Runnable() {
            @Override

            public void run() {
                int hours = seconds / 3600;
                int minutes = (seconds % 3600) / 60;
                int secs = seconds % 60;

                String time = String
                        .format(Locale.getDefault(),
                                "%d:%02d:%02d", hours,
                                minutes, secs);

                if (Utils.isMicrophoneMute(context)){
                    Log.e("recording stop","mic  mute");
                    if (recorder != null){
                        stopRecording(context);
                        CreateNotification.createNotification(context,R.drawable.resume_button,recordingPause);
                    }
                }else{
                    if (recorder == null){
                            CreateNotification.createNotification(context,R.drawable.pause_button,recordingStart);
                            if (SDK_INT >= Build.VERSION_CODES.Q) {
                                startCallRecordingNewDevice(context, "mute");
                            } else {
                                startRecordingOLdDevice(context, "mute");
                            }
                            isPause = false;
                    }
                    Log.e("recording start","mic is not mute");
                }
                Log.e("show timer"+time,"status of sec"+seconds++);


                handler.postDelayed(this, 1000);
            }
        });
    }




    @Override
    public void onDestroy() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);
        super.onDestroy();
    }

}
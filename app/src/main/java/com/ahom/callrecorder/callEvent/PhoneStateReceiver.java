package com.ahom.callrecorder.callEvent;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.ahom.callrecorder.R;
import com.ahom.callrecorder.RecordsHome;
import com.ahom.callrecorder.storage.Preferences;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
import static android.os.Build.VERSION.SDK_INT;
import static com.ahom.callrecorder.callEvent.App.CHANNEL_ID;
import static com.ahom.callrecorder.storage.Constant.Call_Records;

@RequiresApi(api = Build.VERSION_CODES.O)
public class PhoneStateReceiver extends Service {


    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private final Handler mHandler = new Handler();
    String time;


    RecordsHome recordsHome = new RecordsHome();
    Context applicationContext = recordsHome.getContextOfApplication();

    private MediaRecorder recorder = null;
    private File tempFile;
    String callNumber, filePath, audioPath;
    ParcelFileDescriptor parcelFileDescriptor;
    String newDeviceFileName;
    String file_name;
    private boolean recordStarted = false;
    private static final String ACTION_IN = "android.intent.action.PHONE_STATE";
    private static final String ACTION_OUT = "android.intent.action.NEW_OUTGOING_CALL";

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
    public void onCreate() {
        super.onCreate();

        IntentFilter intentToReceiveFilter = new IntentFilter();
        intentToReceiveFilter.addAction(ACTION_IN);
        intentToReceiveFilter.addAction(ACTION_OUT);
        this.registerReceiver(new CallReceiver(), intentToReceiveFilter, null,
                mHandler);

        Thread aThread = new Thread(String.valueOf(this));
        aThread.start();

        serviceLooper = mHandler.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

//        String input = intent.getStringExtra("inputExtra");

        Intent notificationIntent = new Intent(getApplicationContext(), RecordsHome.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
//                .setContentText(input)
                .setContentTitle("Foreground Service")
                .setContentIntent(pendingIntent)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else {
//            startForeground(1, notification);
            startForeground(FOREGROUND_SERVICE_TYPE_MICROPHONE, notification);
        }


        return START_STICKY;
    }

    public void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = "com.ahom.callrecorder";
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);


        Intent notificationIntent = new Intent(this, RecordsHome.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(FOREGROUND_SERVICE_TYPE_MICROPHONE, notification);
    }

    public void stopForegroundService() {
        Log.e("TAG_FOREGROUND_SERVICE", "Stop foreground service.");
//        stopForeground(true);
        stopSelf();
    }

    private void startRecording(String number, Date date) {

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

//            audiofile.deleteOnExit();

            int len = filePath.length() - (44 + file_name.length()); //19 // 44 + f

            String sub = filePath.substring(0, filePath.length() - len) + ".mp3";
            String sub1 = filePath.substring(0, (int) (filePath.length() - 13)) + ".mp3";
            Log.e("print audio path", "" + tempFile);
            Log.e("print audio sub", "temp" + tempFile.getAbsolutePath());
            Log.e("print audio sub", "sub1 " + sub1);
            Log.e("print audio sub", "sub len" + filePath.length());
            Log.e("print audio sub", " len" + len);


        } catch (IOException e) {
            e.printStackTrace();
        }

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
//        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
//        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int len = filePath.length() - (44 + file_name.length());
        audioPath = filePath.substring(0, filePath.length() - len).trim() + ".mp3";
        Log.e("show phone state","audio path"+audioPath);
        recorder.setOutputFile(audioPath);

        try {
            recorder.prepare();
            recorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        recordStarted = true;

    }

    private void stopRecording() {
        if (recordStarted) {
            try {
                recorder.stop();
                recorder.release();
                recorder = null;
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            recordStarted = false;
        }

        Log.e("show phone state","audio path"+audioPath);
        if (audioPath != null || parcelFileDescriptor != null) {
            shareInStorage();

        }
    }

    private void shareInStorage() {

        String fileDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());

        String fullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
        File f = new File(fullPath+"/Call Records/"+fileDate+"/" +newDeviceFileName);
        String path = String.valueOf(f)+".mp3";

        Log.e("show storage","path f "+fullPath);
        Log.e("show storage","path f "+ f);


        Log.e("check ", "storage" + Preferences.getRadioIndex(getApplicationContext(), "radioIndex"));
        String prefToken = Preferences.getDropBoxAccessToken(getApplicationContext(), "Drop_Box_Access_Token");
        switch (Preferences.getRadioIndex(getApplicationContext(), "radioIndex")) {
            case 0:
//                String folderName = callNumber + " " + time;
                GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
                if (acct != null) {
                    if (SDK_INT >= Build.VERSION_CODES.R) {
                        recordsHome.startDriveStorage(getApplicationContext(), newDeviceFileName, path);
                    }else {
                        recordsHome.startDriveStorage(getApplicationContext(), file_name, audioPath);
                    }
                }
                break;

            case 1:
                if (prefToken != null) {
                    if (SDK_INT >= Build.VERSION_CODES.R) {
                        recordsHome.storeInDropBox(getApplicationContext(), newDeviceFileName, path, prefToken);
                    }else {
                        recordsHome.storeInDropBox(getApplicationContext(), file_name, audioPath, prefToken);
                    }
                }
                break;

            case 2:
                Log.e("check ", "log 3");
                if (Preferences.isOneDriveLogin(this, "Is_One_DriveLogIn")) {
                    String folderTime = new SimpleDateFormat("_dd-MMM-yyyy_hh_mm.ssa").format(new Date());
//                    String s = URLEncoder.encode(folderTime,"UTF-8");
                    String oneDriveFileName = callNumber + folderTime + ".mp3".trim();
                    Log.e("show phone state","audio path"+audioPath);
                    if (SDK_INT >= Build.VERSION_CODES.R) {
                        recordsHome.silentOneDriveStorage(getApplicationContext(), oneDriveFileName, path);
                    }else {
                        recordsHome.silentOneDriveStorage(getApplicationContext(), oneDriveFileName, audioPath);
                    }
                }

                break;
            case 3:
                break;
            default:
                break;

        }
    }

    public abstract class PhoneCallService extends BroadcastReceiver {
        private int lastState = TelephonyManager.CALL_STATE_IDLE;
        private Date callStartTime;
        private boolean isIncoming;
        public String savedNumber;

        @Override
        public void onReceive(Context context, Intent intent) {

            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            if(Build.VERSION.SDK_INT >= 26 && intent!=null && intent.getExtras() !=null
                    && TextUtils.isEmpty(intent.getExtras().getString("incoming_number"))){
                return;
            }

            Bundle bundle = intent.getExtras();
            String phoneNr= bundle.getString("incoming_number");
            Log.e("show phone number",""+phoneNr);

            savedNumber = intent.getStringExtra("incoming_number");
            String contact = getContactName(savedNumber, context);

            Log.e("show contact name","call state"+contact);

            int state = 0;
            if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                state = TelephonyManager.CALL_STATE_IDLE;
                Log.e("looper","looper event idle"+state);
                onCallStateChanged(context, state, contact);

            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                state = TelephonyManager.CALL_STATE_OFFHOOK;
                Log.e("looper","looper event hook"+state);
                onCallStateChanged(context, state, contact);

            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                state = TelephonyManager.CALL_STATE_RINGING;
                Log.e("looper","looper event ringing"+state);
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
                    if (SDK_INT >= Build.VERSION_CODES.R) {
                        startCallRecording(number,callStartTime);
                    }else {
                        startRecording(number,callStartTime);
                    }
                    break;

                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if(lastState != TelephonyManager.CALL_STATE_RINGING){
                        isIncoming = false;
                        callStartTime = new Date();
                        onOutgoingCallStarted(context, number, callStartTime);

                            if (SDK_INT >= Build.VERSION_CODES.R) {
                                startCallRecording(number,callStartTime);
                            }else {
                                startRecording(number,callStartTime);
                            }
                        }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if(lastState == TelephonyManager.CALL_STATE_RINGING){
                        //Ring but no pickup-  a miss
                        onMissedCall(context, number, callStartTime);
                    }
                    else if(isIncoming){
                        onIncomingCallEnded(context, number, callStartTime, new Date());
                        stopRecording();
                    }
                    else{
                        onOutgoingCallEnded(context, number, callStartTime, new Date());
                        stopRecording();
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
        }


        @Override
        protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
            Log.d("onIncomingCallEnded", number + " " + start.toString() + "\t" + end.toString());
        }

        @Override
        protected void onOutgoingCallStarted(Context ctx, String number, Date start) {
            Log.d("onOutgoingCallStarted", number + " " + start.toString());
        }

        @Override
        protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
            Log.d("onOutgoingCallEnded", number + " " + start.toString() + "\t" + end.toString());
        }

        @Override
        protected void onMissedCall(Context ctx, String number, Date start) {
            Log.d("onMissedCall", number + " " + start.toString());
        }

    }

    public class Restarter extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("Broadcast Listened", "Service tried to stop");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.startForegroundService(new Intent(context, PhoneStateReceiver.class));
                startMyOwnForeground();

            } else {
                Notification notification  = new Notification();
                startForeground(1, notification);
            }
        }
    }



    //method to retrieve contact name
        private String getContactName(String number, Context context) {
            String contactName = null;

            // // define the columns I want the query to return
            String[] projection = new String[] {
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.NUMBER,
                    ContactsContract.PhoneLookup.HAS_PHONE_NUMBER };

            // encode the phone number and build the filter URI
            Uri contactUri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(number));

            // query time
            Cursor cursor = context.getContentResolver().query(contactUri,
                    projection, null, null, null);
            // querying all contacts = Cursor cursor =
            // context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
            // projection, null, null, null);

            if (cursor.moveToFirst()) {
                contactName = cursor.getString(cursor
                        .getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                Log.e("show number cursor","get contact "+number);
                Log.e("show name cursor","get contact "+contactName);
            }
            cursor.close();

            if (contactName == null){
                return number;
            }
            return contactName;

//            return contactNumber.equals(null) ? number : contactName;
        }



    @RequiresApi(api = Build.VERSION_CODES.R)
    private void startCallRecording(String number, Date date) {
        try{
            Uri audiouri;

//            ParcelFileDescriptor file;
            callNumber = number;
            String fileDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
            time = new SimpleDateFormat("dd-MM-yyyy hh_mm_ss a").format(new Date());
            newDeviceFileName = number + " " + time;
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, newDeviceFileName+".mp3");
            values.put(MediaStore.Audio.Media.DATE_ADDED, (int) (System.currentTimeMillis() / 1000));
            values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg");
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Call Records/"+fileDate+"/");
            audiouri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
            parcelFileDescriptor = getContentResolver().openFileDescriptor(audiouri, "w");

            if (parcelFileDescriptor != null) {
                recorder = new MediaRecorder();

                recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

//                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_2_TS);
//                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setOutputFile(parcelFileDescriptor.getFileDescriptor());
                recorder.setAudioChannels(1);
                recorder.prepare();
                recorder.start();

            }

        }catch(IOException e) {
            e.printStackTrace();
        }

        recordStarted = true;
    }



    @Override
    public void onDestroy() {
//        stopForegroundService();
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(new CallReceiver());
//        getApplicationContext().unregisterReceiver(new CallReceiver());
        super.onDestroy();
    }


//    https://github.com/judemanutd/AutoStarter  .../autostart permission
}

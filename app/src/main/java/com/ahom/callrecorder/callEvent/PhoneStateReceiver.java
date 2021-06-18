package com.ahom.callrecorder.callEvent;

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
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
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
import android.provider.Settings;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
import static android.media.AudioManager.*;
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



    AudioRecord audioRecord;
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format
    AudioManager audiomanager;

    private MediaProjectionManager _mediaProjectionManager;
    private MediaProjection _mediaProjection;
    private Thread _recordingThread = null;


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
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(FOREGROUND_SERVICE_TYPE_MICROPHONE, notification);

    }

    public void stopForegroundService() {
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

            } catch (IOException e) {
                e.printStackTrace();
            }

            recorder = new MediaRecorder();

            //android.permission.MODIFY_AUDIO_SETTINGS
            AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            mAudioManager.setSpeakerphoneOn(true);
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 100, 0);

            //turn on speaker
            if (mAudioManager != null) {
                mAudioManager.setMode(MODE_IN_COMMUNICATION); //MODE_IN_COMMUNICATION | MODE_IN_CALL
                mAudioManager.setSpeakerphoneOn(true);
                mAudioManager.setStreamVolume(STREAM_VOICE_CALL, mAudioManager.getStreamMaxVolume(STREAM_VOICE_CALL), 0); // increase Volume
                hasWiredHeadset(mAudioManager);
            }

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
                if (SDK_INT >= Build.VERSION_CODES.P) {
                    recorder.getActiveMicrophones();
                }
                recorder.start();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            recordStarted = true;
        }

    private boolean hasWiredHeadset(AudioManager mAudioManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return mAudioManager.isWiredHeadsetOn();
        } else {
            @SuppressLint("WrongConstant") final AudioDeviceInfo[] devices = mAudioManager.getDevices(AudioManager.GET_DEVICES_ALL);
            for (AudioDeviceInfo device : devices) {
                final int type = device.getType();
                if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                    Log.d("LOG_TAG_S", "hasWiredHeadset: found wired headset");
                    return true;
                } else if (type == AudioDeviceInfo.TYPE_USB_DEVICE) {
                    Log.d("LOG_TAG_S", "hasWiredHeadset: found USB audio device");
                    return true;
                } else if (type == AudioDeviceInfo.TYPE_TELEPHONY) {
                    Log.d("LOG_TAG_S", "hasWiredHeadset: found audio signals over the telephony network");
                    return true;
                }
            }
            return false;
        }
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

        if (audioPath != null || parcelFileDescriptor != null) {
            shareInStorage();
        }
    }

    private void shareInStorage() {

        String fileDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());

        String fullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
        File f = new File(fullPath+"/Call Records/"+fileDate+"/" +newDeviceFileName);
        String path = String.valueOf(f)+".mp3";

        Log.e("check ", "storage" + Preferences.getRadioIndex(getApplicationContext(), "radioIndex"));
        String prefToken = Preferences.getDropBoxAccessToken(getApplicationContext(), "Drop_Box_Access_Token");
        switch (Preferences.getRadioIndex(getApplicationContext(), "radioIndex")) {
            case 0:
//                String folderName = callNumber + " " + time;
                GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
                if (acct != null) {
                    if (SDK_INT >= Build.VERSION_CODES.Q) {
                        recordsHome.startDriveStorage(getApplicationContext(), newDeviceFileName, path);
                    }else {
                        recordsHome.startDriveStorage(getApplicationContext(), file_name, audioPath);
                    }
                }
                break;

            case 1:
                if (prefToken != null) {
                    if (SDK_INT >= Build.VERSION_CODES.Q) {
                        recordsHome.storeInDropBox(getApplicationContext(), newDeviceFileName, path, prefToken);
                    }else {
                        recordsHome.storeInDropBox(getApplicationContext(), file_name, audioPath, prefToken);
                    }
                }
                break;

            case 2:
                if (Preferences.isOneDriveLogin(this, "Is_One_DriveLogIn")) {
                    String folderTime = new SimpleDateFormat("_dd-MMM-yyyy_hh_mm_ssa").format(new Date());
//                    String s = URLEncoder.encode(folderTime,"UTF-8");
                    String oneDriveFileName = callNumber + folderTime + ".mp3".trim();
                    Log.e("show phone state","audio path"+audioPath);
                    if (SDK_INT >= Build.VERSION_CODES.Q) {
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
                onCallStateChanged(context, state, contact);
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
                    if (SDK_INT >= Build.VERSION_CODES.Q) {
                        startCallRecording(number,callStartTime);
                    }else {
                        startRecording(number,callStartTime);
//                        startAudioRecording(number,_mediaProjection);
                    }
                    break;

                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if(lastState != TelephonyManager.CALL_STATE_RINGING){
                        isIncoming = false;
                        callStartTime = new Date();
                        onOutgoingCallStarted(context, number, callStartTime);

                            if (SDK_INT >= Build.VERSION_CODES.Q) {
                                startCallRecording(number,callStartTime);
                            }else {
                                startRecording(number,callStartTime);
//                                startAudioRecording(_mediaProjection);
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.startForegroundService(new Intent(context, PhoneStateReceiver.class));
                startMyOwnForeground();

            } else {
                Notification notification  = new Notification();
                startForeground(FOREGROUND_SERVICE_TYPE_MICROPHONE, notification);
            }
        }
    }


    //method to retrieve contact name
        private String getContactName(String number, Context context) {
            String contactName = null;

             // define the columns I want the query to return
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
                Log.e("show name cursor","get contact "+contactName);
            }
            cursor.close();

            if (contactName == null){
                return number;
            }
            return contactName;

//            return contactNumber.equals(null) ? number : contactName;
        }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void startCallRecording(String number, Date date) {

        audiomanager = (AudioManager)getSystemService(AUDIO_SERVICE);
        audiomanager.setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_ALL);
        audiomanager.setMode(MODE_IN_CALL);
        audiomanager.startBluetoothSco();

        audiomanager.setStreamVolume(STREAM_VOICE_CALL,
                audiomanager.getStreamMaxVolume(STREAM_VOICE_CALL), 0);
        audiomanager.setSpeakerphoneOn(true);

        try{
            Uri audiouri;
//            ParcelFileDescriptor file;
            callNumber = number;
            String fileDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
            time = new SimpleDateFormat("dd-MM-yyyy hh_mm_ssa").format(new Date());
            newDeviceFileName = number + " " + time;
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.DISPLAY_NAME, newDeviceFileName);
            values.put(MediaStore.Audio.Media.DATE_ADDED, (int) (System.currentTimeMillis() / 1000));
            values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg");
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Call Records/"+fileDate+"/");
            audiouri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
            parcelFileDescriptor = getContentResolver().openFileDescriptor(audiouri, "w");

//            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, audiouri));

            if (parcelFileDescriptor != null) {
                recorder = new MediaRecorder();
//                recorder.setPrivacySensitive(false);
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);


//                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_2_TS);
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


    // try code
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void startAudioRecording(Intent callingIntent) {

        _mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        _mediaProjection = _mediaProjectionManager.getMediaProjection(-1, callingIntent);
        if (SDK_INT >= Build.VERSION_CODES.Q) {
            startAudioRecording(_mediaProjection);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void startAudioRecording(MediaProjection mediaProjection) {

        AudioPlaybackCaptureConfiguration config =
                null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            config = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                    .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .build();
        }

        AudioFormat audioFormat = new AudioFormat.Builder()
                .setEncoding(RECORDER_AUDIO_ENCODING)
                .setSampleRate(RECORDER_SAMPLERATE)
                .setChannelMask(RECORDER_CHANNELS)
                .build();
        if (SDK_INT >= Build.VERSION_CODES.Q) {
            audioRecord = new AudioRecord.Builder()
    //                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                    .setAudioPlaybackCaptureConfig(config)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(BufferElements2Rec * BytesPerElement)
                    .build();
        }

        recorder.setAudioSource(audioRecord.getAudioSource());
        audioRecord.startRecording();

        _recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordStarted = true;
        _recordingThread.start();
    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private void writeAudioDataToFile() {

/*        File sampleDir = new File(getExternalFilesDir(null), "/TestRecordingDasa1");
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }*/
        String fileDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        File dateDir = new File(Environment.getExternalStorageDirectory(), "/" + Call_Records + "/" + fileDate);
        File sampleDir = new File(Environment.getExternalStorageDirectory(), "/" + Call_Records + "");
        if (!sampleDir.exists()) {
            sampleDir.mkdir();
        }
        if (!dateDir.exists()) {
            dateDir.mkdir();
        }

        String fileName = "Record-" + new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss").format(new Date()) + ".pcm";
        String filePath = sampleDir.getAbsolutePath() + "/" + fileName;
        //String filePath = "/sdcard/voice8K16bitmono.pcm";
        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (recordStarted) {
            // gets the voice output from microphone to byte format
            audioRecord.read(sData, 0, BufferElements2Rec);
            System.out.println("Short wirting to file" + sData.toString());
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
                Log.e("show the ","output data "+bData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i("MainActivity.LOG_PREFIX", String.format("Recording finished. File saved to '%s'\nadb pull %s .", filePath, filePath));

    }


    private void initRecorder() {
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setVideoEncodingBitRate(512 * 1000);
        recorder.setOutputFile("/sdcard/capture.mp4");
    }



    public  static  boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        //your package /   accesibility service path/class
        //
        // final String service = "com.example.sotsys_014.accessibilityexample/com.accessibilityexample.Service.MyAccessibilityService";

        final String service = "nisarg.app.demo/nisarg.app.demo.MyService";


        boolean accessibilityFound = false;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            Log.v("LOG_TAG_S", "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.e("LOG_TAG_S", "Error finding setting, default accessibility to not found: "
                    + e.getMessage());
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            Log.v("LOG_TAG_S", "***ACCESSIBILIY IS ENABLED*** -----------------");
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
                splitter.setString(settingValue);
                while (splitter.hasNext()) {
                    String accessabilityService = splitter.next();

                    Log.v("LOG_TAG_S", "-------------- > accessabilityService :: " + accessabilityService);
                    if (accessabilityService.equalsIgnoreCase(service)) {
                        Log.v("LOG_TAG_S", "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }
                }
            }
        } else {
            Log.v("LOG_TAG_S", "***ACCESSIBILIY IS DISABLED***");
        }

        return accessibilityFound;
    }


    

//    https://github.com/judemanutd/AutoStarter  .../autostart permission
}

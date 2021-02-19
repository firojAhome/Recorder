package com.example.recorder.callEvent;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.recorder.Home;
import com.example.recorder.google.GoogleDriveLogin;
import com.example.recorder.onedrive.OneDrive;
import com.example.recorder.storage.Preferences;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.example.recorder.callEvent.App.CHANNEL_ID;

@RequiresApi(api = Build.VERSION_CODES.O)
public class PhoneStateReceiver extends Service{

    GoogleDriveLogin googleDriveLogin = new GoogleDriveLogin();
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private final Handler mHandler = new Handler();
    String time;

    Home home = new Home();
    Context applicationContext = Home.getContextOfApplication();

    private static final String TAG = "PhoneStateReceiver";
    private MediaRecorder recorder = null;
    private File tempFile;
    String callNumber,filePath,audioPath;
    private boolean recordstarted = false;
    private static final String ACTION_IN = "android.intent.action.PHONE_STATE";
    private static final String ACTION_OUT = "android.intent.action.NEW_OUTGOING_CALL";

    private final class ServiceHandler extends Handler {

        public ServiceHandler(@NonNull Looper looper) {
            super(looper);

        }

        public void handleMessage(Message msg) {
            int time = 5;
            for (int i=0; i<time; i++){
                Log.v("timer", "i value = "+i);
            }
            try {
                Thread.sleep(5000);
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

//         String input = intent.getStringExtra("input extrat");

        Intent notificationIntent = new Intent(this,Home.class);
         PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

         Notification notification = new NotificationCompat.Builder(this,CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentIntent(pendingIntent)
                .build();
         startForeground(1, notification);

        return START_STICKY;
    }


    public void stopForegroundService() {
        Log.e("TAG_FOREGROUND_SERVICE", "Stop foreground service.");

        // Stop foreground service and remove the notification.
//        stopForeground(true);

        // Stop the foreground service.
        stopSelf();
    }

    private void startRecording(String number, Date date) {

        String fileDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        File dateDir = new File(Environment.getExternalStorageDirectory(),"/CallRecords/"+fileDate);
        File sampleDir = new File(Environment.getExternalStorageDirectory(), "/CallRecords");
        if (!sampleDir.exists()) {
            sampleDir.mkdir();
        } if (!dateDir.exists()){
            dateDir.mkdir();
        }

        callNumber = number;
        time = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss ").format(new Date());

        String file_name = number +" "+ time;
        try {
            System.out.println("check file name suffix"+file_name);
            tempFile = File.createTempFile(file_name,".mp3",dateDir);

            filePath = tempFile.getAbsolutePath();
            if (tempFile.exists()) {
                tempFile.delete();
            }

//            audiofile.deleteOnExit();
            String sub = filePath.substring(0,filePath.length()-13)+".mp3";
            Log.e("print audio path",""+tempFile);
            Log.e("print audio sub",""+sub);


        } catch (IOException e) {
            e.printStackTrace();
        }

        recorder = new MediaRecorder();
//                          recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        audioPath = filePath.substring(0,filePath.length()-14)+".mp3";
        recorder.setOutputFile(audioPath);

        try {
            recorder.prepare();
            recorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        recordstarted = true;

    }


    private void stopRecording() {
        if (recordstarted) {
            try{
                recorder.stop();
            }catch (IllegalStateException e){
                e.printStackTrace();
            }
            recordstarted = false;
        }

        if(audioPath != null){
            shareInStorage();
        }
    }

    private void shareInStorage() {
        Log.e("check ","storage"+Preferences.getRadioIndex(getApplicationContext(),"radioIndex"));
        String prefToken = Preferences.getDropBoxAccessToken(applicationContext,"Drop_Box_Access_Token");
        switch (Preferences.getRadioIndex(getApplicationContext(),"radioIndex")){
            case 0:
                GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
                if (acct != null) {
                    String folderName = callNumber+" "+time;
                    googleDriveLogin.startDriveStorage(getApplicationContext(),folderName,audioPath);
                }
                break;
            case 1:
                home.storeInDropBox(getApplicationContext(),callNumber,audioPath,prefToken);
                break;
            case 2:
                Log.e("check ","log 3");
                String folderTime = new SimpleDateFormat(" dd-MM-yyyy hh-mm-ss").format(new Date());
                String oneDriveFileName = callNumber+folderTime+".mp3".trim();
                OneDrive oneDrive = new OneDrive();
                oneDrive.silentOneDriveStorage(getApplicationContext(),oneDriveFileName,audioPath);
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
        private String savedNumber;

        @Override
        public void onReceive(Context context, Intent intent)   {
            //We listen to two intents.  The new outgoing call only tells us of an outgoing call.  We use it to get the number.
            if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
                savedNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
            } else {
                String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);

                 savedNumber = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                int state = 0;
                if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    state = TelephonyManager.CALL_STATE_IDLE;
                    Log.e("looper","looper event");

                } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    state = TelephonyManager.CALL_STATE_OFFHOOK;
                    Log.e("looper","looper event");

                } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    state = TelephonyManager.CALL_STATE_RINGING;
                    Log.e("looper","looper event");

                }

                onCallStateChanged(context, state, savedNumber);
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
                    savedNumber = number;
                    onIncomingCallReceived(context, savedNumber, callStartTime);
                    startRecording(savedNumber, callStartTime);
                    break;

                case TelephonyManager.CALL_STATE_OFFHOOK:
                    //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                    if(lastState != TelephonyManager.CALL_STATE_RINGING){
                        isIncoming = false;
                        callStartTime = new Date();
                        onOutgoingCallStarted(context, savedNumber, callStartTime);
                        startRecording(savedNumber, callStartTime);

                        }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                    if(lastState == TelephonyManager.CALL_STATE_RINGING){
                        //Ring but no pickup-  a miss
                        onMissedCall(context, savedNumber, callStartTime);
                    }
                    else if(isIncoming){
                        onIncomingCallEnded(context, savedNumber, callStartTime, new Date());
                        stopRecording();
                    }
                    else{
                        onOutgoingCallEnded(context, savedNumber, callStartTime, new Date());
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


    @Override
    public void onDestroy() {

        stopForegroundService();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(new CallReceiver());
//        this.unregisterReceiver(new CallReceiver());
        super.onDestroy();
    }

}

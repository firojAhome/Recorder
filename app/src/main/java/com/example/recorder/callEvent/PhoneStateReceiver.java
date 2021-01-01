package com.example.recorder.callEvent;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.recorder.Home;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.O)
public class PhoneStateReceiver extends Service {

    private static final String TAG = "PhoneStateReceiver";
    private MediaRecorder recorder = null;
    private File audiofile;
    private boolean recordstarted = false;
    private static final String ACTION_IN = "android.intent.action.PHONE_STATE";
    private static final String ACTION_OUT = "android.intent.action.NEW_OUTGOING_CALL";

//    Date dirDate = new java.util.Date(System.currentTimeMillis());
//    String baseDir = "/CallRecords";
//    String newDir = createDateBasedDirectory(baseDir, dirDate);

    public PhoneStateReceiver() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_OUT);
        filter.addAction(ACTION_IN);
        this.registerReceiver(new CallReceiver(), filter);
        return super.onStartCommand(intent, flags, startId);
    }


    private void startRecording(String number, Date date) {

        File sampleDir = new File(Environment.getExternalStorageDirectory(), "/CallRecords");
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.FRANCE);
        String file_name = number + "-"+ date;
        try {
            audiofile = File.createTempFile(file_name, ".mp3", sampleDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        recorder = new MediaRecorder();
//                          recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        recorder.setOutputFile(audiofile.getAbsolutePath());

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
                recorder.reset();
                recorder.release();
                recorder = null;
            }catch (IllegalStateException e){
                e.printStackTrace();
            }
            recordstarted = false;
        }

        Home home = new Home();
        home.storeInDropBox(audiofile.getAbsolutePath());
    }

    public abstract class PhoneCallService extends BroadcastReceiver {
        private int lastState = TelephonyManager.CALL_STATE_IDLE;
        private Date callStartTime;
        private boolean isIncoming;
        private String savedNumber;

        @Override
        public void onReceive(Context context, Intent intent) {

            //We listen to two intents.  The new outgoing call only tells us of an outgoing call.  We use it to get the number.
            if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
                savedNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
            } else {
                String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);

                 savedNumber = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                int state = 0;
                if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    state = TelephonyManager.CALL_STATE_IDLE;
                } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    state = TelephonyManager.CALL_STATE_OFFHOOK;
                } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    state = TelephonyManager.CALL_STATE_RINGING;
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
    public void unregisterReceiver(BroadcastReceiver receiver) {
        if (recordstarted == false){
            super.unregisterReceiver(receiver);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String createDateBasedDirectory(String baseDirectory, Date argDate) {
        String newDir = null;

        if (baseDirectory != null && argDate != null) {
            try {
                String format = "yyyy-MM-dd";
                DateFormat dateFormatter = new SimpleDateFormat(format);
                String date = dateFormatter.format(argDate);

                // check if the directory exists:

                String todaysLogDir = baseDirectory + "\\" + date; // create the path as String

                // then create a Path (java.nio, alternatives possible)
                Path todaysDirectoryPath = Paths.get(todaysLogDir);
                // and check if this Path exists
                if (Files.exists(todaysDirectoryPath)){
                    // if present, just return it in order to write (into) a log file there
                    return todaysDirectoryPath.toUri().toString();
                } else {
                    newDir = baseDirectory + date;
                    new File(newDir).mkdir();
                    // create it the way you want and return the path as String

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }else {
            newDir = baseDirectory + argDate;
            new File(newDir).mkdir();
        }

        return newDir;
    }
}
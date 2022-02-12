package com.example.recorder.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityRecord;

import com.example.recorder.callService.PhoneStateReceiver;

import java.util.Date;

public class MyAccessibilityService extends AccessibilityService {

    private Context context;
    public static final String CHANNEL_ID = "MyAccessibilityService";
    private static final String ACTION_IN = "android.intent.action.PHONE_STATE";
    private static final String ACTION_OUT = "android.intent.action.NEW_OUTGOING_CALL";
    private final Handler mHandler = new Handler();

    MediaRecorder mRecorder;
    private boolean isStarted;
    byte buffer[] = new byte[8916];


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.e("TAG", "Event :"+event.getEventType());

        if (Build.VERSION.SDK_INT >= 28) {
//            DeviceInfo f8 = f();
//            if (n.a(f8.k(), n.U0("FNZFHATN_ONTO_DEVICE_LAUNCH", 7), true)) {
//                q(accessibilityEvent);
//            } else if ((n.a(f8.k(), n.U0("TBBTYRYA_PBAARPGVBA_QRIVPR_YNHAPU", 6), true)) || (n.a(f8.k(), n.U0("ZBGBEBYN_ONTO_DEVICE_LAUNCH", 8), true)) || (n.a(f8.k(), n.U0("UZQ Tybony_ONTO_DEVICE_LAUNCH", 10), true))) {
//                o(accessibilityEvent);
//            } else if (n.a(f8.k(), n.U0("BARCYHFYN_ONTO_DEVICE_LAUNCH", 7), true)) {
//                m(accessibilityEvent);
//            } else if (n.a(f8.k(), n.U0("KVNBZVLN_CONNECTION_DEVICE_LAUNCH", 6), true)) {
//                y(accessibilityEvent);
//            } else if (n.a(f8.k(), n.U0("UHNJRV_CONNECTION_DEVICE_LAUNCH", 6), true)) {
//                g(accessibilityEvent);
//            } else if (n.a(f8.k(), n.U0("FBALEBYN_ONTO_DEVICE_LAUNCH", 4), true)) {
//                s(accessibilityEvent);
//            } else if (n.a(f8.k(), n.U0("BCCBVLN_CONNECTION_DEVICE_LAUNCH", 4), true)) {
//                n(accessibilityEvent);
//            } else if (n.a(f8.k(), n.U0("IVIBVLN_CONNECTION_DEVICE_LAUNCH", 4), true)) {
//                g(accessibilityEvent);
//            } else if (n.a(f8.k(), n.U0("ernyzr__CONNECTION_DEVICE_LAUNCH", 6), true)) {
//                p(accessibilityEvent);
//            } else if (n.a(f8.k(), n.U0("YTRFHATN_ONTO_DEVICE_LAUNCH", 3), true)) {
//                i(accessibilityEvent);
//            }
        }
    }


    @Override
    public void onInterrupt() {
        Log.e("TAG","show the interrupt:error");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("TAG","MyAccessibilityService Salesken Started ...");

    }

    /* access modifiers changed from: protected */
    @SuppressLint("WrongConstant")
    public void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo accessibilityServiceInfo = new AccessibilityServiceInfo();
        accessibilityServiceInfo.eventTypes = 2080;
        accessibilityServiceInfo.packageNames = new String[]{"com.example.recorder"};
        accessibilityServiceInfo.feedbackType = 1;
        accessibilityServiceInfo.notificationTimeout = 100;
        accessibilityServiceInfo.flags = 80;
        setServiceInfo(accessibilityServiceInfo);


    }

    public int onStartCommand(Intent intent, int i, int i2) {
        if (intent != null){
            String  action = intent.getAction();
            Log.e("show the action ","w accessibillity "+action);
        }
        return super.onStartCommand(intent, i, i2);

    }




    @Override
    public void onDestroy() {
        super.onDestroy();
    }


}

package com.example.recorder;

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.example.recorder.callService.CreateNotification;
import com.example.recorder.callService.PhoneStateReceiver;
import com.example.recorder.callService.Restarter;
import com.example.recorder.utils.Constant;
import com.example.recorder.utils.Preferences;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    RadioGroup radioGroup;
    SwitchCompat recording_switchCompat;
    LinearLayout recording_layout;
    RelativeLayout relative_permission;
    TextView allow_permission;
    //radio button
    RadioButton google;
    RadioButton dropBox;
    RadioButton oneDrive;
    RadioButton local;
    NotificationManager notificationManager;

    PhoneStateReceiver phoneStateReceiver = new PhoneStateReceiver();

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.READ_CONTACTS,Manifest.permission.READ_CALL_LOG, Manifest.permission.FOREGROUND_SERVICE};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //permission
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        findView();
        selectRadioButton();
        setChecked();
        recordingEnable();
        createChannel();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
                }
            }
            if (SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager();
            }
        }
    }

    private void findView() {
        recording_layout =  findViewById(R.id.recording_layout);
        radioGroup = findViewById(R.id.home_radio_group);
        recording_switchCompat = findViewById(R.id.recording_switchButton);

        google = findViewById(R.id.google);
        dropBox = findViewById(R.id.drop_box);
        oneDrive = findViewById(R.id.oneDrive);
        local = findViewById(R.id.local);
        relative_permission = findViewById(R.id.relative_permission);
        allow_permission = findViewById(R.id.allow_permission);

        if(Preferences.getServiceInfo(MainActivity.this,"service")){
            recording_layout.setBackgroundResource(R.drawable.orange_card);
            TextView switchLabel = (TextView) findViewById(R.id.recoroding_label);
            switchLabel.setText("Recording on");
        }
    }

    public void selectRadioButton() {
        LinearLayout googledrive_layout = (LinearLayout) findViewById(R.id.googledrive_layout);
        LinearLayout dropbox_layout = (LinearLayout) findViewById(R.id.dropbox_layout);
        LinearLayout onedrive_layout = (LinearLayout) findViewById(R.id.onedrive_layout);
        LinearLayout localstorage_layout = (LinearLayout) findViewById(R.id.localstorage_layout);

        RelativeLayout relativeGoogle = findViewById(R.id.relative_google);
        RelativeLayout relativeDrop = findViewById(R.id.relative_dropBox);
        RelativeLayout relativeOne = findViewById(R.id.relative_oneDrive);
        RelativeLayout relativeLocal = findViewById(R.id.relative_local);

//        relativeGoogle.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                google.setChecked(true);
//                dropBox.setChecked(false);
//                oneDrive.setChecked(false);
//                local.setChecked(false);
//                googledrive_layout.setBackgroundResource(R.drawable.active_card);
//                dropbox_layout.setBackgroundResource(R.drawable.inactive_card);
//                onedrive_layout.setBackgroundResource(R.drawable.inactive_card);
//                localstorage_layout.setBackgroundResource(R.drawable.inactive_card);
//
//                Preferences.setRadioIndex(getApplicationContext(),"radioIndex",0);
//                setChecked();
//
//            }
//        });
//
//        relativeDrop.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                google.setChecked(false);
//                dropBox.setChecked(true);
//                oneDrive.setChecked(false);
//                local.setChecked(false);
//
//                dropbox_layout.setBackgroundResource(R.drawable.active_card);
//                googledrive_layout.setBackgroundResource(R.drawable.inactive_card);
//                onedrive_layout.setBackgroundResource(R.drawable.inactive_card);
//                localstorage_layout.setBackgroundResource(R.drawable.inactive_card);
//                Preferences.setRadioIndex(getApplicationContext(),"radioIndex",1);
//                setChecked();
//
//            }
//        });
//
//        relativeOne.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                google.setChecked(false);
//                dropBox.setChecked(false);
//                oneDrive.setChecked(true);
//                local.setChecked(false);
//
//                onedrive_layout.setBackgroundResource(R.drawable.active_card);
//                dropbox_layout.setBackgroundResource(R.drawable.inactive_card);
//                googledrive_layout.setBackgroundResource(R.drawable.inactive_card);
//                localstorage_layout.setBackgroundResource(R.drawable.inactive_card);
//                Preferences.setRadioIndex(getApplicationContext(),"radioIndex",2);
//                setChecked();
//
//            }
//        });

        relativeLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                google.setChecked(false);
                dropBox.setChecked(false);
                oneDrive.setChecked(false);
                local.setChecked(true);

                localstorage_layout.setBackgroundResource(R.drawable.active_card);
                dropbox_layout.setBackgroundResource(R.drawable.inactive_card);
                onedrive_layout.setBackgroundResource(R.drawable.inactive_card);
                googledrive_layout.setBackgroundResource(R.drawable.inactive_card);
                Preferences.setRadioIndex(getApplicationContext(),"radioIndex",3);
                setChecked();

            }
        });
    }

    private void setChecked() {

        LinearLayout googledrive_layout = (LinearLayout) findViewById(R.id.googledrive_layout);
        LinearLayout dropbox_layout = (LinearLayout) findViewById(R.id.dropbox_layout);
        LinearLayout onedrive_layout = (LinearLayout) findViewById(R.id.onedrive_layout);
        LinearLayout localstorage_layout = (LinearLayout) findViewById(R.id.localstorage_layout);

        int radioIndex = Preferences.getRadioIndex(MainActivity.this,"radioIndex");
        switch (radioIndex){
            case 0:
                google.setChecked(true);
                dropBox.setChecked(false);
                oneDrive.setChecked(false);
                local.setChecked(false);

                googledrive_layout.setBackgroundResource(R.drawable.active_card);
                dropbox_layout.setBackgroundResource(R.drawable.inactive_card);
                onedrive_layout.setBackgroundResource(R.drawable.inactive_card);
                localstorage_layout.setBackgroundResource(R.drawable.inactive_card);
                break;

            case 1:
                google.setChecked(false);
                dropBox.setChecked(true);
                oneDrive.setChecked(false);
                local.setChecked(false);

                dropbox_layout.setBackgroundResource(R.drawable.active_card);
                googledrive_layout.setBackgroundResource(R.drawable.inactive_card);
                onedrive_layout.setBackgroundResource(R.drawable.inactive_card);
                localstorage_layout.setBackgroundResource(R.drawable.inactive_card);

                break;
            case 2:
                google.setChecked(false);
                dropBox.setChecked(false);
                oneDrive.setChecked(true);
                local.setChecked(false);

                onedrive_layout.setBackgroundResource(R.drawable.active_card);
                dropbox_layout.setBackgroundResource(R.drawable.inactive_card);
                googledrive_layout.setBackgroundResource(R.drawable.inactive_card);
                localstorage_layout.setBackgroundResource(R.drawable.inactive_card);
                break;

            case 3:
                google.setChecked(false);
                dropBox.setChecked(false);
                oneDrive.setChecked(false);
                local.setChecked(true);

                localstorage_layout.setBackgroundResource(R.drawable.active_card);
                dropbox_layout.setBackgroundResource(R.drawable.inactive_card);
                onedrive_layout.setBackgroundResource(R.drawable.inactive_card);
                googledrive_layout.setBackgroundResource(R.drawable.inactive_card);
                break;

            default:
                Log.e("nothing selected","?");
                break;
        }
    }

    private void startService() {
        Intent intent = new Intent(MainActivity.this, PhoneStateReceiver.class);
        startService(intent);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void stopService() {
        Intent intent = new Intent(MainActivity.this,PhoneStateReceiver.class);
        phoneStateReceiver.stopForegroundService();
        stopService(intent);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(Constant.CHANNEL_ID,
                    "KOD Dev", NotificationManager.IMPORTANCE_HIGH);

            notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null){
                notificationManager.createNotificationChannel(channel);
            }
        }
    }



    public void accessibitiy(View view) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.recording_switchButton:
                recordingEnable();
                break;

        }
    }


    private void recordingEnable() {
        if (Preferences.getServiceInfo(MainActivity.this,"service")){
            startService();
        }
        SharedPreferences sharedPreferences = getSharedPreferences("save", MODE_PRIVATE);
        recording_switchCompat.setChecked(sharedPreferences.getBoolean("value", false));
        recording_switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked){
                    SharedPreferences.Editor editor = getSharedPreferences("save", MODE_PRIVATE).edit();
                    editor.putBoolean("value", true);
                    editor.apply();
                    Preferences.setServiceStart(MainActivity.this,"service",true);
                    Toast.makeText(MainActivity.this, "Call recording on", Toast.LENGTH_SHORT).show();
                    recording_layout.setBackgroundResource(R.drawable.orange_card);
                    TextView switchLabel = (TextView) findViewById(R.id.recoroding_label);
                    switchLabel.setText("Recording on");
                    startService();
                } else {
                    SharedPreferences.Editor editor = getSharedPreferences("save", MODE_PRIVATE).edit();
                    editor.putBoolean("value", false);
                    recording_switchCompat.setChecked(false);
                    Preferences.setServiceStart(MainActivity.this,"service",false);
                    Toast.makeText(MainActivity.this, "Call recording off", Toast.LENGTH_SHORT).show();
                    editor.apply();
                    if (SDK_INT >= Build.VERSION_CODES.O) {
                        stopService();
                    }
                    recording_layout.setBackgroundResource(R.drawable.inactive_orange_card);
                    TextView switchLabel = (TextView) findViewById(R.id.recoroding_label);
                    switchLabel.setText("Recording off");
                }

            }
        });
    }


}
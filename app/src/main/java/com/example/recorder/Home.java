package com.example.recorder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.dropbox.core.v1.DbxEntry;
import com.example.recorder.drop.DropboxClient;
import com.example.recorder.drop.URI_to_Path;
import com.example.recorder.drop.UploadTask;
import com.example.recorder.fragment.AudioRecording;
import com.example.recorder.fragment.DriveServiceHelper;
import com.example.recorder.fragment.Setting;
import com.example.recorder.fragment.SharedStorage;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static com.example.recorder.drop.URI_to_Path.getPath;
import static com.google.api.client.googleapis.testing.auth.oauth2.MockGoogleCredential.ACCESS_TOKEN;


public class Home extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    SwitchCompat switchCompat;

    Button start, stop;

    MediaRecorder recorder;
    private static String fileName = null;
    static final String TAG = "MediaRecording";
    //audio
    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};


    //drive service helper
    DriveServiceHelper driveServiceHelper;


//    drop
    private String ACCESS_TOKEN;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        switchCompat = findViewById(R.id.switchButton);
        start = findViewById(R.id.startRecording);
        stop = findViewById(R.id.stopRecording);

        recorder = new MediaRecorder();
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                start.setVisibility(View.INVISIBLE);
                try {
                    String Path = "/sdcard/records/";
                    String fileName;
//                    fileName = getExternalCacheDir().getAbsolutePath();
//                    fileName += "/audiorecordtest.3gp";
                    fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                            + UUID.randomUUID().toString() + "audio.3gp";

                    recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    java.io.File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//                    File file = new File(path,fileName);
                    String file = path + "/record.mp3";
                    recorder.setOutputFile(file);
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                    uploadAudioInDrive(file);
                    //drop box
                    storeInDropBox(file);
                    recorder.prepare();
                    recorder.start();

                    switch (SharedStorage.getPrefsIndex("radioIndex",getApplicationContext())){
                        case 0:
                            uploadAudioInDrive(fileName);
                            break;

                        case 1:
                            storeInDropBox(fileName);
                            break;
                        case 2:
                            recorder.setOutputFile(path.getAbsolutePath() + "/rec.mp3");
                            break;
                        default:
                            break;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.stop();
                start.setVisibility(View.VISIBLE);
            }
        });

        setSupportActionBar(toolbar);

        navigationView.bringToFront();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);

        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_first_fragment);

        recorder = new MediaRecorder();

//        if (switchCompat.isChecked() && RADIO_GROUP.booleanValue() == true) {
//            startRecording();
//        } else {
//            stopRecording();
//        }

//shared preference
        SharedPreferences sharedPreferences = getSharedPreferences("save", MODE_PRIVATE);
        switchCompat.setChecked(sharedPreferences.getBoolean("value", false));

        switchCompat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchCompat.isChecked()) {
                    SharedPreferences.Editor editor = getSharedPreferences("save", MODE_PRIVATE).edit();
                    editor.putBoolean("value", true);
                    editor.apply();
                    checkedRadioButton();


                } else {
                    //unchecked
                    SharedPreferences.Editor editor = getSharedPreferences("save", MODE_PRIVATE).edit();
                    editor.putBoolean("value", false);
                    switchCompat.setChecked(false);
                    editor.apply();

                }

            }
        });


    }


    //upload data in drive
    public void uploadAudioInDrive(String fileName){
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getApplicationContext());

//        String filePath = "filepath to get audio";
       try {
            driveServiceHelper.createAudioFile(fileName).addOnSuccessListener(new OnSuccessListener<String>() {
                @Override
                public void onSuccess(String s) {

                    Toast.makeText(Home.this, "Upload Successful", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(Home.this, "Error to upload!", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //drop box
    private String retrieveAccessToken() {
        //check if ACCESS_TOKEN is previously stored on previous app launches
        SharedPreferences prefs = getSharedPreferences("dropAccessToken", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("access-token", null);
        if (accessToken == null) {
            Log.d("AccessToken Status", "No token found");
            return null;
        } else {
            //accessToken already exists
            Log.d("AccessToken Status", "Token exists");
            return accessToken;
        }
    }


    private void storeInDropBox(String file) {
        ACCESS_TOKEN = retrieveAccessToken();
        if (ACCESS_TOKEN == null)return;
        new UploadTask(DropboxClient.getClient(ACCESS_TOKEN), new File(file),getApplicationContext()).execute();
    }




//    private void stopRecording() {
//        Intent stop = new Intent(getApplicationContext(), AudioRecording.class);
//        Toast.makeText(this, "stop Recording", Toast.LENGTH_SHORT).show();
//
//        stopService(stop);
//    }
//
//    private void startRecording() {
//        Intent start = new Intent(getApplicationContext(), AudioRecording.class);
//        Toast.makeText(this, "stop Recording", Toast.LENGTH_SHORT).show();
//        startService(start);
//    }


    private void checkedRadioButton() {
        Boolean radioButton = SharedStorage.getRadioButton("radioButton",getApplicationContext());

        if (radioButton.booleanValue() == false){
            Intent i  = new Intent(this,Setting.class);
            startActivity(i);
        }
    }


    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        super.onBackPressed();
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.nav_first_fragment:
                break;

            case R.id.nav_second_fragment:
                Intent i = new Intent(this, Setting.class);
                startActivity(i);
                break;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }


}



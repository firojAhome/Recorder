package com.example.recorder;

import androidx.annotation.NonNull;
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
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.recorder.callEvent.PhoneStateReceiver;
import com.example.recorder.drop.DropboxClient;
import com.example.recorder.drop.UploadTask;
import com.example.recorder.storage.Preferences;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.IOException;



public class Home extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    Toolbar toolbar;
    SwitchCompat switchCompat;
    private IntentFilter mIntentFilter;
    //audio
    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_EXTERNAL_STORAGE};


    //drive service helper
    public static Context contextOfApplication;


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

        //shared preference to access non activity class
        contextOfApplication = getApplicationContext();



        setSupportActionBar(toolbar);

        navigationView.bringToFront();
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);

        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_first_fragment);

        int radio = Preferences.getRadioIndex(this,"radioIndex");
        String prefToken = Preferences.getPreferences(this,"prefreToken");
        Log.e(LOG_TAG," radio Id:"+ radio);


//shared preference
        SharedPreferences sharedPreferences = getSharedPreferences("save", MODE_PRIVATE);
        switchCompat.setChecked(sharedPreferences.getBoolean("value", false));

        switchCompat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchCompat.isChecked()) {
                    SharedPreferences.Editor editor = getSharedPreferences("save", MODE_PRIVATE).edit();
                    editor.putBoolean("value", true);
                    Toast.makeText(Home.this, "Call recording Start!!!", Toast.LENGTH_SHORT).show();
                    editor.apply();

                } else {
                    //unchecked
                    SharedPreferences.Editor editor = getSharedPreferences("save", MODE_PRIVATE).edit();
                    editor.putBoolean("value", false);
                    switchCompat.setChecked(false);
                    Toast.makeText(Home.this, "Call recording off!!!", Toast.LENGTH_SHORT).show();
                    editor.apply();
                    stopService();

                }

            }
        });

        if(switchCompat.isChecked()){
            startService();
        }

    }

    public static Context getContextOfApplication(){
        return contextOfApplication;
    }




    public void storeInDropBox(String absolutePath, String prefToken) {

        Log.e(LOG_TAG,"Phone_recever_Token"+prefToken);

        if (prefToken == null){
            return;
        }
        if (absolutePath != null){
            new UploadTask(DropboxClient.getClient(prefToken), new File(absolutePath),Home.this).execute();
            Log.e(LOG_TAG,"file");
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

//    @Override
//    protected void onResume() {
//        unregisterReceiver(testReceiver);
//        super.onResume();
//    }

//
//    private BroadcastReceiver testReceiver = new BroadcastReceiver() {
//        @RequiresApi(api = Build.VERSION_CODES.O)
//        @Override
//        public void onReceive(Context context, Intent intent) {
////            int resultCode = intent.getIntExtra("resultCode", RESULT_CANCELED);
//            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
//           if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)){
//               //call idle
//               Log.v("timer", "i value = "+state);
//               startService();
//           }if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)){
//                Log.v("timer", "i value = "+state);
//                startService();
//            }if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)){
//                Log.v("timer", "i value = "+state);
//                startService();
//            }
//        }
//    };


    public void startService(){
        Intent intent = new Intent(this,PhoneStateReceiver.class);
        startService(intent);
    }
    public void stopService(){
        Intent intent = new Intent(this, PhoneStateReceiver.class);
        stopService(intent);
    }
}



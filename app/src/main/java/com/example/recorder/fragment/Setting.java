package com.example.recorder.fragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.recorder.Home;
import com.example.recorder.R;
import com.example.recorder.drop.DropBoxLogin;
import com.example.recorder.storage.Variable;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;

public class Setting extends AppCompatActivity {

    private static final int REQUEST_CODE_SIGN_IN = 1;
    public static String TAG = "Setting";
    public String MyPreference = "Radio_Button";
    RadioGroup radioGroup;
    //    RadioButton google,drop,local;
    Toolbar toolbar;
    Button button;
    LinearLayout linearLayout;

    DriveServiceHelper driveServiceHelper;
    //drop box

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);


        radioGroup = findViewById(R.id.radio_group);
        toolbar = findViewById(R.id.toolbar_setting);
        button = findViewById(R.id.submit);
        linearLayout = findViewById(R.id.linear_setting);


        Update();



        setSupportActionBar(toolbar);
        // add back arrow to toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }


        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = group.findViewById(checkedId);
                RadioButton checkIndex = group.findViewById(checkedId);
                int radioIndex = group.indexOfChild(checkIndex);
                Variable.sharedPreferences = getSharedPreferences(Variable.pref_name,Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = Variable.sharedPreferences.edit();
                editor.putInt(String.valueOf(Variable.index_ID), radioIndex);
                editor.commit();
                SharedStorage.setRadioIndex("radioIndex",radioIndex,getApplicationContext());
                SaveInSharedPreference(MyPreference, radioIndex);
                CheckedPermission();
                
            }

        });


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), Home.class);
                startActivity(i);
                finish();
            }
        });


    }

    private void CheckedPermission() {
        switch (radioGroup.getCheckedRadioButtonId()) {
            case R.id.google:
                GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                if(acct != null){
                    Toast.makeText(this, "Google", Toast.LENGTH_SHORT).show();
                }
                requestSignIn();


                break;

            case R.id.drop_box:
                if (!tokenExists()) {
                    Intent dropBoxLogin = new Intent(this, DropBoxLogin.class);
                    startActivity(dropBoxLogin);
                }else {
                    Intent dropIntent = new Intent(this,Home.class);
                    startActivity(dropIntent);
                }
                Toast.makeText(this, "drop", Toast.LENGTH_SHORT).show();
                break;

            case R.id.local:
                Toast.makeText(this, "local", Toast.LENGTH_SHORT).show();
                break;

            default:
                Toast.makeText(this, "Please Select any option", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void requestSignIn() {

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(getApplicationContext(), signInOptions);

        // The result of the sign-in Intent is handled in onActivityResult.
        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    Toast.makeText(this, "Google drive signIn successful", Toast.LENGTH_SHORT).show();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    handleSignInResult(task);
                }

                break;
        }
    }

        private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(this, Collections.singleton((DriveScopes.DRIVE)));
            credential.setSelectedAccountName(account.getDisplayName());

            Drive googleDriveService =
                    new Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName("Recorder")
                            .build();

            Intent i = new Intent(this,Home.class);
            startActivity(i);
            finish();

        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());

        }

    }


    private void SaveInSharedPreference(String key, int value) {
        SharedPreferences sharedPreferences = getSharedPreferences(MyPreference, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private void Update() {
        SharedPreferences sharedPreferences = getSharedPreferences(MyPreference, MODE_PRIVATE);
        int savedRadio = sharedPreferences.getInt(MyPreference, 2);
        RadioButton savedRadioButton = (RadioButton) radioGroup.getChildAt(savedRadio);
        savedRadioButton.setChecked(true);
        return;
    }

    //drop box access token
    private boolean tokenExists() {
        Variable.sharedPreferences = getSharedPreferences(Variable.pref_name,Context.MODE_PRIVATE);
        String accessToken = Variable.sharedPreferences.getString(Variable.Drop_Access_Token,null);
        return accessToken != null;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


}
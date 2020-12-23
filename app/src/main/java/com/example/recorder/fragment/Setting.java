package com.example.recorder.fragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;

public class Setting extends AppCompatActivity {

    public static String TAG = "Setting";
    public String MyPreference = "Radio_Button";
    RadioGroup radioGroup;
    //    RadioButton google,drop,local;
    Toolbar toolbar;
    Button button,dropButton;
    SignInButton signInButton;
    GoogleSignInClient mGoogleSignInClient;
    int RC_SIGN_IN = 0;
    LinearLayout linearLayout;

    DriveServiceHelper driveServiceHelper;
//    private DriveClient mDriveClient;
//    private DriveResourceClient mDriveResourceClient;
    //drop box
    private static final int IMAGE_REQUEST_CODE = 101;
    private String ACCESS_TOKEN;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        radioGroup = findViewById(R.id.radio_group);
        toolbar = findViewById(R.id.toolbar_setting);
        button = findViewById(R.id.submit);
        signInButton = findViewById(R.id.sign_in_button);
        linearLayout = findViewById(R.id.linear_setting);
        dropButton = findViewById(R.id.drop_sign_in_button);

        //google
//        //check signIn or not
//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestEmail()
//                .requestScopes(new Scope(DriveScopes.DRIVE))
//                .build();
//
//        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
//        startActivityForResult(mGoogleSignInClient.getSignInIntent(), 400);

        Update();

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestSignIn();
            }

        });


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
                SharedStorage.radioGroupIsSelected("radioButton",true,getApplicationContext());
                SharedStorage.setRadioIndex("radioIndex",radioIndex,getApplicationContext());
                SaveInSharedPreference(MyPreference, radioIndex);
                CheckedPermission();
            }

        });


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckedPermission();
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
                if (acct != null) {
                    String personName = acct.getDisplayName();
                    String personGivenName = acct.getGivenName();
                    String personFamilyName = acct.getFamilyName();
                    String personEmail = acct.getEmail();
                    String personId = acct.getId();
                    Uri personPhoto = acct.getPhotoUrl();

                    Toast.makeText(this, "User logIn"+personName, Toast.LENGTH_SHORT).show();
                }else {
                    signInButton.setVisibility(View.VISIBLE);
                    linearLayout.setVisibility(View.INVISIBLE);
                }

                Toast.makeText(this, "Google", Toast.LENGTH_SHORT).show();

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


    private void SaveInSharedPreference(String key, int value) {
        SharedPreferences sharedPreferences = getSharedPreferences(MyPreference, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private void Update() {
        SharedPreferences sharedPreferences = getSharedPreferences(MyPreference, MODE_PRIVATE);
        int savedRadio = sharedPreferences.getInt(MyPreference, 0);
        RadioButton savedRadioButton = (RadioButton) radioGroup.getChildAt(savedRadio);
        savedRadioButton.setChecked(true);
        return;
    }

    //drop box access token
    private boolean tokenExists() {
        SharedPreferences prefs = getSharedPreferences("dropAccessToken", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("access-token", null);
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


    //drive permission
    private void requestSignIn() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(Drive.SCOPE_FILE)
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);

        startActivityForResult(client.getSignInIntent(), 400);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case 400:
                if (requestCode == RESULT_OK){
                    handleSignInResult(data);
                }
                break;
        }
    }
//
    private void handleSignInResult(Intent data) {

        GoogleSignIn.getSignedInAccountFromIntent(data).addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
            @Override
            public void onSuccess(GoogleSignInAccount googleSignInAccount) {
//                GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(Setting.this, Collections.singleton(DriveScopes.DRIVE_FILE));
//                credential.setSelectedAccount(googleSignInAccount.getAccount());

                Toast.makeText(Setting.this, "Successful", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(Setting.this,Home.class);
                startActivity(i);
//                Drive googleDriveService = new Drive.Builder(AndroidHttp.newCompatibleTransport(),
//                        new GsonFactory(),
//                        credential)
//                        .setApplicationName("Recorder")
//                        .build();
//
//                // pass data to drive
//                driveServiceHelper = new DriveServiceHelper(googleDriveService);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(Setting.this, "Failed SignIn", Toast.LENGTH_SHORT).show();
            }
        });
    }



//    if user not sign In onclick method call

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
//        if (requestCode == RC_SIGN_IN) {
//            // The Task returned from this call is always completed, no need to attach
//            // a listener.
//            Log.i(TAG, "Signed in successfully.");
//            // Use the last signed in account here since it already have a Drive scope.
//
//            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//            handleSignInResult(task);
//        }
//    }
//
//    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
//        try {
//            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
//
//            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(this, Collections.singleton((DriveScopes.DRIVE)));
//            credential.setSelectedAccountName(account.getDisplayName());
//            Drive service = new Drive.Builder(AndroidHttp.newCompatibleTransport(),new GsonFactory(),credential).build();
//            // Signed in successfully, show authenticated UI.
//            Intent googleSign = new Intent(this,Home.class);
//            //startActivity(googleSign);
////            updateUI(account);
//        } catch (ApiException e) {
//            // The ApiException status code indicates the detailed failure reason.
//            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
//
//        }
//    }

}
package com.ahom.callrecorder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import android.Manifest;
import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.dropbox.core.android.Auth;
import com.ahom.callrecorder.callEvent.PhoneStateReceiver;
import com.ahom.callrecorder.drop.DropboxClient;
import com.ahom.callrecorder.drop.UploadTask;
import com.ahom.callrecorder.google.GoogleDriveService;
import com.ahom.callrecorder.storage.Preferences;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.people.v1.model.Person;
import com.google.gson.JsonObject;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.os.Build.VERSION.SDK_INT;
import static com.ahom.callrecorder.storage.Constant.Call_Records;

public class RecordsHome extends AppCompatActivity {

    RadioGroup radioGroup;
    SwitchCompat switchCompat;
    Toolbar toolbar;
    LinearLayout recording_layout;
    RelativeLayout relative_permission;
    TextView allow_permission;
    //radio button
    RadioButton google;
    RadioButton dropBox;
    RadioButton oneDrive;
    RadioButton local;
    private static final String TAG = "RecorderHome";

    //google singIn
    private static final int RC_SIGN_IN = 1;
    GoogleSignInClient mGoogleSignInClient;
    GoogleSignInOptions gso;
    GoogleDriveService mGoogleDriveService;
    Scope driveSCOPE = new Scope(Scopes.DRIVE_FILE);

    //oneDrive
    private final static String[] SCOPES = {"Files.ReadWrite.All"};
    final static String AUTHORITY = "https://login.microsoftonline.com/common";
    public ISingleAccountPublicClientApplication mSingleAccountApp;
    String token = null;
    ProgressBar progressBar;
    public boolean DROPBOX_SIGNIN = false;
    boolean dropbox_toast = false;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;

    private String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_CONTACTS,Manifest.permission.READ_CALL_LOG};

    //drive service helper
    public static Context contextOfApplication;
    String serverClientId = "497527836670-dop0atulau30hc9527um519i07s4d864.apps.googleusercontent.com";

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                if (SDK_INT >= Build.VERSION_CODES.Q) {
                    Environment.isExternalStorageManager();
                    return;
                }
                permissionToRecordAccepted = grantResults[5] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();
        checkSwitchCompact();
        Log.e("show ","permission status");

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_home);


        mSingAccountClint(this);

        Log.e("print one driv",""+mSingleAccountApp);
        contextOfApplication = getApplicationContext();
        //permission
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.DRIVE_FILE))
                .requestServerAuthCode(serverClientId)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        hooks();
        checkSwitchCompact();
        selectRadioButton();

        allow_permission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionAllow();
            }
        });

        recording_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkPermissionAllow()){
                    Toast.makeText(RecordsHome.this, "Recording can not start until you not allow the permission", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean checkPermissionAllow() {
        if((ContextCompat.checkSelfPermission(RecordsHome.this, Manifest.permission.READ_PHONE_STATE)  == PermissionChecker.PERMISSION_GRANTED) &&
                (PermissionChecker.checkSelfPermission(RecordsHome.this, Manifest.permission.RECORD_AUDIO)  == PermissionChecker.PERMISSION_GRANTED) &&
                (PermissionChecker.checkSelfPermission(RecordsHome.this, Manifest.permission.READ_CALL_LOG)  == PermissionChecker.PERMISSION_GRANTED) &&
                (PermissionChecker.checkSelfPermission(RecordsHome.this, Manifest.permission.READ_CONTACTS)  == PermissionChecker.PERMISSION_GRANTED) &&
                (PermissionChecker.checkSelfPermission(RecordsHome.this, Manifest.permission.READ_EXTERNAL_STORAGE)  == PermissionChecker.PERMISSION_GRANTED) &&
                (PermissionChecker.checkSelfPermission(RecordsHome.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)  == PermissionChecker.PERMISSION_GRANTED)){

            checkSwitchCompact();
            switchCompat.setClickable(true);
            relative_permission.setVisibility(View.GONE);

            return true;
        } else {
            if (SDK_INT >= Build.VERSION_CODES.M) {
                switchCompat.setClickable(false);
                relative_permission.setVisibility(View.VISIBLE);

                if ((ActivityCompat.shouldShowRequestPermissionRationale(RecordsHome.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)) &&
                        ActivityCompat.shouldShowRequestPermissionRationale(RecordsHome.this,Manifest.permission.READ_EXTERNAL_STORAGE) &&
                        ActivityCompat.shouldShowRequestPermissionRationale(RecordsHome.this,Manifest.permission.READ_CONTACTS) &&
                        ActivityCompat.shouldShowRequestPermissionRationale(RecordsHome.this,Manifest.permission.READ_CALL_LOG) &&
                        ActivityCompat.shouldShowRequestPermissionRationale(RecordsHome.this,Manifest.permission.RECORD_AUDIO)){
                    Log.e("shcekljalfj","check status");
                    requestPermissions(permissions,REQUEST_RECORD_AUDIO_PERMISSION);
                }else {
                    displayNeverAskAgainDialog();
                }
            }
            Log.e("shwo 2","check status");
            return false;
        }

    }


    private void displayNeverAskAgainDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(RecordsHome.this);
        builder.setMessage("We need to allowed all permission for performing necessary task. Please permit the permission through "
                + "Settings screen.\n\nSelect Permissions -> Enable permission");
        builder.setCancelable(false);
        builder.setPositiveButton("Permit Manually", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    public static Context getContextOfApplication(){
        return contextOfApplication;
    }

    private void hooks() {
        recording_layout =  findViewById(R.id.recording_layout);
        radioGroup = findViewById(R.id.home_radio_group);
        switchCompat = findViewById(R.id.switchButton);
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progress_bar);

        google = findViewById(R.id.google);
        dropBox = findViewById(R.id.drop_box);
        oneDrive = findViewById(R.id.oneDrive);
        local = findViewById(R.id.local);
        relative_permission = findViewById(R.id.relative_permission);
        allow_permission = findViewById(R.id.allow_permission);

    }



    @Override
    protected void onResume() {
        super.onResume();
        if (DROPBOX_SIGNIN){
            getAccessToken();
            Log.e("get drop box","access token");
        }
    }


    public void getAccessToken() {
        String accessToken = Auth.getOAuth2Token(); //generate Access Token
        if (accessToken != null) {
            //Store accessToken in SharedPreferences
            Preferences.setDropBoxAccessToken(RecordsHome.this,"Drop_Box_Access_Token",accessToken);
            //Proceed to MainActivity
            Preferences.setRadioIndex(getApplicationContext(),"radioIndex",1);
            setChecked();
            if (dropbox_toast){
                Toast.makeText(RecordsHome.this, "DropBox log in successfully", Toast.LENGTH_SHORT).show();
                dropbox_toast = false;
            }
            progressBar.setVisibility(View.GONE);
        }
    }


    private void checkSwitchCompact() {

        if(!((ContextCompat.checkSelfPermission(RecordsHome.this, Manifest.permission.READ_PHONE_STATE)  == PermissionChecker.PERMISSION_GRANTED) &&
                (PermissionChecker.checkSelfPermission(RecordsHome.this, Manifest.permission.RECORD_AUDIO)  == PermissionChecker.PERMISSION_GRANTED) &&
                (PermissionChecker.checkSelfPermission(RecordsHome.this, Manifest.permission.READ_CALL_LOG)  == PermissionChecker.PERMISSION_GRANTED) &&
                (PermissionChecker.checkSelfPermission(RecordsHome.this, Manifest.permission.READ_CONTACTS)  == PermissionChecker.PERMISSION_GRANTED) &&
                (PermissionChecker.checkSelfPermission(RecordsHome.this, Manifest.permission.READ_EXTERNAL_STORAGE)  == PermissionChecker.PERMISSION_GRANTED) &&
                (PermissionChecker.checkSelfPermission(RecordsHome.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)  == PermissionChecker.PERMISSION_GRANTED))){

            relative_permission.setVisibility(View.VISIBLE);
            switchCompat.setClickable(false);
            Log.e("chec ","switch comppact");
            return;
        }

        Log.e("chec ","switch comppact1");
        SharedPreferences sharedPreferences = getSharedPreferences("save", MODE_PRIVATE);
        switchCompat.setChecked(sharedPreferences.getBoolean("value", true));
        switchCompat.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                if (switchCompat.isChecked()) {
                    SharedPreferences.Editor editor = getSharedPreferences("save", MODE_PRIVATE).edit();
                    Preferences.setServiceStart(RecordsHome.this,"service",true);
                    editor.putBoolean("value", true);
                    startService();
                    editor.apply();

                    Log.e("chec ","switch comppact2 ");
                    recording_layout.setBackgroundResource(R.drawable.orange_card);
                    TextView switchLabel = (TextView) findViewById(R.id.recoroding_label);
                    switchLabel.setText("Recording on");
                } else {
                    //unchecked
                    SharedPreferences.Editor editor = getSharedPreferences("save", MODE_PRIVATE).edit();
                    editor.putBoolean("value", false);
                    switchCompat.setChecked(false);
                    Preferences.setServiceStart(RecordsHome.this,"service",false);
                    Toast.makeText(RecordsHome.this, "Call recording off", Toast.LENGTH_SHORT).show();
                    editor.apply();
                    stopService();

                    recording_layout.setBackgroundResource(R.drawable.inactive_orange_card);
                    TextView switchLabel = (TextView) findViewById(R.id.recoroding_label);
                    switchLabel.setText("Recording off");
                }
            }
        });

        if(switchCompat.isChecked()){
            Preferences.setServiceStart(RecordsHome.this,"service",true);
            startService();
            recording_layout.setBackgroundResource(R.drawable.orange_card);
            TextView switchLabel = (TextView) findViewById(R.id.recoroding_label);
            switchLabel.setText("Recording on");
            progressBar.setVisibility(View.GONE);
        }else{
            recording_layout.setBackgroundResource(R.drawable.inactive_orange_card);
            TextView switchLabel = (TextView) findViewById(R.id.recoroding_label);
            switchLabel.setText("Recording off");
            progressBar.setVisibility(View.GONE);
        }
    }


    private void startService() {

        Intent intent = new Intent(RecordsHome.this, PhoneStateReceiver.class);
        startService(intent);
        relative_permission.setVisibility(View.GONE);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void stopService() {
        Intent intent = new Intent(RecordsHome.this,PhoneStateReceiver.class);
        PhoneStateReceiver phoneStateReceiver = new PhoneStateReceiver();
        phoneStateReceiver.stopForegroundService();
        stopService(intent);
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

        relativeGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                google.setChecked(true);
                dropBox.setChecked(false);
                oneDrive.setChecked(false);
                local.setChecked(false);
                googledrive_layout.setBackgroundResource(R.drawable.active_card);
                dropbox_layout.setBackgroundResource(R.drawable.inactive_card);
                onedrive_layout.setBackgroundResource(R.drawable.inactive_card);
                localstorage_layout.setBackgroundResource(R.drawable.inactive_card);

                Preferences.setRadioIndex(getApplicationContext(),"radioIndex",0);
                CheckedPermission();
                progressBar.setVisibility(View.GONE);
                if (!Preferences.getDriveButton(getApplicationContext(),"Is_Clicked")){
                    Preferences.setRadioIndex(getApplicationContext(),"radioIndex",3);
                    setChecked();
                }
            }
        });

        relativeDrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                google.setChecked(false);
                dropBox.setChecked(true);
                oneDrive.setChecked(false);
                local.setChecked(false);

                dropbox_layout.setBackgroundResource(R.drawable.active_card);
                googledrive_layout.setBackgroundResource(R.drawable.inactive_card);
                onedrive_layout.setBackgroundResource(R.drawable.inactive_card);
                localstorage_layout.setBackgroundResource(R.drawable.inactive_card);

                Preferences.setRadioIndex(getApplicationContext(),"radioIndex",1);
                CheckedPermission();
                progressBar.setVisibility(View.GONE);

                if (!tokenExists()){
                    Preferences.setRadioIndex(getApplicationContext(),"radioIndex",3);
                    setChecked();

                }
            }
        });

        relativeOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                google.setChecked(false);
                dropBox.setChecked(false);
                oneDrive.setChecked(true);
                local.setChecked(false);

                onedrive_layout.setBackgroundResource(R.drawable.active_card);
                dropbox_layout.setBackgroundResource(R.drawable.inactive_card);
                googledrive_layout.setBackgroundResource(R.drawable.inactive_card);
                localstorage_layout.setBackgroundResource(R.drawable.inactive_card);

                Preferences.setRadioIndex(getApplicationContext(),"radioIndex",2);
                CheckedPermission();
                progressBar.setVisibility(View.GONE);

                if (!Preferences.isOneDriveLogin(getApplicationContext(),"Is_One_DriveLogIn")){
                    Preferences.setRadioIndex(getApplicationContext(),"radioIndex",3);
                    setChecked();
                }
            }
        });

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

                progressBar.setVisibility(View.GONE);
                Preferences.setRadioIndex(getApplicationContext(),"radioIndex",3);
            }
        });

        setChecked();
    }

    private void setChecked() {

        LinearLayout googledrive_layout = (LinearLayout) findViewById(R.id.googledrive_layout);
        LinearLayout dropbox_layout = (LinearLayout) findViewById(R.id.dropbox_layout);
        LinearLayout onedrive_layout = (LinearLayout) findViewById(R.id.onedrive_layout);
        LinearLayout localstorage_layout = (LinearLayout) findViewById(R.id.localstorage_layout);

        int radioIndex = Preferences.getRadioIndex(RecordsHome.this,"radioIndex");
        Log.e("check id",""+radioIndex);
        switch (radioIndex){
            case 0:
                Log.e("switch ","log"+radioIndex);
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
                Log.e("switch ","log"+radioIndex);

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
                Log.e("switch ","log"+radioIndex);

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
                Log.e("switch ","log"+radioIndex);

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
        }
    }


    private void CheckedPermission() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(RecordsHome.this,R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_bottom_sheet,findViewById(R.id.bottom_sheet));

        int drive = Preferences.getRadioIndex(RecordsHome.this,"radioIndex");
        Log.e("check radio","button permission");
        switch (drive) {
            case 0:
                if (!Preferences.getDriveButton(RecordsHome.this,"Is_Clicked")){
                    view.findViewById(R.id.dropLinear).setVisibility(View.INVISIBLE);
                    view.findViewById(R.id.google_sign_in_button).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.oneLinear).setVisibility(View.INVISIBLE);
                    view.findViewById(R.id.google_sign_in_button).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            bottomSheetDialog.dismiss();
                            googleSignIn();
                            progressBar.setVisibility(View.VISIBLE);
                        }
                    });

                    bottomSheetDialog.setContentView(view);
                    bottomSheetDialog.show();

                }else {
                    Toast.makeText(RecordsHome.this, "Google", Toast.LENGTH_SHORT).show();
                }
                break;

            case 1:
                Log.e("check drop box token"," "+tokenExists());
                if (!tokenExists()) {
                    view.findViewById(R.id.dropLinear).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.google_sign_in_button).setVisibility(View.INVISIBLE);
                    view.findViewById(R.id.oneLinear).setVisibility(View.INVISIBLE);
                    view.findViewById(R.id.dropLinear).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dropBoxSignIn();
                            dropbox_toast = true;
                            progressBar.setVisibility(View.VISIBLE);
                            bottomSheetDialog.dismiss();
                        }
                    });

                    bottomSheetDialog.setContentView(view);
                    bottomSheetDialog.show();

                }else {
                    Toast.makeText(RecordsHome.this, "Drop box", Toast.LENGTH_SHORT).show();
                }
                break;

            case 2:
                if (!Preferences.isOneDriveLogin(RecordsHome.this,"Is_One_DriveLogIn")){

                    view.findViewById(R.id.dropLinear).setVisibility(View.INVISIBLE);
                    view.findViewById(R.id.google_sign_in_button).setVisibility(View.INVISIBLE);
                    view.findViewById(R.id.oneLinear).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.oneLinear).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            oneDriveSignIn();
                            progressBar.setVisibility(View.VISIBLE);
                            bottomSheetDialog.dismiss();
                        }
                    });

                    bottomSheetDialog.setContentView(view);
                    bottomSheetDialog.show();

                }else {
                    Toast.makeText(RecordsHome.this, "One drive", Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                Toast.makeText(RecordsHome.this, "Please Select any option", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    //drop box access token
    private void dropBoxSignIn() {
        Log.e("dropbox","click tast");
        Auth.startOAuth2Authentication(getApplicationContext(), getString(R.string.APP_KEY));
        DROPBOX_SIGNIN = true;
    }

    public void storeInDropBox(Context applicationContext, String number, String absolutePath, String prefToken) {

        Log.e("LOG_TAG","Phone_recever_Token"+prefToken);
        DROPBOX_SIGNIN = true;
        if (prefToken == null){
            return;
        }
        if (absolutePath != null){
            new UploadTask(number, DropboxClient.getClient(prefToken), new File(absolutePath),applicationContext).execute();
            Log.e("LOG_TAG","file");
        }

    }

    private boolean tokenExists() {
        String accessToken = Preferences.getDropBoxAccessToken(RecordsHome.this,"Drop_Box_Access_Token");
        return accessToken != null;
    }


    //google sign In
    private void googleSignIn() {
        Log.e("google","click toast");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
        Log.e("credential result", " " + signInIntent);
        Toast.makeText(RecordsHome.this, "Google SingIn", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.e("check ","handle signIN resulafat");
        switch (requestCode) {
            case RC_SIGN_IN:
                Log.e("print google signin","data"+data);
                Log.e("print google signin","resultCode"+resultCode);

                handleSignInResult(data);
                if (resultCode == Activity.RESULT_OK && data != null) {
                    Log.e("check ","handle signIN result");
                }
                break;
        }
    }


    public void handleSignInResult(Intent result) {

        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    Log.d(TAG, "Signed in as " + googleAccount.getEmail());
                    System.out.println("google token ---> " + googleAccount.getIdToken());

                    // Use the authenticated account to sign in to the Drive service.
                    GoogleAccountCredential credential =
                            GoogleAccountCredential.usingOAuth2(
                                    this, Collections.singleton(DriveScopes.DRIVE_FILE));
                    credential.setSelectedAccount(googleAccount.getAccount());
                    Drive googleDriveService =
                            new Drive.Builder(
                                    AndroidHttp.newCompatibleTransport(),
                                    new GsonFactory(),
                                    credential)
                                    .setApplicationName("Call Recorder")
                                    .build();

                    mGoogleDriveService = new GoogleDriveService(googleDriveService);

                    Preferences.checkedDriveButton(RecordsHome.this,"Is_Clicked",true);
                    Preferences.setRadioIndex(getApplicationContext(),"radioIndex",0);
                    Log.e("login successfully","google drive");
                    setChecked();
                    query(RecordsHome.this);
                    progressBar.setVisibility(View.GONE);


                })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }

    public void query(Context context) {
        if (mGoogleDriveService != null) {
            Log.d(TAG, "Querying for files.");

            mGoogleDriveService.queryFiles()
                    .addOnSuccessListener(fileList -> {

                        ArrayList drivList = new ArrayList();
                        for (com.google.api.services.drive.model.File file : fileList.getFiles()) {

                            drivList.add(file.getName());
                            drivList.add(file.getId());
                        }

                        for (int i = 0; i < drivList.size(); i++) {
                            String arrayListName = drivList.get(i).toString();
                            Log.e("name", "arraylist name" + arrayListName);

                            switch (arrayListName) {
                                case "Call Records":
                                    Preferences.setDrviefolderId(context, "Google_Drive_Folder_Id", drivList.get(i + 1).toString());
                                    Log.e("check folder name "," "+arrayListName);

                                    Toast.makeText(context, "Google Drive login successfully", Toast.LENGTH_SHORT).show();
                                    break;
                            }

                        }
                        Log.e("builderArray", "drivList " + drivList.size());

                        if (Preferences.getDriveFolderId(context,"Google_Drive_Folder_Id") == null){
                            Log.e("google drive ","folder created");
                            mGoogleDriveService.createFolder(context,Call_Records);
                        }


                    })
                    .addOnFailureListener(exception -> Log.e(TAG, "Unable to query files.", exception));
        }
    }

//    private void createFolder() {
//        String driveFolderId = Preferences.getDriveFolderId(this, "Google_Drive_Folder_Id");
//        Log.e("driveFOlderID CHECK ", " " + driveFolderId);
//        if (mGoogleDriveService != null){
//            if (driveFolderId == null) {
//            }
//
//
//        }
//    }

    private void createSubFolder(Context context) {

        String fileDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        Date date = new Date();

        Long prefSaveDate = Preferences.getSubFolderDate(context, "Google_Drive_Date");
        Log.e("prefDate", " " + prefSaveDate);
        Log.e("prefDate", "checkDate " + date.getDate());

        if (prefSaveDate != date.getDate()) {
            Preferences.setSubFolderDate(context, "Google_Drive_Date", date);
            mGoogleDriveService.createSubFolder(context,fileDate);
            Log.e("ahofa","check"+Preferences.getSubFolderDate(context,"Google_Drive_Date"));
        }

    }

    //this authenticate during phone call
    public void startDriveStorage(Context context, String number, String absolutePath) {

        if (!GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(context),
                driveSCOPE)) {
            GoogleSignIn.requestPermissions(
                    (Activity) context,
                    RC_SIGN_IN,
                    GoogleSignIn.getLastSignedInAccount(context),
                    driveSCOPE);

        } else {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);

            GetContactsTask task = new GetContactsTask(account.getAccount(),context,number,absolutePath);
            task.execute();

        }

    }


    private class GetContactsTask extends AsyncTask<Void, Void, List<Person>> {

        private WeakReference<Context> contextRef;
        Account mAccount;
        //        Context phoneContext;
        String filePath;
        String phoneNumber;

        public GetContactsTask(Account account, Context context,String name, String absolutePath) {
            mAccount = account;
//            phoneContext = context;
            contextRef = new WeakReference<>(context);
            filePath = absolutePath;
            phoneNumber = name;
        }


        @Override
        protected List<Person> doInBackground(Void... voids) {
            List<Person> result = null;
            Context context = contextRef.get();
            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            context,
                            Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(mAccount);

            Drive googleDriveService =
                    new Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName("Recorder")
                            .build();
//                    com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential@73596e/@ec93af4

            mGoogleDriveService = new GoogleDriveService(googleDriveService);

            //query for check parent folder exist or not
            query(context);

            createSubFolder(context);

            String subRootFolderId = Preferences.getDriveSubFolderId(context, "Google_Drive_SubFolder_Id");

            Log.e("subfolder save Id",""+subRootFolderId);

            mGoogleDriveService.uploadFile(context,phoneNumber,filePath);


            return null;
        }
    }


    //One drive
    public void mSingAccountClint(Context context) {

        PublicClientApplication.createSingleAccountPublicClientApplication(context,
                R.raw.auth_config_single_account, new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        mSingleAccountApp = application;

                    }
                    @Override
                    public void onError(MsalException exception) {
                        Log.e("message","errer Exception"+exception);
                        Toast.makeText(context, "One drive login exception", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });

    }

    private void oneDriveSignIn() {
        Log.e("oneDrive","click toast");
        if (mSingleAccountApp ==null) {
            Log.e("check return","one drive");
            return;
        }
        Log.e("check sigIn","one drive");
        mSingleAccountApp.signIn(RecordsHome.this, null, SCOPES, getAuthInteractiveCallback());
    }

    public void silentOneDriveStorage (Context context,String fileName, String filePath){

        Log.e("phonestate","recever");
        mSingAccountClint(context);

        AsyncTaskRunner runner = new AsyncTaskRunner(context,fileName,filePath);
        String sleepTime = String.valueOf(1);
        runner.execute(sleepTime);


    }

    private class AsyncTaskRunner extends AsyncTask<String, String, String>{
        private String resp;
        Context asyncContext;
        String asyncFileName;
        String asyncFilePath;

        public AsyncTaskRunner(Context context, String fileName, String filePath) {
            this.asyncContext = context;
            this.asyncFileName = fileName;
            this.asyncFilePath = filePath;
        }

        @Override
        protected void onPreExecute() {
            mSingAccountClint(asyncContext);

            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {

            try {
                String[] params = new String[0];
                int time = 1*1000;
                Thread.sleep(time);
                resp = "Slept for " + params[0] + " seconds";
            } catch (InterruptedException e) {
                e.printStackTrace();
                resp = e.getMessage();
            } catch (Exception e) {
                e.printStackTrace();
                resp = e.getMessage();
            }
            mSingleAccountApp.acquireTokenSilentAsync(SCOPES, AUTHORITY, getAuthSilentCallback(asyncContext,asyncFileName,asyncFilePath));
            return resp;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.e("Success","storage");
        }

    }

    private AuthenticationCallback getAuthInteractiveCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Log.d(TAG, "Successfully authenticated");

                Preferences.setOnDriveLogin(RecordsHome.this,"Is_One_DriveLogIn",true);
                Log.d(TAG, "ID Token: " + authenticationResult.getAccount().getClaims().get("id_token"));
                Log.d(TAG, " root: " + authenticationResult.getAccount().getClaims().get("root"));
                Log.e("OneDrive tenentId"," "+authenticationResult.getTenantId());
                Log.e("OneDrive scope"," "+authenticationResult.getScope());

                Log.e("login successfully","one drive");
                callGraphAPI(authenticationResult);

            }

            @Override
            public void onError(MsalException exception) {
                Log.d(TAG, "Authentication failed: " + exception.toString());
            }
            @Override
            public void onCancel() {

                Log.d(TAG, "User cancelled login.");
            }
        };
    }

    //silent  authentication
    private SilentAuthenticationCallback getAuthSilentCallback(Context context, String fileName, String filePath) {
        return new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Log.d(TAG, "Successfully authenticated");
                token = authenticationResult.getAccessToken();
                Log.d("AUTH", String.format("Access token: %s", token));

                saveDataInOneDrive(context,fileName,filePath);
                deleteLocalFile(filePath);
            }
            @Override
            public void onError(MsalException exception) {
                Log.d(TAG, "Authentication failed: " + exception.toString());
                Log.e("silent auth"," "+exception);
            }
        };
    }


    private void callGraphAPI(IAuthenticationResult authenticationResult) {
        final String accessToken = authenticationResult.getAccessToken();
        final String auth = authenticationResult.getAuthenticationScheme();
        Log.e("accessToken"," "+accessToken);
        token = accessToken;


        IGraphServiceClient graphClient =
                GraphServiceClient
                        .builder()
                        .authenticationProvider(new IAuthenticationProvider() {
                            @Override
                            public void authenticateRequest(IHttpRequest request) {
                                Log.d(TAG, "Authenticating request," + request.getRequestUrl());
                                request.addHeader("Authorization", "Bearer " + accessToken);

                            }
                        })
                        .buildClient();

        graphClient
                .me()
                .drive()
                .buildRequest()
                .get(new ICallback<com.microsoft.graph.models.extensions.Drive>() {
                    @Override
                    public void success(final com.microsoft.graph.models.extensions.Drive drive) {
                        Log.d(TAG, "Found Drive " + drive.items);
                        Log.e("TAG", "Found Drive " + drive.id);
                        displayGraphResult(drive.getRawObject());
                    }

                    @Override
                    public void failure(ClientException ex) {
                        Log.e("call graph api failed"," "+ex);
                    }
                });

        Preferences.setRadioIndex(getApplicationContext(),"radioIndex",2);
        setChecked();
        Toast.makeText(RecordsHome.this, "One Drive log in successfully", Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.GONE);

    }


    private void displayGraphResult(JsonObject rawObject) {
        Log.e("graph result"," "+rawObject);
    }



    public void saveDataInOneDrive(Context context,String fileName, String filePath){

        String _audioBase64 = null;
        String time = new SimpleDateFormat("dd_MM_yyyy").format(new Date());
        Log.e("print time"," "+time);

        String subfileName = time+"/"+fileName;

        String URL = "https://graph.microsoft.com/v1.0/me/drive/root:/"+"CallRecords"+"/"+subfileName+":/content";
        Log.e("check upload url","url "+URL);

        File audioFile = new File(filePath);
        long fileSize = audioFile.length();

        Log.e("file size"," file"+fileSize);

        byte[] audioBytes = null;
        try {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(new File(filePath));
            byte[] buf = new byte[1024];
            int n;
            while (-1 != (n = fis.read(buf)))
                baos.write(buf, 0, n);
            audioBytes = baos.toByteArray();

//            _audioBase64 = Arrays.toString(audioBytes);
//            _audioBase64 = Base64.encodeToString(audioBytes, Base64.DEFAULT);

        } catch (Exception e) {
            Log.e("exception"," "+e);
        }
        Log.e("_audioBase64 ","audio"+_audioBase64);

        String actualArray = Arrays.toString(audioBytes);
        final String requestBody = actualArray;
        Log.e("check folder created"," "+requestBody);
        RequestQueue queue= Volley.newRequestQueue(context);

        byte[] finalAudioBytes = audioBytes;
        StringRequest request = new StringRequest(Request.Method.PUT, URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d ("PAST QUOTES SAVE", "Created file on server"+response);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        Log.e ("PAST QUOTES SAVE", "Failed to create file on server: " + error.toString());
                    }
                })
        {
            @Override
            public byte[] getBody() throws AuthFailureError {
//                try {
//                    return requestBody == null ? null : requestBody.getBytes("utf-8");
//                } catch (UnsupportedEncodingException uee) {
//                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
//                    return null;
//                }
                return finalAudioBytes;

            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put ("Authorization", "Bearer " + token);
                return headers;
            }

            @Override
            public String getBodyContentType ()
            {
                return ("text/plain");
            }
        };
        queue.add(request);

    }

    public static void deleteLocalFile(String filePath) {
        // for delete local file
        java.io.File fdelete = new java.io.File(filePath);
        if (fdelete.exists()) {
            if (SDK_INT >= Build.VERSION_CODES.Q) {
                return;
            }
            if (fdelete.delete()) {
                System.out.println("file Deleted :" + filePath);
            } else {
                System.out.println("file not Deleted :" + filePath);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(getApplicationContext(), PhoneStateReceiver.Restarter.class);
        this.sendBroadcast(broadcastIntent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        progressBar.setVisibility(View.GONE);
    }

}
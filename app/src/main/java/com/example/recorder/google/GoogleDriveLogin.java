package com.example.recorder.google;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.example.recorder.Home;
import com.example.recorder.R;
import com.example.recorder.onedrive.OneDrive;
import com.example.recorder.storage.Preferences;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.people.v1.model.Person;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;



public class GoogleDriveLogin extends AppCompatActivity{

    Toolbar toolbar;
    private static final int    RC_SIGN_IN = 1;
    private static final String TAG = "GoogleDriveLogin";
    SignInButton signInButton;
    GoogleSignInClient mGoogleSignInClient;
    GoogleSignInOptions gso;

    GoogleDriveService mGoogleDriveService;

    String serverClientId = "23602232397-2ndsrgt44jqt7dodt20gquonfqm2i4qm.apps.googleusercontent.com";

    Scope driveSCOPE = new Scope(Scopes.DRIVE_FILE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_drive_login);

        signInButton = findViewById(R.id.sign_in_button);
        toolbar = findViewById(R.id.google_drive_toolbar);

        setSupportActionBar(toolbar);
        // add back arrow to toolbar
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });



        Log.e("check","google drive login");
        //client secret  S99n52ETvRg2aYxLUsVsdPhr
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.DRIVE_FILE))
                .requestServerAuthCode(serverClientId)
                .requestEmail()
                .build();


        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
                Log.e("credential result", " " + signInIntent);
            }
        });

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case RC_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    handleSignInResult(data);

                }
                break;

        }

        super.onActivityResult(requestCode, resultCode, data);

    }

    public void handleSignInResult(Intent result) {

        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    Log.d(TAG, "Signed in as " + googleAccount.getEmail());

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
                                    .setApplicationName("Recorder")
                                    .build();

                    mGoogleDriveService = new GoogleDriveService(googleDriveService);

                    Preferences.checkedDriveButton(this,"isClicked",true);
                    query();

                })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }


    public void query() {
        if (mGoogleDriveService != null) {
            Log.d(TAG, "Querying for files.");

            mGoogleDriveService.queryFiles()
                    .addOnSuccessListener(fileList -> {

                        ArrayList drivList = new ArrayList();
                        for (File file : fileList.getFiles()) {

                            drivList.add(file.getName());
                            drivList.add(file.getId());
                        }

                        for (int i = 0; i < drivList.size(); i++) {
                            String arrayListName = drivList.get(i).toString();
                            Log.e("name", "arraylist name" + arrayListName);

                            switch (arrayListName) {
                                case "Call Recorder":
                                    Preferences.setDrviefolderId(this, "driveFolderName", drivList.get(i + 1).toString());
                                    Log.e("check folder name "," "+arrayListName);
                                    Intent intent = new Intent(GoogleDriveLogin.this, Home.class);
                                    startActivity(intent);
                                    finish();
                                    break;
                            }

                        }
                        Log.e("builderArray", "drivList " + drivList.size());

                        createFolder();


                    })
                    .addOnFailureListener(exception -> Log.e(TAG, "Unable to query files.", exception));
        }
    }

    private void createFolder() {
        String driveFolderId = Preferences.getDriveFolderId(this, "driveFolderName");
        Log.e("driveFOlderID CHECK ", " " + driveFolderId);
        if (mGoogleDriveService != null){
            if (driveFolderId == null) {
                mGoogleDriveService.createFolder(GoogleDriveLogin.this,"Call Recorder");

                startActivity(new Intent(this,Home.class));
                finish();
            }

            // createSubFolder(this);

        }
    }

    private void createSubFolder(Context context) {

        String fileDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        Date date = new Date();

        Long prefSaveDate = Preferences.getSubFolderDate(context, "subFolderDate");
        Log.e("prefDate", " " + prefSaveDate);
        Log.e("prefDate", "checkDate " + date.getDate());

        if (prefSaveDate != date.getDate()) {
            Preferences.setSubFolderDate(context, "subFolderDate", date);
            mGoogleDriveService.createSubFolder(context,fileDate);
            Log.e("ahofa","check"+Preferences.getSubFolderDate(context,"subFolderDate"));
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

            createSubFolder(context);

            String subRootFolderId = Preferences.getDriveSubFolderId(context, "subFolderId");

            Log.e("subfolder save Id",""+subRootFolderId);

            mGoogleDriveService.uploadFile(context,phoneNumber,filePath);


            return null;
        }
    }




    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
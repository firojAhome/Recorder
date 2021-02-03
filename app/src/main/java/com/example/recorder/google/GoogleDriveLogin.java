package com.example.recorder.google;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.recorder.R;
import com.example.recorder.storage.Preferences;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.people.v1.model.Person;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class GoogleDriveLogin extends AppCompatActivity{

    private static final int RC_SIGN_IN = 1;
    private static final String TAG = "GoogleDriveLogin";
    public static final int REQUEST_PERMISSION_TO_UPLOAD = 2;
    SignInButton signInButton;
    GoogleSignInClient mGoogleSignInClient;
    GoogleSignInOptions gso;

    DriveResourceClient mDriveResourceClient;
    GoogleDriveService mGoogleDriveService;
    String serverClientId = "23602232397-2ndsrgt44jqt7dodt20gquonfqm2i4qm.apps.googleusercontent.com";

    Context context;
    Button button;
    Scope driveSCOPE = new Scope(Scopes.DRIVE_FILE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_drive_login);

        button = findViewById(R.id.button);
        signInButton = findViewById(R.id.sign_in_button);
        Log.e("GoogledriveLogin", "Activity");

        Log.e("Google drive service", "0" + mGoogleDriveService);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

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

    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(GoogleDriveLogin.this, "LogOut", Toast.LENGTH_SHORT).show();
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

            case REQUEST_PERMISSION_TO_UPLOAD:
                if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_PERMISSION_TO_UPLOAD) {
                    Log.e("signIn result", "resul drive " + REQUEST_PERMISSION_TO_UPLOAD);
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
                mGoogleDriveService.createFolder("Call Recorder");

            }
            createSubFolder();
        }
    }

    private void createSubFolder() {

        String driveFolderId = Preferences.getDriveFolderId(this, "driveFolderName");
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date date = new Date();
        String strDate = formatter.format(date);
        System.out.println(formatter.format(date));

        String prefSaveDate = Preferences.getSubFolderDate(this, "subFolderDate");
        Log.e("prefDate", " " + prefSaveDate);
        Log.e("driveFOlderID CHECK ", " " + driveFolderId);
        Log.e("folder date CHECK ", " " + strDate);

        if (prefSaveDate != strDate) {
            mGoogleDriveService.createSubFolder(this,driveFolderId, strDate);
            Preferences.setSubFolderDate(this, "subFolderDate", strDate);

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

        Account mAccount;
        Context phoneContext;
        String filePath;
        String phoneNumber;

        public GetContactsTask(Account account, Context context, String absolutePath, String name) {
            mAccount = account;
            phoneContext = context;
            filePath = absolutePath;
            phoneNumber = name;
        }


        @Override
        protected List<Person> doInBackground(Void... voids) {
            List<Person> result = null;
            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            phoneContext,
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

            checkDriveStoragePermission(phoneContext,phoneNumber,filePath);

            return null;
        }
    }

    public void checkDriveStoragePermission(Context context, String phoneNumber, String absolutePath) {

        String driveFolderId = Preferences.getDriveFolderId(context, "driveFolderName");
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date date = new Date();
            String strDate = formatter.format(date);
        System.out.println(formatter.format(date));

        String prefSaveDate = Preferences.getSubFolderDate(context, "subFolderDate");
        Log.e("prefDate", " " + prefSaveDate);
        Log.e("driveFOlderID CHECK ", " " + driveFolderId);
        Log.e("folder date CHECK ", " " + strDate);
        if (prefSaveDate != strDate) {
            mGoogleDriveService.createSubFolder(this,driveFolderId, strDate);
            Preferences.setSubFolderDate(context, "subFolderDate", strDate);
            Log.e("subFolderDate","subDate "+strDate);
        }
        String subRootFolderId = Preferences.getDriveSubFolderId(context, "subFolderId");


        mGoogleDriveService.uploadFIleInDrive(subRootFolderId,phoneNumber,absolutePath);


    }


}
package com.example.recorder.onedrive;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.recorder.Home;
import com.example.recorder.R;
import com.example.recorder.storage.Preferences;
import com.google.gson.JsonObject;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.models.extensions.Drive;
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class OneDrive extends AppCompatActivity {

    private final static String[] SCOPES = {"Files.ReadWrite.AppFolder","Files.ReadWrite.All","User.Read","email"};
    /* Azure AD v2 Configs */
    final static String AUTHORITY = "https://login.microsoftonline.com/common";

    public ISingleAccountPublicClientApplication mSingleAccountApp;

    private static final String TAG = OneDrive.class.getSimpleName();


    /* UI & Debugging Variables */
    Toolbar toolbar;
    Button signInButton;
    String token = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_one_drive);

        toolbar = findViewById(R.id.one_drive_toolbar);
        signInButton = findViewById(R.id.one_drive_signIn);

        setSupportActionBar(toolbar);


        mSingAccountClint(this);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSingleAccountApp == null) {
                    Log.e("check sigIn 1","one drive");
                    return;
                }
                Log.e("check sigIn","one drive");

                mSingleAccountApp.signIn(OneDrive.this, null, SCOPES, getAuthInteractiveCallback());
            }
        });

    }


    public void mSingAccountClint(Context context) {

        Log.e("check is working","OneDrive");
        PublicClientApplication.createSingleAccountPublicClientApplication(context,
                R.raw.auth_config_single_account, new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        mSingleAccountApp = application;

                    }
                    @Override
                    public void onError(MsalException exception) {
                        Log.e("message","errer Exception"+exception);
                    }
                });


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

                Preferences.setOnDriveLogin(OneDrive.this,"Is_One_DriveLogIn",true);
                Log.d(TAG, "ID Token: " + authenticationResult.getAccount().getClaims().get("id_token"));
                Log.d(TAG, " root: " + authenticationResult.getAccount().getClaims().get("root"));
                Log.e("OneDrive tenentId"," "+authenticationResult.getTenantId());
                Log.e("OneDrive scope"," "+authenticationResult.getScope());

                Log.e("checkcheck","check");
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
                .get(new ICallback<Drive>() {
                    @Override
                    public void success(final Drive drive) {
                        Log.d(TAG, "Found Drive " + drive.items);
                        Log.e("TAG", "Found Drive " + drive.id);
                        displayGraphResult(drive.getRawObject());
                    }
                    
                    @Override
                    public void failure(ClientException ex) {
                        Log.e("call graph api failed"," "+ex);
                    }
                });

        Intent i = new Intent(OneDrive.this, Home.class);
        startActivity(i);
    }


    private void displayGraphResult(JsonObject rawObject) {
        Log.e("graph result"," "+rawObject);
    }



    public void saveDataInOneDrive(Context context,String fileName, String filePath){

        String _audioBase64 = null;
        String time = new SimpleDateFormat("dd_MM_yyyy").format(new Date());
        Log.e("print time"," "+time);

        String subfileName = time+"/"+fileName;

        String URL = "https://graph.microsoft.com/v1.0/me/drive/root:/"+"Call Records"+"/"+subfileName+":/content";
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


}
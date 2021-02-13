package com.example.recorder.onedrive;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.recorder.R;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.authentication.MSALAuthenticationProvider;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.core.IClientConfig;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.models.extensions.Drive;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.Folder;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.onedrive.sdk.authentication.MSAAuthenticator;
import com.onedrive.sdk.core.DefaultClientConfig;
import com.onedrive.sdk.extensions.IOneDriveClient;
import com.onedrive.sdk.extensions.Item;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;


public class OneDrive extends AppCompatActivity implements View.OnClickListener {

    private final static String[] SCOPES = {"Files.ReadWrite.AppFolder"};
    /* Azure AD v2 Configs */
    final static String AUTHORITY = "https://login.microsoftonline.com/common";
    //    https://graph.microsoft.com/v1.0/me/drive/root/children
    private ISingleAccountPublicClientApplication mSingleAccountApp;

    private static final String TAG = OneDrive.class.getSimpleName();
    AuthenticationResult authenticationResult;

    /* UI & Debugging Variables */
    Toolbar toolbar;
    Button signInButton, signOut,silentLogin;
    IAccount mAccount;
    String token = null;
    private AuthenticationHelper mAuthHelper = null;

    RequestQueue requestQueue;
    //git
    MSAAuthenticator msaAuthenticator;
    private ConnectivityManager mConnectivityManager;
    private final AtomicReference<IOneDriveClient> mClient = new AtomicReference<>();
    MSALAuthenticationProvider msalAuthenticationProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_one_drive);

        toolbar = findViewById(R.id.one_drive_toolbar);
        signInButton = findViewById(R.id.one_drive_signIn);
        silentLogin = findViewById(R.id.silentLogin);
        signOut = findViewById(R.id.signOut);

        signInButton.setOnClickListener(this);
        silentLogin.setOnClickListener(this);
        signOut.setOnClickListener(this);

        setSupportActionBar(toolbar);
        // add back arrow to toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }



       PublicClientApplication.createSingleAccountPublicClientApplication(getApplicationContext(),
                R.raw.auth_config_single_account, new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        mSingleAccountApp = application;
                        loadAccount();
                    }
                    @Override
                    public void onError(MsalException exception) {
                        Log.e("message","errer Exception"+exception);
                    }
                });

        msaAuthenticator = new MSAAuthenticator() {
            @Override
            public String getClientId() {
                return "05e4ded4-f2e7-4eea-a676-ebc00e6d6811";
            }

            @Override
            public String[] getScopes() {
                return new String[] {"Files.ReadWrite.AppFolder","Files.ReadWrite.All","User.Read","email","wl.offline_access"};
            }
        };

        AuthenticationHelper.getInstance(getApplicationContext(),
                new IAuthenticationHelperCreatedListener() {
                    @Override
                    public void onCreated(AuthenticationHelper authHelper) {
                        mAuthHelper = authHelper;

                    }

                    @Override
                    public void onError(com.example.recorder.onedrive.MsalException e) {
                        Log.e("AUTH", "Error creating auth helper"+e);
                    }


                });

    }


//    public void authProvider() throws MsalClientException {
//        PublicClientApplication publicClientApplication = new PublicClientApplication(getApplicationContext(), "05e4ded4-f2e7-4eea-a676-ebc00e6d6811");
//        msalAuthenticationProvider = new MSALAuthenticationProvider(
//                this,
//                getApplication(),
//                publicClientApplication,
//                SCOPES);
//
//    }


    private void loadAccount() {

        if (mSingleAccountApp == null) {
            return;
        }

        mSingleAccountApp.getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(@Nullable IAccount activeAccount) {
                // You can use the account data to update your UI or your app database.
                 mAccount = activeAccount;
//                updateUI();

                if (mAccount != null){
                    Log.e("authority",""+activeAccount.getAuthority());
                    Log.e("check",""+mAccount.getAuthority());

                }

                return;
            }

            @Override
            public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
                if (currentAccount == null) {
                    // Perform a cleanup task as the signed-in account changed.
//                    showToastOnSignOut();
                    Log.e("no One drive user","is login");
                }
            }

            @Override
            public void onError(@NonNull MsalException exception) {
//                displayError(exception);
                Log.e("check error"," "+exception);
            }
        });

    }


    @Override
    public void onClick(View v) {
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



        signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSingleAccountApp == null){
                    return;
                }
                mSingleAccountApp.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
                    @Override
                    public void onSignOut() {
//                        updateUI(null);
//                        performOperationOnSignOut();

                    }
                    @Override
                    public void onError(@NonNull MsalException exception){
                        Log.e("signOut",""+exception);
                    }
                });
            }
        });

        silentLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSingleAccountApp == null) {
                    return;
                }
//                mAuthHelper.acquireTokenInteractively(this, getAuthCallback());
                mSingleAccountApp.acquireToken(OneDrive.this, SCOPES, getAuthInteractiveCallback());
//                mSingleAccountApp.acquireTokenSilentAsync(SCOPES, AUTHORITY, getAuthSilentCallback());
                creatFolderInOneDrive();
                creatFolderInOneDrive();
                volleyCreateFolder();
//                createFolder();
            }

        });
    }




    private AuthenticationCallback getAuthInteractiveCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                //* Successfully got a token, use it to call a protected resource - MSGraph *//*
                Log.d(TAG, "Successfully authenticated");

                Log.d(TAG, "ID Token: " + authenticationResult.getAccount().getClaims().get("id_token"));
                Log.d(TAG, " root: " + authenticationResult.getAccount().getClaims().get("root"));
                Log.e("OneDrive tenentId"," "+authenticationResult.getTenantId());
                Log.e("OneDrive scope"," "+authenticationResult.getScope());
//                updateUI(authenticationResult.getAccount());

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

//                mSingleAccountApp.acquireTokenSilentAsync(SCOPES, AUTHORITY, getAuthSilentCallback());
//silent  authentication
    private SilentAuthenticationCallback getAuthSilentCallback() {
        return new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Log.d(TAG, "Successfully authenticated");
                token = authenticationResult.getAccessToken();
                Log.d("AUTH", String.format("Access token: %s", token));
//                callGraphAPI(authenticationResult);

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

        /*try {
            PublicClientApplication publicClientApplication = new PublicClientApplication(getApplicationContext(), "05e4ded4-f2e7-4eea-a676-ebc00e6d6811");
        } catch (MsalClientException e) {
            e.printStackTrace();
        }*/


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
                        Log.d(TAG, "Found Drive " + drive.id);
                        Log.e("TAG", "Found Drive " + drive.id);
                        displayGraphResult(drive.getRawObject());
                    }


                    @Override
                    public void failure(ClientException ex) {
//                        displayError(ex);
                        Log.e("call graph api failed"," "+ex);
                    }
                });



    }


    private void displayGraphResult(JsonObject rawObject) {
//        logTextView.setText(graphResponse.toString());
        Log.e("graph result"," "+rawObject);
    }


    public void createFolder(){

         msalAuthenticationProvider = new MSALAuthenticationProvider(
                this,
                getApplication(),
                (PublicClientApplication) mSingleAccountApp,
                SCOPES);

        Log.e("check auth","0000 ");
        Log.e("check auth","0000 "+msalAuthenticationProvider);


//        IGraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider((IAuthenticationProvider) msaAuthenticator).buildClient();
        IGraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider(msalAuthenticationProvider).buildClient();
        DriveItem driveItem = new DriveItem();
        driveItem.name = "Call Recorder";
        Folder folder = new Folder();
        driveItem.folder = folder;
        driveItem.additionalDataManager().put("@microsoft.graph.conflictBehavior", new JsonPrimitive("rename"));

        graphClient.me().drive().root().children()
                .buildRequest()
                .post(driveItem);

        Log.e("create folder",""+graphClient.toString());
    }

    String data = "{"+
            "\"name"+"\"+ "+"New Folder"+"\","+
            "\"folder"+"\"+ "+"{}"+"\","+
            "\"@microsoft.graph.conflictBehavior"+"\"+ "+"rename"+"\""+"}";


    private void volleyCreateFolder(){
        String saveDate = data;
        String URL = "https://graph.microsoft.com/v1.0/me/drive/root/children";
//        String URL = "https://login.microsoftonline.com/3b7a0af3-e7d6-41f8-b648-3213e12358d0/v2.0/me/drive/root/children";

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {
                    JsonObject object = new JsonObject();
                    Log.e("Object response"," "+object.toString());
                }catch (Exception e){

                }

            }
        } ,new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(OneDrive.this, "Something Error!!!", Toast.LENGTH_SHORT).show();
            }
        }){
            //post data to server
            @Override
            public String getBodyContentType() {
                return token +"application/json";
            }


            @Override
            public byte[] getBody() throws AuthFailureError {

                try {
                    return saveDate == null ? null : saveDate.getBytes("utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
        requestQueue.add(stringRequest);

    }

    public void creatFolderInOneDrive(){
        if (token == null) {
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, "https://graph.microsoft.com/v1.0/me/calendars",
                null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {

                /* Successfully called graph, process data and send to UI */
                Log.d(TAG, "Response: " + response.toString());
            }

        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error: " + error.toString());
            }

        })
        {
            @Override
            public Map<String, String> getHeaders() {

                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        Log.d(TAG, "Adding HTTP GET to Queue, Request: " + request.toString());
        request.setRetryPolicy(new DefaultRetryPolicy(3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }

    //upload data
    public void saveDataInOneDrive(){

        StringRequest request = new StringRequest(Request.Method.PUT, "https://graph.microsoft.com/v1.0/me/drive/root:/ " + "fileName" + ":/content",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response)
                    {
                        Log.d ("PAST QUOTES SAVE", "Created file on server");
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
//                return textToSave.getBytes();
                return null;
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

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    ///rn0m6TJIR79gIT+Hb/ZVR1V3+c=
    //h8CU+6EM+5Pq2B3fm8Hwvw0ioas= //tomeko.com
//    https://github.com/OneDrive/onedrive-explorer-android/blob/master/app/src/main/java/com/microsoft/onedrive/apiexplorer/DeltaFragment.java

}
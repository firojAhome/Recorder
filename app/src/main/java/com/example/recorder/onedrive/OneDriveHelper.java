package com.example.recorder.onedrive;

import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.requests.extensions.GraphServiceClient;

public class OneDriveHelper implements IAuthenticationProvider {
    private static OneDriveHelper INSTANCE = null;
    private IGraphServiceClient mClient = null;
    private String mAccessToken = null;

    private OneDriveHelper() {
        mClient = GraphServiceClient.builder()
                .authenticationProvider(this).buildClient();
    }

    public static synchronized OneDriveHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new OneDriveHelper();
        }

        return INSTANCE;
    }

    // Part of the Graph IAuthenticationProvider interface
    // This method is called before sending the HTTP request
    @Override
    public void authenticateRequest(IHttpRequest request) {
        // Add the access token in the Authorization header
        request.addHeader("Authorization", "Bearer " + mAccessToken);
    }

    public void getUser(String accessToken, ICallback<User> callback) {
        mAccessToken = accessToken;

        // GET /me (logged in user)
        mClient.me().buildRequest()
                .select("displayName,mail,mailboxSettings,userPrincipalName")
                .get(callback);
    }
}
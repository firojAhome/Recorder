package com.example.recorder.onedrive;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.recorder.R;
import com.microsoft.aad.adal.AuthenticationCallback;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;

public class AuthenticationHelper {

    private static AuthenticationHelper INSTANCE = null;
    private ISingleAccountPublicClientApplication mPCA = null;
    private String[] mScopes = { "User.Read", "MailboxSettings.Read", "Calendars.ReadWrite" };

    AuthenticationHelper(Context ctx, final IAuthenticationHelperCreatedListener listener) {
        PublicClientApplication.createSingleAccountPublicClientApplication(ctx, R.raw.auth_config_single_account,
                new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        mPCA = application;
                        listener.onCreated(INSTANCE);
                    }

                    @Override
                    public void onError(com.microsoft.identity.client.exception.MsalException e) {
                        Log.e("AUTHHELPER", "Error creating MSAL application", e);

                    }

                });
    }

    public static synchronized void getInstance(Context ctx, IAuthenticationHelperCreatedListener listener) {
        if (INSTANCE == null) {
            INSTANCE = new AuthenticationHelper(ctx, listener);
        } else {
            listener.onCreated(INSTANCE);
        }
    }

    // Version called from fragments. Does not create an
    // instance if one doesn't exist
    public static synchronized AuthenticationHelper getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                    "AuthenticationHelper has not been initialized from MainActivity");
        }

        return INSTANCE;
    }

    public void acquireTokenInteractively(Activity activity, AuthenticationCallback callback) {
        mPCA.signIn(activity, null, mScopes, (com.microsoft.identity.client.AuthenticationCallback) callback);
    }

    public void acquireTokenSilently(AuthenticationCallback callback) {
        // Get the authority from MSAL config
        String authority = mPCA.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
        mPCA.acquireTokenSilentAsync(mScopes, authority, (SilentAuthenticationCallback) callback);
    }

    public void signOut() {
        mPCA.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
            @Override
            public void onSignOut() {
                Log.d("AUTHHELPER", "Signed out");
            }

            @Override
            public void onError(@NonNull com.microsoft.identity.client.exception.MsalException e) {
                Log.d("AUTHHELPER", "MSAL error signing out", e);
            }

        });
    }

}

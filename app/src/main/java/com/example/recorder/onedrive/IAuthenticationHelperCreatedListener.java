package com.example.recorder.onedrive;

public interface IAuthenticationHelperCreatedListener {

    void onCreated(final AuthenticationHelper authHelper);
    void onError(final MsalException exception);
}

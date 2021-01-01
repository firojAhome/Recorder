package com.example.recorder.drop;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.dropbox.core.android.Auth;
import com.example.recorder.Home;
import com.example.recorder.R;
import com.example.recorder.storage.Variable;
import com.example.recorder.storage.Preferences;

public class DropBoxLogin extends AppCompatActivity {

    Button dropLogin;
    Toolbar dropToolbar;
    SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drop_box_login);

        dropLogin = findViewById(R.id.drop_sign_in_button);
        dropToolbar = findViewById(R.id.drop_back_button);

        setSupportActionBar(dropToolbar);
        // add back arrow to toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }


        dropLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Auth.startOAuth2Authentication(getApplicationContext(), getString(R.string.APP_KEY));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getAccessToken();
    }

    public void getAccessToken() {
        String accessToken = Auth.getOAuth2Token(); //generate Access Token
        Variable.staticToken = accessToken;
        if (accessToken != null) {
            //Store accessToken in SharedPreferences
            sharedPreferences = getSharedPreferences(Variable.pref_name,Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(Variable.Drop_Access_Token, accessToken);
            editor.commit();
            Preferences.setToStorage(this,"preferenceToken",accessToken);

            //Proceed to MainActivity
            Intent intent = new Intent(DropBoxLogin.this, Home.class);
            startActivity(intent);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
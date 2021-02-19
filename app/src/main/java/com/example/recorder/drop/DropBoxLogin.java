package com.example.recorder.drop;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.dropbox.core.android.Auth;
import com.example.recorder.Home;
import com.example.recorder.R;
import com.example.recorder.storage.Preferences;


public class DropBoxLogin extends AppCompatActivity {

    Button dropLogin;
    Toolbar dropToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drop_box_login);

        dropToolbar = findViewById(R.id.drop_back_button);
        dropLogin = findViewById(R.id.drop_sign_in_button);

        setSupportActionBar(dropToolbar);
        // add back arrow to toolbar
        dropToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });



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
        if (accessToken != null) {
            //Store accessToken in SharedPreferences
            Preferences.setDropBoxAccessToken(this,"Drop_Box_Access_Token",accessToken);
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
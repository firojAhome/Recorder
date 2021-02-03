package com.example.recorder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.recorder.drop.DropBoxLogin;
import com.example.recorder.google.GoogleDriveLogin;
import com.example.recorder.google.GoogleDriveService;
import com.example.recorder.storage.Preferences;

public class Setting extends AppCompatActivity {

    RadioGroup radioGroup;
    Toolbar toolbar;
    Button button;
    LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);


        radioGroup = findViewById(R.id.radio_group);
        toolbar = findViewById(R.id.toolbar_setting);
        button = findViewById(R.id.submit);
        linearLayout = findViewById(R.id.linear_setting);



        setSupportActionBar(toolbar);
        // add back arrow to toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        RadioButton radioButton = (RadioButton) radioGroup.getChildAt(Preferences.getRadioIndex(this,"radioIndex"));
        radioButton.setChecked(true);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = group.findViewById(checkedId);
                RadioButton checkIndex = group.findViewById(checkedId);
                int radioIndex = group.indexOfChild(checkIndex);
                Preferences.setRadioIndex(getApplicationContext(),"radioIndex",radioIndex);
                CheckedPermission();
                
            }

        });


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), Home.class);
                startActivity(i);
                finish();
            }
        });


    }


    private void CheckedPermission() {
        switch (radioGroup.getCheckedRadioButtonId()) {
            case R.id.google:
                Intent i = new Intent(this, GoogleDriveLogin.class);
                startActivity(i);
                Toast.makeText(this, "Google drive login!", Toast.LENGTH_SHORT).show();
                break;

            case R.id.drop_box:
                if (!tokenExists()) {
                    Intent dropBoxLogin = new Intent(this, DropBoxLogin.class);
                    startActivity(dropBoxLogin);
                }else {
                    Intent dropIntent = new Intent(this,Home.class);
                    startActivity(dropIntent);
                }
                Toast.makeText(this, "drop", Toast.LENGTH_SHORT).show();
                break;

            case R.id.local:
                Toast.makeText(this, "local", Toast.LENGTH_SHORT).show();
                break;

            default:
                Toast.makeText(this, "Please Select any option", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    //drop box access token
    private boolean tokenExists() {
        String accessToken = Preferences.getPreferences(this,"prefreToken");
        return accessToken != null;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


}
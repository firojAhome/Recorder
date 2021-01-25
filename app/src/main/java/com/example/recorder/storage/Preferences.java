package com.example.recorder.storage;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.widget.RadioGroup;

import java.util.List;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

public class Preferences {

    Context context;


    public Preferences(Context context) {
        this.context = context;
    }



    protected final static String DEFAULT = null;
    String temp = null;

    // work
    public static void setToStorage(Context context, String key, String value) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.apply();
    }


    public static String getPreferences(Context context, String keyValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(keyValue, "");
    }


    //for radio Index
    public static void setRadioIndex(Context context, String RadioIndex, int key){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(RadioIndex, key);
        editor.apply();
    }

    public static int getRadioIndex(Context context, String keyValue){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(keyValue, 0);
    }

    public static void setGoogleAccestoken(Context context, String key, String value){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getDriveToken(Context context, String keyValue){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(keyValue, "");
    }
}

package com.example.recorder.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharedStorage {

    private static  int PREFS_INDEX = 111;
    private static  String RADIO_GROUP = null ;


    private Context context;


    public SharedStorage(Context context) {
        this.context = context;
    }




    public static boolean radioGroupIsSelected(String key, Boolean value, Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key,value);
        return true;
    }
    public static boolean getRadioButton(String key, Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(key,false);
    }



    //worked
    public static void setRadioIndex(String key, int value, Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key,value);
        editor.commit();

    }

    public static int getPrefsIndex(String key, Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(key, 012);
    }




}

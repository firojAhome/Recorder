package com.example.recorder.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharedStorage {

    private static  int PREFS_INDEX = 111;

    private Context context;
    private static SharedPreferences prefSetting;
    private static SharedPreferences.Editor prefEditor;
    public static final int PREFERENCE_MODE_PRIVATE = 0;
    public static final String MY_UNIQE_PREF_FILE = "DrawIt";

    public SharedStorage(Context context) {
        this.context = context;
    }

//    public void saveRadioIndex(int index) {
//        prefSetting = context.getSharedPreferences(MY_UNIQE_PREF_FILE,PREFERENCE_MODE_PRIVATE);
//        prefEditor = prefSetting.edit();
//        prefEditor.putInt("keyIndex", index);
//    }
//
//    public static int getSaveIndex(Context ctx){
//        prefSetting = ctx.getSharedPreferences(MY_UNIQE_PREF_FILE,PREFERENCE_MODE_PRIVATE);
//        int index = prefSetting.getInt("keyIndex",10);
//        return index;
//    }


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

    public static void setDropBoxToken(String key, String value, Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key,value);
        editor.commit();
    }

    public static String getDropBoxToken(String key, Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(key, null);
    }



}

package com.example.recorder.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.microsoft.identity.client.ISingleAccountPublicClientApplication;

import java.util.Date;

public class Preferences {


    Context context;

    public Preferences(Context context) {
        this.context = context;
    }



    // work
    public static void setDropBoxAccessToken(Context context, String key, String value) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.apply();
    }


    public static String getDropBoxAccessToken(Context context, String keyValue) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(keyValue, null);
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
        return sharedPreferences.getInt(keyValue, 3);
    }


    public static void setDrviefolderId(Context context, String key, String value){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getDriveFolderId(Context context, String keyValue){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(keyValue, null);
    }

    //drive
    public static void setSubFolderDate(Context context, String key, Date value){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(key, value.getDate());
        editor.apply();
    }

    public static Long getSubFolderDate(Context context, String keyValue){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(keyValue, 0);
    }


    public static void setDrvieSubFolderId(Context context, String key, String value){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getDriveSubFolderId(Context context, String keyValue){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(keyValue, null);
    }


    public static void checkedDriveButton(Context context, String key, boolean value){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static boolean getDriveButton(Context context, String keyValue){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(keyValue, false);
    }

    //dropbox date
    public static void setDropboxSubFolderDate(Context context, String key, Date value){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(key, value.getDate());
        editor.apply();
    }

    public static Long getDropboxSubFolderDate(Context context, String keyValue){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(keyValue, 0);
    }

    //oneDrive
    public static void setOnDriveLogin(Context context, String key, boolean value){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static boolean isOneDriveLogin(Context context, String keyValue){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(keyValue, false);
    }

}
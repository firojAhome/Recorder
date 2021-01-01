package com.example.recorder.storage;

import android.content.SharedPreferences;

public class Variable {

    public static SharedPreferences sharedPreferences;
    public static final String pref_name = "pref_name";

    public static int index_ID = 111;

    public static final String baseDir = "/CallRecords";

    //drop
    public static final String Drop_Access_Token = " drop_access_token";
    
    public static String staticToken;

}

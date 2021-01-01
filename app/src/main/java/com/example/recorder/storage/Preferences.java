package com.example.recorder.storage;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.List;
import java.util.Objects;

public class Preferences {

    Context context;
    public Preferences(Context context) {
        this.context = context;
    }


    public static void setToStorage(Context context, String name, String value) {
        SharedPreferences settings = context.getSharedPreferences(
                Variable.pref_name, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(name, value);
        editor.apply();
    }

    public static String getStorage(Context context, String param) {
        String value = "";
        if (checkIfActivityAlive(context)) {
            SharedPreferences settings = context.getSharedPreferences(Variable.pref_name, 0);
            value = settings.getString(param, "");
        }
        return value;
    }

    public static boolean checkIfActivityAlive(Context context) {
        if (context != null) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (Build.VERSION.SDK_INT == 29) {
                List<RunningTaskInfo> tasks = Objects.requireNonNull(activityManager).getRunningTasks(Integer.MAX_VALUE);
                for (RunningTaskInfo task : tasks) {
                    if (task.baseActivity != null && context.getPackageName().equalsIgnoreCase(task.baseActivity.getPackageName()))
                        return true;
                }
            } else {
                List<ActivityManager.RunningAppProcessInfo> tasks = Objects.requireNonNull(activityManager).getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo task : tasks) {
                    if (context.getPackageName().equalsIgnoreCase(task.processName))
                        return true;
                }
            }
        }
        return false;
    }
}

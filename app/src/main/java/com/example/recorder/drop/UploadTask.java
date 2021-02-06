package com.example.recorder.drop;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.LookupError;
import com.dropbox.core.v2.files.WriteMode;
import com.example.recorder.storage.Preferences;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UploadTask extends AsyncTask{

    private String name;
    private DbxClientV2 dbxClient;
    private File file;
    private Context context;


    public UploadTask(String number, DbxClientV2 dbxClient, File file, Context context) {
        this.name = number;
        this.dbxClient = dbxClient;
        this.file = file;
        this.context = context;

    }

    @Override
    protected Object doInBackground(Object[] params) {

        createFolder();
        Date date = new Date();
        String fileDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());

        Log.e("dropbox date","filedate "+fileDate);
        String filePath = "/Call Recorder/"+fileDate+"/";
        Long prefDropBox = Preferences.getDropboxSubFolderDate(context, "DropboxDate");

        Log.e("check0 date","pref"+prefDropBox);
        if (prefDropBox != date.getDate()) {
            try {
                dbxClient.files().createFolder("/Call Recorder/"+fileDate);
                Log.e("check date","pref"+prefDropBox);
            } catch (DbxException e) {
                e.printStackTrace();
            }
            Preferences.setDropboxSubFolderDate(context, "DropboxDate", date);
            Log.e("dropboxdate ","check"+Preferences.getDropboxSubFolderDate(context,"DropboxDate"));
        }
        String time = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss ").format(new Date());

        String fileName = name+" "+time;
        try {
            // Upload to Dropbox
//            FileInputStream inputStream = new FileInputStream(file);
            InputStream inputStream = new FileInputStream(file);
                  dbxClient.files().uploadBuilder(filePath + fileName+".mp3")
                            .withMode(WriteMode.ADD)
                            .uploadAndFinish(inputStream);
                    Log.d("Upload Status", "Success");


        } catch (DbxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
    }


    private void createFolder() {
        try
        {
            dbxClient.files().getMetadata("/Call Recorder");
        }
        catch (GetMetadataErrorException e) {
            // TODO Auto-generated catch block
            if (e.errorValue.isPath()) {
                LookupError le = e.errorValue.getPathValue();
                if (le.isNotFound()) {
                    System.out.println("Path doesn't exist on Dropbox: ");
                    try {
                        dbxClient.files().createFolder("/Call Recorder");
                    } catch (CreateFolderErrorException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (DbxException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            }
        } catch (DbxException e) {
            e.printStackTrace();
        }
    }



}

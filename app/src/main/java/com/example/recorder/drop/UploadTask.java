package com.example.recorder.drop;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.dropbox.core.DbxException;
import com.dropbox.core.v1.DbxEntry;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.LookupError;
import com.dropbox.core.v2.files.WriteMode;
import com.example.recorder.storage.Preferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Struct;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.security.auth.callback.Callback;

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
        Long prefDropBox = Preferences.getDropboxSubFolderDate(context, "subFolderDropboxDate");

        if (prefDropBox != date.getDate()) {
            try {
                dbxClient.files().createFolder("/Call Recorder/"+fileDate);
                Preferences.setDropboxSubFolderDate(context, "subFolderDropboxDate", date);
            } catch (DbxException e) {
                e.printStackTrace();
            }

            Log.e("dropboxdate ","check"+Preferences.getDropboxSubFolderDate(context,"subFolderDropboxDate"));
        }

        try {
            // Upload to Dropbox
//            FileInputStream inputStream = new FileInputStream(file);
            InputStream inputStream = new FileInputStream(file);
                    dbxClient.files().uploadBuilder(filePath + name+".mp3")
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


  /*  @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected FileMetadata doInBackground(String... params) {

        String localUri = params[0];
//        File localFile = new File(UriHelpers.getPath(context, Uri.fromFile(new File(localUri))));

        File localFile = new File(String.valueOf(file));
        if (localFile != null) {
            String remoteFolderPath = params[1];

            // Note - this is not ensuring the name is a valid dropbox file name
            String remoteFileName = localFile.getName();
            try (InputStream inputStream = new FileInputStream(localFile)) {
                return dbxClient.files().uploadBuilder(remoteFolderPath + "/" + remoteFileName)
                        .withMode(WriteMode.ADD)
                        .uploadAndFinish(inputStream);
            }  catch (DbxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(FileMetadata fileMetadata) {
        super.onPostExecute(fileMetadata);
        Toast.makeText(context, "audio uploaded successfully", Toast.LENGTH_SHORT).show();

    }
*/

}

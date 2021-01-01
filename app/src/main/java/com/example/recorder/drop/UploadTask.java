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
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import javax.security.auth.callback.Callback;

public class UploadTask extends AsyncTask{

    private DbxClientV2 dbxClient;


    private File file;
    private Context context;


    public UploadTask(DbxClientV2 dbxClient, File file, Context context) {
        this.dbxClient = dbxClient;
        this.file = file;
        this.context = context;
    }


    @Override
    protected Object doInBackground(Object[] params) {

        try {
            // Upload to Dropbox
//            FileInputStream inputStream = new FileInputStream(file);
            InputStream inputStream = new FileInputStream(file);
                    dbxClient.files().uploadBuilder("/" + file.getName())
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
        Toast.makeText(context, "audio uploaded successfully", Toast.LENGTH_SHORT).show();
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

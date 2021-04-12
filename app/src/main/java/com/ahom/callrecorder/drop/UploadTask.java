package com.ahom.callrecorder.drop;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.os.Build.VERSION.SDK_INT;
import static com.ahom.callrecorder.RecordsHome.deleteLocalFile;
import static com.ahom.callrecorder.storage.Constant.Call_Records;

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


        String fileDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        Log.e("dropbox date","filedate "+fileDate);
//        String filePath = "/"+Call_Records+"/"+fileDate+"/";
        String filePath = "/"+fileDate+"/";

        try {
            // Upload to Dropbox
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

        deleteLocalFile(file.getAbsolutePath());

        return null;
    }
    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
    }

  /*  private void createFolder(String folderName) {
        try
        {
            dbxClient.files().getMetadata(folderName);
        }
        catch (GetMetadataErrorException e) {
            // TODO Auto-generated catch block
            if (e.errorValue.isPath()) {
                LookupError le = e.errorValue.getPathValue();
                if (le.isNotFound()) {
                    System.out.println("Path doesn't exist on Dropbox: ");
                    try {
                        dbxClient.files().createFolder(folderName);
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
*/


}

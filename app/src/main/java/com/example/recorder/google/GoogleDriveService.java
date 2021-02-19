package com.example.recorder.google;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.example.recorder.Home;
import com.example.recorder.storage.Preferences;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;


import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.example.recorder.storage.Constant.Google_Drive_Folder_Id;
import static com.example.recorder.storage.Constant.Google_Drive_SubFolder_Id;

public class GoogleDriveService {

    Context context;
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;


    public GoogleDriveService(Drive mDriveService) {
        this.mDriveService = mDriveService;
    }


    /**
     * Creates a text file in the user's My Drive folder and returns its file ID.
     */

    public Task<String> createFolder(Context context,String name){
        return Tasks.call(mExecutor,() ->{
            File fileMetadata = new File();
            fileMetadata.setName(name);
            fileMetadata.setMimeType("application/vnd.google-apps.folder");

            File parentFolder = mDriveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
            System.out.println("Folder ID: " + parentFolder.getId());

            Preferences.setDrviefolderId(context, "Google_Drive_Folder_Id", parentFolder.getId());

            Log.e("check id saved"," "+Preferences.getDriveFolderId(context,"Google_Drive_Folder_Id"));
//            Log.e("check folder Id"," "+Preferences.getSharedPreferenceValue(context,"Google_Drive_Folder_Id"));
            return parentFolder.getId();

        });

    }


// we have to share parent folder id the create
    public Task<String> createSubFolder(Context context, String subFolderName) {

        String driveFolderId = Preferences.getDriveFolderId(context, "driveFolderName");
        Log.e("print drivefolderId"," "+driveFolderId);
        return Tasks.call(mExecutor, () -> {
            File metadata = new File()
                    .setParents(Collections.singletonList(driveFolderId))
                    .setName(subFolderName)
                    .setMimeType("application/vnd.google-apps.folder");

            File googleFile = mDriveService.files().create(metadata).execute();

            Log.e("sub root Folder Id"," "+googleFile.getId());
            Log.e("sub root Folder Id"," "+"check");
            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }

            System.out.println("Folder Id "+googleFile.getId());
            Preferences.setDrvieSubFolderId(context,"Google_Drive_SubFolder_Id",googleFile.getId());
            Log.e("SERvice java",Preferences.getDriveSubFolderId(context,"Google_Drive_SubFolder_Id"));
            return googleFile.getId();

        });
    }

   /* public void uploadFIleInDrive(String fileId, String name, String absolutePath){


        AsyncTask<Void, Long, String> task = new AsyncTask<Void, Long, String>() {

            @Override
            protected String doInBackground(Void... voids) {

                java.io.File fileContent = new java.io.File(absolutePath);
                FileContent mediaContent = new FileContent("audio/mpeg", fileContent);


                File metadata = new File()
                        .setParents(Collections.singletonList(fileId))
                        .setName(name)
                        .setMimeType("audio/mpeg");

                File file = null;
                try {
                    file = mDriveService.files().create(metadata, mediaContent).setFields("id").execute();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.e("uploadfile name",""+file.getName());
                return file.getName();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                Toast.makeText(context, "Successfull!", Toast.LENGTH_SHORT).show();
            }

        };
    }*/

    public Task<String> uploadFile(Context context,String name, String absolutePath) {
        return Tasks.call(mExecutor, () -> {
            String subRootFolderId = Preferences.getDriveSubFolderId(context, "Google_Drive_SubFolder_Id");
            Log.e("subfolder save Id cla",""+subRootFolderId);

            File metadata = new File()
                    .setParents(Collections.singletonList(subRootFolderId))
                    .setName(name)
                    .setMimeType("audio/mpeg");

            java.io.File filePath = new java.io.File(absolutePath);


            FileContent mediaContent = new FileContent("audio/mpeg", filePath);

            Long fileLength =  mediaContent.getLength();

            InputStreamContent mediaContent2 = new InputStreamContent("application/vnd.google-apps.audio", new FileInputStream(absolutePath));

            mediaContent2.setLength(absolutePath.length());

            Drive.Files.Create create=mDriveService.files().create(metadata,mediaContent2);

            if(fileLength > 5 * 1024 * 1024)
            {
                MediaHttpUploader uploader = create.getMediaHttpUploader();
                uploader.setDirectUploadEnabled(false);
                uploader.setChunkSize(MediaHttpUploader.MINIMUM_CHUNK_SIZE);

               File file = create.execute();
                if (file != null) {

                }
            }

            File file = mDriveService.files().create(metadata, mediaContent)
                    .setFields("id")
                    .execute();
            System.out.println("File ID: " + file.getId());

            return file.getId();
        });
    }


    public Task<Pair<String, String>> readFile(String fileId) {
        return Tasks.call(mExecutor, () -> {
            // Retrieve the metadata as a File object.
            File metadata = mDriveService.files().get(fileId).execute();
            String name = metadata.getName();

            // Stream the file contents to a String.
            try (InputStream is = mDriveService.files().get(fileId).executeMediaAsInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                 StringBuilder stringBuilder = new StringBuilder();
                 String line;

                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String contents = stringBuilder.toString();

                return Pair.create(name, contents);
            }
        });
    }

    /*public void storeData(String fileId, String name, String filePath){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                File body = new File();
                body.setName(name);
                body.setMimeType("application/vnd.google-apps.audio");
                body.setParents(Collections.singletonList(fileId));

                HttpHeaders header = new HttpHeaders();
                header.setContentLength(0L);
                header.setContentType(filePath);
                header.set("X-Upload-Content-Type","image/jpeg");

                try {
                    HttpResponse response= mDriveService
                            .files()
                            .create(body)
                            .setRequestHeaders(header)
                            .set("uploadType","resumable")
                            .buildHttpRequest()
                            .execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        });
        thread.start();
    }*/



    public Task<FileList> queryFiles() {
        return Tasks.call(mExecutor, () ->
                mDriveService.files().list().setSpaces("drive").execute());
    }



    public Intent createFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/.mp3");
        return intent;
    }


    public Task<Pair<String, String>> openFileUsingStorageAccessFramework(
            ContentResolver contentResolver, Uri uri) {
        return Tasks.call(mExecutor, () -> {
            // Retrieve the document's display name from its metadata.
            String name;
            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    name = cursor.getString(nameIndex);
                } else {
                    throw new IOException("Empty cursor returned for file.");
                }
            }

            // Read the document's contents as a String.
            String content;
            try (InputStream is = contentResolver.openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                content = stringBuilder.toString();
            }

            return Pair.create(name, content);
        });
    }


//    https://github.com/googleworkspace/android-samples/blob/master/drive/deprecation/app/src/main/java/com/google/android/gms/drive/sample/driveapimigration/MainActivity.java

}

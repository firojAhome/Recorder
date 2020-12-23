package com.example.recorder.fragment;

import android.content.Context;


import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DriveServiceHelper {

    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private com.google.api.services.drive.Drive mDriveService;

    public DriveServiceHelper(Drive mDriveService) {
        this.mDriveService = mDriveService;
    }


//    create file in google
    public Task<String> createAudioFile(String filePath) throws IOException {
        return Tasks.call(mExecutor,() -> {
            com.google.api.services.drive.model.File fileMetaData = new com.google.api.services.drive.model.File();
            fileMetaData.setName("MyCall's");

            java.io.File file = new java.io.File(filePath);
            FileContent mediaContent = new FileContent("application/audio",file);
            com.google.api.services.drive.model.File myFile = null;
            try {
                myFile = mDriveService.files().create(fileMetaData,mediaContent).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (myFile == null){
                throw new IOException("Null result when requesting file creation");
            }

            return myFile.getId();
        });
    }
}

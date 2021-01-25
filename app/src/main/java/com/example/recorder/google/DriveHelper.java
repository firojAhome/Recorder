package com.example.recorder.google;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DriveHelper {

    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;

    public DriveHelper(Drive mDriveService) {
        this.mDriveService = mDriveService;
    }

    public Task<String> createFileAudio(String filePath){

        return Tasks.call(mExecutor, () ->{

            File fileMetaData = new File();
            fileMetaData.setName("Recorder");

            java.io.File file = new java.io.File(filePath);
            FileContent fileContent = new FileContent("recorder/.mp3",file);

            File myFile = mDriveService.files().create(fileMetaData,fileContent).execute();

            if (myFile == null){
                throw new IOException("NUll result");
            }
            return myFile.getId();


        });
    }
}

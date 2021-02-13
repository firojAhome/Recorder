package com.example.recorder.onedrive;


import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.webkit.MimeTypeMap;

import com.google.api.client.http.FileContent;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class OneDriveService {


    private static final String ANSI_INVALID_CHARACTERS = "\\/:*?\"<>|";


    public OneDriveService() {
    }


    static byte[] getFileBytes(final ContentProviderClient contentProvider, final Uri data)
            throws IOException, RemoteException {
        final ParcelFileDescriptor descriptor = contentProvider.openFile(data, "r");
        if (descriptor == null) {
            throw new RuntimeException("Unable to get the file ParcelFileDescriptor");
        }

        final int fileSize = (int) descriptor.getStatSize();
        return getFileBytes(contentProvider, data, 0, fileSize);
    }

    static byte[] getFileBytes(final ContentProviderClient contentProvider,
                               final Uri data,
                               final int offset,
                               final int size)
            throws IOException, RemoteException {
        final ParcelFileDescriptor descriptor = contentProvider.openFile(data, "r");
        if (descriptor == null) {
            throw new RuntimeException("Unable to get the file ParcelFileDescriptor");
        }

        final FileInputStream fis = new FileInputStream(descriptor.getFileDescriptor());
        final ByteArrayOutputStream memorySteam = new ByteArrayOutputStream(size);
//        FileContent.copyStreamContents(offset, size, fis, memorySteam);
        return memorySteam.toByteArray();
    }


    static int getFileSize(final ContentProviderClient contentProvider,
                           final Uri data)
            throws FileNotFoundException, RemoteException {
        final ParcelFileDescriptor descriptor = contentProvider.openFile(data, "r");
        if (descriptor == null) {
            throw new RuntimeException("Unable to get the file ParcelFileDescriptor");
        }

        return (int) descriptor.getStatSize();
    }


    static String getValidFileName(final ContentResolver contentResolver, final Uri data) {
        String fileName = removeInvalidCharacters(data.getLastPathSegment());
        if (fileName.indexOf('.') == -1) {
            final String mimeType = contentResolver.getType(data);
            final String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            fileName = fileName + "." + extension;
        }
        return fileName;
    }


    private static String removeInvalidCharacters(final String fileName) {
        // TODO: This is not complete as there are UNICODE specific characters that also need to be removed.
        String fixedUpString = Uri.decode(fileName);
        for (int i = 0; i < ANSI_INVALID_CHARACTERS.length(); i++) {
            fixedUpString = fixedUpString.replace(ANSI_INVALID_CHARACTERS.charAt(i), '_');
        }
        return Uri.encode(fixedUpString);
    }


    private static int copyStreamContents(final long offset,
                                          final int size,
                                          final InputStream input,
                                          final OutputStream output)
            throws IOException {
        byte[] buffer = new byte[size];
        int count = 0;
        int n;

        final long skipAmount = input.skip(offset);
        if (skipAmount != offset) {
            throw new RuntimeException(
                    String.format("Unable to skip in the input stream actual %d, expected %d", skipAmount, offset));
        }
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
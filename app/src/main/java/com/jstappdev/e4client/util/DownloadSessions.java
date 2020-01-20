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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;

public class DownloadSessions extends AsyncTask<ArrayList<E4Session>, String, String> {

    private final WeakReference<MainActivity> contextRef;
    private final SharedViewModel sharedViewModel;
    private final SessionsAdapter adapter;

    public DownloadSessions(final SessionsAdapter sessionsAdapter, final Context context) {
        this.adapter = sessionsAdapter;
        contextRef = new WeakReference<>((MainActivity) context);
        sharedViewModel = ViewModelProviders.of((MainActivity) context).get(SharedViewModel.class);
    }

    @SuppressLint("DefaultLocale")
    @SafeVarargs
    @Override
    protected final String doInBackground(ArrayList<E4Session>... listsOfSessions) {

        final String url = "https://www.empatica.com/connect/download.php?id=";
        final int totalSessions = listsOfSessions[0].size();
        int downloadedSessions = 0;

        for (final E4Session e4Session : listsOfSessions[0]) {

            if (sharedViewModel.isSessionDownloaded(e4Session)) {
                continue;
            }

            final String sessionId = e4Session.getId();
            final String filename = e4Session.getZIPFilename();
            final Request request = new Request.Builder().url(url + sessionId).build();

            publishProgress(String.format("Downloading session %d/%d..", downloadedSessions++, totalSessions));

            MainActivity.okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    String message = String.format("Download failed for session %s: %s", e4Session.getId(), e.getMessage());

                    publishProgress(message);
                    Log.d(MainActivity.TAG, message);
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    if (response.isSuccessful()) {
                        final InputStream inputStream = response.body().byteStream();
                        final FileOutputStream out = contextRef.get().openFileOutput(filename, Context.MODE_PRIVATE);

                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = inputStream.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }

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
        return String.format("Downloading %d sessions.", totalSessions);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);

        if (values.length > 0 && values[0].length() > 0)
            sharedViewModel.getCurrentStatus().postValue(values[0]);

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        Collections.sort(sharedViewModel.getE4Sessions());

        adapter.notifyDataSetChanged();

        sharedViewModel.getCurrentStatus().postValue(s);
    }

}

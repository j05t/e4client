package com.jstappdev.e4client.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.R;
import com.jstappdev.e4client.Session;
import com.jstappdev.e4client.SessionsAdapter;
import com.jstappdev.e4client.SharedViewModel;
import com.jstappdev.e4client.Utils;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SessionsFragment extends Fragment {

    private SharedViewModel sharedViewModel;

    private CookieManager mCookieManager = null;

    private TextView statusTextView;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private String userid;

    private List<Session> sessions = new ArrayList<>();

    private OkHttpClient okHttpClient;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(SharedViewModel.class);

        View root = inflater.inflate(R.layout.fragment_sessions, container, false);
        statusTextView = root.findViewById(R.id.text_sessions);
        recyclerView = root.findViewById(R.id.recyclerview);


        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);

        root.findViewById(R.id.button_download_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (sessions.size() > 0) {
                    new DownloadAllSessions().execute();
                }
            }
        });
        root.findViewById(R.id.button_sync).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // todo: implement
            }
        });

        updateSessions();

        return root;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCookieManager = new CookieManager();
        mCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(mCookieManager);

        okHttpClient = new OkHttpClient();
        okHttpClient.setFollowRedirects(true);
        okHttpClient.setFollowSslRedirects(true);
        okHttpClient.setRetryOnConnectionFailure(true);
        okHttpClient.setConnectTimeout(120, TimeUnit.SECONDS);
        okHttpClient.setReadTimeout(120, TimeUnit.SECONDS);
        okHttpClient.setWriteTimeout(120, TimeUnit.SECONDS);
        okHttpClient.setCookieHandler(mCookieManager);
    }


    private List<HttpCookie> getCookies() {
        if (mCookieManager == null)
            return null;
        else
            return mCookieManager.getCookieStore().getCookies();
    }

    public void clearCookies() {
        if (mCookieManager != null)
            mCookieManager.getCookieStore().removeAll();
    }

    public boolean isCookieManagerEmpty() {
        if (mCookieManager == null)
            return true;
        else
            return mCookieManager.getCookieStore().getCookies().isEmpty();
    }


    public String getCookieValues() {
        String cookieValue = "";

        if (!isCookieManagerEmpty()) {
            for (HttpCookie eachCookie : getCookies())
                cookieValue = cookieValue + String.format("%s=%s; ", eachCookie.getName(), eachCookie.getValue());
        }

        return cookieValue;
    }

    private boolean login() {
        final String url = "https://www.empatica.com/connect/authenticate.php";

        final com.squareup.okhttp.RequestBody formBody = new com.squareup.okhttp.FormEncodingBuilder()
                .add("username", sharedViewModel.getUsername())
                .add("password", sharedViewModel.getPassword())
                .build();

        final Request request = new Request.Builder().url(url).post(formBody).build();

        final Response loginResponse;
        try {
            loginResponse = okHttpClient.newCall(request).execute();
            loginResponse.body().close();
        } catch (IOException e) {
            return false;
        }

        return loginResponse.isSuccessful();
    }

    private class LoginAndGetAllSessions extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {

            if (login()) {
                try {
                    final String sessionOverview = "https://www.empatica.com/connect/sessions.php";
                    final Request r = new Request.Builder().url(sessionOverview).build();
                    final Response sessionResponse = okHttpClient.newCall(r).execute();
                    final String res = sessionResponse.body().string();

                    final Pattern pattern = Pattern.compile("var userId = (.*?);");
                    final Matcher matcher = pattern.matcher(res);
                    if (matcher.find()) {
                        Log.d("e4", "found userid: " + matcher.group(1));
                        userid = matcher.group(1);
                    }

                    final String loadAllSessions = "https://www.empatica.com/connect/connect.php/users/"
                            + userid
                            + "/sessions?from=0&to=999999999999";

                    final Request s = new Request.Builder().url(loadAllSessions).build();
                    final String sessionsJSON = okHttpClient.newCall(s).execute().body().string();

                    final JSONArray jArray = new JSONArray(sessionsJSON);
                    for (int i = 0; i < jArray.length(); i++) {
                        try {
                            JSONObject oneObject = jArray.getJSONObject(i);

                            final String id = oneObject.getString("id");
                            final Long start_time = oneObject.getLong("start_time");
                            final Long duration = oneObject.getLong("duration");
                            final String device_id = oneObject.getString("device_id");
                            final String label = oneObject.getString("label");
                            final String device = oneObject.getString("device");
                            final String status = oneObject.getString("status");
                            final String exit_code = oneObject.getString("exit_code");

                            final Session session = new Session(id, start_time, duration, device_id, label, device, status, exit_code);
                            sessions.add(session);
                        } catch (JSONException e) {
                            // Oops
                        }
                    }

                    return "Synchronization successful";

                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
            }

            return "Synchronization failed";
        }

        @Override
        protected void onPostExecute(String result) {
            statusTextView.setText(result);
            mAdapter = new SessionsAdapter(sessions);
            recyclerView.setAdapter(mAdapter);
        }
    }

    private void updateSessions() {
        if (sharedViewModel.getUsername().isEmpty() || sharedViewModel.getPassword().isEmpty()) {
            Toast.makeText(requireContext(), "Please insert your Empatica account credentials first.", Toast.LENGTH_LONG).show();
            return;
        }

        new LoginAndGetAllSessions().execute();
    }


    private class DownloadAllSessions extends AsyncTask<Void, String, String> {
        final String url = "https://www.empatica.com/connect/download.php?id=";

        @Override
        protected String doInBackground(Void... voids) {
            for (Session session : sessions) {
                final String sessionId = session.getId();
                final String filename = Utils.getFileName(session);

                final File file = new File(requireActivity().getFilesDir(), filename);
                if (file.exists()) {
                    publishProgress("File " + filename + " already downloaded.");
                    continue;
                }

                final Request request = new Request.Builder().url(url + sessionId).build();

                publishProgress("Downloading Session " + sessionId);

                okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                        publishProgress("Download failed for " + filename);

                        Log.d(MainActivity.TAG, "Failed Download for " + filename + " " + e.getMessage());
                    }

                    @Override
                    public void onResponse(Response response) throws IOException {
                        if (response.isSuccessful()) {
                            InputStream inputStream = response.body().byteStream();
                            FileOutputStream out = requireActivity().openFileOutput(filename, Context.MODE_PRIVATE);

                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = inputStream.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }

                            publishProgress("Downloaded " + filename);
                            Log.d(MainActivity.TAG, "Downloaded " + filename);
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
                statusTextView.setText(values[0]);
        }
    }



}
package com.jstappdev.e4client.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    private Button downloadAllSessionsButton;
    private Button syncWithGoogleFitButton;

    private List<Session> sessions = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(SharedViewModel.class);

        mCookieManager = new CookieManager();
        mCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(mCookieManager);

        View root = inflater.inflate(R.layout.fragment_sessions, container, false);
        statusTextView = root.findViewById(R.id.text_sessions);
        recyclerView = root.findViewById(R.id.recyclerview);


        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);

        updateSessions();

        downloadAllSessionsButton = root.findViewById(R.id.button_download_all);
        syncWithGoogleFitButton = root.findViewById(R.id.button_sync);

        downloadAllSessionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (sessions.size() > 0) {
                    ArrayList<String> sessionIds = new ArrayList<String>();

                    for (Session session : sessions)
                        sessionIds.add(session.getId());

                    Log.d(MainActivity.TAG, sessions.toString());
                    Log.d(MainActivity.TAG, sessionIds.toString());


                    new DownloadAllSessions().execute(sessionIds.toArray(new String[0]));
                }
            }
        });
        syncWithGoogleFitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // todo: implement
            }
        });

        return root;
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

    private class GetAllSessions extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            OkHttpClient client = new OkHttpClient();

            com.squareup.okhttp.RequestBody formBody = new com.squareup.okhttp.FormEncodingBuilder()
                    .add("username", sharedViewModel.getUsername())
                    .add("password", sharedViewModel.getPassword())
                    .build();

            Request request = new Request.Builder().url(urls[0]).post(formBody).build();

            try {
                if ((client.newCall(request).execute()).isSuccessful()) {
                    final String sessionOverview = "https://www.empatica.com/connect/sessions.php";
                    Request r = new Request.Builder().url(sessionOverview).build();
                    Response sessionResponse = client.newCall(r).execute();
                    String res = sessionResponse.body().string();

                    Pattern pattern = Pattern.compile("var userId = (.*?);");
                    Matcher matcher = pattern.matcher(res);
                    if (matcher.find()) {
                        Log.d("e4", "found userid: " + matcher.group(1));
                        userid = matcher.group(1);
                    }

                    String loadAllSessions = "https://www.empatica.com/connect/connect.php/users/"
                            + userid
                            + "/sessions?from=0&to=999999999999";

                    Request s = new Request.Builder().url(loadAllSessions).build();
                    String sessionsJSON = client.newCall(s).execute().body().string();

                    JSONArray jArray = new JSONArray(sessionsJSON);
                    for (int i = 0; i < jArray.length(); i++) {
                        try {
                            JSONObject oneObject = jArray.getJSONObject(i);

                            String id = oneObject.getString("id");
                            String start_time = oneObject.getString("start_time");
                            String duration = oneObject.getString("duration");
                            String device_id = oneObject.getString("device_id");
                            String label = oneObject.getString("label");
                            String device = oneObject.getString("device");
                            String status = oneObject.getString("status");
                            String exit_code = oneObject.getString("exit_code");

                            Session session = new Session(id, start_time, duration, device_id, label, device, status, exit_code);
                            sessions.add(session);
                        } catch (JSONException e) {
                            // Oops
                        }
                    }

                    return sessionsJSON;

                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return "Download failed";
        }

        @Override
        protected void onPostExecute(String result) {
            statusTextView.setText("Synchronized with Empatica Cloud Account");

            mAdapter = new SessionsAdapter(sessions);
            recyclerView.setAdapter(mAdapter);
        }
    }

    private void updateSessions() {
        if (sharedViewModel.getUsername().isEmpty() || sharedViewModel.getPassword().isEmpty()) {
            Toast.makeText(requireContext(), "Please insert your Empatica account credentials first.", Toast.LENGTH_LONG).show();
            return;
        }

        GetAllSessions task = new GetAllSessions();
        task.execute("https://www.empatica.com/connect/authenticate.php");
    }


    private class DownloadAllSessions extends AsyncTask<String, String, String> {
        final String url = "https://www.empatica.com/connect/download.php?id=";

        @Override
        protected String doInBackground(String... ids) {

            OkHttpClient client = new OkHttpClient();

            for (String sessionId : ids) {

                // todo: check if already downloaded and skip
                String filename = "session_" + sessionId + ".zip";
                Request request = new Request.Builder().url(url + sessionId).build();

                publishProgress("Downloading Session " + sessionId);

                Response response = null;
                InputStream inputStream = null;

                try {
                    response = client.newCall(request).execute();

                    if (response.isSuccessful()) {

                        inputStream = response.body().byteStream();
                        FileOutputStream out = Objects.requireNonNull(getActivity()).openFileOutput(filename, Context.MODE_PRIVATE);

                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = inputStream.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }

                        publishProgress("Downloaded " + filename);
                    } else {
                        publishProgress("Download failed for " + filename);
                    }

                    response.body().close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            return "All Sessions Downloaded";
        }

        @Override
        protected void onPostExecute(String result) {
            statusTextView.setText(result);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            if (values.length > 0)
                statusTextView.setText(values[0]);
        }
    }
}
package com.jstappdev.e4client.ui;

import android.os.AsyncTask;
import android.os.Bundle;
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

import com.jstappdev.e4client.R;
import com.jstappdev.e4client.SessionsAdapter;
import com.jstappdev.e4client.SharedViewModel;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SessionsFragment extends Fragment {

    private SharedViewModel sharedViewModel;

    private CookieManager mCookieManager = null;

    private HashMap<String, String> credentials;

    TextView textView;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private String userid;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(SharedViewModel.class);

        mCookieManager = new CookieManager();
        mCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(mCookieManager);

        View root = inflater.inflate(R.layout.fragment_sessions, container, false);
        textView = root.findViewById(R.id.text_sessions);
        recyclerView = root.findViewById(R.id.recyclerview);


        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        mAdapter = new SessionsAdapter();
        recyclerView.setAdapter(mAdapter);

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
        String cookieValue = new String();

        if (!isCookieManagerEmpty()) {
            for (HttpCookie eachCookie : getCookies())
                cookieValue = cookieValue + String.format("%s=%s; ", eachCookie.getName(), eachCookie.getValue());
        }

        return cookieValue;
    }

    @Override
    public void onStart() {
        super.onStart();

        updateSessions();
    }

    private class DownloadWebPageTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            OkHttpClient client = new OkHttpClient();

            com.squareup.okhttp.RequestBody formBody = new com.squareup.okhttp.FormEncodingBuilder()
                    .add("username", sharedViewModel.getUsername())
                    .add("password", sharedViewModel.getPassword())
                    .build();

            Request request =
                    new Request.Builder()
                            .url(urls[0]).post(formBody)
                            .build();
            try {
                if ((client.newCall(request).execute()).isSuccessful()) {
                    final String sessionOverview = "https://www.empatica.com/connect/sessions.php";
                    Request r = new Request.Builder().url(sessionOverview).build();
                    Response sessionResponse = client.newCall(r).execute();
                    String res = sessionResponse.body().string();

                    //Log.d("e4", "sessionResponse: " + res);

                    Pattern pattern = Pattern.compile("var userId = (.*?);");
                    Matcher matcher = pattern.matcher(res);
                    if (matcher.find()) {
                        Log.d("e4", "found userid: " + matcher.group(1));
                        userid = matcher.group(1);
                    }

                    String loadAllSessions = "https://www.empatica.com/connect/connect.php/users/"
                            + userid
                            + "/sessions?from=0&to=999999999999";

                    //Log.d("e4", "opening: " + loadAllSessions);

                    Request s = new Request.Builder().url(loadAllSessions).build();
                    String sessionsJSON = client.newCall(s).execute().body().string();

                    //Log.d("e4", "sessionsJSON: " + sessionsJSON);

                    JSONArray jArray = new JSONArray(sessionsJSON);
                    for (int i = 0; i < jArray.length(); i++) {
                        try {
                            JSONObject oneObject = jArray.getJSONObject(i);

                            String start_time = oneObject.getString("start_time");
                            String duration = oneObject.getString("duration");
                            String device_id = oneObject.getString("device_id");
                            String device = oneObject.getString("device");
                            String status = oneObject.getString("status");
                            String exit_code = oneObject.getString("exit_code");

                            Log.d("e4", "session start: " + start_time + " duration" + duration + " status: " + status);
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
            textView.setText(result);
            Log.d("e4", result);
        }
    }


    // Triggered via a button in your layout
    public void updateSessions() {
        if (sharedViewModel.getUsername().isEmpty() || sharedViewModel.getPassword().isEmpty()) {
            Toast.makeText(requireContext(), "Please insert your login credentials first.", Toast.LENGTH_LONG).show();
            return;
        }

        DownloadWebPageTask task = new DownloadWebPageTask();
        task.execute("https://www.empatica.com/connect/authenticate.php");

    }
}
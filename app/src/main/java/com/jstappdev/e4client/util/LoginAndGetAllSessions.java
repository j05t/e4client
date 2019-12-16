package com.jstappdev.e4client.util;

import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.ViewModelProviders;

import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.SessionsAdapter;
import com.jstappdev.e4client.SharedViewModel;
import com.jstappdev.e4client.data.E4Session;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginAndGetAllSessions extends AsyncTask<Void, Void, String> {
    private final SharedViewModel sharedViewModel = ViewModelProviders.of(MainActivity.context).get(SharedViewModel.class);
    private SessionsAdapter adapter;

    public LoginAndGetAllSessions(SessionsAdapter mAdapter) {
        this.adapter = mAdapter;
    }

    @Override
    protected String doInBackground(Void... voids) {
        final String url = "https://www.empatica.com/connect/authenticate.php";

        final com.squareup.okhttp.RequestBody formBody = new com.squareup.okhttp.FormEncodingBuilder()
                .add("username", sharedViewModel.getUsername())
                .add("password", sharedViewModel.getPassword())
                .build();

        final Request request = new Request.Builder().url(url).post(formBody).build();

        final Response loginResponse;
        try {
            loginResponse = MainActivity.okHttpClient.newCall(request).execute();
            loginResponse.body().close();
        } catch (IOException e) {
            return "Failed to log in to Empatica cloud account.";
        }

        if (loginResponse.isSuccessful()) {
            try {
                final String sessionOverview = "https://www.empatica.com/connect/sessions.php";
                final Request r = new Request.Builder().url(sessionOverview).build();
                final Response sessionResponse = MainActivity.okHttpClient.newCall(r).execute();
                final String res = sessionResponse.body().string();

                final Pattern pattern = Pattern.compile("var userId = (.*?);");
                final Matcher matcher = pattern.matcher(res);
                if (matcher.find()) {
                    Log.d("e4", "found userid: " + matcher.group(1));
                    sharedViewModel.setUserId(matcher.group(1));
                } else {
                    return "Error: User ID not found.";
                }

                final String loadAllSessions = "https://www.empatica.com/connect/connect.php/users/"
                        + sharedViewModel.getUserId()
                        + "/sessions?from=0&to=999999999999";

                final Request s = new Request.Builder().url(loadAllSessions).build();
                final String sessionsJSON = MainActivity.okHttpClient.newCall(s).execute().body().string();
                final JSONArray jArray = new JSONArray(sessionsJSON);

                for (int i = 0; i < jArray.length(); i++) {
                    try {
                        final JSONObject oneObject = jArray.getJSONObject(i);

                        final String id = oneObject.getString("id");
                        final Long start_time = oneObject.getLong("start_time");
                        final Long duration = oneObject.getLong("duration");
                        final String device_id = oneObject.getString("device_id");
                        final String label = oneObject.getString("label");
                        final String device = oneObject.getString("device");
                        final String status = oneObject.getString("status");
                        final String exit_code = oneObject.getString("exit_code");

                        final E4Session e4Session = new E4Session(id, start_time, duration, device_id, label, device, status, exit_code);

                        if (!sharedViewModel.getE4Sessions().contains(e4Session))
                            sharedViewModel.getE4Sessions().add(e4Session);

                    } catch (JSONException e) {
                        // Oops
                    }
                }

                if (!Utils.isUploading) {
                    Collections.sort(sharedViewModel.getE4Sessions());
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
        sharedViewModel.getCurrentStatus().setValue(result);
        adapter.notifyDataSetChanged();
    }

}

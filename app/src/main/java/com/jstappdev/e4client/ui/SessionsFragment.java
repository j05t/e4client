package com.jstappdev.e4client.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.R;
import com.jstappdev.e4client.SessionsAdapter;
import com.jstappdev.e4client.SharedViewModel;
import com.jstappdev.e4client.Utils;
import com.jstappdev.e4client.data.E4Session;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static androidx.core.content.ContextCompat.getDrawable;


public class SessionsFragment extends Fragment {

    private SharedViewModel sharedViewModel;
    private SessionsAdapter mAdapter;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(SharedViewModel.class);

        final View root = inflater.inflate(R.layout.fragment_sessions, container, false);
        final TextView statusTextView = root.findViewById(R.id.text_sessions);
        final RecyclerView recyclerView = root.findViewById(R.id.recyclerview);


        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // vertical separator
        final DividerItemDecoration itemDecorator = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(Objects.requireNonNull(getDrawable(requireContext(), R.drawable.divider)));
        recyclerView.addItemDecoration(itemDecorator);

        mAdapter = new SessionsAdapter(sharedViewModel);
        recyclerView.setAdapter(mAdapter);


        root.findViewById(R.id.button_sync_empatica).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedViewModel.getSessionStatus().setValue("Syncing with Empatica cloud account.");
                new LoginAndGetAllSessions(mAdapter).execute();
            }
        });
        root.findViewById(R.id.button_download_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (sharedViewModel.getE4Sessions().size() > 0) {
                    //noinspection unchecked
                    new DownloadSessions(mAdapter).execute(sharedViewModel.getE4Sessions());
                }
            }
        });
        root.findViewById(R.id.button_sync_google).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // todo: implement google fit synchronization

            }
        });

        sharedViewModel.getSessionStatus().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                statusTextView.setText(s);
            }
        });


        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (sharedViewModel.getUsername().isEmpty() || sharedViewModel.getPassword().isEmpty()) {
            sharedViewModel.getSessionStatus().setValue("Please edit your Empatica account settings.");
            return;
        }

        final File directory = new File(requireContext().getFilesDir().getPath());
        final File[] files = directory.listFiles();

        if (files == null || files.length == 0) {
            Log.d(MainActivity.TAG, "no downloaded files found");

            sharedViewModel.getSessionStatus().setValue("No sessions in local storage.");
            return;
        }

        for (File file : files) {
            final String filename = file.getName();

            Log.d(MainActivity.TAG, "In files directory: " + filename);

            final String[] split = filename.split("_");

            if (split.length != 8) continue;

            // file name format: 1566420867_739826_31590_1e2fcd_10674_E4 3.2_0_0.zip

            final Long start_time = Long.valueOf(split[0]);
            final String id = split[1];
            final Long duration = Long.valueOf(split[2]);
            final String device_id = split[3];
            final String label = split[4];
            final String device = split[5];
            final String status = split[6];
            final String exit_code = split[7].substring(0, split[7].length() - 4); // strip .zip extension

            final E4Session e4Session = new E4Session(id, start_time, duration, device_id, label, device, status, exit_code);

            sharedViewModel.getSessionStatus().setValue(String.format("Found %s", e4Session));
            Log.d(MainActivity.TAG, "Already downloaded: " + e4Session);

            if (!sharedViewModel.getE4Sessions().contains(e4Session))
                sharedViewModel.getE4Sessions().add(e4Session);
        }
        sharedViewModel.getSessionStatus().setValue("Sessions in local storage: " + sharedViewModel.getE4Sessions().size());

        Collections.sort(sharedViewModel.getE4Sessions());

        mAdapter.notifyDataSetChanged();
    }


    private static class LoginAndGetAllSessions extends AsyncTask<Void, Void, String> {
        private SharedViewModel sharedViewModel = ViewModelProviders.of(MainActivity.context).get(SharedViewModel.class);
        private SessionsAdapter adapter;

        LoginAndGetAllSessions(SessionsAdapter mAdapter) {
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

                    return "Synchronization successful";

                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
            }

            return "Synchronization failed";
        }

        @Override
        protected void onPostExecute(String result) {
            sharedViewModel.getSessionStatus().setValue(result);
            adapter.notifyDataSetChanged();
        }

    }

    private static class DownloadSessions extends AsyncTask<ArrayList<E4Session>, String, String> {

        private SharedViewModel sharedViewModel = ViewModelProviders.of(MainActivity.context).get(SharedViewModel.class);
        private SessionsAdapter adapter;

        DownloadSessions(SessionsAdapter sessionsAdapter) {
            this.adapter = sessionsAdapter;
        }

        @SafeVarargs
        @Override
        protected final String doInBackground(ArrayList<E4Session>... listsOfSessions) {

            final String url = "https://www.empatica.com/connect/download.php?id=";

            for (final E4Session e4Session : listsOfSessions[0]) {

                final String sessionId = e4Session.getId();
                final String filename = e4Session.getZIPFilename();

                if (Utils.isSessionDownloaded(e4Session)) {
                    publishProgress("File " + filename + " already downloaded.");
                    continue;
                }

                final Request request = new Request.Builder().url(url + sessionId).build();

                publishProgress("Downloading Session " + sessionId);

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
                sharedViewModel.getSessionStatus().setValue(values[0]);

            adapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Collections.sort(sharedViewModel.getE4Sessions());

            adapter.notifyDataSetChanged();

            sharedViewModel.getSessionStatus().setValue(s);
        }

    }

}
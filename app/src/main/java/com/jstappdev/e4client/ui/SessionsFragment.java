package com.jstappdev.e4client.ui;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.R;
import com.jstappdev.e4client.SessionsAdapter;
import com.jstappdev.e4client.SharedViewModel;
import com.jstappdev.e4client.Utils;
import com.jstappdev.e4client.data.E4Session;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static androidx.core.content.ContextCompat.getDrawable;


public class SessionsFragment extends Fragment {

    private SharedViewModel sharedViewModel;
    private SessionsAdapter mAdapter;


    @SuppressLint("SourceLockedOrientationActivity")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

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
                sharedViewModel.getCurrentStatus().setValue("Syncing with Empatica cloud account..");
                new LoginAndGetAllSessions(mAdapter).execute();
            }
        });
        root.findViewById(R.id.button_download_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (sharedViewModel.getE4Sessions().size() > 0) {
                    //noinspection unchecked
                    new Utils.DownloadSessions(mAdapter).execute(sharedViewModel.getE4Sessions());
                }
            }
        });
        root.findViewById(R.id.button_sync_google).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (MainActivity.fitnessOptions == null) {
                    Toast.makeText(MainActivity.context, "Failed to initialize Google Fit Options. Check Google API settings.", Toast.LENGTH_LONG).show();
                } else if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(MainActivity.context), MainActivity.fitnessOptions)) {
                    GoogleSignIn.requestPermissions(
                            MainActivity.context,
                            MainActivity.GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                            GoogleSignIn.getLastSignedInAccount(MainActivity.context),
                            MainActivity.fitnessOptions);
                } else {
                    // todo: upload only selected sessions
                    //noinspection unchecked
                    new Utils.UploadE4SessionsToGoogleFit().execute(sharedViewModel.getE4Sessions());
                }
            }
        });

        sharedViewModel.getCurrentStatus().observe(getViewLifecycleOwner(), new Observer<String>() {
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

        final File directory = new File(requireContext().getFilesDir().getPath());
        final File[] files = directory.listFiles();

        if (files == null || files.length == 0) {
            sharedViewModel.getCurrentStatus().setValue("No sessions in local storage.");
        } else {
            for (final File file : files) {
                final String filename = file.getName();
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

                sharedViewModel.getCurrentStatus().setValue(String.format("Found session %s", e4Session));

                if (!sharedViewModel.getE4Sessions().contains(e4Session))
                    sharedViewModel.getE4Sessions().add(e4Session);
            }
        }

        sharedViewModel.getCurrentStatus().setValue("Sessions in local storage: " + sharedViewModel.getE4Sessions().size());

        if (!Utils.isUploading)
            Collections.sort(sharedViewModel.getE4Sessions());

        mAdapter.notifyDataSetChanged();

        if (sharedViewModel.getUsername().isEmpty() || sharedViewModel.getPassword().isEmpty()) {
            sharedViewModel.getCurrentStatus().setValue("Please edit your Empatica account settings.");
        } else if (sharedViewModel.getUserId() == null) {
            new LoginAndGetAllSessions(mAdapter).execute();
        }

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


}
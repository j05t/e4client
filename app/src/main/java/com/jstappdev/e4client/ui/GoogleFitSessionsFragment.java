package com.jstappdev.e4client.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.SessionReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.R;
import com.jstappdev.e4client.SharedViewModel;
import com.jstappdev.e4client.Utils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class GoogleFitSessionsFragment extends Fragment {

    private SharedViewModel sharedViewModel;

    private TextView textView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedViewModel = ViewModelProviders.of(requireActivity()).get(SharedViewModel.class);

        View root = inflater.inflate(R.layout.fragment_google_fit_sessions, container, false);

        textView = root.findViewById(R.id.text_send);

        return root;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Set a start and end time for our query, using a start time of 1 week before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.YEAR, -1);
        long startTime = cal.getTimeInMillis();

// Build a session read request
        SessionReadRequest readRequest = new SessionReadRequest.Builder()
                .setTimeInterval(1L, endTime, TimeUnit.MILLISECONDS).enableServerQueries()
                .readSessionsFromAllApps().setSessionName(MainActivity.SESSION_NAME)
                .build();

        Log.d(MainActivity.TAG, readRequest.toString());

// Invoke the Sessions API to fetch the session with the query and wait for the result
// of the read request. Note: Fitness.SessionsApi.readSession() requires the
// ACCESS_FINE_LOCATION permission.
        Fitness.getSessionsClient(MainActivity.context, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(MainActivity.context)))
                .readSession(readRequest)
                .addOnSuccessListener(new OnSuccessListener<SessionReadResponse>() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public void onSuccess(SessionReadResponse sessionReadResponse) {

                        final List<Session> sessions = sessionReadResponse.getSessions();

                        @SuppressLint("DefaultLocale") StringBuilder sb = new StringBuilder(String.format("Session read was successful.\nNumber of returned sessions is: %d\n\n", sessions.size()));

                        for (final Session session : sessions) {
                            // Process the session
                            sb.append(session.getName()).append(session.getIdentifier());

                            if (!sharedViewModel.getUploadedSessionIDs().contains(session.getIdentifier())) {
                                sharedViewModel.getUploadedSessionIDs().add(session.getIdentifier());
                                Log.d(MainActivity.TAG, "already uploaded to Google Fit: Session " + session.getIdentifier());
                            }

                            // Process the data sets for this session
                            List<DataSet> dataSets = sessionReadResponse.getDataSet(session);

                            sb.append(String.format("\nStart:\t%s\nEnd:\t%s\nDatasets:\t%d\n\n",
                                    Utils.getDate(session.getStartTime(TimeUnit.MILLISECONDS)),
                                    Utils.getDate(session.getEndTime(TimeUnit.MILLISECONDS)),
                                    dataSets.size()));

                            /*
                            for (DataSet dataSet : dataSets) {
                                sb.append("\n")
                                        .append(dataSet.getDataSource()).append("\n")
                                        .append(dataSet.getDataType().toString())
                                        .append(dataSet.getDataPoints().toString())
                                        .append("\n\n");

                                dumpDataSet(dataSet);
                            }
                             */
                        }
                        textView.setText(sb.toString());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(MainActivity.TAG, "Failed to read session");
                    }
                });
    }

    private static void dumpDataSet(DataSet dataSet) {
        Log.i(MainActivity.TAG, "Data returned for Data type: " + dataSet.getDataType().getName());

        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(MainActivity.TAG, "Data point:");
            Log.i(MainActivity.TAG, "\tType: " + dp.getDataType().getName());
            Log.i(MainActivity.TAG, "\tStart: " + Utils.getDate(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(MainActivity.TAG, "\tEnd: " + Utils.getDate(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for (Field field : dp.getDataType().getFields()) {
                Log.i(MainActivity.TAG, "\tField: " + field.getName() + " Value: " + dp.getValue(field));
            }
        }
    }
}
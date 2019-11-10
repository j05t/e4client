package com.jstappdev.e4client.ui;

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
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
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

public class SendFragment extends Fragment {

    private SharedViewModel sharedViewModel;

    private TextView textView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedViewModel = ViewModelProviders.of(requireActivity()).get(SharedViewModel.class);

        View root = inflater.inflate(R.layout.fragment_send, container, false);

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
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .readSessionsFromAllApps().setSessionName(MainActivity.SESSION_NAME)
                .build();

        Log.d(MainActivity.TAG, readRequest.toString());

// Invoke the Sessions API to fetch the session with the query and wait for the result
// of the read request. Note: Fitness.SessionsApi.readSession() requires the
// ACCESS_FINE_LOCATION permission.
         Fitness.getSessionsClient(MainActivity.context, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(MainActivity.context)))
                .readSession(readRequest)
                .addOnSuccessListener(new OnSuccessListener<SessionReadResponse>() {
                    @Override
                    public void onSuccess(SessionReadResponse sessionReadResponse) {
                        // Get a list of the sessions that match the criteria to check the result.
                        List<Session> sessions = sessionReadResponse.getSessions();

                        StringBuilder sb = new StringBuilder("Session read was successful. Number of returned sessions is: "
                                + sessions.size());
                        for (Session session : sessions) {
                            // Process the session
                            sb.append(session.getName()).append(session.getIdentifier());

                            // Process the data sets for this session
                            List<DataSet> dataSets = sessionReadResponse.getDataSet(session);
                            for (DataSet dataSet : dataSets) {
                                sb.append(dataSet.getDataSource()).append(dataSet.getDataType()).append(dataSet.getDataPoints());
                            }
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
}
package com.jstappdev.e4client.ui;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
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
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.R;
import com.jstappdev.e4client.SharedViewModel;
import com.jstappdev.e4client.Utils;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class GoogleFitSessionsFragment extends Fragment {

    private SharedViewModel sharedViewModel;

    private TextView textView;

    @SuppressLint("SourceLockedOrientationActivity")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        sharedViewModel = ViewModelProviders.of(requireActivity()).get(SharedViewModel.class);

        View root = inflater.inflate(R.layout.fragment_google_fit_sessions, container, false);

        textView = root.findViewById(R.id.text_send);

        return root;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        final long endTime = cal.getTimeInMillis();
        cal.add(Calendar.YEAR, -1);
        final long startTime = cal.getTimeInMillis();

        final FitnessOptions.Builder fitnessOptionsBuilder = FitnessOptions.builder();
        fitnessOptionsBuilder.addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ);
        for (DataType dataType : MainActivity.dataTypes) {
            Log.d(MainActivity.TAG, "adding datatype to fitnessoptions: " + dataType.toString());
            fitnessOptionsBuilder.addDataType(dataType, FitnessOptions.ACCESS_READ);
        }

        FitnessOptions fitnessOptions = fitnessOptionsBuilder.build();

        Log.d(MainActivity.TAG, "created fitnessOptions with implied scopes");
        for (Scope s : fitnessOptions.getImpliedScopes()) {
            Log.d(MainActivity.TAG, s.toString());
        }

        if (!GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(MainActivity.context),
                fitnessOptions)
        ) {
            Log.e(MainActivity.TAG, "no permission");
            GoogleSignIn.requestPermissions(
                    MainActivity.context, // your activity
                    MainActivity.GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(MainActivity.context),
                    fitnessOptions);
        } else {
            Log.e(MainActivity.TAG, "has permission");
        }

        Log.d(MainActivity.TAG, "signed in as " +
                GoogleSignIn.getLastSignedInAccount(MainActivity.context).getDisplayName());

        Fitness.getConfigClient(MainActivity.context,
                GoogleSignIn.getLastSignedInAccount(MainActivity.context))
                .readDataType("com.jstappdev.e4client.eda").addOnCompleteListener(new OnCompleteListener<DataType>() {
            @Override
            public void onComplete(@NonNull Task<DataType> task) {
                Log.e(MainActivity.TAG, "read com.jstappdev.e4client.eda: " + task.getResult().toString());
            }
        });

        DataSource dataSource = new DataSource.Builder()
                .setDataType(MainActivity.dataTypes.get(0))
                .setType(DataSource.TYPE_RAW)
                .setAppPackageName(MainActivity.context.getPackageName())
                .build();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .read(dataSource).enableServerQueries()
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        Log.d(MainActivity.TAG, "read request: " + readRequest.toString());

        Task<DataReadResponse> result = Fitness.getHistoryClient(MainActivity.context,
                Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(MainActivity.context)))
                .readData(readRequest);


        result.addOnCompleteListener(new OnCompleteListener<DataReadResponse>() {
            @Override
            public void onComplete(@NonNull Task<DataReadResponse> task) {
                StringBuilder sb = new StringBuilder(readRequest.toString());

                sb.append(task.getResult().getStatus().toString());
                sb.append(task.getResult().getDataSets().toString());
                for (DataSet dataSet : task.getResult().getDataSets()) {
                    sb.append(dataSet.getDataPoints());
                }
                textView.setText(sb.toString());
                Log.d(MainActivity.TAG, sb.toString());
            }
        });

        /*

// Build a session read request
        final SessionReadRequest readRequest = new SessionReadRequest.Builder()
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

                            // Process the data sets for the session
                            //List<DataSet> dataSets = sessionReadResponse.getDataSet(session);
                            List<DataSet> dataSets =sessionReadResponse.getDataSet(session, MainActivity.dataTypes.get(0));

                            sb.append(String.format("\nStart:\t%s\nEnd:\t%s\nDatasets:\t%d\n\n",
                                    Utils.getDate(session.getStartTime(TimeUnit.MILLISECONDS)),
                                    Utils.getDate(session.getEndTime(TimeUnit.MILLISECONDS)),
                                    dataSets.size()));


                            for (DataSet dataSet : dataSets) {
                                sb.append("\n")
                                        .append(dataSet.getDataSource()).append("\n")
                                        .append(dataSet.getDataType().toString())
                                        .append(dataSet.getDataPoints().toString())
                                        .append("\n\n");

                                dumpDataSet(dataSet);
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

         */
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
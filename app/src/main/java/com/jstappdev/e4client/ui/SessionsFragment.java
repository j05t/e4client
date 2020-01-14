package com.jstappdev.e4client.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

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
import com.jstappdev.e4client.data.E4Session;
import com.jstappdev.e4client.util.DownloadSessions;
import com.jstappdev.e4client.util.LoginAndGetAllSessions;
import com.jstappdev.e4client.util.UploadE4SessionsToGoogleFit;
import com.jstappdev.e4client.util.Utils;

import java.io.File;
import java.util.Collections;
import java.util.Objects;

import static androidx.core.content.ContextCompat.getDrawable;


public class SessionsFragment extends Fragment {

    private SharedViewModel sharedViewModel;
    private SessionsAdapter mAdapter;

    @SuppressLint("SourceLockedOrientationActivity")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(SharedViewModel.class);

        final View root = inflater.inflate(R.layout.fragment_sessions, container, false);
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

        mAdapter = new SessionsAdapter(requireContext());
        recyclerView.setAdapter(mAdapter);


        root.findViewById(R.id.button_sync_empatica).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedViewModel.getCurrentStatus().setValue("Syncing with Empatica cloud account..");
                new LoginAndGetAllSessions(mAdapter, sharedViewModel).execute();
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

                if (MainActivity.fitnessOptions == null) {
                    sharedViewModel.getCurrentStatus().setValue("Failed to initialize Google Fit Options. Check Google API settings.");
                } else if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(requireContext()), MainActivity.fitnessOptions)) {
                    GoogleSignIn.requestPermissions(
                            ((MainActivity) requireContext()),
                            MainActivity.GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                            GoogleSignIn.getLastSignedInAccount(requireContext()),
                            MainActivity.fitnessOptions);
                } else {
                    // todo: upload only selected sessions
                    //noinspection unchecked
                    new UploadE4SessionsToGoogleFit(requireContext()).execute(sharedViewModel.getE4Sessions());
                }
            }
        });

        final ProgressBar progressBar = root.findViewById(R.id.progressBar);

        //noinspection unchecked
        sharedViewModel.getIsLoading().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isLoading) {
                if (isLoading) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    sharedViewModel.setLoadingProgress(0);
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        sharedViewModel.getLoadingProgress().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer progress) {
                progressBar.setProgress(progress);
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
        }
    }


}
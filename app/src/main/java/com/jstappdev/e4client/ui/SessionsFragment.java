package com.jstappdev.e4client.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.jstappdev.e4client.R;
import com.jstappdev.e4client.SharedViewModel;

public class SessionsFragment extends Fragment {

    private SharedViewModel sharedViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedViewModel = ViewModelProviders.of(getActivity()).get(SharedViewModel.class);

        View root = inflater.inflate(R.layout.fragment_sessions, container, false);
        final TextView textView = root.findViewById(R.id.text_slideshow);

        return root;
    }
}
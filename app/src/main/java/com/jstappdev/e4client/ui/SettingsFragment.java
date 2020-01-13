package com.jstappdev.e4client.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.R;
import com.jstappdev.e4client.SharedViewModel;

import java.util.Objects;

public class SettingsFragment extends Fragment {

    private SharedViewModel sharedViewModel;

    private EditText edt_username;
    private EditText edt_password;

    @SuppressLint("SourceLockedOrientationActivity")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        sharedViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(SharedViewModel.class);

        final View root = inflater.inflate(R.layout.fragment_settings, container, false);

        edt_username = root.findViewById(R.id.e4username);
        edt_password = root.findViewById(R.id.e4password);

        root.findViewById(R.id.saveButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePreferences();
            }
        });

        edt_username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                final String user = edt_username.getText().toString();
                sharedViewModel.setUsername(user);
            }
        });
        edt_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                final String pass = edt_password.getText().toString();
                sharedViewModel.setPassword(pass);
            }
        });

        loadPreferences();

        return root;
    }


    private void loadPreferences() {

        final SharedPreferences settings = requireContext().getSharedPreferences(MainActivity.PREFS_NAME,
                Context.MODE_PRIVATE);

        sharedViewModel.setUsername(settings.getString(MainActivity.PREF_UNAME, ""));
        sharedViewModel.setPassword(settings.getString(MainActivity.PREF_PASSWORD, ""));

        edt_username.setText(sharedViewModel.getUsername());
        edt_password.setText(sharedViewModel.getPassword());
    }

    private void savePreferences() {
        final SharedPreferences settings = requireContext().getSharedPreferences(MainActivity.PREFS_NAME,
                Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = settings.edit();

        editor.putString(MainActivity.PREF_UNAME, sharedViewModel.getUsername());
        editor.putString(MainActivity.PREF_PASSWORD, sharedViewModel.getPassword());

        editor.apply();

        Toast.makeText(requireContext(), "Settings saved!", Toast.LENGTH_SHORT).show();
    }
}
package com.jstappdev.e4client.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.R;
import com.jstappdev.e4client.SharedViewModel;

public class SettingsFragment extends Fragment {

    private SharedViewModel sharedViewModel;

    private EditText edt_username;
    private EditText edt_password;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedViewModel = ViewModelProviders.of(getActivity()).get(SharedViewModel.class);

        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        edt_username = root.findViewById(R.id.e4username);
        edt_password = root.findViewById(R.id.e4password);

        return root;
    }


    @Override
    public void onResume() {
        super.onResume();
        loadPreferences();
    }

    private void loadPreferences() {

        SharedPreferences settings = requireContext().getSharedPreferences(MainActivity.PREFS_NAME,
                Context.MODE_PRIVATE);

        sharedViewModel.setUsername(settings.getString(MainActivity.PREF_UNAME, ""));
        sharedViewModel.setPassword(settings.getString(MainActivity.PREF_PASSWORD, ""));

        edt_username.setText(sharedViewModel.getUsername());
        edt_password.setText(sharedViewModel.getPassword());
    }

    @Override
    public void onPause() {
        super.onPause();
        savePreferences();
    }

    private void savePreferences() {
        final SharedPreferences settings = requireContext().getSharedPreferences(MainActivity.PREFS_NAME,
                Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = settings.edit();

        final String user = edt_username.getText().toString();
        final String pass = edt_password.getText().toString();

        sharedViewModel.setUsername(user);
        sharedViewModel.setPassword(pass);

        editor.putString(MainActivity.PREF_UNAME, user);
        editor.putString(MainActivity.PREF_PASSWORD, pass);

        Log.d("e4", "committed credentials for " + user + " with pass " + pass);
        editor.commit();
    }
}
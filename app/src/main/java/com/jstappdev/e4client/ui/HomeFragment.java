package com.jstappdev.e4client.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.R;
import com.jstappdev.e4client.SharedViewModel;

import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final String TAG = "e4";

    private SharedViewModel sharedViewModel;

    private TextView accel_xLabel;
    private TextView accel_yLabel;
    private TextView accel_zLabel;
    private TextView bvpLabel;
    private TextView edaLabel;
    private TextView ibiLabel;
    private TextView temperatureLabel;
    private TextView batteryLabel;
    private TextView statusLabel;
    private TextView deviceNameLabel;

    private LinearLayout dataCnt;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedViewModel = ViewModelProviders.of(getActivity()).get(SharedViewModel.class);

        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView textView = root.findViewById(R.id.status);
        sharedViewModel.getStatus().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Initialize vars that reference UI components
        statusLabel = getView().findViewById(R.id.status);
        dataCnt = getView().findViewById(R.id.dataArea);
        accel_xLabel = getView().findViewById(R.id.accel_x);
        accel_yLabel = getView().findViewById(R.id.accel_y);
        accel_zLabel = getView().findViewById(R.id.accel_z);
        bvpLabel = getView().findViewById(R.id.bvp);
        edaLabel = getView().findViewById(R.id.eda);
        ibiLabel = getView().findViewById(R.id.ibi);
        temperatureLabel = getView().findViewById(R.id.temperature);
        batteryLabel = getView().findViewById(R.id.battery);
        deviceNameLabel = getView().findViewById(R.id.deviceName);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getView().findViewById(R.id.disconnectButton).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (MainActivity.getDeviceManager() != null) {

                    MainActivity.getDeviceManager().disconnect();
                }
            }
        });

        sharedViewModel.getAcc().observe(this, new Observer<List<Integer>>() {

            public void onChanged(List<Integer> acc) {
                accel_xLabel.setText(String.format(Locale.getDefault(), "%d", acc.get(0)));
                accel_yLabel.setText(String.format(Locale.getDefault(), "%d", acc.get(1)));
                accel_zLabel.setText(String.format(Locale.getDefault(), "%d", acc.get(2)));
            }
        });

        sharedViewModel.getDeviceName().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String deviceName) {
                deviceNameLabel.setText(deviceName);
            }
        });
        sharedViewModel.getStatus().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String status) {
                statusLabel.setText(status);
            }
        });
        sharedViewModel.getOnWrist().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean onWrist) {
                // todo: maybe indicate on wrist
                //statusLabel.setText(onWrist);
                Log.d(TAG, onWrist.toString());
            }
        });
        sharedViewModel.getBattery().observe(this, new Observer<Float>() {
            @Override
            public void onChanged(Float battery) {
                batteryLabel.setText(String.format(Locale.getDefault(), "%.0f %%", battery * 100));
            }
        });
        sharedViewModel.getGsr().observe(this, new Observer<Float>() {
            @Override
            public void onChanged(Float gsr) {
                edaLabel.setText(String.format(Locale.getDefault(), "%.0f", gsr));
            }
        });
        sharedViewModel.getIbi().observe(this, new Observer<Float>() {
            @Override
            public void onChanged(Float ibi) {
                ibiLabel.setText(String.format(Locale.getDefault(), "%.0f", ibi));
            }
        });
        sharedViewModel.getTemp().observe(this, new Observer<Float>() {
            @Override
            public void onChanged(Float temp) {
                temperatureLabel.setText(String.format(Locale.getDefault(), "%.0f", temp));
            }
        });
        sharedViewModel.getBvp().observe(this, new Observer<Float>() {
            @Override
            public void onChanged(Float bvp) {
                bvpLabel.setText(String.format(Locale.getDefault(), "%.0f", bvp));
            }
        });
        sharedViewModel.getTemp().observe(this, new Observer<Float>() {
            @Override
            public void onChanged(Float temp) {
                temperatureLabel.setText(String.format(Locale.getDefault(), "%.0f", temp));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
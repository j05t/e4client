package com.jstappdev.e4client.ui;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.R;
import com.jstappdev.e4client.SharedViewModel;
import com.jstappdev.e4client.data.E4SessionData;
import com.jstappdev.e4client.util.Utils;

import java.util.Locale;

public class ConnectionFragment extends Fragment {

    private SharedViewModel sharedViewModel;

    private TextView accel_xLabel;
    private TextView accel_yLabel;
    private TextView accel_zLabel;
    private TextView bvpLabel;
    private TextView edaLabel;
    private TextView ibiLabel;
    private TextView hrLabel;
    private TextView temperatureLabel;
    private TextView batteryLabel;
    private TextView statusLabel;
    private TextView deviceNameLabel;
    private TextView wristStatusLabel;

    private View dataArea;

    private int currentBatteryDrawableId = R.drawable.ic_battery_full;
    private String microsiemens;
    private String celsius;


    @SuppressLint("SourceLockedOrientationActivity")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        final View root = inflater.inflate(R.layout.fragment_connection, container, false);
        final TextView textView = root.findViewById(R.id.status);

        sharedViewModel.getSessionStatus().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        statusLabel = view.findViewById(R.id.status);
        accel_xLabel = view.findViewById(R.id.accel_x);
        accel_yLabel = view.findViewById(R.id.accel_y);
        accel_zLabel = view.findViewById(R.id.accel_z);
        bvpLabel = view.findViewById(R.id.bvp);
        edaLabel = view.findViewById(R.id.eda);
        ibiLabel = view.findViewById(R.id.ibi);
        hrLabel = view.findViewById(R.id.hr);
        temperatureLabel = view.findViewById(R.id.temperature);
        batteryLabel = view.findViewById(R.id.battery);
        deviceNameLabel = view.findViewById(R.id.deviceName);
        wristStatusLabel = view.findViewById(R.id.wrist_status_label);
        dataArea = view.findViewById(R.id.dataArea);

        microsiemens = getString(R.string.microsiemens);
        celsius = getString(R.string.celsius);

        view.findViewById(R.id.disconnectButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) requireContext()).disconnect();
                dataArea.setVisibility(View.GONE);
                ((MainActivity) requireContext()).openFragment(R.id.nav_home);
            }
        });

        //noinspection ConstantConditions
        if (!sharedViewModel.getIsConnected().getValue()) {
            ((MainActivity) requireContext()).initEmpaticaDeviceManager();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        final LifecycleOwner owner = getViewLifecycleOwner();

        sharedViewModel.getIsConnected().observe(owner, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isConnected) {
                if (isConnected) {
                    dataArea.setVisibility(View.VISIBLE);

                    E4SessionData.clear();

                    E4SessionData.getInstance().setInitialTime(Utils.getCurrentTimestamp());
                } else {
                    dataArea.setVisibility(View.GONE);
                }
            }
        });
        sharedViewModel.getDeviceName().observe(owner, new Observer<String>() {
            @Override
            public void onChanged(String deviceName) {
                deviceNameLabel.setText(deviceName);
            }
        });
        sharedViewModel.getSessionStatus().observe(owner, new Observer<String>() {
            @Override
            public void onChanged(String status) {
                statusLabel.setText(status);
            }
        });
        sharedViewModel.getOnWrist().observe(owner, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean onWrist) {
                wristStatusLabel.setText(onWrist ? "ON WRIST" : "NOT ON WRIST");
            }
        });
        sharedViewModel.getBattery().observe(owner, new Observer<Float>() {
            @Override
            public void onChanged(Float battery) {
                batteryLabel.setText(String.format(Locale.getDefault(), "%.0f %%", battery * 100));
            }
        });

        sharedViewModel.getCurrentAccX().observe(owner, new Observer<Integer>() {
            public void onChanged(Integer accX) {
                accel_xLabel.setText(accX.toString());
            }
        });
        sharedViewModel.getCurrentAccY().observe(owner, new Observer<Integer>() {
            public void onChanged(Integer accY) {
                accel_yLabel.setText(accY.toString());
            }
        });
        sharedViewModel.getCurrentAccZ().observe(owner, new Observer<Integer>() {
            public void onChanged(Integer accZ) {
                accel_zLabel.setText(accZ.toString());
            }
        });

        sharedViewModel.getCurrentGsr().observe(owner, new Observer<Float>() {
            @Override
            public void onChanged(Float gsr) {
                edaLabel.setText(String.format(Locale.getDefault(), "%.2f %s", gsr, microsiemens));
            }
        });

        sharedViewModel.getCurrentIbi().observe(owner, new Observer<Float>() {
            @Override
            public void onChanged(Float ibi) {
                ibiLabel.setText(String.format(Locale.getDefault(), "%.2f s", ibi));
            }
        });

        sharedViewModel.getCurrentHr().observe(owner, new Observer<Float>() {
            @Override
            public void onChanged(Float averageHr) {
                hrLabel.setText(String.format(Locale.getDefault(), "%.0f BPM", averageHr));
            }
        });

        sharedViewModel.getCurrentTemp().observe(owner, new Observer<Float>() {
            @Override
            public void onChanged(Float temp) {
                temperatureLabel.setText(String.format(Locale.getDefault(), "%.2f %s", temp, celsius));
            }
        });

        sharedViewModel.getCurrentBvp().observe(owner, new Observer<Float>() {
            int count = 0;

            @Override
            public void onChanged(Float bvp) {
                if (count++ % 10 == 0)
                    bvpLabel.setText(String.format(Locale.getDefault(), "%.0f", bvp));
            }
        });
    }
}

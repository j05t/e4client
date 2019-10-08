package com.jstappdev.e4client.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.R;
import com.jstappdev.e4client.SharedViewModel;

import java.util.Locale;
import java.util.Objects;

public class ConnectionFragment extends Fragment {

    private static final int CALIBRATION_SAMPLES = 42;

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
    private TextView wristStatusLabel;

    private View dataArea;

    private ImageView batteryImageView;

    private int currentBatteryDrawableId = R.drawable.ic_battery_full;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(SharedViewModel.class);

        final View root = inflater.inflate(R.layout.fragment_connection, container, false);
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
        statusLabel = view.findViewById(R.id.status);
        accel_xLabel = view.findViewById(R.id.accel_x);
        accel_yLabel = view.findViewById(R.id.accel_y);
        accel_zLabel = view.findViewById(R.id.accel_z);
        bvpLabel = view.findViewById(R.id.bvp);
        edaLabel = view.findViewById(R.id.eda);
        ibiLabel = view.findViewById(R.id.ibi);
        temperatureLabel = view.findViewById(R.id.temperature);
        batteryLabel = view.findViewById(R.id.battery);
        deviceNameLabel = view.findViewById(R.id.deviceName);
        wristStatusLabel = view.findViewById(R.id.wrist_status_label);
        batteryImageView = view.findViewById(R.id.batteryImageView);
        dataArea = view.findViewById(R.id.dataArea);

        view.findViewById(R.id.disconnectButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.context.disconnect();
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final LifecycleOwner owner = getViewLifecycleOwner();

        sharedViewModel.getIsConnected().observe(owner, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isConnected) {
                if (isConnected) dataArea.setVisibility(View.VISIBLE);
                else dataArea.setVisibility(View.GONE);
            }
        });
        sharedViewModel.getDeviceName().observe(owner, new Observer<String>() {
            @Override
            public void onChanged(String deviceName) {
                deviceNameLabel.setText(deviceName);
            }
        });
        sharedViewModel.getStatus().observe(owner, new Observer<String>() {
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
                int id = currentBatteryDrawableId;

                if (battery > .87f)
                    id = R.drawable.ic_battery_full;
                else if (battery < .87f && battery > .49f)
                    id = R.drawable.ic_battery66;
                else if (battery < .49f && battery > .1f)
                    id = R.drawable.ic_battery33;
                else if (battery < .1f)
                    id = R.drawable.ic_battery_empty;

                if (id != currentBatteryDrawableId) {
                    batteryImageView.setImageResource(id);
                    currentBatteryDrawableId = id;
                }
            }
        });

        sharedViewModel.getLastAcc().observe(owner, new Observer<Integer>() {
            public void onChanged(Integer lastAcc) {
                if (lastAcc < CALIBRATION_SAMPLES) return;

                accel_xLabel.setText(String.format(Locale.getDefault(), "%d", sharedViewModel.getSesssionData().getAcc().getLast().get(0)));
                accel_yLabel.setText(String.format(Locale.getDefault(), "%d", sharedViewModel.getSesssionData().getAcc().getLast().get(1)));
                accel_zLabel.setText(String.format(Locale.getDefault(), "%d", sharedViewModel.getSesssionData().getAcc().getLast().get(2)));
            }
        });
        sharedViewModel.getLastGsr().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastGsr) {
                if (lastGsr < CALIBRATION_SAMPLES) return;

                edaLabel.setText(String.format(Locale.getDefault(), "%.0f", sharedViewModel.getSesssionData().getGsr().getLast()));
            }
        });
        sharedViewModel.getLastIbi().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastIbi) {
                if (lastIbi < CALIBRATION_SAMPLES) return;

                ibiLabel.setText(String.format(Locale.getDefault(), "%.0f", sharedViewModel.getSesssionData().getIbi().getLast()));
            }
        });
        sharedViewModel.getLastTemp().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastTemp) {
                if (lastTemp < CALIBRATION_SAMPLES) return;

                temperatureLabel.setText(String.format(Locale.getDefault(), "%.0f", sharedViewModel.getSesssionData().getTemp().getLast()));
            }
        });
        sharedViewModel.getLastBvp().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastBvp) {
                if (lastBvp < CALIBRATION_SAMPLES) return;

                bvpLabel.setText(String.format(Locale.getDefault(), "%.0f", sharedViewModel.getSesssionData().getBvp().getLast()));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
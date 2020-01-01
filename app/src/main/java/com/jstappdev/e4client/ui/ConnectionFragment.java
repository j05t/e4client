package com.jstappdev.e4client.ui;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
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
import com.jstappdev.e4client.data.E4SessionData;
import com.jstappdev.e4client.util.Utils;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

public class ConnectionFragment extends Fragment {

    private SharedViewModel sharedViewModel;

    private TextView accel_xLabel;
    private TextView accel_yLabel;
    private TextView accel_zLabel;
    private TextView bvpLabel;
    private TextView edaLabel;
    private TextView ibiLabel;
    private TextView hrLabel;
    private TextView hrvLabel;
    private TextView temperatureLabel;
    private TextView batteryLabel;
    private TextView statusLabel;
    private TextView deviceNameLabel;
    private TextView wristStatusLabel;

    private View dataArea;

    private ImageView batteryImageView;

    private int currentBatteryDrawableId = R.drawable.ic_battery_full;


    @SuppressLint("SourceLockedOrientationActivity")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        sharedViewModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(SharedViewModel.class);

        final View root = inflater.inflate(R.layout.fragment_connection, container, false);
        final TextView textView = root.findViewById(R.id.status);

        sharedViewModel.getSessionStatus().observe(this, new Observer<String>() {
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
        hrvLabel = view.findViewById(R.id.hrv);
        hrLabel = view.findViewById(R.id.hr);
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
                dataArea.setVisibility(View.GONE);
            }
        });

        //noinspection ConstantConditions
        if (!sharedViewModel.getIsConnected().getValue())
            MainActivity.context.initEmpaticaDeviceManager();
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
                try {
                    accel_xLabel.setText(String.format(Locale.getDefault(), "%d", E4SessionData.getInstance().getAcc().getLast().get(0)));
                    accel_yLabel.setText(String.format(Locale.getDefault(), "%d", E4SessionData.getInstance().getAcc().getLast().get(1)));
                    accel_zLabel.setText(String.format(Locale.getDefault(), "%d", E4SessionData.getInstance().getAcc().getLast().get(2)));
                } catch (NoSuchElementException e) {
                    Log.e("e4", "no such element");
                }
            }
        });
        sharedViewModel.getLastGsr().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastGsr) {
                try {
                    edaLabel.setText(String.format(Locale.getDefault(), "%.2f uS", E4SessionData.getInstance().getGsr().getLast()));
                } catch (NoSuchElementException e) {
                    Log.e("e4", "no such element");
                }
            }
        });
        sharedViewModel.getLastIbi().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastIbi) {
                try {
                    // HR/HRV is calculated from IBI
                    ibiLabel.setText(String.format(Locale.getDefault(), "%.2f s", E4SessionData.getInstance().getIbi().getLast()));
                    hrLabel.setText(String.format(Locale.getDefault(), "%.0f BPM", E4SessionData.getInstance().getHr().getLast()));
                    if (lastIbi % 10 == 0) {
                        final float hrv = Utils.calcHrvSDRR(E4SessionData.getInstance().getIbi());
                        hrvLabel.setText(String.format(Locale.getDefault(), "%.0f ms", hrv));
                    }
                } catch (NoSuchElementException e) {
                    Log.e("e4", "no such element");
                }
            }
        });
        sharedViewModel.getLastTemp().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastTemp) {
                try {
                    temperatureLabel.setText(String.format(Locale.getDefault(), "%.2f C", E4SessionData.getInstance().getTemp().getLast()));
                } catch (NoSuchElementException e) {
                    Log.e("e4", "no such element in " + E4SessionData.getInstance());
                }
            }
        });
        sharedViewModel.getLastBvp().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastBvp) {
                try {
                    if (lastBvp % 10 == 0)
                        bvpLabel.setText(String.format(Locale.getDefault(), "%.0f", E4SessionData.getInstance().getBvp().getLast()));
                } catch (NoSuchElementException e) {
                    Log.e("e4", "no such element ");
                }
            }
        });
    }
}

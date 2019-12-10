package com.jstappdev.e4client.ui;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.jstappdev.e4client.MainActivity;
import com.jstappdev.e4client.R;
import com.jstappdev.e4client.SharedViewModel;
import com.jstappdev.e4client.Utils;
import com.jstappdev.e4client.data.E4Session;
import com.jstappdev.e4client.data.E4SessionData;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Arrays;
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

                saveSessionToFile();
            }
        });

        MainActivity.context.initEmpaticaDeviceManager();
    }

    @SuppressLint("DefaultLocale")
    private synchronized void saveSessionToFile() {
        final E4SessionData sd = sharedViewModel.getSessionData();
        final E4Session e4Session = new E4Session("local", sd.getInitialTime(), new Timestamp(System.currentTimeMillis()).getTime() - sd.getInitialTime(), sharedViewModel.getDeviceName().getValue(), "local", sharedViewModel.getDeviceName().getValue(), "0", "0");
        e4Session.setE4SessionData(sd);

        final File sessionFile = new File(MainActivity.context.getFilesDir(), e4Session.getZIPFilename());

        Utils.trimCache(MainActivity.context);

        final String basePath = MainActivity.context.getCacheDir().getPath() + "/";

        final File edaFile = new File(basePath + "EDA.csv");
        final File tempFile = new File(basePath + "TEMP.csv");
        final File bvpFile = new File(basePath + "BVP.csv");
        final File hrFile = new File(basePath + "HR.csv");
        final File tagFile = new File(basePath + "tags.csv");
        final File ibiFile = new File(basePath + "IBI.csv");
        final File accFile = new File(basePath + "ACC.csv");

        try (final PrintWriter writer = new PrintWriter(new FileWriter(edaFile))) {
            writer.println(e4Session.getStartTime());
            writer.println("4.000000");
            for (float f : sd.getGsr()) writer.println(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (final PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
            writer.println(e4Session.getStartTime());
            writer.println("4.000000");
            for (float f : sd.getTemp()) writer.println(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (final PrintWriter writer = new PrintWriter(new FileWriter(bvpFile))) {
            writer.println(e4Session.getStartTime());
            writer.println("4.000000");
            for (float f : sd.getBvp()) writer.println(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (final PrintWriter writer = new PrintWriter(new FileWriter(hrFile))) {
            writer.println(e4Session.getStartTime());
            writer.println("1.000000");
            for (float f : sd.getHr()) writer.println(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (final PrintWriter writer = new PrintWriter(new FileWriter(tagFile))) {
            for (Double f : sd.getTags()) writer.println(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (final PrintWriter writer = new PrintWriter(new FileWriter(ibiFile))) {
            writer.println(e4Session.getStartTime() + ", IBI");
            writer.println("1.000000");
            for (int i = 0; i < sd.getIbi().size(); i++) {
                writer.println(String.format("%s,%s", sd.getIbiTimestamps().get(i) - e4Session.getStartTime(), sd.getIbi().get(i)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (final PrintWriter writer = new PrintWriter(new FileWriter(accFile))) {
            writer.println(e4Session.getStartTime() + ", " + e4Session.getStartTime() + ", " + e4Session.getStartTime());
            writer.println("32.000000, 32.000000, 32.000000");
            for (int i = 0; i < sd.getIbi().size(); i++) {
                writer.println(String.format("%s,%s,%s", sd.getAcc().get(0), sd.getAcc().get(1), sd.getAcc().get(2)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            new ZipFile(sessionFile).addFiles(Arrays.asList(edaFile, tempFile, bvpFile, accFile, hrFile, ibiFile, tagFile));
            Toast.makeText(requireContext(), "Session saved to local storage: " + sessionFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (ZipException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error saving file!", Toast.LENGTH_LONG).show();
        }
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
                if (lastAcc < CALIBRATION_SAMPLES) return;

                accel_xLabel.setText(String.format(Locale.getDefault(), "%d", sharedViewModel.getSessionData().getAcc().getLast().get(0)));
                accel_yLabel.setText(String.format(Locale.getDefault(), "%d", sharedViewModel.getSessionData().getAcc().getLast().get(1)));
                accel_zLabel.setText(String.format(Locale.getDefault(), "%d", sharedViewModel.getSessionData().getAcc().getLast().get(2)));
            }
        });
        sharedViewModel.getLastGsr().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastGsr) {
                if (lastGsr < CALIBRATION_SAMPLES) return;

                edaLabel.setText(String.format(Locale.getDefault(), "%.0f", sharedViewModel.getSessionData().getGsr().getLast()));
            }
        });
        sharedViewModel.getLastIbi().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastIbi) {
                if (lastIbi < CALIBRATION_SAMPLES) return;

                ibiLabel.setText(String.format(Locale.getDefault(), "%.0f", sharedViewModel.getSessionData().getIbi().getLast()));
            }
        });
        sharedViewModel.getLastTemp().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastTemp) {
                if (lastTemp < CALIBRATION_SAMPLES) return;

                temperatureLabel.setText(String.format(Locale.getDefault(), "%.0f", sharedViewModel.getSessionData().getTemp().getLast()));
            }
        });
        sharedViewModel.getLastBvp().observe(owner, new Observer<Integer>() {
            @Override
            public void onChanged(Integer lastBvp) {
                if (lastBvp < CALIBRATION_SAMPLES) return;

                bvpLabel.setText(String.format(Locale.getDefault(), "%.0f", sharedViewModel.getSessionData().getBvp().getLast()));
            }
        });
    }

}
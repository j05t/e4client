package com.jstappdev.e4client;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.jstappdev.e4client.data.E4Session;
import com.jstappdev.e4client.util.Utils;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SharedViewModel extends ViewModel implements EmpaDataDelegate {

    private MutableLiveData<Boolean> onWrist;
    private MutableLiveData<String> sessionStatus;
    private MutableLiveData<Boolean> isConnected;
    private MutableLiveData<String> deviceName;
    private MutableLiveData<Float> battery;
    private MutableLiveData<Double> tag;

    // we just keep track of the index of the last sensor reading
    private MutableLiveData<Integer> lastAcc;
    private MutableLiveData<Integer> lastBvp;
    private MutableLiveData<Integer> lastGsr;
    private MutableLiveData<Integer> lastIbi;
    private MutableLiveData<Integer> lastTemp;

    private MutableLiveData<String> currentStatus;
    private List<String> uploadedSessionIDs;
    private ArrayList<E4Session> e4Sessions = new ArrayList<>();
    private String username;
    private String password;
    private String userId;

    private File edaFile;
    private File tempFile;
    private File bvpFile;
    private File hrFile;
    private File tagFile;
    private File ibiFile;
    private File accFile;

    private PrintWriter edaWriter;
    private PrintWriter tempWriter;
    private PrintWriter bvpWriter;
    private PrintWriter hrWriter;
    private PrintWriter ibiWriter;
    private PrintWriter accWriter;
    private PrintWriter tagWriter;

    private double firstIbiTimestamp;
    private long initialTime;

    public SharedViewModel() {
        onWrist = new MutableLiveData<>();
        sessionStatus = new MutableLiveData<>();
        isConnected = new MutableLiveData<>(false);
        deviceName = new MutableLiveData<>();
        battery = new MutableLiveData<>();

        lastAcc = new MutableLiveData<>(0);
        lastBvp = new MutableLiveData<>(0);
        lastGsr = new MutableLiveData<>(0);
        lastIbi = new MutableLiveData<>(0);
        lastTemp = new MutableLiveData<>(0);
        tag = new MutableLiveData<>();

        currentStatus = new MutableLiveData<>();

        uploadedSessionIDs = new ArrayList<>();
    }

    public MutableLiveData<String> getCurrentStatus() {
        return currentStatus;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public MutableLiveData<Boolean> getIsConnected() {
        return isConnected;
    }

    void setIsConnected(boolean isConnected) {
        this.isConnected.postValue(isConnected);

        if (isConnected) {
            connected();
        } else {
            if (lastGsr.getValue() > 0)
                saveSession();
        }
    }

    public MutableLiveData<Integer> getLastAcc() {
        return lastAcc;
    }

    public MutableLiveData<Integer> getLastBvp() {
        return lastBvp;
    }

    public MutableLiveData<Float> getBattery() {
        return battery;
    }

    public MutableLiveData<Integer> getLastGsr() {
        return lastGsr;
    }

    public MutableLiveData<Integer> getLastIbi() {
        return lastIbi;
    }

    public MutableLiveData<Integer> getLastTemp() {
        return lastTemp;
    }

    public MutableLiveData<Double> getTag() {
        return tag;
    }

    public LiveData<Boolean> getOnWrist() {
        return onWrist;
    }

    void setOnWrist(Boolean onWrist) {
        this.onWrist.postValue(onWrist);
    }

    public LiveData<String> getSessionStatus() {
        return sessionStatus;
    }

    void setSessionStatus(String name) {
        this.sessionStatus.postValue(name);
    }

    public LiveData<String> getDeviceName() {
        return deviceName;
    }

    void setDeviceName(String deviceName) {
        this.deviceName.postValue(deviceName);
    }

    //@Override
    void didUpdateOnWristStatus(@EmpaSensorStatus final int status) {
        if (status == EmpaSensorStatus.ON_WRIST) {
            onWrist.postValue(true);
        } else {
            onWrist.postValue(false);
        }
    }

    @Override
    public void didReceiveBatteryLevel(float battery, double timestamp) {
        this.battery.postValue(battery);
    }

    @Override
    public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
        if (lastAcc.getValue() == 0) {
            accWriter.println(String.format("%s, %s, %s", timestamp, timestamp, timestamp));
            accWriter.println("32.000000, 32.000000, 32.000000");
        }
        accWriter.println(String.format("%s,%s,%s", x, y, z));
        lastAcc.postValue(lastAcc.getValue() + 1);
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        if (lastBvp.getValue() == 0) {
            bvpWriter.println(timestamp);
            bvpWriter.println("4.000000");
        }
        bvpWriter.println(bvp);
        lastBvp.postValue(lastBvp.getValue() + 1);
    }

    @Override
    public void didReceiveGSR(float gsr, double timestamp) {
        if (lastGsr.getValue() == 0) {
            edaWriter.println(timestamp);
            edaWriter.println("4.000000");
        }
        edaWriter.println(gsr);
        lastGsr.postValue(lastGsr.getValue() + 1);
    }

    // HR is calculated from IBI
    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        if (lastIbi.getValue() == 0) {
            hrWriter.println(timestamp);
            hrWriter.println("1.000000");

            ibiWriter.println(timestamp + ", IBI");
            firstIbiTimestamp = timestamp;
        }

        double time = timestamp - firstIbiTimestamp;
        ibiWriter.println(String.format("%s,%s", time, ibi));

        // fixme : should be calculated and written every second
        final float currentHr = 60.0f / ibi;
        hrWriter.println(currentHr);

        lastIbi.postValue(lastIbi.getValue() + 1);
    }

    @Override
    public void didReceiveTemperature(float temp, double timestamp) {
        if (lastTemp.getValue() == 0) {
            tempWriter.println(timestamp);
            tempWriter.println("4.000000");
        }
        tempWriter.println(temp);
        lastTemp.postValue(lastTemp.getValue() + 1);
    }

    @Override
    public void didReceiveTag(double timestamp) {
        tagWriter.println(timestamp);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ArrayList<E4Session> getE4Sessions() {
        return e4Sessions;
    }

    public void setE4Sessions(ArrayList<E4Session> e4Sessions) {
        this.e4Sessions = e4Sessions;
    }

    public List<String> getUploadedSessionIDs() {
        return uploadedSessionIDs;
    }


    private synchronized void connected() {

        Utils.trimCache(MainActivity.context);

        initialTime = Utils.getCurrentTimestamp();

        final String basePath = MainActivity.context.getCacheDir().getPath() + "/";

        edaFile = new File(basePath + "EDA.csv");
        tempFile = new File(basePath + "TEMP.csv");
        bvpFile = new File(basePath + "BVP.csv");
        hrFile = new File(basePath + "HR.csv");
        tagFile = new File(basePath + "tags.csv");
        ibiFile = new File(basePath + "IBI.csv");
        accFile = new File(basePath + "ACC.csv");

        try {
            edaWriter = new PrintWriter(new FileWriter(edaFile));
            tempWriter = new PrintWriter(new FileWriter(tempFile));
            bvpWriter = new PrintWriter(new FileWriter(bvpFile));
            hrWriter = new PrintWriter(new FileWriter(hrFile));
            ibiWriter = new PrintWriter(new FileWriter(ibiFile));
            accWriter = new PrintWriter(new FileWriter(accFile));
            tagWriter = new PrintWriter(new FileWriter(tagFile));
        } catch (IOException e) {
            currentStatus.postValue("Error creating file writers!");
            e.printStackTrace();
        }
    }

    private synchronized void saveSession() {
        final E4Session e4Session = new E4Session("id", initialTime / 1000, Utils.getCurrentTimestamp() / 1000 - initialTime / 1000, "E4", "label", "device", "0", "0");
        final File sessionFile = new File(MainActivity.context.getFilesDir(), e4Session.getZIPFilename());

        Log.d(MainActivity.TAG, "Saving as " + e4Session.getZIPFilename());

        try {
            new ZipFile(sessionFile).addFiles(Arrays.asList(edaFile, tempFile, bvpFile, accFile, hrFile, ibiFile, tagFile));
            currentStatus.postValue("Session saved to local storage: " + sessionFile.getAbsolutePath());
        } catch (ZipException e) {
            currentStatus.postValue("Error creating file: " + sessionFile.getAbsolutePath());
            e.printStackTrace();
        }
    }
}
package com.jstappdev.e4client;

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
    private MutableLiveData<Integer> currentAccX;
    private MutableLiveData<Integer> currentAccY;
    private MutableLiveData<Integer> currentAccZ;
    private MutableLiveData<Float> currentBvp;
    private MutableLiveData<Float> currentHr;
    private MutableLiveData<Float> currentGsr;
    private MutableLiveData<Float> currentIbi;
    private MutableLiveData<Float> currentTemp;
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

    private PrintWriter gsrWriter;
    private PrintWriter tempWriter;
    private PrintWriter bvpWriter;
    private PrintWriter hrWriter;
    private PrintWriter ibiWriter;
    private PrintWriter accWriter;
    private PrintWriter tagWriter;

    private double firstIbiTimestamp;
    private long timeConnected;

    private boolean tempWritten;
    private boolean accWritten;
    private boolean bvpWritten;
    private boolean hrWritten;
    private boolean ibiWritten;
    private boolean gsrWritten;

    public SharedViewModel() {
        uploadedSessionIDs = new ArrayList<>();

        onWrist = new MutableLiveData<>();
        sessionStatus = new MutableLiveData<>();
        isConnected = new MutableLiveData<>(false);
        deviceName = new MutableLiveData<>();
        battery = new MutableLiveData<>();
        currentStatus = new MutableLiveData<>();
        currentAccX = new MutableLiveData<Integer>();
        currentAccY = new MutableLiveData<Integer>();
        currentAccZ = new MutableLiveData<Integer>();
        currentBvp = new MutableLiveData<Float>();
        currentHr = new MutableLiveData<Float>();
        currentGsr = new MutableLiveData<Float>();
        currentIbi = new MutableLiveData<Float>();
        currentTemp = new MutableLiveData<Float>();
        tag = new MutableLiveData<Double>();
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
        if (isConnected) connected();
        else saveSession();

        this.isConnected.postValue(isConnected);
    }

    public MutableLiveData<Integer> getCurrentAccX() {
        return currentAccX;
    }

    public MutableLiveData<Integer> getCurrentAccY() {
        return currentAccY;
    }

    public MutableLiveData<Integer> getCurrentAccZ() {
        return currentAccZ;
    }

    public MutableLiveData<Float> getCurrentBvp() {
        return currentBvp;
    }

    public MutableLiveData<Float> getCurrentHr() {
        return currentHr;
    }

    public MutableLiveData<Float> getBattery() {
        return battery;
    }

    public MutableLiveData<Float> getCurrentGsr() {
        return currentGsr;
    }

    public MutableLiveData<Float> getCurrentIbi() {
        return currentIbi;
    }

    public MutableLiveData<Float> getCurrentTemp() {
        return currentTemp;
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
        if(!accWritten) {
            accWritten = true;
            accWriter.println(String.format("%s, %s, %s", timestamp, timestamp, timestamp));
            accWriter.println("32.000000, 32.000000, 32.000000");
        }
        accWriter.println(String.format("%s,%s,%s", x, y, z));
        currentAccX.postValue(x);
        currentAccY.postValue(y);
        currentAccZ.postValue(z);
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        if(!bvpWritten) {
            bvpWritten = true;
            bvpWriter.println(timestamp);
            bvpWriter.println("4.000000");
        }
        bvpWriter.println(bvp);
        currentBvp.postValue(bvp);
    }

    @Override
    public void didReceiveGSR(float gsr, double timestamp) {
        if(!gsrWritten) {
            gsrWritten = true;
            gsrWriter.println(timestamp);
            gsrWriter.println("4.000000");
        }
        gsrWriter.println(gsr);
        currentGsr.postValue(gsr);
    }

    // HR is calculated from IBI
    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        if (!ibiWritten) {
            ibiWritten = true;

            firstIbiTimestamp = timestamp;
            ibiWriter.println(timestamp + ", IBI");

            hrWriter.println(timestamp);
            hrWriter.println("1.000000");
        }

        // fixme: timeConnected?
        double time = timestamp - firstIbiTimestamp;
        ibiWriter.println(String.format("%s,%s", time, ibi));

        // fixme : should be calculated and written once per second
        final float hr = 60.0f / ibi;
        hrWriter.println(hr);

        currentIbi.postValue(ibi);
        currentHr.postValue(hr);
    }

    @Override
    public void didReceiveTemperature(float temp, double timestamp) {
        if(!tempWritten) {
            tempWriter.println(timestamp);
            tempWriter.println("4.000000");
            tempWritten = true;
        }
        tempWriter.println(temp);
        currentTemp.postValue(temp);
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

        final String basePath = MainActivity.context.getCacheDir().getPath() + "/";

        edaFile = new File(basePath + "EDA.csv");
        tempFile = new File(basePath + "TEMP.csv");
        bvpFile = new File(basePath + "BVP.csv");
        hrFile = new File(basePath + "HR.csv");
        tagFile = new File(basePath + "tags.csv");
        ibiFile = new File(basePath + "IBI.csv");
        accFile = new File(basePath + "ACC.csv");

        timeConnected = Utils.getCurrentTimestamp();

        try {
            tagWriter = new PrintWriter(new FileWriter(tagFile));
            tempWriter = new PrintWriter(new FileWriter(tempFile));
            ibiWriter = new PrintWriter(new FileWriter(ibiFile));
            hrWriter = new PrintWriter(new FileWriter(hrFile));
            gsrWriter = new PrintWriter(new FileWriter(edaFile));
            bvpWriter = new PrintWriter(new FileWriter(bvpFile));
            accWriter = new PrintWriter(new FileWriter(accFile));
        } catch (IOException e) {
            currentStatus.postValue("Error creating file: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private synchronized void saveSession() {
        final E4Session e4Session = new E4Session("id", timeConnected / 1000, Utils.getCurrentTimestamp() / 1000 - timeConnected / 1000, "E4", "label", "device", "0", "0");
        final File sessionFile = new File(MainActivity.context.getFilesDir(), e4Session.getZIPFilename());

        gsrWriter.close();
        tempWriter.close();
        tagWriter.close();
        bvpWriter.close();
        accWriter.close();
        hrWriter.close();
        ibiWriter.close();

        try {
            new ZipFile(sessionFile).addFiles(Arrays.asList(edaFile, tempFile, bvpFile, accFile, hrFile, ibiFile, tagFile));
            currentStatus.postValue("Session saved to local storage: " + sessionFile.getAbsolutePath());
        } catch (ZipException e) {
            currentStatus.postValue("Error creating ZIP file: " + sessionFile.getAbsolutePath());
            e.printStackTrace();
        }
    }


}
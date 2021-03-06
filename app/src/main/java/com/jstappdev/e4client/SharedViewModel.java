package com.jstappdev.e4client;

import android.annotation.SuppressLint;

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
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SharedViewModel extends ViewModel implements EmpaDataDelegate {

    private final static double timezoneOffset = TimeZone.getDefault().getRawOffset() / 1000d;

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
    private MutableLiveData<Float> currentAccMag;
    private MutableLiveData<String> currentStatus;

    private MutableLiveData<Boolean> isLoading;
    private MutableLiveData<Integer> loadingProgress;

    private List<String> uploadedSessionIDs;
    private ArrayList<E4Session> e4Sessions = new ArrayList<>();
    private String username;
    private String password;
    private String userId;
    private File filesDir;

    private File edaFile;
    private File tempFile;
    private File bvpFile;
    private File hrFile;
    private File tagFile;
    private File ibiFile;
    private File accFile;
    private File tagDescriptionFile;

    private PrintWriter gsrWriter;
    private PrintWriter tempWriter;
    private PrintWriter bvpWriter;
    private PrintWriter hrWriter;
    private PrintWriter ibiWriter;
    private PrintWriter accWriter;
    private PrintWriter tagWriter;
    private PrintWriter tagDescriptionWriter;

    private double firstIbiTimestamp;
    private long timeConnected;
    private boolean tempWritten;
    private boolean accWritten;
    private boolean bvpWritten;
    private boolean ibiWritten;
    private boolean gsrWritten;
    private String apiKey;

    private ScheduledExecutorService scheduler;
    private float averageHr = 0;

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
        currentAccMag = new MutableLiveData<Float>();
        tag = new MutableLiveData<Double>();

        isLoading = new MutableLiveData<Boolean>(false);
        loadingProgress = new MutableLiveData<Integer>(0);
    }

    long getTimeConnected() {
        return timeConnected;
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

    public LiveData<Boolean> getIsConnected() {
        return isConnected;
    }

    synchronized void setIsConnected(boolean isConnected) {
        //noinspection ConstantConditions
        if (isConnected & !this.isConnected.getValue()) connected();

        this.isConnected.postValue(isConnected);
    }

    public LiveData<Integer> getCurrentAccX() {
        return currentAccX;
    }

    public LiveData<Integer> getCurrentAccY() {
        return currentAccY;
    }

    public LiveData<Integer> getCurrentAccZ() {
        return currentAccZ;
    }

    public LiveData<Float> getCurrentBvp() {
        return currentBvp;
    }

    public LiveData<Float> getCurrentHr() {
        return currentHr;
    }

    public LiveData<Float> getBattery() {
        return battery;
    }

    public LiveData<Float> getCurrentGsr() {
        return currentGsr;
    }

    public LiveData<Float> getCurrentIbi() {
        return currentIbi;
    }

    public LiveData<Float> getCurrentTemp() {
        return currentTemp;
    }

    public LiveData<Double> getTag() {
        return tag;
    }

    public LiveData<Float> getCurrentAccMag() {
        return currentAccMag;
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
        if (!accWritten) {
            accWritten = true;
            timestamp += timezoneOffset;
            accWriter.println(String.format("%.6f, %.6f, %.6f", timestamp, timestamp, timestamp));
            accWriter.println("32.000000, 32.000000, 32.000000");
        }
        accWriter.println(String.format("%d,%d,%d", x, y, z));

        currentAccX.postValue(x);
        currentAccY.postValue(y);
        currentAccZ.postValue(z);
        currentAccMag.postValue(Utils.magnitude(x, y, z));
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        if (!bvpWritten) {
            bvpWritten = true;
            bvpWriter.println(String.format("%.6f", timestamp + timezoneOffset));
            bvpWriter.println("4.000000");
        }
        bvpWriter.println(bvp);
        currentBvp.postValue(bvp);
    }

    @Override
    public void didReceiveGSR(float gsr, double timestamp) {
        if (!gsrWritten) {
            gsrWritten = true;
            gsrWriter.println(String.format("%.6f", timestamp + timezoneOffset));
            gsrWriter.println("4.000000");
        }
        gsrWriter.println(gsr);
        currentGsr.postValue(gsr);
    }

    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        timestamp += timezoneOffset;

        if (!ibiWritten) {
            ibiWritten = true;

            firstIbiTimestamp = timestamp;
            ibiWriter.println(String.format("%.6f, IBI", timestamp));

            hrWriter.println(String.format("%.6f", timestamp));
            hrWriter.println("1.000000");

            // start writing average heart rate from IBI every 1000ms
            // todo: should be calculated from BVP
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate
                    (new Runnable() {
                        public void run() {
                            if (isConnected.getValue()) {
                                if (averageHr != 0) {
                                    hrWriter.println(averageHr);
                                    currentHr.postValue(averageHr);
                                }
                            } else {
                                scheduler.shutdown();
                            }
                        }
                    }, 0, 1000, TimeUnit.MILLISECONDS);

            return;
        }

        final double time = timestamp - firstIbiTimestamp;
        final float hr = 60.0f / ibi;
        averageHr = (float) (averageHr * 0.8 + hr * 0.2);

        ibiWriter.println(String.format("%f,%f", time, ibi));
        currentIbi.postValue(ibi);
    }

    @Override
    public void didReceiveTemperature(float temp, double timestamp) {
        if (!tempWritten) {
            tempWritten = true;
            tempWriter.println(String.format("%.6f", timestamp + timezoneOffset));
            tempWriter.println("4.000000");
        }
        tempWriter.println(temp);
        currentTemp.postValue(temp);
    }

    @Override
    public void didReceiveTag(double timestamp) {
        timestamp += timezoneOffset;

        tagWriter.println(String.format("%.2f", timestamp));
        tag.postValue(timestamp);
    }

    @SuppressLint("DefaultLocale")
    void addTagDescription(double time, String description) {
        // tag may be added after connection is already closed
        //noinspection ConstantConditions
        if (isConnected.getValue())
            tagDescriptionWriter.println(String.format("%.2f,%s", time, description));
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

    public List<String> getUploadedSessionIDs() {
        return uploadedSessionIDs;
    }


    private synchronized void connected() {

        timeConnected = Utils.getCurrentTimestamp();

        final String basePath = filesDir + "/";
        edaFile = new File(basePath + "EDA.csv");
        tempFile = new File(basePath + "TEMP.csv");
        bvpFile = new File(basePath + "BVP.csv");
        hrFile = new File(basePath + "HR.csv");
        tagFile = new File(basePath + "tags.csv");
        ibiFile = new File(basePath + "IBI.csv");
        accFile = new File(basePath + "ACC.csv");
        tagDescriptionFile = new File(basePath + "tags_description.csv");

        try {
            tagWriter = new PrintWriter(new FileWriter(tagFile));
            tagDescriptionWriter = new PrintWriter(new FileWriter(tagDescriptionFile));
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

    synchronized void saveSession() {
        // session description must not include underscores
        final E4Session e4Session = new E4Session("e4c" + timeConnected / 1000000, timeConnected / 1000, Utils.getCurrentTimestamp() / 1000 - timeConnected / 1000, "000", "local", "E4", "0", "0");
        final File sessionFile = new File(filesDir, e4Session.getZIPFilename());

        gsrWriter.close();
        tempWriter.close();
        tagWriter.close();
        bvpWriter.close();
        accWriter.close();
        hrWriter.close();
        ibiWriter.close();
        tagDescriptionWriter.close();

        try {
            new ZipFile(sessionFile).addFiles(Arrays.asList(edaFile, tempFile, bvpFile, accFile, hrFile, ibiFile, tagFile, tagDescriptionFile));
            currentStatus.postValue("Session saved to local storage: " + sessionFile.getAbsolutePath());

            edaFile.delete();
            tempFile.delete();
            bvpFile.delete();
            accFile.delete();
            ibiFile.delete();
            hrFile.delete();
            tagFile.delete();
            tagDescriptionFile.delete();
        } catch (ZipException e) {
            currentStatus.postValue("Error creating ZIP file: " + e.getMessage());
            e.printStackTrace();
        }

        gsrWritten = false;
        tempWritten = false;
        bvpWritten = false;
        accWritten = false;
        ibiWritten = false;
    }

    public void flushFiles() {
        //noinspection ConstantConditions
        if (isConnected.getValue()) {
            gsrWriter.flush();
            tempWriter.flush();
            bvpWriter.flush();
            accWriter.flush();
            ibiWriter.flush();
            hrWriter.flush();
            tagWriter.flush();
            tagDescriptionWriter.flush();
        }
    }

    public LiveData getIsLoading() {
        return isLoading;
    }

    public void setIsLoading(final boolean isLoading) {
        this.isLoading.postValue(isLoading);
    }

    public MutableLiveData<Integer> getLoadingProgress() {
        return loadingProgress;
    }

    public void setLoadingProgress(final int progress) {
        this.loadingProgress.postValue(progress);
    }

    File getFilesDir() {
        return filesDir;
    }

    void setFilesDir(File filesDir) {
        this.filesDir = filesDir;
    }

    public boolean isSessionDownloaded(final E4Session e4Session) {
        return new File(getFilesDir(), e4Session.getZIPFilename()).exists();
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public void setApiKey(String key) {
        this.apiKey = key;
    }
}
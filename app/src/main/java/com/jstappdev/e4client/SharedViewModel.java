package com.jstappdev.e4client;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.jstappdev.e4client.data.E4Session;
import com.jstappdev.e4client.data.E4SessionData;

import java.util.ArrayList;
import java.util.LinkedList;
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

    public SharedViewModel() {
        onWrist = new MutableLiveData<>();
        sessionStatus = new MutableLiveData<>();
        isConnected = new MutableLiveData<>();
        deviceName = new MutableLiveData<>();
        battery = new MutableLiveData<>();

        lastAcc = new MutableLiveData<>();
        lastBvp = new MutableLiveData<>();
        lastGsr = new MutableLiveData<>();
        lastIbi = new MutableLiveData<>();
        lastTemp = new MutableLiveData<>();
        tag = new MutableLiveData<>();

        currentStatus = new MutableLiveData<>();

        isConnected.setValue(false);

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

    @Override
    public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
        LinkedList<Integer> acceleration = new LinkedList<Integer>();
        acceleration.add(x);
        acceleration.add(y);
        acceleration.add(z);
        E4SessionData.getInstance().addAcc(acceleration, timestamp);
        lastAcc.postValue(E4SessionData.getInstance().getAcc().size() - 1);
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        E4SessionData.getInstance().addBvp(bvp, timestamp);
        this.lastBvp.postValue(E4SessionData.getInstance().getBvp().size() - 1);
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
    public void didReceiveGSR(float gsr, double timestamp) {
        E4SessionData.getInstance().addGsr(gsr, timestamp);
        this.lastGsr.postValue(E4SessionData.getInstance().getGsr().size() - 1);
    }

    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        E4SessionData.getInstance().addIbi(ibi, timestamp);
        this.lastIbi.postValue(E4SessionData.getInstance().getIbi().size() - 1);
    }

    @Override
    public void didReceiveTemperature(float temp, double timestamp) {
        E4SessionData.getInstance().addTemp(temp, timestamp);
        this.lastTemp.postValue(E4SessionData.getInstance().getTemp().size() - 1);
    }

    @Override
    public void didReceiveTag(double timestamp) {
        E4SessionData.getInstance().addTag(timestamp);
        this.tag.postValue(timestamp);
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
}
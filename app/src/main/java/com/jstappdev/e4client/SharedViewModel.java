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
    private MutableLiveData<String> status;
    private MutableLiveData<Boolean> isConnected;
    private MutableLiveData<String> deviceName;
    private MutableLiveData<Float> battery;
    private MutableLiveData<Double> tag;

    // we just keep track of the position of the last sensor reading
    private MutableLiveData<Integer> lastAcc;
    private MutableLiveData<Integer> lastBvp;
    private MutableLiveData<Integer> lastGsr;
    private MutableLiveData<Integer> lastIbi;
    private MutableLiveData<Integer> lastTemp;

    public MutableLiveData<String> getSessionStatus() {
        return sessionStatus;
    }

    private MutableLiveData<String> sessionStatus;

    private List<String> uploadedSessionIDs;

    private ArrayList<E4Session> e4Sessions = new ArrayList<>();
    private E4SessionData e4SessionData;

    private String username;
    private String password;
    private String userId;

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


    public SharedViewModel() {
        e4SessionData = E4SessionData.getInstance();

        onWrist = new MutableLiveData<>();
        status = new MutableLiveData<>();
        isConnected = new MutableLiveData<>();
        deviceName = new MutableLiveData<>();
        battery = new MutableLiveData<>();

        lastAcc = new MutableLiveData<>();
        lastBvp = new MutableLiveData<>();
        lastGsr = new MutableLiveData<>();
        lastIbi = new MutableLiveData<>();
        lastTemp = new MutableLiveData<>();
        tag = new MutableLiveData<>();

        sessionStatus = new MutableLiveData<>();

        isConnected.setValue(false);

        uploadedSessionIDs = new ArrayList<>();
    }

    public MutableLiveData<Boolean> getIsConnected() {
        return isConnected;
    }

    void setIsConnected(boolean isConnected) {
        if (isConnected) E4SessionData.clear();

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

    public LiveData<String> getStatus() {
        return status;
    }

    void setStatus(String name) {
        this.status.postValue(name);
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
        e4SessionData.addAcc(acceleration, timestamp);
        lastAcc.postValue(e4SessionData.getAcc().size() - 1);
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        e4SessionData.addBvp(bvp, timestamp);
        this.lastBvp.postValue(e4SessionData.getBvp().size() - 1);
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
        e4SessionData.addGsr(gsr, timestamp);
        this.lastGsr.postValue(e4SessionData.getGsr().size() - 1);
    }

    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        e4SessionData.addIbi(ibi, timestamp);
        this.lastIbi.postValue(e4SessionData.getIbi().size() - 1);
    }

    @Override
    public void didReceiveTemperature(float temp, double timestamp) {
        e4SessionData.addTemp(temp, timestamp);
        this.lastTemp.postValue(e4SessionData.getTemp().size() - 1);
    }

    @Override
    public void didReceiveTag(double timestamp) {
        e4SessionData.addTag(timestamp);
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

    public E4SessionData getSessionData() {
        return this.e4SessionData;
    }

    public List<String> getUploadedSessionIDs() {
        return uploadedSessionIDs;
    }
}
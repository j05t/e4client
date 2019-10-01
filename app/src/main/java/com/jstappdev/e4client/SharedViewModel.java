package com.jstappdev.e4client;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.jstappdev.e4client.data.Session;
import com.jstappdev.e4client.data.SessionData;

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


    private List<Session> sessions = new ArrayList<>();
    private SessionData sessionData;

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
        sessionData = SessionData.getInstance();

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
        tag = new MutableLiveData<Double>();

    }

    public MutableLiveData<Boolean> getIsConnected() {
        return isConnected;
    }

    public void setIsConnected(boolean isConnected) {
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

    public void setOnWrist(Boolean onWrist) {
        this.onWrist.postValue(onWrist);
    }

    public LiveData<String> getStatus() {
        return status;
    }

    public void setStatus(String name) {
        this.status.postValue(name);
    }

    public LiveData<String> getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName.postValue(deviceName);
    }

    @Override
    public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
        LinkedList<Integer> acceleration = new LinkedList<Integer>();
        acceleration.add(x);
        acceleration.add(y);
        acceleration.add(z);
        sessionData.addAcc(acceleration, timestamp);
        lastAcc.postValue(sessionData.getAcc().size() - 1);
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        sessionData.addBvp(bvp, timestamp);
        this.lastBvp.postValue(sessionData.getBvp().size() - 1);
    }

    //@Override
    public void didUpdateOnWristStatus(@EmpaSensorStatus final int status) {

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
        sessionData.addGsr(gsr, timestamp);
        this.lastGsr.postValue(sessionData.getGsr().size() - 1);
    }

    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        sessionData.addIbi(ibi, timestamp);
        this.lastIbi.postValue(sessionData.getIbi().size() - 1);
    }

    @Override
    public void didReceiveTemperature(float temp, double timestamp) {
        sessionData.addTemp(temp, timestamp);
        this.lastTemp.postValue(sessionData.getTemp().size() - 1);
    }

    @Override
    public void didReceiveTag(double timestamp) {
        sessionData.addTag(timestamp);
        this.tag.postValue(timestamp);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<Session> getSessions() {
        return sessions;
    }

    public void setSessions(List<Session> sessions) {
        this.sessions = sessions;
    }

}
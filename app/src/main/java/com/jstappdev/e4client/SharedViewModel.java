package com.jstappdev.e4client;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;

import java.util.ArrayList;
import java.util.List;

public class SharedViewModel extends ViewModel implements EmpaDataDelegate {

    private MutableLiveData<Boolean> onWrist;
    private MutableLiveData<String> status;
    private MutableLiveData<Boolean> isConnected;
    private MutableLiveData<String> deviceName;
    private MutableLiveData<Float> battery;

    private MutableLiveData<List<Integer>> acc;
    private MutableLiveData<Float> bvp;
    private MutableLiveData<Float> gsr;
    private MutableLiveData<Float> ibi;
    private MutableLiveData<Float> temp;

    private SessionData sessionData;

    public SharedViewModel() {
        sessionData = SessionData.getInstance();

        onWrist = new MutableLiveData<Boolean>();
        status = new MutableLiveData<String>();
        isConnected = new MutableLiveData<Boolean>();
        deviceName = new MutableLiveData<String>();
        battery = new MutableLiveData<Float>();

        acc = new MutableLiveData<List<Integer>>();
        bvp = new MutableLiveData<Float>();
        gsr = new MutableLiveData<Float>();
        ibi = new MutableLiveData<Float>();
        temp = new MutableLiveData<Float>();
    }

    public MutableLiveData<Boolean> getIsConnected() {
        return isConnected;
    }

    public void setIsConnected(boolean isConnected) {
        this.isConnected.postValue(isConnected);
    }

    public MutableLiveData<List<Integer>> getAcc() {
        return acc;
    }

    public MutableLiveData<Float> getBvp() {
        return bvp;
    }

    public MutableLiveData<Float> getBattery() {
        return battery;
    }

    public MutableLiveData<Float> getGsr() {
        return gsr;
    }

    public MutableLiveData<Float> getIbi() {
        return ibi;
    }

    public MutableLiveData<Float> getTemp() {
        return temp;
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

    @Override
    public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
        List<Integer> acceleration = new ArrayList<Integer>();
        acceleration.add(x);
        acceleration.add(y);
        acceleration.add(z);
        acc.postValue(new ArrayList<Integer>(acceleration));
        sessionData.addAcc(acceleration);
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        this.bvp.postValue(bvp);
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
        this.gsr.postValue(gsr);
        sessionData.addGsr(gsr);
    }

    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        this.ibi.postValue(ibi);
        sessionData.addIbi(ibi);
    }

    @Override
    public void didReceiveTemperature(float temp, double timestamp) {
        this.temp.postValue(temp);
        sessionData.addTemp(temp);
    }

    @Override
    public void didReceiveTag(double timestamp) {
        sessionData.addTag(timestamp);
    }


    public LiveData<String> getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName.postValue(deviceName);
    }
}
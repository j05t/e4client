package com.jstappdev.e4client;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;

import java.util.ArrayList;
import java.util.List;

public class SharedViewModel extends ViewModel implements EmpaDataDelegate {

    private MutableLiveData<String> onWrist;
    private MutableLiveData<String> status;

    private MutableLiveData<String> deviceName;
    private MutableLiveData<List<Integer>> acc;
    private MutableLiveData<Float> bvp;
    private MutableLiveData<Float> battery;
    private MutableLiveData<Float> gsr;
    private MutableLiveData<Float> ibi;
    private MutableLiveData<Float> temp;

    public SharedViewModel() {
        onWrist = new MutableLiveData<String>();
        status = new MutableLiveData<String>();

        deviceName = new MutableLiveData<String>();
        acc = new MutableLiveData<List<Integer>>();

        bvp = new MutableLiveData<Float>();
        gsr = new MutableLiveData<Float>();
        ibi = new MutableLiveData<Float>();
        battery = new MutableLiveData<Float>();
        temp = new MutableLiveData<Float>();
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

    public LiveData<String> getOnWrist() {
        return onWrist;
    }

    public void setOnWrist(String onWrist) {
        this.onWrist.setValue(onWrist);
    }

    public LiveData<String> getStatus() {
        return status;
    }

    public void setStatus(String name) {
        this.status.setValue(name);
    }

    @Override
    public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
        List<Integer> acceleration = new ArrayList<Integer>();
        acceleration.add(x);
        acceleration.add(y);
        acceleration.add(z);
        acc.setValue(new ArrayList<Integer>(acceleration));
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        this.bvp.setValue(bvp);
    }

    //@Override
    public void didUpdateOnWristStatus(@EmpaSensorStatus final int status) {

        if (status == EmpaSensorStatus.ON_WRIST) {

            onWrist.setValue("ON WRIST");
        } else {

            onWrist.setValue("NOT ON WRIST");
        }
    }

    @Override
    public void didReceiveBatteryLevel(float battery, double timestamp) {
        this.battery.setValue(battery);
    }

    @Override
    public void didReceiveGSR(float gsr, double timestamp) {
        //updateLabel(edaLabel, "" + gsr);
        this.gsr.setValue(gsr);
    }

    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        //updateLabel(ibiLabel, "" + ibi);
        this.ibi.setValue(ibi);
    }

    @Override
    public void didReceiveTemperature(float temp, double timestamp) {
        //updateLabel(temperatureLabel, "" + temp);
        this.temp.setValue(temp);
    }

    @Override
    public void didReceiveTag(double timestamp) {

    }


    public LiveData<String> getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName.setValue(deviceName);
    }
}
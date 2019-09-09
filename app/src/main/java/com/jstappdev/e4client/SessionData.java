package com.jstappdev.e4client;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;


public class SessionData {

    private static SessionData INSTANCE;

    private long initialTime;

    private LinkedList<List<Integer>> acc;
    private LinkedList<Float> bvp;
    private LinkedList<Float> gsr;
    private LinkedList<Float> ibi;
    private LinkedList<Float> temp;
    private LinkedList<Double> tags;

    private LinkedList<Double> accTimestamps;
    private LinkedList<Double> bvpTimestamps;
    private LinkedList<Double> gsrTimestamps;
    private LinkedList<Double> ibiTimestamps;
    private LinkedList<Double> tempTimestamps;
    private LinkedList<Double> hrTimestamps;

    private Deque<Float> lastIbis = new LinkedList<Float>();
    private float hr = 0f;

    private SessionData() {
        acc = new LinkedList<>();
        bvp = new LinkedList<>();
        gsr = new LinkedList<>();
        ibi = new LinkedList<>();
        temp = new LinkedList<>();
        tags = new LinkedList<>();

        accTimestamps = new LinkedList<>();
        bvpTimestamps = new LinkedList<>();
        gsrTimestamps = new LinkedList<>();
        ibiTimestamps = new LinkedList<>();
        tempTimestamps = new LinkedList<>();
        hrTimestamps = new LinkedList<>();
    }

    public static SessionData getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SessionData();
        }
        return INSTANCE;
    }

    public float getHr() {
        return hr;
    }

    public LinkedList<Double> getAccTimestamps() {
        return accTimestamps;
    }

    public LinkedList<Double> getBvpTimestamps() {
        return bvpTimestamps;
    }

    public void setBvpTimestamps(LinkedList<Double> bvpTimestamps) {
        this.bvpTimestamps = bvpTimestamps;
    }

    public LinkedList<Double> getGsrTimestamps() {
        return gsrTimestamps;
    }

    public void setGsrTimestamps(LinkedList<Double> gsrTimestamps) {
        this.gsrTimestamps = gsrTimestamps;
    }

    public LinkedList<Double> getIbiTimestamps() {
        return ibiTimestamps;
    }

    public void setIbiTimestamps(LinkedList<Double> ibiTimestamps) {
        this.ibiTimestamps = ibiTimestamps;
    }

    public LinkedList<Double> getTempTimestamps() {
        return tempTimestamps;
    }

    public void setTempTimestamps(LinkedList<Double> tempTimestamps) {
        this.tempTimestamps = tempTimestamps;
    }

    public LinkedList<Double> getHrTimestamps() {
        return hrTimestamps;
    }

    public void setHrTimestamps(LinkedList<Double> hrTimestamps) {
        this.hrTimestamps = hrTimestamps;
    }

    public long getInitialTime() {
        return initialTime;
    }

    public void setInitialTime(long initialTime) {
        this.initialTime = initialTime;
    }

    public LinkedList<List<Integer>> getAcc() {
        return acc;
    }

    public void setAcc(LinkedList<List<Integer>> acc) {
        this.acc = acc;
    }

    public void addAcc(LinkedList<Integer> acc, double timestamp) {
        this.acc.add(acc);
        this.accTimestamps.add(timestamp);
    }

    public LinkedList<Float> getBvp() {
        return bvp;
    }

    public void setBvp(LinkedList<Float> bvp) {
        this.bvp = bvp;
    }

    public void addBvp(float bvp, double timestamp) {
        this.bvp.add(bvp);
        this.bvpTimestamps.add(timestamp);
    }

    public LinkedList<Float> getGsr() {
        return gsr;
    }

    public void setGsr(LinkedList<Float> gsr) {
        this.gsr = gsr;
    }

    public void addGsr(float gsr, double timestamp) {
        this.gsr.add(gsr);
        this.gsrTimestamps.add(timestamp);
    }

    public LinkedList<Float> getIbi() {
        return ibi;
    }

    public void setIbi(LinkedList<Float> ibi) {
        this.ibi = ibi;
    }

    public void addIbi(float ibi, double timestamp) {

        // last 20 ibis used to calulate heart rate
        if (lastIbis.size() > 20)
            lastIbis.removeFirst();

        lastIbis.addLast(ibi);

        // update average heart rate
        // todo: calculate better estimate of current heart rate
        if (lastIbis.size() > 5) {
            float cur_hr = 0f;

            for (float lastIbi : lastIbis) {
                cur_hr += lastIbi;
            }
            cur_hr /= lastIbis.size();
            hr = cur_hr;
        }

        this.ibi.add(ibi);
        this.ibiTimestamps.add(timestamp);
    }

    public LinkedList<Float> getTemp() {
        return temp;
    }

    public void setTemp(LinkedList<Float> temp) {
        this.temp = temp;
    }

    public void addTemp(float temp, double timestamp) {
        this.temp.add(temp);
        this.tempTimestamps.add(timestamp);
    }

    public LinkedList<Double> getTags() {
        return tags;
    }

    public void setTags(LinkedList<Double> tags) {
        this.tags = tags;
    }

    public void addTag(double tag) {
        this.tags.add(tag);
    }
}

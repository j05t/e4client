package com.jstappdev.e4client.data;

import java.util.LinkedList;
import java.util.List;


public class E4SessionData {

    private static E4SessionData INSTANCE;

    private long initialTime;

    private String description;

    private LinkedList<List<Integer>> acc;
    private LinkedList<Float> bvp;
    private LinkedList<Float> gsr;
    private LinkedList<Float> ibi;
    private LinkedList<Float> temp;
    private LinkedList<Float> hr;
    private LinkedList<Double> tags;

    private LinkedList<Double> accTimestamps;
    private LinkedList<Double> bvpTimestamps;
    private LinkedList<Double> gsrTimestamps;
    private LinkedList<Double> ibiTimestamps;
    private LinkedList<Double> tempTimestamps;
    private LinkedList<Double> hrTimestamps;


    private E4SessionData() {
        acc = new LinkedList<>();
        bvp = new LinkedList<>();
        gsr = new LinkedList<>();
        ibi = new LinkedList<>();
        temp = new LinkedList<>();
        hr = new LinkedList<>();
        tags = new LinkedList<>();

        accTimestamps = new LinkedList<>();
        bvpTimestamps = new LinkedList<>();
        gsrTimestamps = new LinkedList<>();
        ibiTimestamps = new LinkedList<>();
        hrTimestamps = new LinkedList<>();
        tempTimestamps = new LinkedList<>();
    }

    public synchronized static E4SessionData getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new E4SessionData();
        }
        return INSTANCE;
    }

    public static void clear() {
        INSTANCE = new E4SessionData();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public LinkedList<Float> getHr() {
        return this.hr;
    }

    public void setHr(LinkedList<Float> hr) {
        this.hr = hr;
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

        final float currentHr = 60.0f / ibi;

        this.ibi.add(ibi);
        this.ibiTimestamps.add(timestamp);

        this.hr.add(currentHr);
        this.hrTimestamps.add(timestamp);
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

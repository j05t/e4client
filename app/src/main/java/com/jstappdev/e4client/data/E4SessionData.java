package com.jstappdev.e4client.data;

import java.util.ArrayList;
import java.util.List;


public class E4SessionData {

    private static E4SessionData INSTANCE;

    private long initialTime;

    private String description;

    private ArrayList<List<Integer>> acc;
    private ArrayList<Float> bvp;
    private ArrayList<Float> gsr;
    private ArrayList<Float> ibi;
    private ArrayList<Float> temp;
    private ArrayList<Float> hr;
    private ArrayList<Float> accMag;
    private ArrayList<Double> tags;

    private ArrayList<Double> accTimestamps;
    private ArrayList<Double> accMagTimestamps;
    private ArrayList<Double> bvpTimestamps;
    private ArrayList<Double> gsrTimestamps;
    private ArrayList<Double> ibiTimestamps;
    private ArrayList<Double> tempTimestamps;
    private ArrayList<Double> hrTimestamps;

    private E4SessionData() {
        acc = new ArrayList<>();
        bvp = new ArrayList<>();
        gsr = new ArrayList<>();
        ibi = new ArrayList<>();
        temp = new ArrayList<>();
        hr = new ArrayList<>();
        accMag = new ArrayList<>();
        tags = new ArrayList<>();

        accTimestamps = new ArrayList<>();
        accMagTimestamps = new ArrayList<>();
        bvpTimestamps = new ArrayList<>();
        gsrTimestamps = new ArrayList<>();
        ibiTimestamps = new ArrayList<>();
        hrTimestamps = new ArrayList<>();
        tempTimestamps = new ArrayList<>();
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

    public ArrayList<Double> getAccMagTimestamps() {
        return accMagTimestamps;
    }

    public void setAccMagTimestamps(ArrayList<Double> accMagTimestamps) {
        this.accMagTimestamps = accMagTimestamps;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<Double> getAccTimestamps() {
        return accTimestamps;
    }

    public ArrayList<Double> getBvpTimestamps() {
        return bvpTimestamps;
    }

    public void setBvpTimestamps(ArrayList<Double> bvpTimestamps) {
        this.bvpTimestamps = bvpTimestamps;
    }

    public ArrayList<Double> getGsrTimestamps() {
        return gsrTimestamps;
    }

    public void setGsrTimestamps(ArrayList<Double> gsrTimestamps) {
        this.gsrTimestamps = gsrTimestamps;
    }

    public ArrayList<Double> getIbiTimestamps() {
        return ibiTimestamps;
    }

    public void setIbiTimestamps(ArrayList<Double> ibiTimestamps) {
        this.ibiTimestamps = ibiTimestamps;
    }

    public ArrayList<Double> getTempTimestamps() {
        return tempTimestamps;
    }

    public void setTempTimestamps(ArrayList<Double> tempTimestamps) {
        this.tempTimestamps = tempTimestamps;
    }

    public ArrayList<Double> getHrTimestamps() {
        return hrTimestamps;
    }

    public void setHrTimestamps(ArrayList<Double> hrTimestamps) {
        this.hrTimestamps = hrTimestamps;
    }

    public ArrayList<Float> getHr() {
        return this.hr;
    }

    public void setHr(ArrayList<Float> hr) {
        this.hr = hr;
    }

    public long getInitialTime() {
        return initialTime;
    }

    public void setInitialTime(long initialTime) {
        this.initialTime = initialTime;
    }

    public List<List<Integer>> getAcc() {
        return acc;
    }

    public void setAcc(ArrayList<List<Integer>> acc) {
        this.acc = acc;
    }

    public void addAcc(ArrayList<Integer> acc, double timestamp) {
        this.acc.add(acc);
        this.accTimestamps.add(timestamp);
    }

    public ArrayList<Float> getBvp() {
        return bvp;
    }

    public void setBvp(ArrayList<Float> bvp) {
        this.bvp = bvp;
    }

    public void addBvp(float bvp, double timestamp) {
        this.bvp.add(bvp);
        this.bvpTimestamps.add(timestamp);
    }

    public List<Float> getGsr() {
        return gsr;
    }

    public void setGsr(ArrayList<Float> gsr) {
        this.gsr = gsr;
    }

    public void addGsr(float gsr, double timestamp) {
        this.gsr.add(gsr);
        this.gsrTimestamps.add(timestamp);
    }

    public ArrayList<Float> getIbi() {
        return ibi;
    }

    public void setIbi(ArrayList<Float> ibi) {
        this.ibi = ibi;
    }

    public void addIbi(float ibi, double timestamp) {

        final float currentHr = 60.0f / ibi;

        this.ibi.add(ibi);
        this.ibiTimestamps.add(timestamp);

        this.hr.add(currentHr);
        this.hrTimestamps.add(timestamp);
    }

    public ArrayList<Float> getTemp() {
        return temp;
    }

    public void setTemp(ArrayList<Float> temp) {
        this.temp = temp;
    }

    public void addTemp(float temp, double timestamp) {
        this.temp.add(temp);
        this.tempTimestamps.add(timestamp);
    }

    public ArrayList<Double> getTags() {
        return tags;
    }

    public void setTags(ArrayList<Double> tags) {
        this.tags = tags;
    }

    public void addTag(double tag) {
        this.tags.add(tag);
    }


    public List<Float> getAccMag() {
        return accMag;
    }
}

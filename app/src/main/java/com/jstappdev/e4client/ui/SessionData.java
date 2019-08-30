package com.jstappdev.e4client.ui;

import java.util.LinkedList;
import java.util.List;


public class SessionData {
    private long initialTime;

    private List<List<Integer>> acc;
    private List<Float> bvp;
    private List<Float> gsr;
    private List<Float> ibi;
    private List<Float> temp;
    private List<Long> tags;

    private long accTimestamp;
    private long bvpTimestamp;
    private long gsrTimestamp;
    private long ibiTimestamp;
    private long tempTimestamp;
    private long hrTimestamp;

    public SessionData() {
        acc = new LinkedList<>();
        bvp = new LinkedList<>();
        gsr = new LinkedList<>();
        ibi = new LinkedList<>();
        temp = new LinkedList<>();
        tags = new LinkedList<>();
    }

    public List<Float> getHr() {
        // todo: average heart rate extracted from the BVP signal
        return null;
    }

    public void setAcc(List<List<Integer>> acc) {
        this.acc = acc;
    }

    public void setBvp(List<Float> bvp) {
        this.bvp = bvp;
    }

    public void setGsr(List<Float> gsr) {
        this.gsr = gsr;
    }

    public void setIbi(List<Float> ibi) {
        this.ibi = ibi;
    }

    public void setTemp(List<Float> temp) {
        this.temp = temp;
    }

    public void setTags(List<Long> tags) {
        this.tags = tags;
    }

    public long getAccTimestamp() {
        return accTimestamp;
    }

    public void setAccTimestamp(long accTimestamp) {
        this.accTimestamp = accTimestamp;
    }

    public long getBvpTimestamp() {
        return bvpTimestamp;
    }

    public void setBvpTimestamp(long bvpTimestamp) {
        this.bvpTimestamp = bvpTimestamp;
    }

    public long getGsrTimestamp() {
        return gsrTimestamp;
    }

    public void setGsrTimestamp(long gsrTimestamp) {
        this.gsrTimestamp = gsrTimestamp;
    }

    public long getIbiTimestamp() {
        return ibiTimestamp;
    }

    public void setIbiTimestamp(long ibiTimestamp) {
        this.ibiTimestamp = ibiTimestamp;
    }

    public long getTempTimestamp() {
        return tempTimestamp;
    }

    public void setTempTimestamp(long tempTimestamp) {
        this.tempTimestamp = tempTimestamp;
    }

    public long getHrTimestamp() {
        return hrTimestamp;
    }

    public void setHrTimestamp(long hrTimestamp) {
        this.hrTimestamp = hrTimestamp;
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

    public void addAcc(List<Integer> acc) {
        this.acc.add(acc);
    }

    public List<Float> getBvp() {
        return bvp;
    }

    public void addBvp(float bvp) {
        this.bvp.add(bvp);
    }

    public List<Float> getGsr() {
        return gsr;
    }

    public void addGsr(float gsr) {
        this.gsr.add(gsr);
    }

    public List<Float> getIbi() {
        return ibi;
    }

    public void addIbi(float ibi) {
        this.ibi.add(ibi);
    }

    public List<Float> getTemp() {
        return temp;
    }

    public void addTemp(float temp) {
        this.temp.add(temp);
    }

    public List<Long> getTags() {
        return tags;
    }

    public void addTag(long tag) {
        this.tags.add(tag);
    }
}

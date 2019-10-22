package com.jstappdev.e4client.data;

import android.annotation.SuppressLint;

import com.jstappdev.e4client.Utils;

import java.util.Objects;

public class E4Session implements Comparable<E4Session> {

    private String id;
    private long startTime;
    private long duration;
    private String deviceId;
    private String label;
    private String device;
    private String status;
    private String exit_code;
    private E4SessionData e4SessionData;

    public E4Session(String id, Long startTime, Long duration, String deviceId, String label, String device, String status, String exit_code) {
        this.id = id;
        this.startTime = startTime;
        this.duration = duration;
        this.deviceId = deviceId;
        this.label = label;
        this.device = device;
        this.status = status;
        this.exit_code = exit_code;
    }

    @SuppressLint("DefaultLocale")
    public String getZIPFilename() {
        return String.format("%s_%s_%s_%s_%s_%s_%s_%s.zip", getStartTime(), getId(), getDuration(), getDeviceId(), getLabel(), getDevice(), getStatus(), getExit_code());
    }

    public String getStartDate() {
        return Utils.getDate(this.startTime);
    }

    public String getDurationAsString() {
        return Utils.getDuration(this.duration);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExit_code() {
        return exit_code;
    }

    public void setExit_code(String exit_code) {
        this.exit_code = exit_code;
    }

    public E4SessionData getE4SessionData() {
        return e4SessionData;
    }

    public void setE4SessionData(E4SessionData e4SessionData) {
        this.e4SessionData = e4SessionData;
    }

    @Override
    public String toString() {
        return "Session{" +
                "id='" + id + '\'' +
                ", startTime=" + startTime +
                ", duration=" + duration +
                ", deviceId='" + deviceId + '\'' +
                ", label='" + label + '\'' +
                ", device='" + device + '\'' +
                ", status='" + status + '\'' +
                ", exit_code='" + exit_code + '\'' +
                ", filename='" + getZIPFilename() +
                "\'}";
    }

    @Override
    public int compareTo(E4Session o) {
        return (int) (o.getStartTime() - this.getStartTime());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        E4Session e4Session = (E4Session) o;
        return startTime == e4Session.startTime &&
                duration == e4Session.duration &&
                id.equals(e4Session.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, startTime, duration);
    }
}

package com.jstappdev.e4client.data;

import android.annotation.SuppressLint;

import com.jstappdev.e4client.Utils;

import java.util.Objects;

public class Session implements Comparable<Session> {

    private String id;
    private long startTime;
    private long duration;
    private String deviceId;
    private String label;
    private String device;
    private String status;
    private String exit_code;
    private SessionData sessionData;

    public Session(String id, Long startTime, Long duration, String deviceId, String label, String device, String status, String exit_code) {
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
    public String getFilename() {
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

    public SessionData getSessionData() {
        return sessionData;
    }

    public void setSessionData(SessionData sessionData) {
        this.sessionData = sessionData;
    }

    @Override
    public String toString() {
        return "Session " + getFilename();
    }

    @Override
    public int compareTo(Session o) {
        return (int) (o.getStartTime() - this.getStartTime());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Session session = (Session) o;
        return startTime == session.startTime &&
                duration == session.duration &&
                id.equals(session.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, startTime, duration);
    }
}

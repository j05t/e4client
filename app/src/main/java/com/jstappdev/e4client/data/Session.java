package com.jstappdev.e4client.data;

import com.jstappdev.e4client.Utils;

public class Session {

    private String id;
    private long startTime;
    private long duration;
    private String deviceId;
    private String label;
    private String device;
    private String status;
    private String exit_code;

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

    public String getFilename() {
        return getStartTime() + "_" + getId() + ".zip";
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

}

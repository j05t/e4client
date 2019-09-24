package com.jstappdev.e4client;

public class Session {

    private String id;
    private long start_time;
    private long duration;
    private String device_id;
    private String label;
    private String device;
    private String status;
    private String exit_code;

    public Session(String id, Long start_time, Long duration, String device_id, String label, String device, String status, String exit_code) {
        this.id = id;
        this.start_time = start_time;
        this.duration = duration;
        this.device_id = device_id;
        this.label = label;
        this.device = device;
        this.status = status;
        this.exit_code = exit_code;
    }

    public String getFilename() {
        return "e4_session_" + getStart_time() + "_" + getId() + ".zip";
    }
    public String getStartDate() {
        return Utils.getDate(this.getStart_time());
    }

    public String getDurationAsString() {
        return Utils.getDuration(this.getStart_time());
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

    public long getStart_time() {
        return start_time;
    }

    public void setStart_time(long start_time) {
        this.start_time = start_time;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
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

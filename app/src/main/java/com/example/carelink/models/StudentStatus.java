package com.example.carelink.models;

public class StudentStatus {
    private String uid;
    private String name;
    private int heartRate;
    private String state; // Normal, Warning, Emergency
    private String locationStatus; // At School, Wandering, Home
    private boolean isSos;

    public StudentStatus(String uid, String name) {
        this.uid = uid;
        this.name = name;
        this.heartRate = 0;
        this.state = "Normal";
        this.locationStatus = "Locating...";
        this.isSos = false;
    }

    public String getUid() { return uid; }
    public String getName() { return name; }
    public int getHeartRate() { return heartRate; }
    public String getState() { return state; }
    public String getLocationStatus() { return locationStatus; }
    public boolean isSos() { return isSos; }

    public void setHeartRate(int heartRate) { this.heartRate = heartRate; }
    public void setState(String state) { this.state = state; }
    public void setLocationStatus(String locationStatus) { this.locationStatus = locationStatus; }
    public void setSos(boolean sos) { isSos = sos; }
}
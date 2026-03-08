package com.example.carelink.models;

public class TimeSlot {
    private String timeRange;
    private String duration;
    private double price;
    private Doctor doctor;
    private boolean isAvailable;

    public TimeSlot(String timeRange, String duration, double price,
                    Doctor doctor, boolean isAvailable) {
        this.timeRange = timeRange;
        this.duration = duration;
        this.price = price;
        this.doctor = doctor;
        this.isAvailable = isAvailable;
    }

    public String getTimeRange() { return timeRange; }
    public String getDuration() { return duration; }
    public double getPrice() { return price; }
    public Doctor getDoctor() { return doctor; }
    public boolean isAvailable() { return isAvailable; }
}
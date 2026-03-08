package com.example.carelink.models;

import java.io.Serializable;

public class Appointment implements Serializable {
    private String id;
    private String patientName;
    private String phoneNumber;
    private String clinicName;
    private String clinicAddress;
    private double clinicLat;
    private double clinicLng;
    private String date;
    private String time;
    private String doctorName;
    private String specialty;
    private double price;
    private String status; // "upcoming", "completed", "canceled"
    private String paymentMethod;
    private String bookingTime;

    public Appointment(String id, String patientName, String phoneNumber,
                       String clinicName, String clinicAddress, double clinicLat,
                       double clinicLng, String date, String time, String doctorName,
                       String specialty, double price, String status,
                       String paymentMethod, String bookingTime) {
        this.id = id;
        this.patientName = patientName;
        this.phoneNumber = phoneNumber;
        this.clinicName = clinicName;
        this.clinicAddress = clinicAddress;
        this.clinicLat = clinicLat;
        this.clinicLng = clinicLng;
        this.date = date;
        this.time = time;
        this.doctorName = doctorName;
        this.specialty = specialty;
        this.price = price;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.bookingTime = bookingTime;
    }

    public String getId() { return id; }
    public String getPatientName() { return patientName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getClinicName() { return clinicName; }
    public String getClinicAddress() { return clinicAddress; }
    public double getClinicLat() { return clinicLat; }
    public double getClinicLng() { return clinicLng; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getDoctorName() { return doctorName; }
    public String getSpecialty() { return specialty; }
    public double getPrice() { return price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getBookingTime() { return bookingTime; }
}
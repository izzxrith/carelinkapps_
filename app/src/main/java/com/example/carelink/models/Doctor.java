package com.example.carelink.models;

public class Doctor {
    private String name;
    private String specialty;
    private int imageResource;
    private double rating;

    public Doctor(String name, String specialty, int imageResource, double rating) {
        this.name = name;
        this.specialty = specialty;
        this.imageResource = imageResource;
        this.rating = rating;
    }

    public String getName() { return name; }
    public String getSpecialty() { return specialty; }
    public int getImageResource() { return imageResource; }
    public double getRating() { return rating; }
}
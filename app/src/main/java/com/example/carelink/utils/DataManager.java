package com.example.carelink.utils;

import com.example.carelink.models.Appointment;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    private static DataManager instance;
    private List<Appointment> appointments;

    private DataManager() {
        appointments = new ArrayList<>();
        // Add sample data
        appointments.add(new Appointment("APT001", "John Doe", "0123456789",
                "City Medical Center", "Jalan Universiti, Petaling Jaya", 3.1209, 101.6538,
                "30/01/2026", "10:00 AM", "Dr. Sarah Ahmed", "Cardiologist", 150.0,
                "upcoming", "Touch 'n Go", "Booked on 28/01/2026"));
    }

    public static synchronized DataManager getInstance() {
        if (instance == null) instance = new DataManager();
        return instance;
    }

    public void addAppointment(Appointment appointment) {
        appointments.add(appointment);
    }

    public List<Appointment> getAppointments() {
        return appointments;
    }

    public List<Appointment> getAppointmentsByStatus(String status) {
        List<Appointment> filtered = new ArrayList<>();
        for (Appointment apt : appointments) {
            if (apt.getStatus().equalsIgnoreCase(status)) {
                filtered.add(apt);
            }
        }
        return filtered;
    }
}
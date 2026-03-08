package com.example.carelink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carelink.adapters.DoctorSlotAdapter;
import com.example.carelink.models.Doctor;
import com.example.carelink.models.TimeSlot;
import java.util.ArrayList;
import java.util.List;

public class AvailableSlotsActivity extends AppCompatActivity implements DoctorSlotAdapter.OnSlotClickListener {

    private RecyclerView recyclerView;
    private DoctorSlotAdapter adapter;
    private List<TimeSlot> timeSlots;

    private String patientName, phone, clinicName, clinicAddress, date;
    private double clinicLat, clinicLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_slots);

        patientName = getIntent().getStringExtra("PATIENT_NAME");
        phone = getIntent().getStringExtra("PHONE");
        clinicName = getIntent().getStringExtra("CLINIC_NAME");
        clinicAddress = getIntent().getStringExtra("CLINIC_ADDRESS");
        clinicLat = getIntent().getDoubleExtra("CLINIC_LAT", 0);
        clinicLng = getIntent().getDoubleExtra("CLINIC_LNG", 0);
        date = getIntent().getStringExtra("DATE");

        initViews();
        setupHeader();
        loadTimeSlots();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rvTimeSlots);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupHeader() {
        TextView tvRoute = findViewById(R.id.tvRoute);
        TextView tvDate = findViewById(R.id.tvDate);

        tvRoute.setText("Available Doctors");
        tvDate.setText(date + " | " + clinicName);
    }

    private void loadTimeSlots() {
        timeSlots = new ArrayList<>();

        Doctor doc1 = new Doctor("Dr. Sarah", "Nephrology (Kidney diseases & Care)", R.drawable.ic_doctor_female, 4.8);
        Doctor doc2 = new Doctor("Dr. Rajesh Kumar", "Radiology (X-ray & CT Scan)", R.drawable.ic_doctor_male, 4.5);
        Doctor doc3 = new Doctor("Dr. Lim Mei Hua", "General Surgery", R.drawable.ic_doctor_female, 4.9);
        Doctor doc4 = new Doctor("Dr. Ahmad Abdullah", "Dermatology (Skin & Hair)", R.drawable.ic_doctor_male, 4.7);

        timeSlots.add(new TimeSlot("09:00 AM - 10:00 AM", "1 Hour", 150.00, doc1, true));
        timeSlots.add(new TimeSlot("10:30 AM - 11:30 AM", "1 Hour", 120.00, doc2, true));
        timeSlots.add(new TimeSlot("02:00 PM - 03:00 PM", "1 Hour", 180.00, doc3, true));
        timeSlots.add(new TimeSlot("04:30 PM - 05:30 PM", "1 Hour", 130.00, doc4, true));

        adapter = new DoctorSlotAdapter(timeSlots, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onSlotClick(TimeSlot slot) {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("PATIENT_NAME", patientName);
        intent.putExtra("PHONE", phone);
        intent.putExtra("CLINIC_NAME", clinicName);
        intent.putExtra("CLINIC_ADDRESS", clinicAddress);
        intent.putExtra("CLINIC_LAT", clinicLat);
        intent.putExtra("CLINIC_LNG", clinicLng);
        intent.putExtra("DATE", date);
        intent.putExtra("TIME", slot.getTimeRange());
        intent.putExtra("DOCTOR_NAME", slot.getDoctor().getName());
        intent.putExtra("SPECIALTY", slot.getDoctor().getSpecialty());
        intent.putExtra("PRICE", slot.getPrice());
        startActivity(intent);
    }
}
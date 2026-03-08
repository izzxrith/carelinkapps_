package com.example.carelink;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carelink.adapters.DoctorSlotAdapter;
import com.example.carelink.models.Doctor;
import com.example.carelink.models.TimeSlot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AvailableSlotsActivity extends AppCompatActivity implements DoctorSlotAdapter.OnSlotClickListener {

    private static final String TAG = "AvailableSlotsActivity";
    private RecyclerView recyclerView;
    private DoctorSlotAdapter adapter;
    private List<TimeSlot> timeSlots;
    private FirebaseFirestore db;

    private String patientName, phone, clinicName, clinicAddress, date;
    private double clinicLat, clinicLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_available_slots);

        db = FirebaseFirestore.getInstance();

        patientName = getIntent().getStringExtra("PATIENT_NAME");
        phone = getIntent().getStringExtra("PHONE");
        clinicName = getIntent().getStringExtra("CLINIC_NAME");
        clinicAddress = getIntent().getStringExtra("CLINIC_ADDRESS");
        clinicLat = getIntent().getDoubleExtra("CLINIC_LAT", 0);
        clinicLng = getIntent().getDoubleExtra("CLINIC_LNG", 0);
        date = getIntent().getStringExtra("DATE");

        initViews();
        setupHeader();
        loadTimeSlotsFromFirestore();
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

    private void loadTimeSlotsFromFirestore() {
        timeSlots = new ArrayList<>();
        
        // Fetching from Firestore collection "time_slots"
        db.collection("time_slots")
            .whereEqualTo("date", date) // Only show slots for the selected date
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        try {
                            String time = document.getString("time");
                            String duration = document.getString("duration");
                            Double price = document.getDouble("price");
                            
                            // Get doctor details nested in the document
                            Map<String, Object> docData = (Map<String, Object>) document.get("doctor");
                            String docName = (String) docData.get("name");
                            String specialty = (String) docData.get("specialty");
                            Long imgResLong = (Long) docData.get("imageResource");
                            int imgRes = imgResLong != null ? imgResLong.intValue() : R.drawable.ic_doctor_male;
                            Double rating = (Double) docData.get("rating");

                            Doctor doctor = new Doctor(docName, specialty, imgRes, rating != null ? rating : 0.0);
                            timeSlots.add(new TimeSlot(time, duration, price != null ? price : 0.0, doctor, true));
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing slot: " + e.getMessage());
                        }
                    }
                    
                    // If no slots found in cloud, load local sample data so the screen isn't empty
                    if (timeSlots.isEmpty()) {
                        loadSampleData();
                    }

                    adapter = new DoctorSlotAdapter(timeSlots, this);
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(this, "Error fetching slots", Toast.LENGTH_SHORT).show();
                    loadSampleData();
                }
            });
    }

    private void loadSampleData() {
        Doctor doc1 = new Doctor("Dr. Sarah", "Nephrology", R.drawable.ic_doctor_female, 4.8);
        timeSlots.add(new TimeSlot("09:00 AM - 10:00 AM", "1 Hour", 150.00, doc1, true));
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
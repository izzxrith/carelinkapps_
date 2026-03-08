package com.example.carelink;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carelink.adapters.AppointmentAdapter;
import com.example.carelink.models.Appointment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ScheduleActivity extends AppCompatActivity {

    private static final String TAG = "ScheduleActivity";
    private TextView tvScheduleTitle, tvEmptyState;
    private CardView cardUpcoming, cardCompleted, cardCanceled;
    private TextView tvUpcomingCount, tvCompletedCount, tvCanceledCount;
    private RecyclerView recyclerView;
    private AppointmentAdapter adapter;
    private String currentFilter = "upcoming";

    private LinearLayout navHome, navMessages, navSchedule, navProfile;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<Appointment> allAppointments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupCategoryTabs();
        setupBottomNavigation();
        fetchAppointmentsFromFirestore();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAppointmentsFromFirestore();
    }

    private void initViews() {
        tvScheduleTitle = findViewById(R.id.tvScheduleTitle);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        cardUpcoming = findViewById(R.id.cardUpcoming);
        cardCompleted = findViewById(R.id.cardCompleted);
        cardCanceled = findViewById(R.id.cardCanceled);

        tvUpcomingCount = findViewById(R.id.tvUpcomingCount);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        tvCanceledCount = findViewById(R.id.tvCanceledCount);

        recyclerView = findViewById(R.id.rvAppointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        navHome = findViewById(R.id.navHome);
        navMessages = findViewById(R.id.navMessages);
        navSchedule = findViewById(R.id.navSchedule);
        navProfile = findViewById(R.id.navProfile);

        tvScheduleTitle.setText("Schedule");
    }

    private void setupCategoryTabs() {
        cardUpcoming.setOnClickListener(v -> {
            currentFilter = "upcoming";
            highlightCard(cardUpcoming);
            filterAndDisplay();
        });

        cardCompleted.setOnClickListener(v -> {
            currentFilter = "completed";
            highlightCard(cardCompleted);
            filterAndDisplay();
        });

        cardCanceled.setOnClickListener(v -> {
            currentFilter = "canceled";
            highlightCard(cardCanceled);
            filterAndDisplay();
        });

        highlightCard(cardUpcoming);
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        navMessages.setOnClickListener(v -> {
            Intent intent = new Intent(this, MessageActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        navSchedule.setOnClickListener(v -> {
            Toast.makeText(this, "Already on Schedule", Toast.LENGTH_SHORT).show();
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void highlightCard(CardView selected) {
        cardUpcoming.setAlpha(0.6f);
        cardCompleted.setAlpha(0.6f);
        cardCanceled.setAlpha(0.6f);

        selected.setAlpha(1.0f);
    }

    private void fetchAppointmentsFromFirestore() {
        String userId = mAuth.getUid();
        if (userId == null) return;

        db.collection("users").document(userId).collection("my_appointments")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allAppointments.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                String id = document.getString("id");
                                String patientName = document.getString("patientName");
                                String phoneNumber = document.getString("phoneNumber");
                                String clinicName = document.getString("clinicName");
                                String clinicAddress = document.getString("clinicAddress");
                                Double clinicLat = document.getDouble("clinicLat");
                                Double clinicLng = document.getDouble("clinicLng");
                                String date = document.getString("date");
                                String time = document.getString("time");
                                String doctorName = document.getString("doctorName");
                                String specialty = document.getString("specialty");
                                Double price = document.getDouble("price");
                                String status = document.getString("status");
                                String paymentMethod = document.getString("paymentMethod");
                                String bookingTime = document.getString("bookingTime");

                                Appointment appointment = new Appointment(
                                        id, patientName, phoneNumber, clinicName, clinicAddress,
                                        clinicLat != null ? clinicLat : 0,
                                        clinicLng != null ? clinicLng : 0,
                                        date, time, doctorName, specialty,
                                        price != null ? price : 0,
                                        status, paymentMethod, bookingTime
                                );
                                allAppointments.add(appointment);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing appointment: " + e.getMessage());
                            }
                        }
                        updateCounts();
                        filterAndDisplay();
                    } else {
                        Toast.makeText(this, "Error loading appointments", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateCounts() {
        int upcoming = 0, completed = 0, canceled = 0;

        for (Appointment apt : allAppointments) {
            switch (apt.getStatus().toLowerCase()) {
                case "upcoming": upcoming++; break;
                case "completed": completed++; break;
                case "canceled": canceled++; break;
            }
        }

        tvUpcomingCount.setText(String.valueOf(upcoming));
        tvCompletedCount.setText(String.valueOf(completed));
        tvCanceledCount.setText(String.valueOf(canceled));
    }

    private void filterAndDisplay() {
        List<Appointment> filtered = new ArrayList<>();
        for (Appointment apt : allAppointments) {
            if (apt.getStatus().equalsIgnoreCase(currentFilter)) {
                filtered.add(apt);
            }
        }

        if (filtered.isEmpty()) {
            tvEmptyState.setVisibility(android.view.View.VISIBLE);
            recyclerView.setVisibility(android.view.View.GONE);
            tvEmptyState.setText("No " + currentFilter + " appointments");
        } else {
            tvEmptyState.setVisibility(android.view.View.GONE);
            recyclerView.setVisibility(android.view.View.VISIBLE);

            adapter = new AppointmentAdapter(filtered, this::onAppointmentClick);
            recyclerView.setAdapter(adapter);
        }
    }

    private void onAppointmentClick(Appointment appointment) {
        if (appointment.getStatus().equalsIgnoreCase("upcoming")) {
            androidx.appcompat.app.AlertDialog.Builder builder =
                    new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("Appointment Options")
                    .setItems(new String[]{"Open in Google Maps", "Cancel Appointment"},
                            (dialog, which) -> {
                                if (which == 0) {
                                    openInMaps(appointment);
                                } else {
                                    cancelAppointment(appointment);
                                }
                            })
                    .show();
        }
    }

    private void openInMaps(Appointment apt) {
        String uri = "geo:" + apt.getClinicLat() + "," + apt.getClinicLng() +
                "?q=" + android.net.Uri.encode(apt.getClinicName());
        Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        startActivity(intent);
    }

    private void cancelAppointment(Appointment apt) {
        String userId = mAuth.getUid();
        if (userId == null) return;

        // Update locally first for responsiveness
        apt.setStatus("canceled");
        updateCounts();
        filterAndDisplay();

        // Update in Firestore
        db.collection("users").document(userId).collection("my_appointments")
                .document(apt.getId())
                .update("status", "canceled")
                .addOnSuccessListener(aVoid -> {
                    // Also update global collection
                    db.collection("appointments").document(apt.getId())
                            .update("status", "canceled");

                    Toast.makeText(this, "Appointment canceled", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to cancel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Re-fetch to reset state if failed
                    fetchAppointmentsFromFirestore();
                });
    }
}
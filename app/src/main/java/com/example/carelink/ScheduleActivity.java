package com.example.carelink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carelink.adapters.AppointmentAdapter;
import com.example.carelink.models.Appointment;
import com.example.carelink.utils.DataManager;
import java.util.List;

public class ScheduleActivity extends AppCompatActivity {

    private TextView tvScheduleTitle, tvEmptyState;
    private CardView cardUpcoming, cardCompleted, cardCanceled;
    private TextView tvUpcomingCount, tvCompletedCount, tvCanceledCount;
    private RecyclerView recyclerView;
    private AppointmentAdapter adapter;
    private String currentFilter = "upcoming";

    private LinearLayout navHome, navMessages, navSchedule, navProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        initViews();
        setupCategoryTabs();
        setupBottomNavigation();
        loadAppointments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCounts();
        loadAppointments();
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
            loadAppointments();
        });

        cardCompleted.setOnClickListener(v -> {
            currentFilter = "completed";
            highlightCard(cardCompleted);
            loadAppointments();
        });

        cardCanceled.setOnClickListener(v -> {
            currentFilter = "canceled";
            highlightCard(cardCanceled);
            loadAppointments();
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

    private void updateCounts() {
        List<Appointment> all = DataManager.getInstance().getAppointments();
        int upcoming = 0, completed = 0, canceled = 0;

        for (Appointment apt : all) {
            switch (apt.getStatus()) {
                case "upcoming": upcoming++; break;
                case "completed": completed++; break;
                case "canceled": canceled++; break;
            }
        }

        tvUpcomingCount.setText(String.valueOf(upcoming));
        tvCompletedCount.setText(String.valueOf(completed));
        tvCanceledCount.setText(String.valueOf(canceled));
    }

    private void loadAppointments() {
        List<Appointment> filtered = DataManager.getInstance()
                .getAppointmentsByStatus(currentFilter);

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
        if (appointment.getStatus().equals("upcoming")) {
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
        apt.setStatus("canceled");
        updateCounts();
        loadAppointments();
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setMessage("Appointment canceled successfully")
                .setPositiveButton("OK", null)
                .show();
    }
}
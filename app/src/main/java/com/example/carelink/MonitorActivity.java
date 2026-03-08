package com.example.carelink;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonitorActivity extends AppCompatActivity {

    private static final String TAG = "MonitorActivity";
    private ImageView btnBack;
    private LinearLayout btnSeeAllMetrics, navHome, navMessages, navSchedule, navProfile;
    private ProgressBar sleepProgress, gradeProgress;
    private TextView tvSleepPercent, tvSleepDate, tvSleepChange, tvCurrentHeartRate, tvHealthStatus;
    private LineChart chartHeartRate;
    private RadarChart chartRadar;

    private DatabaseReference watchDataRef;
    private ValueEventListener watchListener;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<Entry> heartRateEntries = new ArrayList<>();
    private String currentUserName = "Student";
    
    // SPARK OPTIMIZATION:
    private long lastAlertTime = 0;
    private static final long ALERT_COOLDOWN = 60000; // 1 minute between alerts
    private long lastUpdateUI = 0;
    private static final long UI_UPDATE_INTERVAL = 15000; // 15 seconds between UI updates

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        initViews();
        setupCharts();
        setupClickListeners();
        setupBottomNavigation();
        loadUserData(); // Fetch name for alerts
        loadLiveWatchData();

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        tvSleepDate.setText(sdf.format(new Date()));
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnSeeAllMetrics = findViewById(R.id.btnSeeAllMetrics);
        navHome = findViewById(R.id.navHome);
        navMessages = findViewById(R.id.navMessages);
        navSchedule = findViewById(R.id.navSchedule);
        navProfile = findViewById(R.id.navProfile);

        sleepProgress = findViewById(R.id.sleepProgress);
        gradeProgress = findViewById(R.id.gradeProgress);
        tvSleepPercent = findViewById(R.id.tvSleepPercent);
        tvSleepDate = findViewById(R.id.tvSleepDate);
        tvSleepChange = findViewById(R.id.tvSleepChange);
        tvCurrentHeartRate = findViewById(R.id.tvCurrentHeartRate);
        tvHealthStatus = findViewById(R.id.tvHealthStatus);
        chartHeartRate = findViewById(R.id.chartHeartRate);
        chartRadar = findViewById(R.id.chartRadar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadUserData() {
        db.collection("users").document(mAuth.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUserName = doc.contains("name") ? doc.getString("name") : "Student";
                    }
                });
    }

    private void setupCharts() {
        setupHeartRateChart();
        setupRadarChart();
    }

    private void setupHeartRateChart() {
        LineDataSet dataSet = new LineDataSet(heartRateEntries, "Live Pulse");
        dataSet.setColor(Color.parseColor("#D32F2F"));
        dataSet.setLineWidth(3f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        chartHeartRate.setData(lineData);
        chartHeartRate.getDescription().setEnabled(false);
        chartHeartRate.getLegend().setEnabled(false);
        chartHeartRate.setTouchEnabled(false);

        XAxis xAxis = chartHeartRate.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(5);

        YAxis leftAxis = chartHeartRate.getAxisLeft();
        leftAxis.setAxisMinimum(40);
        leftAxis.setAxisMaximum(180);
        chartHeartRate.getAxisRight().setEnabled(false);
    }

    private void setupRadarChart() {
        List<RadarEntry> entries1 = new ArrayList<>();
        entries1.add(new RadarEntry(80)); entries1.add(new RadarEntry(65));
        entries1.add(new RadarEntry(90)); entries1.add(new RadarEntry(75));
        RadarDataSet dataSet1 = new RadarDataSet(entries1, "Current Metrics");
        dataSet1.setColor(Color.parseColor("#4CAF50"));
        dataSet1.setFillColor(Color.parseColor("#4CAF50"));
        dataSet1.setDrawFilled(true);
        dataSet1.setFillAlpha(100);
        chartRadar.setData(new RadarData(dataSet1));
        chartRadar.getDescription().setEnabled(false);
        chartRadar.invalidate();
    }

    private void loadLiveWatchData() {
        String uid = mAuth.getUid();
        watchDataRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("vitals");

        watchListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    long now = System.currentTimeMillis();
                    
                    Integer heartRate = snapshot.child("heart_rate").getValue(Integer.class);
                    Integer sleep = snapshot.child("sleep_quality").getValue(Integer.class);
                    
                    if (now - lastUpdateUI > UI_UPDATE_INTERVAL) {
                        if (heartRate != null) updateHeartRateUI(heartRate);
                        if (sleep != null) {
                            sleepProgress.setProgress(sleep);
                            tvSleepPercent.setText(sleep + "%");
                        }
                        lastUpdateUI = now;
                    }
                    
                    if (heartRate != null) {
                        checkPredictiveAlerts(heartRate);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Watch Data Error: " + error.getMessage());
            }
        };

        watchDataRef.addValueEventListener(watchListener);
    }

    private void updateHeartRateUI(int bpm) {
        tvCurrentHeartRate.setText(bpm + " BPM");
        if (heartRateEntries.size() > 15) heartRateEntries.remove(0);
        heartRateEntries.add(new Entry(heartRateEntries.size(), bpm));
        LineDataSet dataSet = (LineDataSet) chartHeartRate.getData().getDataSetByIndex(0);
        dataSet.setValues(heartRateEntries);
        chartHeartRate.getData().notifyDataChanged();
        chartHeartRate.notifyDataSetChanged();
        chartHeartRate.invalidate();
    }

    private void checkPredictiveAlerts(int bpm) {
        long now = System.currentTimeMillis();
        
        if (bpm > 120 || bpm < 50) {
            String type = bpm > 120 ? "Tachycardia Detected" : "Bradycardia Detected";
            tvHealthStatus.setText("Alert: " + type);
            tvHealthStatus.setTextColor(Color.RED);
            
            if (now - lastAlertTime > ALERT_COOLDOWN) {
                sendCaregiverAlert(type, bpm);
                lastAlertTime = now;
            }
        } else {
            tvHealthStatus.setText("Normal Pulse");
            tvHealthStatus.setTextColor(Color.parseColor("#4CAF50"));
        }
    }

    private void sendCaregiverAlert(String type, int value) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", type);
        alert.put("value", value);
        alert.put("patientName", currentUserName);
        alert.put("timestamp", System.currentTimeMillis());
        alert.put("status", "ACTIVE"); // Dashboard listens for ACTIVE
        alert.put("patient_uid", mAuth.getUid());

        db.collection("emergency_alerts").add(alert)
            .addOnSuccessListener(doc -> Log.d(TAG, "Vitals Alert Sent"))
            .addOnFailureListener(e -> Log.e(TAG, "Alert failed: " + e.getMessage()));
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSeeAllMetrics.setOnClickListener(v -> Toast.makeText(this, "Detailed History Loaded", Toast.LENGTH_SHORT).show());
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        });
        navSchedule.setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity.class)));
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (watchDataRef != null && watchListener != null) {
            watchDataRef.removeEventListener(watchListener);
        }
    }
}
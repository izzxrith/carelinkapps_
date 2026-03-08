package com.example.carelink;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MonitorActivity extends AppCompatActivity {

    private ImageView btnBack;
    private LinearLayout btnSeeAllMetrics, navHome, navMessages, navSchedule, navProfile;
    private ProgressBar sleepProgress, gradeProgress;
    private TextView tvSleepPercent, tvSleepDate, tvSleepChange, tvCurrentHeartRate, tvHealthStatus;
    private LineChart chartHeartRate;
    private RadarChart chartRadar;

    private DatabaseReference watchDataRef;
    private Handler realtimeHandler;
    private Runnable realtimeRunnable;

    private List<Entry> heartRateEntries = new ArrayList<>();
    private int dataIndex = 0;
    private final String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

        initViews();
        setupCharts();
        setupClickListeners();
        setupBottomNavigation();
        startRealtimeDataSimulation();
        loadWatchData();

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

        // Initialize Firebase
        watchDataRef = FirebaseDatabase.getInstance().getReference("watch_data");
    }

    private void setupCharts() {
        setupHeartRateChart();
        setupRadarChart();
    }

    private void setupHeartRateChart() {
        // Initial data
        heartRateEntries.add(new Entry(0, 115));
        heartRateEntries.add(new Entry(1, 118));
        heartRateEntries.add(new Entry(2, 122));
        heartRateEntries.add(new Entry(3, 128));
        heartRateEntries.add(new Entry(4, 125));
        heartRateEntries.add(new Entry(5, 120));
        heartRateEntries.add(new Entry(6, 118));

        dataIndex = 7;

        LineDataSet dataSet = new LineDataSet(heartRateEntries, "Heart Rate");
        dataSet.setColor(Color.parseColor("#D32F2F"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.parseColor("#D32F2F"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        chartHeartRate.setData(lineData);

        // Styling
        chartHeartRate.getDescription().setEnabled(false);
        chartHeartRate.getLegend().setEnabled(false);
        chartHeartRate.setTouchEnabled(false);

        XAxis xAxis = chartHeartRate.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = chartHeartRate.getAxisLeft();
        leftAxis.setAxisMinimum(100);
        leftAxis.setAxisMaximum(140);
        leftAxis.setDrawGridLines(true);

        chartHeartRate.getAxisRight().setEnabled(false);
        chartHeartRate.invalidate();
    }

    private void setupRadarChart() {
        List<RadarEntry> entries1 = new ArrayList<>();
        entries1.add(new RadarEntry(80)); // Sleep
        entries1.add(new RadarEntry(65)); // Water
        entries1.add(new RadarEntry(90)); // Blood Pressure
        entries1.add(new RadarEntry(75)); // Oxygen

        List<RadarEntry> entries2 = new ArrayList<>();
        entries2.add(new RadarEntry(60)); // Sleep
        entries2.add(new RadarEntry(85)); // Water
        entries2.add(new RadarEntry(70)); // Blood Pressure
        entries2.add(new RadarEntry(80)); // Oxygen

        RadarDataSet dataSet1 = new RadarDataSet(entries1, "This Month");
        dataSet1.setColor(Color.parseColor("#4CAF50"));
        dataSet1.setFillColor(Color.parseColor("#4CAF50"));
        dataSet1.setDrawFilled(true);
        dataSet1.setFillAlpha(100);

        RadarDataSet dataSet2 = new RadarDataSet(entries2, "Last Month");
        dataSet2.setColor(Color.parseColor("#9C27B0"));
        dataSet2.setFillColor(Color.parseColor("#9C27B0"));
        dataSet2.setDrawFilled(true);
        dataSet2.setFillAlpha(100);

        RadarData radarData = new RadarData(dataSet1, dataSet2);

        chartRadar.setData(radarData);
        chartRadar.getDescription().setEnabled(false);

        String[] labels = {"Sleep", "Water", "BP", "Oxygen"};
        chartRadar.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartRadar.getYAxis().setAxisMinimum(0);
        chartRadar.getYAxis().setAxisMaximum(100);
        chartRadar.invalidate();
    }

    private void startRealtimeDataSimulation() {
        realtimeHandler = new Handler(Looper.getMainLooper());
        realtimeRunnable = new Runnable() {
            @Override
            public void run() {
                // Simulate new heart rate data from watch
                int newHeartRate = 110 + (int)(Math.random() * 20); // 110-130 bpm

                // Update current display
                tvCurrentHeartRate.setText("Heart Rate\n" + newHeartRate);

                // Add to chart
                if (heartRateEntries.size() > 10) {
                    heartRateEntries.remove(0);
                    // Re-index entries
                    for (int i = 0; i < heartRateEntries.size(); i++) {
                        heartRateEntries.get(i).setX(i);
                    }
                }
                heartRateEntries.add(new Entry(heartRateEntries.size(), newHeartRate));

                LineDataSet dataSet = (LineDataSet) chartHeartRate.getData().getDataSetByIndex(0);
                dataSet.setValues(heartRateEntries);
                chartHeartRate.getData().notifyDataChanged();
                chartHeartRate.notifyDataSetChanged();
                chartHeartRate.invalidate();

                // Update sleep progress (simulate from watch)
                int sleepQuality = 70 + (int)(Math.random() * 10);
                sleepProgress.setProgress(sleepQuality);
                tvSleepPercent.setText(sleepQuality + "%");

                // Schedule next update
                realtimeHandler.postDelayed(this, 3000); // Update every 3 seconds
            }
        };

        realtimeHandler.post(realtimeRunnable);
    }

    private void loadWatchData() {
        // Listen to Firebase Realtime Database for watch data
        watchDataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Get heart rate
                    Integer heartRate = snapshot.child("heart_rate").getValue(Integer.class);
                    if (heartRate != null) {
                        tvCurrentHeartRate.setText("Heart Rate\n" + heartRate);
                    }

                    // Get sleep data
                    Integer sleep = snapshot.child("sleep_quality").getValue(Integer.class);
                    if (sleep != null) {
                        sleepProgress.setProgress(sleep);
                        tvSleepPercent.setText(sleep + "%");
                    }

                    // Get steps
                    Integer steps = snapshot.child("steps").getValue(Integer.class);

                    // Update health grade based on data
                    updateHealthGrade(heartRate, sleep, steps);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MonitorActivity.this,
                        "Failed to load watch data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateHealthGrade(Integer heartRate, Integer sleep, Integer steps) {
        int score = 0;
        if (heartRate != null && heartRate >= 60 && heartRate <= 100) score += 30;
        else if (heartRate != null) score += 20;

        if (sleep != null && sleep >= 80) score += 35;
        else if (sleep != null) score += 25;

        if (steps != null && steps >= 10000) score += 35;
        else if (steps != null) score += 20;

        gradeProgress.setProgress(score);

        if (score >= 90) {
            tvHealthStatus.setText("Excellent");
            tvHealthStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else if (score >= 75) {
            tvHealthStatus.setText("Good");
            tvHealthStatus.setTextColor(Color.parseColor("#8BC34A"));
        } else if (score >= 60) {
            tvHealthStatus.setText("Fair");
            tvHealthStatus.setTextColor(Color.parseColor("#FFC107"));
        } else {
            tvHealthStatus.setText("Needs Attention");
            tvHealthStatus.setTextColor(Color.parseColor("#F44336"));
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSeeAllMetrics.setOnClickListener(v -> {
            Toast.makeText(this, "All metrics - Coming Soon", Toast.LENGTH_SHORT).show();
        });
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
            Intent intent = new Intent(this, ScheduleActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realtimeHandler != null && realtimeRunnable != null) {
            realtimeHandler.removeCallbacks(realtimeRunnable);
        }
    }
}
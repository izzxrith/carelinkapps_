package com.example.carelink;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carelink.adapters.TopDoctorAdapter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "DashboardActivity";
    private TextView tvWelcome, tvSubtitle, tvSeeAllLocation, tvSeeAllDoctors;
    private EditText etSearch;
    private LinearLayout btnDoctor, btnMonitor, btnEmotion, btnAmbulance, btnLink, btnOpenMap;
    private CardView cardBanner;
    private RecyclerView rvTopDoctors;
    private MapView ivMapPreview;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TopDoctorAdapter doctorAdapter;
    private List<DoctorItem> doctorList;
    private EmergencyMonitor emergencyMonitor;
    private ListenerRegistration sosListener;

    private GoogleMap googleMap;
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";
    
    private String currentUserName = "User";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        loadUserData();
        setupClickListeners();
        loadTopDoctors();
        startEmergencyMonitoring();
        setupMap(savedInstanceState);
        
        // SK PINJI FEATURE: Listen for SOS from students
        startSOSListener();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        etSearch = findViewById(R.id.etSearch);
        btnDoctor = findViewById(R.id.btnDoctor);
        btnMonitor = findViewById(R.id.btnMonitor);
        btnEmotion = findViewById(R.id.btnEmotion);
        btnAmbulance = findViewById(R.id.btnAmbulance);
        btnLink = findViewById(R.id.btnLink);
        btnOpenMap = findViewById(R.id.btnOpenMap);

        cardBanner = findViewById(R.id.cardBanner);
        rvTopDoctors = findViewById(R.id.rvTopDoctors);
        ivMapPreview = findViewById(R.id.ivMapPreview);

        tvSeeAllLocation = findViewById(R.id.tvSeeAllLocation);
        tvSeeAllDoctors = findViewById(R.id.tvSeeAllDoctors);

        setupCustomBottomNavigation();

        rvTopDoctors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        doctorList = new ArrayList<>();
        doctorAdapter = new TopDoctorAdapter(doctorList, this::onDoctorClick);
        rvTopDoctors.setAdapter(doctorAdapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    private void startSOSListener() {
        String guardianUid = mAuth.getUid();
        if (guardianUid == null) return;

        // Listen for ANY active emergency alert
        sosListener = db.collection("emergency_alerts")
                .whereEqualTo("status", "ACTIVE")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String patientName = doc.getString("patientName");
                        String type = doc.getString("type");
                        showEmergencyDialog(patientName, type, doc.getId());
                    }
                });
    }

    private void showEmergencyDialog(String patient, String type, String alertId) {
        new AlertDialog.Builder(this)
                .setTitle("🚨 EMERGENCY ALERT")
                .setMessage(patient + " is in distress! (" + type + ")\nDo you want to track their location now?")
                .setPositiveButton("TRACK NOW", (dialog, which) -> {
                    // Mark as resolved so it doesn't pop up again
                    db.collection("emergency_alerts").document(alertId).update("status", "RESPONDED");
                    startActivity(new Intent(this, LocationMapActivity.class));
                })
                .setNegativeButton("DISMISS", (dialog, which) -> {
                    db.collection("emergency_alerts").document(alertId).update("status", "DISMISSED");
                })
                .setCancelable(false)
                .show();
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            currentUserName = doc.getString("name");
                            tvWelcome.setText("Welcome, " + currentUserName);
                            String role = doc.getString("role");
                            tvSubtitle.setText(role + " Dashboard");
                        }
                    });
        }
    }

    private void setupClickListeners() {
        btnMonitor.setOnClickListener(v -> startActivity(new Intent(this, MonitorActivity.class)));
        btnEmotion.setOnClickListener(v -> startActivity(new Intent(this, EmotionActivity.class)));
        btnOpenMap.setOnClickListener(v -> startActivity(new Intent(this, LocationMapActivity.class)));
        btnAmbulance.setOnClickListener(v -> triggerSmartSOS());
        btnDoctor.setOnClickListener(v -> startActivity(new Intent(this, BookingActivity.class)));
        btnLink.setOnClickListener(v -> startActivity(new Intent(this, QRCodeActivity.class)));
    }

    private void triggerSmartSOS() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(android.net.Uri.parse("tel:999"));
        startActivity(intent);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        FirebaseDatabase.getInstance().getReference("locations").child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        double lat = 0, lng = 0;
                        if (snapshot.exists()) {
                            lat = snapshot.child("latitude").getValue(Double.class);
                            lng = snapshot.child("longitude").getValue(Double.class);
                        }
                        sendSOSAlertToCloud(user.getUid(), lat, lng);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        sendSOSAlertToCloud(user.getUid(), 0, 0);
                    }
                });
    }

    private void sendSOSAlertToCloud(String uid, double lat, double lng) {
        Map<String, Object> sos = new HashMap<>();
        sos.put("type", "CRITICAL SOS");
        sos.put("patientName", currentUserName);
        sos.put("latitude", lat);
        sos.put("longitude", lng);
        sos.put("timestamp", System.currentTimeMillis());
        sos.put("status", "ACTIVE");

        db.collection("emergency_alerts").add(sos)
                .addOnSuccessListener(doc -> Toast.makeText(this, "SOS Sent!", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Log.e(TAG, "SOS Failed: " + e.getMessage()));
    }

    private void setupCustomBottomNavigation() {
        findViewById(R.id.navMessages).setOnClickListener(v -> startActivity(new Intent(this, MessageActivity.class)));
        findViewById(R.id.navSchedule).setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void loadTopDoctors() {
        doctorList.add(new DoctorItem("Dr. Sarah", "Nephrology", "4.7", "2km", R.drawable.ic_doctor_male));
        doctorList.add(new DoctorItem("Dr. Rajesh", "Radiology", "4.9", "1.5km", R.drawable.ic_doctor_female));
        doctorAdapter.notifyDataSetChanged();
    }

    private void onDoctorClick(DoctorItem doctor) {
        Intent intent = new Intent(this, BookingActivity.class);
        intent.putExtra("DOCTOR_NAME", doctor.name);
        startActivity(intent);
    }

    private void startEmergencyMonitoring() {
        Intent fallService = new Intent(this, FallDetectionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(fallService);
        else startService(fallService);
        emergencyMonitor = new EmergencyMonitor(this);
        emergencyMonitor.startMonitoring();
    }

    private void setupMap(Bundle savedInstanceState) {
        Bundle mapViewBundle = (savedInstanceState != null) ? savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY) : null;
        ivMapPreview.onCreate(mapViewBundle);
        ivMapPreview.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        LatLng kl = new LatLng(3.1390, 101.6869);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kl, 12f));
        googleMap.setOnMapClickListener(latLng -> startActivity(new Intent(this, LocationMapActivity.class)));
    }

    @Override protected void onStart() { super.onStart(); ivMapPreview.onStart(); }
    @Override protected void onResume() { super.onResume(); ivMapPreview.onResume(); }
    @Override protected void onPause() { ivMapPreview.onPause(); super.onPause(); }
    @Override protected void onStop() { ivMapPreview.onStop(); super.onStop(); }
    @Override protected void onDestroy() { 
        ivMapPreview.onDestroy(); super.onDestroy(); 
        if (emergencyMonitor != null) emergencyMonitor.stopMonitoring(); 
        if (sosListener != null) sosListener.remove();
    }
    @Override public void onLowMemory() { super.onLowMemory(); ivMapPreview.onLowMemory(); }
    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle bundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (bundle == null) outState.putBundle(MAP_VIEW_BUNDLE_KEY, new Bundle());
        ivMapPreview.onSaveInstanceState(outState.getBundle(MAP_VIEW_BUNDLE_KEY));
    }

    public static class DoctorItem {
        public String name, specialty, rating, distance;
        public int imageRes;
        public DoctorItem(String n, String s, String r, String d, int i) {
            name = n; specialty = s; rating = r; distance = d; imageRes = i;
        }
    }
}
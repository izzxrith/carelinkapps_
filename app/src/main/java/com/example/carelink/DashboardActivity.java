package com.example.carelink;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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
    private ListenerRegistration sosListener;

    private GoogleMap googleMap;
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";
    
    private String currentUserName = "User";
    private String userRole = "Guardian";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        loadUserData();
        setupClickListeners();
        setupBottomNavigation();
        loadTopDoctors();
        
        try {
            setupMap(savedInstanceState);
        } catch (Exception e) {
            Log.e(TAG, "Map error: " + e.getMessage());
        }
        
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

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            currentUserName = doc.contains("name") ? doc.getString("name") : "User";
                            userRole = doc.contains("role") ? doc.getString("role") : "Guardian";
                            
                            tvWelcome.setText("Welcome, " + currentUserName);
                            tvSubtitle.setText(userRole + " Dashboard");
                            
                            if ("Guardian".equalsIgnoreCase(userRole)) {
                                checkLinkedStudentsCount();
                            }
                        }
                    });
        }
    }

    private void checkLinkedStudentsCount() {
        db.collection("student_guardian_links")
                .whereEqualTo("guardian_uid", mAuth.getUid())
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        tvSubtitle.setText("Tap 'Link' to add a student");
                    } else {
                        tvSubtitle.setText("Monitoring " + snapshots.size() + " student(s)");
                    }
                });
    }

    private void startSOSListener() {
        String guardianUid = mAuth.getUid();
        if (guardianUid == null) return;

        sosListener = db.collection("emergency_alerts")
                .whereEqualTo("status", "ACTIVE")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String patientUid = doc.getString("patient_uid");
                        if (patientUid != null) {
                            verifyLinkAndShowAlert(patientUid, doc.getString("patientName"), doc.getString("type"), doc.getId());
                        }
                    }
                });
    }

    private void verifyLinkAndShowAlert(String studentUid, String name, String type, String alertId) {
        db.collection("student_guardian_links")
                .document(mAuth.getUid() + "_" + studentUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        showEmergencyDialog(name, type, alertId);
                    }
                });
    }

    private void showEmergencyDialog(String patient, String type, String alertId) {
        if (isFinishing()) return;
        
        new AlertDialog.Builder(this)
                .setTitle("🚨 STUDENT SOS")
                .setMessage(patient + " needs help! (" + type + ")\nOpen tracking map?")
                .setPositiveButton("TRACK NOW", (dialog, which) -> {
                    db.collection("emergency_alerts").document(alertId).update("status", "RESOLVED");
                    startActivity(new Intent(this, LocationMapActivity.class));
                })
                .setNegativeButton("DISMISS", (dialog, which) -> {
                    db.collection("emergency_alerts").document(alertId).update("status", "DISMISSED");
                })
                .setCancelable(false)
                .show();
    }

    private void setupClickListeners() {
        btnDoctor.setOnClickListener(v -> startActivity(new Intent(this, BookingActivity.class)));
        btnMonitor.setOnClickListener(v -> startActivity(new Intent(this, MonitorActivity.class)));
        btnEmotion.setOnClickListener(v -> startActivity(new Intent(this, EmotionActivity.class)));
        btnAmbulance.setOnClickListener(v -> triggerSmartSOS());
        btnLink.setOnClickListener(v -> startActivity(new Intent(this, QRCodeActivity.class)));
        etSearch.setOnClickListener(v -> startActivity(new Intent(this, BookingActivity.class)));
        btnOpenMap.setOnClickListener(v -> startActivity(new Intent(this, LocationMapActivity.class)));
        tvSeeAllLocation.setOnClickListener(v -> startActivity(new Intent(this, LocationMapActivity.class)));
        tvSeeAllDoctors.setOnClickListener(v -> startActivity(new Intent(this, BookingActivity.class)));
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v -> Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navMessages).setOnClickListener(v -> startActivity(new Intent(this, MessageActivity.class)));
        findViewById(R.id.navSchedule).setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void triggerSmartSOS() {
        Toast.makeText(this, "SOS Active", Toast.LENGTH_SHORT).show();
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
                            Double latVal = snapshot.child("latitude").getValue(Double.class);
                            Double lngVal = snapshot.child("longitude").getValue(Double.class);
                            lat = latVal != null ? latVal : 0;
                            lng = lngVal != null ? lngVal : 0;
                        }
                        sendSOSToCloud(lat, lng);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void sendSOSToCloud(double lat, double lng) {
        Map<String, Object> sos = new HashMap<>();
        sos.put("type", "GUARDIAN SOS");
        sos.put("patientName", currentUserName);
        sos.put("latitude", lat);
        sos.put("longitude", lng);
        sos.put("timestamp", System.currentTimeMillis());
        sos.put("status", "ACTIVE");
        sos.put("patient_uid", mAuth.getUid());

        db.collection("emergency_alerts").add(sos);
    }

    private void loadTopDoctors() {
        // SK PINJI LOCALIZATION: Diverse local Malay names for Ipoh demo
        doctorList.clear();
        doctorList.add(new DoctorItem("Dr. Ahmad Zaki", "Pediatric Specialist", "4.9", "0.5km", R.drawable.ic_doctor_male));
        doctorList.add(new DoctorItem("Dr. Siti Noraini", "Occupational Therapist", "4.8", "1.2km", R.drawable.ic_doctor_female));
        doctorList.add(new DoctorItem("Dr. Azman Hassan", "Clinical Psychologist", "4.7", "2.0km", R.drawable.ic_doctor_male));
        doctorAdapter.notifyDataSetChanged();
    }

    private void onDoctorClick(DoctorItem doctor) {
        startActivity(new Intent(this, BookingActivity.class));
    }

    private void setupMap(Bundle savedInstanceState) {
        Bundle bundle = (savedInstanceState != null) ? savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY) : null;
        if (ivMapPreview != null) {
            ivMapPreview.onCreate(bundle);
            ivMapPreview.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        LatLng ipoh = new LatLng(4.5975, 101.1031);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ipoh, 12f));
        googleMap.setOnMapClickListener(latLng -> startActivity(new Intent(this, LocationMapActivity.class)));
    }

    @Override protected void onStart() { super.onStart(); if (ivMapPreview != null) ivMapPreview.onStart(); }
    @Override protected void onResume() { super.onResume(); if (ivMapPreview != null) ivMapPreview.onResume(); }
    @Override protected void onPause() { if (ivMapPreview != null) ivMapPreview.onPause(); super.onPause(); }
    @Override protected void onStop() { if (ivMapPreview != null) ivMapPreview.onStop(); super.onStop(); }
    
    @Override protected void onDestroy() { 
        if (ivMapPreview != null) ivMapPreview.onDestroy(); 
        super.onDestroy(); 
        if (sosListener != null) {
            sosListener.remove();
            sosListener = null;
        }
    }
    
    @Override public void onLowMemory() { super.onLowMemory(); if (ivMapPreview != null) ivMapPreview.onLowMemory(); }
    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (ivMapPreview != null) ivMapPreview.onSaveInstanceState(outState.getBundle(MAP_VIEW_BUNDLE_KEY));
    }

    public static class DoctorItem {
        public String name, specialty, rating, distance;
        public int imageRes;
        public DoctorItem(String n, String s, String r, String d, int i) {
            name = n; specialty = s; rating = r; distance = d; imageRes = i;
        }
    }
}
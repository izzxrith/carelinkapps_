package com.example.carelink;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carelink.adapters.StudentStatusAdapter;
import com.example.carelink.adapters.TopDoctorAdapter;
import com.example.carelink.models.StudentStatus;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
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
    private LinearLayout btnDoctor, btnMonitor, btnEmotion, btnAmbulance, btnLink, btnOpenMap, layoutStudentBoard;
    private RecyclerView rvTopDoctors, rvStudentStatus;
    private MapView ivMapPreview;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TopDoctorAdapter doctorAdapter;
    private List<DoctorItem> doctorList;
    
    private StudentStatusAdapter statusAdapter;
    private List<StudentStatus> studentStatusList = new ArrayList<>();
    
    // Safety: Group listeners for cleanup
    private Map<String, ValueEventListener> vitalsListeners = new HashMap<>();
    private Map<String, ValueEventListener> locationListeners = new HashMap<>();

    private ListenerRegistration sosListener;
    private GoogleMap googleMap;
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";
    
    private static final double SK_PINJI_LAT = 4.565549;
    private static final double SK_PINJI_LNG = 101.081350;

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
        
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }
        if (ivMapPreview != null) {
            ivMapPreview.onCreate(mapViewBundle);
            ivMapPreview.getMapAsync(this);
        }
        
        startSOSListener();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        btnDoctor = findViewById(R.id.btnDoctor);
        btnMonitor = findViewById(R.id.btnMonitor);
        btnEmotion = findViewById(R.id.btnEmotion);
        btnAmbulance = findViewById(R.id.btnAmbulance);
        btnLink = findViewById(R.id.btnLink);
        btnOpenMap = findViewById(R.id.btnOpenMap);
        layoutStudentBoard = findViewById(R.id.layoutStudentBoard);
        rvStudentStatus = findViewById(R.id.rvStudentStatus);
        rvTopDoctors = findViewById(R.id.rvTopDoctors);
        ivMapPreview = findViewById(R.id.ivMapPreview);
        tvSeeAllLocation = findViewById(R.id.tvSeeAllLocation);
        tvSeeAllDoctors = findViewById(R.id.tvSeeAllDoctors);
        etSearch = findViewById(R.id.etSearch);

        rvTopDoctors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        doctorList = new ArrayList<>();
        doctorAdapter = new TopDoctorAdapter(doctorList, this::onDoctorClick);
        rvTopDoctors.setAdapter(doctorAdapter);

        rvStudentStatus.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        statusAdapter = new StudentStatusAdapter(studentStatusList, this);
        rvStudentStatus.setAdapter(statusAdapter);
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("name");
                    String role = doc.getString("role");
                    tvWelcome.setText("Welcome, " + (name != null ? name : "User"));
                    if ("Guardian".equalsIgnoreCase(role)) {
                        if (layoutStudentBoard != null) layoutStudentBoard.setVisibility(View.VISIBLE);
                        fetchLinkedStudents();
                    }
                }
            });
    }

    private void fetchLinkedStudents() {
        db.collection("student_guardian_links")
            .whereEqualTo("guardian_uid", mAuth.getUid())
            .addSnapshotListener((snapshots, e) -> {
                if (e != null || snapshots == null) return;
                studentStatusList.clear();
                for (QueryDocumentSnapshot doc : snapshots) {
                    loadStudentForBoard(doc.getString("student_uid"));
                }
            });
    }

    private void loadStudentForBoard(String uid) {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                StudentStatus status = new StudentStatus(uid, doc.getString("name"));
                studentStatusList.add(status);
                statusAdapter.notifyDataSetChanged();
                attachPredictiveListeners(uid, status);
            }
        });
    }

    private void attachPredictiveListeners(String uid, StudentStatus status) {
        // 1. Vitals
        ValueEventListener vListener = FirebaseDatabase.getInstance().getReference("users")
            .child(uid).child("vitals")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Integer hr = snapshot.child("heart_rate").getValue(Integer.class);
                        if (hr != null) {
                            status.setHeartRate(hr);
                            if (hr > 120) status.setState("Emergency");
                            else if (hr > 100) status.setState("Warning"); 
                            else status.setState("Normal");
                            statusAdapter.notifyDataSetChanged();
                        }
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        vitalsListeners.put(uid, vListener);

        // 2. Location
        ValueEventListener lListener = FirebaseDatabase.getInstance().getReference("locations")
            .child(uid)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Double lat = snapshot.child("latitude").getValue(Double.class);
                        Double lng = snapshot.child("longitude").getValue(Double.class);
                        if (lat != null && lng != null) {
                            float[] results = new float[1];
                            Location.distanceBetween(lat, lng, SK_PINJI_LAT, SK_PINJI_LNG, results);
                            if (results[0] > 500) status.setLocationStatus("Wandering");
                            else status.setLocationStatus("Safe");
                            statusAdapter.notifyDataSetChanged();
                        }
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        locationListeners.put(uid, lListener);
    }

    private void startSOSListener() {
        sosListener = db.collection("emergency_alerts")
            .whereEqualTo("status", "ACTIVE")
            .addSnapshotListener((snapshots, e) -> {
                if (e != null || snapshots == null) return;
                for (QueryDocumentSnapshot doc : snapshots) {
                    String studentUid = doc.getString("patient_uid");
                    if (studentUid != null) checkLinkAndNotify(studentUid, doc);
                }
            });
    }

    private void checkLinkAndNotify(String studentUid, QueryDocumentSnapshot alert) {
        db.collection("student_guardian_links").document(mAuth.getUid() + "_" + studentUid).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    for (StudentStatus s : studentStatusList) if (s.getUid().equals(studentUid)) s.setSos(true);
                    statusAdapter.notifyDataSetChanged();
                    showEmergencyDialog(alert.getString("patientName"), alert.getString("type"), alert.getId());
                }
            });
    }

    private void showEmergencyDialog(String patient, String type, String alertId) {
        if (isFinishing()) return;
        new AlertDialog.Builder(this)
            .setTitle("🚨 EMERGENCY ALERT")
            .setMessage(patient + " is in distress! (" + type + ")\nOpen tracking map?")
            .setPositiveButton("TRACK NOW", (dialog, which) -> {
                db.collection("emergency_alerts").document(alertId).update("status", "RESOLVED");
                startActivity(new Intent(this, LocationMapActivity.class));
            })
            .setNegativeButton("DISMISS", (dialog, which) -> db.collection("emergency_alerts").document(alertId).update("status", "DISMISSED"))
            .show();
    }

    private void setupClickListeners() {
        btnDoctor.setOnClickListener(v -> startActivity(new Intent(this, BookingActivity.class)));
        btnMonitor.setOnClickListener(v -> startActivity(new Intent(this, MonitorActivity.class)));
        btnEmotion.setOnClickListener(v -> startActivity(new Intent(this, EmotionActivity.class)));
        btnAmbulance.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:999"))));
        btnLink.setOnClickListener(v -> startActivity(new Intent(this, QRCodeActivity.class)));
        btnOpenMap.setOnClickListener(v -> startActivity(new Intent(this, LocationMapActivity.class)));
        tvSeeAllLocation.setOnClickListener(v -> startActivity(new Intent(this, LocationMapActivity.class)));
        tvSeeAllDoctors.setOnClickListener(v -> startActivity(new Intent(this, BookingActivity.class)));
        etSearch.setOnClickListener(v -> startActivity(new Intent(this, BookingActivity.class)));
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navMessages).setOnClickListener(v -> startActivity(new Intent(this, MessageActivity.class)));
        findViewById(R.id.navSchedule).setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void loadTopDoctors() {
        doctorList.clear();
        doctorList.add(new DoctorItem("Dr. Ahmad Zaki", "Pediatric Specialist", "4.9", "0.5km", R.drawable.ic_doctor2));
        doctorList.add(new DoctorItem("Dr. Siti Noraini", "Occupational Therapist", "4.8", "1.2km", R.drawable.ic_doctor1));
        doctorList.add(new DoctorItem("Dr. Azman Hassan", "Clinical Psychologist", "4.7", "2.0km", R.drawable.ic_doctor3));
        doctorAdapter.notifyDataSetChanged();
    }

    private void onDoctorClick(DoctorItem doctor) { startActivity(new Intent(this, BookingActivity.class)); }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        LatLng school = new LatLng(SK_PINJI_LAT, SK_PINJI_LNG);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(school, 15f));
    }

    @Override protected void onStart() { super.onStart(); if (ivMapPreview != null) ivMapPreview.onStart(); }
    @Override protected void onResume() { super.onResume(); if (ivMapPreview != null) ivMapPreview.onResume(); }
    @Override protected void onPause() { if (ivMapPreview != null) ivMapPreview.onPause(); super.onPause(); }
    @Override protected void onStop() { if (ivMapPreview != null) ivMapPreview.onStop(); super.onStop(); }
    
    @Override protected void onDestroy() { 
        if (ivMapPreview != null) ivMapPreview.onDestroy(); 
        super.onDestroy(); 
        if (sosListener != null) sosListener.remove();
        // CLEANUP ALL LISTENERS
        for (String uid : vitalsListeners.keySet()) {
            FirebaseDatabase.getInstance().getReference("users").child(uid).child("vitals").removeEventListener(vitalsListeners.get(uid));
        }
        for (String uid : locationListeners.keySet()) {
            FirebaseDatabase.getInstance().getReference("locations").child(uid).removeEventListener(locationListeners.get(uid));
        }
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle bundle = new Bundle();
        outState.putBundle(MAP_VIEW_BUNDLE_KEY, bundle);
        if (ivMapPreview != null) ivMapPreview.onSaveInstanceState(bundle);
    }

    public static class DoctorItem {
        public String name, specialty, rating, distance;
        public int imageRes;
        public DoctorItem(String n, String s, String r, String d, int i) {
            name = n; specialty = s; rating = r; distance = d; imageRes = i;
        }
    }
}
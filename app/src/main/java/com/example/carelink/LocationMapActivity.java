package com.example.carelink;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class LocationMapActivity extends AppCompatActivity {

    private static final String TAG = "LocationMapActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    
    private String currentUserId; 
    private String targetTrackId; 

    private ImageView btnBack;
    private MaterialButton btnTrack;
    private TextView tvLastUpdated, tvTrackingName;

    private DatabaseReference databaseRef;
    private ValueEventListener locationListener;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private double latestLat = 0.0;
    private double latestLng = 0.0;
    private String lastUpdatedTime = "--:--";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_map);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            targetTrackId = currentUserId; 
        } else {
            // If somehow reached here without login, just go back
            finish();
            return;
        }

        initViews();
        setupClickListeners();
        setupBottomNavigation();
        
        // SK PINJI FIX: Determine who to track without kicking user to Login
        determineTrackingTarget();
        checkLocationPermission();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnTrack = findViewById(R.id.btnTrack);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        // Using the header title for tracking status
        tvTrackingName = findViewById(R.id.header).findViewById(android.R.id.text1 != 0 ? android.R.id.text1 : R.id.btnBack); 
        // Fallback: we'll find the center title by ID if possible, otherwise use a Toast
    }

    private void determineTrackingTarget() {
        // First, check the teacher's role
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String role = doc.getString("role");
                    if ("Guardian".equalsIgnoreCase(role)) {
                        findLinkedStudent();
                    } else {
                        initFirebaseTracking(currentUserId);
                    }
                }
            })
            .addOnFailureListener(e -> {
                // Fail-safe: just track yourself
                initFirebaseTracking(currentUserId);
            });
    }

    private void findLinkedStudent() {
        db.collection("student_guardian_links")
            .whereEqualTo("guardian_uid", currentUserId)
            .limit(1)
            .get()
            .addOnSuccessListener(snapshots -> {
                if (!snapshots.isEmpty()) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        targetTrackId = doc.getString("student_uid");
                        initFirebaseTracking(targetTrackId);
                    }
                } else {
                    Toast.makeText(this, "No linked students found", Toast.LENGTH_SHORT).show();
                    initFirebaseTracking(currentUserId);
                }
            });
    }

    private void initFirebaseTracking(String uid) {
        if (locationListener != null && databaseRef != null) {
            databaseRef.removeEventListener(locationListener);
        }

        databaseRef = FirebaseDatabase.getInstance().getReference("locations").child(uid);
        locationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        latestLat = snapshot.child("latitude").getValue(Double.class);
                        latestLng = snapshot.child("longitude").getValue(Double.class);
                        lastUpdatedTime = snapshot.child("dateTime").getValue(String.class);
                        updateLocationUI();
                    } catch (Exception e) {
                        Log.e(TAG, "GPS Error");
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        databaseRef.addValueEventListener(locationListener);
    }

    private void updateLocationUI() {
        if (tvLastUpdated != null) {
            tvLastUpdated.setText("Live Signal: " + lastUpdatedTime);
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnTrack.setOnClickListener(v -> {
            if (latestLat != 0.0) {
                String uri = "http://maps.google.com/maps?q=" + latestLat + "," + latestLng;
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
            } else {
                Toast.makeText(this, "Waiting for student GPS...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationService();
        }
    }

    private void startLocationService() {
        // Students are the only ones sending location
        db.collection("users").document(currentUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists() && "Student".equalsIgnoreCase(doc.getString("role"))) {
                Intent intent = new Intent(this, LocationTrackingService.class);
                intent.putExtra("userId", currentUserId);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
                else startService(intent);
            }
        });
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v -> finish());
        findViewById(R.id.navSchedule).setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseRef != null && locationListener != null) databaseRef.removeEventListener(locationListener);
    }
}
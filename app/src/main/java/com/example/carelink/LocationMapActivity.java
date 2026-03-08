package com.example.carelink;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

import java.util.ArrayList;
import java.util.List;

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
    
    private List<StudentLink> linkedStudents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_map);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // REDUNDANT EMULATOR CALLS REMOVED - HANDLED BY CareLinkApp.java
        
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            targetTrackId = currentUserId; 
        } else {
            finish();
            return;
        }

        initViews();
        setupClickListeners();
        setupBottomNavigation();
        determineTrackingTarget();
        checkLocationPermission();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnTrack = findViewById(R.id.btnTrack);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);
        tvTrackingName = findViewById(R.id.tvTrackingName);

        if (tvTrackingName != null) {
            tvTrackingName.setOnClickListener(v -> showStudentPicker());
        }
    }

    private void determineTrackingTarget() {
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String role = doc.getString("role");
                    if ("Guardian".equalsIgnoreCase(role)) {
                        fetchAllLinkedStudents();
                    } else {
                        if (tvTrackingName != null) tvTrackingName.setText("My Location");
                        initFirebaseTracking(currentUserId);
                    }
                }
            })
            .addOnFailureListener(e -> initFirebaseTracking(currentUserId));
    }

    private void fetchAllLinkedStudents() {
        db.collection("student_guardian_links")
            .whereEqualTo("guardian_uid", currentUserId)
            .get()
            .addOnSuccessListener(snapshots -> {
                linkedStudents.clear();
                if (!snapshots.isEmpty()) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String uid = doc.getString("student_uid");
                        fetchStudentDetails(uid);
                    }
                } else {
                    if (tvTrackingName != null) tvTrackingName.setText("No Students Linked");
                    initFirebaseTracking(currentUserId);
                }
            });
    }

    private void fetchStudentDetails(String uid) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("name");
                    linkedStudents.add(new StudentLink(uid, name));
                    if (linkedStudents.size() == 1) {
                        targetTrackId = uid;
                        if (tvTrackingName != null) tvTrackingName.setText("Tracking: " + name + " ▼");
                        initFirebaseTracking(uid);
                    }
                }
            });
    }

    private void showStudentPicker() {
        if (linkedStudents.size() <= 1) return;

        String[] names = new String[linkedStudents.size()];
        for (int i = 0; i < linkedStudents.size(); i++) {
            names[i] = linkedStudents.get(i).name;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Student")
                .setItems(names, (dialog, which) -> {
                    StudentLink selected = linkedStudents.get(which);
                    targetTrackId = selected.uid;
                    if (tvTrackingName != null) tvTrackingName.setText("Tracking: " + selected.name + " ▼");
                    initFirebaseTracking(targetTrackId);
                })
                .show();
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
                        if (tvLastUpdated != null) tvLastUpdated.setText("Live Signal: " + lastUpdatedTime);
                    } catch (Exception e) {}
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        databaseRef.addValueEventListener(locationListener);
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnTrack != null) {
            btnTrack.setOnClickListener(v -> {
                if (latestLat != 0.0) {
                    String uri = "http://maps.google.com/maps?q=" + latestLat + "," + latestLng;
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
                } else {
                    Toast.makeText(this, "GPS signal not ready...", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void setupBottomNavigation() {
        View home = findViewById(R.id.navHome);
        if (home != null) home.setOnClickListener(v -> finish());
        
        View schedule = findViewById(R.id.navSchedule);
        if (schedule != null) schedule.setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity.class)));
        
        View profile = findViewById(R.id.navProfile);
        if (profile != null) profile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseRef != null && locationListener != null) databaseRef.removeEventListener(locationListener);
    }

    private static class StudentLink {
        String uid, name;
        StudentLink(String uid, String name) { this.uid = uid; this.name = name; }
    }
}
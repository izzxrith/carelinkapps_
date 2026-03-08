package com.example.carelink;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LocationMapActivity extends AppCompatActivity {

    private static final String TAG = "LocationMapActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    
    private String currentUserId; 

    private ImageView btnBack, btnMore;
    private MaterialButton btnYesterday, btnWednesday, btnThursday, btnFriday, btnTrack;
    private LinearLayout navHome, navMessages, navSchedule, navProfile;
    private TextView tvLastUpdated;

    private DatabaseReference databaseRef;
    private ValueEventListener locationListener;
    private FirebaseAuth mAuth;

    private double latestLat = 0.0;
    private double latestLng = 0.0;
    private String lastUpdatedTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_map);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Session expired. Please login.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupClickListeners();
        setupBottomNavigation();
        initFirebaseTracking();

        checkLocationPermission();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnMore = findViewById(R.id.btnMore);
        btnYesterday = findViewById(R.id.btnYesterday);
        btnWednesday = findViewById(R.id.btnWednesday);
        btnThursday = findViewById(R.id.btnThursday);
        btnFriday = findViewById(R.id.btnFriday);
        btnTrack = findViewById(R.id.btnTrack);

        tvLastUpdated = findViewById(R.id.tvLastUpdated);

        navHome = findViewById(R.id.navHome);
        navMessages = findViewById(R.id.navMessages);
        navSchedule = findViewById(R.id.navSchedule);
        navProfile = findViewById(R.id.navProfile);
    }

    private void initFirebaseTracking() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        // Tracking the current logged-in user
        databaseRef = database.getReference("locations").child(currentUserId);

        locationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        latestLat = snapshot.child("latitude").getValue(Double.class);
                        latestLng = snapshot.child("longitude").getValue(Double.class);
                        lastUpdatedTime = snapshot.child("dateTime").getValue(String.class);

                        Log.d(TAG, "Location Update: " + latestLat + ", " + latestLng);
                        updateLocationUI();
                    } catch (Exception e) {
                        Log.e(TAG, "Data Error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LocationMapActivity.this,
                        "Tracking Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        databaseRef.addValueEventListener(locationListener);
    }

    private void updateLocationUI() {
        if (tvLastUpdated != null && lastUpdatedTime != null) {
            tvLastUpdated.setText("Live Location: " + lastUpdatedTime);
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnMore.setOnClickListener(v -> {
            Toast.makeText(this, "Alert History - Coming Soon", Toast.LENGTH_SHORT).show();
        });

        btnYesterday.setOnClickListener(v -> selectDay(btnYesterday));
        btnWednesday.setOnClickListener(v -> selectDay(btnWednesday));
        btnThursday.setOnClickListener(v -> selectDay(btnThursday));
        btnFriday.setOnClickListener(v -> selectDay(btnFriday));

        btnTrack.setOnClickListener(v -> {
            if (latestLat != 0.0 && latestLng != 0.0) {
                openGoogleMaps(latestLat, latestLng);
            } else {
                Toast.makeText(this, "Waiting for live signal...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openGoogleMaps(double latitude, double longitude) {
        Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude +
                    "?q=" + latitude + "," + longitude + "(CareLink Patient)&z=18");
        
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback to browser
            String url = "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude;
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }

    private void checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                startLocationService();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                startLocationService();
            }
        }
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        serviceIntent.putExtra("userId", currentUserId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
            }
        }
    }

    private void selectDay(MaterialButton selected) {
        resetDayButton(btnYesterday);
        resetDayButton(btnWednesday);
        resetDayButton(btnThursday);
        resetDayButton(btnFriday);
        selected.setBackgroundTintList(getColorStateList(R.color.green_primary));
        selected.setTextColor(getColor(R.color.white));
    }

    private void resetDayButton(MaterialButton button) {
        button.setBackgroundTintList(getColorStateList(R.color.gray_light));
        button.setTextColor(getColor(R.color.gray));
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
        if (databaseRef != null && locationListener != null) {
            databaseRef.removeEventListener(locationListener);
        }
    }
}
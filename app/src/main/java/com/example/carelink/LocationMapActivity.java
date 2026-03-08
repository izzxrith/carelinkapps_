package com.example.carelink;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LocationMapActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String TRACKED_USER_ID = "elderly_user_001"; // ID of person being tracked

    private ImageView btnBack, btnMore;
    private MaterialButton btnYesterday, btnWednesday, btnThursday, btnFriday, btnTrack;
    private LinearLayout navHome, navMessages, navSchedule, navProfile;
    private TextView tvLastUpdated;

    private DatabaseReference databaseRef;
    private ValueEventListener locationListener;
    private Handler handler;
    private Runnable refreshRunnable;

    private double latestLat = 0.0;
    private double latestLng = 0.0;
    private String lastUpdatedTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_map);

        initViews();
        setupClickListeners();
        setupBottomNavigation();
        initFirebase();

        checkLocationPermission();

        FirebaseDatabase.getInstance().getReference("test").setValue("Hello Firebase")
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Firebase Connected!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Firebase Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
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

    private void initFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseRef = database.getReference("locations").child(TRACKED_USER_ID);

        locationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    latestLat = snapshot.child("latitude").getValue(Double.class);
                    latestLng = snapshot.child("longitude").getValue(Double.class);
                    lastUpdatedTime = snapshot.child("dateTime").getValue(String.class);

                    // Update UI
                    updateLocationUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LocationMapActivity.this,
                        "Failed to load location: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        databaseRef.addValueEventListener(locationListener);
    }

    private void updateLocationUI() {
        // Update the last updated time text
        if (tvLastUpdated != null && lastUpdatedTime != null) {
            tvLastUpdated.setText("Last updated: " + lastUpdatedTime);
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnMore.setOnClickListener(v -> {
            Toast.makeText(this, "More options", Toast.LENGTH_SHORT).show();
        });

        btnYesterday.setOnClickListener(v -> selectDay(btnYesterday));
        btnWednesday.setOnClickListener(v -> selectDay(btnWednesday));
        btnThursday.setOnClickListener(v -> selectDay(btnThursday));
        btnFriday.setOnClickListener(v -> selectDay(btnFriday));

        btnTrack.setOnClickListener(v -> {
            if (latestLat != 0.0 && latestLng != 0.0) {
                openGoogleMaps(latestLat, latestLng);
            } else {
                Toast.makeText(this, "Location not available yet. Please wait...",
                        Toast.LENGTH_SHORT).show();
                // Optionally open default map
                openGoogleMaps(0, 0);
            }
        });
    }

    private void openGoogleMaps(double latitude, double longitude) {
        Uri gmmIntentUri;

        if (latitude != 0.0 && longitude != 0.0) {
            // Open specific location with marker
            gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude +
                    "?q=" + latitude + "," + longitude + "(Tracked User)&z=17");
        } else {
            // Open Google Maps app default
            gmmIntentUri = Uri.parse("geo:0,0?q=Current+Location");
        }

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://maps.google.com/?q=" + latitude + "," + longitude));
            startActivity(browserIntent);
        }
    }

    private void checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        },
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                startLocationService();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                startLocationService();
            }
        }
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        serviceIntent.putExtra("userId", TRACKED_USER_ID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Toast.makeText(this, "Location tracking started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
            } else {
                Toast.makeText(this, "Location permission required for tracking",
                        Toast.LENGTH_LONG).show();
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

        Toast.makeText(this, selected.getText() + " selected", Toast.LENGTH_SHORT).show();
    }

    private void resetDayButton(MaterialButton button) {
        button.setBackgroundTintList(getColorStateList(R.color.gray_light));
        button.setTextColor(getColor(R.color.gray));
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        navMessages.setOnClickListener(v -> {
            Toast.makeText(this, "Messages - Coming Soon", Toast.LENGTH_SHORT).show();
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
        if (databaseRef != null && locationListener != null) {
            databaseRef.removeEventListener(locationListener);
        }
        if (handler != null && refreshRunnable != null) {
            handler.removeCallbacks(refreshRunnable);
        }
    }
}
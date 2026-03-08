package com.example.carelink;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LocationTrackingService extends Service {

    private static final String TAG = "LocationTrackingService";
    private static final String CHANNEL_ID = "location_tracking_channel";
    private static final int NOTIFICATION_ID = 1;

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference databaseRef;
    private FirebaseFirestore db;
    private LocationCallback locationCallback;
    private String userId;

    private static final long UPDATE_INTERVAL = 30000; // 30 seconds for live
    private static final long MIN_UPDATE_INTERVAL = 20000; // 20 seconds
    
    // FORTIFICATION: Track timing for historical logging
    private long lastHistoryLogTime = 0;
    private static final long HISTORY_LOG_INTERVAL = 600000; // Log to Firestore every 10 minutes (Breadcrumbs)

    // SK PINJI PRECISE COORDINATES
    private static final double SK_PINJI_LAT = 4.565549;
    private static final double SK_PINJI_LNG = 101.081350;

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseRef = database.getReference("locations");
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            userId = intent.getStringExtra("userId");
        }
        if (userId == null) {
            userId = "user_default";
        }

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        startLocationUpdates();

        return START_STICKY;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY, UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL)
                .setMaxUpdateDelayMillis(UPDATE_INTERVAL * 2)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    updateLocationToFirebase(location);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted", e);
        }
    }

    private void updateLocationToFirebase(Location location) {
        long now = System.currentTimeMillis();
        String dateTime = getCurrentDateTime();

        // 1. LIVE DATA (Realtime DB - for immediate tracking)
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("accuracy", location.getAccuracy());
        locationData.put("timestamp", now);
        locationData.put("dateTime", dateTime);

        // PREDICTIVE LOGIC: Check if outside school zone
        float[] results = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(), SK_PINJI_LAT, SK_PINJI_LNG, results);
        if (results[0] > 500) {
            locationData.put("status", "WANDERING");
        } else {
            locationData.put("status", "SAFE");
        }

        databaseRef.child(userId).setValue(locationData);

        // 2. HISTORICAL DATA (Firestore - 90-day breadcrumbs)
        if (now - lastHistoryLogTime >= HISTORY_LOG_INTERVAL) {
            logLocationToHistory(location, now, dateTime);
            lastHistoryLogTime = now;
        }
    }

    private void logLocationToHistory(Location location, long timestamp, String dateTime) {
        Map<String, Object> historyData = new HashMap<>();
        historyData.put("latitude", location.getLatitude());
        historyData.put("longitude", location.getLongitude());
        historyData.put("timestamp", timestamp);
        historyData.put("dateTime", dateTime);

        db.collection("users").document(userId)
                .collection("location_history").add(historyData);
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("CareLink tracking active for student safety");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, LocationMapActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("CareLink Safety Active")
                .setContentText("Student location is being protected.")
                .setSmallIcon(R.drawable.ic_location_pin)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
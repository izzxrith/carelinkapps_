package com.example.carelink;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EmergencyAlertActivity extends AppCompatActivity {

    private TextView tvAlertTitle, tvAlertMessage, tvCountdown, tvLocation, tvUserName;
    private Button btnImSafe, btnCallEmergency;
    private Vibrator vibrator;
    private MediaPlayer mediaPlayer;
    private CountDownTimer countDownTimer;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private static final int COUNTDOWN_SECONDS = 15;
    private static final String EMERGENCY_NUMBER = "999";
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int SMS_PERMISSION_REQUEST = 1002;
    private static final int CALL_PERMISSION_REQUEST = 1003;

    private String userName = "MeiYu";
    private String userId = "";
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private boolean isEmergencyActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_emergency_alert);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            loadUserName();
        }

        String emergencyType = getIntent().getStringExtra("EMERGENCY_TYPE");
        String emergencyMessage = getIntent().getStringExtra("EMERGENCY_MESSAGE");
        int heartRate = getIntent().getIntExtra("HEART_RATE", 0);

        initViews();
        setupEmergency(emergencyType, emergencyMessage, heartRate);

        if (checkPermissions()) {
            startEmergencyProtocol();
        }
    }

    private void initViews() {
        tvAlertTitle = findViewById(R.id.tvAlertTitle);
        tvAlertMessage = findViewById(R.id.tvAlertMessage);
        tvCountdown = findViewById(R.id.tvCountdown);
        tvLocation = findViewById(R.id.tvLocation);
        tvUserName = findViewById(R.id.tvUserName);
        btnImSafe = findViewById(R.id.btnImSafe);
        btnCallEmergency = findViewById(R.id.btnCallEmergency);

        btnImSafe.setOnClickListener(v -> cancelEmergency());
        btnCallEmergency.setOnClickListener(v -> triggerEmergencyAlert());
    }

    private void loadUserName() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        if (name != null && !name.isEmpty()) {
                            userName = name;
                            tvUserName.setText(userName + " needs help!");
                        }
                    }
                });
    }

    private void setupEmergency(String type, String message, int heartRate) {
        tvUserName.setText(userName + " needs help!");

        switch (type) {
            case "FALL_DETECTED":
                tvAlertTitle.setText("FALL DETECTED!");
                tvAlertMessage.setText("Sudden impact detected! " + userName + " may have fallen.\n\nChecking if they're okay...");
                break;

            case "LOW_EMOTION":
                tvAlertTitle.setText("EMOTIONAL CRISIS");
                tvAlertMessage.setText(userName + "'s emotional state indicates severe distress.\nImmediate support needed!");
                break;

            case "ABNORMAL_HEART_RATE_HIGH":
                tvAlertTitle.setText("DANGEROUS HEART RATE");
                tvAlertMessage.setText("Heart rate is critically HIGH: " + heartRate + " BPM\nPossible cardiac emergency!");
                break;

            case "ABNORMAL_HEART_RATE_LOW":
                tvAlertTitle.setText("DANGEROUS HEART RATE");
                tvAlertMessage.setText("Heart rate is critically LOW: " + heartRate + " BPM\nPossible bradycardia!");
                break;

            case "NO_MOVEMENT":
                tvAlertTitle.setText("NO MOVEMENT DETECTED");
                tvAlertMessage.setText(userName + " hasn't moved for an extended period.\nUnusual inactivity alert!");
                break;

            default:
                tvAlertTitle.setText("[SOS] EMERGENCY ALERT");
                tvAlertMessage.setText(message != null ? message : "Emergency situation detected!");
        }
    }

    private boolean checkPermissions() {
        boolean locationGranted = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean smsGranted = ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean callGranted = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
        boolean vibrateGranted = ContextCompat.checkSelfPermission(this,
                Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED;

        if (!locationGranted) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return false;
        }

        if (!smsGranted) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST);
            return false;
        }

        if (!callGranted) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    CALL_PERMISSION_REQUEST);
            return false;
        }

        if (!vibrateGranted) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.VIBRATE},
                    1004);
            return false;
        }

        return true;
    }

    private void startEmergencyProtocol() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            mediaPlayer = MediaPlayer.create(this, alarmUri);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(1.0f, 1.0f);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 500, 200, 500, 200, 500, 200};
            vibrator.vibrate(pattern, 0);
        }

        startLocationUpdates();

        startCountdown();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(1000)
                .setMaxUpdateDelayMillis(3000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    updateLocation(location);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback,
                    Looper.getMainLooper());
        }
    }

    private void updateLocation(Location location) {
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();

        String locationText = String.format("REAL-TIME LOCATION\n" +
                        "Latitude: %.6f\n" +
                        "Longitude: %.6f\n" +
                        "Accuracy: %.1f meters\n" +
                        "Speed: %.1f m/s\n" +
                        "Updated: %s",
                currentLatitude,
                currentLongitude,
                location.getAccuracy(),
                location.hasSpeed() ? location.getSpeed() : 0.0,
                java.text.DateFormat.getTimeInstance().format(new java.util.Date()));

        tvLocation.setText(locationText);

        saveEmergencyToFirebase(location);
    }

    private void saveEmergencyToFirebase(Location location) {
        Map<String, Object> emergencyData = new HashMap<>();
        emergencyData.put("userId", userId);
        emergencyData.put("userName", userName);
        emergencyData.put("latitude", location.getLatitude());
        emergencyData.put("longitude", location.getLongitude());
        emergencyData.put("accuracy", location.getAccuracy());
        emergencyData.put("timestamp", System.currentTimeMillis());
        emergencyData.put("status", isEmergencyActive ? "ACTIVE" : "CANCELLED");
        emergencyData.put("locationString", location.getLatitude() + ", " + location.getLongitude());
        emergencyData.put("googleMapsLink", "https://maps.google.com/?q=" +
                location.getLatitude() + "," + location.getLongitude());

        db.collection("emergencies").document(userId)
                .set(emergencyData);
    }

    private void startCountdown() {
        countDownTimer = new CountDownTimer(COUNTDOWN_SECONDS * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                tvCountdown.setText(String.format("Emergency services will be contacted in: %d seconds", seconds));

                if (seconds <= 5) {
                    tvCountdown.setTextColor(getColor(android.R.color.holo_red_dark));
                }
            }
            @Override
            public void onFinish() {
                if (isEmergencyActive) {
                    triggerEmergencyAlert();
                }
            }
        }.start();
    }

    private void cancelEmergency() {
        isEmergencyActive = false;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        stopAlarmAndVibration();

        Map<String, Object> update = new HashMap<>();
        update.put("status", "CANCELLED_BY_USER");
        update.put("cancelledAt", System.currentTimeMillis());
        db.collection("emergencies").document(userId).update(update);

        Toast.makeText(this, "Emergency cancelled. Stay safe, " + userName + "!", Toast.LENGTH_LONG).show();
        finish();
    }

    private void triggerEmergencyAlert() {
        isEmergencyActive = true;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        tvCountdown.setText("CONTACTING EMERGENCY SERVICES NOW!");
        tvCountdown.setTextColor(getColor(android.R.color.holo_red_dark));

        sendEmergencySMS();

        sendPushNotification();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            makeEmergencyCall();
        }, 3000);
    }

    private void sendEmergencySMS() {
        db.collection("users").document(userId).collection("emergency_contacts")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        sendSMSToNumber("+60123456789", "DEFAULT");
                        return;
                    }

                    for (com.google.firebase.firestore.DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String phone = document.getString("phone");
                        String name = document.getString("name");
                        if (phone != null) {
                            sendSMSToNumber(phone, name);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    sendSMSToNumber("+60123456789", "Emergency Contact");
                });
    }

    private void sendSMSToNumber(String phoneNumber, String contactName) {
        try {
            String message = String.format(
                    "CARELINK EMERGENCY ALERT!\n\n" +
                            "%s needs IMMEDIATE help!\n\n" +
                            "Location: https://maps.google.com/?q=%.6f,%.6f\n" +
                            "Time: %s\n\n" +
                            "Real-time tracking active. Please respond immediately!",
                    userName,
                    currentLatitude,
                    currentLongitude,
                    java.text.DateFormat.getDateTimeInstance().format(new java.util.Date())
            );

            SmsManager smsManager = SmsManager.getDefault();

            if (message.length() > 160) {
                java.util.ArrayList<String> parts = smsManager.divideMessage(message);
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendPushNotification() {
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", "Emergency: " + userName + " needs help!");
        notification.put("body", "Location: " + currentLatitude + ", " + currentLongitude);
        notification.put("userId", userId);
        notification.put("latitude", currentLatitude);
        notification.put("longitude", currentLongitude);
        notification.put("timestamp", System.currentTimeMillis());

        db.collection("notifications").add(notification);
    }

    private void makeEmergencyCall() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {

            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_CALL);
            intent.setData(android.net.Uri.parse("tel:" + EMERGENCY_NUMBER));
            startActivity(intent);
        }
    }

    private void stopAlarmAndVibration() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer = null;
        }

        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            startEmergencyProtocol();
        } else {
            Toast.makeText(this, "Permissions required for emergency features", Toast.LENGTH_LONG).show();
            startEmergencyProtocol();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarmAndVibration();

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed(); // Added super call
        Toast.makeText(this, "Click 'I'M SAFE' to cancel emergency", Toast.LENGTH_SHORT).show();
    }
}
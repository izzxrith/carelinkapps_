package com.example.carelink;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WatchDashboardActivity extends AppCompatActivity {

    private static final String TAG = "WatchDashboard";
    private TextView tvWatchBpm, tvWatchLogout;
    private MaterialButton btnWatchSOS;
    private ImageView ivWatchLink;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DatabaseReference watchRef;
    
    private String currentUserName = "Student";
    private boolean isSosTriggered = false;
    private Handler sensorHandler = new Handler();
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        tvWatchBpm = findViewById(R.id.tvWatchBpm);
        btnWatchSOS = findViewById(R.id.btnWatchSOS);
        tvWatchLogout = findViewById(R.id.tvWatchLogout);
        ivWatchLink = findViewById(R.id.ivWatchLink);

        loadUserData();
        startVitalsStreaming(); // SK PINJI: Start sending data to Teacher

        btnWatchSOS.setOnClickListener(v -> triggerSOS());
        ivWatchLink.setOnClickListener(v -> startActivity(new Intent(this, QRCodeActivity.class)));
        tvWatchLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void loadUserData() {
        db.collection("users").document(mAuth.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUserName = doc.contains("name") ? doc.getString("name") : "Student";
                    }
                });
    }

    // This method makes the Watch "Send" data to the Teacher's Dashboard
    private void startVitalsStreaming() {
        watchRef = FirebaseDatabase.getInstance().getReference("users")
                .child(mAuth.getUid()).child("vitals");

        sensorHandler.post(new Runnable() {
            @Override
            public void run() {
                // Simulate sensor reading (70-90 BPM)
                int heartRate = 70 + random.nextInt(20);
                tvWatchBpm.setText(heartRate + " BPM");

                // Push to Firebase so Teacher sees it
                Map<String, Object> vitals = new HashMap<>();
                vitals.put("heart_rate", heartRate);
                vitals.put("timestamp", System.currentTimeMillis());
                watchRef.setValue(vitals);

                // Send every 30 seconds (Spark Plan Optimized)
                sensorHandler.postDelayed(this, 30000);
            }
        });
    }

    private void triggerSOS() {
        if (isSosTriggered) return;
        isSosTriggered = true;

        Toast.makeText(this, "SOS SENT TO TEACHER!", Toast.LENGTH_LONG).show();

        // 1. Dial Emergency (Manual Backup)
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:999"));
        startActivity(intent);

        // 2. Send Smart SOS to Teacher's Dashboard
        FirebaseDatabase.getInstance().getReference("locations").child(mAuth.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        double lat = snapshot.child("latitude").getValue(Double.class) != null ? snapshot.child("latitude").getValue(Double.class) : 0;
                        double lng = snapshot.child("longitude").getValue(Double.class) != null ? snapshot.child("longitude").getValue(Double.class) : 0;
                        sendSOSAlertToCloud(lat, lng);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        new Handler().postDelayed(() -> isSosTriggered = false, 5000);
    }

    private void sendSOSAlertToCloud(double lat, double lng) {
        Map<String, Object> sos = new HashMap<>();
        sos.put("type", "WATCH SOS");
        sos.put("patientName", currentUserName);
        sos.put("latitude", lat);
        sos.put("longitude", lng);
        sos.put("timestamp", System.currentTimeMillis());
        sos.put("status", "ACTIVE");

        db.collection("emergency_alerts").add(sos);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorHandler.removeCallbacksAndMessages(null);
    }
}
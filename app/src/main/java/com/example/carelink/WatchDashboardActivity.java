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
            startActivity(new Intent(this, WatchPairingActivity.class));
            finish();
            return;
        }

        tvWatchBpm = findViewById(R.id.tvWatchBpm);
        btnWatchSOS = findViewById(R.id.btnWatchSOS);
        tvWatchLogout = findViewById(R.id.tvWatchLogout);
        ivWatchLink = findViewById(R.id.ivWatchLink);

        loadUserDataSync(); // Critical: Load name before allowing SOS
        startVitalsStreaming();

        btnWatchSOS.setOnClickListener(v -> triggerSOS());
        ivWatchLink.setOnClickListener(v -> startActivity(new Intent(this, QRCodeActivity.class)));
        
        tvWatchLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, IntroActivity.class)); // Go back to start
            finish();
        });
    }

    private void loadUserDataSync() {
        db.collection("users").document(mAuth.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUserName = doc.contains("name") ? doc.getString("name") : "Student";
                        Log.d(TAG, "Watch active for: " + currentUserName);
                    }
                });
    }

    private void startVitalsStreaming() {
        watchRef = FirebaseDatabase.getInstance().getReference("users")
                .child(mAuth.getUid()).child("vitals");

        sensorHandler.post(new Runnable() {
            @Override
            public void run() {
                int heartRate = 70 + random.nextInt(20);
                tvWatchBpm.setText(heartRate + " BPM");

                Map<String, Object> vitals = new HashMap<>();
                vitals.put("heart_rate", heartRate);
                vitals.put("timestamp", System.currentTimeMillis());
                watchRef.setValue(vitals);

                sensorHandler.postDelayed(this, 30000);
            }
        });
    }

    private void triggerSOS() {
        if (isSosTriggered) return;
        isSosTriggered = true;

        Toast.makeText(this, "EMERGENCY SIGNAL SENT", Toast.LENGTH_LONG).show();

        FirebaseDatabase.getInstance().getReference("locations").child(mAuth.getUid())
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
                        sendSOSAlertToCloud(lat, lng);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        // Dial 999 as backup
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:999"));
        startActivity(intent);

        new Handler().postDelayed(() -> isSosTriggered = false, 5000);
    }

    private void sendSOSAlertToCloud(double lat, double lng) {
        Map<String, Object> sos = new HashMap<>();
        sos.put("type", "CRITICAL WATCH SOS");
        sos.put("patientName", currentUserName);
        sos.put("latitude", lat);
        sos.put("longitude", lng);
        sos.put("timestamp", System.currentTimeMillis());
        sos.put("status", "ACTIVE");
        sos.put("patient_uid", mAuth.getUid());

        db.collection("emergency_alerts").add(sos);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorHandler.removeCallbacksAndMessages(null);
    }
}
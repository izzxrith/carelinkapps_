package com.example.carelink;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class EmotionActivity extends AppCompatActivity {

    private static final String TAG = "EmotionActivity";
    private TextView tvStatus, tvHeartRate, tvEmotionResult, tvAnalysis;
    private ImageView ivHeart, ivEmotionIcon;
    private Button btnOkay;
    private CardView cardResult;
    private View pulseRing1, pulseRing2, pulseRing3;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DatabaseReference watchRef;
    private ValueEventListener watchListener;

    private String targetUserId;
    private int currentHeartRate = 72;
    private boolean isAnalyzing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        initViews();
        animateHeart();
        
        // SK PINJI: Determine if we should monitor a linked student or self
        determineTargetUser();

        setupClickListeners();

        // Start analysis after a delay to simulate "Predictive Processing"
        new Handler().postDelayed(() -> {
            if (isAnalyzing) {
                isAnalyzing = false;
                analyzeEmotionFromPulse();
            }
        }, 5000);
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvHeartRate = findViewById(R.id.tvHeartRate);
        tvEmotionResult = findViewById(R.id.tvEmotionResult);
        tvAnalysis = findViewById(R.id.tvAnalysis);
        ivHeart = findViewById(R.id.ivHeart);
        ivEmotionIcon = findViewById(R.id.ivEmotionIcon);
        btnOkay = findViewById(R.id.btnOkay);
        cardResult = findViewById(R.id.cardResult);
        pulseRing1 = findViewById(R.id.pulseRing1);
        pulseRing2 = findViewById(R.id.pulseRing2);
        pulseRing3 = findViewById(R.id.pulseRing3);

        tvStatus.setText("Connecting to Cloud...");
        cardResult.setVisibility(View.GONE);
    }

    private void determineTargetUser() {
        String myUid = mAuth.getUid();
        
        db.collection("users").document(myUid).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String role = doc.getString("role");
                    if ("Guardian".equalsIgnoreCase(role)) {
                        // Find the linked student
                        findLinkedStudent(myUid);
                    } else {
                        // User is a student, monitor self
                        targetUserId = myUid;
                        connectToLiveWatchData(targetUserId);
                    }
                }
            });
    }

    private void findLinkedStudent(String guardianUid) {
        db.collection("student_guardian_links")
            .whereEqualTo("guardian_uid", guardianUid)
            .limit(1)
            .get()
            .addOnSuccessListener(snapshots -> {
                if (!snapshots.isEmpty()) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        targetUserId = doc.getString("student_uid");
                        tvStatus.setText("Syncing with Student Watch...");
                        connectToLiveWatchData(targetUserId);
                    }
                } else {
                    tvStatus.setText("No Student Linked");
                    Toast.makeText(this, "Please link a student first", Toast.LENGTH_LONG).show();
                }
            });
    }

    private void connectToLiveWatchData(String uid) {
        watchRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("vitals");

        watchListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAnalyzing) {
                    Integer hr = snapshot.child("heart_rate").getValue(Integer.class);
                    if (hr != null) {
                        currentHeartRate = hr;
                        tvHeartRate.setText(currentHeartRate + " BPM");
                        tvStatus.setText("Receiving Live Vitals...");
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        watchRef.addValueEventListener(watchListener);
    }

    private void analyzeEmotionFromPulse() {
        String emotion;
        String analysis;
        int emotionIcon;
        int colorRes;
        int bgGradient;

        // Predictive Thresholds
        if (currentHeartRate <= 75) {
            emotion = "Peaceful & Calm";
            analysis = "Vitals indicate a stable, relaxed state. The student appears tranquil.";
            emotionIcon = R.drawable.ic_calm;
            colorRes = R.color.calm_blue;
            bgGradient = R.drawable.gradient_calm;
        } else if (currentHeartRate <= 100) {
            emotion = "Content & Happy";
            analysis = "A gentle elevation suggests a positive emotional state or engagement.";
            emotionIcon = R.drawable.ic_happy;
            colorRes = R.color.happy_yellow;
            bgGradient = R.drawable.gradient_happy;
        } else if (currentHeartRate <= 120) {
            emotion = "Excited / Active";
            analysis = "Elevated heart rate detected. High energy or excitement observed.";
            emotionIcon = R.drawable.ic_excited;
            colorRes = R.color.excited_orange;
            bgGradient = R.drawable.gradient_excited;
        } else {
            emotion = "Distressed / Anxious";
            analysis = "PREDICTIVE ALERT: Sustained high pulse indicates distress. Immediate attention recommended.";
            emotionIcon = R.drawable.ic_stressed;
            colorRes = R.color.stressed_red;
            bgGradient = R.drawable.gradient_stressed;
        }

        displayResult(emotion, analysis, emotionIcon, colorRes, bgGradient);
        saveInsightToCloud(emotion, currentHeartRate);
    }

    private void displayResult(String emotion, String analysis, int icon, int color, int bg) {
        tvEmotionResult.setText(emotion);
        tvAnalysis.setText(analysis);
        ivEmotionIcon.setImageResource(icon);
        tvEmotionResult.setTextColor(ContextCompat.getColor(this, color));
        btnOkay.setBackgroundColor(ContextCompat.getColor(this, color));
        cardResult.setBackgroundResource(bg);

        cardResult.setVisibility(View.VISIBLE);
        cardResult.setAlpha(0f);
        cardResult.animate().alpha(1f).setDuration(500).start();
        tvStatus.setVisibility(View.GONE);
    }

    private void saveInsightToCloud(String emotion, int hr) {
        if (targetUserId == null) return;
        Map<String, Object> insight = new HashMap<>();
        insight.put("emotion", emotion);
        insight.put("heartRate", hr);
        insight.put("timestamp", System.currentTimeMillis());
        db.collection("users").document(targetUserId).collection("behavioral_insights").add(insight);
    }

    private void setupClickListeners() {
        btnOkay.setOnClickListener(v -> finish());
    }

    private void animateHeart() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivHeart, "scaleX", 1f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivHeart, "scaleY", 1f, 1.2f, 1f);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setDuration(800);
        scaleY.setDuration(800);
        scaleX.start();
        scaleY.start();

        animatePulseRing(pulseRing1, 0);
        animatePulseRing(pulseRing2, 400);
    }

    private void animatePulseRing(View ring, long delay) {
        ObjectAnimator sX = ObjectAnimator.ofFloat(ring, "scaleX", 1f, 2.5f);
        ObjectAnimator sY = ObjectAnimator.ofFloat(ring, "scaleY", 1f, 2.5f);
        ObjectAnimator a = ObjectAnimator.ofFloat(ring, "alpha", 0.6f, 0f);
        sX.setRepeatCount(ValueAnimator.INFINITE);
        sY.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatCount(ValueAnimator.INFINITE);
        sX.setDuration(1500); sY.setDuration(1500); a.setDuration(1500);
        sX.setStartDelay(delay); sY.setStartDelay(delay); a.setStartDelay(delay);
        sX.start(); sY.start(); a.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (watchRef != null && watchListener != null) {
            watchRef.removeEventListener(watchListener);
        }
    }
}
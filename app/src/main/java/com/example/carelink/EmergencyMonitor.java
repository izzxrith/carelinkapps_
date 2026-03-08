package com.example.carelink;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EmergencyMonitor {

    private Context context;
    private FirebaseFirestore db;
    private Handler handler;
    private List<Integer> heartRateHistory;
    private List<String> emotionHistory;

    private static final int CRITICAL_HIGH_HR = 150;
    private static final int CRITICAL_LOW_HR = 40;
    private static final int LOW_EMOTION_THRESHOLD = 3; // 3 consecutive low emotions

    private int consecutiveLowEmotions = 0;
    private boolean isMonitoring = false;

    public EmergencyMonitor(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.handler = new Handler(Looper.getMainLooper());
        this.heartRateHistory = new ArrayList<>();
        this.emotionHistory = new ArrayList<>();
    }

    public void startMonitoring() {
        isMonitoring = true;
        monitorHeartRate();
        monitorEmotion();
    }

    private void monitorHeartRate() {
        if (!isMonitoring) return;

        handler.postDelayed(() -> {
            int currentHR = getCurrentHeartRate();
            heartRateHistory.add(currentHR);

            if (currentHR > CRITICAL_HIGH_HR) {
                triggerEmergency("ABNORMAL_HEART_RATE_HIGH",
                        "Heart rate critical: " + currentHR, currentHR);
            } else if (currentHR < CRITICAL_LOW_HR) {
                triggerEmergency("ABNORMAL_HEART_RATE_LOW",
                        "Heart rate too low: " + currentHR, currentHR);
            }

            monitorHeartRate();
        }, 5000);
    }

    private void monitorEmotion() {
        if (!isMonitoring) return;

        handler.postDelayed(() -> {
            String currentEmotion = getCurrentEmotion();
            emotionHistory.add(currentEmotion);

            if (isNegativeEmotion(currentEmotion)) {
                consecutiveLowEmotions++;
                if (consecutiveLowEmotions >= LOW_EMOTION_THRESHOLD) {
                    triggerEmergency("LOW_EMOTION",
                            "Sustained emotional distress detected", 0);
                    consecutiveLowEmotions = 0;
                }
            } else {
                consecutiveLowEmotions = 0;
            }

            monitorEmotion();
        }, 30000);
    }

    private boolean isNegativeEmotion(String emotion) {
        return emotion.contains("Stressed") ||
                emotion.contains("Anxious") ||
                emotion.contains("Sad") ||
                emotion.contains("Depressed");
    }

    private void triggerEmergency(String type, String message, int heartRate) {
        Intent intent = new Intent(context, EmergencyAlertActivity.class);
        intent.putExtra("EMERGENCY_TYPE", type);
        intent.putExtra("EMERGENCY_MESSAGE", message);
        intent.putExtra("HEART_RATE", heartRate);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    private int getCurrentHeartRate() {
        // Get existing heart rate monitor
        // Return simulated value for now
        return 75; // Replace with actual sensor data
    }

    private String getCurrentEmotion() {
        // Get existing emotion detection
        return "Happy"; // Replace with actual emotion
    }

    public void stopMonitoring() {
        isMonitoring = false;
        handler.removeCallbacksAndMessages(null);
    }
}
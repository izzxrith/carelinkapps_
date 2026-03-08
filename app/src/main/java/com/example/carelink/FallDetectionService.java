package com.example.carelink;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FallDetectionService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private PowerManager.WakeLock wakeLock;
    private FirebaseFirestore db;
    private String userId;

    private static final float FALL_THRESHOLD = 15.0f; 
    private static final long MIN_TIME_BETWEEN_FALLS = 10000; 
    private long lastFallTime = 0;
    private boolean isProcessingFall = false;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getUid();
        }

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CareLink::FallDetection");
        wakeLock.acquire();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        startForeground(1, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            detectFall(event.values);
        }
    }

    private void detectFall(float[] values) {
        if (isProcessingFall) return;

        double acceleration = Math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2]);
        long currentTime = System.currentTimeMillis();

        if (acceleration > FALL_THRESHOLD && (currentTime - lastFallTime) > MIN_TIME_BETWEEN_FALLS) {
            lastFallTime = currentTime;
            isProcessingFall = true;
            triggerFallAlert();
        }
    }

    private void triggerFallAlert() {
        // 1. Local Alert
        Intent intent = new Intent(this, EmergencyAlertActivity.class);
        intent.putExtra("EMERGENCY_TYPE", "FALL_DETECTED");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        // 2. Cloud Alert to Guardian
        if (userId != null) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "FALL DETECTED");
            alert.put("patient_uid", userId);
            alert.put("timestamp", System.currentTimeMillis());
            alert.put("status", "ACTIVE");
            alert.put("patientName", "Student (Automated)");

            db.collection("emergency_alerts").add(alert)
                .addOnSuccessListener(doc -> Log.d("FallService", "Alert sent to Guardian"));
        }

        new android.os.Handler().postDelayed(() -> isProcessingFall = false, 10000);
    }

    private Notification createNotification() {
        String channelId = "fall_detection_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Fall Detection", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("CareLink Fall Detection")
                .setContentText("Monitoring for falls...")
                .setSmallIcon(R.drawable.ic_monitor)
                .build();
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
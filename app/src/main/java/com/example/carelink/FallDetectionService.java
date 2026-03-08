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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class FallDetectionService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private PowerManager.WakeLock wakeLock;

    private static final float FALL_THRESHOLD = 15.0f; // m/s²
    private static final long MIN_TIME_BETWEEN_FALLS = 5000; // 5 seconds
    private long lastFallTime = 0;
    private boolean isProcessingFall = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // Keep CPU running
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

        double acceleration = Math.sqrt(
                values[0] * values[0] +
                        values[1] * values[1] +
                        values[2] * values[2]
        );

        long currentTime = System.currentTimeMillis();

        if (acceleration > FALL_THRESHOLD &&
                (currentTime - lastFallTime) > MIN_TIME_BETWEEN_FALLS) {

            lastFallTime = currentTime;
            isProcessingFall = true;

            triggerFallAlert();
        }
    }

    private void triggerFallAlert() {
        Intent intent = new Intent(this, EmergencyAlertActivity.class);
        intent.putExtra("EMERGENCY_TYPE", "FALL_DETECTED");
        intent.putExtra("EMERGENCY_MESSAGE", "Fall detected via accelerometer");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        new android.os.Handler().postDelayed(() -> {
            isProcessingFall = false;
        }, 10000);
    }

    private Notification createNotification() {
        String channelId = "fall_detection_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Fall Detection",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("CareLink Fall Detection")
                .setContentText("Monitoring for falls...")
                .setSmallIcon(R.drawable.ic_monitor)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
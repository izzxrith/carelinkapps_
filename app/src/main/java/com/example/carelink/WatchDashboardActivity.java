package com.example.carelink;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WatchDashboardActivity extends AppCompatActivity {

    private static final String TAG = "WatchDashboard";
    private TextView tvWatchBpm, tvWatchLogout, tvBandStatus;
    private LinearLayout layoutBandStatus;
    private MaterialButton btnWatchSOS;
    private ImageView ivWatchLink;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DatabaseReference watchRef;
    private ValueEventListener cloudMirrorListener;
    
    private String currentUserName = "Student";
    private boolean isSosTriggered = false;
    private Handler sensorHandler = new Handler();
    private Random random = new Random();

    // HUAWEI BAND 7 CONFIG
    private static final String TARGET_MAC = "30:66:D0:05:8B:E1";
    private static final String TARGET_NAME_KEYWORD = "Huawei";
    
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt mGatt;
    private boolean isHardwareAuthorized = false;
    private ScanCallback scanCallback;

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

        initViews();
        
        if (isEmulator()) {
            setupCloudMirroring();
        } else {
            checkBluetoothPermissions();
            startVitalsStreaming();
        }

        loadUserDataSync();
        setupClickListeners();
    }

    private boolean isEmulator() {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86");
    }

    private void initViews() {
        tvWatchBpm = findViewById(R.id.tvWatchBpm);
        btnWatchSOS = findViewById(R.id.btnWatchSOS);
        tvWatchLogout = findViewById(R.id.tvWatchLogout);
        ivWatchLink = findViewById(R.id.ivWatchLink);
        tvBandStatus = findViewById(R.id.tvBandStatus);
        layoutBandStatus = findViewById(R.id.layoutBandStatus);
        
        if (isEmulator()) {
            tvBandStatus.setText("Mirroring Live Pulse...");
            layoutBandStatus.setBackgroundColor(Color.parseColor("#1976D2"));
        }
    }

    private void setupCloudMirroring() {
        watchRef = FirebaseDatabase.getInstance().getReference("users")
                .child(mAuth.getUid()).child("vitals");

        cloudMirrorListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer hr = snapshot.child("heart_rate").getValue(Integer.class);
                    if (hr != null) {
                        tvWatchBpm.setText(hr + " BPM");
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        watchRef.addValueEventListener(cloudMirrorListener);
    }

    private void checkBluetoothPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION};
        } else {
            permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        }

        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) ActivityCompat.requestPermissions(this, permissions, 101);
        else startBleScan();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startBleScan();
        }
    }

    private void startBleScan() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            tvBandStatus.setText("Enable BT & Location");
            return;
        }
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) return;

        tvBandStatus.setText("Scanning for Band...");
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                String deviceName = "";
                try {
                    if (ActivityCompat.checkSelfPermission(WatchDashboardActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        deviceName = device.getName();
                    }
                } catch (Exception e) {}

                // BROADER SEARCH: Match MAC or keyword "Huawei"
                if (TARGET_MAC.equalsIgnoreCase(device.getAddress()) || 
                   (deviceName != null && deviceName.toLowerCase().contains(TARGET_NAME_KEYWORD.toLowerCase()))) {
                    
                    authorizeHardware(device);
                    
                    if (ActivityCompat.checkSelfPermission(WatchDashboardActivity.this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                        bleScanner.stopScan(this);
                    }
                }
            }
        };
        bleScanner.startScan(scanCallback);
    }

    private void authorizeHardware(BluetoothDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        tvBandStatus.setText("Authorizing Hardware...");
        mGatt = device.connectGatt(this, true, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isHardwareAuthorized = true;
                runOnUiThread(() -> {
                    tvBandStatus.setText("Authorized: Huawei Band 7");
                    layoutBandStatus.setBackgroundColor(Color.parseColor("#2E7D32"));
                    Toast.makeText(WatchDashboardActivity.this, "Hardware Bridge Active", Toast.LENGTH_SHORT).show();
                });
            }
        }
    };

    private void startVitalsStreaming() {
        watchRef = FirebaseDatabase.getInstance().getReference("users").child(mAuth.getUid()).child("vitals");
        sensorHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isHardwareAuthorized) {
                    int heartRate = 70 + random.nextInt(15); 
                    tvWatchBpm.setText(heartRate + " BPM");
                    Map<String, Object> vitals = new HashMap<>();
                    vitals.put("heart_rate", heartRate);
                    vitals.put("timestamp", System.currentTimeMillis());
                    watchRef.setValue(vitals);
                } else {
                    tvWatchBpm.setText("-- BPM");
                }
                sensorHandler.postDelayed(this, 5000); 
            }
        });
    }

    private void loadUserDataSync() {
        db.collection("users").document(mAuth.getUid()).get().addOnSuccessListener(doc -> {
            if (doc.exists()) currentUserName = doc.getString("name");
        });
    }

    private void setupClickListeners() {
        btnWatchSOS.setOnClickListener(v -> triggerSOS());
        ivWatchLink.setOnClickListener(v -> startActivity(new Intent(this, QRCodeActivity.class)));
        tvWatchLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, IntroActivity.class));
            finish();
        });
    }

    private void triggerSOS() {
        if (isSosTriggered) return;
        isSosTriggered = true;
        Toast.makeText(this, "EMERGENCY SIGNAL SENT", Toast.LENGTH_LONG).show();
        sendSOSToCloud();
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:999")));
        new Handler().postDelayed(() -> isSosTriggered = false, 5000);
    }

    private void sendSOSToCloud() {
        Map<String, Object> sos = new HashMap<>();
        sos.put("type", "WATCH SOS");
        sos.put("patientName", currentUserName);
        sos.put("timestamp", System.currentTimeMillis());
        sos.put("status", "ACTIVE");
        sos.put("patient_uid", mAuth.getUid());
        db.collection("emergency_alerts").add(sos);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorHandler.removeCallbacksAndMessages(null);
        if (cloudMirrorListener != null && watchRef != null) watchRef.removeEventListener(cloudMirrorListener);
        if (mGatt != null) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    mGatt.disconnect();
                }
            } catch (Exception e) {}
        }
    }
}
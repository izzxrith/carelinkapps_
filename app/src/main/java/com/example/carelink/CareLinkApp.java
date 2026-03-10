package com.example.carelink;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

public class CareLinkApp extends Application {
    
    // --- SK PINJI DUAL-DEMO CONFIG ---
    private static final String LAPTOP_IP = "172.20.10.2"; 
    private static final String EMULATOR_INTERNAL_IP = "10.0.2.2";

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            boolean isDebug = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
            
            if (isDebug) {
                // AUTO-DETECT: Are we on an emulator or a real phone?
                String targetIp = isEmulator() ? EMULATOR_INTERNAL_IP : LAPTOP_IP;
                
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                FirebaseAuth auth = FirebaseAuth.getInstance();
                FirebaseDatabase rtdb = FirebaseDatabase.getInstance();

                auth.useEmulator(targetIp, 9099);
                db.useEmulator(targetIp, 8080);
                rtdb.useEmulator(targetIp, 9000);
                
                Log.d("CareLinkApp", "Running on " + (isEmulator() ? "Emulator" : "Physical Device"));
                Log.d("CareLinkApp", "Connected to Demo Cloud at: " + targetIp);
            }
        } catch (Exception e) {
            Log.d("CareLinkApp", "Emulator already connected");
        }
    }

    // Helper to detect if the app is running on an Emulator
    private boolean isEmulator() {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator");
    }
}
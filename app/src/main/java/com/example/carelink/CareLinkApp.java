package com.example.carelink;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

public class CareLinkApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // --- SK PINJI: GLOBAL EMULATOR CONFIG ---
        // This ensures the entire app uses the local backend from the start.
        try {
            // Check if we are in Debug mode (development) without relying on generated BuildConfig
            boolean isDebug = (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
            
            if (isDebug) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                FirebaseAuth auth = FirebaseAuth.getInstance();
                FirebaseDatabase rtdb = FirebaseDatabase.getInstance();

                auth.useEmulator("10.0.2.2", 9099);
                db.useEmulator("10.0.2.2", 8080);
                rtdb.useEmulator("10.0.2.2", 9000);
                
                Log.d("CareLinkApp", "Global Emulator Connection Active");
            }
        } catch (Exception e) {
            Log.d("CareLinkApp", "Emulator already initialized or connection error");
        }
    }
}